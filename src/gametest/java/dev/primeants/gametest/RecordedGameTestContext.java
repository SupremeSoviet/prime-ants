package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.PrimeAnts;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;

/** Initial native fixture inputs only. Never changes a running case, actor, item or worker decision. */
public final class RecordedGameTestContext {
    private static final Gson JSON=new Gson();
    private static boolean loaded;
    private static JsonObject context;
    private static List<String> order=List.of();
    private static final IdentityHashMap<GameTestInfo,BlockPos> prepared=new IdentityHashMap<>();
    private static final Set<String> preparedNames=new HashSet<>();
    private static final Set<GameTestInfo> started=Collections.newSetFromMap(new IdentityHashMap<>());
    private static final IdentityHashMap<GameTestInfo,Integer> waits=new IdentityHashMap<>();
    private static final IdentityHashMap<GameTestInfo,Boolean> readiness=new IdentityHashMap<>();
    private static int next;
    private RecordedGameTestContext(){}
    private static synchronized JsonObject context(){
        if(loaded)return context;
        loaded=true;String path=System.getProperty("prime_ants.caseManifest");if(path==null)return null;
        try{
            var manifest=JsonParser.parseString(Files.readString(Path.of(path),StandardCharsets.UTF_8)).getAsJsonObject();
            if(!manifest.has("recorded_context"))return null;
            context=manifest.getAsJsonObject("recorded_context");
            if(!context.get("namespace_version").getAsString().equals(TestIdentities.NAMESPACE))throw new IllegalStateException("Recorded identity namespace changed");
            var names=new ArrayList<String>();context.getAsJsonArray("case_order").forEach(v->names.add(v.getAsString()));
            if(names.isEmpty()||names.size()!=new HashSet<>(names).size()||!new HashSet<>(names).equals(context.getAsJsonObject("cases").keySet()))throw new IllegalStateException("Recorded context identities differ");
            order=List.copyOf(names);return context;
        }catch(java.io.IOException error){throw new IllegalStateException("Recorded GameTest context could not be read",error);}
    }
    public static <T> Stream<T> select(List<T> registered,List<String> assigned,Function<T,String> name){
        var nativeSelection=registered.stream().filter(v->assigned.contains(name.apply(v))).toList();
        if(context()==null)return nativeSelection.stream();
        if(!new HashSet<>(assigned).equals(new HashSet<>(order))||assigned.size()!=order.size())throw new IllegalStateException("Recorded order is not the exact assigned set");
        var byName=new HashMap<String,T>();nativeSelection.forEach(v->byName.put(name.apply(v),v));
        if(byName.size()!=order.size())throw new IllegalStateException("Recorded native selection incomplete");
        PrimeAnts.LOGGER.info("T21 RECORDED CONTEXT SELECTION {}",JSON.toJson(Map.of("case_order",order,"policy_sha256",context.get("policy_sha256").getAsString(),"namespace_version",TestIdentities.NAMESPACE)));
        return order.stream().map(byName::get);
    }
    private static JsonObject row(GameTestInfo info){
        var c=context();if(c==null||!c.getAsJsonObject("cases").has(info.id().toString()))return null;
        return c.getAsJsonObject("cases").getAsJsonObject(info.id().toString());
    }
    private static BlockPos origin(JsonObject r){var a=r.getAsJsonArray("origin");if(a.size()!=3)throw new IllegalStateException("Recorded origin must have three coordinates");return new BlockPos(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt());}
    public static BlockPos initialTestBlockPos(GameTestInfo info,BlockPos vanilla){
        if(info==null)return vanilla;var r=row(info);if(r==null)return vanilla;
        if(started.contains(info))throw new IllegalStateException("Recorded fixture position requested after scenario start");
        if(!r.get("rotation").getAsString().equals("NONE")||!info.getRotation().name().equals("NONE"))throw new IllegalStateException("Recorded fixture rotation differs");
        // Pinned vanilla getStartCorner(NONE) = testBlock + padding + STRUCTURE_OFFSET.
        // Read only the original metadata and the pure native offset arithmetic.
        int padding=info.getTest().padding();var offset=TestInstanceBlockEntity.getStructurePos(BlockPos.ZERO);var expected=origin(r);
        var testBlock=expected.offset(-padding-offset.getX(),-padding-offset.getY(),-padding-offset.getZ());
        if(!prepared.containsKey(info)&&!preparedNames.add(info.id().toString()))throw new IllegalStateException("Duplicate primary recorded fixture");
        prepared.put(info,testBlock);return testBlock;
    }
    public static void beforeOriginalBody(GameTestInfo info){
        if(!prepared.containsKey(info))return;var r=row(info);String id=info.id().toString();
        if(!started.add(info)||next>=order.size()||!order.get(next).equals(id))throw new IllegalStateException("Actual native case order differs from recorded context at index "+next+": "+id);
        var expected=origin(r);var actual=info.getTestOrigin();
        if(!actual.equals(expected)||!info.getRotation().name().equals(r.get("rotation").getAsString()))throw new IllegalStateException("Actual native origin/rotation differs from recorded fixture: "+id+" expected="+expected+" actual="+actual);
        var evidence=new LinkedHashMap<String,Object>();evidence.put("case",id);evidence.put("index",next++);evidence.put("gameTime",info.getLevel().getGameTime());evidence.put("seed",info.getLevel().getSeed());evidence.put("dimension",info.getLevel().dimension().identifier().toString());evidence.put("expected_origin",expected.toShortString());evidence.put("actual_origin",actual.toShortString());evidence.put("rotation",info.getRotation().name());evidence.put("namespace_version",TestIdentities.NAMESPACE);evidence.put("policy_sha256",context.get("policy_sha256").getAsString());
        PrimeAnts.LOGGER.info("T21 RECORDED CONTEXT START {}",JSON.toJson(evidence));
    }
    public static boolean observePending(GameTestInfo info){return prepared.containsKey(info)&&!started.contains(info)&&!info.isDone();}
    public static void actualPreparationReturn(GameTestInfo info,GameTestInfo result){
        if(!prepared.containsKey(info))return;
        var data=new LinkedHashMap<String,Object>();data.put("case",info.id().toString());data.put("gameTime",info.getLevel().getGameTime());data.put("actual_return_null",result==null);data.put("actual_return_same_info",result==info);data.put("initial_test_block",info.getTestBlockPos().toShortString());
        if(result==info)data.put("actual_prepared_origin",info.getTestOrigin().toShortString());
        PrimeAnts.LOGGER.info("T21 RECORDED CONTEXT PREPARATION RETURN {}",JSON.toJson(data));
    }
    public static void actualReadiness(GameTestInfo info,boolean result,List<Map<String,Object>> evaluated){
        if(!observePending(info))return;
        Boolean before=readiness.put(info,result);
        if(before==null||before!=result){
            var row=new LinkedHashMap<String,Object>();row.put("case",info.id().toString());row.put("helperTick",info.getTick());row.put("gameTime",info.getLevel().getGameTime());row.put("actual_allMatch_result",result);row.put("actually_evaluated_chunk_predicates",evaluated);
            PrimeAnts.LOGGER.info("T21 RECORDED CONTEXT READINESS {}",JSON.toJson(row));
        }
    }
    public static boolean originalTickAllowed(GameTestInfo info){
        if(!observePending(info)||info.getTick()< -1)return true;
        if(next<order.size()&&order.get(next).equals(info.id().toString()))return true;
        int count=waits.merge(info,1,Integer::sum);
        if(count>200)throw new IllegalStateException("Recorded pre-start order wait exceeded200ready calls: "+info.id()+" awaiting "+(next<order.size()?order.get(next):"none"));
        if(count==1||count==200)PrimeAnts.LOGGER.info("T21 RECORDED CONTEXT DISPATCH WAIT {}",JSON.toJson(Map.of("case",info.id().toString(),"helperTick",info.getTick(),"ready_dispatch_calls",count,"awaiting",next<order.size()?order.get(next):"none","scope","unstarted native info only; no running scenario paused")));
        return false;
    }
}
