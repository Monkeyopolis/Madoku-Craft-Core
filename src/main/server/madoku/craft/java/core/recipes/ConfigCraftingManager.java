package madoku.craft.java.core.recipes;

import java.util.List;
import java.util.Map;

public final class ConfigCraftingManager {
	private static volatile boolean initialized;
	private ConfigCraftingManager() { }
	static void initialize() { initialized = true; }
	static void reset() { initialized = false; }
	public static boolean isInitialized() { return initialized; }

	public static List<AddedCraftingRecipe> buildAddedRecipes() {
		return List.of(
			new AddedCraftingRecipe("chainmail/helmet", "minecraft:chainmail_helmet", List.of("NNN", "N N"), 'N', "minecraft:iron_nugget"),
			new AddedCraftingRecipe("chainmail/chestplate", "minecraft:chainmail_chestplate", List.of("N N", "NNN", "NNN"), 'N', "minecraft:iron_nugget"),
			new AddedCraftingRecipe("chainmail/leggings", "minecraft:chainmail_leggings", List.of("NNN", "N N", "N N"), 'N', "minecraft:iron_nugget"),
			new AddedCraftingRecipe("chainmail/boots", "minecraft:chainmail_boots", List.of("N N", "N N"), 'N', "minecraft:iron_nugget"),
			new AddedCraftingRecipe(
				"enchanted-golden-apple",
				"minecraft:enchanted_golden_apple",
				List.of("GGG", "GAG", "GGG"),
				Map.of('G', "minecraft:gold_block", 'A', "minecraft:apple")
			),
			new AddedCraftingRecipe(
				"food/sweet-berry-cookie",
				"madoku-craft:sweet-berry-cookie",
				List.of(" B ", "AAA", " B "),
				Map.of('A', "minecraft:sweet_berries", 'B', "minecraft:wheat"),
				4
			)
		);
	}

	public record AddedCraftingRecipe(
		String path,
		String resultItemId,
		List<String> pattern,
		Map<Character, String> ingredients,
		int resultCount
	) {
		public AddedCraftingRecipe(String path, String resultItemId, List<String> pattern, char symbol, String ingredientItemId) {
			this(path, resultItemId, pattern, Map.of(symbol, ingredientItemId), 1);
		}

		public AddedCraftingRecipe(String path, String resultItemId, List<String> pattern, Map<Character, String> ingredients) {
			this(path, resultItemId, pattern, ingredients, 1);
		}

		public AddedCraftingRecipe {
			pattern = List.copyOf(pattern);
			ingredients = Map.copyOf(ingredients);
			resultCount = Math.max(1, resultCount);
		}
	}
}
