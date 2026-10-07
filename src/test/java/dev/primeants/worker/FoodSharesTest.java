package dev.primeants.worker;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The food cache's admission rule (stage-1 T06): a cache full of one kind never keeps the other kind out. */
class FoodSharesTest {
    private static final int C = FoodShares.CAPACITY;

    @Test
    void eachKindFillsAtMostItsShareAndTheOtherKindStillGetsIn() {
        assertEquals(List.of(6, 2, 4), List.of(C, FoodShares.RESERVED, FoodShares.share(C)));
        // Chickens stop at four; apples still come in until the cache is full.
        assertTrue(FoodShares.admits(C, 0, 3, true));
        assertFalse(FoodShares.admits(C, 0, 4, true), "a fifth chicken waits on the ground");
        assertTrue(FoodShares.admits(C, 0, 4, false) && FoodShares.admits(C, 1, 4, false), "apples are admitted beside four chickens");
        assertFalse(FoodShares.admits(C, 2, 4, false), "a full cache admits nothing");
        // And the same the other way round.
        assertFalse(FoodShares.admits(C, 4, 0, false));
        assertTrue(FoodShares.admits(C, 4, 0, true) && FoodShares.admits(C, 4, 1, true));
        // A forager picks up only a kind below its share; at a full cache it waits with it, as in 0.1.0.
        assertTrue(FoodShares.shareRoom(C, 3, 3, true) && !FoodShares.admits(C, 3, 3, true));
        assertFalse(FoodShares.shareRoom(C, 0, 4, true));
    }

    @Test
    void noSequenceOfDepositsAndMealsLocksEitherKindOut() {
        // Every state reachable from an empty cache by admitted deposits and any withdrawals: neither kind ever holds more
        // than its share, and whenever the cache has a free slot, a unit of each kind below its share gets in.
        var seen = new HashSet<List<Integer>>(); var queue = new ArrayDeque<List<Integer>>(List.of(List.of(0, 0)));
        while (!queue.isEmpty()) {
            var s = queue.remove(); if (!seen.add(s)) continue;
            int sugar = s.get(0), protein = s.get(1);
            assertTrue(sugar <= FoodShares.share(C) && protein <= FoodShares.share(C) && sugar + protein <= C, "reachable " + s);
            if (sugar + protein < C) {
                assertTrue(protein == FoodShares.share(C) || FoodShares.admits(C, sugar, protein, true), "protein gets in: " + s);
                assertTrue(sugar == FoodShares.share(C) || FoodShares.admits(C, sugar, protein, false), "sugar gets in: " + s);
                if (protein == FoodShares.share(C)) assertTrue(FoodShares.admits(C, sugar, protein, false), "a cache full of chickens still takes apples: " + s);
                if (sugar == FoodShares.share(C)) assertTrue(FoodShares.admits(C, sugar, protein, true), "a cache full of apples still takes chickens: " + s);
            }
            if (FoodShares.admits(C, sugar, protein, false)) queue.add(List.of(sugar + 1, protein));
            if (FoodShares.admits(C, sugar, protein, true)) queue.add(List.of(sugar, protein + 1));
            if (sugar > 0) queue.add(List.of(sugar - 1, protein));
            if (protein > 0) queue.add(List.of(sugar, protein - 1));
        }
        assertEquals(22, seen.size(), "every split of up to six units with at most four of a kind: " + seen);
    }

    @Test
    void aCacheSavedFullOfOneKindBeforeTheSharesShedsItsExcessWhenTheOtherKindArrives() {
        // Contents saved before T06 load as they were: admission alone takes nothing into six chickens; two gone, apples get
        // in, chickens wait.
        assertFalse(FoodShares.admits(C, 0, 6, false) || FoodShares.admits(C, 0, 6, true));
        assertTrue(FoodShares.admits(C, 0, 5, false) && !FoodShares.admits(C, 0, 5, true), "five chickens: an apple takes the free slot, a chicken does not");
        assertTrue(FoodShares.admits(C, 0, 4, false) && !FoodShares.admits(C, 0, 4, true));
        // T07: the cache sheds the units beyond the share when the other kind arrives (NestCache, through transfer custody),
        // so the other kind never waits for them to be eaten.
        assertEquals(2, FoodShares.shed(C, 0, 6, false), "six chickens shed two for an apple");
        assertEquals(2, FoodShares.shed(C, 6, 0, true), "six apples shed two for a chicken");
        assertEquals(1, FoodShares.shed(C, 1, 5, false), "five chickens and an apple shed one chicken for a second apple");
        assertEquals(0, FoodShares.shed(C, 0, 6, true), "a chicken sheds nothing: its own kind is over the share and it waits");
        assertEquals(0, FoodShares.shed(C, 0, 5, false), "a free slot takes the apple without shedding");
        assertEquals(0, FoodShares.shed(C, 2, 4, false), "a full cache within the shares sheds nothing; a third apple waits as in 0.1.0");
        // Every split of up to six units: a unit of a kind below its share gets in after shedding unless the cache is full
        // within the shares (then it waits, as in 0.1.0); a kind sheds exactly down to its share, never below it.
        for (int sugar = 0; sugar <= C; sugar++) for (int protein = 0; sugar + protein <= C; protein++) for (boolean unitProtein : new boolean[]{false, true}) {
            int n = FoodShares.shed(C, sugar, protein, unitProtein), s = unitProtein ? sugar - n : sugar, p = unitProtein ? protein : protein - n;
            if (FoodShares.shareRoom(C, sugar, protein, unitProtein))
                assertEquals(sugar + protein < C || n > 0, FoodShares.admits(C, s, p, unitProtein), "after shedding " + n + " from " + sugar + "/" + protein);
            if (n > 0) assertEquals(FoodShares.share(C), unitProtein ? s : p, "sheds down to the share exactly: " + sugar + "/" + protein);
        }
    }
}
