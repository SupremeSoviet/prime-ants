package dev.primeants.gametest;

import com.google.gson.*;
import com.mojang.serialization.*;
import dev.primeants.brood.*;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

/** Read-only T27 natural-source observer. No spawning, food supplies, sprinting or biological edits. */
public final class T27ReleaseHarness implements ModInitializer {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static T27ReleaseHarness active;
    private final String phase=System.getProperty("prime_ants.restartPhase","");
    private final Path evidence=Path.of(System.getProperty("prime_ants.restartEvidence","build/restart-evidence"));
    private final AABB bounds=new AABB(64,-64,32,320,160,192);
    private final Map<String,JsonElement> firstActors=new TreeMap<>(),firstPiles=new TreeMap<>();
    private final Map<UUID,Long> previousAge=new HashMap<>();
    private final List<JsonObject> rows=new ArrayList<>();
    private int ticks,loaded;
    private boolean done;
    private long warmStart,firstMeasured,lastMeasured;
    private JsonObject declaration;
    private Set<UUID> expected;

    @Override public void onInitialize() {
        if(!phase.startsWith("t27-"))return;
        active=this;
        try { declaration=JsonParser.parseString(Files.readString(Path.of(System.getProperty("prime_ants.releaseSource")))).getAsJsonObject(); }
        catch(Exception e){throw new IllegalStateException("Explicit T27 source declaration required",e);}
        expected=new HashSet<>();declaration.getAsJsonArray("adult_ids").forEach(x->expected.add(UUID.fromString(x.getAsString())));
        var ticket=Registry.register(BuiltInRegistries.TICKET_TYPE,Identifier.fromNamespaceAndPath("prime_ants_test","t27_release"),new TicketType(0,14));
        ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{
            if(level==level(level.getServer()) && entity instanceof LasiusNigerEntity a && expected.contains(a.getUUID()))
                firstActors.putIfAbsent(a.getUUID().toString(),actor(a,level));
        });
        ServerLifecycleEvents.SERVER_STARTED.register(s->{
            write(phase+"-process",process(s));var l=level(s);
            for(var home:declaration.getAsJsonArray("homes")) {
                var a=home.getAsJsonArray();var p=new BlockPos(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt());
                var chunk=ChunkPos.containing(p);l.getChunkSource().addTicketWithRadius(ticket,chunk,4);l.getChunk(chunk.x(),chunk.z());
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(s->{
            if(done)return;
            try{observe(s);}catch(Throwable e){done=true;var j=process(s);j.addProperty("failure",e.toString());write(phase+"-failure",j);dev.primeants.PrimeAnts.LOGGER.error("T27 release observer failure",e);s.halt(false);}
        });
        // No world/chunk/entity reads after shutdown. This event records only process status.
        ServerLifecycleEvents.SERVER_STOPPED.register(s->write(phase+"-stopped",process(s)));
    }
    public static void beforePileTick(BroodPile pile,ServerLevel l) {
        if(active==null||active.done||active.firstPiles.size()>=active.declaration.getAsJsonArray("queen_ids").size())return;
        if(active.declaration.getAsJsonArray("queen_ids").asList().stream().anyMatch(x->x.getAsString().equals(String.valueOf(pile.queenId()))))
            active.firstPiles.putIfAbsent(pile.getBlockPos().toShortString(),tag(pile.saveWithFullMetadata(l.registryAccess())));
    }
    private static JsonElement tag(net.minecraft.nbt.CompoundTag tag){return NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE,tag);}
    private static JsonElement actor(Entity a,ServerLevel l){var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());require(a.save(out),"Real entity save failed");return tag(out.buildResult());}
    private ServerLevel level(MinecraftServer s){var l=s.getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.parse(declaration.get("dimension").getAsString())));require(l!=null,"Declared source dimension unavailable");return l;}
    private static void require(boolean b,String message){if(!b)throw new IllegalStateException(message);}
    private void write(String name,JsonObject j){try{Files.createDirectories(evidence);Files.writeString(evidence.resolve(name+".json"),JSON.toJson(j),java.nio.charset.StandardCharsets.UTF_8);}catch(Exception e){throw new RuntimeException(e);}}
    private JsonObject process(MinecraftServer s){
        var j=new JsonObject();j.addProperty("phase",phase);j.addProperty("pid",ProcessHandle.current().pid());j.addProperty("tick",s.getTickCount());j.addProperty("world",s.getWorldPath(LevelResource.ROOT).toAbsolutePath().toString());
        j.addProperty("java",System.getProperty("java.version"));j.addProperty("vm",System.getProperty("java.vm.name"));j.addProperty("os",System.getProperty("os.name")+" "+System.getProperty("os.arch"));j.addProperty("processors",Runtime.getRuntime().availableProcessors());j.addProperty("max_heap",Runtime.getRuntime().maxMemory());
        j.add("jvm_args",JSON.toJsonTree(java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments()));j.add("source",declaration.deepCopy());return j;
    }
    private <T> JsonElement data(Codec<T> codec,T value){return codec.encodeStart(JsonOps.INSTANCE,value).getOrThrow();}
    private JsonObject snapshot(MinecraftServer s,List<LasiusNigerEntity> ants){
        var l=level(s);var j=process(s);var actors=new JsonObject();ants.forEach(a->actors.add(a.getUUID().toString(),actor(a,l)));j.add("actors",actors);
        var piles=new JsonObject();var caches=new JsonObject();
        for(var a:ants)if(a.form()==AntForm.QUEEN&&a.founding().plan()!=null){var plan=a.founding().plan();
            if(l.getBlockEntity(plan.nursery()) instanceof BroodPile p)piles.add(p.getBlockPos().toShortString(),tag(p.saveWithFullMetadata(l.registryAccess())));
            if(l.getBlockEntity(plan.cache()) instanceof NestCache c)caches.add(c.getBlockPos().toShortString(),tag(c.saveWithFullMetadata(l.registryAccess())));
        }j.add("piles",piles);j.add("caches",caches);
        j.add("members",data(ColonyMembers.CODEC,ColonyMembers.get(l)));j.add("adult_history",data(AdultHistory.CODEC,AdultHistory.get(l)));j.add("brood_history",data(BroodHistory.CODEC,BroodHistory.get(l)));
        j.add("transfers",data(TransferCustody.CODEC,TransferCustody.get(l)));j.add("terrain",data(ColonyTerrain.CODEC,ColonyTerrain.get(l)));j.add("plugs",data(ColonyPlugs.CODEC,ColonyPlugs.get(l)));j.add("expansion",data(NestExpansion.CODEC,NestExpansion.get(l)));j.add("alarms",data(ColonyAlarm.CODEC,ColonyAlarm.get(l)));
        j.add("first_actor_load",JSON.toJsonTree(firstActors));j.add("first_pile_tick",JSON.toJsonTree(firstPiles));return j;
    }
    private void finish(MinecraftServer s,List<LasiusNigerEntity> ants){
        var frozen=snapshot(s,ants);write(phase+"-before-close",frozen);done=true;
        require(s.saveEverything(false,true,true),"Normal whole-world flush failed");s.halt(false);
    }
    private void observe(MinecraftServer s){
        long observerStart=System.nanoTime();ticks++;require(ticks<4000,"T27 bounded loading/measurement failed");var l=level(s);
        require(!s.tickRateManager().isSprinting()&&!s.tickRateManager().isFrozen()&&s.tickRateManager().tickrate()==20,"Only ordinary unfrozen 20 TPS allowed");
        var ants=l.getEntitiesOfClass(LasiusNigerEntity.class,bounds,a->a.isAlive()&&!a.isRemoved());
        var ids=new HashSet<UUID>();int ticking=0,queens=0,workers=0;var ages=new JsonObject();
        for(var a:ants){require(ids.add(a.getUUID()),"Duplicate loaded adult identity");require(!a.isNoAi(),"AI may not be paused");ages.addProperty(a.getUUID().toString(),a.elapsedAgeTicks());
            Long before=previousAge.put(a.getUUID(),a.elapsedAgeTicks());boolean advanced=before!=null&&a.elapsedAgeTicks()==before+1&&l.isPositionEntityTicking(a.blockPosition());
            if(advanced){ticking++;if(a.form()==AntForm.QUEEN)queens++;else workers++;}
        }
        if(!ids.containsAll(expected)||ticking<20||queens<2){
            if(ticks==20){var j=process(s);j.addProperty("visible_adults",ants.size());j.addProperty("ticking_adults",ticking);j.addProperty("ticking_queens",queens);var missing=new HashSet<>(expected);missing.removeAll(ids);j.add("missing_ids",JSON.toJsonTree(missing));j.add("ages",ages);write(phase+"-loading-diagnosis",j);}
            require(ticks<100||loaded>0,"Targeted loading check failed; inspect loading-diagnosis before workload");return;
        }
        loaded++;
        if(loaded==1){warmStart=System.nanoTime();write(phase+"-loaded",snapshot(s,ants));}
        if(!phase.equals("t27-measure")){if(loaded==100)finish(s,ants);return;}
        if(loaded<=600)return;
        var row=new JsonObject();row.addProperty("tick",s.getTickCount());row.addProperty("loaded_tick",loaded);row.addProperty("queens",queens);row.addProperty("workers",workers);row.addProperty("ticking_adults",ticking);row.add("ages",ages);
        row.addProperty("native_tick_work_ns",s.getTickTimesNanos()[s.getTickCount()%100]);row.addProperty("tickrate",s.tickRateManager().tickrate());row.addProperty("sprint",s.tickRateManager().isSprinting());
        row.addProperty("loaded_chunks",l.getChunkSource().getLoadedChunksCount());long entityCount=0;for(var entity:l.getAllEntities())entityCount++;row.addProperty("vanilla_and_mod_entities",entityCount);
        long now=System.nanoTime();if(rows.isEmpty())firstMeasured=now;lastMeasured=now;row.addProperty("end_time_ns",now);row.addProperty("observer_ns",now-observerStart);rows.add(row);
        if(rows.size()==2400){var result=process(s);result.addProperty("warmup_ticks",600);result.addProperty("measured_ticks",2400);result.addProperty("warmup_wall_seconds",(firstMeasured-warmStart)/1e9);result.addProperty("measured_interval_seconds",(lastMeasured-firstMeasured)/1e9);result.addProperty("observed_tps",2399e9/(lastMeasured-firstMeasured));
            result.addProperty("timing_boundary","MinecraftServer native tickServer entry to tallying; excludes scheduled sleep and Fabric END observer. Population/age verification is at END after the tally. Observer work is separately recorded and affects cadence.");result.add("samples",JSON.toJsonTree(rows));write("t27-performance",result);finish(s,ants);}
    }
}
