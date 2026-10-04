package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import net.minecraft.world.entity.PathfinderMob;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Test-only observation. No random calls, scheduling changes or goal mutations. */
public final class IdleTrace {
    private static final Set<UUID> tracked = new HashSet<>();
    public static void track(PathfinderMob mob) { tracked.add(mob.getUUID()); }
    public static boolean tracks(PathfinderMob mob) { return tracked.contains(mob.getUUID()); }
    public static void log(PathfinderMob mob, String event, Object detail) {
        if (tracks(mob)) PrimeAnts.LOGGER.info("T04 idle uuid={} age={} event={} {}", mob.getUUID(), mob.tickCount, event, detail);
    }
}
