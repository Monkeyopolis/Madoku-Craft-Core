package madoku.craft.api;

import madoku.craft.api.season.MadokuSeasonManager;
import madoku.craft.api.time.MadokuTimeManager;
import madoku.craft.api.time.TimeSleepManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class MadokuCraftAPI implements ModInitializer {
	public static final String MOD_ID = "madoku-craft-api";
	public static final String SHARED_NAMESPACE = "madoku-craft";

	@Override
	public void onInitialize() {
		MadokuAPIManager.initialize();
		EntitySleepEvents.ALLOW_RESETTING_TIME.register(TimeSleepManager::shouldAllowResettingTime);

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			MadokuAPIManager.reset();
			MadokuAPIManager.loadPersistedData(server);
			MadokuAPIManager.onServerStarted(server);
			MadokuTimeManager.broadcastWorldTimeNow(server);
			MadokuSeasonManager.broadcastWorldSeasonNow(server);
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(MadokuAPIManager::onServerStopping);

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> MadokuAPIManager.reset());

		ServerTickEvents.START_SERVER_TICK.register(TimeSleepManager::refreshTickIncrement);
		ServerTickEvents.START_SERVER_TICK.register(MadokuSeasonManager::onServerStartTick);

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			MadokuTimeManager.advance(server, TimeSleepManager.getCachedTickIncrement());
			MadokuTimeManager.update(server);
			MadokuAPIManager.onServerTick(server);
			MadokuAPIManager.autosavePersistedData(server);
			MadokuSeasonManager.onServerTick(server);
			if (MadokuAPIManager.shouldRunWorldSync(server)) {
				MadokuTimeManager.broadcastWorldTimeIfChanged(server);
				MadokuSeasonManager.broadcastWorldSeasonIfChanged(server);
				MadokuSeasonManager.syncPlayerClimateIfChanged(server);
			}
		});
	}
}
