package madoku.craft.java.core.module;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** Installs the standard server lifecycle for a standalone Madoku module. */
public final class MadokuStandaloneRuntime {
	private static final java.util.Set<net.minecraft.resources.Identifier> REGISTERED_MODULES = new java.util.HashSet<>();

	private MadokuStandaloneRuntime() {
	}

	public static void initialize(MadokuStandaloneModule module) {
		if (module == null) {
			throw new IllegalArgumentException("Standalone module must not be null.");
		}

		MadokuModuleLifecycleManager lifecycle = MadokuModuleLifecycleManager.server();
		synchronized (REGISTERED_MODULES) {
			if (!REGISTERED_MODULES.add(module.id())) return;
		}
		lifecycle.register(module);
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			if (!lifecycle.isRunning(module.id())) return;
			lifecycle.reset(module.id());
			module.loadPersistedData(server);
			module.onServerStarted(server);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			if (lifecycle.isRunning(module.id())) module.onServerStopping(server);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			if (!lifecycle.isRunning(module.id())) return;
			module.onServerStopped(server);
			lifecycle.reset(module.id());
		});
		ServerTickEvents.START_SERVER_TICK.register(server -> {
			if (lifecycle.isRunning(module.id())) module.onServerStartTick(server);
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (!lifecycle.isRunning(module.id())) return;
			module.onServerTick(server);
			module.autosavePersistedData(server);
		});
	}
}
