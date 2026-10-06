package dev.primeants.gametest;

import com.google.gson.GsonBuilder;
import dev.primeants.entity.*;
import dev.primeants.client.AntModel;
import dev.primeants.client.AntRenderer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.impl.client.gametest.world.TestWorldSaveImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;

/** Passive appearance observations from immutable ordinary client save copies. */
public final class AppearanceScenario {
    private static final UUID QUEEN=UUID.fromString("9c71829b-25f1-3b96-acbb-96252228271d");
    private static final UUID WORKER=UUID.fromString("9a5b931b-bb9b-332d-b42a-34a6202eccca");
    private static UUID cameraSubject;
    private static double cameraSide, cameraForward, cameraHeight, targetHeight;
    public record View(Vec3 eye, float yaw, float pitch) {}
    public static View view() {
        if(cameraSubject==null)return null;
        var client=Minecraft.getInstance();var ant=clientAnt(client,cameraSubject);if(ant==null)return null;
        double yaw=Math.toRadians(ant.yBodyRot);var forward=new Vec3(-Math.sin(yaw),0,Math.cos(yaw));var side=new Vec3(forward.z,0,-forward.x);
        var eye=ant.position().add(forward.scale(cameraForward)).add(side.scale(cameraSide)).add(0,cameraHeight,0);
        var delta=ant.position().add(0,targetHeight,0).subtract(eye);
        return new View(eye,(float)Math.toDegrees(Math.atan2(-delta.x,delta.z)),(float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.horizontalDistanceSqr()))));
    }
    private final Path dir=Path.of(System.getProperty("prime_ants.captureDir"));
    private final String prefix=System.getProperty("prime_ants.capturePrefix");
    private final Map<String,Object> evidence=new LinkedHashMap<>();
    private final List<Object> captures=new ArrayList<>();
    private net.minecraft.server.MinecraftServer owned;
    private int start;
    private final int prior=Integer.getInteger("prime_ants.appearancePriorTicks",0);
    private final int attemptSpent=Integer.getInteger("prime_ants.appearanceAttemptSpent",0);
    private void save(){try{Files.createDirectories(dir);Files.writeString(dir.resolve(prefix+"-appearance.json"),new GsonBuilder().setPrettyPrinting().create().toJson(evidence),StandardCharsets.UTF_8);}catch(Exception e){throw new RuntimeException(e);}}
    private static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public void run(ClientGameTestContext c){
        require(prior>=0&&prior<12000,"Declared appearance observation allowance");
        evidence.put("run_id",System.getProperty("prime_ants.runId"));evidence.put("scenario","appearance-t24");evidence.put("status","opening");
        evidence.put("prior_ticks",prior);evidence.put("total_tick_bound",12000);evidence.put("attempt_tick_bound",1800-attemptSpent);evidence.put("attempt_ticks_already_spent",attemptSpent);evidence.put("captures",captures);
        evidence.put("ant_position_edits",0);evidence.put("ai_pauses",0);evidence.put("forced_poses",0);evidence.put("terrain_edits",0);evidence.put("supplied_ants",0);evidence.put("supplied_food",0);
        evidence.put("render_distance",8);evidence.put("simulation_distance",8);evidence.put("native_frame",List.of(1600,1000));
        evidence.put("camera_policy","Living spectator on verified dry FULL support; separate observer lens follows natural body heading. No actor or animation writes.");save();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(s->{if(owned==null){owned=s;start=s.getTickCount();save();}});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.START_SERVER_TICK.register(s->{if(s==owned)for(var p:s.getPlayerList().getPlayers())if(p.isAlive())p.setGameMode(GameType.SPECTATOR);});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(s->{if(s==owned){evidence.put("additional_ticks",s.getTickCount()-start);evidence.put("accumulated_ticks",prior+s.getTickCount()-start);save();}});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->{if(s==owned){evidence.put("normal_server_stop",true);save();}});
        c.runOnClient(client->{client.options.renderDistance().set(8);client.options.simulationDistance().set(8);client.getWindow().setWindowed(1600,1000);client.options.guiScale().set(2);client.options.fovEffectScale().set(0.0);});
        Path closed=null;
        try(var w=open(c)){
            closed=w.getWorldSave().getSaveDirectory();evidence.put("save",closed.toString());
            if(w.getServer().computeOnServer(s->!w.getConnection().getServerPlayer().isAlive())){c.waitForScreen(net.minecraft.client.gui.screens.DeathScreen.class);c.waitTicks(25);c.clickScreenButton("deathScreen.respawn");c.waitFor(client->client.player!=null&&client.player.isAlive(),200);evidence.put("respawn_recovery","ordinary respawn button");}
            c.waitForScreen(null);w.getConnection().waitForChunksRender(false);
            w.getServer().runCommand("time set noon");w.getServer().runCommand("weather clear");
            w.getServer().runCommand("effect clear @p minecraft:night_vision");
            w.getServer().runOnServer(s->{var l=w.getConnection().getServerLevel();var p=w.getConnection().getServerPlayer();require(p.isAlive(),"Living observer");p.setGameMode(GameType.SPECTATOR);p.teleportTo(l,168.5,87,149.5,Set.of(),0,30,true);});
            w.getConnection().waitForClientboundPackets();wait(c,w,10);
            for(int t=0;t<300&&!w.getServer().computeOnServer(s->w.getConnection().getServerLevel().getEntity(QUEEN)!=null);t+=10)wait(c,w,10);
            var stand=w.getServer().computeOnServer(s->{var l=w.getConnection().getServerLevel();for(int radius=3;radius<=8;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){var p=ObserverSafety.surface(l,168+dx,149+dz);if(ObserverSafety.problem(l,w.getConnection().getServerPlayer(),p)==null)return p;}return null;});
            require(stand!=null,"Dry FULL observer stand available");
            w.getServer().runOnServer(s->{var l=w.getConnection().getServerLevel();var p=w.getConnection().getServerPlayer();require(ObserverSafety.problem(l,p,stand)==null,"Safe observer support/body");p.teleportTo(l,stand.getX()+.5,stand.getY(),stand.getZ()+.5,Set.of(),0,20,true);});
            w.getConnection().waitForClientboundPackets();wait(c,w,10);
            evidence.put("safe_observer_feet",List.of(stand.getX(),stand.getY(),stand.getZ()));evidence.put("safe_observer_checks","FULL, build height/border, dry solid support, complete body space, alive and ticks advancing");
            var q=w.getServer().computeOnServer(s->w.getConnection().getServerLevel().getEntity(QUEEN));require(q instanceof LasiusNigerEntity&&q.isAlive(),"Declared living queen");
            require(w.getServer().computeOnServer(s->dev.primeants.founding.NaturalPlacement.get(w.getConnection().getServerLevel()).decisions().entrySet().stream().map(Map.Entry::getValue).anyMatch(d->"PLACED".equals(d.getAsJsonObject().get("status").getAsString())&&QUEEN.toString().equals(d.getAsJsonObject().get("queen").getAsString()))),"Production automatic placement identity");
            evidence.put("queen_identity",QUEEN.toString());
            c.runOnClient(client->{if(!client.gui.hud.isHidden())client.gui.hud.toggle();});
            if("queen".equals(System.getProperty("prime_ants.appearanceSpecimen"))){captureWhenClear(c,w,QUEEN,"queen-daylight",true,55,3.1,1.1,1.6,.35);}
            else{
                if(!"interior".equals(System.getProperty("prime_ants.appearanceSpecimen"))){
                    captureWhenClear(c,w,WORKER,"worker-daylight",true,45,1.55,.55,.48,.24);
                    for(int frame=1;frame<=3;frame++){wait(c,w,4);capture(c,w,WORKER,"worker-walk-"+frame);}
                }
                w.getServer().runCommand("effect give @p minecraft:night_vision 99999 0 true");
                captureWhenClear(c,w,QUEEN,"queen-interior",false,110,-1.1,-.25,1.9,.35);
            }
            evidence.put("status","captured");save();
        }catch(Throwable e){evidence.put("failure",e.toString());evidence.put("status","failed");save();throw new RuntimeException(e);}
        finally{cameraSubject=null;}
        require(closed!=null&&Files.exists(closed.resolve("level.dat"))&&Boolean.TRUE.equals(evidence.get("normal_server_stop")),"Normal saved closure");
        require(((Number)evidence.get("accumulated_ticks")).intValue()<=12000,"Appearance total bound");evidence.put("normal_close",true);evidence.put("status","closed");save();
    }
    private TestSingleplayerContext open(ClientGameTestContext c)throws Exception{
        String source=System.getProperty("prime_ants.appearanceSource"),selected=System.getProperty("prime_ants.appearanceRoot");require(source!=null&&selected!=null,"Explicit source and archive root");
        var target=Path.of("saves",prefix).toAbsolutePath();require(!Files.exists(target),"Fresh immutable source copy");Files.createDirectories(target);
        try(var z=new ZipFile(source)){require(z.getEntry(selected+"/level.dat")!=null,"Actual source root");require(z.stream().noneMatch(e->e.getName().startsWith(selected+"/dimensions/prime_ants_test/")),"Ordinary client world only");
            for(var entry:z.stream().toList())if(entry.getName().startsWith(selected+"/")){var p=target.resolve(entry.getName().substring(selected.length()+1)).normalize();require(p.startsWith(target),"Safe archive path");if(entry.isDirectory())Files.createDirectories(p);else{Files.createDirectories(p.getParent());try(var in=z.getInputStream(entry)){Files.copy(in,p);}}}}
        evidence.put("source_archive",source);evidence.put("archive_root",selected);save();return new TestWorldSaveImpl(c,target).open();
    }
    private void wait(ClientGameTestContext c,TestSingleplayerContext w,int ticks){int before=w.getServer().computeOnServer(s->s.getTickCount());require(attemptSpent+before-start+ticks<1700&&prior+before-start+ticks<11900,"Appearance wait bound");c.waitTicks(ticks);require(w.getServer().computeOnServer(s->s.getTickCount()>before&&w.getConnection().getServerPlayer().isAlive()),"Live observer and advancing ticks");}
    private void captureWhenClear(ClientGameTestContext c,TestSingleplayerContext w,UUID id,String name,boolean exterior,int fov,double side,double forward,double height,double target){
        c.runOnClient(client->client.options.fov().set(fov));cameraSide=side;cameraForward=forward;cameraHeight=height;targetHeight=target;cameraSubject=id;
        boolean ready=false;
        for(int t=0;t<1000;t+=5){ready=c.computeOnClient(client->{var a=clientAnt(client,id);if(a==null||!a.isAlive()||a.isCallow()||exterior&&a.getY()<66.9)return false;var v=view();return clear(client,a,v.eye());});if(ready)break;wait(c,w,5);}
        require(ready,"Declared subject unavailable/unobscured view missing: "+name);capture(c,w,id,name);
    }
    private static boolean clear(Minecraft client,LasiusNigerEntity a,Vec3 eye){
        if(!client.level.getBlockState(BlockPos.containing(eye)).isAir()||!client.level.getFluidState(BlockPos.containing(eye)).isEmpty())return false;
        double yaw=Math.toRadians(a.yBodyRot);var forward=new Vec3(-Math.sin(yaw),0,Math.cos(yaw));var side=new Vec3(forward.z,0,-forward.x);double scale=a.form()==AntForm.QUEEN?2.2:1;
        for(double z:new double[]{-.47,.0,.47})for(double x:new double[]{-.22,.22})for(double y:new double[]{.12,.42})if(client.level.clip(new net.minecraft.world.level.ClipContext(eye,a.position().add(forward.scale(z*scale)).add(side.scale(x*scale)).add(0,y,0),net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,net.minecraft.world.phys.shapes.CollisionContext.empty())).getType()!=HitResult.Type.MISS)return false;
        // The broad body probes do not cover long scapes/jaws. Reuse the baked
        // production mesh/state, without touching the actual renderer or actor.
        var renderer=(AntRenderer)client.getEntityRenderDispatcher().getRenderer(a);
        var state=renderer.createRenderState();renderer.extractRenderState(a,state,1);
        var model=new AntModel(AntModel.createBodyLayer(a.form()).bakeRoot());model.setupAnim(state);
        boolean[] visible={true};double angle=Math.toRadians(state.bodyRot),cos=Math.cos(angle),sin=Math.sin(angle);
        model.root().visit(new com.mojang.blaze3d.vertex.PoseStack(),(pose,path,index,cube)->{
            if(!visible[0]||!path.startsWith("/ant/head"))return;
            for(var polygon:cube.polygons)for(var vertex:polygon.vertices()){
                var p=pose.pose().transformPosition(new org.joml.Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ()));
                var target=a.position().add(cos*p.x+sin*p.z,1.501-p.y,sin*p.x-cos*p.z);
                if(client.level.clip(new net.minecraft.world.level.ClipContext(eye,target,net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,net.minecraft.world.phys.shapes.CollisionContext.empty())).getType()!=HitResult.Type.MISS){visible[0]=false;return;}
            }
        });
        if(!visible[0])return false;
        return true;
    }
    private void capture(ClientGameTestContext c,TestSingleplayerContext w,UUID id,String name){
        c.runOnClient(client->AntRenderRecorder.start(id));var png=c.takeScreenshot(TestScreenshotOptions.of(prefix+"-"+name).disableCounterPrefix().withDeltaTicks(1).withDestinationDir(dir));
        var frame=new LinkedHashMap<String,Object>();frame.put("name",name);frame.put("image",png.toString());frame.put("subject_uuid",id.toString());frame.put("server_tick",w.getServer().computeOnServer(s->s.getTickCount()));
        frame.put("recipe",Map.of("side",cameraSide,"forward",cameraForward,"height",cameraHeight,"target_height",targetHeight,"fov",c.computeOnClient(client->client.options.fov().get())));
        frame.put("camera",c.computeOnClient(client->{var v=view();return Map.of("eye",List.of(v.eye.x,v.eye.y,v.eye.z),"yaw",v.yaw,"pitch",v.pitch);}));
        var rendered=c.computeOnClient(client->AntRenderRecorder.finish());frame.put("rendered_ants",rendered);captures.add(frame);save();require(!rendered.isEmpty(),"Actual stable 1600x1000 rendered subject required");
    }
    private static LasiusNigerEntity clientAnt(Minecraft client,UUID id){if(client.level!=null)for(var e:client.level.entitiesForRendering())if(e instanceof LasiusNigerEntity a&&a.getUUID().equals(id))return a;return null;}

    /** Two ordinary save observations in one process; every world closes separately.
     * Counts add across copies and the shared 1,800-tick attempt limit remains. */
    public static void comparisons(ClientGameTestContext c){
        String base=System.getProperty("prime_ants.capturePrefix");
        Path dir=Path.of(System.getProperty("prime_ants.captureDir"));
        int prior=Integer.getInteger("prime_ants.appearancePriorTicks",0);
        String[] keys={"prime_ants.capturePrefix","prime_ants.appearanceSource","prime_ants.appearanceRoot","prime_ants.appearanceSpecimen","prime_ants.appearancePriorTicks","prime_ants.appearanceAttemptSpent"};
        var original=new HashMap<String,String>();for(String key:keys)original.put(key,System.getProperty(key));
        var attempted=new ArrayList<Path>();var segments=new ArrayList<Object>();var frames=new ArrayList<Object>();
        var summary=new LinkedHashMap<String,Object>();summary.put("run_id",System.getProperty("prime_ants.runId"));summary.put("scenario","appearance-t24");summary.put("prior_ticks",prior);summary.put("total_tick_bound",12000);summary.put("attempt_tick_bound",1800);
        Throwable failure=null;
        try{
            System.setProperty("prime_ants.capturePrefix",base+"-nest");System.setProperty("prime_ants.appearanceSpecimen",System.getProperty("prime_ants.appearanceFirstSpecimen","interior"));System.setProperty("prime_ants.appearanceAttemptSpent","0");
            Path nest=dir.resolve(base+"-nest-appearance.json");attempted.add(nest);new AppearanceScenario().run(c);
            var first=com.google.gson.JsonParser.parseString(Files.readString(nest,StandardCharsets.UTF_8)).getAsJsonObject();int spent=first.get("additional_ticks").getAsInt();
            System.setProperty("prime_ants.capturePrefix",base+"-queen");System.setProperty("prime_ants.appearanceSpecimen","queen");
            System.setProperty("prime_ants.appearanceSource",Objects.requireNonNull(System.getProperty("prime_ants.appearanceSecondSource")));
            System.setProperty("prime_ants.appearanceRoot",Objects.requireNonNull(System.getProperty("prime_ants.appearanceSecondRoot")));
            System.setProperty("prime_ants.appearancePriorTicks",Integer.toString(prior+spent));System.setProperty("prime_ants.appearanceAttemptSpent",Integer.toString(spent));
            attempted.add(dir.resolve(base+"-queen-appearance.json"));new AppearanceScenario().run(c);
        }catch(Throwable e){failure=e;summary.put("failure",e.toString());}
        finally{
            for(String key:keys){String value=original.get(key);if(value==null)System.clearProperty(key);else System.setProperty(key,value);}
            try{
                int ticks=0;boolean known=true,closed=true,stopped=true;
                for(Path p:attempted){
                    if(!Files.exists(p)){known=false;closed=false;stopped=false;continue;}
                    var record=com.google.gson.JsonParser.parseString(Files.readString(p,StandardCharsets.UTF_8)).getAsJsonObject();segments.add(record);
                    for(var frame:record.getAsJsonArray("captures"))frames.add(frame);
                    if(record.has("additional_ticks"))ticks+=record.get("additional_ticks").getAsInt();else known=false;
                    closed&=record.has("normal_close")&&record.get("normal_close").getAsBoolean();
                    stopped&=record.has("normal_server_stop")&&record.get("normal_server_stop").getAsBoolean();
                }
                summary.put("segments",segments);summary.put("captures",frames);summary.put("normal_close",closed);summary.put("normal_server_stop",stopped);
                if(known){summary.put("additional_ticks",ticks);summary.put("accumulated_ticks",prior+ticks);}
                summary.put("status",failure==null&&closed&&known&&ticks<=1800&&prior+ticks<=12000?"closed":"failed");
                Files.writeString(dir.resolve(base+"-appearance.json"),new GsonBuilder().setPrettyPrinting().create().toJson(summary),StandardCharsets.UTF_8);
            }catch(Exception e){throw new RuntimeException(e);}
        }
        if(failure!=null)throw new RuntimeException(failure);
        require("closed".equals(summary.get("status")),"Combined observation bound/closure");
    }
}
