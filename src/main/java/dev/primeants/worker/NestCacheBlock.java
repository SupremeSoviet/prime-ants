package dev.primeants.worker;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
public final class NestCacheBlock extends Block implements EntityBlock {
    public static final List<IntegerProperty> SLOTS=java.util.stream.IntStream.range(0,NestCache.CAPACITY).mapToObj(i->IntegerProperty.create("slot"+i,0,5)).toList();
    public NestCacheBlock(Properties p) { super(p);BlockState s=stateDefinition.any();for(var v:SLOTS)s=s.setValue(v,0);registerDefaultState(s); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { SLOTS.forEach(b::add); }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) { return Block.box(1,0,1,15,3,15); }
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s) { return new NestCache(p,s); }
}
