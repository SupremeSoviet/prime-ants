package dev.primeants.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Pair-specific lineage, never species/proximity/task identity. Placement must still count all bodies. */
public final class Nestmates {
    private Nestmates() { }
    public static boolean matching(Entity a, Entity b) {
        if (!(a instanceof LasiusNigerEntity first) || !(b instanceof LasiusNigerEntity second)) return false;
        var identity = first.colonyIdentity();
        return identity != null && identity.equals(second.colonyIdentity());
    }
    public static boolean movementClear(Level level, Entity actor, AABB box) {
        return level.noCollision(actor, box) && level.getEntities(actor, box, e -> e instanceof net.minecraft.world.entity.LivingEntity && !matching(actor, e)).isEmpty();
    }
}
