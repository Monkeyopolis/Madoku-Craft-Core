package madoku.craft.java.core.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.platform.InputConstants;
import madoku.craft.java.core.input.KeybindAPIManager;
import madoku.craft.java.core.json.JSONAPIManager;
import madoku.craft.java.core.json.JSONFormatAPIManager;
import madoku.craft.java.core.json.JSONTypeAPIManager;
import madoku.craft.mixin.core.KeyMappingAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Shared client-side UI positioning and hold-to-edit behavior for HUD/UI systems. */
public final class UiLayoutAPIManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(UiLayoutAPIManager.class);
	private static final String CONFIG_FOLDER_NAME = "madoku-craft-core";
	private static final String CONFIG_FILE_NAME = "madoku-core";
	private static final String POSITIONS = "positions";
	private static final String X = "x";
	private static final String Y = "y";
	private static final Identifier EDIT_KEY_ID = Identifier.fromNamespaceAndPath("madoku-craft", "edit_ui_layout");
	private static final int EDIT_KEY_DEFAULT = InputConstants.KEY_LALT;
	private static final int OUTLINE_COLOR = 0xCCFFFFFF;
	private static final int SELECTED_OUTLINE_COLOR = 0xFFFFFFFF;
	private static final int INSTRUCTION_BACKGROUND_COLOR = 0x99000000;
	private static final int INSTRUCTION_TEXT_COLOR = 0xFFFFFFFF;

	private static final Map<Identifier, UiLayoutElement> ELEMENTS = new LinkedHashMap<>();
	private static final Map<Identifier, Position> POSITIONS_BY_ID = new LinkedHashMap<>();
	private static final Set<Identifier> CONFIGURED_POSITION_IDS = new HashSet<>();
	private static final JSONFormatAPIManager.DynamicEntryNormalizer POSITION_ENTRY_NORMALIZER =
		(key, sourceValue) -> normalizePositionEntry(sourceValue);
	private static KeyMapping editKey;
	private static boolean initialized;
	private static boolean editMode;
	private static boolean editActivationHandled;
	private static boolean closingEditor;

	private UiLayoutAPIManager() { }

	public static void initialize() {
		if (initialized) return;
		loadConfig();
		editKey = KeybindAPIManager.register(
			EDIT_KEY_ID,
			"key.madoku-craft.edit_ui",
			EDIT_KEY_DEFAULT
		);
		ClientTickEvents.END_CLIENT_TICK.register(UiLayoutAPIManager::handleClientTick);
		initialized = true;
	}

	public static void registerElement(UiLayoutElement element) {
		if (element == null || element.id() == null) {
			throw new IllegalArgumentException("UI layout element and id must not be null.");
		}
		ELEMENTS.put(element.id(), element);
		POSITIONS_BY_ID.putIfAbsent(element.id(), new Position(element.defaultX(), element.defaultY()));
	}

	public static void unregisterElement(Identifier id) {
		if (id != null) ELEMENTS.remove(id);
	}

	public static List<UiLayoutElement> elements() {
		return List.copyOf(ELEMENTS.values());
	}

	public static boolean isEditMode() {
		return editMode;
	}

	public static Position position(UiLayoutElement element) {
		if (element == null) return new Position(0, 0);
		Minecraft client = Minecraft.getInstance();
		Position defaultPosition = new Position(element.defaultX(client), element.defaultY(client));
		Position position = CONFIGURED_POSITION_IDS.contains(element.id())
			? position(element.id(), defaultPosition.x(), defaultPosition.y())
			: defaultPosition;
		return clampToScreen(element, client, position);
	}

	public static Position position(Identifier id, int defaultX, int defaultY) {
		if (id == null) return new Position(defaultX, defaultY);
		return POSITIONS_BY_ID.computeIfAbsent(id, ignored -> new Position(defaultX, defaultY));
	}

	public static UiLayoutElement.Bounds bounds(Minecraft client, UiLayoutElement element) {
		if (element == null) return new UiLayoutElement.Bounds(0, 0, 0, 0);
		Position position = position(element);
		return element.bounds(client, position.x(), position.y());
	}

	public static void move(Identifier id, Minecraft client, double deltaX, double deltaY) {
		if (id == null || client == null) return;
		UiLayoutElement element = ELEMENTS.get(id);
		if (element == null) return;
		Position current = position(element);
		setPosition(id, client, current.x() + (int) Math.round(deltaX), current.y() + (int) Math.round(deltaY));
	}

	public static void setPosition(Identifier id, Minecraft client, int requestedX, int requestedY) {
		if (id == null) return;
		UiLayoutElement element = ELEMENTS.get(id);
		if (element == null) return;

		POSITIONS_BY_ID.put(id, clampToScreen(element, client, new Position(requestedX, requestedY)));
		CONFIGURED_POSITION_IDS.add(id);
	}

	public static void finishEditing() {
		if (!editMode) return;
		editMode = false;
		saveConfig();
	}

	private static void handleClientTick(Minecraft client) {
		if (client == null) return;
		boolean editKeyDown = isEditKeyDown();

		if (editMode && !(client.gui.screen() instanceof UiLayoutScreen)) {
			finishEditing();
		}

		if (editMode && !hasEnabledElement(client)) {
			closeEditor(client);
		}

		if (!editKeyDown && editMode) {
			closeEditor(client);
		}

		if (!editKeyDown) {
			editActivationHandled = false;
		} else if (editKeyDown && !editMode && !editActivationHandled) {
			editActivationHandled = true;
			if (canOpenEditor(client)) openEditor(client);
		}
	}

	private static boolean isEditKeyDown() {
		if (editKey == null) return false;
		InputConstants.Key key = ((KeyMappingAccessor) (Object) editKey).madokuCraft$getKey();
		if (key == null) return false;
		return key.getType() == InputConstants.Type.MOUSE
			? editKey.isDown()
			: InputConstants.isKeyDown(key.getValue());
	}

	private static boolean canOpenEditor(Minecraft client) {
		return client.player != null
			&& client.level != null
			&& client.gui.screen() == null
			&& !client.player.isSpectator()
			&& hasEnabledElement(client);
	}

	private static boolean hasEnabledElement(Minecraft client) {
		return ELEMENTS.values().stream().anyMatch(element -> element.isEnabled(client));
	}

	private static void openEditor(Minecraft client) {
		if (editMode) return;
		editMode = true;
		client.setScreenAndShow(new UiLayoutScreen());
	}

	private static void closeEditor(Minecraft client) {
		finishEditing();
		if (closingEditor) return;
		if (client.gui.screen() instanceof UiLayoutScreen) {
			closingEditor = true;
			try {
				client.setScreenAndShow(null);
			} finally {
				closingEditor = false;
			}
		}
	}

	private static JsonObject buildDefaultsJson() {
		return JSONFormatAPIManager.object().object(POSITIONS, ignored -> { }).build();
	}

	private static void loadConfig() {
		try {
			CONFIGURED_POSITION_IDS.clear();
			Path directory = JSONAPIManager.getOrCreateGlobalSystemDirectory(CONFIG_FOLDER_NAME);
			Path file = directory.resolve(CONFIG_FILE_NAME + ".json");
			JsonObject normalized = JSONFormatAPIManager.ensureManagedFile(
				file,
				buildDefaultsJson(),
				JSONTypeAPIManager.DYNAMIC_CONFIG,
				POSITION_ENTRY_NORMALIZER
			);
			JsonElement positionsElement = normalized.get(POSITIONS);
			if (positionsElement != null && positionsElement.isJsonObject()) {
				for (Map.Entry<String, JsonElement> entry : positionsElement.getAsJsonObject().entrySet()) {
					Identifier id = normalizeLayoutIdentifier(entry.getKey());
					if (id == null || !entry.getValue().isJsonObject()) continue;
					JsonObject position = entry.getValue().getAsJsonObject();
					POSITIONS_BY_ID.put(id, new Position(
						readInt(position, X, 0),
						readInt(position, Y, 0)
					));
					CONFIGURED_POSITION_IDS.add(id);
				}
			}
			JSONFormatAPIManager.writeManagedFile(
				file,
				buildConfigJson(),
				buildDefaultsJson(),
				JSONTypeAPIManager.DYNAMIC_CONFIG,
				POSITION_ENTRY_NORMALIZER
			);
		} catch (IOException | RuntimeException exception) {
			LOGGER.error("Failed to load Madoku UI layout configuration; using defaults.", exception);
		}
	}

	private static JsonObject buildConfigJson() {
		JSONFormatAPIManager.ObjectBuilder root = JSONFormatAPIManager.object();
		root.object(POSITIONS, positions -> POSITIONS_BY_ID.forEach((id, position) -> {
			if (!CONFIGURED_POSITION_IDS.contains(id)) return;
			positions.object(id.toString(), child -> child.put(X, position.x()).put(Y, position.y()));
		}));
		return root.build();
	}

	private static void saveConfig() {
		try {
			Path directory = JSONAPIManager.getOrCreateGlobalSystemDirectory(CONFIG_FOLDER_NAME);
			Path file = directory.resolve(CONFIG_FILE_NAME + ".json");
			JSONFormatAPIManager.writeManagedFile(
				file,
				buildConfigJson(),
				buildDefaultsJson(),
				JSONTypeAPIManager.DYNAMIC_CONFIG,
				POSITION_ENTRY_NORMALIZER
			);
		} catch (IOException | RuntimeException exception) {
			LOGGER.error("Failed to save Madoku UI layout configuration.", exception);
		}
	}

	private static JsonElement normalizePositionEntry(JsonElement sourceValue) {
		if (sourceValue == null || !sourceValue.isJsonObject()) return null;
		JsonObject source = sourceValue.getAsJsonObject();
		return JSONFormatAPIManager.object()
			.put(X, readInt(source, X, 0))
			.put(Y, readInt(source, Y, 0))
			.build();
	}

	private static Identifier normalizeLayoutIdentifier(String rawId) {
		Identifier parsed = Identifier.tryParse(rawId);
		if (parsed == null) return null;
		String normalizedPath = parsed.getPath().replace('_', '-');
		return normalizedPath.equals(parsed.getPath())
			? parsed
			: Identifier.fromNamespaceAndPath(parsed.getNamespace(), normalizedPath);
	}

	private static Position clampToScreen(UiLayoutElement element, Minecraft client, Position position) {
		if (element == null || client == null || client.getWindow() == null || position == null) return position;
		UiLayoutElement.Bounds bounds = element.bounds(client, position.x(), position.y());
		int screenWidth = client.getWindow().getGuiScaledWidth();
		int screenHeight = client.getWindow().getGuiScaledHeight();
		int maxX = Math.max(0, screenWidth - Math.max(0, bounds.width()));
		int maxY = Math.max(0, screenHeight - Math.max(0, bounds.height()));
		return new Position(
			Math.max(0, Math.min(maxX, position.x())),
			Math.max(0, Math.min(maxY, position.y()))
		);
	}

	private static int readInt(JsonObject source, String key, int fallback) {
		try {
			return source != null && source.has(key) ? source.get(key).getAsInt() : fallback;
		} catch (RuntimeException exception) {
			return fallback;
		}
	}

	public record Position(int x, int y) { }

	/** Transparent screen that owns cursor input while the edit key remains held. */
	private static final class UiLayoutScreen extends Screen {
		private Identifier draggingElement;

		private UiLayoutScreen() {
			super(Component.translatable("ui.madoku-craft.edit_layout"));
		}

		@Override
		public boolean isPauseScreen() {
			return false;
		}

		@Override
		public void onClose() {
			finishEditing();
			Minecraft.getInstance().setScreenAndShow(null);
		}

		@Override
		protected void init() {
			this.addRenderableOnly((context, mouseX, mouseY, partialTick) -> renderEditor(context, mouseX, mouseY));
		}

		@Override
		public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
			if (event.button() != 1) return true;
			List<UiLayoutElement> elements = new ArrayList<>(ELEMENTS.values());
			for (int index = elements.size() - 1; index >= 0; index--) {
				UiLayoutElement element = elements.get(index);
				Minecraft client = Minecraft.getInstance();
				if (!element.isEnabled(client) || !element.isVisible(client)) continue;
				if (bounds(client, element).contains(event.x(), event.y())) {
					draggingElement = element.id();
					return true;
				}
			}
			return true;
		}

		@Override
		public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
			if (event.button() == 1) draggingElement = null;
			return true;
		}

		@Override
		public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double deltaX, double deltaY) {
			if (event.button() == 1 && draggingElement != null) {
				UiLayoutAPIManager.move(draggingElement, Minecraft.getInstance(), deltaX, deltaY);
			}
			return true;
		}

		private void renderEditor(GuiGraphicsExtractor context, int mouseX, int mouseY) {
			Minecraft client = Minecraft.getInstance();
			for (UiLayoutElement element : ELEMENTS.values()) {
				if (!element.isEnabled(client) || !element.isVisible(client)) continue;
				Position position = position(element);
				UiLayoutElement.Bounds elementBounds = element.bounds(client, position.x(), position.y());
				element.renderEditor(context, client, position.x(), position.y());
				int color = element.id().equals(draggingElement) ? SELECTED_OUTLINE_COLOR : OUTLINE_COLOR;
				drawOutline(context, elementBounds, color);
			}

			String instruction = Component.translatable("ui.madoku-craft.edit_layout.instructions").getString();
			int textWidth = client.font.width(instruction);
			int textX = (this.width - textWidth) / 2;
			int textY = 8;
			context.fill(RenderPipelines.GUI, textX - 5, textY - 3, textX + textWidth + 5, textY + client.font.lineHeight + 3, INSTRUCTION_BACKGROUND_COLOR);
			context.text(client.font, instruction, textX, textY, INSTRUCTION_TEXT_COLOR, true);
		}

		private void drawOutline(GuiGraphicsExtractor context, UiLayoutElement.Bounds bounds, int color) {
			if (bounds.width() <= 0 || bounds.height() <= 0) return;
			int left = bounds.x();
			int top = bounds.y();
			int right = left + bounds.width();
			int bottom = top + bounds.height();
			context.fill(RenderPipelines.GUI, left, top, right, top + 1, color);
			context.fill(RenderPipelines.GUI, left, bottom - 1, right, bottom, color);
			context.fill(RenderPipelines.GUI, left, top, left + 1, bottom, color);
			context.fill(RenderPipelines.GUI, right - 1, top, right, bottom, color);
		}
	}
}
