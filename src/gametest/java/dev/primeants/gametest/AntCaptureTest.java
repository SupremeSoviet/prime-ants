package dev.primeants.gametest;

import com.google.gson.GsonBuilder;
import dev.primeants.entity.AntEntities;
import dev.primeants.entity.AntForm;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.NestPlan;
import dev.primeants.founding.QueenFounding;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Fresh generated world, actual egg packet and production founding. Only spectator observer position and vanilla night vision. */
public final class AntCaptureTest implements FabricClientGameTest {
    public static final long SEED = 2026100402L;
    private final List<Map<String,Object>> observations = new ArrayList<>();
    private final Map<String,Object> provenance = new LinkedHashMap<>();
    private final String runId = System.getProperty("prime_ants.runId"), prefix = System.getProperty("prime_ants.capturePrefix");
    @Override public void runTest(ClientGameTestContext context) {
        provenance.put("caption","T05 queen physically founds a protected excavated nest");
        provenance.put("run_id",runId); provenance.put("capture_prefix",prefix); provenance.put("entrypoint",getClass().getName());
        provenance.put("status","running"); provenance.put("seed",SEED); provenance.put("started_utc",Instant.now().toString());
        provenance.put("observations",observations); provenance.put("staged_terrain_edits",0); provenance.put("ant_teleports",0);
        provenance.put("ai_disabled",false); provenance.put("animation_poses_forced",false);
        provenance.put("work_multiplier",QueenFounding.multiplier()); provenance.put("cadence_ticks",QueenFounding.cadence());
        provenance.put("observer_effect","vanilla /effect give @p minecraft:night_vision 99999 0 true; spectator only");
        try (TestSingleplayerContext world=context.worldBuilder().setUseConsistentSettings(false).adjustSettings(settings->{
            settings.setSeed(Long.toString(SEED)); settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE); settings.setAllowCommands(true);
        }).create()) {
            world.getConnection().waitForChunksRender(); context.waitForScreen(null);
            BlockPos ground=world.getServer().computeOnServer(server->CaptureGround.findFounding(world.getConnection().getServerLevel(),world.getConnection().getServerPlayer().blockPosition()));
            provenance.put("selected_existing_ground",List.of(ground.getX(),ground.getY(),ground.getZ()));
            provenance.put("origin_evidence","Production TERRAIN completion witness -> ProtoChunk FULL conversion -> persistent NaturalSoil record. No fixture origin path.");
            world.getServer().runCommand(String.format(Locale.ROOT,"tp @p %.2f %.2f %.2f",ground.getX()+0.5,ground.getY()+1.0,ground.getZ()-2.0));
            world.getConnection().waitForClientboundPackets(); context.waitTicks(3);
            world.getServer().runOnServer(server->world.getConnection().getServerPlayer().setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AntItems.DEBUG_QUEEN_EGG)));
            world.getConnection().waitForClientboundPackets(); context.waitFor(client->client.player.getMainHandItem().is(AntItems.DEBUG_QUEEN_EGG));
            context.runOnClient(client->require(client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(ground).add(0,0.5,0),Direction.UP,ground,false)).consumesAction(),"Actual egg interaction must succeed"));
            world.getConnection().waitForServerboundPackets();
            UUID queen=world.getServer().computeOnServer(server->{
                var ants=world.getConnection().getServerLevel().getEntitiesOfClass(LasiusNigerEntity.class,new AABB(ground).inflate(12));
                require(ants.size()==1 && ants.getFirst().form()==AntForm.QUEEN,"Egg creates exactly one queen and no workers"); return ants.getFirst().getUUID();
            }); provenance.put("queen_uuid",queen.toString());
            world.getServer().runCommand("gamemode spectator @p"); world.getServer().runCommand("effect give @p minecraft:night_vision 99999 0 true");
            world.getConnection().waitForClientboundEntityUpdates(AntEntities.QUEEN); context.waitTicks(4);
            context.runOnClient(client->{if(!client.gui.hud.isHidden())client.gui.hud.toggle();client.options.fov().set(70);});
            NestPlan plan=waitForPlan(context,world,queen);
            provenance.put("entrance",List.of(plan.entrance().getX(),plan.entrance().getY(),plan.entrance().getZ())); provenance.put("direction",plan.direction().getName());
            waitFor(context,world,queen,"transport",()->world.getServer().computeOnServer(server->{
                var ant=serverAnt(world,queen); var f=ant.founding();
                return f.phase()==QueenFounding.Phase.TRANSPORTING && f.carried()>0 && ant.onGround()
                        && ant.getY()>=plan.entrance().getY()+0.99 && ant.getDeltaMovement().horizontalDistanceSqr()>0.000004;
            }));
            capture(context,world,queen,plan,"transport");
            waitFor(context,world,queen,"entrance-spoil",()->world.getServer().computeOnServer(server->serverAnt(world,queen).founding().deposited()==3));
            capture(context,world,queen,plan,"entrance-spoil");
            waitFor(context,world,queen,"chamber",()->world.getServer().computeOnServer(server->serverAnt(world,queen).founding().sealed()));
            capture(context,world,queen,plan,"chamber");
            provenance.put("final",observe(world,queen)); provenance.put("completed_utc",Instant.now().toString()); provenance.put("status","success");
        } catch(Throwable failure) { provenance.put("status","failed"); provenance.put("failure",failure.toString());provenance.put("failed_utc",Instant.now().toString());throw failure; }
        finally { try {
            Path dir=Path.of(System.getProperty("prime_ants.captureDir"));Files.createDirectories(dir);
            Files.writeString(dir.resolve(prefix+"-provenance.json"),new GsonBuilder().setPrettyPrinting().create().toJson(provenance));
        } catch(Exception e){throw new RuntimeException(e);} }
    }
    private NestPlan waitForPlan(ClientGameTestContext c,TestSingleplayerContext world,UUID id) {
        for(int i=0;i<100;i++) { var p=world.getServer().computeOnServer(server->{var f=serverAnt(world,id).founding();require(f.phase()!=QueenFounding.Phase.FAILED,f.reason());return f.plan();});if(p!=null)return p;c.waitTick(); }
        throw new AssertionError("No production site selected");
    }
    private void waitFor(ClientGameTestContext c,TestSingleplayerContext world,UUID id,String stage,java.util.function.BooleanSupplier predicate) {
        for(int i=0;i<18000;i++) {
            var state=observe(world,id);require(!state.get("phase").equals("FAILED"),"Founding failed: "+state);
            if(predicate.getAsBoolean())return;
            if(i%200==0) { observations.add(state);dev.primeants.PrimeAnts.LOGGER.info("T05 capture waiting {}: {}",stage,state); }
            c.waitTick();
        }
        throw new AssertionError("Production founding did not reach "+stage+" in 18000 observed ticks");
    }
    private void capture(ClientGameTestContext c,TestSingleplayerContext world,UUID id,NestPlan plan,String stage) {
        c.runOnClient(client->camera(client,id,plan,stage));
        var frame=observe(world,id);frame.put("event","founding_frame");frame.put("stage",stage);frame.put("run_id",runId);
        frame.put("work_multiplier",QueenFounding.multiplier());frame.put("capture_started_utc",Instant.now().toString());observations.add(frame);
        c.runOnClient(client->AntRenderRecorder.start(id));
        Path png=c.takeScreenshot(TestScreenshotOptions.of(prefix+"-"+stage).disableCounterPrefix().withDeltaTicks(1).withSize(1600,1000)
                .withDestinationDir(Path.of(System.getProperty("prime_ants.captureDir"))));
        frame.put("image",png.toAbsolutePath().toString());frame.put("capture_completed_utc",Instant.now().toString());
        var renders=c.computeOnClient(client->AntRenderRecorder.finish());frame.put("screenshot_render_extractions",renders);
        if(stage.equals("transport"))require(renders.stream().anyMatch(r->Boolean.TRUE.equals(r.get("carried_soil_rendered")) && Boolean.TRUE.equals(r.get("moving"))),"Moving carried soil must use production renderer");
        if(stage.equals("chamber"))require(!renders.isEmpty(),"Settled queen must actually render inside chamber");
        try {
            var image=javax.imageio.ImageIO.read(png.toFile());frame.put("width",image.getWidth());frame.put("height",image.getHeight());
            frame.put("modified_epoch_ms",Files.getLastModifiedTime(png).toMillis());frame.put("sha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(png))));
            Files.writeString(png.resolveSibling(png.getFileName()+".md"),"T05 "+stage+". Run "+runId+"; queen "+id+"; loaded founding tick "+frame.get("founding_ticks")+"; phase "+frame.get("phase")+"; work multiplier "+QueenFounding.multiplier()+"; 1600x1000; SHA-256 "+frame.get("sha256")+".\nProduction egg/founding/navigation/rendering. No ant teleport, staged terrain or forced pose. Observer: spectator with vanilla night vision. Soil: removed="+frame.get("removed")+", carried="+frame.get("carried")+", deposited="+frame.get("deposited")+", released="+frame.get("released")+", plugging="+frame.get("plugged")+".\n");
        } catch(Exception e){throw new RuntimeException(e);}
        dev.primeants.PrimeAnts.LOGGER.info("T05 fresh capture {}: {}",stage,frame);
    }
    private static LasiusNigerEntity serverAnt(TestSingleplayerContext world,UUID id) {
        var ant=(LasiusNigerEntity)world.getConnection().getServerLevel().getEntity(id);
        require(ant!=null && ant.isAlive() && !ant.isNoAi() && !ant.noPhysics,"Living queen with normal AI and physics required");return ant;
    }
    private static Map<String,Object> observe(TestSingleplayerContext world,UUID id) {
        return world.getServer().computeOnServer(server->{var ant=serverAnt(world,id);var f=ant.founding();Map<String,Object> m=new LinkedHashMap<>();
            m.put("uuid",id.toString());m.put("form","queen");m.put("phase",f.phase().name());m.put("reason",f.reason());m.put("server_game_time",ant.level().getGameTime());
            m.put("founding_ticks",f.loadedTicks());m.put("elapsed_age_ticks",ant.elapsedAgeTicks());m.put("position",List.of(ant.getX(),ant.getY(),ant.getZ()));
            m.put("removed",f.removed());m.put("carried",f.carried());m.put("deposited",f.deposited());m.put("released",f.released());m.put("plugged",f.plugged());m.put("sealed",f.sealed());
            m.put("on_ground",ant.onGround());m.put("ai_enabled",!ant.isNoAi());m.put("normal_physics",!ant.noPhysics);return m;
        });
    }
    private static LasiusNigerEntity clientAnt(Minecraft client, UUID uuid) {
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity.getUUID().equals(uuid)) return (LasiusNigerEntity)entity;
        }
        throw new AssertionError("Tracked specimen not present on client: " + uuid);
    }


    private static void camera(Minecraft client,UUID id,NestPlan plan,String stage) {
        var ant=clientAnt(client,id);Vec3 eye,target;
        if(stage.equals("transport")) {
            double yaw=Math.toRadians(ant.yBodyRot);eye=ant.position().add(3.4*Math.cos(yaw),1.0,3.4*Math.sin(yaw));target=ant.position().add(0,0.35,0);
        } else if(stage.equals("entrance-spoil")) {
            eye=Vec3.atBottomCenterOf(plan.at(-4,-4,1)).add(0,5,0);target=Vec3.atCenterOf(plan.at(-1,0,0));
        } else {
            eye=Vec3.atBottomCenterOf(plan.at(3,-1,-2)).add(-plan.direction().getStepX()*0.2,1.25,-plan.direction().getStepZ()*0.2);
            target=ant.position().add(0,0.35,0);client.options.fov().set(80);
        }
        double eyeOffset=client.player.getEyeY()-client.player.getY();client.player.setPos(eye.x,eye.y-eyeOffset,eye.z);
        client.player.lookAt(EntityAnchorArgument.Anchor.EYES,target);
        client.player.xo=client.player.getX();client.player.yo=client.player.getY();client.player.zo=client.player.getZ();
        client.player.yRotO=client.player.getYRot();client.player.xRotO=client.player.getXRot();
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
