package dev.primeants.gametest;

import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.SurfaceWork;
import dev.primeants.gametest.mixin.GameTestHelperAccessor;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.*;

/** Scoped permanent regression measurement of the original guard, before ordinary movement. */
public final class SurfaceOccupancyProbe implements AutoCloseable {
    public record Body(UUID worker,BlockPos target,BlockPos source,Vec3 position,AABB bounds,int recovered,int placed,int cargo) { }
    private static final Map<UUID,SurfaceOccupancyProbe> scopes=new HashMap<>();
    private final UUID owner;private final ServerLevel level;private Body first;
    private SurfaceOccupancyProbe(GameTestHelper test,UUID owner){this.owner=owner;level=test.getLevel();scopes.put(owner,this);}
    public static SurfaceOccupancyProbe watch(GameTestHelper test,UUID owner){
        var scope=new SurfaceOccupancyProbe(test,owner);
        ((GameTestHelperAccessor)test).primeAntsTestInfo().addListener(new GameTestListener(){
            public void testStructureLoaded(GameTestInfo i){}
            public void testPassed(GameTestInfo i,GameTestRunner r){scope.close();}
            public void testFailed(GameTestInfo i,GameTestRunner r){scope.close();}
            public void testAddedForRerun(GameTestInfo i,GameTestInfo copy,GameTestRunner r){scope.close();}
        });return scope;
    }
    public Body first(){return first;}
    public static void returned(ServerLevel level,UUID owner,SurfaceWork.Job job,String result){
        var scope=scopes.get(owner);
        if(scope==null||scope.level!=level||scope.first!=null||!"occupied_target".equals(result)||job.completed()!=0||job.claim==null)return;
        if(level.getEntity(job.claim) instanceof LasiusNigerEntity worker&&worker.workerTasks().phase()==dev.primeants.worker.WorkerTasks.Phase.SURFACE_FETCH
            &&worker.getBoundingBox().intersects(new AABB(job.next())))
            scope.first=new Body(worker.getUUID(),job.next(),job.source,worker.position(),worker.getBoundingBox(),job.recovered(),job.placed(),job.carried());
    }
    @Override public void close(){scopes.remove(owner,this);}
}
