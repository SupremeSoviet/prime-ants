package dev.primeants.gametest;

import com.google.gson.GsonBuilder;
import dev.primeants.PrimeAnts;
import dev.primeants.client.AntRenderer;
import dev.primeants.entity.AntEntities;
import dev.primeants.entity.AntForm;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.item.AntItems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Fresh natural world. Only observer placement/HUD change; adults use production routes and free AI. */
public final class AntCaptureTest implements FabricClientGameTest {
    public static final long SEED = 2026100402L;
    private final List<Map<String, Object>> observations = new ArrayList<>();
    private final Map<String, Object> provenance = new LinkedHashMap<>();

    @Override
    public void runTest(ClientGameTestContext context) {
        provenance.put("caption", "T02 debug entity specimens; colony not implemented");
        provenance.put("seed", SEED);
        provenance.put("started_utc", Instant.now().toString());
        provenance.put("observations", observations);
        provenance.put("terrain_edits", 0);
        provenance.put("ant_teleports", 0);
        provenance.put("ai_disabled", false);
        provenance.put("animation_poses_forced", false);
        try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(settings -> {
                    settings.setSeed(Long.toString(SEED));
                    settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    settings.setAllowCommands(true);
                }).create()) {
            world.getConnection().waitForChunksRender();
            context.waitForScreen(null);
            BlockPos ground = world.getServer().computeOnServer(server -> findGround(world.getConnection().getServerLevel(),
                    world.getConnection().getServerPlayer().blockPosition()));
            provenance.put("selected_existing_ground", List.of(ground.getX(), ground.getY(), ground.getZ()));
            world.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f", ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() - 2.0));
            world.getConnection().waitForClientboundPackets();
            context.waitTicks(3);
            world.getServer().runOnServer(server -> world.getConnection().getServerPlayer().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AntItems.DEBUG_QUEEN_EGG)));
            world.getConnection().waitForClientboundPackets();
            context.waitFor(client -> client.player.getMainHandItem().is(AntItems.DEBUG_QUEEN_EGG));
            // Real client interaction sends the normal use-on packet to the production server item path.
            context.runOnClient(client -> {
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false);
                require(client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit).consumesAction(), "Queen egg client interaction must succeed");
            });
            world.getConnection().waitForServerboundPackets();
            UUID queen = world.getServer().computeOnServer(server -> {
                var adults = world.getConnection().getServerLevel().getEntitiesOfClass(LasiusNigerEntity.class, new AABB(ground).inflate(8));
                require(adults.size() == 1 && adults.getFirst().form() == AntForm.QUEEN, "Production egg must create exactly one queen and no workers");
                return adults.getFirst().getUUID();
            });
            observations.add(observe(world, queen, "created", "production queen egg: client useItemOn -> packet -> ItemStack.useOn -> SpawnEggItem"));
            BlockPos workerGround = world.getServer().computeOnServer(server -> findGround(world.getConnection().getServerLevel(), ground.offset(8, 0, 0), ground));
            provenance.put("worker_existing_ground", List.of(workerGround.getX(), workerGround.getY(), workerGround.getZ()));
            // Ordinary permission-checked operator command, sent through the player's command connection.
            String summon = String.format(Locale.ROOT, "summon prime_ants:lasius_niger_worker %.2f %.2f %.2f", workerGround.getX() + 0.5, workerGround.getY() + 1.0, workerGround.getZ() + 0.5);
            context.runOnClient(client -> client.player.connection.sendCommand(summon));
            world.getConnection().waitForServerboundPackets();
            UUID worker = world.getServer().computeOnServer(server -> {
                var adults = world.getConnection().getServerLevel().getEntitiesOfClass(LasiusNigerEntity.class, new AABB(workerGround).inflate(4), ant -> ant.form() == AntForm.WORKER);
                require(adults.size() == 1, "Operator summon must create one worker");
                return adults.getFirst().getUUID();
            });
            observations.add(observe(world, worker, "created", "ordinary operator /" + summon));
            world.getServer().runCommand("gamemode spectator @p");
            world.getConnection().waitForClientboundEntityUpdates(AntEntities.WORKER, AntEntities.QUEEN);
            context.waitTicks(4);
            context.runOnClient(client -> {
                if (!client.gui.hud.isHidden()) client.gui.hud.toggle();
                client.options.fov().set(45);
            });
            capture(context, world, worker, "worker");
            capture(context, world, queen, "queen");
            provenance.put("completed_utc", Instant.now().toString());
            provenance.put("status", "success");
        } finally {
            try {
                Path directory = Path.of(System.getProperty("prime_ants.captureDir"));
                Files.createDirectories(directory);
                Files.writeString(directory.resolve("t02-capture-provenance.json"), new GsonBuilder().setPrettyPrinting().create().toJson(provenance));
            } catch (Exception e) { throw new RuntimeException(e); }
        }
    }

    private void capture(ClientGameTestContext context, TestSingleplayerContext world, UUID uuid, String form) {
        Map<String, Object> initial = observe(world, uuid, "before_observation", "autonomous wandering");
        observations.add(initial);
        for (int frame = 1; frame <= 3; frame++) {
            int waited = 0;
            while (waited < 300) {
                context.runOnClient(client -> camera(client, uuid, form));
                context.waitTick();
                waited++;
                boolean moving = context.computeOnClient(client -> {
                    LasiusNigerEntity ant = clientAnt(client, uuid);
                    double dx = ant.getX() - ant.xo, dz = ant.getZ() - ant.zo;
                    return ant.isAlive() && ant.onGround() && dx * dx + dz * dz > 0.000004;
                });
                if (moving) break;
                if (waited % 40 == 0) observations.add(observe(world, uuid, "waiting_for_walk", "AI and physics active"));
            }
            require(waited < 300, "No autonomous ground walking observed for " + form);
            context.runOnClient(client -> camera(client, uuid, form));
            Map<String, Object> observation = observe(world, uuid, "walking_frame_" + frame, "autonomous wandering");
            observation.put("waited_client_ticks", waited);
            observation.put("capture_utc", Instant.now().toString());
            Path image = context.takeScreenshot(TestScreenshotOptions.of(System.getProperty("prime_ants.capturePrefix") + "-" + form + "-walking-" + frame)
                    .disableCounterPrefix().withSize(1600, 1000)
                    .withDestinationDir(Path.of(System.getProperty("prime_ants.captureDir"))));
            observation.put("image", image.toAbsolutePath().toString());
            observations.add(observation);
            PrimeAnts.LOGGER.info("T02 capture {}: {}", form, observation);
            context.waitTicks(4);
        }
    }

    private static Map<String, Object> observe(TestSingleplayerContext world, UUID uuid, String event, String route) {
        return world.getServer().computeOnServer(server -> {
            LasiusNigerEntity ant = (LasiusNigerEntity)world.getConnection().getServerLevel().getEntity(uuid);
            require(ant != null && ant.isAlive() && !ant.isNoAi() && !ant.noPhysics, "Specimen must be living with production AI/physics");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("uuid", uuid.toString()); result.put("form", ant.form().serializedName());
            result.put("event", event); result.put("route", route);
            result.put("server_game_time", ant.level().getGameTime()); result.put("elapsed_age_ticks", ant.elapsedAgeTicks());
            result.put("position", List.of(ant.getX(), ant.getY(), ant.getZ()));
            result.put("delta_movement", List.of(ant.getDeltaMovement().x, ant.getDeltaMovement().y, ant.getDeltaMovement().z));
            result.put("on_ground", ant.onGround()); result.put("ai_enabled", !ant.isNoAi()); result.put("normal_physics", !ant.noPhysics);
            result.put("idle_ticks", ant.getNoActionTime()); result.put("navigation_done", ant.getNavigation().isDone());
            return result;
        });
    }

    private static LasiusNigerEntity clientAnt(Minecraft client, UUID uuid) {
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity.getUUID().equals(uuid)) return (LasiusNigerEntity)entity;
        }
        throw new AssertionError("Tracked specimen not present on client: " + uuid);
    }

    private static void camera(Minecraft client, UUID uuid, String form) {
        LasiusNigerEntity ant = clientAnt(client, uuid);
        require(client.getEntityRenderDispatcher().getRenderer(ant) instanceof AntRenderer, "Production renderer must be registered");
        double yaw = Math.toRadians(ant.yBodyRot);
        boolean queen = form.equals("queen");
        double side = queen ? 3.3 : 2.0, forward = queen ? 3.0 : 1.7;
        double eyeHeight = queen ? 2.0 : 1.25;
        Vec3 eye = ant.position().add(side * Math.cos(yaw) - forward * Math.sin(yaw), eyeHeight,
                side * Math.sin(yaw) + forward * Math.cos(yaw));
        double playerEyeOffset = client.player.getEyeY() - client.player.getY();
        client.player.setPos(eye.x, eye.y - playerEyeOffset, eye.z);
        client.player.lookAt(EntityAnchorArgument.Anchor.EYES, ant.position().add(0, queen ? 0.35 : 0.2, 0));
        client.player.xo = client.player.getX(); client.player.yo = client.player.getY(); client.player.zo = client.player.getZ();
        client.player.yRotO = client.player.getYRot(); client.player.xRotO = client.player.getXRot();
    }

    private static BlockPos findGround(ServerLevel level, BlockPos origin) {
        return findGround(level, origin, null);
    }

    private static BlockPos findGround(ServerLevel level, BlockPos origin, BlockPos otherSpecimenGround) {
        for (int radius = 0; radius <= 40; radius++) for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
            int x = origin.getX() + dx, z = origin.getZ() + dz;
            if (otherSpecimenGround != null) {
                int separationX = x - otherSpecimenGround.getX(), separationZ = z - otherSpecimenGround.getZ();
                if (separationX * separationX + separationZ * separationZ < 49) continue;
            }
            if (!level.hasChunkAt(x, z)) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockPos ground = new BlockPos(x, y, z);
            var state = level.getBlockState(ground);
            if (!(state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.STONE) || state.is(Blocks.SAND))) continue;
            boolean clear = true;
            for (int ox = -2; ox <= 2; ox++) for (int oz = -2; oz <= 2; oz++) {
                BlockPos p = ground.offset(ox, 0, oz);
                if (!level.getBlockState(p).isSolidRender() || !level.getBlockState(p.above()).isAir() || !level.getBlockState(p.above(2)).isAir()) clear = false;
            }
            if (clear) return ground;
        }
        throw new AssertionError("No existing unobstructed natural ground near observer; terrain was not altered");
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
