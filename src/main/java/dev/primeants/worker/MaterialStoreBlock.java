package dev.primeants.worker;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
/** A low tamped pad on a store chamber's floor. Ants walk over it; the block entity owns ownership and contents. The
 * state shows them: a clay heap and a heap of the other stock, each 0..4 (four units a level, full at the 16-unit
 * share), and what the other heap mostly is. A projection only, never the inventory. */
public final class MaterialStoreBlock extends Block implements EntityBlock {
    public enum Heap implements StringRepresentable {
        COBBLESTONE("cobblestone", "minecraft:cobblestone"), STONE("stone", "minecraft:stone"), GRAVEL("gravel", "minecraft:gravel"), SAND("sand", "minecraft:sand"),
        COAL("coal", "minecraft:coal"), COPPER("copper", "minecraft:raw_copper"), IRON("iron", "minecraft:raw_iron");
        private final String name, item;
        Heap(String name, String item) { this.name = name; this.item = item; }
        @Override public String getSerializedName() { return name; }
        static Heap of(ItemStack s) {
            var id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
            for (var h : values()) if (h.item.equals(id)) return h;
            return null;
        }
    }
    public static final IntegerProperty CLAY = IntegerProperty.create("clay", 0, 4), STOCK = IntegerProperty.create("stock", 0, 4);
    public static final EnumProperty<Heap> HEAP = EnumProperty.create("heap", Heap.class);
    public MaterialStoreBlock(Properties p) { super(p); registerDefaultState(stateDefinition.any().setValue(CLAY, 0).setValue(STOCK, 0).setValue(HEAP, Heap.COBBLESTONE)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(CLAY, STOCK, HEAP); }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) { return Block.box(1,0,1,15,4+3*Math.max(s.getValue(CLAY),s.getValue(STOCK)),15); }
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s) { return new MaterialStore(p,s); }
    /** The state that shows these contents; the other heap shows its most numerous item, the first such in Heap order. */
    static BlockState showing(BlockState s, List<ItemStack> contents) {
        int[] counts = new int[Heap.values().length]; long clay = 0, other = 0;
        for (var stack : contents) {
            var heap = Heap.of(stack);
            if (MaterialStore.kind(stack) == MaterialUnits.Material.CLAY) clay++;
            else if (heap != null) { counts[heap.ordinal()]++; other++; }
        }
        var most = Heap.COBBLESTONE;
        for (var h : Heap.values()) if (counts[h.ordinal()] > counts[most.ordinal()]) most = h;
        return s.setValue(CLAY, MaterialUnits.level(clay)).setValue(STOCK, MaterialUnits.level(other)).setValue(HEAP, most);
    }
}
