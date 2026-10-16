package madoku.craft.java.core.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;

/** Provider contract for indexed per-dimension chunk data. */
public interface WorldChunkDataProvider {
	default void initialize() { }
	default void reset() { }
	default boolean isInitialized() { return false; }
	default void loadPersistedData(MinecraftServer server) { }
	default void onServerStarted(MinecraftServer server) { }
	default void autosavePersistedData(MinecraftServer server) { }
	default void onServerStopping(MinecraftServer server) { }
	default void savePersistedData(MinecraftServer server) { }
	default long getAutoSaveMinutes() { return 0L; }
	default long getAutoSaveIntervalTicks() { return 0L; }
	default String dimensionId(ServerLevel level) { return ""; }
	default CompoundTag getChunkData(ServerLevel level, int chunkX, int chunkZ) { return new CompoundTag(); }
	default void setChunkData(ServerLevel level, int chunkX, int chunkZ, CompoundTag data) { }
	default CompoundTag getChunkSystemData(ServerLevel level, int chunkX, int chunkZ, String systemId) { return new CompoundTag(); }
	default void setChunkSystemData(ServerLevel level, int chunkX, int chunkZ, String systemId, CompoundTag data) { }
	default CompoundTag getChunkSystemData(WorldChunkDataKey key, String systemId) { return new CompoundTag(); }
	default void setChunkSystemData(WorldChunkDataKey key, String systemId, CompoundTag data) { }
	default void removeChunkSystemData(WorldChunkDataKey key, String systemId) { }
	default Map<WorldChunkDataKey, CompoundTag> getAllChunkSystemData(String systemId) { return Map.of(); }
}
