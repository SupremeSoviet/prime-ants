package dev.primeants.gametest.mixin;
import dev.primeants.brood.BroodPile;
import dev.primeants.gametest.T27ReleaseHarness;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Records the first restored canonical pile before any development tick, only in explicit T27 mode. */
@Mixin(BroodPile.class)
public abstract class T27PileLoadMixin {
    @Inject(method="serverTick",at=@At("HEAD"))
    private void t27BeforeTick(ServerLevel level,CallbackInfo ci){T27ReleaseHarness.beforePileTick((BroodPile)(Object)this,level);}
}
