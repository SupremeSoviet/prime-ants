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

/** Untouched closed-checkpoint owned-copy observer: diagnostic tickets, real ticks and receipts only. */
final class PlacementReplayExperiment {
    private ServerLevel level;
    private Path evidence;
    private JsonObject declaration;
    private int ticks;
    private int limit;
    private boolean followup;
    private boolean done,initial;
    private JsonObject closedResult;
    private final JsonArray events=new JsonArray(),snapshots=new JsonArray();
    private final Map<UUID,Long> receipts=new HashMap<>();
    private final Map<UUID,Integer> cargo=new HashMap<>(),durations=new HashMap<>();
    private final Map<UUID,BlockPos> sources=new HashMap<>();
    void install(){
        evidence=Path.of(System.getProperty("prime_ants.placementEvidence"));
        ServerLifecycleEvents.SERVER_STARTED.register(s->{try{start(s);}catch(Throwable e){fail(s,e);}});
        ServerTickEvents.START_SERVER_TICK.register(s->{if(done||level==null)return;try{beforeTick(s);}catch(Throwable e){fail(s,e);}});
        ServerTickEvents.END_SERVER_TICK.register(s->{if(done||level==null)return;try{afterTick(s);}catch(Throwable e){fail(s,e);}});
        ServerLifecycleEvents.SERVER_STOPPING.register(s->closedResult=result(s));
        ServerLifecycleEvents.SERVER_STOPPED.register(s->write("stopped",closedResult));
    }
    void installClosureProbe(ServerLevel testLevel,Path destination){
        level=testLevel;evidence=destination;declaration=new JsonObject();declaration.add("queens",new JsonArray());declaration.add("initial_nurseries",new JsonArray());followup=true;
        ServerLifecycleEvents.SERVER_STOPPING.register(s->closedResult=result(s));
        ServerLifecycleEvents.SERVER_STOPPED.register(s->write("stopped",closedResult));
    }
    private void start(MinecraftServer s)throws Exception{
        declaration=JsonParser.parseString(Files.readString(Path.of(System.getProperty("prime_ants.placementDeclaration")))).getAsJsonObject();
        limit=declaration.get("server_tick_limit").getAsInt();followup=declaration.get("mode").getAsString().equals("t19-replay-v1");
        level=s.getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.fromNamespaceAndPath("prime_ants_test","placement_native")));
        require(level!=null&&level.getSeed()==2026100501L,"Original seed/dimension required");
        write("process",result(s));
        for(var v:declaration.getAsJsonArray("selected")){var a=v.getAsJsonArray();var p=new ChunkPos(a.get(0).getAsInt(),a.get(1).getAsInt());level.getChunkSource().addTicketWithRadius(PlacementSettings.TICKET,p,3);level.getChunkSource().getChunk(p.x(),p.z(),ChunkStatus.FULL,true);}
        s.tickRateManager().requestGameToSprint(limit);
    }
    private List<LasiusNigerEntity> ants(){var out=new ArrayList<LasiusNigerEntity>();for(var e:level.getAllEntities())if(e instanceof LasiusNigerEntity a&&a.isAlive())out.add(a);return out;}
    private void beforeTick(MinecraftServer s){
        var ants=ants();
        if(!initial&&ants.size()==8){
            var expected=new HashSet<String>();declaration.getAsJsonArray("queens").forEach(v->expected.add(v.getAsString()));declaration.getAsJsonArray("workers").forEach(v->expected.add(v.getAsString()));
            require(ants.stream().map(a->a.getUUID().toString()).collect(java.util.stream.Collectors.toSet()).equals(expected),"Original two queens/six worker identities required");
            if(followup){
                for(var v:declaration.getAsJsonArray("initial_actors")){var row=v.getAsJsonObject();var a=ants.stream().filter(e->e.getUUID().toString().equals(row.get("uuid").getAsString())).findFirst().orElseThrow();
                    require(a.elapsedAgeTicks()>=row.get("age").getAsLong()&&a.elapsedAgeTicks()<=row.get("age").getAsLong()+ticks,"Original persisted adult clock; no renewal");
                    require(a.adultLife().lifespan()==144000&&a.adultLife().grace()==24000&&a.broodNeglectGrace()==24000&&a.cocoonWaitingBound()==24000,"Production life/expiry policies");
                    require(a.nutrition().nectar()>=row.getAsJsonObject("nutrition").get("Nectar").getAsLong()&&a.nutrition().chickens()==0&&a.nutrition().flesh()==0&&a.bodyReserve()==row.get("reserve").getAsLong(),"Original receipts and exhausted reserve preserved");
                }
                require(FlowerNectar.get(level).harvests()>=5,"T18 nonzero source histories preserved");
            }else{
                require(ants.stream().allMatch(a->a.getMainHandItem().isEmpty()&&a.nutrition().consumedUnits()==0&&a.nutrition().sugar()==0&&a.nutrition().protein()==0),"Original zero food/receipts required");
                for(var a:ants)if(a.form()==AntForm.QUEEN){var p=a.founding().plan();require(p!=null&&a.bodyReserve()==0&&a.founding().ready(),"Original settled/open exhausted queen");require(!(level.getBlockEntity(p.cache()) instanceof NestCache n)||n.size()==0,"Zero cached food");require(level.getBlockEntity(p.nursery()) instanceof dev.primeants.brood.BroodPile b&&b.records().isEmpty()&&b.consumedFood()==0,"Original completed zero-food clutch");}
                require(FlowerNectar.get(level).harvests()==0,"Original zero native harvests");
            }
            initial=true;write("initial",snapshot());
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
        if(ticks==limit){require(initial,"Initial verification must precede observation");write("result",result(s));done=true;s.halt(false);}
    }
    private JsonObject event(String kind,LasiusNigerEntity a){var row=new JsonObject();row.addProperty("kind",kind);row.addProperty("server_tick",ticks);row.addProperty("actor",a.getUUID().toString());row.addProperty("position",a.position().toString());return row;}
    private JsonObject snapshot(){
        var out=new JsonObject();out.addProperty("tick",ticks);out.addProperty("harvests",FlowerNectar.get(level).harvests());var rows=new JsonArray();
        for(var a:ants()){
            var row=event("live",a);row.addProperty("form",a.form().name());row.addProperty("age",a.elapsedAgeTicks());row.addProperty("fasting",a.adultLife().fasting());row.addProperty("lifespan",a.adultLife().lifespan());row.addProperty("phase",a.workerTasks().phase().name());row.addProperty("reason",a.workerTasks().reason());row.addProperty("cargo",a.getMainHandItem().toString());row.addProperty("nectar_receipts",a.nutrition().nectar());row.addProperty("sugar",a.nutrition().sugar());row.addProperty("protein",a.nutrition().protein());row.addProperty("spent_sugar",a.nutrition().spentSugar());row.addProperty("spent_protein",a.nutrition().spentProtein());row.addProperty("protein_receipts",a.nutrition().chickens()+a.nutrition().flesh());row.addProperty("fasting_grace",a.adultLife().grace());row.addProperty("colony",a.colonyIdentity()==null?"":a.colonyIdentity().toString());row.addProperty("reserve",a.bodyReserve());row.addProperty("queen_ready",a.form()==AntForm.QUEEN&&a.founding().ready());row.addProperty("navigation_done",a.getNavigation().isDone());row.addProperty("ground",a.onGround());
            var source=a.workerTasks().flowerSource();if(source!=null){row.addProperty("source",source.asLong());row.addProperty("source_state",level.getBlockState(source).toString());row.addProperty("source_ticking",level.isPositionEntityTicking(source));row.addProperty("source_ready",FlowerNectar.get(level).ready(level,source));row.addProperty("reach_visibility",WorkerTasks.reaches(level,a,net.minecraft.world.phys.Vec3.atCenterOf(source)));row.addProperty("action_ticks",a.workerTasks().harvestingTicks());}
            rows.add(row);
        }
        out.add("actors",rows);if(followup){out.add("colonies",colonies());out.add("physical_food",physicalFood());out.add("adult_terminal",AdultHistory.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,AdultHistory.get(level)).getOrThrow());}out.add("source_ledger",FlowerNectar.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,FlowerNectar.get(level)).getOrThrow());return out;
    }
    private JsonArray colonies(){
        var rows=new JsonArray();if(declaration==null)return rows;
        for(var value:declaration.getAsJsonArray("queens")){
            UUID id=UUID.fromString(value.getAsString());var row=new JsonObject();row.addProperty("queen",id.toString());var actor=level.getEntity(id);var queen=actor instanceof LasiusNigerEntity q&&q.isAlive()?q:null;
            row.addProperty("queen_alive",queen!=null);row.addProperty("queen_lookup",actor!=null);if(queen!=null){row.addProperty("queen_ready",queen.founding().ready());row.addProperty("founding_condition",queen.founding().reason());row.addProperty("fasting",queen.adultLife().fasting());}
            var members=ColonyMembers.get(level).members(id);row.addProperty("occupied_workers",ColonyMembers.get(level).occupied(id));row.addProperty("dead_workers",members.stream().filter(ColonyMembers.Member::dead).count());row.addProperty("loaded_workers",ants().stream().filter(a->id.equals(a.queenId())).count());
            long nectar=0,protein=0;for(var a:ants())if(id.equals(a.colonyIdentity())){nectar+=a.nutrition().nectar();protein+=a.nutrition().chickens()+a.nutrition().flesh();}
            for(var receipt:AdultHistory.get(level).records().values()){var r=JsonParser.parseString(receipt).getAsJsonObject();if(r.get("queen").getAsString().equals(id.toString())){nectar+=r.getAsJsonObject("nutrition").get("nectar").getAsLong();protein+=r.getAsJsonObject("nutrition").get("chickens").getAsLong()+r.getAsJsonObject("nutrition").get("flesh").getAsLong();}}
            net.minecraft.core.BlockPos nursery=null;for(var v:declaration.getAsJsonArray("initial_nurseries")){var b=v.getAsJsonObject();if(b.get("Queen").getAsString().equals(id.toString()))nursery=new BlockPos(b.get("x").getAsInt(),b.get("y").getAsInt(),b.get("z").getAsInt());}
            if(nursery!=null&&level.getBlockEntity(nursery) instanceof dev.primeants.brood.BroodPile b){row.addProperty("brood_condition",b.condition());row.addProperty("viable_brood",b.records().size());row.addProperty("expired_brood",b.expired().size());row.addProperty("original_clutch",b.original().size());row.addProperty("emerged_brood",b.consumed().size());row.addProperty("stage_duration",b.stageDuration());nectar+=b.consumedNectar();protein+=b.consumedChickens()+b.consumedFlesh();}
            row.addProperty("nectar_intake_receipts",nectar);row.addProperty("protein_intake_receipts",protein);rows.add(row);
        }return rows;
    }
    private JsonObject physicalFood(){
        var rows=new JsonObject();for(String name:List.of("nectar","chicken","flesh","apple","berries")){
            var item=switch(name){case "nectar"->dev.primeants.item.AntItems.FLOWER_NECTAR;case "chicken"->net.minecraft.world.item.Items.CHICKEN;case "flesh"->net.minecraft.world.item.Items.ROTTEN_FLESH;case "apple"->net.minecraft.world.item.Items.APPLE;default->net.minecraft.world.item.Items.SWEET_BERRIES;};
            long world=0,cargo=0,cache=0,custody=0;for(var e:level.getAllEntities()){if(e instanceof net.minecraft.world.entity.item.ItemEntity i&&i.isAlive()&&i.getItem().is(item))world+=i.getItem().getCount();if(e instanceof LasiusNigerEntity a&&a.isAlive()&&a.getMainHandItem().is(item))cargo+=a.getMainHandItem().getCount();}
            for(var v:declaration.getAsJsonArray("initial_nurseries")){var b=v.getAsJsonObject();var queen=level.getEntity(UUID.fromString(b.get("Queen").getAsString()));if(queen instanceof LasiusNigerEntity q&&q.founding().plan()!=null&&level.getBlockEntity(q.founding().plan().cache()) instanceof NestCache n)cache+=n.contents().stream().filter(s->s.is(item)).mapToInt(net.minecraft.world.item.ItemStack::getCount).sum();}
            custody=TransferCustody.get(level).contents().stream().filter(t->t.stack().is(item)).mapToInt(t->t.stack().getCount()).sum();var row=new JsonObject();row.addProperty("world",world);row.addProperty("cargo",cargo);row.addProperty("cache",cache);row.addProperty("custody",custody);row.addProperty("physical_total",world+cargo+cache+custody);rows.add(name,row);
        }return rows;
    }
    private JsonObject result(MinecraftServer s){var r=new JsonObject();r.addProperty("mode",System.getProperty("prime_ants.placementMode"));r.addProperty("seed",2026100501L);r.addProperty("world",s.getWorldPath(LevelResource.ROOT).toAbsolutePath().toString());r.addProperty("server_ticks",ticks);r.addProperty("initial_verified",initial);r.addProperty("server_tick_limit",limit);r.addProperty("sprint",true);r.addProperty("production_tickets",false);r.addProperty("biome_queries",0);r.addProperty("budget_used",PlacementSettings.replayBudget());r.addProperty("total_full_chunks",PlacementSettings.EXISTING_FULL.size()+(int)PlacementSettings.ALL_FULL.stream().filter(p->!PlacementSettings.EXISTING_FULL.contains(p)).count());
        var generated=new JsonArray();PlacementSettings.ALL_FULL.stream().filter(p->!PlacementSettings.EXISTING_FULL.contains(p)).sorted().forEach(generated::add);r.add("new_full",generated);r.addProperty("new_full_count",generated.size());r.add("events",events);r.add("snapshots",snapshots);if(level!=null)r.add("final",snapshot());return r;}
    private void require(boolean ok,String why){if(!ok)throw new IllegalStateException(why);}
    private void fail(MinecraftServer s,Throwable e){done=true;var r=result(s);r.addProperty("failure",e.toString());write("failure",r);dev.primeants.PrimeAnts.LOGGER.error("Owned replay failed",e);s.halt(false);}
    private void write(String name,JsonObject row){try{Files.createDirectories(evidence);Files.writeString(evidence.resolve(name+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(row));}catch(Exception e){throw new RuntimeException(e);}}
}
