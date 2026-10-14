package dev.primeants.worker;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import dev.primeants.brood.*;
import dev.primeants.colony.AdultBound;
import dev.primeants.entity.*;
import dev.primeants.founding.NestPlan;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Nonbinding bounded service intentions. Cache units stay in their canonical owner until completion.
 * No intention reserves a unit or is a receipt. Physical nurse cargo stays in its saved equipment.
 * The loaded, validated chamber rows approximate travel; this never moves an actor or replays a path. */
public final class AggregateFeeding {
    private record Service(UUID actor,UUID recipient,String source,String kind,long start,long due,long returnTicks){
        String encode(){var o=new JsonObject();o.addProperty("actor",actor.toString());o.addProperty("recipient",recipient.toString());o.addProperty("source",source);o.addProperty("kind",kind);o.addProperty("start",start);o.addProperty("due",due);o.addProperty("return",returnTicks);return o.toString();}
        static Service decode(String text){var o=JsonParser.parseString(text).getAsJsonObject();var s=new Service(UUID.fromString(o.get("actor").getAsString()),UUID.fromString(o.get("recipient").getAsString()),o.get("source").getAsString(),o.get("kind").getAsString(),o.get("start").getAsLong(),o.get("due").getAsLong(),o.get("return").getAsLong());if(!Set.of("cache","cargo","crop").contains(s.source)||s.kind.isEmpty()||s.start<0||s.due<s.start||s.returnTicks<0||s.returnTicks>CropSharing.APPROACH_BOUND+WorkerTasks.FEEDING_TICKS)throw new IllegalArgumentException("Invalid saved service intention");return s;}
    }
    private final List<Service> services=new ArrayList<>();
    private final Map<UUID,Long> ready=new LinkedHashMap<>();
    // A completed abstract return is a location for service accounting, never an actor position.
    // Its completion timestamp lets ordinary actor observations revoke this approximation.
    private final Map<UUID,Long> cacheReturned=new LinkedHashMap<>();
    private long cropWake=Long.MAX_VALUE;
    public int pending(){return services.size();}
    public List<String> observations(){return services.stream().map(Service::encode).toList();}
    public long nextCompletion(){return services.stream().mapToLong(Service::due).min().orElse(Long.MAX_VALUE);}
    private static String kind(ItemStack s){return BuiltInRegistries.ITEM.getKey(s.getItem()).toString();}
    private boolean busy(UUID actor){return services.stream().anyMatch(s->s.actor.equals(actor));}
    private boolean receiving(UUID recipient){return services.stream().anyMatch(s->s.recipient.equals(recipient)&&s.source.equals("crop"));}
    private static long route(LasiusNigerEntity worker,Vec3 a,Vec3 b,int legs){
        double distance=Math.abs(a.x-b.x)+Math.abs(a.y-b.y)+Math.abs(a.z-b.z);
        double speed=worker.getAttributeValue(Attributes.MOVEMENT_SPEED);
        if(!Double.isFinite(speed)||speed<=0)return CropSharing.APPROACH_BOUND;
        // A current path may wait one refresh interval. A completed leg starts the next immediately
        // through WorkerTasks' isDone branch; it does not pay another refresh interval per row.
        return Math.min(CropSharing.APPROACH_BOUND,(long)Math.ceil(distance/speed)+(legs>0?WorkerTasks.NAVIGATION_INTERVAL:0));
    }
    private void add(LasiusNigerEntity actor,UUID recipient,String source,String kind,long cursor,long delay,long back){
        services.add(new Service(actor.getUUID(),recipient,source,kind,cursor,AdultLife.saturatingAdd(cursor,Math.max(1,delay)-1),back));
    }
    private long queued(UUID recipient,boolean protein){
        return services.stream().filter(s->s.recipient.equals(recipient)&&!s.source.equals("crop"))
            .mapToLong(s->protein?protein(s.kind):sugar(s.kind)).sum();
    }
    private static long sugar(String kind){return Nutrition.sugarYield(new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(kind))));}
    private static long protein(String kind){return Nutrition.proteinYield(new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(kind))));}
    private UUID recipient(ServerLevel l,BroodPile pile,LasiusNigerEntity queen,ItemStack unit){
        return WorkerTasks.selectNursingRecipient(pile,queen,unit,id->{
            if(id.equals(pile.queenId()))return queen.nutrition().sugar()+queued(id,false)+Nutrition.sugarYield(unit)<=Nutrition.QUEEN_SUGAR_CAPACITY
                &&queen.nutrition().protein()+queued(id,true)+Nutrition.proteinYield(unit)<=Nutrition.QUEEN_PROTEIN_CAPACITY
                &&(Nutrition.proteinYield(unit)==0||queen.nutrition().protein()+queued(id,true)<Nutrition.EGG_PROTEIN);
            var r=pile.records().stream().filter(b->b.id().equals(id)).findFirst().orElseThrow();
            return r.nutrition().gainedSugar()+queued(id,false)+Nutrition.sugarYield(unit)<=Nutrition.LARVA_SUGAR
                &&r.nutrition().gainedProtein()+queued(id,true)+Nutrition.proteinYield(unit)<=Nutrition.LARVA_PROTEIN;
        });
    }
    public void plan(ServerLevel l,BroodPile pile,List<LasiusNigerEntity> adults,NestCache cache,long cursor){
        // A live ordinary interval can service or revoke old intentions. It never earns a retrospective receipt.
        services.removeIf(s->adults.stream().noneMatch(a->a.getUUID().equals(s.actor))||adults.stream().filter(a->a.getUUID().equals(s.actor)).anyMatch(a->a.workerTasks().sharing().lastLoadedTick()>=s.start));
        ready.keySet().removeIf(id->adults.stream().noneMatch(a->a.getUUID().equals(id)));
        cacheReturned.entrySet().removeIf(e->{var actor=adults.stream().filter(a->a.getUUID().equals(e.getKey())).findFirst().orElse(null);boolean revoked=actor==null||actor.workerTasks().sharing().lastLoadedTick()>e.getValue();if(revoked)ready.remove(e.getKey());return revoked;});
        var home=pile.catchupPlan();var queen=l.getEntity(pile.queenId()) instanceof LasiusNigerEntity q?q:null;
        // Ordinary crop sharing precedes personal cache eating. Its transfer uses the shared exact reserve/portion.
        cropWake=Long.MAX_VALUE;
        for(var donor:adults){
            if(!CropSharing.aggregateDonorEligible(l,donor)||receiving(donor.getUUID()))continue;
            int cooldown=donor.workerTasks().sharing().cooldownRemaining();
            if(cooldown>0){cropWake=Math.min(cropWake,AdultLife.saturatingAdd(cursor,cooldown));continue;}
            if(busy(donor.getUUID())||ready.getOrDefault(donor.getUUID(),0L)>cursor)continue;
            var target=adults.stream().filter(a->!receiving(a.getUUID())&&!busy(a.getUUID())&&CropSharing.aggregateEligible(l,donor,a))
                .min(CropSharing.recipientOrder(donor)).orElse(null);
            if(target!=null){boolean near=WorkerTasks.reaches(l,donor,target.position().add(0,.25,0))&&WorkerTasks.reaches(l,target,donor.position().add(0,.25,0));int partial=target.getUUID().equals(donor.workerTasks().sharing().target())&&near?donor.workerTasks().sharing().actionTicks():0;
                long approach=near?0:route(donor,donor.position(),target.position(),1);add(donor,target.getUUID(),"crop","crop",cursor,CropSharing.ACTION_TICKS-partial+approach,40);}
        }
        if(cache==null||cache.size()==0&&adults.stream().noneMatch(a->WorkerTasks.food(a.getMainHandItem())))return;
        var available=new ArrayList<>(cache.contents());for(var s:services)if(s.source.equals("cache")){int at=-1;for(int i=0;i<available.size();i++)if(kind(available.get(i)).equals(s.kind)){at=i;break;}if(at>=0)available.remove(at);}
        var cacheStand=Vec3.atBottomCenterOf(home.at(4,-1,-2));
        // Physical cache access precedes ingestion. A distant actor's future intention must
        // not take the first portion ahead of an eligible body already at the cache.
        var cacheOrder=new ArrayList<>(adults);
        cacheOrder.sort(Comparator.comparingLong(a->WorkerTasks.food(a.getMainHandItem())||cacheReturned.containsKey(a.getUUID())||WorkerTasks.reaches(l,a,Vec3.atBottomCenterOf(home.cache()).add(0,.15,0))?0:route(a,a.position(),cacheStand,1)));
        for(var actor:cacheOrder){
            if(actor.form()!=AntForm.WORKER||actor.isNoAi()||actor.workerTasks().defending()||busy(actor.getUUID())||ready.getOrDefault(actor.getUUID(),0L)>cursor)continue;
            if(actor.workerTasks().mealNeeded(l)&&!receiving(actor.getUUID())){
                var carried=actor.getMainHandItem();var meal=WorkerTasks.food(carried)&&Nutrition.sugarYield(carried)>0?carried:available.stream().filter(s->Nutrition.sugarYield(s)>0&&actor.nutrition().accepts(s,Nutrition.QUEEN_SUGAR_CAPACITY,0)).findFirst().orElse(null);
                if(meal!=null&&actor.nutrition().accepts(meal,Nutrition.QUEEN_SUGAR_CAPACITY,0)){
                    boolean cargo=!carried.isEmpty();long approach=cargo||cacheReturned.containsKey(actor.getUUID())||WorkerTasks.reaches(l,actor,Vec3.atBottomCenterOf(home.cache()).add(0,.15,0))?0:route(actor,actor.position(),cacheStand,1);
                    add(actor,actor.getUUID(),cargo?"cargo":"cache",kind(meal),cursor,AdultLife.MEAL_TICKS-actor.adultLife().mealTicks()+approach,0);if(!cargo)available.remove(meal);continue;
                }
            }
            if(!actor.workerTasks().caregiverWithValidatedHabitat(l,home)||actor.workerTasks().mealNeeded(l)||receiving(actor.getUUID()))continue;
            var carried=actor.getMainHandItem();boolean cargo=WorkerTasks.food(carried);
            if(!cargo&&!carried.isEmpty())continue;
            ItemStack unit=null;UUID to=null;
            if(cargo){unit=carried;to=actor.workerTasks().recipientId();if(to==null)to=recipient(l,pile,queen,unit);}
            else for(var candidate:available){var id=recipient(l,pile,queen,candidate);if(id!=null){unit=candidate;to=id;break;}}
            if(unit==null||to==null)continue;
            boolean toQueen=to.equals(pile.queenId());Vec3 target=toQueen?queen.position().add(0,.25,0):Vec3.atBottomCenterOf(home.nursery()).add(0,.15,0);
            boolean near=WorkerTasks.reaches(l,actor,target);int partial=cargo&&near?actor.workerTasks().feedingTicks():0;
            long fetch=cargo||cacheReturned.containsKey(actor.getUUID())||WorkerTasks.reaches(l,actor,Vec3.atBottomCenterOf(home.cache()).add(0,.15,0))?0:route(actor,actor.position(),cacheStand,1);
            Vec3 destination=Vec3.atBottomCenterOf(home.at(toQueen?4:5,toQueen?-1:1,-2));
            long travel=cargo&&near?0:route(actor,cargo?actor.position():cacheStand,destination,toQueen?1:2);
            long back=WorkerTasks.FEEDING_COOLDOWN+route(actor,destination,cacheStand,toQueen?1:2);
            add(actor,to,cargo?"cargo":"cache",kind(unit),cursor,fetch+(cargo?0:1)+travel+WorkerTasks.FEEDING_TICKS-partial,back);if(!cargo)available.remove(unit);
        }
    }
    public long nextEvent(long cursor){long due=Math.min(cropWake,Math.min(nextCompletion(),ready.values().stream().filter(n->n>cursor).mapToLong(Long::longValue).min().orElse(Long.MAX_VALUE)));return due==Long.MAX_VALUE?Long.MAX_VALUE:due<=cursor?1:due-cursor;}
    /** Called only for the coordinator's confirmed missing adult segments, before biological debit.
     * Stop cooldown counting at the exact maintenance step that would remove donor eligibility. */
    public void advanceCooldown(ServerLevel l,List<LasiusNigerEntity> adults,long ticks){
        for(var a:adults)if(CropSharing.aggregateDonorEligible(l,a)&&!receiving(a.getUUID())){
            long units=a.nutrition().sugar()-CropSharing.RESERVE-1;
            long eligible=AdultLife.saturatingAdd(a.adultLife().coverageRemaining(),units>Long.MAX_VALUE/AdultLife.maintenancePeriod()?Long.MAX_VALUE:units*AdultLife.maintenancePeriod());
            a.workerTasks().sharing().advanceAggregateCooldown(Math.min(ticks,eligible));
        }
    }
    public void complete(ServerLevel l,BroodPile pile,NestCache cache,long now){
        for(var service:List.copyOf(services))if(service.due<=now){
            var entity=l.getEntity(service.actor);var recipient=l.getEntity(service.recipient);
            if(!(entity instanceof LasiusNigerEntity actor)||!actor.isAlive()||actor.isRemoved()||actor.isNoAi()||actor.workerTasks().defending()){services.remove(service);continue;}
            boolean served=false;
            if(service.source.equals("crop"))served=recipient instanceof LasiusNigerEntity target&&CropSharing.transferAggregate(l,actor,target);
            else{
                Nutrition nutrition=null;long sugarCap=Nutrition.QUEEN_SUGAR_CAPACITY,proteinCap=Nutrition.QUEEN_PROTEIN_CAPACITY;
                java.util.function.Predicate<ItemStack> accepts;
                if(service.recipient.equals(actor.getUUID())){nutrition=actor.nutrition();proteinCap=0;accepts=s->Nutrition.sugarYield(s)>0&&actor.workerTasks().mealNeeded(l);}
                else if(service.recipient.equals(pile.queenId())&&recipient instanceof LasiusNigerEntity queen){nutrition=queen.nutrition();accepts=s->WorkerTasks.queenNeedsFood(queen,s);}
                else{var brood=pile.records().stream().filter(r->r.id().equals(service.recipient)).findFirst().orElse(null);if(brood==null){services.remove(service);continue;}nutrition=brood.nutrition();sugarCap=Nutrition.LARVA_SUGAR;proteinCap=Nutrition.LARVA_PROTEIN;accepts=s->pile.accepts(brood,s);}
                if(service.recipient.equals(actor.getUUID())||actor.workerTasks().caregiverWithValidatedHabitat(l,pile.catchupPlan())){
                    if(service.source.equals("cargo")){var unit=actor.getMainHandItem();if(kind(unit).equals(service.kind)&&accepts.test(unit)&&nutrition.ingest(unit,sugarCap,proteinCap)){actor.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);served=true;}}
                    else if(cache!=null)served=cache.serveAggregate(nutrition,sugarCap,proteinCap,s->kind(s).equals(service.kind)&&accepts.test(s));
                }
            }
            if(served){pile.catchup().foodEvent();ready.put(service.actor,AdultLife.saturatingAdd(now,service.returnTicks));if(service.source.equals("cache")||!service.source.equals("crop")&&!service.recipient.equals(service.actor))cacheReturned.put(service.actor,now);else cacheReturned.remove(service.actor);if(service.source.equals("cache"))pile.aggregateFoodChanged();}
            services.remove(service);pile.setChanged();
        }
    }
    public void save(ValueOutput out){out.store("Services",Codec.STRING.listOf(),services.stream().map(Service::encode).toList());out.store("ServiceReady",Codec.unboundedMap(Codec.STRING,Codec.LONG),ready.entrySet().stream().collect(java.util.stream.Collectors.toMap(e->e.getKey().toString(),Map.Entry::getValue)));out.store("ServiceCacheReturned",Codec.unboundedMap(Codec.STRING,Codec.LONG),cacheReturned.entrySet().stream().collect(java.util.stream.Collectors.toMap(e->e.getKey().toString(),Map.Entry::getValue)));}
    public void load(ValueInput in){services.clear();ready.clear();cacheReturned.clear();services.addAll(in.read("Services",Codec.STRING.listOf()).orElse(List.of()).stream().map(Service::decode).toList());in.read("ServiceReady",Codec.unboundedMap(Codec.STRING,Codec.LONG)).orElse(Map.of()).forEach((id,time)->ready.put(UUID.fromString(id),time));in.read("ServiceCacheReturned",Codec.unboundedMap(Codec.STRING,Codec.LONG)).orElse(Map.of()).forEach((id,time)->cacheReturned.put(UUID.fromString(id),time));if(services.size()>AdultBound.MAX||ready.size()>AdultBound.MAX||services.stream().map(Service::actor).distinct().count()!=services.size()||ready.values().stream().anyMatch(n->n<0)||!ready.keySet().containsAll(cacheReturned.keySet())||cacheReturned.values().stream().anyMatch(n->n<0))throw new IllegalArgumentException("Invalid bounded aggregate service state");}
}
