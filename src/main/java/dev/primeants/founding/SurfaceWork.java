package dev.primeants.founding;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import dev.primeants.PrimeAnts;
import dev.primeants.brood.NurseryBlocks;
import dev.primeants.colony.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.worker.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.world.phys.*;

/** Bounded descriptive surface plans. A real claimed worker is the only physical operation caller. */
public final class SurfaceWork extends SavedData {
    public static final int ACTION_TICKS=20;
    public static final class Job {
        static final Codec<Job> CODEC=Codec.STRING.xmap(Job::decode,Job::encode);
        public final NestPlan home; public final SurfacePlan.Plan plan; public final Map<String,Integer> ground;
        private final List<String> receipts=new ArrayList<>();
        private final Map<String,Integer> transfers=new LinkedHashMap<>();
        public UUID claim; public BlockPos source; public String reason="planned";
        private int recovered,placed,released; public long ticks;
        Job(NestPlan home,SurfacePlan.Plan plan,Map<String,Integer> ground){this.home=home.routeGeometry();this.plan=plan;this.ground=Map.copyOf(ground);validate();}
        public int completed(){return receipts.size();}public int recovered(){return recovered;}public int placed(){return placed;}public int released(){return released;}
        public int carried(){return recovered-placed-released;}public Map<String,Integer> transfers(){return Map.copyOf(transfers);}
        public List<String> receipts(){return List.copyOf(receipts);}public boolean complete(){return completed()==plan.cells().size();}
        public boolean currentComplete(ServerLevel l,UUID owner){return complete()&&!stopped()&&paidProblem(l,owner)==null;}
        String paidProblem(ServerLevel l,UUID owner){
            for(int i=0;i<completed();i++){
                var at=at(i);if(!NestPlan.loaded(l,at))return "unknown_paid_surface";
                if(!ColonyTerrain.get(l).surface(l,at,owner))return "paid_surface_revoked";
                var c=plan.cells().get(i);var expected=c.material().equals("gate")?NurseryBlocks.MOUND_GATE:NurseryBlocks.NEST_SOIL;
                if(!l.getBlockState(at).is(expected)&&!(plan.stage()==ColonyStage.MATURE&&c.component().equals("arch_pillar")&&c.layer()==0&&l.getBlockState(at).is(NurseryBlocks.MOUND_GATE)))return "paid_surface_changed";
            }return null;
        }
        public boolean stopped(){return reason.startsWith("stopped_")||reason.startsWith("quarantined_");}
        public BlockPos at(int index){var c=plan.cells().get(index);return home.at(c.forward(),c.side(),ground.get(c.column())+1+c.layer());}
        public List<BlockPos> cells(){var out=new ArrayList<BlockPos>();for(int i=0;i<plan.cells().size();i++)out.add(at(i));return List.copyOf(out);}
        public BlockPos next(){return at(completed());}
        public Block block(){return plan.cells().get(completed()).material().equals("gate")?NurseryBlocks.MOUND_GATE:NurseryBlocks.NEST_SOIL;}
        public boolean unlocked(ServerLevel l,UUID owner){return MoundSoil.stage(l,home,owner).ordinal()>=plan.stage().ordinal();}
        void receipt(ServerLevel l,String mode,LasiusNigerEntity worker){
            receipts.add(mode+":"+(worker==null?"previous_paid_surface":worker.getUUID())+":"+nextKey());source=null;get(l).setDirty();
        }
        private String nextKey(){return plan.cells().get(receipts.size()).key();}
        private void validate(){
            if(plan.cells().isEmpty()||ground.size()!=plan.cells().stream().map(SurfacePlan.Cell::column).distinct().count()
                ||plan.cells().stream().anyMatch(c->!ground.containsKey(c.column()))||ground.values().stream().anyMatch(y->Math.abs(y)>NestMound.GROUND_RANGE)
                ||receipts.size()>plan.cells().size()||recovered<0||placed<0||released<0||carried()<0||carried()>1||ticks<0
                ||transfers.values().stream().mapToInt(Integer::intValue).sum()!=released||transfers.values().stream().anyMatch(n->n!=1))throw new IllegalArgumentException("Invalid saved surface plan/custody");
            transfers.keySet().forEach(UUID::fromString);
            if(carried()==1&&(claim==null||source==null))throw new IllegalArgumentException("Carried surface unit lacks canonical claim/source");
            int relocationReceipts=0;
            for(int i=0;i<receipts.size();i++){
                var parts=receipts.get(i).split(":",3);
                if(parts.length!=3||!parts[2].equals(plan.cells().get(i).key())||!Set.of("inherited","retained_paid_foundation","paid_gate_conversion","relocated").contains(parts[0]))throw new IllegalArgumentException("Invalid surface receipt prefix");
                if(parts[0].equals("inherited")){if(!parts[1].equals("previous_paid_surface"))throw new IllegalArgumentException("Invalid inherited surface receipt");}
                else UUID.fromString(parts[1]);if(parts[0].equals("relocated"))relocationReceipts++;
            }
            if(relocationReceipts!=placed)throw new IllegalArgumentException("Surface placement receipts disagree with paid movements");
            if(source!=null&&!sourceBounded(home,source))throw new IllegalArgumentException("Surface source outside bounded exterior");
        }
        private String encode(){
            var o=new JsonObject();o.addProperty("entrance",home.entrance().asLong());o.addProperty("direction",home.direction().getName());o.add("plan",JsonParser.parseString(plan.description()));
            var g=new JsonObject();ground.forEach(g::addProperty);o.add("ground",g);var r=new JsonArray();receipts.forEach(r::add);o.add("receipts",r);
            o.addProperty("claim",claim==null?"":claim.toString());if(source!=null)o.addProperty("source",source.asLong());o.addProperty("reason",reason);
            o.addProperty("recovered",recovered);o.addProperty("placed",placed);o.addProperty("released",released);o.addProperty("ticks",ticks);
            var t=new JsonObject();transfers.forEach(t::addProperty);o.add("transfers",t);return o.toString();
        }
        private static Job decode(String text){
            var o=JsonParser.parseString(text).getAsJsonObject();var d=Direction.byName(o.get("direction").getAsString());if(d==null||d.getAxis().isVertical())throw new IllegalArgumentException("Surface direction");
            var g=new LinkedHashMap<String,Integer>();o.getAsJsonObject("ground").entrySet().forEach(e->g.put(e.getKey(),e.getValue().getAsInt()));
            var j=new Job(NestPlan.geometry(BlockPos.of(o.get("entrance").getAsLong()),d),SurfacePlan.compile(o.get("plan").toString()),g);
            o.getAsJsonArray("receipts").forEach(e->j.receipts.add(e.getAsString()));String claim=o.get("claim").getAsString();j.claim=claim.isEmpty()?null:UUID.fromString(claim);
            j.source=o.has("source")?BlockPos.of(o.get("source").getAsLong()):null;j.reason=o.get("reason").getAsString();j.recovered=o.get("recovered").getAsInt();j.placed=o.get("placed").getAsInt();j.released=o.get("released").getAsInt();j.ticks=o.get("ticks").getAsLong();
            o.getAsJsonObject("transfers").entrySet().forEach(e->j.transfers.put(e.getKey(),e.getValue().getAsInt()));j.validate();return j;
        }
    }
    public static final Codec<SurfaceWork> CODEC=Codec.unboundedMap(Codec.STRING,Job.CODEC).xmap(SurfaceWork::new,d->Map.copyOf(d.jobs));
    public static final SavedDataType<SurfaceWork> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("prime_ants","surface_work"),SurfaceWork::new,CODEC,DataFixTypes.LEVEL);
    private final Map<String,Job> jobs;private final Map<UUID,List<Job>> byOwner=new HashMap<>();private final Map<UUID,Long> nextAttempt=new HashMap<>();
    public SurfaceWork(){this(Map.of());}private SurfaceWork(Map<String,Job> jobs){this.jobs=new LinkedHashMap<>(jobs);index();}
    private void index(){
        byOwner.clear();var claims=new HashSet<UUID>();
        for(var e:jobs.entrySet()){
            var parts=e.getKey().split(":");if(parts.length!=2)throw new IllegalArgumentException("Surface owner key");var owner=UUID.fromString(parts[0]);
            if(!parts[1].equals(e.getValue().plan.stage().serializedName()))throw new IllegalArgumentException("Surface stage key");
            byOwner.computeIfAbsent(owner,k->new ArrayList<>()).add(e.getValue());
            if(e.getValue().claim!=null&&!claims.add(e.getValue().claim))throw new IllegalArgumentException("Duplicate surface builder identity");
        }
        byOwner.replaceAll((owner,list)->{if(list.size()>2||list.stream().filter(j->j.claim!=null).count()>1)throw new IllegalArgumentException("Surface builder/plan cap");return List.copyOf(list);});
    }
    public static SurfaceWork get(ServerLevel l){return l.getDataStorage().computeIfAbsent(TYPE);}
    public List<Job> jobs(UUID owner){return byOwner.getOrDefault(owner,List.of());}
    public Job job(UUID owner,ColonyStage s){return jobs.get(owner+":"+s.serializedName());}
    public Job claimedBy(LasiusNigerEntity w){for(var j:jobs(w.queenId()))if(w.getUUID().equals(j.claim))return j;return null;}
    public boolean anyClaim(UUID owner){for(var j:jobs(owner))if(j.claim!=null)return true;return false;}
    public static boolean sourceBounded(NestPlan p,BlockPos source){
        var delta=source.subtract(p.entrance());var d=p.direction();var s=d.getClockWise();int f=delta.getX()*d.getStepX()+delta.getZ()*d.getStepZ(),side=delta.getX()*s.getStepX()+delta.getZ()*s.getStepZ();
        return f>=-NestMound.BACK&&f<0&&Math.abs(side)<=NestMound.SIDE&&side!=0&&delta.getY()>=-NestMound.GROUND_RANGE+1&&delta.getY()<=NestMound.GROUND_RANGE+NestMound.LAYERS;
    }
    private static boolean structuralColumn(NestPlan home,BlockPos at){
        var delta=at.subtract(home.entrance());var d=home.direction();var s=d.getClockWise();int f=delta.getX()*d.getStepX()+delta.getZ()*d.getStepZ(),side=delta.getX()*s.getStepX()+delta.getZ()*s.getStepZ();
        return SurfacePlan.bundled(ColonyStage.GREAT).cells().stream().anyMatch(c->c.forward()==f&&c.side()==side);
    }
    public static boolean recoverable(ServerLevel l,NestPlan home,UUID owner,BlockPos at){
        if(at==null||!sourceBounded(home,at)||structuralColumn(home,at)||!ColonyTerrain.get(l).mound(l,at,owner)||!NestPlan.loaded(l,at.above())
            ||!l.getBlockState(at.above()).isAir()||!l.getFluidState(at.above()).isEmpty()||!l.getEntities(null,new AABB(at).expandTowards(0,2,0).inflate(0.05)).isEmpty())return false;
        if(!NestPlan.loaded(l,at.below())||!l.getBlockState(at.below()).isSolidRender()||!l.getFluidState(at.below()).isEmpty())return false;
        for(var d:Direction.values())if(!NestPlan.loaded(l,at.relative(d)))return false;
        return SupportSurvival.problem(l,at,Blocks.AIR.defaultBlockState())==null;
    }
    public static String targetProblem(ServerLevel l,UUID owner,Job j){
        if(j.complete())return "complete";var target=j.next();if(!NestPlan.loaded(l,target))return "unknown_target";
        var terrain=ColonyTerrain.get(l);boolean paid=terrain.mound(l,target,owner)||terrain.surface(l,target,owner);
        if(!paid&&!terrain.surfacePermission(l,target,owner))return "revoked_or_obstructed_target";
        if(!l.getFluidState(target).isEmpty()||l.getBlockEntity(target)!=null)return "obstructed_target";
        var c=j.plan.cells().get(j.completed());var below=target.below();
        if(!NestPlan.loaded(l,below))return "unknown_support";
        boolean supported=l.getBlockState(below).isSolidRender()&&(NaturalSoil.get(l).floorSupport(l,below)||terrain.mound(l,below,owner)||terrain.surface(l,below,owner));
        if(c.component().equals("arch_lintel")){
            supported=true;for(int side:new int[]{-1,1}){var pillar=j.home.at(c.forward(),side,j.ground.get(c.column())+1+c.layer());
                if(!NestPlan.loaded(l,pillar))return "unknown_support";supported&=terrain.surface(l,pillar,owner)&&l.getBlockState(pillar).isSolidRender();}
        }
        if(!supported)return "unsupported_target";
        if(!l.getEntities(null,new AABB(target)).isEmpty())return "occupied_target";
        var survival=SupportSurvival.problem(l,target,j.block().defaultBlockState());return survival==null?null:survival;
    }
    private Job bind(ServerLevel l,UUID owner,NestPlan home,SurfacePlan.Plan plan){
        var ground=new LinkedHashMap<String,Integer>();
        for(var c:plan.cells())if(!ground.containsKey(c.column())){
            var column=NestMound.column(dy->MoundSoil.read(l,home.at(c.forward(),c.side(),dy),owner),1);
            if(column.ground()==null)return null;ground.put(c.column(),column.ground());
        }
        // A single lintel cannot join pillars on different levels without changing its paid shape.
        if(!ground.get("-1,-1").equals(ground.get("-1,0"))||!ground.get("-1,1").equals(ground.get("-1,0")))return null;
        var j=new Job(home,plan,ground);
        for(var target:j.cells()){
            if(!NestPlan.loaded(l,target))return null;
            var r=MoundSoil.read(l,target,owner);
            if(r==NestMound.Read.OPEN||r==NestMound.Read.PLANT)ColonyTerrain.get(l).permitSurface(l,target,owner);
        }return j;
    }
    public boolean priority(ServerLevel l,UUID owner,NestPlan home){
        var widening=NestExpansion.get(l).job(owner);if(widening!=null&&!widening.complete())return true;
        if(ChamberExcavation.get(l).jobs(owner).stream().anyMatch(j->!j.complete()&&!j.stopped()))return true;
        if(ChamberUpgrade.get(l).jobs(owner).stream().anyMatch(j->!j.complete()&&!j.stopped())||ChamberUpgrade.get(l).hasPriority(l,owner,home))return true;
        var mining=Mining.get(l).job(owner);return mining!=null&&!mining.complete()&&!mining.stopped();
    }
    public void consider(ServerLevel l,LasiusNigerEntity queen,List<LasiusNigerEntity> workers){
        var owner=queen.getUUID();var home=queen.founding().plan();long now=l.getGameTime();if(home==null||now<nextAttempt.getOrDefault(owner,Long.MIN_VALUE))return;
        nextAttempt.put(owner,now+100);reconcile(l,queen);
        var stage=MoundSoil.stage(l,home,owner);if(stage.ordinal()<ColonyStage.MATURE.ordinal()||DigJob.anyClaim(l,owner)||priority(l,owner,home))return;
        Job j=null;for(var wanted:List.of(ColonyStage.MATURE,ColonyStage.GREAT)){
            if(stage.ordinal()<wanted.ordinal())break;var trial=job(owner,wanted);
            if(trial==null){trial=bind(l,owner,home,SurfacePlan.bundled(wanted));if(trial==null)return;jobs.put(owner+":"+wanted.serializedName(),trial);index();setDirty();}
            var paidProblem=trial.paidProblem(l,owner);if(paidProblem!=null){trial.reason=(paidProblem.startsWith("unknown")?"waiting_":"stopped_")+paidProblem;setDirty();return;}
            if(trial.stopped())return;
            // A higher plan can inherit only an owned physically paid surface cell with its actual previous receipt.
            while(!trial.complete()&&ColonyTerrain.get(l).surface(l,trial.next(),owner)&&l.getBlockState(trial.next()).is(trial.block()))trial.receipt(l,"inherited",null);
            if(!trial.complete()){j=trial;break;}
        }
        if(j==null)return;String problem=targetProblem(l,owner,j);if(problem!=null){j.reason="waiting_"+problem;setDirty();return;}
        final Job selected=j;
        var candidates=workers.stream().filter(w->w.workerTasks().canConstruct(home)&&NestExpansion.remainingCaregivers(l,owner,home,w)>=2).sorted(Comparator.comparing(w->w.getUUID().toString())).toList();
        for(var w:candidates){
            boolean paid=ColonyTerrain.get(l).mound(l,j.next(),owner)||ColonyTerrain.get(l).surface(l,j.next(),owner);
            var buildStand=stand(l,w,j.next(),!paid);if(buildStand==null)continue;
            BlockPos source=null,sourceStand=null;
            if(!paid)for(var p:MoundSoil.cells(l,home,owner))if(recoverable(l,home,owner,p)){
                var found=stand(l,w,p,false);if(found!=null){source=p;sourceStand=found;break;}
            }
            if(!paid&&source==null){j.reason="waiting_no_safe_owned_source";setDirty();return;}
            if(w.workerTasks().assignSurface(home,paid,paid?j.next():source,paid?buildStand:sourceStand,buildStand)){j.source=source;j.claim=w.getUUID();j.reason="ordinary_surface_worker_assigned";setDirty();
                PrimeAnts.LOGGER.info("T12 SURFACE CLAIM queen={} worker={} stage={} index={} target={} source={} paid={}",owner,j.claim,j.plan.stage(),j.completed(),j.next(),source,paid);return;}
        }
    }
    public void reconcile(ServerLevel l,LasiusNigerEntity queen){
        for(var j:jobs(queen.getUUID()))if(j.claim!=null&&l.getEntity(j.claim) instanceof LasiusNigerEntity w){
            if(!ColonyMembers.get(l).belongs(w,queen.getUUID(),j.home.chamber())||!w.workerTasks().surfaceWorking()
                ||w.workerTasks().plan()==null||!w.workerTasks().plan().entrance().equals(j.home.entrance())||w.workerTasks().plan().direction()!=j.home.direction()
                ||j.carried()!= (w.getMainHandItem().is(Items.DIRT)?w.getMainHandItem().getCount():0)){
                j.reason="quarantined_incompatible_surface_claim_cargo_retained";if(j.carried()==0)j.claim=null;setDirty();
            }
        }
    }
    public static Vec3 point(ServerLevel l,LasiusNigerEntity w,BlockPos target,boolean empty){return pointFrom(l,w,target,empty,w.position());}
    private static Vec3 pointFrom(ServerLevel l,LasiusNigerEntity w,BlockPos target,boolean empty,Vec3 position){
        var mouth=position.add(0,0.25,0);
        for(double y:new double[]{0.5,0.01,0.99})for(double x:new double[]{0.5,0.01,0.99})for(double z:new double[]{0.5,0.01,0.99}){
            var p=new Vec3(target.getX()+x,target.getY()+y,target.getZ()+z);if(mouth.distanceToSqr(p)>5.0)continue;
            boolean loaded=true;var delta=p.subtract(mouth);for(int n=0;n<=16;n++)if(!NestPlan.loaded(l,BlockPos.containing(mouth.add(delta.scale(n/16.0))))){loaded=false;break;}if(!loaded)continue;
            var hit=l.clip(new net.minecraft.world.level.ClipContext(mouth,p,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.ANY,w));
            if(empty?hit.getType()==HitResult.Type.MISS:hit.getType()==HitResult.Type.BLOCK&&((BlockHitResult)hit).getBlockPos().equals(target))return p;
        }return null;
    }
    public static boolean validStand(ServerLevel l,LasiusNigerEntity w,BlockPos target,boolean empty,BlockPos stand){
        if(stand==null||target==null||!NestPlan.loaded(l,stand)||!NestPlan.loaded(l,stand.above())||!NestPlan.loaded(l,stand.below())||!NestPlan.walkable(l,stand))return false;
        var box=w.getBoundingBox().move(Vec3.atBottomCenterOf(stand).subtract(w.position()));
        return !box.intersects(new AABB(target))&&dev.primeants.entity.Nestmates.movementClear(l,w,box)&&pointFrom(l,w,target,empty,Vec3.atBottomCenterOf(stand))!=null;
    }
    public static BlockPos stand(ServerLevel l,LasiusNigerEntity w,BlockPos target,boolean empty){
        var stands=new ArrayList<BlockPos>();
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(x!=0||z!=0)for(int y=-2;y<=1;y++){
            var p=target.offset(x,y,z);if(!NestPlan.loaded(l,p)||!NestPlan.loaded(l,p.above())||!NestPlan.loaded(l,p.below())||!NestPlan.walkable(l,p)||Vec3.atBottomCenterOf(p).add(0,0.25,0).distanceToSqr(Vec3.atCenterOf(target))>8)continue;
            var box=w.getBoundingBox().move(Vec3.atBottomCenterOf(p).subtract(w.position()));if(box.intersects(new AABB(target))||!dev.primeants.entity.Nestmates.movementClear(l,w,box))continue;
            if(pointFrom(l,w,target,empty,Vec3.atBottomCenterOf(p))!=null)stands.add(p);
        }
        if(stands.contains(w.blockPosition())&&w.onGround()&&point(l,w,target,empty)!=null)return w.blockPosition();
        stands.sort(Comparator.<BlockPos>comparingDouble(p->w.position().distanceToSqr(Vec3.atBottomCenterOf(p))).thenComparingLong(BlockPos::asLong));
        for(var p:stands){var path=w.getNavigation().createPath(p,0,32);if(path!=null&&path.canReach())return p;}return null;
    }
    public boolean recover(ServerLevel l,LasiusNigerEntity w,Job j){
        if(!w.workerTasks().surfaceAuthorized(l)||!j.unlocked(l,w.queenId())||j.carried()!=0||!w.getMainHandItem().isEmpty()||targetProblem(l,w.queenId(),j)!=null
            ||!recoverable(l,j.home,w.queenId(),j.source)||!w.onGround()||point(l,w,j.source,false)==null||!l.mayInteract(w,j.source))return false;
        var source=j.source;if(!l.setBlock(source,Blocks.AIR.defaultBlockState(),3))return false;
        w.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIRT));j.recovered++;setDirty();
        PrimeAnts.LOGGER.info("T12 SURFACE RECOVERY queen={} worker={} source={} target={} recovered={} carried={} position={} excavationReceipt=false actionTicks=20",w.queenId(),w.getUUID(),source,j.next(),j.recovered,j.carried(),w.position());return true;
    }
    public boolean place(ServerLevel l,LasiusNigerEntity w,Job j){
        if(!w.workerTasks().surfaceAuthorized(l)||!j.unlocked(l,w.queenId())||targetProblem(l,w.queenId(),j)!=null||!w.onGround()||!l.mayInteract(w,j.next()))return false;
        var at=j.next();var terrain=ColonyTerrain.get(l);boolean paid=terrain.mound(l,at,w.queenId())||terrain.surface(l,at,w.queenId());
        if(paid?j.carried()!=0||!w.getMainHandItem().isEmpty():j.carried()!=1||!w.getMainHandItem().is(Items.DIRT)||w.getMainHandItem().getCount()!=1)return false;
        if(point(l,w,at,!paid)==null||w.getBoundingBox().intersects(new AABB(at)))return false;
        var block=j.block();boolean retained=paid&&l.getBlockState(at).is(block);
        if(!retained&&!l.setBlock(at,block.defaultBlockState(),3))return false;terrain.surfaceBuilt(at,w.queenId(),block);
        if(!paid){w.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);j.placed++;}
        PrimeAnts.LOGGER.info("T12 SURFACE ACTION queen={} worker={} stage={} index={} component={} target={} block={} existingPaid={} retainedFoundation={} recovered={} relocated={} carried={} position={} stand={} actionTicks=20",w.queenId(),w.getUUID(),j.plan.stage(),j.completed(),j.plan.cells().get(j.completed()).component(),at,block,paid,retained,j.recovered,j.placed,j.carried(),w.position(),w.blockPosition());
        j.receipt(l,retained?"retained_paid_foundation":paid?"paid_gate_conversion":"relocated",w);return true;
    }
    public void relinquish(ServerLevel l,LasiusNigerEntity w,Job j,String reason){
        if(j.carried()!=0)throw new IllegalStateException("Surface cargo must first enter physical custody");j.claim=null;j.source=null;j.reason=reason;setDirty();
    }
    public void released(ServerLevel l,LasiusNigerEntity w,UUID transfer){
        var j=claimedBy(w);if(j==null)return;if(j.carried()==1){j.released++;j.transfers.put(transfer.toString(),1);}j.claim=null;j.source=null;j.reason="surface_unit_released_exactly_once";setDirty();
    }
    public void abandonCargo(ServerLevel l,LasiusNigerEntity w,Job j,String reason){
        var transfer=UUID.nameUUIDFromBytes(("surface-cargo:"+w.queenId()+":"+j.plan.stage()+":"+j.recovered+":"+w.getUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        TransferCustody.get(l).take(transfer,"surface:"+w.getUUID(),w.position(),w.getMainHandItem());w.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);released(l,w,transfer);TransferCustody.get(l).retry(l);j.reason="stopped_"+reason;setDirty();
    }
}
