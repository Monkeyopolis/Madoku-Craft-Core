package madoku.craft.java.core.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.LinkedHashMap;
import java.util.Map;

/** Built-in chunk-data provider backed by native persistent chunk attachments. */
public final class MadokuWorldChunkDataProvider implements WorldChunkDataProvider {
	@Override public void initialize() { WorldChunkDataRuntimeManager.initialize(); }
	@Override public void reset() { WorldChunkDataRuntimeManager.reset(); }
	@Override public boolean isInitialized() { return WorldChunkDataRuntimeManager.isInitialized(); }
	@Override public void loadPersistedData(MinecraftServer server) { WorldChunkDataRuntimeManager.loadPersistedData(server); }
	@Override public void onServerStarted(MinecraftServer server) { WorldChunkDataRuntimeManager.onServerStarted(server); }
	@Override public void autosavePersistedData(MinecraftServer server) { WorldChunkDataRuntimeManager.autosavePersistedData(server); }
	@Override public void onServerStopping(MinecraftServer server) { WorldChunkDataRuntimeManager.onServerStopping(server); }
	@Override public void savePersistedData(MinecraftServer server) { WorldChunkDataRuntimeManager.savePersistedData(server); }
	@Override public long getAutoSaveMinutes() { return WorldChunkDataRuntimeManager.getAutoSaveMinutes(); }
	@Override public long getAutoSaveIntervalTicks() { return WorldChunkDataRuntimeManager.getAutoSaveIntervalTicks(); }
	@Override public String dimensionId(ServerLevel level) { return WorldChunkDataRuntimeManager.dimensionId(level); }
	@Override public CompoundTag getChunkData(ServerLevel level, int chunkX, int chunkZ) { return WorldChunkDataRuntimeManager.getChunkData(level, chunkX, chunkZ); }
	@Override public void setChunkData(ServerLevel level, int chunkX, int chunkZ, CompoundTag data) { WorldChunkDataRuntimeManager.setChunkData(level, chunkX, chunkZ, data); }
	@Override public CompoundTag getChunkSystemData(ServerLevel level, int chunkX, int chunkZ, String systemId) { return WorldChunkDataRuntimeManager.getChunkSystemData(level, chunkX, chunkZ, systemId); }
	@Override public void setChunkSystemData(ServerLevel level, int chunkX, int chunkZ, String systemId, CompoundTag data) { WorldChunkDataRuntimeManager.setChunkSystemData(level, chunkX, chunkZ, systemId, data); }
	@Override public CompoundTag getChunkSystemData(WorldChunkDataKey key, String systemId) { return WorldChunkDataRuntimeManager.getChunkSystemData(toRuntimeKey(key), systemId); }
	@Override public void setChunkSystemData(WorldChunkDataKey key, String systemId, CompoundTag data) { WorldChunkDataRuntimeManager.setChunkSystemData(toRuntimeKey(key), systemId, data); }
	@Override public void removeChunkSystemData(WorldChunkDataKey key, String systemId) { WorldChunkDataRuntimeManager.removeChunkSystemData(toRuntimeKey(key), systemId); }
	@Override public Map<WorldChunkDataKey, CompoundTag> getAllChunkSystemData(String systemId) {
		Map<WorldChunkDataKey, CompoundTag> result = new LinkedHashMap<>();
		for (Map.Entry<WorldChunkDataRuntimeManager.ChunkDataKey, CompoundTag> entry : WorldChunkDataRuntimeManager.getAllChunkSystemData(systemId).entrySet()) {
			WorldChunkDataRuntimeManager.ChunkDataKey key = entry.getKey();
			result.put(new WorldChunkDataKey(key.dimensionId(), key.chunkX(), key.chunkZ()), entry.getValue());
		}
		return result;
	}

	private static WorldChunkDataRuntimeManager.ChunkDataKey toRuntimeKey(WorldChunkDataKey key) {
		return key == null ? null : new WorldChunkDataRuntimeManager.ChunkDataKey(key.dimensionId(), key.chunkX(), key.chunkZ());
	}
}
