package dev.primeants.time;

import dev.primeants.entity.AntForm;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.worker.AdultHistory;
import dev.primeants.worker.ColonyMembers;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** One pre-step snapshot. No applied marker, queue, member polling or biological mutation. */
public record ReloadObservation(UUID queen, BlockPos nursery, ColonyClock.Interval interval, List<Member> members) {
    public ReloadObservation { nursery = nursery.immutable(); members = List.copyOf(members); }
    public record Member(UUID adult, UUID brood, UUID queen, BlockPos home, String availability,
                         boolean reserved, AdultSimulation.Snapshot evidence,
                         ColonyClock.Interval ownInterval, ColonyClock.Interval currentTimestampInterval,
                         AdultSimulation.Boundary returnBoundary, AdultSimulation.Coverage coverage) { }

    public static ReloadObservation capture(ServerLevel level, UUID queen, BlockPos nursery, BlockPos home,
                                            ColonyClock.Interval interval) {
        var rows = new ArrayList<Member>();
        boolean queenDead = AdultHistory.get(level).recorded(queen);
        rows.add(observe(level, queen, null, queen, home, !queenDead, queenDead, interval));
        for (var member : ColonyMembers.get(level).members(queen)) {
            if (!member.dead()) rows.add(observe(level, member.worker(), member.brood(), queen,
                member.home(), true, false, interval));
        }
        return new ReloadObservation(queen, nursery, interval, rows);
    }
    private static Member observe(ServerLevel level, UUID id, UUID brood, UUID queen, BlockPos home,
                                  boolean reserved, boolean dead, ColonyClock.Interval interval) {
        var entity = level.getEntity(id); // UUID lookup only: never requests terrain or scans world entities.
        String availability = dead ? "DEAD" : "UNKNOWN_UNAVAILABLE";
        AdultSimulation.Snapshot snapshot = null;
        AdultSimulation.Boundary boundary = null;
        if (!dead && entity instanceof LasiusNigerEntity ant && ant.isAlive() && !ant.isRemoved()) {
            boolean identity = brood == null ? ant.form() == AntForm.QUEEN && id.equals(ant.colonyIdentity())
                : ant.form() == AntForm.WORKER && brood.equals(ant.broodId())
                  && queen.equals(ant.queenId()) && home.equals(ant.nurseryHome());
            availability = identity ? "OBSERVED" : "UNKNOWN_IDENTITY_CONFLICT";
            if (identity) {
                snapshot = ant.simulationEvidence();
                boundary = ant.simulationBoundaryBefore(interval.beforeGameTime());
            }
        }
        return new Member(id, brood, queen, home.immutable(), availability, reserved, snapshot,
            boundary == null ? null : boundary.ownInterval(),
            snapshot == null ? null : snapshot.ownIntervalBefore(interval.beforeGameTime()),
            boundary == null || boundary.ownInterval().ticks() == 0 ? null : boundary,
            snapshot == null ? AdultSimulation.Coverage.unresolved(availability) : snapshot.reconcile(interval));
    }
}
