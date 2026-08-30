package madoku.craft.mixin.core;

import madoku.craft.core.enchant.EnchantBooksManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies Core's configured protection values without importing non-Core armor systems. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityEnchantmentDamageMixin {
	@Inject(method = "getDamageAfterArmorAbsorb", at = @At("RETURN"), cancellable = true)
	private void madokuCraft$applyConfiguredPostArmorEnchantments(
		DamageSource source,
		float amount,
		CallbackInfoReturnable<Float> callbackInfo
	) {
		LivingEntity entity = (LivingEntity) (Object) this;
		float damageAfterArmor = EnchantBooksManager.applyConfiguredSmiteVulnerability(
			entity,
			source,
			callbackInfo.getReturnValue()
		);
		EnchantBooksManager.capturePostArmorDamage(damageAfterArmor);
		callbackInfo.setReturnValue(damageAfterArmor);
	}

	@Redirect(
		method = "getDamageAfterMagicAbsorb",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getDamageProtection(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)F"
		)
	)
	private float madokuCraft$resolveConfiguredDamageProtection(
		ServerLevel serverLevel,
		LivingEntity entity,
		DamageSource source
	) {
		return EnchantBooksManager.resolveDamageProtection(serverLevel, entity, source);
	}

	@Redirect(
		method = "getDamageAfterMagicAbsorb",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterMagicAbsorb(FF)F"
		)
	)
	private float madokuCraft$applyConfiguredDamageReduction(float damage, float vanillaProtection) {
		return EnchantBooksManager.applyConfiguredDamageReduction(damage, vanillaProtection);
	}
}
