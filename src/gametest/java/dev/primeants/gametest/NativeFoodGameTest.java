package dev.primeants.gametest;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import dev.primeants.brood.*;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.item.AntItems;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.phys.*;
import static dev.primeants.gametest.NaturalPlacementGameTest.*;
import static dev.primeants.gametest.NectarGameTest.*;

/** Controlled generated-cover fixtures, real founding/emergence/action/feeding ticks. */
public final class NativeFoodGameTest {
    private static net.minecraft.world.level.ChunkPos setup(GameTestHelper c,ServerLevel l,int base){
        var p=prepare(c,l,base);l.getChunkSource().addTicketWithRadius(PlacementSettings.TICKET,p,3);return p;
    }
    private static BlockPos cover(ServerLevel l,LasiusNigerEntity q){
        var o=q.founding().plan().outside();
        for(var p:BlockPos.betweenClosed(o.offset(-24,-3,-24),o.offset(24,3,24)))if(NestPlan.loaded(l,p)&&NativePrey.habitat(l,p))return p.immutable();
        throw new AssertionError("Witnessed generated short cover required");
    }
    private static long ingested(ServerLevel l,LasiusNigerEntity q){
        var b=pile(l,q);long n=q.nutrition().prey()+(b==null?0:b.consumedPrey());
        for(var e:l.getAllEntities())if(e instanceof LasiusNigerEntity w&&w.isAlive()&&q.getUUID().equals(w.queenId()))n+=w.nutrition().prey();
        for(var r:AdultHistory.get(l).records().values()){var row=JsonParser.parseString(r).getAsJsonObject();var v=row.getAsJsonObject("nutrition");if(row.get("queen").getAsString().equals(q.getUUID().toString())&&v.has("prey"))n+=v.get("prey").getAsLong();}return n;
    }
    private static long stock(ServerLevel l,LasiusNigerEntity q){
        var n=cache(l,q);var box=new AABB(q.founding().plan().outside()).inflate(32,6,32);
        return workers(l,q).stream().filter(w->w.getMainHandItem().is(AntItems.SMALL_PREY)).mapToInt(w->w.getMainHandItem().getCount()).sum()
            +(n==null?0:n.contents().stream().filter(s->s.is(AntItems.SMALL_PREY)).mapToInt(ItemStack::getCount).sum())
            +l.getEntitiesOfClass(ItemEntity.class,box,i->i.isAlive()&&i.getItem().is(AntItems.SMALL_PREY)).stream().mapToInt(i->i.getItem().getCount()).sum()
            +TransferCustody.get(l).contents().stream().filter(t->box.contains(t.position())&&t.stack().is(AntItems.SMALL_PREY)).mapToInt(t->t.stack().getCount()).sum();
    }
    private static long harvested(ServerLevel l,LasiusNigerEntity q){var box=new AABB(q.founding().plan().outside()).inflate(24,3,24);return NativePrey.get(l).harvestedSources().entrySet().stream().filter(e->box.contains(Vec3.atCenterOf(e.getKey()))).mapToLong(Map.Entry::getValue).sum();}
    private static void conserve(GameTestHelper c,ServerLevel l,LasiusNigerEntity q){c.assertTrue(harvested(l,q)==stock(l,q)+ingested(l,q),"Physical prey + real terminal ingestion equals harvests; source readiness is no food");}
    @GameTest(maxTicks=1000)
    public void diagnosticRadiusFourActuallyEntityTicksWholeEnlargedBox(GameTestHelper c){
        var l=level(c,"placement_soil");var cp=prepare(c,l,4240);l.getChunkSource().addTicketWithRadius(PlacementSettings.TICKET,cp,4);
        c.onEachTick(()->{
            if(!(l.getEntity(uuid(l,cp)) instanceof LasiusNigerEntity q)||q.founding().plan()==null)return;
            var o=q.founding().plan().outside();
            for(int x=-24;x<=24;x+=8)for(int z=-24;z<=24;z+=8)if(!NestPlan.loaded(l,o.offset(x,0,z))||!l.isPositionEntityTicking(o.offset(x,0,z)))return;
            c.assertTrue(true,"Radius-four diagnostic ticket covers every chunk intersecting enlarged 49x49 box with real entity ticking");c.succeed();
        });
    }
    @GameTest(maxTicks=24000)
    public void genuineCoverPreyAndFarNectarTransportFundBothRecipientsAndNewBrood(GameTestHelper c){
        var l=level(c,"t20_food");var cp=setup(c,l,4000);boolean[] carried={false},stored={false},far={false};var actions=new HashMap<UUID,Long>();
        c.onEachTick(()->{
            if(!(l.getEntity(uuid(l,cp)) instanceof LasiusNigerEntity q)||q.founding().plan()==null)return;conserve(c,l,q);
            for(var w:workers(l,q)){
                if(w.workerTasks().harvestingTicks()>0)actions.putIfAbsent(w.getUUID(),w.elapsedAgeTicks());
                if(w.getMainHandItem().is(AntItems.SMALL_PREY)&&!w.workerTasks().nursing()){
                    carried[0]=true;c.assertTrue(!w.isCallow()&&pile(l,q).original().contains(w.broodId())&&w.getMainHandItem().getCount()==1&&w.elapsedAgeTicks()-actions.get(w.getUUID())>=19,"Genuine mature worker finishes 20 loaded action ticks for one canonical unit");
                }
                var src=w.workerTasks().flowerSource();if(src!=null&&FlowerNectar.habitat(l,src))far[0]|=Math.abs(src.getX()-q.founding().plan().outside().getX())>10;
            }
            var n=cache(l,q);stored[0]|=n!=null&&n.contents().stream().anyMatch(s->s.is(AntItems.SMALL_PREY));
            var b=pile(l,q);
            if(b!=null&&b.records().stream().anyMatch(r->!r.founding()&&r.nutrition().prey()>0&&r.nutrition().nectarV2()>0&&r.nourishment()>0)){
                c.assertTrue(carried[0]&&stored[0]&&far[0]&&ingested(l,q)>0&&q.nutrition().nectarV2()>0,"Native protein, far-range sweetness and new brood are actually ingested after carry/cache/nursing");
                c.assertTrue(NativeVegetation.get(l).eligible(l,cover(l,q)),"Harvest leaves native cover intact");c.succeed();
            }
        });
    }
    @GameTest(maxTicks=18000)
    public void preyCooldownDiskRestoreAndSameStateReplacementKeepTombstone(GameTestHelper c){
        var l=level(c,"t20_food");var cp=setup(c,l,4040);boolean[] done={false};
        c.onEachTick(()->{
            if(done[0]||!(l.getEntity(uuid(l,cp)) instanceof LasiusNigerEntity q)||q.founding().plan()==null)return;conserve(c,l,q);
            if(harvested(l,q)==1){
                done[0]=true;var p=cover(l,q);int before=NativePrey.get(l).remaining(p);l.getDataStorage().saveAndJoin();
                try(var disk=new SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){
                    var data=disk.get(NativePrey.TYPE);c.assertTrue(data!=null&&data.remaining(p)==before&&data.harvests()==NativePrey.get(l).harvests(),"Actual disk source restoration keeps exact cooldown and portions");l.getDataStorage().set(NativePrey.TYPE,data);
                }
                l.setBlock(p,l.getBlockState(p),3);
                c.assertTrue(!NativeVegetation.get(l).eligible(l,p)&&NativePrey.get(l).remaining(p)==1200&&!NativePrey.get(l).ready(l,p),"Even identical replacement revokes native authority and retains cooldown tombstone");
                c.runAfterDelay(1300,()->{conserve(c,l,q);c.assertTrue(harvested(l,q)==1&&NativePrey.get(l).remaining(p)==1200,"Unavailable/replaced source cannot accrue offline/loaded readiness or extra food");c.succeed();});
            }
        });
    }
    @GameTest(maxTicks=18000)
    public void changedCoverDuringActionCannotYieldPrey(GameTestHelper c){negative(c,4080,false);}
    @GameTest(maxTicks=18000)
    public void genuineBarrierPreventsRemoteCoverHarvestAndDroppedFoodPickup(GameTestHelper c){negative(c,4120,true);}
    private void negative(GameTestHelper c,int base,boolean barrier){
        var l=level(c,"t20_food");var cp=setup(c,l,base);boolean[] done={false};
        c.onEachTick(()->{
            if(done[0]||!(l.getEntity(uuid(l,cp)) instanceof LasiusNigerEntity q)||q.founding().plan()==null)return;
            for(var w:workers(l,q))if(barrier?w.workerTasks().phase()==WorkerTasks.Phase.SEARCH:w.workerTasks().harvestingTicks()>=5&&NativePrey.flower(l.getBlockState(w.workerTasks().flowerSource()))){
                done[0]=true;var p=cover(l,q);c.assertTrue(harvested(l,q)==0,"Negative intervention before any prey harvest");
                if(barrier){
                    for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=0;y<=3;y++)if(x!=0||z!=0||y==3)l.setBlock(p.offset(x,y,z),Blocks.STONE.defaultBlockState(),3);
                    var drop=new ItemEntity(l,p.getX()+.5,p.getY(),p.getZ()+.5,new ItemStack(Items.CHICKEN));drop.setPickUpDelay(0);l.addFreshEntity(drop);
                    c.runAfterDelay(500,()->{c.assertTrue(drop.isAlive()&&drop.getItem().getCount()==1&&harvested(l,q)==0&&ingested(l,q)==0,"Real solid barrier prevents remote native harvesting and ordinary pickup");c.succeed();});
                }else{
                    l.setBlock(p,Blocks.AIR.defaultBlockState(),3);c.runAfterDelay(300,()->{c.assertTrue(harvested(l,q)==0&&stock(l,q)==0&&ingested(l,q)==0,"Source changed/unavailable mid-action yields nothing");c.succeed();});
                }return;
            }
        });
    }
    @GameTest(maxTicks=18000)
    public void preyFullStorageRestorationAndDeathConserveCanonicalCargo(GameTestHelper c){
        var l=level(c,"t20_food");var chunk=setup(c,l,4160);boolean[] filled={false},killed={false};
        c.onEachTick(()->{
            if(!(l.getEntity(uuid(l,chunk)) instanceof LasiusNigerEntity q)||q.founding().plan()==null||killed[0])return;conserve(c,l,q);
            var ws=workers(l,q);var p=q.founding().plan();
            if(!filled[0]&&ws.stream().anyMatch(w->w.getMainHandItem().is(AntItems.SMALL_PREY))){
                filled[0]=true;ws.stream().filter(w->!q.founding().claimedBy(w)).forEach(w->w.setNoAi(true));
                // Explicit negative full-storage fixture: six physical chicken stacks, separate from nectar accounting.
                l.setBlock(p.cache(),dev.primeants.brood.NurseryBlocks.NEST_CACHE.defaultBlockState(),3);var n=cache(l,q);
                var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess());
                out.putString("Colony",q.getUUID().toString());out.store("Entrance",BlockPos.CODEC,p.entrance());out.putString("Direction",p.direction().getName());
                out.store("Contents",ItemStack.CODEC.listOf(),java.util.stream.IntStream.range(0,6).mapToObj(i->new ItemStack(Items.CHICKEN)).toList());
                n.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess(),out.buildResult()));n.setChanged();
            }
            if(filled[0])for(var w:ws)if(w.getMainHandItem().is(AntItems.SMALL_PREY)&&w.workerTasks().reason().equals("cache_full_blocked_or_foreign_cargo_retained")){
                killed[0]=true;var source=cover(l,q);int cooldown=NativePrey.get(l).remaining(source);var loaded=restore(c,l,w);reloadPrey(l);
                c.assertTrue(cache(l,q).size()==6&&stock(l,q)==1&&NativePrey.get(l).remaining(source)==cooldown,"Full storage and disk/entity restoration retain sole nectar cargo and exact source state");
                c.runAfterDelay(40,()->{
                    c.assertTrue(loaded.getMainHandItem().is(AntItems.SMALL_PREY)&&stock(l,q)==1,"Full storage retries retain cargo");
                    loaded.hurtServer(l,loaded.damageSources().generic(),1000);
                    c.runAfterDelay(40,()->{conserve(c,l,q);c.assertTrue(stock(l,q)==1&&harvested(l,q)==1&&loaded.getMainHandItem().isEmpty()&&loaded.workerTasks().phase()==WorkerTasks.Phase.DEAD,"Ordinary death releases one canonical cargo through normal world/custody, no source refund or worker replacement");c.succeed();});
                });return;
            }
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:foraging_pair")
    public void twoGenuineWorkersCompeteForOneWitnessedCoverPortion(GameTestHelper c){
        var f=new WorkerForagingGameTest();var l=c.getLevel();
        var records=NaturalSoil.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,NaturalSoil.get(l)).getOrThrow().getAsJsonObject();
        for(int x=1;x<=30;x++)for(int z=1;z<=30;z++)for(int y=1;y<=4;y++) {c.setBlock(x,y,z,Blocks.DIRT);records.addProperty(Long.toString(c.absolutePos(new BlockPos(x,y,z)).asLong()),"minecraft:dirt");}
        l.getDataStorage().set(NaturalSoil.TYPE,NaturalSoil.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,records).getOrThrow());
        var a=f.pairEgg(c,new BlockPos(8,4,10));var b=f.pairEgg(c,new BlockPos(20,4,10));var source=c.absolutePos(new BlockPos(14,5,12));
        boolean[] supplied={false};var approached=new HashSet<UUID>();long[] firstHarvest={-1};
        c.onEachTick(()->{
            if(!supplied[0]&&a.founding().lifecycle()==QueenFounding.Lifecycle.OPEN&&b.founding().lifecycle()==QueenFounding.Lifecycle.OPEN
                &&f.workers(c,a).stream().anyMatch(w->w.workerTasks().phase()==WorkerTasks.Phase.SEARCH)
                &&f.workers(c,b).stream().anyMatch(w->w.workerTasks().phase()==WorkerTasks.Phase.SEARCH)){
                supplied[0]=true;l.setBlock(source,Blocks.SHORT_GRASS.defaultBlockState(),3);
                var plants=NativeVegetation.CODEC.encodeStart(JsonOps.INSTANCE,NativeVegetation.get(l)).getOrThrow().getAsJsonObject();plants.addProperty(Long.toString(source.asLong()),l.getBlockState(source).toString());l.getDataStorage().set(NativeVegetation.TYPE,NativeVegetation.CODEC.parse(JsonOps.INSTANCE,plants).getOrThrow()); // Explicit controlled witness fixture; positive native test uses actual generation. // Explicit controlled source, no supplied adults/tasks.
            }
            if(!supplied[0])return;
            java.util.stream.Stream.concat(f.workers(c,a).stream(),f.workers(c,b).stream()).filter(w->source.equals(w.workerTasks().flowerSource())).forEach(w->approached.add(w.getUUID()));
            long count=NativePrey.get(l).harvestedSources().getOrDefault(source,0L);if(count>0&&firstHarvest[0]<0)firstHarvest[0]=l.getGameTime();
            long held=java.util.stream.Stream.concat(f.workers(c,a).stream(),f.workers(c,b).stream()).filter(w->w.getMainHandItem().is(AntItems.SMALL_PREY)).mapToInt(w->w.getMainHandItem().getCount()).sum();
            long cached=java.util.stream.Stream.of(f.cache(c,a),f.cache(c,b)).filter(Objects::nonNull).flatMap(n->n.contents().stream()).filter(s->s.is(AntItems.SMALL_PREY)).mapToInt(ItemStack::getCount).sum();
            long world=l.getEntitiesOfClass(ItemEntity.class,c.getBounds(),i->i.isAlive()&&i.getItem().is(AntItems.SMALL_PREY)).stream().mapToInt(i->i.getItem().getCount()).sum();
            c.assertTrue(count==held+cached+world+ingested(l,a)+ingested(l,b),"One shared source is counted once against both real colonies' cargo/cache/consumption");
            if(firstHarvest[0]>=0&&l.getGameTime()-firstHarvest[0]>100){c.assertTrue(count==1&&approached.size()==2,"Two genuinely emerged mature foragers independently approached same ready flower; only one wins before cooldown");l.setBlock(source,Blocks.AIR.defaultBlockState(),3);c.succeed();}
        });
    }
    private static void reloadPrey(ServerLevel l){
        var json=NativePrey.CODEC.encodeStart(JsonOps.INSTANCE,NativePrey.get(l)).getOrThrow();l.getDataStorage().set(NativePrey.TYPE,NativePrey.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow());
    }
    @GameTest(maxTicks=18000)
    public void sourceFreeScanInspectsEntireDeclaredBoxWithinSearchBound(GameTestHelper c){
        var l=level(c,"placement_soil");var cp=setup(c,l,4200);var foreign=setup(c,l,4280);boolean[] scanned={false};
        c.onEachTick(()->{
            if(!(l.getEntity(uuid(l,cp)) instanceof LasiusNigerEntity q)||q.founding().plan()==null)return;
            for(var w:workers(l,q))if(w.workerTasks().flowerInspections()==WorkerTasks.FLOWER_INSPECTION_BUDGET)scanned[0]=true;
            if(scanned[0]&&l.getEntity(uuid(l,foreign)) instanceof LasiusNigerEntity other&&other.founding().plan()!=null&&harvests(l,other)>0){
                var o=q.founding().plan().outside();
                for(var cell:BlockPos.betweenClosed(o.offset(-24,-3,-24),o.offset(24,3,24)))if(NestPlan.loaded(l,cell))
                    c.assertTrue(!NativePrey.habitat(l,cell)&&!FlowerNectar.habitat(l,cell),"Local absence fixture is genuinely source-free under both food ecologies");
                c.assertTrue(WorkerTasks.FLOWER_INSPECTION_BUDGET==16807&&workers(l,q).stream().allMatch(w->w.getMainHandItem().isEmpty())&&harvested(l,q)==0&&harvests(l,q)==0&&FlowerNectar.get(l).harvests()>0,"Complete local scan remains source-free while an unrelated genuine colony physically harvests; no global food disable");c.succeed();
            }
        });
    }
    @GameTest
    public void legacyNutritionStacksHistoryAndActiveCooldownLoadUnchanged(GameTestHelper c){
        var l=c.getLevel();var o=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());
        o.putLong("Sugar",622);o.putLong("Nectar",5);o.putLong("SpentSugar",4378);
        var n=new Nutrition();n.load(TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),o.buildResult()),8000,16000);
        c.assertTrue(n.sugar()==622&&n.nectar()==5&&n.gainedSugar()==5000&&n.spentSugar()==4378,"Legacy reserves/receipts retain exact historical 1000 yield");
        var old=new ItemStack(AntItems.FLOWER_NECTAR);var encoded=ItemStack.CODEC.encodeStart(l.registryAccess().createSerializationContext(JsonOps.INSTANCE),old).getOrThrow();var loaded=ItemStack.CODEC.parse(l.registryAccess().createSerializationContext(JsonOps.INSTANCE),encoded).getOrThrow();
        c.assertTrue(ItemStack.matches(old,loaded)&&Nutrition.sugarYield(loaded)==1000&&Nutrition.sugarYield(new ItemStack(AntItems.FLOWER_NECTAR_V2))==4000,"Item-ID version survives actual stack codec; no old repricing");
        c.assertTrue(n.ingest(new ItemStack(AntItems.FLOWER_NECTAR_V2),8000,16000)&&n.gainedSugar()==9000&&n.sugar()==4622,"New portions credit declared yield beside old receipts");
        c.assertTrue(!n.spend(1000,2000)&&n.protein()==0,"Sugar cannot pay protein costs");
        var round=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());n.save(round);var again=new Nutrition();again.load(TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),round.buildResult()),8000,16000);c.assertTrue(again.gainedSugar()==9000&&again.nectarV2()==1,"Mixed nutrition saves preserve actual credited yields");
        var src=new JsonObject();var row=new JsonObject();row.addProperty("state",Blocks.POPPY.defaultBlockState().toString());row.addProperty("remaining",1100);row.addProperty("harvests",5);row.addProperty("lastTick",123);src.addProperty("1",row.toString());
        var data=FlowerNectar.CODEC.parse(JsonOps.INSTANCE,src).getOrThrow();c.assertTrue(data.remaining(BlockPos.of(1))==1100&&data.harvests()==5&&FlowerNectar.COOLDOWN_TICKS==600,"Old active cooldown above new 600 remains untouched");
        var history=new JsonObject();history.addProperty("id",UUID.randomUUID().toString());var nutrition=new JsonObject();nutrition.addProperty("nectar",5);history.add("nutrition",nutrition);
        c.assertTrue(BroodHistory.units(history.toString(),"nectar")==5&&BroodHistory.units(history.toString(),"nectarV2")==0&&BroodHistory.units(history.toString(),"prey")==0,"Historical brood/absence of new version fields remains legacy");
        var plan=NestPlan.geometry(c.absolutePos(new BlockPos(8,4,8)),net.minecraft.core.Direction.NORTH);var pile=new BroodPile(plan.nursery(),NurseryBlocks.BROOD_PILE.defaultBlockState());
        var archive=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());archive.putString("Queen",UUID.randomUUID().toString());archive.store("Entrance",BlockPos.CODEC,plan.entrance());archive.putString("Direction","north");
        var originals=java.util.stream.IntStream.range(0,3).mapToObj(i->UUID.randomUUID().toString()).toList();archive.store("Original",com.mojang.serialization.Codec.STRING.listOf(),originals);archive.store("Consumed",com.mojang.serialization.Codec.STRING.listOf(),originals);archive.putLong("ConsumedNectar",5);
        pile.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),archive.buildResult()));
        c.assertTrue(pile.consumedNectar()==5&&pile.consumedNectarV2()==0&&pile.gainedSugar()==5000&&pile.gainedProtein()==0,"Legacy retired brood receipts remain 1000 each through actual block-entity load");
        var adultRow=new JsonObject();adultRow.addProperty("queen",UUID.randomUUID().toString());adultRow.add("nutrition",nutrition);var adults=new JsonObject();String id=UUID.randomUUID().toString();adults.addProperty(id,adultRow.toString());
        var oldAdults=AdultHistory.CODEC.parse(JsonOps.INSTANCE,adults).getOrThrow();c.assertTrue(oldAdults.records().get(id).equals(adultRow.toString()),"Existing adult terminal history is retained byte-for-byte, no retroactive receipt revaluation");c.succeed();
    }
}
