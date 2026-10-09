package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import dev.primeants.brood.*;
import dev.primeants.colony.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.util.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;

/** Player-style supply waves with separate physical food/clay/soil ledgers. No jobs, cargo, adults or progress assigned. */
final class TierTwoFixture {
    final NestPlanFixture fx=new NestPlanFixture();
    final int extraClay;
    final boolean hallSugarWaves;
    long apples,chickens,clay,lastApple,lastChicken,lastWorkMeal,lastWorkProtein,materialEnd=-1;
    boolean opened,feeding;
    TierTwoFixture(int extra){this(extra,false);}
    TierTwoFixture(int extra,boolean hallSugarWaves){extraClay=extra;this.hallSugarWaves=hallSugarWaves;}
    void supply(GameTestHelper c,LasiusNigerEntity q,int sugar,int protein){fx.food.supply(c,q,sugar,protein);apples+=sugar;chickens+=protein;}
    void material(GameTestHelper c,LasiusNigerEntity q,int units){
        var at=q.founding().plan().at(-4,0,1);
        c.assertTrue(NestPlan.walkable(c.getLevel(),at),"A supported player clay drop: "+at);
        fx.drop(c,at,new ItemStack(Items.CLAY_BALL,units));clay+=units;
    }
    void feed(GameTestHelper c,LasiusNigerEntity q){
        if(!opened){if(q.founding().lifecycle()!=QueenFounding.Lifecycle.OPEN)return;opened=true;supply(c,q,12,10);}
        var p=NestPlanFixture.pile(c,q);var store=NestPlanFixture.store(c,q);var e=p==null?null:p.stageEvaluation();
        var cache=fx.f.cache(c,q);long tick=c.getTick();
        if(clay==0&&e!=null&&e.inputs().adults().known()>=25)material(c,q,16);
        if(clay==16&&e!=null&&e.inputs().tier(ChamberFunction.NURSERY).known()==2)material(c,q,extraClay);
        boolean waiting=clay>0&&NurseryUpgradeGameTest.ground(c,Items.CLAY_BALL)>0&&store!=null&&store.room(new ItemStack(Items.CLAY_BALL));
        if(waiting){
            // Feed in finite waves while a long physical haul continues. Between waves the food-first foragers
            // return to clay; withholding every apple until eighty clay are delivered can starve the colony.
            if(e!=null&&e.inputs().tier(ChamberFunction.NURSERY).known()==2&&tick-lastWorkMeal>=2000
                &&NurseryUpgradeGameTest.ground(c,Items.APPLE)==0){
                long sugar=q.nutrition().sugar()+fx.f.workers(c,q).stream().mapToLong(w->w.nutrition().sugar()).sum();
                if(sugar<24000){lastWorkMeal=tick;lastApple=tick;supply(c,q,8,0);PrimeAnts.LOGGER.info("T08 PLAYER WORK MEAL queen={} tick={} adultSugar={} clayStored={}",q.getUUID(),tick,sugar,store.size());}
            }
            if(e!=null&&e.inputs().tier(ChamberFunction.NURSERY).known()==2&&tick-lastWorkProtein>=2000
                &&NurseryUpgradeGameTest.ground(c,Items.CHICKEN)==0&&p.supply(c.getLevel()).storedProtein()<40000){
                long held=ForagerScalingGameTest.cached(cache,Items.CHICKEN)+incomingChicken(c,q);
                if(held<4){lastWorkProtein=tick;lastChicken=tick;supply(c,q,0,(int)Math.min(2,4-held));}
            }
            return;
        }
        if(clay>0&&!feeding){
            if(clay==16){
                // Keep Mature's four food units funded while holding apples back until the nursery is rebuilt.
                long held=ForagerScalingGameTest.cached(cache,Items.CHICKEN)+incomingChicken(c,q);
                if(NurseryUpgradeGameTest.ground(c,Items.CHICKEN)==0&&held<4&&tick-lastChicken>=200){lastChicken=tick;supply(c,q,0,(int)Math.min(2,4-held));}
                return;
            }
            if(materialEnd<0)materialEnd=tick;
            long sugar=q.nutrition().sugar()+fx.f.workers(c,q).stream().mapToLong(w->w.nutrition().sugar()).sum();
            // Work meals already supplied the finite dry-spell/wave pattern. Resume both kinds as the haul ends;
            // another pause here can put the protein refill beyond the fixed natural acceptance bound.
            feeding=true;PrimeAnts.LOGGER.info("T08 PLAYER MEAL WAVE queen={} tick={} adultSugar={} materialEnd={}",q.getUUID(),tick,sugar,materialEnd);
        }
        int proteinShare=feeding&&cache!=null?FoodShares.share(cache.capacity()):4;
        long heldApple=ForagerScalingGameTest.cached(cache,Items.APPLE),heldChicken=ForagerScalingGameTest.cached(cache,Items.CHICKEN);
        int appleTarget=feeding?4:0;
        if(hallSugarWaves&&feeding&&e!=null&&e.inputs().tier(ChamberFunction.QUEENS_HALL).known()==2){
            // A full cache and satiated adults stop new sugar receipts. Let the finite stock be consumed, then
            // drop a real twelve-apple meal wave. The unchanged growth gate still observes ingestion, not drops.
            if(NurseryUpgradeGameTest.ground(c,Items.APPLE)==0&&p.supply(c.getLevel()).storedSugar()<24000&&tick-lastApple>=2000){
                lastApple=tick;PrimeAnts.LOGGER.info("T08 PLAYER HALL SUGAR WAVE queen={} tick={} intake={}/{} supply={}",q.getUUID(),tick,p.growth().recentSugar(),p.growth().recentProtein(),p.supply(c.getLevel()));supply(c,q,12,0);
            }
        }else if(NurseryUpgradeGameTest.ground(c,Items.APPLE)==0&&heldApple<=appleTarget&&tick-lastApple>=200){lastApple=tick;supply(c,q,4,0);}
        heldChicken+=incomingChicken(c,q);
        if(NurseryUpgradeGameTest.ground(c,Items.CHICKEN)==0&&heldChicken<proteinShare&&tick-lastChicken>=200){lastChicken=tick;supply(c,q,0,(int)Math.min(2,proteinShare-heldChicken));}
    }
    private long incomingChicken(GameTestHelper c,LasiusNigerEntity q){return fx.f.workers(c,q).stream().filter(w->q.founding().claimedBy(w)&&w.getMainHandItem().is(Items.CHICKEN)).mapToInt(w->w.getMainHandItem().getCount()).sum();}
    /** Owner decision, 2026-10-09 15:25: an unknown habitat pauses work, but valid retained nurse roles count.
     * Every qualification except habitat authorization remains physical; DAMAGED keeps the ordinary active predicate. */
    static boolean retainedNurse(GameTestHelper c,LasiusNigerEntity q,LasiusNigerEntity w){
        var l=c.getLevel();var home=q.founding().plan();var tasks=w.workerTasks();var roleHome=tasks.plan();
        return home!=null&&home.nurseryFindings(l,q.getUUID(),true).verdict()==Findings.Verdict.UNKNOWN
            &&l.getEntity(q.getUUID())==q&&q.isAlive()&&q.founding().occupancyProblem(l)==null
            &&l.getEntity(w.getUUID())==w&&w.isAlive()&&!w.isRemoved()&&!w.isCallow()&&!w.isNoAi()
            &&NestPlan.loaded(l,w.blockPosition())&&l.isPositionEntityTicking(w.blockPosition())
            &&tasks.nursing()&&roleHome!=null&&roleHome.entrance().equals(home.entrance())&&roleHome.direction()==home.direction()
            &&ColonyMembers.get(l).belongs(w,q.getUUID(),home.chamber())
            &&!q.founding().claimedBy(w)&&DigJob.claimedBy(l,w)==null&&ChamberUpgrade.get(l).claimedBy(w)==null
            &&l.getBlockEntity(home.nursery()) instanceof BroodPile p&&p.ownedBy(q.getUUID(),home)&&p.operational();
    }
    static long caregivers(GameTestHelper c,LasiusNigerEntity q){
        var home=q.founding().plan();var unknown=home.nurseryFindings(c.getLevel(),q.getUUID(),true).verdict()==Findings.Verdict.UNKNOWN;
        return new WorkerForagingGameTest().workers(c,q).stream().filter(w->unknown?retainedNurse(c,q,w):w.workerTasks().caregiver(c.getLevel(),home)).count();
    }
    private long foodUnits(GameTestHelper c,LasiusNigerEntity q,Item item){
        var l=c.getLevel();var box=c.getBounds().inflate(8);var cache=fx.f.cache(c,q);var pile=NestPlanFixture.pile(c,q);
        long total=l.getEntitiesOfClass(ItemEntity.class,box,i->i.isAlive()&&i.getItem().is(item)).stream().mapToInt(i->i.getItem().getCount()).sum();
        total+=fx.f.workers(c,q).stream().filter(w->w.getMainHandItem().is(item)).mapToInt(w->w.getMainHandItem().getCount()).sum();
        total+=cache==null?0:cache.contents().stream().filter(s->s.is(item)).count();
        total+=TransferCustody.get(l).contents().stream().filter(t->t.stack().is(item)&&box.contains(t.position())).mapToInt(t->t.stack().getCount()).sum();
        boolean apple=item==Items.APPLE;
        total+=apple?q.nutrition().apples():q.nutrition().chickens();
        total+=pile==null?0:apple?pile.consumedApples():pile.consumedChickens();
        total+=fx.f.workers(c,q).stream().mapToLong(w->apple?w.nutrition().apples():w.nutrition().chickens()).sum();
        for(var row:AdultHistory.get(l).records().values()){
            var r=com.google.gson.JsonParser.parseString(row).getAsJsonObject();
            if(!r.get("queen").getAsString().equals(q.getUUID().toString()))continue;
            var nutrition=r.getAsJsonObject("nutrition");var key=apple?"apples":"chickens";total+=nutrition.has(key)?nutrition.get(key).getAsLong():0;
        }
        return total;
    }
    /** A snapshot of the already productive miner, used only by the foreign-store interruption body. */
    record DamagePause(LasiusNigerEntity miner,ItemStack cargo,int removed,int deposited,int released,Map<String,String> transfers,Set<UUID> foragers,Set<UUID> nurses){
        static DamagePause begin(GameTestHelper c,LasiusNigerEntity q,LasiusNigerEntity w){
            var l=c.getLevel();var j=Mining.get(l).job(q.getUUID());var plan=q.founding().plan();
            c.assertTrue(w!=null&&w.isAlive()&&w.workerTasks().construction()&&w.getUUID().equals(j.claim)&&j.pending(w.getMainHandItem())
                &&w.getMainHandItem().getCount()==1&&j.removed()>j.deposited&&j.deposited>0&&Mining.unlocked(l,plan)
                &&plan.nurseryFindings(l,q.getUUID(),true).verdict()==Findings.Verdict.CLEAR
                &&MaterialStore.confirmed(l,q.getUUID(),plan)!=null&&caregivers(c,q)>=2,"The existing miner genuinely removed and delivered before foreign-store damage");
            var workers=new WorkerForagingGameTest().workers(c,q);
            return new DamagePause(w,w.getMainHandItem().copy(),j.removed(),j.deposited,j.released,Map.copyOf(j.transfers()),
                Set.copyOf(q.founding().foragers()),workers.stream().filter(a->a.workerTasks().nursing()).map(LasiusNigerEntity::getUUID).collect(java.util.stream.Collectors.toSet()));
        }
        void waiting(GameTestHelper c,LasiusNigerEntity q){
            var l=c.getLevel();var plan=q.founding().plan();var j=Mining.get(l).job(q.getUUID());var store=NestPlanFixture.store(c,q);
            c.assertTrue(plan.nurseryFindings(l,q.getUUID(),true).verdict()==Findings.Verdict.DAMAGED
                &&store!=null&&NestPlan.loaded(l,store.getBlockPos())&&!store.ownedBy(q.getUUID(),plan)&&MaterialStore.confirmed(l,q.getUUID(),plan)==null,
                "Specific loaded foreign-store damage remains observed and unauthorized");
            c.assertTrue(l.getEntity(miner.getUUID())==miner&&miner.isAlive()&&!miner.isRemoved()&&NestPlan.loaded(l,miner.blockPosition())
                &&miner.getUUID().equals(j.claim)&&miner.workerTasks().construction()&&ItemStack.matches(cargo,miner.getMainHandItem()),
                "The same living worker retains the same mining claim and exact cargo during damage");
            c.assertTrue(j.removed()==removed&&j.deposited==deposited&&j.released==released&&j.transfers().equals(transfers),
                "Damage waiting has no additional removal, delivery credit or release");
            var workers=new WorkerForagingGameTest().workers(c,q);
            c.assertTrue(workers.stream().noneMatch(a->a.workerTasks().nursingAuthorized(l)),"All nursing remains unauthorized during loaded damage");
            c.assertTrue(Set.copyOf(q.founding().foragers()).equals(foragers)
                &&workers.stream().filter(a->nurses.contains(a.getUUID())).allMatch(a->a.workerTasks().nursing()&&!q.founding().claimedBy(a)
                    &&DigJob.claimedBy(l,a)==null&&ChamberUpgrade.get(l).claimedBy(a)==null),"Damage waiting diverts no nurse and introduces no replacement builder or forager");
        }
    }
    void ledgers(GameTestHelper c,LasiusNigerEntity q){ledgers(c,q,null);}
    void ledgers(GameTestHelper c,LasiusNigerEntity q,DamagePause pause){
        if(pause!=null)pause.waiting(c,q);
        fx.soil(c,q);if(!opened)return;
        c.assertTrue(fx.food.total(c,q)==apples+chickens,"Every physical food unit: "+fx.food.total(c,q)+" / "+(apples+chickens));
        c.assertTrue(foodUnits(c,q,Items.APPLE)==apples&&foodUnits(c,q,Items.CHICKEN)==chickens,"Independent apple/chicken ledgers: "+foodUnits(c,q,Items.APPLE)+"/"+apples+" "+foodUnits(c,q,Items.CHICKEN)+"/"+chickens);
        fx.food.yields(c,q);
        var mining=Mining.get(c.getLevel()).job(q.getUUID());long minedClay=mining==null?0:mining.produced("minecraft:clay_ball");
        c.assertTrue(NurseryUpgradeGameTest.clay(c,fx,q)==clay+minedClay,"Clay supplied + mined is ground, carried, stored, converted once or pending custody: "+NurseryUpgradeGameTest.clay(c,fx,q)+"/"+(clay+minedClay));
        var l=c.getLevel();var widening=NestExpansion.get(l).job(q.getUUID());
        long builders=(widening!=null&&widening.claim!=null?1:0)+ChamberExcavation.get(l).jobs(q.getUUID()).stream().filter(j->j.claim!=null).count()
            +ChamberUpgrade.get(l).jobs(q.getUUID()).stream().filter(j->j.claim!=null).count()+(mining!=null&&mining.claim!=null?1:0);
        c.assertTrue(builders<=1,"One builder across digging and upgrades: "+builders);
        if(builders>0){
            var workers=fx.f.workers(c,q);long active=workers.stream().filter(w->w.workerTasks().caregiver(l,q.founding().plan())).count();
            if(active<2&&c.getTick()%100==0)PrimeAnts.LOGGER.info("T09 CAREGIVER DIAGNOSIS queen={} tick={} builders={} active={} nursingRoles={} habitat={} mining={} workerTasks={}",q.getUUID(),c.getTick(),builders,active,
                workers.stream().filter(w->w.workerTasks().nursing()).count(),q.founding().plan().nurseryFindings(l,q.getUUID(),true).problem(),mining==null?null:mining.reason,
                workers.stream().map(w->w.getUUID()+":"+w.workerTasks().phase()+":"+w.workerTasks().reason()+":"+w.getMainHandItem()).toList());
            if(pause==null)c.assertTrue(caregivers(c,q)>=2,"At least two caregivers remain while building (valid retained nurses only during UNKNOWN habitat)");
        }
        for(var job:ChamberUpgrade.get(l).jobs(q.getUUID()))NurseryUpgradeGameTest.ledger(c,fx,q,job);
    }
    void trace(GameTestHelper c,LasiusNigerEntity q,String label){
        if(c.getTick()%1000!=0)return;var p=NestPlanFixture.pile(c,q);if(p==null)return;
        var cache=fx.f.cache(c,q);var store=NestPlanFixture.store(c,q);
        PrimeAnts.LOGGER.info("T08 {} CURVE queen={} tick={} adults={} foragers={} stocksFood={} stocksMaterial={} brood={} slots={} supply={} intake={}/{} queenNutrition={}/{} care={} cadence={} gate={} walls={} stage={} foodDropped={}/{} clayDropped={}",
            label,q.getUUID(),c.getTick(),p.stageEvaluation()==null?null:p.stageEvaluation().inputs().adults(),q.founding().foragers().size(),cache==null?null:cache.contents(),store==null?null:store.contents(),
            p.records().stream().map(r->r.slot()+":"+r.stage()+":"+r.progress()).toList(),p.nursery(),p.supply(c.getLevel()),p.growth().recentSugar(),p.growth().recentProtein(),q.nutrition().sugar(),q.nutrition().protein(),q.founding().ready(),p.layingInterval(c.getLevel()),p.growth().reason(),
            ChamberUpgrade.get(c.getLevel()).jobs(q.getUUID()).stream().map(j->j.chamber+":"+j.built().size()+"/"+j.cells.size()+":"+j.reason+":"+j.ledger()).toList(),p.stageEvaluation(),apples,chickens,clay);
    }
}
