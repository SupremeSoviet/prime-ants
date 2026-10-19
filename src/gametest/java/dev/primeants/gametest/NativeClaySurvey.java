package dev.primeants.gametest;

import com.mojang.serialization.JsonOps;
import dev.primeants.PrimeAnts;
import dev.primeants.founding.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;

/** Frozen ordinary Overworld geology, independent of the controlled worker fixtures. No colony progression claim. */
final class NativeClaySurvey {
    static void run(GameTestHelper c){
        var l=NaturalPlacementGameTest.level(c,"natural_clay");
        c.assertTrue(l.getSeed()==0&&l.getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator,"Unmodified seed-zero Overworld noise geology");
        var frozen=NativeClaySnapshot.specification();
        var expected=new HashSet<>(BlockPos.CODEC.listOf().parse(JsonOps.INSTANCE,frozen.get("clay_positions")).getOrThrow());
        var expectedDry=new HashSet<>(BlockPos.CODEC.listOf().parse(JsonOps.INSTANCE,frozen.get("dry_clay_positions")).getOrThrow());
        var min=BlockPos.CODEC.parse(JsonOps.INSTANCE,frozen.get("minimum")).getOrThrow();var max=BlockPos.CODEC.parse(JsonOps.INSTANCE,frozen.get("maximum")).getOrThrow();
        var entrance=BlockPos.CODEC.parse(JsonOps.INSTANCE,frozen.get("candidate_entrance")).getOrThrow();var cp=new ChunkPos(2048,758);
        int halo=frozen.get("full_chunk_halo").getAsInt();c.assertTrue(halo==3,"The frozen load footprint covers every census cell and neighbour");
        c.assertTrue(l.getChunkSource().getChunk(cp.x(),cp.z(),net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false)==null,"The frozen genuinely generated site has not yet been loaded by this server");
        PrimeAnts.LOGGER.info("CLAY ORDINARY FROZEN BEFORE READBACK seed=0 chunk={} min={} max={} expectedClay={} expectedDry={} candidate={} fullHalo=3 snapshot={} materialGifts=0 foodAssistance=0 colonyProgressionClaim=false",cp,min,max,expected.size(),expectedDry.size(),entrance,frozen.get("snapshot_sha256").getAsString());
        l.getChunkSource().addTicketWithRadius(PlacementSettings.TICKET,cp,3);
        for(int dx=-halo;dx<=halo;dx++)for(int dz=-halo;dz<=halo;dz++)l.getChunkSource().getChunk(cp.x()+dx,cp.z()+dz,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,true);
        c.runAfterDelay(80,()->{
            var clay=new HashSet<BlockPos>();var dry=new HashSet<BlockPos>();
            for(var mutable:BlockPos.betweenClosed(min,max)){
                var at=mutable.immutable();if(!l.getBlockState(at).is(Blocks.CLAY))continue;
                c.assertTrue(NaturalMaterials.get(l).eligible(l,at),"Every census clay block retains genuine generation-observed authority, not recognition by type");
                c.assertTrue(!NaturalSoil.get(l).eligible(l,at),"Clay never becomes ordinary soil or simultaneous dirt credit");clay.add(at);
                boolean safe=true;for(var face:Direction.values())if(!l.getFluidState(at.relative(face)).isEmpty())safe=false;if(safe)dry.add(at);
            }
            PrimeAnts.LOGGER.info("CLAY ORDINARY ACTUAL CENSUS fullHalo=3 clay={} dry={} positions={} dryPositions={}",clay.size(),dry.size(),clay,dry);
            c.assertTrue(clay.equals(expected)&&dry.equals(expectedDry),"Frozen native site and complete resource census match before any accepted intervention");
            var inBounds=dry.stream().filter(p->MiningShape.within(p.getX()-entrance.getX(),p.getY()-entrance.getY(),p.getZ()-entrance.getZ())).toList();
            var walls=new HashSet<>(NestWalls.walls(NestBlueprint.FOUNDING_CHAMBER));walls.addAll(NestWalls.walls(NestBlueprint.MATERIAL_STORE.getFirst().room().cells()));walls.addAll(NestWalls.walls(NestBlueprint.QUEENS_HALL.getFirst().room().cells()));
            c.assertTrue(inBounds.size()==frozen.get("candidate_dry_clay").getAsInt()&&walls.size()*NestWalls.CLAY_PER_CELL==frozen.get("distinct_wall_recipe").getAsInt(),"The declared potential layout needs 40 distinct wall payments; only 31 initially dry units lie in its bounds");
            c.assertTrue(inBounds.size()<walls.size()*NestWalls.CLAY_PER_CELL,"Resource census is not a complete ordinary-world progression acceptance");
            var sample=inBounds.getFirst();var state=l.getBlockState(sample);l.setBlock(sample,state,3);
            c.assertTrue(!NaturalMaterials.get(l).eligible(l,sample)&&l.getBlockState(sample).equals(state),"A same-state ordinary player write revokes even genuinely witnessed clay");
            var before=NaturalMaterials.CODEC.encodeStart(JsonOps.INSTANCE,NaturalMaterials.get(l)).getOrThrow();l.getDataStorage().saveAndJoin();
            try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){l.getDataStorage().set(NaturalMaterials.TYPE,Objects.requireNonNull(disk.get(NaturalMaterials.TYPE)));}
            var sampleChunk=new ChunkPos(sample.getX()>>4,sample.getZ()>>4);NaturalPlacementGameTest.reloadChunk(l,sampleChunk);NaturalPlacementGameTest.reloadChunk(l,sampleChunk);
            c.assertTrue(before.equals(NaturalMaterials.CODEC.encodeStart(JsonOps.INSTANCE,NaturalMaterials.get(l)).getOrThrow())&&!NaturalMaterials.get(l).eligible(l,sample),"Disk readback and normal FULL chunk decoding preserve authority and cannot regrant a revoked clay record");
            c.assertTrue(clay.stream().filter(p->!p.equals(sample)).allMatch(p->NaturalMaterials.get(l).eligible(l,p)),"Untouched positive resource records survive the actual readback");
            PrimeAnts.LOGGER.info("CLAY ORDINARY CENSUS ACCEPTED seed=0 chunk={} clay={} dry={} candidateDry={} distinctRecipe={} playerSameStateRevoked={} disk=true chunkReadback=true foodAssistance=0 materialGifts=0 ordinaryColonyProgression=false positions={}",cp,clay.size(),dry.size(),inBounds.size(),walls.size()*NestWalls.CLAY_PER_CELL,sample,clay);
            l.getChunkSource().removeTicketWithRadius(PlacementSettings.TICKET,cp,3);c.succeed();
        });
    }
}
