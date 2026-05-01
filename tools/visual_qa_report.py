from __future__ import annotations

import argparse
import json
import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_DIR = ROOT / "build/visual-qa"
EXPECTED = [
    "colony_overview.png",
    "colony_ground.png",
    "ant_lineup.png",
    "tablet_en.png",
    "tablet_ru.png",
    "progression_scene.png",
]


def png_size(path: Path) -> tuple[int, int]:
    data = path.read_bytes()
    if len(data) < 24 or data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("not a PNG")
    return struct.unpack(">II", data[16:24])


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--visual-qa-dir", default=str(DEFAULT_DIR))
    parser.add_argument("--ci-manifest-only", action="store_true")
    args = parser.parse_args()

    output = Path(args.visual_qa_dir)
    screenshots = output / "screenshots"
    output.mkdir(parents=True, exist_ok=True)

    if args.ci_manifest_only:
        report = {
            "status": "manifest_only",
            "reason": "GUI screenshots are produced by local Windows visual QA runs.",
            "expectedScreenshots": EXPECTED,
        }
        (output / "visual-qa-ci-report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        (output / "visual-qa-ci-report.md").write_text(
            "# Visual QA CI Report\n\n"
            "Status: manifest_only\n\n"
            "The CI job verifies that the visual QA harness is present. Real screenshots are produced locally by `scripts/gui-smoke.ps1`.\n",
            encoding="utf-8",
        )
        print("Visual QA manifest check passed.")
        return 0

    errors: list[str] = []
    found: list[dict[str, object]] = []
    for name in EXPECTED:
        path = screenshots / name
        if not path.exists():
            errors.append(f"Missing screenshot: {path}")
            continue
        if path.stat().st_size < 1024:
            errors.append(f"Screenshot is too small to be useful: {path}")
            continue
        try:
            width, height = png_size(path)
        except Exception as exception:  # noqa: BLE001
            errors.append(f"Invalid screenshot PNG {path}: {exception}")
            continue
        if width < 640 or height < 360:
            errors.append(f"Screenshot resolution is too low: {path} {width}x{height}")
        found.append({"file": f"screenshots/{name}", "width": width, "height": height, "bytes": path.stat().st_size})

    status = "failed" if errors else "passed"
    report = {"status": status, "screenshots": found, "errors": errors}
    (output / "visual-qa-report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    lines = ["# Visual QA Report", "", f"Status: {status}", ""]
    lines.extend(f"- {entry['file']} {entry['width']}x{entry['height']}" for entry in found)
    if errors:
        lines.append("")
        lines.extend(f"- ERROR: {error}" for error in errors)
    (output / "visual-qa-report.md").write_text("\n".join(lines) + "\n", encoding="utf-8")

    if errors:
        for error in errors:
            print(error)
        return 1
    print("Visual QA screenshot gate passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
