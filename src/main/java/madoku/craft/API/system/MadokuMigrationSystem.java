package madoku.craft.API.system;

import madoku.craft.API.MadokuCraftAPI;

import net.fabricmc.loader.api.FabricLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Centralized migration helpers for legacy Madoku JSON and data files. */
public final class MadokuMigrationSystem {
	public static final String SYSTEM_NAME = "Madoku-Migration-System";

	private static final Logger LOGGER = LoggerFactory.getLogger(MadokuCraftAPI.MOD_ID);
	private static final String CURRENT_JSON_ROOT_FOLDER = "madoku-craft";
	private static final String LEGACY_MOD_CONFIG_FOLDER = MadokuCraftAPI.MOD_ID;
	private static final String LEGACY_DATA_FOLDER = "madoku-data";
	private static final String LEGACY_FEATURE_FALLBACK = "feature";

	private MadokuMigrationSystem() {
	}

	/** Migrates a legacy JSON config file to the new Madoku-JSON-System path when needed. */
	public static void migrateJsonIfNeeded(Path targetPath, String folderPath, String fileName) {
		if (targetPath == null || Files.exists(targetPath)) {
			return;
		}

		Path configDir = FabricLoader.getInstance().getConfigDir();
		List<String> canonicalSegments = MadokuNamingSystem.scopedPathSegments(folderPath);
		String canonicalBase = MadokuNamingSystem.jsonFileBaseName(fileName);
		String legacyFriendlyBase = legacyFriendlyFileBase(fileName);
		String legacyUnderscoreBase = legacyUnderscoreId(fileName);
		List<String> legacyFileAsFolder = new ArrayList<>();
		legacyFileAsFolder.add(legacyFriendlyBase);

		List<Path> candidates = new ArrayList<>();
		candidates.add(buildNestedPath(configDir.resolve(CURRENT_JSON_ROOT_FOLDER), canonicalSegments, canonicalBase.toLowerCase(Locale.ROOT)));
		candidates.add(buildNestedPath(configDir.resolve(CURRENT_JSON_ROOT_FOLDER), legacyFileAsFolder, canonicalBase));
		candidates.add(configDir.resolve(CURRENT_JSON_ROOT_FOLDER).resolve(canonicalBase.toLowerCase(Locale.ROOT) + ".json"));
		candidates.add(configDir.resolve(CURRENT_JSON_ROOT_FOLDER).resolve(legacyFriendlyBase + ".json"));
		candidates.add(configDir.resolve(LEGACY_MOD_CONFIG_FOLDER).resolve(legacyUnderscoreBase + ".json"));

		moveFirstExisting(candidates, targetPath, "JSON", folderPath + "/" + fileName);
	}

	private static Path buildNestedPath(Path root, List<String> segments, String fileBase) {
		Path resolved = root;
		if (segments != null) {
			for (String segment : segments) {
				if (segment != null && !segment.isBlank()) {
					resolved = resolved.resolve(segment);
				}
			}
		}
		return resolved.resolve(fileBase + ".json");
	}

	/** Migrates a legacy data file to the new Madoku-Data-System path when needed. */
	public static void migrateDataIfNeeded(Path targetPath, String dataName, Path baseDir) {
		if (targetPath == null || baseDir == null || Files.exists(targetPath)) {
			return;
		}

		String canonicalBase = MadokuNamingSystem.jsonFileBaseName(dataName);
		String legacyUnderscoreBase = legacyUnderscoreId(dataName);

		List<Path> candidates = new ArrayList<>();
		candidates.add(baseDir.resolve(CURRENT_JSON_ROOT_FOLDER).resolve(canonicalBase.toLowerCase(Locale.ROOT) + ".json"));
		candidates.add(baseDir.resolve(LEGACY_DATA_FOLDER).resolve(legacyUnderscoreBase + ".json"));
		candidates.add(baseDir.resolve(LEGACY_DATA_FOLDER).resolve(canonicalBase.toLowerCase(Locale.ROOT) + ".json"));

		moveFirstExisting(candidates, targetPath, "DATA", dataName);
	}

	private static void moveFirstExisting(List<Path> sources, Path target, String kind, String label) {
		if (sources == null || sources.isEmpty()) {
			return;
		}
		Set<Path> deduped = new LinkedHashSet<>(sources);
		for (Path source : deduped) {
			if (source == null || source.equals(target) || !Files.exists(source)) {
				continue;
			}
			try {
				Files.createDirectories(target.getParent());
				Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
				LOGGER.info("{} migrated {} {} from {} to {}", SYSTEM_NAME, kind, label, source, target);
				return;
			} catch (IOException exc) {
				throw new UncheckedIOException(
					"Unable to migrate legacy " + kind + " " + label + " from " + source + " to " + target,
					exc
				);
			}
		}
	}

	private static String legacyFriendlyFileBase(String rawName) {
		String normalized = rawName == null ? "" : rawName.trim();
		if (normalized.toLowerCase(Locale.ROOT).startsWith(MadokuNamingSystem.MADOKU_PREFIX)) {
			normalized = normalized.substring(MadokuNamingSystem.MADOKU_PREFIX.length());
		}
		if (normalized.toLowerCase(Locale.ROOT).startsWith("madoku_craft_")) {
			normalized = normalized.substring("madoku_craft_".length());
		} else if (normalized.toLowerCase(Locale.ROOT).startsWith("madoku-craft-")) {
			normalized = normalized.substring("madoku-craft-".length());
		}
		normalized = normalized.toLowerCase(Locale.ROOT);
		normalized = normalized.replaceAll("[^a-z0-9]+", "-");
		normalized = normalized.replaceAll("^-+|-+$", "");
		if (normalized.isEmpty()) {
			normalized = LEGACY_FEATURE_FALLBACK;
		}
		return "madoku-craft." + normalized;
	}

	private static String legacyUnderscoreId(String rawName) {
		String normalized = rawName == null ? "" : rawName.trim();
		normalized = normalized.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase(Locale.ROOT);
		if (normalized.isEmpty()) {
			return LEGACY_FEATURE_FALLBACK;
		}
		return normalized;
	}
}
