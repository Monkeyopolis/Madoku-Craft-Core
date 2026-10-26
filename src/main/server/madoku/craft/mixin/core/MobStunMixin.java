package madoku.craft.mixin.core;

import madoku.craft.java.core.status.EntityStunManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobStunMixin {
	@Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
	private void madokuCraft$disableStunnedMobAi(CallbackInfo ci) {
		Mob mob = (Mob) (Object) this;
		if (!EntityStunManager.isStunned(mob)) {
			return;
		}
		Vec3 movement = mob.getDeltaMovement();
		mob.setDeltaMovement(new Vec3(0.0D, movement.y, 0.0D));
		mob.getNavigation().stop();
		mob.setTarget(null);
		ci.cancel();
	}

	@Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
	private void madokuCraft$disableStunnedMobAttack(
		ServerLevel level,
		Entity target,
		CallbackInfoReturnable<Boolean> cir
	) {
		if (EntityStunManager.isStunned((Mob) (Object) this)) {
			cir.setReturnValue(false);
		}
	}
}
