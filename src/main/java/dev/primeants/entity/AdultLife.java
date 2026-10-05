package dev.primeants.entity;

import dev.primeants.founding.QueenFounding;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Loaded adult ticks only. Configuration is selected once at birth, independent of brood speed. */
public final class AdultLife {
    public static final long DEFAULT_LIFESPAN=144_000, DEFAULT_FASTING=24_000;
    private long lifespan=duration("prime_ants.adultLifespanTicks",DEFAULT_LIFESPAN);
    private long grace=duration("prime_ants.adultFastingTicks",DEFAULT_FASTING);
    private long fasting,maintenanceTicks,activeTicks;
    private int damageTicks,mealTicks;
    private boolean started;
    private String death="";
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
        activeTicks++;
        if(ant.nutrition().spend(1,0)){maintenanceTicks++;fasting=0;}else if(fasting<Long.MAX_VALUE)fasting++;
        boolean old=ant.form()==AntForm.WORKER&&ant.elapsedAgeTicks()>=lifespan;
        boolean starving=fasting>=grace;
        if(!old&&!starving){damageTicks=0;return;}
        if(++damageTicks<20)return;
        damageTicks=0;death=old?"age_limit":"starvation";
        ant.hurtServer(level,ant.damageSources().starve(),1.0F);
        if(ant.isAlive())death="";
    }
    public void save(ValueOutput o){o.putLong("Lifespan",lifespan);o.putLong("FastingGrace",grace);o.putLong("Fasting",fasting);o.putLong("Maintenance",maintenanceTicks);o.putLong("ActiveTicks",activeTicks);o.putInt("DamageTicks",damageTicks);o.putInt("MealTicks",mealTicks);o.putBoolean("Started",started);o.putString("Death",death);}
    public void load(ValueInput i,LasiusNigerEntity ant){
        // Legacy duration defaults are stable production values, never current test configuration.
        // Preserve age; conservatively count existing worker age as fasting rather than renew grace.
        lifespan=i.getLongOr("Lifespan",DEFAULT_LIFESPAN);grace=i.getLongOr("FastingGrace",DEFAULT_FASTING);
        boolean legacy=i.getLong("Lifespan").isEmpty();
        started=i.getBooleanOr("Started",ant.form()==AntForm.WORKER||ant.founding().lifecycle()!=QueenFounding.Lifecycle.CLAUSTRAL);
        activeTicks=i.getLongOr("ActiveTicks",legacy&&ant.form()==AntForm.WORKER?ant.elapsedAgeTicks():0);
        fasting=i.getLongOr("Fasting",legacy&&ant.form()==AntForm.WORKER?ant.elapsedAgeTicks():0);
        maintenanceTicks=i.getLongOr("Maintenance",0);damageTicks=i.getIntOr("DamageTicks",0);mealTicks=i.getIntOr("MealTicks",0);death=i.getStringOr("Death","");
        if(lifespan<1||grace<1||fasting<0||activeTicks<0||maintenanceTicks<0||damageTicks<0||damageTicks>=20||mealTicks<0||mealTicks>=20)throw new IllegalArgumentException("Invalid persisted adult life");
    }
}
