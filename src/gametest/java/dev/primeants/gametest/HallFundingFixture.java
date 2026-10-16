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

/** Independent T07 meal-eligible pre-existing habitat; no construction, ingestion or nutrition credit. */
final class HallFundingFixture {
    final NestPlanFixture fx=new NestPlanFixture();
    final Set<UUID> initialAdults=new HashSet<>();
    long apples,chickens;
    LasiusNigerEntity start(GameTestHelper c,boolean shortage){
        SurfaceFixture.terrain(c);var l=c.getLevel();var home=NestPlan.geometry(c.absolutePos(new BlockPos(32,4,32)),Direction.EAST);
        int count=9;var broodIds=new ArrayList<UUID>();for(int n=0;n<count;n++)broodIds.add(dev.primeants.gametest.TestIdentities.next());
        var queen=AntEntities.QUEEN.create(l,EntitySpawnReason.COMMAND);c.assertTrue(queen!=null,"Real initial habitat queen");queen.setPos(Vec3.atBottomCenterOf(home.chamber()));
        var state=new CompoundTag();state.putString("Phase","SETTLED");state.putString("Lifecycle","OPEN");SurfaceFixture.identity(state,home,"FixtureOwner",queen.getUUID());
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
        SurfaceFixture.identity(pileTag,home,"Queen",queen.getUUID());pileTag.putInt("AdultCapacityBound",120);
        // The initial nursery's three original identities are three of the real declared living adults below.
        // This is existing membership history, with zero new brood, excavation, upgrade or surface work receipts.
        var originals=broodIds.subList(0,3).stream().map(UUID::toString).toList();
        pileTag.put("Original",com.mojang.serialization.Codec.STRING.listOf().encodeStart(NbtOps.INSTANCE,originals).getOrThrow());
        pileTag.put("Consumed",com.mojang.serialization.Codec.STRING.listOf().encodeStart(NbtOps.INSTANCE,originals).getOrThrow());SurfaceFixture.loadBlock(c,pile,pileTag);
        l.setBlock(home.cache(),NurseryBlocks.NEST_CACHE.defaultBlockState(),3);var cache=(NestCache)l.getBlockEntity(home.cache());var cacheTag=cache.saveWithFullMetadata(l.registryAccess());SurfaceFixture.identity(cacheTag,home,"Colony",queen.getUUID());cacheTag.putInt("InventoryFormat",2);
        cacheTag.putInt("InventoryFormat",3);
        var initialFood=new ArrayList<ItemStack>();for(int n=0;n<8;n++)initialFood.add(new ItemStack(Items.APPLE));for(int n=0;n<(shortage?1:3);n++)initialFood.add(new ItemStack(Items.CHICKEN));
        cacheTag.put("Contents",ItemStack.CODEC.listOf().encodeStart(NbtOps.INSTANCE,initialFood).getOrThrow());SurfaceFixture.loadBlock(c,cache,cacheTag);apples=8;chickens=shortage?1:3;
        l.setBlock(store.marker(),NurseryBlocks.MATERIAL_STORE.defaultBlockState(),3);var material=(MaterialStore)l.getBlockEntity(store.marker());var materialTag=material.saveWithFullMetadata(l.registryAccess());SurfaceFixture.identity(materialTag,home,"Colony",queen.getUUID());materialTag.putString("Placement","right");materialTag.putInt("InventoryFormat",2);
        materialTag.put("Contents",ItemStack.CODEC.listOf().encodeStart(NbtOps.INSTANCE,List.<ItemStack>of()).getOrThrow());SurfaceFixture.loadBlock(c,material,materialTag);
        var registry=ChamberRegistry.get(l);registry.found(queen.getUUID(),home);
        registry.register(queen.getUUID(),new ChamberRegistry.Chamber(ChamberExcavation.STORE,store.min(),store.max(),Set.of(ChamberFunction.MATERIAL_STORE),3,Map.of(ChamberFunction.MATERIAL_STORE,store.marker())));
        registry.register(queen.getUUID(),new ChamberRegistry.Chamber(ChamberExcavation.HALL,hall.min(),hall.max(),Set.of(ChamberFunction.QUEENS_HALL),3,Map.of()));
        for(var chamber:registry.colony(queen.getUUID()).chambers())for(var at:ChamberUpgrade.walls(home,chamber))if(!opened.contains(at)){
            l.setBlock(at,NurseryBlocks.NEST_CUT_STONE.defaultBlockState(),3);ColonyTerrain.get(l).built(at,queen.getUUID(),NurseryBlocks.NEST_CUT_STONE);
        }
        InitialSurfaceHabitat.declare(c,queen.getUUID(),opened);history(c,queen,false);initialAdults.add(queen.getUUID());c.assertTrue(l.addFreshEntity(queen),"Insert the real initial queen");
        int[][] positions={{3,-2},{4,-2},{5,-2},{3,-3},{4,-3},{5,-3},{3,0},{5,0},{4,2}};
        for(int n=0;n<count;n++){
            UUID brood=broodIds.get(n);var w=AntEntities.WORKER.create(l,EntitySpawnReason.COMMAND);c.assertTrue(w!=null,"Real controlled worker");w.setUUID(ColonyMembers.workerId(brood));w.initializeCallow(brood,queen.getUUID(),home.chamber());
            var feet=home.at(positions[n][0],positions[n][1],-2);c.assertTrue(NestPlan.walkable(l,feet),"Declared adult has a supported initial floor");w.setPos(Vec3.atBottomCenterOf(feet));history(c,w,true);initialAdults.add(w.getUUID());c.assertTrue(l.addFreshEntity(w),"Insert real registered worker");ColonyMembers.get(l).record(brood,queen.getUUID(),home.chamber());
        }
        c.assertTrue(ChamberExcavation.get(l).jobs(queen.getUUID()).isEmpty()&&ChamberUpgrade.get(l).jobs(queen.getUUID()).isEmpty()&&queen.founding().removed()==0,"Initial habitat manufactures no excavation or upgrade work receipts");

        c.assertTrue(queen.nutrition().consumedUnits()==0&&fx.f.workers(c,queen).stream().allMatch(w->w.nutrition().consumedUnits()==0),"No initial ingested units or receipts");
        var liveCache=fx.f.cache(c,queen);
        c.assertTrue(liveCache.capacity()==18&&liveCache.size()==(shortage?9:11)&&8<=FoodShares.share(liveCache.capacity())&&(shortage?1:3)<=FoodShares.share(liveCache.capacity()),"Initial physical cache respects capacity and both food shares");ledger(c,queen);
        PrimeAnts.LOGGER.info("T07 CONTROL SETUP queen={} workers=9 preExistingTier=3 initialCache=8/{} adultAge=12000 active=12000 fasting=12000 callowComplete=true emptyNutrition=true noWorkReceipts=true beforeFirstTick=true",queen.getUUID(),chickens);
        return queen;
    }
    /** One coherent zero-consumption adult history, before insertion/the first callback only. */
    private void history(GameTestHelper c,LasiusNigerEntity a,boolean worker){
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,c.getLevel().registryAccess());a.save(out);var tag=out.buildResult();
        tag.putLong("AntElapsedAgeTicks",12000);if(worker)tag.putLong("CallowTicks",tag.getLongOr("CallowDuration",BroodPile.callowTicks()));
        var life=tag.getCompoundOrEmpty("AdultLife");life.putLong("ActiveTicks",12000);life.putLong("Fasting",12000);life.putBoolean("Started",true);tag.put("AdultLife",life);
        a.load(TagValueInput.create(ProblemReporter.DISCARDING,c.getLevel().registryAccess(),tag));
        c.assertTrue(a.elapsedAgeTicks()==12000&&a.adultLife().activeTicks()==12000&&a.adultLife().fasting()==12000&&a.adultLife().hungry()&&!a.isCallow()
            &&a.adultLife().lifespan()==AdultLife.DEFAULT_LIFESPAN&&a.adultLife().grace()==AdultLife.DEFAULT_FASTING&&a.adultLife().maintenanceTicks()==0&&a.adultLife().coveredTicks()==0&&a.adultLife().coverageRemaining()==0
            &&a.nutrition().consumedUnits()==0&&a.nutrition().sugar()==0&&a.nutrition().protein()==0,"Coherent pre-existing fasting adult, default policy and zero food/coverage/receipts");
    }
    void supply(GameTestHelper c,LasiusNigerEntity q,int a,int p){
        var at=q.founding().plan().at(-4,0,1);
        c.assertTrue(NestPlan.walkable(c.getLevel(),at),"Declared physical dropped source has ordinary dry support");
        if(a>0)fx.drop(c,at,new ItemStack(Items.APPLE,a));
        if(p>0)fx.drop(c,at,new ItemStack(Items.CHICKEN,p));
        apples+=a;chickens+=p;
        PrimeAnts.LOGGER.info("T07 CONTROL PHYSICAL SUPPLY queen={} tick={} delta={}/{} total={}/{}",q.getUUID(),c.getTick(),a,p,apples,chickens);
    }
    long units(GameTestHelper c,LasiusNigerEntity q,Item item){
        var l=c.getLevel();var pile=NestPlanFixture.pile(c,q);var cache=fx.f.cache(c,q);var workers=fx.f.workers(c,q);
        long physical=l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,c.getBounds().inflate(8),i->i.isAlive()&&i.getItem().is(item)).stream().mapToLong(i->i.getItem().getCount()).sum();
        physical+=workers.stream().filter(w->w.getMainHandItem().is(item)).mapToLong(w->w.getMainHandItem().getCount()).sum();
        physical+=cache.contents().stream().filter(s->s.is(item)).mapToLong(ItemStack::getCount).sum();
        physical+=TransferCustody.get(l).contents().stream().filter(t->c.getBounds().inflate(8).contains(t.position())&&t.stack().is(item)).mapToLong(t->t.stack().getCount()).sum();
        boolean apple=item==Items.APPLE;
        return physical+(apple?q.nutrition().apples()+pile.consumedApples():q.nutrition().chickens()+pile.consumedChickens())
            +workers.stream().mapToLong(w->apple?w.nutrition().apples():w.nutrition().chickens()).sum();
    }
    void ledger(GameTestHelper c,LasiusNigerEntity q){
        c.assertTrue(units(c,q,Items.APPLE)==apples&&units(c,q,Items.CHICKEN)==chickens,"Independent supplied = physical + single consumed apple/chicken ledgers");
        fx.food.yields(c,q);
        c.assertTrue(initialAdults.stream().allMatch(id->c.getLevel().getEntity(id) instanceof LasiusNigerEntity a&&a.isAlive()&&!a.isRemoved()),"Every declared initial adult survives; genuine births retain separate identities");
    }
}
