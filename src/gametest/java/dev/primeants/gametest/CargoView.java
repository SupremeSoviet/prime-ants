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
        double angle=Math.toRadians(yaw);return position.add(-Math.sin(angle)*0.25,0.25,Math.cos(angle)*0.25);
    }
    public static Vec3 eye(BlockGetter level,Vec3 position,double yaw){
        double angle=Math.toRadians(yaw);var forward=new Vec3(-Math.sin(angle),0,Math.cos(angle));var side=new Vec3(forward.z,0,-forward.x);
        var target=target(position,yaw);
        for(double height:new double[]{1.6,2.5,3.5})for(int sign:new int[]{1,-1}){
            var p=position.add(forward.scale(1.8)).add(side.scale(sign*1.4)).add(0,height,0);if(clear(level,p,target))return p;
        }
        // A one-cell entrance lane can be surrounded by opaque spoil. Its own air column remains a real view.
        for(double height:new double[]{1.6,1.2,2.5,3.5,4.5}){
            var p=position.add(0,height,0);if(clear(level,p,target))return p;
        }
        return null;
    }
    public static boolean clear(BlockGetter level,Vec3 eye,Vec3 target){return level.getBlockState(BlockPos.containing(eye)).isAir()
        &&level.clip(new ClipContext(eye,target,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,CollisionContext.empty())).getType()==HitResult.Type.MISS;}
}
