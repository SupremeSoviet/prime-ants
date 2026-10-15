package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.entity.LasiusNigerEntity;
import java.util.*;
import net.minecraft.server.level.ServerLevel;

/** Read-only receipt observer. Inter-completion windows include navigation, selection and cooldown. */
public final class CaregiverCalibration {
    private static final Map<UUID,Map<UUID,List<Long>>> WATCH=new LinkedHashMap<>();
    public static void start(UUID queen){WATCH.put(queen,new LinkedHashMap<>());}
    public static void completed(ServerLevel l,LasiusNigerEntity worker,net.minecraft.world.item.ItemStack unit,UUID recipient){
        var rows=WATCH.get(worker.queenId());if(rows==null)return;
        rows.computeIfAbsent(worker.getUUID(),id->new ArrayList<>()).add(l.getGameTime());
        var row=new JsonObject();row.addProperty("queen",worker.queenId().toString());row.addProperty("caregiver",worker.getUUID().toString());row.addProperty("tick",l.getGameTime());row.addProperty("physical_completion",true);
        row.addProperty("physical_unit",unit.toString());row.addProperty("recipient",String.valueOf(recipient));row.addProperty("hand_empty_after",worker.getMainHandItem().isEmpty());row.addProperty("ordinary_caregiver",worker.workerTasks().caregiver(l,worker.workerTasks().plan()));
        ReconciliationEvidence.log("t04_calibration_receipt","native_nurse_feed_success",row);
    }
    public static JsonObject finish(UUID queen,long start,long end){
        var rows=WATCH.remove(queen);var row=new JsonObject();row.addProperty("queen",queen.toString());row.addProperty("start",start);row.addProperty("end",end);row.add("caregiver_receipts",new Gson().toJsonTree(rows));
        long ticks=0,samples=0;for(var times:rows.values())if(times.size()>1){ticks+=times.getLast()-times.getFirst();samples+=times.size()-1;}
        row.addProperty("complete_cycle_ticks",ticks);row.addProperty("complete_cycle_samples",samples);row.addProperty("aggregate_applied",0);
        row.addProperty("startup_and_censoring","Exclude first receipt latency and final incomplete tail per caregiver; pool consecutive complete inter-receipt windows, never colony wall time divided by completions.");
        ReconciliationEvidence.log("t04_calibration_window","native_live_control",row);return row;
    }
}
