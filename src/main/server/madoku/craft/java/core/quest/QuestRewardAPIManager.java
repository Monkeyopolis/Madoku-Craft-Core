package madoku.craft.java.core.quest;

import net.minecraft.server.level.ServerPlayer;

/** Access point for optional progression rewards granted by Madoku Quests. */
public final class QuestRewardAPIManager {
	private static final QuestRewardProvider UNAVAILABLE_PROVIDER = (player, amount) -> { };
	private static volatile QuestRewardProvider provider = UNAVAILABLE_PROVIDER;

	private QuestRewardAPIManager() { }

	public static void registerProvider(QuestRewardProvider candidate) {
		if (candidate == null) throw new IllegalArgumentException("Quest reward provider must not be null.");
		provider = candidate;
	}

	public static void unregisterProvider() {
		provider = UNAVAILABLE_PROVIDER;
	}

	public static void addMadokuLevelsXp(ServerPlayer player, int amount) {
		provider.addMadokuLevelsXp(player, amount);
	}
}
