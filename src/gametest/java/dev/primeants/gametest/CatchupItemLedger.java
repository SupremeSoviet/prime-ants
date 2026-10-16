package dev.primeants.gametest;

import com.google.gson.*;
import dev.primeants.brood.BroodPile;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.item.AntItems;
import dev.primeants.worker.*;
import java.util.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;

/** Exact within-branch item operands, independent of cross-branch tolerances. */
final class CatchupItemLedger {
    private static final List<net.minecraft.world.item.Item> ITEMS=List.of(Items.APPLE,Items.SWEET_BERRIES,Items.CHICKEN,AntItems.FLOWER_NECTAR,AntItems.FLOWER_NECTAR_V2,AntItems.SMALL_PREY,Items.ROTTEN_FLESH);
    static String kind(ItemStack s){for(int i=0;i<ITEMS.size();i++)if(s.is(ITEMS.get(i)))return CatchupOutcomes.KINDS.get(i);throw new IllegalArgumentException("Not physical food");}
    static JsonObject snapshot(GameTestHelper c,LasiusNigerEntity q,BroodPile p,NestCache cache){
        var state=AllocationObservations.state(c,q,p,0);var row=new JsonObject();var releaseIds=new HashSet<UUID>();
        for(var m:ColonyMembers.get(c.getLevel()).members(q.getUUID()))releaseIds.add(UUID.nameUUIDFromBytes(("worker-cargo:"+m.worker()).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        var tag=cache.saveWithFullMetadata(c.getLevel().registryAccess());String release=tag.getStringOr("ReleaseId","");
        for(int n=0;n<tag.getIntOr("SharesShed",0);n++)releaseIds.add(UUID.nameUUIDFromBytes((release+":share:"+n).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        var released=new ArrayList<ItemStack>();var releases=new JsonArray();
        for(var id:releaseIds){
            if(c.getLevel().getEntity(id) instanceof net.minecraft.world.entity.item.ItemEntity item){released.add(item.getItem());var r=new JsonObject();r.addProperty("id",id.toString());r.addProperty("destination","world");r.addProperty("stack",item.getItem().toString());releases.add(r);}
            for(var pending:TransferCustody.get(c.getLevel()).contents())if(pending.id().equals(id)){released.add(pending.stack());var r=new JsonObject();r.addProperty("id",id.toString());r.addProperty("destination","custody");r.addProperty("stack",pending.stack().toString());releases.add(r);}
        }
        // Keep actual actor custody and recipient receipts, not only a fungible grand total.
        var carriers=new JsonObject();var adultReceipts=new JsonObject();var broodReceipts=new JsonObject();
        for(var adult:state.getAsJsonArray("adults")){
            var a=adult.getAsJsonObject();var actor=new JsonObject();var body=(LasiusNigerEntity)c.getLevel().getEntity(UUID.fromString(a.get("id").getAsString()));
            var hand=body.getMainHandItem();actor.addProperty("stack",hand.toString());actor.addProperty("count",hand.getCount());
            if(WorkerTasks.food(hand))actor.addProperty("kind",kind(hand));actor.add("position",a.get("position"));
            carriers.add(a.get("id").getAsString(),actor);adultReceipts.add(a.get("id").getAsString(),receipts(a.getAsJsonObject("nutrition")));
        }
        for(var brood:state.getAsJsonArray("brood")){var b=brood.getAsJsonObject();broodReceipts.add(b.get("id").getAsString(),receipts(b.getAsJsonObject("nutrition")));}
        row.add("carriers",carriers);row.add("adult_receipts",adultReceipts);row.add("current_brood_receipts",broodReceipts);
        row.add("brood_receipts_including_archived",state.get("brood_consumption"));
        row.add("actual_releases",releases);var kinds=new JsonObject();
        for(int i=0;i<ITEMS.size();i++){
            var item=ITEMS.get(i);var kind=CatchupOutcomes.KINDS.get(i);var r=new JsonObject();
            long stock=cache.contents().stream().filter(s->s.is(item)).mapToLong(ItemStack::getCount).sum();
            long cargo=AutomaticCatchupGameTest.adults(c,q).stream().map(LasiusNigerEntity::getMainHandItem).filter(s->s.is(item)).mapToLong(ItemStack::getCount).sum();
            long receipts=CatchupOutcomes.consumed(state,kind),releaseCount=released.stream().filter(s->s.is(item)).mapToLong(ItemStack::getCount).sum();
            r.addProperty("stock",stock);r.addProperty("carried",cargo);r.addProperty("ingested_including_preboundary_and_archived_brood",receipts);r.addProperty("actual_released",releaseCount);r.addProperty("total",stock+cargo+receipts+releaseCount);kinds.add(kind,r);
        }
        row.add("kinds",kinds);return row;
    }
    static JsonObject receipts(JsonObject nutrition){var row=new JsonObject();for(var kind:CatchupOutcomes.KINDS)row.add(kind,nutrition.get(kind));return row;}
    static boolean compare(JsonObject initial,JsonObject end,String branch,JsonObject row){
        var ledger=new JsonObject();ledger.add("initial",initial);ledger.add("final",end);boolean exact=true;var equations=new JsonArray();
        for(var kind:CatchupOutcomes.KINDS){long a=initial.getAsJsonObject("kinds").getAsJsonObject(kind).get("total").getAsLong(),b=end.getAsJsonObject("kinds").getAsJsonObject(kind).get("total").getAsLong();var e=new JsonObject();e.addProperty("kind",kind);e.addProperty("initial",a);e.addProperty("final",b);e.addProperty("exact",a==b);equations.add(e);exact&=a==b;}
        ledger.add("equations",equations);ledger.addProperty("exact",exact);row.add(branch+"_item_ledger",ledger);return exact;
    }
}
