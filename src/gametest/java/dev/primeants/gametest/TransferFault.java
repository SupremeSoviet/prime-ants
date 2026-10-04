package dev.primeants.gametest;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.item.ItemEntity;

/** Development-only HEAD refusal, scoped to a unique component on the intended source stock. */
public final class TransferFault {
    private static final Map<String,int[]> faults=new HashMap<>();
    private static final Map<net.minecraft.world.phys.Vec3,int[]> positions=new HashMap<>();
    public static void blockAt(net.minecraft.world.phys.Vec3 position) {positions.put(position,new int[]{0,0,0});}
    public static int refusedAt(net.minecraft.world.phys.Vec3 position) {return positions.get(position)[2];}
    public static void releaseAt(net.minecraft.world.phys.Vec3 position) {positions.remove(position);}
    public static void block(String name,int successes) { faults.put(name,new int[]{successes,0,0}); }
    public static void release(String name) { faults.remove(name); }
    public static int refused(String name) { return faults.get(name)[2]; }
    public static boolean refuse(ItemEntity item) {
        var name=item.getItem().get(DataComponents.CUSTOM_NAME);
        var f=name==null?null:faults.get(name.getString());
        if(f==null)f=positions.get(item.position());if(f==null)return false;
        f[1]++;if(f[1]<=f[0])return false;
        f[2]++;return true;
    }
}
