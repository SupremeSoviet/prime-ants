package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import dev.primeants.brood.*;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;

/** Real loaded ticks: food starts as dropped stacks, never injected recipient nutrition or supplied adults. */
public final class NursingGameTest {
    private final WorkerForagingGameTest fixture=new WorkerForagingGameTest();
    BroodPile pile(GameTestHelper c,LasiusNigerEntity q){return q.founding().plan()!=null&&c.getLevel().getBlockEntity(q.founding().plan().nursery()) instanceof BroodPile p?p:null;}
    long consumed(GameTestHelper c,LasiusNigerEntity q){var p=pile(c,q);return q.nutrition().consumedUnits()+(p==null?0:p.consumedFood())+AdultReceipts.consumed(c.getLevel(),q);}
    long total(GameTestHelper c,LasiusNigerEntity q){
        var n=fixture.cache(c,q);var plan=q.founding().plan();
        long world=c.getLevel().getEntitiesOfClass(ItemEntity.class,c.getBounds().inflate(8),i->i.isAlive()&&WorkerTasks.food(i.getItem())).stream().mapToInt(i->i.getItem().getCount()).sum();
        long carried=fixture.workers(c,q).stream().filter(w->WorkerTasks.food(w.getMainHandItem())).mapToInt(w->w.getMainHandItem().getCount()).sum();
        long custody=TransferCustody.get(c.getLevel()).contents().stream().filter(p->WorkerTasks.food(p.stack())&&c.getBounds().inflate(8).contains(p.position())).mapToInt(p->p.stack().getCount()).sum();
        return world+carried+custody+(n==null?0:n.size())+consumed(c,q);
    }
    void supply(GameTestHelper c,LasiusNigerEntity q,int sugar,int protein){var p=q.founding().plan();if(sugar>0)fixture.drop(c,p.at(-3,0,1),new ItemStack(Items.APPLE,sugar));if(protein>0)fixture.drop(c,p.at(-4,0,1),new ItemStack(Items.CHICKEN,protein));}
    void yields(GameTestHelper c,LasiusNigerEntity q){
        var p=pile(c,q);var n=q.nutrition();
        c.assertTrue(Nutrition.APPLE_SUGAR==4000&&Nutrition.BERRY_SUGAR==2000&&Nutrition.CHICKEN_PROTEIN==8000,"Declared yields are explicit");
        c.assertTrue(n.gainedSugar()==n.apples()*4000+n.berries()*2000&&n.gainedProtein()==n.chickens()*8000&&n.gainedSugar()==n.sugar()+n.spentSugar()&&n.gainedProtein()==n.protein()+n.spentProtein(),"Queen credit independently equals actual terminal units times declared yields");
        if(p!=null)for(var r:p.records()){var v=r.nutrition();c.assertTrue(v.gainedSugar()==v.apples()*4000+v.berries()*2000&&v.gainedProtein()==v.chickens()*8000&&v.gainedSugar()==v.sugar()+v.spentSugar()&&v.gainedProtein()==v.protein()+v.spentProtein(),"Each larva owns finite persisted credited nutrition");}
    }
    @GameTest(maxTicks=24000,structure="prime_ants_test:idle_ground")
    public void physicalNursesRaiseNewIdentitiesAndContinueLaying(GameTestHelper c){
        var q=fixture.start(c);boolean[] supplied={false},carried={false},fed={false};Set<UUID> original=new HashSet<>(),newIds=new HashSet<>(),firstWorkers=new HashSet<>();
        c.onEachTick(()->{
            c.assertTrue(q.founding().phase()!=QueenFounding.Phase.FAILED,"Production founding remains valid");var p=pile(c,q);if(p==null)return;
            original.addAll(p.original());var ws=fixture.workers(c,q);
            if(!supplied[0]&&ws.size()==3&&ws.stream().allMatch(w->!w.isCallow())&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;ws.forEach(w->firstWorkers.add(w.getUUID()));supply(c,q,6,8);}
            if(!supplied[0])return;
            c.assertTrue(total(c,q)==14,"Every supplied food unit is world/cache/cargo/custody or genuinely consumed: "+total(c,q));yields(c,q);
            for(var w:ws){if(w.workerTasks().nursing()&&WorkerTasks.food(w.getMainHandItem())){carried[0]=true;c.assertTrue(!q.founding().claimedBy(w)&&ColonyMembers.get(c.getLevel()).belongs(w,q.getUUID(),q.founding().plan().chamber()),"Nurse membership does not steal forager claim");}if(w.workerTasks().feedingTicks()>0)fed[0]=true;}
            p.records().stream().filter(r->!r.founding()).forEach(r->newIds.add(r.id()));p.consumed().stream().filter(id->!original.contains(id)).forEach(newIds::add);
            if(c.getTick()%500==0)PrimeAnts.LOGGER.info("T10 growth tick={} stock={} consumed={} queenSugar={} queenProtein={} records={} workers={}",c.getTick(),total(c,q),consumed(c,q),q.nutrition().sugar(),q.nutrition().protein(),p.records().stream().map(r->r.id()+" "+r.stage()+" "+r.progress()+" "+r.nutrition().sugar()+"/"+r.nutrition().protein()).toList(),ws.stream().map(w->w.getUUID()+" "+w.position()+" "+w.workerTasks().phase()+" "+w.workerTasks().reason()+" "+w.getMainHandItem()).toList());
            var additional=ws.stream().filter(w->!firstWorkers.contains(w.getUUID())&&w.isCallow()).findFirst();
            if(additional.isPresent()&&newIds.size()>3){
                c.assertTrue(original.size()==3&&p.consumed().containsAll(original)&&Collections.disjoint(original,newIds)&&ws.size()>3&&firstWorkers.stream().allMatch(id->c.getLevel().getEntity(id) instanceof LasiusNigerEntity),"Original clutch remains, different new IDs reuse slots, actual workers increase beyond three");
                c.assertTrue(carried[0]&&fed[0]&&consumed(c,q)>0&&q.bodyReserve()==0&&additional.get().broodId()!=null&&newIds.contains(additional.get().broodId()),"Physical nurses feed; extra pale worker derives from new food-fed brood");
                PrimeAnts.LOGGER.info("T10 GROWTH SUCCESS supplied=6apple/8chicken consumed={} queenReceipts={}/{}/{} larvaReceipts={}/{}/{} oldBrood={} newBrood={} originalWorkers={} liveWorkers={} additional={}",consumed(c,q),q.nutrition().apples(),q.nutrition().berries(),q.nutrition().chickens(),p.consumedApples(),p.consumedBerries(),p.consumedChickens(),original,newIds,firstWorkers,ws.stream().map(LasiusNigerEntity::getUUID).toList(),additional.get().getUUID());c.succeed();
            }
        });
    }
}
