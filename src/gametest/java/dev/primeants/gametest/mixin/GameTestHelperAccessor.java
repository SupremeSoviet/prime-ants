package dev.primeants.gametest.mixin;
import net.minecraft.gametest.framework.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(GameTestHelper.class)
public interface GameTestHelperAccessor {
    @Accessor("testInfo") GameTestInfo primeAntsTestInfo();
}
