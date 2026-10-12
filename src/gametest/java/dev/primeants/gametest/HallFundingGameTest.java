package dev.primeants.gametest;
import dev.primeants.brood.BroodPile;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Independent physical-food experiment, never a replacement for either frozen hall body. */
public final class HallFundingGameTest {
    @GameTest(maxTicks=5400,structure="prime_ants_test:surface_ground")
    public void physicalFundingDeliversFourEggsAtThreeUninterruptedHallIntervals(GameTestHelper c){run(c,false);}
    @GameTest(maxTicks=5400,structure="prime_ants_test:surface_ground")
    public void matchedPhysicalProteinShortageRefusesUnfundedInvestment(GameTestHelper c){run(c,true);}
    private void run(GameTestHelper c,boolean shortage){
        var f=new HallFundingFixture();var q=f.start(c,shortage);
        var p=HallFundingProbe.watch(c,q,f,shortage);
        FoodServiceProbe.watch(c,q,5400,shortage?"t07_hall_protein_shortage":"t07_hall_funded");
        boolean[] initial={false};
        c.onEachTick(()->{
            if(!initial[0]){c.assertTrue(c.getTick()<=1,"Prospective first physical supply at helper 0/1");initial[0]=true;f.supply(c,q,8,shortage?0:3);}
            f.ledger(c,q);
            if(shortage&&p.shortageDecisions()>=30){p.accept();FoodServiceProbe.terminal(c,q);c.succeed();return;}
            if(!shortage&&p.accepted()){p.accept();FoodServiceProbe.terminal(c,q);c.succeed();return;}
            if(c.getTick()==4800&&p.measurement()<0)c.fail("T07 meal-eligible preparation never qualified within frozen4800; inspect the measured prerequisite");
            if(p.measurement()>=0)c.assertTrue(NestPlanFixture.pile(c,q).loadedTicks()-p.measurement()<600,"Frozen600 loaded-tick measurement bound; interruptions retain resets");
        });
    }
}
