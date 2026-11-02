package madoku.craft.java.core.menu;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import madoku.craft.java.core.input.KeybindAPIManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Owns the Core menu keybind, route registry, and screen transitions. */
public final class MenuManager {
	private static final Identifier OPEN_MENU_KEY_ID = Identifier.fromNamespaceAndPath("madoku-craft", "open_menu");
	private static KeyMapping openMenuKey;
	private static final Map<String, MenuEntry> ENTRIES = new LinkedHashMap<>();
	private static boolean initialized;

	private MenuManager() { }

	public static void initialize() {
		if (initialized) return;

		openMenuKey = KeybindAPIManager.register(
			OPEN_MENU_KEY_ID,
			"key.madoku-craft.open_menu",
			InputConstants.KEY_TAB
		);
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
		if (openMenuKey != null && openMenuKey.consumeClick() && client.gui.screen() == null) open(client);
	}

}
