package dev.primeants.gametest;
import com.google.gson.Gson;
import dev.primeants.PrimeAnts;
import dev.primeants.brood.*;
import dev.primeants.colony.*;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import dev.primeants.gametest.mixin.GameTestHelperAccessor;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;

/** Every row is from the actual pre-lay call. Preparation never credits food, receipts or growth. */
public final class HallFundingProbe {
    private static final Map<UUID,HallFundingProbe> ACTIVE=new HashMap<>();
    private static final Gson JSON=new Gson();
    private final GameTestHelper c;private final LasiusNigerEntity q;private final HallFundingFixture f;private final boolean shortage;
    private final List<Map<String,Object>> chain=new ArrayList<>(),gates=new ArrayList<>();
    private final Set<UUID> eggs=new HashSet<>();
    private long measurement=-1,lastLay=-1,lastGate=-1;private int consecutive,shortageDecisions;
    private boolean open=true,closed;private Map<String,Object> latest=Map.of();
    private HallFundingProbe(GameTestHelper c,LasiusNigerEntity q,HallFundingFixture f,boolean shortage){this.c=c;this.q=q;this.f=f;this.shortage=shortage;ACTIVE.put(q.getUUID(),this);}
    static HallFundingProbe watch(GameTestHelper c,LasiusNigerEntity q,HallFundingFixture f,boolean shortage){
        var p=new HallFundingProbe(c,q,f,shortage);
        ((GameTestHelperAccessor)c).primeAntsTestInfo().addListener(new GameTestListener(){
            public void testStructureLoaded(GameTestInfo i){}
            public void testPassed(GameTestInfo i,GameTestRunner r){p.finish("passed");}
            public void testFailed(GameTestInfo i,GameTestRunner r){p.finish("failed_listener_not_extra_measurement");}
            public void testAddedForRerun(GameTestInfo i,GameTestInfo copy,GameTestRunner r){p.finish("rerun");}
        });return p;
    }
    int shortageDecisions(){return shortageDecisions;} long measurement(){return measurement;} boolean accepted(){return consecutive==3;}
    private void emit(String type,Object value){PrimeAnts.LOGGER.info("T06 HALL FUNDING {}",JSON.toJson(Map.of("type",type,"shortage",shortage,"queen",q.getUUID(),"helperTick",c.getTick(),"nurseryLoaded",NestPlanFixture.pile(c,q).loadedTicks(),"value",value)));}
    private Map<String,Object> state(){return Map.of("lastPreDecision",latest,"measurementLoaded",measurement,"consecutive",consecutive,"eggs",eggs,"chain",chain,"shortageDecisions",shortageDecisions,"apples",f.apples,"chickens",f.chickens,"conservedApples",f.units(c,q,Items.APPLE),"conservedChickens",f.units(c,q,Items.CHICKEN));}
    void accept(){emit("accepted_physical_result",state());}
    private void finish(String why){if(closed)return;emit("endpoint",Map.of("scope",why,"state",state()));closed=true;ACTIVE.remove(q.getUUID(),this);}
    public static void beforeLay(ServerLevel l,BroodPile pile,LasiusNigerEntity queen,boolean care,boolean original){
        var p=ACTIVE.get(pile.queenId());if(p==null||p.closed||p.c.getLevel()!=l||p.c.getTick()>=5400)return;
        var s=pile.supply(l);var g=pile.growth();int hall=ColonyDevelopment.confirmedTier(l,p.q.getUUID(),p.q.founding().plan(),ChamberExcavation.HALL,ChamberFunction.QUEENS_HALL);
        long reserve=((s.adults()+s.brood()+1L)*FoodLimitedGrowth.WINDOW*5+4L*AdultLife.maintenancePeriod()-1)/(4L*AdultLife.maintenancePeriod());
        long requiredS=s.committedSugar()+Nutrition.EGG_SUGAR+Nutrition.LARVA_SUGAR+reserve,requiredP=s.committedProtein()+Nutrition.EGG_PROTEIN+Nutrition.LARVA_PROTEIN;
        int cap=pile.stageEvaluation()==null?0:AdultBound.effectiveCap(pile.stageEvaluation().stage(),pile.adultCapacity());
        boolean base=care&&pile.operational()&&original&&hall==3&&pile.records().size()<pile.nursery().slots()&&ColonyMembers.get(l).occupied(p.q.getUUID())+pile.records().size()<cap-1;
        boolean nutrition=queen!=null&&queen.nutrition().sugar()>=Nutrition.EGG_SUGAR&&queen.nutrition().protein()>=Nutrition.EGG_PROTEIN;
        boolean income=s.complete()&&!s.hungry()&&g.observedTicks()>=FoodLimitedGrowth.WARMUP&&g.recentSugar()>=reserve;
        boolean funded=s.storedSugar()>=requiredS&&s.storedProtein()>=requiredP;
        boolean eligible=base&&nutrition&&income&&funded;
        var row=new LinkedHashMap<String,Object>();row.put("loaded",pile.loadedTicks());row.put("care",care);row.put("habitat",p.q.founding().plan().nurseryProblem(l,p.q.getUUID(),pile.operational()));row.put("operational",pile.operational());row.put("originalTerminal",original);row.put("hallTier",hall);row.put("cadence",pile.layingInterval(l));row.put("slots",pile.nursery().slots());row.put("brood",pile.records().size());row.put("occupied",ColonyMembers.get(l).occupied(p.q.getUUID()));row.put("cap",cap);row.put("supply",s);row.put("requiredSugar",requiredS);row.put("requiredProtein",requiredP);row.put("maintenance",reserve);row.put("recentSugar",g.recentSugar());row.put("recentProtein",g.recentProtein());row.put("observedIncomeTicks",g.observedTicks());row.put("queenSugar",queen==null?0:queen.nutrition().sugar());row.put("queenProtein",queen==null?0:queen.nutrition().protein());row.put("qualified",eligible);row.put("proteinOnlyShortage",base&&nutrition&&income&&s.storedSugar()>=requiredS&&s.storedProtein()<requiredP);p.latest=row;
        if(eligible&&p.measurement<0){p.measurement=pile.loadedTicks();p.emit("measurement_boundary_pre_decision",row);}
        if(p.c.getTick()%200==0)p.emit("preparation_or_measurement_sample",row);
        if(p.lastLay>=0){
            if(!eligible||pile.loadedTicks()!=p.lastGate+1){p.open=false;p.consecutive=0;p.chain.clear();}
            p.gates.add(row);
        }
        p.lastGate=pile.loadedTicks();
    }
    public static void fundingDecision(FoodLimitedGrowth growth,FoodLimitedGrowth.Supply s,boolean allowed){
        for(var p:ACTIVE.values())if(!p.closed&&NestPlanFixture.pile(p.c,p.q).growth()==growth){
            if(Boolean.TRUE.equals(p.latest.get("qualified")))p.c.assertTrue(allowed,"Qualified mirror must agree with actual unchanged funding gate");
            if(Boolean.TRUE.equals(p.latest.get("proteinOnlyShortage"))){
                p.c.assertTrue(!allowed&&growth.reason().equals("stores_below_brood_and_safety_commitments"),"Actual existing gate refuses only the declared physical protein shortage");
                p.shortageDecisions++;if(p.shortageDecisions==1)p.emit("actual_protein_only_funding_refusal",Map.of("preDecision",p.latest,"actualReason",growth.reason(),"allowed",allowed));
            }
        }
    }
    public static void afterLay(ServerLevel l,BroodPile pile,boolean laid){
        var p=ACTIVE.get(pile.queenId());if(p==null||p.closed||p.c.getLevel()!=l||!laid||p.c.getTick()>=5400)return;
        UUID id=pile.records().getLast().id();p.c.assertTrue(p.eggs.add(id),"Each egg is a distinct actual laid identity");
        var event=Map.<String,Object>of("brood",id,"loaded",pile.lastLayingTick(),"preDecision",p.latest);p.emit("actual_lay",event);
        if(p.lastLay>=0){long delta=pile.lastLayingTick()-p.lastLay;
            if(p.open&&delta==BroodPile.layingCadence(3)&&p.gates.size()==delta){p.consecutive++;p.emit("qualified_interval",Map.of("from",p.lastLay,"to",pile.lastLayingTick(),"interval",delta,"consecutive",p.consecutive,"allPreDecisionRows",List.copyOf(p.gates)));}
            else{p.consecutive=0;p.chain.clear();p.emit("interruption_reset",Map.of("delta",delta,"eligibleThroughout",p.open,"observedRows",p.gates.size()));}
        }
        p.chain.add(event);p.lastLay=pile.lastLayingTick();p.open=true;p.gates.clear();
        if(p.consecutive==3)p.c.assertTrue(p.chain.size()==4,"Three intervals join exactly four actual eggs");
    }
}
