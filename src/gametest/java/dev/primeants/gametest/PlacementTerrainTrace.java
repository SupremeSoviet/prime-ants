package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.founding.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;

/** Read-only inspectors of the actual production call sites; development archives only. */
public final class PlacementTerrainTrace {
    public static final JsonArray COLUMNS=new JsonArray();
    public static final JsonArray INSERTION_TERRAIN=new JsonArray();
    private static final Map<UUID,JsonObject> snapshots=new HashMap<>();
    public static void insertionBegin(ServerLevel l,net.minecraft.world.entity.Entity e) {
        if(!(PlacementSettings.biomeExperiment() || PlacementSettings.frozenExperiment()) || !(e instanceof dev.primeants.entity.LasiusNigerEntity))return;
        var r=NaturalPlacement.get(l).decisions().asMap().values().stream().map(JsonElement::getAsJsonObject).filter(d->d.get("queen").getAsString().equals(e.getUUID().toString())).findFirst().orElse(null);if(r==null || !r.has("surface"))return;
        var surface=BlockPos.of(r.get("surface").getAsLong());var out=new JsonObject();out.addProperty("uuid",e.getUUID().toString());out.addProperty("surface",surface.asLong());out.add("before",states(l,surface));snapshots.put(e.getUUID(),out);
    }
    public static void insertionEnd(ServerLevel l,net.minecraft.world.entity.Entity e,boolean inserted) {
        var out=snapshots.remove(e.getUUID());if(out==null)return;
        out.add("after",states(l,BlockPos.of(out.get("surface").getAsLong())));out.addProperty("zero_edits",out.get("before").equals(out.get("after")));out.addProperty("insertion_return",inserted);out.addProperty("actual_world_lookup",l.getEntity(e.getUUID())==e);INSERTION_TERRAIN.add(out);
    }
    private static JsonArray states(ServerLevel l,BlockPos p) {var a=new JsonArray();NaturalPlacement.terrain(l,p).forEach(s->a.add(s.toString()));return a;}
    private static JsonObject active;
    private static int beforeEvaluations;
    public static void begin(ServerLevel l,JsonObject r,ChunkPos chunk) {
        active=null;if(!(PlacementSettings.biomeExperiment() || PlacementSettings.frozenExperiment()))return;
        int column=r.get("column").getAsInt();if(column>=r.get("searchColumns").getAsInt())return;
        if(r.has("retryAfter") && l.getGameTime()<r.get("retryAfter").getAsLong())return;
        var offset=NaturalPlacement.searchOffset(l,r,column);
        var p=NestPlan.soilSurface(l,new BlockPos(chunk.getMinBlockX()+offset[0],0,chunk.getMinBlockZ()+offset[1]));
        active=new JsonObject();active.addProperty("chunk_x",chunk.x());active.addProperty("chunk_z",chunk.z());active.addProperty("column",column);active.addProperty("biome",l.getBiome(p).getRegisteredName());active.addProperty("eligible_biome",l.getBiome(p).is(NaturalPlacement.BIOMES));active.add("surface",block(l,p));active.add("above",block(l,p.above()));active.add("plans",new JsonArray());
        beforeEvaluations=r.has("evaluations")?r.get("evaluations").getAsInt():0;
    }
    public static void end(JsonObject r) {
        if(active==null)return;
        if(r.has("evaluations") && r.get("evaluations").getAsInt()>beforeEvaluations) {active.addProperty("decision",r.get("reason").getAsString());active.addProperty("status",r.get("status").getAsString());COLUMNS.add(active);}
        active=null;
    }
    public static NestPlan plan(ServerLevel l,BlockPos p,Direction d) {
        var plan=NestPlan.candidate(l,p,d);
        if(active!=null) {
            var row=new JsonObject();row.addProperty("direction",d.getName());row.addProperty("passed",plan!=null);
            if(plan==null)row.add("first_failure",failure(l,NestPlan.geometry(p,d)));active.getAsJsonArray("plans").add(row);
        }
        return plan;
    }
    private static JsonObject block(ServerLevel l,BlockPos p) {
        var r=new JsonObject();r.addProperty("x",p.getX());r.addProperty("y",p.getY());r.addProperty("z",p.getZ());r.addProperty("state",l.getBlockState(p).toString());r.addProperty("native_origin",NaturalSoil.get(l).eligible(l,p));r.addProperty("fluid",!l.getFluidState(p).isEmpty());r.addProperty("surface_y",l.getHeight(Heightmap.Types.WORLD_SURFACE,p.getX(),p.getZ())-1);return r;
    }
    private static String category(ServerLevel l,BlockPos p) {
        var state=l.getBlockState(p);String name=state.toString();
        if(!l.getFluidState(p).isEmpty())return "fluid";
        if(NaturalSoil.material(state))return "unknown_or_revoked_origin";
        if(name.contains("grass") || name.contains("flower") || name.contains("leaves") || name.contains("log") || name.contains("bush"))return "vegetation";
        if(state.isAir())return "slope_or_air";
        return "thin_soil_or_non_soil_substrate";
    }
    private static JsonObject fail(ServerLevel l,String stage,BlockPos p) {
        var r=new JsonObject();r.addProperty("stage",stage);r.addProperty("category",category(l,p));r.add("block",block(l,p));return r;
    }
    private static JsonObject failure(ServerLevel l,NestPlan g) {
        var soil=NaturalSoil.get(l);
        for(var p:g.tasks())if(!soil.eligible(l,p))return fail(l,"excavation_task",p);
        for(int f=0;f<3;f++)if(!soil.floorSupport(l,g.at(f,0,-f-1)))return fail(l,"shell_support",g.at(f,0,-f-1));
        for(int f=3;f<=5;f++)for(int s=-1;s<=1;s++)for(int dy:new int[]{-3,0})if(!(dy==-3?soil.floorSupport(l,g.at(f,s,dy)):soil.eligible(l,g.at(f,s,dy))))return fail(l,"shell_floor_roof",g.at(f,s,dy));
        for(int f=3;f<=5;f++)for(int s=-1;s<=1;s++)for(int dy=-2;dy<=-1;dy++)for(var d:Direction.Plane.HORIZONTAL) {
            var n=g.at(f,s,dy).relative(d);if(!g.tasks().contains(n) && !soil.eligible(l,n))return fail(l,"shell_wall",n);
        }
        for(int f=-3;f<=-1;f++)for(int s=-1;s<=1;s++) {
            var anchor=g.at(f,s,1);var p=anchor;for(int dy=1;dy>=-1;dy--){var n=anchor.offset(0,dy,0);if(soil.eligible(l,n.below())&&NestPlan.walkable(l,n)){p=n;break;}}if(!soil.eligible(l,p.below()))return fail(l,"approach_support",p.below());if(!NestPlan.walkable(l,p))return fail(l,"approach_body",p);
        }
        var deposits=new JsonArray();int count=0;
        for(var anchor:g.deposits()){var p=anchor;for(int dy=1;dy>=-1;dy--){var n=anchor.offset(0,dy,0);if(soil.eligible(l,n.below())&&NestPlan.walkable(l,n)){p=n;break;}}if(l.getBlockState(p).isAir() && soil.eligible(l,p.below()) && NestPlan.walkable(l,p))count++;else {var row=fail(l,"deposit_space",soil.eligible(l,p.below())?p:p.below());deposits.add(row);}}
        if(count<NestPlan.HARD_CAP-2) {var r=new JsonObject();r.addProperty("stage","deposit_space");r.addProperty("category","deposit_space");r.addProperty("supported_walkable",count);r.add("failed_cells",deposits);return r;}
        for(var p:g.tasks())for(var d:Direction.values())if(!l.getFluidState(p.relative(d)).isEmpty())return fail(l,"adjacent_fluid",p.relative(d));
        for(var p:g.tasks())if(!g.tasks().contains(p.above()) && NativeVegetation.material(l.getBlockState(p.above())) && !NativeVegetation.get(l).eligible(l,p.above()))return fail(l,"excavation_plant_authority",p.above());
        var r=new JsonObject();r.addProperty("category","surface_connectivity_or_deposit_stand");return r;
    }
}
