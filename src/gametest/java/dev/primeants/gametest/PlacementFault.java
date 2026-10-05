package dev.primeants.gametest;
import java.util.*;
public final class PlacementFault {
    private static final Map<UUID,Integer> blocked=new HashMap<>();
    public static void block(UUID id) {blocked.put(id,0);}
    public static int attempts(UUID id) {return blocked.getOrDefault(id,0);}
    public static void release(UUID id) {blocked.remove(id);}
    public static boolean refuse(UUID id) {if(!blocked.containsKey(id))return false;blocked.put(id,blocked.get(id)+1);return true;}
}
