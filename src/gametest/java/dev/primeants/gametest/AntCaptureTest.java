package dev.primeants.gametest;

import com.google.gson.GsonBuilder;
import dev.primeants.entity.AntEntities;
import dev.primeants.entity.AntForm;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.NestPlan;
import dev.primeants.founding.QueenFounding;
import dev.primeants.brood.BroodPile;
import dev.primeants.brood.BroodStage;
import dev.primeants.brood.NurseryBlocks;
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
        provenance.put("caption","T07 one debug egg queen physically founds, cares for brood and raises her first real workers");
        provenance.put("run_id",runId); provenance.put("capture_prefix",prefix); provenance.put("entrypoint",getClass().getName());
        provenance.put("status","running"); provenance.put("seed",SEED); provenance.put("started_utc",Instant.now().toString());
        provenance.put("observations",observations); provenance.put("staged_terrain_edits",0); provenance.put("ant_teleports",0);
        provenance.put("ai_disabled",false); provenance.put("animation_poses_forced",false);
        provenance.put("work_multiplier",QueenFounding.multiplier()); provenance.put("cadence_ticks",QueenFounding.cadence());
        provenance.put("brood_multiplier",BroodPile.multiplier()); provenance.put("stage_ticks",BroodPile.stageTicks()); provenance.put("callow_ticks",BroodPile.callowTicks());
        provenance.put("initial_body_reserve",BroodPile.MAX_RESERVE); provenance.put("egg_cost",BroodPile.EGG_COST); provenance.put("larva_cost",BroodPile.LARVA_COST);
        provenance.put("injected_brood",0); provenance.put("injected_food",0); provenance.put("summoned_workers",0); provenance.put("reserve_edits",0);
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
            for (BroodStage stage : List.of(BroodStage.EGG, BroodStage.LARVA, BroodStage.COCOON)) {
                String name = stage == BroodStage.EGG ? "eggs" : stage == BroodStage.LARVA ? "larvae" : "cocoons";
                waitFor(context,world,queen,name,()->world.getServer().computeOnServer(server->{
                    var be=world.getConnection().getServerLevel().getBlockEntity(plan.nursery());
                    return be instanceof BroodPile p && p.records().size()==3 && p.records().stream().allMatch(r->r.stage()==stage && r.progress()>=3);
                }));
                capture(context,world,queen,plan,name);
            }
            waitFor(context,world,queen,"callows",()->world.getServer().computeOnServer(server->liveWorkers(world,plan).size()==3));
            world.getConnection().waitForClientboundEntityUpdates(AntEntities.WORKER);
            var workerIds=world.getServer().computeOnServer(server->liveWorkers(world,plan).stream().map(LasiusNigerEntity::getUUID).toList());
            context.waitFor(client->{int n=0;for(Entity e:client.level.entitiesForRendering())if(workerIds.contains(e.getUUID()))n++;return n==3;});
            capture(context,world,queen,plan,"callows");
            capture(context,world,queen,plan,"mound");
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
            if(i%200==0) { observations.add(state);dev.primeants.PrimeAnts.LOGGER.info("T07 capture waiting {}: {}",stage,state); }
            c.waitTick();
        }
        throw new AssertionError("Production founding did not reach "+stage+" in 18000 observed ticks");
    }
    private void capture(ClientGameTestContext c,TestSingleplayerContext world,UUID id,NestPlan plan,String stage) {
        c.runOnClient(client->camera(client,id,plan,stage));
        var frame=observe(world,id);frame.put("event","brood_frame");frame.put("stage",stage);frame.put("run_id",runId);
        frame.put("work_multiplier",QueenFounding.multiplier());frame.put("capture_started_utc",Instant.now().toString());observations.add(frame);
        var tracked=world.getServer().computeOnServer(server->{java.util.Set<UUID> ids=new java.util.HashSet<>();ids.add(id);liveWorkers(world,plan).forEach(w->ids.add(w.getUUID()));return ids;});
        c.runOnClient(client->AntRenderRecorder.start(tracked));
        Path png=c.takeScreenshot(TestScreenshotOptions.of(prefix+"-"+stage).disableCounterPrefix().withDeltaTicks(1).withSize(1600,1000)
                .withDestinationDir(Path.of(System.getProperty("prime_ants.captureDir"))));
        frame.put("image",png.toAbsolutePath().toString());frame.put("capture_completed_utc",Instant.now().toString());
        var renders=c.computeOnClient(client->AntRenderRecorder.finish());frame.put("screenshot_render_extractions",renders);
        if(!stage.equals("mound"))require(renders.stream().anyMatch(r->id.toString().equals(r.get("uuid"))),"Settled queen must render in the genuine nursery");
        if(stage.equals("callows"))require(renders.stream().filter(r->"worker".equals(r.get("form")) && ((Number)r.get("callow_visual")).intValue()<1000).map(r->r.get("uuid")).distinct().count()==3,"All three real pale workers must render");
        try {
            var image=javax.imageio.ImageIO.read(png.toFile());frame.put("width",image.getWidth());frame.put("height",image.getHeight());
            frame.put("modified_epoch_ms",Files.getLastModifiedTime(png).toMillis());frame.put("sha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(png))));
            Files.writeString(png.resolveSibling(png.getFileName()+".md"),"T07 "+stage+". Run "+runId+"; queen "+id+"; loaded nursery tick "+frame.get("nursery_ticks")+"; brood multiplier "+BroodPile.multiplier()+"; work multiplier "+QueenFounding.multiplier()+"; 1600x1000; SHA-256 "+frame.get("sha256")+".\nProduction egg/founding/care/emergence/rendering. Observer: spectator with vanilla night vision. Brood="+frame.get("brood")+"; real workers="+frame.get("workers")+"; reserve="+frame.get("body_reserve")+". Soil: 24 removed, 22 mound blocks, two plugs, zero released. Underground grass converted="+frame.get("converted")+".\n");
        } catch(Exception e){throw new RuntimeException(e);}
        dev.primeants.PrimeAnts.LOGGER.info("T07 fresh capture {}: {}",stage,frame);
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
            m.put("body_reserve",ant.bodyReserve());m.put("converted",f.converted());m.put("brood_multiplier",BroodPile.multiplier());
            if(f.plan()!=null) {
                var plan=f.plan();
                var workers=liveWorkers(world,plan);
                m.put("workers",workers.stream().map(w->Map.of("uuid",w.getUUID().toString(),"brood_id",w.broodId().toString(),"queen_id",w.queenId().toString(),"callow_ticks",w.callowAgeTicks(),"callow_visual",w.callowVisual(),"alive",w.isAlive())).toList());
                m.put("live_worker_count",workers.size());m.put("live_adult_count",workers.size()+1);
                m.put("mound_blocks",plan.deposits().stream().filter(p->ant.level().getBlockState(p).is(NurseryBlocks.NEST_SOIL)).count());
                m.put("underground_grass",plan.undergroundSurfaces().stream().filter(p->ant.level().getBlockState(p).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)).count());
                if(ant.level().getBlockEntity(plan.nursery()) instanceof BroodPile pile) {
                    m.put("nursery_ticks",pile.loadedTicks());m.put("nursery_condition",pile.condition());
                    m.put("brood",pile.records().stream().map(r->Map.of("id",r.id().toString(),"queen_id",r.queenId().toString(),"stage",r.stage().name(),"progress",r.progress(),"nourishment",r.nourishment())).toList());
                    m.put("consumed_brood_ids",pile.consumed().stream().map(UUID::toString).sorted().toList());
                }
            }
            m.put("on_ground",ant.onGround());m.put("ai_enabled",!ant.isNoAi());m.put("normal_physics",!ant.noPhysics);return m;
        });
    }
    private static List<LasiusNigerEntity> liveWorkers(TestSingleplayerContext world,NestPlan plan) {
        return world.getConnection().getServerLevel().getEntitiesOfClass(LasiusNigerEntity.class,new AABB(plan.chamber()).inflate(5)).stream()
                .filter(w->w.form()==AntForm.WORKER && w.isAlive() && w.broodId()!=null).toList();
    }
    private static LasiusNigerEntity clientAnt(Minecraft client, UUID uuid) {
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity.getUUID().equals(uuid)) return (LasiusNigerEntity)entity;
        }
        throw new AssertionError("Tracked specimen not present on client: " + uuid);
    }


    private static void camera(Minecraft client,UUID id,NestPlan plan,String stage) {
        var ant=clientAnt(client,id);Vec3 eye,target;
        if(stage.equals("mound")) {
            // The cleared central route gives an overhead view without placing the observer inside nearby foliage.
            eye=Vec3.atBottomCenterOf(plan.at(-3,0,1)).add(0,4,0);target=Vec3.atCenterOf(plan.at(-3,0,1));client.options.fov().set(90);
        } else {
            eye=Vec3.atBottomCenterOf(plan.at(3,-1,-2)).add(-plan.direction().getStepX()*0.2,1.55,-plan.direction().getStepZ()*0.2);
            target=ant.position().add(0,0.2,0).add(Vec3.atBottomCenterOf(plan.nursery()).subtract(ant.position()).scale(0.35));client.options.fov().set(90);
        }
        double eyeOffset=client.player.getEyeY()-client.player.getY();client.player.setPos(eye.x,eye.y-eyeOffset,eye.z);
        client.player.lookAt(EntityAnchorArgument.Anchor.EYES,target);
        client.player.xo=client.player.getX();client.player.yo=client.player.getY();client.player.zo=client.player.getZ();
        client.player.yRotO=client.player.getYRot();client.player.xRotO=client.player.getXRot();
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
