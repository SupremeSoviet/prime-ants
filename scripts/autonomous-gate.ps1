param(
    [switch]$AllowMissingGitHub,
    [string]$AssessmentReport = "build\visual-qa\formic-visual-assessment.md",
    [string]$IntentDir = "docs\visual-intent",
    [ValidateSet("openai", "glm5v")]
    [string]$VisionAssessor = "openai",
    [switch]$SkipVisionAssessment,
    [switch]$NoLaunch,
    [string]$FreshnessMarker = "",
    [string]$CodexJsonLog = ""
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
. (Join-Path $PSScriptRoot "java-env.ps1")

function Get-ActiveFreshnessMarker {
    if (-not [string]::IsNullOrWhiteSpace($FreshnessMarker)) {
        return $FreshnessMarker
    }
    $statePath = Join-Path $RepoRoot "build\autonomous-loop\visual-loop-state.json"
    if (-not (Test-Path -LiteralPath $statePath)) {
        return ""
    }
    try {
        $state = Get-Content -Raw -LiteralPath $statePath | ConvertFrom-Json
        if ($state.PSObject.Properties["freshnessMarker"] -and -not [string]::IsNullOrWhiteSpace($state.freshnessMarker)) {
            return $state.freshnessMarker
        }
        if ($state.PSObject.Properties["lastArtifacts"] -and $state.lastArtifacts.PSObject.Properties["freshnessMarker"]) {
            return $state.lastArtifacts.freshnessMarker
        }
    }
    catch {
        return ""
    }
    return ""
}

function Resolve-RepoRelativePath {
    param([string]$Path)
    if ([string]::IsNullOrWhiteSpace($Path)) {
        return ""
    }
    if ([System.IO.Path]::IsPathRooted($Path)) {
        return $Path
    }
    return (Join-Path $RepoRoot $Path)
}

function Assert-VisualInputsFreshForNoLaunch {
    param([string]$Marker)
    if ([string]::IsNullOrWhiteSpace($Marker)) {
        return
    }
    $markerPath = Resolve-RepoRelativePath -Path $Marker
    if (-not (Test-Path -LiteralPath $markerPath)) {
        throw "Visual freshness marker is missing: $markerPath"
    }
    $markerTime = (Get-Item -LiteralPath $markerPath).LastWriteTimeUtc
    $visualQaDir = Join-Path $RepoRoot "build\visual-qa"
    $reportJsonPath = Join-Path $visualQaDir "visual-qa-report.json"
    $reportMdPath = Join-Path $visualQaDir "visual-qa-report.md"
    foreach ($requiredPath in @($reportJsonPath, $reportMdPath)) {
        if (-not (Test-Path -LiteralPath $requiredPath)) {
            throw "Missing visual QA artifact: $requiredPath"
        }
        if ((Get-Item -LiteralPath $requiredPath).LastWriteTimeUtc -le $markerTime) {
            throw "Visual QA artifact is older than freshness marker: $requiredPath"
        }
    }
    $report = Get-Content -Raw -LiteralPath $reportJsonPath | ConvertFrom-Json
    foreach ($entry in @($report.screenshots)) {
        if (-not $entry.PSObject.Properties["file"]) {
            throw "Visual QA report screenshot entry is missing file."
        }
        $screenshotPath = Join-Path $visualQaDir $entry.file
        if (-not (Test-Path -LiteralPath $screenshotPath)) {
            throw "Missing reported screenshot: $screenshotPath"
        }
        if ((Get-Item -LiteralPath $screenshotPath).LastWriteTimeUtc -le $markerTime) {
            throw "Reported screenshot is older than freshness marker: $screenshotPath"
        }
    }
}

Push-Location $RepoRoot
try {
    $java = Use-FormicJava -MinimumMajor 21
    if (-not $java.Ok) {
        throw "Java 21+ is required for the autonomous gate. $($java.Detail)"
    }

    & (Join-Path $PSScriptRoot "doctor.ps1") -AllowMissingGitHub:$AllowMissingGitHub
    & (Join-Path $PSScriptRoot "test-mod.ps1") -AllowMissingGitHub:$AllowMissingGitHub
    & (Join-Path $PSScriptRoot "gui-smoke.ps1") -NoLaunch:$NoLaunch
    & (Join-Path $PSScriptRoot "visual-loop-brief.ps1") -IntentDir $IntentDir
    $activeFreshnessMarker = Get-ActiveFreshnessMarker
    if ($NoLaunch) {
        try {
            Assert-VisualInputsFreshForNoLaunch -Marker $activeFreshnessMarker
        }
        catch {
            Write-Host "NoLaunch cannot reuse stale visual artifacts. Run scripts\gui-smoke.cmd before gate."
            Write-Host $_.Exception.Message
            exit 2
        }
    }

    $requiredAssessor = ""
    if (-not $SkipVisionAssessment) {
        if ($VisionAssessor -eq "glm5v") {
            & (Join-Path $PSScriptRoot "glm5v-visual-assessment.ps1") -VisualQaDir "build\visual-qa" -Output $AssessmentReport -IntentDir $IntentDir
            $requiredAssessor = "GLM-5V-Turbo"
        } else {
            & (Join-Path $PSScriptRoot "openai-visual-assessment.ps1") -VisualQaDir "build\visual-qa" -Output $AssessmentReport -IntentDir $IntentDir
            $requiredAssessor = "GPT-5.4 mini"
        }
        if ($LASTEXITCODE -ne 0) {
            exit $LASTEXITCODE
        }
    }
    & (Join-Path $PSScriptRoot "visual-loop-brief.ps1") -IntentDir $IntentDir

    $gateArgs = @(
        "tools\visual_assessment_gate.py",
        "--visual-qa-dir", "build\visual-qa",
        "--assessment-report", $AssessmentReport,
        "--matrix", "build\autonomous-loop\visual-feature-matrix.json",
        "--loop-state", "build\autonomous-loop\visual-loop-state.json"
    )
    if (-not [string]::IsNullOrWhiteSpace($activeFreshnessMarker)) {
        $gateArgs += @("--freshness-marker", $activeFreshnessMarker)
    }
    if (-not [string]::IsNullOrWhiteSpace($CodexJsonLog)) {
        $gateArgs += @("--codex-json-log", $CodexJsonLog)
    }
    if (-not $SkipVisionAssessment -and -not [string]::IsNullOrWhiteSpace($requiredAssessor)) {
        $gateArgs += @("--require-assessor", $requiredAssessor)
    }
    & python @gateArgs
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
    Write-Host "Autonomous gate passed."
}
finally {
    Pop-Location
}
