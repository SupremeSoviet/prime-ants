package dev.primeants.gametest.mixin;
import dev.primeants.gametest.RoleFoodCausalProbe;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(SavedDataStorage.class)
public abstract class RoleFoodTableMixin {
    @Inject(method="set",at=@At("HEAD")) private void before(SavedDataType<?> type,SavedData value,CallbackInfo ci){RoleFoodCausalProbe.table((SavedDataStorage)(Object)this,type,value,"entry");}
    @Inject(method="set",at=@At("RETURN")) private void after(SavedDataType<?> type,SavedData value,CallbackInfo ci){RoleFoodCausalProbe.table((SavedDataStorage)(Object)this,type,value,"return");}
}
