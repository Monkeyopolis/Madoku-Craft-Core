package madoku.craft.java.core.chunk;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/** Public active-chunk heartbeat contract for feature modules. */
public final class ActiveChunkTickAPIManager {
	private static final ActiveChunkTickProvider UNAVAILABLE_PROVIDER = new ActiveChunkTickProvider() { };
	private static volatile ActiveChunkTickProvider provider = UNAVAILABLE_PROVIDER;

	private ActiveChunkTickAPIManager() { }

	public static void registerProvider(ActiveChunkTickProvider candidate) {
		if (candidate == null) {
			throw new IllegalArgumentException("Active chunk-tick provider must not be null.");
		}
		provider = candidate;
	}

	public static void unregisterProvider() {
		provider = UNAVAILABLE_PROVIDER;
	}

	public static void initialize() { provider.initialize(); }

	public static void reset() { provider.reset(); }

	public static void registerListener(ActiveChunkTickListener listener) {
		provider.registerListener(listener);
	}

	public static void unregisterListener(ActiveChunkTickListener listener) {
		provider.unregisterListener(listener);
	}

	/** Dispatches one active chunk heartbeat. Vanilla random-tick speed is independent of this signal. */
	public static void dispatch(ServerLevel level, LevelChunk chunk) {
		if (level != null && chunk != null) {
			provider.dispatch(new ActiveChunkTickEvent(level, chunk));
		}
	}
}
