package madoku.craft.java.core.data;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Public contract for persistent chunk-position provenance data. */
public final class ChunkDataAPIManager {
	private static final ChunkDataProvider UNAVAILABLE_PROVIDER = new ChunkDataProvider() { };
	private static volatile ChunkDataProvider provider = UNAVAILABLE_PROVIDER;

	private ChunkDataAPIManager() { }

	public static void registerProvider(ChunkDataProvider candidate) {
		if (candidate == null) throw new IllegalArgumentException("Chunk data provider must not be null.");
		provider = candidate;
	}
	public static void unregisterProvider() { provider = UNAVAILABLE_PROVIDER; }
	public static void initialize() { provider.initialize(); }
	public static void reset() { provider.reset(); }
	public static void loadPersistedData(MinecraftServer server) { provider.loadPersistedData(server); }
	public static void autosavePersistedData(MinecraftServer server) { provider.autosavePersistedData(server); }
	public static void savePersistedData(MinecraftServer server) { provider.savePersistedData(server); }
	public static void recordPlayerPlacedBlock(ServerLevel level, BlockPos pos) { provider.recordPlayerPlacedBlock(level, pos); }
	public static boolean isPlayerPlacedBlock(ServerLevel level, BlockPos pos) { return provider.isPlayerPlacedBlock(level, pos); }
	public static void removePlayerPlacedBlock(ServerLevel level, BlockPos pos) { provider.removePlayerPlacedBlock(level, pos); }
	public static void recordPlayerModifiedContainerChange(ServerLevel level, BlockPos pos, int slot, ItemStack before, ItemStack after) { provider.recordPlayerModifiedContainerChange(level, pos, slot, before, after); }
	public static boolean isPlayerModifiedContainer(ServerLevel level, BlockPos pos) { return provider.isPlayerModifiedContainer(level, pos); }
	public static boolean isPlayerModifiedContainerSlot(ServerLevel level, BlockPos pos, int slot) { return provider.isPlayerModifiedContainerSlot(level, pos, slot); }
	public static void removePlayerModifiedContainer(ServerLevel level, BlockPos pos) { provider.removePlayerModifiedContainer(level, pos); }
	public static int consumePlayerModifiedContainerItem(ServerLevel level, BlockPos pos, ItemStack stack, int maximum) { return provider.consumePlayerModifiedContainerItem(level, pos, stack, maximum); }
}
