package dev.primeants.gametest;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.primeants.PrimeAnts;
import dev.primeants.entity.AntEntities;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.NaturalSoil;
import dev.primeants.founding.NestPlan;
import dev.primeants.founding.QueenFounding;
import dev.primeants.item.AntItems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import com.mojang.authlib.GameProfile;

/** Controlled origin fixtures only here. No manual ticks, entity movement, plan completion or mutation shortcuts. */
public final class QueenFoundingGameTest {
    private static final BlockPos ENTRANCE = new BlockPos(7, 4, 4);
    void terrain(GameTestHelper c, BlockState material, boolean observed) {
        terrain(c, material, observed, false);
    }
    void terrain(GameTestHelper c, BlockState material, boolean observed, boolean wide) {
        JsonObject records = NaturalSoil.CODEC.encodeStart(JsonOps.INSTANCE, NaturalSoil.get(c.getLevel())).getOrThrow().getAsJsonObject();
        for (int x = wide ? 1 : 2; x <= (wide ? 14 : 12); x++) for (int z = 1; z <= (wide ? 14 : 12); z++) for (int y = 1; y <= 4; y++) {
            c.setBlock(x, y, z, material);
            if (observed) records.addProperty(Long.toString(c.absolutePos(new BlockPos(x,y,z)).asLong()), BuiltInRegistries.BLOCK.getKey(material.getBlock()).toString());
        }
        if (observed) c.getLevel().getDataStorage().set(NaturalSoil.TYPE, NaturalSoil.CODEC.parse(JsonOps.INSTANCE, records).getOrThrow());
    }
    private Player player(GameTestHelper c) {
        return new Player(c.getLevel(), new GameProfile(UUID.randomUUID(), "founding-fixture")) {
            public GameType gameMode() { return GameType.CREATIVE; }
            public boolean isClientAuthoritative() { return false; }
            public PermissionSet permissions() { return PermissionSet.ALL_PERMISSIONS; }
        };
    }
    LasiusNigerEntity egg(GameTestHelper c) {
        BlockPos p = c.absolutePos(ENTRANCE); BlockState before = c.getLevel().getBlockState(p); Player player = player(c);
        ItemStack egg = new ItemStack(AntItems.DEBUG_QUEEN_EGG); player.setItemInHand(InteractionHand.MAIN_HAND, egg);
        c.assertTrue(egg.useOn(new UseOnContext(c.getLevel(), player, InteractionHand.MAIN_HAND, egg,
                new BlockHitResult(Vec3.atCenterOf(p).add(0,0.5,0), Direction.UP, p, false))).consumesAction(), "Actual authorized egg must succeed");
        var ants = c.getLevel().getEntitiesOfClass(LasiusNigerEntity.class, c.getBounds());
        c.assertTrue(ants.size() == 1 && ants.getFirst().founding().phase() == QueenFounding.Phase.SEEKING, "One queen must request production founding");
        c.assertEntityNotPresent(AntEntities.WORKER);
        c.assertTrue(c.getLevel().getBlockState(p).equals(before), "Egg interaction must not edit terrain");
        return ants.getFirst();
    }
    private Map<BlockPos, BlockState> snapshot(GameTestHelper c) {
        Map<BlockPos, BlockState> states = new HashMap<>();
        for (int x = 0; x < 16; x++) for (int y = 0; y < 8; y++) for (int z = 0; z < 16; z++) {
            BlockPos p = c.absolutePos(new BlockPos(x,y,z)); states.put(p, c.getLevel().getBlockState(p));
        }
        return states;
    }
    private void balance(GameTestHelper c, LasiusNigerEntity queen) {
        var f = queen.founding();
        c.assertTrue(f.removed() == f.carried() + f.deposited() + f.released() + f.plugged(), "Every removed soil unit must have a physical owner");
        int blocks = 0;
        if (f.plan() != null) {
            for (BlockPos p : f.plan().deposits()) if (c.getLevel().getBlockState(p).is(dev.primeants.brood.NurseryBlocks.NEST_SOIL)) blocks++;
            c.assertTrue(blocks == f.deposited(), "Recorded deposits must be real blocks");
            for (BlockPos p : f.plan().plugs().subList(0, f.plugged())) if (c.getLevel().getBlockState(p).is(Blocks.DIRT))
                c.assertTrue(!NaturalSoil.get(c.getLevel()).eligible(c.getLevel(), p), "Plugged recovered soil must remain non-natural");
        }
        int items = c.getLevel().getEntitiesOfClass(ItemEntity.class, c.getBounds()).stream().filter(i -> i.getItem().is(Items.DIRT)).mapToInt(i -> i.getItem().getCount()).sum();
        c.assertTrue(items == f.released(), "Released units must exist as actual items, including merged stacks");
    }
    @GameTest(maxTicks=30000, structure="prime_ants_test:idle_ground")
    public void foundingExcavatesTransportsWalksAndSealsOverRealTicks(GameTestHelper c) {
        terrain(c, Blocks.DIRT.defaultBlockState(), true); var before = snapshot(c); var queen = egg(c); Vec3 start = queen.position();
        int[] previous = {0}, moving = {0}; boolean[] open = {false}, carrying = {false};
        c.onEachTick(() -> {
            var f = queen.founding();
            c.assertTrue(f.phase() != QueenFounding.Phase.FAILED, "Founding failed: " + f.reason() + " pos=" + queen.position());
            c.assertTrue(f.removed() - previous[0] <= 1 && f.removed() >= previous[0], "Excavation must be incremental, at most one block per tick/action");
            previous[0] = f.removed();
            if (queen.position().distanceToSqr(start) > 0.04) moving[0]++;
            if (f.carried() > 0 && f.phase() == QueenFounding.Phase.TRANSPORTING) carrying[0] = true;
            if (f.phase() == QueenFounding.Phase.ENTERING && f.removed() == 24) { open[0] = f.plan().openWalkable(c.getLevel()); }
            c.assertTrue(c.getLevel().noCollision(queen, queen.getBoundingBox().deflate(0.001)), "Living queen must not intersect terrain");
            if (c.getTick() % 200 == 0) PrimeAnts.LOGGER.info("T05 founding trace tick={} phase={} progress={} pos={} reason={}", c.getTick(), f.phase(), f.removed(), queen.position(), f.reason());
            balance(c, queen);
            for (var entry : before.entrySet()) if (!entry.getValue().equals(c.getLevel().getBlockState(entry.getKey()))) {
                var p = f.plan(); c.assertTrue(p != null && (p.tasks().contains(entry.getKey()) || p.deposits().contains(entry.getKey()) || p.plugs().contains(entry.getKey())
                        || p.undergroundSurfaces().contains(entry.getKey()) && c.getLevel().getBlockState(entry.getKey()).is(dev.primeants.brood.NurseryBlocks.NEST_SOIL)), "No edit beyond declared areas: " + entry.getKey());
            }
        });
        c.succeedWhen(() -> {
            var f = queen.founding(); c.assertTrue(f.sealed(), "Queen must settle after two recovered-soil plug actions");
            c.assertTrue(open[0] && carrying[0] && moving[0] > 20, "Observe complete open route/headroom before intentional sealing, carrying and real movement");
            c.assertTrue(f.removed() == 24 && f.deposited() == 22 && f.plugged() == 2 && f.released() == 0 && f.carried() == 0, "24 = 22 deposited + 0 released + 2 plugs");
            c.assertTrue(queen.position().distanceToSqr(Vec3.atBottomCenterOf(f.plan().chamber())) < 0.1, "Queen must navigate into chamber center");
            for(int forward=3;forward<=5;forward++)for(int side=-1;side<=1;side++)
                c.assertTrue(c.getLevel().getBlockState(f.plan().at(forward,side,-3)).is(dev.primeants.brood.NurseryBlocks.NEST_SOIL),"Native dirt floor must be prepared before grass can spread into it");
            PrimeAnts.LOGGER.info("T05 SUCCESS uuid={} duration={} cadence={} multiplier={} movedTicks={} soil=24/0/22/0/2", queen.getUUID(), f.loadedTicks(), QueenFounding.cadence(), QueenFounding.multiplier(), moving[0]);
        });
    }
    @GameTest(maxTicks=30000, structure="prime_ants_test:idle_ground")
    public void sideWallOpenedDuringSealingPreventsSettlement(GameTestHelper c) { sealingIntervention(c, false); }
    @GameTest(maxTicks=30000, structure="prime_ants_test:idle_ground")
    public void transportFinishesVerticalColumnBeforeLeavingExposedPendingSoil(GameTestHelper c) {
        terrain(c,Blocks.DIRT.defaultBlockState(),true);var queen=egg(c);boolean[] transported={false};
        c.onEachTick(()->{
            var f=queen.founding();c.assertTrue(f.phase()!=QueenFounding.Phase.FAILED,"Physical column job: "+f.reason());balance(c,queen);
            if(f.phase()==QueenFounding.Phase.TRANSPORTING&&f.removed()>0&&f.removed()<f.plan().tasks().size()) {
                transported[0]=true;var previous=f.plan().tasks().get(f.removed()-1);var next=f.plan().tasks().get(f.removed());
                c.assertTrue(!(previous.getX()==next.getX()&&previous.getZ()==next.getZ()&&previous.getY()==next.getY()+1),"Do not leave a half-excavated vertical column exposing a pending native target during a long soil trip");
            }
            if(f.sealed()) {c.assertTrue(transported[0]&&f.removed()==24&&f.deposited()==22&&f.plugged()==2&&f.released()==0,"Same physical 24=22+2 after complete-column transport");PrimeAnts.LOGGER.info("T08 COLUMN founding loadedTicks={} cadence={} multiplier={} soil=24/22/2",f.loadedTicks(),QueenFounding.cadence(),QueenFounding.multiplier());c.succeed();}
        });
    }
    @GameTest(maxTicks=30000, structure="prime_ants_test:idle_ground")
    public void placedPlugRemovedBeforeSettlementPreventsSettlement(GameTestHelper c) { sealingIntervention(c, true); }
    private void sealingIntervention(GameTestHelper c, boolean removePlug) {
        terrain(c, Blocks.DIRT.defaultBlockState(), true); var queen = egg(c);
        boolean[] edited = {false}, checked = {false}; BlockPos[] breach = {null};
        c.onEachTick(() -> {
            var f = queen.founding();
            if (!edited[0] && f.phase() == QueenFounding.Phase.SEALING && f.plugged() == (removePlug ? 2 : 1)) {
                edited[0] = true; breach[0] = removePlug ? f.plan().plugs().getFirst() : f.plan().at(4, 2, -2);
                c.assertTrue(c.getLevel().getBlockState(breach[0]).is(Blocks.DIRT)
                        || !removePlug && c.getLevel().getBlockState(breach[0]).is(dev.primeants.brood.NurseryBlocks.NEST_SOIL), "Intervene on an existing physical soil block (plugs remain dirt)");
                c.getLevel().setBlock(breach[0], Blocks.AIR.defaultBlockState(), 3);
                PrimeAnts.LOGGER.info("T06 intervention kind={} tick={} phase={} removed={} placedPlugs={} breach={} queen={}",
                        removePlug ? "plug" : "side_wall", c.getTick(), f.phase(), f.removed(), f.plugged(), breach[0], queen.getUUID());
            }
            if (edited[0] && f.phase() == QueenFounding.Phase.SETTLED) {
                PrimeAnts.LOGGER.error("T06 FALSE SETTLEMENT kind={} tick={} phase={} reason={} sealed={} livePlugs={} shell={}",
                        removePlug ? "plug" : "side_wall", c.getTick(), f.phase(), f.reason(), f.sealed(),
                        f.plan().plugs().stream().filter(p -> c.getLevel().getBlockState(p).is(Blocks.DIRT)).count(), f.plan().enclosedChamber(c.getLevel()));
            }
            c.assertTrue(f.phase() != QueenFounding.Phase.SETTLED, "Breached enclosure must not announce successful settlement: " + f.reason());
            if (!checked[0] && f.phase() == QueenFounding.Phase.FAILED) {
                checked[0] = true;
                c.assertTrue(edited[0] && f.reason().equals(removePlug ? "enclosure_plug_missing" : "enclosure_shell_open"), "Explicit breach diagnostic: " + f.reason());
                var frozen = snapshot(c); int removed = f.removed(), carried = f.carried(), placed = f.plugged();
                c.runAfterDelay(80, () -> {
                    c.assertTrue(!f.sealed() && f.phase() == QueenFounding.Phase.FAILED, "Failed enclosure remains unready");
                    c.assertTrue(f.removed() == removed && f.carried() == carried && f.plugged() == placed && frozen.equals(snapshot(c)), "Failure must not excavate, refund soil or repair external edits");
                    balance(c, queen);
                    long livePlugs = f.plan().plugs().stream().filter(p -> c.getLevel().getBlockState(p).is(Blocks.DIRT)).count();
                    int externallyRemoved = removePlug ? 1 : 0;
                    c.assertTrue(f.removed() == 24 && f.deposited() == 22 && f.released() == 0 && f.carried() == 0 && f.plugged() == 2
                            && livePlugs == 2 - externallyRemoved && 24 == 22 + 0 + livePlugs + externallyRemoved,
                            "Physical accounting: historical placements are not live blocks; one externally removed plug belongs to external actor");
                    c.assertTrue(c.getLevel().getBlockState(breach[0]).isAir(), "External breach stays open");
                    PrimeAnts.LOGGER.info("T06 intervention prevented kind={} reason={} soil=24/0/22/0 placed={} live={} externallyRemoved={} unchangedTicks=80",
                            removePlug ? "plug" : "side_wall", f.reason(), placed, livePlugs, externallyRemoved); c.succeed();
                });
            }
        });
    }
    @GameTest(maxTicks=30000, structure="prime_ants_test:idle_ground")
    public void chamberObstructedDuringSealingPreventsSettlement(GameTestHelper c) {
        terrain(c, Blocks.DIRT.defaultBlockState(), true); var queen = egg(c); boolean[] edited = {false}, checked = {false};
        c.onEachTick(() -> {
            var f = queen.founding();
            if (!edited[0] && f.phase() == QueenFounding.Phase.SEALING && f.plugged() == 2) {
                edited[0] = true; c.getLevel().setBlock(f.plan().at(5, 1, -1), Blocks.STONE.defaultBlockState(), 3);
            }
            c.assertTrue(f.phase() != QueenFounding.Phase.SETTLED, "Obstructed chamber must not complete");
            if (!checked[0] && f.phase() == QueenFounding.Phase.FAILED) {
                checked[0] = true; c.assertTrue(edited[0] && f.reason().equals("enclosure_chamber_obstructed"), "Explicit chamber diagnostic: " + f.reason());
                var frozen = snapshot(c);
                c.runAfterDelay(80, () -> {
                    c.assertTrue(!f.sealed() && f.removed() == 24 && frozen.equals(snapshot(c)), "No obstruction repair or further excavation");
                    balance(c, queen); PrimeAnts.LOGGER.info("T06 obstructed completion prevented reason={} unchangedTicks=80", f.reason()); c.succeed();
                });
            }
        });
    }
    @GameTest(maxTicks=30000, structure="prime_ants_test:idle_ground")
    public void invalidSettledEnclosureStaysUnreadyThroughSaveLoad(GameTestHelper c) {
        terrain(c, Blocks.DIRT.defaultBlockState(), true); LasiusNigerEntity[] queen = {egg(c)}; boolean[] restored = {false};
        c.onEachTick(() -> {
            var f = queen[0].founding();
            c.assertTrue(f.phase() != QueenFounding.Phase.FAILED, "Intact founding must complete before intervention: " + f.reason());
            if (!restored[0] && f.sealed()) {
                restored[0] = true; var level = c.getLevel(); BlockPos obstruction = f.plan().at(5, 1, -1);
                level.setBlock(obstruction, Blocks.STONE.defaultBlockState(), 3);
                c.assertTrue(f.phase() == QueenFounding.Phase.SETTLED && !f.sealed()
                        && f.reason().equals("settled_not_ready_enclosure_chamber_obstructed"), "Historical SETTLED must immediately stop exposing readiness");
                var chunk = level.getChunkAt(obstruction);
                var serial = net.minecraft.world.level.chunk.storage.SerializableChunkData.copyOf(level, chunk);
                var parsed = net.minecraft.world.level.chunk.storage.SerializableChunkData.parse(level, level.palettedContainerFactory(), serial.write());
                var reloadedChunk = parsed.read(level, level.getPoiManager(), new net.minecraft.world.level.chunk.storage.RegionStorageInfo("test", level.dimension(), "chunk"), chunk.getPos());
                c.assertTrue(reloadedChunk.getBlockState(obstruction).is(Blocks.STONE), "FULL chunk serialization retains actual obstruction");
                level.getDataStorage().saveAndJoin();
                try (var disk = new net.minecraft.world.level.storage.SavedDataStorage(
                        net.minecraft.world.level.dimension.DimensionType.getStorageFolder(level.dimension(), level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),
                        net.minecraft.util.datafix.DataFixers.getDataFixer(), level.registryAccess())) {
                    var loaded = disk.get(NaturalSoil.TYPE);
                    c.assertTrue(loaded != null && !loaded.eligible(level, f.plan().plugs().getFirst()), "Disk reload must not mint plug origin");
                    level.getDataStorage().set(NaturalSoil.TYPE, loaded);
                }
                TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
                c.assertTrue(queen[0].save(output), "Serialize settled queen through vanilla path");
                CompoundTag tag = output.buildResult(); UUID id = queen[0].getUUID(); queen[0].discard();
                queen[0] = (LasiusNigerEntity)EntityType.loadEntityRecursive(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag), level, EntitySpawnReason.LOAD, e -> e);
                c.assertTrue(queen[0] != null && queen[0].getUUID().equals(id) && queen[0].founding().phase() == QueenFounding.Phase.SETTLED
                        && !queen[0].founding().sealed() && queen[0].founding().reason().equals("settled_not_ready_enclosure_chamber_obstructed"), "Restored historical completion cannot grant current readiness");
                c.assertTrue(level.tryAddFreshEntityWithPassengers(queen[0]), "Restore exactly one settled queen");
                var frozen = snapshot(c);
                c.runAfterDelay(80, () -> {
                    var loaded = queen[0].founding();
                    c.assertTrue(loaded.phase() == QueenFounding.Phase.SETTLED && !loaded.sealed()
                            && loaded.reason().equals("settled_not_ready_enclosure_chamber_obstructed"), "Live ticks preserve historical phase and explicit present diagnostic");
                    c.assertTrue(loaded.removed() == 24 && loaded.plugged() == 2 && frozen.equals(snapshot(c)), "No post-load work, duplication or automatic repair");
                    balance(c, queen[0]);
                    // A second external edit also revokes readiness; placement history stays two.
                    level.setBlock(loaded.plan().plugs().getFirst(), Blocks.AIR.defaultBlockState(), 3);
                    c.assertTrue(!loaded.sealed() && loaded.reason().equals("settled_not_ready_enclosure_plug_missing"), "Historical plug counter cannot prove a live seal");
                    PrimeAnts.LOGGER.info("T06 invalid SETTLED entity/chunk serialization + SavedData disk reload: phase={} reason={} unchangedTicks=80; no process restart", loaded.phase(), loaded.reason()); c.succeed();
                });
            }
        });
    }
    @GameTest(maxTicks=30000, structure="prime_ants_test:idle_ground")
    public void settledReadinessRequiresQueenInside(GameTestHelper c) {
        terrain(c, Blocks.DIRT.defaultBlockState(), true); var queen = egg(c); boolean[] displaced = {false};
        c.onEachTick(() -> {
            var f = queen.founding(); c.assertTrue(f.phase() != QueenFounding.Phase.FAILED, "Queen must first found entirely through real ticks: " + f.reason());
            if (!displaced[0] && f.sealed()) {
                displaced[0] = true; var frozen = snapshot(c); Vec3 outside = Vec3.atBottomCenterOf(f.plan().outside());
                // External displacement AFTER genuine completion; never a shortcut to excavation, entry or settlement.
                queen.teleportTo(outside.x, outside.y, outside.z);
                c.assertTrue(f.phase() == QueenFounding.Phase.SETTLED && !f.sealed()
                        && f.reason().equals("settled_not_ready_enclosure_queen_not_inside"), "An intact nest without its queen inside is not currently ready");
                c.runAfterDelay(80, () -> {
                    c.assertTrue(!f.sealed() && f.phase() == QueenFounding.Phase.SETTLED && frozen.equals(snapshot(c)), "Historical settlement does not move the queen back or repair terrain");
                    balance(c, queen);
                    PrimeAnts.LOGGER.info("T06 externally displaced settled queen remains unready reason={} unchangedTicks=80", f.reason()); c.succeed();
                });
            }
        });
    }
    @GameTest(maxTicks=60)
    public void genuineNativeSoilGenerationRecordsAndRevokesOrigin(GameTestHelper c) {
        var dimension = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("prime_ants_test", "native_soil"));
        var level = c.getLevel().getServer().getLevel(dimension);
        c.assertTrue(level != null, "Development-only native-soil dimension must be declared by resolved test-world preset");
        c.assertTrue(level.getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource, "Declared vanilla flat generator");
        var generator = (net.minecraft.world.level.levelgen.FlatLevelSource)level.getChunkSource().getGenerator();
        var layers = generator.settings().getLayers();
        c.assertTrue(layers.size() == 8 && layers.getFirst().is(Blocks.BEDROCK) && layers.getLast().is(Blocks.GRASS_BLOCK)
                && layers.subList(1, 7).stream().allMatch(s -> s.is(Blocks.DIRT)), "Resolved settings: bedrock 1, dirt 6, grass 1; no assumed default surface");
        BlockPos far = new BlockPos(4096, level.getMinY() + 7, 4096);
        var region = net.minecraft.world.level.dimension.DimensionType.getStorageFolder(dimension,
                level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("region/r.8.8.mca");
        PrimeAnts.LOGGER.info("T06 positive fixture dimension={} newRegion={} exists={}", dimension.identifier(), region.toAbsolutePath(), java.nio.file.Files.exists(region));
        c.assertTrue(!java.nio.file.Files.exists(region)
                && level.getChunkSource().getChunk(256, 256, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false) == null,
                "Request must begin with genuinely new terrain, absent on disk and in live FULL cache");
        var chunk = (net.minecraft.world.level.chunk.LevelChunk)level.getChunkSource().getChunk(256, 256, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true);
        var soil = NaturalSoil.get(level);
        c.assertTrue(level.getBlockState(far).is(Blocks.GRASS_BLOCK) && soil.eligible(level, far)
                && level.getBlockState(far.below()).is(Blocks.DIRT) && soil.eligible(level, far.below()), "Positive native grass AND dirt must be recorded by unmodified generation witness/observer");
        int recorded = 0;
        for (int x = 4096; x < 4112; x++) for (int z = 4096; z < 4112; z++) for (int y = level.getMinY() + 1; y <= far.getY(); y++)
            if (soil.eligible(level, new BlockPos(x, y, z))) recorded++;
        c.assertTrue(recorded == 1792, "All 7 supported layers of this new FULL chunk must have positive origin");
        level.setBlock(far, Blocks.AIR.defaultBlockState(), 3); level.setBlock(far, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        c.assertTrue(!soil.eligible(level, far) && soil.eligible(level, far.below()), "Mutation revokes only edited origin");
        var serial = net.minecraft.world.level.chunk.storage.SerializableChunkData.copyOf(level, chunk);
        var parsed = net.minecraft.world.level.chunk.storage.SerializableChunkData.parse(level, level.palettedContainerFactory(), serial.write());
        var reloaded = parsed.read(level, level.getPoiManager(), new net.minecraft.world.level.chunk.storage.RegionStorageInfo("test", dimension, "chunk"), chunk.getPos());
        c.assertTrue(reloaded.getBlockState(far).is(Blocks.GRASS_BLOCK) && !soil.eligible(level, far) && soil.eligible(level, far.below()), "Ordinary FULL chunk loading cannot regrant revoked permission");
        PrimeAnts.LOGGER.info("T06 POSITIVE genuine generation dimension={} chunk={} layers=bedrock1/dirt6/grass1 recorded={} grass=true dirt=true mutated=false ordinaryReload=false", dimension.identifier(), chunk.getPos(), recorded);
        c.succeed();
    }
    @GameTest(maxTicks=50, structure="prime_ants_test:idle_ground") public void unknownOriginSoilRemainsUntouched(GameTestHelper c) { protectedSite(c, false, false); }
    @GameTest(maxTicks=50, structure="prime_ants_test:idle_ground") public void preQueenPlayerPlacedMatchingSoilRemainsUntouched(GameTestHelper c) { protectedSite(c, true, false); }
    @GameTest(maxTicks=50, structure="prime_ants_test:idle_ground") public void unsuitableSubstratesRemainUntouched(GameTestHelper c) { protectedSite(c, true, true); }
    private void protectedSite(GameTestHelper c, boolean recorded, boolean unsuitable) {
        terrain(c, Blocks.DIRT.defaultBlockState(), recorded);
        if (recorded) for (int x=2;x<=12;x++) for (int z=1;z<=12;z++) for (int y=1;y<=4;y++)
            c.setBlock(x,y,z, unsuitable ? Blocks.STONE.defaultBlockState() : Blocks.DIRT.defaultBlockState()); // same-state LevelChunk placement revokes, before queen exists
        var before = snapshot(c); var queen = egg(c);
        c.runAfterDelay(12, () -> {
            c.assertTrue(queen.founding().phase() == QueenFounding.Phase.FAILED && queen.founding().reason().startsWith("no_verified"), "Unsuitable/unknown/replaced site must explain rejection");
            c.assertTrue(queen.founding().removed() == 0 && before.equals(snapshot(c)), "Rejected sites must remain untouched"); c.succeed();
        });
    }
    @GameTest(maxTicks=100, structure="prime_ants_test:idle_ground") public void ordinaryPlayerReplacementInvalidatesPlannedTask(GameTestHelper c) {
        terrain(c, Blocks.DIRT.defaultBlockState(), true); var queen = egg(c);
        c.runAfterDelay(2, () -> {
            var plan = queen.founding().plan(); c.assertTrue(plan != null && queen.founding().removed() == 0, "Plan must precede physical work");
            BlockPos target = plan.tasks().getFirst(); c.getLevel().setBlock(target, Blocks.AIR.defaultBlockState(), 3);
            Player p = player(c); p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT));
            c.assertTrue(p.getMainHandItem().useOn(new UseOnContext(c.getLevel(), p, InteractionHand.MAIN_HAND, p.getMainHandItem(),
                    new BlockHitResult(Vec3.atCenterOf(target.below()).add(0,0.5,0), Direction.UP, target.below(), false))).consumesAction(), "Ordinary vanilla BlockItem use must replace planned soil");
            c.runAfterDelay(10, () -> {
                c.assertTrue(!NaturalSoil.get(c.getLevel()).eligible(c.getLevel(), target), "Matching player replacement must revoke origin");
                c.assertTrue(queen.founding().phase() == QueenFounding.Phase.FAILED && queen.founding().removed() == 0
                        && c.getLevel().getBlockState(target).is(Blocks.DIRT), "Planned replaced block stays untouched with diagnostic failure"); c.succeed();
            });
        });
    }
    @GameTest(maxTicks=60) public void genuineGenerationAndChunkReloadCannotGrantUnknownOrigin(GameTestHelper c) {
        var level = c.getLevel(); BlockPos far = c.absolutePos(new BlockPos(512, 0, 512));
        var chunk = (net.minecraft.world.level.chunk.LevelChunk)level.getChunkSource().getChunk(far.getX()>>4, far.getZ()>>4, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true);
        int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,far.getX(),far.getZ())-1;
        BlockPos soil = new BlockPos(far.getX(),top,far.getZ());
        c.assertTrue(level.getBlockState(soil).is(Blocks.SANDSTONE) && !NaturalSoil.get(level).eligible(level,soil), "Genuinely generated unsupported test sandstone must stay ineligible");
        BlockState original = Blocks.DIRT.defaultBlockState();
        level.setBlock(soil, Blocks.AIR.defaultBlockState(), 3); level.setBlock(soil, original, 3);
        c.assertTrue(!NaturalSoil.get(level).eligible(level, soil), "Removed/replaced native material cannot regain permission");
        var serial = net.minecraft.world.level.chunk.storage.SerializableChunkData.copyOf(level,chunk);
        var parsed = net.minecraft.world.level.chunk.storage.SerializableChunkData.parse(level, level.palettedContainerFactory(), serial.write());
        var reloaded = parsed.read(level, level.getPoiManager(), new net.minecraft.world.level.chunk.storage.RegionStorageInfo("test",level.dimension(),"chunk"), chunk.getPos());
        c.assertTrue(reloaded.getBlockState(soil).equals(original) && !NaturalSoil.get(level).eligible(level,soil), "Ordinary FULL chunk serialization/loading preserves terrain without minting origin");
        c.assertTrue(!NaturalSoil.material(Blocks.STONE.defaultBlockState()) && !NaturalSoil.material(Blocks.SAND.defaultBlockState())
                && !NaturalSoil.material(Blocks.GRAVEL.defaultBlockState()) && !NaturalSoil.material(Blocks.WATER.defaultBlockState())
                && !NaturalSoil.material(Blocks.CHEST.defaultBlockState()), "Stone, falling soils, liquids and block entities are ineligible");
        PrimeAnts.LOGGER.info("T05 real generation + FULL chunk serialization protection checked at {}",soil); c.succeed();
    }
    @GameTest(maxTicks=30000, structure="prime_ants_test:idle_ground") public void interruptedFoundingAndProtectionPersistThroughSaveLoad(GameTestHelper c) {
        terrain(c, Blocks.DIRT.defaultBlockState(), true); LasiusNigerEntity[] queen = {egg(c)}; boolean[] restored = {false};
        c.onEachTick(() -> {
            var f = queen[0].founding(); c.assertTrue(f.phase() != QueenFounding.Phase.FAILED, "Restored job failure: " + f.reason());
            if (!restored[0] && f.removed() >= 3 && f.carried() > 0) {
                restored[0] = true; BlockPos protectedBlock = c.absolutePos(new BlockPos(2,1,1));
                c.getLevel().setBlock(protectedBlock, Blocks.DIRT.defaultBlockState(), 3);
                c.getLevel().getDataStorage().saveAndJoin();
                // A new production SavedDataStorage reads the real disk file; running process stays alive.
                try (var disk = new net.minecraft.world.level.storage.SavedDataStorage(
                        net.minecraft.world.level.dimension.DimensionType.getStorageFolder(c.getLevel().dimension(), c.getLevel().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),
                        net.minecraft.util.datafix.DataFixers.getDataFixer(), c.getLevel().registryAccess())) {
                    NaturalSoil loaded = disk.get(NaturalSoil.TYPE);
                    c.assertTrue(loaded != null && !loaded.eligible(c.getLevel(), protectedBlock)
                            && loaded.eligible(c.getLevel(), f.plan().tasks().get(f.removed())), "Disk origin reload must retain revoked and positive permissions");
                    c.getLevel().getDataStorage().set(NaturalSoil.TYPE, loaded);
                }
                TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, c.getLevel().registryAccess());
                c.assertTrue(queen[0].save(output), "Queen serializes through normal entity path"); CompoundTag tag = output.buildResult();
                UUID id = queen[0].getUUID(); int carried = f.carried(), removed = f.removed(), released = f.released(), deposited = f.deposited(); queen[0].discard();
                queen[0] = (LasiusNigerEntity)EntityType.loadEntityRecursive(TagValueInput.create(ProblemReporter.DISCARDING, c.getLevel().registryAccess(), tag), c.getLevel(), EntitySpawnReason.LOAD, e -> e);
                c.assertTrue(queen[0] != null && queen[0].getUUID().equals(id) && queen[0].founding().removed() == removed
                        && queen[0].founding().carried() == carried && queen[0].founding().released() == released && queen[0].founding().deposited() == deposited, "Phase/tasks/progress/stack/UUID must restore without duplicate actions");
                c.assertTrue(c.getLevel().tryAddFreshEntityWithPassengers(queen[0]), "Restore one queen");
            }
            balance(c, queen[0]);
        });
        c.succeedWhen(() -> {
            c.assertTrue(restored[0] && queen[0].founding().sealed(), "Interrupted physical job must finish through real ticks");
            c.assertTrue(c.getLevel().getEntitiesOfClass(LasiusNigerEntity.class,c.getBounds()).size()==1, "No duplicate queen");
            PrimeAnts.LOGGER.info("T05 entity + SavedData DISK restoration completed; no full process restart; removed={}", queen[0].founding().removed());
        });
    }
    @GameTest(maxTicks=1000, structure="prime_ants_test:idle_ground") public void queenDeathReleasesCarriedSoilOnceAndStopsWork(GameTestHelper c) {
        terrain(c, Blocks.DIRT.defaultBlockState(), true); var queen=egg(c); boolean[] killed={false};
        c.onEachTick(() -> {
            if (!killed[0] && queen.founding().carried()>0) {
                killed[0]=true; int removed=queen.founding().removed(); queen.hurtServer(c.getLevel(),queen.damageSources().generic(),1000);
                queen.die(queen.damageSources().generic()); // repeated normal death entry must not release twice
                c.runAfterDelay(30, () -> {
                    c.assertTrue(queen.isRemoved() && queen.founding().removed()==removed && queen.founding().carried()==0, "Death must stop all work and clear released stack");
                    balance(c,queen); c.assertEntityNotPresent(AntEntities.QUEEN); c.assertEntityNotPresent(AntEntities.WORKER); c.succeed();
                });
            }
        });
    }
}
