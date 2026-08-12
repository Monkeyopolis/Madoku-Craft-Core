package madoku.craft.api.loot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import madoku.craft.api.json.JSONFormatManager;
import madoku.craft.api.json.MadokuJSONManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Loads and rolls the shared weighted entity loot-table format. */
public final class LootTableEntitiesManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(LootTableEntitiesManager.class);
	private static final String LOOT_CONFIG_TABLES_FOLDER_NAME = "madoku-entities";
	private static final String ENTITY_LOOT_NAMESPACE = "minecraft";
	private static final String ENTITY_LOOT_PREFIX = "minecraft:entities/";
	private static volatile Settings settings = Settings.defaults();
	private static volatile Map<String, MadokuLootTableManager.SharedLootTable> tablesById = Map.of();
	private static volatile Map<String, MadokuLootTableManager.SharedLootTable> tablesByFileKey = Map.of();
	private static volatile long nextReloadAtMillis;

	private LootTableEntitiesManager() { }

	public static void initialize() { reloadNow(null); }

	public static void reset() {
		settings = Settings.defaults();
		tablesById = Map.of();
		tablesByFileKey = Map.of();
		nextReloadAtMillis = 0L;
	}

	public static boolean applyManagedLootTable(Container container, ResourceKey<LootTable> lootTableKey,
		long lootTableSeed, ServerLevel level, ServerPlayer player) {
		if (container == null || lootTableKey == null || level == null) return false;
		reloadIfNeeded(level.getServer());
		if (!settings.enabled || !settings.overrideEntityLootTables) return false;
		MadokuLootTableManager.SharedLootTable table = resolveManagedTableByLootId(lootTableKey.identifier().toString());
		if (table == null) return false;
		RandomSource random = lootTableSeed == 0L ? level.getRandom() : RandomSource.create(lootTableSeed);
		fillContainer(container, MadokuLootTableManager.rollSharedTable(table, random), random);
		return true;
	}

	public static List<ItemStack> generateManagedLootForContext(LootContext lootContext) {
		if (lootContext == null) return null;
		ServerLevel level = lootContext.getLevel();
		reloadIfNeeded(level == null ? null : level.getServer());
		if (!settings.enabled || !settings.overrideEntityLootTables) return null;
		String tableId = resolveQueriedLootTableId(lootContext);
		if (tableId.isBlank()) {
			tableId = resolveEntityLootTableId(lootContext);
		}
		MadokuLootTableManager.SharedLootTable table = resolveManagedTableByLootId(tableId);
		if (table == null) return null;
		RandomSource random = lootContext.getRandom();
		return MadokuLootTableManager.rollSharedTable(table, random == null
			? (level == null ? RandomSource.create() : level.getRandom()) : random);
	}

	/** Rolls a table by its managed file key or table id for API consumers. */
	public static List<ItemStack> generateManagedLootForReference(String configuredReference,
		ServerPlayer player, RandomSource random) {
		reloadIfNeeded(null);
		if (!settings.enabled || !settings.overrideEntityLootTables) return null;
		MadokuLootTableManager.SharedLootTable table = resolveManagedTableByConfigReference(configuredReference);
		return table == null ? null : MadokuLootTableManager.rollSharedTable(table,
			random == null ? RandomSource.create() : random);
	}

	private static void fillContainer(Container container, List<ItemStack> generated, RandomSource random) {
		if (container == null) return;
		if (generated == null || generated.isEmpty()) {
			container.setChanged();
			return;
		}
		int containerSize = container.getContainerSize();
		if (containerSize <= 0) {
			container.setChanged();
			return;
		}
		List<Integer> targetSlots = new ArrayList<>(containerSize);
		for (int slot = 0; slot < containerSize; slot++) targetSlots.add(slot);
		if (random != null) {
			for (int index = targetSlots.size() - 1; index > 0; index--) {
				Collections.swap(targetSlots, index, random.nextInt(index + 1));
			}
		}
		for (int index = 0; index < Math.min(targetSlots.size(), generated.size()); index++) {
			ItemStack stack = generated.get(index);
			if (stack != null && !stack.isEmpty()) container.setItem(targetSlots.get(index), stack.copy());
		}
		container.setChanged();
	}

	private static void reloadIfNeeded(net.minecraft.server.MinecraftServer server) {
		if (System.currentTimeMillis() >= nextReloadAtMillis) reloadNow(server);
	}

	private static synchronized void reloadNow(net.minecraft.server.MinecraftServer server) {
		long now = System.currentTimeMillis();
		try {
			Path root = MadokuJSONManager.getOrCreateGlobalSystemDirectory(MadokuLootTableManager.CONFIG_ROOT_FOLDER_NAME);
			Path settingsFile = root.resolve("madoku-loot-tables.json");
			JsonObject defaults = LootTableConfigManager.buildSettingsDefaults();
			Settings loadedSettings = Settings.fromJson(JSONFormatManager.ensureManagedFile(settingsFile, defaults));
			JSONFormatManager.writeManagedFile(settingsFile, loadedSettings.toConfigJson(), defaults);

			Map<String, JsonObject> files = JSONFormatManager.ensureManagedFolder(
				root.resolve(LOOT_CONFIG_TABLES_FOLDER_NAME),
				EntitiesConfigManager.buildDefaultEntityTableFiles(),
				ignored -> new JsonObject(),
				LootTableEntitiesManager::isSupportedLootTableFile,
				(key, value) -> value == null || value.isJsonNull() ? null : value.deepCopy()
			);
			Map<String, MadokuLootTableManager.SharedLootTable> byId = new HashMap<>();
			Map<String, MadokuLootTableManager.SharedLootTable> byFileKey = new HashMap<>();
			for (Map.Entry<String, JsonObject> entry : files.entrySet()) {
				MadokuLootTableManager.SharedLootTable table = MadokuLootTableManager.parseSharedTable(entry.getValue());
				if (table == null) continue;
				byId.put(table.tableId(), table);
				String fileKey = normalizeFileKey(entry.getKey());
				if (!fileKey.isBlank()) byFileKey.put(fileKey, table);
			}
			settings = loadedSettings;
			tablesById = Map.copyOf(byId);
			tablesByFileKey = Map.copyOf(byFileKey);
		} catch (IOException | RuntimeException exception) {
			LOGGER.error("Failed to reload Madoku entity loot tables; preserving last valid cache.", exception);
		} finally {
			nextReloadAtMillis = now + MadokuLootTableManager.resolveReloadIntervalMillis(server);
		}
	}

	private static boolean isSupportedLootTableFile(String fileKey, JsonObject source) {
		return source != null && (!readBoolean(source, LootTableConfigManager.FIELD_ENABLED, true)
			|| !normalizeTableId(readString(source, LootTableConfigManager.FIELD_TABLE_ID, "")).isBlank());
	}

	private static String normalizeTableId(String value) {
		return value == null || value.isBlank() ? "" : MadokuJSONManager.normalizeRegistryIdentifierForLookup(value);
	}

	private static String resolveEntityLootTableId(LootContext lootContext) {
		if (lootContext == null) return "";
		try {
			Entity entity = lootContext.getOptionalParameter(LootContextParams.THIS_ENTITY);
			if (entity == null) return "";
			Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
			return entityId == null ? "" : normalizeTableId(
				entityId.getNamespace() + ":entities/" + entityId.getPath()
			);
		} catch (RuntimeException ignored) {
			return "";
		}
	}

	private static String normalizeFileKey(String value) {
		if (value == null || value.isBlank()) return "";
		String normalized = value.trim().toLowerCase(Locale.ROOT).replace('\\', '/');
		int slash = normalized.lastIndexOf('/');
		if (slash >= 0 && slash < normalized.length() - 1) normalized = normalized.substring(slash + 1);
		return normalized.endsWith(".json") ? normalized.substring(0, normalized.length() - 5) : normalized;
	}

	private static MadokuLootTableManager.SharedLootTable resolveManagedTableByLootId(String rawId) {
		String normalized = normalizeTableId(rawId);
		if (normalized.isBlank()) return null;
		String canonicalId = resolveEntityCanonicalId(normalized);
		return tablesById.get(canonicalId.isBlank() ? normalized : canonicalId);
	}

	private static MadokuLootTableManager.SharedLootTable resolveManagedTableByConfigReference(String rawReference) {
		if (rawReference == null || rawReference.isBlank()) return null;
		MadokuLootTableManager.SharedLootTable byFile = tablesByFileKey.get(normalizeFileKey(rawReference));
		return byFile == null ? resolveManagedTableByLootId(rawReference) : byFile;
	}

	private static String resolveEntityCanonicalId(String rawLootTableId) {
		IdentifierParts parts = IdentifierParts.parse(normalizeTableId(rawLootTableId));
		if (parts == null || !ENTITY_LOOT_NAMESPACE.equals(parts.namespace()) || !parts.path().startsWith("entities/")) return "";
		return ENTITY_LOOT_PREFIX + parts.path().substring("entities/".length());
	}

	private static String resolveQueriedLootTableId(LootContext context) {
		for (String methodName : new String[] { "getQueriedLootTableId", "queriedLootTableId" }) {
			try {
				Method method = context.getClass().getMethod(methodName);
				String id = normalizeIdentifierObject(method.invoke(context));
				if (!id.isBlank()) return id;
			} catch (ReflectiveOperationException | RuntimeException ignored) { }
		}
		return "";
	}

	private static String normalizeIdentifierObject(Object value) {
		if (value == null) return "";
		if (value instanceof java.util.Optional<?> optional) return optional.isPresent() ? normalizeIdentifierObject(optional.get()) : "";
		if (value instanceof ResourceKey<?> key) return normalizeTableId(key.identifier().toString());
		String candidate = value.toString().trim();
		if (candidate.startsWith("Optional[") && candidate.endsWith("]")) candidate = candidate.substring(9, candidate.length() - 1);
		return normalizeTableId(candidate.toLowerCase(Locale.ROOT));
	}

	private static boolean readBoolean(JsonObject root, String key, boolean fallback) {
		JsonElement value = root == null ? null : root.get(key);
		return value instanceof JsonPrimitive primitive && primitive.isBoolean() ? primitive.getAsBoolean() : fallback;
	}

	private static String readString(JsonObject root, String key, String fallback) {
		JsonElement value = root == null ? null : root.get(key);
		return value instanceof JsonPrimitive primitive && primitive.isString() ? primitive.getAsString() : fallback;
	}

	private record IdentifierParts(String namespace, String path) {
		private static IdentifierParts parse(String value) {
			int separator = value.indexOf(':');
			if (separator <= 0 || separator == value.length() - 1) return null;
			return new IdentifierParts(value.substring(0, separator), value.substring(separator + 1));
		}
	}

	private static final class Settings {
		private final boolean enabled;
		private final boolean useMadokuLuck;
		private final boolean overrideStructureLootTables;
		private final boolean overrideEntityLootTables;

		private Settings(boolean enabled, boolean useMadokuLuck, boolean overrideStructureLootTables,
			boolean overrideEntityLootTables) {
			this.enabled = enabled;
			this.useMadokuLuck = useMadokuLuck;
			this.overrideStructureLootTables = overrideStructureLootTables;
			this.overrideEntityLootTables = overrideEntityLootTables;
		}

		private static Settings defaults() { return fromJson(LootTableConfigManager.buildSettingsDefaults()); }

		private static Settings fromJson(JsonObject source) {
			Settings defaults = new Settings(true, true, true, true);
			return new Settings(
				readBoolean(source, LootTableConfigManager.FIELD_ENABLED, defaults.enabled),
				readBoolean(source, LootTableConfigManager.FIELD_USE_MADOKU_LUCK, defaults.useMadokuLuck),
				readBoolean(source, LootTableConfigManager.FIELD_OVERRIDE_STRUCTURE_LOOT_TABLES, defaults.overrideStructureLootTables),
				readBoolean(source, LootTableConfigManager.FIELD_OVERRIDE_ENTITY_LOOT_TABLES, defaults.overrideEntityLootTables));
		}

		private JsonObject toConfigJson() {
			return JSONFormatManager.object()
				.put(LootTableConfigManager.FIELD_ENABLED, enabled)
				.put(LootTableConfigManager.FIELD_USE_MADOKU_LUCK, useMadokuLuck)
				.put(LootTableConfigManager.FIELD_OVERRIDE_STRUCTURE_LOOT_TABLES, overrideStructureLootTables)
				.put(LootTableConfigManager.FIELD_OVERRIDE_ENTITY_LOOT_TABLES, overrideEntityLootTables)
				.build();
		}
	}
}
