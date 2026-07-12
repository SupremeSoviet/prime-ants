from __future__ import annotations

import argparse
import json
import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_DIR = ROOT / "build/visual-qa"
FULL_EXPECTED = [
    "colony_overview.png",
    "colony_ground.png",
    "mound_interior.png",
    "mound_storage_interior.png",
    "mound_lookout_interior.png",
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
WORLD_EXPECTED = [name for name in FULL_EXPECTED if not name.startswith("tablet_")]
STRUCTURE_EXPECTED = [
    "structure_preview_front.png",
    "structure_preview_3q.png",
    "mound_interior.png",
    "mound_storage_interior.png",
    "mound_lookout_interior.png",
    "food_store_variants.png",
    "food_store_interior.png",
    "nursery_variants.png",
    "nursery_interior.png",
    "mine_variants.png",
    "mine_interior.png",
    "chitin_farm_variants.png",
    "chitin_farm_interior.png",
    "barracks_variants.png",
    "barracks_interior.png",
    "market_variants.png",
    "market_courtyard.png",
    "archive_variants.png",
    "archive_hall_interior.png",
    "archive_loft_interior.png",
    "armory_variants.png",
    "armory_interior.png",
    "shrine_variants.png",
    "shrine_sanctum.png",
    "resin_depot_variants.png",
    "resin_depot_interior.png",
    "fungus_garden_variants.png",
    "fungus_garden_interior.png",
    "venom_press_variants.png",
    "venom_press_interior.png",
    "watch_post_variants.png",
    "watch_post_guard_interior.png",
    "watch_post_lookout_interior.png",
    "great_mound_growth.png",
    "great_mound_larder_interior.png",
    "great_mound_workshop_interior.png",
    "great_mound_crown_interior.png",
    "queen_vault_descent_interior.png",
    "queen_vault_guard_interior.png",
    "queen_vault_lower_interior.png",
    "queen_vault_sanctum_interior.png",
    "trade_hub_exterior.png",
    "trade_hub_courtyard.png",
    "trade_hub_warehouse.png",
    "trade_hub_brokerage.png",
]
EXPECTED_BY_SCOPE = {
    "full": FULL_EXPECTED,
    "world": WORLD_EXPECTED,
    "structure": STRUCTURE_EXPECTED,
}


def png_size(path: Path) -> tuple[int, int]:
    data = path.read_bytes()
    if len(data) < 24 or data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("not a PNG")
    return struct.unpack(">II", data[16:24])


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--visual-qa-dir", default=str(DEFAULT_DIR))
    parser.add_argument("--scope", choices=EXPECTED_BY_SCOPE, default="full")
    parser.add_argument("--ci-manifest-only", action="store_true")
    args = parser.parse_args()

    output = Path(args.visual_qa_dir)
    screenshots = output / "screenshots"
    output.mkdir(parents=True, exist_ok=True)
    expected = EXPECTED_BY_SCOPE[args.scope]

    if args.ci_manifest_only:
        report = {
            "status": "manifest_only",
            "reason": "GUI screenshots are produced by local Windows visual QA runs.",
            "scope": args.scope,
            "expectedScreenshots": expected,
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

    # Focused structure scenes emit a precise server-side failure when their
    # authored crown cannot be placed (for example, when an invalid QA ground Y
    # would push it above the world ceiling). Do not let a non-empty landscape PNG
    # turn that failed command into a false-positive screenshot pass.
    client_log = output / "runClient.log"
    if client_log.exists():
        for line in client_log.read_text(encoding="utf-8", errors="replace").splitlines():
            if "(formic_frontier) Invalid " in line and " crown at " in line:
                errors.append(f"Runtime structure assertion failed: {line.strip()}")

    for name in expected:
        path = screenshots / name
        if not path.exists():
            errors.append(f"Missing screenshot: {path}")
            continue
        # The dark, mostly flat-color tablet UI compresses substantially better
        # than world screenshots. Valid 1600x900 tablet captures routinely land
        # around 85-100 KiB, while genuinely blank frames stay far below this.
        minimum_bytes = 70_000 if name.startswith("tablet_") else 200_000
        if path.stat().st_size < minimum_bytes:
            errors.append(
                f"Screenshot is suspiciously small and likely blank: {path} "
                f"({path.stat().st_size} bytes; expected at least {minimum_bytes})"
            )
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
    report = {"status": status, "scope": args.scope, "screenshots": found, "errors": errors}
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
