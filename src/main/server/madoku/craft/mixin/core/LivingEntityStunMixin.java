package madoku.craft.mixin.core;

import madoku.craft.java.core.status.EntityStunManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityStunMixin {
	@ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
	private Vec3 madokuCraft$blockStunnedMovementInput(Vec3 travelVector) {
		LivingEntity entity = (LivingEntity) (Object) this;
		return EntityStunManager.isStunned(entity) ? Vec3.ZERO : travelVector;
	}

	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void madokuCraft$preventStunnedEntityAttack(
		ServerLevel level,
		DamageSource source,
		float amount,
		CallbackInfoReturnable<Boolean> cir
	) {
		if (source != null && source.getEntity() instanceof LivingEntity attacker) {
			boolean stunned = EntityStunManager.isStunned(attacker);
			if (stunned) {
				cir.setReturnValue(false);
			}
		}
	}
}
