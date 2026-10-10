package dev.primeants.founding;

import dev.primeants.colony.ColonyStage;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Prefix geometry is separate from physical server acceptance and does not authorize any world edit. */
class SurfacePrefixTest {
    private static boolean clear(Set<BlockPos> paid,BlockPos feet,int ceiling){
        return feet.getY()>0&&feet.getY()+1<ceiling&&!paid.contains(feet)&&!paid.contains(feet.above())
            &&(feet.getY()==1||paid.contains(feet.below()));
    }
    private static Set<BlockPos> connected(Set<BlockPos> paid,int ceiling){
        var reached=new HashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();queue.add(new BlockPos(-14,1,0));
        while(!queue.isEmpty()){
            var p=queue.remove();if(!clear(paid,p,ceiling)||!reached.add(p))continue;
            for(var delta:List.of(new BlockPos(1,0,0),new BlockPos(-1,0,0),new BlockPos(0,0,1),new BlockPos(0,0,-1)))for(int y=-1;y<=1;y++){
                var next=p.offset(delta).offset(0,y,0);if(next.getX()>=-14&&next.getX()<=-1&&Math.abs(next.getZ())<=12&&!reached.contains(next))queue.add(next);
            }
        }return reached;
    }
    private static boolean visible(Set<BlockPos> paid,BlockPos target,BlockPos stand){
        var body=new AABB(stand.getX()+0.2,stand.getY(),stand.getZ()+0.2,stand.getX()+0.8,stand.getY()+0.4,stand.getZ()+0.8);
        if(body.intersects(new AABB(target)))return false;var mouth=Vec3.atBottomCenterOf(stand).add(0,0.25,0);
        for(double y:new double[]{0.5,0.01,0.99})for(double x:new double[]{0.5,0.01,0.99})for(double z:new double[]{0.5,0.01,0.99}){
            var point=new Vec3(target.getX()+x,target.getY()+y,target.getZ()+z);if(mouth.distanceToSqr(point)>5.0)continue;
            boolean clipped=paid.stream().anyMatch(p->new AABB(p).clip(mouth,point).isPresent());if(!clipped)return true;
        }return false;
    }
    private static Set<BlockPos> stands(Set<BlockPos> paid,BlockPos target,int ceiling){
        var reachable=connected(paid,ceiling);var result=new HashSet<BlockPos>();
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(x!=0||z!=0)for(int y=-2;y<=1;y++){
            var stand=target.offset(x,y,z);if(reachable.contains(stand)&&visible(paid,target,stand))result.add(stand);
        }return result;
    }
    @Test void actualTwelvePaidPrefixesDecodeWithOriginalCellKeysReceiptsAndReleaseIdentities() throws Exception {
        JsonObject input;
        try(var stream=getClass().getResourceAsStream("/dev/primeants/founding/t12-paid-surface-prefixes.json")){
            assertNotNull(stream);input=JsonParser.parseString(new String(stream.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();
        }
        var data=SurfaceWork.CODEC.parse(JsonOps.INSTANCE,input).getOrThrow();
        var output=SurfaceWork.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow().getAsJsonObject();
        assertEquals(2,input.size());int releases=0;
        for(var entry:input.entrySet()){
            var original=JsonParser.parseString(entry.getValue().getAsString()).getAsJsonObject();
            assertEquals(original,JsonParser.parseString(output.get(entry.getKey()).getAsString()),"No lost unit, receipt identity, cell key, or reassigned payment");
            var owner=UUID.fromString(entry.getKey().split(":")[0]);var job=data.job(owner,ColonyStage.MATURE);
            assertEquals(15,job.completed());assertEquals(15,job.placed());assertEquals(15+job.released(),job.recovered());assertEquals(0,job.carried());assertNull(job.claim);
            assertEquals("-4,3,2",job.plan.cells().get(job.completed()).key());releases+=job.released();
        }assertEquals(1,releases,"The named T12 lethal release remains one historical release, separate from live custody");
        var forged=input.deepCopy();var first=forged.entrySet().iterator().next();var bad=JsonParser.parseString(first.getValue().getAsString()).getAsJsonObject();
        var receipt=bad.getAsJsonArray("receipts");receipt.set(14,new JsonPrimitive(receipt.get(14).getAsString().replace("-1,1,2","-5,3,2")));forged.addProperty(first.getKey(),bad.toString());
        assertThrows(IllegalArgumentException.class,()->SurfaceWork.CODEC.parse(JsonOps.INSTANCE,forged).getOrThrow(),"A payment cannot be forged onto the narrative's different cell");
    }
    @Test void everyPaidPrefixLeavesAReachableSupportedVisibleStandOnOpenDeclaredGround(){
        for(var stage:List.of(ColonyStage.MATURE,ColonyStage.GREAT)){
            var paid=new HashSet<BlockPos>();var plan=SurfacePlan.bundled(stage);
            for(var cell:plan.cells()){
                var target=new BlockPos(cell.forward(),1+cell.layer(),cell.side());
                assertFalse(stands(paid,target,8).isEmpty(),"Prefix "+paid.size()+" cannot reach the next task "+cell);
                paid.add(target);
                for(int f=-14;f<0;f++)assertTrue(clear(paid,new BlockPos(f,1,0),8),"The preserved two-high approach remains open");
            }
        }
    }
    @Test void frozenEightHighHabitatAtGroundFourRejectsTheRaisedPrefixBeforeAnyPathSearch(){
        var paid=new HashSet<BlockPos>();var plan=SurfacePlan.bundled(ColonyStage.MATURE);
        for(int i=0;i<15;i++){var c=plan.cells().get(i);paid.add(new BlockPos(c.forward(),1+c.layer(),c.side()));}
        var next=plan.cells().get(15);var target=new BlockPos(next.forward(),1+next.layer(),next.side());
        assertEquals("-4,3,2",next.key(),"T12's actual compiled receipt prefix, not its mislabelled narrative");
        assertFalse(stands(paid,target,8).isEmpty(),"The same prefix is accessible without the artificial ceiling");
        assertTrue(stands(paid,target,4).isEmpty(),"A ceiling four above local ground rejects all supported visible candidates");
    }
}
