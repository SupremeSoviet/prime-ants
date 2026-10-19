package dev.primeants.gametest;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.HexFormat;
import java.util.zip.ZipInputStream;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.level.storage.LevelResource;

/** Exact saved vanilla terrain and its original generation observations. No colony, jobs or inventory is imported. */
public final class NativeClaySnapshot implements ModInitializer {
    static JsonObject specification(){
        try(var input=NativeClaySnapshot.class.getResourceAsStream("/dev/primeants/gametest/native-clay-seed-zero.json")){
            if(input==null)throw new IllegalStateException("Missing native clay specification");
            return JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(IOException e){throw new UncheckedIOException(e);}
    }
    private static String hash(byte[] data){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    @Override public void onInitialize(){
        ServerLifecycleEvents.SERVER_STARTING.register(server->{
            var spec=specification();
            var root=server.getWorldPath(LevelResource.ROOT).resolve("dimensions/prime_ants_test/natural_clay").toAbsolutePath().normalize();
            var marker=root.resolve(".frozen-native-clay");
            try{
                // A restart must retain actual revocations and edits, never reinstall the original observations.
                if(Files.exists(marker)){
                    if(!Files.readString(marker).equals(spec.get("snapshot_sha256").getAsString()))throw new IllegalStateException("Native clay snapshot identity mismatch");
                    return;
                }
                byte[] archive;
                try(var input=NativeClaySnapshot.class.getResourceAsStream("/dev/primeants/gametest/native-clay-seed-zero.zip")){
                    if(input==null)throw new IllegalStateException("Missing native clay snapshot");archive=input.readAllBytes();
                }
                if(!hash(archive).equals(spec.get("snapshot_sha256").getAsString()))throw new IllegalStateException("Native clay snapshot hash mismatch");
                var files=spec.getAsJsonObject("snapshot_files");int copied=0;
                try(var zip=new ZipInputStream(new ByteArrayInputStream(archive))){
                    java.util.zip.ZipEntry entry;
                    while((entry=zip.getNextEntry())!=null){
                        if(entry.isDirectory()||!files.has(entry.getName()))throw new IllegalStateException("Unexpected native snapshot member");
                        var target=root.resolve(entry.getName()).normalize();
                        if(!target.startsWith(root)||Files.exists(target))throw new IllegalStateException("Native snapshot destination is not fresh");
                        byte[] bytes=zip.readAllBytes();if(!hash(bytes).equals(files.get(entry.getName()).getAsString()))throw new IllegalStateException("Native snapshot member hash mismatch");
                        Files.createDirectories(target.getParent());Files.write(target,bytes,StandardOpenOption.CREATE_NEW);copied++;
                    }
                }
                if(copied!=files.size())throw new IllegalStateException("Incomplete native clay snapshot");
                Files.writeString(marker,spec.get("snapshot_sha256").getAsString(),StandardOpenOption.CREATE_NEW);
                dev.primeants.PrimeAnts.LOGGER.info("CLAY NATIVE SNAPSHOT READY seed=0 chunks={} inventoryEntries={} colonyBlocks={} files={} sha256={}",spec.get("snapshot_full_chunks"),spec.get("snapshot_inventory_entries"),spec.get("snapshot_colony_blocks"),copied,spec.get("snapshot_sha256").getAsString());
            }catch(IOException e){throw new UncheckedIOException(e);}
        });
    }
}
