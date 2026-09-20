package madoku.craft.java.core.menu;

/** Client bootstrap for the Core menu provider. */
public final class MadokuMenuClient {
	private static boolean initialized;

	private MadokuMenuClient() { }

	public static void initialize() {
		if (initialized) return;
		LevelsMenuClient.initialize();
		MenuAPIManager.registerProvider(new MadokuMenuProvider());
		MenuAPIManager.initializeClient();
		initialized = true;
	}
}
