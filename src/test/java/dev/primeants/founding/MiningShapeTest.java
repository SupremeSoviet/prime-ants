package dev.primeants.founding;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MiningShapeTest {
    @Test void galleryIsConnectedTwoHighAndRetainsEveryChamberWallAndFloor(){
        var cells=MiningShape.cells();assertEquals(16,cells.size());assertEquals(cells.size(),new HashSet<>(cells).size());
        var protectedCells=new HashSet<>(NestBlueprint.foundingSpace());
        for(var placement:NestBlueprint.MATERIAL_STORE){protectedCells.addAll(placement.room().cells());protectedCells.addAll(NestWalls.walls(placement.room().cells()));}
        for(var placement:NestBlueprint.QUEENS_HALL){protectedCells.addAll(placement.room().cells());protectedCells.addAll(NestWalls.walls(placement.room().cells()));}
        var reachable=new HashSet<NestBlueprint.Cell>();reachable.add(new NestBlueprint.Cell(7,0,-2));reachable.add(new NestBlueprint.Cell(7,0,-1));
        for(var c:cells){assertTrue(MiningShape.within(c.forward(),c.dy(),c.side()));assertFalse(protectedCells.contains(c));assertTrue(c.neighbors().stream().anyMatch(reachable::contains));reachable.add(c);}
        for(var c:cells)if(c.dy()==-2){assertTrue(cells.contains(new NestBlueprint.Cell(c.forward(),0,-1)));assertFalse(cells.contains(new NestBlueprint.Cell(c.forward(),0,-3)));}
    }
    @Test void limitsRejectRadialCornersSurfaceAndSeventhDepth(){
        assertTrue(MiningShape.within(16,-1,0));assertTrue(MiningShape.within(0,-6,-16));
        assertFalse(MiningShape.within(16,-2,1));assertFalse(MiningShape.within(17,-2,0));assertFalse(MiningShape.within(0,0,0));assertFalse(MiningShape.within(0,-7,0));assertEquals(64,MiningShape.MAX_EDITS);
    }
    @Test void naturalBlockUnitMappingHasExactlySevenSingleProducers(){
        assertEquals(Map.of("minecraft:stone","minecraft:cobblestone","minecraft:clay","minecraft:clay_ball","minecraft:gravel","minecraft:gravel","minecraft:sand","minecraft:sand","minecraft:coal_ore","minecraft:coal","minecraft:copper_ore","minecraft:raw_copper","minecraft:iron_ore","minecraft:raw_iron"),MiningShape.UNITS);
    }
    @Test void anUnavailableNextFaceIsUnknownAndNeverHidesALoadedGalleryBreach(){
        var cells=MiningShape.cells();var dug=cells.subList(0,2);var next=cells.get(2);
        var connections=Set.of(new NestBlueprint.Cell(7,0,-2),new NestBlueprint.Cell(7,0,-1));
        var broken=new HashSet<NestBlueprint.Cell>();
        DugSpace.Ground<NestBlueprint.Cell> ground=new DugSpace.Ground<>(){
            public boolean loaded(NestBlueprint.Cell p){return !p.equals(next);}
            public boolean open(NestBlueprint.Cell p){return dug.contains(p);}
            public boolean closed(NestBlueprint.Cell p){return !broken.contains(p);}
            public NestBlueprint.Cell face(NestBlueprint.Cell p,int n){return p.neighbors().get(n);}
        };
        var unknown=new Findings();DugSpace.scan(ground,dug,connections,cells,DugSpace.Labels.of("mining"),unknown);
        assertEquals(Findings.Verdict.UNKNOWN,unknown.verdict());assertEquals("mining_chunk_unavailable",unknown.problem());
        broken.add(new NestBlueprint.Cell(8,1,-2));
        var damaged=new Findings();DugSpace.scan(ground,dug,connections,cells,DugSpace.Labels.of("mining"),damaged);
        assertEquals(Findings.Verdict.DAMAGED,damaged.verdict());assertEquals("mining_shell_or_support_open",damaged.problem());
    }
}
