package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public final class BootstrapGameTest {
    @GameTest(maxTicks = 80)
    public void foodItemLifecycle(GameTestHelper context) {
        context.assertTrue(FabricLoader.getInstance().isModLoaded(PrimeAnts.MOD_ID), "prime_ants must be loaded");
        context.assertEntityNotPresent(EntityTypes.ITEM);
        // Only the isolated GameTest fixture is edited; the client capture world is untouched.
        context.setBlock(3, 1, 3, Blocks.STONE);
        ItemEntity food = context.spawnItem(Items.APPLE, 3.5F, 2.0F, 3.5F);
        long startTick = context.getTick();
        int initialAge = food.getAge();
        context.assertItemEntityPresent(Items.APPLE);
        PrimeAnts.LOGGER.info("T01 food created: testTick={}, age={}", startTick, initialAge);

        context.runAfterDelay(10, () -> {
            context.assertTrue(context.getTick() - startTick >= 10, "Ten real test ticks must elapse");
            context.assertTrue(food.getAge() - initialAge >= 10, "Food must age through server ticks");
            context.assertItemEntityPresent(Items.APPLE);
            PrimeAnts.LOGGER.info("T01 food observed: elapsedTestTicks={}, elapsedEntityAge={}",
                    context.getTick() - startTick, food.getAge() - initialAge);
            food.discard();

            context.runAfterDelay(3, () -> {
                context.assertTrue(food.isRemoved(), "Created food must be removed");
                context.assertEntityNotPresent(EntityTypes.ITEM);
                context.assertTrue(context.getLevel().getEntity(food.getUUID()) == null,
                        "Removed food must leave the server entity lookup");
                PrimeAnts.LOGGER.info("T01 food absent: elapsedTestTicks={}", context.getTick() - startTick);
                context.succeed();
            });
        });
    }
}
