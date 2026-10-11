package madoku.craft.java.core.chunk;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/** One heartbeat for a chunk that vanilla is actively ticking. */
public record ActiveChunkTickEvent(ServerLevel level, LevelChunk chunk) {
}
