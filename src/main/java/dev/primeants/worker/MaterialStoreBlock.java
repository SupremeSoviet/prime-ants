package dev.primeants.worker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
/** A low tamped pad on a store chamber's floor. Ants walk over it; the block entity owns ownership and contents. */
public final class MaterialStoreBlock extends Block implements EntityBlock {
    public MaterialStoreBlock(Properties p) { super(p); }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) { return Block.box(1,0,1,15,4,15); }
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s) { return new MaterialStore(p,s); }
}
