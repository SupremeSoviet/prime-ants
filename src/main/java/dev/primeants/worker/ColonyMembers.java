package dev.primeants.worker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.primeants.entity.LasiusNigerEntity;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.*;

/** Identity registry of real emerged workers, independent of the single forager claim.
 * An absent lookup is unknown, never dead or free capacity. Only actual death records a tombstone. */
public final class ColonyMembers extends SavedData {
    public record Member(UUID worker,UUID brood,UUID queen,BlockPos home,boolean dead){
        static final Codec<UUID> ID=Codec.STRING.xmap(UUID::fromString,UUID::toString);
        static final Codec<Member> CODEC=RecordCodecBuilder.create(i->i.group(ID.fieldOf("worker").forGetter(Member::worker),ID.fieldOf("brood").forGetter(Member::brood),ID.fieldOf("queen").forGetter(Member::queen),BlockPos.CODEC.fieldOf("home").forGetter(Member::home),Codec.BOOL.fieldOf("dead").forGetter(Member::dead)).apply(i,Member::new));
    }
    public static final Codec<ColonyMembers> CODEC=Member.CODEC.listOf().xmap(ColonyMembers::new,m->List.copyOf(m.members.values()));
    public static final SavedDataType<ColonyMembers> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("prime_ants","colony_members"),ColonyMembers::new,CODEC,DataFixTypes.LEVEL);
    private final Map<UUID,Member> members=new LinkedHashMap<>();
    public ColonyMembers(){} private ColonyMembers(List<Member> list){for(var m:list)if(!m.worker().equals(workerId(m.brood()))||members.putIfAbsent(m.worker(),m)!=null)throw new IllegalArgumentException("Invalid colony identity registry");}
    public static ColonyMembers get(ServerLevel l){return l.getDataStorage().computeIfAbsent(TYPE);}
    public static UUID workerId(UUID brood){return UUID.nameUUIDFromBytes(("prime_ants:worker:"+brood).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    public Member member(UUID worker){return members.get(worker);}
    public List<Member> members(UUID queen){return members.values().stream().filter(m->m.queen().equals(queen)).toList();}
    public long occupied(UUID queen){return members(queen).stream().filter(m->!m.dead()).count();}
    public void record(UUID brood,UUID queen,BlockPos home){
        var m=new Member(workerId(brood),brood,queen,home.immutable(),false);var old=members.get(m.worker());
        if(old!=null){if(!old.brood().equals(brood)||!old.queen().equals(queen)||!old.home().equals(home))throw new IllegalStateException("Colony identity conflict");return;}
        members.put(m.worker(),m);setDirty();
    }
    public boolean belongs(LasiusNigerEntity w,UUID queen,BlockPos home){var m=members.get(w.getUUID());return m!=null&&!m.dead()&&m.brood().equals(w.broodId())&&m.queen().equals(queen)&&m.home().equals(home)&&queen.equals(w.queenId())&&home.equals(w.nurseryHome());}
    public void died(LasiusNigerEntity w){var m=members.get(w.getUUID());if(m!=null&&belongs(w,m.queen(),m.home())){members.put(w.getUUID(),new Member(m.worker(),m.brood(),m.queen(),m.home(),true));setDirty();}}
}
