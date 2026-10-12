package dev.primeants.gametest.mixin;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.gametest.FounderAccessProbe;
import dev.primeants.worker.WorkerTasks;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(WorkerTasks.class)
public abstract class FounderAccessWorkerMixin {
    @Shadow @Final private LasiusNigerEntity worker;
    @Inject(method="surfaceAccess",at=@At("HEAD"))
    private void begin(ServerLevel l,SurfaceWork work,SurfaceWork.Job j,CallbackInfo ci){FounderAccessProbe.begin(worker.queenId());}
    @Redirect(method="surfaceAccess",at=@At(value="INVOKE",target="Ldev/primeants/founding/SurfaceWork;priority(Lnet/minecraft/server/level/ServerLevel;Ljava/util/UUID;Ldev/primeants/founding/NestPlan;)Z"))
    private boolean priority(SurfaceWork work,ServerLevel l,java.util.UUID owner,NestPlan plan){boolean result=work.priority(l,owner,plan);FounderAccessProbe.priority(owner,result);return result;}
    @Redirect(method="surfaceAccess",at=@At(value="INVOKE",target="Ldev/primeants/founding/SurfaceWork$Job;unlocked(Lnet/minecraft/server/level/ServerLevel;Ljava/util/UUID;)Z"))
    private boolean unlocked(SurfaceWork.Job j,ServerLevel l,java.util.UUID owner){boolean result=j.unlocked(l,owner);FounderAccessProbe.unlocked(owner,result);return result;}
}
