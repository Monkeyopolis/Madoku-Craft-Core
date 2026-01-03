package madoku.craft.API;

import com.google.gson.JsonObject;

import madoku.craft.API.system.JsonFeatureSystem;
import madoku.craft.API.system.MadokuSavingSystem;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MadokuCraftAPI implements ModInitializer {
	public static final String MOD_ID = "madoku-craft-api";

	public static JsonFeatureSystem.ManagedFeature API_FEATURE;
	public static MadokuSavingSystem.MadokuData API_DATA;

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		JsonObject defaults = buildDefaults();
		API_FEATURE = JsonFeatureSystem.loadFeature("madoku_craft_api", defaults);
		LOGGER.info("JSON feature system ready at {}", API_FEATURE.getPath());

		API_DATA = MadokuSavingSystem.load("madoku_craft_api_data", buildSavingDefaults());
		LOGGER.info("Madoku Data ready at {}", API_DATA.getPath());
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
