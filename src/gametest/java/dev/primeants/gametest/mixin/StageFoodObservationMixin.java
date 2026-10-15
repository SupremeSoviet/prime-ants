package dev.primeants.gametest.mixin;

import dev.primeants.brood.*;
import dev.primeants.worker.AggregateFeeding;
import dev.primeants.worker.NestCache;
import dev.primeants.entity.LasiusNigerEntity;
import java.util.List;
import dev.primeants.gametest.StageClockObservations;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AggregateFeeding.class)
public abstract class StageFoodObservationMixin {
    @Inject(method="distribute",at=@At("HEAD"))
    private void before(ServerLevel l,BroodPile p,List<LasiusNigerEntity> missing,NestCache cache,CallbackInfoReturnable<Boolean> ci){StageClockObservations.sample(l,p,l.getGameTime(),"before_fixed_distribution_at_observation");}
    @Inject(method="distribute",at=@At("RETURN"))
    private void after(ServerLevel l,BroodPile p,List<LasiusNigerEntity> missing,NestCache cache,CallbackInfoReturnable<Boolean> ci){StageClockObservations.sample(l,p,l.getGameTime(),"after_fixed_distribution_at_observation");}
}
