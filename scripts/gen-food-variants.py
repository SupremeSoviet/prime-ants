#!/usr/bin/env python3
"""Author helper for FormicSchematic food-chamber VARIANTS.

This does NOT change the runtime format - it just emits well-formed layered
character-grid JSON (the same declarative format the LLM authors by hand),
computing circular cross-sections so the domes are smooth instead of
hand-eyeballed. Geometry is still compiled deterministically by FormicSchematic.

Coordinate convention (must match FormicSchematic.place):
  pos = origin + (col - halfW, y, row - halfD)
  col -> X, row -> Z. row 0 = north (-Z), max row = south (+Z).
  The preview FRONT camera sits at +Z, so the ENTRANCE faces +Z = high rows.
"""
import json
import math
import os

OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "formic_structures")

PALETTE = {
    "M": "mound",
    "D": "dirt",
    "R": "rooted",
    "m": "moss",
    "#": "dark",
    "C": "food_core",
    "o": "food_node",
    "g": "glow",
    "P": "path",
    ".": "air",
}

AIR = "."


def blank(w, d, layers):
    return [[[AIR for _ in range(w)] for _ in range(d)] for _ in range(layers)]


def accent(col, row, y):
    """Deterministic surface dressing so the shell is not monotone dark earth.
    Mixes rooted dirt, coarse dirt and moss patches into the mound shell.
    The bottom two layers stay pure NEST_MOUND so the earthen base reads solid
    and native (the campus gametest samples native mass at y=1)."""
    if y <= 1:
        return "M"
    h = (col * 7 + row * 13 + y * 5) % 100
    if h < 14:
        return "R"   # rooted dirt patch (lighter, roots)
    if h < 26:
        return "D"   # coarse dirt patch
    if h < 34:
        return "m"   # moss patch (green life on the mound)
    return "M"


def carve_dome(grid, w, d, centers, profile, wall, interior_top):
    """centers: list of (cx, cz, radius_scale). profile: radius per y layer.
    Fills the union of disks as shell 'M', then carves a hollow interior."""
    layers = len(profile)
    for y in range(layers):
        base_r = profile[y]
        # fill shell (union of all bulb disks at this layer)
        for row in range(d):
            for col in range(w):
                inside = False
                for (cx, cz, scale) in centers:
                    r = base_r * scale
                    if r <= 0:
                        continue
                    dist = math.hypot(col - cx, row - cz)
                    if dist <= r + 0.25:
                        inside = True
                        break
                if inside:
                    grid[y][row][col] = accent(col, row, y)
        # carve interior air (leave solid floor at y0 and a solid cap up top)
        if 1 <= y <= interior_top:
            for row in range(d):
                for col in range(w):
                    if grid[y][row][col] == AIR:
                        continue
                    for (cx, cz, scale) in centers:
                        r = base_r * scale - wall
                        if r <= 0:
                            continue
                        if math.hypot(col - cx, row - cz) <= r:
                            grid[y][row][col] = AIR
                            break


def add_entrance(grid, w, d, cx, cz, height):
    """Carve a south-facing tunnel (+Z) through the shell into the interior."""
    for y in range(1, height + 1):
        for col in range(cx - 1, cx + 2):
            for row in range(cz, d):
                if grid[y][row][col] != AIR:
                    grid[y][row][col] = AIR
    # ground-level path corridor leading in
    for col in range(cx - 1, cx + 2):
        for row in range(cz, d):
            if grid[0][row][col] == AIR:
                grid[0][row][col] = "P"
    # dark mouth rim: the outermost solid ring around the opening reads as a tunnel
    for y in range(1, height + 1):
        for col in range(cx - 2, cx + 3):
            for row in range(d - 1, -1, -1):
                if grid[y][row][col] in ("M", "R", "D", "m"):
                    grid[y][row][col] = "#"
                    break


def add_food_core(grid, w, d, cx, cz, top):
    """Stored-food core column down the centre of the chamber. NOTE: no side
    food nodes in the interior - those collide with the recurring-event markers
    (famine/brood) that the gameplay code safeSet()s onto the building, and a
    FOOD_NODE there blocks the marker. Entrance baskets convey 'food' instead."""
    for y in range(0, top):
        if grid[y][cz][cx] == AIR or y == 0:
            grid[y][cz][cx] = "C" if y % 2 == 0 else "o"


def add_glow_vents(grid, w, d, cx, cz, spots):
    """A few glowing vents on the upper shell so the dome is not a dark lump."""
    for (col, row, y) in spots:
        if 0 <= y < len(grid) and 0 <= row < d and 0 <= col < w:
            if grid[y][row][col] in ("M", "R", "D", "m"):
                grid[y][row][col] = "g"


def add_food_baskets(grid, w, d, cx, cz):
    """Food-node 'baskets' on the ground around the south entrance so the
    building visibly reads as a FOOD store from the front."""
    for (dc, dr) in ((-3, 0), (3, 0), (-3, -1), (3, -1)):
        col, row = cx + dc, cz + dr
        if 0 <= row < d and 0 <= col < w and grid[0][row][col] in ("M", "R", "D", "m", AIR):
            grid[0][row][col] = "o"
        if 0 <= row < d and 0 <= col < w and len(grid) > 1 and grid[1][row][col] in ("M", "R", "D", "m"):
            grid[1][row][col] = "o"


def to_json(name, note, grid):
    layers = []
    for y in range(len(grid)):
        rows = ["".join(grid[y][row]) for row in range(len(grid[y]))]
        layers.append({"y": y, "rows": rows})
    return {"name": name, "note": note, "palette": PALETTE, "layers": layers}


def build_spire():
    """v2 (CHOSEN, hero food building): tall termite-style mound - a broad
    bulged base sweeping up into a tapering organic chimney. Bigger and more
    richly dressed than the first pass: clearer arch entrance, food baskets at
    the door, moss/root patches and several glow vents up the flank."""
    w, d = 15, 15
    cx, cz = 7, 7
    # radius per layer: broad bulged base, long smooth taper to a chimney cap
    profile = [5.4, 5.9, 5.9, 5.6, 5.2, 4.7, 4.1, 3.5, 2.9, 2.4, 1.9, 1.5, 1.2, 1.0, 0.9]
    grid = blank(w, d, len(profile))
    carve_dome(grid, w, d, [(cx, cz, 1.0)], profile, wall=1.9, interior_top=10)
    add_entrance(grid, w, d, cx, cz, height=4)
    add_food_core(grid, w, d, cx, cz, top=9)
    add_food_baskets(grid, w, d, cx, cz)
    # glowing vents climbing the flanks + a beacon near the chimney
    add_glow_vents(grid, w, d, cx, cz, [
        (cx - 4, cz, 3), (cx + 4, cz, 3), (cx - 3, cz - 3, 5), (cx + 3, cz + 3, 5),
        (cx, cz - 4, 4), (cx, cz, 11),
    ])
    # frame the doorway with two glow lanterns at the mouth
    for dc in (-2, 2):
        if grid[1][d - 4][cx + dc] in ("M", "R", "D", "m", "#"):
            grid[1][d - 4][cx + dc] = "g"
    return to_json("food_chamber_v2", "Tall termite-style spire (chosen hero food building): broad bulged base, tapering organic chimney, arched south entrance with food baskets, moss/root/glow dressing, hollow food core.", grid)


def build_cluster():
    """v3: clustered multi-bulb mound - one main dome fused with two satellites."""
    w, d = 19, 13
    main = (8, 6, 1.0)
    sat_a = (14, 7, 0.62)
    sat_b = (3, 5, 0.55)
    profile = [4.4, 4.6, 4.4, 4.0, 3.4, 2.8, 2.2, 1.6, 1.1, 0.8]
    grid = blank(w, d, len(profile))
    carve_dome(grid, w, d, [main, sat_a, sat_b], profile, wall=1.7, interior_top=6)
    add_entrance(grid, w, d, main[0], main[1], height=3)
    add_food_core(grid, w, d, main[0], main[1], top=5)
    # satellites get their own little food nodes
    add_glow_vents(grid, w, d, main[0], main[1], [
        (main[0] - 3, main[1], 4), (sat_a[0], sat_a[1] - 2, 3), (sat_b[0], sat_b[1] - 1, 2),
    ])
    return to_json("food_chamber_v3", "Clustered multi-bulb mound: a main food dome fused with two smaller satellite bulbs, hollow, south tunnel.", grid)


def build_campus_food():
    """GAMEPLAY food building (FOOD_STORE): the chosen v2 spire shape resized to
    fit the campus envelope (radius <= 5 so it clears the diplomacy caches 6 out,
    height <= 14 per the campus mound profile). Same termite-spire identity as the
    preview hero, just packed into the campus grid."""
    w, d = 11, 11
    cx, cz = 5, 5
    # radius <= 5 at the base, smooth taper to a closed chimney cap by y12
    profile = [4.8, 5.0, 4.9, 4.6, 4.2, 3.7, 3.2, 2.7, 2.2, 1.7, 1.3, 1.0, 0.8]
    grid = blank(w, d, len(profile))
    carve_dome(grid, w, d, [(cx, cz, 1.0)], profile, wall=1.8, interior_top=9)
    add_entrance(grid, w, d, cx, cz, height=3)
    add_food_core(grid, w, d, cx, cz, top=7)
    add_food_baskets(grid, w, d, cx, cz)
    add_glow_vents(grid, w, d, cx, cz, [
        (cx - 3, cz, 3), (cx + 3, cz, 3), (cx, cz - 3, 4), (cx, cz, 10),
    ])
    # FOOD_CHAMBER core guaranteed at the exact site centre (gametest landmark)
    grid[0][cz][cx] = "C"
    return to_json("food_store", "Gameplay food building: campus-fit termite spire (radius<=5, height<=14), arched south entrance, food baskets, hollow FOOD_CHAMBER core, moss/root/glow dressing.", grid)


def _lobe(dx, dz, sx, sz):
    return math.exp(-((dx / sx) ** 2 + (dz / sz) ** 2))


def build_queen(name, note, w, peak_h, rad_x, rad_z, peak_x, peak_z, lobes):
    """Central QUEEN_CHAMBER mound: a SOLID organic heightmap mass (unlike the
    hollow satellite spires) - every column filled to an off-centre dome height
    plus asymmetric shoulder lobes so it never prints concentric ziggurat rings.
    The gameplay detail functions (entrances/chambers/vents/trails) carve into
    this mass afterwards, so the schematic only owns the monumental silhouette."""
    d = w
    c = w // 2
    grid = blank(w, d, peak_h + 2)
    for row in range(d):
        for col in range(w):
            ndx = (col - c - peak_x) / rad_x
            ndz = (row - c - peak_z) / rad_z
            dist = math.sqrt(ndx * ndx + ndz * ndz)
            if dist > 1.06:
                continue
            dd = min(1.0, dist)
            base = peak_h * (0.5 + 0.5 * math.cos(dd * math.pi))
            relief = max(0.0, 1.0 - dd)
            extra = 0.0
            for (lx, lz, sx, sz, lh) in lobes:
                extra += lh * _lobe(col - c - lx, row - c - lz, sx, sz) * relief
            h = max(0, min(peak_h, int(round(base + extra))))
            for y in range(0, h + 1):
                grid[y][row][col] = accent(col, row, y)
    grid[0][c][c] = "C"
    return to_json(name, note, grid)


def build_queen_spire():
    # A: tall central cathedral spire with two shoulder lobes - vertical drama
    return build_queen(
        "queen_mound_a", "Central mound A - grand cathedral spire: broad base sweeping to a tall off-centre central peak with two asymmetric shoulder lobes.",
        w=25, peak_h=18, rad_x=11.5, rad_z=11.5, peak_x=2, peak_z=-2,
        lobes=[(-5, 4, 4.0, 4.0, 3.4), (5, 3, 3.4, 3.4, 2.6)],
    )


def build_queen_turrets():
    # B: central dome flanked by two tall offset turrets - fortress/capital read
    return build_queen(
        "queen_mound_b", "Central mound B - twin-turret citadel: a central dome with two tall offset sub-spires rising beside it like guard turrets.",
        w=25, peak_h=15, rad_x=12.0, rad_z=12.0, peak_x=0, peak_z=0,
        lobes=[(-7, -5, 3.0, 3.0, 10.0), (7, 4, 3.2, 3.2, 9.0), (1, 6, 2.6, 2.6, 5.0)],
    )


def build_queen_citadel():
    # C: broad, lower, sprawling layered mound with wide asymmetric shoulders
    return build_queen(
        "queen_mound_c", "Central mound C - broad layered citadel: a wide low monumental mound with sprawling asymmetric shoulders, more horizontal authority than height.",
        w=27, peak_h=13, rad_x=13.5, rad_z=13.0, peak_x=-1, peak_z=1,
        lobes=[(-7, 1, 5.5, 5.5, 4.0), (6, -3, 5.0, 5.0, 3.6), (2, 7, 4.5, 4.5, 3.0)],
    )


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    for builder in (build_spire, build_cluster, build_campus_food,
                    build_queen_spire, build_queen_turrets, build_queen_citadel):
        doc = builder()
        path = os.path.join(OUT_DIR, doc["name"] + ".json")
        with open(path, "w", encoding="utf-8") as fh:
            json.dump(doc, fh, indent=2)
            fh.write("\n")
        print("wrote", os.path.normpath(path), "layers:", len(doc["layers"]))


if __name__ == "__main__":
    main()
