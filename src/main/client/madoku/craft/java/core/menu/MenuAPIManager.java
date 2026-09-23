package madoku.craft.java.core.menu;

import net.minecraft.client.Minecraft;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Public API boundary for the client-side Core menu subsystem. */
public final class MenuAPIManager {
	private static final MenuProvider UNAVAILABLE_PROVIDER = new MenuProvider() { };
	private static final Map<String, MenuEntry> PENDING_ENTRIES = new LinkedHashMap<>();
	private static volatile MenuProvider provider = UNAVAILABLE_PROVIDER;

	private MenuAPIManager() { }

	public static void registerProvider(MenuProvider candidate) {
		if (candidate == null) throw new IllegalArgumentException("Menu provider must not be null.");
		provider = candidate;
		for (MenuEntry entry : PENDING_ENTRIES.values()) candidate.registerEntry(entry);
		PENDING_ENTRIES.clear();
	}

	public static void unregisterProvider() {
		provider = UNAVAILABLE_PROVIDER;
	}

	public static void initializeClient() { provider.initializeClient(); }
	public static void resetClient() { provider.resetClient(); }
	public static void open(Minecraft client) { provider.open(client); }
	public static List<MenuEntry> entries() { return provider.entries(); }
	public static void registerEntry(MenuEntry entry) {
		if (entry == null) throw new IllegalArgumentException("Menu entry must not be null.");
		if (provider == UNAVAILABLE_PROVIDER) PENDING_ENTRIES.put(entry.id(), entry);
		else provider.registerEntry(entry);
	}
	public static void unregisterEntry(String id) {
		if (id == null) return;
		PENDING_ENTRIES.remove(id);
		provider.unregisterEntry(id);
	}
	public static void openEntry(String id, Minecraft client) { provider.openEntry(id, client); }
}
