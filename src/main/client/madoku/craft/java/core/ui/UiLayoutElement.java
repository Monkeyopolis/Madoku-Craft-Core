package madoku.craft.java.core.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** A client-side UI/HUD element that can participate in the shared layout editor. */
public interface UiLayoutElement {
	Identifier id();

	int defaultX();

	int defaultY();

	default int defaultX(Minecraft client) {
		return defaultX();
	}

	default int defaultY(Minecraft client) {
		return defaultY();
	}

	Bounds bounds(Minecraft client, int x, int y);

	/** Whether this element's owning system is enabled and may be edited. */
	default boolean isEnabled(Minecraft client) {
		return true;
	}

	default boolean isVisible(Minecraft client) {
		return true;
	}

	/** Renders the element while edit mode is active. */
	default void renderEditor(GuiGraphicsExtractor context, Minecraft client, int x, int y) { }

	default String displayName() {
		return id().toString();
	}

	record Bounds(int x, int y, int width, int height) {
		public boolean contains(double mouseX, double mouseY) {
			return mouseX >= x && mouseX < x + Math.max(0, width)
				&& mouseY >= y && mouseY < y + Math.max(0, height);
		}
	}
}
