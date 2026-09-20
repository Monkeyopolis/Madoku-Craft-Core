package madoku.craft.java.core.menu;

/** Shared client-open contract used by feature modules without exposing Core client classes. */
public final class LevelsMenuClientAPIManager {
	private static final Runnable UNAVAILABLE_OPENER = () -> { };
	private static volatile Runnable opener = UNAVAILABLE_OPENER;

	private LevelsMenuClientAPIManager() { }

	public static void registerOpener(Runnable candidate) {
		if (candidate == null) throw new IllegalArgumentException("Levels menu opener must not be null.");
		opener = candidate;
	}

	public static void unregisterOpener() {
		opener = UNAVAILABLE_OPENER;
	}

	public static void open() {
		opener.run();
	}
}
