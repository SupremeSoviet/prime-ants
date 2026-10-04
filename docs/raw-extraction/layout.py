"""Layout analysis: replicates ColonyBuilder.siteFor + blueprint footprints and checks
site overlaps / distance to the cleared radius. Also vault stats."""
import json, math, os
from bpstats import Mound, REPO, jfloor, jceil

def load(name):
    return Mound(json.load(open(os.path.join(REPO, name + ".json"), encoding="utf-8")))

FAMILIES = {
    "FOOD_STORE": ["food_store_a", "food_store_b"],
    "NURSERY": ["nursery_a", "nursery_b"],
    "MINE": ["mine_a", "mine_b"],
    "CHITIN_FARM": ["chitin_farm_a", "chitin_farm_b", "chitin_farm_c"],
    "BARRACKS": ["barracks_a", "barracks_b"],
    "MARKET": ["market_a", "market_b"],
    "PHEROMONE_ARCHIVE": ["pheromone_archive_a", "pheromone_archive_b"],
    "ARMORY": ["armory_a", "armory_b"],
    "DIPLOMACY_SHRINE": ["diplomacy_shrine_a", "diplomacy_shrine_b"],
    "RESIN_DEPOT": ["resin_depot_a", "resin_depot_b"],
    "FUNGUS_GARDEN": ["fungus_garden_a", "fungus_garden_b"],
    "VENOM_PRESS": ["venom_press_a", "venom_press_b"],
    "WATCH_POST": ["watch_post_a", "watch_post_b", "watch_post_c"],
    "TRADE_HUB": ["trade_hub"],
    "QUEEN_CHAMBER": ["queen_mound_stage_1"],
    "GREAT_MOUND": ["queen_mound_stage_2"],
}

def site(t, e):
    if t == "QUEEN_CHAMBER": return (0, 0)
    if t == "FOOD_STORE": return (38, 0) if e == 0 else (66 + (e-1)*28, 21 + (e-1)*24)
    if t == "NURSERY": return (-38, 0) if e == 0 else (-66 - (e-1)*28, -21 - (e-1)*24)
    if t == "MINE": return (0, 38) if e == 0 else (21 + (e-1)*28, 66 + (e-1)*28)
    if t == "CHITIN_FARM": return {0: (-38, 34), 1: (-67, 58)}.get(e, (-91 - (e-2)*30, 30 - (e-2)*26))
    if t == "BARRACKS": return (0, -38) if e == 0 else (25 + (e-1)*31, -68 - (e-1)*28)
    if t == "MARKET": return (34, -34) if e == 0 else (66 + (e-1)*31, -57 - (e-1)*28)
    if t == "DIPLOMACY_SHRINE": return (-36, -42) if e == 0 else (-70 - (e-1)*32, -75 - (e-1)*28)
    if t == "WATCH_POST": return {0: (58, -104), 1: (-103, -68), 2: (108, 88)}.get(e, (-125 - (e-3)*34, 82 + (e-3)*31))
    if t == "RESIN_DEPOT": return (50, 50) if e == 0 else (82 + (e-1)*33, 79 + (e-1)*29)
    if t == "PHEROMONE_ARCHIVE": return (-58, -18) if e == 0 else (-88 - (e-1)*30, -43 - (e-1)*26)
    if t == "FUNGUS_GARDEN": return (-70, 92) if e == 0 else (-108 - (e-1)*38, 119 + (e-1)*29)
    if t == "VENOM_PRESS": return (92, -8) if e == 0 else (129 + (e-1)*37, 22 + (e-1)*30)
    if t == "ARMORY": return (0, -72) if e == 0 else (35 + (e-1)*32, -82 - (e-1)*28)
    if t == "GREAT_MOUND": return (0, 0)
    if t == "TRADE_HUB": return (74 + e*34, -52 - e*30)
    raise KeyError(t)

cache = {}
def shadow(name):
    if name not in cache:
        m = load(name)
        x0, x1, z0, z1, my = m.bounds()
        s = set()
        for y in range(0, my + 1):
            for x in range(x0, x1 + 1):
                for z in range(z0, z1 + 1):
                    if m.is_solid(x, y, z):
                        s.add((x, z))
        cache[name] = s
    return cache[name]

def placed(t, e, ox=0, oz=0):
    sx, sz = site(t, e)
    return [(v, {(x + sx, z + sz) for (x, z) in shadow(v)}) for v in FAMILIES[t]]

def diamond(cx, cz, r):
    return {(cx + x, cz + z) for x in range(-2, 3) for z in range(-2, 3) if abs(x) + abs(z) <= r}

items = []
for t, n in [("QUEEN_CHAMBER", 1), ("GREAT_MOUND", 1), ("FOOD_STORE", 3), ("NURSERY", 3), ("MINE", 3),
             ("CHITIN_FARM", 6), ("BARRACKS", 3), ("MARKET", 2), ("DIPLOMACY_SHRINE", 2), ("WATCH_POST", 6),
             ("RESIN_DEPOT", 2), ("PHEROMONE_ARCHIVE", 2), ("FUNGUS_GARDEN", 2), ("VENOM_PRESS", 2),
             ("ARMORY", 2), ("TRADE_HUB", 1)]:
    for e in range(n):
        items.append((f"{t}#{e}", placed(t, e)))
# expansion outpost watch posts (claim edge 34..42 -> x = edge, z = 12)
for edge in (34, 40, 42):
    items.append((f"OUTPOST_WATCH@({edge},12)", [(v, {(x + edge, z + 12) for (x, z) in shadow(v)}) for v in FAMILIES["WATCH_POST"]]))
nodes = {"FOOD_NODE(54,8)": diamond(54, 8, 3), "ORE_NODE(8,54)": diamond(8, 54, 3), "CHITIN_NODE(-54,8)": diamond(-54, 8, 3)}
for k, v in nodes.items():
    items.append((k, [("cluster", v)]))

print("== site distances / clear radius 72 coverage (origin 0,0) ==")
for name, variants in items:
    if "NODE" in name:
        continue
    t = name.split("#")[0]
    for v, cells in variants[:1]:
        pass
    far = max(max(math.hypot(x, z) for (x, z) in cells) for _, cells in variants)
    near = min(min(math.hypot(x, z) for (x, z) in cells) for _, cells in variants)
    print(f"{name:28s} center-dist={math.hypot(*site(t, int(name.split('#')[1]))) if '#' in name else 0:6.1f} "
          f"footprint dist range {near:5.1f}..{far:5.1f}  {'OUTSIDE r72' if near > 72 else ('PARTLY outside' if far > 72 else 'inside')}")

print("\n== overlaps (any variant combination) ==")
for i in range(len(items)):
    for j in range(i + 1, len(items)):
        a, av = items[i]
        b, bv = items[j]
        if {a.split('#')[0], b.split('#')[0]} == {"QUEEN_CHAMBER", "GREAT_MOUND"}:
            continue
        worst = 0
        combos = []
        for va, ca in av:
            for vb, cb in bv:
                ov = len(ca & cb)
                if ov:
                    combos.append(f"{va}/{vb}:{ov}")
                worst = max(worst, ov)
        if worst:
            print(f"{a} x {b}: max {worst} shared columns; {', '.join(combos)}")
