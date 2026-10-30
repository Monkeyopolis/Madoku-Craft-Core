package madoku.craft.java.core.attribute;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** Supplies additive contributions to shared player stats. */
public interface PlayerAttributeProvider {
	String id();

	default double bonus(Player player, PlayerAttributeStat stat) {
		return player instanceof ServerPlayer serverPlayer ? bonus(serverPlayer, stat) : 0.0D;
	}

	default double bonus(Player player, PlayerAttributeStat stat, BlockState state) {
		return bonus(player, stat);
	}

	default double bonus(ServerPlayer player, PlayerAttributeStat stat) { return 0.0D; }
}
