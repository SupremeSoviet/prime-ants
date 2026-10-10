package dev.primeants.gametest.mixin;

import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Exact names replace only selection; the original environments, batching and simulation remain intact. */
@Mixin(GameTestServer.class)
public abstract class ServerShardSelectionMixin {
    @Inject(method="getTestsForSelection",at=@At("HEAD"),cancellable=true)
    private static void selected(RegistryAccess registries,String filter,CallbackInfoReturnable<Stream<Holder.Reference<GameTestInstance>>> ci) throws java.io.IOException {
        String path=System.getProperty("prime_ants.caseManifest");if(path==null)return;
        var manifest=JsonParser.parseString(Files.readString(Path.of(path),StandardCharsets.UTF_8)).getAsJsonObject();
        var names=new ArrayList<String>();manifest.getAsJsonArray("names").forEach(n->names.add(n.getAsString()));
        var permanent=new ArrayList<String>();manifest.getAsJsonArray("permanent_names").forEach(n->permanent.add(n.getAsString()));
        var registered=registries.lookupOrThrow(Registries.TEST_INSTANCE).listElements()
            .filter(h->h.key().identifier().getNamespace().equals("prime_ants_test")).toList();
        var actual=registered.stream().map(h->h.key().identifier().toString()).toList();
        if(names.isEmpty()||names.size()!=new HashSet<>(names).size()||permanent.size()!=new HashSet<>(permanent).size()
                ||actual.size()!=new HashSet<>(actual).size()||!new HashSet<>(actual).equals(new HashSet<>(permanent))
                ||!actual.containsAll(names)||registered.stream().anyMatch(h->h.value().manualOnly()))throw new IllegalStateException("Exact frozen permanent/shard discovery differs");
        dev.primeants.PrimeAnts.LOGGER.info("Exact frozen shard selection label={} assigned={} permanent={} process={}",manifest.get("label"),names.size(),actual.size(),ProcessHandle.current().pid());
        ci.setReturnValue(registered.stream().filter(h->names.contains(h.key().identifier().toString())));
    }
}
