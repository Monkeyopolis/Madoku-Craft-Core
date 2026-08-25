package madoku.craft.api.rarity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

import madoku.craft.api.rarity.RarityTierManager.Tier;

/** Orchestrates the shared rarity API without owning an item or gameplay system. */
public final class MadokuRarityManager {
	private static volatile Predicate<ItemStack> rarityItemPredicate = stack -> false;
	private static volatile ToDoubleFunction<ServerPlayer> luckStatResolver = player -> 0.0D;

	private MadokuRarityManager() { }

	public static void initialize() {
		RarityConfigManager.initialize();
		RarityRuntimeManager.initialize();
	}

	public static void reset() {
		RarityRuntimeManager.reset();
		RarityConfigManager.reset();
	}

	public static boolean isEnabled() { return RarityConfigManager.isEnabled(); }

	/** Registers the consuming mod's definition of an item eligible for automatic rarity. */
	public static void setRarityItemPredicate(Predicate<ItemStack> predicate) {
		rarityItemPredicate = predicate == null ? stack -> false : predicate;
	}

	public static boolean isRarityItem(ItemStack stack) {
		try {
			return stack != null && !stack.isEmpty()
				&& (rarityItemPredicate.test(stack) || RarityRuntimeManager.detectAppliedRarity(stack) != null);
		}
		catch (RuntimeException exception) { return false; }
	}

	/** Registers an optional consuming-mod luck source without making Luck a dependency of the API. */
	public static void setLuckStatResolver(ToDoubleFunction<ServerPlayer> resolver) {
		luckStatResolver = resolver == null ? player -> 0.0D : resolver;
	}

	public static List<ItemStack> applyCraftedRarity(ServerPlayer player, ItemStack stack) {
		return RarityRuntimeManager.applyCraftedRarity(player, stack);
	}

	public static void applyGeneratedRarity(ItemStack stack, RandomSource randomSource) {
		RarityRuntimeManager.applyGeneratedRarity(stack, randomSource, null);
	}

	public static void applyGeneratedRarity(ItemStack stack, RandomSource randomSource, ServerPlayer luckPlayer) {
		RarityRuntimeManager.applyGeneratedRarity(stack, randomSource, luckPlayer);
	}

	public static void applyConfiguredRarity(ItemStack stack, Tier rarity) {
		RarityRuntimeManager.applyConfiguredRarity(stack, rarity);
	}

	public static void deliverCraftExtras(ServerPlayer player, List<ItemStack> extras) {
		RarityRuntimeManager.deliverCraftExtras(player, extras);
	}

	public static ItemStack createSmithingUpgradeResult(ItemStack baseStack, ItemStack vanillaResult) {
		return RarityRuntimeManager.createSmithingUpgradeResult(baseStack, vanillaResult);
	}

	public static void updateDurabilityLore(ItemStack stack) {
		RarityRuntimeManager.updateDurabilityLore(stack);
	}

	public static Tier detectAppliedRarity(ItemStack stack) {
		return RarityRuntimeManager.detectAppliedRarity(stack);
	}

	/** Resolves configured weight using a caller-provided luck value when desired. */
	public static double resolveWeight(Tier tier, double luckStat, boolean useMadokuLuck) {
		RarityConfigManager.RaritySettings rarity = RarityConfigManager.settings(tier);
		if (rarity == null || !rarity.enabled || rarity.weight <= 0) return 0.0D;
		double luckWeight = useMadokuLuck && RarityConfigManager.useMadokuLuck() && Double.isFinite(luckStat)
			? Math.max(0.0D, luckStat) * Math.max(0.0D, rarity.weightAdjustment) : 0.0D;
		return Math.max(0.0D, rarity.weight + luckWeight);
	}

	public static double resolveWeight(Tier tier, ServerPlayer player, boolean useMadokuLuck) {
		return resolveWeight(tier, resolveLuckStat(player, useMadokuLuck), useMadokuLuck && player != null);
	}

	public static double resolveLuckStat(ServerPlayer player, boolean enabled) {
		if (player == null || !enabled) return 0.0D;
		double luckStat;
		try { luckStat = luckStatResolver.applyAsDouble(player); }
		catch (RuntimeException exception) { luckStat = 0.0D; }
		return Double.isFinite(luckStat) ? luckStat : 0.0D;
	}

	public static double resolveWeightMultiplier(Tier tier, double luckStat, boolean useMadokuLuck) {
		RarityConfigManager.RaritySettings rarity = RarityConfigManager.settings(tier);
		if (rarity == null || !rarity.enabled || rarity.weight <= 0) return 0.0D;
		return resolveWeight(tier, luckStat, useMadokuLuck) / rarity.weight;
	}
}
