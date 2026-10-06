package dev.primeants.gametest;

import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.phys.*;

/** Client-created vanilla overworld. No imported dimensions or experimental chunk allowance. */
public final class FreshSurvivalScenario {
    public static final long SEED=2026100501L;
    public static final int DISCOVERY_RADIUS=128, ADDITIONAL_TICKS=24000;
    private final int priorScenarioTicks=Integer.getInteger("prime_ants.priorScenarioTicks",0);
    private final Map<String,Object> evidence=new LinkedHashMap<>();
    private final Path dir=Path.of(System.getProperty("prime_ants.captureDir"));
    private final String prefix=System.getProperty("prime_ants.capturePrefix");
    private net.minecraft.server.MinecraftServer ownedServer;
    private void checkpoint(){try{Files.createDirectories(dir);Files.writeString(dir.resolve(prefix+"-fresh.json"),new GsonBuilder().setPrettyPrinting().create().toJson(evidence));}catch(Exception e){throw new RuntimeException(e);}}
    public void run(ClientGameTestContext c){
        evidence.put("run_id",System.getProperty("prime_ants.runId"));evidence.put("scenario","fresh-survival");evidence.put("seed",SEED);
        evidence.put("status","creating");evidence.put("natural_placement","production default");evidence.put("render_distance",8);evidence.put("simulation_distance",8);checkpoint();
        evidence.put("founding_multiplier",QueenFounding.multiplier());evidence.put("brood_multiplier",dev.primeants.brood.BroodPile.multiplier());
        evidence.put("observer_setup","Survival with observer commands enabled; fixed 1280x720 native window at startup, HUD hidden, daytime/camera/player setup moves disclosed. Combat uses ordinary survival inputs and physics.");
        evidence.put("ant_position_edits",0);evidence.put("supplied_ants",0);evidence.put("supplied_food",0);evidence.put("terrain_edits",0);evidence.put("ai_pauses",0);
        evidence.put("loading_only",Boolean.getBoolean("prime_ants.freshLoadingOnly"));
        require(priorScenarioTicks>=0&&priorScenarioTicks<ADDITIONAL_TICKS,"Declared prior natural scenario ticks");evidence.put("prior_scenario_ticks",priorScenarioTicks);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(s->{if(s==ownedServer&&evidence.containsKey("additional_start_tick")){evidence.put("additional_ticks_through_close",s.getTickCount()-((Number)evidence.get("additional_start_tick")).intValue());checkpoint();}});
        c.runOnClient(client->{client.options.renderDistance().set(8);client.options.simulationDistance().set(8);client.getWindow().setWindowed(1280,720);});
        Path save=null;
        try(var world=c.worldBuilder().setUseConsistentSettings(false).adjustSettings(s->{s.setSeed(Long.toString(SEED));s.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);s.setAllowCommands(true);}).create()){
            save=world.getWorldSave().getSaveDirectory();evidence.put("save",save.toString());evidence.put("status","joined");checkpoint();
            ownedServer=world.getServer().computeOnServer(s->s);
            // Fabric's download predicate requires every corner of a square.
            // Vanilla's radius-eight tracking view excludes those corners. Wait
            // for actual rendering, then require compiled terrain at the body.
            world.getConnection().waitForChunksRender(false);c.waitForScreen(null);
            c.waitFor(client->client.levelRenderer.isSectionCompiledAndVisible(client.player.blockPosition().below(),0));
            var generation=world.getServer().computeOnServer(s->{var l=world.getConnection().getServerLevel();require(l.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)&&l.getSeed()==SEED&&NaturalPlacement.productionEnabled(),"Ordinary overworld, declared seed and default-enabled production placement");return Map.of("dimension",l.dimension().identifier().toString(),"generator",l.getChunkSource().getGenerator().getClass().getName(),"seed",l.getSeed());});evidence.put("generation",generation);
            var before=world.getServer().computeOnServer(s->world.getConnection().getServerPlayer().position());
            int serverBefore=world.getServer().computeOnServer(s->s.getTickCount());
            int clientBefore=c.computeOnClient(client->client.player.tickCount);
            c.getInput().holdKeyFor(o->o.keyUp,20);c.waitTicks(2);
            var after=world.getServer().computeOnServer(s->world.getConnection().getServerPlayer().position());
            int serverAfter=world.getServer().computeOnServer(s->s.getTickCount());
            int clientAfter=c.computeOnClient(client->client.player.tickCount);
            require(serverAfter>serverBefore&&clientAfter>clientBefore,"Both integrated server and client ticks advance");
            require(before.distanceToSqr(after)>.01,"Ordinary forward input moves survival body");
            require(world.getServer().computeOnServer(s->!world.getConnection().getServerPlayer().isCreative()&&!world.getConnection().getServerPlayer().isSpectator()),"Normal survival mode");
            evidence.put("server_ticks",List.of(serverBefore,serverAfter));evidence.put("client_ticks",List.of(clientBefore,clientAfter));
            evidence.put("movement",Map.of("before",List.of(before.x,before.y,before.z),"after",List.of(after.x,after.y,after.z),"input","keyUp held 20 client ticks"));
            c.runOnClient(client->{if(!client.gui.hud.isHidden())client.gui.hud.toggle();});c.waitTicks(20);
            Path png=c.takeScreenshot(TestScreenshotOptions.of(prefix+"-terrain").disableCounterPrefix().withDestinationDir(dir));
            evidence.put("terrain_image",png.toString());evidence.put("status","terrain_rendered_movement_checked");checkpoint();
            if(!Boolean.getBoolean("prime_ants.freshLoadingOnly"))natural(c,world);
        }catch(Throwable e){evidence.put("status","failed");evidence.put("failure",e.toString());checkpoint();throw e;}
        require(save!=null&&Files.exists(save.resolve("level.dat")),"Normal close leaves populated level.dat");
        if(evidence.containsKey("additional_start_tick"))require(evidence.containsKey("additional_ticks_through_close")&&priorScenarioTicks+((Number)evidence.get("additional_ticks_through_close")).intValue()<=ADDITIONAL_TICKS,"Natural scenario and normal closure including prior attempt stay within additional server tick bound");
        evidence.put("status","success");evidence.put("normal_close",true);checkpoint();
    }
    private int ticks(TestSingleplayerContext w){return w.getServer().computeOnServer(s->s.getTickCount());}
    private UUID queen(TestSingleplayerContext w,BlockPos spawn){return w.getServer().computeOnServer(s->{
        var l=w.getConnection().getServerLevel();var candidates=new ArrayList<LasiusNigerEntity>();
        for(var entry:NaturalPlacement.get(l).decisions().entrySet()){
            var d=entry.getValue().getAsJsonObject();if(!"PLACED".equals(d.get("status").getAsString()))continue;
            if(l.getEntity(UUID.fromString(d.get("queen").getAsString())) instanceof LasiusNigerEntity q&&q.isAlive()
                &&Math.pow(q.getX()-spawn.getX(),2)+Math.pow(q.getZ()-spawn.getZ(),2)<=DISCOVERY_RADIUS*DISCOVERY_RADIUS)candidates.add(q);
        }
        return candidates.stream().min(Comparator.comparingDouble(q->q.position().distanceToSqr(Vec3.atCenterOf(spawn)))).map(LasiusNigerEntity::getUUID).orElse(null);
    });}
    private void observerMove(TestSingleplayerContext w,BlockPos feet){
        w.getServer().runCommand(String.format(Locale.ROOT,"tp @p %.3f %.3f %.3f",feet.getX()+.5,(double)feet.getY(),feet.getZ()+.5));
        evidence.put("observer_setup_moves",((Number)evidence.getOrDefault("observer_setup_moves",0)).intValue()+1);
    }
    private BlockPos stand(TestSingleplayerContext w,Vec3 focus,int minimum,int maximum){return w.getServer().computeOnServer(s->{
        var l=w.getConnection().getServerLevel();var options=new ArrayList<BlockPos>();
        for(int x=-maximum;x<=maximum;x++)for(int z=-maximum;z<=maximum;z++){
            if(x*x+z*z<minimum*minimum||x*x+z*z>maximum*maximum)continue;int xx=(int)Math.floor(focus.x)+x,zz=(int)Math.floor(focus.z)+z;
            var top=new BlockPos(xx,l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,xx,zz),zz);
            var v=Vec3.atBottomCenterOf(top);if(NestPlan.walkable(l,top)&&l.noCollision(w.getConnection().getServerPlayer(),new AABB(v.x-.3,v.y,v.z-.3,v.x+.3,v.y+1.8,v.z+.3)))options.add(top);
        }
        return options.stream().min(Comparator.comparingDouble(p->Vec3.atBottomCenterOf(p).distanceToSqr(focus))).orElse(null);
    });}
    private LasiusNigerEntity clientAnt(net.minecraft.client.Minecraft client,UUID id){
        if(client.level!=null)for(var e:client.level.entitiesForRendering())if(e instanceof LasiusNigerEntity a&&a.getUUID().equals(id))return a;return null;
    }
    private Path capture(ClientGameTestContext c,TestSingleplayerContext w,UUID colony,String name){
        var ids=w.getServer().computeOnServer(s->{var set=new HashSet<UUID>();set.add(colony);ColonyMembers.get(w.getConnection().getServerLevel()).members(colony).forEach(m->set.add(m.worker()));return set;});
        c.runOnClient(client->AntRenderRecorder.start(ids));
        var png=c.takeScreenshot(TestScreenshotOptions.of(prefix+"-"+name).disableCounterPrefix().withDeltaTicks(1).withDestinationDir(dir));
        var rendered=c.computeOnClient(client->AntRenderRecorder.finish());require(!rendered.isEmpty(),"Actual colony ants rendered in stable native frame");
        evidence.put(name+"_image",png.toString());evidence.put(name+"_rendered_ants",rendered);checkpoint();return png;
    }
    private void natural(ClientGameTestContext c,TestSingleplayerContext w){
        int start=ticks(w);evidence.put("additional_start_tick",start);evidence.put("additional_tick_bound",ADDITIONAL_TICKS-priorScenarioTicks);evidence.put("discovery_radius",DISCOVERY_RADIUS);
        var spawn=w.getServer().computeOnServer(s->w.getConnection().getServerLevel().getRespawnData().pos());evidence.put("spawn",List.of(spawn.getX(),spawn.getY(),spawn.getZ()));
        UUID id=null;int[][] offsets={{0,0},{96,0},{0,96},{-96,0},{0,-96}};
        for(var offset:offsets){
            if(ticks(w)-start>=4000)break;
            if(offset[0]!=0||offset[1]!=0){var feet=w.getServer().computeOnServer(s->{var l=w.getConnection().getServerLevel();int x=spawn.getX()+offset[0],z=spawn.getZ()+offset[1];return new BlockPos(x,l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,x,z),z);});observerMove(w,feet);}
            for(int i=0;i<50&&ticks(w)-start<4000;i++){id=queen(w,spawn);if(id!=null)break;c.waitTicks(10);}if(id!=null)break;
        }
        if(id==null){evidence.put("natural_result","bounded_absence");evidence.put("placement_decisions",w.getServer().computeOnServer(s->NaturalPlacement.get(w.getConnection().getServerLevel()).decisions()));finishNatural(w,start);return;}
        final UUID colony=id;evidence.put("colony_uuid",id.toString());evidence.put("colony_origin","Production automatic placement in this client-generated ordinary overworld");
        var pos=w.getServer().computeOnServer(s->w.getConnection().getServerLevel().getEntity(colony).position());var observer=stand(w,pos,5,8);if(observer!=null)observerMove(w,observer);
        evidence.put("status","waiting_natural_founding_and_maturation");checkpoint();
        UUID worker=null;
        while(ticks(w)-start<20000){
            worker=w.getServer().computeOnServer(s->{var l=w.getConnection().getServerLevel();if(!(l.getEntity(colony) instanceof LasiusNigerEntity q)||!q.isAlive()||q.founding().plan()==null||q.founding().lifecycle()!=QueenFounding.Lifecycle.OPEN)return null;
                return ColonyMembers.get(l).members(colony).stream().map(m->l.getEntity(m.worker())).filter(e->e instanceof LasiusNigerEntity).map(e->(LasiusNigerEntity)e)
                    .filter(a->a.isAlive()&&!a.isCallow()&&a.getY()>=q.founding().plan().entrance().getY()+.8).map(LasiusNigerEntity::getUUID).findFirst().orElse(null);});
            if(worker!=null)break;c.waitTicks(10);
        }
        if(worker==null){evidence.put("natural_result","incomplete_maturation");finishNatural(w,start);return;}
        final UUID subject=worker;evidence.put("mature_worker_uuid",subject.toString());
        var plan=w.getServer().computeOnServer(s->((LasiusNigerEntity)w.getConnection().getServerLevel().getEntity(colony)).founding().plan());
        var viewing=stand(w,Vec3.atBottomCenterOf(plan.entrance()),3,5);if(viewing!=null)observerMove(w,viewing);
        w.getServer().runCommand("time set day");evidence.put("daytime_observer_command","time set day before watching/capture; loaded biological clocks remain active");
        w.getConnection().waitForChunksRender(false);c.waitTicks(100);
        require(w.getServer().computeOnServer(s->ColonyAlarm.get(w.getConnection().getServerLevel()).alarm(colony)==null),"Ordinary watching produces no hostility");
        evidence.put("unprovoked_watch_ticks",100);
        boolean entranceWindow=false;
        for(int i=0;i<800&&ticks(w)-start<21000;i++){
            boolean near=w.getServer().computeOnServer(s->{var a=w.getConnection().getServerLevel().getEntity(subject);return a!=null&&a.getY()>=plan.entrance().getY()+.8&&a.position().distanceToSqr(Vec3.atBottomCenterOf(plan.entrance()))<=64;});
            entranceWindow=near&&c.computeOnClient(client->{var a=clientAnt(client,subject);return a!=null&&client.level.clip(new net.minecraft.world.level.ClipContext(client.player.getEyePosition(),a.position().add(0,.25,0),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.ANY,client.player)).getType()==HitResult.Type.MISS;});
            if(entranceWindow)break;c.waitTick();
        }
        require(entranceWindow,"Bounded real entrance/worker visibility window without ant positioning");
        c.runOnClient(client->{var a=clientAnt(client,subject);if(a!=null)client.player.lookAt(EntityAnchorArgument.Anchor.EYES,a.position().add(0,.3,0));else client.player.lookAt(EntityAnchorArgument.Anchor.EYES,Vec3.atCenterOf(plan.entrance()));});c.waitTicks(4);
        capture(c,w,colony,"entrance");
        float healthBefore=w.getServer().computeOnServer(s->w.getConnection().getServerPlayer().getHealth());evidence.put("health_before_provocation",healthBefore);
        boolean attacked=false;
        try{for(int i=0;i<800&&ticks(w)-start<21800;i++){
            boolean aimed=c.computeOnClient(client->{var a=clientAnt(client,subject);if(a==null)return false;client.player.lookAt(EntityAnchorArgument.Anchor.EYES,a.position().add(0,.25,0));return client.hitResult instanceof EntityHitResult h&&h.getEntity()==a;});
            if(aimed){c.getInput().releaseKey(o->o.keyUp);healthBefore=w.getServer().computeOnServer(s->w.getConnection().getServerPlayer().getHealth());evidence.put("health_before_provocation",healthBefore);c.getInput().pressKey(o->o.keyAttack);c.waitTicks(2);
                attacked=w.getServer().computeOnServer(s->{var a=ColonyAlarm.get(w.getConnection().getServerLevel()).alarm(colony);return a!=null&&a.player().equals(w.getConnection().getServerPlayer().getUUID());});if(attacked)break;
            }else{c.getInput().holdKey(o->o.keyUp);if(i%20==0)c.getInput().pressKey(o->o.keyJump);else c.waitTick();}
        }}finally{c.getInput().releaseKey(o->o.keyUp);c.getInput().releaseKey(o->o.keyAttack);c.getInput().releaseKey(o->o.keyJump);}
        require(attacked,"Ordinary survival attack input must cause accepted damage and alarm within bound");evidence.put("provocation","ordinary keyAttack input on genuine mature worker");
        Map<String,Object> bite=null;
        for(int i=0;i<400&&ticks(w)-start<22300;i++){
            bite=w.getServer().computeOnServer(s->{var p=w.getConnection().getServerPlayer();var d=p.getLastDamageSource();
                if(d==null||!(d.getEntity() instanceof LasiusNigerEntity a)||!colony.equals(a.queenId()))return null;
                return Map.<String,Object>of("ant_uuid",a.getUUID().toString(),"damage_source",d.type().msgId(),"player_uuid",p.getUUID().toString(),"player_health",p.getHealth(),"server_tick",s.getTickCount(),"ant_bites",a.workerTasks().bites());});if(bite!=null)break;c.waitTick();
        }
        require(bite!=null&&((Number)bite.get("player_health")).floatValue()<healthBefore,"Actual ant-caused normal survival player damage must follow approach");evidence.put("damage",bite);
        UUID biter=UUID.fromString((String)bite.get("ant_uuid"));c.runOnClient(client->{var a=clientAnt(client,biter);if(a!=null)client.player.lookAt(EntityAnchorArgument.Anchor.EYES,a.position().add(0,.25,0));});
        capture(c,w,colony,"defense");evidence.put("natural_result","defense_demonstrated");finishNatural(w,start);
    }
    private void finishNatural(TestSingleplayerContext w,int start){int elapsed=ticks(w)-start;require(elapsed<ADDITIONAL_TICKS,"Declared additional server tick bound");evidence.put("additional_server_ticks",elapsed);checkpoint();}
    private static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
}
