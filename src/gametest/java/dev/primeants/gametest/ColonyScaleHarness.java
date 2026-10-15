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
    private long allocationStart;
    private long[] profileStart;
    private UUID controlBrood;
    private long controlProgress=-1,controlObserved;
    private boolean controlLateSupply;
    private ChunkPos youngSite;private UUID youngOwner;private boolean youngRegistered;
    private static final List<String> TABLES=List.of("founding.NaturalPlacement","founding.EstablishedNest","founding.NaturalSoil","founding.GenerationAir","founding.ColonyTerrain","founding.ColonyPlugs","founding.ChamberExcavation","worker.ColonyMembers","colony.ChamberRegistry","worker.AdultHistory","brood.BroodHistory","worker.TransferCustody","founding.NestExpansion","founding.ChamberUpgrade","founding.SurfaceWork","founding.Mining","worker.ColonyAlarm");
    @Override public void onInitialize(){
        if(phase.isEmpty())return;active=this;
        try{declaration=JsonParser.parseString(Files.readString(Path.of(System.getProperty("prime_ants.scaleDeclaration")))).getAsJsonObject();}catch(Exception ex){throw new IllegalStateException("Frozen declaration required",ex);}
        ticket=Registry.register(BuiltInRegistries.TICKET_TYPE,Identifier.fromNamespaceAndPath("prime_ants_test","t10_scale"),new TicketType(0,14));
        ServerEntityEvents.ENTITY_LOAD.register((a,l)->{if(reopen()&&a instanceof LasiusNigerEntity)firstActors.add(a.getUUID().toString(),actor(a,l));});
        ServerLifecycleEvents.SERVER_STARTED.register(s->{try{start(s);}catch(Throwable ex){fail(s,ex);}});
        ServerTickEvents.END_SERVER_TICK.register(s->{if(done)return;try{observe(s);}catch(Throwable ex){fail(s,ex);}});
        ServerTickEvents.START_SERVER_TICK.register(s->{if(phase.equals("profile"))allocationStart=ScaleValidationProbe.allocated();});
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
        }else if(phase.equals("profile")||phase.equals("control")){
            sites.add(select(6300,ColonyStage.YOUNG));hold(sites.getFirst());
        }else{
            int colonies=phase.equals("empty")&&declaration.has("regions")?declaration.get("regions").getAsInt():phase.equals("one")||phase.equals("probe")?1:3;
            for(int i=0;i<colonies;i++){
                if(phase.equals("empty")&&declaration.has("sites")){var a=declaration.getAsJsonArray("sites").get(i).getAsJsonArray();var cp=new ChunkPos(a.get(0).getAsInt(),a.get(1).getAsInt());sites.add(cp);hold(cp);continue;}
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
        if(declaration.has("controlledGreat")&&declaration.get("controlledGreat").getAsBoolean())controlledGreat(q);
    }
    /** Explicit controlled initial physical habitat. No stage/cap setter, job payment, birth or growth receipt.
     * These supplied walls and stock are initial benchmark conditions, never evidence of worker construction. */
    private void controlledGreat(LasiusNigerEntity q){
        var p=q.founding().plan();var owner=q.getUUID();var terrain=ColonyTerrain.get(level);var walls=new LinkedHashSet<BlockPos>();
        for(var chamber:ChamberRegistry.get(level).colony(owner).chambers())walls.addAll(ChamberUpgrade.walls(p,chamber));
        for(var cell:walls){require(NestPlan.loaded(level,cell)&&level.getBlockState(cell).isSolidRender(),"Controlled initial wall must already be loaded solid shell");require(level.setBlock(cell,NurseryBlocks.PACKED_CLAY.defaultBlockState(),3),"Controlled initial physical wall");terrain.built(cell,owner,NurseryBlocks.PACKED_CLAY);}
        var store=MaterialStore.owned(level,owner,p);require(store!=null,"Controlled Great owned initial store");var tag=store.saveWithFullMetadata(level.registryAccess());var contents=new ArrayList<>(store.contents());for(int i=0;i<32;i++)contents.add(new ItemStack(Items.COBBLESTONE));
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());out.store("Contents",ItemStack.CODEC.listOf(),contents);tag.put("Contents",out.buildResult().get("Contents"));
        var replaced=(MaterialStore)BlockEntity.loadStatic(store.getBlockPos(),store.getBlockState(),tag,level.registryAccess());require(replaced!=null,"Controlled initial physical inventory");level.setBlockEntity(replaced);replaced.setChanged();replaced.refreshDisplay();
        var pile=(BroodPile)level.getBlockEntity(p.nursery());var e=ColonyDevelopment.evaluate(level,owner,p,true,q.colonyAdultCapacity(),pile.loadedTicks());require(e.stage()==ColonyStage.GREAT&&e.cap()==120&&e.inputs().adults().known()==120,"Great must derive from actual physical chambers, stocks and population");
        var j=new JsonObject();j.addProperty("initialCondition","controlled_supplied_physical_Great_not_natural_growth_or_observed_construction");j.addProperty("owner",owner.toString());j.addProperty("walls",walls.size());j.addProperty("suppliedCobblestone",32);j.addProperty("actualAdults",120);j.addProperty("derivedStage",e.stage().serializedName());j.addProperty("derivedCap",e.cap());j.addProperty("stageAssigned",false);write("controlled-great-"+owner,j);
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
                    for(var cp:sites){var q=(LasiusNigerEntity)level.getEntity(NaturalPlacementGameTest.uuid(level,cp));if(phase.equals("profile")||phase.equals("control")){owners.add(q.getUUID());expected.add(q.getUUID());ColonyMembers.get(level).members(q.getUUID()).forEach(m->expected.add(m.worker()));if(phase.equals("profile"))supply(q);}else cohort(q);}
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
        if(phase.equals("control")){control(s);return;}
        if(phase.equals("probe")){if(loaded>=20)finish(s);return;}
        if(phase.equals("profile")){
            if(loaded==50)profileStart=ScaleValidationProbe.snapshot();
            if(loaded>50){var c=ScaleValidationProbe.snapshot();var row=new long[c.length+3];row[0]=s.getTickCount();row[1]=s.getTickTimesNanos()[s.getTickCount()%100];row[2]=ScaleValidationProbe.allocated()-allocationStart;for(int i=0;i<c.length;i++)row[i+3]=c[i]-profileStart[i];profileStart=c;rows.add(row);}
            if(rows.size()==200){var j=runtime(s);j.add("counts",JSON.toJsonTree(ScaleValidationProbe.NAMES));j.add("rows",JSON.toJsonTree(rows));j.addProperty("columnsPrefix","serverTick,nativeNs,serverTickAllocatedBytes");j.addProperty("warmupLoadedTicks",50);j.addProperty("measuredLoadedTicks",200);write("profile-repetition",j);finish(s);}return;
        }
        if(loaded<=600)return;
        long entities=0;for(var a:level.getAllEntities())entities++;int refused=0;for(var id:owners){var q=(LasiusNigerEntity)level.getEntity(id);var pile=(BroodPile)level.getBlockEntity(q.founding().plan().nursery());if(pile.condition().contains("capacity"))refused++;if(declaration.has("controlledGreat")&&declaration.get("controlledGreat").getAsBoolean())require(pile.stageEvaluation()!=null&&pile.stageEvaluation().stage()==ColonyStage.GREAT&&pile.stageEvaluation().cap()==120,"Every measured row requires the real live Great cap120");}
        long nativeNs=s.getTickTimesNanos()[s.getTickCount()%100];long end=System.nanoTime();rows.add(new long[]{s.getTickCount(),level.getGameTime(),nativeNs,end,end-observer,ants.size(),level.getChunkSource().getLoadedChunksCount(),entities,moved,search,hauling,nursing,consumed,refused});ages.add(age);
        if(rows.size()==2400){var j=runtime(s);j.add("columns",JSON.toJsonTree(List.of("tick","gameTime","nativeNs","endNs","observerNs","adults","loadedChunks","entities","moving","searching","carrying","nursing","consumed","capRefusingColonies")));j.add("rows",JSON.toJsonTree(rows));j.add("ages",JSON.toJsonTree(ages));j.add("ageIdentityOrder",JSON.toJsonTree(expected));write(phase+"-samples",j);finish(s);}
    }
    private void control(MinecraftServer s){
        var q=(LasiusNigerEntity)level.getEntity(owners.getFirst());var pile=(BroodPile)level.getBlockEntity(q.founding().plan().nursery());
        require(declaration.has("controlBound"),"Prospective small-control bound required");
        if(loaded==5800){require(!controlLateSupply,"One finite scheduled physical food wave only");drop(q.founding().plan().outside(),Items.APPLE,8,"T11 finite control apples");drop(q.founding().plan().outside(),Items.CHICKEN,4,"T11 finite control chickens");controlLateSupply=true;}
        for(var b:pile.records())if(!b.founding()){
            if(controlBrood==null){controlBrood=b.id();controlProgress=b.progress();controlObserved=0;}
            if(b.id().equals(controlBrood)){if(b.progress()==controlProgress+1)controlObserved++;else controlObserved=0;controlProgress=b.progress();}
        }
        if(loaded%200==0||controlObserved>=200){var j=process(s);j.addProperty("loaded",loaded);j.addProperty("nurseryLoaded",pile.loadedTicks());j.addProperty("condition",pile.condition());j.add("supply",JSON.toJsonTree(pile.supply(level)));j.addProperty("observedBrood",controlBrood==null?null:controlBrood.toString());j.addProperty("successiveProgressTicks",controlObserved);j.addProperty("progress",controlProgress);var clocks=new JsonArray();for(var a:ants()){var v=new JsonObject();v.addProperty("id",a.getUUID().toString());v.addProperty("fasting",a.adultLife().fasting());v.addProperty("age",a.elapsedAgeTicks());v.addProperty("callow",a.callowAgeTicks());v.addProperty("receipts",a.nutrition().consumedUnits());v.addProperty("sugar",a.nutrition().sugar());v.addProperty("protein",a.nutrition().protein());clocks.add(v);}j.add("actors",clocks);write("control-observation-"+loaded,j);}
        if(controlObserved>=200){require(pile.supply(level).foodSugar()>0&&q.nutrition().consumedUnits()>0,"Real ingestion income and queen physical meal");var j=process(s);j.addProperty("naturallyLaidBrood",controlBrood.toString());j.addProperty("successiveLoadedProgressTicks",controlObserved);j.addProperty("loaded",loaded);j.addProperty("latePhysicalSupplies",controlLateSupply);write("control-accepted",j);finish(s);return;}
        require(loaded<declaration.get("controlBound").getAsInt(),"Small default-speed preparation prerequisite not demonstrated within its declared bound");
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
