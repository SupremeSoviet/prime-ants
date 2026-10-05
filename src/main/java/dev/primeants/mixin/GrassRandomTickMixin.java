package dev.primeants.mixin;
import dev.primeants.founding.NativeGrassWrite;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SpreadingSnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Pinned 26.3 has exactly two spread/decay writes here; bonemeal/mycelium are excluded. */
@Mixin(SpreadingSnowyBlock.class)
public abstract class GrassRandomTickMixin {
    @Redirect(method="randomTick",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"),require=2)
    private boolean primeAntsGrassWrite(ServerLevel l,BlockPos p,BlockState next) {
        return NativeGrassWrite.write(l,p,next,(Object)this==Blocks.GRASS_BLOCK);
    }
}
