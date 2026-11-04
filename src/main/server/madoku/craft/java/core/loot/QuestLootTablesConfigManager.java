package madoku.craft.java.core.loot;

import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** Builds the default configurable item-reward tables used by quest difficulties. */
public final class QuestLootTablesConfigManager {
	private QuestLootTablesConfigManager() {
	}

	public static Map<String, JsonObject> buildDefaultQuestTableFiles() {
		Map<String, JsonObject> defaults = new LinkedHashMap<>();
		put(defaults, "quest-easy-difficulty", 0, 2);
		put(defaults, "quest-normal-difficulty", 1, 3);
		put(defaults, "quest-hard-difficulty", 2, 4);
		return defaults;
	}

	private static void put(
		Map<String, JsonObject> defaults,
		String tableId,
		int materialMinRolls,
		int materialMaxRolls
	) {
		defaults.put(
			LootTableConfigManager.fileKeyFromTableId(tableId, "quest-table"),
			buildQuestTable(tableId, materialMinRolls, materialMaxRolls)
		);
	}

	private static JsonObject buildQuestTable(
		String tableId,
		int materialMinRolls,
		int materialMaxRolls
	) {
		JsonObject table = LootTableAPIManager.buildSharedTable(
			tableId,
			buildMaterialRewardEntry(materialMinRolls, materialMaxRolls)
		);
		// Quest IDs intentionally remain namespace-free in the config, while the
		// shared parser still normalizes them for registry-style lookups.
		table.addProperty(LootTableConfigManager.FIELD_TABLE_ID, tableId);
		return table;
	}

	private static JsonObject buildMaterialRewardEntry(int minRolls, int maxRolls) {
		return LootTableAPIManager.buildSharedTableEntry(
			minRolls,
			maxRolls,
			reward("minecraft:coal", 42, 5, 7),
			reward("minecraft:copper-ingot", 27, 4, 6),
			reward("minecraft:iron-ingot", 17, 3, 5),
			reward("minecraft:gold-ingot", 10, 2, 4),
			reward("minecraft:diamond", 3, 1, 3),
			reward("minecraft:netherite-scrap", 1, 0, 2)
		);
	}

	private static JsonObject reward(String itemId, int weight, int minCount, int maxCount) {
		return LootTableAPIManager.buildSharedEntry(itemId, weight, minCount, maxCount);
	}
}
