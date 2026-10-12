package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.brood.*;
import dev.primeants.entity.*;
import dev.primeants.worker.*;
import java.util.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;

/** Compact read-only event samples. Worker correspondence is the fixture's initial registry order. */
public final class GrowthObservations {
    private static final Map<UUID,GrowthObservations> WATCH=new HashMap<>();
    private int aggregateLayingDecisions;
    private final GameTestHelper c;private final LasiusNigerEntity q;private final String run;
    private final List<UUID> originals;private final Map<UUID,Integer> broodOrder=new LinkedHashMap<>();
    private JsonObject previous;private String signature="";private final long baseline;
    private GrowthObservations(GameTestHelper c,LasiusNigerEntity q,String run){this.c=c;this.q=q;this.run=run;var workers=new ArrayList<>(AutomaticCatchupGameTest.adults(c,q));workers.remove(q);workers.sort(Comparator.comparing(a->!a.adultLife().hungry()));var matched=new ArrayList<UUID>();matched.add(q.getUUID());workers.forEach(a->matched.add(a.getUUID()));originals=List.copyOf(matched);baseline=pile().loadedTicks();WATCH.put(q.getUUID(),this);sample(c.getLevel().getGameTime(),"baseline");}
    static void start(GameTestHelper c,LasiusNigerEntity q,String run){new GrowthObservations(c,q,run);}
    private BroodPile pile(){return (BroodPile)c.getLevel().getBlockEntity(q.founding().plan().nursery());}
    public static void sample(ServerLevel l,BroodPile p,long now,String point){var x=WATCH.get(p.queenId());if(x!=null&&x.c.getLevel()==l)x.sample(now,point);}
    static void sample(GameTestHelper c,LasiusNigerEntity q,long now,String point){sample(c.getLevel(),(BroodPile)c.getLevel().getBlockEntity(q.founding().plan().nursery()),now,point);}
    public static void stop(LasiusNigerEntity q){WATCH.remove(q.getUUID());}
    static JsonObject snapshot(LasiusNigerEntity q){return WATCH.get(q.getUUID()).state();}
    static int aggregateLayingDecisions(LasiusNigerEntity q){return WATCH.get(q.getUUID()).aggregateLayingDecisions;}
    public static void layingDecision(ServerLevel l,BroodPile p,long delay){var observer=WATCH.get(p.queenId());if(observer==null||observer.c.getLevel()!=l||!p.catchup().active())return;observer.aggregateLayingDecisions++;var row=new JsonObject();row.addProperty("run",observer.run);row.addProperty("loaded",p.loadedTicks());row.addProperty("aggregate_active",true);row.addProperty("event_budget_available",p.catchup().eventAvailable());row.addProperty("returned_delay",delay);row.add("supply",new Gson().toJsonTree(p.supply(l)));ReconciliationEvidence.log("t07_aggregate_laying_decision","production_hook",row);}
    private JsonObject state(){
        var p=pile();var row=new JsonObject();row.addProperty("elapsed",p.loadedTicks()-baseline);row.addProperty("loaded",p.loadedTicks());row.addProperty("last_egg",p.lastLayingTick());row.addProperty("condition",p.condition());row.addProperty("pending_ticks",p.catchup().pendingTicks());
        row.addProperty("care",p.aggregateCare(c.getLevel()));row.add("supply",new Gson().toJsonTree(p.supply(c.getLevel())));row.add("service_intentions",new Gson().toJsonTree(p.catchup().serviceObservations()));
        row.addProperty("queen_next_egg_affordable",q.nutrition().sugar()>=Nutrition.EGG_SUGAR&&q.nutrition().protein()>=Nutrition.EGG_PROTEIN);
        var people=new JsonArray();for(int i=0;i<originals.size();i++){var a=(LasiusNigerEntity)c.getLevel().getEntity(originals.get(i));var n=new JsonObject();n.addProperty("matched",i==0?"queen":"worker"+i);n.addProperty("id",originals.get(i).toString());if(a!=null){n.add("nutrition",new Gson().toJsonTree(a.nutrition().snapshot()));n.addProperty("hungry",a.adultLife().hungry());n.addProperty("starving",a.adultLife().fasting()>=a.adultLife().grace());n.addProperty("fasting",a.adultLife().fasting());n.addProperty("cargo",a.getMainHandItem().toString());n.addProperty("recipient",String.valueOf(a.workerTasks().recipientId()));n.addProperty("service_ticks",a.workerTasks().feedingTicks());n.addProperty("meal_ticks",a.adultLife().mealTicks());n.addProperty("phase",a.workerTasks().phase().name());n.addProperty("maintenance",a.adultLife().maintenanceTicks());}people.add(n);}row.add("original_adults",people);
        var brood=new JsonArray();for(var r:p.records()){broodOrder.computeIfAbsent(r.id(),key->broodOrder.size());var b=new JsonObject();b.addProperty("order",broodOrder.get(r.id()));b.addProperty("id",r.id().toString());b.addProperty("stage",r.stage().name());b.addProperty("progress",r.progress());b.add("nutrition",new Gson().toJsonTree(r.nutrition().snapshot()));brood.add(b);}row.add("brood",brood);
        row.addProperty("virtual_births",p.consumed().size()-p.original().size());row.add("cache",new Gson().toJsonTree(TierThreeEffectsProbe.contents(((NestCache)c.getLevel().getBlockEntity(q.founding().plan().cache())).contents())));return row;
    }
    private static String signature(JsonObject row){
        var out=new JsonObject();out.add("last_egg",row.get("last_egg"));out.add("queen_next_egg_affordable",row.get("queen_next_egg_affordable"));out.add("cache",row.get("cache"));out.add("virtual_births",row.get("virtual_births"));out.add("service_intentions",row.get("service_intentions"));
        var people=new JsonArray();for(var v:row.getAsJsonArray("original_adults")){var a=v.getAsJsonObject();var n=a.getAsJsonObject("nutrition");people.add(a.get("matched")+":"+n.get("apples")+":"+n.get("chickens")+":"+n.get("receivedSugar")+":"+n.get("givenSugar")+":"+a.get("hungry")+":"+a.get("starving")+":"+a.get("cargo")+":"+a.get("recipient"));}out.add("people",people);
        var brood=new JsonArray();for(var v:row.getAsJsonArray("brood")){var b=v.getAsJsonObject();var n=b.getAsJsonObject("nutrition");brood.add(b.get("order")+":"+b.get("stage")+":"+n.get("apples")+":"+n.get("chickens"));}out.add("brood",brood);return out.toString();
    }
    private void sample(long now,String point){var row=state();String next=signature(row);if(!next.equals(signature)||point.equals("final")){row.addProperty("run",run);row.addProperty("virtual_time",now);row.addProperty("point",point);if(previous!=null)row.addProperty("since_previous_loaded",row.get("loaded").getAsLong()-previous.get("loaded").getAsLong());ReconciliationEvidence.log("t07_growth","ordinary_and_aggregate_events",row);signature=next;}previous=row;}
}
