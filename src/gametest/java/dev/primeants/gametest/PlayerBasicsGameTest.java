package dev.primeants.gametest;

import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;

/** Source-free controlled colony; mock survival body uses collision/gravity movement and normal server inventory drop. */
public final class PlayerBasicsGameTest {
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void survivalBodyEntersExistingStairsAndInventoryDropReachesOwnedCache(GameTestHelper c){
        var f=new WorkerForagingGameTest();var q=f.start(c);var player=(ServerPlayer)c.makeMockServerPlayer(GameType.SURVIVAL);
        boolean[] started={false},entered={false},carried={false};ItemEntity[] dropped={null};int[] ticks={0};
        c.onEachTick(()->{
            var p=q.founding().plan();if(p==null||q.founding().lifecycle()!=QueenFounding.Lifecycle.OPEN)return;
            if(!started[0]){
                started[0]=true;var v=Vec3.atBottomCenterOf(p.at(-3,0,1));player.setPos(v.x,v.y,v.z);player.setYRot(p.direction().toYRot());player.setXRot(65);
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE));player.drop(true);
                c.assertTrue(player.getMainHandItem().isEmpty()&&!player.isCreative()&&!player.isSpectator(),"Normal survival inventory loses one declared apple through ServerPlayer.drop");
                var items=c.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(p.outside()).inflate(6),i->i.isAlive()&&i.getItem().is(Items.APPLE));c.assertTrue(items.size()==1,"One normal world item after player inventory removal");dropped[0]=items.getFirst();
            }
            if(!entered[0]){
                Vec3 target=Vec3.atBottomCenterOf(p.at(2,0,-2));Vec3 delta=target.subtract(player.position());Vec3 horizontal=new Vec3(delta.x,0,delta.z);if(horizontal.lengthSqr()>.01)horizontal=horizontal.normalize().scale(.08);
                player.move(MoverType.SELF,horizontal.add(0,-.08,0));ticks[0]++;
                entered[0]=player.getY()<=p.entrance().getY()-1.8&&player.position().distanceToSqr(target)<.6;
                c.assertTrue(ticks[0]<400,"Supported ordinary collision/gravity traversal must enter within bound");
            }
            carried[0]|=f.workers(c,q).stream().anyMatch(w->w.getMainHandItem().is(Items.APPLE)&&q.founding().claimedBy(w)&&!w.isCallow());
            var n=f.cache(c,q);
            if(entered[0]&&carried[0]&&n!=null&&n.ownedBy(q.getUUID(),p)&&n.contents().stream().anyMatch(s->s.is(Items.APPLE))){
                c.assertTrue(!dropped[0].isAlive()&&java.util.stream.IntStream.range(0,3).allMatch(i->NestPlan.walkable(c.getLevel(),p.at(i,0,-i)))&&q.isAlive(),"Same world unit follows genuine forager -> owned cache; existing two-high nest remains intact");c.succeed();
            }
        });
    }
}
