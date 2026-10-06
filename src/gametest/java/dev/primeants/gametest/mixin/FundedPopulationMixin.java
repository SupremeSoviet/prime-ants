package dev.primeants.gametest.mixin;
import dev.primeants.brood.BroodPile;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.FundedPopulationFixture;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BroodPile.class)
public abstract class FundedPopulationMixin {
    @Inject(method="lay",at=@At("HEAD"),cancellable=true)
    private void setupOnly(ServerLevel l,LasiusNigerEntity q,boolean care,CallbackInfoReturnable<Boolean> cir){
        if(FundedPopulationFixture.held((BroodPile)(Object)this))cir.setReturnValue(false);
    }
}
