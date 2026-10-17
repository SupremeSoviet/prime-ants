package dev.primeants.gametest;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import dev.primeants.brood.BroodPile;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.worker.*;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.TagValueOutput;

/** T19 opt-in observer. Only finite dropped sources may change the world here. */
final class YoungPersistenceControl {
    interface Delivery { void drop(Item item,int units,String name); }
    private final Set<Integer> waves=new HashSet<>();
    private final Map<UUID,CompoundTag> groundBefore=new HashMap<>();
    private long suppliedApples=4,suppliedChickens=2,expiredApples,expiredChickens;
    private UUID brood;
    private long progress=-1,lastLoaded=-1,consecutive;
    private JsonObject previous;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException(why);}
    private static CompoundTag saved(Entity a,ServerLevel l){
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());
        require(a.save(out),"T19 actor serialization");return out.buildResult();
    }
    private static JsonElement json(CompoundTag tag){return NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE,tag);}
    boolean observe(ServerLevel l,LasiusNigerEntity q,BroodPile pile,List<LasiusNigerEntity> ants,
                    int loaded,JsonObject declaration,Delivery delivery,BiConsumer<String,JsonElement> write){
        for(var value:declaration.getAsJsonArray("physicalFoodSchedule")){
            var wave=value.getAsJsonObject();int at=wave.get("loaded_tick").getAsInt();
            if(loaded==at){
                require(waves.add(at),"T19 source wave repeated");
                int apples=wave.get("apples").getAsInt(),chickens=wave.get("chickens").getAsInt();
                delivery.drop(Items.APPLE,apples,"T19 finite apples tick"+at);
                delivery.drop(Items.CHICKEN,chickens,"T19 finite chickens tick"+at);
                suppliedApples+=apples;suppliedChickens+=chickens;
                write.accept("control-wave-"+loaded,wave.deepCopy());
            }
        }
        long apples=0,chickens=0;var ground=new HashMap<UUID,CompoundTag>();var groundJson=new JsonObject();
        for(var a:l.getAllEntities())if(a instanceof ItemEntity item){
            var stack=item.getItem();if(!stack.is(Items.APPLE)&&!stack.is(Items.CHICKEN))continue;
            var tag=saved(item,l);ground.put(item.getUUID(),tag);groundJson.add(item.getUUID().toString(),json(tag));
            if(stack.is(Items.APPLE))apples+=stack.getCount();else chickens+=stack.getCount();
        }
        // A disappearing old ground stack is classified only at vanilla's expiry boundary.
        // Pickup/deposit/ingestion is independently balanced by the complete physical equation below.
        for(var entry:groundBefore.entrySet())if(!ground.containsKey(entry.getKey())&&entry.getValue().getIntOr("Age",0)>=5999){
            var item=entry.getValue().getCompoundOrEmpty("Item");
            String id=item.getStringOr("id","");int count=item.getIntOr("count",0);
            if(id.equals("minecraft:apple"))expiredApples+=count;
            if(id.equals("minecraft:chicken"))expiredChickens+=count;
            write.accept("control-expiry-"+loaded+"-"+entry.getKey(),json(entry.getValue()));
        }
        groundBefore.clear();groundBefore.putAll(ground);
        var cache=(NestCache)l.getBlockEntity(q.founding().plan().cache());
        require(cache!=null&&cache.ownedBy(q.getUUID(),q.founding().plan()),"T19 canonical owned cache");
        for(var stack:cache.contents()){if(stack.is(Items.APPLE))apples+=stack.getCount();if(stack.is(Items.CHICKEN))chickens+=stack.getCount();}
        long receiptsA=pile.consumedApples(),receiptsC=pile.consumedChickens();var actors=new JsonObject();
        for(var a:ants){
            require(a.nutrition().snapshot().valid(),"T19 exact nutrition equation "+a.getUUID());
            receiptsA+=a.nutrition().apples();receiptsC+=a.nutrition().chickens();
            var stack=a.getMainHandItem();if(stack.is(Items.APPLE))apples+=stack.getCount();if(stack.is(Items.CHICKEN))chickens+=stack.getCount();
            actors.add(a.getUUID().toString(),json(saved(a,l)));
        }
        var row=new JsonObject();row.addProperty("loaded",loaded);row.addProperty("gameTime",l.getGameTime());row.addProperty("nurseryLoaded",pile.loadedTicks());
        row.addProperty("condition",pile.condition());row.addProperty("recentSugar",pile.growth().recentSugar());row.addProperty("recentProtein",pile.growth().recentProtein());row.addProperty("incomeObservedTicks",pile.growth().observedTicks());
        row.add("supply",new Gson().toJsonTree(pile.supply(l)));row.add("actors",actors);row.add("ground",groundJson);
        row.add("nursery",json(pile.saveWithFullMetadata(l.registryAccess())));row.add("cache",json(cache.saveWithFullMetadata(l.registryAccess())));
        var account=new JsonObject();account.addProperty("suppliedApplesIncludingGeneratedCache",suppliedApples);account.addProperty("suppliedChickensIncludingGeneratedCache",suppliedChickens);
        account.addProperty("physicalApples",apples);account.addProperty("physicalChickens",chickens);account.addProperty("appleReceipts",receiptsA);account.addProperty("chickenReceipts",receiptsC);
        account.addProperty("expiredApples",expiredApples);account.addProperty("expiredChickens",expiredChickens);
        boolean balanced=apples+receiptsA+expiredApples==suppliedApples&&chickens+receiptsC+expiredChickens==suppliedChickens;
        account.addProperty("balanced",balanced);row.add("foodAccounting",account);
        var selected=pile.records().stream().filter(r->!r.founding()&&(brood==null||brood.equals(r.id()))).findFirst().orElse(null);
        if(selected!=null){
            if(brood==null){
                brood=selected.id();require(selected.stage()==dev.primeants.brood.BroodStage.EGG,"T19 observe actual newly laid egg");
                require(pile.growth().recentSugar()>=4500,"T19 real rolling income at natural laying");
                row.addProperty("newBrood",brood.toString());
                write.accept("control-natural-egg",row.deepCopy());
                if(previous!=null)write.accept("control-before-natural-egg",previous.deepCopy());
            }else if(lastLoaded==loaded-1&&selected.progress()==progress+1)consecutive++;else consecutive=0;
            progress=selected.progress();lastLoaded=loaded;
        }else if(brood!=null)consecutive=0;
        row.addProperty("observedBrood",brood==null?null:brood.toString());row.addProperty("successiveProgressSteps",consecutive);row.addProperty("progress",progress);
        if(loaded==1||loaded%200==0||selected!=null||!balanced||loaded>=declaration.get("controlBound").getAsInt())write.accept("control-observation-"+loaded,row);
        previous=row;
        require(balanced,"T19 physical food equation diverged at loaded="+loaded);
        if(consecutive>=200){
            require(receiptsA>=2&&q.nutrition().consumedUnits()>0,"T19 real apple income and queen ingestion");
            write.accept("control-accepted",row.deepCopy());return true;
        }
        require(loaded<declaration.get("controlBound").getAsInt(),"T19 default-speed Young prerequisite unmet: "+pile.condition());return false;
    }
}
