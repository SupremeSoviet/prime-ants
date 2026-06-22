param(
    [string]$IntentDir = "docs\visual-intent",
    [string]$LoopDir = "build\autonomous-loop",
    [string]$VisualQaDir = "build\visual-qa",
    [string]$OutputMarkdown = "",
    [string]$OutputJson = ""
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")

function Resolve-RepoPath {
    param([string]$Path)
    if ([System.IO.Path]::IsPathRooted($Path)) {
        return $Path
    }
    return (Join-Path $RepoRoot $Path)
}

function Read-TextOrEmpty {
    param([string]$Path, [int]$MaxChars = 12000)
    if (-not (Test-Path -LiteralPath $Path)) {
        return ""
    }
    $text = Get-Content -Raw -LiteralPath $Path
    if ($text.Length -le $MaxChars) {
        return $text
    }
    return $text.Substring($text.Length - $MaxChars)
}

function Read-JsonOrNull {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        return $null
    }
    try {
        return Get-Content -Raw -LiteralPath $Path | ConvertFrom-Json
    }
    catch {
        return $null
    }
}

function Get-PropertyValue {
    param([object]$Object, [string]$Name, [object]$Default = $null)
    if ($null -ne $Object -and $Object.PSObject.Properties[$Name]) {
        return $Object.PSObject.Properties[$Name].Value
    }
    return $Default
}

function Get-RelativePathText {
    param([string]$Path)
    try {
        $resolved = Resolve-Path -LiteralPath $Path -ErrorAction Stop
        $rootText = ([System.IO.Path]::GetFullPath("$RepoRoot")).TrimEnd("\", "/")
        $pathText = [System.IO.Path]::GetFullPath($resolved.Path)
        if ($pathText.StartsWith($rootText, [System.StringComparison]::OrdinalIgnoreCase)) {
            return $pathText.Substring($rootText.Length).TrimStart("\", "/").Replace("\", "/")
        }
        return $pathText.Replace("\", "/")
    }
    catch {
        return $Path.Replace("\", "/")
    }
}

$intentPath = Resolve-RepoPath $IntentDir
$loopPath = Resolve-RepoPath $LoopDir
$visualQaPath = Resolve-RepoPath $VisualQaDir
$intentDocPath = Join-Path $intentPath "formic-visual-intent.md"
$manifestPath = Join-Path $intentPath "reference-manifest.json"
$templatePath = Join-Path $intentPath "visual-feature-matrix.template.json"
$matrixPath = Join-Path $loopPath "visual-feature-matrix.json"
$progressPath = Join-Path $loopPath "visual-progress.jsonl"
$assessmentPath = Join-Path $visualQaPath "formic-visual-assessment.md"
$visualReportJsonPath = Join-Path $visualQaPath "visual-qa-report.json"
$runStatePath = Join-Path $loopPath "run-state.json"
$loopStatePath = Join-Path $loopPath "visual-loop-state.json"

if ([string]::IsNullOrWhiteSpace($OutputMarkdown)) {
    $OutputMarkdown = Join-Path $loopPath "visual-loop-brief.md"
} elseif (-not [System.IO.Path]::IsPathRooted($OutputMarkdown)) {
    $OutputMarkdown = Join-Path $RepoRoot $OutputMarkdown
}
if ([string]::IsNullOrWhiteSpace($OutputJson)) {
    $OutputJson = Join-Path $loopPath "visual-loop-brief.json"
} elseif (-not [System.IO.Path]::IsPathRooted($OutputJson)) {
    $OutputJson = Join-Path $RepoRoot $OutputJson
}

New-Item -ItemType Directory -Force -Path $loopPath | Out-Null

if (-not (Test-Path -LiteralPath $intentDocPath)) {
    throw "Missing visual intent doc: $intentDocPath"
}
if (-not (Test-Path -LiteralPath $templatePath)) {
    throw "Missing visual feature matrix template: $templatePath"
}

if (-not (Test-Path -LiteralPath $matrixPath)) {
    $template = Get-Content -Raw -LiteralPath $templatePath | ConvertFrom-Json
    $template | Add-Member -NotePropertyName "createdAt" -NotePropertyValue (Get-Date).ToString("o") -Force
    $template | Add-Member -NotePropertyName "updatedAt" -NotePropertyValue (Get-Date).ToString("o") -Force
    $template | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $matrixPath -Encoding UTF8
}

$matrix = Get-Content -Raw -LiteralPath $matrixPath | ConvertFrom-Json
$templateMatrix = Get-Content -Raw -LiteralPath $templatePath | ConvertFrom-Json
if ($matrix.PSObject.Properties["criteria"] -and $templateMatrix.PSObject.Properties["criteria"]) {
    $existingIds = @{}
    $criteriaList = New-Object System.Collections.ArrayList
    foreach ($row in @($matrix.criteria)) {
        [void]$criteriaList.Add($row)
        $rowId = Get-PropertyValue -Object $row -Name "id" -Default ""
        if (-not [string]::IsNullOrWhiteSpace($rowId)) {
            $existingIds[$rowId] = $true
        }
    }
    $mergedTemplateRows = @()
    foreach ($templateRow in @($templateMatrix.criteria)) {
        $templateId = Get-PropertyValue -Object $templateRow -Name "id" -Default ""
        if (-not [string]::IsNullOrWhiteSpace($templateId) -and -not $existingIds.ContainsKey($templateId)) {
            [void]$criteriaList.Add($templateRow)
            $existingIds[$templateId] = $true
            $mergedTemplateRows += $templateId
        }
    }
    if ($mergedTemplateRows.Count -gt 0) {
        $matrix.PSObject.Properties["criteria"].Value = @($criteriaList)
        $matrix | Add-Member -NotePropertyName "updatedAt" -NotePropertyValue (Get-Date).ToString("o") -Force
        $matrix | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $matrixPath -Encoding UTF8
        $matrix = Get-Content -Raw -LiteralPath $matrixPath | ConvertFrom-Json
    }
}
$criteria = @()
if ($matrix.PSObject.Properties["criteria"]) {
    $criteria = @($matrix.criteria)
}

$required = @($criteria | Where-Object {
    [bool](Get-PropertyValue -Object $_ -Name "required" -Default $false) -and
    ((Get-PropertyValue -Object $_ -Name "phase" -Default "visual_baseline").ToString().ToLowerInvariant()) -in @("visual_baseline", "visual-baseline")
})
$requiredOpen = @($required | Where-Object { ((Get-PropertyValue -Object $_ -Name "status" -Default "unknown").ToString().ToLowerInvariant()) -ne "pass" })
$p0p1Open = @($requiredOpen | Where-Object { (Get-PropertyValue -Object $_ -Name "priority" -Default "") -in @("P0", "P1") })
$counts = [ordered]@{
    required = $required.Count
    pass = @($required | Where-Object { ((Get-PropertyValue -Object $_ -Name "status" -Default "unknown").ToString().ToLowerInvariant()) -eq "pass" }).Count
    fail = @($required | Where-Object { ((Get-PropertyValue -Object $_ -Name "status" -Default "unknown").ToString().ToLowerInvariant()) -eq "fail" }).Count
    unknown = @($required | Where-Object { ((Get-PropertyValue -Object $_ -Name "status" -Default "unknown").ToString().ToLowerInvariant()) -eq "unknown" }).Count
    open = $requiredOpen.Count
    openP0P1 = $p0p1Open.Count
}
$visualBaselinePass = ($requiredOpen.Count -eq 0 -and $required.Count -gt 0)
$matrix | Add-Member -NotePropertyName "visualBaselinePass" -NotePropertyValue $visualBaselinePass -Force
$matrix | Add-Member -NotePropertyName "updatedAt" -NotePropertyValue (Get-Date).ToString("o") -Force
$matrix | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $matrixPath -Encoding UTF8

$manifest = Read-JsonOrNull -Path $manifestPath
$references = @()
if ($null -ne $manifest -and $manifest.PSObject.Properties["references"]) {
    foreach ($ref in @($manifest.references)) {
        $file = Get-PropertyValue -Object $ref -Name "file" -Default ""
        $refPath = if ([string]::IsNullOrWhiteSpace($file)) { "" } else { Join-Path $intentPath $file }
        $references += [ordered]@{
            id = Get-PropertyValue -Object $ref -Name "id" -Default ""
            file = $file
            exists = (-not [string]::IsNullOrWhiteSpace($refPath) -and (Test-Path -LiteralPath $refPath))
            role = Get-PropertyValue -Object $ref -Name "role" -Default ""
        }
    }
}

$loopState = Read-JsonOrNull -Path $loopStatePath
$runState = Read-JsonOrNull -Path $runStatePath
$assessmentText = Read-TextOrEmpty -Path $assessmentPath -MaxChars 16000
$assessmentVerdict = "unknown"
if ($assessmentText -match '(?im)^\s*Verdict\s*:\s*(PASS WITH NOTES|PASS|FAIL)\b') {
    $assessmentVerdict = $Matches[1].ToUpperInvariant()
}
$p0Count = ([regex]::Matches($assessmentText, '(?im)\[\s*P0\s*\]')).Count
$p1Count = ([regex]::Matches($assessmentText, '(?im)\[\s*P1\s*\]')).Count
$p2p3Lines = @($assessmentText -split "`r?`n" | Where-Object { $_ -match '\[\s*P[23]\s*\]' } | Select-Object -First 20)

$screenshots = @()
$visualReport = Read-JsonOrNull -Path $visualReportJsonPath
if ($null -ne $visualReport -and $visualReport.PSObject.Properties["screenshots"]) {
    foreach ($entry in @($visualReport.screenshots)) {
        $file = Get-PropertyValue -Object $entry -Name "file" -Default ""
        $scenePath = if ([string]::IsNullOrWhiteSpace($file)) { "" } else { Join-Path $visualQaPath $file }
        $item = if (-not [string]::IsNullOrWhiteSpace($scenePath) -and (Test-Path -LiteralPath $scenePath)) { Get-Item -LiteralPath $scenePath } else { $null }
        $screenshots += [ordered]@{
            scene = [System.IO.Path]::GetFileNameWithoutExtension($file)
            file = $file
            exists = ($null -ne $item)
            modifiedAt = if ($null -ne $item) { $item.LastWriteTime.ToString("o") } else { "" }
            bytes = if ($null -ne $item) { $item.Length } else { 0 }
        }
    }
}

$nextTargets = @()
foreach ($row in @($requiredOpen | Select-Object -First 5)) {
    $nextTargets += [ordered]@{
        id = Get-PropertyValue -Object $row -Name "id" -Default ""
        priority = Get-PropertyValue -Object $row -Name "priority" -Default ""
        owner = Get-PropertyValue -Object $row -Name "owner" -Default ""
        nextAction = Get-PropertyValue -Object $row -Name "nextAction" -Default ""
        acceptance = Get-PropertyValue -Object $row -Name "acceptance" -Default ""
    }
}

$brief = [ordered]@{
    generatedAt = (Get-Date).ToString("o")
    phase = if ($visualBaselinePass) { "playability_mechanics_ready" } else { "visual_baseline" }
    visualBaselinePass = $visualBaselinePass
    requiredCounts = $counts
    activeSlice = Get-PropertyValue -Object $loopState -Name "activeSlice" -Default ""
    nextVisualTarget = Get-PropertyValue -Object $loopState -Name "nextVisualTarget" -Default ""
    assessmentVerdict = $assessmentVerdict
    p0 = $p0Count
    p1 = $p1Count
    matrixPath = Get-RelativePathText -Path $matrixPath
    intentPath = Get-RelativePathText -Path $intentDocPath
    progressPath = Get-RelativePathText -Path $progressPath
    references = $references
    openTargets = $nextTargets
    screenshots = $screenshots
}

$mdLines = @(
    "# Visual Loop Brief",
    "",
    "Generated: $($brief.generatedAt)",
    "Phase: $($brief.phase)",
    "Visual baseline pass: $($brief.visualBaselinePass)",
    "Required rows: $($counts.pass)/$($counts.required) pass, $($counts.fail) fail, $($counts.unknown) unknown, $($counts.open) open.",
    "Open P0/P1 rows: $($counts.openP0P1)",
    "",
    "## Intent",
    "",
    '- Read `docs/visual-intent/formic-visual-intent.md` before selecting work.',
    "- References are art direction, not a shader dependency.",
    '- Do not move to mechanics until every required matrix row is `pass`.',
    ""
)
if ($references.Count -gt 0) {
    $mdLines += "## Reference Slots"
    $mdLines += ""
    foreach ($ref in $references) {
        $status = if ($ref["exists"]) { "present" } else { "missing slot" }
        $mdLines += ('- `{0}` ({1}): {2}' -f $ref["file"], $status, $ref["role"])
    }
    $mdLines += ""
}
$mdLines += "## Current State"
$mdLines += ""
$mdLines += "- Active slice: $($brief.activeSlice)"
$mdLines += "- Next visual target: $($brief.nextVisualTarget)"
$mdLines += "- Latest assessment verdict: $assessmentVerdict"
$mdLines += "- Latest P0/P1 count: P0=$p0Count, P1=$p1Count"
$mdLines += ""
$mdLines += "## Next Required Targets"
$mdLines += ""
if ($nextTargets.Count -eq 0) {
    $mdLines += "- All required visual rows are pass. Mechanics/playability can start only after gate confirms this state."
} else {
    foreach ($target in $nextTargets) {
        $mdLines += ('- [{0}] `{1}` owner={2}: {3}' -f $target["priority"], $target["id"], $target["owner"], $target["nextAction"])
        $mdLines += ("  Acceptance: {0}" -f $target["acceptance"])
    }
}
$mdLines += ""
$mdLines += "## P2/P3 Backlog From Latest Assessment"
$mdLines += ""
if ($p2p3Lines.Count -eq 0) {
    $mdLines += "- No P2/P3 lines found in latest assessment."
} else {
    foreach ($line in $p2p3Lines) {
        $mdLines += "- $line"
    }
}
$mdLines += ""
$mdLines += "## Screenshot Evidence"
$mdLines += ""
if ($screenshots.Count -eq 0) {
    $mdLines += "- No visual QA screenshot metadata found yet."
} else {
    foreach ($shot in ($screenshots | Select-Object -First 30)) {
        $mdLines += ('- `{0}` exists={1} modified={2} bytes={3}' -f $shot["file"], $shot["exists"], $shot["modifiedAt"], $shot["bytes"])
    }
}
$mdLines += ""
$mdLines += "## Loop Rules"
$mdLines += ""
$mdLines += "- Pick one narrow visual target from the open matrix rows."
$mdLines += '- Update `visual-feature-matrix.json` row statuses only with screenshot evidence.'
$mdLines += '- Append every scout/build/smoke/assessment/gate event to `visual-progress.jsonl`.'
$mdLines += "- Run fresh gui-smoke and image-capable assessment before claiming progress."

[System.IO.File]::WriteAllText($OutputMarkdown, ($mdLines -join [Environment]::NewLine), [System.Text.UTF8Encoding]::new($false))
$brief | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $OutputJson -Encoding UTF8

$progressEvent = [ordered]@{
    ts = (Get-Date).ToString("o")
    event = "brief.generated"
    phase = $brief.phase
    visualBaselinePass = $visualBaselinePass
    requiredOpen = $counts.open
    openP0P1 = $counts.openP0P1
    assessmentVerdict = $assessmentVerdict
    brief = Get-RelativePathText -Path $OutputMarkdown
}
($progressEvent | ConvertTo-Json -Compress -Depth 8) | Add-Content -LiteralPath $progressPath -Encoding UTF8

Write-Host "Visual loop brief written: $OutputMarkdown"
Write-Host "Visual feature matrix: $matrixPath"
