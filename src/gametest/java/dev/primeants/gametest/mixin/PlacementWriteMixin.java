package dev.primeants.gametest.mixin;
import dev.primeants.gametest.PlacementFault;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.nbt.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Throws at the actual NBT write call, inside Minecraft's tryWrite IOException catch. */
@Mixin(NbtIo.class)
public abstract class PlacementWriteMixin {
    @Inject(method="writeCompressed(Lnet/minecraft/nbt/CompoundTag;Ljava/nio/file/Path;)V",at=@At("HEAD"))
    private static void fail(CompoundTag tag,Path file,CallbackInfo ci) throws IOException {PlacementFault.write(tag,file);}
}
