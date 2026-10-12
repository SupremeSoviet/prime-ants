package dev.primeants.entity;

import dev.primeants.founding.QueenFounding;
import dev.primeants.worker.Nutrition;
import java.util.OptionalLong;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Loaded adult ticks only. Configuration is selected once at birth, independent of brood speed. */
public final class AdultLife {
    public static final long DEFAULT_LIFESPAN=144_000, DEFAULT_FASTING=24_000;
    private long lifespan=duration("prime_ants.adultLifespanTicks",DEFAULT_LIFESPAN);
    private long grace=duration("prime_ants.adultFastingTicks",DEFAULT_FASTING);
    private long fasting,maintenanceTicks,activeTicks;
    private int damageTicks,mealTicks,coverageRemaining;
    private long coveredTicks;
    public int coverageRemaining(){return coverageRemaining;} public long coveredTicks(){return coveredTicks;}
    private boolean started;
    private String death="";
    public static int maintenancePeriod(){return 4;}
    public static int damagePeriod(){return 20;}
    public enum Phase { ACTIVE, RESERVE_ONLY, UNKNOWN }
    public record State(long lifespan,long grace,long fasting,long maintenanceTicks,long activeTicks,
                        int damageTicks,int mealTicks,int coverageRemaining,long coveredTicks,boolean started,String death) {
        public boolean valid(){return lifespan>0&&grace>0&&fasting>=0&&maintenanceTicks>=0&&activeTicks>=0
            &&damageTicks>=0&&damageTicks<damagePeriod()&&mealTicks>=0&&mealTicks<20
            &&coverageRemaining>=0&&coverageRemaining<maintenancePeriod()&&coveredTicks>=0&&death!=null&&death.isEmpty();}
        State active(){return new State(lifespan,grace,fasting,maintenanceTicks,activeTicks,damageTicks,mealTicks,coverageRemaining,coveredTicks,true,death);}
    }
    public State snapshot(){return new State(lifespan,grace,fasting,maintenanceTicks,activeTicks,damageTicks,mealTicks,coverageRemaining,coveredTicks,started,death);}
    private void restore(State s){fasting=s.fasting();maintenanceTicks=s.maintenanceTicks();activeTicks=s.activeTicks();damageTicks=s.damageTicks();coverageRemaining=s.coverageRemaining();coveredTicks=s.coveredTicks();started=s.started();}
    /** Accepted nonnegative counters retain their maximum, in both ordinary and aggregate advancement. */
    public static long saturatingAdd(long a,long b){if(a<0||b<0)throw new IllegalArgumentException("Negative counter");return a>Long.MAX_VALUE-b?Long.MAX_VALUE:a+b;}
    record Evolution(State life,Nutrition.State nutrition,boolean damage) { }
    private static long coverage(State s,Nutrition.State n){return Math.addExact(s.coverageRemaining(),Math.multiplyExact(n.sugar(),maintenancePeriod()));}
    private static OptionalLong sum(long a,long b){return a>Long.MAX_VALUE-b?OptionalLong.empty():OptionalLong.of(a+b);}
    /** First consecutive qualifying tick, including coverage's reset of prior fasting/damage. */
    private static OptionalLong qualifying(State s,Nutrition.State n,AntForm form,long age){
        long covered=coverage(s,n);
        OptionalLong starving=covered>0?sum(covered,s.grace()):OptionalLong.of(Math.max(1,s.grace()-s.fasting()));
        OptionalLong old=form==AntForm.WORKER?OptionalLong.of(age>=s.lifespan()?1:s.lifespan()-age):OptionalLong.empty();
        if(old.isEmpty())return starving;if(starving.isEmpty())return old;
        return OptionalLong.of(Math.min(old.getAsLong(),starving.getAsLong()));
    }
    static OptionalLong firstDamage(State s,Nutrition.State n,AntForm form,long age){
        if(!s.started())return OptionalLong.empty();
        var first=qualifying(s,n,form,age);if(first.isEmpty())return first;
        return sum(first.getAsLong(),damagePeriod()-1L-(first.getAsLong()==1?s.damageTicks():0));
    }
    /** Constant work shared by the live one-tick owner and the non-terminal preview. */
    static Evolution evolve(State s,Nutrition.State n,AntForm form,long age,long ticks){
        return evolve(s,n,form,age,ticks,false);
    }
    private static Evolution evolve(State s,Nutrition.State n,AntForm form,long age,long ticks,boolean immune){
        if(!s.started()||ticks==0)return new Evolution(s,n,false);
        long covered=Math.min(ticks,coverage(s,n)),newlyCovered=Math.max(0,covered-s.coverageRemaining());
        long meals=newlyCovered/maintenancePeriod()+(newlyCovered%maintenancePeriod()==0?0:1);
        int remaining=ticks<=s.coverageRemaining()?s.coverageRemaining()-(int)ticks
            :(int)(Math.multiplyExact(meals,maintenancePeriod())-newlyCovered);
        long fasting=covered>0?ticks-covered:(s.fasting()>Long.MAX_VALUE-ticks?Long.MAX_VALUE:s.fasting()+ticks);
        var first=qualifying(s,n,form,age);long damage=first.isPresent()&&first.getAsLong()<=ticks
            ?Math.addExact(ticks-first.getAsLong()+1,first.getAsLong()==1?s.damageTicks():0):0;
        if(!immune&&damage>damagePeriod())throw new IllegalArgumentException("Evolution crosses health event");
        boolean event=!immune&&damage==damagePeriod();
        return new Evolution(new State(s.lifespan(),s.grace(),fasting,saturatingAdd(s.maintenanceTicks(),meals),
            saturatingAdd(s.activeTicks(),ticks),immune?(int)(damage%damagePeriod()):event?0:(int)damage,s.mealTicks(),remaining,
            saturatingAdd(s.coveredTicks(),covered),true,s.death()),n.spend(meals,0),event);
    }
    private static long duration(String key,long fallback){long n=Long.parseLong(System.getProperty(key,Long.toString(fallback)));if(n<1)throw new IllegalArgumentException("Adult duration must be positive: "+key);return n;}
    public long lifespan(){return lifespan;} public long grace(){return grace;}
    public long fasting(){return fasting;} public long maintenanceTicks(){return maintenanceTicks;}
    public long activeTicks(){return activeTicks;} public int mealTicks(){return mealTicks;}
    public String death(){return death;} public boolean started(){return started;}
    public boolean hungry(){return started&&fasting>=Math.max(1,grace/2);}
    public void resetMeal(){mealTicks=0;}
    public void selectAtEmergence(LasiusNigerEntity ant,long selectedLifespan,long selectedGrace){
        if(ant.elapsedAgeTicks()!=0||started||selectedLifespan<1||selectedGrace<1)throw new IllegalStateException("Adult policy is birth-time only");
        lifespan=selectedLifespan;grace=selectedGrace;
    }
    public boolean mealAction(){return ++mealTicks>=20;}
    public void tick(ServerLevel level,LasiusNigerEntity ant){
        if(!started&&(ant.form()==AntForm.WORKER||!ant.founding().reserveOnlyFounding(level)))started=true;
        if(!started)return; // First-clutch queen uses only the unchanged 39,000 reserve budget.
        advance(level,ant,ant.elapsedAgeTicks()-1,1,false);
    }
    /** Shared spending and damage owner. The coordinator stops at the next health event. */
    public long nextEvent(LasiusNigerEntity ant,Phase phase){
        var s=snapshot();if(!s.started()&&phase==Phase.ACTIVE)s=s.active();
        long result=ant.isPermanentlyInvulnerable()?Long.MAX_VALUE:firstDamage(s,ant.nutrition().snapshot(),ant.form(),ant.elapsedAgeTicks()).orElse(Long.MAX_VALUE);
        if(s.started()&&!hungry())result=Math.min(result,saturatingAdd(coverage(s,ant.nutrition().snapshot()),Math.max(1,Math.max(1,grace/2)-fasting)));
        return Math.max(1,result);
    }
    void advance(ServerLevel level,LasiusNigerEntity ant,long priorAge,long ticks,boolean aggregate){
        var s=snapshot();
        if(!s.started()&&(ant.form()==AntForm.WORKER||!ant.founding().reserveOnlyFounding(level)))s=s.active();
        // Permanent source-independent immunity is stable for this server-thread invocation. Ordinary
        // steps and temporary cooldowns keep the original damage path; only repeated no-op events fold.
        var next=evolve(s,ant.nutrition().snapshot(),ant.form(),priorAge,ticks,aggregate&&ant.isPermanentlyInvulnerable());
        long spent=ant.nutrition().sugar()-next.nutrition().sugar();
        if(!ant.nutrition().spend(spent,0))throw new IllegalStateException("Resolved maintenance spend failed");
        restore(next.life());
        if(!next.damage())return;
        death=ant.form()==AntForm.WORKER&&ant.elapsedAgeTicks()>=lifespan?"age_limit":"starvation";
        // Historical damage events are separated by simulation ticks, not by this one server invocation.
        // Preserve the currently loaded invulnerability window for the resumed ordinary tick.
        int cooldown=ant.damageCooldownTime;
        if(aggregate)ant.damageCooldownTime=0;
        ant.hurtServer(level,ant.damageSources().starve(),1.0F);
        if(aggregate)ant.damageCooldownTime=cooldown;
        if(ant.isAlive())death="";
    }
    public void save(ValueOutput o){o.putInt("CoverageRemaining",coverageRemaining);o.putLong("CoveredTicks",coveredTicks);o.putLong("Lifespan",lifespan);o.putLong("FastingGrace",grace);o.putLong("Fasting",fasting);o.putLong("Maintenance",maintenanceTicks);o.putLong("ActiveTicks",activeTicks);o.putInt("DamageTicks",damageTicks);o.putInt("MealTicks",mealTicks);o.putBoolean("Started",started);o.putString("Death",death);}
    public void load(ValueInput i,LasiusNigerEntity ant){
        // Legacy duration defaults are stable production values, never current test configuration.
        // Preserve age; conservatively count existing worker age as fasting rather than renew grace.
        lifespan=i.getLongOr("Lifespan",DEFAULT_LIFESPAN);grace=i.getLongOr("FastingGrace",DEFAULT_FASTING);
        boolean legacy=i.getLong("Lifespan").isEmpty();
        started=i.getBooleanOr("Started",ant.form()==AntForm.WORKER||ant.founding().lifecycle()!=QueenFounding.Lifecycle.CLAUSTRAL);
        activeTicks=i.getLongOr("ActiveTicks",legacy&&ant.form()==AntForm.WORKER?ant.elapsedAgeTicks():0);
        fasting=i.getLongOr("Fasting",legacy&&ant.form()==AntForm.WORKER?ant.elapsedAgeTicks():0);
        coverageRemaining=i.getIntOr("CoverageRemaining",0);coveredTicks=i.getLongOr("CoveredTicks",i.getLongOr("Maintenance",0));
        maintenanceTicks=i.getLongOr("Maintenance",0);damageTicks=i.getIntOr("DamageTicks",0);mealTicks=i.getIntOr("MealTicks",0);death=i.getStringOr("Death","");
        if(coverageRemaining<0||coverageRemaining>=maintenancePeriod()||coveredTicks<0||lifespan<1||grace<1||fasting<0||activeTicks<0||maintenanceTicks<0||damageTicks<0||damageTicks>=20||mealTicks<0||mealTicks>=20)throw new IllegalArgumentException("Invalid persisted adult life");
    }
}
