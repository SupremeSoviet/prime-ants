package dev.primeants.gametest;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import dev.primeants.brood.BroodPile;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.gametest.mixin.GameTestHelperAccessor;
import dev.primeants.item.AntItems;
import dev.primeants.time.*;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.*;

/** Isolated remote controlled-origin egg colony. Normal ticket/chunk/entity lifecycle, no manual removal or clocks. */
public final class ReconciliationChunkGameTest implements ModInitializer {
    private static TicketType ticket;
    private static final List<Fixture> fixtures = new ArrayList<>();
    @Override public void onInitialize() {
        ticket = Registry.register(BuiltInRegistries.TICKET_TYPE, Identifier.fromNamespaceAndPath("prime_ants_test","reconciliation"), new TicketType(0,14));
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
            for (var fixture : List.copyOf(fixtures)) if (fixture.level == level && fixture.chunk.equals(chunk.getPos())) fixture.unloadEvents++;
        });
    }
    private static final class Fixture implements AutoCloseable {
        final ServerLevel level; final net.minecraft.world.level.ChunkPos chunk;
        boolean held; int unloadEvents;
        Fixture(GameTestHelper c) {
            level = c.getLevel(); chunk = new net.minecraft.world.level.ChunkPos(8192,8192); fixtures.add(this);
            ((GameTestHelperAccessor)c).primeAntsTestInfo().addListener(new GameTestListener(){
                public void testStructureLoaded(GameTestInfo i){}
                public void testPassed(GameTestInfo i,GameTestRunner r){close();}
                public void testFailed(GameTestInfo i,GameTestRunner r){diagnose("failed");close();}
                public void testAddedForRerun(GameTestInfo i,GameTestInfo copy,GameTestRunner r){close();}
            });
        }
        void hold() { if (!held) { level.getChunkSource().addTicketWithRadius(ticket,chunk,2); held=true; } }
        void release() { if (held) { level.getChunkSource().removeTicketWithRadius(ticket,chunk,2); held=false; } }
        boolean unavailable(BlockPos nursery) {
            return level.getChunkSource().getChunk(chunk.x(),chunk.z(),ChunkStatus.FULL,false)==null
                && level.getChunkSource().getChunkNow(chunk.x(),chunk.z())==null && !level.isPositionEntityTicking(nursery);
        }
        void diagnose(String label) {
            var out=new JsonObject();out.addProperty("game_time",level.getGameTime());out.addProperty("chunk",chunk.toString());
            out.addProperty("held",held);out.addProperty("unload_events",unloadEvents);
            out.addProperty("chunk_state",level.getChunkSource().getChunkDebugData(chunk));
            var storage=level.getDataStorage().computeIfAbsent(net.minecraft.world.level.TicketStorage.TYPE);
            out.addProperty("tickets",storage.getTickets(chunk.pack()).toString());
            ReconciliationEvidence.log(label,"genuine_chunk_lifecycle",out);
        }
        @Override public void close(){release();fixtures.remove(this);}
    }
    private static BlockPos remoteEgg(GameTestHelper c, Fixture fixture) {
        var l=fixture.level; fixture.hold(); l.getChunkSource().getChunk(fixture.chunk.x(),fixture.chunk.z(),ChunkStatus.FULL,true);
        // Disclosed controlled terrain/origin exactly like idle_ground founding, outside helper retention.
        var origin=NaturalSoil.CODEC.encodeStart(JsonOps.INSTANCE,NaturalSoil.get(l)).getOrThrow().getAsJsonObject();
        int x0=fixture.chunk.getMinBlockX(),z0=fixture.chunk.getMinBlockZ(),y0=80;
        for(int x=1;x<=14;x++)for(int z=1;z<=14;z++)for(int y=0;y<=8;y++){
            var pos=new BlockPos(x0+x,y0+y,z0+z);var block=y==0?Blocks.STONE:y<=4?Blocks.DIRT:Blocks.AIR;
            l.setBlock(pos,block.defaultBlockState(),3);
            if(y>=1&&y<=4)origin.addProperty(Long.toString(pos.asLong()),"minecraft:dirt");
        }
        l.getDataStorage().set(NaturalSoil.TYPE,NaturalSoil.CODEC.parse(JsonOps.INSTANCE,origin).getOrThrow());
        var p=new BlockPos(x0+7,y0+4,z0+4);
        c.assertTrue(!c.getBounds().inflate(128).contains(Vec3.atCenterOf(p)),"Remote colony lies outside helper retained area");
        var player=new net.minecraft.world.entity.player.Player(l,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"reconciliation-fixture")){
            public GameType gameMode(){return GameType.CREATIVE;}public boolean isClientAuthoritative(){return false;}
            public net.minecraft.server.permissions.PermissionSet permissions(){return net.minecraft.server.permissions.PermissionSet.ALL_PERMISSIONS;}
        };
        var egg=new ItemStack(AntItems.DEBUG_QUEEN_EGG);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,egg);
        c.assertTrue(egg.useOn(new net.minecraft.world.item.context.UseOnContext(l,player,net.minecraft.world.InteractionHand.MAIN_HAND,egg,new BlockHitResult(Vec3.atCenterOf(p).add(0,.5,0),Direction.UP,p,false))).consumesAction(),"Remote production queen egg");
        return p; // Remote entity visibility follows the native entity-manager tick, not the ticket request.
    }

    @GameTest(maxTicks=8000, structure="prime_ants_test:idle_ground")
    public void genuineRemoteChunkDisappearsAndReloadsOriginalColonyWhileLiveControlTicks(GameTestHelper c) {
        var live=new WorkerForagingGameTest().start(c);var fixture=new Fixture(c);var spawn=remoteEgg(c,fixture);var l=c.getLevel();
        LasiusNigerEntity[] remote={null};
        int[] phase={0}; long[] requested={0}, disappeared={0}, controlAge={0}, controlLatest={0}, nurseryLast={0}, oldAge={0}, oldLoaded={0};
        BroodPile[] old={null};List<LasiusNigerEntity> adults=new ArrayList<>();Map<UUID,AdultSimulation.Snapshot> boundaries=new LinkedHashMap<>();
        Map<UUID,Long> ages=new LinkedHashMap<>();Set<UUID> brood=new HashSet<>();
        c.onEachTick(()->{
            if(remote[0]==null){
                var candidates=l.getEntitiesOfClass(LasiusNigerEntity.class,new AABB(spawn).inflate(2),a->a.form()==AntForm.QUEEN);
                if(candidates.isEmpty()){
                    if(c.getTick()>100){fixture.diagnose("spawn_visibility_stalled");c.fail("Production remote egg body not visible after 100 actual manager ticks");}
                    return;
                }
                c.assertTrue(candidates.size()==1,"Exactly one remote production egg body becomes visible");remote[0]=candidates.getFirst();
            }
            var q=remote[0];var queen=q.getUUID();
            if(phase[0]==0){
                if(q.founding().plan()==null||!q.founding().ready())return;
                var pos=q.founding().plan().nursery(); if(!(l.getBlockEntity(pos) instanceof BroodPile p))return;
                var ws=ColonyMembers.get(l).members(queen);if(ws.size()!=3||ws.stream().anyMatch(m->!(l.getEntity(m.worker()) instanceof LasiusNigerEntity)))return;
                c.assertTrue(l.isPositionEntityTicking(pos)&&q.elapsedAgeTicks()>100&&live.elapsedAgeTicks()>100,"Both remote colony and independent helper control really tick");
                old[0]=p;adults.add(q);for(var m:ws)adults.add((LasiusNigerEntity)l.getEntity(m.worker()));brood.addAll(p.consumed());
                c.assertTrue(ChunkPos.containing(pos).equals(fixture.chunk)&&adults.stream().allMatch(a->ChunkPos.containing(a.blockPosition()).equals(fixture.chunk)),"All measured original colony bodies and nursery reside in fixture chunk");
                c.assertTrue(l.getServer().saveEverything(false,true,true),"Normal server checkpoint before owned ticket release");
                requested[0]=l.getGameTime();fixture.diagnose("release_requested");fixture.release();phase[0]=1;return;
            }
            if(phase[0]==1){
                if(l.getGameTime()-requested[0]>1200){fixture.diagnose("unload_stalled");c.fail("Native chunk eviction did not complete in fixed 1200-tick phase");return;}
                if(c.getTick()%100==0)fixture.diagnose("waiting_for_actual_unload");
                if(!fixture.unavailable(old[0].getBlockPos())||adults.stream().anyMatch(a->l.getEntity(a.getUUID())!=null)||fixture.unloadEvents==0)return;
                disappeared[0]=l.getGameTime();controlAge[0]=live.elapsedAgeTicks();controlLatest[0]=live.simulationEvidence().latest();
                nurseryLast[0]=old[0].lastSimulationGameTime().orElseThrow();oldAge[0]=q.elapsedAgeTicks();oldLoaded[0]=old[0].loadedTicks();
                var out=new JsonObject();out.addProperty("requested",requested[0]);out.addProperty("disappeared",disappeared[0]);out.addProperty("nursery_saved_boundary",nurseryLast[0]);
                out.addProperty("queen",queen.toString());out.addProperty("nursery",old[0].getBlockPos().asLong());out.addProperty("unload_events",fixture.unloadEvents);
                out.addProperty("chunk_unavailable",true);out.addProperty("entities_absent",true);out.addProperty("entity_ticking",false);
                var saved=new JsonArray();for(var adult:adults){
                    c.assertTrue(adult.isRemoved()&&adult.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK,"Vanilla chunk lifecycle actually removed original body");
                    boundaries.put(adult.getUUID(),adult.simulationEvidence());ages.put(adult.getUUID(),adult.elapsedAgeTicks());
                    var row=ReconciliationEvidence.adult(adult.simulationEvidence());row.addProperty("uuid",adult.getUUID().toString());row.addProperty("age",adult.elapsedAgeTicks());saved.add(row);
                }out.add("saved_adults",saved);out.addProperty("live_control_uuid",live.getUUID().toString());out.addProperty("live_control_age",controlAge[0]);out.addProperty("live_control_latest",controlLatest[0]);
                ReconciliationEvidence.log("actual_disappearance","genuine_chunk_lifecycle",out);phase[0]=2;return;
            }
            if(phase[0]==2){
                c.assertTrue(fixture.unavailable(old[0].getBlockPos())&&adults.stream().allMatch(a->l.getEntity(a.getUUID())==null)
                    && q.elapsedAgeTicks()==oldAge[0]&&old[0].loadedTicks()==oldLoaded[0],"Measured remote interval has real absence and zero original biological ticking");
                c.assertTrue(ColonyMembers.get(l).occupied(queen)==3&&adults.stream().skip(1).allMatch(a->!ColonyMembers.get(l).member(a.getUUID()).dead()),"Unavailable registry retains all three original occupied identities");
                if(l.getGameTime()-disappeared[0]<40)return;
                long span=l.getGameTime()-disappeared[0];
                c.assertTrue(span==40&&live.elapsedAgeTicks()-controlAge[0]==40&&live.simulationEvidence().latest()-controlLatest[0]==40,"Independent control completed exactly every one of 40 measured game ticks");
                var out=new JsonObject();out.addProperty("unloaded_from",disappeared[0]);out.addProperty("unloaded_until",l.getGameTime());out.addProperty("measured_span",span);
                out.addProperty("live_control_steps",live.elapsedAgeTicks()-controlAge[0]);out.addProperty("live_control_latest",live.simulationEvidence().latest());out.addProperty("live_control_uuid",live.getUUID().toString());
                ReconciliationEvidence.log("measured_unloaded_span","genuine_chunk_lifecycle",out);
                fixture.hold();l.getChunkSource().getChunk(fixture.chunk.x(),fixture.chunk.z(),ChunkStatus.FULL,true);phase[0]=3;return;
            }
            if(phase[0]==3){
                var pos=old[0].getBlockPos();if(!l.isPositionEntityTicking(pos)||!(l.getBlockEntity(pos) instanceof BroodPile p)||p.reloadObservation()==null)return;
                var capture=p.reloadObservation();if(capture.interval().afterGameTime().orElseThrow()!=nurseryLast[0])return;
                if(adults.stream().anyMatch(a->!(l.getEntity(a.getUUID()) instanceof LasiusNigerEntity)))return;
                c.assertTrue(p!=old[0]&&p.queenId().equals(queen)&&p.original().equals(old[0].original())&&p.consumed().equals(brood),"Native reload reconstructs original nursery identity/brood, no replacement");
                c.assertTrue(capture.interval().equals(ReconciliationGameTest.interval(nurseryLast[0],capture.interval().beforeGameTime()))
                    && capture.interval().ticks()>=40&&p.lastSimulationGameTime().orElseThrow()>=capture.interval().beforeGameTime(),"Pre-step nursery interval retained after normal resumed stamp");
                var out=ReconciliationEvidence.observation(capture);var returning=new JsonArray();
                for(var original:adults){
                    var adult=(LasiusNigerEntity)l.getEntity(original.getUUID());var saved=boundaries.get(adult.getUUID());var boundary=adult.lastSimulationBoundary();
                    c.assertTrue(adult!=original&&adult.getUUID().equals(original.getUUID())&&Objects.equals(adult.broodId(),original.broodId())
                        &&Objects.equals(adult.queenId(),original.queenId())&&Objects.equals(adult.nurseryHome(),original.nurseryHome()),"Native entity reload retains exact identity/lineage/home");
                    c.assertTrue(boundary!=null&&boundary.evidence().equals(saved)&&boundary.ownInterval().afterGameTime().orElseThrow()==saved.latest(),"Adult pre-step boundary retains exact persisted ranges before resumption");
                    var capturedMember=ReconciliationGameTest.member(capture,adult.getUUID());
                    c.assertTrue(capturedMember.reserved()&&capturedMember.adult().equals(original.getUUID())
                        &&Objects.equals(capturedMember.brood(),original.broodId())&&capturedMember.queen().equals(queen),
                        "Frozen capture retains every original identity and reservation");
                    long first=boundary.ownInterval().beforeGameTime();
                    long observedAfterNursery=0;
                    for(var run:saved.runs()){
                        long lo=Math.max(nurseryLast[0]+1,run.first()),hi=Math.min(capture.interval().beforeGameTime()-1,run.last());
                        if(hi>=lo)observedAfterNursery+=hi-lo+1;
                    }
                    if(capturedMember.availability().equals("OBSERVED")){
                        long capturedResumed=Math.max(0,Math.min(capturedMember.evidence().latest(),capture.interval().beforeGameTime()-1)-first+1);
                        long expectedCaptured=observedAfterNursery+capturedResumed;
                        c.assertTrue(capturedMember.coverage().status().equals("RESOLVED")
                            &&capturedMember.coverage().alreadySimulated().orElseThrow()==expectedCaptured
                            &&capturedMember.coverage().missed().orElseThrow()==capture.interval().ticks()-expectedCaptured,
                            "Frozen nursery member evidence has exact native lifecycle counts");
                        if(first==capture.interval().beforeGameTime())c.assertTrue(capturedMember.ownInterval().equals(boundary.ownInterval()),
                            "Frozen current-C return retains original adult boundary in native ordering");
                    }else{
                        c.assertTrue(capturedMember.availability().equals("UNKNOWN_UNAVAILABLE")&&capturedMember.evidence()==null
                            &&capturedMember.coverage().alreadySimulated().isEmpty()&&capturedMember.coverage().missed().isEmpty(),
                            "Body unavailable at capture remains UNKNOWN even after its later native return");
                    }
                    // Later reconciliation is separate evidence; it never repairs/replaces a captured UNKNOWN row.
                    var reconciled=adult.simulationEvidence().reconcile(capture.interval());
                    c.assertTrue(reconciled.status().equals("RESOLVED")&&reconciled.missed().orElseThrow()==0,
                        "Production hooks automatically fill all confirmed absence; no test application callback");
                    var row=ReconciliationEvidence.adult(adult.simulationEvidence());row.addProperty("uuid",adult.getUUID().toString());row.add("own_boundary",ReconciliationEvidence.interval(boundary.ownInterval()));
                    row.addProperty("already_simulated",reconciled.alreadySimulated().orElseThrow());row.addProperty("missed",reconciled.missed().orElseThrow());row.addProperty("saved_age",ages.get(adult.getUUID()));row.addProperty("reloaded_age",adult.elapsedAgeTicks());
                    long totalAfter=adult.simulationEvidence().latest()-Math.max(nurseryLast[0],saved.latest());
                    c.assertTrue(adult.elapsedAgeTicks()-ages.get(adult.getUUID())==totalAfter,
                        "Native reload changes age by exact absent plus resumed ticks, including C only once");returning.add(row);
                }
                c.assertTrue(ColonyMembers.get(l).occupied(queen)==3&&p.records().isEmpty(),"Same three reservations, no new adult or brood on native reload");
                c.assertTrue(p.catchup().pendingTicks()==0&&p.catchup().appliedTicks()>=40&&p.catchup().lastEvents()<=ColonyCatchup.MAX_EVENTS,"Native reload applies bounded catchup automatically");
                out.addProperty("applied_ticks",p.catchup().appliedTicks());out.addProperty("pending_ticks",p.catchup().pendingTicks());out.addProperty("events",p.catchup().lastEvents());out.addProperty("application_nanoseconds",p.catchup().lastNanos());
                out.addProperty("no_manual_application",true);out.addProperty("live_control_adults",ColonyMembers.get(l).occupied(live.getUUID())+1);out.addProperty("catchup_adults",ColonyMembers.get(l).occupied(queen)+1);
                out.addProperty("finite_initial_queen_reserve",BroodPile.MAX_RESERVE);out.addProperty("incoming_supplies",0);
                c.assertTrue(ColonyMembers.get(l).occupied(live.getUUID())==3&&p.records().isEmpty()&&new NursingGameTest().pile(c,live).records().isEmpty(),"Matched finite live control has same four adults, zero brood and no incoming supplies");
                out.add("later_returning_adults_reconciliation",returning);out.addProperty("nursery_stamp_after_capture",p.lastSimulationGameTime().orElseThrow());out.addProperty("loaded_ticks_after",p.loadedTicks());out.addProperty("loaded_ticks_before",oldLoaded[0]);
                ReconciliationEvidence.log("native_reload_capture","genuine_chunk_lifecycle",out);fixture.close();phase[0]=4;c.succeed();
            }
        });
    }
}
