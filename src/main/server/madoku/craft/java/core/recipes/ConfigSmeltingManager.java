package madoku.craft.java.core.recipes;

import java.util.List;

public final class ConfigSmeltingManager {
	private static volatile boolean initialized;
	private ConfigSmeltingManager() { }
	static void initialize() { initialized = true; }
	static void reset() { initialized = false; }
	public static boolean isInitialized() { return initialized; }

	public static AddedCookingRecipe buildAddedRecipe() {
		return new AddedCookingRecipe("minecraft:soul_sand", "minecraft:tinted_glass", 0.1F, 200);
	}

	public static List<AddedCookingRecipe> buildAddedRecipes() {
		return List.of(
			buildAddedRecipe(),
			new AddedCookingRecipe("minecraft:tropical_fish", "madoku-craft:cooked-tropical-fish", 0.35F, 200)
		);
	}

	public record AddedCookingRecipe(String inputItemId, String resultItemId, float experience, int cookingTime) { }
}
