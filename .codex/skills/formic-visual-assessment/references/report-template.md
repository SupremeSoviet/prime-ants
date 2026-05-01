# Formic Visual Assessment Report Template

Use this format for strict screenshot QA reports.

```markdown
# Formic Visual Assessment

Verdict: FAIL | PASS WITH NOTES | PASS
Artifacts:
- Report: build/visual-qa/visual-qa-report.md
- Screenshots: build/visual-qa/screenshots
- Logs: build/visual-qa/latest.log (present/missing)

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
