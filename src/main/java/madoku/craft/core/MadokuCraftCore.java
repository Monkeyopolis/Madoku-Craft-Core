package madoku.craft.core;

import madoku.craft.core.season.MadokuSeasonManager;
import madoku.craft.core.time.MadokuTimeManager;
import madoku.craft.core.time.TimeSleepManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class MadokuCraftCore implements ModInitializer {
	public static final String MOD_ID = "madoku-craft-core";
	public static final String SHARED_NAMESPACE = "madoku-craft";

	@Override
	public void onInitialize() {
		MadokuCoreManager.initialize();
		EntitySleepEvents.ALLOW_RESETTING_TIME.register(TimeSleepManager::shouldAllowResettingTime);

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			MadokuCoreManager.reset();
			MadokuCoreManager.loadPersistedData(server);
			MadokuCoreManager.onServerStarted(server);
			MadokuTimeManager.broadcastWorldTimeNow(server);
			MadokuSeasonManager.broadcastWorldSeasonNow(server);
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(MadokuCoreManager::onServerStopping);

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> MadokuCoreManager.reset());

		ServerTickEvents.START_SERVER_TICK.register(TimeSleepManager::refreshTickIncrement);
		ServerTickEvents.START_SERVER_TICK.register(MadokuSeasonManager::onServerStartTick);

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			MadokuTimeManager.advance(server, TimeSleepManager.getCachedTickIncrement());
			MadokuTimeManager.update(server);
			MadokuCoreManager.onServerTick(server);
			MadokuCoreManager.autosavePersistedData(server);
			MadokuSeasonManager.onServerTick(server);
			if (MadokuCoreManager.shouldRunWorldSync(server)) {
				MadokuTimeManager.broadcastWorldTimeIfChanged(server);
				MadokuSeasonManager.broadcastWorldSeasonIfChanged(server);
				MadokuSeasonManager.syncPlayerClimateIfChanged(server);
			}
		});
	}
}
