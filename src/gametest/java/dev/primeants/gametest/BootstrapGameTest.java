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
        // T23 targeted observer recovery uses only this isolated fixture, before any client attempt.
        var stand=context.absolutePos(new net.minecraft.core.BlockPos(3,2,3));
        context.setBlock(3,2,3,Blocks.AIR);context.setBlock(3,3,3,Blocks.AIR);
        context.assertTrue(ObserverSafety.problem(context.getLevel(),null,stand)==null,"Dry FULL-loaded full-body observer destination accepted");
        var unloaded=new net.minecraft.core.BlockPos(1000000,80,1000000);
        context.assertTrue(ObserverSafety.surface(context.getLevel(),unloaded.getX(),unloaded.getZ())==null
                &&"terrain_not_FULL".equals(ObserverSafety.problem(context.getLevel(),null,unloaded)),"Unloaded terrain rejected before height/support reads");
        context.assertTrue(ObserverSafety.problem(context.getLevel(),null,stand.atY(context.getLevel().getMinY()))!=null,"Void/minimum-Y destination rejected");
        context.setBlock(3,2,3,Blocks.WATER);
        context.assertTrue("wet_destination".equals(ObserverSafety.problem(context.getLevel(),null,stand)),"Wet feet rejected");
        context.setBlock(3,2,3,Blocks.AIR);context.setBlock(3,3,3,Blocks.STONE);
        context.assertTrue("body_collision".equals(ObserverSafety.problem(context.getLevel(),null,stand)),"Complete standing body collision rejected");
        context.setBlock(3,3,3,Blocks.AIR);
        PrimeAnts.LOGGER.info("T23 observer recovery: dry accepted; unloaded, void, wet, blocked-head rejected");
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
