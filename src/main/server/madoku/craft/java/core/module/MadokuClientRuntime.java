package madoku.craft.java.core.module;

/** Registers a client module with the shared client-side lifecycle manager. */
public final class MadokuClientRuntime {
	private MadokuClientRuntime() {
	}

	public static void initialize(MadokuModule module) {
		MadokuModuleLifecycleManager.client().register(module);
	}
}
