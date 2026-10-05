package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.*;

public final class NaturalPlacementGameTest {
    static ServerLevel level(GameTestHelper c,String name) {
        return c.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.fromNamespaceAndPath("prime_ants_test",name)));
    }
    static ChunkPos prepare(GameTestHelper c,ServerLevel l,int base) {
        ChunkPos selected=null;
        for(int x=base;x<base+4;x++)for(int z=base;z<base+4;z++)if(NaturalPlacement.selected(l,new ChunkPos(x,z)))selected=new ChunkPos(x,z);
        c.assertTrue(selected!=null,"Deterministic density selects one candidate in 4x4");
        c.assertTrue(l.getChunkSource().getChunk(selected.x(),selected.z(),ChunkStatus.FULL,false)==null,"Positive chunk must be genuinely new");
        l.getChunkSource().addTicketWithRadius(PlacementSettings.TICKET,selected,2);
        l.getChunkSource().getChunk(selected.x(),selected.z(),ChunkStatus.FULL,true);
        c.assertTrue(decision(l,selected)!=null,"Generation conversion automatically queues candidate");
        return selected;
    }
    static JsonObject decision(ServerLevel l,ChunkPos p) {return NaturalPlacement.get(l).decisions().getAsJsonObject(Long.toString(p.pack()));}
    static UUID uuid(ServerLevel l,ChunkPos p) {return UUID.fromString(decision(l,p).get("queen").getAsString());}
    static String status(ServerLevel l,ChunkPos p) {return decision(l,p).get("status").getAsString();}
    static void reloadChunk(ServerLevel l,ChunkPos p) {
        var chunk=(LevelChunk)l.getChunkSource().getChunk(p.x(),p.z(),ChunkStatus.FULL,false);
        var serial=SerializableChunkData.copyOf(l,chunk);
        SerializableChunkData.parse(l,l.palettedContainerFactory(),serial.write()).read(l,l.getPoiManager(),new RegionStorageInfo("placement",l.dimension(),"chunk"),p);
    }
    @GameTest(maxTicks=500)
    public void genuineGenerationPlacesAndTicksFoundingQueen(GameTestHelper c) {
        var l=level(c,"placement_soil");var p=prepare(c,l,512);boolean[] seen={false};
        c.onEachTick(()->{
            if(!status(l,p).equals("PLACED"))return;
            var r=decision(l,p);var q=(LasiusNigerEntity)l.getEntity(uuid(l,p));
            c.assertTrue(q!=null && q.form()==AntForm.QUEEN && r.get("zeroBlockEdits").getAsBoolean(),"Verified insertion of exactly one real queen with zero placement edits");
            if(!seen[0]) {
                seen[0]=true;c.assertTrue(q.elapsedAgeTicks()==0 && q.founding().phase()==QueenFounding.Phase.SEEKING && q.founding().removed()==0,"Shared finalizeSpawn requests founding before any real queen tick");
                c.assertTrue(q.getMainHandItem().isEmpty() && q.bodyReserve()==dev.primeants.brood.BroodPile.MAX_RESERVE,"Only normal queen initialization reserves");
            }
            if(q.founding().removed()>0) {c.assertTrue(q.elapsedAgeTicks()>0 && q.founding().loadedTicks()>0,"Automatic queen advances and excavates through actual server ticks");c.succeed();}
        });
    }
    @GameTest(maxTicks=300)
    public void biomeSubstrateAndPlayerReplacementRejectSafely(GameTestHelper c) {
        var desert=level(c,"placement_desert");var d=prepare(c,desert,600);
        var stone=level(c,"placement_stone");var s=prepare(c,stone,700);
        var l=level(c,"placement_soil");var p=prepare(c,l,800);int y=l.getMinY()+7;
        for(int x=p.getMinBlockX();x<=p.getMaxBlockX();x++)for(int z=p.getMinBlockZ();z<=p.getMaxBlockZ();z++)l.setBlock(new BlockPos(x,y,z),Blocks.GRASS_BLOCK.defaultBlockState(),3);
        var before=NaturalPlacement.terrain(l,new BlockPos(p.getMinBlockX()+8,y,p.getMinBlockZ()+8));
        c.succeedWhen(()->{
            c.assertTrue(status(desert,d).equals("REJECTED") && status(stone,s).equals("REJECTED") && status(l,p).equals("REJECTED"),"Bounded negative evaluations finish");
            c.assertTrue(decision(desert,d).getAsJsonObject("rejections").has("unsupported_biome") && decision(stone,s).getAsJsonObject("rejections").has("unsupported_surface")
                && decision(l,p).getAsJsonObject("rejections").has("unknown_or_revoked_origin"),"Biome, substrate and same-state player-origin rejection reasons");
            c.assertTrue(before.equals(NaturalPlacement.terrain(l,new BlockPos(p.getMinBlockX()+8,y,p.getMinBlockZ()+8))) && l.getEntity(uuid(l,p))==null,"Rejected placement changes nothing");
        });
    }
    @GameTest(maxTicks=300)
    public void fluidOccupiedAndUnknownFootprintsRejectSafely(GameTestHelper c) {
        var l=level(c,"placement_negative");var fluid=prepare(c,l,900);var occupied=prepare(c,l,1000);var unknown=prepare(c,l,1100);var actors=prepare(c,l,1152);int y=l.getMinY()+7;
        for(var p:List.of(fluid,occupied,unknown))for(int x=p.getMinBlockX();x<=p.getMaxBlockX();x++)for(int z=p.getMinBlockZ();z<=p.getMaxBlockZ();z++) {
            if(p.equals(unknown))NaturalSoil.get(l).invalidate(new BlockPos(x,y-2,z));
            else l.setBlock(new BlockPos(x,y+1,z),(p.equals(fluid)?Blocks.WATER:Blocks.STONE).defaultBlockState(),3);
        }
        // Negative occupancy only: ordinary stationary armor stands occupy every bounded search column.
        var occupantIds=new ArrayList<UUID>();
        for(int[] offset:new int[][]{{7,7},{8,8},{6,8},{9,7},{7,9},{8,6},{6,6},{9,9}}) {
            var stand=net.minecraft.world.entity.EntityTypes.ARMOR_STAND.create(l,net.minecraft.world.entity.EntitySpawnReason.EVENT);
            c.assertTrue(stand!=null,"Negative occupant factory");stand.setPos(actors.getMinBlockX()+offset[0]+.5,y+1,actors.getMinBlockZ()+offset[1]+.5);
            c.assertTrue(l.addFreshEntity(stand),"Negative occupant insertion accepted before chunk becomes accessible");occupantIds.add(stand.getUUID());
        }
        c.succeedWhen(()->{for(var p:List.of(fluid,occupied,unknown,actors))c.assertTrue(status(l,p).equals("REJECTED") && l.getEntity(uuid(l,p))==null,"Fluids, occupied surface/real actors and unknown prospective soil remain protected");
            c.assertTrue(occupantIds.stream().allMatch(id->l.getEntity(id)!=null),"Every negative occupant is actually accessible in the loaded world");
            c.assertTrue(decision(l,actors).getAsJsonObject("rejections").has("occupied_or_obstructed_space"),"Actual entity collision occupancy must reject the production candidate");});
    }
    @GameTest(maxTicks=400)
    public void insertionRefusalRetriesOnePersistedIdentity(GameTestHelper c) {
        var l=level(c,"placement_retry");var p=prepare(c,l,1200);var id=uuid(l,p);PlacementFault.block(id);boolean[] released={false};
        c.onEachTick(()->{
            if(!released[0] && PlacementFault.attempts(id)>0) {
                c.assertTrue(l.getEntity(id)==null && status(l,p).equals("PENDING"),"Refusal retains safe retry");
                l.getDataStorage().saveAndJoin();
                try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())) {
                    var loaded=disk.get(NaturalPlacement.TYPE);c.assertTrue(loaded!=null,"Actual persisted pending candidate loads");l.getDataStorage().set(NaturalPlacement.TYPE,loaded);
                }
                reloadChunk(l,p);c.assertTrue(uuid(l,p).equals(id) && status(l,p).equals("PENDING"),"Loading preserves original authority and identity without minting");
                PlacementFault.release(id);released[0]=true;
            }
            if(released[0] && status(l,p).equals("PLACED")) {c.assertTrue(uuid(l,p).equals(id) && decision(l,p).get("attempts").getAsInt()==2 && l.getEntity(id)!=null,"Exactly one identity after actual insertion retry");c.succeed();}
        });
    }
    @GameTest(maxTicks=350)
    public void loadingDeathRepeatedAuthorityAndMissingAuthorityNeverMint(GameTestHelper c) {
        var l=level(c,"placement_history");var p=prepare(c,l,1300);boolean[] removed={false};
        c.onEachTick(()->{
            if(removed[0] || !status(l,p).equals("PLACED"))return;
            removed[0]=true;var id=uuid(l,p);var count=NaturalPlacement.get(l).decisions().size();reloadChunk(l,p);reloadChunk(l,p);
            c.assertTrue(NaturalPlacement.get(l).decisions().size()==count && uuid(l,p).equals(id),"Repeated FULL deserialization cannot create candidate or actor");
            l.getDataStorage().saveAndJoin();
            try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())) {
                var restored=disk.get(NaturalPlacement.TYPE);c.assertTrue(restored!=null,"Actual placed decision reloads from disk");l.getDataStorage().set(NaturalPlacement.TYPE,restored);
            }
            c.assertTrue(status(l,p).equals("PLACED") && uuid(l,p).equals(id),"Disk reload retains committed identity");
            var q=(LasiusNigerEntity)l.getEntity(id);q.hurtServer(l,q.damageSources().generic(),1000);
            var json=NaturalPlacement.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,NaturalPlacement.get(l)).getOrThrow();
            var loaded=NaturalPlacement.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,json).getOrThrow();l.getDataStorage().set(NaturalPlacement.TYPE,loaded);
            c.runAfterDelay(30,()->{c.assertTrue(l.getEntity(id)==null && status(l,p).equals("PLACED") && NaturalPlacement.get(l).decisions().size()==count,"Placed event persists after death: zero replacements");
                // Negative corrupted pending records, derived from genuine authority, never a positive fixture.
                var corrupted=json.deepCopy().getAsJsonObject();var r=JsonParser.parseString(corrupted.get(Long.toString(p.pack())).getAsString()).getAsJsonObject();
                r.addProperty("status","RESERVED");corrupted.addProperty(Long.toString(p.pack()),r.toString());
                var interrupted=NaturalPlacement.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,corrupted).getOrThrow();
                c.assertTrue(interrupted.decisions().getAsJsonObject(Long.toString(p.pack())).get("status").getAsString().equals("INDETERMINATE"),"Interrupted durable reservation fails closed without retry");
                r.remove("authority");r.addProperty("status","PENDING");corrupted.addProperty(Long.toString(p.pack()),r.toString());
                l.getDataStorage().set(NaturalPlacement.TYPE,NaturalPlacement.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,corrupted).getOrThrow());
                c.runAfterDelay(5,()->{c.assertTrue(status(l,p).equals("REJECTED") && l.getEntity(id)==null,"Missing authority fails closed");c.succeed();});
            });
        });
    }
    @GameTest(maxTicks=20)
    public void productionPlacementIsDisabledByDefault(GameTestHelper c) {
        c.assertTrue(!NaturalPlacement.enabled(c.getLevel()) && !Boolean.getBoolean("prime_ants.experimentalNaturalPlacement"),"Production placement remains disabled by default");
        c.succeed();
    }
}
