package dev.primeants.founding;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import dev.primeants.entity.AntEntities;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.world.phys.AABB;

/** Generation-triggered placement EVENT ledger. Never counts population or replaces missing actors. */
public final class NaturalPlacement extends SavedData {
    public static final int COLUMNS = 64, PER_TICK = 2, RETRIES = 3, MIN_SPACING = 56;
    // Sixty-four columns plus at most two same-column insertion retries. Checkpoint BEFORE any plan/factory call.
    public static final int MAX_EVALUATIONS = COLUMNS + RETRIES - 1;
    public static final String AUTHORITY = "new_terrain_v1";
    public static final TagKey<Biome> BIOMES = TagKey.create(Registries.BIOME,
            Identifier.fromNamespaceAndPath("prime_ants", "founding_queen"));
    public static final Codec<NaturalPlacement> CODEC = Codec.PASSTHROUGH.xmap(d -> {
        var data = new NaturalPlacement();
        d.getMapValues().getOrThrow().forEach((k,v) -> {
            var r = JsonParser.parseString(v.asString().getOrThrow()).getAsJsonObject();
            // A durable reservation surviving a crash is uncertain: NEVER retry it.
            if (r.get("status").getAsString().equals("RESERVED")) {
                r.addProperty("status", "INDETERMINATE"); r.addProperty("reason", "interrupted_insertion_no_replacement");
            }
            data.records.put(k.asString().getOrThrow(), r);
            if (r.get("status").getAsString().equals("PENDING") && !r.has("searchVersion")) {
                r.addProperty("searchVersion",1);r.addProperty("searchColumns",8);r.addProperty("evaluationLimit",10);
                r.addProperty("searchMin",6);r.addProperty("searchMax",9);
            }
            if (r.get("status").getAsString().equals("PENDING")) data.queue.add(k.asString().getOrThrow());
        });
        return data;
    }, data -> {
        var object = new JsonObject(); data.records.forEach((k,v) -> object.addProperty(k,v.toString()));
        return new com.mojang.serialization.Dynamic<>(com.mojang.serialization.JsonOps.INSTANCE,object);
    });
    public static final SavedDataType<NaturalPlacement> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("prime_ants", "natural_placement"), NaturalPlacement::new, CODEC, DataFixTypes.LEVEL);
    private final Map<String,JsonObject> records = new LinkedHashMap<>();
    private final ArrayDeque<String> queue = new ArrayDeque<>();
    public static NaturalPlacement get(ServerLevel l) { return l.getDataStorage().computeIfAbsent(TYPE); }
    /** Promoted after protected native soil excavation. Explicit opt-out; legacy explicit setting remains compatible. */
    public static boolean productionEnabled() {
        return Boolean.parseBoolean(System.getProperty("prime_ants.naturalPlacement",System.getProperty("prime_ants.experimentalNaturalPlacement","true")));
    }
    public static boolean enabled(ServerLevel l) {
        return productionEnabled() && l.dimension().equals(Level.OVERWORLD)
            || net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment()
                && Arrays.asList(System.getProperty("prime_ants.developmentPlacementDimensions", "").split(","))
                    .contains(l.dimension().identifier().toString());
    }
    private static long salt(ServerLevel l) {
        return l.getSeed() ^ UUID.nameUUIDFromBytes(l.dimension().identifier().toString().getBytes(StandardCharsets.UTF_8)).getMostSignificantBits();
    }
    public static boolean selected(ServerLevel l, ChunkPos p) {
        // One central chunk per 4x4 cell. Seed/dimension choose residue 1 or 2; negative coordinates use floorMod.
        return Math.floorMod(p.x(),4) == 1 + (int)(salt(l)&1)
                && Math.floorMod(p.z(),4) == 1 + (int)((salt(l)>>>1)&1);
    }
    private static UUID identity(ServerLevel l, ChunkPos p) {
        return UUID.nameUUIDFromBytes((AUTHORITY+":"+l.getSeed()+":"+l.dimension().identifier()+":"+p.x()+":"+p.z()).getBytes(StandardCharsets.UTF_8));
    }
    public JsonObject decisions() {
        var copy = new JsonObject(); records.forEach((k,v) -> copy.add(k,v.deepCopy())); return copy;
    }
    /** Main-thread conversion only. Reads the passed proto, no world queries/neighbor loading/insertion. */
    public void observeGenerated(ServerLevel l, ProtoChunk proto) {
        if (!l.getServer().isSameThread()) throw new IllegalStateException("Placement authority requires server thread");
        if (!enabled(l) || !((GenerationWitness)proto).primeAntsGeneratedTerrain()
                || proto.isUpgrading() || proto.getBelowZeroRetrogen()!=null || !selected(l,proto.getPos())) return;
        String key = Long.toString(proto.getPos().pack());
        if (records.containsKey(key)) return;
        var r = new JsonObject(); r.addProperty("authority",AUTHORITY); r.addProperty("seed",l.getSeed());
        r.addProperty("dimension",l.dimension().identifier().toString()); r.addProperty("x",proto.getPos().x()); r.addProperty("z",proto.getPos().z());
        r.addProperty("queen",identity(l,proto.getPos()).toString()); r.addProperty("status","PENDING");
        r.addProperty("reason","awaiting_full_entity_ticking"); r.addProperty("column",0); r.addProperty("attempts",0);
        r.addProperty("searchVersion",2);r.addProperty("searchColumns",COLUMNS);r.addProperty("evaluationLimit",MAX_EVALUATIONS);
        r.addProperty("searchMin",4);r.addProperty("searchMax",11);
        r.add("rejections",new JsonObject()); records.put(key,r); queue.add(key); setDirty();
    }
    public static void tick(ServerLevel l) { if (enabled(l)) get(l).evaluate(l); }
    private void evaluate(ServerLevel l) {
        if (!l.getServer().isSameThread()) throw new IllegalStateException("Placement evaluation requires server thread");
        int budget = Math.min(PER_TICK,queue.size());
        for (int i=0;i<budget;i++) {
            String key=queue.remove(); var r=records.get(key);
            if (!"PENDING".equals(r.get("status").getAsString())) continue;
            var p=new ChunkPos(r.get("x").getAsInt(),r.get("z").getAsInt());
            if (!authority(l,r,p,key)) finish(r,"REJECTED","missing_or_mismatched_generation_authority");
            else if (!searchValid(r)) finish(r,"REJECTED","unsupported_or_mismatched_search_version");
            else if (PlacementReservation.fenced(l,r)) finish(r,"INDETERMINATE","persisted_insertion_fence_no_replacement");
            else if (ready(l,p)) evaluateColumn(l,r,p);
            if ("PENDING".equals(r.get("status").getAsString())) queue.add(key);
        }
    }
    private boolean authority(ServerLevel l,JsonObject r,ChunkPos p,String key) {
        return r.has("authority") && AUTHORITY.equals(r.get("authority").getAsString()) && r.has("seed") && r.get("seed").getAsLong()==l.getSeed()
                && r.has("dimension") && r.get("dimension").getAsString().equals(l.dimension().identifier().toString())
                && key.equals(Long.toString(p.pack())) && selected(l,p) && identity(l,p).toString().equals(r.get("queen").getAsString());
    }
    public static boolean ready(ServerLevel l,ChunkPos p) {
        // Search offsets 4..11 may cross the candidate boundary; inspect a FULL halo without acquiring tickets.
        // A loaded halo also covers the larger read-only insertion snapshot; never request missing neighbors.
        for(int x=p.x()-1;x<=p.x()+1;x++) for(int z=p.z()-1;z<=p.z()+1;z++) {
            var q=new ChunkPos(x,z);
            if(l.getChunkSource().getChunk(x,z,ChunkStatus.FULL,false)==null) return false;
        }
        return l.areEntitiesActuallyLoadedAndTicking(p)
            && l.isPositionEntityTicking(new BlockPos(p.getMinBlockX()+8,l.getMinY()+8,p.getMinBlockZ()+8));
    }
    private static final int[][] LEGACY_OFFSETS={{7,7},{8,8},{6,8},{9,7},{7,9},{8,6},{6,6},{9,9}};
    private static boolean searchValid(JsonObject r) {
        int v=r.has("searchVersion")?r.get("searchVersion").getAsInt():0;
        int columns=v==1?8:v==2?64:0;
        return columns>0 && r.has("searchColumns") && r.get("searchColumns").getAsInt()==columns
            && r.has("evaluationLimit") && r.get("evaluationLimit").getAsInt()==columns+2
            && r.has("searchMin") && r.get("searchMin").getAsInt()==(v==1?6:4)
            && r.has("searchMax") && r.get("searchMax").getAsInt()==(v==1?9:11);
    }
    public static int[] searchOffset(ServerLevel l,JsonObject r,int column) {
        if(r.get("searchVersion").getAsInt()==1) return LEGACY_OFFSETS[(column+(int)(salt(l)&7))%8].clone();
        int index=(column+(int)(salt(l)&63))%64;
        return new int[]{4+index/8,4+index%8}; // rotated x-major grid; direction order is vanilla HORIZONTAL.
    }
    private void reject(JsonObject r,String why) {
        var counts=r.getAsJsonObject("rejections"); counts.addProperty(why,counts.has(why)?counts.get(why).getAsInt()+1:1);
        r.addProperty("reason",why); setDirty();
    }
    private void finish(JsonObject r,String status,String why) { r.addProperty("status",status);r.addProperty("reason",why);setDirty(); }
    private static void count(JsonObject r,String field) {r.addProperty(field,r.has(field)?r.get(field).getAsInt()+1:1);}
    private String saveVerified(ServerLevel l,JsonObject r,String... fields) {
        try {l.getDataStorage().saveAndJoin();return PlacementReservation.verify(l,r,fields);}
        catch(Exception e){return e.toString();}
    }
    private void storageDiagnostic(JsonObject r,String stage,String why) {
        r.addProperty("storageDiagnostic",stage+":"+why);setDirty();
        dev.primeants.PrimeAnts.LOGGER.error("PLACEMENT persistence verification refused queen={} attempt={} stage={} detail={}",r.get("queen"),r.get("attemptId"),stage,why);
    }
    private void evaluateColumn(ServerLevel l,JsonObject r,ChunkPos p) {
        if(r.has("retryAfter") && l.getGameTime()<r.get("retryAfter").getAsLong()) return;
        int column=r.get("column").getAsInt();
        int columns=r.get("searchColumns").getAsInt();
        if(column<0 || column>=columns) {finish(r,"REJECTED","bounded_search_exhausted");return;}
        int evaluations=r.has("evaluations")?r.get("evaluations").getAsInt():0;
        if(evaluations>=r.get("evaluationLimit").getAsInt() || r.get("attempts").getAsInt()>=RETRIES) {finish(r,"REJECTED","evaluation_or_insertion_budget_exhausted");return;}
        count(r,"evaluations");setDirty();
        String checkpoint=saveVerified(l,r,"evaluations","column","attempts","searchVersion","searchColumns","evaluationLimit","searchMin","searchMax");
        if(checkpoint!=null) {storageDiagnostic(r,"search_checkpoint",checkpoint);finish(r,"INDETERMINATE","unverified_search_progress_no_insertion");return;}
        int[] offset=searchOffset(l,r,column);
        var surface=new BlockPos(p.getMinBlockX()+offset[0],0,p.getMinBlockZ()+offset[1]);
        surface=NestPlan.soilSurface(l,surface);
        r.addProperty("biome",l.getBiome(surface).getRegisteredName());
        String problem=null; NestPlan plan=null;
        if(!l.getBiome(surface).is(BIOMES)) problem="unsupported_biome";
        else if(!l.getFluidState(surface).isEmpty() || !l.getFluidState(surface.above()).isEmpty()) problem="fluid";
        else if(!NaturalSoil.material(l.getBlockState(surface))) problem="unsupported_surface";
        else if(!NaturalSoil.get(l).eligible(l,surface)) problem="unknown_or_revoked_origin";
        else {
            for(Direction d:Direction.Plane.HORIZONTAL) {count(r,"preliminaryPlans");plan=NestPlan.candidate(l,surface,d);if(plan!=null)break;}
            if(plan==null) problem="protected_footprint_or_support_roof_floor";
        }
        if(problem!=null) {reject(r,problem);r.addProperty("column",column+1);if(column+1==columns)finish(r,"REJECTED","bounded_search_exhausted");return;}
        var feet=surface.above();
        var box=AntEntities.QUEEN.getSpawnAABB(feet.getX()+.5,feet.getY(),feet.getZ()+.5);
        if(!l.getWorldBorder().isWithinBounds(feet) || !NestPlan.walkable(l,feet) || !l.noCollision(box)
                || !l.getEntities(null,box).isEmpty()) problem="occupied_or_obstructed_space";
        // Selected chunks are separated by >=64 blocks on a fixed lattice; offsets 4..11
        // reduce the nearest possible separation to 57, exceeding MIN_SPACING. No historical-ledger scan.
        if(!l.getEntitiesOfClass(dev.primeants.entity.LasiusNigerEntity.class,new AABB(feet).inflate(MIN_SPACING),
                q->q.form()==dev.primeants.entity.AntForm.QUEEN && q.isAlive()).isEmpty()) problem="existing_queen_spacing";
        if(problem!=null) {reject(r,problem);r.addProperty("column",column+1);return;}
        var uuid=UUID.fromString(r.get("queen").getAsString());
        if(l.getEntityInAnyDimension(uuid)!=null) {finish(r,"INDETERMINATE","identity_already_present_no_replacement");return;}
        count(r,"temporaryQueens");var queen=AntEntities.QUEEN.create(l,EntitySpawnReason.CHUNK_GENERATION);
        if(queen==null) {finish(r,"REJECTED","entity_factory_refused");return;}
        queen.setUUID(uuid); queen.snapTo(feet.getX()+.5,feet.getY(),feet.getZ()+.5,0,0);
        queen.finalizeSpawn(l,l.getCurrentDifficultyAt(feet),EntitySpawnReason.CHUNK_GENERATION,null);
        // No world mutation between final live checks and insertion. All prospective work still uses original protection.
        count(r,"immediateValidations");
        if(!ready(l,p) || NestPlan.candidate(l,plan.entrance(),plan.direction())==null
                || !l.getBiome(surface).is(BIOMES) || !l.noCollision(queen,queen.getBoundingBox())
                || !l.getEntities(queen,queen.getBoundingBox()).isEmpty()) {
            reject(r,"immediate_revalidation_failed");r.addProperty("column",column+1);
            if(column+1==columns)finish(r,"REJECTED","bounded_search_exhausted");
            String progress=saveVerified(l,r,"evaluations","column");
            if(progress!=null) {storageDiagnostic(r,"final_refusal_progress",progress);finish(r,"INDETERMINATE","unverified_search_progress_no_insertion");}
            return;
        }
        r.addProperty("surface",surface.asLong());r.addProperty("direction",plan.direction().getName());
        var before=terrain(l,surface); r.addProperty("attempts",r.get("attempts").getAsInt()+1);
        r.addProperty("attemptId",UUID.randomUUID().toString());
        finish(r,"RESERVED","insertion_reservation_awaiting_disk_verification");
        count(r,"reservationChecks");String reservation=saveVerified(l,r,"attemptId","attempts","evaluations");
        if(reservation!=null) {
            storageDiagnostic(r,"reservation",reservation);reject(r,"reservation_verification_failed");
            if(r.get("attempts").getAsInt()>=RETRIES)finish(r,"REJECTED","reservation_retry_budget_exhausted");
            else {finish(r,"PENDING","unverified_reservation_no_insertion");r.addProperty("retryAfter",l.getGameTime()+20);}
            return;
        }
        count(r,"verifiedReservations");String fence=PlacementReservation.createFence(l,r);
        if(fence!=null) {storageDiagnostic(r,"insertion_fence",fence);finish(r,"INDETERMINATE","unverified_insertion_fence_no_insertion");return;}
        count(r,"insertionCalls");
        boolean inserted=l.addFreshEntity(queen);
        boolean actual=l.getEntity(uuid)==queen && queen.level()==l && !queen.isRemoved();
        r.addProperty("zeroBlockEdits",before.equals(terrain(l,surface)));
        if(!r.get("zeroBlockEdits").getAsBoolean()) throw new IllegalStateException("Natural placement altered terrain");
        if(inserted && actual) finish(r,"PLACED","verified_world_insertion");
        else if(actual || inserted) finish(r,"INDETERMINATE","insertion_identity_uncertain_no_replacement");
        else if(r.get("attempts").getAsInt()>=RETRIES) finish(r,"REJECTED","insertion_retry_budget_exhausted");
        else {finish(r,"PENDING","insertion_refused_retry_same_identity");r.addProperty("retryAfter",l.getGameTime()+20);}
        String completion=saveVerified(l,r,"attemptId","attempts");
        if(completion!=null)storageDiagnostic(r,"completion",completion);
        // PLACED/uncertain results keep the fence forever, even after death. No actor lookup can release it.
        if(!inserted && !actual && "PENDING".equals(r.get("status").getAsString())) {
            if(completion==null) {
                String release=PlacementReservation.releaseFence(l,r);
                if(release!=null) {storageDiagnostic(r,"fence_release",release);finish(r,"INDETERMINATE","persisted_insertion_fence_no_replacement");}
            }else finish(r,"INDETERMINATE","unverified_completion_no_replacement");
        }
        dev.primeants.PrimeAnts.LOGGER.info("PLACEMENT DIAGNOSTIC queen={} chunk={} status={} attempts={} zeroBlockEdits={}",uuid,p,r.get("status"),r.get("attempts"),r.get("zeroBlockEdits"));
    }
    /** Read-only insertion invariant covering the complete founding envelope. */
    public static List<net.minecraft.world.level.block.state.BlockState> terrain(ServerLevel l,BlockPos surface) {
        var result=new ArrayList<net.minecraft.world.level.block.state.BlockState>();
        for(var p:BlockPos.betweenClosed(surface.offset(-8,-4,-8),surface.offset(8,3,8))) result.add(l.getBlockState(p));
        return result;
    }
}
