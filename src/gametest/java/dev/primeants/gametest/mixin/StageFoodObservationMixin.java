package dev.primeants.gametest.mixin;

import dev.primeants.brood.*;
import dev.primeants.worker.AggregateFeeding;
import dev.primeants.worker.NestCache;
import dev.primeants.gametest.StageClockObservations;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AggregateFeeding.class)
public abstract class StageFoodObservationMixin {
    @Inject(method="complete",at=@At("HEAD"))
    private void before(ServerLevel l,BroodPile p,NestCache cache,long now,CallbackInfo ci){StageClockObservations.sample(l,p,now,"before_food_completion");}
    @Inject(method="complete",at=@At("RETURN"))
    private void after(ServerLevel l,BroodPile p,NestCache cache,long now,CallbackInfo ci){StageClockObservations.sample(l,p,now,"after_food_completion");}
}
