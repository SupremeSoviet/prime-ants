# Formic Visual Assessment Report Template

Use this format for strict screenshot QA reports.

```markdown
# Formic Visual Assessment

Verdict: FAIL | PASS WITH NOTES | PASS
Artifacts:
- Report: build/visual-qa/visual-qa-report.md
- Screenshots: build/visual-qa/screenshots
- Logs: build/visual-qa/latest.log (present/missing)

## Reference Diff

Compare the current wide colony shots (colony_overview, settlement_scale,
culture_styles, endgame_project) directly against the attached reference images
(reference-mega-nest-wide, reference-mega-nest-front). One line each:

- Topology: ONE broad continuous chambered mound mass, or N separate
  buildings/cones? Name which the current shots read as.
- Silhouette: broad dome (wider than tall) vs steep pointed cone/pyramid/ziggurat.
- Chambers: irregular noise-scattered mouths with dark depth vs regular
  grid/pegboard holes vs none readable.
- Surface: organic/jittered with debris vs clean block stair-steps.
- Biggest single gap from reference, named concretely.

If the gap is topological (wrong overall form, e.g. a cluster of cones instead
of one carved mound), say so explicitly: the next fix must be a representational
change, not a parameter tweak.

## Blockers

1. [P0/P1] Short issue title
   Scene: build/visual-qa/screenshots/<file>.png
   Evidence: Specific visible evidence from the screenshot/log.
   Impact: What player understanding, readability, or playability is blocked.
   Fix: Concrete direction for the dev agent.
   Acceptance: Exact condition the next screenshot/log must satisfy.

## Scene Findings

### colony_overview
- Verdict: PASS | FAIL | NEEDS WORK
- Findings:
  - [P?] ...

### colony_ground
- Verdict: PASS | FAIL | NEEDS WORK
- Findings:
  - [P?] ...

### ant_lineup
- Verdict: PASS | FAIL | NEEDS WORK
- Findings:
  - [P?] ...

### work_cycle
- Verdict: PASS | FAIL | NEEDS WORK
- Findings:
  - [P?] ...

### tablet_en
- Verdict: PASS | FAIL | NEEDS WORK
- Findings:
  - [P?] ...

### tablet_ru
- Verdict: PASS | FAIL | NEEDS WORK
- Findings:
  - [P?] ...

### tablet_guide
- Verdict: PASS | FAIL | NEEDS WORK
- Findings:
  - [P?] ...

### progression_scene
- Verdict: PASS | FAIL | NEEDS WORK
- Findings:
  - [P?] ...

### construction_stage
- Verdict: PASS | FAIL | NEEDS WORK
- Findings:
  - [P?] ...

### repair_scene
- Verdict: PASS | FAIL | NEEDS WORK
- Findings:
  - [P?] ...

### culture_styles
- Verdict: PASS | FAIL | NEEDS WORK
- Findings:
  - [P?] ...

### diplomacy_scene
- Verdict: PASS | FAIL | NEEDS WORK
- Findings:
  - [P?] ...

## Matrix Scorecard

One line per required visual_baseline matrix row:

`<row_id>: pass | partial | fail | unknown - score N/5 - <one concrete next instruction>`

- score 5 = matches the reference intent; 0 = wrong representation / not started.
- Use `unknown` only when the evidence scenes do not show enough to judge; then
  say which capture/camera is missing.
- If a row was already `fail` for the same root cause in its matrix `lastVerdict`
  on 2+ prior attempts, prefix the line with `REPEAT:` and make the instruction a
  representational change (different generator/algorithm/shape), not another
  parameter tweak of the failing approach.

## Prioritized Fix Backlog

1. P0/P1 fix first, with owner-facing implementation hint.
2. P2 quality fix.
3. P3 polish fix.

## Acceptance Checks For Dev Agent

- scripts/test-mod.cmd passes.
- scripts/gui-smoke.cmd produces all expected screenshots.
- No P0/P1 issues remain in the next visual assessment.
- Specific screenshot checks:
  - tablet_en/tablet_ru: no clipped or overlapping text.
  - tablet_guide: guide chapters fit and locked/unlocked states are understandable.
  - colony_ground: colony floor visibly sits on ground.
  - ant_lineup: all castes are distinguishable at captured resolution.
  - work_cycle: at least three jobs are visually distinguishable without debug text.
```

Rules:

- Keep the top verdict blunt.
- Put blockers before scene-by-scene notes.
- Do not include "nice to have" polish above blockers.
- Do not write a vague fix. If the fix cannot be named, write the next diagnostic step as the fix.
