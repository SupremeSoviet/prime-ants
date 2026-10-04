"""Replicates TieredMoundBlueprint / SubterraneanVaultBlueprint geometry (Java) to
report exact solid-cell statistics for every blueprint. Read-only on the repo."""
import json, math, os, sys
from collections import Counter

REPO = r"C:/Users/user/Documents/Codex/2026-04-26/new-chat/src/main/resources/formic_blueprints"
M64 = (1 << 64) - 1


def to_signed(v):
    v &= M64
    return v - (1 << 64) if v >> 63 else v


def urs(v, n):  # java >>> on long
    return (v & M64) >> n


def signed_noise(x, y, z, seed):
    value = (x * 73428767) ^ (y * 912931) ^ (z * 43828933) ^ (seed * 199999)
    value &= M64
    value ^= urs(value, 33)
    value = (value * 0xff51afd7ed558ccd) & M64
    value ^= urs(value, 33)
    return ((value & 0xffff) / 32767.5) - 1.0


def jfloor(v):
    return int(math.floor(v))


def jceil(v):
    return int(math.ceil(v))


def floor_mod(a, b):
    return a % b


class Mound:
    def __init__(self, d):
        self.d = d
        self.seed = d["seed"]
        self.tiers = d["tiers"]
        self.terraces = d.get("terraces", [])
        self.chambers = {c["id"]: c for c in d["chambers"]}
        self.chamber_list = d["chambers"]
        self.pits = d.get("pits", [])
        self.connections = d.get("connections", [])
        self.mouths = d.get("mouths", [])

    @staticmethod
    def top_y(t):
        return t["baseY"] + t["height"] - 1

    def tier_dist(self, t, x, y, z):
        p = (y - t["baseY"]) / (t["height"] - 1)
        rx = t["baseRadiusX"] + (t["topRadiusX"] - t["baseRadiusX"]) * p
        rz = t["baseRadiusZ"] + (t["topRadiusZ"] - t["baseRadiusZ"]) * p
        nx = (x - t["offsetX"]) / rx
        nz = (z - t["offsetZ"]) / rz
        return nx * nx + nz * nz

    def contains(self, x, y, z):
        for tr in self.terraces:
            if tr["y"] - tr["thickness"] + 1 <= y <= tr["y"]:
                nx = (x - tr["x"]) / tr["radiusX"]
                nz = (z - tr["z"]) / tr["radiusZ"]
                if nx * nx + nz * nz <= 1.0:
                    return True
        for t in self.tiers:
            if not (t["baseY"] <= y <= self.top_y(t)):
                continue
            dd = self.tier_dist(t, x, y, z)
            if dd <= 0.82:
                return True
            if dd <= 1.0 + signed_noise(x, y, z, self.seed) * 0.07:
                return True
        return False

    @staticmethod
    def ch_top(c):
        return c["floorY"] + c["height"]

    def ch_carves(self, c, px, py, pz):
        if py <= c["floorY"] or py > self.ch_top(c):
            return False
        top = self.ch_top(c)
        scale = 1.0 if c.get("openToSky") else (0.72 if py == top else (0.9 if py == top - 1 else 1.0))
        nx = (px - c["x"]) / (c["radiusX"] * scale)
        nz = (pz - c["z"]) / (c["radiusZ"] * scale)
        return nx * nx + nz * nz <= 1.0

    def mouth_carves(self, m, px, py, pz):
        hw = m["width"] // 2
        top = m["y"] + m["height"] - 1
        rear = m["frontZ"] + m["depth"]
        if px < m["x"] - hw or px > m["x"] + hw or py < m["y"] or py > top or pz < m["frontZ"] or pz >= rear:
            return False
        return py != top or abs(px - m["x"]) != hw or floor_mod(px + py + pz, 2) == 0

    def pit_depth_at(self, p, px, pz):
        nx = (px - p["x"]) / p["radiusX"]
        nz = (pz - p["z"]) / p["radiusZ"]
        dd = nx * nx + nz * nz
        if dd > 1.0:
            return 0
        return p["depth"] if dd <= 0.36 else max(1, p["depth"] - 1)

    def pit_carves(self, p, px, py, pz):
        owner = self.chambers[p["chamber"]]
        ld = self.pit_depth_at(p, px, pz)
        return ld > 0 and py <= owner["floorY"] and py > owner["floorY"] - ld

    @staticmethod
    def dxdz(direction):
        return {"east": (1, 0), "west": (-1, 0), "south": (0, 1), "north": (0, -1)}.get(direction, (0, 0))

    def conn_carves(self, c, px, py, pz):
        f = self.chambers[c["from"]]
        t = self.chambers[c["to"]]
        rise = t["floorY"] - f["floorY"]
        dx, dz = self.dxdz(c["direction"])
        sx, sz = -dz, dx
        for step in range(rise):
            for lane in range(c["width"]):
                x = c["startX"] + dx * step + sx * lane
                y = f["floorY"] + step
                z = c["startZ"] + dz * step + sz * lane
                if px == x and pz == z and y + 1 <= py <= y + 2:
                    return True
        lx = c["startX"] + dx * rise
        lz = c["startZ"] + dz * rise
        return px == lx and pz == lz and t["floorY"] + 1 <= py <= t["floorY"] + 2

    def is_solid(self, x, y, z):
        if not self.contains(x, y, z):
            return False
        if any(self.mouth_carves(m, x, y, z) for m in self.mouths):
            return False
        if any(self.ch_carves(c, x, y, z) for c in self.chamber_list):
            return False
        if any(self.pit_carves(p, x, y, z) for p in self.pits):
            return False
        if any(self.conn_carves(c, x, y, z) for c in self.connections):
            return False
        return True

    def bounds(self):
        tmin_x = min(jfloor(t["offsetX"] - t["baseRadiusX"]) - 1 for t in self.tiers)
        tmax_x = max(jceil(t["offsetX"] + t["baseRadiusX"]) + 1 for t in self.tiers)
        tmin_z = min(jfloor(t["offsetZ"] - t["baseRadiusZ"]) - 1 for t in self.tiers)
        tmax_z = max(jceil(t["offsetZ"] + t["baseRadiusZ"]) + 1 for t in self.tiers)
        if self.terraces:
            tmin_x = min(tmin_x, min(jfloor(r["x"] - r["radiusX"]) - 1 for r in self.terraces))
            tmax_x = max(tmax_x, max(jceil(r["x"] + r["radiusX"]) + 1 for r in self.terraces))
            tmin_z = min(tmin_z, min(jfloor(r["z"] - r["radiusZ"]) - 1 for r in self.terraces))
            tmax_z = max(tmax_z, max(jceil(r["z"] + r["radiusZ"]) + 1 for r in self.terraces))
        max_y = max(self.top_y(t) for t in self.tiers)
        return tmin_x, tmax_x, tmin_z, tmax_z, max_y

    def stats(self):
        x0, x1, z0, z1, my = self.bounds()
        solid = []
        contained = 0
        for y in range(0, my + 1):
            for x in range(x0, x1 + 1):
                for z in range(z0, z1 + 1):
                    if self.contains(x, y, z):
                        contained += 1
                        if self.is_solid(x, y, z):
                            solid.append((x, y, z))
        foot = [(x, z) for (x, y, z) in solid if y == 0]
        xs = [c[0] for c in solid]
        zs = [c[2] for c in solid]
        fxs = [c[0] for c in foot]
        fzs = [c[1] for c in foot]
        # shadow = union of all columns having any solid cell
        shadow = {(x, z) for (x, y, z) in solid}
        carved = contained - len(solid)
        return {
            "solid": len(solid),
            "footprint_y0": len(foot),
            "shadow_cols": len(shadow),
            "x": (min(xs), max(xs)), "z": (min(zs), max(zs)),
            "width": max(xs) - min(xs) + 1, "depth": max(zs) - min(zs) + 1,
            "foot_x": (min(fxs), max(fxs)), "foot_z": (min(fzs), max(fzs)),
            "height": max(c[1] for c in solid) + 1,
            "carved_in_envelope": carved,
            "bbox": (x0, x1, z0, z1, my),
        }


def main():
    files = sorted(f for f in os.listdir(REPO) if f.endswith(".json"))
    for f in files:
        d = json.load(open(os.path.join(REPO, f), encoding="utf-8"))
        if "surfaceAccess" in d:
            continue
        m = Mound(d)
        s = m.stats()
        print(f"{f[:-5]:24s} solid={s['solid']:5d} foot(y0)={s['footprint_y0']:4d} shadow={s['shadow_cols']:4d} "
              f"W x D = {s['width']:2d} x {s['depth']:2d} (x {s['x'][0]}..{s['x'][1]}, z {s['z'][0]}..{s['z'][1]}) "
              f"H={s['height']:2d} carved={s['carved_in_envelope']:4d}")


if __name__ == "__main__":
    main()
