# Stage 1 design: colony stages, dug chambers and materials (as built in T01–T04)

A colony's stage comes only from live bodies and blocks (GDD v2 §1, decision 23); the stage cap is the only adult limit.
Code: `colony/` (stages), `founding/` (plan, digging), `worker/` (stores, hauling).

## Model

- **`ChamberRegistry`** (`SavedData`, per queen): chambers (bounds, functions, tier 1–3, markers) and the last stage.
  The 0.1.0 founding room counts as built; a dug chamber registers once.
- **`ColonyDevelopment`** confirms each function from the nest's live checks (`NestPlan.nurseryFindings`), the
  chamber's cells and its marker. **A fault seen in loaded blocks beats unavailable terrain** (`Findings`): ABSENT if
  anything loaded is wrong, UNKNOWN if only a needed cell or body is unavailable, else CONFIRMED.
- **`StageRules`** promotes only to a stage certainly met and demotes only below one its unknowns cannot hold.

| Stage | Requirements (cumulative) | Cap |
|---|---|---|
| Founding | founding chamber registered | 5 |
| Young | 5+ adults (queen + 4 workers), nursery, food store | 30 |
| Mature | 25+ adults, queen's hall, material store, 4+ food, 16+ clay | 60 |
| Great | 50+ adults, all four functions at tier 2+, 32+ stone | 120 |

## Digging (`NestBlueprint`, `DigJob`, `ChamberExcavation`)

- **Plan.** Two-high rooms compiled into a validated dig queue, never blocks: 10 blocks out, 3 deep, ≤ 32 cells.
  Widening, then store, then hall; one builder at a time, leaving 2 caregivers; live colony soil only; exact soil
  accounting; a replaced or player-placed planned cell is never dug.
- **Material store.** A 3×3 room behind the founding back wall (24 cells); its mirror room stays free.
- **Queen's hall.** The widening side the 0.1.0 widening did not take: 3×2 columns, two high, 12 cells, dug from the
  queen's chamber. Eggs develop only within 2.25 blocks of the queen, and her readiness needs her inside her room, so
  the hall enlarges her own chamber and neither she nor the pile moves; its cells are authorized openings of that
  chamber. It registers once as its own chamber (`queens_hall`, tier 1) without a marker block: it counts while its
  cells and the habitat are intact and the living queen is settled inside; an unloaded queen is unknown.
- **Scan rule (`DugSpace`).** Each dug cell must be the colony's opening and each face solid dry ground, a dug cell or
  the chamber it opens into; every cell and face is read by its own chunk's availability, so an unloaded dug cell
  never hides a broken loaded face. Brood care scans a job's dug cells and the stage a room's cells with this one scan.

## Materials (`MaterialUnits`, `MaterialStore`, `WorkerTasks`)

- **Units.** One item is one unit: clay ball (clay); cobblestone, stone (stone); gravel; sand; coal, raw copper, raw
  iron (ore, for stage 2). Nothing else.
- **Hauling.** Only when its trip found no dropped or native food, a forager takes one unit from items on the ground in
  its search area and carries it in its mandibles to the colony's confirmed store. A missing, full or unconfirmed store
  leaves it in the mandibles; a dead carrier's unit goes to custody once.
- **Capacity.** Tier 1 holds 32 units, 16 kept for clay, so no other stock crowds out Mature's clay. The block shows a
  clay heap and a heap of the rest, four units a level, textured by its main item.
- **Stage input.** Clay and stone count only in confirmed stores; an unknown store adds 32 clay or 16 stone to possible.

## Limits and the T05–T06 proposal

- **Brood capacity.** A 3-slot nursery holds each brood 36,000 ticks (3 stages × 12,000): one adult per 12,000 ticks.
  Workers die at 144,000, so it sustains ~12 (4 per slot); Mature, Great and 120 need 6, 13 and 30 slots. GameTests run
  brood ×100 with real lifespans and hide this. The T22 growth gate (income ≥ 125 % of upkeep, no adult fasting 6,000+
  ticks) also stops laying between feeding waves.
- **Proposal.** Sustained adults = 4 × slots × speed: nursery tier 2 at 6 slots ×1.5 (36), tier 3 at 10 slots ×2 (80),
  a second nursery in the store's mirror room toward 120. Hall tiers shorten the 1,200-tick laying cadence once it binds
  (above ~120). Verify in real ticks at brood ×1 that a fed colony keeps laying.
- **Mound.** It holds twice the surface deposits (44–80 cells); founding, widening, store and hall need 72. On overworld
  noise 20 % of founding sites admit the store with room for its soil, 0.9 % both rooms (T04 survey). Growing it is
  surface-structure work.
- **Tiers** (T05–T06): confirmed tier = min(saved, live wall scan); stone in tier-3 walls counts with stored stone.
