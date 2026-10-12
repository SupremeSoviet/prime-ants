package dev.primeants.time;

import dev.primeants.PrimeAnts;
import dev.primeants.brood.*;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.*;

/** Enabled server-thread coordinator. State lives with its owned nursery; no global polling or chunk tickets. */
public final class ColonyCatchup {
    public static final int MAX_EVENTS=512;
    private long cursor=-1,through=-1,appliedThrough=-1;
    private long appliedTicks,invocations,lastNanos;
    private int lastEvents;
    private String status="idle";
    private long invokedAt=Long.MIN_VALUE;
    private boolean running;
    public long pendingTicks(){return cursor<0?0:through-cursor+1;}
    public long appliedTicks(){return appliedTicks;}
    public long appliedThrough(){return appliedThrough;}
    public long invocations(){return invocations;}
    public long lastNanos(){return lastNanos;}
    public int lastEvents(){return lastEvents;}
    public String status(){return status;}
    public void measuredInvocation(long nanos){lastNanos=nanos;}

    public static void beforeAdult(ServerLevel level,LasiusNigerEntity adult){
        if(level.getEntity(adult.getUUID())!=adult||adult.isRemoved())return;
        NestPlan plan=adult.form()==AntForm.QUEEN?adult.founding().plan():adult.workerTasks().plan();
        if(plan==null||adult.colonyIdentity()==null||!NestPlan.loaded(level,plan.nursery()))return;
        if(level.getBlockEntity(plan.nursery()) instanceof BroodPile pile&&pile.ownedBy(adult.colonyIdentity(),plan))pile.resumeCatchup(level);
    }
    private static List<LasiusNigerEntity> confirmed(ServerLevel level,BroodPile pile){
        var result=new ArrayList<LasiusNigerEntity>();var queen=pile.queenId();
        var owner=level.getEntity(queen);
        if(!AdultHistory.get(level).recorded(queen)){
            if(!(owner instanceof LasiusNigerEntity q)||!q.isAlive()||q.isRemoved()||q.form()!=AntForm.QUEEN)return null;
            result.add(q);
        }
        for(var member:ColonyMembers.get(level).members(queen)){
            if(member.dead())continue;
            var entity=level.getEntity(member.worker());
            if(!(entity instanceof LasiusNigerEntity ant)||!ant.isAlive()||ant.isRemoved()
                ||!ColonyMembers.get(level).belongs(ant,queen,pile.catchupPlan().chamber()))return null;
            result.add(ant);
        }
        if(result.size()>pile.adultCapacity())return null;
        for(var ant:result)if(!NestPlan.loaded(level,ant.blockPosition()))return null;
        return result;
    }
    private record Segment(boolean missing,long length){}
    private static Segment segment(AdultSimulation.Snapshot evidence,long cursor,long through){
        if(evidence.invalidTime())return null;
        if(cursor<=evidence.boundary()&&evidence.origin()==AdultSimulation.Origin.BIRTH)
            return new Segment(false,Math.min(through,evidence.boundary())-cursor+1);
        if(cursor-1<evidence.knownAfter())return null;
        for(var run:evidence.runs()){
            if(run.last()<cursor)continue;
            if(run.first()<=cursor)return new Segment(false,Math.min(through,run.last())-cursor+1);
            return new Segment(true,Math.min(through,run.first()-1)-cursor+1);
        }
        return new Segment(true,through-cursor+1);
    }
    /** False retains the nursery's uncompleted work. Adults may ordinarily tick; their runs exclude those steps later. */
    public boolean resume(ServerLevel level,BroodPile pile,ColonyClock.Interval observation){
        long now=level.getGameTime();
        if(running)return false;
        if(cursor<0&&observation.afterGameTime().isPresent()){
            long after=Math.max(observation.afterGameTime().getAsLong(),appliedThrough);
            if(ColonyClock.missedTicks(after,now)>0){cursor=after+1;through=now-1;}
        }
        if(cursor<0)return true;
        if(now<=through){status="pending_reversed_or_same_time";return false;}
        through=now-1; // Nursery ordinary biology is paused while pending. Every waiting game tick remains owed.
        if(invokedAt==now)return false;
        invokedAt=now;running=true;long start=System.nanoTime();lastEvents=0;invocations++;
        try{
            String identity=pile.reconcileCatchupMembers(level);
            if(identity!=null){status=identity;return false;}
            var adults=confirmed(level,pile);
            if(adults==null){status="pending_unknown_members";return false;}
            var habitat=pile.catchupPlan().nurseryFindings(level,pile.queenId(),pile.operational());
            if(habitat.verdict()==Findings.Verdict.UNKNOWN){status="pending_unknown_terrain";return false;}
            for(var a:adults)if(segment(a.simulationEvidence(),cursor,through)==null){status="pending_unknown_adult_evidence";return false;}
            for(var a:adults)if(a.elapsedAgeTicks()==Long.MAX_VALUE){status="pending_age_domain";return false;}
            pile.evaluateAggregateStart(level);
            while(cursor>=0&&lastEvents<MAX_EVENTS){
                adults=confirmed(level,pile);
                if(adults==null){status="pending_unknown_members";return false;}
                var plan=pile.catchupPlan();
                NestCache cache=habitat.verdict()==Findings.Verdict.CLEAR&&NestPlan.loaded(level,plan.cache())
                    &&level.getBlockEntity(plan.cache()) instanceof NestCache c&&c.ownedBy(pile.queenId(),plan)?c:null;
                boolean nurse=adults.stream().anyMatch(a->a.form()==AntForm.WORKER&&a.workerTasks().caregiver(level,plan));
                // One decision batch plus all physical units it could allocate fits before any mutation.
                int reserve=1+(cache==null?0:cache.size());
                if(lastEvents>MAX_EVENTS-reserve){status="pending_event_bound";return false;}
                var segments=new LinkedHashMap<LasiusNigerEntity,Segment>();
                long span=through-cursor+1;
                for(var a:adults){
                    var sg=segment(a.simulationEvidence(),cursor,through);
                    if(sg==null){status="pending_unknown_adult_evidence";return false;}
                    segments.put(a,sg);span=Math.min(span,sg.length());
                    if(sg.missing()){
                        if(cache!=null&&a.adultLife().hungry()&&(a.form()==AntForm.WORKER||nurse)
                            &&cache.serveAggregate(a.nutrition(),Nutrition.QUEEN_SUGAR_CAPACITY,a.form()==AntForm.WORKER?0:Nutrition.QUEEN_PROTEIN_CAPACITY,s->Nutrition.sugarYield(s)>0))lastEvents++;
                        var phase=a.form()==AntForm.WORKER||a.adultLife().started()||!a.founding().reserveOnlyFounding(level)?AdultLife.Phase.ACTIVE:AdultLife.Phase.RESERVE_ONLY;
                        span=Math.min(span,a.adultLife().nextEvent(a,phase));
                        if(a.elapsedAgeTicks()==Long.MAX_VALUE){status="pending_age_domain";return false;}
                        span=Math.min(span,Long.MAX_VALUE-a.elapsedAgeTicks());
                    }
                }
                if(cache!=null&&nurse)for(var brood:pile.records()){
                    // Actual kinds and larval lifetime intake caps; no new nutrition rules or virtual income.
                    while(lastEvents<MAX_EVENTS-1&&cache.serveAggregate(brood.nutrition(),Nutrition.LARVA_SUGAR,Nutrition.LARVA_PROTEIN,s->pile.accepts(brood,s)))lastEvents++;
                }
                span=Math.min(span,pile.nextBroodEvent(level,habitat.problem()));
                // Avoid arithmetic overflow in the nursery's speed accounting, even for maximal saved time domains.
                span=Math.min(span,Long.MAX_VALUE/BroodCapacity.MAX_SLOTS/4);
                long end=cursor+span-1;
                for(var entry:segments.entrySet())if(entry.getValue().missing())entry.getKey().advanceMissing(level,cursor,end);
                pile.advanceAggregate(level,span,end,habitat.problem());
                lastEvents++;appliedTicks=AdultLife.saturatingAdd(appliedTicks,span);appliedThrough=end;
                cursor=end==through?-1:end+1;
                pile.setChanged();
            }
            status=cursor<0?"completed":"pending_event_bound";
            return cursor<0;
        }finally{
            lastNanos=System.nanoTime()-start;running=false;pile.setChanged();
            PrimeAnts.LOGGER.info("AUTOMATIC_CATCHUP queen={} status={} applied={} pending={} events={} nanos={} through={}",pile.queenId(),status,appliedTicks,pendingTicks(),lastEvents,lastNanos,appliedThrough);
        }
    }
    public void save(ValueOutput out){out.putInt("Version",1);out.putLong("Cursor",cursor);out.putLong("Through",through);out.putLong("AppliedThrough",appliedThrough);out.putLong("AppliedTicks",appliedTicks);out.putLong("Invocations",invocations);out.putString("Status",status);}
    public void load(ValueInput in){
        cursor=in.getLongOr("Cursor",-1);through=in.getLongOr("Through",-1);appliedThrough=in.getLongOr("AppliedThrough",-1);
        appliedTicks=in.getLongOr("AppliedTicks",0);invocations=in.getLongOr("Invocations",0);status=in.getStringOr("Status","idle");
        if(in.getIntOr("Version",1)!=1||cursor< -1||through< -1||appliedThrough< -1||appliedTicks<0||invocations<0||cursor>=0&&(through<cursor||cursor<=appliedThrough))throw new IllegalArgumentException("Invalid catchup ledger");
        invokedAt=Long.MIN_VALUE;running=false;lastEvents=0;lastNanos=0;
    }
}
