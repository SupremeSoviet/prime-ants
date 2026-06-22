param(
    [switch]$AllowMissingGitHub,
    [int]$MaxIterations = 0,
    [int]$PauseSeconds = 30,
    [string]$Model = "",
    [string]$CodexProfile = "",
    [string]$CodexCommand = "codex",
    [int]$MaxGuardRetries = 6,
    [int]$CodexCompletionWatchdogMinutes = 5,
    [int]$CodexActivityWatchdogMinutes = 45,
    [int]$RateLimitRetrySeconds = 180,
    [int]$RateLimitMaxRetrySeconds = 21600,
    [string]$ProxyCommand = ""
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

function Get-CodexHome {
    if (-not [string]::IsNullOrWhiteSpace($env:CODEX_HOME)) {
        return $env:CODEX_HOME
    }
    return (Join-Path $env:USERPROFILE ".codex")
}

function Ensure-ZaiProxyToken {
    if ($CodexProfile -ne "zai-glm52") {
        return
    }
    if ([string]::IsNullOrWhiteSpace($env:ZAI_CODEX_PROXY_TOKEN)) {
        $env:ZAI_CODEX_PROXY_TOKEN = [guid]::NewGuid().ToString("N")
    }
}

if (-not [string]::IsNullOrWhiteSpace($CodexProfile)) {
    if ($CodexProfile -notmatch '^[A-Za-z0-9_.-]+$') {
        throw "CodexProfile must be a simple profile name, not a path or command line."
    }
    $profilePath = Join-Path (Get-CodexHome) "$CodexProfile.config.toml"
    if (-not (Test-Path -LiteralPath $profilePath)) {
        throw "Codex profile was not found: $profilePath"
    }
}

Ensure-ZaiProxyToken

$codex = Get-Command $CodexCommand -ErrorAction SilentlyContinue
if (-not $codex) {
    throw "codex CLI is not available: $CodexCommand"
}

$argsList = @(
    "-NoProfile",
    "-ExecutionPolicy", "Bypass",
    "-File", "`"$(Join-Path $PSScriptRoot 'autonomous-loop.ps1')`"",
    "-MaxIterations", "$MaxIterations",
    "-PauseSeconds", "$PauseSeconds",
    "-MaxGuardRetries", "$MaxGuardRetries",
    "-CodexCompletionWatchdogMinutes", "$CodexCompletionWatchdogMinutes",
    "-CodexActivityWatchdogMinutes", "$CodexActivityWatchdogMinutes",
    "-RateLimitRetrySeconds", "$RateLimitRetrySeconds",
    "-RateLimitMaxRetrySeconds", "$RateLimitMaxRetrySeconds",
    "-CodexCommand", "`"$($codex.Source)`""
)
if ($AllowMissingGitHub) {
    $argsList += "-AllowMissingGitHub"
}
if (-not [string]::IsNullOrWhiteSpace($Model)) {
    $argsList += @("-Model", $Model)
}
if (-not [string]::IsNullOrWhiteSpace($CodexProfile)) {
    $argsList += @("-CodexProfile", $CodexProfile)
}
if (-not [string]::IsNullOrWhiteSpace($ProxyCommand)) {
    $argsList += @("-ProxyCommand", "`"$ProxyCommand`"")
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
