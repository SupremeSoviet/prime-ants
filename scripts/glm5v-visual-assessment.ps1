param(
    [string]$VisualQaDir = "build\visual-qa",
    [string]$Output = "build\visual-qa\formic-visual-assessment.md",
    [string]$IntentDir = "docs\visual-intent",
    [string]$Model = "",
    [string]$Endpoint = "",
    [string]$EnvKey = "ZAI_API_KEY",
    [int]$TimeoutSeconds = 300,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")

Push-Location $RepoRoot
try {
    $argsList = @(
        "tools\glm5v_visual_assessment.py",
        "--visual-qa-dir", $VisualQaDir,
        "--output", $Output,
        "--intent-dir", $IntentDir,
        "--env-key", $EnvKey,
        "--timeout", "$TimeoutSeconds"
    )
    if (-not [string]::IsNullOrWhiteSpace($Model)) {
        $argsList += @("--model", $Model)
    }
    if (-not [string]::IsNullOrWhiteSpace($Endpoint)) {
        $argsList += @("--endpoint", $Endpoint)
    }
    if ($DryRun) {
        $argsList += "--dry-run"
    }
    & python @argsList
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
