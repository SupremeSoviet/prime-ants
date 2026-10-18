package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.PrimeAnts;
import dev.primeants.entity.LasiusNigerEntity;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.world.entity.Entity;

/** Development source set only. No world, path, nutrition or gameplay decision is changed here. */
public final class TestIdentities {
    public static final String NAMESPACE="prime_ants:gametest:identity:v1";
    private static final Map<GameTestInfo,Scope> cases=new IdentityHashMap<>();
    private static final Map<String,Integer> replicas=new HashMap<>();
    private static final Map<UUID,Scope> owners=new HashMap<>();
    private static final ThreadLocal<Deque<Scope>> active=ThreadLocal.withInitial(ArrayDeque::new);
    private static final Set<UUID> allocated=new HashSet<>();
    private static final class Scope {
        final GameTestInfo test; final String key; final Map<String,Long> counts=new HashMap<>();
        final Set<String> selections=new HashSet<>();
        final Map<UUID,Births> births=new HashMap<>();
        final boolean eventCompatibility;
        Scope(GameTestInfo t,String k){test=t;key=k;eventCompatibility=t==null||List.of(
            "adult_advance_game_test_","adult_reference_game_test_","automatic_catchup_game_test_",
            "catchup_lifecycle_game_test_","catchup_recovery_game_test_","colony_clock_game_test_",
            "reconciliation_game_test_","reconciliation_chunk_game_test_")
            .stream().anyMatch(prefix->t.id().getPath().startsWith(prefix));}
    }
    private static final class Births {
        long next;
        final Map<String,UUID> committed=new HashMap<>();
        final Map<UUID,Long> ordinals=new HashMap<>();
    }
    private TestIdentities(){}
    public static void enter(GameTestInfo t){
        var s=cases.computeIfAbsent(t,x->new Scope(t,t.id()+":replica:"+(replicas.merge(t.id().toString(),1,Integer::sum)-1)));
        active.get().push(s);
    }
    public static void enter(Entity e){
        Scope s=owners.get(e.getUUID());
        if(s==null&&e instanceof LasiusNigerEntity a)s=owners.get(a.queenId());
        // ArrayDeque cannot hold null; a marker balances unowned vanilla ticks.
        active.get().push(s==null?UNSCOPED:s);
    }
    private static final Scope UNSCOPED=new Scope(null,"unscoped");
    public static void leave(){active.get().pop();}
    private static Scope current(){var stack=active.get();return stack.isEmpty()||stack.peek()==UNSCOPED?null:stack.peek();}
    private static String caller(){
        return StackWalker.getInstance().walk(frames->frames
            .filter(f->f.getClassName().startsWith("dev.primeants.gametest.")&&!f.getClassName().equals(TestIdentities.class.getName())&&!f.getClassName().contains(".mixin."))
            .findFirst().map(f->f.getClassName()+"#"+f.getMethodName()).orElse("engine"));
    }
    public static UUID next(){return next("fixture:"+caller());}
    public static UUID next(String path){
        var s=current();if(s==null)return UUID.randomUUID();
        long n=s.counts.merge(path,1L,Long::sum)-1;
        UUID id=UUID.nameUUIDFromBytes((NAMESPACE+"|"+s.key+"|"+path+"|"+n).getBytes(StandardCharsets.UTF_8));
        if(!allocated.add(id))throw new IllegalStateException("Test identity reused: "+id);
        owners.put(id,s);
        var j=event(s,"allocation");j.addProperty("path",path);j.addProperty("ordinal",n);j.addProperty("id",id.toString());emit(j);
        return id;
    }
    public static void assigned(Entity e){var s=current();if(s!=null)owners.putIfAbsent(e.getUUID(),s);}
    public static void constructed(Entity e){
        var s=current();if(s==null)return;
        // Entity's original RNG call has already happened; only its fresh UUID is replaced.
        e.setUUID(next("entity:"+e.getType()+":"+caller()));owners.put(e.getUUID(),s);
    }
    public static UUID founding(UUID queen){
        var s=owners.get(queen);if(s==null)return UUID.randomUUID();
        active.get().push(s);try{return next("founding-brood:"+queen);}finally{leave();}
    }
    /** Pure prediction: unsuccessful funding/admission does not consume an identity. */
    public static UUID predictedBirth(UUID queen,long tick,int slot){
        var s=owners.get(queen);if(s==null||s.eventCompatibility)return null;
        var b=s.births.computeIfAbsent(queen,q->new Births());
        var previous=b.committed.get(tick+":"+slot);if(previous!=null)return previous;
        var id=UUID.nameUUIDFromBytes((NAMESPACE+"|"+s.key+"|laid-brood:"+queen+"|"+b.next).getBytes(StandardCharsets.UTF_8));
        b.ordinals.putIfAbsent(id,b.next);return id;
    }
    /** Observe a real successful production lay; never create brood or change its funding. */
    public static void committedBirth(UUID queen,long tick,int slot,UUID id){
        var s=owners.get(queen);if(s==null||s.eventCompatibility)return;
        var b=s.births.get(queen);
        if(b==null||!id.equals(predictedBirth(queen,tick,slot))||!Objects.equals(b.ordinals.get(id),b.next))
            throw new IllegalStateException("Unexpected successful test birth identity");
        long ordinal=b.next;
        if(b.committed.putIfAbsent(tick+":"+slot,id)!=null||!allocated.add(id))throw new IllegalStateException("Test birth identity reused: "+id);
        b.next=Math.incrementExact(b.next);
        owners.put(id,s);owners.put(dev.primeants.worker.ColonyMembers.workerId(id),s);
        var allocation=event(s,"allocation");allocation.addProperty("path","laid-brood:"+queen);allocation.addProperty("ordinal",ordinal);allocation.addProperty("id",id.toString());emit(allocation);
        var j=event(s,"birth_committed");j.addProperty("queen",queen.toString());j.addProperty("loadedTick",tick);j.addProperty("slot",slot);j.addProperty("birthOrdinal",ordinal);j.addProperty("id",id.toString());j.addProperty("workerId",dev.primeants.worker.ColonyMembers.workerId(id).toString());emit(j);
    }
    public static void laying(UUID queen,long tick,int slot,UUID id){
        var s=owners.get(queen);if(s==null||!s.selections.add("egg:"+id))return;
        // Event-clock/captured controls retain production IDs; ordinary physical fixtures use birth ordinals.
        owners.put(id,s);owners.put(dev.primeants.worker.ColonyMembers.workerId(id),s);
        var j=event(s,"laying_identity");j.addProperty("queen",queen.toString());j.addProperty("loadedTick",tick);j.addProperty("slot",slot);j.addProperty("id",id.toString());j.addProperty("rule",s.eventCompatibility?"production_event":"birth_ordinal");
        if(!s.eventCompatibility)j.addProperty("birthOrdinal",s.births.get(queen).ordinals.get(id));emit(j);
    }
    public static boolean traced(GameTestInfo t){
        String n=t.id().toString();return n.contains("large_colony_foragers_carry_at_once")||n.contains("upgrade_counts_every_clay")||n.contains("supplied_founding_to_mature")||n.contains("physical_oversized_inventories");
    }
    private static JsonObject event(Scope s,String type){
        var j=new JsonObject();j.addProperty("type",type);j.addProperty("case",s.test.id().toString());j.addProperty("namespace",s.key);
        j.addProperty("helperTick",s.test.getTick());j.addProperty("gameTime",s.test.getLevel().getGameTime());return j;
    }
    private static void emit(JsonObject j){PrimeAnts.LOGGER.info("T14 IDENTITY {}",j);}
    public static void lifecycle(GameTestInfo t,String type){
        var s=cases.get(t);if(s==null)return;var j=event(s,type);j.addProperty("error",String.valueOf(t.getError()));
        j.addProperty("origin",String.valueOf(t.getTestOrigin()));j.addProperty("rotation",t.getRotation().name());emit(j);
        if(!type.equals("begin"))snapshot(t,type);
    }
    public static void snapshot(GameTestInfo t,String why){
        if(!traced(t))return;var s=cases.get(t);if(s==null)return;
        var j=event(s,"snapshot");j.addProperty("why",why);var rows=new JsonArray();
        for(var a:t.getLevel().getEntitiesOfClass(LasiusNigerEntity.class,t.getStructureBounds().inflate(8))){
            var r=new JsonObject();r.addProperty("id",a.getUUID().toString());r.addProperty("queen",String.valueOf(a.queenId()));r.addProperty("brood",String.valueOf(a.broodId()));
            var v=a.position().subtract(net.minecraft.world.phys.Vec3.atLowerCornerOf(t.getTestOrigin()));
            r.add("position",new Gson().toJsonTree(new double[]{v.x,v.y,v.z}));r.addProperty("form",a.form().name());r.addProperty("alive",a.isAlive());r.addProperty("canonical",t.getLevel().getEntity(a.getUUID())==a);r.addProperty("phase",a.workerTasks().phase().name());r.addProperty("reason",a.workerTasks().reason());r.addProperty("hand",a.getMainHandItem().toString());
            r.addProperty("sugar",a.nutrition().sugar());r.addProperty("protein",a.nutrition().protein());r.addProperty("fasting",a.adultLife().fasting());r.addProperty("age",a.elapsedAgeTicks());
            if(a.form()==dev.primeants.entity.AntForm.QUEEN)r.add("nursery",nursery(t,a));
            rows.add(r);
        }
        j.add("entityQueryOrder",rows);emit(j);
    }
    /** Existing loaded objects and cached decisions only: no validation, funding, care or path call. */
    private static JsonObject nursery(GameTestInfo t,LasiusNigerEntity q){
        var j=new JsonObject();var home=q.founding().plan();if(home==null)return j;
        var l=t.getLevel();boolean loaded=l.hasChunkAt(home.nursery());j.addProperty("nurseryChunkLoaded",loaded);
        if(!loaded||!(l.getBlockEntity(home.nursery()) instanceof dev.primeants.brood.BroodPile p))return j;
        j.addProperty("loadedTick",p.loadedTicks());j.addProperty("condition",p.condition());j.addProperty("lastLayingTick",p.lastLayingTick());
        j.addProperty("cachedStageEvaluation",String.valueOf(p.stageEvaluation()));
        if(p.stageEvaluation()!=null)j.addProperty("cachedStageEvaluationAge",p.loadedTicks()-p.stageEvaluatedAt());
        j.addProperty("cachedGrowthReason",p.growth().reason());j.addProperty("recentSugar",p.growth().recentSugar());j.addProperty("recentProtein",p.growth().recentProtein());
        j.addProperty("growthReasonIsCached",true);
        var records=new JsonArray();for(var b:p.records()){
            var r=new JsonObject();r.addProperty("id",b.id().toString());r.addProperty("slot",b.slot());r.addProperty("stage",b.stage().name());r.addProperty("progress",b.progress());records.add(r);
        }j.add("records",records);
        if(l.hasChunkAt(home.cache())&&l.getBlockEntity(home.cache()) instanceof dev.primeants.worker.NestCache cache)
            j.add("physicalFoodNow",new Gson().toJsonTree(cache.contents().stream().map(Object::toString).toList()));
        return j;
    }
    public static void chosen(UUID queen,List<? extends Entity> input,Entity chosen){
        MinerCausalProbe.selection(queen,"actual_first_forager_chosen",String.valueOf(chosen==null?null:chosen.getUUID()));
        var s=owners.get(queen);if(s==null||!traced(s.test))return;
        selection(queen,"first_forager_min_input",input);
        var j=event(s,"selection_result");j.addProperty("queen",queen.toString());j.addProperty("site","first_forager_min");j.addProperty("chosen",chosen==null?null:chosen.getUUID().toString());emit(j);
    }
    public static void claims(dev.primeants.entity.LasiusNigerEntity queen){
        var s=owners.get(queen.getUUID());if(s==null||!traced(s.test))return;
        var ids=queen.founding().foragers();if(!s.selections.add("claims:"+queen.getUUID()+":"+ids))return;
        var j=event(s,"forager_claims");j.addProperty("queen",queen.getUUID().toString());j.add("claims",new Gson().toJsonTree(ids.stream().map(UUID::toString).toList()));emit(j);
    }
    /** Observes the real production query's returned list without sorting/filtering/re-querying it. */
    public static void selection(UUID queen,String site,List<? extends Entity> rows){
        MinerCausalProbe.selection(queen,"actual_"+site,rows.stream().map(e->e.getUUID()+":"+e.position()).toList());
        var s=owners.get(queen);if(s==null||!traced(s.test))return;
        var j=event(s,"selection_input");j.addProperty("queen",queen.toString());j.addProperty("site",site);var list=new JsonArray();
        var q=s.test.getLevel().getEntity(queen) instanceof LasiusNigerEntity a?a:null;
        for(var e:rows){var r=new JsonObject();r.addProperty("id",e.getUUID().toString());var v=e.position().subtract(net.minecraft.world.phys.Vec3.atLowerCornerOf(s.test.getTestOrigin()));r.add("position",new Gson().toJsonTree(new double[]{v.x,v.y,v.z}));
            if(q!=null&&q.founding().plan()!=null){var p=q.founding().plan();var d=e.position().subtract(net.minecraft.world.phys.Vec3.atBottomCenterOf(p.chamber()));var side=p.direction().getClockWise();r.addProperty("sideRowComparator",-Math.abs(d.x*side.getStepX()+d.z*side.getStepZ()));}
            list.add(r);}j.add("queryOrder",list);emit(j);
    }
}
