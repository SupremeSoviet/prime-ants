package dev.primeants.gametest;

import dev.primeants.entity.AdultLife;
import dev.primeants.entity.AntForm;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.worker.Nutrition;

/** Test-only transcription of debb542 AdultLife.tick and Nutrition.spend, before T03 extraction.
 * The input age has already been incremented by the ordinary entity owner. Constants and arithmetic
 * deliberately retain that source's semantics, including its Java long counter overflow.
 * No call to evolve, advanceAdult, State.spend or a production arithmetic helper is permitted here. */
public final class LegacyAdultReference {
    private LegacyAdultReference() { }
    public record Step(AdultLife.State life, Nutrition.State nutrition, boolean damage, String cause) { }
    public static Step step(LasiusNigerEntity.Biology input, boolean reserveOnly, boolean survivesDamage) {
        var s=input.life();var n=input.nutrition();
        boolean started=s.started();
        if(!started&&(input.form()==AntForm.WORKER||!reserveOnly))started=true;
        if(!started)return new Step(s,n,false,"");
        long active=s.activeTicks()+1,maintenance=s.maintenanceTicks(),coveredTicks=s.coveredTicks();
        long fasting=s.fasting(),sugar=n.sugar(),spent=n.spentSugar();
        int coverage=s.coverageRemaining(),damage=s.damageTicks();
        boolean covered=coverage>0;
        if(covered)coverage--;
        else if(sugar>=1){sugar-=1;spent+=1;maintenance++;coverage=3;covered=true;}
        if(covered){coveredTicks++;fasting=0;}else if(fasting<Long.MAX_VALUE)fasting++;
        boolean old=input.form()==AntForm.WORKER&&input.age()>=s.lifespan();
        boolean starving=fasting>=s.grace();
        String cause="";boolean event=false;
        if(!old&&!starving)damage=0;
        else if(++damage>=20){damage=0;event=true;cause=old?"age_limit":"starvation";}
        return new Step(new AdultLife.State(s.lifespan(),s.grace(),fasting,maintenance,active,damage,
            s.mealTicks(),coverage,coveredTicks,started,event&&!survivesDamage?cause:""),
            new Nutrition.State(sugar,n.protein(),n.apples(),n.berries(),n.chickens(),n.nectar(),n.nectarV2(),
                n.prey(),n.flesh(),spent,n.spentProtein(),n.receivedSugar(),n.givenSugar()),event,cause);
    }
}
