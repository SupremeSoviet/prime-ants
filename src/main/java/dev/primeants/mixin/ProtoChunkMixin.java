package dev.primeants.mixin;
import dev.primeants.founding.GenerationWitness;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
@Mixin(ProtoChunk.class)
public abstract class ProtoChunkMixin implements GenerationWitness {
    @Unique private boolean primeAntsGenerated;
    public boolean primeAntsGeneratedTerrain() { return primeAntsGenerated; }
    public void primeAntsGeneratedTerrain(boolean value) { primeAntsGenerated = value; }
}
