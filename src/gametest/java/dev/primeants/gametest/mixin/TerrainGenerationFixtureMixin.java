package dev.primeants.gametest.mixin;

import dev.primeants.founding.NaturalPlacement;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.*;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Isolated development generation fixtures only; never native experiment or historical loading. */
@Mixin(ChunkStatusTasks.class)
public abstract class TerrainGenerationFixtureMixin {
    @Inject(method="generateFeatures",at=@At("RETURN"),cancellable=true)
    private static void fixture(WorldGenContext context, ChunkStep step, StaticCache2D<GenerationChunkHolder> chunks,
            ChunkAccess chunk, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
        String dimension=context.level().dimension().identifier().toString();
        boolean plants=dimension.equals("prime_ants_test:t16_plants") || dimension.equals("prime_ants_test:t16_protected");
        boolean slope=dimension.equals("prime_ants_test:t16_step");
        boolean nectar=dimension.equals("prime_ants_test:t17_nectar");
        if(!plants && !slope && !nectar)return;
        long salt=context.level().getSeed()^UUID.nameUUIDFromBytes(dimension.getBytes(StandardCharsets.UTF_8)).getMostSignificantBits();
        int centerX=Math.floorDiv(chunk.getPos().x(),4)*4+1+(int)(salt&1);
        int centerZ=Math.floorDiv(chunk.getPos().z(),4)*4+1+(int)((salt>>>1)&1);
        int index=(int)(salt&63),ex=centerX*16+4+index/8,ez=centerZ*16+4+index%8;
        cir.setReturnValue(cir.getReturnValue().thenApply(generated -> {
            for(int x=chunk.getPos().getMinBlockX();x<=chunk.getPos().getMaxBlockX();x++)
                for(int z=chunk.getPos().getMinBlockZ();z<=chunk.getPos().getMaxBlockZ();z++) {
                    int top=generated.getHeight(Heightmap.Types.WORLD_SURFACE,x&15,z&15);
                    boolean late=Math.floorDiv(chunk.getPos().x(),4)==890 && Math.floorDiv(chunk.getPos().z(),4)==890;
                    // Isolated late-search source on the untouched side of the founding roof.
                    // Far fixtures also need the standard radius-3 diagnostic entity-ticking halo.
                    if(nectar && x==(late?ex+10:ex-3) && z==(late?ez-5:ez+3)) generated.setBlockState(new BlockPos(x,top+1,z),Blocks.POPPY.defaultBlockState(),0);
                    if(slope && z>ez) generated.setBlockState(new BlockPos(x,top+1,z),Blocks.DIRT.defaultBlockState(),0);
                    if(plants && (x==ex && z>=ez-2 && z<=ez || (x&3)==0 && (z&3)==0)) {
                        var plant=x==ex && z==ez?Blocks.SHORT_GRASS:x==ex && z==ez-1?Blocks.FERN:x==ex && z==ez-2?Blocks.POPPY:Blocks.WILDFLOWERS;
                        generated.setBlockState(new BlockPos(x,top+1,z),plant.defaultBlockState(),0);
                    }
                }
            return generated;
        }));
    }
}
