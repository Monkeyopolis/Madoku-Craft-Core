package madoku.craft.java.core.menu;

import net.minecraft.client.Minecraft;

import java.util.List;

/** Provider contract for the client-side Core menu subsystem. */
public interface MenuProvider {
	default void initializeClient() { }
	default void resetClient() { }
	default void open(Minecraft client) { }
	default List<MenuEntry> entries() { return List.of(); }
	default void registerEntry(MenuEntry entry) { }
	default void unregisterEntry(String id) { }
	default void openEntry(String id, Minecraft client) { }
}
