package dev.primeants.gametest;

import dev.primeants.colony.*;
import dev.primeants.gametest.mixin.GameTestHelperAccessor;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;

/** Read-only observation of every production evaluation, including multiple evaluations within a nursery tick. */
public final class StageEvaluations implements AutoCloseable {
    private static final Set<StageEvaluations> scopes=new HashSet<>();
    private final GameTestHelper test; private final UUID queen;
    public long promotion=-1, promotionLoaded=-1; public int held, unknownHall;
    public ColonyDevelopment.Evaluation loss, falseHallClock;
    private StageEvaluations(GameTestHelper c,UUID owner){test=c;queen=owner;scopes.add(this);}
    public static StageEvaluations watch(GameTestHelper c,UUID owner){
        var scope=new StageEvaluations(c,owner);
        ((GameTestHelperAccessor)c).primeAntsTestInfo().addListener(new GameTestListener(){
            public void testStructureLoaded(GameTestInfo i){}
            public void testPassed(GameTestInfo i,GameTestRunner r){scope.close();}
            public void testFailed(GameTestInfo i,GameTestRunner r){scope.close();}
            public void testAddedForRerun(GameTestInfo i,GameTestInfo copy,GameTestRunner r){scope.close();}
        }); return scope;
    }
    public static void observed(ServerLevel level,UUID owner,long loaded,ColonyDevelopment.Evaluation evaluation){
        for(var scope:scopes)if(scope.test.getLevel()==level&&scope.queen.equals(owner))scope.record(loaded,evaluation);
    }
    private void record(long loaded,ColonyDevelopment.Evaluation e){
        if(promotion<0&&e.stage()==ColonyStage.MATURE){promotion=test.getTick();promotionLoaded=loaded;}
        if(promotion<0)return;
        held++;
        if(e.stage()!=ColonyStage.MATURE&&loss==null)loss=e;
        if(e.chambers().stream().anyMatch(s->s.functions().get(ChamberFunction.QUEENS_HALL)==ColonyDevelopment.Presence.UNKNOWN)){
            unknownHall++;
            if(e.result().unmetSince().keySet().stream().anyMatch(k->k.endsWith(":queens_hall")||k.endsWith(":queens_hall_tier"))&&falseHallClock==null)falseHallClock=e;
        }
    }
    @Override public void close(){scopes.remove(this);}
}
