package madoku.craft.API;

import com.google.gson.JsonObject;

import madoku.craft.API.system.MadokuDataSystem;
import madoku.craft.API.system.MadokuDeathSystem;
import madoku.craft.API.system.MadokuJSONSystem;
import madoku.craft.API.system.MadokuNamingSystem;
import madoku.craft.API.system.MadokuTickSystem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MadokuCraftAPI implements ModInitializer {
	public static final String MOD_ID = "madoku-craft-api";

	public static MadokuJSONSystem.ManagedJSON API_JSON;
	public static MadokuDataSystem.MadokuData API_DATA;

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		MadokuTickSystem.init();
		MadokuDeathSystem.init();

		JsonObject defaults = buildDefaults();
		String systemsFolder = MadokuNamingSystem.scopedName("systems");
		String apiFile = MadokuNamingSystem.scopedName("API");
		API_JSON = MadokuJSONSystem.load(systemsFolder, apiFile, defaults);
		LOGGER.info("{} ready at {}", MadokuJSONSystem.SYSTEM_NAME, API_JSON.getPath());

		String apiDataName = MadokuNamingSystem.scopedName("API");
		API_DATA = MadokuDataSystem.load(apiDataName, MadokuDataSystem.StorageScope.WORLD, buildSavingDefaults());
		LOGGER.info("{} prepared with {} scope.", MadokuDataSystem.SYSTEM_NAME, API_DATA.getScope());

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			MadokuDataSystem.bindWorld(API_DATA, apiDataName, buildSavingDefaults(), server);
			LOGGER.info("{} ready at {}", MadokuDataSystem.SYSTEM_NAME, API_DATA.getPath());
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
