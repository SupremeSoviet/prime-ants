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

/** Controlled initial tier-two habitat. All setup ends before the acceptance boundary below. */
final class TierThreeFixture {
    final NestPlanFixture fx=new NestPlanFixture();
    long apples=54,chickens=5,stone,clay,cobble;
    long lastFood=-200;
    boolean constructionWave, hungryInitialAdults;
    final Set<BlockPos> initialWalls=new LinkedHashSet<>();
    private final Map<String,Integer> observedTaken=new HashMap<>(),observedBuilt=new HashMap<>();
    private final Set<UUID> observedCargo=new HashSet<>();
    int withdrawals,conversions;
    LasiusNigerEntity start(GameTestHelper c,int stoneUnits){return start(c,stoneUnits,false);}
    LasiusNigerEntity start(GameTestHelper c,int stoneUnits,boolean hungry){
        hungryInitialAdults=hungry;
        SurfaceFixture.terrain(c);var l=c.getLevel();var home=NestPlan.geometry(c.absolutePos(new BlockPos(32,4,32)),Direction.EAST);
        int count=49;var broodIds=new ArrayList<UUID>();for(int n=0;n<count;n++)broodIds.add(UUID.randomUUID());
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
        var food=new ArrayList<ItemStack>();for(int n=0;n<4;n++){food.add(new ItemStack(Items.APPLE));food.add(new ItemStack(Items.CHICKEN));}
        cacheTag.put("Contents",ItemStack.CODEC.listOf().encodeStart(NbtOps.INSTANCE,food).getOrThrow());SurfaceFixture.loadBlock(c,cache,cacheTag);
        l.setBlock(store.marker(),NurseryBlocks.MATERIAL_STORE.defaultBlockState(),3);var material=(MaterialStore)l.getBlockEntity(store.marker());var materialTag=material.saveWithFullMetadata(l.registryAccess());SurfaceFixture.identity(materialTag,home,"Colony",queen.getUUID());materialTag.putString("Placement","right");materialTag.putInt("InventoryFormat",2);
        var stone=new ArrayList<ItemStack>();for(int n=0;n<stoneUnits;n++)stone.add(new ItemStack(Items.STONE));if(stoneUnits==1){for(int n=0;n<31;n++)stone.add(new ItemStack(Items.COBBLESTONE));cobble=31;}materialTag.put("Contents",ItemStack.CODEC.listOf().encodeStart(NbtOps.INSTANCE,stone).getOrThrow());SurfaceFixture.loadBlock(c,material,materialTag);
        var registry=ChamberRegistry.get(l);registry.found(queen.getUUID(),home);
        registry.register(queen.getUUID(),new ChamberRegistry.Chamber(ChamberExcavation.STORE,store.min(),store.max(),Set.of(ChamberFunction.MATERIAL_STORE),2,Map.of(ChamberFunction.MATERIAL_STORE,store.marker())));
        registry.register(queen.getUUID(),new ChamberRegistry.Chamber(ChamberExcavation.HALL,hall.min(),hall.max(),Set.of(ChamberFunction.QUEENS_HALL),2,Map.of()));
        for(var chamber:registry.colony(queen.getUUID()).chambers())for(var at:ChamberUpgrade.walls(home,chamber))if(!opened.contains(at)){
            l.setBlock(at,NurseryBlocks.PACKED_CLAY.defaultBlockState(),3);ColonyTerrain.get(l).built(at,queen.getUUID(),NurseryBlocks.PACKED_CLAY);
        }
        InitialSurfaceHabitat.declare(c,queen.getUUID(),opened);c.assertTrue(l.addFreshEntity(queen),"Insert the real initial queen");
        for(int n=0;n<count;n++){
            UUID brood=broodIds.get(n);var w=AntEntities.WORKER.create(l,EntitySpawnReason.COMMAND);c.assertTrue(w!=null,"Real controlled worker");w.setUUID(ColonyMembers.workerId(brood));w.initializeCallow(brood,queen.getUUID(),home.chamber());
            w.setPos(Vec3.atBottomCenterOf(home.at(3+n%3,n%2==0?-1:0,-2)));c.assertTrue(l.addFreshEntity(w),"Insert real registered worker");ColonyMembers.get(l).record(brood,queen.getUUID(),home.chamber());
            c.assertTrue(w.nutrition().ingest(new ItemStack(Items.APPLE),Nutrition.QUEEN_SUGAR_CAPACITY,Nutrition.QUEEN_PROTEIN_CAPACITY),"Declared initial ingested apple");
            if(hungry){
                // Controlled PRE-BOUNDARY history only: one prior meal has been fully spent on maintenance.
                // Default life/food gates remain; all subsequent meals and eggs need real workers.
                c.assertTrue(w.nutrition().spend(4000,0),"Historical initial adult maintenance");
                var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());c.assertTrue(w.save(saved),"Declared existing adult history");
                var adultTag=saved.buildResult();adultTag.putLong("AntElapsedAgeTicks",28000);adultTag.putLong("CallowTicks",BroodPile.callowTicks());
                var life=adultTag.getCompound("AdultLife").orElseThrow();life.putBoolean("Started",true);life.putLong("Fasting",12000);life.putLong("Maintenance",4000);life.putLong("CoveredTicks",16000);life.putLong("ActiveTicks",28000);life.putInt("CoverageRemaining",0);
                w.load(TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),adultTag));
                c.assertTrue(!w.isCallow()&&w.adultLife().hungry()&&w.nutrition().sugar()==0&&w.elapsedAgeTicks()==28000,"Explicit pre-existing hungry adult with coherent spent meal");
            }
        }
        c.assertTrue(queen.nutrition().ingest(new ItemStack(Items.APPLE),Nutrition.QUEEN_SUGAR_CAPACITY,Nutrition.QUEEN_PROTEIN_CAPACITY)&&queen.nutrition().ingest(new ItemStack(Items.CHICKEN),Nutrition.QUEEN_SUGAR_CAPACITY,Nutrition.QUEEN_PROTEIN_CAPACITY),"Declared queen's existing ingested apple/chicken");
        c.assertTrue(ChamberExcavation.get(l).jobs(queen.getUUID()).isEmpty()&&ChamberUpgrade.get(l).jobs(queen.getUUID()).isEmpty()&&queen.founding().removed()==0,"Initial habitat manufactures no excavation or upgrade work receipts");

        initialWalls.addAll(NurseryUpgradeGameTest.allWalls(c,queen));this.stone=stoneUnits;
        c.assertTrue(initialWalls.stream().allMatch(at->ColonyTerrain.get(l).retainedClay(l,at,queen.getUUID())),"Every initial wall is explicitly pre-existing paid tier two");
        c.assertTrue(initialWalls.stream().noneMatch(at->l.getBlockState(at).is(NurseryBlocks.NEST_CUT_STONE)),"No initial tier-three walls");
        c.assertTrue(MoundSoil.cells(l,home,queen.getUUID()).stream().noneMatch(at->ColonyTerrain.get(l).mound(l,at,queen.getUUID())||ColonyTerrain.get(l).surface(l,at,queen.getUUID())),"No recoverable surface construction supplies in this chamber fixture");
        PrimeAnts.LOGGER.info("T3 ACCEPTANCE BOUNDARY queen={} workers=49 initialClayCores={} initialStone={} initialCache=8 initialIngestedApples=50 initialIngestedChicken=1 tierThreeCells=0 upgradeReceipts=0 hungryAdults={} historicalSpentSugarPerWorker={}",queen.getUUID(),initialWalls.size(),stoneUnits,hungry,hungry?4000:0);
        return queen;
    }
    void drop(GameTestHelper c,LasiusNigerEntity q,Item item,int n){
        var at=q.founding().plan().at(-4,0,1);c.assertTrue(NestPlan.walkable(c.getLevel(),at),"Existing supported contribution point");
        int remaining=n;
        while(remaining>0){int count=Math.min(remaining,new ItemStack(item).getMaxStackSize());fx.drop(c,at,new ItemStack(item,count));remaining-=count;}
        if(item==Items.APPLE)apples+=n;else if(item==Items.CHICKEN)chickens+=n;else if(item==Items.STONE)stone+=n;else if(item==Items.CLAY_BALL)clay+=n;else if(item==Items.COBBLESTONE)cobble+=n;
    }
    void feed(GameTestHelper c,LasiusNigerEntity q){
        var paid=ChamberUpgrade.get(c.getLevel()).jobs(q.getUUID()).stream().filter(j->j.tier==3).mapToInt(j->j.built().size()).sum();
        if(!constructionWave&&stone==32&&paid==32&&loose(c,q,Items.STONE)==0){constructionWave=true;drop(c,q,Items.STONE,12);}
        if(!hungryInitialAdults)return; // Well-fed safety bodies need no additional food before their bounded endpoints.
        if(c.getTick()-lastFood<200)return;lastFood=c.getTick();var cache=fx.f.cache(c,q);
        if(cache==null)return;
        if(NurseryUpgradeGameTest.ground(c,Items.APPLE)==0&&cache.contents().stream().filter(s->s.is(Items.APPLE)).count()<=4)drop(c,q,Items.APPLE,4);
        if(NurseryUpgradeGameTest.ground(c,Items.CHICKEN)==0&&cache.contents().stream().filter(s->s.is(Items.CHICKEN)).count()<4)drop(c,q,Items.CHICKEN,2);
    }
    List<LasiusNigerEntity> workers(GameTestHelper c,LasiusNigerEntity q){return fx.f.workers(c,q);}
    long loose(GameTestHelper c,LasiusNigerEntity q,Item item){
        var box=c.getBounds().inflate(8);var l=c.getLevel();var store=MaterialStore.owned(l,q.getUUID(),q.founding().plan());
        return NurseryUpgradeGameTest.ground(c,item)+workers(c,q).stream().filter(w->w.getMainHandItem().is(item)).mapToInt(w->w.getMainHandItem().getCount()).sum()
            +(store==null?0:store.contents().stream().filter(s->s.is(item)).count())
            +TransferCustody.get(l).contents().stream().filter(t->t.stack().is(item)&&box.contains(t.position())).mapToInt(t->t.stack().getCount()).sum();
    }
    void ledger(GameTestHelper c,LasiusNigerEntity q){
        var l=c.getLevel();var terrain=ColonyTerrain.get(l);var jobs=ChamberUpgrade.get(l).jobs(q.getUUID());
        long paid=jobs.stream().filter(j->j.tier==3).mapToLong(j->j.built().size()).sum();
        c.assertTrue(loose(c,q,Items.STONE)+paid==stone,"Every facing stone is ground, cargo, store, paid cell or pending custody: "+loose(c,q,Items.STONE)+"+"+paid+"="+stone);
        c.assertTrue(loose(c,q,Items.CLAY_BALL)==clay,"No added clay disappears into tier three");
        c.assertTrue(loose(c,q,Items.COBBLESTONE)==cobble,"Cobblestone is never facing feedstock");
        var widening=NestExpansion.get(l).job(q.getUUID());var mining=Mining.get(l).job(q.getUUID());
        long claims=jobs.stream().filter(j->j.claim!=null).count()+ChamberExcavation.get(l).jobs(q.getUUID()).stream().filter(j->j.claim!=null).count()
            +SurfaceWork.get(l).jobs(q.getUUID()).stream().filter(j->j.claim!=null).count()+(widening!=null&&widening.claim!=null?1:0)+(mining!=null&&mining.claim!=null?1:0);
        c.assertTrue(claims<=1,"Single builder across every work owner");
        if(claims>0)c.assertTrue(TierTwoFixture.caregivers(c,q)>=2,"Two valid caregivers including retained UNKNOWN roles");
        for(var j:jobs){
            var w=j.claim==null?null:l.getEntity(j.claim);int cargo=w instanceof LasiusNigerEntity a&&j.accepts(a.getMainHandItem())?1:0;
            c.assertTrue(j.ledger().exact()&&j.carried()==cargo,"Material-aware exact job ledger and canonical equipment: "+j.ledger());
            int beforeTaken=observedTaken.getOrDefault(j.chamber,0),beforeBuilt=observedBuilt.getOrDefault(j.chamber,0);
            if(j.taken()>beforeTaken){
                c.assertTrue(j.taken()==beforeTaken+1&&w instanceof LasiusNigerEntity a&&j.accepts(a.getMainHandItem())&&WorkerTasks.reaches(l,a,Vec3.atBottomCenterOf(MaterialStore.owned(l,q.getUUID(),q.founding().plan()).getBlockPos()).add(0,0.15,0)),"Observed one-unit physical store withdrawal into actual reachable builder equipment");
                observedCargo.add(j.claim);withdrawals++;
            }
            if(j.built().size()>beforeBuilt){
                c.assertTrue(j.built().size()==beforeBuilt+1&&w instanceof LasiusNigerEntity a&&observedCargo.contains(a.getUUID())&&a.getMainHandItem().isEmpty()&&a.onGround()&&NestPlan.walkable(l,a.blockPosition())&&a.position().distanceToSqr(Vec3.atCenterOf(j.built().getLast()))<=5.0,"Previously observed stone cargo became a real wall from an actual supported stand within unchanged reach");
                conversions++;var a=(LasiusNigerEntity)w;
                PrimeAnts.LOGGER.info("T3 SUPPORTED CONVERSION queen={} worker={} tick={} cell={} stand={} position={} recipe={} retainedClay=1 paid={}",q.getUUID(),a.getUUID(),c.getTick(),j.built().getLast(),a.blockPosition(),a.position(),j.material,j.built().size());
            }
            observedTaken.put(j.chamber,j.taken());observedBuilt.put(j.chamber,j.built().size());
            for(var at:j.built())c.assertTrue(terrain.built(l,at,q.getUUID(),j.block())&&terrain.retainedClay(l,at,q.getUUID()),"Actual owned facing retains its paid clay core");
        }
        if(c.getTick()%500==0)PrimeAnts.LOGGER.info("T3 CURVE queen={} tick={} stone={} jobs={} nursery={} food={} material={} gate={} supply={}",q.getUUID(),c.getTick(),stone,jobs.stream().map(j->j.chamber+":"+j.tier+":"+j.reason+":"+j.ledger()).toList(),NestPlanFixture.pile(c,q).records().size(),fx.f.cache(c,q).size(),MaterialStore.owned(l,q.getUUID(),q.founding().plan()).size(),NestPlanFixture.pile(c,q).growth().reason(),NestPlanFixture.pile(c,q).supply(l));
    }
    boolean complete(GameTestHelper c,LasiusNigerEntity q){
        var l=c.getLevel();var jobs=ChamberUpgrade.get(l).jobs(q.getUUID());
        return NestPlanFixture.pile(c,q).stageEvaluation()!=null&&Arrays.stream(ChamberFunction.values()).allMatch(f->NestPlanFixture.pile(c,q).stageEvaluation().inputs().tier(f).known()==3)&&jobs.size()==3&&jobs.stream().allMatch(j->j.tier==3&&j.complete()&&j.claim==null&&j.carried()==0)
            &&Arrays.stream(ChamberFunction.values()).allMatch(f->ColonyDevelopment.confirmedTier(l,q.getUUID(),q.founding().plan(),f==ChamberFunction.NURSERY||f==ChamberFunction.FOOD_STORE?ChamberRegistry.FOUNDING:f==ChamberFunction.MATERIAL_STORE?ChamberExcavation.STORE:ChamberExcavation.HALL,f)==3);
    }
}
