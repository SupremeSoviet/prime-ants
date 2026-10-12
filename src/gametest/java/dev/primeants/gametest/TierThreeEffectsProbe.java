package dev.primeants.gametest;

import com.google.gson.Gson;
import dev.primeants.PrimeAnts;
import dev.primeants.brood.*;
import dev.primeants.colony.*;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.gametest.mixin.GameTestHelperAccessor;
import dev.primeants.worker.*;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.Vec3;

/** Read-only observations scoped to one actual queen/world/test lifetime. No production gate is invoked to mutate it. */
public final class TierThreeEffectsProbe {
    private static final Map<UUID,TierThreeEffectsProbe> active=new HashMap<>();
    private static final Gson JSON=new Gson();
    private final GameTestHelper c;
    private final LasiusNigerEntity q;
    private final TierThreeFixture f;
    private final int deadline;
    private record SearchFrame(int inspections,boolean sugar,boolean protein){}
    private final Map<UUID,SearchFrame> searches=new HashMap<>();
    private final Map<String,Long> searchMetrics=new TreeMap<>();
    private final Map<UUID,String> lastWorker=new HashMap<>();
    private final Map<String,Long> workerTicks=new TreeMap<>();
    private final List<Map<String,Object>> intervalGates=new ArrayList<>(),chain=new ArrayList<>();
    private List<Map<String,Object>> acceptedChain=List.of();
    private final Map<String,Long> gateTicks=new TreeMap<>();
    private long intervalSamples;
    private long lastLay=-1,lastGate=-1,completed=-1;
    private boolean intervalOpen=true;
    private int consecutive,maxConsecutive;
    private Map<String,Object> latestGate=Map.of();
    private String gateSignature="";
    private boolean closed;
    private TierThreeEffectsProbe(GameTestHelper c,LasiusNigerEntity q,TierThreeFixture f,int deadline){this.c=c;this.q=q;this.f=f;this.deadline=deadline;active.put(q.getUUID(),this);}
    static TierThreeEffectsProbe watch(GameTestHelper c,LasiusNigerEntity q,TierThreeFixture f){
        return watch(c,q,f,20000);
    }
    static TierThreeEffectsProbe watch(GameTestHelper c,LasiusNigerEntity q,TierThreeFixture f,int deadline){
        var p=new TierThreeEffectsProbe(c,q,f,deadline);
        ((GameTestHelperAccessor)c).primeAntsTestInfo().addListener(new GameTestListener(){
            public void testStructureLoaded(GameTestInfo i){}
            public void testPassed(GameTestInfo i,GameTestRunner r){p.finish("passed");}
            public void testFailed(GameTestInfo i,GameTestRunner r){p.finish("failed_listener_diagnostic_not_extra_acceptance_time");}
            public void testAddedForRerun(GameTestInfo i,GameTestInfo copy,GameTestRunner r){p.finish("rerun");}
        });
        p.emit("start",Map.of("deadline",deadline,"broodMultiplier",BroodPile.multiplier(),"workers",f.workers(c,q).stream().map(w->Map.of("worker",w.getUUID(),"brood",w.broodId(),"sugar",w.nutrition().sugar(),"spentSugar",w.nutrition().spentSugar(),"age",w.elapsedAgeTicks(),"fasting",w.adultLife().fasting())).toList()));return p;
    }
    void completed(long loaded){if(completed<0){completed=loaded;lastLay=-1;lastGate=-1;intervalOpen=true;consecutive=0;chain.clear();intervalGates.clear();}}
    int consecutive(){return consecutive;}
    int maxConsecutive(){return maxConsecutive;}
    void emit(String type,Object value){var m=new LinkedHashMap<String,Object>();m.put("queen",q.getUUID());m.put("helperTick",c.getTick());m.put("nurseryLoaded",NestPlanFixture.pile(c,q).loadedTicks());m.put("type",type);m.put("value",value);PrimeAnts.LOGGER.info("T02 EFFECT OBS {}",JSON.toJson(m));}
    /** Pure mirror of FoodLimitedGrowth.allows; exact receipt and funding operands accompany the result. */
    private static String funding(FoodLimitedGrowth g,FoodLimitedGrowth.Supply s){
        long projected=s.adults()+s.brood()+1;
        long maintenance=(projected*FoodLimitedGrowth.WINDOW*5+4L*AdultLife.maintenancePeriod()-1)/(4L*AdultLife.maintenancePeriod());
        if(!s.complete())return "income_data_unavailable";
        if(s.hungry())return "hungry_adults_unserved";
        if(g.observedTicks()<FoodLimitedGrowth.WARMUP)return "income_warmup";
        if(g.recentSugar()<maintenance)return "recent_income_below_adult_commitments";
        if(s.storedSugar()<s.committedSugar()+Nutrition.EGG_SUGAR+Nutrition.LARVA_SUGAR+maintenance||s.storedProtein()<s.committedProtein()+Nutrition.EGG_PROTEIN+Nutrition.LARVA_PROTEIN)return "stores_below_brood_and_safety_commitments";
        return "funded_growth";
    }
    public static void beforeLay(ServerLevel l,BroodPile pile,LasiusNigerEntity queen,boolean care,boolean originalTerminal){
        var p=active.get(pile.queenId());if(p==null||p.closed||p.c.getLevel()!=l)return;
        var s=pile.supply(l);var g=pile.growth();var e=pile.stageEvaluation();
        int cap=e==null?0:AdultBound.effectiveCap(e.stage(),pile.adultCapacity());
        long occupied=ColonyMembers.get(l).occupied(pile.queenId());
        int hall=ColonyDevelopment.confirmedTier(l,pile.queenId(),p.q.founding().plan(),ChamberExcavation.HALL,ChamberFunction.QUEENS_HALL);
        String funded=funding(g,s);long sugar=queen==null?0:queen.nutrition().sugar(),protein=queen==null?0:queen.nutrition().protein();
        boolean qualified=care&&pile.operational()&&originalTerminal&&hall==3&&pile.records().size()<pile.nursery().slots()&&occupied+pile.records().size()<cap-1&&funded.equals("funded_growth")&&sugar>=Nutrition.EGG_SUGAR&&protein>=Nutrition.EGG_PROTEIN;
        var row=new LinkedHashMap<String,Object>();row.put("loaded",pile.loadedTicks());row.put("care",care);row.put("habitat",p.q.founding().plan().nurseryProblem(l,pile.queenId(),pile.operational()));row.put("operational",pile.operational());row.put("originalTerminal",originalTerminal);row.put("hallTier",hall);row.put("cadence",BroodPile.layingCadence(hall));row.put("brood",pile.records().size());row.put("slots",pile.nursery().slots());row.put("occupiedWorkers",occupied);row.put("populationCap",cap);row.put("funding",funded);row.put("observedIncomeTicks",g.observedTicks());row.put("recentSugar",g.recentSugar());row.put("recentProtein",g.recentProtein());row.put("supply",s);row.put("queenSugar",sugar);row.put("queenProtein",protein);row.put("qualified",qualified);p.latestGate=row;
        String sig=care+":"+hall+":"+funded+":"+(pile.records().size()<pile.nursery().slots())+":"+(occupied+pile.records().size()<cap-1)+":"+(sugar>=Nutrition.EGG_SUGAR)+":"+(protein>=Nutrition.EGG_PROTEIN);
        if(!sig.equals(p.gateSignature)){p.gateSignature=sig;p.emit("gate_transition_pre_decision",row);}
        if(p.c.getTick()==p.deadline-1)p.emit("last_bounded_pre_decision",Map.of("state",p.snapshot(),"scope","strictly_before_helper_deadline_before_production_laying_decision_at_recorded_clocks"));
        if(p.completed<0||p.c.getTick()>=p.deadline)return;
        p.gateTicks.merge("care="+care+";hall="+hall+";slotsFree="+(pile.records().size()<pile.nursery().slots())+";populationFree="+(occupied+pile.records().size()<cap-1)+";funding="+funded+";queenSugar="+(sugar>=Nutrition.EGG_SUGAR)+";queenProtein="+(protein>=Nutrition.EGG_PROTEIN),1L,Long::sum);
        if(p.lastLay>=0){
            if(p.lastGate>=0&&pile.loadedTicks()!=p.lastGate+1)p.intervalOpen=false;
            if(!qualified){p.intervalOpen=false;p.consecutive=0;p.chain.clear();}
            p.intervalSamples++;
            if(p.intervalGates.size()<32)p.intervalGates.add(row);
        }
        p.lastGate=pile.loadedTicks();
    }
    public static void afterLay(ServerLevel l,BroodPile pile,boolean laid){
        var p=active.get(pile.queenId());if(p==null||p.closed||p.c.getLevel()!=l||!laid)return;
        var event=new LinkedHashMap<String,Object>();event.put("loaded",pile.lastLayingTick());event.put("brood",pile.records().getLast().id());event.put("preDecision",p.latestGate);
        p.emit("actual_lay",event);
        if(p.completed<0||p.c.getTick()>=p.deadline)return;
        long delta=p.lastLay<0?-1:pile.lastLayingTick()-p.lastLay;
        boolean qualifies=p.lastLay>=0&&delta==BroodPile.layingCadence(3)&&p.intervalOpen&&p.intervalSamples==delta&&p.intervalGates.size()==delta;
        if(qualifies){p.consecutive++;p.maxConsecutive=Math.max(p.maxConsecutive,p.consecutive);p.chain.add(event);if(p.consecutive==3&&p.acceptedChain.isEmpty())p.acceptedChain=List.copyOf(p.chain);p.emit("qualified_interval",Map.of("fromLoaded",p.lastLay,"toLoaded",pile.lastLayingTick(),"consecutive",p.consecutive,"preDecisionGates",List.copyOf(p.intervalGates),"layingEvents",List.copyOf(p.chain)));}
        else{p.consecutive=0;p.chain.clear();p.chain.add(event);p.emit("interrupted_interval",Map.of("delta",delta,"gateInterrupted",!p.intervalOpen,"observedLoadedTicks",p.intervalSamples,"storedGatePrefix",List.copyOf(p.intervalGates)));}
        p.lastLay=pile.lastLayingTick();p.intervalOpen=true;p.intervalGates.clear();p.intervalSamples=0;
    }
    public static void fundingDecision(FoodLimitedGrowth growth,FoodLimitedGrowth.Supply supply,boolean allowed){
        for(var p:active.values())if(!p.closed&&p.c.getTick()<p.deadline&&NestPlanFixture.pile(p.c,p.q).growth()==growth){
            String expected=funding(growth,supply);p.c.assertTrue(allowed==expected.equals("funded_growth")&&growth.reason().equals(expected),"Read-only funding mirror agrees with the actual production decision and exact inputs");
        }
    }
    static Map<String,Long> contents(List<ItemStack> stacks){var m=new TreeMap<String,Long>();for(var s:stacks)m.merge(BuiltInRegistries.ITEM.getKey(s.getItem()).toString(),(long)s.getCount(),Long::sum);return m;}
    public static void searchBegin(ServerLevel l,LasiusNigerEntity w){
        var p=active.get(w.queenId());if(p==null||p.closed||p.c.getLevel()!=l||p.c.getTick()>=p.deadline||w.tickCount%20!=0)return;
        var t=w.workerTasks();p.searches.put(w.getUUID(),new SearchFrame(t.flowerInspections(),t.harvestRoom(l,false),t.harvestRoom(l,true)));
    }
    public static void searchEnd(ServerLevel l,LasiusNigerEntity w){
        var p=active.get(w.queenId());if(p==null||p.closed||p.c.getLevel()!=l)return;
        var before=p.searches.remove(w.getUUID());if(before==null)return;var t=w.workerTasks();int inspected=t.flowerInspections()-before.inspections();boolean closed=!before.sugar()&&!before.protein();
        p.searchMetrics.merge("native_inspections_"+(closed?"both_admission_gates_closed":"some_admission_gate_open"),(long)inspected,Long::sum);
        p.searchMetrics.merge("search_pulses_"+(closed?"both_admission_gates_closed":"some_admission_gate_open"),1L,Long::sum);
        if(inspected>0&&closed||t.phase()==WorkerTasks.Phase.APPROACH&&t.reason().equals("supported_dropped_material_found")){
            var row=new LinkedHashMap<String,Object>();row.put("worker",w.getUUID());row.put("nativeSugarAdmission",before.sugar());row.put("nativeProteinAdmission",before.protein());row.put("inspectionsBefore",before.inspections());row.put("inspectionsAfter",t.flowerInspections());row.put("phaseAfter",t.phase());row.put("reasonAfter",t.reason());row.put("cache",contents(p.f.fx.f.cache(p.c,p.q).contents()));row.put("materialSource",String.valueOf(t.sourceId()));row.put("storeClayRoom",MaterialStore.owned(l,p.q.getUUID(),p.q.founding().plan()).room(new ItemStack(Items.CLAY_BALL)));row.put("groundClay",NurseryUpgradeGameTest.ground(p.c,Items.CLAY_BALL));p.emit("search_pulse",row);
        }
    }
    Map<String,Object> inventories(){
        var cache=f.fx.f.cache(c,q);var store=MaterialStore.owned(c.getLevel(),q.getUUID(),q.founding().plan());var m=new LinkedHashMap<String,Object>();
        m.put("food",Map.of("items",contents(cache.contents()),"orderedStacks",cache.contents().stream().map(ItemStack::toString).toList(),"count",cache.size(),"owner",cache.componentOwner(),"capacity",cache.capacity(),"display",cache.getBlockState().toString()));
        m.put("materials",Map.of("items",contents(store.contents()),"orderedStacks",store.contents().stream().map(ItemStack::toString).toList(),"count",store.size(),"owner",store.componentOwner(),"capacity",store.capacity(),"display",store.getBlockState().toString()));return m;
    }
    void sample(){
        if(closed||c.getTick()>=deadline)return;var l=c.getLevel();var cache=f.fx.f.cache(c,q);var store=MaterialStore.owned(l,q.getUUID(),q.founding().plan());
        for(var w:f.workers(c,q)){
            var t=w.workerTasks();var cargo=w.getMainHandItem();boolean forager=q.founding().claimedBy(w);
            if(forager||t.nursing())workerTicks.merge(w.getUUID()+":"+t.phase()+":"+t.reason()+":"+(WorkerTasks.food(cargo)?"food":MaterialStore.material(cargo)?"material":"empty"),1L,Long::sum);
            String sig=t.phase()+":"+t.reason()+":"+cargo+":"+t.sourceId();if(!sig.equals(lastWorker.put(w.getUUID(),sig))&&(forager||t.nursing()||MaterialStore.material(cargo))){
                var row=new LinkedHashMap<String,Object>();row.put("worker",w.getUUID());row.put("phase",t.phase());row.put("reason",t.reason());row.put("cargo",cargo.toString());row.put("source",t.sourceId());row.put("forager",forager);row.put("position",w.position().subtract(Vec3.atBottomCenterOf(q.founding().plan().entrance())).toString());row.put("foodNow",contents(cache.contents()));row.put("incoming",f.workers(c,q).stream().filter(a->q.founding().claimedBy(a)||a.workerTasks().phase()==WorkerTasks.Phase.NURSE_RETURN).filter(a->WorkerTasks.food(a.getMainHandItem())).map(a->a.getUUID()+":"+a.getMainHandItem()).toList());row.put("appleTrip",cache.tripRoom(new ItemStack(Items.APPLE)));row.put("chickenTrip",cache.tripRoom(new ItemStack(Items.CHICKEN)));row.put("appleAdmits",cache.admits(new ItemStack(Items.APPLE)));row.put("chickenAdmits",cache.admits(new ItemStack(Items.CHICKEN)));row.put("materialRoom",MaterialStore.material(cargo)&&store.room(cargo));row.put("materialReach",WorkerTasks.reaches(l,w,Vec3.atBottomCenterOf(store.getBlockPos()).add(0,0.15,0)));emit("worker_transition",row);
            }
        }
        if(c.getTick()%500==0)emit("summary",snapshot());
    }
    private Map<String,Object> snapshot(){
        var m=new LinkedHashMap<String,Object>();m.put("queenCanonicalLookup",c.getLevel().getEntity(q.getUUID())==q);m.put("queenAlive",q.isAlive());m.put("queenRemoved",q.isRemoved());m.put("inventories",inventories());m.put("gatePreDecision",latestGate);m.put("completedLoaded",completed);m.put("consecutiveIntervals",consecutive);m.put("maximumConsecutiveIntervals",maxConsecutive);m.put("layingChain",chain);m.put("acceptedFourEventChain",acceptedChain);m.put("gateLoadedTicks",gateTicks);m.put("workerPhaseTicks",workerTicks);m.put("searchMetrics",searchMetrics);
        var loose=new TreeMap<String,Object>();for(var item:List.of(Items.STONE,Items.CLAY_BALL,Items.COBBLESTONE,Items.APPLE,Items.CHICKEN))loose.put(BuiltInRegistries.ITEM.getKey(item).toString(),Map.of("ground",NurseryUpgradeGameTest.ground(c,item),"cargo",f.workers(c,q).stream().filter(w->w.getMainHandItem().is(item)).mapToInt(w->w.getMainHandItem().getCount()).sum(),"custody",TransferCustody.get(c.getLevel()).contents().stream().filter(t->t.stack().is(item)&&c.getBounds().inflate(8).contains(t.position())).mapToInt(t->t.stack().getCount()).sum()));m.put("loose",loose);m.put("workers",f.workers(c,q).stream().filter(w->q.founding().claimedBy(w)||w.workerTasks().nursing()).map(w->Map.of("worker",w.getUUID(),"phase",w.workerTasks().phase(),"reason",w.workerTasks().reason(),"cargo",w.getMainHandItem().toString(),"feedingTicks",w.workerTasks().feedingTicks(),"recipient",String.valueOf(w.workerTasks().recipientId()),"position",w.position().subtract(Vec3.atBottomCenterOf(q.founding().plan().entrance())).toString())).toList());return m;
    }
    private void finish(String why){if(closed)return;emit("endpoint",Map.of("scope",why,"state",snapshot()));closed=true;active.remove(q.getUUID(),this);}
}
