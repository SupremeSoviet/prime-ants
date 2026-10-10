package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.NestPlan;
import dev.primeants.worker.MaterialStore;
import dev.primeants.gametest.mixin.GameTestHelperAccessor;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;

/** Test-only custody observation. Every canonical hand write invalidates the prior identity. */
public final class NamedMaterialRecovery implements AutoCloseable {
    public record Pickup(UUID source,UUID worker,ItemStack cargo,long tick) {}
    public record Deposit(Pickup pickup,BlockPos store,long tick) {}
    private static final Set<NamedMaterialRecovery> scopes=new HashSet<>();
    private final GameTestHelper test; private final UUID owner;
    private final Map<UUID,Pickup> active=new HashMap<>();
    public final List<Pickup> pickups=new ArrayList<>();
    public final List<Deposit> deposits=new ArrayList<>();
    private NamedMaterialRecovery(GameTestHelper c,UUID queen){test=c;owner=queen;scopes.add(this);}
    public static NamedMaterialRecovery watch(GameTestHelper c,UUID queen){
        var scope=new NamedMaterialRecovery(c,queen);
        ((GameTestHelperAccessor)c).primeAntsTestInfo().addListener(new GameTestListener(){
            public void testStructureLoaded(GameTestInfo i){}
            public void testPassed(GameTestInfo i,GameTestRunner r){scope.close();}
            public void testFailed(GameTestInfo i,GameTestRunner r){scope.close();}
            public void testAddedForRerun(GameTestInfo i,GameTestInfo copy,GameTestRunner r){scope.close();}
        });return scope;
    }
    public static void handWrite(LasiusNigerEntity worker){for(var scope:scopes)scope.active.remove(worker.getUUID());}
    public static void pickedUp(LasiusNigerEntity worker,UUID source,ItemStack cargo){
        for(var scope:scopes)if(worker.level()==scope.test.getLevel()&&scope.owner.equals(worker.queenId())){
            var event=new Pickup(source,worker.getUUID(),cargo.copy(),scope.test.getTick());
            scope.active.put(worker.getUUID(),event);scope.pickups.add(event);
            PrimeAnts.LOGGER.info("T12 DIRECT PICKUP queen={} tick={} source={} worker={} cargo={}",scope.owner,event.tick(),source,worker.getUUID(),cargo);
        }
    }
    /** Snapshot before forwarding the actual deposit; no identity survives an intervening hand write. */
    public static Map<NamedMaterialRecovery,Pickup> beforeDeposit(LasiusNigerEntity worker,MaterialStore store,NestPlan plan){
        var result=new HashMap<NamedMaterialRecovery,Pickup>();
        for(var scope:scopes){var p=scope.active.get(worker.getUUID());
            if(p!=null&&worker.level()==scope.test.getLevel()&&scope.owner.equals(worker.queenId())
                &&store.ownedBy(scope.owner,plan)&&ItemStack.matches(p.cargo(),worker.getMainHandItem()))result.put(scope,p);
        }return result;
    }
    public static void deposited(Map<NamedMaterialRecovery,Pickup> before,MaterialStore store,boolean result){
        if(!result)return;
        before.forEach((scope,p)->{
            scope.active.remove(p.worker());var event=new Deposit(p,store.getBlockPos(),scope.test.getTick());scope.deposits.add(event);
            PrimeAnts.LOGGER.info("T12 DIRECT DEPOSIT queen={} tick={} source={} worker={} cargo={} store={}",scope.owner,event.tick(),p.source(),p.worker(),p.cargo(),event.store());
        });
    }
    public List<Pickup> pickups(UUID source){return pickups.stream().filter(p->source.equals(p.source())).toList();}
    public List<Deposit> deposits(UUID source){return deposits.stream().filter(d->source.equals(d.pickup().source())).toList();}
    @Override public void close(){scopes.remove(this);active.clear();}
}
