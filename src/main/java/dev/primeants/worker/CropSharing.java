package dev.primeants.worker;

import dev.primeants.entity.*;
import dev.primeants.founding.NestPlan;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** An interrupt owned by WorkerTasks, never a new role or equipment owner. */
public final class CropSharing {
    public static final int ACTION_TICKS=20, APPROACH_BOUND=200;
    public static final long RESERVE=1000, PORTION=1500;
    private final LasiusNigerEntity donor;
    private UUID target,receivingFrom;
    private int actionTicks,approachTicks,cooldown;
    private long lastTick=Long.MIN_VALUE;
    public CropSharing(LasiusNigerEntity ant){donor=ant;}
    public boolean busy(){return target!=null||receivingFrom!=null;}
    public int actionTicks(){return actionTicks;} public UUID target(){return target;}
    private static boolean live(ServerLevel l,LasiusNigerEntity a){
        if(l.getEntity(a.getUUID())!=a||!a.isAlive()||a.isRemoved()||a.isNoAi()||a.isCallow()
            ||!NestPlan.loaded(l,a.blockPosition())||!l.isPositionEntityTicking(a.blockPosition()))return false;
        return a.form()==AntForm.QUEEN||a.queenId()!=null&&a.nurseryHome()!=null&&ColonyMembers.get(l).belongs(a,a.queenId(),a.nurseryHome());
    }
    private boolean recipient(ServerLevel l,LasiusNigerEntity a){return a!=donor&&live(l,a)&&Objects.equals(donor.colonyIdentity(),a.colonyIdentity())
        &&a.nutrition().sugar()<PORTION&&a.adultLife().fasting()>=Math.max(1,a.adultLife().grace()/4)
        &&a.workerTasks().sharing().target==null;}
    public boolean needsCrop(ServerLevel l){
        if(!live(l,donor)||donor.form()!=AntForm.WORKER||donor.nutrition().sugar()>RESERVE)return false;
        for(var m:ColonyMembers.get(l).members(donor.queenId()))if(l.getEntity(m.worker()) instanceof LasiusNigerEntity a&&recipient(l,a)&&donor.distanceToSqr(a)<=256)return true;
        return l.getEntity(donor.queenId()) instanceof LasiusNigerEntity q&&recipient(l,q)&&donor.distanceToSqr(q)<=256;
    }
    private void clear(ServerLevel l){
        if(target!=null&&l.getEntity(target) instanceof LasiusNigerEntity a&&donor.getUUID().equals(a.workerTasks().sharing().receivingFrom)){
            a.workerTasks().sharing().receivingFrom=null;a.setSocialAction(false);
        }
        donor.setSocialAction(false);target=null;actionTicks=0;approachTicks=0;cooldown=40;donor.getNavigation().stop();
    }
    public boolean tick(ServerLevel l){
        if(lastTick>=l.getGameTime())return target!=null||receivingFrom!=null;
        lastTick=l.getGameTime();
        if(receivingFrom!=null){
            if(l.getEntity(receivingFrom) instanceof LasiusNigerEntity a&&live(l,a)&&donor.getUUID().equals(a.workerTasks().sharing().target)
                &&WorkerTasks.reaches(l,a,donor.position().add(0,.25,0))){
                donor.getNavigation().stop();donor.getLookControl().setLookAt(a.position().add(0,.25,0));return true;
            }
            receivingFrom=null;donor.setSocialAction(false);
        }
        if(!live(l,donor)||donor.form()!=AntForm.WORKER||donor.nutrition().sugar()<=RESERVE){if(target!=null)clear(l);return false;}
        if(cooldown>0){cooldown--;return false;}
        if(target==null&&donor.tickCount%20==0){
            var choices=new ArrayList<LasiusNigerEntity>();
            for(var m:ColonyMembers.get(l).members(donor.queenId()))if(l.getEntity(m.worker()) instanceof LasiusNigerEntity a&&recipient(l,a)&&donor.distanceToSqr(a)<=256)choices.add(a);
            if(l.getEntity(donor.queenId()) instanceof LasiusNigerEntity a&&recipient(l,a)&&donor.distanceToSqr(a)<=256)choices.add(a);
            choices.sort(Comparator.comparingLong((LasiusNigerEntity a)->-a.adultLife().fasting()).thenComparingDouble(donor::distanceToSqr));
            int trials=0;for(var a:choices){if(++trials>8)break;if(a.workerTasks().sharing().receivingFrom!=null)continue;
                var path=donor.getNavigation().createPath(a.blockPosition(),0,24);if(path!=null&&path.canReach()){target=a.getUUID();donor.getNavigation().moveTo(path,1.0);break;}}
        }
        if(target==null)return false;
        if(!(l.getEntity(target) instanceof LasiusNigerEntity a)||!recipient(l,a)||++approachTicks>APPROACH_BOUND){clear(l);return false;}
        var mouth=a.position().add(0,.25,0);
        donor.getLookControl().setLookAt(mouth);
        if(!WorkerTasks.reaches(l,donor,mouth)||!WorkerTasks.reaches(l,a,donor.position().add(0,.25,0))){
            actionTicks=0;donor.setSocialAction(false);
            if(donor.getUUID().equals(a.workerTasks().sharing().receivingFrom)){a.workerTasks().sharing().receivingFrom=null;a.setSocialAction(false);}
            if(donor.tickCount%20==0||donor.getNavigation().isDone())donor.getNavigation().moveTo(a.getX(),a.getY(),a.getZ(),0,1.0);
            return true;
        }
        var receiving=a.workerTasks().sharing();
        if(receiving.receivingFrom!=null&&!donor.getUUID().equals(receiving.receivingFrom)){clear(l);return false;}
        receiving.receivingFrom=donor.getUUID();a.getNavigation().stop();a.getLookControl().setLookAt(donor.position().add(0,.25,0));
        donor.getNavigation().stop();donor.setSocialAction(true);a.setSocialAction(true);
        if(++actionTicks<ACTION_TICKS)return true;
        // No asynchronous callbacks between paired mutations. Clear action before any later callback.
        long amount=Math.min(PORTION,Math.min(donor.nutrition().sugar()-RESERVE,Nutrition.QUEEN_SUGAR_CAPACITY-a.nutrition().sugar()));
        if(live(l,donor)&&recipient(l,a)&&WorkerTasks.reaches(l,donor,mouth)&&WorkerTasks.reaches(l,a,donor.position().add(0,.25,0))
            &&donor.nutrition().shareWith(a.nutrition(),amount,RESERVE,Nutrition.QUEEN_SUGAR_CAPACITY))
            dev.primeants.PrimeAnts.LOGGER.info("Physical crop sharing donor={} recipient={} loadedActionTicks={} quantity={} donorGiven={} recipientReceived={}",donor.getUUID(),a.getUUID(),actionTicks,amount,donor.nutrition().givenSugar(),a.nutrition().receivedSugar());
        clear(l);return true;
    }
    public void save(ValueOutput o){o.putLong("LastLoadedTick",lastTick);if(target!=null)o.putString("Target",target.toString());o.putInt("ActionTicks",actionTicks);o.putInt("ApproachTicks",approachTicks);o.putInt("Cooldown",cooldown);}
    public void load(ValueInput i){target=i.getString("Target").map(UUID::fromString).orElse(null);actionTicks=i.getIntOr("ActionTicks",0);approachTicks=i.getIntOr("ApproachTicks",0);cooldown=i.getIntOr("Cooldown",0);receivingFrom=null;lastTick=i.getLongOr("LastLoadedTick",Long.MIN_VALUE);
        if(actionTicks<0||actionTicks>=ACTION_TICKS||approachTicks<0||approachTicks>APPROACH_BOUND||cooldown<0||target==null&&actionTicks!=0)throw new IllegalArgumentException("Invalid crop action");donor.setSocialAction(false);}
}
