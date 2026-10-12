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

/** Independent T06 pre-existing tier-three habitat; no worker-built construction evidence or credited nutrition. */
final class HallFundingFixture {
    final NestPlanFixture fx=new NestPlanFixture();
    long apples,chickens;
    LasiusNigerEntity start(GameTestHelper c){
        SurfaceFixture.terrain(c);var l=c.getLevel();var home=NestPlan.geometry(c.absolutePos(new BlockPos(32,4,32)),Direction.EAST);
        int count=9;var broodIds=new ArrayList<UUID>();for(int n=0;n<count;n++)broodIds.add(UUID.randomUUID());
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
        cacheTag.put("Contents",ItemStack.CODEC.listOf().encodeStart(NbtOps.INSTANCE,List.<ItemStack>of()).getOrThrow());SurfaceFixture.loadBlock(c,cache,cacheTag);
        l.setBlock(store.marker(),NurseryBlocks.MATERIAL_STORE.defaultBlockState(),3);var material=(MaterialStore)l.getBlockEntity(store.marker());var materialTag=material.saveWithFullMetadata(l.registryAccess());SurfaceFixture.identity(materialTag,home,"Colony",queen.getUUID());materialTag.putString("Placement","right");materialTag.putInt("InventoryFormat",2);
        materialTag.put("Contents",ItemStack.CODEC.listOf().encodeStart(NbtOps.INSTANCE,List.<ItemStack>of()).getOrThrow());SurfaceFixture.loadBlock(c,material,materialTag);
        var registry=ChamberRegistry.get(l);registry.found(queen.getUUID(),home);
        registry.register(queen.getUUID(),new ChamberRegistry.Chamber(ChamberExcavation.STORE,store.min(),store.max(),Set.of(ChamberFunction.MATERIAL_STORE),3,Map.of(ChamberFunction.MATERIAL_STORE,store.marker())));
        registry.register(queen.getUUID(),new ChamberRegistry.Chamber(ChamberExcavation.HALL,hall.min(),hall.max(),Set.of(ChamberFunction.QUEENS_HALL),3,Map.of()));
        for(var chamber:registry.colony(queen.getUUID()).chambers())for(var at:ChamberUpgrade.walls(home,chamber))if(!opened.contains(at)){
            l.setBlock(at,NurseryBlocks.NEST_CUT_STONE.defaultBlockState(),3);ColonyTerrain.get(l).built(at,queen.getUUID(),NurseryBlocks.NEST_CUT_STONE);
        }
        InitialSurfaceHabitat.declare(c,queen.getUUID(),opened);c.assertTrue(l.addFreshEntity(queen),"Insert the real initial queen");
        for(int n=0;n<count;n++){
            UUID brood=broodIds.get(n);var w=AntEntities.WORKER.create(l,EntitySpawnReason.COMMAND);c.assertTrue(w!=null,"Real controlled worker");w.setUUID(ColonyMembers.workerId(brood));w.initializeCallow(brood,queen.getUUID(),home.chamber());
            w.setPos(Vec3.atBottomCenterOf(home.at(3+n%3,n%2==0?-1:0,-2)));c.assertTrue(l.addFreshEntity(w),"Insert real registered worker");ColonyMembers.get(l).record(brood,queen.getUUID(),home.chamber());
        }
        c.assertTrue(ChamberExcavation.get(l).jobs(queen.getUUID()).isEmpty()&&ChamberUpgrade.get(l).jobs(queen.getUUID()).isEmpty()&&queen.founding().removed()==0,"Initial habitat manufactures no excavation or upgrade work receipts");

        c.assertTrue(queen.nutrition().consumedUnits()==0&&fx.f.workers(c,queen).stream().allMatch(w->w.nutrition().consumedUnits()==0),"No initial ingested units or receipts");
        PrimeAnts.LOGGER.info("T06 CONTROL SETUP queen={} workers=9 preExistingTier=3 emptyFood=true emptyNutrition=true noWorkReceipts=true beforeFirstTick=true",queen.getUUID());
        return queen;
    }
    void supply(GameTestHelper c,LasiusNigerEntity q,int a,int p){
        var at=q.founding().plan().at(-4,0,1);
        if(a>0)fx.drop(c,at,new ItemStack(Items.APPLE,a));
        if(p>0)fx.drop(c,at,new ItemStack(Items.CHICKEN,p));
        apples+=a;chickens+=p;
        PrimeAnts.LOGGER.info("T06 CONTROL PHYSICAL SUPPLY queen={} tick={} delta={}/{} total={}/{}",q.getUUID(),c.getTick(),a,p,apples,chickens);
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
        c.assertTrue(fx.f.workers(c,q).size()==9&&q.isAlive(),"Declared adults survive preparation and measurement");
    }
}
