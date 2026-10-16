package dev.primeants.gametest.mixin;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.F2CarryProbe;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
public abstract class F2HandProbeMixin {
    @Inject(method="setItemSlot",at=@At("HEAD"))
    private void hand(EquipmentSlot slot,ItemStack next,CallbackInfo ci){if(slot==EquipmentSlot.MAINHAND&&(Object)this instanceof LasiusNigerEntity a)F2CarryProbe.event(a,"hand_write",a.getMainHandItem()+" -> "+next);}
}
