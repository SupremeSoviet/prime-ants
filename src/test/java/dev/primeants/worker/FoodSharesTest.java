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
    void aCacheSavedFullOfOneKindBeforeTheSharesTakesTheOtherKindOnceItIsEaten() {
        // Contents saved before T06 load as they were: six chickens admit nothing; two eaten, apples get in, chickens wait.
        assertFalse(FoodShares.admits(C, 0, 6, false) || FoodShares.admits(C, 0, 6, true));
        assertTrue(FoodShares.admits(C, 0, 5, false) && !FoodShares.admits(C, 0, 5, true), "five chickens: an apple takes the free slot, a chicken does not");
        assertTrue(FoodShares.admits(C, 0, 4, false) && !FoodShares.admits(C, 0, 4, true));
    }
}
