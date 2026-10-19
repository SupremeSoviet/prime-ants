package dev.primeants.founding;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A small integrity exception, never a clearing permission or a collision exception. */
public final class NestLighting {
    private NestLighting() { }
    /** State transition only; the caller must independently hold current opening authority. */
    public static boolean transition(ServerLevel level,BlockPos pos,BlockState before,BlockState after){
        boolean wasLight=supported(level,pos,before),isLight=supported(level,pos,after);
        return (wasLight||isLight)&&(before.equals(Blocks.AIR.defaultBlockState())||wasLight)
            &&(after.equals(Blocks.AIR.defaultBlockState())||isLight);
    }
    public static boolean supported(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.getFluidState().isEmpty() || !NestPlan.loaded(level,pos)) return false;
        var support=support(pos,state);
        return support!=null && NestPlan.loaded(level,support) && level.getFluidState(support).isEmpty() && state.canSurvive(level,pos);
    }
    /** Unknown support is not evidence of a broken light. Callers report it as unavailable, separately from damage. */
    public static boolean supportLoaded(ServerLevel level,BlockPos pos,BlockState state){
        var support=support(pos,state);return support==null || !state.getFluidState().isEmpty() || NestPlan.loaded(level,support);
    }
    private static BlockPos support(BlockPos pos,BlockState state){
        if (state.is(Blocks.TORCH) || state.is(Blocks.SOUL_TORCH)) return pos.below();
        else if (state.is(Blocks.WALL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH))
            return pos.relative(state.getValue(WallTorchBlock.FACING).getOpposite());
        else if (state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN))
            return pos.relative(state.getValue(LanternBlock.HANGING)?Direction.UP:Direction.DOWN);
        else return null;
    }
}
