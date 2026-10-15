package dev.primeants.worker;

import dev.primeants.brood.*;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Fixed distribution at confirmed biological boundaries. No trips, intentions or controller clocks.
 * Only existing food in the owned cache (including upgraded food storage) and reachable member hands
 * can become ingestion receipts. Existing crop moves with balanced given/received sugar receipts. */
public final class AggregateFeeding {
    private static final Comparator<LasiusNigerEntity> ID=Comparator.comparing(a->a.getUUID().toString());
    private static final Comparator<LasiusNigerEntity> HUNGER=Comparator
        .comparingLong((LasiusNigerEntity a)->a.adultLife().fasting()).reversed().thenComparing(ID);

    /** CLEAR findings prove the original connected chamber and entrance steps. Outside that domain
     * only adjoining loaded supported entrance stands are admitted. This availability check has
     * no path, distance cost, future location or travel estimate. */
    private static boolean reachable(ServerLevel l,NestPlan home,LasiusNigerEntity a){
        var stands=new ArrayList<BlockPos>();
        for(int f=3;f<=5;f++)for(int s=-1;s<=1;s++)stands.add(home.at(f,s,-2));
        for(int f=0;f<3;f++)stands.add(home.at(f,0,-f));
        if(NestPlan.loaded(l,home.entrance().above())&&NestPlan.walkable(l,home.entrance().above())){
            stands.add(home.entrance().above());
            for(int f=-1;f>=-2;f--){var p=home.at(f,0,1);if(!NestPlan.loaded(l,p)||!NestPlan.walkable(l,p))break;stands.add(p);}
        }
        stands.addAll(NestExpansion.get(l).circulationSpace(l,a.colonyIdentity()));
        return stands.stream().anyMatch(p->WorkerTasks.reaches(l,a,Vec3.atBottomCenterOf(p).add(0,.25,0)));
    }
    private static boolean eligible(ServerLevel l,BroodPile pile,LasiusNigerEntity a){
        return l.getEntity(a.getUUID())==a&&a.isAlive()&&!a.isRemoved()&&!a.isNoAi()&&!a.isCallow()
            &&!a.workerTasks().defending()&&pile.queenId().equals(a.colonyIdentity())
            &&NestPlan.loaded(l,a.blockPosition())&&l.isPositionEntityTicking(a.blockPosition())
            &&(a.form()==AntForm.QUEEN||ColonyMembers.get(l).belongs(a,pile.queenId(),pile.catchupPlan().chamber()))
            &&reachable(l,pile.catchupPlan(),a);
    }
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
        var bodies=missing.stream().filter(a->eligible(l,pile,a)).toList();
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
        // Real confirmed care is required. Ordinary ordering gives accepting larvae priority
        // over the queen, including protein that her ingested stores cannot export.
        if(bodies.stream().noneMatch(a->a.workerTasks().caregiverWithValidatedHabitat(l,pile.catchupPlan())))return true;
        var queen=bodies.stream().filter(a->a.form()==AntForm.QUEEN).findFirst().orElse(null);
        for(var source:available){
            var id=WorkerTasks.selectNursingRecipient(pile,queen,source.unit,ignored->true);
            if(id==null)continue;
            var larva=pile.records().stream().filter(r->r.id().equals(id)).findFirst().orElse(null);
            if(!ingest(pile,source,larva==null?queen.nutrition():larva.nutrition(),larva==null?Nutrition.QUEEN_SUGAR_CAPACITY:Nutrition.LARVA_SUGAR,larva==null?Nutrition.QUEEN_PROTEIN_CAPACITY:Nutrition.LARVA_PROTEIN))return false;
        }
        return true;
    }
}
