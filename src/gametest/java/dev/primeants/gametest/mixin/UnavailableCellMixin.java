package dev.primeants.gametest.mixin;
import dev.primeants.founding.NestPlan;
import dev.primeants.gametest.UnavailableCells;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Reports only test-chosen cells as unavailable chunks; every other cell keeps the real chunk lookup. */
@Mixin(NestPlan.class)
public abstract class UnavailableCellMixin {
    @Inject(method="loaded",at=@At("HEAD"),cancellable=true)
    private static void primeAntsUnavailableCell(ServerLevel level,BlockPos p,CallbackInfoReturnable<Boolean> cir){
        if(UnavailableCells.hidden(level,p))cir.setReturnValue(false);
    }
}
