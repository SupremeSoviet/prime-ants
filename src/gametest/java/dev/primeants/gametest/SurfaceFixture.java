package dev.primeants.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import dev.primeants.PrimeAnts;
import dev.primeants.brood.*;
import dev.primeants.colony.*;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.item.AntItems;
import dev.primeants.worker.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Declared finite fixtures. Initial habitat is explicitly excluded from worker construction evidence. */
final class SurfaceFixture {
    final NestPlanFixture fx=new NestPlanFixture();
    final Set<BlockPos> initialSoil=new LinkedHashSet<>();
    final Map<BlockPos,BlockState> protectedCells=new LinkedHashMap<>();
    private int lastReceipt=-1;
    static void terrain(GameTestHelper c){
        var l=c.getLevel();var records=NaturalSoil.CODEC.encodeStart(JsonOps.INSTANCE,NaturalSoil.get(l)).getOrThrow().getAsJsonObject();
        for(int x=13;x<=52;x++)for(int z=13;z<=52;z++)for(int y=1;y<=4;y++){
            var at=c.absolutePos(new BlockPos(x,y,z));l.setBlock(at,Blocks.DIRT.defaultBlockState(),3);records.addProperty(Long.toString(at.asLong()),"minecraft:dirt");
        }l.getDataStorage().set(NaturalSoil.TYPE,NaturalSoil.CODEC.parse(JsonOps.INSTANCE,records).getOrThrow());
    }
    LasiusNigerEntity founder(GameTestHelper c){
        terrain(c);Player player=new Player(c.getLevel(),new GameProfile(UUID.randomUUID(),"surface-fixture")){
            public GameType gameMode(){return GameType.CREATIVE;}public boolean isClientAuthoritative(){return false;}public PermissionSet permissions(){return PermissionSet.ALL_PERMISSIONS;}
        };
        var egg=new ItemStack(AntItems.DEBUG_QUEEN_EGG);player.setItemInHand(InteractionHand.MAIN_HAND,egg);var at=c.absolutePos(new BlockPos(32,4,32));
        c.assertTrue(egg.useOn(new UseOnContext(c.getLevel(),player,InteractionHand.MAIN_HAND,egg,new BlockHitResult(Vec3.atCenterOf(at).add(0,0.5,0),Direction.UP,at,false))).consumesAction(),"Actual founding egg on declared witnessed ground");
        var queens=c.getLevel().getEntitiesOfClass(LasiusNigerEntity.class,c.getBounds(),q->q.form()==AntForm.QUEEN);c.assertTrue(queens.size()==1,"One real surface fixture queen");return queens.getFirst();
    }
    static void loadBlock(GameTestHelper c,net.minecraft.world.level.block.entity.BlockEntity be,CompoundTag tag){
        var l=c.getLevel();var at=be.getBlockPos();var state=be.getBlockState();l.removeBlockEntity(at);
        var back=net.minecraft.world.level.block.entity.BlockEntity.loadStatic(at,state,tag,l.registryAccess());c.assertTrue(back!=null,"Canonical initial/reloaded block entity");l.setBlockEntity(back);
    }
    static void identity(CompoundTag tag,NestPlan home,String key,UUID queen){
        tag.putString(key,queen.toString());tag.put("Entrance",BlockPos.CODEC.encodeStart(NbtOps.INSTANCE,home.entrance()).getOrThrow());tag.putString("Direction",home.direction().getName());
    }
    LasiusNigerEntity habitat(GameTestHelper c,boolean great){return habitat(c,great,false);}
    /** Separate declared diagonal source approach for the self-occupancy regression, before any worker work. */
    LasiusNigerEntity habitat(GameTestHelper c,boolean great,boolean diagonalSource){
        terrain(c);var l=c.getLevel();var home=NestPlan.geometry(c.absolutePos(new BlockPos(32,4,32)),Direction.EAST);
        int count=great?49:29;var broodIds=new ArrayList<UUID>();for(int n=0;n<count;n++)broodIds.add(UUID.randomUUID());
        var queen=AntEntities.QUEEN.create(l,EntitySpawnReason.COMMAND);c.assertTrue(queen!=null,"Real initial habitat queen");queen.setPos(Vec3.atBottomCenterOf(home.chamber()));
        var state=new CompoundTag();state.putString("Phase","SETTLED");state.putString("Lifecycle","OPEN");identity(state,home,"FixtureOwner",queen.getUUID());
        state.put("Tasks",BlockPos.CODEC.listOf().encodeStart(NbtOps.INSTANCE,home.tasks()).getOrThrow());
        state.put("Expected",BlockState.CODEC.listOf().encodeStart(NbtOps.INSTANCE,Collections.nCopies(home.tasks().size(),Blocks.DIRT.defaultBlockState())).getOrThrow());
        // Zero labor/removal/deposit receipts: this is a pre-existing controlled habitat, not a completed founder.
        queen.founding().load(TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),state));
        var opened=new HashSet<BlockPos>(home.tasks());for(var cell:NestBlueprint.WIDENINGS)opened.add(home.at(cell.forward(),cell.side(),cell.dy()));
        var store=ChamberExcavation.build(home,"right");var hall=ChamberExcavation.build(home,"hall_left");opened.addAll(store.tasks());opened.addAll(hall.tasks());
        for(var at:opened)l.setBlock(at,Blocks.AIR.defaultBlockState(),3);
        var plugs=ColonyPlugs.CODEC.encodeStart(JsonOps.INSTANCE,ColonyPlugs.get(l)).getOrThrow().getAsJsonObject();
        for(var p:home.plugs())plugs.addProperty(Long.toString(p.asLong()),queen.getUUID()+":open");l.getDataStorage().set(ColonyPlugs.TYPE,ColonyPlugs.CODEC.parse(JsonOps.INSTANCE,plugs).getOrThrow());
        l.setBlock(home.nursery(),NurseryBlocks.BROOD_PILE.defaultBlockState(),3);var pile=(BroodPile)l.getBlockEntity(home.nursery());var pileTag=pile.saveWithFullMetadata(l.registryAccess());
        identity(pileTag,home,"Queen",queen.getUUID());pileTag.putInt("AdultCapacityBound",120);
        // The initial nursery's three original identities are three of the real declared living adults below.
        // This is existing membership history, with zero new brood, excavation, upgrade or surface work receipts.
        var originals=broodIds.subList(0,3).stream().map(UUID::toString).toList();
        pileTag.put("Original",com.mojang.serialization.Codec.STRING.listOf().encodeStart(NbtOps.INSTANCE,originals).getOrThrow());
        pileTag.put("Consumed",com.mojang.serialization.Codec.STRING.listOf().encodeStart(NbtOps.INSTANCE,originals).getOrThrow());loadBlock(c,pile,pileTag);
        l.setBlock(home.cache(),NurseryBlocks.NEST_CACHE.defaultBlockState(),3);var cache=(NestCache)l.getBlockEntity(home.cache());var cacheTag=cache.saveWithFullMetadata(l.registryAccess());identity(cacheTag,home,"Colony",queen.getUUID());cacheTag.putInt("InventoryFormat",2);
        var food=new ArrayList<ItemStack>();for(int n=0;n<4;n++){food.add(new ItemStack(Items.APPLE));food.add(new ItemStack(Items.CHICKEN));}
        cacheTag.put("Contents",ItemStack.CODEC.listOf().encodeStart(NbtOps.INSTANCE,food).getOrThrow());loadBlock(c,cache,cacheTag);
        l.setBlock(store.marker(),NurseryBlocks.MATERIAL_STORE.defaultBlockState(),3);var material=(MaterialStore)l.getBlockEntity(store.marker());var materialTag=material.saveWithFullMetadata(l.registryAccess());identity(materialTag,home,"Colony",queen.getUUID());materialTag.putString("Placement","right");materialTag.putInt("InventoryFormat",2);
        var stone=new ArrayList<ItemStack>();if(great)for(int n=0;n<32;n++)stone.add(new ItemStack(Items.COBBLESTONE));materialTag.put("Contents",ItemStack.CODEC.listOf().encodeStart(NbtOps.INSTANCE,stone).getOrThrow());loadBlock(c,material,materialTag);
        var registry=ChamberRegistry.get(l);registry.found(queen.getUUID(),home);
        registry.register(queen.getUUID(),new ChamberRegistry.Chamber(ChamberExcavation.STORE,store.min(),store.max(),Set.of(ChamberFunction.MATERIAL_STORE),2,Map.of(ChamberFunction.MATERIAL_STORE,store.marker())));
        registry.register(queen.getUUID(),new ChamberRegistry.Chamber(ChamberExcavation.HALL,hall.min(),hall.max(),Set.of(ChamberFunction.QUEENS_HALL),2,Map.of()));
        for(var chamber:registry.colony(queen.getUUID()).chambers())for(var at:ChamberUpgrade.walls(home,chamber))if(!opened.contains(at)){
            l.setBlock(at,NurseryBlocks.PACKED_CLAY.defaultBlockState(),3);ColonyTerrain.get(l).built(at,queen.getUUID(),NurseryBlocks.PACKED_CLAY);
        }
        // Initial surface supplies are eighty existing owned mound units, never placed at a structural target.
        var structural=SurfacePlan.bundled(ColonyStage.GREAT).cells().stream().map(SurfacePlan.Cell::column).collect(java.util.stream.Collectors.toSet());
        var initialPlan=diagonalSource?new ArrayList<>(NestMound.plan(ColonyStage.MATURE)):new ArrayList<>(NestMound.plan(ColonyStage.YOUNG));
        if(diagonalSource)initialPlan.removeIf(cell->cell.layer()!=0||cell.forward()>=-5);
        for(var cell:initialPlan){
            if(structural.contains(cell.forward()+","+cell.side()))continue;var at=home.at(cell.forward(),cell.side(),1+cell.layer());
            if(!l.getBlockState(at.below()).isSolidRender())continue;l.setBlock(at,NurseryBlocks.NEST_SOIL.defaultBlockState(),3);ColonyTerrain.get(l).deposited(at,queen.getUUID());initialSoil.add(at);if(initialSoil.size()==80)break;
        }c.assertTrue(initialSoil.size()==80,"Eighty finite declared owned mound units");
        InitialSurfaceHabitat.declare(c,queen.getUUID(),opened);c.assertTrue(l.addFreshEntity(queen),"Insert the real initial queen");
        for(int n=0;n<count;n++){
            UUID brood=broodIds.get(n);var w=AntEntities.WORKER.create(l,EntitySpawnReason.COMMAND);c.assertTrue(w!=null,"Real controlled worker");w.setUUID(ColonyMembers.workerId(brood));w.initializeCallow(brood,queen.getUUID(),home.chamber());
            w.setPos(Vec3.atBottomCenterOf(home.at(3+n%3,n%2==0?-1:0,-2)));c.assertTrue(l.addFreshEntity(w),"Insert real registered worker");ColonyMembers.get(l).record(brood,queen.getUUID(),home.chamber());
            c.assertTrue(w.nutrition().ingest(new ItemStack(Items.APPLE),Nutrition.QUEEN_SUGAR_CAPACITY,Nutrition.QUEEN_PROTEIN_CAPACITY),"Declared initial ingested apple");
        }
        c.assertTrue(queen.nutrition().ingest(new ItemStack(Items.APPLE),Nutrition.QUEEN_SUGAR_CAPACITY,Nutrition.QUEEN_PROTEIN_CAPACITY)&&queen.nutrition().ingest(new ItemStack(Items.CHICKEN),Nutrition.QUEEN_SUGAR_CAPACITY,Nutrition.QUEEN_PROTEIN_CAPACITY),"Declared queen's existing ingested apple/chicken");
        c.assertTrue(ChamberExcavation.get(l).jobs(queen.getUUID()).isEmpty()&&ChamberUpgrade.get(l).jobs(queen.getUUID()).isEmpty()&&queen.founding().removed()==0,"Initial habitat manufactures no excavation or upgrade work receipts");
        PrimeAnts.LOGGER.info("T12 INITIAL HABITAT queen={} workers={} soil=80 stone={} cacheApple=4 cacheChicken=4 existingIngestedApples={} existingIngestedChickens=1 excavationReceipts=0 surfacePlacements=0 stageNotSet=true",queen.getUUID(),count,great?32:0,count+1);
        return queen;
    }
    void snapshot(GameTestHelper c,LasiusNigerEntity q){
        var l=c.getLevel();var home=q.founding().plan();var allowed=new HashSet<BlockPos>(MoundSoil.cells(l,home,q.getUUID()));
        for(var cell:SurfacePlan.bundled(ColonyStage.GREAT).cells())allowed.add(home.at(cell.forward(),cell.side(),1+cell.layer()));
        allowed.add(home.cache());allowed.add(home.nursery());var material=MaterialStore.owned(l,q.getUUID(),home);if(material!=null)allowed.add(material.getBlockPos());
        for(int f=-15;f<=16;f++)for(int side=-13;side<=13;side++)for(int dy=-3;dy<=9;dy++){
            var at=home.at(f,side,dy);if(!allowed.contains(at))protectedCells.put(at,l.getBlockState(at));
        }
    }
    void protectedTerrain(GameTestHelper c,LasiusNigerEntity q,boolean settled){
        int receipts=SurfaceWork.get(c.getLevel()).jobs(q.getUUID()).stream().mapToInt(SurfaceWork.Job::completed).sum();
        if(receipts!=lastReceipt||settled){lastReceipt=receipts;for(var e:protectedCells.entrySet())c.assertTrue(c.getLevel().getBlockState(e.getKey()).equals(e.getValue()),"All ground, protected supports and cells outside the footprint unchanged: "+e.getKey());}
        var home=q.founding().plan();for(int f=-14;f<0;f++)c.assertTrue(NestPlan.walkable(c.getLevel(),home.at(f,0,1)),"Actual two-high connected surface approach "+f);
        c.assertTrue(NestPlan.walkable(c.getLevel(),home.outside())&&NestPlan.walkable(c.getLevel(),home.at(0,0,0))&&NestPlan.walkable(c.getLevel(),home.at(1,0,-1))&&NestPlan.walkable(c.getLevel(),home.at(2,0,-2)),"Exterior stand and every stair remain physically walkable");
    }
    void soil(GameTestHelper c,LasiusNigerEntity q){
        var l=c.getLevel();var all=new HashSet<BlockPos>(initialSoil);for(var j:SurfaceWork.get(l).jobs(q.getUUID()))all.addAll(j.cells());
        long blocks=all.stream().filter(p->l.getBlockState(p).is(NurseryBlocks.NEST_SOIL)||l.getBlockState(p).is(NurseryBlocks.MOUND_GATE)).count();
        long cargo=fx.f.workers(c,q).stream().filter(w->w.getMainHandItem().is(Items.DIRT)).mapToInt(w->w.getMainHandItem().getCount()).sum();
        long ground=l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,c.getBounds().inflate(8),i->i.isAlive()&&i.getItem().is(Items.DIRT)).stream().mapToInt(i->i.getItem().getCount()).sum();
        long pending=TransferCustody.get(l).contents().stream().filter(t->t.stack().is(Items.DIRT)&&c.getBounds().inflate(8).contains(t.position())).mapToInt(t->t.stack().getCount()).sum();
        c.assertTrue(blocks+cargo+ground+pending==80,"Eighty declared soil units stay in current blocks/cargo/ground/custody, including gates: "+blocks+"/"+cargo+"/"+ground+"/"+pending);
        for(var j:SurfaceWork.get(l).jobs(q.getUUID())){
            var w=j.claim==null?null:fx.f.workers(c,q).stream().filter(a->a.getUUID().equals(j.claim)).findFirst().orElse(null);int held=w!=null&&w.getMainHandItem().is(Items.DIRT)?w.getMainHandItem().getCount():0;
            c.assertTrue(j.recovered()==j.placed()+j.released()+held&&j.carried()==held,"Surface relocation history is separate from historical excavation deposits and exact current cargo");
        }
        var widening=NestExpansion.get(l).job(q.getUUID());var mining=Mining.get(l).job(q.getUUID());
        long builders=SurfaceWork.get(l).jobs(q.getUUID()).stream().filter(j->j.claim!=null).count()
            +(widening!=null&&widening.claim!=null?1:0)+(mining!=null&&mining.claim!=null?1:0)
            +ChamberExcavation.get(l).jobs(q.getUUID()).stream().filter(j->j.claim!=null).count()+ChamberUpgrade.get(l).jobs(q.getUUID()).stream().filter(j->j.claim!=null).count();
        c.assertTrue(builders<=1,"One current shared builder across every work owner per colony");
        if(builders>0)c.assertTrue(TierTwoFixture.caregivers(c,q)>=2,"Two ordinary authorized caregivers retained during surface construction");
    }
    static void completed(GameTestHelper c,LasiusNigerEntity q,ColonyStage stage){
        var l=c.getLevel();var j=SurfaceWork.get(l).job(q.getUUID(),stage);c.assertTrue(j!=null&&j.complete()&&j.claim==null,"Complete surface work with no retained builder");
        for(int i=0;i<j.plan.cells().size();i++)c.assertTrue(ColonyTerrain.get(l).surface(l,j.at(i),q.getUUID())&&l.getBlockState(j.at(i)).is(j.plan.cells().get(i).material().equals("gate")?NurseryBlocks.MOUND_GATE:NurseryBlocks.NEST_SOIL),"Every completed cell is the actual owned paid material "+j.plan.cells().get(i));
        c.assertTrue(j.receipts().size()==j.plan.cost()&&j.placed()>0,"Saved flags alone are insufficient: actual new worker placements and physical cells");
        c.assertTrue(j.plan.cells().stream().filter(cell->cell.component().equals("watch_post")&&cell.layer()==4).map(SurfacePlan.Cell::side).distinct().count()==(stage==ColonyStage.GREAT?2:0),"Two actual raised watch posts only in Great");
    }
}
