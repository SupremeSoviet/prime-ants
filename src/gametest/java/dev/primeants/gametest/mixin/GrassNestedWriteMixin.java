package dev.primeants.gametest.mixin;
import dev.primeants.gametest.GrassNestedWriteProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LevelChunk.class)
public abstract class GrassNestedWriteMixin {
    @Shadow @Final private Level level;
    @Inject(method="setBlockState",at=@At("RETURN"))
    private void after(BlockPos p,BlockState state,int flags,CallbackInfoReturnable<BlockState> ci){if(level instanceof ServerLevel l)GrassNestedWriteProbe.after(l,p);}
}
