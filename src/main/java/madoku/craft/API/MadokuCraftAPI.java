package madoku.craft.API;

import madoku.craft.clock.MadokuClock;
import madoku.craft.clock.MadokuTicks;
import madoku.craft.config.StaticJsonSystem;
import madoku.craft.debug.MadokuDebug;
import madoku.craft.network.WorldSeasonSync;
import madoku.craft.scheduler.MadokuScheduler;
import madoku.craft.season.MadokuSeason;
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
		MadokuSeason.initialize();
		WorldSeasonSync.initialize();
		EntitySleepEvents.ALLOW_RESETTING_TIME.register(player -> !MadokuTime.isEnabled());

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			MadokuDebug.resetSession();
			MadokuClock.reset();
			MadokuSleep.reset();
			MadokuTime.reset();
			MadokuSeason.reset();
			MadokuScheduler.reset();
			MadokuTime.loadPersistedData(server);
			MadokuSeason.loadPersistedData(server);
			MadokuScheduler.loadPersistedData(server);
			MadokuSeason.onServerStarted(server);
			MadokuTime.update(server);
			WorldSeasonSync.reset();
			WorldSeasonSync.broadcastNow(server);
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			MadokuTime.savePersistedData(server);
			MadokuSeason.savePersistedData(server);
			MadokuScheduler.savePersistedData(server);
			MadokuClock.reset();
			MadokuSleep.reset();
			MadokuTime.reset();
			MadokuSeason.reset();
			MadokuScheduler.reset();
			WorldSeasonSync.reset();
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			long tickIncrement = MadokuSleep.getTickIncrement(server);
			MadokuTicks.advance(server, tickIncrement);
			MadokuScheduler.autosavePersistedData(server);
			MadokuTime.autosavePersistedData(server);
			MadokuSeason.autosavePersistedData(server);
			MadokuTime.update(server);
			MadokuSeason.onServerTick(server);
			WorldSeasonSync.broadcastIfChanged(server);
		});
	}
}
