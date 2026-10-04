param(
    [Parameter(Mandatory=$true)][string]$Name,
    [Parameter(Mandatory=$true)][string[]]$GradleArgs,
    [Parameter(Mandatory=$true)][string]$EvidenceDirectory
)
$ErrorActionPreference = 'Stop'
$workspace = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $workspace
New-Item -ItemType Directory -Path $EvidenceDirectory -Force | Out-Null
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$start = Get-Date
$log = Join-Path $EvidenceDirectory "$Name.log"
$metadata = [ordered]@{
    command = '.\gradlew.bat ' + ($GradleArgs -join ' ')
    working_directory = $workspace
    java_home = $env:JAVA_HOME
    start = $start.ToString('o')
    log = $log
}
$metadata | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $EvidenceDirectory "$Name.json")
$timer = [Diagnostics.Stopwatch]::StartNew()
# Native stderr is evidence; let Gradle's exit code determine success.
$ErrorActionPreference = 'Continue'
& .\gradlew.bat @GradleArgs *> $log
$exitCode = $LASTEXITCODE
$timer.Stop()
$ErrorActionPreference = 'Stop'
$metadata.end = (Get-Date).ToString('o')
$metadata.elapsed_seconds = $timer.Elapsed.TotalSeconds
$metadata.exit_code = $exitCode
$metadata | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $EvidenceDirectory "$Name.json")
Get-Content -Encoding UTF8 -LiteralPath $log
Write-Output "EXIT_CODE=$exitCode ELAPSED_SECONDS=$($timer.Elapsed.TotalSeconds) EVIDENCE=$log"
exit $exitCode
