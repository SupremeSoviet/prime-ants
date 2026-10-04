# Extraction 05 — Automated tests, visual QA harness, autonomous development pipeline

Project: "Prime Ants" (mod id `formic_frontier`, Gradle project `formic-frontier`, package `com.formicfrontier`, Minecraft 1.21.11 / Fabric Loader 0.19.2 / Fabric API 0.141.3+1.21.11).
Source: `C:\Users\user\Documents\Codex\2026-04-26\new-chat`, branch `rebuild/anthills-from-scratch`, HEAD `75a70f7` (2026-07-13). Extracted read-only on 2026-09-25.

This document is raw material. It records what the old project's tests guaranteed, how the visual QA harness and the model-based assessment worked, and how the autonomous loop was orchestrated, with `path:line` citations.

---

## 0. Method, scope, caveats

**Read in full:** the gametest class and its `fabric.mod.json`; `VisualQaScenes.java`; `VisualQaClient.java`; every file in `scripts/` (`*.ps1`, `*.cmd`, `zai-codex-proxy.py`); `tools/visual_assessment_gate.py`, `openai_visual_assessment.py`, `glm5v_visual_assessment.py`, `content_feature_gate.py`, `visual_qa_report.py`, `validate_assets.py`; all 7 `.codex/agents/*.toml`; the `formic-visual-assessment` skill (SKILL.md, rubric, report template, `agents/openai.yaml`); `docs/autonomous-dev.md`, `docs/local-stack/*.md`, `docs/manual-playtest.md`, both matrix templates; the full commit history (37 commits).

**Also read, because they bear on what the tests guarantee:** all four JUnit test files under `src/test/java` (56 tests); `build.gradle`; `.github/workflows/build.yaml`; `gradle.properties`; `.gitignore`; `.gitattributes`; `README.md`; `docs/visual-intent/formic-visual-intent.md`; `docs/content-intent/formic-content-intent.md`; `docs/visual-intent/reference-manifest.json`; the relevant parts of `FormicCommands.java`, `ColonySavedState.java`, `ColonyDiscoveryService.java`, `ColonyRecurringEvents.java`, and `ModNetworking.java`; the roadmap headings; and `git reflog`.

**Nothing from the repo was executed.** No Gradle, no scripts, no state-changing git commands. I ran one experiment outside the repo: two throwaway PowerShell scripts in the scratchpad, to confirm how Windows PowerShell 5.1 handles `exit` inside a nested script (see §8.5).

**No runtime artifacts exist in this checkout.** There is no `build/`, `run/` or `.local/` directory. That means the loop logs, screenshots, assessment reports, runtime feature matrices, and `visual-progress.jsonl` are all gone. Everything below about how runs actually went is inferred from code, docs, commit messages and the reflog.

**Secrets:** none are committed. A regex scan of tracked text files for key, token and private-key patterns came back negative. Secrets were supplied only through environment variables (Appendix B). Docs use `"<secret>"` placeholders (`docs/autonomous-dev.md:71`, `README.md:80`). The git author identity is the same on all 37 commits and is not reproduced here.

**Path aliases used below** (all relative to the repo root):
- `GT` = `src/gametest/java/com/formicfrontier/test/FormicFrontierGameTest.java` (3085 lines)
- `CET` = `src/test/java/com/formicfrontier/sim/ColonyEconomyTest.java` (1318)
- `OBT` = `src/test/java/com/formicfrontier/world/structure/OrganicBuildingBlueprintTest.java` (842)
- `TMT` = `src/test/java/com/formicfrontier/world/structure/TieredMoundBlueprintTest.java` (161)
- `SVT` = `src/test/java/com/formicfrontier/world/structure/SubterraneanVaultBlueprintTest.java` (126)
- `VQS` = `src/main/java/com/formicfrontier/qa/VisualQaScenes.java` (1924)
- `VQC` = `src/client/java/com/formicfrontier/client/VisualQaClient.java` (344)
- `LOOP` = `scripts/autonomous-loop.ps1` (1371)

---

## 1. Key findings

1. **There are 115 automated tests.** 59 are server gametests in a single class, and 56 are JUnit tests: 36 in the economy/sim class and 20 blueprint-data tests. All 59 gametests run synchronously inside one tick; none lets the game advance. Simulated time means calling `*.tick()` service methods directly.
2. **The tests are mostly exact-coordinate layout contracts.** The gametest file has 292 `assertBlockPresent(block, center.offset(x,y,z))` calls and 29 `assertBlockInColumn` calls, plus many exact air checks. They pin furniture and markers to specific offsets, alongside mass-per-layer taper thresholds. The R2 commit messages show building geometry being bent to fit test coordinates. For example, one commit capped the crown reach at ±5 because a radius-6 crown "breaks the cache column assertion in tribute/truce diplomacy tests" (commit `15b5932`).
3. **Some tests enforce placeholder behaviour.** Planned, construction, damaged and repairing buildings must be a single marker block with air above (`GT:226-234`, used at `GT:415-418`). Upgraded buildings must look identical to complete ones.
4. **Over a third of the gametests involve the QA harness rather than only gameplay.** Six tests seed QA scenes or check camera math directly (group J). Another 17 embed `VisualQaScenes.scenes().contains(...)` checks, which couples the production test suite to the screenshot harness. That makes 23 of 59; there are 57 scene-id checks in 22 tests.
5. **The world-visual gate ignored test failures.** `autonomous-gate.ps1` calls `test-mod.ps1` and `doctor.ps1` with `&` and never checks `$LASTEXITCODE` (`scripts/autonomous-gate.ps1:94-96`). Both scripts signal failure only with `exit 1`. I confirmed empirically that PowerShell 5.1 continues past a nested `exit 1`. The final decision rests on `tools/visual_assessment_gate.py`, which never reads the test summary. In the world-visual track, red gametests therefore could not block an iteration. The content gate did check (`tools/content_feature_gate.py:62-76`).
6. **The visual gate was self-graded.** Since `88b040f` (2026-07-12) the default assessor is "manual". The same Codex child that edits the code writes `formic-visual-assessment.md` with a fixed assessor string (`scripts/autonomous-gate.ps1:112-117`). The prompt also tells that child to update the matrix row statuses the gate relies on (`LOOP:830`). The maker/checker split existed only in prompts and agent TOMLs.
7. **With the GLM profile, the child could not see screenshots.** The Z.AI proxy forwards only text: `text_from_content` drops image parts (`scripts/zai-codex-proxy.py:42-66`) and the advertised `input_modalities` is `["text"]` (`:245`). Yet the prompt required a "manual original-resolution" review of the PNGs.
8. **The proxy's auth token was never enforced.** `ZAI_CODEX_PROXY_TOKEN` is read and reported in `/health` (`scripts/zai-codex-proxy.py:641`, `:272`), but `do_POST` never checks an `Authorization` header (`:280-303`). Any local process could use the Z.AI key through the proxy.
9. **The visual gate could only pass once the whole baseline was self-certified.** It requires every required visual-baseline row in the runtime matrix to be `pass` (`tools/visual_assessment_gate.py:425-439`). The template ships 11 such rows as `unknown`. A world-visual iteration therefore fails unless the agent marks every row `pass`. The child also picks its own gate by writing `activeTrack` (`LOOP:402-420`), and the content gate is far easier to pass.
10. **The visual QA screenshots show heavily staged scenes.** Examples: forest-floor dressing that the game never generates (`VQS:1601-1646`, used only by QA), ants spawned with forced work states and floating item/block markers, cleared camera corridors, and NoAI lineup ants. Buildings themselves are placed through the real placer ("PREVIEW = GAME", `VQS:352-354`).
11. **The scene list is defined inconsistently in about 10 places** (65 / 23 / 16 / 45 / 19 / 17 / 13 entries; §6.5). The 45 focused "structure" scenes, which were the entire July rebuild, have no automated gate. They were only reviewable manually.
12. **The loop never commits.** No script calls `git commit` (a grep finds only a prompt line, `LOOP:775`). All 37 commits were made outside the loop. Three carry `Co-Authored-By: Claude Opus 4.8`. The reflog shows a history rewrite on 2026-05-01 and a `reset: moving to HEAD` on 2026-06-22.
13. **The debug command is unguarded.** `/formic` has no `.requires(...)` permission check (`src/main/java/com/formicfrontier/command/FormicCommands.java:24-25`). `/formic qa scene <name>` wipes all colonies and flattens up to a 136-block radius, clearing up to the world ceiling on the first run (`VQS:344,350`). Any player can run it in a normal world.
14. **Committed portability hazards:** a `gradle.properties` JVM flag pointing name resolution at a WSL-style hosts file (`gradle.properties:1`), committed `__pycache__/*.pyc` and `textures/block.zip`, and a junk `.gitignore` line (`/II, data*`, `.gitignore:14`).
15. **Worth porting:** the blueprint JUnit invariants (BFS connectivity, carved rooms, stair headroom, enclosure except mouths, variant distinctness), `validate_assets.py`, the scene command plus client capture driver, freshness markers, per-attempt git checkpoints, and the P0–P3 rubric. Drop the self-graded gate, the PowerShell supervisor as written, the text-only proxy, staged QA decoration, and coordinate-exact gametests.

---

## 2. Test infrastructure

### 2.1 Source sets and runners

- **Split source sets:** `main` + `client` (`build.gradle:13-22`).
- **Gametests:** `fabricApi.configureTests { createSourceSet = true; modId = "formic_frontier_test"; enableGameTests = true; enableClientGameTests = false; eula = true }` (`build.gradle:24-32`). The gametest mod declares entrypoint `fabric-gametest` → `com.formicfrontier.test.FormicFrontierGameTest`, environment `*` (`src/gametest/resources/fabric.mod.json:1-12`). **There are no client gametests**, so no screen rendering or client networking is exercised by any automated test.
- **Gametests run as part of `./gradlew build`.** This is stated in `README.md:52-53`, the CI step is named "Build and server GameTests" (`.github/workflows/build.yaml:28-29`), and `test-mod.ps1` scans `build\run\gameTest\logs\latest.log` (`scripts/test-mod.ps1:43`). I did not run it.
- **JUnit:** `testImplementation "net.fabricmc:fabric-loader-junit"` (`build.gradle:43`). The `test` task uses the JUnit Platform with a 2 GB heap (`build.gradle:76-92`). It copies four `fabric.*` system properties from `.gradle/loom-cache/launch.cfg`, but only if that file already exists at Gradle configuration time (`build.gradle:79-91`). This is an unverified risk: on a fresh checkout the properties may be missing on the first build.
- **Asset validation:** `validateAssets` runs `python tools/validate_assets.py`, and `check` depends on it (`build.gradle:94-100`). `python` must be on PATH, or set via `-DpythonExecutable`.
- **Visual QA properties** passed to `run*` tasks: `formic.visualQa`, `.dir`, `.exit`, `.world`, `.scope`, `.scenes` (`build.gradle:58-67`).
- **Test style:** all 59 methods use a bare `@GameTest` with no structure, timeout or batch arguments. Every test ends with `helper.succeed()` in the same tick. There are 59 `succeed()` calls and no `runAfterDelay`, `succeedWhen` or `onEachTick`. One test uses `helper.makeMockServerPlayerInLevel()` (`GT:2077`). Three tests rely on a production debug hook, `AntEntity.debugWorkTarget` (`GT:1102,1119,1135`; defined at `src/main/java/com/formicfrontier/entity/AntEntity.java:221`).

### 2.2 Test counts over time (counted per commit)

| Commit | Date | Gametests | JUnit | QA scene constants | Note |
|---|---|---|---|---|---|
| 9ed2fa7 | 2026-04-29 | 11 | 11 | 0 | remote-side "Add Prime Ants mod project" |
| a74a7fa | 2026-05-01 | 13 | 11 | 6 | QA harness v1 |
| b9f9faa | 2026-05-01 | 48 | 25 | 19 | "Renovate colony scale and tablet UI" |
| cc9db89 | 2026-06-28 | 49 | 35 | 19 | content-track rows (+737 JUnit lines) |
| f5e9857 / c3a7aeb | 2026-06-28 | 49 | 35 | 21 | commit bodies say "All 50 gametests pass" but the file had 49 `@GameTest` |
| 946ea44 | 2026-07-11 | 49 | 36 | 21 | CasteBalancer test (last master commit) |
| 6fb5bcd | 2026-07-11 | 49 | 36 | 21 | "Reset anthill structures to minimal markers" (−746 lines in GT) |
| 87d8192…f4b4e04 | 07-11 → 07-12 | 49 → 59 | 37 → 56 | 24 → 64 | one commit per building blueprint |
| 75a70f7 (HEAD) | 2026-07-13 | 59 | 56 | 65 | |

### 2.3 Shared gametest fixtures and helpers

- **`prepareCampusArea(helper, origin, radius=72)`** (`GT:3065-3084`). Inside a disc of the given radius: dirt at y−2 and y−1, grass at y0, and air from y1 to y26. Radii used across tests: 48, 64, 72 (default), 76, 80, 96, 104, 108, 112, 120, 130, 150. The origin is always relative `(2,3,2)`, except the two vault/hub endgame tests, which use `(2,20,2)` to leave room underground.
  - This writes far outside the default test structure. Tests share the global `ColonySavedState`, whose `clearColonies()` resets ids to 1 (`src/main/java/com/formicfrontier/world/ColonySavedState.java:90-94`), and some entity counts filter only by work state. Isolation relies on synchronous execution. That is a latent cross-test interference risk, not an observed failure.
- **`assertMinimalBuildingMarker(center, block)`** (`GT:2226-2234`): the expected block at the center, air at +1 and +2.
- **`assertBlockInColumn(base, block, minOff, maxOff)`** (`GT:3045-3052`): the block appears somewhere between `base.above(minOff)` and `base.above(maxOff)`. This y-tolerance is used for event and diplomacy markers.
- **`countMoundLayer(center, y, r)`** (`GT:3023-3043`): counts 19 "mound" block types in a (2r+1)² square at height y. **`isOrganicMoundShell(block)`** (`GT:2814-2845`) accepts 30 block types, including GOLD_BLOCK, AMETHYST_BLOCK, SLIME_BLOCK, OCHRE_FROGLIGHT, CUT_COPPER and mushroom blocks. The two lists differ: the mass counter excludes mycelium, blackstone, froglight, gold and others. The "organic" check is therefore weak and does not enforce a native palette.
- **Culture helpers:** `landmarkMarker(culture)` returns Leafcutter MOSS_BLOCK, Fire RED_TERRACOTTA, Carpenter MANGROVE_ROOTS, Amber CHISELED_TUFF (`GT:2167-2174`). `trailHeadDressingBlock` returns Leafcutter PODZOL, Fire COARSE_DIRT, Carpenter ROOTED_DIRT, Amber PACKED_MUD (`GT:2217-2224`). `isLandmarkTrail` accepts DIRT_PATH or COARSE_DIRT (`GT:2176-2178`).
- **`firstApproachTrailBlock`** (`GT:2180-2200`) walks an L-path from the player to the colony, skipping 8 blocks near the start and 13 near the end. **`hasGroundedTrailHeadDressing`** (`GT:2202-2209`) checks for ROOTED_DIRT on the left and the culture dressing block on the right.

### 2.4 Building "profile" contracts, as asserted by the gametest helpers

Coordinates are offsets from the building center. "Mass y:r≥n" means `countMoundLayer(y, r) ≥ n`. These helpers are used both by the blueprint gametests and by the colony and event tests.

- **Tiered queen mound, stage 1** (`assertTieredMoundProfile`, `GT:2847-2885`).
  - Structure: center NEST_MOUND over NEST_CORE; NEST_MOUND at (0,23,1); air at (0,24,1), so the height is exactly 24.
  - Openings: air at three facade mouths (0,2,−6), (−2,9,−4), (2,15,−3) and three rooms behind them (0,2,2), (−2,9,1), (2,15,1).
  - Floor 1: CHEST (−4,1,2), BARREL (4,1,2), CRAFTING_TABLE (3,1,5), CHITIN_BED (0,1,5), LANTERN (−4,2,2).
  - Floor 2: CHEST (−5,8,1), COMPOSTER (1,8,3).
  - Floor 3: PHEROMONE_ARCHIVE block (2,14,3), BELL (4,14,2).
  - Stairs: MUD_BRICK_STAIRS at (4,0,3), (−2,6,3), (−4,7,0), (1,12,0).
  - Taper, strictly decreasing: y1:r12 ≥120, y8:r10 ≥70, y14:r8 ≥35, y20:r6 ≥5.
- **Great Mound, stage 2** (`assertGreatMoundProfile`, `GT:2887-2972`).
  - Structure: center over NEST_CORE; organic block at (0,32,1) and air at (0,33,1), so it is 33 tall.
  - Openings: 12 must be air. Six facade mouths: (0,2,−7), (−7,2,0), (6,2,0), (−2,9,−4), (2,15,−3), (−1,22,−2). Six rooms: (0,2,2), (−7,2,4), (6,2,5), (−2,9,1), (2,15,2), (−1,22,1).
  - The stage-1 furniture is kept.
  - Larder annex: CHEST (−9,1,4), BARREL (−5,1,4), HAY (−9,1,6), COMPOSTER (−5,1,6), FOOD_NODE (−7,1,6).
  - Workshop annex: CRAFTING_TABLE (4,1,7), SMITHING_TABLE (6,1,7), GRINDSTONE (8,1,7), OCHRE_FROGLIGHT (8,2,5).
  - Crown map room: CARTOGRAPHY_TABLE (−3,21,2), LECTERN (1,21,2), AMETHYST (−2,21,3), CUT_COPPER (0,21,3), PHEROMONE_ARCHIVE (−1,21,3), LIGHTNING_ROD (−1,22,3).
  - Stairs: six, each with two blocks of headroom, at (4,0,3), (−2,6,3), (−4,7,0), (1,12,0), (4,13,1), (−2,19,1). The crown landing (−3,20,1) also has headroom.
  - Taper, strictly decreasing: y1:r16 ≥140, y8:r13 ≥90, y15:r10 ≥50, y22:r8 ≥20, y28:r6 ≥8, y32:r3 ≥1.
- **Queen Vault**, underground beneath the mound (`assertQueenVaultProfile`, `GT:2974-3021`).
  - Stairs: 12 MUD_BRICK_STAIRS with two-block headroom in two flights, (−4,0,3)…(1,−5,3) then (4,−6,3)…(−1,−11,3).
  - Carved air at (0,−4,2), (−3,−10,3), (4,−10,3), (0,−10,4), (1,−10,4).
  - Guard vestibule (y−5): CHEST (−4,−5,2), BARREL (4,−5,2), TARGET (−3,−5,5), SMITHING_TABLE (3,−5,5), NEST_CORE (0,−5,5).
  - Treasury (y−11): CHEST (−6,−11,3), BARREL (−1,−11,2), GOLD (−5,−11,5), NEST_CORE (−3,−11,5), AMETHYST (−1,−11,5).
  - Brood sanctum (y−11): CHITIN_BED (2,−11,3) and (7,−11,3), BONE (2,−11,5), HONEYCOMB (6,−11,5), OCHRE_FROGLIGHT (4,−11,5).
- **Food store** (`GT:2236-2265`).
  - FOOD_CHAMBER center, not over NEST_CORE. Air at the mouth (0,2,−5) and inside (0,2,1).
  - CHEST (−4,1,1), BARREL (4,1,1), HAY (−3,1,4), COMPOSTER (3,1,4), FOOD_NODE (0,1,4), LANTERN (±4,2,1).
  - Crown organic at (0,6,1), air at (0,7,1).
  - Mass y0:r12 ≥90, y4:r12 ≥20, with base > shoulder. A comment says the threshold was relaxed because worker routes replace ground (`GT:2260-2261`).
- **Nursery** (`GT:2784-2812`).
  - NURSERY_CHAMBER, not over the core. Air at (0,2,−5) and (0,2,1).
  - CHITIN_BED (±4,1,1), HONEYCOMB (−3,1,4), BONE (3,1,4), BARREL (−2,1,5), OCHRE_FROGLIGHT (2,1,5), LANTERN (±4,2,1).
  - Crown at y7, air at y8. Mass y0:r13 ≥100, y5 ≥20.
- **Mine** (`GT:2267-2299`).
  - MINE_CHAMBER. Air at (0,2,−5) and (0,2,1).
  - Pit: air at (0,0,3) and (0,−1,3); ORE_NODE at (0,−2,3); COBBLED_DEEPSLATE at (−2,−1,3).
  - CHEST (−4,1,1), BARREL (4,1,1), DEEPSLATE_WALL (−3,1,4), IRON_ORE (3,1,4), LANTERN (±2,2,4).
  - Crown at y6, air at y7. Mass y0:r12 ≥100, y4 ≥20.
- **Chitin farm** (`GT:2301-2327`).
  - CHITIN_BED center. Air at (0,2,−5) and (0,2,1). PACKED_MUD at (0,0,4).
  - CHITIN_BED (±4,1,1), BONE (−3,1,3), HONEYCOMB (3,1,3), CHITIN_NODE (−1,1,4), COMPOSTER (1,1,4), LANTERN (±4,2,1).
  - Crown at y5, air at y6. Mass y0:r13 ≥120, y4 ≥20.
- **Barracks** (`GT:2329-2361`).
  - BARRACKS_CHAMBER. A 5-wide entrance: air at (±2,2,−5) and (0,2,1). PACKED_MUD at (0,0,4).
  - CHITIN_BED at (±6,1,1) and (±5,1,4); ANVIL (−3,1,5), SMITHING_TABLE (0,1,5), TARGET (3,1,5); LANTERN (±6,3,1).
  - Organic shoulders at (±8,2,1). Crown at y7, air at y8. Mass y0:r15 ≥150, y5 ≥35.
- **Market** (`GT:2363-2395`).
  - MARKET_CHAMBER. Air at (±2,2,−5). The courtyard is open to the sky: air at (0,1..5,0). PACKED_MUD at (0,0,3).
  - CHEST (−4,1,0), BARREL (4,1,0), HAY (−3,1,3), COMPOSTER (3,1,3), BELL (0,1,4), OAK_FENCE (±3,1,−2), LANTERN (±3,2,−2).
  - Earth banks at (±6,2,1). Mass y0:r13 ≥140, y2 ≥45, y3 ≥20, strictly decreasing.
- **Trade Hub** (`GT:2397-2437`).
  - MARKET_CHAMBER. A gate: air at (±2,2,−7). The court is open to the sky: air at (0,1..8,2). PACKED_MUD at (0,0,4). Side passages: air at (±4,2,3).
  - Court: CHEST (−5,1,1), BARREL (5,1,1), HAY (−3,1,4), HONEYCOMB (3,1,4), CARTOGRAPHY (−2,1,6), LECTERN (2,1,6), BELL (0,1,6).
  - Warehouse: CHEST (−10,1,3), BARREL (−10,1,5), HAY (−8,1,5), HONEYCOMB (−6,1,5), CUT_COPPER (−4,1,5).
  - Brokerage: CARTOGRAPHY (10,1,3), LECTERN (10,1,5), PHEROMONE_ARCHIVE (4,1,5), GOLD (6,1,5), AMETHYST (8,1,5).
  - No mass check.
- **Pheromone archive**, two variants detected by the archive block at (2,6,3) or (0,6,3) (`GT:2439-2487`).
  - Air at (0,2,−5), (0,2,1), (0,7,−3), (0,7,1).
  - Lower hall: CHEST (−4,1,1), BARREL (4,1,1), CHISELED_BOOKSHELF (−3,1,4), LECTERN (3,1,4), archive block (0,1,4), LANTERN (±4,2,1).
  - Loft, relative to its detected center ux: BOOKSHELF, CHISELED_BOOKSHELF, AMETHYST, LANTERNs.
  - Stairs mirrored with the variant: MUD_BRICK_STAIRS at (±3,0,2) and (±1,4,2).
  - Organic at (±6,3,1) and (0,10,1), air at (0,11,1). Mass y0:r13 ≥140, y5:r11 ≥55, y8:r9 ≥15, decreasing.
- **Armory**, forge variant detected by the ARMORY block at (3,1,1) or (5,1,1) (`GT:2489-2537`).
  - Air at (0,2,−6) and (0,2,1).
  - Forge: ANVIL, SMITHING_TABLE, BLAST_FURNACE, GRINDSTONE, LANTERNs.
  - Weapon vault at x = ±4: CHEST, BARREL, IRON_BLOCK, TARGET, IRON_BARS, LANTERN.
  - Shared passage: air at (±1,1,2).
  - Organic at (±7,3,1) and (0,8,1), air at (0,9,1). Mass y0:r14 ≥190, y5:r13 ≥45, y8:r9 ≥12.
- **Diplomacy shrine** (`GT:2539-2571`).
  - Air at (0,2,−6). The sanctum is open to the sky at (0,1..8,1).
  - CHISELED_TUFF (±3,1,1), LANTERN (±3,2,1), HONEYCOMB (−2,1,3), GOLD (2,1,3), CANDLE (±2,2,3), shrine block (0,1,3), BELL (0,1,4).
  - Horns: organic at (±6,2,1) and (0,4,6). Mass y0:r13 ≥170, y6:r12 ≥12, y7:r11 ≥4.
- **Resin depot**, workshop variant detected at (∓1,1,4) (`GT:2573-2620`).
  - Mouth air at (wx,2,−6) and (wx,2,1).
  - Workshop: BARREL, CHEST, CAULDRON, CRAFTING_TABLE, LANTERNs.
  - Vault at x = ±4: two BARRELs, HONEY_BLOCK, HONEYCOMB, OCHRE_FROGLIGHT, LANTERN.
  - Throat: air at (±1,1,2).
  - Organic at (±8,2,3) and (0,7,2), air at (0,8,2). Mass y0:r14 ≥190, y5:r13 ≥38, y7:r10 ≥12.
- **Fungus garden**, hall variant detected at (∓1,1,6) (`GT:2622-2662`).
  - Mouth air at (hx,2,−6) and (hx,2,2).
  - MYCELIUM with BROWN_MUSHROOM, PODZOL with RED_MUSHROOM, brown and red mushroom blocks, two SHROOMLIGHTs, COMPOSTER, BARREL.
  - Organic at (±8,2,3), (0,4,8) and (0,6,2), air at (0,7,2). Mass y0:r14 ≥200, y4:r13 ≥45, y6:r10 ≥12.
- **Venom press**, hall variant detected at (∓1,1,5) (`GT:2664-2711`).
  - Aisle air at (hx,2,−6) and (hx,2,1).
  - Hall: POLISHED_BLACKSTONE with BREWING_STAND, BARREL, two VERDANT_FROGLIGHTs, SLIME with LIME_STAINED_GLASS, and CAULDRON + IRON_CHAIN + PISTON stacked.
  - Vault at vx = −hx·4: BARREL, CHEST, SLIME, BREWING_STAND, SOUL_LANTERN.
  - Organic at (±8,2,1), (−2hx,5,7) and (0,8,2), air at (0,9,2). Mass y0:r14 ≥210, y4:r13 ≥55, y8:r8 ≥8.
- **Watch post**, lookout variant detected by the WATCH_POST block at (1,7,3), (−1,7,3) or (0,7,4) (`GT:2713-2782`).
  - Air at (0,2,−5) and (0,2,1).
  - Guard room: BARREL (−3,1,1), CHEST (3,1,1), LANTERN (±3,2,1), FLETCHING_TABLE (−3,1,3), TARGET (3,1,3), post block (0,1,4), BELL (0,2,4).
  - Lookout: balcony air at (ux,8,−3); CARTOGRAPHY, LECTERN, LANTERNs, OCHRE_FROGLIGHT, DEEPSLATE_IRON_ORE, LIGHTNING_ROD.
  - Stairs: at least 6 MUD_BRICK_STAIRS in the box x∈[−5,5], y∈[0,5], z∈[−4,6].
  - Mass y0:r13 ≥135, y6:r10 ≥40, y13:r7 ≥6, decreasing. Air at (0,14,1).

---

## 3. All 59 gametests, as acceptance criteria

Each entry gives the method name with its location, the setup, what it asserts, and a **Behaviour** line restating it as an acceptance criterion. All tests start at relative origin (2,3,2) on a flattened campus (radius 72 unless stated). Unless noted, `createColony` means `ColonyService.createColony(level, abs(origin))`, which creates a player-allied colony.

### A. Colony founding and starter layout (9)

- **A1 `createColonyPlacesCoreChambersAndResources`** (`GT:42-63`)
  - Setup: `createColony`.
  - Asserts:
    - The queen mound matches the tiered profile.
    - The food store, nursery, mine and barracks match their profiles at `ColonyBuilder.siteFor(origin,type,0)`.
    - FOOD_NODE at +(54,0,8), ORE_NODE at +(8,0,54), CHITIN_NODE at +(−54,0,8).
    - DIRT_PATH at (0,0,−10) and (0,0,−9).
    - `casteCount(GIANT)==0`.
  - **Behaviour:** a new colony builds the 24-block queen mound, four furnished starter chambers at fixed campus sites, three resource nodes about 54 blocks out to the E, S and W, and a path leading south. It starts without a giant.
- **A2 `starterColonyHasEconomyButNoFreeGiant`** (`GT:65-78`)
  - Asserts: food, ore and chitin are all > 0; `AntCaste.GIANT.canGrowFrom(colony)` is false.
  - **Behaviour:** the starter colony has seed stock but cannot afford a giant.
- **A3 `colonyCreationAnchorsHighRequestsToGround`** (`GT:80-94`)
  - Setup: request founding 8 blocks above ground.
  - Asserts: `colony.origin()==abs(origin)`; NEST_MOUND at the origin; solid block below.
  - **Behaviour:** a founding request in the air snaps down to the surface and has solid support.
- **A4 `cultureStarterQueuesAreAppliedToCreatedColonies`** (`GT:167-179`)
  - Setup: radius 150, `clearColonies`, four `createWildColony` calls at x = −72, −24, 24, 72.
  - Asserts: the head of each build queue is DIPLOMACY_SHRINE (Amber), FUNGUS_GARDEN (Leafcutter), WATCH_POST (Fire), RESIN_DEPOT (Carpenter).
  - **Behaviour:** each culture's colony queues its signature building first.
- **A5 `starterAntsSpawnAboveSupportedFloor`** (`GT:481-511`)
  - Asserts:
    - Every colony ant within ±16 horizontally stands on a non-air block.
    - The seven spawn points (0,1,−11), (±2,1,−11), (0,1,−13), (3,1,−13), (±4,1,−12)… each have three air blocks.
  - **Behaviour:** starter ants spawn south of the mound on solid ground with at least 3 blocks of headroom.
- **A6 `rivalColoniesUseStageFourCultures`** (`GT:513-526`)
  - Setup: `createColony(..., false)`.
  - Asserts: culture is not AMBER; resin, fungus and venom are all > 0.
  - **Behaviour:** rival colonies get a non-Amber culture and start with advanced resources.
- **A7 `colonyRenovatePreservesEconomyAndPlacesCampus`** (`GT:1047-1070`)
  - Setup: resin 77, CHITIN_CULTIVATION research done, then `renovateColony`.
  - Asserts: resin still 77; research kept; the five starter profiles are re-placed.
  - **Behaviour:** renovation rebuilds the campus structures without touching resources or research.
- **A8 `colonyLabelsAndImportantAntNamesAreVisible`** (`GT:1072-1094`)
  - Asserts, within ±48:
    - Some `TextDisplay` has a custom name.
    - One of those contains the colony name.
    - An ant's custom name contains "Queen".
  - **Behaviour:** buildings get floating labels, the queen-mound label shows the colony name, and the queen is visibly named.
- **A9 `antSpawnPositionUsesRequestedGroundY`** (`GT:1150-1165`)
  - Setup: `ColonyService.spawnAnt` at +(8,1,0).
  - Asserts: `ant.getY()` equals the requested Y within 0.01.
  - **Behaviour:** ants spawn at exactly the requested Y, with no +1 offset.

### B. Building blueprints placed in the world (13)

All of these call `StructurePlacer.placeBuilding(level, pos, type, COMPLETE, culture)` directly, with no colony data, except where noted. Most also assert that particular QA scene ids exist.

- **B1 `starterQueenChamberUsesTallTieredBlueprint`** (`GT:96-110`)
  - Setup: `createColony`.
  - Asserts: the tiered profile; scenes `mound_interior`, `mound_storage_interior` and `mound_lookout_interior` exist.
  - **Behaviour:** the starter queen chamber is the 24-block, three-floor furnished mound.
- **B2 `greatMoundUpgradeAddsFurnishedAnnexesAndAConnectedCrownFloor`** (`GT:112-129`)
  - Setup: radius 48; place QUEEN_CHAMBER, then GREAT_MOUND at the same origin.
  - Asserts: the Great Mound profile; four great-mound scenes exist.
  - **Behaviour:** the Great Mound upgrades the queen chamber in place to 33 blocks. It adds a larder annex, a workshop annex and a crown map room, and keeps the original rooms furnished.
- **B3 `culturesShareTheOrganicQueenAndFoodBuildingLanguage`** (`GT:131-165`)
  - Setup: radius 150. For each of the 4 cultures at x = −84, −28, 28, 84: a queen, a food store at +28, and the signature building at +56.
  - Asserts:
    - The same queen and food profiles for every culture.
    - Shrine, fungus and watch profiles for the Amber, Leafcutter and Fire signature buildings.
    - For the Carpenter RESIN_DEPOT, only `assertMinimalBuildingMarker` (`GT:163`). **Stale and weak (inference):** a full resin-depot blueprint also passes this, because the space above the center block is a carved room.
  - **Behaviour:** all cultures share the queen and food-store blueprints, and each culture's signature building is placed.
- **B4 `starterColonyUsesOrganicEconomyAndBarracksBlueprints`** (`GT:181-205`)
  - Setup: `createColony`.
  - Asserts: the food, nursery, mine and barracks profiles; 10 variant/interior scenes exist (food, nursery, mine, chitin farm, barracks).
- **B5 `mineAndChitinFarmCompileToDistinctInhabitedMounds`** (`GT:207-222`)
  - Setup: radius 64; MINE at (−22,0,0), CHITIN_FARM at (22,0,0).
  - Asserts: the mine and chitin-farm profiles (pit versus pit-free).
- **B6 `marketCompilesToOpenInhabitedCourtyardVariants`** (`GT:224-243`)
  - Setup: two MARKETs at (−18,0,0) and (18,0,1).
  - Asserts: the market profile for both; `market_variants` and `market_courtyard` scenes exist.
  - Note: the test does not assert that the two markets are different variants. Variant distinctness is only checked in JUnit.
- **B7 `pheromoneArchiveCompilesToConnectedTwoFloorVariants`** (`GT:245-265`): two archives → archive profile; three archive scenes exist.
- **B8 `armoryCompilesToConnectedForgeAndWeaponVaultVariants`** (`GT:267-286`): two armories → armory profile; two scenes exist.
- **B9 `diplomacyShrineCompilesToOpenFurnishedSanctumVariants`** (`GT:288-307`): two shrines → shrine profile; two scenes exist.
- **B10 `resinDepotCompilesToConnectedWorkshopAndSealedVaultVariants`** (`GT:309-328`, Carpenter): two depots → resin profile; two scenes exist.
- **B11 `fungusGardenCompilesToEnclosedFurnishedCloverVariants`** (`GT:330-349`, Leafcutter): two gardens → fungus profile; two scenes exist.
- **B12 `venomPressCompilesToFurnishedMachineAndReagentVariants`** (`GT:351-370`, Fire): two presses → venom profile; two scenes exist.
- **B13 `watchPostCompilesToThreeFurnishedTwoStoreySentinels`** (`GT:372-394`, Fire, radius 80)
  - Setup: three posts at (−26,0,0), (0,0,1), (26,0,2).
  - Asserts: the watch profile for all three; three scenes exist.

### C. Building lifecycle: stages, construction, repair, upgrade (6)

- **C1 `buildingVisualStagesUseBlueprintsOnlyWhenOperational`** (`GT:396-422`)
  - Setup: a MARKET placed in each of six stages at six positions.
  - Asserts:
    - PLANNED → a DIRT_PATH single-block marker.
    - CONSTRUCTION, DAMAGED and REPAIRING → a MARKET_CHAMBER single-block marker with air above.
    - COMPLETE and UPGRADED → the full market profile.
  - **Behaviour, as encoded:** only operational stages render the blueprint, and an upgrade is visually identical to a complete building. This is a placeholder state (see §5.3).
- **C2 `workerConstructionDeliveryAdvancesActiveBuilding`** (`GT:1167-1183`)
  - Setup: a planned MARKET building is added.
  - Asserts: `depositConstructionWork(level, pos, colonyId, WORKER, 9)` returns true and `constructionProgress > 0`.
  - **Behaviour:** a worker delivery to an active construction site advances it.
- **C3 `damagedBuildingConsumesChitinAndRestoresMarketBlueprint`** (`GT:1185-1217`)
  - Setup: build queue cleared, chitin 40, a complete MARKET with `disableFor(120)`, so it starts DAMAGED.
  - Asserts:
    - After one `ColonyBuilder.tick`: REPAIRING, shown as a single-block marker.
    - After at most 4 more ticks: disabledTicks 0 and COMPLETE, with the full market profile restored.
  - **Behaviour:** with chitin in stock, a damaged building enters repair on the next builder tick and is fully restored and re-placed within 5 ticks.
- **C4 `repairContractDeliveryStartsDamagedBuildingRepair`** (`GT:1219-1258`)
  - Setup: chitin 0 and a damaged market.
  - Asserts:
    - After a tick it is still DAMAGED, and a contract with reason "repair market" and resource CHITIN exists.
    - `fulfillContract(id, missing)` succeeds and completes.
    - The next tick moves it to REPAIRING; chitin is back to 0 and the request list is empty.
  - **Behaviour:** missing repair material opens a chitin contract. A full delivery starts the repair, which consumes the delivered chitin and clears the request.
- **C5 `constructionContractDeliveryStartsMinimalQueuedSite`** (`GT:1260-1300`)
  - Setup: queue = [MARKET]; food and chitin exactly at cost; ore 0.
  - Asserts:
    - After a tick, a contract "construction market" for ORE exists.
    - Fulfilling it stocks at least the ore cost.
    - The next tick starts MARKET in the PLANNED stage, removes it from the queue, and empties the requests.
    - A DIRT_PATH marker is at the site.
  - **Behaviour:** missing construction material opens a construction contract. Delivery stocks the colony, and the next builder tick starts the site and clears the request.
- **C6 `idleMatureColonyStartsAndCompletesMarketUpgrade`** (`GT:1338-1369`)
  - Setup: all resources 500 and a complete MARKET.
  - Asserts:
    - After one tick: level 2, progress 0, stage CONSTRUCTION, and the market profile is still in place.
    - After at most 5 more ticks: UPGRADED, still with the profile.
  - **Behaviour:** an idle, rich colony auto-upgrades a building to level 2 in about 6 builder ticks, and the building stays intact during the upgrade.
  - Note: this contrasts with C1, where the CONSTRUCTION stage renders as a marker. The difference is that the placer is not re-invoked when an upgrade starts.

### D. Endgame projects (5)

- **D1 `citadelColonyCompletesMinimalGreatMoundProject`** (`GT:1772-1816`)
  - Setup: radius 72; all resources 800; reputation +100; +12 workers and +5 soldiers; completed and placed CHITIN_FARM, MARKET, PHEROMONE_ARCHIVE, ARMORY, DIPLOMACY_SHRINE.
  - Asserts:
    - After one tick a GREAT_MOUND building exists, and within 4 more ticks it is complete.
    - The Great Mound profile at the origin.
    - An event contains "great_mound".
    - Three endgame/great-mound scenes exist.
  - **Behaviour:** a citadel-level colony with the prerequisites auto-starts the Great Mound, finishes it within 5 builder ticks, and logs it.
- **D2 `citadelColonyCompletesMinimalQueenVaultAfterGreatMound`** (`GT:1818-1875`)
  - Setup: origin (2,20,2), radius 76, resources 900, plus GREAT_MOUND as a prerequisite. GRANITE is placed at 15 points along the vault route to exercise "live excavation through granite" (`GT:1838-1848`).
  - Asserts: QUEEN_VAULT starts and completes within 5 ticks; four vault scenes exist; the Great Mound and Queen Vault profiles; an event contains "queen_vault".
  - **Behaviour:** after the Great Mound, the Queen Vault auto-starts and completes, carving its descent and three rooms even through stone.
- **D3 `citadelColonyCompletesMinimalTradeHubAfterQueenVault`** (`GT:1877-1923`)
  - Setup: origin (2,20,2), radius 108, resources 1000, plus QUEEN_VAULT.
  - Asserts: TRADE_HUB starts and completes within 5 ticks; the hub profile at `siteFor(origin,TRADE_HUB,0)`; four hub scenes exist; an event contains "trade_hub".
  - **Behaviour:** the Trade Hub is the third endgame project, built after the vault.
- **D4 `completedTradeHubImprovesTradeTerms`** (`GT:1925-1950`)
  - Setup: a bare `new ColonyData(99, pos)`, before and after adding a complete TRADE_HUB and +20 reputation.
  - Asserts:
    - `sell_wheat` output is the starter value + 1.
    - `buy_colony_seal` input is 14 and lower than the starter value (the message says 16).
    - The availability text for both offers contains "Trade Hub".
  - **Behaviour:** a Trade Hub adds 1 pheromone token to sell offers, discounts token purchases from 16 to 14, and says so in the row text.
  - Note: the reputation increase is added at the same time, so the test does not isolate the hub's effect.
- **D5 `queenVaultAbsorbsRaidQueenDamage`** (`GT:569-605`)
  - Setup: an allied colony (no soldiers, majors or giants; a complete, placed QUEEN_VAULT; raid cooldown 600) and a rival 92 blocks away; both RIVAL toward each other; rival cooldown 0 with +20 soldiers.
  - Asserts: `RaidPlanner.tick` returns true; queen health is unchanged; `currentTask` and an event both contain "Queen Vault absorbed".
  - **Behaviour:** a completed Queen Vault fully absorbs raid damage to the queen and reports it.

### E. Ant work-target assignment (3)

These test only the target calculation, not movement.

- **E1 `workerAssignmentUsesDistantCampusTargets`** (`GT:1096-1110`): `debugWorkTarget(WORKER)` returns a target more than 18 blocks from the colony origin.
- **E2 `minerAssignmentTargetsOreNodeBeyondOldRadius`** (`GT:1112-1127`): the MINER target is within 3 blocks of the ore node at +(8,0,54).
- **E3 `soldierAssignmentUsesCampusPatrolPoint`** (`GT:1129-1148`): the SOLDIER target is more than 18 blocks out, on a solid block with 2 air blocks above.
- **Behaviour:** work targets are campus-aware. They reach past the old 18-block scan radius, miners pick the ore node, and soldiers patrol outdoor points with headroom.

### F. Research, tablet controls, trade UI (3)

- **F1 `archiveCanCompleteFirstResearch`** (`GT:1025-1045`)
  - Setup: a complete archive recorded in data only; knowledge, resin and ore at 40.
  - Asserts: `ColonyLogistics.startResearch(RESIN_MASONRY)` starts, and after 6 `ColonyLogistics.tick` calls the research is complete.
- **F2 `tabletControlsChangeColonyState`** (`GT:2066-2144`)
  - Setup: a mock server player standing at the colony; a complete archive; knowledge 60, resin 40, ore 40.
  - Asserts:
    - `ColonyService.startResearch(player, "resin_masonry")` returns true and records the active node.
    - Knowledge decreases, and the task contains "researching".
    - After opening a famine request (24 food) and giving the player 64 wheat, `ColonyService.completeContract(player, id)` returns true.
    - The request is closed, reputation rises, and the player's pheromone tokens increase.
    - `ColonyUiSnapshot` shows the active research and no longer shows the famine request.
  - **Behaviour:** the server entry points behind the tablet's Research and Needs buttons change real state and reward the player. These entry points are the same ones the network handlers call (`src/main/java/com/formicfrontier/registry/ModNetworking.java:25,37`). The client→server payload itself is not tested.
- **F3 `tabletTradeSceneShowsTradeHubTerms`** (`GT:1952-1979`)
  - Setup: `ColonyData(99)` with +20 reputation and a TRADE_HUB.
  - Asserts:
    - The snapshot's initial tab is "Trade".
    - `sell_wheat` has output 2 and status contains "Trade Hub".
    - `buy_colony_seal` has input 14, is available, and has "Trade Hub" in its status.
    - After adding the event "Recurring event: trade caravan exchanged 8 food for 8 resin with colony #7", `tradeActivity` contains "8 Food -> 8 Resin with #7".
  - **Behaviour:** the Trade tab shows hub terms and summarises the last caravan payoff, parsed from the event log.

### G. Recurring events (8)

Common pattern: advance colony age by `ColonyRecurringEvents.EVENT_INTERVAL_TICKS`. Then `ColonyRecurringEvents.tick(level, colony)` returns true, the event name appears in `currentTask()` and in the event log, and **a second tick returns false** (no repeat while its request is open, or before the next interval).

- **G1 `alliedMarketTradeCaravanRunsOutsideTributeDiplomacy`** (`GT:653-725`)
  - Setup: the allied colony and a wild Carpenter colony 64 blocks east, both with a complete MARKET, both ALLY. Allied food 160 and chitin 0; partner resin 40.
  - Asserts:
    - Resources move: allied food −8 and resin +8; partner food +8 and resin −8.
    - The event log on both sides contains "trade caravan".
    - The trade snapshot shows "8 Food -> 8 Resin" and "#partnerId".
    - At `tradeCaravanCamp(...)`: a BARREL at +1..+2, HAY (−1,1,0), HONEY_BLOCK (1,1,0), OCHRE_FROGLIGHT (0,1,1). DIRT_PATH at +(8,0,0).
    - Within ±5 of the camp there is an allied worker CARRYING_FOOD and a partner worker CARRYING_RESIN.
  - **Behaviour:** allied market colonies periodically swap 8 food for 8 resin, independent of tribute. The swap is visible as a camp with two carriers and is logged on both sides.
- **G2 `queenBroodRecurringEventLeavesNurseryProof`** (`GT:1371-1409`)
  - Setup: food 260, chitin 80.
  - Asserts:
    - "queen brood bloom" appears; workers +2; food and chitin both decrease.
    - The nursery profile is intact, plus CHITIN_NODE (0,1,−2), HONEYCOMB (−1,1,−1), OCHRE_FROGLIGHT (0,2,−1) at the nursery.
  - **Behaviour:** a well-fed colony periodically hatches 2 workers, paying food and chitin, with markers at the nursery.
- **G3 `famineRecurringEventMarksFoodStoreAndOpensContract`** (`GT:1411-1450`)
  - Setup: food 4.
  - Asserts:
    - "famine warning" appears.
    - A request (FOOD_STORE, FOOD, `FAMINE_REASON`) is opened.
    - Food is still 4.
    - Markers at the food store: FOOD_NODE (0,1,−2), HAY (1,1,−1), RED_MUSHROOM_BLOCK (−2,1,0), RED_TERRACOTTA (0,2,−1).
  - **Behaviour:** a starving colony warns, opens a player food contract without eating its last food, and does not repeat while the contract is open.
- **G4 `migrationRecurringEventMarksDaughterNestTrail`** (`GT:1452-1507`)
  - Setup: MARKET complete; food 240, chitin 90, +26 workers.
  - Asserts:
    - "migration preparation" appears.
    - A request (MARKET, FOOD, `MIGRATION_REASON`) is opened.
    - Trail markers: DIRT_PATH (−10,0,−10), HAY (−14,1,−14).
    - The camp at +(−34,0,−30) has HAY, OCHRE_FROGLIGHT, ROOTED_DIRT and PACKED_MUD.
    - Exactly 3 colony SCOUTs are PATROLLING at the camp.
  - **Behaviour:** an overcrowded, well-fed colony prepares to migrate: a food contract, a trail to a daughter-nest camp, and three scouts.
- **G5 `invasionWarningRecurringEventMarksApproachAndOpensDefenseContract`** (`GT:1509-1564`)
  - Setup: radius 112; a rival 64 blocks east, both RIVAL; rival cooldown 120 with +2 soldiers.
  - Asserts:
    - "invasion warning" appears.
    - A request (BARRACKS, CHITIN, `INVASION_WARNING_REASON`) is opened.
    - The rally point at +(36,0,0) has red terracotta, blackstone and candle columns, BONE to the south, CHITIN_NODE, PODZOL and RED_TERRACOTTA.
    - Exactly 3 allied SOLDIERs are PATROLLING there.
  - **Behaviour:** an upcoming rival raid window triggers a warning, a barracks chitin contract, a marked approach, and three guards.
- **G6 `treatyOpportunityRecurringEventMarksEnvoyRouteAndOpensResinContract`** (`GT:1566-1631`)
  - Setup: the allied colony has a DIPLOMACY_SHRINE; a wild Leafcutter colony is NEUTRAL 64 blocks away; food 260, chitin 4.
  - Asserts:
    - "treaty opportunity" appears in both colonies' logs.
    - A request (DIPLOMACY_SHRINE, RESIN, `TREATY_OPPORTUNITY_REASON`) is opened, with contract priority ≥ 4.
    - The camp has honeycomb, amethyst and candle columns plus MOSS; DIRT_PATH at +(8,0,0).
    - Exactly 2 carriers: an allied SCOUT CARRYING_RESIN and a neutral WORKER CARRYING_FUNGUS.
  - **Behaviour:** a shrine colony next to a neutral neighbour gets a prioritised treaty opportunity, shown as an envoy camp with one envoy from each side.
- **G7 `expansionOpportunityRecurringEventMarksClaimEdgeOutpostAndOpensOreContract`** (`GT:1633-1700`)
  - Setup: MARKET and WATCH_POST complete; food 160, chitin 4, ore 0.
  - Asserts:
    - "expansion opportunity" appears.
    - A request (WATCH_POST, ORE, `EXPANSION_OPPORTUNITY_REASON`) is opened, priority ≥ 4.
    - The outpost at `expansionOutpost(origin, HIVE.claimRadius())` has a WATCH_POST, a deepslate wall and an ochre froglight column; a trail at +(8,0,8); IRON_ORE, CHITIN_NODE, OAK_FENCE.
    - Exactly 3 crew ants (carrying ore or chitin, or patrolling).
  - **Behaviour:** a mature, watched colony offers a claim-edge outpost contract, staged with a marker and a crew.
- **G8 `completedExpansionOutpostContractSecuresClaimEdgeWatchPost`** (`GT:1702-1770`)
  - Setup: same as G7, then fulfil the contract and call `completeExpansionOutpost`.
  - Asserts:
    - A WATCH_POST single-block marker plus HONEYCOMB and OCHRE_FROGLIGHT.
    - A complete WATCH_POST building is registered at the outpost.
    - Claim radius is larger than before and ≥ 44.
    - Task contains "Expansion outpost secured"; an event contains "Expansion complete".
    - Requests are empty, and no re-offer happens after another interval.
  - **Behaviour:** delivering the ore secures the outpost as a completed watch post, grows the claim, and prevents the same expansion being offered again.

### H. Diplomacy consequences and raids (5)

- **H1 `rivalRaidLeavesVisibleTrailAndDamagedTarget`** (`GT:528-567`)
  - Setup: radius 104; colonies at x = −46 (allied, undefended) and +46 (rival), both RIVAL; rival cooldown 0 with +2 soldiers.
  - Asserts:
    - `RaidPlanner.tick` returns true; the `diplomacy_scene` scene exists.
    - Along x from −24 to 24 there is some DIRT_PATH or COARSE_DIRT, with a RED_TERRACOTTA or BLACKSTONE marker above.
    - Some allied building is `damaged`.
  - **Behaviour:** a raid leaves a marked trail and damages an allied building. The message says "non-queen", but that is not asserted.
- **H2 `tributeDiplomacyPlacesVisiblePactMarkers`** (`GT:607-651`)
  - Setup: a wild Carpenter colony 64 blocks east; ALLY set by the test.
  - Asserts:
    - `DiplomacyConsequences.apply(TRIBUTE, ALLY)` returns true.
    - The relation re-check only verifies what the test itself set (tautological).
    - Beacons at +12 and +52 (honeycomb and candle columns).
    - The cache at `pactCacheSite` (honeycomb, amethyst, candle).
    - The caravan at (32,0,−18) (barrel, hay, honeycomb), with an allied worker CARRYING_RESIN and a treaty worker CARRYING_FUNGUS.
- **H3 `truceDiplomacyCoolsRaidRouteWithVisibleMarkers`** (`GT:727-760`)
  - Setup: NEUTRAL both ways; cooldowns 0; rival +4 soldiers.
  - Asserts:
    - `apply(TRUCE, NEUTRAL)` returns true.
    - Both raid cooldowns are ≥ `TRUCE_COOLDOWN_TICKS`.
    - CHISELED_TUFF seals at +12 and +52; a moss and candle cache.
    - `RaidPlanner.tick` returns false.
  - **Behaviour:** a truce places seals and a cache and cools down raids on both sides.
- **H4 `warPactDiplomacyPlacesVisibleMusterRoute`** (`GT:762-799`)
  - Setup: WAR both ways; cooldowns 600.
  - Asserts:
    - `apply(WAR_PACT, WAR)` returns true, and both cooldowns become 0.
    - Musters at (12,0,−14) and (52,0,−14) with red terracotta and candle columns; a war line at (32,0,−14) with bone and blackstone.
    - At least 4 allied PATROLLING ants at the source muster.
  - **Behaviour:** a war pact opens an immediate raid window and musters troops.
- **H5 `alliedDefensivePactSendsVisibleGuardResponse`** (`GT:801-842`)
  - Setup: allied at −46, rival at +46, and a wild Carpenter ally at (0,0,36); the rival raids.
  - Asserts:
    - The rally point at (−28,0,8) has polished deepslate, honeycomb and candle columns, with BONE to the north and south.
    - At least 3 treaty PATROLLING ants there.
    - Both allied tasks contain "Defensive pact".
  - **Behaviour:** when a rival raids, the victim's ally sends guards to a marked rally point.

### I. World discovery (1)

- **I1 `survivalDiscoveryFindsStableWildEncounterSite`** (`GT:844-924`)
  - Setup: radius 150; seed `0x46F06D1C`.
  - Asserts, about discovery:
    - `findEncounterSite` called twice gives the same, non-empty result, at least `DISCOVERY_DISTANCE_MIN` from the player.
    - `spawnEncounter` creates a colony with faction "wild" that is not player-allied, with a NEST_MOUND at its origin.
    - A trail at +(−12,0,−42); a FOOD_NODE at (0,0,−58); the culture marker at (54,1,−16).
    - A collapsed scout nest at (38,0,22): NEST_MOUND over NEST_CORE, CHITIN_NODE at (−2,1,0), BONE at (2,1,0), with a trail at (24,0,10) and an event "Collapsed scout nest".
    - An approach trail head exists on the player's side, with air above it, ROOTED_DIRT on the left and the culture dressing on the right.
  - Asserts, about the QA camera, mixed into the same test:
    - The `worldgen_encounter` scene exists.
    - |camera.x − origin.x| ≤ 22; camera.y ≤ origin + 19; camera.z within origin + 36..44.
    - Target z within origin − 24..−12; the camp sits between origin + 18 and camera.z − 10.
  - **Behaviour:** survival discovery deterministically picks the same nearby but non-adjacent flat site for a given seed and region. It spawns a wild colony with a visible mound, trails, a forage patch, a culture boundary marker, a logged collapsed-scout-nest landmark, and a grounded approach trail from the player's side.

### J. Visual-QA harness invariants (6)

These test QA code and camera math, not gameplay.

- **J1 `constructionStageSceneShowsMaterialDelivery`** (`GT:424-479`)
  - Setup: `VisualQaScenes.seedConstructionStages`.
  - Asserts:
    - The planned site is a path marker; construction, damaged and repairing sites are chamber markers.
    - The complete and upgraded sites are open courtyards.
    - Resin-delivery blocks: MANGROVE_ROOTS, two RESIN_DEPOT blocks, BARREL, BONE.
    - Staged ants are WORKING, CARRYING_RESIN, PATROLLING and CARRYING_CHITIN, with at least 2 `Display` markers.
- **J2 `colonyOverviewSceneKeepsPreparedGroundBeyondCamera`** (`GT:926-963`)
  - Pure geometry. The overview radius (128) minus the camera's ground distance (72) is ≥ 40. The target is at y + 10.5..12.5. Every starter landmark and resource node leaves ≥ 18 blocks of prepared ground beyond it.
- **J3 `settlementScaleSceneAndLayoutUseLargeVillageFootprint`** (`GT:965-1023`)
  - Scene ids exist, plus a real **site-spacing contract** via `ColonyBuilder.siteFor`:
    - Food store and nursery are ≥ 36 from the origin on X.
    - Repeated food stores, nurseries and mines are ≥ 28 apart; the three chitin farms are pairwise ≥ 28; barracks ≥ 32.
    - Market ≥ 34 from the origin (Chebyshev distance).
    - Armory ≥ 30 from the barracks, and armories ≥ 34 from each other.
    - Watch post ≥ 50 from the origin.
    - The `settlement_scale` scene radius is ≥ 120.
- **J4 `repairSceneKeepsGameplayCuesAroundMinimalMarkers`** (`GT:1302-1336`)
  - Setup: `seedRepairScene`.
  - Asserts: the damaged and repairing sites are markers; the restored site has the market profile; four cue blocks are present; ants are WORKING and CARRYING_CHITIN.
- **J5 `workCycleSceneAndWorkStatesAreAvailable`** (`GT:1981-2026`)
  - Asserts:
    - Four job centers are clustered within |dx| ≤ 10 and |dz| ≤ 4 of +(0,0,−23).
    - After `seedWorkCycle` there are exactly 4 ants, WORKING, CARRYING_ORE, CARRYING_FOOD and PATROLLING.
    - There is no `TextDisplay` in the scene bounds.
    - Ten specific task-pad blocks are present.
- **J6 `antLineupKeepsSmallCastesNearCameraFocus`** (`GT:2028-2064`)
  - Asserts: worker, scout and miner are within |dx| ≤ 12 on row z = origin + 20; all castes are within |dx| ≤ 18; every pair is at least 5 blocks apart.

**QA-scene existence checks inside non-QA tests:** `GT:104-107`, `122-126`, `192-201`, `238-239`, `259-261`, `281-282`, `302-303`, `323-324`, `344-345`, `365-366`, `388-390`, `547`, `897`, `928`, `967-970`, `1305`, `1800-1802`, `1863-1866`, `1913-1916`, `1954`, `1983`, `2030`.

---

## 4. JUnit tests (56)

### 4.1 `ColonyEconomyTest`: 36 pure-simulation tests, no world

Base fixture `baseColony()` (`CET:1298-1308`): id 1, origin (0,64,0); food 120, ore 20, chitin 24; castes queen 1, worker 3, miner 2, soldier 2. Culture Amber, rank OUTPOST.

**Economy, upkeep and growth**
- `economyConsumesUpkeepAndProducesResources` (`CET:13-24`): after one `ColonyEconomy.tick`, food < before + 20, ore increased, and age ≥ `ECONOMY_TICK_INTERVAL`.
- `resourceTickProducesAndConsumesOverMultipleTicksAssertingRiseAndFall` (`CET:26-81`): with the queen dead (no growth), 6 ticks give exactly ore +4 per tick and food −12 per tick.
  - Formula from the comments: ore = miners·2 + mines + rankBonus/2; food income = 4 + workers·3 + bonuses; upkeep = queen 12, worker 1, miner 2, soldier 3.
- `giantRequiresLargeEconomy` (`CET:83-98`): a poor colony (food 10, ore 0, chitin 0) cannot grow a giant; a rich one (500/100/100) can.
- `queenDeathSuspendsGrowth` (`CET:112-125`): with queen health 0, population is unchanged and the task contains "Queen lost".
- `defensePriorityRaisesSoldierBeforeBalancedGrowth` (`CET:295-307`): with priorities [DEFENSE, …] and a rich colony, one tick adds +1 soldier.
- `smallCastesKeepReadableRenderScaleWithoutChangingGameplaySize` (`CET:100-110`): hitbox heights are worker 1.5, scout 1.55, miner 1.7. Visual scales are ordered worker < scout < miner < soldier, and the worker is scaled up beyond its hitbox.

**Persistence**
- `colonyDataRoundTripsThroughCodec` (`CET:127-162`): a JSON codec round trip preserves id, resources, castes, chambers, completed, damaged, repairing and upgraded buildings, culture, name, personality, relationship "friendly", task, requests, research and events. Status text contains "Personality:" and "Relationship: friendly".

**Building lifecycle, data only**
- `buildingVisualStageDerivesFromLifecycleState` (`CET:164-172`), with `(level, progress, disabledTicks)` → stage:
  - planned → PLANNED
  - (1, 50, 0) → CONSTRUCTION
  - complete → COMPLETE
  - (2, 100, 0) → UPGRADED
  - (1, 100, 20) → DAMAGED
  - (1, 50, 20) → REPAIRING
- `damagedBuildingCanEnterAndFinishRepair` (`CET:174-185`): `beginRepair(55)` → REPAIRING; `repair(80)` returns true, leaving 0 disabled ticks, progress 100, COMPLETE.

**UI snapshot (tablet data model)**
- `colonyUiSnapshotExposesStructuredStateWithoutStatusParsing` (`CET:187-215`): the snapshot carries id, tab, feedback, personality and relationship lang keys and colour, every resource and caste, an overview with `top_need`, buildings, requests, research (complete), guide entries for every chapter (FIRST_STEPS unlocked), and a task string without "===".
- `recurringEventsAppearInOverviewWithoutShowingFoundingNoise` (`CET:217-229`): the "Colony founded" event does not appear in the overview; "Recurring event: queen brood bloom…" does.
- `contractRowsExposeDeliveryCostAndUrgentOrder` (`CET:231-252`): with two requests, the resin request (research) sorts first.
  - Resin: delivery item `item.formic_frontier.resin_glob`, count 2, amount 6.
  - Food: delivery item wheat, count 8, amount 12.
- `wildColoniesExposeDiscoverableRelationship` (`CET:254-264`): a wild Leafcutter colony has faction "wild", is not allied, relationship "wild", with the matching lang key and status text.
- `guideChaptersTeachBasicsAndUnlockAdvancedTopics` (`CET:266-293`):
  - At start, CASTES, RESOURCES, BUILDINGS, CULTURES and HELPING are unlocked; RELATIONS and RESEARCH are locked and show their locked keys.
  - Setting a relation unlocks RELATIONS; adding a complete archive unlocks RESEARCH.
- `endgameProjectsAppearAsNamedBuildingsInUiSnapshot` (`CET:508-526`): great_mound, queen_vault and trade_hub appear with their `formic_frontier.building.*` keys.

**Rank and diplomacy**
- `rankReflectsBuildingsReputationAndPopulation` (`CET:309-322`): starts OUTPOST; with MARKET, SHRINE and WATCH_POST, reputation 80, +15 workers and +8 soldiers it reaches at least HIVE.
- `diplomacyActionsMoveRelationsWithCostsAndRankGates` (`CET:324-333`):
  - ENVOY: WAR → RIVAL.
  - TRIBUTE: NEUTRAL → ALLY.
  - TRUCE: WAR or RIVAL → NEUTRAL.
  - WAR_PACT: ALLY → WAR.
  - TRUCE costs more tokens than ENVOY; WAR_PACT needs CITADEL rank.
- `politicsRelationsShiftFromActionsChangeTradeRate` (`CET:956-1009`):
  - TRIBUTE by a BURROW+ actor (made BURROW by one SHRINE) moves NEUTRAL → ALLY, and the trade rate rises.
  - INCITE moves ALLY → NEUTRAL, and the rate falls.
  - An OUTPOST actor is blocked from TRIBUTE and the relation stays unchanged.

**Logistics, contracts, research**
- `logisticsRequestsConsumeResourcesUntilFulfilled` (`CET:335-345`): with resin 6 and a request for 3, one tick leaves resin 3 and the request complete.
- `requestsExposePlayerContractsWithRewardsAndReputation` (`CET:347-370`): a contract has an id, resource cost 12, priority ≥ 3, reward tokens and reputation. Fulfilling 12 succeeds and completes; reputation is > 0; requests are empty; the task contains "Contract fulfilled".
- `famineRequestsDoNotDrainEmergencyFoodStores` (`CET:372-382`): with food 4 and a famine request, a tick leaves food at 4 and `fulfilled` at 0.
- `famineContractsRestoreFoodWhenPlayerHelps` (`CET:384-399`): the famine contract has priority ≥ 5; delivering 12 succeeds but does not complete; food is 16 and `fulfilled` is 12.
- `completedResearchContractStartsResearchWithDeliveredMaterials` (`CET:401-427`): SCENTED_LEDGER cannot start without resin, so a resin contract opens. Delivering the missing amount starts the research, with payoff message "Research started: Scented Ledger"; resin and knowledge drop to 0, requests empty, and the task shows "Researching".
- `researchRequiresArchiveResourcesAndCompletes` (`CET:429-447`): no archive means research does not start; with an archive and 40/40/40, it starts and completes after 6 ticks, leaving no active research.

**Culture**
- `cultureModifiersAffectEconomyWithoutMoodState` (`CET:449-467`): Leafcutter produces more fungus than Amber, and status text never contains "mood".
- `cultureSignatureBuildingsChangeEconomyOutputs` (`CET:469-496`):
  - Amber with SHRINE and MARKET: knowledge ≥ 2.
  - Leafcutter with FUNGUS_GARDEN: more food and fungus than without.
  - Fire with ARMORY and WATCH_POST: venom ≥ 4.
  - Carpenter with RESIN_DEPOT and ARCHIVE: resin ≥ 8.
- `cultureStarterQueuesExposeDistinctProgressionPaths` (`CET:498-506`): the first queued building per culture is as in A4. Leafcutter's queue contains the ARCHIVE; Fire's contains the ARMORY.

**"Content rows"**

These were written for the content track. Each carries a comment "Content row <id> … proven by a gametest", but all nine are JUnit tests, not gametests.
- `casteJobLoopsChangeColonyStateEndToEnd` (`CET:529-591`, row `caste_job_loops_change_state`). One `CasteJobLoop.tick`:
  - Food +3 (workers), ore +4 (miners), chitin +2 (with 2 scouts added).
  - An incomplete MARKET gains construction progress.
  - A damaged WATCH_POST's disabled ticks fall below 8.
  - A wounded queen heals, capped at maximum.
  - The task starts with "Job loop:".
  - The comment claims this loop is wired into `ColonySavedState.tickEconomy()`. That is true in the code (`src/main/java/com/formicfrontier/world/ColonySavedState.java:104`), but the test does not assert it.
- `colonyAdvancesStageAutonomouslyAndUnlocksBuilding` (`CET:592-681`, row `progression_stage_advance_autonomous`):
  - Documented thresholds: FOUNDING→GROWTH at age ≥ 3, food ≥ 120, composite ≥ 40 (unlocks BARRACKS). GROWTH→ESTABLISHED at age ≥ 6, food ≥ 400, composite ≥ 180 (unlocks PHEROMONE_ARCHIVE). ESTABLISHED→MATURE at age ≥ 12, food ≥ 900, composite ≥ 500 (unlocks GREAT_MOUND).
  - Setup: food 800, ore 120, chitin 120, resin 60, then 7× (`ColonyEconomy.tick` + `ColonyStageProgression.tick`).
  - Asserts: at least ESTABLISHED; BARRACKS and ARCHIVE queued; an "advanced to the" event exists; the recorded stage is ≤ the earned stage.
  - With the queen dead the stage freezes: no advance and no regression.
- `researchUnlockEnablesEliteGiantCaste` (`CET:684-748`, row `research_unlocks_have_effects`):
  - Uses a funded DEFENSE colony. Without MANDIBLE_PLATING, 3 ticks grow no giant even though giant costs are met.
  - With it, a giant grows within 3 ticks.
  - The "research" step subtracts the costs by hand and calls `completeResearch`, **bypassing the real research path**.
- `tradeCaravanExchangesResourcesByRelationAndScarcity` (`CET:750-834`, row `trade_caravan_exchanges_resources`). `TradeCaravan.exchange` with a MARKET-owning source (food 200) and a target at food 0:
  - ALLY: runs at rate 1.0, cargo FOOD, conserved exactly.
  - NEUTRAL: rate 0.5, ships strictly less.
  - RIVAL: refused, nothing moves.
  - Source food 10 (below the surplus threshold of 40): no caravan.
- `nativeBlockFungusGardenCompostsFoodIntoFungus` (`CET:836-922`, row `block_native_gameplay_roles`). `NativeBlockRole.tick`:
  - Two gardens with food 200: both run; food −2·`FOOD_PER_GARDEN`, fungus +2·`FUNGUS_PER_GARDEN`.
  - Food at reserve + 6: throttled to 1 garden and never drops below `FOOD_RESERVE` (40; cost 5 per garden per the comments).
  - Food at the reserve: idle.
  - No garden: no-op.
- `soldierWeaponLoadoutChangesCombatStats` (`CET:1011-1080`, row `weapon_soldier_combat_stats`):
  - Unarmed: 0 armories, 0 attack, 0 defense, weapon "bare_mandibles".
  - ARMORY + MANDIBLE_PLATING: weapon "mandible_saber", attack = armed soldiers × `MANDIBLE_SABER_ATTACK`.
  - **Tautological part:** "military strength" is recomputed by hand in the test (soldier 4, major 8, giant 16), not via `RaidPlanner`.
- `venomSpearAndArmorAdvanceCombatLoadout` (`CET:1082-1125`):
  - VENOM_DRILLS alone gives "venom_spear". Adding MANDIBLE_PLATING switches to the saber, with higher attack.
  - A CHITIN_FARM adds armor defense; RESIN_MASONRY upgrades to resin-chitin, with more defense.
- `raidOutcomeChangesColonyStateWithCasualtiesAndRelationShift` (`CET:1126-1204`, row `defense_raid_outcome_changes_state`):
  - `RaidPlanner.resolveCombat` + `applyRaidOutcome`, with 8 attacking soldiers against 2 defending.
  - Loot is taken from the richest resource (food) and moves exactly. The defender takes casualties ≥ the attacker's. The relation RIVAL → WAR is recorded.
  - A strong defender (18 soldiers + 2 watch posts: defense 36 vs attack 32) loses fewer and does not escalate.
- `castePopulationAutoBalancesTowardColonyNeeds` (`CET:1205-1297`, row `caste_population_auto_balances`):
  - No soldiers and 8 workers: workers become soldiers, with population conserved and "reassigned" in the task.
  - Food 0 with surplus miners: miners become workers.
  - A healthy colony is a no-op; a queenless colony is a no-op.

### 4.2 Blueprint-data tests: 20 tests, no world

These check the declarative blueprints before they are compiled to blocks. They share helpers:
- 6-neighbour BFS connectivity over solid cells (`TMT:140-157`, `SVT:104-122`, `OBT:818-835`).
- `assertRearShell` (`OBT:749-756`).
- `assertEnclosedExceptMouths` (`OBT:758-793`).
- Chamber overlap at head height (`TMT:109-120`, `SVT:77-88`, `OBT:737-747`).
- Footprint = the set of y = 0 cells.

Caveat: `solidCells` iterates from y = 0 upward (`OBT:804-816`), so the mine pit's cells below y 0 are not included in the connectivity check.

- **`TMT` `queenStageOneIsCompactTallConnectedAndMultiLevel`** (`TMT:11-44`):
  - Height 24; width ≤ 21; depth ≤ 18; height > width.
  - 4 tiers, 2 terraces, 3 chambers with purposes {queen_hall, storage, lookout}, 2 stair connections, and 3 mouths at y {1, 8, 14}.
  - Terraces solid at (−2,7,−6) and (2,13,−5); rooms carved at (0,2,2), (−2,9,1), (2,15,1).
  - Exactly one connected component.
- **`TMT` `queenStageTwoGrowsUpwardAndAddsConnectedAsymmetricAnnexes`** (`TMT:46-103`):
  - Name `queen_mound_stage_2`, palette `great_mound`.
  - Height 33 > width. Solid volume > 1.65× stage 1; base footprint > 1.35× stage 1.
  - 7 tiers, 3 terraces, 6 chambers with purposes {queen_hall, great_larder, great_workshop, storage, lookout, great_crown}, 3 connections, 6 mouths.
  - Both annexes open into the queen hall at head height.
  - Solid at (−11,1,4) and (10,1,5); carved at (−7,2,4), (6,2,5), (−1,22,1).
  - Solid at the peak (0,32,1), with nothing at (0,33,1).
  - The last stair lands in `crown_chamber`, whose floor is at y 20. Exactly one connected component.
- **`SVT` `queenVaultIsAConnectedTwoDepthProtectedInterior`** (`SVT:12-62`):
  - Name and palette `queen_vault`; y from −12 to 0.
  - Chambers guard_vestibule (`vault_guard`, floor −6), royal_treasury (`vault_treasury`, −12) and brood_sanctum (`vault_sanctum`, −12).
  - Surface access drops 6 and lands at (2,3); the lower descent drops 6 and lands at (−2,3).
  - All rooms are carved. Treasury and sanctum overlap at head height.
  - Two blocks of headroom at every descent step. Nothing solid above the mound floor at (0,1,0). One connected component.
- **`OBT` `foodStoresAreLowSingleStoreyConnectedMoundsWithDistinctSilhouettes`** (`OBT:16-47`): variants `food_store_a`/`_b`; maxY ≤ 6; 1 chamber (`food_store`); no stairs; 1 mouth; carved (0,2,1); rear shell present; connected; footprint ≥ 130; the two footprints differ.
- **`OBT` `nurseriesAreWarmBulbousSingleStoreyMoundsWithDistinctSilhouettes`** (`OBT:49-80`): `nursery_a`/`_b`; maxY ≤ 7; carved (0,2,2); footprint ≥ 150; distinct.
- **`OBT` `positionSelectorIsStableAndUsesBothFoodStoreVariants`** (`OBT:82-93`) and **`…Nursery…`** (`OBT:95-106`): over 64 sites, the same site always gets the same variant, and both variants are used.
- **`OBT` `minesAreLowRoundedMoundsWithAValidatedSteppedPit`** (`OBT:108-134`): `mine_a`/`_b`; maxY ≤ 6; 1 pit of depth 2; minY −2; the chamber floor opens into the pit; footprint ≥ 130; distinct.
- **`OBT` `chitinFarmsReuseTheLowMoundLanguageWithoutAnyPit`** (`OBT:136-158`): `chitin_farm_a/b/c`; maxY ≤ 5; no pit; minY 0; floor (0,0,4) solid; footprint ≥ 150; 3 distinct footprints.
- **`OBT` `barracksAreWideElongatedFortifiedMounds`** (`OBT:160-190`): maxY ≤ 7; mouth width 5; carved (0,2,1); width ≥ 22 and ≥ depth + 5; footprint ≥ 200; distinct.
- **`OBT` `marketsAreLowRooflessCourtyardsWithDistinctPerimeters`** (`OBT:192-225`): maxY = 3; the one chamber is `openToSky` with topY = maxY; mouth width 5; solid floor; roofless above; footprint ≥ 180; top ring ≥ 24 cells; distinct.
- **`OBT` `tradeHubIsALargeRooflessThreeZoneCaravanComplexAtAnOpenSite`** (`OBT:227-277`):
  - 1 variant; maxY 7; 4 tiers.
  - Chambers: court (open to the sky), warehouse and brokerage (both enclosed). No connections; mouth width 5.
  - The court is roofless from y 1 to 7, and both side rooms overlap into it.
  - Footprint ≥ 390; width ≥ 27; depth ≥ 21.
  - The hub site is ≥ 42 from the MARKET, VENOM_PRESS and WATCH_POST sites.
- **`OBT` `pheromoneArchivesHaveTwoDistinctFloorsJoinedByAnInternalStair`** (`OBT:279-328`): maxY 10; hall at floor 0 and loft at floor 5; 2 mouths; one 1-wide stair with headroom; upper mass ≥ 45; footprint ≥ 170; stair directions {east, west}; distinct.
- **`OBT` `armoriesAreHeavyFortifiedMoundsWithConnectedForgeAndWeaponVault`** (`OBT:330-379`): maxY 8; forge and vault on the same floor, overlapping; mouth width 3; enclosed except mouths; footprint ≥ 220; width ≥ 19; depth ≥ 17; |w − d| ≤ 5.
- **`OBT` `diplomacyShrinesAreOpenThreeHornSanctumsWithDistinctSilhouettes`** (`OBT:381-418`): maxY 7; open-sky sanctum; mouth width 3; three horns at y ≥ 6 (x ≤ −5, x ≥ 5, z ≥ 6); crown mass ≥ 12; footprint ≥ 190.
- **`OBT` `resinDepotsAreLowTwinPodCisternsWithConnectedWorkshopAndVault`** (`OBT:420-470`): maxY 7; workshop and sealed vault overlapping; mouth width 3; enclosed; footprint ≥ 200; lobes at x ≤ −9 and x ≥ 9; cap mass (y ≥ 6) ≥ 18.
- **`OBT` `fungusGardensAreLowCloverMoundsWithEnclosedCultivationHalls`** (`OBT:472-505`): maxY 6; enclosed hall; mouth width 5; footprint ≥ 220; pods at x ≤ −10, x ≥ 10 and z ≥ 10; crown mass (y ≥ 5) ≥ 24.
- **`OBT` `venomPressesAreDarkJawedWorkshopsWithSealedReagentVaults`** (`OBT:507-558`): maxY 8; hall and vault overlapping; mouth width 3; enclosed; footprint ≥ 220; three lobes; crown mass (y ≥ 6) ≥ 34.
- **`OBT` `watchPostsAreTallTwoStoreySentinelsWithInternalStairs`** (`OBT:560-617`): variants a/b/c; maxY 13; guard room at floor 0 and lookout at floor 6; 2 mouths; 1 stair with headroom; footprint ≥ 150; width ≤ 22; depth ≤ 20; upper mass ≥ 45; crown mass (y ≥ 11) ≥ 20; stair directions {east, west, south}.
- **`OBT` `repeatedRoleSitesSelectEveryAuthoredVariant`** (`OBT:619-726`): for origin (11,0,−7), repeated `siteFor` calls produce every variant, with these minimum distances:
  - mine 2 variants, farm 3, barracks 2, market 2, archive 2;
  - armories ≥ 34 apart; shrines ≥ 42; resin depots ≥ 42; fungus gardens ≥ 46; venom presses ≥ 46;
  - 4 watch sites use all 3 variants, and every pair is ≥ 90 apart.

---

## 5. Test coverage assessment

### 5.1 What the automated tests actually guarantee

1. **Blueprint invariants for all 17 building blueprints plus the queen stage-1, stage-2 and vault blueprints** (JUnit, data level): heights, footprints, chamber counts and purposes, carved rooms, stair headroom, single connectivity, enclosure except mouths, distinct variants, deterministic per-site variant selection, and minimum site spacing.
2. **The world-placement contract for each COMPLETE blueprint** (gametests): the center block, open mouths and rooms, furniture at exact offsets, crown height, and a layer-mass taper. Transitional stages are single-block markers.
3. **The colony founding layout:** chamber sites, resource nodes about 54 blocks out, ground anchoring, safe ant spawns, labels and queen name, starter resources with no giant, culture starter queues, and rival cultures with advanced stock.
4. **Unit-level simulation:** exact economy deltas, upkeep, growth gates, codec round trip, UI snapshot structure, contracts and rewards, research gating, culture modifiers, stage progression thresholds, caste job loop, balancer, caravan rates, diplomacy transitions and rank gates, weapon and armor loadouts, and raid resolution.
5. **World-side consequences of events and diplomacy:** each event's trigger condition, task and log strings, which request it opens and at what priority, exact marker blocks, staged ants with fixed work states and exact counts, and no repeat while a request is open.
6. **The endgame chain:** Great Mound, then Queen Vault, then Trade Hub, each auto-starting and completing within 5 builder ticks when resources are prepared. The Trade Hub improves trade terms, and the Queen Vault absorbs queen damage.
7. **Tablet server entry points** change state, and the snapshot reflects it.
8. **QA harness invariants:** camera distances, prepared-ground margins, lineup spacing, and that the scene ids exist.
9. **Asset validity**, as part of `check` (see §8.13).

### 5.2 What is not tested

- **The live tick composition.** No test calls `ColonySavedState.tickEconomy()` or `tickWorld()` (`src/main/java/com/formicfrontier/world/ColonySavedState.java:96-139`, `180-192`). The combined effect of `ColonyEconomy`, `ColonyLogistics`, `CasteJobLoop`, `ColonyStageProgression`, `CasteBalancer`, `NativeBlockRole`, the envoy pass (`DiplomacyService.tick`, never tested) and the caravan pass is untested. Two separate trade systems run in live play: the recurring-event caravan (8 for 8, G1) and `TradeCaravan.exchange` (scarcity and relation rate). Possible double-counting (for example, food from both the economy and the job loop) is not examined.
- **Anything that takes game time:** entity AI, pathfinding, carrying, walking to targets, raid timing across ticks, discovery scheduling (`DISCOVERY_CHECK_INTERVAL_TICKS`, the max-colonies cap), and building construction driven by real ants rather than direct service calls.
- **Items and player interactions:** Queen Egg use, opening the tablet from its item, pheromone dust, hand-feeding, chitin spore planting and harvest, recipes (32 recipe JSONs are only checked for JSON validity), weapon and armor item stats, and `/formic trade` beyond the Trade Hub modifiers.
- **Commands,** including permissions: `/formic` has no permission check.
- **Networking payloads and all client code.** Client gametests are disabled. Screen layout and overlap are verified only by screenshot review.
- **World save and reload** (only the codec round trip). The manual checklist item 26 covers it (`docs/manual-playtest.md:31`).
- **Uneven terrain.** Every gametest first flattens a disc of up to radius 150, so placement on slopes, water or trees, and anchoring beyond the single "high request" case, are untested.
- **Localization fit** (only key parity and mojibake), **block texture sizes** (only item, ant and armor textures are size-checked), and **performance** (the cost of structure placement and ticks).
- **Negative and edge cases** are sparse: storage capacity, overflow, and multiple colonies of the same culture.

### 5.3 Quality problems and fragility in the tests

- **Coordinate coupling.** Exact offsets for furniture and markers turn every geometry change into test churn. R2 commit bodies record geometry constrained by unrelated tests. Example: a crown reach of Z ±7 "lands inside the crown … drops a crew ant (2/3)" in the expansion test (`4bd779b`); forest-floor dressing "breaking the famine and starter-side-chambers game tests" (`33c1659`).
- **Placeholder behaviour encoded as a requirement:** single-block markers for planned, construction, damaged and repairing stages; an upgrade identical to complete (C1, J1, J4, C3–C5). These came from the reset commit `6fb5bcd`, which renamed "Visible…" tests to "Minimal…" and deleted the earlier structure assertions (native-palette ratio, mega-mound taper, no long vertical wall runs, deep tunnel mouths).
- **Production tests coupled to the QA harness:** 57 scene-id checks spread over 22 tests (17 outside group J), plus 6 harness tests. A renamed QA scene breaks gameplay tests.
- **Tautologies and bypasses:** H2 re-checks relations the test set itself. `soldierWeaponLoadoutChangesCombatStats` recomputes military strength by hand. `researchUnlockEnablesEliteGiantCaste` bypasses the research flow. "Wired into tickEconomy" claims live in comments only.
- **Weak shell checks:** `isOrganicMoundShell` accepts GOLD, AMETHYST, SLIME, FROGLIGHT and COPPER, and differs from the `countMoundLayer` list. Mass thresholds were relaxed with comments about worker routes (`GT:2260-2262`). Carpenter's signature building only gets a marker check (`GT:163`).
- **String matching** on `currentTask` and event text: "Queen Vault absorbed", "trade caravan", "8 Food -> 8 Resin", "Job loop:".
- **Global shared state.** A single `ColonySavedState` is shared, `clearColonies()` resets ids, and tests write up to 150 blocks outside their bounds. Some counts (G6 envoys) filter only by work state.
- **Mislabelled evidence.** Content-matrix acceptance required "proven by a gametest", but 9 of 10 rows were proven by JUnit sim tests. `content_feature_gate.py` only checks that `evidenceTest` is non-empty (`tools/content_feature_gate.py:94-97`).
- **Inaccurate commit claims:** "All 50 gametests pass" when the file had 49 (`f5e9857`, `c3a7aeb`).

---

## 6. Visual QA harness

### 6.1 Architecture and end-to-end flow

1. **`scripts/gui-smoke.ps1`** is the launcher (details in §8.13).
   - Deletes `build/visual-qa` unless `-NoLaunch` or `-Scenes` is given (`:83-85`).
   - Runs `doctor`, then `prepare-gui-world` (`:89-96`).
   - Overwrites `run/options.txt` (`:20-45`): `lang:en_us`, `guiScale:0`, `renderDistance:12`, `simulationDistance:8`, `particles:0`, `pauseOnLostFocus:false`, clouds off.
   - Runs `gradlew runClient` with `-Dformic.visualQa=true`, `.dir=build/visual-qa`, `.exit=true`, `.world=FormicVisualQA`, `.scope=<full|world|structure>`, optional `.scenes=`, and `--quickPlaySingleplayer=FormicVisualQA --width 1600 --height 900` (`:99-116`).
   - Times out after 900 s (`:121-129`). A nonzero client exit code is tolerated (`:131-134`).
   - Copies `runClient.log` to `latest.log` and runs `tools/visual_qa_report.py` (`:135-143`).
2. **Client driver `VQC`** is registered only when `-Dformic.visualQa=true` (`VQC:113-118`; hooked from `src/client/java/com/formicfrontier/client/FormicFrontierClient.java:18`).
   - Scene selection: an explicit `formic.visualQa.scenes` list, or a scope list (`VQC:98-111`).
   - Every client tick (`VQC:120-173`):
     - Disables the tutorial.
     - Sets `hideGui` for non-tablet scenes.
     - Waits for the world. After 80 ticks with a screen open it opens `FormicVisualQA` itself; after 600 ticks it writes a `waiting_for_world` summary and quits (`VQC:260-285`).
     - For each scene: closes screens, switches language if needed, and sends the chat command `formic qa scene <name>`.
     - Waits `captureDelayTicks(scene)`, checks `readyForCapture`: for world scenes, no screen, GUI hidden, player is a spectator (`VQC:245-258`).
     - Calls `Screenshot.grab(build/visual-qa, "<scene>.png")`, which writes to `build/visual-qa/screenshots/` (`VQC:287-293`).
     - Moves on after `max(110, delay + 20)` ticks.
   - At the end it writes `visual-qa-summary.json` and `.md` with status `complete`, the capture timestamp, `worldLoaded`, and the screenshot list, restores `hideGui`, and stops the client (`VQC:295-326`).
   - Tunables: `formic.visualQa.worldWaitTicks` 600, `captureDelayTicks` 70, `sceneTicks` 110 (`VQC:71-73`).
3. **Server command** `/formic qa scene <name>` (`src/main/java/com/formicfrontier/command/FormicCommands.java:69-73`) calls `VisualQaScenes.run` (`VQS:277-586`). It has tab-completion over `scenes()` and **no permission requirement**.
4. **`tools/visual_qa_report.py`** turns the screenshots into `visual-qa-report.json`/`.md` (§6.4). The gate and the assessors consume that report.

**Language per scene:** `tablet_ru` and `tablet_requests` switch to `ru_ru`; everything else uses `en_us` (`VQC:175-181`). The switch is done by `setSelected`, `options.languageCode`, **`options.save()`** and `reloadResourcePacks()` (`VQC:183-209`), so the language persists in `run/options.txt`.

**Capture delays** (`VQC:219-243`):
- default 70 ticks;
- `colony_overview` 220 (first cold wide scene; a comment says a shorter delay captured a "half-rendered snowy canopy");
- `structure_preview_*` and the food / nursery / mine / chitin-farm / barracks / market / archive / armory / shrine variants: 120;
- `settlement_scale`, `diplomacy_scene`, `endgame_project`: 120;
- great-mound and queen-vault scenes: 240;
- trade-hub scenes: 180;
- `colony_ground`, `culture_styles`, `construction_stage`, `repair_scene`, `progression_scene`: 90.

**Visual QA mode also changes gameplay:** with `formic.visualQa` set, the discovery tick and recurring events are disabled globally (`src/main/java/com/formicfrontier/world/ColonyDiscoveryService.java:28`, `src/main/java/com/formicfrontier/world/ColonyRecurringEvents.java:53`).

### 6.2 Staging rules shared by all scenes (`VisualQaScenes.run`)

- **Pinned origin.** One process-wide `qaOrigin` is taken from the first command's X and Z, with ground Y = clamp(96, minY + 16, maxY − 97) (`VQS:335-342`). The comment explains why: deriving each scene from the previous spectator camera had "ratcheted the platform upward until tall mound crowns crossed Y=320". This was fixed in `88b040f`.
- **Flattening** (`prepareFlatQaArea`, `VQS:1880-1904`). Each scene removes ants, `Display` entities and item drops within its radius, then writes dirt, dirt, grass and air. On the first scene air is cleared up to the world ceiling; afterwards up to origin + 96.
  - Radius per scene (`qaRadius`, `VQS:608-629`): previews and `great_mound_growth` 112; overview and settlement 128; diplomacy 136; culture 124; worldgen 104; endgame and progression 112; everything else 58.
  - Only the current radius is reset. Content left by an earlier, wider scene outside that radius persists, so screenshots depend on scene order (an inference from the code).
- **Environment:** `setDayTime(6000)` and clear weather (`VQS:346-347`).
- **Destructive:** `savedState.clearColonies()` on every scene (`VQS:350`).
- **Camera:** the player is set to SPECTATOR, teleported to the camera position, and made to look at the target (`positionCamera`, `VQS:1385-1590`).
- **"PREVIEW = GAME."** Focused scenes use the same public placement path as live colonies (`VQS:352-354`). The **decoration and actors, however, are QA-only**:
  - `dressForestFloor` (`VQS:1601-1646`): a hash-deterministic mix within Manhattan radius 96, only on grass cells, of coarse dirt, podzol, rooted dirt, moss, mud, cobblestone and dirt path; tufts of grass, ferns, flowers, mushrooms, stone buttons and dead bushes; 8 root, stone and litter clusters; and `dressColonyCore` worker routes (3-wide paths) with trampled skirts. **Nothing in gameplay calls this.** It is applied to every non-interior, non-role, non-tablet scene.
  - `spawnWorkAnt` gives ants forced `AntWorkState`s and yaws (`VQS:1258-1266`). `placeItemMarker` and `placeBlockMarker` float billboard `ItemDisplay`/`BlockDisplay` entities above them (`VQS:1268-1307`). `placeJobAnchor` paints short paths to the job (`VQS:1233-1252`).
  - Crown validation (`validateMoundPreview`, `VQS:1353-1383`): only for `structure_preview_*` (peak (0,23,1)) and `great_mound_growth` (peaks (−18,23,1) and (18,32,1)). If the peak is air or the block above is not air, the scene logs `Invalid … crown` and sends a failure. The report script greps the client log for this (§6.4).
  - Interior scenes discard any ant within 2 blocks of the camera (`VQS:499-505`).

### 6.3 Scene catalogue: all 65 scene ids (`VQS:64-196`)

**World and tablet scenes** (the `full` scope, 23; `VQC:17-41`):

| Scene | What is staged | Camera → target (offsets from origin) | Radius | Delay |
|---|---|---|---|---|
| `colony_overview` | Allied colony + `seedVisualState`: every resource 240, +4 workers, +2 scouts, +3 miners, +3 soldiers, all 9 "advanced" buildings complete at their `siteFor` sites (`VQS:631-712`), task "QA campus review"; forest dressing | (56,34,−72) → centre +11 (`VQS:600-606`) | 128 | 220 |
| `colony_ground` | Colony + extra castes only (early return, starter layout); dressing | (16,8.5,−30) → centre +11 | 58 | 90 |
| `mound_interior` | Full colony (structure path falls through to `createColony`, `VQS:486-488`); no dressing | (0.5,3.4,−1.2) → (0.5,1.7+,3.5) | 58 | 70 |
| `mound_storage_interior` | same | (−1.5,10.3,−1.2) → (−1.5,9.2,3.0) | 58 | 70 |
| `mound_lookout_interior` | same | (2.5,14.7,−0.8) → (2.5,14.8,2.5) | 58 | 70 |
| `ant_lineup` | Colony; ants cleared in x ±45, z −15..60; foreground x ±34, z 21..62 flattened up to y48; 7 castes in a row at z +20, x from −15 to 15 in steps of 5 (queen, giant, major, worker, scout, miner, soldier), named by caste id, **NoAI**, persistent (`VQS:1309-1351`, `260-275`); dressing | (0,11,52) → (0,2,20) | 58 | 70 |
| `work_cycle` | Colony; clear x ±24, z −40..−12; a 45% MARKET construction at (−9,−25); ore task pad at (0,−23); WATCH_POST at (9,−25); logistics pad at (2,−19); 4 ants (WORKING, CARRYING_ORE, PATROLLING, CARRYING_FOOD) with markers (mangrove roots, raw iron, polished deepslate, wheat); text labels removed (`VQS:1152-1204`); dressing | (14,6.6,−38) → (0,1+1.35,−23) | 58 | 70 |
| `equipment_showcase` | No colony. Deepslate floor and wall backdrop, 2 shroomlights; armor stands "Chitin Guard" (chitin armor, mandible saber, chitin plate) and "Resin Chitin" (resin-chitin armor, venom spear, war banner) (`VQS:766-816`) | (0.5,3,−9) → centre +(0,2.4,0.5) | 58 | 70 |
| `tablet_en` | Colony + advanced buildings; requests: archive resin 18, market food 24; task "QA contracts awaiting player help"; screen tab "Needs" | (12,5,−18) → centre +3; GUI visible | 58 | 70 |
| `tablet_ru` | As overview seeding; tab "Instinct"; **ru_ru** | same | 58 | 70 |
| `tablet_guide` | Knowledge 0, task "QA guide: basics open, advanced notes locked"; no advanced buildings; tab "Guide" | same | 58 | 70 |
| `tablet_trade` | Trade Hub complete, reputation +20, caravan event "8 food for 8 resin with colony #7" (`VQS:753-764`); tab "Trade" | same | 58 | 70 |
| `tablet_research_map` | Knowledge 160, `startResearch(resin_masonry)`; tab "Research" | same | 58 | 70 |
| `tablet_market` | **Identical staging to `tablet_trade`** (same seed, same tab) | same | 58 | 70 |
| `tablet_requests` | 3 requests: archive resin 28, market food 32, nursery chitin 18; tab "Needs"; **ru_ru** | same | 58 | 70 |
| `progression_scene` | Advanced buildings; food 320, chitin 80, ore 8 → queen brood bloom; food 6 → famine warning; expansion opportunity triggered and its contract completed (`VQS:697-705`); dressing | (52,36,−66) → centre +11 | 112 | 90 |
| `settlement_scale` | Advanced buildings, task "QA settlement scale…", labels synced; dressing | same as overview | 128 | 120 |
| `construction_stage` | `seedConstructionStages`: clear x ±36, z −44..−8; 6 MARKETs at (−24,−32), (−8,−32), (8,−32), (24,−32), (−10,−16), (10,−16), one per stage (planned, construction 45%, complete, upgraded L2, damaged with 120 disabled ticks, repairing 65% with 80); resin-delivery and late-stage staged ants and blocks (`VQS:990-1044`); dressing | (18,13,−55) → (0,2.8,−25) | 58 | 90 |
| `repair_scene` | `seedRepairScene`: MARKETs at (−18,−26) damaged, (0,−26) repairing 62%, (18,−26) restored; cue blocks; repair worker, chitin carrier, inspector soldier; chitin 36 (`VQS:1069-1150`); dressing | (20,12,−55) → (0,2.6,−26) | 58 | 90 |
| `culture_styles` | No colony data. For 4 cultures at x −84, −28, 28, 84 (z −8): queen, food store at +18, first starter-queue building at +32, and a text label with the culture name (`VQS:818-848`); dressing | (0,44,−92) → (0,4,4) | 124 | 90 |
| `diplomacy_scene` | Allied at (−78,0,0), rival at (78,0,0), wild Carpenter treaty at (0,0,72), wild Leafcutter envoy at (0,0,−78). Allied gets a shrine and market, treaty a market. Relations set, then run in order: TRIBUTE, trade caravan, invasion warning, raid tick, TRUCE, WAR_PACT, treaty opportunity, plus narrative events (`VQS:850-914`); dressing | (0,42,−112) → centre +4 | 136 | 120 |
| `worldgen_encounter` | `spawnEncounterAt` wild Leafcutter at (0,0,−34); camp at (0,0,24) with campfire, log and barrel; L-shaped trail with markers every 7 steps and a grounded trail head (`VQS:916-982`); dressing | (18,18,38) → (0,3.2,−18) | 104 | 70 |
| `endgame_project` | Advanced buildings + `seedEndgameProject`: resources 520, reputation +100, castes, and Great Mound, Queen Vault and Trade Hub completed and placed (`VQS:724-751`); dressing | (54,36,−68) → centre +11 | 112 | 120 |

**Focused structure scenes** (the `structure` scope, 45; `VQC:48-69`). They do not use colony data (except the three mound interiors), use no dressing (except the previews), and use the stage COMPLETE.

| Scene(s) | Placement | Camera pattern | Delay |
|---|---|---|---|
| `structure_preview_front`, `structure_preview_3q` | QUEEN_CHAMBER (Amber); crown validated | front (0.5,13,−34), 3/4 view (20,17,−34) → (1,11,0) | 120 |
| `food_store_variants`, `nursery_variants`, `mine_variants` | 2 copies at (−16,0,0) and (16,0,1) | (0.5,11,−38) | 120 |
| `chitin_farm_variants` | 3 copies at (−26,0,0), (0,0,1), (26,0,2) | (0.5,12.5,−48) | 120 |
| `barracks_variants`, `archive_variants`, `armory_variants`, `shrine_variants` | 2 copies at ±18 | (0.5,13–16,−43) | 120 |
| `market_variants` | 2 copies at ±16 | (0.5,15,−37) | 120 |
| `resin_depot_variants` (Carpenter), `fungus_garden_variants` (Leafcutter), `venom_press_variants` (Fire) | 2 copies at ±18 | (0.5,13–14,−43) | 70 |
| `watch_post_variants` (Fire) | 3 copies at −26, 0, 26 | (0.5,20,−54) → +6.8 | 70 |
| `*_interior` (food, nursery, mine, chitin farm, barracks, archive hall and loft, armory, resin, fungus, venom, watch guard and lookout), `shrine_sanctum`, `market_courtyard` | one at the origin | eye level just outside the mouth, ~(0.5,1.1–1.4,−2.8…−3.2), looking inward. Exceptions: `mine_interior` looks down into the pit; `archive_loft_interior` at y 6.3; the watch lookout at y 7.3; `market_courtyard` is aerial from (9,11,−11) | 70 |
| `great_mound_growth` | QUEEN_CHAMBER at (−18,0,0) and GREAT_MOUND at (18,0,0); two crowns validated | (0.5,27,−58) → (2,15,1) | 240 |
| `great_mound_larder_interior`, `…_workshop_interior`, `…_crown_interior` | GREAT_MOUND at the origin; the camera is placed from the blueprint chamber coordinates | inside the room | 240 |
| `queen_vault_descent_interior`, `…_guard_…`, `…_lower_…`, `…_sanctum_…` | stone fill below (`prepareVaultQaSubsurface`, `VQS:1906-1915`), then GREAT_MOUND and QUEEN_VAULT | underground, from vault chamber coordinates | 240 |
| `trade_hub_exterior`, `_courtyard`, `_warehouse`, `_brokerage` | TRADE_HUB at the origin | exterior (14,9,−16); the rest inside each zone | 180 |

### 6.4 Screenshot validation (`tools/visual_qa_report.py`)

- Expected lists by scope: full 23, world 16 (full minus tablets), structure 45 (`tools/visual_qa_report.py:11-88`).
- **Failures** (`:127-162`):
  - a missing PNG;
  - a file smaller than **200,000 bytes** (world) or **70,000 bytes** (tablet) is treated as "likely blank". The tablet threshold was lowered from 100,000 in `88b040f`; the comment says valid tablet captures are 85–100 KiB;
  - an invalid PNG header;
  - resolution below 640×360;
  - any `runClient.log` line containing `(formic_frontier) Invalid ` and ` crown at ` (`:134-138`).
- Output: `visual-qa-report.json` with `{status: passed|failed, scope, screenshots: [{file, width, height, bytes}], errors}`, plus a Markdown copy.
- `--ci-manifest-only` writes a "manifest_only" report and always passes (`:110-125`). This is all CI does.
- **Not checked:** image content (variance, sky-only frames), whether the server command succeeded (except the crown check), and per-scene acknowledgement from the server.

### 6.5 Scene lists defined in about 10 places, inconsistently

| Where | Count | Location |
|---|---|---|
| Server scene registry | 65 | `VQS:130-196` |
| Client `full` / `world` / `structure` | 23 / 16 / 45 | `VQC:17-69` |
| Report `full` / `world` / `structure` | 23 / 16 / 45 | `tools/visual_qa_report.py:11-88` |
| Gate `EXPECTED_SCENES` | 19 (full minus `equipment_showcase` and the 3 mound interiors) | `tools/visual_assessment_gate.py:24-44` |
| OpenAI and GLM assessors | 19 | `scripts/openai-visual-assessment.ps1:95-115`, `tools/openai_visual_assessment.py:22-42`, `tools/glm5v_visual_assessment.py:24-44` |
| Skill "Expected Scenes" | 17 (no `tablet_trade`, `worldgen_encounter` or `endgame_project`) | `.codex/skills/formic-visual-assessment/SKILL.md:36-54` |
| Report template sections | 13 | `.codex/skills/formic-visual-assessment/references/report-template.md:44-107` |
| docs | 19 | `docs/autonomous-dev.md:147-167` |

The gate also requires **exactly 1600×900** PNGs (`tools/visual_assessment_gate.py:230-231`). The report only requires at least 640×360.

### 6.6 Harness fragility and half-finished parts

- **Staged, not observed.** Screenshots used for acceptance include decoration and actors that gameplay never produces: forest dressing, NoAI ants with forced states, floating markers, cleared corridors, removed labels. Visual verdicts therefore judged QA staging as much as the game.
- **No server-to-client acknowledgement.** The client captures even when the scene command failed. The only exception is the crown check, which is caught later by a log grep.
- **Order dependence:** leftover out-of-radius content, the pinned origin, and a persisted language option.
- **Environment:** it needs an unlocked Windows desktop session (`docs/autonomous-dev.md:136`), takes minutes per run (900 s timeout), and assumes an exact 1600×900 framebuffer.
- **Duplicated scenes:** `tablet_trade` and `tablet_market` are staged identically; `tablet_requests` is actually a Russian-locale scene.
- **Dead conditions:** `CULTURE_STYLES`, `DIPLOMACY_SCENE` and `WORLDGEN_ENCOUNTER` are tested in `seedVisualState` (`VQS:653`) but those scenes return before reaching it. The comment block at `VQS:640-652` is garbled, with interleaved sentences. A comment at `VQS:266` is truncated mid-sentence.
- **Wrong source set:** the harness lives in the main mod jar, and the command is unguarded (§9).
- **No gate for the structure scope.** The 45 focused scenes, which were all the July rebuild work, can only be reviewed manually. The gate's scene list would fail on a structure-scope report.

---

## 7. Visual assessment (model-based and "manual") and the rubric

### 7.1 Assessor modes

`scripts/autonomous-gate.ps1 -VisionAssessor manual|openai|glm5v` (`scripts/autonomous-gate.ps1:5-6`):

| Mode | Who judges | Transport, model, parameters | Required `Assessor:` line |
|---|---|---|---|
| **manual** (default since `88b040f`, 2026-07-12) | The Codex child itself, which may be GLM-5.2 through the text-only proxy. It must write `build/visual-qa/formic-visual-assessment.md`. The gate only checks that the file exists and has the right format (`scripts/autonomous-gate.ps1:112-117`). | none | `Codex manual original-resolution review (GPT-5.4 mini explicitly disabled by user)` |
| **openai** (the default before `88b040f`) | GPT-5.4 mini | Default transport is the Codex CLI: `codex exec --cd <repo> --sandbox read-only --ephemeral --json -m gpt-5.4-mini -o <report> -i <refs…> -i <19 screenshots> -` (`scripts/openai-visual-assessment.ps1:231-246`). The `api` transport posts to `https://api.openai.com/v1/responses` with `OPENAI_API_KEY`, detail `high`, reasoning effort `low`, `max_output_tokens` 8192, 300 s timeout (`tools/openai_visual_assessment.py:18-20`, `305-313`, `351-368`). The model, assessor and endpoint can be overridden with `OPENAI_VISION_MODEL`, `OPENAI_VISION_ASSESSOR`, `OPENAI_RESPONSES_ENDPOINT`. | `GPT-5.4 mini` |
| **glm5v** | GLM-5V-Turbo | Chat Completions to `https://api.z.ai/api/paas/v4/chat/completions`, falling back to `https://api.z.ai/api/coding/paas/v4/chat/completions`. Model `glm-5v-turbo`, temperature 0.1, `thinking: enabled`, `max_tokens` 8192, 300 s timeout, `ZAI_API_KEY`. Overrides: `ZAI_VISION_MODEL`, `ZAI_VISION_ENDPOINT`/`ZAI_GLM5V_ENDPOINT` (`tools/glm5v_visual_assessment.py:18-22`, `297-303`, `337-362`). | `GLM-5V-Turbo` |

**Inputs attached to the model modes:**
- the three reference PNGs from `docs/visual-intent/reference-manifest.json`, sent first;
- the 19 screenshots, each preceded by a text caption;
- the visual QA report;
- `visual-loop-state.json`;
- the visual intent doc (truncated at 20k characters);
- the runtime feature matrix, or the template if none exists (22k);
- the rubric (18k) and the report template (12k);
- the last 12k characters of `latest.log` (`tools/openai_visual_assessment.py:90-192`).

**If the model call fails**, the tools write a blocked report: `Verdict: FAIL` with `1. [P0] … could not complete` (`tools/openai_visual_assessment.py:282-297`; `glm5v` `:275-290`). `normalize_report` injects the `Assessor:` and `Model:` lines if they are missing (`tools/openai_visual_assessment.py:266-279`), so the assessor-line check proves nothing about who actually wrote the report.

**Reference images:** the manifest notes that "two legacy filenames are swapped" (`docs/visual-intent/reference-manifest.json:4`). `reference-forest-foraging.png` actually shows the compact tall ant-hill facade, and `reference-mega-nest-front.png` shows forest-floor density. The intent doc (`docs/visual-intent/formic-visual-intent.md:13-18`), the matrix `intentRefs`, and the template's Reference Diff instructions still use the filenames' literal meaning, which is a source of confusion for the assessor.

### 7.2 Prompt contract (the Codex-transport prompt is the richest; `scripts/openai-visual-assessment.ps1:143-176`)

- The report must start with `# Formic Visual Assessment`, `Verdict: FAIL | PASS WITH NOTES | PASS`, `Assessor: …`, `Model: …`.
- Any P0 or P1 finding means FAIL. Missing, blank, crashed, stale or unreadable scenes are P0. Core readability or playability problems are P1.
- In non-tablet shots, a visible HUD, hotbar, crosshair, hand, toast, or foliage blocking the subject is an **invalid artifact** and gets P1. The fix must go to the capture harness, not the geometry.
- **R2 hard bar:** `colony_overview`, `colony_ground` and `settlement_scale` must show real 20–30 block vertical silhouettes. "A broad flat pad with a thin column, cap, or table-like crown" is P1 and FAIL.
- One tall central mound cannot carry the verdict. Surrounding role buildings must have mass, height and entrances; if they don't, that is at least P2, or P1 if the slice claimed R2 completion.
- PASS is forbidden while required matrix rows remain visually unproven.
- Each issue must state the scene, visible evidence, player impact, a concrete fix direction, and an acceptance check. The report must mention every screenshot and the relevant matrix row ids.
- **`## Reference Diff`:** compare the wide shots with the mega-nest references on topology (one carved mass versus N cones), silhouette, chamber style, and the single biggest gap. If the form is wrong, demand a representational change rather than a parameter tweak.
- **`## Matrix Scorecard`:** one line per required row, `<row_id>: pass|partial|fail|unknown - score N/5 - <instruction>`.
- **Anti-repeat rule:** if a row failed for the same root cause in 2 or more attempts, prefix it with `REPEAT:` and prescribe a different generator or algorithm.
- The API-transport prompt (`tools/openai_visual_assessment.py:130-192`) and the GLM prompt (`tools/glm5v_visual_assessment.py:124-186`) are an **older subset**. They lack the HUD-artifact rule, the R2 hard bar wording, Reference Diff, Scorecard and REPEAT. The prompts had drifted apart by transport.

### 7.3 Rubric (`.codex/skills/formic-visual-assessment/references/rubric.md`)

- **Sources cited:** the NN/G 10 heuristics and heuristic-evaluation method, WCAG 2.2 (text contrast **4.5:1**, **3:1** for large text and non-text), the Xbox accessibility guidelines, Game Accessibility Guidelines, and APX patterns (`rubric.md:5-14`).
- **Severity** (`rubric.md:16-23`):
  - **P0:** unusable artifact or scene (blank, wrong resolution, crash, missing screenshot, missing model or texture, broken UI state).
  - **P1:** the player cannot reliably read, identify or use a core feature (clipped tablet text, overlapping values, indistinguishable ants, a floating colony, unclear controls).
  - **P2:** it works but looks untrustworthy, noisy or ugly.
  - **P3:** polish that should not block a merge on its own.
  - Default verdict: any P0 or P1 means FAIL.
- **Lenses:**
  - **UI readability** (`rubric.md:25-36`): readable at captured resolution; no clipping, overlap or spill; Russian text must fit; contrast; hierarchy (current tab, state, values, actions); controls look clickable; disabled states distinguishable without colour alone; scannable density; "Minecraft GUI style is not an excuse".
  - **Minecraft world fit** (`rubric.md:38-48`): grounded, no air under key blocks, grid-aligned, the camera shows the subject, lighting, no overlays, a blocky and coherent style.
  - **Ant quality** (`rubric.md:50-59`): distinct caste silhouettes, scale or pose; identifiable at gameplay distance; readable textures; clear head, body and legs; the lineup shows variety. Placeholder or toy-like ants are at least P2, or P1 if identification is blocked.
  - **Colony quality** (`rubric.md:61-78`): hub, paths, resources, buildings and progression are communicated; focal point; distinct nodes and buildings; scale. **A single tall mound does not prove the settlement.** Low platforms are at least P2, or P1 if the slice promised R2 completion.
  - **Localization** (`rubric.md:80-84`): both EN and RU assessed; mojibake, missing glyphs, untranslated keys and clipped RU text are serious.
  - **Evidence standard** (`rubric.md:86-96`): Scene, Evidence, Impact, Fix, Acceptance. Use "likely" when uncertain, and make no claims about code causes unless the code was inspected.
- **SKILL.md verdict rules** (`.codex/skills/formic-visual-assessment/SKILL.md:56-67`):
  - FAIL = any P0 or P1;
  - PASS WITH NOTES = only P2 or P3 remain;
  - PASS = no material issues.
  - The skill also requires "inspect every expected screenshot at original resolution" and "lead with blockers" (`:21-32`), and says GPT-5.4 mini is disabled (`:14-17`).

### 7.4 Report template (`.codex/skills/formic-visual-assessment/references/report-template.md`)

The template fixes the section order:
1. Header: Verdict, Assessor, Artifacts.
2. `## Reference Diff`: topology, silhouette, chambers, surface, biggest gap.
3. `## Blockers`, numbered `[P0/P1]` items with Scene, Evidence, Impact, Fix, Acceptance.
4. `## Scene Findings`, per scene: Verdict PASS / FAIL / NEEDS WORK.
5. `## Matrix Scorecard`: score 5 = matches the reference intent, 0 = wrong representation or not started; `unknown` must name the missing capture; `REPEAT:` rule.
6. `## Prioritized Fix Backlog`.
7. `## Acceptance Checks For Dev Agent`: test-mod passes, all screenshots present, no P0/P1, plus specific checks — tablet text not clipped, guide states understandable, colony floor grounded, castes distinguishable, at least 3 jobs visible in `work_cycle`.

### 7.5 Visual feature matrix (`docs/visual-intent/visual-feature-matrix.template.json`)

- **14 rows; 11 are required and in the visual-baseline phase.**
- Required rows:

| Row | Priority | Owner | Evidence scenes |
|---|---|---|---|
| `multiple_large_organic_chambers` | P1 | world-worker | overview, settlement, progression, culture |
| `visible_tunnel_mouths_and_chambers` | P1 | world-worker | ground, settlement, construction, repair |
| `no_single_mound_pass` | P1 | gatekeeper | overview, settlement, culture |
| `organic_asymmetric_ant_buildings` | P1 | world-worker | overview, settlement, culture, construction, repair |
| `structure_spacing_non_overlap` | P1 | world-worker | overview, settlement, culture |
| `ants_read_as_real_insects` | P1 | ant-worker | lineup, work_cycle, ground |
| `ant_lineup_all_castes_unclipped` | P1 | owner "formic-qa-harness" (no such agent exists) | lineup |
| `organic_mound_camera_framing` | P1 | qa-harness | overview, settlement, culture, progression |
| `holey_block_texture_redraw` | P2 | asset-worker | overview, settlement, construction, repair |
| `formic_textures_32x32` | P2 | asset-worker | overview, settlement, construction, repair, tablet_market, tablet_requests |
| `tablet_visual_hierarchy` | P2 | tablet-worker | tablet_en, _ru, research_map, market, requests |

- **Optional rows, already `pass` "by user directive" on 2026-06-23:** `forest_floor_life_density` (P3) and `formic_native_material_palette` (P3).
- **Phase switch:** `mechanics_phase_locked_until_visual_pass` (P0). It is excluded from the gate's count because its phase is not visual-baseline.
- **Lifecycle** (`scripts/visual-loop-brief.ps1:102-159`):
  - The runtime copy `build/autonomous-loop/visual-feature-matrix.json` is created from the template only if it is missing.
  - New template rows are merged in by id, but **changes to existing rows are never propagated**.
  - `visualBaselinePass` becomes true only when every required visual-baseline row is `pass`.
  - Agents edit row statuses themselves (`LOOP:830`). The file lives in gitignored `build/`, so it is lost on a clean checkout.

### 7.6 The gate: `tools/visual_assessment_gate.py`

Checks, in order (`tools/visual_assessment_gate.py:306-446`):

1. `visual-loop-state.json` parses, if it exists.
2. **Destructive-command scan** of the Codex JSONL (`--codex-json-log`). Any `command_execution` matching `git checkout --`, `git reset`, `git restore`, `git clean`, or a `Remove-Item` whose own arguments target `src/` fails the gate (`:267-292`, `:342-348`).
3. `visual-qa-report.json` exists, has status `passed`, and lists screenshot entries. Each entry must be a PNG inside the QA dir whose byte count and dimensions match the report, **1600×900**, and together they must cover all 19 `EXPECTED_SCENES` (`:199-239`).
4. If present, `visual-qa-summary.json` has status `complete` or `passed` (`:371-379`).
5. **Freshness:** if a marker is given (by argument, or read from the loop state), the report JSON, report MD, every screenshot and the assessment must have an mtime strictly newer than the marker. Otherwise the gate exits with code 2 (`:381-393`).
6. The assessment exists. If not, it writes `formic-visual-assessment-required.md` and exits 2. The assessment's mtime must be ≥ the newest screenshot's, otherwise exit 2 (`:395-407`).
7. A `Verdict:` line is present (regex `(?im)^\s*Verdict\s*:\s*(PASS WITH NOTES|PASS|FAIL)\b`), and the required `Assessor:` line matches exactly (`:66-68`, `:108-112`, `:409-416`).
8. **Block if** the verdict is FAIL, any line reads `Verdict: FAIL`, or any line starts with an optional list marker followed by `[P0]` or `[P1]` (`:71-76`, `:417-423`). A blocking excerpt from `## Blockers` is printed.
9. **Matrix:** every required visual-baseline row must be `pass`. The advisory flag `--allow-open-visual-matrix` exists but is never passed by the scripts (`:425-443`). A missing matrix fails unless `--allow-missing-matrix` is given.

The gate does **not** read `build/qa/test-mod-summary.json`, the Scorecard, or whether the images were actually viewed.

### 7.7 Weaknesses of the assessment design

- **Self-certification.** In manual mode, the agent that changed the code writes the verdict and edits the matrix statuses. The gate is a format and freshness checker. The maker/checker split exists only in prose (`docs/autonomous-dev.md:194-201`; the agent TOMLs).
- **The worker could be blind.** Through the GLM proxy (text-only, §8.8) a "manual original-resolution review" cannot be grounded in pixels. The earlier skill text said so directly: "Text-only GLM-5.2 agents … must not synthesize the final screenshot verdict themselves." That sentence was replaced in `88b040f` (see `git show 88b040f -- .codex/skills/formic-visual-assessment/SKILL.md`).
- **Contradictory instructions.** `formic-visual-assessor.toml` still says the final judgment must come from GPT-5.4 mini with `Assessor: GPT-5.4 mini` (`.codex/agents/formic-visual-assessor.toml:6-15`). A report written that way fails the default manual gate.
- **The gate structure pushes toward self-certification.** The gate passes only when all 11 required rows are `pass`. Every world-visual iteration that doesn't claim the whole baseline fails and is retried, up to 6 times. That pressures the agent either to mark rows `pass` or to switch to the content track.
- **Brittle regex parsing.** Only line-leading `[P0]`/`[P1]` tags block. A P1 written inline, as "(P1)", or as "Severity: P1" passes.
- **Thresholds loosened to pass:** tablet PNGs 100 KB → 70 KB.
- **Drift across prompts and lists:** prompts differ per transport, and scene lists differ (§6.5).

---

## 8. Autonomous development pipeline

### 8.1 Inventory

Every `.cmd` file is a 2-line wrapper: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0<name>.ps1" %*`.

| Script / tool | Lines | Role |
|---|---|---|
| `scripts/start-autonomous-loop.ps1` | 112 | Refuses to start if the pid file points at a live process. Removes the stop file, validates the Codex profile name and file, generates `ZAI_CODEX_PROXY_TOKEN` for `zai-glm52`, then launches `autonomous-loop.ps1` in a **hidden** `powershell.exe`, redirecting stdout and stderr to `build/autonomous-loop/supervisor.{out,err}.log` and writing `supervisor.pid` |
| `LOOP` (`scripts/autonomous-loop.ps1`) | 1371 | The supervisor. Iterations, attempts, Codex child process, watchdogs, checkpoints, guards, per-track gating, state files |
| `scripts/stop-autonomous-loop.ps1` | 41 | Writes `stop.requested`; `-StopProxy` also kills the proxy pid |
| `scripts/autonomous-gate.ps1` | 158 | World-visual gate |
| `scripts/content-gate.ps1` | 32 | Content gate |
| `scripts/visual-loop-brief.ps1` | 330 | Maintains the runtime matrix, brief, and progress log |
| `scripts/doctor.ps1`, `scripts/java-env.ps1` | 68, 133 | Environment checks; JDK auto-selection |
| `scripts/test-mod.ps1` | 114 | Gradle build, tests, log scan, summary |
| `scripts/prepare-gui-world.ps1`, `scripts/gui-smoke.ps1` | 136, 148 | Quick-play world preparation; screenshot run |
| `scripts/openai-visual-assessment.ps1`, `scripts/glm5v-visual-assessment.ps1` | 267, 39 | Optional model assessors |
| `scripts/zai-codex-proxy.ps1` + `.py` | 29 + 693 | Responses-to-Chat-Completions proxy for GLM-5.2 |
| `tools/visual_assessment_gate.py`, `content_feature_gate.py`, `visual_qa_report.py`, `validate_assets.py` | 450, 107, 183, 136 | Deterministic checks |
| `tools/generate_formic_textures.py`, `render_texture_contact_sheet.py` | 520, 36 | Asset generation (a dependency-free pixel canvas) and an item contact sheet written to `build/visual-qa/formic-item-textures.png`. Not part of any gate |

### 8.2 End-to-end flow of one supervisor run

1. **Startup** (`LOOP:1135-1158`):
   - Write `supervisor.pid` and remove any stale stop file.
   - `Use-FormicJava -MinimumMajor 21` (throws if it fails).
   - `Assert-CodexProfile`: the name must match `^[A-Za-z0-9_.-]+$` and `%CODEX_HOME%\<profile>.config.toml` must exist.
   - `Ensure-ZaiProxyToken`, then `Start-ZaiProxyIfNeeded`. For the `zai-glm52` profile, if `/v1/health` is not OK it requires `ZAI_API_KEY`, kills any stale proxy, starts `zai-codex-proxy.ps1` hidden, and waits up to 30 s for health `{status: ok, model: glm-5.2, has_api_key, has_proxy_auth_token}` (`LOOP:451-527`).
   - Resolve the `codex` command, then run `doctor.ps1`. **Its exit code is ignored.**
2. **Per iteration** (`LOOP:1160-1179`):
   - Create a timestamped **freshness marker** file, `iteration-NNN-<stamp>.visual-marker.txt`.
   - Update `visual-loop-state.json` (screenshot verdict `pending`) and append `iteration.started` to `visual-progress.jsonl`.
   - Run `visual-loop-brief.ps1` and write the iteration prompt to `iteration-NNN-<stamp>.prompt.md`.
3. **Per attempt** (`LOOP:1184-1346`):
   - For retries, create new prompt, log and marker files with suffix `retry-NN` (NN = attempt − 1), using the guard-retry prompt (`LOOP:1185-1200`).
   - **Checkpoint:** save `git status --porcelain`, `git diff --binary` and `diff --stat` to `build/autonomous-loop/checkpoints/iteration-NNN-attempt-NN-<ts>/before-*`, plus SHA-256 hashes (`LOOP:288-316`).
   - **Run Codex:** `codex exec --cd <repo> --sandbox danger-full-access --json -o <final.md> [-m <Model>] [-p <Profile>] -`, with the prompt on stdin, via `System.Diagnostics.Process` and event handlers that stream stdout to `.jsonl` and stderr to `.err.log`. The child pid goes to `codex-child.pid` (`LOOP:938-1133`).
   - **After-checkpoint** and `unverifiedChanges` summary (`LOOP:318-359`).
   - **Destructive-command guard:** the JSONL is scanned for `git checkout --`, `reset`, `restore`, `clean`, or `Remove-Item …src\`. On a hit the attempt is marked failed and retried, up to `MaxGuardRetries`, after which the status becomes `blocked_destructive_command` and the loop exits 1 (`LOOP:361-392`, `1213-1227`). **This detects the command after it has already run; it prevents nothing.**
   - **Codex exit code ≠ 0:**
     - *Transient* failures: stream disconnects, TLS errors, `responses_retry 5/5`, or rate limits (HTTP 429, "too many requests", "rate limit", quota, "resource exhausted", Z.AI code `1305`, overloaded) (`LOOP:136-179`). Backoff is parsed from the error text (a reset timestamp +300 s, "N hours" +300 s, "N minutes" +60 s, or `retry-after` +30 s), clamped to [180 s, 1800 s] (`LOOP:181-222`); non-rate-limit transients wait 10 s.
     - **Rate-limit retries do not count toward the retry limit**, so the loop retries indefinitely. The comment reads "Z.AI 5h rolling quota" (`LOOP:1252-1255`, `1272`).
     - If the failed attempt changed the worktree, it becomes a "repair-only retry" whose prompt includes the latest `test-mod-summary.md` (`LOOP:1243-1266`).
     - Any other nonzero exit is a `codex_nonzero_repair` retry (`LOOP:1280-1296`).
   - **Track gate** (`LOOP:1298-1327`):
     - `Get-ActiveTrack` reads `activeTrack` from `visual-loop-state.json`, **which the child wrote**, and defaults to world-visual (`LOOP:402-420`).
     - content → `content-gate.ps1`.
     - world-visual → `autonomous-gate.ps1 -NoLaunch -FreshnessMarker <m> -CodexJsonLog <jsonl>`, then `Assert-FreshVisualArtifacts`: the report JSON and MD and the assessment must be newer than the marker, the report must be `passed`, every screenshot must be newer than the marker, and the assessment must be at least as new as the newest screenshot (`LOOP:696-742`).
   - **Accept or retry:** if the gate passes, the iteration is accepted (`gate.passed`). Otherwise `gate.failed` is logged and the attempt retried. Once `retryCount` (attempt − 1) reaches `MaxGuardRetries` (default 6), the state becomes `blocked` and the process **exits 1** (`LOOP:1329-1346`). Nothing restarts the supervisor after that.
4. **After an accepted iteration:** state becomes `passed`, the brief is refreshed, and the loop sleeps `PauseSeconds` (30) before the next iteration. It runs until `MaxIterations` (0 means unlimited) or a stop file (`LOOP:1353-1360`).
5. **Watchdogs,** polled every 10 s in the Codex wait loop (`LOOP:1067-1101`):
   - *Activity watchdog:* if the JSONL has not grown for 45 minutes and there is no final message, the child is killed and the attempt returns 124, which leads to a repair retry.
   - *Completion watchdog:* if a final message exists, `turn.completed` appears in the JSONL, and the log has been idle for 5 minutes, the child is killed and the attempt **treated as exit 0**.
   - *Stop file:* the child is killed within about 10 s and the attempt returns 130. `stop-autonomous-loop.ps1`, however, prints "will stop after the current iteration or sleep" (`scripts/stop-autonomous-loop.ps1:21`), which is inaccurate: the current turn is killed mid-edit.
   - A 30 s timeout on draining the output streams returns 124 (`LOOP:1102-1115`).

### 8.3 What the iteration prompt tells the child (`New-IterationPrompt`, `LOOP:744-875`)

- Read `docs/roadmap.md`, `docs/autonomous-dev.md`, both intent docs, `docs/local-stack/index.md`, the skill's SKILL.md, and the runtime state, matrix, brief and progress files.
- **Pick exactly one track:** world-visual (visual gate) or content (test-mod plus content gate), and record it as `activeTrack` in `visual-loop-state.json`. "When the current world-visual target is blocked … advance a content row instead" (`LOOP:765-768`).
- A handoff is inlined (`LOOP:634-687`): the brief, `manual-visual-diagnosis.md` (no script produces this file, so it must have been written by a human or another agent), the last assessment (12k characters), the state, and the matrix.
- **Constraints:**
  - "GitHub publishing is not available yet because this repo has no remote and gh is missing". In fact the repo has an `origin` GitHub remote.
  - Do not revert uncommitted baseline changes.
  - **Destructive recovery commands are forbidden.**
  - A **CWD guard** must prefix every shell command. The prompt mentions agents mistakenly working in the sibling folder `2026-04-28\new-chat` (`LOOP:776-780`).
  - Preserve every QA scene. Use subagents with maker/checker separation. Start from screenshot evidence.
  - Never end the turn with a plan or an "I need to…" message.
- **Hard-coded art direction and user retargeting** (`LOOP:787-816`): the renovation order R1, R2, R3; priority order architecture > textures (32×32) > tablet beauty > mechanics; R2 scale rules (main mound 20–30 blocks tall on a 16–24 footprint); current user blockers (no pancake or table crown, no symmetric or overlapping houses, no freestanding arches); forest floor and material palette accepted; "**Break the local minimum**": read the Reference Diff and Scorecard, and on `REPEAT:` change the representation.
- **Visual compute loop:** scout → one worker ownership area → implement → fresh screenshots → inspect every PNG and write the manual report ("Do not run GPT-5.4 mini") → repair P0/P1 only → gatekeeper.
- **Artifact contract:** update `visual-loop-state.json` and append to `visual-progress.jsonl` after every phase (scout, build, smoke, assessment, gate); update matrix row statuses; do not claim a final summary that was not written (`LOOP:827-833`).
- **Ownership boundaries** by area (`LOOP:835-840`).
- **Definition of done:**
  - world-visual: implement → `test-mod` → `gui-smoke` → manual report → `autonomous-gate -NoLaunch`.
  - content: implement one content-matrix row → add a gametest → `test-mod` green → mark the row `pass` only with an evidence test → `content-gate` (`LOOP:844-860`).
- **Final response must include** the track, row id, files changed, gate results, and the next slice (`LOOP:867-871`).

### 8.4 The retry prompt (`New-GuardRetryPrompt`, `LOOP:877-936`)

It includes the failure detail, a new marker, the CWD guard, the ban on destructive commands, "Do not restart broad investigation", continue the same track and slice, the R2 bar, the retargeting notes, "If the manual image-capable diagnosis says a screenshot artifact is invalid … fix the QA harness/camera first", the mandatory order `gui-smoke` → assessment → gate, and the previous final response.

### 8.5 Gates, and the exit-code hole (verified)

**World-visual gate** (`scripts/autonomous-gate.ps1:87-158`), in order:
1. `Use-FormicJava`.
2. `& doctor.ps1`.
3. `& test-mod.ps1`.
4. `& gui-smoke.ps1 -NoLaunch`, which only regenerates the report.
5. `& visual-loop-brief.ps1`.
6. With `-NoLaunch`, the report and every screenshot must be newer than the marker, otherwise exit 2.
7. Assessor step: manual (the file must exist), or run the OpenAI or GLM assessor.
8. The brief again.
9. `python tools\visual_assessment_gate.py …` with the marker, the JSONL and the required assessor. Its exit code is the result.

**Content gate** (`scripts/content-gate.ps1:9-31`): Java, `& doctor.ps1`, `& test-mod.ps1`, then `python tools\content_feature_gate.py --matrix build\autonomous-loop\content-feature-matrix.json --test-summary build\qa\test-mod-summary.json`. The Python gate:
1. Requires the test summary to have status `passed`.
2. Requires the matrix to exist with at least one row.
3. Rejects any row marked `pass` without an `evidenceTest` string.
4. Prints the open rows. **Open rows do not fail it** (`tools/content_feature_gate.py:59-103`).

**The hole:**
- `test-mod.ps1` and `doctor.ps1` report failure only through `exit 1` (`scripts/test-mod.ps1:106-109`, `scripts/doctor.ps1:58-62`). Both gates call them with `&` and never read `$LASTEXITCODE`.
- I ran a two-script experiment on this machine (Windows PowerShell 5.1; parent `$ErrorActionPreference = "Stop"`, child `exit 1` inside `try/finally`). The parent printed `CONTINUED after child exit; LASTEXITCODE=1` and the host exited 0.
- **World-visual track:** a failing build or failing gametests did not stop the gate. `visual_assessment_gate.py` never reads the test summary. The prompt asked the agent to run `test-mod`, but nothing enforced green tests.
- **Content track:** protected, because the Python gate re-reads the summary JSON.
- The same pattern existed in the v1 gate (`git show b9f9faa:scripts/autonomous-gate.ps1`).

**`-NoLaunch` versus launching.** Without `-NoLaunch`, `autonomous-gate.ps1` relaunches `gui-smoke`, which **deletes `build/visual-qa` including the manual report** (`scripts/gui-smoke.ps1:83-85`). The gate then throws "Manual visual assessment report is missing". The documented one-liner `scripts\autonomous-gate.cmd -AllowMissingGitHub` (`README.md:64`, `docs/autonomous-dev.md:10`) therefore cannot pass in manual mode. The supervisor always passes `-NoLaunch`.

### 8.6 The content track

- The runtime matrix `build/autonomous-loop/content-feature-matrix.json` is **never generated by any script** (a grep shows only reads). The template `docs/content-intent/content-feature-matrix.template.json` has 9 rows, all required, all status `unknown`:
  - **P1:** `resource_production_consumption_tick`, `caste_job_loops_change_state`, `progression_stage_advance_autonomous`, `research_unlocks_have_effects`.
  - **P2:** `trade_caravan_exchanges_resources`, `politics_relations_shift_from_actions`, `block_native_gameplay_roles`, `weapon_soldier_combat_stats`, `defense_raid_outcome_changes_state`.
  - Owners named there are `formic-sim-worker`, `formic-diplomacy-worker`, `formic-content-worker` and `formic-combat-worker`. **None of these agent roles exist** in `.codex/agents/`.
- Code comments cite two more rows, `caste_population_auto_balances` (`CET:1207`, `ColonySavedState.java:106`) and `tablet_interactions_functional` (`GT:2068`). **Neither is in the committed template.** The runtime matrix in `build/` is lost.
- The acceptance bar (`docs/content-intent/formic-content-intent.md:79-92`): a deterministic gametest proves the end-to-end state change, `test-mod` is green, the feature is reachable in normal play, a UI feature gets a tablet screenshot, and nothing regresses. The anti-patterns forbid weakening or deleting tests (`:94-100`).
- In practice, 9 of the 10 rows were proven by JUnit tests. The gate does not check that the named `evidenceTest` exists or ran.

### 8.7 Models, providers, APIs

- **Worker:** Codex CLI `codex exec` (`LOOP:948-961`).
  - The model is not pinned in the repo: `-Model` defaults to empty, so Codex's default model is used. The user reports GPT 5.4 and 5.5. The string "gpt-5.5" appears nowhere in the repo.
  - With `-CodexProfile zai-glm52`, the user-level config `C:\Users\user\.codex\zai-glm52.config.toml` (not in the repo) points Codex at `http://127.0.0.1:11452/v1`, i.e. the proxy, which forwards to Z.AI model **glm-5.2** at `https://api.z.ai/api/coding/paas/v4/chat/completions` (`scripts/zai-codex-proxy.py:17-21`; `docs/autonomous-dev.md:64-90`).
- **Vision assessors:** GPT-5.4 mini (Codex CLI auth or the OpenAI Responses API), GLM-5V-Turbo (Z.AI), or "manual" (§7.1).
- **Claude Opus 4.8** appears only as `Co-Authored-By` on 3 commits from 2026-06-28 (`cc9db89`, `f5e9857`, `c3a7aeb`). These were interactive sessions; nothing in the loop calls Anthropic APIs.
- The local-stack docs include digests of Anthropic engineering posts and the Codex docs, framed as "harness rules" for offline GLM workers (`docs/local-stack/*.md`).

### 8.8 The Z.AI → Codex proxy (`scripts/zai-codex-proxy.py`)

- **Purpose:** Codex custom providers speak the OpenAI Responses API, while Z.AI's Coding Plan offers Chat Completions (`docs/local-stack/codex-harness.md:21-28`).
- **Server:** `ThreadingHTTPServer` on 127.0.0.1:11452 (`:634-689`). Endpoints:
  - `GET /health` and `/v1/health` report status, model, `has_api_key` and `has_proxy_auth_token` (`:264-274`).
  - `GET /models` and `/v1/models` return a synthetic catalog (`:196-247`): "Z.AI GLM 5.2", reasoning levels low to xhigh, `apply_patch_tool_type: freeform`, context window 1,000,000, `input_modalities: ["text"]`.
  - `POST /responses` and `/v1/responses` (`:280-303`).
- **Translation:**
  - `instructions` become a system message; input messages map roles, with developer → system.
  - `function_call` becomes an assistant `tool_calls` message, and `function_call_output` becomes a `tool` message (`:83-135`).
  - Tools of type `function` (or no type) are converted; **other tool types are silently dropped** (`:138-166`).
  - `max_output_tokens` → `max_tokens`; `temperature` and `parallel_tool_calls` pass through; the upstream request always uses `stream: true` (`:169-193`).
- **Streaming back** as Responses SSE events: created, in_progress, output_item.added, content_part.added, output_text.delta and done, function_call_arguments.delta and done, output_item.done, completed or failed, then `[DONE]` (`:324-595`).
  - A **heartbeat thread** writes `: keep-alive` every 5 s. The comment explains that "codex drops the stream with IncompleteRead" during GLM's 10–40 s time-to-first-token (`:346-368`).
  - If there is no content but there is reasoning content, the reasoning is emitted as the answer (`:519-520`).
  - `usage` is always `None`.
- **State:** conversations are kept in a process-global dict `THREADS[response_id]`, the last 80 messages each. It is never evicted, grows without bound, and is lost when the proxy restarts, which breaks `previous_response_id` (`:23`, `:170-172`, `:594`, `:619`).
- **Security and fidelity problems:**
  - The auth token is never checked (`:641` is read, but there is no header check in `:280-303`). Exposure is local-only.
  - **Images are dropped** (`text_from_content`, `:42-66`), so the GLM worker could not view screenshots.
  - Non-function tools are dropped.
  - The upstream timeout is 1800 s (`:671`).
  - Logs go to `build/zai-codex-proxy/proxy.log` (request summaries and character counts only).
- `scripts/zai-codex-proxy.ps1` requires `ZAI_API_KEY` in the environment and launches the Python script (`:14-26`). A `__pycache__/zai-codex-proxy.cpython-312.pyc` is committed.

### 8.9 State files and artifacts (all in gitignored `build/`)

- **`build/autonomous-loop/`:**
  - `run-state.json` (supervisor status, iteration, child pid, JSONL path, whether the final message exists, idle minutes, transport status, backoff, last failure kind, unverified changes, checkpoint; `LOOP:37-98`);
  - `visual-loop-state.json`, merge-updated so that scout and assessor fields such as `acceptanceBrief`, `screenshotVerdict`, `blockingSeverityCount` and `polishBacklog` are preserved (`LOOP:529-617`);
  - `visual-feature-matrix.json`, `content-feature-matrix.json`;
  - `visual-loop-brief.md` and `.json`;
  - `visual-progress.jsonl` (append-only events);
  - per-iteration `iteration-NNN-<stamp>[-retry-NN].{prompt.md, jsonl, err.log, final.md, visual-marker.txt}`;
  - `checkpoints/<id>/{before,after}-{status.txt, diff.patch, diffstat.txt}`, `metadata.json`, `after-metadata.json`;
  - `supervisor.{pid, out.log, err.log}`, `codex-child.pid`, `stop.requested`, `manual-visual-diagnosis.md`;
  - `openai-visual-assessment-<stamp>.{prompt.md, jsonl, err.log}`.
- **`build/visual-qa/`:** `screenshots/*.png`, `visual-qa-summary.{json,md}` (client), `visual-qa-report.{json,md}` (report tool), `formic-visual-assessment.md`, `formic-visual-assessment-required.md`, `runClient.log`, `runClient.err.log`, `latest.log`, `quickplay.json`, `prepare-world.{log,err.log}`, `options.before-visual-qa.txt` (never restored).
- **`build/qa/`:** `gradle-build.log`, `test-mod-summary.{json,md}`.
- **`build/zai-codex-proxy/`:** `proxy.{pid, out.log, err.log, log}`.
- None of these exist in the current checkout.

### 8.10 How commits happened

- **The loop never commits or pushes.** There is no `git commit`, `add` or `push` anywhere in `scripts/`, `tools/`, `.codex/` or the docs. The prompt tells the agent to leave uncommitted baseline changes alone (`LOOP:775`), and the checkpoints only snapshot diffs. Accepted iterations stay as uncommitted worktree changes until someone commits them.
- **Evidence from the history:** 37 commits by a single author identity.
  - The R2 commits (06-23) include "retry-07" and "Checkpoint … (attempt 6)" in their messages, which suggests a human or interactive agent committed after supervisor attempts.
  - `7af4b42` bundles 47 files: the whole v2 harness, game code, docs, three roughly 2.2–2.5 MB reference PNGs, and a `.pyc`.
  - The July commits are one per building blueprint at 35–70 minute intervals, including overnight. They carry no trailer, so the tool used is not recorded.
- **Reflog** (read-only):
  - The original local root `79e18c0` was dated 2026-04-29 21:53.
  - On 2026-05-01 the branch was renamed main ↔ master, `origin/master` was merged, and the history was then rewritten: the two rewritten commits `a74a7fa` and `b9f9faa` share the timestamp 12:17:43.
  - A `reset: moving to HEAD` occurred on 2026-06-22 22:18.
  - `rebuild/anthills-from-scratch` was checked out from master (`946ea44`) on 2026-07-11 at 13:00.
  - `origin/master` is at `c3a7aeb`; local master is 1 commit ahead.

### 8.11 Agent roles (`.codex/agents/*.toml`) and the skill

| Agent | Sandbox | Mandate (quoted or paraphrased) |
|---|---|---|
| `formic-slice-scout` | `read-only` (`:17`) | Pick one narrow visual slice from screenshots and evidence. Return the target and player promise, the proving scenes, the recommended worker, disjoint file ownership, and the P0/P1 risks for the assessor. |
| `formic-world-worker` | default | Settlement, architecture, construction, repair and culture visuals. Owns `StructurePlacer`, the world parts of `VisualQaScenes`, and related tests. Preserve scene names. Never write the assessment report. |
| `formic-tablet-worker` | default | `ColonyStatusScreen`, snapshot and lang. All 7 tablet scenes must stay readable; RU without mojibake or clipping. |
| `formic-ant-worker` | default | Ant renderer, model, textures and caste scale. Preserve UV assumptions. Acceptance via `ant_lineup`, `work_cycle`, `colony_ground`. |
| `formic-asset-worker` | default | Resource-pack assets. Do not rename registry ids. Run asset validation. Acceptance must include screenshots. |
| `formic-visual-assessor` | default | "Checker, not maker". Its normal action is `openai-visual-assessment.cmd`, it must use `Assessor: GPT-5.4 mini`, and it must not rely on "text-only GLM-5.2 summaries". **Stale:** contradicts the manual default. |
| `formic-gatekeeper` | default | Run or inspect `test-mod`, `gui-smoke` if needed, assessment freshness and verdict, and `autonomous-gate -NoLaunch`. Never rewrite the assessor's verdict. |

All workers carry "You are not alone in the codebase. Do not revert edits made by others" and "list changed paths; state exact blockers". The skill `.codex/skills/formic-visual-assessment/` has SKILL.md, the rubric, the report template, and `agents/openai.yaml`: display name "Formic Visual Assessment", default prompt "Use $formic-visual-assessment to review the latest … screenshots and produce a strict fix report" (`agents/openai.yaml:1-4`).

### 8.12 Supporting scripts

- **`scripts/doctor.ps1`** (`:24-67`):
  - Required: Java 21+ (via `Use-FormicJava`), `gradlew.bat`, a git repo, and `gh` installed and authenticated (optional with `-AllowMissingGitHub`, which the loop always passes).
  - Optional WARN checks: Docker and WSL ("future isolated runners / Linux parity").
  - Fails via `exit 1`.
- **`scripts/java-env.ps1`** (`:1-133`):
  - Candidates: `JAVA_HOME`, `java` on PATH, and subfolders of the Adoptium, Java, Microsoft, BellSoft, Zulu and Corretto install roots under `C:\Program Files`.
  - Parses `java -version`, picks the highest major ≥ 21, and prepends it to `PATH` and `JAVA_HOME`.
  - CI uses Temurin 25; the project targets release 21 (`build.gradle:54-56,72-73`).
- **`scripts/test-mod.ps1`** (`:26-114`):
  - Runs doctor; unless `-SkipClean`, deletes `build\run\gameTest` and writes an empty `server.properties`.
  - Runs `cmd /c .\gradlew.bat build > build\qa\gradle-build.log 2>&1`.
  - **Log scan** of `build\run\gameTest\logs\latest.log`, the repo-root `logs\latest.log` (possibly a stale log from an earlier manual run) and the Gradle log. Any line containing `CRASH`, `CrashReport`, `Missing texture`, `missing model`, `Exception` or `ERROR` is a finding, except five ignore patterns (`fabric-crash-report-info`, and the Windows `server.properties` exceptions) and `Yggdrasil Key Fetcher`.
  - status = `passed` only if the Gradle exit code is 0 **and** there are zero findings. Writes `test-mod-summary.json` (status, buildExit, findings, log path) and `.md`, then exits 1 on failure.
- **`scripts/prepare-gui-world.ps1`** (`:51-135`):
  - Skips if `run/saves/<World>/level.dat` exists.
  - Otherwise writes `run/eula.txt` (`eula=true`, auto-accepted) and `run/server.properties` (level-name, creative, peaceful, offline mode, no spawn protection, command blocks). **No seed is set,** so the terrain varies between machines.
  - Runs `gradlew runServer --args=--nogui` until `level.dat` exists and "Done (" appears in the log, with a 180 s timeout.
  - Tree-kills the Gradle wrapper and **every `java.exe` whose command line contains the repo path together with `runServer` or `KnotServer`**, waits up to 30 s for `session.lock` to release, sleeps 3 s, copies the world to `run/saves/<World>`, and deletes the copied `session.lock`. A comment records an earlier bug: copying before the lock was released led to "Failed to read level data … locked".
- **`scripts/visual-loop-brief.ps1`:** creates or merges the matrix, computes counts and `visualBaselinePass`, parses the assessment verdict and P0/P1 counts plus the first 20 P2/P3 lines, and lists screenshot metadata. Writes the brief MD and JSON (phase `visual_baseline` or `playability_mechanics_ready`, required counts, the 8 highest-priority open rows with owner, next action and acceptance, references, screenshots) and appends a `brief.generated` event (`:102-327`).
- **`tools/validate_assets.py`** (`:26-132`):
  - All JSON under `assets/` and `data/` parses.
  - `en_us` and `ru_ru` have identical key sets.
  - No RU value contains the mojibake tokens `Ã Â Ð Ñ �`.
  - Item model references resolve (`items/*.json`); blockstate `variants` model references resolve (**`multipart` is not checked**); model texture references resolve.
  - All textures are valid PNGs no larger than 512 px.
  - **Item textures must be exactly 32×32; `ant_*.png` exactly 256×128; armor textures (humanoid and humanoid_leggings) exactly 128×64.** Block textures are not size-checked, although the visual matrix demanded 32×32.
  - The size contracts were added in `75a70f7`.

### 8.13 CI (`.github/workflows/build.yaml`)

It runs on every push and pull request, on ubuntu-24.04:
1. `asset-validation`: Python 3.12, `python tools/validate_assets.py`.
2. `build`: Temurin **25**, Gradle wrapper validation, `./gradlew build` ("Build and server GameTests"); uploads `build/reports` and `build/test-results` on failure.
3. `visual-qa-report`: `python tools/visual_qa_report.py --ci-manifest-only` and uploads `build/visual-qa/`. **This job is a no-op that always passes.**

No evidence of CI run results exists in the repo.

### 8.14 How the pipeline evolved

- **v1, 2026-05-01** (`a74a7fa` then `b9f9faa`): `doctor`, `test-mod`, `prepare-gui-world`, `gui-smoke`, `VisualQaClient` with 6 scenes, then 19; the gate v1 (30 lines); a supervisor v1 (157 lines) running one `codex exec` per iteration and stopping ("blocked") on the first failure; the skill and rubric; `visual_assessment_gate.py` v1.
- **v2, 2026-06-23** (`7af4b42`): the 7 agents, local-stack docs, visual intent with reference PNGs and matrix template, the loop rewritten (+941 lines: retries, rate-limit backoff with a 6 h maximum, watchdogs, checkpoints, destructive guard, freshness markers, visual-loop state), the GLM proxy, the OpenAI and GLM assessors, `visual-loop-brief`, and the gate rewrite (+293 lines). The OpenAI assessor was the default.
- **v3, 2026-06-28** (`cc9db89`): the proxy heartbeat and incremental streaming, backoff capped at 30 min with unlimited rate-limit retries, the parallel **content track** (content intent, matrix template, `content-gate`, `content_feature_gate.py`), a fix for a guard regex false positive, and track-aware gating.
- **v4, 2026-07-12** (`88b040f`): default assessor switched to **manual** ("GPT-5.4 mini explicitly disabled by user"), QA ground Y pinned, crown validation, the `formic.visualQa.scenes` property, the tablet PNG threshold lowered to 70 KB, and the README and docs rewritten.

---

## 9. What failed, was fragile, or was left half-finished (consolidated)

### 9.1 Failure modes shown by the history and by the guard-rails that were added

The guard-rails are countermeasures, so the failures they target are **inferred** from them.

- **An architecture local minimum.** On 2026-06-23, five StructurePlacer-only commits in under two hours tuned "satellite crowns", tunnel-mouth reach and dome shape (`15b5932`, `59fba60`, `4bd779b`, `556111f`, `33c1659`). Iterations reached attempt 6 and "retry-07" (the 8th attempt).
  - The whole approach was later discarded: `6fb5bcd` "Reset anthill structures to minimal markers" deleted 1082 lines, including the structure tests. Every building was then rebuilt as validated blueprints (07-11 to 07-12).
  - The intent doc records the lesson: one large geometry method "encouraged repeated parameter nudges without a clear description of the intended form" (`docs/visual-intent/formic-visual-intent.md:83-90`).
  - The Scorecard `REPEAT:` rule and "Break the local minimum" (`LOOP:807`) were added in response.
- **Geometry negotiated against test coordinates.** R2 commit bodies explain crown reach limits chosen specifically to avoid collisions with assertion coordinates in unrelated diplomacy and expansion tests (`15b5932`, `59fba60`, `4bd779b`). The forest dressing broke the famine and side-chamber tests (`33c1659`).
- **Transport instability with GLM-5.2 through the proxy:** stream disconnects ("IncompleteRead") during long time-to-first-token, and Z.AI quota or rate limits ("5h rolling quota"). The responses were a 5 s heartbeat, unlimited rate-limit retries, and a 30-minute backoff cap (`cc9db89` body; `scripts/zai-codex-proxy.py:346-368`; `LOOP:1252-1255`).
- **Agent misbehaviour targeted by prompt rules:**
  - running repo commands in the sibling folder `2026-04-28\new-chat` (CWD guard, `LOOP:778-780`);
  - destructive git or deletion commands (guard, `LOOP:776`; reflog `reset: moving to HEAD` on 2026-06-22);
  - ending turns with a plan instead of doing the work (`LOOP:786`, `926`);
  - claiming final artifacts that do not exist (`LOOP:832`);
  - assessing stale screenshots (freshness markers);
  - judging screenshots spoiled by hotbar, toast, hand, foliage or sky-only framing (the retry prompt at `LOOP:920`; the prompt rule at `scripts/openai-visual-assessment.ps1:162`);
  - a regex false positive in the destructive guard, fixed in `cc9db89` by stopping the `Remove-Item` match at `; & |` (`tools/visual_assessment_gate.py:271-277`, `LOOP:1215`).
- **Harness bugs found and fixed:**
  - the QA platform ratcheting upward past Y=320 (`VQS:336-338`);
  - cold-start capture of a "half-rendered snowy canopy" (`VQC:221-224`);
  - world copy made while `session.lock` was still held (`scripts/prepare-gui-world.ps1:107-109`);
  - a missing crown producing a "valid" non-empty PNG (`tools/visual_qa_report.py:130-133`).
- **Gates passed things the player found broken.** The "Latest player feedback (2026-06-27)" lists roofless square boxes, an ugly tiled "holey" texture, and tablet menus that are "just buttons that do not clearly do anything" (`docs/visual-intent/formic-visual-intent.md:48-72`). All of this was present while the automated gates were in use.
- **The assessor was switched from GPT-5.4 mini to "manual"** on user instruction (`88b040f`; "explicitly disabled by user"). The reason is not recorded.

### 9.2 Pipeline defects, verified by reading the code

1. The world-visual gate ignores `test-mod` and `doctor` failures (§8.5; verified semantics).
2. The gate is self-graded (manual assessor string plus agent-edited matrix), and the checker role is not enforced (§7.7).
3. The GLM worker could not see images; the proxy drops image parts and non-function tools (§8.8).
4. The proxy accepts unauthenticated local requests (§8.8).
5. The child chooses its own gate through `activeTrack` (`LOOP:402-420`).
6. The world-visual gate cannot pass until all 11 required rows are `pass`, which builds in retry churn (§7.6–7.7).
7. The destructive guard is post hoc detection; Codex runs with `--sandbox danger-full-access` (`LOOP:951`).
8. A stop request kills the child mid-turn while the message says otherwise (§8.2).
9. Rate-limit retries are unlimited, and every retry reruns a full Gradle build and GUI smoke. There is no automatic restart after `blocked`.
10. There is no commit, branch or PR automation, although the doctor message says git is "required for autonomous branch/PR workflow" (`scripts/doctor.ps1:41`). The prompt falsely claims there is no remote.
11. Runtime matrices, state and logs live only in gitignored `build/`. Template changes to existing rows never propagate, and the content matrix is never created from its template (§7.5, §8.6).
12. The docs' one-line gate command (`autonomous-gate.cmd -AllowMissingGitHub` without `-NoLaunch`) deletes the manual report and fails (§8.5).
13. The prompts embed changing user direction ("Current user blocker", "Current user acceptance") in the supervisor code (`LOOP:797-816`, `912`), so policy changes require editing the script.
14. The scene list is defined about 10 times (§6.5); the assessor prompts differ by transport (§7.2).
15. The optional agent roles referenced by the matrices do not exist; `formic-visual-assessor.toml` contradicts the default gate.
16. `test-mod` treats any log line containing `Exception` or `ERROR` as a failure, and scans the root `logs/latest.log`, which can be stale (`scripts/test-mod.ps1:43-80`).

### 9.3 Repository hygiene and portability

- `gradle.properties:1` sets `-Djdk.net.hosts.file=/mnt/c/Users/user/Documents/Codex/2026-04-26/new-chat/.local/hosts.gradle` (a WSL-style path; `.local/` is gitignored and absent). This has been present since the first harness commit. If the file does not exist, JDK name resolution for the Gradle daemon is redirected to a missing hosts file. The effect is unverified; it plausibly breaks dependency downloads on a fresh machine.
- Committed binaries and junk: `scripts/__pycache__/zai-codex-proxy.cpython-312.pyc`, `tools/__pycache__/*.pyc`, `src/main/resources/assets/formic_frontier/textures/block.zip` (added in `cc9db89`), and the `.gitignore` line `/II, data*` (`.gitignore:14`). `__pycache__/` is not ignored.
- `.gitattributes` does not cover `*.ps1`, `*.cmd` or `*.py` (`.gitattributes:1-14`); the `.cmd` wrappers are stored with LF line endings.
- The manual playtest checklist is unmaintained: step "20." appears four times (`docs/manual-playtest.md:22-25`).
- The debug and QA command ships in the main jar without permission checks. `/formic qa scene` wipes all colonies (`VQS:350`) and flattens terrain (`VQS:344`) in whatever world it runs in (`src/main/java/com/formicfrontier/command/FormicCommands.java:24-25,69-73`).

### 9.4 Half-finished work visible in tests and harness

- **Construction, damage and repair visuals.** Only single-block markers exist, enforced by tests (C1, J1, J4). Upgrades have no visual difference from complete buildings.
- **Texture migration:** the `holey_block_texture_redraw` and `formic_textures_32x32` rows stayed open. Only item textures are validated at 32×32.
- **Tablet "beautiful and functional":** only the server entry points and snapshot are tested. Visual acceptance relied on manual screenshots, and there are no client tests.
- **Structure-scope screenshots** (45 scenes) have no gate.
- **The mechanics phase-switch row** and the `playability_mechanics_ready` phase were never reached. Per the brief logic, that would require every required visual row to be `pass`.
- **Planned but never built:** GitHub publishing, Docker/WSL "future isolated runners" (`scripts/doctor.ps1:52-56`), a seeded reproducible QA world, and integration tests of the live tick.

---

## 10. Reusable for the rebuild

### Port (largely as is)

- **Blueprint-data JUnit invariants** (`OBT`, `TMT`, `SVT`): 6-neighbour BFS connectivity, "carved at head height", stair headroom per step, rear-shell and "enclosed except authored mouths" checks, chamber-overlap checks for connected rooms, footprint-distinct variants, deterministic per-site variant selection, and minimum site spacing. These are fast and meaningful, and they test intent rather than coordinates.
- **Economy tests with deterministic fixtures and exact per-tick deltas**, in the style of `resourceTickProducesAndConsumesOverMultipleTicksAssertingRiseAndFall` (`CET:26-81`), plus the codec round trip and the UI-snapshot structure tests.
- **`tools/validate_assets.py`**: JSON validity, lang key parity, mojibake scan, model and texture reference resolution, texture size contracts. Extend it to `multipart` blockstates and block textures.
- **The rubric and report skeleton**: the P0–P3 definitions, the verdict rule, the evidence standard (Scene, Evidence, Impact, Fix, Acceptance), Reference Diff, a 0–5 scorecard, and the `REPEAT:` anti-local-minimum rule.
- **Freshness markers** (artifacts must be newer than the iteration marker) and **per-attempt git checkpoints** (before and after status, diff and hashes).

### Port with changes

- **The visual QA command plus client capture driver.** Keep the tick-driven client loop, pinned origin, fixed time and weather, spectator camera, `hideGui`, per-scene capture delays, per-scene language switching, and summary JSON. Change the following:
  - move it to a dev-only source set or companion mod;
  - require operator permission and never call `clearColonies()` on a real save;
  - have the server acknowledge each scene (success or failure) to the client, and fail the run on any failed scene;
  - define the scene list once, for example an enum exported to JSON for the Python tools and the gate;
  - stage no decoration or actors that gameplay cannot produce, or keep them in clearly separate "beauty" scenes that are not used for acceptance;
  - use a seeded world;
  - detect blank frames from image content (variance or histogram), not file size;
  - restore `options.txt` afterwards.
- **Gametests.** Keep flatten → create colony → assert, but assert **semantic** properties by reusing the blueprint analysis on the placed world (connected mass, chamber count, open mouths, stair headroom) instead of furniture coordinates. Add multi-tick gametests (`succeedWhen`) for AI and construction; tests that drive `tickEconomy()`/`tickWorld()`; per-test isolated colony state; and terrain variety (slope, water, trees).
- **Content matrix with an "evidenceTest must be named" rule.** Version the matrix in git, and have the gate parse JUnit and gametest XML to confirm the named test exists and passed. Say "test" rather than "gametest" when a unit test is appropriate.
- **A small orchestrator**, if autonomy is wanted again:
  - check every step's exit code;
  - keep gates that do not depend on anything the agent wrote about itself: tests, asset validation, screenshot presence and freshness, plus an **independent** image-capable reviewer run as a separate process with its own credentials, whose report the worker cannot edit;
  - run in an isolated git worktree or container instead of `danger-full-access` with post hoc regex guards;
  - commit on green to a branch;
  - keep changing user direction in a versioned brief file, not in script literals.

### Drop

- The self-graded "manual" assessor line and agent-edited matrix statuses used as a gate.
- The 1371-line PowerShell supervisor as written, including its hard-coded prompt policy, hidden-window process management and unlimited rate-limit retries.
- The Z.AI Responses proxy, unless it is fixed to enforce the token, forward images or declare the model unfit for visual work, and forward all tool types.
- Production gametests that assert QA scene ids or QA staging, and tests that enforce placeholder single-block markers for building stages.
- The CI `visual-qa-report --ci-manifest-only` no-op job, the `gradle.properties` hosts-file flag, the committed `.pyc` and `block.zip` files, and the stale agent TOMLs.

---

## 11. Timeline

| When (local, UTC+5) | Commits | What happened |
|---|---|---|
| 2026-04-29 20:38–20:44 | `10de828`, `9ed2fa7`, `b940586` | Remote-side root: "Initial commit", "Add Prime Ants mod project" (already 11 gametests and 11 JUnit tests), and the gradlew executable bit. |
| 2026-04-29 21:53 | reflog `79e18c0` | Local initial commit "Initial Formic Frontier autonomous QA harness", later rewritten. |
| 2026-05-01 10:49–12:18 | `a74a7fa`, `b9f9faa`, `e45a5bd` | QA harness v1: doctor, test-mod, world prep, gui-smoke, client and scenes with 6 scenes, CI. Then "Renovate colony scale and tablet UI": supervisor v1, gate v1, the skill and rubric, 19 scenes, 48 gametests and 25 JUnit tests. Branch renames, a merge of `origin/master` (gradlew conflict), and a history rewrite at 12:17 (two commits share the timestamp 12:17:43). |
| 2026-06-22 22:18 | reflog | `reset: moving to HEAD`. |
| 2026-06-22 | — | The user provided three reference images (`docs/visual-intent/reference-manifest.json:4`); local-stack digests are dated this day. |
| 2026-06-23 02:58–04:52 | `15b5932`, `7af4b42`, `59fba60`, `4bd779b`, `556111f`, `33c1659` | R2 architecture loop with GLM-5.2 via the proxy and GPT-5.4 mini as assessor. "Checkpoint … (attempt 6)" bundles pipeline v2 (agents, intent pack, loop rewrite, proxy, assessors, gate). "R2 retry-07" is the 8th attempt of an iteration. |
| 2026-06-23 | — | User directive: forest floor and material palette accepted (matrix rows set to `pass`). |
| 2026-06-27 | — | "Latest player feedback": roofless boxes, a bad tiled texture, non-functional tablet (`docs/visual-intent/formic-visual-intent.md:48`). |
| 2026-06-28 06:47 | `cc9db89` (Opus 4.8 co-author) | "Stabilize autonomous loop": proxy heartbeat, rate-limit policy, the content track and content gate, 10 content rows backed by tests (+737 JUnit lines), and a one-mound landmass. |
| 2026-06-28 16:31–17:25 | `f5e9857`, `c3a7aeb` (Opus 4.8) | A schematic queen and food store (`FormicSchematic`, `gen-food-variants.py`; both deleted later) and a from-scratch tablet UI rebuild ("all 50 gametests pass"; the actual count was 49). |
| 2026-07-11 13:00 | `946ea44` | `CasteBalancer` plus landmass work. This is the last commit on master; local master is 1 ahead of `origin/master` at `c3a7aeb`. |
| 2026-07-11 13:00–13:15 | branch, `6fb5bcd` | `rebuild/anthills-from-scratch` created. All anthill structures reset to minimal markers; the old structure tests deleted. |
| 2026-07-11 23:16 → 07-12 19:14 | `87d8192` … `f4b4e04` (16 commits) | One blueprint per commit, each with its JUnit test, gametest profile and focused QA scenes: queen stage 1, food store, nursery, mine and chitin farm, barracks, market, archive, armory, shrine, resin depot, fungus garden, venom press, watch post, Great Mound, Queen Vault, Trade Hub. Gametests went from 49 to 59, JUnit from 37 to 56, scene constants from 24 to 64. |
| 2026-07-12 21:36–21:38 | `6b1c0b3`, `88b040f` | Tablet redesign. "Harden manual visual QA for tall mounds": the manual assessor becomes the default, ground Y pinned, crown validation added. |
| 2026-07-13 00:09–00:10 | `06d1005`, `75a70f7` | Tablet "glass" polish, then redesign of ant castes and equipment art with texture size contracts in `validate_assets.py`. **Last commit.** |

---

## Appendix A. In-scope files and line counts

- Tests:
  - `src/gametest/java/com/formicfrontier/test/FormicFrontierGameTest.java` (3085)
  - `src/gametest/resources/fabric.mod.json` (12)
  - `src/test/java/com/formicfrontier/sim/ColonyEconomyTest.java` (1318)
  - `…/world/structure/OrganicBuildingBlueprintTest.java` (842)
  - `…/TieredMoundBlueprintTest.java` (161)
  - `…/SubterraneanVaultBlueprintTest.java` (126)
- QA:
  - `src/main/java/com/formicfrontier/qa/VisualQaScenes.java` (1924)
  - `src/client/java/com/formicfrontier/client/VisualQaClient.java` (344)
- Scripts:
  - `autonomous-loop.ps1` (1371), `autonomous-gate.ps1` (158), `start-autonomous-loop.ps1` (112), `stop-autonomous-loop.ps1` (41), `content-gate.ps1` (32)
  - `doctor.ps1` (68), `java-env.ps1` (133), `test-mod.ps1` (114), `gui-smoke.ps1` (148), `prepare-gui-world.ps1` (136), `visual-loop-brief.ps1` (330)
  - `openai-visual-assessment.ps1` (267), `glm5v-visual-assessment.ps1` (39)
  - `zai-codex-proxy.ps1` (29), `zai-codex-proxy.py` (693)
  - 13 two-line `.cmd` wrappers
- Tools: `visual_assessment_gate.py` (450), `openai_visual_assessment.py` (387), `glm5v_visual_assessment.py` (366), `content_feature_gate.py` (107), `visual_qa_report.py` (183), `validate_assets.py` (136); also `generate_formic_textures.py` (520) and `render_texture_contact_sheet.py` (36).
- Agents and skill: 7 TOMLs (14–18 lines each), `SKILL.md` (84), `rubric.md` (96), `report-template.md` (147), `agents/openai.yaml` (4).
- Docs: `autonomous-dev.md` (247), `local-stack/*.md` (37 + 54 + 54 + 44), `manual-playtest.md` (31), the content matrix template (117), the visual matrix template (190).
- Build and CI: `build.gradle` (116), `.github/workflows/build.yaml` (52), `gradle.properties` (11).

## Appendix B. Environment variables and external endpoints (no values in the repo)

- **Environment variables:**
  - `ZAI_API_KEY`: required by the proxy and GLM-5V (`scripts/zai-codex-proxy.ps1:14`, `tools/glm5v_visual_assessment.py:300`).
  - `ZAI_CODEX_PROXY_TOKEN`: a GUID generated by the launcher or supervisor; not enforced by the proxy.
  - `OPENAI_API_KEY`: API transport only.
  - Optional overrides: `OPENAI_VISION_MODEL`, `OPENAI_VISION_ASSESSOR`, `OPENAI_RESPONSES_ENDPOINT`, `ZAI_VISION_MODEL`, `ZAI_VISION_ENDPOINT`, `ZAI_GLM5V_ENDPOINT`, `CODEX_HOME`, `JAVA_HOME`.
  - JVM properties: `-DpythonExecutable`, and `formic.visualQa*`.
- **External configuration:** the user-level `C:\Users\user\.codex\zai-glm52.config.toml` (not in the repo).
- **Endpoints:**
  - `https://api.z.ai/api/coding/paas/v4/chat/completions` (GLM-5.2 through the proxy; GLM-5V fallback)
  - `https://api.z.ai/api/paas/v4/chat/completions` (GLM-5V primary)
  - `https://api.openai.com/v1/responses` (OpenAI API transport)
  - `http://127.0.0.1:11452/v1` (local proxy)
