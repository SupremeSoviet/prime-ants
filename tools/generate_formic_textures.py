from __future__ import annotations

import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "src/main/resources/assets/formic_frontier/textures"


def rgba(color: tuple[int, int, int] | tuple[int, int, int, int]) -> tuple[int, int, int, int]:
    if len(color) == 3:
        return color[0], color[1], color[2], 255
    return color


class Canvas:
    def __init__(self, width: int, height: int, background: tuple[int, int, int, int] = (0, 0, 0, 0)):
        self.width = width
        self.height = height
        self.pixels = [[background for _ in range(width)] for _ in range(height)]

    def set(self, x: int, y: int, color: tuple[int, int, int] | tuple[int, int, int, int]) -> None:
        if 0 <= x < self.width and 0 <= y < self.height:
            self.pixels[y][x] = rgba(color)

    def rect(self, x: int, y: int, width: int, height: int, color: tuple[int, int, int] | tuple[int, int, int, int]) -> None:
        for yy in range(y, y + height):
            for xx in range(x, x + width):
                self.set(xx, yy, color)

    def frame(self, x: int, y: int, width: int, height: int, color: tuple[int, int, int] | tuple[int, int, int, int]) -> None:
        self.rect(x, y, width, 1, color)
        self.rect(x, y + height - 1, width, 1, color)
        self.rect(x, y, 1, height, color)
        self.rect(x + width - 1, y, 1, height, color)

    def ellipse(self, cx: int, cy: int, rx: int, ry: int, color: tuple[int, int, int] | tuple[int, int, int, int]) -> None:
        for yy in range(cy - ry, cy + ry + 1):
            for xx in range(cx - rx, cx + rx + 1):
                if ((xx - cx) * (xx - cx)) * ry * ry + ((yy - cy) * (yy - cy)) * rx * rx <= rx * rx * ry * ry:
                    self.set(xx, yy, color)

    def line(self, x0: int, y0: int, x1: int, y1: int, color: tuple[int, int, int] | tuple[int, int, int, int], thickness: int = 1) -> None:
        dx = abs(x1 - x0)
        dy = -abs(y1 - y0)
        sx = 1 if x0 < x1 else -1
        sy = 1 if y0 < y1 else -1
        err = dx + dy
        x, y = x0, y0
        while True:
            r = thickness // 2
            for yy in range(y - r, y + r + 1):
                for xx in range(x - r, x + r + 1):
                    self.set(xx, yy, color)
            if x == x1 and y == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy
                x += sx
            if e2 <= dx:
                err += dx
                y += sy

    def material(self, x: int, y: int, width: int, height: int, dark, base, mid, light) -> None:
        for yy in range(height):
            for xx in range(width):
                shade = base
                if xx < 2 or yy < 2 or xx >= width - 2 or yy >= height - 2:
                    shade = dark
                elif (xx + yy) % 11 == 0:
                    shade = light
                elif (xx * 2 + yy) % 7 in (0, 1):
                    shade = mid
                self.set(x + xx, y + yy, shade)
        self.frame(x, y, width, height, dark)
        self.line(x + 2, y + height - 3, x + width - 4, y + 2, light)

    def save(self, path: Path) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        raw = bytearray()
        for row in self.pixels:
            raw.append(0)
            for pixel in row:
                raw.extend(pixel)
        compressed = zlib.compress(bytes(raw), 9)

        def chunk(tag: bytes, data: bytes) -> bytes:
            return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

        png = b"\x89PNG\r\n\x1a\n"
        png += chunk(b"IHDR", struct.pack(">IIBBBBB", self.width, self.height, 8, 6, 0, 0, 0))
        png += chunk(b"IDAT", compressed)
        png += chunk(b"IEND", b"")
        path.write_bytes(png)


PALETTES = {
    "worker": ((48, 25, 17), (135, 73, 36), (199, 124, 55), (250, 183, 91)),
    "scout": ((37, 31, 23), (117, 91, 47), (214, 159, 72), (255, 215, 129)),
    "miner": ((24, 25, 28), (64, 67, 72), (143, 129, 101), (224, 197, 142)),
    "soldier": ((45, 13, 14), (133, 38, 30), (203, 79, 42), (255, 155, 72)),
    "major": ((33, 19, 16), (89, 41, 28), (172, 92, 47), (238, 157, 79)),
    "giant": ((38, 18, 13), (111, 48, 24), (219, 101, 38), (255, 182, 86)),
    "queen": ((74, 43, 26), (185, 112, 49), (239, 177, 88), (255, 224, 154)),
}


def entity_texture(name: str) -> None:
    dark, base, mid, light = PALETTES[name]
    c = Canvas(96, 64)
    c.material(0, 0, 32, 17, dark, base, mid, light)
    c.material(0, 17, 28, 23, dark, base, mid, light)
    c.material(34, 0, 36, 22, dark, base, mid, light)
    c.material(24, 0, 8, 11, dark, dark, base, mid)
    c.material(24, 12, 14, 14, dark, dark, base, mid)
    for x in (44, 52, 60, 68, 76, 84):
        c.material(x, 0, 8, 16, dark, base, mid, light)
    eye = (255, 149, 38) if name != "miner" else (116, 213, 255)
    if name == "soldier":
        eye = (100, 255, 115)
    c.rect(7, 5, 3, 2, eye)
    c.rect(20, 5, 3, 2, eye)
    c.line(4, 14, 28, 3, light)
    c.line(4, 32, 23, 21, light)
    c.line(39, 18, 65, 4, light)
    if name in {"soldier", "major", "giant"}:
        c.line(2, 11, 29, 11, (64, 64, 70), 2)
        c.line(38, 2, 68, 21, (74, 74, 80), 2)
    if name == "miner":
        c.rect(6, 2, 18, 2, (168, 153, 117))
        c.rect(8, 21, 12, 4, (107, 112, 120))
    if name == "queen":
        c.material(70, 0, 24, 20, (107, 76, 54), (225, 192, 134), (244, 220, 164), (255, 240, 194))
        c.line(73, 3, 90, 18, (139, 96, 58))
    c.save(TEXTURES / "entity" / f"ant_{name}.png")


def icon_armor(path: str, palette, kind: str) -> None:
	dark, base, mid, light = palette
	c = Canvas(16, 16)
	if kind == "helmet":
		c.ellipse(8, 7, 6, 5, dark)
		c.ellipse(8, 7, 5, 4, base)
		c.rect(4, 8, 9, 3, mid)
		c.line(5, 11, 2, 14, dark)
		c.line(11, 11, 14, 14, dark)
		c.rect(6, 5, 2, 2, light)
		c.rect(10, 5, 2, 2, light)
		c.line(5, 3, 1, 0, mid)
		c.line(11, 3, 15, 0, mid)
	elif kind == "chestplate":
		c.rect(4, 3, 8, 11, dark)
		c.rect(5, 4, 6, 9, base)
		c.rect(2, 4, 3, 5, mid)
		c.rect(11, 4, 3, 5, mid)
		c.rect(5, 11, 6, 3, mid)
		c.line(5, 12, 11, 4, light)
		c.rect(7, 5, 2, 2, dark)
	elif kind == "leggings":
		c.rect(4, 3, 8, 3, dark)
		c.rect(5, 4, 6, 2, mid)
		c.rect(4, 6, 4, 8, base)
		c.rect(8, 6, 4, 8, base)
		c.frame(4, 6, 4, 8, dark)
		c.frame(8, 6, 4, 8, dark)
		c.line(5, 12, 7, 7, light)
		c.line(9, 12, 11, 7, light)
	elif kind == "boots":
		c.rect(3, 6, 4, 7, base)
		c.rect(9, 6, 4, 7, base)
		c.frame(3, 6, 4, 7, dark)
		c.frame(9, 6, 4, 7, dark)
		c.rect(2, 12, 6, 2, dark)
		c.rect(8, 12, 6, 2, dark)
		c.rect(4, 7, 2, 3, light)
		c.rect(10, 7, 2, 3, light)
	c.save(TEXTURES / "item" / path)


def item_material(path: str, colors, shape: str) -> None:
    dark, base, mid, light = colors
    c = Canvas(16, 16)
    if shape == "shard":
        c.line(4, 13, 12, 2, dark, 3)
        c.line(5, 12, 11, 3, base, 2)
        c.line(6, 11, 10, 4, light)
    elif shape == "fiber":
        for x in (5, 8, 11):
            c.line(x, 13, x - 2, 3, mid, 2)
            c.line(x + 1, 13, x - 1, 3, light)
    elif shape == "plate":
        c.ellipse(8, 8, 5, 6, base)
        c.frame(4, 3, 9, 11, dark)
        c.line(5, 12, 12, 4, light)
    elif shape == "spore":
        c.ellipse(8, 8, 4, 4, base)
        c.ellipse(6, 6, 2, 2, light)
        c.line(4, 13, 12, 3, dark)
    c.save(TEXTURES / "item" / path)


def equipment_texture(name: str, palette, resin: bool = False) -> None:
    dark, base, mid, light = palette
    for folder in ("humanoid", "humanoid_leggings"):
        c = Canvas(64, 32)
        c.material(0, 0, 64, 32, dark, base, mid, light)
        for x in range(0, 64, 8):
            c.line(x, 31, x + 8, 0, (dark[0], dark[1], dark[2], 210))
        for x in (4, 20, 36, 52):
            c.rect(x, 4, 6, 4, light)
            c.rect(x + 1, 5, 4, 2, mid)
        if resin:
            gold = (255, 191, 76)
            c.line(0, 2, 63, 2, gold, 2)
            c.line(0, 29, 63, 29, gold, 2)
            for x in range(6, 64, 16):
                c.ellipse(x, 16, 3, 3, gold)
                c.ellipse(x, 16, 1, 1, light)
        c.save(TEXTURES / "entity/equipment" / folder / f"{name}.png")


def main() -> None:
    for name in PALETTES:
        entity_texture(name)

    chitin = ((66, 43, 30), (177, 124, 68), (221, 170, 92), (255, 221, 148))
    resin = ((39, 24, 19), (118, 61, 37), (213, 119, 51), (255, 196, 84))
    for armor_name, palette in (("chitin", chitin), ("resin_chitin", resin)):
        for kind in ("helmet", "chestplate", "leggings", "boots"):
            icon_armor(f"{armor_name}_{kind}.png", palette, kind)
    item_material("chitin_shard.png", chitin, "shard")
    item_material("chitin_fiber.png", chitin, "fiber")
    item_material("chitin_plate.png", chitin, "plate")
    item_material("chitin_spore.png", chitin, "spore")
    equipment_texture("chitin", chitin)
    equipment_texture("resin_chitin", resin, resin=True)


if __name__ == "__main__":
    main()
