package dev.primeants.gametest;

import com.google.gson.Gson;
import dev.primeants.PrimeAnts;
import dev.primeants.brood.*;
import dev.primeants.colony.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.gametest.mixin.GameTestHelperAccessor;
import dev.primeants.worker.WorkerTasks;
import dev.primeants.worker.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Read-only, one-test scope. All guard/selector observations are original RETURN values. */
public final class GreatSurfaceProbe implements AutoCloseable {
    private static final Map<UUID,GreatSurfaceProbe> scopes=new HashMap<>();
    private static final Gson json=new Gson();
    private final GameTestHelper test;
    private final LasiusNigerEntity queen;
    private final ServerLevel level;
    private final Set<BlockPos> initial;
    private final Map<String,String> transitions=new HashMap<>();
    private final Map<String,Long> calls=new TreeMap<>(),times=new TreeMap<>();
    private final Map<UUID,Frame> frames=new HashMap<>();
    private final Map<UUID,Long> lastController=new HashMap<>();
    private final Map<UUID,Map<String,Object>> latestWorkers=new LinkedHashMap<>();
    private long helperSamples,controllerSamples,duplicates,controllerGaps,computed=-1,mature=-1,admitted=-1,lastHelper=-1,helperGaps;
    private int events,suppressed;
    private boolean closed;
    private static final class Frame {
        final SurfaceWork.Job job;final int actionBefore;final WorkerTasks.Phase phase;
        final Map<String,Object> values=new LinkedHashMap<>();boolean body,arrive,commit;
        Frame(SurfaceWork.Job job,int action,WorkerTasks.Phase phase){this.job=job;actionBefore=action;this.phase=phase;}
    }
    private GreatSurfaceProbe(GameTestHelper test,LasiusNigerEntity queen,Set<BlockPos> initial){
        this.test=test;this.queen=queen;level=test.getLevel();this.initial=Set.copyOf(initial);scopes.put(queen.getUUID(),this);
    }
    public static GreatSurfaceProbe watch(GameTestHelper test,LasiusNigerEntity queen,Set<BlockPos> initial){
        var scope=new GreatSurfaceProbe(test,queen,initial);
        ((GameTestHelperAccessor)test).primeAntsTestInfo().addListener(new GameTestListener(){
            public void testStructureLoaded(GameTestInfo i){}
            public void testPassed(GameTestInfo i,GameTestRunner r){scope.finish("passed");}
            public void testFailed(GameTestInfo i,GameTestRunner r){scope.finish("failed");}
            public void testAddedForRerun(GameTestInfo i,GameTestInfo copy,GameTestRunner r){scope.finish("rerun_refused");}
        });scope.emit("start",Map.of("deadline",12000,"initialSoil",initial.size()));return scope;
    }
    private static GreatSurfaceProbe scope(ServerLevel l,UUID owner){var p=scopes.get(owner);return p!=null&&p.level==l&&!p.closed?p:null;}
    private Map<String,Object> base(){
        var m=new LinkedHashMap<String,Object>();m.put("queen",queen.getUUID().toString());m.put("helperTick",test.getTick());m.put("gameTime",level.getGameTime());
        var p=NestPlanFixture.pile(test,queen);m.put("nurseryLoaded",p==null?null:p.loadedTicks());return m;
    }
    private void emit(String type,Object value){var m=base();m.put("type",type);m.put("value",value);PrimeAnts.LOGGER.info("T19 GREAT OBS {}",json.toJson(m));}
    private void event(String key,Object value){String encoded=json.toJson(value);if(encoded.equals(transitions.put(key,encoded)))return;
        if(events++<4096)emit(key,value);else suppressed++;
    }
    private Object pos(BlockPos p){return p==null?null:List.of(p.getX()-queen.founding().plan().entrance().getX(),p.getY()-queen.founding().plan().entrance().getY(),p.getZ()-queen.founding().plan().entrance().getZ());}
    private Object pos(Vec3 p){return List.of(p.x-queen.founding().plan().entrance().getX(),p.y-queen.founding().plan().entrance().getY(),p.z-queen.founding().plan().entrance().getZ());}
    private Map<String,Object> job(SurfaceWork.Job j){
        var m=new LinkedHashMap<String,Object>();m.put("stage",j.plan.stage());m.put("completed",j.completed());m.put("cost",j.plan.cost());m.put("recovered",j.recovered());m.put("placed",j.placed());m.put("released",j.released());m.put("carried",j.carried());m.put("claim",j.claim);m.put("source",pos(j.source));m.put("reasonDescriptive",j.reason);
        if(!j.complete()){m.put("next",pos(j.next()));m.put("component",j.plan.cells().get(j.completed()));}return m;
    }
    public void sample(){
        if(closed)return;long tick=test.getTick();if(lastHelper>=0&&tick-lastHelper>1)helperGaps+=tick-lastHelper-1;lastHelper=tick;helperSamples++;
        jobs();if(tick%500==0)emit("summary",snapshot(false));
    }
    private void jobs(){
        long tick=test.getTick();
        var work=SurfaceWork.get(level);var a=work.job(queen.getUUID(),ColonyStage.MATURE);var b=work.job(queen.getUUID(),ColonyStage.GREAT);
        if(a!=null&&a.complete()&&mature<0){mature=tick;emit("mature_complete",job(a));}
        if(b!=null&&admitted<0){admitted=tick;emit("great_admitted",job(b));}
        for(var j:work.jobs(queen.getUUID()))event("job_transition_"+j.plan.stage(),job(j));
    }
    public static void considered(ServerLevel l,LasiusNigerEntity q){var p=scope(l,q.getUUID());if(p!=null)p.jobs();}
    public static void evaluated(ServerLevel l,UUID owner,long loaded,ColonyDevelopment.Evaluation e){
        var p=scope(l,owner);if(p!=null&&p.computed<0&&e.result().certain()==ColonyStage.GREAT){p.computed=p.test.getTick();p.emit("first_computed_great",Map.of("evaluationLoaded",loaded,"stage",e.stage().toString()));}
    }
    public static void begin(ServerLevel l,LasiusNigerEntity w,int action,WorkerTasks.Phase phase){
        var p=scope(l,w.queenId());if(p==null)return;
        var j=SurfaceWork.get(l).claimedBy(w);if(j!=null)p.frames.put(w.getUUID(),new Frame(j,action,phase));
    }
    public static void body(ServerLevel l,LasiusNigerEntity w){var p=scope(l,w.queenId());if(p!=null&&p.frames.containsKey(w.getUUID()))p.frames.get(w.getUUID()).body=true;}
    public static void observed(ServerLevel l,UUID owner,LasiusNigerEntity w,String kind,Object value){
        var p=scope(l,owner);if(p==null)return;p.calls.merge(kind+":"+String.valueOf(value),1L,Long::sum);
        if(w!=null){var f=p.frames.get(w.getUUID());if(f!=null){f.values.put(kind,value);if(kind.equals("arriveSupported"))f.arrive=true;if(kind.equals("recover")||kind.equals("place"))f.commit=true;}}
    }
    public static void target(ServerLevel l,UUID owner,SurfaceWork.Job j,String result){
        var p=scope(l,owner);if(p==null)return;observed(l,owner,j.claim!=null&&l.getEntity(j.claim) instanceof LasiusNigerEntity w?w:null,"targetProblem",result==null?"ok":result);
        if(result!=null&&!result.equals("complete"))p.event("target_refusal_"+j.plan.stage(),Map.of("result",result,"job",p.job(j)));
    }
    public static void source(ServerLevel l,UUID owner,BlockPos at,boolean result){
        var p=scope(l,owner);if(p==null)return;observed(l,owner,null,"recoverable",result);
        for(var e:p.frames.entrySet())if(Objects.equals(e.getValue().job.source,at)){
            e.getValue().values.put("recoverable",result);
            if(!result)p.event("source_refusal",Map.of("worker",e.getKey(),"source",p.pos(at),"result",false));
        }
    }
    public static void priority(ServerLevel l,UUID owner,boolean result){
        var p=scope(l,owner);if(p==null)return;observed(l,owner,null,"priority",result);
        for(var f:p.frames.values())f.values.put("priority",result);
        p.event("priority_transition",result);
    }
    public static void receipt(ServerLevel l,LasiusNigerEntity w,SurfaceWork.Job j,String mode){
        var owner=w==null?null:w.queenId();
        // Inheritance has no actor; find only an already active matching world/job scope.
        if(w==null){for(var p:List.copyOf(scopes.values()))if(p.level==l&&SurfaceWork.get(l).jobs(p.queen.getUUID()).contains(j))p.emit("receipt",Map.of("mode",mode,"receipt",j.receipts().getLast(),"job",p.job(j)));}
        else{var p=scope(l,owner);if(p!=null){p.emit("receipt",Map.of("mode",mode,"receipt",j.receipts().getLast(),"job",p.job(j)));p.jobs();}}
    }
    public static void released(ServerLevel l,LasiusNigerEntity w,SurfaceWork.Job j,String why){var p=scope(l,w.queenId());if(p!=null)p.emit("release",Map.of("worker",w.getUUID(),"why",why,"job",p.job(j)));}
    public static void end(ServerLevel l,LasiusNigerEntity w,int action,int cooldown,int phaseTicks,BlockPos stand,BlockPos target){
        var p=scope(l,w.queenId());if(p==null)return;var f=p.frames.remove(w.getUUID());if(f==null)return;
        Long last=p.lastController.put(w.getUUID(),l.getGameTime());if(last!=null&&last==l.getGameTime()){p.duplicates++;return;}
        // Counts are controller observations, NOT helper or nursery elapsed time. Claim gaps are explicit.
        if(last!=null&&l.getGameTime()-last>1)p.controllerGaps+=l.getGameTime()-last-1;p.controllerSamples++;
        String kind;
        if(Boolean.FALSE.equals(f.values.get("surfaceAuthorized"))||Boolean.TRUE.equals(f.values.get("priority"))
            ||f.values.containsKey("targetProblem")&&!"ok".equals(f.values.get("targetProblem"))
            ||Boolean.FALSE.equals(f.values.get("recoverable"))||f.body&&stand==null&&!Boolean.TRUE.equals(f.values.get("recover")))kind="blocked_observed";
        else if(f.arrive)kind=f.phase==WorkerTasks.Phase.SURFACE_FETCH?"travelling_fetch":"travelling_build";
        else if(f.body&&(action>0||f.actionBefore>0||f.commit))kind=f.phase==WorkerTasks.Phase.SURFACE_FETCH?"fetching_action":"building_action";
        else kind=cooldown>0?"cooldown":"unknown_or_interrupted";
        p.times.merge(f.job.plan.stage()+":"+kind,1L,Long::sum);
        var m=new LinkedHashMap<String,Object>();m.put("worker",w.getUUID());m.put("stage",f.job.plan.stage());m.put("phaseBefore",f.phase);m.put("phaseAfter",w.workerTasks().phase());m.put("category",kind);m.put("position",p.pos(w.position()));m.put("stand",p.pos(stand));m.put("target",p.pos(target));m.put("onGround",w.onGround());m.put("cargo",w.getMainHandItem().toString());m.put("actionBefore",f.actionBefore);m.put("actionAfter",action);m.put("cooldown",cooldown);m.put("phaseTicks",phaseTicks);m.put("observedResults",f.values);
        var path=w.getNavigation().getPath();m.put("path",path==null?null:Map.of("canReach",path.canReach(),"done",path.isDone(),"nextIndex",path.getNextNodeIndex(),"nodes",path.getNodeCount()));
        p.latestWorkers.put(w.getUUID(),m);
        // Deduplicate by semantic controller transition; full physical state at transitions and 500 ticks.
        String key=w.getUUID()+":"+f.phase+":"+w.workerTasks().phase()+":"+kind+":"+p.pos(stand)+":"+p.pos(target)+":"+w.getMainHandItem()+":"+f.values;
        if(!key.equals(p.transitions.put("controller:"+w.getUUID(),key))||p.test.getTick()%500==0){if(p.events++<4096)p.emit("controller",m);else p.suppressed++;}
    }
    private Map<String,Object> snapshot(boolean endpoint){
        var m=new LinkedHashMap<String,Object>();m.put("firstComputed",computed);m.put("matureComplete",mature);m.put("greatAdmitted",admitted);m.put("helperSamples",helperSamples);m.put("helperGaps",helperGaps);m.put("controllerSamples",controllerSamples);m.put("duplicateControllerCalls",duplicates);m.put("betweenClaimControllerGaps",controllerGaps);m.put("controllerTimes",new TreeMap<>(times));m.put("originalReturnCounts",new TreeMap<>(calls));m.put("suppressedEvents",suppressed);
        var jobs=SurfaceWork.get(level).jobs(queen.getUUID());m.put("jobs",jobs.stream().map(this::job).toList());
        var all=new HashSet<>(initial);for(var j:jobs)all.addAll(j.cells());
        long blocks=all.stream().filter(at->level.getBlockState(at).is(NurseryBlocks.NEST_SOIL)||level.getBlockState(at).is(NurseryBlocks.MOUND_GATE)).count();
        var workers=level.getEntitiesOfClass(LasiusNigerEntity.class,test.getBounds().inflate(8),w->queen.getUUID().equals(w.queenId()));
        long cargo=workers.stream().filter(w->w.getMainHandItem().is(Items.DIRT)).mapToInt(w->w.getMainHandItem().getCount()).sum();
        long ground=level.getEntitiesOfClass(ItemEntity.class,test.getBounds().inflate(8),i->i.isAlive()&&i.getItem().is(Items.DIRT)).stream().mapToInt(i->i.getItem().getCount()).sum();
        long custody=TransferCustody.get(level).contents().stream().filter(t->t.stack().is(Items.DIRT)&&test.getBounds().inflate(8).contains(t.position())).mapToInt(t->t.stack().getCount()).sum();
        m.put("physicalSoil",Map.of("blocks",blocks,"cargo",cargo,"ground",ground,"custody",custody,"total",blocks+cargo+ground+custody,"initial",initial.size()));
        var store=MaterialStore.owned(level,queen.getUUID(),queen.founding().plan());m.put("stoneInStore",store==null?null:store.units(MaterialUnits.Material.STONE));
        if(endpoint){var missing=new ArrayList<Object>();var great=SurfaceWork.get(level).job(queen.getUUID(),ColonyStage.GREAT);
            if(great!=null)for(int i=0;i<great.plan.cost();i++){var at=great.at(i);var cell=great.plan.cells().get(i);boolean owned=ColonyTerrain.get(level).surface(level,at,queen.getUUID());boolean material=level.getBlockState(at).is(cell.material().equals("gate")?NurseryBlocks.MOUND_GATE:NurseryBlocks.NEST_SOIL);
                if(!owned||!material)missing.add(Map.of("index",i,"cell",cell,"position",pos(at),"block",level.getBlockState(at).toString(),"owned",owned,"correctMaterial",material));}
            m.put("unfinishedPhysicalCells",missing);m.put("receipts",jobs.stream().map(j->Map.of("stage",j.plan.stage(),"receipts",j.receipts(),"transfers",j.transfers())).toList());m.put("lastControllerStates",latestWorkers.values());
        }return m;
    }
    private void finish(String outcome){if(closed)return;try{var m=snapshot(true);m.put("outcome",outcome);emit("endpoint",m);}finally{close();}}
    @Override public void close(){closed=true;scopes.remove(queen.getUUID(),this);frames.clear();}
}
