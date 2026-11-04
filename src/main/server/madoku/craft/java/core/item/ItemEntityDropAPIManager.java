package madoku.craft.java.core.item;

import net.minecraft.world.entity.item.ItemEntity;

/** Shared item-entity provenance API for systems that exclude player-sourced drops. */
public final class ItemEntityDropAPIManager {
	private ItemEntityDropAPIManager() { }

	public static boolean isPlayerDropped(ItemEntity itemEntity) {
		return itemEntity instanceof ItemEntityDropAccess access && access.madokuCraft$isPlayerDropped();
	}

	public static boolean isPlayerPlacedBlockDrop(ItemEntity itemEntity) {
		return itemEntity instanceof ItemEntityDropAccess access && access.madokuCraft$isPlayerPlacedBlockDrop();
	}

	public static boolean isPlayerModifiedContainerDrop(ItemEntity itemEntity) {
		return itemEntity instanceof ItemEntityDropAccess access && access.madokuCraft$isPlayerModifiedContainerDrop();
	}

	public static boolean isPlayerAttributedDrop(ItemEntity itemEntity) {
		return itemEntity instanceof ItemEntityDropAccess access && access.madokuCraft$getPlayerAttributedCount() > 0;
	}

	public static int consumePlayerAttributedAmount(ItemEntity itemEntity, int maximum) {
		if (maximum <= 0 || !(itemEntity instanceof ItemEntityDropAccess access)) return 0;
		int excluded = Math.min(maximum, Math.max(0, access.madokuCraft$getPlayerAttributedCount()));
		if (excluded > 0) {
			access.madokuCraft$setPlayerAttributedCount(access.madokuCraft$getPlayerAttributedCount() - excluded);
		}
		return excluded;
	}
}
