---
name: eval-was-a-phantom
description: "Why the visual QA loop was optimizing a building the player never sees, and how it's being fixed"
metadata: 
  node_type: memory
  type: project
  originSessionId: f4152c93-af52-4383-a3ce-2526f6fd6f25
---

Root cause the user and I found behind "the mod looks bad in actual gameplay": the visual-QA eval rendered a PHANTOM. The QA scenes seed via `VisualQaScenes.seedVisualState`, which sets `StructurePlacer.SUPPRESS_SATELLITE_CROWN_MASS=true` and calls QA-only overlays (`placeSharedCampusLandmass2D`, `carveSharedMoundChamberMouths`) that real gameplay `ColonyService.createColony` NEVER runs. So GLM-5.2 in the autonomous loop was optimizing screenshots that diverged from what a player actually sees.

The fix direction (user-approved): make eval == game, per-building. The new `structure_preview_3q/front` scenes render through the REAL `StructurePlacer.placeBuilding` path (no seed, no QA overlay), so the preview is exactly the in-game building. As each building is converted to the [[schematic-building-system]], its preview/eval reflects real gameplay. Remaining QA scenes that still use `seedVisualState` overlays are legacy and should be migrated/retired as the rollout proceeds.

Practical loop note: iteration is cheap — `gradlew build` runs the 50 gametests in ~30-55s incrementally; `scripts/gui-smoke.ps1` renders the preview scenes (it exits 1 with bogus "missing screenshot" warnings because it checks an old 19-screenshot list — NOT a real failure; check the actual PNGs' mtime/size in build/visual-qa/screenshots/). The client capture list lives in `VisualQaClient.java` SCENES (currently reduced to the 2 previews for fast iteration — RESTORE the full 19 before any real visual-baseline run).
