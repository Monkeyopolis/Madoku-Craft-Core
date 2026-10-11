package madoku.craft.mixin.core;

import madoku.craft.java.core.chunk.ActiveChunkTickAPIManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives core consumers one callback for each active vanilla chunk tick. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelActiveChunkTickMixin {
	@Inject(method = "tickChunk", at = @At("TAIL"))
	private void madokuCraft$dispatchActiveChunkTick(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
		ActiveChunkTickAPIManager.dispatch((ServerLevel) (Object) this, chunk);
	}
}
