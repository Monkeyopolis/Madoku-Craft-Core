package madoku.craft.API.system;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import madoku.craft.API.MadokuCraftAPI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized INFO-debug gate shared by Madoku systems and integrations.
 * Controlled by API JSON: infoDebug.enabled.
 */
public final class MadokuInfoDebugSystem {
	private static final Logger LOGGER = LoggerFactory.getLogger(MadokuCraftAPI.MOD_ID);
	private static final String ROOT_KEY = "infoDebug";
	private static final String ENABLED_KEY = "enabled";
	private static final String DEFAULT_SOURCE = "madoku-craft";

	private MadokuInfoDebugSystem() {
	}

	public static JsonObject buildDefaults() {
		JsonObject defaults = new JsonObject();
		defaults.addProperty(ENABLED_KEY, false);
		return defaults;
	}

	public static boolean isEnabled() {
		MadokuJSONSystem.ManagedJSON apiJson = MadokuCraftAPI.API_JSON;
		if (apiJson == null) {
			return false;
		}
		JsonObject root = apiJson.getRoot();
		if (root == null) {
			return false;
		}

		JsonElement systemElement = root.get(ROOT_KEY);
		if (systemElement == null || !systemElement.isJsonObject()) {
			return false;
		}
		return readBoolean(systemElement.getAsJsonObject(), ENABLED_KEY, false);
	}

	public static void info(String source, String message, Object... args) {
		info(LOGGER, source, message, args);
	}

	public static void info(Logger logger, String source, String message, Object... args) {
		if (!isEnabled()) {
			return;
		}
		Logger targetLogger = logger == null ? LOGGER : logger;
		String template = "[{}] " + (message == null ? "" : message);
		targetLogger.info(template, prependSource(normalizeSource(source), args));
	}

	private static Object[] prependSource(String source, Object[] args) {
		Object[] safeArgs = args == null ? new Object[0] : args;
		Object[] merged = new Object[safeArgs.length + 1];
		merged[0] = source;
		System.arraycopy(safeArgs, 0, merged, 1, safeArgs.length);
		return merged;
	}

	private static String normalizeSource(String source) {
		if (source == null || source.isBlank()) {
			return DEFAULT_SOURCE;
		}
		return source.trim();
	}

	private static boolean readBoolean(JsonObject root, String key, boolean fallback) {
		if (root == null || key == null || key.isBlank()) {
			return fallback;
		}
		JsonElement element = root.get(key);
		if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
			return element.getAsBoolean();
		}
		return fallback;
	}
}
