param(
    [string]$WorldName = "FormicVisualQA",
    [int]$TimeoutSeconds = 180
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$RunDir = Join-Path $RepoRoot "run"
$SaveDir = Join-Path $RunDir "saves\$WorldName"
$ServerWorldDir = Join-Path $RunDir $WorldName
$VisualQaDir = Join-Path $RepoRoot "build\visual-qa"
$ServerLog = Join-Path $VisualQaDir "prepare-world.log"
$ServerErr = Join-Path $VisualQaDir "prepare-world.err.log"

function Assert-WorkspaceChild {
    param([string]$Path)
    $resolvedRoot = [System.IO.Path]::GetFullPath($RepoRoot)
    $resolvedTarget = [System.IO.Path]::GetFullPath($Path)
    if (-not $resolvedTarget.StartsWith($resolvedRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing path outside workspace: $resolvedTarget"
    }
}

function Remove-WorkspacePath {
    param([string]$Path)
    Assert-WorkspaceChild $Path
    if (Test-Path -LiteralPath $Path) {
        Remove-Item -LiteralPath $Path -Recurse -Force
    }
}

function Stop-WorkspaceRunServer {
    $repoNeedle = [string]$RepoRoot
    Get-CimInstance Win32_Process -Filter "name='java.exe'" |
        Where-Object {
            $_.CommandLine -and
            $_.CommandLine.Contains($repoNeedle) -and
            ($_.CommandLine.Contains("runServer") -or $_.CommandLine.Contains("KnotServer"))
        } |
        ForEach-Object {
            Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
        }
}

if ($WorldName -match '\s') {
    throw "WorldName must not contain spaces for reliable quick-play automation. Use a folder name like FormicVisualQA."
}

Push-Location $RepoRoot
try {
    $levelDat = Join-Path $SaveDir "level.dat"
    if (Test-Path -LiteralPath $levelDat) {
        Write-Host "GUI world already exists: $SaveDir"
        exit 0
    }

    New-Item -ItemType Directory -Force -Path $RunDir | Out-Null
    New-Item -ItemType Directory -Force -Path (Join-Path $RunDir "saves") | Out-Null
    New-Item -ItemType Directory -Force -Path $VisualQaDir | Out-Null
    Remove-WorkspacePath $ServerWorldDir
    Remove-WorkspacePath $SaveDir

    Set-Content -Path (Join-Path $RunDir "eula.txt") -Value "eula=true" -Encoding ASCII
    Set-Content -Path (Join-Path $RunDir "server.properties") -Encoding ASCII -Value @"
level-name=$WorldName
gamemode=creative
difficulty=peaceful
online-mode=false
allow-flight=true
spawn-protection=0
enable-command-block=true
view-distance=8
simulation-distance=6
"@

    Write-Host "Generating GUI quick-play world: $WorldName"
    $process = Start-Process -FilePath "cmd.exe" -ArgumentList @("/d", "/c", ".\gradlew.bat runServer --args=--nogui") -WorkingDirectory $RepoRoot -PassThru -NoNewWindow -RedirectStandardOutput $ServerLog -RedirectStandardError $ServerErr
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $ready = $false
    while (-not $process.HasExited -and (Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 2
        $process.Refresh()
        $serverLevel = Join-Path $ServerWorldDir "level.dat"
        if ((Test-Path -LiteralPath $serverLevel) -and (Select-String -Path $ServerLog -Pattern "Done \(" -Quiet -ErrorAction SilentlyContinue)) {
            $ready = $true
            break
        }
    }

    if (-not $process.HasExited) {
        Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
    }
    Stop-WorkspaceRunServer
    Start-Sleep -Seconds 2

    if (-not $ready -and -not (Test-Path -LiteralPath (Join-Path $ServerWorldDir "level.dat"))) {
        throw "World generation did not complete before timeout. See $ServerLog and $ServerErr."
    }

    Copy-Item -LiteralPath $ServerWorldDir -Destination $SaveDir -Recurse -Force
    $sessionLock = Join-Path $SaveDir "session.lock"
    if (Test-Path -LiteralPath $sessionLock) {
        Remove-Item -LiteralPath $sessionLock -Force
    }
    Write-Host "GUI world prepared: $SaveDir"
}
finally {
    Pop-Location
}
