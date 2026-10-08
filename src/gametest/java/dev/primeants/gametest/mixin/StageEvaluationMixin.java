package dev.primeants.gametest.mixin;
import dev.primeants.colony.ColonyDevelopment;
import dev.primeants.founding.NestPlan;
import dev.primeants.gametest.StageEvaluations;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Observe only; no evaluation result, terrain or clock is modified. */
@Mixin(ColonyDevelopment.class)
public abstract class StageEvaluationMixin {
    @Inject(method="evaluate",at=@At("RETURN"))
    private static void primeAntsObserveEvaluation(ServerLevel level,UUID queen,NestPlan plan,boolean operational,int bound,long loaded,CallbackInfoReturnable<ColonyDevelopment.Evaluation> result){
        StageEvaluations.observed(level,queen,loaded,result.getReturnValue());
    }
}
