package madoku.craft.java;

import madoku.craft.java.core.MadokuCoreManager;
import madoku.craft.java.core.season.SeasonAPIManager;
import madoku.craft.java.core.time.TimeAPIManager;
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
		EntitySleepEvents.ALLOW_RESETTING_TIME.register(TimeAPIManager::shouldAllowResettingTime);

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			MadokuCoreManager.reset();
			MadokuCoreManager.loadPersistedData(server);
			MadokuCoreManager.onServerStarted(server);
			TimeAPIManager.broadcastWorldTimeNow(server);
			SeasonAPIManager.broadcastWorldSeasonNow(server);
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(MadokuCoreManager::onServerStopping);

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> MadokuCoreManager.reset());

		ServerTickEvents.START_SERVER_TICK.register(TimeAPIManager::refreshSleepTickIncrement);
		ServerTickEvents.START_SERVER_TICK.register(SeasonAPIManager::onServerStartTick);

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			long tickIncrement = TimeAPIManager.getCachedSleepTickIncrement();
			TimeAPIManager.advance(server, tickIncrement);
			TimeAPIManager.update(server);
			MadokuCoreManager.onServerTick(server);
			MadokuCoreManager.autosavePersistedData(server);
			SeasonAPIManager.onServerTick(server);
			if (MadokuCoreManager.shouldRunWorldSync(server)) {
				TimeAPIManager.broadcastWorldTimeIfChanged(server);
				SeasonAPIManager.broadcastWorldSeasonIfChanged(server);
				SeasonAPIManager.syncPlayerClimateIfChanged(server);
			}
		});
	}
}
