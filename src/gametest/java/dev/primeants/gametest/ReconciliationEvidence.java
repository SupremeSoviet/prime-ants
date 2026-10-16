package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.PrimeAnts;
import dev.primeants.time.*;
import java.util.OptionalLong;

/** Machine-readable acceptance observations. Test-only, no change to production decisions. */
final class ReconciliationEvidence {
    static void number(JsonObject out, String field, OptionalLong n) {
        if (n.isPresent()) out.addProperty(field,n.getAsLong()); else out.add(field,JsonNull.INSTANCE);
    }
    static JsonObject interval(ColonyClock.Interval interval) {
        var out=new JsonObject(); number(out,"after",interval.afterGameTime());
        out.addProperty("before",interval.beforeGameTime()); out.addProperty("ticks",interval.ticks()); return out;
    }
    static JsonObject adult(AdultSimulation.Snapshot evidence) {
        var out=new JsonObject(); out.addProperty("boundary",evidence.boundary()); out.addProperty("origin",evidence.origin().name());
        out.addProperty("known_after",evidence.knownAfter()); out.addProperty("latest",evidence.latest());
        out.addProperty("invalid_time",evidence.invalidTime());
        var runs=new JsonArray(); for(var r:evidence.runs()){var run=new JsonArray();run.add(r.first());run.add(r.last());runs.add(run);}out.add("runs",runs);return out;
    }
    static JsonObject member(ReloadObservation.Member member) {
        var out=new JsonObject();out.addProperty("adult",member.adult().toString());
        if(member.brood()!=null)out.addProperty("brood",member.brood().toString());
        out.addProperty("queen",member.queen().toString());out.addProperty("home",member.home().asLong());
        out.addProperty("availability",member.availability());out.addProperty("reserved",member.reserved());
        out.addProperty("status",member.coverage().status());number(out,"already_simulated",member.coverage().alreadySimulated());number(out,"missed",member.coverage().missed());
        out.addProperty("before_birth_boundary",member.coverage().beforeBoundary());
        if(member.evidence()!=null){
            out.add("evidence",adult(member.evidence()));out.add("own_interval",interval(member.ownInterval()));
            out.add("current_timestamp_interval",interval(member.currentTimestampInterval()));
            if(member.returnBoundary()!=null){
                out.add("return_interval",interval(member.returnBoundary().ownInterval()));
                out.add("return_evidence",adult(member.returnBoundary().evidence()));
            }
        }return out;
    }
    static JsonObject observation(ReloadObservation observation) {
        var out=new JsonObject();out.addProperty("queen",observation.queen().toString());out.addProperty("nursery",observation.nursery().asLong());
        out.add("interval",interval(observation.interval()));var members=new JsonArray();observation.members().forEach(m->members.add(member(m)));out.add("members",members);return out;
    }
    static void log(String label,String kind,JsonObject out) {
        out.addProperty("label",label);out.addProperty("fixture_kind",kind);PrimeAnts.LOGGER.info("T02 EVIDENCE {}",out);
    }
}
