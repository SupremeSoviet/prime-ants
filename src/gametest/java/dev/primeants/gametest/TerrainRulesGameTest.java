package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import static dev.primeants.gametest.NaturalPlacementGameTest.*;

/** Fresh isolated generation and actual queen ticks. Never reset a shared fixture ledger. */
public final class TerrainRulesGameTest {
    @GameTest(maxTicks=2000)
    public void generatedVegetationClearsOnlyDeclaredExcavationWithConservedSoil(GameTestHelper c) {
        var l=level(c,"t16_plants");var chunk=prepare(c,l,2000);boolean[] seen={false};
        Map<BlockPos,BlockState> before=new HashMap<>();
        c.onEachTick(()->{
            var actor=l.getEntity(uuid(l,chunk));if(!(actor instanceof LasiusNigerEntity q))return;
            if(!seen[0]) {
                seen[0]=true;c.assertTrue(q.elapsedAgeTicks()==0 && decision(l,chunk).get("zeroBlockEdits").getAsBoolean(),"Insertion makes zero edits before first queen tick");
                var e=BlockPos.of(decision(l,chunk).get("surface").getAsLong());
                for(var b:BlockPos.betweenClosed(e.offset(-7,-4,-7),e.offset(7,3,7))) before.put(b.immutable(),l.getBlockState(b));
                c.assertTrue(NativeVegetation.get(l).eligible(l,e.above()),"Short grass was witnessed during genuine fixture generation");
            }
            c.assertTrue(q.founding().phase()!=QueenFounding.Phase.FAILED,"Physical vegetation founding must keep progressing: "+q.founding().reason());
            var p=q.founding().plan();if(p==null || q.founding().removed()<6)return;
            c.assertTrue(p.plants().size()==3 && q.founding().removedPlants().size()==3 && p.plants().containsAll(q.founding().removedPlants()),"Native short grass, fern and flower cleared only in three declared excavation columns by the living queen");
            var allowed=new HashSet<BlockPos>(p.tasks());allowed.addAll(p.undergroundSurfaces());allowed.addAll(p.deposits());allowed.addAll(p.plants());
            before.forEach((b,s)->c.assertTrue(s.equals(l.getBlockState(b)) || allowed.contains(b) || s.is(Blocks.GRASS_BLOCK) && l.getBlockState(b).is(Blocks.DIRT) && p.deposits().contains(b.above()) && ColonyTerrain.get(l).mound(l,b.above(),q.getUUID()),"No vegetation/lane/mound-site clearing outside excavation: "+b));
            c.assertTrue(q.founding().removed()==q.founding().carried()+q.founding().deposited()+q.founding().released()+q.founding().plugged(),"Plants never add soil or cargo");
            c.assertTrue(q.nutrition().sugar()==0 && q.nutrition().protein()==0,"Plant clearing adds no nutrition");
            var saved=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess());
            c.assertTrue(q.save(saved),"Normal entity save includes declared plant actions and adaptive exterior coordinates");
            var restored=(LasiusNigerEntity)net.minecraft.world.entity.EntityType.loadEntityRecursive(
                net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess(),saved.buildResult()),l,net.minecraft.world.entity.EntitySpawnReason.LOAD,e->e);
            c.assertTrue(restored!=null && restored.getUUID().equals(q.getUUID()) && restored.founding().plan().equals(p)
                && restored.founding().removedPlants().equals(q.founding().removedPlants()) && restored.founding().removed()==q.founding().removed()
                && restored.founding().carried()==q.founding().carried() && restored.bodyReserve()==q.bodyReserve(),"Read-only restored actor preserves plans, plant cursor, reserves, cargo and soil progress; never inserted as a replacement");c.succeed();
        });
    }
    @GameTest(maxTicks=1200)
    public void sameStatePlantReplacementRevokesAuthorityBeforeQueenWork(GameTestHelper c) {
        var l=level(c,"t16_protected");var chunk=prepare(c,l,2100);boolean[] replaced={false};BlockPos[] protectedPlant={null};BlockState[] original={null};
        c.onEachTick(()->{
            var actor=l.getEntity(uuid(l,chunk));if(!(actor instanceof LasiusNigerEntity q))return;
            if(!replaced[0]) {
                replaced[0]=true;var e=BlockPos.of(decision(l,chunk).get("surface").getAsLong());protectedPlant[0]=e.above();original[0]=l.getBlockState(e.above());
                c.assertTrue(q.elapsedAgeTicks()==0 && NativeVegetation.get(l).eligible(l,e.above()),"Replacement intervenes after real zero-edit insertion, before queen work");
                l.setBlock(e.above(),original[0],3);
                c.assertTrue(!NativeVegetation.get(l).eligible(l,e.above()),"Same-state write revokes positive authority");
                c.runAfterDelay(300,()->{
                    c.assertTrue(l.getBlockState(protectedPlant[0]).equals(original[0]) && !q.founding().removedPlants().contains(protectedPlant[0]),"Protected vegetation remains intact through actual queen ticks");
                    c.assertTrue(NaturalSoil.get(l).eligible(l,protectedPlant[0].below()),"Excavation cannot implicitly clear a protected plant by removing its support");c.succeed();
                });
            }
        });
    }
    @GameTest(maxTicks=1500)
    public void replacementAfterPlantClearingCannotBeImplicitlyRemovedWithSoil(GameTestHelper c) {
        var l=level(c,"t16_protected");var chunk=prepare(c,l,2160);boolean[] replaced={false};BlockPos[] plant={null};
        c.onEachTick(()->{
            var actor=l.getEntity(uuid(l,chunk));if(!(actor instanceof LasiusNigerEntity q))return;
            if(!replaced[0] && !q.founding().removedPlants().isEmpty()) {
                replaced[0]=true;plant[0]=q.founding().removedPlants().getFirst();
                c.assertTrue(q.founding().removed()==0,"Plant removal is a separate action before any soil removal");
                l.setBlock(plant[0],q.founding().plan().plantExpected().getFirst(),3);
                c.assertTrue(!NativeVegetation.get(l).eligible(l,plant[0]),"Later replacement cannot regain generation authority");
            }
            if(replaced[0] && q.founding().phase()==QueenFounding.Phase.FAILED) {
                c.assertTrue(q.founding().reason().startsWith("cleared_plant_cell_replaced_at_") && q.founding().removed()==0 && q.founding().carried()==0
                    && l.getBlockState(plant[0]).equals(q.founding().plan().plantExpected().getFirst()) && NaturalSoil.get(l).eligible(l,plant[0].below()),
                    "Protected replacement and its native soil support remain untouched; plants create no soil");c.succeed();
            }
        });
    }
    @GameTest(maxTicks=300)
    public void unknownPlantsTreesCropsAndHistoricalLoadingNeverGrantRemoval(GameTestHelper c) {
        var l=level(c,"t16_unsafe");var chunk=prepare(c,l,2200);PlacementFault.blockFinal(l,chunk);
        int[] offset=NaturalPlacement.searchOffset(l,decision(l,chunk),0);
        var e=NestPlan.soilSurface(l,new BlockPos(chunk.getMinBlockX()+offset[0],0,chunk.getMinBlockZ()+offset[1]));
        for(var block:List.of(Blocks.OAK_LOG,Blocks.OAK_LEAVES,Blocks.OAK_SAPLING,Blocks.WHEAT,Blocks.TALL_GRASS,Blocks.LARGE_FERN))
            c.assertTrue(!NativeVegetation.material(block.defaultBlockState()),"Trees, crops and taller plants are outside authority: "+block);
        l.setBlock(e.above(),Blocks.FERN.defaultBlockState(),3);
        c.assertTrue(!NativeVegetation.get(l).eligible(l,e.above()),"Unknown/player fern has no witness");
        c.assertTrue(NativeVegetation.material(Blocks.WILDFLOWERS.defaultBlockState()) && NativeVegetation.material(Blocks.PINK_PETALS.defaultBlockState()),"Pinned vanilla short flower beds are supported; exact state and witness are still required");
        for(var d:Direction.Plane.HORIZONTAL)c.assertTrue(NestPlan.candidate(l,e,d)==null,"Unknown excavation vegetation refuses every direction");
        reloadChunk(l,chunk);l.getDataStorage().saveAndJoin();
        c.runAfterDelay(30,()->{
            c.assertTrue(l.getBlockState(e.above()).is(Blocks.FERN) && !NativeVegetation.get(l).eligible(l,e.above()) && NaturalSoil.get(l).eligible(l,e),"Historical conversion and real ticks grant no plant permission");
            l.setBlock(e.above(),Blocks.OAK_LOG.defaultBlockState(),3);
            for(var d:Direction.Plane.HORIZONTAL)c.assertTrue(NestPlan.candidate(l,e,d)==null,"Tree blocks refuse excavation without tree clearing");c.succeed();
        });
    }
    @GameTest(maxTicks=10000)
    public void thinNativeSoilKeepsUntouchedMineralChamberFloor(GameTestHelper c) {
        var l=level(c,"t16_thin");var chunk=prepare(c,l,2300);Map<BlockPos,BlockState> floor=new HashMap<>();
        c.onEachTick(()->{
            var actor=l.getEntity(uuid(l,chunk));if(!(actor instanceof LasiusNigerEntity q) || q.founding().plan()==null)return;
            var p=q.founding().plan();if(floor.isEmpty())for(int f=3;f<=5;f++)for(int s=-1;s<=1;s++) {
                var b=p.at(f,s,-3);floor.put(b,l.getBlockState(b));
                c.assertTrue(l.getBlockState(b).is(Blocks.STONE) && NaturalSoil.get(l).floorSupport(l,b) && !NaturalSoil.get(l).eligible(l,b),"Witnessed mineral supports the floor without excavation authority");
            }
            floor.forEach((b,s)->c.assertTrue(l.getBlockState(b).equals(s),"Mineral is never excavated or converted to nest soil"));
            c.assertTrue(q.founding().phase()!=QueenFounding.Phase.FAILED,"Thin soil remains physically valid: "+q.founding().reason());
            if(q.founding().sealed()) {c.assertTrue(q.founding().removed()==24 && q.founding().deposited()==22 && q.founding().plugged()==2 && p.enclosedChamber(l),"Actual supported connected two-high chamber, intact shell and conserved soil");c.succeed();}
        });
    }
    @GameTest(maxTicks=3000)
    public void oneBlockSurfaceStepUsesReachableExteriorStandsAndDeposits(GameTestHelper c) {
        var l=level(c,"t16_step");var chunk=prepare(c,l,2400);boolean[] climbed={false};
        c.onEachTick(()->{
            var actor=l.getEntity(uuid(l,chunk));if(!(actor instanceof LasiusNigerEntity q) || q.founding().plan()==null)return;
            var p=q.founding().plan();
            c.assertTrue(q.founding().phase()!=QueenFounding.Phase.FAILED,"Step founding retains physical checks: "+q.founding().reason());
            if(q.onGround() && q.getY()>=p.entrance().getY()+1.9)climbed[0]=true;
            if(q.founding().deposited()>0 && q.founding().removed()>=4) {
                c.assertTrue(climbed[0] && p.surfaceDeposits().stream().anyMatch(b->b.getY()==p.entrance().getY()+2),"Queen actually climbs the one-block exterior step and physically deposits there");
                c.assertTrue(q.founding().removed()==q.founding().deposited()+q.founding().carried() && p.tasks().size()==24,"Unchanged excavation cap and soil balance");
                var routed=p.routeGeometry();c.assertTrue(routed.outside().equals(p.outside()) && routed.deposits().equals(p.deposits()) && routed.plants().isEmpty(),"Worker routing retains exterior heights without plant-removal authority");
                var task=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess());
                task.putString("Phase","NURSERY");task.store("Entrance",BlockPos.CODEC,p.entrance());task.putString("Direction",p.direction().getName());
                task.store("SurfaceDeposits",BlockPos.CODEC.listOf(),p.surfaceDeposits());task.store("ExteriorStand",BlockPos.CODEC,p.exteriorStand());
                var uninserted=AntEntities.WORKER.create(l,net.minecraft.world.entity.EntitySpawnReason.LOAD);c.assertTrue(uninserted!=null,"Read-only worker restoration factory");
                uninserted.workerTasks().load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess(),task.buildResult()));
                c.assertTrue(uninserted.workerTasks().plan().outside().equals(p.outside()) && uninserted.workerTasks().plan().deposits().equals(p.deposits()),"Saved worker task restoration preserves adaptive exterior; uninserted actor grants no role or replacement");c.succeed();
            }
        });
    }
    @GameTest(maxTicks=300)
    public void unsafeRoofFluidMineralTargetsAndTwoBlockExteriorStepsFailClosed(GameTestHelper c) {
        var l=level(c,"t16_unsafe");var edits=new ArrayList<BlockPos>();var sites=new ArrayList<BlockPos>();
        for(int kind=0;kind<4;kind++) {
            var chunk=prepare(c,l,2500+kind*40);PlacementFault.blockFinal(l,chunk);
            int[] offset=NaturalPlacement.searchOffset(l,decision(l,chunk),0);
            var e=NestPlan.soilSurface(l,new BlockPos(chunk.getMinBlockX()+offset[0],0,chunk.getMinBlockZ()+offset[1]));
            var initial=NestPlan.candidate(l,e,Direction.NORTH);c.assertTrue(initial!=null,"Independent genuine untouched terrain control "+kind);sites.add(e);
            var target=kind==0?initial.tasks().getLast():kind==1?initial.at(4,0,0):kind==2?initial.at(3,2,-1):initial.at(-2,0,1);
            l.setBlock(target,(kind==0 || kind==3?Blocks.STONE:kind==1?Blocks.AIR:Blocks.WATER).defaultBlockState(),3);edits.add(target);
            if(kind==3)l.setBlock(target.above(),Blocks.STONE.defaultBlockState(),3);
            c.assertTrue(NestPlan.candidate(l,e,Direction.NORTH)==null,"Each negative independently refuses: mineral target, exposed roof, fluid, two-block exterior "+kind);
            if(kind==1)c.assertTrue(!initial.enclosedChamber(l),"Enclosure is never waived to admit a slope");
        }
        c.runAfterDelay(30,()->{for(var e:sites)c.assertTrue(NestPlan.candidate(l,e,Direction.NORTH)==null,"Unsafe terrain stays refused through real evaluation ticks");
            c.assertTrue(l.getBlockState(edits.getFirst()).is(Blocks.STONE),"Mineral target remains protected");c.succeed();});
    }
    @GameTest(maxTicks=300)
    public void legacyPendingSearchRestoresOriginalBudgetAndTerminalCannotReopen(GameTestHelper c) {
        var l=level(c,"t16_legacy");var chunk=prepare(c,l,2600);PlacementFault.blockFinal(l,chunk);
        var grid=new HashSet<String>();var fresh=decision(l,chunk);
        for(int i=0;i<64;i++){var a=NaturalPlacement.searchOffset(l,fresh,i);c.assertTrue(a[0]>=4&&a[0]<=11&&a[1]>=4&&a[1]<=11,"Declared grid bounds");grid.add(a[0]+":"+a[1]);}
        c.assertTrue(grid.size()==64 && fresh.get("evaluationLimit").getAsInt()==66 && 64-7>=NaturalPlacement.MIN_SPACING,"Finite wider grid and minimum nearest separation 57");
        var encoded=NaturalPlacement.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,NaturalPlacement.get(l)).getOrThrow().getAsJsonObject();
        var old=JsonParser.parseString(encoded.get(Long.toString(chunk.pack())).getAsString()).getAsJsonObject();
        for(var field:List.of("searchVersion","searchColumns","evaluationLimit","searchMin","searchMax"))old.remove(field);
        encoded.addProperty(Long.toString(chunk.pack()),old.toString());l.getDataStorage().set(NaturalPlacement.TYPE,NaturalPlacement.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow());
        boolean[] restored={false},terminal={false};
        c.onEachTick(()->{
            var r=decision(l,chunk);if(!restored[0] && r.get("column").getAsInt()==4){restored[0]=true;l.getDataStorage().saveAndJoin();PlacementDurabilityGameTest.reload(l);c.assertTrue(decision(l,chunk).get("evaluations").getAsInt()==4,"Actual disk restoration retains old cursor/evaluations");}
            if(terminal[0] || !status(l,chunk).equals("REJECTED"))return;terminal[0]=true;
            c.assertTrue(restored[0] && r.get("searchColumns").getAsInt()==8 && r.get("evaluationLimit").getAsInt()==10 && r.get("evaluations").getAsInt()==8,"Older pending record never receives the larger budget");
            l.getDataStorage().saveAndJoin();PlacementDurabilityGameTest.reload(l);
            c.runAfterDelay(30,()->{c.assertTrue(status(l,chunk).equals("REJECTED") && decision(l,chunk).get("evaluations").getAsInt()==8 && l.getEntity(uuid(l,chunk))==null,"Terminal record cannot reopen or mint an actor");c.succeed();});
        });
    }
    @GameTest(maxTicks=20)
    public void frozenSelectionRecoveryUsesPureImportWithoutBiomeSurvey(GameTestHelper c) throws Exception {
        var file=c.getLevel().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("t16-frozen-recovery.json");
        String text="{\"mode\":\"t15-biome-v1\",\"seed\":42,\"queries\":4096,\"selected\":[{\"x\":26,\"z\":14},{\"x\":-30,\"z\":6},{\"x\":22,\"z\":-22}]}";
        Files.writeString(file,text);var hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
        var first=FrozenPlacementSelection.read(file,hash,42);var recovery=FrozenPlacementSelection.read(file,hash,42);
        c.assertTrue(first.equals(recovery),"Recovery reuses exact frozen evidence; pure import has no world/resolver/survey capability");
        boolean refused=false;try{FrozenPlacementSelection.read(file,hash,43);}catch(IllegalStateException expected){refused=true;}
        c.assertTrue(refused,"Mismatched seed fails closed");c.runAfterDelay(2,c::succeed);
    }
}
