# Stage 1 design: colony stages, chambers, mound, materials and upgrades (as built in T01–T06)

A colony's stage comes only from live bodies and blocks (GDD v2 §1, decision 23); the stage cap is the only adult limit.
Code: `colony/` (stages), `founding/` (plan, digging, mound, walls, upgrades), `worker/` (food cache, stores, hauling),
`brood/` (capacity).

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

## Digging, the mound and materials (T03–T06)

- **Plan** (`NestBlueprint`): two-high rooms compiled into dig queues: widening, a 3×3 material store behind the founding
  chamber, then the queen's hall on the free widening side. One builder at a time, 2 caregivers kept, exact soil
  accounting (removed = carried + on the mound + released + custody); a player's cell is never dug.
- **Mound** (T06, `NestMound`, `MoundSoil`): room soil goes on the colony's current-stage mound, a declarative surface plan
  of the old blueprint's tiers (stacked elliptical truncated cones) compiled into ordered deposit cells: two lobes behind
  the entrance, either side of the approach lane, each growing as a dome from its centre, a cell never before the one
  beneath it. Young: 3 layers, 15 × 8 columns, 214 cells. Mature: 5 layers, 23 × 13 columns, 798 cells, containing
  Young; Great keeps Mature's. The lane, the exterior standing spot and the stairs stay clear. The 0.1.0 founding
  deposits and widening keep their own lists (inside Young's lobes).
- **Cells**: each column sits on its own ground (the highest witnessed natural soil or stone within 2 of the entrance's
  level). A cell takes soil if it is air or a witnessed short native plant (buried, no drops) resting on natural ground
  or the colony's own mound soil, and vanilla support survives. Fluids, players' blocks, block entities, logs, leaves,
  crops and anything else stop their column beneath them; nothing is covered or removed. A room is planned only if the
  plan's free cells hold its soil; a builder with nowhere to lay keeps its soil and waits. Mound soil is the colony's own
  nest soil (`ColonyTerrain` mound records), a counted stock for structure work; regression never removes it.
- **Survey** (T04's regions, columns and site test; GameTest seed 0): 1,809 founding sites; at all 753 placements where
  store and hall pass terrain (100%), the Young plan's free cells hold their 24 + 12 units after the founding's 22 and
  the widening's 12 are laid where 0.1.0 lays them (T04's deposit rule: 15). Two reruns (1,845 and 1,833 sites, as
  witness records vary): 100% too. The land costs about 6.5% of Young's cells.
- **Hauling** and **store** (T04–T05): one unit per trip into the confirmed store, food first; 32 units, 16 kept for
  clay; clay heap 0–8 and other stock 0–4 levels plus a lump per other material (720 states).

## Food cache (T06, `FoodShares`)

The 6-slot cache keeps two slots for each kind: at most 4 sugar units (apples, berries, nectar) and 4 protein units
(chicken, rotten flesh, prey). Foragers pick up dropped food only while its kind is below its share (at search, before a
material leaves the ground, at pickup); at a full cache they wait as in 0.1.0. Earlier saves load as they were. A cache
full of chickens still takes apples, and four held chickens keep Mature's 4 food through hunger waves, which eat apples.

## Walls, upgrades and brood capacity (T05: `NestWalls`, `ChamberUpgrade`, `BroodCapacity`)

- **Tier** = the lowest tier of a chamber's walls (faces the plan never opens; founding chamber: 8), read live: anything
  solid 1, the colony's own packed clay or resin masonry 2, its own nest-cut stone 3; a player's block never raises it.
- **Work**: Mature unlocks tier 2, nursery chamber first; a builder rams 1 store clay into each wall cell's own earth.
  Exact: taken = carried + built + released (released: a dead builder's units, for good); the units still in transfer
  custody now are a separate figure (T06). Wall and mandible clay count toward Mature's 16.
- **Brood**: workers sustained = slots × speed × 4, at most 120. Founding 3 slots (12); + hall 4 (28); + tier-2 walls 3
  slots and ×1.5 (60); tier 3 (T07) 15 × 2 (120). Brood past three slots shows as one heap.

## Results and limits carried forward

- **Holding Mature (T06, not met)**: none of six long-path copies held Mature from promotion to the tier-2 egg. Near 25
  adults one forager delivers about a unit per 300 ticks; adults eat apples as they arrive and larvae eat chickens, so
  food sits at 2-3, promotion comes as a delivery reaches 4, and the next meal undoes it within 100-300 ticks, before any
  upgrade. One copy promoted while satiated held 18,600 ticks on four cached chickens but laid no egg (T22 gate).
- **Moved to T07**: tier-2 upgrades of the material store and the queen's hall with their effects (store capacity, hall
  laying limit); the food store's capacity effect; tier 3; the Great path; more foragers scaling with colony size.
- **Clay before Mature (T08)**: without a player a colony gets no clay before Mature (miners come at Mature).
- **Hall entrance unload (known)**: the hall reads as lost while only its entrance chunk is unloaded.
- **Plug soil at tight sites (found in T06, 0.1.0 behaviour)**: 351 of 1,833 surveyed sites (19%) have only 22 or 23
  surface deposits; after the queen's 22, the opening forager cannot lay both plug units on the 0.1.0 list, waits with
  its soil, and the nest never opens. Owner question (route plug soil onto the stage mound?).
