package dev.primeants.gametest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.primeants.brood.*;
import dev.primeants.colony.*;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.time.*;
import dev.primeants.worker.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.util.ProblemReporter;

/** Production hooks and normal reconstruction. Declared timestamp/accepted-save inputs, not native eviction. */
public final class CatchupRecoveryGameTest {
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void ordinaryFedLarvaSuccessfulFirstSubstepRetainsIdentityAtGraceOne(GameTestHelper c){
        new AutomaticCatchupGameTest().colony(c,false,(q,p)->{
            var plan=q.founding().plan();
            for(var wall:ChamberUpgrade.walls(plan,ChamberRegistry.foundingChamber(plan))){
                c.getLevel().setBlock(wall,NurseryBlocks.NEST_CUT_STONE.defaultBlockState(),3);
                ColonyTerrain.get(c.getLevel()).built(wall,q.getUUID(),NurseryBlocks.NEST_CUT_STONE);
            }
            var tag=p.saveWithFullMetadata(c.getLevel().registryAccess());tag.putLong("StageDuration",120);
            var row=tag.getList("Brood").orElseThrow().getCompound(0).orElseThrow();
            var id=p.records().getFirst().id();row.putString("Stage","LARVA");row.putLong("Progress",59);
            row.putBoolean("Founding",false);row.putLong("NeglectGrace",1);row.putLong("NeglectTicks",0);
            var n=new Nutrition();c.assertTrue(n.ingest(new ItemStack(Items.SWEET_BERRIES),4000,8000)&&n.ingest(new ItemStack(Items.ROTTEN_FLESH),4000,8000),"One real berry and flesh receipt");
            c.assertTrue(n.spend(59*Nutrition.LARVA_SUGAR/120,59*Nutrition.LARVA_PROTEIN/120),"Exact accepted prior larval spending");
            var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,c.getLevel().registryAccess());n.save(out);
            row.put("Nutrition",out.buildResult());row.putLong("Nourishment",n.spentSugar()+n.spentProtein());
            var restored=BroodGameTest.restorePile(c,p,tag);long last=restored.lastSimulationGameTime().orElseThrow();
            AutomaticCatchupGameTest.tickPile(c,restored,last+1);
            var evidence=new JsonObject();evidence.addProperty("id",id.toString());evidence.addProperty("speed",restored.nursery().speed());evidence.add("records",new Gson().toJsonTree(restored.records().stream().map(r->r.id()+":"+r.progress()+":"+r.neglectTicks()+":"+r.nourishment()).toList()));evidence.add("expired",new Gson().toJsonTree(restored.expired()));ReconciliationEvidence.log("ordinary_substep","production_nursery_tick",evidence);
            c.assertTrue(restored.nursery().speed()==4,"Two actual development substeps");
            var living=restored.records().stream().filter(r->r.id().equals(id)).findFirst().orElse(null);
            c.assertTrue(living!=null&&!restored.expired().containsKey(id)&&BroodHistory.get(c.getLevel()).terminal(id)==null,"Successful first substep retains the same living brood at NeglectGrace=1");
            c.assertTrue(living.progress()==60&&living.neglectTicks()==0&&living.nutrition().spentSugar()==2000&&living.nutrition().spentProtein()==4000&&living.nourishment()==6000&&living.nutrition().sugar()==0&&living.nutrition().protein()==0,"Exact inherited progress, spending and zero neglect after unsupported second substep");
            c.succeed();
        });
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void legacyFloor101RecoversAfterOrdinaryAdultTicksAndReconstruction(GameTestHelper c){
        new AutomaticCatchupGameTest().colony(c,false,(q,p)->{
            var pileTag=p.saveWithFullMetadata(c.getLevel().registryAccess());pileTag.putLong(ColonyClock.FIELD,100);pileTag.putLong("LastOrdinaryGameTime",100);
            var adultTag=ReconciliationGameTest.save(c,q);adultTag.remove(AdultSimulation.FIELD);
            LasiusNigerEntity[] returning={q};ReconciliationGameTest.atTime(c,102,()->returning[0]=AutomaticCatchupGameTest.replace(c,returning[0],adultTag));
            var restored=BroodGameTest.restorePile(c,p,pileTag);long loaded=restored.loadedTicks();var ids=restored.records().stream().map(BroodRecord::id).toList();
            AutomaticCatchupGameTest.tickPile(c,restored,103);
            for(long now=104;now<=105;now++){long time=now;ReconciliationGameTest.atTime(c,time,returning[0]::tick);AutomaticCatchupGameTest.tickPile(c,restored,time);}
            var saved=restored.saveWithFullMetadata(c.getLevel().registryAccess());var again=BroodGameTest.restorePile(c,restored,saved);
            ReconciliationGameTest.atTime(c,106,returning[0]::tick);AutomaticCatchupGameTest.tickPile(c,again,106);
            var e=new JsonObject();e.addProperty("floor",returning[0].simulationEvidence().knownAfter());e.addProperty("pending",again.catchup().pendingTicks());e.addProperty("loadedBefore",loaded);e.addProperty("loadedAfter",again.loadedTicks());e.addProperty("status",again.catchup().status());e.addProperty("ledger",again.saveWithFullMetadata(c.getLevel().registryAccess()).getCompound("Catchup").orElseThrow().toString());ReconciliationEvidence.log("legacy_floor_101","ordinary_ticks_and_reconstruction",e);
            c.assertTrue(again.catchup().pendingTicks()==0&&again.loadedTicks()==loaded+4,"Evidence floor cannot permanently stall ordinary nursery development after ticks and reconstruction");
            c.assertTrue(again.records().stream().map(BroodRecord::id).toList().equals(ids),"Migration retains biological identities");
            c.assertTrue(again.catchup().recoveryBaseline()==102&&again.catchup().unknownTicks()==2&&again.catchup().unknownThrough()==102&&again.catchup().appliedTicks()==0,"101..102 is persisted unknown time, never simulated or retrospectively charged");
            var before=again.loadedTicks();AutomaticCatchupGameTest.tickPile(c,again,117);
            c.assertTrue(again.catchup().pendingTicks()==0&&again.catchup().appliedTicks()==10&&again.loadedTicks()==before+11,"Subsequent known absence catches up normally");
            c.succeed();
        });
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void staggeredLegacyMembersRecoverOnlyAfterAllOriginalIdentitiesReturn(GameTestHelper c){
        new AutomaticCatchupGameTest().colony(c,true,(q,p)->{
            long last=p.lastSimulationGameTime().orElseThrow(),loaded=p.loadedTicks();
            var w=new WorkerForagingGameTest().workers(c,q).getFirst();var wt=ReconciliationGameTest.save(c,w);wt.remove(AdultSimulation.FIELD);
            var qt=ReconciliationGameTest.save(c,q);qt.remove(AdultSimulation.FIELD);
            var saved=p.saveWithFullMetadata(c.getLevel().registryAccess());w.remove(net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK);
            LasiusNigerEntity[] qr={q};ReconciliationGameTest.atTime(c,last+2,()->qr[0]=AutomaticCatchupGameTest.replace(c,qr[0],qt));
            var r=BroodGameTest.restorePile(c,p,saved);var original=ColonyMembers.get(c.getLevel()).member(w.getUUID());
            AutomaticCatchupGameTest.tickPile(c,r,last+3);
            c.assertTrue(r.catchup().unknownTicks()==0&&r.catchup().pendingTicks()==2&&original.equals(ColonyMembers.get(c.getLevel()).member(w.getUUID())),"Missing original member remains a genuine deferral, no migration or inferred death");
            var persisted=r.saveWithFullMetadata(c.getLevel().registryAccess());r=BroodGameTest.restorePile(c,r,persisted);
            LasiusNigerEntity[] wr={null};ReconciliationGameTest.atTime(c,last+5,()->{wr[0]=ReconciliationGameTest.load(c,wt);c.assertTrue(c.getLevel().tryAddFreshEntityWithPassengers(wr[0]),"Staggered original worker inserted once");});
            var biology=AutomaticCatchupGameTest.adults(c,qr[0]).stream().map(LasiusNigerEntity::biologySnapshot).toList();
            AutomaticCatchupGameTest.tickPile(c,r,last+6);
            c.assertTrue(r.catchup().pendingTicks()==0&&r.catchup().unknownTicks()==5&&r.catchup().recoveryBaseline()==last+5&&r.loadedTicks()==loaded+1,"Common persisted floor terminates staggered loading without charging five unknown ticks");
            c.assertTrue(biology.equals(AutomaticCatchupGameTest.adults(c,qr[0]).stream().map(LasiusNigerEntity::biologySnapshot).toList())&&original.equals(ColonyMembers.get(c.getLevel()).member(w.getUUID())),"Rebase preserves all biological state and membership reservations");
            AutomaticCatchupGameTest.tickPile(c,r,last+17);
            c.assertTrue(r.catchup().appliedTicks()==10&&r.catchup().unknownTicks()==5&&r.catchup().pendingTicks()==0,"Known post-baseline absence works once");c.succeed();
        });
    }
    @GameTest(maxTicks=8000,structure="prime_ants_test:idle_ground")
    public void retainedHistoryCompactionRebasesUnknownPrefixAndKeepsOrdinaryReceipts(GameTestHelper c){
        new AutomaticCatchupGameTest().colony(c,false,(q,p)->{
            long last=p.lastSimulationGameTime().orElseThrow(),loaded=p.loadedTicks(),age=q.elapsedAgeTicks();
            // Native ordinary adult steps while its nursery is detached. Every disjoint run is real.
            c.getLevel().removeBlockEntity(p.getBlockPos());
            for(int i=1;i<=34;i++){long time=last+2L*i;ReconciliationGameTest.atTime(c,time,q::tick);}
            var evidence=q.simulationEvidence();c.assertTrue(evidence.runs().size()==AdultSimulation.MAX_RUNS&&evidence.knownAfter()>last,"Real bounded history compacted");
            c.getLevel().setBlockEntity(p);var saved=p.saveWithFullMetadata(c.getLevel().registryAccess());
            var restored=BroodGameTest.restorePile(c,p,saved);AutomaticCatchupGameTest.tickPile(c,restored,last+70);
            long unknown=evidence.knownAfter()-last,ordinary=evidence.runs().stream().mapToLong(run->run.last()-run.first()+1).sum();
            c.assertTrue(restored.catchup().unknownTicks()==unknown&&restored.catchup().appliedTicks()==69-unknown&&restored.loadedTicks()==loaded+70-unknown&&restored.catchup().pendingTicks()==0,"Only unreconstructable prefix unknown; known nursery absence completed");
            c.assertTrue(q.elapsedAgeTicks()==age+34+(69-unknown)-ordinary,"Retained ordinary adult receipts excluded from application exactly");
            var t=restored.saveWithFullMetadata(c.getLevel().registryAccess());var replay=BroodGameTest.restorePile(c,restored,t);AutomaticCatchupGameTest.tickPile(c,replay,last+70);
            c.assertTrue(replay.saveWithFullMetadata(c.getLevel().registryAccess()).equals(t),"Compaction baseline and application receipts reconstruct without replay");c.succeed();
        });
    }
}
