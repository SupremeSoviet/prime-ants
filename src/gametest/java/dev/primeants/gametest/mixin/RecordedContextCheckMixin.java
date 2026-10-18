package dev.primeants.gametest.mixin;
import dev.primeants.gametest.RecordedGameTestContext;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.gametest.framework.GameTestInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(GameTestInfo.class)
public abstract class RecordedContextCheckMixin {
    @Inject(method="prepareTestStructure",at=@At("RETURN")) private void observedPreparation(CallbackInfoReturnable<GameTestInfo> ci){RecordedGameTestContext.actualPreparationReturn((GameTestInfo)(Object)this,ci.getReturnValue());}
    @Inject(method="startTest",at=@At("HEAD")) private void verifyInitialContext(CallbackInfo ci){RecordedGameTestContext.beforeOriginalBody((GameTestInfo)(Object)this);}
    @Redirect(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/gametest/framework/GameTestInfo;tickInternal()V"))
    private void originalDispatch(GameTestInfo info){if(RecordedGameTestContext.originalTickAllowed(info))((RecordedTickInvoker)info).primeAnts$originalTickInternal();}
    @Redirect(method="tick",at=@At(value="INVOKE",target="Ljava/util/stream/Stream;allMatch(Ljava/util/function/Predicate;)Z"))
    private boolean originalReadiness(Stream<Object> candidates,Predicate<Object> original){
        var info=(GameTestInfo)(Object)this;
        if(!RecordedGameTestContext.observePending(info))return candidates.allMatch(original);
        var evaluated=new ArrayList<Map<String,Object>>();
        boolean result=candidates.allMatch(chunk->{boolean accepted=original.test(chunk);evaluated.add(Map.of("chunk",chunk.toString(),"actual_result",accepted));return accepted;});
        RecordedGameTestContext.actualReadiness(info,result,evaluated);return result;
    }
}
