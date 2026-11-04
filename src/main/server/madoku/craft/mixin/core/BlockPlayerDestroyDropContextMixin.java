package madoku.craft.mixin.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import madoku.craft.java.core.data.ChunkDataAPIManager;
import madoku.craft.java.core.helper.BlockDropContextAPIManager;

@Mixin(Block.class)
public abstract class BlockPlayerDestroyDropContextMixin {
	@Inject(
		method = "playerWillDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/level/block/state/BlockState;",
		at = @At("HEAD")
	)
	private void madokuCraft$beginEarlyDropContext(
		Level level,
		BlockPos pos,
		BlockState state,
		Player player,
		CallbackInfoReturnable<BlockState> cir
	) {
		if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
			BlockDropContextAPIManager.begin(serverLevel, serverPlayer, pos, state, serverLevel.getBlockEntity(pos));
		}
	}

	@Inject(
		method = "playerDestroy(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/item/ItemStack;)V",
		at = @At("HEAD")
	)
	private void madokuCraft$beginPlayerDestroyDropContext(
		ServerLevel serverLevel,
		ServerPlayer serverPlayer,
		BlockPos pos,
		BlockState state,
		BlockEntity blockEntity,
		ItemStack tool,
		CallbackInfo ci
	) {
		BlockDropContextAPIManager.begin(serverLevel, serverPlayer, pos, state, blockEntity);
	}

	@Inject(
		method = "playerDestroy(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/item/ItemStack;)V",
		at = @At("RETURN")
	)
	private void madokuCraft$endPlayerDestroyDropContext(
		ServerLevel serverLevel,
		ServerPlayer serverPlayer,
		BlockPos pos,
		BlockState state,
		BlockEntity blockEntity,
		ItemStack tool,
		CallbackInfo ci
	) {
		BlockDropContextAPIManager.end();
		ChunkDataAPIManager.removePlayerPlacedBlock(serverLevel, pos);
		ChunkDataAPIManager.removePlayerModifiedContainer(serverLevel, pos);
	}
}

