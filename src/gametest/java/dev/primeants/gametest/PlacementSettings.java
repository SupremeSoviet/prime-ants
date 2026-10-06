package dev.primeants.gametest;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.TicketType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import java.util.*;
import net.minecraft.world.level.ChunkPos;

/** Development-only declared generation settings, separate from retained origin fixtures. */
public final class PlacementSettings implements ModInitializer {
    public static TicketType TICKET;
    public static final Set<ChunkPos> NATIVE_FULL=new HashSet<>();
    public static final Set<String> ALL_FULL=new HashSet<>();
    public static final Set<ChunkPos> DECLARED_FULL=new HashSet<>();
    public static final Set<String> EXISTING_FULL=new HashSet<>();
    public static boolean replayExperiment(){return Set.of("t18-replay-v1","t19-replay-v1","t20-tuned-v1","t21-policy-v1").contains(System.getProperty("prime_ants.placementMode",""));}
    public static int replayFullLimit(){return "t21-policy-v1".equals(System.getProperty("prime_ants.placementMode"))||System.getProperty("prime_ants.playerBudgetDeclaration")!=null?300:"t20-tuned-v1".equals(System.getProperty("prime_ants.placementMode"))?200:150;}
    private static int workflowBefore;
    public static int replayBudget(){return (System.getProperty("prime_ants.playerBudgetDeclaration")!=null?workflowBefore:EXISTING_FULL.size()+Integer.getInteger("prime_ants.placementPriorFull",0))+(int)ALL_FULL.stream().filter(p->!EXISTING_FULL.contains(p)).count();}
    public static boolean integrationExperiment() {return "t17-nectar-v1".equals(System.getProperty("prime_ants.placementMode"));}
    public static boolean frozenExperiment() {return integrationExperiment() || "t16-frozen-v1".equals(System.getProperty("prime_ants.placementMode"));}
    public static boolean biomeExperiment() {return "t15-biome-v1".equals(System.getProperty("prime_ants.placementMode"));}
    @Override public void onInitialize() {
        String dimensions="prime_ants_test:placement_soil,prime_ants_test:placement_desert,prime_ants_test:placement_stone,prime_ants_test:placement_retry,prime_ants_test:placement_history,prime_ants_test:placement_negative";
        dimensions+=",prime_ants_test:placement_durability_retry,prime_ants_test:placement_durability_history,prime_ants_test:placement_durability_refusal";
        dimensions+=",prime_ants_test:t16_plants,prime_ants_test:t16_protected,prime_ants_test:t16_step,prime_ants_test:t16_thin,prime_ants_test:t16_unsafe,prime_ants_test:t16_legacy";
        dimensions+=",prime_ants_test:t17_nectar,prime_ants_test:t20_food";
        if(System.getProperty("prime_ants.placementSeed")!=null) dimensions+=",prime_ants_test:placement_native";
        System.setProperty("prime_ants.developmentPlacementDimensions",dimensions);
        TICKET=Registry.register(BuiltInRegistries.TICKET_TYPE,Identifier.fromNamespaceAndPath("prime_ants_test","placement"),new TicketType(0,14));
        if(replayExperiment()||System.getProperty("prime_ants.playerBudgetDeclaration")!=null) {
            try {
                var d=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(java.nio.file.Path.of(System.getProperty("prime_ants.playerBudgetDeclaration",System.getProperty("prime_ants.placementDeclaration"))))).getAsJsonObject();
                d.getAsJsonArray("existing_full").forEach(v->EXISTING_FULL.add(v.getAsString()));
                for(var v:d.getAsJsonArray("selected")){var p=v.getAsJsonArray();for(int x=p.get(0).getAsInt()-d.get("halo_radius").getAsInt();x<=p.get(0).getAsInt()+d.get("halo_radius").getAsInt();x++)for(int z=p.get(1).getAsInt()-d.get("halo_radius").getAsInt();z<=p.get(1).getAsInt()+d.get("halo_radius").getAsInt();z++)DECLARED_FULL.add(new ChunkPos(x,z));}
                workflowBefore=d.has("workflow_used_before")?d.get("workflow_used_before").getAsInt():106;
                if(System.getProperty("prime_ants.playerBudgetDeclaration")==null&&EXISTING_FULL.size()!=106||replayBudget()>=replayFullLimit())throw new IllegalStateException("Replay existing/prior budget invalid");
            }catch(java.io.IOException e){throw new RuntimeException(e);}
        }
        if(System.getProperty("prime_ants.playerBudgetDeclaration")!=null)net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server->{
            var row=new com.google.gson.JsonObject();row.addProperty("server_ticks",server.getTickCount());row.addProperty("workflow_used_before",workflowBefore);row.addProperty("budget_used",replayBudget());row.addProperty("full_limit",replayFullLimit());row.add("source_full",new com.google.gson.Gson().toJsonTree(EXISTING_FULL.stream().sorted().toList()));row.add("new_full",new com.google.gson.Gson().toJsonTree(ALL_FULL.stream().filter(k->!EXISTING_FULL.contains(k)).sorted().toList()));row.add("constructed_full",new com.google.gson.Gson().toJsonTree(ALL_FULL.stream().sorted().toList()));
            try{java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("prime_ants.captureDir"),System.getProperty("prime_ants.capturePrefix")+"-loading.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(row));}catch(java.io.IOException e){throw new RuntimeException(e);}
        });
        ServerChunkEvents.CHUNK_LOAD.register((l,c,g)->recordFull(l,c.getPos()));
    }
    public static void recordFull(net.minecraft.server.level.ServerLevel l, ChunkPos p) {
        if(System.getProperty("prime_ants.placementSeed")==null&&System.getProperty("prime_ants.playerBudgetDeclaration")==null)return;
        if(replayExperiment()||System.getProperty("prime_ants.playerBudgetDeclaration")!=null) {
            String key=l.dimension().identifier()+":"+p.x()+":"+p.z();
            if(l.dimension().identifier().toString().equals("prime_ants_test:placement_native")&&!DECLARED_FULL.contains(p))throw new IllegalStateException("Replay native region expansion refused: "+key);
            if(!EXISTING_FULL.contains(key)&&!ALL_FULL.contains(key)&&replayBudget()>=replayFullLimit())throw new IllegalStateException("Replay FULL allowance exhausted before construction: "+key);
            ALL_FULL.add(key);if(l.dimension().identifier().toString().equals("prime_ants_test:placement_native"))NATIVE_FULL.add(p);return;
        }
        ALL_FULL.add(l.dimension().identifier()+":"+p.x()+":"+p.z());
        if(ALL_FULL.size()+Integer.getInteger("prime_ants.placementPriorFull",0)>(frozenExperiment()?150:100))
            throw new IllegalStateException("Total diagnostic FULL budget includes every dimension and prior attempt");
        if(l.dimension().identifier().toString().equals("prime_ants_test:placement_native")) {
            NATIVE_FULL.add(p);
            if(frozenExperiment() ? !DECLARED_FULL.contains(p) : biomeExperiment() ? !DECLARED_FULL.contains(p) || NATIVE_FULL.size()>75
                : p.x() < -1 || p.x()>8 || p.z() < -1 || p.z()>8 || NATIVE_FULL.size()>100)
                throw new IllegalStateException("Native placement FULL budget/region exceeded: "+p);
        }
    }

}
