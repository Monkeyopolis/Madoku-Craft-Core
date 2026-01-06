package madoku.craft.API.system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonParser;

import madoku.craft.API.MadokuCraftAPI;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Persists feature-specific \"Madoku Data\" files on disk. */
public final class MadokuSavingSystem {
	private static final Logger LOGGER = LoggerFactory.getLogger(MadokuCraftAPI.MOD_ID);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String DATA_LABEL_KEY = "madokuDataLabel";
	private static final String DATA_LABEL_VALUE = "Madoku Data";

	private MadokuSavingSystem() {
	}

	public static MadokuData load(String featureId, JsonObject defaults) {
		return loadForPath(featureId, defaults, null);
	}

	/** Loads global Madoku Data stored in the game directory. */
	public static MadokuData loadGlobal(String featureId, JsonObject defaults) {
		return load(featureId, defaults);
	}

	public static MadokuData loadDeferred(String featureId, JsonObject defaults) {
		Path path = resolvePath(featureId, null);
		JsonObject root = deepCopy(defaults);
		return new MadokuData(featureId, path, root, true);
	}

	public static MadokuData loadForWorld(MinecraftServer server, String featureId, JsonObject defaults) {
		if (server == null) {
			return loadDeferred(featureId, defaults);
		}
		Path worldRoot = server.getSavePath(WorldSavePath.ROOT);
		return loadForPath(featureId, defaults, worldRoot);
	}

	public static void reloadForWorld(MadokuData data, String featureId, JsonObject defaults, MinecraftServer server) {
		if (data == null || server == null) {
			return;
		}
		Path worldRoot = server.getSavePath(WorldSavePath.ROOT);
		if (worldRoot == null) {
			return;
		}
		Path path = resolvePath(featureId, worldRoot);
		JsonObject diskJson = prepareJson(path, defaults, featureId);
		data.replace(path, diskJson);
	}

	private static MadokuData loadForPath(String featureId, JsonObject defaults, Path baseDir) {
		Path path = resolvePath(featureId, baseDir);
		JsonObject diskJson = prepareJson(path, defaults, featureId);
		return new MadokuData(featureId, path, diskJson, false);
	}

	private static JsonObject prepareJson(Path path, JsonObject defaults, String featureId) {
		createParent(path);
		JsonObject diskJson = readJson(path);
		boolean created = diskJson == null;
		if (created) {
			LOGGER.info("Creating Madoku Data file {} at {}", featureId, path);
			diskJson = deepCopy(defaults);
		}

		boolean dirty = mergeDefaults(diskJson, defaults);
		ensureLabel(diskJson);

		if (created || dirty) {
			writeJson(path, diskJson);
		}

		return diskJson;
	}

	private static JsonObject readJson(Path path) {
		if (!Files.exists(path)) {
			return null;
		}

		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JsonElement parsed = JsonParser.parseReader(reader);
			if (parsed != null && parsed.isJsonObject()) {
				return parsed.getAsJsonObject();
			}
			LOGGER.warn("Madoku Data at {} is not an object.", path);
		} catch (IOException | JsonParseException exc) {
			LOGGER.warn("Unable to read Madoku Data at {}: {}", path, exc.getMessage());
		}

		return null;
	}

	private static void writeJson(Path path, JsonObject json) {
		try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			GSON.toJson(json, writer);
		} catch (IOException exc) {
			throw new UncheckedIOException("Unable to write Madoku Data to " + path, exc);
		}
	}

	private static JsonObject deepCopy(JsonObject source) {
		return source == null ? new JsonObject() : source.deepCopy();
	}

	private static boolean mergeDefaults(JsonObject target, JsonObject defaults) {
		boolean added = false;
		if (defaults == null) {
			return added;
		}
		for (Map.Entry<String, JsonElement> entry : defaults.entrySet()) {
			String key = entry.getKey();
			if (target.has(key)) {
				JsonElement targetValue = target.get(key);
				JsonElement defaultValue = entry.getValue();
				if (targetValue.isJsonObject() && defaultValue.isJsonObject()) {
					added |= mergeDefaults(targetValue.getAsJsonObject(), defaultValue.getAsJsonObject());
				}
				continue;
			}
			target.add(key, entry.getValue().deepCopy());
			added = true;
		}
		return added;
	}

	private static void ensureLabel(JsonObject json) {
		JsonElement label = json.get(DATA_LABEL_KEY);
		if (!(label instanceof JsonPrimitive primitive && primitive.isString() && DATA_LABEL_VALUE.equals(primitive.getAsString()))) {
			json.addProperty(DATA_LABEL_KEY, DATA_LABEL_VALUE);
		}
	}

	private static void createParent(Path path) {
		try {
			Files.createDirectories(path.getParent());
		} catch (IOException exc) {
			throw new UncheckedIOException("Unable to prepare Madoku Data directory for " + path, exc);
		}
	}

	private static Path resolvePath(String featureId, Path baseDir) {
		String sanitized = sanitizeFeatureId(featureId);
		Path root = baseDir != null ? baseDir : FabricLoader.getInstance().getGameDir();
		return root.resolve("madoku-data").resolve(sanitized + ".json");
	}

	private static String sanitizeFeatureId(String featureId) {
		String sanitized = featureId == null ? "feature" : featureId;
		sanitized = sanitized.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
		if (sanitized.isEmpty()) {
			sanitized = "feature";
		}
		return sanitized;
	}

	/** Represents an open, mutable Madoku Data file. */
	public static final class MadokuData {
		private final String featureId;
		private Path path;
		private JsonObject root;
		private boolean deferred;

		private MadokuData(String featureId, Path path, JsonObject root, boolean deferred) {
			this.featureId = featureId;
			this.path = path;
			this.root = root;
			this.deferred = deferred;
		}

		public Path getPath() {
			return path;
		}

		public JsonObject getRoot() {
			return root;
		}

		public void save() {
			if (deferred) {
				LOGGER.warn("Skipping Madoku Data save for {} because no world is active yet.", featureId);
				return;
			}
			ensureLabel(root);
			writeJson(path, root);
			LOGGER.info("Madoku Data {} saved to {}", featureId, path);
		}

		private void replace(Path path, JsonObject root) {
			this.path = path;
			this.root = root;
			this.deferred = false;
		}
	}
}
