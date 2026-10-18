<p align="center">
  <img src="docs/assets/banner.png" alt="A black garden ant carrying nectar through a flower meadow" width="100%">
</p>

<h3 align="center">Wild, realistic ant colonies for Minecraft. Every ant is a real creature.</h3>

<p align="center">
  🇷🇺 <a href="README.ru.md">Читать на русском</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-26.3-5B8731?style=flat-square" alt="Minecraft 26.3">
  <img src="https://img.shields.io/badge/Fabric_Loader-0.19.5-DBD0B4?style=flat-square" alt="Fabric Loader 0.19.5">
  <img src="https://img.shields.io/badge/Fabric_API-0.161.0%2B26.3-DBD0B4?style=flat-square" alt="Fabric API 0.161.0+26.3">
  <img src="https://img.shields.io/badge/Java-25-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java 25">
  <br>
  <a href="https://github.com/SupremeSoviet/prime-ants/releases/latest"><img src="https://img.shields.io/github/v/release/SupremeSoviet/prime-ants?style=flat-square&color=8B5A2B&label=release" alt="Latest release"></a>
  <img src="https://img.shields.io/badge/status-early_release-orange?style=flat-square" alt="Early release">
  <img src="https://img.shields.io/badge/species-Lasius_niger-3B2A20?style=flat-square" alt="Lasius niger">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue?style=flat-square" alt="MIT license"></a>
</p>

<p align="center">
  <a href="https://github.com/SupremeSoviet/prime-ants/releases/latest"><b>⬇️ Download</b></a> ·
  <a href="#-getting-started">Getting started</a> ·
  <a href="#%EF%B8%8F-gallery">Gallery</a> ·
  <a href="#%EF%B8%8F-roadmap">Roadmap</a>
</p>

---

> **[Release 0.2.0](https://github.com/SupremeSoviet/prime-ants/releases/tag/v0.2.0):** accepted T21 complete6, a fresh T23 fast checkpoint and the existing native client/four PNGs.
> On 2026-10-18 at 08:33:25 UTC the owner accepted the client recording exception: chunk `(10,9)`, cell `(2,2)`, 0–2,351 cumulative loaded search/wait ticks against 1,200. Compliance remains unproved; release acceptance does not prove the bound was met.
> [Current acceptance](docs/release-0.2.0-acceptance.md) · [EN/RU notes](docs/release-0.2.0.md) · [Known timeouts](docs/known-flaky-timeouts.md).

## 🐜 What is this?

Prime Ants fills your world with wild colonies of the black garden ant, *Lasius niger*.

A young queen chooses a patch of meadow, digs a small chamber and raises her first workers on her own. Then the
colony takes over: workers open the entrance, carry soil out into a mound, bring home nectar and prey, and feed the
brood mouth to mouth.

There are no hidden counters. **A colony is exactly the ants you can see.** If one dies, it is gone, and the colony has
to raise a replacement.

## ✨ Features

| | |
|---|---|
| 👑 **Queens found colonies on their own** | in plains, meadows and flower forests, dug into natural terrain |
| 🥚 **A real brood cycle** | egg → larva → cocoon → pale young worker → adult |
| ⛏️ **Nests dug into the ground** | stairs and chambers two blocks high, with the soil carried out into a mound |
| 🌼 **Real food, carried home** | nectar from flowers, small prey in the grass, and tasty things you drop |
| 🤝 **Sharing** | nurses feed the queen and the larvae, and workers pass sugar mouth to mouth |
| 📈 **Growth that follows food** | a colony grows only as fast as food comes in; ants age, starve and die |
| 🛡️ **They defend their home** | hurt an ant or break the nest, and nearby workers will bite |
| 🔦 **You can visit** | walk down the stairs, light a chamber with a torch and watch the nursery |
| 🏰 **Established nests** | Young and Mature colonies can appear in newly generated suitable terrain |
| 🧱 **Colony development** | four chamber functions, physical upgrades through tier three and Great fortifications |
| 💾 **Resuming after loading** | bounded automatic catch-up; persistence coverage is scoped in the release notes |

> 🕰️ One in-game day is roughly one month in the life of a colony.

## 🖼️ Gallery

Four individually reviewed native F2 PNGs from the accepted 0.2.0 runtime in the existing T21 fresh Survival world. The owner accepted the disclosed recording exception; no new client or screenshot campaign was run.

| | |
|:---:|:---:|
| <img src="docs/images/v0.2.0-t21/exterior.png" width="420" alt="Generated Young nest exterior beside a village; grass left intact."><br><sub>Generated Young nest exterior beside a village; grass left intact.</sub> | <img src="docs/images/v0.2.0-t21/worker-at-entrance.png" width="420" alt="Real worker at the existing entrance, before player lighting."><br><sub>Real worker at the existing entrance, before player lighting.</sub> |
| <img src="docs/images/v0.2.0-t21/populated-interior.png" width="420" alt="Living queen, workers and food cache in the existing chamber."><br><sub>Living queen, workers and food cache in the existing chamber.</sub> | <img src="docs/images/v0.2.0-t21/cache-detail.png" width="420" alt="Chamber, workers and physical cache detail from the opposite corner."><br><sub>Chamber, workers and physical cache detail from the opposite corner.</sub> |

<sub>Generated initial state, ordinary speed, player-camera access and one normal torch. Uninterrupted service after lighting is not claimed. [Provenance](docs/release-0.2.0-acceptance.md). The banner depicts an earlier model revision.</sub>

## 🚀 Getting started

**You need:** Minecraft Java Edition 26.3 · Java 25 · Fabric Loader 0.19.5 · Fabric API 0.161.0+26.3

1. Install a [Fabric](https://fabricmc.net/use/installer/) profile for Minecraft 26.3.
2. Put [`prime_ants-0.2.0.jar`](https://github.com/SupremeSoviet/prime-ants/releases/download/v0.2.0/prime_ants-0.2.0.jar) and the matching
   [Fabric API](https://modrinth.com/mod/fabric-api) into your `mods` folder.
3. Create a **new world** and go looking for ants. 🔍

## 🔎 Finding and enjoying a colony

- **Where to look.** Plains, meadows and flower forests. Look for a small mound of soil next to a stair entrance, or a
  queen busy digging.
- **Be patient.** Each brood stage takes about 10 minutes, and the first workers appear about half an hour after the
  first eggs. Colonies live only while their chunks are loaded, so stay nearby.
- **Feeding.** Drop apples or sweet berries (sugar) and raw chicken or rotten flesh (protein) near a trail, then step
  back at least four blocks. Foragers pick the food up and carry it home.
- **Visiting.** Once the workers open the entrance, walk down the two-block-high stairs. Put a torch on a wall to see
  inside, and leave the walls, the nursery and the food store alone.
- **Manners.** Watching and feeding are welcome. Hitting ants or breaking their nest makes the workers bite (half a
  heart per bite).

## ⚠️ Good to know

**Release 0.2.0 retains the disclosed owner-accepted recording exception and limitations below.**

- One species, *Lasius niger*. Established Young/Mature generation and automatic catch-up are implemented;
  nuptial flights and active fruit harvesting remain future work.
- Great fortifications and physical tier-three upgrades have controlled acceptance. This does not establish
  a complete unattended player growth campaign.
- Two fresh-process reopens of a finite Young/Mature world prove exact graceful persistence and resumed
  ordinary service. They do not establish crash durability or broad modpack compatibility.
- Terrain, food shortages and crowding can stall a colony. Long sessions and multiplayer have limited coverage;
  back up your worlds.

Several loaded Great colonies can cause substantial lag. T16 measured one colony with **120 adults:
about 19 TPS; mean 52 ms; p95 approximately 66 ms**. Three colonies with **360 adults:
about 6.7 TPS; mean 149 ms; p95 approximately 216 ms**.
The controlled development server ran on a Ryzen 7 7700 (8 cores/16 threads), Windows 11 Pro build 26200,
approximately 95.1 GiB RAM, Temurin 25.0.3+9/G1 and a 3 GiB heap; Minecraft 26.3, Loader 0.19.5,
Fabric API 0.161.0+26.3 and Loom 1.18.2. Each supplied scene used 600 warm-up and 2,400 measured ticks,
work/brood 1/1 and an ordinary 20 TPS request; shared-machine contention and test instrumentation remain included.
These are server ticks, not client FPS or universal hardware guarantees. The mean 40 ms / p95 50 ms budgets failed.
Performance remains a limitation outside the 0.2.0 gate; no new benchmark is claimed.
See [T16 conditions](docs/t16-cap-performance.md), [final EN/RU notes](docs/release-0.2.0.md),
[exact known-flaky timeouts](docs/known-flaky-timeouts.md) and [current acceptance](docs/release-0.2.0-acceptance.md).

## 🗺️ Roadmap

- 🪽 nuptial flights, with young queens founding new colonies nearby
- 🏰 established Young/Mature nests at world generation — delivered in 0.2.0
- 🌿 aphid herding and honeydew
- ⚔️ border fights and wars between colonies
- 📓 a field journal for your observations
- 🐜 more species: the red wood ant, a harvester ant and a leaf-cutter ant

## 🛠️ Building from source

```bash
./gradlew build
```

You need JDK 25. The jar ends up in `build/libs/`, and the build also runs the unit, model and in-game server tests.

The old Great timeout is retained in the historical report; Great later passed controlled acceptance.
The current release-correctness checkpoint is recorded in the [Stage-1 report](docs/stage-1-report.md).
The separate every-turn fast suite and packaging can be run with:

```bash
./gradlew checkFast assemble --console=plain --rerun-tasks
```

This checks the fast suite, not complete Stage-1 or release acceptance. Local builds carry version **0.2.0**; use the published asset and SHA256SUMS above to identify the exact tested release bytes.

## 📚 More

- [Player guide](docs/player-guide.md)
- [Technical report for 0.1.0](docs/slice-1-report.md)
- [Partial Stage-1 report](docs/stage-1-report.md)
- [Design decisions](docs/00-decisions.md) (in Russian)

## 📄 License

[MIT](LICENSE)
