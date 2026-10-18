package dev.primeants.gametest.mixin;
import net.minecraft.gametest.framework.GameTestInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
/** Bridge only for the original dispatch operation; never called in addition to it. */
@Mixin(GameTestInfo.class)
public interface RecordedTickInvoker {
    @Invoker("tickInternal") void primeAnts$originalTickInternal();
}
