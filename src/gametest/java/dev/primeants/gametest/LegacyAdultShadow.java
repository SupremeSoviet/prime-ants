package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.entity.*;
import dev.primeants.gametest.mixin.GameTestHelperAccessor;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;

/** Read-only test observer. One pending step and one final state per original UUID, no tick transcript. */
public final class LegacyAdultShadow implements AutoCloseable {
    private static final List<LegacyAdultShadow> sessions=new ArrayList<>();
    private final UUID colony;
    private final String fixture;
    private final Map<UUID,Row> rows=new LinkedHashMap<>();
    private JsonObject firstMismatch;
    private boolean closed;
    private static final class Row {
        LasiusNigerEntity.Biology input,last;
        LegacyAdultReference.Step expected;
        float health;
        long steps,exempt,activations,damage,terminal,incomingMeals,spent;
        boolean reserve;
        UUID brood,queen;net.minecraft.core.BlockPos home;
    }
    private LegacyAdultShadow(UUID colony,String fixture){this.colony=colony;this.fixture=fixture;sessions.add(this);}
    public static LegacyAdultShadow watch(GameTestHelper c,UUID colony,String fixture){
        var observer=new LegacyAdultShadow(colony,fixture);
        ((GameTestHelperAccessor)c).primeAntsTestInfo().addListener(new GameTestListener(){
            public void testStructureLoaded(GameTestInfo i){}
            public void testPassed(GameTestInfo i,GameTestRunner r){observer.close();}
            public void testFailed(GameTestInfo i,GameTestRunner r){observer.close();}
            public void testAddedForRerun(GameTestInfo i,GameTestInfo copy,GameTestRunner r){observer.close();}
        });
        return observer;
    }
    private boolean includes(LasiusNigerEntity ant){return !closed&&colony.equals(ant.colonyIdentity());}
    public static void beforeLife(LasiusNigerEntity ant){
        for(var observer:List.copyOf(sessions))if(observer.includes(ant))observer.begin(ant);
    }
    public static void afterLife(LasiusNigerEntity ant){
        for(var observer:List.copyOf(sessions))if(observer.includes(ant))observer.endLife(ant);
    }
    public static void afterEntity(LasiusNigerEntity ant){
        for(var observer:List.copyOf(sessions))if(observer.includes(ant))observer.endEntity(ant);
    }
    private void begin(LasiusNigerEntity ant){
        if(!rows.containsKey(ant.getUUID())&&rows.size()>=121){mismatch(ant,null,"observer_identity_bound",null);return;}
        var r=rows.computeIfAbsent(ant.getUUID(),id->new Row());
        r.input=ant.biologySnapshot();r.health=ant.getHealth();
        r.reserve=ant.form()==AntForm.QUEEN&&ant.founding().reserveOnlyFounding((ServerLevel)ant.level());
        r.expected=LegacyAdultReference.step(r.input,r.reserve,r.health>1);
        r.brood=ant.broodId();r.queen=ant.queenId();r.home=ant.nurseryHome();
        if(r.last!=null){
            var a=r.last.nutrition();var b=r.input.nutrition();
            if(a.apples()!=b.apples()||a.berries()!=b.berries()||a.chickens()!=b.chickens()
                ||a.nectar()!=b.nectar()||a.nectarV2()!=b.nectarV2()||a.prey()!=b.prey()
                ||a.flesh()!=b.flesh()||a.receivedSugar()!=b.receivedSugar())r.incomingMeals++;
        }
    }
    private void endLife(LasiusNigerEntity ant){
        var r=rows.get(ant.getUUID());if(r==null||r.input==null)return;
        var actual=ant.biologySnapshot();
        if(!actual.life().equals(r.expected.life())||!actual.nutrition().equals(r.expected.nutrition())
            ||actual.age()!=r.input.age()||ant.getHealth()!=r.health-(r.expected.damage()?1:0))
            mismatch(ant,r,"after_ordinary_life",null);
        r.steps++;if(!r.expected.life().started())r.exempt++;
        if(!r.input.life().started()&&r.expected.life().started())r.activations++;
        if(r.expected.damage())r.damage++;
        if(!ant.isAlive())r.terminal++;
        r.spent+=r.expected.nutrition().spentSugar()-r.input.nutrition().spentSugar();
    }
    private void endEntity(LasiusNigerEntity ant){
        var r=rows.get(ant.getUUID());if(r==null||r.input==null)return;
        long callow=r.input.callowAge();
        if(ant.isAlive()&&r.input.hasBrood()&&callow<r.input.callowDuration())callow++;
        if(ant.elapsedAgeTicks()!=r.input.age()||ant.callowAgeTicks()!=callow
            ||ant.simulationEvidence().latest()!=ant.level().getGameTime()
            ||!Objects.equals(r.brood,ant.broodId())||!Objects.equals(r.queen,ant.queenId())||!Objects.equals(r.home,ant.nurseryHome()))
            mismatch(ant,r,"after_ordinary_entity_completion",null);
        r.last=ant.biologySnapshot();r.input=null;
    }
    public void exception(LasiusNigerEntity ant,RuntimeException error){mismatch(ant,rows.get(ant.getUUID()),"ordinary_step_exception",error);}
    private void mismatch(LasiusNigerEntity ant,Row r,String boundary,RuntimeException error){
        if(firstMismatch!=null)return;
        firstMismatch=new JsonObject();var gson=new Gson();
        firstMismatch.addProperty("uuid",ant.getUUID().toString());firstMismatch.addProperty("game_time",ant.level().getGameTime());
        firstMismatch.addProperty("boundary",boundary);
        if(r!=null){firstMismatch.add("input_after_age_before_life",gson.toJsonTree(r.input));firstMismatch.add("expected_legacy",gson.toJsonTree(r.expected));firstMismatch.addProperty("health_before",r.health);firstMismatch.addProperty("reserve_only",r.reserve);}
        firstMismatch.add("actual",gson.toJsonTree(ant.biologySnapshot()));firstMismatch.addProperty("health_after",ant.getHealth());
        if(error!=null)firstMismatch.addProperty("exception",error.toString());
        ReconciliationEvidence.log("legacy_first_mismatch",fixture,firstMismatch.deepCopy());
    }
    public void assertAgreement(GameTestHelper c){c.assertTrue(firstMismatch==null,"Independent debb542 reference mismatch: "+firstMismatch);}
    public long steps(){return rows.values().stream().mapToLong(r->r.steps).sum();}
    public long exempt(){return rows.values().stream().mapToLong(r->r.exempt).sum();}
    public long activations(){return rows.values().stream().mapToLong(r->r.activations).sum();}
    public long activations(UUID adult){var row=rows.get(adult);return row==null?0:row.activations;}
    @Override public void close(){
        if(closed)return;closed=true;sessions.remove(this);
        var out=new JsonObject();out.addProperty("colony",colony.toString());out.addProperty("identities",rows.size());
        out.addProperty("steps",steps());out.addProperty("agreement",firstMismatch==null);var adults=new JsonArray();
        for(var entry:rows.entrySet()){
            var r=entry.getValue();var a=new JsonObject();a.addProperty("uuid",entry.getKey().toString());
            a.addProperty("steps",r.steps);a.addProperty("exempt",r.exempt);a.addProperty("activations",r.activations);
            a.addProperty("damage",r.damage);a.addProperty("terminal",r.terminal);a.addProperty("incoming_meal_boundaries",r.incomingMeals);a.addProperty("maintenance_sugar_spent",r.spent);
            a.add("final_biology",new Gson().toJsonTree(r.last));adults.add(a);
        }
        out.add("adults",adults);if(firstMismatch!=null)out.add("first_mismatch",firstMismatch);
        ReconciliationEvidence.log("legacy_shadow_summary",fixture,out);
    }
}
