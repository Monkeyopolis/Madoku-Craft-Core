package madoku.craft.java.core.helper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Public contract for the active block-drop context. */
public final class BlockDropContextAPIManager {
	private static final BlockDropContextProvider UNAVAILABLE_PROVIDER = new BlockDropContextProvider() { };
	private static volatile BlockDropContextProvider provider = UNAVAILABLE_PROVIDER;

	private BlockDropContextAPIManager() { }

	public static void registerProvider(BlockDropContextProvider candidate) {
		if (candidate == null) throw new IllegalArgumentException("Block-drop context provider must not be null.");
		provider = candidate;
	}
	public static void unregisterProvider() { provider = UNAVAILABLE_PROVIDER; }
	public static void begin(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) { provider.begin(level, player, pos, state); }
	public static void begin(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		provider.begin(level, player, pos, state, blockEntity);
	}
	public static void end() { provider.end(); }
	public static Context current() { return provider.current(); }
	public static ServerPlayer resolvePlayer() { return provider.resolvePlayer(); }
	public static boolean isActiveDropPlayerPlacedBlock() { return provider.isActiveDropPlayerPlacedBlock(); }
	public static boolean isActiveDropContainer() { return provider.isActiveDropContainer(); }
	public static boolean isActiveDropPlayerModifiedContainer() { return provider.isActiveDropPlayerModifiedContainer(); }
	public static int consumeActiveDropPlayerModifiedContainerAmount(ItemStack stack) { return provider.consumeActiveDropPlayerModifiedContainerAmount(stack); }

	public record Context(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) { }
}
