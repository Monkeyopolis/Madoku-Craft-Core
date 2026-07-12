package madoku.craft.api;

import madoku.craft.api.season.MadokuSeasonManager;
import madoku.craft.api.time.MadokuTimeManager;
import madoku.craft.api.time.SleepManager;
import madoku.craft.loot.system.MadokuLootTableManager;
import madoku.craft.network.WorldSeasonSync;
import madoku.craft.recipe.system.MadokuRecipe;
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
		MadokuRecipe.initialize();
		MadokuLootTableManager.initialize();
		WorldSeasonSync.initialize();
		EntitySleepEvents.ALLOW_RESETTING_TIME.register(SleepManager::shouldAllowResettingTime);

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			WorldSeasonSync.reset();
			MadokuAPIManager.reset();
			MadokuAPIManager.loadPersistedData(server);
			MadokuAPIManager.onServerStarted(server);
			WorldSeasonSync.broadcastNow(server);
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(MadokuAPIManager::onServerStopping);

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			MadokuAPIManager.reset();
			WorldSeasonSync.reset();
		});

		ServerTickEvents.START_SERVER_TICK.register(SleepManager::refreshTickIncrement);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			MadokuTimeManager.advance(server, SleepManager.getCachedTickIncrement());
			MadokuTimeManager.update(server);
			MadokuAPIManager.onServerTick(server);
			MadokuAPIManager.autosavePersistedData(server);
			MadokuSeasonManager.onServerTick(server);
			WorldSeasonSync.broadcastIfChanged(server);
		});
	}
}
