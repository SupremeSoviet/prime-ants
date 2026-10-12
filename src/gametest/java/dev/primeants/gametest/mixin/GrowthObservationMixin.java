package dev.primeants.gametest.mixin;

import dev.primeants.brood.BroodPile;
import dev.primeants.gametest.GrowthObservations;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BroodPile.class)
public abstract class GrowthObservationMixin {
    @Inject(method="nextLayingEvent",at=@At("RETURN"))
    private void laying(ServerLevel l,String habitat,CallbackInfoReturnable<Long> ci){GrowthObservations.layingDecision(l,(BroodPile)(Object)this,ci.getReturnValue());}
    @Inject(method="advanceAggregate",at=@At("HEAD"))
    private void before(ServerLevel l,long ticks,long time,String habitat,CallbackInfo ci){GrowthObservations.sample(l,(BroodPile)(Object)this,time-ticks,"aggregate_after_allocation_before_development");}
    @Inject(method="advanceAggregate",at=@At("RETURN"))
    private void after(ServerLevel l,long ticks,long time,String habitat,CallbackInfo ci){GrowthObservations.sample(l,(BroodPile)(Object)this,time,"aggregate_after_development");}
}
