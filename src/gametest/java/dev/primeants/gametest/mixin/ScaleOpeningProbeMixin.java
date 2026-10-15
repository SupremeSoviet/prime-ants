package dev.primeants.gametest.mixin;
import dev.primeants.founding.DigJob;
import dev.primeants.gametest.ScaleValidationProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(DigJob.class)
public abstract class ScaleOpeningProbeMixin {
    @Inject(method="anyOpening",at=@At("HEAD")) private static void opening(CallbackInfoReturnable<?> c){ScaleValidationProbe.count(7);}
}
