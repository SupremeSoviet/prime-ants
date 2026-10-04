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
