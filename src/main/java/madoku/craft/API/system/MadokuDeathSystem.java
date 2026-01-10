package madoku.craft.API.system;

import madoku.craft.API.MadokuCraftAPI;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

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
	private static final List<PlayerDeathHandler> DEATH_HANDLERS = new CopyOnWriteArrayList<>();
	private static final List<PlayerRespawnHandler> RESPAWN_HANDLERS = new CopyOnWriteArrayList<>();
	private static final Map<UUID, PlayerDeathContext> LAST_DEATH = new ConcurrentHashMap<>();
	private static volatile boolean initialized;

	private MadokuDeathSystem() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(entity instanceof ServerPlayerEntity player)) {
				return;
			}
			PlayerDeathContext context = PlayerDeathContext.capture(player, source);
			LAST_DEATH.put(player.getUuid(), context);
			runDeathHandlers(player, context);
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			PlayerDeathContext context = LAST_DEATH.remove(newPlayer.getUuid());
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
