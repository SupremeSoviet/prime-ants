package dev.primeants.founding;

import com.google.gson.*;
import dev.primeants.brood.*;
import dev.primeants.colony.*;
import dev.primeants.entity.*;
import dev.primeants.worker.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Bounded initial-world-state installation. No worker job, historical receipt, replacement, or neighbor ticket.
 * All validation precedes reservation/fence/mutation, on the server thread. A partial installation is terminal. */
public final class EstablishedGeneration {
    public static final String VERSION="established_v1";
    private static ServerLevel installingLevel; private static UUID installingOwner; private static NestPlan installingHome;
    private static JsonObject installingEvent; private static Set<BlockPos> installingFootprint;
    private EstablishedGeneration(){}
    public static ColonyStage select(UUID queen){int bucket=Math.floorMod(queen.getLeastSignificantBits(),8);return bucket<4?ColonyStage.FOUNDING:bucket<6?ColonyStage.YOUNG:ColonyStage.MATURE;}
    public static UUID lineage(UUID queen,int index){return UUID.nameUUIDFromBytes((queen+":"+VERSION+":worker:"+index).getBytes(StandardCharsets.UTF_8));}
    public static int workers(ColonyStage s){return s==ColonyStage.YOUNG?4:24;}
    /** Unforgeable by ordinary call sites: exists only inside this synchronous production installation. */
    public static boolean initializing(ServerLevel l,UUID owner,NestPlan home){return l==installingLevel&&l.getServer().isSameThread()&&owner.equals(installingOwner)&&installingHome!=null&&home.entrance().equals(installingHome.entrance())&&home.direction()==installingHome.direction();}
    /** All admitted chunk writes during initial installation, including canonical stock-state projections.
     * Runs before mutation/revocation; it grants no permission to any runtime worker. */
    public static void observeWrite(ServerLevel l,BlockPos p){
        if(l!=installingLevel||installingEvent==null)return;
        require(l.getServer().isSameThread()&&installingFootprint.contains(p),"installation_write_outside_frozen_footprint:"+p);
        require(installingEvent.get("blockWrites").getAsInt()<installingEvent.get("blockWriteLimit").getAsInt(),"installation_total_write_budget");NaturalPlacement.count(installingEvent,"blockWrites");
    }
    public record Plan(EstablishedNest.Initial initial,Map<BlockPos,BlockState> edits,List<BlockPos> actors){
        public NestPlan home(){return initial.home();}
        public ColonyStage profile(){return initial.profile();}
        public int editLimit(){return edits.size()+(profile()==ColonyStage.MATURE?3:2);}
    }
    public static Map<BlockPos,BlockState> surface(NestPlan home,ColonyStage stage){
        var out=new LinkedHashMap<BlockPos,BlockState>();
        for(int f=-3;f<=-2;f++)for(int side:new int[]{-3,-2,2,3})out.put(home.at(f,side,1),NurseryBlocks.NEST_SOIL.defaultBlockState());
        if(stage==ColonyStage.MATURE)for(var c:SurfacePlan.bundled(stage).cells())out.put(home.at(c.forward(),c.side(),1+c.layer()),NurseryBlocks.NEST_SOIL.defaultBlockState());
        return Collections.unmodifiableMap(out);
    }
    /** Strict level fit: no slope flattening, no plant removal outside the surface cells, no unknown air. */
    public static Plan candidate(ServerLevel l,BlockPos entrance,Direction direction,ColonyStage stage){
        int adultBound=Integer.getInteger(AdultBound.PROPERTY,AdultBound.MAX);
        if(!AdultBound.valid(adultBound)||AdultBound.effectiveCap(stage,adultBound)<workers(stage)+1)return null;
        var base=NestPlan.candidate(l,entrance,direction);if(base==null||!base.plants().isEmpty())return null;
        var n=EstablishedNest.of(base,stage);var edits=new LinkedHashMap<BlockPos,BlockState>();
        for(var p:n.space())edits.put(p,Blocks.AIR.defaultBlockState());edits.putAll(surface(base,stage));
        var required=new LinkedHashSet<BlockPos>(n.shell());required.addAll(edits.keySet());
        // Retain and validate the canonical supported exterior adaptation used by ordinary worker routes.
        required.add(base.outside().below());required.add(base.outside());required.add(base.outside().above());
        // The complete declared surface envelope includes its supports and all dry adjacent cells.
        for(var p:edits.keySet())for(var d:Direction.values())required.add(p.relative(d));
        for(var p:required)if(!NestPlan.loaded(l,p)||!l.isPositionEntityTicking(p)||!l.getWorldBorder().isWithinBounds(p))return null;
        var soil=NaturalSoil.get(l);var air=GenerationAir.get(l);var plants=NativeVegetation.get(l);
        for(var p:required){
            if(!l.getFluidState(p).isEmpty()||l.getBlockEntity(p)!=null)return null;
            if(n.space().contains(p)){if(!soil.eligible(l,p))return null;}
            else if(p.getY()<entrance.getY()){
                if(!(soil.eligible(l,p)||n.space().contains(p.above())&&soil.floorSupport(l,p)))return null;
            }else if(!(soil.eligible(l,p)||air.eligible(l,p)||plants.eligible(l,p)))return null;
        }
        for(var p:surface(base,stage).keySet()){
            if(!(air.eligible(l,p)||plants.eligible(l,p)))return null;
            var ground=p.atY(entrance.getY());if(!soil.eligible(l,ground))return null;
        }
        // Safe disjoint bodies, one per floor column; canonical marker columns and the stairs stay free.
        var floors=n.space().stream().filter(p->p.getY()==entrance.getY()-2&&n.space().contains(p.above())&&!p.equals(base.nursery())&&!p.equals(base.cache())
            &&!(stage==ColonyStage.MATURE&&p.equals(MaterialStore.marker(base,"left")))).toList();
        var positions=new ArrayList<BlockPos>();positions.add(base.chamber());for(var p:floors)if(!positions.contains(p))positions.add(p);
        if(positions.size()<workers(stage)+1)return null;positions=new ArrayList<>(positions.subList(0,workers(stage)+1));
        var boxes=new ArrayList<AABB>();for(int i=0;i<positions.size();i++){
            var p=positions.get(i);var b=(i==0?AntEntities.QUEEN:AntEntities.WORKER).getSpawnAABB(p.getX()+.5,p.getY(),p.getZ()+.5);
            if(boxes.stream().anyMatch(b::intersects)||!l.getEntities(null,b).isEmpty())return null;boxes.add(b);
        }
        for(var p:required)if(!l.getEntities(null,new AABB(p)).isEmpty())return null;
        return new Plan(n,Collections.unmodifiableMap(edits),List.copyOf(positions));
    }
    static void attempt(ServerLevel l,NaturalPlacement ledger,JsonObject r,ChunkPos chunk,BlockPos surface){
        var stage=ColonyStage.byName(r.get("profile").getAsString());Plan plan=null;
        for(var d:Direction.Plane.HORIZONTAL){NaturalPlacement.count(r,"preliminaryPlans");plan=candidate(l,surface,d,stage);if(plan!=null)break;}
        if(plan==null){ledger.advance(r,"established_complete_footprint_refused");return;}
        var owner=UUID.fromString(r.get("queen").getAsString());var ids=new JsonArray();ids.add(owner.toString());
        for(int i=0;i<workers(stage);i++)ids.add(ColonyMembers.workerId(lineage(owner,i)).toString());
        for(var id:ids)if(l.getEntityInAnyDimension(UUID.fromString(id.getAsString()))!=null){ledger.finish(r,"INDETERMINATE","established_identity_already_present_no_replacement");return;}
        NaturalPlacement.count(r,"immediateValidations");if(!NaturalPlacement.ready(l,chunk)||candidate(l,surface,plan.home().direction(),stage)==null){ledger.advance(r,"established_immediate_revalidation_failed");return;}
        r.addProperty("surface",surface.asLong());r.addProperty("direction",plan.home().direction().getName());r.add("actors",ids);
        r.addProperty("planVersion",VERSION);r.addProperty("provenance","generated_initial:"+VERSION);r.addProperty("editLimit",plan.editLimit());
        r.addProperty("editCounterScope","explicit_space_surface_and_marker_operations");r.addProperty("footprintCells",plan.edits().size());r.addProperty("blockWrites",0);r.addProperty("blockWriteLimit",plan.editLimit()+(stage==ColonyStage.MATURE?2:1));
        r.addProperty("edits",0);r.addProperty("insertedActors",0);r.addProperty("attempts",r.get("attempts").getAsInt()+1);r.addProperty("attemptId",UUID.randomUUID().toString());
        ledger.finish(r,"RESERVED","established_installation_reservation");NaturalPlacement.count(r,"reservationChecks");
        String reservation=ledger.saveVerified(l,r,"profile","planVersion","actors","surface","direction","attemptId","editLimit","footprintCells","blockWriteLimit");
        if(reservation!=null){ledger.finish(r,"INDETERMINATE","established_unverified_reservation_no_mutation");return;}
        NaturalPlacement.count(r,"verifiedReservations");String fence=PlacementReservation.createFence(l,r);
        if(fence!=null){ledger.finish(r,"INDETERMINATE","established_unverified_fence_no_mutation");return;}
        installingLevel=l;installingOwner=owner;installingHome=plan.home();installingEvent=r;installingFootprint=plan.edits().keySet();
        try{
            install(l,r,plan,owner);
            ledger.finish(r,"PLACED","complete_generated_initial_state_live_confirmed");
        }catch(RuntimeException failure){
            r.addProperty("installationFailure",failure.toString());ledger.finish(r,"INDETERMINATE","partial_installation_retained_no_replacement");
        }finally{installingLevel=null;installingOwner=null;installingHome=null;installingEvent=null;installingFootprint=null;}
        String completion=ledger.saveVerified(l,r,"profile","planVersion","actors","edits","blockWrites","insertedActors","attemptId");
        if(completion!=null){r.addProperty("storageDiagnostic",completion);ledger.setDirty();}
        dev.primeants.PrimeAnts.LOGGER.info("ESTABLISHED INSTALL {}",r);
    }
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException(why);}
    private static void write(ServerLevel l,JsonObject r,BlockPos p,BlockState state){require(l.setBlock(p,state,3),"block_write_refused:"+p);NaturalPlacement.count(r,"edits");require(r.get("edits").getAsInt()<=r.get("editLimit").getAsInt(),"edit_budget");}
    private static void install(ServerLevel l,JsonObject r,Plan plan,UUID owner){
        var home=plan.home();var terrain=ColonyTerrain.get(l);
        for(var e:plan.edits().entrySet()){
            write(l,r,e.getKey(),e.getValue());terrain.generated(e.getKey(),owner,e.getValue().isAir()?"open":"surface");
        }
        for(var p:home.plugs())ColonyPlugs.get(l).generatedOpen(l,p,owner);
        write(l,r,home.nursery(),NurseryBlocks.BROOD_PILE.defaultBlockState());write(l,r,home.cache(),NurseryBlocks.NEST_CACHE.defaultBlockState());
        if(plan.profile()==ColonyStage.MATURE)write(l,r,MaterialStore.marker(home,"left"),NurseryBlocks.MATERIAL_STORE.defaultBlockState());
        var actors=new ArrayList<LasiusNigerEntity>();
        for(int i=0;i<plan.actors().size();i++){
            var actor=(i==0?AntEntities.QUEEN:AntEntities.WORKER).create(l,EntitySpawnReason.CHUNK_GENERATION);require(actor!=null,"entity_factory_refused");
            var p=plan.actors().get(i);actor.setUUID(i==0?owner:ColonyMembers.workerId(lineage(owner,i-1)));actor.snapTo(p.getX()+.5,p.getY(),p.getZ()+.5,0,0);
            actor.setOnGround(true);actor.setNoAi(true);actor.initializeGenerated(home,owner,i-1);
            require(l.noCollision(actor,actor.getBoundingBox())&&l.getEntities(actor,actor.getBoundingBox()).isEmpty(),"actor_fit_changed");
            NaturalPlacement.count(r,"insertionCalls");boolean inserted=l.addFreshEntity(actor);
            require(inserted&&l.getEntity(actor.getUUID())==actor&&!actor.isRemoved(),"actor_insertion_refused_or_uncertain:"+actor.getUUID());
            NaturalPlacement.count(r,"insertedActors");actors.add(actor);
        }
        var queen=actors.getFirst();queen.founding().initializeGenerated(l,home);
        require(l.getBlockEntity(home.nursery()) instanceof BroodPile,"nursery_block_entity");
        var pile=(BroodPile)l.getBlockEntity(home.nursery());pile.initializeGenerated(queen,home);
        var cache=(NestCache)l.getBlockEntity(home.cache());cache.initializeGenerated(l,owner,home);
        var registry=ChamberRegistry.get(l);registry.found(owner,home);
        if(plan.profile()==ColonyStage.MATURE){
            var store=(MaterialStore)l.getBlockEntity(MaterialStore.marker(home,"left"));store.initializeGenerated(l,owner,home);
            for(var name:List.of("left","hall_left")){var b=ChamberExcavation.build(home,name);require(registry.register(owner,new ChamberRegistry.Chamber(b.room(),b.min(),b.max(),EnumSet.of(b.function()),1,b.marker()==null?Map.of():Map.of(b.function(),b.marker()))),"chamber_registration");}
        }
        EstablishedNest.get(l).installed(l,owner,plan.initial());
        for(int i=1;i<actors.size();i++)ColonyMembers.get(l).generated(l,home,lineage(owner,i-1),owner);
        var live=ColonyDevelopment.evaluate(l,owner,home,true,queen.colonyAdultCapacity(),0);
        require(live.stage()==plan.profile()&&live.cap()>=actors.size()&&live.chambers().stream().allMatch(c->c.functions().values().stream().allMatch(p->p==ColonyDevelopment.Presence.CONFIRMED)),"initial_live_stage_or_chambers:"+live);
        for(var actor:actors)actor.setNoAi(false);
        r.addProperty("liveStage",live.stage().serializedName());r.addProperty("liveAdultCap",live.cap());r.addProperty("initialFood",cache.size());r.addProperty("initialClay",plan.profile()==ColonyStage.MATURE?16:0);
    }
}
