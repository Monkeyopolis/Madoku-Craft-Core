package madoku.craft.API;

import madoku.craft.clock.MadokuClock;
import madoku.craft.clock.MadokuTicks;
import madoku.craft.chunk.ChunkManagerSystem;
import madoku.craft.config.JsonManagerSystem;
import madoku.craft.debug.MadokuDebug;
import madoku.craft.loot.system.MadokuLootTableSystem;
import madoku.craft.network.WorldSeasonSync;
import madoku.craft.recipe.system.MadokuRecipe;
import madoku.craft.scheduler.SchedulerManagerSystem;
import madoku.craft.season.MadokuSeason;
import madoku.craft.time.MadokuSleep;
import madoku.craft.time.MadokuTime;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class MadokuCraftAPI implements ModInitializer {
	public static final String MOD_ID = "madoku-craft-api";
	public static final String SHARED_NAMESPACE = "madoku-craft";

	@Override
	public void onInitialize() {
		JsonManagerSystem.initialize();
		ChunkManagerSystem.initialize();
		MadokuRecipe.initialize();
		MadokuLootTableSystem.initialize();
		MadokuDebug.initialize();
		MadokuTime.initialize();
		MadokuSeason.initialize();
		WorldSeasonSync.initialize();
		EntitySleepEvents.ALLOW_RESETTING_TIME.register(player -> !MadokuTime.isEnabled());

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			MadokuDebug.resetSession();
			MadokuTicks.reset();
			MadokuClock.reset();
			MadokuSleep.reset();
			MadokuTime.reset();
			MadokuSeason.reset();
			ChunkManagerSystem.reset();
			SchedulerManagerSystem.reset();
			SchedulerManagerSystem.loadPersistedData(server);
			ChunkManagerSystem.loadPersistedData(server);
			MadokuTime.loadPersistedData(server);
			MadokuSeason.loadPersistedData(server);
			ChunkManagerSystem.onServerStarted(server);
			MadokuSeason.onServerStarted(server);
			MadokuTime.update(server);
			WorldSeasonSync.reset();
			WorldSeasonSync.broadcastNow(server);
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(ChunkManagerSystem::onServerStopping);

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			MadokuTime.savePersistedData(server);
			MadokuSeason.savePersistedData(server);
			ChunkManagerSystem.savePersistedData(server);
			SchedulerManagerSystem.savePersistedData(server);
			MadokuClock.reset();
			MadokuTicks.reset();
			MadokuSleep.reset();
			MadokuTime.reset();
			MadokuSeason.reset();
			ChunkManagerSystem.reset();
			SchedulerManagerSystem.reset();
			WorldSeasonSync.reset();
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			long tickIncrement = MadokuSleep.getTickIncrement(server);
			MadokuTicks.advance(server, tickIncrement);
			MadokuTime.update(server);
			if (MadokuTime.isEnabled()) {
				SchedulerManagerSystem.onClockTick(server);
			} else {
				SchedulerManagerSystem.onServerTick(server);
			}
			SchedulerManagerSystem.autosavePersistedData(server);
			ChunkManagerSystem.autosavePersistedData(server);
			MadokuTime.autosavePersistedData(server);
			MadokuSeason.autosavePersistedData(server);
			MadokuSeason.onServerTick(server);
			WorldSeasonSync.broadcastIfChanged(server);
		});
	}
}
