# Stage 1 design: colony stages and dug chambers (as built in T01–T03)

A colony's stage comes only from live bodies and blocks (GDD v2 §1, decision 23), and the stage cap is the only adult
limit. Code: `colony/` (stages), `founding/` (plan, digging), `worker/MaterialStore`.

## Model

- **`ChamberRegistry`** (`SavedData`, per queen): nest anchor, chambers (bounds, functions, tier 1–3, a marker per
  function) and the last evaluated stage. The 0.1.0 founding room counts as built; a dug chamber registers once its
  marker block is set up.
- **`ColonyDevelopment`** runs every check of the connected nest (`NestPlan.nurseryFindings`: stairs, founding chamber,
  widening, dug spaces) plus a dug chamber's own cells. **A fault seen in loaded blocks beats unavailable terrain**
  (`Findings`): a function is ABSENT if anything loaded is wrong (open shell, obstructed floor, missing or foreign
  marker), UNKNOWN if only a needed cell is unavailable, else CONFIRMED. Brood care reads the same findings.
- **`StageRules`** promotes only to a stage certainly met and demotes only below one its unknowns cannot support;
  `missing(target)` is cumulative. **`BroodPile`** applies `min(stage cap, birth bound 4..120)`.

| Stage | Requirements (cumulative) | Cap |
|---|---|---|
| Founding | founding chamber registered | 5 |
| Young | 5+ adults (queen + 4 workers), nursery, food store | 30 |
| Mature | 25+ adults, queen's hall, material store, 4+ food, 16+ clay | 60 |
| Great | 50+ adults, all four functions at tier 2+, 32+ stone | 120 |

Each next threshold fits under the current cap, also after a regression (checked at class load and in unit tests).

## Nest plan (`NestBlueprint`)

- **Format.** A Java record table of semantic primitives in the founding nest's frame (forward, side, dy): two-high
  `Room`s (box, function, marker) and `Placement`s (passage from the founding chamber, room, rooms kept free).
  `compile` validates a placement and emits a **dig queue**, never blocks: passage then room, each column floor first.
- **Material store.** A 3×3 room at the founding depth through the centre of the founding back wall, 24 cells.
  Placements `left`, then `right` (forward 7..9, sides ∓2..4); each keeps the other room for the queen's hall.
- **Bounds.** Planned and shell cells stay within 10 blocks of the entrance forward or sideways, from 3 below its level
  up to it; at most 32 cells per queue. Unit tests check connectivity, bounds, no overlap with stairs, plugs, founding
  chamber, either widening side or deposits, a closed shell except the declared opening, and the covered order.

## Digging (`DigJob`, `ChamberExcavation`)

- **One machinery.** The 0.1.0 widening and plan chambers share `DigJob` and the `DIG`/`DIG_OUT` phases. Removed =
  carried + deposited + released at every point; soil goes to the supported mound, or to custody if a builder dies.
- **Trigger.** Young, `material_store` missing and not unknown, a real worker that leaves 2 caregivers, the widening
  done, one builder at a time. Placements are tried every 100 ticks: observed ineligible terrain (not witnessed natural
  or colony-prepared soil, fluid, too little mound) rejects one; an unloaded cell waits.
- **Openings.** Dug cells recorded as the colony's openings are authorized in the founding shell; only the dug space's
  faces join the habitat, so pending cells cannot harm brood care.
- **Stop.** A replaced, player-placed or ineligible planned cell is never removed: the job stops after the builder
  delivers its soil (an undug job re-plans to the next placement).
- **Store.** The builder sets up the owned `MaterialStore` (16 units, empty for now) on the floor; the chamber registers
  at tier 1 and is confirmed by the same precedence rule. Breaking the store alarms the colony.

## Extending it

- **Queen's hall:** table rows on the reserved side, a marker block, an `ownedMarker` case.
- **Tiers by wall material:** a shell scan (clay or resin 2, dressed stone 3); confirmed tier = min(saved, wall).
- **Material stocks:** sum confirmed stores; an unloaded store adds its capacity to *possible* only. Count stone laid
  in the colony's own tier-3 walls with stored stone, so a store-to-wall move cannot demote a Great colony.
