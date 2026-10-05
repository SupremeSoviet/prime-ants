package dev.primeants.gametest.mixin;

import dev.primeants.gametest.BambooStageFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BambooSaplingBlock.class)
public abstract class BambooStageFixtureMixin {
    @Inject(method="randomTick",at=@At("HEAD"),cancellable=true)
    private void keepDeclaredSapling(BlockState state,ServerLevel level,BlockPos pos,RandomSource random,CallbackInfo ci){
        if(BambooStageFixture.holds(level,pos))ci.cancel();
    }
}
