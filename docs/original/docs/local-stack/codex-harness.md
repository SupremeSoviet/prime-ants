# Codex Harness Digest

Retrieval date: 2026-06-22.

Sources:

- https://developers.openai.com/codex/config-advanced
- https://developers.openai.com/codex/subagents
- https://developers.openai.com/codex/codex-manual.md

This file summarizes how the local autonomous loop should use Codex.

## Non-Interactive Runs

- Use `codex exec` for unattended work.
- Run in the repository root with an explicit `--cd`.
- Keep prompts as files under `build/autonomous-loop/` for replay and audit.
- Store output summaries as `*.final.md`.
- Store JSON event logs as `*.jsonl` and stderr as `*.err.log`.

## GLM 5.2 Provider

- The active GLM worker profile is `zai-glm52`.
- It uses a local compatibility proxy because the Z.AI Coding Plan endpoint is
  chat-completions shaped while current Codex custom providers expect Responses
  wire behavior.
- Secrets must stay in environment variables. Do not write API keys into repo
  files, config, prompts, logs, or docs.

## Subagent Pattern

- `formic-slice-scout`: inspect evidence and choose one narrow target.
- `formic-world-worker`: world/settlement/structure generation only.
- `formic-ant-worker`: ant model, texture, animation readability only.
- `formic-tablet-worker`: tablet UI only.
- `formic-asset-worker`: resource pack assets only.
- `formic-visual-assessor`: independent visual critique, no source edits.
- `formic-gatekeeper`: deterministic checks and artifact freshness.

The maker/checker split is mandatory. A maker can run tests, but final
acceptance must come from the assessor plus gatekeeper.

## Local Loop Contract

Every accepted visual iteration needs:

- `scripts/test-mod.cmd -AllowMissingGitHub`
- `scripts/gui-smoke.cmd`
- `scripts/openai-visual-assessment.cmd`
- `scripts/autonomous-gate.cmd -AllowMissingGitHub -NoLaunch`
- fresh screenshots newer than the iteration marker
- updated `visual-loop-state.json`
- appended `visual-progress.jsonl`
- refreshed `visual-loop-brief.md`
