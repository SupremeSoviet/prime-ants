package dev.primeants.gametest;

import dev.primeants.brood.*;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.worker.*;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;

/** Negative migration fixture only: existing underfunded dependents must still stall/expire.
 * Unchanged real dropped meals fund every old egg; the current gate deliberately refuses them. */
final class PaidLegacyBrood {
    static boolean restoreEggs(GameTestHelper c,LasiusNigerEntity q,BroodPile pile,int count){
        if(!pile.operational()||!pile.records().isEmpty()||pile.consumed().size()!=3||q.nutrition().sugar()<count*Nutrition.EGG_SUGAR||q.nutrition().protein()<count*Nutrition.EGG_PROTEIN)return false;
        c.assertTrue(count<=3&&ColonyMembers.get(c.getLevel()).occupied(q.getUUID())+count<pile.adultCapacity(),"Legacy dependent slots remain inside original capacity");
        c.assertTrue(!pile.growth().allows(pile.supply(c.getLevel())),"Current gate refuses this unfunded new investment; legacy fixture never claims current laying");
        c.assertTrue(q.nutrition().spend(count*Nutrition.EGG_SUGAR,count*Nutrition.EGG_PROTEIN),"Each restored old egg debits actual physically ingested queen stock exactly once");
        var tag=pile.saveWithFullMetadata(c.getLevel().registryAccess());var records=new ListTag();
        for(int slot=0;slot<count;slot++){
            var r=new CompoundTag();r.putString("Id",UUID.randomUUID().toString());r.putString("Queen",q.getUUID().toString());r.putInt("Slot",slot);r.putString("Stage","EGG");r.putLong("Progress",0);r.putLong("Nourishment",0);r.putBoolean("Founding",false);r.putLong("NeglectGrace",q.broodNeglectGrace());r.putLong("WaitingBound",q.cocoonWaitingBound());records.add(r);
        }
        tag.put("Brood",records);tag.putLong("LastLayingTick",pile.loadedTicks());
        pile.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING,c.getLevel().registryAccess(),tag));pile.setChanged();
        var state=pile.getBlockState();for(var property:java.util.List.of(BroodPileBlock.A,BroodPileBlock.B,BroodPileBlock.C))state=state.setValue(property,BroodStage.EGG);
        c.getLevel().setBlock(pile.getBlockPos(),state,3);
        dev.primeants.PrimeAnts.LOGGER.info("T21 controlled paid legacy dependent restore queen={} count={} sugarDebit={} proteinDebit={} originalClutchPreserved=true noNewIncome=true",q.getUUID(),count,count*Nutrition.EGG_SUGAR,count*Nutrition.EGG_PROTEIN);return true;
    }
}
