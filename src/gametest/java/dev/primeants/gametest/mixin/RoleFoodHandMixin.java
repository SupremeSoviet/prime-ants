package dev.primeants.gametest.mixin;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.RoleFoodCausalProbe;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
public abstract class RoleFoodHandMixin {
    @Inject(method="setItemSlot",at=@At("HEAD")) private void before(EquipmentSlot slot,ItemStack stack,CallbackInfo ci){if(slot==EquipmentSlot.MAINHAND&&(Object)this instanceof LasiusNigerEntity w)RoleFoodCausalProbe.hand(w,stack,"before_write");}
    @Inject(method="setItemSlot",at=@At("RETURN")) private void after(EquipmentSlot slot,ItemStack stack,CallbackInfo ci){if(slot==EquipmentSlot.MAINHAND&&(Object)this instanceof LasiusNigerEntity w)RoleFoodCausalProbe.hand(w,stack,"after_write");}
}
