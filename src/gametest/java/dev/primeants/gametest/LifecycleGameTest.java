package dev.primeants.gametest;

import dev.primeants.entity.*;
import dev.primeants.brood.*;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Controlled colony birth policies and environmental mob deaths; never native ecology. */
public final class LifecycleGameTest {
    private final WorkerForagingGameTest f=new WorkerForagingGameTest();
    private LasiusNigerEntity start(GameTestHelper c,long lifespan,long grace,int cap){
        var keys=List.of("prime_ants.adultLifespanTicks","prime_ants.adultFastingTicks","prime_ants.colonyAdultCapacity");var old=keys.stream().map(System::getProperty).toList();
        try{System.setProperty(keys.get(0),Long.toString(lifespan));System.setProperty(keys.get(1),Long.toString(grace));System.setProperty(keys.get(2),Integer.toString(cap));return f.start(c);}
        finally{for(int i=0;i<keys.size();i++)if(old.get(i)==null)System.clearProperty(keys.get(i));else System.setProperty(keys.get(i),old.get(i));}
    }
    private BroodPile pile(GameTestHelper c,LasiusNigerEntity q){return q.founding().plan()!=null&&c.getLevel().getBlockEntity(q.founding().plan().nursery()) instanceof BroodPile b?b:null;}
    private LasiusNigerEntity restore(GameTestHelper c,LasiusNigerEntity a){
        var l=c.getLevel();var o=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess());c.assertTrue(a.save(o),"Normal live entity save");
        return (LasiusNigerEntity)EntityType.loadEntityRecursive(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess(),o.buildResult()),l,EntitySpawnReason.LOAD,e->e);
    }
    @GameTest(maxTicks=26000,structure="prime_ants_test:idle_ground")
    public void environmentalChickenLootFundsNewWorkerThenQueenDeathEndsColony(GameTestHelper c){
        var q=start(c,7000,3500,5);boolean[] supplied={false},killed={false},finished={false},lootClosed={false};var original=new HashSet<UUID>();var all=new HashSet<UUID>();var loot=new HashMap<UUID,ItemStack>();
        var observedWorld=new HashMap<UUID,ItemEntity>();var expired=new HashMap<UUID,Integer>();
        var mobs=new ArrayList<net.minecraft.world.entity.animal.chicken.Chicken>();var hazards=new ArrayList<net.minecraft.core.BlockPos>();
        c.onEachTick(()->{
            if(finished[0])return;
            var p=q.founding().plan();var b=pile(c,q);var ws=f.workers(c,q);if(p==null||b==null)return;ws.forEach(w->all.add(w.getUUID()));
            if(!supplied[0]&&ws.size()==3&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){
                supplied[0]=true;ws.forEach(w->original.add(w.getUUID()));f.drop(c,p.at(-3,0,1),new ItemStack(Items.APPLE,6)); // Declared controlled sugar, separate from genuine world protein.
                for(int i=-1;i<=1;i++){
                    var cell=p.at(-6,i,1);c.assertTrue(c.getLevel().getBlockState(cell).isAir(),"Temporary controlled suffocation cell starts air");hazards.add(cell);c.getLevel().setBlock(cell,Blocks.STONE.defaultBlockState(),3);
                    var mob=EntityTypes.CHICKEN.create(c.getLevel(),EntitySpawnReason.COMMAND);c.assertTrue(mob!=null,"Real vanilla chicken");var pos=Vec3.atBottomCenterOf(cell);mob.setPos(pos.x,pos.y,pos.z);mob.setNoAi(true);mob.setPersistenceRequired();c.assertTrue(c.getLevel().addFreshEntity(mob),"Uninjured real mob inserted into controlled physical hazard");mobs.add(mob);
                }
            }
            if(!supplied[0])return;
            for(var item:c.getLevel().getEntitiesOfClass(ItemEntity.class,c.getBounds().inflate(8),i->i.isAlive()&&i.getItem().is(Items.CHICKEN))){
                observedWorld.putIfAbsent(item.getUUID(),item);if(!lootClosed[0])loot.putIfAbsent(item.getUUID(),item.getItem().copy());
            }
            observedWorld.forEach((id,item)->{if(!item.isAlive()&&item.getAge()>=6000&&!item.getItem().isEmpty()&&!expired.containsKey(id)){expired.put(id,item.getItem().getCount());dev.primeants.PrimeAnts.LOGGER.info("T18 actual vanilla food expiry item={} age={} count={}",id,item.getAge(),item.getItem().getCount());}});
            if(mobs.stream().allMatch(m->!m.isAlive())&&!hazards.isEmpty()){lootClosed[0]=true;hazards.forEach(cell->c.getLevel().setBlock(cell,Blocks.AIR.defaultBlockState(),3));hazards.clear();}
            long actual=loot.values().stream().mapToInt(ItemStack::getCount).sum();
            long worldRaw=c.getLevel().getEntitiesOfClass(ItemEntity.class,c.getBounds().inflate(8),i->i.isAlive()&&i.getItem().is(Items.CHICKEN)).stream().mapToInt(i->i.getItem().getCount()).sum();
            long carriedRaw=ws.stream().filter(w->w.getMainHandItem().is(Items.CHICKEN)).mapToInt(w->w.getMainHandItem().getCount()).sum();
            var cache=f.cache(c,q);long storedRaw=cache==null?0:cache.contents().stream().filter(s->s.is(Items.CHICKEN)).mapToInt(ItemStack::getCount).sum();
            long custodyRaw=TransferCustody.get(c.getLevel()).contents().stream().filter(t->t.stack().is(Items.CHICKEN)&&c.getBounds().inflate(8).contains(t.position())).mapToInt(t->t.stack().getCount()).sum();
            c.assertTrue(actual==worldRaw+carriedRaw+storedRaw+custodyRaw+q.nutrition().chickens()+b.consumedChickens()+expired.values().stream().mapToInt(Integer::intValue).sum(),"Actual environmental raw loot = world/cargo/cache/custody + terminal ingestion + witnessed vanilla expiry; releases are not new loot");
            if(!killed[0]&&ws.stream().anyMatch(w->!original.contains(w.getUUID()))){
                killed[0]=true;var added=ws.stream().filter(w->!original.contains(w.getUUID())).findFirst().orElseThrow();
                c.assertTrue(actual>0&&mobs.stream().allMatch(m->!m.isAlive())&&q.nutrition().chickens()>0&&b.consumedChickens()>0&&b.consumed().contains(added.broodId())&&!b.original().contains(added.broodId())&&b.records().isEmpty(),"Actual environmental raw chicken protein funds unique additional worker; all live brood completed within five-adult cap");
                q.hurtServer(c.getLevel(),q.damageSources().genericKill(),1000);c.assertTrue(!q.isAlive(),"Normal queen death after world-food worker birth");
                dev.primeants.PrimeAnts.LOGGER.info("T18 WORLD FOOD BIRTH queen={} mobs={} actualRawLoot={} controlledApples=6 originalWorkers={} additional={} brood={} adultCap=5 receiptsQueen={} receiptsBrood={}",q.getUUID(),mobs.stream().map(Entity::getUUID).toList(),actual,original,added.getUUID(),added.broodId(),q.nutrition().chickens(),b.consumedChickens());
            }
            if(killed[0])c.assertTrue(b.records().isEmpty(),"Queen death cannot lay another egg or renew dependent brood");
            if(killed[0]&&!finished[0]&&ws.isEmpty()){
                finished[0]=true;c.assertTrue(all.size()==4&&ColonyMembers.get(c.getLevel()).occupied(q.getUUID())==0&&all.stream().allMatch(id->ColonyMembers.get(c.getLevel()).member(id).dead()),"Four actual unique workers eventually die through persisted mortality");
                c.runAfterDelay(30,()->{c.assertTrue(all.stream().allMatch(id->c.getLevel().getEntity(id)==null)&&c.getLevel().getEntity(q.getUUID())==null&&b.records().isEmpty(),"Normal body removal yields true bounded controlled extinction");dev.primeants.PrimeAnts.LOGGER.info("T18 WORLD FOOD EXTINCTION queen={} members={} living=0 dependentBrood=0 actualRawLoot={} witnessedExpiry={}",q.getUUID(),all,actual,expired);c.succeed();});
            }
        });
    }
    @GameTest(maxTicks=22000,structure="prime_ants_test:idle_ground")
    public void restoredProteinCarrierStarvesReleasesCargoAndKeepsDeathReceipts(GameTestHelper c){
        var q=start(c,10000,6000,30);boolean[] supplied={false},saved={false},finished={false};UUID[] id={null};
        c.onEachTick(()->{
            var p=q.founding().plan();var ws=f.workers(c,q);if(p==null)return;
            if(!supplied[0]&&ws.size()==3&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;ws.stream().filter(w->!q.founding().claimedBy(w)).forEach(w->w.setNoAi(true));f.drop(c,p.at(-3,0,1),new ItemStack(Items.CHICKEN,7));}
            if(!saved[0]&&f.cache(c,q)!=null&&f.cache(c,q).size()==6)for(var w:ws)if(w.getMainHandItem().is(Items.CHICKEN)){
                saved[0]=true;id[0]=w.getUUID();var copy=restore(c,w);c.assertTrue(copy!=null&&copy.elapsedAgeTicks()==w.elapsedAgeTicks()&&copy.adultLife().fasting()==w.adultLife().fasting()&&ItemStack.matches(copy.getMainHandItem(),w.getMainHandItem()),"Real physical carrier restores exact clocks and cargo");w.discard();c.assertTrue(c.getLevel().tryAddFreshEntityWithPassengers(copy),"Same carrier identity reinserted for real mortality ticks");
            }
            if(saved[0]&&!finished[0]&&ColonyMembers.get(c.getLevel()).member(id[0]).dead()){
                finished[0]=true;c.assertTrue(q.founding().workerClaim()==null&&AdultHistory.get(c.getLevel()).records().containsKey(id[0].toString()),"Normal starvation releases forager claim, tombstone and terminal history");
                c.runAfterDelay(30,()->{
                    long world=c.getLevel().getEntitiesOfClass(ItemEntity.class,c.getBounds().inflate(8),i->i.isAlive()&&i.getItem().is(Items.CHICKEN)).stream().mapToInt(i->i.getItem().getCount()).sum();
                    long custody=TransferCustody.get(c.getLevel()).contents().stream().filter(t->t.stack().is(Items.CHICKEN)&&c.getBounds().inflate(8).contains(t.position())).mapToInt(t->t.stack().getCount()).sum();
                    c.assertTrue(c.getLevel().getEntity(id[0])==null&&f.cache(c,q).size()==6&&world+custody==1,"After ordinary body removal seven physical protein units remain six cached plus one custody/world release; never consumed as adult sugar");
                    var r=com.google.gson.JsonParser.parseString(AdultHistory.get(c.getLevel()).records().get(id[0].toString())).getAsJsonObject();c.assertTrue(r.get("cause").getAsString().equals("starvation")&&r.getAsJsonObject("nutrition").get("chickens").getAsLong()==0,"Terminal receipts survive body removal and distinguish cargo from ingestion");c.succeed();
                });
            }
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void queenDeathStopsLayingViableCocoonsEmergeAndAllAdultsDie(GameTestHelper c){
        var q=start(c,1800,600,30);boolean[] killed={false},finishing={false};long[] laying={0};var brood=new HashSet<UUID>();var born=new HashSet<UUID>();
        c.onEachTick(()->{
            var p=pile(c,q);if(p==null)return;
            if(!killed[0]&&p.records().size()==3&&p.records().stream().allMatch(r->r.stage()==BroodStage.COCOON)){
                killed[0]=true;p.records().forEach(r->brood.add(r.id()));laying[0]=p.lastLayingTick();
                c.assertTrue(q.bodyReserve()==0&&f.workers(c,q).isEmpty(),"Entire unchanged first-clutch budget spent; three genuine viable cocoons, no supplied workers");
                q.hurtServer(c.getLevel(),q.damageSources().genericKill(),1000);
                c.assertTrue(!q.isAlive()&&q.founding().phase()==QueenFounding.Phase.DEAD,"Normal queen damage/death callbacks");
            }
            if(!killed[0])return;
            c.assertTrue(p.lastLayingTick()==laying[0]&&p.records().stream().allMatch(r->brood.contains(r.id())),"No new egg identity or laying after queen death");
            f.workers(c,q).forEach(w->{born.add(w.getUUID());c.assertTrue(brood.contains(w.broodId()),"Post-death adult derives from supported viable cocoon");});
            if(!finishing[0]&&born.size()==3&&f.workers(c,q).isEmpty()&&p.records().isEmpty()){
                finishing[0]=true;
                c.assertTrue(p.consumed().containsAll(brood)&&ColonyMembers.get(c.getLevel()).occupied(q.getUUID())==0&&born.stream().allMatch(id->ColonyMembers.get(c.getLevel()).member(id).dead()),"All genuine emerged adults die naturally, no replacements or purged brood");
                c.runAfterDelay(30,()->{c.assertTrue(c.getLevel().getEntity(q.getUUID())==null&&born.stream().allMatch(id->c.getLevel().getEntity(id)==null)&&p.records().isEmpty(),"Normal corpse callbacks leave no living or dependent ants");dev.primeants.PrimeAnts.LOGGER.info("T18 EXTINCTION queen={} originalBrood={} postDeathWorkers={} living=0 dependentBrood=0 history={}",q.getUUID(),brood,born,AdultHistory.get(c.getLevel()).records());c.succeed();});
            }
        });
    }
    @GameTest(maxTicks=24000,structure="prime_ants_test:idle_ground")
    public void birthSelectedCapacityKeepsUnloadedIdentitiesAndDeathReleasesReservation(GameTestHelper c){
        var q=start(c,144000,24000,4);boolean[] supplied={false},checked={false};var ids=new HashSet<UUID>();
        c.onEachTick(()->{
            var p=pile(c,q);var ws=f.workers(c,q);if(p==null)return;ws.forEach(w->ids.add(w.getUUID()));
            if(!supplied[0]&&ws.size()==3&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;f.drop(c,q.founding().plan().at(-3,0,1),new ItemStack(Items.APPLE,3));f.drop(c,q.founding().plan().at(-4,0,1),new ItemStack(Items.CHICKEN,2));}
            if(!checked[0]&&supplied[0]&&q.nutrition().sugar()>=1000&&q.nutrition().protein()>=2000){
                checked[0]=true;c.assertTrue(p.adultCapacity()==4&&p.records().isEmpty()&&ColonyMembers.get(c.getLevel()).occupied(q.getUUID())==3,"Configured four adults including queen refuses further real brood reservations despite food");
                var copy=restore(c,q);c.assertTrue(copy!=null&&copy.colonyAdultCapacity()==4,"Birth cap persists across restoration");
                var data=ColonyMembers.get(c.getLevel());var unknown=ColonyMembers.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,ColonyMembers.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,data).getOrThrow()).getOrThrow();
                c.assertTrue(unknown.occupied(q.getUUID())==3&&ids.stream().allMatch(id->unknown.member(id)!=null&&!unknown.member(id).dead()),"Registry restoration alone retains three identities with no world lookup; absence cannot free capacity");
                var victim=ws.stream().filter(w->!q.founding().claimedBy(w)).findFirst().orElseThrow();var saved=restore(c,victim);long age=victim.elapsedAgeTicks();
                victim.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                c.assertTrue(c.getLevel().getEntity(victim.getUUID())==null&&ColonyMembers.get(c.getLevel()).occupied(q.getUUID())==3,"Actual unload-reason removal leaves absent identity occupying its slot");
                c.runAfterDelay(60,()->{
                    c.assertTrue(p.records().isEmpty()&&ColonyMembers.get(c.getLevel()).occupied(q.getUUID())==3&&!ColonyMembers.get(c.getLevel()).member(victim.getUUID()).dead(),"Real nursery ticks cannot reserve a slot while a member is unloaded");
                    c.assertTrue(saved.elapsedAgeTicks()==age&&c.getLevel().tryAddFreshEntityWithPassengers(saved),"Exact original identity restores with no unloaded age catch-up");
                    saved.hurtServer(c.getLevel(),saved.damageSources().genericKill(),1000);
                    c.assertTrue(ColonyMembers.get(c.getLevel()).member(saved.getUUID()).dead()&&ColonyMembers.get(c.getLevel()).occupied(q.getUUID())==2,"Actual death alone releases one adult slot");
                });
            }
            if(checked[0]&&!p.records().isEmpty()){
                c.assertTrue(ColonyMembers.get(c.getLevel()).occupied(q.getUUID())+p.records().size()+1<=4&&p.records().stream().allMatch(r->!p.original().contains(r.id()))&&ids.size()==3,"Released slot permits a unique food-funded reservation within cap; no fictitious members");c.succeed();
            }
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void legacyWorkerAgeAdoptsStableDefaultsOnceWithoutRenewal(GameTestHelper c){
        var q=f.start(c);boolean[] done={false};
        c.onEachTick(()->{
            if(done[0])return;var worker=f.workers(c,q).stream().filter(w->w.elapsedAgeTicks()>200).findFirst();if(worker.isEmpty())return;done[0]=true;
            var w=worker.get();var l=c.getLevel();var o=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess());c.assertTrue(w.save(o),"Actual worker snapshot");var tag=o.buildResult();tag.remove("AdultLife");
            // Only absent new schema is simulated; identity, age, health, reserves and cargo stay exact.
            var legacy=(LasiusNigerEntity)EntityType.loadEntityRecursive(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,l.registryAccess(),tag),l,EntitySpawnReason.LOAD,e->e);
            c.assertTrue(legacy!=null&&legacy.getUUID().equals(w.getUUID())&&legacy.elapsedAgeTicks()==w.elapsedAgeTicks()&&legacy.adultLife().fasting()==w.elapsedAgeTicks()&&legacy.adultLife().lifespan()==144000&&legacy.adultLife().grace()==24000,"Existing worker age retained; stable defaults adopted with conservative fasting");
            var again=restore(c,legacy);c.assertTrue(again!=null&&again.elapsedAgeTicks()==legacy.elapsedAgeTicks()&&again.adultLife().fasting()==legacy.adultLife().fasting()&&again.nutrition().consumedUnits()==legacy.nutrition().consumedUnits(),"Repeated restoration cannot renew life or fasting");c.succeed();
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void daylightZombieLootTravelsThroughCacheAndNursing(GameTestHelper c){
        var q=f.start(c);boolean[] spawned={false},picked={false},stored={false},nursing={false};var mobs=new ArrayList<net.minecraft.world.entity.monster.zombie.Zombie>();var loot=new HashMap<UUID,ItemStack>();
        c.onEachTick(()->{
            var ws=f.workers(c,q);var p=q.founding().plan();if(p==null)return;
            if(!spawned[0]&&ws.size()==3&&ws.stream().allMatch(w->!w.isCallow())&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){
                spawned[0]=true;c.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
                c.getLevel().clockManager().setTotalTicks(c.getLevel().dimensionType().defaultClock().orElseThrow(),6000);
                for(int i=0;i<3;i++){
                    var mob=EntityTypes.ZOMBIE.create(c.getLevel(),EntitySpawnReason.COMMAND);c.assertTrue(mob!=null,"Real vanilla zombie");
                    var pos=Vec3.atBottomCenterOf(p.at(-3,i-1,1));mob.setPos(pos.x,pos.y,pos.z);mob.setNoAi(true);mob.setPersistenceRequired();
                    c.assertTrue(mob.getHealth()==mob.getMaxHealth()&&mob.getMainHandItem().isEmpty()&&mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),"Uninjured vanilla mob with no supplied food or sun protection");
                    c.assertTrue(c.getLevel().addFreshEntity(mob),"Ordinary real mob insertion");mobs.add(mob);
                }
                dev.primeants.PrimeAnts.LOGGER.info("T18 controlled daylight zombies={} sourcePositions={} adultDefaults={}/{}",mobs.stream().map(Entity::getUUID).toList(),mobs.stream().map(Entity::position).toList(),ws.getFirst().adultLife().lifespan(),ws.getFirst().adultLife().grace());
            }
            if(!spawned[0])return;
            for(var item:c.getLevel().getEntitiesOfClass(ItemEntity.class,c.getBounds().inflate(8),i->i.isAlive()&&i.getItem().is(Items.ROTTEN_FLESH)))if(!loot.containsKey(item.getUUID())){
                c.assertTrue(mobs.stream().anyMatch(m->!m.isAlive()),"Actual inserted vanilla death loot follows environmental death");loot.put(item.getUUID(),item.getItem().copy());
                dev.primeants.PrimeAnts.LOGGER.info("T18 actual vanilla loot item={} stack={} position={}",item.getUUID(),item.getItem(),item.position());
            }
            for(var w:ws)if(w.getMainHandItem().is(Items.ROTTEN_FLESH)){picked[0]=true;c.assertTrue(loot.values().stream().anyMatch(s->ItemStack.isSameItemSameComponents(s,w.getMainHandItem())),"Actual loot components retained in mandibles");if(w.workerTasks().nursing())nursing[0]=true;}
            var cache=f.cache(c,q);if(cache!=null&&cache.contents().stream().anyMatch(s->s.is(Items.ROTTEN_FLESH)))stored[0]=true;
            long consumed=q.nutrition().flesh()+(pile(c,q)==null?0:pile(c,q).consumedFlesh());
            long world=c.getLevel().getEntitiesOfClass(ItemEntity.class,c.getBounds().inflate(8),i->i.isAlive()&&i.getItem().is(Items.ROTTEN_FLESH)).stream().mapToInt(i->i.getItem().getCount()).sum();
            long held=ws.stream().filter(w->w.getMainHandItem().is(Items.ROTTEN_FLESH)).mapToInt(w->w.getMainHandItem().getCount()).sum();
            long stock=cache==null?0:cache.contents().stream().filter(s->s.is(Items.ROTTEN_FLESH)).mapToInt(ItemStack::getCount).sum();
            // Sampling before vanilla merging may see both sources; inserted stacks are counted at first observation.
            long actual=loot.values().stream().mapToInt(ItemStack::getCount).sum();
            c.assertTrue(actual==world+held+stock+consumed,"Actual inserted flesh balance: "+actual+" = "+world+" + "+held+" + "+stock+" + "+consumed);
            if(consumed>0){
                c.assertTrue(picked[0]&&stored[0]&&nursing[0]&&q.nutrition().gainedSugar()==0&&q.nutrition().gainedProtein()==q.nutrition().flesh()*4000&&q.bodyReserve()==0,"World flesh physically picked, returned, cached, nursed; protein only, exhausted founding budget unchanged");
                var copy=restore(c,q);c.assertTrue(copy!=null&&copy.nutrition().flesh()==q.nutrition().flesh()&&copy.nutrition().protein()==q.nutrition().protein(),"Normal restoration preserves explicit actual-loot receipts");
                dev.primeants.PrimeAnts.LOGGER.info("T18 PROTEIN PASS zombies={} actualLoot={} world={} carried={} cache={} consumed={} recipient={} protein={} sources={}",mobs.stream().map(Entity::getUUID).toList(),actual,world,held,stock,consumed,q.getUUID(),q.nutrition().protein(),loot);c.succeed();
            }
        });
    }
    @GameTest(maxTicks=18000,structure="prime_ants_test:idle_ground")
    public void genuineUnfedWorkersStarveBeforeTheirBirthSelectedAgeLimit(GameTestHelper c){
        var q=start(c,6000,2000,30);var ids=new HashSet<UUID>();boolean[] jumped={false};
        c.onEachTick(()->{
            var ws=f.workers(c,q);ws.forEach(w->ids.add(w.getUUID()));
            for(var w:ws){c.assertTrue(w.adultLife().lifespan()==6000&&w.adultLife().grace()==2000,"Isolated birth policy; brood multiplier does not accelerate adult age");
                if(!jumped[0]&&w.elapsedAgeTicks()>200){jumped[0]=true;long age=w.elapsedAgeTicks(),fast=w.adultLife().fasting();c.getLevel().clockManager().setTotalTicks(c.getLevel().dimensionType().defaultClock().orElseThrow(),900000);c.assertTrue(age==w.elapsedAgeTicks()&&fast==w.adultLife().fasting(),"Daylight jumps do not age/starve adults");}
            }
            if(ids.size()==3&&ws.isEmpty()){
                var history=AdultHistory.get(c.getLevel()).records();for(var id:ids){var r=com.google.gson.JsonParser.parseString(history.get(id.toString())).getAsJsonObject();c.assertTrue(r.get("cause").getAsString().equals("starvation")&&r.get("age").getAsLong()<6000&&ColonyMembers.get(c.getLevel()).member(id).dead(),"Starvation earlier than finite lifespan, actual death tombstone, terminal receipts retained");}
                c.assertTrue(ColonyMembers.get(c.getLevel()).occupied(q.getUUID())==0,"Capacity releases only actual dead members");c.succeed();
            }
        });
    }
    @GameTest(maxTicks=22000,structure="prime_ants_test:idle_ground")
    public void physicalMealsSustainWorkersUntilAgeDeathAndPersistThroughRestore(GameTestHelper c){
        var q=start(c,6000,3000,30);boolean[] supplied={false},restored={false};var ids=new HashSet<UUID>();var fed=new HashSet<UUID>();
        c.onEachTick(()->{
            var ws=f.workers(c,q);ws.forEach(w->ids.add(w.getUUID()));var p=q.founding().plan();if(p==null)return;
            if(!supplied[0]&&ws.size()==3&&q.founding().lifecycle()==QueenFounding.Lifecycle.OPEN){supplied[0]=true;f.drop(c,p.at(-3,0,1),new ItemStack(Items.APPLE,12));}
            for(var w:ws)if(w.nutrition().apples()>0){fed.add(w.getUUID());
                if(!restored[0]&&w.adultLife().maintenanceTicks()>100){restored[0]=true;var copy=restore(c,w);c.assertTrue(copy!=null&&copy.elapsedAgeTicks()==w.elapsedAgeTicks()&&copy.adultLife().fasting()==w.adultLife().fasting()&&copy.adultLife().lifespan()==6000&&copy.adultLife().grace()==3000&&copy.adultLife().maintenanceTicks()==w.adultLife().maintenanceTicks()&&copy.nutrition().apples()==w.nutrition().apples()&&ItemStack.matches(copy.getMainHandItem(),w.getMainHandItem()),"Restoration preserves age, fasting, maintenance, real meals and cargo");
                    long age=copy.elapsedAgeTicks();w.discard();c.assertTrue(c.getLevel().tryAddFreshEntityWithPassengers(copy),"Original saved identity resumes real loaded ticks");
                    c.runAfterDelay(30,()->c.assertTrue(copy.elapsedAgeTicks()>age&&copy.adultLife().maintenanceTicks()>100,"Restored adult actually advances age and physical-food maintenance"));
                }
            }
            var history=AdultHistory.get(c.getLevel()).records();for(var id:fed)if(history.containsKey(id.toString())){
                var r=com.google.gson.JsonParser.parseString(history.get(id.toString())).getAsJsonObject();
                if(r.get("cause").getAsString().equals("age_limit")){c.assertTrue(restored[0]&&r.get("age").getAsLong()>=6000&&r.get("fasting").getAsLong()<3000&&ColonyMembers.get(c.getLevel()).member(id).dead(),"Physically fed worker lives past unfed grace and reaches age limit; normal tombstone");c.succeed();}
            }
        });
    }
}
