from __future__ import annotations

import argparse
import json
import re
import struct
import sys
from pathlib import Path

# Visual assessment excerpts frequently contain Cyrillic (e.g. Russian tablet
# copy). Force stdout/stderr to UTF-8 so the gate never crashes with a
# cp1252 UnicodeEncodeError when printing blocker excerpts on Windows.
try:
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
except Exception:
    pass

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_VISUAL_QA_DIR = ROOT / "build/visual-qa"
DEFAULT_ASSESSMENT = DEFAULT_VISUAL_QA_DIR / "formic-visual-assessment.md"
DEFAULT_MATRIX = ROOT / "build/autonomous-loop/visual-feature-matrix.json"
DEFAULT_LOOP_STATE = ROOT / "build/autonomous-loop/visual-loop-state.json"
EXPECTED_SCENES = {
    "colony_overview",
    "colony_ground",
    "ant_lineup",
    "work_cycle",
    "tablet_en",
    "tablet_ru",
    "tablet_guide",
    "tablet_trade",
    "tablet_research_map",
    "tablet_market",
    "tablet_requests",
    "progression_scene",
    "settlement_scale",
    "construction_stage",
    "repair_scene",
    "culture_styles",
    "diplomacy_scene",
    "worldgen_encounter",
    "endgame_project",
}


def load_json(path: Path) -> dict[str, object]:
    with path.open("r", encoding="utf-8-sig") as handle:
        data = json.load(handle)
    if not isinstance(data, dict):
        raise ValueError(f"{path} must contain a JSON object")
    return data


def write_assessment_request(path: Path, assessment_path: Path) -> None:
    path.write_text(
        "# Formic Visual Assessment Required\n\n"
        "Run `scripts\\openai-visual-assessment.cmd` against the latest `build/visual-qa` artifacts, "
        f"then save the report to `{assessment_path.as_posix()}`.\n\n"
        "The gate accepts `Verdict: PASS` or `Verdict: PASS WITH NOTES` only, requires the configured vision assessor, "
        "and blocks any `P0` or `P1` finding.\n",
        encoding="utf-8",
    )


def parse_verdict(text: str) -> str | None:
    match = re.search(r"(?im)^\s*Verdict\s*:\s*(PASS WITH NOTES|PASS|FAIL)\b", text)
    return match.group(1).upper() if match else None


def has_blocking_severity(text: str) -> bool:
    return re.search(r"(?im)^\s*(?:[-*]|\d+[.)])?\s*\[\s*P[01]\s*\]", text) is not None


def has_fail_verdict(text: str) -> bool:
    return re.search(r"(?im)^\s*Verdict\s*:\s*FAIL\b", text) is not None


def blocking_excerpt(text: str, max_lines: int = 80) -> str:
    lines = text.splitlines()
    selected: list[str] = []
    in_blockers = False
    for line in lines:
        stripped = line.strip()
        if re.match(r"(?i)^##\s+blockers\b", stripped):
            in_blockers = True
            selected.append(line)
            continue
        if in_blockers and re.match(r"^##\s+", stripped):
            break
        if in_blockers:
            selected.append(line)

    if not selected:
        for index, line in enumerate(lines):
            if re.search(r"(?im)^\s*(?:[-*]|\d+[.)])?\s*\[\s*P[01]\s*\]", line):
                start = max(0, index - 1)
                end = min(len(lines), index + 7)
                selected.extend(lines[start:end])
                selected.append("")

    excerpt = "\n".join(selected).strip()
    if not excerpt:
        return ""
    return "\n".join(excerpt.splitlines()[:max_lines])


def has_required_assessor(text: str, assessor: str) -> bool:
    if not assessor:
        return True
    pattern = rf"(?im)^\s*Assessor\s*:\s*{re.escape(assessor)}\s*$"
    return re.search(pattern, text) is not None


def matrix_rows(matrix: dict[str, object]) -> list[dict[str, object]]:
    rows = matrix.get("criteria")
    if not isinstance(rows, list):
        return []
    return [row for row in rows if isinstance(row, dict)]


def is_visual_baseline_row(row: dict[str, object]) -> bool:
    phase = str(row.get("phase", "visual_baseline")).strip().lower()
    return phase in ("", "visual_baseline", "visual-baseline")


def required_open_rows(matrix: dict[str, object]) -> list[dict[str, object]]:
    open_rows: list[dict[str, object]] = []
    for row in matrix_rows(matrix):
        if not is_visual_baseline_row(row):
            continue
        if not row.get("required"):
            continue
        status = str(row.get("status", "unknown")).strip().lower()
        if status != "pass":
            open_rows.append(row)
    return open_rows


def loop_requests_mechanics(loop_state: dict[str, object] | None) -> bool:
    if not loop_state:
        return False
    fields = [
        "status",
        "activeSlice",
        "acceptanceBrief",
        "nextVisualTarget",
        "currentOwner",
    ]
    text = " ".join(str(loop_state.get(field, "")) for field in fields).lower()
    return any(token in text for token in ("mechanic", "mechanics", "playability", "gameplay"))


def matrix_claims_visual_complete(matrix: dict[str, object]) -> bool:
    phase = str(matrix.get("phase", "")).strip().lower()
    if phase and phase not in ("visual_baseline", "visual-baseline"):
        return True
    if matrix.get("visualBaselinePass") is True:
        return True
    return False


def print_open_matrix_rows(open_rows: list[dict[str, object]], max_rows: int = 12) -> None:
    print("Open required visual intent rows:")
    for row in open_rows[:max_rows]:
        row_id = row.get("id", "")
        priority = row.get("priority", "")
        status = row.get("status", "unknown")
        owner = row.get("owner", "")
        next_action = row.get("nextAction", "")
        print(f"- [{priority}] {row_id} status={status} owner={owner} next={next_action}")
    if len(open_rows) > max_rows:
        print(f"- ... {len(open_rows) - max_rows} more")


def resolve_under(base: Path, relative_path: str) -> Path:
    candidate = (base / relative_path).resolve()
    base_resolved = base.resolve()
    try:
        candidate.relative_to(base_resolved)
    except ValueError as exception:
        raise ValueError(f"reported screenshot escapes visual QA dir: {relative_path}") from exception
    return candidate


def png_dimensions(path: Path) -> tuple[int, int]:
    with path.open("rb") as handle:
        header = handle.read(24)
    if len(header) < 24 or header[:8] != b"\x89PNG\r\n\x1a\n" or header[12:16] != b"IHDR":
        raise ValueError(f"not a PNG screenshot: {path}")
    return struct.unpack(">II", header[16:24])


def reported_screenshot_paths(report: dict[str, object], visual_qa_dir: Path) -> list[Path]:
    screenshots = report.get("screenshots")
    if not isinstance(screenshots, list) or not screenshots:
        raise ValueError("visual-qa-report.json must contain a non-empty screenshots array")

    paths: list[Path] = []
    scenes: set[str] = set()
    for index, entry in enumerate(screenshots):
        if not isinstance(entry, dict):
            raise ValueError(f"screenshots[{index}] must be an object")
        file_value = str(entry.get("file", "")).strip()
        if not file_value:
            raise ValueError(f"screenshots[{index}] is missing file")
        path = resolve_under(visual_qa_dir, file_value)
        if not path.exists():
            raise ValueError(f"reported screenshot is missing: {path}")
        if path.suffix.lower() != ".png":
            raise ValueError(f"reported screenshot is not a PNG: {path}")

        actual_bytes = path.stat().st_size
        reported_bytes = entry.get("bytes")
        if isinstance(reported_bytes, int) and reported_bytes != actual_bytes:
            raise ValueError(f"reported byte count is stale for {path}: report={reported_bytes} actual={actual_bytes}")

        width, height = png_dimensions(path)
        reported_width = entry.get("width")
        reported_height = entry.get("height")
        if isinstance(reported_width, int) and reported_width != width:
            raise ValueError(f"reported width is stale for {path}: report={reported_width} actual={width}")
        if isinstance(reported_height, int) and reported_height != height:
            raise ValueError(f"reported height is stale for {path}: report={reported_height} actual={height}")
        if (width, height) != (1600, 900):
            raise ValueError(f"reported screenshot must be 1600x900, got {width}x{height}: {path}")

        scenes.add(path.stem)
        paths.append(path)

    missing = sorted(EXPECTED_SCENES - scenes)
    if missing:
        raise ValueError(f"visual QA report is missing expected scenes: {', '.join(missing)}")
    return paths


def freshness_marker_from_loop_state(loop_state: dict[str, object] | None) -> str:
    if not loop_state:
        return ""
    marker = str(loop_state.get("freshnessMarker", "")).strip()
    if marker:
        return marker
    artifacts = loop_state.get("lastArtifacts")
    if isinstance(artifacts, dict):
        return str(artifacts.get("freshnessMarker", "")).strip()
    return ""


def assert_newer_than_marker(paths: list[Path], marker_path: Path) -> None:
    if not marker_path.exists():
        raise ValueError(f"visual freshness marker is missing: {marker_path}")
    marker_time = marker_path.stat().st_mtime
    stale = [path for path in paths if not path.exists() or path.stat().st_mtime <= marker_time]
    if stale:
        names = ", ".join(str(path) for path in stale[:8])
        extra = "" if len(stale) <= 8 else f", ... {len(stale) - 8} more"
        raise ValueError(f"visual artifacts are older than freshness marker {marker_path}: {names}{extra}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--visual-qa-dir", default=str(DEFAULT_VISUAL_QA_DIR))
    parser.add_argument("--assessment-report", default=str(DEFAULT_ASSESSMENT))
    parser.add_argument("--require-assessor", default="")
    parser.add_argument("--matrix", default=str(DEFAULT_MATRIX))
    parser.add_argument("--loop-state", default=str(DEFAULT_LOOP_STATE))
    parser.add_argument("--freshness-marker", default="")
    parser.add_argument("--allow-missing-matrix", action="store_true")
    args = parser.parse_args()

    visual_qa_dir = Path(args.visual_qa_dir)
    if not visual_qa_dir.is_absolute():
        visual_qa_dir = ROOT / visual_qa_dir
    assessment = Path(args.assessment_report)
    if not assessment.is_absolute():
        assessment = ROOT / assessment
    matrix_path = Path(args.matrix)
    if not matrix_path.is_absolute():
        matrix_path = ROOT / matrix_path
    loop_state_path = Path(args.loop_state)
    if not loop_state_path.is_absolute():
        loop_state_path = ROOT / loop_state_path
    loop_state: dict[str, object] | None = None
    if loop_state_path.exists():
        try:
            loop_state = load_json(loop_state_path)
        except Exception as exception:  # noqa: BLE001
            print(f"Invalid visual loop state: {exception}")
            return 1

    report_json = visual_qa_dir / "visual-qa-report.json"
    report_md = visual_qa_dir / "visual-qa-report.md"
    summary_json = visual_qa_dir / "visual-qa-summary.json"
    if not report_json.exists():
        print(f"Missing visual QA report: {report_json}")
        return 1

    try:
        report = load_json(report_json)
    except Exception as exception:  # noqa: BLE001
        print(f"Invalid visual QA report: {exception}")
        return 1
    if report.get("status") != "passed":
        print(f"Visual QA screenshot gate is not passed: {report.get('status')}")
        return 1
    try:
        screenshot_paths = reported_screenshot_paths(report, visual_qa_dir)
    except Exception as exception:  # noqa: BLE001
        print(f"Invalid visual QA screenshot report: {exception}")
        return 1

    if summary_json.exists():
        try:
            summary = load_json(summary_json)
        except Exception as exception:  # noqa: BLE001
            print(f"Invalid visual QA summary: {exception}")
            return 1
        if summary.get("status") not in ("complete", "passed"):
            print(f"Visual QA client did not complete: {summary.get('status')}")
            return 1

    marker_text = args.freshness_marker.strip() or freshness_marker_from_loop_state(loop_state)
    if marker_text:
        marker_path = Path(marker_text)
        if not marker_path.is_absolute():
            marker_path = ROOT / marker_path
        try:
            freshness_paths = [report_json, report_md, *screenshot_paths]
            if assessment.exists():
                freshness_paths.append(assessment)
            assert_newer_than_marker(freshness_paths, marker_path)
        except Exception as exception:  # noqa: BLE001
            print(f"Visual freshness gate failed: {exception}")
            return 2

    request_path = visual_qa_dir / "formic-visual-assessment-required.md"
    if not assessment.exists():
        write_assessment_request(request_path, assessment.relative_to(ROOT))
        print(f"Missing visual assessment report: {assessment}")
        print(f"Assessment request written: {request_path}")
        return 2

    newest_screenshot_time = max((path.stat().st_mtime for path in screenshot_paths), default=report_json.stat().st_mtime)
    if assessment.stat().st_mtime < newest_screenshot_time:
        write_assessment_request(request_path, assessment.relative_to(ROOT))
        print(f"Visual assessment report is stale: {assessment}")
        print(f"Assessment request written: {request_path}")
        return 2

    text = assessment.read_text(encoding="utf-8")
    verdict = parse_verdict(text)
    if verdict is None:
        print(f"Visual assessment report is missing a verdict: {assessment}")
        return 1
    if not has_required_assessor(text, args.require_assessor):
        print(f"Visual assessment report is not from required assessor {args.require_assessor}: {assessment}")
        return 1
    if verdict == "FAIL" or has_fail_verdict(text) or has_blocking_severity(text):
        print(f"Visual assessment blocks the feature: verdict={verdict}")
        excerpt = blocking_excerpt(text)
        if excerpt:
            print("Blocking visual findings:")
            print(excerpt)
        return 1

    if matrix_path.exists():
        try:
            matrix = load_json(matrix_path)
            open_rows = required_open_rows(matrix)
        except Exception as exception:  # noqa: BLE001
            print(f"Invalid visual feature matrix state: {exception}")
            return 1
        if open_rows:
            if matrix_claims_visual_complete(matrix) or loop_requests_mechanics(loop_state):
                print("Visual baseline cannot be closed and mechanics/playability cannot start yet.")
                print_open_matrix_rows(open_rows)
                return 1
            print(f"Visual intent matrix still has {len(open_rows)} required open rows; continuing visual_baseline loop.")
            print_open_matrix_rows(open_rows, max_rows=6)
    else:
        print(f"Visual intent matrix not found yet: {matrix_path}")
        if not args.allow_missing_matrix:
            return 1

    print(f"Visual assessment gate passed: verdict={verdict}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
