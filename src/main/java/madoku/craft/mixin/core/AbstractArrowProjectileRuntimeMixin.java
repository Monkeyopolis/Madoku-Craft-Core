package madoku.craft.mixin.core;

import madoku.craft.core.helper.HelperProjectileManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Connects Core projectile runtime state to vanilla arrow hit handling. */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowProjectileRuntimeMixin {
	@Shadow
	protected abstract void doKnockback(LivingEntity target, DamageSource source);

	@Redirect(
		method = "onHitEntity",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
		)
	)
	@SuppressWarnings("deprecation")
	private boolean madokuCraft$applyCoreProjectileDamage(
		Entity entity,
		DamageSource source,
		float originalDamage
	) {
		AbstractArrow arrow = (AbstractArrow) (Object) this;
		if (HelperProjectileManager.shouldBypassInvulnerability(arrow) && entity instanceof LivingEntity livingEntity) {
			livingEntity.invulnerableTime = 0;
			livingEntity.hurtTime = 0;
		}
		float resolvedDamage = HelperProjectileManager.resolveProjectileDamageOverride(arrow, originalDamage);
		boolean hit = entity.hurtOrSimulate(source, resolvedDamage);
		if (hit && HelperProjectileManager.isManagedHomingProjectile(arrow)) {
			HelperProjectileManager.clearProjectileHoming(arrow);
		}
		HelperProjectileManager.clearInvulnerabilityBypass(arrow);
		return hit;
	}

	@Redirect(
		method = "onHitEntity",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;doKnockback(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)V"
		)
	)
	private void madokuCraft$skipCoreHomingArrowKnockback(
		AbstractArrow arrow,
		LivingEntity target,
		DamageSource source
	) {
		if (!HelperProjectileManager.isManagedHomingProjectile(arrow)) {
			this.doKnockback(target, source);
		}
	}
}
