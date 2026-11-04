package madoku.craft.mixin.core;

import madoku.craft.java.core.helper.BlockDropContextAPIManager;
import madoku.craft.java.core.item.ItemEntityDropAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Persists the player provenance of item entities without modifying their ItemStacks. */
@Mixin(ItemEntity.class)
public abstract class ItemEntityDropTrackingMixin implements ItemEntityDropAccess {
	@Unique
	private static final String MADOKU_CRAFT_PLAYER_DROPPED = "MadokuCraftPlayerDropped";
	@Unique
	private static final String MADOKU_CRAFT_PLAYER_PLACED_BLOCK_DROP = "MadokuCraftPlayerPlacedBlockDrop";
	@Unique
	private static final String MADOKU_CRAFT_PLAYER_MODIFIED_CONTAINER_DROP = "MadokuCraftPlayerModifiedContainerDrop";
	@Unique
	private static final String MADOKU_CRAFT_PLAYER_ATTRIBUTED_COUNT = "MadokuCraftPlayerAttributedCount";

	@Unique
	private boolean madokuCraft$playerDropped;
	@Unique
	private boolean madokuCraft$playerPlacedBlockDrop;
	@Unique
	private boolean madokuCraft$playerModifiedContainerDrop;
	@Unique
	private int madokuCraft$playerAttributedCount;
	@Unique
	private ItemEntity madokuCraft$mergeOther;
	@Unique
	private int madokuCraft$mergeThisInitialCount;
	@Unique
	private int madokuCraft$mergeOtherInitialCount;
	@Unique
	private int madokuCraft$mergeThisInitialAttributedCount;
	@Unique
	private int madokuCraft$mergeOtherInitialAttributedCount;

	@Inject(method = "<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"))
	private void madokuCraft$markBlockDropProvenance(
		Level level,
		double x,
		double y,
		double z,
		ItemStack stack,
		CallbackInfo ci
	) {
		madokuCraft$markBlockDropProvenance(stack);
	}

	@Inject(method = "<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;DDD)V", at = @At("RETURN"))
	private void madokuCraft$markBlockDropProvenanceWithVelocity(
		Level level,
		double x,
		double y,
		double z,
		ItemStack stack,
		double velocityX,
		double velocityY,
		double velocityZ,
		CallbackInfo ci
	) {
		madokuCraft$markBlockDropProvenance(stack);
	}

	@Inject(method = "setItem(Lnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"))
	private void madokuCraft$markSetItemProvenance(ItemStack stack, CallbackInfo ci) {
		madokuCraft$markBlockDropProvenance(stack);
	}

	@Unique
	private void madokuCraft$markBlockDropProvenance(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return;
		if (madokuCraft$playerAttributedCount > 0) return;
		BlockDropContextAPIManager.Context context = BlockDropContextAPIManager.current();
		boolean activePlayerPlacedBlock = BlockDropContextAPIManager.isActiveDropPlayerPlacedBlock();
		boolean isSourceBlockItem = context != null && stack.is(context.state().getBlock().asItem());
		if (activePlayerPlacedBlock && isSourceBlockItem) {
			madokuCraft$playerPlacedBlockDrop = true;
			madokuCraft$playerAttributedCount = stack.getCount();
			return;
		}
		int modifiedContainerAmount = BlockDropContextAPIManager.consumeActiveDropPlayerModifiedContainerAmount(stack);
		if (modifiedContainerAmount > 0) {
			madokuCraft$playerModifiedContainerDrop = true;
			madokuCraft$playerAttributedCount = Math.min(stack.getCount(), modifiedContainerAmount);
		}
	}

	@Inject(method = "setThrower", at = @At("RETURN"))
	private void madokuCraft$markPlayerDrop(Entity thrower, CallbackInfo ci) {
		if (thrower instanceof Player) {
			madokuCraft$playerDropped = true;
			madokuCraft$playerAttributedCount = ((ItemEntity) (Object) this).getItem().getCount();
		}
	}

	@Inject(method = "tryToMerge(Lnet/minecraft/world/entity/item/ItemEntity;)V", at = @At("HEAD"))
	private void madokuCraft$beginMergeTracking(ItemEntity other, CallbackInfo ci) {
		if (!(other instanceof ItemEntityDropAccess otherAccess)) {
			madokuCraft$clearMergeTracking();
			return;
		}
		madokuCraft$mergeOther = other;
		madokuCraft$mergeThisInitialCount = ((ItemEntity) (Object) this).getItem().getCount();
		madokuCraft$mergeOtherInitialCount = other.getItem().getCount();
		madokuCraft$mergeThisInitialAttributedCount = Math.min(
			madokuCraft$mergeThisInitialCount,
			Math.max(0, madokuCraft$playerAttributedCount)
		);
		madokuCraft$mergeOtherInitialAttributedCount = Math.min(
			madokuCraft$mergeOtherInitialCount,
			Math.max(0, otherAccess.madokuCraft$getPlayerAttributedCount())
		);
	}

	@Inject(method = "tryToMerge(Lnet/minecraft/world/entity/item/ItemEntity;)V", at = @At("RETURN"))
	private void madokuCraft$finishMergeTracking(ItemEntity other, CallbackInfo ci) {
		if (madokuCraft$mergeOther != other) return;
		try {
			ItemEntity thisEntity = (ItemEntity) (Object) this;
			if (!(other instanceof ItemEntityDropAccess otherAccess)) return;
			ItemEntityDropAccess thisAccess = (ItemEntityDropAccess) (Object) thisEntity;
			int thisFinalCount = thisEntity.getItem().getCount();
			int otherFinalCount = other.getItem().getCount();
			int movedToThis = Math.max(0, thisFinalCount - madokuCraft$mergeThisInitialCount);
			int movedFromThis = Math.max(0, madokuCraft$mergeThisInitialCount - thisFinalCount);
			int movedToOther = Math.max(0, otherFinalCount - madokuCraft$mergeOtherInitialCount);
			int movedFromOther = Math.max(0, madokuCraft$mergeOtherInitialCount - otherFinalCount);

			if (movedToThis > 0 && movedFromOther > 0) {
				madokuCraft$applyMergeAttribution(
					otherAccess,
					thisAccess,
					madokuCraft$mergeOtherInitialAttributedCount,
					madokuCraft$mergeThisInitialAttributedCount,
					otherFinalCount,
					thisFinalCount
				);
			} else if (movedToOther > 0 && movedFromThis > 0) {
				madokuCraft$applyMergeAttribution(
					thisAccess,
					otherAccess,
					madokuCraft$mergeThisInitialAttributedCount,
					madokuCraft$mergeOtherInitialAttributedCount,
					thisFinalCount,
					otherFinalCount
				);
			}
		} finally {
			madokuCraft$clearMergeTracking();
		}
	}

	@Unique
	private static void madokuCraft$applyMergeAttribution(
		ItemEntityDropAccess source,
		ItemEntityDropAccess destination,
		int sourceInitialAttributedCount,
		int destinationInitialAttributedCount,
		int sourceFinalCount,
		int destinationFinalCount
	) {
		boolean sourceTainted = sourceInitialAttributedCount > 0;
		boolean destinationTainted = destinationInitialAttributedCount > 0;
		boolean mergedTainted = sourceTainted || destinationTainted;

		source.madokuCraft$setPlayerAttributedCount(sourceTainted ? sourceFinalCount : 0);
		destination.madokuCraft$setPlayerAttributedCount(mergedTainted ? destinationFinalCount : 0);
		if (mergedTainted) {
			destination.madokuCraft$setPlayerDropped(
				destination.madokuCraft$isPlayerDropped() || source.madokuCraft$isPlayerDropped()
			);
			destination.madokuCraft$setPlayerPlacedBlockDrop(
				destination.madokuCraft$isPlayerPlacedBlockDrop() || source.madokuCraft$isPlayerPlacedBlockDrop()
			);
			destination.madokuCraft$setPlayerModifiedContainerDrop(
				destination.madokuCraft$isPlayerModifiedContainerDrop()
					|| source.madokuCraft$isPlayerModifiedContainerDrop()
			);
		}
		if (!sourceTainted) {
			source.madokuCraft$setPlayerDropped(false);
			source.madokuCraft$setPlayerPlacedBlockDrop(false);
			source.madokuCraft$setPlayerModifiedContainerDrop(false);
		}
	}

	@Unique
	private void madokuCraft$clearMergeTracking() {
		madokuCraft$mergeOther = null;
		madokuCraft$mergeThisInitialCount = 0;
		madokuCraft$mergeOtherInitialCount = 0;
		madokuCraft$mergeThisInitialAttributedCount = 0;
		madokuCraft$mergeOtherInitialAttributedCount = 0;
	}

	@Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
	private void madokuCraft$saveProvenance(ValueOutput output, CallbackInfo ci) {
		if (madokuCraft$playerDropped) output.putBoolean(MADOKU_CRAFT_PLAYER_DROPPED, true);
		if (madokuCraft$playerPlacedBlockDrop) output.putBoolean(MADOKU_CRAFT_PLAYER_PLACED_BLOCK_DROP, true);
		if (madokuCraft$playerModifiedContainerDrop) output.putBoolean(MADOKU_CRAFT_PLAYER_MODIFIED_CONTAINER_DROP, true);
		if (madokuCraft$playerAttributedCount > 0) output.putInt(MADOKU_CRAFT_PLAYER_ATTRIBUTED_COUNT, madokuCraft$playerAttributedCount);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
	private void madokuCraft$loadProvenance(ValueInput input, CallbackInfo ci) {
		madokuCraft$playerDropped = input.getBooleanOr(MADOKU_CRAFT_PLAYER_DROPPED, false);
		madokuCraft$playerPlacedBlockDrop = input.getBooleanOr(MADOKU_CRAFT_PLAYER_PLACED_BLOCK_DROP, false);
		madokuCraft$playerModifiedContainerDrop = input.getBooleanOr(MADOKU_CRAFT_PLAYER_MODIFIED_CONTAINER_DROP, false);
		madokuCraft$playerAttributedCount = input.getIntOr(MADOKU_CRAFT_PLAYER_ATTRIBUTED_COUNT, 0);
	}

	@Override
	public boolean madokuCraft$isPlayerDropped() { return madokuCraft$playerDropped; }

	@Override
	public boolean madokuCraft$isPlayerPlacedBlockDrop() { return madokuCraft$playerPlacedBlockDrop; }

	@Override
	public boolean madokuCraft$isPlayerModifiedContainerDrop() { return madokuCraft$playerModifiedContainerDrop; }

	@Override
	public int madokuCraft$getPlayerAttributedCount() { return madokuCraft$playerAttributedCount; }

	@Override
	public void madokuCraft$setPlayerDropped(boolean value) { madokuCraft$playerDropped = value; }

	@Override
	public void madokuCraft$setPlayerPlacedBlockDrop(boolean value) { madokuCraft$playerPlacedBlockDrop = value; }

	@Override
	public void madokuCraft$setPlayerModifiedContainerDrop(boolean value) { madokuCraft$playerModifiedContainerDrop = value; }

	@Override
	public void madokuCraft$setPlayerAttributedCount(int value) { madokuCraft$playerAttributedCount = Math.max(0, value); }
}
