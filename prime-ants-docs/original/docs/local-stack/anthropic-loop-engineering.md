# Anthropic Loop Engineering Digest

Retrieval date: 2026-06-22.

Sources:

- https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents
- https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents
- https://www.anthropic.com/engineering/writing-tools-for-agents
- https://www.anthropic.com/engineering/claude-code-best-practices
- https://docs.anthropic.com/en/docs/claude-code/hooks-guide

This file is a structured digest for local agents. It avoids large verbatim
copies and turns the source ideas into project-specific harness rules.

## Durable Loop Shape

- Keep the goal externalized in files, not only in chat context.
- Use an append-only progress log for what happened, why it happened, evidence,
  and next action.
- Give workers a short current brief rather than the whole history.
- Make each iteration small enough that tests, screenshots, and critique can
  complete before the agent stops.
- Separate maker and checker roles so the same worker does not grade its own
  visual work.
- Prefer artifact checks over self-report. A screenshot, log, test summary, and
  gate result are stronger than a narrative claim.

## Context Engineering Rules

- Put stable requirements in docs and templates.
- Put changing state in runtime files under `build/autonomous-loop/`.
- Summarize long logs into a handoff brief before each run.
- Preserve unresolved requirements until evidence closes them.
- Explicitly name the active slice, owner, screenshots, verdict, blockers, and
  next target.

## Tool And Harness Rules

- Design tools with narrow, predictable inputs and outputs.
- Fail loudly when required artifacts are missing or stale.
- Do not allow a long-running worker to silently hang after it has already
  produced a final answer and completion event.
- Keep hooks/gates deterministic: parse reports, timestamps, and JSON state
  rather than relying on model confidence.

## Formic Application

- `visual-feature-matrix.json` is the durable requirement list.
- `visual-progress.jsonl` is the append-only memory.
- `visual-loop-brief.md` is the short context packet for GLM.
- `openai-visual-assessment` is the image-capable checker.
- `visual_assessment_gate.py` is the deterministic gate.
- Mechanics remain locked until required visual rows pass.
