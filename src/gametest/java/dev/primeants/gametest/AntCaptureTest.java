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
import java.security.MessageDigest;
import java.util.HexFormat;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Fresh natural world. Only observer placement/HUD change; adults use production routes and free AI. */
public final class AntCaptureTest implements FabricClientGameTest {
    public static final long SEED = 2026100402L;
    private final List<Map<String, Object>> observations = new ArrayList<>();
    private final Map<String, Object> provenance = new LinkedHashMap<>();
    private final AntModelGameTest gameplayGeometry = new AntModelGameTest();
    private final String runId = System.getProperty("prime_ants.runId");
    private final String prefix = System.getProperty("prime_ants.capturePrefix");

    @Override
    public void runTest(ClientGameTestContext context) {
        provenance.put("caption", "T04 debug entity specimens; colony not implemented");
        provenance.put("run_id", runId);
        provenance.put("capture_prefix", prefix);
        provenance.put("entrypoint", getClass().getName());
        provenance.put("status", "running");
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
            provenance.put("ground_selection_evidence", "CaptureGround: minecraft:full across search/footprint, then live block-state and clearance queries; no saved plateau assumption");
            BlockPos ground = world.getServer().computeOnServer(server -> CaptureGround.find(world.getConnection().getServerLevel(),
                    world.getConnection().getServerPlayer().blockPosition(), null));
            provenance.put("selected_existing_ground", List.of(ground.getX(), ground.getY(), ground.getZ()));
            world.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f", ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() - 2.0));
            world.getConnection().waitForClientboundPackets();
            context.waitTicks(3);
            world.getServer().runOnServer(server -> world.getConnection().getServerPlayer().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AntItems.DEBUG_QUEEN_EGG)));
            world.getConnection().waitForClientboundPackets();
            context.waitFor(client -> client.player.getMainHandItem().is(AntItems.DEBUG_QUEEN_EGG));
            world.getServer().runOnServer(server -> require(CaptureGround.clear(world.getConnection().getServerLevel(), ground, 2), "NO_SUITABLE_GROUND before egg interaction"));
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
            world.getServer().runCommand("gamemode spectator @p");
            world.getConnection().waitForClientboundEntityUpdates(AntEntities.QUEEN);
            context.waitTicks(4);
            context.runOnClient(client -> {
                if (!client.gui.hud.isHidden()) client.gui.hud.toggle();
                client.options.fov().set(45);
            });
            capture(context, world, queen, "queen");
            UUID worker = createWorker(context, world, ground);
            world.getConnection().waitForClientboundEntityUpdates(AntEntities.WORKER);
            capture(context, world, worker, "worker");
            provenance.put("completed_utc", Instant.now().toString());
            provenance.put("status", "success");
        } catch (Throwable failure) {
            provenance.put("status", "failed");
            provenance.put("failure", failure.toString());
            provenance.put("failed_utc", Instant.now().toString());
            throw failure;
        } finally {
            try {
                Path directory = Path.of(System.getProperty("prime_ants.captureDir"));
                Files.createDirectories(directory);
                Files.writeString(directory.resolve(prefix + "-provenance.json"), new GsonBuilder().setPrettyPrinting().create().toJson(provenance));
                gameplayGeometry.writeFootTrace(directory.resolve(prefix + "-gameplay-feet.csv"));
            } catch (Exception e) { throw new RuntimeException(e); }
        }
    }

    private UUID createWorker(ClientGameTestContext context, TestSingleplayerContext world, BlockPos queenGround) {
        BlockPos workerGround = world.getServer().computeOnServer(server -> CaptureGround.find(world.getConnection().getServerLevel(), queenGround.offset(8, 0, 0), queenGround));
        provenance.put("worker_existing_ground", List.of(workerGround.getX(), workerGround.getY(), workerGround.getZ()));
        // Sequential creation avoids losing a specimen's original open ground while
        // observing the other. Once created, both specimens keep free production AI.
        String summon = String.format(Locale.ROOT, "summon prime_ants:lasius_niger_worker %.2f %.2f %.2f", workerGround.getX() + 0.5, workerGround.getY() + 1.0, workerGround.getZ() + 0.5);
        world.getServer().runOnServer(server -> require(CaptureGround.clear(world.getConnection().getServerLevel(), workerGround, 2), "NO_SUITABLE_GROUND before operator summon"));
        context.runOnClient(client -> client.player.connection.sendCommand(summon));
        world.getConnection().waitForServerboundPackets();
        UUID worker = world.getServer().computeOnServer(server -> {
            var adults = world.getConnection().getServerLevel().getEntitiesOfClass(LasiusNigerEntity.class, new AABB(workerGround).inflate(4), ant -> ant.form() == AntForm.WORKER);
            require(adults.size() == 1, "Operator summon must create one worker");
            return adults.getFirst().getUUID();
        });
        observations.add(observe(world, worker, "created", "ordinary operator /" + summon));
        return worker;
    }

    private void capture(ClientGameTestContext context, TestSingleplayerContext world, UUID uuid, String form) {
        Map<String, Object> initial = observe(world, uuid, "before_observation", "autonomous wandering");
        observations.add(initial);
        for (String view : new String[]{"side", "oblique"}) {
        for (int frame = 1; frame <= 1; frame++) {
            int waited = 0;
            while (waited < 300) {
                context.runOnClient(client -> camera(client, uuid, form, view));
                context.waitTick();
                waited++;
                boolean moving = context.computeOnClient(client -> {
                    LasiusNigerEntity ant = clientAnt(client, uuid);
                    double dx = ant.getX() - ant.xo, dz = ant.getZ() - ant.zo;
                    return ant.isAlive() && ant.onGround() && dx * dx + dz * dz > 0.000004
                            && ant.walkAnimation.speed(1) > 0.02F;
                });
                boolean unobstructed = moving && world.getServer().computeOnServer(server -> {
                    Entity ant = world.getConnection().getServerLevel().getEntity(uuid);
                    BlockPos feet = ant.blockPosition();
                    return CaptureGround.clear(world.getConnection().getServerLevel(), feet.below(), 1);
                });
                if (unobstructed) break;
                if (waited % 40 == 0) observations.add(observe(world, uuid, moving ? "waiting_for_observation_ground" : "waiting_for_walk", "AI and physics active; ground clearance and movement diagnosed separately"));
            }
            require(waited < 300, "No eligible walking frame for " + form + "; waiting events distinguish not walking from unsuitable observation ground");
            context.runOnClient(client -> camera(client, uuid, form, view));
            Map<String, Object> observation = observe(world, uuid, "walking_frame", "autonomous wandering");
            observation.put("view", view);
            observation.put("sequence_position", frame);
            observation.put("run_id", runId);
            observation.put("existing_ground_observation_clearance", "3x3 solid ground with two air blocks above; observation selection only");
            observation.put("waited_client_ticks", waited);
            observation.put("capture_started_utc", Instant.now().toString());
            observations.add(observation); // Preserve partial attempts even if later validation fails.
            context.runOnClient(client -> AntRenderRecorder.start(uuid));
            Path image = context.takeScreenshot(TestScreenshotOptions.of(prefix + "-" + form + "-" + view + "-" + frame)
                    .disableCounterPrefix().withDeltaTicks(1).withSize(1600, 1000)
                    .withDestinationDir(Path.of(System.getProperty("prime_ants.captureDir"))));
            observation.put("image", image.toAbsolutePath().toString());
            observation.put("capture_completed_utc", Instant.now().toString());
            try {
                var png = javax.imageio.ImageIO.read(image.toFile());
                observation.put("width", png.getWidth()); observation.put("height", png.getHeight());
                observation.put("modified_epoch_ms", Files.getLastModifiedTime(image).toMillis());
                observation.put("sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(image))));
                String caption = "T04 debug entity specimens; colony not implemented — " + form + ", " + view + ", walking frame. Run " + runId + ". AI, collision and physics active.\n";
                Files.writeString(image.resolveSibling(image.getFileName() + ".md"), caption);
            } catch (Exception e) { throw new RuntimeException(e); }
            var renderFrames = context.computeOnClient(client -> AntRenderRecorder.finish());
            observation.put("screenshot_render_extractions", renderFrames);
            require(renderFrames.size() == 1, "Expected exactly one screenshot-sized production render extraction; got " + renderFrames.size());
            observation.putAll(renderFrames.getFirst());
            gameplayGeometry.checkFootMotion(form.equals("queen") ? AntForm.QUEEN : AntForm.WORKER,
                    ((Number)observation.get("walk_animation_speed")).floatValue());
            observation.put("geometry_motion_verified", true);
            PrimeAnts.LOGGER.info("T04 capture {}/{} frame {}: {}", form, view, frame, observation);
        }
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

    private static void camera(Minecraft client, UUID uuid, String form, String view) {
        LasiusNigerEntity ant = clientAnt(client, uuid);
        require(client.getEntityRenderDispatcher().getRenderer(ant) instanceof AntRenderer, "Production renderer must be registered");
        double yaw = Math.toRadians(ant.yBodyRot);
        boolean queen = form.equals("queen");
        boolean sideView = view.equals("side");
        double side = sideView ? (queen ? 3.8 : 2.1) : (queen ? 1.5 : 0.9);
        double forward = sideView ? 0.25 : (queen ? 1.1 : 0.5);
        double eyeHeight = sideView ? (queen ? 1.0 : 0.55) : (queen ? 3.5 : 2.0);
        Vec3 eye = ant.position().add(side * Math.cos(yaw) - forward * Math.sin(yaw), eyeHeight,
                side * Math.sin(yaw) + forward * Math.cos(yaw));
        double playerEyeOffset = client.player.getEyeY() - client.player.getY();
        client.player.setPos(eye.x, eye.y - playerEyeOffset, eye.z);
        client.player.lookAt(EntityAnchorArgument.Anchor.EYES, ant.position().add(0, queen ? 0.35 : 0.2, 0));
        client.player.xo = client.player.getX(); client.player.yo = client.player.getY(); client.player.zo = client.player.getZ();
        client.player.yRotO = client.player.getYRot(); client.player.xRotO = client.player.getXRot();
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
