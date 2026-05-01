param(
    [switch]$AllowMissingGitHub,
    [int]$MaxIterations = 0,
    [int]$PauseSeconds = 30,
    [string]$Model = ""
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$LoopDir = Join-Path $RepoRoot "build\autonomous-loop"
$StopFile = Join-Path $LoopDir "stop.requested"
$StateFile = Join-Path $LoopDir "run-state.json"
$PidFile = Join-Path $LoopDir "supervisor.pid"
. (Join-Path $PSScriptRoot "java-env.ps1")

function Write-LoopState {
    param(
        [string]$Status,
        [int]$Iteration,
        [string]$Detail = ""
    )
    $state = [ordered]@{
        status = $Status
        iteration = $Iteration
        detail = $Detail
        updatedAt = (Get-Date).ToString("o")
        pid = $PID
        repo = "$RepoRoot"
    }
    $state | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $StateFile -Encoding UTF8
}

function New-IterationPrompt {
    param([int]$Iteration)
    @"
You are a local Codex autonomous development agent working in Formic Frontier.

Run exactly one small playable slice from the active Renovation Track, then stop and summarize.

Start by reading:
- docs/roadmap.md
- docs/autonomous-dev.md
- .codex/skills/formic-visual-assessment/SKILL.md

Current operating constraints:
- This is local autonomous development. GitHub publishing is not available yet because this repo has no remote and gh is missing.
- The worktree can contain existing uncommitted baseline changes. Do not revert them.
- Preserve `/formic qa scene <name>` and every existing screenshot scene.
- Prefer the next unfinished active Renovation Track slice in this order:
  1. R1 Settlement scale renovation
  2. R2 Architecture polish
  3. R3 Colony Tablet 2.0
- Do not continue the old generic Stage 1-7 roadmap while renovation work is unfinished.
- Keep the slice narrow enough to complete with tests and visual QA in this run.
- Visual QA must include the renovation scenes when available: `settlement_scale`, `tablet_research_map`, `tablet_market`, and `tablet_requests`.

Definition of done for this iteration:
1. Implement the slice.
2. Run `scripts/test-mod.cmd -AllowMissingGitHub`.
3. Run `scripts/gui-smoke.cmd`.
4. Use `$formic-visual-assessment` on the fresh `build/visual-qa` artifacts and save the report to `build/visual-qa/formic-visual-assessment.md`.
5. Run `scripts/autonomous-gate.cmd -AllowMissingGitHub -NoLaunch`.
6. If any command or visual assessment fails, fix the issue or clearly leave the iteration blocked with the exact blocker.

Final response must include:
- implemented slice
- files changed
- test/gate results
- visual assessment verdict
- next recommended slice

Autonomous loop iteration: $Iteration
"@
}

Push-Location $RepoRoot
try {
    New-Item -ItemType Directory -Force -Path $LoopDir | Out-Null
    Set-Content -LiteralPath $PidFile -Value $PID -Encoding ASCII
    if (Test-Path -LiteralPath $StopFile) {
        Remove-Item -LiteralPath $StopFile -Force
    }

    $java = Use-FormicJava -MinimumMajor 21
    if (-not $java.Ok) {
        throw "Java 21+ is required. $($java.Detail)"
    }

    $codex = Get-Command codex -ErrorAction SilentlyContinue
    if (-not $codex) {
        throw "codex CLI is not available on PATH"
    }

    & (Join-Path $PSScriptRoot "doctor.ps1") -AllowMissingGitHub:$AllowMissingGitHub

    $iteration = 1
    while (($MaxIterations -le 0 -or $iteration -le $MaxIterations) -and -not (Test-Path -LiteralPath $StopFile)) {
        Write-LoopState -Status "running" -Iteration $iteration -Detail "Starting Codex iteration"
        $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
        $promptPath = Join-Path $LoopDir "iteration-$('{0:D3}' -f $iteration)-$stamp.prompt.md"
        $jsonLog = Join-Path $LoopDir "iteration-$('{0:D3}' -f $iteration)-$stamp.jsonl"
        $errLog = Join-Path $LoopDir "iteration-$('{0:D3}' -f $iteration)-$stamp.err.log"
        $finalMessage = Join-Path $LoopDir "iteration-$('{0:D3}' -f $iteration)-$stamp.final.md"

        New-IterationPrompt -Iteration $iteration | Set-Content -LiteralPath $promptPath -Encoding UTF8

        $codexArgs = @(
            "exec",
            "--cd", "$RepoRoot",
            "--sandbox", "danger-full-access",
            "--json",
            "-o", "$finalMessage"
        )
        if (-not [string]::IsNullOrWhiteSpace($Model)) {
            $codexArgs += @("-m", $Model)
        }
        $codexArgs += "-"

        $prompt = Get-Content -Raw -LiteralPath $promptPath
        $previousErrorActionPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        try {
            $prompt | & $codex.Source @codexArgs 1> $jsonLog 2> $errLog
            $codexExit = $LASTEXITCODE
        }
        finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }
        if ($codexExit -ne 0) {
            Write-LoopState -Status "blocked" -Iteration $iteration -Detail "Codex iteration exited with $codexExit; see $errLog"
            exit $codexExit
        }

        Write-LoopState -Status "gating" -Iteration $iteration -Detail "Running autonomous gate"
        & (Join-Path $PSScriptRoot "autonomous-gate.ps1") -AllowMissingGitHub:$AllowMissingGitHub -NoLaunch
        if ($LASTEXITCODE -ne 0) {
            Write-LoopState -Status "blocked" -Iteration $iteration -Detail "Autonomous gate failed after iteration $iteration"
            exit $LASTEXITCODE
        }

        Write-LoopState -Status "passed" -Iteration $iteration -Detail "Iteration $iteration passed strict gate"
        $iteration++
        if ($PauseSeconds -gt 0 -and ($MaxIterations -le 0 -or $iteration -le $MaxIterations)) {
            Start-Sleep -Seconds $PauseSeconds
        }
    }

    Write-LoopState -Status "stopped" -Iteration ($iteration - 1) -Detail "Loop stopped by limit or stop file"
}
catch {
    New-Item -ItemType Directory -Force -Path $LoopDir | Out-Null
    Write-LoopState -Status "error" -Iteration 0 -Detail $_.Exception.Message
    throw
}
finally {
    Pop-Location
}
