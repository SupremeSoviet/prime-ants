package dev.primeants.gametest.mixin;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.QueenFounding;
import dev.primeants.gametest.TestIdentities;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(QueenFounding.class)
public abstract class TestSelectionInputsMixin {
    @Shadow @Final private LasiusNigerEntity queen;
    @Redirect(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private <T extends Entity> List<T> first(ServerLevel l,Class<T> kind,AABB box,Predicate<? super T> predicate){
        var result=l.getEntitiesOfClass(kind,box,predicate);TestIdentities.selection(queen.getUUID(),"first_forager_query",result);return result;
    }
    @Redirect(method="claimForager",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",ordinal=0))
    private <T extends Entity> List<T> additional(ServerLevel l,Class<T> kind,AABB box,Predicate<? super T> predicate){
        var result=l.getEntitiesOfClass(kind,box,predicate);TestIdentities.selection(queen.getUUID(),"additional_forager_candidates",result);return result;
    }
    @Redirect(method="claimForager",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",ordinal=1))
    private <T extends Entity> List<T> caregivers(ServerLevel l,Class<T> kind,AABB box,Predicate<? super T> predicate){
        var result=l.getEntitiesOfClass(kind,box,predicate);TestIdentities.selection(queen.getUUID(),"additional_forager_caregivers",result);return result;
    }
    @Redirect(method="tick",at=@At(value="INVOKE",target="Ljava/util/stream/Stream;min(Ljava/util/Comparator;)Ljava/util/Optional;",ordinal=0))
    private Optional<LasiusNigerEntity> chosen(Stream<LasiusNigerEntity> input,Comparator<? super LasiusNigerEntity> comparator){
        var rows=input.toList();var result=rows.stream().min(comparator);TestIdentities.chosen(queen.getUUID(),rows,result.orElse(null));return result;
    }
    @Inject(method="tick",at=@At("RETURN")) private void claims(CallbackInfo ci){TestIdentities.claims(queen);}
}
