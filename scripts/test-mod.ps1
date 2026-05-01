param(
    [switch]$SkipClean,
    [switch]$AllowMissingGitHub
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$QaDir = Join-Path $RepoRoot "build\qa"
$GradleLog = Join-Path $QaDir "gradle-build.log"
$SummaryJson = Join-Path $QaDir "test-mod-summary.json"
$SummaryMd = Join-Path $QaDir "test-mod-summary.md"

function Remove-WorkspaceChild {
    param([string]$RelativePath)
    $target = Join-Path $RepoRoot $RelativePath
    $resolvedRoot = [System.IO.Path]::GetFullPath($RepoRoot)
    $resolvedTarget = [System.IO.Path]::GetFullPath($target)
    if (-not $resolvedTarget.StartsWith($resolvedRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove path outside workspace: $resolvedTarget"
    }
    if (Test-Path -LiteralPath $resolvedTarget) {
        Remove-Item -LiteralPath $resolvedTarget -Recurse -Force
    }
}

Push-Location $RepoRoot
try {
    New-Item -ItemType Directory -Force -Path $QaDir | Out-Null
    & (Join-Path $PSScriptRoot "doctor.ps1") -AllowMissingGitHub:$AllowMissingGitHub

    if (-not $SkipClean) {
        Remove-WorkspaceChild "build\run\gameTest"
    }
    $gameTestRunDir = Join-Path $RepoRoot "build\run\gameTest"
    New-Item -ItemType Directory -Force -Path $gameTestRunDir | Out-Null
    Set-Content -Path (Join-Path $gameTestRunDir "server.properties") -Value "" -Encoding ASCII

    Write-Host "Running Gradle build and server GameTests..."
    & cmd.exe /d /c ".\gradlew.bat build > ""$GradleLog"" 2>&1"
    $buildExit = $LASTEXITCODE

    $logs = @()
    foreach ($path in @("build\run\gameTest\logs\latest.log", "logs\latest.log")) {
        $full = Join-Path $RepoRoot $path
        if (Test-Path $full) {
            $logs += Get-Content $full
        }
    }
    if (Test-Path $GradleLog) {
        $logs += Get-Content $GradleLog
    }

    $badPatterns = "CRASH","CrashReport","Missing texture","missing model","Exception","ERROR"
    $ignoredPatterns = @(
        "fabric-crash-report-info",
        "Failed to load properties from file: server.properties",
        "NoSuchFileException: server.properties",
        "WindowsException.translateToIOException",
        "WindowsException.rethrowAsIOException"
    )
    $findings = @()
    foreach ($line in $logs) {
        $lineText = [string]$line
        $ignored = $false
        foreach ($ignoredPattern in $ignoredPatterns) {
            if ($lineText -like "*$ignoredPattern*") {
                $ignored = $true
                break
            }
        }
        if ($ignored) {
            continue
        }
        foreach ($pattern in $badPatterns) {
            if ($lineText -like "*$pattern*" -and $lineText -notlike "*Yggdrasil Key Fetcher*") {
                $findings += $lineText
                break
            }
        }
    }

    $status = if ($buildExit -eq 0 -and $findings.Count -eq 0) { "passed" } else { "failed" }
    $json = @{
        status = $status
        buildExit = $buildExit
        findings = $findings
        gradleLog = "build/qa/gradle-build.log"
    } | ConvertTo-Json -Depth 4
    Set-Content -Path $SummaryJson -Value $json -Encoding UTF8

    $md = New-Object System.Text.StringBuilder
    [void]$md.AppendLine("# Formic Test Mod Report")
    [void]$md.AppendLine("")
    [void]$md.AppendLine("Status: $status")
    [void]$md.AppendLine("")
    [void]$md.AppendLine("Gradle exit: $buildExit")
    if ($findings.Count -gt 0) {
        [void]$md.AppendLine("")
        [void]$md.AppendLine("Findings:")
        foreach ($finding in $findings) {
            [void]$md.AppendLine("- $finding")
        }
    }
    Set-Content -Path $SummaryMd -Value $md.ToString() -Encoding UTF8

    if ($status -ne "passed") {
        Write-Host "Test mod report failed: $SummaryMd"
        exit 1
    }
    Write-Host "Test mod report passed: $SummaryMd"
}
finally {
    Pop-Location
}
