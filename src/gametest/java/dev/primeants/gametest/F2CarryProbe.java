package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.PrimeAnts;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.worker.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Path;

/** F2-only passive trace. Never calls decision methods, creates paths, or changes an actor. */
public final class F2CarryProbe {
    private static GameTestHelper helper;
    private static UUID queen;
    private static boolean active;
    public static LasiusNigerEntity current;
    private F2CarryProbe() { }
    public static void attach(GameTestHelper c,UUID id) {
        helper=c;queen=id;active=false;
        var setup=base(null,"setup");setup.addProperty("worldSeed",c.getLevel().getSeed());setup.addProperty("dimension",c.getLevel().dimension().identifier().toString());setup.addProperty("rotation",c.getTestRotation().name());setup.addProperty("bounds",c.getBounds().toString());setup.addProperty("randomState","UNKNOWN_NOT_EXPOSED");setup.addProperty("uuidGenerationInputs","UNKNOWN");emit(setup);
        c.onEachTick(()->{if(active){
            var l=c.getLevel();var q=l.getEntity(queen) instanceof LasiusNigerEntity a?a:null;
            if(q==null)return;
            for(var id2:q.founding().foragers())if(l.getEntity(id2) instanceof LasiusNigerEntity w)state(w,"callback");
            var j=base(null,"inventory");var items=new JsonArray();
            for(var i:l.getEntitiesOfClass(ItemEntity.class,c.getBounds().inflate(8),i->i.isAlive())){
                var v=new JsonObject();v.addProperty("id",i.getUUID().toString());v.addProperty("cargo",i.getItem().toString());v.addProperty("count",i.getItem().getCount());v.addProperty("position",i.position().toString());v.addProperty("support",l.getBlockState(i.blockPosition().below()).toString());items.add(v);
            }
            j.add("items",items);var p=q.founding().plan();j.addProperty("outside",p.outside().toShortString());
            var s=NestPlanFixture.store(c,q);j.addProperty("store",s==null?null:s.contents().toString());j.addProperty("storePosition",s==null?null:s.getBlockPos().toShortString());
            var cache=l.getBlockEntity(p.cache());j.addProperty("cache",cache instanceof NestCache n?n.contents().toString():null);
            j.addProperty("custody",TransferCustody.get(l).contents().stream().filter(t->c.getBounds().inflate(8).contains(t.position())).toList().toString());emit(j);
        }});
    }
    public static void begin(){active=true;emit(base(null,"finite_clay_begin"));}
    public static void endpoint(String reason){if(helper==null)return;var j=base(null,"endpoint");j.addProperty("reason",reason);emit(j);var q=helper.getLevel().getEntity(queen);if(q instanceof LasiusNigerEntity a)for(var id:a.founding().foragers())if(helper.getLevel().getEntity(id) instanceof LasiusNigerEntity w)state(w,"endpoint_state");active=false;current=null;}
    public static boolean observes(LasiusNigerEntity w){return active&&helper!=null&&w.level()==helper.getLevel()&&queen.equals(w.queenId())&&helper.getTick()<=30001;}
    private static JsonObject base(LasiusNigerEntity w,String event){var j=new JsonObject();j.addProperty("event",event);j.addProperty("tick",helper.getTick());j.addProperty("gameTime",helper.getLevel().getGameTime());j.addProperty("queen",queen.toString());if(w!=null)j.addProperty("worker",w.getUUID().toString());return j;}
    private static void emit(JsonObject j){PrimeAnts.LOGGER.info("T13 F2 OBS {}",j);}
    public static void event(LasiusNigerEntity w,String event,Object value){if(!observes(w))return;var j=base(w,event);j.addProperty("value",String.valueOf(value));emit(j);}
    public static void state(LasiusNigerEntity w,String event){
        if(!observes(w))return;var j=base(w,event);var t=w.workerTasks();var l=helper.getLevel();
        j.addProperty("objectIdentity",System.identityHashCode(w));j.addProperty("canonical",l.getEntity(w.getUUID())==w);j.addProperty("alive",w.isAlive());j.addProperty("home",String.valueOf(w.nurseryHome()));
        var q=l.getEntity(queen);j.addProperty("claimed",q instanceof LasiusNigerEntity a&&a.founding().claimedBy(w));
        j.addProperty("phase",t.phase().name());j.addProperty("reason",t.reason());j.addProperty("source",String.valueOf(t.sourceId()));j.addProperty("flower",String.valueOf(t.flowerSource()));j.addProperty("inspections",t.flowerInspections());
        j.addProperty("cargo",w.getMainHandItem().toString());j.addProperty("position",w.position().toString());j.addProperty("body",w.getBoundingBox().toString());j.addProperty("ground",w.onGround());
        j.addProperty("sugar",w.nutrition().sugar());j.addProperty("fasting",w.adultLife().fasting());j.addProperty("sharing",t.sharing().busy());
        j.addProperty("floor",l.getBlockState(w.blockPosition().below()).toString());
        j.addProperty("nearbyBodies",l.getEntitiesOfClass(LasiusNigerEntity.class,w.getBoundingBox().inflate(.35),a->a!=w).stream().map(a->a.getUUID()+":"+a.position()+":"+a.getBoundingBox()).toList().toString());
        j.add("path",path(w.getNavigation().getPath()));emit(j);
    }
    public static JsonElement path(Path p){
        if(p==null)return JsonNull.INSTANCE;var j=new JsonObject();j.addProperty("reaches",p.canReach());j.addProperty("cursor",p.getNextNodeIndex());j.addProperty("target",String.valueOf(p.getTarget()));var nodes=new JsonArray();
        for(int i=0;i<p.getNodeCount();i++){var n=p.getNode(i);nodes.add(n.x+","+n.y+","+n.z);}j.add("nodes",nodes);return j;
    }
    public static void pathResult(LasiusNigerEntity w,BlockPos target,Path result){if(!observes(w))return;var j=base(w,"createPath_return");j.addProperty("target",target.toShortString());j.add("path",path(result));emit(j);}
}
