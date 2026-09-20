package madoku.craft.java.core.menu;

/** Shared API boundary between the Core Levels menu UI and the optional Levels feature module. */
public final class LevelsMenuAPIManager {
	private static final LevelsMenuProvider UNAVAILABLE_PROVIDER = new LevelsMenuProvider() { };
	private static volatile LevelsMenuProvider provider = UNAVAILABLE_PROVIDER;

	private LevelsMenuAPIManager() { }

	public static void registerProvider(LevelsMenuProvider candidate) {
		if (candidate == null) throw new IllegalArgumentException("Levels menu provider must not be null.");
		provider = candidate;
	}

	public static void unregisterProvider() {
		provider = UNAVAILABLE_PROVIDER;
	}

	public static int version() { return provider.version(); }
	public static LevelsMenuProvider.Snapshot snapshot() { return provider.snapshot(); }
	public static void requestUpgrade(String statId) { provider.requestUpgrade(statId); }
}
