package madoku.craft.mixin.core;

import madoku.craft.java.core.data.ChunkDataAPIManager;
import madoku.craft.java.core.helper.ContainerModificationContextManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Persists that a player changed the contents of a block-backed container. */
@Mixin(BaseContainerBlockEntity.class)
public abstract class BaseContainerBlockEntityModificationMixin {
	@Inject(method = "setItem", at = @At("HEAD"))
	private void madokuCraft$recordSetItem(int slot, ItemStack stack, CallbackInfo ci) {
		BaseContainerBlockEntity container = (BaseContainerBlockEntity) (Object) this;
		madokuCraft$recordModification(container, slot, container.getItem(slot).copy(), stack);
	}

	@Inject(method = "removeItem", at = @At("HEAD"))
	private void madokuCraft$recordRemoveItem(int slot, int amount, CallbackInfoReturnable<ItemStack> cir) {
		BaseContainerBlockEntity container = (BaseContainerBlockEntity) (Object) this;
		ItemStack before = container.getItem(slot).copy();
		int remaining = Math.max(0, before.getCount() - Math.max(0, amount));
		ItemStack after = remaining == 0 ? ItemStack.EMPTY : before.copyWithCount(remaining);
		madokuCraft$recordModification(container, slot, before, after);
	}

	@Inject(method = "removeItemNoUpdate", at = @At("HEAD"))
	private void madokuCraft$recordRemoveItemNoUpdate(int slot, CallbackInfoReturnable<ItemStack> cir) {
		BaseContainerBlockEntity container = (BaseContainerBlockEntity) (Object) this;
		ItemStack before = container.getItem(slot).copy();
		madokuCraft$recordModification(container, slot, before, ItemStack.EMPTY);
	}

	private void madokuCraft$recordModification(BaseContainerBlockEntity container, int slot, ItemStack before, ItemStack after) {
		if (!(container.getLevel() instanceof ServerLevel serverLevel)
			|| !ContainerModificationContextManager.isPlayerModifying((Container) (Object) container)) {
			return;
		}
		ChunkDataAPIManager.recordPlayerModifiedContainerChange(serverLevel, container.getBlockPos(), slot, before, after);
	}
}
