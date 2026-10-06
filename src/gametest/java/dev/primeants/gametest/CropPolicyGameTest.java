package dev.primeants.gametest;

import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.storage.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.phys.Vec3;

public final class CropPolicyGameTest {
    private final WorkerForagingGameTest f=new WorkerForagingGameTest();
    private static net.minecraft.nbt.CompoundTag save(GameTestHelper c,LasiusNigerEntity a){var o=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,c.getLevel().registryAccess());a.save(o);return o.buildResult();}
    private static void fasting(GameTestHelper c,LasiusNigerEntity a,long ticks){var t=save(c,a).getCompoundOrEmpty("AdultLife");t.putLong("Fasting",ticks);t.putBoolean("Started",true);a.adultLife().load(TagValueInput.create(ProblemReporter.DISCARDING,c.getLevel().registryAccess(),t),a);}
    private static void balance(GameTestHelper c,LasiusNigerEntity a){var n=a.nutrition();c.assertTrue(n.gainedSugar()+n.receivedSugar()==n.sugar()+n.spentSugar()+n.givenSugar(),"Endpoint sugar conservation");}
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void hungryProteinNurseReceivesFullPhysicalCropActionAndCoherentRestore(GameTestHelper c){sharing(c,false,false);}
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void shortSocialInterruptionKeepsBuilderCargoAndClaim(GameTestHelper c){sharing(c,true,false);}
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void changedRecipientRefusesCommitWithoutLostCargoOrDuplicatedSugar(GameTestHelper c){sharing(c,false,true);}
    private void sharing(GameTestHelper c,boolean builder,boolean refusal){
        var q=f.start(c);boolean[] supplied={false},midRestored={false};boolean[] setup={false},disabled={false},restored={false};LasiusNigerEntity[] donor={null},recipient={null};int[] first={-1};UUID[] claim={null};
        c.onEachTick(()->{
            var l=c.getLevel();
            if(!setup[0]){
                var ws=f.workers(c,q);var p=q.founding().plan();var j=NestExpansion.get(l).job(q.getUUID());
                if(builder&&!supplied[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;new NursingGameTest().supply(c,q,6,8);}
                if(ws.size()<3||ws.stream().anyMatch(LasiusNigerEntity::isCallow)||!q.founding().ready()||builder&&j==null)return;
                var r=builder?ws.stream().filter(w->w.getUUID().equals(j.claim)&&w.getMainHandItem().is(Items.DIRT)).findFirst().orElse(null):ws.stream().filter(w->w.workerTasks().nursing()&&w.getMainHandItem().isEmpty()).findFirst().orElse(null);
                if(r==null)return;
                var d=ws.stream().filter(w->w!=r&&w.getMainHandItem().isEmpty()).findFirst().orElse(null);if(d==null)return;
                // Disclosed negative fixture: original emerged bodies, supported room, genuine
                // 20-tick adult ingestion; hungry nurse's protein is canonical equipment.
                for(var w:ws)if(w!=d&&w!=r)w.setNoAi(true);
                var pos=Vec3.atBottomCenterOf(p.at(4,-1,-2));r.teleportTo(pos.x,pos.y,pos.z);d.teleportTo(pos.x+1,pos.y,pos.z);r.setOnGround(true);d.setOnGround(true);
                if(!builder)q.setNoAi(true); // Refusing queen isolates receipt while the hungry protein nurse stays enabled.
                if(!builder)r.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.CHICKEN));
                d.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.APPLE));fasting(c,d,builder?12000:0);fasting(c,r,20000);
                donor[0]=d;recipient[0]=r;claim[0]=j==null?null:j.claim;setup[0]=true;
            }
            var d=donor[0];var r=recipient[0];balance(c,d);balance(c,r);
            if(d.workerTasks().sharing().actionTicks()>0){
                if(first[0]<0)first[0]=(int)c.getTick();
                c.assertTrue(!d.workerTasks().freeForConstruction()&&!r.workerTasks().freeForConstruction(),"Active social endpoint cannot be reassigned away from suspended work");
                if(!midRestored[0]||d.workerTasks().sharing().actionTicks()!=10)c.assertTrue(d.socialAction()&&r.socialAction(),"Both actual action endpoints synchronize model behavior");
                if(!refusal&&d.workerTasks().sharing().actionTicks()==10&&!midRestored[0]){
                    donor[0]=f.restore(c,d);midRestored[0]=true;c.assertTrue(donor[0].workerTasks().sharing().actionTicks()==10&&donor[0].nutrition().givenSugar()==0,"Coherent partial action resumes remaining loaded ticks without premature sugar");return;
                }
                if(refusal&&d.workerTasks().sharing().actionTicks()==10&&!disabled[0]){disabled[0]=true;r.setNoAi(true);c.runAfterDelay(30,()->{c.assertTrue(d.nutrition().givenSugar()==0&&r.nutrition().receivedSugar()==0&&r.getMainHandItem().is(Items.CHICKEN),"Disabled changed endpoint refuses with cargo and reserves intact");c.succeed();});}
            }
            if(refusal)return;
            if(d.nutrition().givenSugar()>0&&!restored[0]){
                c.assertTrue(first[0]>=0&&c.getTick()-first[0]>=19&&d.nutrition().apples()==1&&r.nutrition().consumedUnits()==0&&d.nutrition().givenSugar()==r.nutrition().receivedSugar()&&d.nutrition().sugar()>=CropSharing.RESERVE,"20 loaded ticks transfer existing ingested crop, no second ingestion and reserve retained");
                c.assertTrue(builder?r.workerTasks().construction()&&r.getMainHandItem().is(Items.DIRT)&&claim[0].equals(NestExpansion.get(l).job(q.getUUID()).claim):r.workerTasks().nursing()&&r.getMainHandItem().is(Items.CHICKEN),"Incompatible role, work and exact cargo survive social interruption: phase="+r.workerTasks().phase()+" cargo="+r.getMainHandItem()+" claim="+claim[0]);
                long given=d.nutrition().givenSugar(),received=r.nutrition().receivedSugar();
                donor[0]=f.restore(c,d);recipient[0]=f.restore(c,r);restored[0]=true;
                donor[0].workerTasks().sharing().tick(l);donor[0].workerTasks().sharing().tick(l);
                c.assertTrue(donor[0].nutrition().givenSugar()==given&&recipient[0].nutrition().receivedSugar()==received,"Coherent completed endpoint restoration and repeated callback do not replay transfer");
                c.runAfterDelay(5,()->{balance(c,donor[0]);balance(c,recipient[0]);c.assertTrue(recipient[0].adultLife().fasting()==0,"Only ordinary paid maintenance clears fasting after receipt");
                    var dead=recipient[0];dead.hurtServer(l,dead.damageSources().generic(),1000);dead.die(dead.damageSources().generic());
                    var row=com.google.gson.JsonParser.parseString(AdultHistory.get(l).records().get(dead.getUUID().toString())).getAsJsonObject().getAsJsonObject("nutrition");
                    c.assertTrue(row.get("sugarReceived").getAsLong()==received&&row.get("gainedSugar").getAsLong()+row.get("sugarReceived").getAsLong()==row.get("sugar").getAsLong()+row.get("spentSugar").getAsLong()+row.get("sugarGiven").getAsLong(),"Terminal accounting conserves transfers and repeated death keeps once-only receipt");c.succeed();});
            }
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void maintenancePartialPaymentsPersistAndLegacySpendingIsUnchanged(GameTestHelper c){
        var q=f.start(c);LasiusNigerEntity[] body={null};long[] baseAge={0},baseSpend={0};var seen=new HashSet<Integer>();boolean[] setup={false};
        c.onEachTick(()->{
            if(!setup[0]){
                var ws=f.workers(c,q);if(ws.size()!=3||ws.stream().anyMatch(LasiusNigerEntity::isCallow))return;
                body[0]=ws.getFirst();c.assertTrue(body[0].nutrition().ingest(new ItemStack(Items.APPLE),8000,16000),"Declared finite ingested-store fixture for coverage boundary");baseAge[0]=body[0].elapsedAgeTicks();baseSpend[0]=body[0].nutrition().spentSugar();setup[0]=true;return;
            }
            var a=body[0];var before=save(c,a);int remaining=a.adultLife().coverageRemaining();
            if(seen.add(remaining)){
                body[0]=f.restore(c,a);var restored=body[0];
                c.assertTrue(restored.adultLife().coverageRemaining()==remaining&&restored.nutrition().spentSugar()==a.nutrition().spentSugar()&&restored.elapsedAgeTicks()==a.elapsedAgeTicks(),"Each fractional paid-coverage checkpoint restores exactly");
                var legacy=before.getCompoundOrEmpty("AdultLife").copy();legacy.remove("CoverageRemaining");legacy.remove("CoveredTicks");restored.adultLife().load(TagValueInput.create(ProblemReporter.DISCARDING,c.getLevel().registryAccess(),legacy),restored);
                c.assertTrue(restored.adultLife().coverageRemaining()==0&&restored.adultLife().maintenanceTicks()==a.adultLife().maintenanceTicks(),"Legacy restore preserves historical payment and grants no free coverage");
                restored.adultLife().load(TagValueInput.create(ProblemReporter.DISCARDING,c.getLevel().registryAccess(),before.getCompoundOrEmpty("AdultLife")),restored);
            }
            if(seen.size()==4&&body[0].elapsedAgeTicks()-baseAge[0]>=4){var now=body[0];long ticks=now.elapsedAgeTicks()-baseAge[0];c.assertTrue(ticks>=4&&now.nutrition().spentSugar()-baseSpend[0]==(ticks+3)/4&&now.adultLife().fasting()==0,"One debit per four covered loaded ticks without age slowdown or free reload: ticks="+ticks+" spent="+(now.nutrition().spentSugar()-baseSpend[0])+" coverage="+now.adultLife().coverageRemaining()+" fast="+now.adultLife().fasting());c.succeed();}
        });
    }
    @GameTest
    public void flowWindowPausesWithQueenStockAndResumesWithoutInventingIncome(GameTestHelper c){
        var flow=new FoodLimitedGrowth();
        for(int t=0;t<=2400;t+=200)flow.observe(t,supply(t*20L,t*20L));
        c.assertTrue(flow.allows(supply(48000,48000)),"Actual recent receipt stream funds projected commitments");
        for(int t=2600;t<=5000;t+=200)flow.observe(t,supply(48000,48000));
        c.assertTrue(!flow.allows(supply(48000,48000))&&flow.reason().equals("recent_income_below_adult_commitments"),"Falling income pauses despite 8000 queen sugar and 16000 protein");
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,c.getLevel().registryAccess());flow.save(out);var copy=new FoodLimitedGrowth();copy.load(TagValueInput.create(ProblemReporter.DISCARDING,c.getLevel().registryAccess(),out.buildResult()));
        copy.observe(5200,supply(48000,48000));c.assertTrue(copy.recentSugar()==0,"Replayed absolute receipts and reload are not income");
        for(int t=5400;t<=7800;t+=200)copy.observe(t,supply(48000+(t-5200)*20L,48000+(t-5200)*20L));
        c.assertTrue(copy.allows(supply(100000,100000))&&copy.resumes()==2&&copy.pauses()==1,"Restored actual intake resumes funded gate, metadata spends or supplies nothing");
        var missing=new FoodLimitedGrowth.Supply(false,false,100000,100000,8000,16000,0,0,4,0);copy.observe(8000,missing);c.assertTrue(!copy.allows(missing)&&copy.observedTicks()==0,"Missing loaded data conservatively discards income window and requires full warmup");c.succeed();
    }
    private static FoodLimitedGrowth.Supply supply(long s,long p){return new FoodLimitedGrowth.Supply(true,false,s,p,8000,16000,0,0,4,0);}
    @GameTest(maxTicks=18000)
    public void eightUnreachableDropsCannotHideNinthBeforeNativeFallback(GameTestHelper c){drops(c,false);}
    @GameTest(maxTicks=18000)
    public void removedDropQueueContinuesAcrossSearchWindowAndRestore(GameTestHelper c){drops(c,true);}
    private void drops(GameTestHelper c,boolean boundary){
        var l=NaturalPlacementGameTest.level(c,"t20_food");var cp=NaturalPlacementGameTest.prepare(c,l,boundary?4480:4400);l.getChunkSource().addTicketWithRadius(PlacementSettings.TICKET,cp,3);
        boolean[] setup={false},continued={false};UUID[] ninth={null};LasiusNigerEntity[] actor={null};var hidden=new ArrayList<net.minecraft.world.entity.item.ItemEntity>();
        c.onEachTick(()->{
            if(!(l.getEntity(NaturalPlacementGameTest.uuid(l,cp)) instanceof LasiusNigerEntity q)||q.founding().plan()==null)return;
            var p=q.founding().plan();
            if(!setup[0])for(var w:NectarGameTest.workers(l,q))if(w.workerTasks().phase()==WorkerTasks.Phase.SEARCH){
                var near=p.at(-6,0,1);var far=p.at(-9,0,1);
                c.assertTrue(NestPlan.walkable(l,near)&&NestPlan.walkable(l,far),"Both candidate feet remain supported and walkable");
                for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=0;y<=3;y++)if(x!=0||z!=0||y==3)l.setBlock(near.offset(x,y,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
                c.assertTrue(NestPlan.walkable(l,near),"Enclosed drop is walkable but unreachable");
                for(int i=0;i<8;i++){var drop=new net.minecraft.world.entity.item.ItemEntity(l,near.getX()+.5,near.getY(),near.getZ()+.5,new ItemStack(Items.APPLE));drop.setPickUpDelay(0);drop.setUnlimitedLifetime();l.addFreshEntity(drop);hidden.add(drop);}
                var candidates=new ArrayList<net.minecraft.core.BlockPos>();for(int fwd=-12;fwd<=12;fwd++)for(int side=-12;side<=12;side++)candidates.add(p.at(fwd,side,1));
                far=candidates.stream().filter(cell->NestPlan.walkable(l,cell)&&w.position().distanceToSqr(Vec3.atBottomCenterOf(cell))>w.position().distanceToSqr(Vec3.atBottomCenterOf(near))+4).filter(cell->{var path=w.getNavigation().createPath(cell,0,48);return path!=null&&path.canReach();}).findFirst().orElseThrow(()->new AssertionError("Bounded positive reachable drop stand required"));
                var reachable=new net.minecraft.world.entity.item.ItemEntity(l,far.getX()+.5,far.getY(),far.getZ()+.5,new ItemStack(Items.CHICKEN));reachable.setPickUpDelay(0);reachable.setUnlimitedLifetime();l.addFreshEntity(reachable);ninth[0]=reachable.getUUID();actor[0]=w;setup[0]=true;
                var bad=w.getNavigation().createPath(near,0,48);var good=w.getNavigation().createPath(far,0,48);
                c.assertTrue((bad==null||!bad.canReach())&&good!=null&&good.canReach()&&w.distanceToSqr(hidden.getFirst())<w.distanceToSqr(reachable),"Eight nearer unreachable paths, ninth genuinely reachable: bad="+(bad!=null&&bad.canReach())+" good="+(good!=null&&good.canReach())+" worker="+w.position()+" near="+near+" far="+far);
                boolean nativeReady=false;for(var cell:net.minecraft.core.BlockPos.betweenClosed(p.outside().offset(-24,-3,-24),p.outside().offset(24,3,24)))if(NestPlan.loaded(l,cell)&&(NativePrey.get(l).ready(l,cell)||FlowerNectar.get(l).ready(l,cell))){nativeReady=true;break;}
                c.assertTrue(nativeReady,"Native fallback genuinely available in the same search box");break;
            }
            if(!setup[0])return;var w=actor[0];
            if(boundary&&!continued[0]){
                var o=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());w.workerTasks().save(o);var task=o.buildResult();
                if(task.getIntOr("DroppedCursor",0)>=8&&w.workerTasks().phase()==WorkerTasks.Phase.SEARCH){
                    hidden.getFirst().discard();task.putInt("SearchTicks",240);w.workerTasks().load(TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),task));continued[0]=true;
                }
            }
            c.assertTrue(w.workerTasks().flowerSource()==null,"Pending reachable dropped candidate receives turn before native-source fallback");
            if(ninth[0].equals(w.workerTasks().sourceId())||w.getMainHandItem().is(Items.CHICKEN)){
                c.assertTrue(!boundary||continued[0],"Cursor crosses bounded SEARCH return and coherent task restoration, removed identity cannot restart eight");c.succeed();
            }
        });
    }
    @GameTest
    public void bambooScopesOverlapAndCloseIndependently(GameTestHelper c){
        var l=c.getLevel();var a=c.absolutePos(new net.minecraft.core.BlockPos(1,1,1));var b=a.east();
        try(var first=BambooStageFixture.watch(c,l,a);var second=BambooStageFixture.watch(c,l,b);var overlap=BambooStageFixture.watch(c,l,a)){
            c.assertTrue(BambooStageFixture.holds(l,a)&&BambooStageFixture.holds(l,b),"Independent active fixture locations coexist");first.close();first.close();
            c.assertTrue(BambooStageFixture.holds(l,a)&&BambooStageFixture.holds(l,b),"Closing one token twice cannot clear another overlapping registration");overlap.close();c.assertTrue(!BambooStageFixture.holds(l,a)&&BambooStageFixture.holds(l,b),"Overlap release isolated from other fixture");
        }c.assertTrue(!BambooStageFixture.holds(l,a)&&!BambooStageFixture.holds(l,b),"All scopes clean up on completion; listener also owns failure cleanup");c.succeed();
    }
}
