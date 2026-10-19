package dev.primeants.worker;

import dev.primeants.brood.BroodPile;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Bounded physical home cells, independent of mouth contact and line of sight.
 * Called only after the nursery's owned geometry has CLEAR live Findings. */
public final class AggregateHomeDomain {
    private AggregateHomeDomain(){}
    public static boolean contains(ServerLevel l,BroodPile pile,LasiusNigerEntity a){
        var home=pile.catchupPlan();var owner=pile.queenId();var feet=a.blockPosition();
        if(!pile.ownedBy(owner,home)||!NestPlan.loaded(l,feet)||!l.isPositionEntityTicking(feet))return false;
        var cells=new HashSet<BlockPos>();
        for(int f=3;f<=5;f++)for(int s=-1;s<=1;s++)cells.add(home.at(f,s,-2));
        for(int f=0;f<3;f++)cells.add(home.at(f,0,-f));
        // Supported entrance and the same bounded exterior footprint declared by NestPlan.
        // Only cells connected by adjacent supported columns at a step of at most one block.
        var exterior=new HashSet<BlockPos>();
        for(int f=-5;f<=-1;f++)for(int s=-4;s<=4;s++)for(int y=0;y<=2;y++){
            var p=home.at(f,s,y);if(loadedStand(l,p))exterior.add(p);
        }
        // The excavated entrance is a supported stair one block below the exterior.
        // Its above cell has no floor after excavation and must not be our only seed.
        if(loadedStand(l,home.entrance())){
            var queue=new ArrayDeque<BlockPos>();queue.add(home.entrance());
            if(loadedStand(l,home.entrance().above()))queue.add(home.entrance().above());
            var visited=new HashSet<BlockPos>();
            while(!queue.isEmpty()){
                var p=queue.remove();if(!visited.add(p))continue;cells.add(p);
                for(var n:exterior)if(Math.abs(p.getX()-n.getX())+Math.abs(p.getZ()-n.getZ())==1&&Math.abs(p.getY()-n.getY())<=1&&!visited.contains(n))queue.add(n);
            }
        }
        var extension=NestExpansion.get(l).job(owner);
        if(extension!=null&&sameHome(home,extension.home))cells.addAll(NestExpansion.get(l).circulationSpace(l,owner));
        // Completed owned excavation cells also cover connected added halls and stores.
        for(var j:ChamberExcavation.get(l).jobs(owner))if(sameHome(home,j.home)&&j.problem(l,owner)==null)
            for(var p:j.completed())if(j.completed().contains(p.above())&&loadedStand(l,p)&&ColonyTerrain.get(l).openingIntact(l,p,owner))cells.add(p);
        return cells.contains(feet);
    }
    private static boolean loadedStand(ServerLevel l,BlockPos p){return NestPlan.loaded(l,p)&&NestPlan.loaded(l,p.above())&&NestPlan.loaded(l,p.below())&&NestPlan.walkable(l,p);}
    private static boolean sameHome(NestPlan a,NestPlan b){return a.entrance().equals(b.entrance())&&a.direction()==b.direction();}
    public static boolean recipient(ServerLevel l,BroodPile pile,LasiusNigerEntity a){
        return l.getEntity(a.getUUID())==a&&a.isAlive()&&!a.isRemoved()&&!a.isNoAi()
            &&pile.queenId().equals(a.colonyIdentity())
            &&(a.form()==AntForm.QUEEN&&a.getUUID().equals(pile.queenId())||ColonyMembers.get(l).belongs(a,pile.queenId(),pile.catchupPlan().chamber()))
            &&contains(l,pile,a);
    }
}
