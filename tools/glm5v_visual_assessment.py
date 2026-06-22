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
DEFAULT_ENDPOINTS = [
    "https://api.z.ai/api/paas/v4/chat/completions",
    "https://api.z.ai/api/coding/paas/v4/chat/completions",
]
DEFAULT_MODEL = "glm-5v-turbo"

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


def build_prompt(visual_qa_dir: Path, output: Path, model: str, intent_doc: str, manifest_text: str, matrix_text: str) -> str:
    report_md = read_text(visual_qa_dir / "visual-qa-report.md", limit=12000)
    latest_log = read_text(visual_qa_dir / "latest.log", limit=12000)
    loop_state = read_text(ROOT / "build/autonomous-loop/visual-loop-state.json", limit=16000)
    rubric = read_text(ROOT / ".codex/skills/formic-visual-assessment/references/rubric.md", limit=18000)
    template = read_text(ROOT / ".codex/skills/formic-visual-assessment/references/report-template.md", limit=12000)
    rel_output = output.relative_to(ROOT) if output.is_relative_to(ROOT) else output

    return f"""You are the strict visual QA checker for the Formic Frontier Minecraft mod.

You are GLM-5V-Turbo and MUST judge the attached screenshots visually. Do not rely only on pixel-count summaries.
Write a final Markdown report for `{rel_output}`. The report must start exactly with:

# Formic Visual Assessment

Verdict: FAIL | PASS WITH NOTES | PASS
Assessor: GLM-5V-Turbo
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


def build_messages(
    visual_qa_dir: Path,
    output: Path,
    model: str,
    paths: list[Path],
    reference_paths: list[Path],
    intent_doc: str,
    manifest_text: str,
    matrix_text: str,
) -> list[dict[str, object]]:
    content: list[dict[str, object]] = [{"type": "text", "text": build_prompt(visual_qa_dir, output, model, intent_doc, manifest_text, matrix_text)}]
    for path in reference_paths:
        rel = path.relative_to(ROOT) if path.is_relative_to(ROOT) else path
        content.append({"type": "text", "text": f"Art direction reference: {rel}"})
        content.append({"type": "image_url", "image_url": {"url": data_url(path)}})
    for path in paths:
        content.append({"type": "text", "text": f"Screenshot: build/visual-qa/screenshots/{path.name}"})
        content.append({"type": "image_url", "image_url": {"url": data_url(path)}})
    return [{"role": "user", "content": content}]


def endpoint_candidates(cli_endpoint: str | None) -> list[str]:
    raw = cli_endpoint or os.environ.get("ZAI_VISION_ENDPOINT") or os.environ.get("ZAI_GLM5V_ENDPOINT")
    if raw:
        return [part.strip() for part in raw.split(",") if part.strip()]
    return DEFAULT_ENDPOINTS


def post_chat_completion(endpoint: str, api_key: str, payload: dict[str, object], timeout: int) -> dict[str, object]:
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
        raise RuntimeError(f"HTTP {error.code} from {endpoint}: {detail[:1200]}") from error


def extract_content(response: dict[str, object]) -> str:
    choices = response.get("choices")
    if not isinstance(choices, list) or not choices:
        raise ValueError("Response is missing choices")
    first = choices[0]
    if not isinstance(first, dict):
        raise ValueError("Response choice is not an object")
    message = first.get("message")
    if not isinstance(message, dict):
        raise ValueError("Response choice is missing message")
    content = message.get("content")
    if isinstance(content, str):
        return content.strip()
    if isinstance(content, list):
        parts: list[str] = []
        for item in content:
            if isinstance(item, dict) and isinstance(item.get("text"), str):
                parts.append(item["text"])
        return "\n".join(parts).strip()
    raise ValueError("Response message content is not text")


def normalize_report(text: str, model: str) -> str:
    text = text.strip()
    if not text.startswith("# Formic Visual Assessment"):
        text = "# Formic Visual Assessment\n\n" + text
    if not re.search(r"(?im)^\s*Assessor\s*:\s*GLM-5V-Turbo\s*$", text):
        text = re.sub(
            r"(?im)^(\s*Verdict\s*:\s*(?:FAIL|PASS WITH NOTES|PASS)\s*)$",
            rf"\1\nAssessor: GLM-5V-Turbo\nModel: {model}",
            text,
            count=1,
        )
    if not re.search(r"(?im)^\s*Assessor\s*:\s*GLM-5V-Turbo\s*$", text):
        text = text.replace("# Formic Visual Assessment", f"# Formic Visual Assessment\n\nAssessor: GLM-5V-Turbo\nModel: {model}", 1)
    return text.rstrip() + "\n"


def write_blocked_report(output: Path, reason: str, model: str) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(
        "# Formic Visual Assessment\n\n"
        "Verdict: FAIL\n"
        "Assessor: GLM-5V-Turbo\n"
        f"Model: {model}\n\n"
        "## Blockers\n\n"
        "1. [P0] GLM-5V-Turbo visual assessment could not complete\n"
        "   Scene: build/visual-qa/screenshots/*.png\n"
        f"   Evidence: {reason}\n"
        "   Impact: The visual gate cannot trust a text-only or stale assessment.\n"
        "   Fix: Restore GLM-5V-Turbo API access and rerun `scripts\\glm5v-visual-assessment.cmd` after fresh `scripts\\gui-smoke.cmd` artifacts exist.\n"
        "   Acceptance: `formic-visual-assessment.md` is regenerated by GLM-5V-Turbo and contains no P0/P1 findings.\n",
        encoding="utf-8",
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--visual-qa-dir", default=str(DEFAULT_VISUAL_QA_DIR))
    parser.add_argument("--output", default=str(DEFAULT_OUTPUT))
    parser.add_argument("--intent-dir", default=str(DEFAULT_INTENT_DIR))
    parser.add_argument("--model", default=os.environ.get("ZAI_VISION_MODEL", DEFAULT_MODEL))
    parser.add_argument("--endpoint", default=None, help="Comma-separated endpoint override. Defaults to Z.AI regular endpoint, then Coding Plan endpoint.")
    parser.add_argument("--env-key", default="ZAI_API_KEY")
    parser.add_argument("--timeout", type=int, default=300)
    parser.add_argument("--max-tokens", type=int, default=8192)
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
        write_blocked_report(output, str(exception), args.model)
        print(f"GLM-5V visual assessment blocked before API call: {exception}")
        return 2

    total_bytes = sum(path.stat().st_size for path in paths)
    if args.dry_run:
        print(f"GLM-5V dry run OK: {len(paths)} screenshots, {len(reference_paths)} reference images, {total_bytes} screenshot bytes, output={output}")
        return 0

    api_key = os.environ.get(args.env_key)
    if not api_key:
        reason = f"{args.env_key} is not set"
        write_blocked_report(output, reason, args.model)
        print(f"GLM-5V visual assessment blocked: {reason}")
        return 2

    payload: dict[str, object] = {
        "model": args.model,
        "messages": build_messages(visual_qa_dir, output, args.model, paths, reference_paths, intent_doc, manifest_text, matrix_text),
        "temperature": 0.1,
        "max_tokens": args.max_tokens,
        "thinking": {"type": "enabled"},
    }

    errors: list[str] = []
    for endpoint in endpoint_candidates(args.endpoint):
        started = time.time()
        try:
            response = post_chat_completion(endpoint, api_key, payload, args.timeout)
            report_text = normalize_report(extract_content(response), args.model)
            output.parent.mkdir(parents=True, exist_ok=True)
            output.write_text(report_text, encoding="utf-8")
            elapsed = time.time() - started
            print(f"GLM-5V visual assessment written: {output} ({elapsed:.1f}s via {endpoint})")
            return 0
        except Exception as exception:  # noqa: BLE001
            errors.append(str(exception))

    reason = " | ".join(errors)
    write_blocked_report(output, reason, args.model)
    print(f"GLM-5V visual assessment failed: {reason}")
    return 1


if __name__ == "__main__":
    sys.exit(main())
