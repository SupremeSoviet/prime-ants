package dev.primeants.gametest;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.gametest.framework.*;
import dev.primeants.gametest.mixin.GameTestHelperAccessor;
/** Each test owns a removable token; overlapping registrations cannot clear one another. */
public final class BambooStageFixture implements AutoCloseable {
    private static final Map<UUID,BambooStageFixture> registrations=new HashMap<>();
    private final UUID token=UUID.randomUUID();
    private final ServerLevel level;private final BlockPos plant;
    private BambooStageFixture(ServerLevel l,BlockPos p){level=l;plant=p.immutable();registrations.put(token,this);}
    public static BambooStageFixture watch(GameTestHelper c,ServerLevel l,BlockPos p){
        var scope=new BambooStageFixture(l,p);
        ((GameTestHelperAccessor)c).primeAntsTestInfo().addListener(new GameTestListener(){
            public void testStructureLoaded(GameTestInfo i){}
            public void testPassed(GameTestInfo i,GameTestRunner r){scope.close();}
            public void testFailed(GameTestInfo i,GameTestRunner r){scope.close();}
            public void testAddedForRerun(GameTestInfo i,GameTestInfo copy,GameTestRunner r){scope.close();}
        });return scope;
    }
    public static boolean holds(ServerLevel l,BlockPos p){return registrations.values().stream().anyMatch(s->s.level==l&&s.plant.equals(p));}
    @Override public void close(){registrations.remove(token);}
}
