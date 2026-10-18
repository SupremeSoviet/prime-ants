package dev.primeants.gametest.mixin;
import dev.primeants.gametest.RoleFoodCausalProbe;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemEntity.class)
public abstract class RoleFoodItemMixin {
    @Inject(method="setItem",at=@At("HEAD")) private void before(ItemStack stack,CallbackInfo ci){RoleFoodCausalProbe.itemChange((ItemEntity)(Object)this,"before_setItem",stack);}
    @Inject(method="setItem",at=@At("RETURN")) private void after(ItemStack stack,CallbackInfo ci){RoleFoodCausalProbe.itemChange((ItemEntity)(Object)this,"after_setItem",stack);}
}
