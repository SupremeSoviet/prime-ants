package dev.primeants.worker;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.shapes.*;
/** Bounded projection of the canonical inventory: exact total 0..12 and six food-kind samples. 13 x 64 = 832 states. */
public final class NestCacheBlock extends Block implements EntityBlock {
    public static final List<String> KIND_NAMES=List.of("apple","chicken","berries","nectar","flesh","prey");
    public static final List<BooleanProperty> KINDS=KIND_NAMES.stream().map(BooleanProperty::create).toList();
    public static final IntegerProperty UNITS=IntegerProperty.create("units",0,NestCache.MAX_CAPACITY);
    public NestCacheBlock(Properties p) { super(p); var state=stateDefinition.any().setValue(UNITS,0); for(var kind:KINDS)state=state.setValue(kind,false);registerDefaultState(state); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(UNITS);KINDS.forEach(b::add); }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) { return Block.box(1,0,1,15,3,15); }
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s) { return new NestCache(p,s); }
    public static int kind(ItemStack s) {
        return s.is(dev.primeants.item.AntItems.SMALL_PREY)?5:s.is(Items.ROTTEN_FLESH)?4:s.is(Items.CHICKEN)?1:s.is(Items.SWEET_BERRIES)?2:
            (s.is(dev.primeants.item.AntItems.FLOWER_NECTAR)||s.is(dev.primeants.item.AntItems.FLOWER_NECTAR_V2))?3:0;
    }
    public static BlockState showing(BlockState state,List<ItemStack> contents) {
        var next=state.setValue(UNITS,contents.size());
        for(int i=0;i<KINDS.size();i++){final int k=i;next=next.setValue(KINDS.get(i),contents.stream().anyMatch(s->kind(s)==k));}
        return next;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level,BlockState state,BlockEntityType<T> type) {
        if(level.isClientSide()||type!=dev.primeants.brood.NurseryBlocks.CACHE_TYPE)return null;
        return (l,p,s,be)->{if(l.getGameTime()%20==0)((NestCache)be).refreshDisplay();};
    }
}
