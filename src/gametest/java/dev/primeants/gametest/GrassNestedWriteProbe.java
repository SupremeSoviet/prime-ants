package dev.primeants.gametest;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
/** A single development-only ordinary write nested inside the real grass chunk-write return. */
public final class GrassNestedWriteProbe {
    private static ServerLevel level;private static BlockPos natural,ordinary;
    public static void watch(ServerLevel l,BlockPos n,BlockPos o){level=l;natural=n;ordinary=o;}
    public static void after(ServerLevel l,BlockPos p){
        if(l!=level||natural==null||!natural.equals(p))return;
        var target=ordinary;natural=null;ordinary=null;level=null;l.setBlock(target,l.getBlockState(target),3);
    }
}
