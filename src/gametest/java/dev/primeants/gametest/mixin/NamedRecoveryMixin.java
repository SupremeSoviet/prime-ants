package dev.primeants.gametest.mixin;

import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.NestPlan;
import dev.primeants.gametest.NamedMaterialRecovery;
import dev.primeants.worker.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Narrow read-only wrappers: each original operation executes once, with its original result. */
@Mixin(WorkerTasks.class)
public abstract class NamedRecoveryMixin {
    @Shadow @Final private LasiusNigerEntity worker;
    @Shadow private java.util.UUID source;
    @Redirect(method="pickup",at=@At(value="INVOKE",target="Ldev/primeants/entity/LasiusNigerEntity;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V"))
    private void pickupHand(LasiusNigerEntity actor,EquipmentSlot slot,ItemStack cargo){
        var id=source;actor.setItemSlot(slot,cargo);
        if(slot==EquipmentSlot.MAINHAND&&id!=null&&ItemStack.matches(actor.getMainHandItem(),cargo))NamedMaterialRecovery.pickedUp(actor,id,cargo);
    }
    @Redirect(method="store",at=@At(value="INVOKE",target="Ldev/primeants/worker/MaterialStore;deposit(Ldev/primeants/entity/LasiusNigerEntity;Ldev/primeants/founding/NestPlan;)Z"))
    private boolean depositStore(MaterialStore store,LasiusNigerEntity actor,NestPlan plan){
        var before=NamedMaterialRecovery.beforeDeposit(actor,store,plan);
        boolean result=store.deposit(actor,plan);NamedMaterialRecovery.deposited(before,store,result);return result;
    }
}
