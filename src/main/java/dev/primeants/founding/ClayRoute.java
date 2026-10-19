package dev.primeants.founding;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;

/** Bounded two-high soil access to positively witnessed clay, with supported one-block steps. No writes. */
final class ClayRoute {
    private ClayRoute() { }
    static List<BlockPos> next(ServerLevel l, UUID owner, NestPlan home, List<BlockPos> existing) {
        return next(l,owner,home,existing,Set.of(home.at(7,0,-2),home.at(7,0,-1)));
    }
    /** Access air grants no removal permission; every planned solid cell still needs positive authority. */
    static Set<BlockPos> bankAccess(ServerLevel l,NestPlan home){
        var cells=new HashSet<BlockPos>();
        for(int x=-16;x<=16;x++)for(int z=-16;z<=16;z++)if(x*x+z*z<=256)
            for(int y=-5;y<=1;y++){
                var p=home.entrance().offset(x,y,z);
                if(NestPlan.loaded(l,p)&&NestPlan.loaded(l,p.above())&&NestPlan.walkable(l,p)&&l.getBlockState(p).isAir()&&l.getBlockState(p.above()).isAir()) {cells.add(p);cells.add(p.above());}
            }
        return Set.copyOf(cells);
    }
    static List<BlockPos> next(ServerLevel l, UUID owner, NestPlan home, List<BlockPos> existing,Set<BlockPos> access) {
        int remaining=MiningShape.MAX_EDITS-existing.size();
        if(remaining<1)return List.of();
        var protectedCells=new HashSet<>(NestWalls.OPENABLE);
        protectedCells.addAll(NestBlueprint.DEPOSITS);
        protectedCells.addAll(NestWalls.walls(NestBlueprint.FOUNDING_CHAMBER));
        for(var placements:List.of(NestBlueprint.MATERIAL_STORE,NestBlueprint.QUEENS_HALL))
            for(var placement:placements)protectedCells.addAll(NestWalls.walls(placement.room().cells()));
        var forbidden=new HashSet<BlockPos>();
        // Keep the complete nest's support columns intact, including below future rooms.
        for(var cell:protectedCells)forbidden.add(home.at(cell.forward(),cell.side(),-2));
        var open=new HashSet<>(existing);
        open.addAll(access);
        var queue=new ArrayDeque<List<BlockPos>>();
        var seen=new HashSet<BlockPos>();
        for(var p:open.stream().filter(p->open.contains(p.above())&&NestPlan.walkable(l,p)).sorted(Comparator.comparingLong(BlockPos::asLong)).toList()){
            queue.add(List.of(p));seen.add(p);
        }
        List<BlockPos> best=null;int distance=Integer.MAX_VALUE,bestClay=0;
        while(!queue.isEmpty()){
            var path=queue.removeFirst();var last=path.getLast();
            if(path.size()>distance)break;
            for(var direction:Direction.Plane.HORIZONTAL)for(int step:new int[]{0,-1,1}){
                var at=last.relative(direction).offset(0,step,0);
                if(!seen.add(at)||open.contains(at)&&open.contains(at.above())||forbidden.contains(at.atY(home.entrance().getY()-2))
                    ||at.getY()+1>=home.entrance().getY()
                    ||!MiningShape.within(at.getX()-home.entrance().getX(),at.getY()-home.entrance().getY(),at.getZ()-home.entrance().getZ()))continue;
                boolean valid=true;
                for(var cell:List.of(at,at.above())){
                    if(open.contains(cell))continue;
                    if(!NestPlan.loaded(l,cell)||!l.getFluidState(cell).isEmpty()
                        ||!(ColonyTerrain.get(l).eligible(l,cell,owner)
                            ||l.getBlockState(cell).is(Blocks.CLAY)&&NaturalMaterials.get(l).eligible(l,cell))) {valid=false;break;}
                    for(var face:Direction.values()){
                        var neighbor=cell.relative(face);
                        if(!NestPlan.loaded(l,neighbor)||!l.getFluidState(neighbor).isEmpty()
                            ||!open.contains(neighbor)&&!l.getBlockState(neighbor).isSolidRender())valid=false;
                    }
                    if(l.getBlockState(cell.above()).getBlock() instanceof FallingBlock)valid=false;
                }
                if(!valid)continue;
                var extended=new ArrayList<>(path);extended.add(at);
                var result=new ArrayList<BlockPos>();
                for(int n=1;n<extended.size();n++){
                    var column=extended.get(n);boolean down=column.getY()<extended.get(n-1).getY();
                    for(var cell:down?List.of(column.above(),column):List.of(column,column.above()))
                        if(!open.contains(cell)&&!result.contains(cell))result.add(cell);
                }
                if(result.size()>remaining)continue;
                if(l.getBlockState(at).is(Blocks.CLAY)||l.getBlockState(at.above()).is(Blocks.CLAY)){
                    int clay=(int)result.stream().filter(cell->l.getBlockState(cell).is(Blocks.CLAY)).count();
                    // Among equally short accesses, prefer more clay per actual edit. The cumulative cap includes soil.
                    if(best==null||clay*best.size()>bestClay*result.size()) {best=List.copyOf(result);bestClay=clay;distance=path.size();}
                    continue;
                }
                queue.add(List.copyOf(extended));
            }
        }
        return best==null?List.of():best;
    }
}
