# Autonomous Development Loop

Formic Frontier now has a two-layer QA loop for Codex-driven work:

1. `scripts/doctor.cmd` checks local readiness: Java 21+, Gradle wrapper, Git repo, and GitHub auth.
2. `scripts/test-mod.cmd` runs the build, unit tests, server GameTests, asset validation, and log scan.
3. `scripts/prepare-gui-world.cmd` bootstraps a dev single-player world if it is missing.
4. `scripts/gui-smoke.cmd` launches the Fabric client with `-Dformic.visualQa=true`, prepares deterministic visual QA scenes, captures screenshots, and writes `build/visual-qa/visual-qa-report.md`.
5. `scripts/openai-visual-assessment.cmd` sends fresh screenshots to OpenAI `gpt-5.4-mini` and writes `build/visual-qa/formic-visual-assessment.md`.
6. `scripts/autonomous-gate.cmd -AllowMissingGitHub` runs the local gate and requires a fresh `build/visual-qa/formic-visual-assessment.md` with `Assessor: GPT-5.4 mini` and `Verdict: PASS` or `Verdict: PASS WITH NOTES`.
7. `scripts/start-autonomous-loop.cmd -AllowMissingGitHub` starts a hidden local Codex supervisor that runs one playable roadmap slice per iteration and re-checks `autonomous-gate.cmd -AllowMissingGitHub -NoLaunch` after each iteration.

When `docs/roadmap.md` contains an active Renovation Track, autonomous agents
must finish that focused track before returning to the generic Stage 1-7
roadmap. The current order is R1 settlement scale, R2 architecture polish, then
R3 Colony Tablet 2.0.

R2 architecture polish is a structural scale pass, not a decorative pass. Agents
should be willing to spend visual compute on much larger ant-hill forms:
complete landmark mounds and important buildings should usually reach 20-30
blocks of height with broad bases, layered mound shells, tunnel mouths,
vertical shafts, ribs, yards, terraces, and role-specific crowns or chambers.
Tiny 3-5 block huts with a few accent blocks do not satisfy R2 if screenshots
still read as arcade props. QA camera framing and prepared terrain radius may be
expanded so the larger structures can be assessed.

## Visual Intent Pack

`docs/visual-intent/formic-visual-intent.md` is the art-direction source of
truth for the visual baseline. It describes the desired family resemblance to
the user's references: a huge organic earthen ant-hill, multiple chambers and
tunnel mouths, realistic insect silhouettes, dense forest-floor life, and
screenshots that show both base and peak. The reference PNG slots are listed in
`docs/visual-intent/reference-manifest.json`; if those PNGs are present, the
OpenAI assessor attaches them before the Minecraft screenshots. If they are
missing, the textual intent still controls the pass bar.

`scripts/visual-loop-brief.ps1` refreshes the runtime handoff before each
autonomous turn:

- `build/autonomous-loop/visual-feature-matrix.json`
- `build/autonomous-loop/visual-loop-brief.md`
- `build/autonomous-loop/visual-loop-brief.json`
- `build/autonomous-loop/visual-progress.jsonl`

Visual baseline remains active until every required matrix row is `pass`.
Mechanics and playability are locked until the matrix and gate agree that the
visual baseline is complete. A single tall mound is not enough: required rows
demand multiple large organic chambers, readable tunnel mouths, realistic ants,
complete camera framing, organic/asymmetrical ant-like buildings, and readable
spacing between buildings. Current user retargeting accepts forest-floor density
and the broad material palette for now. Do not select those as the next repair
target unless the user reopens them. The remaining material task is narrow:
redraw the visible block with a hole at 32x32 so it no longer reads as
honeycomb or an unrelated placeholder. The asset baseline also requires custom
Formic block/item textures to be upgraded from 16x16 to 32x32 before mechanics
or playability work can start.

The supervisor also has a Codex child watchdog. If a child has written its final
summary, the JSON log contains `turn.completed`, and the log stops growing for
the configured idle window, the supervisor terminates that child and gates the
produced artifacts. The Z.AI proxy is not a watchdog target.

## GLM 5.2 Codex Harness

The local Codex CLI currently requires custom providers to expose the Responses
wire API. Z.AI GLM 5.2 Coding Plan is available through a Chat Completions
endpoint, so this repo uses a local compatibility proxy:

```powershell
$env:ZAI_API_KEY = "<secret>"
$env:ZAI_CODEX_PROXY_TOKEN = [guid]::NewGuid().ToString("N")
scripts\zai-codex-proxy.cmd
codex exec -p zai-glm52 --sandbox read-only "Return READY only."
```

The profile lives at `C:\Users\user\.codex\zai-glm52.config.toml` and points
Codex at `http://127.0.0.1:11452/v1`. `ZAI_CODEX_PROXY_TOKEN` is a temporary
local bearer token for the localhost proxy; it prevents Codex from sending any
other provider auth token to the proxy. The Z.AI API key must stay in the
environment only; do not put it in repo files, prompts, or logs.

The autonomous supervisor can start the default proxy automatically when the
`zai-glm52` profile is selected:

```powershell
scripts\start-autonomous-loop.cmd -AllowMissingGitHub -CodexProfile zai-glm52 -MaxIterations 1
```

The supervisor auto-generates `ZAI_CODEX_PROXY_TOKEN` when it is missing.

Optional knobs:

- `-CodexCommand <path-or-name>` uses a specific Codex binary.
- `-ProxyCommand <powershell-command>` starts a custom proxy instead of the
  bundled `scripts\zai-codex-proxy.ps1`.
- `scripts\stop-autonomous-loop.cmd` requests supervisor shutdown and leaves the
  bundled proxy running by default. Use `scripts\stop-autonomous-loop.cmd -StopProxy`
  only when the local Z.AI proxy should be stopped too.

Proxy diagnostics live in `build/zai-codex-proxy/`, which is ignored by Git.

## GPT-5.4 Mini Visual Assessment

Codex development still uses `glm-5.2` through the local Responses proxy because
that model is strong for long-running text/code work. Final visual acceptance is
separate: `scripts/openai-visual-assessment.cmd` calls OpenAI `gpt-5.4-mini`
through the Responses API with the actual `build/visual-qa/screenshots/*.png`
images.

This split is intentional. `glm-5.2` is text-only in Z.AI's model metadata, so it
must not be trusted to visually accept Minecraft screenshots from pixel summaries
alone. `gpt-5.4-mini` is the checker for silhouette, scale, camera framing, UI
readability, ant readability, clipping, and composition.

The assessor uses Codex CLI auth by default (`-Transport codex`), so it does not
need an `OPENAI_API_KEY` for the normal autonomous loop. When explicitly run with
`-Transport api`, it uses `OPENAI_API_KEY` from the environment only and does not
store the key in the repo, prompts, or logs. The API endpoint for that transport is:

```text
https://api.openai.com/v1/responses
```

The OpenAI model id is `gpt-5.4-mini`. It supports image input; the local script
uses `detail=high` for 1600x900 Minecraft screenshots.

By default, `scripts/openai-visual-assessment.cmd` uses Codex CLI auth
(`-Transport codex`), so autonomous agents must not block merely because
`OPENAI_API_KEY` is unset. `OPENAI_API_KEY` is needed only when explicitly using
`-Transport api`. Because assessment sends 19 screenshots plus reference
images, it can take longer than five minutes; a command timeout should be
retried with more time or checked for background output before diagnosing auth.

Manual run:

```powershell
scripts\gui-smoke.cmd
scripts\openai-visual-assessment.cmd
scripts\autonomous-gate.cmd -AllowMissingGitHub -NoLaunch
```

`autonomous-gate.cmd` runs the OpenAI visual assessor by default after fresh GUI
smoke. Use `-SkipVisionAssessment` only for offline debugging; autonomous visual
loop acceptance must not use that bypass. `-VisionAssessor glm5v` is still
available if Z.AI GLM-5V access is restored later.

## Required Local Setup

- Install Temurin JDK 21 or 25 and make `java -version` report 21+.
- If Java 8 appears first on `PATH`, the scripts auto-select a compatible JDK from common Windows install locations such as `C:\Program Files\Eclipse Adoptium`.
- Initialize this folder as a Git repository and connect it to GitHub.
- Authenticate GitHub CLI or the GitHub connector before asking agents to publish branches or PRs.
- For GUI QA, keep the Windows session unlocked. `scripts/gui-smoke.cmd` prepares a `FormicVisualQA` quick-play world automatically when missing.
- Install and authenticate Codex CLI for background autonomous work. The local runner uses `codex exec` and writes logs to `build/autonomous-loop`.

## Visual QA Scenes

The server command is:

```text
/formic qa scene <name>
```

Scenes:

- `colony_overview`
- `colony_ground`
- `ant_lineup`
- `work_cycle`
- `tablet_en`
- `tablet_ru`
- `tablet_guide`
- `tablet_trade`
- `tablet_research_map`
- `tablet_market`
- `tablet_requests`
- `progression_scene`
- `settlement_scale`
- `construction_stage`
- `repair_scene`
- `culture_styles`
- `diplomacy_scene`
- `worldgen_encounter`
- `endgame_project`

Each scene flattens a test area, snaps the colony to the surface, sets time/weather, positions the player camera, and opens the tablet for tablet scenes.

## Agent Gates

Agents should treat a change as ready only when:

- `scripts/test-mod.cmd` passes.
- `scripts/gui-smoke.cmd` produces all expected screenshots.
- `scripts/openai-visual-assessment.cmd` is run on the latest screenshots and saves `build/visual-qa/formic-visual-assessment.md` with `Assessor: GPT-5.4 mini`.
- `scripts/autonomous-gate.cmd -AllowMissingGitHub -NoLaunch` accepts the saved assessment report.
- The GUI tester agent confirms no `P0`/`P1` issues: no unreadable UI, floating colony floors, invisible ants, broken labels, mojibake, or visual clutter regressions.

## Subagent Visual Loop

Visual-heavy work should spend compute in scoped loops instead of broad edits.
The default project subagents are:

- `formic-slice-scout`: read-only visual target and acceptance brief.
- `formic-world-worker`: settlement, architecture, construction, repair, culture.
- `formic-tablet-worker`: Colony Tablet UI, localization, research, market, requests.
- `formic-ant-worker`: ant model, renderer, caste silhouettes, textures.
- `formic-asset-worker`: item/block/resource texture and model polish.
- `formic-visual-assessor`: checker that may write only `build/visual-qa/formic-visual-assessment.md`.
- `formic-gatekeeper`: final artifact freshness and command gate.

The maker/checker split is mandatory: a worker that changes code must not be the
agent that accepts the visual result. Each loop should follow:

1. scout chooses one visual target and screenshot acceptance set;
2. one worker owns one subsystem and write area;
3. fresh `scripts/gui-smoke.cmd` screenshots are produced;
4. visual assessor runs `scripts/openai-visual-assessment.cmd` and writes `build/visual-qa/formic-visual-assessment.md`;
5. gatekeeper verifies `test-mod`, assessment freshness, and `autonomous-gate`.

For R2 architecture loops, the scout must prefer one substantial structural
target over cosmetic accents: for example, a 20-30 block queen mound, a tall
fungus tower/chamber, a broad market mound with entrances and terraces, or a
large staged construction shell. The visual assessor should flag tiny hut-like
buildings as blocking or high-priority polish when the slice promised scale.
The active R2 blocker is building character: role buildings should be
organically asymmetrical and ant-like, not mirrored houses, temple pads, or
overlapping towers. Entrances should be irregular tunnel cuts with dark depth;
do not add or preserve a freestanding arch in front of an entrance.

The supervisor creates an iteration freshness marker before handing work to
Codex. The final gate rejects visual reports, screenshots, or assessments whose
mtime is not newer than that marker, so stale screenshot sets cannot pass by
only rerunning `visual_qa_report.py`.

Durable loop state lives at `build/autonomous-loop/visual-loop-state.json` and
is merge-updated so scout/assessor fields such as `acceptanceBrief`,
`screenshotVerdict`, `blockingSeverityCount`, and `polishBacklog` are not
clobbered by supervisor status updates.

## Background Loop

Start:

```text
scripts\start-autonomous-loop.cmd -AllowMissingGitHub
```

Stop gracefully:

```text
scripts\stop-autonomous-loop.cmd
```

Status and logs:

- `build/autonomous-loop/supervisor.pid`
- `build/autonomous-loop/run-state.json`
- `build/autonomous-loop/supervisor.out.log`
- `build/autonomous-loop/iteration-*.jsonl`
- `build/autonomous-loop/iteration-*.final.md`

The supervisor is local-only. It can edit files, run Gradle, launch GUI smoke,
run the GPT-5.4 mini visual assessment command, and enforce the local gate. It cannot publish
pull requests until a Git remote and GitHub tooling are configured.
