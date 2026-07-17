package madoku.craft.loot.system;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.atomic.AtomicReference;

public final class MadokuLootHooks {
	private static final LootHook NOOP = new LootHook() {
	};
	private static final AtomicReference<LootHook> ACTIVE = new AtomicReference<>(NOOP);

	private MadokuLootHooks() {
	}

	public static void install(LootHook hook) {
		ACTIVE.set(hook == null ? NOOP : hook);
	}

	public static LootHook current() {
		return ACTIVE.get();
	}

	public static boolean isLuckEnabled() {
		return current().isLuckEnabled();
	}

	public static double resolveLootLuckStat(ServerPlayer player) {
		return current().resolveLootLuckStat(player);
	}

	public static boolean isGroupTagEnabled(String tag) {
		return current().isGroupTagEnabled(tag);
	}

	public static void applyConfiguredRarity(ItemStack stack, MadokuLootRarity rarity) {
		current().applyConfiguredRarity(stack, rarity);
	}

	public static void applySupportedSpawnEggLore(ItemStack stack) {
		current().applySupportedSpawnEggLore(stack);
	}

	public interface LootHook {
		default boolean isLuckEnabled() {
			return false;
		}

		default double resolveLootLuckStat(ServerPlayer player) {
			return 0.0D;
		}

		default boolean isGroupTagEnabled(String tag) {
			return true;
		}

		default void applyConfiguredRarity(ItemStack stack, MadokuLootRarity rarity) {
		}

		default void applySupportedSpawnEggLore(ItemStack stack) {
		}
	}
}
