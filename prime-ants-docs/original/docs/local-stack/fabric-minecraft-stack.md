# Fabric And Minecraft Stack Digest

Retrieval date: 2026-06-22.

Source:

- https://docs.fabricmc.net/develop

This is a local digest for GLM workers editing the Formic Frontier Fabric mod.

## Project Areas

- Java sources live under `src/main/java` and `src/client/java`.
- Client-only visual capture and screen code must stay in client source sets.
- Assets live under `src/main/resources/assets/formic_frontier/`.
- Localization lives in `lang/en_us.json` and `lang/ru_ru.json`.
- GameTest and test harness code lives under `src/gametest/java`.

## Visual QA Scenes

- Preserve existing `/formic qa scene <name>` scene names.
- Prefer changing camera staging when the current frame hides the visual target.
- Do not remove screenshots from the expected report just to pass a gate.
- If a structure grows to 20-30 blocks tall, update camera distance, target, and
  prepared terrain radius so both base and peak are visible.

## Build And Test Commands

- Use `scripts/test-mod.cmd -AllowMissingGitHub` for normal code validation.
- Use `scripts/gui-smoke.cmd` for Minecraft client screenshots.
- Use `scripts/autonomous-gate.cmd -AllowMissingGitHub -NoLaunch` for final
  local acceptance after screenshots and visual assessment exist.

## Asset Guidance

- Minecraft textures should remain readable at native resolution.
- Use resource-pack conventions for texture paths and model references.
- Prefer deterministic generated assets when using tools.
- Validate assets after touching textures, models, blockstates, or item models.
- Colony architecture should use native Formic Frontier materials. Do not make
  honey, apatite-like minerals, amethyst-like accents, or unrelated vanilla
  decorative blocks the primary structure palette. Add custom blocks/textures
  when the colony needs mound earth, tunnel wall, roots, resin, brood, fungus,
  or trail materials.
