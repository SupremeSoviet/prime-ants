package dev.primeants.founding;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** One exact vanilla randomTick write, consumed at the first chunk mutation BEFORE nested callbacks. */
public final class NativeGrassWrite {
    private record Write(ServerLevel level,BlockPos pos,BlockState old,BlockState next) {}
    private static final ThreadLocal<Write> PENDING=new ThreadLocal<>();
    private NativeGrassWrite() {}
    public static boolean write(ServerLevel l,BlockPos p,BlockState next,boolean actualGrass) {
        var old=l.getBlockState(p);
        if(!actualGrass || !NaturalSoil.grassPair(old,next))return l.setBlockAndUpdate(p,next);
        var previous=PENDING.get();PENDING.set(new Write(l,p.immutable(),old,next));
        try{return l.setBlockAndUpdate(p,next);}finally{if(previous==null)PENDING.remove();else PENDING.set(previous);}
    }
    public static boolean consume(ServerLevel l,BlockPos p,BlockState next) {
        var w=PENDING.get();PENDING.remove(); // Even a mismatch consumes; never authorize an arbitrary nested write.
        return w!=null && w.level==l && w.pos.equals(p) && w.next.equals(next)
            && NaturalSoil.get(l).nativeGrassWrite(l,p,w.old,next);
    }
}
