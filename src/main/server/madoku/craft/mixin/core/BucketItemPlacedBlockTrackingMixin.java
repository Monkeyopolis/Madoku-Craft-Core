package madoku.craft.mixin.core;

import madoku.craft.java.core.data.ChunkDataAPIManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Persists bucket-placed fluid positions for systems that exclude player-created sources. */
@Mixin(BucketItem.class)
public abstract class BucketItemPlacedBlockTrackingMixin {
	@Inject(method = "emptyContents", at = @At("RETURN"))
	private void madokuCraft$trackPlacedFluid(
		LivingEntity entity,
		Level level,
		BlockPos pos,
		BlockHitResult hitResult,
		CallbackInfoReturnable<Boolean> cir
	) {
		if (!(level instanceof ServerLevel serverLevel)
			|| !(entity instanceof ServerPlayer)
			|| !Boolean.TRUE.equals(cir.getReturnValue())
			|| pos == null
			|| serverLevel.getFluidState(pos).isEmpty()) {
			return;
		}
		ChunkDataAPIManager.recordPlayerPlacedBlock(serverLevel, pos);
	}
}
