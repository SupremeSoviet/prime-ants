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

/** Vanilla multipart models expose each canonical slot; no separate visual brood entities. */
public final class BroodPileBlock extends Block implements EntityBlock {
    public static final EnumProperty<BroodStage> A = EnumProperty.create("a", BroodStage.class);
    public static final EnumProperty<BroodStage> B = EnumProperty.create("b", BroodStage.class);
    public static final EnumProperty<BroodStage> C = EnumProperty.create("c", BroodStage.class);
    public BroodPileBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(A, BroodStage.EMPTY).setValue(B, BroodStage.EMPTY).setValue(C, BroodStage.EMPTY));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(A, B, C); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Block.box(1, 0, 1, 15, 3, 15); }
    @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new BroodPile(p, s); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState s, BlockEntityType<T> type) {
        if (level.isClientSide() || type != NurseryBlocks.BROOD_TYPE) return null;
        return (l, p, state, be) -> ((BroodPile)be).serverTick((net.minecraft.server.level.ServerLevel)l);
    }
}
