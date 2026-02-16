package madoku.craft.API.system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import madoku.craft.API.MadokuCraftAPI;

import net.fabricmc.loader.api.FabricLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Centralized JSON file manager for Madoku Craft API integrations. */
public final class MadokuJSONSystem {
	public static final String SYSTEM_NAME = "Madoku-JSON-System";

	private static final Logger LOGGER = LoggerFactory.getLogger(MadokuCraftAPI.MOD_ID);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String ROOT_FOLDER = "madoku-craft";
	private static final String VERSION_FIELD = "version";
	private static final String DEFAULT_VERSION = "0.0.0";

	private MadokuJSONSystem() {
	}

	/**
	 * Loads a JSON file from config/madoku-craft/{folderPath}/{fileName}.json.
	 * The file keeps user values, adds missing defaults, removes unknown keys, and syncs version.
	 */
	public static ManagedJSON load(String folderPath, String fileName, JsonObject defaults) {
		Path path = resolvePath(folderPath, fileName);
		MadokuMigrationSystem.migrateJsonIfNeeded(path, folderPath, fileName);
		createParent(path);

		JsonObject defaultTemplate = deepCopy(defaults);
		JsonObject root = readJson(path);
		boolean dirty = false;

		if (root == null) {
			root = deepCopy(defaultTemplate);
			dirty = true;
			LOGGER.info("Creating {} file at {}", SYSTEM_NAME, path);
		}

		dirty |= reconcileStructure(root, defaultTemplate);

		String apiVersion = getApiVersion();
		if (!apiVersion.equals(obtainVersion(root))) {
			root.addProperty(VERSION_FIELD, apiVersion);
			dirty = true;
		}

		if (dirty) {
			writeJson(path, root);
		}

		return new ManagedJSON(path, root, defaultTemplate);
	}

	public static Path getRootDirectory() {
		return FabricLoader.getInstance().getConfigDir().resolve(ROOT_FOLDER);
	}

	private static JsonObject readJson(Path path) {
		if (!Files.exists(path)) {
			return null;
		}

		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JsonElement parsed = JsonParser.parseReader(reader);
			if (parsed.isJsonObject()) {
				return parsed.getAsJsonObject();
			}
			LOGGER.warn("{} root at {} is not a JSON object.", SYSTEM_NAME, path);
		} catch (IOException | JsonParseException exc) {
			LOGGER.warn("Unable to parse {} file {}: {}", SYSTEM_NAME, path, exc.getMessage());
		}

		return null;
	}

	private static void writeJson(Path path, JsonObject json) {
		try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			GSON.toJson(json, writer);
		} catch (IOException exc) {
			throw new UncheckedIOException("Unable to write " + SYSTEM_NAME + " file " + path, exc);
		}
	}

	private static void createParent(Path path) {
		try {
			Files.createDirectories(path.getParent());
		} catch (IOException exc) {
			throw new UncheckedIOException("Unable to create " + SYSTEM_NAME + " directory for " + path, exc);
		}
	}

	private static JsonObject deepCopy(JsonObject source) {
		return source == null ? new JsonObject() : source.deepCopy();
	}

	private static boolean reconcileStructure(JsonObject target, JsonObject defaults) {
		boolean dirty = false;
		if (defaults == null) {
			return false;
		}

		List<String> keysToRemove = new ArrayList<>();
		for (Map.Entry<String, JsonElement> entry : target.entrySet()) {
			String key = entry.getKey();
			if (VERSION_FIELD.equals(key)) {
				continue;
			}
			if (!defaults.has(key)) {
				keysToRemove.add(key);
			}
		}

		for (String key : keysToRemove) {
			target.remove(key);
			dirty = true;
		}

		for (Map.Entry<String, JsonElement> entry : defaults.entrySet()) {
			String key = entry.getKey();
			JsonElement defaultValue = entry.getValue();
			if (!target.has(key)) {
				target.add(key, defaultValue.deepCopy());
				dirty = true;
				continue;
			}
			JsonElement targetValue = target.get(key);
			if (targetValue.isJsonObject() && defaultValue.isJsonObject()) {
				dirty |= reconcileStructure(targetValue.getAsJsonObject(), defaultValue.getAsJsonObject());
			}
		}

		return dirty;
	}

	private static String obtainVersion(JsonObject json) {
		JsonElement versionElement = json.get(VERSION_FIELD);
		if (versionElement != null && versionElement.isJsonPrimitive() && versionElement.getAsJsonPrimitive().isString()) {
			return versionElement.getAsString();
		}
		return DEFAULT_VERSION;
	}

	private static String getApiVersion() {
		return FabricLoader.getInstance()
			.getModContainer(MadokuCraftAPI.MOD_ID)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse(DEFAULT_VERSION);
	}

	private static Path resolvePath(String folderPath, String fileName) {
		Path folder = resolveFolderPath(folderPath);
		String sanitizedFile = sanitizeFileName(fileName);
		return folder.resolve(sanitizedFile + ".json");
	}

	private static Path resolveFolderPath(String folderPath) {
		Path resolved = getRootDirectory();
		List<String> segments = MadokuNamingSystem.scopedPathSegments(folderPath);
		for (String segment : segments) {
			resolved = resolved.resolve(segment);
		}
		return resolved;
	}

	private static String sanitizeFileName(String fileName) {
		return MadokuNamingSystem.jsonFileBaseName(fileName);
	}

	public static final class ManagedJSON {
		private Path path;
		private JsonObject root;
		private final JsonObject defaults;

		private ManagedJSON(Path path, JsonObject root, JsonObject defaults) {
			this.path = path;
			this.root = root;
			this.defaults = defaults;
		}

		public Path getPath() {
			return path;
		}

		public JsonObject getRoot() {
			return root;
		}

		/** Persists mutations while keeping defaults and version synchronized. */
		public void save() {
			reconcileStructure(root, defaults);
			root.addProperty(VERSION_FIELD, getApiVersion());
			writeJson(path, root);
			LOGGER.info("{} saved {}", SYSTEM_NAME, path);
		}
	}
}
