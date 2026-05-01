param(
    [switch]$AllowMissingGitHub,
    [string]$AssessmentReport = "build\visual-qa\formic-visual-assessment.md",
    [switch]$NoLaunch
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
. (Join-Path $PSScriptRoot "java-env.ps1")

Push-Location $RepoRoot
try {
    $java = Use-FormicJava -MinimumMajor 21
    if (-not $java.Ok) {
        throw "Java 21+ is required for the autonomous gate. $($java.Detail)"
    }

    & (Join-Path $PSScriptRoot "doctor.ps1") -AllowMissingGitHub:$AllowMissingGitHub
    & (Join-Path $PSScriptRoot "test-mod.ps1") -AllowMissingGitHub:$AllowMissingGitHub
    & (Join-Path $PSScriptRoot "gui-smoke.ps1") -NoLaunch:$NoLaunch

    & python tools\visual_assessment_gate.py --visual-qa-dir build\visual-qa --assessment-report $AssessmentReport
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
    Write-Host "Autonomous gate passed."
}
finally {
    Pop-Location
}
