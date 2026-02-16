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
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Centralized data manager for MOD-scoped JSON data.
 * Supports global and world-based storage with built-in tracker sections.
 */
public final class MadokuDataSystem {
	public static final String SYSTEM_NAME = "Madoku-Data-System";

	private static final Logger LOGGER = LoggerFactory.getLogger(MadokuCraftAPI.MOD_ID);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String ROOT_FOLDER = "madoku-craft";
	private static final String VERSION_FIELD = "version";
	private static final String DEFAULT_VERSION = "0.0.0";

	private MadokuDataSystem() {
	}

	/** Defines where a data file should be stored. */
	public enum StorageScope {
		GLOBAL,
		WORLD
	}

	/** Built-in sections for tracking values across common game object types. */
	public enum Tracker {
		PLAYERS("players"),
		ITEMS("items"),
		BLOCKS("blocks"),
		STORAGE_BLOCKS("storageBlocks"),
		STORAGE_ITEMS("storageItems"),
		ENTITIES("entities");

		private final String key;

		Tracker(String key) {
			this.key = key;
		}

		public String key() {
			return key;
		}
	}

	/** Loads data in the requested scope. WORLD scope without a server starts deferred. */
	public static MadokuData load(String dataName, StorageScope scope, JsonObject defaults) {
		return load(dataName, scope, defaults, null);
	}

	/** Loads data in the requested scope using the current server when needed. */
	public static MadokuData load(String dataName, StorageScope scope, JsonObject defaults, MinecraftServer server) {
		if (scope == StorageScope.GLOBAL) {
			return loadGlobal(dataName, defaults);
		}
		return loadWorld(server, dataName, defaults);
	}

	/** Loads global data stored under the game directory. */
	public static MadokuData loadGlobal(String dataName, JsonObject defaults) {
		return loadForPath(dataName, StorageScope.GLOBAL, defaults, null);
	}

	/**
	 * Creates deferred world data. Call bindWorld(...) once a server world is ready.
	 * Useful during mod init where no world exists yet.
	 */
	public static MadokuData loadWorldDeferred(String dataName, JsonObject defaults) {
		Path placeholderPath = resolvePath(dataName, StorageScope.WORLD, null);
		JsonObject template = buildTemplate(defaults);
		return new MadokuData(dataName, StorageScope.WORLD, placeholderPath, template.deepCopy(), template, true);
	}

	/** Loads world data immediately if a server is available, otherwise returns deferred world data. */
	public static MadokuData loadWorld(MinecraftServer server, String dataName, JsonObject defaults) {
		if (server == null) {
			return loadWorldDeferred(dataName, defaults);
		}
		Path worldRoot = server.getSavePath(WorldSavePath.ROOT);
		return loadForPath(dataName, StorageScope.WORLD, defaults, worldRoot);
	}

	/**
	 * Rebinds an existing deferred world data handle to the active world path and reloads from disk.
	 */
	public static void bindWorld(MadokuData data, String dataName, JsonObject defaults, MinecraftServer server) {
		if (data == null || server == null || data.scope != StorageScope.WORLD) {
			return;
		}
		Path worldRoot = server.getSavePath(WorldSavePath.ROOT);
		if (worldRoot == null) {
			return;
		}
		Path path = resolvePath(dataName, StorageScope.WORLD, worldRoot);
		Path base = resolveBasePath(StorageScope.WORLD, worldRoot);
		MadokuMigrationSystem.migrateDataIfNeeded(path, dataName, base);
		JsonObject template = buildTemplate(defaults);
		JsonObject diskJson = prepareJson(path, template, dataName, StorageScope.WORLD);
		data.replace(path, diskJson, template);
	}

	/** Backward-compatible alias. Defaults to GLOBAL storage. */
	@Deprecated
	public static MadokuData load(String featureId, JsonObject defaults) {
		return loadGlobal(featureId, defaults);
	}

	/** Backward-compatible alias for deferred WORLD storage. */
	@Deprecated
	public static MadokuData loadDeferred(String featureId, JsonObject defaults) {
		return loadWorldDeferred(featureId, defaults);
	}

	/** Backward-compatible alias for WORLD storage. */
	@Deprecated
	public static MadokuData loadForWorld(MinecraftServer server, String featureId, JsonObject defaults) {
		return loadWorld(server, featureId, defaults);
	}

	/** Backward-compatible alias for world rebind/reload. */
	@Deprecated
	public static void reloadForWorld(MadokuData data, String featureId, JsonObject defaults, MinecraftServer server) {
		bindWorld(data, featureId, defaults, server);
	}

	private static MadokuData loadForPath(String dataName, StorageScope scope, JsonObject defaults, Path worldRoot) {
		Path base = resolveBasePath(scope, worldRoot);
		Path path = resolvePath(dataName, scope, worldRoot);
		MadokuMigrationSystem.migrateDataIfNeeded(path, dataName, base);
		JsonObject template = buildTemplate(defaults);
		JsonObject diskJson = prepareJson(path, template, dataName, scope);
		return new MadokuData(dataName, scope, path, diskJson, template, false);
	}

	private static JsonObject prepareJson(Path path, JsonObject template, String dataName, StorageScope scope) {
		createParent(path);
		JsonObject diskJson = readJson(path);
		boolean created = diskJson == null;
		if (created) {
			LOGGER.info("Creating {} file {} at {} ({}).", SYSTEM_NAME, dataName, path, scope);
			diskJson = template.deepCopy();
		}

		boolean dirty = mergeMissingDefaults(diskJson, template);
		dirty |= syncVersion(diskJson);

		if (created || dirty) {
			writeJson(path, diskJson);
		}

		return diskJson;
	}

	private static JsonObject buildTemplate(JsonObject defaults) {
		JsonObject template = deepCopy(defaults);
		template.addProperty(VERSION_FIELD, getApiVersion());
		return template;
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
			LOGGER.warn("{} at {} is not an object.", SYSTEM_NAME, path);
		} catch (IOException | JsonParseException exc) {
			LOGGER.warn("Unable to read {} at {}: {}", SYSTEM_NAME, path, exc.getMessage());
		}

		return null;
	}

	private static void writeJson(Path path, JsonObject json) {
		try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			GSON.toJson(json, writer);
		} catch (IOException exc) {
			throw new UncheckedIOException("Unable to write " + SYSTEM_NAME + " to " + path, exc);
		}
	}

	private static JsonObject deepCopy(JsonObject source) {
		return source == null ? new JsonObject() : source.deepCopy();
	}

	private static boolean mergeMissingDefaults(JsonObject target, JsonObject defaults) {
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
					added |= mergeMissingDefaults(targetValue.getAsJsonObject(), defaultValue.getAsJsonObject());
				}
				continue;
			}
			target.add(key, entry.getValue().deepCopy());
			added = true;
		}
		return added;
	}

	private static void createParent(Path path) {
		try {
			Files.createDirectories(path.getParent());
		} catch (IOException exc) {
			throw new UncheckedIOException("Unable to prepare " + SYSTEM_NAME + " directory for " + path, exc);
		}
	}

	private static boolean syncVersion(JsonObject json) {
		String current = obtainVersion(json);
		String target = getApiVersion();
		if (target.equals(current)) {
			return false;
		}
		json.addProperty(VERSION_FIELD, target);
		return true;
	}

	private static String obtainVersion(JsonObject json) {
		JsonElement versionElement = json.get(VERSION_FIELD);
		if (versionElement instanceof JsonPrimitive primitive && primitive.isString()) {
			return primitive.getAsString();
		}
		return DEFAULT_VERSION;
	}

	private static String getApiVersion() {
		return FabricLoader.getInstance()
			.getModContainer(MadokuCraftAPI.MOD_ID)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse(DEFAULT_VERSION);
	}

	private static Path resolvePath(String dataName, StorageScope scope, Path worldRoot) {
		Path base = resolveBasePath(scope, worldRoot);
		String fileBase = MadokuNamingSystem.jsonFileBaseName(dataName);
		return base.resolve(ROOT_FOLDER).resolve(fileBase + ".json");
	}

	private static Path resolveBasePath(StorageScope scope, Path worldRoot) {
		return scope == StorageScope.WORLD && worldRoot != null
			? worldRoot
			: FabricLoader.getInstance().getGameDir();
	}

	private static JsonObject ensureObject(JsonObject parent, String key) {
		if (parent.has(key) && parent.get(key).isJsonObject()) {
			return parent.getAsJsonObject(key);
		}
		JsonObject object = new JsonObject();
		parent.add(key, object);
		return object;
	}

	private static String normalizeEntryId(String rawId) {
		String value = rawId == null ? "" : rawId.trim();
		value = value.toLowerCase(Locale.ROOT);
		value = value.replaceAll("[^a-z0-9._:/-]+", "_");
		value = value.replaceAll("_+", "_");
		value = value.replaceAll("^[_:/.-]+|[_:/.-]+$", "");
		return value.isEmpty() ? "unknown" : value;
	}

	/** Represents an open, mutable data file with common tracker helpers. */
	public static final class MadokuData {
		private final String dataName;
		private final StorageScope scope;
		private Path path;
		private JsonObject root;
		private JsonObject defaults;
		private boolean deferred;

		private MadokuData(
			String dataName,
			StorageScope scope,
			Path path,
			JsonObject root,
			JsonObject defaults,
			boolean deferred
		) {
			this.dataName = dataName;
			this.scope = scope;
			this.path = path;
			this.root = root;
			this.defaults = defaults;
			this.deferred = deferred;
		}

		public Path getPath() {
			return path;
		}

		public JsonObject getRoot() {
			return root;
		}

		public StorageScope getScope() {
			return scope;
		}

		public boolean isDeferred() {
			return deferred;
		}

		public JsonObject section(Tracker tracker) {
			return ensureObject(root, tracker.key());
		}

		public JsonObject players() {
			return section(Tracker.PLAYERS);
		}

		public JsonObject items() {
			return section(Tracker.ITEMS);
		}

		public JsonObject blocks() {
			return section(Tracker.BLOCKS);
		}

		public JsonObject storageBlocks() {
			return section(Tracker.STORAGE_BLOCKS);
		}

		public JsonObject storageItems() {
			return section(Tracker.STORAGE_ITEMS);
		}

		public JsonObject entities() {
			return section(Tracker.ENTITIES);
		}

		public JsonObject entry(Tracker tracker, String entryId) {
			JsonObject section = section(tracker);
			return ensureObject(section, normalizeEntryId(entryId));
		}

		public void putValue(Tracker tracker, String entryId, String key, JsonElement value) {
			if (key == null || key.isBlank()) {
				return;
			}
			JsonObject entry = entry(tracker, entryId);
			entry.add(key, value == null ? null : value.deepCopy());
		}

		public void putString(Tracker tracker, String entryId, String key, String value) {
			if (key == null || key.isBlank()) {
				return;
			}
			entry(tracker, entryId).addProperty(key, value);
		}

		public void putNumber(Tracker tracker, String entryId, String key, Number value) {
			if (key == null || key.isBlank()) {
				return;
			}
			entry(tracker, entryId).addProperty(key, value);
		}

		public void putBoolean(Tracker tracker, String entryId, String key, boolean value) {
			if (key == null || key.isBlank()) {
				return;
			}
			entry(tracker, entryId).addProperty(key, value);
		}

		public void removeEntry(Tracker tracker, String entryId) {
			JsonObject section = section(tracker);
			section.remove(normalizeEntryId(entryId));
		}

		public void save() {
			if (deferred) {
				LOGGER.warn(
					"Skipping {} save for {} because WORLD scope is deferred until a server is active.",
					SYSTEM_NAME,
					dataName
				);
				return;
			}
			mergeMissingDefaults(root, defaults);
			root.addProperty(VERSION_FIELD, getApiVersion());
			writeJson(path, root);
			LOGGER.info("{} {} saved to {}", SYSTEM_NAME, dataName, path);
		}

		private void replace(Path path, JsonObject root, JsonObject defaults) {
			this.path = path;
			this.root = root;
			this.defaults = defaults;
			this.deferred = false;
		}
	}
}
