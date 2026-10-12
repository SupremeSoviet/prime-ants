package dev.primeants.gametest.mixin;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.FoodServiceProbe;
import dev.primeants.worker.WorkerTasks;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(WorkerTasks.class)
public abstract class FoodServiceWorkerMixin {
    @Shadow @Final private LasiusNigerEntity worker;
    @Inject(method="tick",at=@At("RETURN"))
    private void observe(ServerLevel l,CallbackInfo ci){FoodServiceProbe.workerTick(l,worker);}
}
