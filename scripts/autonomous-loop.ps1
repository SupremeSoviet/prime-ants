param(
    [switch]$AllowMissingGitHub,
    [int]$MaxIterations = 0,
    [int]$PauseSeconds = 30,
    [string]$Model = "",
    [string]$CodexProfile = "",
    [string]$CodexCommand = "codex",
    [int]$MaxGuardRetries = 6,
    [int]$CodexCompletionWatchdogMinutes = 5,
    [int]$CodexActivityWatchdogMinutes = 45,
    [int]$RateLimitRetrySeconds = 180,
    [int]$RateLimitMaxRetrySeconds = 21600,
    [string]$ProxyCommand = ""
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$LoopDir = Join-Path $RepoRoot "build\autonomous-loop"
$StopFile = Join-Path $LoopDir "stop.requested"
$StateFile = Join-Path $LoopDir "run-state.json"
$VisualLoopStateFile = Join-Path $LoopDir "visual-loop-state.json"
$PidFile = Join-Path $LoopDir "supervisor.pid"
$ChildPidFile = Join-Path $LoopDir "codex-child.pid"
$ProxyDir = Join-Path $RepoRoot "build\zai-codex-proxy"
$VisualQaDir = Join-Path $RepoRoot "build\visual-qa"
$VisualScreenshotDir = Join-Path $VisualQaDir "screenshots"
$VisualQaReportJson = Join-Path $VisualQaDir "visual-qa-report.json"
$VisualQaReportMd = Join-Path $VisualQaDir "visual-qa-report.md"
$VisualAssessmentReport = Join-Path $VisualQaDir "formic-visual-assessment.md"
$VisualIntentDir = Join-Path $RepoRoot "docs\visual-intent"
$VisualFeatureMatrixFile = Join-Path $LoopDir "visual-feature-matrix.json"
$VisualLoopBriefMd = Join-Path $LoopDir "visual-loop-brief.md"
$VisualProgressLog = Join-Path $LoopDir "visual-progress.jsonl"
. (Join-Path $PSScriptRoot "java-env.ps1")

function Write-LoopState {
    param(
        [string]$Status,
        [int]$Iteration,
        [string]$Detail = "",
        [int]$ChildPid = 0,
        [string]$JsonLog = "",
        [string]$FinalMessage = "",
        [double]$IdleMinutes = -1
    )
    $state = [ordered]@{
        status = $Status
        iteration = $Iteration
        detail = $Detail
        updatedAt = (Get-Date).ToString("o")
        pid = $PID
        repo = "$RepoRoot"
        codexProfile = $CodexProfile
        model = $Model
    }
    if ($ChildPid -gt 0) {
        $state["childPid"] = $ChildPid
    }
    if (-not [string]::IsNullOrWhiteSpace($JsonLog)) {
        $state["jsonLog"] = $JsonLog
    }
    if (-not [string]::IsNullOrWhiteSpace($FinalMessage)) {
        $state["finalMessage"] = $FinalMessage
    }
    if ($IdleMinutes -ge 0) {
        $state["jsonLogIdleMinutes"] = [math]::Round($IdleMinutes, 2)
    }
    $state | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $StateFile -Encoding UTF8
}

function Write-VisualProgressEvent {
    param(
        [int]$Iteration,
        [string]$Event,
        [string]$Detail = "",
        [hashtable]$Data = $null
    )
    New-Item -ItemType Directory -Force -Path $LoopDir | Out-Null
    $entry = [ordered]@{
        ts = (Get-Date).ToString("o")
        iteration = $Iteration
        event = $Event
        detail = $Detail
    }
    if ($null -ne $Data) {
        $entry["data"] = $Data
    }
    ($entry | ConvertTo-Json -Compress -Depth 8) | Add-Content -LiteralPath $VisualProgressLog -Encoding UTF8
}

function Get-CodexFailureText {
    param(
        [string]$JsonLog,
        [string]$ErrLog
    )

    $combined = ""
    foreach ($path in @($JsonLog, $ErrLog)) {
        if (Test-Path -LiteralPath $path) {
            $combined += [Environment]::NewLine
            $combined += Get-Content -Raw -LiteralPath $path
        }
    }
    return $combined
}

function Test-CodexTransientFailure {
    param(
        [string]$JsonLog,
        [string]$ErrLog,
        [string]$FinalMessage
    )

    if (Test-Path -LiteralPath $FinalMessage) {
        return $false
    }

    $combined = Get-CodexFailureText -JsonLog $JsonLog -ErrLog $ErrLog

    if ([string]::IsNullOrWhiteSpace($combined)) {
        return $false
    }

    return $combined -match "stream disconnected before completion" `
        -or $combined -match "Remote end closed connection without response" `
        -or $combined -match "DECRYPTION_FAILED_OR_BAD_RECORD_MAC" `
        -or $combined -match "responses_retry.*5/5" `
        -or (Test-CodexRateLimitFailure -JsonLog $JsonLog -ErrLog $ErrLog)
}

function Test-CodexRateLimitFailure {
    param(
        [string]$JsonLog,
        [string]$ErrLog
    )

    $combined = Get-CodexFailureText -JsonLog $JsonLog -ErrLog $ErrLog

    if ([string]::IsNullOrWhiteSpace($combined)) {
        return $false
    }

    return $combined -match "HTTP 429" `
        -or $combined -match "(?i)too many requests" `
        -or $combined -match "(?i)rate[_ -]?limit" `
        -or $combined -match "(?i)\bquota\b" `
        -or $combined -match "(?i)resource exhausted" `
        -or $combined -match '"code"\s*:\s*"1305"' `
        -or $combined -match "temporarily overloaded"
}

function Get-CodexRateLimitBackoffSeconds {
    param(
        [string]$JsonLog,
        [string]$ErrLog,
        [int]$DefaultSeconds,
        [int]$MaxSeconds
    )

    $combined = Get-CodexFailureText -JsonLog $JsonLog -ErrLog $ErrLog
    if ([string]::IsNullOrWhiteSpace($combined)) {
        return $DefaultSeconds
    }

    $seconds = $DefaultSeconds
    $resetAtPattern = "(?i)(?:reset|available|until)[^\d]{0,40}(\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2})"
    $hourPattern = "(?i)(?:retry|reset|available|quota|limit|window|cooldown|again)[^`r`n]{0,120}?(\d+(?:\.\d+)?)\s*(?:h|hr|hrs|hour|hours)\b"
    $minutePattern = "(?i)(?:retry|reset|available|quota|limit|window|cooldown|again)[^`r`n]{0,120}?(\d+(?:\.\d+)?)\s*(?:m|min|mins|minute|minutes)\b"
    $retryAfterPattern = "(?i)retry-after[^\d]{0,20}(\d+)"
    if ($combined -match $resetAtPattern) {
        $resetAt = [datetime]::MinValue
        if ([datetime]::TryParseExact($Matches[1], "yyyy-MM-dd HH:mm:ss", [Globalization.CultureInfo]::InvariantCulture, [Globalization.DateTimeStyles]::AssumeLocal, [ref]$resetAt)) {
            $untilReset = [int][Math]::Ceiling(($resetAt - (Get-Date)).TotalSeconds) + 300
            if ($untilReset -gt 0) {
                $seconds = $untilReset
            }
        }
    } elseif ($combined -match $hourPattern) {
        $seconds = [int][Math]::Ceiling(([double]$Matches[1]) * 3600) + 300
    } elseif ($combined -match $minutePattern) {
        $seconds = [int][Math]::Ceiling(([double]$Matches[1]) * 60) + 60
    } elseif ($combined -match $retryAfterPattern) {
        $seconds = [int]$Matches[1] + 30
    }

    if ($seconds -lt $DefaultSeconds) {
        $seconds = $DefaultSeconds
    }
    if ($MaxSeconds -gt 0 -and $seconds -gt $MaxSeconds) {
        $seconds = $MaxSeconds
    }
    return $seconds
}

function Get-CodexHome {
    if (-not [string]::IsNullOrWhiteSpace($env:CODEX_HOME)) {
        return $env:CODEX_HOME
    }
    return (Join-Path $env:USERPROFILE ".codex")
}

function Assert-CodexProfile {
    if ([string]::IsNullOrWhiteSpace($CodexProfile)) {
        return
    }
    if ($CodexProfile -notmatch '^[A-Za-z0-9_.-]+$') {
        throw "CodexProfile must be a simple profile name, not a path or command line."
    }
    $profilePath = Join-Path (Get-CodexHome) "$CodexProfile.config.toml"
    if (-not (Test-Path -LiteralPath $profilePath)) {
        throw "Codex profile was not found: $profilePath"
    }
}

function Ensure-ZaiProxyToken {
    if ($CodexProfile -ne "zai-glm52") {
        return
    }
    if ([string]::IsNullOrWhiteSpace($env:ZAI_CODEX_PROXY_TOKEN)) {
        $env:ZAI_CODEX_PROXY_TOKEN = [guid]::NewGuid().ToString("N")
    }
}

function Test-ZaiProxyHealth {
    try {
        $response = Invoke-RestMethod -Uri "http://127.0.0.1:11452/v1/health" -TimeoutSec 2
        return (
            $response.status -eq "ok" -and
            $response.model -eq "glm-5.2" -and
            $response.has_api_key -eq $true -and
            $response.has_proxy_auth_token -eq $true
        )
    }
    catch {
        return $false
    }
}

function Stop-StaleZaiProxy {
    $proxyPidFile = Join-Path $ProxyDir "proxy.pid"
    if (-not (Test-Path -LiteralPath $proxyPidFile)) {
        return
    }
    $proxyPidText = (Get-Content -Raw -LiteralPath $proxyPidFile).Trim()
    if ($proxyPidText -match '^\d+$') {
        $proxyProcess = Get-Process -Id ([int]$proxyPidText) -ErrorAction SilentlyContinue
        if ($proxyProcess) {
            Stop-Process -Id ([int]$proxyPidText) -Force
            Start-Sleep -Milliseconds 500
        }
    }
}

function Start-ZaiProxyIfNeeded {
    if ($CodexProfile -ne "zai-glm52") {
        return
    }
    if (Test-ZaiProxyHealth) {
        return
    }
    if ([string]::IsNullOrWhiteSpace($env:ZAI_API_KEY)) {
        throw "ZAI_API_KEY is required for CodexProfile zai-glm52."
    }

    New-Item -ItemType Directory -Force -Path $ProxyDir | Out-Null
    Stop-StaleZaiProxy
    $proxyOut = Join-Path $ProxyDir "proxy.out.log"
    $proxyErr = Join-Path $ProxyDir "proxy.err.log"
    if ([string]::IsNullOrWhiteSpace($ProxyCommand)) {
        $proxyArgs = @(
            "-NoProfile",
            "-ExecutionPolicy", "Bypass",
            "-File", "`"$(Join-Path $PSScriptRoot 'zai-codex-proxy.ps1')`""
        )
    } else {
        $proxyArgs = @(
            "-NoProfile",
            "-ExecutionPolicy", "Bypass",
            "-Command", $ProxyCommand
        )
    }
    $process = Start-Process `
        -FilePath "powershell.exe" `
        -ArgumentList $proxyArgs `
        -WorkingDirectory $RepoRoot `
        -PassThru `
        -WindowStyle Hidden `
        -RedirectStandardOutput $proxyOut `
        -RedirectStandardError $proxyErr
    Set-Content -LiteralPath (Join-Path $ProxyDir "proxy.pid") -Value $process.Id -Encoding ASCII

    $deadline = (Get-Date).AddSeconds(30)
    while ((Get-Date) -lt $deadline) {
        if (Test-ZaiProxyHealth) {
            return
        }
        Start-Sleep -Milliseconds 500
    }
    throw "Z.AI Codex proxy did not become healthy. See $proxyErr"
}

function Write-VisualLoopState {
    param(
        [int]$Iteration,
        [string]$Status,
        [string]$ActiveSlice = $null,
        [string]$CurrentOwner = $null,
        [string]$NextVisualTarget = $null,
        [string]$ScreenshotVerdict = $null,
        [hashtable]$BlockingSeverityCount = $null,
        [object[]]$PolishBacklog = $null,
        [hashtable]$LastArtifacts = $null,
        [string]$FreshnessMarker = $null
    )
    $existing = $null
    if (Test-Path -LiteralPath $VisualLoopStateFile) {
        try {
            $existing = Get-Content -Raw -LiteralPath $VisualLoopStateFile | ConvertFrom-Json
        }
        catch {
            $existing = $null
        }
    }
    function Get-ExistingVisualValue {
        param([object]$Object, [string]$Name, [object]$Default)
        if ($null -ne $Object -and $Object.PSObject.Properties[$Name]) {
            return $Object.PSObject.Properties[$Name].Value
        }
        return $Default
    }
    $defaultArtifacts = [ordered]@{
        testSummary = "build/qa/test-mod-summary.md"
        visualReport = "build/visual-qa/visual-qa-report.md"
        visualReportJson = "build/visual-qa/visual-qa-report.json"
        screenshots = "build/visual-qa/screenshots"
        visualAssessment = "build/visual-qa/formic-visual-assessment.md"
        autonomousFinal = ""
        freshnessMarker = ""
    }
    $existingArtifacts = Get-ExistingVisualValue -Object $existing -Name "lastArtifacts" -Default $null
    if ($null -ne $existingArtifacts) {
        foreach ($property in $existingArtifacts.PSObject.Properties) {
            $defaultArtifacts[$property.Name] = $property.Value
        }
    }
    if ($null -ne $LastArtifacts) {
        foreach ($key in $LastArtifacts.Keys) {
            $defaultArtifacts[$key] = $LastArtifacts[$key]
        }
    }
    if (-not [string]::IsNullOrWhiteSpace($FreshnessMarker)) {
        $defaultArtifacts["freshnessMarker"] = $FreshnessMarker
    }

    $state = [ordered]@{
        status = $Status
        iteration = $Iteration
        activeSlice = if ($PSBoundParameters.ContainsKey("ActiveSlice") -and -not [string]::IsNullOrWhiteSpace($ActiveSlice)) { $ActiveSlice } else { Get-ExistingVisualValue -Object $existing -Name "activeSlice" -Default "renovation" }
        currentOwner = if ($PSBoundParameters.ContainsKey("CurrentOwner") -and -not [string]::IsNullOrWhiteSpace($CurrentOwner)) { $CurrentOwner } else { Get-ExistingVisualValue -Object $existing -Name "currentOwner" -Default "orchestrator" }
        acceptanceBrief = Get-ExistingVisualValue -Object $existing -Name "acceptanceBrief" -Default ""
        screenshotVerdict = if ($PSBoundParameters.ContainsKey("ScreenshotVerdict")) { $ScreenshotVerdict } else { Get-ExistingVisualValue -Object $existing -Name "screenshotVerdict" -Default "unknown" }
        blockingSeverityCount = if ($null -ne $BlockingSeverityCount) { $BlockingSeverityCount } else { Get-ExistingVisualValue -Object $existing -Name "blockingSeverityCount" -Default @{ P0 = 0; P1 = 0 } }
        polishBacklog = if ($null -ne $PolishBacklog) { $PolishBacklog } else { Get-ExistingVisualValue -Object $existing -Name "polishBacklog" -Default @() }
        lastArtifacts = $defaultArtifacts
        nextVisualTarget = if ($PSBoundParameters.ContainsKey("NextVisualTarget") -and -not [string]::IsNullOrWhiteSpace($NextVisualTarget)) { $NextVisualTarget } else { Get-ExistingVisualValue -Object $existing -Name "nextVisualTarget" -Default "choose next scoped visual track" }
        freshnessMarker = if (-not [string]::IsNullOrWhiteSpace($FreshnessMarker)) { $FreshnessMarker } else { Get-ExistingVisualValue -Object $existing -Name "freshnessMarker" -Default "" }
        updatedAt = (Get-Date).ToString("o")
    }
    $state | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $VisualLoopStateFile -Encoding UTF8
}

function Get-TrimmedFileText {
    param(
        [string]$Path,
        [int]$MaxChars = 12000
    )
    if (-not (Test-Path -LiteralPath $Path)) {
        return ""
    }
    $text = Get-Content -Raw -LiteralPath $Path
    if ($text.Length -le $MaxChars) {
        return $text
    }
    return $text.Substring($text.Length - $MaxChars)
}

function Get-VisualFailureHandoff {
    $manualDiagnosisPath = Join-Path $LoopDir "manual-visual-diagnosis.md"
    $manualDiagnosis = Get-TrimmedFileText -Path $manualDiagnosisPath -MaxChars 5000
    $assessment = Get-TrimmedFileText -Path $VisualAssessmentReport -MaxChars 12000
    $state = Get-TrimmedFileText -Path $VisualLoopStateFile -MaxChars 7000
    $brief = Get-TrimmedFileText -Path $VisualLoopBriefMd -MaxChars 9000
    $matrix = Get-TrimmedFileText -Path $VisualFeatureMatrixFile -MaxChars 9000

    $parts = @()
    if (-not [string]::IsNullOrWhiteSpace($brief)) {
        $parts += @(
            "Current visual loop brief:",
            '```markdown',
            $brief,
            '```'
        )
    }
    if (-not [string]::IsNullOrWhiteSpace($manualDiagnosis)) {
        $parts += @(
            "Manual image-capable diagnosis:",
            '```markdown',
            $manualDiagnosis,
            '```'
        )
    }
    if (-not [string]::IsNullOrWhiteSpace($assessment)) {
        $parts += @(
            "Latest independent GPT visual assessment:",
            '```markdown',
            $assessment,
            '```'
        )
    }
    if (-not [string]::IsNullOrWhiteSpace($state)) {
        $parts += @(
            "Current visual-loop state:",
            '```json',
            $state,
            '```'
        )
    }
    if (-not [string]::IsNullOrWhiteSpace($matrix)) {
        $parts += @(
            "Current visual feature matrix:",
            '```json',
            $matrix,
            '```'
        )
    }
    if ($parts.Count -eq 0) {
        return "No visual assessment handoff exists yet. Generate fresh screenshots and run scripts/openai-visual-assessment.cmd."
    }
    return ($parts -join [Environment]::NewLine)
}

function New-VisualFreshnessMarker {
    param([int]$Iteration, [string]$Stamp)
    $marker = Join-Path $LoopDir "iteration-$('{0:D3}' -f $Iteration)-$Stamp.visual-marker.txt"
    Set-Content -LiteralPath $marker -Value (Get-Date).ToUniversalTime().ToString("o") -Encoding ASCII
    return $marker
}

function Assert-FreshVisualArtifacts {
    param([string]$MarkerPath)
    if (-not (Test-Path -LiteralPath $MarkerPath)) {
        throw "Visual freshness marker is missing: $MarkerPath"
    }
    $markerTime = (Get-Item -LiteralPath $MarkerPath).LastWriteTimeUtc
    foreach ($requiredPath in @($VisualQaReportJson, $VisualQaReportMd, $VisualAssessmentReport)) {
        if (-not (Test-Path -LiteralPath $requiredPath)) {
            throw "Missing required visual artifact after iteration: $requiredPath"
        }
        if ((Get-Item -LiteralPath $requiredPath).LastWriteTimeUtc -le $markerTime) {
            throw "Visual artifact was not refreshed after iteration marker: $requiredPath"
        }
    }

    if (-not (Test-Path -LiteralPath $VisualScreenshotDir)) {
        throw "Missing visual screenshot directory: $VisualScreenshotDir"
    }
    $report = Get-Content -Raw -LiteralPath $VisualQaReportJson | ConvertFrom-Json
    if ($report.status -ne "passed") {
        throw "Visual QA report is not passed: $($report.status)"
    }
    $screenshots = @($report.screenshots)
    if ($screenshots.Count -eq 0) {
        throw "Visual QA report contains no screenshots."
    }
    $newestScreenshotTime = [datetime]::MinValue
    foreach ($entry in $screenshots) {
        if (-not $entry.PSObject.Properties["file"]) {
            throw "Visual QA report screenshot entry is missing file path."
        }
        $screenshotPath = Join-Path $VisualQaDir $entry.file
        if (-not (Test-Path -LiteralPath $screenshotPath)) {
            throw "Missing reported screenshot: $screenshotPath"
        }
        $screenshotItem = Get-Item -LiteralPath $screenshotPath
        if ($screenshotItem.LastWriteTimeUtc -le $markerTime) {
            throw "Screenshot was not refreshed after iteration marker: $screenshotPath"
        }
        if ($screenshotItem.LastWriteTimeUtc -gt $newestScreenshotTime) {
            $newestScreenshotTime = $screenshotItem.LastWriteTimeUtc
        }
    }
    if ((Get-Item -LiteralPath $VisualAssessmentReport).LastWriteTimeUtc -lt $newestScreenshotTime) {
        throw "Visual assessment is older than the newest screenshot: $VisualAssessmentReport"
    }
}

function New-IterationPrompt {
    param([int]$Iteration, [string]$FreshnessMarker)
    $visualHandoff = Get-VisualFailureHandoff
    @"
You are a local Codex autonomous development agent working in Formic Frontier.

Run exactly one small playable visual loop from the active Renovation Track, then stop and summarize.

Start by reading:
- docs/roadmap.md
- docs/autonomous-dev.md
- docs/visual-intent/formic-visual-intent.md
- docs/local-stack/index.md
- .codex/skills/formic-visual-assessment/SKILL.md
- build/autonomous-loop/visual-loop-state.json when present
- build/autonomous-loop/visual-feature-matrix.json when present
- build/autonomous-loop/visual-loop-brief.md when present
- build/autonomous-loop/visual-progress.jsonl tail when present

Critical visual handoff for this iteration:
$visualHandoff

Current operating constraints:
- This is local autonomous development. GitHub publishing is not available yet because this repo has no remote and gh is missing.
- The worktree can contain existing uncommitted baseline changes. Do not revert them.
- Repository root is: $RepoRoot
- CWD guard is mandatory. Start every shell command that reads or writes repo files with:
  `Set-Location -LiteralPath '$RepoRoot'; if (-not ((Get-Location).Path -ieq '$RepoRoot')) { throw 'Wrong cwd' }`
- Do not run repo-relative commands from sibling folders such as `2026-04-28\new-chat`; if a command reports the wrong cwd, fix cwd first and retry once.
- Preserve /formic qa scene <name> and every existing screenshot scene.
- Use subagents for separable work. Keep maker/checker separation: the worker who edits the feature must not perform final visual acceptance.
- Start from screenshot evidence and one visual target, not a broad code sweep.
- Freshness marker for this iteration: $FreshnessMarker. Visual QA reports, every reported screenshot, and formic-visual-assessment.md must be newer than this file.
- If existing build/visual-qa artifacts are older than the freshness marker, do not stop after reporting that fact. Immediately run scripts/gui-smoke.cmd to create fresh screenshots before inspecting or assessing them.
- Do not end the turn with a plan, intention, or "I need to..." message. Final response is allowed only after implementation plus required checks, or after a real command failure with the exact blocker.
- Prefer the next unfinished active Renovation Track slice in this order:
  1. R1 Settlement scale renovation
  2. R2 Architecture polish
  3. R3 Colony Tablet 2.0
- Do not continue the old generic Stage 1-7 roadmap while renovation work is unfinished.
- Keep the slice narrow enough to complete with tests and visual QA in this run.
- The visual feature matrix is the durable acceptance contract. Pick one open required row or one direct prerequisite for it. Do not mark a row pass without screenshot evidence and visual assessment support.
- Mechanics/playability is locked until every required visual_baseline row in build/autonomous-loop/visual-feature-matrix.json is status=pass.
- Visual QA must include the renovation scenes when available: settlement_scale, tablet_research_map, tablet_market, and tablet_requests.
- Do not run a testing-only or mechanics-only iteration while P2/P3 visual debt remains. Every iteration must have a visible target and a screenshot acceptance check.
- Visual priority order:
  1. settlement/building scale, monumental ant-hill mass, silhouettes, diversity, composition, camera framing, and roomy village feel;
  2. textures, assets, ant readability, caste silhouette quality, and visible animation/work cues;
  3. Colony Tablet/interface beauty, hierarchy, icons/cards, localization fit, and research/market/request presentation;
  4. mechanics and playability only after the visual baseline is materially better.
- R2 architecture scale requirements:
  - Do not be timid. Architecture polish is allowed to make buildings much larger, taller, and wider when screenshots need it.
  - Main mounds and landmark buildings should target roughly 20-30 blocks of vertical silhouette when complete, with broad 24-40 block footprints where appropriate.
  - Secondary buildings can be smaller, but they must still read as substantial chambers rather than 3-5 block huts, pads, or decorative markers.
  - Current user blocker: if colony_overview, colony_ground, or settlement_scale still reads as flat wide pads/pancake terrain with only a thin column, cap, or table-like crown, treat the visual target as failed. The next repair must change actual world structure generation/camera staging enough to show a broad 20-30 block organic ant-hill mass, not just widen low layers or tweak colors.
  - Prefer structural mass over surface decoration: layered mound shells, tunnel mouths, vertical shafts, ribs/buttresses, terraces, yards, crowns, brood/fungus/storage volumes, and role-specific entrances.
  - A slice that only adds a few accent blocks to tiny buildings is not enough for R2 unless the screenshot already proves the buildings have real scale.
  - You may adjust QA camera height/distance and prepared terrain radius so the larger structures are visible in settlement_scale, culture_styles, construction_stage, and repair_scene.
- You may change visual QA camera angles/framing in VisualQaScenes or client screenshot setup when the current angle hides the feature or makes beauty/readability impossible to assess. Preserve all scene names and expected screenshots.

Visual compute loop:
1. Use a scout-style pass to choose one scoped visual target and write the acceptance brief.
2. Assign exactly one worker ownership area: tablet UI, settlement/world, ants, assets/icons, or QA harness.
3. Implement only that target.
4. Generate fresh screenshots; never assess stale screenshots.
5. Use scripts/openai-visual-assessment.cmd for the separate visual assessor pass. This sends visual intent plus fresh PNG screenshots to OpenAI gpt-5.4-mini through Codex CLI auth and writes build/visual-qa/formic-visual-assessment.md. Do not synthesize the final visual verdict yourself with text-only GLM-5.2.
   - Default assessment transport is Codex CLI auth; do not block on OPENAI_API_KEY unless you explicitly switch to -Transport api.
   - Image assessment can take longer than five minutes for 19 screenshots plus references. Run it with a long command timeout when the harness allows it, and treat a local command timeout as "retry with more time or inspect background output", not as a missing API key.
6. If P0/P1 appears, repair only those findings or leave a precise blocker.
7. Use a final gatekeeper pass for commands and artifact freshness.

Artifact reporting contract:
- Update build/autonomous-loop/visual-loop-state.json after scout, build, smoke, assessment, and gate phases.
- Append one JSONL event to build/autonomous-loop/visual-progress.jsonl after scout, build, smoke, assessment, and gate phases. Include phase, owner, files changed, screenshots, verdict, matrix row ids, and next action.
- Update build/autonomous-loop/visual-feature-matrix.json for the active row with status fail/unknown/pass, evidence screenshots, lastVerdict, and nextAction. Do not delete rows.
- Keep acceptanceBrief, currentOwner, screenshotVerdict, blockingSeverityCount, polishBacklog, lastArtifacts, and nextVisualTarget current.
- The state file is a handoff contract for the next GLM iteration; do not leave it generic or stale.

Ownership boundaries:
- Tablet/UI: ColonyStatusScreen, UI snapshot/lang keys, tablet screenshots.
- Settlement/world: StructurePlacer, world portions of VisualQaScenes, settlement screenshots.
- Ants: render/model/entity textures, ant_lineup, work_cycle, colony_ground.
- Assets/icons: resource-pack textures/models/blockstates/items/equipment plus relevant screenshots.
- QA harness: `VisualQaClient`, smoke/report scripts, scene synchronization.

Maintain build/autonomous-loop/visual-loop-state.json with active slice, current owner, screenshot verdict, P0/P1 count, P2/P3 backlog, last artifact paths, and next visual target.

Definition of done for this iteration:
1. Implement the slice.
2. Run scripts/test-mod.cmd -AllowMissingGitHub.
3. Run scripts/gui-smoke.cmd.
4. Run scripts/openai-visual-assessment.cmd on the fresh build/visual-qa artifacts. The report must include Assessor: GPT-5.4 mini and be saved to build/visual-qa/formic-visual-assessment.md.
5. Run scripts/autonomous-gate.cmd -AllowMissingGitHub -NoLaunch.
6. If any command or visual assessment fails, fix the issue or clearly leave the iteration blocked with the exact blocker.

Mandatory command behavior:
- If screenshots are stale, scripts/gui-smoke.cmd is the next command, not a future recommendation.
- If scripts/gui-smoke.cmd cannot run because Minecraft/client automation is blocked, stop with BLOCKED: and include the command output path or exact error.
- Never accept or assess screenshots older than the freshness marker.

Final response must include:
- implemented slice
- visual target and screenshots inspected
- files changed
- test/gate results
- visual assessment verdict
- P0/P1 count and P2/P3 backlog
- next recommended slice

Autonomous loop iteration: $Iteration
"@
}

function New-GuardRetryPrompt {
    param(
        [int]$Iteration,
        [int]$Attempt,
        [string]$FreshnessMarker,
        [string]$FailureDetail,
        [string]$PreviousFinal
    )
    $visualHandoff = Get-VisualFailureHandoff
    @"
You are continuing Formic Frontier autonomous visual loop iteration $Iteration.

The previous attempt ended before the strict outer gate could accept it.

Failure to fix now:
$FailureDetail

Freshness marker for this retry attempt: $FreshnessMarker

Repository root is: $RepoRoot

CWD guard is mandatory. Start every shell command that reads or writes repo files with:
`Set-Location -LiteralPath '$RepoRoot'; if (-not ((Get-Location).Path -ieq '$RepoRoot')) { throw 'Wrong cwd' }`

Do not run repo-relative commands from sibling folders such as `2026-04-28\new-chat`; if a command reports the wrong cwd, fix cwd first and retry once.

Do not restart broad investigation. Continue the same scoped visual target from build/autonomous-loop/visual-loop-state.json.

Hard visual contract still applies:
- Mechanics/playability is locked until every required `visual_baseline` row in build/autonomous-loop/visual-feature-matrix.json is `pass`.
- Every retry must choose one open visual matrix row or a direct prerequisite; no testing-only or mechanics-only pass.
- R2 scale bar is intentionally high: broad organic mound/chamber masses, readable tunnel mouths, forest-floor density, and native Formic materials. Do not settle for low stepped pads, tiny 3-5 block huts, or decorative accent tweaks.
- Visual QA reports, every reported screenshot, and formic-visual-assessment.md must be newer than the retry freshness marker.
- Run fresh scripts/gui-smoke.cmd before visual assessment whenever screenshots are older than this retry marker.
- Use scripts/openai-visual-assessment.cmd for the independent image-capable verdict; do not synthesize final visual acceptance with text-only GLM.

Critical visual handoff:
$visualHandoff

If the manual image-capable diagnosis says a screenshot artifact is invalid (for example hotbar, toast, first-person hand, foliage blocking the subject, or sky-only framing), fix the QA harness/camera first. Do not keep changing world geometry from a bad screenshot.

Mandatory continuation behavior:
- If the failure says visual artifacts or screenshots were not refreshed, your first action must be to run scripts/gui-smoke.cmd.
- After fresh screenshots exist, run scripts/openai-visual-assessment.cmd and write build/visual-qa/formic-visual-assessment.md with Assessor: GPT-5.4 mini.
- `scripts/openai-visual-assessment.cmd` uses Codex CLI auth by default; do not require OPENAI_API_KEY unless using -Transport api.
- Then run scripts/autonomous-gate.cmd -AllowMissingGitHub -NoLaunch.
- Do not finish with an intention, plan, or "let me..." message.
- Final response is allowed only after fresh screenshots, fresh assessment, and the gate have completed, or after a real command failure with exact output/path.
- Keep build/autonomous-loop/visual-loop-state.json current while continuing.
- Keep build/autonomous-loop/visual-feature-matrix.json and build/autonomous-loop/visual-progress.jsonl current while continuing.

Previous final response:
$PreviousFinal

Continuation attempt: $Attempt
"@
}

function Invoke-CodexPrompt {
    param(
        [object]$CodexCommandInfo,
        [string]$PromptPath,
        [string]$JsonLog,
        [string]$ErrLog,
        [string]$FinalMessage,
        [int]$Iteration,
        [int]$Attempt
    )
    $codexArgs = @(
        "exec",
        "--cd", "$RepoRoot",
        "--sandbox", "danger-full-access",
        "--json",
        "-o", "$FinalMessage"
    )
    if (-not [string]::IsNullOrWhiteSpace($Model)) {
        $codexArgs += @("-m", $Model)
    }
    if (-not [string]::IsNullOrWhiteSpace($CodexProfile)) {
        $codexArgs += @("-p", $CodexProfile)
    }
    $codexArgs += "-"

    function ConvertTo-NativeArgument {
        param([string]$Argument)
        if ($null -eq $Argument -or $Argument.Length -eq 0) {
            return '""'
        }
        if ($Argument -notmatch '[\s"]') {
            return $Argument
        }
        $builder = [System.Text.StringBuilder]::new()
        [void]$builder.Append('"')
        $backslashes = 0
        foreach ($char in $Argument.ToCharArray()) {
            if ($char -eq '\') {
                $backslashes++
                continue
            }
            if ($char -eq '"') {
                if ($backslashes -gt 0) {
                    [void]$builder.Append('\' * ($backslashes * 2))
                    $backslashes = 0
                }
                [void]$builder.Append('\"')
                continue
            }
            if ($backslashes -gt 0) {
                [void]$builder.Append('\' * $backslashes)
                $backslashes = 0
            }
            [void]$builder.Append($char)
        }
        if ($backslashes -gt 0) {
            [void]$builder.Append('\' * ($backslashes * 2))
        }
        [void]$builder.Append('"')
        return $builder.ToString()
    }

    function Test-CodexTurnCompleted {
        param([string]$Path)
        if (-not (Test-Path -LiteralPath $Path)) {
            return $false
        }
        try {
            return [bool](Select-String -LiteralPath $Path -Pattern "turn.completed" -Quiet)
        }
        catch {
            return $false
        }
    }

    function Test-FinalMessageReady {
        param([string]$Path)
        if (-not (Test-Path -LiteralPath $Path)) {
            return $false
        }
        try {
            return ((Get-Item -LiteralPath $Path).Length -gt 0)
        }
        catch {
            return $false
        }
    }

    $prompt = Get-Content -Raw -LiteralPath $PromptPath
    $arguments = ($codexArgs | ForEach-Object { ConvertTo-NativeArgument "$_" }) -join " "
    $utf8NoBom = [System.Text.UTF8Encoding]::new($false)
    $outputWriter = [System.IO.StreamWriter]::new($JsonLog, $false, $utf8NoBom)
    $errorWriter = [System.IO.StreamWriter]::new($ErrLog, $false, $utf8NoBom)
    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo.FileName = $CodexCommandInfo.Source
    $process.StartInfo.Arguments = $arguments
    $process.StartInfo.WorkingDirectory = "$RepoRoot"
    $process.StartInfo.UseShellExecute = $false
    $process.StartInfo.CreateNoWindow = $true
    $process.StartInfo.RedirectStandardInput = $true
    $process.StartInfo.RedirectStandardOutput = $true
    $process.StartInfo.RedirectStandardError = $true
    $outputEvent = $null
    $errorEvent = $null
    $killedByWatchdog = $false
    $lastHeartbeat = [datetime]::MinValue
    try {
        $outputEvent = Register-ObjectEvent -InputObject $process -EventName OutputDataReceived -MessageData $outputWriter -Action {
            if ($null -ne $EventArgs.Data) {
                $Event.MessageData.WriteLine($EventArgs.Data)
                $Event.MessageData.Flush()
            }
        }
        $errorEvent = Register-ObjectEvent -InputObject $process -EventName ErrorDataReceived -MessageData $errorWriter -Action {
            if ($null -ne $EventArgs.Data) {
                $Event.MessageData.WriteLine($EventArgs.Data)
                $Event.MessageData.Flush()
            }
        }
        [void]$process.Start()
        Set-Content -LiteralPath $ChildPidFile -Value $process.Id -Encoding ASCII
        Write-LoopState -Status "running" -Iteration $Iteration -Detail "Codex child active attempt $Attempt" -ChildPid $process.Id -JsonLog $JsonLog -FinalMessage $FinalMessage
        $process.BeginOutputReadLine()
        $process.BeginErrorReadLine()
        $promptBytes = $utf8NoBom.GetBytes($prompt)
        $process.StandardInput.BaseStream.Write($promptBytes, 0, $promptBytes.Length)
        $process.StandardInput.BaseStream.Flush()
        $process.StandardInput.Close()

        while (-not $process.HasExited) {
            if (Test-Path -LiteralPath $StopFile) {
                $errorWriter.WriteLine("Stop requested; terminating Codex child PID $($process.Id).")
                $errorWriter.Flush()
                try { $process.Kill() } catch {}
                return 130
            }
            $now = (Get-Date).ToUniversalTime()
            $logItemForActivity = if (Test-Path -LiteralPath $JsonLog) { Get-Item -LiteralPath $JsonLog } else { $null }
            $idleMinutesForActivity = if ($null -ne $logItemForActivity) { ($now - $logItemForActivity.LastWriteTimeUtc).TotalMinutes } else { 0 }
            if (((Get-Date) - $lastHeartbeat).TotalSeconds -ge 60) {
                $lastHeartbeat = Get-Date
                Write-LoopState -Status "running" -Iteration $Iteration -Detail "Codex child active attempt $Attempt" -ChildPid $process.Id -JsonLog $JsonLog -FinalMessage $FinalMessage -IdleMinutes $idleMinutesForActivity
            }
            if ($CodexActivityWatchdogMinutes -gt 0 -and -not (Test-FinalMessageReady -Path $FinalMessage) -and $null -ne $logItemForActivity -and $idleMinutesForActivity -ge $CodexActivityWatchdogMinutes) {
                $errorWriter.WriteLine("Codex activity watchdog terminated child PID $($process.Id) after $([math]::Round($idleMinutesForActivity, 2)) minutes with no JSONL growth and no final output.")
                $errorWriter.Flush()
                try { $process.Kill() } catch {}
                return 124
            }
            if ($CodexCompletionWatchdogMinutes -gt 0 -and (Test-FinalMessageReady -Path $FinalMessage) -and (Test-CodexTurnCompleted -Path $JsonLog)) {
                $logItem = if (Test-Path -LiteralPath $JsonLog) { Get-Item -LiteralPath $JsonLog } else { $null }
                if ($null -ne $logItem) {
                    $idleMinutes = ((Get-Date).ToUniversalTime() - $logItem.LastWriteTimeUtc).TotalMinutes
                    if ($idleMinutes -ge $CodexCompletionWatchdogMinutes) {
                        $killedByWatchdog = $true
                        $errorWriter.WriteLine("Codex completion watchdog terminated child PID $($process.Id) after turn.completed, final output, and $([math]::Round($idleMinutes, 2)) idle minutes.")
                        $errorWriter.Flush()
                        try { $process.Kill() } catch {}
                        break
                    }
                }
            }
            Start-Sleep -Seconds 10
        }
        try { $process.WaitForExit() } catch {}
        if ($killedByWatchdog) {
            return 0
        }
        return $process.ExitCode
    }
    finally {
        if ($null -ne $outputEvent) {
            Unregister-Event -SubscriptionId $outputEvent.Id -ErrorAction SilentlyContinue
        }
        if ($null -ne $errorEvent) {
            Unregister-Event -SubscriptionId $errorEvent.Id -ErrorAction SilentlyContinue
        }
        $outputWriter.Dispose()
        $errorWriter.Dispose()
        Remove-Item -LiteralPath $ChildPidFile -Force -ErrorAction SilentlyContinue
        $process.Dispose()
    }
}

Push-Location $RepoRoot
try {
    New-Item -ItemType Directory -Force -Path $LoopDir | Out-Null
    Set-Content -LiteralPath $PidFile -Value $PID -Encoding ASCII
    if (Test-Path -LiteralPath $StopFile) {
        Remove-Item -LiteralPath $StopFile -Force
    }

    $java = Use-FormicJava -MinimumMajor 21
    if (-not $java.Ok) {
        throw "Java 21+ is required. $($java.Detail)"
    }

    Assert-CodexProfile
    Ensure-ZaiProxyToken
    Start-ZaiProxyIfNeeded

    $codex = Get-Command $CodexCommand -ErrorAction SilentlyContinue
    if (-not $codex) {
        throw "codex CLI is not available: $CodexCommand"
    }

    & (Join-Path $PSScriptRoot "doctor.ps1") -AllowMissingGitHub:$AllowMissingGitHub

    $iteration = 1
    while (($MaxIterations -le 0 -or $iteration -le $MaxIterations) -and -not (Test-Path -LiteralPath $StopFile)) {
        Write-LoopState -Status "running" -Iteration $iteration -Detail "Starting Codex iteration"
        $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
        $promptPath = Join-Path $LoopDir "iteration-$('{0:D3}' -f $iteration)-$stamp.prompt.md"
        $jsonLog = Join-Path $LoopDir "iteration-$('{0:D3}' -f $iteration)-$stamp.jsonl"
        $errLog = Join-Path $LoopDir "iteration-$('{0:D3}' -f $iteration)-$stamp.err.log"
        $finalMessage = Join-Path $LoopDir "iteration-$('{0:D3}' -f $iteration)-$stamp.final.md"
        $freshnessMarker = New-VisualFreshnessMarker -Iteration $iteration -Stamp $stamp

        Write-VisualLoopState `
            -Status "running" `
            -Iteration $iteration `
            -ScreenshotVerdict "pending" `
            -BlockingSeverityCount @{ P0 = 0; P1 = 0 } `
            -FreshnessMarker $freshnessMarker `
            -LastArtifacts @{ autonomousFinal = $finalMessage; freshnessMarker = $freshnessMarker }

        Write-VisualProgressEvent -Iteration $iteration -Event "iteration.started" -Detail "Created visual freshness marker and state." -Data @{ freshnessMarker = $freshnessMarker }
        & (Join-Path $PSScriptRoot "visual-loop-brief.ps1") -IntentDir "docs\visual-intent" | Out-Null
        New-IterationPrompt -Iteration $iteration -FreshnessMarker $freshnessMarker | Set-Content -LiteralPath $promptPath -Encoding UTF8

        $attempt = 1
        $iterationAccepted = $false
        $lastFailure = ""
        while (-not $iterationAccepted -and -not (Test-Path -LiteralPath $StopFile)) {
            if ($attempt -gt 1) {
                $retryStamp = Get-Date -Format "yyyyMMdd-HHmmss"
                $retrySuffix = "retry-$('{0:D2}' -f ($attempt - 1))"
                $previousFinalPath = $finalMessage
                $promptPath = Join-Path $LoopDir "iteration-$('{0:D3}' -f $iteration)-$retryStamp-$retrySuffix.prompt.md"
                $jsonLog = Join-Path $LoopDir "iteration-$('{0:D3}' -f $iteration)-$retryStamp-$retrySuffix.jsonl"
                $errLog = Join-Path $LoopDir "iteration-$('{0:D3}' -f $iteration)-$retryStamp-$retrySuffix.err.log"
                $finalMessage = Join-Path $LoopDir "iteration-$('{0:D3}' -f $iteration)-$retryStamp-$retrySuffix.final.md"
                $freshnessMarker = New-VisualFreshnessMarker -Iteration $iteration -Stamp "$retryStamp-$retrySuffix"
                $previousFinal = if (Test-Path -LiteralPath $previousFinalPath) { Get-Content -Raw -LiteralPath $previousFinalPath } else { "" }
                Write-VisualLoopState -Status "retrying" -Iteration $iteration -CurrentOwner "orchestrator" -FreshnessMarker $freshnessMarker -LastArtifacts @{ autonomousFinal = $finalMessage; freshnessMarker = $freshnessMarker }
                Write-VisualProgressEvent -Iteration $iteration -Event "iteration.retry_marker" -Detail "Created retry-specific visual freshness marker." -Data @{ attempt = $attempt; freshnessMarker = $freshnessMarker }
                & (Join-Path $PSScriptRoot "visual-loop-brief.ps1") -IntentDir "docs\visual-intent" | Out-Null
                New-GuardRetryPrompt -Iteration $iteration -Attempt $attempt -FreshnessMarker $freshnessMarker -FailureDetail $lastFailure -PreviousFinal $previousFinal | Set-Content -LiteralPath $promptPath -Encoding UTF8
                Write-VisualProgressEvent -Iteration $iteration -Event "iteration.retry_prompt" -Detail $lastFailure -Data @{ attempt = $attempt; prompt = $promptPath }
            }

            Write-LoopState -Status "running" -Iteration $iteration -Detail "Codex iteration attempt $attempt"
            Write-VisualLoopState -Status "running" -Iteration $iteration -CurrentOwner "orchestrator" -FreshnessMarker $freshnessMarker -LastArtifacts @{ autonomousFinal = $finalMessage; freshnessMarker = $freshnessMarker }
            $codexExit = Invoke-CodexPrompt -CodexCommandInfo $codex -PromptPath $promptPath -JsonLog $jsonLog -ErrLog $errLog -FinalMessage $finalMessage -Iteration $iteration -Attempt $attempt
            Write-VisualProgressEvent -Iteration $iteration -Event "codex.completed" -Detail "Codex child completed." -Data @{ attempt = $attempt; exitCode = $codexExit; final = $finalMessage; jsonLog = $jsonLog; errLog = $errLog }
            if ($codexExit -ne 0) {
                if (Test-CodexTransientFailure -JsonLog $jsonLog -ErrLog $errLog -FinalMessage $finalMessage) {
                    $isRateLimited = Test-CodexRateLimitFailure -JsonLog $jsonLog -ErrLog $errLog
                    $backoffSeconds = if ($isRateLimited) {
                        Get-CodexRateLimitBackoffSeconds -JsonLog $jsonLog -ErrLog $errLog -DefaultSeconds $RateLimitRetrySeconds -MaxSeconds $RateLimitMaxRetrySeconds
                    } else {
                        10
                    }
                    $retryCount = $attempt - 1
                    if ($isRateLimited) {
                        $lastFailure = "Z.AI upstream returned quota/rate-limit/overload on attempt $attempt. This is a provider capacity issue, not a visual implementation failure. Sleeping $backoffSeconds seconds, then continuing from current working tree changes without restarting broad investigation. See $errLog and $jsonLog."
                    } else {
                        $lastFailure = "Codex/Z.AI stream disconnected before completion on attempt $attempt. Treat this as a transient harness failure: continue from current working tree changes, run the required tests/screenshots/assessment/gate, and do not restart broad investigation. See $errLog and $jsonLog."
                    }
                    Write-VisualProgressEvent -Iteration $iteration -Event $(if ($isRateLimited) { "codex.rate_limited" } else { "codex.transient_failure" }) -Detail $lastFailure -Data @{ attempt = $attempt; retryCount = $retryCount; final = $finalMessage; jsonLog = $jsonLog; errLog = $errLog; backoffSeconds = $backoffSeconds }
                    Write-LoopState -Status $(if ($isRateLimited) { "rate_limited" } else { "retrying" }) -Iteration $iteration -Detail $lastFailure
                    Write-VisualLoopState -Status "retrying" -Iteration $iteration -CurrentOwner "orchestrator" -NextVisualTarget $lastFailure -FreshnessMarker $freshnessMarker -LastArtifacts @{ autonomousFinal = $finalMessage; freshnessMarker = $freshnessMarker }
                    if ((-not $isRateLimited) -and $MaxGuardRetries -gt 0 -and $retryCount -ge $MaxGuardRetries) {
                        Write-LoopState -Status "blocked" -Iteration $iteration -Detail "Transient Codex retry limit reached. Last failure: $lastFailure"
                        exit $codexExit
                    }
                    $attempt++
                    Start-Sleep -Seconds $backoffSeconds
                    continue
                }
                Write-LoopState -Status "blocked" -Iteration $iteration -Detail "Codex iteration exited with $codexExit; see $errLog"
                exit $codexExit
            }

            $lastFailure = ""
            Write-LoopState -Status "gating" -Iteration $iteration -Detail "Running autonomous gate after attempt $attempt"
            Write-VisualLoopState -Status "gating" -Iteration $iteration -CurrentOwner "gatekeeper" -NextVisualTarget "verify artifact freshness and visual assessment" -FreshnessMarker $freshnessMarker -LastArtifacts @{ autonomousFinal = $finalMessage; freshnessMarker = $freshnessMarker }
            Write-VisualProgressEvent -Iteration $iteration -Event "gate.started" -Detail "Running autonomous-gate after Codex attempt." -Data @{ attempt = $attempt }
            & (Join-Path $PSScriptRoot "autonomous-gate.ps1") -AllowMissingGitHub:$AllowMissingGitHub -NoLaunch -FreshnessMarker $freshnessMarker
            if ($LASTEXITCODE -ne 0) {
                $assessmentSnippet = Get-TrimmedFileText -Path $VisualAssessmentReport -MaxChars 8000
                $lastFailure = "Autonomous gate failed after iteration $iteration attempt $attempt. Run or fix scripts/autonomous-gate.cmd -AllowMissingGitHub -NoLaunch."
                if (-not [string]::IsNullOrWhiteSpace($assessmentSnippet)) {
                    $lastFailure += [Environment]::NewLine + [Environment]::NewLine + "Latest visual assessment excerpt:" + [Environment]::NewLine + $assessmentSnippet
                }
            } else {
                try {
                    Assert-FreshVisualArtifacts -MarkerPath $freshnessMarker
                }
                catch {
                    $lastFailure = $_.Exception.Message
                }
            }

            if ([string]::IsNullOrWhiteSpace($lastFailure)) {
                $iterationAccepted = $true
                Write-VisualProgressEvent -Iteration $iteration -Event "gate.passed" -Detail "Strict autonomous gate accepted iteration." -Data @{ attempt = $attempt }
                break
            }

            $retryCount = $attempt - 1
            Write-VisualLoopState -Status "retrying" -Iteration $iteration -CurrentOwner "gatekeeper" -NextVisualTarget $lastFailure -FreshnessMarker $freshnessMarker -LastArtifacts @{ autonomousFinal = $finalMessage; freshnessMarker = $freshnessMarker }
            Write-VisualProgressEvent -Iteration $iteration -Event "gate.failed" -Detail $lastFailure -Data @{ attempt = $attempt; retryCount = $retryCount }
            Write-LoopState -Status "retrying" -Iteration $iteration -Detail $lastFailure
            if ($MaxGuardRetries -gt 0 -and $retryCount -ge $MaxGuardRetries) {
                Write-VisualLoopState -Status "blocked" -Iteration $iteration -CurrentOwner "gatekeeper" -NextVisualTarget $lastFailure -FreshnessMarker $freshnessMarker -LastArtifacts @{ autonomousFinal = $finalMessage; freshnessMarker = $freshnessMarker }
                Write-LoopState -Status "blocked" -Iteration $iteration -Detail "Guard retry limit reached. Last failure: $lastFailure"
                exit 1
            }
            $attempt++
            Start-Sleep -Seconds 5
        }

        if (-not $iterationAccepted) {
            Write-LoopState -Status "stopped" -Iteration ($iteration - 1) -Detail "Loop stopped during iteration $iteration"
            break
        }

        Write-LoopState -Status "passed" -Iteration $iteration -Detail "Iteration $iteration passed strict gate"
        Write-VisualLoopState -Status "passed" -Iteration $iteration -CurrentOwner "orchestrator" -NextVisualTarget "select next open visual matrix row" -FreshnessMarker $freshnessMarker -LastArtifacts @{ autonomousFinal = $finalMessage; freshnessMarker = $freshnessMarker }
        & (Join-Path $PSScriptRoot "visual-loop-brief.ps1") -IntentDir "docs\visual-intent" | Out-Null
        $iteration++
        if ($PauseSeconds -gt 0 -and ($MaxIterations -le 0 -or $iteration -le $MaxIterations)) {
            Start-Sleep -Seconds $PauseSeconds
        }
    }

    Write-LoopState -Status "stopped" -Iteration ($iteration - 1) -Detail "Loop stopped by limit or stop file"
}
catch {
    New-Item -ItemType Directory -Force -Path $LoopDir | Out-Null
    Write-LoopState -Status "error" -Iteration 0 -Detail $_.Exception.Message
    throw
}
finally {
    Pop-Location
}
