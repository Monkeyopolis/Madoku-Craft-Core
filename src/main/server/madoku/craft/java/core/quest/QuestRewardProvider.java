package madoku.craft.java.core.quest;

import net.minecraft.server.level.ServerPlayer;

/** Optional bridge for progression systems that receive rewards from quests. */
public interface QuestRewardProvider {
	void addMadokuLevelsXp(ServerPlayer player, int amount);
}
