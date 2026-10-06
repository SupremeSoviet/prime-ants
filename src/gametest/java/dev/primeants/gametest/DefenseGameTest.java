package dev.primeants.gametest;

import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.*;

/** Real emerged workers, ordinary player attacks/breaks and loaded ticks. Only players/hazards are arranged. */
public final class DefenseGameTest {
    private final WorkerForagingGameTest f=new WorkerForagingGameTest();
    private record Key(net.minecraft.server.level.ServerLevel level,BlockPos pos){}
    private static final Set<Key> canceled=new HashSet<>();
    private static final Set<ServerPlayer> players=new HashSet<>();
    private record Contact(UUID ant,long tick,float before,float after,double distance,boolean visible){}
    private static final Map<UUID,Contact> contacts=new HashMap<>();
    static {
        PlayerBlockBreakEvents.BEFORE.register((l,p,pos,s,e)->!canceled.contains(new Key((net.minecraft.server.level.ServerLevel)l,pos)));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_LEVEL_TICK.register(l->{for(var p:List.copyOf(players))if(p.level()==l&&!p.isRemoved())p.doTick();});
    }
    private static Vec3 stand(GameTestHelper c,LasiusNigerEntity w){
        return stand(c,w,1.4,8);
    }
    private static Vec3 stand(GameTestHelper c,LasiusNigerEntity w,double minimum,double maximum){
        var l=c.getLevel();var options=new ArrayList<Vec3>();
        for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)for(int dy=-1;dy<=1;dy++){
            var feet=w.blockPosition().offset(dx,dy,dz);var v=Vec3.atBottomCenterOf(feet);double dist=w.position().distanceToSqr(v);
            if(dist<minimum||dist>maximum||!NestPlan.walkable(l,feet)||!l.noCollision(null,new AABB(v.x-.3,v.y,v.z-.3,v.x+.3,v.y+1.8,v.z+.3)))continue;
            if(l.clip(new net.minecraft.world.level.ClipContext(w.position().add(0,.25,0),v.add(0,.25,0),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.ANY,w)).getType()==HitResult.Type.MISS)options.add(v);
        }
        return options.stream().min(Comparator.comparingDouble(w.position()::distanceToSqr)).orElseThrow(()->new AssertionError("Supported clear normal player stand in real melee range"));
    }
    private static void cleanup(GameTestHelper c,Runnable action){
        ((dev.primeants.gametest.mixin.GameTestHelperAccessor)c).primeAntsTestInfo().addListener(new GameTestListener(){
            public void testStructureLoaded(GameTestInfo i){}
            public void testPassed(GameTestInfo i,GameTestRunner r){action.run();}
            public void testFailed(GameTestInfo i,GameTestRunner r){action.run();}
            public void testAddedForRerun(GameTestInfo i,GameTestInfo copy,GameTestRunner r){action.run();}
        });
    }
    private static ServerPlayer player(GameTestHelper c,Vec3 pos,GameType mode){
        var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"defense-fixture");
        var p=new ServerPlayer(c.getLevel().getServer(),c.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault()){
            @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel l,net.minecraft.world.damagesource.DamageSource source,float amount){
                var ant=source.getEntity() instanceof LasiusNigerEntity a?a:null;
                var mouth=ant==null?Vec3.ZERO:ant.position().add(0,.25,0);var target=position().add(0,.25,0);float before=getHealth();
                boolean visible=ant!=null&&l.clip(new net.minecraft.world.level.ClipContext(mouth,target,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.ANY,ant)).getType()==HitResult.Type.MISS;
                boolean accepted=super.hurtServer(l,source,amount);
                if(accepted&&getHealth()<before&&ant!=null)contacts.put(getUUID(),new Contact(ant.getUUID(),l.getGameTime(),before,getHealth(),mouth.distanceToSqr(target),visible));
                return accepted;
            }
        };
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel=new io.netty.channel.embedded.EmbeddedChannel(connection);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(c.getLevel().getServer(),connection,p,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false));
        p.setGameMode(mode);p.setPos(pos.x,pos.y,pos.z);p.setOnGround(true);c.getLevel().addNewPlayer(p);players.add(p);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        cleanup(c,()->{players.remove(p);contacts.remove(p.getUUID());p.level().removePlayerImmediately(p,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);channel.finishAndReleaseAll();});
        c.assertTrue(p.connection.hasClientLoaded()&&(mode!=GameType.SURVIVAL||!p.getAbilities().invulnerable),"Loaded normal player; measured combat has no invulnerability");return p;
    }
    private static void move(ServerPlayer p,Vec3 pos){p.setPos(pos.x,pos.y,pos.z);p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);}
    private static void attack(GameTestHelper c,ServerPlayer p,LasiusNigerEntity w){
        c.assertTrue(p.position().distanceToSqr(w.position())<=9,"Controlled player is within ordinary survival melee range");
        float before=w.getHealth();p.attack(w);
        c.assertTrue(w.getHealth()<before&&ColonyAlarm.get(c.getLevel()).alarm(w.queenId())!=null,"Actual accepted survival-player damage raises colony alarm");
    }
    private static void box(GameTestHelper c,ServerPlayer p,BlockPos feet){
        var l=c.getLevel();move(p,Vec3.atBottomCenterOf(feet));
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=0;y<=2;y++)if(x!=0||z!=0||y==2){
            var pos=feet.offset(x,y,z);c.assertTrue(ColonyAlarm.ownedComponent(l,pos)==null,"Controlled wall never overwrites owned nest");
            l.setBlock(pos,Blocks.STONE.defaultBlockState(),3);
        }
    }
    private static BlockPos hazardStand(GameTestHelper c,LasiusNigerEntity w){
        var l=c.getLevel();var options=new ArrayList<BlockPos>();
        for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++)for(int y=-1;y<=4;y++){
            var feet=w.blockPosition().offset(x,y,z);double distance=w.position().distanceToSqr(Vec3.atBottomCenterOf(feet));
            if(distance<16||distance>121||!NestPlan.walkable(l,feet))continue;boolean owned=false;
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)for(int dy=0;dy<=2;dy++)if(ColonyAlarm.ownedComponent(l,feet.offset(dx,dy,dz))!=null)owned=true;
            if(!owned)options.add(feet);
        }
        return options.stream().min(Comparator.comparingDouble(p->w.position().distanceToSqr(Vec3.atBottomCenterOf(p)))).orElseThrow(()->new AssertionError("Supported bounded hazard outside all actual owned nest cells"));
    }
    private LasiusNigerEntity surface(GameTestHelper c,LasiusNigerEntity q){
        var p=q.founding().plan();return p==null?null:f.workers(c,q).stream().filter(w->!w.isCallow()&&w.onGround()&&w.getY()>=p.entrance().getY()+.9).findFirst().orElse(null);
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void acceptedSurvivalHarmBitesOnlyProvokerWithReachCooldownAndCoherentRestore(GameTestHelper c){
        var q=f.start(c);ServerPlayer[] p={null},visitor={null};LasiusNigerEntity[] subject={null};boolean[] hit={false},restored={false};int[] first={-1};long[] firstBiteTick={-1};
        c.onEachTick(()->{
            if(!hit[0]){
                c.assertTrue(ColonyAlarm.get(c.getLevel()).alarm(q.getUUID())==null,"Ordinary founding and benign watching remain unprovoked");
                var w=surface(c,q);if(w==null)return;
                subject[0]=w;p[0]=player(c,stand(c,w),GameType.SURVIVAL);visitor[0]=player(c,stand(c,w,4,16),GameType.SURVIVAL);
                attack(c,p[0],w);hit[0]=true;return;
            }
            var damage=p[0].getLastDamageSource();if(damage==null||!(damage.getEntity() instanceof LasiusNigerEntity ant))return;
            var accepted=contacts.get(p[0].getUUID());
            c.assertTrue(q.getUUID().equals(ant.queenId())&&accepted!=null&&accepted.before()-accepted.after()==1&&visitor[0].getHealth()==20,"Actual accepted ant damage belongs to harmed colony and only provoking player; normal saturation regeneration may follow the recorded damage");
            if(first[0]<0){first[0]=(int)c.getTick();firstBiteTick[0]=accepted.tick();subject[0]=ant;c.assertTrue(ant.workerTasks().biteCooldown()>0,"20-tick bite cooldown starts with physical contact");
                var contact=contacts.get(p[0].getUUID());c.assertTrue(contact!=null&&contact.ant().equals(ant.getUUID())&&contact.distance()<=1.6&&contact.visible()&&contact.before()-contact.after()==1,"Accepted bite snapshot has physical reach and visibility before normal knockback");
            }
            if(accepted.ant().equals(subject[0].getUUID())&&accepted.tick()!=firstBiteTick[0])c.assertTrue(accepted.tick()-firstBiteTick[0]>=20,"Same ant cannot accept another bite before twenty loaded ticks");
            if(!restored[0]){
                var w=subject[0];long bites=w.workerTasks().bites();int cooldown=w.workerTasks().biteCooldown();subject[0]=f.restore(c,w);restored[0]=true;
                subject[0].workerTasks().tick(c.getLevel());subject[0].workerTasks().tick(c.getLevel());
                int after=subject[0].workerTasks().biteCooldown();subject[0].workerTasks().tick(c.getLevel());
                c.assertTrue(subject[0].workerTasks().bites()==bites&&after>=cooldown-1&&after<=cooldown&&subject[0].workerTasks().biteCooldown()==after,"Coherent restoration permits at most one current loaded-tick decrement; repeated callbacks cannot duplicate bite or decrement");
                move(p[0],stand(c,subject[0],16,36));return;
            }
            if(c.getTick()-first[0]>=25){c.assertTrue(visitor[0].getHealth()==20,"No retargeting to innocent nearby player");c.succeed();}
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:foraging_pair")
    public void ownedBreakCapturesBeforeRevocationAlarmsOnlyOwnerAndDefendsUnreadyNest(GameTestHelper c){
        var l=c.getLevel();var records=NaturalSoil.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,NaturalSoil.get(l)).getOrThrow().getAsJsonObject();
        for(int x=1;x<=30;x++)for(int z=1;z<=30;z++)for(int y=1;y<=4;y++){c.setBlock(x,y,z,Blocks.DIRT);records.addProperty(Long.toString(c.absolutePos(new BlockPos(x,y,z)).asLong()),"minecraft:dirt");}
        l.getDataStorage().set(NaturalSoil.TYPE,NaturalSoil.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,records).getOrThrow());
        var q=f.pairEgg(c,new BlockPos(8,4,10));var other=f.pairEgg(c,new BlockPos(20,4,10));boolean[] broken={false};ServerPlayer[] p={null};
        c.onEachTick(()->{
            if(!broken[0]){
                c.assertTrue(ColonyAlarm.get(l).alarm(q.getUUID())==null&&ColonyAlarm.get(l).alarm(other.getUUID())==null,"Ant excavation is not a player break");
                if(q.founding().lifecycle()!=QueenFounding.Lifecycle.OPEN||other.founding().lifecycle()!=QueenFounding.Lifecycle.OPEN)return;
                var plan=q.founding().plan();var wall=plan.undergroundSurfaces().stream().filter(b->ColonyTerrain.get(l).prepared(l,b,q.getUUID())).findFirst().orElse(null);
                c.assertTrue(wall!=null,"Live prepared shell ownership must exist before successful break");
                var w=f.workers(c,q).stream().filter(a->!a.isCallow()&&!a.isNoAi()&&ColonyMembers.get(l).belongs(a,q.getUUID(),plan.chamber())).min(Comparator.comparingDouble(a->a.position().distanceToSqr(Vec3.atCenterOf(wall)))).orElse(null);
                c.assertTrue(w!=null,"A genuine living mature member is required; role assignment may follow OPEN next tick");
                p[0]=player(c,stand(c,w),GameType.SURVIVAL);
                c.assertTrue(q.getUUID().equals(ColonyAlarm.ownedComponent(l,wall))&&p[0].gameMode.destroyBlock(wall),"Successful ordinary survival break of actually owned prepared shell");
                c.assertTrue(ColonyAlarm.ownedComponent(l,wall)==null&&ColonyAlarm.get(l).alarm(q.getUUID())!=null&&ColonyAlarm.get(l).alarm(other.getUUID())==null,"AFTER sees revoked ownership but pre-mutation capture retains exactly its owner");
                c.assertTrue(!q.founding().ready(),"The genuine player breach invalidates normal nest readiness");broken[0]=true;return;
            }
            var d=p[0].getLastDamageSource();if(d!=null&&d.getEntity() instanceof LasiusNigerEntity w){c.assertTrue(q.getUUID().equals(w.queenId())&&!q.founding().ready()&&ColonyAlarm.get(l).alarm(other.getUUID())==null,"Living registered workers defend breached unready colony without alarming neighbor");c.succeed();}
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void canceledUnknownAndOrdinaryWritesFoodDropsAndZeroDamageStayBenign(GameTestHelper c){
        var q=f.start(c);boolean[] acted={false};int[] start={0};
        c.onEachTick(()->{
            var l=c.getLevel();c.assertTrue(ColonyAlarm.get(l).alarm(q.getUUID())==null,"No false alarm during ordinary ant work or benign actions");
            if(!acted[0]){
                var w=surface(c,q);if(w==null)return;var plan=q.founding().plan();
                var p=player(c,w.position().add(2,0,0),GameType.SURVIVAL);var nest=plan.nursery();var key=new Key(l,nest);canceled.add(key);cleanup(c,()->canceled.remove(key));
                c.assertTrue(!p.gameMode.destroyBlock(nest)&&l.getBlockEntity(nest) instanceof dev.primeants.brood.BroodPile,"Canceled real break preserves owned nursery");canceled.remove(key);
                var unknown=plan.at(-6,3,1);l.setBlock(unknown,dev.primeants.brood.NurseryBlocks.NEST_SOIL.defaultBlockState(),3);
                c.assertTrue(ColonyAlarm.ownedComponent(l,unknown)==null&&p.gameMode.destroyBlock(unknown),"Player matching-material block has no ownership");
                var mound=plan.deposits().stream().filter(b->ColonyTerrain.get(l).mound(l,b,q.getUUID())).findFirst().orElseThrow();
                l.setBlock(mound,l.getBlockState(mound),3);c.assertTrue(ColonyAlarm.ownedComponent(l,mound)==null&&p.gameMode.destroyBlock(mound),"Same-state ordinary write revokes authority; old mound coordinates cannot alarm");
                w.hurtServer(l,w.damageSources().playerAttack(p),0);w.hurtServer(l,w.damageSources().generic(),.1F);
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE));p.drop(true);move(p,p.position().add(4,0,0));
                acted[0]=true;start[0]=(int)c.getTick();return;
            }
            if(c.getTick()-start[0]>=80)c.succeed();
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void wallsRangeCreativeSpectatorAndMissingTargetsPreventRemoteBites(GameTestHelper c){
        var q=f.start(c);ServerPlayer[] p={null};boolean[] setup={false};int[] start={0};UUID[] id={null};
        c.onEachTick(()->{
            if(!setup[0]){var w=surface(c,q);if(w==null)return;id[0]=w.getUUID();p[0]=player(c,stand(c,w),GameType.SURVIVAL);attack(c,p[0],w);
                box(c,p[0],BlockPos.containing(w.position()).offset(3,0,0));start[0]=(int)c.getTick();setup[0]=true;return;}
            int elapsed=(int)c.getTick()-start[0];c.assertTrue(p[0].getHealth()==20,"Closed colliding wall/range prevents remote or through-wall bite");
            if(elapsed==80){var a=ColonyAlarm.get(c.getLevel()).alarm(q.getUUID());move(p[0],Vec3.atBottomCenterOf(a.origin()).add(20,0,0));}
            if(elapsed==90){c.assertTrue(f.workers(c,q).stream().noneMatch(w->w.workerTasks().defending()),"Target outside finite chase boundary resumes work");p[0].setGameMode(GameType.CREATIVE);move(p[0],((LasiusNigerEntity)c.getLevel().getEntity(id[0])).position().add(.8,0,0));}
            if(elapsed==100){c.assertTrue(f.workers(c,q).stream().noneMatch(w->w.workerTasks().defending()),"Creative player gets no pursuit");p[0].setGameMode(GameType.SPECTATOR);}
            if(elapsed==110){c.assertTrue(f.workers(c,q).stream().noneMatch(w->w.workerTasks().defending()),"Spectator gets no pursuit");
                p[0].setGameMode(GameType.SURVIVAL);var other=c.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
                c.assertTrue(other!=null&&p[0].teleportTo(other,p[0].getX(),128,p[0].getZ(),Set.of(),0,0,false),"Controlled provoking player actually leaves dimension");}
            if(elapsed==120){c.assertTrue(p[0].level()!=c.getLevel()&&f.workers(c,q).stream().noneMatch(w->w.workerTasks().defending()),"Player in another dimension gets no chase or bite");p[0].discard();}
            if(elapsed==130){c.assertTrue(f.workers(c,q).stream().noneMatch(w->w.workerTasks().defending()),"Absent target gets no pursuit or substitute");c.succeed();}
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void missingProvokerRestoresSameForagerExactSoilCargoClaimAndTask(GameTestHelper c){cargo(c,false);}
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void loadedAlarmExpiryRestoresSameForagerExactSoilCargoClaimAndTask(GameTestHelper c){cargo(c,true);}
    private void cargo(GameTestHelper c,boolean expiry){
        var q=f.start(c);LasiusNigerEntity[] actor={null};ServerPlayer[] p={null};ItemStack[] cargo={null};WorkerTasks.Phase[] phase={null};boolean[] begun={false},restored={false};UUID[] claim={null};int[] start={0};
        c.onEachTick(()->{
            var l=c.getLevel();
            if(!begun[0]){
                var w=f.workers(c,q).stream().filter(a->!a.isCallow()&&a.workerTasks().phase()==WorkerTasks.Phase.SOIL_OUT&&a.getMainHandItem().is(Items.DIRT)).findFirst().orElse(null);if(w==null)return;
                actor[0]=w;cargo[0]=w.getMainHandItem().copy();phase[0]=w.workerTasks().phase();claim[0]=q.founding().workerClaim();
                p[0]=player(c,stand(c,w),GameType.SURVIVAL);attack(c,p[0],w);start[0]=(int)c.getTick();begun[0]=true;return;
            }
            if(!restored[0]){
                c.assertTrue(actor[0].workerTasks().defending()&&ItemStack.matches(cargo[0],actor[0].getMainHandItem()),"Defense suspends genuine soil trip without consuming equipment");
                actor[0]=f.restore(c,actor[0]);var alarms=ColonyAlarm.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,ColonyAlarm.get(l)).getOrThrow();
                l.getDataStorage().set(ColonyAlarm.TYPE,ColonyAlarm.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,alarms).getOrThrow());
                c.assertTrue(actor[0].workerTasks().defending()&&ColonyAlarm.get(l).alarm(q.getUUID()).remaining()<=600,"Persisted interrupt and alarm restore finite remaining lifetime");
                if(expiry)box(c,p[0],hazardStand(c,actor[0]));else p[0].discard();restored[0]=true;return;
            }
            c.assertTrue(claim[0].equals(q.founding().workerClaim())&&actor[0].getUUID().equals(claim[0]),"Original forager claim remains owned by same identity");
            if(actor[0].workerTasks().defending()){c.assertTrue(ItemStack.matches(cargo[0],actor[0].getMainHandItem())&&actor[0].workerTasks().phase()==phase[0],"Suspended task and exact cargo survive real defense ticks");return;}
            c.assertTrue(ItemStack.matches(cargo[0],actor[0].getMainHandItem())&&actor[0].workerTasks().phase()==phase[0],"Same worker resumes original task, exact canonical cargo and claim");
            if(expiry)c.assertTrue(c.getTick()-start[0]>=600&&ColonyAlarm.get(l).alarm(q.getUUID())==null,"Alarm expires after exactly bounded loaded ticks");c.succeed();
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void liveOwnedPlugBreakRetainsSourceAcrossOrdinaryWriteRevocation(GameTestHelper c){
        var q=f.start(c);c.onEachTick(()->{
            var plan=q.founding().plan();if(plan==null||!q.founding().sealed()||!q.founding().ready())return;
            var plug=plan.plugs().stream().filter(b->ColonyPlugs.get(c.getLevel()).owned(c.getLevel(),b,q.getUUID())).findFirst().orElse(null);if(plug==null)return;
            var p=player(c,Vec3.atBottomCenterOf(plan.at(3,-1,-2)),GameType.SURVIVAL);
            c.assertTrue(q.getUUID().equals(ColonyAlarm.ownedComponent(c.getLevel(),plug))&&p.gameMode.destroyBlock(plug),"Actual owned compacted plug accepts survival break");
            c.assertTrue(ColonyAlarm.ownedComponent(c.getLevel(),plug)==null&&ColonyAlarm.get(c.getLevel()).alarm(q.getUUID())!=null,"Before captured plug owner survives successful mutation which revokes ownership");c.succeed();
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void successfulMoundCacheAndNurseryBreaksRenewOnlyActualOwner(GameTestHelper c){
        var q=f.start(c);boolean[] dropped={false};ServerPlayer[] p={null};
        c.onEachTick(()->{
            var plan=q.founding().plan();if(plan==null||q.founding().lifecycle()!=QueenFounding.Lifecycle.OPEN)return;
            if(!dropped[0]){
                p[0]=player(c,Vec3.atBottomCenterOf(plan.at(-3,1,1)),GameType.SURVIVAL);
                p[0].setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE));p[0].drop(true);move(p[0],Vec3.atBottomCenterOf(plan.at(-7,1,1)));dropped[0]=true;return;
            }
            var cache=f.cache(c,q);if(cache==null||cache.size()==0)return;
            c.assertTrue(ColonyAlarm.get(c.getLevel()).alarm(q.getUUID())==null,"Benign food visit leaves colony peaceful");
            var mound=plan.deposits().stream().filter(b->ColonyTerrain.get(c.getLevel()).mound(c.getLevel(),b,q.getUUID())).min(Comparator.comparingDouble(b->Vec3.atCenterOf(b).distanceToSqr(Vec3.atBottomCenterOf(plan.outside())))).orElseThrow();
            for(var pos:List.of(mound,plan.cache(),plan.nursery())){
                move(p[0],Vec3.atBottomCenterOf(pos.equals(mound)?plan.outside():plan.at(5,-1,-2)));
                c.assertTrue(p[0].position().distanceToSqr(Vec3.atCenterOf(pos))<=20.25,"Controlled player stands within normal block interaction reach");
                c.assertTrue(q.getUUID().equals(ColonyAlarm.ownedComponent(c.getLevel(),pos)),"Live component owner comes from actual colony writes/block entity");
                c.assertTrue(p[0].gameMode.destroyBlock(pos),"Successful ordinary player break");
                var alarm=ColonyAlarm.get(c.getLevel()).alarm(q.getUUID());
                c.assertTrue(alarm!=null&&alarm.player().equals(p[0].getUUID())&&alarm.origin().equals(pos)&&alarm.remaining()==600,"Repeated genuine owned harm renews finite alarm for exactly provoking player");
            }
            c.succeed();
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void provocationCancelsUncommittedFeedingAndPreservesPhysicalFood(GameTestHelper c){
        var q=f.start(c);boolean[] supplied={false},hit={false};LasiusNigerEntity[] actor={null};ServerPlayer[] p={null};ItemStack[] cargo={null};
        c.onEachTick(()->{
            if(!supplied[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){new NursingGameTest().supply(c,q,6,8);supplied[0]=true;}
            if(!hit[0]){
                var w=f.workers(c,q).stream().filter(a->a.workerTasks().feedingTicks()>2&&WorkerTasks.food(a.getMainHandItem())).findFirst().orElse(null);if(w==null)return;
                actor[0]=w;cargo[0]=w.getMainHandItem().copy();p[0]=player(c,stand(c,w),GameType.SURVIVAL);attack(c,p[0],w);hit[0]=true;return;
            }
            if(actor[0].workerTasks().defending()){
                c.assertTrue(actor[0].workerTasks().feedingTicks()==0&&!actor[0].workerTasks().sharing().busy()&&ItemStack.matches(cargo[0],actor[0].getMainHandItem()),"Uncommitted feeding/sharing canceled while genuine food and original nurse phase remain");
                p[0].discard();c.succeed();
            }
        });
    }
    @GameTest(maxTicks=22000,structure="prime_ants_test:idle_ground")
    public void provocationCancelsRealPartialCropSharingWithoutTransferCargoOrClaimLoss(GameTestHelper c){
        var q=f.start(c);boolean[] supplied={false},hit={false};LasiusNigerEntity[] donor={null},recipient={null};ServerPlayer[] p={null};
        ItemStack[] cargo={null},otherCargo={null};long[] given={0},received={0};UUID[] claim={null};
        c.onEachTick(()->{
            if(!supplied[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){new NursingGameTest().supply(c,q,8,8);supplied[0]=true;}
            if(!hit[0]){
                var d=f.workers(c,q).stream().filter(a->a.workerTasks().sharing().actionTicks()>2&&a.workerTasks().sharing().target()!=null).findFirst().orElse(null);if(d==null)return;
                if(!(c.getLevel().getEntity(d.workerTasks().sharing().target()) instanceof LasiusNigerEntity r)||r.form()!=AntForm.WORKER)return;
                c.assertTrue(r.workerTasks().sharing().busy()&&d.socialAction()&&r.socialAction(),"Genuine emerged bodies have actually started a physical crop action without forced fasting or target assignment");
                donor[0]=d;recipient[0]=r;cargo[0]=d.getMainHandItem().copy();otherCargo[0]=r.getMainHandItem().copy();given[0]=d.nutrition().givenSugar();received[0]=r.nutrition().receivedSugar();claim[0]=q.founding().workerClaim();
                p[0]=player(c,stand(c,d),GameType.SURVIVAL);attack(c,p[0],d);hit[0]=true;return;
            }
            if(!donor[0].workerTasks().defending()||!recipient[0].workerTasks().defending())return;
            c.assertTrue(!donor[0].workerTasks().sharing().busy()&&!recipient[0].workerTasks().sharing().busy()&&!donor[0].socialAction()&&!recipient[0].socialAction(),"Defense clears both uncommitted social endpoints");
            c.assertTrue(donor[0].nutrition().givenSugar()==given[0]&&recipient[0].nutrition().receivedSugar()==received[0]&&ItemStack.matches(cargo[0],donor[0].getMainHandItem())&&ItemStack.matches(otherCargo[0],recipient[0].getMainHandItem())&&Objects.equals(claim[0],q.founding().workerClaim()),"No premature transfer, duplicate sugar, lost exact equipment or stolen forager claim");
            p[0].discard();c.succeed();
        });
    }
}
