package dev.primeants.founding;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** Vanilla survival against a read-only single-state overlay. Never writes or loads terrain. */
public final class SupportSurvival {
    private SupportSurvival() {}
    private static final class Unavailable extends RuntimeException {}

    public static String problem(ServerLevel world, BlockPos changed, BlockState replacement) {
        if (!NestPlan.loaded(world,changed)) return "support_cells_unavailable_at_"+changed;
        // Default LevelReader methods must execute on the view so their nested reads see the
        // replacement too. Unrelated abstract reads delegate to the real world. This interface
        // has no terrain mutation API; no temporary write or neighbor update is performed.
        LevelReader view=(LevelReader)Proxy.newProxyInstance(LevelReader.class.getClassLoader(),new Class<?>[]{LevelReader.class},(proxy,method,args)->{
            if(args!=null)for(Object arg:args)if(arg instanceof BlockPos p&&!NestPlan.loaded(world,p))throw new Unavailable();
            String name=method.getName();
            if(args!=null&&args.length==1&&changed.equals(args[0])) {
                if(name.equals("getBlockState"))return replacement;
                if(name.equals("getFluidState"))return replacement.getFluidState();
                if(name.equals("getBlockEntity"))return null;
            }
            if(name.equals("getChunk")&&args!=null&&args.length==4) {
                var chunk=world.getChunk((int)args[0],(int)args[1],(ChunkStatus)args[2],false);
                if(chunk==null)throw new Unavailable();return chunk;
            }
            if(name.equals("hasChunk")&&args!=null&&args.length==2&&!world.hasChunk((int)args[0],(int)args[1]))throw new Unavailable();
            if(method.isDefault())return InvocationHandler.invokeDefault(proxy,method,args);
            try{return method.invoke(world,args);}catch(InvocationTargetException e){throw e.getCause();}
        });
        for(Direction direction:Direction.values()) {
            var neighbor=changed.relative(direction);
            if(!NestPlan.loaded(world,neighbor))return "support_cells_unavailable_at_"+neighbor;
            try {
                if(!world.getBlockState(neighbor).canSurvive(view,neighbor))
                    return "protected_support_survival_at_"+changed+"_neighbor_"+neighbor;
            }catch(Unavailable e){return "support_cells_unavailable_at_"+neighbor;}
        }
        return null;
    }
}
