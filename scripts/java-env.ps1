function Get-FormicJavaMajorFromText {
    param([string]$VersionText)
    if ($VersionText -match 'version "1\.(\d+)') {
        return [int]$Matches[1]
    }
    if ($VersionText -match 'version "(\d+)') {
        return [int]$Matches[1]
    }
    return 0
}

function Get-FormicJavaCandidate {
    param(
        [string]$JavaExe,
        [string]$Source
    )
    if (-not $JavaExe -or -not (Test-Path -LiteralPath $JavaExe)) {
        return $null
    }
    try {
        $command = '"' + $JavaExe + '" -version 2>&1'
        $versionText = (& cmd.exe /d /c $command) -join "`n"
    } catch {
        return $null
    }
    $major = Get-FormicJavaMajorFromText $versionText
    $binDir = Split-Path -Parent $JavaExe
    $javaHome = Split-Path -Parent $binDir
    [PSCustomObject]@{
        JavaExe = $JavaExe
        JavaHome = $javaHome
        Major = $major
        Source = $Source
        VersionText = $versionText
    }
}

function Get-FormicJavaCandidates {
    $seen = New-Object 'System.Collections.Generic.HashSet[string]' ([System.StringComparer]::OrdinalIgnoreCase)
    $candidates = New-Object System.Collections.Generic.List[object]

    function Add-JavaExe {
        param(
            [string]$JavaExe,
            [string]$Source
        )
        if (-not $JavaExe) {
            return
        }
        try {
            $resolved = [System.IO.Path]::GetFullPath($JavaExe)
        } catch {
            return
        }
        if (-not $seen.Add($resolved)) {
            return
        }
        $candidate = Get-FormicJavaCandidate -JavaExe $resolved -Source $Source
        if ($candidate) {
            $candidates.Add($candidate) | Out-Null
        }
    }

    if ($env:JAVA_HOME) {
        Add-JavaExe (Join-Path $env:JAVA_HOME "bin\java.exe") "JAVA_HOME"
    }

    Get-Command java -All -ErrorAction SilentlyContinue | ForEach-Object {
        Add-JavaExe $_.Source "PATH"
    }

    $roots = @(
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files\Java",
        "C:\Program Files\Microsoft",
        "C:\Program Files\BellSoft",
        "C:\Program Files\Zulu",
        "C:\Program Files\Amazon Corretto"
    )
    foreach ($root in $roots) {
        if (-not (Test-Path -LiteralPath $root)) {
            continue
        }
        Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue | ForEach-Object {
            Add-JavaExe (Join-Path $_.FullName "bin\java.exe") $root
        }
    }

    return $candidates
}

function Use-FormicJava {
    param([int]$MinimumMajor = 21)
    $candidates = @(Get-FormicJavaCandidates)
    $usable = @($candidates | Where-Object { $_.Major -ge $MinimumMajor } | Sort-Object -Property Major -Descending)
    $selected = if ($usable.Count -gt 0) {
        $usable[0]
    } elseif ($candidates.Count -gt 0) {
        @($candidates | Sort-Object -Property Major -Descending)[0]
    } else {
        $null
    }

    if ($selected -and $selected.Major -ge $MinimumMajor) {
        $env:JAVA_HOME = $selected.JavaHome
        $javaBin = Join-Path $selected.JavaHome "bin"
        $pathParts = @($env:Path -split ';' | Where-Object { $_ })
        $alreadyOnPath = $pathParts | Where-Object { $_.Equals($javaBin, [System.StringComparison]::OrdinalIgnoreCase) }
        if (-not $alreadyOnPath) {
            $env:Path = "$javaBin;$env:Path"
        }
    }

    if (-not $selected) {
        return [PSCustomObject]@{
            Ok = $false
            Major = 0
            JavaHome = $env:JAVA_HOME
            JavaExe = ""
            Source = "not found"
            Detail = "java is not on PATH and no common JDK install was found"
        }
    }

    return [PSCustomObject]@{
        Ok = ($selected.Major -ge $MinimumMajor)
        Major = $selected.Major
        JavaHome = $selected.JavaHome
        JavaExe = $selected.JavaExe
        Source = $selected.Source
        Detail = ("major={0}; JAVA_HOME={1}; source={2}" -f $selected.Major, $selected.JavaHome, $selected.Source)
    }
}
