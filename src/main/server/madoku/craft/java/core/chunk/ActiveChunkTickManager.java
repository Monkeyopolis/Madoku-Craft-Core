package madoku.craft.java.core.chunk;

import java.util.concurrent.CopyOnWriteArrayList;

final class ActiveChunkTickManager {
	private static final CopyOnWriteArrayList<ActiveChunkTickListener> LISTENERS = new CopyOnWriteArrayList<>();

	private ActiveChunkTickManager() { }

	static void initialize() { }

	static void reset() {
		// Listener registrations belong to module initialization, not to one
		// server instance. Standalone modules can receive SERVER_STARTED in a
		// different order, so clearing this list here can silently remove a
		// listener that a feature registered after its own reset.
	}

	static void registerListener(ActiveChunkTickListener listener) {
		if (listener != null && !LISTENERS.contains(listener)) {
			LISTENERS.add(listener);
		}
	}

	static void unregisterListener(ActiveChunkTickListener listener) {
		if (listener != null) {
			LISTENERS.remove(listener);
		}
	}

	static void dispatch(ActiveChunkTickEvent event) {
		if (event == null) {
			return;
		}
		for (ActiveChunkTickListener listener : LISTENERS) {
			listener.onActiveChunkTick(event);
		}
	}
}
