package dev.primeants.gametest;
import dev.primeants.founding.NaturalSoil;
import net.minecraft.nbt.*;
import java.nio.file.Path;
public final class SoilDecodeProbe {
    public static void main(String[] args)throws Exception {
        var tag=NbtIo.readCompressed(Path.of(args[0]),NbtAccounter.unlimitedHeap()).get("data");long start=System.nanoTime();
        var restored=NaturalSoil.CODEC.parse(NbtOps.INSTANCE,tag).getOrThrow();double ms=(System.nanoTime()-start)/1e6;
        var encoded=NaturalSoil.CODEC.encodeStart(NbtOps.INSTANCE,restored).getOrThrow();
        if(!tag.equals(encoded))throw new AssertionError("Actual saved soil records changed");
        System.out.println("SOIL_DECODE entries="+((CompoundTag)tag).size()+" elapsed_ms="+ms+" exact_record_equality=true");
    }
}
