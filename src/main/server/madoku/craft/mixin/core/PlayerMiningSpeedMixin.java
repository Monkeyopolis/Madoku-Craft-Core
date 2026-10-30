package madoku.craft.mixin.core;

import madoku.craft.java.core.attribute.PlayerAttributeAPIManager;
import madoku.craft.java.core.attribute.PlayerAttributeStat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies all registered additive mining-speed bonuses in one place. */
@Mixin(Player.class)
public abstract class PlayerMiningSpeedMixin {
	@Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
	private void madokuCraft$applyMiningSpeedBonuses(BlockState state, CallbackInfoReturnable<Float> callbackInfo) {
		Player player = (Player) (Object) this;
		float speedBeforeBonuses = callbackInfo.getReturnValue();
		double bonus = PlayerAttributeAPIManager.resolveBonus(player, PlayerAttributeStat.MINING, state);
		float speedAfterBonuses = speedBeforeBonuses + (float) bonus;
		if (bonus <= 0.0D) return;
		callbackInfo.setReturnValue(speedAfterBonuses);
	}
}
