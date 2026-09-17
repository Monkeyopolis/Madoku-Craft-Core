package madoku.craft.mixin.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import madoku.craft.java.core.recipes.RecipesAPIManager;

import java.util.List;
import java.util.Map;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerRecipeOverridesMixin {
	@Shadow
	@Mutable
	private RecipeMap recipes;

	@Unique
	private boolean madokuCraft$rebuildingRecipeCaches;

	@Shadow
	public abstract void finalizeRecipeLoading(FeatureFlagSet featureFlags);

	@Inject(method = "finalizeRecipeLoading", at = @At("TAIL"))
	private void madoku$applyRecipeConfig(FeatureFlagSet featureFlags, CallbackInfo ci) {
		if (this.madokuCraft$rebuildingRecipeCaches) {
			return;
		}
		if (this.recipes == null) {
			return;
		}

		List<RecipeHolder<?>> resolvedRecipes = RecipesAPIManager.applyRecipeOverrides(this.recipes.values());
		this.recipes = madokuCraft$createRecipeMap(resolvedRecipes);
		this.madokuCraft$rebuildingRecipeCaches = true;
		try {
			this.finalizeRecipeLoading(featureFlags);
		} finally {
			this.madokuCraft$rebuildingRecipeCaches = false;
		}
	}

	@Unique
	private static RecipeMap madokuCraft$createRecipeMap(List<RecipeHolder<?>> resolvedRecipes) {
		ImmutableMultimap.Builder<RecipeType<?>, RecipeHolder<?>> byType = ImmutableMultimap.builder();
		ImmutableMap.Builder<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey = ImmutableMap.builder();
		for (RecipeHolder<?> holder : resolvedRecipes) {
			if (holder == null || holder.id() == null || holder.value() == null) {
				continue;
			}
			byType.put(holder.value().getType(), holder);
			byKey.put(holder.id(), holder);
		}

		try {
			var constructor = RecipeMap.class.getDeclaredConstructor(Multimap.class, Map.class);
			constructor.setAccessible(true);
			return constructor.newInstance(byType.build(), byKey.build());
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Unable to rebuild the 26.3 recipe map.", exception);
		}
	}
}

