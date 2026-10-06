package dev.primeants.worker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.*;
import java.util.*;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.*;

/** A local, loaded-time response to accepted player harm. No target is assigned to a mob. */
public final class ColonyAlarm extends SavedData {
    public static final int DURATION=600, RESPONSE_RADIUS=12, CHASE_RADIUS=16;
    private static final Codec<UUID> ID=Codec.STRING.xmap(UUID::fromString,UUID::toString);
    public record Alarm(UUID player,BlockPos origin,int remaining){
        public Alarm {if(remaining<1||remaining>DURATION)throw new IllegalArgumentException("Invalid bounded alarm");origin=origin.immutable();}
        static final Codec<Alarm> CODEC=RecordCodecBuilder.create(i->i.group(ID.fieldOf("player").forGetter(Alarm::player),BlockPos.CODEC.fieldOf("origin").forGetter(Alarm::origin),Codec.INT.fieldOf("remaining").forGetter(Alarm::remaining)).apply(i,Alarm::new));
    }
    public static final Codec<ColonyAlarm> CODEC=Codec.unboundedMap(ID,Alarm.CODEC).xmap(ColonyAlarm::new,a->Map.copyOf(a.alarms));
    public static final SavedDataType<ColonyAlarm> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("prime_ants","colony_alarm"),ColonyAlarm::new,CODEC,DataFixTypes.LEVEL);
    private final Map<UUID,Alarm> alarms;
    public ColonyAlarm(){this(Map.of());}private ColonyAlarm(Map<UUID,Alarm> a){alarms=new HashMap<>(a);}
    public static ColonyAlarm get(ServerLevel l){return l.getDataStorage().computeIfAbsent(TYPE);}
    public Alarm alarm(UUID colony){return alarms.get(colony);}
    public static boolean validPlayer(ServerLevel l,Player p){return p!=null&&p.level()==l&&p.isAlive()&&!p.isRemoved()&&!p.isCreative()&&!p.isSpectator();}
    public void harm(ServerLevel l,UUID colony,Player player,BlockPos origin,String cause){
        if(colony==null||!validPlayer(l,player))return;
        alarms.put(colony,new Alarm(player.getUUID(),origin,DURATION));setDirty();
        dev.primeants.PrimeAnts.LOGGER.info("Colony alarm colony={} player={} source={} origin={} loadedTicks={}",colony,player.getUUID(),cause,origin,DURATION);
    }
    public static void tick(ServerLevel l){
        var d=get(l);var it=d.alarms.entrySet().iterator();
        while(it.hasNext()){var e=it.next();var a=e.getValue();if(!loaded(l,e.getKey(),a))continue;
            if(a.remaining()==1)it.remove();else e.setValue(new Alarm(a.player(),a.origin(),a.remaining()-1));d.setDirty();}
        pending.keySet().removeIf(k->k.level()==l);
    }
    private static boolean loaded(ServerLevel l,UUID colony,Alarm a){
        if(NestPlan.loaded(l,a.origin())&&l.isPositionEntityTicking(a.origin()))return true;
        // A loaded responder/target beside an unloaded anchor must still spend
        // alarm time. Otherwise a chunk boundary could make pursuit unlimited.
        var origin=net.minecraft.world.phys.Vec3.atBottomCenterOf(a.origin());
        var p=l.getPlayerByUUID(a.player());if(validPlayer(l,p)&&p.position().distanceToSqr(origin)<=CHASE_RADIUS*CHASE_RADIUS&&l.isPositionEntityTicking(p.blockPosition()))return true;
        for(var m:ColonyMembers.get(l).members(colony))if(!m.dead()&&l.getEntity(m.worker()) instanceof LasiusNigerEntity w&&w.isAlive()&&w.position().distanceToSqr(origin)<=CHASE_RADIUS*CHASE_RADIUS&&l.isPositionEntityTicking(w.blockPosition()))return true;
        return false;
    }
    /** Current ownership only. Materials and old plan coordinates never grant authority. */
    public static UUID ownedComponent(ServerLevel l,BlockPos p){
        var owner=ColonyTerrain.get(l).componentOwner(l,p);if(owner!=null)return owner;
        owner=ColonyPlugs.get(l).componentOwner(l,p);if(owner!=null)return owner;
        if(l.getBlockEntity(p) instanceof dev.primeants.brood.BroodPile b)return b.componentOwner();
        if(l.getBlockEntity(p) instanceof NestCache cache)return cache.componentOwner();
        if(l.getBlockEntity(p) instanceof MaterialStore store)return store.componentOwner();return null;
    }
    private record BreakKey(ServerLevel level,UUID player,BlockPos pos){}
    private record Before(UUID colony,BlockState state,long tick){}
    private static final Map<BreakKey,Before> pending=new HashMap<>();
    public static void initialize(){
        PlayerBlockBreakEvents.BEFORE.register((world,player,pos,state,entity)->{
            if(world instanceof ServerLevel l){var key=new BreakKey(l,player.getUUID(),pos.immutable());pending.remove(key);
                var owner=ownedComponent(l,pos);if(owner!=null&&validPlayer(l,player))pending.put(key,new Before(owner,state,l.getGameTime()));}
            return true;
        });
        PlayerBlockBreakEvents.CANCELED.register((world,player,pos,state,entity)->{if(world instanceof ServerLevel l)pending.remove(new BreakKey(l,player.getUUID(),pos));});
        PlayerBlockBreakEvents.AFTER.register((world,player,pos,state,entity)->{
            if(world instanceof ServerLevel l){var before=pending.remove(new BreakKey(l,player.getUUID(),pos));
                // Fabric calls AFTER inside the successful removeBlock branch. Ordinary
                // LevelChunk writes have already revoked soil/plug ownership by now.
                if(before!=null&&before.tick()==l.getGameTime()&&before.state().getBlock()==state.getBlock())get(l).harm(l,before.colony(),player,pos,"successful_owned_player_break");}
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_LEVEL_TICK.register(ColonyAlarm::tick);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->pending.clear());
    }
}
