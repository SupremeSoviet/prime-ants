package dev.primeants.gametest;

import com.google.gson.Gson;
import dev.primeants.PrimeAnts;
import dev.primeants.brood.BroodPile;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.Mining;
import dev.primeants.founding.NestPlan;
import dev.primeants.gametest.mixin.GameTestHelperAccessor;
import dev.primeants.worker.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.pathfinder.Path;

/** Passive, test-only executed-decision trace. Never creates paths or calls an admission gate. */
public final class MinerCausalProbe {
    public static final String TARGET="prime_ants_test:mining_game_test_mid_haul_disk_reload_resumes_then_miner_death_releases_one_unit_exactly_once";
    private static final Gson JSON=new Gson();
    private static GameTestHelper helper;
    private static UUID queen,named;
    private static boolean enabled;
    public static LasiusNigerEntity current;
    public static int pathDepth;
    public static boolean predicateCall;
    private static final Deque<String> calls=new ArrayDeque<>();
    private static final Map<UUID,String> states=new HashMap<>();
    private MinerCausalProbe(){}
    public static void attach(GameTestHelper c,UUID q){
        if(!((GameTestHelperAccessor)c).primeAntsTestInfo().id().toString().equals(TARGET))return;
        helper=c;queen=q;named=null;enabled=false;states.clear();
        emit(null,"attach",Map.of("seed",c.getLevel().getSeed(),"rotation",c.getTestRotation().name(),"bounds",c.getBounds().toString(),"namespace",TestIdentities.NAMESPACE));
        ((GameTestHelperAccessor)c).primeAntsTestInfo().addListener(new GameTestListener(){
            public void testStructureLoaded(GameTestInfo i){}
            public void testPassed(GameTestInfo i,GameTestRunner r){finish("passed");}
            public void testFailed(GameTestInfo i,GameTestRunner r){finish("failure_listener");}
            public void testAddedForRerun(GameTestInfo i,GameTestInfo copy,GameTestRunner r){finish("rerun");}
        });
        c.onEachTick(()->{
            var j=Mining.get(c.getLevel()).job(queen);
            if(named==null&&j!=null&&!j.transfers().isEmpty()){
                named=UUID.fromString(j.transfers().keySet().iterator().next());enabled=true;
                emit(null,"custody_begin",Map.of("named",named,"transfers",j.transfers()));snapshot();
            }
            if(enabled&&c.getTick()%20==0)snapshot();
        });
    }
    private static void finish(String why){if(helper!=null){emit(null,"endpoint",why);if(enabled)snapshot();}enabled=false;current=null;calls.clear();}
    public static boolean observes(LasiusNigerEntity w){return enabled&&helper!=null&&w.level()==helper.getLevel()&&queen.equals(w.queenId());}
    public static boolean active(){return current!=null&&observes(current);}
    public static String context(){return calls.toString();}
    public static boolean directGate(){return active()&&pathDepth==0&&!calls.isEmpty()&&!calls.peek().equals("search");}
    public static void emit(LasiusNigerEntity w,String event,Object value){
        if(helper==null)return;var row=new LinkedHashMap<String,Object>();row.put("event",event);row.put("helperTick",helper.getTick());row.put("gameTime",helper.getLevel().getGameTime());row.put("queen",queen);row.put("named",named);row.put("worker",w==null?null:w.getUUID());row.put("calls",new ArrayList<>(calls));row.put("value",value);PrimeAnts.LOGGER.info("T21 MINER {}",JSON.toJson(row));
    }
    public static void event(LasiusNigerEntity w,String event,Object value){if(observes(w))emit(w,event,value);}
    public static void begin(LasiusNigerEntity w,String method){if(observes(w)){calls.push(method);emit(w,method+"_entry",worker(w));}}
    public static void end(LasiusNigerEntity w,String method){if(observes(w)){emit(w,method+"_return",worker(w));if(!calls.isEmpty())calls.pop();}}
    public static Map<String,Object> item(ItemEntity i){
        var m=new LinkedHashMap<String,Object>();m.put("id",i.getUUID());m.put("cargo",i.getItem().toString());m.put("position",i.position().toString());m.put("block",i.blockPosition().toShortString());m.put("box",i.getBoundingBox().toString());m.put("alive",i.isAlive());m.put("removed",i.isRemoved());m.put("delay",i.hasPickUpDelay());
        var l=helper.getLevel();m.put("canonical",l.getEntity(i.getUUID())==i);m.put("chunkLoaded_snapshot",l.hasChunkAt(i.blockPosition()));m.put("entityTicking_snapshot",l.isPositionEntityTicking(i.blockPosition()));
        if(current!=null){var p=current.workerTasks().plan();if(p!=null){var a=p.outside();var b=i.blockPosition();m.put("withinSearch_calculated_not_called",Math.abs(a.getX()-b.getX())<=24&&Math.abs(a.getZ()-b.getZ())<=24&&Math.abs(a.getY()-b.getY())<=3);m.put("distanceSquared",current.distanceToSqr(i));}}
        return m;
    }
    public static Object worker(LasiusNigerEntity w){var t=w.workerTasks();var m=new LinkedHashMap<String,Object>();m.put("canonical",helper.getLevel().getEntity(w.getUUID())==w);m.put("alive",w.isAlive());m.put("removed",w.isRemoved());m.put("phase",t.phase());m.put("reason",t.reason());m.put("source",t.sourceId());m.put("cargo",w.getMainHandItem().toString());m.put("position",w.position().toString());m.put("box",w.getBoundingBox().toString());m.put("ground",w.onGround());m.put("sharingBusy",t.sharing().busy());m.put("home",String.valueOf(w.nurseryHome()));m.put("flowerInspections",t.flowerInspections());m.put("navigation",F2CarryProbe.path(w.getNavigation().getPath()));return m;}
    public static void state(LasiusNigerEntity w){if(!observes(w))return;String key=w.workerTasks().phase()+":"+w.workerTasks().reason()+":"+w.workerTasks().sourceId()+":"+w.getMainHandItem();if(!key.equals(states.put(w.getUUID(),key)))emit(w,"worker_transition",worker(w));}
    public static void path(LasiusNigerEntity w,BlockPos p,Path result){if(observes(w))emit(w,"actual_createPath_return",Map.of("target",p.toShortString(),"path",F2CarryProbe.path(result)));}
    public static void selection(UUID q,String type,Object value){if(enabled&&queen.equals(q))emit(null,type,value);}
    private static void snapshot(){
        var l=helper.getLevel();var m=new LinkedHashMap<String,Object>();var e=l.getEntity(named);m.put("namedEntity",e instanceof ItemEntity i?item(i):String.valueOf(e));
        m.put("custody",TransferCustody.get(l).contents().stream().filter(t->t.id().equals(named)).map(t->t.toString()).toList());
        var q=l.getEntity(queen) instanceof LasiusNigerEntity a?a:null;
        if(q!=null){var p=q.founding().plan();m.put("foragerClaims",q.founding().foragers());m.put("members",ColonyMembers.get(l).members(queen).stream().map(Object::toString).toList());
            if(p!=null){m.put("outside",p.outside().toShortString());m.put("queryBox_calculated",new net.minecraft.world.phys.AABB(p.outside()).inflate(24,3,24).toString());if(l.getBlockEntity(p.nursery()) instanceof BroodPile pile)m.put("nurseryLoaded",pile.loadedTicks());
                var s=MaterialStore.owned(l,queen,p);m.put("store_snapshot_owned_only",s==null?null:Map.of("position",s.getBlockPos().toShortString(),"contents",s.contents().toString(),"size",s.size(),"capacity",s.capacity()));
                if(helper.getTick()%100==0)m.put("workers",ColonyMembers.get(l).members(queen).stream().map(v->l.getEntity(v.worker())).filter(v->v instanceof LasiusNigerEntity).map(v->Map.of("id",v.getUUID(),"state",worker((LasiusNigerEntity)v))).toList());
            }
        }emit(null,"periodic_snapshot_not_decision",m);
    }
}
