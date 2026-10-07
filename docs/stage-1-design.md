# Stage 1 design: colony stages, chambers, mound, materials and upgrades (as built in T01–T07)

A colony's stage comes only from live bodies and blocks (GDD v2 §1, decision 23); the stage cap is the only adult limit.
Code: `colony/` (stages), `founding/` (plan, digging, mound, walls, upgrades), `worker/` (food cache, foragers, stores,
hauling), `brood/` (capacity).

## Model

- **`ChamberRegistry`** (`SavedData`, per queen): chambers (bounds, functions, claimed tier, markers), the last stage and
  its unmet-since clocks (T07).
- **`ColonyDevelopment`** confirms each function from live checks of the nest, the chamber's cells and its marker; a
  fault seen in loaded blocks beats unavailable terrain (`Findings`). A confirmed chamber's tier is read from its walls.
- **`StageRules`** promotes at once to a stage certainly met, never demotes on unknowns, and drops a held stage only by
  the owner's hysteresis below.

| Stage | Requirements (cumulative) | Cap |
|---|---|---|
| Founding | founding chamber registered | 5 |
| Young | 5+ adults (queen + 4 workers), nursery, food store | 30 |
| Mature | 25+ adults, queen's hall, material store, 4+ food, 16+ clay | 60 |
| Great | 50+ adults, all four functions at tier 2+, 32+ stone | 120 |

## Stage hysteresis (T07, the owner's decision of 2026-10-07)

A held stage drops only once one of its requirements has been certainly unmet for **24,000 loaded ticks** in a row, on
that requirement's own clock (the nursery's loaded ticks, saved in the registry, surviving reloads). Waiting: food, clay,
stone; a chamber still confirmed below a needed tier; adults below the held stage's threshold but not below the lower
stage's. At once: the queen observed dead; a function no confirmed or unknown chamber holds (breached shell, missing or
foreign marker, the hall without its living queen); adults below the lower stage's threshold, which drop the colony to
the stage its adults support. Unknowns never demote and run no clock. Promotion stays immediate.

## Digging, the mound and materials (T03–T07)

- **Plan** (`NestBlueprint`): widening, a 3×3 material store behind the founding chamber, then the queen's hall on the
  free widening side. One builder at a time, 2 caregivers kept, exact soil accounting; a player's cell is never dug.
- **Mound** (T06, `NestMound`, `MoundSoil`): room soil goes on the colony's stage mound, the old blueprint's tiers compiled
  into ordered cells, two lobes behind the entrance either side of the lane: Young 3 layers, 214 cells; Mature 5 layers,
  798 cells, containing Young. A cell takes soil if it is air or a witnessed short plant (buried, no drops) on natural
  ground or the colony's mound soil; nothing else is covered or removed. **Plug soil (T07)**: when the founding deposit
  list is full, the nest-opening forager lays the two plug units on the stage plan (Young's while the colony is Founding).
- **Survey (T07, frozen set)**: T04's 49 regions and 17,689 columns, GameTest seed 0; 1,846 founding sites, their
  identities saved by the turn. With the production rules, laid virtually in order (the queen's 22, both plug units, the
  widening's 12): 1,842 nests open (337 of them through the plug-soil rule); 4 cannot, as in 0.1.0 (a free founding
  deposit with no walkable stand beside it). Of the 1,842, 764 pass the store and hall terrain checks, and all 764 fit
  the store's 24 and the hall's 12 on Young's free cells; 763 counting only cells with a supported builder stand in reach
  and surviving vanilla support. Not checked: walking routes and the dig timeline.
- **Hauling** and **store** (T04–T05): one unit per trip, food first; 32 units, 16 kept for clay.

## Food cache and foragers (T06–T07)

- **Shares** (T06, `FoodShares`): 6 slots, at most 4 of each kind; foragers take dropped food only while its kind has
  room. **Saved caches (T07)**: a cache holding more of one kind than its share sheds the excess through transfer custody
  onto the chamber floor when the other kind arrives; every unit stays in the world.
- **Foragers (T07, `Foragers`)**: one per ten living workers, at least one: exactly one below 20 workers (0.1.0's test
  colonies), two at 20–29, five at Mature's cap, eleven at Great's. A further claim leaves 2 caregivers and, without a
  builder, the builder slot; a further forager the colony no longer keeps returns to care after its trip. Claims are saved
  with the queen; each forager carries its own cargo.

## Walls, upgrades and brood capacity (T05–T06: `NestWalls`, `ChamberUpgrade`, `BroodCapacity`)

- **Tier** = the lowest tier of a chamber's walls, read live: solid 1, the colony's packed clay 2, its nest-cut stone 3.
- **Work**: Mature unlocks tier 2, nursery first; a builder rams 1 store clay into each wall cell. Exact: taken = carried
  + built + released; units still in transfer custody are a separate figure (shown pending through a reload in T07).
- **Brood**: workers sustained = slots × speed × 4, at most 120. Founding 3 slots; + hall 4; + tier-2 walls 3 and ×1.5.

## Results and limits carried forward

- **Holding Mature (T07)**: with hysteresis and two foragers from 20 workers, four concurrent long-path colonies were
  promoted at 27,504–28,503 ticks, held Mature, rebuilt the nursery (tier 2 at 29,004–29,983) and laid at tier-2 speed by
  32,357–33,638. Their player drops the clay at 25 adults and holds apples back from then until the nursery is rebuilt:
  a satiated colony does not lay (T22), and an egg laid during the upgrade would straddle the two speeds.
- **Growth past Mature (T07)**: a colony fed on demand, with two foragers from 20 workers, became Mature and grew past 30
  adults: 31 adults at 30,270–31,474 ticks in three diagnostic runs and at 46,980 in the final build (Mature at 33,669).
- **Expanded brood (not shown, T06's 1d cut again)**: in two 60,000-tick runs a rebuilt nursery never held more than two
  brood at once (the long path's colonies three): each egg needs stored protein for a whole larva and the 6-slot cache
  holds four chickens at most, so a four-egg burst needs about 42,000 protein of headroom. A larger food store is T08's.
- **Moved to T08 (the rest of checkpoint 1)**: tier-2 upgrade work and effects for the food store, the material store and
  the queen's hall; tier 3; the Great path.
- **Clay before Mature (T08)**: without a player a colony gets no clay before Mature (miners come at Mature).
- **Hall entrance unload (known)**: the hall reads as lost while only its entrance chunk is unloaded.
