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
        // Pinned renderer flip/translation and the production worker jaw-tip dimensions.
        double reach=(10.75+3.75+2.3*Math.cos(.22))/32;
        double height=1.501-24/16.0+(8.5-.65)/32;
        double angle=Math.toRadians(yaw);return position.add(-Math.sin(angle)*reach,height,Math.cos(angle)*reach);
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
