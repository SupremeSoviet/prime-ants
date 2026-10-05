package dev.primeants.gametest;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.primeants.PrimeAnts;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.worker.WorkerTasks;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.*;

/** Development-only passive trace: getters/serialization only, no path creation or random draws. */
final class ConstructionTrace {
    private Object expansion, terrain, plugs;
    private String records="";
    void tick(GameTestHelper c,LasiusNigerEntity q) {
        var l=c.getLevel();var data=NestExpansion.get(l);var j=data.job(q.getUUID());if(j==null)return;
        var t=ColonyTerrain.get(l);var p=ColonyPlugs.get(l);
        var encoded=ColonyTerrain.CODEC.encodeStart(JsonOps.INSTANCE,t).getOrThrow().getAsJsonObject();
        var relevant=new JsonObject();
        for(var b:j.home.tasks())if(encoded.has(Long.toString(b.asLong())))relevant.add(Long.toString(b.asLong()),encoded.get(Long.toString(b.asLong())));
        for(var b:j.tasks)if(encoded.has(Long.toString(b.asLong())))relevant.add(Long.toString(b.asLong()),encoded.get(Long.toString(b.asLong())));
        if(expansion!=data||terrain!=t||plugs!=p||!records.equals(relevant.toString())){
            PrimeAnts.LOGGER.info("T13 saved-data tick={} queen={} expansionReplacement={} terrainReplacement={} plugsReplacement={} recordsBefore={} recordsAfter={}",c.getTick(),q.getUUID(),expansion!=data,terrain!=t,plugs!=p,records,relevant);
            expansion=data;terrain=t;plugs=p;records=relevant.toString();
        }
        if(c.getTick()%200!=0)return;
        var target=j.removed()<j.tasks.size()?j.tasks.get(j.removed()):null;
        var workers=l.getEntitiesOfClass(LasiusNigerEntity.class,c.getBounds().inflate(8),w->w.isAlive()&&q.getUUID().equals(w.queenId()));
        var b=workers.stream().filter(w->w.getUUID().equals(j.claim)).findFirst().orElse(null);
        var stands=new ArrayList<String>();
        if(target!=null&&b!=null)for(var d:Direction.Plane.HORIZONTAL)for(int y=-1;y<=0;y++){
            var feet=target.relative(d).offset(0,y,0);var face=new BlockPos(feet.getX(),target.getY(),feet.getZ());var pos=Vec3.atBottomCenterOf(feet);
            stands.add(feet+" walk="+NestPlan.walkable(l,feet)+" support="+l.getBlockState(feet.below())+" face="+l.getBlockState(face)+" reach="+pos.add(0,.5,0).distanceToSqr(Vec3.atCenterOf(target))+" bodies="+l.getEntities(b,b.getBoundingBox().move(pos.subtract(b.position()))).stream().map(e->e.getUUID().toString()).toList());
        }
        String path="none",move="none",chosen="none";
        if(b!=null){var nav=b.getNavigation().getPath();if(nav!=null)path="target="+nav.getTarget()+" reached="+nav.canReach()+" index="+nav.getNextNodeIndex()+" nodes="+java.util.stream.IntStream.range(0,nav.getNodeCount()).mapToObj(nav::getNodePos).toList();
            var m=b.getMoveControl();move=m.hasWanted()+" "+m.getWantedX()+","+m.getWantedY()+","+m.getWantedZ();
            if(target!=null)try{var method=WorkerTasks.class.getDeclaredMethod("digStand",net.minecraft.server.level.ServerLevel.class,BlockPos.class);method.setAccessible(true);chosen=String.valueOf(method.invoke(b.workerTasks(),l,target));}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }
        PrimeAnts.LOGGER.info("T13 construction tick={} queen={} ready={} queenReason={} claim={} removed={} deposited={} released={} jobProblem={} target={} actual={} expected={} permission={} chosen={} stands={} path={} move={} circulation={} workers={}",
            c.getTick(),q.getUUID(),q.founding().ready(),q.founding().reason()+" pos="+q.position()+" body="+q.getBoundingBox(),j.claim,j.removed(),j.deposited,j.released,j.problem(l,q.getUUID()),target,target==null?null:l.getBlockState(target),target==null?null:j.expected.get(j.removed()),target!=null&&t.eligible(l,target,q.getUUID()),chosen,stands,path,move,data.circulationSpace(l,q.getUUID()),workers.stream().map(w->w.getUUID()+" pos="+w.position()+" phase="+w.workerTasks().phase()+" reason="+w.workerTasks().reason()+" cargo="+w.getMainHandItem()+" enabled="+!w.isNoAi()+" ground="+w.onGround()+" recipient="+w.workerTasks().recipientId()+" route="+route(w)).toList());
    }
    private static String route(LasiusNigerEntity w){var p=w.getNavigation().getPath();var m=w.getMoveControl();return "path="+(p==null?"none":"target="+p.getTarget()+" reached="+p.canReach()+" index="+p.getNextNodeIndex()+" nodes="+java.util.stream.IntStream.range(0,p.getNodeCount()).mapToObj(p::getNodePos).toList())+" wanted="+m.hasWanted()+" move="+m.getWantedX()+","+m.getWantedY()+","+m.getWantedZ();}
}
