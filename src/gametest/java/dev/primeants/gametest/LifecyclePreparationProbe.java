package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.PrimeAnts;
import dev.primeants.brood.BroodPile;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.NestPlan;
import java.util.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;

/** Optional read-only guard/binding trace for one unchanged accelerated lifecycle test. */
public final class LifecyclePreparationProbe {
    private LifecyclePreparationProbe() { }
    public static void attach(GameTestHelper c){
        if(!Boolean.getBoolean("prime_ants.lifecycleProbe"))return;
        var queens=c.getLevel().getEntitiesOfClass(LasiusNigerEntity.class,c.getBounds());
        c.assertTrue(queens.size()==1,"Diagnostic setup must retain the original factory's one actual queen");
        var original=queens.getFirst();var id=original.getUUID();String[] previous={""};
        PrimeAnts.LOGGER.info("T11 LIFE PROBE INITIAL queen={} object={} phase={} lifespan={} grace={} bounds={}",id,System.identityHashCode(original),original.founding().phase(),original.adultLife().lifespan(),original.adultLife().grace(),c.getBounds());
        // Registered at test-function return, alongside the original callback, never during iteration.
        c.onEachTick(()->{
            var l=c.getLevel();var entity=l.getEntity(id);var live=entity instanceof LasiusNigerEntity a?a:null;
            var workers=new ArrayList<>(l.getEntitiesOfClass(LasiusNigerEntity.class,c.getBounds().inflate(8),a->id.equals(a.queenId())));
            workers.sort(Comparator.comparing(a->a.getUUID()));
            var key=new StringBuilder().append(live==original).append('/').append(original.isRemoved()).append('/').append(original.founding().phase()).append('/').append(original.founding().lifecycle());
            var p=original.founding().plan();key.append('/').append(p==null?"NO_PLAN":NestPlan.loaded(l,p.nursery())?l.getBlockState(p.nursery()).toString():"UNAVAILABLE");
            for(var w:workers)key.append('/').append(w.getUUID()).append(':').append(w.workerTasks().phase()).append(':').append(w.workerTasks().reason());
            if(c.getTick()>2&&c.getTick()%200!=0&&previous[0].equals(key.toString()))return;previous[0]=key.toString();
            var row=new JsonObject();row.addProperty("helperTick",c.getTick());row.addProperty("gameTime",l.getGameTime());row.addProperty("dimension",l.dimension().identifier().toString());row.addProperty("sameCanonicalObject",original==live);row.addProperty("worldLookupType",entity==null?"UNKNOWN_ABSENT":entity.getClass().getName());
            row.add("captured",view(l,original));row.add("current",live==null?JsonNull.INSTANCE:view(l,live));
            var ants=new JsonArray();for(var w:workers){var a=view(l,w);a.addProperty("phase",w.workerTasks().phase().name());a.addProperty("reason",w.workerTasks().reason());a.addProperty("cargoDiagnosticString",w.getMainHandItem().toString());a.addProperty("callow",w.isCallow());a.addProperty("onGround",w.onGround());ants.add(a);}row.add("workers",ants);
            PrimeAnts.LOGGER.info("T11 LIFE PROBE {}",row);
        });
    }
    private static JsonObject view(ServerLevel l,LasiusNigerEntity a){
        var j=new JsonObject();j.addProperty("id",a.getUUID().toString());j.addProperty("object",System.identityHashCode(a));j.addProperty("alive",a.isAlive());j.addProperty("removed",a.isRemoved());j.addProperty("position",a.position().toString());j.addProperty("age",a.elapsedAgeTicks());j.addProperty("fasting",a.adultLife().fasting());j.addProperty("noAi",a.isNoAi());j.addProperty("receipts",a.nutrition().consumedUnits());j.addProperty("sugar",a.nutrition().sugar());j.addProperty("protein",a.nutrition().protein());j.addProperty("foundingPhase",a.founding().phase().name());j.addProperty("lifecycle",a.founding().lifecycle().name());j.addProperty("foundingReason",a.founding().reason());
        var p=a.founding().plan();j.addProperty("hasPlan",p!=null);
        if(p!=null){j.addProperty("entrance",p.entrance().toShortString());j.addProperty("outside",p.outside().toShortString());j.addProperty("removedSoil",a.founding().removed());j.addProperty("carriedSoil",a.founding().carried());j.addProperty("depositedSoil",a.founding().deposited());j.addProperty("plugged",a.founding().plugged());j.addProperty("nursery",p.nursery().toShortString());boolean loaded=NestPlan.loaded(l,p.nursery());j.addProperty("nurseryLoaded",loaded);
            if(loaded){j.addProperty("nurseryBlock",l.getBlockState(p.nursery()).toString());var b=l.getBlockEntity(p.nursery());j.addProperty("nurseryEntity",b==null?"MISSING":b.getClass().getName());if(b instanceof BroodPile pile){j.addProperty("ownedNursery",pile.ownedBy(a.getUUID(),p));j.addProperty("nurseryTicks",pile.loadedTicks());j.addProperty("nurseryCondition",pile.condition());}}
        }
        return j;
    }
}
