param(
    [switch]$StopProxy
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$LoopDir = Join-Path $RepoRoot "build\autonomous-loop"
$StopFile = Join-Path $LoopDir "stop.requested"
$PidFile = Join-Path $LoopDir "supervisor.pid"
$ProxyPidFile = Join-Path $RepoRoot "build\zai-codex-proxy\proxy.pid"

New-Item -ItemType Directory -Force -Path $LoopDir | Out-Null
Set-Content -LiteralPath $StopFile -Value (Get-Date).ToString("o") -Encoding ASCII
Write-Host "Stop requested: $StopFile"

if (Test-Path -LiteralPath $PidFile) {
    $pidText = (Get-Content -Raw -LiteralPath $PidFile).Trim()
    if ($pidText -match '^\d+$') {
        $process = Get-Process -Id ([int]$pidText) -ErrorAction SilentlyContinue
        if ($process) {
            Write-Host "Supervisor PID $pidText is still running; it will stop after the current iteration or sleep."
        } else {
            Write-Host "No running supervisor found for PID $pidText."
        }
    }
}

if ($StopProxy -and (Test-Path -LiteralPath $ProxyPidFile)) {
    $proxyPidText = (Get-Content -Raw -LiteralPath $ProxyPidFile).Trim()
    if ($proxyPidText -match '^\d+$') {
        $proxyProcess = Get-Process -Id ([int]$proxyPidText) -ErrorAction SilentlyContinue
        if ($proxyProcess) {
            Stop-Process -Id ([int]$proxyPidText) -Force
            Write-Host "Stopped Z.AI Codex proxy PID $proxyPidText."
        } else {
            Write-Host "No running Z.AI Codex proxy found for PID $proxyPidText."
        }
    }
} elseif (Test-Path -LiteralPath $ProxyPidFile) {
    Write-Host "Z.AI Codex proxy left running. Use -StopProxy to stop it too."
}
