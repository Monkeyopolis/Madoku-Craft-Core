package madoku.craft.API.system;

import madoku.craft.API.MadokuCraftAPI;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Centralized client tick hub for Madoku Craft mods. */
public final class MadokuClientTickSystem {
	public enum Phase {
		START,
		END
	}

	@FunctionalInterface
	public interface TickHandler {
		void onTick(MinecraftClient client);
	}

	@FunctionalInterface
	public interface PlayerTickHandler {
		void onPlayerTick(MinecraftClient client, ClientPlayerEntity player);
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

	private MadokuClientTickSystem() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		ClientTickEvents.START_CLIENT_TICK.register(MadokuClientTickSystem::onStartTick);
		ClientTickEvents.END_CLIENT_TICK.register(MadokuClientTickSystem::onEndTick);
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

	private static void onStartTick(MinecraftClient client) {
		tickCount++;
		runHandlers(START_HANDLERS, client, Phase.START);
		runPlayerHandlers(START_PLAYER_HANDLERS, client, Phase.START);
		runIntervals(START_INTERVALS, client, Phase.START);
	}

	private static void onEndTick(MinecraftClient client) {
		runHandlers(END_HANDLERS, client, Phase.END);
		runPlayerHandlers(END_PLAYER_HANDLERS, client, Phase.END);
		runIntervals(END_INTERVALS, client, Phase.END);
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

	private static void runHandlers(List<TickHandler> handlers, MinecraftClient client, Phase phase) {
		if (client == null || handlers.isEmpty()) {
			return;
		}
		for (TickHandler handler : handlers) {
			try {
				handler.onTick(client);
			} catch (RuntimeException exc) {
				LOGGER.warn("Client tick handler failed during {} phase: {}", phase, handler.getClass().getName(), exc);
			}
		}
	}

	private static void runPlayerHandlers(List<PlayerTickHandler> handlers, MinecraftClient client, Phase phase) {
		if (client == null || handlers.isEmpty()) {
			return;
		}
		ClientPlayerEntity player = client.player;
		if (player == null) {
			return;
		}
		for (PlayerTickHandler handler : handlers) {
			try {
				handler.onPlayerTick(client, player);
			} catch (RuntimeException exc) {
				LOGGER.warn("Client player tick handler failed during {} phase: {}", phase, handler.getClass().getName(), exc);
			}
		}
	}

	private static void runIntervals(List<IntervalTask> tasks, MinecraftClient client, Phase phase) {
		if (client == null || tasks.isEmpty()) {
			return;
		}
		for (IntervalTask task : tasks) {
			if (task.shouldRun(tickCount)) {
				try {
					task.handler.onTick(client);
				} catch (RuntimeException exc) {
					LOGGER.warn("Client interval tick handler failed during {} phase: {}", phase, task.handler.getClass().getName(), exc);
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
