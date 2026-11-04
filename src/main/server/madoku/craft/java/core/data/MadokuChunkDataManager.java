package madoku.craft.java.core.data;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import madoku.craft.java.core.chunk.ChunkAPIManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Native per-chunk state for player provenance markers. */
public final class MadokuChunkDataManager {
	private static final String PLAYER_PLACED_BLOCKS_SYSTEM_ID = "player-placed-blocks";
	private static final String PLAYER_MODIFIED_CONTAINERS_SYSTEM_ID = "player-modified-containers";
	private static final String FIELD_PACKED_POSITIONS = "packed-positions";
	private static final String FIELD_MODIFIED_SLOTS = "modified-slots";
	private static final String FIELD_ITEM_ID = "item-id";
	private static final String FIELD_COUNT = "count";

	private static final TrackedPositionData PLAYER_PLACED_BLOCKS = new TrackedPositionData();
	private static final ModifiedContainerData PLAYER_MODIFIED_CONTAINERS = new ModifiedContainerData();
	private static final Set<ChunkRefKey> LOADED_CHUNKS = new LinkedHashSet<>();
	private static final Set<PendingRemoval> PENDING_REMOVALS = new LinkedHashSet<>();
	private static volatile boolean dirty;
	private static volatile long lastAutosaveBucket = Long.MIN_VALUE;
	private static volatile boolean eventHandlersRegistered;
	private static final ChunkAPIManager.ChunkLifecycleListener CHUNK_LISTENER = new ChunkAPIManager.ChunkLifecycleListener() {
		@Override
		public void onChunkLoaded(ServerLevel level, int chunkX, int chunkZ) {
			// Provenance data is loaded on first query.
		}

		@Override
		public void onChunkUnloaded(ServerLevel level, int chunkX, int chunkZ) {
			releaseChunk(level, chunkX, chunkZ);
		}
	};

	private MadokuChunkDataManager() { }

	public static void initialize() {
		ChunkAPIManager.registerChunkLifecycleListener(CHUNK_LISTENER);
		if (!eventHandlersRegistered) {
			PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
				if (world instanceof ServerLevel level) {
					PENDING_REMOVALS.add(new PendingRemoval(level, pos == null ? null : pos.immutable()));
				}
			});
			ServerTickEvents.END_SERVER_TICK.register(server -> flushPendingRemovals());
			eventHandlersRegistered = true;
		}
	}

	public static void reset() {
		PLAYER_PLACED_BLOCKS.clear();
		PLAYER_MODIFIED_CONTAINERS.clear();
		LOADED_CHUNKS.clear();
		PENDING_REMOVALS.clear();
		dirty = false;
		lastAutosaveBucket = Long.MIN_VALUE;
	}

	public static void loadPersistedData(MinecraftServer server) {
		if (server == null) return;

		PLAYER_PLACED_BLOCKS.clear();
		PLAYER_MODIFIED_CONTAINERS.clear();
		LOADED_CHUNKS.clear();
		PENDING_REMOVALS.clear();
		dirty = false;
		lastAutosaveBucket = Math.floorDiv(
			madoku.craft.java.core.time.TimeAPIManager.getGameplayTicks(),
			WorldChunkDataAPIManager.getAutoSaveIntervalTicks());
	}

	public static void autosavePersistedData(MinecraftServer server) {
		if (server == null) return;
		long bucket = Math.floorDiv(
			madoku.craft.java.core.time.TimeAPIManager.getGameplayTicks(),
			WorldChunkDataAPIManager.getAutoSaveIntervalTicks());
		if (bucket == lastAutosaveBucket) return;
		lastAutosaveBucket = bucket;
		if (dirty) savePersistedData(server);
	}

	public static void savePersistedData(MinecraftServer server) {
		if (server == null) return;
		Set<ChunkRefKey> dirtyChunkKeys = PLAYER_PLACED_BLOCKS.collectDirtyChunkKeys();
		dirtyChunkKeys.addAll(PLAYER_MODIFIED_CONTAINERS.collectDirtyChunkKeys());
		for (ChunkRefKey chunkKey : dirtyChunkKeys) {
			WorldChunkDataKey dataKey = new WorldChunkDataKey(
				chunkKey.levelId(), chunkKey.chunkX(), chunkKey.chunkZ());
			saveChunkSystemData(dataKey, PLAYER_PLACED_BLOCKS_SYSTEM_ID, PLAYER_PLACED_BLOCKS.createChunkPersistedData(chunkKey));
			saveChunkSystemData(dataKey, PLAYER_MODIFIED_CONTAINERS_SYSTEM_ID, PLAYER_MODIFIED_CONTAINERS.createChunkPersistedData(chunkKey));
		}
		PLAYER_PLACED_BLOCKS.clearDirtyChunkKeys();
		PLAYER_MODIFIED_CONTAINERS.clearDirtyChunkKeys();
		dirty = false;
	}

	public static void recordPlayerPlacedBlock(ServerLevel level, BlockPos pos) {
		ensureChunkLoaded(level, pos);
		if (level != null && pos != null && PLAYER_PLACED_BLOCKS.record(level, pos)) dirty = true;
	}

	public static boolean isPlayerPlacedBlock(ServerLevel level, BlockPos pos) {
		ensureChunkLoaded(level, pos);
		return level != null && pos != null && PLAYER_PLACED_BLOCKS.contains(level, pos);
	}

	public static void removePlayerPlacedBlock(ServerLevel level, BlockPos pos) {
		ensureChunkLoaded(level, pos);
		if (level != null && pos != null && PLAYER_PLACED_BLOCKS.remove(level, pos)) dirty = true;
	}

	public static void recordPlayerModifiedContainerChange(ServerLevel level, BlockPos pos, int slot, ItemStack before, ItemStack after) {
		ensureChunkLoaded(level, pos);
		if (level != null && pos != null && PLAYER_MODIFIED_CONTAINERS.recordChange(level, pos, slot, before, after)) dirty = true;
	}

	public static boolean isPlayerModifiedContainer(ServerLevel level, BlockPos pos) {
		ensureChunkLoaded(level, pos);
		return level != null && pos != null && PLAYER_MODIFIED_CONTAINERS.contains(level, pos);
	}

	public static boolean isPlayerModifiedContainerSlot(ServerLevel level, BlockPos pos, int slot) {
		ensureChunkLoaded(level, pos);
		return level != null && pos != null && PLAYER_MODIFIED_CONTAINERS.containsSlot(level, pos, slot);
	}

	public static void removePlayerModifiedContainer(ServerLevel level, BlockPos pos) {
		ensureChunkLoaded(level, pos);
		if (level != null && pos != null && PLAYER_MODIFIED_CONTAINERS.remove(level, pos)) dirty = true;
	}

	public static int consumePlayerModifiedContainerItem(ServerLevel level, BlockPos pos, ItemStack stack, int maximum) {
		ensureChunkLoaded(level, pos);
		if (level == null || pos == null || stack == null || stack.isEmpty() || maximum <= 0) return 0;
		int consumed = PLAYER_MODIFIED_CONTAINERS.consume(level, pos, stack, maximum);
		if (consumed > 0) dirty = true;
		return consumed;
	}

	private static void flushPendingRemovals() {
		if (PENDING_REMOVALS.isEmpty()) {
			return;
		}
		Set<PendingRemoval> pending = new LinkedHashSet<>(PENDING_REMOVALS);
		PENDING_REMOVALS.removeAll(pending);
		for (PendingRemoval removal : pending) {
			if (removal != null && removal.level() != null && removal.pos() != null) {
				removePlayerPlacedBlock(removal.level(), removal.pos());
			}
		}
	}

	private static void ensureChunkLoaded(ServerLevel level, BlockPos pos) {
		if (level == null || pos == null) return;
		ChunkRefKey chunkKey = new ChunkRefKey(levelId(level), pos.getX() >> 4, pos.getZ() >> 4);
		if (!LOADED_CHUNKS.add(chunkKey)) return;
		CompoundTag placedBlockData = WorldChunkDataAPIManager.getChunkSystemData(
			level,
			chunkKey.chunkX(),
			chunkKey.chunkZ(),
			PLAYER_PLACED_BLOCKS_SYSTEM_ID);
		PLAYER_PLACED_BLOCKS.applyChunkPersistedData(placedBlockData, chunkKey);
		CompoundTag modifiedContainerData = WorldChunkDataAPIManager.getChunkSystemData(
			level,
			chunkKey.chunkX(),
			chunkKey.chunkZ(),
			PLAYER_MODIFIED_CONTAINERS_SYSTEM_ID);
		PLAYER_MODIFIED_CONTAINERS.applyChunkPersistedData(modifiedContainerData, chunkKey);
	}

	private static void releaseChunk(ServerLevel level, int chunkX, int chunkZ) {
		if (level == null) return;
		ChunkRefKey chunkKey = new ChunkRefKey(levelId(level), chunkX, chunkZ);
		if (PLAYER_PLACED_BLOCKS.isDirty(chunkKey) || PLAYER_MODIFIED_CONTAINERS.isDirty(chunkKey)) {
			WorldChunkDataKey dataKey = new WorldChunkDataKey(
				chunkKey.levelId(), chunkKey.chunkX(), chunkKey.chunkZ());
			saveChunkSystemData(dataKey, PLAYER_PLACED_BLOCKS_SYSTEM_ID, PLAYER_PLACED_BLOCKS.createChunkPersistedData(chunkKey));
			saveChunkSystemData(dataKey, PLAYER_MODIFIED_CONTAINERS_SYSTEM_ID, PLAYER_MODIFIED_CONTAINERS.createChunkPersistedData(chunkKey));
			PLAYER_PLACED_BLOCKS.clearDirtyChunkKey(chunkKey);
			PLAYER_MODIFIED_CONTAINERS.clearDirtyChunkKey(chunkKey);
			dirty = PLAYER_PLACED_BLOCKS.hasDirtyChunkKeys() || PLAYER_MODIFIED_CONTAINERS.hasDirtyChunkKeys();
		}
		PLAYER_PLACED_BLOCKS.evictChunk(chunkKey);
		PLAYER_MODIFIED_CONTAINERS.evictChunk(chunkKey);
		LOADED_CHUNKS.remove(chunkKey);
	}

	private static void saveChunkSystemData(WorldChunkDataKey dataKey, String systemId, CompoundTag chunkData) {
		if (chunkData == null) {
			WorldChunkDataAPIManager.removeChunkSystemData(dataKey, systemId);
		} else {
			WorldChunkDataAPIManager.setChunkSystemData(dataKey, systemId, chunkData);
		}
	}

	private static String levelId(ServerLevel level) {
		return WorldChunkDataAPIManager.dimensionId(level);
	}

	private static int packLocalBlockPos(BlockPos pos) {
		int localX = pos.getX() & 15;
		int localZ = pos.getZ() & 15;
		int localY = pos.getY() & 0xFFFF;
		return (localY << 8) | (localX << 4) | localZ;
	}

	private static final class TrackedPositionData {
		private final Map<ChunkRefKey, IntOpenHashSet> positionsByChunk = new HashMap<>();
		private final Set<ChunkRefKey> dirtyChunkKeys = new LinkedHashSet<>();

		private void clear() {
			positionsByChunk.clear();
			dirtyChunkKeys.clear();
		}

		private boolean record(ServerLevel level, BlockPos pos) {
			String levelId = levelId(level);
			if (levelId.isBlank()) return false;
			ChunkRefKey chunkKey = new ChunkRefKey(levelId, pos.getX() >> 4, pos.getZ() >> 4);
			boolean added = positionsByChunk.computeIfAbsent(chunkKey, ignored -> new IntOpenHashSet()).add(packLocalBlockPos(pos));
			if (added) dirtyChunkKeys.add(chunkKey);
			return added;
		}

		private boolean contains(ServerLevel level, BlockPos pos) {
			ChunkRefKey chunkKey = new ChunkRefKey(levelId(level), pos.getX() >> 4, pos.getZ() >> 4);
			IntOpenHashSet positions = positionsByChunk.get(chunkKey);
			return positions != null && positions.contains(packLocalBlockPos(pos));
		}

		private boolean remove(ServerLevel level, BlockPos pos) {
			ChunkRefKey chunkKey = new ChunkRefKey(levelId(level), pos.getX() >> 4, pos.getZ() >> 4);
			IntOpenHashSet positions = positionsByChunk.get(chunkKey);
			if (positions == null || !positions.remove(packLocalBlockPos(pos))) return false;
			if (positions.isEmpty()) positionsByChunk.remove(chunkKey);
			dirtyChunkKeys.add(chunkKey);
			return true;
		}

		private Set<ChunkRefKey> collectDirtyChunkKeys() { return new LinkedHashSet<>(dirtyChunkKeys); }
		private void clearDirtyChunkKeys() { dirtyChunkKeys.clear(); }
		private boolean isDirty(ChunkRefKey chunkKey) { return dirtyChunkKeys.contains(chunkKey); }
		private void clearDirtyChunkKey(ChunkRefKey chunkKey) { dirtyChunkKeys.remove(chunkKey); }
		private boolean hasDirtyChunkKeys() { return !dirtyChunkKeys.isEmpty(); }
		private void evictChunk(ChunkRefKey chunkKey) { positionsByChunk.remove(chunkKey); }

		private CompoundTag createChunkPersistedData(ChunkRefKey chunkKey) {
			IntOpenHashSet positions = positionsByChunk.get(chunkKey);
			if (positions == null || positions.isEmpty()) return null;
			CompoundTag data = new CompoundTag();
			data.putByteArray(FIELD_PACKED_POSITIONS, encode(positions));
			return data;
		}

		private byte[] encode(IntOpenHashSet positions) {
			int[] sorted = positions.toIntArray();
			Arrays.sort(sorted);
			byte[] packed = new byte[sorted.length * 3];
			int offset = 0;
			for (int position : sorted) {
				int value = position & 0xFFFFFF;
				packed[offset++] = (byte) (value >>> 16);
				packed[offset++] = (byte) (value >>> 8);
				packed[offset++] = (byte) value;
			}
			return packed;
		}

		private IntOpenHashSet decode(CompoundTag source) {
			IntOpenHashSet positions = new IntOpenHashSet();
			byte[] packed = source.getByteArray(FIELD_PACKED_POSITIONS).orElse(new byte[0]);
			for (int index = 0; index + 2 < packed.length; index += 3) {
				positions.add(((packed[index] & 0xFF) << 16)
					| ((packed[index + 1] & 0xFF) << 8)
					| (packed[index + 2] & 0xFF));
			}
			return positions;
		}

		private void applyChunkPersistedData(CompoundTag source, ChunkRefKey chunkKey) {
			if (chunkKey == null || source == null || source.isEmpty()) return;
			IntOpenHashSet positions = decode(source);
			if (!positions.isEmpty()) positionsByChunk.put(chunkKey, positions);
		}
	}

	private static final class ModifiedContainerData {
		private final Map<ChunkRefKey, Map<Integer, Map<Integer, ModifiedStack>>> stacksByChunk = new HashMap<>();
		private final Set<ChunkRefKey> dirtyChunkKeys = new LinkedHashSet<>();

		private void clear() {
			stacksByChunk.clear();
			dirtyChunkKeys.clear();
		}

		private boolean recordChange(ServerLevel level, BlockPos pos, int slot, ItemStack before, ItemStack after) {
			if (slot < 0) return false;
			String levelId = levelId(level);
			if (levelId.isBlank()) return false;
			ChunkRefKey chunkKey = new ChunkRefKey(levelId, pos.getX() >> 4, pos.getZ() >> 4);
			int packedPosition = packLocalBlockPos(pos);
			Map<Integer, Map<Integer, ModifiedStack>> containers = stacksByChunk.computeIfAbsent(chunkKey, ignored -> new HashMap<>());
			Map<Integer, ModifiedStack> stacks = containers.computeIfAbsent(packedPosition, ignored -> new HashMap<>());
			ModifiedStack previous = stacks.get(slot);
			String beforeItemId = itemId(before);
			String afterItemId = itemId(after);
			ModifiedStack replacement = null;

			if (!afterItemId.isBlank()) {
				boolean sameItem = beforeItemId.equals(afterItemId);
				boolean removedFromStack = sameItem && count(before) > count(after);
				if (!removedFromStack || previous != null) {
					replacement = new ModifiedStack(afterItemId, count(after));
				}
			}

			if (sameModifiedStack(previous, replacement)) return false;
			if (replacement == null) stacks.remove(slot);
			else stacks.put(slot, replacement);
			if (stacks.isEmpty()) containers.remove(packedPosition);
			if (containers.isEmpty()) stacksByChunk.remove(chunkKey);
			dirtyChunkKeys.add(chunkKey);
			return true;
		}

		private boolean contains(ServerLevel level, BlockPos pos) {
			ChunkRefKey chunkKey = new ChunkRefKey(levelId(level), pos.getX() >> 4, pos.getZ() >> 4);
			Map<Integer, Map<Integer, ModifiedStack>> containers = stacksByChunk.get(chunkKey);
			Map<Integer, ModifiedStack> stacks = containers == null ? null : containers.get(packLocalBlockPos(pos));
			return stacks != null && !stacks.isEmpty();
		}

		private boolean containsSlot(ServerLevel level, BlockPos pos, int slot) {
			if (slot < 0) return false;
			ChunkRefKey chunkKey = new ChunkRefKey(levelId(level), pos.getX() >> 4, pos.getZ() >> 4);
			Map<Integer, Map<Integer, ModifiedStack>> containers = stacksByChunk.get(chunkKey);
			Map<Integer, ModifiedStack> stacks = containers == null ? null : containers.get(packLocalBlockPos(pos));
			return stacks != null && stacks.containsKey(slot);
		}

		private boolean remove(ServerLevel level, BlockPos pos) {
			ChunkRefKey chunkKey = new ChunkRefKey(levelId(level), pos.getX() >> 4, pos.getZ() >> 4);
			Map<Integer, Map<Integer, ModifiedStack>> containers = stacksByChunk.get(chunkKey);
			if (containers == null || containers.remove(packLocalBlockPos(pos)) == null) return false;
			if (containers.isEmpty()) stacksByChunk.remove(chunkKey);
			dirtyChunkKeys.add(chunkKey);
			return true;
		}

		private int consume(ServerLevel level, BlockPos pos, ItemStack stack, int maximum) {
			ChunkRefKey chunkKey = new ChunkRefKey(levelId(level), pos.getX() >> 4, pos.getZ() >> 4);
			Map<Integer, Map<Integer, ModifiedStack>> containers = stacksByChunk.get(chunkKey);
			Map<Integer, ModifiedStack> stacks = containers == null ? null : containers.get(packLocalBlockPos(pos));
			if (stacks == null) return 0;
			String itemId = itemId(stack);
			int remaining = maximum;
			int consumed = 0;
			var iterator = stacks.entrySet().iterator();
			while (iterator.hasNext() && remaining > 0) {
				Map.Entry<Integer, ModifiedStack> entry = iterator.next();
				if (!entry.getValue().itemId().equals(itemId)) continue;
				ModifiedStack modifiedStack = entry.getValue();
				int amount = Math.min(remaining, modifiedStack.count());
				remaining -= amount;
				consumed += amount;
				int left = modifiedStack.count() - amount;
				if (left <= 0) iterator.remove();
				else entry.setValue(new ModifiedStack(modifiedStack.itemId(), left));
			}
			if (consumed > 0) {
				if (stacks.isEmpty()) {
					containers.remove(packLocalBlockPos(pos));
					if (containers.isEmpty()) stacksByChunk.remove(chunkKey);
				}
				dirtyChunkKeys.add(chunkKey);
			}
			return consumed;
		}

		private Set<ChunkRefKey> collectDirtyChunkKeys() { return new LinkedHashSet<>(dirtyChunkKeys); }
		private void clearDirtyChunkKeys() { dirtyChunkKeys.clear(); }
		private boolean isDirty(ChunkRefKey chunkKey) { return dirtyChunkKeys.contains(chunkKey); }
		private void clearDirtyChunkKey(ChunkRefKey chunkKey) { dirtyChunkKeys.remove(chunkKey); }
		private boolean hasDirtyChunkKeys() { return !dirtyChunkKeys.isEmpty(); }
		private void evictChunk(ChunkRefKey chunkKey) { stacksByChunk.remove(chunkKey); }

		private CompoundTag createChunkPersistedData(ChunkRefKey chunkKey) {
			Map<Integer, Map<Integer, ModifiedStack>> containers = stacksByChunk.get(chunkKey);
			if (containers == null || containers.isEmpty()) return null;
			CompoundTag data = new CompoundTag();
			CompoundTag modifiedSlots = new CompoundTag();
			for (Map.Entry<Integer, Map<Integer, ModifiedStack>> container : containers.entrySet()) {
				for (Map.Entry<Integer, ModifiedStack> entry : container.getValue().entrySet()) {
					long packedSlot = ((long) container.getKey() << 8) | (entry.getKey() & 0xFFL);
					CompoundTag modifiedStack = new CompoundTag();
					modifiedStack.putString(FIELD_ITEM_ID, entry.getValue().itemId());
					modifiedStack.putInt(FIELD_COUNT, entry.getValue().count());
					modifiedSlots.put(Long.toString(packedSlot), modifiedStack);
				}
			}
			data.put(FIELD_MODIFIED_SLOTS, modifiedSlots);
			return data;
		}

		private void applyChunkPersistedData(CompoundTag source, ChunkRefKey chunkKey) {
			if (chunkKey == null || source == null || source.isEmpty()) return;
			CompoundTag modifiedSlots = source.getCompoundOrEmpty(FIELD_MODIFIED_SLOTS);
			if (modifiedSlots.isEmpty()) return;
			Map<Integer, Map<Integer, ModifiedStack>> containers = new HashMap<>();
			for (String key : modifiedSlots.keySet()) {
				try {
					long packedSlot = Long.parseLong(key);
					int packedPosition = (int) (packedSlot >>> 8);
					int slot = (int) (packedSlot & 0xFFL);
					CompoundTag modifiedStack = modifiedSlots.getCompoundOrEmpty(key);
					String itemId = modifiedStack.getStringOr(FIELD_ITEM_ID, "");
					int count = modifiedStack.getIntOr(FIELD_COUNT, 0);
					if (!itemId.isBlank() && count > 0) {
						containers.computeIfAbsent(packedPosition, ignored -> new HashMap<>())
							.put(slot, new ModifiedStack(itemId, count));
					}
				} catch (NumberFormatException ignored) {
					// Ignore malformed provenance entries.
				}
			}
			if (!containers.isEmpty()) {
				stacksByChunk.put(chunkKey, containers);
			}
		}

		private static String itemId(ItemStack stack) {
			return stack == null || stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		}

		private static int count(ItemStack stack) { return stack == null || stack.isEmpty() ? 0 : stack.getCount(); }

		private static boolean sameModifiedStack(ModifiedStack left, ModifiedStack right) {
			return left == right || left != null && left.equals(right);
		}
	}

	private record ModifiedStack(String itemId, int count) { }

	private record ChunkRefKey(String levelId, int chunkX, int chunkZ) { }
	private record PendingRemoval(ServerLevel level, BlockPos pos) { }
}
