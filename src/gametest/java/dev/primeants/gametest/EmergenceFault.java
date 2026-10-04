package dev.primeants.gametest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Development-only insertion refusal, keyed to one genuinely raised clutch. Never creates brood/adults. */
public final class EmergenceFault {
    private static final Map<UUID, Integer> refusals = new HashMap<>();
    public static void block(UUID queen) { refusals.put(queen, 0); }
    public static int attempts(UUID queen) { return refusals.getOrDefault(queen, 0); }
    public static void release(UUID queen) { refusals.remove(queen); }
    public static boolean refuse(UUID queen) {
        if (!refusals.containsKey(queen)) return false;
        refusals.put(queen, refusals.get(queen) + 1); return true;
    }
}
