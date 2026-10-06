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
    static UUID cameraSubject;
    static View fixedView;
    static double cameraSide, cameraForward, cameraHeight, targetHeight;
    public record View(Vec3 eye, float yaw, float pitch) {}
    public static View view() {
        if(fixedView!=null)return fixedView;
        if(cameraSubject==null)return null;
        var client=Minecraft.getInstance();var ant=clientAnt(client,cameraSubject);if(ant==null)return null;
        double yaw=Math.toRadians(ant.yBodyRot);var forward=new Vec3(-Math.sin(yaw),0,Math.cos(yaw));var side=new Vec3(forward.z,0,-forward.x);
        var eye=ant.position().add(forward.scale(cameraForward)).add(side.scale(cameraSide)).add(0,cameraHeight,0);
        var delta=ant.position().add(0,targetHeight,0).subtract(eye);
        return new View(eye,(float)Math.toDegrees(Math.atan2(-delta.x,delta.z)),(float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.horizontalDistanceSqr()))));
    }
    final Path dir=Path.of(System.getProperty("prime_ants.captureDir"));
    final String prefix=System.getProperty("prime_ants.capturePrefix");
    final Map<String,Object> evidence=new LinkedHashMap<>();
    private final List<Object> captures=new ArrayList<>();
    private net.minecraft.server.MinecraftServer owned;
    private int start;
    private final int prior=Integer.getInteger("prime_ants.appearancePriorTicks",0);
    private final int attemptSpent=Integer.getInteger("prime_ants.appearanceAttemptSpent",0);
    void save(){try{Files.createDirectories(dir);Files.writeString(dir.resolve(prefix+"-appearance.json"),new GsonBuilder().setPrettyPrinting().create().toJson(evidence),StandardCharsets.UTF_8);}catch(Exception e){throw new RuntimeException(e);}}
    static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private final boolean t26="appearance-t26".equals(System.getProperty("prime_ants.clientScenario"));
    private final int totalBound=Integer.getInteger("prime_ants.appearanceTotalBound",12000);
    private final int attemptBound=Integer.getInteger("prime_ants.appearanceAttemptBound",1800);
    private boolean measuredSurvival;
    public void survival(boolean value){measuredSurvival=value;cameraSubject=null;fixedView=null;}
    public void run(ClientGameTestContext c){
        require(prior>=0&&prior<totalBound,"Declared appearance observation allowance");
        evidence.put("run_id",System.getProperty("prime_ants.runId"));evidence.put("scenario",t26?"appearance-t26":"appearance-t25");evidence.put("status","opening");
        evidence.put("prior_ticks",prior);evidence.put("total_tick_bound",totalBound);evidence.put("attempt_tick_bound",attemptBound-attemptSpent);evidence.put("attempt_ticks_already_spent",attemptSpent);evidence.put("captures",captures);
        evidence.put("ant_position_edits",0);evidence.put("ai_pauses",0);evidence.put("forced_poses",0);evidence.put("terrain_edits",0);evidence.put("supplied_ants",0);evidence.put("supplied_food",0);
        evidence.put("render_distance",8);evidence.put("simulation_distance",8);evidence.put("native_frame",List.of(1600,1000));
        evidence.put("camera_policy","Living spectator on verified dry FULL support; separate observer lens follows natural body heading. No actor or animation writes.");save();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(s->{if(owned==null){owned=s;start=s.getTickCount();save();}});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.START_SERVER_TICK.register(s->{if(s==owned&&!measuredSurvival)for(var p:s.getPlayerList().getPlayers())if(p.isAlive())p.setGameMode(GameType.SPECTATOR);});
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
            String specimen=System.getProperty("prime_ants.appearanceSpecimen");
            if(t26&&"brood-final".equals(specimen)){
                new T26Observations(this).broodFinal(c,w);
            } else if(t26&&"brood".equals(specimen)){
                new T26Observations(this).brood(c,w,stand);
            } else if(t26&&"transition".equals(specimen)){
                captureWhenClear(c,w,WORKER,"worker-ordinary",true,70,2.2,.5,1.25,.24);
                captureWhenClear(c,w,WORKER,"worker-detail",true,50,1.65,.65,1.2,.24);
                new T26Observations(this).transition(c,w,WORKER);
            } else if("clearance-reproducer".equals(specimen)) {
                captureWhenClear(c,w,WORKER,"clear-before",true,70,2.2,.5,1.25,.24);
                var previous=capturedView();
                evidence.put("reproducer_previously_clear_following_lens",List.of(previous.eye.x,previous.eye.y,previous.eye.z));
                // Move only the observer lens across the native ground boundary between readiness and capture.
                fixedView=c.computeOnClient(client->{
                    for(int dy=1;dy<=4;dy++) {var pos=BlockPos.containing(previous.eye.add(0,-dy,0));if(client.level.getBlockState(pos).isSolidRender())return new View(Vec3.atCenterOf(pos),previous.yaw,previous.pitch);}
                    throw new AssertionError("Native solid below clear lens required");
                });
                require(!capture(c,w,WORKER,"crossed-solid-rejected"),"A clear-before following lens entering solid must reject at capture");
                evidence.put("reproducer_policy","Observer-only downward crossing into existing solid after readiness, before screenshot extraction; no terrain or actor edit");
                fixedView=previous;
                require(capture(c,w,WORKER,"fixed-lens-recovery"),"Verified fixed lens recovers valid capture");
            } else if("queen".equals(specimen)) {
                captureWhenClear(c,w,QUEEN,"queen-daylight",true,55,3.1,1.1,1.8,.35);
            } else {
                captureWhenClear(c,w,WORKER,"worker-ordinary",true,70,2.2,.5,1.25,.24);
                captureWhenClear(c,w,WORKER,"worker-detail",true,50,1.65,.65,1.2,.24);
                fixedView=capturedView();
                for(int frame=1;frame<=4;frame++){wait(c,w,2);capture(c,w,WORKER,"worker-walk-"+frame);}
                observeFood(c,w,stand);
            }
            evidence.put("status","captured");save();
        }catch(Throwable e){evidence.put("failure",e.toString());evidence.put("status","failed");save();throw new RuntimeException(e);}
        finally{cameraSubject=null;fixedView=null;}
        require(closed!=null&&Files.exists(closed.resolve("level.dat"))&&Boolean.TRUE.equals(evidence.get("normal_server_stop")),"Normal saved closure");
        require(((Number)evidence.get("accumulated_ticks")).intValue()<=totalBound,"Appearance total bound");evidence.put("normal_close",true);evidence.put("status","closed");save();
    }
    private boolean tryFoodFrame(ClientGameTestContext c,TestSingleplayerContext w,String name) {
        var candidates=w.getServer().computeOnServer(server->{
            var list=new ArrayList<Map<String,Object>>();
            for(var e:w.getConnection().getServerLevel().getAllEntities())if(e instanceof LasiusNigerEntity a&&a.form()==AntForm.WORKER&&a.isAlive()&&!a.isCallow()&&QUEEN.equals(a.queenId())&&dev.primeants.worker.WorkerTasks.food(a.getMainHandItem())&&a.getY()>=66.9) {
                var row=new LinkedHashMap<String,Object>();row.put("uuid",a.getUUID().toString());row.put("queen",a.queenId().toString());row.put("brood",a.broodId().toString());row.put("item",net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(a.getMainHandItem().getItem()).toString());row.put("count",a.getMainHandItem().getCount());row.put("phase",a.workerTasks().phase().toString());
                if(a.workerTasks().flowerSource()!=null)row.put("flower_source",List.of(a.workerTasks().flowerSource().getX(),a.workerTasks().flowerSource().getY(),a.workerTasks().flowerSource().getZ()));
                if(a.workerTasks().sourceId()!=null)row.put("source_item_uuid",a.workerTasks().sourceId().toString());
                list.add(row);
            }
            list.sort(Comparator.comparingInt(row->String.valueOf(row.get("item")).startsWith("prime_ants:")?0:1));
            return list;
        });
        for(var candidate:candidates) {
            UUID id=UUID.fromString((String)candidate.get("uuid"));
            fixedView=null;cameraSubject=id;cameraForward=1.0;cameraHeight=1.0;targetHeight=.24;
            c.runOnClient(client->client.options.fov().set(45));
            for(double side:new double[]{1.6,-1.6}) {
                cameraSide=side;
                boolean ready=c.computeOnClient(client->{var ant=clientAnt(client,id);return ant!=null&&ant.isAlive()&&!ant.isCallow()&&dev.primeants.worker.WorkerTasks.food(ant.getMainHandItem())&&clear(client,ant,view().eye);});
                if(!ready)continue;
                evidence.put("food_candidate_server",candidate);save();
                boolean valid=capture(c,w,id,name+"-"+captures.size());
                var frame=(Map<?,?>)captures.getLast();
                boolean equipment=((List<?>)frame.get("rendered_ants")).stream().anyMatch(r->{var row=(Map<?,?>)r;return candidate.get("item").equals(row.get("carried_item"))&&Boolean.TRUE.equals(row.get("carried_soil_rendered"))&&((Number)row.get("client_carried_soil_units")).intValue()>0;});
                if(valid&&equipment){evidence.put("food_carrying_frame",frame.get("name"));evidence.put("food_actor",candidate);save();return true;}
                // One rejected frame is retained; the opposite lens is a changed targeted recovery.
            }
        }
        return false;
    }
    private void observeFood(ClientGameTestContext c,TestSingleplayerContext w,BlockPos retreat) {
        evidence.put("natural_food_wait_bound",600);evidence.put("food_source_policy","Passive naturally harvested nectar/prey preferred; one declared inventory apple fallback only");save();
        for(int tick=0;tick<600;tick++) {if(tryFoodFrame(c,w,"worker-food-natural")){evidence.put("food_source","natural/existing production cargo; no T25 food supplied");return;}wait(c,w,1);}
        evidence.put("natural_food_wait_exhausted",true);save();
        if(!Boolean.parseBoolean(System.getProperty("prime_ants.appearanceAppleAllowed","false"))){evidence.put("food_carrying_incomplete","One T25 player apple already used; no renewed supply");save();return;}
        // Inventory-to-world feeding, with ordinary player drop behavior and immediate retreat.
        var dropStand=w.getServer().computeOnServer(server->{
            var level=w.getConnection().getServerLevel();
            for(int r=1;r<=4;r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){var feet=ObserverSafety.surface(level,168+dx,149+dz);if(ObserverSafety.problem(level,w.getConnection().getServerPlayer(),feet)==null)return feet;}
            return null;
        });
        require(dropStand!=null,"Safe player feeding stand");
        w.getServer().runOnServer(server->{var player=w.getConnection().getServerPlayer();require(player.getInventory().countItem(net.minecraft.world.item.Items.APPLE)==0,"No existing apple confound");player.teleportTo(w.getConnection().getServerLevel(),dropStand.getX()+.5,dropStand.getY(),dropStand.getZ()+.5,Set.of(),0,45,true);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.APPLE));});
        w.getConnection().waitForClientboundPackets();
        var trace=new LinkedHashMap<String,Object>();evidence.put("player_drop",trace);evidence.put("supplied_food",1);evidence.put("food_source","one explicitly supplied player-inventory apple; proves carrying/player feeding only");
        var origin=w.getServer().computeOnServer(server->w.getConnection().getServerPlayer().position());
        var away=w.getServer().computeOnServer(server->{var level=w.getConnection().getServerLevel();for(int r=5;r<=8;r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){var feet=ObserverSafety.surface(level,168+dx,149+dz);if(Vec3.atBottomCenterOf(feet).distanceTo(origin)>=4&&ObserverSafety.problem(level,w.getConnection().getServerPlayer(),feet)==null)return feet;}return null;});
        require(away!=null,"Safe immediate four-block observer retreat");
        w.getServer().runOnServer(server->{var player=w.getConnection().getServerPlayer();require(player.getMainHandItem().is(net.minecraft.world.item.Items.APPLE),"Declared inventory apple");player.setYRot((float)Math.toDegrees(Math.atan2(-(168.5-player.getX()),149.5-player.getZ())));player.setXRot(45);var item=player.drop(player.getMainHandItem().split(1),true,net.minecraft.util.Prediction.SERVER_ONLY);require(item!=null,"Physical ordinary player drop");trace.put("world_item_uuid",item.getUUID().toString());trace.put("world_item_position",List.of(item.getX(),item.getY(),item.getZ()));trace.put("inventory_loss",1);require(player.getInventory().countItem(net.minecraft.world.item.Items.APPLE)==0,"One apple leaves inventory");player.teleportTo(w.getConnection().getServerLevel(),away.getX()+.5,away.getY(),away.getZ()+.5,Set.of(),0,20,true);});
        trace.put("origin",List.of(origin.x,origin.y,origin.z));trace.put("retreat",List.of(away.getX(),away.getY(),away.getZ()));trace.put("retreat_distance",Vec3.atBottomCenterOf(away).distanceTo(origin));trace.put("policy","ServerPlayer.drop from actual inventory, spectator observer retreat in same server action; no ant state/target writes");save();
        for(int tick=0;tick<600;tick++){if(tryFoodFrame(c,w,"worker-food-player-drop"))return;wait(c,w,1);}
        evidence.put("food_carrying_incomplete","No clear equipment-synchronized food frame within declared waits");save();
    }
    private TestSingleplayerContext open(ClientGameTestContext c)throws Exception{
        String source=System.getProperty("prime_ants.appearanceSource"),selected=System.getProperty("prime_ants.appearanceRoot");require(source!=null&&selected!=null,"Explicit source and archive root");
        var target=Path.of("saves",prefix).toAbsolutePath();require(!Files.exists(target),"Fresh immutable source copy");Files.createDirectories(target);
        try(var z=new ZipFile(source)){require(z.getEntry(selected+"/level.dat")!=null,"Actual source root");require(z.stream().noneMatch(e->e.getName().startsWith(selected+"/dimensions/prime_ants_test/")),"Ordinary client world only");
            for(var entry:z.stream().toList())if(entry.getName().startsWith(selected+"/")){var p=target.resolve(entry.getName().substring(selected.length()+1)).normalize();require(p.startsWith(target),"Safe archive path");if(entry.isDirectory())Files.createDirectories(p);else{Files.createDirectories(p.getParent());try(var in=z.getInputStream(entry)){Files.copy(in,p);}}}}
        evidence.put("source_archive",source);evidence.put("archive_root",selected);save();return new TestWorldSaveImpl(c,target).open();
    }
    void wait(ClientGameTestContext c,TestSingleplayerContext w,int ticks){int before=w.getServer().computeOnServer(s->s.getTickCount());require(attemptSpent+before-start+ticks<attemptBound-100&&prior+before-start+ticks<totalBound-100,"Appearance wait bound");c.waitTicks(ticks);require(w.getServer().computeOnServer(s->s.getTickCount()>before&&w.getConnection().getServerPlayer().isAlive()),"Live observer and advancing ticks");}
    void captureWhenClear(ClientGameTestContext c,TestSingleplayerContext w,UUID id,String name,boolean exterior,int fov,double side,double forward,double height,double target){
        fixedView=null;c.runOnClient(client->client.options.fov().set(fov));cameraSide=side;cameraForward=forward;cameraHeight=height;targetHeight=target;cameraSubject=id;
        boolean ready=false;
        for(int t=0;t<1000;t+=5){ready=c.computeOnClient(client->{var a=clientAnt(client,id);if(a==null||!a.isAlive()||a.isCallow()||exterior&&a.getY()<66.9)return false;var v=view();return clear(client,a,v.eye());});if(ready)break;wait(c,w,5);}
        require(ready,"Declared subject unavailable/unobscured view missing: "+name);
        require(capture(c,w,id,name),"Render-time clearance/subject/frame rejection: "+name);
    }
    public static boolean clear(Minecraft client,LasiusNigerEntity a,Vec3 eye){
        for(double dx:new double[]{-.075,.075})for(double dy:new double[]{-.075,.075})for(double dz:new double[]{-.075,.075}) {
            var lens=BlockPos.containing(eye.add(dx,dy,dz));
            if(!client.level.hasChunkAt(lens)||!client.level.getWorldBorder().isWithinBounds(lens)||!client.level.getBlockState(lens).isAir()||!client.level.getFluidState(lens).isEmpty())return false;
        }
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
    boolean capture(ClientGameTestContext c,TestSingleplayerContext w,UUID id,String name){
        c.runOnClient(client->AntRenderRecorder.arm(id));
        var png=c.takeScreenshot(TestScreenshotOptions.of(prefix+"-"+name).disableCounterPrefix().withDeltaTicks(1).withDestinationDir(dir));
        var bound=c.computeOnClient(client->AntRenderRecorder.finishCapture());
        var frame=new LinkedHashMap<String,Object>();frame.put("name",name);frame.put("image",png.toString());frame.put("subject_uuid",id.toString());
        frame.put("recipe",Map.of("side",cameraSide,"forward",cameraForward,"height",cameraHeight,"target_height",targetHeight));
        frame.put("camera",bound);
        var rendered=(List<?>)bound.get("rendered_ants");frame.put("rendered_ants",rendered);
        boolean valid=Boolean.TRUE.equals(bound.get("clearance"))&&Boolean.TRUE.equals(bound.get("living_subject"))&&Boolean.TRUE.equals(bound.get("living_observer"))&&!rendered.isEmpty()&&bound.get("viewport").equals(List.of(1600,1000));
        frame.put("capture_valid",valid);frame.put("acceptance",valid?"pending-manual-review":"rejected-at-capture");
        captures.add(frame);save();return valid;
    }
    @SuppressWarnings("unchecked")
    private View capturedView() {
        var frame=(Map<String,Object>)captures.getLast();
        var camera=(Map<String,Object>)frame.get("camera");
        var eye=(List<Number>)camera.get("eye");
        return new View(new Vec3(eye.get(0).doubleValue(),eye.get(1).doubleValue(),eye.get(2).doubleValue()),((Number)camera.get("yaw")).floatValue(),((Number)camera.get("pitch")).floatValue());
    }
    public static LasiusNigerEntity clientAnt(Minecraft client,UUID id){if(client.level!=null)for(var e:client.level.entitiesForRendering())if(e instanceof LasiusNigerEntity a&&a.getUUID().equals(id))return a;return null;}

    /** Two ordinary save observations in one process; every world closes separately.
     * Counts add across copies and the shared 1,800-tick attempt limit remains. */
    public static void comparisons(ClientGameTestContext c){
        int totalBound=Integer.getInteger("prime_ants.appearanceTotalBound",12000),attemptBound=Integer.getInteger("prime_ants.appearanceAttemptBound",1800);
        String base=System.getProperty("prime_ants.capturePrefix");
        Path dir=Path.of(System.getProperty("prime_ants.captureDir"));
        int prior=Integer.getInteger("prime_ants.appearancePriorTicks",0);
        String[] keys={"prime_ants.capturePrefix","prime_ants.appearanceSource","prime_ants.appearanceRoot","prime_ants.appearanceSpecimen","prime_ants.appearancePriorTicks","prime_ants.appearanceAttemptSpent"};
        var original=new HashMap<String,String>();for(String key:keys)original.put(key,System.getProperty(key));
        var attempted=new ArrayList<Path>();var segments=new ArrayList<Object>();var frames=new ArrayList<Object>();
        var summary=new LinkedHashMap<String,Object>();summary.put("run_id",System.getProperty("prime_ants.runId"));summary.put("scenario",System.getProperty("prime_ants.clientScenario"));summary.put("prior_ticks",prior);summary.put("total_tick_bound",totalBound);summary.put("attempt_tick_bound",attemptBound);
        Throwable failure=null;
        try{
            System.setProperty("prime_ants.capturePrefix",base+"-nest");System.setProperty("prime_ants.appearanceSpecimen",System.getProperty("prime_ants.appearanceFirstSpecimen","interior"));System.setProperty("prime_ants.appearanceAttemptSpent","0");
            Path nest=dir.resolve(base+"-nest-appearance.json");attempted.add(nest);new AppearanceScenario().run(c);
            var first=com.google.gson.JsonParser.parseString(Files.readString(nest,StandardCharsets.UTF_8)).getAsJsonObject();int spent=first.get("additional_ticks").getAsInt();
            if("appearance-t26".equals(System.getProperty("prime_ants.clientScenario"))&&"brood-final".equals(System.getProperty("prime_ants.appearanceFirstSpecimen"))){
                System.setProperty("prime_ants.capturePrefix",base+"-worker");System.setProperty("prime_ants.appearanceSpecimen","transition");
                var queenSource=Path.of(System.getProperty("prime_ants.appearanceSecondSource"));
                System.setProperty("prime_ants.appearanceSource",queenSource.getParent().getParent().resolve("T23/t23-player-a5-world.zip").toString());System.setProperty("prime_ants.appearanceRoot","t23-player-a5");
                System.setProperty("prime_ants.appearancePriorTicks",Integer.toString(prior+spent));System.setProperty("prime_ants.appearanceAttemptSpent",Integer.toString(spent));
                Path worker=dir.resolve(base+"-worker-appearance.json");attempted.add(worker);new AppearanceScenario().run(c);
                spent+=com.google.gson.JsonParser.parseString(Files.readString(worker,StandardCharsets.UTF_8)).getAsJsonObject().get("additional_ticks").getAsInt();
            }
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
                summary.put("status",failure==null&&closed&&known&&ticks<=attemptBound&&prior+ticks<=totalBound?"closed":"failed");
                Files.writeString(dir.resolve(base+"-appearance.json"),new GsonBuilder().setPrettyPrinting().create().toJson(summary),StandardCharsets.UTF_8);
            }catch(Exception e){throw new RuntimeException(e);}
        }
        if(failure!=null)throw new RuntimeException(failure);
        require("closed".equals(summary.get("status")),"Combined observation bound/closure");
    }
}
