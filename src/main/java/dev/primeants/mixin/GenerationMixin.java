package dev.primeants.mixin;
import dev.primeants.founding.GenerationWitness;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ChunkStatusTasks.class)
public abstract class GenerationMixin {
    @Inject(method = "buildTerrain", at = @At("RETURN"), cancellable = true)
    private static void primeAntsWitness(WorldGenContext context, ChunkStep step, StaticCache2D<GenerationChunkHolder> chunks,
            ChunkAccess chunk, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
        cir.setReturnValue(cir.getReturnValue().thenApply(generated -> {
            if (generated instanceof ProtoChunk proto && !proto.isUpgrading() && proto.getBelowZeroRetrogen() == null)
                ((GenerationWitness)proto).primeAntsGeneratedTerrain(true);
            return generated;
        }));
    }
}
