package dev.primeants.gametest;

import com.google.gson.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Pure file import: deliberately no Minecraft world, biome resolver, generation or survey dependency. */
final class FrozenPlacementSelection {
    static JsonObject read(Path path, String expectedHash, long seed) throws Exception {
        byte[] bytes=Files.readAllBytes(path);
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        if(!hash.equals(expectedHash))throw new IllegalStateException("Frozen selection hash mismatch");
        var selection=JsonParser.parseString(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        if(selection.get("seed").getAsLong()!=seed || !selection.get("mode").getAsString().equals("t15-biome-v1")
            || selection.get("queries").getAsInt()!=4096 || selection.getAsJsonArray("selected").size()!=3)
            throw new IllegalStateException("Frozen selection schema/seed mismatch");
        return selection;
    }
}
