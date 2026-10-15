# T11 validation and scale checkpoint (2026-10-15/16)

T10 remains a valid negative checkpoint: supplied 120/360-adult Mature cohorts had
live cap60, native mean38.447/115.134ms and p95 63.828/195.508ms. Each used600 warm-up
and2400 contiguous measured ticks at ordinary20TPS, work/brood1/1. One diagnostic
reopen had exact decoded equality but zero brood and no resumed physical service.
The crashed preparation world remains unchanged. Its watchdog/JFR localizes a
repeated validation path; it does not prove one intrinsically sixty-second call.

The prospective contract is external `raw/t11-repair-plan.json`, followed by the
measured selection `raw/t11-repair-selection.json` in the T11 turn directory.
A genuinely generated five-adult Young colony ran50 loaded warm-up ticks and200
real diagnostic ticks. It made6691 habitat queries,6491 shell requests,1717569
availability calls and697566 terrain-match calls, allocating329464856 bytes on
the server thread. This diagnostic is separate from budget acceptance.

The repair reuses immutable nest/job geometry and completed-cell membership,
hoists generated marker coordinates, and gives each `ColonyTerrain` instance a
derived long-position/owner/kind lookup. It checks that lookup before reading
world availability and block state. A positive match also rechecks the current
canonical terrain instance after those reads, including the access-mound prefix
branch. Canonical serialized strings remain unchanged.
Constructor decoding reconstructs the index; `record` updates both representations;
`invalidate` removes both, including same-state writes. The only completion append,
`DigJob.removed`, updates completion membership. Job decoding reconstructs it.
No derived index is serialized. Unreachable noncanonical legacy keys stay serialized
without acquiring authority.

No live findings, chunks, block states, markers, registry results, claims or
authorization are cached. Each query builds fresh caller-owned `Findings` and
reads the current SavedData instance and physical world. The geometry cache holds
at most256 immutable plan shapes, keyed by the complete immutable plan. It stores
coordinates only; sharing equivalent coordinates across dimensions grants no
ownership. Different task sets and retained raised exterior routes stay distinct.
Every loaded damage check still runs beside unavailable cells, retaining verdict
and first-label precedence.

The identical small repeat made the same6691 habitat queries. Allocations fell to
186049432 bytes (43.5% less); diagnostic native mean fell3.151→2.715ms. Availability
calls fell to1207522 because absent/foreign terrain authority no longer causes a
world lookup. Both processes closed normally with unchanged frozen inputs.
This supports the initial causal prediction, not a scale-budget verdict or final
candidate acceptance. A later adversarial real-delivery diagnostic exposed a
stale positive when a same-state write occurred inside the indexed lookup's world
read. The failed predicate and exact executable inputs are retained. The corrected
predicate checks current canonical ownership after world reads; its regression
requires that the real forager retain the exact named clay cargo and its live claim.
The first diagnostic hook fired on an earlier outer availability read and therefore
did not exercise this ordering; that limitation is preserved separately.

The final corrected C7 diagnostic repeats the original 6,691 habitat queries and
697,566 terrain matches, with 1,207,522 availability calls. Server-thread allocation
is 186,140,840 bytes versus the original 329,464,856 (43.5% less); mean native time
is 2.577 versus 3.151 ms. Both processes close normally with unchanged frozen inputs.
The allocation/query prediction passes; the short timing remains descriptive and
does not replace the frozen 600/2,400-tick budgets.

The initial complete C4 long suite passes 224/225, with an existing lifecycle
timeout at 26,000 ticks. Its isolated read-only diagnostic passes without changing
that body's assertions, supply, lifespan or bound; the original failure is retained
without a causal explanation. The C7 combined one-server target passes 5/6,
including the boundary refusal and actual cargo-reload/missing-supply overlap.
The owner miner fixture times out at 60,000. Its last regular trace at 59,000 has
28 adults but remains Young, with food 0/4 and clay 8/16 for Mature. No first
UNKNOWN-intervention marker appears, so it does not reach its cargo/damage
assertions in that run. The completed C7 full-suite miner shard subsequently passes
73/73, including the unchanged owner case at tick 46,274 with every 600/200 window.
Neither earlier preparation negative is erased by these scoped positives.

The C7 complete suite passes 225/226 in 6,099.572 seconds, Gradle/root exit 1,
native exits [0,1,0]. Its sole failure exposes the existing 30,000-tick forager-scaling
simultaneous-cargo witness as a red gate. Its retained trace restores two actual
apple carriers at 19,491, drops the finite eight clay at 22,659, and shows all eight
stored by 29,000. No two-claimed-clay witness marker appears. Exact apple cargo
reconstruction and eight sequential deliveries cannot substitute for that witness.
No source, bound, route, supply or assertion in that body was changed to rescue it.
The cause remains unresolved; the optimization is not credited with complete
validation acceptance. Final fast acceptance passes 74 server, 118 unit and 26
model checks, exit 0. Native and merged XML, exact assignments and executable
archives remain in the external settling packet. Downstream cap timing and persistence stay
gated while this correctness result is red.

The isolated `control` phase uses a genuine Young colony at defaults and a fresh
9000-loaded-tick bound. Actual initial fasting clocks are zero despite worker ages
4800..5100. Sharing eligibility therefore starts around6000 active ticks, and
personal hunger around12000. The2400-tick income window requires4500 sugar for
five adults plus the prospective sixth; at least two4000-sugar apples must actually
be eaten in that window. Funding additionally requires9500 sugar and10000 protein
still present. One finite physical wave of eight apples/four chickens at loaded
tick5800 precedes sharing eligibility and retains normal item aging. No hunger,
nutrition, income receipt or progress is assigned. Actual natural laying followed
by200 consecutive observed progress ticks decides the prerequisite; failure stops
large persistence preparation.

Scale declarations may select a controlled initial physical Great habitat. Its
existing loaded chamber walls become supplied owned packed clay, and its physical
store receives32 supplied cobblestone units during initial setup. No stage, cap,
job payment or biological progress is assigned. Production evaluation must derive
Great/cap120 from these chambers, stocks and120 actual living adults; every measured
row requires that live stage/cap. This is controlled initial state, not observed
construction, growth or sustainability. Fresh empty one/three-region controls use
the populated scenarios' exact ticket centers in an equivalent flat dimension;
dimension/context differences remain explicit, and no subtraction decides budgets.

The control and Great clauses above are prospective setup declarations in this
checkpoint. The complete correctness red gates their launch. No T11 120/360-adult
budget, default-speed brood-preparation result or accepted persistence reopen is
claimed. The legacy preparation material-cargo flag remains descriptive rather
than required; that phase was neither launched nor accepted. Strengthened live
food/material task gates and resumed service after two real process reopens remain
outstanding.

The settling result, complete regression coverage, retained negatives and any
unmet persistence gates are recorded in external
`raw/t11-scale-persistence-acceptance.json`. Item4 remains open until both its
performance and nonempty two-reopen obligations pass.
