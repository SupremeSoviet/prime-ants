package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.*;
import static dev.primeants.gametest.NaturalPlacementGameTest.*;

public final class PlacementDurabilityGameTest {
    static Path file(ServerLevel l) {return DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(LevelResource.ROOT)).resolve("data/prime_ants/natural_placement.dat");}
    static String key(ChunkPos p) {return Long.toString(p.pack());}
    static JsonObject raw(ServerLevel l,ChunkPos p) {
        try {return JsonParser.parseString(NbtIo.readCompressed(file(l),NbtAccounter.unlimitedHeap()).getCompoundOrEmpty("data").getString(key(p)).orElseThrow()).getAsJsonObject();}
        catch(Exception e){throw new RuntimeException(e);}
    }
    static void reload(ServerLevel l) {
        try(var disk=new SavedDataStorage(file(l).getParent().getParent(),net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())) {
            var loaded=disk.get(NaturalPlacement.TYPE);if(loaded==null)throw new AssertionError("Real disk record required");l.getDataStorage().set(NaturalPlacement.TYPE,loaded);
        }
    }
    static void snapshot(ServerLevel l,ChunkPos p,String name) {
        try {
            Path out=l.getServer().getWorldPath(LevelResource.ROOT).resolve("t15-fixtures");Files.createDirectories(out);
            Files.copy(file(l),out.resolve(name+".dat"));
            var result=new JsonObject();result.add("disk",raw(l,p));result.add("live",decision(l,p));result.addProperty("insertion_calls",PlacementFault.insertionCalls(uuid(l,p)));result.addProperty("final_calls",PlacementFault.finalCalls(l,p));
            Files.writeString(out.resolve(name+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));
            dev.primeants.PrimeAnts.LOGGER.info("T15 fixture snapshot {} {}",name,result);
        }catch(Exception e){throw new RuntimeException(e);}
    }
    @GameTest(maxTicks=300)
    public void swallowedReservationWriteCannotInsertAndRecoversOriginalIdentity(GameTestHelper c) {
        var l=level(c,"placement_durability_retry");var p=prepare(c,l,1500);var id=uuid(l,p);PlacementFault.watch(id);
        l.getDataStorage().saveAndJoin();snapshot(l,p,"reservation-initial-pending");
        PlacementFault.blockWrite(file(l),key(p),"RESERVED");boolean[] recovered={false};String[] failedAttempt={null};
        c.onEachTick(()->{
            if(!recovered[0] && PlacementFault.writeFailures(file(l),key(p),"RESERVED")>0) {
                recovered[0]=true;
                snapshot(l,p,"reservation-write-failed");
                failedAttempt[0]=decision(l,p).get("attemptId").getAsString();
                c.assertTrue(l.getEntity(id)==null && PlacementFault.insertionCalls(id)==0,"A normally completed save future must not authorize insertion");
                c.assertTrue(raw(l,p).get("status").getAsString().equals("PENDING"),"Caught IOException leaves actual original PENDING disk record");
                reload(l);c.assertTrue(status(l,p).equals("PENDING") && uuid(l,p).equals(id),"Reload actual unchanged disk, same UUID");
                PlacementFault.releaseWrite(file(l),key(p),"RESERVED");recovered[0]=true;
            }
            if(recovered[0] && status(l,p).equals("PLACED")) {
                c.assertTrue(uuid(l,p).equals(id) && PlacementFault.insertionCalls(id)==1 && l.getEntity(id)!=null,"Recovered storage inserts at most once with original UUID");
                c.assertTrue(!decision(l,p).get("attemptId").getAsString().equals(failedAttempt[0]),"Recovery requires a fresh reservation attempt, never the failed attempt's identity");
                snapshot(l,p,"reservation-recovered");c.succeed();
            }
        });
    }
    @GameTest(maxTicks=300)
    public void completionWriteFailureReloadAndNormalDeathCannotReplace(GameTestHelper c) {
        var l=level(c,"placement_durability_history");var p=prepare(c,l,1600);var id=uuid(l,p);PlacementFault.watch(id);
        PlacementFault.blockWrite(file(l),key(p),"PLACED");boolean[] seen={false};
        c.onEachTick(()->{
            if(seen[0] || PlacementFault.writeFailures(file(l),key(p),"PLACED")==0)return;
            seen[0]=true;var disk=raw(l,p);snapshot(l,p,"completion-write-failed");
            c.assertTrue(disk.get("status").getAsString().equals("RESERVED") && disk.has("attemptId"),"Actual disk retains this attempt's verified reservation");
            c.assertTrue(PlacementFault.insertionCalls(id)==1 && l.getEntity(id)!=null,"Successful world insertion before swallowed completion failure");
            reload(l);c.assertTrue(status(l,p).equals("INDETERMINATE"),"Surviving reservation never authorizes replacement");
            PlacementFault.releaseWrite(file(l),key(p),"PLACED");
            var q=(LasiusNigerEntity)l.getEntity(id);q.hurtServer(l,q.damageSources().generic(),1000);
            c.runAfterDelay(40,()->{c.assertTrue(l.getEntity(id)==null && PlacementFault.insertionCalls(id)==1 && status(l,p).equals("INDETERMINATE"),"Normal queen death after actual disk reload produces no replacement");l.getDataStorage().saveAndJoin();snapshot(l,p,"completion-death-no-replacement");c.succeed();});
        });
    }
    @GameTest(maxTicks=300)
    public void persistentFinalRefusalTerminatesAcrossDiskRestore(GameTestHelper c) {
        var l=level(c,"placement_durability_refusal");var p=prepare(c,l,1700);var id=uuid(l,p);PlacementFault.watch(id);PlacementFault.blockFinal(l,p);
        var surface=new BlockPos(p.getMinBlockX()+8,l.getMinY()+7,p.getMinBlockZ()+8);var before=NaturalPlacement.terrain(l,surface);boolean[] restored={false};int[] wait={0};
        c.onEachTick(()->{
            int calls=PlacementFault.finalCalls(l,p);if(calls==0)return;
            if(!restored[0] && calls>=4) {
                restored[0]=true;
                snapshot(l,p,"final-refusal-partway");reload(l);restored[0]=true;
                c.assertTrue(decision(l,p).get("column").getAsInt()==4,"Disk restore must retain half of finite final-gate budget");
            }
            if(++wait[0]==100 || status(l,p).equals("REJECTED")) {
                l.getDataStorage().saveAndJoin();snapshot(l,p,"final-refusal-terminal");
                c.assertTrue(restored[0] && status(l,p).equals("REJECTED") && calls==64 && decision(l,p).get("column").getAsInt()==64,"Exactly sixty-four final refusals reach persisted terminal search bound");
                var r=decision(l,p);c.assertTrue(r.get("evaluations").getAsInt()==64 && r.get("preliminaryPlans").getAsInt()==64 && r.get("temporaryQueens").getAsInt()==64 && r.get("immediateValidations").getAsInt()==64,"Actual refused work counts stay bounded across reload");
                c.assertTrue(PlacementFault.insertionCalls(id)==0 && l.getEntity(id)==null && before.equals(NaturalPlacement.terrain(l,surface)),"Negative final gate performs no insertion or terrain edit");c.succeed();
            }
        });
    }
}
