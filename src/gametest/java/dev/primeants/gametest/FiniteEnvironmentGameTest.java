package dev.primeants.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

/** Finite ledgers exclude ambient hostile spawns, while explicit mob fixtures retain real loot. */
public final class FiniteEnvironmentGameTest {
    @GameTest(maxTicks=200,structure="prime_ants_test:idle_ground")
    public void ambientMonstersAreDisabledButExplicitMobsKeepVanillaLoot(GameTestHelper c) {
        c.assertTrue(!c.getLevel().getGameRules().get(GameRules.SPAWN_MONSTERS),
                "Finite fixture budgets exclude unrecorded natural monster spawns");
        c.assertTrue(c.getLevel().getGameRules().get(GameRules.MOB_DROPS),
                "Explicit mob fixtures retain vanilla death drops");
        var cell=c.absolutePos(new BlockPos(3,2,3));
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)
            c.getLevel().setBlock(cell.offset(x,3,z),Blocks.STONE.defaultBlockState(),3);
        var zombie=EntityTypes.ZOMBIE.create(c.getLevel(),EntitySpawnReason.COMMAND);
        var chicken=EntityTypes.CHICKEN.create(c.getLevel(),EntitySpawnReason.COMMAND);
        c.assertTrue(zombie!=null&&chicken!=null,"Real explicit vanilla mobs created");
        var pos=Vec3.atBottomCenterOf(cell);
        zombie.setPos(pos.x,pos.y,pos.z);zombie.setNoAi(true);zombie.setPersistenceRequired();
        chicken.setPos(pos.x+2,pos.y,pos.z);chicken.setNoAi(true);chicken.setPersistenceRequired();
        c.assertTrue(c.getLevel().addFreshEntity(zombie)&&c.getLevel().addFreshEntity(chicken),
                "Disabling ambient spawning permits explicit fixture insertion");
        c.runAfterDelay(20,()->{
            c.assertTrue(zombie.isAlive()&&chicken.isAlive(),"Explicit mobs survive ordinary server ticks");
            chicken.hurtServer(c.getLevel(),chicken.damageSources().genericKill(),1000);
            c.assertTrue(!chicken.isAlive(),"Real lethal damage invokes vanilla mob death");
            c.runAfterDelay(20,()->{
                c.assertTrue(zombie.isAlive(),"Explicit hostile fixture remains alive");
                c.assertTrue(c.getLevel().getEntitiesOfClass(ItemEntity.class,c.getBounds().inflate(4),
                        item->item.isAlive()&&item.getItem().is(Items.CHICKEN)).stream()
                        .mapToInt(item->item.getItem().getCount()).sum()>=1,
                        "Vanilla death callback creates physical chicken loot");
                zombie.discard();c.succeed();
            });
        });
    }
}
