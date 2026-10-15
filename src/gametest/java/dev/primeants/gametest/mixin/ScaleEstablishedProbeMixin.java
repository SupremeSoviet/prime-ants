package dev.primeants.gametest.mixin;
import dev.primeants.founding.EstablishedNest;
import dev.primeants.gametest.ScaleValidationProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EstablishedNest.class)
public abstract class ScaleEstablishedProbeMixin {
    @Inject(method="findings",at=@At("HEAD")) private void findings(CallbackInfo c){ScaleValidationProbe.count(8);}
}
