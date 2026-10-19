# Testing

Prime Ants is tested mostly inside a real Minecraft server: tests spawn colonies and let them live for thousands of
ticks, then check what the ants actually did.

## Test suites

| Suite | What it covers | Size |
|---|---|---:|
| Unit tests | rules and calculations without a world (JUnit) | 124 |
| Model tests | the ant model's geometry and textures | 26 |
| Fast in-game tests | short GameTests: founding, food, nursing, defence, saving | 78 |
| Long in-game tests | whole stretches of colony life: growth, stages, chambers, mining, surface building, reloads | 311 |

## Running them

| Command | Runs | Time |
|---|---|---|
| `./gradlew checkFast` | unit, model and fast in-game tests, plus the build checks | a few minutes |
| `./gradlew checkLong` | all long in-game tests | hours |
| `./gradlew build` | everything, then packages the jar | hours |

Run `checkFast` before every commit. Run `checkLong` after changing shared colony behaviour, and before a release.

The long tests run on several game servers at once. By default there are 4 shards and 4 servers running together.
Change this with `-PprimeAntsGameTestShards=N` and `-PprimeAntsGameTestConcurrency=N`. Each server needs about 1.7 CPU
cores and 5.5 GB of RAM.

To run a few long tests by name while debugging, pass them with `-PprimeAntsLongNames=...` to `runLongGameTests`. This
is for investigation only: a release needs the complete suite.

Test reports are written as JUnit XML under `build/test-results/`.

The default GameTest environment disables natural monster spawning. Finite food ledgers count every edible unit in
the fixture area, including rotten flesh; ambient hostile loot previously introduced an unbudgeted unit into a
mining fixture. Explicit mobs, normal damage and vanilla death drops remain enabled and have a fast regression.
This setting belongs to the test resources and does not change normal worlds. Fixture supplies and recovery limits
are unchanged. Food ledger diagnostics retain both original total evaluations and record their actual operands.

## Known flaky long tests

Long tests play out long stretches of real colony life, and the order in which ants act differs a little from run to
run. A few tests are sensitive to that order: usually they pass, but sometimes the colony does not finish its task
within the test's tick limit.

| Test | What it checks | Tick limit |
|---|---|---:|
| `forager_scaling_game_test_large_colony_foragers_carry_at_once_with_exact_accounting_and_claims_survive_reload` | many foragers carry food at once; every unit is counted; claims survive a reload | 30,000 |
| `surface_game_test_supplied_founding_to_mature_retains_its_small_mound_and_workers_build_larger_crests_and_an_open_arch` | a colony grows from Founding to Mature and builds its Mature mound | 60,000 |
| `lifecycle_game_test_environmental_chicken_loot_funds_new_worker_then_queen_death_ends_colony` | food from a chicken raises a new worker; the queen's death ends the colony | 26,000 |
| `mature_colony_game_test_upgrade_counts_every_clay_unit_in_each_place_through_its_builder_death` | a chamber upgrade accounts for every clay unit, even when its builder dies | 60,000 |
| `mining_game_test_mid_haul_disk_reload_resumes_then_miner_death_releases_one_unit_exactly_once` | a miner's haul survives a reload; after its death the unit is released exactly once | 60,000 |
| `mining_game_test_full_store_prevents_removal_then_physical_stock_release_and_two_evaluations_resume_mining` | a full store pauses mining; physical stock release and two evaluations resume it | 60,000 |
| `nursery_upgrade_game_test_young_colony_reaches_mature_by_real_play_then_rebuilds_its_nursery_in_packed_clay_and_its_brood_develops_faster` | a Young colony reaches Mature and rebuilds its nursery in packed clay | 60,000 |
| `tier_three_game_test_physical_oversized_inventories_survive_in_memory_reconstruction_and_capacity_loss` | over-full stores survive being rebuilt in memory and losing capacity | 20,000 |
| `colony_stage_game_test_unavailable_entrance_never_hides_loaded_broken_wall_and_the_colony_regresses` | a broken wall behind a blocked entrance is still noticed, and the colony regresses | 24,000 |
| `nursery_upgrade_game_test_upgrade_keeps_every_unit_through_reload_and_killed_builder_and_never_converts_player_changed_wall` | a nursery upgrade keeps every unit through a reload and a killed builder, and never converts a wall the player changed | 60,000 |

All names are in the `prime_ants_test` namespace.

**Release rule.** A timeout in one of these tests does not block a release, provided the same test passes in other
complete runs. Anything else blocks it:

- a failed check, even inside these tests (for example the miner test's 6,000-tick recovery check);
- an error or a wrong result;
- a timeout in any other test.

If one of these tests starts failing in a new way, it comes off this list and gets investigated.

Only the exact outer timeout at the original tick limit qualifies. A focused pass or a pass with an extended limit
does not replace the required pass in another complete run. The earlier lifecycle assertion remains unexplained;
later passes do not establish its cause.

The full-store mining test has an exact native pass in another complete run, so its later 60,000-tick outer timeout
qualifies under this rule. Its timing variability remains unexplained; a different failure still needs investigation.
The miner death test's inner 6,000 nursery-loaded-tick recovery assertion remains a blocking failure. Neither its
listed outer timeout nor a focused pass qualifies that assertion.

A recovery diagnostic also failed food conservation before mining, and
`forager_scaling_game_test_scaled_foragers_grow_colony_past_thirty_adults` reached an unlisted 60,000-tick timeout.
Both remain blocking; neither is qualified by the timeout rule.

Live shell regressions compare the solid-and-dry predicate with separate world reads and cover waterlogged blocks,
unavailable cells, fault order and repairs in the same tick. They complement the physical nursing, delivery and
generated-colony tests, including pending legacy nursery initialization.
