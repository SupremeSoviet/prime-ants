package dev.primeants.gametest.mixin;
import dev.primeants.founding.ColonyTerrain;
import dev.primeants.gametest.InitialSurfaceHabitat;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Fixture-declared current opening ownership only; ordinary production result stays first. */
@Mixin(ColonyTerrain.class)
public abstract class InitialHabitatTerrainMixin {
    @Inject(method="opened",at=@At("RETURN"),cancellable=true)
    private void opening(ServerLevel l,BlockPos p,UUID owner,CallbackInfoReturnable<Boolean> result){
        if(!result.getReturnValue()&&InitialSurfaceHabitat.opening(l,p,owner))result.setReturnValue(true);
    }
    @Inject(method="invalidate",at=@At("HEAD"))
    private void invalidate(BlockPos p,CallbackInfo ci){InitialSurfaceHabitat.invalidate(p);}
}
