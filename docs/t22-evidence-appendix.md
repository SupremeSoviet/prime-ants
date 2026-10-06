# T22 evidence appendix

Current unfiltered acceptance is GREEN 15/195/26. Fresh loading/closure is demonstrated. Natural-client colony/combat evidence remains incomplete; the observed nonselection is qualified below.

## Implementation and observation boundaries

Actual food alone funds commitments, a prospective investment and unchanged maintenance margin. Mature registered workers use the existing action owner to respond to one actual provoking player, including after nest readiness fails. Alarm: 600 loaded ticks; response 12; chase 16; mob bite one health point at physical reach/visibility, cooldown 20. Construction/forager claims and canonical equipment remain held. Both uncommitted social endpoints and feeding/meal timers cancel. Alarm remaining time and interrupt/cooldown/task state persist; broader cold restart is T23.

Ownership comes from actual preparation/mound/plug writes or owned nursery/cache block entities. BEFORE captures it before mutation; canceled attempts discard it and successful AFTER consumes it once. Matching player blocks, historical coordinates, ordinary ant work, zero/other-source damage and benign visits/drops do not signal.

One declared seed 2026100501, ordinary generated overworld/survival, default placement, render/simulation 8/8. Loading proof requires client/server clocks, key input movement, rendered terrain and normal populated close. A separate declared fresh natural scenario uses brood 100x/founding 20x, normal adult biology/physics, radius 128 and at most 24,000 extra server ticks through close. Observer startup window 1280x720, hidden HUD, daytime, camera and player setup moves are disclosed. No ant positioning, AI pauses, forced stages/poses, supplied ants/food, terrain clearing, imports or diagnostic placement.

## Failures and recoveries

- First unfiltered funding build: exit 1, 709.70 s, 185 server cases; five old controls timed out (one nursing continued-laying and four role conflicts). Exact names/messages: T22/funding-failed-names.json. Original guards/bounds stayed intact.
- Unheld nursing source increased only from six to ten apples (still eight chicken), conservation 14->18; genuine continued laying/emergence recovered in 17.27 s.
- Reservation diagnostics: more drops alone, internal piles, mixed internal piles and a queen AI hold did not provide adequate current funding before early NoAi holds paused intake. All those unsuccessful arrangements were removed. Six failed focused commands retain their logs/metadata.
- Final negative fixture recovery delays admission only during setup until actual collection reaches 21,000/30,000 current stores. No food credit, manufactured brood or altered stages; normal food-fed laying follows. All four original role controls recovered at their unchanged 26,000-tick bounds. This additional test-only setup hold is explicitly disclosed; production admission/ecology is unchanged.
- First defense harm fixture signaled but no bite; supported clear player stands and normal player ticks recovered actual mob damage. Subsequent assertions incorrectly sampled reach after knockback or health after saturation regeneration; observational contact snapshots now measure accepted pre-knockback reach/visibility and raw one-point damage, with unchanged physics. Innocent player placement is also supported. Four failed harm commands remain.
- Expiry fixture attempted a wall over an owned mound and failed its protection guard. It now selects a supported bounded hazard outside every current owned cell; no protection was widened. Exact cargo/claim/phase expiry passes.
- rg was unavailable; PowerShell/Python searches replaced it. Two atomic patch attempts were rejected (partial-line mismatch, duplicate file operations); corrected before dependent work. No source regeneration, timeout/RAM increase or owner-system change.

- First defense-inclusive unfiltered t22-build-final: exit 1, 619.92 s, 195 cases, one new owned-break fixture failed with No value present. Both nests could become OPEN before nurse assignment; requiring a nurse label introduced a scheduling race. The case now selects an actual living mature registered member, preserving break ownership, invalid-readiness and actual bite requirements. Exact-case recovery t22-defense-owned-break-mature passed in 16.28 s before fresh t22-build-final-r1. No time bound or production guard changed.

## Commands and completed metadata

Each named command has T22/<name>.json and .log under the evidence root. Diagnostic filters validate only one registered passing name and never count as acceptance.

| Name | Exit | Seconds | Actual Gradle command |
|---|---:|---:|---|
| t22-build-final-r1 | 0 | 620.38 | `.\gradlew.bat build --console=plain --rerun-tasks` |
| t22-build-final | 1 | 619.92 | `.\gradlew.bat build --console=plain --rerun-tasks` |
| t22-build-funding | 1 | 709.70 | `.\gradlew.bat build --console=plain --rerun-tasks` |
| t22-build-loading-recovery | 0 | 620.09 | `.\gradlew.bat build --console=plain --rerun-tasks` |
| t22-build-scenario-recovery | 0 | 670.96 | `.\gradlew.bat build --console=plain --rerun-tasks` |
| t22-client-fresh-loading | 1 | 111.13 | `.\gradlew.bat runClientGameTest --console=plain -PprimeAntsCapturePrefix=t22-fresh-a1 -PprimeAntsClientScenario=fresh-survival -PprimeAntsFreshLoadingOnly=true -PprimeAntsBroodMultiplier=100 -PprimeAntsFoundingMultiplier=20` |
| t22-client-loading-recovery | 0 | 37.49 | `.\gradlew.bat runClientGameTest --console=plain -PprimeAntsCapturePrefix=t22-fresh-a2 -PprimeAntsClientScenario=fresh-survival -PprimeAntsFreshLoadingOnly=true -PprimeAntsBroodMultiplier=100 -PprimeAntsFoundingMultiplier=20` |
| t22-client-natural-defense | 1 | 72.20 | `.\gradlew.bat runClientGameTest --console=plain -PprimeAntsCapturePrefix=t22-fresh-a3 -PprimeAntsClientScenario=fresh-survival -PprimeAntsBroodMultiplier=100 -PprimeAntsFoundingMultiplier=20` |
| t22-client-natural-recovery | 0 | 330.19 | `.\gradlew.bat runClientGameTest --console=plain -PprimeAntsCapturePrefix=t22-fresh-a4 -PprimeAntsClientScenario=fresh-survival -PprimeAntsPriorScenarioTicks=501 -PprimeAntsBroodMultiplier=100 -PprimeAntsFoundingMultiplier=20` |
| t22-compile | 0 | 4.91 | `.\gradlew.bat gametestClasses --console=plain` |
| t22-defense-benign | 0 | 14.20 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:defense_game_test_canceled_unknown_and_ordinary_writes_food_drops_and_zero_damage_stay_benign` |
| t22-defense-bounds | 0 | 15.26 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:defense_game_test_walls_range_creative_spectator_and_missing_targets_prevent_remote_bites` |
| t22-defense-components | 0 | 14.93 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:defense_game_test_successful_mound_cache_and_nursery_breaks_renew_only_actual_owner` |
| t22-defense-expiry-cargo-clear | 0 | 15.78 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:defense_game_test_loaded_alarm_expiry_restores_same_forager_exact_soil_cargo_claim_and_task` |
| t22-defense-expiry-cargo | 1 | 14.76 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:defense_game_test_loaded_alarm_expiry_restores_same_forager_exact_soil_cargo_claim_and_task` |
| t22-defense-expiry-loaded | 0 | 18.40 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:defense_game_test_loaded_alarm_expiry_restores_same_forager_exact_soil_cargo_claim_and_task` |
| t22-defense-feeding | 0 | 14.58 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:defense_game_test_provocation_cancels_uncommitted_feeding_and_preserves_physical_food` |
| t22-defense-harm-contact | 1 | 15.96 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:defense_game_test_accepted_survival_harm_bites_only_provoker_with_reach_cooldown_and_coherent_restore` |
| t22-defense-harm-measured | 0 | 15.71 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:defense_game_test_accepted_survival_harm_bites_only_provoker_with_reach_cooldown_and_coherent_restore` |
| t22-defense-harm-supported | 1 | 15.19 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:defense_game_test_accepted_survival_harm_bites_only_provoker_with_reach_cooldown_and_coherent_restore` |
| t22-defense-harm-visitors-supported | 1 | 15.96 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:defense_game_test_accepted_survival_harm_bites_only_provoker_with_reach_cooldown_and_coherent_restore` |
| t22-defense-harm | 1 | 21.57 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:defense_game_test_accepted_survival_harm_bites_only_provoker_with_reach_cooldown_and_coherent_restore` |
| t22-defense-missing-cargo | 0 | 15.32 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:defense_game_test_missing_provoker_restores_same_forager_exact_soil_cargo_claim_and_task` |
| t22-defense-owned-break-mature | 0 | 16.28 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:defense_game_test_owned_break_captures_before_revocation_alarms_only_owner_and_defends_unready_nest` |
| t22-defense-owned-break | 0 | 14.88 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:defense_game_test_owned_break_captures_before_revocation_alarms_only_owner_and_defends_unready_nest` |
| t22-defense-plug | 0 | 13.91 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:defense_game_test_live_owned_plug_break_retains_source_across_ordinary_write_revocation` |
| t22-defense-sharing | 0 | 19.79 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:defense_game_test_provocation_cancels_real_partial_crop_sharing_without_transfer_cargo_or_claim_loss` |
| t22-funding-conflict-dead | 0 | 23.34 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:role_conflict_game_test_dead_forager_cannot_steal_empty_handed_partial_builder` |
| t22-funding-conflict-persisted | 0 | 22.03 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:role_conflict_game_test_conflicting_persisted_forager_claim_releases_without_losing_builder_progress` |
| t22-funding-conflict-restore | 0 | 21.72 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:role_conflict_game_test_restored_partial_builder_and_queen_keep_reciprocal_claims` |
| t22-funding-four-mature-admission | 0 | 20.50 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:role_conflict_game_test_disabled_nursing_labels_cannot_reserve_care_with_four_other_mature_workers` |
| t22-funding-four-mature-diagnose | 1 | 28.38 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:role_conflict_game_test_disabled_nursing_labels_cannot_reserve_care_with_four_other_mature_workers` |
| t22-funding-four-mature-internal | 1 | 28.04 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:role_conflict_game_test_disabled_nursing_labels_cannot_reserve_care_with_four_other_mature_workers` |
| t22-funding-four-mature-mixed | 1 | 27.63 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:role_conflict_game_test_disabled_nursing_labels_cannot_reserve_care_with_four_other_mature_workers` |
| t22-funding-four-mature-stocked | 1 | 27.79 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:role_conflict_game_test_disabled_nursing_labels_cannot_reserve_care_with_four_other_mature_workers` |
| t22-funding-four-mature-stocked20 | 1 | 26.38 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:role_conflict_game_test_disabled_nursing_labels_cannot_reserve_care_with_four_other_mature_workers` |
| t22-funding-four-mature | 1 | 26.86 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:role_conflict_game_test_disabled_nursing_labels_cannot_reserve_care_with_four_other_mature_workers` |
| t22-funding-laying-control | 0 | 17.27 | `.\gradlew.bat runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:nursing_game_test_physical_nurses_raise_new_identities_and_continue_laying` |

## Client and final acceptance

Current acceptance: t22-build-scenario-recovery, exact unfiltered build, exit 0/670.96 s; 15 unit/195 server/26 model, all 184 baseline server names retained plus eleven additions, zero failures/errors/skips, stable inputs and clean production archives. Earlier green names remain valid for their earlier inputs. All four client workloads followed completed current green acceptance. T22 adds four actual launches: history 30 launches, native startup 21/9, tasks 13/17. These task outcomes include a passing bounded nonselection, not client defense sign-off. Thirty-three headless server launches are separate. No FULL allowance is assigned to client/playtest modes; interrupted old consumption stays unknown.

T21's 36,000-tick native survival was observed under the pre-fix funding rule. It is not re-accounted or promoted to current-rule balance evidence. T23 remains player feeding, multi-colony MSPT and broad save/reload; T24-T26 visuals and T27-T30 release.


## Fresh-client recovery sequence

A1: exit 1/111.13 s. World creation and server founding tick 953 progressed; timeout was Fabric TestServerConnectionImpl.waitForChunksRender's download predicate, not the historical transfer deadlock. Native world closed/saved normally and was archived immediately (56 files, level.dat). A read-only thread-dump query ran after the owned process had already exited; no JVM was stopped and no hang signature was fabricated. Cached API requires the full square of render-distance chunks; vanilla ChunkTrackingView at distance eight excludes its corners and IntegratedServer uses the configured view distance. The harness now uses waitForChunksRender(false), then requires compiled visible terrain at the player. It preserves distance 8/8 and the timeout; current unfiltered acceptance preceded its retry.

A2: exit 0/37.49 s. NoiseBasedChunkGenerator, minecraft:overworld, seed 2026100501; server ticks 14->36, client 12->34; W input moves about four blocks. Opened 1280x720 PNG: readable ordinary shore, forest and water. Normal populated close and immediate archive (52 files, level.dat) pass.

A3 natural scenario: exit 1/72.20 s. Loading/movement/terrain work again. The first observer move threw IllegalFormatConversionException because a height integer was formatted with %f. The owned world closed/saved and was archived immediately; 501 additional server ticks are recorded through close. No colony/combat/photo claim followed. The single cause is corrected by a double height argument. Those 501 ticks count against the same 24,000-tick natural scenario budget; no seed change or unknown-as-zero accounting. Fresh full acceptance is required before its targeted recovery.


## Final bounded natural result and limitation

A4: task exit 0/330.19 s, ordinary generation and startup movement/render again, normal populated close, archive immediately (61 files, level.dat). Natural scene used 2,501 additional server ticks through close; together with A3's 501 the declared scenario consumed 3,002/24,000. Seed and radius never changed. Discovery recorded 53 candidates: 12 rejected, 37 pending/unknown, four actual automatic queens, all outside the declared horizontal radius 128. No colony/worker was selected; no ant attack input, client bite, entrance/mound or combat PNG followed.

The automatic observer route had a real defect: a water waypoint led to drowning, and a subsequent height query for a not-yet-generated waypoint returned -64 and was used for teleport setup. No invulnerability, revival, terrain edit or ant manipulation occurred. This limits the interpretation: it is a bounded observed nonselection, NOT a complete near-spawn absence proof or a reliable discovery/combat driver. Further attempts/area widening were stopped. This defect and the natural combat/capture gap must be carried forward.

Actual opened current PNGs are only terrain loading evidence: t22-fresh-a2-terrain.png (clear shore/forest/water) and t22-fresh-a4-terrain.png (readable ordinary shoreline, foreground leaves). A3 terrain PNG is retained with its failed-scenario provenance. There is no real-ant T22 colony/defense image. Server actual damage is separately documented in damage-evidence.txt: mob source, ant 31506261-a284-3b0b-9f0a-da0eaaa5a04d, health 20->19, cooldown 20. It is not attributed to a client image.
