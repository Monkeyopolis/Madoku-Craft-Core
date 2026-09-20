package madoku.craft.java.core.menu;

import net.minecraft.client.Minecraft;

/** Registers Core's client-side opener for the shared Levels menu contract. */
public final class LevelsMenuClient {
	private static boolean initialized;

	private LevelsMenuClient() { }

	public static void initialize() {
		if (initialized) return;
		LevelsMenuClientAPIManager.registerOpener(() -> {
			Minecraft client = Minecraft.getInstance();
			if (client.player != null) client.setScreenAndShow(new LevelsMenuScreen());
		});
		initialized = true;
	}
}
