package dev.primeants.gametest;
import java.util.*;
public final class PlacementFault {
    private static final Map<UUID,Integer> blocked=new HashMap<>();
    public static void block(UUID id) {blocked.put(id,0);}
    public static int attempts(UUID id) {return blocked.getOrDefault(id,0);}
    public static void release(UUID id) {blocked.remove(id);}
    public static boolean refuse(UUID id) {if(!blocked.containsKey(id))return false;blocked.put(id,blocked.get(id)+1);return true;}
    private static final Map<UUID,Integer> insertedCalls=new HashMap<>();
    public static void watch(UUID id) {insertedCalls.put(id,0);}
    public static void insertion(UUID id) {if(insertedCalls.containsKey(id))insertedCalls.put(id,insertedCalls.get(id)+1);}
    public static int insertionCalls(UUID id) {return insertedCalls.getOrDefault(id,0);}
    private static final Map<String,Integer> finalGates=new HashMap<>();
    private static String key(net.minecraft.server.level.ServerLevel l,net.minecraft.core.BlockPos p) {return l.dimension().identifier()+":"+(p.getX()>>4)+":"+(p.getZ()>>4);}
    public static void blockFinal(net.minecraft.server.level.ServerLevel l,net.minecraft.world.level.ChunkPos p) {finalGates.put(l.dimension().identifier()+":"+p.x()+":"+p.z(),0);}
    public static boolean finalGate(net.minecraft.server.level.ServerLevel l,net.minecraft.core.BlockPos p) {String k=key(l,p);if(!finalGates.containsKey(k))return false;finalGates.put(k,finalGates.get(k)+1);return true;}
    public static int finalCalls(net.minecraft.server.level.ServerLevel l,net.minecraft.world.level.ChunkPos p) {return finalGates.getOrDefault(l.dimension().identifier()+":"+p.x()+":"+p.z(),0);}
    private record WriteFault(java.nio.file.Path file,String candidate,String status) {}
    private static final Map<WriteFault,Integer> writes=new HashMap<>();
    public static synchronized void blockWrite(java.nio.file.Path file,String candidate,String status) {writes.put(new WriteFault(file.toAbsolutePath().normalize(),candidate,status),0);}
    public static synchronized int writeFailures(java.nio.file.Path file,String candidate,String status) {return writes.getOrDefault(new WriteFault(file.toAbsolutePath().normalize(),candidate,status),0);}
    public static synchronized void releaseWrite(java.nio.file.Path file,String candidate,String status) {writes.remove(new WriteFault(file.toAbsolutePath().normalize(),candidate,status));}
    public static synchronized void write(net.minecraft.nbt.CompoundTag tag,java.nio.file.Path file) throws java.io.IOException {
        for(var entry:writes.entrySet()) {
            var f=entry.getKey();if(!f.file.equals(file.toAbsolutePath().normalize()))continue;
            var text=tag.getCompoundOrEmpty("data").getString(f.candidate);if(text.isEmpty())continue;
            var record=com.google.gson.JsonParser.parseString(text.get()).getAsJsonObject();
            if(!record.get("status").getAsString().equals(f.status))continue;
            entry.setValue(entry.getValue()+1);
            if(entry.getValue()==1 && java.nio.file.Files.exists(file)) {
                java.nio.file.Files.copy(file,file.resolveSibling("t15-before-failed-"+f.candidate+"-"+f.status+".dat"));
                java.nio.file.Files.writeString(file.resolveSibling("t15-attempted-"+f.candidate+"-"+f.status+".json"),record.toString());
            }
            dev.primeants.PrimeAnts.LOGGER.info("T15 scoped write fault file={} candidate={} attempted={}",file,f.candidate,record);
            throw new java.io.IOException("T15 fixture placement write failure: "+f.candidate+" "+f.status);
        }
    }
}
