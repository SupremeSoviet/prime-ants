package dev.primeants.gametest;

import com.mojang.authlib.GameProfile;
import dev.primeants.PrimeAnts;
import dev.primeants.entity.AntEntities;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.item.AntItems;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.ProblemReporter;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Fixtures may place blocks and request paths. All elapsed work is performed by the server. */
public final class AntEntityGameTest {
    @GameTest public void captureGroundRejectsObstructionsAndIncompleteFootprints(GameTestHelper c) {
        floor(c);
        BlockPos ground = c.absolutePos(new BlockPos(3, 1, 3));
        CaptureGround.generateFootprint(c.getLevel(), ground.getX() - 2, ground.getX() + 2, ground.getZ() - 2, ground.getZ() + 2);
        c.assertTrue(CaptureGround.clear(c.getLevel(), ground, 2), "FULL chunks and actual solid support/two air layers must pass");
        c.setBlock(4, 2, 3, Blocks.BIRCH_LEAVES.defaultBlockState());
        c.assertTrue(!CaptureGround.clear(c.getLevel(), ground, 2), "Property-bearing live vegetation must reject candidate");
        c.setBlock(4, 2, 3, Blocks.AIR);
        c.assertTrue(!CaptureGround.clear(c.getLevel(), c.absolutePos(new BlockPos(1, 1, 1)), 2), "Incomplete reachable floor must reject candidate");
        c.assertTrue(!CaptureGround.clear(c.getLevel(), ground.offset(160000, 0, 160000), 2), "An unloaded footprint must be rejected without trusting a heightmap");
        c.succeed();
    }
    private void floor(GameTestHelper c) {
        for (int x = 1; x <= 6; x++) for (int z = 1; z <= 6; z++) c.setBlock(x, 1, z, Blocks.STONE);
    }

    private int adults(GameTestHelper c) {
        return c.getLevel().getEntitiesOfClass(LasiusNigerEntity.class, c.getBounds(), ant -> ant.isAlive()).size();
    }

    @GameTest(maxTicks = 220) public void workerNavigatesGroundAroundWall(GameTestHelper c) { navigation(c, AntEntities.WORKER); }
    @GameTest(maxTicks = 600) public void queenNavigatesGroundAroundWall(GameTestHelper c) { navigation(c, AntEntities.QUEEN); }

    private void navigation(GameTestHelper c, EntityType<LasiusNigerEntity> type) {
        floor(c);
        for (int z = 1; z <= 4; z++) for (int y = 2; y <= 4; y++) c.setBlock(3, y, z, Blocks.STONE);
        LasiusNigerEntity ant = c.spawn(type, new Vec3(1.5, 2, 2.5));
        Vec3 start = ant.position();
        Vec3 destination = c.absoluteVec(new Vec3(5.5, 2, 2.5));
        double[] maxZ = {start.z};
        int[] movedTicks = {0};
        c.assertTrue(ant.getNavigation() instanceof GroundPathNavigation && !ant.isNoAi(), "Production ground AI must be enabled");
        c.onEachTick(() -> {
            if (c.getTick() % 20 == 0) PrimeAnts.LOGGER.info("T02 path diagnostic {} tick={}: pos={}, dest={}, onGround={}, noAi={}, path={}, done={}, delta={}",
                    ant.form(), c.getTick(), ant.position(), destination, ant.onGround(), ant.isNoAi(), ant.getNavigation().getPath(), ant.getNavigation().isDone(), ant.getDeltaMovement());
            if (ant.position().distanceToSqr(start) > 0.01) movedTicks[0]++;
            maxZ[0] = Math.max(maxZ[0], ant.getZ());
            c.assertTrue(c.getLevel().noCollision(ant, ant.getBoundingBox().deflate(0.001)), "Ant must not intersect fixture wall or floor");
        });
        c.runAfterDelay(3, () -> c.assertTrue(ant.getNavigation().moveTo(destination.x, destination.y, destination.z, 0, 1), "Exact reachable path must be found"));
        c.succeedWhen(() -> {
            c.assertTrue(ant.position().distanceToSqr(destination) < 0.8, "Ant must reach the other side using real navigation ticks");
            c.assertTrue(movedTicks[0] >= 10, "Movement must span multiple real ticks");
            c.assertTrue(maxZ[0] >= c.absoluteVec(new Vec3(0, 0, 5)).z, "Route must go around wall");
            PrimeAnts.LOGGER.info("T02 navigation {}: uuid={}, elapsedAge={}, movedTicks={}, start={}, final={}, detourMaxZ={}",
                    ant.form(), ant.getUUID(), ant.elapsedAgeTicks(), movedTicks[0], start, ant.position(), maxZ[0]);
        });
    }

    @GameTest(maxTicks = 60) public void workerAgeIgnoresDaylightChanges(GameTestHelper c) { age(c, AntEntities.WORKER); }
    @GameTest(maxTicks = 60) public void queenAgeIgnoresDaylightChanges(GameTestHelper c) { age(c, AntEntities.QUEEN); }
    private void age(GameTestHelper c, EntityType<LasiusNigerEntity> type) {
        floor(c);
        LasiusNigerEntity ant = c.spawn(type, new Vec3(2.5, 2, 2.5));
        c.runAfterDelay(3, () -> {
            long startAge = ant.elapsedAgeTicks();
            long startTime = c.getLevel().getGameTime();
            var clock = c.getLevel().dimensionType().defaultClock().orElseThrow();
            c.getLevel().getServer().clockManager().setTotalTicks(clock, 9_000_000);
            c.runAfterDelay(5, () -> c.getLevel().getServer().clockManager().setTotalTicks(clock, 0));
            c.runAfterDelay(12, () -> {
                long ticks = c.getLevel().getGameTime() - startTime;
                c.assertTrue(ticks == 12, "Exactly twelve server ticks must elapse");
                c.assertTrue(ant.elapsedAgeTicks() - startAge == ticks, "Age must advance once per live server tick, independently of daylight");
                PrimeAnts.LOGGER.info("T02 age {}: uuid={}, elapsedTicks={}, ageDelta={}", ant.form(), ant.getUUID(), ticks, ant.elapsedAgeTicks() - startAge);
                c.succeed();
            });
        });
    }

    @GameTest(maxTicks = 240, structure = "prime_ants_test:idle_ground") public void queenWandersAfterLongIdle(GameTestHelper c) {
        idleWandering(c, 5, 0);
    }
    @GameTest(maxTicks = 240, structure = "prime_ants_test:idle_ground") public void queenWandersAfterLongIdleOddPhase(GameTestHelper c) {
        idleWandering(c, 5, 1);
    }
    @GameTest(maxTicks = 240, structure = "prime_ants_test:idle_ground") public void queenWandersAfterSparseLandCandidates(GameTestHelper c) {
        idleWandering(c, 2, 1);
    }
    @GameTest(maxTicks = 240, structure = "prime_ants_test:idle_ground") public void queenWandersAfterSparseLandCandidatesEvenPhase(GameTestHelper c) {
        idleWandering(c, 2, 0);
    }
    @GameTest(maxTicks = 240, structure = "prime_ants_test:idle_ground") public void queenWandersAfterNullLandCandidates(GameTestHelper c) {
        idleWandering(c, 0, 1);
    }
    @GameTest(maxTicks = 240, structure = "prime_ants_test:idle_ground") public void queenWandersAfterNullLandCandidatesEvenPhase(GameTestHelper c) {
        idleWandering(c, 0, 0);
    }
    @GameTest(maxTicks = 240) public void queenWandersToAdjacentLandCandidate(GameTestHelper c) {
        idleWandering(c, 5, 1, false);
    }
    private void idleWandering(GameTestHelper c, long regressionSeed, int idParity) {
        idleWandering(c, regressionSeed, idParity, true);
    }
    private void idleWandering(GameTestHelper c, long regressionSeed, int idParity, boolean wide) {
        // The old 6x6 floor excluded most of the production +/-6 destination envelope.
        if (wide) {
            for (int x = 1; x <= 14; x++) for (int z = 1; z <= 14; z++) c.setBlock(x, 1, z, Blocks.STONE);
        } else floor(c); // Retain the original failing neighbor-selection regression.
        // Vanilla schedules by (tickCount + entityId) parity. Allocate/discard fixture
        // items before spawning to cover BOTH phases without changing an ant ID,
        // random draw, goal, navigation destination, idle counter or tick schedule.
        var marker = c.spawnItem(net.minecraft.world.item.Items.APPLE, 2, 4, 2);
        int nextParity = (marker.getId() + 1) % 2;
        marker.discard();
        if (nextParity != idParity) c.spawnItem(net.minecraft.world.item.Items.APPLE, 2, 4, 2).discard();
        LasiusNigerEntity queen = c.spawn(AntEntities.QUEEN, new Vec3(wide ? 7.5 : 3.5, 2, wide ? 7.5 : 3.5));
        c.assertTrue(queen.getId() % 2 == idParity, "Declared vanilla scheduler phase must be exercised");
        Vec3 start = queen.position();
        // Fixture begins beyond vanilla's idle cutoff; no destination or tick is supplied.
        queen.setNoActionTime(140);
        IdleTrace.track(queen);
        // Seed 5 FAILED before the exact-reach correction. Seeds 2/0 preserve
        // observed sparse/null-candidate sequences; no passing-seed selection fix.
        long seed = Long.parseLong(System.getProperty("prime_ants.idleSeed", Long.toString(regressionSeed)));
        queen.getRandom().setSeed(seed);
        PrimeAnts.LOGGER.info("T04 idle fixture seed={} entityId={} bounds={} start={} nearbyAdults={}", seed, queen.getId(), c.getBounds(), start, adults(c));
        c.onEachTick(() -> {
            if (c.getTick() % 20 == 0) IdleTrace.log(queen, "snapshot", "testTick=" + c.getTick() + " position=" + queen.position()
                    + " delta=" + queen.getDeltaMovement() + " onGround=" + queen.onGround() + " navigationDone=" + queen.getNavigation().isDone()
                    + " disabled=" + ((dev.primeants.gametest.mixin.IdleControlsAccessor)queen.getGoalSelector()).primeAntsDisabledFlags()
                    + " running=" + queen.getGoalSelector().getAvailableGoals().stream().filter(g -> g.isRunning()).map(g -> g.getPriority() + ":" + g.getFlags()).toList()
                    + " nearbyAdults=" + adults(c));
        });
        c.succeedWhen(() -> {
            c.assertTrue(!queen.isNoAi() && queen.getNoActionTime() > 100, "Persistent specimen remains active after long idle");
            c.assertTrue(queen.position().distanceToSqr(start) > 0.04, "Production wandering must resume after the vanilla idle cutoff");
            PrimeAnts.LOGGER.info("T02 autonomous queen resumed: uuid={}, age={}, idleTicks={}, start={}, final={}",
                    queen.getUUID(), queen.elapsedAgeTicks(), queen.getNoActionTime(), start, queen.position());
        });
    }

    @GameTest(maxTicks = 60) public void workerLethalDamageHasNoReplacement(GameTestHelper c) { death(c, AntEntities.WORKER); }
    @GameTest(maxTicks = 60) public void queenLethalDamageHasNoReplacement(GameTestHelper c) { death(c, AntEntities.QUEEN); }
    private void death(GameTestHelper c, EntityType<LasiusNigerEntity> type) {
        floor(c);
        LasiusNigerEntity ant = c.spawn(type, new Vec3(2.5, 2, 2.5));
        c.runAfterDelay(4, () -> {
            c.assertTrue(ant.hurtServer(c.getLevel(), ant.damageSources().generic(), 1000), "Normal damage path must accept lethal damage");
            c.assertTrue(!ant.isAlive() && adults(c) == 0, "Lethal damage must remove the adult from living queries immediately");
            long ageAtDeath = ant.elapsedAgeTicks();
            c.runAfterDelay(25, () -> {
                c.assertTrue(ant.isRemoved() && c.getLevel().getEntity(ant.getUUID()) == null, "Vanilla death must remove the entity");
                c.assertTrue(adults(c) == 0, "No adult may replace the dead specimen");
                c.assertTrue(ant.elapsedAgeTicks() == ageAtDeath, "Dead ants must not continue aging");
                PrimeAnts.LOGGER.info("T02 death {}: uuid={}, replacements=0", ant.form(), ant.getUUID());
                c.succeed();
            });
        });
    }

    @GameTest(maxTicks = 60) public void workerEntitySerializationPreservesIdentity(GameTestHelper c) { serialization(c, AntEntities.WORKER); }
    @GameTest(maxTicks = 60) public void queenEntitySerializationPreservesIdentity(GameTestHelper c) { serialization(c, AntEntities.QUEEN); }
    private void serialization(GameTestHelper c, EntityType<LasiusNigerEntity> type) {
        floor(c);
        LasiusNigerEntity original = c.spawn(type, new Vec3(2.5, 2, 2.5));
        c.runAfterDelay(10, () -> {
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, c.getLevel().registryAccess());
            c.assertTrue(original.save(output), "Entity must serialize through production ValueOutput");
            CompoundTag saved = output.buildResult();
            long age = original.elapsedAgeTicks();
            UUID identity = original.getUUID();
            original.discard();
            c.runAfterDelay(2, () -> {
                LasiusNigerEntity restored = (LasiusNigerEntity)EntityType.loadEntityRecursive(
                        TagValueInput.create(ProblemReporter.DISCARDING, c.getLevel().registryAccess(), saved),
                        c.getLevel(), EntitySpawnReason.LOAD, entity -> entity);
                c.assertTrue(restored != null && restored.getUUID().equals(identity) && restored.form() == original.form()
                        && restored.elapsedAgeTicks() == age, "Serialized UUID, registered form and age must round-trip");
                c.assertTrue(adults(c) == 0, "Deserialization must not automatically insert or duplicate an adult");
                c.assertTrue(c.getLevel().tryAddFreshEntityWithPassengers(restored), "Restored entity must insert once");
                LasiusNigerEntity duplicate = (LasiusNigerEntity)EntityType.loadEntityRecursive(
                        TagValueInput.create(ProblemReporter.DISCARDING, c.getLevel().registryAccess(), saved),
                        c.getLevel(), EntitySpawnReason.LOAD, entity -> entity);
                c.assertTrue(!c.getLevel().tryAddFreshEntityWithPassengers(duplicate) && adults(c) == 1,
                        "Vanilla UUID guard must reject duplicate restoration");
                long time = c.getLevel().getGameTime();
                c.runAfterDelay(8, () -> {
                    c.assertTrue(restored.elapsedAgeTicks() - age == c.getLevel().getGameTime() - time,
                            "Restored adult must continue server-owned age exactly once per tick");
                    c.assertTrue(adults(c) == 1 && c.getLevel().getEntity(identity) == restored, "One restored adult must remain");
                    PrimeAnts.LOGGER.info("T02 entity serialization {}: uuid={}, savedAge={}, duplicates=0; no world restart", restored.form(), identity, age);
                    c.succeed();
                });
            });
        });
    }

    private Player player(GameTestHelper c, GameType mode, PermissionSet permissions) {
        return new Player(c.getLevel(), new GameProfile(UUID.randomUUID(), "ant-test-player")) {
            @Override public GameType gameMode() { return mode; }
            @Override public boolean isClientAuthoritative() { return false; }
            @Override public PermissionSet permissions() { return permissions; }
        };
    }

    private InteractionResult egg(GameTestHelper c, Player player, BlockPos ground) {
        ItemStack stack = new ItemStack(AntItems.DEBUG_QUEEN_EGG);
        if (player != null) player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = c.absolutePos(ground);
        return stack.useOn(new UseOnContext(c.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false)));
    }

    @GameTest public void survivalEggCreatesNothing(GameTestHelper c) {
        floor(c);
        Player player = player(c, GameType.SURVIVAL, PermissionSet.NO_PERMISSIONS);
        c.assertTrue(egg(c, player, new BlockPos(3, 1, 3)) == InteractionResult.FAIL, "Unauthorized egg must fail");
        c.assertTrue(adults(c) == 0 && player.getMainHandItem().getCount() == 1, "Denied egg must create nothing and consume nothing");
        c.succeed();
    }
    @GameTest public void creativeEggCreatesExactlyOneQueen(GameTestHelper c) { allowedEgg(c, GameType.CREATIVE, PermissionSet.NO_PERMISSIONS); }
    @GameTest public void operatorSurvivalEggCreatesExactlyOneQueen(GameTestHelper c) { allowedEgg(c, GameType.SURVIVAL, PermissionSet.ALL_PERMISSIONS); }
    private void allowedEgg(GameTestHelper c, GameType mode, PermissionSet permissions) {
        floor(c);
        c.assertTrue(egg(c, player(c, mode, permissions), new BlockPos(3, 1, 3)).consumesAction(), "Authorized production item interaction must succeed");
        c.assertTrue(adults(c) == 1, "Egg must create exactly one adult");
        c.assertEntityPresent(AntEntities.QUEEN);
        c.assertEntityNotPresent(AntEntities.WORKER);
        c.succeed();
    }
    @GameTest public void nullPlayerEggCannotSpawn(GameTestHelper c) {
        floor(c);
        c.assertTrue(egg(c, null, new BlockPos(3, 1, 3)) == InteractionResult.FAIL && adults(c) == 0, "Automation cannot impersonate a player");
        c.succeed();
    }
    @GameTest public void debugEggCannotConfigureSpawner(GameTestHelper c) {
        c.setBlock(3, 1, 3, Blocks.SPAWNER);
        c.assertTrue(egg(c, player(c, GameType.CREATIVE, PermissionSet.ALL_PERMISSIONS), new BlockPos(3, 1, 3)) == InteractionResult.FAIL
                && adults(c) == 0, "Even operators cannot configure repeat adult creation using the egg");
        c.succeed();
    }
    @GameTest(maxTicks = 30) public void dispenserCannotBypassEggRestriction(GameTestHelper c) {
        floor(c);
        c.setBlock(3, 2, 3, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        DispenserBlockEntity dispenser = (DispenserBlockEntity)c.getLevel().getBlockEntity(c.absolutePos(new BlockPos(3, 2, 3)));
        dispenser.setItem(0, new ItemStack(AntItems.DEBUG_QUEEN_EGG));
        c.setBlock(3, 3, 3, Blocks.REDSTONE_BLOCK);
        c.runAfterDelay(12, () -> {
            c.assertTrue(adults(c) == 0 && dispenser.getItem(0).getCount() == 1, "Actual powered dispenser must retain egg and create no queen");
            c.succeed();
        });
    }
}
