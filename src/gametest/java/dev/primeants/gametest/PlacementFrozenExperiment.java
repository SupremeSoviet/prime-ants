package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.NaturalPlacement;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;

/** T16 imported frozen selection; zero biome survey. Loading/tickets only; production generation and ticks own actors. */
final class PlacementFrozenExperiment {
    private final boolean integration=PlacementSettings.integrationExperiment();
    private final String mode=integration?"t17-nectar-v1":"t16-frozen-v1";
    private final int observationLimit=integration?16000:1000;
    private ServerLevel level;
    private Path evidence;
    private final List<ChunkPos> selected=new ArrayList<>();
    private final JsonArray survey=new JsonArray(),sites=new JsonArray(),insertions=new JsonArray();
    private final Map<UUID,Long> firstAges=new HashMap<>();
    private int setupTicks,observedTicks;
    private boolean done,ready;
    private String declarationHash;
    void install() {
        evidence=Path.of(System.getProperty("prime_ants.placementEvidence"));
        ServerLifecycleEvents.SERVER_STARTED.register(s->{try{start(s);}catch(Throwable e){fail(s,e);}});
        ServerTickEvents.END_SERVER_TICK.register(s->{if(done || level==null)return;try{tick(s);}catch(Throwable e){fail(s,e);}});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->write("stopped",process(s)));
    }
    private void require(boolean condition,String why) {if(!condition)throw new IllegalStateException(why);}
    private void start(MinecraftServer s) throws Exception {
        var declaration=Path.of(System.getProperty("prime_ants.placementDeclaration"));
        declarationHash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(declaration)));
        var d=JsonParser.parseString(Files.readString(declaration)).getAsJsonObject();require(d.get("mode").getAsString().equals(mode),"Matching frozen declaration required");
        level=s.getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.fromNamespaceAndPath("prime_ants_test","placement_native")));
        require(level!=null && level.getSeed()==Long.parseLong(System.getProperty("prime_ants.placementSeed")),"Actual seed/dimension must match");
        require(level.getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator,"Actual vanilla noise required");
        require(PlacementSettings.NATIVE_FULL.isEmpty(),"Frozen import must precede native FULL generation");
        write("process",process(s));
        require(d.get("max_biome_queries_per_seed").getAsInt()==0,"Zero survey budget required");
        var imported=d.getAsJsonArray("imports").asList().stream().map(JsonElement::getAsJsonObject)
            .filter(r -> r.get("seed").getAsLong()==level.getSeed()).findFirst().orElseThrow();
        require(imported.get("dimension").getAsString().equals(level.dimension().identifier().toString()),"Imported dimension mismatch");
        var dimensionProof=Path.of(imported.get("dimension_evidence").getAsString());
        require(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(dimensionProof)))
            .equals(imported.get("dimension_evidence_sha256").getAsString()),"Historical dimension proof hash mismatch");
        var historical=JsonParser.parseString(Files.readString(dimensionProof)).getAsJsonObject().getAsJsonObject("ledgers")
            .getAsJsonObject("dimensions/prime_ants_test/placement_native/data/prime_ants/natural_placement.dat");
        require(historical.size()==3 && historical.asMap().values().stream().map(JsonElement::getAsJsonObject).allMatch(row ->
            row.get("seed").getAsLong()==level.getSeed() && row.get("dimension").getAsString().equals(level.dimension().identifier().toString())),"Original frozen dimension/seed proof mismatch");
        var frozen=FrozenPlacementSelection.read(Path.of(imported.get("path").getAsString()),imported.get("sha256").getAsString(),level.getSeed());
        for(int i=0;i<3;i++) {
            var row=frozen.getAsJsonArray("selected").get(i).getAsJsonObject();var expected=imported.getAsJsonArray("coordinates").get(i).getAsJsonArray();
            var p=new ChunkPos(row.get("x").getAsInt(),row.get("z").getAsInt());
            require(p.x()==expected.get(0).getAsInt() && p.z()==expected.get(1).getAsInt() && NaturalPlacement.selected(level,p),"Frozen coordinates/lattice mismatch");
            require(historical.asMap().values().stream().map(JsonElement::getAsJsonObject).anyMatch(r -> r.get("x").getAsInt()==p.x() && r.get("z").getAsInt()==p.z()),"Original dimension-proof coordinate mismatch");
            selected.add(p);sites.add(row.deepCopy());
        }
        // A recovery re-enters only this import path; the T15 survey class is never installed.
        for(var p:selected)for(int x=p.x()-3;x<=p.x()+3;x++)for(int z=p.z()-3;z<=p.z()+3;z++)PlacementSettings.DECLARED_FULL.add(new ChunkPos(x,z));
        var importedRecord=process(s);importedRecord.add("selected",sites.deepCopy());importedRecord.add("import",imported.deepCopy());importedRecord.addProperty("queries",0);importedRecord.addProperty("full_before_native_generation",PlacementSettings.ALL_FULL.size());
        var halo=new JsonArray();PlacementSettings.DECLARED_FULL.stream().sorted(Comparator.comparingInt(ChunkPos::x).thenComparingInt(ChunkPos::z)).forEach(p->halo.add(pair(p)));importedRecord.add("declared_full_halo",halo);write("selection-before-generation",importedRecord);
        // Diagnostic radius 3 keeps the complete cross-boundary work footprint entity-ticking.
        for(var p:selected)level.getChunkSource().addTicketWithRadius(PlacementSettings.TICKET,p,3);
        for(var p:selected)level.getChunkSource().getChunk(p.x(),p.z(),ChunkStatus.FULL,true);
    }
    private JsonArray pair(ChunkPos p) {var a=new JsonArray();a.add(p.x());a.add(p.z());return a;}
    private void tick(MinecraftServer s) {
        observeActors();
        if(!ready) {
            setupTicks++;
            if(selected.stream().allMatch(p->NaturalPlacement.ready(level,p))) {
                ready=true;
                for(int i=0;i<selected.size();i++) {
                    var p=selected.get(i);var row=sites.get(i).getAsJsonObject();var surface=new BlockPos(p.getMinBlockX()+7,level.getHeight(Heightmap.Types.WORLD_SURFACE,p.getMinBlockX()+7,p.getMinBlockZ()+7)-1,p.getMinBlockZ()+7);
                    row.addProperty("surface_y",surface.getY());row.addProperty("actual_surface_biome",level.getBiome(surface).getRegisteredName());row.addProperty("actual_surface_eligible",level.getBiome(surface).is(NaturalPlacement.BIOMES));
                }
                write("footprints-ready",process(s));
                // Keep asynchronous generation on normal ticks; sprint only the declared observation.
                s.tickRateManager().requestGameToSprint(observationLimit);
            }else require(setupTicks<300,"Declared readiness bound exhausted");
            return;
        }
        if(++observedTicks==observationLimit)finish(s);
    }
    private void observeActors() {
        NaturalPlacement.get(level).decisions().asMap().values().forEach(v->{var d=v.getAsJsonObject();var id=UUID.fromString(d.get("queen").getAsString());var actor=level.getEntity(id);
            if(actor instanceof LasiusNigerEntity q && !firstAges.containsKey(id)) {
                firstAges.put(id,q.elapsedAgeTicks());var o=new JsonObject();o.addProperty("uuid",id.toString());o.addProperty("first_age_ticks",q.elapsedAgeTicks());o.addProperty("world_lookup",level.getEntity(id)==q);o.addProperty("reserve",q.bodyReserve());o.addProperty("phase",q.founding().phase().name());o.addProperty("removed",q.founding().removed());o.addProperty("deposited",q.founding().deposited());o.addProperty("carried",q.founding().carried());o.addProperty("loaded_founding_ticks",q.founding().loadedTicks());
                o.addProperty("released",q.founding().released());o.addProperty("plugged",q.founding().plugged());
                var declared=new JsonArray();if(q.founding().plan()!=null)q.founding().plan().plants().forEach(b->declared.add(b.asLong()));o.add("declared_plants",declared);
                var cleared=new JsonArray();q.founding().removedPlants().forEach(b->cleared.add(b.asLong()));o.add("removed_plants",cleared);o.add("placement",d.deepCopy());insertions.add(o);
            }
        });
    }
    private JsonObject process(MinecraftServer s) {
        var r=new JsonObject();r.addProperty("mode",mode);r.addProperty("seed",Long.parseLong(System.getProperty("prime_ants.placementSeed")));r.addProperty("declaration_sha256",declarationHash);r.addProperty("world",s.getWorldPath(LevelResource.ROOT).toAbsolutePath().toString());r.addProperty("pid",ProcessHandle.current().pid());
        var all=new JsonArray();PlacementSettings.ALL_FULL.stream().sorted().forEach(all::add);r.add("all_generated_full",all);r.addProperty("total_full_chunks",all.size());r.addProperty("setup_ticks",setupTicks);r.addProperty("observed_ticks",observedTicks);r.addProperty("prior_full_chunks",Integer.getInteger("prime_ants.placementPriorFull",0));return r;
    }
    private void finish(MinecraftServer s) {
        var r=process(s);r.addProperty("biome_queries",survey.size());r.add("selected",sites);r.add("columns",PlacementTerrainTrace.COLUMNS);r.add("decisions",NaturalPlacement.get(level).decisions());r.add("insertion_snapshots",insertions);r.add("insertion_terrain_snapshots",PlacementTerrainTrace.INSERTION_TERRAIN);
        if(integration)r.add("colony_observation",colonyObservation());
        var observations=new JsonArray();firstAges.forEach((id,age)->{var actor=level.getEntity(id);var o=new JsonObject();o.addProperty("uuid",id.toString());o.addProperty("world_lookup",actor!=null);if(actor instanceof LasiusNigerEntity q) {o.addProperty("age_ticks",q.elapsedAgeTicks());o.addProperty("measured_entity_tick_advancement",q.elapsedAgeTicks()-age);o.addProperty("phase",q.founding().phase().name());o.addProperty("reason",q.founding().reason());o.addProperty("reserve",q.bodyReserve());o.addProperty("removed",q.founding().removed());o.addProperty("deposited",q.founding().deposited());o.addProperty("carried",q.founding().carried());o.addProperty("loaded_founding_ticks",q.founding().loadedTicks());
                o.addProperty("released",q.founding().released());o.addProperty("plugged",q.founding().plugged());
                var declared=new JsonArray();if(q.founding().plan()!=null)q.founding().plan().plants().forEach(b->declared.add(b.asLong()));o.add("declared_plants",declared);
                var cleared=new JsonArray();q.founding().removedPlants().forEach(b->cleared.add(b.asLong()));o.add("removed_plants",cleared);}observations.add(o);});r.add("early_founding",observations);
        var all=new JsonArray();PlacementSettings.ALL_FULL.stream().sorted().forEach(all::add);r.add("all_generated_full",all);r.addProperty("total_full_chunks",all.size());r.addProperty("full_chunks",PlacementSettings.NATIVE_FULL.size());
        require(survey.size()==0 && selected.size()==3 && all.size()+Integer.getInteger("prime_ants.placementPriorFull",0)<=150,"Declared bounded experiment required");
        require(NaturalPlacement.get(level).decisions().size()==selected.size(),"Only frozen selected candidates may generate placement records");
        write("result",r);done=true;s.halt(false);
    }
    private JsonObject colonyObservation(){
        var r=new JsonObject();var colonies=new JsonArray();var nectar=dev.primeants.worker.FlowerNectar.get(level);
        r.add("source_availability_not_inventory",dev.primeants.worker.FlowerNectar.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,nectar).getOrThrow());
        var sources=new JsonArray();nectar.harvestedSources().forEach((p,count)->{var o=new JsonObject();o.addProperty("position",p.asLong());o.addProperty("state",level.getBlockState(p).toString());o.addProperty("native_vegetation",dev.primeants.founding.NativeVegetation.get(level).eligible(level,p));o.addProperty("harvests",count);o.addProperty("cooldown",nectar.remaining(p));sources.add(o);});r.add("sources",sources);
        long held=0,cached=0,consumed=0;
        for(var id:firstAges.keySet())if(level.getEntity(id) instanceof LasiusNigerEntity q){
            var o=new JsonObject();o.addProperty("queen",id.toString());o.addProperty("founding_phase",q.founding().phase().name());o.addProperty("reason",q.founding().reason());o.addProperty("lifecycle",q.founding().lifecycle().name());o.addProperty("ready",q.founding().ready());o.addProperty("body_reserve",q.bodyReserve());
            o.addProperty("removed",q.founding().removed());o.addProperty("deposited",q.founding().deposited());o.addProperty("carried_soil",q.founding().carried());o.addProperty("plugged",q.founding().plugged());o.addProperty("released",q.founding().released());o.addProperty("prepared",q.founding().converted());
            var plants=new JsonArray();q.founding().removedPlants().forEach(p->plants.add(p.asLong()));o.add("cleared_plants",plants);
            var workers=new JsonArray();var p=q.founding().plan();
            if(p!=null){
                o.addProperty("entrance",p.entrance().asLong());o.addProperty("direction",p.direction().getName());
                var declared=new JsonArray();p.plants().forEach(b->declared.add(b.asLong()));o.add("declared_plants",declared);
                for(var w:level.getEntitiesOfClass(LasiusNigerEntity.class,new net.minecraft.world.phys.AABB(p.chamber()).inflate(20),a->a.isAlive()&&id.equals(a.queenId()))){
                    var row=new JsonObject();row.addProperty("uuid",w.getUUID().toString());row.addProperty("brood",w.broodId().toString());row.addProperty("callow",w.isCallow());row.addProperty("age",w.elapsedAgeTicks());row.addProperty("position",w.position().toString());row.addProperty("phase",w.workerTasks().phase().name());row.addProperty("reason",w.workerTasks().reason());row.addProperty("cargo",w.getMainHandItem().toString());row.addProperty("opened",w.workerTasks().opened());row.addProperty("mound_units",w.workerTasks().placed());workers.add(row);
                    if(w.getMainHandItem().is(dev.primeants.item.AntItems.FLOWER_NECTAR))held+=w.getMainHandItem().getCount();
                }
                if(level.getBlockEntity(p.nursery()) instanceof dev.primeants.brood.BroodPile brood){
                    var original=new JsonArray();brood.original().stream().map(UUID::toString).sorted().forEach(original::add);o.add("first_clutch",original);
                    var current=new JsonArray();for(var b:brood.records()){var row=new JsonObject();row.addProperty("uuid",b.id().toString());row.addProperty("stage",b.stage().name());row.addProperty("founding",b.founding());row.addProperty("sugar",b.nutrition().sugar());row.addProperty("protein",b.nutrition().protein());row.addProperty("nectar_consumed",b.nutrition().nectar());current.add(row);}o.add("brood",current);consumed+=brood.consumedNectar();
                }
                if(level.getBlockEntity(p.cache()) instanceof dev.primeants.worker.NestCache cache){var stacks=new JsonArray();cache.contents().forEach(stack->{stacks.add(stack.toString());});o.add("cache",stacks);cached+=cache.contents().stream().filter(stack->stack.is(dev.primeants.item.AntItems.FLOWER_NECTAR)).mapToInt(net.minecraft.world.item.ItemStack::getCount).sum();}
            }
            o.add("workers",workers);o.addProperty("queen_nectar_consumed",q.nutrition().nectar());o.addProperty("queen_sugar",q.nutrition().sugar());o.addProperty("queen_protein",q.nutrition().protein());consumed+=q.nutrition().nectar();colonies.add(o);
        }
        r.add("colonies",colonies);long world=0;var drops=new JsonArray();
        for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.item.ItemEntity i&&i.isAlive()&&i.getItem().is(dev.primeants.item.AntItems.FLOWER_NECTAR)){world+=i.getItem().getCount();var row=new JsonObject();row.addProperty("uuid",i.getUUID().toString());row.addProperty("position",i.position().toString());row.addProperty("cargo",i.getItem().toString());drops.add(row);}
        long custody=dev.primeants.worker.TransferCustody.get(level).contents().stream().filter(t->t.stack().is(dev.primeants.item.AntItems.FLOWER_NECTAR)).mapToInt(t->t.stack().getCount()).sum();
        r.add("world_drops",drops);r.addProperty("harvests",nectar.harvests());r.addProperty("held",held);r.addProperty("cached",cached);r.addProperty("world",world);r.addProperty("custody",custody);r.addProperty("consumed",consumed);r.addProperty("physical_stock",held+cached+world+custody);r.addProperty("conserved",nectar.harvests()==held+cached+world+custody+consumed);return r;
    }
    private void fail(MinecraftServer s,Throwable e) {done=true;var r=process(s);r.addProperty("failure",e.toString());r.add("columns",PlacementTerrainTrace.COLUMNS);write("failure",r);dev.primeants.PrimeAnts.LOGGER.error("T16 placement experiment failed",e);s.halt(false);}
    private void write(String name,JsonObject r) {try{Files.createDirectories(evidence);Files.writeString(evidence.resolve(name+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(r));}catch(Exception e){throw new RuntimeException(e);}}
}
