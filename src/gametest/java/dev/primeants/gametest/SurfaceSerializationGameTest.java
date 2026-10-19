package dev.primeants.gametest;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import dev.primeants.PrimeAnts;
import dev.primeants.colony.ColonyStage;
import dev.primeants.founding.SurfaceWork;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.storage.*;

/** Historical jobs are serialization inputs only, never installed in the live world's ledger. */
public final class SurfaceSerializationGameTest {
    private static final String TESTED="26a9b99c-662e-3728-9dff-35317afd6cbd:mature";
    private static final String FOREIGN="22983d0c-2f40-3b74-a460-d5f359392793:great";
    private static final int ROUNDS=4;

    private static JsonObject fixture(){
        try(var in=Objects.requireNonNull(SurfaceSerializationGameTest.class.getResourceAsStream(
                "/dev/primeants/gametest/legacy-surface-two-owners.json"))){
            return JsonParser.parseString(new String(in.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(java.io.IOException e){throw new IllegalStateException(e);}
    }
    private static JsonObject job(JsonObject ledger,String key){return JsonParser.parseString(ledger.get(key).getAsString()).getAsJsonObject();}
    private static JsonObject encode(SurfaceWork data){return SurfaceWork.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow().getAsJsonObject();}
    private static SurfaceWork decode(JsonObject data){return SurfaceWork.CODEC.parse(JsonOps.INSTANCE,data).getOrThrow();}
    private static JsonObject canonical(JsonObject input){
        var result=input.deepCopy();
        for(var key:input.keySet()){
            var j=job(input,key);var ground=j.getAsJsonObject("ground");var sorted=new JsonObject();
            new TreeSet<>(ground.keySet()).forEach(k->sorted.add(k,ground.get(k)));j.add("ground",sorted);result.addProperty(key,j.toString());
        }return result;
    }
    private static void semantics(GameTestHelper c,JsonObject input,JsonObject output){
        c.assertTrue(input.keySet().equals(output.keySet()),"Every owner/stage survives, including the foreign Great job");
        for(var key:input.keySet())c.assertTrue(job(input,key).equals(job(output,key)),
            "Decoded values and ordered receipts, access operations, releases and transitions remain exact: "+key);
        var before=decode(input);var after=decode(output);
        for(var owner:List.of(UUID.fromString(TESTED.split(":")[0]),UUID.fromString(FOREIGN.split(":")[0]))){
            var stage=owner.toString().equals(TESTED.split(":")[0])?ColonyStage.MATURE:ColonyStage.GREAT;
            var a=before.job(owner,stage);var b=after.job(owner,stage);
            c.assertTrue(a.plan.cells().equals(b.plan.cells())&&a.cells().equals(b.cells()),"Plan-cell order and derived positions remain exact");
        }
    }
    private static JsonObject mismatch(String phase,JsonObject expected,JsonObject actual){
        if(expected.equals(actual))return null;
        var row=new JsonObject();row.addProperty("phase",phase);row.add("owner_keys",new Gson().toJsonTree(expected.keySet()));
        row.add("expected",expected);row.add("actual",actual);var values=new JsonObject();
        for(var key:expected.keySet()){
            var pair=new JsonObject();pair.add("expected",job(expected,key));pair.add("actual",job(actual,key));
            pair.addProperty("decoded_equal",job(expected,key).equals(job(actual,key)));values.add(key,pair);
        }row.add("decoded_values",values);return row;
    }
    @GameTest(maxTicks=200,structure="prime_ants_test:surface_ground")
    public void foreignGreatJobGroundOrderSurvivesDiskRoundtrips(GameTestHelper c){
        var legacy=fixture();var input=legacy.getAsJsonObject("before");var expected=canonical(encode(decode(input)));
        c.assertTrue(input.size()==2&&input.has(TESTED)&&input.has(FOREIGN),"Exact tested bound attempt and unrelated Great owner share one ledger");
        c.assertTrue(job(input,TESTED).has("access")&&!job(input,TESTED).get("claim").getAsString().isEmpty(),"Tested attempt retains its original binding and canonical actor claim");
        var ground=job(input,FOREIGN).getAsJsonObject("ground");
        c.assertTrue(ground.has("-2,1")&&ground.has("-2,-4"),"Both colliding legacy ground keys are present");
        var variants=new ArrayList<JsonObject>();variants.add(input);variants.add(legacy.getAsJsonObject("after"));
        var keys=new ArrayList<>(ground.keySet());
        for(int n=0;n<keys.size();n++){
            Collections.rotate(keys,1);var reordered=new JsonObject();keys.forEach(k->reordered.add(k,ground.get(k)));
            var j=job(input,FOREIGN);j.add("ground",reordered);var ledger=input.deepCopy();ledger.addProperty(FOREIGN,j.toString());variants.add(ledger);
        }
        JsonObject firstOrder=null,firstRoundtrip=null,firstCanonical=null,reference=null;int diskReads=0;
        var l=c.getLevel();var folder=l.getServer().getWorldPath(LevelResource.ROOT).resolve("surface-serialization-fixture");
        for(int v=0;v<variants.size();v++){
            var original=variants.get(v);semantics(c,input,original);var data=decode(original);var encoded=encode(data);
            semantics(c,original,encoded);
            if(reference==null)reference=encoded;
            if(firstOrder==null)firstOrder=mismatch("equivalent_order_"+v,reference,encoded);
            if(firstCanonical==null)firstCanonical=mismatch("canonical_order_"+v,expected,encoded);
            for(int round=0;round<ROUNDS;round++){
                var previous=encoded;data=decode(encoded);encoded=encode(data);semantics(c,original,encoded);
                if(firstRoundtrip==null)firstRoundtrip=mismatch("codec_"+v+"_"+round,previous,encoded);
                // Separate real SavedData storage: no actors, terrain authority or live construction use these receipts.
                try(var disk=new SavedDataStorage(folder,net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){
                    disk.set(SurfaceWork.TYPE,data);data.setDirty();disk.saveAndJoin();
                }
                previous=encoded;
                try(var disk=new SavedDataStorage(folder,net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){
                    data=Objects.requireNonNull(disk.get(SurfaceWork.TYPE));encoded=encode(data);diskReads++;
                }
                semantics(c,original,encoded);
                if(firstRoundtrip==null)firstRoundtrip=mismatch("disk_"+v+"_"+round,previous,encoded);
            }
        }
        var result=new JsonObject();result.addProperty("variants",variants.size());result.addProperty("rounds_per_variant",ROUNDS);
        result.addProperty("disk_readbacks",diskReads);result.add("owner_keys",new Gson().toJsonTree(input.keySet()));
        result.add("equivalent_order_mismatch",firstOrder);result.add("roundtrip_mismatch",firstRoundtrip);result.add("canonical_mismatch",firstCanonical);
        result.add("canonical",expected);result.addProperty("semantic_preservation",true);
        PrimeAnts.LOGGER.info("SURFACE_SERIALIZATION {}",result);
        c.assertTrue(firstOrder==null,"Equivalent legacy ground insertion orders must encode exactly equally, including the foreign Great job");
        c.assertTrue(firstRoundtrip==null,"Repeated codec and real SavedData disk roundtrips must preserve exact ledger strings");
        c.assertTrue(firstCanonical==null,"Unordered ground properties must encode in lexical key order");
        c.succeed();
    }
}
