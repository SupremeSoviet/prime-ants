package dev.primeants.founding;

import java.util.Collection;
import java.util.Set;

/** The live integrity of one dug nest-plan space. One scan serves brood care (a job's completed cells, part of the
 * habitat) and the stage (a registered room's planned cells), so for a finished room both reach the same verdict.
 * Each dug cell must still be the colony's opening, and each face of a dug cell must be another dug cell, the founding
 * chamber it opens into, or solid dry ground. A cell and each of its faces are read only if their own chunk is loaded:
 * an unloaded dug cell never hides a loaded face beside it, so observed damage beats unavailable terrain (Findings). */
public final class DugSpace {
    /** What the scan reads: the live level in play, a table in unit tests. */
    public interface Ground<P> {
        boolean loaded(P p);
        /** Still this colony's worker opening; the plan's marker cell may instead hold the colony's own marker block. */
        boolean open(P p);
        /** Solid and dry. */
        boolean closed(P p);
        /** The i-th of a cell's six neighbours. */
        P face(P p, int i);
    }
    /** Reason labels of one room, built once: the scan runs per shell cell on every habitat check. */
    public record Labels(String unavailable, String revoked, String pendingBreach, String shellOpen) {
        public static Labels of(String room) {
            return new Labels(room + "_chunk_unavailable", room + "_completed_opening_revoked", room + "_unauthorized_pending_breach", room + "_shell_or_support_open");
        }
    }
    private DugSpace() { }
    /** Scans dug cells; planned cells not yet dug must stay closed like the shell. */
    public static <P> void scan(Ground<P> g, Collection<P> dug, Set<P> connections, Collection<P> planned, Labels labels, Findings r) {
        for (var p : dug) {
            if (r.cell(g.loaded(p), labels.unavailable()) && !g.open(p)) r.fault(labels.revoked());
            for (int i = 0; i < 6; i++) {
                var n = g.face(p, i);
                if (dug.contains(n) || connections.contains(n) || !r.cell(g.loaded(n), labels.unavailable())) continue;
                if (!g.closed(n)) r.fault(planned.contains(n) ? labels.pendingBreach() : labels.shellOpen());
            }
        }
    }
}
