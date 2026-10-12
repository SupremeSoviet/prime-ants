# Catch-up T06: lifecycle, recovery and acceptance limits

The interrupted T06 changes were preserved and resumed on `catchup` after merge `33b76c1`.
Automatic adult/nursery reload hooks remain enabled. Aggregate development includes real costed eggs,
deterministic brood/worker identities, virtual worker birth times, stage promotion and the existing
24,000-tick shortfall grace. Digging, building and mining are excluded.

Ordinary brood preserves a successful feeding substep's entire tick, including speed/credit rounding,
and keeps inherited cocoon waiting. Recovery records unreconstructable pre-baseline time separately
as `UnknownTicks`, `UnknownThrough` and `RecoveryBaseline`; it neither charges that time nor calls it
simulation. Missing/conflicting original members and UNKNOWN terrain cannot authorize a migration.
Retained ordinary adult ranges stay excluded after bounded history compaction and reconstruction.

Aggregate laying uses `FoodLimitedGrowth.allowsAggregate`: finite confirmed nutrition/cache stock must
cover existing brood, one egg and larva investment, and the existing 25% maintenance reserve. It
creates no income observations or forage supply and leaves the ordinary rolling-income gate intact.
Funding availability belongs to the next virtual tick: ingested sugar or existing maintenance coverage
resets fasting then. Admission checks care, original-clutch retirement, slots, cap, cadence, event
identity and replay before any queen spending or food transfer. The coordinator reserves the whole
physical food batch plus one lifecycle event before mutation, within its unchanged 512-event ceiling.

An invocation caches its synchronous terrain/material observation. Population revision, queen death,
actual food changes and requirement clocks refresh the rule inputs. Newly observed reload shortfalls
have no evidence that they existed at the interval's beginning: new clocks wait until its end.
Persisted clocks and resource/population changes at simulated events retain their own times. UNKNOWN
uses the ordinary evaluator, so only requirements whose certainty breaks lose their clocks.

Permanent, source-independent invulnerability folds repeated no-op damage analytically. Ordinary
damage and temporary cooldowns keep their existing path; a later unprotected absence applies real
damage. A million-tick protected absence must finish in one invocation without changing the ceiling.

`CatchupLifecycleGameTest` supplies labelled accepted-history/timestamp controls, separately from
`ReconciliationChunkGameTest`'s genuine native eviction and automatic reload application. The finite
competition control has five adults, a paid/funded existing dependent, four apples and four chickens
in a confirmed tier-two store, and 32 real stored stone units. Both controls mature through 80
ordinary pre-boundary ticks with empty caches, then receive identical finite baseline stocks.
The accepted rolling receipt history is entirely before B and backed by initial ingestion; absence
adds no observations. Nursery duration is 1,200 steps; absence is 1,850 ticks. Bounds stay one adult,
one occupied brood slot, one cached unit per represented kind, and 120 development steps (10% of one
stage). Food/cargo/material identities, per-owner nutrition equations and save/replay are exact.

The stricter comparison exposed a timing discrepancy despite matched counts and exact conservation:
aggregate allocation omits physical nursing travel/service and allocates competing meals differently.
The case stays red when it exceeds the frozen timing bound. An earlier provisional pass skipped
comparison when stages differed and is not timing acceptance. No tolerance or guard was widened.
T07 must address finite nursing service/allocation or retain this explicit unsupported scope.

The hysteresis control compares a saved Mature food clock before/after 24,000 ticks, within the
existing 100-tick evaluator interval, then restores food and interrupts certainty with UNKNOWN.
Its fixed 25-adult bound isolates clocks from new laying. Original `TierThreeFixture` callers keep
their 49 workers, 54 declared apples, five chickens, terrain and original assertions.

Cost samples include eight prospective invocations each for four and 120 registered adults, plus
every selected native invocation, including deferrals. Index zero is the fixture's first invocation
("cold"), and indices one through seven are subsequent invocations ("warm"); these labels do not
claim an unwarmed JVM or control Windows scheduling. Inclusive hook timing excludes load I/O and
Gradle duration. Nested stage/commit cost is subtracted from lifecycle. Empty-nursery profile samples
check their phase sum against the inclusive timer. Capture can include a nested display commit when
initial stale brood/member reconciliation retires a record; that path's capture and commit fields
must not be summed as exclusive costs. Early returns also leave an unattributed residual. The
inclusive invocation timer is authoritative; these phase attribution limits remain explicit.
The p95 target remains 20 ms. Slow samples remain in cold, warm and pooled results. If unmet, T07
should reuse the already validated synchronous habitat/caregiver observation across adult phase and
allocation decisions to remove repeated per-actor terrain scans, then rerun the same prospective set.

The T06 settling packet and original reports live under the external turn artifacts:
`turns/T06/raw/catchup-checkpoint.json` and `turns/T06/report.md`. The packet records final complete
checkpoint union, all native/launcher exits and durations, guards, frozen inputs, every red/recovery,
historical evidence relocation/hashes, cost samples and the final commit/tree. Historical scoped
proof alone does not accept final executable inputs.
