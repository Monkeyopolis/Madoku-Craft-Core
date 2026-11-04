package madoku.craft.java.core.data;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Provider contract for persistent chunk-position provenance data. */
public interface ChunkDataProvider {
	default void initialize() { }
	default void reset() { }
	default void loadPersistedData(MinecraftServer server) { }
	default void autosavePersistedData(MinecraftServer server) { }
	default void savePersistedData(MinecraftServer server) { }
	default void recordPlayerPlacedBlock(ServerLevel level, BlockPos pos) { }
	default boolean isPlayerPlacedBlock(ServerLevel level, BlockPos pos) { return false; }
	default void removePlayerPlacedBlock(ServerLevel level, BlockPos pos) { }
	default void recordPlayerModifiedContainerChange(ServerLevel level, BlockPos pos, int slot, ItemStack before, ItemStack after) { }
	default boolean isPlayerModifiedContainer(ServerLevel level, BlockPos pos) { return false; }
	default boolean isPlayerModifiedContainerSlot(ServerLevel level, BlockPos pos, int slot) { return false; }
	default void removePlayerModifiedContainer(ServerLevel level, BlockPos pos) { }
	default int consumePlayerModifiedContainerItem(ServerLevel level, BlockPos pos, ItemStack stack, int maximum) { return 0; }
}
