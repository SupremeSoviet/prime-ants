# Catch-up acceptance: catchup5 T01

**NOT_READY.** The candidate fails the named-pickup disk-reload assertion in `SurfaceAccessSafetyGameTest`, while the same identity passes the exact merged master. This triggers the required negative stop. Five other native reds remain unexempted. The complete command was cancelled, leaving its outer exit and final assembly unobserved; local master also advanced to `36744e2ad2a11a7c7503530655894d7ab8a37c11` after executable freeze.

Tested/merged master: `14ef11f68a4f55c87ad2b8725ebc748f6f5d8573`. Initial clean catchup: `d2e222466c4544a2fa07b78ae56d9ec56f3e0c9a`; `git merge master` exited 0, already up to date, no conflicts. Master was rechecked unchanged before final fast and complete validation. The external catchup5/T01 `raw/t01-acceptance.json` identifies the final committed candidate, tested source hashes and post-commit status. No owner integration was performed.

Local master later advanced to `36744e2ad2a11a7c7503530655894d7ab8a37c11` at 12:47:46, after the executable snapshot and while the orphaned native children were still running. The enclosing build had already been cancelled at 12:13:06. This tested candidate does not include that advancement. Integration onto current master is blocked until reconciliation and refreshed affected, fast and complete acceptance. No new inputs were merged into active servers.

The owner's 2026-10-15 product decision replaces the former 120-primary/240-fallback contract with one **300-step / 25%-of-stage** bound. `CatchupOutcomes` now has one step limit and the equivalent stage fraction; both finite controls use it in verdicts, evidence and messages. Original-brood birth timing uses `stageDuration * .25` in its own tick units (30 ticks for its 120-tick stage), including reported timing limits. Stage durations are unchanged. Earlier limits below and in T07/design checkpoint history are historical measurements.

Final complete-run growth operands (all limits 300):

| Control | Live | Catch-up | Absolute deviations | Maximum |
|---|---|---|---|---:|
| no_initial_adult_demand | [1200, 2553, 2670] | [1200, 2629, 2767] | [0, 76, 97] | 97 |
| original | [1200, 2566, 2626] | [1200, 2629, 2767] | [0, 63, 141] | 141 |

The automatic original-brood control's live birth ticks are `[360, 360, 360]`, versus catch-up `[360, 360, 360]`. Its maximum timing deviation is **0 ticks**, within **30.0 ticks = 25% of its unchanged 120-tick stage** (`raw/complete-birth-timing.json`).

Earlier native measurements on the same executable were focused: original=264, no_initial_adult_demand=79; affected: original=141, no_initial_adult_demand=97. They remain separately recorded; the table above uses the complete run, with no refitting or substitution of an earlier vector.

Both load-order controls require valid nutrition for the original larva from physical food. They preserve exact partial ordinary actions, full saved WorkerTask NBT state, actor identity/position, normal reconstruction, UNKNOWN handling, resumed catch-up and same/older replay. They permit retained unconsumed cargo. No selected donor is forced. Every checkpoint records actual carrier UUIDs/stacks/positions, per-recipient receipts, current and canonical archived brood receipts, actual releases and seven-kind custody. Per-kind physical debits equal ingestion deltas and cover the original larva's receipts; no synthetic release term or grand-total-only attribution is used.

| Load order | Original selected hand | Final selected hand | Partial ticks before/after | Original larva meals | Total custody/receipts |
|---|---|---|---|---|---|
| nursery_first | 1 minecraft:apple | 1 minecraft:apple | 5/5 | 0→2 | 10=10 |
| actor_first | 1 minecraft:apple | 1 minecraft:apple | 5/5 | 0→2 | 10=10 |


Exact cargo operands are `(stock, carried, ingested including prior/archived, actual release)`:

| Order | Kind | Initial | Final |
|---|---|---|---|
| nursery_first | apples | (0, 2, 6, 0) | (0, 1, 7, 0) |
| nursery_first | berries | (0, 0, 0, 0) | (0, 0, 0, 0) |
| nursery_first | chickens | (0, 1, 1, 0) | (0, 0, 2, 0) |
| nursery_first | nectar | (0, 0, 0, 0) | (0, 0, 0, 0) |
| nursery_first | nectarV2 | (0, 0, 0, 0) | (0, 0, 0, 0) |
| nursery_first | prey | (0, 0, 0, 0) | (0, 0, 0, 0) |
| nursery_first | flesh | (0, 0, 0, 0) | (0, 0, 0, 0) |
| actor_first | apples | (0, 2, 6, 0) | (0, 1, 7, 0) |
| actor_first | berries | (0, 0, 0, 0) | (0, 0, 0, 0) |
| actor_first | chickens | (0, 1, 1, 0) | (0, 0, 2, 0) |
| actor_first | nectar | (0, 0, 0, 0) | (0, 0, 0, 0) |
| actor_first | nectarV2 | (0, 0, 0, 0) | (0, 0, 0, 0) |
| actor_first | prey | (0, 0, 0, 0) | (0, 0, 0, 0) |
| actor_first | flesh | (0, 0, 0, 0) | (0, 0, 0, 0) |

Actual carrier custody (all actors carrying at either endpoint):

| Order | Actor UUID | Initial hand | Final hand |
|---|---|---|---|
| nursery_first | `237525f8-9aac-3020-8e02-c62a177452d9` | 1 minecraft:apple | 0 minecraft:air |
| nursery_first | `296fcba8-5a5e-382a-91b7-218cb6d00b8f` | 1 minecraft:apple | 1 minecraft:apple |
| nursery_first | `053dcf1c-c0b8-35d6-916b-17caf32e2ec1` | 1 minecraft:chicken | 0 minecraft:air |
| actor_first | `a057acdb-7f97-38e8-adda-dce7789bcf24` | 1 minecraft:apple | 0 minecraft:air |
| actor_first | `b5e5a7cb-07fa-3cc3-8ac0-604e2cfa3b25` | 1 minecraft:apple | 1 minecraft:apple |
| actor_first | `78c451fd-4e6b-3c8a-85e9-8dffe78a1cc3` | 1 minecraft:chicken | 0 minecraft:air |

`AllocationObservations` already emits exclusive brood nectar v1/v2 subtotals. The outcome helper now adds those subtotals without a second v2 subtraction. Regression `catchup_lifecycle_game_test_brood_nectar_v2observer_outcome_counts_one_v2and_zero_v1` exercises a real brood nutrition receipt through the observer and outcome helper. The old executable reported v1/v2 **-1/1**, native exit 1; final reports **0/1**, expected **0/1**, with no negative consumption or double attribution. Registration changes are one inventory addition and lifecycle guard 24→25.

All six scarcity cases retain the original supplies, configurations, positions, horizons, orders and observation offsets. Cadence stays 393 missing + seven ordinary steps versus 400 live steps. Hunger/starvation classes, 8,000 larval protein, crop balance, nutrition, reconstruction and replay pass. Consumption differs by at most one unit per item kind. Kind order below is apple, berries, chicken, nectar v1, nectar v2, prey, flesh.

| Input | Order | Larval protein | Adult classes match | Live consumption | Catch-up consumption |
|---|---|---:|---|---|---|
| captured_971 | saved | 8000 | True | [2, 1, 2, 0, 0, 0, 0] | [2, 1, 2, 0, 0, 0, 0] |
| captured_971 | nurses-first | 8000 | True | [2, 1, 2, 0, 0, 0, 0] | [2, 1, 2, 0, 0, 0, 0] |
| captured_1500 | saved | 8000 | True | [2, 1, 2, 0, 0, 0, 0] | [2, 1, 2, 0, 0, 0, 0] |
| generated | nurses-first | 8000 | True | [2, 1, 2, 0, 0, 0, 0] | [2, 1, 2, 0, 0, 0, 0] |
| generated | saved | 8000 | True | [2, 1, 2, 0, 0, 0, 0] | [2, 1, 2, 0, 0, 0, 0] |
| captured_1500 | nurses-first | 8000 | True | [2, 1, 2, 0, 0, 0, 0] | [2, 1, 2, 0, 0, 0, 0] |

The complete outcome audit independently recomputes **210 exact per-kind equations**: 126 scarcity/growth/final-cargo equations plus 84 cargo-checkpoint equations. Every equation retains initial/final stock, cargo, prior ingestion, canonical archived/emerged brood once, and actual releases, including all zero kinds. It also cross-checks carried counts from actual named actor hands and ingestion totals from adult and canonical brood receipt operands. Existing adult/slot, residual nutrition, cache and queen-affordability assertions remain. The caregiver interval remains **92 ticks**, and production behaviour is unchanged.

Affected selection: all historical 37 identities read from catchup4 `raw/affected-final-selected.json`, plus the nectar regression; **38/38** execute once and pass. Final fast: **76 server + 124 unit + 26 model**, native exit **0**. One unfiltered `check assemble` invocation was launched after all executable changes. Its native children finished with **363/369 server passes**; its completed fast phase has 124 unit and 26 model cases. Exact fresh XML coverage is 369 server identities, with no missing, duplicate, unexpected, stale, errored or skipped identities. **Overall check/assemble acceptance is incomplete:** the daemon confirmed client-disconnection cancellation of `runLongGameTests` at 12:13:06.620; the wrapper's native exit/end, final long summary and completed assembly graph were not observed. Native child exits are fast **0**, long **3, 1, 0, 2**, retained unchanged. No outer exit is inferred from them. The independent union audit reads final child records/XML directly and does not manufacture a native harness summary.

Named residual failures and exact-master dispositions (no exemption is currently granted):

- `prime_ants_test:forager_scaling_game_test_large_colony_foragers_carry_at_once_with_exact_accounting_and_claims_survive_reload`: Not run after the brief required stopping on the proved SurfaceAccessSafety non-exception; no exemption claimed. Candidate message: `(-9158366, 4, 7496781) Didn't succeed or fail within 30000 ticks`; master result: `NOT_RUN`. Evidence: `not collected after mandatory stop`.
- `prime_ants_test:surface_access_safety_game_test_same_state_paid_target_write_during_named_pickup_cancels_without_acquisition`: This candidate failure is not a valid master exception: matching identity passed on exact master. The brief requires stopping further repair/broad reruns. Candidate message: `(-9158437, 4, 7496854) Actual disk data reload preserves exact bound attempt, locations, reservations, claim, partial clock and history on tick 690`; master result: `PASS`. Evidence: `master-baseline\evidence\raw\master-probe-audit.json`.
- `prime_ants_test:tier_three_game_test_worker_built_stone_chambers_confirm_all_functions_and_deliver_every_tier_three_effect`: Not run after the brief required stopping on the proved SurfaceAccessSafety non-exception; no exemption claimed. Candidate message: `(-9158295, 4, 7496854) Didn't succeed or fail within 20000 ticks`; master result: `NOT_RUN`. Evidence: `not collected after mandatory stop`.
- `prime_ants_test:tier_three_game_test_physical_oversized_inventories_survive_in_memory_reconstruction_and_capacity_loss`: Not run after the brief required stopping on the proved SurfaceAccessSafety non-exception; no exemption claimed. Candidate message: `(-13101090, 4, 9667202) Didn't succeed or fail within 20000 ticks`; master result: `NOT_RUN`. Evidence: `not collected after mandatory stop`.
- `prime_ants_test:nursing_recovery_game_test_personal_meal_of_carried_food_resumes_physical_service_by_the_same_nurse`: Not run after the brief required stopping on the proved SurfaceAccessSafety non-exception; no exemption claimed. Candidate message: `(-1946883, 4, -3218612) Every finite food unit stays ground/cache/cargo/custody or exactly one terminal consumption on tick 6291`; master result: `NOT_RUN`. Evidence: `not collected after mandatory stop`.
- `prime_ants_test:tier_three_game_test_hall_has_three_consecutive_gate_qualified_laying_intervals`: Not run after the brief required stopping on the proved SurfaceAccessSafety non-exception; no exemption claimed. Candidate message: `(-1947096, 4, -3218247) Didn't succeed or fail within 20000 ticks`; master result: `NOT_RUN`. Evidence: `not collected after mandatory stop`.

The isolated **master-probe passed 1/1**, native exit **0**, **56.952 seconds**, on the exact merged master with all **999** tracked source bytes unchanged. The candidate's `surface_access_safety_game_test_same_state_paid_target_write_during_named_pickup_cancels_without_acquisition` failure is therefore **not a master exception**. The brief's stop condition ended further repair and broader reruns; the other five master checks were not run and none is exempted, including all three actual TierThree reds. The successful fresh-parent probe also demonstrates recovery of targeted command/exit supervision after the original cancellation; it does not restore missing complete acceptance.

No obsolete injection target or summary-pipe hang was observed. The distinct infrastructure failure was Gradle client disconnection/cancellation during exec-a1 transport loss; surviving native children finished after their task graph had been cancelled. `raw/complete-cancellation-diagnosis.json` and `logs/complete-daemon-cancellation.log` preserve the diagnosis and unknown outer exit. No full restart occurred. All 28 harness regressions passed before broad launches. Routine compiler deprecation, Windows telemetry and fresh server.properties warnings did not prevent native completion. The million-tick event-bound control used 22 events/zero pending work and does not establish continuation across an actual 512-event cutoff; scarcity larvae still do not emerge within preserved horizons.

External evidence is under catchup5/T01: `raw/t01-acceptance.json`, `raw/*-audit.json`, `raw/complete-*-vectors.json`, `raw/complete-cargo-checkpoints.json`, `raw/complete-growth-ledgers.json`, `raw/complete-independent-outcome-audit.json`, `raw/guard-reports`, `raw/tested-source.json`, `raw/tested-source.zip`, `raw/source-correspondence.json`, frozen `raw/sharded-servers` XML/logs/input hashes, command launchers and `logs/*.json`. Native-tested code is unchanged by the final documentation/commit. See [the merge note](catchup-merge-note.md) for exact owner-session instructions.
