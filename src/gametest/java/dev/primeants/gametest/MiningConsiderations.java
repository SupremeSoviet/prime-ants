package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.gametest.mixin.GameTestHelperAccessor;
import dev.primeants.worker.MaterialStore;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;

/** Scoped, read-only observations of the actual production admission guard's returned value. */
public final class MiningConsiderations implements AutoCloseable {
    private static final Set<MiningConsiderations> scopes=new HashSet<>();
    private final GameTestHelper test;
    private final UUID queen;
    public record Event(long tick,long loaded,String problem,int removed,int deposited,UUID claim,List<UUID> eligible,long caregivers) {}
    public Event last;
    public int refused;
    private String previous="";
    private MiningConsiderations(GameTestHelper c,UUID owner){test=c;queen=owner;scopes.add(this);}
    public static MiningConsiderations watch(GameTestHelper c,UUID owner){
        var scope=new MiningConsiderations(c,owner);
        ((GameTestHelperAccessor)c).primeAntsTestInfo().addListener(new GameTestListener(){
            public void testStructureLoaded(GameTestInfo i){}
            public void testPassed(GameTestInfo i,GameTestRunner r){scope.close();}
            public void testFailed(GameTestInfo i,GameTestRunner r){scope.close();}
            public void testAddedForRerun(GameTestInfo i,GameTestInfo copy,GameTestRunner r){scope.close();}
        });return scope;
    }
    public static void observed(ServerLevel l,LasiusNigerEntity q,List<LasiusNigerEntity> workers,Mining.Job j,String problem){
        for(var scope:scopes)if(scope.test.getLevel()==l&&scope.queen.equals(q.getUUID())){
            var plan=q.founding().plan();var pile=NestPlanFixture.pile(scope.test,q);
            var eligible=workers.stream().filter(w->w.workerTasks().canConstruct(plan)&&NestExpansion.remainingCaregivers(l,q.getUUID(),plan,w)>=2).map(LasiusNigerEntity::getUUID).toList();
            long carers=workers.stream().filter(w->w.workerTasks().caregiver(l,plan)).count();
            scope.last=new Event(scope.test.getTick(),pile==null?-1:pile.loadedTicks(),problem,j.removed(),j.deposited,j.claim,List.copyOf(eligible),carers);
            if(problem!=null)scope.refused++;
            String state=problem+":"+j.removed()+":"+j.deposited+":"+j.claim+":"+eligible.isEmpty();
            if(!state.equals(scope.previous)||scope.test.getTick()%1000<100){
                scope.previous=state;PrimeAnts.LOGGER.info("T11 PRODUCTION MINING CONSIDERATION queen={} event={} ready={} unlocked={} storeConfirmed={} jobReason={} refused={}",q.getUUID(),scope.last,q.founding().ready(),Mining.unlocked(l,plan),MaterialStore.confirmed(l,q.getUUID(),plan)!=null,j.reason,scope.refused);
            }
        }
    }
    @Override public void close(){scopes.remove(this);}
}
