package dev.primeants.gametest.mixin;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.MinerCausalProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(PathNavigation.class)
public abstract class MinerPathMixin {
    @Shadow @Final protected Mob mob;
    @Inject(method="createPath(Lnet/minecraft/core/BlockPos;II)Lnet/minecraft/world/level/pathfinder/Path;",at=@At("HEAD"))
    private void beforePath(BlockPos p,int reach,int range,CallbackInfoReturnable<Path> ci){if(mob instanceof LasiusNigerEntity w&&MinerCausalProbe.observes(w))MinerCausalProbe.pathDepth++;}
    @Inject(method="createPath(Lnet/minecraft/core/BlockPos;II)Lnet/minecraft/world/level/pathfinder/Path;",at=@At("RETURN"))
    private void path(BlockPos p,int reach,int range,CallbackInfoReturnable<Path> ci){if(mob instanceof LasiusNigerEntity w){if(MinerCausalProbe.observes(w))MinerCausalProbe.pathDepth--;MinerCausalProbe.path(w,p,ci.getReturnValue());}}
    @Inject(method="moveTo(DDDID)Z",at=@At("RETURN"))
    private void move(double x,double y,double z,int reach,double speed,CallbackInfoReturnable<Boolean> ci){if(mob instanceof LasiusNigerEntity w)MinerCausalProbe.event(w,"actual_moveTo_return",x+","+y+","+z+":"+ci.getReturnValue());}
}
