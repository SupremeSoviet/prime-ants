package dev.primeants.gametest;
import com.google.gson.Gson;
import dev.primeants.PrimeAnts;
import dev.primeants.brood.BroodPile;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.mixin.GameTestHelperAccessor;
import dev.primeants.worker.FoodLimitedGrowth;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
/** Actual original income/lay returns plus the existing scoped passive food observer. */
public final class FullStoreCausalProbe {
    private static final String TARGET="prime_ants_test:mining_game_test_full_store_prevents_removal_then_physical_stock_release_and_two_evaluations_resume_mining";
    private static final Gson JSON=new Gson();
    private static final Map<GameTestHelper,Scope> ACTIVE=new IdentityHashMap<>();
    private static final class Scope {final GameTestHelper c;final LasiusNigerEntity q;long lastAllows=Long.MIN_VALUE;Scope(GameTestHelper c,LasiusNigerEntity q){this.c=c;this.q=q;}}
    private FullStoreCausalProbe(){}
    public static void attach(GameTestHelper c,LasiusNigerEntity q){
        if(!((GameTestHelperAccessor)c).primeAntsTestInfo().id().toString().equals(TARGET)||ACTIVE.containsKey(c))return;
        var s=new Scope(c,q);ACTIVE.put(c,s);FoodServiceProbe.watch(c,q,60000,"t21_fullstore_original_legacy");
        ((GameTestHelperAccessor)c).primeAntsTestInfo().addListener(new GameTestListener(){
            public void testStructureLoaded(GameTestInfo i){}
            public void testPassed(GameTestInfo i,GameTestRunner r){close(s,"passed");}
            public void testFailed(GameTestInfo i,GameTestRunner r){close(s,"failed");}
            public void testAddedForRerun(GameTestInfo i,GameTestInfo copy,GameTestRunner r){close(s,"rerun");}
        });emit(s,"attach",Map.of("seed",c.getLevel().getSeed(),"bounds",c.getBounds().toString(),"rotation",c.getTestRotation().name(),"scope","Actual calls distinguished from existing calculated FoodService snapshots; original fixture/LEGACY profile/60000 bound unchanged"));
    }
    private static void close(Scope s,String why){emit(s,"endpoint_after_listener_not_acceptance",why);ACTIVE.remove(s.c);}
    private static BroodPile pile(Scope s){var p=s.q.founding().plan();return p!=null&&s.c.getLevel().getBlockEntity(p.nursery()) instanceof BroodPile b?b:null;}
    private static void emit(Scope s,String event,Object value){var m=new LinkedHashMap<String,Object>();m.put("event",event);m.put("helperTick",s.c.getTick());m.put("gameTime",s.c.getLevel().getGameTime());m.put("queen",s.q.getUUID());var p=pile(s);m.put("nurseryLoaded",p==null?null:p.loadedTicks());m.put("value",value);PrimeAnts.LOGGER.info("T21 FULLSTORE GATE {}",JSON.toJson(m));}
    public static void growth(FoodLimitedGrowth g,FoodLimitedGrowth.Supply actual,boolean result){
        for(var s:ACTIVE.values()){var p=pile(s);if(p==null||p.growth()!=g)continue;s.lastAllows=s.c.getLevel().getGameTime();
            long projected=actual.adults()+actual.brood()+1L;long reference=(projected*FoodLimitedGrowth.WINDOW*5+4L*dev.primeants.entity.AdultLife.maintenancePeriod()-1)/(4L*dev.primeants.entity.AdultLife.maintenancePeriod());
            emit(s,"actual_ordinary_income_allows_return",Map.of("result",result,"reason_after_original_call",g.reason(),"actualSupplyArgument",actual,"recentSugar",g.recentSugar(),"recentProtein",g.recentProtein(),"observedTicks",g.observedTicks(),"maintenance_formula_reference_not_extra_gate",reference));
        }
    }
    public static void lay(ServerLevel l,BroodPile p,boolean care,boolean aggregate,boolean result){for(var s:ACTIVE.values())if(s.c.getLevel()==l&&s.q.getUUID().equals(p.queenId()))emit(s,"actual_lay_return",Map.of("actualCareArgument",care,"actualAggregateArgument",aggregate,"result",result,"condition_after_original_call",p.condition(),"income_call_observed_this_gameTime",s.lastAllows==l.getGameTime(),"brood",p.records().size()));}
}
