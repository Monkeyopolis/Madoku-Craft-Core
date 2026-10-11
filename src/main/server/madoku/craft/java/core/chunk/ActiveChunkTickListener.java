package madoku.craft.java.core.chunk;

@FunctionalInterface
public interface ActiveChunkTickListener {
	void onActiveChunkTick(ActiveChunkTickEvent event);
}
