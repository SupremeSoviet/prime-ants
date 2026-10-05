package dev.primeants.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Read-only observer geometry; never changes terrain or an actor. */
public final class CargoView {
    public static Vec3 target(Vec3 position,double yaw){
        // Pinned LivingEntityRenderer flip/translation, AntModel root/head and AntSoilLayer
        // put the actual worker item centre 10.5/32 + .40/2 forward, at 1.501-24/16+8/32-1.2/32 high.
        double angle=Math.toRadians(yaw);return position.add(-Math.sin(angle)*0.528125,0.2135,Math.cos(angle)*0.528125);
    }
    public static Vec3 eye(BlockGetter level,Vec3 position,double yaw){
        double angle=Math.toRadians(yaw);var forward=new Vec3(-Math.sin(angle),0,Math.cos(angle));var side=new Vec3(forward.z,0,-forward.x);
        var target=target(position,yaw);
        for(double height:new double[]{0.65,1.0,1.6,2.5,3.5})for(int sign:new int[]{1,-1}){
            var p=position.add(forward.scale(1.8)).add(side.scale(sign*1.4)).add(0,height,0);if(clear(level,p,target))return p;
        }
        // A one-cell entrance lane can be surrounded by opaque spoil. Its own air column remains a real view.
        for(double height:new double[]{1.6,1.2,2.5,3.5,4.5}){
            var p=position.add(0,height,0);if(clear(level,p,target))return p;
        }
        return null;
    }
    public static boolean clear(BlockGetter level,Vec3 eye,Vec3 target){
        if(!level.getBlockState(BlockPos.containing(eye)).isAir())return false;
        // Test the item region, including visible foliage. A centre ray can pass through a
        // one-cell air lane while most of the carried cube is hidden behind the mound.
        for(double x:new double[]{-0.10,0,0.10})for(double y:new double[]{-0.08,0.08})for(double z:new double[]{-0.10,0.10})
            if(level.clip(new ClipContext(eye,target.add(x,y,z),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,CollisionContext.empty())).getType()!=HitResult.Type.MISS)return false;
        return true;
    }
}
