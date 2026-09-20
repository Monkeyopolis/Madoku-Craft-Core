package madoku.craft.java.core.menu;

import net.minecraft.client.Minecraft;

import java.util.List;

/** Public API boundary for the client-side Core menu subsystem. */
public final class MenuAPIManager {
	private static final MenuProvider UNAVAILABLE_PROVIDER = new MenuProvider() { };
	private static volatile MenuProvider provider = UNAVAILABLE_PROVIDER;

	private MenuAPIManager() { }

	public static void registerProvider(MenuProvider candidate) {
		if (candidate == null) throw new IllegalArgumentException("Menu provider must not be null.");
		provider = candidate;
	}

	public static void unregisterProvider() {
		provider = UNAVAILABLE_PROVIDER;
	}

	public static void initializeClient() { provider.initializeClient(); }
	public static void resetClient() { provider.resetClient(); }
	public static void open(Minecraft client) { provider.open(client); }
	public static List<MenuEntry> entries() { return provider.entries(); }
	public static void registerEntry(MenuEntry entry) { provider.registerEntry(entry); }
	public static void unregisterEntry(String id) { provider.unregisterEntry(id); }
	public static void openEntry(String id, Minecraft client) { provider.openEntry(id, client); }
}
