package dev.primeants.gametest.mixin;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.TierThreeEffectsProbe;
import dev.primeants.worker.WorkerTasks;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(WorkerTasks.class)
public abstract class TierThreeSearchProbeMixin {
    @Shadow @Final private LasiusNigerEntity worker;
    @Inject(method="search",at=@At("HEAD"))
    private void before(ServerLevel l,CallbackInfo ci){TierThreeEffectsProbe.searchBegin(l,worker);dev.primeants.gametest.FoodServiceProbe.search(l,worker,true);}
    @Inject(method="search",at=@At("RETURN"))
    private void after(ServerLevel l,CallbackInfo ci){TierThreeEffectsProbe.searchEnd(l,worker);dev.primeants.gametest.FoodServiceProbe.search(l,worker,false);}
}
