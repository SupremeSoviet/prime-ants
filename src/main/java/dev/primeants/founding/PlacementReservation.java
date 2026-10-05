package dev.primeants.founding;

import com.google.gson.*;
import java.nio.file.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;

/** Placement-only disk checks. Readback is not an atomic entity/ledger or power-loss guarantee. */
final class PlacementReservation {
    private static Path folder(ServerLevel l) {
        return DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(LevelResource.ROOT)).resolve("data");
    }
    private static Path ledger(ServerLevel l) {return NaturalPlacement.TYPE.id().withSuffix(".dat").resolveAgainst(folder(l));}
    private static Path fence(ServerLevel l,JsonObject r) {return folder(l).resolve("prime_ants/placement-insertions/"+r.get("queen").getAsString()+".json");}
    static boolean fenced(ServerLevel l,JsonObject r) {return Files.exists(fence(l,r),LinkOption.NOFOLLOW_LINKS);}
    private static void match(JsonObject actual,JsonObject expected,String... fields) {
        for(String field:fields)if(!expected.has(field) || !expected.get(field).equals(actual.get(field)))throw new IllegalStateException("missing_or_mismatched_"+field);
    }
    private static void identity(JsonObject actual,JsonObject expected) {match(actual,expected,"authority","seed","dimension","x","z","queen","status");}
    /** Reads raw NBT, bypassing the loader's RESERVED -> INDETERMINATE recovery conversion. */
    static String verify(ServerLevel l,JsonObject r,String... fields) {
        try {
            String key=Long.toString(new net.minecraft.world.level.ChunkPos(r.get("x").getAsInt(),r.get("z").getAsInt()).pack());
            var tag=NbtIo.readCompressed(ledger(l),NbtAccounter.uncompressedQuota());
            var actual=JsonParser.parseString(tag.getCompound("data").orElseThrow().getString(key).orElseThrow()).getAsJsonObject();
            identity(actual,r);match(actual,r,fields);return null;
        }catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
    }
    /** A separate, never-overwritten insertion fence survives even a truncated completion ledger. */
    static String createFence(ServerLevel l,JsonObject r) {
        try {
            Path file=fence(l,r);Files.createDirectories(file.getParent());
            Files.writeString(file,r.toString(),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
            var actual=JsonParser.parseString(Files.readString(file)).getAsJsonObject();identity(actual,r);match(actual,r,"attemptId");return null;
        }catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
    }
    // Only definite non-insertion AND independently verified PENDING may release the fence.
    static String releaseFence(ServerLevel l,JsonObject r) {
        try {Files.delete(fence(l,r));return null;}catch(Exception e){return e.toString();}
    }
}
