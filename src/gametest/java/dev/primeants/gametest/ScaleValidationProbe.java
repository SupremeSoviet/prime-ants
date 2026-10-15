package dev.primeants.gametest;

import java.lang.management.ManagementFactory;

/** Opt-in real-tick diagnostic counters, absent from the production source set. */
public final class ScaleValidationProbe {
    public static final boolean ENABLED=System.getProperty("prime_ants.scalePhase", "").equals("profile");
    public static final String[] NAMES={"nurseryFindings","undergroundSurfaces","geometry","loaded","matches","generatedOpen","roomBuild","anyOpening","establishedFindings"};
    private static final long[] counts=new long[NAMES.length];
    private static final com.sun.management.ThreadMXBean THREADS=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
    private ScaleValidationProbe() { }
    public static void count(int i){if(ENABLED)counts[i]++;}
    public static long[] snapshot(){return counts.clone();}
    public static long allocated(){return THREADS.getThreadAllocatedBytes(Thread.currentThread().threadId());}
}
