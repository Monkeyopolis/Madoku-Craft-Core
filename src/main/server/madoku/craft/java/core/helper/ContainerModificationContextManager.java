package madoku.craft.java.core.helper;

import madoku.craft.java.core.data.ChunkDataAPIManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;

import java.util.ArrayList;
import java.util.List;

/** Identifies container mutations caused by a server player using an inventory menu. */
public final class ContainerModificationContextManager {
	private static final ThreadLocal<Context> ACTIVE_CONTEXT = new ThreadLocal<>();

	private ContainerModificationContextManager() { }

	public static void begin(AbstractContainerMenu menu, Player player) {
		if (menu == null || !(player instanceof ServerPlayer serverPlayer)) {
			ACTIVE_CONTEXT.remove();
			return;
		}
		ACTIVE_CONTEXT.set(new Context(menu, serverPlayer, snapshot(menu)));
	}

	public static void end() {
		Context context = ACTIVE_CONTEXT.get();
		if (context != null) recordSnapshotChanges(context);
		ACTIVE_CONTEXT.remove();
	}

	public static boolean isPlayerModifying(Container container) {
		Context context = ACTIVE_CONTEXT.get();
		if (context == null || container == null) return false;
		for (Slot slot : context.menu().slots) {
			if (slot.container == container) return true;
		}
		return false;
	}

	private static List<ContainerSlotSnapshot> snapshot(AbstractContainerMenu menu) {
		List<ContainerSlotSnapshot> snapshots = new ArrayList<>();
		for (Slot slot : menu.slots) {
			if (!(slot.container instanceof BaseContainerBlockEntity container)) continue;
			snapshots.add(new ContainerSlotSnapshot(container, slot.getContainerSlot(), slot.getItem().copy()));
		}
		return snapshots;
	}

	private static void recordSnapshotChanges(Context context) {
		for (ContainerSlotSnapshot snapshot : context.snapshots()) {
			BaseContainerBlockEntity container = snapshot.container();
			if (!(container.getLevel() instanceof ServerLevel level)) continue;

			int slot = snapshot.slot();
			if (slot < 0 || slot >= container.getContainerSize()) continue;
			ItemStack after = container.getItem(slot).copy();
			if (sameStack(snapshot.before(), after)) continue;

			ChunkDataAPIManager.recordPlayerModifiedContainerChange(
				level,
				container.getBlockPos(),
				slot,
				snapshot.before(),
				after
			);
		}
	}

	private static boolean sameStack(ItemStack left, ItemStack right) {
		if (left == null || left.isEmpty()) return right == null || right.isEmpty();
		if (right == null || right.isEmpty()) return false;
		return left.getCount() == right.getCount() && ItemStack.isSameItemSameComponents(left, right);
	}

	private record Context(
		AbstractContainerMenu menu,
		ServerPlayer player,
		List<ContainerSlotSnapshot> snapshots
	) { }

	private record ContainerSlotSnapshot(
		BaseContainerBlockEntity container,
		int slot,
		ItemStack before
	) { }
}
