package dev.primeants.worker;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
/** A low tamped pad on a store chamber's floor. Ants walk over it; the block entity owns ownership and contents. The
 * state shows them (MaterialUnits.display): a clay heap of 0..8 levels and a heap of the other stock of 0..4 levels, four
 * units a level, and a sample lump for each other material present (stone, gravel, sand, ore): 9 x 5 x 16 = 720 states.
 * A projection only, never the inventory. */
public final class MaterialStoreBlock extends Block implements EntityBlock {
    public static final IntegerProperty CLAY = IntegerProperty.create("clay", 0, MaterialUnits.CLAY_LEVELS),
        STOCK = IntegerProperty.create("stock", 0, MaterialUnits.STOCK_LEVELS);
    public static final BooleanProperty STONE = BooleanProperty.create("stone"), GRAVEL = BooleanProperty.create("gravel"),
        SAND = BooleanProperty.create("sand"), ORE = BooleanProperty.create("ore");
    public MaterialStoreBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(CLAY, 0).setValue(STOCK, 0).setValue(STONE, false).setValue(GRAVEL, false).setValue(SAND, false).setValue(ORE, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(CLAY, STOCK, STONE, GRAVEL, SAND, ORE); }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) { return Block.box(1,0,1,15,Math.max(4+1.5*s.getValue(CLAY),4+3*s.getValue(STOCK)),15); }
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s) { return new MaterialStore(p,s); }
    /** The state that shows these contents. */
    static BlockState showing(BlockState s, List<ItemStack> contents) {
        var d = MaterialUnits.display(contents.stream().map(MaterialStore::kind).filter(java.util.Objects::nonNull).toList());
        return s.setValue(CLAY, Math.min(d.clay(), MaterialUnits.CLAY_LEVELS)).setValue(STOCK, Math.min(d.stock(), MaterialUnits.STOCK_LEVELS))
            .setValue(STONE, d.stone()).setValue(GRAVEL, d.gravel()).setValue(SAND, d.sand()).setValue(ORE, d.ore());
    }
}
