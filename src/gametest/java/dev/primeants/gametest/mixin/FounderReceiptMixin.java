package dev.primeants.gametest.mixin;
import dev.primeants.gametest.FounderGrowthProbe;
import dev.primeants.worker.FoodLimitedGrowth;
import java.util.List;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FoodLimitedGrowth.class)
public abstract class FounderReceiptMixin {
    @Shadow @Final private List<Long> times;
    @Shadow @Final private List<Long> sugar;
    @Shadow @Final private List<Long> protein;
    @Inject(method="observe",at=@At("RETURN"))
    private void receipt(long loaded,FoodLimitedGrowth.Supply supply,CallbackInfo ci){FounderGrowthProbe.sampled((FoodLimitedGrowth)(Object)this,loaded,supply,times,sugar,protein);}
}
