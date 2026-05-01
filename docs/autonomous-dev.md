# Autonomous Development Loop

Formic Frontier now has a two-layer QA loop for Codex-driven work:

1. `scripts/doctor.cmd` checks local readiness: Java 21+, Gradle wrapper, Git repo, and GitHub auth.
2. `scripts/test-mod.cmd` runs the build, unit tests, server GameTests, asset validation, and log scan.
3. `scripts/prepare-gui-world.cmd` bootstraps a dev single-player world if it is missing.
4. `scripts/gui-smoke.cmd` launches the Fabric client with `-Dformic.visualQa=true`, prepares deterministic visual QA scenes, captures screenshots, and writes `build/visual-qa/visual-qa-report.md`.
5. `scripts/autonomous-gate.cmd -AllowMissingGitHub` runs the local gate and requires a fresh `build/visual-qa/formic-visual-assessment.md` with `Verdict: PASS` or `Verdict: PASS WITH NOTES`.
6. `scripts/start-autonomous-loop.cmd -AllowMissingGitHub` starts a hidden local Codex supervisor that runs one playable roadmap slice per iteration and re-checks `autonomous-gate.cmd -AllowMissingGitHub -NoLaunch` after each iteration.

When `docs/roadmap.md` contains an active Renovation Track, autonomous agents
must finish that focused track before returning to the generic Stage 1-7
roadmap. The current order is R1 settlement scale, R2 architecture polish, then
R3 Colony Tablet 2.0.

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
- `$formic-visual-assessment` is run on the latest screenshots and saved to `build/visual-qa/formic-visual-assessment.md`.
- `scripts/autonomous-gate.cmd -AllowMissingGitHub -NoLaunch` accepts the saved assessment report.
- The GUI tester agent confirms no `P0`/`P1` issues: no unreadable UI, floating colony floors, invisible ants, broken labels, mojibake, or visual clutter regressions.

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
run the visual assessment skill, and enforce the local gate. It cannot publish
pull requests until a Git remote and GitHub tooling are configured.
