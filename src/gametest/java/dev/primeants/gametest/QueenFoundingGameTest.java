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
    private void terrain(GameTestHelper c, BlockState material, boolean observed) {
        JsonObject records = NaturalSoil.CODEC.encodeStart(JsonOps.INSTANCE, NaturalSoil.get(c.getLevel())).getOrThrow().getAsJsonObject();
        for (int x = 2; x <= 12; x++) for (int z = 1; z <= 12; z++) for (int y = 1; y <= 4; y++) {
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
    private LasiusNigerEntity egg(GameTestHelper c) {
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
            for (BlockPos p : f.plan().deposits()) if (c.getLevel().getBlockState(p).is(Blocks.DIRT)) blocks++;
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
                var p = f.plan(); c.assertTrue(p != null && (p.tasks().contains(entry.getKey()) || p.deposits().contains(entry.getKey()) || p.plugs().contains(entry.getKey())), "No edit beyond declared areas: " + entry.getKey());
            }
        });
        c.succeedWhen(() -> {
            var f = queen.founding(); c.assertTrue(f.sealed(), "Queen must settle after two recovered-soil plug actions");
            c.assertTrue(open[0] && carrying[0] && moving[0] > 20, "Observe complete open route/headroom before intentional sealing, carrying and real movement");
            c.assertTrue(f.removed() == 24 && f.deposited() == 3 && f.plugged() == 2 && f.released() == 19 && f.carried() == 0, "24 = 3 deposited + 19 released + 2 plug");
            c.assertTrue(queen.position().distanceToSqr(Vec3.atBottomCenterOf(f.plan().chamber())) < 0.1, "Queen must navigate into chamber center");
            PrimeAnts.LOGGER.info("T05 SUCCESS uuid={} duration={} cadence={} multiplier={} movedTicks={} soil=24/0/3/19/2", queen.getUUID(), f.loadedTicks(), QueenFounding.cadence(), QueenFounding.multiplier(), moving[0]);
        });
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
