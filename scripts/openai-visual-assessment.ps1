param(
    [string]$VisualQaDir = "build\visual-qa",
    [string]$Output = "build\visual-qa\formic-visual-assessment.md",
    [string]$IntentDir = "docs\visual-intent",
    [string]$Model = "gpt-5.4-mini",
    [string]$Assessor = "GPT-5.4 mini",
    [string]$CodexCommand = "codex",
    [ValidateSet("codex", "api")]
    [string]$Transport = "codex",
    [string]$Endpoint = "",
    [string]$EnvKey = "OPENAI_API_KEY",
    [string]$Detail = "high",
    [string]$ReasoningEffort = "low",
    [int]$TimeoutSeconds = 300,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$LoopDir = Join-Path $RepoRoot "build\autonomous-loop"

Push-Location $RepoRoot
try {
    if ($Transport -eq "api") {
        $argsList = @(
            "tools\openai_visual_assessment.py",
            "--visual-qa-dir", $VisualQaDir,
            "--output", $Output,
            "--intent-dir", $IntentDir,
            "--model", $Model,
            "--assessor", $Assessor,
            "--env-key", $EnvKey,
            "--detail", $Detail,
            "--reasoning-effort", $ReasoningEffort,
            "--timeout", "$TimeoutSeconds"
        )
        if (-not [string]::IsNullOrWhiteSpace($Endpoint)) {
            $argsList += @("--endpoint", $Endpoint)
        }
        if ($DryRun) {
            $argsList += "--dry-run"
        }
        & python @argsList
        exit $LASTEXITCODE
    }

    $codex = Get-Command $CodexCommand -ErrorAction SilentlyContinue
    if (-not $codex) {
        throw "codex CLI is not available: $CodexCommand"
    }

    $visualQaPath = Resolve-Path $VisualQaDir
    $intentPath = if ([System.IO.Path]::IsPathRooted($IntentDir)) { $IntentDir } else { Join-Path $RepoRoot $IntentDir }
    if (-not (Test-Path -LiteralPath $intentPath)) {
        throw "Missing visual intent directory: $intentPath"
    }
    $intentDocPath = Join-Path $intentPath "formic-visual-intent.md"
    if (-not (Test-Path -LiteralPath $intentDocPath)) {
        throw "Missing visual intent doc: $intentDocPath"
    }
    $intentDoc = Get-Content -Raw -LiteralPath $intentDocPath
    $manifestPath = Join-Path $intentPath "reference-manifest.json"
    $manifestText = if (Test-Path -LiteralPath $manifestPath) { Get-Content -Raw -LiteralPath $manifestPath } else { "{}" }
    $referenceImages = @()
    if (Test-Path -LiteralPath $manifestPath) {
        try {
            $manifest = Get-Content -Raw -LiteralPath $manifestPath | ConvertFrom-Json
            foreach ($ref in @($manifest.references)) {
                if ($ref.PSObject.Properties["file"] -and -not [string]::IsNullOrWhiteSpace($ref.file)) {
                    $refPath = Join-Path $intentPath $ref.file
                    if (Test-Path -LiteralPath $refPath) {
                        $referenceImages += (Resolve-Path $refPath).Path
                    }
                }
            }
        }
        catch {
            throw "Invalid visual intent reference manifest: $manifestPath"
        }
    }
    $featureMatrixPath = Join-Path $RepoRoot "build\autonomous-loop\visual-feature-matrix.json"
    if (-not (Test-Path -LiteralPath $featureMatrixPath)) {
        $featureMatrixPath = Join-Path $intentPath "visual-feature-matrix.template.json"
    }
    $featureMatrix = if (Test-Path -LiteralPath $featureMatrixPath) { Get-Content -Raw -LiteralPath $featureMatrixPath } else { "{}" }
    $reportJson = Join-Path $visualQaPath "visual-qa-report.json"
    if (-not (Test-Path -LiteralPath $reportJson)) {
        throw "Missing visual QA report: $reportJson"
    }
    $report = Get-Content -Raw -LiteralPath $reportJson | ConvertFrom-Json
    if ($report.status -ne "passed") {
        throw "Visual QA report status is not passed: $($report.status)"
    }

    $expected = @(
        "colony_overview.png",
        "colony_ground.png",
        "ant_lineup.png",
        "work_cycle.png",
        "tablet_en.png",
        "tablet_ru.png",
        "tablet_guide.png",
        "tablet_trade.png",
        "tablet_research_map.png",
        "tablet_market.png",
        "tablet_requests.png",
        "progression_scene.png",
        "settlement_scale.png",
        "construction_stage.png",
        "repair_scene.png",
        "culture_styles.png",
        "diplomacy_scene.png",
        "worldgen_encounter.png",
        "endgame_project.png"
    )
    $screenshots = @()
    foreach ($name in $expected) {
        $path = Join-Path $visualQaPath "screenshots\$name"
        if (-not (Test-Path -LiteralPath $path)) {
            throw "Missing screenshot: $path"
        }
        $screenshots += (Resolve-Path $path).Path
    }

    New-Item -ItemType Directory -Force -Path $LoopDir | Out-Null
    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $promptPath = Join-Path $LoopDir "openai-visual-assessment-$stamp.prompt.md"
    $jsonLog = Join-Path $LoopDir "openai-visual-assessment-$stamp.jsonl"
    $errLog = Join-Path $LoopDir "openai-visual-assessment-$stamp.err.log"
    $outPath = if ([System.IO.Path]::IsPathRooted($Output)) { $Output } else { Join-Path $RepoRoot $Output }

    $reportMd = if (Test-Path -LiteralPath (Join-Path $visualQaPath "visual-qa-report.md")) { Get-Content -Raw -LiteralPath (Join-Path $visualQaPath "visual-qa-report.md") } else { "" }
    $latestLogPath = Join-Path $visualQaPath "latest.log"
    $latestLog = if (Test-Path -LiteralPath $latestLogPath) { (Get-Content -Raw -LiteralPath $latestLogPath) } else { "" }
    if ($latestLog.Length -gt 12000) { $latestLog = $latestLog.Substring($latestLog.Length - 12000) }
    $loopStatePath = Join-Path $RepoRoot "build\autonomous-loop\visual-loop-state.json"
    $loopState = if (Test-Path -LiteralPath $loopStatePath) { Get-Content -Raw -LiteralPath $loopStatePath } else { "{}" }
    $rubricPath = Join-Path $RepoRoot ".codex\skills\formic-visual-assessment\references\rubric.md"
    $templatePath = Join-Path $RepoRoot ".codex\skills\formic-visual-assessment\references\report-template.md"
    $rubric = if (Test-Path -LiteralPath $rubricPath) { Get-Content -Raw -LiteralPath $rubricPath } else { "" }
    $template = if (Test-Path -LiteralPath $templatePath) { Get-Content -Raw -LiteralPath $templatePath } else { "" }

    $promptParts = @(
        "You are the strict visual QA checker for the Formic Frontier Minecraft mod.",
        "",
        "You are $Assessor and MUST judge the attached screenshots visually. Do not rely only on pixel-count summaries.",
        "Use the attached images at original/full resolution where possible.",
        "",
        "Write only the final Markdown report. Do not edit repo files and do not run tools.",
        "The report must start exactly with:",
        "",
        "# Formic Visual Assessment",
        "",
        "Verdict: FAIL | PASS WITH NOTES | PASS",
        "Assessor: $Assessor",
        "Model: $Model",
        "",
        "Strict rules:",
        "- Any P0 or P1 finding means Verdict: FAIL.",
        "- Treat missing, blank, crashed, stale, or unreadable scenes as P0.",
        "- Treat core visual readability/playability problems as P1.",
        "- For non-tablet world screenshots, visible Minecraft HUD, hotbar, crosshair, player hand, tutorial/toast overlays, or foreground foliage blocking the subject are invalid QA artifacts. Mark P1 and require the QA camera/client capture harness to be fixed before judging architecture from that scene.",
        "- For R2 architecture work, be harsh: if buildings are merely wider but look vertically truncated, pancake-like, flat arcade pads, or tiny 3-5 block huts, mark at least P1 when the slice promised monumental scale.",
        "- Current hard visual bar: colony_overview, colony_ground, and settlement_scale must show real 20-30 block vertical ant-hill/settlement silhouettes. If the hub still reads as a broad flat pad with a thin column, cap, or table-like crown, mark P1 and Verdict: FAIL; do not downgrade this to P2 polish.",
        "- Do not let one central tall mound carry the whole architecture verdict. For PASS, at least the central landmark and several surrounding role buildings must read as substantial mound/chamber volumes with height, mass, entrances, and organic silhouette. If the central mound is tall but most surrounding buildings remain low stepped pads with decorative towers, mark that as at least P2, and P1 if the current slice claims R2 scale completion.",
        "- Passing architecture needs visible base-to-peak mound mass, organic taper, readable height, role-specific chamber forms, tunnel mouths/entrances, and multiple landmarks that feel built upward rather than just spread wider on the ground.",
        "- The visual intent pack is mandatory art direction. References are not a shader dependency, but PASS requires family resemblance in shape, scale, chamber density, insect realism, and forest-floor life.",
        "- PASS is forbidden if required visual-feature matrix rows remain visually unproven: multiple large organic chambers, visible tunnel mouths, realistic insects, forest-floor density, and no single-mound-only pass.",
        "- Do not accept `"more structure pixels`" as beauty. Judge silhouette, height, massing, groundedness, camera framing, and Minecraft-fit by sight.",
        "- Every issue must include scene, visible evidence, player impact, concrete fix direction, and acceptance check.",
        "- Mention relevant matrix row ids when a finding blocks or advances visual baseline.",
        "- Mention every screenshot you inspected.",
        "- Fill the ``## Reference Diff`` section: compare the wide colony shots (colony_overview, settlement_scale, culture_styles, endgame_project) directly against the attached mega-nest references and name the topology (ONE carved mound mass vs N separate cones), silhouette (broad dome vs steep cone), chamber style (irregular deep mouths vs grid holes), and the single biggest gap. If the overall form is wrong, state explicitly that the next fix must be a representational change, not a parameter tweak.",
        "- Fill the ``## Matrix Scorecard`` section: one line per required visual_baseline matrix row as ``<row_id>: pass|partial|fail|unknown - score N/5 - <one concrete next instruction>``. Use ``unknown`` only when a scene cannot prove the row, and say which capture is missing.",
        "- Anti-repeat: if the visual-loop state or a matrix row lastVerdict shows that row failing for the same root cause across 2+ attempts, prefix its scorecard line with ``REPEAT:`` and make the instruction a different generator/algorithm/shape, not another center-offset, radius, or taper tweak of the failing approach.",
        "",
        "Attached reference images, when present, appear before screenshots and are art-direction references only:",
        '```json',
        $manifestText,
        '```',
        "",
        "Attached screenshots, in order:"
    )
    foreach ($name in $expected) {
        $promptParts += "- build/visual-qa/screenshots/$name"
    }
    $promptParts += @(
        "",
        "Current visual QA report:",
        '```markdown',
        $reportMd,
        '```',
        "",
        "Current autonomous visual-loop state:",
        '```json',
        $loopState,
        '```',
        "",
        "Visual intent pack:",
        '```markdown',
        $intentDoc,
        '```',
        "",
        "Visual feature matrix:",
        '```json',
        $featureMatrix,
        '```',
        "",
        "Assessment rubric:",
        '```markdown',
        $rubric,
        '```',
        "",
        "Report template:",
        '```markdown',
        $template,
        '```',
        "",
        "Latest Minecraft visual QA log tail:",
        '```text',
        $latestLog,
        '```'
    )
    [System.IO.File]::WriteAllText($promptPath, ($promptParts -join [Environment]::NewLine), [System.Text.UTF8Encoding]::new($false))

    if ($DryRun) {
        Write-Host "Codex visual assessment dry run OK: $($screenshots.Count) screenshots, $($referenceImages.Count) reference images, model=$Model, output=$outPath, prompt=$promptPath"
        exit 0
    }

    $codexArgs = @(
        "exec",
        "--cd", "$RepoRoot",
        "--sandbox", "read-only",
        "--ephemeral",
        "--json",
        "-m", $Model,
        "-o", $outPath
    )
    foreach ($image in $referenceImages) {
        $codexArgs += @("-i", $image)
    }
    foreach ($image in $screenshots) {
        $codexArgs += @("-i", $image)
    }
    $codexArgs += "-"

    $prompt = Get-Content -Raw -LiteralPath $promptPath
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $prompt | & $codex.Source @codexArgs 1> $jsonLog 2> $errLog
        $exitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($exitCode -ne 0) {
        Write-Host "Codex visual assessment failed. Logs: $jsonLog $errLog"
        exit $exitCode
    }
    Write-Host "Codex visual assessment written: $outPath"
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
