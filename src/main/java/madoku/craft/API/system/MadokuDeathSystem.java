package madoku.craft.API.system;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import madoku.craft.API.MadokuCraftAPI;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Centralized player death and respawn hooks for Madoku Craft mods. */
public final class MadokuDeathSystem {
	@FunctionalInterface
	public interface PlayerDeathHandler {
		void onPlayerDeath(ServerPlayerEntity player, PlayerDeathContext context);
	}

	@FunctionalInterface
	public interface PlayerRespawnHandler {
		void onPlayerRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive, PlayerDeathContext lastDeath);
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(MadokuCraftAPI.MOD_ID);
	private static final String JSON_FOLDER_ID = "API";
	private static final String SYSTEM_ID = "death";
	private static final String LOG_SOURCE = "DEATH";
	private static final String TOTALS_ENTRY = "totals";

	private static final List<PlayerDeathHandler> DEATH_HANDLERS = new CopyOnWriteArrayList<>();
	private static final List<PlayerRespawnHandler> RESPAWN_HANDLERS = new CopyOnWriteArrayList<>();
	private static final Map<UUID, PlayerDeathContext> LAST_DEATH = new ConcurrentHashMap<>();
	private static MadokuJSONSystem.ManagedJSON systemJson;
	private static MadokuDataSystem.MadokuData systemData;
	private static volatile boolean initialized;

	private MadokuDeathSystem() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		systemJson = MadokuJSONSystem.load(JSON_FOLDER_ID, SYSTEM_ID, buildJsonDefaults());
		systemData = MadokuDataSystem.load(SYSTEM_ID, MadokuDataSystem.StorageScope.WORLD, buildDataDefaults());

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			MadokuDataSystem.bindWorld(systemData, SYSTEM_ID, buildDataDefaults(), server);
			MadokuInfoDebugSystem.info(LOG_SOURCE, "{} data ready at {}", MadokuDataSystem.SYSTEM_NAME, systemData.getPath());
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			if (systemData != null && !systemData.isDeferred()) {
				systemData.save();
			}
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(entity instanceof ServerPlayerEntity player)) {
				return;
			}
			PlayerDeathContext context = PlayerDeathContext.capture(player, source);
			LAST_DEATH.put(player.getUuid(), context);
			recordDeath(player, context);
			runDeathHandlers(player, context);
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			PlayerDeathContext context = LAST_DEATH.remove(newPlayer.getUuid());
			recordRespawn(newPlayer);
			runRespawnHandlers(oldPlayer, newPlayer, alive, context);
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (handler != null && handler.player != null) {
				LAST_DEATH.remove(handler.player.getUuid());
			}
		});
	}

	public static void registerDeath(PlayerDeathHandler handler) {
		if (handler == null) {
			return;
		}
		DEATH_HANDLERS.add(handler);
	}

	public static void registerRespawn(PlayerRespawnHandler handler) {
		if (handler == null) {
			return;
		}
		RESPAWN_HANDLERS.add(handler);
	}

	public static PlayerDeathContext getLastDeath(ServerPlayerEntity player) {
		if (player == null) {
			return null;
		}
		return LAST_DEATH.get(player.getUuid());
	}

	public static PlayerDeathContext getLastDeath(UUID playerId) {
		if (playerId == null) {
			return null;
		}
		return LAST_DEATH.get(playerId);
	}

	private static void runDeathHandlers(ServerPlayerEntity player, PlayerDeathContext context) {
		if (player == null || DEATH_HANDLERS.isEmpty()) {
			return;
		}
		for (PlayerDeathHandler handler : DEATH_HANDLERS) {
			try {
				handler.onPlayerDeath(player, context);
			} catch (RuntimeException exc) {
				LOGGER.warn("Player death handler failed: {}", handler.getClass().getName(), exc);
			}
		}
	}

	private static void runRespawnHandlers(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive, PlayerDeathContext context) {
		if (newPlayer == null || RESPAWN_HANDLERS.isEmpty()) {
			return;
		}
		for (PlayerRespawnHandler handler : RESPAWN_HANDLERS) {
			try {
				handler.onPlayerRespawn(oldPlayer, newPlayer, alive, context);
			} catch (RuntimeException exc) {
				LOGGER.warn("Player respawn handler failed: {}", handler.getClass().getName(), exc);
			}
		}
	}

	private static JsonObject buildJsonDefaults() {
		JsonObject defaults = new JsonObject();
		defaults.addProperty("enabled", true);
		defaults.addProperty("saveOnDeath", true);
		defaults.addProperty("saveOnRespawn", false);
		defaults.addProperty("trackPosition", true);
		defaults.addProperty("trackWorld", true);
		defaults.addProperty("trackDamageType", true);
		return defaults;
	}

	private static JsonObject buildDataDefaults() {
		JsonObject defaults = new JsonObject();
		defaults.addProperty("totalDeaths", 0L);
		defaults.addProperty("totalRespawns", 0L);
		return defaults;
	}

	private static void recordDeath(ServerPlayerEntity player, PlayerDeathContext context) {
		if (player == null || context == null || systemData == null || systemData.isDeferred()) {
			return;
		}
		JsonObject config = systemJson == null ? new JsonObject() : systemJson.getRoot();
		if (!readBoolean(config, "enabled", true)) {
			return;
		}

		JsonObject totals = systemData.entry(MadokuDataSystem.Tracker.ENTITIES, TOTALS_ENTRY);
		long totalDeaths = readLong(totals, "deaths", 0L) + 1L;
		totals.addProperty("deaths", totalDeaths);
		systemData.getRoot().addProperty("totalDeaths", totalDeaths);

		String playerId = context.getPlayerId().toString();
		JsonObject playerEntry = systemData.entry(MadokuDataSystem.Tracker.PLAYERS, playerId);
		playerEntry.addProperty("name", context.getPlayerName());
		playerEntry.addProperty("deathCount", readLong(playerEntry, "deathCount", 0L) + 1L);
		playerEntry.addProperty("lastDeathTick", context.getTick());

		if (readBoolean(config, "trackDamageType", true)) {
			playerEntry.addProperty("lastDamageSource", describeDamageSource(context.getDamageSource()));
		}
		if (readBoolean(config, "trackWorld", true) && context.getWorldId() != null) {
			playerEntry.addProperty("lastWorld", context.getWorldId().toString());
		}
		if (readBoolean(config, "trackPosition", true) && context.getBlockPos() != null) {
			playerEntry.addProperty("lastX", context.getBlockPos().getX());
			playerEntry.addProperty("lastY", context.getBlockPos().getY());
			playerEntry.addProperty("lastZ", context.getBlockPos().getZ());
		}

		if (readBoolean(config, "saveOnDeath", true)) {
			systemData.save();
		}
	}

	private static void recordRespawn(ServerPlayerEntity player) {
		if (player == null || systemData == null || systemData.isDeferred()) {
			return;
		}
		JsonObject config = systemJson == null ? new JsonObject() : systemJson.getRoot();
		if (!readBoolean(config, "enabled", true)) {
			return;
		}

		JsonObject totals = systemData.entry(MadokuDataSystem.Tracker.ENTITIES, TOTALS_ENTRY);
		long totalRespawns = readLong(totals, "respawns", 0L) + 1L;
		totals.addProperty("respawns", totalRespawns);
		systemData.getRoot().addProperty("totalRespawns", totalRespawns);

		JsonObject playerEntry = systemData.entry(MadokuDataSystem.Tracker.PLAYERS, player.getUuidAsString());
		playerEntry.addProperty("respawnCount", readLong(playerEntry, "respawnCount", 0L) + 1L);

		if (readBoolean(config, "saveOnRespawn", false)) {
			systemData.save();
		}
	}

	private static String describeDamageSource(DamageSource source) {
		if (source == null) {
			return "unknown";
		}
		String name = source.getName();
		return name == null || name.isBlank() ? "unknown" : name;
	}

	private static boolean readBoolean(JsonObject root, String key, boolean fallback) {
		JsonElement element = root.get(key);
		if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
			return element.getAsBoolean();
		}
		return fallback;
	}

	private static long readLong(JsonObject root, String key, long fallback) {
		JsonElement element = root.get(key);
		if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
			return element.getAsLong();
		}
		return fallback;
	}

	/** Snapshot of a player's death, kept until respawn handlers consume it. */
	public static final class PlayerDeathContext {
		private final UUID playerId;
		private final String playerName;
		private final DamageSource damageSource;
		private final BlockPos blockPos;
		private final Identifier worldId;
		private final long tick;

		private PlayerDeathContext(UUID playerId, String playerName, DamageSource damageSource, BlockPos blockPos, Identifier worldId, long tick) {
			this.playerId = playerId;
			this.playerName = playerName;
			this.damageSource = damageSource;
			this.blockPos = blockPos;
			this.worldId = worldId;
			this.tick = tick;
		}

		private static PlayerDeathContext capture(ServerPlayerEntity player, DamageSource source) {
			UUID id = player.getUuid();
			String name = player.getName().getString();
			BlockPos pos = player.getBlockPos();
			Identifier world = player.getEntityWorld().getRegistryKey().getValue();
			long tick = MadokuTickSystem.getTickCount();
			return new PlayerDeathContext(id, name, source, pos, world, tick);
		}

		public UUID getPlayerId() {
			return playerId;
		}

		public String getPlayerName() {
			return playerName;
		}

		public DamageSource getDamageSource() {
			return damageSource;
		}

		public BlockPos getBlockPos() {
			return blockPos;
		}

		public Identifier getWorldId() {
			return worldId;
		}

		public long getTick() {
			return tick;
		}
	}
}
