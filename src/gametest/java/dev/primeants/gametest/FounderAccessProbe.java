package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.PrimeAnts;
import dev.primeants.brood.*;
import dev.primeants.colony.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import dev.primeants.gametest.mixin.GameTestHelperAccessor;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;

/** Scoped read-only observations. No access checks, reconciliation, stage evaluation or confirmation calls. */
public final class FounderAccessProbe implements AutoCloseable {
    private static final Map<UUID,FounderAccessProbe> scopes=new HashMap<>();
    private final GameTestHelper test; private final LasiusNigerEntity queen; private final int bound;
    private Boolean priority,unlocked; private ChamberUpgrade.PrioritySelection available;
    private boolean deciding; private long decisionAt=-1;
    private JsonObject firstStop;
    private java.util.function.Consumer<SurfaceAccess> admission;
    private FounderAccessProbe(GameTestHelper c,LasiusNigerEntity q,int bound){test=c;queen=q;this.bound=bound;scopes.put(q.getUUID(),this);}
    public static FounderAccessProbe watch(GameTestHelper c,LasiusNigerEntity q,int bound){
        var p=new FounderAccessProbe(c,q,bound);
        c.onEachTick(()->{if(c.getTick()==bound-1)p.emit("final_pre_deadline",p.snapshot(null,null));});
        ((GameTestHelperAccessor)c).primeAntsTestInfo().addListener(new GameTestListener(){
            public void testStructureLoaded(GameTestInfo i){}
            public void testPassed(GameTestInfo i,GameTestRunner r){p.emit("passed_listener",p.snapshot(null,null));p.close();}
            public void testFailed(GameTestInfo i,GameTestRunner r){p.emit("failure_listener_no_extra_construction",p.snapshot(null,null));p.close();}
            public void testAddedForRerun(GameTestInfo i,GameTestInfo copy,GameTestRunner r){p.close();}
        });return p;
    }
    public JsonObject firstStop(){return firstStop==null?null:firstStop.deepCopy();}
    public void onAdmission(java.util.function.Consumer<SurfaceAccess> assertion){admission=assertion;}
    public static void admitted(ServerLevel l,UUID owner,SurfaceAccess access){
        var p=scopes.get(owner);if(p==null||p.test.getLevel()!=l||access==null)return;
        if(p.admission!=null)p.admission.accept(access);
        p.emit("actual_admission_after_ordering_assertion",p.snapshot(null,null));
    }
    public static void begin(UUID owner){var p=scopes.get(owner);if(p!=null){p.deciding=true;p.decisionAt=p.test.getLevel().getGameTime();p.priority=null;p.unlocked=null;p.available=null;}}
    public static void priority(UUID owner,boolean value){var p=scopes.get(owner);if(p!=null)p.priority=value;}
    public static void unlocked(UUID owner,boolean value){var p=scopes.get(owner);if(p!=null){p.unlocked=value;p.deciding=false;}}
    public static void available(UUID owner,ChamberUpgrade.PrioritySelection value){var p=scopes.get(owner);if(p!=null&&p.deciding)p.available=value;}
    public static void stopping(ServerLevel l,LasiusNigerEntity w,SurfaceWork.Job j,String reason){
        var p=scopes.get(w.queenId());if(p==null||p.test.getLevel()!=l||p.firstStop!=null||j.access.stopped)return;
        p.firstStop=p.snapshot(w,reason);p.emit("first_stop_decision",p.firstStop);
    }
    private void emit(String type,JsonObject value){var o=new JsonObject();o.addProperty("type",type);o.addProperty("helperTick",test.getTick());o.addProperty("bound",bound);o.addProperty("owner",queen.getUUID().toString());o.add("value",value);PrimeAnts.LOGGER.info("T05 ACCESS OBS {}",o);}
    private JsonObject snapshot(LasiusNigerEntity actor,String stopReason){
        var l=test.getLevel();var o=new JsonObject();o.addProperty("gameTime",l.getGameTime());
        var home=queen.founding().plan();if(home==null)return o;
        o.addProperty("entrance",home.entrance().asLong());o.addProperty("direction",home.direction().getName());
        var pile=l.getBlockEntity(home.nursery()) instanceof BroodPile p?p:null;
        o.addProperty("nurseryLoaded",pile==null?-1:pile.loadedTicks());o.addProperty("stageEvaluatedAt",pile==null?-1:pile.stageEvaluatedAt());
        o.addProperty("stageEvaluation",pile==null?null:String.valueOf(pile.stageEvaluation()));
        o.addProperty("actualPriorityReturn",priority);o.addProperty("actualUnlockedReturn",unlocked);
        o.addProperty("lastActualDecisionGameTime",decisionAt);o.addProperty("decisionIsCurrent",decisionAt==l.getGameTime());
        var widening=NestExpansion.get(l).job(queen.getUUID());o.addProperty("wideningPriority",widening!=null&&!widening.complete());
        var digs=new JsonArray();for(var j:ChamberExcavation.get(l).jobs(queen.getUUID()))if(!j.complete()){
            var r=new JsonObject();r.addProperty("room",j.room);r.addProperty("removed",j.removed());r.addProperty("total",j.tasks.size());r.addProperty("complete",j.complete());r.addProperty("stopped",j.stopped());r.addProperty("claim",String.valueOf(j.claim));r.addProperty("reason",j.reason);digs.add(r);
        }o.add("unfinishedExcavation",digs);
        var upgrades=new JsonArray();for(var j:ChamberUpgrade.get(l).jobs(queen.getUUID())){
            var r=new JsonObject();r.addProperty("chamber",j.chamber);r.addProperty("tier",j.tier);r.addProperty("built",j.built().size());r.addProperty("total",j.cells.size());r.addProperty("complete",j.complete());r.addProperty("stopped",j.stopped());r.addProperty("claim",String.valueOf(j.claim));r.addProperty("reason",j.reason);r.addProperty("priority",!j.complete()&&!j.stopped()&&(j.tier==2||j.claim!=null));upgrades.add(r);
        }o.add("existingUpgrades",upgrades);
        var a=new JsonObject();a.addProperty("evaluatedByRealPriorityCall",available!=null);
        if(available!=null){a.addProperty("applies",available.applies());a.addProperty("confirmedStore",available.confirmedStore());a.addProperty("recipeUnits",available.recipeUnits());var s=available.selection();if(s!=null){a.addProperty("chamber",s.state().id());a.addProperty("tier",s.tier());a.addProperty("chamberTier",s.state().tier());a.addProperty("confirmedFunctions",s.state().functions().toString());}}
        o.add("availableUpgrade",a);var mining=Mining.get(l).job(queen.getUUID());o.addProperty("miningPriority",mining!=null&&!mining.complete()&&!mining.stopped());
        var j=SurfaceWork.get(l).job(queen.getUUID(),ColonyStage.MATURE);if(j==null)return o;
        o.addProperty("surfaceReason",j.reason);o.addProperty("completed",j.completed());o.addProperty("recovered",j.recovered());o.addProperty("placed",j.placed());o.addProperty("released",j.released());o.addProperty("claim",String.valueOf(j.claim));o.add("receipts",new Gson().toJsonTree(j.receipts()));
        long physical=0;for(int n=0;n<j.plan.cost();n++)if(ColonyTerrain.get(l).surface(l,j.at(n),queen.getUUID())&&l.getBlockState(j.at(n)).is(j.plan.cells().get(n).material().equals("gate")?NurseryBlocks.MOUND_GATE:NurseryBlocks.NEST_SOIL))physical++;
        o.addProperty("physicalPaidMatureCells",physical);
        if(j.access!=null){o.add("access",j.access.encode());o.addProperty("alreadyStopped",j.access.stopped);o.addProperty("priorReason",j.access.reason);o.addProperty("stopReason",stopReason);o.addProperty("motivatingTarget",j.at(4).asLong());
            var reservations=new JsonArray();for(var op:j.access.operations){var r=new JsonObject();r.addProperty("unit",op.unit.toString());r.addProperty("source",op.source.asLong());r.addProperty("destination",op.destination.asLong());r.addProperty("reserved",ColonyTerrain.get(l).accessReservation(l,op.destination,queen.getUUID(),op.unit));r.addProperty("sourceOwned",op.removed?ColonyTerrain.get(l).accessOpen(l,op.source,queen.getUUID(),op.unit):ColonyTerrain.get(l).mound(l,op.source,queen.getUUID()));r.addProperty("depositedOwned",ColonyTerrain.get(l).accessMound(l,op.destination,queen.getUUID(),op.unit));reservations.add(r);}o.add("reservations",reservations);
        }
        if(actor==null&&j.claim!=null&&l.getEntity(j.claim) instanceof LasiusNigerEntity w)actor=w;
        if(actor!=null){o.addProperty("actor",actor.getUUID().toString());o.addProperty("actorPhase",actor.workerTasks().phase().name());o.addProperty("actorCargo",actor.getMainHandItem().toString());o.addProperty("actorPosition",actor.position().toString());o.addProperty("actorAction",actor.workerTasks().harvestingTicks());}
        var custody=new JsonArray();for(var p:TransferCustody.get(l).contents())if(j.access!=null&&j.access.operations.stream().anyMatch(op->ItemStack.matches(p.stack(),SurfaceAccess.unitStack(op.unit))))custody.add(p.toString());o.add("pendingCustody",custody);
        var ground=new JsonArray();for(var item:l.getEntitiesOfClass(ItemEntity.class,test.getBounds().inflate(8),i->i.getItem().is(Items.DIRT))){var r=new JsonObject();r.addProperty("entity",item.getUUID().toString());r.addProperty("stack",item.getItem().toString());r.addProperty("position",item.position().toString());ground.add(r);}o.add("groundDirt",ground);return o;
    }
    public void close(){scopes.remove(queen.getUUID(),this);}
}
