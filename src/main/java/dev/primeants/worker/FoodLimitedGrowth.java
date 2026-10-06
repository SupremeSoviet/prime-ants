package dev.primeants.worker;

import java.util.*;
import net.minecraft.world.level.storage.*;
import com.mojang.serialization.Codec;

/** Descriptive rolling receipt differences. This object cannot credit or spend food. */
public final class FoodLimitedGrowth {
    public static final int WINDOW=2400, SAMPLE=200, WARMUP=2400;
    private final List<Long> times=new ArrayList<>(),sugar=new ArrayList<>(),protein=new ArrayList<>();
    private long clock,pauses,resumes;
    private boolean open;
    private String reason="income_warmup";
    public record Supply(boolean complete,boolean hungry,long foodSugar,long foodProtein,long storedSugar,long storedProtein,
                         long committedSugar,long committedProtein,int adults,int brood){}
    public String reason(){return reason;} public long pauses(){return pauses;} public long resumes(){return resumes;}
    public long recentSugar(){return sugar.size()<2?0:sugar.getLast()-sugar.getFirst();}
    public long recentProtein(){return protein.size()<2?0:protein.getLast()-protein.getFirst();}
    public long observedTicks(){return times.size()<2?0:times.getLast()-times.getFirst();}
    public void observe(long loadedTick,Supply s){
        clock=loadedTick;
        if(!s.complete()){times.clear();sugar.clear();protein.clear();set(false,"income_data_unavailable");return;}
        if(!sugar.isEmpty()&&(s.foodSugar()<sugar.getLast()||s.foodProtein()<protein.getLast())){times.clear();sugar.clear();protein.clear();set(false,"income_receipts_regressed");}
        if(times.isEmpty()||clock-times.getLast()>=SAMPLE){
            times.add(clock);sugar.add(s.foodSugar());protein.add(s.foodProtein());
            while(times.size()>WINDOW/SAMPLE+1){times.removeFirst();sugar.removeFirst();protein.removeFirst();}
        }
    }
    private void set(boolean value,String why){if(value!=open){if(value)resumes++;else pauses++;}open=value;reason=why;}
    public boolean allows(Supply s){
        String why;
        long projected=s.adults()+s.brood()+1;
        // 25% safety over all existing/future adults for one entire window.
        long maintenance=(projected*WINDOW*5+4L*dev.primeants.entity.AdultLife.maintenancePeriod()-1)/(4L*dev.primeants.entity.AdultLife.maintenancePeriod());
        if(!s.complete())why="income_data_unavailable";
        else if(s.hungry())why="hungry_adults_unserved";
        else if(observedTicks()<WARMUP)why="income_warmup";
        else if(recentSugar()<maintenance)why="recent_income_below_adult_commitments";
        // Receipts are a rate observation, never available funding. Reserve the
        // existing brood, this investment and maintenance from food still here.
        else if(s.storedSugar()<s.committedSugar()+Nutrition.EGG_SUGAR+Nutrition.LARVA_SUGAR+maintenance
            ||s.storedProtein()<s.committedProtein()+Nutrition.EGG_PROTEIN+Nutrition.LARVA_PROTEIN)why="stores_below_brood_and_safety_commitments";
        else why="funded_growth";
        set(why.equals("funded_growth"),why);return open;
    }
    public void save(ValueOutput o){o.store("Times",Codec.LONG.listOf(),times);o.store("Sugar",Codec.LONG.listOf(),sugar);o.store("Protein",Codec.LONG.listOf(),protein);o.putLong("Clock",clock);o.putLong("Pauses",pauses);o.putLong("Resumes",resumes);o.putBoolean("Open",open);o.putString("Reason",reason);}
    public void load(ValueInput i){times.clear();sugar.clear();protein.clear();times.addAll(i.read("Times",Codec.LONG.listOf()).orElse(List.of()));sugar.addAll(i.read("Sugar",Codec.LONG.listOf()).orElse(List.of()));protein.addAll(i.read("Protein",Codec.LONG.listOf()).orElse(List.of()));clock=i.getLongOr("Clock",0);pauses=i.getLongOr("Pauses",0);resumes=i.getLongOr("Resumes",0);open=i.getBooleanOr("Open",false);reason=i.getStringOr("Reason","income_warmup");
        if(times.size()>WINDOW/SAMPLE+1||times.size()!=sugar.size()||times.size()!=protein.size()||clock<0||pauses<0||resumes<0)throw new IllegalArgumentException("Invalid food flow");
        for(int n=0;n<times.size();n++)if(times.get(n)<0||times.get(n)>clock||sugar.get(n)<0||protein.get(n)<0||n>0&&(times.get(n)-times.get(n-1)!=SAMPLE||sugar.get(n)<sugar.get(n-1)||protein.get(n)<protein.get(n-1)))throw new IllegalArgumentException("Invalid rolling receipts");
    }
}
