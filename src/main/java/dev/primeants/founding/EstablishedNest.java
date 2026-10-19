package dev.primeants.founding;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import dev.primeants.colony.ColonyStage;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.*;

/** Initial-world-state plans, never excavation/haul/payment/birth receipts. Ownership remains revocable per cell.
 * This ledger grants only confirmation of its already installed physical space, never runtime clearing. */
public final class EstablishedNest extends SavedData {
    public record Initial(NestPlan home,ColonyStage profile,List<BlockPos> space,List<BlockPos> shell){
        String encode(){var j=new JsonObject();j.addProperty("version",EstablishedGeneration.VERSION);j.addProperty("entrance",home.entrance().asLong());j.addProperty("direction",home.direction().getName());j.addProperty("profile",profile.serializedName());return j.toString();}
        static Initial decode(String s){var j=JsonParser.parseString(s).getAsJsonObject();if(!EstablishedGeneration.VERSION.equals(j.get("version").getAsString()))throw new IllegalArgumentException("Unsupported generated nest version");var d=Direction.byName(j.get("direction").getAsString());return of(NestPlan.geometry(BlockPos.of(j.get("entrance").getAsLong()),d),ColonyStage.byName(j.get("profile").getAsString()));}
    }
    public static Initial of(NestPlan p,ColonyStage stage){
        if(stage!=ColonyStage.YOUNG&&stage!=ColonyStage.MATURE)throw new IllegalArgumentException("Established profile");
        var space=new LinkedHashSet<>(p.tasks());
        if(stage==ColonyStage.MATURE){space.addAll(NestExpansion.targets(p,1));for(var name:List.of("left","hall_left"))space.addAll(ChamberExcavation.build(p,name).tasks());}
        var shell=new LinkedHashSet<BlockPos>();
        for(var cell:space)for(var d:Direction.values()){var n=cell.relative(d);if(n.getY()<p.entrance().getY()&&!space.contains(n))shell.add(n);}
        return new Initial(p.routeGeometry(),stage,List.copyOf(space),List.copyOf(shell));
    }
    private static final Codec<Initial> INITIAL=Codec.STRING.xmap(Initial::decode,Initial::encode);
    public static final Codec<EstablishedNest> CODEC=Codec.unboundedMap(Codec.STRING,INITIAL).xmap(EstablishedNest::new,n->Map.copyOf(n.nests));
    public static final SavedDataType<EstablishedNest> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("prime_ants","established_nests"),EstablishedNest::new,CODEC,DataFixTypes.LEVEL);
    private final Map<String,Initial> nests=new HashMap<>();
    public EstablishedNest(){} private EstablishedNest(Map<String,Initial> data){nests.putAll(data);}
    public static EstablishedNest get(ServerLevel l){return l.getDataStorage().computeIfAbsent(TYPE);}
    public Initial initial(UUID owner){return owner==null?null:nests.get(owner.toString());}
    void installed(ServerLevel l,UUID owner,Initial initial){
        if(!EstablishedGeneration.initializing(l,owner,initial.home())||nests.putIfAbsent(owner.toString(),initial)!=null)throw new IllegalStateException("Generated nest initialization conflict");setDirty();
    }
    public List<BlockPos> floors(ServerLevel l,UUID owner){
        var n=initial(owner);if(n==null)return List.of();var terrain=ColonyTerrain.get(l);
        return n.space().stream().filter(p->p.getY()==n.home().entrance().getY()-2&&terrain.generatedOpeningIntact(l,p,owner)&&terrain.generatedOpeningIntact(l,p.above(),owner)&&NestPlan.walkable(l,p)).toList();
    }
    public void findings(ServerLevel l,UUID owner,Findings r){
        var n=initial(owner);if(n==null)return;var t=ColonyTerrain.get(l);
        var nursery=n.home().nursery();var cache=n.home().cache();
        var store=n.profile()==ColonyStage.MATURE?dev.primeants.worker.MaterialStore.marker(n.home(),"left"):null;
        for(var p:n.space())if(r.cell(t.openingLoaded(l,p,owner),"generated_space_unavailable")&&!p.equals(nursery)&&!p.equals(cache)
                &&!p.equals(store)&&!t.generatedOpeningIntact(l,p,owner))r.fault("generated_space_revoked_or_obstructed");
        for(var p:n.shell())if(r.cell(t.openingLoaded(l,p,owner),"generated_shell_unavailable")&&(!l.getBlockState(p).isSolidRender()||!l.getFluidState(p).isEmpty())&&!DigJob.anyOpening(l,p,owner))r.fault("generated_shell_open");
    }
}
