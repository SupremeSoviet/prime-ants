package dev.primeants.gametest.mixin;
import dev.primeants.founding.ColonyTerrain;
import dev.primeants.gametest.ScaleValidationProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;
@Mixin(ColonyTerrain.class)
public abstract class ScaleTerrainProbeMixin {
    @Inject(method="matches",at=@At("RETURN")) private void readBoundary(ServerLevel l,BlockPos p,UUID owner,String kind,BlockState expected,CallbackInfoReturnable<Boolean> c){dev.primeants.gametest.ReadBoundaryWrite.afterMatches(l,p,c.getReturnValue());}
    @Inject(method="matches",at=@At("HEAD")) private void matches(CallbackInfoReturnable<?> c){ScaleValidationProbe.count(4);}
    @Inject(method="generatedOpen",at=@At("HEAD")) private void generated(CallbackInfoReturnable<?> c){ScaleValidationProbe.count(5);}
}
