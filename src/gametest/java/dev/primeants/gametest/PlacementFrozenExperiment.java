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
        var d=JsonParser.parseString(Files.readString(declaration)).getAsJsonObject();require(d.get("mode").getAsString().equals("t16-frozen-v1"),"Frozen T16 declaration required");
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
                s.tickRateManager().requestGameToSprint(1000);
            }else require(setupTicks<300,"Declared readiness bound exhausted");
            return;
        }
        if(++observedTicks==1000)finish(s);
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
        var r=new JsonObject();r.addProperty("mode","t16-frozen-v1");r.addProperty("seed",Long.parseLong(System.getProperty("prime_ants.placementSeed")));r.addProperty("declaration_sha256",declarationHash);r.addProperty("world",s.getWorldPath(LevelResource.ROOT).toAbsolutePath().toString());r.addProperty("pid",ProcessHandle.current().pid());
        var all=new JsonArray();PlacementSettings.ALL_FULL.stream().sorted().forEach(all::add);r.add("all_generated_full",all);r.addProperty("total_full_chunks",all.size());r.addProperty("setup_ticks",setupTicks);r.addProperty("observed_ticks",observedTicks);r.addProperty("prior_full_chunks",Integer.getInteger("prime_ants.placementPriorFull",0));return r;
    }
    private void finish(MinecraftServer s) {
        var r=process(s);r.addProperty("biome_queries",survey.size());r.add("selected",sites);r.add("columns",PlacementTerrainTrace.COLUMNS);r.add("decisions",NaturalPlacement.get(level).decisions());r.add("insertion_snapshots",insertions);r.add("insertion_terrain_snapshots",PlacementTerrainTrace.INSERTION_TERRAIN);
        var observations=new JsonArray();firstAges.forEach((id,age)->{var actor=level.getEntity(id);var o=new JsonObject();o.addProperty("uuid",id.toString());o.addProperty("world_lookup",actor!=null);if(actor instanceof LasiusNigerEntity q) {o.addProperty("age_ticks",q.elapsedAgeTicks());o.addProperty("measured_entity_tick_advancement",q.elapsedAgeTicks()-age);o.addProperty("phase",q.founding().phase().name());o.addProperty("reason",q.founding().reason());o.addProperty("reserve",q.bodyReserve());o.addProperty("removed",q.founding().removed());o.addProperty("deposited",q.founding().deposited());o.addProperty("carried",q.founding().carried());o.addProperty("loaded_founding_ticks",q.founding().loadedTicks());
                o.addProperty("released",q.founding().released());o.addProperty("plugged",q.founding().plugged());
                var declared=new JsonArray();if(q.founding().plan()!=null)q.founding().plan().plants().forEach(b->declared.add(b.asLong()));o.add("declared_plants",declared);
                var cleared=new JsonArray();q.founding().removedPlants().forEach(b->cleared.add(b.asLong()));o.add("removed_plants",cleared);}observations.add(o);});r.add("early_founding",observations);
        var all=new JsonArray();PlacementSettings.ALL_FULL.stream().sorted().forEach(all::add);r.add("all_generated_full",all);r.addProperty("total_full_chunks",all.size());r.addProperty("full_chunks",PlacementSettings.NATIVE_FULL.size());
        require(survey.size()==0 && selected.size()==3 && all.size()+Integer.getInteger("prime_ants.placementPriorFull",0)<=150,"Declared bounded experiment required");
        require(NaturalPlacement.get(level).decisions().size()==selected.size(),"Only frozen selected candidates may generate placement records");
        write("result",r);done=true;s.halt(false);
    }
    private void fail(MinecraftServer s,Throwable e) {done=true;var r=process(s);r.addProperty("failure",e.toString());r.add("columns",PlacementTerrainTrace.COLUMNS);write("failure",r);dev.primeants.PrimeAnts.LOGGER.error("T16 placement experiment failed",e);s.halt(false);}
    private void write(String name,JsonObject r) {try{Files.createDirectories(evidence);Files.writeString(evidence.resolve(name+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(r));}catch(Exception e){throw new RuntimeException(e);}}
}
