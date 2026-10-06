package dev.primeants.founding;

/** Every live check of one nest space, collected instead of stopping at the first problem. A fault seen in loaded
 * blocks beats unavailable terrain, so an unloaded cell can never hide observed damage; unavailable terrain alone stays
 * unknown. The first label of each kind is kept, in check order. */
public final class Findings {
    public enum Verdict { CLEAR, UNKNOWN, DAMAGED }
    private String fault, unavailable;
    /** Records a cell; an unloaded one is unavailable and must not be read. Returns whether it may be read. */
    public boolean cell(boolean loaded, String unavailableLabel) {
        if (!loaded) unavailable(unavailableLabel);
        return loaded;
    }
    public Findings fault(String label) { if (fault == null) fault = label; return this; }
    public Findings unavailable(String label) { if (unavailable == null) unavailable = label; return this; }
    public Findings add(Findings other) {
        if (other.fault != null) fault(other.fault);
        if (other.unavailable != null) unavailable(other.unavailable);
        return this;
    }
    public Findings copy() { return new Findings().add(this); }
    public String fault() { return fault; }
    /** The reported problem: the first observed fault, else the first unavailable cell, else null. */
    public String problem() { return fault != null ? fault : unavailable; }
    public Verdict verdict() { return fault != null ? Verdict.DAMAGED : unavailable != null ? Verdict.UNKNOWN : Verdict.CLEAR; }
}
