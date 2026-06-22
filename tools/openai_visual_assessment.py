from __future__ import annotations

import argparse
import base64
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_VISUAL_QA_DIR = ROOT / "build/visual-qa"
DEFAULT_OUTPUT = DEFAULT_VISUAL_QA_DIR / "formic-visual-assessment.md"
DEFAULT_INTENT_DIR = ROOT / "docs/visual-intent"
DEFAULT_ENDPOINT = "https://api.openai.com/v1/responses"
DEFAULT_MODEL = "gpt-5.4-mini"
DEFAULT_ASSESSOR = "GPT-5.4 mini"

SCENE_ORDER = [
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
    "endgame_project.png",
]


def read_text(path: Path, limit: int | None = None) -> str:
    if not path.exists():
        return f"(missing: {path.relative_to(ROOT) if path.is_relative_to(ROOT) else path})"
    text = path.read_text(encoding="utf-8", errors="replace")
    if limit is not None and len(text) > limit:
        return text[-limit:]
    return text


def load_report(visual_qa_dir: Path) -> dict[str, object]:
    report_path = visual_qa_dir / "visual-qa-report.json"
    if not report_path.exists():
        raise FileNotFoundError(f"Missing visual QA report: {report_path}")
    with report_path.open("r", encoding="utf-8") as handle:
        report = json.load(handle)
    if not isinstance(report, dict):
        raise ValueError(f"{report_path} must contain a JSON object")
    if report.get("status") != "passed":
        raise ValueError(f"Visual QA report status is not passed: {report.get('status')}")
    return report


def screenshot_paths(visual_qa_dir: Path, report: dict[str, object]) -> list[Path]:
    by_name: dict[str, Path] = {}
    for entry in report.get("screenshots", []):
        if isinstance(entry, dict) and isinstance(entry.get("file"), str):
            path = visual_qa_dir / entry["file"]
            by_name[path.name] = path
    paths = [by_name[name] for name in SCENE_ORDER if name in by_name]
    missing = [name for name in SCENE_ORDER if name not in by_name]
    if missing:
        raise FileNotFoundError("Missing expected screenshots in report: " + ", ".join(missing))
    for path in paths:
        if not path.exists():
            raise FileNotFoundError(f"Missing screenshot file: {path}")
        if path.stat().st_size < 1024:
            raise ValueError(f"Screenshot is too small to assess: {path}")
    return paths


def data_url(path: Path) -> str:
    encoded = base64.b64encode(path.read_bytes()).decode("ascii")
    return f"data:image/png;base64,{encoded}"


def load_intent(intent_dir: Path) -> tuple[str, str, str, list[Path]]:
    intent_doc_path = intent_dir / "formic-visual-intent.md"
    if not intent_doc_path.exists():
        raise FileNotFoundError(f"Missing visual intent doc: {intent_doc_path}")
    manifest_path = intent_dir / "reference-manifest.json"
    template_matrix_path = intent_dir / "visual-feature-matrix.template.json"
    runtime_matrix_path = ROOT / "build/autonomous-loop/visual-feature-matrix.json"
    intent_doc = read_text(intent_doc_path, limit=20000)
    manifest_text = read_text(manifest_path, limit=12000) if manifest_path.exists() else "{}"
    matrix_path = runtime_matrix_path if runtime_matrix_path.exists() else template_matrix_path
    matrix_text = read_text(matrix_path, limit=22000)
    reference_paths: list[Path] = []
    if manifest_path.exists():
        with manifest_path.open("r", encoding="utf-8") as handle:
            manifest = json.load(handle)
        for entry in manifest.get("references", []):
            if not isinstance(entry, dict) or not isinstance(entry.get("file"), str):
                continue
            path = intent_dir / entry["file"]
            if path.exists() and path.stat().st_size >= 1024:
                reference_paths.append(path)
    return intent_doc, manifest_text, matrix_text, reference_paths


def build_prompt(
    visual_qa_dir: Path,
    output: Path,
    model: str,
    assessor: str,
    intent_doc: str,
    manifest_text: str,
    matrix_text: str,
) -> str:
    report_md = read_text(visual_qa_dir / "visual-qa-report.md", limit=12000)
    latest_log = read_text(visual_qa_dir / "latest.log", limit=12000)
    loop_state = read_text(ROOT / "build/autonomous-loop/visual-loop-state.json", limit=16000)
    rubric = read_text(ROOT / ".codex/skills/formic-visual-assessment/references/rubric.md", limit=18000)
    template = read_text(ROOT / ".codex/skills/formic-visual-assessment/references/report-template.md", limit=12000)
    rel_output = output.relative_to(ROOT) if output.is_relative_to(ROOT) else output

    return f"""You are the strict visual QA checker for the Formic Frontier Minecraft mod.

You are {assessor} and MUST judge the attached screenshots visually. Do not rely only on pixel-count summaries.
Write a final Markdown report for `{rel_output}`. The report must start exactly with:

# Formic Visual Assessment

Verdict: FAIL | PASS WITH NOTES | PASS
Assessor: {assessor}
Model: {model}

Strict rules:
- Any P0 or P1 finding means Verdict: FAIL.
- Treat missing, blank, crashed, stale, or unreadable scenes as P0.
- Treat core visual readability/playability problems as P1.
- For R2 architecture work, be harsh: if buildings are merely wider but look vertically truncated, pancake-like, flat arcade pads, or tiny 3-5 block huts, mark at least P1 when the slice promised monumental scale.
- The visual intent pack is mandatory art direction. References are not a shader dependency, but PASS requires family resemblance in shape, scale, chamber density, insect realism, and forest-floor life.
- PASS is forbidden if required visual-feature matrix rows remain visually unproven: multiple large organic chambers, visible tunnel mouths, realistic insects, forest-floor density, and no single-mound-only pass.
- Do not accept "more structure pixels" as beauty. Judge silhouette, height, massing, groundedness, camera framing, and Minecraft-fit by sight.
- Every issue must include scene, visible evidence, player impact, concrete fix direction, and acceptance check.
- Mention relevant matrix row ids when a finding blocks or advances visual baseline.
- Mention every screenshot you inspected.

Attached reference images, when present, are provided before screenshots and are art-direction references only:
```json
{manifest_text}
```

Current visual QA report:
```markdown
{report_md}
```

Current autonomous visual-loop state:
```json
{loop_state}
```

Visual intent pack:
```markdown
{intent_doc}
```

Visual feature matrix:
```json
{matrix_text}
```

Assessment rubric:
```markdown
{rubric}
```

Report template:
```markdown
{template}
```

Latest Minecraft visual QA log tail:
```text
{latest_log}
```
"""


def build_input(
    visual_qa_dir: Path,
    output: Path,
    model: str,
    assessor: str,
    paths: list[Path],
    reference_paths: list[Path],
    detail: str,
    intent_doc: str,
    manifest_text: str,
    matrix_text: str,
) -> list[dict[str, object]]:
    content: list[dict[str, object]] = [
        {
            "type": "input_text",
            "text": build_prompt(visual_qa_dir, output, model, assessor, intent_doc, manifest_text, matrix_text),
        }
    ]
    for path in reference_paths:
        rel = path.relative_to(ROOT) if path.is_relative_to(ROOT) else path
        content.append({"type": "input_text", "text": f"Art direction reference: {rel}"})
        content.append({"type": "input_image", "image_url": data_url(path), "detail": detail})
    for path in paths:
        content.append({"type": "input_text", "text": f"Screenshot: build/visual-qa/screenshots/{path.name}"})
        content.append({"type": "input_image", "image_url": data_url(path), "detail": detail})
    return [{"role": "user", "content": content}]


def post_response(endpoint: str, api_key: str, payload: dict[str, object], timeout: int) -> dict[str, object]:
    body = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        endpoint,
        data=body,
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
            "Accept": "application/json",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {error.code} from {endpoint}: {detail[:1600]}") from error


def extract_output_text(response: dict[str, object]) -> str:
    output_text = response.get("output_text")
    if isinstance(output_text, str) and output_text.strip():
        return output_text.strip()
    output = response.get("output")
    if isinstance(output, list):
        parts: list[str] = []
        for item in output:
            if not isinstance(item, dict):
                continue
            content = item.get("content")
            if not isinstance(content, list):
                continue
            for part in content:
                if isinstance(part, dict):
                    text = part.get("text") or part.get("output_text")
                    if isinstance(text, str):
                        parts.append(text)
        if parts:
            return "\n".join(parts).strip()
    raise ValueError("Response is missing output text")


def normalize_report(text: str, model: str, assessor: str) -> str:
    text = text.strip()
    if not text.startswith("# Formic Visual Assessment"):
        text = "# Formic Visual Assessment\n\n" + text
    if not re.search(rf"(?im)^\s*Assessor\s*:\s*{re.escape(assessor)}\s*$", text):
        text = re.sub(
            r"(?im)^(\s*Verdict\s*:\s*(?:FAIL|PASS WITH NOTES|PASS)\s*)$",
            rf"\1\nAssessor: {assessor}\nModel: {model}",
            text,
            count=1,
        )
    if not re.search(rf"(?im)^\s*Assessor\s*:\s*{re.escape(assessor)}\s*$", text):
        text = text.replace("# Formic Visual Assessment", f"# Formic Visual Assessment\n\nAssessor: {assessor}\nModel: {model}", 1)
    return text.rstrip() + "\n"


def write_blocked_report(output: Path, reason: str, model: str, assessor: str) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(
        "# Formic Visual Assessment\n\n"
        "Verdict: FAIL\n"
        f"Assessor: {assessor}\n"
        f"Model: {model}\n\n"
        "## Blockers\n\n"
        f"1. [P0] {assessor} visual assessment could not complete\n"
        "   Scene: build/visual-qa/screenshots/*.png\n"
        f"   Evidence: {reason}\n"
        "   Impact: The visual gate cannot trust a text-only or stale assessment.\n"
        "   Fix: Restore OpenAI API access and rerun `scripts\\openai-visual-assessment.cmd` after fresh `scripts\\gui-smoke.cmd` artifacts exist.\n"
        f"   Acceptance: `formic-visual-assessment.md` is regenerated by {assessor} and contains no P0/P1 findings.\n",
        encoding="utf-8",
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--visual-qa-dir", default=str(DEFAULT_VISUAL_QA_DIR))
    parser.add_argument("--output", default=str(DEFAULT_OUTPUT))
    parser.add_argument("--intent-dir", default=str(DEFAULT_INTENT_DIR))
    parser.add_argument("--model", default=os.environ.get("OPENAI_VISION_MODEL", DEFAULT_MODEL))
    parser.add_argument("--assessor", default=os.environ.get("OPENAI_VISION_ASSESSOR", DEFAULT_ASSESSOR))
    parser.add_argument("--endpoint", default=os.environ.get("OPENAI_RESPONSES_ENDPOINT", DEFAULT_ENDPOINT))
    parser.add_argument("--env-key", default="OPENAI_API_KEY")
    parser.add_argument("--timeout", type=int, default=300)
    parser.add_argument("--max-output-tokens", type=int, default=8192)
    parser.add_argument("--detail", default="high", choices=["low", "high", "auto"])
    parser.add_argument("--reasoning-effort", default="low", choices=["none", "low", "medium", "high", "xhigh"])
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    visual_qa_dir = Path(args.visual_qa_dir)
    if not visual_qa_dir.is_absolute():
        visual_qa_dir = ROOT / visual_qa_dir
    output = Path(args.output)
    if not output.is_absolute():
        output = ROOT / output
    intent_dir = Path(args.intent_dir)
    if not intent_dir.is_absolute():
        intent_dir = ROOT / intent_dir

    try:
        report = load_report(visual_qa_dir)
        paths = screenshot_paths(visual_qa_dir, report)
        intent_doc, manifest_text, matrix_text, reference_paths = load_intent(intent_dir)
    except Exception as exception:  # noqa: BLE001
        write_blocked_report(output, str(exception), args.model, args.assessor)
        print(f"OpenAI visual assessment blocked before API call: {exception}")
        return 2

    total_bytes = sum(path.stat().st_size for path in paths)
    if args.dry_run:
        print(
            "OpenAI visual assessment dry run OK: "
            f"{len(paths)} screenshots, {len(reference_paths)} reference images, {total_bytes} screenshot bytes, "
            f"model={args.model}, detail={args.detail}, output={output}"
        )
        return 0

    api_key = os.environ.get(args.env_key)
    if not api_key:
        reason = f"{args.env_key} is not set"
        write_blocked_report(output, reason, args.model, args.assessor)
        print(f"OpenAI visual assessment blocked: {reason}")
        return 2

    payload: dict[str, object] = {
        "model": args.model,
        "input": build_input(
            visual_qa_dir,
            output,
            args.model,
            args.assessor,
            paths,
            reference_paths,
            args.detail,
            intent_doc,
            manifest_text,
            matrix_text,
        ),
        "max_output_tokens": args.max_output_tokens,
        "reasoning": {"effort": args.reasoning_effort},
        "text": {"verbosity": "medium"},
    }

    started = time.time()
    try:
        response = post_response(args.endpoint, api_key, payload, args.timeout)
        report_text = normalize_report(extract_output_text(response), args.model, args.assessor)
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(report_text, encoding="utf-8")
        elapsed = time.time() - started
        print(f"OpenAI visual assessment written: {output} ({elapsed:.1f}s via {args.endpoint})")
        return 0
    except Exception as exception:  # noqa: BLE001
        reason = str(exception)
        write_blocked_report(output, reason, args.model, args.assessor)
        print(f"OpenAI visual assessment failed: {reason}")
        return 1


if __name__ == "__main__":
    sys.exit(main())
