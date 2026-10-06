# T23 player checkpoint evidence appendix

Evidence root: `C:/Users/user/Documents/turnloop/directions/prime-ants-slice1/turns/T23`.
The summary-first current result is in [slice-1-report.md](slice-1-report.md).
T22's original current report is preserved verbatim in [t22-status-historical.md](t22-status-historical.md).

## Scope and predeclaration

Seed 2026100501, client-created vanilla overworld, default production placement and render/simulation 8/8.
Read-only source is T22 `t22-fresh-a4-world.zip`, root `New World/`, copied into the task-owned `saves/t23-player-a1`.
Route declared before launch: primary `9c71829b-25f1-3b96-acbb-96252228271d`, initial ground (168,67,149);
fallback `05888952-07db-3426-8bbc-6cf27cdcf840`, initial ground (168,71,86), only after recorded initial viability/loading failure.
These are sites, not safe destination heights. This is local known-site access, without a walk-from-spawn discovery claim.
No imported-server diagnostic dimension, placement call, supplied ants/brood, clock/reserve/stage edits, AI pause,
ant positioning, forced targets or player terrain clearing are authorized or introduced.

The exact source/route arguments are:

```text
runClientGameTest --console=plain
-PprimeAntsClientScenario=fresh-survival
-PprimeAntsCapturePrefix=t23-player-a1
-PprimeAntsPriorScenarioTicks=3002
-PprimeAntsBroodMultiplier=100
-PprimeAntsFoundingMultiplier=20
-PprimeAntsFreshWorldArchive=C:\Users\user\Documents\turnloop\directions\prime-ants-slice1\turns\T22\t22-fresh-a4-world.zip
-PprimeAntsKnownSiteRoute=t22-a4-primary-fallback
```

`client-a1.ps1` executes the current acceptance gate, the synchronous evidence helper with the explicit argument array,
immediate exit capture and `Archive-ClientAttempt.ps1` before another Gradle task.
A continuation must use its actual accumulated ticks and closed archive; `primeAntsFreshArchiveWorld` selects
one archive root when more than one client save is present. It cannot renew the original 24,000-tick allowance.

## Observer/result recovery

Matching cached Minecraft 26.3 sources and Fabric client GameTest 6.0.7 APIs were read before changes.
The source of minimum-Y failure is `Level.getHeight` on an unavailable column. `ObserverSafety` first requires a
non-generating actual FULL `LevelChunk`, then checks the standing body/support halo, build height, border,
solid dry support, empty feet/head fluid and whole-body collision. Ticks and liveness are checked around waits;
live survival water/out-of-bounds/unloaded exposure also rejects immediately. Setup alone may use spectator.

The isolated bootstrap recovery retains its original case/name and physical food lifecycle assertions,
adding dry acceptance and unloaded/void/wet/blocked-head rejection. `t23-observer-recovery` exits 0.
`verifyFreshObserverResultHandling` additionally rejects dead-observer/claimed-absence, bare absence,
discarded prior ticks, exceeded bounds and unreviewed images. Incomplete maturation is explicitly incomplete.
`verifyFreshPlayerCheckpoint` requires all measured stages and individually reviewed PNGs matching the run/files.
The scenario cannot set checkpoint success merely by completing loading, closure or interactions.

T22 A4's saved player has health 0, position (-15.5,56.76373079905836,-15.5), death time 797, survival and overworld.
The source has ordinary vanilla dimensions and seed 2026100501. `source-inspection.json` records the read-only NBT check.
The source failure is recoverable through the normal Respawn UI; no health edit is used.
T22's successful original task exit remains historical. Its radius-128 nonselection is an observer failure,
not reliable absence evidence; its four genuine automatically placed queens are preserved.

## Failed commands and focused diagnosis

The first guide-restoration command failed before writing because Windows PowerShell's piped literal lost its
Cyrillic search marker. `restore-guide.py` reads Git's UTF-8 bytes and writes explicit UTF-8; saved strict decoding
reports 1,878 Cyrillic characters (baseline 852), no replacement character and no question-mark replacement runs.
Two patch application errors changed no files; their target/context errors were corrected.
Unavailable `rg` was replaced by narrow PowerShell/Python reads. The optional `nbtlib` read failed because the
module was absent; a small turn-local read-only NBT reader then inspected the actual 26.3 `players/data` and
`data/minecraft/world_gen_settings.dat` paths. An initial UTF-8 log read was corrected to detect PowerShell UTF-16.

`t23-compile` exits 1: `JsonObject` has no `values()` API; use its existing `entrySet`.
`t23-compile-r1` exits 1: archive opening adds a checked exception, so catch/rethrow is wrapped.
`t23-compile-r2` exits 0, including the executable result recovery.

The first exact full `t23-build` exits 1 in 771.202 s: all 195 names execute, with one lifecycle timeout.
It also predates final client/result review, so cannot authorize the client as unchanged current acceptance.
Failure: `prime_ants_test:lifecycle_game_test_environmental_chicken_loot_funds_new_worker_then_queen_death_ends_colony`,
26,000-tick timeout. Two actual raw chicken drops expire at normal age 6,000; available protein ends at 8,000,
below the unchanged egg/larva commitment. Its log, failed XML and current closed server UUID ZIP are retained.

Original isolated `t23-lifecycle-reproducer` exits 0, proving the full-run resource ordering is variable.
Only the controlled fixture's six existing apples now wait for actual current environmental protein to cover
the egg/larva commitment. Three vanilla chickens, physical suffocation/loot, costs, lifespan/fasting, item expiry,
five-adult cap, 26,000 bound and conservation/birth/extinction assertions remain. The sugar-supply assertion is stronger.
`t23-lifecycle-recovery` exits 0: actual stock 16,000 protein, real additional worker and final extinction.
No production food/AI/life rule changes. This is a disclosed test-order correction, not native ecology evidence.

An optional server-fixture archive initially selected all retained UUID folders; its owned Python PID was stopped
and the partial file retained. The corrected `t23-build-current-server-world.zip` contains only the failed current
closed UUID. No original world or historical artifact was modified. Client attempts use the required existing archiver.

## Native attempts and current accepted result

| Attempt | Prior -> closed total ticks | Source/normal closure | Outcome |
|---|---|---|---|
| A1 | 3002 ->10084 | T22 A4, saved/closed/immediate archive | Normal Respawn; primary founded/opened/three workers; benign watch. Exact recorder1600x1000 guard rejected1280x720 frame; task1. |
| A2 | 10084 ->10320 | Closed A1 copy, saved/closed/immediate archive | Explicit `primeAntsFreshObserverRecoveryOnly=true`; native1600x1000 recovery, three real render records; task0, full checkpoint false. |
| A3 | 10320 ->10849 | Closed A2 copy, saved/closed/immediate archive | All measured actions completed; one required apple delivered, real bite20->19. Task1 awaiting image review; food PNG rejected because client equipment was air. |
| A4 | 10849 ->11051 | Closed A3 copy, saved/closed/immediate archive | Explicit `primeAntsFreshFoodSyncRecoveryOnly=true`, passive existing/native-food sync; no supply, real client nectar record. Task0, full checkpoint false; grass-occluded PNG rejected for food readability. |
| A5 | 11051 ->12029 | Closed A4 copy, saved/closed/immediate archive | Current-green full survival repeat with one optional raw chicken and prior mandatory A3 apple evidence. All interactions and four readable images. Raw task1 awaiting manual review; final checkpoint verification0. |

Five native launches all initialize and close normally; client history35 launches, startup26/9, raw tasks15/20.
One accepted full player checkpoint/four accepted checkpoint images are separate from raw process/task counts.
Ten T23 headless server launches are separate (43 with the brief's33). Natural total12029 includes original3002,
all recovery/load/wait/interaction/closure;11971 remain. No renewed allowance, seed campaign or fallback selection.

Source/root selectors and exact arrays are in T23/client-a1.ps1, client-a2-recovery.ps1, client-a3.ps1,
client-a4-sync-recovery.ps1 and client-a5.ps1. Final repeat adds:

```text
-PprimeAntsPlayerFood=raw-chicken
-PprimeAntsPriorAppleEvidence=C:\Users\user\Documents\prime-ants\docs\screenshots\t23-player-a3-fresh.json
```

The optional chicken cannot replace the required same-colony apple trace. No apple repeat or other player food was supplied.
A3 apple world3f9328c9-2dbc-470b-9e87-1dec82260bb1 -> forager9a5b931b-bb9b-332d-b42a-34a6202eccca -> owned cache,
inventory loss1/final0,4.115340m retreat. A5 chicken world947cdc0c-3104-4b01-bf72-12db6b591545 -> same forager -> owned cache,
inventory loss1/final0,4.115339m retreat. Intake-after counters are0 at each delivery checkpoint, so cache delivery is claimed,
not ingestion. Closed A3 NBT confirms exactly one apple in the owned cache and all actual first-clutch brood identities.
Player supplies do not prove autonomous intake.

The selected primary is alive20HP after loading, with genuine placement and current FULL terrain; original ground/actual entrance
(168,67,149), directionnorth. A1 actual progress3->24 removed,1->22 deposited, founding loaded488->6514; three real original
clutch workers emerge and physically open both plugs. Stage duration120, adult lifespan144000/fasting24000 remain persisted.
The existing chamber floor is native mineral, with no player digging/cutaway.

Observer modes/setup moves are in each fresh JSON: spectator site travel and dry stand setup; then verified survival physics
for watch/entry/drop/retreat/attack. Main survival stand(168,67,152), all body/support/FULL/border/fluid/liveness/tick checks true.
No measured teleport/noclip/flight. Daylight, hidden HUD and native1600x1000 are disclosed. Prior A3 real alarm persists; A5 waits318
normal loaded ticks until expiration, without clearing records or assigning targets.
A5 attack is ordinary keyAttack input. Biter ec9d7a07-ea92-396a-a736-38ed3d707167, source`mob`, actual health20->19 at tick941,
bite counter1, living player, ordinary retreat. A3 also has actual c6aabfad-4c5c-39e7-bb2d-96f01f1fa75d mob bite20->19.
No health, damage or invulnerability edits, combat death, fresh drowning/void or unsafe observer occurred in T23.

## Additional capture and acceptance recovery

A3's server carried-food observation preceded client equipment synchronization. Its feeding extraction contains only air;
the PNG was opened and rejected. Capture now waits for actual client equipment and asserts that the captured actor still holds
the declared food. A4 observes actual native nectar equipment without any supply; its grass-occluded food remains rejected.
The authorized optional one-chicken repeat A5 produces a clearly visible mandible-carried food PNG after sync.

A subsequent full `t23-build-equipment-final` exits1:195 names, one 22000-tick timeout in
`prime_ants_test:defense_game_test_provocation_cancels_real_partial_crop_sharing_without_transfer_cargo_or_claim_loss`.
Original isolated `t23-sharing-reproducer` exits0. Hypothesis: simultaneously rich supply can remove the hungry-worker
precondition before the partial action is observed. Only fixture ordering changes: initial 3 apples, remaining 5 apples/8 chicken
when an actual partial worker-to-worker action starts. Original total8/8,22000 bound, worker-recipient/cargo/claim/no-transfer
assertions, real attack, life and food costs remain; no forced fasting/target/AI/stage edits. `t23-sharing-recovery` exits0 and
records real actionTicks 3 before accepted damage cancels both endpoints. Failed XML/log/current closed UUID ZIP are preserved.

Current completed `t23-build-current-green`, exact `build --console=plain --rerun-tasks`, exits0:
15 unit/195 server/26 model, all required executed names, zero failures/errors/skips,448 unchanged source/build inputs,
and both production JAR helper/preset exclusions. It precedes A5 and remains current after A5/verification.
No production source/client mechanics, ecology, damage, lineage, costs or pinned toolchain changed.
No timeout/RAM increase, widened recorder guard, skipped test or softened assertion.

Every PNG (11 total) was opened in original resolution. Four A5 frames are individually accepted in
[image review](screenshots/t23-player-a5-image-review.json): explicitly visible entrance/worker/mound edges;
existing chamber/queen/mineral floor; actual chicken at mandibles; actual fight/red hit worker/owned biter.
Nursery is empty after first-clutch emergence; no visible-brood claim. A3 feeding and A4 food-sync remain rejected;
A1/A2 partials and all T20/T22 qualifications are retained. Fresh one-line caption files and screenshot index record these limits.

`verifyFreshPlayerCheckpoint --console=plain -PprimeAntsCapturePrefix=t23-player-a5` exits0 after manual image review
and mandatory prior-apple validation. Normal loading/closure, recovery-only, incomplete maturation and observer failure
cannot pass this full verifier. Raw A3/A5 task exits1 for unreviewed images remain unchanged historical exits.
The final proof is T23/client-outcome.json plus checkpoint-verify0, not a promotion of those raw exits.

Final guide saved explicitly UTF-8 and strictly decoded: baseline 852 Cyrillic, restored 1878, final 2168;
no replacement character or question-mark replacement runs. English/current behavior and Russian restored paragraphs remain;
watching/feeding benign, defense after harm, current real client limits/evidence, release/appearance scheduling are documented.

## Commands and artifacts

Complete exact commands/exits/elapsed/log paths: T23/commands-summary.json and each immutable helper metadata JSON.
All five closed client world ZIPs/archives are in T23; final build executed XML/JARs in t23-build-current-green-executed.
The current summary verifier is T23/final-verification.json; earlier accepted inputs retain their original archives.
Performance and broad save/reload remain T27-T30; T24-T26 remains appearance. Local known-site access is not spawn discovery.

| Evidence command name | Exit | Seconds |
|---|---:|---:|
| `t23-build-current-green` | 0 | 639.87 |
| `t23-build-equipment-final` | 1 | 792.77 |
| `t23-build-final` | 0 | 635.98 |
| `t23-build-frame-final` | 0 | 657.34 |
| `t23-build` | 1 | 771.20 |
| `t23-checkpoint-verify` | 0 | 0.95 |
| `t23-client-food-sync-recovery-a4` | 0 | 29.69 |
| `t23-client-frame-recovery-a2` | 0 | 33.98 |
| `t23-client-player-a1` | 1 | 548.46 |
| `t23-client-player-a3` | 1 | 45.51 |
| `t23-client-player-a5` | 1 | 67.77 |
| `t23-compile-r1` | 1 | 2.38 |
| `t23-compile-r2` | 0 | 2.50 |
| `t23-compile` | 1 | 3.83 |
| `t23-food-sync-compile` | 0 | 3.86 |
| `t23-frame-result-recovery` | 0 | 0.96 |
| `t23-lifecycle-recovery` | 0 | 19.82 |
| `t23-lifecycle-reproducer` | 0 | 21.04 |
| `t23-observer-recovery` | 0 | 13.59 |
| `t23-sharing-recovery` | 0 | 19.38 |
| `t23-sharing-reproducer` | 0 | 19.64 |

Final whitespace check caught duplicated Windows carriage returns in the guide after an explicit UTF-8 rewrite. Only guide line endings were normalized to explicit LF; strict UTF-8/Cyrillic2168/no-question-runs verification repeated and git diff --check passed. No gameplay/build input changed.
