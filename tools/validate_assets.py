from __future__ import annotations

import json
import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/formic_frontier"
DATA = ROOT / "src/main/resources/data/formic_frontier"


def load_json(path: Path) -> object:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def png_size(path: Path) -> tuple[int, int]:
    data = path.read_bytes()
    if len(data) < 24 or data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("not a PNG")
    width, height = struct.unpack(">II", data[16:24])
    return width, height


def main() -> int:
    errors: list[str] = []

    for path in list(ASSETS.rglob("*.json")) + list(DATA.rglob("*.json")):
        try:
            load_json(path)
        except Exception as exception:  # noqa: BLE001
            errors.append(f"Invalid JSON: {path.relative_to(ROOT)}: {exception}")

    try:
        en = load_json(ASSETS / "lang/en_us.json")
        ru = load_json(ASSETS / "lang/ru_ru.json")
        missing_ru = sorted(set(en) - set(ru))
        extra_ru = sorted(set(ru) - set(en))
        if missing_ru:
            errors.append(f"ru_ru.json missing keys: {missing_ru}")
        if extra_ru:
            errors.append(f"ru_ru.json extra keys: {extra_ru}")
        mojibake_tokens = ("Ã", "Â", "Ð", "Ñ", "\ufffd")
        mojibake = [
            key
            for key, value in ru.items()
            if isinstance(value, str) and any(token in value for token in mojibake_tokens)
        ]
        if mojibake:
            errors.append(f"ru_ru.json contains mojibake-looking values: {mojibake[:20]}")
    except Exception as exception:  # noqa: BLE001
        errors.append(f"Language validation failed: {exception}")

    for path in (ASSETS / "items").glob("*.json"):
        data = load_json(path)
        model = data.get("model", {}).get("model") if isinstance(data, dict) else None
        if isinstance(model, str) and model.startswith("formic_frontier:item/"):
            model_name = model.split("/", 1)[1]
            expected = ASSETS / "models/item" / f"{model_name}.json"
            if not expected.exists():
                errors.append(f"Missing item model for {path.relative_to(ROOT)} -> {expected.relative_to(ROOT)}")

    for path in (ASSETS / "blockstates").glob("*.json"):
        data = load_json(path)
        variants = data.get("variants", {}) if isinstance(data, dict) else {}
        for value in variants.values():
            entries = value if isinstance(value, list) else [value]
            for entry in entries:
                model = entry.get("model") if isinstance(entry, dict) else None
                if isinstance(model, str) and model.startswith("formic_frontier:block/"):
                    model_name = model.split("/", 1)[1]
                    expected = ASSETS / "models/block" / f"{model_name}.json"
                    if not expected.exists():
                        errors.append(f"Missing block model for {path.relative_to(ROOT)} -> {expected.relative_to(ROOT)}")

    for path in (ASSETS / "models").rglob("*.json"):
        data = load_json(path)
        textures = data.get("textures", {}) if isinstance(data, dict) else {}
        for value in textures.values():
            if isinstance(value, str) and value.startswith("formic_frontier:"):
                texture_name = value.split(":", 1)[1]
                expected = ASSETS / "textures" / f"{texture_name}.png"
                if not expected.exists():
                    errors.append(f"Missing texture for {path.relative_to(ROOT)} -> {expected.relative_to(ROOT)}")

    for path in (ASSETS / "textures").rglob("*.png"):
        try:
            width, height = png_size(path)
        except Exception as exception:  # noqa: BLE001
            errors.append(f"Invalid PNG: {path.relative_to(ROOT)}: {exception}")
            continue
        if width <= 0 or height <= 0 or width > 512 or height > 512:
            errors.append(f"Suspicious PNG size: {path.relative_to(ROOT)} is {width}x{height}")

    if errors:
        print("Asset validation failed:")
        for error in errors:
            print(f"- {error}")
        return 1

    print("Asset validation passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
