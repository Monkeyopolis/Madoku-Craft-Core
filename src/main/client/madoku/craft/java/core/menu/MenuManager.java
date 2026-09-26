package madoku.craft.java.core.menu;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
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

		registerDefaultEntries();
		KeyMappingHelper.registerKeyMapping(OPEN_MENU_KEY);
		ClientTickEvents.END_CLIENT_TICK.register(MenuManager::handleClientTick);
		initialized = true;
	}

	public static void reset() {
		ENTRIES.clear();
		initialized = false;
	}

	public static void open(Minecraft client) {
		if (client == null || client.player == null || client.gui.screen() != null) return;
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

	private static void registerDefaultEntries() {
		registerDefaultEntry(defaultEntry("player_stats", "menu.madoku-craft.player_stats", "player-stats-button", 10));
		registerDefaultEntry(defaultEntry("settings", "menu.madoku-craft.settings", "settings-button", 20));
		registerDefaultEntry(levelsEntry());
		registerDefaultEntry(defaultEntry("items", "menu.madoku-craft.items", "items-button", 40));
		registerDefaultEntry(defaultEntry("pets", "menu.madoku-craft.pets", "pets-button", 50));
	}

	private static void registerDefaultEntry(MenuEntry entry) {
		ENTRIES.putIfAbsent(entry.id(), entry);
	}

	private static MenuEntry levelsEntry() {
		String id = "levels";
		String labelKey = "menu.madoku-craft.levels";
		Identifier texture = Identifier.fromNamespaceAndPath("madoku-craft", "textures/madoku-menu/main-menu/levels-button.png");
		Identifier highlightedTexture = Identifier.fromNamespaceAndPath("madoku-craft", "textures/madoku-menu/main-menu/levels-button-highlighted.png");
		return new MenuEntry(id, labelKey, texture, highlightedTexture, 30, client -> LevelsMenuClientAPIManager.open());
	}

	private static MenuEntry defaultEntry(String id, String labelKey, String textureName, int order) {
		Identifier texture = Identifier.fromNamespaceAndPath(
			"madoku-craft",
			"textures/madoku-menu/main-menu/" + textureName + ".png"
		);
		Identifier highlightedTexture = Identifier.fromNamespaceAndPath(
			"madoku-craft",
			"textures/madoku-menu/main-menu/" + textureName + "-highlighted.png"
		);
		return new MenuEntry(
			id,
			labelKey,
			texture,
			highlightedTexture,
			order,
			client -> client.setScreenAndShow(new MenuPlaceholderScreen(Component.translatable(labelKey)))
		);
	}
}
