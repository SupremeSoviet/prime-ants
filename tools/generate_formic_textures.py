from __future__ import annotations

import struct
import zlib
from pathlib import Path
from typing import Iterable

ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "src/main/resources/assets/formic_frontier/textures"

Color = tuple[int, int, int] | tuple[int, int, int, int]
Palette = tuple[Color, Color, Color, Color]


def rgba(color: Color) -> tuple[int, int, int, int]:
    if len(color) == 3:
        return color[0], color[1], color[2], 255
    return color


def alpha(color: Color, value: int) -> tuple[int, int, int, int]:
    opaque = rgba(color)
    return opaque[0], opaque[1], opaque[2], value


class Canvas:
    """Tiny dependency-free, nearest-neighbour pixel-art canvas."""

    def __init__(self, width: int, height: int, background: Color = (0, 0, 0, 0)):
        self.width = width
        self.height = height
        self.pixels = [[rgba(background) for _ in range(width)] for _ in range(height)]

    def set(self, x: int, y: int, color: Color) -> None:
        if 0 <= x < self.width and 0 <= y < self.height:
            self.pixels[y][x] = rgba(color)

    def rect(self, x: int, y: int, width: int, height: int, color: Color) -> None:
        for yy in range(y, y + height):
            for xx in range(x, x + width):
                self.set(xx, yy, color)

    def frame(self, x: int, y: int, width: int, height: int, color: Color, thickness: int = 1) -> None:
        for inset in range(thickness):
            self.rect(x + inset, y + inset, width - inset * 2, 1, color)
            self.rect(x + inset, y + height - inset - 1, width - inset * 2, 1, color)
            self.rect(x + inset, y + inset, 1, height - inset * 2, color)
            self.rect(x + width - inset - 1, y + inset, 1, height - inset * 2, color)

    def ellipse(self, cx: int, cy: int, rx: int, ry: int, color: Color) -> None:
        if rx <= 0 or ry <= 0:
            return
        for yy in range(cy - ry, cy + ry + 1):
            for xx in range(cx - rx, cx + rx + 1):
                if ((xx - cx) * (xx - cx)) * ry * ry + ((yy - cy) * (yy - cy)) * rx * rx <= rx * rx * ry * ry:
                    self.set(xx, yy, color)

    def line(self, x0: int, y0: int, x1: int, y1: int, color: Color, thickness: int = 1) -> None:
        dx = abs(x1 - x0)
        dy = -abs(y1 - y0)
        sx = 1 if x0 < x1 else -1
        sy = 1 if y0 < y1 else -1
        err = dx + dy
        x, y = x0, y0
        while True:
            radius = thickness // 2
            for yy in range(y - radius, y + radius + 1):
                for xx in range(x - radius, x + radius + 1):
                    self.set(xx, yy, color)
            if x == x1 and y == y1:
                break
            twice = 2 * err
            if twice >= dy:
                err += dy
                x += sx
            if twice <= dx:
                err += dx
                y += sy

    def polygon(self, points: Iterable[tuple[int, int]], color: Color) -> None:
        vertices = list(points)
        if len(vertices) < 3:
            return
        min_y = min(point[1] for point in vertices)
        max_y = max(point[1] for point in vertices)
        for y in range(min_y, max_y + 1):
            crossings: list[float] = []
            previous = vertices[-1]
            for current in vertices:
                x1, y1 = previous
                x2, y2 = current
                if y1 != y2 and min(y1, y2) <= y < max(y1, y2):
                    crossings.append(x1 + (y - y1) * (x2 - x1) / (y2 - y1))
                previous = current
            crossings.sort()
            for index in range(0, len(crossings) - 1, 2):
                for x in range(round(crossings[index]), round(crossings[index + 1]) + 1):
                    self.set(x, y, color)
        previous = vertices[-1]
        for current in vertices:
            self.line(previous[0], previous[1], current[0], current[1], color)
            previous = current

    def save(self, path: Path) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        raw = bytearray()
        for row in self.pixels:
            raw.append(0)
            for pixel in row:
                raw.extend(pixel)
        compressed = zlib.compress(bytes(raw), 9)

        def chunk(tag: bytes, data: bytes) -> bytes:
            checksum = zlib.crc32(tag + data) & 0xFFFFFFFF
            return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", checksum)

        png = b"\x89PNG\r\n\x1a\n"
        png += chunk(b"IHDR", struct.pack(">IIBBBBB", self.width, self.height, 8, 6, 0, 0, 0))
        png += chunk(b"IDAT", compressed)
        png += chunk(b"IEND", b"")
        path.write_bytes(png)


ANT_PALETTES: dict[str, Palette] = {
    "worker": ((35, 19, 16), (101, 50, 31), (171, 91, 43), (241, 158, 75)),
    "scout": ((26, 25, 22), (89, 70, 40), (181, 132, 61), (255, 211, 121)),
    "miner": ((21, 23, 27), (56, 61, 67), (122, 115, 94), (222, 193, 139)),
    "soldier": ((34, 10, 13), (101, 27, 29), (184, 61, 37), (255, 135, 58)),
    "major": ((28, 15, 15), (75, 33, 27), (151, 72, 40), (235, 145, 70)),
    "giant": ((30, 13, 11), (87, 35, 20), (187, 75, 29), (255, 159, 63)),
    "queen": ((47, 25, 20), (133, 72, 38), (222, 143, 67), (255, 218, 139)),
}

CHITIN: Palette = ((47, 28, 24), (123, 70, 42), (197, 123, 62), (247, 190, 101))
RESIN_CHITIN: Palette = ((29, 19, 20), (77, 42, 35), (175, 83, 39), (255, 171, 60))
BONE: Palette = ((49, 42, 35), (151, 132, 100), (213, 191, 145), (255, 236, 187))
PHEROMONE: Palette = ((38, 26, 56), (105, 66, 139), (176, 111, 214), (236, 186, 255))
VENOM: Palette = ((18, 43, 38), (39, 111, 76), (87, 191, 100), (191, 255, 142))
LEAF: Palette = ((27, 47, 28), (65, 105, 51), (121, 161, 72), (194, 211, 108))
ROYAL: Palette = ((69, 43, 21), (174, 104, 34), (240, 172, 61), (255, 231, 145))


def material_region(c: Canvas, u: int, v: int, width: int, height: int, palette: Palette, seed: int = 0) -> None:
    """Paint a logical UV region at 2x density with plated chitin detail."""
    dark, base, mid, light = palette
    x = u * 2
    y = v * 2
    w = width * 2
    h = height * 2
    c.rect(x, y, w, h, dark)
    if w > 4 and h > 4:
        c.rect(x + 2, y + 2, w - 4, h - 4, base)
    for yy in range(y + 3, y + h - 2):
        for xx in range(x + 3, x + w - 2):
            noise = (xx * 7 + yy * 11 + seed * 13) % 29
            if noise in (0, 1):
                c.set(xx, yy, mid)
            elif noise == 2:
                c.set(xx, yy, light)
    if w >= 10 and h >= 8:
        c.line(x + 3, y + h - 4, x + w - 4, y + 3, mid)
        c.line(x + 5, y + h - 4, x + w - 4, y + 5, light)
    c.frame(x, y, w, h, dark, 2)


def wing_region(c: Canvas, u: int, v: int, palette: Palette, mirrored: bool) -> None:
    dark, _, mid, light = palette
    x = u * 2
    y = v * 2
    w, h = 40, 32
    c.rect(x, y, w, h, alpha(dark, 125))
    c.rect(x + 2, y + 2, w - 4, h - 4, alpha(light, 150))
    start = x + w - 4 if mirrored else x + 3
    end = x + 3 if mirrored else x + w - 4
    c.line(start, y + 3, end, y + h - 4, alpha(mid, 210), 2)
    c.line(start, y + h // 2, end, y + 4, alpha(mid, 180), 2)
    c.frame(x, y, w, h, alpha(dark, 190), 2)


def entity_texture(name: str) -> None:
    palette = ANT_PALETTES[name]
    c = Canvas(256, 128)
    regions = (
        (0, 0, 32, 15),       # head
        (0, 17, 26, 14),      # thorax
        (34, 0, 34, 15),      # abdomen
        (34, 17, 12, 6),      # petiole
        (48, 17, 16, 8),      # post-petiole
        (70, 0, 4, 6), (75, 0, 4, 6),
        (80, 0, 4, 7), (85, 0, 4, 7),
        (70, 9, 12, 6), (84, 9, 12, 6),
        (70, 17, 10, 5), (82, 17, 10, 5),
        (70, 25, 30, 9),      # crest
        (70, 35, 30, 9),      # thorax plate
    )
    for index, region in enumerate(regions):
        material_region(c, *region, palette, seed=index + len(name))
    for index, u in enumerate((0, 9, 18, 27, 36, 45)):
        material_region(c, u, 34, 8, 8, palette, seed=40 + index)
    for index, u in enumerate((0, 5, 10, 15, 20, 25)):
        material_region(c, u, 43, 4, 9, palette, seed=50 + index)

    # Eyes are geometry, not merely painted guesses on a cube face.
    eye = (255, 172, 59) if name != "miner" else (103, 222, 255)
    if name == "soldier":
        eye = (129, 255, 119)
    if name in {"major", "giant"}:
        eye = (255, 74, 48)
    c.rect(240, 0, 12, 6, (12, 10, 10))
    c.rect(242, 1, 8, 4, eye)
    c.rect(243, 1, 3, 1, (255, 249, 198))

    # Segment bands survive distance better than random noise alone.
    dark, _, mid, light = palette
    for yy in (8, 18):
        c.line(72, yy, 132, yy, dark, 2)
        c.line(74, yy + 2, 130, yy + 2, mid)
    c.line(4, 27, 56, 4, alpha(light, 235), 2)
    c.line(4, 59, 48, 39, alpha(light, 215), 2)

    if name in {"soldier", "major", "giant"}:
        steel = (61, 62, 66)
        c.line(142, 54, 196, 54, steel, 3)
        c.line(142, 74, 196, 74, (111, 100, 87), 2)
    elif name == "miner":
        c.line(142, 54, 196, 54, (169, 158, 125), 3)
        c.rect(10, 40, 32, 5, (86, 93, 101))
    elif name == "queen":
        c.line(142, 54, 196, 54, (255, 224, 145), 3)
        c.line(148, 60, 190, 60, (171, 92, 39), 2)

    wing_region(c, 70, 46, palette, mirrored=False)
    wing_region(c, 92, 46, palette, mirrored=True)
    c.save(TEXTURES / "entity" / f"ant_{name}.png")


def outlined_line(c: Canvas, start: tuple[int, int], end: tuple[int, int], dark: Color, inner: Color, width: int = 2) -> None:
    c.line(start[0], start[1], end[0], end[1], dark, width + 2)
    c.line(start[0], start[1], end[0], end[1], inner, width)


def armor_icon(path: str, palette: Palette, kind: str, resin: bool) -> None:
    dark, base, mid, light = palette
    c = Canvas(32, 32)
    if kind == "helmet":
        outlined_line(c, (9, 8), (3, 2), dark, mid, 2)
        outlined_line(c, (23, 8), (29, 2), dark, mid, 2)
        c.ellipse(16, 14, 12, 10, dark)
        c.ellipse(16, 14, 10, 8, base)
        c.polygon(((5, 15), (11, 11), (16, 13), (21, 11), (27, 15), (24, 25), (19, 22), (16, 27), (13, 22), (8, 25)), dark)
        c.polygon(((8, 15), (12, 13), (16, 15), (20, 13), (24, 15), (22, 21), (18, 19), (16, 23), (14, 19), (10, 21)), mid)
        c.rect(10, 14, 4, 3, (22, 18, 18))
        c.rect(18, 14, 4, 3, (22, 18, 18))
        c.rect(11, 14, 2, 1, light)
        c.rect(19, 14, 2, 1, light)
    elif kind == "chestplate":
        c.polygon(((10, 3), (16, 6), (22, 3), (30, 9), (26, 15), (23, 12), (23, 28), (9, 28), (9, 12), (6, 15), (2, 9)), dark)
        c.polygon(((11, 6), (16, 9), (21, 6), (26, 10), (22, 14), (20, 12), (20, 25), (12, 25), (12, 12), (10, 14), (6, 10)), base)
        c.polygon(((13, 9), (16, 11), (19, 9), (20, 18), (16, 24), (12, 18)), mid)
        c.line(16, 11, 16, 23, light, 2)
        c.rect(5, 9, 5, 3, mid)
        c.rect(22, 9, 5, 3, mid)
    elif kind == "leggings":
        c.polygon(((8, 4), (24, 4), (25, 10), (21, 29), (15, 29), (14, 16), (11, 29), (5, 29), (7, 10)), dark)
        c.polygon(((10, 7), (22, 7), (22, 11), (19, 26), (16, 26), (15, 12), (12, 26), (8, 26), (10, 11)), base)
        c.rect(10, 8, 12, 4, mid)
        c.line(10, 23, 13, 14, light, 2)
        c.line(19, 23, 17, 14, light, 2)
    elif kind == "boots":
        c.polygon(((6, 7), (14, 7), (13, 23), (16, 27), (13, 30), (3, 29), (4, 23)), dark)
        c.polygon(((18, 7), (26, 7), (28, 23), (29, 29), (19, 30), (16, 27), (19, 23)), dark)
        c.polygon(((8, 10), (12, 10), (11, 23), (13, 26), (6, 26)), base)
        c.polygon(((20, 10), (24, 10), (26, 26), (19, 26), (21, 23)), base)
        c.rect(7, 12, 4, 6, mid)
        c.rect(21, 12, 4, 6, mid)
    if resin:
        gold = (255, 175, 53)
        c.line(9, 9, 21, 23, gold, 2)
        c.line(20, 7, 12, 25, light)
        c.ellipse(16, 16, 2, 2, gold)
    c.save(TEXTURES / "item" / path)


def resource_icon(name: str) -> None:
    c = Canvas(32, 32)
    if name == "chitin_shard":
        dark, base, mid, light = CHITIN
        c.polygon(((4, 29), (8, 12), (21, 2), (18, 14), (28, 9), (21, 26), (13, 21)), dark)
        c.polygon(((8, 25), (11, 13), (18, 7), (15, 18), (23, 14), (19, 23), (13, 18)), base)
        c.line(10, 23, 18, 8, light, 2)
        c.line(15, 19, 22, 14, mid, 2)
    elif name == "chitin_fiber":
        dark, base, mid, light = BONE
        for offset in (0, 5, 10):
            outlined_line(c, (7 + offset, 27), (5 + offset, 7), dark, base, 2)
            c.line(8 + offset, 25, 7 + offset, 9, light)
        c.line(5, 12, 24, 20, mid, 2)
        c.line(7, 7, 24, 13, light)
    elif name == "chitin_plate":
        dark, base, mid, light = CHITIN
        c.ellipse(16, 16, 12, 14, dark)
        c.ellipse(16, 16, 10, 12, base)
        c.polygon(((7, 13), (16, 6), (25, 13), (22, 24), (16, 29), (10, 24)), mid)
        c.line(8, 15, 24, 15, dark, 2)
        c.line(10, 20, 22, 20, dark, 2)
        c.line(11, 11, 18, 6, light, 2)
    elif name == "chitin_spore":
        dark, base, mid, light = LEAF
        c.ellipse(15, 18, 11, 10, dark)
        c.ellipse(15, 18, 9, 8, base)
        for x, y, radius in ((10, 14, 3), (18, 12, 4), (21, 20, 3), (11, 22, 3)):
            c.ellipse(x, y, radius, radius, mid)
            c.rect(x - 1, y - 1, 2, 2, light)
        c.line(6, 27, 24, 9, dark, 2)
    elif name == "aphid_honeydew":
        dark, base, mid, light = ROYAL
        c.polygon(((16, 2), (25, 16), (23, 26), (16, 30), (9, 26), (7, 16)), dark)
        c.polygon(((16, 6), (22, 17), (20, 24), (16, 27), (11, 24), (10, 17)), mid)
        c.ellipse(13, 14, 3, 5, light)
        c.rect(12, 11, 2, 5, (255, 249, 207))
    elif name == "colony_seal":
        dark, base, mid, light = ROYAL
        c.ellipse(16, 16, 14, 14, dark)
        c.ellipse(16, 16, 11, 11, base)
        c.ellipse(16, 16, 8, 8, mid)
        c.ellipse(16, 15, 3, 3, dark)
        c.line(16, 17, 16, 24, dark, 2)
        c.line(13, 20, 8, 24, dark, 2)
        c.line(19, 20, 24, 24, dark, 2)
        c.rect(12, 8, 8, 2, light)
    elif name == "colony_tablet":
        c.polygon(((7, 2), (25, 2), (29, 6), (27, 29), (5, 29), (3, 6)), CHITIN[0])
        c.polygon(((8, 5), (23, 5), (26, 8), (24, 26), (8, 26), (6, 8)), CHITIN[2])
        c.polygon(((10, 7), (22, 7), (23, 23), (9, 23)), (29, 37, 38))
        c.line(11, 11, 20, 9, ROYAL[3])
        c.line(11, 15, 21, 14, PHEROMONE[2])
        c.line(11, 19, 18, 21, VENOM[2])
        c.ellipse(16, 25, 1, 1, ROYAL[3])
    elif name == "fungus_culture":
        dark, base, mid, light = LEAF
        c.ellipse(10, 13, 8, 5, dark)
        c.ellipse(10, 12, 6, 3, mid)
        c.rect(8, 14, 4, 12, BONE[2])
        c.ellipse(22, 17, 7, 5, dark)
        c.ellipse(22, 16, 5, 3, base)
        c.rect(20, 18, 4, 9, BONE[1])
        c.rect(7, 10, 2, 1, light)
        c.rect(20, 14, 2, 1, light)
    elif name == "leaf_mash":
        dark, base, mid, light = LEAF
        c.polygon(((4, 19), (10, 8), (25, 4), (27, 18), (20, 28), (7, 27)), dark)
        c.polygon(((7, 19), (12, 10), (23, 7), (24, 17), (19, 25), (9, 24)), base)
        c.line(8, 23, 23, 9, light, 2)
        c.ellipse(13, 22, 5, 3, mid)
        c.ellipse(21, 19, 4, 3, mid)
    elif name == "mandible_plate":
        dark, base, mid, light = CHITIN
        c.ellipse(16, 17, 9, 11, dark)
        c.ellipse(16, 17, 7, 9, base)
        outlined_line(c, (13, 13), (3, 3), dark, light, 2)
        outlined_line(c, (19, 13), (29, 3), dark, light, 2)
        c.line(10, 18, 22, 18, mid, 2)
        c.line(12, 23, 20, 11, light)
    elif name == "mandible_saber":
        dark, base, mid, light = BONE
        c.polygon(((3, 29), (9, 19), (18, 5), (29, 2), (25, 12), (13, 23), (7, 30)), dark)
        c.polygon(((7, 26), (11, 19), (19, 8), (25, 5), (22, 10), (11, 22)), light)
        c.line(6, 23, 13, 30, CHITIN[2], 3)
        c.rect(3, 27, 7, 3, CHITIN[0])
    elif name == "pheromone_dust":
        dark, base, mid, light = PHEROMONE
        c.polygon(((4, 27), (9, 19), (16, 17), (24, 20), (29, 28)), dark)
        c.ellipse(16, 24, 10, 5, base)
        c.ellipse(16, 23, 7, 3, mid)
        for x, y in ((8, 10), (15, 5), (22, 11), (26, 5), (5, 16)):
            c.ellipse(x, y, 2, 2, mid)
            c.set(x, y - 1, light)
    elif name == "pheromone_token":
        dark, base, mid, light = PHEROMONE
        outer = ((10, 2), (22, 2), (30, 10), (30, 22), (22, 30), (10, 30), (2, 22), (2, 10))
        inner = ((11, 5), (21, 5), (27, 11), (27, 21), (21, 27), (11, 27), (5, 21), (5, 11))
        c.polygon(outer, dark)
        c.polygon(inner, base)
        c.ellipse(16, 16, 8, 8, mid)
        c.line(16, 9, 20, 13, light, 2)
        c.line(20, 13, 13, 20, light, 2)
        c.line(13, 20, 10, 16, light, 2)
    elif name == "queen_egg":
        c.ellipse(16, 17, 11, 14, BONE[0])
        c.ellipse(16, 17, 9, 12, BONE[2])
        c.ellipse(13, 12, 4, 6, BONE[3])
        c.line(10, 22, 15, 17, ROYAL[2], 2)
        c.line(15, 17, 22, 11, ROYAL[2], 2)
        c.line(15, 17, 22, 24, ROYAL[1], 2)
    elif name == "raw_biomass":
        c.ellipse(12, 18, 9, 9, CHITIN[0])
        c.ellipse(20, 18, 9, 10, LEAF[0])
        c.ellipse(16, 14, 10, 8, CHITIN[1])
        c.ellipse(11, 13, 5, 4, LEAF[2])
        c.ellipse(21, 20, 5, 5, CHITIN[2])
        c.line(8, 25, 24, 8, LEAF[3], 2)
    elif name == "resin_glob":
        dark, base, mid, light = RESIN_CHITIN
        c.ellipse(16, 20, 12, 10, dark)
        c.ellipse(16, 19, 10, 8, mid)
        c.ellipse(12, 14, 5, 7, base)
        c.ellipse(20, 18, 6, 7, mid)
        c.ellipse(11, 13, 3, 4, light)
        c.rect(10, 10, 2, 5, (255, 224, 135))
    elif name == "royal_jelly":
        dark, base, mid, light = ROYAL
        c.rect(7, 11, 18, 16, dark)
        c.rect(9, 13, 14, 12, base)
        c.rect(8, 7, 16, 5, CHITIN[0])
        c.rect(10, 8, 12, 3, CHITIN[2])
        c.ellipse(16, 18, 5, 5, mid)
        c.ellipse(14, 16, 2, 3, light)
        c.rect(9, 27, 14, 2, dark)
    elif name == "royal_wax":
        dark, base, mid, light = ROYAL
        c.polygon(((8, 4), (24, 4), (30, 14), (25, 27), (9, 29), (2, 18)), dark)
        c.polygon(((9, 7), (22, 7), (27, 15), (23, 24), (10, 26), (5, 18)), base)
        for cx, cy in ((11, 13), (19, 13), (15, 20), (23, 20)):
            c.ellipse(cx, cy, 3, 3, mid)
            c.ellipse(cx, cy, 1, 1, dark)
        c.line(8, 9, 20, 7, light, 2)
    elif name == "venom_sac":
        dark, base, mid, light = VENOM
        c.polygon(((16, 3), (25, 11), (27, 22), (22, 29), (10, 29), (5, 22), (7, 11)), dark)
        c.polygon(((16, 7), (22, 12), (23, 21), (19, 26), (11, 26), (8, 21), (10, 12)), base)
        c.ellipse(13, 15, 4, 6, mid)
        c.rect(12, 10, 2, 7, light)
        c.line(11, 5, 21, 5, CHITIN[2], 3)
    elif name == "venom_spear":
        outlined_line(c, (4, 29), (23, 9), CHITIN[0], CHITIN[2], 2)
        c.polygon(((20, 11), (23, 2), (30, 2), (28, 9), (24, 14)), VENOM[0])
        c.polygon(((23, 9), (25, 4), (28, 4), (26, 9), (24, 11)), VENOM[3])
        c.rect(3, 27, 5, 3, CHITIN[3])
    elif name == "war_banner":
        c.line(6, 2, 6, 30, CHITIN[0], 3)
        c.rect(7, 4, 22, 18, (72, 14, 18))
        c.polygon(((7, 21), (12, 27), (18, 22), (23, 27), (29, 21)), (72, 14, 18))
        c.frame(7, 4, 22, 18, (32, 10, 13), 2)
        c.ellipse(18, 12, 4, 3, (228, 118, 54))
        c.line(18, 15, 18, 20, (228, 118, 54), 2)
        c.line(15, 16, 11, 20, (228, 118, 54), 2)
        c.line(21, 16, 25, 20, (228, 118, 54), 2)
    else:
        raise ValueError(f"Unknown resource icon: {name}")
    c.save(TEXTURES / "item" / f"{name}.png")


def equipment_texture(name: str, palette: Palette, resin: bool = False) -> None:
    dark, base, mid, light = palette
    for folder in ("humanoid", "humanoid_leggings"):
        c = Canvas(128, 64)
        material_region(c, 0, 0, 64, 32, palette, seed=71 if resin else 31)
        # Broad segmented plates follow the vanilla 64x32 armor atlas while
        # retaining 2x micro-detail inside every logical texel.
        for logical_x in range(0, 64, 8):
            x = logical_x * 2
            c.line(x, 62, min(127, x + 17), 2, alpha(dark, 235), 2)
        for logical_x in (4, 20, 36, 52):
            x = logical_x * 2
            c.rect(x, 8, 12, 8, mid)
            c.rect(x + 2, 10, 8, 4, light)
            c.frame(x, 8, 12, 8, dark, 2)
        c.line(2, 31, 125, 31, dark, 2)
        c.line(3, 33, 124, 33, mid, 2)
        if resin:
            gold = (255, 174, 48)
            c.line(0, 5, 127, 5, gold, 3)
            c.line(0, 58, 127, 58, gold, 3)
            for x in range(12, 128, 32):
                c.line(x, 12, x + 9, 31, gold, 2)
                c.line(x + 9, 31, x + 3, 49, light, 2)
                c.ellipse(x + 9, 31, 3, 3, gold)
        c.save(TEXTURES / "entity/equipment" / folder / f"{name}.png")


def main() -> None:
    for name in ANT_PALETTES:
        entity_texture(name)

    for armor_name, palette, resin in (
        ("chitin", CHITIN, False),
        ("resin_chitin", RESIN_CHITIN, True),
    ):
        for kind in ("helmet", "chestplate", "leggings", "boots"):
            armor_icon(f"{armor_name}_{kind}.png", palette, kind, resin)
        equipment_texture(armor_name, palette, resin=resin)

    for name in (
        "aphid_honeydew",
        "chitin_fiber",
        "chitin_plate",
        "chitin_shard",
        "chitin_spore",
        "colony_seal",
        "colony_tablet",
        "fungus_culture",
        "leaf_mash",
        "mandible_plate",
        "mandible_saber",
        "pheromone_dust",
        "pheromone_token",
        "queen_egg",
        "raw_biomass",
        "resin_glob",
        "royal_jelly",
        "royal_wax",
        "venom_sac",
        "venom_spear",
        "war_banner",
    ):
        resource_icon(name)


if __name__ == "__main__":
    main()
