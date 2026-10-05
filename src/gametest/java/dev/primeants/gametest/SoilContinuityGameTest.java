package dev.primeants.gametest;

import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import static dev.primeants.gametest.NaturalPlacementGameTest.*;

/** Actual auto-placed queen ticks; interventions are explicitly negative fixtures. */
public final class SoilContinuityGameTest {
    @GameTest(maxTicks=1500)
    public void actualNaturalWriteCannotExemptNestedOrdinaryWrite(GameTestHelper c){
        c.getLevel().clockManager().setTotalTicks(c.getLevel().dimensionType().defaultClock().orElseThrow(),6000); // Explicit bright-light fixture for real vanilla spread; shared day clock.
        var l=level(c,"placement_soil");var chunk=prepare(c,l,3120);boolean[] changed={false};
        c.onEachTick(()->{
            if(!(l.getEntity(uuid(l,chunk)) instanceof LasiusNigerEntity q)||q.founding().removed()!=2||changed[0])return;
            changed[0]=true;var p=q.founding().plan();var target=p.tasks().get(2);var ordinary=p.at(1,2,-1);
            c.assertTrue(NaturalSoil.get(l).eligible(l,target)&&NaturalSoil.get(l).eligible(l,ordinary),"Both cells initially authorized");
            GrassNestedWriteProbe.watch(l,target,ordinary);spread(l,p.at(1,1,0),target);
            c.assertTrue(l.getBlockState(target).is(Blocks.GRASS_BLOCK)&&NaturalSoil.get(l).eligible(l,target)&&!NaturalSoil.get(l).eligible(l,ordinary),"Exact real spread survives; identical ordinary nested write has no exemption");reloadSoil(l);
            c.assertTrue(NaturalSoil.get(l).eligible(l,target)&&!NaturalSoil.get(l).eligible(l,ordinary),"Disk retains exact-write distinction");c.succeed();
        });
    }
    @GameTest(maxTicks=1500)
    public void latePlantRefusalRetainsExistingCargoThroughRestoration(GameTestHelper c){
        var l=level(c,"placement_soil");var chunk=prepare(c,l,3080);boolean[] placed={false},saved={false};
        c.onEachTick(()->{
            if(!(l.getEntity(uuid(l,chunk)) instanceof LasiusNigerEntity q)||q.founding().plan()==null||saved[0])return;
            if(!placed[0]&&q.founding().removed()==2){placed[0]=true;var target=q.founding().plan().tasks().get(2);l.setBlock(target.above(),Blocks.FERN.defaultBlockState(),3);}
            if(placed[0]&&q.founding().phase()==QueenFounding.Phase.FAILED){
                saved[0]=true;c.assertTrue(q.founding().reason().startsWith("protected_vegetation_support_at_")&&q.founding().removed()==2&&q.founding().carried()==2,"Refusal retains existing physical cargo and progress");reloadSoil(l);
                var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess());c.assertTrue(q.save(out),"Failed queen and canonical equipment save");
                var copy=(LasiusNigerEntity)net.minecraft.world.entity.EntityType.loadEntityRecursive(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess(),out.buildResult()),l,net.minecraft.world.entity.EntitySpawnReason.LOAD,e->e);
                c.assertTrue(copy!=null&&copy.founding().phase()==QueenFounding.Phase.FAILED&&copy.founding().removed()==2&&copy.founding().carried()==2&&copy.founding().removed()==copy.founding().carried()+copy.founding().deposited()+copy.founding().released()+copy.founding().plugged(),"Restoration neither revives failed queen nor loses its soil; copy never inserted");c.succeed();
            }
        });
    }
    static void reloadSoil(net.minecraft.server.level.ServerLevel l) {
        l.getDataStorage().saveAndJoin();
        try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(
            net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),
            net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())) {
            var data=disk.get(NaturalSoil.TYPE);if(data==null)throw new AssertionError("Actual soil disk data required");l.getDataStorage().set(NaturalSoil.TYPE,data);
        }
    }
    static void spread(net.minecraft.server.level.ServerLevel l,BlockPos source,BlockPos target) {
        // Controlled RNG invokes the real inherited vanilla randomTick, never a labelled synthetic write.
        int[] draws={target.getX()-source.getX()+1,target.getY()-source.getY()+3,target.getZ()-source.getZ()+1};
        l.getBlockState(source).randomTick(l,source,new net.minecraft.world.level.levelgen.LegacyRandomSource(17){
            int index;@Override public int nextInt(int bound){int result=draws[index++%3];if(result<0||result>=bound)throw new AssertionError("Spread draw bounds");return result;}
        });
    }
    @GameTest(maxTicks=1500)
    public void actualVanillaSpreadRetainsPendingAuthorityAndQueenContinues(GameTestHelper c) {
        c.getLevel().clockManager().setTotalTicks(c.getLevel().dimensionType().defaultClock().orElseThrow(),6000);
        var l=level(c,"placement_soil");var chunk=prepare(c,l,2880);boolean[] changed={false};
        c.onEachTick(()->{
            if(!(l.getEntity(uuid(l,chunk)) instanceof LasiusNigerEntity q) || q.founding().plan()==null)return;
            if(!changed[0] && q.founding().removed()==2) {
                changed[0]=true;var p=q.founding().plan();var target=p.tasks().get(2);var source=p.at(1,1,0);
                c.assertTrue(l.getBlockState(target).is(Blocks.DIRT) && NaturalSoil.get(l).eligible(l,target) && l.getBlockState(source).is(Blocks.GRASS_BLOCK),"Pending generated dirt plus actual vanilla grass source");
                spread(l,source,target);
                c.assertTrue(l.getBlockState(target).is(Blocks.GRASS_BLOCK) && NaturalSoil.get(l).eligible(l,target),"Actual spread successful with pre-existing authority");
                reloadSoil(l);c.assertTrue(NaturalSoil.get(l).compatible(l,target,p.expected().get(2)),"Disk provenance keeps current grass compatible with saved dirt target");
            }
            if(changed[0] && q.founding().removed()>3){c.assertTrue(q.founding().phase()!=QueenFounding.Phase.FAILED,"Actual queen work continues after natural spread");c.succeed();}
        });
    }
    @GameTest(maxTicks=1500)
    public void actualVanillaDecayRetainsAuthorityAndQueenContinues(GameTestHelper c) {
        var l=level(c,"placement_soil");var chunk=prepare(c,l,2920);boolean[] changed={false};
        c.onEachTick(()->{
            if(!(l.getEntity(uuid(l,chunk)) instanceof LasiusNigerEntity q) || q.founding().plan()==null)return;
            if(!changed[0]) {
                changed[0]=true;var target=q.founding().plan().tasks().getFirst();
                c.assertTrue(NaturalSoil.get(l).eligible(l,target) && l.getBlockState(target).is(Blocks.GRASS_BLOCK),"Generated pending grass before decay");
                l.setBlock(target.above(),Blocks.STONE.defaultBlockState(),3);
                l.getBlockState(target).randomTick(l,target,net.minecraft.util.RandomSource.create(17));
                c.assertTrue(l.getBlockState(target).is(Blocks.DIRT) && NaturalSoil.get(l).eligible(l,target),"Actual vanilla blocked-light decay retains observation");
                l.setBlock(target.above(),Blocks.AIR.defaultBlockState(),3);reloadSoil(l);
            }
            if(q.founding().removed()>2){c.assertTrue(q.founding().phase()!=QueenFounding.Phase.FAILED,"Queen excavates real naturally decayed dirt after restoration");c.succeed();}
        });
    }
    @GameTest(maxTicks=1500)
    public void identicalOrdinaryGrassWriteRevokesAndFailedQueenStaysFailedOnRestore(GameTestHelper c) { ordinary(c,2960,false); }
    @GameTest(maxTicks=1500)
    public void identicalOrdinaryDirtWriteAfterNaturalDecayStillRevokes(GameTestHelper c) { ordinary(c,3000,true); }
    private void ordinary(GameTestHelper c,int base,boolean dirt) {
        var l=level(c,"placement_soil");var chunk=prepare(c,l,base);boolean[] changed={false};
        c.onEachTick(()->{
            if(!(l.getEntity(uuid(l,chunk)) instanceof LasiusNigerEntity q) || q.founding().plan()==null)return;
            var target=q.founding().plan().tasks().getFirst();
            if(!changed[0]) {
                changed[0]=true;
                if(dirt){l.setBlock(target.above(),Blocks.STONE.defaultBlockState(),3);l.getBlockState(target).randomTick(l,target,net.minecraft.util.RandomSource.create(17));l.setBlock(target.above(),Blocks.AIR.defaultBlockState(),3);}
                c.assertTrue(NaturalSoil.get(l).eligible(l,target),"Valid authority before identical ordinary entity/player write");
                l.setBlock(target,l.getBlockState(target),3);c.assertTrue(!NaturalSoil.get(l).eligible(l,target),"Even unsuccessful same-state write revokes");reloadSoil(l);
            }
            if(q.founding().phase()==QueenFounding.Phase.FAILED){
                c.assertTrue(q.founding().removed()==0 && !NaturalSoil.get(l).eligible(l,target),"Real queen refuses identically replaced soil through disk reload");
                var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess());c.assertTrue(q.save(out),"Failed queen save");
                var restored=(LasiusNigerEntity)net.minecraft.world.entity.EntityType.loadEntityRecursive(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess(),out.buildResult()),l,net.minecraft.world.entity.EntitySpawnReason.LOAD,e->e);
                c.assertTrue(restored!=null && restored.founding().phase()==QueenFounding.Phase.FAILED && restored.founding().removed()==0,"Restoration does not repair historical failed queen; copy is never inserted");c.succeed();
            }
        });
    }
    @GameTest(maxTicks=1500)
    public void laterRealSpreadCannotReviveRevokedOrUnknownSoil(GameTestHelper c) {
        c.getLevel().clockManager().setTotalTicks(c.getLevel().dimensionType().defaultClock().orElseThrow(),6000);
        var l=level(c,"placement_soil");var chunk=prepare(c,l,3040);boolean[] changed={false};
        c.onEachTick(()->{
            if(!(l.getEntity(uuid(l,chunk)) instanceof LasiusNigerEntity q) || q.founding().removed()!=2 || changed[0])return;
            changed[0]=true;var p=q.founding().plan();var target=p.tasks().get(2);var unknown=p.at(1,2,-1);var source=p.at(1,1,0);
            // Explicit negative fixture exposure: vanilla cannot spread under an opaque grass cap.
            // Do not edit/grant the tested dirt; the queen's pending target was already exposed by her own work.
            l.setBlock(unknown.above(),Blocks.AIR.defaultBlockState(),3);
            c.assertTrue(NaturalSoil.get(l).eligible(l,unknown),"Removing only the cap retains the dirt's original generation observation before the denied-cell fixture");
            l.setBlock(target,l.getBlockState(target),3);NaturalSoil.get(l).invalidate(unknown);
            spread(l,source,target);spread(l,source,unknown);
            c.assertTrue(l.getBlockState(target).is(Blocks.GRASS_BLOCK) && l.getBlockState(unknown).is(Blocks.GRASS_BLOCK),"Actual spread succeeds even for denied cells");
            reloadSoil(l);c.assertTrue(!NaturalSoil.get(l).eligible(l,target) && !NaturalSoil.get(l).eligible(l,unknown),"Revoked and unknown records remain denied after real spread and disk restore");
            c.runAfterDelay(100,()->{c.assertTrue(q.founding().phase()==QueenFounding.Phase.FAILED && q.founding().removed()==2 && q.founding().carried()==2,"Continued actual excavation refuses revoked cell and retains prior cargo");c.succeed();});
        });
    }
    @GameTest(maxTicks=1500)
    public void latePlantOnInitiallyBarePendingSoilKeepsPlantAndSupport(GameTestHelper c) {
        var l=level(c,"placement_soil");var chunk=prepare(c,l,2800);boolean[] placed={false};
        c.onEachTick(()->{
            if(!(l.getEntity(uuid(l,chunk)) instanceof LasiusNigerEntity q) || q.founding().plan()==null || placed[0])return;
            placed[0]=true;var p=q.founding().plan();var soil=p.tasks().getFirst();var plant=soil.above();
            c.assertTrue(l.getBlockState(plant).isAir() && !p.plants().contains(plant),"Originally bare upper cell is absent from saved plants");
            var support=l.getBlockState(soil);l.setBlock(plant,Blocks.FERN.defaultBlockState(),3);
            c.assertTrue(NaturalSoil.get(l).eligible(l,soil),"Plant write does not revoke underlying native soil authority");
            c.runAfterDelay(300,()->{
                c.assertTrue(l.getBlockState(plant).is(Blocks.FERN) && l.getBlockState(soil).equals(support),"Late protected plant AND support survive real queen ticks");
                c.assertTrue(q.founding().phase()==QueenFounding.Phase.FAILED && q.founding().reason().startsWith("protected_vegetation_support_at_")
                    && q.founding().removed()==0 && q.founding().carried()==0,"Explicit refusal conserves work and material");c.succeed();
            });
        });
    }
    @GameTest(maxTicks=1500)
    public void loweredPlantOutsideExcavationSurvivesNeighborSoilPreparation(GameTestHelper c) {
        var l=level(c,"placement_soil");var chunk=prepare(c,l,2840);boolean[] placed={false};
        c.onEachTick(()->{
            if(!(l.getEntity(uuid(l,chunk)) instanceof LasiusNigerEntity q) || q.founding().plan()==null || placed[0])return;
            placed[0]=true;var p=q.founding().plan();var support=p.at(1,1,-1);var plant=support.above();
            c.assertTrue(!p.tasks().contains(support) && p.undergroundSurfaces().contains(support),"Lowered support is beside stair, outside all excavation columns");
            var soil=l.getBlockState(support);l.setBlock(plant,Blocks.AIR.defaultBlockState(),3);l.setBlock(plant,Blocks.POPPY.defaultBlockState(),3);
            c.assertTrue(NaturalSoil.get(l).eligible(l,support) && l.getBlockState(plant).canSurvive(l,plant),"Controlled lowered plant has still-authorized support");
            c.runAfterDelay(400,()->{
                c.assertTrue(q.founding().removed()>=3,"Queen actually excavated the neighboring stair");
                c.assertTrue(l.getBlockState(plant).is(Blocks.POPPY) && NaturalSoil.get(l).compatible(l,support,soil),"Preparation preserves lowered plant AND authoritative support with normal neighbor updates: plant="+l.getBlockState(plant)+" support="+l.getBlockState(support)+" old="+soil);
                c.assertTrue(!ColonyTerrain.get(l).prepare(l,support,q.getUUID()),"Central preparation guard also refuses worker call path");
                c.assertTrue(ColonyTerrain.get(l).preparationProblem(l,support).startsWith("protected_vegetation_support_at_"),"Central refusal exposes explicit live reason");
                c.assertTrue(q.founding().removed()==q.founding().carried()+q.founding().deposited()+q.founding().plugged()+q.founding().released(),"Plant refusal adds no soil");c.succeed();
            });
        });
    }
}
