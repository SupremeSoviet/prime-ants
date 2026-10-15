# T10 bounded scale and normal disk reopen checkpoint (2026-10-15)

The opt-in development task is `runColonyScaleServer`. It uses the gametest source set,
an owned `build/run/t10-colony-scale/<attempt>` directory and an explicit JSON declaration.
It leaves the historical `runRestartServer` task and its users unchanged.

Before launch, create and freeze a declaration with `dimension`, plus `eula.txt` and
`server.properties` in the owned run directory. The seed is 0, view/simulation distances
are 2, `pause-when-empty-seconds=0`, no players, port 0 and offline local operation.
Use the `flat_all_dimensions` development world preset. Timed phases run sequentially,
alone among this direction's servers, at 20 TPS without sprint and work/brood scaling 1/1.

```powershell
.\gradlew.bat runColonyScaleServer --console=plain `
  -PprimeAntsScaleAttempt=<attempt> -PprimeAntsScalePhase=<phase> `
  -PprimeAntsScaleEvidence=<turnloop>/directions/prime-ants-stage1c/turns/T10/raw/<evidence> `
  -PprimeAntsScaleDeclaration=<turnloop>/directions/prime-ants-stage1c/turns/T10/raw/<declaration>.json
```

The phases are `probe` (20 loaded ticks), `empty`, `one`, `three` (600 contiguous
warm-up + 2,400 measured ticks), `prepare` (bounded nonempty persistence setup),
`reopen1` and `reopen2` (at least 200 fully loaded ticks in each new server process).
Each invocation closes normally and reports its real Java PID and native exit. The
`prepare`/reopen phases require the same already-saved world directory and reuse its
cohort manifest. The reopen phases never insert actors or supplies.

The full-size cohorts start with genuine generated Mature colonies (queen +24), then
receive 95 explicitly labelled supplied adult workers with canonical lineage and
membership. Every supplied adult starts mature, age 4,800 +10 per index, with one
actually ingested supplied apple. This is a controlled initial condition, not observed
birth, construction, natural growth or sustainability. Existing generated workers and
queens keep their production initial state. Each colony receives finite named drops of
64 apples, 32 chickens and 16 clay balls; no sample refills occur. No AI, lifespan,
hunger, costs, priorities, checks or caps are changed. The actual live stage/cap must be
reported: a supplied 120-body Mature cohort can exceed its derived cap without proving
a Great colony or a maximum breeding/mining workload.

Tickets of radius 4, type `(0,14)`, hold the declared regions. A declaration may supply
an exact `bases` list for the same bounded 16-cell selector. Read-only seed/profile
derivation must validate each region first; a finite region can contain no Mature
candidate. The corrected three-colony input uses bases 6000/6200/6500. The baseline uses an
equivalent empty flat dimension with three regions; populated scenarios use one or
three declared regions in `placement_soil`. Loading/chunk and other entity differences
are reported, and absolute native time remains the budget result.

Sampling reads MinecraftServer's current `tickCount % 100` native tick ring at Fabric
END_SERVER_TICK, after native tallying. Native work excludes scheduled sleep and the
END observer. Compact rows retain all actor ages and identity order. Serialization,
snapshots and formatting happen outside the measured window. Observer cost and actual
end-to-end cadence are separate. Each populated scenario's prospective native mean
budget is <=40 ms and p95 <=50 ms in the ordinary 50 ms/tick envelope.

The persistence world is the three-colony measurement world, with a separately declared
generated Young region added during `prepare`. Naturally laid/progressing brood and
claimed carried food must be present at the pre-close boundary; actual material cargo,
custody and terminal histories are labelled by exercised coverage. Complete actor,
block entity, registry and local terrain snapshots retain exact components, identities,
clocks, routes, claims, inventories and provenance. First normal decoded actors/block
entities and first loaded registries are compared before evaluating subsequent loaded
advances. Any normalization must name the exact field and cause. Empty states prove
only empty-state preservation. This is normal close/reopen consistency, not crash or
power-loss durability.

The settling verdict and exact commands, frozen inputs, native logs, XML and limitations
are in `<turnloop>/directions/prime-ants-stage1c/turns/T10/raw/t10-scale-persistence-acceptance.json`.

## Measured checkpoint and limits

T10's checkpoint is negative. Each row below has 600 contiguous warm-up ticks and
2,400 measured ticks; every declared adult's loaded age advanced by one on every tick.
No optimization, production behavior change, AI suppression or biological discount was used.

| Scene | Adults | Mean / median / p95 / p99 / max native ms | Above 50 ms | Observed TPS | Budget |
|---|---:|---|---:|---:|---|
| Empty, three equivalent flat regions | 0 | 0.308 / 0.300 / 0.373 / 0.438 / 3.490 | 0% | 20.001 | baseline |
| One supplied full-size Mature colony | 120 | 38.447 / 36.054 / 63.828 / 76.376 / 431.485 | 7.792% | 19.628 | FAIL p95 |
| Three supplied full-size Mature colonies | 360 | 115.134 / 106.592 / 195.508 / 213.909 / 736.469 | 96.750% | 8.620 | FAIL mean/p95 |

Observer means were 0.015 / 0.068 / 0.233 ms respectively. The machine was a Ryzen 7
7700 (8 cores/16 logical), approximately 95.1 GiB RAM, Windows 11 Pro build 26200,
Temurin 25.0.3+9. The heap was 3 GiB. Shared-machine contention was observed without
stopping or waiting for the parallel direction. The actual live stage/cap was Mature/60
under an unchanged operator bound of 120. These are above-cap supplied cohorts, not
evidence of Great-stage growth or sustainability. Nursing, movement, food trips, real
paid wall construction and cap refusal occurred. Searching was absent in the one-colony
measured window and present for 37 ticks in the three-colony window; mining and births
were absent. Full caches blocked some food carriers. No worst-case workload is claimed.

A 200-loaded-tick diagnostic JFR reopen of the normally closed one-colony world used a
new process and found exact first-decoded actor, block-entity and registry equality.
It is one scoped diagnostic reload, not the required two-reopen persistence cohort.
JFR localized repeated habitat validation through `nurseryFindings`,
`ColonyTerrain.generatedOpen/matches`, chunk availability and geometry membership.

The three-colony world's later preparation added one generated Young region (365 total
adults), then hit the unchanged 60-second watchdog before a normal close. The sampled
stack was in the same habitat-check path. The watchdog compares wall time to the next
scheduled tick, so its message alone is not proof of one intrinsic 60-second method call.
The crash and world are preserved; no acceptance reopen, reinitialization, save editing,
watchdog extension or unchanged retry followed. Naturally progressing brood and complete
nonempty-state restoration remain unproved. Further persistence work must first recover
the performance/loading prerequisite. The declared 6,000-tick preparation bound also
needs review against default fasting/crop and food-income clocks; no biology was sped up.

An independent Windows long-destination report-copy failure was repaired by retaining
ZIP entries and hashes. Process metadata is now written before report archival so an
archiver failure cannot erase the observed child exit. The original missing runner
record is disclosed, not reconstructed as an observed parent exit. T09's earlier lost
failed unit/model reports remain a historical retention gap.

Production, scheduling and canonical serialization sources are unchanged. T10 uses
`checkFast` and the exact twelve `EstablishedGenerationGameTest` long identities:
the shared changed observer affects those bodies; scale hooks are inert without their
explicit property. The complete unfiltered T12 suite remains mandatory. Performance
and persistence are not accepted, and 0.2.0 release acceptance has not begun.
