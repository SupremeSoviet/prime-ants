package dev.primeants.gametest.mixin;

import dev.primeants.brood.BroodPile;
import dev.primeants.gametest.StageClockObservations;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BroodPile.class)
public abstract class StageClockObservationMixin {
    @Inject(method="evaluateAggregateStart",at=@At("RETURN"))
    private void start(ServerLevel l,long remaining,CallbackInfo ci){StageClockObservations.sample(l,(BroodPile)(Object)this,l.getGameTime(),"aggregate_start_evaluated");}
    @Inject(method="refreshAggregateStage",at=@At("RETURN"))
    private void refresh(ServerLevel l,boolean first,CallbackInfo ci){StageClockObservations.sample(l,(BroodPile)(Object)this,l.getGameTime(),"aggregate_stage_refresh");}
    @Inject(method="observeUnknownTerrain",at=@At("RETURN"))
    private void unknown(ServerLevel l,CallbackInfo ci){StageClockObservations.sample(l,(BroodPile)(Object)this,l.getGameTime(),"unknown_evaluated");}
}
