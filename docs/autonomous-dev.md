# Autonomous Development Loop

Formic Frontier now has a two-layer QA loop for Codex-driven work:

1. `scripts/doctor.cmd` checks local readiness: Java 21+, Gradle wrapper, Git repo, and GitHub auth.
2. `scripts/test-mod.cmd` runs the build, unit tests, server GameTests, asset validation, and log scan.
3. `scripts/prepare-gui-world.cmd` bootstraps a dev single-player world if it is missing.
4. `scripts/gui-smoke.cmd` launches the Fabric client with `-Dformic.visualQa=true`, prepares deterministic visual QA scenes, captures screenshots, and writes `build/visual-qa/visual-qa-report.md`.

## Required Local Setup

- Install Temurin JDK 21 or 25 and make `java -version` report 21+.
- Initialize this folder as a Git repository and connect it to GitHub.
- Authenticate GitHub CLI or the GitHub connector before asking agents to publish branches or PRs.
- For GUI QA, keep the Windows session unlocked. `scripts/gui-smoke.cmd` prepares a `FormicVisualQA` quick-play world automatically when missing.

## Visual QA Scenes

The server command is:

```text
/formic qa scene <name>
```

Scenes:

- `colony_overview`
- `colony_ground`
- `ant_lineup`
- `tablet_en`
- `tablet_ru`
- `progression_scene`

Each scene flattens a test area, snaps the colony to the surface, sets time/weather, positions the player camera, and opens the tablet for tablet scenes.

## Agent Gates

Agents should treat a change as ready only when:

- `scripts/test-mod.cmd` passes.
- `scripts/gui-smoke.cmd` produces all expected screenshots.
- The GUI tester agent confirms no obvious unreadable UI, floating colony floors, invisible ants, broken labels, or visual clutter regressions.
