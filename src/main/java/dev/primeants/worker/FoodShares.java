package dev.primeants.worker;

/** The food cache's admission rule (stage-1 T06), pure so the cache (NestCache), its foragers and unit tests share it.
 * Adults eat only sugar and larvae need protein, so the cache keeps RESERVED of its slots for each kind: one kind fills at
 * most capacity - RESERVED slots, and a cache full of one kind never keeps the other kind out. */
public final class FoodShares {
    public static final int CAPACITY = 6, RESERVED = 2;
    private FoodShares() { }
    /** The most units of one kind a cache of this capacity admits. */
    public static int share(int capacity) { return capacity - RESERVED; }
    /** A unit of this kind fits a cache of this capacity that holds this much sugar and protein: a free slot, and its own
     * kind below its share, so the other kind's reserved slots stay free. */
    public static boolean admits(int capacity, int sugar, int protein, boolean unitProtein) {
        return sugar + protein < capacity && (unitProtein ? protein : sugar) < share(capacity);
    }
    /** Its kind is below its share: a forager may pick the unit up for this cache (it waits with it while the cache is full). */
    public static boolean shareRoom(int capacity, int sugar, int protein, boolean unitProtein) {
        return (unitProtein ? protein : sugar) < share(capacity);
    }
}
