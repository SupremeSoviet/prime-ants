package dev.primeants.brood;

import dev.primeants.PrimeAnts;
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

/** Reusable physical nursery slots. Only the loaded server block-entity ticker advances brood or lays eggs. */
public final class BroodPile extends BlockEntity {
    public static final int CAPACITY = 3;
    public static final int ADULT_CAPACITY=30; // queen + at most 29 actual workers; unknown unloaded members occupy space
    public static final long BASE_LAYING_TICKS=1200;
    public static long layingCadence(){return new SimulationTimeScale(multiplier()).ticksForGameDays(BASE_LAYING_TICKS/24000.0);}
    public static final long EGG_COST = 1000, LARVA_COST = 12000, MAX_RESERVE = CAPACITY * (EGG_COST + LARVA_COST);
    public static final double CARE_REACH_SQUARED = 2.25 * 2.25;
    private final dev.primeants.worker.FoodLimitedGrowth growth=new dev.primeants.worker.FoodLimitedGrowth();
    public dev.primeants.worker.FoodLimitedGrowth growth(){return growth;}
    private UUID queenId;
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
    private long adultLifespan=dev.primeants.entity.AdultLife.DEFAULT_LIFESPAN,fastingGrace=dev.primeants.entity.AdultLife.DEFAULT_FASTING;
    private int adultCapacity=ADULT_CAPACITY;
    public int adultCapacity(){return adultCapacity;}
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
    private boolean lay(ServerLevel l,LasiusNigerEntity q,boolean care){
        if(!care||!operational||!originalTerminal()||loadedTicks-lastLayingTick<layingCadence())return false;
        if(records.size()>=CAPACITY){if(!condition.equals("larva_sugar_or_protein_exhausted"))condition="nursery_slots_full";return false;}
        if(ColonyMembers.get(l).occupied(queenId)+records.size()>=adultCapacity-1){condition="colony_capacity_full_or_unloaded";return false;}
        if(!growth.allows(supply(l))){condition=growth.reason();return false;}
        if(!q.nutrition().spend(Nutrition.EGG_SUGAR,Nutrition.EGG_PROTEIN)){condition="queen_ingested_nutrition_exhausted";return false;}
        int slot=0;while(true){final int index=slot;if(records.stream().noneMatch(r->r.slot()==index))break;slot++;}
        var r=new BroodRecord(UUID.randomUUID(),queenId,slot,neglectGrace,waitingBound);r.founding=false;records.add(r);lastLayingTick=loadedTicks;condition="food_fed_egg_laid";
        PrimeAnts.LOGGER.info("Food-fed egg queen={} brood={} slot={} pileTicks={} cadence={} sugarCost={} proteinCost={}",queenId,r.id(),slot,loadedTicks,layingCadence(),Nutrition.EGG_SUGAR,Nutrition.EGG_PROTEIN);return true;
    }
    private void changed() {
        setChanged();
        if (level == null || level.isClientSide()) return;
        BlockState before = getBlockState(), next = before;
        var properties = List.of(BroodPileBlock.A, BroodPileBlock.B, BroodPileBlock.C);
        for (int slot = 0; slot < CAPACITY; slot++) {
            final int index = slot;
            BroodStage stage = records.stream().filter(r -> r.slot() == index).map(BroodRecord::stage).findFirst().orElse(BroodStage.EMPTY);
            next = next.setValue(properties.get(slot), stage);
        }
        if (!next.equals(before)) level.setBlock(getBlockPos(), next, 3);
        level.sendBlockUpdated(getBlockPos(), before, next, 3);
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
    private boolean unsupported(ServerLevel server,BroodRecord r,String reason){
        r.neglectReason=reason;
        if(r.stage()==BroodStage.COCOON){if(r.waitingTicks<Long.MAX_VALUE)r.waitingTicks++;if(r.waitingTicks>=r.waitingBound()){expire(server,r,"cocoon_wait_"+reason);return true;}}
        else{if(r.neglectTicks<Long.MAX_VALUE)r.neglectTicks++;if(r.neglectTicks>=r.neglectGrace()){expire(server,r,"neglect_"+reason);return true;}}
        return false;
    }
    void serverTick(ServerLevel server) {
        if (queenId == null || plan == null || !ownedBy(queenId, plan)) return;
        loadedTicks++;
        growth.observe(loadedTicks,supply(server));
        var owner = server.getEntity(queenId);
        if(owner instanceof LasiusNigerEntity q && q.founding().lifecycle()!=dev.primeants.founding.QueenFounding.Lifecycle.CLAUSTRAL)operational=true;
        for(UUID id:consumed)ColonyMembers.get(server).record(id,queenId,plan.chamber());
        expired.replaceAll((id,row)->BroodHistory.get(server).retain(id,row));
        String habitat = plan.nurseryProblem(server, queenId, operational);
        LasiusNigerEntity queen = owner instanceof LasiusNigerEntity q && q.isAlive() && !q.isRemoved() ? q : null;
        boolean care = habitat==null && queen != null && !queen.isNoAi() && queen.founding().ready()
                && queen.position().distanceToSqr(Vec3.atBottomCenterOf(getBlockPos())) <= CARE_REACH_SQUARED;
        condition = habitat!=null?habitat:care?"cared":"caregiver_absent_or_out_of_reach";
        boolean visual = false;
        for (BroodRecord record : List.copyOf(records)) {
            if (consumed.contains(record.id())) { records.remove(record); visual = true; continue; }
            // Verified real workers take precedence, before habitat failure and before terminal inference.
            if(reconcileMember(server,record)){visual=true;continue;}
            if(ColonyMembers.get(server).member(record.workerId())!=null||server.getEntity(record.workerId())!=null){condition="emergence_identity_conflict";visual|=unsupported(server,record,condition);continue;}
            String terminal=BroodHistory.get(server).terminal(record.id());
            if(terminal!=null){var row=com.google.gson.JsonParser.parseString(terminal).getAsJsonObject();if(!row.get("queen").getAsString().equals(queenId.toString())||row.get("home").getAsLong()!=plan.chamber().asLong())throw new IllegalStateException("Terminal brood lineage conflict");expired.put(record.id(),terminal);records.remove(record);visual=true;continue;}
            if(habitat!=null){visual|=unsupported(server,record,habitat);continue;}
            if(record.stage==BroodStage.EGG){
                if(!care){visual|=unsupported(server,record,"egg_care_missing");continue;}
                // Actual valid care and development, never mere caregiver presence.
                record.neglectTicks=0;record.neglectReason="";
            }
            if (record.stage == BroodStage.LARVA) {
                long next = Math.min(stageDuration, record.progress + 1);
                long required = (next * LARVA_COST / stageDuration) - record.nourishment;
                if(record.founding){
                    if(required>0||record.nourishment<LARVA_COST){
                        if(!care||queen.bodyReserve()==0||!queen.spendReserve(required)){condition=care?"queen_reserve_exhausted":"larva_care_missing";visual|=unsupported(server,record,condition);continue;}
                    }
                }else{
                    long sugar=next*Nutrition.LARVA_SUGAR/stageDuration-record.nutrition().spentSugar();
                    long protein=next*Nutrition.LARVA_PROTEIN/stageDuration-record.nutrition().spentProtein();
                    if(record.nourishment<LARVA_COST&&(record.nutrition().sugar()==0||record.nutrition().protein()==0||!record.nutrition().spend(sugar,protein))){condition="larva_sugar_or_protein_exhausted";visual|=unsupported(server,record,condition);continue;}
                }
                record.nourishment = record.founding?record.nourishment+required:record.nutrition().spentSugar()+record.nutrition().spentProtein();
                // Zero-cost rounding and failed feeds do not renew neglected viability.
                if(required>0){record.neglectTicks=0;record.neglectReason="";}
            }
            if (record.progress < stageDuration) {record.progress++;if(record.stage==BroodStage.COCOON)record.waitingTicks=0;}
            if (record.progress < stageDuration) continue;
            if (record.stage == BroodStage.COCOON) { if (emerge(server, record)) visual = true;else visual|=unsupported(server,record,condition); }
            else {
                if (record.stage == BroodStage.LARVA && record.nourishment != LARVA_COST) continue;
                record.stage = record.stage == BroodStage.EGG ? BroodStage.LARVA : BroodStage.COCOON;
                record.progress = 0; visual = true;
                PrimeAnts.LOGGER.info("Brood stage queen={} brood={} stage={} pileTicks={} nourishment={}", queenId, record.id(), record.stage, loadedTicks, record.nourishment);
            }
        }
        if(lay(server,queen,care))visual=true;
        if (visual) changed(); else setChanged();
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
        if(ColonyMembers.get(server).occupied(queenId)+queenSlot>=adultCapacity){condition="emergence_capacity_full_or_unloaded";return false;}
        var worker = AntEntities.WORKER.create(server, EntitySpawnReason.BREEDING);
        if (worker == null) { condition = "worker_creation_failed"; return false; }
        worker.setUUID(r.workerId()); worker.initializeCallow(r.id(), queenId, plan.chamber(),adultLifespan,fastingGrace);
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
        growth.save(out.child("GrowthFlow"));
        super.saveAdditional(out);
        if (queenId == null || plan == null) return;
        out.putString("Queen", queenId.toString()); out.store("Entrance", BlockPos.CODEC, plan.entrance()); out.putString("Direction", plan.direction().getName());
        out.putLong("LoadedTicks", loadedTicks); out.putString("Condition", condition);
        out.putLong("StageDuration", stageDuration);
        out.putLong("NeglectGrace",neglectGrace);out.putLong("WaitingBound",waitingBound);
        out.store("Expired",com.mojang.serialization.Codec.unboundedMap(com.mojang.serialization.Codec.STRING,com.mojang.serialization.Codec.STRING),expired.entrySet().stream().collect(java.util.stream.Collectors.toMap(e->e.getKey().toString(),java.util.Map.Entry::getValue)));
        out.putLong("AdultLifespan",adultLifespan);out.putLong("AdultFastingGrace",fastingGrace);out.putInt("AdultCapacity",adultCapacity);
        out.putBoolean("Operational",operational);out.putLong("LastLayingTick",lastLayingTick);out.putLong("ConsumedApples",archivedApples);out.putLong("ConsumedBerries",archivedBerries);out.putLong("ConsumedChickens",archivedChickens);out.putLong("ConsumedNectar",archivedNectar);out.putLong("ConsumedNectarV2",archivedNectarV2);out.putLong("ConsumedPrey",archivedPrey);out.putLong("ConsumedFlesh",archivedFlesh);
        out.store("Original",com.mojang.serialization.Codec.STRING.listOf(),original.stream().map(UUID::toString).sorted().toList());
        var list = out.childrenList("Brood"); for (BroodRecord r : records) r.save(list.addChild());
        out.store("Consumed", com.mojang.serialization.Codec.STRING.listOf(), consumed.stream().map(UUID::toString).sorted().toList());
    }
    @Override protected void loadAdditional(ValueInput in) {
        growth.load(in.childOrEmpty("GrowthFlow"));
        super.loadAdditional(in); records.clear(); consumed.clear();original.clear();expired.clear(); queenId = null; plan = null;
        adultLifespan=in.getLongOr("AdultLifespan",dev.primeants.entity.AdultLife.DEFAULT_LIFESPAN);fastingGrace=in.getLongOr("AdultFastingGrace",dev.primeants.entity.AdultLife.DEFAULT_FASTING);adultCapacity=in.getIntOr("AdultCapacity",ADULT_CAPACITY);
        if(adultLifespan<1||fastingGrace<1||adultCapacity<4||adultCapacity>ADULT_CAPACITY)throw new IllegalArgumentException("Invalid saved birth policy");
        neglectGrace=in.getLongOr("NeglectGrace",BroodRecord.DEFAULT_NEGLECT);waitingBound=in.getLongOr("WaitingBound",BroodRecord.DEFAULT_COCOON_WAIT);
        if(neglectGrace<1||waitingBound<1)throw new IllegalArgumentException("Invalid saved brood policy");
        in.read("Expired",com.mojang.serialization.Codec.unboundedMap(com.mojang.serialization.Codec.STRING,com.mojang.serialization.Codec.STRING)).orElse(java.util.Map.of()).forEach((k,v)->expired.put(UUID.fromString(k),v));
        if (in.getString("Queen").isEmpty()) return;
        UUID id = UUID.fromString(in.getStringOr("Queen", "")); BlockPos entrance = in.read("Entrance", BlockPos.CODEC).orElseThrow();
        Direction direction = Direction.byName(in.getStringOr("Direction", ""));
        if (direction == null || direction.getAxis().isVertical()) throw new IllegalArgumentException("Invalid nursery direction");
        NestPlan p = NestPlan.geometry(entrance, direction);
        stageDuration = in.getLongOr("StageDuration", stageTicks());
        if (stageDuration < 1 || stageDuration > Long.MAX_VALUE / LARVA_COST) throw new IllegalArgumentException("Invalid brood duration");
        if (!p.nursery().equals(getBlockPos())) throw new IllegalArgumentException("Misplaced nursery");
        Set<Integer> slots = new HashSet<>(); Set<UUID> ids = new HashSet<>();
        for (var child : in.childrenListOrEmpty("Brood")) {
            BroodRecord r = BroodRecord.load(child);
            if (!id.equals(r.queenId()) || !slots.add(r.slot()) || !ids.add(r.id()) || r.progress() > stageDuration) throw new IllegalArgumentException("Duplicate, foreign or invalid brood");
            records.add(r);
        }
        for (String s : in.read("Consumed", com.mojang.serialization.Codec.STRING.listOf()).orElse(List.of())) consumed.add(UUID.fromString(s));
        original.addAll(in.read("Original",com.mojang.serialization.Codec.STRING.listOf()).orElseGet(()->java.util.stream.Stream.concat(java.util.stream.Stream.concat(consumed.stream(),expired.keySet().stream()),records.stream().filter(BroodRecord::founding).map(BroodRecord::id)).map(UUID::toString).toList()).stream().map(UUID::fromString).toList());
        if (records.size() > CAPACITY || ids.stream().anyMatch(consumed::contains)||original.size()!=CAPACITY||expired.keySet().stream().anyMatch(consumed::contains)||!java.util.stream.Stream.concat(java.util.stream.Stream.concat(ids.stream(),consumed.stream()),expired.keySet().stream()).toList().containsAll(original)) throw new IllegalArgumentException("Invalid reusable nursery ownership");
        operational=in.getBooleanOr("Operational",false);lastLayingTick=in.getLongOr("LastLayingTick",0);archivedApples=in.getLongOr("ConsumedApples",0);archivedBerries=in.getLongOr("ConsumedBerries",0);archivedChickens=in.getLongOr("ConsumedChickens",0);archivedNectar=in.getLongOr("ConsumedNectar",0);archivedNectarV2=in.getLongOr("ConsumedNectarV2",0);archivedPrey=in.getLongOr("ConsumedPrey",0);archivedFlesh=in.getLongOr("ConsumedFlesh",0);
        if(lastLayingTick<0||lastLayingTick>in.getLongOr("LoadedTicks",0)||archivedApples<0||archivedBerries<0||archivedChickens<0||archivedNectar<0||archivedNectarV2<0||archivedNectarV2>archivedNectar||archivedPrey<0||archivedFlesh<0)throw new IllegalArgumentException("Invalid nutrition history");
        queenId = id; plan = p; loadedTicks = Math.max(0, in.getLongOr("LoadedTicks", 0)); condition = in.getStringOr("Condition", "restored");
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
