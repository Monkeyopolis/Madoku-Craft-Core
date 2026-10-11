package madoku.craft.java.core.chunk;

/** Built-in active-chunk provider backed by the core listener registry. */
public final class MadokuActiveChunkTickProvider implements ActiveChunkTickProvider {
	@Override public void initialize() { ActiveChunkTickManager.initialize(); }
	@Override public void reset() { ActiveChunkTickManager.reset(); }
	@Override public void registerListener(ActiveChunkTickListener listener) {
		ActiveChunkTickManager.registerListener(listener);
	}
	@Override public void unregisterListener(ActiveChunkTickListener listener) {
		ActiveChunkTickManager.unregisterListener(listener);
	}
	@Override public void dispatch(ActiveChunkTickEvent event) { ActiveChunkTickManager.dispatch(event); }
}
