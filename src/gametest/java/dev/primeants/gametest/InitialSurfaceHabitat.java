package dev.primeants.gametest;

import dev.primeants.gametest.mixin.GameTestHelperAccessor;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;

/** Controlled starting-habitat ownership, never an excavation/removal/delivery receipt or surface placement. */
public final class InitialSurfaceHabitat implements AutoCloseable {
    private static final Set<InitialSurfaceHabitat> scopes=new HashSet<>();
    private final GameTestHelper test;private final UUID owner;private final Set<BlockPos> cells;
    private InitialSurfaceHabitat(GameTestHelper c,UUID owner,Set<BlockPos> cells){test=c;this.owner=owner;this.cells=new HashSet<>(cells);scopes.add(this);}
    public static InitialSurfaceHabitat declare(GameTestHelper c,UUID owner,Set<BlockPos> cells){
        var scope=new InitialSurfaceHabitat(c,owner,cells);
        ((GameTestHelperAccessor)c).primeAntsTestInfo().addListener(new GameTestListener(){
            public void testStructureLoaded(GameTestInfo i){}
            public void testPassed(GameTestInfo i,GameTestRunner r){scope.close();}
            public void testFailed(GameTestInfo i,GameTestRunner r){scope.close();}
            public void testAddedForRerun(GameTestInfo i,GameTestInfo copy,GameTestRunner r){scope.close();}
        });return scope;
    }
    public static boolean opening(ServerLevel l,BlockPos p,UUID owner){
        for(var s:scopes)if(s.test.getLevel()==l&&s.owner.equals(owner)&&s.cells.contains(p)
            &&dev.primeants.founding.NestPlan.loaded(l,p)&&l.getBlockState(p).isAir()&&l.getFluidState(p).isEmpty())return true;
        return false;
    }
    public static void invalidate(BlockPos p){for(var scope:scopes)scope.cells.remove(p);}
    @Override public void close(){scopes.remove(this);}
}
