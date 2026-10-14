package dev.primeants.gametest;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import dev.primeants.brood.*;
import dev.primeants.entity.*;
import dev.primeants.worker.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Captured accepted saves, translated as a whole; no target, contact or receipt is forced.
 * Entity saves do not contain navigation paths. Both branches start with native reconstructed navigation. */
final class CapturedFiniteControl {
    record Result(LasiusNigerEntity queen,BroodPile pile,LasiusNigerEntity hungry,NestCache cache){}
    private static int[] uuid(UUID id){return new int[]{(int)(id.getMostSignificantBits()>>32),(int)id.getMostSignificantBits(),(int)(id.getLeastSignificantBits()>>32),(int)id.getLeastSignificantBits()};}
    private static Tag translate(Tag tag,String key,BlockPos delta,Map<UUID,UUID> ids){
        if(tag instanceof CompoundTag compound){var out=new CompoundTag();for(var e:compound.entrySet())out.put(e.getKey(),translate(e.getValue(),e.getKey(),delta,ids));
            if(compound.getInt("x").isPresent()&&compound.getInt("y").isPresent()&&compound.getInt("z").isPresent()){out.putInt("x",compound.getIntOr("x",0)+delta.getX());out.putInt("y",compound.getIntOr("y",0)+delta.getY());out.putInt("z",compound.getIntOr("z",0)+delta.getZ());}return out;}
        if(tag instanceof IntArrayTag array){var a=array.getAsIntArray();if(a.length==3)return new IntArrayTag(new int[]{a[0]+delta.getX(),a[1]+delta.getY(),a[2]+delta.getZ()});
            if(a.length==4){var id=new UUID((long)a[0]<<32|Integer.toUnsignedLong(a[1]),(long)a[2]<<32|Integer.toUnsignedLong(a[3]));if(ids.containsKey(id))return new IntArrayTag(uuid(ids.get(id)));}return tag.copy();}
        if(tag instanceof ListTag list){var out=new ListTag();for(int i=0;i<list.size();i++)out.add(translate(list.get(i),key,delta,ids));
            if(key.equals("Pos")){out.set(0,DoubleTag.valueOf(list.getDoubleOr(0,0)+delta.getX()));out.set(1,DoubleTag.valueOf(list.getDoubleOr(1,0)+delta.getY()));out.set(2,DoubleTag.valueOf(list.getDoubleOr(2,0)+delta.getZ()));}return out;}
        if(tag instanceof StringTag s){var value=s.asString().orElseThrow();try{var id=UUID.fromString(value);if(ids.containsKey(id))return StringTag.valueOf(ids.get(id).toString());}catch(IllegalArgumentException ignored){} }
        return tag.copy();
    }
    static Result restore(GameTestHelper c,LasiusNigerEntity q,BroodPile pile,LasiusNigerEntity hungry,NestCache cache,String name,boolean nursesFirst){
        try(var stream=CapturedFiniteControl.class.getResourceAsStream("/catchup-controls/"+name+".json")){
            c.assertTrue(stream!=null,"Captured portable control exists");var config=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();var input=config.getAsJsonObject("inputs");
            var oldPile=TagParser.parseCompoundFully(input.get("saved_pile").getAsString());var oldEntrance=oldPile.getIntArray("Entrance").orElseThrow();var entrance=q.founding().plan().entrance();
            c.assertTrue(oldPile.getStringOr("Direction","").equals(q.founding().plan().direction().getName()),"Captured control translation preserves orientation");var delta=entrance.subtract(new BlockPos(oldEntrance[0],oldEntrance[1],oldEntrance[2]));
            var current=AutomaticCatchupGameTest.adults(c,q);var nurses=current.stream().filter(a->a.form()==AntForm.WORKER&&a!=hungry).toList();var ids=new HashMap<UUID,UUID>();var oldTags=new ArrayList<CompoundTag>();int nurse=0;
            for(var text:input.getAsJsonArray("saved_adults"))oldTags.add(TagParser.parseCompoundFully(text.getAsString()));
            int hungryIndex=config.get("hungry_index").getAsInt();
            var order=new ArrayList<UUID>();
            for(int i=0;i<oldTags.size();i++){
                var old=oldTags.get(i);var source=input.getAsJsonObject("baseline").getAsJsonArray("adults").get(i).getAsJsonObject();var target=i==0?q:i==hungryIndex?hungry:nurses.get(nurse++);
                ids.put(UUID.fromString(source.get("id").getAsString()),target.getUUID());order.add(target.getUUID());
                if(i>0)ids.put(UUID.fromString(old.getStringOr("BroodId","")),target.broodId());
            }
            ids.put(UUID.fromString(oldPile.getListOrEmpty("Brood").getCompoundOrEmpty(0).getStringOr("Id","")),pile.records().getFirst().id());
            var tags=oldTags.stream().map(t->(CompoundTag)translate(t,"",delta,ids)).toList();
            var members=ColonyMembers.CODEC.encodeStart(JsonOps.INSTANCE,ColonyMembers.get(c.getLevel())).getOrThrow().getAsJsonArray();var sorted=new JsonArray();
            var workerOrder=new ArrayList<>(order.subList(1,order.size()));if(nursesFirst){workerOrder.remove(hungry.getUUID());workerOrder.add(hungry.getUUID());}
            for(var id:workerOrder)for(var m:members)if(m.getAsJsonObject().get("worker").getAsString().equals(id.toString()))sorted.add(m);
            for(var m:members)if(!workerOrder.contains(UUID.fromString(m.getAsJsonObject().get("worker").getAsString())))sorted.add(m);
            current.forEach(a->a.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK));
            c.getLevel().getDataStorage().set(ColonyMembers.TYPE,ColonyMembers.CODEC.parse(JsonOps.INSTANCE,sorted).getOrThrow());
            var restored=new ArrayList<LasiusNigerEntity>();
            for(int i=0;i<tags.size();i++){var a=ReconciliationGameTest.load(c,tags.get(i));a.tickCount=input.getAsJsonArray("saved_tick_counts").get(i).getAsInt();c.assertTrue(c.getLevel().tryAddFreshEntityWithPassengers(a),"Captured identity remapped to existing paid lineage");
                var saved=ReconciliationGameTest.save(c,a);var expected=tags.get(i).copy();
                // Legacy captures predate the optional phase observation. Its fallback is the saved counter.
                expected.getCompoundOrEmpty("WorkerTask").getCompoundOrEmpty("CropSharing").putInt("SearchPhase",Math.floorMod(a.tickCount,20));
                expected.getCompoundOrEmpty("WorkerTask").getCompoundOrEmpty("CropSharing").putLong("ObservedTicks",0);
                for(var key:List.of("Nutrition","AdultLife","WorkerTask","AdultSimulation","Pos"))c.assertTrue(saved.get(key).equals(expected.get(key)),"Captured "+key+" survives ordinary reconstruction exactly");restored.add(a);}
            var back=BroodGameTest.restorePile(c,pile,(CompoundTag)translate(oldPile,"",delta,ids));
            var cacheTag=(CompoundTag)translate(TagParser.parseCompoundFully(input.get("saved_cache").getAsString()),"",delta,ids);var pos=cache.getBlockPos();var block=cache.getBlockState();c.getLevel().removeBlockEntity(pos);
            var stock=(NestCache)BlockEntity.loadStatic(pos,block,cacheTag,c.getLevel().registryAccess());c.assertTrue(stock!=null,"Captured finite stock normally reconstructed");c.getLevel().setBlockEntity(stock);stock.refreshDisplay();
            var row=new JsonObject();row.addProperty("configuration",name);row.addProperty("nurses_first",nursesFirst);row.addProperty("historical_crop_provenance",config.get("historical_received").getAsLong());row.addProperty("navigation_limit","Unsaved native path/move-control internals reconstructed in both branches");row.add("state",AllocationObservations.state(c,restored.getFirst(),back,back.lastSimulationGameTime().orElseThrow()+1));ReconciliationEvidence.log("t03_captured_control","portable_saved_reconstruction",row);
            return new Result(restored.getFirst(),back,restored.get(hungryIndex),stock);
        }catch(Exception e){throw new IllegalStateException("Captured finite control reconstruction failed",e);}
    }
}
