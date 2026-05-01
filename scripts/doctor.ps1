param(
    [switch]$AllowMissingGitHub
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$Failures = New-Object System.Collections.Generic.List[string]

function Add-Check {
    param(
        [string]$Name,
        [bool]$Ok,
        [string]$Detail,
        [bool]$Required = $true
    )
    $status = if ($Ok) { "OK" } elseif ($Required) { "FAIL" } else { "WARN" }
    Write-Host ("[{0}] {1}: {2}" -f $status, $Name, $Detail)
    if (-not $Ok -and $Required) {
        $Failures.Add($Name) | Out-Null
    }
}

function Get-JavaMajor {
    try {
        $versionText = (& cmd.exe /d /c "java -version 2>&1") -join "`n"
    } catch {
        return @{ Major = 0; Text = "java is not on PATH" }
    }
    if ($versionText -match 'version "1\.(\d+)') {
        return @{ Major = [int]$Matches[1]; Text = $versionText }
    }
    if ($versionText -match 'version "(\d+)') {
        return @{ Major = [int]$Matches[1]; Text = $versionText }
    }
    return @{ Major = 0; Text = $versionText }
}

Push-Location $RepoRoot
try {
    Write-Host "Formic Frontier environment doctor"
    Write-Host "Repo: $RepoRoot"

    $java = Get-JavaMajor
    Add-Check "Java 21+" ($java.Major -ge 21) ("major={0}; JAVA_HOME={1}" -f $java.Major, $env:JAVA_HOME)

    Add-Check "Gradle wrapper" (Test-Path ".\gradlew.bat") "gradlew.bat present"

    $isGitRepo = $false
    try {
        & cmd.exe /d /c "git rev-parse --is-inside-work-tree >NUL 2>NUL"
        $isGitRepo = ($LASTEXITCODE -eq 0)
    } catch {
        $isGitRepo = $false
    }
    Add-Check "Git repository" $isGitRepo "required for autonomous branch/PR workflow"

    $gh = Get-Command gh -ErrorAction SilentlyContinue
    if ($gh) {
        & cmd.exe /d /c "gh auth status >NUL 2>NUL"
        $ghAuthed = ($LASTEXITCODE -eq 0)
        Add-Check "GitHub auth" $ghAuthed "gh auth status" (-not $AllowMissingGitHub)
    } else {
        Add-Check "GitHub CLI" $false "gh is not installed" (-not $AllowMissingGitHub)
    }

    $docker = Get-Command docker -ErrorAction SilentlyContinue
    Add-Check "Docker optional" ($null -ne $docker) "used only for future isolated runners" $false

    $wsl = Get-Command wsl -ErrorAction SilentlyContinue
    Add-Check "WSL optional" ($null -ne $wsl) "used only for future Linux parity checks" $false

    if ($Failures.Count -gt 0) {
        Write-Host ""
        Write-Host "Doctor failed. Fix: install Temurin JDK 21/25, initialize Git, and authenticate GitHub CLI/app access."
        exit 1
    }
    Write-Host ""
    Write-Host "Doctor passed."
}
finally {
    Pop-Location
}
