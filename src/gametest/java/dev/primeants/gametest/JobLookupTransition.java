package dev.primeants.gametest;

import com.mojang.serialization.JsonOps;
import dev.primeants.PrimeAnts;
import dev.primeants.founding.*;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;

/** Observes ordinary job creation; never inserts a job or retains authority for a caller. */
final class JobLookupTransition {
    private final UUID unrelated=TestIdentities.next();
    private Object lastMissing;
    private boolean witnessed;
    private int missingTableSize=-1;
    void expansion(GameTestHelper c,ServerLevel l,UUID owner){
        var data=NestExpansion.get(l);boolean dirty=data.isDirty();var job=data.job(owner);
        c.assertTrue(data.job(null)==null&&data.job(unrelated)==null&&data.isDirty()==dirty,"Read-only expansion null/unrelated lookups preserve dirty state");
        if(job==null){lastMissing=data;missingTableSize=NestExpansion.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow().getAsJsonObject().size();return;}
        if(witnessed)return;witnessed=true;
        c.assertTrue(lastMissing!=null&&data.job(owner)==job,"Real expansion creation after a prior missing lookup returns the same live job");
        var encoded=NestExpansion.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow();var decoded=NestExpansion.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow();
        c.assertTrue(decoded.job(null)==null&&decoded.job(unrelated)==null&&decoded.job(owner)!=job&&encoded.equals(NestExpansion.CODEC.encodeStart(JsonOps.INSTANCE,decoded).getOrThrow()),"Nonempty expansion reconstruction retains exact serialized keys and job state");
        PrimeAnts.LOGGER.info("T17 LOOKUP TRANSITION kind=expansion owner={} missingTableSize={} sameInstance={} claim={} removed={}",owner,missingTableSize,lastMissing==data,job.claim,job.removed());
    }
    void mining(GameTestHelper c,ServerLevel l,UUID owner){
        var data=Mining.get(l);boolean dirty=data.isDirty();var job=data.job(owner);
        c.assertTrue(data.job(null)==null&&data.job(unrelated)==null&&data.isDirty()==dirty,"Read-only mining null/unrelated lookups preserve dirty state");
        if(job==null){lastMissing=data;missingTableSize=Mining.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow().getAsJsonObject().size();return;}
        if(witnessed)return;witnessed=true;
        c.assertTrue(lastMissing!=null&&data.job(owner)==job,"Real mining creation after a prior missing lookup returns the same live job");
        var encoded=Mining.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow();var decoded=Mining.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow();
        c.assertTrue(decoded.job(null)==null&&decoded.job(unrelated)==null&&decoded.job(owner)!=job&&encoded.equals(Mining.CODEC.encodeStart(JsonOps.INSTANCE,decoded).getOrThrow()),"Nonempty mining reconstruction retains exact serialized keys and job state");
        PrimeAnts.LOGGER.info("T17 LOOKUP TRANSITION kind=mining owner={} missingTableSize={} sameInstance={} claim={} removed={}",owner,missingTableSize,lastMissing==data,job.claim,job.removed());
    }
}
