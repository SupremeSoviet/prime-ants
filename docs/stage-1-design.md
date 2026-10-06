# Stage 1 design: colony stages (as built in T01–T02)

A colony's stage comes only from live bodies and blocks (GDD v2 §1, decision 23), and the stage cap is the only adult
limit. Code: `src/main/java/dev/primeants/colony/`.

## Model

- **`ChamberRegistry`** (`SavedData`, one entry per queen UUID) stores the nest anchor, the chambers and the last
  evaluated stage. A chamber has bounds, functions (nursery, food store, material store, queen's hall), a tier 1–3 and
  one marker block per function. A 0.1.0 founding room is registered as built: tier 1, markers at its pile and cache, no
  terrain change.
- **`ColonyDevelopment`** reads the world. A function counts only while its owned marker (`BroodPile.ownedBy`,
  `NestCache.ownedBy`) sits in an open, enclosed room (`NestPlan.nurseryProblem`). A check that fails only because a
  cell's chunk is unavailable (`*_chunk_unavailable`, the entrance route included) leaves the function unknown; only
  loaded blocks observed wrong count as loss. An adult counts only as a found living body; a failed lookup is unknown.
- **`StageRules`** is one rule table over (known, possible) bounds. It promotes only to a stage that is certainly met
  and demotes only below one that its unknowns could not support. So unloaded members change nothing, and a stale saved
  stage is recomputed once everything is loaded. `missing(target)` is cumulative: every unconfirmed requirement of the
  target and each lower stage, lowest first. Each transition is logged with its evidence.
- **`BroodPile`** applies `min(stage cap, bound)` to laying and emergence.
  - The bound is the birth-selected `prime_ants.colonyAdultCapacity`: default 120, valid 4..120. The 0.1.0 fields
    migrate once: 30 → 120, and 4..29 are kept.
  - Evaluation runs at those decisions after a birth or death, and at least every 100 loaded nursery ticks.

## Rule table

Counts include the queen. Brood reserves cap space as in 0.1.0. The stock numbers are starting values.

| Stage | Requirements (cumulative) | Cap |
|---|---|---|
| Founding | founding chamber registered | 5 |
| Young | 5+ adults (queen + 4 workers), nursery, food store | 30 |
| Mature | 25+ adults, queen's hall, material store, 4+ food, 16+ clay | 60 |
| Great | 50+ adults, all four functions at tier 2+, 32+ stone | 120 |

Each next threshold fits under the current cap (5 ≤ 5, 25 ≤ 30, 50 ≤ 60), including after a regression; this is checked
at class load and in unit tests. Mature and Great stay unreachable until later turns, and the evaluator lists what they
lack.

## Extending it

- **Dug chambers.** Register each finished nest-plan chamber. Generalize `nurseryProblem` into a per-chamber rule in
  `ColonyDevelopment.confirm`, and add queen's-hall and material-store markers to `ownedMarker`.
- **Tiers by wall material.** Replace `EARTHEN` with a shell scan: packed clay or resin counts as 2, dressed stone as 3.
  The confirmed tier is min(saved tier, wall tier).
- **Material stocks.** Sum confirmed stores. An unloaded store adds its capacity to *possible* only.
- **Trap.** Stone spent on tier-3 walls must not, by itself, demote a Great colony. Count stored stone plus the stone
  laid in the colony's own confirmed tier-3 walls, so a store-to-wall move is neutral. Treat clay and tier-2 walls the
  same way for Mature.
