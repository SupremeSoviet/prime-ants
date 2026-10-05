package dev.primeants.gametest;

import dev.primeants.entity.*;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Isolated physics fixtures; no supplied actor is used to obtain traffic/native recovery. */
public final class NestmateGameTest {
    private void ground(GameTestHelper c) {for(int x=1;x<15;x++)for(int z=1;z<15;z++)c.setBlock(x,1,z,Blocks.STONE);}
    private LasiusNigerEntity actor(GameTestHelper c,AntForm form,UUID lineage,double x,double z) {
        var q=(form==AntForm.QUEEN?AntEntities.QUEEN:AntEntities.WORKER).create(c.getLevel(),EntitySpawnReason.EVENT);
        c.assertTrue(q!=null,"Physics actor factory");q.setNoAi(true);
        var p=c.absolutePos(new BlockPos((int)x,2,(int)z));q.setPos(p.getX()+x-(int)x,p.getY(),p.getZ()+z-(int)z);
        if(lineage!=null)q.initializeCallow(UUID.randomUUID(),lineage,p);
        c.assertTrue(c.getLevel().addFreshEntity(q),"Physics actor insertion");return q;
    }
    @GameTest(maxTicks=200,structure="prime_ants_test:idle_ground")
    public void nestmatesMoveAndPushInBothDirectionsWithForeignAndUnknownControls(GameTestHelper c) {
        ground(c);var queen=actor(c,AntForm.QUEEN,null,3.5,4.5);var worker=actor(c,AntForm.WORKER,queen.getUUID(),6.5,4.5);
        var peer=actor(c,AntForm.WORKER,queen.getUUID(),6.5,6.5);var unknown=actor(c,AntForm.WORKER,null,3.5,9.5);
        var foreign=actor(c,AntForm.WORKER,UUID.randomUUID(),3.8,9.5);
        c.assertTrue(Nestmates.matching(queen,worker) && Nestmates.matching(worker,queen) && Nestmates.matching(worker,peer),"Queen UUID and persisted worker lineage match pairwise");
        c.assertTrue(!Nestmates.matching(unknown,foreign) && !Nestmates.matching(unknown,actor(c,AntForm.WORKER,null,4.2,9.5)),"Missing identity is never a shared colony");
        queen.push(worker);worker.push(queen);worker.push(peer);peer.push(worker);
        c.assertTrue(queen.getDeltaMovement().equals(Vec3.ZERO) && worker.getDeltaMovement().equals(Vec3.ZERO) && peer.getDeltaMovement().equals(Vec3.ZERO),"Both push directions add no nestmate impulse");
        unknown.push(foreign);c.assertTrue(unknown.getDeltaMovement().horizontalDistanceSqr()>0 && foreign.getDeltaMovement().horizontalDistanceSqr()>0,"Ordinary foreign/unknown pushing retained");
        c.assertTrue(!Nestmates.movementClear(c.getLevel(),unknown,foreign.getBoundingBox()),"Foreign body still obstructs explicit movement/work stands");
        var workerStart=worker.position();var queenStart=queen.position();
        c.onEachTick(()->{
            if(c.getTick()<45){queen.move(MoverType.SELF,new Vec3(.09,0,0));worker.move(MoverType.SELF,new Vec3(-.09,0,0));}
            if(c.getTick()==46){
                c.assertTrue(queen.elapsedAgeTicks()>30 && worker.elapsedAgeTicks()>30 && queen.getX()>workerStart.x && worker.getX()<queenStart.x,"Real server ticks preserve bidirectional queen/worker movement through one another");
                // Worker/worker opposite traversal, through the same pair-specific rule.
                peer.setPos(workerStart.x,peer.getY(),worker.getZ());
            }
            if(c.getTick()>46 && c.getTick()<90){worker.move(MoverType.SELF,new Vec3(.09,0,0));peer.move(MoverType.SELF,new Vec3(-.09,0,0));}
            if(c.getTick()==91){c.assertTrue(worker.getX()>peer.getX() && queen.isAlive() && !queen.noPhysics,"Worker pairs pass while ordinary physics remains active");c.succeed();}
        });
    }
    @GameTest(maxTicks=200,structure="prime_ants_test:idle_ground")
    public void nestmateContactsExcludeCrammingButRetainTerrainGravityDamageAndForeignCramming(GameTestHelper c) {
        ground(c);var queen=actor(c,AntForm.QUEEN,null,4.5,4.5);var unknown=actor(c,AntForm.WORKER,null,10.5,10.5);
        var controls=new java.util.ArrayList<LasiusNigerEntity>();controls.add(unknown);
        for(int i=0;i<25;i++){actor(c,AntForm.WORKER,queen.getUUID(),4.5,4.5);controls.add(actor(c,AntForm.WORKER,null,10.5,10.5));}
        c.assertTrue(c.getLevel().getPushableEntities(queen,queen.getBoundingBox()).isEmpty(),"Vanilla shared pushing/cramming contact list excludes only matching nestmates");
        c.assertTrue(c.getLevel().getPushableEntities(unknown,unknown.getBoundingBox()).size()>=24,"Unknown/foreign cramming contacts retained under unchanged game rule");
        float health=queen.getHealth();
        c.runAfterDelay(70,()->{
            long survivors=controls.stream().filter(LasiusNigerEntity::isAlive).count();
            dev.primeants.PrimeAnts.LOGGER.info("T16 cramming control initial=26 survivors={} chosenControlAlive={} nestmateQueenHealth={}",survivors,unknown.isAlive(),queen.getHealth());
            c.assertTrue(queen.isAlive() && queen.getHealth()==health && survivors<=24,"Actual vanilla ticks exclude nestmate cramming; foreign group loses bodies to the unchanged 24-entity threshold without requiring a particular random victim");
            c.assertTrue(queen.hurtServer(c.getLevel(),queen.damageSources().generic(),1) && queen.getHealth()<health,"Ordinary damage remains effective");
            var wall=queen.blockPosition().east();c.getLevel().setBlock(wall,Blocks.STONE.defaultBlockState(),3);c.getLevel().setBlock(wall.above(),Blocks.STONE.defaultBlockState(),3);
            double x=queen.getX();queen.move(MoverType.SELF,new Vec3(2,0,0));c.assertTrue(queen.getX()<x+1,"Terrain still physically blocks movement");
            var gravity=actor(c,AntForm.WORKER,queen.getUUID(),8.5,4.5);gravity.setNoAi(false);
            gravity.setPos(gravity.getX(),gravity.getY()+2,gravity.getZ());double raised=gravity.getY();
            c.runAfterDelay(25,()->{c.assertTrue(gravity.getY()<raised && gravity.onGround() && !gravity.noPhysics,"Gravity and supported landing remain active");queen.hurtServer(c.getLevel(),queen.damageSources().generic(),1000);c.assertTrue(!queen.isAlive(),"Ordinary death still removes population");c.succeed();});
        });
    }
    @GameTest(maxTicks=10000)
    public void occupiedNestmateBodiesStillRefusePhysicalMoundsAndPlugs(GameTestHelper c) {
        var l=NaturalPlacementGameTest.level(c,"t16_thin");var mound=NaturalPlacementGameTest.prepare(c,l,2800);var plug=NaturalPlacementGameTest.prepare(c,l,2880);
        boolean[] crowded={false,false},checked={false,false};
        c.onEachTick(()->{
            for(int kind=0;kind<2;kind++) {
                var chunk=kind==0?mound:plug;var entity=l.getEntity(NaturalPlacementGameTest.uuid(l,chunk));if(!(entity instanceof LasiusNigerEntity q))continue;
                var phase=q.founding().phase();var p=q.founding().plan();if(p==null)continue;
                if(!crowded[kind] && phase==(kind==0?dev.primeants.founding.QueenFounding.Phase.TRANSPORTING:dev.primeants.founding.QueenFounding.Phase.SEALING)) {
                    crowded[kind]=true;
                    for(var b:kind==0?p.deposits():java.util.List.of(p.plugs().getFirst())) {
                        var body=AntEntities.WORKER.create(l,EntitySpawnReason.EVENT);c.assertTrue(body!=null,"Negative occupied-body factory");
                        body.initializeCallow(UUID.randomUUID(),q.getUUID(),p.chamber());body.setNoAi(true);body.setPos(Vec3.atBottomCenterOf(b));
                        c.assertTrue(l.addFreshEntity(body) && Nestmates.matching(q,body),"Real verified nestmate body occupies the placement cell");
                    }
                }
                if(crowded[kind] && phase==dev.primeants.founding.QueenFounding.Phase.FAILED) {
                    checked[kind]=true;c.assertTrue(q.founding().carried()>0 && q.founding().removed()==q.founding().carried()+q.founding().deposited()+q.founding().plugged(),"Refused placement retains physical cargo and conservation");
                    c.assertTrue(kind==0?q.founding().deposited()==0 && q.founding().reason().equals("bounded_mound_deposition_blocked")
                        :q.founding().plugged()==0 && q.founding().reason().equals("plug_revalidation_failed"),"Pass-through cannot insert a mound/plug through occupied nestmate bodies");
                }
            }
            if(checked[0] && checked[1])c.succeed();
        });
    }

}
