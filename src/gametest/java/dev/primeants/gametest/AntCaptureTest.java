package dev.primeants.gametest;

import com.google.gson.GsonBuilder;
import dev.primeants.entity.AntEntities;
import dev.primeants.entity.AntForm;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.NestPlan;
import dev.primeants.founding.NestExpansion;
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
        if(java.util.Set.of("appearance-t25","appearance-t26").contains(System.getProperty("prime_ants.clientScenario",""))){if("comparisons".equals(System.getProperty("prime_ants.appearanceSpecimen")))AppearanceScenario.comparisons(context);else new AppearanceScenario().run(context);return;}
        if("fresh-survival".equals(System.getProperty("prime_ants.clientScenario"))){new FreshSurvivalScenario().run(context);return;}
        if(System.getProperty("prime_ants.playerBasicsWorld")!=null){new PlayerBasicsScenario().run(context);return;}
        provenance.put("caption","T13 real colony close-ups, entrance traffic, visible brood and real cargo; observer-only mandible-region framing");
        provenance.put("run_id",runId); provenance.put("capture_prefix",prefix); provenance.put("entrypoint",getClass().getName());
        provenance.put("status","running"); provenance.put("seed",SEED); provenance.put("started_utc",Instant.now().toString());
        provenance.put("observations",observations); provenance.put("staged_terrain_edits",0); provenance.put("ant_teleports",0);
        provenance.put("ai_disabled",false); provenance.put("animation_poses_forced",false);
        provenance.put("work_multiplier",QueenFounding.multiplier()); provenance.put("cadence_ticks",QueenFounding.cadence());
        provenance.put("brood_multiplier",BroodPile.multiplier()); provenance.put("stage_ticks",BroodPile.stageTicks()); provenance.put("callow_ticks",BroodPile.callowTicks());
        provenance.put("initial_body_reserve",BroodPile.MAX_RESERVE); provenance.put("egg_cost",BroodPile.EGG_COST); provenance.put("larva_cost",BroodPile.LARVA_COST);
        provenance.put("laying_cadence",BroodPile.layingCadence());provenance.put("adult_capacity",BroodPile.ADULT_CAPACITY);provenance.put("nursery_capacity",BroodPile.CAPACITY);
        provenance.put("apple_sugar_yield",4000);provenance.put("chicken_protein_yield",8000);provenance.put("egg_sugar_cost",1000);provenance.put("egg_protein_cost",2000);provenance.put("larva_sugar_requirement",4000);provenance.put("larva_protein_requirement",8000);
        provenance.put("injected_brood",0); provenance.put("injected_food",0); provenance.put("summoned_workers",0); provenance.put("reserve_edits",0);
        provenance.put("observer_effect","vanilla /effect give @p minecraft:night_vision 99999 0 true; spectator only");
        try (TestSingleplayerContext world=context.worldBuilder().setUseConsistentSettings(false).adjustSettings(settings->{
            settings.setSeed(Long.toString(SEED)); settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE); settings.setAllowCommands(true);
        }).create()) {
            world.getConnection().waitForChunksRender(); context.waitForScreen(null);
            BlockPos searchOrigin=world.getServer().computeOnServer(server->world.getConnection().getServerLevel().getRespawnData().pos());
            provenance.put("search_origin",List.of(searchOrigin.getX(),searchOrigin.getY(),searchOrigin.getZ()));
            provenance.put("search_origin_policy","declared world spawn; bounded radius 60, independent of randomized observer respawn offset");
            BlockPos ground=world.getServer().computeOnServer(server->CaptureGround.findFounding(world.getConnection().getServerLevel(),searchOrigin));
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
            waitFor(context,world,queen,"entrance traffic",()->world.getServer().computeOnServer(server->
                    serverAnt(world,queen).founding().lifecycle()==QueenFounding.Lifecycle.OPEN && liveWorkers(world,plan).stream().anyMatch(w->w.getY()>=plan.entrance().getY() && w.workerTasks().phase()==dev.primeants.worker.WorkerTasks.Phase.EXIT)));
            world.getConnection().waitForClientboundEntityUpdates(AntEntities.WORKER);
            provenance.put("first_generation",observe(world,queen));
            capture(context,world,queen,plan,"traffic");
            capture(context,world,queen,plan,"queen-closeup");
            waitFor(context,world,queen,"unobscured colony worker close-up",()->world.getServer().computeOnServer(server->liveWorkers(world,plan).stream().anyMatch(w->serverAnt(world,queen).founding().claimedBy(w)&&w.getY()>=plan.entrance().getY()+1&&CargoView.eye(w.level(),w.position(),w.yBodyRot)!=null)));
            capture(context,world,queen,plan,"worker-closeup");
            ordinaryDrop(context,world,plan.at(-3,0,1),net.minecraft.world.item.Items.APPLE,6);
            ordinaryDrop(context,world,plan.at(-4,0,1),net.minecraft.world.item.Items.CHICKEN,8);
            waitFor(context,world,queen,"readable actual food carrier",()->world.getServer().computeOnServer(server->liveWorkers(world,plan).stream().anyMatch(w->serverAnt(world,queen).founding().claimedBy(w)&&dev.primeants.worker.WorkerTasks.food(w.getMainHandItem())&&w.getY()>=plan.entrance().getY()+1&&CargoView.eye(w.level(),w.position(),w.yBodyRot)!=null)));
            capture(context,world,queen,plan,"food-carrying");
            waitFor(context,world,queen,"visible new brood in connected original nursery",()->world.getServer().computeOnServer(server->world.getConnection().getServerLevel().getBlockEntity(plan.nursery()) instanceof BroodPile pile&&!pile.records().isEmpty()));
            capture(context,world,queen,plan,"brood-interior");
            waitFor(context,world,queen,"automatically assigned worker removing soil",()->world.getServer().computeOnServer(server->{var j=NestExpansion.get(world.getConnection().getServerLevel()).job(queen);return j!=null&&j.removed()>0&&j.removed()<12&&liveWorkers(world,plan).stream().anyMatch(w->w.getUUID().equals(j.claim)&&w.workerTasks().phase()==dev.primeants.worker.WorkerTasks.Phase.DIG);}));
            provenance.put("expansion_start",observe(world,queen));capture(context,world,queen,plan,"excavation");
            waitFor(context,world,queen,"builder transporting actual soil outside with clear item region",()->world.getServer().computeOnServer(server->{var j=NestExpansion.get(world.getConnection().getServerLevel()).job(queen);return j!=null&&liveWorkers(world,plan).stream().anyMatch(w->w.getUUID().equals(j.claim)&&w.workerTasks().phase()==dev.primeants.worker.WorkerTasks.Phase.DIG_OUT&&w.getMainHandItem().is(net.minecraft.world.item.Items.DIRT)&&w.getY()>=plan.entrance().getY()+1&&CargoView.eye(w.level(),w.position(),w.yBodyRot)!=null);}));
            capture(context,world,queen,plan,"soil-carrying");
            waitFor(context,world,queen,"finished enlarged mound with entrance traffic",()->world.getServer().computeOnServer(server->{var j=NestExpansion.get(world.getConnection().getServerLevel()).job(queen);return j!=null&&j.complete()&&liveWorkers(world,plan).stream().anyMatch(w->serverAnt(world,queen).founding().claimedBy(w)&&w.getY()>=plan.entrance().getY());}));
            capture(context,world,queen,plan,"expanded-mound");
            long previousRoomWorkers=((Number)((Map<?,?>)provenance.get("expansion_start")).get("original_room_workers")).longValue();
            waitFor(context,world,queen,"existing nurses relieving original-room congestion",()->{
                var state=observe(world,queen);return state.get("extension") instanceof Map<?,?> job&&Boolean.TRUE.equals(job.get("complete"))
                    &&((Number)state.get("extension_workers")).longValue()>=3&&((Number)state.get("original_room_workers")).longValue()<previousRoomWorkers;
            });
            capture(context,world,queen,plan,"usable-interior");
            provenance.put("final",observe(world,queen)); provenance.put("completed_utc",Instant.now().toString()); provenance.put("status","success");
        } catch(Throwable failure) { provenance.put("status","failed"); provenance.put("failure",failure.toString());provenance.put("failed_utc",Instant.now().toString());throw failure; }
        finally { try {
            Path dir=Path.of(System.getProperty("prime_ants.captureDir"));Files.createDirectories(dir);
            Files.writeString(dir.resolve(prefix+"-provenance.json"),new GsonBuilder().setPrettyPrinting().create().toJson(provenance));
        } catch(Exception e){throw new RuntimeException(e);} }
    }
    private void ordinaryDrop(ClientGameTestContext c,TestSingleplayerContext world,BlockPos feet,net.minecraft.world.item.Item item,int count) {
        world.getServer().runCommand("gamemode creative @p");
        world.getServer().runCommand(String.format(Locale.ROOT,"tp @p %.3f %.3f %.3f 0 85",feet.getX()+0.5,feet.getY()+0.1,feet.getZ()+0.5));
        world.getServer().runOnServer(server->{
            var level=world.getConnection().getServerLevel();require(NestPlan.walkable(level,feet),"Player food drop requires existing supported open trail");
            world.getConnection().getServerPlayer().setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item,count));
        });
        world.getConnection().waitForClientboundPackets();c.waitFor(client->client.player.getMainHandItem().is(item));
        c.runOnClient(client->client.gameMode.dropItem(client.player,true));world.getConnection().waitForServerboundPackets();
        var drop=world.getServer().computeOnServer(server->{var items=world.getConnection().getServerLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(feet).inflate(3),i->i.getItem().is(item));require(items.size()==1&&items.getFirst().getItem().getCount()==count,"Real supplied stack from ordinary player DROP_ALL_ITEMS packet");var i=items.getFirst();return Map.of("item",net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString(),"source_uuid",i.getUUID().toString(),"count",count,"position",List.of(i.getX(),i.getY(),i.getZ()),"method","creative inventory -> ordinary client DROP_ALL_ITEMS packet");});
        observations.add(new LinkedHashMap<>(drop));provenance.put("food_source","6 vanilla apples and 8 raw chickens from ordinary player DROP_ALL_ITEMS packets on existing trail");
        world.getServer().runCommand("gamemode spectator @p");world.getConnection().waitForClientboundPackets();
    }
    private NestPlan waitForPlan(ClientGameTestContext c,TestSingleplayerContext world,UUID id) {
        for(int i=0;i<100;i++) { var p=world.getServer().computeOnServer(server->{var f=serverAnt(world,id).founding();require(f.phase()!=QueenFounding.Phase.FAILED,f.reason());return f.plan();});if(p!=null)return p;c.waitTick(); }
        throw new AssertionError("No production site selected");
    }
    private void waitFor(ClientGameTestContext c,TestSingleplayerContext world,UUID id,String stage,java.util.function.BooleanSupplier predicate) {
        for(int i=0;i<18000;i++) {
            var state=observe(world,id);require(!state.get("phase").equals("FAILED"),"Founding failed: "+state);
            if(predicate.getAsBoolean())return;
            if(i%200==0) { observations.add(state);dev.primeants.PrimeAnts.LOGGER.info("T08 capture waiting {}: {}",stage,state); }
            c.waitTick();
        }
        throw new AssertionError("Production founding did not reach "+stage+" in 18000 observed ticks");
    }
    private void capture(ClientGameTestContext c,TestSingleplayerContext world,UUID id,NestPlan plan,String stage) {
        UUID subject = world.getServer().computeOnServer(server->{
            var job=NestExpansion.get(world.getConnection().getServerLevel()).job(id);
            if(stage.equals("excavation")||stage.equals("soil-carrying"))return job.claim;
            if(stage.equals("traffic")||stage.equals("expanded-mound"))return serverAnt(world,id).founding().workerClaim();
            if(stage.equals("worker-closeup")||stage.equals("food-carrying"))return serverAnt(world,id).founding().workerClaim();
            if(stage.equals("usable-interior"))return liveWorkers(world,plan).stream().filter(w->w.workerTasks().nursing()&&job.usable(world.getConnection().getServerLevel(),id).contains(w.blockPosition())).findFirst().orElseThrow().getUUID();
            if(stage.startsWith("nurse-"))return liveWorkers(world,plan).stream().filter(w->w.workerTasks().phase()==dev.primeants.worker.WorkerTasks.Phase.NURSE_FEED&&dev.primeants.worker.WorkerTasks.food(w.getMainHandItem())&&(!stage.equals("nurse-feeding")||w.workerTasks().feedingTicks()>0)).findFirst().orElseThrow().getUUID();
            if(stage.equals("additional-callow"))return liveWorkers(world,plan).stream().filter(LasiusNigerEntity::isCallow).findFirst().orElseThrow().getUUID();return id;
        });
        if(stage.equals("excavation")||stage.equals("soil-carrying"))c.waitFor(client->clientAnt(client,subject).getMainHandItem().is(net.minecraft.world.item.Items.DIRT));
        if(stage.startsWith("nurse-"))c.waitFor(client->dev.primeants.worker.WorkerTasks.food(clientAnt(client,subject).getMainHandItem()));
        if(stage.equals("food-carrying"))c.waitFor(client->dev.primeants.worker.WorkerTasks.food(clientAnt(client,subject).getMainHandItem()));
        c.runOnClient(client->camera(client,id,subject,plan,stage));
        var frame=observe(world,id);frame.put("event",List.of("traffic","queen-closeup","worker-closeup","food-carrying","brood-interior").contains(stage)?"colony_frame":"expansion_frame");frame.put("stage",stage);frame.put("run_id",runId);frame.put("subject_uuid",subject.toString());
        frame.put("observer",c.computeOnClient(client->Map.of("eye",List.of(client.player.getX(),client.player.getEyeY(),client.player.getZ()),"yaw",client.player.getYRot(),"pitch",client.player.getXRot(),"fov",client.options.fov().get())));
        frame.put("work_multiplier",QueenFounding.multiplier());frame.put("capture_started_utc",Instant.now().toString());observations.add(frame);
        var tracked=world.getServer().computeOnServer(server->{java.util.Set<UUID> ids=new java.util.HashSet<>();ids.add(id);liveWorkers(world,plan).forEach(w->ids.add(w.getUUID()));return ids;});
        c.runOnClient(client->AntRenderRecorder.start(tracked));
        Path png=c.takeScreenshot(TestScreenshotOptions.of(prefix+"-"+stage).disableCounterPrefix().withDeltaTicks(1).withSize(1600,1000)
                .withDestinationDir(Path.of(System.getProperty("prime_ants.captureDir"))));
        frame.put("image",png.toAbsolutePath().toString());frame.put("capture_completed_utc",Instant.now().toString());
        var renders=c.computeOnClient(client->AntRenderRecorder.finish());frame.put("screenshot_render_extractions",renders);
        require(renders.stream().anyMatch(r->subject.toString().equals(r.get("uuid"))),"Actual feeding/growth subject renders");
        if(stage.equals("excavation")||stage.equals("soil-carrying"))require(renders.stream().anyMatch(r->subject.toString().equals(r.get("uuid"))&&Boolean.TRUE.equals(r.get("carried_soil_rendered"))&&"minecraft:dirt".equals(r.get("carried_item"))),"Actual builder soil in production mandible render");
        if(stage.startsWith("nurse-"))require(renders.stream().anyMatch(r->subject.toString().equals(r.get("uuid"))&&Boolean.TRUE.equals(r.get("carried_soil_rendered"))&&List.of("minecraft:apple","minecraft:chicken","minecraft:sweet_berries").contains(r.get("carried_item"))),"Real nurse mandible cargo renders");
        if(stage.equals("food-carrying"))require(renders.stream().anyMatch(r->subject.toString().equals(r.get("uuid"))&&Boolean.TRUE.equals(r.get("carried_soil_rendered"))&&List.of("minecraft:apple","minecraft:chicken","minecraft:sweet_berries").contains(r.get("carried_item"))),"Actual forager food must still render during exposure");
        try {
            var image=javax.imageio.ImageIO.read(png.toFile());frame.put("width",image.getWidth());frame.put("height",image.getHeight());
            frame.put("modified_epoch_ms",Files.getLastModifiedTime(png).toMillis());frame.put("sha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(png))));
            Files.writeString(png.resolveSibling(png.getFileName()+".md"),"T13 "+stage+". Run "+runId+"; subject "+subject+"; queen "+id+"; nursery tick "+frame.get("nursery_ticks")+"; brood multiplier "+BroodPile.multiplier()+"; work multiplier "+QueenFounding.multiplier()+"; 1600x1000; SHA-256 "+frame.get("sha256")+".\nOne production egg, real founding/foraging/nursing/growth and bounded worker excavation/soil transport/deposition. Observer spectator with vanilla night vision. Supplied 6 apples + 8 raw chicken through ordinary player stack-drop packets. Original brood="+frame.get("original_brood_ids")+"; new brood="+frame.get("brood")+"; real workers="+frame.get("workers")+"; consumption and finite nutrition are recorded in provenance. No staged actors, terrain, food, reserve or AI edits.\n");
        } catch(Exception e){throw new RuntimeException(e);}
        dev.primeants.PrimeAnts.LOGGER.info("T08 fresh capture {}: {}",stage,frame);
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
            m.put("lifecycle",f.lifecycle().name()); m.put("ready",f.ready());
            m.put("worker_claim",f.workerClaim()==null?"":f.workerClaim().toString());
            m.put("body_reserve",ant.bodyReserve());m.put("converted",f.converted());m.put("brood_multiplier",BroodPile.multiplier());
            var nutrition=ant.nutrition();m.put("queen_nutrition",Map.of("sugar",nutrition.sugar(),"protein",nutrition.protein(),"apples",nutrition.apples(),"berries",nutrition.berries(),"chickens",nutrition.chickens(),"gained_sugar",nutrition.gainedSugar(),"gained_protein",nutrition.gainedProtein(),"spent_sugar",nutrition.spentSugar(),"spent_protein",nutrition.spentProtein()));m.put("queen_consumed",nutrition.consumedUnits());
            if(f.plan()!=null) {
                var plan=f.plan();
                var workers=liveWorkers(world,plan);
                m.put("workers",workers.stream().map(w->Map.ofEntries(Map.entry("uuid",w.getUUID().toString()),Map.entry("brood_id",w.broodId().toString()),Map.entry("queen_id",w.queenId().toString()),Map.entry("callow_ticks",w.callowAgeTicks()),Map.entry("callow_visual",w.callowVisual()),Map.entry("alive",w.isAlive()),
                        Map.entry("task",w.workerTasks().phase().name()),Map.entry("feeding_ticks",w.workerTasks().feedingTicks()),Map.entry("nursing",w.workerTasks().nursing()),Map.entry("recipient",String.valueOf(w.workerTasks().recipientId())),Map.entry("opened",w.workerTasks().opened()),Map.entry("placed",w.workerTasks().placed()),Map.entry("cargo_item",net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(w.getMainHandItem().getItem()).toString()),Map.entry("cargo_count",w.getMainHandItem().getCount()),Map.entry("position",List.of(w.getX(),w.getY(),w.getZ())))).toList());
                m.put("live_plugs",plan.plugs().stream().filter(p->dev.primeants.founding.ColonyPlugs.material(ant.level().getBlockState(p))).count());
                m.put("owned_openings",plan.plugs().stream().filter(p->dev.primeants.founding.ColonyPlugs.get((net.minecraft.server.level.ServerLevel)ant.level()).opened((net.minecraft.server.level.ServerLevel)ant.level(),p,id)).count());
                m.put("world_food",ant.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(plan.chamber()).inflate(16),i->i.isAlive()&&dev.primeants.worker.WorkerTasks.food(i.getItem())).stream().mapToInt(i->i.getItem().getCount()).sum());
                m.put("custody_food",dev.primeants.worker.TransferCustody.get((net.minecraft.server.level.ServerLevel)ant.level()).contents().stream().filter(p->dev.primeants.worker.WorkerTasks.food(p.stack())).mapToInt(p->p.stack().getCount()).sum());
                m.put("worker_soil",workers.stream().filter(w->w.getMainHandItem().is(net.minecraft.world.item.Items.DIRT)).mapToInt(w->w.getMainHandItem().getCount()).sum());
                if(ant.level().getBlockEntity(plan.cache()) instanceof dev.primeants.worker.NestCache cache) {
                    m.put("cache_capacity",cache.CAPACITY);m.put("cache_contents",cache.contents().stream().map(s->Map.of("item",net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString(),"count",s.getCount())).toList());
                } else {m.put("cache_capacity",6);m.put("cache_contents",List.of());}
                m.put("live_worker_count",workers.size());m.put("live_adult_count",workers.size()+1);
                m.put("mound_blocks",NestExpansion.deposits(plan).stream().filter(p->ant.level().getBlockState(p).is(NurseryBlocks.NEST_SOIL)).count());
                var job=NestExpansion.get((net.minecraft.server.level.ServerLevel)ant.level()).job(id);
                if(job!=null){
                    var level=(net.minecraft.server.level.ServerLevel)ant.level();
                    m.put("extension",Map.ofEntries(Map.entry("planned",job.tasks.stream().map(p->List.of(p.getX(),p.getY(),p.getZ())).toList()),Map.entry("completed",job.completed().stream().map(p->List.of(p.getX(),p.getY(),p.getZ())).toList()),Map.entry("removed",job.removed()),Map.entry("deposited",job.deposited),Map.entry("released",job.released),Map.entry("usable",job.usable(level,id).size()),Map.entry("side",job.side),Map.entry("claim",String.valueOf(job.claim)),Map.entry("ticks",job.ticks),Map.entry("reason",job.reason),Map.entry("trigger_workers",job.triggerWorkers),Map.entry("used_by",job.usedBy),Map.entry("use",job.use),Map.entry("complete",job.complete()),Map.entry("problem",String.valueOf(job.problem(level,id)))));
                    m.put("original_room_workers",workers.stream().filter(w->{var d=w.position().subtract(Vec3.atBottomCenterOf(plan.entrance()));double fwd=d.x*plan.direction().getStepX()+d.z*plan.direction().getStepZ(),side=d.x*plan.direction().getClockWise().getStepX()+d.z*plan.direction().getClockWise().getStepZ();return fwd>=2.5&&fwd<5.5&&Math.abs(side)<1.5&&w.getY()==plan.entrance().getY()-2;}).count());
                    m.put("extension_workers",workers.stream().filter(w->job.usable(level,id).contains(w.blockPosition())).count());
                    long emptyOriginal=0;for(int forwardCell=3;forwardCell<=5;forwardCell++)for(int side=-1;side<=1;side++)if(NestPlan.walkable(level,plan.at(forwardCell,side,-2)))emptyOriginal++;
                    m.put("original_usable_floor_cells",emptyOriginal);m.put("usable_floor_cells",emptyOriginal+job.usable(level,id).size());m.put("structural_floor_cells",9+job.usable(level,id).size());
                }
                m.put("world_soil",ant.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(plan.chamber()).inflate(16),i->i.isAlive()&&i.getItem().is(net.minecraft.world.item.Items.DIRT)).stream().mapToInt(i->i.getItem().getCount()).sum());
                m.put("custody_soil",dev.primeants.worker.TransferCustody.get((net.minecraft.server.level.ServerLevel)ant.level()).contents().stream().filter(p->p.stack().is(net.minecraft.world.item.Items.DIRT)).mapToInt(p->p.stack().getCount()).sum());
                m.put("underground_grass",plan.undergroundSurfaces().stream().filter(p->ant.level().getBlockState(p).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)).count());
                if(ant.level().getBlockEntity(plan.nursery()) instanceof BroodPile pile) {
                    m.put("nursery_ticks",pile.loadedTicks());m.put("nursery_condition",pile.condition());
                    m.put("brood",pile.records().stream().map(r->Map.ofEntries(Map.entry("id",r.id().toString()),Map.entry("queen_id",r.queenId().toString()),Map.entry("stage",r.stage().name()),Map.entry("progress",r.progress()),Map.entry("nourishment",r.nourishment()),Map.entry("founding",r.founding()),Map.entry("sugar",r.nutrition().sugar()),Map.entry("protein",r.nutrition().protein()),Map.entry("apples",r.nutrition().apples()),Map.entry("berries",r.nutrition().berries()),Map.entry("chickens",r.nutrition().chickens()),Map.entry("gained_sugar",r.nutrition().gainedSugar()),Map.entry("gained_protein",r.nutrition().gainedProtein()),Map.entry("spent_sugar",r.nutrition().spentSugar()),Map.entry("spent_protein",r.nutrition().spentProtein()))).toList());
                    m.put("consumed_brood_ids",pile.consumed().stream().map(UUID::toString).sorted().toList());
                    m.put("original_brood_ids",pile.original().stream().map(UUID::toString).sorted().toList());m.put("last_laying_tick",pile.lastLayingTick());m.put("larva_consumed",pile.consumedFood());m.put("larva_receipts",Map.of("apples",pile.consumedApples(),"berries",pile.consumedBerries(),"chickens",pile.consumedChickens(),"gained_sugar",pile.gainedSugar(),"gained_protein",pile.gainedProtein()));
                    m.put("occupied_worker_identities",dev.primeants.worker.ColonyMembers.get((net.minecraft.server.level.ServerLevel)ant.level()).occupied(id));
                }
            }
            m.put("on_ground",ant.onGround());m.put("ai_enabled",!ant.isNoAi());m.put("normal_physics",!ant.noPhysics);return m;
        });
    }
    private static List<LasiusNigerEntity> liveWorkers(TestSingleplayerContext world,NestPlan plan) {
        return world.getConnection().getServerLevel().getEntitiesOfClass(LasiusNigerEntity.class,new AABB(plan.chamber()).inflate(16)).stream()
                .filter(w->w.form()==AntForm.WORKER && w.isAlive() && w.broodId()!=null).toList();
    }
    private static LasiusNigerEntity clientAnt(Minecraft client, UUID uuid) {
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity.getUUID().equals(uuid)) return (LasiusNigerEntity)entity;
        }
        throw new AssertionError("Tracked specimen not present on client: " + uuid);
    }


    private static void camera(Minecraft client,UUID id,UUID subject,NestPlan plan,String stage) {
        var ant=clientAnt(client,id);Vec3 eye,target;
        if(stage.equals("queen-closeup")){
            eye=Vec3.atBottomCenterOf(plan.at(3,0,-2)).add(0,1.25,0);target=ant.position().add(0,0.4,0);client.options.fov().set(65);
        }else if(stage.equals("brood-interior")){
            eye=Vec3.atBottomCenterOf(plan.at(3,1,-2)).add(0,1.4,0);target=Vec3.atBottomCenterOf(plan.nursery()).add(0,.15,0);client.options.fov().set(95);
        }else if(stage.equals("worker-closeup")){
            var worker=clientAnt(client,subject);target=worker.position().add(0,0.25,0);eye=CargoView.eye(client.level,worker.position(),worker.yBodyRot);require(eye!=null,"Clear real worker close-up required");client.options.fov().set(45);
        }else if(stage.equals("excavation")){
            var worker=clientAnt(client,subject);eye=Vec3.atBottomCenterOf(plan.at(5,-1,-2)).add(0,1.5,0);target=worker.position().add(0,0.25,0);client.options.fov().set(90);
        }else if(stage.equals("usable-interior")){
            var worker=clientAnt(client,subject);eye=Vec3.atBottomCenterOf(plan.at(5,-1,-2)).add(0,1.5,0);target=worker.position().add(0,0.25,0);client.options.fov().set(100);
        }else if(stage.startsWith("nurse-")){
            var nurse=clientAnt(client,subject);eye=Vec3.atBottomCenterOf(plan.at(5,-1,-2)).add(0,1.45,0);target=nurse.position().add(0,0.25,0);client.options.fov().set(80);
        }else if(stage.equals("additional-callow")){
            var callow=clientAnt(client,subject);eye=Vec3.atBottomCenterOf(plan.at(5,-1,-2)).add(0,1.55,0);target=callow.position().add(0,0.2,0);client.options.fov().set(100);
        }else if(stage.equals("new-brood")){
            eye=Vec3.atBottomCenterOf(plan.at(5,-1,-2)).add(0,1.55,0);target=Vec3.atBottomCenterOf(plan.nursery()).add(0,0.15,0);client.options.fov().set(100);
        }else if(stage.equals("traffic")||stage.equals("expanded-mound")) {
            // The cleared central route gives an overhead view without placing the observer inside nearby foliage.
            eye=Vec3.atBottomCenterOf(plan.at(-3,0,1)).add(0,4,0);target=Vec3.atCenterOf(plan.at(-3,0,1));client.options.fov().set(90);
        } else if(stage.equals("sugar") || stage.equals("protein") || stage.equals("soil-carrying") || stage.equals("food-carrying")) {
            var worker=clientAnt(client,subject);target=CargoView.target(worker.position(),worker.yBodyRot);eye=CargoView.eye(client.level,worker.position(),worker.yBodyRot);
            require(eye!=null,"No unoccluded observer view of actual mandible cargo");
            client.options.fov().set(eye.subtract(worker.position()).horizontalDistanceSqr()<0.1?70:50);
        } else {
            eye=Vec3.atBottomCenterOf(plan.at(5,-1,-2)).add(-plan.direction().getStepX()*0.15,1.55,-plan.direction().getStepZ()*0.15);
            target=ant.position().add(0,0.2,0).add(Vec3.atBottomCenterOf(plan.cache()).subtract(ant.position()).scale(0.6));client.options.fov().set(90);
        }
        double eyeOffset=client.player.getEyeY()-client.player.getY();client.player.setPos(eye.x,eye.y-eyeOffset,eye.z);
        client.player.lookAt(EntityAnchorArgument.Anchor.EYES,target);
        client.player.xo=client.player.getX();client.player.yo=client.player.getY();client.player.zo=client.player.getZ();
        client.player.yRotO=client.player.getYRot();client.player.xRotO=client.player.getXRot();
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
