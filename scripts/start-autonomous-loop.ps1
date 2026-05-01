param(
    [switch]$AllowMissingGitHub,
    [int]$MaxIterations = 0,
    [int]$PauseSeconds = 30,
    [string]$Model = ""
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$LoopDir = Join-Path $RepoRoot "build\autonomous-loop"
$PidFile = Join-Path $LoopDir "supervisor.pid"
$OutLog = Join-Path $LoopDir "supervisor.out.log"
$ErrLog = Join-Path $LoopDir "supervisor.err.log"
$StopFile = Join-Path $LoopDir "stop.requested"

New-Item -ItemType Directory -Force -Path $LoopDir | Out-Null

if (Test-Path -LiteralPath $PidFile) {
    $existingPid = (Get-Content -Raw -LiteralPath $PidFile).Trim()
    if ($existingPid -match '^\d+$') {
        $existingProcess = Get-Process -Id ([int]$existingPid) -ErrorAction SilentlyContinue
        if ($existingProcess) {
            Write-Host "Autonomous loop is already running. PID: $existingPid"
            exit 0
        }
    }
}

if (Test-Path -LiteralPath $StopFile) {
    Remove-Item -LiteralPath $StopFile -Force
}

if (-not (Get-Command codex -ErrorAction SilentlyContinue)) {
    throw "codex CLI is not available on PATH"
}

$argsList = @(
    "-NoProfile",
    "-ExecutionPolicy", "Bypass",
    "-File", "`"$(Join-Path $PSScriptRoot 'autonomous-loop.ps1')`"",
    "-MaxIterations", "$MaxIterations",
    "-PauseSeconds", "$PauseSeconds"
)
if ($AllowMissingGitHub) {
    $argsList += "-AllowMissingGitHub"
}
if (-not [string]::IsNullOrWhiteSpace($Model)) {
    $argsList += @("-Model", $Model)
}

$process = Start-Process `
    -FilePath "powershell.exe" `
    -ArgumentList $argsList `
    -WorkingDirectory $RepoRoot `
    -PassThru `
    -WindowStyle Hidden `
    -RedirectStandardOutput $OutLog `
    -RedirectStandardError $ErrLog

Set-Content -LiteralPath $PidFile -Value $process.Id -Encoding ASCII
Write-Host "Autonomous loop started. PID: $($process.Id)"
Write-Host "State: $LoopDir\run-state.json"
Write-Host "Logs: $LoopDir"
