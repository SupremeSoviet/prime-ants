package dev.primeants.gametest.mixin;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.MiningPreparationCausalProbe;
import java.util.*;
import net.minecraft.gametest.framework.GameTestHelper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Actual original fixture calls, not a replacement feeder or eligibility decision. */
@Mixin(targets="dev.primeants.gametest.TierTwoFixture",remap=false)
public abstract class PreparationFixtureMixin {
    @Shadow long apples,chickens,clay,lastApple,lastChicken,lastWorkMeal,lastWorkProtein,materialEnd;
    @Shadow boolean opened,feeding;
    @Unique private Map<String,Object> primeAnts$fixtureCounters(){return Map.ofEntries(Map.entry("apples",apples),Map.entry("chickens",chickens),Map.entry("clay",clay),Map.entry("lastApple",lastApple),Map.entry("lastChicken",lastChicken),Map.entry("lastWorkMeal",lastWorkMeal),Map.entry("lastWorkProtein",lastWorkProtein),Map.entry("materialEnd",materialEnd),Map.entry("opened",opened),Map.entry("feeding",feeding));}
    @Inject(method="feed",at=@At("HEAD")) private void beforeFeed(GameTestHelper c,LasiusNigerEntity q,CallbackInfo ci){if(MiningPreparationCausalProbe.observes(c,q))MiningPreparationCausalProbe.fixture(c,q,"actual_fixture_feed_entry",primeAnts$fixtureCounters());}
    @Inject(method="feed",at=@At("RETURN")) private void afterFeed(GameTestHelper c,LasiusNigerEntity q,CallbackInfo ci){if(MiningPreparationCausalProbe.observes(c,q))MiningPreparationCausalProbe.fixture(c,q,"actual_fixture_feed_return",primeAnts$fixtureCounters());}
    @Inject(method="supply",at=@At("HEAD")) private void beforeSupply(GameTestHelper c,LasiusNigerEntity q,int sugar,int protein,CallbackInfo ci){if(MiningPreparationCausalProbe.observes(c,q))MiningPreparationCausalProbe.fixture(c,q,"actual_fixture_supply_entry",Map.of("actualSugarUnitsArgument",sugar,"actualProteinUnitsArgument",protein,"before",primeAnts$fixtureCounters()));}
    @Inject(method="supply",at=@At("RETURN")) private void afterSupply(GameTestHelper c,LasiusNigerEntity q,int sugar,int protein,CallbackInfo ci){if(MiningPreparationCausalProbe.observes(c,q))MiningPreparationCausalProbe.fixture(c,q,"actual_fixture_supply_return",Map.of("actualSugarUnitsArgument",sugar,"actualProteinUnitsArgument",protein,"after",primeAnts$fixtureCounters()));}
    @Inject(method="material",at=@At("RETURN")) private void afterMaterial(GameTestHelper c,LasiusNigerEntity q,int units,CallbackInfo ci){if(MiningPreparationCausalProbe.observes(c,q))MiningPreparationCausalProbe.fixture(c,q,"actual_fixture_material_return",Map.of("actualUnitsArgument",units,"after",primeAnts$fixtureCounters()));}
}
