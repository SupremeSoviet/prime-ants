package dev.primeants.gametest;

import com.google.gson.GsonBuilder;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.impl.client.gametest.world.TestWorldSaveImpl;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;

/** Client-created ordinary overworld; observer setup is separate from measured survival inputs. */
public final class FreshSurvivalScenario {
    public static final long SEED=2026100501L;
    public static final int ADDITIONAL_TICKS=24000;
    private static final UUID PRIMARY=UUID.fromString("9c71829b-25f1-3b96-acbb-96252228271d"),FALLBACK=UUID.fromString("05888952-07db-3426-8bbc-6cf27cdcf840");
    private final int priorScenarioTicks=Integer.getInteger("prime_ants.priorScenarioTicks",0);
    private final Map<String,Object> evidence=new LinkedHashMap<>();
    private final List<Object> snapshots=new ArrayList<>(),moves=new ArrayList<>(),checks=new ArrayList<>();
    private final Path dir=Path.of(System.getProperty("prime_ants.captureDir"));
    private final String prefix=System.getProperty("prime_ants.capturePrefix");
    private net.minecraft.server.MinecraftServer ownedServer;
    private boolean protectedJoin;
    private int startTick;
    private void checkpoint(){try{Files.createDirectories(dir);Files.writeString(dir.resolve(prefix+"-fresh.json"),new GsonBuilder().setPrettyPrinting().create().toJson(evidence),StandardCharsets.UTF_8);}catch(Exception e){throw new RuntimeException(e);}}
    private static final class ObserverFailure extends AssertionError {ObserverFailure(String why){super(why);}}
    private void observerRequire(boolean ok,String why){if(!ok){evidence.put("natural_result","observer_harness_failure");evidence.put("observer_failure",why);checkpoint();throw new ObserverFailure(why);}}
    private static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public void run(ClientGameTestContext c){
        evidence.put("run_id",System.getProperty("prime_ants.runId"));evidence.put("scenario","fresh-survival");evidence.put("seed",SEED);
        evidence.put("status","opening");evidence.put("checkpoint_success",false);evidence.put("natural_placement","production default");
        evidence.put("render_distance",8);evidence.put("simulation_distance",8);evidence.put("founding_multiplier",QueenFounding.multiplier());evidence.put("brood_multiplier",dev.primeants.brood.BroodPile.multiplier());
        evidence.put("observer_setup","Spectator while loading/travelling/maturing; dry FULL-loaded complete-body check before survival. Camera/daylight setup disclosed; measured entry, drop/retreat and attack use survival inputs.");
        evidence.put("observer_setup_moves",moves);evidence.put("safe_destination_checks",checks);evidence.put("maturation_samples",snapshots);
        evidence.put("ant_position_edits",0);evidence.put("supplied_ants",0);evidence.put("supplied_food",0);evidence.put("terrain_edits",0);evidence.put("ai_pauses",0);evidence.put("measured_teleports",0);
        evidence.put("loading_only",Boolean.getBoolean("prime_ants.freshLoadingOnly"));evidence.put("recovery_only",Boolean.getBoolean("prime_ants.freshObserverRecoveryOnly")||Boolean.getBoolean("prime_ants.freshFoodSyncRecoveryOnly"));evidence.put("native_frame",List.of(1600,1000));require(priorScenarioTicks>=0&&priorScenarioTicks<ADDITIONAL_TICKS,"Declared accumulated natural ticks");
        evidence.put("prior_scenario_ticks",priorScenarioTicks);evidence.put("additional_tick_bound",ADDITIONAL_TICKS-priorScenarioTicks);checkpoint();
        // Count opening/recovery/setup and closure as well as the biological wait.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(s->{ownedServer=s;startTick=s.getTickCount();evidence.put("additional_start_tick",startTick);checkpoint();});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.START_SERVER_TICK.register(s->{if(s==ownedServer&&!protectedJoin)for(var p:s.getPlayerList().getPlayers())if(p.isAlive()){
            p.setGameMode(GameType.SPECTATOR);protectedJoin=true;evidence.put("join_protection","First living server body becomes spectator before physics");checkpoint();break;
        }});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(s->{if(s==ownedServer){evidence.put("additional_ticks_through_close",s.getTickCount()-startTick);evidence.put("accumulated_scenario_ticks",priorScenarioTicks+s.getTickCount()-startTick);checkpoint();}});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->{if(s==ownedServer){evidence.put("normal_server_stop",true);checkpoint();}});
        c.runOnClient(client->{client.options.renderDistance().set(8);client.options.simulationDistance().set(8);client.getWindow().setWindowed(1600,1000);client.options.guiScale().set(2);});
        Path save=null;
        try(var w=open(c)){
            save=w.getWorldSave().getSaveDirectory();evidence.put("save",CapturePaths.repositoryRelative(save));evidence.put("status","joined");checkpoint();
            boolean dead=w.getServer().computeOnServer(s->!w.getConnection().getServerPlayer().isAlive());evidence.put("persisted_observer_dead",dead);
            if(dead){c.waitForScreen(net.minecraft.client.gui.screens.DeathScreen.class);c.waitTicks(25);c.clickScreenButton("deathScreen.respawn");c.waitFor(client->client.player!=null&&client.player.isAlive(),200);evidence.put("respawn_recovery","Normal deathScreen.respawn button; no health edits");}
            alive(w);c.waitForScreen(null);w.getConnection().waitForChunksRender(false);
            evidence.put("generation",w.getServer().computeOnServer(s->{var l=w.getConnection().getServerLevel();require(l.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)&&l.getSeed()==SEED&&NaturalPlacement.productionEnabled(),"Ordinary overworld, declared seed, default placement");return Map.of("dimension",l.dimension().identifier().toString(),"generator",l.getChunkSource().getGenerator().getClass().getName(),"seed",l.getSeed());}));
            int sb=ticks(w),cb=c.computeOnClient(client->client.player.tickCount);wait(c,w,10);
            evidence.put("server_ticks",List.of(sb,ticks(w)));evidence.put("client_ticks",List.of(cb,c.computeOnClient(client->client.player.tickCount)));evidence.put("loading_success",true);checkpoint();
            if(Boolean.getBoolean("prime_ants.freshLoadingOnly"))evidence.put("natural_result","loading_only");else natural(c,w);
        }catch(Throwable e){evidence.put("status","failed");evidence.put("failure",e.toString());if(!evidence.containsKey("natural_result"))evidence.put("natural_result",evidence.containsKey("colony_uuid")?"interaction_incomplete":"observer_harness_failure");evidence.put("normal_close",Boolean.TRUE.equals(evidence.get("normal_server_stop"))&&save!=null&&Files.exists(save.resolve("level.dat")));checkpoint();throw new RuntimeException(e);}
        require(save!=null&&Files.exists(save.resolve("level.dat"))&&Boolean.TRUE.equals(evidence.get("normal_server_stop")),"Normal populated save and closure");
        observerRequire(((Number)evidence.get("accumulated_scenario_ticks")).intValue()<=ADDITIONAL_TICKS,"Opening/interaction/closure exceeded original bound");evidence.put("normal_close",true);evidence.put("status","closed");checkpoint();
    }
    private TestSingleplayerContext open(ClientGameTestContext c)throws Exception {
        String source=System.getProperty("prime_ants.freshWorldArchive");
        if(source==null)return c.worldBuilder().setUseConsistentSettings(false).adjustSettings(s->{s.setSeed(Long.toString(SEED));s.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);s.setAllowCommands(true);}).create();
        require("t22-a4-primary-fallback".equals(System.getProperty("prime_ants.knownSiteRoute"))&&priorScenarioTicks>=3002,"Explicit route and all 3,002 historical ticks required");
        Path save=Path.of("saves",prefix).toAbsolutePath();require(!Files.exists(save),"New owned copy; never overwrite");Files.createDirectories(save);
        evidence.put("source_archive",CapturePaths.publicPath(Path.of(source)));evidence.put("route",List.of(Map.of("priority","primary","queen",PRIMARY.toString(),"initial_ground",List.of(168,67,149)),Map.of("priority","fallback","queen",FALLBACK.toString(),"initial_ground",List.of(168,71,86))));
        try(var zip=new ZipFile(source)){
            var levels=zip.stream().filter(e->e.getName().endsWith("/level.dat")||e.getName().equals("level.dat")).toList();String selected=System.getProperty("prime_ants.freshArchiveWorld");
            if(selected!=null)levels=levels.stream().filter(e->e.getName().equals(selected+"/level.dat")).toList();require(levels.size()==1,"One selected closed client-created world");
            String root=levels.getFirst().getName().replaceFirst("level\\.dat$","");evidence.put("archive_world_root",root);require(zip.stream().noneMatch(e->e.getName().startsWith(root+"dimensions/prime_ants_test/")),"Imported diagnostic dimensions excluded");
            for(var entry:zip.stream().toList())if(entry.getName().startsWith(root)){
                Path target=save.resolve(entry.getName().substring(root.length())).normalize();require(target.startsWith(save),"Safe archive member");
                if(entry.isDirectory())Files.createDirectories(target);else{Files.createDirectories(target.getParent());try(var in=zip.getInputStream(entry)){Files.copy(in,target);}}
            }
        }checkpoint();return new TestWorldSaveImpl(c,save).open();
    }
    private int ticks(TestSingleplayerContext w){return w.getServer().computeOnServer(s->s.getTickCount());}
    private int remaining(TestSingleplayerContext w){return ADDITIONAL_TICKS-priorScenarioTicks-(ticks(w)-startTick);}
    private void alive(TestSingleplayerContext w){
        var state=w.getServer().computeOnServer(s->{var p=w.getConnection().getServerPlayer();var d=p.getLastDamageSource();return Map.<String,Object>of("alive",p.isAlive(),"health",p.getHealth(),"ant",d!=null&&d.getEntity() instanceof LasiusNigerEntity a&&a.queenId()!=null&&a.queenId().toString().equals(evidence.get("colony_uuid")));});
        if(!Boolean.TRUE.equals(state.get("alive"))&&evidence.containsKey("provocation")&&Boolean.TRUE.equals(state.get("ant"))){evidence.put("natural_result","combat_death");evidence.put("combat_death",state);checkpoint();throw new AssertionError("Actual ant combat death");}
        observerRequire(Boolean.TRUE.equals(state.get("alive")),"dead_observer");
        observerRequire(w.getServer().computeOnServer(s->{var p=w.getConnection().getServerPlayer();var l=w.getConnection().getServerLevel();var feet=p.blockPosition();
            return p.isSpectator()||ObserverSafety.full(l,feet.getX(),feet.getZ())&&l.isInWorldBounds(feet)&&l.isInWorldBounds(feet.above())&&l.getWorldBorder().isWithinBounds(p.getBoundingBox())&&l.getFluidState(feet).isEmpty()&&l.getFluidState(feet.above()).isEmpty();
        }),"unsafe_live_observer_fluid_bounds_or_unloaded_body");
    }
    private void wait(ClientGameTestContext c,TestSingleplayerContext w,int n){alive(w);if(remaining(w)<=n+128){evidence.put("natural_result","interaction_bound_exhausted");checkpoint();throw new AssertionError("Original observation bound: preserve incomplete actions and close");}int sb=ticks(w),cb=c.computeOnClient(client->client.player.tickCount);c.waitTicks(n);alive(w);observerRequire(ticks(w)>sb&&c.computeOnClient(client->client.player.tickCount)>cb,"client_or_server_ticks_not_advancing");}
    private void travel(ClientGameTestContext c,TestSingleplayerContext w,BlockPos site){
        alive(w);w.getServer().runOnServer(s->{var p=w.getConnection().getServerPlayer();p.setGameMode(GameType.SPECTATOR);var l=w.getConnection().getServerLevel();p.teleportTo(l,site.getX()+.5,Math.min(l.getMaxY()-4,site.getY()+20),site.getZ()+.5,Set.of(),0,30,true);});
        moves.add(Map.of("mode","spectator","site",List.of(site.getX(),site.getY(),site.getZ()),"purpose","predeclared site loading; Y is camera only"));checkpoint();w.getConnection().waitForClientboundPackets();wait(c,w,10);
    }
    private BlockPos stand(TestSingleplayerContext w,Vec3 focus,int minimum,int maximum){return w.getServer().computeOnServer(s->{var l=w.getConnection().getServerLevel();var options=new ArrayList<BlockPos>();
        for(int x=-maximum;x<=maximum;x++)for(int z=-maximum;z<=maximum;z++){
            if(x*x+z*z<minimum*minimum||x*x+z*z>maximum*maximum)continue;var p=ObserverSafety.surface(l,(int)Math.floor(focus.x)+x,(int)Math.floor(focus.z)+z);
            if(ObserverSafety.problem(l,w.getConnection().getServerPlayer(),p)==null)options.add(p);
        }return options.stream().min(Comparator.comparingDouble(p->Vec3.atBottomCenterOf(p).distanceToSqr(focus))).orElse(null);
    });}
    private void setupAt(ClientGameTestContext c,TestSingleplayerContext w,BlockPos feet,boolean survival){
        String problem=w.getServer().computeOnServer(s->ObserverSafety.problem(w.getConnection().getServerLevel(),w.getConnection().getServerPlayer(),feet));observerRequire(problem==null,"No safe stand: "+problem);
        w.getServer().runOnServer(s->{var p=w.getConnection().getServerPlayer();p.setGameMode(GameType.SPECTATOR);p.teleportTo(w.getConnection().getServerLevel(),feet.getX()+.5,feet.getY(),feet.getZ()+.5,Set.of(),0,20,true);});
        moves.add(Map.of("mode","spectator setup","feet",List.of(feet.getX(),feet.getY(),feet.getZ()),"return_to_survival",survival));w.getConnection().waitForClientboundPackets();wait(c,w,5);
        c.waitFor(client->client.levelRenderer.isSectionCompiledAndVisible(client.player.blockPosition().below(),0),200);
        if(survival){problem=w.getServer().computeOnServer(s->ObserverSafety.problem(w.getConnection().getServerLevel(),w.getConnection().getServerPlayer(),feet));observerRequire(problem==null,"Destination changed: "+problem);
            w.getServer().runOnServer(s->w.getConnection().getServerPlayer().setGameMode(GameType.SURVIVAL));w.getConnection().waitForClientboundPackets();wait(c,w,5);
            observerRequire(c.computeOnClient(client->client.player.isAlive()&&!client.player.isSpectator()&&!client.player.isCreative()&&!client.player.noPhysics&&!client.player.getAbilities().flying),"ordinary_survival_physics_missing");
            observerRequire(w.getServer().computeOnServer(s->ObserverSafety.problem(w.getConnection().getServerLevel(),w.getConnection().getServerPlayer(),w.getConnection().getServerPlayer().blockPosition())==null),"Unsafe survival arrival");
        }
        checks.add(Map.of("feet",List.of(feet.getX(),feet.getY(),feet.getZ()),"FULL",true,"build_height_border",true,"dry_support_feet_head",true,"complete_body_clear",true,"alive",true,"ticks_advancing",true,"mode_after_setup",survival?"survival":"spectator"));checkpoint();
    }
    private Map<String,Object> sample(TestSingleplayerContext w,UUID id){return w.getServer().computeOnServer(s->{var l=w.getConnection().getServerLevel();var out=new LinkedHashMap<String,Object>();out.put("server_tick",s.getTickCount());
        if(!(l.getEntity(id) instanceof LasiusNigerEntity q)){out.put("queen_loaded",false);return out;}
        out.put("queen",id.toString());out.put("alive",q.isAlive());out.put("health",q.getHealth());out.put("position",List.of(q.getX(),q.getY(),q.getZ()));out.put("phase",q.founding().phase().toString());out.put("reason",q.founding().reason());out.put("lifecycle",q.founding().lifecycle().toString());out.put("removed",q.founding().removed());out.put("deposited",q.founding().deposited());out.put("founding_loaded_ticks",q.founding().loadedTicks());out.put("adult_age",q.adultLife().activeTicks());out.put("workers",ColonyMembers.get(l).members(id).size());
        if(q.founding().plan()!=null&&l.getBlockEntity(q.founding().plan().nursery()) instanceof dev.primeants.brood.BroodPile b){out.put("brood_loaded_ticks",b.loadedTicks());out.put("persisted_stage_duration",b.stageDuration());out.put("brood_condition",b.condition());out.put("brood",b.records().stream().map(r->Map.of("id",r.id().toString(),"stage",r.stage().toString())).toList());out.put("first_clutch",b.original().stream().map(UUID::toString).toList());}
        return out;
    });}
    private UUID select(ClientGameTestContext c,TestSingleplayerContext w){
        require(System.getProperty("prime_ants.freshWorldArchive")!=null,"Natural checkpoint requires existing client archive, not blind discovery");
        UUID[] route={PRIMARY,FALLBACK};BlockPos[] sites={new BlockPos(168,67,149),new BlockPos(168,71,86)};var attempts=new ArrayList<Object>();evidence.put("site_viability",attempts);
        for(int i=0;i<route.length;i++){UUID id=route[i];BlockPos site=sites[i];travel(c,w,site);int deadline=ticks(w)+800;boolean loaded=false;
            while(ticks(w)<deadline&&remaining(w)>512){loaded=w.getServer().computeOnServer(s->{var l=w.getConnection().getServerLevel();return ObserverSafety.full(l,site.getX()-12,site.getZ()-12)&&ObserverSafety.full(l,site.getX()+12,site.getZ()+12)&&l.getEntity(id)!=null;});if(loaded)break;wait(c,w,10);}
            var snap=sample(w,id);attempts.add(snap);boolean identity=w.getServer().computeOnServer(s->NaturalPlacement.get(w.getConnection().getServerLevel()).decisions().entrySet().stream().map(Map.Entry::getValue).anyMatch(d->"PLACED".equals(d.getAsJsonObject().get("status").getAsString())&&id.toString().equals(d.getAsJsonObject().get("queen").getAsString())));
            snap.put("FULL_site_loaded",loaded);snap.put("genuine_placement_identity",identity);snap.put("route_site",List.of(site.getX(),site.getY(),site.getZ()));if(!loaded||!identity||!Boolean.TRUE.equals(snap.get("alive"))||List.of("FAILED","DEAD","NONE").contains(snap.get("phase"))){evidence.put(i==0?"primary_viability_failure":"fallback_viability_failure",Map.of("loaded",loaded,"placement_identity",identity,"sample",snap));checkpoint();continue;}
            evidence.put("colony_uuid",id.toString());evidence.put("selected_priority",i==0?"primary":"fallback");evidence.put("colony_origin","Existing production automatic queen in closed T22 A4 client-created ordinary overworld; known-site access, not spawn discovery");
            var pos=w.getServer().computeOnServer(s->w.getConnection().getServerLevel().getEntity(id).position());var observer=stand(w,pos,5,8);observerRequire(observer!=null,"FULL site has no safe dry stand");setupAt(c,w,observer,false);snapshots.add(sample(w,id));checkpoint();return id;
        }observerRequire(false,"Predeclared sites unavailable or nonviable");return null;
    }
    private void natural(ClientGameTestContext c,TestSingleplayerContext w){
        final UUID colony=select(c,w);if(Boolean.getBoolean("prime_ants.freshFoodSyncRecoveryOnly")){foodSyncRecovery(c,w,colony);return;}evidence.put("status","waiting_natural_founding_and_maturation");checkpoint();UUID worker=null;int nextSample=ticks(w);
        while(remaining(w)>160){var state=sample(w,colony);if(ticks(w)>=nextSample){snapshots.add(state);checkpoint();nextSample=ticks(w)+200;}
            if(!Boolean.TRUE.equals(state.get("alive"))||List.of("FAILED","DEAD").contains(state.get("phase"))){evidence.put("natural_result","colony_failure");evidence.put("terminal_colony",state);checkpoint();return;}
            worker=w.getServer().computeOnServer(s->{var l=w.getConnection().getServerLevel();var q=(LasiusNigerEntity)l.getEntity(colony);if(q.founding().plan()==null||q.founding().lifecycle()!=QueenFounding.Lifecycle.OPEN)return null;
                return ColonyMembers.get(l).members(colony).stream().map(m->l.getEntity(m.worker())).filter(e->e instanceof LasiusNigerEntity).map(e->(LasiusNigerEntity)e).filter(a->a.isAlive()&&!a.isCallow()&&a.getY()>=q.founding().plan().entrance().getY()+.8).map(LasiusNigerEntity::getUUID).findFirst().orElse(null);});
            if(worker!=null)break;wait(c,w,10);
        }snapshots.add(sample(w,colony));if(worker==null){evidence.put("natural_result","incomplete_maturation");evidence.put("missing_stages",List.of("watch","entry_return","feeding","client_bite","checkpoint_images"));checkpoint();return;}
        final UUID subject=worker;evidence.put("mature_worker_uuid",subject.toString());var plan=w.getServer().computeOnServer(s->((LasiusNigerEntity)w.getConnection().getServerLevel().getEntity(colony)).founding().plan());evidence.put("entrance",List.of(plan.entrance().getX(),plan.entrance().getY(),plan.entrance().getZ()));
        // A closed continuation may still contain the previous genuine alarm. Wait it out normally;
        // watching/feeding cannot be judged benign by clearing that persisted production state.
        int alarmWait=0;while(alarmWait<620&&w.getServer().computeOnServer(s->ColonyAlarm.get(w.getConnection().getServerLevel()).alarm(colony)!=null)){wait(c,w,1);alarmWait++;}
        evidence.put("prior_alarm_loaded_wait_ticks",alarmWait);require(w.getServer().computeOnServer(s->ColonyAlarm.get(w.getConnection().getServerLevel()).alarm(colony)==null),"Prior real alarm must expire through production ticks before benign watching");
        var viewing=stand(w,Vec3.atBottomCenterOf(plan.entrance()),3,5);observerRequire(viewing!=null,"No safe exterior survival stand");setupAt(c,w,viewing,true);
        w.getServer().runCommand("time set day");evidence.put("daytime_observer_command","time set day; production loaded clocks continue");c.runOnClient(client->{if(!client.gui.hud.isHidden())client.gui.hud.toggle();client.options.fov().set(70);client.player.lookAt(EntityAnchorArgument.Anchor.EYES,Vec3.atCenterOf(plan.entrance()));});wait(c,w,100);
        require(w.getServer().computeOnServer(s->ColonyAlarm.get(w.getConnection().getServerLevel()).alarm(colony)==null),"Watching must be benign");evidence.put("watch",true);evidence.put("unprovoked_watch_ticks",100);capture(c,w,colony,"entrance");
        if(Boolean.getBoolean("prime_ants.freshObserverRecoveryOnly")){evidence.put("natural_result","observer_recovery_only");checkpoint();return;}
        walk(c,w,Vec3.atBottomCenterOf(plan.outside()),200,false);walk(c,w,Vec3.atBottomCenterOf(plan.at(0,0,0)),120,false);walk(c,w,Vec3.atBottomCenterOf(plan.at(1,0,-1)),120,false);walk(c,w,Vec3.atBottomCenterOf(plan.at(2,0,-2)),120,false);walk(c,w,Vec3.atBottomCenterOf(plan.at(3,0,-2)),180,false);
        require(c.computeOnClient(client->client.player.getY()<plan.entrance().getY()-1.5),"Walk into existing chamber");evidence.put("entered",true);c.runOnClient(client->client.player.lookAt(EntityAnchorArgument.Anchor.EYES,Vec3.atCenterOf(plan.nursery()).add(0,.1,0)));capture(c,w,colony,"interior");
        walk(c,w,Vec3.atBottomCenterOf(plan.at(2,0,-2)),120,false);walk(c,w,Vec3.atBottomCenterOf(plan.at(1,0,-1)),120,true);walk(c,w,Vec3.atBottomCenterOf(plan.at(0,0,0)),120,true);walk(c,w,Vec3.atBottomCenterOf(plan.outside()),180,true);evidence.put("returned",true);checkpoint();
        feed(c,w,colony,plan);defend(c,w,colony,subject);evidence.put("natural_result","interactions_complete");evidence.put("interactions_complete",true);checkpoint();
    }
    private void walk(ClientGameTestContext c,TestSingleplayerContext w,Vec3 target,int bound,boolean jump){c.getInput().holdKey(o->o.keyUp);if(jump)c.getInput().holdKey(o->o.keyJump);
        try{for(int t=0;t<bound;t++){alive(w);observerRequire(c.computeOnClient(client->!client.player.isSpectator()&&!client.player.noPhysics&&!client.player.getAbilities().flying),"Measured movement lost survival physics");if(c.computeOnClient(client->client.player.position().distanceToSqr(target)<.36))return;
            c.runOnClient(client->{var d=target.subtract(client.player.position());client.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));});wait(c,w,1);
        }throw new AssertionError("Survival walk did not reach existing waypoint "+target);}finally{c.getInput().releaseKey(o->o.keyUp);c.getInput().releaseKey(o->o.keyJump);}
    }
    private long intake(ServerLevel l,UUID colony,Item item){long total=0;for(var e:l.getAllEntities())if(e instanceof LasiusNigerEntity a&&colony.equals(a.colonyIdentity()))total+=item==Items.APPLE?a.nutrition().apples():a.nutrition().chickens();var q=(LasiusNigerEntity)l.getEntity(colony);if(q!=null&&q.founding().plan()!=null&&l.getBlockEntity(q.founding().plan().nursery()) instanceof dev.primeants.brood.BroodPile b)total+=item==Items.APPLE?b.consumedApples():b.consumedChickens();return total;}
    private void feed(ClientGameTestContext c,TestSingleplayerContext w,UUID colony,NestPlan plan){
        String choice=System.getProperty("prime_ants.playerFood","apple");require(choice.equals("apple")||choice.equals("raw-chicken"),"Only declared apple or optional raw chicken");final Item food=choice.equals("apple")?Items.APPLE:Items.CHICKEN;
        if(food==Items.CHICKEN){try{Path parent=Path.of(System.getProperty("prime_ants.priorAppleEvidence"));var prior=com.google.gson.JsonParser.parseString(Files.readString(parent,StandardCharsets.UTF_8)).getAsJsonObject();var trace=prior.getAsJsonObject("food_trace");require(prior.get("normal_close").getAsBoolean()&&colony.toString().equals(prior.get("colony_uuid").getAsString())&&trace.get("item").getAsString().equals("minecraft:apple")&&trace.get("delivered").getAsBoolean()&&trace.get("inventory_final").getAsInt()==0,"Required prior apple trace in this same colony");evidence.put("prior_apple_evidence",CapturePaths.publicPath(parent));}catch(Exception e){throw new RuntimeException(e);}}
        w.getServer().runOnServer(s->{var p=w.getConnection().getServerPlayer();require(p.getInventory().countItem(food)==0,"No preexisting declared food may confound trace");p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(food));});evidence.put("supplied_food","One declared player "+choice+", separate from autonomous intake");w.getConnection().waitForClientboundPackets();c.waitFor(client->client.player.getMainHandItem().is(food),100);
        long intake=w.getServer().computeOnServer(s->intake(w.getConnection().getServerLevel(),colony,food));var trace=new LinkedHashMap<String,Object>();evidence.put("food_trace",trace);trace.put("item",net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(food).toString());trace.put("intake_before",intake);checkpoint();var origin=c.computeOnClient(client->client.player.position());
        c.runOnClient(client->{client.player.setYRot(plan.direction().toYRot());client.player.setXRot(45);});c.getInput().holdKey(o->o.keyDrop);c.getInput().holdKey(o->o.keyDown);c.getInput().holdKey(o->o.keyJump);UUID drop;
        try{wait(c,w,1);c.getInput().releaseKey(o->o.keyDrop);w.getConnection().waitForServerboundPackets();drop=w.getServer().computeOnServer(s->{var l=w.getConnection().getServerLevel();require(w.getConnection().getServerPlayer().getInventory().countItem(food)==0,"Inventory loses declared food");var items=l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(plan.outside()).inflate(7),e->e.isAlive()&&e.getItem().is(food));require(items.size()==1,"Unique actual player world-item UUID");return items.getFirst().getUUID();});
            trace.put("world_uuid",drop.toString());trace.put("inventory_loss",1);trace.put("input","keyDrop with immediate keyDown/keyJump");checkpoint();for(int t=0;t<100&&c.computeOnClient(client->horizontal(client.player.position(),origin)<16);t++)wait(c,w,1);
        }finally{c.getInput().releaseKey(o->o.keyDrop);c.getInput().releaseKey(o->o.keyDown);c.getInput().releaseKey(o->o.keyJump);}
        double away=c.computeOnClient(client->Math.sqrt(horizontal(client.player.position(),origin)));require(away>=4,"Immediate four-block retreat");trace.put("step_away_blocks",away);UUID carrier=null;
        for(int t=0;t<2400&&carrier==null&&remaining(w)>400;t++){wait(c,w,1);require(w.getServer().computeOnServer(s->w.getConnection().getServerPlayer().getInventory().countItem(food)==0),"Declared food not recollected");carrier=w.getServer().computeOnServer(s->{var l=w.getConnection().getServerLevel();if(l.getEntity(drop)!=null)return null;for(var e:l.getAllEntities())if(e instanceof LasiusNigerEntity a&&a.isAlive()&&colony.equals(a.queenId())&&a.getMainHandItem().is(food)&&!a.workerTasks().nursing())return a.getUUID();return null;});}
        require(carrier!=null,"Genuine forager collects declared food");final UUID actor=carrier;trace.put("carrier",actor.toString());trace.put("carried_units",1);trace.put("world_item_removed",true);checkpoint();w.getConnection().waitForClientboundPackets();c.waitFor(client->{var ant=clientAnt(client,actor);return ant!=null&&ant.getMainHandItem().is(food);},100);c.runOnClient(client->{var a=clientAnt(client,actor);if(a!=null)client.player.lookAt(EntityAnchorArgument.Anchor.EYES,a.position().add(0,.25,0));});capture(c,w,colony,"feeding");require(((java.util.List<?>)evidence.get("feeding_rendered_ants")).stream().anyMatch(r->actor.toString().equals(((Map<?,?>)r).get("uuid"))&&net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(food).toString().equals(((Map<?,?>)r).get("carried_item"))),"Actual client food must still be present in the captured frame");boolean delivered=false;
        for(int t=0;t<1800&&!delivered&&remaining(w)>400;t++){wait(c,w,1);require(w.getServer().computeOnServer(s->w.getConnection().getServerPlayer().getInventory().countItem(food)==0),"Declared food not recollected before delivery");delivered=w.getServer().computeOnServer(s->{var l=w.getConnection().getServerLevel();return l.getBlockEntity(plan.cache()) instanceof NestCache n&&n.ownedBy(colony,plan)&&n.contents().stream().anyMatch(stack->stack.is(food))||intake(l,colony,food)>intake;});}
        require(delivered,"Cargo reaches owned cache or actual consumption");trace.put("delivered",true);trace.put("inventory_final",0);trace.put("intake_after",w.getServer().computeOnServer(s->intake(w.getConnection().getServerLevel(),colony,food)));evidence.put("fed",true);require(w.getServer().computeOnServer(s->ColonyAlarm.get(w.getConnection().getServerLevel()).alarm(colony)==null),"Feeding must stay benign");checkpoint();
    }
    private void foodSyncRecovery(ClientGameTestContext c,TestSingleplayerContext w,UUID colony){
        evidence.put("status","targeted_food_equipment_sync_recovery");evidence.put("recovery_supply","none; passive existing/native food only");checkpoint();
        c.runOnClient(client->{if(!client.gui.hud.isHidden())client.gui.hud.toggle();});
        for(int t=0;t<1600&&remaining(w)>256;t++){
            var actor=w.getServer().computeOnServer(s->{var l=w.getConnection().getServerLevel();for(var e:l.getAllEntities())if(e instanceof LasiusNigerEntity ant&&ant.isAlive()&&colony.equals(ant.queenId())&&(Nutrition.sugarYield(ant.getMainHandItem())>0||Nutrition.proteinYield(ant.getMainHandItem())>0))return ant.getUUID();return null;});
            if(actor!=null){w.getConnection().waitForClientboundPackets();boolean ready=c.computeOnClient(client->{var ant=clientAnt(client,actor);return ant!=null&&(Nutrition.sugarYield(ant.getMainHandItem())>0||Nutrition.proteinYield(ant.getMainHandItem())>0);});
                if(ready){c.runOnClient(client->client.player.lookAt(EntityAnchorArgument.Anchor.EYES,clientAnt(client,actor).position().add(0,.25,0)));capture(c,w,colony,"food-sync");
                    boolean actual=((java.util.List<?>)evidence.get("food-sync_rendered_ants")).stream().anyMatch(r->actor.toString().equals(((Map<?,?>)r).get("uuid"))&&!"minecraft:air".equals(((Map<?,?>)r).get("carried_item")));
                    require(actual,"Equipment-synced native frame must contain actual carried food");evidence.put("food_sync_actor",actor.toString());evidence.put("natural_result","food_sync_recovery_only");checkpoint();return;
                }
            }wait(c,w,1);
        }evidence.put("natural_result","food_sync_recovery_incomplete");checkpoint();throw new AssertionError("Bounded native equipment observation unavailable; no supply or actor edits");
    }
    private void defend(ClientGameTestContext c,TestSingleplayerContext w,UUID colony,UUID subject){float before=w.getServer().computeOnServer(s->w.getConnection().getServerPlayer().getHealth());boolean attacked=false;
        try{for(int t=0;t<800&&!attacked&&remaining(w)>512;t++){alive(w);boolean aimed=c.computeOnClient(client->{var a=clientAnt(client,subject);if(a==null)return false;client.player.lookAt(EntityAnchorArgument.Anchor.EYES,a.position().add(0,.25,0));return client.hitResult instanceof EntityHitResult h&&h.getEntity()==a;});
            if(aimed){c.getInput().releaseKey(o->o.keyUp);before=w.getServer().computeOnServer(s->w.getConnection().getServerPlayer().getHealth());c.getInput().pressKey(o->o.keyAttack);attacked=w.getServer().computeOnServer(s->{var a=ColonyAlarm.get(w.getConnection().getServerLevel()).alarm(colony);return a!=null&&a.player().equals(w.getConnection().getServerPlayer().getUUID());});}else{c.getInput().holdKey(o->o.keyUp);wait(c,w,1);}
        }}finally{c.getInput().releaseKey(o->o.keyUp);c.getInput().releaseKey(o->o.keyAttack);}
        require(attacked,"Ordinary attack genuinely harms worker before alarm");evidence.put("provocation","keyAttack on genuine mature worker");evidence.put("health_before_provocation",before);checkpoint();Map<String,Object> bite=null;
        for(int t=0;t<400&&bite==null&&remaining(w)>256;t++){bite=w.getServer().computeOnServer(s->{var p=w.getConnection().getServerPlayer();var d=p.getLastDamageSource();if(d==null||!(d.getEntity() instanceof LasiusNigerEntity a)||!colony.equals(a.queenId()))return null;return Map.<String,Object>of("ant_uuid",a.getUUID().toString(),"damage_source",d.type().msgId(),"player_health",p.getHealth(),"server_tick",s.getTickCount(),"ant_bites",a.workerTasks().bites(),"player_alive",p.isAlive());});if(bite==null)wait(c,w,1);}
        require(bite!=null&&((Number)bite.get("player_health")).floatValue()<before,"Actual ant-source health loss");evidence.put("damage",bite);evidence.put("client_bite",true);checkpoint();UUID actor=UUID.fromString((String)bite.get("ant_uuid"));c.runOnClient(client->{var a=clientAnt(client,actor);if(a!=null)client.player.lookAt(EntityAnchorArgument.Anchor.EYES,a.position().add(0,.25,0));});capture(c,w,colony,"defense");
        if(!Boolean.TRUE.equals(bite.get("player_alive"))){evidence.put("combat_death",true);evidence.put("natural_result","combat_death");throw new AssertionError("Actual ant combat death; no completed retreat");}
        c.getInput().holdKey(o->o.keyDown);c.getInput().holdKey(o->o.keyJump);try{wait(c,w,35);}finally{c.getInput().releaseKey(o->o.keyDown);c.getInput().releaseKey(o->o.keyJump);}evidence.put("defense_retreat",true);
    }
    private static double horizontal(Vec3 a,Vec3 b){double x=a.x-b.x,z=a.z-b.z;return x*x+z*z;}
    private LasiusNigerEntity clientAnt(net.minecraft.client.Minecraft client,UUID id){if(client.level!=null)for(var e:client.level.entitiesForRendering())if(e instanceof LasiusNigerEntity a&&a.getUUID().equals(id))return a;return null;}
    private void capture(ClientGameTestContext c,TestSingleplayerContext w,UUID colony,String name){var ids=w.getServer().computeOnServer(s->{var set=new HashSet<UUID>();set.add(colony);ColonyMembers.get(w.getConnection().getServerLevel()).members(colony).forEach(m->set.add(m.worker()));return set;});c.runOnClient(client->AntRenderRecorder.start(ids));var png=c.takeScreenshot(TestScreenshotOptions.of(prefix+"-"+name).disableCounterPrefix().withDeltaTicks(1).withDestinationDir(dir));var rendered=c.computeOnClient(client->AntRenderRecorder.finish());evidence.put(name+"_image",CapturePaths.repositoryRelative(png));evidence.put(name+"_rendered_ants",rendered);checkpoint();require(!rendered.isEmpty(),"Actual ants must render; external readability inspection still required");}
}
