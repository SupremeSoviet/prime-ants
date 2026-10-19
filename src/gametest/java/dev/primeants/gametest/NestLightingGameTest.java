package dev.primeants.gametest;

import com.mojang.serialization.JsonOps;
import dev.primeants.PrimeAnts;
import dev.primeants.brood.*;
import dev.primeants.colony.*;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Real server ticks and production openings. Fixtures supply finite food, never completed digs or ownership. */
public final class NestLightingGameTest {
    private static final List<Block> LIGHTS=List.of(Blocks.TORCH,Blocks.WALL_TORCH,Blocks.SOUL_TORCH,
        Blocks.SOUL_WALL_TORCH,Blocks.LANTERN,Blocks.LANTERN,Blocks.SOUL_LANTERN,Blocks.SOUL_LANTERN);
    private static BlockState state(int variant,Direction facing){
        var s=LIGHTS.get(variant).defaultBlockState();
        if(s.getBlock() instanceof WallTorchBlock)s=s.setValue(WallTorchBlock.FACING,facing);
        if(s.getBlock() instanceof LanternBlock)s=s.setValue(LanternBlock.HANGING,variant==5||variant==7);
        return s;
    }
    private static String record(ServerLevel l,BlockPos p){
        var j=ColonyTerrain.CODEC.encodeStart(JsonOps.INSTANCE,ColonyTerrain.get(l)).getOrThrow().getAsJsonObject();
        var r=j.get(Long.toString(p.asLong()));return r==null?null:r.getAsString();
    }
    /** Actual disk readback of authority, then ordinary live controller ticks. Also reconstruct the live queen,
     * workers and nursery with normal serializers once per scenario; this is not a server-process restart. */
    private static void reload(GameTestHelper c,ServerLevel l){
        var before=ColonyTerrain.CODEC.encodeStart(JsonOps.INSTANCE,ColonyTerrain.get(l)).getOrThrow();
        l.getDataStorage().saveAndJoin();
        try(var disk=new SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),
                l.getServer().getWorldPath(LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){
            l.getDataStorage().set(ColonyTerrain.TYPE,Objects.requireNonNull(disk.get(ColonyTerrain.TYPE)));
            reloadData(l,disk,ColonyPlugs.TYPE);reloadData(l,disk,NestExpansion.TYPE);reloadData(l,disk,ChamberExcavation.TYPE);
            reloadData(l,disk,Mining.TYPE);reloadData(l,disk,EstablishedNest.TYPE);reloadData(l,disk,ChamberRegistry.TYPE);reloadData(l,disk,ColonyMembers.TYPE);
        }
        c.assertTrue(before.equals(ColonyTerrain.CODEC.encodeStart(JsonOps.INSTANCE,ColonyTerrain.get(l)).getOrThrow()),"Disk retains exact legacy terrain strings, including lit openings");
    }
    private static <T extends net.minecraft.world.level.saveddata.SavedData> void reloadData(ServerLevel l,SavedDataStorage disk,net.minecraft.world.level.saveddata.SavedDataType<T> type){
        var saved=disk.get(type);if(saved!=null)l.getDataStorage().set(type,saved);
    }
    private static void reconstruct(GameTestHelper c,LasiusNigerEntity q){
        var f=new WorkerForagingGameTest();
        for(var w:List.copyOf(f.workers(c,q)))f.restore(c,w);
        f.restore(c,q);
        var p=q.founding().plan();var l=c.getLevel();
        for(var pos:List.of(p.nursery(),p.cache())){
            var b=l.getBlockEntity(pos);var saved=b.saveWithFullMetadata(l.registryAccess());
            var restored=net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,l.getBlockState(pos),saved,l.registryAccess());
            c.assertTrue(restored!=null,"Normal block entity reconstruction");l.setBlockEntity(restored);
            c.assertTrue(saved.equals(restored.saveWithFullMetadata(l.registryAccess())),"Nursery/cache exact save round trip");
        }
    }
    private static final class Cycle {
        int variant;long placed=-1;BlockPos pos;BlockState light;String owned;boolean restored;
        boolean lit(){return placed>=0;}
        boolean tick(GameTestHelper c,ServerLevel l,LasiusNigerEntity q,List<BlockPos> cells,String context){
            var t=ColonyTerrain.get(l);var owner=q.getUUID();
            if(variant==LIGHTS.size())return true;
            if(!lit()){
                search: for(var p:cells)for(var d:Direction.Plane.HORIZONTAL){
                    var candidate=state(variant,d);
                    if(!t.opened(l,p,owner)||!NestLighting.supported(l,p,candidate)
                            ||l.getEntitiesOfClass(LasiusNigerEntity.class,new AABB(p),a->a.isAlive()).stream().anyMatch(a->
                                candidate.getCollisionShape(l,p).toAabbs().stream().anyMatch(box->box.move(p).intersects(a.getBoundingBox()))))continue;
                    pos=p;light=candidate;owned=record(l,p);placed=l.getGameTime();restored=false;
                    c.assertTrue(l.setBlock(p,light,3),"Supported dry light placement succeeds");
                    PrimeAnts.LOGGER.info("LIGHTING PLACE context={} variant={} owner={} pos={} state={} record={}",context,variant,owner,pos,light,owned);
                    break search;
                }
                if(!lit())return false;
            }
            c.assertTrue(l.getBlockState(pos).equals(light)&&t.openingIntact(l,pos,owner)&&owned.equals(record(l,pos)),"Same owner and exact light remain through real ticks: "+context+" "+variant);
            c.assertTrue(!t.openingIntact(l,pos,UUID.fromString("00000000-0000-0000-0000-000000000001")),"Lighting cannot reassign ownership");
            c.assertTrue(!t.eligible(l,pos,owner),"Lighting never grants excavation permission");
            c.assertTrue(q.founding().ready()&&q.founding().plan().nurseryProblem(l,owner,true)==null,"Lighting does not damage habitat or readiness: "+context+" "+q.founding().reason());
            var pile=(BroodPile)l.getBlockEntity(q.founding().plan().nursery());
            c.assertTrue(pile.stageEvaluation()==null||pile.stageEvaluation().stage()!=ColonyStage.FOUNDING,"Lighting never regresses an established colony to Founding");
            if(light.getBlock() instanceof LanternBlock)c.assertTrue(!NestPlan.traversable(l,pos)&&!NestPlan.walkable(l,pos),"Lantern keeps its actual collision and is excluded from walkable stands");
            if(l.getGameTime()-placed==60){
                reload(c,l);restored=true;
                var chunk=l.getChunkAt(pos);var serial=net.minecraft.world.level.chunk.storage.SerializableChunkData.copyOf(l,chunk);
                var read=net.minecraft.world.level.chunk.storage.SerializableChunkData.parse(l,l.palettedContainerFactory(),serial.write())
                    .read(l,l.getPoiManager(),new net.minecraft.world.level.chunk.storage.RegionStorageInfo("lighting",l.dimension(),"chunk"),chunk.getPos());
                c.assertTrue(read.getBlockState(pos).equals(light)&&ColonyTerrain.get(l).openingIntact(l,pos,owner),"Normal chunk serialization and disk authority agree while lit");
                if(context.equals("expansion")&&variant==0)reconstruct(c,q);
            }
            if(l.getGameTime()-placed<240)return false;
            c.assertTrue(restored,"Each lighting variant was read from disk before removal");
            l.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
            c.assertTrue(ColonyTerrain.get(l).opened(l,pos,owner)&&owned.equals(record(l,pos))&&NestPlan.traversable(l,pos),"Removal restores original usable owned opening without excavation");
            PrimeAnts.LOGGER.info("LIGHTING REMOVE context={} variant={} owner={} elapsed={} diskReload=true originalRecord={} usable=true",context,variant,owner,l.getGameTime()-placed,owned);
            variant++;placed=-1;return variant==LIGHTS.size();
        }
    }
    @GameTest(maxTicks=30000,structure="prime_ants_test:idle_ground")
    public void workerExpansionLightsSurviveDiskReloadPhysicalNursingAndBroodProgress(GameTestHelper c){
        var f=new WorkerForagingGameTest();var food=new NursingGameTest();var audit=new ExpansionGameTest();
        LasiusNigerEntity[] q={f.start(c)};boolean[] supplied={false},fed={false},carried={false},progress={false};
        var cycle=new Cycle();var brood=new HashMap<UUID,Long>();long[] given={-1};int[] nurses={0};
        c.onEachTick(()->{
            var l=c.getLevel();q[0]=(LasiusNigerEntity)l.getEntity(q[0].getUUID());var queen=q[0];
            if(!supplied[0]&&queen.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;food.supply(c,queen,10,8);}
            if(supplied[0])c.assertTrue(food.total(c,queen)==18,"Finite ten apples/eight chicken conserved through lighting and reload");
            audit.balance(c,queen);var job=NestExpansion.get(l).job(queen.getUUID());if(job==null||!job.complete())return;
            c.assertTrue(job.removed()==12&&job.deposited==12&&job.released==0,"Lighting/removal never repeats excavation or creates material");
            if(cycle.lit()){
                var ws=f.workers(c,queen);long current=ws.stream().mapToLong(w->w.nutrition().givenSugar()).sum();if(given[0]<0)given[0]=current;if(current>given[0])fed[0]=true;
                for(var w:ws)if(w.workerTasks().nursing()){
                    if(WorkerTasks.food(w.getMainHandItem()))carried[0]=true;
                    if(w.workerTasks().feedingTicks()>0){nurses[0]++;c.assertTrue(w.workerTasks().nursingAuthorized(l),"Physical feed ticks remain habitat-authorized while lit");}
                }
                var pile=food.pile(c,queen);for(var r:pile.records()){
                    long value=r.stage().ordinal()*100000L+r.progress();var prior=brood.put(r.id(),value);
                    if(prior!=null&&value>prior)progress[0]=true;
                }
            }
            if(cycle.tick(c,l,queen,job.completed(),"expansion")){
                c.assertTrue(fed[0]&&carried[0]&&nurses[0]>0&&progress[0],"Actual nurse cargo, physical feeding, transferred sugar and identified brood progression observed while lit");
                PrimeAnts.LOGGER.info("LIGHTING ACCEPT expansion variants=8 physicalFeedTicks={} transferredSugar={} broodProgress={} removed={} deposited={}",nurses[0],fed[0],progress[0],job.removed(),job.deposited);c.succeed();
            }
        });
    }
    @GameTest(maxTicks=48000,structure="prime_ants_test:idle_ground")
    public void completedChamberLightsPreserveBothIntegrityScansAndSettledSoil(GameTestHelper c){
        var fx=new NestPlanFixture();var q=fx.start(c);boolean[] supplied={false};var cycle=new Cycle();
        boolean[] markerChecked={false};long[] markerSince={-1};
        c.onEachTick(()->{
            fx.grow(c,q,supplied);fx.soil(c,q);var l=c.getLevel();var j=NestPlanFixture.job(c,q,ChamberExcavation.STORE);
            if(j==null)return;
            if(!markerChecked[0]&&j.complete()&&!j.established(l,q.getUUID())){
                var marker=j.built.marker();
                if(markerSince[0]<0){
                    if(j.pendingMarker(l,q.getUUID())==null)return;
                    c.assertTrue(ColonyTerrain.get(l).opened(l,marker,q.getUUID()),"Pending store marker is an actual worker-owned opening");
                    markerSince[0]=l.getGameTime();l.setBlock(marker,Blocks.TORCH.defaultBlockState(),3);
                }
                c.assertTrue(l.getBlockState(marker).is(Blocks.TORCH)&&j.problem(l,q.getUUID())==null&&q.founding().ready(),"Worker retains light in owned pending marker without damaging habitat or replacing it with a store");
                if(l.getGameTime()-markerSince[0]==60)reload(c,l);
                if(l.getGameTime()-markerSince[0]<240)return;
                l.setBlock(marker,Blocks.AIR.defaultBlockState(),3);markerChecked[0]=true;
                PrimeAnts.LOGGER.info("LIGHTING ACCEPT pendingMarker owner={} retainedTicks=240 noReplacement=true originalOpening=true",q.getUUID());
            }
            if(!j.established(l,q.getUUID()))return;
            var registered=ChamberRegistry.get(l).colony(q.getUUID()).chamber(ChamberExcavation.STORE);
            if(registered==null)return;
            var room=ChamberExcavation.roomFindings(l,q.getUUID(),q.founding().plan(),registered.min(),registered.max());
            c.assertTrue(j.problem(l,q.getUUID())==null&&room.verdict()==Findings.Verdict.CLEAR,"Finished job and registered chamber scan both stay clear with lighting");
            if(cycle.tick(c,l,q,j.completed().stream().filter(p->!p.equals(j.built.marker())).toList(),"chamber")){
                c.assertTrue(markerChecked[0],"Real pending-marker lighting observed before normal store establishment");
                PrimeAnts.LOGGER.info("LIGHTING ACCEPT chamber variants=8 removed={} deposited={} released={}",j.removed(),j.deposited,j.released);c.succeed();
            }
        });
    }
    @GameTest(maxTicks=60000,structure="prime_ants_test:idle_ground")
    public void miningGalleryLightsPreserveCompletedOpeningsAndResourceBalance(GameTestHelper c){
        var player=new TierTwoFixture(40,true);var q=player.fx.start(c);boolean[] geology={false};var cycle=new Cycle();
        c.onEachTick(()->{
            var l=c.getLevel();if(q.founding().plan()!=null&&!geology[0]){MiningGameTest.geology(c,q);geology[0]=true;}
            MiningGameTest.feedMining(c,player,q);player.ledgers(c,q);player.trace(c,q,"LIGHTING_MINING");
            c.assertTrue(player.apples<=2048&&player.chickens<=1024&&player.clay<=56,"Declared finite lighting-fixture supply ceiling, no autonomous-material claim");
            var j=Mining.get(l).job(q.getUUID());if(j==null||!j.complete()||j.claim!=null)return;
            c.assertTrue(j.removed()==16&&j.deposited==16&&j.released==0&&j.problem(l,q.getUUID())==null,"Sixteen genuine gallery edits remain settled, no extra extraction or damage");
            for(var item:MiningGameTest.MATERIALS){var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();
                c.assertTrue(j.deliveries().getOrDefault(id,0)==j.produced(id),"Every original resource still delivered exactly once: "+id);}
            if(cycle.tick(c,l,q,j.completed(),"mining")){
                PrimeAnts.LOGGER.info("LIGHTING ACCEPT mining variants=8 removed={} deliveries={}",j.removed(),j.deliveries());c.succeed();
            }
        });
    }
    @GameTest(maxTicks=5000)
    public void generatedNestLightsPreserveAuthorityStageAndActivityAcrossDiskReadback(GameTestHelper c){
        var l=NaturalPlacementGameTest.level(c,"placement_soil");var cp=NaturalPlacementGameTest.prepare(c,l,8100,ColonyStage.YOUNG);var cycle=new Cycle();
        var start=new HashMap<UUID,Vec3>();boolean[] moved={false};int[] ready={0};
        c.onEachTick(()->{
            if(!NaturalPlacementGameTest.status(l,cp).equals("PLACED"))return;
            var q=EstablishedGenerationGameTest.queen(l,cp);
            if(cycle.variant==0&&!cycle.lit()){
                ready[0]=q.founding().ready()?ready[0]+1:0;
                if(ready[0]<200)return;
                PrimeAnts.LOGGER.info("LIGHTING BASELINE generated unlitReadyTicks={} owner={}",ready[0],q.getUUID());
            }
            for(var member:ColonyMembers.get(l).members(q.getUUID()))if(l.getEntity(member.worker()) instanceof LasiusNigerEntity w){
                var before=start.putIfAbsent(w.getUUID(),w.position());if(cycle.lit()&&before!=null&&w.position().distanceToSqr(before)>.25)moved[0]=true;
            }
            var initial=EstablishedNest.get(l).initial(q.getUUID());
            var home=q.founding().plan();var room=new ArrayList<BlockPos>();
            for(int f=3;f<=5;f++)for(int s=-1;s<=1;s++)for(int y=-2;y<=-1;y++)if(initial.space().contains(home.at(f,s,y)))room.add(home.at(f,s,y));
            if(cycle.tick(c,l,q,room,"generated")){
                c.assertTrue(moved[0],"Canonical generated workers actually move with lights present");
                PrimeAnts.LOGGER.info("LIGHTING ACCEPT generated variants=8 workerMovement={} owner={}",moved[0],q.getUUID());c.succeed();
            }
        });
    }
    @GameTest(maxTicks=20000,structure="prime_ants_test:idle_ground")
    public void openedEntranceTorchesRetainPlugAuthorityAndNeverRepairSolidRevocation(GameTestHelper c){
        var f=new WorkerForagingGameTest();var q=f.start(c);int[] variant={0};long[] since={-1};BlockPos[] at={null};BlockState[] torch={null};String[] authority={null};boolean[] ended={false};
        c.onEachTick(()->{
            var l=c.getLevel();if(ended[0]||q.founding().lifecycle()!=QueenFounding.Lifecycle.OPEN)return;
            var p=q.founding().plan();var plugs=ColonyPlugs.get(l);
            if(since[0]<0){
                at[0]=p.plugs().getFirst();
                search: for(var pos:p.plugs())for(var d:Direction.Plane.HORIZONTAL){
                    var candidate=state(variant[0],d);if(!NestLighting.supported(l,pos,candidate))continue;
                    at[0]=pos;torch[0]=candidate;break search;
                }
                c.assertTrue(torch[0]!=null&&plugs.opened(l,at[0],q.getUUID()),"Already opened entrance supports the torch variant");
                authority[0]=ColonyPlugs.CODEC.encodeStart(JsonOps.INSTANCE,plugs).getOrThrow().getAsJsonObject().get(Long.toString(at[0].asLong())).getAsString();
                l.setBlock(at[0],torch[0],3);since[0]=l.getGameTime();
            }
            c.assertTrue(l.getBlockState(at[0]).equals(torch[0])&&ColonyPlugs.get(l).opened(l,at[0],q.getUUID())&&q.founding().ready()&&p.nurseryProblem(l,q.getUUID(),true)==null,"Supported entrance torch preserves same ownership and actual dry traversal");
            if(l.getGameTime()-since[0]==60){
                reload(c,l);
                var support=torch[0].getBlock() instanceof WallTorchBlock?at[0].relative(torch[0].getValue(WallTorchBlock.FACING).getOpposite()):at[0].below();
                try(var scope=UnavailableCells.hide(c,l,support)){
                    var occupancy=new Findings();q.founding().occupancyFindings(l,occupancy);
                    c.assertTrue(p.nurseryFindings(l,q.getUUID(),true).verdict()==Findings.Verdict.UNKNOWN&&occupancy.verdict()==Findings.Verdict.UNKNOWN,"Unavailable entrance-light support stays UNKNOWN in habitat and queen occupancy");
                }
                c.assertTrue(ColonyPlugs.get(l).opened(l,at[0],q.getUUID()),"Support knowledge returns without a new opening or ownership assignment");
            }
            if(l.getGameTime()-since[0]<240)return;
            l.setBlock(at[0],Blocks.AIR.defaultBlockState(),3);
            c.assertTrue(ColonyPlugs.get(l).opened(l,at[0],q.getUUID())&&authority[0].equals(ColonyPlugs.CODEC.encodeStart(JsonOps.INSTANCE,ColonyPlugs.get(l)).getOrThrow().getAsJsonObject().get(Long.toString(at[0].asLong())).getAsString()),"Removing entrance torch retains exact original legacy plug record");
            PrimeAnts.LOGGER.info("LIGHTING ACCEPT entrance variant={} originalRecord={} loadedTicks=240",variant[0],authority[0]);
            variant[0]++;since[0]=-1;torch[0]=null;
            if(variant[0]==4){
                ended[0]=true;l.setBlock(at[0],Blocks.STONE.defaultBlockState(),3);l.setBlock(at[0],Blocks.AIR.defaultBlockState(),3);l.setBlock(at[0],Blocks.TORCH.defaultBlockState(),3);reload(c,l);
                c.assertTrue(!ColonyPlugs.get(l).opened(l,at[0],q.getUUID()),"Entrance lighting cannot restore revoked plug ownership");
                c.runAfterDelay(240,()->{c.assertTrue(!q.founding().ready()&&!ColonyPlugs.get(l).opened(l,at[0],q.getUUID()),"Solid-revoked entrance stays damaged through real ticks");c.succeed();});
            }
        });
    }
    @GameTest(maxTicks=3000)
    public void lanternCollisionRequiresActualAntDetourThroughAlternateOwnedRoute(GameTestHelper c){
        var l=NaturalPlacementGameTest.level(c,"placement_soil");var cp=NaturalPlacementGameTest.prepare(c,l,8200,ColonyStage.MATURE);
        LasiusNigerEntity[] walker={null};BlockPos[] light={null};Vec3[] dest={null},start={null};boolean[] detour={false};int[] ticks={0},leg={0};
        var waypoints=new ArrayList<Vec3>();
        c.onEachTick(()->{
            if(!NaturalPlacementGameTest.status(l,cp).equals("PLACED"))return;
            var q=EstablishedGenerationGameTest.queen(l,cp);var p=q.founding().plan();
            if(walker[0]==null){
                // Isolate path physics from 25 unrelated bodies pushing across the same small room. These original
                // actors are parked outside with AI disabled; activity is tested separately by the growing/generated cases.
                var parked=new ArrayList<LasiusNigerEntity>();parked.add(q);
                for(var member:ColonyMembers.get(l).members(q.getUUID()))if(l.getEntity(member.worker()) instanceof LasiusNigerEntity a)parked.add(a);
                for(var a:parked){a.setNoAi(true);a.teleportTo(p.entrance().getX()+24.5,p.entrance().getY()+3,p.entrance().getZ()+24.5);}
                PrimeAnts.LOGGER.info("LIGHTING DETOUR fixture parkedActors={} navigationActorSeparate=true originalTerrain=true",parked.size());
                // A separate navigation actor makes no colony, dig, nursing or population claim.
                light[0]=p.at(4,2,-2);var state=Blocks.LANTERN.defaultBlockState();
                c.assertTrue(ColonyTerrain.get(l).opened(l,light[0],q.getUUID())&&NestLighting.supported(l,light[0],state),"Generated widening owns supported lantern cell");
                l.setBlock(light[0],state,3);start[0]=Vec3.atBottomCenterOf(p.at(3,2,-2));dest[0]=Vec3.atBottomCenterOf(p.at(5,2,-2));
                for(var at:List.of(p.at(3,3,-2),p.at(5,3,-2),p.at(5,2,-2))){
                    c.assertTrue(NestPlan.walkable(l,at)&&ColonyTerrain.get(l).opened(l,at,q.getUUID()),"Alternate route has actual supported owned walking cells");waypoints.add(Vec3.atBottomCenterOf(at));
                }
                var w=AntEntities.WORKER.create(l,net.minecraft.world.entity.EntitySpawnReason.COMMAND);w.snapTo(start[0].x,start[0].y,start[0].z,0,0);l.addFreshEntity(w);walker[0]=w;
            }
            var w=walker[0];ticks[0]++;
            c.assertTrue(!w.isNoAi()&&l.noCollision(w,w.getBoundingBox().deflate(.001)),"Moving ant never intersects the lantern's real collision");
            c.assertTrue(w.getY()<=start[0].y+.05,"Navigation detours on the original floor rather than stepping over the lantern");
            if(Math.abs(w.position().subtract(start[0]).dot(new Vec3(p.direction().getClockWise().getStepX(),0,p.direction().getClockWise().getStepZ())))>.6)detour[0]=true;
            if(leg[0]<waypoints.size()&&w.position().distanceToSqr(waypoints.get(leg[0]))<.15)leg[0]++;
            if(ticks[0]>=3&&w.onGround()&&leg[0]<waypoints.size()&&(ticks[0]%10==3||w.getNavigation().isDone())){
                var to=waypoints.get(leg[0]);boolean path=w.getNavigation().moveTo(to.x,to.y,to.z,0,1);
                PrimeAnts.LOGGER.info("LIGHTING DETOUR pathRequest tick={} leg={} pos={} onGround={} destination={} returned={} path={}",ticks[0],leg[0],w.position(),w.onGround(),to,path,w.getNavigation().getPath());
                c.assertTrue(path,"Requested alternate-route waypoint has a physical ground path");
            }
            if(leg[0]==waypoints.size()&&detour[0]&&ticks[0]>10){
                c.assertTrue(l.getBlockState(light[0]).is(Blocks.LANTERN)&&ColonyTerrain.get(l).openingIntact(l,light[0],q.getUUID()),"Ant leaves lantern and original authority intact");
                PrimeAnts.LOGGER.info("LIGHTING ACCEPT detour worker={} start={} end={} ticks={} lateralDetour={} collisionFree=true requestedWaypoints={}",w.getUUID(),start[0],w.position(),ticks[0],detour[0],waypoints);w.discard();c.succeed();
            }
        });
    }
    @GameTest(maxTicks=3000)
    public void lightingNeverRestoresRevokedUnknownPendingWetForeignOrUnavailableAuthority(GameTestHelper c){
        var l=NaturalPlacementGameTest.level(c,"placement_soil");var cp=NaturalPlacementGameTest.prepare(c,l,8300,ColonyStage.MATURE);boolean[] checked={false};
        c.onEachTick(()->{
            if(checked[0]||!NaturalPlacementGameTest.status(l,cp).equals("PLACED"))return;checked[0]=true;
            var q=EstablishedGenerationGameTest.queen(l,cp);var p=q.founding().plan();var owner=q.getUUID();var at=p.at(5,3,-2);var light=Blocks.TORCH.defaultBlockState();
            var control=p.at(4,2,-2);l.setBlock(control,light,3);
            try(var scope=UnavailableCells.hide(c,l,control.below())){
                var own=new Findings();EstablishedNest.get(l).findings(l,owner,own);
                c.assertTrue(own.verdict()==Findings.Verdict.UNKNOWN&&p.nurseryFindings(l,owner,true).verdict()==Findings.Verdict.UNKNOWN,"Unavailable light support is UNKNOWN, not revoked ownership or structural damage");
                var stranger=UUID.fromString("00000000-0000-0000-0000-000000000001");
                c.assertTrue(ColonyTerrain.get(l).openingLoaded(l,control,stranger)&&!ColonyTerrain.get(l).openingIntact(l,control,stranger),"Unavailable support cannot hide known foreign opening ownership");
                var wall=p.at(4,4,-2);var original=l.getBlockState(wall);l.setBlock(wall,Blocks.AIR.defaultBlockState(),3);
                c.assertTrue(p.nurseryFindings(l,owner,true).verdict()==Findings.Verdict.DAMAGED,"Loaded broken shell still beats unavailable light support");l.setBlock(wall,original,3);
            }
            c.assertTrue(ColonyTerrain.get(l).openingIntact(l,control,owner)&&p.nurseryFindings(l,owner,true).verdict()==Findings.Verdict.CLEAR,"Support availability restores knowledge without changing opening ownership");
            l.setBlock(control,Blocks.AIR.defaultBlockState(),3);
            c.assertTrue(ColonyTerrain.get(l).opened(l,at,owner),"Positive original generated opening");
            l.setBlock(at,Blocks.STONE.defaultBlockState(),3);
            c.assertTrue(p.nurseryFindings(l,owner,true).verdict()==Findings.Verdict.DAMAGED&&record(l,at)==null,"Solid player edit revokes authority and causes structural damage");
            l.setBlock(at,Blocks.AIR.defaultBlockState(),3);l.setBlock(at,light,3);reload(c,l);
            c.assertTrue(!ColonyTerrain.get(l).openingIntact(l,at,owner)&&p.nurseryFindings(l,owner,true).verdict()==Findings.Verdict.DAMAGED,"Removing solid and relighting never recovers revoked ownership, even on disk");
            var unknown=p.at(-4,3,1);l.setBlock(unknown,light,3);
            c.assertTrue(NestLighting.supported(l,unknown,light)&&!ColonyTerrain.get(l).openingIntact(l,unknown,owner)&&!ColonyTerrain.get(l).eligible(l,unknown,owner),"Supported light in unknown air grants neither ownership nor removal");
            var pending=p.at(16,0,-2);l.setBlock(pending,light,3);
            c.assertTrue(!ColonyTerrain.get(l).openingIntact(l,pending,owner)&&!ColonyTerrain.get(l).eligible(l,pending,owner),"Light in undug terrain grants no replacement or excavation permission");
            var wet=p.at(3,3,-2);var waterlogged=Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.WATERLOGGED,true);
            l.setBlock(wet,waterlogged,2);c.assertTrue(!NestLighting.supported(l,wet,waterlogged)&&!ColonyTerrain.get(l).openingIntact(l,wet,owner),"Wet lantern revokes opening authority");
            var foreign=p.cache();var cache=(NestCache)l.getBlockEntity(foreign);var original=cache.saveWithFullMetadata(l.registryAccess());
            var tag=original.copy();tag.putString("Colony",UUID.fromString("00000000-0000-0000-0000-000000000001").toString());
            var stranger=net.minecraft.world.level.block.entity.BlockEntity.loadStatic(foreign,l.getBlockState(foreign),tag,l.registryAccess());l.setBlockEntity(stranger);
            c.assertTrue(!((NestCache)stranger).ownedBy(owner,p)&&p.nurseryFindings(l,owner,true).verdict()==Findings.Verdict.DAMAGED,"Lighting does not mask a foreign component");
            l.setBlock(foreign,light,3);c.assertTrue(!ColonyTerrain.get(l).openingIntact(l,foreign,owner),"Replacing a component never authorizes a light");
            var hidden=p.at(4,3,-2);l.setBlock(hidden,light,3);
            c.assertTrue(ColonyTerrain.get(l).openingIntact(l,hidden,owner),"Owned lighting positive availability control");
            try(var scope=UnavailableCells.hide(c,l,hidden)){
                c.assertTrue(!ColonyTerrain.get(l).openingIntact(l,hidden,owner),"Unavailable lit cell remains unknown");
            }
            var shell=p.at(4,4,-2);l.setBlock(shell,Blocks.AIR.defaultBlockState(),3);
            var findings=new Findings();EstablishedNest.get(l).findings(l,owner,findings);
            c.assertTrue(findings.verdict()==Findings.Verdict.DAMAGED,"Lighting cannot hide a broken shell");
            c.runAfterDelay(240,()->{
                reload(c,l);c.assertTrue(!ColonyTerrain.get(l).openingIntact(l,at,owner)&&!ColonyTerrain.get(l).eligible(l,pending,owner)
                    &&l.getBlockState(pending).equals(light),"Real ticks and another reload never excavate/replace the unowned light or restore legacy damage");
                PrimeAnts.LOGGER.info("LIGHTING ACCEPT negatives solidRevoked=true legacyRecovery=false unknown=true pending=true wet=true foreign=true unavailable=true shell=true");c.succeed();
            });
        });
    }
    @GameTest(maxTicks=30000,structure="prime_ants_test:idle_ground")
    public void lightInActualPendingExpansionTargetNeverAuthorizesRemovalOrReplacement(GameTestHelper c){
        var f=new WorkerForagingGameTest();var food=new NursingGameTest();var q=f.start(c);boolean[] supplied={false},placed={false};
        c.onEachTick(()->{
            var l=c.getLevel();if(!supplied[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;food.supply(c,q,6,8);}
            if(supplied[0])c.assertTrue(food.total(c,q)==14,"Fourteen declared units remain conserved");
            var j=NestExpansion.get(l).job(q.getUUID());if(placed[0]||j==null||j.removed()<2||j.removed()%2!=0||j.removed()>=j.tasks.size())return;
            placed[0]=true;var target=j.tasks.get(j.removed());var light=Blocks.TORCH.defaultBlockState();int removed=j.removed();
            c.assertTrue(ColonyTerrain.get(l).compatible(l,target,q.getUUID(),j.expected.get(removed))&&NestLighting.supported(l,target,light),"Actual next pending excavation cell with dry torch support");
            l.setBlock(target,light,3);reload(c,l);
            c.assertTrue(!ColonyTerrain.get(l).openingIntact(l,target,q.getUUID())&&!ColonyTerrain.get(l).eligible(l,target,q.getUUID())&&j.problem(l,q.getUUID())!=null,"Pending light revokes target authority; it does not become a successful dig");
            c.runAfterDelay(240,()->{
                var current=NestExpansion.get(l).job(q.getUUID());c.assertTrue(current.removed()==removed&&l.getBlockState(target).equals(light),"Actual builders leave the pending light untouched, with no excavation or duplicate cargo");
                l.setBlock(target,Blocks.AIR.defaultBlockState(),3);l.setBlock(target,light,3);reload(c,l);
                c.assertTrue(!ColonyTerrain.get(l).openingIntact(l,target,q.getUUID()),"Removing and replacing pending light cannot claim it");
                PrimeAnts.LOGGER.info("LIGHTING ACCEPT actualPending target={} removed={} noReplacement=true noAuthority=true",target,removed);c.succeed();
            });
        });
    }
}
