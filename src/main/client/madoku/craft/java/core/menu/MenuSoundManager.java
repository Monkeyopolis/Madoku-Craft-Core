package madoku.craft.java.core.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/** Shared client-side sound feedback for Madoku menu controls. */
public final class MenuSoundManager {
	private MenuSoundManager() { }

	public static void playButtonClick() {
		Minecraft client = Minecraft.getInstance();
		client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}
}
