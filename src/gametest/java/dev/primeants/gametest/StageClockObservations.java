package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.brood.BroodPile;
import dev.primeants.colony.ChamberRegistry;
import java.util.*;
import net.minecraft.server.level.ServerLevel;

/** Read-only scoped clock transitions, including food completion's actual virtual time. */
public final class StageClockObservations {
    private static final Set<UUID> WATCH=new HashSet<>();
    public static void start(UUID queen){WATCH.add(queen);}
    public static void stop(UUID queen){WATCH.remove(queen);}
    public static void sample(ServerLevel l,BroodPile p,long eventTime,String point){
        if(!WATCH.contains(p.queenId()))return;
        var row=new JsonObject();row.addProperty("queen",p.queenId().toString());row.addProperty("point",point);
        row.addProperty("event_game_time",eventTime);row.addProperty("level_game_time",l.getGameTime());row.addProperty("observation_end",Math.max(eventTime,l.getGameTime()));
        row.addProperty("loaded_ticks",p.loadedTicks());row.addProperty("loaded_ticks_minus_one",p.loadedTicks()-1);
        row.addProperty("stage_evaluated_at",p.stageEvaluatedAt());
        row.addProperty("pending_ticks",p.catchup().pendingTicks());row.addProperty("unknown_ticks",p.catchup().unknownTicks());row.addProperty("applied_ticks",p.catchup().appliedTicks());
        row.addProperty("status",p.catchup().status());ReconciliationEvidence.number(row,"last_simulation_time",p.lastSimulationGameTime());
        var colony=ChamberRegistry.get(l).colony(p.queenId());var clocks=colony==null?Map.<String,Long>of():colony.unmetSince();
        row.add("unmet_since",new Gson().toJsonTree(clocks));
        Long since=clocks.get("mature:food");row.add("since",since==null?JsonNull.INSTANCE:new JsonPrimitive(since));
        if(since!=null)row.addProperty("signed_discrepancy",since-(p.loadedTicks()-1));
        row.addProperty("brood_food_receipts",p.consumedFood());
        row.add("stage_inputs",new Gson().toJsonTree(p.stageEvaluation()==null?null:p.stageEvaluation().inputs()));
        row.addProperty("services",p.catchup().serviceObservations().toString());
        var adults=new JsonArray();for(var a:ColonyMembersView.adults(l,p)){var n=new JsonObject();n.addProperty("id",a.getUUID().toString());n.addProperty("cargo",a.getMainHandItem().toString());n.add("nutrition",new Gson().toJsonTree(a.nutrition().snapshot()));adults.add(n);}row.add("adults",adults);
        ReconciliationEvidence.log("t01_stage_clock","read_only_production_events",row);
    }
    private static final class ColonyMembersView {
        static List<dev.primeants.entity.LasiusNigerEntity> adults(ServerLevel l,BroodPile p){
            var out=new ArrayList<dev.primeants.entity.LasiusNigerEntity>();if(l.getEntity(p.queenId()) instanceof dev.primeants.entity.LasiusNigerEntity q)out.add(q);
            for(var m:dev.primeants.worker.ColonyMembers.get(l).members(p.queenId()))if(l.getEntity(m.worker()) instanceof dev.primeants.entity.LasiusNigerEntity a)out.add(a);return out;
        }
    }
}
