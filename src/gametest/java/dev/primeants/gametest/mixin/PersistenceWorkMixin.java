package dev.primeants.gametest.mixin;
import com.google.gson.JsonObject;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.PersistenceHarness;
import dev.primeants.worker.WorkerTasks;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(WorkerTasks.class)
public abstract class PersistenceWorkMixin {
    @Shadow @Final private LasiusNigerEntity worker;
    @Unique private JsonObject t19Meal,t19Feed,t19Build;
    @Inject(method="adultMeal",at=@At("HEAD")) private void mealStart(ServerLevel l,CallbackInfoReturnable<Boolean> ci){t19Meal=PersistenceHarness.actionBefore(worker,"adultMeal");}
    @Inject(method="adultMeal",at=@At("RETURN")) private void mealEnd(ServerLevel l,CallbackInfoReturnable<Boolean> ci){PersistenceHarness.actionAfter(worker,"adultMeal",t19Meal);}
    @Inject(method="nurseFeed",at=@At("HEAD")) private void feedStart(ServerLevel l,CallbackInfo ci){t19Feed=PersistenceHarness.actionBefore(worker,"nurseFeed");}
    @Inject(method="nurseFeed",at=@At("RETURN")) private void feedEnd(ServerLevel l,CallbackInfo ci){PersistenceHarness.actionAfter(worker,"nurseFeed",t19Feed);}
    @Inject(method="upgradeBuild",at=@At("HEAD")) private void buildStart(ServerLevel l,CallbackInfo ci){t19Build=PersistenceHarness.actionBefore(worker,"upgradeBuild");}
    @Inject(method="upgradeBuild",at=@At("RETURN")) private void buildEnd(ServerLevel l,CallbackInfo ci){PersistenceHarness.actionAfter(worker,"upgradeBuild",t19Build);}
}
