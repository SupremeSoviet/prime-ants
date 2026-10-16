package dev.primeants.gametest.mixin;
import dev.primeants.gametest.TestIdentities;
import dev.primeants.worker.*;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin({NestCache.class,MaterialStore.class})
public abstract class TestStoreIdentityMixin {
    @Redirect(method="<init>",at=@At(value="INVOKE",target="Ljava/util/UUID;randomUUID()Ljava/util/UUID;"))
    private UUID release(){return TestIdentities.next("store-release:"+((Object)this).getClass().getName());}
}
