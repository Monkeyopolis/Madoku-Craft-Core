package madoku.craft.API;

import madoku.craft.clock.MadokuClock;
import madoku.craft.clock.MadokuGameplayClock;
import madoku.craft.config.StaticJsonSystem;
import madoku.craft.debug.MadokuDebug;
import madoku.craft.scheduler.MadokuScheduler;
import madoku.craft.time.MadokuSleep;
import madoku.craft.time.MadokuTime;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class MadokuCraftAPI implements ModInitializer {
	public static final String MOD_ID = "madoku-craft-api";

	@Override
	public void onInitialize() {
		StaticJsonSystem.initialize();
		MadokuDebug.initialize();
		MadokuTime.initialize();
		EntitySleepEvents.ALLOW_RESETTING_TIME.register(player -> !MadokuTime.isEnabled());

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			MadokuDebug.resetSession();
			MadokuClock.reset();
			MadokuGameplayClock.reset();
			MadokuSleep.reset();
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
			MadokuSleep.reset();
			MadokuTime.reset();
			MadokuScheduler.reset();
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			MadokuGameplayClock.tick();
			long tickIncrement = MadokuSleep.getTickIncrement(server);
			MadokuScheduler.tick(server);
			MadokuScheduler.autosavePersistedData(server);
			MadokuClock.tick(tickIncrement);
			MadokuTime.autosavePersistedData(server);
			MadokuTime.update(server);
		});
	}
}
