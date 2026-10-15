# Catch-up acceptance: catchup4 T01

**NOT_READY.** The final original growth control exceeds the authorized 240-step fallback (**264**), and actor-first cargo still fails eventual consumption of the selected physical unit. Final affected coverage: **35/37 pass**, two failures, zero errors/skips, no missing/duplicate/unexpected/stale identities. The explicit stop condition ends further implementation and broader validation. No master-red exception is claimed.

Merged/tested local master: `14ef11f68a4f55c87ad2b8725ebc748f6f5d8573`. Initial catchup was clean at `934c7496ad0276a1fe8d8dd4d32cccc76c3ffd3f`; `git merge master` exited 0 (already up to date), no conflicts. Master was rechecked unchanged after the final measurement. The external catchup4/T01 packet records final commit/source correspondence.

## Home-domain eligibility

`AggregateHomeDomain` defines occupied **feet cells**, without a mouth-distance or line-of-sight test. The confirmed original chamber is forward 3..5, side -1..1, depth -2; the stairs are forward 0..2 at depth -forward. Exterior membership is confined to forward -5..-1, side -4..4, candidate feet heights 0..2 relative to the entrance. Only loaded, dry, supported, two-high walkable exterior cells connected to the excavated entrance stair by adjacent columns with a height difference <=1 are admitted. This is a bounded geometry flood fill, with no actor route, trip, travel cost or saved destination. It supports uneven exterior heights and the colony's physical mound. The excavated entrance, rather than its unsupported above cell, seeds connectivity.

Expansion coverage consists of the colony's verified `NestExpansion.circulationSpace` and completed, still owned, walkable two-high cells from its `ChamberExcavation` jobs. Each job must match this nursery's entrance and direction. Pending/unverified space and arbitrary world locations confer no membership. Nursery CLEAR Findings, real actor lookup, alive/enabled state, colony identity and registry/home membership remain required by the aggregate call path. UNKNOWN habitat defers application. Physical cache ownership/component confirmation remains separate.

Callow and defending members can receive meals. These statuses grant no nursing authority: nursing still requires the unchanged ordinary `caregiverWithValidatedHabitat` authorization. Only missing adult segments enter distribution; observed ordinary intervals earn no aggregate food or throughput. Hungry adults retain fasting-descending and UUID tie priorities. Existing actual crop surplus uses the 1000 reserve, 1500 portion and exact balanced given/received receipts. Physical cache/hand units are debited once. Ordinary nursing recipient selection retains accepting larvae before the queen, including larval protein. Ordinary controllers, movement, crop sharing, biology, StageRules, costs and thresholds were not edited.

## Measured throughput

The frozen interval is **92 ticks**: `(731 + 824) / (8 + 9) = 1555 / 17 = 91.4705882353`, rounded upward. The two native calibration windows are [83,1934] and [103,1954], 1851 ordinary steps each, with three caregiver identities each. There were 11 and 12 successful physical feeding completions; excluding the first completion per caregiver leaves 8 and 9 full cycles. First-completion startup latency and the final incomplete tail are censored separately for each caregiver. All intervening navigation, selection and cooldown ticks remain in each consecutive completion window. It is not colony wall time divided by total completions, and `FEEDING_TICKS=20` is not used as the interval.

`calibration-01` was retained as a demand-limited diagnostic: 1707/5 and 1899/5 included roughly 800-tick no-demand gaps. Before any candidate outcome evaluation, the calibration workload was changed to six paid empty larvae and twelve finite physical food units, with native controllers unchanged. The interval and derivation were frozen in `raw/frozen-service-interval.json` before implementation/outcome runs. No scarcity/growth supply, configuration, position, horizon or observation offset was changed. Later calibration observations do not refit the frozen value.

`CaregiverThroughput` persists only UUID -> earned ticks in [0,92], saturating at one service. Confirmed ordinary caregivers accrue only their missing eligible ticks. One real nursing receipt spends one caregiver's full phase, then resets it; both brood and nursing-to-queen receipts use this capacity. Adult meals keep their separate priority rule. The coordinator splits chronological advancement at the next earned boundary. With zero initial phase, the first 92 missing ticks advance without a receipt; feeding becomes available before the next missing tick's biology. Subsequent completions are separated by at least 92 earned missing ticks per caregiver. No horizon budget is front-loaded. Ordinary/ineligible segments clear that caregiver's phase, UNKNOWN recovery baselines reset phases, and unconfirmed intervals grant none. Repeated/same/older calls cannot advance a completed cursor. Phase survives save/reload and bounded continuation. The 512-event ceiling and reserved biology event remain. Legacy Service* intentions are ignored and grant nothing; ordinary controller saves are unchanged.

| Control / caregiver | Native receipt ticks | Full cycle ticks | Samples |
|---|---|---:|---:|
| demand / `1db88c22-055e-3875-81e5-b3d41ee3a9c1` | [143, 233, 324, 415] | 272 | 3 |
| demand / `5dc82d3f-d3d0-311c-ae23-33a28d4291f9` | [151, 242, 333, 424] | 273 | 3 |
| demand / `d3a49a00-689d-32dc-bd7b-8d291ad904cc` | [164, 257, 350] | 186 | 2 |
| no-demand / `18970786-3bf9-3f87-85f6-218461733a17` | [163, 253, 344, 435] | 272 | 3 |
| no-demand / `b7718d82-bf5f-301e-b375-f174d2e852ce` | [167, 258, 349, 440] | 273 | 3 |
| no-demand / `b6a197b9-7dca-3c74-bc75-9b21fbda50ba` | [188, 281, 374, 467] | 279 | 3 |

## Six scarcity outcomes

Saved inputs/order variants, 11/40/80/120/200/300/400 offsets and 400-step horizon are unchanged. Comparison remains **393 missing + seven ordinary adult steps vs 400 live steps**. The native driver still uses commonTick-before-tick via tickNonPassenger, one counter advance, refreshed newborn membership, nursery after adults, and zero aggregate application in live controls.

| Input/order | Hungry worker sugar live/catch-up | Crop receipts live/catch-up | Protein live/catch-up | Adult classes |
|---|---:|---:|---:|---|
| captured_1500 / saved | 1434/3900 | 1500/0 | 8000/8000 | all four matched, non-hungry/non-starving |
| captured_1500 / nurses-first | 1434/3900 | 1500/0 | 8000/8000 | all four matched, non-hungry/non-starving |
| captured_971 / saved | 910/3900 | 971/0 | 8000/8000 | all four matched, non-hungry/non-starving |
| captured_971 / nurses-first | 910/3900 | 971/0 | 8000/8000 | all four matched, non-hungry/non-starving |
| generated / saved | 1434/3900 | 1500/0 | 8000/8000 | all four matched, non-hungry/non-starving |
| generated / nurses-first | 910/3900 | 971/0 | 8000/8000 | all four matched, non-hungry/non-starving |

All six retain exact nutrition equations, crop given/received balance, normal reconstruction and same/older-time replay. Catch-up gives the hungry worker a real physical meal instead of matching live's donor-specific crop allocation. At offset 400 the original scarcity larva remains a larva at progress 60 in all six catch-up branches; it has the required physical 8,000 protein credit. None emerges in these preserved finite horizons. No scarcity-emergence completion is claimed; the observer supports archived receipts, and the independent growth/birth controls exercise archived/emerged brood accounting.

Physical consumption below is `live/catch-up/difference`, including zeros:

| Input/order | Apple | Berries | Chicken | Nectar v1 | Nectar v2 | Prey | Flesh |
|---|---:|---:|---:|---:|---:|---:|---:|
| captured_1500 / saved | 2/2/+0 | 1/1/+0 | 2/2/+0 | 0/0/+0 | 0/0/+0 | 0/0/+0 | 0/0/+0 |
| captured_1500 / nurses-first | 2/2/+0 | 1/1/+0 | 2/2/+0 | 0/0/+0 | 0/0/+0 | 0/0/+0 | 0/0/+0 |
| captured_971 / saved | 2/2/+0 | 1/1/+0 | 2/2/+0 | 0/0/+0 | 0/0/+0 | 0/0/+0 | 0/0/+0 |
| captured_971 / nurses-first | 2/2/+0 | 1/1/+0 | 2/2/+0 | 0/0/+0 | 0/0/+0 | 0/0/+0 | 0/0/+0 |
| generated / saved | 2/2/+0 | 1/1/+0 | 2/2/+0 | 0/0/+0 | 0/0/+0 | 0/0/+0 | 0/0/+0 |
| generated / nurses-first | 2/2/+0 | 1/1/+0 | 2/2/+0 | 0/0/+0 | 0/0/+0 | 0/0/+0 | 0/0/+0 |

## Exact ledgers

For **each of all six inputs and each branch**, initial `(stock, carried, prior ingestion, actual release) -> final` is:

| Kind | Initial | Final | Exact equation |
|---|---|---|---|
| Apple | (1,0,1,0) | (0,0,2,0) | 2=2 |
| Berries | (1,0,0,0) | (0,0,1,0) | 1=1 |
| Chicken | (1,0,1,0) | (0,0,2,0) | 2=2 |
| Nectar v1 | (0,0,0,0) | (0,0,0,0) | 0=0 |
| Nectar v2 | (0,0,0,0) | (0,0,0,0) | 0=0 |
| Prey | (0,0,0,0) | (0,0,0,0) | 0=0 |
| Flesh | (0,0,0,0) | (0,0,0,0) | 0=0 |

The two pre-boundary queen receipts are included, rather than subtracted as a synthetic balancing term. The observer counts current plus archived/emerged brood receipts through the nursery's canonical counters, once, separately from adult ingestion. Nectar v1 excludes the nursery's inclusive v2 subtotal. Actual stable worker-cargo releases and cache-share releases are inspected in custody/world destinations; these controls recorded none. Growth ledgers independently conserve 10 apples and 6 chicken units, with the other five kinds exactly zero in each branch. Cargo ledgers conserve 8 apples + 2 chicken (actor-first) and 7 apples + 2 chicken (nursery-first), with zeros for the other kinds. Every initial/final stock, cargo, receipt and actual-release operand is in the raw ledgers. This exact within-branch equation is independent of the cross-branch one-unit tolerance.

## Growth and other gates

| Control | Live | Catch-up | Absolute differences | Maximum | Primary 120 | Fallback 240 |
|---|---|---|---|---:|---|---|
| no_initial_adult_demand | [1200, 2553, 2670] | [1200, 2629, 2767] | [0, 76, 97] | 97 | PASS | PASS |
| original | [1200, 2467, 2503] | [1200, 2629, 2767] | [0, 162, 264] | 264 | FAIL | FAIL |


The final original case fails both primary and fallback. The no-initial-demand case meets primary. Other existing adult/slot, residual nutrition/hunger/starvation, cache, queen affordability, material custody/reconstruction and virtual-birth gates passed before the growth assertion. Growth nutrition and item ledgers and replay were collected before rejecting the timing outcome. Do not use the earlier 141-step result to replace the final 264-step negative.

## Cargo compatibility

The measured timing oracle now checks at `B + 2*92 + 8`, retaining the ordinary partial action and position. Operands are emitted before every comparison assertion in the changed acceptance fixtures/reference driver; individual primitive/object comparisons use `ComparisonEvidence`, preserving single evaluation and short-circuit behavior. Cargo rows explicitly expose all conjunct operands, and same/older replay runs before the final gameplay assertion.

Final **actor-first FAIL**: selected actor `55241ab3-34ae-35d8-8d1d-3495ac0115b8`, B=96, horizon=288, original/final hand both one apple, partial feeding ticks 5/5, exact same position and identity, full WorkerTask save bytes equal. Per-item custody is exact (10/10 units); larval nutrition is valid and its apple receipts increase 0->1. That apple came from another actual carrier: the initial ledger has two carried apples. The selected carrier's original apple remains physically held. No lost/duplicated receipt is shown, but the required eventual single consumption of that selected cargo is **not proved**. This failure is not waived as a fungible-item success or a master exception.

Final **nursery-first PASS**: original apple hand becomes empty, partial ticks stay 5, full controller/identity/position remain exact, single-kind recipient receipt increases by one, and seven-kind custody is exact (9/9). Both load orders pass normal reconstruction, UNKNOWN custody preservation and same/older replay before the final consumption assertion. No source-priority redesign was introduced after the 264-step stop condition.

## Coverage, replacements and recovery

All original 30 affected identities are preserved. Six new identities cover two native calibrations, zero/one/two caregiver capacity with chronological nutrition and reload/replay, and home-domain callow/defender recipients beyond every old stand's contact plus blocked chamber LOS, with outside/foreign/disabled exclusions. The existing fixed-event-bound reconstruction check is additionally selected: **37 total**. Ordinary CropPolicy identities remain selected. The registration guard changes 18->24 solely for the six added lifecycle methods.

Authorized semantic updates, without identity removal: `fixedDistributionFundsOnlyConfirmedMissingBiologyAndReplaysExactly` observes 31 funded steps after the 92 unfed ticks; the priority control waits for measured service availability; `fixedDistributionDefersUnknownForeignCacheAndUnreachableCargo` places the cargo at forward -6, distinguishing outside-domain from in-domain lack of contact; the two physical-cargo controls use the measured interval horizon and strengthen full controller/per-item/identity assertions. The growth controls report both 120 and the pre-authorized 240 fallback, without fitting the interval to growth errors.

The 28 offline harness regressions passed before calibration, focused and broad affected launches; catchup3's obsolete mixin target/summary-pipe repairs were retained. No workload infrastructure failure or summary-pipe hang recurred. Nonzero attempts below are native assertion failures, not lost reports. Routine Windows telemetry/server.properties startup warnings remain in raw logs and did not prevent native completion.

`focus-01` and `focus-02` each passed 8/9 and failed the new home-domain test. The first stand was not supported; the second was inside deposited nest soil. A one-name entrance-seed recovery still failed; the one-name diagnostic recorded exact feet/head/floor blocks, membership, identity, CLEAR habitat and ticking state. It identified both the entrance seed and the mound-height issue. `domain-recovery-02` passed after using the supported excavated stair and an observed supported height. The full affected run began only after this targeted proof. Exploratory read-only path misses for `worker/NestExpansion.java` and `StageFoodObservations.java` were corrected to their actual founding/clock-observer locations.

`affected-01` passed 37/37 and recorded growth deviations 141/79. `fast-01` then passed 76 server + 124 unit + 26 model cases. A final home/job-geometry guard and explicit all-old-stand/LOS observer were added afterward, so that fast result is **not acceptance of the final executable**. The required refreshed affected run produced 35/37, with growth 264/97 and the actor-first cargo red. Earlier positives are retained but do not supersede this final negative.

## Acceptance boundary

Final frozen run: `6682c727-d7a2-4f7e-b7d1-406b960d8ae6`, native Gradle exit **1**, **151.860 s**. All 37 selected identities executed exactly once. All 1,179 frozen executable/runtime input hashes match the final working source/build inputs before the scoped commit. Only report/merge-note documentation changes follow that native run.

**Complete `check assemble` was not run.** Refreshing final `checkFast` was also stopped after the 264-step outcome. Current discovery has 368 server identities; the remaining 331 server identities have no final-candidate native acceptance here. No complete-suite result, final-fast pass, READY status, isolated master-baseline exception or owner integration is claimed. The owner instructed stopping with a committed negative when either measured growth deviation exceeds 240; that instruction controls these skipped steps.

Evidence is external in catchup4/T01: `raw/t01-acceptance.json`, `raw/frozen-service-interval.json`, `raw/final-scarcity-vectors.json`, `raw/final-growth-vectors.json`, `raw/final-cargo-vectors.json`, `raw/final-growth-ledgers.json`, `raw/runs-audit.json`, frozen native XML/logs and source snapshots. `raw/infrastructure-events.json` retains the attempt/recovery sequence. Historical catchup3 evidence remains read-only.
