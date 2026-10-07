# Stage 1 design: colony stages, chambers, materials and upgrades (as built in T01–T05)

A colony's stage comes only from live bodies and blocks (GDD v2 §1, decision 23); the stage cap is the only adult limit.
Code: `colony/` (stages), `founding/` (plan, digging, walls, upgrades), `worker/` (stores, hauling), `brood/` (capacity).

## Model

- **`ChamberRegistry`** (`SavedData`, per queen): chambers (bounds, functions, claimed tier, markers) and the last stage.
- **`ColonyDevelopment`** confirms each function from live checks of the nest, the chamber's cells and its marker; a
  fault seen in loaded blocks beats unavailable terrain (`Findings`). A confirmed chamber's tier is read from its walls.
- **`StageRules`** promotes only to a stage certainly met and demotes only below one its unknowns cannot hold.

| Stage | Requirements (cumulative) | Cap |
|---|---|---|
| Founding | founding chamber registered | 5 |
| Young | 5+ adults (queen + 4 workers), nursery, food store | 30 |
| Mature | 25+ adults, queen's hall, material store, 4+ food, 16+ clay | 60 |
| Great | 50+ adults, all four functions at tier 2+, 32+ stone | 120 |

## Digging and materials (T03–T04)

- **Plan** (`NestBlueprint`): two-high rooms compiled into dig queues: widening, a 3×3 material store behind the
  founding chamber, then the queen's hall on the free widening side, enlarging her own chamber (she and the pile stay).
  One builder at a time, 2 caregivers kept, exact soil accounting; a player's cell is never dug.
- **Hauling**: one unit per trip (clay ball; cobblestone, stone; gravel; sand; coal, raw copper, raw iron) into the
  confirmed store; food first, both when the trip searches and again at the moment a material would be picked up.
- **Store**: 32 units, 16 kept for clay. It shows a clay heap (0–8 levels), an other-stock heap (0–4), four units a
  level, and a lump per other material present: totals 8 apart or different materials never look alike (720 states).

## Walls, tiers and upgrade work (T05: `NestWalls`, `ChamberUpgrade`)

- **Walls**: the horizontal faces of a chamber's open cells that the plan never opens (stairs, plugs, widening sides,
  any placement's dug or reserved cells); not floor (may be witnessed stone) or roof (the surface layer). The founding
  chamber, home of the nursery, has 8: front and back walls, two high.
- **Tier** = the lowest wall tier, read live: anything solid 1; the colony's own packed clay or resin masonry 2; its own
  nest-cut stone 3 (a `ColonyTerrain` "built" record on that exact block). A player's block never raises a tier. The new
  blocks have no loot; a colony-built cell is an owned component for `ColonyAlarm`.
- **Trigger**: the stage unlocks the tier (Mature → 2), the nursery chamber is confirmed below it, the confirmed store
  holds a cell's clay, digs are done, no other builder works (shared claim rule), 2 caregivers remain. Nursery first.
- **Work**: the builder takes 1 clay from the store, carries it and rams it into the next wall cell's own earth (1 clay
  a cell, no soil moves). A cell no longer natural or colony earth stops the job and the unit goes back; a short store
  pauses it. Exact: taken = carried + built + custody; a dead builder's unit leaves once through custody. Clay in a
  confirmed chamber's own walls and in the builder's mandibles keeps counting toward Mature's 16. No repair yet.

## Brood capacity (T05, `BroodCapacity`)

Workers sustained = slots × speed × 144,000 / 36,000 (lifespan over brood time), at most 144,000 / 1,200 eggs (the
hall's laying limit, T06). Each stage can build enough for its next threshold (checked at class load and by unit test):

| Built by | Cause | Slots | Speed | Workers | Needs |
|---|---|---|---|---|---|
| Founding | founding chamber, earth | 3 | ×1 | 12 | 5 |
| Young | + queen's hall (+4) | 7 | ×1 | 28 | 25 |
| Mature | + tier-2 nursery walls (+3) | 10 | ×1.5 | 60 | 50 |
| Great | + tier-3 walls (+8, T06) | 15 | ×2 | 120 | 120 |

One pile, in the queen's reach, reads the hall and nursery tier from its last evaluation (every 100 loaded ticks).
Speed adds steps per tick to running and new stages (a saved credit). Brood past three slots shows as one heap.

## Limits carried forward

- **Mound room (T07)**: both rooms passed terrain at 759 of 1,809 surveyed placements, only 16 with mound room for
  their soil; the enlarged fixtures prove the rooms possible, not that ordinary worlds get them.
- **Clay before Mature (T07)**: Mature needs 16 clay and miners come only at Mature; T07's mining must close this.
- **Hall entrance unload (known)**: the hall reads as lost when only its entrance chunk is unloaded
  (`ColonyPlugs.opened` is false for an unloaded cell). No save or play damage.
- **Food at Mature (T06)**: Mature's 4 food sit in a 6-slot cache that an eating wave drains (a brief demotion), and a
  cache full of chickens blocks apples and starves adults. GameTests run brood ×100 with real lifespans.
