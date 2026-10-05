package dev.primeants.gametest.mixin;
import com.google.gson.JsonObject;
import dev.primeants.founding.*;
import dev.primeants.gametest.PlacementTerrainTrace;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(NaturalPlacement.class)
public abstract class PlacementTraceMixin {
    @Inject(method="evaluateColumn",at=@At("HEAD"))
    private void begin(ServerLevel l,JsonObject r,ChunkPos p,CallbackInfo ci) {PlacementTerrainTrace.begin(l,r,p);}
    @Inject(method="evaluateColumn",at=@At("RETURN"))
    private void end(ServerLevel l,JsonObject r,ChunkPos p,CallbackInfo ci) {PlacementTerrainTrace.end(r);}
    @Redirect(method="evaluateColumn",at=@At(value="INVOKE",target="Ldev/primeants/founding/NestPlan;candidate(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Ldev/primeants/founding/NestPlan;",ordinal=0))
    private NestPlan plan(ServerLevel l,BlockPos p,Direction d) {return PlacementTerrainTrace.plan(l,p,d);}
}
