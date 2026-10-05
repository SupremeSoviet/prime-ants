# Natural founding-queen placement feasibility - T14

The disabled experimental production pathway is feasible on genuine controlled generation: it automatically inserts a real queen with zero block edits, requests the existing founding behavior, and advances through actual entity ticks. The predeclared vanilla-noise experiment produced **zero placements**: **246 FULL chunks total** (243 native + 3 auxiliary), **192 candidate-region chunks**, **12 selected candidates**, **12 final rejections**, and **96 column rejections for unsupported biomes**. Five sampled chunks were biome eligible; none was selected by the sparse lattice. This establishes bounded safe rejection and persisted event identity in these regions. It does not establish natural-colony availability, native founding success, or survival playability.

The queen/outbound-worker single-throat lock remains a blocking gameplay defect. Placement diagnostics do not authorize captures, performance sampling or a release. The final current unfiltered build status is recorded separately below.

## Predeclared experiment and results

The declaration is `T14/experiment-declaration.json`; its earlier draft is preserved as `experiment-declaration-superseded-before-run.json`. Before any native attempt, the harness changed from GameTest to an ordinary dedicated headless server so the FULL budget includes auxiliary generation. An offline inspection showed that a focused GameTest world contains 31 auxiliary FULL chunks. No native outcome had been observed when this declaration was amended.

All three seeds used candidate chunks **x=0..7, z=0..7**, with a maximum FULL halo **x=-1..8, z=-1..8**. Only the 64 core chunks were explicitly requested; four native tickets at radius 2 supplied the required simulation and 81 native FULL chunks. One ordinary flat-overworld FULL chunk was also generated per server. Runtime guards count every generated FULL chunk in every dimension, cap each server at 100, and refuse native FULL coordinates outside the halo. Total **246 <= 300**. Normal lower-status generation dependencies extend outside the FULL halo; their saved status counts are in the independent audit. They are not FULL chunks or placement candidates.

| Vanilla seed | Native FULL / all FULL | Core chunks | Biome-eligible center samples | Selected / rejected / inserted | Column rejections |
|---|---:|---:|---:|---:|---:|
| 2026100501 | 81 / 82 | 64 | 5 | 4 / 4 / 0 | 32 unsupported biome |
| 42 | 81 / 82 | 64 | 0 | 4 / 4 / 0 | 32 unsupported biome |
| 8675309 | 81 / 82 | 64 | 0 | 4 / 4 / 0 | 32 unsupported biome |
| Total | 243 / 246 | 192 | 5 | 12 / 12 / 0 | 96 unsupported biome |

Eligibility counts sample `(chunk minX+7, live WORLD_SURFACE-1, chunk minZ+7)`, rather than claiming homogeneous chunk biomes. Detailed core samples, generated FULL coordinates, each candidate UUID/status/reason and the empty inserted-UUID list are preserved in each `seed-*/result.json` and `22-native-independent-audit.json`. Candidate terminal reasons are `bounded_search_exhausted`; the recorded underlying reason is `unsupported_biome` for all 96 evaluated columns. Those candidates never reached substrate or prospective-plan validation. It would be incorrect to infer a native soil or slope failure from this experiment.

Inserted native queen UUIDs: **none**. Native insertion terrain invariance and early founding were therefore **not observed**, rather than failed or measured as zero. The controlled generation tests supply insertion invariance and actual founding evidence. The candidate lattice centers are 64 blocks apart; offset variation guarantees at least 61 blocks between prospective queen positions, exceeding the declared 56-block minimum. Actual placed-queen spacing is unavailable for the native experiment.

Each native server observed 1,000 real server ticks with an explicitly requested sprint (no manual entity ticks), founding work 20x and brood 100x. This is a placement diagnostic, not an MSPT sample. No queens were present to advance; `early_founding=[]` in all three results. The three worlds were flushed, normally stopped and archived immediately before the next invocation.

## Production boundaries and initialization

`NaturalPlacement` is behind `-Dprime_ants.experimentalNaturalPlacement=true`, **false by default**, and production eligibility is restricted to `minecraft:overworld`. A development-only dimension provider enables isolated test dimensions through Fabric's development-environment check. It is excluded from the production jar. Vanilla overworld noise settings and biome source in `prime_ants_test:placement_native` use the actual recorded server seed; no block or soil origin is supplied by the harness.

The declared biome tag `prime_ants:founding_queen` includes plains, sunflower plains, meadow and flower forest. Selection is deterministic from world seed, dimension and chunk coordinates: one central chunk per 4x4 cell, with seed/dimension residues 1 or 2 on each axis. It does not scan historical worlds. Density skips 180 of the 192 core chunks in this experiment.

Generation workers only set the existing transient `GenerationWitness` after the genuine TERRAIN generation task. The LOADING pyramid does not run that task. Upgrading/retrogen protos are excluded. At the main-thread ProtoChunk-to-LevelChunk constructor, `NaturalSoil` records native origin and the placement ledger queues a selected candidate. That constructor reads the passed proto only; it does not query neighboring world chunks, create an entity or acquire tickets.

The server END_LEVEL_TICK queue evaluates at most **two candidates/columns per level tick**, eight deterministic central columns per candidate, and at most four existing `NestPlan.candidate` directions per column: at most **32 prospective plan calls per candidate / eight per tick**, plus bounded immediate revalidation on insertion. Refused insertion allows at most **three attempts** with a 20-tick retry delay. Unavailable chunks stay pending without loading requests. The whole prospective founding envelope is inside the selected chunk because offsets are 6..9 and the envelope extends at most six blocks; a FULL halo covers the larger read-only insertion snapshot. The selected chunk must have actual loaded/ticking entities and be in entity-ticking range.

Live validation checks the tagged biome, current surface, fluids, positively observed soil, body/support/occupancy, world border, prospective digging tasks, native shell/roof/floor, approach and supported deposits through existing protections. Existing nearby live queens cause an additional conservative spacing rejection. No historical event-ledger scan is used for spacing. The prospective plan, biome, readiness and actual body space are revalidated immediately before insertion.

Natural queens use the registered production `AntEntities.QUEEN` factory and `EntitySpawnReason.CHUNK_GENERATION`. `LasiusNigerEntity.finalizeSpawn` requests the same `QueenFounding.request()` used by egg-created queens. No debug egg, command, fixture marker, test adult factory, terrain clearing, vegetation removal, finished nest, brood, worker, food, mound or extra reserve is involved in positive placement. Only the normal finite queen initialization reserve is present. Normal founding work starts on subsequent real ticks.

Insertion uses the boolean `ServerLevel.addFreshEntity` result and verifies `level.getEntity(uuid)==queen`, actual level identity and nonremoved state. The bounded 17x17x8 block-state snapshot around the surface must remain identical across insertion. This assertion passed in controlled production-path insertions and the refused attempt. Production placement acquires no tickets and never calls FULL retrieval with `loadOrGenerate=true`.

## Persistence and deduplication

`prime_ants:natural_placement` is a SavedData **placement-event ledger**, not a population model. A candidate stores generation-authority version, seed, dimension, chunk, stable name-derived UUID, search cursor, attempt count, reasons and status. Repeated callbacks for the same key return without creating another candidate. UUID derivation was independently verified against all 12 archived native records.

Only the genuine conversion callback can mint a record. Chunk deserialization, historical/retrogen loading and SavedData loading cannot mint one. Pending records must retain their original matching generation authority, seed/dimension/chunk selection and UUID; current NaturalSoil and habitat validation still apply. Missing authority fails closed. Ordinary player replacement, including a same-state matching soil write, revokes NaturalSoil permission and remains protected.

Immediately before insertion, the record becomes `RESERVED` and SavedData is flushed. Verified success becomes `PLACED`; definite refusal with no actual actor becomes `PENDING` with the same UUID (or terminal rejection at the attempt cap). An uncertain result becomes terminal `INDETERMINATE`. A reservation surviving data loading is also `INDETERMINATE` and is never retried. This deliberately favors missing placement over duplicate actors if a process stops between files. It does not claim an atomic multi-file Minecraft entity/ledger transaction or abrupt-crash fault-injection evidence.

`PLACED` and rejected/indeterminate records never reenter the queue. Death, unloading or absent lookup cannot request replacement. Tests cover actual pending/placed SavedData disk loading, repeated FULL chunk deserialization, death, missing authority and reservation decoding. These are running-process storage/codec and chunk-loading checks, not cold process restart, arbitrary unloading or broad colony nutrition/layout persistence acceptance.

## Focused verification and pinned APIs

Six required named GameTests cover genuine automatic placement/ticks, biome/substrate/matching-player-soil rejection, fluids/occupied terrain/real actor occupancy/unknown footprint, refused insertion/disk reload/same identity, placed disk reload/repeated loading/death/missing authority/interrupted reservation, and default-disabled production settings. Positive generation uses a separate declared vanilla flat generator with bedrock 1 / dirt 6 / grass 1, genuine new region/chunks, and native tickets. No positive origin or queen is manufactured. Negative tests alter their own terrain or add stationary armor stands solely to reject unsafe candidates; they are not positive-placement or photographic evidence. Persistence/negative fixtures use independent development dimensions so they cannot rewind retained concurrent native-soil tests.

Compile and each exact focused diagnostic use separate evidence-helper invocations. The first invocation bound an argument array incorrectly (01, exit 1); 02 then found an invalid FabricGameTest interface assumption (exit 1), removed before 03 passed. The first positive server run (04) stayed pending because radius 0 means FULL level 33, not entity-ticking level 31. Reading pinned TicketStorage/ChunkLevel led to radius 2; 05 compiled and 06 passed. The later real-occupant setup check (21) queried entity identity before chunk accessibility; its archived failure was corrected by verifying every occupant at the loaded checkpoint, without changing production checks or deadlines. There were no unchanged full retries or native search retries.

Pinned source-reading evidence: `T14/source-reading.json`, `source-api-reading.txt` and `pinned-api-excerpts.txt`. Read exact 26.3 ChunkPyramid/ChunkStatusTasks, LevelChunk, ServerChunkCache, ChunkMap, ServerLevel, TicketStorage/ChunkLevel/TicketType, EntityType/insertion/persistence and GameTestServer; matching Fabric lifecycle and GameTest/Loom testing sources came from `C:/Users/user/.gradle/caches/prime-ants-source-inspection`. Gotchas: FULL conversion runs on `mainThreadExecutor`; Fabric chunk-load/status callbacks do not guarantee accessible entities or completed status futures; radius 2 is needed for center entity ticking; EntityType.spawn does not return insertion success. Toolchain remains Minecraft 26.3, Java 25, Gradle 9.7.1, Loader 0.19.5, API 0.161.0+26.3, Loom 1.18.2.

## Reproduction and archives

From the repository with helper Java `C:/Program Files/Eclipse Adoptium/jdk-25.0.3.9-hotspot`:

```powershell
& .\scripts\Invoke-GradleEvidence.ps1 -Name 'placement-compile-new-name' -GradleArgs @('gametestClasses','--console=plain') -EvidenceDirectory '<owned-evidence>'
python scripts/placement-evidence.py --evidence '<owned-evidence>' --name positive-new-name --focus genuine_generation_places_and_ticks_founding_queen
```

The second command expands to `runGameTest --console=plain -PprimeAntsServerDiagnosticFilter=prime_ants_test:natural_placement_game_test_genuine_generation_places_and_ticks_founding_queen`. Each other exact method is in `NaturalPlacementGameTest` and each executed argv/exit/elapsed is in T14's command metadata.

The exact native task pattern (run **each seed once**, with fresh owned paths) is:

```powershell
& .\scripts\Invoke-GradleEvidence.ps1 -Name 'native-seed-2026100501-new-name' -GradleArgs @('runPlacementDiagnostic','--console=plain','-PprimeAntsPlacementSeed=2026100501','-PprimeAntsPlacementAttempt=t14-seed-2026100501-new-name','-PprimeAntsPlacementEvidence=<owned-evidence>\seed-2026100501') -EvidenceDirectory '<owned-evidence>'
```

Use seeds 42 and 8675309 for the other two invocations. The committed `placement-evidence.py --seed <seed>` wrapper additionally writes exact argv, checks exit and immediately archives each world; its fixed attempt names refuse existing directories. Actual owned paths are `build/run/t14-placement/t14-seed-2026100501/world`, `t14-seed-42/world`, and `t14-seed-8675309/world`. Results/normal stop metadata are under T14 `seed-*/`. Runtime guards and a missing result fail the task. This task is not connected to check or any green-build capture/performance authorization.

| Archive in T14 | SHA-256 |
|---|---|
| 17-native-seed-2026100501-world.zip | 15135710a8bc540c7c900a01fe9cfef1cd7620a5d8c0298024c15a5a53358327 |
| 18-native-seed-42-world.zip | 51a97b944cdc8e8dad56996bd655871b9c3b669fe45d3ba75bdfa9e976ba43da |
| 19-native-seed-8675309-world.zip | fce108f5cd26d5f681cb939ff146f9464bb1f3e5f10767263b3d2a503e3e7212 |

The independent saved-region audit confirms 81 native / 82 all FULL per world, exact persisted decisions and no archived ant actors. Source partial-chunk counts remain available, rather than being hidden as FULL. Failed focused worlds are archived too. T13's nonexistent appendix filename was corrected to `t13-build-final-failed-world.zip`; its original report is preserved in T14 before that single authorized edit.

## Current build and next Phase 2 task

The one current unfiltered `t14-build-status` exited **0 in 175.518951 s**: **15 unit / 105 server / 24 model**, zero failures or skips. All 99 baseline names and six new required cases ran. Source/build-input snapshots stayed unchanged; executed XML and production binary/source jars are archived in `T14/t14-build-status-executed/`, and the full world in `t14-build-status-world.zip`. This is a single passing current run, not a traffic repair or robust playability acceptance. Production QueenFounding/WorkerTasks and their deadlines were unchanged; the previously evidenced throat lock remains unresolved.

Keep the prototype disabled. The next Phase 2 task should predeclare a bounded biome-aware candidate-selection/terrain-fit diagnostic that gives eligible generated terrain coverage while retaining spacing, provenance, collision and roof/floor protections. The present 12 lattice candidates tested no eligible native footprint. A further experiment needs a new explicit declaration; expanding these regions to chase a success is not warranted. The known traffic defect must remain an independent playability blocker.

Fresh readable original close-ups/forager views are still missing. MSPT is unmeasured. Broad nutrition/layout cold restart, arbitrary unload/offline behavior, natural food, adult mortality, flights/defense and later Phase 2 stages remain outstanding. No client launched: lifetime history stays **24 launches, native 16/8, tasks 11/13**; the three dedicated diagnostic servers and focused GameTest JVMs are counted separately in Report T14. There is no new toolchain/entity infeasibility or unexplained-red kill evidence, and no survival/release promotion.
