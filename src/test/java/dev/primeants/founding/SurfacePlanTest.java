package dev.primeants.founding;

import dev.primeants.colony.ColonyStage;
import java.util.*;
import net.minecraft.core.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SurfacePlanTest {
    @Test void envelopesRemainCapacitiesWhilePaidSilhouettesDiffer(){
        assertEquals(214,NestMound.plan(ColonyStage.YOUNG).size());assertEquals(798,NestMound.plan(ColonyStage.MATURE).size());
        var young=SurfacePlan.bundled(ColonyStage.YOUNG);var mature=SurfacePlan.bundled(ColonyStage.MATURE);var great=SurfacePlan.bundled(ColonyStage.GREAT);
        assertEquals(0,young.cost());assertEquals(19,mature.cost());assertEquals(57,great.cost());
        assertEquals(4,1+mature.cells().stream().mapToInt(SurfacePlan.Cell::layer).max().orElseThrow());
        assertEquals(5,1+great.cells().stream().mapToInt(SurfacePlan.Cell::layer).max().orElseThrow());
        assertTrue(great.cells().stream().filter(c->c.component().equals("watch_post")&&c.layer()==4).map(SurfacePlan.Cell::side).distinct().count()==2);
        assertEquals(2,great.cells().stream().filter(c->c.material().equals("gate")).count());
        assertTrue(great.cells().stream().filter(c->c.component().equals("rampart")).count()==8);
    }
    @Test void deterministicUniqueSupportedOrderPreservesPassageAndProtectedStand(){
        for(var stage:List.of(ColonyStage.MATURE,ColonyStage.GREAT)){
            var p=SurfacePlan.bundled(stage);assertEquals(p,SurfacePlan.compile(p.description()));
            var seen=new HashSet<String>();
            for(var c:p.cells()){
                assertTrue(c.forward()<0&&c.forward()>=-14&&Math.abs(c.side())<=12&&c.layer()<6);
                assertFalse(c.forward()==-2&&c.side()==0);assertFalse(c.side()==0&&c.layer()<2);
                if(c.layer()>0)assertTrue(seen.contains(c.forward()+","+c.side()+","+(c.layer()-1))
                    ||c.component().equals("arch_lintel")&&seen.contains("-1,-1,2")&&seen.contains("-1,1,2"));
                assertTrue(seen.add(c.key()),"No duplicate task: "+c);
            }
        }
    }
    @Test void allCardinalRotationsPreserveUniqueFootprintAndWalkingLane(){
        for(var d:Direction.Plane.HORIZONTAL){
            var home=NestPlan.geometry(new BlockPos(20,7,31),d);var mapped=new HashSet<BlockPos>();
            for(var c:SurfacePlan.bundled(ColonyStage.GREAT).cells()){
                var p=home.at(c.forward(),c.side(),1+c.layer());assertTrue(mapped.add(p));
                assertFalse(p.equals(home.outside()));
            }
            for(int f=-14;f<=2;f++)for(int y:new int[]{1,2})if(f<0)assertFalse(mapped.contains(home.at(f,0,y)));
            assertEquals(57,mapped.size());
        }
    }
    @Test void rampartsConnectBothRaisedPostsToTheGateWithoutFillingPassage(){
        var cells=SurfacePlan.bundled(ColonyStage.GREAT).cells();
        for(int sign:new int[]{-1,1})for(int side=1;side<=4;side++){
            final int s=sign*side;assertTrue(cells.stream().anyMatch(c->c.forward()==-1&&c.side()==s&&c.layer()==0));
        }
        assertTrue(cells.stream().noneMatch(c->c.side()==0&&!c.component().equals("arch_lintel")));
    }
    @Test void invalidSemanticDimensionsPaletteAndOverlappingComponentsAreRejected(){
        var p=SurfacePlan.bundled(ColonyStage.GREAT).description();
        assertThrows(IllegalArgumentException.class,()->SurfacePlan.compile(p.replace("\"postHeight\":5","\"postHeight\":7")));
        assertThrows(IllegalArgumentException.class,()->SurfacePlan.compile(p.replace("\"palette\":\"earth\"","\"palette\":\"stone\"")));
        assertThrows(IllegalArgumentException.class,()->SurfacePlan.compile(p.replace("\"posts\":2","\"posts\":1")));
        assertThrows(IllegalArgumentException.class,()->SurfacePlan.compile(p.replace("\"width\":3","\"width\":2")));
    }
    @Test void raisedPlatformsHaveConnectedOneBlockStepsAndTwoHighStandingClearance(){
        for(var stage:List.of(ColonyStage.MATURE,ColonyStage.GREAT)){
            var cells=SurfacePlan.bundled(stage).cells();var heights=new HashMap<String,Integer>();
            for(var c:cells)heights.merge(c.column(),c.layer()+1,Math::max);
            var starts=new ArrayDeque<List<Integer>>();var reached=new HashSet<List<Integer>>();starts.add(List.of(-14,0));
            while(!starts.isEmpty()){
                var p=starts.remove();if(!reached.add(p))continue;int h=p.get(1)==0?0:heights.getOrDefault(p.get(0)+","+p.get(1),0);
                final int floor=h;assertTrue(cells.stream().noneMatch(c->c.forward()==p.get(0)&&c.side()==p.get(1)&&(c.layer()==floor||c.layer()==floor+1)),"Two-high standing clearance");
                for(var delta:List.of(List.of(1,0),List.of(-1,0),List.of(0,1),List.of(0,-1))){
                    var next=List.of(p.get(0)+delta.get(0),p.get(1)+delta.get(1));if(next.get(0)<-14||next.get(0)>-1||Math.abs(next.get(1))>12)continue;
                    // The lintel is a bridge, not a floor under the approach's open two-high passage.
                    int nh=next.get(1)==0?0:heights.getOrDefault(next.get(0)+","+next.get(1),0);
                    if(Math.abs(nh-h)<=1&&!reached.contains(next))starts.add(next);
                }
            }
            assertTrue(reached.contains(List.of(-4,3)),"The taller mound crest is physically accessible by one-block steps");
            if(stage==ColonyStage.GREAT)for(int side:new int[]{-4,4})assertTrue(reached.contains(List.of(-1,side)),"Both raised watch platforms have connected step access");
        }
    }
}
