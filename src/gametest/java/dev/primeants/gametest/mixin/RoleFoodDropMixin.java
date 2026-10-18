package dev.primeants.gametest.mixin;
import dev.primeants.gametest.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(WorkerForagingGameTest.class)
public abstract class RoleFoodDropMixin {
    @Inject(method="drop",at=@At("RETURN")) private void drop(GameTestHelper c,BlockPos p,ItemStack stack,CallbackInfoReturnable<ItemEntity> ci){RoleFoodCausalProbe.drop(c,ci.getReturnValue());}
}
