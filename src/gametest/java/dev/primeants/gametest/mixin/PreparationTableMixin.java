package dev.primeants.gametest.mixin;
import dev.primeants.gametest.MiningPreparationCausalProbe;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(SavedDataStorage.class)
public abstract class PreparationTableMixin {
    @Inject(method="set",at=@At("HEAD")) private void before(SavedDataType<?> type,SavedData data,CallbackInfo ci){MiningPreparationCausalProbe.table((SavedDataStorage)(Object)this,type,data,"before_original");}
    @Inject(method="set",at=@At("RETURN")) private void after(SavedDataType<?> type,SavedData data,CallbackInfo ci){MiningPreparationCausalProbe.table((SavedDataStorage)(Object)this,type,data,"after_original");}
}
