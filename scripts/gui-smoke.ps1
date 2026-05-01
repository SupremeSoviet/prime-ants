param(
    [string]$QuickPlayWorld = "FormicVisualQA",
    [int]$TimeoutSeconds = 900,
    [int]$Width = 1600,
    [int]$Height = 900,
    [switch]$SkipWorldPrepare,
    [switch]$NoLaunch
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$VisualQaDir = Join-Path $RepoRoot "build\visual-qa"
$ClientLog = Join-Path $VisualQaDir "runClient.log"
$ClientErr = Join-Path $VisualQaDir "runClient.err.log"

function Set-ClientOption {
    param(
        [string]$Key,
        [string]$Value
    )
    $runDir = Join-Path $RepoRoot "run"
    $optionsPath = Join-Path $runDir "options.txt"
    New-Item -ItemType Directory -Force -Path $runDir | Out-Null
    $lines = if (Test-Path -LiteralPath $optionsPath) { Get-Content -LiteralPath $optionsPath } else { @() }
    $prefix = "$Key`:"
    $updated = New-Object System.Collections.Generic.List[string]
    $found = $false
    foreach ($line in $lines) {
        if ($line.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) {
            $updated.Add("$prefix$Value") | Out-Null
            $found = $true
        } else {
            $updated.Add($line) | Out-Null
        }
    }
    if (-not $found) {
        $updated.Add("$prefix$Value") | Out-Null
    }
    Set-Content -LiteralPath $optionsPath -Value $updated -Encoding UTF8
}

Push-Location $RepoRoot
try {
    $resolvedRoot = [System.IO.Path]::GetFullPath($RepoRoot)
    $resolvedQa = [System.IO.Path]::GetFullPath($VisualQaDir)
    if (-not $resolvedQa.StartsWith($resolvedRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to write visual QA outside workspace: $resolvedQa"
    }
    if (Test-Path -LiteralPath $VisualQaDir) {
        Remove-Item -LiteralPath $VisualQaDir -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $VisualQaDir | Out-Null

    if (-not $NoLaunch) {
        & (Join-Path $PSScriptRoot "doctor.ps1") -AllowMissingGitHub

        if ($QuickPlayWorld -match '\s') {
            throw "QuickPlayWorld must not contain spaces for reliable Windows automation. Use a world folder name like FormicVisualQA."
        }
        if (-not $SkipWorldPrepare) {
            & (Join-Path $PSScriptRoot "prepare-gui-world.ps1") -WorldName $QuickPlayWorld
        }
        Set-ClientOption "tutorialStep" "none"
        $quickPlayLog = Join-Path $VisualQaDir "quickplay.json"
        $quickPlayArgs = "--quickPlayPath $quickPlayLog --quickPlaySingleplayer=$QuickPlayWorld --width $Width --height $Height"
        $gradleArgs = @(
            "-Dformic.visualQa=true",
            "-Dformic.visualQa.dir=$VisualQaDir",
            "-Dformic.visualQa.exit=true",
            "-Dformic.visualQa.world=$QuickPlayWorld",
            "runClient",
            "`"--args=$quickPlayArgs`""
        )
        $gradleCommand = ($gradleArgs | ForEach-Object { $_ }) -join " "
        $gradleCommand = ".\gradlew.bat $gradleCommand"

        Write-Host "Launching visual QA client. World: $QuickPlayWorld"
        $process = Start-Process -FilePath "cmd.exe" -ArgumentList @("/d", "/c", $gradleCommand) -WorkingDirectory $RepoRoot -PassThru -NoNewWindow -RedirectStandardOutput $ClientLog -RedirectStandardError $ClientErr
        $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
        while (-not $process.HasExited -and (Get-Date) -lt $deadline) {
            Start-Sleep -Seconds 2
            $process.Refresh()
        }
        if (-not $process.HasExited) {
            Stop-Process -Id $process.Id -Force
            throw "Visual QA client timed out after $TimeoutSeconds seconds."
        }
        if ($process.ExitCode -ne 0) {
            Write-Host "runClient exited with $($process.ExitCode). Continuing to report collected artifacts."
        }
    }

    & python tools\visual_qa_report.py --visual-qa-dir $VisualQaDir
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
    Write-Host "Visual QA report: $(Join-Path $VisualQaDir 'visual-qa-report.md')"
}
finally {
    Pop-Location
}
