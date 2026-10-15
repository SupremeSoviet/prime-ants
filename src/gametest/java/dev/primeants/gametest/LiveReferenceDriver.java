package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.entity.LasiusNigerEntity;
import java.util.*;
import net.minecraft.gametest.framework.GameTestHelper;

/** Test-only ordinary lifecycle. Pinned ServerLevel.tickNonPassenger calls commonTick before tick.
 * Each synchronous reference step snapshots current membership before adults, then ticks its nursery
 * once. Births in that nursery phase join the next step; no server-level tick runs inside these loops. */
final class LiveReferenceDriver {
    static void adults(GameTestHelper c,LasiusNigerEntity q,long now){
        var bodies=AutomaticCatchupGameTest.adults(c,q).stream().filter(a->a!=null&&a.isAlive()&&!a.isRemoved()).toList();
        c.assertTrue(ComparisonEvidence.longs(bodies.stream().map(LasiusNigerEntity::getUUID).distinct().count(),bodies.size(),"==","bodies.stream().map(LasiusNigerEntity::getUUID).distinct().count()==bodies.size()"),"Reference adult membership has no duplicate identity");
        ReconciliationGameTest.atTime(c,now,()->bodies.forEach(a->adult(c,a,now)));
    }
    static void adult(GameTestHelper c,LasiusNigerEntity a,long now){
        int before=a.tickCount;
        c.assertTrue(!ComparisonEvidence.flag(a.isPassenger(),"a.isPassenger()"),"Ordinary reference adult is a non-passenger");
        c.getLevel().tickNonPassenger(a);
        c.assertTrue(!ComparisonEvidence.flag(a.isAlive(),"a.isAlive()")||ComparisonEvidence.flag(a.isRemoved(),"a.isRemoved()")||ComparisonEvidence.longs(a.tickCount,before+1,"==","a.tickCount==before+1"),"Every surviving reference adult advances exactly once through native lifecycle");
        if(a.tickCount%20==0){
            var row=new JsonObject();row.addProperty("time",now);row.addProperty("id",a.getUUID().toString());
            row.addProperty("before",before);row.addProperty("after",a.tickCount);row.addProperty("search_boundary",true);
            row.addProperty("crop_target",String.valueOf(a.workerTasks().sharing().target()));
            row.addProperty("cooldown",a.workerTasks().sharing().cooldownRemaining());
            ReconciliationEvidence.log("t02_cadence","native_reference_boundary",row);
        }
    }
    static JsonObject counters(GameTestHelper c,LasiusNigerEntity q){
        var row=new JsonObject();for(var a:AutomaticCatchupGameTest.adults(c,q))if(a!=null&&!a.isRemoved())row.addProperty(a.getUUID().toString(),a.tickCount);return row;
    }
}
