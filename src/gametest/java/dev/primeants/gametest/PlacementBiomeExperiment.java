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

/** T15 biome-only frozen selection. Loading/tickets only; production generation and ticks own actors. */
final class PlacementBiomeExperiment {
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
        var d=JsonParser.parseString(Files.readString(declaration)).getAsJsonObject();require(d.get("mode").getAsString().equals("t15-biome-v1"),"Frozen T15 declaration required");
        level=s.getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.fromNamespaceAndPath("prime_ants_test","placement_native")));
        require(level!=null && level.getSeed()==Long.parseLong(System.getProperty("prime_ants.placementSeed")),"Actual seed/dimension must match");
        require(level.getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator,"Actual vanilla noise required");
        require(PlacementSettings.NATIVE_FULL.isEmpty(),"Survey must precede native FULL generation");
        write("process",process(s));
        var candidates=new ArrayList<ChunkPos>();
        for(int x=-128;x<=127;x++)for(int z=-128;z<=127;z++){var p=new ChunkPos(x,z);if(NaturalPlacement.selected(level,p))candidates.add(p);}
        candidates.sort(Comparator.comparingLong((ChunkPos p)->(long)p.x()*p.x()+(long)p.z()*p.z()).thenComparingInt(ChunkPos::x).thenComparingInt(ChunkPos::z));
        require(candidates.size()==4096,"Production lattice survey bounded to 4096 candidates");
        var resolver=level.getChunkSource().getGenerator().getBiomeSource().createCachingResolver(level.getChunkSource().randomState());
        for(var p:candidates) {
            var biome=resolver.getNoiseBiome(QuartPos.fromBlock(p.getMinBlockX()+7),QuartPos.fromBlock(80),QuartPos.fromBlock(p.getMinBlockZ()+7));
            var row=new JsonObject();row.addProperty("x",p.x());row.addProperty("z",p.z());row.addProperty("predicted_biome",biome.getRegisteredName());row.addProperty("predicted_eligible",biome.is(NaturalPlacement.BIOMES));survey.add(row);
            if(selected.size()<3 && biome.is(NaturalPlacement.BIOMES)) {selected.add(p);sites.add(row.deepCopy());}
        }
        require(PlacementSettings.NATIVE_FULL.isEmpty(),"Biome survey must not generate FULL chunks");
        for(var p:selected)for(int x=p.x()-2;x<=p.x()+2;x++)for(int z=p.z()-2;z<=p.z()+2;z++)PlacementSettings.DECLARED_FULL.add(new ChunkPos(x,z));
        var frozen=process(s);frozen.add("survey",survey);frozen.add("selected",sites.deepCopy());frozen.addProperty("queries",survey.size());frozen.addProperty("full_before_native_generation",PlacementSettings.ALL_FULL.size());
        var halo=new JsonArray();PlacementSettings.DECLARED_FULL.stream().sorted(Comparator.comparingInt(ChunkPos::x).thenComparingInt(ChunkPos::z)).forEach(p->halo.add(pair(p)));frozen.add("declared_full_halo",halo);write("selection-before-generation",frozen);
        // Ticket radius 2 means level 31 at center and FULL through Chebyshev radius 2.
        for(var p:selected)level.getChunkSource().addTicketWithRadius(PlacementSettings.TICKET,p,2);
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
                firstAges.put(id,q.elapsedAgeTicks());var o=new JsonObject();o.addProperty("uuid",id.toString());o.addProperty("first_age_ticks",q.elapsedAgeTicks());o.addProperty("world_lookup",level.getEntity(id)==q);o.addProperty("reserve",q.bodyReserve());o.addProperty("phase",q.founding().phase().name());o.addProperty("removed",q.founding().removed());o.addProperty("deposited",q.founding().deposited());o.addProperty("carried",q.founding().carried());o.add("placement",d.deepCopy());insertions.add(o);
            }
        });
    }
    private JsonObject process(MinecraftServer s) {
        var r=new JsonObject();r.addProperty("mode","t15-biome-v1");r.addProperty("seed",Long.parseLong(System.getProperty("prime_ants.placementSeed")));r.addProperty("declaration_sha256",declarationHash);r.addProperty("world",s.getWorldPath(LevelResource.ROOT).toAbsolutePath().toString());r.addProperty("pid",ProcessHandle.current().pid());r.addProperty("setup_ticks",setupTicks);r.addProperty("observed_ticks",observedTicks);r.addProperty("prior_full_chunks",Integer.getInteger("prime_ants.placementPriorFull",0));return r;
    }
    private void finish(MinecraftServer s) {
        var r=process(s);r.addProperty("biome_queries",survey.size());r.add("selected",sites);r.add("columns",PlacementTerrainTrace.COLUMNS);r.add("decisions",NaturalPlacement.get(level).decisions());r.add("insertion_snapshots",insertions);r.add("insertion_terrain_snapshots",PlacementTerrainTrace.INSERTION_TERRAIN);
        var observations=new JsonArray();firstAges.forEach((id,age)->{var actor=level.getEntity(id);var o=new JsonObject();o.addProperty("uuid",id.toString());o.addProperty("world_lookup",actor!=null);if(actor instanceof LasiusNigerEntity q) {o.addProperty("age_ticks",q.elapsedAgeTicks());o.addProperty("measured_entity_tick_advancement",q.elapsedAgeTicks()-age);o.addProperty("phase",q.founding().phase().name());o.addProperty("reason",q.founding().reason());o.addProperty("reserve",q.bodyReserve());o.addProperty("removed",q.founding().removed());o.addProperty("deposited",q.founding().deposited());o.addProperty("carried",q.founding().carried());}observations.add(o);});r.add("early_founding",observations);
        var all=new JsonArray();PlacementSettings.ALL_FULL.stream().sorted().forEach(all::add);r.add("all_generated_full",all);r.addProperty("total_full_chunks",all.size());r.addProperty("full_chunks",PlacementSettings.NATIVE_FULL.size());
        require(survey.size()==4096 && selected.size()<=3 && all.size()<=100 && PlacementSettings.NATIVE_FULL.size()<=75,"Declared bounded experiment required");
        require(NaturalPlacement.get(level).decisions().size()==selected.size(),"Only frozen selected candidates may generate placement records");
        write("result",r);done=true;s.halt(false);
    }
    private void fail(MinecraftServer s,Throwable e) {done=true;var r=process(s);r.addProperty("failure",e.toString());r.add("columns",PlacementTerrainTrace.COLUMNS);write("failure",r);dev.primeants.PrimeAnts.LOGGER.error("T15 placement experiment failed",e);s.halt(false);}
    private void write(String name,JsonObject r) {try{Files.createDirectories(evidence);Files.writeString(evidence.resolve(name+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(r));}catch(Exception e){throw new RuntimeException(e);}}
}
