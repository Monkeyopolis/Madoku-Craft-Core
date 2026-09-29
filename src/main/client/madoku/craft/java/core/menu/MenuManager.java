package madoku.craft.java.core.menu;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Owns the Core menu keybind, route registry, and screen transitions. */
public final class MenuManager {
	private static final Identifier MENU_CATEGORY_ID = Identifier.fromNamespaceAndPath("madoku-craft", "menu");
	private static final KeyMapping.Category MENU_CATEGORY = KeyMapping.Category.register(MENU_CATEGORY_ID);
	private static final KeyMapping OPEN_MENU_KEY = new KeyMapping(
		"key.madoku-craft.open_menu",
		InputConstants.KEY_TAB,
		MENU_CATEGORY
	);
	private static final Map<String, MenuEntry> ENTRIES = new LinkedHashMap<>();
	private static boolean initialized;

	private MenuManager() { }

	public static void initialize() {
		if (initialized) return;

		KeyMappingHelper.registerKeyMapping(OPEN_MENU_KEY);
		ClientTickEvents.END_CLIENT_TICK.register(MenuManager::handleClientTick);
		initialized = true;
	}

	public static void reset() {
		ENTRIES.clear();
		initialized = false;
	}

	public static void open(Minecraft client) {
		if (client == null || client.player == null || client.gui.screen() != null || ENTRIES.isEmpty()) return;
		client.setScreenAndShow(new MenuScreen());
	}

	public static List<MenuEntry> entries() {
		return ENTRIES.values().stream()
			.sorted(Comparator.comparingInt(MenuEntry::order).thenComparing(MenuEntry::id))
			.toList();
	}

	public static void registerEntry(MenuEntry entry) {
		if (entry == null) throw new IllegalArgumentException("Menu entry must not be null.");
		ENTRIES.put(entry.id(), entry);
	}

	public static void unregisterEntry(String id) {
		if (id != null) ENTRIES.remove(id);
	}

	public static void openEntry(String id, Minecraft client) {
		if (id == null || client == null) return;
		MenuEntry entry = ENTRIES.get(id);
		if (entry != null && entry.action() != null) entry.action().open(client);
	}

	private static void handleClientTick(Minecraft client) {
		if (client == null || client.player == null) return;
		if (OPEN_MENU_KEY.consumeClick() && client.gui.screen() == null) open(client);
	}

}
