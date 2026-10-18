# T02 queen recipient predicate experiment

**WITHDRAW. Production matches starting master `f2ace51d5eebbfc55a1298569d81236c69c1fa3d`.**
The single experiment moved `queenNeedsFood(q,s)` before `q.founding().ready()` in
`WorkerTasks.selectNursingRecipient`. Larva-first selection, null/alive/noAI checks,
eligibility callback order, public authorization and actual feeding gates were unchanged.

The skipped readiness chain is not free of required effects. `nurseryFindings` can read
a pending block entity through vanilla `LevelChunk` promotion. For a legacy nursery
without `LastSimulationGameTime`, `BroodPile.setLevel` initializes that saved observation
boundary and marks it changed. A controlled reconstruction fixture with a food-refusing
queen and null pile argument exposes the difference: the candidate leaves the nursery
pending (`actualClock=-1`); explicitly invoking readiness then promotes it and saves
the current clock. Restoring the original predicate order passes the identical final
regression, with promotion and the exact current saved clock already present.
Ordinary chunk postprocessing usually promotes pending NBT first; this test establishes
the public selector boundary difference, without claiming its natural-play frequency.

Fresh unfiltered candidate acceptance executed all 77 fast, 124 unit, 26 model and
310 long cases: the original 309 long plus one selector regression, all passing,
with wrapper/Gradle/native exits zero. The regression was then strengthened to cover
the missing lazy boundary. Its candidate diagnostic fails the required-initialization
assertion; the restored-production diagnostic passes the same body. These probes differ
only in the declared source and compiled `WorkerTasks` class; dependencies and regression
code match. Fresh final restored fast acceptance passes 77 server, 124 unit and 26 model
cases with wrapper/Gradle/native exits zero, in 125.945 seconds.
No final complete-suite pass is inferred from the isolated diagnostic, and T17's old
lifecycle assertion remains unexplained.

No T02 timing or candidate JFR was launched: `NOT_MEASURED_CORRECTNESS_BLOCKER`.
No speedup, budget pass or candidate cost ranking is claimed. T01 remains historical,
not a matched control. The copied declarations retain 600 warm-up/2,400 measured ticks,
ordinary 20 TPS, work/brood 1/1, 3 GiB heap and the original readiness/watchdog gates.
Whole-server mean <=40 ms and p95 <=50 ms remain required independently for one and
three colonies. Diagnostics, contextual controls and historical profiles cannot settle them.

The original T01 manifest is unchanged. T02 independently reproduced the one grown
executor stdout entry with a matching recorded prefix and verified the other 335 entries.
Its corrected T01 seal names T01 as the artifact root and excludes live orchestration
streams. T02 uses the same closed-evidence sealing method.

The full audit, native/merged XML, commands/exits, preserved failed probes, report ZIPs,
executable archives, restoration hashes and corrected seals are external under
`<turnloop>/directions/prime-ants-perf/turns/T02/`, indexed by
`raw/t02-recipient-predicate-acceptance.json` and `report.md`.
Only the regression, additive inventory/native-context registration, discovery count
and this document are retained. Stage-1 decisions and all production/save formats remain
unchanged. T01 evaluated zero candidates; T02 attempted one and measured zero. Missing
measurements are not improvement or a measured nonimprovement turn.
