# Saved clock and reload reconciliation (T01–T02)

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

## T02 pre-step boundary and lifetime

`BroodPile.serverTick` captures `ReloadObservation` immediately after its ownership guard and **before**
`loadedTicks++`, supply observation, stage evaluation, brood changes or the final `clock.completed(C)`.
The capture contains the original exclusive `(L,C)`, queen UUID, nursery position and immutable member rows.
Every row retains adult UUID, brood lineage, queen, home, reservation, availability, an immutable adult evidence
snapshot when available, that adult's own exclusive last-step interval and its reconciliation result for `(L,C)`.
The current tick C is excluded irrespective of whether that adult has already ticked at C.

The instance holds exactly one latest capture. A newly loaded instance captures at its first authorized ordinary
step, even for a zero interval; a later discontinuity replaces it. Consecutive steps retain the same immutable
capture after the nursery timestamp advances. Loading new NBT clears the transient capture and arms the boundary
again. It is neither serialized as a work queue nor labeled applied; there is no dormant backlog. Repeated
same-time reconstruction can expose the same interval, but does not apply anything.

T03's consumer must run synchronously **at this capture point**, before ordinary biology or the stamp, and establish
its own durable, idempotent application contract. Reconstructing `(L,C)` from the overwritten nursery clock is
incorrect. Read-only re-observation of a returning adult is explicit; this snapshot never polls unavailable UUIDs
or retrospectively replaces an earlier UNKNOWN row. T02 does not promise durable deferral for unavailable members.

## Adult evidence and classifications

`LasiusNigerEntity` owns version 1 `AdultSimulation` metadata, separate from `AdultLife`. The fields are `Boundary`,
`Origin` (`BIRTH`, `LEGACY_LOAD`, `REBASED`), `KnownAfter`, `LastSimulationGameTime`, `InvalidTime` and up to **32**
inclusive completed game-time `Runs`. Adjacent steps extend one run in constant work; intermittent gaps remain
explicit. On a 33rd disjoint run the oldest run is removed and `KnownAfter` advances to its end. Queries needing
earlier evidence return `UNKNOWN_EVIDENCE_BEFORE_<floor>` with **no numeric owed or simulated result**. This is
explicit loss of resolution, not silent truncation or an assumption of continuous ticking from a recent timestamp.

Within retained evidence, reconciliation intersects at most 32 ranges with `(L,C)`. Already simulated ticks are
excluded exactly; eligible ticks without a completed observation form the read-only proposed missed count. A
birth baseline clips eligibility so time before birth is never charged. Legacy load cannot establish earlier
absence: an interval reaching before its server-load baseline is unresolved. An observed adult can therefore
have a different own absence interval from the nursery, including intermittent simulation during nursery absence.

| Member condition | Capture classification and accounting |
|---|---|
| Matching living queen or registered worker, evidence covers interval | `OBSERVED` / `RESOLVED`; exact already-simulated and proposed missed ticks |
| UUID unavailable or body not observed alive | `UNKNOWN_UNAVAILABLE`; no numeric inference; original identity and occupied reservation retained |
| Observed conflicting lineage/type/home | `UNKNOWN_IDENTITY_CONFLICT`; no numeric inference or replacement |
| Known queen terminal receipt | `DEAD`; no proposed biology; existing death owner remains authoritative |
| Evidence reaches before migration/compaction/rebase floor | Explicit `UNKNOWN_EVIDENCE_BEFORE_*`; no guessed count |
| Negative completed game-time domain | Persisted `InvalidTime` / `UNKNOWN_NEGATIVE_TIME_DOMAIN`; next nonnegative completion starts an explicit new evidence domain |
| Equal, reversed or negative exclusive nursery endpoints | Zero by `ColonyClock` arithmetic; no invented duration |

The queen's identity reserves its usual slot until actual terminal evidence. Workers use the existing
`ColonyMembers` registry; absence never records a death, frees capacity or re-emerges consumed brood. Its derived
queen-to-member index makes capture work colony-local; UUID lookup does not load chunks. Tombstones and the registry's
existing lifecycle remain unchanged. Work is independent of missed duration, with no per-missed-tick replay,
world-wide entity scan, production ticket or polling loop. A capture contains only this colony's living reservations.

## Completion, migration and ordering

New adult construction establishes a birth observation baseline without charging earlier time. Emergence keeps
the existing UUID/brood/queen/home and birth-selected policies. Entity load supplies server-level game time;
missing observation metadata initializes `LEGACY_LOAD` there, without changing elapsed age, health, nutrition,
fasting, coverage, cargo or selected durations. Normal serialization preserves all ranges/floors. Inspection/save
does not append a run. Client inspection is not server biological evidence.

Only the existing biological branch in `LasiusNigerEntity.tick` observes a completed step. Its early fatal return
still completes an observation. `die` records that terminal step before the existing `AdultHistory` receipt,
and the final completion is idempotent. External death does not pretend a biological step occurred. Exceptions
are not marked completed by the normal completion path. Duplicate game-time observations add no evidence;
nonnegative reversal explicitly rebases the evidence domain. Negative completions persist uncertainty without
inventing a nonnegative timestamp. None of these hooks changes the number or contents of ordinary biological steps.

The adult also retains one transient immutable `Boundary` before its first resumed biological step, so its own
saved interval remains inspectable after it stamps C. Nursery capture can happen before or after that ordinary
adult step: range intersection excludes C in both orderings.

## Acceptance scope and next slice

T02 currently has compilation, six exact reconciliation unit cases and existing unit/model/guard verification.
Its nine new permanent GameTests are compiled but **server acceptance is pending**: launches were held while the
parallel Stage-1 campaign occupied three servers, under the safest global reading of T02's two-server limit.
The T02 settling packet records this partial result; genuine chunk eviction/reload is not yet verified.

`ReconciliationGameTest` labels timestamp, detached entity/nursery and manual removal fixtures explicitly. They
exercise actual production tick/save/reconstruction methods but are not chunk eviction. `ReconciliationChunkGameTest`
uses a controlled-origin egg-founded remote colony outside helper retention, a uniquely registered fixture ticket,
normal server checkpoint and ticket removal, actual chunk-unload event/unavailability and native entity disappearance,
a measured 40-game-tick absent interval with an independently ticking live control, then normal ticket/chunk reload.
The fixture releases only its own ticket, including on failure. Its recorded pre-step intervals/identities, not a
requested ticket removal, settle genuine lifecycle coverage when the test passes. This is normal running-server
chunk persistence, not cold restart or abrupt-crash durability.

T02 leaves all aggregation inactive. T03 should first add a finite adult age/callow and `AdultLife` maintenance
aggregate slice for **resolved, observed members only**, deriving the adult's eligible interval from retained
evidence and excluding completed ordinary ticks. It must reuse birth policies and prepaid coverage, preserve
UNKNOWN reservations, add durable application idempotence at the pre-step boundary, and prove equivalence to
ordinary biology including early death/custody. Feeding, laying, brood and stage/stock aggregation remain deferred;
digging/building/mining are not fast-forwarded. T03 is also due the **complete long checkpoint**, including the
unresolved historical Great case. Reconciliation success alone establishes neither lifecycle accuracy nor the
final catch-up cost limit.
