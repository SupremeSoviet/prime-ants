package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import dev.primeants.brood.*;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/** Real server ticks and authorized egg. Negative interventions never supply ants or shortcut development. */
public final class BroodGameTest {
    private LasiusNigerEntity start(GameTestHelper c) {
        var fixture = new QueenFoundingGameTest(); fixture.terrain(c, Blocks.DIRT.defaultBlockState(), true); return fixture.egg(c);
    }
    private BroodPile pile(GameTestHelper c, LasiusNigerEntity queen) {
        var f = queen.founding();
        c.assertTrue(f.phase() != QueenFounding.Phase.FAILED, "Production founding failed: " + f.reason());
        return f.plan() != null && c.getLevel().getBlockEntity(f.plan().nursery()) instanceof BroodPile p ? p : null;
    }
    private List<LasiusNigerEntity> workers(GameTestHelper c) {
        return c.getLevel().getEntitiesOfClass(LasiusNigerEntity.class, c.getBounds()).stream().filter(a -> a.form() == AntForm.WORKER && a.isAlive()).toList();
    }
    private boolean stage(BroodPile p, BroodStage s) { return p != null && !p.records().isEmpty() && p.records().stream().allMatch(r -> r.stage() == s); }
    private List<String> state(BroodPile p) {
        return p.records().stream().map(r -> r.id()+"/"+r.queenId()+"/"+r.slot()+"/"+r.stage()+"/"+r.progress()+"/"+r.nourishment()).toList();
    }
    private LasiusNigerEntity restoreAnt(GameTestHelper c, LasiusNigerEntity ant) {
        var out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, c.getLevel().registryAccess());
        c.assertTrue(ant.save(out), "Normal entity serialization"); var tag = out.buildResult(); UUID id = ant.getUUID(); ant.discard();
        var restored = (LasiusNigerEntity)EntityType.loadEntityRecursive(TagValueInput.create(ProblemReporter.DISCARDING, c.getLevel().registryAccess(), tag), c.getLevel(), EntitySpawnReason.LOAD, e -> e);
        c.assertTrue(restored != null && restored.getUUID().equals(id) && c.getLevel().tryAddFreshEntityWithPassengers(restored), "Restore one identity through vanilla UUID insertion");
        return restored;
    }
    private BroodPile restorePile(GameTestHelper c, BroodPile p, CompoundTag tag) {
        var pos = p.getBlockPos(); var block = p.getBlockState(); c.getLevel().removeBlockEntity(pos);
        var restored = (BroodPile)BlockEntity.loadStatic(pos, block, tag, c.getLevel().registryAccess());
        c.assertTrue(restored != null, "Normal block entity deserialization"); c.getLevel().setBlockEntity(restored); return restored;
    }

    @GameTest(maxTicks=30000, structure="prime_ants_test:idle_ground")
    public void floorAndWallTorchesKeepGenuineBroodDeveloping(GameTestHelper c) {
        var q=start(c);boolean[] lit={false};
        c.onEachTick(()->{
            var p=pile(c,q);if(lit[0] || !stage(p,BroodStage.LARVA) || p.records().getFirst().progress()<8)return;
            lit[0]=true;var plan=q.founding().plan();var floor=plan.at(3,1,-2);var wall=plan.at(5,-1,-1);
            var direction=plan.direction().getClockWise();
            var wallState=Blocks.WALL_TORCH.defaultBlockState().setValue(net.minecraft.world.level.block.WallTorchBlock.FACING,direction);
            c.assertTrue(Blocks.TORCH.defaultBlockState().canSurvive(c.getLevel(),floor)&&wallState.canSurvive(c.getLevel(),wall),"Torches have actual existing floor/wall supports");
            c.assertTrue(c.getLevel().setBlock(floor,Blocks.TORCH.defaultBlockState(),3)&&c.getLevel().setBlock(wall,wallState,3),"Player lighting intervention with neighbor updates");
            var ids=p.records().stream().map(BroodRecord::id).collect(java.util.stream.Collectors.toSet());
            long reserve=q.bodyReserve();
            c.assertTrue(NestPlan.walkable(c.getLevel(),floor)&&NestPlan.traversable(c.getLevel(),wall),"Shared supported traversal accepts dry collision-free torches");
            c.runAfterDelay(80,()->{
                c.assertTrue(c.getLevel().getBlockState(floor).is(Blocks.TORCH)&&c.getLevel().getBlockState(wall).is(Blocks.WALL_TORCH),"Lighting persists on actual supports");
                c.assertTrue(q.founding().sealed()&&p.records().stream().allMatch(r->r.stage()==BroodStage.LARVA&&r.progress()>=88&&r.nourishment()>=8800)&&q.bodyReserve()<reserve,"Normal paid care and development continue with lighting");
                c.runAfterDelay(180,()->{
                    c.assertTrue(c.getLevel().getBlockState(floor).is(Blocks.TORCH)&&c.getLevel().getBlockState(wall).is(Blocks.WALL_TORCH),"No automatic torch removal after emergence");
                    c.assertTrue(workers(c).size()==3&&p.records().isEmpty()&&p.consumed().equals(ids)&&workers(c).stream().allMatch(w->ids.contains(w.broodId()))&&q.bodyReserve()==0,"Exactly three genuine emerged lineages and all original costs");
                    PrimeAnts.LOGGER.info("T27 TORCH EMERGENCE queen={} brood={} workers={} duration={} reserve={}",q.getUUID(),ids,workers(c).stream().map(LasiusNigerEntity::getUUID).toList(),p.stageDuration(),q.bodyReserve());c.succeed();
                });
            });
        });
    }

    @GameTest(maxTicks=30000, structure="prime_ants_test:idle_ground")
    public void solidChamberObstacleStillStallsGenuineBrood(GameTestHelper c) { chamberBlocker(c,Blocks.STONE.defaultBlockState()); }
    @GameTest(maxTicks=30000, structure="prime_ants_test:idle_ground")
    public void chamberFluidStillStallsGenuineBrood(GameTestHelper c) { chamberBlocker(c,Blocks.WATER.defaultBlockState()); }
    @GameTest(maxTicks=30000, structure="prime_ants_test:idle_ground")
    public void collisionFreeForeignComponentsStillRequireCanonicalOwnership(GameTestHelper c) { chamberBlocker(c,NurseryBlocks.NEST_CACHE.defaultBlockState()); }
    private void chamberBlocker(GameTestHelper c,net.minecraft.world.level.block.state.BlockState block) {
        var q=start(c);boolean[] blocked={false};
        c.onEachTick(()->{
            var p=pile(c,q);if(blocked[0]||!stage(p,BroodStage.LARVA)||p.records().getFirst().progress()<8)return;
            blocked[0]=true;var pos=q.founding().plan().at(3,1,-2);var before=state(p);long reserve=q.bodyReserve();
            c.assertTrue(c.getLevel().setBlock(pos,block,3)&&!NestPlan.traversable(c.getLevel(),pos),"Actual obstruction/fluid/foreign block entity rejected by shared traversal");
            c.runAfterDelay(60,()->{
                c.assertTrue(before.equals(state(p))&&q.bodyReserve()==reserve&&!q.founding().sealed()&&workers(c).isEmpty()&&q.founding().plan().nurseryProblem(c.getLevel(),q.getUUID())!=null,"Negative habitat keeps genuine care and development stalled");
                PrimeAnts.LOGGER.info("T27 NEGATIVE GUARD block={} condition={} brood={}",block,p.condition(),state(p));c.succeed();
            });
        });
    }
    @GameTest(maxTicks=65000, structure="prime_ants_test:idle_ground")
    public void authorizedQueenRaisesIdentifiedFirstClutchThroughEveryStage(GameTestHelper c) {
        var queen = start(c); Set<BroodStage> seen = EnumSet.noneOf(BroodStage.class); Set<UUID> ids = new HashSet<>(); boolean[] finished = {false};
        c.onEachTick(() -> {
            var p = pile(c, queen); if (p == null) { c.assertTrue(workers(c).isEmpty(), "No adult before nursery"); return; }
            c.assertTrue(queen.founding().sealed(), "Owned nursery contents must retain live founding readiness");
            for (var r : p.records()) { seen.add(r.stage()); ids.add(r.id()); c.assertTrue(r.queenId().equals(queen.getUUID()), "Brood lineage"); }
            if (!seen.contains(BroodStage.COCOON)) c.assertTrue(workers(c).isEmpty(), "No worker before cocoon emergence");
            long spent = BroodPile.CAPACITY * BroodPile.EGG_COST + p.records().stream().mapToLong(BroodRecord::nourishment).sum() + p.consumed().size()*BroodPile.LARVA_COST;
            c.assertTrue(queen.bodyReserve() == BroodPile.MAX_RESERVE - spent, "Acceleration cannot skip a single care cost");
            c.assertTrue(p.records().size()+workers(c).size()==3, "One brood owner becomes exactly one live entity");
            if (!finished[0] && workers(c).size()==3) {
                finished[0]=true;
                c.assertTrue(seen.containsAll(List.of(BroodStage.EGG, BroodStage.LARVA, BroodStage.COCOON)) && ids.size()==3 && p.records().isEmpty() && p.consumed().equals(ids), "Three visible identities traverse all stages and are consumed");
                c.assertTrue(queen.bodyReserve()==0 && workers(c).stream().allMatch(w -> ids.contains(w.broodId()) && queen.getUUID().equals(w.queenId()) && w.isCallow() && !w.noPhysics && !w.isNoAi()), "Pale entities, lineage, physics and exact finite reserve exhaustion");
                PrimeAnts.LOGGER.info("T07 FIRST CLUTCH queen={} brood={} workers={} pileTicks={} reserve={} stageDuration={}", queen.getUUID(), ids, workers(c).stream().map(LasiusNigerEntity::getUUID).toList(), p.loadedTicks(), queen.bodyReserve(), p.stageDuration()); c.succeed();
            }
        });
    }
    @GameTest(maxTicks=30000, structure="prime_ants_test:idle_ground")
    public void caregiverAbsenceStallsDependentLarvae(GameTestHelper c) {
        var queen=start(c); boolean[] displaced={false};
        c.onEachTick(() -> {
            var p=pile(c,queen);
            if (!displaced[0] && stage(p,BroodStage.LARVA) && p.records().getFirst().progress()>8) {
                displaced[0]=true; var states=state(p); long reserve=queen.bodyReserve(); Vec3 outside=Vec3.atBottomCenterOf(queen.founding().plan().outside());
                queen.teleportTo(outside.x,outside.y,outside.z); // negative intervention after genuine physical founding
                c.runAfterDelay(180,()->{
                    c.assertTrue(!queen.founding().sealed() && states.equals(state(p)) && queen.bodyReserve()==reserve && workers(c).isEmpty(), "Out-of-reach absent caregiver cannot advance/nourish larvae");
                    queen.hurtServer(c.getLevel(),queen.damageSources().generic(),1000);
                    c.runAfterDelay(180,()->{c.assertTrue(states.equals(state(p)) && workers(c).isEmpty() && p.records().size()==3, "Death preserves dependent larvae but supplies no care or eggs");c.succeed();});
                });
            }
        });
    }
    @GameTest(maxTicks=30000, structure="prime_ants_test:idle_ground")
    public void finiteProductionReserveStallsUnsupportedLarvae(GameTestHelper c) {
        String old=System.getProperty("prime_ants.queenInitialReserve"); LasiusNigerEntity queen;
        // Supported production initial-budget setting, BELOW normal reserve; no later reserve edits or free food.
        System.setProperty("prime_ants.queenInitialReserve","3300");
        try { queen=start(c); } finally { if(old==null)System.clearProperty("prime_ants.queenInitialReserve");else System.setProperty("prime_ants.queenInitialReserve",old); }
        boolean[] checked={false};
        c.onEachTick(()->{
            var p=pile(c,queen);
            if(!checked[0] && stage(p,BroodStage.LARVA) && queen.bodyReserve()==0 && p.condition().equals("queen_reserve_exhausted")) {
                checked[0]=true;var states=state(p);
                c.assertTrue(p.records().stream().mapToLong(BroodRecord::nourishment).sum()==300 && p.condition().equals("queen_reserve_exhausted"),"Declared reduced budget is exactly exhausted by normal care");
                c.runAfterDelay(180,()->{c.assertTrue(queen.founding().sealed() && states.equals(state(p)) && queen.bodyReserve()==0 && workers(c).isEmpty(),"Present queen without bodily reserve cannot nourish/develop larvae");c.succeed();});
            }
        });
    }
    @GameTest(maxTicks=30000, structure="prime_ants_test:idle_ground")
    public void queenDeathLeavesViableCocoonsToEmerge(GameTestHelper c) {
        var queen=start(c);boolean[] killed={false};Set<UUID> ids=new HashSet<>();
        c.onEachTick(()->{
            var p=pile(c,queen);if(!killed[0] && stage(p,BroodStage.COCOON)) {
                killed[0]=true;p.records().forEach(r->ids.add(r.id()));queen.hurtServer(c.getLevel(),queen.damageSources().generic(),1000);
            }
            if(killed[0] && workers(c).size()==3) {
                c.assertTrue(queen.isRemoved() && p.records().isEmpty() && p.consumed().equals(ids) && workers(c).stream().allMatch(w->ids.contains(w.broodId())),"Nourished cocoons remain viable independently of dead queen");c.succeed();
            }
        });
    }
    @GameTest(maxTicks=30000, structure="prime_ants_test:idle_ground")
    public void insertionRefusalAndRestoredCocoonCannotDuplicateEmergence(GameTestHelper c) {
        LasiusNigerEntity[] queen={start(c)};boolean[] armed={false}, restored={false}, reconciled={false};CompoundTag[] stale={null};
        c.onEachTick(()->{
            var p=pile(c,queen[0]);
            if(!armed[0] && stage(p,BroodStage.COCOON)) {armed[0]=true;EmergenceFault.block(queen[0].getUUID());}
            if(!restored[0] && armed[0] && EmergenceFault.attempts(queen[0].getUUID())>=6) {
                restored[0]=true;
                c.assertTrue(workers(c).isEmpty() && p.records().size()==3 && p.records().stream().allMatch(r->r.progress()==p.stageDuration()) && p.condition().equals("worker_insertion_failed"),"Rejected insertion retains the exact three ready cocoons");
                stale[0]=p.saveWithFullMetadata(c.getLevel().registryAccess());var states=state(p);var ids=p.records().stream().map(BroodRecord::workerId).toList();
                var loaded=restorePile(c,p,stale[0]);long reserve=queen[0].bodyReserve();queen[0]=restoreAnt(c,queen[0]);
                c.assertTrue(states.equals(state(loaded)) && queen[0].bodyReserve()==reserve && ids.equals(loaded.records().stream().map(BroodRecord::workerId).toList()),"Restore IDs, exact stage/progress/nourishment/reserve and retry identities");
                EmergenceFault.release(queen[0].getUUID());
            }
            if(restored[0] && !reconciled[0] && workers(c).size()==3) {
                reconciled[0]=true;Set<UUID> adults=new HashSet<>();workers(c).forEach(w->adults.add(w.getUUID()));
                // Saved ready-cocoon/live-adult overlap, through the normal load API. No stages forced to create adults.
                restorePile(c,p,stale[0]);
                c.runAfterDelay(30,()->{
                    var now=pile(c,queen[0]);c.assertTrue(now.records().isEmpty() && now.consumed().size()==3 && workers(c).size()==3 && workers(c).stream().allMatch(w->adults.contains(w.getUUID())),"Retry reconciles existing lineage without another insertion");c.succeed();
                });
            }
        });
    }
    @GameTest(maxTicks=30000, structure="prime_ants_test:idle_ground")
    public void occupiedEmergenceSpaceRetainsCocoons(GameTestHelper c) {
        var queen=start(c);List<ItemEntity> blockers=new ArrayList<>();boolean[] blocked={false},checked={false};
        c.onEachTick(()->{
            var p=pile(c,queen);var plan=queen.founding().plan();
            if(!blocked[0] && stage(p,BroodStage.COCOON)) {
                blocked[0]=true;
                for(int f=3;f<=5;f++)for(int s=-1;s<=1;s++) {
                    BlockPos pos=plan.at(f,s,-2);if(pos.equals(plan.chamber())||pos.equals(plan.nursery()))continue;
                    var v=Vec3.atBottomCenterOf(pos);var item=new ItemEntity(c.getLevel(),v.x,v.y,v.z,new ItemStack(Items.COBBLESTONE));item.setDeltaMovement(Vec3.ZERO);item.setUnlimitedLifetime();
                    c.assertTrue(c.getLevel().addFreshEntity(item),"Physical stone-item obstruction fixture");blockers.add(item);
                }
            }
            if(!checked[0] && blocked[0] && p.records().stream().allMatch(r->r.progress()==p.stageDuration())) {
                checked[0]=true;var states=state(p);
                c.runAfterDelay(30,()->{
                    c.assertTrue(queen.founding().sealed() && p.condition().equals("emergence_space_blocked") && states.equals(state(p)) && workers(c).isEmpty(),"Live supported enclosure with occupied entity space retains viable cocoons");
                    blockers.forEach(ItemEntity::discard);
                    c.runAfterDelay(30,()->{c.assertTrue(workers(c).size()==3 && p.records().isEmpty(),"Clearing actual space permits exactly three production emergences");c.succeed();});
                });
            }
        });
    }
    @GameTest(maxTicks=30000, structure="prime_ants_test:idle_ground")
    public void callowLoadedTickMaturationRestoresAndWorkerDeathHasNoReplacement(GameTestHelper c) {
        var queen=start(c);boolean[] observed={false};LasiusNigerEntity[] worker={null};
        c.onEachTick(()->{
            if(!observed[0] && workers(c).size()==3) {
                observed[0]=true;worker[0]=workers(c).getFirst();UUID brood=worker[0].broodId(),id=worker[0].getUUID();long age=worker[0].callowAgeTicks();
                c.assertTrue(worker[0].isCallow(),"New real worker is pale");
                c.getLevel().getServer().clockManager().setTotalTicks(c.getLevel().dimensionType().defaultClock().orElseThrow(),500000L);
                worker[0]=restoreAnt(c,worker[0]);
                c.assertTrue(worker[0].getUUID().equals(id)&&worker[0].broodId().equals(brood)&&worker[0].queenId().equals(queen.getUUID())&&worker[0].callowAgeTicks()==age&&worker[0].isCallow(),"Normal restoration and daylight jump preserve callow identity/age/colour");
                long restoredGameTime=c.getLevel().getGameTime();
                c.runAfterDelay(10,()->{
                    long actualTicks=c.getLevel().getGameTime()-restoredGameTime;
                    c.assertTrue(worker[0].callowAgeTicks()==Math.min(worker[0].callowDuration(),age+actualTicks)&&worker[0].isCallow(),"Daylight and serialization do not advance biological maturation: exactly elapsed loaded ticks");
                });
                c.runAfterDelay(BroodPile.callowTicks()+5,()->{
                    c.assertTrue(workers(c).size()==3&&!worker[0].isCallow()&&worker[0].callowAgeTicks()==worker[0].callowDuration()&&worker[0].position().distanceToSqr(Vec3.atBottomCenterOf(queen.founding().plan().chamber()))<5,"Elapsed loaded ticks darken real workers which stay in nursery");
                    worker[0].hurtServer(c.getLevel(),worker[0].damageSources().generic(),1000);
                    c.runAfterDelay(180,()->{c.assertTrue(worker[0].isRemoved()&&workers(c).size()==2&&pile(c,queen).records().isEmpty(),"Death reduces actual population without replacement");c.succeed();});
                });
            }
        });
    }
    @GameTest(maxTicks=30000, structure="prime_ants_test:idle_ground")
    public void interruptedLarvalCareRestoresAndFinishesWithSameIdentities(GameTestHelper c) {
        LasiusNigerEntity[] queen={start(c)};boolean[] restored={false};Set<UUID> ids=new HashSet<>();
        c.onEachTick(()->{
            var p=pile(c,queen[0]);
            if(!restored[0]&&stage(p,BroodStage.LARVA)&&p.records().getFirst().progress()>8) {
                restored[0]=true;var states=state(p);p.records().forEach(r->ids.add(r.id()));long reserve=queen[0].bodyReserve(),duration=p.stageDuration();
                var loaded=restorePile(c,p,p.saveWithFullMetadata(c.getLevel().registryAccess()));queen[0]=restoreAnt(c,queen[0]);
                c.assertTrue(states.equals(state(loaded))&&loaded.stageDuration()==duration&&queen[0].bodyReserve()==reserve,"Running-process normal entity/block-entity restore preserves all larval accounting");
            }
            if(restored[0]&&workers(c).size()==3) {
                c.assertTrue(p.records().isEmpty()&&p.consumed().equals(ids)&&queen[0].bodyReserve()==0&&workers(c).stream().allMatch(w->ids.contains(w.broodId())&&queen[0].getUUID().equals(w.queenId())),"Restored larval care completes exactly the original clutch with unchanged full costs");c.succeed();
            }
        });
    }
    @GameTest(maxTicks=30000, structure="prime_ants_test:idle_ground")
    public void nurseryHandoffRetainsLiveObstructionAndBreachGuardsThroughRestoration(GameTestHelper c) {
        LasiusNigerEntity[] queen={start(c)};boolean[] checked={false};
        c.onEachTick(()->{
            var p=pile(c,queen[0]);if(!checked[0]&&stage(p,BroodStage.LARVA)&&p.records().getFirst().progress()>8) {
                checked[0]=true;c.assertTrue(queen[0].founding().sealed(),"Owned pile alone must be accepted");var states=state(p);long reserve=queen[0].bodyReserve();
                var tag=p.saveWithFullMetadata(c.getLevel().registryAccess());var plan=queen[0].founding().plan();BlockPos obstruction=plan.at(5,1,-1);
                c.getLevel().setBlock(obstruction,Blocks.STONE.defaultBlockState(),3);
                // The obstruction can be across a chunk face from the pile in a fresh UUID world.
                // Serialize its actual chunk; reading that coordinate from the pile's chunk tests unrelated local bits.
                var chunk=c.getLevel().getChunkAt(obstruction);c.assertTrue(chunk.getPos().equals(net.minecraft.world.level.ChunkPos.containing(obstruction)),"Serialize the physical obstruction's own chunk");
                PrimeAnts.LOGGER.info("T10 obstruction serialization pileChunk={} obstructionChunk={}",net.minecraft.world.level.ChunkPos.containing(p.getBlockPos()),chunk.getPos());
                var serial=net.minecraft.world.level.chunk.storage.SerializableChunkData.copyOf(c.getLevel(),chunk);
                var parsed=net.minecraft.world.level.chunk.storage.SerializableChunkData.parse(c.getLevel(),c.getLevel().palettedContainerFactory(),serial.write());
                var reload=parsed.read(c.getLevel(),c.getLevel().getPoiManager(),new net.minecraft.world.level.chunk.storage.RegionStorageInfo("test",c.getLevel().dimension(),"chunk"),chunk.getPos());
                c.assertTrue(reload.getBlockState(obstruction).is(Blocks.STONE),"FULL chunk preserves physical invalidity");
                restorePile(c,p,tag);queen[0]=restoreAnt(c,queen[0]);
                c.runAfterDelay(80,()->{
                    var now=pile(c,queen[0]);c.assertTrue(states.equals(state(now))&&queen[0].bodyReserve()==reserve&&!queen[0].founding().sealed()&&queen[0].founding().reason().endsWith("enclosure_chamber_obstructed"),"Restored owned brood cannot authorize unrelated obstacles or care");
                    c.getLevel().setBlock(obstruction,Blocks.AIR.defaultBlockState(),3);
                    c.getLevel().setBlock(plan.at(4,2,-2),Blocks.AIR.defaultBlockState(),3);
                    c.runAfterDelay(30,()->{c.assertTrue(!queen[0].founding().sealed()&&states.equals(state(now))&&now.condition().equals("enclosure_shell_open"),"Owned nursery cannot hide a shell breach");c.succeed();});
                });
            }
        });
    }
    @GameTest(maxTicks=30000, structure="prime_ants_test:idle_ground")
    public void moundBalanceAndOnlyExposedNativeUndergroundGrassConvert(GameTestHelper c) {
        // Controlled grass-origin fixture: random vanilla grass decay must not revoke pending tasks.
        // Entity physics, AI, block-entity care and development still run on real server ticks.
        var rule=net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED;
        int previousRandom=c.getLevel().getGameRules().get(rule);
        c.getLevel().getGameRules().set(rule,0,c.getLevel().getServer());
        var fixture=new QueenFoundingGameTest();fixture.terrain(c,Blocks.GRASS_BLOCK.defaultBlockState(),true);var queen=fixture.egg(c);boolean[] protectedOne={false};BlockPos[] protectedPos={null};
        c.onEachTick(()->{
            var f=queen.founding();c.assertTrue(f.phase()!=QueenFounding.Phase.FAILED,"Grass founding: "+f.reason());
            if(!protectedOne[0]&&f.plan()!=null) {
                protectedOne[0]=true;protectedPos[0]=f.plan().at(5,1,-3);
                // Same-state player replacement AFTER planning revokes origin without changing shell support.
                c.getLevel().setBlock(protectedPos[0],Blocks.GRASS_BLOCK.defaultBlockState(),3);
            }
            if(f.sealed()) {
                c.assertTrue(f.removed()==24&&f.deposited()==22&&f.plugged()==2&&f.released()==0&&f.carried()==0,"Physical 24=22 exterior blocks+two plugs, no normal item dumping");
                c.assertTrue(f.plan().deposits().stream().filter(p->c.getLevel().getBlockState(p).is(NurseryBlocks.NEST_SOIL)&&c.getLevel().getBlockState(p.below()).isSolidRender()).count()==22,"Exactly 22 supported blocks lie inside declared bounded deposit area");
                c.assertTrue(f.converted()>0&&c.getLevel().getBlockState(protectedPos[0]).is(Blocks.GRASS_BLOCK)&&!NaturalSoil.get(c.getLevel()).eligible(c.getLevel(),protectedPos[0]),"Protected grass remains untouched");
                for(var p:f.plan().undergroundSurfaces()) if(!p.equals(protectedPos[0])&&p.getY()==f.plan().entrance().getY()-3)
                    c.assertTrue(c.getLevel().getBlockState(p).is(NurseryBlocks.NEST_SOIL),"Exposed native underground floor is soil: "+p);
                c.assertTrue(c.getLevel().getBlockState(c.absolutePos(new BlockPos(12,4,12))).is(Blocks.GRASS_BLOCK),"Unrelated surface grass preserved");
                c.getLevel().getGameRules().set(rule,previousRandom,c.getLevel().getServer());
                PrimeAnts.LOGGER.info("T07 TERRAIN removed=24 mound=22 plugs=2 released=0 converted={} protectedFloor={}",f.converted(),protectedPos[0]);c.succeed();
            }
        });
    }
    @GameTest(maxTicks=30000, structure="prime_ants_test:idle_ground")
    public void blockedDeclaredMoundRetainsCarriedSoilWithoutDumping(GameTestHelper c) {
        var queen=start(c);boolean[] blocked={false},checked={false};
        c.onEachTick(()->{
            var f=queen.founding();if(!blocked[0]&&f.phase()==QueenFounding.Phase.TRANSPORTING) {
                blocked[0]=true;f.plan().deposits().forEach(p->c.getLevel().setBlock(p,Blocks.STONE.defaultBlockState(),3));
            }
            if(!checked[0]&&f.phase()==QueenFounding.Phase.FAILED) {
                checked[0]=true;c.assertTrue(blocked[0]&&f.reason().equals("bounded_mound_deposition_blocked")&&f.carried()>0&&f.released()==0,"Blocked area must report refusal and retain real material");int held=f.carried();
                c.runAfterDelay(80,()->{c.assertTrue(f.carried()==held&&f.released()==0&&f.deposited()==0&&f.plan().deposits().stream().allMatch(p->c.getLevel().getBlockState(p).is(Blocks.STONE)),"No remote completion, discard, dumping or player-block overwrite");c.succeed();});
            }
        });
    }
    @GameTest(maxTicks=30000, structure="prime_ants_test:idle_ground")
    public void boundedMoundSkipsPlantsAndRevokedPlayerSupports(GameTestHelper c) {
        var fixture=new QueenFoundingGameTest();fixture.terrain(c,Blocks.DIRT.defaultBlockState(),true,true);var queen=fixture.egg(c);
        boolean[] edited={false};List<BlockPos> plants=new ArrayList<>();BlockPos[] support={null};
        c.onEachTick(()->{
            var f=queen.founding();c.assertTrue(f.phase()!=QueenFounding.Phase.FAILED,"Selective bounded mound: "+f.reason());
            if(!edited[0]&&f.plan()!=null) {
                edited[0]=true;var available=f.plan().deposits().stream().filter(p->NestPlan.walkable(c.getLevel(),p)&&NaturalSoil.get(c.getLevel()).eligible(c.getLevel(),p.below())).toList();
                c.assertTrue(available.size()>=29,"Fixture has enough native supported choices");
                plants.addAll(available.subList(0,6));plants.forEach(p->c.getLevel().setBlock(p,Blocks.SHORT_GRASS.defaultBlockState(),3));
                support[0]=available.get(6).below();c.getLevel().setBlock(support[0],Blocks.DIRT.defaultBlockState(),3);
            }
            if(f.sealed()) {
                c.assertTrue(f.deposited()==22&&f.released()==0&&f.plugged()==2&&f.carried()==0,"Alternative supported choices still conserve 24=22+2");
                c.assertTrue(plants.stream().allMatch(p->c.getLevel().getBlockState(p).is(Blocks.SHORT_GRASS))&&c.getLevel().getBlockState(support[0].above()).isAir()&&!NaturalSoil.get(c.getLevel()).eligible(c.getLevel(),support[0]),"No plant overwrite and no placement on revoked player support");c.succeed();
            }
        });
    }
}
