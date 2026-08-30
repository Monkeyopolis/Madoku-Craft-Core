package madoku.craft.mixin.core;

import madoku.craft.core.enchant.EnchantBooksManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies Core enchantment on-hit effects independently of mob aggro behavior. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityEnchantmentOnHitMixin {
	@Inject(
		method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
		at = @At("RETURN")
	)
	private void madokuCraft$applyConfiguredOnHit(
		ServerLevel level,
		DamageSource source,
		float amount,
		CallbackInfoReturnable<Boolean> callbackInfo
	) {
		if (callbackInfo.getReturnValueZ()) {
			EnchantBooksManager.applyOnHit((LivingEntity) (Object) this, source);
		}
	}
}
