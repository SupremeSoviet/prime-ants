# Local Stack Pack

This directory gives offline workers enough local context to keep Formic
Frontier visual loops moving without internet access. It is a digest pack, not
a wholesale mirror, unless a source clearly permits copying.

Retrieval date: 2026-06-22.

## Read Order For GLM

1. `docs/visual-intent/formic-visual-intent.md`
2. `build/autonomous-loop/visual-loop-brief.md`
3. `build/autonomous-loop/visual-feature-matrix.json`
4. `docs/local-stack/anthropic-loop-engineering.md`
5. `docs/local-stack/codex-harness.md`
6. `docs/local-stack/fabric-minecraft-stack.md`

## Source Manifest

- Anthropic engineering:
  - https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents
  - https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents
  - https://www.anthropic.com/engineering/writing-tools-for-agents
  - https://www.anthropic.com/engineering/claude-code-best-practices
  - https://docs.anthropic.com/en/docs/claude-code/hooks-guide
- OpenAI Codex:
  - https://developers.openai.com/codex/config-advanced
  - https://developers.openai.com/codex/subagents
  - https://developers.openai.com/codex/codex-manual.md
- Fabric:
  - https://docs.fabricmc.net/develop

## Rule For Workers

Treat these files as local operating instructions. Do not fetch the internet
from GLM. If documentation is missing, write the exact missing question into
`build/autonomous-loop/visual-progress.jsonl` and continue with repo evidence.
