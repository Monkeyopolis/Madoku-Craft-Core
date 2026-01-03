package madoku.craft.API.system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import madoku.craft.API.MadokuCraftAPI;

import net.fabricmc.loader.api.FabricLoader;

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

/** Provides a centralized system for creating and maintaining feature JSON files. */
public final class JsonFeatureSystem {
	private static final Logger LOGGER = LoggerFactory.getLogger(MadokuCraftAPI.MOD_ID);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String VERSION_FIELD = "version";
	private static final String DEFAULT_INITIAL_VERSION = "0.0.0";

	private JsonFeatureSystem() {
	}

	/** Loads or creates a feature file, keeping it synchronized with the MOD version and defaults. */
	public static ManagedFeature loadFeature(String featureId, JsonObject defaults) {
		Path featurePath = buildPath(featureId);
		try {
			Files.createDirectories(featurePath.getParent());
		} catch (IOException exc) {
			throw new UncheckedIOException("Unable to prepare config path for " + featureId, exc);
		}

		JsonObject defaultTemplate = deepCopy(defaults);
		JsonObject diskJson = readJson(featurePath);
		boolean existed = diskJson != null;
		boolean dirty = !existed;

		if (!existed) {
			LOGGER.info("Creating JSON feature file {} at {}", featureId, featurePath);
			diskJson = deepCopy(defaultTemplate);
		} else {
			dirty |= mergeDefaults(diskJson, defaultTemplate);
		}

		String storedVersion = obtainVersion(diskJson);
		String modVersion = getModVersion();
		Version currentVersion = Version.parse(storedVersion);
		Version targetVersion = Version.parse(modVersion);

		if (existed && isIncompatible(currentVersion, targetVersion)) {
			LOGGER.warn("Replacing incompatible JSON for {} ({} vs {}).", featureId, storedVersion, modVersion);
			diskJson = deepCopy(defaultTemplate);
			dirty = true;
		}

		dirty |= mergeDefaults(diskJson, defaultTemplate);

		if (!modVersion.equals(obtainVersion(diskJson))) {
			diskJson.addProperty(VERSION_FIELD, modVersion);
			dirty = true;
		}

		if (dirty) {
			writeJson(featurePath, diskJson);
		}

		return new ManagedFeature(featureId, featurePath, diskJson, deepCopy(defaultTemplate), modVersion);
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
			LOGGER.warn("JSON root at {} is not an object.", path);
		} catch (IOException | JsonParseException exc) {
			LOGGER.warn("Unable to parse JSON at {}: {}", path, exc.getMessage());
		}

		return null;
	}

	private static void writeJson(Path path, JsonObject json) {
		try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			GSON.toJson(json, writer);
		} catch (IOException exc) {
			throw new UncheckedIOException("Unable to write JSON to " + path, exc);
		}
	}

	private static JsonObject deepCopy(JsonObject source) {
		return source == null ? new JsonObject() : source.deepCopy();
	}

	private static String obtainVersion(JsonObject json) {
		JsonElement versionElement = json.get(VERSION_FIELD);
		if (versionElement instanceof JsonPrimitive primitive && primitive.isString()) {
			return primitive.getAsString();
		}
		return DEFAULT_INITIAL_VERSION;
	}

	private static boolean mergeDefaults(JsonObject target, JsonObject defaults) {
		boolean added = false;
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

	private static String getModVersion() {
		return FabricLoader.getInstance()
			.getModContainer(MadokuCraftAPI.MOD_ID)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse(DEFAULT_INITIAL_VERSION);
	}

	private static Path buildPath(String featureId) {
		String sanitized = featureId == null ? "feature" : featureId;
		sanitized = sanitized.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
		if (sanitized.isEmpty()) {
			sanitized = "feature";
		}
		Path configDir = FabricLoader.getInstance().getConfigDir();
		return configDir
			.resolve(MadokuCraftAPI.MOD_ID)
			.resolve(sanitized + ".json");
	}

	private static boolean isIncompatible(Version current, Version target) {
		if (current == null || target == null) {
			return true;
		}
		return current.major() != target.major();
	}

	/** Wraps the loaded JSON structure so callers can persist changes safely. */
	public static final class ManagedFeature {
		private final String featureId;
		private final Path path;
		private final JsonObject defaults;
		private final JsonObject root;
		private final String modVersion;

		private ManagedFeature(String featureId, Path path, JsonObject root, JsonObject defaults, String modVersion) {
			this.featureId = featureId;
			this.path = path;
			this.root = root;
			this.defaults = defaults;
			this.modVersion = modVersion;
		}

		public Path getPath() {
			return path;
		}

		public JsonObject getRoot() {
			return root;
		}

		/** Saves any mutations that happened to the JSON object, keeping defaults and version in sync. */
		public void save() {
			mergeDefaults(root, defaults);
			if (!modVersion.equals(obtainVersion(root))) {
				root.addProperty(VERSION_FIELD, modVersion);
			}
			writeJson(path, root);
			LOGGER.info("Saved JSON feature {} to {}", featureId, path);
		}
	}

	private record Version(int major, int minor, int patch) {
		private static Version parse(String raw) {
			if (raw == null || raw.isBlank()) {
				return null;
			}
			String[] pieces = raw.split("\\.");
			Integer major = parseSegment(pieces, 0);
			Integer minor = parseSegment(pieces, 1);
			Integer patch = parseSegment(pieces, 2);
			if (major == null) {
				return null;
			}
			return new Version(major, minor == null ? 0 : minor, patch == null ? 0 : patch);
		}

		private static Integer parseSegment(String[] pieces, int index) {
			if (index >= pieces.length) {
				return 0;
			}
			String part = pieces[index];
			if (part == null || part.isEmpty()) {
				return 0;
			}
			StringBuilder digits = new StringBuilder();
			for (char ch : part.toCharArray()) {
				if (Character.isDigit(ch)) {
					digits.append(ch);
				} else {
					break;
				}
			}
			if (digits.isEmpty()) {
				return null;
			}
			return Integer.parseInt(digits.toString());
		}
	}
}
