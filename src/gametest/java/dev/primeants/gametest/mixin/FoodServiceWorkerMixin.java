package dev.primeants.gametest.mixin;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.FoodServiceProbe;
import dev.primeants.gametest.CaregiverCalibration;
import dev.primeants.worker.WorkerTasks;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(WorkerTasks.class)
public abstract class FoodServiceWorkerMixin {
    @Shadow @Final private LasiusNigerEntity worker;
    @Unique private net.minecraft.world.item.ItemStack primeAnts$feedingUnit;
    @Unique private java.util.UUID primeAnts$feedingRecipient;
    @Inject(method="tick",at=@At("RETURN"))
    private void observe(ServerLevel l,CallbackInfo ci){FoodServiceProbe.workerTick(l,worker);}
    @Inject(method="nurseFeed",at=@At("HEAD"))
    private void beforeReceipt(ServerLevel l,CallbackInfo ci){primeAnts$feedingUnit=worker.getMainHandItem().copy();primeAnts$feedingRecipient=worker.workerTasks().recipientId();}
    @Inject(method="nurseFeed",at=@At("RETURN"))
    private void receipt(ServerLevel l,CallbackInfo ci){
        if(worker.workerTasks().reason().equals("carried_food_consumed_once")&&!primeAnts$feedingUnit.isEmpty()&&worker.getMainHandItem().isEmpty())CaregiverCalibration.completed(l,worker,primeAnts$feedingUnit,primeAnts$feedingRecipient);
    }
}
