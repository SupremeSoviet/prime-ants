package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import dev.primeants.entity.*;
import dev.primeants.worker.*;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public final class TransferGameTest {
    static ItemStack stock(String name,int n) { var s=new ItemStack(Items.APPLE,n);s.set(DataComponents.CUSTOM_NAME,Component.literal(name));return s; }
    static int world(GameTestHelper c,String name) {return c.getLevel().getEntitiesOfClass(ItemEntity.class,c.getBounds().inflate(8),i->i.isAlive()&&i.getItem().get(DataComponents.CUSTOM_NAME)!=null&&name.equals(i.getItem().get(DataComponents.CUSTOM_NAME).getString())).stream().mapToInt(i->i.getItem().getCount()).sum();}
    static java.util.List<TransferCustody.Pending> pending(GameTestHelper c,String name) {return TransferCustody.get(c.getLevel()).contents().stream().filter(p->p.stack().get(DataComponents.CUSTOM_NAME)!=null&&name.equals(p.stack().get(DataComponents.CUSTOM_NAME).getString())).toList();}
    static int custody(GameTestHelper c,String name) {return pending(c,name).stream().mapToInt(p->p.stack().getCount()).sum();}
    void restoreCustody(GameTestHelper c,String name) {
        var before=pending(c,name);c.getLevel().getDataStorage().saveAndJoin();
        try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(
                net.minecraft.world.level.dimension.DimensionType.getStorageFolder(c.getLevel().dimension(),c.getLevel().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),
                net.minecraft.util.datafix.DataFixers.getDataFixer(),c.getLevel().registryAccess())) {
            var saved=disk.get(TransferCustody.TYPE);c.assertTrue(saved!=null,"Pending transfer read from normal SavedData file");
            for(var p:before)c.assertTrue(saved.contents().stream().anyMatch(r->r.id().equals(p.id())&&r.source().equals(p.source())&&r.position().equals(p.position())&&ItemStack.matches(r.stack(),p.stack())),"Disk restores actual stack components, quantity, position and stable identity");
            c.getLevel().getDataStorage().set(TransferCustody.TYPE,saved);
        }
    }
    private void recover(GameTestHelper c,String name,int expected) {
        var original=pending(c,name).getFirst().stack().copy();restoreCustody(c,name);TransferFault.release(name);
        c.runAfterDelay(80,()->{
            c.assertTrue(custody(c,name)==0&&world(c,name)==expected,"Real ticks recover once into verified world items; physical total="+world(c,name));
            c.assertTrue(c.getLevel().getEntitiesOfClass(ItemEntity.class,c.getBounds().inflate(8),i->i.isAlive()&&i.getItem().get(DataComponents.CUSTOM_NAME)!=null&&name.equals(i.getItem().get(DataComponents.CUSTOM_NAME).getString())).stream().allMatch(i->ItemStack.isSameItemSameComponents(original,i.getItem())),"Recovered world ownership retains exact original stack components");c.succeed();
        });
    }
    @GameTest(maxTicks=200)
    public void dyingWorkerRefusalSurvivesNormalCleanup(GameTestHelper c) {
        String name="T09-worker-"+UUID.randomUUID();var s=stock(name,3);
        var w=c.spawn(AntEntities.WORKER,2,2,2);w.setItemSlot(EquipmentSlot.MAINHAND,s);TransferFault.block(name,0);
        w.hurtServer(c.getLevel(),w.damageSources().generic(),1000);w.die(w.damageSources().generic());
        c.runAfterDelay(40,()->{
            int physical=world(c,name)+custody(c,name)+(w.isRemoved()?0:w.getMainHandItem().getCount());
            PrimeAnts.LOGGER.info("T09 worker refusal actualWorld={} sourceRemoved={} cargo={} refused={} physical={}",world(c,name),w.isRemoved(),w.getMainHandItem(),TransferFault.refused(name),physical);
            c.assertTrue(w.isRemoved()&&TransferFault.refused(name)>0,"Real refusal and ordinary death cleanup");
            c.assertTrue(physical==3&&custody(c,name)==3&&w.getMainHandItem().isEmpty(),"Refused worker cargo survives cleanup: actual physical="+physical+" expected=3");
            var ids=pending(c,name).stream().map(TransferCustody.Pending::id).toList();w.die(w.damageSources().generic());
            c.assertTrue(ids.equals(pending(c,name).stream().map(TransferCustody.Pending::id).toList()),"Repeated death retains exact pending identity and quantity");recover(c,name,3);
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void cacheReplacementPartialRefusalConservesPhysicalStacks(GameTestHelper c) { cache(c,1); }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void cacheRemovalCompleteRefusalConservesPhysicalStacks(GameTestHelper c) { cache(c,0); }
    private void cache(GameTestHelper c,int successes) {
        var f=new WorkerForagingGameTest();var q=f.start(c);String name="T09-cache-"+UUID.randomUUID();boolean[] dropped={false},removed={false};
        c.onEachTick(()->{
            if(removed[0])return;f.absentCaregivers(c,q);var p=q.founding().plan();if(p==null)return;
            if(!dropped[0]&&q.founding().sealed()){dropped[0]=true;f.drop(c,p.at(-3,0,1),stock(name,2));}
            var n=f.cache(c,q);if(n==null||n.size()!=2)return;removed[0]=true;
            TransferFault.block(name,successes);var old=n.getBlockState();c.getLevel().setBlock(p.cache(),Blocks.STONE.defaultBlockState(),3);
            n.preRemoveSideEffects(p.cache(),old); // repeat the callback after actual vanilla deletion
            c.runAfterDelay(40,()->{
                int physical=world(c,name)+custody(c,name);
                PrimeAnts.LOGGER.info("T09 cache refusal successful={} world={} sourceRemoved={} actualBlock={} contents={} refused={}",successes,physical,n.isRemoved(),c.getLevel().getBlockState(p.cache()),n.contents(),TransferFault.refused(name));
                c.assertTrue(n.isRemoved()&&c.getLevel().getBlockEntity(p.cache())==null&&TransferFault.refused(name)>0,"Actual replacement removed source and really refused insertion");
                c.assertTrue(physical==2&&world(c,name)==successes&&custody(c,name)==2-successes&&n.contents().isEmpty(),"Removed cache stock survives refusal: actual physical="+physical+" expected=2");
                var ids=pending(c,name).stream().map(TransferCustody.Pending::id).toList();n.preRemoveSideEffects(p.cache(),old);
                c.assertTrue(ids.equals(pending(c,name).stream().map(TransferCustody.Pending::id).toList())&&world(c,name)==successes,"Repeated removal cannot duplicate successful partial drops");recover(c,name,2);
            });
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void foundingQueenSoilRefusalPreservesComponentsAfterDeath(GameTestHelper c) {
        var f=new WorkerForagingGameTest();var q=f.start(c);String name="T09-soil-"+UUID.randomUUID();boolean[] killed={false};
        c.onEachTick(()->{
            if(killed[0]||q.founding().carried()==0)return;killed[0]=true;
            var actual=q.getMainHandItem().copy();int count=actual.getCount();actual.set(DataComponents.CUSTOM_NAME,Component.literal(name));q.setItemSlot(EquipmentSlot.MAINHAND,actual);TransferFault.block(name,0);
            q.hurtServer(c.getLevel(),q.damageSources().generic(),1000);q.die(q.damageSources().generic());
            c.runAfterDelay(40,()->{
                c.assertTrue(q.isRemoved()&&q.getMainHandItem().isEmpty()&&world(c,name)==0&&custody(c,name)==count&&TransferFault.refused(name)>0,"Queen source removed; original carried soil remains in persistent custody");
                c.assertTrue(pending(c,name).stream().allMatch(p->ItemStack.matches(actual,p.stack())),"Founding death preserves the actual original soil components");recover(c,name,count);
            });
        });
    }
}
