# Catch-up simulation

This document describes what happens to a colony while its chunks are unloaded, and what the game does when they
load again, as of Prime Ants 0.2.0.

## Why it exists

Prime Ants simulates a colony only while its chunks are loaded. Ants are entities, and the nursery and its brood live
in a block entity; Minecraft ticks neither in unloaded chunks. Without catch-up, a colony you walk away from would
stand still until you came back.

Catch-up fills that gap. When the colony loads again, the mod works out how much game time passed while it was not
ticking and applies a simplified version of that time in a few large steps: adults age, eat and may die, brood
develops, the queen may lay, and the colony's stage is re-evaluated. Nothing runs while the chunks are unloaded, and
catch-up never loads chunks by itself. It is always on; there is no setting to disable it.

## When it runs

Catch-up is coordinated by the colony's nursery, the brood pile. After a reload it runs the first time the nursery
takes its normal server tick, or an ant of the colony takes its normal step while the nursery's chunk is loaded,
whichever comes first. It runs on the server thread before that tick's ordinary step, so normal play continues from
the caught-up state, and since both entry points run the same code first, the result does not depend on which loads
first. A colony gets at most one catch-up round per game tick. There is no world scan, no background thread and no
chunk ticket. A queen that has not yet set up her brood pile has no nursery, so catch-up does not apply to her.

### Clocks and elapsed time

Catch-up measures game time, the world's tick counter, never the time of day or the wall clock. Time while the server
is stopped or a singleplayer game is paused counts for nothing, so closing a world and reopening it later adds no
owed time. Catch-up matters when chunks unload while the world keeps running: when you travel far away, or when
nobody is near the colony on a server.

Three clocks are involved: the game tick of the nursery's last completed normal step, which the nursery saves; each
adult's own record of the stretches in which it actually ticked (up to 32); and the nursery's loaded-tick counter,
which drives brood development, the laying cadence and the stage rules, and which catch-up advances.

If the nursery last stepped at tick L and the current tick is C, catch-up owes the ticks strictly between them:
C − L − 1. Step L already happened, and step C belongs to normal play. Equal or reversed times owe nothing (the next
normal step simply resets the clock), and there is no upper limit on the length of an absence. Each adult has its own
timeline: an ant whose chunk stayed loaded while the nursery's did not has already lived through part of the
interval, so catch-up skips the stretches its record shows as ticked and simulates only the rest.

### Unknown time

Some time cannot be reconstructed, and catch-up does not guess it:

- A nursery saved before catch-up existed starts counting at its first load; nothing is owed for earlier time.
- If an adult's record does not reach back to the start of the owed interval (it came from an older save, it was
  shortened after more than 32 stretches, or it restarted after the game clock went backwards), the time before the
  earliest known point is recorded as unknown. It is not simulated and nobody is charged for it. Running
  stage-shortfall clocks are cleared, because nothing shows that the shortfall lasted through that time.
- An ant with an invalid (negative) time record makes catch-up wait until its next normal step starts a fresh record.

## How a round works

The owed interval is processed in batches, not tick by tick. A batch ends at the earliest upcoming event: an adult's
next health event or the moment it becomes hungry, a brood stage change or viability limit, the next egg the queen
can lay, a stage-shortfall deadline, the next moment a caregiver can serve food, or a point where an ant's own record
switches between ticked and not ticked. Nothing changes course between events, so a batch is applied in one step and
its cost does not depend on its length. At the start of each batch food is distributed; then the batch is applied to
the adults and the nursery, and an egg that has come due is laid.

A round may use at most 512 events per colony. Each batch counts as one, and so does each food serving or crop
share. If the limit is reached, the exact remainder is saved and continues on the next tick. While work is pending,
the nursery's normal step stays paused and every waiting tick is added to the owed time; the ants keep ticking.

## What it simulates

Catch-up reuses the live rules for upkeep, damage, brood development, laying costs and stage changes. What it
simplifies is how often decisions are made and how food gets around.

### Adults

Every adult that was not ticking ages by the batch length, and a callow worker keeps maturing. Upkeep follows the live
rule: each point of sugar in the crop pays for four ticks (an apple's 4,000 sugar lasts 16,000 ticks), and time not
paid for counts as fasting. An ant starts losing health once its fasting reaches the fasting grace (one game day by
default), and a worker also once it passes its lifespan (six game days by default): one health point every 20 ticks.
Batches stop at every health event, so each one goes through the normal damage path in order, and a fatal one through
the normal death handling, which records the death and deals with whatever the ant was carrying. The live damage
cooldown does not swallow separate simulated hits, and the ant's current cooldown is restored afterwards. Ants marked
invulnerable take no simulated damage, and a founding queen still living on her body reserve pays no upkeep.

### Food

Only real food takes part: items in the colony's own food store, and food already held in the mandibles of colony
ants inside the nest or near its entrance. Nothing arrives while the colony is away and nothing is created; each
serving moves one item into an ant's or a larva's nutrition in a single step. At the start of each batch:

1. Hungry adults, those that have fasted for at least half their grace and hold little sugar, are served hungriest
   first. Each first receives sugar shared from other workers' crops (donors keep a reserve), then, if still short,
   eats one sugary food item.
2. Each caregiver, a worker that was nursing when the colony unloaded, can deliver one item per 92 ticks of
   simulated time, the average length of a live nursing round with walking and cooldown included. The item goes
   where live nursing would send it: to the first larva that can still accept it, otherwise to the queen. The queen
   takes sugar whenever she has room, but protein only while she holds less than one egg's worth, so protein goes to
   the larvae first.

### Brood

Eggs, larvae and cocoons develop at the nursery's speed (×1, ×1.5 or ×2, depending on its tier), through the same
code as live play. Eggs need the queen's care: she must be alive, active and within reach of the brood pile. Larvae
pay for growth from the sugar and protein they have been fed (4,000 sugar and 8,000 protein over the larval stage);
when that runs out they stop growing and start counting neglect. Since larvae are fed only by caregivers, larval
growth is capped by the number of caregivers and the 92-tick interval. The founding clutch draws on the queen's body
reserve, as in live play. Neglect and cocoon waiting build up as in live play, and either can kill brood. A finished
cocoon hatches into a real worker with its predetermined identity if there is room and the colony is below its cap;
otherwise it keeps waiting. New workers are dated to their simulated hatch time and keep aging from there.

### Laying

The queen can lay during catch-up under the live conditions: her founding clutch is finished, she is within reach of
the brood pile, a brood slot is free, the colony is below its adult cap, she can pay the egg cost from her own
nutrition (1,000 sugar and 2,000 protein), and the laying cadence of her hall allows it. Live laying also checks the
colony's recent food intake over a rolling window. Time away provides no such observations, so catch-up uses a stock
check instead: the food actually present (stored, carried, in crops and in larvae) must cover what the existing brood
still needs, one more egg and larva, and upkeep for all current and future adults over the next 2,400 ticks plus 25%,
and no adult may be going hungry with an empty crop. Egg identities are derived from the queen, the tick and the
slot, so the same egg is never laid twice, even across a reload.

### Stage changes

The stage rules read the nursery's loaded ticks, so they see the simulated time. The stage is re-evaluated at batch
boundaries and whenever the population or the stored food changes. A colony is promoted as soon as every requirement
is certainly met, and catastrophic losses (the queen's death, too few adults even for the stage below, a missing
chamber function) demote it at once. A shortfall that was already being timed keeps counting, and a batch ends
exactly when its 24,000-tick grace runs out, so a colony can drop a stage during catch-up. A shortfall first seen at
reload is not backdated into the absence: its clock starts at the end of the interval. Only changes that catch-up
makes itself, such as eating from the store, a birth or a death, can start a shortfall clock inside the interval.

## What it does not simulate

- Digging, building, mining and chamber upgrades. Catch-up never adds or spends building materials.
- Foraging or any other food income.
- Movement. No ant is moved or teleported and no route is replayed; ants are where they were left, with their tasks.
- Changes to the world. The nest is checked once per round and treated as fixed while the round runs.
- The exact travel and order of nursing, which the 92-tick caregiver interval stands in for.

When something is uncertain, catch-up waits instead of guessing:

- **A living member is missing.** If the queen or any living worker is not loaded, or an ant or brood record
  conflicts with the colony's records, catch-up waits. Nobody is declared dead and their place stays reserved.
- **Part of the nest is unloaded.** Catch-up waits. It grants no growth or food, and causes no starvation, neglect
  or demotion.
- **The nest is visibly damaged.** Catch-up proceeds but treats the nest as unable to support brood: ants age and
  spend their crops, nobody is fed, brood gets no care and builds up neglect, and no eggs are laid.
- **No time is owed.** The same or an earlier game time means there is nothing to do.
- **Ticks were already simulated.** The nursery remembers how far catch-up has applied and which normal step it saved
  last, and never steps again at or before either, so reloading mid-catch-up cannot apply anything twice.

## How close it stays to live play

Catch-up is designed to land close to what the same colony would have reached had it stayed loaded for the same time:

| Quantity | Design bound |
|---|---|
| Development of each brood slot; hatch times of new workers | Within 25% of a brood stage (about 2.5 minutes at normal speed) |
| Number of adults; occupied brood slots | Within one |
| Food eaten; food left in the store | Within one item per food kind |
| Each adult's stored nutrition | Within one apple's sugar and one chicken's protein, same hungry or starving state |
| Whether the queen can pay for her next egg | The same |

Two properties are exact rather than approximate. Food is conserved: every item present when catch-up starts ends up
still stored, still carried, or recorded as eaten by a particular adult or larva, for every food kind. And crop
sharing balances: the sugar given always equals the sugar received.

## Saves and compatibility

The nursery saves its catch-up state in a `Catchup` entry: the owed range still pending, how far catch-up has
applied, any unknown time, each caregiver's progress towards its next serving and a status label. Each ant saves its
record of ticked time in an `AdultSimulation` entry. Unfinished work survives a save and resumes after the next load.
Catch-up changes only the colony's biology and stage: ages, nutrition and health, brood, new eggs and workers. Apart
from the food it eats and the normal consequences of a death, it leaves alone everything an ordinary save and reload
preserves: surface-access work, item custody, claims and reservations, the ants' positions and their current tasks.
A save and reload with no time away owes nothing, so catch-up does not run at all.

Existing worlds load without conversion. A nursery or ant saved before catch-up existed sets its baseline at the
first load, and no absence is charged for earlier time. Leftover fields from development builds are ignored.

## Limitations

- It is an approximation. Results stay within the bounds above, but they do not match live play tick for tick.
- The whole colony has to be loaded. A worker that wandered into chunks that are still unloaded holds back the
  catch-up, and with it the nursery's own progress, until that worker is loaded again.
- Nothing is built, dug, mined or foraged while you are away, so a colony lives only on the food it already had.
- A long absence in a large colony may take several ticks to process. The limit is a number of events, not a time
  budget, so the tick on which a big catch-up runs can take noticeably longer.
- After a catch-up of more than 200 ticks, the live laying rule restarts its food-intake window, so normal laying
  resumes only after a fresh 2,400 ticks of observation.
- Catch-up is built around normal saves. After an abrupt crash, when the nursery and its ants may have been saved at
  different moments, consistency is not guaranteed.

## Where to look in the code

Under `src/main/java/dev/primeants/`: the coordinator is `time/ColonyCatchup.java`, the clocks are
`time/ColonyClock.java` and `time/AdultSimulation.java`, the entry point (`resumeCatchup`) and nursery side are in
`brood/BroodPile.java`, adults advance in `entity/LasiusNigerEntity.java` and `entity/AdultLife.java`, and feeding
lives in `worker/AggregateFeeding.java` and `worker/CaregiverThroughput.java`. The saved status label (for example
`pending_unknown_members` or `pending_event_bound`) says why a colony is waiting, and each round writes one
`AUTOMATIC_CATCHUP_COST` line to the server log with the events used, the ticks still pending and the time taken.
