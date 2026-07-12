from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ITEM_TEXTURES = ROOT / "src/main/resources/assets/formic_frontier/textures/item"
OUTPUT = ROOT / "build/visual-qa/formic-item-textures.png"


def main() -> None:
    files = sorted(ITEM_TEXTURES.glob("*.png"))
    columns = 5
    cell_width = 152
    cell_height = 78
    rows = (len(files) + columns - 1) // columns
    sheet = Image.new("RGBA", (cell_width * columns, cell_height * rows), (20, 23, 24, 255))
    draw = ImageDraw.Draw(sheet)

    for index, path in enumerate(files):
        source = Image.open(path).convert("RGBA")
        preview = source.resize((48, 48), Image.Resampling.NEAREST)
        x = index % columns * cell_width
        y = index // columns * cell_height
        sheet.alpha_composite(preview, (x + 4, y + 4))
        draw.text((x + 56, y + 8), path.stem, fill=(236, 225, 204, 255))
        draw.text((x + 56, y + 26), f"{source.width}x{source.height}", fill=(173, 145, 98, 255))

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(OUTPUT)
    print(OUTPUT)


if __name__ == "__main__":
    main()
