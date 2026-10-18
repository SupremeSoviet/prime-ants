# Performance

Every ant in Prime Ants is a real mob with its own needs and work, so a big colony costs real server time. This page
records how much, how it is measured and what is being done about it.

## Budget

At 20 TPS the server has 50 ms per tick for everything. A colony scene should leave room for the rest of the game:

- **mean** whole-server tick time of at most **40 ms**;
- **95th percentile** of at most **50 ms**.

## How it is measured

- A dedicated benchmark server builds Great colonies of 120 adults each (one queen and 119 workers), runs **600
  warm-up ticks**, then records **2,400 ticks**.
- Scenes: one Great colony (120 adults), three Great colonies (360 adults), and empty worlds as controls.
- The game runs at normal speed and a normal 20 TPS, with a 3 GiB heap.
- Times are for the whole server tick, not just the mod's code. Percentiles cover every measured tick.
- In these scenes ants move, carry food and nurse brood. They do not forage, build or mine while being measured, so
  real play can cost more.
- Machine: AMD Ryzen 7 7700 (8 cores, 16 threads), 95 GB RAM, Windows 11, Temurin 25 with G1. Nothing else runs on the
  machine during a measurement.

The benchmark server task is `runColonyScaleServer` (see `gradle/scale-evidence.gradle` and
`scripts/run-colony-scale.py`).

## Current numbers

Version 0.2.0, measured 2026-10-18:

| Scene | Adults | Mean | Median | p95 | p99 | Max | Ticks over 50 ms | TPS |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| One Great colony | 120 | 46.8 ms | 44.2 ms | 47.2 ms | 57.9 ms | 399 ms | 2.2% | 20.0 |
| Three Great colonies | 360 | 141.0 ms | 133.6 ms | 144.0 ms | 486.9 ms | 511 ms | 96.8% | 7.0 |
| Empty world | 0 | 0.13 ms | 0.10 ms | 0.21 ms | 0.24 ms | 14 ms | 0% | 20.0 |

One Great colony keeps 20 TPS but misses the 40 ms mean budget, so there is almost no headroom. Three Great colonies
miss both budgets by a wide margin.

An earlier measurement on 2026-10-17, made with the same scenes while other work was running on the machine, gave
51.7 ms mean / 66.5 ms p95 for one colony and 149 ms / 216 ms for three.

## Where the time goes

Profiling with Java Flight Recorder points at a few hot spots:

- **Nursery checks.** About 80% of server samples are inside the code that checks whether the nursery is still intact
  and usable (`nurseryFindings`). It re-reads the nursery's blocks every time it is asked.
- **Choosing whom to feed.** Most of those checks come from nurses deciding whom to feed next (`hasRecipient`, in about
  73% of samples).
- **Block lookups.** The most common leaf is a block-state lookup (`PalettedContainer.get`), followed by chunk and map
  lookups.
- **Not garbage collection.** GC pauses add up to about 45 ms over 600 ticks, with the longest at about 2 ms.
- **Empty job lookups.** Job lookups convert ant UUIDs to strings even when there are no jobs. This shows up in
  allocation profiles but costs little time.

## What has been tried

- **Checking the queen's hunger first** (2026-10-18). When a nurse picks whom to feed, the idea was to ask whether the
  queen needs food before running the nursery readiness check. It was rejected: the readiness check also finishes
  loading a nursery saved by an older version of the mod and starts its saved clock, so skipping it could leave such a
  nursery uninitialized. A regression test now covers this. No timing was taken.

## Next ideas

1. Remember the result of the nursery check and only re-check when the nursery's blocks change, instead of re-reading
   them on every call.
2. Skip the UUID-to-string conversion when a job map is empty.

Each change must leave gameplay exactly as it was, and is measured with the same scenes before it is kept.
