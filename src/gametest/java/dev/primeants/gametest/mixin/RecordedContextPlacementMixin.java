package dev.primeants.gametest.mixin;
import dev.primeants.gametest.RecordedGameTestContext;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(StructureGridSpawner.class)
public abstract class RecordedContextPlacementMixin {
    @Unique private GameTestInfo primeAnts$initialFixture;
    @Inject(method="spawnStructure",at=@At("HEAD")) private void before(GameTestInfo info,CallbackInfoReturnable<Optional<GameTestInfo>> ci){primeAnts$initialFixture=info;}
    @Redirect(method="lambda$gridFor$0",at=@At(value="INVOKE",target="Ljava/util/function/Function;apply(Ljava/lang/Object;)Ljava/lang/Object;"))
    @SuppressWarnings("unchecked") private Object initialGrid(Function<Object,Object> original,Object dimension){var vanilla=(BlockPos)original.apply(dimension);return RecordedGameTestContext.initialTestBlockPos(primeAnts$initialFixture,vanilla);}
    @Redirect(method="spawnStructure",at=@At(value="INVOKE",target="Lnet/minecraft/gametest/framework/GameTestInfo;setTestBlockPos(Lnet/minecraft/core/BlockPos;)V"))
    private void initialPosition(GameTestInfo info,BlockPos vanilla){info.setTestBlockPos(RecordedGameTestContext.initialTestBlockPos(info,vanilla));}
    @Inject(method="spawnStructure",at=@At("RETURN")) private void after(GameTestInfo info,CallbackInfoReturnable<Optional<GameTestInfo>> ci){primeAnts$initialFixture=null;}
}
