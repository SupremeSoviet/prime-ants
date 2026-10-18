package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.PrimeAnts;
import dev.primeants.brood.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.mixin.GameTestHelperAccessor;
import dev.primeants.worker.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.phys.AABB;

/** Scoped passive observation of the original finite-food assertion, never its replacement. */
public final class RoleFoodCausalProbe {
    private static final String TARGET="prime_ants_test:role_conflict_game_test_disabled_nursing_labels_cannot_reserve_care_with_four_other_mature_workers";
    private static final Gson JSON=new Gson();
    private static final Map<GameTestHelper,Scope> scopes=new IdentityHashMap<>();
    private static Scope current;
    private static final class Scope {
        final GameTestHelper c; final Set<UUID> sources=new HashSet<>();
        LasiusNigerEntity q; boolean dumped;
        Scope(GameTestHelper helper){c=helper;}
    }
    private RoleFoodCausalProbe(){}
    private static Scope scope(GameTestHelper c){
        if(!((GameTestHelperAccessor)c).primeAntsTestInfo().id().toString().equals(TARGET))return null;
        var old=scopes.get(c);if(old!=null)return old;
        var s=new Scope(c);scopes.put(c,s);
        ((GameTestHelperAccessor)c).primeAntsTestInfo().addListener(new GameTestListener(){
            public void testStructureLoaded(GameTestInfo i){}
            public void testPassed(GameTestInfo i,GameTestRunner r){close(s,"passed");}
            public void testFailed(GameTestInfo i,GameTestRunner r){close(s,"failed");}
            public void testAddedForRerun(GameTestInfo i,GameTestInfo copy,GameTestRunner r){close(s,"rerun");}
        });emit(s,"attach",Map.of("bounds",c.getBounds().toString(),"rotation",c.getTestRotation().name(),"seed",c.getLevel().getSeed()));return s;
    }
    private static void close(Scope s,String result){emit(s,"endpoint_after_listener_not_acceptance",result);scopes.remove(s.c);if(current==s)current=null;}
    private static void emit(Scope s,String event,Object value){
        var m=new LinkedHashMap<String,Object>();m.put("event",event);m.put("helperTick",s.c.getTick());m.put("gameTime",s.c.getLevel().getGameTime());m.put("queen",s.q==null?null:s.q.getUUID());m.put("testDone",((GameTestHelperAccessor)s.c).primeAntsTestInfo().isDone());m.put("value",value);PrimeAnts.LOGGER.info("T21 ROLE FOOD {}",JSON.toJson(m));
    }
    private static Scope worker(LasiusNigerEntity w){for(var s:scopes.values())if(s.q!=null&&w.level()==s.c.getLevel()&&s.q.getUUID().equals(w.queenId()))return s;return null;}
    private static Map<String,Object> nutrition(Nutrition n){var m=new LinkedHashMap<String,Object>();m.put("units",n.consumedUnits());m.put("apples",n.apples());m.put("chickens",n.chickens());m.put("berries",n.berries());m.put("nectar",n.nectar());m.put("nectarV2_subset",n.nectarV2());m.put("prey",n.prey());m.put("flesh",n.flesh());m.put("sugar",n.sugar());m.put("protein",n.protein());return m;}
    private static Map<String,Object> actor(Scope s,LasiusNigerEntity w){var m=new LinkedHashMap<String,Object>();m.put("id",w.getUUID());m.put("canonical",s.c.getLevel().getEntity(w.getUUID())==w);m.put("alive",w.isAlive());m.put("removed",w.isRemoved());m.put("NoAi",w.isNoAi());m.put("callow",w.isCallow());m.put("position",w.position().toString());m.put("box",w.getBoundingBox().toString());m.put("inside_original_query_calculated",s.c.getBounds().inflate(8).intersects(w.getBoundingBox()));m.put("hand",w.getMainHandItem().toString());m.put("nutrition",nutrition(w.nutrition()));var t=w.workerTasks();m.put("phase",t.phase());m.put("reason",t.reason());m.put("source",t.sourceId());m.put("recipient",t.recipientId());m.put("flower",String.valueOf(t.flowerSource()));return m;}
    private static Map<String,Object> item(ItemEntity i){return Map.of("id",i.getUUID(),"item",i.getItem().toString(),"position",i.position().toString(),"box",i.getBoundingBox().toString(),"alive",i.isAlive(),"removed",i.isRemoved(),"delay",i.hasPickUpDelay(),"canonical",i.level() instanceof ServerLevel l&&l.getEntity(i.getUUID())==i,"chunkLoaded_snapshot",i.level() instanceof ServerLevel l&&l.hasChunkAt(i.blockPosition()),"entityTicking_snapshot",i.level() instanceof ServerLevel l&&l.isPositionEntityTicking(i.blockPosition()));}
    public static void drop(GameTestHelper c,ItemEntity i){var s=scope(c);if(s!=null){s.sources.add(i.getUUID());emit(s,"actual_original_fixture_drop_return",item(i));}}
    public static void enter(GameTestHelper c,LasiusNigerEntity q){current=scope(c);if(current!=null)current.q=q;}
    public static void query(AABB box,List<?> result){if(current!=null)emit(current,"actual_total_ground_query_return",Map.of("bounds",box.toString(),"items",result.stream().filter(x->x instanceof ItemEntity).map(x->item((ItemEntity)x)).toList()));}
    public static void total(NursingGameTest original,GameTestHelper c,LasiusNigerEntity q,long value){
        var s=current;if(s==null)return;
        try{
            var snapshot=original.snapshot(c,q);var parts=new LinkedHashMap<String,Object>();parts.put("actualOriginalReturn",value);parts.put("independentSnapshotTotal",snapshot.total());parts.put("ground",snapshot.ground());parts.put("carried",snapshot.carried());parts.put("cache",snapshot.cache());parts.put("custody",snapshot.custody());parts.put("queen",snapshot.queen());parts.put("brood",snapshot.brood());parts.put("liveAdults",snapshot.adults());parts.put("adultHistory",snapshot.history());emit(s,"original_total_return_before_assertion",parts);
            if(c.getTick()%100==0||value!=18){
                var m=new LinkedHashMap<String,Object>();m.put("snapshot",snapshot);m.put("queen",actor(s,q));var workers=new ArrayList<Object>();
                for(var entity:c.getLevel().getAllEntities())if(entity instanceof LasiusNigerEntity w&&q.getUUID().equals(w.queenId()))workers.add(actor(s,w));m.put("canonical_owner_bodies_all_level_not_bounded_query",workers);
                m.put("canonicalMembership",ColonyMembers.get(c.getLevel()).members(q.getUUID()).toString());m.put("foragerClaims",q.founding().foragers());emit(s,"independent_passive_state_not_decisions",m);
            }
            if(value!=18&&!s.dumped){s.dumped=true;dump(s);}
        }finally{current=null;}
    }
    private static void dump(Scope s){
        var c=s.c;var l=c.getLevel();var q=s.q;var m=new LinkedHashMap<String,Object>();var p=q.founding().plan();
        if(p!=null){if(l.getBlockEntity(p.nursery()) instanceof BroodPile pile)m.put("actualNurseryNBT",pile.saveWithFullMetadata(l.registryAccess()).toString());if(l.getBlockEntity(p.cache()) instanceof NestCache cache)m.put("actualCacheNBT",cache.saveWithFullMetadata(l.registryAccess()).toString());}
        var adults=new LinkedHashMap<String,String>();AdultHistory.get(l).records().forEach((id,v)->{var row=JsonParser.parseString(v).getAsJsonObject();if(row.has("queen")&&row.get("queen").getAsString().equals(q.getUUID().toString()))adults.put(id,v);});m.put("actualAdultHistory",adults);
        var brood=new LinkedHashMap<String,String>();BroodHistory.get(l).records().forEach((id,v)->{var row=JsonParser.parseString(v).getAsJsonObject();if(row.has("queen")&&row.get("queen").getAsString().equals(q.getUUID().toString()))brood.put(id,v);});m.put("actualBroodHistory",brood);
        m.put("actualCustody",TransferCustody.get(l).contents().stream().filter(v->c.getBounds().inflate(8).contains(v.position())).map(Object::toString).toList());m.put("originalSourceIds",s.sources);
        var sourceStates=new LinkedHashMap<String,Object>();for(var id:s.sources){var e=l.getEntity(id);sourceStates.put(id.toString(),e instanceof ItemEntity i?item(i):String.valueOf(e));}m.put("originalSourceEntityLookup_null_is_not_proof_of_loss",sourceStates);emit(s,"first_divergence_serialized_state_before_original_assertion",m);
    }
    public static void hand(LasiusNigerEntity w,ItemStack intended,String when){var s=worker(w);if(s!=null)emit(s,"actual_hand_"+when,Map.of("worker",actor(s,w),"intended",intended.toString()));}
    public static void cache(LasiusNigerEntity w,String method,Object result){var s=worker(w);if(s==null)return;var m=new LinkedHashMap<String,Object>();m.put("worker",actor(s,w));m.put("result",result);var p=s.q.founding().plan();if(p!=null&&s.c.getLevel().getBlockEntity(p.cache()) instanceof NestCache cache)m.put("contents",cache.contents().toString());emit(s,"actual_cache_"+method,m);}
    public static void table(SavedDataStorage storage,SavedDataType<?> type,SavedData data,String when){for(var s:scopes.values())if(s.c.getLevel().getDataStorage()==storage)emit(s,"actual_shared_table_set_"+when,Map.of("type",type.toString(),"class",data.getClass().getName(),"objectIdentity",System.identityHashCode(data)));}
    public static void itemChange(ItemEntity i,String when,Object value){for(var s:scopes.values())if(i.level()==s.c.getLevel()&&(s.sources.contains(i.getUUID())||s.c.getBounds().inflate(8).intersects(i.getBoundingBox())&&WorkerTasks.food(i.getItem())))emit(s,"actual_item_"+when,Map.of("state",item(i),"value",String.valueOf(value)));}
}
