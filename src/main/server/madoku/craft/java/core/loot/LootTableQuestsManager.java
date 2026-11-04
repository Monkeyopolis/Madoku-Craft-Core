package madoku.craft.java.core.loot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import madoku.craft.java.core.json.JSONAPIManager;
import madoku.craft.java.core.json.JSONFormatAPIManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Loads configurable item-reward loot tables for quest difficulty tiers. */
public final class LootTableQuestsManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(LootTableQuestsManager.class);
	private static final String TABLES_FOLDER = "madoku-quests";

	private static volatile boolean settingsEnabled = true;
	private static volatile Map<String, LootTableAPIManager.SharedLootTable> tablesById = Map.of();
	private static volatile Map<String, LootTableAPIManager.SharedLootTable> tablesByFileKey = Map.of();
	private static volatile long nextReloadAtMillis;

	private LootTableQuestsManager() {
	}

	public static void initialize() {
		reloadNow(null);
	}

	public static void reset() {
		settingsEnabled = true;
		tablesById = Map.of();
		tablesByFileKey = Map.of();
		nextReloadAtMillis = 0L;
	}

	public static List<ItemStack> generateManagedLootForTable(String tableId, RandomSource random) {
		return generateManagedLootForTable(tableId, random, 1.0d);
	}

	public static List<ItemStack> generateManagedLootForTable(String tableId, RandomSource random, double rewardMultiplier) {
		reloadIfNeeded(null);
		if (!settingsEnabled) {
			return List.of();
		}

		String normalizedId = normalizeTableId(tableId);
		LootTableAPIManager.SharedLootTable table = tablesById.get(normalizedId);
		if (table == null) {
			table = tablesByFileKey.get(normalizeFileKey(tableId));
		}
		return table == null
			? List.of()
			: LootTableAPIManager.rollSharedTable(
				table,
				random == null ? RandomSource.create() : random,
				rewardMultiplier
			);
	}

	private static void reloadIfNeeded(MinecraftServer server) {
		if (System.currentTimeMillis() >= nextReloadAtMillis) {
			reloadNow(server);
		}
	}

	private static synchronized void reloadNow(MinecraftServer server) {
		long now = System.currentTimeMillis();
		try {
			Path root = JSONAPIManager.getOrCreateGlobalSystemDirectory(LootTableAPIManager.CONFIG_ROOT_FOLDER_NAME);
			Path settingsFile = root.resolve("madoku-loot-tables.json");
			JsonObject defaults = LootTableConfigManager.buildSettingsDefaults();
			JsonObject normalizedSettings = JSONFormatAPIManager.ensureManagedFile(settingsFile, defaults);
			boolean loadedEnabled = readBoolean(normalizedSettings, LootTableConfigManager.FIELD_ENABLED, true);

			Map<String, JsonObject> normalizedFiles = JSONFormatAPIManager.ensureManagedFolder(
				root.resolve(TABLES_FOLDER),
				QuestLootTablesConfigManager.buildDefaultQuestTableFiles(),
				ignored -> new JsonObject(),
				LootTableQuestsManager::isSupportedTable,
				LootTableQuestsManager::copyDynamicEntry
			);

			Map<String, LootTableAPIManager.SharedLootTable> resolvedById = new HashMap<>();
			Map<String, LootTableAPIManager.SharedLootTable> resolvedByFileKey = new HashMap<>();
			for (Map.Entry<String, JsonObject> entry : normalizedFiles.entrySet()) {
				LootTableAPIManager.SharedLootTable table = LootTableAPIManager.parseSharedTable(entry.getValue());
				if (table == null) {
					continue;
				}
				resolvedById.put(table.tableId(), table);
				resolvedByFileKey.put(normalizeFileKey(entry.getKey()), table);
			}

			settingsEnabled = loadedEnabled;
			tablesById = Map.copyOf(resolvedById);
			tablesByFileKey = Map.copyOf(resolvedByFileKey);
		} catch (IOException | RuntimeException exception) {
			LOGGER.error("Failed to reload quest loot tables; preserving last valid cache.", exception);
		} finally {
			nextReloadAtMillis = now + LootTableAPIManager.resolveReloadIntervalMillis(server);
		}
	}

	private static boolean isSupportedTable(String fileKey, JsonObject source) {
		if (source == null || source.isEmpty()) {
			return false;
		}
		if (!readBoolean(source, LootTableConfigManager.FIELD_ENABLED, true)) {
			return true;
		}
		return !normalizeTableId(readString(source, LootTableConfigManager.FIELD_TABLE_ID, "")).isBlank();
	}

	private static JsonElement copyDynamicEntry(String key, JsonElement value) {
		return value == null || value.isJsonNull() ? null : value.deepCopy();
	}

	private static String normalizeTableId(String value) {
		return LootTableAPIManager.normalizeSharedTableId(value);
	}

	private static String normalizeFileKey(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		String normalized = value.trim().toLowerCase(Locale.ROOT).replace('\\', '/');
		int slashIndex = normalized.lastIndexOf('/');
		if (slashIndex >= 0 && slashIndex < normalized.length() - 1) {
			normalized = normalized.substring(slashIndex + 1);
		}
		return normalized.endsWith(".json")
			? normalized.substring(0, normalized.length() - ".json".length())
			: normalized;
	}

	private static boolean readBoolean(JsonObject source, String key, boolean fallback) {
		JsonElement value = source == null ? null : source.get(key);
		return value instanceof JsonPrimitive primitive && primitive.isBoolean()
			? primitive.getAsBoolean() : fallback;
	}

	private static String readString(JsonObject source, String key, String fallback) {
		JsonElement value = source == null ? null : source.get(key);
		return value instanceof JsonPrimitive primitive && primitive.isString()
			? primitive.getAsString() : fallback;
	}
}
