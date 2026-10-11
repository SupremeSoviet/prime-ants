package dev.primeants.founding;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import dev.primeants.colony.ColonyStage;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Codec-only models, never evidence of physical work. The prefix is an actual unchanged T15 founder save. */
class SurfaceAccessTest {
    private JsonObject golden() throws Exception {
        try(var stream=getClass().getResourceAsStream("/dev/primeants/founding/t15-founder-paid-prefix.json")){
            assertNotNull(stream);return JsonParser.parseString(new String(stream.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    private JsonObject model() throws Exception {
        var input=golden();var owner=UUID.fromString(input.keySet().iterator().next().split(":")[0]);
        var data=SurfaceWork.CODEC.parse(JsonOps.INSTANCE,input).getOrThrow();var j=data.job(owner,ColonyStage.MATURE);
        assertTrue(SurfaceAccess.prefix(j));j.access=new SurfaceAccess(owner,j);
        return SurfaceWork.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow().getAsJsonObject();
    }
    private JsonObject job(JsonObject data){return JsonParser.parseString(data.entrySet().iterator().next().getValue().getAsString()).getAsJsonObject();}
    private JsonObject replace(JsonObject data,JsonObject job){var result=data.deepCopy();result.addProperty(result.keySet().iterator().next(),job.toString());return result;}
    private void rejects(JsonObject data,JsonObject job){assertThrows(IllegalArgumentException.class,()->SurfaceWork.CODEC.parse(JsonOps.INSTANCE,replace(data,job)).getOrThrow());}
    @Test void actualFounderPrefixWithoutAccessFieldsRoundTripsExactlyAndModelsOnlyTwoNamedUnits() throws Exception {
        var original=golden();assertEquals(job(original),job(SurfaceWork.CODEC.encodeStart(JsonOps.INSTANCE,SurfaceWork.CODEC.parse(JsonOps.INSTANCE,original).getOrThrow()).getOrThrow().getAsJsonObject()));
        var model=model();assertEquals(model,SurfaceWork.CODEC.encodeStart(JsonOps.INSTANCE,SurfaceWork.CODEC.parse(JsonOps.INSTANCE,model).getOrThrow()).getOrThrow());
        var access=job(model).getAsJsonObject("access");assertEquals(2,access.get("cap").getAsInt());assertEquals(2,access.getAsJsonArray("operations").size());
        assertFalse(access.toString().contains("stack"));assertFalse(access.toString().contains("cargo"));
    }
    @Test void endpointCapUnitBindingOwnerAndInvalidActionCannotBeRestored() throws Exception {
        var data=model();var j=job(data);var a=j.getAsJsonObject("access");
        for(var field:List.of("cap","index","action")){var bad=j.deepCopy();bad.getAsJsonObject("access").addProperty(field,field.equals("action")?20:3);rejects(data,bad);}
        for(var field:List.of("source","destination","removalStand","depositStand")){var bad=j.deepCopy();var op=bad.getAsJsonObject("access").getAsJsonArray("operations").get(0).getAsJsonObject();op.addProperty(field,op.get(field).getAsLong()+1);rejects(data,bad);}
        for(var field:List.of("unit")){var bad=j.deepCopy();bad.getAsJsonObject("access").getAsJsonArray("operations").get(0).getAsJsonObject().addProperty(field,UUID.randomUUID().toString());rejects(data,bad);}
        for(var field:List.of("owner","binding")){var bad=j.deepCopy();bad.getAsJsonObject("access").addProperty(field,UUID.randomUUID().toString());rejects(data,bad);}
        assertEquals(4,a.getAsJsonArray("prefix").size());
    }
    @Test void terminalSecondDeathHistoryKeepsOneCurrentCustodyAndRejectsASecondPickup() throws Exception {
        var data=model();var j=job(data);var a=j.getAsJsonObject("access");a.addProperty("stopped",true);a.addProperty("reason","second_actual_carrier_death");
        var op=a.getAsJsonArray("operations").get(0).getAsJsonObject();op.addProperty("removed",true);op.addProperty("phase","TERMINAL");op.addProperty("deaths",2);op.addProperty("pickups",1);
        var ids=new JsonArray();ids.add(UUID.randomUUID().toString());ids.add(UUID.randomUUID().toString());op.add("releases",ids);
        var events=new JsonArray();events.add("removed:model:1:20");events.add("released:model:first");events.add("named_pickup:model:2:20");events.add("released:model:second");op.add("transitions",events);
        var accepted=replace(data,j);var restored=SurfaceWork.CODEC.parse(JsonOps.INSTANCE,accepted).getOrThrow();var owner=UUID.fromString(accepted.keySet().iterator().next().split(":")[0]);var access=restored.job(owner,ColonyStage.MATURE).access;
        assertTrue(access.stopped);assertEquals(0,access.carried());assertEquals(1,access.removed());assertEquals(2,access.released());assertEquals(1,access.pickups());
        assertEquals(accepted,SurfaceWork.CODEC.encodeStart(JsonOps.INSTANCE,restored).getOrThrow());op.addProperty("pickups",2);rejects(data,j);
    }
    @Test void completedAccessRetainsItsOriginalBindingAfterOrdinaryPrefixProgressWithoutReopeningUnits() throws Exception {
        var data=model();var j=job(data);var a=j.getAsJsonObject("access");a.addProperty("index",2);
        for(var item:a.getAsJsonArray("operations")){var op=item.getAsJsonObject();op.addProperty("removed",true);op.addProperty("redeposited",true);op.addProperty("phase","DEPOSITED");var events=new JsonArray();events.add("removed:model:1:20");events.add("redeposited:model:2:20");op.add("transitions",events);}
        String worker=j.getAsJsonArray("receipts").get(0).getAsString().split(":")[1];j.getAsJsonArray("receipts").add("retained_paid_foundation:"+worker+":-4,3,0");
        var accepted=replace(data,j);assertEquals(accepted,SurfaceWork.CODEC.encodeStart(JsonOps.INSTANCE,SurfaceWork.CODEC.parse(JsonOps.INSTANCE,accepted).getOrThrow()).getOrThrow());
        a.addProperty("index",0);rejects(data,j);
    }
    @Test void unclaimedPartialAccessRecordsRemainRejectedRatherThanGrantingCompatibilityAuthority() throws Exception {
        var data=model();var j=job(data);var a=j.getAsJsonObject("access");a.addProperty("action",5);a.addProperty("lastActionTime",100);
        rejects(data,j); // Exact T16 empty-death invalid shape is not silently admitted or given cargo.
        j.addProperty("claim",UUID.randomUUID().toString());
        assertEquals(replace(data,j),SurfaceWork.CODEC.encodeStart(JsonOps.INSTANCE,SurfaceWork.CODEC.parse(JsonOps.INSTANCE,replace(data,j)).getOrThrow()).getOrThrow());
    }
    @Test void emptySourceDeathResetsPartialClockAndStopsWithoutChangingBoundUnitsOrCredit() throws Exception {
        var data=model();var owner=UUID.fromString(data.keySet().iterator().next().split(":")[0]);var restored=SurfaceWork.CODEC.parse(JsonOps.INSTANCE,data).getOrThrow();var j=restored.job(owner,ColonyStage.MATURE);
        var a=j.access;var binding=a.binding;var operations=a.encode().getAsJsonArray("operations").deepCopy();var receipts=j.receipts();j.claim=UUID.randomUUID();a.action=5;a.lastActionTime=100;
        a.emptyHandedDeath(j);a.emptyHandedDeath(j);
        assertNull(j.claim);assertTrue(a.stopped);assertEquals(0,a.action);assertEquals(Long.MIN_VALUE,a.lastActionTime);assertEquals(binding,a.binding);
        assertEquals(operations,a.encode().getAsJsonArray("operations"));assertEquals(receipts,j.receipts());assertEquals(0,a.released());assertEquals(0,a.current().deaths);
        assertEquals(SurfaceWork.CODEC.encodeStart(JsonOps.INSTANCE,restored).getOrThrow(),SurfaceWork.CODEC.encodeStart(JsonOps.INSTANCE,SurfaceWork.CODEC.parse(JsonOps.INSTANCE,SurfaceWork.CODEC.encodeStart(JsonOps.INSTANCE,restored).getOrThrow()).getOrThrow()).getOrThrow());
    }
    @Test void savedTargetPauseGateIsAdditiveAndRejectsInheritedPartialWork() throws Exception {
        var data=model();var j=job(data);var a=j.getAsJsonObject("access");a.addProperty("targetPaused",true);a.addProperty("targetConfirmations",1);a.addProperty("targetEvaluationAt",200);
        var accepted=replace(data,j);assertEquals(job(accepted),job(SurfaceWork.CODEC.encodeStart(JsonOps.INSTANCE,SurfaceWork.CODEC.parse(JsonOps.INSTANCE,accepted).getOrThrow()).getOrThrow().getAsJsonObject()));
        a.addProperty("action",5);a.addProperty("lastActionTime",300);j.addProperty("claim",UUID.randomUUID().toString());rejects(data,j);
        a.addProperty("action",0);a.addProperty("targetConfirmations",3);rejects(data,j);
    }
}
