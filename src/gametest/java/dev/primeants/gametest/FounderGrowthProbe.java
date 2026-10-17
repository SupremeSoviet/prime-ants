package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.PrimeAnts;
import dev.primeants.brood.*;
import dev.primeants.entity.*;
import dev.primeants.worker.*;
import dev.primeants.gametest.mixin.GameTestHelperAccessor;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;

/** Bounded passive founder checkpoint. Never calls allows, development or pathfinding. */
public final class FounderGrowthProbe {
    private static final Map<UUID,FounderGrowthProbe> ACTIVE=new HashMap<>();
    private static final Gson JSON=new Gson();
    private final GameTestHelper c;private final LasiusNigerEntity q;
    private String lastDecision="";private long sample=-1,callbacks,allows;
    private final Map<String,Long> guards=new TreeMap<>(),actions=new TreeMap<>();
    private String guard="";
    private FounderGrowthProbe(GameTestHelper c,LasiusNigerEntity q){this.c=c;this.q=q;}
    static void watch(GameTestHelper c,LasiusNigerEntity q){
        var p=new FounderGrowthProbe(c,q);ACTIVE.put(q.getUUID(),p);
        ((GameTestHelperAccessor)c).primeAntsTestInfo().addListener(new GameTestListener(){
            public void testStructureLoaded(GameTestInfo i){}
            public void testPassed(GameTestInfo i,GameTestRunner r){p.finish("passed");}
            public void testFailed(GameTestInfo i,GameTestRunner r){p.finish("failed");}
            public void testAddedForRerun(GameTestInfo i,GameTestInfo copy,GameTestRunner r){p.finish("rerun");}
        });
    }
    private BroodPile pile(){return NestPlanFixture.pile(c,q);}
    private static FounderGrowthProbe get(BroodPile p){return ACTIVE.get(p.queenId());}
    private void emit(String type,Object value){var p=pile();var row=new LinkedHashMap<String,Object>();row.put("type",type);row.put("queen",q.getUUID());row.put("helper",c.getTick());row.put("loaded",p==null?-1:p.loadedTicks());row.put("game",c.getLevel().getGameTime());row.put("value",value);PrimeAnts.LOGGER.info("T15 FOUNDER {}",JSON.toJson(row));}
    private Map<String,Object> state(){
        var p=pile();var l=c.getLevel();var row=new LinkedHashMap<>(FoodServiceProbe.passiveSnapshot(c,q));
        row.put("queenFullNutrition",q.nutrition().snapshot());row.put("lastLay",p.lastLayingTick());row.put("callbacks",callbacks);row.put("allowsCalls",allows);row.put("earlierGuardCounts",new TreeMap<>(guards));row.put("cacheActions",new TreeMap<>(actions));
        row.put("adultFullNutrition",ColonyMembers.get(l).members(q.getUUID()).stream().filter(m->!m.dead()).map(m->l.getEntity(m.worker())).filter(a->a instanceof LasiusNigerEntity).map(a->(LasiusNigerEntity)a).map(a->Map.of("id",a.getUUID(),"nutrition",a.nutrition().snapshot(),"life",a.adultLife().snapshot(),"canonical",l.getEntity(a.getUUID())==a,"authorizedForager",a.workerTasks().authorized(l),"authorizedNurse",a.workerTasks().nursingAuthorized(l))).toList());
        var history=new TreeMap<String,Object>();for(var e:AdultHistory.get(l).records().entrySet()){var r=JsonParser.parseString(e.getValue()).getAsJsonObject();if(r.get("queen").getAsString().equals(q.getUUID().toString()))history.put(e.getKey().toString(),r);}row.put("archivedAdults",history);
        row.put("broodUnitsIncludingRetiredAndExpired",Map.of("apples",p.consumedApples(),"berries",p.consumedBerries(),"chickens",p.consumedChickens(),"nectar",p.consumedNectar(),"nectarV2",p.consumedNectarV2(),"prey",p.consumedPrey(),"flesh",p.consumedFlesh()));row.put("expiredBrood",p.expired());row.put("members",ColonyMembers.get(l).members(q.getUUID()));
        return row;
    }
    public static void before(ServerLevel l,BroodPile pile,LasiusNigerEntity q,boolean care,boolean original){
        var p=get(pile);if(p==null||p.c.getLevel()!=l)return;p.callbacks++;
        p.guard=!care?"care":!pile.operational()?"not_operational":!original?"original_not_terminal":pile.loadedTicks()-pile.lastLayingTick()<pile.layingInterval(l)?"cadence":pile.records().size()>=pile.nursery().slots()?"slots":"capacity_or_funding_or_queen";
    }
    public static void after(BroodPile pile,boolean laid){var p=get(pile);if(p==null)return;p.guards.merge(p.guard,1L,Long::sum);if(laid)p.emit("actual_lay",p.state());}
    public static void funding(FoodLimitedGrowth g,FoodLimitedGrowth.Supply s,boolean result){
        for(var p:ACTIVE.values())if(p.pile()!=null&&p.pile().growth()==g){p.allows++;p.guard="actual_allows_"+result+":"+g.reason();String sig=p.guard+":"+s.adults()+":"+s.brood();if(!sig.equals(p.lastDecision)){p.lastDecision=sig;p.emit("actual_allows_return",Map.of("allowed",result,"reason",g.reason(),"supply",s,"recentSugar",g.recentSugar(),"recentProtein",g.recentProtein(),"observedTicks",g.observedTicks(),"requiredIncome",((s.adults()+s.brood()+1L)*FoodLimitedGrowth.WINDOW*5+4L*AdultLife.maintenancePeriod()-1)/(4L*AdultLife.maintenancePeriod())));}}
    }
    public static void sampled(FoodLimitedGrowth g,long loaded,FoodLimitedGrowth.Supply s,List<Long> times,List<Long> sugar,List<Long> protein){
        for(var p:ACTIVE.values())if(p.pile()!=null&&p.pile().growth()==g){long last=times.isEmpty()?-1:times.getLast();if(p.sample==last)return;p.sample=last;p.emit("receipt_sample",Map.of("observedAt",loaded,"times",List.copyOf(times),"sugar",List.copyOf(sugar),"protein",List.copyOf(protein),"supply",s,"reasonIsCached",g.reason()));if(loaded%1000<200||p.c.getTick()>=12000&&p.c.getTick()<=30000)p.emit("physical_sample",p.state());}
    }
    public static void cache(ServerLevel l,LasiusNigerEntity w,String method,boolean allowed){var p=ACTIVE.get(w.queenId());if(p==null||p.c.getLevel()!=l)return;p.actions.merge(method+"="+allowed,1L,Long::sum);if(allowed)p.emit("cache_action",Map.of("worker",w.getUUID(),"method",method,"cargoAfter",w.getMainHandItem().toString(),"phase",w.workerTasks().phase(),"position",w.position().toString()));}
    private void finish(String outcome){if(pile()!=null)emit("endpoint_"+outcome,state());ACTIVE.remove(q.getUUID(),this);}
}
