package madoku.craft.API.system;

import madoku.craft.API.MadokuCraftAPI;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Centralized server tick hub for Madoku Craft mods. */
public final class MadokuTickSystem {
	public enum Phase {
		START,
		END
	}

	@FunctionalInterface
	public interface TickHandler {
		void onTick(MinecraftServer server);
	}

	@FunctionalInterface
	public interface PlayerTickHandler {
		void onPlayerTick(MinecraftServer server, ServerPlayerEntity player);
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(MadokuCraftAPI.MOD_ID);
	private static final List<TickHandler> START_HANDLERS = new CopyOnWriteArrayList<>();
	private static final List<TickHandler> END_HANDLERS = new CopyOnWriteArrayList<>();
	private static final List<PlayerTickHandler> START_PLAYER_HANDLERS = new CopyOnWriteArrayList<>();
	private static final List<PlayerTickHandler> END_PLAYER_HANDLERS = new CopyOnWriteArrayList<>();
	private static final List<IntervalTask> START_INTERVALS = new CopyOnWriteArrayList<>();
	private static final List<IntervalTask> END_INTERVALS = new CopyOnWriteArrayList<>();
	private static volatile boolean initialized;
	private static long tickCount;

	private MadokuTickSystem() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		ServerTickEvents.START_SERVER_TICK.register(MadokuTickSystem::onStartTick);
		ServerTickEvents.END_SERVER_TICK.register(MadokuTickSystem::onEndTick);
	}

	public static long getTickCount() {
		return tickCount;
	}

	public static void register(Phase phase, TickHandler handler) {
		if (handler == null) {
			return;
		}
		getHandlers(phase).add(handler);
	}

	public static void registerPlayer(Phase phase, PlayerTickHandler handler) {
		if (handler == null) {
			return;
		}
		getPlayerHandlers(phase).add(handler);
	}

	public static void registerInterval(Phase phase, int intervalTicks, TickHandler handler) {
		if (handler == null || intervalTicks <= 0) {
			return;
		}
		getIntervals(phase).add(new IntervalTask(intervalTicks, handler));
	}

	private static void onStartTick(MinecraftServer server) {
		tickCount++;
		runHandlers(START_HANDLERS, server, Phase.START);
		runPlayerHandlers(START_PLAYER_HANDLERS, server, Phase.START);
		runIntervals(START_INTERVALS, server, Phase.START);
	}

	private static void onEndTick(MinecraftServer server) {
		runHandlers(END_HANDLERS, server, Phase.END);
		runPlayerHandlers(END_PLAYER_HANDLERS, server, Phase.END);
		runIntervals(END_INTERVALS, server, Phase.END);
	}

	private static List<TickHandler> getHandlers(Phase phase) {
		return phase == Phase.START ? START_HANDLERS : END_HANDLERS;
	}

	private static List<PlayerTickHandler> getPlayerHandlers(Phase phase) {
		return phase == Phase.START ? START_PLAYER_HANDLERS : END_PLAYER_HANDLERS;
	}

	private static List<IntervalTask> getIntervals(Phase phase) {
		return phase == Phase.START ? START_INTERVALS : END_INTERVALS;
	}

	private static void runHandlers(List<TickHandler> handlers, MinecraftServer server, Phase phase) {
		if (server == null || handlers.isEmpty()) {
			return;
		}
		for (TickHandler handler : handlers) {
			try {
				handler.onTick(server);
			} catch (RuntimeException exc) {
				LOGGER.warn("Tick handler failed during {} phase: {}", phase, handler.getClass().getName(), exc);
			}
		}
	}

	private static void runPlayerHandlers(List<PlayerTickHandler> handlers, MinecraftServer server, Phase phase) {
		if (server == null || handlers.isEmpty()) {
			return;
		}
		List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
		if (players.isEmpty()) {
			return;
		}
		for (ServerPlayerEntity player : players) {
			for (PlayerTickHandler handler : handlers) {
				try {
					handler.onPlayerTick(server, player);
				} catch (RuntimeException exc) {
					LOGGER.warn("Player tick handler failed during {} phase: {}", phase, handler.getClass().getName(), exc);
				}
			}
		}
	}

	private static void runIntervals(List<IntervalTask> tasks, MinecraftServer server, Phase phase) {
		if (server == null || tasks.isEmpty()) {
			return;
		}
		for (IntervalTask task : tasks) {
			if (task.shouldRun(tickCount)) {
				try {
					task.handler.onTick(server);
				} catch (RuntimeException exc) {
					LOGGER.warn("Interval tick handler failed during {} phase: {}", phase, task.handler.getClass().getName(), exc);
				}
			}
		}
	}

	private static final class IntervalTask {
		private final int interval;
		private final TickHandler handler;
		private long nextTick;

		private IntervalTask(int interval, TickHandler handler) {
			this.interval = interval;
			this.handler = handler;
			this.nextTick = tickCount + interval;
		}

		private boolean shouldRun(long currentTick) {
			if (currentTick < nextTick) {
				return false;
			}
			nextTick = currentTick + interval;
			return true;
		}
	}
}
