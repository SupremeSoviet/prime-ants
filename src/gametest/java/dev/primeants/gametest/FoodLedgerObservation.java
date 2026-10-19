package dev.primeants.gametest;

import com.google.gson.Gson;
import dev.primeants.PrimeAnts;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.mixin.GameTestHelperAccessor;
import dev.primeants.worker.TransferCustody;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

/** Records the operands actually used by the fixture's conservation check. No controller calls. */
final class FoodLedgerObservation {
    private static final Gson JSON=new Gson();
    private static final Map<GameTestHelper,Scope> scopes=new IdentityHashMap<>();
    private static final class Scope {
        boolean armed,predicate; long apples,chickens,expected; Actual actual;
    }
    private record Actual(long ground,long carried,long custody,long cache,long consumed,long total,AABB box,
            List<ItemEntity> groundCandidates,List<LasiusNigerEntity> workerCandidates,List<TransferCustody.Pending> custodyCandidates) {}
    private FoodLedgerObservation() {}
    static void begin(GameTestHelper c,long apples,long chickens) {
        var info=((GameTestHelperAccessor)c).primeAntsTestInfo();
        if(info.isDone())return;
        var scope=scopes.get(c);
        if(scope==null){
            scope=new Scope();scopes.put(c,scope);
            info.addListener(new GameTestListener(){
                public void testStructureLoaded(GameTestInfo i){}
                public void testPassed(GameTestInfo i,GameTestRunner r){scopes.remove(c);}
                public void testFailed(GameTestInfo i,GameTestRunner r){scopes.remove(c);}
                public void testAddedForRerun(GameTestInfo i,GameTestInfo copy,GameTestRunner r){scopes.remove(c);}
            });
        }
        scope.apples=apples;scope.chickens=chickens;scope.actual=null;scope.armed=true;
    }
    static void capture(GameTestHelper c,long ground,long carried,long custody,long cache,long consumed,long total,AABB box,
            List<ItemEntity> groundCandidates,List<LasiusNigerEntity> workerCandidates,List<TransferCustody.Pending> custodyCandidates) {
        var scope=scopes.get(c);if(scope==null||!scope.armed)return;
        scope.actual=new Actual(ground,carried,custody,cache,consumed,total,box,List.copyOf(groundCandidates),List.copyOf(workerCandidates),List.copyOf(custodyCandidates));
    }
    static void finished(GameTestHelper c,LasiusNigerEntity q,long usedTotal,long expected,boolean passed,NursingGameTest audit) {
        var scope=scopes.get(c);if(scope==null)return;scope.armed=false;
        scope.expected=expected;scope.predicate=passed;
        if(passed&&c.getTick()%1000!=0)return;
        try { emit(c,q,usedTotal,passed,audit,scope); }
        catch(RuntimeException failure) {
            PrimeAnts.LOGGER.info("Food accounting evidence {}",JSON.toJson(Map.of("event","observation_incomplete","helperTick",c.getTick(),"usedTotal",usedTotal,"actualPredicateUsed",passed,"diagnosticFailure",failure.toString())));
        }
    }
    private static void emit(GameTestHelper c,LasiusNigerEntity q,long usedTotal,boolean passed,NursingGameTest audit,Scope scope) {
        var row=new LinkedHashMap<String,Object>();
        row.put("event","actual_food_conservation_check");row.put("case",((GameTestHelperAccessor)c).primeAntsTestInfo().id().toString());
        row.put("helperTick",c.getTick());row.put("gameTime",c.getLevel().getGameTime());row.put("queen",q.getUUID());
        row.put("suppliedApples",scope.apples);row.put("suppliedChickens",scope.chickens);row.put("usedTotal",usedTotal);row.put("actualExpectedTotalUsed",scope.expected);row.put("actualPredicateUsed",passed);
        var actual=scope.actual;
        row.put("captureMatchesUsedTotal",actual!=null&&actual.total()==usedTotal);
        if(actual!=null){
            row.put("actualOperands",Map.of("ground",actual.ground(),"carried",actual.carried(),"custody",actual.custody(),"cache",actual.cache(),"consumed",actual.consumed(),"total",actual.total()));
            row.put("actualQueryBox",actual.box().toString());
            row.put("actualReturnedGroundCandidates",actual.groundCandidates().stream().map(i->Map.of("id",i.getUUID(),"cargo",i.getItem().toString(),"position",i.position().toString())).toList());
            row.put("actualReturnedWorkerCandidates",actual.workerCandidates().stream().map(w->Map.of("id",w.getUUID(),"queen",String.valueOf(w.queenId()),"home",String.valueOf(w.nurseryHome()),"cargo",w.getMainHandItem().toString(),"position",w.position().toString(),"phase",w.workerTasks().phase().toString(),"reason",w.workerTasks().reason())).toList());
            row.put("actualAcceptedCustodyCandidates",actual.custodyCandidates().stream().map(p->Map.of("id",p.id(),"source",p.source(),"cargo",p.stack().toString(),"position",p.position().toString())).toList());
        }
        PrimeAnts.LOGGER.info("Food accounting evidence {}",JSON.toJson(row));
    }
    static void messageValue(GameTestHelper c,LasiusNigerEntity q,long messageTotal,long messageExpected,NursingGameTest audit) {
        var scope=scopes.get(c);if(scope==null)return;
        if(scope.predicate&&c.getTick()%1000!=0&&scope.actual!=null&&scope.actual.total()==messageTotal)return;
        try {
            var row=new LinkedHashMap<String,Object>();row.put("event","actual_food_message_return");
            row.put("case",((GameTestHelperAccessor)c).primeAntsTestInfo().id().toString());row.put("helperTick",c.getTick());row.put("gameTime",c.getLevel().getGameTime());row.put("queen",q.getUUID());
            row.put("messageTotal",messageTotal);row.put("messageExpected",messageExpected);row.put("comparisonTotal",scope.actual==null?"UNKNOWN":scope.actual.total());row.put("comparisonExpected",scope.expected);row.put("comparisonPredicateUsed",scope.predicate);
            if(scope.actual!=null)row.put("actualComparisonOperands",Map.of("ground",scope.actual.ground(),"carried",scope.actual.carried(),"custody",scope.actual.custody(),"cache",scope.actual.cache(),"consumed",scope.actual.consumed(),"total",scope.actual.total()));
            if(!scope.predicate||scope.actual!=null&&scope.actual.total()!=messageTotal)row.put("independentSnapshotAfterBothOriginalReturns",audit.snapshot(c,q));
            PrimeAnts.LOGGER.info("Food accounting evidence {}",JSON.toJson(row));
        } catch(RuntimeException failure) {
            PrimeAnts.LOGGER.info("Food accounting evidence {}",JSON.toJson(Map.of("event","observation_incomplete","purpose","message","helperTick",c.getTick(),"diagnosticFailure",failure.toString())));
        }
    }
}
