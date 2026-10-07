package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import dev.primeants.brood.BroodStage;
import dev.primeants.brood.NurseryBlocks;
import dev.primeants.colony.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Stage-1 T05 in real loaded ticks: the nursery's upgrade to tier 2. A colony founded from an egg and fed as a player
 * would (dropped clay, then dropped food) grows to Mature by itself, rebuilds its nursery chamber's walls with clay from
 * its own store, block by block, and its brood then develops at the tier-2 speed. Production founding, digging, hauling
 * and upgrade work; nothing assigns a stage, a tier, a job or an inventory. */
public final class NurseryUpgradeGameTest {
    private final NestPlanFixture fx = new NestPlanFixture();
    static long food(GameTestHelper c) {
        return c.getLevel().getEntitiesOfClass(ItemEntity.class, c.getBounds().inflate(8), i -> i.isAlive() && WorkerTasks.food(i.getItem())).stream().mapToInt(i -> i.getItem().getCount()).sum();
    }
    /** A player's feeding for the long path: the fixture's growth food and the clay behind the entrance as the nest opens;
     * once the store has held 16 clay, four apples and two chickens whenever the last drop has been collected. Once the
     * colony is Mature the chickens come only while the cache shows fewer than two: adults eat only sugar, and without
     * larvae to eat them chickens pile up in the six-slot cache, block apples and starve the adults (T05 diagnostic).
     * The player watches the cache and the colony, as anyone could. fed = {opening drop done, waves, last wave tick, store
     * held 16 clay}. */
    void feed(GameTestHelper c, LasiusNigerEntity q, int clay, long[] fed) {
        boolean[] opened = {fed[0] == 1}; fx.grow(c, q, opened);
        if (opened[0] && fed[0] == 0) { fed[0] = 1; fx.drop(c, q.founding().plan().at(-5, 0, 1), new ItemStack(Items.CLAY_BALL, clay)); }
        var s = NestPlanFixture.store(c, q);
        if (s != null && s.units(MaterialUnits.Material.CLAY) >= 16) fed[3] = 1;
        if (fed[3] == 1 && food(c) == 0 && c.getTick() - fed[2] >= 200) {
            var cache = fx.f.cache(c, q); long chickens = cache == null ? 0 : cache.contents().stream().filter(st -> st.is(Items.CHICKEN)).count();
            var colony = ChamberRegistry.get(c.getLevel()).colony(q.getUUID()); boolean mature = colony != null && colony.stage() == ColonyStage.MATURE;
            fed[1]++; fed[2] = c.getTick(); fx.food.supply(c, q, 4, !mature || chickens < 2 ? 2 : 0);
        }
    }
    /** Every clay ball the player dropped, now: on the ground, in any worker's mandibles, in the store, rammed into the
     * colony's own walls, or in transfer custody. */
    static long clay(GameTestHelper c, NestPlanFixture fx, LasiusNigerEntity q) {
        var l = c.getLevel(); var box = c.getBounds().inflate(8); var p = q.founding().plan(); var s = NestPlanFixture.store(c, q);
        long ground = l.getEntitiesOfClass(ItemEntity.class, box, i -> i.isAlive() && i.getItem().is(Items.CLAY_BALL)).stream().mapToInt(i -> i.getItem().getCount()).sum();
        long carried = fx.f.workers(c, q).stream().filter(w -> w.getMainHandItem().is(Items.CLAY_BALL)).mapToInt(w -> w.getMainHandItem().getCount()).sum();
        long stored = s == null ? 0 : s.units(MaterialUnits.Material.CLAY);
        long walls = walls(p).stream().filter(b -> ColonyTerrain.get(l).built(l, b, q.getUUID(), NurseryBlocks.PACKED_CLAY)).count() * NestWalls.CLAY_PER_CELL;
        long custody = TransferCustody.get(l).contents().stream().filter(t -> t.stack().is(Items.CLAY_BALL) && box.contains(t.position())).mapToInt(t -> t.stack().getCount()).sum();
        return ground + carried + stored + walls + custody;
    }
    static List<BlockPos> walls(NestPlan p) { return ChamberUpgrade.walls(p, ChamberRegistry.foundingChamber(p)); }
    static ChamberUpgrade.Job upgrade(GameTestHelper c, LasiusNigerEntity q) { return ChamberUpgrade.get(c.getLevel()).job(q.getUUID(), ChamberRegistry.FOUNDING, 2); }
    static LasiusNigerEntity builder(GameTestHelper c, NestPlanFixture fx, LasiusNigerEntity q, ChamberUpgrade.Job j) {
        return j == null || j.claim == null ? null : fx.f.workers(c, q).stream().filter(w -> w.getUUID().equals(j.claim)).findFirst().orElse(null);
    }
    /** The upgrade's exact accounting at this tick: units taken = carried by the claimed builder + built + in custody. */
    static void ledger(GameTestHelper c, NestPlanFixture fx, LasiusNigerEntity q, ChamberUpgrade.Job j) {
        if (j == null) return;
        var w = builder(c, fx, q, j); int carried = w != null && w.getMainHandItem().is(Items.CLAY_BALL) ? w.getMainHandItem().getCount() : 0;
        c.assertTrue(j.ledger().exact() && j.carried() == carried && j.built().size() * NestWalls.CLAY_PER_CELL == j.ledger().built(),
            "Upgrade units taken = carried + built + custody, and the builder carries exactly the job's carried units: " + j.ledger() + " builder=" + carried);
        for (var b : j.built()) c.assertTrue(ColonyTerrain.get(c.getLevel()).built(c.getLevel(), b, q.getUUID(), NurseryBlocks.PACKED_CLAY), "Each built cell is the colony's own packed clay: " + b);
    }

    @GameTest(maxTicks=90000,structure="prime_ants_test:idle_ground")
    public void youngColonyReachesMatureByRealPlayThenRebuildsItsNurseryInPackedClayAndItsBroodDevelopsFaster(GameTestHelper c){ longPath(c); }
    /** The long path: Mature by real play, the nursery's upgrade to tier 2, and its brood at the tier-2 speed. */
    private void longPath(GameTestHelper c){
        var q=fx.start(c);long[] fed={0,0,0,0};long[] mature={-1},tier2={-1},confirmed={-1};ColonyDevelopment.Evaluation[] seen={null};
        Map<UUID,long[]> stage=new HashMap<>();List<Long> tierOneEggs=new ArrayList<>();List<String> evidence=new ArrayList<>();int[] most={0};
        c.onEachTick(()->{
            var l=c.getLevel();var p=NestPlanFixture.pile(c,q);feed(c,q,16,fed);fx.soil(c,q);if(p==null)return;
            var e=p.stageEvaluation();var plan=q.founding().plan();var j=upgrade(c,q);
            if(fed[0]==1)c.assertTrue(clay(c,fx,q)==16,"All 16 dropped clay balls are on the ground, carried, stored, in the walls or in custody: "+clay(c,fx,q));
            ledger(c,fx,q,j);
            // Brood stage timing, in the pile's own loaded ticks: when each record's stage began, and at which speed.
            var speed=p.nursery().speed();
            if(speed==3&&tier2[0]<0)tier2[0]=p.loadedTicks();
            if(speed!=3&&tier2[0]>=0)c.fail("The nursery stays at tier 2 once rebuilt: speed "+speed);
            most[0]=Math.max(most[0],p.records().size());
            // Only stages cared for on every tick count: a missed tick of care rightly lengthens a stage.
            for(var r:p.records()){
                var was=stage.get(r.id());
                if(was!=null&&was[0]==r.stage().ordinal()){if(r.neglectTicks()>0)was[3]=0;continue;}
                if(was!=null&&was[0]==BroodStage.EGG.ordinal()&&was[3]==1){
                    long took=p.loadedTicks()-was[1];
                    if(was[2]==2)tierOneEggs.add(took);
                    else if(was[2]==3&&tier2[0]>=0&&was[1]>=tier2[0])evidence.add(r.id()+" egg stage "+took+" loaded ticks at tier 2");
                }
                stage.put(r.id(),new long[]{r.stage().ordinal(),p.loadedTicks(),speed,r.neglectTicks()>0||was==null&&r.progress()>0?0:1});
            }
            if(c.getTick()%1000==0)PrimeAnts.LOGGER.info("T05 LONG PATH tick={} adults={} stage={} records={} gate={} cache={} store={} clayWalls={} job={} waves={} evaluation={}",c.getTick(),e==null?null:e.inputs().adults(),e==null?null:e.stage(),
                p.records().stream().map(r->r.stage()+":"+r.progress()).toList(),p.growth().reason(),fx.f.cache(c,q)==null?null:fx.f.cache(c,q).size(),NestPlanFixture.store(c,q)==null?null:NestPlanFixture.store(c,q).contents().size(),
                walls(plan).stream().filter(b->ColonyTerrain.get(l).built(l,b,q.getUUID(),NurseryBlocks.PACKED_CLAY)).count(),j==null?null:j.reason+" "+j.ledger(),fed[1],e);
            if(e==null||e==seen[0])return;
            var previous=seen[0];seen[0]=e;
            if(mature[0]<0){
                c.assertTrue(j==null,"No upgrade before Mature unlocks tier 2: "+e);
                if(e.stage()!=ColonyStage.MATURE)return;
                // The first real-tick promotion to Mature: every requirement met by live bodies and blocks.
                var colony=ChamberRegistry.get(l).colony(q.getUUID());
                c.assertTrue(previous!=null&&previous.stage()==ColonyStage.YOUNG&&colony.stage()==ColonyStage.MATURE&&e.result().certain()==ColonyStage.MATURE&&e.inputs().adults().known()>=25
                    &&e.inputs().tier(ChamberFunction.QUEENS_HALL).known()>=1&&e.inputs().tier(ChamberFunction.MATERIAL_STORE).known()>=1&&e.inputs().food().known()>=4&&e.inputs().clay().known()>=16
                    &&e.inputs().tier(ChamberFunction.NURSERY).known()==1&&e.cap()==ColonyStage.MATURE.adultCap(),"A Young colony is promoted to Mature by what it physically holds: "+e);
                c.assertTrue(p.nursery().equals(new dev.primeants.brood.BroodCapacity.Nursery(7,2)),"With its hall and earth walls the nursery keeps 7 brood at x1: "+p.nursery());
                mature[0]=c.getTick();PrimeAnts.LOGGER.info("T05 MATURE BY REAL PLAY queen={} tick={} waves={} registry={} evaluation={}",q.getUUID(),c.getTick(),fed[1],colony,e);return;
            }
            // From the promotion until the rebuilt nursery is confirmed at tier 2 the colony stays Mature: moving clay from
            // the store into the walls never demotes it. Afterwards only food may cost the stage: in an eating wave a Mature
            // colony's six-slot cache can dip below Mature's four food units (food-store capacity is T06's).
            if(confirmed[0]<0)c.assertTrue(e.stage()==ColonyStage.MATURE&&ChamberRegistry.get(l).colony(q.getUUID()).stage()==ColonyStage.MATURE&&e.inputs().clay().known()>=16,"The colony stays Mature throughout the upgrade: "+e);
            else{
                // After the upgrade only food may cost the stage: the walls keep their clay and the colony its adults and rooms.
                c.assertTrue(e.inputs().clay().known()==16&&(e.stage()==ColonyStage.MATURE||e.result().missing(ColonyStage.MATURE).stream().allMatch(m->m.requirement().name().equals("food"))),
                    "After the upgrade the colony keeps its 16 clay and leaves Mature only for want of food: "+e);
                if(previous.stage()!=e.stage())PrimeAnts.LOGGER.info("T05 stage after the upgrade queen={} tick={} from={} to={} missing={}",q.getUUID(),c.getTick(),previous.stage(),e.stage(),e.result().missing(ColonyStage.MATURE));
            }
            if(j==null)return;
            if(j.built().isEmpty()&&j.taken()==0)c.assertTrue(j.cells.equals(walls(plan))&&j.tier==2,"The job rebuilds the nursery chamber's eight wall cells to tier 2: "+j.cells);
            if(confirmed[0]<0){
                if(!j.complete()||j.claim!=null)return;
                var founding=NestPlanFixture.chamber(e,ChamberRegistry.FOUNDING);
                if(founding.tier()!=2)return; // the nursery's next evaluation
                var store=NestPlanFixture.store(c,q);var registry=ChamberRegistry.get(l).colony(q.getUUID()).chamber(ChamberRegistry.FOUNDING);
                c.assertTrue(e.inputs().tier(ChamberFunction.NURSERY).known()==2&&founding.clay()==8&&registry.tier()==2&&j.taken()==8&&j.released()==0&&store.units(MaterialUnits.Material.CLAY)==8
                    &&e.inputs().clay().known()==16&&!j.stopped(),"The nursery chamber is confirmed at tier 2 from its eight packed-clay walls, built with eight units from the store: "+e+" store="+store.contents());
                c.assertTrue(p.nursery().equals(new dev.primeants.brood.BroodCapacity.Nursery(10,3)),"The tier-2 nursery keeps 10 brood at x1.5: "+p.nursery());
                confirmed[0]=c.getTick();PrimeAnts.LOGGER.info("T05 NURSERY CONFIRMED TIER 2 queen={} tick={} matureAt={} job={} evaluation={}",q.getUUID(),c.getTick(),mature[0],j.reason+" "+j.ledger(),e);
            }
            if(evidence.isEmpty()){if((c.getTick()-mature[0])%1000==0)PrimeAnts.LOGGER.info("T05 waits for a tier-2 brood stage queen={} tick={} records={} gate={}",q.getUUID(),c.getTick(),p.records().stream().map(r->r.stage()+":"+r.progress()).toList(),p.growth().reason());return;}
            c.assertTrue(evidence.stream().allMatch(x->{var t=Long.parseLong(x.replaceAll(".* egg stage (\\d+) .*","$1"));return t>=79&&t<=81;})&&tierOneEggs.stream().allMatch(t->t==120),
                "Egg stages take the tier-2 duration (80 loaded ticks) after the upgrade, against 120 at tier 1: "+evidence+" tierOne="+tierOneEggs);
            PrimeAnts.LOGGER.info("T05 NURSERY TIER 2 queen={} tick={} matureAt={} confirmedAt={} tier2PileTick={} evidence={} tierOneEggs={} mostBroodAtOnce={} slots={} waves={} job={} evaluation={}",
                q.getUUID(),c.getTick(),mature[0],confirmed[0],tier2[0],evidence,tierOneEggs.size(),most[0],p.nursery().slots(),fed[1],j.reason+" "+j.ledger(),e);
            c.succeed();
        });
    }
    /** Upgrade safety in one Mature colony, fed as in the long path with two clay to spare: a player replaces the job's
     * last wall cell as soon as it is planned (with packed clay of their own, which never counts); a save and reload
     * mid-upgrade keeps the job, the carried unit, the store and the walls exactly once; a builder killed with a unit in
     * its mandibles releases it through transfer custody exactly once; and the next builder stops at the player's cell,
     * never converting it, and puts its unit back. The ledger is exact at every tick. */
    @GameTest(maxTicks=72000,structure="prime_ants_test:idle_ground")
    public void upgradeKeepsEveryUnitThroughReloadAndKilledBuilderAndNeverConvertsPlayerChangedWall(GameTestHelper c){
        LasiusNigerEntity[] q={fx.start(c)};long[] fed={0,0,0,0};int[] step={0};BlockPos[] changed={null};UUID[] killed={null},transfer={null};long[] pending={0};
        c.onEachTick(()->{
            var l=c.getLevel();var p=NestPlanFixture.pile(c,q[0]);feed(c,q[0],18,fed);fx.soil(c,q[0]);if(p==null)return;
            var plan=q[0].founding().plan();var j=upgrade(c,q[0]);
            if(fed[0]==1)c.assertTrue(clay(c,fx,q[0])==18,"All 18 dropped clay balls are on the ground, carried, stored, in the colony's walls or in custody: "+clay(c,fx,q[0]));
            ledger(c,fx,q[0],j);
            if(changed[0]!=null)c.assertTrue(l.getBlockState(changed[0]).is(NurseryBlocks.PACKED_CLAY)&&!ColonyTerrain.get(l).built(l,changed[0],q[0].getUUID(),NurseryBlocks.PACKED_CLAY)
                &&ColonyTerrain.get(l).wallTier(l,changed[0],q[0].getUUID())==1,"The player's cell stays theirs and never counts as tier 2");
            if(transfer[0]!=null){
                boolean inCustody=TransferCustody.get(l).contents().stream().anyMatch(t->t.id().equals(transfer[0]));boolean entity=l.getEntity(transfer[0]) instanceof ItemEntity i&&i.isAlive();
                c.assertTrue(!(inCustody&&entity)&&TransferCustody.get(l).contents().stream().filter(t->t.id().equals(transfer[0])).count()<=1,"The dead builder's unit is released exactly once, through custody");
                if(inCustody)pending[0]++;
            }
            if(c.getTick()%1000==0)PrimeAnts.LOGGER.info("T05 SAFETY tick={} step={} stage={} job={} store={}",c.getTick(),step[0],p.stageEvaluation()==null?null:p.stageEvaluation().stage(),j==null?null:j.reason+" "+j.ledger()+" built="+j.built().size(),NestPlanFixture.store(c,q[0])==null?null:NestPlanFixture.store(c,q[0]).contents().size());
            if(j==null)return;
            if(step[0]==0){
                // Planned: a player replaces the job's last wall cell with a packed-clay block of their own.
                c.assertTrue(j.built().isEmpty()&&j.cells.equals(walls(plan))&&p.stageEvaluation().stage()==ColonyStage.MATURE,"Planned for a Mature colony over the nursery chamber's eight wall cells: "+j.cells);
                changed[0]=j.cells.getLast();l.setBlock(changed[0],NurseryBlocks.PACKED_CLAY.defaultBlockState(),3);step[0]=1;
                PrimeAnts.LOGGER.info("T05 SAFETY PLAYER CHANGED WALL queen={} cell={} tick={}",q[0].getUUID(),changed[0],c.getTick());return;
            }
            if(step[0]==1){
                if(j.built().size()<2||j.carried()!=1)return;
                // Mid-upgrade save and reload: saved data from disk, then the builder, the queen and the store block entity.
                var w=builder(c,fx,q[0],j);if(w==null)return;var cargo=w.getMainHandItem().copy();var ledger=j.ledger();var built=j.built();var claim=j.claim;var store=NestPlanFixture.store(c,q[0]);var contents=store.contents();long before=clay(c,fx,q[0]);
                l.getDataStorage().saveAndJoin();
                try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){
                    var upgrades=disk.get(ChamberUpgrade.TYPE);var registry=disk.get(ChamberRegistry.TYPE);var excavation=disk.get(ChamberExcavation.TYPE);var terrain=disk.get(ColonyTerrain.TYPE);var custody=disk.get(TransferCustody.TYPE);
                    c.assertTrue(upgrades!=null&&registry!=null&&excavation!=null&&terrain!=null,"Disk holds the upgrade, the registry, the excavation and the terrain records");
                    l.getDataStorage().set(ChamberUpgrade.TYPE,upgrades);l.getDataStorage().set(ChamberRegistry.TYPE,registry);l.getDataStorage().set(ChamberExcavation.TYPE,excavation);l.getDataStorage().set(ColonyTerrain.TYPE,terrain);
                    if(custody!=null)l.getDataStorage().set(TransferCustody.TYPE,custody);
                }
                var loaded=fx.f.restore(c,w);q[0]=fx.f.restore(c,q[0]);
                var marker=store.getBlockPos();var tag=store.saveWithFullMetadata(l.registryAccess());var state=store.getBlockState();l.removeBlockEntity(marker);
                var back=(MaterialStore)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(marker,state,tag,l.registryAccess());c.assertTrue(back!=null,"Normal store restore");l.setBlockEntity(back);
                var chunk=l.getChunkAt(built.getFirst());var serial=net.minecraft.world.level.chunk.storage.SerializableChunkData.copyOf(l,chunk);
                var read=net.minecraft.world.level.chunk.storage.SerializableChunkData.parse(l,l.palettedContainerFactory(),serial.write()).read(l,l.getPoiManager(),new net.minecraft.world.level.chunk.storage.RegionStorageInfo("test",l.dimension(),"chunk"),chunk.getPos());
                c.assertTrue(read.getBlockState(built.getFirst()).is(NurseryBlocks.PACKED_CLAY),"A saved chunk keeps the rebuilt wall cell");
                var after=upgrade(c,q[0]);
                c.assertTrue(after!=j&&after.ledger().equals(ledger)&&after.built().equals(built)&&claim.equals(after.claim)&&ItemStack.matches(loaded.getMainHandItem(),cargo)&&back.contents().size()==contents.size()
                    &&clay(c,fx,q[0])==before,"The job, the carried unit, the store and the walls reload exactly as they were: "+after.ledger()+" "+loaded.getMainHandItem());
                PrimeAnts.LOGGER.info("T05 SAFETY MID-UPGRADE RELOAD queen={} builder={} built={} ledger={} cargo={} store={}",q[0].getUUID(),claim,built.size(),ledger,cargo,contents.size());
                step[0]=2;return;
            }
            c.assertTrue(fx.f.workers(c,q[0]).stream().filter(a->a.getUUID().equals(j.claim)).count()<=1,"Never a duplicate builder identity after the reload");
            if(step[0]==2){
                if(j.built().size()<3||j.carried()!=1)return;
                // The restored builder carried on; now it is killed with a unit in its mandibles.
                var w=builder(c,fx,q[0],j);if(w==null)return;
                killed[0]=w.getUUID();transfer[0]=UUID.nameUUIDFromBytes(("worker-cargo:"+w.getUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                c.assertTrue(TransferCustody.get(l).contents().stream().noneMatch(t->t.id().equals(transfer[0]))&&l.getEntity(transfer[0])==null,"No custody record before the death");
                w.hurtServer(l,w.damageSources().genericKill(),1000);
                c.assertTrue(!w.isAlive()&&w.getMainHandItem().isEmpty()&&ColonyMembers.get(l).member(w.getUUID()).dead()&&j.claim==null&&j.released()==1&&j.carried()==0&&j.ledger().exact(),
                    "A real lethal hit releases the builder's unit to custody once and frees the job: "+j.ledger());
                PrimeAnts.LOGGER.info("T05 SAFETY BUILDER KILLED queen={} worker={} transfer={} ledger={}",q[0].getUUID(),w.getUUID(),transfer[0],j.ledger());step[0]=3;return;
            }
            c.assertTrue(!killed[0].equals(j.claim),"The dead builder never builds again");
            if(!j.stopped()||j.claim!=null)return;
            // The next builder reached the player's cell: the job stopped there and its unit went back to the store.
            var founding=NestPlanFixture.chamber(p.stageEvaluation(),ChamberRegistry.FOUNDING);
            c.assertTrue(j.reason.startsWith("stopped_wall_cell_not_colony_earth_at_")&&j.built().size()==7&&j.built().equals(j.cells.subList(0,7))&&j.released()==1&&j.taken()==8&&j.carried()==0
                &&founding.tier()==1&&p.nursery().speed()==2,"The job stops at the player's cell, never converting it; seven cells are rebuilt and the chamber stays at tier 1: "+j.reason+" "+j.ledger()+" "+founding);
            PrimeAnts.LOGGER.info("T05 SAFETY STOPPED AT PLAYER CELL queen={} tick={} cell={} reason={} ledger={} custodyTicks={}",q[0].getUUID(),c.getTick(),changed[0],j.reason,j.ledger(),pending[0]);
            // A survival player breaks one rebuilt wall cell: an owned component, so the colony raises its alarm; the block
            // has no loot, so it yields nothing, never more than went into it. The player's own block is no component.
            var cell=j.built().getFirst();var near=new net.minecraft.world.phys.AABB(cell).inflate(3);
            c.assertTrue(q[0].getUUID().equals(ColonyAlarm.ownedComponent(l,cell))&&ColonyAlarm.ownedComponent(l,changed[0])==null&&ColonyAlarm.get(l).alarm(q[0].getUUID())==null,"Rebuilt walls are the colony's own components, the player's block is not");
            long items=l.getEntitiesOfClass(ItemEntity.class,near,ItemEntity::isAlive).size();
            var player=DefenseGameTest.player(c,net.minecraft.world.phys.Vec3.atBottomCenterOf(plan.outside()),net.minecraft.world.level.GameType.SURVIVAL);
            c.assertTrue(player.gameMode.destroyBlock(cell)&&l.getBlockState(cell).isAir()&&ColonyAlarm.get(l).alarm(q[0].getUUID())!=null&&l.getEntitiesOfClass(ItemEntity.class,near,ItemEntity::isAlive).size()==items,
                "A survival break of a rebuilt wall cell raises the colony's alarm and drops nothing");
            PrimeAnts.LOGGER.info("T05 SAFETY PLAYER BROKE REBUILT WALL queen={} cell={} alarm={}",q[0].getUUID(),cell,ColonyAlarm.get(l).alarm(q[0].getUUID()));c.succeed();
        });
    }
}
