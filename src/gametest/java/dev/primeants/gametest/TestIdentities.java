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
    private static final Deque<Scope> active=new ArrayDeque<>();
    private static final Set<UUID> allocated=new HashSet<>();
    private static final class Scope {
        final GameTestInfo test; final String key; final Map<String,Long> counts=new HashMap<>();
        final Set<String> selections=new HashSet<>();
        Scope(GameTestInfo t,String k){test=t;key=k;}
    }
    private TestIdentities(){}
    public static void enter(GameTestInfo t){
        var s=cases.computeIfAbsent(t,x->new Scope(t,t.id()+":replica:"+(replicas.merge(t.id().toString(),1,Integer::sum)-1)));
        active.push(s);
    }
    public static void enter(Entity e){
        Scope s=owners.get(e.getUUID());
        if(s==null&&e instanceof LasiusNigerEntity a)s=owners.get(a.queenId());
        // ArrayDeque cannot hold null; a marker balances unowned vanilla ticks.
        active.push(s==null?UNSCOPED:s);
    }
    private static final Scope UNSCOPED=new Scope(null,"unscoped");
    public static void leave(){active.pop();}
    private static Scope current(){return active.isEmpty()||active.peek()==UNSCOPED?null:active.peek();}
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
    public static void constructed(Entity e){
        var s=current();if(s==null)return;
        // Entity's original RNG call has already happened; only its fresh UUID is replaced.
        e.setUUID(next("entity:"+e.getType()+":"+caller()));owners.put(e.getUUID(),s);
    }
    public static UUID founding(UUID queen){
        var s=owners.get(queen);if(s==null)return UUID.randomUUID();
        active.push(s);try{return next("founding-brood:"+queen);}finally{leave();}
    }
    public static void laying(UUID queen,long tick,int slot,UUID id){
        var s=owners.get(queen);if(s==null||!s.selections.add("egg:"+id))return;
        // Pure production event identity is intentionally retained, including captured conflict controls.
        owners.put(id,s);owners.put(dev.primeants.worker.ColonyMembers.workerId(id),s);
        var j=event(s,"laying_identity");j.addProperty("queen",queen.toString());j.addProperty("loadedTick",tick);j.addProperty("slot",slot);j.addProperty("id",id.toString());emit(j);
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
        snapshot(t,type);
    }
    public static void snapshot(GameTestInfo t,String why){
        if(!traced(t))return;var s=cases.get(t);if(s==null)return;
        var j=event(s,"snapshot");j.addProperty("why",why);var rows=new JsonArray();
        for(var a:t.getLevel().getEntitiesOfClass(LasiusNigerEntity.class,t.getStructureBounds().inflate(8))){
            owners.put(a.getUUID(),s);var r=new JsonObject();r.addProperty("id",a.getUUID().toString());r.addProperty("queen",String.valueOf(a.queenId()));r.addProperty("brood",String.valueOf(a.broodId()));
            var v=a.position().subtract(net.minecraft.world.phys.Vec3.atLowerCornerOf(t.getTestOrigin()));
            r.add("position",new Gson().toJsonTree(new double[]{v.x,v.y,v.z}));r.addProperty("form",a.form().name());r.addProperty("alive",a.isAlive());r.addProperty("canonical",t.getLevel().getEntity(a.getUUID())==a);r.addProperty("phase",a.workerTasks().phase().name());r.addProperty("reason",a.workerTasks().reason());r.addProperty("hand",a.getMainHandItem().toString());
            rows.add(r);
        }
        j.add("entityQueryOrder",rows);emit(j);
    }
    /** Observes the real production query's returned list without sorting/filtering/re-querying it. */
    public static void selection(UUID queen,String site,List<? extends Entity> rows){
        var s=owners.get(queen);if(s==null||!traced(s.test)||!s.selections.add(site+":"+rows.stream().map(Entity::getUUID).toList()))return;
        var j=event(s,"selection_input");j.addProperty("queen",queen.toString());j.addProperty("site",site);var list=new JsonArray();
        var q=s.test.getLevel().getEntity(queen) instanceof LasiusNigerEntity a?a:null;
        for(var e:rows){var r=new JsonObject();r.addProperty("id",e.getUUID().toString());var v=e.position().subtract(net.minecraft.world.phys.Vec3.atLowerCornerOf(s.test.getTestOrigin()));r.add("position",new Gson().toJsonTree(new double[]{v.x,v.y,v.z}));
            if(q!=null&&q.founding().plan()!=null){var p=q.founding().plan();var d=e.position().subtract(net.minecraft.world.phys.Vec3.atBottomCenterOf(p.chamber()));var side=p.direction().getClockWise();r.addProperty("sideRowComparator",-Math.abs(d.x*side.getStepX()+d.z*side.getStepZ()));}
            list.add(r);}j.add("queryOrder",list);emit(j);
    }
}
