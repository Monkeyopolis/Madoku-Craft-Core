package madoku.craft.java.core.chunk;

public interface ActiveChunkTickProvider {
	default void initialize() { }
	default void reset() { }
	default void registerListener(ActiveChunkTickListener listener) { }
	default void unregisterListener(ActiveChunkTickListener listener) { }
	default void dispatch(ActiveChunkTickEvent event) { }
}
