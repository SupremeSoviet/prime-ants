package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import dev.primeants.colony.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Stage-1 T07: foragers scale with the colony (Foragers: one per ten living workers, at least one, caregivers and the
 * builder slot kept). Real loaded ticks on the T04 nest-plan fixture: production founding, digging, foraging and hauling;
 * a player only drops food and clay behind the entrance. */
public final class ForagerScalingGameTest {
    private final NestPlanFixture fx = new NestPlanFixture();
    static List<LasiusNigerEntity> foragers(GameTestHelper c, NestPlanFixture fx, LasiusNigerEntity q) { return fx.f.workers(c, q).stream().filter(w -> q.founding().claimedBy(w)).toList(); }
    static long cached(NestCache n, net.minecraft.world.item.Item item) { return n == null ? 0 : n.contents().stream().filter(s -> s.is(item)).count(); }
    /** Finite player refills include food on the ground and in every worker's cargo. One free delivery slot and a
     * per-kind margin keep pending food from blocking both foragers; every unit still travels physically. */
    long onDemand(GameTestHelper c, LasiusNigerEntity q, long[] last) {
        var cache = fx.f.cache(c, q); long tick = c.getTick();
        var pending = new ArrayList<ItemStack>(); if (cache != null) pending.addAll(cache.contents());
        fx.f.workers(c,q).stream().map(w->w.getMainHandItem()).filter(WorkerTasks::food).forEach(pending::add);
        var box=c.getBounds().inflate(8);
        c.getLevel().getEntitiesOfClass(ItemEntity.class,box,i->i.isAlive()&&WorkerTasks.food(i.getItem())).forEach(i->pending.add(i.getItem()));
        TransferCustody.get(c.getLevel()).contents().stream().filter(t->box.contains(t.position())&&WorkerTasks.food(t.stack())).forEach(t->pending.add(t.stack()));
        int capacity=cache==null?NestCache.CAPACITY:cache.capacity(), target=capacity-1, proteinTarget=FoodShares.reserved(capacity), sugarTarget=target-proteinTarget;
        long total=pending.stream().mapToLong(ItemStack::getCount).sum();
        long protein=pending.stream().filter(NestCache::protein).mapToLong(ItemStack::getCount).sum(), sugar=total-protein;
        if(total>=target)return 0;
        if(sugar<sugarTarget&&tick-last[0]>=200){last[0]=tick;fx.food.supply(c,q,1,0);return 1;}
        if(protein<proteinTarget&&tick-last[1]>=200){last[1]=tick;fx.food.supply(c,q,0,1);return 1;}
        return 0;
    }

    /** (f2) A colony of twenty workers or more claims a second forager, so two or more claimed foragers carry at once.
     * Every food unit and every clay ball is exactly accounted at every tick, each further claim leaves the colony its
     * caregivers, and a reload of the queen and of each carrying forager keeps every claim, cargo and phase; the restored
     * carriers then deliver their units. Food on demand; once two foragers are claimed and the store is confirmed, eight
     * clay balls once both foragers have actually delivered their food and are searching, with no food dropped while
     * clay lies on the ground (food first would keep both foragers on food). Opening food is three apples/two chickens;
     * later finite refills count ground/cache/cargo/custody and leave a physical delivery margin. */
    @GameTest(maxTicks=30000,structure="prime_ants_test:carrier_route")
    public void largeColonyForagersCarryAtOnceWithExactAccountingAndClaimsSurviveReload(GameTestHelper c) { simultaneousCarriers(c); }
    private void simultaneousCarriers(GameTestHelper c) {
        LasiusNigerEntity[] q = {fx.start(c)}; boolean[] opened = {false}; long[] dropped = {0}, last = {0, 0}, claimedAt = {-1}; int[] step = {0}, most = {0};
        List<UUID> claims = new ArrayList<>(); Map<UUID, ItemStack> carried = new HashMap<>(); Set<UUID> delivered = new HashSet<>(); boolean[] clay = {false}, simultaneousClay = {false};
        c.onEachTick(() -> {
            var l = c.getLevel();
            if (!opened[0]) { if(q[0].founding().lifecycle()==dev.primeants.founding.QueenFounding.Lifecycle.OPEN){opened[0]=true;fx.food.supply(c,q[0],3,2);dropped[0]+=5;} return; }
            var p = NestPlanFixture.pile(c, q[0]); var store = NestPlanFixture.store(c, q[0]);
            boolean clayWaits = clay[0] && NurseryUpgradeGameTest.ground(c, Items.CLAY_BALL) > 0 && store != null && store.room(new ItemStack(Items.CLAY_BALL));
            boolean preparingClay=!clay[0]&&q[0].founding().foragers().size()>=2&&NestPlanFixture.storeConfirmed(p==null?null:p.stageEvaluation())&&store!=null;
            if (!clayWaits&&!preparingClay) dropped[0] += onDemand(c, q[0], last);
            fx.soil(c, q[0]);
            c.assertTrue(fx.food.total(c, q[0]) == dropped[0], "Every food unit dropped is on the ground, carried, cached, in custody or eaten: " + fx.food.total(c, q[0]) + " of " + dropped[0]);
            if (clay[0]) c.assertTrue(NurseryUpgradeGameTest.clay(c, fx, q[0]) == 8, "All 8 dropped clay balls are on the ground, carried, stored, in the colony's walls or in custody: " + NurseryUpgradeGameTest.clay(c, fx, q[0]));
            long workers = ColonyMembers.get(l).occupied(q[0].getUUID()); var all = q[0].founding().foragers(); var ws = foragers(c, fx, q[0]);
            c.assertTrue(all.size() <= Foragers.target(workers), "Never more forager claims than the colony keeps: " + all.size() + " for " + workers + " workers");
            if (all.size() > claims.size() && all.size() >= 2) {
                // A further claim: it is a living member claimed by this queen, and the colony keeps two caregivers beside it.
                long caregivers = fx.f.workers(c, q[0]).stream().filter(w -> w.workerTasks().caregiver(l, q[0].founding().plan())).count();
                c.assertTrue(all.size() == claims.size() + 1 && all.containsAll(claims) && caregivers >= Foragers.CAREGIVERS_KEPT && workers >= Foragers.WORKERS_PER_FORAGER * all.size(),
                    "One more forager at a time, for every ten workers, with two caregivers kept: " + all + " workers=" + workers + " caregivers=" + caregivers);
                if (all.size() == 2) claimedAt[0] = c.getTick();
                PrimeAnts.LOGGER.info("T07 F2 FORAGER CLAIMED queen={} tick={} foragers={} workers={} caregivers={}", q[0].getUUID(), c.getTick(), all, workers, caregivers);
            }
            claims.clear(); claims.addAll(all);
            var carriers = ws.stream().filter(w -> !w.getMainHandItem().isEmpty()).toList(); most[0] = Math.max(most[0], carriers.size());
            if (preparingClay && ws.size()==all.size() && ws.stream().allMatch(w->w.getMainHandItem().isEmpty()&&w.workerTasks().phase()==WorkerTasks.Phase.SEARCH)
                && NurseryUpgradeGameTest.ground(c,Items.APPLE)==0 && NurseryUpgradeGameTest.ground(c,Items.CHICKEN)==0) {
                var plan = q[0].founding().plan();
                // A declared supported L route, wholly inside this fixture and the unchanged search box. Three steps
                // climb above the existing mound; no nest/mound cell is removed. A longer real carrying leg lets the
                // half-cycle-offset foragers overlap without assigning cargo, moving bodies or changing their AI.
                var outside=plan.outside();
                for(int x=1;x<=7;x++)runway(c,q[0],outside.offset(x,Math.min(x,3)-1,0));
                for(int z=1;z<=7;z++)runway(c,q[0],outside.offset(7,2,z));
                var drop=outside.offset(7,3,7);
                c.assertTrue(c.getBounds().contains(net.minecraft.world.phys.Vec3.atCenterOf(drop))&&ws.stream().allMatch(w->w.workerTasks().withinSearch(drop)),"The supported supply remains inside this fixture and both foragers' existing search box");
                clay[0] = true; fx.drop(c, drop, new ItemStack(Items.CLAY_BALL, 8));
                PrimeAnts.LOGGER.info("T07 F2 CLAY DROPPED queen={} tick={} foragers={} workers={}", q[0].getUUID(), c.getTick(), all, workers);
            }
            if (!simultaneousClay[0] && carriers.stream().filter(w -> w.getMainHandItem().is(Items.CLAY_BALL)).count() >= 2) {
                simultaneousClay[0] = true;
                PrimeAnts.LOGGER.info("T08 F2 TWO CLAIMED CLAY CARRIERS queen={} tick={} carriers={}",q[0].getUUID(),c.getTick(),carriers.stream().filter(w -> w.getMainHandItem().is(Items.CLAY_BALL)).map(w -> w.getUUID()+":"+w.getMainHandItem()).toList());
            }
            if (c.getTick() % 500 == 0) PrimeAnts.LOGGER.info("T07 F2 trace tick={} workers={} foragers={} carriers={} most={} cache={} store={} clayWaits={}", c.getTick(), workers, all.size(),
                carriers.stream().map(w -> w.getMainHandItem().toString()).toList(), most[0], fx.f.cache(c, q[0]) == null ? null : fx.f.cache(c, q[0]).contents(), store == null ? null : store.contents(), clayWaits);
            if (step[0] == 0) {
                if (carriers.size() < 2) return;
                // Two or more claimed foragers carry at once: reload the queen and each carrier from their saves.
                var saved = List.copyOf(all); long before = fx.food.total(c, q[0]);
                for (var w : carriers) carried.put(w.getUUID(), w.getMainHandItem().copy());
                var phases = new HashMap<UUID, WorkerTasks.Phase>(); for (var w : carriers) phases.put(w.getUUID(), w.workerTasks().phase());
                q[0] = fx.f.restore(c, q[0]);
                for (var w : carriers) {
                    var back = fx.f.restore(c, w);
                    c.assertTrue(q[0].founding().claimedBy(back) && ItemStack.matches(back.getMainHandItem(), carried.get(w.getUUID())) && back.workerTasks().phase() == phases.get(w.getUUID()),
                        "A restored carrier keeps its claim, its cargo and its phase: " + back.getUUID());
                }
                c.assertTrue(q[0].founding().foragers().equals(saved) && fx.food.total(c, q[0]) == before, "The restored queen keeps every forager claim in order, and no unit is lost or doubled: " + q[0].founding().foragers() + " " + saved);
                PrimeAnts.LOGGER.info("T07 F2 RELOAD queen={} tick={} foragers={} carriers={} workers={} claimedAt={}", q[0].getUUID(), c.getTick(), saved, carried, workers, claimedAt[0]);
                step[0] = 1; return;
            }
            // After the reload each restored carrier still holds its claim until it has delivered its unit.
            for (var id : carried.keySet()) {
                if (delivered.contains(id)) continue;
                var w = l.getEntity(id) instanceof LasiusNigerEntity a ? a : null;
                c.assertTrue(w != null && w.isAlive() && q[0].founding().claimedBy(w), "A restored carrier keeps its claim: " + id);
                if (!ItemStack.matches(w.getMainHandItem(), carried.get(id))) delivered.add(id);
            }
            if (delivered.size() < carried.size() || !simultaneousClay[0]) return;
            c.assertTrue(simultaneousClay[0],"Two claimed foragers carried clay on the same tick");
            PrimeAnts.LOGGER.info("T07 F2 DELIVERED AFTER RELOAD queen={} tick={} carriers={} mostAtOnce={} foragers={} workers={}", q[0].getUUID(), c.getTick(), carried.keySet(), most[0], q[0].founding().foragers(), workers);
            c.succeed();
        });
    }

    /** Fixture route only: stone floors are never material items or colony excavations. Preserve recorded mound soil. */
    private static void runway(GameTestHelper c,LasiusNigerEntity q,net.minecraft.core.BlockPos floor){
        var l=c.getLevel();var terrain=dev.primeants.founding.ColonyTerrain.get(l);
        c.assertTrue(!terrain.mound(l,floor.above(),q.getUUID())&&!terrain.mound(l,floor.above(2),q.getUUID()),"The route clears no recorded mound soil");
        if(!terrain.mound(l,floor,q.getUUID()))l.setBlock(floor,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        l.setBlock(floor.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
        l.setBlock(floor.above(2),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
        c.assertTrue(dev.primeants.founding.NestPlan.walkable(l,floor.above()),"Every runway cell is supported and two-high: "+floor);
    }

    /** (f3) With scaled foragers a colony grows past thirty adults, beyond the one-forager plateau of 24 to 28 adults and
     * beyond Young's cap: it reaches Mature by real play, then grows on. Fed as a player would: food on demand; sixteen clay
     * balls once the colony has two foragers and nears Young's cap (26 adults), with no food dropped while that clay lies
     * on the ground and the store has room; then food on demand again. */
    @GameTest(maxTicks=60000,structure="prime_ants_test:idle_ground")
    public void scaledForagersGrowColonyPastThirtyAdults(GameTestHelper c) {
        var q = fx.start(c); boolean[] opened = {false}, clay = {false}; long[] dropped = {0}, last = {0, 0}, mature = {-1}, thirty = {-1}; int[] most = {0};
        c.onEachTick(() -> {
            var l = c.getLevel();
            if (!opened[0]) { if(q.founding().lifecycle()==dev.primeants.founding.QueenFounding.Lifecycle.OPEN){opened[0]=true;fx.food.supply(c,q,3,2);dropped[0]+=5;} return; }
            var p = NestPlanFixture.pile(c, q); var store = NestPlanFixture.store(c, q); var e = p == null ? null : p.stageEvaluation();
            boolean clayWaits = clay[0] && NurseryUpgradeGameTest.ground(c, Items.CLAY_BALL) > 0 && store != null && store.room(new ItemStack(Items.CLAY_BALL));
            if (!clayWaits) dropped[0] += onDemand(c, q, last);
            fx.soil(c, q);
            c.assertTrue(fx.food.total(c, q) == dropped[0], "Every food unit dropped is on the ground, carried, cached, in custody or eaten: " + fx.food.total(c, q) + " of " + dropped[0]);
            if (clay[0]) c.assertTrue(NurseryUpgradeGameTest.clay(c, fx, q) == 16, "All 16 dropped clay balls are on the ground, carried, stored, in the colony's walls or in custody: " + NurseryUpgradeGameTest.clay(c, fx, q));
            long workers = ColonyMembers.get(l).occupied(q.getUUID()); int claims = q.founding().foragers().size(); most[0] = Math.max(most[0], claims);
            c.assertTrue(claims <= Foragers.target(workers), "Never more forager claims than the colony keeps: " + claims + " for " + workers);
            if (e == null) return;
            long adults = e.inputs().adults().known();
            if (!clay[0] && claims >= 2 && adults >= 26) {
                clay[0] = true; fx.drop(c, q.founding().plan().at(-5, 0, 1), new ItemStack(Items.CLAY_BALL, 16));
                PrimeAnts.LOGGER.info("T07 F3 CLAY DROPPED queen={} tick={} adults={} foragers={}", q.getUUID(), c.getTick(), adults, claims);
            }
            if (mature[0] < 0 && e.stage() == ColonyStage.MATURE) {
                c.assertTrue(e.result().certain() == ColonyStage.MATURE && e.inputs().clay().known() >= 16 && e.inputs().food().known() >= 4 && adults >= 25, "Promoted to Mature by what it holds: " + e);
                mature[0] = c.getTick(); PrimeAnts.LOGGER.info("T07 F3 MATURE queen={} tick={} adults={} foragers={} evaluation={}", q.getUUID(), c.getTick(), adults, claims, e);
            }
            if (adults >= 30 && thirty[0] < 0) { thirty[0] = c.getTick(); PrimeAnts.LOGGER.info("T07 F3 THIRTY ADULTS queen={} tick={} stage={} foragers={} workers={}", q.getUUID(), c.getTick(), e.stage(), claims, workers); }
            if (c.getTick() % 1000 == 0) PrimeAnts.LOGGER.info("T07 F3 trace tick={} adults={} stage={} foragers={} brood={} gate={} cache={} storeClay={} groundClay={}", c.getTick(), e.inputs().adults(), e.stage(), claims,
                p.records().size(), p.growth().reason(), fx.f.cache(c, q) == null ? null : fx.f.cache(c, q).contents(), store == null ? -1 : store.units(MaterialUnits.Material.CLAY), NurseryUpgradeGameTest.ground(c, Items.CLAY_BALL));
            if (adults <= 30) return;
            c.assertTrue(e.stage() == ColonyStage.MATURE && ChamberRegistry.get(l).colony(q.getUUID()).stage() == ColonyStage.MATURE && e.cap() == ColonyStage.MATURE.adultCap() && mature[0] >= 0 && most[0] >= 2,
                "Past thirty adults the colony is Mature, with its cap of 60, and it has had two or more foragers: " + e + " foragers=" + most[0]);
            PrimeAnts.LOGGER.info("T07 F3 PAST THIRTY queen={} tick={} adults={} matureAt={} thirtyAt={} mostForagers={} foragers={} workers={}", q.getUUID(), c.getTick(), adults, mature[0], thirty[0], most[0], claims, workers);
            c.succeed();
        });
    }
}
