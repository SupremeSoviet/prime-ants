param(
    [string]$HostName = "127.0.0.1",
    [int]$Port = 11452,
    [string]$Model = "glm-5.2",
    [string]$LogDir = "",
    [string]$Python = "python"
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
if ([string]::IsNullOrWhiteSpace($LogDir)) {
    $LogDir = Join-Path $RepoRoot "build\zai-codex-proxy"
}
if ([string]::IsNullOrWhiteSpace($env:ZAI_API_KEY)) {
    throw "ZAI_API_KEY is not set. Set it in the user environment before starting the proxy."
}

Push-Location $RepoRoot
try {
    New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
    & $Python (Join-Path $PSScriptRoot "zai-codex-proxy.py") `
        --host $HostName `
        --port $Port `
        --model $Model `
        --log-dir $LogDir
}
finally {
    Pop-Location
}
