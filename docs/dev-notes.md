# T01 development notes

This is a Minecraft Java 26.3 infrastructure foundation. Colony and brood development are unimplemented.

## Pinned toolchain

Verified on 2026-10-04. The fetched metadata is preserved in the T01 evidence directory (`C:\Users\user\Documents\turnloop\directions\prime-ants-slice1\turns\T01`). No newer stable patch appeared in the requested version lines.

| Dependency | Pin | Primary source |
| --- | --- | --- |
| Minecraft Java | 26.3 | [Official 26.3 template](https://raw.githubusercontent.com/FabricMC/fabric-example-mod/26.3/gradle.properties) |
| Java compilation/runtime | release 25 / Temurin 25.0.3+9-LTS | `01-wrapper-version.log`; local JDK `C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot` |
| Gradle | 9.7.1 | [Official wrapper](https://raw.githubusercontent.com/FabricMC/fabric-example-mod/26.3/gradle/wrapper/gradle-wrapper.properties) |
| Fabric Loader and Loader JUnit | 0.19.5 | [26.3 Loader registry](https://meta.fabricmc.net/v2/versions/loader/26.3), stable flag true |
| Fabric API | 0.161.0+26.3 | [Maven metadata](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml) |
| Loom | 1.18.2 | [Maven metadata](https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml), stable release; replaces template `1.18-SNAPSHOT` |
| Server GameTest module | 4.0.32+3434d6d95d | Resolved API dependency and matching Maven sources |
| Client GameTest module | 6.0.7+4be74c3f5d | Resolved API dependency and matching Maven sources |
| JUnit Jupiter parameters | 5.10.0 | Matches Loader JUnit 0.19.5's JUnit BOM; [published POM](https://maven.fabricmc.net/net/fabricmc/fabric-loader-junit/0.19.5/fabric-loader-junit-0.19.5.pom) |

The wrapper scripts, jar, and initial properties came directly from the official 26.3 branch. Download URLs and hashes are in `source-downloads.json` and `wrapper-downloads.json`. The distribution is additionally pinned by `distributionSha256Sum`, from [Gradle's checksum](https://services.gradle.org/distributions/gradle-9.7.1-bin.zip.sha256).

## Source inspection before API-dependent code

`02-genSources.log` records successful `genSources` (exit 0, 94.903 seconds). Common and client game sources were generated and inspected before writing entrypoints or GameTests. Exact extracted files are preserved under `T01/inspected-sources/`.

Local game source jars:

- `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-common-7e9a32a5b8/26.3/minecraft-common-7e9a32a5b8-26.3-sources.jar`
- `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-clientOnly-7e9a32a5b8/26.3/minecraft-clientOnly-7e9a32a5b8-26.3-sources.jar`

Matching API/Loom source jars are under `C:\Users\user\.gradle\caches\prime-ants-source-inspection\`; their exact URLs and SHA-256 hashes are in `api-source-downloads.json`:

- `fabric-gametest-api-v1-4.0.32+3434d6d95d-sources.jar`
- `fabric-client-gametest-api-v1-6.0.7+4be74c3f5d-sources.jar`
- `fabric-loom-1.18.2-sources.jar`

Relevant resolved classes and signatures:

| Class | Observed signature or behavior |
| --- | --- |
| `net.minecraft.gametest.framework.GameTestHelper` | `spawnItem(Item, float, float, float): ItemEntity`; `runAfterDelay(long, Runnable)` schedules against test ticks; `getTick(): long`; `assertItemEntityPresent(Item)`; `assertEntityNotPresent(EntityType<?>)`; `assertTrue(boolean, String)` |
| `net.minecraft.world.entity.item.ItemEntity` | `getAge(): int`; normal `tick()` increments age; tests observe that increment without invoking `tick()` |
| `net.fabricmc.fabric.api.gametest.v1.GameTest` | Public instance method taking `GameTestHelper`; default empty 8x8 fixture, barrier enclosure, one attempt, required=true; `maxTicks()` |
| `TestAnnotationLocator.TestMethod` | Namespace is test mod ID; class + method name converted from camelCase to snake_case |
| `GameTestServer` | No matching tests exit -1; completed required failures become process exit count; crashes exit 1 |
| `JUnitLikeTestReporter` | Nested `testsuite` elements; no tests/failures count attributes. Count `testcase` and failure children instead |
| `FabricGameTestRunner` / `GameTestSystemProperties` | `fabric-api.gametest.report-file` saves XML; `fabric-api.gametest.filter` selects tests |
| `FabricApiTesting` (Loom) | Creates `gametest`; connects `runGameTest` to `check`; client directory is `build/run/clientGameTest`, server directory `build/run/gameTest` |
| `TestWorldBuilder` | `setUseConsistentSettings(boolean)`; `adjustSettings(Consumer<WorldCreationUiState>)`; `create(): TestSingleplayerContext` |
| `WorldCreationUiState` | `setSeed(String)` changes world options |
| `TestSingleplayerContext` | `getConnection(): TestServerConnection`; `close()` leaves the world |
| `TestServerConnection` | `waitForChunksRender(): int`; default chunk-load timeout is one minute of game ticks |
| `ClientGameTestContext` | `waitForScreen(Class<? extends Screen>)`; `waitTicks(int)`; `takeScreenshot(TestScreenshotOptions): Path` |
| `TestScreenshotCommonOptions` | `disableCounterPrefix()`; `withSize(int, int)`; `withDestinationDir(Path)` |

## Observed 26.x gotchas

- The official build uses `net.fabricmc.fabric-loom`, plain `implementation` dependencies, and Java 25. No mappings dependency or old remapping configuration is transplanted.
- [Official automated testing documentation](https://docs.fabricmc.net/develop/automatic-testing) currently displays 26.2. Its client sample's `singleplayer.getClientLevel().waitForChunksRender()` does not match the resolved 26.3 API. Use `singleplayer.getConnection().waitForChunksRender()`.
- Client test world builders default to flat terrain, fixed seed 1, disabled time/weather/mobs. `setUseConsistentSettings(false)` gives normal default terrain and survival settings. The capture changes only the seed, not terrain, time, weather, player pose, or entity population.
- Fabric's client test runner itself defaults to render distance 5, clouds off, no tutorial, no chunk fade, and music muted. These API defaults are retained and disclosed; no comparison verdict is produced.
- Development tests live in their own `prime_ants_test` mod/source set. Production archives use only common/client outputs. Both binary and sources archives must be inspected for test/capture leakage.
- Configuration cache is disabled for this initial test workflow; the template enabled it. No machine-specific JVM hosts-file flag is added.
- On this machine scripts are disabled by PowerShell policy. The evidence helper is run in a child process with `-ExecutionPolicy Bypass`; the global policy is unchanged.
- Loader JUnit supplies the Jupiter engine and launcher but not parameterized tests. The first build's server test passed, then `compileTestJava` failed on missing `org.junit.jupiter.params`. Add `junit-jupiter-params:5.10.0`, matching Loader JUnit's BOM, and demonstrate recovery with the smaller `test` task before resuming `build`.
- Server startup emitted Windows OSHI/Perflib system-report errors and a missing empty client resources-directory warning, but continued and executed the test. These diagnostic warnings are preserved; no registry or system configuration is changed.
- The successful client run additionally logged `Illegal option value 0 for Anisotropic Filtering` (the API's test defaults set zero) and Realms authorization diagnostics for the development token. Rendering, world creation, screenshot capture, and automatic exit still completed. These remain documented upstream/environment diagnostics; no authentication or settings workaround is applied.

## Reproduction

From the repository, set `JAVA_HOME` to the Java 25 JDK and prepend its `bin` to `PATH`. Run commands separately and stop on nonzero exits:

```powershell
.\gradlew.bat --version
.\gradlew.bat genSources --console=plain
.\gradlew.bat tasks --all --console=plain
.\gradlew.bat build --console=plain
.\gradlew.bat runClientGameTest --console=plain
```

`runGameTest` is the verified server task. Server XML is `build/test-results/gametest/server.xml`; unit XML is under `build/test-results/test/`, with HTML under `build/reports/tests/test/`. `build` executes both suites and checks that the named bootstrap test appears in a fresh server XML report. Client capture is a separate, dedicated attempt after the final passing build.

The optional `scripts/Invoke-GradleEvidence.ps1` takes `-Name`, `-GradleArgs` and `-EvidenceDirectory`, captures command/start/end/elapsed time/output, reads `$LASTEXITCODE` immediately, and exits with that code.
