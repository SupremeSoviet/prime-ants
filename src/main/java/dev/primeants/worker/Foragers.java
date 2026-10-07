package dev.primeants.worker;

/** How many foragers a colony keeps (stage-1 T07), pure so the queen's claims (QueenFounding), the trip's end
 * (WorkerTasks) and unit tests share it. One forager for every WORKERS_PER_FORAGER living workers, at least one: every
 * colony below twenty workers keeps exactly the one forager it always had, as 0.1.0's test colonies (at most ten workers)
 * and stage-1's earlier test colonies (at most seventeen) do. A further forager is claimed only while, without it, the
 * colony keeps CAREGIVERS_KEPT caregivers and, unless a builder is already at work, one more worker for the builder slot. */
public final class Foragers {
    public static final int WORKERS_PER_FORAGER = 10, CAREGIVERS_KEPT = 2;
    private Foragers() { }
    /** The foragers a colony of this many living workers keeps. */
    public static int target(long workers) { return (int) Math.max(1, workers / WORKERS_PER_FORAGER); }
    /** Caregivers a colony keeps beside its foragers: two, and one more for the builder slot while no builder works. */
    public static int kept(boolean builderClaimed) { return CAREGIVERS_KEPT + (builderClaimed ? 0 : 1); }
    /** Another forager may be claimed now: fewer claims than the target, and the caregivers left once the candidate leaves
     * them (caregiversAfter) are still the kept number. */
    public static boolean another(int claims, long workers, long caregiversAfter, boolean builderClaimed) {
        return claims < target(workers) && caregiversAfter >= kept(builderClaimed);
    }
    /** A further forager back from a trip keeps foraging while the claims are within the target and the caregivers still
     * number the kept ones; otherwise it returns to the colony's care. The first forager always keeps its claim. */
    public static boolean keep(int claims, long workers, long caregivers, boolean builderClaimed) {
        return claims <= target(workers) && caregivers >= kept(builderClaimed);
    }
}
