package dev.primeants.gametest.mixin;
import dev.primeants.founding.NestPlan;
import dev.primeants.gametest.ScaleValidationProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(NestPlan.class)
public abstract class ScalePlanProbeMixin {
    @Inject(method="nurseryFindings",at=@At("HEAD")) private void findings(CallbackInfoReturnable<?> c){ScaleValidationProbe.count(0);}
    @Inject(method="undergroundSurfaces",at=@At("HEAD")) private void surfaces(CallbackInfoReturnable<?> c){ScaleValidationProbe.count(1);}
    @Inject(method="geometry",at=@At("HEAD")) private static void geometry(CallbackInfoReturnable<?> c){ScaleValidationProbe.count(2);}
    @Inject(method="loaded",at=@At("HEAD")) private static void loaded(CallbackInfoReturnable<?> c){ScaleValidationProbe.count(3);}
}
