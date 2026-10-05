package dev.primeants.gametest;

import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.worker.AdultHistory;
import net.minecraft.server.level.ServerLevel;

/** Terminal food conservation includes real adult meals, even after the body is gone. */
final class AdultReceipts {
    static long consumed(ServerLevel l,LasiusNigerEntity queen){return count(l,queen,false);}
    static long nectar(ServerLevel l,LasiusNigerEntity queen){return count(l,queen,true);}
    private static long count(ServerLevel l,LasiusNigerEntity queen,boolean nectar){
        long total=0;
        for(var e:l.getAllEntities())if(e instanceof LasiusNigerEntity w&&w.isAlive()&&queen.getUUID().equals(w.queenId()))total+=nectar?w.nutrition().nectar():w.nutrition().consumedUnits();
        for(var entry:AdultHistory.get(l).records().entrySet())if(!entry.getKey().equals(queen.getUUID().toString())){
            var row=com.google.gson.JsonParser.parseString(entry.getValue()).getAsJsonObject();
            if(!row.get("queen").getAsString().equals(queen.getUUID().toString()))continue;
            var n=row.getAsJsonObject("nutrition");
            if(nectar)total+=n.get("nectar").getAsLong();else for(String item:new String[]{"apples","berries","chickens","nectar","flesh"})total+=n.get(item).getAsLong();
        }
        return total;
    }
}
