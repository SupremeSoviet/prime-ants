package dev.primeants.gametest;

import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.brood.BroodPile;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;

/** Negative scheduling setup: delay admission until actual collection has funded
 * a clutch. Never supplies food, records, stages or identities. Normal production
 * laying/emergence proceeds after release; the unheld nursing control is separate. */
public final class FundedPopulationFixture implements AutoCloseable {
    private record Key(ServerLevel level,UUID queen){}
    private static final Set<Key> holds=new HashSet<>();
    private final Key key;
    private boolean released;
    private FundedPopulationFixture(Key k){key=k;holds.add(k);}
    public static FundedPopulationFixture hold(GameTestHelper c,LasiusNigerEntity q){
        var scope=new FundedPopulationFixture(new Key(c.getLevel(),q.getUUID()));
        ((dev.primeants.gametest.mixin.GameTestHelperAccessor)c).primeAntsTestInfo().addListener(new GameTestListener(){
            public void testStructureLoaded(GameTestInfo i){}
            public void testPassed(GameTestInfo i,GameTestRunner r){scope.close();}
            public void testFailed(GameTestInfo i,GameTestRunner r){scope.close();}
            public void testAddedForRerun(GameTestInfo i,GameTestInfo copy,GameTestRunner r){scope.close();}
        });return scope;
    }
    public static boolean held(BroodPile p){return p.getLevel() instanceof ServerLevel l&&holds.contains(new Key(l,p.queenId()));}
    public boolean releaseIfStocked(BroodPile p){
        if(released)return true;if(p==null)return false;var s=p.supply(key.level());
        if(!s.complete()||s.hungry()||s.storedSugar()<21000||s.storedProtein()<30000)return false;
        released=true;close();dev.primeants.PrimeAnts.LOGGER.info("T22 negative fixture releases admission queen={} actualSupply={}",key.queen(),s);return true;
    }
    @Override public void close(){holds.remove(key);}
}
