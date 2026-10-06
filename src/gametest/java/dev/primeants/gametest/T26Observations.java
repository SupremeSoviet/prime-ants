package dev.primeants.gametest;

import dev.primeants.brood.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.NestPlan;
import dev.primeants.worker.NestCache;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Bounded passive observations and explicitly authorized ordinary player interventions. */
final class T26Observations {
    private static final UUID QUEEN=UUID.fromString("9c71829b-25f1-3b96-acbb-96252228271d");
    private final AppearanceScenario a;
    private final List<Object> samples=new ArrayList<>(),drops=new ArrayList<>(),torches=new ArrayList<>();
    T26Observations(AppearanceScenario a){this.a=a;}
    private static void require(boolean b,String reason){AppearanceScenario.require(b,reason);}
    private static List<Integer> xyz(BlockPos p){return List.of(p.getX(),p.getY(),p.getZ());}
    private static List<Double> xyz(Vec3 p){return List.of(p.x,p.y,p.z);}

    private Map<String,Object> snapshot(TestSingleplayerContext w,NestPlan plan){
        return w.getServer().computeOnServer(s->{
            var l=w.getConnection().getServerLevel();var out=new LinkedHashMap<String,Object>();
            require(NestPlan.loaded(l,plan.nursery()),"FULL loaded nursery");
            require(l.getEntity(QUEEN) instanceof LasiusNigerEntity q&&q.isAlive(),"Living natural queen");
            var q=(LasiusNigerEntity)l.getEntity(QUEEN);var b=(BroodPile)l.getBlockEntity(plan.nursery());
            require(b!=null&&b.ownedBy(QUEEN,plan),"Real owned production nursery");
            out.put("server_tick",s.getTickCount());out.put("game_time",l.getGameTime());out.put("pile",xyz(plan.nursery()));out.put("pile_loaded_ticks",b.loadedTicks());out.put("persisted_stage_duration",b.stageDuration());out.put("condition",b.condition());out.put("last_laying_tick",b.lastLayingTick());out.put("growth_reason",b.growth().reason());out.put("supply",b.supply(l));out.put("blockstate",b.getBlockState().toString());
            out.put("brood",b.records().stream().map(r->Map.of("uuid",r.id().toString(),"slot",r.slot(),"stage",r.stage().toString(),"progress",r.progress(),"founding",r.founding(),"nourishment",r.nourishment(),"sugar",r.nutrition().sugar(),"protein",r.nutrition().protein(),"neglect_ticks",r.neglectTicks(),"expired",b.expired().containsKey(r.id()))).toList());
            out.put("queen",Map.of("uuid",q.getUUID().toString(),"position",xyz(q.position()),"sugar",q.nutrition().sugar(),"protein",q.nutrition().protein(),"apples",q.nutrition().apples(),"chickens",q.nutrition().chickens()));
            out.put("workers",java.util.stream.StreamSupport.stream(l.getAllEntities().spliterator(),false).filter(e->e instanceof LasiusNigerEntity ant&&QUEEN.equals(ant.queenId())).map(e->{var ant=(LasiusNigerEntity)e;return Map.of("uuid",ant.getUUID().toString(),"alive",ant.isAlive(),"position",xyz(ant.position()),"task",ant.workerTasks().phase().toString(),"item",ant.getMainHandItem().toString(),"sugar",ant.nutrition().sugar(),"protein",ant.nutrition().protein());}).toList());
            out.put("cache",l.getBlockEntity(plan.cache()) instanceof NestCache n?n.contents().stream().map(Object::toString).toList():List.of());
            out.put("observer",Map.of("alive",w.getConnection().getServerPlayer().isAlive(),"mode",w.getConnection().getServerPlayer().gameMode.getGameModeForPlayer().toString(),"position",xyz(w.getConnection().getServerPlayer().position())));return out;
        });
    }
    void brood(ClientGameTestContext c,TestSingleplayerContext w,BlockPos exterior){
        var plan=w.getServer().computeOnServer(s->((LasiusNigerEntity)w.getConnection().getServerLevel().getEntity(QUEEN)).founding().plan());
        require(plan!=null,"Existing natural nest plan");
        a.evidence.put("brood_samples",samples);a.evidence.put("player_drops",drops);a.evidence.put("torch_placements",torches);a.evidence.put("work_multiplier",20);a.evidence.put("brood_multiplier",100);a.evidence.put("supplied_apples",0);a.evidence.put("supplied_chickens",0);a.evidence.put("supplied_torches",0);
        a.evidence.put("intervention_caption","Player-fed natural colony; normal survival inventory drops and vanilla torch interactions; production care/laying/development only");
        samples.add(snapshot(w,plan));a.save();
        int apples=Integer.getInteger("prime_ants.appearanceApples",0),chickens=Integer.getInteger("prime_ants.appearanceChickens",0);
        if(apples+chickens>0){
            require(apples<=4&&chickens<=2&&apples>=0&&chickens>=0,"Declared food allowance");
            var start=w.getServer().computeOnServer(s->{var l=w.getConnection().getServerLevel();for(int f=-3;f<=-1;f++){var p=f==-3?plan.outside():plan.at(f,0,0);if(ObserverSafety.problem(l,w.getConnection().getServerPlayer(),p)==null)return p;}return exterior;});
            setup(c,w,start);a.survival(true);w.getServer().runOnServer(s->w.getConnection().getServerPlayer().setGameMode(GameType.SURVIVAL));w.getConnection().waitForClientboundPackets();a.wait(c,w,2);
            w.getServer().runOnServer(server->{var player=w.getConnection().getServerPlayer();if(player.getMainHandItem().is(Items.TORCH)){require(player.getOffhandItem().isEmpty(),"Empty offhand for preserving already authorized torches");player.setItemInHand(InteractionHand.OFF_HAND,player.getMainHandItem().copy());player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);}});
            w.getConnection().waitForClientboundPackets();
            for(var pair:List.of(new AbstractMap.SimpleEntry<>(Items.APPLE,apples),new AbstractMap.SimpleEntry<>(Items.CHICKEN,chickens))){
                if(pair.getValue()==0)continue;var item=pair.getKey();int count=pair.getValue();
                w.getServer().runOnServer(s->{var p=w.getConnection().getServerPlayer();require(p.getInventory().countItem(item)==0,"No undeclared food inventory");p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item,count));});w.getConnection().waitForClientboundPackets();
                c.waitFor(client->client.player.getMainHandItem().is(item));
                var origin=c.computeOnClient(client->client.player.position());
                c.runOnClient(client->{client.player.lookAt(EntityAnchorArgument.Anchor.EYES,Vec3.atCenterOf(plan.entrance()));client.player.setXRot(45);client.gameMode.dropItem(client.player,true);});w.getConnection().waitForServerboundPackets();
                var row=new LinkedHashMap<String,Object>();row.put("item",net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString());row.put("count",count);row.put("origin",xyz(origin));
                row.put("world_items",w.getServer().computeOnServer(s->w.getConnection().getServerLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(start).inflate(5),e->e.isAlive()&&e.getItem().is(item)).stream().map(e->Map.of("uuid",e.getUUID().toString(),"position",xyz(e.position()),"count",e.getItem().getCount())).toList()));
                require(w.getServer().computeOnServer(s->w.getConnection().getServerPlayer().getInventory().countItem(item)==0),"Declared food leaves real survival inventory");
                drops.add(row);a.evidence.put(item==Items.APPLE?"supplied_apples":"supplied_chickens",count);a.save();
            }
            var origin=c.computeOnClient(client->client.player.position());
            c.getInput().holdKey(o->o.keyDown);c.getInput().holdKey(o->o.keyJump);
            try{for(int t=0;t<90&&c.computeOnClient(client->client.player.position().subtract(origin).horizontalDistanceSqr()<16);t++)a.wait(c,w,1);}finally{c.getInput().releaseKey(o->o.keyDown);c.getInput().releaseKey(o->o.keyJump);}
            require(c.computeOnClient(client->client.player.position().subtract(origin).horizontalDistanceSqr()>=16),"Four-block ordinary survival retreat");
            a.evidence.put("food_retreat",xyz((Vec3)c.computeOnClient(client->client.player.position())));a.save();
        }
        // Setup relocation is before measured entry/placement. No spectator callback survives this switch.
        a.survival(false);w.getServer().runOnServer(s->w.getConnection().getServerPlayer().setGameMode(GameType.SPECTATOR));setup(c,w,plan.at(3,0,-2));
        a.survival(true);w.getServer().runOnServer(s->w.getConnection().getServerPlayer().setGameMode(GameType.SURVIVAL));w.getConnection().waitForClientboundPackets();a.wait(c,w,2);
        a.evidence.put("interior_access","Spectator setup on verified supported chamber entry before measured survival placement/view; existing connected two-high stairs, T23 and T26 A3 normal entry; no measured teleport");a.save();
        w.getServer().runOnServer(server->{var player=w.getConnection().getServerPlayer();if(player.getOffhandItem().is(Items.TORCH)&&player.getMainHandItem().isEmpty()){player.setItemInHand(InteractionHand.MAIN_HAND,player.getOffhandItem().copy());player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);}});w.getConnection().waitForClientboundPackets();
        int supplied=Integer.getInteger("prime_ants.appearanceTorches",0);require(supplied>=0&&supplied<=2,"At most two declared torches");
        if(supplied>0){w.getServer().runOnServer(s->{var p=w.getConnection().getServerPlayer();require(p.getInventory().countItem(Items.TORCH)==0,"No undeclared torches");p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.TORCH,supplied));});w.getConnection().waitForClientboundPackets();c.waitFor(client->client.player.getMainHandItem().is(Items.TORCH));}
        a.evidence.put("supplied_torches",supplied);a.save();
        int available=w.getServer().computeOnServer(server->w.getConnection().getServerPlayer().getInventory().countItem(Items.TORCH));
        require(available<=2,"Only the two authorized inventory torches may be used, including continuation");
        for(int n=0;n<available;n++)placeTorch(c,w,plan,n);
        w.getServer().runCommand("effect clear @p minecraft:night_vision");
        c.runOnClient(client->{client.options.gamma().set(.5);client.options.fov().set(70);client.player.lookAt(EntityAnchorArgument.Anchor.EYES,Vec3.atBottomCenterOf(plan.nursery()).add(0,.15,0));});a.wait(c,w,5);
        var seen=new HashSet<String>();int bound=5000;
        a.evidence.put("brood_wait_bound",bound);a.save();
        for(int t=0;t<bound;t++){
            var snap=snapshot(w,plan);var brood=(List<?>)snap.get("brood");
            if(t%100==0){samples.add(snap);a.save();}
            for(Object obj:brood){var row=(Map<?,?>)obj;String stage=row.get("stage").toString();if(seen.add(stage)){
                samples.add(snap);a.save();broodCapture(c,w,plan,"brood-"+stage.toLowerCase()+"-overview",70);
                broodCapture(c,w,plan,"brood-"+stage.toLowerCase()+"-detail",45);
            }}
            if(seen.containsAll(Set.of("EGG","LARVA","COCOON")))break;
            a.wait(c,w,1);
        }
        samples.add(snapshot(w,plan));a.evidence.put("observed_live_stages",seen.stream().sorted().toList());a.evidence.put("missing_live_stages",Set.of("EGG","LARVA","COCOON").stream().filter(s->!seen.contains(s)).sorted().toList());a.save();
    }
    void broodFinal(ClientGameTestContext c,TestSingleplayerContext w){
        var plan=w.getServer().computeOnServer(server->((LasiusNigerEntity)w.getConnection().getServerLevel().getEntity(QUEEN)).founding().plan());
        a.evidence.put("brood_samples",samples);a.evidence.put("supplied_apples",0);a.evidence.put("supplied_chickens",0);a.evidence.put("supplied_torches",0);
        samples.add(snapshot(w,plan));a.save();
        setup(c,w,plan.at(1,0,-1));a.survival(true);w.getServer().runOnServer(server->w.getConnection().getServerPlayer().setGameMode(GameType.SURVIVAL));w.getConnection().waitForClientboundPackets();a.wait(c,w,3);
        w.getServer().runCommand("effect clear @p minecraft:night_vision");c.runOnClient(client->client.options.gamma().set(.5));
        a.evidence.put("interior_access","Pre-measurement spectator setup on dry FULL supported existing corridor, followed by living survival view; no virtual lens or measured teleport");
        a.evidence.put("intervention_caption","Continuation of the actual closed player-fed T26 A5 state, with the same two survival-placed vanilla torches; no renewed food/torches or biological writes");
        broodCapture(c,w,plan,"nursery-final-overview",85);broodCapture(c,w,plan,"nursery-final-detail",70);
        samples.add(snapshot(w,plan));a.save();
    }
    private void setup(ClientGameTestContext c,TestSingleplayerContext w,BlockPos feet){
        w.getServer().runOnServer(s->{var l=w.getConnection().getServerLevel();var p=w.getConnection().getServerPlayer();require(ObserverSafety.problem(l,p,feet)==null,"Dry FULL support and complete player body at setup "+feet+": "+ObserverSafety.problem(l,p,feet));p.teleportTo(l,feet.getX()+.5,feet.getY(),feet.getZ()+.5,Set.of(),0,30,true);});w.getConnection().waitForClientboundPackets();a.wait(c,w,2);
    }
    private void walk(ClientGameTestContext c,TestSingleplayerContext w,Vec3 target,int bound){
        c.getInput().holdKey(o->o.keyUp);
        try{for(int t=0;t<bound;t++){
            if(c.computeOnClient(client->client.player.position().distanceToSqr(target)<.12))return;
            c.runOnClient(client->{var d=target.subtract(client.player.position());client.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));});a.wait(c,w,1);
        }throw new AssertionError("Ordinary survival walk did not reach "+target);}finally{c.getInput().releaseKey(o->o.keyUp);a.wait(c,w,1);}
    }
    private void placeTorch(ClientGameTestContext c,TestSingleplayerContext w,NestPlan plan,int index){
        BlockHitResult hit=c.computeOnClient(client->{
            var eye=client.player.getEyePosition();
            for(int f=3;f<=5;f++)for(int side:new int[]{-1,1}){
                var target=plan.at(f,side,-1);if(!client.level.getBlockState(target).isAir()||!client.level.getFluidState(target).isEmpty())continue;
                for(var d:Direction.Plane.HORIZONTAL){var support=target.relative(d);if(!client.level.getBlockState(support).isSolidRender())continue;
                    var face=Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(d.getOpposite().getUnitVec3i()).scale(.5));
                    if(eye.distanceTo(face)>4.5)continue;
                    var ray=client.level.clip(new ClipContext(eye,face.add(Vec3.atLowerCornerOf(d.getUnitVec3i()).scale(.02)),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,client.player));
                    if(ray.getType()==HitResult.Type.BLOCK&&ray.getBlockPos().equals(support))return ray;
                }
            }return null;
        });require(hit!=null,"Reachable existing torch support with unobstructed click ray");
        var before=c.computeOnClient(client->client.player.getMainHandItem().getCount());var origin=c.computeOnClient(client->client.player.position());
        c.runOnClient(client->{require(!client.player.isSpectator()&&client.player.isAlive()&&!client.player.getAbilities().flying,"Living survival torch placement");client.player.lookAt(EntityAnchorArgument.Anchor.EYES,hit.getLocation());client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,hit);});w.getConnection().waitForServerboundPackets();w.getConnection().waitForClientboundPackets();a.wait(c,w,2);
        var target=hit.getBlockPos().relative(hit.getDirection());
        require(w.getServer().computeOnServer(s->{var l=w.getConnection().getServerLevel();var p=w.getConnection().getServerPlayer();return p.isAlive()&&p.gameMode.getGameModeForPlayer()==GameType.SURVIVAL&&(l.getBlockState(target).is(Blocks.TORCH)||l.getBlockState(target).is(Blocks.WALL_TORCH))&&p.getMainHandItem().getCount()==before-1;}),"Normal survival interaction places vanilla torch and consumes inventory");
        torches.add(Map.of("index",index,"support",xyz(hit.getBlockPos()),"target",xyz(target),"face",hit.getDirection().toString(),"eye",xyz((Vec3)c.computeOnClient(client->client.player.getEyePosition())),"player",xyz(origin),"reach",origin.add(0,1.62,0).distanceTo(hit.getLocation()),"method","MultiPlayerGameMode.useItemOn; normal survival prediction/packet/BlockItem placement"));a.save();
    }
    private void broodCapture(ClientGameTestContext c,TestSingleplayerContext w,NestPlan plan,String name,int fov){
        c.runOnClient(client->{client.options.fov().set(fov);var focus=Vec3.atBottomCenterOf(plan.nursery()).add(0,.15,0);if(name.startsWith("nursery-final")){var queen=AppearanceScenario.clientAnt(client,QUEEN);require(queen!=null,"Living queen for nursery framing");focus=focus.add(queen.position().add(0,.3,0)).scale(.5);}client.player.lookAt(EntityAnchorArgument.Anchor.EYES,focus);AntRenderRecorder.armBrood(QUEEN,plan.nursery());});
        a.evidence.put("brood_capture_before",snapshot(w,plan));
        var png=c.takeScreenshot(TestScreenshotOptions.of(a.prefix+"-"+name).disableCounterPrefix().withDeltaTicks(1).withDestinationDir(a.dir));
        var bound=c.computeOnClient(client->AntRenderRecorder.finishCapture());
        var capture=new LinkedHashMap<String,Object>();capture.put("name",name);capture.put("image",png.toString());capture.put("camera",bound);capture.put("server_after",snapshot(w,plan));capture.put("capture_valid",Boolean.TRUE.equals(bound.get("clearance"))&&Boolean.TRUE.equals(bound.get("living_observer"))&&Boolean.TRUE.equals(bound.get("survival_observer"))&&!Boolean.TRUE.equals(bound.get("night_vision")));capture.put("acceptance","pending-manual-review");
        @SuppressWarnings("unchecked") var frames=(List<Object>)a.evidence.get("captures");frames.add(capture);a.save();
    }
    void transition(ClientGameTestContext c,TestSingleplayerContext w,UUID id){
        a.evidence.put("transition_subject",id.toString());a.evidence.put("transition_sample_policy","Every loaded server tick and actual client render extraction, no AI/velocity/phase writes");a.save();
        c.runOnClient(client->{AppearanceScenario.fixedView=null;AppearanceScenario.cameraSubject=id;client.options.fov().set(60);AppearanceScenario.cameraSide=2.2;AppearanceScenario.cameraForward=.5;AppearanceScenario.cameraHeight=1.25;AppearanceScenario.targetHeight=.24;AntRenderRecorder.startContinuous(id);});
        var serverRows=new ArrayList<Object>();a.evidence.put("transition_server_ticks",serverRows);
        var srv=w.getServer().computeOnServer(s->s);int begin=srv.getTickCount();
        var observedLevel=w.getServer().computeOnServer(server->w.getConnection().getServerLevel());
        var observing=new java.util.concurrent.atomic.AtomicBoolean(true);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s->{if(observing.get()&&s==srv&&s.getTickCount()-begin<1800){var l=observedLevel;if(l.getEntity(id) instanceof LasiusNigerEntity ant)serverRows.add(Map.of("tick",s.getTickCount(),"game_time",l.getGameTime(),"position",xyz(ant.position()),"alive",ant.isAlive(),"loaded_FULL",NestPlan.loaded(l,ant.blockPosition()),"on_ground",ant.onGround(),"phase",ant.workerTasks().phase().toString()));}});
        int stationary=0,moving=0,stop=-1,restart=-1;boolean hadMovement=false;
        try{for(int t=0;t<1500;t++){
            a.wait(c,w,1);
            var current=c.computeOnClient(client->AntRenderRecorder.latestContinuous());
            if(current==null)continue;
            boolean active=Boolean.TRUE.equals(current.get("moving"));
            if(active){moving++;if(moving>=4)hadMovement=true;if(stop>=0&&stationary>=8){restart=t;a.capture(c,w,id,"transition-restart-first");break;}}else{moving=0;if(hadMovement){if(stop<0){stop=t;a.capture(c,w,id,"transition-first-stop");}stationary++;if(stationary==5||stationary==12||stationary==30)a.capture(c,w,id,"transition-rest-"+stationary);}}
            if(t<12&&t%4==0)a.capture(c,w,id,"transition-start-"+t);
            if(t%100==0)a.save();
        }
        if(restart>=0)for(int t=1;t<=Math.max(60,120-restart);t++){a.wait(c,w,1);if(t==2||t==6||t==12)a.capture(c,w,id,"transition-restart-"+t);}
        }finally{observing.set(false);}
        c.runOnClient(client->{});
        a.evidence.put("transition_render_ticks",c.computeOnClient(client->AntRenderRecorder.finishContinuous()));a.evidence.put("transition_stop_offset",stop);a.evidence.put("transition_restart_offset",restart);a.evidence.put("stationary_samples",stationary);a.evidence.put("transition_end_server_tick",srv.getTickCount());a.evidence.put("transition_begin_server_tick",begin);a.save();
    }
}
