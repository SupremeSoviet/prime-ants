---
name: launching-the-autonomous-loop
description: "How to start the Formic autonomous dev loop, and which codex.exe actually works (it is not on PATH)"
metadata: 
  node_type: memory
  type: reference
  originSessionId: f4152c93-af52-4383-a3ce-2526f6fd6f25
---

To launch the autonomous dev loop: `scripts/start-autonomous-loop.cmd -CodexProfile zai-glm52 -AllowMissingGitHub -CodexCommand <full-path-to-codex.exe>`. It self-detaches a hidden supervisor (writes `build/autonomous-loop/supervisor.pid`); the supervisor runs `codex exec --cd <repo> --sandbox danger-full-access --json -o <final> -p zai-glm52 -` and pipes the iteration prompt via stdin. Stop it with `scripts/stop-autonomous-loop.cmd`.

**The `-CodexCommand` gotcha (cost me real time):** `codex` is NOT on PATH in non-interactive shells, so the launcher's default `Get-Command codex` throws "codex CLI is not available". You must pass the full exe path. There are multiple `codex.exe` on this machine — pick the right one:
- ✅ The VS Code Codex extension's bundled CLI: `C:\Users\user\.vscode\extensions\openai.chatgpt-<ver>\bin\windows-x86_64\codex.exe` (full CLI, ~323MB, was codex-cli 0.142.3). The `<ver>` changes on extension update — glob it.
- ❌ `C:\Users\user\.codex\.sandbox-bin\codex.exe` (~206MB) is Codex's INTERNAL sandbox helper. It exits immediately when fed the exec prompt over stdin ("The pipe has been ended" at autonomous-loop.ps1:1066) and the supervisor then crashes (ErrorActionPreference Stop). Do not use it.
- Pass the path UNQUOTED through the `cmd //c "...".` Bash wrapper (the path has no spaces); embedding `\"...\"` makes the literal quotes part of the value and `Get-Command` fails.

Verify a launch is healthy: supervisor pid + `childPid` in `build/autonomous-loop/run-state.json` are both alive (`tasklist //FI "PID eq <id>"`), the iteration `*.jsonl` is growing, and `supervisor.err.log` is empty. The Z.AI proxy (`http://127.0.0.1:11452/v1/health` → `has_api_key:true`, model glm-5.2) is a separate long-lived process that already holds the key — you do not need ZAI_API_KEY in your own shell if the proxy is already up. See [[schematic-building-system]] and [[eval-was-a-phantom]] for what the loop iterates on.
