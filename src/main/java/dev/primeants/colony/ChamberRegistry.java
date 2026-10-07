package dev.primeants.colony;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.primeants.founding.NestPlan;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.*;

/** Per-colony chambers and the last evaluated stage, keyed by queen UUID. Every entry is a claim about the world:
 * ColonyDevelopment counts a function only while live blocks confirm it and recomputes the stage from live state. */
public final class ChamberRegistry extends SavedData {
    public static final String FOUNDING = "founding";
    public static final int MAX_CHAMBERS = 32, MAX_CELLS = 512;
    private static final Codec<UUID> ID = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    private static <E> Codec<E> named(java.util.function.Function<String, E> parse, java.util.function.Function<E, String> name) {
        return Codec.STRING.comapFlatMap(s -> { try { return DataResult.success(parse.apply(s)); } catch (IllegalArgumentException e) { return DataResult.error(e::getMessage); } }, name);
    }
    static final Codec<ChamberFunction> FUNCTION = named(ChamberFunction::byName, ChamberFunction::serializedName);
    static final Codec<ColonyStage> STAGE = named(ColonyStage::byName, ColonyStage::serializedName);
    /** Inclusive cell bounds, designated functions with their marker blocks, and the tier claimed by built work. */
    public record Chamber(String id, BlockPos min, BlockPos max, Set<ChamberFunction> functions, int tier, Map<ChamberFunction, BlockPos> markers) {
        static final Codec<Chamber> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(Chamber::id),
            BlockPos.CODEC.fieldOf("min").forGetter(Chamber::min),
            BlockPos.CODEC.fieldOf("max").forGetter(Chamber::max),
            FUNCTION.listOf().fieldOf("functions").forGetter(c -> List.copyOf(c.functions())),
            Codec.INT.fieldOf("tier").forGetter(Chamber::tier),
            Codec.unboundedMap(FUNCTION, BlockPos.CODEC).fieldOf("markers").forGetter(Chamber::markers)
        ).apply(i, (id, min, max, functions, tier, markers) -> {
            if (new HashSet<>(functions).size() != functions.size()) throw new IllegalArgumentException("Duplicate function in chamber " + id);
            return new Chamber(id, min, max, new HashSet<>(functions), tier, markers);
        }));
        public Chamber {
            if (id == null || id.isEmpty() || functions.isEmpty() || tier < 1 || tier > 3
                    || min.getX() > max.getX() || min.getY() > max.getY() || min.getZ() > max.getZ()
                    || (long)(max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1) > MAX_CELLS)
                throw new IllegalArgumentException("Invalid registered chamber " + id);
            min = min.immutable(); max = max.immutable();
            functions = Collections.unmodifiableSet(EnumSet.copyOf(functions));
            var copy = new EnumMap<ChamberFunction, BlockPos>(ChamberFunction.class);
            for (var e : markers.entrySet()) {
                var p = e.getValue();
                if (!functions.contains(e.getKey()) || p.getX() < min.getX() || p.getX() > max.getX() || p.getY() < min.getY() || p.getY() > max.getY()
                        || p.getZ() < min.getZ() || p.getZ() > max.getZ()) throw new IllegalArgumentException("Chamber marker outside its function or bounds " + id);
                copy.put(e.getKey(), p.immutable());
            }
            markers = Collections.unmodifiableMap(copy);
        }
    }
    public record Colony(UUID queen, BlockPos entrance, Direction direction, List<Chamber> chambers, ColonyStage stage) {
        static final Codec<Colony> CODEC = RecordCodecBuilder.create(i -> i.group(
            ID.fieldOf("queen").forGetter(Colony::queen),
            BlockPos.CODEC.fieldOf("entrance").forGetter(Colony::entrance),
            Direction.CODEC.fieldOf("direction").forGetter(Colony::direction),
            Chamber.CODEC.listOf().fieldOf("chambers").forGetter(Colony::chambers),
            STAGE.fieldOf("stage").forGetter(Colony::stage)
        ).apply(i, Colony::new));
        public Colony {
            var ids = new HashSet<String>();
            if (direction.getAxis().isVertical() || chambers.isEmpty() || chambers.size() > MAX_CHAMBERS || !chambers.stream().allMatch(c -> ids.add(c.id())) || !ids.contains(FOUNDING))
                throw new IllegalArgumentException("Invalid or duplicate registered chambers for colony " + queen);
            entrance = entrance.immutable(); chambers = List.copyOf(chambers);
        }
        public Chamber chamber(String id) { return chambers.stream().filter(c -> c.id().equals(id)).findFirst().orElse(null); }
    }
    public static final Codec<ChamberRegistry> CODEC = Colony.CODEC.listOf().xmap(ChamberRegistry::new, r -> List.copyOf(r.colonies.values()));
    public static final SavedDataType<ChamberRegistry> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath("prime_ants", "chamber_registry"), ChamberRegistry::new, CODEC, DataFixTypes.LEVEL);
    private final Map<UUID, Colony> colonies = new LinkedHashMap<>();
    public ChamberRegistry() { }
    private ChamberRegistry(List<Colony> list) { for (var c : list) if (colonies.putIfAbsent(c.queen(), c) != null) throw new IllegalArgumentException("Duplicate registered colony " + c.queen()); }
    public static ChamberRegistry get(ServerLevel l) { return l.getDataStorage().computeIfAbsent(TYPE); }
    public Colony colony(UUID queen) { return colonies.get(queen); }
    public List<Colony> colonies() { return List.copyOf(colonies.values()); }
    /** The 0.1.0 founding room as built, with no terrain change: tier 1, nursery and food-store markers, nothing else. */
    public static Chamber foundingChamber(NestPlan p) {
        BlockPos a = p.at(3, -1, -2), b = p.at(5, 1, -1);
        var markers = new EnumMap<ChamberFunction, BlockPos>(ChamberFunction.class);
        markers.put(ChamberFunction.NURSERY, p.nursery()); markers.put(ChamberFunction.FOOD_STORE, p.cache());
        return new Chamber(FOUNDING, new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ())),
            new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ())),
            EnumSet.of(ChamberFunction.NURSERY, ChamberFunction.FOOD_STORE), 1, markers);
    }
    /** Registers a colony's founding chamber once; an existing entry must describe the same nest. */
    public Colony found(UUID queen, NestPlan plan) {
        var expected = foundingChamber(plan);
        var existing = colonies.get(queen);
        if (existing == null) {
            var colony = new Colony(queen, plan.entrance(), plan.direction(), List.of(expected), ColonyStage.FOUNDING);
            colonies.put(queen, colony); setDirty(); return colony;
        }
        var saved = existing.chamber(FOUNDING);
        if (!existing.entrance().equals(plan.entrance()) || existing.direction() != plan.direction() || !saved.min().equals(expected.min())
                || !saved.max().equals(expected.max()) || !saved.functions().equals(expected.functions()) || !saved.markers().equals(expected.markers()))
            throw new IllegalStateException("Chamber registry conflict for colony " + queen);
        return existing;
    }
    /** Registers a chamber dug from the nest plan once. A saved entry with the same id must describe the same chamber;
     * a conflicting one is kept and reported. */
    public boolean register(UUID queen, Chamber chamber) {
        var c = colonies.get(queen);
        if (c == null) return false;
        var saved = c.chamber(chamber.id());
        if (saved != null) {
            if (saved.equals(chamber)) return true;
            dev.primeants.PrimeAnts.LOGGER.error("Chamber registry conflict queen={} saved={} built={}", queen, saved, chamber);
            return false;
        }
        var chambers = new ArrayList<>(c.chambers()); chambers.add(chamber);
        colonies.put(queen, new Colony(c.queen(), c.entrance(), c.direction(), chambers, c.stage())); setDirty();
        dev.primeants.PrimeAnts.LOGGER.info("Chamber registered queen={} chamber={} functions={} tier={} markers={}", queen, chamber.id(), chamber.functions(), chamber.tier(), chamber.markers());
        return true;
    }
    /** Raises the tier a chamber's finished upgrade work claims (ChamberUpgrade). A claim only: the confirmed tier is
     * always read from the walls, and the claim counts only while the chamber is unknown. */
    public void claimTier(UUID queen, String id, int tier) {
        var c = colonies.get(queen); var saved = c == null ? null : c.chamber(id);
        if (saved == null || tier <= saved.tier()) return;
        var chambers = new ArrayList<Chamber>();
        for (var ch : c.chambers()) chambers.add(ch.id().equals(id) ? new Chamber(ch.id(), ch.min(), ch.max(), ch.functions(), tier, ch.markers()) : ch);
        colonies.put(queen, new Colony(c.queen(), c.entrance(), c.direction(), chambers, c.stage())); setDirty();
        dev.primeants.PrimeAnts.LOGGER.info("Chamber tier claimed queen={} chamber={} tier={}", queen, id, tier);
    }
    void stage(UUID queen, ColonyStage stage) {
        var c = colonies.get(queen);
        colonies.put(queen, new Colony(c.queen(), c.entrance(), c.direction(), c.chambers(), stage)); setDirty();
    }
}
