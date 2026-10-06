package dev.primeants.gametest;

import com.mojang.serialization.JsonOps;
import dev.primeants.PrimeAnts;
import dev.primeants.brood.BroodPile;
import dev.primeants.colony.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.NestPlan;
import dev.primeants.founding.QueenFounding;
import dev.primeants.worker.ColonyMembers;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;

/** Production egg founding, dropped food and real loaded ticks. The stage is only ever evaluated by the colony's own
 * nursery; tests read the registry. Fixtures are real damage, entity/block-entity/SavedData restores, saved tags, real
 * blocks and one injected unavailable cell (UnavailableCells). */
public final class ColonyStageGameTest {
    private final WorkerForagingGameTest f=new WorkerForagingGameTest();
    private final NursingGameTest food=new NursingGameTest();
    private static ChamberRegistry.Colony colony(GameTestHelper c,LasiusNigerEntity q){return ChamberRegistry.get(c.getLevel()).colony(q.getUUID());}
    /** Queen + living-or-unknown workers + brood reservations: what the cap limits. */
    private static long committed(GameTestHelper c,LasiusNigerEntity q,BroodPile p){return 1+ColonyMembers.get(c.getLevel()).occupied(q.getUUID())+p.records().size();}
    private static BroodPile restorePile(GameTestHelper c,BroodPile p,CompoundTag tag){
        var pos=p.getBlockPos();var state=p.getBlockState();c.getLevel().removeBlockEntity(pos);
        var r=(BroodPile)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,state,tag,c.getLevel().registryAccess());
        c.assertTrue(r!=null,"Normal pile restore");c.getLevel().setBlockEntity(r);return r;
    }
    private static CompoundTag save(GameTestHelper c,Entity e){var o=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,c.getLevel().registryAccess());c.assertTrue(e.save(o),"Normal entity save");return o.buildResult();}
    private static LasiusNigerEntity load(GameTestHelper c,CompoundTag tag){return (LasiusNigerEntity)EntityType.loadEntityRecursive(TagValueInput.create(ProblemReporter.DISCARDING,c.getLevel().registryAccess(),tag),c.getLevel(),EntitySpawnReason.LOAD,e->e);}
    /** Saved-data fixture: the only way to obtain a stale saved stage is to save one. */
    private static void saveStaleStage(GameTestHelper c,UUID queen,ColonyStage stage){
        var json=ChamberRegistry.CODEC.encodeStart(JsonOps.INSTANCE,ChamberRegistry.get(c.getLevel())).getOrThrow().getAsJsonArray();
        for(var e:json)if(e.getAsJsonObject().get("queen").getAsString().equals(queen.toString()))e.getAsJsonObject().addProperty("stage",stage.serializedName());
        c.getLevel().getDataStorage().set(ChamberRegistry.TYPE,ChamberRegistry.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow());
    }
    private static ColonyDevelopment.ChamberState founding(ColonyDevelopment.Evaluation e){return e.chambers().stream().filter(s->s.id().equals(ChamberRegistry.FOUNDING)).findFirst().orElseThrow();}
    private static Map<ChamberFunction,ColonyDevelopment.Presence> both(ColonyDevelopment.Presence p){return Map.of(ChamberFunction.NURSERY,p,ChamberFunction.FOOD_STORE,p);}
    /** Young by the nursery's latest evaluation, both founding functions confirmed, no callow worker still maturing. */
    private boolean settledYoung(GameTestHelper c,LasiusNigerEntity q,BroodPile p){
        var colony=colony(c,q);var e=p.stageEvaluation();
        return colony!=null&&colony.stage()==ColonyStage.YOUNG&&e!=null&&e.result().certain()==ColonyStage.YOUNG&&founding(e).problem()==null
            &&founding(e).functions().equals(both(ColonyDevelopment.Presence.CONFIRMED))&&f.workers(c,q).stream().noneMatch(LasiusNigerEntity::isCallow);
    }

    @GameTest(maxTicks=24000,structure="prime_ants_test:idle_ground")
    public void eggFoundedColonyLaysBeyondTheFoundingCapOnlyAfterPromotionToYoung(GameTestHelper c){
        var q=f.start(c);boolean[] supplied={false},promoted={false};
        c.onEachTick(()->{
            var p=food.pile(c,q);if(p==null)return;
            if(!supplied[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;food.supply(c,q,10,8);}
            if(supplied[0])c.assertTrue(food.total(c,q)==18,"Eighteen real supplied food units are conserved");
            var colony=colony(c,q);if(colony==null)return;long committed=committed(c,q,p);
            if(c.getTick()%500==0)PrimeAnts.LOGGER.info("Stage-1 promotion trace tick={} stage={} committed={} condition={} evaluation={}",c.getTick(),colony.stage(),committed,p.condition(),p.stageEvaluation());
            if(colony.stage()==ColonyStage.FOUNDING){
                c.assertTrue(!promoted[0],"No regression without a real loss");
                c.assertTrue(committed<=ColonyStage.FOUNDING.adultCap(),"A Founding colony never lays beyond five adults including the queen: committed="+committed);return;
            }
            c.assertTrue(colony.stage()==ColonyStage.YOUNG,"Mature stays unreachable without a queen's hall and material store");
            if(!promoted[0]){
                promoted[0]=true;var e=p.stageEvaluation();
                c.assertTrue(e!=null&&e.stage()==ColonyStage.YOUNG&&e.inputs().adults().known()>=5&&ColonyMembers.get(c.getLevel()).occupied(q.getUUID())>=4
                    &&e.inputs().tier(ChamberFunction.NURSERY).known()==1&&e.inputs().tier(ChamberFunction.FOOD_STORE).known()==1&&f.cache(c,q)!=null,"Promotion rests on the queen, four real workers, the owned pile and the worker-placed cache: "+e);
                c.assertTrue(e.result().missing(ColonyStage.MATURE).stream().map(m->m.requirement().name()).toList().containsAll(List.of("queens_hall","material_store","clay")),"The evaluator reports what Mature still needs: "+e.result().missing());
                var founding=colony.chamber(ChamberRegistry.FOUNDING);
                c.assertTrue(colony.chambers().size()==1&&founding.tier()==1&&founding.functions().equals(EnumSet.of(ChamberFunction.NURSERY,ChamberFunction.FOOD_STORE)),"The 0.1.0 founding chamber is recognized as built: earthen nursery and food store only");
            }
            if(committed>ColonyStage.FOUNDING.adultCap()){
                PrimeAnts.LOGGER.info("Stage-1 PROMOTION queen={} committed={} records={} evaluation={}",q.getUUID(),committed,p.records().stream().map(r->r.id()+" "+r.stage()).toList(),p.stageEvaluation());c.succeed();
            }
        });
    }

    @GameTest(maxTicks=32000,structure="prime_ants_test:idle_ground")
    public void realWorkerDeathsRegressToFoundingAndTheColonyRegrowsToYoung(GameTestHelper c){
        var q=f.start(c);boolean[] supplied={false},killed={false},regressed={false},finished={false};Set<UUID> survivors=new HashSet<>(),victims=new HashSet<>(),laid=new HashSet<>();
        c.onEachTick(()->{
            if(finished[0])return;var l=c.getLevel();var p=food.pile(c,q);if(p==null)return;
            if(!supplied[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;food.supply(c,q,12,10);}
            var colony=colony(c,q);if(colony==null)return;var ws=f.workers(c,q);
            for(var r:p.records())if(laid.add(r.id())&&colony.stage()==ColonyStage.FOUNDING)
                c.assertTrue(committed(c,q,p)<=ColonyStage.FOUNDING.adultCap(),"An egg laid at Founding fits the five-adult cap: committed="+committed(c,q,p));
            if(c.getTick()%500==0)PrimeAnts.LOGGER.info("Stage-1 regression trace tick={} stage={} committed={} workers={} condition={} evaluation={}",c.getTick(),colony.stage(),committed(c,q,p),ws.size(),p.condition(),p.stageEvaluation());
            if(!killed[0]){
                // No cocoon may emerge before the next evaluation can observe the losses.
                if(colony.stage()!=ColonyStage.YOUNG||ws.stream().anyMatch(LasiusNigerEntity::isCallow)||p.records().stream().anyMatch(r->r.stage()==dev.primeants.brood.BroodStage.COCOON))return;
                killed[0]=true;
                // Keep the real forager and one nurse; every other worker takes real lethal damage through the normal death path.
                ws.stream().sorted(Comparator.comparing((LasiusNigerEntity w)->!q.founding().claimedBy(w)).thenComparing(w->w.getUUID().toString())).limit(2).forEach(w->survivors.add(w.getUUID()));
                for(var w:ws)if(!survivors.contains(w.getUUID())){victims.add(w.getUUID());w.hurtServer(l,w.damageSources().genericKill(),1000);}
                c.assertTrue(victims.size()>=2&&victims.stream().allMatch(id->ColonyMembers.get(l).member(id).dead()),"Each real lethal hit records a death");
                PrimeAnts.LOGGER.info("Stage-1 LETHAL DAMAGE queen={} victims={} survivors={} records={}",q.getUUID(),victims,survivors,p.records().size());return;
            }
            for(var id:survivors)c.assertTrue(l.getEntity(id) instanceof LasiusNigerEntity w&&w.isAlive()&&ColonyMembers.get(l).belongs(w,q.getUUID(),q.founding().plan().chamber()),"A lower cap never kills, evicts or removes surviving adults");
            if(!regressed[0]){
                if(colony.stage()==ColonyStage.YOUNG)return;
                var e=p.stageEvaluation();regressed[0]=true;
                c.assertTrue(colony.stage()==ColonyStage.FOUNDING&&e!=null&&e.stage()==ColonyStage.FOUNDING&&e.inputs().adults().possible()<5&&e.cap()==ColonyStage.FOUNDING.adultCap(),"Counted deaths regress to Founding with its five-adult cap: "+e);
                PrimeAnts.LOGGER.info("Stage-1 REGRESSION queen={} committed={} evaluation={}",q.getUUID(),committed(c,q,p),e);return;
            }
            if(colony.stage()==ColonyStage.YOUNG){
                var e=p.stageEvaluation();
                c.assertTrue(ws.size()>=4&&ws.stream().map(LasiusNigerEntity::getUUID).toList().containsAll(survivors)&&ws.stream().noneMatch(w->victims.contains(w.getUUID()))&&e.inputs().adults().known()>=5,"Regrowth to Young comes from new brood-derived workers: "+e);
                PrimeAnts.LOGGER.info("Stage-1 REGROWTH queen={} workers={} evaluation={}",q.getUUID(),ws.stream().map(LasiusNigerEntity::getUUID).toList(),e);finished[0]=true;c.succeed();
            }
        });
    }

    @GameTest(maxTicks=30000,structure="prime_ants_test:idle_ground")
    public void chamberRegistryAndStageSurviveReloadAndLegacyCapacityFieldsLoad(GameTestHelper c){
        LasiusNigerEntity[] q={f.start(c)};boolean[] supplied={false},started={false};
        c.onEachTick(()->{
            if(started[0])return;var l=c.getLevel();var p=food.pile(c,q[0]);if(p==null)return;
            if(!supplied[0]&&q[0].founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;food.supply(c,q[0],10,8);}
            var live=colony(c,q[0]);if(live==null||live.stage()!=ColonyStage.YOUNG||f.workers(c,q[0]).stream().anyMatch(LasiusNigerEntity::isCallow))return;
            started[0]=true;UUID id=q[0].getUUID();var plan=q[0].founding().plan();var founding=live.chamber(ChamberRegistry.FOUNDING);
            c.assertTrue(live.chambers().size()==1&&founding.markers().get(ChamberFunction.NURSERY).equals(plan.nursery())&&founding.markers().get(ChamberFunction.FOOD_STORE).equals(plan.cache()),"Registry holds the founding chamber with its pile and cache markers");
            // Fresh SavedData read from disk, then a queen and pile restore.
            l.getDataStorage().saveAndJoin();
            try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(net.minecraft.world.level.dimension.DimensionType.getStorageFolder(l.dimension(),l.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("data"),net.minecraft.util.datafix.DataFixers.getDataFixer(),l.registryAccess())){
                var saved=disk.get(ChamberRegistry.TYPE);c.assertTrue(saved!=null&&live.equals(saved.colony(id)),"Disk restores the same chambers and Young stage");l.getDataStorage().set(ChamberRegistry.TYPE,saved);
            }
            q[0]=f.restore(c,q[0]);restorePile(c,p,p.saveWithFullMetadata(l.registryAccess()));
            c.runAfterDelay(ColonyDevelopment.INTERVAL+20,()->{
                var registry=ChamberRegistry.get(l);var entry=registry.colony(id);var e=food.pile(c,q[0]).stageEvaluation();
                c.assertTrue(registry.colonies().stream().filter(k->k.queen().equals(id)).count()==1&&entry.chambers().size()==1&&entry.chambers().getFirst().equals(founding)&&entry.stage()==ColonyStage.YOUNG&&e!=null&&e.stage()==ColonyStage.YOUNG,"After reload: one colony entry, one founding chamber, live Young stage: "+e);
                saveStaleStage(c,id,ColonyStage.GREAT);var stale=food.pile(c,q[0]);restorePile(c,stale,stale.saveWithFullMetadata(l.registryAccess()));
                c.runAfterDelay(ColonyDevelopment.INTERVAL+20,()->{
                    c.assertTrue(colony(c,q[0]).stage()==ColonyStage.YOUNG&&food.pile(c,q[0]).stageEvaluation().stage()==ColonyStage.YOUNG,"A stale saved Great stage is recomputed from live state once loaded");
                    saveStaleStage(c,id,ColonyStage.FOUNDING);var low=food.pile(c,q[0]);restorePile(c,low,low.saveWithFullMetadata(l.registryAccess()));
                    c.runAfterDelay(ColonyDevelopment.INTERVAL+20,()->{
                        c.assertTrue(colony(c,q[0]).stage()==ColonyStage.YOUNG,"A stale saved Founding stage is recomputed upward from live state");
                        // Unloaded members neither promote nor demote. With every worker unloaded, at most three emerging
                        // callows can be known (four adults), so the colony is not certainly Young, nor certainly not.
                        var away=f.workers(c,q[0]);var copies=away.stream().map(w->save(c,w)).toList();away.forEach(w->w.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK));
                        c.runAfterDelay(ColonyDevelopment.INTERVAL+20,()->{
                            var held=food.pile(c,q[0]).stageEvaluation();
                            c.assertTrue(away.size()>=4&&colony(c,q[0]).stage()==ColonyStage.YOUNG&&held.inputs().adults().known()<5&&held.inputs().adults().possible()>=5
                                &&held.result().certain()==ColonyStage.FOUNDING&&held.result().possible()==ColonyStage.YOUNG,"Unloaded members hold the stage without promoting or demoting: "+held);
                            for(var tag:copies)c.assertTrue(l.tryAddFreshEntityWithPassengers(load(c,tag)),"Same unloaded identity returns");
                            // 0.1.0 capacity fields: the queen's ColonyAdultCapacity and the pile's AdultCapacity.
                            var queenTag=save(c,q[0]);c.assertTrue(queenTag.getInt("ColonyAdultCapacityBound").orElse(0)==AdultBound.MAX&&!queenTag.contains("ColonyAdultCapacity"),"Current saves write only the bound");
                            queenTag.remove("ColonyAdultCapacityBound");queenTag.putInt("ColonyAdultCapacity",7);var reduced=load(c,queenTag);
                            c.assertTrue(reduced!=null&&reduced.colonyAdultCapacity()==7,"A deliberate 0.1.0 reduction stays a reduction");
                            queenTag.putInt("ColonyAdultCapacity",30);q[0].discard();var legacyQueen=load(c,queenTag);
                            c.assertTrue(legacyQueen!=null&&legacyQueen.colonyAdultCapacity()==AdultBound.MAX&&l.tryAddFreshEntityWithPassengers(legacyQueen),"A 0.1.0 queen loads; her saved 30 becomes the full 120 bound");q[0]=legacyQueen;
                            var current=food.pile(c,q[0]);var pileTag=current.saveWithFullMetadata(l.registryAccess());
                            c.assertTrue(pileTag.getInt("AdultCapacityBound").orElse(0)==AdultBound.MAX&&!pileTag.contains("AdultCapacity"),"Current pile saves write only the bound");
                            pileTag.remove("AdultCapacityBound");pileTag.putInt("AdultCapacity",30);var legacyPile=restorePile(c,current,pileTag);long ticks=legacyPile.loadedTicks();
                            c.assertTrue(legacyPile.adultCapacity()==AdultBound.MAX&&AdultBound.effectiveCap(ColonyStage.MATURE,legacyPile.adultCapacity())==60,"A 0.1.0 pile's 30 becomes an upper bound, so the colony can pass 30 once Mature is reachable");
                            c.runAfterDelay(ColonyDevelopment.INTERVAL+20,()->{
                                var after=legacyPile.stageEvaluation();
                                c.assertTrue(legacyPile.loadedTicks()>ticks&&after!=null&&after.stage()==ColonyStage.YOUNG&&after.cap()==ColonyStage.YOUNG.adultCap()&&ChamberRegistry.get(l).colony(id).chambers().size()==1&&q[0].isAlive(),"Legacy queen and pile keep the live colony at Young: "+after);
                                PrimeAnts.LOGGER.info("Stage-1 PERSISTENCE queen={} registry={} evaluation={}",id,ChamberRegistry.get(l).colony(id),after);c.succeed();
                            });
                        });
                    });
                });
            });
        });
    }

    @GameTest(maxTicks=24000,structure="prime_ants_test:idle_ground")
    public void unavailableEntranceCellLeavesBothFunctionsUnknownAndTheColonyYoung(GameTestHelper c){
        var q=f.start(c);boolean[] supplied={false};int[] step={0};ColonyDevelopment.Evaluation[] last={null};UnavailableCells[] hidden={null};
        c.onEachTick(()->{
            var l=c.getLevel();var p=food.pile(c,q);if(p==null||step[0]==3)return;
            if(!supplied[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;food.supply(c,q,10,8);}
            var e=p.stageEvaluation();var cell=q.founding().plan().entrance();
            if(step[0]==0){
                if(!settledYoung(c,q,p))return;
                // Only availability changes: the real entrance cell stays an open, walkable block.
                c.assertTrue(NestPlan.loaded(l,cell)&&NestPlan.walkable(l,cell),"The real entrance cell is loaded and open");
                hidden[0]=UnavailableCells.hide(c,cell);last[0]=e;step[0]=1;return;
            }
            c.assertTrue(colony(c,q).stage()==ColonyStage.YOUNG,"Unavailable terrain never demotes the Young colony");
            if(e==last[0])return; // the nursery's own next evaluation
            var state=founding(e);last[0]=e;
            if(step[0]==1){
                c.assertTrue(!NestPlan.loaded(l,cell)&&NestPlan.walkable(l,cell)&&"enclosure_chunk_unavailable".equals(state.problem())&&state.tier()==0
                    &&state.functions().equals(both(ColonyDevelopment.Presence.UNKNOWN))&&e.stage()==ColonyStage.YOUNG&&e.result().certain()==ColonyStage.FOUNDING
                    &&e.result().possible()==ColonyStage.YOUNG&&e.inputs().adults().known()>=5&&e.cap()==ColonyStage.YOUNG.adultCap()
                    &&e.result().missing(ColonyStage.YOUNG).toString().equals("[nursery 0(+1?)/1, food_store 0(+1?)/1]"),"An unavailable entrance cell leaves both functions unknown and the colony Young: "+e);
                PrimeAnts.LOGGER.info("Stage-1 UNAVAILABLE ENTRANCE queen={} cell={} evaluation={}",q.getUUID(),cell,e);
                hidden[0].close();step[0]=2;return;
            }
            c.assertTrue(state.problem()==null&&state.functions().equals(both(ColonyDevelopment.Presence.CONFIRMED))&&e.result().certain()==ColonyStage.YOUNG
                &&e.result().missing(ColonyStage.YOUNG).isEmpty(),"The available cell confirms both functions again; nothing was lost: "+e);
            PrimeAnts.LOGGER.info("Stage-1 ENTRANCE AVAILABLE AGAIN queen={} evaluation={}",q.getUUID(),e);step[0]=3;c.succeed();
        });
    }

    @GameTest(maxTicks=24000,structure="prime_ants_test:idle_ground")
    public void loadedObstructedEntranceCellLosesBothFunctionsAndTheColonyDropsToFounding(GameTestHelper c){
        var q=f.start(c);boolean[] supplied={false};int[] step={0};ColonyDevelopment.Evaluation[] last={null};
        c.onEachTick(()->{
            var l=c.getLevel();var p=food.pile(c,q);if(p==null||step[0]==3)return;
            if(!supplied[0]&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;food.supply(c,q,10,8);}
            var e=p.stageEvaluation();var cell=q.founding().plan().entrance();
            if(step[0]==0){
                // The same entrance cell, loaded, takes a real block once no ant body overlaps it.
                if(!settledYoung(c,q,p)||!l.getEntitiesOfClass(LasiusNigerEntity.class,new AABB(cell)).isEmpty())return;
                c.assertTrue(l.getBlockState(cell).isAir()&&NestPlan.walkable(l,cell),"The entrance cell is open before the obstruction");
                l.setBlock(cell,Blocks.STONE.defaultBlockState(),3);last[0]=e;step[0]=1;return;
            }
            if(e==last[0])return; // the nursery's own next evaluation
            var state=founding(e);last[0]=e;
            if(step[0]==1){
                c.assertTrue(NestPlan.loaded(l,cell)&&!NestPlan.walkable(l,cell)&&"enclosure_route_obstructed".equals(state.problem())&&state.tier()==0
                    &&state.functions().equals(both(ColonyDevelopment.Presence.ABSENT))&&e.stage()==ColonyStage.FOUNDING&&e.result().possible()==ColonyStage.FOUNDING
                    &&colony(c,q).stage()==ColonyStage.FOUNDING&&e.inputs().adults().known()>=5&&e.cap()==ColonyStage.FOUNDING.adultCap()
                    &&e.result().missing(ColonyStage.YOUNG).toString().equals("[nursery 0/1, food_store 0/1]"),"A loaded, really obstructed entrance cell loses both functions and the colony drops to Founding with its adults alive: "+e);
                PrimeAnts.LOGGER.info("Stage-1 OBSTRUCTED ENTRANCE queen={} cell={} evaluation={}",q.getUUID(),cell,e);
                l.setBlock(cell,Blocks.AIR.defaultBlockState(),3);step[0]=2;return;
            }
            c.assertTrue(state.problem()==null&&state.functions().equals(both(ColonyDevelopment.Presence.CONFIRMED))&&e.stage()==ColonyStage.YOUNG&&colony(c,q).stage()==ColonyStage.YOUNG,
                "Clearing the obstruction confirms both functions and Young again: "+e);
            PrimeAnts.LOGGER.info("Stage-1 ENTRANCE CLEARED queen={} evaluation={}",q.getUUID(),e);step[0]=3;c.succeed();
        });
    }
}
