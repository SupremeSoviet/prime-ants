package dev.primeants.gametest.mixin;
import dev.primeants.founding.ChamberExcavation;
import dev.primeants.gametest.ScaleValidationProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ChamberExcavation.class)
public abstract class ScaleRoomProbeMixin {
    @Inject(method="build",at=@At("HEAD")) private static void build(CallbackInfoReturnable<?> c){ScaleValidationProbe.count(6);}
}
