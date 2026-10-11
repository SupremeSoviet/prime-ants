package dev.primeants.gametest.mixin;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.worker.WorkerTasks;
import dev.primeants.gametest.GreatSurfaceProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(WorkerTasks.class)
public abstract class GreatWorkerProbeMixin {
    @Shadow @Final private LasiusNigerEntity worker;
    @Shadow private int harvestingTicks,cooldown,phaseTicks;
    @Shadow private BlockPos surfaceStand,surfaceTarget;
    @Shadow private WorkerTasks.Phase phase;
    @Inject(method="tick",at=@At("HEAD"))
    private void begin(ServerLevel l,CallbackInfo r){GreatSurfaceProbe.begin(l,worker,harvestingTicks,phase);}
    @Inject(method="tick",at=@At("RETURN"))
    private void end(ServerLevel l,CallbackInfo r){GreatSurfaceProbe.end(l,worker,harvestingTicks,cooldown,phaseTicks,surfaceStand,surfaceTarget);}
    @Inject(method="surface",at=@At("HEAD"))
    private void body(ServerLevel l,CallbackInfo r){GreatSurfaceProbe.body(l,worker);}
    @Inject(method="surfaceAuthorized",at=@At("RETURN"))
    private void authorized(ServerLevel l,CallbackInfoReturnable<Boolean> r){GreatSurfaceProbe.observed(l,worker.queenId(),worker,"surfaceAuthorized",r.getReturnValue());}
    @Inject(method="arriveSupported",at=@At("RETURN"))
    private void arrive(ServerLevel l,Vec3 dest,CallbackInfoReturnable<Boolean> r){GreatSurfaceProbe.observed(l,worker.queenId(),worker,"arriveSupported",r.getReturnValue());}
}
