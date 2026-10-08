package dev.primeants.colony;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

/** The single stage rule table (GDD v2 section 1, decision 23). Pure: callers supply live observations only.
 * <pre>
 * Stage     Requirements (cumulative: every lower row must hold too)                       Adult cap
 * FOUNDING  founding chamber registered (queen settled and laid her first clutch)         5
 * YOUNG     5+ living adults incl. the queen (4+ workers); nursery; food store            30
 * MATURE    25+ adults; queen's hall; material store; 4+ food units; 16+ clay units       60
 * GREAT     50+ adults; nursery, food store, material store and queen's hall at tier 2+;
 *           32+ stone units held (stored, or laid into the colony's own tier-3 walls)     120
 * </pre>
 * Every input is a {@link Bound}: known counts confirmed state only, possible adds unknown (unloaded) contributions.
 * A colony is promoted only to a stage certainly met and demoted only below a stage certainly unmet, so unknown
 * members or chambers change nothing. Founding is the floor. Each next adult threshold fits under the current cap.
 * <p>Hysteresis (the owner's decision of 2026-10-07): promotion is immediate, but a held stage drops only once one of its
 * requirements has been certainly unmet for {@link #GRACE} loaded ticks in a row, by that requirement's unmet-since clock.
 * Catastrophic losses apply at once: the queen observed dead, on its own and whatever else still holds (Young counts her
 * among its five adults, so the colony is Founding; stage-1 T08), a chamber function observed absent (no confirmed or
 * unknown chamber holds it), and adults certainly below the threshold of the stage beneath the one held, which drop the
 * colony to what its adults support. Shortfalls in counts, stocks and tiers wait: food, clay and stone, a chamber still confirmed
 * below the tier a stage needs, and adults below the held stage's threshold but not below the lower stage's. An unknown
 * requirement never demotes and runs no clock: its clock starts again once its shortfall is certain. */
public final class StageRules {
    private StageRules() { }
    /** Loaded ticks a shortfall that is not catastrophic must last before its stage drops: one Minecraft day. */
    public static final long GRACE = 24_000;
    public record Bound(int known, int possible) {
        public static final Bound NONE = new Bound(0, 0);
        public Bound { if (known < 0 || possible < known) throw new IllegalArgumentException("Invalid observation bound " + known + ".." + possible); }
        public static Bound exactly(int value) { return new Bound(value, value); }
        @Override public String toString() { return known == possible ? Integer.toString(known) : known + "(+" + (possible - known) + "?)"; }
    }
    /** Living adults including the queen, best confirmed tier per function (0 = none), stored units, and whether the queen
     * is observed dead (her body dead, or her recorded death while her lookup fails). */
    public record Inputs(Bound adults, Map<ChamberFunction, Bound> tiers, Bound food, Bound clay, Bound stone, boolean queenDead) {
        public Inputs {
            Objects.requireNonNull(adults); Objects.requireNonNull(food); Objects.requireNonNull(clay); Objects.requireNonNull(stone);
            var copy = new EnumMap<ChamberFunction, Bound>(ChamberFunction.class);
            for (var f : ChamberFunction.values()) {
                var tier = tiers.getOrDefault(f, Bound.NONE);
                if (tier.possible() > 3) throw new IllegalArgumentException("Chamber tier above 3 for " + f);
                copy.put(f, tier);
            }
            tiers = Collections.unmodifiableMap(copy);
        }
        /** A living or unknown queen. */
        public Inputs(Bound adults, Map<ChamberFunction, Bound> tiers, Bound food, Bound clay, Bound stone) { this(adults, tiers, food, clay, stone, false); }
        public Bound tier(ChamberFunction f) { return tiers.get(f); }
    }
    /** What a requirement counts: living adults, a chamber function at a tier, or a stock of units. */
    public enum Kind { ADULTS, CHAMBER, STOCK }
    public record Requirement(ColonyStage stage, String name, Kind kind, int need, Function<Inputs, Bound> input) {
        public Bound have(Inputs in) { return input.apply(in); }
        /** The key of this requirement's unmet-since clock, as saved (ChamberRegistry). */
        public String key() { return stage.serializedName() + ":" + name; }
        @Override public String toString() { return stage.serializedName() + ":" + name + ">=" + need; }
    }
    public record Missing(Requirement requirement, Bound have) {
        /** Not met by confirmed state alone, but an unknown contribution could meet it. */
        public boolean unknown() { return have.possible() >= requirement.need(); }
        @Override public String toString() { return requirement.name() + " " + have + "/" + requirement.need(); }
    }
    /** A shortfall that holds its stage for now: certainly unmet since the loaded tick since. */
    public record Pending(Requirement requirement, Bound have, long since) {
        @Override public String toString() { return requirement.key() + " " + have + "/" + requirement.need() + " since " + since; }
    }
    /** missing(target) lists every requirement of the target and of each lower stage that confirmed state does not
     * meet, lowest stage first. A stage with nothing missing is absent: every stage above the result is listed (except
     * while the queen is observed dead, which alone holds the colony at Founding), and one at or below it only while
     * unknowns or a running clock hold it. certain/possible are the evidence for holding a
     * stage. pending are the certain shortfalls the stage holds through, each with its clock; unmetSince is every
     * running clock, the state to save for the next evaluation. */
    public record Result(ColonyStage stage, ColonyStage certain, ColonyStage possible, Map<ColonyStage, List<Missing>> missing,
                         List<Pending> pending, Map<String, Long> unmetSince) {
        public List<Missing> missing(ColonyStage s) { return missing.getOrDefault(s, List.of()); }
    }
    private static Requirement adults(ColonyStage s, int n) { return new Requirement(s, "adults", Kind.ADULTS, n, Inputs::adults); }
    private static Requirement chamber(ColonyStage s, ChamberFunction f, int tier) {
        return new Requirement(s, tier == 1 ? f.serializedName() : f.serializedName() + "_tier", Kind.CHAMBER, tier, in -> in.tier(f));
    }
    private static Requirement stock(ColonyStage s, String name, int n, Function<Inputs, Bound> input) { return new Requirement(s, name, Kind.STOCK, n, input); }
    public static final List<Requirement> TABLE = List.of(
        adults(ColonyStage.YOUNG, 5), chamber(ColonyStage.YOUNG, ChamberFunction.NURSERY, 1), chamber(ColonyStage.YOUNG, ChamberFunction.FOOD_STORE, 1),
        adults(ColonyStage.MATURE, 25), chamber(ColonyStage.MATURE, ChamberFunction.QUEENS_HALL, 1), chamber(ColonyStage.MATURE, ChamberFunction.MATERIAL_STORE, 1),
        stock(ColonyStage.MATURE, "food", 4, Inputs::food), stock(ColonyStage.MATURE, "clay", 16, Inputs::clay),
        adults(ColonyStage.GREAT, 50), chamber(ColonyStage.GREAT, ChamberFunction.NURSERY, 2), chamber(ColonyStage.GREAT, ChamberFunction.FOOD_STORE, 2),
        chamber(ColonyStage.GREAT, ChamberFunction.MATERIAL_STORE, 2), chamber(ColonyStage.GREAT, ChamberFunction.QUEENS_HALL, 2),
        stock(ColonyStage.GREAT, "stone", 32, Inputs::stone));
    public static List<Requirement> requirements(ColonyStage s) { return TABLE.stream().filter(r -> r.stage() == s).toList(); }
    /** Every clock key a save may hold. */
    public static boolean clockKey(String key) { return TABLE.stream().anyMatch(r -> r.key().equals(key)); }
    /** Adults needed to enter a stage; the founding queen alone founds. */
    public static int minAdults(ColonyStage s) {
        return TABLE.stream().filter(r -> r.stage() == s && r.name().equals("adults")).mapToInt(Requirement::need).max().orElse(1);
    }
    static {
        // No deadlock: a colony must be able to grow to the next threshold under its current cap, also right after a regression.
        for (var s : ColonyStage.values()) if (s.next() != null && minAdults(s.next()) > s.adultCap())
            throw new IllegalStateException("Stage " + s.next() + " threshold exceeds the " + s + " adult cap");
    }
    /** The owner's rule where no clock has run yet: every certain shortfall is seen for the first time. */
    public static Result evaluate(ColonyStage previous, Inputs in) { return evaluate(previous, in, Map.of(), 0); }
    /** The owner's rule: unmetSince holds the clocks saved by the last evaluation (requirement key -> the loaded tick its
     * shortfall became certain) and now is the loaded tick of this one. */
    public static Result evaluate(ColonyStage previous, Inputs in, Map<String, Long> unmetSince, long now) {
        var from = previous == null ? ColonyStage.FOUNDING : previous;
        ColonyStage certain = ColonyStage.FOUNDING, possible = ColonyStage.FOUNDING;
        boolean certainChain = true, possibleChain = true;
        // Cumulative: a lower stage's gap stays in every higher target's list, ahead of that target's own rows.
        var missing = new ArrayList<Missing>();
        var report = new EnumMap<ColonyStage, List<Missing>>(ColonyStage.class);
        for (var s = ColonyStage.FOUNDING.next(); s != null; s = s.next()) {
            boolean metHere = true, possibleHere = true;
            for (var r : requirements(s)) {
                var have = r.have(in);
                if (have.known() < r.need()) { missing.add(new Missing(r, have)); metHere = false; }
                if (have.possible() < r.need()) possibleHere = false;
            }
            certainChain &= metHere; possibleChain &= possibleHere;
            if (certainChain) certain = s;
            if (possibleChain) possible = s;
            if (!missing.isEmpty()) report.put(s, List.copyOf(missing));
        }
        // The queen observed dead is catastrophic on its own (the owner's rule as written, stage-1 T08): Young counts her
        // among its five adults, so no stage above Founding is met, held or promoted to, whatever else still holds.
        if (in.queenDead()) { certain = ColonyStage.FOUNDING; possible = ColonyStage.FOUNDING; }
        // Promotion is immediate. A stage above what the inputs could support is held until, from the lowest stage up, a
        // stage has a certain shortfall that is catastrophic or has lasted GRACE: that stage is lost with all above it.
        var stage = from.compareTo(certain) < 0 ? certain : from;
        if (in.queenDead()) stage = ColonyStage.FOUNDING;
        else if (from.compareTo(possible) > 0) {
            for (var s = ColonyStage.YOUNG; s != null && s.compareTo(from) <= 0; s = s.next()) {
                boolean lost = false;
                for (var r : requirements(s)) {
                    var have = r.have(in);
                    if (have.possible() < r.need() && (catastrophic(r, have, in, from) || now - since(unmetSince, r, now) >= GRACE)) lost = true;
                }
                if (lost) { stage = ColonyStage.values()[s.ordinal() - 1]; break; }
            }
        }
        // Each certain shortfall at or below the stage held keeps its clock, or starts one now; a requirement met, unknown
        // or above the stage held has none.
        var clocks = new TreeMap<String, Long>(); var pending = new ArrayList<Pending>();
        for (var r : TABLE) {
            var have = r.have(in);
            if (r.stage().compareTo(stage) > 0 || have.possible() >= r.need()) continue;
            long since = since(unmetSince, r, now);
            clocks.put(r.key(), since); pending.add(new Pending(r, have, since));
        }
        return new Result(stage, certain, possible, Collections.unmodifiableMap(report), List.copyOf(pending), Collections.unmodifiableMap(clocks));
    }
    /** A certain shortfall that applies at once while a stage is held: the queen observed dead (which evaluate applies on
     * its own, with or without another shortfall); a chamber function no
     * confirmed or unknown chamber holds (observed absent: a breached shell, a missing or foreign marker, the hall without
     * its living queen); adults certainly below the threshold of the stage beneath the one held. */
    public static boolean catastrophic(Requirement r, Bound have, Inputs in, ColonyStage held) {
        if (in.queenDead()) return true;
        return switch (r.kind()) {
            case CHAMBER -> have.possible() == 0;
            case ADULTS -> held.ordinal() > 0 && in.adults().possible() < minAdults(ColonyStage.values()[held.ordinal() - 1]);
            case STOCK -> false;
        };
    }
    /** A requirement's saved clock; none, or one ahead of this loaded tick (a restarted counter), starts now. */
    private static long since(Map<String, Long> unmetSince, Requirement r, long now) {
        var since = unmetSince.get(r.key());
        return since == null || since > now ? now : since;
    }
}
