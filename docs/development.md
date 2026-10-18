# Development

## Toolchain

| Dependency | Version |
|---|---|
| Minecraft Java | 26.3 |
| Java | 25 (Temurin 25.0.3+9) |
| Gradle | 9.7.1 (wrapper, pinned by checksum) |
| Fabric Loader and Loader JUnit | 0.19.5 |
| Fabric API | 0.161.0+26.3 |
| Fabric Loom | 1.18.2 |
| Fabric server GameTest module | 4.0.32+3434d6d95d |
| Fabric client GameTest module | 6.0.7+4be74c3f5d |
| JUnit Jupiter params | 5.10.0 (matches Loader JUnit's BOM) |

The wrapper and initial build files come from the official Fabric example mod for 26.3.

## Building

Set `JAVA_HOME` to a JDK 25 and run:

```bash
./gradlew build
```

The mod jar and a sources jar end up in `build/libs/`. `build` also runs every test. See [testing](testing.md) for the
faster `checkFast`.

Other useful tasks:

| Task | What it does |
|---|---|
| `genSources` | decompiles Minecraft so you can read its source in your IDE |
| `runClient` | starts a development client with the mod |
| `runGameTest` | runs the in-game tests on a single server |
| `runClientGameTest` | runs the client tests, such as screenshot capture |

## Project layout

| Path | Contents |
|---|---|
| `src/main/java/dev/primeants/` | the mod: `colony` (stages, chambers, development), `worker` (work, food, nursing, stores, catch-up), `brood`, `founding`, `entity`, `item`, `time` |
| `src/client/` | client-side code: rendering |
| `src/gametest/` | in-game tests, in their own `prime_ants_test` mod, so they never ship in the release jar |
| `src/test/` | unit tests |
| `scripts/` | test harness and asset generators (Python) |
| `gradle/` | extra build scripts: sharded test runs, the benchmark server, client capture |
| `docs/` | documentation and design notes |

## Things to know about 26.3

- The official build uses `net.fabricmc.fabric-loom` with plain `implementation` dependencies; there is no separate
  mappings dependency.
- Fabric's testing documentation still shows 26.2 in places. In 26.3, use
  `singleplayer.getConnection().waitForChunksRender()` rather than `singleplayer.getClientLevel().waitForChunksRender()`.
- Client test worlds default to flat terrain with seed 1 and time, weather and mobs disabled.
  `setUseConsistentSettings(false)` gives normal terrain and Survival settings.
- The client test runner defaults to render distance 5, no clouds, no tutorial and muted music.
- Loader JUnit provides the Jupiter engine but not parameterized tests, so `junit-jupiter-params` is added
  separately.
- The configuration cache is disabled.
- On Windows, server start-up may log OSHI/Perflib system-report errors. They are harmless.
