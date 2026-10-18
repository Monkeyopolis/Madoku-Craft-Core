package madoku.craft.mixin.core;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Quantizes final damage after armor and magic absorption to a tenth of a health point. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageQuantizationMixin {
	private static final float DAMAGE_STEP = 0.1F;

	@Inject(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"), cancellable = true)
	private void madokuCraft$quantizeFinalDamage(
		DamageSource source,
		float amount,
		CallbackInfoReturnable<Float> callbackInfo
	) {
		float damage = Math.max(0.0F, callbackInfo.getReturnValue());
		callbackInfo.setReturnValue(Math.round(damage / DAMAGE_STEP) * DAMAGE_STEP);
	}
}
