# Saved colony clock (T01)

T01 delivers a saved observation boundary. Gameplay catch-up remains inactive: detecting absence does not apply it.
The colony's one `BroodPile` owns optional NBT long `LastSimulationGameTime`, beside its existing `Queen` UUID and nest
geometry. It uses `ServerLevel.getGameTime()`, never daylight time or wall time. A stopped server contributes no ticks.

Before a resumed ordinary step at game time C, inspect `missedIntervalBefore(level)`. For saved boundary L, both endpoints
are exclusive: `(L, C)` contains `max(0, C - L - 1)` missed ticks. The step at L already completed; the live step at C
still belongs to the ordinary ticker. Equal, reversed or negative timelines owe zero. Subtraction uses ordered
nonnegative endpoints, so even L=0/C=Long.MAX_VALUE is exact without overflow. No artificial duration cap is imposed.
A completed ordinary step rebases a reversed timeline; inspection and saving never do. A missing or invalid negative
saved timestamp initializes once at the first server-level access, whether level assignment precedes or follows tag
loading. Legacy saves receive no absence before that load, including when the nest is not operational. A new successful
founding establishes the same baseline. Failed/unowned ordinary steps do not stamp. Completion stamps at the end of
the existing nursery tick; no applied-catch-up marker exists.

The observation has constant cost independent of absence length. It neither scans colonies nor loads chunks, ticks
unloaded objects, replays ticks or changes biology. Once the next ordinary step completes, the timestamp moves on;
T01 does not retain or claim an applied absence. Later aggregation must consume its chosen interval before that step.

## Existing rule owners and clock domains

| Domain | Owner and existing clock | Relationship to the saved boundary |
|---|---|---|
| Nursery and stage shortfalls | `BroodPile.loadedTicks`; `ColonyDevelopment` evaluates at 100 nursery-loaded ticks or decisions; `StageRules` owns 24,000-tick requirement hysteresis stored by `ChamberRegistry` | Unchanged. Game-time absence does not advance loaded ticks, evaluations or unmet-since clocks. Loaded damage still wins over UNKNOWN. |
| Adult biology | `LasiusNigerEntity` age/callow clocks and `AdultLife` active, maintenance, prepaid coverage, fasting and damage clocks | Each entity owns its actual loaded ticks. The nursery timestamp proves nothing about whether a particular adult was ticking. |
| Brood | `BroodRecord` progress, nutrition, neglect and cocoon waiting; `BroodPile` care, emergence and loaded laying cadence; `BroodCapacity` slots, speed credit and hall cadence | Unchanged. Existing care, lineage, viability, capacity and physical funding rules remain authoritative. |
| Food and admission | `Nutrition` physical ingestion/spending and receipts; `FoodLimitedGrowth` nursery-loaded intake sampling and actual-stock commitments | Absence is no food credit, delivery, sample or permission to lay. UNKNOWN supply still pauses growth. |

Later aggregate work must reuse `StageRules`, `ColonyDevelopment`, `AdultLife`, `BroodPile`, `BroodRecord`,
`BroodCapacity`, `Nutrition` and `FoodLimitedGrowth`, preserving their costs, identities and clock domains.
Nursery absence does not prove every member was unticked. Later reconciliation must identify independently loaded
entities and prevent double simulation; unavailable entities remain UNKNOWN, with retained identity and reservation.

Food/starvation, laying/development, adult death, stage changes and stock aggregation are **not delivered by T01**.
Digging, building and mining will not be fast-forwarded in this version.

`ColonyClockGameTest` uses real egg-founded nurseries, the production block ticker and vanilla save/reconstruction,
including a compressed NBT disk round trip. Detached snapshots and explicit game-time fixtures cover short/long
absence, migration ordering, repeated reload, arithmetic and partial availability. These are serialization and
simulation-path tests, **not actual chunk-unload coverage**. They establish no aggregate accuracy or performance.

Recommended T02 slice: establish per-member observation/reconciliation boundaries for independently loaded adults,
with live-control and UNKNOWN identity tests, before allowing the nursery interval to drive any adult aggregation.
