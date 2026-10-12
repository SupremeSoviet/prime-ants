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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

/** Scoped evidence only: observes existing actions, never selects a path or invokes a second decision. */
public final class FoodServiceProbe {
    private static final Map<UUID,FoodServiceProbe> ACTIVE=new HashMap<>();
    private static final Gson JSON=new Gson();
    private final GameTestHelper c;
    private final LasiusNigerEntity q;
    private final int deadline;
    private final String identity;
    private boolean closed;
    private String gateSignature="";
    private Map<String,Object> gate=Map.of();
    private final Map<UUID,String> workerSignatures=new HashMap<>();
    private final Map<String,Long> refusals=new TreeMap<>();
    private final Map<UUID,Integer> searches=new HashMap<>();
    private FoodServiceProbe(GameTestHelper c,LasiusNigerEntity q,int deadline,String identity){this.c=c;this.q=q;this.deadline=deadline;this.identity=identity;}
    static void watch(GameTestHelper c,LasiusNigerEntity q,int deadline,String identity){
        var p=new FoodServiceProbe(c,q,deadline,identity);
        if(ACTIVE.putIfAbsent(q.getUUID(),p)!=null)throw new IllegalStateException("Duplicate food-service observer");
        ((GameTestHelperAccessor)c).primeAntsTestInfo().addListener(new GameTestListener(){
            public void testStructureLoaded(GameTestInfo i){}
            public void testPassed(GameTestInfo i,GameTestRunner r){p.finish("passed");}
            public void testFailed(GameTestInfo i,GameTestRunner r){p.finish("failure_listener_after_deadline_not_acceptance_time");}
            public void testAddedForRerun(GameTestInfo i,GameTestInfo copy,GameTestRunner r){p.finish("rerun");}
        });
        c.onEachTick(()->{if(!p.closed&&c.getTick()<deadline&&(c.getTick()%200==0||c.getTick()==deadline-1))p.emit(c.getTick()==deadline-1?"last_bounded_terminal":"periodic",p.snapshot());});
        p.emit("start",Map.of("deadline",deadline,"broodMultiplier",BroodPile.multiplier(),"scope","actual_level_queen_lifetime_read_only"));
    }
    private static FoodServiceProbe get(ServerLevel l,UUID id){var p=ACTIVE.get(id);return p!=null&&!p.closed&&p.c.getLevel()==l&&p.c.getTick()<p.deadline?p:null;}
    static void terminal(GameTestHelper c,LasiusNigerEntity q){var p=get(c.getLevel(),q.getUUID());if(p!=null&&p.c==c)p.emit("acceptance_before_success",p.snapshot());}
    private BroodPile pile(){var plan=q.founding().plan();return plan!=null&&c.getLevel().getBlockEntity(plan.nursery()) instanceof BroodPile p?p:null;}
    private void emit(String type,Object value){var p=pile();var row=new LinkedHashMap<String,Object>();row.put("identity",identity);row.put("queen",q.getUUID());row.put("helperTick",c.getTick());row.put("nurseryLoaded",p==null?null:p.loadedTicks());row.put("type",type);row.put("value",value);PrimeAnts.LOGGER.info("T03 FOOD SERVICE {}",JSON.toJson(row));}
    private static Map<String,Long> nutrition(Nutrition n){var m=new TreeMap<String,Long>();m.put("sugar",n.sugar());m.put("protein",n.protein());m.put("gainedSugar_receipt",n.gainedSugar());m.put("gainedProtein_receipt",n.gainedProtein());m.put("spentSugar_receipt",n.spentSugar());m.put("spentProtein_receipt",n.spentProtein());m.put("receivedSugar_transfer",n.receivedSugar());m.put("givenSugar_transfer",n.givenSugar());return m;}
    private List<LasiusNigerEntity> workers(){var l=c.getLevel();return ColonyMembers.get(l).members(q.getUUID()).stream().filter(m->!m.dead()).map(m->l.getEntity(m.worker())).filter(a->a instanceof LasiusNigerEntity).map(a->(LasiusNigerEntity)a).filter(a->a.isAlive()&&!a.isRemoved()).toList();}
    private Map<String,Object> worker(LasiusNigerEntity w){
        var t=w.workerTasks();var l=c.getLevel();var plan=q.founding().plan();var m=new LinkedHashMap<String,Object>();
        m.put("worker",w.getUUID());m.put("phase",t.phase());m.put("reason",t.reason());m.put("source",t.sourceId());m.put("recipient",t.recipientId());m.put("cargo",w.getMainHandItem().toString());m.put("cargoSugar",Nutrition.sugarYield(w.getMainHandItem())*w.getMainHandItem().getCount());m.put("cargoProtein",Nutrition.proteinYield(w.getMainHandItem())*w.getMainHandItem().getCount());m.put("nutrition",nutrition(w.nutrition()));m.put("fasting",w.adultLife().fasting());m.put("mealTicks",w.adultLife().mealTicks());m.put("feedingTicks",t.feedingTicks());m.put("flowerInspections",t.flowerInspections());m.put("flowerSource",t.flowerSource());m.put("foragerClaim",q.founding().claimedBy(w));m.put("sharingTarget",t.sharing().target());m.put("sharingBusy",t.sharing().busy());m.put("sharingAction",t.sharing().actionTicks());m.put("callow",w.isCallow());m.put("onGround",w.onGround());m.put("position",plan==null?w.position().toString():w.position().subtract(Vec3.atBottomCenterOf(plan.entrance())).toString());
        var path=w.getNavigation().getPath();m.put("path",path==null?null:Map.of("canReach",path.canReach(),"done",path.isDone(),"nextIndex",path.getNextNodeIndex(),"nodes",path.getNodeCount(),"target",path.getTarget().toString()));
        if(plan!=null){m.put("cacheReach",WorkerTasks.reaches(l,w,Vec3.atBottomCenterOf(plan.cache()).add(0,.15,0)));m.put("broodReach",WorkerTasks.reaches(l,w,Vec3.atBottomCenterOf(plan.nursery()).add(0,.15,0)));m.put("queenReach",WorkerTasks.reaches(l,w,q.position().add(0,.25,0)));}
        return m;
    }
    private Map<String,Object> snapshot(){
        var l=c.getLevel();var p=pile();var plan=q.founding().plan();var m=new LinkedHashMap<String,Object>();var workers=workers();
        m.put("queenCanonical",l.getEntity(q.getUUID())==q);m.put("queenAlive",q.isAlive());m.put("queenRemoved",q.isRemoved());m.put("queenPosition",q.position().toString());m.put("queenNutrition",nutrition(q.nutrition()));m.put("queenFasting",q.adultLife().fasting());m.put("preDecision",gate);m.put("refusalCalls_not_units",new TreeMap<>(refusals));
        var ground=l.getEntitiesOfClass(ItemEntity.class,c.getBounds().inflate(8),i->i.isAlive()&&!i.isRemoved());
        m.put("ground",ground.stream().filter(i->WorkerTasks.food(i.getItem())||MaterialStore.material(i.getItem())).map(i->Map.of("id",i.getUUID(),"item",i.getItem().toString(),"position",plan==null?i.position().toString():i.position().subtract(Vec3.atBottomCenterOf(plan.entrance())).toString(),"pickupDelay",i.hasPickUpDelay(),"withinSearch",plan!=null&&Math.abs(i.blockPosition().getX()-plan.outside().getX())<=WorkerTasks.SEARCH_RADIUS&&Math.abs(i.blockPosition().getZ()-plan.outside().getZ())<=WorkerTasks.SEARCH_RADIUS&&Math.abs(i.blockPosition().getY()-plan.outside().getY())<=3,"walkable",NestPlan.walkable(l,i.blockPosition()))).toList());
        m.put("custody",TransferCustody.get(l).contents().stream().filter(t->c.getBounds().inflate(8).contains(t.position())).map(t->Map.of("id",t.id(),"item",t.stack().toString(),"position",t.position().toString())).toList());
        long cacheS=0,cacheP=0,broodS=0,broodP=0,commitS=0,commitP=0;
        if(plan!=null){
            m.put("habitat",plan.nurseryProblem(l,q.getUUID(),p!=null&&p.operational()));
            if(l.getBlockEntity(plan.cache()) instanceof NestCache cache){m.put("cache",Map.of("capacity",cache.capacity(),"contents",TierThreeEffectsProbe.contents(cache.contents()),"owner",String.valueOf(cache.componentOwner())));for(var s:cache.contents()){cacheS+=Nutrition.sugarYield(s)*s.getCount();cacheP+=Nutrition.proteinYield(s)*s.getCount();}}
            var store=MaterialStore.owned(l,q.getUUID(),plan);if(store!=null)m.put("materials",Map.of("capacity",store.capacity(),"contents",TierThreeEffectsProbe.contents(store.contents())));
        }
        if(p!=null){
            m.put("supply",p.supply(l));m.put("growthReason",p.growth().reason());m.put("recentSugar",p.growth().recentSugar());m.put("recentProtein",p.growth().recentProtein());m.put("observedIncomeTicks",p.growth().observedTicks());m.put("slots",p.nursery().slots());m.put("condition",p.condition());
            m.put("brood",p.records().stream().map(r->Map.of("id",r.id(),"slot",r.slot(),"stage",r.stage(),"progress",r.progress(),"founding",r.founding(),"nutrition",nutrition(r.nutrition()),"neglect",r.neglectTicks())).toList());
            for(var r:p.records()){broodS+=r.nutrition().sugar();broodP+=r.nutrition().protein();if(!r.founding()&&r.stage()!=BroodStage.COCOON){commitS+=Math.max(0,Nutrition.LARVA_SUGAR-r.nutrition().spentSugar());commitP+=Math.max(0,Nutrition.LARVA_PROTEIN-r.nutrition().spentProtein());}}
        }
        long adultS=q.nutrition().sugar()+workers.stream().mapToLong(w->w.nutrition().sugar()).sum(),adultP=q.nutrition().protein()+workers.stream().mapToLong(w->w.nutrition().protein()).sum();
        long cargoS=workers.stream().mapToLong(w->Nutrition.sugarYield(w.getMainHandItem())*w.getMainHandItem().getCount()).sum(),cargoP=workers.stream().mapToLong(w->Nutrition.proteinYield(w.getMainHandItem())*w.getMainHandItem().getCount()).sum();
        m.put("currentFundingDecomposition",Map.of("cacheSugar",cacheS,"cacheProtein",cacheP,"adultIngestedSugar",adultS,"adultIngestedProtein",adultP,"broodIngestedSugar",broodS,"broodIngestedProtein",broodP,"workerCargoSugar",cargoS,"workerCargoProtein",cargoP,"committedSugar_obligation",commitS,"committedProtein_obligation",commitP));
        m.put("workers",workers.stream().map(this::worker).toList());m.put("foragerClaims",q.founding().foragers());return m;
    }
    public static void beforeLay(ServerLevel l,BroodPile pile,LasiusNigerEntity queen,boolean care,boolean originalTerminal){
        var p=get(l,pile.queenId());if(p==null)return;var s=pile.supply(l);var g=pile.growth();
        long maintenance=((s.adults()+s.brood()+1L)*FoodLimitedGrowth.WINDOW*5+4L*AdultLife.maintenancePeriod()-1)/(4L*AdultLife.maintenancePeriod());
        var m=new LinkedHashMap<String,Object>();m.put("loaded",pile.loadedTicks());m.put("care",care);m.put("originalTerminal",originalTerminal);m.put("operational",pile.operational());m.put("cadence",pile.layingInterval(l));m.put("lastLay",pile.lastLayingTick());m.put("slots",pile.nursery().slots());m.put("brood",pile.records().size());m.put("occupied",ColonyMembers.get(l).occupied(pile.queenId()));m.put("populationCap",pile.stageEvaluation()==null?null:pile.stageEvaluation().cap());m.put("supply",s);m.put("requiredSugar",s.committedSugar()+Nutrition.EGG_SUGAR+Nutrition.LARVA_SUGAR+maintenance);m.put("requiredProtein",s.committedProtein()+Nutrition.EGG_PROTEIN+Nutrition.LARVA_PROTEIN);m.put("maintenance",maintenance);m.put("recentSugar",g.recentSugar());m.put("recentProtein",g.recentProtein());m.put("observedIncomeTicks",g.observedTicks());m.put("queenSugar",queen==null?null:queen.nutrition().sugar());m.put("queenProtein",queen==null?null:queen.nutrition().protein());p.gate=m;
        String sig=care+":"+originalTerminal+":"+pile.operational()+":"+pile.layingInterval(l)+":"+(pile.records().size()<pile.nursery().slots())+":"+s.complete()+":"+s.hungry()+":"+(g.observedTicks()>=FoodLimitedGrowth.WARMUP)+":"+(g.recentSugar()>=maintenance)+":"+(s.storedSugar()>=(long)m.get("requiredSugar"))+":"+(s.storedProtein()>=(long)m.get("requiredProtein"))+":"+(queen!=null&&queen.nutrition().sugar()>=Nutrition.EGG_SUGAR)+":"+(queen!=null&&queen.nutrition().protein()>=Nutrition.EGG_PROTEIN);
        if(!sig.equals(p.gateSignature)){p.gateSignature=sig;p.emit("gate_transition",p.snapshot());}
        if(p.c.getTick()==p.deadline-1)p.emit("last_bounded_pre_decision",p.snapshot());
    }
    public static void afterLay(ServerLevel l,BroodPile pile,boolean laid){var p=get(l,pile.queenId());if(p!=null&&laid)p.emit("actual_lay",p.snapshot());}
    public static void workerTick(ServerLevel l,LasiusNigerEntity w){
        var p=get(l,w.queenId());if(p==null)return;var t=w.workerTasks();String sig=t.phase()+":"+t.reason()+":"+w.getMainHandItem()+":"+t.sourceId()+":"+t.recipientId()+":"+t.sharing().target()+":"+t.sharing().busy()+":"+w.nutrition().consumedUnits();
        if(!sig.equals(p.workerSignatures.put(w.getUUID(),sig)))p.emit("worker_transition",p.worker(w));
    }
    public static void search(ServerLevel l,LasiusNigerEntity w,boolean before){var p=get(l,w.queenId());if(p==null||w.tickCount%20!=0)return;var t=w.workerTasks();if(before)p.searches.put(w.getUUID(),t.flowerInspections());else{var n=p.searches.remove(w.getUUID());if(n!=null)p.emit("search_pulse",Map.of("worker",w.getUUID(),"inspections",t.flowerInspections()-n,"phase",t.phase(),"reason",t.reason(),"source",String.valueOf(t.sourceId())));}}
    public static void admission(ServerLevel l,LasiusNigerEntity w,String method,boolean allowed){var p=get(l,w.queenId());if(p!=null)p.refusals.merge(method+"="+allowed+":"+w.getMainHandItem(),1L,Long::sum);}
    private void finish(String why){if(closed)return;emit("endpoint",Map.of("scope",why,"state",snapshot()));closed=true;ACTIVE.remove(q.getUUID(),this);emit("cleanup",Map.of("removed",!ACTIVE.containsKey(q.getUUID()),"remainingScopedObservers",ACTIVE.size()));}
}
