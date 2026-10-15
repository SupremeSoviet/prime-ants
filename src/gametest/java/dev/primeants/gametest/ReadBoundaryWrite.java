package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import dev.primeants.worker.ColonyMembers;
import dev.primeants.worker.MaterialStore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** One scoped real write during an actual delivery's final habitat read, never a production hook. */
public final class ReadBoundaryWrite implements AutoCloseable {
    private static ReadBoundaryWrite armed;
    private static ReadBoundaryWrite pending;
    private final ServerLevel level; private final BlockPos cell; private final LasiusNigerEntity queen;
    public LasiusNigerEntity carrier; public ItemStack cargo; public MaterialStore store; public int stock;
    public Boolean lookupAfterWrite;
    private ReadBoundaryWrite(ServerLevel l,BlockPos p,LasiusNigerEntity q){level=l;cell=p.immutable();queen=q;}
    public static ReadBoundaryWrite arm(ServerLevel l,BlockPos p,LasiusNigerEntity q){
        if(armed!=null)throw new IllegalStateException("Overlapping read-boundary fixture");
        return armed=new ReadBoundaryWrite(l,p,q);
    }
    public static void afterLoaded(ServerLevel l,BlockPos p){
        var a=armed;if(a==null||a.level!=l||!a.cell.equals(p))return;
        var frames=StackWalker.getInstance().walk(s->s.toList());
        if(frames.stream().noneMatch(f->f.getClassName().equals(ColonyTerrain.class.getName())&&f.getMethodName().equals("matches"))
                ||frames.stream().noneMatch(f->f.getClassName().equals(MaterialStore.class.getName())&&f.getMethodName().equals("deposit"))
                ||frames.stream().anyMatch(f->f.getClassName().equals("dev.primeants.worker.WorkerTasks")&&(f.getMethodName().equals("authorized")||f.getMethodName().equals("miningDeliveryAuthorized"))))return;
        var home=a.queen.founding().plan();var store=MaterialStore.owned(l,a.queen.getUUID(),home);
        var carrier=ColonyMembers.get(l).members(a.queen.getUUID()).stream().map(m->l.getEntity(m.worker())).filter(e->e instanceof LasiusNigerEntity w&&a.queen.founding().claimedBy(w)&&!w.getMainHandItem().isEmpty()).map(e->(LasiusNigerEntity)e).findFirst().orElseThrow();
        armed=null;a.carrier=carrier;a.cargo=carrier.getMainHandItem().copy();a.store=store;a.stock=store.size();
        l.setBlock(p,l.getBlockState(p),3);
        pending=a;
        PrimeAnts.LOGGER.info("T11 READ BOUNDARY WRITE queen={} actor={} cargo={} stock={}",a.queen.getUUID(),carrier.getUUID(),a.cargo,a.stock);
    }
    public static void afterMatches(ServerLevel l,BlockPos p,boolean value){var a=pending;if(a!=null&&a.level==l&&a.cell.equals(p)){a.lookupAfterWrite=value;pending=null;PrimeAnts.LOGGER.info("T11 READ BOUNDARY RESULT returned={}",value);}}
    @Override public void close(){if(armed==this)armed=null;if(pending==this)pending=null;}
}
