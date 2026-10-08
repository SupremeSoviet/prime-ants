package dev.primeants.mixin;
import dev.primeants.founding.NaturalSoil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
    @Shadow @Final private Level level;
    @Inject(method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V", at = @At("RETURN"))
    private void primeAntsObserve(ServerLevel level, ProtoChunk proto, LevelChunk.PostLoadProcessor post, CallbackInfo ci) {
        NaturalSoil.get(level).observeGenerated(proto);
        dev.primeants.founding.NaturalMaterials.get(level).observeGenerated(proto);
        dev.primeants.founding.NativeVegetation.get(level).observeGenerated(proto);
        dev.primeants.founding.NaturalPlacement.get(level).observeGenerated(level, proto);
    }
    // Includes ordinary placement, breaking/replacement, piston writes and ant deposits.
    // Even a same-state write revokes permission conservatively.
    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void primeAntsRevoke(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
        if (level instanceof ServerLevel server) {
            dev.primeants.founding.NaturalMaterials.get(server).invalidate(pos);
            if(!dev.primeants.founding.NativeGrassWrite.consume(server,pos,state)) NaturalSoil.get(server).invalidate(pos);
            dev.primeants.founding.NativeVegetation.get(server).invalidate(pos);
            dev.primeants.founding.ColonyPlugs.get(server).invalidate(pos);
            dev.primeants.founding.ColonyTerrain.get(server).invalidate(pos);
            dev.primeants.worker.FlowerNectar.get(server).write(pos,state);
            dev.primeants.worker.NativePrey.get(server).write(pos,state);
        }
    }
    @Inject(method="setBlockState",at=@At("RETURN"))
    private void primeAntsFailedWrite(BlockPos pos,BlockState state,int flags,CallbackInfoReturnable<BlockState> cir) {
        if(cir.getReturnValue()==null && level instanceof ServerLevel server) {
            NaturalSoil.get(server).invalidate(pos);
            dev.primeants.founding.NaturalMaterials.get(server).invalidate(pos);
        }
    }
}
