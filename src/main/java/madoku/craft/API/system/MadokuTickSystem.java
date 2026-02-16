package madoku.craft.API.system;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import madoku.craft.API.MadokuCraftAPI;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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
	private static final String JSON_FOLDER = MadokuNamingSystem.scopedName("systems");
	private static final String JSON_FILE = MadokuNamingSystem.scopedName("TICK");
	private static final String DATA_NAME = MadokuNamingSystem.scopedName("TICK");
	private static final String SERVER_ENTRY = "server";

	private static final List<TickHandler> START_HANDLERS = new CopyOnWriteArrayList<>();
	private static final List<TickHandler> END_HANDLERS = new CopyOnWriteArrayList<>();
	private static final List<PlayerTickHandler> START_PLAYER_HANDLERS = new CopyOnWriteArrayList<>();
	private static final List<PlayerTickHandler> END_PLAYER_HANDLERS = new CopyOnWriteArrayList<>();
	private static final List<IntervalTask> START_INTERVALS = new CopyOnWriteArrayList<>();
	private static final List<IntervalTask> END_INTERVALS = new CopyOnWriteArrayList<>();
	private static MadokuJSONSystem.ManagedJSON systemJson;
	private static MadokuDataSystem.MadokuData systemData;
	private static volatile boolean initialized;
	private static long tickCount;

	private MadokuTickSystem() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		systemJson = MadokuJSONSystem.load(JSON_FOLDER, JSON_FILE, buildJsonDefaults());
		systemData = MadokuDataSystem.load(DATA_NAME, MadokuDataSystem.StorageScope.WORLD, buildDataDefaults());

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			MadokuDataSystem.bindWorld(systemData, DATA_NAME, buildDataDefaults(), server);
			if (!systemData.isDeferred()) {
				long starts = readLong(systemData.getRoot(), "serverStartCount", 0L) + 1L;
				systemData.getRoot().addProperty("serverStartCount", starts);
			}
			LOGGER.info("{} data ready at {}", MadokuDataSystem.SYSTEM_NAME, systemData.getPath());
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> saveSystemData(server));

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
		updateSystemData(server);
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

	private static JsonObject buildJsonDefaults() {
		JsonObject defaults = new JsonObject();
		defaults.addProperty("enabled", true);
		defaults.addProperty("saveIntervalTicks", 3600);
		defaults.addProperty("trackOnlinePlayers", true);
		return defaults;
	}

	private static JsonObject buildDataDefaults() {
		JsonObject defaults = new JsonObject();
		defaults.addProperty("tickCount", 0L);
		defaults.addProperty("serverStartCount", 0L);
		return defaults;
	}

	private static void updateSystemData(MinecraftServer server) {
		if (server == null || systemData == null || systemData.isDeferred()) {
			return;
		}

		JsonObject config = systemJson == null ? new JsonObject() : systemJson.getRoot();
		if (!readBoolean(config, "enabled", true)) {
			return;
		}

		systemData.getRoot().addProperty("tickCount", tickCount);

		JsonObject serverEntry = systemData.entry(MadokuDataSystem.Tracker.ENTITIES, SERVER_ENTRY);
		serverEntry.addProperty("tickCount", tickCount);
		if (readBoolean(config, "trackOnlinePlayers", true)) {
			serverEntry.addProperty("onlinePlayers", server.getPlayerManager().getCurrentPlayerCount());
		}

		int saveInterval = readInt(config, "saveIntervalTicks", 3600);
		if (saveInterval > 0 && tickCount % saveInterval == 0) {
			systemData.save();
		}
	}

	private static void saveSystemData(MinecraftServer server) {
		if (server == null || systemData == null || systemData.isDeferred()) {
			return;
		}
		updateSystemData(server);
		systemData.save();
	}

	private static int readInt(JsonObject root, String key, int fallback) {
		JsonElement element = root.get(key);
		if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
			return element.getAsInt();
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

	private static boolean readBoolean(JsonObject root, String key, boolean fallback) {
		JsonElement element = root.get(key);
		if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
			return element.getAsBoolean();
		}
		return fallback;
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
