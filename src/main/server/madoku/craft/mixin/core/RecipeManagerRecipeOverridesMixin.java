package madoku.craft.mixin.core;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import madoku.craft.java.core.recipes.RecipesAPIManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

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
		return RecipeMap.create(madokuCraft$createRecipeLookup(resolvedRecipes));
	}

	@Unique
	private static HolderLookup<Recipe<?>> madokuCraft$createRecipeLookup(List<RecipeHolder<?>> resolvedRecipes) {
		Map<ResourceKey<Recipe<?>>, Holder.Reference<Recipe<?>>> references = new LinkedHashMap<>();
		for (RecipeHolder<?> holder : resolvedRecipes) {
			if (holder == null || holder.id() == null || holder.value() == null) {
				continue;
			}
			if (references.put(holder.id(), new MadokuRecipeReference(holder.id(), holder.value())) != null) {
				throw new IllegalStateException("Duplicate recipe key while rebuilding the 26.3 recipe map: " + holder.id());
			}
		}

		return new HolderLookup<>() {
			@Override
			public Stream<Holder.Reference<Recipe<?>>> listElements() {
				return references.values().stream();
			}

			@Override
			public Stream<HolderSet.Named<Recipe<?>>> listTags() {
				return Stream.empty();
			}

			@Override
			public Optional<Holder.Reference<Recipe<?>>> get(ResourceKey<Recipe<?>> key) {
				return Optional.ofNullable(references.get(key));
			}

			@Override
			public Optional<HolderSet.Named<Recipe<?>>> get(TagKey<Recipe<?>> key) {
				return Optional.empty();
			}
		};
	}

	@Unique
	private static final HolderOwner<Recipe<?>> MADOKU_RECIPE_OWNER = new HolderOwner<>() {
	};

	@Unique
	private static final class MadokuRecipeReference extends Holder.Reference<Recipe<?>> {
		private MadokuRecipeReference(ResourceKey<Recipe<?>> key, Recipe<?> value) {
			super(Holder.Reference.Type.STAND_ALONE, MADOKU_RECIPE_OWNER, key, value);
		}
	}
}

