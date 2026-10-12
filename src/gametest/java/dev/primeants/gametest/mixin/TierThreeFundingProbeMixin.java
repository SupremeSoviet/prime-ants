package dev.primeants.gametest.mixin;
import dev.primeants.gametest.TierThreeEffectsProbe;
import dev.primeants.worker.FoodLimitedGrowth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(FoodLimitedGrowth.class)
public abstract class TierThreeFundingProbeMixin {
    @Inject(method="allows",at=@At("RETURN"))
    private void actualDecision(FoodLimitedGrowth.Supply s,CallbackInfoReturnable<Boolean> ci){TierThreeEffectsProbe.fundingDecision((FoodLimitedGrowth)(Object)this,s,ci.getReturnValue());dev.primeants.gametest.HallFundingProbe.fundingDecision((FoodLimitedGrowth)(Object)this,s,ci.getReturnValue());}
}
