package dev.primeants.gametest.mixin;
import dev.primeants.gametest.PlacementSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Count FULL construction, including chunks that never become accessible before failure/shutdown. */
@Mixin(LevelChunk.class)
public abstract class PlacementFullBudgetMixin {
    @Inject(method="<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V",at=@At("RETURN"))
    private void count(ServerLevel level,ProtoChunk chunk,LevelChunk.PostLoadProcessor post,CallbackInfo ci) {
        PlacementSettings.recordFull(level,chunk.getPos());
    }
}
