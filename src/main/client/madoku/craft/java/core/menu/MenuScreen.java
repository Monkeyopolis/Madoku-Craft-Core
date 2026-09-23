package madoku.craft.java.core.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/** Main player menu opened by the configurable Tab keybind. */
public final class MenuScreen extends Screen {
	private static final Identifier BACKGROUND_SMALL_TEXTURE = texture("main-menu/small-menu-container.png");
	private static final Identifier BACKGROUND_MEDIUM_TEXTURE = texture("main-menu/medium-menu-container.png");
	private static final Identifier BACKGROUND_LARGE_TEXTURE = texture("main-menu/large-menu-container.png");
	private static final Identifier EXIT_TEXTURE = texture("shared-ui/exit-button.png");
	private static final Identifier EXIT_HIGHLIGHTED_TEXTURE = texture("shared-ui/exit-button-highlighted.png");
	private static final int TEXTURE_SIZE = 256;
	private static final int BACKGROUND_RENDER_WIDTH = 256;
	private static final int BACKGROUND_RENDER_HEIGHT = 256;
	private static final int PANEL_WIDTH = 134;
	private static final int PANEL_HEIGHT_SMALL = 92;
	private static final int PANEL_HEIGHT_MEDIUM = 124;
	private static final int PANEL_HEIGHT_LARGE = 156;
	private static final int FACE_SIZE = 32;
	private static final int FACE_SOURCE_SIZE = 8;
	private static final int FACE_LEFT_INSET = 8;
	private static final int FACE_TOP_INSET = 8;
	private static final int BUTTON_WIDTH = 56;
	private static final int BUTTON_HEIGHT = 24;
	private static final int BUTTON_COLUMN_GAP = 8;
	private static final int BUTTON_ROW_GAP = 8;
	private static final int BUTTON_LEFT_INSET = 7;
	private static final int BUTTON_TOP = 53;
	private static final int BUTTON_ICON_LEFT_INSET = 3;
	private static final int BUTTON_ICON_TOP_INSET = 6;
	private static final int BUTTON_ICON_SLOT_SIZE = 12;
	private static final int BUTTON_TEXT_GAP = 4;
	private static final int BUTTON_TEXT_COLOR = 0xFF404040;
	private static final float BUTTON_TEXT_SCALE = 0.8F;
	private static final int EXIT_SIZE = 12;
	private static final int EXIT_RIGHT_INSET = 7;
	private static final int EXIT_TOP_INSET = 7;
	private static final int HEADER_TEXT_COLOR = 0xFF404040;
	private static final int HEADER_CENTER_X = 78;
	private static final int HEADER_SEPARATOR_Y = 23;
	private static final int HEADER_SEPARATOR_HEIGHT = 2;
	private static final int HEADER_TEXT_GAP = 2;
	private static final float HEADER_TEXT_SCALE = 0.8F;
	private static final float PLAYER_NAME_TEXT_SCALE = 0.9F;

	public MenuScreen() {
		super(Component.translatable("menu.madoku-craft.title"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreenAndShow(null);
	}

	@Override
	protected void init() {
		this.addRenderableOnly((guiGraphics, mouseX, mouseY, partialTick) -> renderMenu(guiGraphics, mouseX, mouseY));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();
		if (button != 1) return super.mouseClicked(event, doubleClick);

		List<MenuEntry> entries = MenuAPIManager.entries();
		int visibleEntryCount = Math.min(entries.size(), 5);
		int panelX = panelX();
		int panelY = panelY(visibleEntryCount);
		if (contains(panelX + PANEL_WIDTH - EXIT_RIGHT_INSET - EXIT_SIZE, panelY + EXIT_TOP_INSET, EXIT_SIZE, EXIT_SIZE, mouseX, mouseY)) {
			onClose();
			return true;
		}

		for (int index = 0; index < visibleEntryCount; index++) {
			if (buttonBounds(panelX, panelY, index).contains(mouseX, mouseY)) {
				MenuAPIManager.openEntry(entries.get(index).id(), Minecraft.getInstance());
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void renderMenu(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		guiGraphics.fill(RenderPipelines.GUI, 0, 0, this.width, this.height, 0x88000000);

		List<MenuEntry> entries = MenuAPIManager.entries();
		int visibleEntryCount = Math.min(entries.size(), 5);
		int panelX = panelX();
		int panelY = panelY(visibleEntryCount);
		guiGraphics.blit(
			RenderPipelines.GUI_TEXTURED,
			backgroundTexture(visibleEntryCount),
			panelX,
			panelY,
			0.0F,
			0.0F,
			BACKGROUND_RENDER_WIDTH,
			BACKGROUND_RENDER_HEIGHT,
			TEXTURE_SIZE,
			TEXTURE_SIZE
		);

		Minecraft client = Minecraft.getInstance();
		if (client.player instanceof AbstractClientPlayer player) drawPlayerFace(guiGraphics, player, panelX + FACE_LEFT_INSET, panelY + FACE_TOP_INSET);

		String username = client.player == null ? "" : client.player.getName().getString();
		String experienceLevel = Component.translatable("menu.madoku-craft.experience_level", client.player == null ? 0 : client.player.experienceLevel).getString();
		drawScaledCenteredText(guiGraphics, username, panelX + HEADER_CENTER_X, panelY + headerNameY() + 1, PLAYER_NAME_TEXT_SCALE, HEADER_TEXT_COLOR);
		drawScaledCenteredText(guiGraphics, experienceLevel, panelX + HEADER_CENTER_X, panelY + headerLevelY(), HEADER_TEXT_SCALE, HEADER_TEXT_COLOR);

		boolean exitHovered = contains(panelX + PANEL_WIDTH - EXIT_RIGHT_INSET - EXIT_SIZE, panelY + EXIT_TOP_INSET, EXIT_SIZE, EXIT_SIZE, mouseX, mouseY);
		guiGraphics.blit(
			RenderPipelines.GUI_TEXTURED,
			exitHovered ? EXIT_HIGHLIGHTED_TEXTURE : EXIT_TEXTURE,
			panelX + PANEL_WIDTH - EXIT_RIGHT_INSET - EXIT_SIZE,
			panelY + EXIT_TOP_INSET,
			0.0F,
			0.0F,
			EXIT_SIZE,
			EXIT_SIZE,
			EXIT_SIZE,
			EXIT_SIZE
		);

		for (int index = 0; index < visibleEntryCount; index++) {
			MenuEntry entry = entries.get(index);
			Bounds bounds = buttonBounds(panelX, panelY, index);
			boolean hovered = bounds.contains(mouseX, mouseY);
			guiGraphics.blit(
				RenderPipelines.GUI_TEXTURED,
				hovered ? entry.highlightedTexture() : entry.texture(),
				bounds.x(),
				bounds.y(),
				0.0F,
				0.0F,
				BUTTON_WIDTH,
				BUTTON_HEIGHT,
				BUTTON_WIDTH,
				BUTTON_HEIGHT
			);
			String label = Component.translatable(entry.labelKey()).getString();
			int labelHeight = Math.round(this.font.lineHeight * BUTTON_TEXT_SCALE);
			int labelY = bounds.y() + BUTTON_ICON_TOP_INSET + (BUTTON_ICON_SLOT_SIZE - labelHeight + 1) / 2;
			drawScaledLeftText(
				guiGraphics,
				label,
				bounds.x() + BUTTON_ICON_LEFT_INSET + BUTTON_ICON_SLOT_SIZE + BUTTON_TEXT_GAP,
				labelY,
				BUTTON_TEXT_SCALE,
				BUTTON_TEXT_COLOR
			);
		}
	}

	private void drawPlayerFace(GuiGraphicsExtractor guiGraphics, AbstractClientPlayer player, int x, int y) {
		Identifier skinTexture = player.getSkin().body().texturePath();
		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().translate(x, y);
		guiGraphics.pose().scale(FACE_SIZE / (float) FACE_SOURCE_SIZE, FACE_SIZE / (float) FACE_SOURCE_SIZE);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, skinTexture, 0, 0, 8.0F, 8.0F, FACE_SOURCE_SIZE, FACE_SOURCE_SIZE, 64, 64);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, skinTexture, 0, 0, 40.0F, 8.0F, FACE_SOURCE_SIZE, FACE_SOURCE_SIZE, 64, 64);
		guiGraphics.pose().popMatrix();
	}

	private Bounds buttonBounds(int panelX, int panelY, int index) {
		int row = index / 2;
		int column = index % 2;
		int x = panelX + BUTTON_LEFT_INSET + column * (BUTTON_WIDTH + BUTTON_COLUMN_GAP);
		int y = panelY + BUTTON_TOP + row * (BUTTON_HEIGHT + BUTTON_ROW_GAP);
		return new Bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
	}

	private Identifier backgroundTexture(int entryCount) {
		return entryCount <= 2
			? BACKGROUND_SMALL_TEXTURE
			: entryCount <= 4 ? BACKGROUND_MEDIUM_TEXTURE : BACKGROUND_LARGE_TEXTURE;
	}

	private int panelHeight(int entryCount) {
		return entryCount <= 2
			? PANEL_HEIGHT_SMALL
			: entryCount <= 4 ? PANEL_HEIGHT_MEDIUM : PANEL_HEIGHT_LARGE;
	}

	private int panelX() { return (this.width - PANEL_WIDTH) / 2; }
	private int panelY(int entryCount) { return (this.height - panelHeight(entryCount)) / 2; }

	private int headerNameY() {
		return HEADER_SEPARATOR_Y - HEADER_TEXT_GAP - scaledTextHeight(HEADER_TEXT_SCALE);
	}

	private int headerLevelY() {
		return HEADER_SEPARATOR_Y + HEADER_SEPARATOR_HEIGHT + HEADER_TEXT_GAP;
	}

	private int scaledTextHeight(float scale) {
		return Math.round(this.font.lineHeight * scale);
	}

	private void drawScaledCenteredText(GuiGraphicsExtractor guiGraphics, String text, int centerX, int y, float scale, int color) {
		if (text == null || text.isEmpty()) return;
		int startX = Math.round(centerX - (this.font.width(text) * scale / 2.0F));
		drawScaledLeftText(guiGraphics, text, startX, y, scale, color);
	}

	private void drawScaledLeftText(GuiGraphicsExtractor guiGraphics, String text, int x, int y, float scale, int color) {
		if (text == null || text.isEmpty()) return;
		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().scale(scale, scale);
		guiGraphics.text(this.font, text, Math.round(x / scale), Math.round(y / scale), color, false);
		guiGraphics.pose().popMatrix();
	}

	private static boolean contains(int x, int y, int width, int height, double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private static Identifier texture(String path) {
		return Identifier.fromNamespaceAndPath("madoku-craft", "textures/" + path);
	}

	private record Bounds(int x, int y, int width, int height) {
		private boolean contains(double mouseX, double mouseY) {
			return MenuScreen.contains(x, y, width, height, mouseX, mouseY);
		}
	}
}
