package dev.primeants.gametest.mixin;

import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.NamedMaterialRecovery;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe every canonical hand replacement, including unrelated transfers, without changing it. */
@Mixin(LivingEntity.class)
public abstract class CanonicalCargoObservationMixin {
    @Inject(method="setItemSlot",at=@At("HEAD"))
    private void handWrite(EquipmentSlot slot,ItemStack stack,CallbackInfo ci){
        if(slot==EquipmentSlot.MAINHAND&&(Object)this instanceof LasiusNigerEntity worker)NamedMaterialRecovery.handWrite(worker);
    }
}
