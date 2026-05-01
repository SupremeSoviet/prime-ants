from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_VISUAL_QA_DIR = ROOT / "build/visual-qa"
DEFAULT_ASSESSMENT = DEFAULT_VISUAL_QA_DIR / "formic-visual-assessment.md"


def load_json(path: Path) -> dict[str, object]:
    with path.open("r", encoding="utf-8") as handle:
        data = json.load(handle)
    if not isinstance(data, dict):
        raise ValueError(f"{path} must contain a JSON object")
    return data


def write_assessment_request(path: Path, assessment_path: Path) -> None:
    path.write_text(
        "# Formic Visual Assessment Required\n\n"
        "Run Codex with `$formic-visual-assessment` against the latest `build/visual-qa` artifacts, "
        f"then save the report to `{assessment_path.as_posix()}`.\n\n"
        "The gate accepts `Verdict: PASS` or `Verdict: PASS WITH NOTES` only, and blocks any `P0` or `P1` finding.\n",
        encoding="utf-8",
    )


def parse_verdict(text: str) -> str | None:
    match = re.search(r"(?im)^\s*Verdict\s*:\s*(PASS WITH NOTES|PASS|FAIL)\b", text)
    return match.group(1).upper() if match else None


def has_blocking_severity(text: str) -> bool:
    return re.search(r"(?im)^\s*(?:[-*]|\d+[.)])?\s*\[\s*P[01]\s*\]", text) is not None


def has_fail_verdict(text: str) -> bool:
    return re.search(r"(?im)^\s*Verdict\s*:\s*FAIL\b", text) is not None


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--visual-qa-dir", default=str(DEFAULT_VISUAL_QA_DIR))
    parser.add_argument("--assessment-report", default=str(DEFAULT_ASSESSMENT))
    args = parser.parse_args()

    visual_qa_dir = Path(args.visual_qa_dir)
    assessment = Path(args.assessment_report)
    if not assessment.is_absolute():
        assessment = ROOT / assessment

    report_json = visual_qa_dir / "visual-qa-report.json"
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

    if summary_json.exists():
        try:
            summary = load_json(summary_json)
        except Exception as exception:  # noqa: BLE001
            print(f"Invalid visual QA summary: {exception}")
            return 1
        if summary.get("status") not in ("complete", "passed"):
            print(f"Visual QA client did not complete: {summary.get('status')}")
            return 1

    request_path = visual_qa_dir / "formic-visual-assessment-required.md"
    if not assessment.exists():
        write_assessment_request(request_path, assessment.relative_to(ROOT))
        print(f"Missing visual assessment report: {assessment}")
        print(f"Assessment request written: {request_path}")
        return 2

    screenshot_dir = visual_qa_dir / "screenshots"
    newest_screenshot_time = max(
        (path.stat().st_mtime for path in screenshot_dir.glob("*.png")),
        default=report_json.stat().st_mtime,
    )
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
    if verdict == "FAIL" or has_fail_verdict(text) or has_blocking_severity(text):
        print(f"Visual assessment blocks the feature: verdict={verdict}")
        return 1

    print(f"Visual assessment gate passed: verdict={verdict}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
