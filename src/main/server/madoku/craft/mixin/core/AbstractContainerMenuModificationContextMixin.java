package madoku.craft.mixin.core;

import madoku.craft.java.core.helper.ContainerModificationContextManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Establishes a short-lived context around server player inventory actions. */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuModificationContextMixin {
	@Inject(method = "clicked(IILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V", at = @At("HEAD"))
	private void madokuCraft$beginModificationContext(
		int slotId,
		int button,
		ContainerInput input,
		Player player,
		CallbackInfo ci
	) {
		ContainerModificationContextManager.begin((AbstractContainerMenu) (Object) this, player);
	}

	@Inject(method = "clicked(IILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V", at = @At("RETURN"))
	private void madokuCraft$endModificationContext(
		int slotId,
		int button,
		ContainerInput input,
		Player player,
		CallbackInfo ci
	) {
		ContainerModificationContextManager.end();
	}
}
