package dev.primeants.gametest.mixin;

import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.worker.WorkerTasks;
import dev.primeants.gametest.SurfaceChamberPayments;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe the existing call and its physical payment, with no altered result or additional world search. */
@Mixin(WorkerTasks.class)
public abstract class SurfaceChamberPaymentMixin {
    @Shadow @Final private LasiusNigerEntity worker;
    @Unique private SurfaceChamberPayments.Before primeAntsBefore;
    @Inject(method="upgradeBuild", at=@At("HEAD"))
    private void before(ServerLevel l, CallbackInfo ci) { primeAntsBefore = SurfaceChamberPayments.before(worker); }
    @Inject(method="upgradeBuild", at=@At("RETURN"))
    private void after(ServerLevel l, CallbackInfo ci) { SurfaceChamberPayments.after(primeAntsBefore); primeAntsBefore = null; }
}
