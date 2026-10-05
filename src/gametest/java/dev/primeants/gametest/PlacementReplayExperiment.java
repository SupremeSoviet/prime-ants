package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.entity.*;
import dev.primeants.worker.*;
import dev.primeants.founding.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.LevelResource;

/** Untouched T17 owned-copy observer: tickets, real ticks and receipts only. */
final class PlacementReplayExperiment {
    private ServerLevel level;
    private Path evidence;
    private JsonObject declaration;
    private int ticks;
    private boolean done,initial;
    private final JsonArray events=new JsonArray(),snapshots=new JsonArray();
    private final Map<UUID,Long> receipts=new HashMap<>();
    private final Map<UUID,Integer> cargo=new HashMap<>(),durations=new HashMap<>();
    private final Map<UUID,BlockPos> sources=new HashMap<>();
    void install(){
        evidence=Path.of(System.getProperty("prime_ants.placementEvidence"));
        ServerLifecycleEvents.SERVER_STARTED.register(s->{try{start(s);}catch(Throwable e){fail(s,e);}});
        ServerTickEvents.START_SERVER_TICK.register(s->{if(done||level==null)return;try{beforeTick(s);}catch(Throwable e){fail(s,e);}});
        ServerTickEvents.END_SERVER_TICK.register(s->{if(done||level==null)return;try{afterTick(s);}catch(Throwable e){fail(s,e);}});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->write("stopped",result(s)));
    }
    private void start(MinecraftServer s)throws Exception{
        declaration=JsonParser.parseString(Files.readString(Path.of(System.getProperty("prime_ants.placementDeclaration")))).getAsJsonObject();
        level=s.getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.fromNamespaceAndPath("prime_ants_test","placement_native")));
        require(level!=null&&level.getSeed()==2026100501L,"Original seed/dimension required");
        write("process",result(s));
        for(var v:declaration.getAsJsonArray("selected")){var a=v.getAsJsonArray();var p=new ChunkPos(a.get(0).getAsInt(),a.get(1).getAsInt());level.getChunkSource().addTicketWithRadius(PlacementSettings.TICKET,p,3);level.getChunkSource().getChunk(p.x(),p.z(),ChunkStatus.FULL,true);}
        s.tickRateManager().requestGameToSprint(8000);
    }
    private List<LasiusNigerEntity> ants(){var out=new ArrayList<LasiusNigerEntity>();for(var e:level.getAllEntities())if(e instanceof LasiusNigerEntity a&&a.isAlive())out.add(a);return out;}
    private void beforeTick(MinecraftServer s){
        var ants=ants();
        if(!initial&&ants.size()==8){
            var expected=new HashSet<String>();declaration.getAsJsonArray("queens").forEach(v->expected.add(v.getAsString()));declaration.getAsJsonArray("workers").forEach(v->expected.add(v.getAsString()));
            require(ants.stream().map(a->a.getUUID().toString()).collect(java.util.stream.Collectors.toSet()).equals(expected),"Original two queens/six worker identities required");
            require(ants.stream().allMatch(a->a.getMainHandItem().isEmpty()&&a.nutrition().consumedUnits()==0&&a.nutrition().sugar()==0&&a.nutrition().protein()==0),"Original zero food/receipts required");
            for(var a:ants)if(a.form()==AntForm.QUEEN){var p=a.founding().plan();require(p!=null&&a.bodyReserve()==0&&a.founding().ready(),"Original settled/open exhausted queen");require(!(level.getBlockEntity(p.cache()) instanceof NestCache n)||n.size()==0,"Zero cached food");require(level.getBlockEntity(p.nursery()) instanceof dev.primeants.brood.BroodPile b&&b.records().isEmpty()&&b.consumedFood()==0,"Original completed zero-food clutch");}
            require(FlowerNectar.get(level).harvests()==0,"Original zero native harvests");initial=true;write("initial",snapshot());
        }
        if(initial)for(var a:ants){
            receipts.put(a.getUUID(),a.nutrition().nectar());
            cargo.put(a.getUUID(),a.getMainHandItem().is(dev.primeants.item.AntItems.FLOWER_NECTAR)?a.getMainHandItem().getCount():0);
            if(a.workerTasks().flowerSource()!=null){sources.put(a.getUUID(),a.workerTasks().flowerSource());durations.put(a.getUUID(),a.workerTasks().harvestingTicks());}
        }
    }
    private void afterTick(MinecraftServer s){
        ticks++;
        if(initial)for(var a:ants()){
            int now=a.getMainHandItem().is(dev.primeants.item.AntItems.FLOWER_NECTAR)?a.getMainHandItem().getCount():0;
            int prior=cargo.getOrDefault(a.getUUID(),0);
            if(now>prior&&sources.containsKey(a.getUUID())&&a.workerTasks().phase()==WorkerTasks.Phase.RETURN){
                var row=event("harvest",a);var source=sources.get(a.getUUID());row.addProperty("source",source.asLong());row.addProperty("source_state",level.getBlockState(source).toString());row.addProperty("generated_witness",NativeVegetation.get(level).eligible(level,source));row.addProperty("action_ticks",durations.getOrDefault(a.getUUID(),0)+1);row.addProperty("item",a.getMainHandItem().toString());events.add(row);
            }
            if(prior>now&&a.workerTasks().reason().equals("food_physically_stored")){var row=event("cache_delivery",a);row.addProperty("item","prime_ants:flower_nectar");row.addProperty("cache",a.workerTasks().plan().cache().asLong());events.add(row);}
            if(a.nutrition().nectar()>receipts.getOrDefault(a.getUUID(),0L)){var row=event("recipient_receipt",a);row.addProperty("nectar_receipts",a.nutrition().nectar());row.addProperty("sugar",a.nutrition().sugar());events.add(row);}
        }
        if(ticks%1000==0)snapshots.add(snapshot());
        if(ticks==8000){require(initial,"Initial verification must precede observation");write("result",result(s));done=true;s.halt(false);}
    }
    private JsonObject event(String kind,LasiusNigerEntity a){var row=new JsonObject();row.addProperty("kind",kind);row.addProperty("server_tick",ticks);row.addProperty("actor",a.getUUID().toString());row.addProperty("position",a.position().toString());return row;}
    private JsonObject snapshot(){
        var out=new JsonObject();out.addProperty("tick",ticks);out.addProperty("harvests",FlowerNectar.get(level).harvests());var rows=new JsonArray();
        for(var a:ants()){
            var row=event("live",a);row.addProperty("form",a.form().name());row.addProperty("age",a.elapsedAgeTicks());row.addProperty("fasting",a.adultLife().fasting());row.addProperty("lifespan",a.adultLife().lifespan());row.addProperty("phase",a.workerTasks().phase().name());row.addProperty("reason",a.workerTasks().reason());row.addProperty("cargo",a.getMainHandItem().toString());row.addProperty("nectar_receipts",a.nutrition().nectar());row.addProperty("queen_ready",a.form()==AntForm.QUEEN&&a.founding().ready());row.addProperty("navigation_done",a.getNavigation().isDone());row.addProperty("ground",a.onGround());
            var source=a.workerTasks().flowerSource();if(source!=null){row.addProperty("source",source.asLong());row.addProperty("source_state",level.getBlockState(source).toString());row.addProperty("source_ticking",level.isPositionEntityTicking(source));row.addProperty("source_ready",FlowerNectar.get(level).ready(level,source));row.addProperty("reach_visibility",WorkerTasks.reaches(level,a,net.minecraft.world.phys.Vec3.atCenterOf(source)));row.addProperty("action_ticks",a.workerTasks().harvestingTicks());}
            rows.add(row);
        }
        out.add("actors",rows);out.add("source_ledger",FlowerNectar.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,FlowerNectar.get(level)).getOrThrow());return out;
    }
    private JsonObject result(MinecraftServer s){var r=new JsonObject();r.addProperty("mode","t18-replay-v1");r.addProperty("seed",2026100501L);r.addProperty("world",s.getWorldPath(LevelResource.ROOT).toAbsolutePath().toString());r.addProperty("server_ticks",ticks);r.addProperty("initial_verified",initial);r.addProperty("biome_queries",0);r.addProperty("budget_used",PlacementSettings.replayBudget());r.addProperty("total_full_chunks",PlacementSettings.EXISTING_FULL.size()+(int)PlacementSettings.ALL_FULL.stream().filter(p->!PlacementSettings.EXISTING_FULL.contains(p)).count());
        var generated=new JsonArray();PlacementSettings.ALL_FULL.stream().filter(p->!PlacementSettings.EXISTING_FULL.contains(p)).sorted().forEach(generated::add);r.add("new_full",generated);r.addProperty("new_full_count",generated.size());r.add("events",events);r.add("snapshots",snapshots);if(level!=null)r.add("final",snapshot());return r;}
    private void require(boolean ok,String why){if(!ok)throw new IllegalStateException(why);}
    private void fail(MinecraftServer s,Throwable e){done=true;var r=result(s);r.addProperty("failure",e.toString());write("failure",r);dev.primeants.PrimeAnts.LOGGER.error("Owned replay failed",e);s.halt(false);}
    private void write(String name,JsonObject row){try{Files.createDirectories(evidence);Files.writeString(evidence.resolve(name+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(row));}catch(Exception e){throw new RuntimeException(e);}}
}
