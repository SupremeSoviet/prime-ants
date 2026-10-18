package dev.primeants.gametest.mixin;
import dev.primeants.gametest.MiningPreparationCausalProbe;
import dev.primeants.worker.FoodLimitedGrowth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(FoodLimitedGrowth.class)
public abstract class PreparationGrowthMixin {
    @Inject(method="allows",at=@At("RETURN")) private void observed(FoodLimitedGrowth.Supply s,CallbackInfoReturnable<Boolean> ci){MiningPreparationCausalProbe.growth((FoodLimitedGrowth)(Object)this,s,ci.getReturnValue());}
}
