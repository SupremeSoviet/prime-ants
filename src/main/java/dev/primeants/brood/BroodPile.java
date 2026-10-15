package dev.primeants.brood;

import dev.primeants.PrimeAnts;
import dev.primeants.colony.AdultBound;
import dev.primeants.colony.ColonyDevelopment;
import dev.primeants.entity.AntEntities;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.NestPlan;
import dev.primeants.time.SimulationTimeScale;
import dev.primeants.worker.Nutrition;
import dev.primeants.worker.ColonyMembers;
import dev.primeants.worker.WorkerTasks;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

/** Reusable physical nursery slots. Only the loaded server block-entity ticker advances brood or lays eggs. How many
 * slots and how fast brood develops follow the nursery's physical causes (BroodCapacity), read from the pile's own last
 * stage evaluation: one pile per colony, its brood in the queen's reach. */
public final class BroodPile extends BlockEntity {
    /** The founding clutch, and the 0.1.0 nursery's slots. */
    public static final int CAPACITY = BroodCapacity.FOUNDING_SLOTS;
    public static final long BASE_LAYING_TICKS=1200;
    public static long layingCadence(){return layingCadence(1);}
    public static long layingCadence(int hallTier){return new SimulationTimeScale(multiplier()).ticksForGameDays(BroodCapacity.layingTicks(hallTier)/24000.0);}
    /** The hall's live confirmed walls own the laying effect, including immediately after a player replacement. */
    public long layingInterval(ServerLevel l){return layingCadence(dev.primeants.colony.ColonyDevelopment.confirmedTier(l,queenId,plan,dev.primeants.founding.ChamberExcavation.HALL,dev.primeants.colony.ChamberFunction.QUEENS_HALL));}
    public static final long EGG_COST = 1000, LARVA_COST = 12000, MAX_RESERVE = CAPACITY * (EGG_COST + LARVA_COST);
    public static final double CARE_REACH_SQUARED = 2.25 * 2.25;
    private final dev.primeants.worker.FoodLimitedGrowth growth=new dev.primeants.worker.FoodLimitedGrowth();
    public dev.primeants.worker.FoodLimitedGrowth growth(){return growth;}
    private UUID queenId;
    private final dev.primeants.time.ColonyClock clock = new dev.primeants.time.ColonyClock();
    private dev.primeants.time.ReloadObservation reloadObservation;
    private final dev.primeants.time.ColonyCatchup catchup=new dev.primeants.time.ColonyCatchup();
    public dev.primeants.time.ColonyCatchup catchup(){return catchup;}
    /** Both ordinary lifecycle entry points capture this boundary before any nursery stamp. */
    public boolean resumeCatchup(ServerLevel server){
        long start=System.nanoTime(),invocations=catchup.invocations();
        // Reuse the ordinary owner observation before either reload ordering evaluates habitat or nursing.
        // The queen may already have opened the nest after this nursery's last saved step.
        if(server.getEntity(queenId) instanceof LasiusNigerEntity q&&q.founding().lifecycle()!=dev.primeants.founding.QueenFounding.Lifecycle.CLAUSTRAL)operational=true;
        var interval=clock.before(server.getGameTime());
        if(reloadBoundaryPending||interval.ticks()>0&&(reloadObservation==null||!reloadObservation.interval().afterGameTime().equals(interval.afterGameTime()))){reloadObservation=dev.primeants.time.ReloadObservation.capture(server,queenId,getBlockPos(),plan.chamber(),interval);reloadBoundaryPending=false;}
        long preNanos=System.nanoTime()-start;
        boolean completed=catchup.resume(server,this,interval);
        if(catchup.invocations()!=invocations){catchup.measuredInvocation(System.nanoTime()-start,preNanos);PrimeAnts.LOGGER.info("AUTOMATIC_CATCHUP_COST queen={} events={} pending={} nanos={} capture={} stage={} lifecycle={} commit={}",queenId,catchup.lastEvents(),catchup.pendingTicks(),catchup.lastNanos(),catchup.captureNanos(),catchup.stageNanos(),catchup.lifecycleNanos(),catchup.commitNanos());}
        return completed;
    }
    private boolean reloadBoundaryPending = true;
    private long lastOrdinaryGameTime=-1;
    private long restoredOrdinaryFloor=-1;
    /** Latest immutable pre-step capture for this instance; not an applied ledger or dormant queue. */
    public dev.primeants.time.ReloadObservation reloadObservation() { return reloadObservation; }
    public java.util.OptionalLong lastSimulationGameTime() { return clock.lastGameTime(); }
    /** Observe before the resumed ordinary tick. This never applies absence to biology or stock. */
    public dev.primeants.time.ColonyClock.Interval missedIntervalBefore(ServerLevel server) {
        return clock.before(server.getGameTime());
    }
    private void initializeClock() {
        if (queenId != null && level instanceof ServerLevel server && clock.initialize(server.getGameTime())) setChanged();
    }
    @Override public void setLevel(net.minecraft.world.level.Level level) {
        super.setLevel(level);
        initializeClock();
    }
    private NestPlan plan;
    private final List<BroodRecord> records = new ArrayList<>();
    // Bounded consumed identities prevent replacement after death and reconcile saved-cocoon/live-adult overlap.
    private final Set<UUID> consumed = new HashSet<>();
    private final Set<UUID> original=new HashSet<>();
    private final java.util.Map<UUID,String> expired=new java.util.HashMap<>();
    private long neglectGrace=BroodRecord.DEFAULT_NEGLECT,waitingBound=BroodRecord.DEFAULT_COCOON_WAIT;
    public java.util.Map<UUID,String> expired(){return java.util.Map.copyOf(expired);}
    private long terminalUnits(String item){return expired.values().stream().mapToLong(r->BroodHistory.units(r,item)).sum();}
    public boolean firstClutchViable(ServerLevel server){return records.stream().anyMatch(r->original.contains(r.id())&&!expired.containsKey(r.id())&&BroodHistory.get(server).terminal(r.id())==null);}
    private boolean originalTerminal(){return original.stream().allMatch(id->consumed.contains(id)||expired.containsKey(id));}
    private boolean operational;
    private boolean generatedInitial;
    public void initializeGenerated(LasiusNigerEntity queen,NestPlan p){
        if(!(level instanceof ServerLevel l)||queenId!=null||!getBlockPos().equals(p.nursery())||!dev.primeants.founding.EstablishedGeneration.initializing(l,queen.getUUID(),p))throw new IllegalStateException("Generated nursery initialization authority");
        queenId=queen.getUUID();plan=p.routeGeometry();operational=true;generatedInitial=true;
        neglectGrace=queen.broodNeglectGrace();waitingBound=queen.cocoonWaitingBound();adultLifespan=queen.adultLife().lifespan();fastingGrace=queen.adultLife().grace();adultCapacity=queen.colonyAdultCapacity();condition="generated_initial_empty_nursery";changed();
    }
    private long adultLifespan=dev.primeants.entity.AdultLife.DEFAULT_LIFESPAN,fastingGrace=dev.primeants.entity.AdultLife.DEFAULT_FASTING;
    private int adultCapacity=AdultBound.MAX;
    /** Birth-selected upper bound only. The colony stage cap owns the adult limit (queen + workers + brood). */
    public int adultCapacity(){return adultCapacity;}
    // Transient evaluation cache: re-evaluated after a birth/death, a replaced registry, or INTERVAL loaded ticks.
    private ColonyDevelopment.Evaluation stageEvaluation;
    private long stageEvaluatedAt,stageMembersRevision;
    private Object stageMembers,stageRegistry;
    /** Last live stage evaluation, without evaluating; null before this pile's first decision or slow-cadence check. */
    public ColonyDevelopment.Evaluation stageEvaluation(){return stageEvaluation;}
    /** This pile's loaded tick of its last stage evaluation: the colony's hysteresis clock (StageRules) reads these ticks. */
    public long stageEvaluatedAt(){return stageEvaluatedAt;}
    private ColonyDevelopment.Evaluation development(ServerLevel l){
        var members=ColonyMembers.get(l);var registry=dev.primeants.colony.ChamberRegistry.get(l);
        if(stageEvaluation==null||members!=stageMembers||members.revision()!=stageMembersRevision||registry!=stageRegistry||!catchup.active()&&loadedTicks-stageEvaluatedAt>=ColonyDevelopment.INTERVAL){
            long start=System.nanoTime();
            stageEvaluation=ColonyDevelopment.evaluate(l,queenId,plan,operational,adultCapacity,loadedTicks);
            stageEvaluatedAt=loadedTicks;stageMembers=members;stageMembersRevision=members.revision();stageRegistry=registry;
            catchup.stageCost(System.nanoTime()-start);
        }
        return stageEvaluation;
    }
    private int adultCap(ServerLevel l){return AdultBound.effectiveCap(development(l).stage(),adultCapacity);}
    private long lastLayingTick,archivedApples,archivedBerries,archivedChickens,archivedNectar,archivedNectarV2,archivedPrey,archivedFlesh;
    public Set<UUID> original(){return Set.copyOf(original);}
    public boolean operational(){return operational;}
    public long lastLayingTick(){return lastLayingTick;}
    public long consumedApples(){return archivedApples+terminalUnits("apples")+records.stream().mapToLong(r->r.nutrition().apples()).sum();}
    public long consumedBerries(){return archivedBerries+terminalUnits("berries")+records.stream().mapToLong(r->r.nutrition().berries()).sum();}
    public long consumedChickens(){return archivedChickens+terminalUnits("chickens")+records.stream().mapToLong(r->r.nutrition().chickens()).sum();}
    public long consumedNectar(){return archivedNectar+terminalUnits("nectar")+records.stream().mapToLong(r->r.nutrition().nectar()).sum();}
    public long consumedNectarV2(){return archivedNectarV2+terminalUnits("nectarV2")+records.stream().mapToLong(r->r.nutrition().nectarV2()).sum();}
    public long consumedPrey(){return archivedPrey+terminalUnits("prey")+records.stream().mapToLong(r->r.nutrition().prey()).sum();}
    public long consumedFlesh(){return archivedFlesh+terminalUnits("flesh")+records.stream().mapToLong(r->r.nutrition().flesh()).sum();}
    public long consumedFood(){return consumedApples()+consumedBerries()+consumedChickens()+consumedNectar()+consumedPrey()+consumedFlesh();}
    public long gainedSugar(){return consumedApples()*Nutrition.APPLE_SUGAR+consumedBerries()*Nutrition.BERRY_SUGAR+consumedNectar()*Nutrition.NECTAR_SUGAR+consumedNectarV2()*(Nutrition.NECTAR_V2_SUGAR-Nutrition.NECTAR_SUGAR);}
    public long gainedProtein(){return consumedChickens()*Nutrition.CHICKEN_PROTEIN+consumedFlesh()*Nutrition.FLESH_PROTEIN+consumedPrey()*Nutrition.PREY_PROTEIN;}
    private long loadedTicks, stageDuration = stageTicks();
    /** Development steps owed below one whole step (BroodCapacity speed), saved so a faster nursery stays exact across
     * reloads; stageDuration itself never changes. */
    private int developmentCredit;
    /** This nursery now: the queen's hall and the nursery chamber's wall tier as the pile's last evaluation confirmed them
     * (a slow cadence, ColonyDevelopment.INTERVAL); the founding nursery before the first evaluation. */
    public BroodCapacity.Nursery nursery() {
        var e = stageEvaluation;
        if (e == null) return BroodCapacity.nursery(false, 1);
        return BroodCapacity.nursery(e.inputs().tier(dev.primeants.colony.ChamberFunction.QUEENS_HALL).known() >= 1, e.inputs().tier(dev.primeants.colony.ChamberFunction.NURSERY).known());
    }
    private String condition = "unowned";
    public BroodPile(BlockPos pos, BlockState state) { super(NurseryBlocks.BROOD_TYPE, pos, state); }
    public static double multiplier() { return Double.parseDouble(System.getProperty("prime_ants.broodMultiplier", "1")); }
    public static long stageTicks() {
        long duration = new SimulationTimeScale(multiplier()).ticksForGameDays(0.5);
        if (duration > Long.MAX_VALUE / LARVA_COST) throw new IllegalArgumentException("Brood duration exceeds accounting range");
        return duration;
    }
    public static long callowTicks() { return new SimulationTimeScale(multiplier()).ticksForGameDays(0.2); }
    public List<BroodRecord> records() { return List.copyOf(records); }
    public Set<UUID> consumed() { return Set.copyOf(consumed); }
    public UUID queenId() { return queenId; }
    public UUID componentOwner(){return queenId!=null&&plan!=null&&level instanceof ServerLevel l&&l.getBlockEntity(getBlockPos())==this&&ownedBy(queenId,plan)?queenId:null;}
    public long loadedTicks() { return loadedTicks; }
    public long stageDuration() { return stageDuration; }
    public String condition() { return condition; }
    public boolean ownedBy(UUID id, NestPlan p) { return id.equals(queenId) && plan != null && p.entrance().equals(plan.entrance())
            && p.direction() == plan.direction() && getBlockPos().equals(p.nursery()) && getBlockState().is(NurseryBlocks.BROOD_PILE); }
    /** Called exclusively from the settled queen's single founding/nursery controller. */
    public boolean establish(LasiusNigerEntity queen, NestPlan p) {
        if (queenId != null || !getBlockPos().equals(p.nursery())) return false;
        queenId = queen.getUUID(); plan = NestPlan.geometry(p.entrance(), p.direction());
        neglectGrace=queen.broodNeglectGrace();waitingBound=queen.cocoonWaitingBound();
        adultLifespan=queen.adultLife().lifespan();fastingGrace=queen.adultLife().grace();adultCapacity=queen.colonyAdultCapacity();
        if (!queen.founding().sealed() || queen.position().distanceToSqr(Vec3.atBottomCenterOf(getBlockPos())) > CARE_REACH_SQUARED
                || !queen.spendReserve(CAPACITY * EGG_COST)) { queenId = null; plan = null; return false; }
        for (int slot = 0; slot < CAPACITY; slot++) {var r=new BroodRecord(UUID.randomUUID(), queenId, slot,neglectGrace,waitingBound);records.add(r);original.add(r.id());}
        initializeClock();
        condition = "eggs_laid"; changed();
        PrimeAnts.LOGGER.info("Nursery established queen={} pile={} brood={} reserve={} multiplier={} stageTicks={}", queenId, getBlockPos(), records.stream().map(BroodRecord::id).toList(), queen.bodyReserve(), multiplier(), stageTicks());
        return true;
    }
    public boolean accepts(BroodRecord r,net.minecraft.world.item.ItemStack s){
        return !r.founding&&r.stage()==BroodStage.LARVA&&r.nutrition().accepts(s,Nutrition.LARVA_SUGAR,Nutrition.LARVA_PROTEIN)
                &&r.nutrition().gainedSugar()+Nutrition.sugarYield(s)<=Nutrition.LARVA_SUGAR&&r.nutrition().gainedProtein()+Nutrition.proteinYield(s)<=Nutrition.LARVA_PROTEIN;
    }
    public boolean feedBy(LasiusNigerEntity nurse,UUID brood){
        if(!(level instanceof ServerLevel l)||nurse.workerTasks().phase()!=WorkerTasks.Phase.NURSE_FEED||!brood.equals(nurse.workerTasks().recipientId())||nurse.workerTasks().feedingTicks()<WorkerTasks.FEEDING_TICKS
                ||!nurse.workerTasks().nursingAuthorized(l)||!queenId.equals(nurse.queenId())||!WorkerTasks.reaches(l,nurse,Vec3.atBottomCenterOf(getBlockPos()).add(0,0.15,0))||plan.nurseryProblem(l,queenId,operational)!=null)return false;
        var r=records.stream().filter(b->b.id().equals(brood)).findFirst().orElse(null);
        if(r==null||!accepts(r,nurse.getMainHandItem())||!r.nutrition().ingest(nurse.getMainHandItem(),Nutrition.LARVA_SUGAR,Nutrition.LARVA_PROTEIN))return false;
        PrimeAnts.LOGGER.info("Physical larva feeding queen={} brood={} nurse={} consumed={} sugar={} protein={}",queenId,brood,nurse.getUUID(),nurse.getMainHandItem(),r.nutrition().sugar(),r.nutrition().protein());
        nurse.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,net.minecraft.world.item.ItemStack.EMPTY);setChanged();return true;
    }
    private void retire(BroodRecord r){
        consumed.add(r.id());records.remove(r);archivedApples+=r.nutrition().apples();archivedBerries+=r.nutrition().berries();archivedChickens+=r.nutrition().chickens();archivedNectar+=r.nutrition().nectar();archivedNectarV2+=r.nutrition().nectarV2();archivedPrey+=r.nutrition().prey();archivedFlesh+=r.nutrition().flesh();
    }
    public dev.primeants.worker.FoodLimitedGrowth.Supply supply(ServerLevel l){
        boolean complete=true,hungry=false;long incomeS=gainedSugar(),incomeP=gainedProtein(),stockS=0,stockP=0,commitS=0,commitP=0;int adults=0;
        var members=ColonyMembers.get(l);
        var ids=new java.util.ArrayList<UUID>();ids.add(queenId);for(var m:members.members(queenId))if(!m.dead())ids.add(m.worker());
        for(UUID id:ids){
            if(!(l.getEntity(id) instanceof LasiusNigerEntity a)||!a.isAlive()||a.isRemoved()||a.isNoAi()
                ||!NestPlan.loaded(l,a.blockPosition())||!l.isPositionEntityTicking(a.blockPosition())){complete=false;continue;}
            adults++;hungry|=a.adultLife().fasting()>=Math.max(1,a.adultLife().grace()/4);
            incomeS+=a.nutrition().gainedSugar();incomeP+=a.nutrition().gainedProtein();stockS+=a.nutrition().sugar();stockP+=a.nutrition().protein();
            stockS+=Nutrition.sugarYield(a.getMainHandItem());stockP+=Nutrition.proteinYield(a.getMainHandItem());
        }
        for(var entry:dev.primeants.worker.AdultHistory.get(l).records().entrySet()){
            var row=com.google.gson.JsonParser.parseString(entry.getValue()).getAsJsonObject();
            if(row.get("queen").getAsString().equals(queenId.toString())&&!ids.contains(UUID.fromString(entry.getKey()))){var n=row.getAsJsonObject("nutrition");incomeS+=n.get("gainedSugar").getAsLong();incomeP+=n.get("gainedProtein").getAsLong();}
        }
        if(!NestPlan.loaded(l,plan.cache()))complete=false;
        else if(l.getBlockEntity(plan.cache()) instanceof dev.primeants.worker.NestCache cache){
            if(!cache.ownedBy(queenId,plan))complete=false;else for(var stack:cache.contents()){stockS+=Nutrition.sugarYield(stack);stockP+=Nutrition.proteinYield(stack);}
        }else if(!l.getBlockState(plan.cache()).isAir())complete=false;
        for(var r:records){stockS+=r.nutrition().sugar();stockP+=r.nutrition().protein();if(!r.founding()&&r.stage()!=BroodStage.COCOON){commitS+=Math.max(0,Nutrition.LARVA_SUGAR-r.nutrition().spentSugar());commitP+=Math.max(0,Nutrition.LARVA_PROTEIN-r.nutrition().spentProtein());}}
        return new dev.primeants.worker.FoodLimitedGrowth.Supply(complete,hungry,incomeS,incomeP,stockS,stockP,commitS,commitP,adults,records.size());
    }
    /** Admission belongs to the next virtual tick. An already ingested maintenance unit (or its
     * remaining coverage) resets fasting on that tick; do not postpone admission until a distant brood boundary. */
    private dev.primeants.worker.FoodLimitedGrowth.Supply aggregateSupply(ServerLevel server){
        var s=supply(server);boolean hungry=false;
        var ids=new ArrayList<UUID>();ids.add(queenId);for(var m:ColonyMembers.get(server).members(queenId))if(!m.dead())ids.add(m.worker());
        for(var id:ids)if(server.getEntity(id) instanceof LasiusNigerEntity a)
            hungry|=a.adultLife().fasting()>=Math.max(1,a.adultLife().grace()/4)&&a.nutrition().sugar()==0&&a.adultLife().coverageRemaining()==0;
        return new dev.primeants.worker.FoodLimitedGrowth.Supply(s.complete(),hungry,s.foodSugar(),s.foodProtein(),s.storedSugar(),s.storedProtein(),s.committedSugar(),s.committedProtein(),s.adults(),s.brood());
    }
    private boolean lay(ServerLevel l,LasiusNigerEntity q,boolean care){
        return lay(l,q,care,false);
    }
    private boolean lay(ServerLevel l,LasiusNigerEntity q,boolean care,boolean aggregate){
        if(!care||!operational||!originalTerminal())return false;
        long cadence=aggregate?aggregateCadence:layingInterval(l); if(loadedTicks-lastLayingTick<cadence)return false;
        if(records.size()>=nursery().slots()){if(!condition.equals("larva_sugar_or_protein_exhausted"))condition="nursery_slots_full";return false;}
        if(ColonyMembers.get(l).occupied(queenId)+records.size()>=adultCap(l)-1){condition="colony_capacity_full_or_unloaded";return false;}
        if(!(aggregate?growth.allowsAggregate(aggregateSupply(l)):growth.allows(supply(l)))){condition=aggregate?"finite_stores_below_commitments":growth.reason();return false;}
        if(q==null||!q.isAlive()||q.isRemoved())return false;
        int slot=0;while(true){final int index=slot;if(records.stream().noneMatch(r->r.slot()==index))break;slot++;}
        // One canonical event identity survives reconstruction and also owns its deterministic worker identity.
        var id=layingIdentity(loadedTicks,slot);
        if(!layingIdentityAvailable(l,id)){condition="laying_event_already_represented";return false;}
        if(aggregate&&!catchup.eventAvailable())return false;
        if(!q.nutrition().spend(Nutrition.EGG_SUGAR,Nutrition.EGG_PROTEIN)){condition="queen_ingested_nutrition_exhausted";return false;}
        var r=new BroodRecord(id,queenId,slot,neglectGrace,waitingBound);r.founding=false;records.add(r);lastLayingTick=loadedTicks;condition="food_fed_egg_laid";
        PrimeAnts.LOGGER.info("Food-fed egg queen={} brood={} slot={} pileTicks={} cadence={} sugarCost={} proteinCost={}",queenId,r.id(),slot,loadedTicks,cadence,Nutrition.EGG_SUGAR,Nutrition.EGG_PROTEIN);return true;
    }
    private void changed() {
        long start=System.nanoTime();
        setChanged();
        if (level == null || level.isClientSide()) return;
        BlockState before = getBlockState(), next = before;
        var properties = List.of(BroodPileBlock.A, BroodPileBlock.B, BroodPileBlock.C);
        for (int slot = 0; slot < properties.size(); slot++) {
            final int index = slot;
            BroodStage stage = records.stream().filter(r -> r.slot() == index).map(BroodRecord::stage).findFirst().orElse(BroodStage.EMPTY);
            next = next.setValue(properties.get(slot), stage);
        }
        // Brood beyond the first three slots shows in aggregate: one more heap level per three brood.
        next = next.setValue(BroodPileBlock.MORE, BroodPileBlock.more((int)records.stream().filter(r -> r.slot() >= properties.size()).count()));
        if (!next.equals(before)) level.setBlock(getBlockPos(), next, 3);
        level.sendBlockUpdated(getBlockPos(), before, next, 3);
        catchup.commitCost(System.nanoTime()-start);
    }
    private boolean reconcileMember(ServerLevel server,BroodRecord r){
        var member=ColonyMembers.get(server).member(r.workerId());var existing=server.getEntity(r.workerId());
        if(member!=null){
            if(!member.brood().equals(r.id())||!member.queen().equals(queenId)||!member.home().equals(plan.chamber())){condition="emergence_identity_conflict";return false;}
            expired.remove(r.id());retire(r);condition=member.dead()?"emerged_worker_dead_no_respawn":"emergence_recorded_or_unloaded";return true;
        }
        if(existing instanceof LasiusNigerEntity ant&&r.id().equals(ant.broodId())&&queenId.equals(ant.queenId())&&plan.chamber().equals(ant.nurseryHome())){
            ColonyMembers.get(server).record(r.id(),queenId,plan.chamber());expired.remove(r.id());retire(r);condition="emergence_reconciled";return true;
        }
        return false;
    }
    private void expire(ServerLevel server,BroodRecord r,String reason){
        expired.put(r.id(),BroodHistory.get(server).expire(r,plan.chamber(),loadedTicks,reason));records.remove(r);condition="brood_expired_"+reason;
    }
    void serverTick(ServerLevel server) {
        if (queenId == null || plan == null || !ownedBy(queenId, plan)) return;
        // A normal clock rebase without aggregate receipts still belongs to ColonyClock.
        // An already applied historical tick cannot earn a second ordinary step after restoration.
        if(lastOrdinaryGameTime==server.getGameTime()||server.getGameTime()<=restoredOrdinaryFloor||catchup.appliedThrough()>=server.getGameTime())return;
        if(!resumeCatchup(server)){setChanged();return;}
        loadedTicks=dev.primeants.entity.AdultLife.saturatingAdd(loadedTicks,1);
        growth.observe(loadedTicks,supply(server));
        var owner = server.getEntity(queenId);
        if(stageEvaluation==null||loadedTicks-stageEvaluatedAt>=ColonyDevelopment.INTERVAL)development(server); // slow cadence, never per tick
        for(UUID id:consumed)ColonyMembers.get(server).record(id,queenId,plan.chamber());
        expired.replaceAll((id,row)->BroodHistory.get(server).retain(id,row));
        String habitat = plan.nurseryProblem(server, queenId, operational);
        LasiusNigerEntity queen = owner instanceof LasiusNigerEntity q && q.isAlive() && !q.isRemoved() ? q : null;
        boolean care = habitat==null && queen != null && !queen.isNoAi() && queen.founding().ready()
                && queen.position().distanceToSqr(Vec3.atBottomCenterOf(getBlockPos())) <= CARE_REACH_SQUARED;
        condition = habitat!=null?habitat:care?"cared":"caregiver_absent_or_out_of_reach";
        boolean visual = false;
        visual |= advanceBrood(server,1,queen,care,habitat);
        if(lay(server,queen,care))visual=true;
        if (visual) changed(); else setChanged();
        clock.completed(server.getGameTime());
        lastOrdinaryGameTime=server.getGameTime();
    }
    /** A span ends at the earliest lifecycle event selected by the coordinator. No missed-tick loop. */
    private boolean advanceBrood(ServerLevel server,long ticks,LasiusNigerEntity queen,boolean care,String habitat) {
        var n=nursery();int initialCredit=developmentCredit;
        long steps=Math.addExact(Math.multiplyExact(ticks,n.speed()),developmentCredit)/BroodCapacity.SPEED_DIVISOR;
        developmentCredit=(int)((ticks%BroodCapacity.SPEED_DIVISOR*n.speed()+developmentCredit)%BroodCapacity.SPEED_DIVISOR);
        boolean visual=false;
        for(var r:List.copyOf(records)){
            if(consumed.contains(r.id())){records.remove(r);visual=true;continue;}
            if(reconcileMember(server,r)){visual=true;continue;}
            if(ColonyMembers.get(server).member(r.workerId())!=null||server.getEntity(r.workerId())!=null){condition="emergence_identity_conflict";visual|=unsupported(server,r,condition,ticks);continue;}
            String terminal=BroodHistory.get(server).terminal(r.id());
            if(terminal!=null){var row=com.google.gson.JsonParser.parseString(terminal).getAsJsonObject();if(!row.get("queen").getAsString().equals(queenId.toString())||row.get("home").getAsLong()!=plan.chamber().asLong())throw new IllegalStateException("Terminal brood lineage conflict");expired.put(r.id(),terminal);records.remove(r);visual=true;continue;}
            if(habitat!=null){visual|=unsupported(server,r,habitat,ticks);continue;}
            long left=steps;
            while(left>0&&records.contains(r)){
                if(r.stage==BroodStage.EGG){
                    if(!care){visual|=unsupported(server,r,"egg_care_missing",ticks);break;}
                    r.neglectTicks=0;r.neglectReason="";
                }
                long count=Math.min(left,stageDuration-r.progress),next=r.progress+count;
                if(r.stage==BroodStage.LARVA){
                    long low=r.progress,high=next;
                    while(low<high){long mid=low+(high-low+1)/2;if(nourishable(r,mid,queen,care))low=mid;else high=mid-1;}
                    next=low;count=next-r.progress;
                    if(count>0&&!nourish(r,next,queen,care))throw new IllegalStateException("Resolved brood cost refused");
                    if(count==0&&r.progress<stageDuration){
                        condition=r.founding?(care?"queen_reserve_exhausted":"larva_care_missing"):"larva_sugar_or_protein_exhausted";
                        // A successful substep protects its entire ordinary tick. The unsupported tail starts
                        // on the next tick, including at x1.5/x2 and across a saved fractional credit.
                        long supportedTicks=Math.max(0,((steps-left)*BroodCapacity.SPEED_DIVISOR-initialCredit+n.speed()-1)/n.speed());
                        long unsupportedTicks=Math.max(0,ticks-supportedTicks);
                        if(unsupportedTicks>0)visual|=unsupported(server,r,condition,unsupportedTicks);break;
                    }
                }
                r.progress=next;left-=count;
                if(r.stage==BroodStage.COCOON&&count>0)r.waitingTicks=0;
                if(r.progress<stageDuration)continue;
                if(r.stage==BroodStage.COCOON){
                    if(emerge(server,r))visual=true;
                    else {
                        // Inherited cocoon waiting is charged when emergence is attempted on a tick's
                        // first substep, even if that substep just completed its progress.
                        long priorSteps=Math.max(0,steps-left-(count>0?1:0));
                        long supportedTicks=Math.max(0,(priorSteps*BroodCapacity.SPEED_DIVISOR-initialCredit+n.speed()-1)/n.speed());
                        long waiting=Math.max(0,ticks-supportedTicks);
                        if(waiting>0)visual|=unsupported(server,r,condition,waiting);
                    }
                    break;
                }
                if(r.stage==BroodStage.LARVA&&r.nourishment!=LARVA_COST)break;
                r.stage=r.stage==BroodStage.EGG?BroodStage.LARVA:BroodStage.COCOON;r.progress=0;visual=true;
                PrimeAnts.LOGGER.info("Brood stage queen={} brood={} stage={} pileTicks={} nourishment={} slots={} speed={}",queenId,r.id(),r.stage,loadedTicks,r.nourishment,n.slots(),n.rate());
            }
        }
        return visual;
    }
    private boolean nourishable(BroodRecord r,long next,LasiusNigerEntity queen,boolean care){
        long required=next*LARVA_COST/stageDuration-r.nourishment;
        if(r.founding)return !(required>0||r.nourishment<LARVA_COST)||care&&queen!=null&&queen.bodyReserve()>0&&required>=0&&required<=queen.bodyReserve();
        long sugar=next*Nutrition.LARVA_SUGAR/stageDuration-r.nutrition().spentSugar();
        long protein=next*Nutrition.LARVA_PROTEIN/stageDuration-r.nutrition().spentProtein();
        return r.nourishment>=LARVA_COST||r.nutrition().sugar()>0&&r.nutrition().protein()>0&&sugar>=0&&protein>=0&&sugar<=r.nutrition().sugar()&&protein<=r.nutrition().protein();
    }
    private boolean nourish(BroodRecord r,long next,LasiusNigerEntity queen,boolean care){
        if(!nourishable(r,next,queen,care))return false;
        long required=next*LARVA_COST/stageDuration-r.nourishment;
        if(r.founding){if((required>0||r.nourishment<LARVA_COST)&&!queen.spendReserve(required))return false;r.nourishment+=required;}
        else{
            long sugar=next*Nutrition.LARVA_SUGAR/stageDuration-r.nutrition().spentSugar(),protein=next*Nutrition.LARVA_PROTEIN/stageDuration-r.nutrition().spentProtein();
            if(r.nourishment<LARVA_COST&&!r.nutrition().spend(sugar,protein))return false;
            r.nourishment=r.nutrition().spentSugar()+r.nutrition().spentProtein();
        }
        if(required>0){r.neglectTicks=0;r.neglectReason="";}return true;
    }
    private boolean unsupported(ServerLevel server,BroodRecord r,String reason,long ticks){
        r.neglectReason=reason;
        if(r.stage()==BroodStage.COCOON){r.waitingTicks=dev.primeants.entity.AdultLife.saturatingAdd(r.waitingTicks,ticks);if(r.waitingTicks>=r.waitingBound()){expire(server,r,"cocoon_wait_"+reason);return true;}}
        else{r.neglectTicks=dev.primeants.entity.AdultLife.saturatingAdd(r.neglectTicks,ticks);if(r.neglectTicks>=r.neglectGrace()){expire(server,r,"neglect_"+reason);return true;}}
        return false;
    }
    public NestPlan catchupPlan(){return plan;}
    /** Validate all represented worker identities before any aggregate resource/lifecycle mutation. */
    public String reconcileCatchupMembers(ServerLevel server){
        for(var r:records){
            var member=ColonyMembers.get(server).member(r.workerId());var actor=server.getEntity(r.workerId());
            if(member!=null&&(!member.brood().equals(r.id())||!member.queen().equals(queenId)||!member.home().equals(plan.chamber())))return "pending_brood_member_conflict";
            if(actor!=null&&(!(actor instanceof LasiusNigerEntity a)||!r.id().equals(a.broodId())||!queenId.equals(a.queenId())||!plan.chamber().equals(a.nurseryHome())))return "pending_brood_body_conflict";
            if(actor instanceof LasiusNigerEntity a&&(!a.isAlive()||a.isRemoved())&&member==null)return "pending_unregistered_terminal_body";
            if(member!=null&&member.dead()&&actor instanceof LasiusNigerEntity a&&a.isAlive()&&!a.isRemoved())return "pending_recorded_dead_body_conflict";
            String terminal=BroodHistory.get(server).terminal(r.id());
            if(terminal!=null){var row=com.google.gson.JsonParser.parseString(terminal).getAsJsonObject();if(!row.get("queen").getAsString().equals(queenId.toString())||row.get("home").getAsLong()!=plan.chamber().asLong())return "pending_terminal_brood_lineage_conflict";}
        }
        boolean visual=false;for(var r:List.copyOf(records))visual|=reconcileMember(server,r);
        if(visual)changed();return null;
    }
    public boolean aggregateCare(ServerLevel server){
        return server.getEntity(queenId) instanceof LasiusNigerEntity q&&q.isAlive()&&!q.isRemoved()&&!q.isNoAi()&&q.founding().ready()
            &&q.position().distanceToSqr(Vec3.atBottomCenterOf(getBlockPos()))<=CARE_REACH_SQUARED;
    }
    /** Stage and viability boundaries; at most three stages per existing record between resource decisions. */
    public long nextBroodEvent(ServerLevel server,String habitat){
        long result=Long.MAX_VALUE;var n=nursery();boolean care=habitat==null&&aggregateCare(server);
        var q=server.getEntity(queenId) instanceof LasiusNigerEntity a&&a.isAlive()?a:null;
        for(var r:records){
            long viability=r.stage==BroodStage.COCOON?Math.max(1,r.waitingBound()-r.waitingTicks):Math.max(1,r.neglectGrace()-r.neglectTicks);
            long steps=stageDuration-r.progress;
            if(habitat!=null||r.stage==BroodStage.EGG&&!care||r.stage==BroodStage.LARVA&&!nourishable(r,Math.min(stageDuration,r.progress+1),q,care)||steps==0){result=Math.min(result,viability);continue;}
            if(r.stage==BroodStage.LARVA){long lo=r.progress,hi=stageDuration;while(lo<hi){long mid=lo+(hi-lo+1)/2;if(nourishable(r,mid,q,care))lo=mid;else hi=mid-1;}steps=Math.max(1,lo-r.progress);}
            result=Math.min(result,Math.max(1,(steps*BroodCapacity.SPEED_DIVISOR-developmentCredit+n.speed()-1)/n.speed()));
        }
        return result;
    }
    private long aggregateBirthTime=-1;
    private long aggregateCadence;
    private long aggregateObservationTick;
    private final Set<String> deferredInitialShortfalls=new HashSet<>();
    private UUID layingIdentity(long tick,int slot){return UUID.nameUUIDFromBytes(("prime_ants:egg:"+queenId+":"+tick+":"+slot).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private boolean layingIdentityAvailable(ServerLevel server,UUID id){
        return !consumed.contains(id)&&!expired.containsKey(id)&&records.stream().noneMatch(r->r.id().equals(id))
            &&BroodHistory.get(server).terminal(id)==null&&ColonyMembers.get(server).member(ColonyMembers.workerId(id))==null
            &&server.getEntity(ColonyMembers.workerId(id))==null;
    }
    /** A funded event can occur when its confirmed hall cadence is due. Unsupported admission creates no tick loop. */
    public long nextLayingEvent(ServerLevel server,String habitat){
        if(habitat!=null||!aggregateCare(server)||!operational||!originalTerminal()||records.size()>=nursery().slots()
            ||ColonyMembers.get(server).occupied(queenId)+records.size()>=adultCap(server)-1||!aggregateEggFunding(server)||!growth.allowsAggregate(aggregateSupply(server)))return Long.MAX_VALUE;
        long delay=Math.max(1,aggregateCadence-(loadedTicks-lastLayingTick));
        int slot=0;while(true){final int index=slot;if(records.stream().noneMatch(r->r.slot()==index))break;slot++;}
        return layingIdentityAvailable(server,layingIdentity(dev.primeants.entity.AdultLife.saturatingAdd(loadedTicks,delay),slot))?delay:Long.MAX_VALUE;
    }
    /** A failed queen-funding decision is not a new event every virtual tick. Food held by other
     * actors cannot pay her egg cost. Only her nutrition or actually serviceable confirmed cache units can. */
    private boolean aggregateEggFunding(ServerLevel server){
        if(!(server.getEntity(queenId) instanceof LasiusNigerEntity q))return false;
        return q.nutrition().sugar()>=Nutrition.EGG_SUGAR&&q.nutrition().protein()>=Nutrition.EGG_PROTEIN;
    }
    /** Existing StageRules clocks in this nursery's clock domain; no requirement first seen at C is backdated. */
    public long nextStageEvent(ServerLevel server){
        var colony=dev.primeants.colony.ChamberRegistry.get(server).colony(queenId);
        long next=Long.MAX_VALUE;
        if(colony!=null)for(long since:colony.unmetSince().values())
            next=Math.min(next,Math.max(1,dev.primeants.entity.AdultLife.saturatingAdd(since,dev.primeants.colony.StageRules.GRACE)-loadedTicks));
        return next;
    }
    public void advanceAggregate(ServerLevel server,long ticks,long gameTime,String habitat){
        loadedTicks=dev.primeants.entity.AdultLife.saturatingAdd(loadedTicks,ticks);
        var q=server.getEntity(queenId) instanceof LasiusNigerEntity a&&a.isAlive()&&!a.isRemoved()?a:null;
        aggregateBirthTime=gameTime;
        try{advanceBrood(server,ticks,q,habitat==null&&aggregateCare(server),habitat);}finally{aggregateBirthTime=-1;}
        refreshAggregateStage(server,false);
        int slot=0;while(true){final int index=slot;if(records.stream().noneMatch(r->r.slot()==index))break;slot++;}
        // Validate replay, current eligibility and the coordinator's reserved event budget before any egg-funding transfer.
        if(habitat==null&&loadedTicks-lastLayingTick>=aggregateCadence&&nextLayingEvent(server,habitat)==1
            &&layingIdentityAvailable(server,layingIdentity(loadedTicks,slot))&&catchup.eventAvailable()){
            lay(server,q,aggregateCare(server),true);
        }
        refreshAggregateStage(server,false);changed();
    }
    public void evaluateAggregateStart(ServerLevel server,long remaining){
        long start=System.nanoTime();
        aggregateObservationTick=dev.primeants.entity.AdultLife.saturatingAdd(loadedTicks,remaining);
        var previous=dev.primeants.colony.ChamberRegistry.get(server).colony(queenId);
        var clocks=previous==null?java.util.Map.<String,Long>of():previous.unmetSince();
        stageEvaluation=ColonyDevelopment.evaluate(server,queenId,plan,operational,adultCapacity,dev.primeants.entity.AdultLife.saturatingAdd(loadedTicks,1));
        deferredInitialShortfalls.clear();deferredInitialShortfalls.addAll(stageEvaluation.result().unmetSince().keySet());deferredInitialShortfalls.removeAll(clocks.keySet());
        deferUnobservedClocks(server);
        stageEvaluatedAt=loadedTicks;stageMembers=ColonyMembers.get(server);stageMembersRevision=ColonyMembers.get(server).revision();stageRegistry=dev.primeants.colony.ChamberRegistry.get(server);
        aggregateCadence=layingInterval(server);
        catchup.stageCost(System.nanoTime()-start);
    }
    public void refreshAggregateStage(ServerLevel server,boolean firstTick){
        long start=System.nanoTime();var members=ColonyMembers.get(server);
        long now=firstTick?dev.primeants.entity.AdultLife.saturatingAdd(loadedTicks,1):loadedTicks;
        boolean dead=dev.primeants.worker.AdultHistory.get(server).recorded(queenId);
        if(stageEvaluation==null||members!=stageMembers||members.revision()!=stageMembersRevision||dead!=stageEvaluation.inputs().queenDead()){
            deferredInitialShortfalls.removeIf(key->key.endsWith(":adults"));
            stageEvaluation=ColonyDevelopment.evaluate(server,queenId,plan,operational,adultCapacity,now);
        }else stageEvaluation=ColonyDevelopment.refreshFoodAndClocks(server,queenId,adultCapacity,now,stageEvaluation);
        deferUnobservedClocks(server);
        stageEvaluatedAt=loadedTicks;stageMembers=members;stageMembersRevision=members.revision();stageRegistry=dev.primeants.colony.ChamberRegistry.get(server);
        catchup.stageCost(System.nanoTime()-start);
    }
    /** A newly observed reload shortfall has no evidence that it existed at B. Hold its clock until C;
     * exact resource/population changes during the interval instead establish their own event time. */
    private void deferUnobservedClocks(ServerLevel server){
        if(loadedTicks>=aggregateObservationTick||deferredInitialShortfalls.isEmpty())return;
        var result=stageEvaluation.result();var clocks=new java.util.TreeMap<>(result.unmetSince());
        deferredInitialShortfalls.forEach(clocks::remove);
        if(clocks.equals(result.unmetSince()))return;
        ColonyDevelopment.retainObservedClocks(server,queenId,clocks);
        var retained=new dev.primeants.colony.StageRules.Result(result.stage(),result.certain(),result.possible(),result.missing(),
            result.pending().stream().filter(p->!deferredInitialShortfalls.contains(p.requirement().key())).toList(),java.util.Map.copyOf(clocks));
        stageEvaluation=new ColonyDevelopment.Evaluation(stageEvaluation.previous(),retained,stageEvaluation.inputs(),stageEvaluation.chambers(),stageEvaluation.cap());
    }
    /** Only a real cache-stock debit establishes a food-shortfall event inside the interval.
     * Ingestion from previously carried cargo does not observe or change the store's clock. */
    public void aggregateFoodChanged(){deferredInitialShortfalls.remove("mature:food");}
    /** UNKNOWN clears only requirements whose certainty is broken, through the ordinary StageRules evaluator. */
    public void observeUnknownTerrain(ServerLevel server){
        long start=System.nanoTime();
        stageEvaluation=ColonyDevelopment.evaluate(server,queenId,plan,operational,adultCapacity,loadedTicks);
        stageMembers=ColonyMembers.get(server);stageMembersRevision=ColonyMembers.get(server).revision();stageRegistry=dev.primeants.colony.ChamberRegistry.get(server);stageEvaluatedAt=loadedTicks;
        catchup.stageCost(System.nanoTime()-start);
    }
    private boolean emerge(ServerLevel server, BroodRecord r) {
        var member=ColonyMembers.get(server).member(r.workerId());
        if(member!=null){
            if(!member.brood().equals(r.id())||!member.queen().equals(queenId)||!member.home().equals(plan.chamber())){condition="emergence_identity_conflict";return false;}
            retire(r);condition=member.dead()?"emerged_worker_dead_no_respawn":"emergence_recorded_or_unloaded";return true;
        }
        var existing = server.getEntity(r.workerId());
        if (existing != null) {
            if (existing instanceof LasiusNigerEntity ant && r.id().equals(ant.broodId()) && queenId.equals(ant.queenId())) {
                ColonyMembers.get(server).record(r.id(),queenId,plan.chamber());retire(r); condition = "emergence_reconciled"; return true;
            }
            condition = "emergence_identity_conflict"; return false;
        }
        int queenSlot=dev.primeants.worker.AdultHistory.get(server).records().containsKey(queenId.toString())?0:1;
        if(ColonyMembers.get(server).occupied(queenId)+queenSlot>=adultCap(server)){condition="emergence_capacity_full_or_unloaded";return false;}
        var worker = AntEntities.WORKER.create(server, EntitySpawnReason.BREEDING);
        if (worker == null) { condition = "worker_creation_failed"; return false; }
        worker.setUUID(r.workerId()); worker.initializeCallow(r.id(), queenId, plan.chamber(),adultLifespan,fastingGrace);
        if(aggregateBirthTime>=0)worker.simulationBirthAt(aggregateBirthTime);
        var emergence=new ArrayList<BlockPos>(dev.primeants.founding.NestExpansion.get(server).operationalSpace(server,queenId));
        for(int f:new int[]{5,3,4})for(int s:new int[]{-1,0,1})emergence.add(plan.at(f,s,-2));
        for (BlockPos p:emergence) {
            Vec3 pos = Vec3.atBottomCenterOf(p);
            worker.setPos(pos);
            AABB body = worker.getBoundingBox();
            if (!NestPlan.walkable(server, p) || !server.noCollision(worker, body) || !server.getEntities(worker, body).isEmpty()) continue;
            if (!server.addFreshEntity(worker) || server.getEntity(r.workerId()) != worker) { condition = "worker_insertion_failed"; return false; }
            ColonyMembers.get(server).record(r.id(),queenId,plan.chamber());retire(r); condition = "callow_emerged";
            if(dev.primeants.founding.NestExpansion.get(server).operationalSpace(server,queenId).contains(p))dev.primeants.founding.NestExpansion.get(server).used(queenId,worker,"brood_emergence");
            PrimeAnts.LOGGER.info("Callow emerged queen={} brood={} worker={} pileTicks={} remaining={}", queenId, r.id(), worker.getUUID(), loadedTicks, records.size());
            return true;
        }
        condition = "emergence_space_blocked"; return false;
    }
    @Override protected void saveAdditional(ValueOutput out) {
        out.putBoolean("GeneratedInitial",generatedInitial);
        growth.save(out.child("GrowthFlow"));
        super.saveAdditional(out);
        if (queenId == null || plan == null) return;
        clock.save(out);catchup.save(out.child("Catchup"));out.putLong("LastOrdinaryGameTime",lastOrdinaryGameTime);
        out.putString("Queen", queenId.toString()); out.store("Entrance", BlockPos.CODEC, plan.entrance()); out.putString("Direction", plan.direction().getName());
        out.putLong("LoadedTicks", loadedTicks); out.putString("Condition", condition);
        out.putLong("StageDuration", stageDuration); out.putInt("DevelopmentCredit", developmentCredit);
        out.putLong("NeglectGrace",neglectGrace);out.putLong("WaitingBound",waitingBound);
        out.store("Expired",com.mojang.serialization.Codec.unboundedMap(com.mojang.serialization.Codec.STRING,com.mojang.serialization.Codec.STRING),expired.entrySet().stream().collect(java.util.stream.Collectors.toMap(e->e.getKey().toString(),java.util.Map.Entry::getValue)));
        out.putLong("AdultLifespan",adultLifespan);out.putLong("AdultFastingGrace",fastingGrace);out.putInt("AdultCapacityBound",adultCapacity);
        out.putBoolean("Operational",operational);out.putLong("LastLayingTick",lastLayingTick);out.putLong("ConsumedApples",archivedApples);out.putLong("ConsumedBerries",archivedBerries);out.putLong("ConsumedChickens",archivedChickens);out.putLong("ConsumedNectar",archivedNectar);out.putLong("ConsumedNectarV2",archivedNectarV2);out.putLong("ConsumedPrey",archivedPrey);out.putLong("ConsumedFlesh",archivedFlesh);
        out.store("Original",com.mojang.serialization.Codec.STRING.listOf(),original.stream().map(UUID::toString).sorted().toList());
        var list = out.childrenList("Brood"); for (BroodRecord r : records) r.save(list.addChild());
        out.store("Consumed", com.mojang.serialization.Codec.STRING.listOf(), consumed.stream().map(UUID::toString).sorted().toList());
    }
    @Override protected void loadAdditional(ValueInput in) {
        generatedInitial=in.getBooleanOr("GeneratedInitial",false);
        growth.load(in.childOrEmpty("GrowthFlow"));
        super.loadAdditional(in); records.clear(); consumed.clear();original.clear();expired.clear(); queenId = null; plan = null; stageEvaluation = null;
        clock.load(in);catchup.load(in.childOrEmpty("Catchup"));lastOrdinaryGameTime=in.getLongOr("LastOrdinaryGameTime",clock.lastGameTime().orElse(-1));
        // Legacy level-access migration can establish a different baseline while retaining the old
        // ordinary field. Preserve both fields; only a coherent completed pair proves a restore floor.
        restoredOrdinaryFloor=lastOrdinaryGameTime>=0&&clock.lastGameTime().isPresent()&&clock.lastGameTime().getAsLong()==lastOrdinaryGameTime?lastOrdinaryGameTime:-1;
        reloadObservation = null; reloadBoundaryPending = true;
        adultLifespan=in.getLongOr("AdultLifespan",dev.primeants.entity.AdultLife.DEFAULT_LIFESPAN);fastingGrace=in.getLongOr("AdultFastingGrace",dev.primeants.entity.AdultLife.DEFAULT_FASTING);
        // A 0.1.0 pile has only the fixed AdultCapacity (4..30); it migrates once to an upper bound (AdultBound.fromLegacy).
        var bound=in.getInt("AdultCapacityBound");int legacy=in.getIntOr("AdultCapacity",AdultBound.LEGACY_MAX);
        if(adultLifespan<1||fastingGrace<1||(bound.isPresent()?!AdultBound.valid(bound.get()):legacy<AdultBound.MIN||legacy>AdultBound.LEGACY_MAX))throw new IllegalArgumentException("Invalid saved birth policy");
        adultCapacity=bound.isPresent()?bound.get():AdultBound.fromLegacy(legacy);
        neglectGrace=in.getLongOr("NeglectGrace",BroodRecord.DEFAULT_NEGLECT);waitingBound=in.getLongOr("WaitingBound",BroodRecord.DEFAULT_COCOON_WAIT);
        if(neglectGrace<1||waitingBound<1)throw new IllegalArgumentException("Invalid saved brood policy");
        in.read("Expired",com.mojang.serialization.Codec.unboundedMap(com.mojang.serialization.Codec.STRING,com.mojang.serialization.Codec.STRING)).orElse(java.util.Map.of()).forEach((k,v)->expired.put(UUID.fromString(k),v));
        if (in.getString("Queen").isEmpty()) return;
        UUID id = UUID.fromString(in.getStringOr("Queen", "")); BlockPos entrance = in.read("Entrance", BlockPos.CODEC).orElseThrow();
        Direction direction = Direction.byName(in.getStringOr("Direction", ""));
        if (direction == null || direction.getAxis().isVertical()) throw new IllegalArgumentException("Invalid nursery direction");
        NestPlan p = NestPlan.geometry(entrance, direction);
        stageDuration = in.getLongOr("StageDuration", stageTicks()); developmentCredit = in.getIntOr("DevelopmentCredit", 0);
        if (stageDuration < 1 || stageDuration > Long.MAX_VALUE / LARVA_COST || developmentCredit < 0 || developmentCredit >= BroodCapacity.SPEED_DIVISOR) throw new IllegalArgumentException("Invalid brood duration");
        if (!p.nursery().equals(getBlockPos())) throw new IllegalArgumentException("Misplaced nursery");
        Set<Integer> slots = new HashSet<>(); Set<UUID> ids = new HashSet<>();
        for (var child : in.childrenListOrEmpty("Brood")) {
            BroodRecord r = BroodRecord.load(child);
            if (!id.equals(r.queenId()) || !slots.add(r.slot()) || !ids.add(r.id()) || r.progress() > stageDuration) throw new IllegalArgumentException("Duplicate, foreign or invalid brood");
            records.add(r);
        }
        for (String s : in.read("Consumed", com.mojang.serialization.Codec.STRING.listOf()).orElse(List.of())) consumed.add(UUID.fromString(s));
        original.addAll(in.read("Original",com.mojang.serialization.Codec.STRING.listOf()).orElseGet(()->java.util.stream.Stream.concat(java.util.stream.Stream.concat(consumed.stream(),expired.keySet().stream()),records.stream().filter(BroodRecord::founding).map(BroodRecord::id)).map(UUID::toString).toList()).stream().map(UUID::fromString).toList());
        if (records.size() > BroodCapacity.MAX_SLOTS || ids.stream().anyMatch(consumed::contains)||original.size()!=(generatedInitial?0:CAPACITY)||expired.keySet().stream().anyMatch(consumed::contains)||!java.util.stream.Stream.concat(java.util.stream.Stream.concat(ids.stream(),consumed.stream()),expired.keySet().stream()).toList().containsAll(original)) throw new IllegalArgumentException("Invalid reusable nursery ownership");
        operational=in.getBooleanOr("Operational",false);lastLayingTick=in.getLongOr("LastLayingTick",0);archivedApples=in.getLongOr("ConsumedApples",0);archivedBerries=in.getLongOr("ConsumedBerries",0);archivedChickens=in.getLongOr("ConsumedChickens",0);archivedNectar=in.getLongOr("ConsumedNectar",0);archivedNectarV2=in.getLongOr("ConsumedNectarV2",0);archivedPrey=in.getLongOr("ConsumedPrey",0);archivedFlesh=in.getLongOr("ConsumedFlesh",0);
        if(lastLayingTick<0||lastLayingTick>in.getLongOr("LoadedTicks",0)||archivedApples<0||archivedBerries<0||archivedChickens<0||archivedNectar<0||archivedNectarV2<0||archivedNectarV2>archivedNectar||archivedPrey<0||archivedFlesh<0)throw new IllegalArgumentException("Invalid nutrition history");
        queenId = id; plan = p; loadedTicks = Math.max(0, in.getLongOr("LoadedTicks", 0)); condition = in.getStringOr("Condition", "restored");
        initializeClock();
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
