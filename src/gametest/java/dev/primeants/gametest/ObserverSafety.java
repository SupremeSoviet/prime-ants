package dev.primeants.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/** Observer-only guard. Never generates terrain or substitutes a minimum-Y height. */
final class ObserverSafety {
    static AABB body(BlockPos feet) {
        return new AABB(feet.getX()+.2,feet.getY(),feet.getZ()+.2,
                feet.getX()+.8,feet.getY()+1.8,feet.getZ()+.8);
    }
    static boolean full(ServerLevel level, int x, int z) {
        return level.getChunkSource().getChunkNow(x>>4,z>>4)!=null;
    }
    static BlockPos surface(ServerLevel level,int x,int z) {
        // getHeight() returns minimum Y for unavailable terrain; do not call it first.
        if(!full(level,x,z))return null;
        int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
        if(y<=level.getMinY()||!level.isInsideBuildHeight(y+1))return null;
        return new BlockPos(x,y,z);
    }
    static String problem(ServerLevel level,Entity observer,BlockPos feet) {
        if(observer!=null&&!observer.isAlive())return "dead_observer";
        if(feet==null)return "height_unavailable";
        var box=body(feet);
        if(!level.isInWorldBounds(feet.below())||!level.isInWorldBounds(feet.above())
                ||!level.getWorldBorder().isWithinBounds(box))return "outside_build_height_or_border";
        // Check the whole body/support and collision-query neighbour halo without loading.
        for(int x=feet.getX()-1;x<=feet.getX()+1;x++)for(int z=feet.getZ()-1;z<=feet.getZ()+1;z++)
            if(!full(level,x,z))return "terrain_not_FULL";
        if(!level.getFluidState(feet.below()).isEmpty()||!level.getFluidState(feet).isEmpty()
                ||!level.getFluidState(feet.above()).isEmpty())return "wet_destination";
        if(!level.getBlockState(feet.below()).isFaceSturdy(level,feet.below(),Direction.UP)
                ||!level.getBlockState(feet.below()).isSolidRender())return "no_solid_support";
        if(!level.noCollision(observer,box))return "body_collision";
        return null;
    }
}
