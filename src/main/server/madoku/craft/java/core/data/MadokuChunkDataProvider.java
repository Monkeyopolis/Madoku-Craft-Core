package madoku.craft.java.core.data;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Built-in provider backed by the Madoku chunk-data implementation. */
public final class MadokuChunkDataProvider implements ChunkDataProvider {
	@Override public void initialize() { MadokuChunkDataManager.initialize(); }
	@Override public void reset() { MadokuChunkDataManager.reset(); }
	@Override public void loadPersistedData(MinecraftServer server) { MadokuChunkDataManager.loadPersistedData(server); }
	@Override public void autosavePersistedData(MinecraftServer server) { MadokuChunkDataManager.autosavePersistedData(server); }
	@Override public void savePersistedData(MinecraftServer server) { MadokuChunkDataManager.savePersistedData(server); }
	@Override public void recordPlayerPlacedBlock(ServerLevel level, BlockPos pos) { MadokuChunkDataManager.recordPlayerPlacedBlock(level, pos); }
	@Override public boolean isPlayerPlacedBlock(ServerLevel level, BlockPos pos) { return MadokuChunkDataManager.isPlayerPlacedBlock(level, pos); }
	@Override public void removePlayerPlacedBlock(ServerLevel level, BlockPos pos) { MadokuChunkDataManager.removePlayerPlacedBlock(level, pos); }
	@Override public void recordPlayerModifiedContainerChange(ServerLevel level, BlockPos pos, int slot, ItemStack before, ItemStack after) { MadokuChunkDataManager.recordPlayerModifiedContainerChange(level, pos, slot, before, after); }
	@Override public boolean isPlayerModifiedContainer(ServerLevel level, BlockPos pos) { return MadokuChunkDataManager.isPlayerModifiedContainer(level, pos); }
	@Override public boolean isPlayerModifiedContainerSlot(ServerLevel level, BlockPos pos, int slot) { return MadokuChunkDataManager.isPlayerModifiedContainerSlot(level, pos, slot); }
	@Override public void removePlayerModifiedContainer(ServerLevel level, BlockPos pos) { MadokuChunkDataManager.removePlayerModifiedContainer(level, pos); }
	@Override public int consumePlayerModifiedContainerItem(ServerLevel level, BlockPos pos, ItemStack stack, int maximum) { return MadokuChunkDataManager.consumePlayerModifiedContainerItem(level, pos, stack, maximum); }
}
