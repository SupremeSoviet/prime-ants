package dev.primeants.gametest;

import com.mojang.serialization.JsonOps;
import dev.primeants.PrimeAnts;
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
import net.minecraft.world.level.block.Blocks;

/** Stage-1 T06 in real loaded ticks: the colony's mound (NestMound, MoundSoil), its first plan-based surface structure.
 * Soil from nest-plan rooms goes on the colony's stage mound, each column on its own ground, burying witnessed short
 * plants without drops and never covering or removing anything else; every unit stays physical and counted. Production
 * founding, digging and deposits on the T04 nest-plan fixture; nothing places soil or assigns a job. */
public final class MoundGameTest {
    private final NestPlanFixture fx = new NestPlanFixture();

    /** Every block of the colony's own mound soil in its nest's exterior, by position. */
    static Set<BlockPos> mound(GameTestHelper c, LasiusNigerEntity q) {
        var l = c.getLevel(); var p = q.founding().plan(); var out = new HashSet<BlockPos>();
        for (int f = -NestMound.BACK - 1; f <= 0; f++) for (int s = -NestMound.SIDE - 1; s <= NestMound.SIDE + 1; s++) for (int dy = -NestMound.GROUND_RANGE; dy <= NestMound.GROUND_RANGE + NestMound.LAYERS + 1; dy++) {
            var b = p.at(f, s, dy); if (NestPlan.loaded(l, b) && ColonyTerrain.get(l).mound(l, b, q.getUUID())) out.add(b.immutable());
        }
        return out;
    }
    /** The planned cells of a stage's mound where they lie now. */
    static Set<BlockPos> planned(GameTestHelper c, LasiusNigerEntity q, ColonyStage stage) {
        var out = new HashSet<BlockPos>(); for (var s : MoundSoil.slots(c.getLevel(), q.founding().plan(), q.getUUID(), stage)) if (s.pos() != null) out.add(s.pos()); return out;
    }
    /** The approach lane behind the entrance, the exterior standing spot and the stairs stay clear: no mound soil in the
     * lane's cells, the standing spot and the stairs walkable. */
    static void route(GameTestHelper c, LasiusNigerEntity q) {
        var l = c.getLevel(); var p = q.founding().plan();
        for (int f = -NestMound.BACK - 1; f <= -1; f++) for (int dy = -NestMound.GROUND_RANGE; dy <= NestMound.GROUND_RANGE + NestMound.LAYERS + 1; dy++)
            c.assertFalse(l.getBlockState(p.at(f, 0, dy)).is(NurseryBlocks.NEST_SOIL) && ColonyTerrain.get(l).mound(l, p.at(f, 0, dy), q.getUUID()), "No mound soil in the approach lane: " + p.at(f, 0, dy));
        c.assertTrue(NestPlan.walkable(l, p.outside()) && NestPlan.walkable(l, p.at(0, 0, 0)) && NestPlan.walkable(l, p.at(1, 0, -1)) && NestPlan.walkable(l, p.at(2, 0, -2)) || q.founding().lifecycle() != QueenFounding.Lifecycle.OPEN,
            "The exterior standing spot and the stairs stay walkable");
    }
    /** Witnessed short grass, as genuine generation leaves it, on every open cell resting on natural soil in the Mature
     * mound's columns, so the 0.1.0 deposit list keeps only the layer above the queen's own deposits. */
    static Set<BlockPos> plant(GameTestHelper c, LasiusNigerEntity q) {
        var l = c.getLevel(); var p = q.founding().plan(); var records = NativeVegetation.CODEC.encodeStart(JsonOps.INSTANCE, NativeVegetation.get(l)).getOrThrow().getAsJsonObject(); var out = new HashSet<BlockPos>();
        for (var cell : NestMound.plan(ColonyStage.MATURE)) {
            if (cell.layer() != 0) continue;
            for (int dy = NestMound.GROUND_RANGE; dy >= -NestMound.GROUND_RANGE; dy--) {
                var b = p.at(cell.forward(), cell.side(), dy);
                if (!NaturalSoil.get(l).eligible(l, b)) continue;
                var above = b.above();
                if (l.getBlockState(above).isAir()) { l.setBlock(above, Blocks.SHORT_GRASS.defaultBlockState(), 3); records.addProperty(Long.toString(above.asLong()), l.getBlockState(above).toString()); out.add(above.immutable()); }
                break;
            }
        }
        l.getDataStorage().set(NativeVegetation.TYPE, NativeVegetation.CODEC.parse(JsonOps.INSTANCE, records).getOrThrow());
        for (var b : out) if (!NativeVegetation.get(l).eligible(l, b)) throw new IllegalStateException("Fixture grass not witnessed at " + b);
        return out;
    }
    /** The 0.1.0 deposit list's cells that would take a unit now by its own rule (natural soil or colony mound below, air). */
    static long oldDepositRoom(GameTestHelper c, LasiusNigerEntity q) {
        var l = c.getLevel(); var p = q.founding().plan();
        return NestExpansion.deposits(p).stream().filter(b -> NestExpansion.depositSupport(l, b, q.getUUID()) && l.getBlockState(b).isAir()).count();
    }

    /** m1. Witnessed short grass covers the land around the nest once it has opened (the queen's deposits and the plugs'
     * two units are laid where 0.1.0 lays them), so the 0.1.0 deposit list keeps room only on top of those, less than the
     * store's 24 units once the widening has used it. A Young
     * colony digs its material store and its queen's hall and lays every one of their 36 units on its Young mound,
     * burying grass without drops. Soil accounting is exact at every tick, every mound block lies in the plan, and the
     * approach lane, the exterior standing spot and the stairs stay clear. */
    @GameTest(maxTicks=48000,structure="prime_ants_test:idle_ground")
    public void youngColonyLaysItsStoreAndHallSoilOnItsStageMoundWhereShortPlantsCoverTheOldDeposits(GameTestHelper c){
        var q=fx.start(c);boolean[] supplied={false},fed={false};List<Set<BlockPos>> planted=new ArrayList<>();long[] oldRoom={-1};List<Set<BlockPos>> before=new ArrayList<>();
        c.onEachTick(()->{
            var l=c.getLevel();fx.grow(c,q,supplied);fx.soil(c,q);
            var plan=q.founding().plan();if(plan==null||q.founding().phase()!=QueenFounding.Phase.SETTLED)return;
            if(planted.isEmpty()&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){planted.add(plant(c,q));PrimeAnts.LOGGER.info("T06 MOUND PLANTED queen={} grass={} oldDepositRoom={}",q.getUUID(),planted.getFirst().size(),oldDepositRoom(c,q));}
            route(c,q);
            c.assertTrue(l.getEntitiesOfClass(ItemEntity.class,c.getBounds().inflate(8),i->i.isAlive()&&i.getItem().is(Items.WHEAT_SEEDS)).isEmpty(),"Buried grass drops nothing");
            var all=mound(c,q);var allowed=MoundSoil.cells(l,plan,q.getUUID());
            for(var b:all)c.assertTrue(allowed.contains(b),"Every mound block is a 0.1.0 deposit or a planned mound cell: "+b);
            var store=NestPlanFixture.job(c,q,ChamberExcavation.STORE);
            if(store==null)return;
            c.assertFalse(planted.isEmpty(),"The grass was planted before the store was planned");
            if(!fed[0]){fed[0]=true;fx.food.supply(c,q,12,8);} // as the hall test: a full cache keeps laying going
            if(before.isEmpty()){
                // The store was just planned: by the 0.1.0 deposit rule only the layer above the queen's deposits is left.
                before.add(new HashSet<>(all));oldRoom[0]=oldDepositRoom(c,q);
                c.assertTrue(oldRoom[0]<store.tasks.size()&&store.removed()==0,"The 0.1.0 deposit list could not take the store's soil: room "+oldRoom[0]+" for "+store.tasks.size());
                var young=MoundSoil.capacity(MoundSoil.slots(l,plan,q.getUUID(),ColonyStage.YOUNG));
                PrimeAnts.LOGGER.info("T06 MOUND STORE PLANNED queen={} tick={} oldDepositRoom={} young={} mound={}",q.getUUID(),c.getTick(),oldRoom[0],young,all.size());
            }
            var hall=NestPlanFixture.job(c,q,ChamberExcavation.HALL);
            if(c.getTick()%500==0)PrimeAnts.LOGGER.info("T06 MOUND tick={} store={}/{} hall={} mound={}",c.getTick(),store.deposited,store.removed(),hall==null?null:hall.deposited+"/"+hall.removed(),all.size());
            if(hall==null||!hall.complete()||!store.complete())return;
            // Both rooms dug: every unit of theirs is a new block of the colony's own mound soil on its Young plan.
            var added=new HashSet<>(all);added.removeAll(before.getFirst());var young=planned(c,q,ColonyStage.YOUNG);
            c.assertTrue(store.deposited==24&&store.released==0&&hall.deposited==12&&hall.released==0&&added.size()==36&&young.containsAll(added),"All 36 units of the store and the hall lie on the Young mound: added="+added.size()+" outside="+added.stream().filter(b->!young.contains(b)).toList());
            long buried=added.stream().filter(planted.getFirst()::contains).count();
            c.assertTrue(buried>0,"Some of the soil buried witnessed grass: "+buried);
            var capacity=MoundSoil.capacity(MoundSoil.slots(l,plan,q.getUUID(),ColonyStage.YOUNG));
            PrimeAnts.LOGGER.info("T06 MOUND M1 DONE queen={} tick={} oldDepositRoomAtStore={} added={} buriedGrass={} mound={} young={} plantedGrass={} stage={}",q.getUUID(),c.getTick(),oldRoom[0],added.size(),buried,all.size(),capacity,planted.getFirst().size(),MoundSoil.stage(l,plan,q.getUUID()));
            c.succeed();
        });
    }

    /** m2. A player places a block on the cell of the Young mound the store's builder would fill next: it is never covered
     * or removed, and its column takes no soil above it. A save and reload while the builder carries soil, with some of the
     * store's soil already laid, keeps every unit exactly once: the job, the cargo and every mound block. */
    @GameTest(maxTicks=48000,structure="prime_ants_test:idle_ground")
    public void playerBlockOnTheMoundIsNeverCoveredAndMidDepositReloadKeepsEveryUnitOnce(GameTestHelper c){
        LasiusNigerEntity[] q={fx.start(c)};boolean[] supplied={false},reloaded={false};BlockPos[] player={null};
        c.onEachTick(()->{
            var l=c.getLevel();fx.grow(c,q[0],supplied);fx.soil(c,q[0]);
            var plan=q[0].founding().plan();if(plan==null||q[0].founding().phase()!=QueenFounding.Phase.SETTLED)return;
            route(c,q[0]);
            if(player[0]!=null){
                c.assertTrue(l.getBlockState(player[0]).is(Blocks.COBBLESTONE),"The player's block is never removed: "+l.getBlockState(player[0]));
                for(int up=1;up<=NestMound.LAYERS;up++)c.assertFalse(l.getBlockState(player[0].above(up)).is(NurseryBlocks.NEST_SOIL),"The player's block is never covered");
            }
            var store=NestPlanFixture.job(c,q[0],ChamberExcavation.STORE);if(store==null)return;
            if(player[0]==null){
                // A player sets a block on the free Young cell first in deposit order: the builder would fill it next.
                var next=MoundSoil.slots(l,plan,q[0].getUUID(),ColonyStage.YOUNG).stream().filter(s->s.state()==NestMound.Slot.FREE&&s.pos()!=null&&MoundSoil.takes(l,s.pos(),q[0].getUUID())).findFirst().orElseThrow();
                player[0]=next.pos().immutable();l.setBlock(player[0],Blocks.COBBLESTONE.defaultBlockState(),3);
                c.assertTrue(MoundSoil.read(l,player[0],q[0].getUUID())==NestMound.Read.OTHER&&!MoundSoil.takes(l,player[0],q[0].getUUID()),"A player's block is no mound cell to the colony");
                PrimeAnts.LOGGER.info("T06 MOUND PLAYER BLOCK queen={} cell={} layer={} tick={}",q[0].getUUID(),player[0],next.cell(),c.getTick());
            }
            var w=store.claim==null?null:fx.f.workers(c,q[0]).stream().filter(a->a.getUUID().equals(store.claim)).findFirst().orElse(null);
            if(!reloaded[0]&&w!=null&&store.deposited>=6&&w.getMainHandItem().is(Items.DIRT)&&w.workerTasks().phase()==WorkerTasks.Phase.DIG_OUT){
                // Mid-deposit save and reload: saved data from disk, then the builder and the queen as saved entities.
                var cargo=w.getMainHandItem().copy();int removed=store.removed(),deposited=store.deposited;var claim=store.claim;var mound=mound(c,q[0]);var laid=mound.iterator().next();
                l.getDataStorage().saveAndJoin();
                try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){
                    var excavation=disk.get(ChamberExcavation.TYPE);var terrain=disk.get(ColonyTerrain.TYPE);var expansion=disk.get(NestExpansion.TYPE);var registry=disk.get(ChamberRegistry.TYPE);var custody=disk.get(TransferCustody.TYPE);
                    c.assertTrue(excavation!=null&&terrain!=null&&expansion!=null&&registry!=null,"Disk holds the excavation, the terrain records, the widening and the registry");
                    l.getDataStorage().set(ChamberExcavation.TYPE,excavation);l.getDataStorage().set(ColonyTerrain.TYPE,terrain);l.getDataStorage().set(NestExpansion.TYPE,expansion);l.getDataStorage().set(ChamberRegistry.TYPE,registry);
                    if(custody!=null)l.getDataStorage().set(TransferCustody.TYPE,custody);
                }
                var loaded=fx.f.restore(c,w);q[0]=fx.f.restore(c,q[0]);
                var chunk=l.getChunkAt(laid);var serial=net.minecraft.world.level.chunk.storage.SerializableChunkData.copyOf(l,chunk);
                var read=net.minecraft.world.level.chunk.storage.SerializableChunkData.parse(l,l.palettedContainerFactory(),serial.write()).read(l,l.getPoiManager(),new net.minecraft.world.level.chunk.storage.RegionStorageInfo("test",l.dimension(),"chunk"),chunk.getPos());
                c.assertTrue(read.getBlockState(laid).is(NurseryBlocks.NEST_SOIL),"A saved chunk keeps a laid mound block");
                // T07: every mound block, every planned mound cell and the player's block, each read back from its own chunk
                // reconstructed from a save, equals the world as it was saved.
                var cells=new LinkedHashSet<BlockPos>(mound);cells.addAll(MoundSoil.cells(l,q[0].founding().plan(),q[0].getUUID()));cells.add(player[0]);
                var chunks=new HashMap<net.minecraft.world.level.ChunkPos,net.minecraft.world.level.chunk.ChunkAccess>();
                for(var b:cells){
                    var saved=chunks.computeIfAbsent(new net.minecraft.world.level.ChunkPos(b.getX()>>4,b.getZ()>>4),cp->{var live=l.getChunk(cp.x(),cp.z());
                        return net.minecraft.world.level.chunk.storage.SerializableChunkData.parse(l,l.palettedContainerFactory(),net.minecraft.world.level.chunk.storage.SerializableChunkData.copyOf(l,live).write())
                            .read(l,l.getPoiManager(),new net.minecraft.world.level.chunk.storage.RegionStorageInfo("test",l.dimension(),"chunk"),cp);});
                    c.assertTrue(saved.getBlockState(b).equals(l.getBlockState(b)),"A reconstructed chunk keeps "+b+": "+saved.getBlockState(b)+" vs "+l.getBlockState(b));
                }
                c.assertTrue(mound.stream().allMatch(b->chunks.get(new net.minecraft.world.level.ChunkPos(b.getX()>>4,b.getZ()>>4)).getBlockState(b).is(NurseryBlocks.NEST_SOIL))
                    &&chunks.get(new net.minecraft.world.level.ChunkPos(player[0].getX()>>4,player[0].getZ()>>4)).getBlockState(player[0]).is(Blocks.COBBLESTONE),"Every mound block is nest soil and the player's block cobblestone in the reconstructed chunks");
                PrimeAnts.LOGGER.info("T07 M2 RECONSTRUCTED CHUNKS queen={} moundBlocks={} cellsCompared={} chunks={}",q[0].getUUID(),mound.size(),cells.size(),chunks.keySet());
                var after=NestPlanFixture.job(c,q[0],ChamberExcavation.STORE);
                c.assertTrue(after!=store&&after.removed()==removed&&after.deposited==deposited&&claim.equals(after.claim)&&ItemStack.matches(loaded.getMainHandItem(),cargo)&&mound(c,q[0]).equals(mound),
                    "The job, the carried soil and every mound block reload exactly as they were: removed="+after.removed()+" deposited="+after.deposited+" cargo="+loaded.getMainHandItem());
                reloaded[0]=true;PrimeAnts.LOGGER.info("T06 MOUND MID-DEPOSIT RELOAD queen={} builder={} removed={} deposited={} cargo={} mound={}",q[0].getUUID(),claim,removed,deposited,cargo,mound.size());return;
            }
            if(reloaded[0])c.assertTrue(fx.f.workers(c,q[0]).stream().filter(a->a.getUUID().equals(store.claim)).count()<=1,"Never a duplicate builder identity after the reload");
            if(!reloaded[0]||!store.complete()||store.pendingMarker(l,q[0].getUUID())!=null)return;
            c.assertTrue(store.removed()==24&&store.deposited==24&&store.released==0,"The store's 24 units are all on the mound, each once: "+store.removed()+" "+store.deposited+" "+store.released);
            PrimeAnts.LOGGER.info("T06 MOUND M2 DONE queen={} tick={} player={} mound={}",q[0].getUUID(),c.getTick(),player[0],mound(c,q[0]).size());c.succeed();
        });
    }
}
