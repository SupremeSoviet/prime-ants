# Exact known-flaky timeouts — disposition 2026-10-18

The owner's **2026-10-18 00:35 (Asia/Yekaterinburg)** instruction qualifies the exact
20,000-tick oversized-inventory timeout below. It supersedes T19's nonexempt classification;
the sealed T19 reports/results remain unchanged. This policy is not limited to T15's original five.

Only the documented **timeout at its original bound** qualifies, and only when every other test
executes and passes. Assertions, errors, crashes, skips, missing identities, incorrect outcomes,
save mismatches and other failure messages block release. No family is exempt. All tests still
execute with original guards, deadlines and assertions. Native XML and native/Gradle/root exits
retain raw failure. Release-policy acceptance is recorded separately from raw suite success.

| Exact identity | Original tick bound |
|---|---:|
| `prime_ants_test:forager_scaling_game_test_large_colony_foragers_carry_at_once_with_exact_accounting_and_claims_survive_reload` | 30,000 |
| `prime_ants_test:surface_game_test_supplied_founding_to_mature_retains_its_small_mound_and_workers_build_larger_crests_and_an_open_arch` | 60,000 |
| `prime_ants_test:lifecycle_game_test_environmental_chicken_loot_funds_new_worker_then_queen_death_ends_colony` | 26,000 |
| `prime_ants_test:mature_colony_game_test_upgrade_counts_every_clay_unit_in_each_place_through_its_builder_death` | 60,000 |
| `prime_ants_test:mining_game_test_mid_haul_disk_reload_resumes_then_miner_death_releases_one_unit_exactly_once` | 60,000 |
| `prime_ants_test:nursery_upgrade_game_test_young_colony_reaches_mature_by_real_play_then_rebuilds_its_nursery_in_packed_clay_and_its_brood_develops_faster` | 60,000 |
| `prime_ants_test:tier_three_game_test_physical_oversized_inventories_survive_in_memory_reconstruction_and_capacity_loss` | 20,000 |

The original five have complete unfiltered pass/timeout histories in the [T15 dated policy](stage-1-report.md).
The nursery identity has complete native histories and fixture qualifications in that report's
2026-10-17 additional-timeout entry and the [T18 checkpoint](t18-release-correctness.md).

The oversized identity passes in the complete **T15 (309/309 long)** and **T18 (309/309 long)** runs.
External native evidence is indexed by `T15/raw/t15-complete-coverage.json` and
`T18/raw/t18-complete-coverage.json` under `<turnloop>/directions/prime-ants-stage1c/turns/`.
T19's selected twenty-case run is **19/20**, with the original native message
`Didn't succeed or fail within 20000 ticks`, native exits `[0,0,1]` and Gradle/root exits `1/1`.
Its first physical cause remains **UNKNOWN**; the last observed unmet gate is not a cause.
See [the inherited functional proof](t19-functional-persistence.md) and external
`T19/raw/t19-long-oversized-diagnosis.json` for native elements and unchanged-fixture references.

The separate **catchup6** observations are correctly distinguished: the failed candidate's
nine-case cohort times out; two exact-master nine-case cohorts pass at helper17,306 and19,126;
an **isolated** candidate passes at19,447. They are not complete unfiltered passes.
The [actor-order and route comparison](catchup-t02-diagnosis.md) belongs to catchup6, not T19.
Its initial UUID ordering explains the observed different selector result, without proving the
timeout's physical cause or a catch-up defect. No repair is attributed to later passes.

No timeout investigation or repeated unchanged complete run is authorized merely to obtain green.
Fresh non-timeout failures still require preservation and supported diagnosis before publication.

T20's fresh complete oversized result is PASS, without an exemption. Its complete long suite is nevertheless raw red
because the miner mid-haul identity fails the separate original 6,000-loaded-tick named-recovery assertion.
That assertion is nonexempt under this exact policy. See the [current release blocker](t20-release-blocker.md).


## T21 complete6 general-policy and native-exit reconciliation; T22 verification

The owner policy is not a closed seven-name allowlist. Each actual native failure is checked by exact identity,
original outer bound and complete-run pass/timeout history. Every current case outside the qualified set executes
and passes. Assertions, inner miner6,000 assertions, errors, crashes, wrong outcomes and omissions remain blocking.
Complete6 contains383/386 raw passes, three exact qualified outer timeouts and zero errors/skips. Original XML,
root/Gradle/native exits and the old driver's STOPPED_FOR_REVIEW disposition remain unchanged.

Additional exact qualifications, each with an exact native PASS in the complete386-identity T20 run:

| Exact identity | Original outer bound | Complete T20 native XML SHA-256 |
|---|---:|---|
| `prime_ants_test:colony_stage_game_test_unavailable_entrance_never_hides_loaded_broken_wall_and_the_colony_regresses` | 24,000 | `8eb625f92288c138adcc827fb405ceb00ab4d43fbd2400c1c754265e5f2ea7e7` |
| `prime_ants_test:nursery_upgrade_game_test_upgrade_keeps_every_unit_through_reload_and_killed_builder_and_never_converts_player_changed_wall` | 60,000 | `748aae3e9b4064e4f7ecc09822ba6d9a89452435b98e4b7614ce666fd8e219d4` |

The already documented surface60,000 qualification is the third current timeout. Native long exits[0,2,1]
equal required-failure counts[0,2,1] after complete XML and normal shutdown. Pinned GameTestServer.onServerExit
returns getFailedRequiredCount; code2 is not changed to0 or treated as a crash. Eleven failure-shape and four
exit/count controls reject assertions, wrong bounds/identities, missing completion and crashes. The obsolete
0/1-only driver stopped after preserving reports/worlds; separate offline review completed the accounting.
The report-name collision was fixed only in the offline consumer. Original failures remain preserved; no retry
or gameplay change followed these reporting corrections. Historical physical causes remain UNKNOWN.

Fresh T22 checkFast passed77fast/124unit/26model/29Python with exits0/0/[0]. It ran no309-case long suite;
complete6 remains the unchanged source for long acceptance. T23 separately passed the same fast checkpoint77/124/26/29, exits0/0/[0]. On 2026-10-18 at08:33:25UTC, the owner
accepted the existing T21 client recording exception for release. The measured per-site wait remains unresolved:
chunk(10,9), cell(2,2),0–2351ticks against1200; acceptance does not prove the bound was met. [Current status](release-0.2.0-acceptance.md).

Общая политика не ограничена семью именами. Два дополнительных точных имени/предела выше имеют native PASS полного
T20; complete6 содержит три исходных внешних timeout и383прохода. Все прочие тесты выполнены успешно. Exits[0,2,1]
равны required failures после полного XML/штатного shutdown; код2 не превращён в0. Assertions, внутренний miner6 000,
ошибки, crashes и пропуски не освобождены. T22 выполнила только fast-проверку; long-доказательства повторно использованы.

T23 также прошла fast77/124/26/29 с exits0/0/[0]. 2026-10-18 в08:33:25UTC владелец принял существующий клиент T21
с исключением записи: chunk(10,9), cell(2,2),0–2351при1200; соблюдение предела не доказано.
