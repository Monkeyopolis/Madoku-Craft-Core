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
		boolean activeContainer = BlockDropContextAPIManager.isActiveDropContainer();
		boolean isContainerBlockItem = context != null && stack.is(context.state().getBlock().asItem());
		if (activePlayerPlacedBlock && (!activeContainer || isContainerBlockItem)) {
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
	private void madokuCraft$propagateProvenance(ItemEntity other, CallbackInfo ci) {
		if (!(other instanceof ItemEntityDropAccess otherAccess)) return;
		int currentAttributedCount = madokuCraft$playerAttributedCount;
		int otherAttributedCount = otherAccess.madokuCraft$getPlayerAttributedCount();

		boolean playerDropped = madokuCraft$playerDropped || otherAccess.madokuCraft$isPlayerDropped();
		boolean playerPlacedBlockDrop = madokuCraft$playerPlacedBlockDrop || otherAccess.madokuCraft$isPlayerPlacedBlockDrop();
		boolean playerModifiedContainerDrop = madokuCraft$playerModifiedContainerDrop
			|| otherAccess.madokuCraft$isPlayerModifiedContainerDrop();
		int playerAttributedCount = currentAttributedCount + otherAttributedCount;
		madokuCraft$playerDropped = playerDropped;
		madokuCraft$playerPlacedBlockDrop = playerPlacedBlockDrop;
		madokuCraft$playerModifiedContainerDrop = playerModifiedContainerDrop;
		madokuCraft$playerAttributedCount = playerAttributedCount;
		otherAccess.madokuCraft$setPlayerDropped(playerDropped);
		otherAccess.madokuCraft$setPlayerPlacedBlockDrop(playerPlacedBlockDrop);
		otherAccess.madokuCraft$setPlayerModifiedContainerDrop(playerModifiedContainerDrop);
		otherAccess.madokuCraft$setPlayerAttributedCount(playerAttributedCount);
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
