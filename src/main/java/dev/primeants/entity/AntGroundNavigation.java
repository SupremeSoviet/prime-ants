package dev.primeants.entity;

import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Keep a wide queen on each ground waypoint until clear of the corner's collision box. */
public final class AntGroundNavigation extends GroundPathNavigation {
    public AntGroundNavigation(Mob mob, Level level) { super(mob, level); }

    @Override
    protected void followThePath() {
        Vec3 position = getTempMobPos();
        Vec3i node = path.getNextNodePos();
        maxDistanceToWaypoint = 0.15F;
        if (Math.abs(mob.getX() - node.getX() - 0.5) < maxDistanceToWaypoint
                && Math.abs(mob.getZ() - node.getZ() - 0.5) < maxDistanceToWaypoint
                && Math.abs(mob.getY() - node.getY()) < getMaxVerticalDistanceToWaypoint()) {
            path.advance();
        }
        doStuckDetection(position);
    }
}
