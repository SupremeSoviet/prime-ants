# T16 actual Great/cap120 performance checkpoint (2026-10-17)

**Valid negative: performance is not accepted.** T15 correctness remains accepted. T16 changes only the opt-in scale harness and guards for the scale task.

| Scene | Adults | Mean | Median | p95 | p99 | Max ms | >50ms | TPS | Verdict |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---|
| Empty one | 0 | 0.210 | 0.214 | 0.290 | 0.338 | 11.607 | 0 (0.000%) | 20.005 | CONTROL |
| One Great | 120 | 51.738 | 46.699 | 66.497 | 127.608 | 485.007 | 689 (28.708%) | 18.984 | FAIL |
| Empty three | 0 | 0.324 | 0.321 | 0.429 | 0.540 | 4.758 | 0 (0.000%) | 20.035 | CONTROL |
| Three Great | 360 | 149.034 | 137.372 | 215.943 | 490.748 | 563.165 | 2323 (96.792%) | 6.656 | FAIL |

Each populated scene must meet absolute whole-server native mean <=40 ms and p95 <=50 ms. Every scene uses 600 contiguous loaded warm-up ticks and 2,400 measured ticks, an ordinary 20 TPS request without sprint, work/brood multipliers 1/1 and a 3 GiB heap. Percentiles use nearest rank and every row. Empty controls match ticket centers; their dimensions and terrain differ. No subtraction or division by three decides acceptance.

Each colony has one queen and 119 living canonical workers. Generated Mature colonies of 25 adults receive 95 supplied adults, 40 supplied owned packed-clay shell cells and 32 supplied stone units. Production derives Great/cap120 from physical chambers, stocks and population. This establishes a controlled initial condition; births, worker construction, natural growth and sustainability remain unproved. One chicken ingestion per queen occurs before measurement. Measured ingestion, crop transfers, searching, construction and mining are absent. Movement, carrying, nursing-cache phase and cap refusal occur. All actors age normally; raw ages, live stage/cap and normal evaluation timestamps are retained.

The first three-colony setup fails before sampling because immediate Great evaluation precedes normal queen/hall readiness. A readiness gate bounded at 200 server ticks, then a successful physical probe with 360 adults for 20 ticks, precedes the fresh valid measurement. A final phase guard confines that gate to `probe/one/three`, preserving entry behavior for future persistence phases. No actor is repositioned or frozen, and no stage or cap is assigned. Valid timing scenes are not replayed for preferred results.

A separate JFR records 600 diagnostic ticks with 360 adults. Repeated habitat/block/chunk validation remains prominent: `nurseryFindings` appears in 1,762 of 8,738 server samples (20.16%). Overlapping stacks cannot be summed into time fractions. Allocation samples frequently reach UUID-string lookups in `NestExpansion.job` and `Mining.job`, although both job maps are empty throughout the measured scenes. The 163 recorded G1 events sum to 181 ms of pauses, with a maximum of 3.23 ms; pauses alone do not explain the miss.

A bounded next candidate is an empty-map check before `UUID.toString()` in those two job lookups. It must inspect the current canonical map on every call and retain serialization, ownership checks, world reads, AI and biology. Any candidate needs complete correctness acceptance and fresh frozen cap measurements. No production optimization is made or budget recovery promised in T16.

The machine is a Ryzen 7 7700 (8 cores/16 threads), Windows 11 Pro build 26200, approximately 95.1 GiB RAM and Temurin 25.0.3+9 with G1 defaults and a 3 GiB server heap. Aggregate contention, observer costs and cadence are retained; the shared machine was not isolated. The mandated development runtime also retains unchanged test mixins, including empty habitat-scope checks. Their costs are not subtracted. This is not a clean released-server performance guarantee. T10's Mature/cap60 negatives and T11's five-adult allocation result remain separate, without a controlled percentage speedup claim.

Both scoped correctness checkpoints pass: 77 fast, 124 unit, 26 model and 13 selected long cases, including all twelve `EstablishedGenerationGameTest` identities and the exact Great surface identity. The final checkpoint uses the final phase guard. Other long bodies are unaffected by changes confined to the opt-in harness and scale task; the next scheduled complete checkpoint is T18. No flaky exemption is used.

Frozen plans, errata/amendments, declarations, all 9,600 raw budget rows, age/stage matrices, two independent analysis passes, profile, native/merged XML, commands/exits, executable hashes and preservation checks are external local evidence in `<turnloop>/directions/prime-ants-stage1c/turns/T16/raw/t16-cap-performance-acceptance.json`.

Persistence remains open: prove the small default-speed Young brood control; require both actual food and material cargo with claims before the boundary; then perform two fresh-process reopens preserving state and resuming physical service. The legacy material flag and T10's brood-empty diagnostic are insufficient. No preparation, reopen, client acceptance or release/version work occurs in T16.
