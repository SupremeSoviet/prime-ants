package dev.primeants.gametest.mixin;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.F2CarryProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(PathNavigation.class)
public abstract class F2PathProbeMixin {
    @Shadow @Final protected Mob mob;
    @Inject(method="createPath(Lnet/minecraft/core/BlockPos;II)Lnet/minecraft/world/level/pathfinder/Path;",at=@At("RETURN"))
    private void created(BlockPos p,int reach,int range,CallbackInfoReturnable<Path> ci){if(mob instanceof LasiusNigerEntity a)F2CarryProbe.pathResult(a,p,ci.getReturnValue());}
    @Inject(method="moveTo(DDDID)Z",at=@At("RETURN"))
    private void moved(double x,double y,double z,int reach,double speed,CallbackInfoReturnable<Boolean> ci){if(mob instanceof LasiusNigerEntity a)F2CarryProbe.event(a,"moveTo_return",x+","+y+","+z+":"+ci.getReturnValue());}
}
