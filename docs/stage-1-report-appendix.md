# Stage-1 partial handoff: evidence and limits

This is supporting detail for the [public report](stage-1-report.md), not a new gameplay campaign. Repository links
below point to current source or technical history. Paths beginning with `<turnloop>` are **external local evidence**,
not files shipped in this repository. Turn paths are relative to `<turnloop>/directions/prime-ants-stage1/turns/`.

## Concrete identities and evidence

The named server identities below use prefix `prime_ants_test:`. They occur in T18's complete unfiltered native/merged
XML; every listed positive passes there. T20 does not rerun long bodies. Current authoritative inventory is
[gametest-suites.json](../config/gametest-suites.json); source discovery and frozen assignments are validated afresh.

| Requirement | Concrete identity or source | Corresponding evidence |
|---|---|---|
| Live stages | `colony_stage_game_test_egg_founded_colony_lays_beyond_the_founding_cap_only_after_promotion_to_young`; `colony_stage_game_test_real_worker_deaths_regress_to_founding_and_the_colony_regrows_to_young`; `colony_stage_game_test_chamber_registry_and_stage_survive_reload_and_legacy_capacity_fields_load`; [StageRulesTest](../src/test/java/dev/primeants/colony/StageRulesTest.java) | T18 `raw/checkpoint-acceptance.json`, linked XML; [rule/history note](stage-1-design.md), T01/T07/T08 implementation history |
| Functions and tier-two effects | `tier_two_game_test_natural_mature_colony_upgrades_every_function_and_shows_expanded_brood_and_unknown_entrance_stability`; [ChamberUpgrade](../src/main/java/dev/primeants/founding/ChamberUpgrade.java) | T18 complete XML; T08 `report.md`; [design function table](stage-1-design.md) |
| Mining and contributions | `mining_game_test_mature_worker_digs_connected_gallery_and_physically_stores_every_resource_with_settled_ledgers`; `mining_game_test_mid_haul_disk_reload_resumes_then_miner_death_releases_one_unit_exactly_once`; `material_hauling_game_test_dropped_clay_cobblestone_and_coal_reach_the_confirmed_store_after_the_food_with_exact_accounting`; `material_hauling_game_test_dropped_gravel_sand_raw_copper_and_raw_iron_are_food_first_one_unit_trips_and_settle_in_store` | T18 complete XML; T09–T12 reports retain supplied preparation and recovery conditions |
| Mature surface | `surface_game_test_controlled_mature_habitat_completes_every_paid_construction_prefix_and_walking_passage`; `surface_game_test_supplied_founding_to_mature_retains_its_small_mound_and_workers_build_larger_crests_and_an_open_arch` | T18 `raw/checkpoint-physical-excerpts.json` and its original native logs; T17 `raw/cohort-founder-physical.json` |
| Great failure | `surface_game_test_computed_great_habitat_builds_paid_gate_connected_ramparts_and_two_raised_watch_posts` | T18 `raw/checkpoint-acceptance.json`; T19 `raw/great-disposition.json`, `raw/great-phase-measurements.json`, `raw/great-endpoint.json` and linked native XML/log/process records |
| Generation/performance boundary | `natural_placement_game_test_genuine_generation_places_and_ticks_founding_queen`; [0.1.0 report](slice-1-report.md) | Existing founding coverage only. Historical two-colony/30-adult performance remains under its 0.1.0 conditions; no Stage-1 120-adult result |
| Handoff verification | T20 `raw/stage-1-handoff.json`, `raw/fast-validation.json`, `raw/packaging.json`, `raw/historical-reference-validation.json` | External settling packet, exact hashes, exits, fresh XML, frozen inputs, guarded client inventories and final commit |

T18's complete checkpoint is independently valid red despite the diagnostic collector's then-unfixed omitted-log path.
T19 closed that path with actual collector/CLI child-process regressions; its offline packets are verifier fixtures,
not gameplay evidence. No historical packet campaign is replayed in T20. Primary references used here are checked for
hash agreement and preserved bytes, including the exact founder excerpt and T19 observations.

## Physical and clock boundaries

T19's Great prefix comprises 28 relocated units, two retaining gate conversions and thirteen inherited receipts.
Four already-built Mature cells beyond that prefix explain physical **47/57** versus receipt **43/57**. Missing semantic
cells `(forward,side,layer)` are six post steps `(-2,±4,2)`, `(-3,±4,2)`, `(-2,±4,3)` and four upper watch-post cells
`(-1,±4,3)`, `(-1,±4,4)`. No completion, walking passage or 200-tick settlement is inferred from this physical partial.

First certain Great: helper 100 / nursery-loaded 101. Mature completion: helper 4,256; Great admission: helper 4,301.
The native timeout remains 12,000; the failure listener observes 12,002. The bounded controller total is 9,250 after
excluding automatic observations at 12,000/12,001; those lifecycle samples introduce no later material/receipt transition.
The periodic series ends at 11,500 and supplies no periodic 12,000 snapshot. Travel-request counts, supported stands,
path metadata and descriptive reasons do not establish the cause; **cause unresolved**. T19's original appendix is preserved.

Founder outcomes remain separate: T18 19/19 at 44,978; T17 selected 19/19 at 44,720, simultaneous 19/19 at 45,290 and
cohort 5/6; T16 simultaneous priority cancellation 4/19, 71 positively owned soil blocks plus its original named dirt
unit, operation two unstarted. T16 `raw/cohort-founder-custody-proof.json` preserves that negative. A later success
does not retrospectively repair it or the historical incomplete saved Great 43/57.

## Retained uncertainty and authority

Earlier supplied mining preparation/death timeouts, regression and tier-two timing failures remain unexplained in part.
T11's immediate gallery-completion death endpoints conserve eight cobble as seven stored plus one named ground unit;
later physical recovery is separate evidence, not demonstrated material loss or an erased failure. The eighteen-unit
food failure at 10,323 remains unexplained. T12's two concurrent preparation/mining timeouts remain. See the retained
[technical history](stage-1-design.md) and T09–T12 reports for accepted food/clay waves and timing limits.

The physical planner checks a 16-block horizontal radius, depth 1..6 and a 64-edit ceiling, but its fixed gallery has
at most sixteen cells; no arbitrary-vein or all-64 campaign is claimed. Natural-origin authority is witnessed and
revoked by player writes, including same-state writes. One-unit hands, named custody and independent conservation
remain mandatory. Mature-gated mining cannot provide unattended pre-Mature clay. Supplied development is distinct
from unattended development. Survey site/fit counts exclude walking routes and dig timelines.

Other paid-stall/retained-building and persistence gaps, mixed chunk-boundary issues, long-session/multiple-colony and
abrupt-crash coverage remain open. Scoped normal reloads prove only their exercised state. Tier-three construction and
effects, established Young/Mature generation, Great/120-adult performance and complete release acceptance are unmet.
No fresh Stage-1 clean-client launch or required Survival images were supplied; [historical gallery](screenshots/t28-release.md)
labels and previous-model qualifications remain intact.

The owner's only relocation sequence is `(-4,2,1) -> (-9,8,0)`, then `(-4,2,0) -> (-10,7,0)`. No third relocation or broader
clearing is granted. Retained-nurse counting during UNKNOWN, existing miner cargo waits during loaded damage, and named
mining recovery **6,000 nursery-loaded ticks within 60,000 total**, with unchanged **600/200** windows, remain.

## T20 command and release scope

After documentation drafts, T20 runs one final `checkFast assemble --console=plain --rerun-tasks` through the pinned-Java
evidence wrapper in a child PowerShell process. Prelaunch inventory and Python harness checks precede it. Fresh unfiltered
XML, exact discovery, assignments, source/compiled input correspondence, exits, completion/shutdown markers, asset,
public-path, harness, retention and client-validator guards settle fast acceptance. Both `deleteGameTestRunDir` and
`acceptGameTestEula` must actually guard-SKIP; protected client inventories are compared before/after. Retention keeps
the newest ten worlds plus every active lease; compact evidence is retained without world copies.

Runtime/source jar hashes are recorded in external T20 `raw/packaging.json` and compared with T19. Current jar filenames
still say 0.1.0; the runtime metadata says 0.1.0 and the sources retain their original `${version}` template. They are
**unreleased master artifacts**, separate from the accepted [0.1.0 release jar](slice-1-report.md). Packaging proves no release.
All production, client, tests, fixtures, observers, scripts, configuration and assets remain byte-unchanged from T20's
baseline; no long body is affected. T18 remains the complete valid-red checkpoint, with no new Great/long/client campaign.

The recommended next work is a separately bounded Stage-1 completion follow-up before Stage 2, starting with Great's
unresolved deadline failure and keeping negative evidence and physical guards. The twenty-turn direction is exhausted;
this handoff does not request T21, promise a repair or accept 0.2.0.
