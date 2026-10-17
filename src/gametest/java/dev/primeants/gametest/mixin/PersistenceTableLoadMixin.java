package dev.primeants.gametest.mixin;
import dev.primeants.gametest.PersistenceHarness;
import java.nio.file.Path;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(SavedDataStorage.class)
public abstract class PersistenceTableLoadMixin {
    @Shadow @Final private Path dataFolder;
    @Shadow @Final private HolderLookup.Provider registries;
    @Inject(method="readSavedData",at=@At("RETURN"))
    private void first(SavedDataType<?> type,CallbackInfoReturnable<SavedData> ci){PersistenceHarness.tableDecoded(dataFolder,type,ci.getReturnValue(),registries);}
    @Inject(method="readTagFromDisk",at=@At("RETURN"))
    private void input(Path path,DataFixTypes type,int version,CallbackInfoReturnable<CompoundTag> ci){PersistenceHarness.tableInput(path,ci.getReturnValue());}
}
