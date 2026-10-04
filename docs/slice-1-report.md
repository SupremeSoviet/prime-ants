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
