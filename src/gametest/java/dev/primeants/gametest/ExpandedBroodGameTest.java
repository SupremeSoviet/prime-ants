package dev.primeants.gametest;

import com.mojang.serialization.Codec;
import dev.primeants.PrimeAnts;
import dev.primeants.brood.*;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** b2 is deterministic serialization coverage. It does not establish natural expansion or physical upgrade work. */
public final class ExpandedBroodGameTest {
    @GameTest(maxTicks=5000,structure="prime_ants_test:idle_ground")
    public void deterministicExpandedBroodDisplaysAndRestoresEveryRecordSlotClockAndFractionalStep(GameTestHelper c) {
        var l = c.getLevel();
        new QueenFoundingGameTest().terrain(c,Blocks.DIRT.defaultBlockState(),true,true);
        var plan = NestPlan.geometry(c.absolutePos(new BlockPos(7,4,4)),Direction.SOUTH);
        for (var at : plan.tasks()) l.setBlock(at,Blocks.AIR.defaultBlockState(),3);
        for (var at : plan.plugs()) l.setBlock(at,NurseryBlocks.NEST_SOIL.defaultBlockState(),3);
        var q = AntEntities.QUEEN.create(l,EntitySpawnReason.LOAD); c.assertTrue(q != null,"Deterministic queen body");
        q.setPos(Vec3.atBottomCenterOf(plan.chamber()).add(0.2,0,0)); c.assertTrue(l.addFreshEntity(q),"Fixture queen inserted once");
        c.assertTrue(q.spendReserve(BroodPile.MAX_RESERVE),"Deterministic saved history: the original clutch used its finite reserve");
        var nutrition=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());
        nutrition.putLong("Apples",2);nutrition.putLong("Chickens",2);nutrition.putLong("Sugar",3000);nutrition.putLong("Protein",6000);
        nutrition.putLong("SpentSugar",5000);nutrition.putLong("SpentProtein",10000);
        q.nutrition().load(TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),nutrition.buildResult()),8000,16000);
        var f = TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());
        f.putString("Phase","SETTLED"); f.putString("Lifecycle","CLAUSTRAL"); f.putInt("Progress",24); f.putInt("Deposited",22); f.putInt("Plugged",2);
        f.store("Entrance",BlockPos.CODEC,plan.entrance()); f.putString("Direction","south");
        f.store("Tasks",BlockPos.CODEC.listOf(),plan.tasks()); f.store("Expected",net.minecraft.world.level.block.state.BlockState.CODEC.listOf(),Collections.nCopies(24,Blocks.DIRT.defaultBlockState()));
        q.founding().load(TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),f.buildResult()));
        var terrain = com.mojang.serialization.JsonOps.INSTANCE;
        var records = ColonyTerrain.CODEC.encodeStart(terrain,ColonyTerrain.get(l)).getOrThrow().getAsJsonObject();
        for (var at : NurseryUpgradeGameTest.walls(plan)) {
            l.setBlock(at,NurseryBlocks.PACKED_CLAY.defaultBlockState(),3);
            records.addProperty(Long.toString(at.asLong()),q.getUUID()+":built:prime_ants:packed_clay");
        }
        l.getDataStorage().set(ColonyTerrain.TYPE,ColonyTerrain.CODEC.parse(terrain,records).getOrThrow());
        l.setBlock(plan.nursery(),NurseryBlocks.BROOD_PILE.defaultBlockState(),3);
        var seed = (BroodPile)l.getBlockEntity(plan.nursery()); var tag = seed.saveWithFullMetadata(l.registryAccess());
        var data = TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());
        data.putString("Queen",q.getUUID().toString()); data.store("Entrance",BlockPos.CODEC,plan.entrance()); data.putString("Direction","south");
        data.putLong("LoadedTicks",100); data.putLong("LastLayingTick",17); data.putLong("StageDuration",120); data.putInt("DevelopmentCredit",1);
        var ids = java.util.stream.IntStream.range(0,5).mapToObj(i -> UUID.randomUUID()).toList();
        var original=java.util.stream.IntStream.range(0,3).mapToObj(i->UUID.randomUUID().toString()).toList();
        data.store("Original",Codec.STRING.listOf(),original);data.store("Consumed",Codec.STRING.listOf(),original);
        var rows = data.childrenList("Brood"); int[] slots = {0,1,2,3,5}; long[] progress = {10,10,119,20,40};
        for (int i=0;i<5;i++) {
            var row = rows.addChild(); row.putString("Id",ids.get(i).toString()); row.putString("Queen",q.getUUID().toString()); row.putInt("Slot",slots[i]);
            row.putString("Stage",i==1?"COCOON":"EGG"); row.putLong("Progress",progress[i]); row.putLong("Nourishment",i==1?12000:0); row.putBoolean("Founding",false);
            if(i==1){var meal=row.child("Nutrition");meal.putLong("Apples",1);meal.putLong("Chickens",1);meal.putLong("SpentSugar",4000);meal.putLong("SpentProtein",8000);}
        }
        tag.merge(data.buildResult()); restore(c,seed,tag);
        long[] loadedAt = {-1}, before = {-1}; UUID egg = ids.getFirst();
        c.onEachTick(() -> {
            var p = (BroodPile)l.getBlockEntity(plan.nursery()); var saved = p.saveWithFullMetadata(l.registryAccess());
            if (loadedAt[0] >= 0) {
                if (p.loadedTicks() == loadedAt[0]) return;
                var r = p.records().stream().filter(b -> b.id().equals(egg)).findFirst().orElseThrow();
                c.assertTrue(p.loadedTicks()==loadedAt[0]+1 && r.stage()==BroodStage.EGG && r.progress()==before[0]+2 && saved.getIntOr("DevelopmentCredit",-1)==0
                    && p.nursery().speed()==3,"First resumed production tick spends the saved fractional credit: " + before[0]+" -> "+r.progress());
                PrimeAnts.LOGGER.info("T08 B2 DETERMINISTIC RESTORED STEP tick={} records={} loadedAt={} egg={}->{} credit=0",c.getTick(),p.records().size(),loadedAt[0],before[0],r.progress());
                c.succeed(); return;
            }
            if (p.nursery().speed()!=3 || saved.getIntOr("DevelopmentCredit",-1)!=1 || p.records().get(2).stage()!=BroodStage.LARVA || p.records().getFirst().neglectTicks()!=0) return;
            var state = l.getBlockState(plan.nursery());
            c.assertTrue(p.records().size()==5 && state.getValue(BroodPileBlock.A)==BroodStage.EGG && state.getValue(BroodPileBlock.B)==BroodStage.COCOON
                && state.getValue(BroodPileBlock.C)==BroodStage.LARVA && state.getValue(BroodPileBlock.MORE)==BroodPileBlock.more(2),"Five differing slots/stages have the complete aggregate display: " + state);
            var back = restore(c,p,saved); var again = back.saveWithFullMetadata(l.registryAccess());
            c.assertTrue(again.get("Brood").equals(saved.get("Brood")) && back.records().stream().map(r->r.id()+"@"+r.slot()+":"+r.stage()+":"+r.progress()).toList()
                .equals(p.records().stream().map(r->r.id()+"@"+r.slot()+":"+r.stage()+":"+r.progress()).toList()),"Every record and slot is preserved, including all nutrition/survival fields");
            for (var key : List.of("LoadedTicks","LastLayingTick","StageDuration","DevelopmentCredit","Original","Consumed","GrowthFlow"))
                c.assertTrue(Objects.equals(again.get(key),saved.get(key)),"Exact clock, history and credit restore: " + key);
            c.assertTrue(back.getBlockState().equals(state),"Complete display survives normal block-entity restore");
            loadedAt[0] = back.loadedTicks(); before[0] = back.records().getFirst().progress();
            PrimeAnts.LOGGER.info("T08 B2 DETERMINISTIC RELOAD tick={} slots={} credit=1 records={}",c.getTick(),back.records().stream().map(BroodRecord::slot).toList(),again.get("Brood"));
        });
    }
    private static BroodPile restore(GameTestHelper c,BroodPile p,CompoundTag tag) {
        var l=c.getLevel(); var at=p.getBlockPos(); var state=p.getBlockState(); l.removeBlockEntity(at);
        var back=(BroodPile)BlockEntity.loadStatic(at,state,tag,l.registryAccess()); c.assertTrue(back!=null,"Normal expanded pile serialization"); l.setBlockEntity(back); return back;
    }
}
