package madoku.craft.java.core.item;

public interface ItemEntityDropAccess {
	boolean madokuCraft$isPlayerDropped();
	boolean madokuCraft$isPlayerPlacedBlockDrop();
	boolean madokuCraft$isPlayerModifiedContainerDrop();
	int madokuCraft$getPlayerAttributedCount();
	void madokuCraft$setPlayerDropped(boolean value);
	void madokuCraft$setPlayerPlacedBlockDrop(boolean value);
	void madokuCraft$setPlayerModifiedContainerDrop(boolean value);
	void madokuCraft$setPlayerAttributedCount(int value);
}
