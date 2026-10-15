package dev.primeants.gametest.mixin;
import dev.primeants.gametest.ColonyScaleHarness;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** First normal disk decode; observational and inert outside explicit T10 reopen phases. */
@Mixin(BlockEntity.class)
public abstract class ScaleBlockLoadMixin {
    @Inject(method="loadStatic",at=@At("RETURN"))
    private static void decoded(BlockPos pos,BlockState state,CompoundTag tag,HolderLookup.Provider registries,CallbackInfoReturnable<BlockEntity> cir){if(cir.getReturnValue()!=null)ColonyScaleHarness.decoded(cir.getReturnValue(),registries);}
}
