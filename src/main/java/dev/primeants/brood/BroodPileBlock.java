package dev.primeants.brood;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Vanilla multipart models expose the first three canonical slots; brood in further slots (a hall or a tier-2+
 * nursery, BroodCapacity) shows in aggregate as a heap of 0..4 levels, one per three brood. No separate visual brood
 * entities. 4 x 4 x 4 x 5 = 320 states. */
public final class BroodPileBlock extends Block implements EntityBlock {
    public static final EnumProperty<BroodStage> A = EnumProperty.create("a", BroodStage.class);
    public static final EnumProperty<BroodStage> B = EnumProperty.create("b", BroodStage.class);
    public static final EnumProperty<BroodStage> C = EnumProperty.create("c", BroodStage.class);
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty MORE = net.minecraft.world.level.block.state.properties.IntegerProperty.create("more", 0, 4);
    /** The aggregate heap level of brood beyond slot C: one level per three brood. */
    public static int more(int brood) { return Math.min(4, (brood + 2) / 3); }
    public BroodPileBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(A, BroodStage.EMPTY).setValue(B, BroodStage.EMPTY).setValue(C, BroodStage.EMPTY).setValue(MORE, 0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(A, B, C, MORE); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Block.box(1, 0, 1, 15, 3, 15); }
    @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new BroodPile(p, s); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState s, BlockEntityType<T> type) {
        if (level.isClientSide() || type != NurseryBlocks.BROOD_TYPE) return null;
        return (l, p, state, be) -> ((BroodPile)be).serverTick((net.minecraft.server.level.ServerLevel)l);
    }
}
