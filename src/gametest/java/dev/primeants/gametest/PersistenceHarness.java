package dev.primeants.gametest;

import com.google.gson.*;
import com.mojang.serialization.*;
import dev.primeants.PrimeAnts;
import dev.primeants.brood.*;
import dev.primeants.colony.ColonyStage;
import dev.primeants.entity.*;
import dev.primeants.founding.*;
import dev.primeants.worker.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Explicit T19 functional world. No setup actors, canonical-state setters or reopen supplies. */
public final class PersistenceHarness {
    private static PersistenceHarness active;
    private static final ThreadLocal<Boolean> PASSIVE_CAPTURE=ThreadLocal.withInitial(()->false);
    public static boolean passiveCapture(){return PASSIVE_CAPTURE.get();}
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private final String phase;
    private final Path evidence;
    private final JsonObject declaration;
    private final List<UUID> owners=new ArrayList<>();
    private final List<ChunkPos> sites=new ArrayList<>();
    private final Map<UUID,Long> ages=new HashMap<>(),broodProgress=new HashMap<>(),broodRuns=new HashMap<>();
    private final Map<UUID,CompoundTag> groundBefore=new HashMap<>();
    private final Set<Integer> waves=new HashSet<>();
    private final JsonObject firstActors=new JsonObject(),firstBlocks=new JsonObject(),firstTables=new JsonObject(),firstTableRoots=new JsonObject(),firstBlockInputs=new JsonObject();
    private final JsonArray steps=new JsonArray();
    private JsonObject disk;
    private MinecraftServer server;
    private ServerLevel level;
    private TicketType ticket;
    private boolean done,initialized,compared,foodServed,materialServed;
    private int ticks,loaded,settled,events;
    private long servicesCompletedAt=-1;
    private long suppliedApples=8,suppliedChickens=4,suppliedClay=16,expiredApples,expiredChickens,expiredClay;
    private final List<String> tableClasses;
    private JsonObject lastGate;

    public PersistenceHarness(String phase,Path evidence,JsonObject declaration){
        this.phase=phase;this.evidence=evidence;this.declaration=declaration;active=this;
        require(declaration.get("snapshotFormat").getAsInt()==2,"Explicit native snapshot format2 required");
        require(!probe()||phase.equals("reopen1"),"Decode probe is a labelled diagnostic reopen only");
        tableClasses=declaration.getAsJsonArray("tableClasses").asList().stream().map(JsonElement::getAsString).toList();
        if(reopen())disk=read(phase+"-disk");
    }
    private boolean reopen(){return !phase.equals("prepare");}
    private boolean probe(){return declaration.has("decodeProbe")&&declaration.get("decodeProbe").getAsBoolean();}
    public void register(){
        ticket=Registry.register(BuiltInRegistries.TICKET_TYPE,Identifier.fromNamespaceAndPath("prime_ants_test","t19_persistence"),new TicketType(0,14));
        ServerLifecycleEvents.SERVER_STARTING.register(s->server=s);
        ServerLifecycleEvents.SERVER_STARTED.register(s->{server=s;if(done){s.halt(false);return;}try{start();}catch(Throwable e){fail(e);}});
        ServerTickEvents.END_SERVER_TICK.register(s->{if(!done)try{step();}catch(Throwable e){fail(e);}});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->write(phase+"-stopped",process()));
    }
    private static void require(boolean b,String why){if(!b)throw new IllegalStateException(why);}
    private static JsonElement tag(Tag t){return exactNumbers(NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE,t));}
    /** Write a binary32 value as its exact binary64 widening, avoiding decimal display rounding. */
    private static JsonElement exactNumbers(JsonElement value){
        if(value.isJsonObject()){var j=new JsonObject();for(var e:value.getAsJsonObject().entrySet())j.add(e.getKey(),exactNumbers(e.getValue()));return j;}
        if(value.isJsonArray()){var j=new JsonArray();for(var e:value.getAsJsonArray())j.add(exactNumbers(e));return j;}
        if(value.isJsonPrimitive()&&value.getAsJsonPrimitive().isNumber()&&value.getAsNumber() instanceof Float f)return new JsonPrimitive((double)f);
        return value;
    }
    private static CompoundTag saved(Entity a,ServerLevel l){boolean old=PASSIVE_CAPTURE.get();PASSIVE_CAPTURE.set(true);try{var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());require(a.save(out),"T19 entity snapshot");return out.buildResult();}finally{PASSIVE_CAPTURE.set(old);}}
    private JsonObject read(String name){try{return JsonParser.parseString(Files.readString(evidence.resolve(name+".json"))).getAsJsonObject();}catch(Exception e){throw new IllegalStateException(name,e);}}
    private void write(String name,JsonElement v){try{Files.createDirectories(evidence);Files.writeString(evidence.resolve(name+".json"),JSON.toJson(v),StandardCharsets.UTF_8);}catch(Exception e){throw new IllegalStateException(e);}}
    private JsonObject process(){var j=new JsonObject();j.addProperty("phase",phase);j.addProperty("pid",ProcessHandle.current().pid());j.addProperty("processStart",ProcessHandle.current().info().startInstant().orElseThrow().toString());j.addProperty("serverTick",server==null?-1:server.getTickCount());j.addProperty("gameTime",level==null?-1:level.getGameTime());j.addProperty("loaded",loaded);j.addProperty("world",server==null?"":server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().toString());return j;}
    private void fail(Throwable e){if(done)return;done=true;var j=process();j.addProperty("failure",e.toString());if(lastGate!=null)j.add("lastGate",lastGate);write(phase+"-failure",j);write(phase+"-development-steps",steps);if(initialized&&!Files.exists(evidence.resolve(phase+"-first-divergence.json")))try{write(phase+"-failure-state",snapshot());}catch(Throwable capture){j.addProperty("failureSnapshotError",capture.toString());write(phase+"-failure",j);}PrimeAnts.LOGGER.error("T19 persistence failure",e);if(server!=null)server.halt(false);}
    private boolean dimension(ServerLevel l){return l.dimension().identifier().toString().equals(declaration.get("dimension").getAsString());}
    private void compare(String group,String key,JsonElement value){
        if(disk==null||compared||done)return;
        var expected=disk.getAsJsonObject(group).get(key);
        if(expected==null||!expected.equals(value)){
            var j=process();j.addProperty("group",group);j.addProperty("key",key);j.add("actualDisk",expected);j.add("firstDecoded",value);write(phase+"-first-divergence",j);
            fail(new IllegalStateException("Exact first-decode mismatch "+group+"/"+key));
        }
    }
    public static void actorDecoded(Entity a){
        var h=active;if(h==null||!h.reopen()||h.done||h.compared||!(a.level() instanceof ServerLevel l)||!h.dimension(l)||!(a instanceof LasiusNigerEntity||a instanceof ItemEntity))return;
        String key=a.getUUID().toString();if(h.firstActors.has(key))return;
        var value=tag(saved(a,l));h.firstActors.add(key,value);h.write(h.phase+"-first-actors",h.firstActors);h.compare("actors",key,value);
    }
    public static void blockDecoded(BlockEntity b,BlockState state,CompoundTag input,HolderLookup.Provider registries){
        var h=active;if(h==null||!h.reopen()||h.done||h.compared||!(b instanceof BroodPile||b instanceof NestCache||b instanceof MaterialStore))return;
        String key=b.getBlockPos().toShortString();if(!h.disk.getAsJsonObject("blocks").has(key)||h.firstBlocks.has(key))return;
        var raw=tag(input);h.firstBlockInputs.add(key,raw);h.write(h.phase+"-first-block-inputs",h.firstBlockInputs);h.compare("blocks",key,raw);if(h.done)return;
        // Native LevelChunk saving marks an instantiated BE keepPacked=false.
        // Compare that envelope AND every decoded canonical field; retain raw input too.
        var decoded=b.saveWithFullMetadata(registries);decoded.putBoolean("keepPacked",false);
        var value=tag(decoded);h.firstBlocks.add(key,value);h.write(h.phase+"-first-blocks",h.firstBlocks);h.compare("blocks",key,value);
        h.compare("terrain",key,tag(NbtUtils.writeBlockState(state)));
    }
    public static void tableInput(Path path,CompoundTag input){
        var h=active;if(h==null||!h.reopen()||h.done||h.compared||!path.toString().replace('\\','/').contains("dimensions/prime_ants_test/placement_soil/data/prime_ants/"))return;
        String key="prime_ants:"+path.getFileName().toString().replaceFirst("\\.dat$","");if(h.firstTableRoots.has(key))return;
        var value=tag(input);h.firstTableRoots.add(key,value);h.write(h.phase+"-first-table-inputs",h.firstTableRoots);h.compare("tableRoots",key,value);
    }
    @SuppressWarnings({"rawtypes","unchecked"}) public static void tableDecoded(Path folder,SavedDataType type,SavedData data,HolderLookup.Provider registries){
        var h=active;if(h==null||!h.reopen()||h.done||h.compared||data==null||!folder.toString().replace('\\','/').endsWith("dimensions/prime_ants_test/placement_soil/data")||!type.id().getNamespace().equals("prime_ants"))return;
        String key=type.id().toString();if(h.firstTables.has(key))return;
        var ops=registries.createSerializationContext(NbtOps.INSTANCE);var value=tag((Tag)type.codec().encodeStart(ops,data).getOrThrow());
        h.firstTables.add(key,value);h.write(h.phase+"-first-tables",h.firstTables);h.compare("tables",key,value);
    }
    @SuppressWarnings({"unchecked","rawtypes"}) private JsonObject tables(){
        var j=new JsonObject();for(var name:tableClasses)try{
            var c=Class.forName("dev.primeants."+name);SavedDataType type=(SavedDataType)c.getField("TYPE").get(null);
            // get decodes an existing file, and never creates a missing canonical table.
            SavedData data=level.getDataStorage().get(type);if(data!=null)j.add(type.id().toString(),tag((Tag)type.codec().encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE),data).getOrThrow()));
        }catch(Exception e){throw new IllegalStateException("T19 table "+name,e);}return j;
    }
    private List<LasiusNigerEntity> ants(){var a=new ArrayList<LasiusNigerEntity>();for(var e:level.getAllEntities())if(e instanceof LasiusNigerEntity ant&&ant.isAlive())a.add(ant);a.sort(Comparator.comparing(Entity::getUUID));return a;}
    private LasiusNigerEntity queen(UUID id){return (LasiusNigerEntity)level.getEntity(id);}
    private BroodPile pile(UUID id){return (BroodPile)level.getBlockEntity(queen(id).founding().plan().nursery());}
    private ChunkPos select(int base,ColonyStage stage){for(int cell=0;cell<16;cell++)for(int x=base+4*cell;x<base+4*cell+4;x++)for(int z=base;z<base+4;z++){var p=new ChunkPos(x,z);if(NaturalPlacement.selected(level,p)&&NaturalPlacement.selectedProfile(level,p)==stage)return p;}throw new IllegalStateException("No declared "+stage+" candidate");}
    private void start(){
        level=server.getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.parse(declaration.get("dimension").getAsString())));require(level!=null,"Declared dimension");write(phase+"-process",process());
        var format=new JsonObject();var defaults=new JsonObject();for(var block:BuiltInRegistries.BLOCK)defaults.add(BuiltInRegistries.BLOCK.getKey(block).toString(),tag(NbtUtils.writeBlockState(block.defaultBlockState())));format.add("blockDefaults",defaults);format.addProperty("minY",level.getMinY());format.addProperty("maxY",level.getMaxY());format.addProperty("dataVersion",net.minecraft.SharedConstants.getCurrentVersion().dataVersion().version());write(phase+"-native-format",format);
        if(reopen()){
            var cohort=read("cohort");cohort.getAsJsonArray("owners").forEach(x->owners.add(UUID.fromString(x.getAsString())));
            for(var x:cohort.getAsJsonArray("sites")){var a=x.getAsJsonArray();sites.add(new ChunkPos(a.get(0).getAsInt(),a.get(1).getAsInt()));}
            var account=read("prepare-accounting");suppliedApples=account.get("suppliedApples").getAsLong();suppliedChickens=account.get("suppliedChickens").getAsLong();suppliedClay=account.get("suppliedClay").getAsLong();
            String prior=phase.equals("reopen1")?"prepare":"reopen1";var previous=read(prior+"-accounting");expiredApples=previous.get("expiredApples").getAsLong();expiredChickens=previous.get("expiredChickens").getAsLong();expiredClay=previous.get("expiredClay").getAsLong();
            tables();
        }else for(var site:declaration.getAsJsonArray("sites")){var j=site.getAsJsonObject();sites.add(select(j.get("base").getAsInt(),ColonyStage.byName(j.get("profile").getAsString())));}
        for(var p:sites){level.getChunkSource().addTicketWithRadius(ticket,p,4);level.getChunk(p.x(),p.z());}
        if(reopen()){
            // Terrain is read once after the ordinary chunk decode, before the first server tick.
            var terrain=terrain(disk.getAsJsonObject("terrain").keySet());write(phase+"-first-terrain",terrain);
            for(var e:terrain.entrySet()){compare("terrain",e.getKey(),e.getValue());if(done)return;}
        }
    }
    private static BlockPos position(String s){var a=s.split(", ");return new BlockPos(Integer.parseInt(a[0]),Integer.parseInt(a[1]),Integer.parseInt(a[2]));}
    private JsonObject terrain(Set<String> keys){var j=new JsonObject();for(var key:keys){var p=position(key);require(NestPlan.loaded(level,p),"Declared terrain loaded "+key);j.add(key,tag(NbtUtils.writeBlockState(level.getBlockState(p))));}return j;}
    private JsonObject snapshot(){
        var j=process();var actors=new JsonObject();for(var e:level.getAllEntities())if(e instanceof LasiusNigerEntity||e instanceof ItemEntity)actors.add(e.getUUID().toString(),tag(saved(e,level)));j.add("actors",actors);j.add("tables",tables());
        var blocks=new JsonObject();var positions=new TreeSet<String>();
        var virtual=new TreeSet<String>();for(var id:owners){var p=queen(id).founding().plan();for(int f=-16;f<=16;f++)for(int side=-16;side<=16;side++)for(int y=-8;y<=12;y++){var at=p.at(f,side,y);if(level.isInsideBuildHeight(at))positions.add(at.toShortString());else virtual.add(at.toShortString());}}
        for(var key:positions){var p=position(key);var b=level.getBlockEntity(p);if(b!=null)blocks.add(key,tag(level.getChunkAt(p).getBlockEntityNbtForSaving(p,level.registryAccess())));}
        var roots=new JsonObject();for(var e:j.getAsJsonObject("tables").entrySet()){var root=new JsonObject();root.add("data",e.getValue().deepCopy());root.addProperty("DataVersion",net.minecraft.SharedConstants.getCurrentVersion().dataVersion().version());roots.add(e.getKey(),root);}j.add("tableRoots",roots);
        j.add("blocks",blocks);j.add("terrain",terrain(positions));j.add("virtualTerrainNotPersisted",terrain(virtual));return j;
    }
    private void deliver(int at){for(var id:owners){var p=queen(id).founding().plan();for(var entry:List.of(Map.entry(Items.APPLE,8),Map.entry(Items.CHICKEN,4),Map.entry(Items.CLAY_BALL,8))){var stack=new ItemStack(entry.getKey(),entry.getValue());stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("T19 finite "+at+" "+id));var v=Vec3.atBottomCenterOf(p.outside());var item=new ItemEntity(level,v.x,v.y+.1,v.z,stack,0,0,0);item.setDefaultPickUpDelay();require(level.addFreshEntity(item),"Finite source insertion");}}suppliedApples+=16;suppliedChickens+=8;suppliedClay+=16;var j=process();j.addProperty("wave",at);write("prepare-wave-"+at,j);}
    private void initialize(){
        if(!reopen()){
            for(var cp:sites){var d=NaturalPlacementGameTest.decision(level,cp);require(d!=null&&!Set.of("REJECTED","INDETERMINATE").contains(d.get("status").getAsString()),"Generated colony refused "+d);if(!d.get("status").getAsString().equals("PLACED"))return;}
            for(var cp:sites)owners.add(NaturalPlacementGameTest.uuid(level,cp));
        }
        for(var id:owners)if(queen(id)==null)return;
        if(reopen()){
            if(!firstActors.keySet().equals(disk.getAsJsonObject("actors").keySet())||!firstBlocks.keySet().equals(disk.getAsJsonObject("blocks").keySet())){require(ticks<200,"All first-decode events available without overwriting initial observations");return;}
            require(firstActors.keySet().equals(disk.getAsJsonObject("actors").keySet()),"Complete first-decoded actor coverage");
            require(firstBlocks.keySet().equals(disk.getAsJsonObject("blocks").keySet()),"Complete first-decoded block coverage");
            require(firstTableRoots.keySet().equals(disk.getAsJsonObject("tableRoots").keySet()),"Complete first normal table input envelope coverage");
            require(firstTables.keySet().equals(disk.getAsJsonObject("tables").keySet()),"Complete first-decoded table coverage");compared=true;var j=process();j.addProperty("exact",true);j.addProperty("actors",firstActors.size());j.addProperty("blocks",firstBlocks.size());j.addProperty("tables",firstTables.size());j.add("diskClocks",disk.get("clocks"));write(phase+"-decoded-equality",j);
        }
        initialized=true;var cohort=new JsonObject();cohort.add("owners",JSON.toJsonTree(owners));cohort.add("sites",JSON.toJsonTree(sites.stream().map(p->List.of(p.x(),p.z())).toList()));if(!reopen())write("cohort",cohort);
        var runtime=process();runtime.addProperty("work",QueenFounding.multiplier());runtime.addProperty("brood",BroodPile.multiplier());runtime.addProperty("tps",server.tickRateManager().tickrate());runtime.addProperty("heap",Runtime.getRuntime().maxMemory());write(phase+"-runtime",runtime);write(phase+"-first-live",snapshot());
    }
    private JsonObject accounting(){
        long a=0,c=0,m=0,ra=0,rc=0,built=0;var ground=new HashMap<UUID,CompoundTag>();
        for(var e:level.getAllEntities())if(e instanceof ItemEntity item){var stack=item.getItem();if(stack.is(Items.APPLE))a+=stack.getCount();if(stack.is(Items.CHICKEN))c+=stack.getCount();if(stack.is(Items.CLAY_BALL))m+=stack.getCount();ground.put(item.getUUID(),saved(item,level));}
        for(var e:groundBefore.entrySet())if(!ground.containsKey(e.getKey())&&e.getValue().getIntOr("Age",0)>=5999){var stack=e.getValue().getCompoundOrEmpty("Item");String id=stack.getStringOr("id","");int n=stack.getIntOr("count",0);if(id.equals("minecraft:apple"))expiredApples+=n;if(id.equals("minecraft:chicken"))expiredChickens+=n;if(id.equals("minecraft:clay_ball"))expiredClay+=n;write(phase+"-expiry-"+loaded+"-"+e.getKey(),tag(e.getValue()));}
        groundBefore.clear();groundBefore.putAll(ground);
        var stacks=new ArrayList<ItemStack>();
        for(var ant:ants()){stacks.add(ant.getMainHandItem());require(ant.nutrition().snapshot().valid(),"Adult nutrition "+ant.getUUID());ra+=ant.nutrition().apples();rc+=ant.nutrition().chickens();}
        for(var id:owners){var q=queen(id);var p=q.founding().plan();var pile=pile(id);ra+=pile.consumedApples();rc+=pile.consumedChickens();if(level.getBlockEntity(p.cache()) instanceof NestCache cache)stacks.addAll(cache.contents());var store=MaterialStore.owned(level,id,p);if(store!=null)stacks.addAll(store.contents());for(var job:ChamberUpgrade.get(level).jobs(id))if(job.tier==2){require(job.ledger().taken()==job.ledger().carried()+job.ledger().built()+job.ledger().released(),"Material job ledger");built+=job.built().size();}}
        for(var pending:TransferCustody.get(level).contents())stacks.add(pending.stack());
        var history=AdultHistory.CODEC.encodeStart(JsonOps.INSTANCE,AdultHistory.get(level)).getOrThrow().getAsJsonObject();for(var e:history.entrySet()){var row=JsonParser.parseString(e.getValue().getAsString()).getAsJsonObject();if(owners.stream().anyMatch(id->id.toString().equals(row.get("queen").getAsString()))){var n=row.getAsJsonObject("nutrition");ra+=n.get("apples").getAsLong();rc+=n.get("chickens").getAsLong();}}
        for(var stack:stacks){if(stack.is(Items.APPLE))a+=stack.getCount();if(stack.is(Items.CHICKEN))c+=stack.getCount();if(stack.is(Items.CLAY_BALL))m+=stack.getCount();}
        var j=process();j.addProperty("suppliedApples",suppliedApples);j.addProperty("suppliedChickens",suppliedChickens);j.addProperty("suppliedClay",suppliedClay);j.addProperty("physicalApples",a);j.addProperty("physicalChickens",c);j.addProperty("physicalClay",m);j.addProperty("appleReceipts",ra);j.addProperty("chickenReceipts",rc);j.addProperty("builtClay",built);j.addProperty("expiredApples",expiredApples);j.addProperty("expiredChickens",expiredChickens);j.addProperty("expiredClay",expiredClay);boolean balanced=a+ra+expiredApples==suppliedApples&&c+rc+expiredChickens==suppliedChickens&&m+built+expiredClay==suppliedClay;j.addProperty("balanced",balanced);if(!balanced){write(phase+"-first-accounting-divergence",j);require(false,"Physical food/material equation");}return j;
    }
    private JsonObject gate(){
        var j=process();var carriers=new JsonArray();boolean food=false,material=false,inventoryFood=false,inventoryMaterial=false,brood=false;
        for(var id:owners){var q=queen(id);var p=q.founding().plan();var b=pile(id);for(var r:b.records())if(!r.founding()){
            Long old=broodProgress.put(r.id(),r.progress());long run=old!=null&&r.progress()==old+1?broodRuns.getOrDefault(r.id(),0L)+1:0;broodRuns.put(r.id(),run);brood|=run>=200;
            var v=new JsonObject();v.addProperty("loaded",loaded);v.addProperty("gameTime",level.getGameTime());v.addProperty("brood",r.id().toString());v.addProperty("stage",r.stage().name());v.addProperty("progress",r.progress());v.addProperty("run",run);steps.add(v);
        }if(level.getBlockEntity(p.cache()) instanceof NestCache cache)inventoryFood|=!cache.contents().isEmpty();var store=MaterialStore.confirmed(level,id,p);inventoryMaterial|=store!=null&&!store.contents().isEmpty();
        j.addProperty(id+"-catchupApplied",b.catchup().appliedTicks());j.addProperty(id+"-catchupPending",b.catchup().pendingTicks());j.addProperty(id+"-lastSimulation",b.lastSimulationGameTime().orElse(-1));}
        for(var ant:ants())if(ant.form()==AntForm.WORKER&&!ant.getMainHandItem().isEmpty()){
            var q=queen(ant.queenId());var cargo=ant.getMainHandItem();var recipient=ant.workerTasks().recipientId();boolean exactRecipient=recipient!=null&&(recipient.equals(q.getUUID())?WorkerTasks.queenNeedsFood(q,cargo):pile(q.getUUID()).records().stream().anyMatch(r->r.id().equals(recipient)&&pile(q.getUUID()).accepts(r,cargo)));boolean validFood=WorkerTasks.food(cargo)&&(q.founding().claimedBy(ant)&&ant.workerTasks().authorized(level)||ant.workerTasks().nursingAuthorized(level)&&exactRecipient);
            var job=ChamberUpgrade.get(level).claimedBy(ant);var store=MaterialStore.confirmed(level,ant.queenId(),q.founding().plan());boolean validMaterial=MaterialStore.material(cargo)&&((job!=null&&ant.workerTasks().upgradeAuthorized(level)&&!job.complete()&&!job.stopped()&&job.accepts(cargo)&&job.carried()==cargo.getCount()&&job.convertible(level,job.next(),ant.queenId()))||(q.founding().claimedBy(ant)&&ant.workerTasks().authorized(level)&&store!=null&&store.room(cargo)));
            if(validFood||validMaterial){var v=new JsonObject();v.addProperty("actor",ant.getUUID().toString());v.addProperty("queen",ant.queenId().toString());v.addProperty("phase",ant.workerTasks().phase().name());v.addProperty("recipient",String.valueOf(ant.workerTasks().recipientId()));v.addProperty("food",validFood);v.addProperty("material",validMaterial);v.add("cargo",ItemStack.CODEC.encodeStart(level.registryAccess().createSerializationContext(JsonOps.INSTANCE),cargo).getOrThrow());if(job!=null){v.addProperty("destination",job.next()==null?"":job.next().toShortString());v.add("paidPrefix",JSON.toJsonTree(job.built()));v.add("ledger",JSON.toJsonTree(job.ledger()));}else if(store!=null)v.addProperty("destination",store.getBlockPos().toShortString());carriers.add(v);}food|=validFood;material|=validMaterial;
        }
        j.add("carriers",carriers);j.addProperty("foodCargoTask",food);j.addProperty("materialCargoExercised",material);j.addProperty("foodInventory",inventoryFood);j.addProperty("materialInventory",inventoryMaterial);j.addProperty("naturalProgressingBrood",brood);j.addProperty("foodService",foodServed);j.addProperty("materialService",materialServed);j.addProperty("settled",settled);j.addProperty("closure",food&&material&&inventoryFood&&inventoryMaterial&&brood);return j;
    }
    private void step(){
        ticks++;require(server.tickRateManager().tickrate()==20&&!server.tickRateManager().isSprinting()&&!server.tickRateManager().isFrozen(),"Ordinary 20TPS");require(QueenFounding.multiplier()==1&&BroodPile.multiplier()==1,"Default multipliers");
        if(!initialized){initialize();if(!initialized){require(ticks<200,"Initial declared actors loaded");return;}if(done)return;}
        var actual=new HashSet<UUID>();boolean fullyLoaded=true,newborn=false;for(var a:ants()){
            require(a.colonyIdentity()!=null&&owners.contains(a.colonyIdentity())&&!a.isNoAi(),"Declared ordinary canonical adult");actual.add(a.getUUID());Long old=ages.put(a.getUUID(),a.elapsedAgeTicks());boolean born=loaded>0&&old==null&&a.form()==AntForm.WORKER&&a.elapsedAgeTicks()<=1;newborn|=born;fullyLoaded&=(old!=null&&a.elapsedAgeTicks()==old+1||born)&&level.isPositionEntityTicking(a.blockPosition());
            if(a.form()==AntForm.WORKER)require(ColonyMembers.get(level).belongs(a,a.queenId(),a.nurseryHome()),"Canonical live member");}
        for(var id:owners){require(actual.contains(id),"Live queen loaded");for(var m:ColonyMembers.get(level).members(id))if(!m.dead())require(actual.contains(m.worker()),"All canonical members loaded");}
        if(!fullyLoaded){settled=0;require(loaded==0,"Ordinary contiguous actor age/loading");return;}
        if(newborn){var j=process();j.add("members",ColonyMembers.CODEC.encodeStart(JsonOps.INSTANCE,ColonyMembers.get(level)).getOrThrow());write(phase+"-birth-membership-"+ticks,j);}
        loaded++;
        if(!reopen())for(var v:declaration.getAsJsonArray("schedule")){int at=v.getAsInt();if(loaded==at){require(waves.add(at),"No repeated supply");deliver(at);}}
        var account=accounting();lastGate=gate();if(loaded==1||loaded%200==0){write(phase+"-gate-"+loaded,lastGate);write(phase+"-accounting-"+loaded,account);}
        if(probe()&&loaded>=20){write(phase+"-decode-probe-complete",lastGate);write(phase+"-before-close",snapshot());require(server.saveEverything(false,true,true),"Diagnostic normal save");var j=process();j.addProperty("decodeProbe",true);j.addProperty("acceptance",false);write(phase+"-save",j);done=true;server.halt(false);return;}
        if(reopen()&&foodServed&&materialServed&&level.getGameTime()>servicesCompletedAt&&lastGate.get("naturalProgressingBrood").getAsBoolean())settled++;else settled=0;
        int finalSupply=declaration.getAsJsonArray("schedule").asList().stream().mapToInt(JsonElement::getAsInt).max().orElse(0);
        boolean close=reopen()?settled>=200&&(phase.equals("reopen2")||lastGate.get("closure").getAsBoolean()):loaded>=finalSupply&&lastGate.get("closure").getAsBoolean();
        if(close){write(phase+"-qualified-boundary",lastGate);write(phase+"-accounting",account);write(phase+"-development-steps",steps);write(phase+"-before-close",snapshot());require(server.saveEverything(false,true,true),"Normal save/flush");var j=process();j.addProperty("completed",true);write(phase+"-save",j);done=true;server.halt(false);return;}
        require(loaded<(reopen()?12000:declaration.get("prepareBound").getAsInt()),"Declared "+phase+" bound: live cargo/tasks, brood and physical services not qualified");
    }
    public static JsonObject actionBefore(LasiusNigerEntity a,String kind){
        var h=active;if(h==null||h.done||!h.initialized||!(a.level() instanceof ServerLevel l)||!h.dimension(l)||a.getMainHandItem().isEmpty()&&!kind.endsWith("-after"))return null;
        var j=new JsonObject();j.addProperty("kind",kind);j.addProperty("actor",a.getUUID().toString());j.add("actorState",tag(saved(a,l)));j.add("cargo",a.getMainHandItem().isEmpty()?JsonNull.INSTANCE:ItemStack.CODEC.encodeStart(l.registryAccess().createSerializationContext(JsonOps.INSTANCE),a.getMainHandItem()).getOrThrow());
        long receipts=0;for(var ant:h.ants())receipts+=ant.nutrition().consumedUnits();for(var id:h.owners)receipts+=h.pile(id).consumedApples()+h.pile(id).consumedChickens()+h.pile(id).consumedNectar()+h.pile(id).consumedBerries()+h.pile(id).consumedPrey()+h.pile(id).consumedFlesh();j.addProperty("receipts",receipts);
        var job=ChamberUpgrade.get(l).claimedBy(a);j.addProperty("built",job==null?-1:job.built().size());var store=a.queenId()==null?null:MaterialStore.owned(l,a.queenId(),a.workerTasks().plan());j.addProperty("store",store==null?-1:store.size());return j;
    }
    public static void actionAfter(LasiusNigerEntity a,String kind,JsonObject before){
        var h=active;if(h==null||before==null||h.done)return;var after=actionBefore(a,kind+"-after");if(after==null||before.get("cargo").isJsonNull()||!a.getMainHandItem().isEmpty())return;
        boolean food=after.get("receipts").getAsLong()==before.get("receipts").getAsLong()+1;
        boolean material=kind.equals("materialDeposit")&&after.get("store").getAsInt()==before.get("store").getAsInt()+1||kind.equals("upgradeBuild")&&after.get("built").getAsInt()==before.get("built").getAsInt()+1;
        if(food||material){var j=h.process();j.add("before",before);j.add("after",after);j.addProperty("foodReceipt",food);j.addProperty("materialReceipt",material);h.write(h.phase+"-physical-service-"+(++h.events),j);if(h.reopen()){h.foodServed|=food;h.materialServed|=material;if(h.foodServed&&h.materialServed&&h.servicesCompletedAt<0)h.servicesCompletedAt=h.level.getGameTime();}}
    }
}
