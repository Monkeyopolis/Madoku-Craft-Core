package madoku.craft.java.core.menu;

import net.minecraft.resources.Identifier;

import java.util.List;

/** Shared contract used by Core to render and operate the Madoku Levels menu. */
public interface LevelsMenuProvider {
	default int version() { return 0; }
	default Snapshot snapshot() { return Snapshot.empty(); }
	default void requestUpgrade(String statId) { }

	record Snapshot(
		String username,
		int level,
		int currentXp,
		int requiredXp,
		int availablePoints,
		List<Stat> stats
	) {
		public Snapshot {
			username = username == null ? "" : username;
			level = Math.max(1, level);
			currentXp = Math.max(0, currentXp);
			requiredXp = Math.max(1, requiredXp);
			availablePoints = Math.max(0, availablePoints);
			stats = stats == null ? List.of() : List.copyOf(stats);
		}

		public boolean hasData() {
			return !username.isBlank();
		}

		private static Snapshot empty() {
			return new Snapshot("", 1, 0, 1, 0, List.of());
		}
	}

	record Stat(
		String id,
		String label,
		Identifier rowTexture,
		int level,
		int maxLevel
	) {
		public Stat {
			if (id == null || id.isBlank()) throw new IllegalArgumentException("Level stat id must not be blank.");
			if (label == null || label.isBlank()) throw new IllegalArgumentException("Level stat label must not be blank.");
			if (rowTexture == null) throw new IllegalArgumentException("Level stat row texture must not be null.");
			level = Math.max(0, level);
			maxLevel = Math.max(1, maxLevel);
		}
	}
}
