package madoku.craft.API;

import madoku.craft.clock.MadokuClock;
import madoku.craft.clock.MadokuGameplayClock;
import madoku.craft.config.StaticJsonSystem;
import madoku.craft.debug.MadokuDebug;
import madoku.craft.scheduler.MadokuScheduler;
import madoku.craft.time.MadokuTime;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class MadokuCraftAPI implements ModInitializer {
	public static final String MOD_ID = "madoku-craft-api";

	@Override
	public void onInitialize() {
		StaticJsonSystem.initialize();
		MadokuDebug.initialize();
		MadokuTime.initialize();

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			MadokuDebug.resetSession();
			MadokuClock.reset();
			MadokuGameplayClock.reset();
			MadokuTime.reset();
			MadokuScheduler.reset();
			MadokuTime.loadPersistedData(server);
			MadokuScheduler.loadPersistedData(server);
			MadokuTime.update(server);
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			MadokuTime.savePersistedData(server);
			MadokuScheduler.savePersistedData(server);
			MadokuClock.reset();
			MadokuGameplayClock.reset();
			MadokuTime.reset();
			MadokuScheduler.reset();
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			MadokuGameplayClock.tick();
			MadokuScheduler.tick(server);
			MadokuScheduler.autosavePersistedData(server);
			MadokuClock.tick();
			MadokuTime.autosavePersistedData(server);
			MadokuTime.update(server);
		});
	}
}