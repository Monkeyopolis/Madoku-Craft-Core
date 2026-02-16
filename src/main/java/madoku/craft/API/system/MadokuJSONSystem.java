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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Centralized JSON file manager for Madoku Craft API integrations. */
public final class MadokuJSONSystem {
	public static final String SYSTEM_NAME = "Madoku-JSON-System";

	private static final Logger LOGGER = LoggerFactory.getLogger(MadokuCraftAPI.MOD_ID);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String ROOT_FOLDER = "madoku-craft";
	private static final String MADOKU_PREFIX = "madoku-craft.";
	private static final String MADOKU_FILE_PREFIX = "madoku-";
	private static final String MADOKU_PREFIX_UNDERSCORE_DOT = "madoku_craft.";
	private static final String MADOKU_PREFIX_DASH = "madoku-craft-";
	private static final String MADOKU_PREFIX_UNDERSCORE = "madoku_craft_";
	private static final String VERSION_FIELD = "version";
	private static final String DEFAULT_VERSION = "0.0.0";
	private static final String DEFAULT_SCOPED_FALLBACK = "unknown";
	private static final String DEFAULT_FILE_FALLBACK = "file";
	private static final Set<String> ACRONYMS = Set.of("api", "json", "ui", "hud", "gui");

	private MadokuJSONSystem() {
	}

	/**
	 * Loads a JSON file from config/madoku-craft/{folderPath}/{fileName}.json.
	 * The file keeps user values, adds missing defaults, removes unknown keys, and syncs version.
	 */
	public static ManagedJSON load(String folderPath, String fileName, JsonObject defaults) {
		Path path = resolvePath(folderPath, fileName);
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
		List<String> segments = scopedPathSegments(folderPath);
		for (String segment : segments) {
			resolved = resolved.resolve(segment);
		}
		return resolved;
	}

	private static String sanitizeFileName(String fileName) {
		return normalizeFlatFileBaseName(fileName);
	}

	static String normalizeFlatFileBaseName(String rawName) {
		String normalized = rawName == null ? "" : rawName.trim();
		if (normalized.toLowerCase(Locale.ROOT).endsWith(".json")) {
			normalized = normalized.substring(0, normalized.length() - ".json".length());
		}
		return flatFileName(normalized.isBlank() ? DEFAULT_FILE_FALLBACK : normalized);
	}

	private static List<String> scopedPathSegments(String rawPath) {
		List<String> segments = new ArrayList<>();
		if (rawPath == null || rawPath.isBlank()) {
			segments.add(scopedName(DEFAULT_SCOPED_FALLBACK));
			return segments;
		}

		String normalizedPath = rawPath.replace('\\', '/');
		for (String piece : normalizedPath.split("/+")) {
			if (!piece.isBlank()) {
				segments.add(scopedName(piece));
			}
		}

		if (segments.isEmpty()) {
			segments.add(scopedName(DEFAULT_SCOPED_FALLBACK));
		}
		return segments;
	}

	private static String scopedName(String rawName) {
		String withoutPrefix = stripPrefix(rawName);
		String normalized = normalizeScopedBody(withoutPrefix, DEFAULT_SCOPED_FALLBACK);
		return MADOKU_PREFIX + normalized;
	}

	private static String flatFileName(String rawName) {
		String withoutPrefix = stripPrefix(rawName);
		String normalized = normalizeFlatBody(withoutPrefix, DEFAULT_FILE_FALLBACK);
		return MADOKU_FILE_PREFIX + normalized;
	}

	private static String stripPrefix(String rawName) {
		String normalized = rawName == null ? "" : rawName.trim();
		String lower = normalized.toLowerCase(Locale.ROOT);
		boolean changed;
		do {
			changed = false;
			if (lower.startsWith(MADOKU_PREFIX)) {
				normalized = normalized.substring(MADOKU_PREFIX.length());
				changed = true;
			} else if (lower.startsWith(MADOKU_PREFIX_UNDERSCORE_DOT)) {
				normalized = normalized.substring(MADOKU_PREFIX_UNDERSCORE_DOT.length());
				changed = true;
			} else if (lower.startsWith(MADOKU_PREFIX_DASH)) {
				normalized = normalized.substring(MADOKU_PREFIX_DASH.length());
				changed = true;
			} else if (lower.startsWith(MADOKU_PREFIX_UNDERSCORE)) {
				normalized = normalized.substring(MADOKU_PREFIX_UNDERSCORE.length());
				changed = true;
			}
			if (changed) {
				lower = normalized.toLowerCase(Locale.ROOT);
			}
		} while (changed);
		if (normalized.isBlank()) {
			return DEFAULT_SCOPED_FALLBACK;
		}
		return normalized;
	}

	private static String normalizeScopedBody(String raw, String fallback) {
		String value = raw == null ? "" : raw.trim();
		value = value.replaceAll("[^A-Za-z0-9.-]+", "-");
		value = value.replaceAll("-{2,}", "-");
		value = value.replaceAll("\\.+", ".");
		value = value.replaceAll("^[.-]+|[.-]+$", "");
		if (value.isEmpty()) {
			value = fallback;
		}

		List<String> dotParts = new ArrayList<>();
		for (String dotPart : value.split("\\.")) {
			String cleaned = normalizeDashPiece(dotPart);
			if (!cleaned.isEmpty()) {
				dotParts.add(cleaned);
			}
		}

		if (dotParts.isEmpty()) {
			return fallback;
		}
		return String.join(".", dotParts);
	}

	private static String normalizeFlatBody(String raw, String fallback) {
		String value = raw == null ? "" : raw.trim();
		value = value.toLowerCase(Locale.ROOT);
		value = value.replaceAll("[^a-z0-9]+", "-");
		value = value.replaceAll("-{2,}", "-");
		value = value.replaceAll("^-+|-+$", "");
		if (value.startsWith(MADOKU_FILE_PREFIX)) {
			value = value.substring(MADOKU_FILE_PREFIX.length());
		}
		if (value.isEmpty()) {
			value = fallback;
		}
		return value;
	}

	private static String normalizeDashPiece(String raw) {
		String piece = raw == null ? "" : raw.trim();
		piece = piece.replaceAll("[^A-Za-z0-9-]+", "-");
		piece = piece.replaceAll("-{2,}", "-");
		piece = piece.replaceAll("^-+|-+$", "");
		if (piece.isEmpty()) {
			return "";
		}

		List<String> parts = new ArrayList<>();
		for (String token : piece.split("-+")) {
			if (token.isEmpty()) {
				continue;
			}
			String lower = token.toLowerCase(Locale.ROOT);
			if (ACRONYMS.contains(lower)) {
				parts.add(lower.toUpperCase(Locale.ROOT));
			} else if (isExplicitUpperToken(token)) {
				parts.add(token.toUpperCase(Locale.ROOT));
			} else {
				parts.add(lower);
			}
		}
		return String.join("-", parts);
	}

	private static boolean isExplicitUpperToken(String token) {
		if (token == null || token.isEmpty()) {
			return false;
		}
		boolean hasLetter = false;
		for (char ch : token.toCharArray()) {
			if (Character.isLetter(ch)) {
				hasLetter = true;
				if (!Character.isUpperCase(ch)) {
					return false;
				}
			}
		}
		return hasLetter;
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
