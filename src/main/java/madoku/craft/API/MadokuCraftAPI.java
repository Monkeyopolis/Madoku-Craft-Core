package madoku.craft.API;

import com.google.gson.JsonObject;

import madoku.craft.API.system.MadokuDataSystem;
import madoku.craft.API.system.MadokuDeathSystem;
import madoku.craft.API.system.MadokuInfoDebugSystem;
import madoku.craft.API.system.MadokuJSONSystem;
import madoku.craft.API.system.MadokuTickSystem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class MadokuCraftAPI implements ModInitializer {
	public static final String MOD_ID = "madoku-craft-api";
	private static final String API_ID = "API";
	private static final String LOG_SOURCE = "API";

	public static MadokuJSONSystem.ManagedJSON API_JSON;
	public static MadokuDataSystem.MadokuData API_DATA;

	@Override
	public void onInitialize() {
		JsonObject defaults = buildDefaults();
		API_JSON = MadokuJSONSystem.load(API_ID, API_ID, defaults);
		MadokuInfoDebugSystem.info(LOG_SOURCE, "{} ready at {}", MadokuJSONSystem.SYSTEM_NAME, API_JSON.getPath());

		MadokuTickSystem.init();
		MadokuDeathSystem.init();

		API_DATA = MadokuDataSystem.load(API_ID, MadokuDataSystem.StorageScope.WORLD, buildSavingDefaults());
		MadokuInfoDebugSystem.info(LOG_SOURCE, "{} prepared with {} scope.", MadokuDataSystem.SYSTEM_NAME, API_DATA.getScope());

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			MadokuDataSystem.bindWorld(API_DATA, API_ID, buildSavingDefaults(), server);
			MadokuInfoDebugSystem.info(LOG_SOURCE, "{} ready at {}", MadokuDataSystem.SYSTEM_NAME, API_DATA.getPath());
		});
	}

	private static JsonObject buildDefaults() {
		JsonObject defaults = new JsonObject();
		defaults.addProperty("apiEnabled", true);

		JsonObject network = new JsonObject();
		network.addProperty("timeoutTicks", 1200);
		network.addProperty("retryLimit", 3);
		defaults.add("network", network);

		JsonObject statistics = new JsonObject();
		statistics.addProperty("lastSync", 0L);
		statistics.addProperty("requests", 0);
		defaults.add("statistics", statistics);

		defaults.add("infoDebug", MadokuInfoDebugSystem.buildDefaults());

		return defaults;
	}

	private static JsonObject buildSavingDefaults() {
		JsonObject defaults = new JsonObject();
		defaults.addProperty("lastSyncTick", 0L);
		defaults.addProperty("pendingUpdates", 0);

		JsonObject usage = new JsonObject();
		usage.addProperty("totalRequests", 0);
		usage.addProperty("successfulRequests", 0);
		defaults.add("usage", usage);

		return defaults;
	}
}
