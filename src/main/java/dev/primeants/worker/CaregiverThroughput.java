package dev.primeants.worker;

import dev.primeants.entity.LasiusNigerEntity;
import java.util.*;
import net.minecraft.world.level.storage.*;

/** Capacity only, never a trip or a claim on an item/recipient. No ordinary clock mutation. */
public final class CaregiverThroughput {
    // Frozen native live calibration: 1555 per-caregiver ticks / 17 completed cycles,
    // rounded upward. Includes ordinary navigation and cooldown; see catchup-report.md.
    public static final long INTERVAL=92;
    private final Map<UUID,Long> phase=new TreeMap<>();
    public void retain(List<LasiusNigerEntity> caregivers){
        var ids=new HashSet<UUID>();for(var a:caregivers){ids.add(a.getUUID());phase.putIfAbsent(a.getUUID(),0L);}phase.keySet().retainAll(ids);
    }
    public long nextBoundary(){return phase.values().stream().filter(t->t<INTERVAL).mapToLong(t->INTERVAL-t).min().orElse(Long.MAX_VALUE);}
    public boolean available(UUID caregiver){return phase.getOrDefault(caregiver,0L)==INTERVAL;}
    public void complete(UUID caregiver){if(!available(caregiver))throw new IllegalStateException("Unearned caregiver service");phase.put(caregiver,0L);}
    public void advance(long ticks){phase.replaceAll((id,t)->t+Math.min(ticks,INTERVAL-t));}
    public void reset(){phase.clear();}
    public void save(ValueOutput out){var list=out.childrenList("CaregiverThroughput");for(var e:phase.entrySet()){var child=list.addChild();child.putString("Id",e.getKey().toString());child.putLong("Ticks",e.getValue());}}
    public void load(ValueInput in){phase.clear();for(var child:in.childrenListOrEmpty("CaregiverThroughput")){var id=UUID.fromString(child.getStringOr("Id",""));long t=child.getLongOr("Ticks",-1);if(t<0||t>INTERVAL||phase.putIfAbsent(id,t)!=null)throw new IllegalArgumentException("Invalid caregiver capacity phase");}}
}
