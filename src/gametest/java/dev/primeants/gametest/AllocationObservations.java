package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.brood.*;
import dev.primeants.entity.*;
import dev.primeants.worker.*;
import java.util.*;
import net.minecraft.gametest.framework.GameTestHelper;

/** Test-only observations; no controller stamps, movement, receipts or allocation mutations. */
final class AllocationObservations {
    static JsonObject state(GameTestHelper c,LasiusNigerEntity q,BroodPile p,long cursor){
        var row=new JsonObject();row.addProperty("cursor",cursor);
        row.addProperty("completion",p.catchup().nextServiceCompletion());
        row.addProperty("next_wake_delay",p.catchup().nextServiceEvent(cursor));
        row.addProperty("saved_service_state",p.saveWithFullMetadata(c.getLevel().registryAccess()).getCompoundOrEmpty("Catchup").toString());
        var adults=AutomaticCatchupGameTest.adults(c,q);var people=new JsonArray();
        for(var a:adults){var r=new JsonObject();r.addProperty("id",a.getUUID().toString());r.addProperty("form",a.form().name());
            r.add("nutrition",new Gson().toJsonTree(a.nutrition().snapshot()));r.addProperty("fasting",a.adultLife().fasting());r.addProperty("hungry",a.adultLife().hungry());
            r.addProperty("crop_state",ReconciliationGameTest.save(c,a).getCompoundOrEmpty("WorkerTask").getCompoundOrEmpty("CropSharing").toString());
            r.addProperty("position",a.position().toString());r.addProperty("partial_action",a.workerTasks().sharing().actionTicks());r.addProperty("crop_target",String.valueOf(a.workerTasks().sharing().target()));
            r.addProperty("caregiver",a.form()==AntForm.WORKER&&a.workerTasks().caregiverWithValidatedHabitat(c.getLevel(),q.founding().plan()));
            r.addProperty("cargo",a.getMainHandItem().toString());r.addProperty("feeding_ticks",a.workerTasks().feedingTicks());
            r.addProperty("tick_count",a.tickCount);r.addProperty("phase",a.workerTasks().phase().name());
            r.addProperty("nursing",a.workerTasks().nursing());r.addProperty("meal_ticks",a.adultLife().mealTicks());
            r.addProperty("saved_worker_task",ReconciliationGameTest.save(c,a).getCompoundOrEmpty("WorkerTask").toString());
            r.add("adult_life",new Gson().toJsonTree(a.adultLife().snapshot()));
            r.add("simulation",ReconciliationEvidence.adult(a.simulationEvidence()));
            var choices=new JsonArray();adults.stream().filter(t->CropSharing.aggregateEligible(c.getLevel(),a,t))
                .sorted(Comparator.comparingLong((LasiusNigerEntity t)->-t.adultLife().fasting()).thenComparingDouble(a::distanceToSqr))
                .forEach(t->{var x=new JsonObject();x.addProperty("recipient",t.getUUID().toString());x.addProperty("fasting",t.adultLife().fasting());x.addProperty("donor_distance",a.distanceToSqr(t));choices.add(x);});
            r.add("eligible_crop_choices_fasting_then_distance",choices);people.add(r);
        }
        row.add("adults",people);row.add("adult_order",new Gson().toJsonTree(adults.stream().map(LasiusNigerEntity::getUUID).toList()));row.add("brood",new Gson().toJsonTree(p.records().stream().map(r->Map.of("id",r.id(),"stage",r.stage(),"progress",r.progress(),"nutrition",r.nutrition().snapshot())).toList()));
        var cache=c.getLevel().getBlockEntity(q.founding().plan().cache());row.add("cache",new Gson().toJsonTree(cache instanceof NestCache n?n.contents().stream().map(Object::toString).toList():List.of()));return row;
    }
    static void log(GameTestHelper c,LasiusNigerEntity q,BroodPile p,long cursor,String point){var row=state(c,q,p,cursor);row.addProperty("point",point);ReconciliationEvidence.log("t08_allocation","ordinary_production_hooks",row);}
}
