package dev.primeants.worker;

import com.mojang.serialization.Codec;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.NestPlan;
import dev.primeants.founding.NativeVegetation;
import dev.primeants.item.AntItems;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.world.phys.Vec3;

/** One ready physical portion per flower, no offline production or accumulated stock.
 * Yield/cooldown are implementation choices, not Lasius measurements. */
public final class FlowerNectar extends SavedData {
    public static final int ACTION_TICKS=20, COOLDOWN_TICKS=1200;
    public static final Codec<FlowerNectar> CODEC=Codec.unboundedMap(Codec.STRING,Codec.STRING).xmap(FlowerNectar::new,d->d.encode());
    public static final SavedDataType<FlowerNectar> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("prime_ants","flower_nectar"),FlowerNectar::new,CODEC,DataFixTypes.LEVEL);
    private static final class Source {
        String state;int remaining;long harvests,lastTick;
        Source(String s,int r,long h,long t){state=s;remaining=r;harvests=h;lastTick=t;}
    }
    private final Map<BlockPos,Source> sources=new HashMap<>();
    public FlowerNectar(){this(Map.of());}
    private FlowerNectar(Map<String,String> data){data.forEach((key,value)->{
        var o=com.google.gson.JsonParser.parseString(value).getAsJsonObject();
        var s=new Source(o.get("state").getAsString(),o.get("remaining").getAsInt(),o.get("harvests").getAsLong(),o.get("lastTick").getAsLong());
        if(s.remaining<0||s.remaining>COOLDOWN_TICKS||s.harvests<0)throw new IllegalArgumentException("Invalid nectar source");sources.put(BlockPos.of(Long.parseLong(key)),s);
    });}
    private Map<String,String> encode(){var data=new HashMap<String,String>();sources.forEach((p,s)->{
        var o=new com.google.gson.JsonObject();o.addProperty("state",s.state);o.addProperty("remaining",s.remaining);o.addProperty("harvests",s.harvests);o.addProperty("lastTick",s.lastTick);data.put(Long.toString(p.asLong()),o.toString());
    });return data;}
    public static FlowerNectar get(ServerLevel l){return l.getDataStorage().computeIfAbsent(TYPE);}
    public static boolean flower(BlockState s){return NativeVegetation.material(s)&&!s.is(Blocks.SHORT_GRASS)&&!s.is(Blocks.FERN);}
    public static boolean habitat(ServerLevel l,BlockPos p){return NestPlan.loaded(l,p)&&NestPlan.loaded(l,p.below())
        &&flower(l.getBlockState(p))&&l.getBlockState(p).canSurvive(l,p)&&l.getFluidState(p).isEmpty()&&NestPlan.walkable(l,p);}
    public boolean ready(ServerLevel l,BlockPos p){
        if(!habitat(l,p))return false;
        var s=sources.get(p);
        if(s==null){s=new Source(l.getBlockState(p).toString(),0,0,l.getGameTime());sources.put(p.immutable(),s);setDirty();}
        return s.remaining==0&&s.state.equals(l.getBlockState(p).toString());
    }
    /** Any observed replacement, including same-state, delays readiness; never remove the tombstone. */
    public void write(BlockPos p,BlockState next){var s=sources.get(p);if(s!=null){s.state=next.toString();s.remaining=COOLDOWN_TICKS;setDirty();}}
    public int remaining(BlockPos p){var s=sources.get(p);return s==null?-1:s.remaining;}
    public long harvests(){return sources.values().stream().mapToLong(s->s.harvests).sum();}
    public Map<BlockPos,Long> harvestedSources(){var result=new HashMap<BlockPos,Long>();sources.forEach((p,s)->{if(s.harvests>0)result.put(p,s.harvests);});return Map.copyOf(result);}
    public static void tick(ServerLevel l){
        var data=get(l);data.sources.forEach((p,s)->{
            if(s.remaining>0&&s.lastTick!=l.getGameTime()&&NestPlan.loaded(l,p)&&l.isPositionEntityTicking(p)
                &&habitat(l,p)&&s.state.equals(l.getBlockState(p).toString())){s.remaining--;s.lastTick=l.getGameTime();data.setDirty();}
        });
    }
    public boolean harvest(ServerLevel l,LasiusNigerEntity w,BlockPos p,String expected){
        var task=w.workerTasks();var plan=task.plan();
        if(l.getEntity(w.getUUID())!=w||w.isRemoved()||!w.isAlive()||w.isCallow()||w.isNoAi()||!w.getMainHandItem().isEmpty()
            ||!task.authorized(l)||plan==null||!ColonyMembers.get(l).belongs(w,w.queenId(),plan.chamber())
            ||task.phase()!=WorkerTasks.Phase.HARVEST||task.harvestingTicks()<ACTION_TICKS||!p.equals(task.flowerSource())
            ||!l.isPositionEntityTicking(w.blockPosition())||!l.isPositionEntityTicking(p)||!l.mayInteract(w,p)
            ||!task.withinSearch(p)||!habitat(l,p)||!l.getBlockState(p).toString().equals(expected)
            ||!WorkerTasks.reaches(l,w,Vec3.atBottomCenterOf(p).add(0,0.35,0))||!ready(l,p))return false;
        var s=sources.get(p);s.remaining=COOLDOWN_TICKS;s.lastTick=l.getGameTime();s.harvests++;setDirty();
        w.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(AntItems.FLOWER_NECTAR));
        dev.primeants.PrimeAnts.LOGGER.info("Nectar harvest worker={} queen={} source={} sourceHarvests={} cargo={}",w.getUUID(),w.queenId(),p,s.harvests,w.getMainHandItem());return true;
    }
}
