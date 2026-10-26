package madoku.craft.java.core.recipes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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
				3
			),
			new AddedCraftingRecipe(
				"food/bowl-of-melon",
				"madoku-craft:bowl-of-melon",
				List.of("   ", "AAA", " B "),
				Map.of('A', "minecraft:melon-slice", 'B', "minecraft:bowl")
			)
		);
	}

	public static void applyDefaultOverrides(String recipeId, JsonObject defaults) {
		if (!hasDefaultOverride(recipeId) || defaults == null) {
			return;
		}

		defaults.addProperty(RecipesConfigManager.FIELD_CUSTOM_RECIPE, true);
		defaults.addProperty(RecipesConfigManager.FIELD_RESULT_COUNT, 5);
		defaults.addProperty(RecipesConfigManager.FIELD_CRAFTING_SHAPE, RecipesConfigManager.CRAFTING_SHAPE_SHAPED);

		JsonArray pattern = new JsonArray();
		pattern.add("-B-");
		pattern.add("AAA");
		pattern.add("-B-");
		defaults.add(RecipesConfigManager.FIELD_PATTERN, pattern);

		JsonObject key = new JsonObject();
		key.addProperty("A", "minecraft:cocoa_beans");
		key.addProperty("B", "minecraft:wheat");
		defaults.add(RecipesConfigManager.FIELD_KEY, key);

		JsonArray ingredients = new JsonArray();
		ingredients.add("minecraft:cocoa_beans");
		ingredients.add("minecraft:wheat");
		defaults.add(RecipesConfigManager.FIELD_INGREDIENTS, ingredients);
	}

	public static boolean hasDefaultOverride(String recipeId) {
		return recipeId != null && "minecraft:cookie".equalsIgnoreCase(recipeId.trim());
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
