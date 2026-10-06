package dev.primeants.gametest;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.gametest.framework.*;
import dev.primeants.gametest.mixin.GameTestHelperAccessor;
/** Cells a test reports as unavailable chunks through NestPlan.loaded (UnavailableCellMixin). A test nest's chunks stay
 * loaded together, so one unavailable cell beside a loaded chamber is injected rather than produced by unloading; block
 * reads are untouched. Each test owns a removable token, released when it passes or fails. */
public final class UnavailableCells implements AutoCloseable {
    private static final Map<UUID,UnavailableCells> registrations=new HashMap<>();
    private final UUID token=UUID.randomUUID();
    private final ServerLevel level;private final BlockPos cell;
    private UnavailableCells(ServerLevel l,BlockPos p){level=l;cell=p.immutable();registrations.put(token,this);}
    public static UnavailableCells hide(GameTestHelper c,BlockPos p){
        var scope=new UnavailableCells(c.getLevel(),p);
        ((GameTestHelperAccessor)c).primeAntsTestInfo().addListener(new GameTestListener(){
            public void testStructureLoaded(GameTestInfo i){}
            public void testPassed(GameTestInfo i,GameTestRunner r){scope.close();}
            public void testFailed(GameTestInfo i,GameTestRunner r){scope.close();}
            public void testAddedForRerun(GameTestInfo i,GameTestInfo copy,GameTestRunner r){scope.close();}
        });return scope;
    }
    public static boolean hidden(ServerLevel l,BlockPos p){
        if(registrations.isEmpty())return false;
        for(var s:registrations.values())if(s.level==l&&s.cell.equals(p))return true;
        return false;
    }
    @Override public void close(){registrations.remove(token);}
}
