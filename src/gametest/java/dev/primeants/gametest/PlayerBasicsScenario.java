package dev.primeants.gametest;

import com.google.gson.GsonBuilder;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.zip.ZipFile;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.impl.client.gametest.world.TestWorldSaveImpl;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.minecraft.commands.arguments.EntityAnchorArgument;

/** One owned native checkpoint copy; native balance failure remains explicit. Only setup player relocation/inventory; measured movement/drop is survival input. */
final class PlayerBasicsScenario {
    private final Map<String,Object> record=new LinkedHashMap<>();
    private final List<Map<String,Object>> observations=new ArrayList<>();
    private final String prefix=System.getProperty("prime_ants.capturePrefix"),runId=System.getProperty("prime_ants.runId");
    private final UUID queen=UUID.fromString("fcbd4373-abaf-3eb3-bb01-c1281fc43fba");
    void run(ClientGameTestContext c){
        record.put("run_id",runId);record.put("capture_prefix",prefix);record.put("entrypoint","dev.primeants.gametest.AntCaptureTest");record.put("scenario","player_basics");record.put("started_utc",Instant.now().toString());record.put("observations",observations);record.put("queen_uuid",queen.toString());
        record.put("source_world",System.getProperty("prime_ants.playerBasicsWorld"));record.put("native_balance","FAILED: original primary worker took starvation damage; this local smoke cannot satisfy the requested balanced-world predicate");record.put("setup_player_moves",1);record.put("measured_teleports",0);record.put("terrain_edits",0);record.put("ant_position_edits",0);record.put("ai_pauses",0);record.put("supplied_food","1 apple + 1 raw chicken; separate from native evidence");record.put("limit","Copied diagnostic dimension proves local interaction, not ordinary overworld-spawn discovery.");
        Path save=Path.of("saves",prefix).toAbsolutePath();
        try{
            require(!Files.exists(save),"Fresh owned player world required");Files.createDirectories(save);
            try(var zip=new ZipFile(System.getProperty("prime_ants.playerBasicsWorld"))){for(var entry:zip.stream().toList()){
                Path target=save.resolve(entry.getName()).normalize();require(target.startsWith(save),"Safe archive member");
                if(entry.isDirectory())Files.createDirectories(target);else{Files.createDirectories(target.getParent());try(var in=zip.getInputStream(entry)){Files.copy(in,target);}}
            }}
            c.runOnClient(client->{client.options.renderDistance().set(2);client.options.simulationDistance().set(2);}); // Bounded copied-site loading, before opening the world.
            try(TestSingleplayerContext world=new TestWorldSaveImpl(c,save).open()){
                var dimension=ResourceKey.create(Registries.DIMENSION,Identifier.fromNamespaceAndPath("prime_ants_test","placement_native"));
                NestPlan plan=world.getServer().computeOnServer(s->{var l=s.getLevel(dimension);var q=(LasiusNigerEntity)l.getEntity(queen);
                    // Diagnostic saved chunks need loading before the existing identity is available.
                    l.getChunkSource().addTicketWithRadius(PlacementSettings.TICKET,new net.minecraft.world.level.ChunkPos(9,9),4);
                    return q==null?null:q.founding().plan();});
                for(int t=0;plan==null&&t<400;t++) {c.waitTick();plan=world.getServer().computeOnServer(s->{var q=s.getLevel(dimension).getEntity(queen);return q instanceof LasiusNigerEntity a?a.founding().plan():null;});}
                require(plan!=null,"Original naturally founded queen must load");final NestPlan p=plan;
                // Existing supported central exterior; one disclosed setup move, before measurement.
                BlockPos start=world.getServer().computeOnServer(s->{var l=s.getLevel(dimension);for(int f=-5;f<=-2;f++)for(int dy=2;dy>=0;dy--){var feet=p.at(f,0,dy);if(NestPlan.loaded(l,feet)&&NestPlan.walkable(l,feet)&&l.noCollision(new AABB(feet).deflate(.2,0,.2).expandTowards(0,.8,0)))return feet;}throw new AssertionError("Supported exterior ground required");});
                world.getServer().runOnServer(s->{var player=world.getConnection().getServerPlayer();player.setGameMode(GameType.SURVIVAL);player.teleportTo(s.getLevel(dimension),start.getX()+.5,start.getY(),start.getZ()+.5,Set.of(),p.direction().toYRot(),15,true);});
                world.getConnection().waitForClientboundPackets();world.getConnection().waitForChunksRender();c.waitForScreen(null);c.waitTicks(5);
                require(c.computeOnClient(client->!client.player.isSpectator()&&!client.player.getAbilities().flying&&!client.player.noPhysics),"Ordinary survival physics throughout measured walk");
                c.runOnClient(client->{client.options.fov().set(80);client.player.lookAt(EntityAnchorArgument.Anchor.EYES,Vec3.atCenterOf(p.entrance()));});
                record.put("start",List.of(start.getX(),start.getY(),start.getZ()));record.put("local_visible_mound",world.getServer().computeOnServer(s->p.deposits().stream().anyMatch(pos->ColonyTerrain.get(s.getLevel(dimension)).mound(s.getLevel(dimension),pos,queen))));
                walk(c,Vec3.atBottomCenterOf(p.at(0,0,0)),180);
                walk(c,Vec3.atBottomCenterOf(p.at(1,0,-1)),100);
                walk(c,Vec3.atBottomCenterOf(p.at(2,0,-2)),100);
                require(c.computeOnClient(client->client.player.getY()<p.entrance().getY()-1&&client.player.position().distanceToSqr(Vec3.atBottomCenterOf(p.at(2,0,-2)))<1),"Survival input enters existing two-high tunnel");record.put("entered",true);
                // Walk the same stairs back; jump is ordinary player input for each one-block rise.
                walk(c,Vec3.atBottomCenterOf(p.at(2,0,-2)),100);walkUp(c,Vec3.atBottomCenterOf(p.at(1,0,-1)),100);walkUp(c,Vec3.atBottomCenterOf(p.at(0,0,0)),100);walkUp(c,Vec3.atBottomCenterOf(p.outside()),150);
                capture(c,"entrance");
                var drops=new ArrayList<Map<String,Object>>();
                for(var item:List.of(Items.APPLE,Items.CHICKEN)){
                    world.getServer().runOnServer(s->world.getConnection().getServerPlayer().setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item)));
                    world.getConnection().waitForClientboundPackets();c.waitFor(client->client.player.getMainHandItem().is(item));
                    c.runOnClient(client->{client.player.setXRot(65);client.gameMode.dropItem(client.player,true);});world.getConnection().waitForServerboundPackets();
                    var drop=world.getServer().computeOnServer(s->{var l=world.getConnection().getServerLevel();require(world.getConnection().getServerPlayer().getMainHandItem().isEmpty(),"Survival inventory loses supplied unit");var items=l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(p.outside()).inflate(6),e->e.isAlive()&&e.getItem().is(item));require(items.size()==1,"One actual ordinary player world item");return items.getFirst().getUUID();});
                    var trace=new LinkedHashMap<String,Object>();trace.put("item",net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString());trace.put("world_uuid",drop.toString());trace.put("inventory_loss",1);trace.put("method","survival inventory -> ordinary client DROP_ALL_ITEMS packet");
                    UUID carrier=null;
                    for(int t=0;carrier==null&&t<2200;t++){c.waitTick();carrier=world.getServer().computeOnServer(s->{var l=world.getConnection().getServerLevel();for(var e:l.getAllEntities())if(e instanceof LasiusNigerEntity w&&w.isAlive()&&queen.equals(w.queenId())&&w.getMainHandItem().is(item)&&w.founding().phase()==QueenFounding.Phase.NONE&&!w.workerTasks().nursing())return w.getUUID();return null;});}
                    require(carrier!=null,"Genuine forager collects ordinary survival drop");trace.put("carrier",carrier.toString());trace.put("carried_units",1);
                    if(item==Items.APPLE){world.getConnection().waitForClientboundPackets();c.waitTicks(2);final UUID id=carrier;c.runOnClient(client->{for(var e:client.level.entitiesForRendering())if(e.getUUID().equals(id))client.player.lookAt(EntityAnchorArgument.Anchor.EYES,e.position().add(0,.25,0));});capture(c,"feeding");}
                    boolean delivered=false;for(int t=0;!delivered&&t<1500;t++){c.waitTick();delivered=world.getServer().computeOnServer(s->{var l=world.getConnection().getServerLevel();return l.getBlockEntity(p.cache()) instanceof NestCache n&&n.ownedBy(queen,p)&&n.contents().stream().anyMatch(stack->stack.is(item))||actualIntake(l,item)>0;});}
                    require(delivered,"Supplied unit follows carried -> owned cache or actual feeding path");trace.put("owned_cache",world.getServer().computeOnServer(server->world.getConnection().getServerLevel().getBlockEntity(p.cache()) instanceof NestCache n&&n.ownedBy(queen,p)&&n.contents().stream().anyMatch(stack->stack.is(item))));trace.put("actual_feeding",world.getServer().computeOnServer(server->actualIntake(world.getConnection().getServerLevel(),item)>0));drops.add(trace);
                }
                record.put("drops",drops);record.put("status","success");record.put("completed_utc",Instant.now().toString());
            }
        }catch(Throwable t){record.put("status","failed");record.put("failure",t.toString());record.put("failed_utc",Instant.now().toString());throw new RuntimeException(t);}
        finally{try{Files.writeString(Path.of(System.getProperty("prime_ants.captureDir"),prefix+"-provenance.json"),new GsonBuilder().setPrettyPrinting().create().toJson(record));}catch(Exception e){throw new RuntimeException(e);}}
    }
    private long actualIntake(net.minecraft.server.level.ServerLevel l,Item item){
        long total=0;
        for(var e:l.getAllEntities())if(e instanceof LasiusNigerEntity a&&a.isAlive()&&queen.equals(a.colonyIdentity()))total+=item==Items.APPLE?a.nutrition().apples():a.nutrition().chickens();
        for(var e:l.getAllEntities())if(e instanceof LasiusNigerEntity q&&q.getUUID().equals(queen)&&q.founding().plan()!=null&&l.getBlockEntity(q.founding().plan().nursery()) instanceof dev.primeants.brood.BroodPile b)total+=item==Items.APPLE?b.consumedApples():b.consumedChickens();
        return total;
    }
    private void walkUp(ClientGameTestContext c,Vec3 target,int bound){c.getInput().holdKey(options->options.keyJump);try{walk(c,target,bound);}finally{c.getInput().releaseKey(options->options.keyJump);}}
    private void walk(ClientGameTestContext c,Vec3 target,int bound){
        c.getInput().holdKey(options->options.keyUp);
        try{for(int t=0;t<bound;t++){
            if(c.computeOnClient(client->client.player.position().distanceToSqr(target)<.25))return;
            c.runOnClient(client->{Vec3 d=target.subtract(client.player.position());client.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));});c.waitTick();
        }throw new AssertionError("Survival walking did not reach existing supported waypoint "+target);}
        finally{c.getInput().releaseKey(options->options.keyUp);c.waitTick();}
    }
    private void capture(ClientGameTestContext c,String stage)throws Exception{
        var frame=new LinkedHashMap<String,Object>();frame.put("stage",stage);frame.put("event","player_frame");frame.put("capture_started_utc",Instant.now().toString());
        var png=c.takeScreenshot(TestScreenshotOptions.of(prefix+"-"+stage).disableCounterPrefix().withDeltaTicks(1).withSize(1600,1000).withDestinationDir(Path.of(System.getProperty("prime_ants.captureDir"))));
        var im=javax.imageio.ImageIO.read(png.toFile());frame.put("image",png.toAbsolutePath().toString());frame.put("width",im.getWidth());frame.put("height",im.getHeight());frame.put("modified_epoch_ms",Files.getLastModifiedTime(png).toMillis());frame.put("sha256",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(png))));observations.add(frame);
        Files.writeString(png.resolveSibling(png.getFileName()+".md"),"T20 survival "+stage+" in an owned natural-colony checkpoint copy; normal movement/drop inputs, one disclosed exterior setup move; supplied food separate from native balance.\n");
    }
    private static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
}
