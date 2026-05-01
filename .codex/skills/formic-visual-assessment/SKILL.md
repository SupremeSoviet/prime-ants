---
name: formic-visual-assessment
description: Strict visual and usability QA for Formic Frontier Minecraft mod GUI smoke artifacts. Use when Codex needs to review build/visual-qa screenshots, assess UI readability, ant and colony visual quality, ground anchoring, localization, visual noise, Minecraft fit, or produce a prioritized screenshot-based fix report after scripts/gui-smoke.cmd.
---

# Formic Visual Assessment

## Overview

Use this skill to act as a strict GUI tester for Formic Frontier. The goal is to find playability blockers and visual defects from actual QA artifacts, not to give vague aesthetic opinions.

Be harsh, but every criticism must be evidence-based and useful to a dev agent.

## Workflow

1. Locate the latest visual QA artifacts in the current repo:
   - `build/visual-qa/visual-qa-report.md`
   - `build/visual-qa/visual-qa-report.json`
   - `build/visual-qa/screenshots/*.png`
   - `build/visual-qa/latest.log` when present
2. If artifacts are missing, report that the assessment is blocked and ask for or run the existing `scripts/gui-smoke.cmd` only when the user requested a full local GUI assessment.
3. Read the report/logs first. Treat missing screenshots, blank screenshots, crashes, resource errors, missing models, missing textures, or incomplete scenes as `P0`.
4. Inspect every expected screenshot at original resolution. Do not judge from thumbnails only.
5. Use `references/rubric.md` for the assessment lenses and severity rules.
6. Use `references/report-template.md` as the output shape unless the user requested another format.
7. Lead with blockers. Put praise only after blockers, and only if it helps preserve a good direction.

## Expected Scenes

Evaluate these scenes in this order:

- `colony_overview.png`: colony composition, readable layout, grounded floor, buildings/resources visible, no empty or misleading camera.
- `colony_ground.png`: playable eye-level view, no floating colony parts, no overhead clutter, ants/resources readable from normal gameplay distance.
- `ant_lineup.png`: caste silhouettes, scale, orientation, texture clarity, distinct roles, no tiny/unreadable or broken models.
- `work_cycle.png`: worker/resource/construction/patrol jobs readable without relying on debug text, work markers visible but not noisy.
- `tablet_en.png`: English tablet readability, layout, clipping, hierarchy, labels, controls, overflow, contrast.
- `tablet_ru.png`: Russian tablet readability, localization length handling, mojibake, clipping, wrapping, tab state.
- `tablet_guide.png`: guide chapter readability, unlock state clarity, coverage of castes/resources/buildings/cultures/relations/research/help basics.
- `tablet_research_map.png`: research must read as a node map with clear unlocked/active/locked states, not a flat ledger.
- `tablet_market.png`: trade must read as a market with item/resource icons and understandable exchange cards, not terse token buttons.
- `tablet_requests.png`: colony requests must read as player-facing help cards with needed resource, destination, reward, and priority.
- `progression_scene.png`: progression fantasy, visual reward, resource/building readability, clutter, gameplay legibility.
- `settlement_scale.png`: settlement must visibly occupy the larger village-scale footprint with broad trails, roomy mounds, and no empty stretched layout.
- `construction_stage.png`: planned, construction, complete, upgraded, damaged, and repairing structures are visually distinct.
- `repair_scene.png`: damaged, repairing, and restored structures plus repair workers/materials are readable as one loop.
- `culture_styles.png`: culture material differences are legible in one frame without relying only on labels.
- `diplomacy_scene.png`: allied and rival colonies, relation/raid route, damaged or threatened target, and non-player interaction markers are readable.

## Verdict Rules

- `FAIL`: any `P0` or `P1` issue.
- `PASS WITH NOTES`: only `P2` or `P3` issues remain.
- `PASS`: no material visual or usability issues found.

Severity:

- `P0`: broken/blank/crash/unusable, missing critical artifact, missing texture/model, scene did not render.
- `P1`: blocks readability/playability or makes a core feature look broken.
- `P2`: obvious quality debt that harms trust, polish, or comprehension.
- `P3`: polish nit that is worth fixing after larger problems.

## Report Discipline

For every finding include:

- screenshot or artifact path;
- visible evidence;
- player impact;
- likely fix direction;
- concrete acceptance check.

Do not invent unseen behavior. If a claim is an inference from a screenshot, label it as an inference. Avoid generic instructions like "make it better"; specify what should change and how the next screenshot should prove it.

## Reference Map

- `references/rubric.md`: research-backed checklist and Formic-specific lenses.
- `references/report-template.md`: strict report format for dev-agent handoff.
