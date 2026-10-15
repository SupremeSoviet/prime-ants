package dev.primeants.gametest;

import com.google.gson.*;
import java.util.*;

/** Frozen T01 outcome contract: no donor, route, timing or per-recipient item oracle. */
final class CatchupOutcomes {
    static final long CONSUMPTION_TOLERANCE=1, LARVAL_PROTEIN=8000, DEVELOPMENT_TOLERANCE=120, DEVELOPMENT_FALLBACK=240;
    static final List<String> KINDS=List.of("apples","berries","chickens","nectar","nectarV2","prey","flesh");
    static long consumed(JsonObject state,String kind){
        long total=0;
        for(var value:state.getAsJsonArray("adults")){
            var n=value.getAsJsonObject().getAsJsonObject("nutrition");
            total+=n.get(kind).getAsLong();
        }
        return total+state.getAsJsonObject("brood_consumption").get(kind).getAsLong()-(kind.equals("nectar")?state.getAsJsonObject("brood_consumption").get("nectarV2").getAsLong():0);
    }
    static JsonObject compare(JsonObject live,JsonObject away){
        var row=new JsonObject();row.add("live",live);row.add("catchup",away);
        for(var entry:Map.of("live",live,"catchup",away).entrySet()){
            long given=0,received=0;
            for(var value:entry.getValue().getAsJsonArray("adults")){var n=value.getAsJsonObject().getAsJsonObject("nutrition");given+=n.get("givenSugar").getAsLong();received+=n.get("receivedSugar").getAsLong();}
            row.addProperty(entry.getKey()+"_crop_given",given);row.addProperty(entry.getKey()+"_crop_received",received);row.addProperty(entry.getKey()+"_crop_balanced",given==received);
        }
        var consumption=new JsonArray();boolean items=true;
        for(var kind:KINDS){long x=consumed(live,kind),y=consumed(away,kind);var d=new JsonObject();
            d.addProperty("kind",kind);d.addProperty("live",x);d.addProperty("catchup",y);d.addProperty("difference",y-x);
            consumption.add(d);items&=Math.abs(y-x)<=CONSUMPTION_TOLERANCE;
        }
        var classes=new JsonArray();boolean adults=true;
        for(var value:live.getAsJsonArray("adults")){var x=value.getAsJsonObject();var y=away.getAsJsonArray("adults").asList().stream().map(JsonElement::getAsJsonObject).filter(a->a.get("id").equals(x.get("id"))).findFirst().orElse(null);
            var d=new JsonObject();d.add("id",x.get("id"));d.add("live_hungry",x.get("hungry"));d.add("live_starving",x.get("starving"));
            boolean same=y!=null&&x.get("hungry").equals(y.get("hungry"))&&x.get("starving").equals(y.get("starving"));
            if(y!=null){d.add("catchup_hungry",y.get("hungry"));d.add("catchup_starving",y.get("starving"));}
            d.addProperty("matches",same);classes.add(d);adults&=same;
        }
        row.add("per_kind_consumption",consumption);row.add("adult_classes",classes);row.addProperty("adult_classes_match",adults);row.addProperty("consumption_within_one_per_kind",items);
        return row;
    }
}
