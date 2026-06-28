param(
    [switch]$AllowMissingGitHub
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
. (Join-Path $PSScriptRoot "java-env.ps1")

Push-Location $RepoRoot
try {
    $java = Use-FormicJava -MinimumMajor 21
    if (-not $java.Ok) {
        throw "Java 21+ is required for the content gate. $($java.Detail)"
    }

    & (Join-Path $PSScriptRoot "doctor.ps1") -AllowMissingGitHub:$AllowMissingGitHub
    # test-mod is the real content gate: a content feature is only accepted once
    # its own gametest passes and no existing gametest regressed. test-mod.ps1
    # exits non-zero on failure, which aborts this gate.
    & (Join-Path $PSScriptRoot "test-mod.ps1") -AllowMissingGitHub:$AllowMissingGitHub

    & python "tools\content_feature_gate.py" `
        --matrix "build\autonomous-loop\content-feature-matrix.json" `
        --test-summary "build\qa\test-mod-summary.json"
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
    Write-Host "Content gate passed."
}
finally {
    Pop-Location
}
