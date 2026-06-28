from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

# Content acceptance excerpts can contain non-ASCII; force UTF-8 so the gate
# never crashes with a cp1252 error on Windows.
try:
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
except Exception:
    pass

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_MATRIX = ROOT / "build/autonomous-loop/content-feature-matrix.json"
DEFAULT_TEST_SUMMARY = ROOT / "build/qa/test-mod-summary.json"


def load_json(path: Path) -> dict[str, object]:
    with path.open("r", encoding="utf-8-sig") as handle:
        data = json.load(handle)
    if not isinstance(data, dict):
        raise ValueError(f"{path} must contain a JSON object")
    return data


def matrix_rows(matrix: dict[str, object]) -> list[dict[str, object]]:
    rows = matrix.get("criteria")
    if not isinstance(rows, list):
        return []
    return [row for row in rows if isinstance(row, dict)]


def open_required_rows(matrix: dict[str, object]) -> list[dict[str, object]]:
    open_rows: list[dict[str, object]] = []
    for row in matrix_rows(matrix):
        if not row.get("required"):
            continue
        if str(row.get("status", "unknown")).strip().lower() != "pass":
            open_rows.append(row)
    return open_rows


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--matrix", default=str(DEFAULT_MATRIX))
    parser.add_argument("--test-summary", default=str(DEFAULT_TEST_SUMMARY))
    args = parser.parse_args()

    matrix_path = Path(args.matrix)
    if not matrix_path.is_absolute():
        matrix_path = ROOT / matrix_path
    test_summary_path = Path(args.test_summary)
    if not test_summary_path.is_absolute():
        test_summary_path = ROOT / test_summary_path

    # 1. Content correctness + no regression is proven by a green test-mod run.
    #    A content feature may only be accepted once its own gametest passes and
    #    no existing gametest regressed (Anthropic: prove the feature with a test).
    if not test_summary_path.exists():
        print(f"Missing test-mod summary (run scripts/test-mod.cmd first): {test_summary_path}")
        return 1
    try:
        summary = load_json(test_summary_path)
    except Exception as exception:  # noqa: BLE001
        print(f"Invalid test-mod summary: {exception}")
        return 1
    if summary.get("status") != "passed":
        print(f"Content gate blocked: test-mod did not pass (status={summary.get('status')}).")
        findings = summary.get("findings")
        if isinstance(findings, list):
            for finding in findings[:6]:
                print(f"- {finding}")
        return 1

    # 2. The content matrix must be valid and present so progress is tracked.
    if not matrix_path.exists():
        print(f"Missing content feature matrix: {matrix_path}")
        return 1
    try:
        matrix = load_json(matrix_path)
    except Exception as exception:  # noqa: BLE001
        print(f"Invalid content feature matrix: {exception}")
        return 1
    rows = matrix_rows(matrix)
    if not rows:
        print("Content feature matrix has no criteria rows.")
        return 1

    # 3. Guard against silent acceptance hacks: a row may not be marked pass
    #    without recording an evidenceTest (the gametest that proves it).
    for row in rows:
        if str(row.get("status", "")).strip().lower() == "pass" and not str(row.get("evidenceTest", "")).strip():
            print(f"Content row '{row.get('id')}' is marked pass without an evidenceTest; not acceptable.")
            return 1

    open_rows = open_required_rows(matrix)
    print(f"Content gate passed: test-mod green, content matrix valid, {len(open_rows)} required content rows still open.")
    for row in open_rows[:8]:
        print(f"- [{row.get('priority')}] {row.get('id')} status={row.get('status')} owner={row.get('owner')} next={row.get('nextAction')}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
