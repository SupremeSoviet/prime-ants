# Final catch-up handoff — T08, 2026-10-13

Qualified partial delivery. Catch-up runs automatically when an owned colony area reloads: adults age and pay maintenance, confirmed food is served, existing brood develops, affordable eggs may be laid, and StageRules evaluates the resulting colony. Digging, building and mining are excluded from aggregate replay.

The sole final `check assemble -PprimeAntsGameTestConcurrency=2 --console=plain --rerun-tasks` checkpoint executed **346/350 server cases** (76 fast and 274 long), **124 unit** and **26 model** cases. Its launcher exit is **1**, duration **10795.473 s**. No server identity is missing or duplicated. The final Gradle guard additionally rejected expected345 versus actual350: its fixed173 term omits the three FounderAccessPriority and two HallFunding identities. All five executed and passed. Offline frozen-source/XML diagnosis proves complete native coverage; it does not repair the executable guard or make formal check acceptance green. This closes the eight-turn budget; no T09 is requested. Version0.1.0 jars are **unreleased catchup artifacts**, not release acceptance.

## Final verdict

These current failures block complete catch-up delivery:

- `AutomaticCatchupGameTest.confirmedFiniteCacheFeedsAdultsAndExistingFoodFedLarvaExactlyOnce`: At actual service completion all three original physical units are consumed once and credited by kind to one owner on tick 5972
- `CatchupLifecycleGameTest.stageGraceDeadlineAndClockReloadMatchLiveAndUnknownInterruption`: Restored certainty starts at observation end, never backdates previously unknown deficit on tick 6
- `TierThreeGameTest.hallHasThreeConsecutiveGateQualifiedLayingIntervals`: Didn't succeed or fail within 20000 ticks
- `TierThreeGameTest.workerBuiltStoneChambersConfirmAllFunctionsAndDeliverEveryTierThreeEffect`: Didn't succeed or fail within 20000 ticks

Catch-up blocking comparisons: `AutomaticCatchupGameTest.confirmedFiniteCacheFeedsAdultsAndExistingFoodFedLarvaExactlyOnce`, `CatchupLifecycleGameTest.stageGraceDeadlineAndClockReloadMatchLiveAndUnknownInterruption`. Their actual failed assertions remain rejected; earlier targeted positives do not override them. Automatic dispatch remains enabled, with these unsupported behaviors explicitly disclosed. Ordinary/preservation failures also keep the complete checkpoint red. Their attribution is scoped: this turn does not establish that catch-up caused or cannot affect them. No mining, construction, ordinary priority or timeout repair was made to obtain acceptance.

## What reload applies

The saved server game-time boundary defines the exclusive absent interval `(L,C)`. The ordinary step at C runs once. A stopped server adds no game ticks. Known ordinary adult intervals and virtual birth boundaries exclude work already completed. Accepted maximum counters saturate.

Actual adults retain their identities, age, nutrition, health and death precedence. Death cargo uses the existing single custody identity. Confirmed cached food or actual saved nurse cargo pays real ingestion; service intentions themselves reserve no unit and earn no receipt. Food with no eligible recipient remains canonical stock.

Eggs pay queen sugar/protein and larvae pay their own development costs. Absence invents no income, forage or supplies. Existing StageRules retains capacity, confirmed chamber requirements, promotion, regression and the 24,000-tick shortfall grace. Owned food/material stores preserve their real kinds, quantities and release identities, including stock above reduced capacity.

Missing members or UNKNOWN terrain defer the nursery with explicit pending time. Adults that ordinarily tick while waiting are excluded when pending work resumes. Legacy/compacted unknown history is recorded separately and conservatively recovered, not charged as simulated time. Native reload, both partial-service load orders, same/older replay, ordinary brood viability, migration, investment refusal and normal nonzero store reconstruction pass their final permanent cases unless named above. These establish coherent saves and compatibility; cross-object crash durability remains unproved.

## Bounded allocation and accuracy

Ordinary crop behavior is unchanged. Its fasting-then-donor-distance comparator is shared; aggregate planning now selects per donor and respects saved cooldown, recipient claims, target and partial action. Cooldown decreases only over known eligible missing time, without stamping an ordinary controller observation. Travel, periodic search and crowding remain approximated.

The old finite-cache failure was an empty queue during a saved return cooldown (next wake 28 ticks later), not a permanent stall. Its same baseline also retained an unserved hungry worker, recorded separately from the queue diagnosis. The corrected control advances production hooks on independent offsets 11/40/80/120/200/300/400, with its original apple, berry and chicken and 8,000 test bound. The targeted recovery passed, but the final complete run retains its allocation failure. Queue/cooldown timing and recipient allocation remain distinct findings; this case is not final accepted recovery. At the final horizon the hungry worker still has 0 sugar/no meal; the larva received apple/chicken and the queen received the berry. All five initial units including queen receipts remain exact, with exact recorded nutrition equations. Completion, return availability, wake and eligibility are recorded separately. The failed finite case does not reach its later replay assertions; separate replay/load-order cases pass.

The exact four-unit ordinary control disproves the old fifth-adult expectation: duration 120/span 361 leaves four adults and a larva at progress 0, sugar 0/protein 8,000; the queen retains 6,909 sugar/6,000 protein. Aggregate play matches it. The original identity, supplies and failed evidence are retained; only that live-disproved expectation is corrected. Separate funded virtual birth and StageRules promotion/regression coverage stays active.

| Control | Live development | Catch-up development | Maximum difference | Bound |
|---|---|---|---:|---:|
| no_initial_adult_demand | [1200, 2554, 2688] | [1200, 2511, 2682] | 43 | 120 |
| original | [1200, 2506, 2643] | [1200, 2442, 2613] | 64 | 120 |

Both final controls satisfy every unchanged tolerance. Bounds remain adult/occupied-slot difference≤1, cache difference≤1 unit per represented kind, original-recipient residual difference≤4000 sugar/8000 protein, equal hunger/starvation classes and exact within-run identities, conservation and replay. Queen affordability is reported independently with the full nutrition vectors in the packet. T07’s129-step discrepancy remains a rejected historical comparison; it was not rounded to120 or repaired by a fitted delay.

The first original crop receipt occurs at20 live steps versus52 aggregate steps, with a different donor. The full original-adult sugar deltas are0,+8,-1500,0,+1500; protein deltas are0, and hunger/starvation classes and queen affordability match. These results remain within the unchanged quantization bounds. The additional control has zero residual deltas.

The final UNKNOWN-restoration clock comparison remains rejected. Its actual `since` and observation-end operands are not logged, so the numeric difference is UNKNOWN. A food-service event could alter that clock earlier; this possible explanation is not established. No expectation is corrected from this speculation.

## Cost, artifacts and remaining limits

Retained fixed profiles: 4 adults: n=8, p95 4.5965 ms, maximum 4.5965 ms; 120 adults: n=8, p95 12.2594 ms, maximum 12.2594 ms. These are short empty-nursery measurements, with cold/warm and slow samples retained. They do not establish long growing-colony or general 120-adult cost. Instrumented 1,850-tick growing-control invocation maxima: no_initial_adult_demand catch-up: 88.7878 ms; original catch-up: 136.6702 ms. Growth observers are active in those measurements, so these are not source-free production benchmarks. All printed native costs and every≥20ms slow sample are retained separately, including zero-event/UNKNOWN deferrals; they are not pooled into a general latency claim.

The final graph packaged runtime/source jars; inspection found no excluded GameTest/helper entries. The protected `run`, `build/run/gameTest` and `build/run/clientGameTest` inventories are compared in the packet. Own concurrency stayed at two; active leases and newest-ten retention were preserved. No parallel worktree or master was written.

Reproduce the supported verdict from `raw/final-catchup-acceptance.json`, final XML, frozen source/classes and native logs in `<turnloop>/directions/prime-ants-catchup/turns/T08`. The baseline is catchup `474b7c7`, already containing local master `14ef11f`; final commit and later master are recorded in that external packet. Every original T06 negative is revisited there; later positives do not explain the historical failure causes.

Unsupported general allocation could be addressed by conservative deferral of unsupported new investment or a more faithful bounded fetch/approach/feed/return model, with prospectively frozen controls. Neither alternative is silently enabled, and automatic validated behavior remains on. The two ordinary hall/tier-three failures require owner dependency investigation. Six original T06 negatives are current positives, including mining and supplied surface development; their historical causes remain unexplained. No release, version change, tag, push or master integration is claimed.
