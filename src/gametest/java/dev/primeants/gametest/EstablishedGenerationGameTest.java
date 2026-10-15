package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.PrimeAnts;
import dev.primeants.brood.*;
import dev.primeants.colony.*;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;

/** Every positive starts at a genuinely ungenerated chunk and uses the production event/installer.
 * No completed registry, terrain fixture, stage setter, generated-init exception, or actor creation in this class. */
public final class EstablishedGenerationGameTest {
    static void evidence(String kind,ServerLevel l,ChunkPos cp,JsonObject details){
        var j=new JsonObject();j.addProperty("kind",kind);j.addProperty("seed",l.getSeed());j.addProperty("dimension",l.dimension().identifier().toString());j.addProperty("chunkX",cp.x());j.addProperty("chunkZ",cp.z());j.addProperty("gameTime",l.getGameTime());j.add("decision",NaturalPlacementGameTest.decision(l,cp));j.add("details",details);PrimeAnts.LOGGER.info("T09 GENERATION {}",j);
    }
    static LasiusNigerEntity queen(ServerLevel l,ChunkPos p){return (LasiusNigerEntity)l.getEntity(NaturalPlacementGameTest.uuid(l,p));}
    static void physical(GameTestHelper c,ServerLevel l,ChunkPos cp,ColonyStage stage){
        var r=NaturalPlacementGameTest.decision(l,cp);var q=queen(l,cp);c.assertTrue(q!=null&&q.isAlive(),"Real generated queen");var p=q.founding().plan();var owner=q.getUUID();
        c.assertTrue(r.get("authority").getAsString().equals(NaturalPlacement.AUTHORITY)&&r.get("profile").getAsString().equals(stage.serializedName())&&NaturalPlacement.selectedProfile(l,cp)==stage,"Genuine deterministic profile authority");
        c.assertTrue(r.get("provenance").getAsString().equals("generated_initial:"+EstablishedGeneration.VERSION)&&r.get("planVersion").getAsString().equals(EstablishedGeneration.VERSION),"Frozen generated provenance/version");
        c.assertTrue(r.get("edits").getAsInt()==(stage==ColonyStage.YOUNG?34:102)&&r.get("editLimit").getAsInt()==r.get("edits").getAsInt(),"Exact bounded footprint edits");
        c.assertTrue(r.get("footprintCells").getAsInt()==(stage==ColonyStage.YOUNG?32:99)&&r.get("blockWrites").getAsInt()==(stage==ColonyStage.YOUNG?35:104)&&r.get("blockWriteLimit").getAsInt()==r.get("blockWrites").getAsInt(),"Every canonical projection/write is counted within the frozen unique-cell footprint");
        c.assertTrue(q.founding().removed()==0&&q.founding().deposited()==0&&q.founding().converted()==0&&q.founding().released()==0,"No invented excavation/haul history");
        c.assertTrue(p.nurseryProblem(l,owner,true)==null&&q.founding().ready()&&p.plugs().stream().allMatch(at->ColonyPlugs.get(l).opened(l,at,owner)),"Dry connected supported entrance and operational nest");
        var n=EstablishedNest.get(l).initial(owner);c.assertTrue(n!=null&&n.space().size()==(stage==ColonyStage.YOUNG?24:72),"Every declared underground cell");
        var t=ColonyTerrain.get(l);for(var at:n.space()){
            boolean marker=at.equals(p.nursery())||at.equals(p.cache())||stage==ColonyStage.MATURE&&at.equals(MaterialStore.marker(p,"left"));
            c.assertTrue(marker||t.generatedOpen(l,at,owner),"Physical owned generated void "+at);
        }
        var forms=EstablishedGeneration.surface(p,stage);c.assertTrue(forms.size()==(stage==ColonyStage.YOUNG?8:27),"Stage appropriate surface vocabulary");
        for(var at:forms.keySet())c.assertTrue(t.generatedSurface(l,at,owner)&&l.getBlockState(at).is(NurseryBlocks.NEST_SOIL),"Every generated surface component "+at);
        var members=ColonyMembers.get(l).members(owner);c.assertTrue(members.size()==EstablishedGeneration.workers(stage)&&members.stream().noneMatch(ColonyMembers.Member::dead),"Exact finite real membership");
        for(int i=0;i<members.size();i++){
            var lineage=EstablishedGeneration.lineage(owner,i);var id=ColonyMembers.workerId(lineage);var w=(LasiusNigerEntity)l.getEntity(id);
            c.assertTrue(w!=null&&w.isAlive()&&!w.isNoAi()&&!w.isCallow()&&ColonyMembers.get(l).belongs(w,owner,p.chamber())&&ColonyMembers.get(l).member(id).origin().equals("generated_initial:"+EstablishedGeneration.VERSION),"Canonical real generated worker, origin and identity");
        }
        var pile=(BroodPile)l.getBlockEntity(p.nursery());var cache=(NestCache)l.getBlockEntity(p.cache());
        c.assertTrue(pile.ownedBy(owner,p)&&pile.operational()&&pile.records().isEmpty()&&pile.consumed().isEmpty()&&pile.original().isEmpty(),"Owned empty initial nursery, no fabricated births");
        c.assertTrue(cache.ownedBy(owner,p)&&cache.size()==6&&cache.contents().stream().filter(s->s.is(Items.APPLE)).count()==4&&cache.contents().stream().filter(s->s.is(Items.CHICKEN)).count()==2,"Finite physical initial food");
        if(stage==ColonyStage.MATURE){var store=MaterialStore.owned(l,owner,p);c.assertTrue(store!=null&&store.size()==16&&store.units(MaterialUnits.Material.CLAY)==16,"Physical canonical initial clay");}
        c.assertTrue(pile.stageEvaluation()==null||pile.stageEvaluation().stage()==stage,"No forced runtime stage");
        var live=ColonyDevelopment.evaluate(l,owner,p,true,q.colonyAdultCapacity(),pile.loadedTicks());
        c.assertTrue(live.stage()==stage&&live.cap()>=members.size()+1&&r.get("liveAdultCap").getAsInt()==live.cap()&&live.inputs().adults().known()==members.size()+1&&live.chambers().stream().allMatch(ch->ch.functions().values().stream().allMatch(f->f==ColonyDevelopment.Presence.CONFIRMED)),"Production derives required stage/cap and all functions from actual bodies and chambers");
        var initial=new JsonObject();initial.addProperty("outsideFeet",p.outside().toShortString());initial.addProperty("outsideHeightAdaptation",p.outside().getY()-p.entrance().getY()-1);evidence("initial_physical",l,cp,initial);
    }
    static void positive(GameTestHelper c,ColonyStage stage,int base){
        var l=NaturalPlacementGameTest.level(c,"placement_soil");var cp=NaturalPlacementGameTest.prepare(c,l,base,stage);settle(c,l,cp,stage);
    }
    static void settle(GameTestHelper c,ServerLevel l,ChunkPos cp,ColonyStage stage){
        var observation=new Observation();c.onEachTick(()->observation.tick(c,l,cp,stage));
    }
    static final class Observation {
        final boolean[] inspected={false},outside={false},inside={false},readback={false};final long[] settled={-1};final Map<UUID,net.minecraft.world.phys.Vec3> start=new HashMap<>();
        void tick(GameTestHelper c,ServerLevel l,ChunkPos cp,ColonyStage stage){
            var status=NaturalPlacementGameTest.status(l,cp);c.assertTrue(!Set.of("REJECTED","INDETERMINATE").contains(status),"Positive production installation must complete: "+NaturalPlacementGameTest.decision(l,cp));if(!status.equals("PLACED"))return;
            var q=queen(l,cp);var p=q.founding().plan();var pile=(BroodPile)l.getBlockEntity(p.nursery());
            if(!inspected[0]){physical(c,l,cp,stage);for(var m:ColonyMembers.get(l).members(q.getUUID()))start.put(m.worker(),l.getEntity(m.worker()).position());inspected[0]=true;}
            for(var m:ColonyMembers.get(l).members(q.getUUID()))if(l.getEntity(m.worker()) instanceof LasiusNigerEntity w){
                if(w.position().distanceToSqr(start.get(m.worker()))>.25&&w.getY()>=p.entrance().getY())outside[0]=true;
                if(w.position().distanceToSqr(start.get(m.worker()))>.25&&w.getY()<p.entrance().getY()-1)inside[0]=true;
            }
            boolean service=q.nutrition().consumedUnits()>0||ColonyMembers.get(l).members(q.getUUID()).stream().anyMatch(m->l.getEntity(m.worker()) instanceof LasiusNigerEntity w&&w.nutrition().consumedUnits()>0);
            if(!outside[0]||!inside[0]||!service)return;
            if(!readback[0]){readback[0]=true;readback(c,l,cp);settled[0]=pile.loadedTicks();evidence("ordinary_movement_service_and_readback",l,cp,new JsonObject());}
            if(!q.founding().ready()||ChamberRegistry.get(l).colony(q.getUUID()).stage()!=stage){settled[0]=pile.loadedTicks();return;}
            if(pile.loadedTicks()-settled[0]<200)return;
            c.assertTrue(q.founding().ready()&&ChamberRegistry.get(l).colony(q.getUUID()).stage()==stage,"Live stage and readiness persist through settlement");
            c.assertTrue(ColonyMembers.get(l).occupied(q.getUUID())==EstablishedGeneration.workers(stage),"No duplicate or lost generated population");
            for(var at:EstablishedGeneration.surface(p,stage).keySet())c.assertTrue(ColonyTerrain.get(l).generatedSurface(l,at,q.getUUID()),"All generated surface components survive the consecutive settled interval");
            var d=new JsonObject();d.addProperty("settledNurseryTicks",pile.loadedTicks()-settled[0]);d.addProperty("queenConsumed",q.nutrition().consumedUnits());d.addProperty("outsideMovement",outside[0]);d.addProperty("insideMovement",inside[0]);d.addProperty("contributions",0);evidence("accepted",l,cp,d);c.succeed();
        }
    }
    static void readback(GameTestHelper c,ServerLevel l,ChunkPos cp){
        var q=queen(l,cp);var p=q.founding().plan();var owner=q.getUUID();var members=ColonyMembers.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,ColonyMembers.get(l)).getOrThrow();
        var chambers=ChamberRegistry.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,ChamberRegistry.get(l)).getOrThrow();var terrain=ColonyTerrain.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,ColonyTerrain.get(l)).getOrThrow();
        var cache=(NestCache)l.getBlockEntity(p.cache());var cacheTag=cache.saveWithFullMetadata(l.registryAccess());var pile=(BroodPile)l.getBlockEntity(p.nursery());var pileTag=pile.saveWithFullMetadata(l.registryAccess());
        var store=MaterialStore.owned(l,owner,p);var storeTag=store==null?null:store.saveWithFullMetadata(l.registryAccess());
        l.getDataStorage().saveAndJoin();
        try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){
            l.getDataStorage().set(NaturalPlacement.TYPE,Objects.requireNonNull(disk.get(NaturalPlacement.TYPE)));l.getDataStorage().set(ColonyMembers.TYPE,Objects.requireNonNull(disk.get(ColonyMembers.TYPE)));l.getDataStorage().set(ChamberRegistry.TYPE,Objects.requireNonNull(disk.get(ChamberRegistry.TYPE)));l.getDataStorage().set(ColonyTerrain.TYPE,Objects.requireNonNull(disk.get(ColonyTerrain.TYPE)));l.getDataStorage().set(ColonyPlugs.TYPE,Objects.requireNonNull(disk.get(ColonyPlugs.TYPE)));l.getDataStorage().set(EstablishedNest.TYPE,Objects.requireNonNull(disk.get(EstablishedNest.TYPE)));
        }
        c.assertTrue(members.equals(ColonyMembers.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,ColonyMembers.get(l)).getOrThrow())&&chambers.equals(ChamberRegistry.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,ChamberRegistry.get(l)).getOrThrow())&&terrain.equals(ColonyTerrain.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,ColonyTerrain.get(l)).getOrThrow()),"Actual SavedData disk readback preserves canonical membership, chambers and terrain");
        // Normal serializers, in-memory reconstructed block entities; no claim of disk entity/chunk reload.
        for(var tag:storeTag==null?List.of(cacheTag,pileTag):List.of(cacheTag,pileTag,storeTag)){
            var pos=new BlockPos(tag.getIntOr("x",0),tag.getIntOr("y",0),tag.getIntOr("z",0));var restored=net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,l.getBlockState(pos),tag,l.registryAccess());c.assertTrue(restored!=null,"Serialized block entity reconstructs");l.setBlockEntity(restored);
            c.assertTrue(tag.equals(restored.saveWithFullMetadata(l.registryAccess())),"Exact canonical inventory/ownership reconstruction");
        }
        // Exercise normal actor decoding without removing/reinserting the live originals.
        var ids=new ArrayList<UUID>();ids.add(owner);ColonyMembers.get(l).members(owner).forEach(m->ids.add(m.worker()));
        for(var id:ids){
            var original=(LasiusNigerEntity)l.getEntity(id);var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess());original.save(out);
            var back=(LasiusNigerEntity)net.minecraft.world.entity.EntityType.loadEntityRecursive(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess(),out.buildResult()),l,net.minecraft.world.entity.EntitySpawnReason.LOAD,e->e);
            c.assertTrue(back!=null&&back.getUUID().equals(id)&&back.elapsedAgeTicks()==original.elapsedAgeTicks()&&back.nutrition().generatedSugar()==original.nutrition().generatedSugar()&&back.nutrition().consumedUnits()==original.nutrition().consumedUnits()&&back.nurseryClaimed()==original.nurseryClaimed(),"Normal entity reconstruction preserves identity, generated reserves, age and history");
            c.assertTrue(l.getEntity(id)==original,"Decoding never inserts a second actor");
            if(id.equals(owner))c.assertTrue(back.founding().plan().outside().equals(original.founding().plan().outside())&&back.founding().plan().surfaceDeposits().equals(original.founding().plan().surfaceDeposits()),"Canonical validated exterior adaptations survive normal queen decoding");
        }
        NaturalPlacementGameTest.reloadChunk(l,cp);NaturalPlacementGameTest.reloadChunk(l,cp);c.assertTrue(NaturalPlacementGameTest.uuid(l,cp).equals(owner)&&NaturalPlacementGameTest.status(l,cp).equals("PLACED"),"Repeated FULL deserialization does not mint placement");
    }
    @GameTest(maxTicks=6000)
    public void youngGenerationHasLiveNestMovementServiceAndSettledReadback(GameTestHelper c){positive(c,ColonyStage.YOUNG,6000);}
    @GameTest(maxTicks=6000)
    public void matureGenerationHasFourFunctionsMovementServiceAndSettledReadback(GameTestHelper c){positive(c,ColonyStage.MATURE,6200);}
    @GameTest(maxTicks=500)
    public void protectedUnsupportedAndUnavailableFootprintsNeverInstall(GameTestHelper c){
        var l=NaturalPlacementGameTest.level(c,"placement_soil");var protectedCp=NaturalPlacementGameTest.prepare(c,l,6400,ColonyStage.YOUNG);var unsupported=NaturalPlacementGameTest.prepare(c,l,6600,ColonyStage.MATURE);var unavailable=NaturalPlacementGameTest.prepare(c,l,6800,ColonyStage.MATURE);int y=l.getMinY()+7;
        for(var cp:List.of(protectedCp,unsupported))for(int x=cp.getMinBlockX()-16;x<=cp.getMaxBlockX()+16;x++)for(int z=cp.getMinBlockZ()-16;z<=cp.getMaxBlockZ()+16;z++){
            var at=new BlockPos(x,y-2,z);l.setBlock(at,(cp.equals(protectedCp)?Blocks.DIRT:Blocks.AIR).defaultBlockState(),3);
        }
        l.getChunkSource().removeTicketWithRadius(PlacementSettings.TICKET,unavailable,3);
        var before=NaturalPlacement.terrain(l,new BlockPos(protectedCp.getMinBlockX()+8,y,protectedCp.getMinBlockZ()+8));long[] waiting={0};
        c.onEachTick(()->{
            // Required area is unavailable by explicit origin revocation, independent of asynchronous ticket unloading.
            if(waiting[0]++==0)for(int x=unavailable.getMinBlockX()-16;x<=unavailable.getMaxBlockX()+16;x++)for(int z=unavailable.getMinBlockZ()-16;z<=unavailable.getMaxBlockZ()+16;z++)NaturalSoil.get(l).invalidate(new BlockPos(x,y-2,z));
            if(waiting[0]<150)return;
            for(var cp:List.of(protectedCp,unsupported,unavailable)){
                var row=NaturalPlacementGameTest.decision(l,cp);c.assertTrue(!NaturalPlacementGameTest.status(l,cp).equals("PLACED")&&queen(l,cp)==null&&row.get("attempts").getAsInt()==0&&!row.has("edits")&&!row.has("insertionCalls"),"No unauthorized installation or partial mutation");
            }
            c.assertTrue(before.equals(NaturalPlacement.terrain(l,new BlockPos(protectedCp.getMinBlockX()+8,y,protectedCp.getMinBlockZ()+8))),"Protected same-state soil stays unchanged");
            evidence("protected_negative",l,protectedCp,new JsonObject());evidence("unsupported_negative",l,unsupported,new JsonObject());evidence("unavailable_negative",l,unavailable,new JsonObject());c.succeed();
        });
    }
    @GameTest(maxTicks=600)
    public void insertionRefusalRetainsBoundedPartialAndNeverRetries(GameTestHelper c){
        var l=NaturalPlacementGameTest.level(c,"placement_retry");var cp=NaturalPlacementGameTest.prepare(c,l,7000,ColonyStage.MATURE);var id=NaturalPlacementGameTest.uuid(l,cp);PlacementFault.block(id);boolean[] checked={false};
        c.onEachTick(()->{
            if(checked[0]||!NaturalPlacementGameTest.status(l,cp).equals("INDETERMINATE"))return;checked[0]=true;PlacementFault.release(id);var row=NaturalPlacementGameTest.decision(l,cp);
            c.assertTrue(row.get("edits").getAsInt()==102&&row.get("blockWrites").getAsInt()==102&&row.get("insertedActors").getAsInt()==0&&queen(l,cp)==null,"Refusal retains declared bounded partial terrain/empty markers, zero actors");
            var home=NestPlan.geometry(BlockPos.of(row.get("surface").getAsLong()),Direction.byName(row.get("direction").getAsString()));
            var retained=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
            for(var at:EstablishedNest.of(home,ColonyStage.MATURE).space())retained.put(at,l.getBlockState(at));for(var at:EstablishedGeneration.surface(home,ColonyStage.MATURE).keySet())retained.put(at,l.getBlockState(at));
            var encoded=NaturalPlacement.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,NaturalPlacement.get(l)).getOrThrow();l.getDataStorage().set(NaturalPlacement.TYPE,NaturalPlacement.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow());
            NaturalPlacementGameTest.reloadChunk(l,cp);NaturalPlacementGameTest.reloadChunk(l,cp);c.runAfterDelay(200,()->{c.assertTrue(queen(l,cp)==null&&NaturalPlacementGameTest.decision(l,cp).get("insertionCalls").getAsInt()==1&&retained.entrySet().stream().allMatch(e->l.getBlockState(e.getKey()).equals(e.getValue())),"Terminal partial state is not resumed, replaced or replenished");evidence("refusal_partial_retained",l,cp,row);c.succeed();});
        });
    }
    @GameTest(maxTicks=500)
    public void missingTickingAreaDefersWithoutMutationOrNeighborAcquisition(GameTestHelper c){
        var l=NaturalPlacementGameTest.level(c,"placement_history");ChunkPos chosen=null;
        for(int x=7200;x<7264&&chosen==null;x++)for(int z=7200;z<7204;z++){var p=new ChunkPos(x,z);if(NaturalPlacement.selected(l,p)&&NaturalPlacement.selectedProfile(l,p)==ColonyStage.MATURE)chosen=p;}
        c.assertTrue(chosen!=null,"Declared mature candidate");var cp=chosen;
        c.assertTrue(l.getChunkSource().getChunk(cp.x(),cp.z(),ChunkStatus.FULL,false)==null,"Genuinely new missing-area candidate");l.getChunkSource().getChunk(cp.x(),cp.z(),ChunkStatus.FULL,true);
        c.assertTrue(NaturalPlacementGameTest.decision(l,cp)!=null&&!NaturalPlacement.ready(l,cp),"Generation authority exists while required ticking area is unavailable");
        var before=NaturalPlacement.terrain(l,new BlockPos(cp.getMinBlockX()+8,l.getMinY()+7,cp.getMinBlockZ()+8));
        c.runAfterDelay(200,()->{var row=NaturalPlacementGameTest.decision(l,cp);c.assertTrue(row.get("status").getAsString().equals("PENDING")&&!row.has("evaluations")&&!row.has("edits")&&queen(l,cp)==null&&!NaturalPlacement.ready(l,cp),"Production defers without trying to acquire the missing ticking area");c.assertTrue(before.equals(NaturalPlacement.terrain(l,new BlockPos(cp.getMinBlockX()+8,l.getMinY()+7,cp.getMinBlockZ()+8))),"Unavailable installation is read-only");evidence("required_ticking_area_unavailable",l,cp,new JsonObject());c.succeed();});
    }
    static void generatedDeath(GameTestHelper c,ServerLevel l,UUID queen){
        var row=JsonParser.parseString(AdultHistory.get(l).records().get(queen.toString())).getAsJsonObject();var n=row.getAsJsonObject("nutrition");
        c.assertTrue(row.get("origin").getAsString().equals("generated_initial:"+EstablishedGeneration.VERSION)&&n.get("generatedInitialSugar").getAsLong()==8000,"Terminal receipt explicitly retains generated base stock provenance");
        c.assertTrue(n.get("sugar").getAsLong()+n.get("spentSugar").getAsLong()+n.get("sugarGiven").getAsLong()==n.get("generatedInitialSugar").getAsLong()+n.get("gainedSugar").getAsLong()+n.get("sugarReceived").getAsLong(),"Generated terminal sugar balances without fabricated ingestion or income");
    }
    @GameTest(maxTicks=600)
    public void interruptedCompletionReadbackAndObservedDeathsNeverReplaceYoung(GameTestHelper c){
        var l=NaturalPlacementGameTest.level(c,"placement_durability_history");var cp=NaturalPlacementGameTest.prepare(c,l,7400,ColonyStage.YOUNG);var id=NaturalPlacementGameTest.uuid(l,cp);PlacementFault.blockWrite(PlacementDurabilityGameTest.file(l),PlacementDurabilityGameTest.key(cp),"PLACED");boolean[] checked={false};
        c.onEachTick(()->{
            if(checked[0]||PlacementFault.writeFailures(PlacementDurabilityGameTest.file(l),PlacementDurabilityGameTest.key(cp),"PLACED")==0)return;checked[0]=true;
            var r=NaturalPlacementGameTest.decision(l,cp);c.assertTrue(r.get("insertedActors").getAsInt()==5&&queen(l,cp)!=null,"Completed actual world insertion precedes the interrupted completion save");
            PlacementDurabilityGameTest.reload(l);PlacementFault.releaseWrite(PlacementDurabilityGameTest.file(l),PlacementDurabilityGameTest.key(cp),"PLACED");c.assertTrue(NaturalPlacementGameTest.status(l,cp).equals("INDETERMINATE"),"Actual RESERVED disk readback becomes terminal uncertainty");
            var q=queen(l,cp);var p=q.founding().plan();var victim=(LasiusNigerEntity)l.getEntity(ColonyMembers.workerId(EstablishedGeneration.lineage(id,0)));victim.hurtServer(l,victim.damageSources().generic(),1000);q.hurtServer(l,q.damageSources().generic(),1000);generatedDeath(c,l,id);
            var food=((NestCache)l.getBlockEntity(p.cache())).contents().stream().map(s->s.getItem().toString()).toList();NaturalPlacementGameTest.reloadChunk(l,cp);NaturalPlacementGameTest.reloadChunk(l,cp);
            c.runAfterDelay(200,()->{c.assertTrue(l.getEntity(id)==null&&l.getEntity(victim.getUUID())==null&&ColonyMembers.get(l).member(victim.getUUID()).dead()&&ColonyMembers.get(l).members(id).size()==4,"Observed queen/worker deaths retain tombstones without replacement");c.assertTrue(food.equals(((NestCache)l.getBlockEntity(p.cache())).contents().stream().map(s->s.getItem().toString()).toList())&&NaturalPlacementGameTest.status(l,cp).equals("INDETERMINATE"),"Uncertain event never replenishes food");evidence("interrupted_saveddata_and_young_deaths",l,cp,new JsonObject());c.succeed();});
        });
    }
    @GameTest(maxTicks=600)
    public void completedMatureDeathsAndRepeatedLoadingNeverMintActorsOrStocks(GameTestHelper c){
        var l=NaturalPlacementGameTest.level(c,"placement_durability_retry");var cp=NaturalPlacementGameTest.prepare(c,l,7600,ColonyStage.MATURE);boolean[] checked={false};
        c.onEachTick(()->{
            if(checked[0]||!NaturalPlacementGameTest.status(l,cp).equals("PLACED"))return;checked[0]=true;physical(c,l,cp,ColonyStage.MATURE);readback(c,l,cp);var q=queen(l,cp);var p=q.founding().plan();var id=q.getUUID();
            var victim=(LasiusNigerEntity)l.getEntity(ColonyMembers.workerId(EstablishedGeneration.lineage(id,0)));victim.hurtServer(l,victim.damageSources().generic(),1000);q.hurtServer(l,q.damageSources().generic(),1000);generatedDeath(c,l,id);
            var cache=(NestCache)l.getBlockEntity(p.cache());int food=cache.size(),clay=MaterialStore.owned(l,id,p).size();NaturalPlacementGameTest.reloadChunk(l,cp);NaturalPlacementGameTest.reloadChunk(l,cp);
            c.runAfterDelay(200,()->{c.assertTrue(l.getEntity(id)==null&&l.getEntity(victim.getUUID())==null&&ColonyMembers.get(l).member(victim.getUUID()).dead()&&ColonyMembers.get(l).members(id).size()==24,"Completed Mature event retains dead/unknown identities");c.assertTrue(NaturalPlacementGameTest.status(l,cp).equals("PLACED")&&NaturalPlacementGameTest.decision(l,cp).get("insertionCalls").getAsInt()==25&&cache.size()==food&&MaterialStore.owned(l,id,p).size()==clay,"Normal loading/death does not repeat insertion or stock initialization");evidence("completed_mature_deaths_no_replacement",l,cp,new JsonObject());c.succeed();});
        });
    }
    @GameTest(maxTicks=600)
    public void midPopulationRefusalRetainsInertPartialWithoutMembershipOrStocks(GameTestHelper c){
        var l=NaturalPlacementGameTest.level(c,"placement_retry");var cp=NaturalPlacementGameTest.prepare(c,l,7800,ColonyStage.MATURE);var id=NaturalPlacementGameTest.uuid(l,cp);var refused=ColonyMembers.workerId(EstablishedGeneration.lineage(id,2));PlacementFault.block(refused);boolean[] checked={false};
        c.onEachTick(()->{
            if(checked[0]||!NaturalPlacementGameTest.status(l,cp).equals("INDETERMINATE"))return;checked[0]=true;PlacementFault.release(refused);var row=NaturalPlacementGameTest.decision(l,cp);
            c.assertTrue(row.get("edits").getAsInt()==102&&row.get("blockWrites").getAsInt()==102&&row.get("insertedActors").getAsInt()==3&&row.get("insertionCalls").getAsInt()==4,"Bounded partial retains queen and two accepted workers, failed third worker");
            var q=queen(l,cp);var p=NestPlan.geometry(BlockPos.of(row.get("surface").getAsLong()),Direction.byName(row.get("direction").getAsString()));
            c.assertTrue(q.isNoAi()&&!q.founding().ready()&&ColonyMembers.get(l).members(id).isEmpty()&&ChamberRegistry.get(l).colony(id)==null&&EstablishedNest.get(l).initial(id)==null,"Partial actors stay inert and unregistered");
            c.assertTrue(((NestCache)l.getBlockEntity(p.cache())).size()==0&&((MaterialStore)l.getBlockEntity(MaterialStore.marker(p,"left"))).size()==0,"Partial markers have no initialized stock");
            l.getDataStorage().saveAndJoin();PlacementDurabilityGameTest.reload(l);NaturalPlacementGameTest.reloadChunk(l,cp);NaturalPlacementGameTest.reloadChunk(l,cp);
            c.runAfterDelay(200,()->{c.assertTrue(NaturalPlacementGameTest.status(l,cp).equals("INDETERMINATE")&&l.getEntity(refused)==null&&queen(l,cp)==q&&q.isNoAi()&&ColonyMembers.get(l).members(id).isEmpty()&&NaturalPlacementGameTest.decision(l,cp).get("insertionCalls").getAsInt()==4,"Normal reload never resumes uncertain population insertion");evidence("mid_population_refusal_inert_partial",l,cp,new JsonObject());c.succeed();});
        });
    }
    @GameTest(maxTicks=500)
    public void legacyPendingWithoutProfileKeepsFoundingAuthorityAndZeroEditInsertion(GameTestHelper c){
        var l=NaturalPlacementGameTest.level(c,"placement_history");var cp=NaturalPlacementGameTest.prepare(c,l,8000,ColonyStage.MATURE);var json=NaturalPlacement.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,NaturalPlacement.get(l)).getOrThrow().getAsJsonObject();String key=Long.toString(cp.pack());var row=JsonParser.parseString(json.get(key).getAsString()).getAsJsonObject();row.remove("profile");row.remove("profileVersion");json.addProperty(key,row.toString());
        l.getDataStorage().set(NaturalPlacement.TYPE,NaturalPlacement.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,json).getOrThrow());
        c.onEachTick(()->{if(!NaturalPlacementGameTest.status(l,cp).equals("PLACED"))return;var q=queen(l,cp);var r=NaturalPlacementGameTest.decision(l,cp);c.assertTrue(!r.has("profile")&&r.get("zeroBlockEdits").getAsBoolean()&&!r.has("editLimit")&&q.founding().phase()!=QueenFounding.Phase.SETTLED,"Legacy pre-profile pending authority remains founding even on a now-Mature deterministic bucket");if(q.founding().removed()>0){evidence("legacy_pending_founding_compatibility",l,cp,new JsonObject());c.succeed();}});
    }
    static void nativeProfile(GameTestHelper c,ColonyStage stage){
        var l=NaturalPlacementGameTest.level(c,"established_native");c.assertTrue(l!=null&&l.getSeed()==0&&l.getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator,"Seed zero with vanilla Overworld noise generator");
        var candidates=new ArrayList<ChunkPos>();for(int x=-32;x<32;x++)for(int z=-32;z<32;z++){var p=new ChunkPos(x,z);if(NaturalPlacement.selected(l,p))candidates.add(p);}
        candidates.sort(Comparator.comparingLong((ChunkPos p)->(long)p.x()*p.x()+(long)p.z()*p.z()).thenComparingInt(ChunkPos::x).thenComparingInt(ChunkPos::z));c.assertTrue(candidates.size()==256,"Frozen bounded 256-query cohort");
        var resolver=l.getChunkSource().getGenerator().getBiomeSource().createCachingResolver(l.getChunkSource().randomState());var selected=new ArrayList<ChunkPos>();var survey=new JsonArray();
        for(var cp:candidates){var biome=resolver.getNoiseBiome(QuartPos.fromBlock(cp.getMinBlockX()+8),QuartPos.fromBlock(80),QuartPos.fromBlock(cp.getMinBlockZ()+8));var row=new JsonObject();row.addProperty("x",cp.x());row.addProperty("z",cp.z());row.addProperty("biome",biome.getRegisteredName());row.addProperty("eligible",biome.is(NaturalPlacement.BIOMES));row.addProperty("profile",NaturalPlacement.selectedProfile(l,cp).serializedName());survey.add(row);if(selected.size()<4&&biome.is(NaturalPlacement.BIOMES)&&NaturalPlacement.selectedProfile(l,cp)==stage)selected.add(cp);}
        c.assertTrue(!selected.isEmpty(),"Declared cohort contains eligible selected profile");var declaration=new JsonObject();declaration.add("survey",survey);declaration.addProperty("queryCount",256);declaration.addProperty("maximumGeneratedSites",4);declaration.addProperty("profile",stage.serializedName());evidence("native_selection_before_generation",l,selected.getFirst(),declaration);
        int[] index={0};boolean[] started={false};ChunkPos[] current={null};var observation=new Observation();
        c.onEachTick(()->{
            if(started[0]){observation.tick(c,l,current[0],stage);return;}
            if(current[0]==null){c.assertTrue(index[0]<selected.size(),"Finite native cohort exhausted for "+stage);var cp=selected.get(index[0]++);c.assertTrue(l.getChunkSource().getChunk(cp.x(),cp.z(),ChunkStatus.FULL,false)==null,"Native candidate genuinely new before tickets");l.getChunkSource().addTicketWithRadius(PlacementSettings.TICKET,cp,3);l.getChunkSource().getChunk(cp.x(),cp.z(),ChunkStatus.FULL,true);current[0]=cp;evidence("native_generated",l,cp,new JsonObject());}
            var cp=current[0];String status=NaturalPlacementGameTest.status(l,cp);
            if(status.equals("REJECTED")){evidence("native_rejected",l,cp,new JsonObject());l.getChunkSource().removeTicketWithRadius(PlacementSettings.TICKET,cp,3);current[0]=null;return;}
            c.assertTrue(!status.equals("INDETERMINATE"),"Native installation uncertainty is not a positive");
            if(status.equals("PLACED")){started[0]=true;observation.tick(c,l,cp,stage);}
        });
    }
    @GameTest(maxTicks=20000)
    public void normalOverworldNoiseYoungGenerationSettlesWithoutContributions(GameTestHelper c){nativeProfile(c,ColonyStage.YOUNG);}
    @GameTest(maxTicks=20000)
    public void normalOverworldNoiseMatureGenerationSettlesWithoutContributions(GameTestHelper c){nativeProfile(c,ColonyStage.MATURE);}
    @GameTest(maxTicks=6000)
    public void genuineRaisedExteriorKeepsValidatedRouteThroughInitializationAndReadback(GameTestHelper c){
        var l=NaturalPlacementGameTest.level(c,"established_route");var cp=NaturalPlacementGameTest.prepare(c,l,8200,ColonyStage.YOUNG);boolean[] checked={false};
        c.onEachTick(()->{if(checked[0]||!NaturalPlacementGameTest.status(l,cp).equals("PLACED"))return;checked[0]=true;var q=queen(l,cp);var p=q.founding().plan();c.assertTrue(p.outside().equals(p.at(-2,0,2))&&NestPlan.walkable(l,p.outside()),"Validated one-block raised exterior must survive initialization as a supported ordinary worker route");readback(c,l,cp);evidence("raised_route_initial_and_readback",l,cp,new JsonObject());});
        settle(c,l,cp,ColonyStage.YOUNG);
    }
}
