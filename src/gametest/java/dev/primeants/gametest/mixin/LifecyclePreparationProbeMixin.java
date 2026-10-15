package dev.primeants.gametest.mixin;
import dev.primeants.gametest.LifecycleGameTest;
import dev.primeants.gametest.LifecyclePreparationProbe;
import net.minecraft.gametest.framework.GameTestHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LifecycleGameTest.class)
public abstract class LifecyclePreparationProbeMixin {
    @Inject(method="environmentalChickenLootFundsNewWorkerThenQueenDeathEndsColony",at=@At("RETURN"))
    private void trace(GameTestHelper c,CallbackInfo ci){LifecyclePreparationProbe.attach(c);}
}
