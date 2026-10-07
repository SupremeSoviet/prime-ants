package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.QueenFounding;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Stage-1 T06 in real loaded ticks: the food cache keeps two slots for each kind (FoodShares), so a cache full of one
 * kind never keeps the other out. Adults eat only sugar and larvae need protein. Production founding, foraging, nursing
 * and adult meals; the player only drops food. */
public final class FoodCacheGameTest {
    private final WorkerForagingGameTest f = new WorkerForagingGameTest();
    private final NursingGameTest food = new NursingGameTest();
    /** This colony's adults fast at most GRACE loaded ticks before starving (birth-selected, as the lifecycle tests do). */
    static final long GRACE = 12_000;
    private LasiusNigerEntity start(GameTestHelper c) {
        var keys = List.of("prime_ants.adultFastingTicks"); var old = keys.stream().map(System::getProperty).toList();
        try { System.setProperty(keys.getFirst(), Long.toString(GRACE)); return f.start(c); }
        finally { if (old.getFirst() == null) System.clearProperty(keys.getFirst()); else System.setProperty(keys.getFirst(), old.getFirst()); }
    }
    static long count(NestCache n, net.minecraft.world.item.Item item) { return n == null ? 0 : n.contents().stream().filter(s -> s.is(item)).count(); }

    /** A player drops twelve chickens as the nest opens and twelve apples 3,000 ticks later, by when a cache without
     * shares would hold six chickens and its forager a seventh it could never store (no larvae eat them: the colony has
     * no sugar to lay on). The cache never takes a fifth chicken and the forager leaves the rest on the ground; apples
     * still get in, so the cache holds both kinds, adults eat apples from it, and for a whole fasting grace after the
     * apples land no adult starves. Every dropped unit stays physical: on the ground, carried, cached, in custody or eaten. */
    @GameTest(maxTicks=30000,structure="prime_ants_test:idle_ground")
    public void cacheFullOfChickensStillTakesApplesAndNoAdultStarves(GameTestHelper c){
        var q=start(c);long[] chickensAt={-1},applesAt={-1},bothAt={-1};long[] meals={0};Set<UUID> starved=new HashSet<>(),waiting=new HashSet<>();Map<UUID,Long> apples=new HashMap<>();
        c.onEachTick(()->{
            var l=c.getLevel();var p=q.founding().plan();if(p==null)return;
            c.assertTrue(q.founding().phase()!=QueenFounding.Phase.FAILED,"Production founding stays valid");
            var n=f.cache(c,q);var ws=f.workers(c,q);
            if(chickensAt[0]<0&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){chickensAt[0]=c.getTick();food.supply(c,q,0,12);PrimeAnts.LOGGER.info("T06 FOOD SHARES chickens dropped queen={} tick={}",q.getUUID(),c.getTick());}
            if(chickensAt[0]<0)return;
            c.assertTrue(food.total(c,q)==12+(applesAt[0]<0?0:12),"Every dropped unit is on the ground, carried, cached, in custody or eaten: "+food.total(c,q));
            c.assertTrue(count(n,Items.CHICKEN)<=FoodShares.share(FoodShares.CAPACITY)&&count(n,Items.APPLE)<=FoodShares.share(FoodShares.CAPACITY),"Neither kind fills more than its share: "+(n==null?null:n.contents()));
            if(applesAt[0]<0&&c.getTick()-chickensAt[0]>=3000){
                c.assertTrue(c.getLevel().getEntitiesOfClass(ItemEntity.class,c.getBounds().inflate(8),i->i.isAlive()&&i.getItem().is(Items.CHICKEN)).stream().mapToInt(i->i.getItem().getCount()).sum()>0
                    &&ws.stream().noneMatch(w->w.getMainHandItem().is(Items.CHICKEN)&&w.workerTasks().reason().equals("cache_full_blocked_or_foreign_cargo_retained")),"Chickens beyond the share wait on the ground, not in a forager's mandibles: "+(n==null?null:n.contents()));
                applesAt[0]=c.getTick();food.supply(c,q,12,0);ws.forEach(w->waiting.add(w.getUUID()));PrimeAnts.LOGGER.info("T06 FOOD SHARES apples dropped queen={} tick={} cache={} adults={}",q.getUUID(),c.getTick(),n.contents(),ws.size());
            }
            if(applesAt[0]<0)return;
            // A forager never waits with a chicken while the protein share is full.
            for(var w:ws)if(w.getMainHandItem().is(Items.CHICKEN)&&w.workerTasks().reason().equals("cache_full_blocked_or_foreign_cargo_retained"))
                c.assertTrue(count(n,Items.CHICKEN)<FoodShares.share(FoodShares.CAPACITY),"No forager holds a chicken the cache's protein share cannot take");
            if(bothAt[0]<0&&count(n,Items.APPLE)>0&&count(n,Items.CHICKEN)>0){bothAt[0]=c.getTick();PrimeAnts.LOGGER.info("T06 FOOD SHARES cache holds both kinds queen={} tick={} cache={}",q.getUUID(),c.getTick(),n.contents());}
            for(var w:ws){long a=w.nutrition().apples();if(a>apples.getOrDefault(w.getUUID(),0L))meals[0]+=a-apples.getOrDefault(w.getUUID(),0L);apples.put(w.getUUID(),a);}
            // Every adult alive when the apples landed is fed (its fast ends) within half a grace, by a meal or a shared crop.
            for(var w:ws)if(w.adultLife().fasting()==0)waiting.remove(w.getUUID());
            if(c.getTick()-applesAt[0]>=GRACE/2)c.assertTrue(waiting.isEmpty(),"Every adult alive when the apples landed was fed within half a grace: unfed="+waiting);
            for(var m:ColonyMembers.get(l).members(q.getUUID()))if(m.dead()&&AdultHistory.get(l).records().get(m.worker().toString()) instanceof String row
                &&com.google.gson.JsonParser.parseString(row).getAsJsonObject().get("cause").getAsString().equals("starvation"))starved.add(m.worker());
            c.assertTrue(starved.isEmpty(),"No adult starves: "+starved);
            if(c.getTick()%1000==0)PrimeAnts.LOGGER.info("T06 FOOD SHARES tick={} cache={} adults={} meals={} fasting={}",c.getTick(),n==null?null:n.contents(),ws.size(),meals[0],ws.stream().map(w->w.adultLife().fasting()).toList());
            if(bothAt[0]<0||c.getTick()-applesAt[0]<GRACE)return;
            // A whole fasting grace after the apples were dropped: adults ate apples from the cache and every living adult is
            // fed. (One forager's apple feeds several nestmates through crop sharing, so meals need not reach the adult count.)
            c.assertTrue(meals[0]>=1&&ws.stream().allMatch(w->w.adultLife().fasting()<GRACE),"Adults eat the apples beside the chickens and none is starving: meals="+meals[0]+" adults="+ws.size());
            PrimeAnts.LOGGER.info("T06 FOOD SHARES DONE queen={} tick={} chickensAt={} applesAt={} bothAt={} meals={} adults={} cache={}",q.getUUID(),c.getTick(),chickensAt[0],applesAt[0],bothAt[0],meals[0],ws.size(),n.contents());
            c.succeed();
        });
    }
}
