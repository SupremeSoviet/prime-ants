package dev.primeants.founding;
/** Transient witness: only a successfully executed new TERRAIN generation step sets this. */
public interface GenerationWitness {
    boolean primeAntsGeneratedTerrain();
    void primeAntsGeneratedTerrain(boolean value);
}
