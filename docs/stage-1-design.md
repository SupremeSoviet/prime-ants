# Stage 1 design: colony stages (as built in T01)

A colony's stage comes only from live bodies and blocks (GDD v2 §1, decision 23), and the stage cap alone limits
adults. Code: `src/main/java/dev/primeants/colony/`.

## Model

- **`ChamberRegistry`** (`SavedData` `prime_ants:chamber_registry`, codec-validated) keeps one entry per queen UUID.
  - The entry holds the nest anchor, the chambers and the last evaluated stage.
  - A chamber has an id, inclusive bounds, functions (nursery, food store, material store, queen's hall), a tier 1–3 and
    one marker block per function.
  - A 0.1.0 nest is recognized as built, without terrain changes. Its `founding` room is tier 1, with markers at the
    brood pile and cache cells. It is neither a queen's hall nor a material store.
- **`ColonyDevelopment`** reads the world. A saved entry never counts on its own.
  - A function counts while its owned marker exists (`BroodPile.ownedBy`, `NestCache.ownedBy`) and
    `NestPlan.nurseryProblem` passes: open cells, enclosed shell, plugs and route. That is the predicate brood care uses.
  - Adults are the queen plus registered members. A found living body is *known*, a failed lookup or unloaded terrain is
    *unknown*, and only a recorded death removes one.
- **`StageRules`** is the one rule table, over (known, possible) bounds.
  - A colony is promoted only to a stage that is certainly met, and demoted only below one that its unknowns could not
    support. Otherwise the saved stage stands.
  - So unknowns change nothing, and a stale saved stage is recomputed once everything is loaded. Transitions are logged
    with adults, chambers, stocks, cap and missing items.
- **Cap.** `BroodPile` applies `min(stage cap, bound)` to laying and emergence.
  - The bound is the birth-selected `prime_ants.colonyAdultCapacity`: default 120, 4..120, saved as
    `ColonyAdultCapacityBound`/`AdultCapacityBound`. 0.1.0's `ColonyAdultCapacity`/`AdultCapacity` migrate: 30 → 120, and
    4..29 are kept.
  - A lower cap never touches existing adults.
  - Evaluation runs at laying and emergence decisions, after a birth or death (`ColonyMembers.revision`) or a replaced
    registry, and at least every 100 loaded nursery ticks. It never runs on every tick.

## Rule table

All counts include the queen. Brood reserves cap space as in 0.1.0: laying is refused at workers + brood ≥ cap − 1. The
stock numbers are starting values.

| Stage | Requirements (cumulative) | Cap |
|---|---|---|
| Founding | founding chamber registered (queen settled, first clutch laid) | 5 |
| Young | 5+ adults (queen + 4 workers), nursery, food store | 30 |
| Mature | 25+ adults, queen's hall, material store, 4+ food units, 16+ clay units | 60 |
| Great | 50+ adults; nursery, food store, material store, queen's hall all tier 2+; 32+ stone units | 120 |

Each next threshold fits under the current cap (5 ≤ 5, 25 ≤ 30, 50 ≤ 60), including straight after a regression. This
is checked at class load and in unit tests. An operator bound below a threshold, such as 4, deliberately holds that
stage. Mature and Great stay unreachable until halls, material stores and tier-2 work exist; until then the evaluator
lists what they are missing.

## How later turns extend it

- **Chambers dug from nest plans.** Register each completed chamber with its bounds, functions and markers. Generalize
  `nurseryProblem` into a per-chamber live rule in `ColonyDevelopment.confirm`: cells open or owned markers, shell solid
  apart from declared openings. Add the material-store and queen's-hall marker blocks to `ownedMarker`.
- **Tiers by wall material.** Replace the fixed `EARTHEN` with a shell scan: the live tier is the lowest wall tier
  present (packed clay or resin = 2, dressed nest stone = 3). The confirmed tier is min(saved tier, live wall tier), so
  saved upgrade work must still be standing.
- **Material stocks.** Sum counted units in confirmed material stores. An unloaded store adds its capacity to *possible*
  only, as an unloaded cache adds its 6 food units today.
- **Trap.** Spending stored stone on tier-3 walls must not, by itself, demote a Great colony. Count stone as the stored
  units plus the stone blocks laid in the colony's own confirmed tier-3 walls, so moving stone from store to wall leaves
  the input unchanged. Treat clay laid in tier-2 walls the same way for Mature.
- Surface structures and world-generated colonies register through the same registry and are recomputed from live
  state.
