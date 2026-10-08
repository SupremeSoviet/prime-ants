package dev.primeants.worker;

import dev.primeants.worker.MaterialUnits.Display;
import dev.primeants.worker.MaterialUnits.Material;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The store's visible state (MaterialUnits.display, shown by MaterialStoreBlock): over every content the room rule
 * admits, two stores look the same only when their totals are within seven units and they hold the same materials. */
class MaterialStoreDisplayTest {
    private static final Material[] OTHER = {Material.STONE, Material.GRAVEL, Material.SAND, Material.ORE};
    private static List<Material> held(int clay, int... other) {
        var out = new ArrayList<Material>(Collections.nCopies(clay, Material.CLAY));
        for (int i = 0; i < other.length; i++) out.addAll(Collections.nCopies(other[i], OTHER[i]));
        return out;
    }
    /** Every content a store can reach: up to 16 other units, up to 32 in all. */
    private static List<List<Material>> reachable() {
        var out = new ArrayList<List<Material>>();
        for (int st = 0; st <= 16; st++) for (int gr = 0; st + gr <= 16; gr++) for (int sa = 0; st + gr + sa <= 16; sa++) for (int ore = 0; st + gr + sa + ore <= 16; ore++)
            for (int clay = 0; clay + st + gr + sa + ore <= MaterialUnits.CAPACITY; clay++) out.add(held(clay, st, gr, sa, ore));
        return out;
    }
    private static Set<Material> kinds(List<Material> h) { return h.isEmpty() ? EnumSet.noneOf(Material.class) : EnumSet.copyOf(h); }

    @Test
    void equalDisplaysMeanTheSameMaterialsAndTotalsWithinSevenUnits() {
        var all = reachable();
        // Every reachable content is admitted by the room rule, added one unit at a time in any order of kinds.
        for (var h : List.of(held(32), held(16, 16, 0, 0, 0), held(16, 4, 4, 4, 4), held(0, 0, 0, 0, 16)))
            for (int n = 0; n < h.size(); n++) assertTrue(MaterialUnits.room(h.subList(0, n), h.get(n)), "reachable " + h);
        var span = new HashMap<Display, int[]>(); var sets = new HashMap<Display, Set<Material>>();
        for (var h : all) {
            var d = MaterialUnits.display(h);
            assertTrue(d.clay() >= 0 && d.clay() <= MaterialUnits.CLAY_LEVELS && d.stock() >= 0 && d.stock() <= MaterialUnits.STOCK_LEVELS, "levels in range " + d);
            span.merge(d, new int[]{h.size(), h.size()}, (a, b) -> new int[]{Math.min(a[0], b[0]), Math.max(a[1], b[1])});
            var previous = sets.putIfAbsent(d, kinds(h));
            assertTrue(previous == null || previous.equals(kinds(h)), "one display, one set of materials: " + d + " " + previous + " " + kinds(h));
        }
        span.forEach((d, s) -> assertTrue(s[1] - s[0] <= 7, "totals within seven units for " + d + ": " + s[0] + ".." + s[1]));
        // So stores whose totals differ by eight or more never look the same, up to the full 32.
        assertTrue(all.size() > 50_000, "every reachable content was checked: " + all.size());
    }

    @Test
    void theReviewersCasesNowLookDifferent() {
        assertNotEquals(MaterialUnits.display(held(16)), MaterialUnits.display(held(32)), "16 and 32 clay");
        var cobble = MaterialUnits.display(held(3, 3, 0, 0, 0)); var withCoal = MaterialUnits.display(held(3, 2, 0, 0, 1));
        assertNotEquals(cobble, withCoal, "3 clay + 3 cobblestone vs 3 clay + 2 cobblestone + 1 coal");
        assertEquals(new Display(1, 1, true, false, false, true), withCoal);
        assertEquals(new Display(8, 0, false, false, false, false), MaterialUnits.display(held(32)), "a full clay store shows eight levels");
        assertEquals(new Display(0, 0, false, false, false, false), MaterialUnits.display(List.of()), "an empty store shows the bare pad");
    }

    @Test
    void theStateCountStaysSmall() {
        // clay 0..8, stock 0..4, four presence lumps: the block's whole state space.
        assertEquals(720,(MaterialUnits.CAPACITY/MaterialUnits.LEVEL+1)*((MaterialUnits.CAPACITY-MaterialUnits.CLAY_SHARE)/MaterialUnits.LEVEL+1)*16,"Tier-one range stays intact");
        assertEquals(2448, (MaterialUnits.CLAY_LEVELS + 1) * (MaterialUnits.STOCK_LEVELS + 1) * 16,"Expanded projection is bounded");
        var shown = new HashSet<Display>(); reachable().forEach(h -> shown.add(MaterialUnits.display(h)));
        assertTrue(shown.size() <= 720, "reachable displays fit the state space: " + shown.size());
    }
    @Test
    void tierTwoDisplaysSeparateAllReachableTotalsAndMaterialSets() {
        // Projection depends only on clay count, total other count and presence, so one representative covers every allocation with those inputs.
        var spans=new HashMap<Display,int[]>();var sets=new HashMap<Display,Set<Material>>();int checked=0;
        for(int clay=0;clay<=64;clay++)for(int other=0;other<=32&&clay+other<=64;other++)for(int mask=0;mask<16;mask++){
            int kinds=Integer.bitCount(mask);if((other==0)!=(mask==0)||kinds>other)continue;
            int[] counts=new int[4];int first=-1;for(int k=0;k<4;k++)if((mask&(1<<k))!=0){counts[k]=1;if(first<0)first=k;}
            if(first>=0)counts[first]+=other-kinds;
            var h=held(clay,counts);var d=MaterialUnits.display(h);checked++;
            spans.merge(d,new int[]{h.size(),h.size()},(a,b)->new int[]{Math.min(a[0],b[0]),Math.max(a[1],b[1])});
            var previous=sets.putIfAbsent(d,kinds(h));assertTrue(previous==null||previous.equals(kinds(h)),"Exact kind set for "+d);
            assertTrue(d.clay()<=16&&d.stock()<=8,"Expanded range "+d);
        }
        spans.forEach((d,span)->assertTrue(span[1]-span[0]<=7,"Eight-unit differences always look different: "+d+" "+Arrays.toString(span)));
        assertTrue(checked>20000);assertTrue(spans.size()<=2448);
        assertNotEquals(MaterialUnits.display(held(32)),MaterialUnits.display(held(64)));
        assertEquals(new Display(16,0,false,false,false,false),MaterialUnits.display(held(64)));
    }

}
