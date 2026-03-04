package madoku.craft.clock;

public final class MadokuClock {
	public static final long TICKS_PER_SECOND = 20L;
	public static final long SECONDS_PER_MINUTE = 60L;

	private static long ticks = 0L;

	private MadokuClock() {
	}

	public static void tick() {
		tick(1L);
	}

	public static void tick(long amount) {
		if (amount <= 0L) {
			return;
		}
		ticks += amount;
	}

	public static void reset() {
		ticks = 0L;
	}

	public static long getTicks() {
		return ticks;
	}

	public static void setTicks(long value) {
		ticks = value;
	}
}
