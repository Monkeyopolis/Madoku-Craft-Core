package madoku.craft.java.core.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/** Public helpers for resolving and comparing registered block types. */
public final class BlockTypeAPIManager {
	private BlockTypeAPIManager() { }

	public static Identifier getId(Block block) {
		return block == null ? null : BuiltInRegistries.BLOCK.getKey(block);
	}

	public static String getPath(Block block) {
		Identifier id = getId(block);
		return id == null ? "" : id.getPath();
	}

	public static boolean isType(Block block, String identifier) {
		Identifier id = getId(block);
		return id != null && identifier != null && identifier.equals(id.toString());
	}
}
