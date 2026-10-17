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
