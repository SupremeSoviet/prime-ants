package dev.primeants.gametest.mixin;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.gametest.GreatSurfaceProbe;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(SurfaceWork.class)
public abstract class GreatSurfaceWorkProbeMixin {
    @Inject(method="consider",at=@At("RETURN"))
    private void considered(ServerLevel l,LasiusNigerEntity q,java.util.List<LasiusNigerEntity> workers,CallbackInfo r){GreatSurfaceProbe.considered(l,q);}
    @Inject(method="priority",at=@At("RETURN"))
    private void priority(ServerLevel l,UUID owner,NestPlan home,CallbackInfoReturnable<Boolean> r){GreatSurfaceProbe.priority(l,owner,r.getReturnValue());}
    @Inject(method="targetProblem",at=@At("RETURN"))
    private static void target(ServerLevel l,UUID owner,SurfaceWork.Job j,CallbackInfoReturnable<String> r){GreatSurfaceProbe.target(l,owner,j,r.getReturnValue());}
    @Inject(method="recoverable",at=@At("RETURN"))
    private static void source(ServerLevel l,NestPlan home,UUID owner,BlockPos at,CallbackInfoReturnable<Boolean> r){GreatSurfaceProbe.source(l,owner,at,r.getReturnValue());}
    @Inject(method="stand",at=@At("RETURN"))
    private static void stand(ServerLevel l,LasiusNigerEntity w,BlockPos at,boolean empty,CallbackInfoReturnable<BlockPos> r){GreatSurfaceProbe.observed(l,w.queenId(),w,"stand",r.getReturnValue()==null?"refused":"selected");}
    @Inject(method="validStand",at=@At("RETURN"))
    private static void valid(ServerLevel l,LasiusNigerEntity w,BlockPos at,boolean empty,BlockPos stand,CallbackInfoReturnable<Boolean> r){GreatSurfaceProbe.observed(l,w.queenId(),w,"validStand",r.getReturnValue());}
    @Inject(method="point",at=@At("RETURN"))
    private static void point(ServerLevel l,LasiusNigerEntity w,BlockPos at,boolean empty,CallbackInfoReturnable<net.minecraft.world.phys.Vec3> r){GreatSurfaceProbe.observed(l,w.queenId(),w,"point",r.getReturnValue()!=null);}
    @Inject(method="recover",at=@At("RETURN"))
    private void recover(ServerLevel l,LasiusNigerEntity w,SurfaceWork.Job j,CallbackInfoReturnable<Boolean> r){GreatSurfaceProbe.observed(l,w.queenId(),w,"recover",r.getReturnValue());}
    @Inject(method="place",at=@At("RETURN"))
    private void place(ServerLevel l,LasiusNigerEntity w,SurfaceWork.Job j,CallbackInfoReturnable<Boolean> r){GreatSurfaceProbe.observed(l,w.queenId(),w,"place",r.getReturnValue());}
    @Inject(method="relinquish",at=@At("RETURN"))
    private void release(ServerLevel l,LasiusNigerEntity w,SurfaceWork.Job j,String why,CallbackInfo r){GreatSurfaceProbe.released(l,w,j,why);}
}
