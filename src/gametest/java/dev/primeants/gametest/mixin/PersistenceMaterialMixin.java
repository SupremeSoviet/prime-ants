package dev.primeants.gametest.mixin;
import com.google.gson.JsonObject;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.NestPlan;
import dev.primeants.gametest.PersistenceHarness;
import dev.primeants.worker.MaterialStore;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(MaterialStore.class)
public abstract class PersistenceMaterialMixin {
    @Unique private JsonObject t19Before;
    @Inject(method="deposit",at=@At("HEAD")) private void before(LasiusNigerEntity a,NestPlan p,CallbackInfoReturnable<Boolean> ci){t19Before=PersistenceHarness.actionBefore(a,"materialDeposit");}
    @Inject(method="deposit",at=@At("RETURN")) private void after(LasiusNigerEntity a,NestPlan p,CallbackInfoReturnable<Boolean> ci){PersistenceHarness.actionAfter(a,"materialDeposit",t19Before);}
}
