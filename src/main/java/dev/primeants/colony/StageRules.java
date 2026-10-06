package dev.primeants.colony;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 * members or chambers change nothing. Founding is the floor. Each next adult threshold fits under the current cap. */
public final class StageRules {
    private StageRules() { }
    public record Bound(int known, int possible) {
        public static final Bound NONE = new Bound(0, 0);
        public Bound { if (known < 0 || possible < known) throw new IllegalArgumentException("Invalid observation bound " + known + ".." + possible); }
        public static Bound exactly(int value) { return new Bound(value, value); }
        @Override public String toString() { return known == possible ? Integer.toString(known) : known + "(+" + (possible - known) + "?)"; }
    }
    /** Living adults including the queen, best confirmed tier per function (0 = none), and stored units. */
    public record Inputs(Bound adults, Map<ChamberFunction, Bound> tiers, Bound food, Bound clay, Bound stone) {
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
        public Bound tier(ChamberFunction f) { return tiers.get(f); }
    }
    public record Requirement(ColonyStage stage, String name, int need, Function<Inputs, Bound> input) {
        public Bound have(Inputs in) { return input.apply(in); }
        @Override public String toString() { return stage.serializedName() + ":" + name + ">=" + need; }
    }
    public record Missing(Requirement requirement, Bound have) {
        /** Not met by confirmed state alone, but an unknown contribution could meet it. */
        public boolean unknown() { return have.possible() >= requirement.need(); }
        @Override public String toString() { return requirement.name() + " " + have + "/" + requirement.need(); }
    }
    /** missing(target) lists every requirement of the target and of each lower stage that confirmed state does not
     * meet, lowest stage first. A stage with nothing missing is absent: every stage above the result is listed, and one
     * at or below it only while unknowns hold it. certain/possible are the evidence for holding a stage. */
    public record Result(ColonyStage stage, ColonyStage certain, ColonyStage possible, Map<ColonyStage, List<Missing>> missing) {
        public List<Missing> missing(ColonyStage s) { return missing.getOrDefault(s, List.of()); }
    }
    private static Requirement adults(ColonyStage s, int n) { return new Requirement(s, "adults", n, Inputs::adults); }
    private static Requirement chamber(ColonyStage s, ChamberFunction f, int tier) {
        return new Requirement(s, tier == 1 ? f.serializedName() : f.serializedName() + "_tier", tier, in -> in.tier(f));
    }
    public static final List<Requirement> TABLE = List.of(
        adults(ColonyStage.YOUNG, 5), chamber(ColonyStage.YOUNG, ChamberFunction.NURSERY, 1), chamber(ColonyStage.YOUNG, ChamberFunction.FOOD_STORE, 1),
        adults(ColonyStage.MATURE, 25), chamber(ColonyStage.MATURE, ChamberFunction.QUEENS_HALL, 1), chamber(ColonyStage.MATURE, ChamberFunction.MATERIAL_STORE, 1),
        new Requirement(ColonyStage.MATURE, "food", 4, Inputs::food), new Requirement(ColonyStage.MATURE, "clay", 16, Inputs::clay),
        adults(ColonyStage.GREAT, 50), chamber(ColonyStage.GREAT, ChamberFunction.NURSERY, 2), chamber(ColonyStage.GREAT, ChamberFunction.FOOD_STORE, 2),
        chamber(ColonyStage.GREAT, ChamberFunction.MATERIAL_STORE, 2), chamber(ColonyStage.GREAT, ChamberFunction.QUEENS_HALL, 2),
        new Requirement(ColonyStage.GREAT, "stone", 32, Inputs::stone));
    public static List<Requirement> requirements(ColonyStage s) { return TABLE.stream().filter(r -> r.stage() == s).toList(); }
    /** Adults needed to enter a stage; the founding queen alone founds. */
    public static int minAdults(ColonyStage s) {
        return TABLE.stream().filter(r -> r.stage() == s && r.name().equals("adults")).mapToInt(Requirement::need).max().orElse(1);
    }
    static {
        // No deadlock: a colony must be able to grow to the next threshold under its current cap, also right after a regression.
        for (var s : ColonyStage.values()) if (s.next() != null && minAdults(s.next()) > s.adultCap())
            throw new IllegalStateException("Stage " + s.next() + " threshold exceeds the " + s + " adult cap");
    }
    public static Result evaluate(ColonyStage previous, Inputs in) {
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
        var stage = from.compareTo(certain) < 0 ? certain : from.compareTo(possible) > 0 ? possible : from;
        return new Result(stage, certain, possible, Collections.unmodifiableMap(report));
    }
}
