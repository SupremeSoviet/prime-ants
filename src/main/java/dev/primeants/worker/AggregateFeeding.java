package dev.primeants.worker;

import dev.primeants.brood.*;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Fixed distribution at confirmed biological boundaries. No trips, intentions or controller clocks.
 * Only existing food in the owned cache (including upgraded food storage) and confirmed home member hands
 * can become ingestion receipts. Existing crop moves with balanced given/received sugar receipts. */
public final class AggregateFeeding {
    private static final Comparator<LasiusNigerEntity> ID=Comparator.comparing(a->a.getUUID().toString());
    private static final Comparator<LasiusNigerEntity> HUNGER=Comparator
        .comparingLong((LasiusNigerEntity a)->a.adultLife().fasting()).reversed().thenComparing(ID);

    private record Source(NestCache cache,LasiusNigerEntity carrier,ItemStack unit){
        boolean serve(Nutrition n,long sugarCap,long proteinCap){
            if(cache!=null)return cache.serveAggregate(n,sugarCap,proteinCap,s->ItemStack.isSameItemSameComponents(s,unit));
            var hand=carrier.getMainHandItem();
            if(hand.isEmpty()||!ItemStack.isSameItemSameComponents(hand,unit)||!n.ingest(unit,sugarCap,proteinCap))return false;
            var remainder=hand.copy();remainder.shrink(1);carrier.setItemSlot(EquipmentSlot.MAINHAND,remainder);return true;
        }
    }
    private static List<Source> sources(NestCache cache,List<LasiusNigerEntity> bodies){
        var result=new ArrayList<Source>();
        if(cache!=null)for(var unit:cache.contents())result.add(new Source(cache,null,unit));
        for(var a:bodies.stream().sorted(ID).toList())if(WorkerTasks.food(a.getMainHandItem()))
            for(int i=0;i<a.getMainHandItem().getCount();i++)result.add(new Source(null,a,a.getMainHandItem().copyWithCount(1)));
        return result;
    }
    private static boolean ingest(BroodPile pile,Source source,Nutrition n,long s,long p){
        if(!pile.catchup().foodAvailable())return false;
        if(!source.serve(n,s,p))throw new IllegalStateException("Confirmed aggregate food changed during distribution");
        pile.catchup().foodEvent();pile.setChanged();if(source.cache!=null)pile.aggregateFoodChanged();return true;
    }
    /** Only missing adults participate. Already observed ordinary intervals earn no extra feeding.
     * False means that bounded pending work must resume on another invocation. */
    public boolean distribute(ServerLevel l,BroodPile pile,List<LasiusNigerEntity> missing,NestCache cache){
        var bodies=missing.stream().filter(a->AggregateHomeDomain.recipient(l,pile,a)).toList();
        if(cache!=null&&(!cache.ownedBy(pile.queenId(),pile.catchupPlan())||cache.componentOwner()==null))cache=null;
        var available=sources(cache,bodies);
        // Only crop present at this boundary is redistributable in this decision batch.
        var crop=new HashMap<LasiusNigerEntity,Long>();
        for(var a:bodies)crop.put(a,Math.max(0,a.nutrition().sugar()-CropSharing.RESERVE));
        for(var recipient:bodies.stream().filter(a->a.adultLife().hungry()&&a.nutrition().sugar()<CropSharing.PORTION).sorted(HUNGER).toList()){
            // Ordinary reserve and portion are values, not a saved approach or cooldown.
            for(var donor:bodies.stream().filter(a->a.form()==AntForm.WORKER&&a!=recipient).sorted(ID).toList()){
                if(recipient.nutrition().sugar()>=CropSharing.PORTION)break;
                long amount=Math.min(CropSharing.PORTION,Math.min(crop.get(donor),Math.min(donor.nutrition().sugar()-CropSharing.RESERVE,Nutrition.QUEEN_SUGAR_CAPACITY-recipient.nutrition().sugar())));
                if(amount<=0)continue;
                if(!pile.catchup().foodAvailable())return false;
                if(!donor.nutrition().shareWith(recipient.nutrition(),amount,CropSharing.RESERVE,Nutrition.QUEEN_SUGAR_CAPACITY))throw new IllegalStateException("Unbalanced aggregate crop transfer");
                crop.put(donor,crop.get(donor)-amount);
                pile.catchup().foodEvent();pile.setChanged();
            }
            if(recipient.nutrition().sugar()>=CropSharing.PORTION)continue;
            var source=available.stream().filter(s->Nutrition.sugarYield(s.unit)>0&&recipient.nutrition().accepts(s.unit,Nutrition.QUEEN_SUGAR_CAPACITY,0)).findFirst().orElse(null);
            if(source!=null){if(!ingest(pile,source,recipient.nutrition(),Nutrition.QUEEN_SUGAR_CAPACITY,0))return false;available.remove(source);}
        }
        // Recipient status never grants caregiver authority. Each ordinary authorized nurse
        // spends only its own earned capacity; one physical nursing receipt per interval.
        var caregivers=bodies.stream().filter(a->a.workerTasks().caregiverWithValidatedHabitat(l,pile.catchupPlan())).sorted(ID).toList();
        var queen=bodies.stream().filter(a->a.form()==AntForm.QUEEN).findFirst().orElse(null);
        for(var caregiver:caregivers){
            if(!pile.catchup().throughput().available(caregiver.getUUID()))continue;
            for(var source:new ArrayList<>(available)){
                var id=WorkerTasks.selectNursingRecipient(pile,queen,source.unit,ignored->true);
                if(id==null)continue;
                var larva=pile.records().stream().filter(r->r.id().equals(id)).findFirst().orElse(null);
                if(!ingest(pile,source,larva==null?queen.nutrition():larva.nutrition(),larva==null?Nutrition.QUEEN_SUGAR_CAPACITY:Nutrition.LARVA_SUGAR,larva==null?Nutrition.QUEEN_PROTEIN_CAPACITY:Nutrition.LARVA_PROTEIN))return false;
                pile.catchup().throughput().complete(caregiver.getUUID());available.remove(source);break;
            }
        }
        return true;
    }
}
