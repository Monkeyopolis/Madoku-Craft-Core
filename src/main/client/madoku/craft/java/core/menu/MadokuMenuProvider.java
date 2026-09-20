package madoku.craft.java.core.menu;

import net.minecraft.client.Minecraft;

import java.util.List;

/** Built-in Core provider for the menu API. */
public final class MadokuMenuProvider implements MenuProvider {
	@Override public void initializeClient() { MenuManager.initialize(); }
	@Override public void resetClient() { MenuManager.reset(); }
	@Override public void open(Minecraft client) { MenuManager.open(client); }
	@Override public List<MenuEntry> entries() { return MenuManager.entries(); }
	@Override public void registerEntry(MenuEntry entry) { MenuManager.registerEntry(entry); }
	@Override public void unregisterEntry(String id) { MenuManager.unregisterEntry(id); }
	@Override public void openEntry(String id, Minecraft client) { MenuManager.openEntry(id, client); }
}
