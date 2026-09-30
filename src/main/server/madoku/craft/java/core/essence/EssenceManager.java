package madoku.craft.java.core.essence;

import madoku.craft.java.core.loot.LootFeatureAPIManager;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Registers the shared Essence item and awards it for player-attributed mob kills. */
public final class EssenceManager {
	private static final Identifier ESSENCE_ID = Identifier.fromNamespaceAndPath("madoku-craft", "essence");
	public static final Item ESSENCE = Registry.register(
		BuiltInRegistries.ITEM,
		ESSENCE_ID,
		new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, ESSENCE_ID))) {
			@Override
			public boolean isFoil(ItemStack stack) {
				return true;
			}
		}
	);
	private static boolean initialized;

	private EssenceManager() {
	}

	public static void initialize() {
		if (initialized) return;
		initialized = true;
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.accept(ESSENCE));
		ServerLivingEntityEvents.AFTER_DEATH.register(EssenceManager::dropForPlayerAttributedKill);
	}

	public static ServerPlayer resolveDirectPlayer(DamageSource damageSource) {
		if (damageSource == null) return null;
		ServerPlayer player = asPlayer(damageSource.getEntity());
		if (player != null) return player;
		Entity directEntity = damageSource.getDirectEntity();
		player = asPlayer(directEntity);
		if (player != null) return player;
		if (directEntity instanceof Projectile projectile) {
			return asPlayer(projectile.getOwner());
		}
		return null;
	}

	private static void dropForPlayerAttributedKill(net.minecraft.world.entity.LivingEntity entity, DamageSource damageSource) {
		if (!(entity instanceof Mob mob) || !(entity.level() instanceof ServerLevel level)) return;
		if (LootFeatureAPIManager.resolvePlayerDamageSource(damageSource) == null) return;

		MobCategory category = mob.getType().getCategory();
		RandomSource random = level.getRandom();
		int amount;
		if (category == MobCategory.MONSTER) {
			if (random.nextFloat() >= 0.50F) return;
			amount = 1 + random.nextInt(3);
		} else {
			if (random.nextFloat() >= 0.25F) return;
			amount = random.nextInt(3);
		}
		if (amount <= 0) return;

		ItemEntity drop = new ItemEntity(level, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), new ItemStack(ESSENCE, amount));
		drop.setDefaultPickUpDelay();
		level.addFreshEntity(drop);
	}

	private static ServerPlayer asPlayer(Entity entity) {
		return entity instanceof ServerPlayer player ? player : null;
	}
}
