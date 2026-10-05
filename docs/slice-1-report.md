# Slice 1 current assessment - T12, 2026-10-05

**Cross-role ownership is repaired in the exercised regressions, but current acceptance is RED.** The final unfiltered build (`t12-build-final-recovered-6`) exited 1 after 149.807826 seconds: **15 unit passed / 96 server executed (95 passed, 1 failed) / 24 model passed**. All 90 original server cases remain present, with six additions. The retained `expansion_game_test_builder_death_uses_custody_and_actual_worker_reassignment` timed out at its unchanged 26,000-tick deadline. This is a gameplay blocker; this report is a current partial result, not milestone sign-off. No T12 native capture or performance server was launched.

Full commands, failures, interpretation limits and artifacts are in [Report T12](C:/Users/user/Documents/turnloop/directions/prime-ants-slice1/turns/T12/report.md). Final executed XML and inspected binary/source jars are in [final-executed](C:/Users/user/Documents/turnloop/directions/prime-ants-slice1/turns/T12/final-executed/). Production archives contain no development test/capture/restart classes. Commit identity is recorded externally in T12/commit-evidence.json.

## Original outcomes 1-5

| Original outcome | Current assessment and evidence |
|---|---|
| 1. Pinned Fabric project, green unit/server build | Project compiles on Minecraft 26.3, Java 25, Gradle 9.7.1, Loader 0.19.5, API 0.161.0+26.3, Loom 1.18.2. Unit/model checks pass, but the latest full server suite is red. [Executed XML](C:/Users/user/Documents/turnloop/directions/prime-ants-slice1/turns/T12/final-executed/server.xml) and [command/exit metadata](C:/Users/user/Documents/turnloop/directions/prime-ants-slice1/turns/T12/t12-build-final-recovered-6.json). |
| 2. Real queen, brood-derived workers, digging, physical food and starvation consequences | Debug-egg founding, finite first-clutch reserve, real emergence/death, physical dropped-food foraging/nursing, finite larval nutrition and bounded worker excavation are demonstrated by retained tests and historical native saves. T12's six new cases pass in the final full suite. Continued construction after builder death is not reliably demonstrated by that suite. Natural food harvesting, adult hunger/lifespan/mortality and unattended survival remain absent. [Role regressions](../src/gametest/java/dev/primeants/gametest/RoleConflictGameTest.java), [expansion cases](../src/gametest/java/dev/primeants/gametest/ExpansionGameTest.java), [nursing guards](../src/gametest/java/dev/primeants/gametest/NursingGuardGameTest.java). |
| 3. Anatomy, animation, caste scale, mandible cargo, EN/RU | Production model/textures/carrying offsets are unchanged; all 24 model cases pass. Observer mouth-region geometry is corrected and tested, but readable current cargo has not been photographed. [Model XML](C:/Users/user/Documents/turnloop/directions/prime-ants-slice1/turns/T12/final-executed/headless.xml), [model tests](../src/gametest/java/dev/primeants/gametest/AntModelGameTest.java), [observer geometry](../src/gametest/java/dev/primeants/gametest/CargoView.java). |
| 4. Actual colony close-ups, mound traffic, connected interior/brood, food carrying | Historical T11 mound/interior images support unchanged views with their original dates. Soil-carrying image is explicitly rejected for occlusion. Fresh colony worker/queen close-ups, readable food and unobscured soil carrying remain missing. No T12 PNG exists. See the visual assessment below. |
| 5. Current report, measured timing/performance and next direction | This report now leads with T12's current result and preserves history below. Default/accelerated duration definitions and historical measured ticks are distinguished. **MSPT and performance population are unknown**, since no permitted sample could follow the red acceptance. A compiled development sample driver is prepared but unexecuted. |

## Ownership, caregiver reservation and bounded loading evidence

The actual baseline failure is [03-failing-before.xml](C:/Users/user/Documents/turnloop/directions/prime-ants-slice1/turns/T12/03-failing-before.xml): at tick 10,220 an enabled empty-handed builder with an active partially built job also became the replacement forager after normal lethal damage to the original forager. Genuine nurses held food; no adult or cargo was manufactured for that state.

Queen selection and worker assignment now reciprocally reject incompatible tasks, claims and equipment. Both claims are published after assignment returns success. Nursing cannot take a forager/builder or any existing cargo; construction cannot take a forager/occupied job or overwrite cargo. A former builder can forage after actual completion/release. Loaded persisted conflicts release the incompatible forager owner or quarantine unsettled soil/foreign association, retaining cargo and edits. Missing entity lookup retains claims. Normal death of a quarantined soil owner accounts release from the claim rather than its corrupt phase. Exercised corrupt-save variants are a duplicate same-member forage claim and incompatible nursing phase with real soil; the foreign-association quarantine branch is compiled, not exhaustive corruption coverage.

The final suite executed and passed these new cases (full namespace `prime_ants_test:`):

- `role_conflict_game_test_dead_forager_cannot_steal_empty_handed_partial_builder`
- `role_conflict_game_test_restored_partial_builder_and_queen_keep_reciprocal_claims`
- `role_conflict_game_test_conflicting_persisted_forager_claim_releases_without_losing_builder_progress`
- `role_conflict_game_test_persisted_incompatible_builder_cargo_is_explicitly_quarantined`
- `role_conflict_game_test_disabled_nursing_labels_cannot_reserve_care_with_four_other_mature_workers`
- `expansion_game_test_completed_excavation_can_relieve_traffic_before_final_soil_delivery`

A remaining caregiver must be the canonical living loaded/ticking entity, mature, AI enabled, associated with the exact colony/home, nursing-authorized and independent of forage/construction claims. The proposed builder is excluded. The negative case has six actual emerged workers: two disabled original nursing labels and four other mature enabled workers. Thus the old four-worker threshold cannot hide an incorrect reservation.

These are bounded real-tick entity/actual SavedData restoration checks, not a new cold restart of nutrition/layout. Negative fixtures explicitly disable/park real ants on existing supported exterior cells and hold the genuine empty-handed partial builder during setup; the builder is enabled before the lethal-damage event. They preserve identity, cargo and physical accounting. This is not native photography or proof of unassisted movement of those held actors. Deadlines, material/origin/custody/nutrition assertions and the retained builder-death case are not removed or extended.

Already verified two-high completed floors may serve nurse circulation while excluding the next excavation and its adjacent work faces. Brood emergence waits for all twelve real removals. This widens the earlier circulation eligibility guard, not the nest plan, removal count, resource balance or excavation permission. The final retained builder-death failure means this has not resolved all continuation problems.

[55-final-builder-death-stall.json](C:/Users/user/Documents/turnloop/directions/prime-ants-slice1/turns/T12/55-final-builder-death-stall.json) reads the failed save: two removals, zero deposits, two released soil units, a real replacement builder still claimed in DIG with empty equipment. The final queen snapshot reports incomplete operational opening. These files are after GameTest shutdown/cleanup; that readiness value is an observation, not an established live root cause. The final world is preserved as T12/final-failed-server-world.zip. No sign-off is based on a claim counter alone.

## Timing and performance

| Timing | Production default, multiplier 1 | Declared development/capture settings |
|---|---|---|
| Egg / larva / cocoon, each | 12,000 loaded ticks = 10 minutes at 20 TPS | Brood multiplier 100: 120 ticks each |
| Egg to worker emergence, excluding care/space stalls | 36,000 loaded ticks = 30 minutes nominal | 360 ticks nominal |
| Callow darkening | 4,800 loaded ticks = 4 minutes | 48 ticks |
| Laying cadence | 1,200 loaded ticks = 60 seconds | 12 ticks |
| Excavation/deposition action cadence | 200 loaded ticks = 10 seconds | Work multiplier 20: 10 ticks |

These definitions are covered by [15 time-scale unit cases](../src/test/java/dev/primeants/time/SimulationTimeScaleTest.java). They are not measurements of a default-speed whole colony. Historical T11 A4 construction took **2,851 loaded construction ticks**, nominally 142.55 seconds at 20 TPS, at work multiplier 20; its full native task took 619.217 seconds. T12 has no new native duration measurement.

A read-only audit of the verified T11 A4 source confirms its **persisted stage duration 120 and six saved callow durations 48**: [18-persisted-timing.json](C:/Users/user/Documents/turnloop/directions/prime-ants-slice1/turns/T12/18-persisted-timing.json). Those accelerated saved durations would affect a replay even if current settings changed.

**Performance: unmeasured; no median/p95/max MSPT, sampled N, loaded-chunk range or active/idle classification is available.** T11's saved one queen + six workers is a historical population, not a measured performance window. The 96 GameTests are not a 20-ant or multi-colony benchmark. No scalability kill criterion is inferred from unknown performance.

[Sample driver](../scripts/sample-performance.py) and the narrowly extended [dedicated harness](../src/gametest/java/dev/primeants/gametest/RestartHarness.java) compile. The unexecuted design uses an untouched owned copy of verified T11 A4, normal 20 TPS without sprint, at least 200 loaded warmup ticks then 1,200 native timing samples, per-tick population/tasks/chunks/ages, normal physical apple/chicken inputs and ordinary expiry. It reads the preceding completed native timing ring at Fabric START (`current tick - 1`); pinned sources place that event after tick-count increment. The last change only removes unlimited food lifetime from this unexecuted performance branch; targeted compile exit 0, T12/t12-58-unexecuted-harness-compile.json. This is preparation, not runtime validation or performance data.

After a new completed green unfiltered build, the intended command is:

```powershell
python .\scripts\sample-performance.py --attempt t12-performance-a1 --evidence C:\Users\user\Documents\turnloop\directions\prime-ants-slice1\turns\T12\performance-a1 --acceptance-name <fresh-green-evidence-name>
```

The driver refuses a missing/red acceptance record. It was not invoked in T12. [Observed hardware](C:/Users/user/Documents/turnloop/directions/prime-ants-slice1/turns/T12/16-hardware.json): Ryzen 7 7700, 8 cores / 16 logical processors, 102,130,163,712 bytes installed RAM; Gradle helper pins Temurin 25.0.3.9. No performance JVM exists to report a sampled heap/load context.

## Visible evidence and original dates

The following actual historical PNGs were opened again in T12; none is relabelled as a T12 capture. Original run **7544f303-8d9d-4e0a-9203-4ff2faa47ace**, original queen **0915c15f-8000-482e-8ccf-fc6caba553e1**, native ordinary founding/growth, spectator with disclosed night vision; [original provenance](screenshots/t11-a4-provenance.json).

| File | Original UTC capture completion | T12 visual inspection |
|---|---|---|
| [T11 mound](screenshots/t11-a4-expanded-mound.png) | 2026-10-05 05:07:29.729738900Z | Mound, entrance lane and multiple real ants visible. Historical support for entrance/traffic view, not a current-code native run or close-up completion. |
| [T11 interior](screenshots/t11-a4-usable-interior.png) | 2026-10-05 05:07:32.242472200Z | Chamber and real adults visible; a pale brood cluster is visible but partly overlapped. Supports an interior view; a clearer connected tunnel/brood overview remains useful for review. |
| [T11 soil carrying](screenshots/t11-a4-soil-carrying.png) | 2026-10-05 05:06:12.885635600Z | **Rejected**: foreground grass/terrain hides the mouth/item region and part of the worker. Render-extraction metadata does not establish readable cargo. |

[13-saved-mouth-region-probe.json](C:/Users/user/Documents/turnloop/directions/prime-ants-slice1/turns/T12/13-saved-mouth-region-probe.json) reads an owned copy of genuine T11 A3. It finds real dirt equipment and tests the production-derived item region against saved voxels; source archive is unchanged. It is geometry evidence only. The observer now targets the worker item at approximately 0.528125 blocks forward / 0.2135 high, checks a region rather than a centre ray and considers foliage. Renderer offsets/adult design are unchanged. Prepared separate colony close-ups and food view have not run; no visual acceptance is inferred from this code or metadata.

## Known limits and next direction

T11's 4-to-1 original-room occupancy change is redistribution, **not demonstrated feeding throughput**. Its final three larvae all lacked sugar; one also lacked protein. Two retained protein stores do not make those larvae nutritionally supported. T12's read-only saved NBT audit preserves that distinction.

Next: diagnose the final retained builder-death continuation with its preserved save and smallest filtered command before another full workload. Establish whether live route/readiness or cross-fixture restoration is responsible; final post-cleanup data alone cannot settle this. Then obtain genuinely green unfiltered acceptance, archive every actual client attempt immediately, obtain and open readable colony/cargo views, and measure the one proportionate normal-TPS sample. **Phase 2 remains pending independent original-outcome review.**

Cold restart of new nutrition/layout, arbitrary unloading, offline catch-up, natural food supply, adult mortality/lifespan and larger-population/multi-colony performance remain missing or unmeasured. Quarantined corrupt persisted states are explicitly blocked rather than silently repaired; no general recovery framework or cargo manufacture is added.

T12 adds **zero client launches**: lifetime remains **24; native 16/8, tasks 11/13**. It launched **39 server GameTest JVM/tasks (23 passing, 16 failing)**, separately from headless model JVMs. No restart/performance server launched. One full workload was mistakenly started after a failed focused check before inspecting its exit; this process deviation is explicitly detailed in T12/report.md. No timeout/RAM increase, removed assertion, filter in build, history rewrite, push or owner-world change was used.

---

## Historical reports through T11

The text below is retained historical evidence. Its earlier milestone/status claims are superseded by the T12 assessment above.

# Current result after T10 - physical food supports continuing brood

One generated-world production queen now raises three original workers, forages for player-dropped food, and receives physical nurse feeding. Six apples and eight raw chickens yielded **four live workers**, a different pale callow and another laying event in the reused pile. Saved NBT independently balances **14 supplied = 6 physical + 8 consumed** (five apples / three chickens consumed). Queen founding reserve stays zero; finite recipient stores fund four new eggs and supported larval development.

Final unfiltered `.\gradlew.bat build --console=plain --rerun-tasks`, exit 0, 74.718 s: **15 unit / 77 server / 24 model**, retaining every earlier case. `.\gradlew.bat runClientGameTest --console=plain -PprimeAntsCapturePrefix=t10-a3 -PprimeAntsBroodMultiplier=100`, exit 0, 462.388 s, run `e530f5cf-9ac5-491b-8675-fdd87c044293`: four fresh images. See [nurse carrying](screenshots/t10-a3-nurse-carrying.png), [feeding action](screenshots/t10-a3-nurse-feeding.png), [new brood with mature workers](screenshots/t10-a3-new-brood.png), [additional pale worker](screenshots/t10-a3-additional-callow.png) and [provenance](screenshots/t10-a3-provenance.json). Sources/jars stayed frozen across capture. No summoned adult, reserve/cache/nutrition injection, forced stage, paused AI or cutaway.

Nursing uses the existing single worker action owner, real cache withdrawal and a 20-tick physical feeding action. Apple/berries/chicken yields are 4,000 sugar / 2,000 sugar / 8,000 protein; queen capacities 8,000/16,000; new egg costs 1,000/2,000; larval requirements 4,000/8,000. Laying is every 1,200 loaded ticks, with three physical slots and cap 30 including the queen. Unknown unloaded member identities occupy capacity. Original/Consumed history and death tombstones prevent restored brood or dead workers duplicating adults. Queen death stops laying but preserves open habitat and viable fed cocoons.

Final-code active-home loading check: one JVM 16248, home (3,-3), carrier (13,-3), flags 14 / radius 2 / level 31. Queen, real home worker and nursery each advanced exactly **400** while saved carrier lookup/loading/ticking were absent. Same claim and named cargo survived; native reload restored the same UUID/task/age and resumed delivery. Disclosed post-pickup and return positioning isolates this fixture only. It is running-process unloading, not a new cold restart. Home consumed zero units during the interval; accounting includes real nurse cargo plus saved carrier stock. Broad restart, offline and crash behavior remain unproved.

Two failed client tasks preceded acceptance: A1 exact wall-face containment and A2 native grass spreading onto a dirt seal. Targeted tests established recovery: inclusive body containment admits contact but rejects 0.001 crossing; newly carried plugs use existing nest soil, retaining legacy dirt readability and every-write ownership revocation. A retained serialization fixture also now saves the obstruction's actual chunk when it crosses the pile's chunk face. No conservation/protection assertion was removed. Full failure/recovery ledger, lost raw A1-world disclosure, source audits, exact identities, commands and commit are in external `turns/T10/report.md` / `commit-evidence.json`.

Client history is **20 launches: native 14/6, tasks 10/10**. T10 added three native exit-0 clients (A2 closed normally for diagnosis), one green and two failed tasks. Dedicated-server outcomes are separate in the T10 report. Dropped-food-supported growth is not unattended survival. Worker expansion is next; natural harvesting, adult hunger/lifespan, performance, broad restart/offline and Phase 2 remain unresolved.

---

# Report T02


## What was done

Delivered production registered Lasius niger worker and queen adults, one restricted debug queen spawn egg, production models/atlases/EN-RU strings, real-tick behavior tests, model tests, and fresh generated-world captures. The colony remains unimplemented. Debug adults are the actual living entities; there is no population counter, automatic replacement, worker egg, test-only adult factory or queen-produced worker.

Baseline: clean `master` at `c7fe3127e9920a410986a7aee2459e0106375eb7`. Commit: recorded after commit in external `commit-evidence.json` (this document does not try to embed its own commit hash). Minecraft 26.3, Java 25, Gradle 9.7.1, Loader 0.19.5, API 0.161.0+26.3 and Loom 1.18.2 are unchanged. No old implementation was imported or owner world edited.

`LasiusNigerEntity` is a persistent `PathfinderMob`; immutable form comes from registered type. It uses ground pathfinding, collision, gravity, health/damage and the normal death sequence. `AntGroundNavigation` waits within 0.15 blocks of each waypoint to avoid prematurely cutting the queen into corners. Stroll chooses safe land positions at modest speed and does not permanently stop at 100 idle ticks. Wandering is not founding or foraging.

Only the living server entity advances `AntElapsedAgeTicks`, once per inherited real tick; renderer state cannot mutate biological state. Vanilla saves UUID and entity type, additional fields save form/age, loading validates registered form and never inserts another entity. Normal server UUID insertion rejects a duplicate. Age freezes outside loaded ticking; no offline catch-up or lifespan is implemented.

The queen egg uses the real spawn-egg item interaction after checking creative mode or operator level 2+ (`Permissions.COMMANDS_GAMEMASTER`), fixed queen type and absence of a spawner block entity. Dispenser behavior is `NOOP`; no recipe or loot exists. Authorized use creates one queen and no workers/nest. Worker diagnostics use vanilla's operator `/summon`. `/give @s prime_ants:debug_lasius_niger_queen_egg` obtains the diagnostic item; no custom command is added.

## Commands run (with exit codes)

All Gradle commands used the existing evidence helper in the workspace, Temurin Java 25, separate invocations, immediate `$LASTEXITCODE` checking, and unchanged time/memory settings. Each evidence ID has full `.json` timing/command metadata and native `.log` output under T02. Native logs were decoded according to BOM before parsing.

| Evidence ID | Actual command | Exit | Seconds |
| --- | --- | --- | --- |
| `06-compile-production` | `.\gradlew.bat compileJava compileClientJava --console=plain` | 1 | 1.715 |
| `07-compile-production-recovery` | `.\gradlew.bat compileJava compileClientJava --console=plain` | 0 | 2.061 |
| `08-compile-tests` | `.\gradlew.bat compileGametestJava --console=plain` | 1 | 2.055 |
| `10-server-targeted` | `.\gradlew.bat runGameTest --console=plain` | 1 | 11.546 |
| `11-navigation-diagnosis` | `.\gradlew.bat runGameTest --console=plain` | 1 | 11.913 |
| `14-navigation-recovery` | `.\gradlew.bat runGameTest --console=plain` | 0 | 12.448 |
| `16-compile-client-tests` | `.\gradlew.bat compileGametestJava --console=plain` | 0 | 2.103 |
| `t02-build-final` | `.\gradlew.bat build --console=plain --rerun-tasks` | 0 | 14.145 |
| `t02-build-final-assets-corrected` | `.\gradlew.bat build --console=plain --rerun-tasks` | 0 | 14.807 |
| `18-client-capture-attempt-2` | `.\gradlew.bat runClientGameTest --console=plain` | 0 | 29.886 |
| `t02-build-final-capture-separated` | `.\gradlew.bat build --console=plain --rerun-tasks` | 0 | 14.475 |
| `20-client-capture-attempt-3` | `.\gradlew.bat runClientGameTest --console=plain` | 1 | 43.397 |
| `21-idle-cutoff-reproducer` | `.\gradlew.bat runGameTest --console=plain` | 1 | 11.422 |
| `23-idle-goal-recovery` | `.\gradlew.bat runGameTest --console=plain` | 0 | 12.615 |
| `t02-build-final-idle-fixed` | `.\gradlew.bat build --console=plain --rerun-tasks` | 0 | 14.680 |
| `24-client-capture-attempt-4` | `.\gradlew.bat runClientGameTest --console=plain` | 0 | 26.189 |

The **last** full build was `t02-build-final-idle-fixed`, a fresh `build --console=plain --rerun-tasks`, exit 0; it executed 15 unit and 16 server cases. The next dedicated client command was `24-client-capture-attempt-4`, exit 0; it executed two client entrypoints, 14 model cases and six walking captures. Documentation/artifact checks followed without more source changes.

Other commands: Git baseline/status/history reads exit 0; source extraction batches exit 0 (missing names recorded, then resolved or identified as old API names); matching rendering/object-builder source downloads via `curl.exe -fL`, both exit 0; `python scripts/generate-ant-textures.py`, exit 0; artifact verifier first exit 1 due RU JSON decoding, targeted recovery exit 0 (`26-artifact-verification-recovery.log`); `git diff --check`, exit 0. One early source-excerpt query ended with `StopIteration` because `Model` was not yet extracted; it was subsequently extracted/read. Initial Russian document output used the wrong PowerShell encoding and was reread as UTF-8 before design work.

## Artifacts (paths)

Evidence root: `C:\Users\user\Documents\turnloop\directions\prime-ants-slice1\turns\T02`.

- Full external handoff: `T02/report.md`; final commit hash and exact changed files: `T02/commit-evidence.json`.
- Source readings: `01-api`, `02-test-api`, `03-matching-api`, `04-registration`, `05-fixture`, `09-clock-camera`, `12-camera`, `13-path-navigation`, `15-capture-api`, `22-land-goal` reading logs/manifests; full extracted files under `inspected-sources/`; extraction command in `inspect_sources.py`.
- Final copied XML: `final-test-results/test/TEST-dev.primeants.time.SimulationTimeScaleTest.xml`, `final-test-results/gametest/server.xml`, `final-test-results/client-model/model.xml`.
- Failed server XML: `10-server-failed.xml`, `11-navigation-failed.xml`, `21-idle-cutoff-failed.xml`. Earlier client model XML and capture provenance retained per attempt.
- `artifact-verification.json`: command ledger, testcase names, XML hashes, complete jar entry lists, PNG hashes/freshness, source creation timestamps and final capture provenance. `verify_artifacts.py` is reproducible verification.
- Production code under `src/main/java/dev/primeants/entity`, `.../item`, and `src/client/java/dev/primeants/client`; tests only under `src/gametest`; texture generator under `scripts`.
- Documentation: `docs/dev-notes.md`, current and accepted historical results in `docs/slice-1-report.md`, and one caption per PNG in `docs/screenshots/README.md`.
- Final frames: `docs/screenshots/t02-a4-worker-walking-{1,2,3}.png` and `t02-a4-queen-walking-{1,2,3}.png`. Nine earlier T02 PNGs are retained with their run limitations. T01 PNG unchanged.

Production archives inspected after the final build:

- `C:\Users\user\Documents\prime-ants\build\libs\prime_ants-0.1.0-sources.jar`: 59908 bytes, SHA-256 `f1bbb208f6426f36ea778a82f1b82bc40947cfd80388b782ee41c70a065c5d24`; no development entries.
- `C:\Users\user\Documents\prime-ants\build\libs\prime_ants-0.1.0.jar`: 70731 bytes, SHA-256 `944f2d475ae37cfe3a927c10327d3d0cfecf73087702b1b05241e8102f5a1813`; no development entries.

## Claims and their evidence

### API sources and chronology

`01-api-read.log` started `2026-10-04T10:42:59Z`, before new entity/item files were created at `10:47:14Z`. Client model files were created at `10:49:51Z`. `artifact-verification.json` preserves Windows creation timestamps. Logs embed the exact extraction command, start/end, archive entries and source SHA-256 hashes; later numbered reads precede corrective edits. Relevant sources include Mob/EntityType/SpawnEggItem/GroundPathNavigation/SynchedEntityData/ValueInput/ValueOutput/PigRenderer/PigRenderState/PigModel, model builders and both registry APIs. The initial broad cache scan also read old Fabric jars, so inspection was narrowed and replayed against exact pinned sources in `03-matching-api-read.log`. Original logs/manifests remain; use matching replay for overwritten API extracts.

Resolved source archives: the common/client 26.3 generated jars given in the brief; matching API/Loom jars in `C:\Users\user\.gradle\caches\prime-ants-source-inspection`. Rendering sources `27.0.14+901a437c5d` and object builder sources `24.1.9+3434d6d95d` were retrieved from `https://maven.fabricmc.net/net/fabricmc/fabric-api/` with hashes in `03-source-downloads.json`. No version pins were changed. Applied API differences are documented in dev notes.

### Executed tests

Retained **15 unit cases**, `SimulationTimeScaleTest` (4 basic durations, 6 invalid multiplier cases, 5 invalid duration cases). No accepted unit assertion or bootstrap assertion changed. `failOnNoDiscoveredTests` and T01 failure propagation/discovery guards remain. Build verifies every ant server test by name alongside bootstrap, and rejects failed/skipped reports.

**16 server GameTests**, no failures/skips:

- `prime_ants_test:ant_entity_game_test_creative_egg_creates_exactly_one_queen`
- `prime_ants_test:ant_entity_game_test_debug_egg_cannot_configure_spawner`
- `prime_ants_test:ant_entity_game_test_operator_survival_egg_creates_exactly_one_queen`
- `prime_ants_test:ant_entity_game_test_survival_egg_creates_nothing`
- `prime_ants_test:ant_entity_game_test_null_player_egg_cannot_spawn`
- `prime_ants_test:bootstrap_game_test_food_item_lifecycle`
- `prime_ants_test:ant_entity_game_test_dispenser_cannot_bypass_egg_restriction`
- `prime_ants_test:ant_entity_game_test_queen_age_ignores_daylight_changes`
- `prime_ants_test:ant_entity_game_test_worker_age_ignores_daylight_changes`
- `prime_ants_test:ant_entity_game_test_queen_entity_serialization_preserves_identity`
- `prime_ants_test:ant_entity_game_test_worker_entity_serialization_preserves_identity`
- `prime_ants_test:ant_entity_game_test_queen_lethal_damage_has_no_replacement`
- `prime_ants_test:ant_entity_game_test_worker_lethal_damage_has_no_replacement`
- `prime_ants_test:ant_entity_game_test_queen_wanders_after_long_idle`
- `prime_ants_test:ant_entity_game_test_worker_navigates_ground_around_wall`
- `prime_ants_test:ant_entity_game_test_queen_navigates_ground_around_wall`

Navigation observes actual movement over multiple real ticks, detouring around a three-high wall and checking collision every tick. Age compares exactly 12 elapsed server ticks across daylight jumps to 9,000,000 and back to zero. Normal lethal damage removes both forms from living queries, then UUID lookup after the vanilla death sequence, without replacement. Entity serialization/restoration round-trips UUID/type/form/age, does not insert during deserialization, rejects a second identical UUID, and resumes age through real ticks. Egg tests exercise production `ItemStack.useOn`; allowed creative and survival operator create exactly one queen; unauthorized survival/null/spawner use creates none. An actual powered dispenser retains the egg and creates none. The long-idle queen test reproduced the goal failure before production correction and passed afterward.

**14 client model cases**, 7 per form, using isolated baked instances of the exact factory registered for the renderer: `sixArticulatedLegsOnMesosoma`, `elbowedAntennaeMandiblesAndEyes`, `onePetioleAndBodySegments`, `tripodsAlternateInActualSetupAnim`, `stationaryLegsStopWhileAntennaeAnimate`, `bakedUvDensityIs32TexelsPerWorldBlock`, `renderedBodyDimensionsMatchForm`. These inspect geometry-bearing hierarchy/UV polygons and execute the real `AntModel.setupAnim`; no parallel anatomy checklist is used. Capture uses registered `AntRenderer` and never sets ant poses. Client suite guard requires 14 passing model cases and six capture records.

No GameTest calls entity or simulation `tick()` manually. Fixture blocks and navigation destinations are controlled only in server tests. Serialization is **an entity serialization test in a running server, not a full world-restart test**.

### Dimensions, UV scale and gait

| Form | Rendered axial body length (jaws/acidopore included; antenna/leg reach excluded) | Body thickness | Collision width × height |
| --- | --- | --- | --- |
| Worker | 1.025142 blocks | 0.226563 | 0.60 × 0.40 |
| Queen | 2.271376 blocks | 0.437500 | 0.95 × 0.70 |

Resting ground-to-body-top is approximately 0.36 / 0.61 blocks. The queen has a larger mesosoma and four wing-scar details. Both forms have elbowed antennae, mandibles, compound-eye surface pixels/geometry, head, mesosoma, one petiole, gaster, acidopore and six articulated femur/tibia/tarsus legs. Visible appendages and the long queen body extend beyond compact axis-aligned collision footprints. Both fit vertically under future two-block-high passages; full passage routing and visual clearance around bends are untested.

The ant subtree scales raw geometry 0.5; vertex conversion divides by 16. Thus `w` raw model/UV units cover `w/32` world blocks and `w` texels, **32 texels per world block**, twice vanilla's 16. Both atlases are 256×128; atlas size alone is not the claim. Client cases measure baked UV texels divided by transformed world-edge lengths and assert 32. Item texture is 16×16.

Tripod A: left front/hind + right middle (0,2,4); B: left middle + right front/hind (1,3,5). Actual walk distance/speed drives phase/amplitude; observed horizontal displacement gates locomotion. Stationary legs return to rest immediately while antennae continue visual animation. Shape cases check both opposing phases and stationary reset. Rendering neither owns nor increments elapsed biological age.

### Capture provenance

Caption for every new PNG: **T02 debug entity specimens; colony not implemented**. Final attempt 4 freshly generated normal terrain, seed `2026100402`, creative operator for debug use, then spectator observer. Queen birth used the normal client item-use packet; worker birth used the ordinary permission-checked player summon command. Existing ground selected without edits; specimens never teleported or had AI/physics disabled. No decoration, forced poses, weather/time adjustment or alternative spawning/rendering occurs. Observer position/HUD/FOV changed; Fabric's disclosed default render distance/cloud/music settings remain.

- **Worker** UUID `b6dbc54f-b790-40fb-856a-d998af428dd1`; creation route: `ordinary operator /summon prime_ants:lasius_niger_worker -14.50 69.00 6.50`. Created at server game tick 61, elapsed age 1. Walking frames at server ticks 96, 102, 108 / ages 36, 42, 48. From first to third frame, horizontal displacement 0.8424 blocks in 12 ticks. Positions [-14.399417238711827, 69.0, 6.600574659240077] → [-13.803730328901468, 69.0, 7.196259994981051]. AI, ground contact and normal physics observed active in all frames.
- **Queen** UUID `f428e324-bddc-48cf-95c1-cc2bb00a9ec5`; creation route: `production queen egg: client useItemOn -> packet -> ItemStack.useOn -> SpawnEggItem`. Created at server game tick 59, elapsed age 1. Walking frames at server ticks 116, 122, 128 / ages 58, 64, 70. From first to third frame, horizontal displacement 0.3739 blocks in 12 ticks. Positions [-9.560473028457066, 73.0, -12.490382755233302] → [-9.934329113352671, 73.0, -12.492358325157618]. AI, ground contact and normal physics observed active in all frames.

Final capture times lie between `2026-10-04T11:15:39.634352Z` and `11:15:41.489134500Z`. All six latest PNG mtimes are later than attempt 4 launch. Files were opened and observed to contain separate adult meshes, actual natural terrain, no HUD, and changing appendage positions. Dark chitin/terrain lighting and cuboid silhouettes remain visible; aesthetic/anatomical acceptance is left to independent review. These are not brood-produced population, nest, foraging, or completed colony-close-up evidence.

| Final PNG | SHA-256 |
| --- | --- |
| `t02-a4-queen-walking-1.png` | `bccda1bf50ba5e20a5769cb5ccfea42d96c45eacaa183f2d1735fa430923f6b8` |
| `t02-a4-queen-walking-2.png` | `993a592f7038afd5e1e149b64c93e50263d6cc0e6c11c8d0a3272ce95ea3f910` |
| `t02-a4-queen-walking-3.png` | `1a95272854dde5d6486b8e94498264ca1e55c6d1174d3c0c4b0b2ab94950a8f3` |
| `t02-a4-worker-walking-1.png` | `c3e9ce3629dcdd0183a3edf4ed0b96cd673140275c76691836ed4ce3951d8b13` |
| `t02-a4-worker-walking-2.png` | `d8445053fdfc0821128d14654ea66a5abf3dfe607dac060e8175adcad6d2d8fb` |
| `t02-a4-worker-walking-3.png` | `fe91c302d9df5989298e52648a63b0ae27f9b31f1c80c36a5dfc83cb62bafd15` |

T01 PNG SHA-256 remains `0a8a35b2fece3dd9dcbf5142dba3c3ea4e037ff2cd75097ca35d3f49b9508609`. The old T01 infrastructure capture class remains unregistered development source, so neither T02 launch invoked it or overwrote the image.

## Deviations, relaxations, skipped steps

- No definition-of-done assertion was intentionally skipped. Visual acceptance remains pending the independent reviewer.
- Two compile errors exposed removed API names: `displayClientMessage` (06, recovered 07) and `setDayTime` (08, corrected using 09 readings). They are preserved, not classified as infrastructure resets.
- Initial source scan mixed old Fabric cache archives with the exact versions. Matching-source replay corrected this before corresponding implementation; full initial logs remain. One excerpt query stopped on a not-yet-extracted Model. UTF-8 reread corrected initial document display.
- Two navigation tests failed (10). Diagnostic 11 showed corner sticking and default reach-range stopping. Production waypoint precision and exact fixture destination request recovered 14. Timeout allowances: original 220, diagnostic recovery 420, final worker 220 and queen 600 after a queen route took 417 ticks. This widens queen duration, not movement/collision requirements. No assertion was weakened or test disabled.
- Generic spawn-egg model resource was missing in 26.3; caught by archive inspection (17) before launch. Dedicated model/texture correction required another fresh build, not another unchanged client launch.
- Client attempt 2 exit 0 produced real mixed adult frames but partly occluded the worker; kept as diagnostic images. The next launch selected separate existing ground rather than moving ants into composition.
- Client attempt 3 exit 1, 43.397 s: worker captures succeeded, queen did not resume walking during 300 observation ticks. Full log, model XML, partial PNGs and provenance retained. A smaller server reproducer (21) failed at the same idle cutoff; the production goal correction passed (23) before the final full build/client resumed. No timeout/memory increase was used as recovery.
- Client attempt 4 exit 0, 26.189 s, after that targeted recovery. Cumulative launch history: **4 attempts, 3 process successes, 1 failure** including accepted T01. T02 adds 3 launches, 2 process successes, 1 failure. Attempt 2's composition limitation is explicit and is not independent visual acceptance. Two failed launches/captures did not occur, so screenshot stop criterion was not reached.
- Artifact verifier first failed decoding RU JSON through Windows cp1252 (25); fixed explicit UTF-8 and only reran verifier (26 exit 0). Native Gradle logs were BOM-detected. T01's parameter dependency failure remains resolved by `junit-jupiter-params:5.10.0` and its targeted-test success; retry log-decoding recovery is carried forward.
- OSHI/Perflib, missing empty resource directory, anisotropic-filter test default, Realms development token, Gradle/API deprecation messages persisted as non-blockers. Attempt 3 passenger-update warnings were preserved; its explicit blocker was autonomous-walking assertion. No global settings/VPN/authentication changes, installations, supervision edits, owner-world changes, destructive cleanup, push or history rewrite occurred.
- **Not tested/implemented:** full world restart, unloaded/offline catch-up, biological lifespan, brood replacement, founding, nests/digging, food collection/storage, full tunnel navigation, colony premise and performance at 20+ ants. No entity-performance or toolchain kill criterion is supported by these observations.

## Questions for the owner

None required for this authorized turn. The independent reviewer should decide whether the rendered anatomy, dark texture, silhouette and walking frames are visually acceptable before further colony work. Debug adult evidence does not answer the colony premise.

## Self-assessment against the definition of done

1. Production worker/queen entities, shared species logic, normal ground physics/damage/death, autonomous wandering, one server age owner and persisted identity/form/age: implemented and server-tested.
2. Restricted real queen egg, one queen/no nest/workers, no dispenser/spawner bypass, ordinary worker summon/no worker factory/egg: implemented, server-tested and production capture routes observed.
3. Production geometry, articulated gait, antenna animation, proportions, 2× measured density and EN/RU strings: implemented and 14 model cases pass. Dimensions documented separately; visual acceptance pending.
4. Accepted guards retained, real-tick movement/age/death/restoration/authorization tests added, named discovery enforced, latest full rerun build green, final XML preserved and jars exclude development code: verified.
5. Fresh separate generated-world close-ups and walking frames with UUID/routes/tick observations/hashes, AI/physics active, T01 preserved and old entrypoint disabled: final attempt 4 verified; earlier composition/failure evidence disclosed.
6. Dev notes, slice report, screenshot captions and full executor handoff updated; descriptive master commit recorded externally after staging/commit. Reviewer decides acceptance.

NEXT: continue - The real adult slice is evidenced; independent visual review and a later brood slice must still test the colony premise.

---

## Accepted T01 historical report

# Report T01

## What was done

The T01 infrastructure definition of done is fulfilled. A minimal Minecraft Java **26.3** Fabric project builds on Java **25**, with a checked-in Gradle **9.7.1** wrapper. Common (`src/main`), client (`src/client`), unit (`src/test`), and development GameTest (`src/gametest`) sources are separate. Production binary and source jars exclude tests and capture code. `en_us` and `ru_ru` resources exist; no gameplay text or content is introduced.

Pins verified against current primary sources: Loader **0.19.5** ([26.3 registry](https://meta.fabricmc.net/v2/versions/loader/26.3)); Fabric API **0.161.0+26.3** ([Maven metadata](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml)); Loom **1.18.2** ([Maven metadata](https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml)). No newer stable compatible patch was found in these version lines. The [official 26.3 template](https://raw.githubusercontent.com/FabricMC/fabric-example-mod/26.3/build.gradle) supplies the new `net.fabricmc.fabric-loom` configuration; its moving snapshot is replaced. Loader JUnit uses 0.19.5; `junit-jupiter-params:5.10.0` matches its published BOM. See [dev-notes.md](dev-notes.md) for exact sources, signatures, and 26.x API differences.

`SimulationTimeScale` converts durations to elapsed simulation ticks without depending on world daylight time. One day at multiplier 1 is 24,000 ticks; two days at multiplier 20 are 2,400 ticks. Nonpositive and non-finite multipliers are rejected. Fractional ticks round up. **Brood development remains unimplemented.**

The server bootstrap creates a vanilla apple entity in the isolated GameTest fixture, observes age advancing by 10 real server ticks, removes it, and verifies absence at elapsed tick 13. No entity/simulation `tick()` is manually called. Final unit and server suites passed. Temporary failing assertions proved that both suites fail `build`; both were restored before the final passing run.

One dedicated Fabric client GameTest automatically created a fresh normal terrain world, waited for rendered chunks, captured a PNG, left the world, and exited. The actual image was inspected for rendered gameplay. Caption: **T01 infrastructure capture; colony not implemented**. This image satisfies none of the four eventual colony views.

Baseline was clean `master` at `aade799a7f3d0ccccf3412025ae2c16c658fa12e`. This turn is committed on `master`; the exact resulting hash and changed-file list are recorded in the external `commit-evidence.json` after commit (the report cannot embed its own commit hash). Existing specification files, including `docs/00-decisions.md`, are unchanged.

Changed files: root `.gitattributes`, `.gitignore`, `build.gradle`, `gradle.properties`, `settings.gradle`, `gradlew`, `gradlew.bat`, the two `gradle/wrapper/` files; `scripts/Invoke-GradleEvidence.ps1`; the common/client entrypoints, time helper, unit test, two development GameTests, both mod metadata files, and EN/RU resources; this report, `docs/dev-notes.md`, and screenshot PNG/caption. Wrapper line endings are pinned for each platform and `gradlew` is executable in Git. No old project code was imported.

## Commands run (with exit codes)

All Gradle commands ran individually from `C:\Users\user\Documents\prime-ants`, with `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot`. The evidence helper reads `$LASTEXITCODE` immediately, records start/end/elapsed time and complete output, and returns the same code. Each ID below corresponds to `<ID>.json` and `<ID>.log` in the T01 evidence directory.

| Evidence ID | Actual Gradle command | Exit | Elapsed seconds | Result |
| --- | --- | --- | --- | --- |
| `01-wrapper-version` | `.\gradlew.bat --version` | 0 | 19.927 | Downloaded Gradle 9.7.1; Java 25.0.3+9-LTS |
| `02-genSources` | `.\gradlew.bat genSources --console=plain` | 0 | 94.903 | Common/client game sources generated and inspected before API-dependent code |
| `03-task-discovery` | `.\gradlew.bat tasks --all --console=plain` | 0 | 1.973 | Verified `runGameTest`, `runClientGameTest`, `test`, `build` |
| `04-build-baseline` | `.\gradlew.bat build --console=plain` | 1 | 34.237 | Server 1/1 passed; unit compilation failed: missing Jupiter parameters |
| `05-unit-recovery` | `.\gradlew.bat test --console=plain` | 0 | 21.918 | Targeted recovery: 15 unit cases passed |
| `06-build-unit-failure-probe` | `.\gradlew.bat build --console=plain` | 1 | 4.124 | Temporary expected 24,001 instead of 24,000: 15 cases, 1 failure; build stopped before server |
| `07-build-server-failure-probe` | `.\gradlew.bat build --console=plain` | 1 | 26.729 | Unit 15/15 passed; temporary server assertion failed at tick 13; game process exit 1; build exit 1 |
| `08-build-final` | `.\gradlew.bat build --console=plain --rerun-tasks` | 0 | 13.316 | Both assertions restored; unit 15/15 and server 1/1 executed and passed, none skipped |
| `09-client-capture-attempt-1` | `.\gradlew.bat runClientGameTest --console=plain` | 0 | 71.282 | One development client entrypoint executed, capture saved, automatic exit |

Helper invocation shape (separate invocation for each row):

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "& .\scripts\Invoke-GradleEvidence.ps1 -Name '08-build-final' -GradleArgs @('build','--console=plain','--rerun-tasks') -EvidenceDirectory 'C:\Users\user\Documents\turnloop\directions\prime-ants-slice1\turns\T01'"
```

Additional setup failures and recovery are preserved: initial direct helper invocation exit 1, 0.047 s (`00-script-policy-failure.json`); SHA-256 response `.Trim()` on bytes exit 1, 2.366 s (`checksum-decode-failure.json`). Process-scoped script execution and UTF-8 byte decoding respectively recovered these checks. The small Loader request returned HTTP 200; downloaded dependency metadata and wrapper/source artifacts have URLs, elapsed time, and hashes in download manifests. Web-tool rendering rejected XML MIME types; PowerShell fetched the same metadata successfully. This was not a dependency-resolution failure.

## Artifacts (paths)

Evidence base: `C:\Users\user\Documents\turnloop\directions\prime-ants-slice1\turns\T01`.

| Artifact | Path |
| --- | --- |
| Production mod | `build/libs/prime_ants-0.1.0.jar` (4,548 bytes) |
| Production sources | `build/libs/prime_ants-0.1.0-sources.jar` (3,073 bytes) |
| Final unit XML / HTML | `build/test-results/test/TEST-dev.primeants.time.SimulationTimeScaleTest.xml`; `build/reports/tests/test/index.html`; preserved as evidence `08-unit-results.xml`, `08-unit-html/` |
| Final server XML / log | `build/test-results/gametest/server.xml`; preserved `08-server-results.xml`, `08-server-latest.log` |
| Failure probe evidence | `06-deliberately-failing-unit.java`, `06-unit-failure-results.xml`, `07-deliberately-failing-server.java`, `07-server-failure-results.xml`, and command logs |
| Source provenance | `source-downloads.json`, `wrapper-downloads.json`, `wrapper-checksum-source.json`, `api-source-downloads.json`, `game-source-jars.json`, `inspected-sources/` |
| Archive inspection | `08-archive-inspection.json` (complete entry lists, hashes, no development entries) |
| Fresh screenshot | [docs/screenshots/t01-infrastructure.png](screenshots/t01-infrastructure.png); [caption/provenance](screenshots/README.md) |
| Capture evidence | `09-capture-preflight.json`, `capture-provenance.json`, `09-client-capture-attempt-1.{json,log}`, `09-client-latest.log` |
| Final Git/evidence inventory | `commit-evidence.json`, `evidence-manifest.json` |

Production jar SHA-256: `89905d0b743c653ffd6b3cfc39adcaa9d8401b906eaaea65c2ff810db2921bf7`.

Sources jar SHA-256: `96fa77c85efc88b273a3a3e238ce61668b0765fa6693a2a2c0e8fec00c59b5f2`.

Capture command start: `2026-10-04T14:55:02.0947520+05:00`; capture: `2026-10-04T09:56:11.695577400Z`; seed `2026100401`; render wait `42` ticks; PNG `1280×720`, `1,118,552` bytes. SHA-256: `0a8a35b2fece3dd9dcbf5142dba3c3ea4e037ff2cd75097ca35d3f49b9508609`.

## Claims and their evidence

- **Working 26.3 toolchain:** `01-wrapper-version.log`, `02-genSources.log`, final `08-build-final.json` exit 0, and production jars.
- **Non-vacuous unit discovery/execution:** final XML contains 15 `testcase` elements, zero failures/errors/skips. Names include `oneGameDayAtNormalSpeedTakes24000ElapsedTicks()`, `twoGameDaysAtTwentyTimesSpeedTake2400ElapsedTicks()`, and individually named invalid multiplier/duration cases.
- **Real server ticks:** final server XML names `prime_ants_test:bootstrap_game_test_food_item_lifecycle`; final log records `elapsedTestTicks=10, elapsedEntityAge=10` and absence at tick 13. One discovered/executed required test, zero failures or skips.
- **Failure propagation:** both deliberate probes returned build exit 1; the server JVM also exited 1. Final rerun executes restored assertions successfully. Unit tests run before the server for fast failure; no tests are relaxed.
- **Test isolation:** only the 8x8 vanilla empty GameTest fixture gets one supporting stone block and one apple. Client capture uses an independent normal survival world and makes no terrain/entity/pose changes.
- **Production exclusion:** both jar entry lists contain common/client code and resources only; no GameTest, capture, test mod, or JUnit code. `08-archive-inspection.json` records this inspection.
- **Real client capture:** fresh destination was absent before the attempt; log records seed, world creation and screenshot; image was opened and showed snowy spruce terrain, sky, survival HUD, and player hand. This is a content observation, with no visual acceptance verdict.
- **Specification preservation:** final Git evidence records clean initial/final worktree and no change to pre-existing tracked specification files.

## Deviations, relaxations, skipped steps

No Minecraft version change, cache wipe, widened timeout/memory limit, old architecture import, system installation/configuration change, push, or owner-world modification. No required step is skipped and no test assertion is relaxed. Configuration cache is disabled for the initial testing workflow; JVM memory remains the official template's 1 GiB setting. The optional license/publication boilerplate is omitted from the minimal template-derived project; no license choice is inferred for the owner.

The initial unit compilation failure was diagnosed from the resolved Loader JUnit POM. Only the missing matching parameter dependency was added; the smaller `test` task passed before any further full build. This infrastructure failure did not recur. The two deliberate failing assertions are separate validation probes, not recovery retries. The unit probe stopped before server execution by design. Final `--rerun-tasks` ensured that neither suite's green evidence was just an up-to-date task.

Unresolved non-fatal diagnostics: Windows OSHI/Perflib cannot read English performance counters and Minecraft cannot record several process metrics; an empty client resources directory warns as missing; Fabric client test defaults cause an anisotropic-filter option diagnostic; development-token Realms authorization diagnostics appear. All remain in logs. Minecraft continued, server tests passed, client rendered and captured, and both final commands exited 0. No workaround disables these checks or changes global configuration. Gradle also reports deprecations for a future Gradle 10; the pinned 9.7.1 build passes.

The screenshot retains Fabric's test rendering defaults (render distance 5, clouds off, no tutorial/fade, music muted) and the natural HUD/hand. It is not staged and is not colony evidence. Empty EN/RU files deliberately contain no invented gameplay strings.

Carry-forward counts: **toolchain turn 1 succeeded** after one recovered unit dependency failure; four `build` invocations (one initial compilation failure, two deliberate negative probes, one final green). **Client attempts: 1; failures: 0; successful captures: 1.** No unresolved blocking failure. Preserve the above non-fatal diagnostics if later failures share their signatures; diagnose a smallest failing check before repeating a workload.

Remaining colony outcomes: ant entities/anatomy/animation, queen, brood and care, population, physical food handling, foraging, digging/nest construction, colony state/persistence, and colony gameplay are unimplemented. The colony premise remains untested, and all four eventual colony views remain outstanding.

## Questions for the owner

None required for T01. The safely chosen interpretation is that empty EN/RU resources are sufficient while no player-facing gameplay text exists. The elapsed-time helper defines infrastructure timing only, not biology or brood behavior.

## Self-assessment against the definition of done

1. Minimal official-template-derived project, `prime_ants`, wrapper, Java 25, four source sets, production exclusion: evidenced.
2. Successful `genSources`, actual game/API inspection before API-dependent code, dependency/source/signature notes: evidenced.
3. Reusable elapsed-tick helper and executed non-vacuous unit tests, brood unimplemented: evidenced.
4. One discovered server test through 13 real ticks, integrated build gate, both negative failure probes restored, final green: evidenced.
5. Exactly one dedicated real-client attempt, fresh normal world, rendered chunks, automatic capture/exit, inspected gameplay, provenance/caption: evidenced.
6. Initial slice report and descriptive commit on `master`: evidenced by the committed report and external final Git record.

NEXT: continue - The verified foundation makes the next entity slice reviewable; the colony premise and all colony views remain untested.

## T01 retry verification — 2026-10-04

Independently checked clean `master` at `85262a20160715627fa296605d665f13ef375ebc`, subject “Establish verified Minecraft 26.3 Fabric foundation and T01 capture”, before repository changes. The foundation, source sets, assertions, wrapper/checksum, pinned dependencies, EN/RU resources, and original attempt-1 capture are retained. This retry changes only this verification record. [Baseline and original manifest checks](../../turnloop/directions/prime-ants-slice1/turns/T01/retry-01-baseline-verification.json), [source provenance](../../turnloop/directions/prime-ants-slice1/turns/T01/retry-01-source-provenance.json), and [restored negative-probe assertions](../../turnloop/directions/prime-ants-slice1/turns/T01/retry-01-restored-probe-diffs.patch) provide the inspection record. All five source jars match their original hashes; all 28 preserved Java extracts match source-jar contents after normalizing BOM/line endings and terminal newlines. Source generation and negative probes were not repeated.

Exactly one fresh `.\gradlew.bat build --console=plain --rerun-tasks` ran through the existing evidence helper: start `2026-10-04T15:17:59.9840245+05:00`, end `2026-10-04T15:18:13.4944384+05:00`, elapsed `13.4752738` seconds, exit `0`. [Command/timing](../../turnloop/directions/prime-ants-slice1/turns/T01/retry-01-build-verification.json) and [complete log](../../turnloop/directions/prime-ants-slice1/turns/T01/retry-01-build-verification.log) show 15 actionable tasks executed, including both suites. [Unit XML](../../turnloop/directions/prime-ants-slice1/turns/T01/retry-01-TEST-dev.primeants.time.SimulationTimeScaleTest.xml) contains 15 cases; [server XML](../../turnloop/directions/prime-ants-slice1/turns/T01/retry-01-server-results.xml) contains `prime_ants_test:bootstrap_game_test_food_item_lifecycle`. Both have zero failures/errors/skips. The log records apple age advancing ten real ticks and absence at elapsed tick 13; the restored source asserts removal from the UUID entity lookup. [Archive inspection](../../turnloop/directions/prime-ants-slice1/turns/T01/retry-01-archive-inspection.json) preserves hashes and full entry lists: both fresh jars match the original hashes and exclude development tests/capture code.

Opened the actual [original PNG](screenshots/t01-infrastructure.png): snowy spruce terrain, sky, survival HUD, and player hand. Independently checked `1280×720`, SHA-256 `0a8a35b2fece3dd9dcbf5142dba3c3ea4e037ff2cd75097ca35d3f49b9508609`, original command start `2026-10-04T14:55:02.0947520+05:00`, capture `2026-10-04T09:56:11.695577400Z`, seed `2026100401`, and caption “T01 infrastructure capture; colony not implemented” against capture code and both original logs. [Capture verification](../../turnloop/directions/prime-ants-slice1/turns/T01/retry-01-capture-verification.json) retains attempt-1 classification and timestamps. No new client launch: lifetime counts remain one attempt, one success, zero failures; this image satisfies none of the four colony views.

Remaining evidence limit: `raw/exec.out` was absent at inspection, so the historical source-inspection-before-coding sequence cannot be independently reconstructed from that transcript. Original `genSources` success, preserved source contents, and current API usage are independently corroborated. The original evidence manifest also differs for seven supervisor-owned brief/prompt/accounting files already replaced before this verification; its foundation evidence matches. Neither missing transcript nor supervisor records were reconstructed or changed. A retry audit-reader UTF-16 decoding failure is preserved in `retry-01-audit-pre.{json,log}`; BOM detection recovered only the pending checks in `retry-01-audit-pre-recovery.{json,log}`. It was not a build/client failure. The established `junit-jupiter-params:5.10.0` fix and targeted `test` recovery remain verified. Non-fatal OSHI/Perflib, empty-resource, anisotropic-filter, development-token Realms, and Gradle deprecation diagnostics remain preserved. Living colony gameplay and all four colony views are unimplemented; performance with ants is unmeasured. Acceptance remains with the reviewer. The [full retry handoff](../../turnloop/directions/prime-ants-slice1/turns/T01/report.md) and `retry-01-git-commit.json` record the resulting commit and carry-forward criteria.


## T03 gait and presentation correction — incomplete

The mirrored-yaw defect was reproduced against the original factory before correction. The current production model uses connected hip/femur/tibia/tarsus inverse kinematics: each stance sweeps monotonically rearward, the complementary tripod returns forward with clearance, and roles exchange every half-cycle. Constant distance-based stride follows the adult vanilla smoothed walk-distance input; movement speed changes cadence and lift rather than scaling longitudinal stride twice. Idle resets feet while antennae continue moving.

Head, mesosoma and gaster use tapered stepped contours. One petiole node and a slender continuous stalk connect the waist; neck, hips, antenna bases and queen scars attach to baked body geometry. Body lengths remain 1.025142 worker / 2.271376 queen blocks, with 32 texels per world block. Body heights excluding appendages are 0.2109375 / 0.40625. Maximum node widths changed from 0.0625 / 0.125 to 0.05625 / 0.109375; stalk widths are 0.025 / 0.046875. Head/mesosoma/gaster anterior widths are 48% of maximum instead of 100% in T02. These measurements are geometric evidence, not visual approval.

Current model discovery executes 24 named cases (12 per form). Tests use ModelPart.visit and actual baked distal tarsus geometry, 64 intervals/cycle at speed inputs 0.03, 0.28, 0.35 and 1.0. A separate offline current-factory probe passed all 70 render inputs preserved from T03 a1/a2. The full CSV traces, before/after failures, profile slices, XML, logs and archive inspections are in the external T03 evidence directory.

The client guard now generates a fresh UUID before launch, passes it to both entrypoints, and uses unique attempt prefixes. It checks named model discovery, matching IDs, capture completion, both forms and side/oblique sequences, support alternation, cycle coverage, and PNG path/time/dimensions/SHA-256. Ten negative fixtures are rejected without Minecraft. Offline validation also rejects the actual failed a3 manifest (18-current-failure-rejected, exit 1) despite old successful provenance remaining present. Initial discovery accidentally included the offline profilesOnly helper; the failure and offline recovery are preserved. Fixtures now use an actually executed headless model XML instead of synthesizing names from the same discovery regex.

No final completion is claimed. Last successful full build: t03-build-final-distance-gait, exit 0, 16.486 s (15 unit, 16 server, 24 model cases). Later builds failed the unchanged queen_wanders_after_long_idle case twice at tick 242; the isolated unchanged case passed at age 136 between them. RandomStrollGoal gates both opportunity and destination; stochastic failure is a supported hypothesis, not an established root-cause fix. Assertions, 240-tick limit, entity mechanics, 0.15 waypoint tolerance, 600-tick queen navigation allowance and production idle-goal correction remain unchanged.

Client a1 exited 1 at the Gradle discovery guard after producing 48 provisional frames; its current-run files passed offline validation after the regex fix. Client a2 exited 1 after 22 current-model queen frames (12 side, 10 oblique); saved terrain confirms its later waiting positions lacked the required 3x3 observation clearance despite active walking. Client a3 exited 1 before specimens/PNGs: the natural plateau found in an offline region scan did not pass live ground selection. Screenshot work is stopped under the repeated-attempt criterion. The executor also launched a3 before inspecting the preceding failed build result, violating the requested success-before-client sequence.

T03 added 3 client launches: Minecraft processes 1 success / 2 failures; all 3 overall Gradle client tasks failed. Lifetime: 7 launches, Minecraft processes 4 successes / 3 failures; overall dedicated client tasks 3 successes / 4 failures. No PNG or JSON from T01/T02 changed (19 original files hash-verified). There are 70 T03 PNGs with individual captions; final worker distance-based gait has no gameplay screenshots. Anatomy/readability acceptance stays with the independent reviewer.

Known OSHI/Perflib, empty client resources, anisotropic-filter default, development-token Realms, passenger-update and Gradle/API deprecation diagnostics remain preserved. Read logs by BOM and JSON as UTF-8 (UTF-8 BOM accepted). No toolchain, global setting, authentication, terrain, AI, physics, permission, or owner-world workaround was made. Full world restart, offline catch-up, lifespan, brood replacement, colony behavior and 20+ ant performance remain unresolved.

Owner decision needed before another autonomous verification turn: may the stochastic server idle fixture be stabilized (for example a recorded fixed random seed) while keeping all assertions, its 240-tick bound and production mechanics? Ground-probe synchronization/live terrain validation also needs diagnosis within capture scope; another unchanged client run is not recovery.


The focused server diagnostic filter initially also allowed a full build to use the single-case discovery branch. This temporary build-guard widening was closed after audit: taskGraph rejects the diagnostic property whenever `:check` is present, before any workload or Minecraft launch. `19-diagnostic-build-bypass-rejected` exits 1 with that guard message; `20-default-graph-config-check` exits 0. Normal full-suite discovery and all test assertions remain intact. No complete build/client rerun followed the stop condition.


## T04 causal recovery and interim adult specimens - recovered

The owner authorized fixture repair and capture resumption for this recovery turn. T03's unresolved idle failure and seven prior client launches were carried forward. This section supersedes the earlier pending request for authorization; historical failures remain preserved.

Instrumentation lives entirely in the development source set. It redirects the original RNG and path calls exactly once, then returns their original results; additional observations only read state. Resolved 26.3 goal/navigation/chunk sources and matching Fabric structure-loader sources were read first (T04/01-source-reading.json, inspected-sources and 01-source-notes.md).

Two mechanisms were demonstrated. On the original 6x6 floor, seed 5 in the odd scheduler phase failed at tick 242: 121 opportunity checks, six accepted opportunities, 59 of 60 unstable land candidates, five null destination results. The sole stable neighbor at age 115 was accepted with vanilla reach range 1 as a length-1 path containing the current node, then completed without movement. Production strolling now passes its own selected coordinates to navigation with reach range 0. The same sequence then yields a length-2 path and movement at age 122 (07-idle-seed5.xml/log versus 09-idle-seed5-recovered.xml/log). Speed, interval 40, random chance, destination range 6/2, waypoint tolerance and deadlines are unchanged.

That correction alone was insufficient. After two intermediate 19-case server passes, t04-build-final exited 1: the same seed in the other vanilla phase produced eight opportunities, 80 unstable candidates and no path attempt inside the bound. Mob.serverAiStep schedules by tickCount + entityId, affecting the shared look/stroll random stream. A single discarded fixture item reproduced that phase in the isolated test (18-idle-even-phase-reproducer, exit 1). No unchanged full retry or client launch followed that red.

The authorized terrain fixture repair uses a 14x14 floor in a 16x8x16 empty test enclosure, centered at (7.5,2,7.5), supporting the complete production +/-6 horizontal choice envelope. Formerly failing seed 5 is retained rather than replaced with a passing seed; seeds 2/0 retain observed sparse/null streams. Normal ID allocations from discarded fixture items explicitly cover both vanilla phases without changing an ant's ID, tick schedule, AI, destination, goal start, idle counter after the initial 140, or pose. All original assertions and maxTicks=240 remain. An additional 6x6 odd-phase seed-5 case retains the neighboring-destination defect as a production-path regression. Seven predeclared targeted cases passed (20-26), at ages 107,14,82,67,20,119,122.

Ground selection removes the unverified (-168,103,34) plateau. CaptureGround requests minecraft:full for every chunk in the search region plus candidate footprint, then queries live solid support and two actual air layers. The short capture uses a 5x5 creation footprint and the existing 3x3 observation clearance; it rechecks immediately before egg/summon. A headless server case rejects vegetation with properties, incomplete floors and unloaded footprints (13, exit 0). Archive audit rejects a2 chunk (-11,2) at minecraft:terrain and decodes property-bearing leaf-litter entries by id at actual block positions. Raw a3 region data was not present in the supplied T03 directory or current client run directory; its failed manifest was read, and neither saved coordinate nor claimed plateau is used as evidence for selection.

Final complete server checks 27/28 each passed 23/23 cases. t04-build-final-recovered ran unfiltered build --console=plain --rerun-tasks, exit 0, 15.846 s: 15 retained unit, all 16 retained server plus seven justified additions (23 total), and 24 retained model cases. Executed XML and production jars are preserved under T04/final-test-results and final-production. Development harness/mixins/fixtures remain excluded from both production archives. The diagnostic filter is still forbidden whenever check/build is present (16 expected exit 1 before workload); seed overrides are diagnostic-only.

Only after that completed build and XML/exit inspection, dedicated client t04-a1 ran (31, exit 0, 32.439 s). Both adults came from the same source/build hash state, independently verified unchanged before/after client. Queen creation used the real egg packet/item interaction; worker used an ordinary operator summon. FULL live ground was (-10,72,-13) and (-15,68,6). Terrain remained untouched; ants were never teleported/posed and production AI/physics remained active. The owner-authorized contract is now exactly four PNGs, one side and one oblique walking frame per form. Previous 48-frame sequence/support-alternation/full-cycle presentation requirements are removed; 24 model cases, movement checks and current-run ID/path/time/dimension/hash checks remain. All ten negative validator fixtures still reject.

Fresh images: [queen side](screenshots/t04-a1-queen-side-1.png); [queen oblique](screenshots/t04-a1-queen-oblique-1.png); [worker side](screenshots/t04-a1-worker-side-1.png); [worker oblique](screenshots/t04-a1-worker-oblique-1.png). [Run provenance](screenshots/t04-a1-provenance.json), per-image captions and T04/32-final-capture-and-regression-verification.json preserve exact paths, 1600x1000 dimensions, timestamps and SHA-256. Run 32eea718-db4c-4a80-9887-4df7a0ffff93. Queen frames: ticks/ages 76/26 and 78/28, displaced 0.1223 and 0.1844 blocks from creation; worker: 230/150 and 232/152, displaced 0.1422 and 0.2752. All four have active navigation, nonzero horizontal velocity, grounded collision, normal physics and measured moving render extraction. All four actual PNGs were opened; both silhouettes are visible in the requested views. This is the accepted interim baseline, not additional visual refinement or colony evidence.

T04 added one successful native/client task launch. Lifetime: eight launches; native processes five successes/three failures; dedicated tasks four successes/four failures. All 166 earlier screenshot-directory files were hash-verified unchanged before documentation append; earlier PNGs, JSON, CSV and individual captions remain unchanged. Known OSHI/Perflib, empty-client-resource, anisotropic-filter default, development-token Realms, passenger-update and Gradle/API deprecation diagnostics remain documented, without OS/auth/settings workarounds. T04 also retains its initial outer-shell exit-check quoting error (02 helper itself completed exit 0), local archive-reader KeyError/id and wrong grass-column assertion; those were repaired without a workload restart.

Full world restart, offline catch-up, lifespan, brood replacement, colony behavior and 20+ ant performance remain unresolved. The next turn must advance to queen founding and the colony core loop. Complete T04 handoff and commit identity are in the external T04/report.md and commit-evidence.json.


## T05 slice result ? first physical colony action

An authorized queen egg still creates exactly one adult through vanilla SpawnEggItem/EntityType. `Mob.finalizeSpawn(SPAWN_ITEM_USE)` requests production founding; item use creates no terrain, workers, completed nest, income or population counter. Other debug adults retain T04 wandering. Founding owns movement while seeking, excavating, carrying, entering, sealing and settling; unsupported/unknown sites fail with a logged reason.

`NaturalSoil` holds positive observations from genuine new TERRAIN generation only. A successful `ChunkStatusTasks.buildTerrain` future witnesses its ProtoChunk; main-thread FULL conversion records stable dirt/grass-family material in the top eight layers. Loading, starting a colony, historical chunks, retrogen and untrusted interrupted proto generation do not confer permission. `LevelChunk.setBlockState` revokes records on every later write, including same-state replacement, normal player placement, breaking and piston writes. Ant spoil/plugs remain revoked. Records persist in dimension SavedData; no world-generation content is added. Unknown-origin historical worlds are unsupported and protected.

`NestPlan` describes 24 removal targets (hard cap 24, below 48): three descending entrance cells and a 3x3 chamber floor with two-block headroom. Tasks reach forward 5 blocks/side 1; outside deposits reach backward 3. Declared horizontal Chebyshev radius is 5 from the entrance, depth bound 3 including support (removed cells reach depth 2). Search radius is 3 around the egg queen. Roof, floor and chamber shell must be verified native stable soil; fluids, falling soil, stone, block entities and unsupported material are excluded. One living nearby queen removes at most one exposed block per work action, with exact state/origin/material/reach checked immediately before mutation. A replaced planned block aborts safely. A blocked route aborts after 600 non-arrival ticks; no alternate geometry placement occurs.

Work cadence is 200 loaded server ticks per removal/deposit/plug at multiplier 1. `prime_ants.foundingWorkMultiplier` is a production JVM property passed through SimulationTimeScale; server/capture tests use 20 (10 ticks per action). Movement speed, navigation, gravity and collision are unchanged. Carry capacity is four real dirt units in vanilla Mob mainhand equipment, synchronized and persisted by Minecraft. `AntSoilLayer` submits a vanilla dirt ItemStackRenderState at existing ant/head mandible transforms; adult anatomy/gait/textures are unchanged. Three soil blocks form the exterior deposit; remaining surplus is released as physical dirt items with unlimited item lifetime. The last two recovered units seal the two-high throat from inside. The queen then navigates to the chamber center and settles. Current sealing also checks actual plug and chamber-shell blocks.

Final soil balance: **24 removed = 0 carried + 3 external blocks + 19 released item units + 2 plug blocks**. Vanilla grass spread later changed the three exterior dirt blocks to grass, retaining the same physical units and revoked excavation permission. Founding phase, entrance/direction, expected block states, tasks, progress, cooldown, loaded work ticks, counters and equipment survive entity serialization. A mid-excavation entity restore plus real SavedData disk read finished without duplicate work/queen. FULL chunk serialization cannot mint origin. These are running-process persistence paths, not a full process restart. Death stops work and releases/clears carried soil once; unfinished terrain remains.

Final `t05-build-final`: unfiltered `build --console=plain --rerun-tasks`, exit 0, **26.665 s**, 15 retained unit cases, 23 retained server cases plus 8 founding cases (31), and 24 retained model cases. Discovery/failure guards remain; 240-tick T04 idle bounds and both scheduler phases remain. All ten prior malformed client-evidence fixtures plus four founding checks reject (14). Executed XML, production jars/hashes and exact command metadata live under external T05.

Measured normal founding: **10,189 loaded ticks** at multiplier 1 / cadence 200 (509.45 s, 8 min 29.45 s at 20 TPS), verified with the final enclosure policy. Final generated-world founding: **3,164 loaded ticks** at multiplier 20 / cadence 10 (158.2 s at 20 TPS). Travel remains ordinary navigation; timing is not simply divided by 20. No manual ticks, ant teleportation or instant queue completion.

After the completed green build, `20-client-capture-a1` ran exit 0 in **181.722 s**. Fresh natural world seed 2026100402; selected entrance (59,79,-41), north; run **67b17cc3-70be-4181-9ba3-bddb6a15f370**, queen **912fbfd6-45d3-4257-97bf-a47427dc4472**. Real client egg interaction, no workers, terrain shortcuts, forced poses or physics changes. Spectator observer position/HUD/FOV and disclosed vanilla night vision only. Three 1600x1000 PNGs: [transport](screenshots/t05-a1-transport.png) at founding tick 201, [entrance/spoil](screenshots/t05-a1-entrance-spoil.png) at 275, [inside sealed chamber](screenshots/t05-a1-chamber.png) at 3164. Captions and [provenance](screenshots/t05-a1-provenance.json) preserve phase, UUID, multiplier and hashes. All three were opened. Current-run validator passed 24 named model cases and three fresh images. The interior frame preserves natural last-step interpolation; no stationary pose was forced.

T05 adds one successful native/client task launch. Lifetime: **nine launches**, native **six successes/three failures**, dedicated tasks **five successes/four failures**. 176 earlier screenshot-directory files and all 53 implementation/build input files were hash-verified unchanged across capture. Earlier images are preserved. Capture raw region/entity/SavedData snapshot and offline audit independently confirm 22 air cells, 2 plugs, 3 external soil blocks, 19 item units, one queen/no workers, two-block headroom and nine untouched positive native roof observations.

Failures are preserved in T05/report.md and the command inventory: test compile's unnecessary IOException catch; missing downward exposed-face case; premature assertion on unexcavated future plugs; sandstone-as-grass fixture assumption; wrong 26.3 dimension SavedData path. Focused recovery preceded the final full build. A wrong ChunkPyramids archive name interrupted the intended diagnostic setup before small attempt 10, which consequently repeated the failing native-surface check unchanged; this execution mistake is disclosed. Instrumented attempt 11 showed a correct generation witness and native sandstone, not an eligibility defect. The added genuine-generation server case now verifies unsupported native sandstone and later unknown placed dirt stay protected through FULL serialization; positive native soil founding is proved in the fresh real-generation client. Offline region-reader string/unnamed-palette and diagnostic path errors were repaired without another game launch.

Limitations: no brood, full process restart, offline catch-up, lifespan, foraging, worker excavation, expansion or multi-ant performance claim. Origin storage scales with witnessed soil; capture saved 228,502 positive records (compressed ~1.18 MB), with no large-world performance benchmark. Native block/property changes revoke origin conservatively. Interrupted proto generation loses its transient witness; no retrospective marking. Released item pickup/other later player modifications are not a colony economy. Accepted adult silhouettes remain the interim baseline.

## T06 trustworthy completion and current readiness - 2026-10-04

The final transition now validates the live enclosure before announcing `SETTLED`. Two production-path regressions first reproduced false settlement: a chamber side wall was opened after the first plug, and a placed lower plug was removed after both plug actions but before settlement. In each pre-fix run production logged `Founding settled` / `settled_throat_sealed` with an invalid enclosure (T06/03-server.xml and 04-server.xml, exit 1). No phase assignment, manual ticks, instant task completion or queen relocation helped the work finish.

`QueenFounding.enclosureProblem` is the shared, phase-independent physical predicate. It checks available FULL chunks, both actual dirt plugs, the closed chamber shell, air and stable support across all nine chamber cells, and a living grounded collision-free queen whose entire body is inside. `sealed()` additionally qualifies historical phase `SETTLED`; that qualified query is never used unchanged to authorize the transition. Physical placement counters are history, not evidence that blocks still exist.

Pre-completion invalidity enters terminal `FAILED`, logs an explicit `enclosure_plug_missing`, `enclosure_shell_open`, `enclosure_chamber_obstructed` or `enclosure_queen_not_inside` diagnostic, and does not emit the successful-settlement result. A completed/restored `SETTLED` phase remains a historical fact. Readiness immediately rechecks live geometry and the queen, and `reason()` reports `settled_not_ready_<physical diagnostic>` even before the next tick; subsequent settled ticks log reason changes. Serialization saves the current reason. No automatic repair, soil refund, replacement material, new excavation or re-entry is performed. A later intentional opening needs a future lifecycle phase; it currently revokes founding readiness. A freshly restored queen must also regain valid physical grounding before readiness can be true.

Passing breach/obstruction regressions freeze terrain and work/soil counters for 80 further real ticks. Intact founding retains **24 = 3 deposited + 19 released + 2 live plugs**, zero carried. The plug-removal intervention retains two historical placements but only one live plug: **24 = 3 deposited + 19 released + 1 live plug + 1 externally removed/destroyed unit**. The side-wall removal is an external native-shell edit, outside the queen's 24 recovered units. Invalid settled obstruction survives normal entity serialization/restore, FULL chunk serialization/read, and a fresh SavedDataStorage disk read in the same process. A separate negative case externally teleports the queen only after real-tick completion and confirms loss of interior readiness; this never shortcuts founding work.

The positive native-origin fixture is entirely in `src/gametest/resources/data/minecraft/worldgen/world_preset/flat_all_dimensions.json`. Matching 26.3 sources show GameTestServer selects that preset rather than standalone datapack dimensions. The three vanilla declarations stay unchanged, including the desert/sandstone overworld negative case. An added `prime_ants_test:native_soil` dimension uses the vanilla flat generator, overworld type/plains biome, bedrock 1 + dirt 6 + grass 1, features/lakes false and no structures. The test asserts the resolved layer settings and absent region/FULL chunk, then requests new chunk (256,256) via `getChunk(FULL,true)`. Unmodified production hooks record **1792** native dirt/grass cells. Editing grass revokes its eligibility; ordinary FULL serialization/loading does not grant it again while untouched dirt remains eligible. No NaturalSoil codec seeding, manual observer invocation, witness assignment or capture-terrain reuse occurs in this case.

Repeatability initially failed at the freshness guard (08/09): Loom's misleadingly named `deleteGameTestRunDir` only clears the client directory, leaving the previous server dimension region. Resolved Loom APIs confirmed the cause. Each server invocation now uses a fresh UUID directory under `build/run/serverGameTest`; prior worlds remain preserved. Targeted 10 recovered. A temporary witness knockout in fresh-world 11 then failed specifically at the positive grass AND dirt assertion; the exact original hook was restored and 12 passed. Production generation hooks are unchanged. Reading/source hashes and this recovery are in T06/01-source-reading.json and 01-source-api-notes.md.

Final **t06-build-final** ran fresh unfiltered `build --console=plain --rerun-tasks`, exit **0**, **25.316616 s**: all **15 unit**, **31 retained + 6 new server = 37**, and **24 model** cases; zero failures/skips. All 14 malformed client-evidence fixtures still reject. Diagnostic filtered build is rejected before workload (17, expected exit 1). Normal multiplier-1 founding passes again at cadence **200**, **10,189 loaded ticks** (18); server acceptance still uses multiplier 20/cadence 10, production defaults to 1. Executed XML, commands/exits/timings, archive hashes and name-retention checks are under T06/final-test-results, final-production, 20-final-verification.json and 21-command-inventory.json. Both production jars exclude development classes, mixins and the preset resource.

No new gameplay/client capture ran. All 183 current screenshot-directory files hash-match the baseline; T05 images retain their original T05 provenance and are not T06 evidence. Pinned Minecraft 26.3, Java 25, Gradle 9.7.1, Loader 0.19.5, API 0.161.0+26.3 and Loom 1.18.2 remain unchanged.

Persistence remains running-process entity/chunk serialization plus SavedData disk reload. No full restart persistence, offline catch-up, lifespan, brood replacement, worker excavation, foraging, expansion or performance claim is made. Historical/unknown-origin terrain remains protected; the observation depth and conservative revocation policy are unchanged. There is no breach repair or later nest-opening lifecycle yet. NEXT: accurate founding readiness and repeatable positive-origin coverage now permit the bounded brood slice.


## T07 first real workers - 2026-10-04/05

One genuine authorized egg queen (`b2e1460f-c367-47e8-9b0f-8125e4df8ffb`, seed 2026100402) physically founded, laid three distinct eggs, nourished all larvae from 39,000 finite body units and produced three pale production workers. Adults and brood are canonical physical state, with one-for-one emergence, persistent lineage, server-owned darkening, normal damage and no automatic replacement.

Final fresh unfiltered `t07-build-final-complete`: exit 0, 52.154 seconds; 15 unit, 49 server (37 retained + 12 new), 24 model, no failure/skip, 19 malformed-evidence rejections. Production default timing is 12,000/12,000/12,000 plus 4,800 loaded ticks. A real 1x diagnostic emerged at 36,000 and spent the same full budget. `t07-client-a4` followed completed green acceptance: exit 0, 337.236 seconds (A3 was 337.912 seconds), brood 100x/founding 20x, five genuine frames with IDs/live counts in [provenance](screenshots/t07-a4-provenance.json).

[Eggs](screenshots/t07-a4-eggs.png), [larvae](screenshots/t07-a4-larvae.png), [cocoons](screenshots/t07-a4-cocoons.png), [queen and pale workers](screenshots/t07-a4-callows.png), [mound](screenshots/t07-a4-mound.png). Nursery surfaces are soil, zero underground grass, with 29 native surfaces prepared. Material balance is 24 = 22 exterior blocks + two plugs, no normal item release. A declared 40-cell area supplies 22 supported choices without overwriting plants/player supports; blocked deposition retains soil. Surface conversion preserves volume and stops grass regrowth.

Only the exact designated owned pile is admitted to the former air cell; live shell/plug/queen/obstruction checks remain required. First-clutch adults stay in their nursery; diagnostic summons stay mature. Restoration checks cover valid resumed larval care, refused insertion, saved cocoon/live adult overlap, reserve/lineage/callow state and worker death. They are running-process entity/block-entity/FULL-chunk checks plus retained origin SavedData disk reads, not a full restart.

Failure/recovery evidence is in `C:\Users\user\Documents\turnloop\directions\prime-ants-slice1\turns\T07/report.md`: placement clearance/path-center diagnosis, controlled grass fixture decay, A1 fixed-mound plant blockage, A2 genuine lifecycle rejected for regrown grass and foliage view, native dirt preparation and retained breach expectations, then A3/A4 success. T07 native 3 success/1 failure and tasks 2 success/2 failures bring history to thirteen launches: native 9/4, tasks 7/6. All older artifacts remain; A2 captions explicitly mark rejection. No machine-launch failure or settings change occurred.

Repeated clutches, full restart/offline catch-up, lifespan, external feeding, worker nursing/foraging/excavation/expansion and 20+ ant performance remain absent. Adult geometry is unchanged. Commit: see the external T07 report and master history. NEXT: verified founding can raise the first real workers through finite care.

Final review added an explicit zero-reserve larval stall even for below-1x zero-cost rounding ticks. The real-server 0.5x diagnostic passes with 180 unchanged ticks; the final build and A4 capture include this guard. A3 remains a successful prior revision. Final persistence/material audit: T07/32-final-saved-world-audit.json.


## T08 physical opening and dropped-item foraging - 2026-10-05

One actual egg-founded colony now connects its nest to food. After claustral rearing of the original three workers, a mature brood-derived worker opens the colony's two proven plugs from inside, transports their existing units onto the mound, crosses the entrance, picks up player-dropped apple and raw chicken units within physical reach, returns and stores both inside the nest. Final A4 has one queen and three workers, **24 mound units + zero plug/held/dropped soil**, **zero world/held food + apple 1 and chicken 1 in the cache**, queen reserve 0. The six-unit cache stores six canonical one-unit ItemStacks, including components; its blockstate projects actual slots. This is dropped-item foraging, with no consumption, nursing, reserve refill or second clutch.

`WorkerTasks` is the sole movement/action owner for brood workers. Callows stay sheltered through their persisted 4,800-tick darkening duration (48 at the disclosed 100x capture setting); other workers keep nursery behavior. The queen reserves one existing mature living worker, preferring a free side row so the worker does not shove her through the throat. Bounded trips search loaded terrain within 10 blocks horizontally / 3 vertically, search for 240 ticks, and bound travel phases at 1,200 ticks. Full/blocked storage retains mainhand cargo, logs diagnosis and retries at 40-tick intervals. Opening stalls retain their phase rather than skipping to foraging. Death releases canonical equipment once, clears the worker task and claim, and recruits another living entity; founding release runs only for queens. Diagnostic adults retain wandering, colour, age and death behavior. Adult anatomy is unchanged; the shared held-item layer has a worker jaw attachment.

`ColonyPlugs` SavedData records only successful actual queen placements, then actual authorized worker removals. LevelChunk.setBlockState HEAD revokes both placement and opening records on every subsequent write, including same-state dirt or air writes. Positions, matching dirt, counters and old founding history cannot grant permission; missing historical records fail closed. The exception is confined to those two proven plugs and never excavates arbitrary mound/prepared/player/unknown blocks. The nest lifecycle is explicitly CLAUSTRAL -> OPENING -> OPEN, separate from historical SETTLED. Strict `sealed()` still requires live dirt plugs. Operational readiness requires recorded openings, intact corridor/chamber shell and floors, supported headroom, exact owned pile/cache, available chunks and the queen inside. Premature removals, unrelated wall/corridor breaches, floor removal, obstructions and same-air replacement invalidate readiness.

Transfers recheck living actor, home/claim, loaded source/destination, stack, range and collision ray immediately before mutation. Sequential server source rereads and split(1)/setItem prevent competing workers duplicating the same actual stack. Cargo uses canonical vanilla equipment; cache uses ItemStack.CODEC. Entity/block-entity/FULL-chunk serialization and fresh SavedDataStorage disk reads test restored phase, cargo/components, identity, cache, opening progress and ownership without duplicate soil/food/workers/actions. These are running-process paths. **Full process restart and T07's unloaded-worker reconciliation gap remain open**; unavailable homes/workers do not cause remote completion or replacement spawning.

Final fresh unfiltered `t08-build-final-visuals`: `build --console=plain --rerun-tasks`, exit 0, **89.046 seconds**, **15 unit / 61 server / 24 model**, no failures/skips. All 49 accepted server cases remain; additions are 11 worker cases and the complete-column founding regression. Named discovery/failure guards and the filtered-build rejection remain. All 19 retained malformed client fixtures and 12 new foraging fixtures reject; cache resource verification checks 18 slot projections, three block-atlas aliases and vanilla 16x16 sprites. Production jars exclude development test classes/mixins/preset resources. Pinned Minecraft 26.3, Java 25, Gradle 9.7.1, Loader 0.19.5, API 0.161.0+26.3 and Loom 1.18.2 remain.

Exact pinned game and matching Fabric source inspection preceded API edits (T08/01-source-reading.json; later drop/spawn/framework/atlas reads are individually preserved). 26.3 block models reject item-atlas sprites: cache models now use SingleFile aliases into the block atlas, preserving the original vanilla 16x16 food textures. The corrected capture camera reads actual body orientation and selects a clear elevated oblique ray; it never turns/poses/teleports an ant.

Final generated A4 world, seed 2026100402, run **8b9a932f-764c-4afb-8a8d-08a518d05dc7**, queen **1bc3fb48-27da-4e49-9dd1-0c53e3f75e44**, entrance (59,79,-41), north. Food was supplied as one creative apple and one raw chicken, each via the ordinary client DROP_ITEM packet; source ItemEntity UUIDs are in provenance. Observer only: spectator camera / FOV / hidden HUD and disclosed vanilla night vision. Brood acceleration is 100x, founding 20x; production defaults remain 1. Normal founding after the column fix measured **10,188 loaded ticks / cadence 200** (509.4 seconds at 20 TPS); A4 founding took **5914 loaded ticks**. Biological 12,000/12,000/12,000 + 4,800 defaults remain unchanged.

Fresh images: [entrance traffic](screenshots/t08-a4-traffic.png), [sugar in mandibles](screenshots/t08-a4-sugar.png), [protein in mandibles](screenshots/t08-a4-protein.png), [stored food with the colony](screenshots/t08-a4-cache.png). A4 client exit 0, **379.053 seconds**, 24 current-run model cases and four fresh 1600x1000 images; all four were opened. [Provenance](screenshots/t08-a4-provenance.json) and T08/46-a4-saved-world-audit.json verify physical world objects, source identity, opening protection, hashes and unchanged 106 build inputs. All 217 pre-T08 screenshot-directory files were verified unchanged before the required README append; the 216 image/data/individual-caption files remain byte-for-byte unchanged. The images evidence entrance traffic/carrying/cache and the colony actually present; they do not depict earlier brood stages or replace every original adult close-up requirement.

Failures are preserved, including the initial unused diagnostic property (03 ran the full 50-case server suite), a global registry-reset test fixture (05; changed to revoke only its colony), central nursery routing / boxed-in worker selection (09/10; side row and side-worker assignment), Groovy ternary parsing (11/12), a missing final protein-cache validator check (13), a lambda-local naming compile error (17), and a post-success GameTest callback (first final build / 23). Exact GameTest sources proved `succeed()` discards entities while other same-tick callbacks can still run; a terminal flag is set only after the final real seven-unit death balance, preserving all physical assertions. The mistaken source-scope hypothesis was reverted. The second parse command was queued before inspecting the first rejection; both stopped before any Minecraft workload. The route-floor omission was reproduced by 27 and recovered by 28 without weakening readiness.

Client A1 failed after native pending dirt regrew grass and correctly lost origin permission (77.085 s); A2 failed site selection before any egg (21.548 s). A1's smallest regression showed a transport departure halfway through a vertical column; the queen now reserves capacity to complete that column before hauling, retaining the same four-unit maximum, 24 targets and every write-revocation check. A2's saved fresh world proved the site lay 68 blocks from randomized observer spawn but 59 from declared world spawn; the unchanged radius-60 selector now anchors to declared spawn. A3 completed the physical path and automated checks (378.170 s) but was visually rejected for occluded protein/edge-on sugar and a missing cache model. Its images/provenance remain provisional and unchanged. Atlas inspection, asset checks and saved-world camera probes preceded green acceptance and A4. No unchanged full restart, timeout/RAM increase, paused AI, edited capture terrain or ownership bypass was used.

T08 adds **native 2 successes / 2 failures; tasks 2 successes / 2 failures**, including automated A3 success despite visual rejection. Lifetime now **17 launches: native 11/6, tasks 9/8**. Both failed clients launched normally; these were gameplay/harness failures, not machine-launch failures. Known OSHI/Perflib, empty client-resource, anisotropic-setting, development Realms token and Gradle deprecation diagnostics remain in logs. Read logs by BOM and JSON as UTF-8. The requested T07 timing correction is explicit: **A4 337.236 seconds; A3 337.912 seconds**, with original evidence preserved.

Remaining gaps: full restart, unloaded-worker reconciliation, offline catch-up, external feeding/nursing, continued growth/repeated clutches, expansion, lifespan/starvation and multi-colony performance. Only two small colonies / six workers were exercised in the competing-source test, not a performance benchmark. Opening death can release a soil unit physically; this turn does not add general dropped-soil retrieval. Phase 2 natural colonies, flight, defense and release packaging remain future work. The colony is not self-sustaining yet. NEXT: physical food delivery enables the next feeding and growth slice.

## T09 current result - refused releases conserved; tested graceful cold restart passes

Development HEAD faults reproduced three-unit worker cargo loss, complete cache loss and partial cache loss through actual block replacement. Dimension SavedData now takes custody of the original component-preserving stacks before normal source cleanup. Stable item identities, exact world ownership checks, per-stack commit and empty source slots prevent repeated successful releases. The queen's founding-death path uses the same custody policy and preserves the actual stack. Pending custody is not delivered food, cache stock or nutrition. Death/removal proceed normally; refused drops appear later at the original source position when its chunk ticks and insertion succeeds. There is no recovery cache or replacement adult.

Fresh unfiltered `t09-build-final-recovered` passed in **59.773 s**: **15 unit / 65 server / 24 model**, all 61 accepted T08 server names retained plus four refusal regressions. Existing discovery/protection/filter rejection and cache assets remain checked; production binary/source archives exclude the development harness, faults, inspectors and test resources. T09's first green build and failed restart remain preserved. The final frozen attempt follows the recovered green build.

`T09/final-02` uses an unchanged copy of accepted `T08/46-a4-world.zip`, preserving its original archive. A **PID 41916** saved normally with flush and exited before B **PID 57432** opened the same unedited world. Both exited 0. The original queen and three workers survive with the same UUIDs, lineages, consumed brood and home/opening state. The original carrier `4e43680e-385f-33b1-9eab-d1ff81e2a4e8` resumes its saved RETURN task with matching cargo components and physically delivers. Selected positive (50,77,-47) and revoked (62,77,-34) soil permissions persist.

Physical balance before/after: **food 5 / soil 25 / mound 24**. Starting A4 contained two cached food units and 24 mound units; three named food units were supplied explicitly through actual world items, and a second production egg queen excavated one dirt unit before dying normally. A's old cache apple/chicken and that carried dirt remained in three refused pending transfers after their sources were deleted. B restores them and releases the same three item UUIDs/stacks; final stock is three named cached units plus two recovered world food units, and 24 mound blocks plus one recovered dirt item. No refund or phantom inventory is involved. `T09/restart-verification.json` contains commands, process identities/exits, frozen inputs and checkpoints. `29-saved-file-physical-audit.json` independently counts the actual A/B saved entity, block-entity, chunk and custody files.

Home-first coverage is precise: B loads the home at FULL/tracked status while the saved carrier chunk/lookup is absent, retaining its claim and consumed brood without reimbursement/recreation. Home entities do not tick during that stage. Normal ticking resumes after loading the carrier chunk. This does not establish actively ticking remote-home reconciliation, arbitrary unloaded schedules, brood-stage restart or offline catch-up; crash recovery is also untested. Consumption/nursing/refill, sustained growth/repeated clutches, excavation/expansion, starvation/lifespan and multi-colony performance remain open. No client was launched; history remains **17 launches, native 11/6, tasks 9/8**. The restart probes/servers are counted separately in the full external T09 report.

The real saved-world diagnostic also exposed a watchdog in the existing soil-permission map decoder. The schema-preserving hash decoder restores all 229,756 actual records in **163.5944 ms** with exact NBT equality. Other harness failures and the smaller recovery checks are preserved and disclosed in `T09/report.md`; no unchanged lottery rerun, timeout/RAM increase, source permission widening, NBT edit or forced shutdown was used.

NEXT: continue - checked release custody and the bounded mature-colony restart now support the next feeding and sustained-growth work.


## T11 worker-built circulation and measured congestion relief

Fresh A4, seed 2026100402, run **7544f303-8d9d-4e0a-9203-4ff2faa47ace**, queen **0915c15f-8000-482e-8ccf-fc6caba553e1**, entrance (59,79,-41), north. Real mature builder **26e2bd44-5633-3fd1-9c2c-a820ecf7475b** was assigned with four mature workers and empty hands. It removed all twelve cells at x=61..62, y=77..78, z=-46..-44, then deposited twelve one-unit blocks. Original 24 + new 12 = **36 actual mound blocks**: 29 base + seven supported upper cells, zero carried/world/custody soil. Original queen, brood pile, cache and entrance anchors remain.

Six new connected supported floors have two-block headroom. Structural chamber floor is 9 -> 15; two brood/cache cells remain reserved, so empty standing floor is **7 -> 13**. In captured live geometry, original-room worker occupancy fell **4 -> 1**, while three nurses physically entered the extension. First recorded use is nurse **8f80fbf7-ae57-32fa-8ef8-a2ceb019ac4f**, `existing_nurse_traversal`. This measures actual occupancy; no feeding-rate or throughput improvement is inferred from volume. The colony has six real workers plus the original queen, retaining all three initial worker/brood identities. Completed layout is also used by ordinary brood emergence selection; no new worker emerging there is claimed for this capture.

Work took **2851 verified loaded construction ticks**, 142.55 seconds at 20 TPS, at action cadence 10 / multiplier 20. A4 client task completed exit 0 in **619.217 s** after the final 15/90/24 build. The source, compiled inputs and archives remained unchanged across A4 (255 files); 255 pre-T11 screenshot/data/individual-caption files are unchanged.

All six supplied apples and eight raw chickens were genuinely consumed through ordinary player drops -> forager -> cache -> nurse mandibles -> actual recipient. No physical supplied food remains. Queen receipts are three apples / three chickens, gained 12,000 sugar / 24,000 protein, spent 6,000 / 12,000 on six distinct new eggs, retaining 6,000 / 12,000. Larvae consumed three apples / five chickens; three new identities have emerged and three further larvae remain with finite nutrition. Body reserve stays zero. Actual NBT independently conserves all fourteen units and credited/spent stores.

Fresh images: [excavation](screenshots/t11-a4-excavation.png), [soil carrying](screenshots/t11-a4-soil-carrying.png), [enlarged mound/traffic](screenshots/t11-a4-expanded-mound.png), [connected interior in use](screenshots/t11-a4-usable-interior.png). All four were opened. The carrying image is constrained by the original entrance/grass occlusion; source identity, real cargo rendering and transport state are verified, without posing or clearing terrain. Observer spectator and disclosed vanilla night vision only. A3's one provisional excavation image and all failure manifests remain preserved.

Final evidence: T11/49-final-build-verification.json, final-physical-route/, 51-a4-independent-world-audit.json and 53-a4-final-artifact-verification.json. The first offline audit rejected the 26.3 compact palette compound with an empty key; correcting that reader schema passed all saved opening ownership, shell/support, connectivity/headroom, mound layering, identity and food checks without another workload. A4 world was archived immediately after close: `T11/t11-a4-world.zip`, SHA-256 **d88c675faca2ca162c172445636c30b718e4b43ef5d0722719f729477993411f**. Failed A1/A2/A3 worlds were likewise archived before later Gradle tasks; no world loss recurred.

T11 adds four client launches: native processes **2 success / 2 failure**, dedicated tasks **1 success / 3 failure**. A2's native success is the disclosed normal closure of a stalled attempt, not successful capture. Lifetime: **24 launches, native 16/8, tasks 11/13**. All launched; failures were gameplay/harness issues, not machine-launch failures. No unchanged full restart followed an unresolved repeated failure; targeted recoveries precede subsequent full workloads. Full command/error/timing history is in T11/report.md.

Broader persistence/offline behavior and cold restart of the new layout/nutrition, natural food supply, adult mortality/lifespan, performance, original four-view/report completion and Phase 2 remain outstanding. This proves one bounded physical nest growth operation and measured local congestion relief, not unattended survival or unlimited expansion.
