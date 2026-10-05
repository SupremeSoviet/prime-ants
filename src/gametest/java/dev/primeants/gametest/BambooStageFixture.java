package dev.primeants.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Hold one declared support-guard fixture at sapling stage; survival and queen work stay real. */
public final class BambooStageFixture {
    private static ServerLevel level;
    private static BlockPos plant;
    public static void watch(ServerLevel l,BlockPos p){level=l;plant=p.immutable();}
    public static boolean holds(ServerLevel l,BlockPos p){return l==level&&p.equals(plant);}
    public static void clear(){level=null;plant=null;}
}
