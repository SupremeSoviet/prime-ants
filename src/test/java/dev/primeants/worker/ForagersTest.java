package dev.primeants.worker;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Foragers scale with the colony (stage-1 T07): one per ten living workers, at least one, with caregivers kept. */
class ForagersTest {
    @Test
    void oneForagerForEveryTenWorkersAndExactlyOneBelowTwenty() {
        assertEquals(10, Foragers.WORKERS_PER_FORAGER); assertEquals(2, Foragers.CAREGIVERS_KEPT);
        // 0.1.0's test colonies (at most ten workers) and stage-1's earlier ones (at most seventeen) keep exactly one.
        for (int workers = 0; workers < 20; workers++) assertEquals(1, Foragers.target(workers), workers + " workers");
        assertEquals(2, Foragers.target(20)); assertEquals(2, Foragers.target(29));
        assertEquals(3, Foragers.target(30)); assertEquals(5, Foragers.target(59), "Mature's cap of 60 adults: five foragers");
        assertEquals(11, Foragers.target(119), "Great's cap of 120 adults: eleven foragers");
        for (int workers = 0; workers < 200; workers++) assertTrue(Foragers.target(workers + 1) >= Foragers.target(workers), "never fewer for more workers");
    }

    @Test
    void aFurtherForagerLeavesTwoCaregiversAndTheBuilderSlot() {
        assertTrue(Foragers.another(1, 20, 3, false), "twenty workers: a second forager while three caregivers stay");
        assertFalse(Foragers.another(1, 20, 2, false), "two caregivers left and no builder yet: the builder slot is kept");
        assertTrue(Foragers.another(1, 20, 2, true), "a builder already works: two caregivers suffice");
        assertFalse(Foragers.another(1, 20, 1, true), "never fewer than two caregivers");
        assertFalse(Foragers.another(2, 29, 10, false), "at the target");
        assertFalse(Foragers.another(1, 19, 10, false), "nineteen workers keep one forager");
        // Back from a trip, a further forager keeps its claim while within the target and the caregivers are kept.
        assertTrue(Foragers.keep(2, 20, 3, false)); assertTrue(Foragers.keep(2, 20, 2, true));
        assertFalse(Foragers.keep(2, 19, 10, false), "the colony shrank below twenty workers");
        assertFalse(Foragers.keep(2, 25, 2, false), "the caregivers fell to two with no builder: it returns to their care");
    }
}
