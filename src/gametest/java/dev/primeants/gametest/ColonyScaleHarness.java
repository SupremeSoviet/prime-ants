package dev.primeants.gametest;

import com.google.gson.*;
import com.mojang.serialization.*;
import dev.primeants.PrimeAnts;
import dev.primeants.brood.*;
import dev.primeants.colony.*;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Opt-in controlled cohort and disk-restart observer, never included in production jars. */
public final class ColonyScaleHarness implements ModInitializer {
    private static ColonyScaleHarness active;
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private final String phase=System.getProperty("prime_ants.scalePhase","");
    private final Path evidence=Path.of(System.getProperty("prime_ants.scaleEvidence","build/scale-evidence"));
    private JsonObject declaration;
    private TicketType ticket;
    private ServerLevel level;
    private final List<ChunkPos> sites=new ArrayList<>();
    private final List<UUID> owners=new ArrayList<>();
    private final Set<UUID> expected=new TreeSet<>();
    private final Map<UUID,Long> previousAge=new HashMap<>();
    private final Map<UUID,Vec3> previousPosition=new HashMap<>();
    private final JsonObject firstActors=new JsonObject(),firstBlocks=new JsonObject();
    private final List<long[]> rows=new ArrayList<>();
    private final List<long[]> ages=new ArrayList<>();
    private boolean initialized,done;
    private int ticks,loaded;
    private long loadStarted;
    private ChunkPos youngSite;private UUID youngOwner;private boolean youngRegistered;
    private static final List<String> TABLES=List.of("founding.NaturalPlacement","founding.EstablishedNest","founding.NaturalSoil","founding.GenerationAir","founding.ColonyTerrain","founding.ColonyPlugs","founding.ChamberExcavation","worker.ColonyMembers","colony.ChamberRegistry","worker.AdultHistory","brood.BroodHistory","worker.TransferCustody","founding.NestExpansion","founding.ChamberUpgrade","founding.SurfaceWork","founding.Mining","worker.ColonyAlarm");
    @Override public void onInitialize(){
        if(phase.isEmpty())return;active=this;
        try{declaration=JsonParser.parseString(Files.readString(Path.of(System.getProperty("prime_ants.scaleDeclaration")))).getAsJsonObject();}catch(Exception ex){throw new IllegalStateException("Frozen declaration required",ex);}
        ticket=Registry.register(BuiltInRegistries.TICKET_TYPE,Identifier.fromNamespaceAndPath("prime_ants_test","t10_scale"),new TicketType(0,14));
        ServerEntityEvents.ENTITY_LOAD.register((a,l)->{if(reopen()&&a instanceof LasiusNigerEntity)firstActors.add(a.getUUID().toString(),actor(a,l));});
        ServerLifecycleEvents.SERVER_STARTED.register(s->{try{start(s);}catch(Throwable ex){fail(s,ex);}});
        ServerTickEvents.END_SERVER_TICK.register(s->{if(done)return;try{observe(s);}catch(Throwable ex){fail(s,ex);}});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->write(phase+"-stopped",process(s)));
    }
    private boolean reopen(){return phase.startsWith("reopen");}
    public static void decoded(BlockEntity b,HolderLookup.Provider registries){if(active==null||!active.reopen()||active.done||!(b instanceof BroodPile||b instanceof NestCache||b instanceof MaterialStore))return;if(!active.firstBlocks.has(b.getBlockPos().toShortString()))active.firstBlocks.add(b.getBlockPos().toShortString(),tag(b.saveWithFullMetadata(registries)));}
    private static JsonElement tag(CompoundTag t){return NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE,t);}
    private static JsonElement actor(Entity a,ServerLevel l){var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());require(a.save(out),"Actor save");return tag(out.buildResult());}
    private static void require(boolean b,String m){if(!b)throw new IllegalStateException(m);}
    private void write(String n,JsonElement j){try{Files.createDirectories(evidence);Files.writeString(evidence.resolve(n+".json"),JSON.toJson(j),StandardCharsets.UTF_8);}catch(Exception e){throw new RuntimeException(e);}}
    private JsonObject process(MinecraftServer s){var j=new JsonObject();j.addProperty("pid",ProcessHandle.current().pid());j.addProperty("phase",phase);j.addProperty("tick",s.getTickCount());j.addProperty("gameTime",level==null?-1:level.getGameTime());j.addProperty("world",s.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().toString());j.addProperty("java",System.getProperty("java.version"));j.addProperty("maxHeap",Runtime.getRuntime().maxMemory());j.add("jvmArgs",JSON.toJsonTree(java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments()));return j;}
    private void fail(MinecraftServer s,Throwable ex){done=true;var j=process(s);j.addProperty("failure",ex.toString());j.addProperty("harnessTicks",ticks);write(phase+"-failure",j);PrimeAnts.LOGGER.error("T10 scale failure",ex);s.halt(false);}
    private void hold(ChunkPos p){level.getChunkSource().addTicketWithRadius(ticket,p,4);level.getChunkSource().getChunk(p.x(),p.z(),ChunkStatus.FULL,true);}
    private void start(MinecraftServer s){
        loadStarted=System.nanoTime();level=s.getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.parse(declaration.get("dimension").getAsString())));require(level!=null,"Declared dimension");
        write(phase+"-process",process(s));
        if(reopen()||phase.equals("prepare")){
            var manifest=read("cohort");manifest.getAsJsonArray("owners").forEach(x->owners.add(UUID.fromString(x.getAsString())));manifest.getAsJsonArray("sites").forEach(x->{var a=x.getAsJsonArray();sites.add(new ChunkPos(a.get(0).getAsInt(),a.get(1).getAsInt()));});manifest.getAsJsonArray("expected").forEach(x->expected.add(UUID.fromString(x.getAsString())));
            write(phase+"-first-tables",tables());for(var p:sites)hold(p);
            if(phase.equals("prepare")){youngSite=select(6300,ColonyStage.YOUNG);sites.add(youngSite);hold(youngSite);}
        }else{
            int colonies=phase.equals("one")||phase.equals("probe")?1:3;
            for(int i=0;i<colonies;i++){
                int base=declaration.has("bases")?declaration.getAsJsonArray("bases").get(i).getAsInt():6000+100*i;ChunkPos selected=null;
                for(int cell=0;cell<16&&selected==null;cell++)for(int x=base+4*cell;x<base+4*cell+4;x++)for(int z=base;z<base+4;z++){var p=new ChunkPos(x,z);if(NaturalPlacement.selected(level,p)&&NaturalPlacement.selectedProfile(level,p)==ColonyStage.MATURE)selected=p;}
                require(selected!=null,"Declared deterministic Mature candidate");sites.add(selected);hold(selected);
            }
        }
    }
    private ChunkPos select(int base,ColonyStage stage){ChunkPos selected=null;for(int cell=0;cell<16&&selected==null;cell++)for(int x=base+4*cell;x<base+4*cell+4;x++)for(int z=base;z<base+4;z++){var p=new ChunkPos(x,z);if(NaturalPlacement.selected(level,p)&&NaturalPlacement.selectedProfile(level,p)==stage)selected=p;}require(selected!=null,"Declared generation candidate");return selected;}
    private JsonObject read(String n){try{return JsonParser.parseString(Files.readString(evidence.resolve(n+".json"))).getAsJsonObject();}catch(Exception e){throw new RuntimeException(e);}}
    @SuppressWarnings({"unchecked","rawtypes"}) private JsonObject tables(){var j=new JsonObject();for(var n:TABLES)try{Class<?> c=Class.forName("dev.primeants."+n);Object value=c.getMethod("get",ServerLevel.class).invoke(null,level);Codec codec=(Codec)c.getField("CODEC").get(null);j.add(n,(JsonElement)codec.encodeStart(JsonOps.INSTANCE,value).getOrThrow());}catch(Exception e){throw new IllegalStateException("Table snapshot "+n,e);}return j;}
    private void drop(BlockPos at,Item item,int count,String name){var stack=new ItemStack(item,count);stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(name));var v=Vec3.atBottomCenterOf(at);var entity=new ItemEntity(level,v.x,v.y+.1,v.z,stack,0,0,0);entity.setDefaultPickUpDelay();require(level.addFreshEntity(entity),"Finite supplied drop");}
    private void supply(LasiusNigerEntity q){var p=q.founding().plan();drop(p.outside(),Items.APPLE,64,"T10 finite food "+q.getUUID());drop(p.outside(),Items.CHICKEN,32,"T10 finite protein "+q.getUUID());drop(p.outside(),Items.CLAY_BALL,16,"T10 finite clay "+q.getUUID());}
    private void cohort(LasiusNigerEntity q){
        var p=q.founding().plan();require(ColonyMembers.get(level).occupied(q.getUUID())==24,"Generated Mature base exactly 25 living adults");
        var supplied=new HashSet<UUID>();
        for(int i=0;i<95;i++){
            var lineage=UUID.nameUUIDFromBytes(("t10:supplied:"+q.getUUID()+":"+i).getBytes(StandardCharsets.UTF_8));var w=AntEntities.WORKER.create(level,EntitySpawnReason.COMMAND);require(w!=null,"Supplied worker creation");w.setUUID(ColonyMembers.workerId(lineage));w.initializeCallow(lineage,q.getUUID(),p.chamber());w.setPos(Vec3.atBottomCenterOf(p.at(3+i%3,i%2==0?-1:0,-2)));
            var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());w.save(out);var t=out.buildResult();t.putLong("CallowTicks",BroodPile.callowTicks());t.putLong("AntElapsedAgeTicks",4800+i*10L);w.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),t));require(!w.isCallow()&&!w.isNoAi(),"Controlled mature starting adult");
            require(w.nutrition().ingest(new ItemStack(Items.APPLE),Nutrition.QUEEN_SUGAR_CAPACITY,0),"Finite preboundary supplied meal");require(level.addFreshEntity(w),"Real supplied worker insertion");ColonyMembers.get(level).record(lineage,q.getUUID(),p.chamber());supplied.add(w.getUUID());
        }
        var encoded=ColonyMembers.CODEC.encodeStart(JsonOps.INSTANCE,ColonyMembers.get(level)).getOrThrow().getAsJsonArray();for(var row:encoded){var m=row.getAsJsonObject();if(supplied.contains(UUID.fromString(m.get("worker").getAsString())))m.addProperty("origin","controlled_supplied:t10");}level.getDataStorage().set(ColonyMembers.TYPE,ColonyMembers.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow());
        owners.add(q.getUUID());expected.add(q.getUUID());for(var m:ColonyMembers.get(level).members(q.getUUID())){expected.add(m.worker());var w=(LasiusNigerEntity)level.getEntity(m.worker());require(w!=null&&ColonyMembers.get(level).belongs(w,q.getUUID(),p.chamber()),"Canonical cohort membership");}
        supply(q);
    }
    private List<LasiusNigerEntity> ants(){var result=new ArrayList<LasiusNigerEntity>();for(var a:level.getAllEntities())if(a instanceof LasiusNigerEntity ant&&ant.isAlive())result.add(ant);result.sort(Comparator.comparing(Entity::getUUID));return result;}
    private JsonObject snapshot(MinecraftServer s){var j=process(s);j.add("tables",tables());var actors=new JsonObject();var blocks=new JsonObject();var terrain=new JsonObject();var items=new JsonObject();
        for(var a:level.getAllEntities()){if(a instanceof LasiusNigerEntity)actors.add(a.getUUID().toString(),actor(a,level));if(a instanceof ItemEntity)items.add(a.getUUID().toString(),actor(a,level));}j.add("actors",actors);j.add("items",items);
        for(var id:owners)if(level.getEntity(id) instanceof LasiusNigerEntity q){var p=q.founding().plan();for(var at:List.of(p.nursery(),p.cache(),MaterialStore.marker(p,"left"),MaterialStore.marker(p,"right"))){var b=level.getBlockEntity(at);if(b!=null)blocks.add(at.toShortString(),tag(b.saveWithFullMetadata(level.registryAccess())));}for(int f=-6;f<=10;f++)for(int side=-6;side<=6;side++)for(int y=-4;y<=5;y++){var at=p.at(f,side,y);terrain.addProperty(at.toShortString(),level.getBlockState(at).toString());}}
        j.add("blocks",blocks);j.add("terrain",terrain);j.add("firstDecodedActors",firstActors.deepCopy());j.add("firstDecodedBlocks",firstBlocks.deepCopy());return j;
    }
    private void finish(MinecraftServer s){done=true;write(phase+"-before-close",snapshot(s));var save=process(s);long now=System.nanoTime();require(s.saveEverything(false,true,true),"Normal whole-world save");save.addProperty("saveNs",System.nanoTime()-now);save.addProperty("completed",true);write(phase+"-save",save);s.halt(false);}
    private void manifest(){var j=new JsonObject();j.add("owners",JSON.toJsonTree(owners));j.add("expected",JSON.toJsonTree(expected));var a=new JsonArray();for(var p:sites){var r=new JsonArray();r.add(p.x());r.add(p.z());a.add(r);}j.add("sites",a);write("cohort",j);}
    private JsonObject runtime(MinecraftServer s){var j=process(s);j.addProperty("workMultiplier",QueenFounding.multiplier());j.addProperty("broodMultiplier",BroodPile.multiplier());j.addProperty("workCadence",QueenFounding.cadence());j.addProperty("stageTicks",BroodPile.stageTicks());j.addProperty("callowTicks",BroodPile.callowTicks());j.addProperty("layingTicks",BroodPile.layingCadence());j.addProperty("lifespanDefault",AdultLife.DEFAULT_LIFESPAN);j.addProperty("fastingDefault",AdultLife.DEFAULT_FASTING);j.addProperty("tickrate",s.tickRateManager().tickrate());j.addProperty("sprint",s.tickRateManager().isSprinting());j.addProperty("dimension",level.dimension().identifier().toString());j.addProperty("seed",level.getSeed());j.addProperty("loadingNs",System.nanoTime()-loadStarted);var contexts=new JsonObject();for(var l:s.getAllLevels()){var r=new JsonObject();long entities=0,ants=0;for(var a:l.getAllEntities()){entities++;if(a instanceof LasiusNigerEntity)ants++;}r.addProperty("chunks",l.getChunkSource().getLoadedChunksCount());r.addProperty("entities",entities);r.addProperty("ants",ants);contexts.add(l.dimension().identifier().toString(),r);}j.add("loadedDimensionContext",contexts);var caps=new JsonObject();for(var id:owners)if(level.getEntity(id) instanceof LasiusNigerEntity q){var r=new JsonObject();r.addProperty("operatorBound",q.colonyAdultCapacity());r.addProperty("actualAdults",ColonyMembers.get(level).occupied(id)+1);var p=(BroodPile)level.getBlockEntity(q.founding().plan().nursery());if(p.stageEvaluation()!=null){r.addProperty("stage",p.stageEvaluation().stage().serializedName());r.addProperty("cap",p.stageEvaluation().cap());}r.addProperty("condition",p.condition());caps.add(id.toString(),r);}j.add("effectiveCaps",caps);return j;}
    private void observe(MinecraftServer s){
        long observer=System.nanoTime();ticks++;require(ticks<=60000,"Finite preparation bound");require(s.tickRateManager().tickrate()==20&&!s.tickRateManager().isSprinting()&&!s.tickRateManager().isFrozen(),"Ordinary 20 TPS only");require(QueenFounding.multiplier()==1&&BroodPile.multiplier()==1,"Default scaling 1/1");
        if(phase.equals("prepare")&&!youngRegistered){var d=NaturalPlacementGameTest.decision(level,youngSite);require(d!=null&&!Set.of("REJECTED","INDETERMINATE").contains(d.get("status").getAsString()),"Persistence Young generation refused: "+d);if(!d.get("status").getAsString().equals("PLACED"))return;youngOwner=NaturalPlacementGameTest.uuid(level,youngSite);owners.add(youngOwner);expected.add(youngOwner);ColonyMembers.get(level).members(youngOwner).forEach(m->expected.add(m.worker()));supply((LasiusNigerEntity)level.getEntity(youngOwner));youngRegistered=true;manifest();}
        if(!initialized){
            if(!reopen()&&!phase.equals("prepare")){
                if(!phase.equals("empty")){
                    for(var cp:sites){var d=NaturalPlacementGameTest.decision(level,cp);require(d!=null&&!Set.of("REJECTED","INDETERMINATE").contains(d.get("status").getAsString()),"Generated base refused: "+d);if(!d.get("status").getAsString().equals("PLACED"))return;}
                    for(var cp:sites)cohort((LasiusNigerEntity)level.getEntity(NaturalPlacementGameTest.uuid(level,cp)));
                }
                manifest();
            }
            if(!ants().stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet()).equals(expected)){require(ticks<200,"Initial exact population/loading prerequisite failed: actual="+ants().stream().map(Entity::getUUID).toList()+" expected="+expected);return;}
            initialized=true;write(phase+"-runtime",runtime(s));write(phase+"-first-live",snapshot(s));
        }
        var ants=ants();var ids=new TreeSet<UUID>();int moved=0,search=0,hauling=0,nursing=0;long consumed=0;var age=new long[ants.size()];int n=0;boolean allTicking=true;
        for(var a:ants){ids.add(a.getUUID());require(!a.isNoAi()&&a.isAlive(),"Normal living AI");Long old=previousAge.put(a.getUUID(),a.elapsedAgeTicks());boolean advanced=old!=null&&a.elapsedAgeTicks()==old+1&&level.isPositionEntityTicking(a.blockPosition());allTicking&=advanced;age[n++]=a.elapsedAgeTicks();var before=previousPosition.put(a.getUUID(),a.position());if(before!=null&&before.distanceToSqr(a.position())>0.0001)moved++;if(a.workerTasks().phase()==WorkerTasks.Phase.SEARCH)search++;if(!a.getMainHandItem().isEmpty())hauling++;if(a.workerTasks().nursing())nursing++;consumed+=a.nutrition().consumedUnits();if(a.form()==AntForm.WORKER)require(ColonyMembers.get(level).belongs(a,a.queenId(),a.nurseryHome()),"Canonical membership throughout");}
        require(ids.equals(expected),"Exact loaded population/identity changed; sample invalid");if(!allTicking){require(loaded==0,"Actor failed normal age/loading during contiguous sample");return;}loaded++;
        if(loaded==1)write(phase+"-loaded",snapshot(s));
        if(reopen()){if(loaded>=200)finish(s);return;}
        if(phase.equals("prepare")){prepare(s);return;}
        if(phase.equals("probe")){if(loaded>=20)finish(s);return;}
        if(loaded<=600)return;
        long entities=0;for(var a:level.getAllEntities())entities++;int refused=0;for(var id:owners){var q=(LasiusNigerEntity)level.getEntity(id);var pile=(BroodPile)level.getBlockEntity(q.founding().plan().nursery());if(pile.condition().contains("capacity"))refused++;}
        long nativeNs=s.getTickTimesNanos()[s.getTickCount()%100];long end=System.nanoTime();rows.add(new long[]{s.getTickCount(),level.getGameTime(),nativeNs,end,end-observer,ants.size(),level.getChunkSource().getLoadedChunksCount(),entities,moved,search,hauling,nursing,consumed,refused});ages.add(age);
        if(rows.size()==2400){var j=runtime(s);j.add("columns",JSON.toJsonTree(List.of("tick","gameTime","nativeNs","endNs","observerNs","adults","loadedChunks","entities","moving","searching","carrying","nursing","consumed","capRefusingColonies")));j.add("rows",JSON.toJsonTree(rows));j.add("ages",JSON.toJsonTree(ages));j.add("ageIdentityOrder",JSON.toJsonTree(expected));write(phase+"-samples",j);finish(s);}
    }
    private void prepare(MinecraftServer s){
        require(loaded<=6000,"Bounded persistence nonempty preparation failed");var q=(LasiusNigerEntity)level.getEntity(youngOwner);var pile=(BroodPile)level.getBlockEntity(q.founding().plan().nursery());
        boolean brood=pile.records().stream().anyMatch(r->r.progress()>=200);var carriers=new JsonArray();
        for(var a:ants())if(a.form()==AntForm.WORKER&&!a.getMainHandItem().isEmpty()){
            var owner=(LasiusNigerEntity)level.getEntity(a.queenId());boolean claim=owner!=null&&(owner.founding().claimedBy(a)||a.workerTasks().nursingAuthorized(level)||DigJob.claimedBy(level,a)!=null||SurfaceWork.get(level).claimedBy(a)!=null);
            if(claim){var j=new JsonObject();j.addProperty("worker",a.getUUID().toString());j.addProperty("phase",a.workerTasks().phase().name());j.addProperty("cargo",a.getMainHandItem().toString());j.addProperty("food",WorkerTasks.food(a.getMainHandItem()));carriers.add(j);}
        }
        boolean food=carriers.asList().stream().anyMatch(x->x.getAsJsonObject().get("food").getAsBoolean());
        if(loaded>=200&&brood&&food){var j=process(s);j.addProperty("naturallyLaidBrood",pile.records().size());j.addProperty("youngQueen",youngOwner.toString());j.add("claimedCarriers",carriers);j.addProperty("preparationLoadedTicks",loaded);j.addProperty("materialCargoExercised",carriers.asList().stream().anyMatch(x->!x.getAsJsonObject().get("food").getAsBoolean()));write("prepare-nonempty",j);finish(s);}
    }
}
