package madoku.craft.java.core.menu;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/** Temporary destination for menu entries whose feature screens are not implemented yet. */
public final class MenuPlaceholderScreen extends Screen {
	public MenuPlaceholderScreen(Component title) {
		super(title == null ? Component.translatable("menu.madoku-craft.title") : title);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	protected void init() {
		this.addRenderableOnly((guiGraphics, mouseX, mouseY, partialTick) -> {
			guiGraphics.fill(RenderPipelines.GUI, 0, 0, this.width, this.height, 0x88000000);
			guiGraphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
			guiGraphics.centeredText(this.font, Component.translatable("menu.madoku-craft.not_configured"), this.width / 2, this.height / 2 + 5, 0xFFCCCCCC);
		});
	}
}
