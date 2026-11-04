package madoku.craft.java.core.helper;

import madoku.craft.java.core.data.ChunkDataAPIManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Provides the shared context for a player destroying a block and generating its drops. */
public final class MadokuBlockDropContextManager {
	private static final ThreadLocal<Context> ACTIVE_CONTEXT = new ThreadLocal<>();

	private MadokuBlockDropContextManager() { }

	public static void begin(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
		begin(level, player, pos, state, null);
	}

	public static void begin(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (level == null || player == null || pos == null || state == null) {
			ACTIVE_CONTEXT.remove();
			return;
		}
		ACTIVE_CONTEXT.set(new Context(level, player, pos.immutable(), state, blockEntity));
	}

	public static void end() {
		ACTIVE_CONTEXT.remove();
	}

	public static Context current() {
		return ACTIVE_CONTEXT.get();
	}

	public static ServerPlayer resolvePlayer() {
		Context context = current();
		return context == null ? null : context.player();
	}

	public static boolean isActiveDropPlayerPlacedBlock() {
		Context context = current();
		return context != null
			&& ChunkDataAPIManager.isPlayerPlacedBlock(context.level(), context.pos());
	}

	public static boolean isActiveDropContainer() {
		Context context = current();
		return context != null && context.blockEntity() instanceof Container;
	}

	public static boolean isActiveDropPlayerModifiedContainer() {
		Context context = current();
		return context != null
			&& ChunkDataAPIManager.isPlayerModifiedContainer(context.level(), context.pos());
	}

	public static int consumeActiveDropPlayerModifiedContainerAmount(ItemStack stack) {
		Context context = current();
		return context == null ? 0 : ChunkDataAPIManager.consumePlayerModifiedContainerItem(
			context.level(), context.pos(), stack, stack == null ? 0 : stack.getCount());
	}

	public record Context(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, BlockEntity blockEntity) { }
}
