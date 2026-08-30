package madoku.craft.mixin.core;

import madoku.craft.core.helper.HelperProjectileManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Connects Core projectile runtime state to vanilla trident hit handling. */
@Mixin(ThrownTrident.class)
public abstract class ThrownTridentProjectileRuntimeMixin {
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
		ThrownTrident trident = (ThrownTrident) (Object) this;
		float resolvedDamage = HelperProjectileManager.resolveProjectileDamageOverride(trident, originalDamage);
		boolean hit = entity.hurtOrSimulate(source, resolvedDamage);
		if (hit) {
			HelperProjectileManager.clearProjectileHoming(trident);
		}
		HelperProjectileManager.clearInvulnerabilityBypass(trident);
		return hit;
	}
}
