package madoku.craft.java.core.menu;

import net.minecraft.client.Minecraft;

/** Action invoked when a registered menu entry is selected. */
@FunctionalInterface
public interface MenuEntryAction {
	void open(Minecraft client);
}
