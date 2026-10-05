package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.NaturalPlacement;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;

/** Scoped native-ticket diagnostic only. No actor factory, origin grant or terrain edit. */
public final class PlacementDiagnosticHarness implements ModInitializer {
    private ServerLevel level;
    private int ticks,eligible;
    private boolean done;
    private final JsonArray chunks=new JsonArray();
    private Path evidence;
    @Override public void onInitialize() {
        if(System.getProperty("prime_ants.placementSeed")==null)return;
        if(PlacementSettings.biomeExperiment()) {new PlacementBiomeExperiment().install();return;}
        evidence=Path.of(System.getProperty("prime_ants.placementEvidence"));
        ServerLifecycleEvents.SERVER_STARTED.register(s->{
            try {
                level=s.getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.fromNamespaceAndPath("prime_ants_test","placement_native")));
                require(level!=null && level.getSeed()==Long.parseLong(System.getProperty("prime_ants.placementSeed")),"Actual declared native dimension/seed required");
                require(level.getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator,"Vanilla noise generator required");
                write("process",process(s));
                for(int x=0;x<8;x++)for(int z=0;z<8;z++)level.getChunkSource().getChunk(x,z,ChunkStatus.FULL,true);
                for(int x=0;x<8;x++)for(int z=0;z<8;z++) {
                    var p=new ChunkPos(x,z);var surface=new BlockPos(x*16+7,level.getHeight(Heightmap.Types.WORLD_SURFACE,x*16+7,z*16+7)-1,z*16+7);
                    var row=new JsonObject();row.addProperty("x",x);row.addProperty("z",z);row.addProperty("biome",level.getBiome(surface).getRegisteredName());row.addProperty("eligible",level.getBiome(surface).is(NaturalPlacement.BIOMES));row.addProperty("selected",NaturalPlacement.selected(level,p));chunks.add(row);
                    if(level.getBiome(surface).is(NaturalPlacement.BIOMES))eligible++;
                    if(NaturalPlacement.selected(level,p))level.getChunkSource().addTicketWithRadius(PlacementSettings.TICKET,p,2);
                }
                s.tickRateManager().requestGameToSprint(1100);
            }catch(Throwable e){fail(s,e);}
        });
        ServerTickEvents.END_SERVER_TICK.register(s->{
            if(done || level==null)return;
            try {if(++ticks==1000)finish(s);}catch(Throwable e){fail(s,e);}
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(s->write("stopped",process(s)));
    }
    private void require(boolean condition,String reason) {if(!condition)throw new IllegalStateException(reason);}
    private JsonObject process(MinecraftServer s) {
        var r=new JsonObject();r.addProperty("label","placement diagnostic");r.addProperty("pid",ProcessHandle.current().pid());r.addProperty("seed",Long.parseLong(System.getProperty("prime_ants.placementSeed")));r.addProperty("world",s.getWorldPath(LevelResource.ROOT).toAbsolutePath().toString());r.addProperty("ticks",ticks);return r;
    }
    private void write(String name,JsonObject result) {
        try {Files.createDirectories(evidence);Files.writeString(evidence.resolve(name+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));}catch(Exception e){throw new RuntimeException(e);}
    }
    private void fail(MinecraftServer s,Throwable e) {done=true;var r=process(s);r.addProperty("failure",e.toString());write("failure",r);dev.primeants.PrimeAnts.LOGGER.error("Placement diagnostic failed",e);s.halt(false);}
    private void finish(MinecraftServer s) {
        var r=process(s);r.addProperty("full_chunks",PlacementSettings.NATIVE_FULL.size());r.addProperty("total_full_chunks",PlacementSettings.ALL_FULL.size());r.addProperty("candidate_region_chunks",64);r.addProperty("biome_eligible_chunks",eligible);r.add("chunks",chunks);
        var full=new JsonArray();PlacementSettings.NATIVE_FULL.stream().sorted(Comparator.comparingInt(ChunkPos::x).thenComparingInt(ChunkPos::z)).forEach(p->{var a=new JsonArray();a.add(p.x());a.add(p.z());full.add(a);});r.add("generated_full",full);
        var all=new JsonArray();PlacementSettings.ALL_FULL.stream().sorted().forEach(all::add);r.add("all_generated_full",all);
        var decisions=NaturalPlacement.get(level).decisions();r.add("decisions",decisions);var observations=new JsonArray();
        decisions.asMap().values().forEach(v->{var d=v.getAsJsonObject();var actor=level.getEntity(UUID.fromString(d.get("queen").getAsString()));if(actor instanceof LasiusNigerEntity q) {
            var o=new JsonObject();o.addProperty("uuid",q.getUUID().toString());o.addProperty("age_ticks",q.elapsedAgeTicks());o.addProperty("phase",q.founding().phase().name());o.addProperty("reason",q.founding().reason());o.addProperty("removed",q.founding().removed());o.addProperty("deposited",q.founding().deposited());o.addProperty("carried",q.founding().carried());observations.add(o);
        }});r.add("early_founding",observations);
        require(PlacementSettings.NATIVE_FULL.size()>=64 && PlacementSettings.NATIVE_FULL.size()<=81 && PlacementSettings.ALL_FULL.size()<=100,"FULL chunk budget includes every auxiliary dimension");
        require(decisions.size()==4,"Exactly four selected candidates; no search enlargement");
        write("result",r);done=true;s.halt(false);
    }
}
