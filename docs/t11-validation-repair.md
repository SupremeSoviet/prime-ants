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

## T12 bounded F2 diagnosis (2026-10-16)

T12 starts from clean `master` at `18a875d`. The prospective diagnostic contract
is external `raw/t12-f2-plan.json`; its settling packet is
`raw/t12-repair-acceptance.json` in turn T12. No production or fixture correction
was demonstrated. Complete repair acceptance remains withheld.

Two separately frozen, single-server observations retain F2's original fixture,
finite eight-clay supply, food priority, caregiver protection, exact queen/carrier
reload assertions and 30,000-tick deadline. Passive test-only hooks observe real
controller calls/returns, paths, admission, meals/sharing and mainhand writes.
The first execution picks up clay at 22,767 and 22,887 and witnesses both claimed
carriers at 22,888. The second picks up at 22,783 and 22,858 and witnesses both at
22,859. Both native/Gradle/root exits are zero. These are unrepaired passes,
not causal recovery or complete acceptance.

At clay-window entry the first pair has 9,216 versus zero native-food inspections;
its material-search calls are 120 ticks apart. The second pair has 6,144 versus
zero inspections, with material-search calls 80 ticks apart and pickups 75 ticks
apart. Both observed paths reach the source; actual authorization and material
admission succeed, dropped-food preemption returns no source, and actual meal and
sharing returns are false during these finite windows. This distinguishes phase
names from search readiness in these passing executions. It does not establish
the first cause of T11's missing overlap or attribute it to production or fixture.
Fresh identities differ; no matched pre-T11 comparison or exact failing checkpoint
is claimed. Original success ends both traces before clay deposit, so both carry
interval endpoints are right-censored. All original per-tick conservation checks
pass through success; later eight-unit settlement is not observed in these runs.

The observer and actual compiled inputs are retained in external executable ZIPs.
After both children and wrappers exit, the temporary hooks are removed and the
baseline executable sources restored. No unsupported fixture adjustment, policy
concession or optimization withdrawal is committed. The canonical ownership
recheck and its adversarial test remain unchanged. Additional safety targets,
the cargo-reconstruction pair and complete fast/long acceptance are not launched:
the required demonstrated targeted correction was not obtained. Previous T11
safety positives remain historical coverage only.

T11's lifecycle 26,000-tick timeout and miner-preparation 60,000-tick timeout remain
separate unexplained negatives; later isolated or full-suite passes do not explain
them. T10 cap-budget failures remain in force. The five-adult 43.5% allocation
reduction is not cap acceptance. Cap timing, default-speed brood preparation,
enforced material-cargo preparation and both persistence reopens remain gated.
Item 3 stays closed; item 4 and 0.2.0 remain open. No-new-green advances from 1 to 2;
the direction stops for the owner's decision on any further bounded diagnosis.

## T13 owner-exception complete attempts (2026-10-16)

The owner's dated one-turn exception overrides the T12 causal-F2 prerequisite.
T13 starts from clean `master` at `bd191a0`; T12's only change from `18a875d` is
documentation. The production optimization remains unchanged throughout T13.
The prospective contract and settling packet are external
`raw/t13-plan.json` and `raw/t13-optimization-acceptance.json` in turn T13.

Passive development-only F2 observers are retained with the tested tree. They
record actual controller/path/movement/admission/meal/sharing/hand returns and
state, without additional decisions, paths, mutating gate calls or actor control.
The fixture has only passive attach/begin additions; its original route, supply,
reload/claim/cargo assertions, same-tick two-carrier witness, eight-clay accounting
and 30,000-tick bound remain intact. Actual seed/dimension/location/rotation and
identities are recorded; internal RNG and UUID-generation inputs remain UNKNOWN.
No deterministic replay is claimed.

Initial fast acceptance passes 74 server, 118 unit and 26 model cases, exit 0,
in 162.716 seconds. The first complete unfiltered long run passes 225/226 in
5,934.902 seconds, native exits [0,0,1], Gradle/root 1. F2 passes at helper 22,612.
The sole failure is the separate mining mid-haul reload/death case at 60,000.
Its last regular trace at 59,000 is Young, 28 adults, food 0/4 and clay 9/16;
no protected reload/death/recovery milestone is reached. Earlier traces retain
a prolonged 17-adult plateau with inadequate recent receipts despite stock.
Exact state in the final unsampled interval is UNKNOWN.

Only that failed mining case now selects the existing finite natural physical
feeding profile. This changes its preparation inputs, retaining ordinary food
priority and physical hauling. Production, the shared helper, every assertion,
geology, cargo/claim obligation and 60,000 total / 6,000 nursery-loaded recovery /
600 custody / 200 settled limits remain unchanged. No nutrition, adults, stage,
roles, cargo or biological progress is assigned.

The corrected case and the cargo-reload/missing-supply pair pass together on one
server: 3/3, native/Gradle/root 0, in 601.718 seconds. Two actual active-job
reconstruction overlaps retain exact all-colony data and nonempty claimed-actor
equipment/task state. Mining success occurs at helper 46,871. Its real named-unit
pickup/deposit are 44,993/45,144, before the gallery milestone 46,671; the reported
zero forward recovery counter does not mean instant transport. The revised full
suite also passes this corrected case at helper 46,057.

Revised fast acceptance passes 74/118/26, exit 0, in 131.368 seconds. Revised
complete long acceptance passes 224/226 in 5,420.671 seconds, native [0,1,1],
Gradle/root 1. F2 passes again at helper 22,575. Its historical failure remains
unrepaired with cause UNKNOWN; those passes do not establish attribution.
No isolated F2 retry or causal repair is claimed. Complete green has not been
earned, so the optimization's complete correctness checkpoint is unaccepted.

The revised failures are the unchanged MatureColony clay-ledger/builder-death
case and the supplied-founding-to-Mature surface case, both at 60,000 ticks.
The first has no builder-death marker. Read-only saved shutdown state is Young,
with no upgrade job and 19 nearby same-queen workers. Its availability and
route-stall reasons do not establish the first cause or exact deadline state.
The surface case's existing passive helper-59,999 snapshot is Young, adults
23/25, food 6, clay 16 and all four base functions confirmed, before any Mature
surface construction. Both bodies and their assertions/bounds are unchanged.
No speculative broader fixture rewrite or unchanged third full replay is made.

Current-canonical read-boundary validation and its adversarial regression remain
unchanged; no live authority cache or save change is introduced. T11's lifecycle
26,000 timeout and owner loaded-damage miner 60,000 preparation timeout remain
separate unexplained negatives. The later 46,274 pass and fresh scoped/full-suite
positives prove their executions only. The supported small five-adult 43.5%
allocation reduction does not accept cap timing. T10's timing negatives remain.

Both complete inventories are exact once: 300 server = 74 fast + 226 long,
118 unit / 26 model. All negatives, unit/model XML, native/merged reports,
source/class inputs, dependency hashes, assignments, actual exits and shutdowns
are preserved externally before subsequent Gradle invocations. No frozen inputs
are changed while servers run. The retained source corresponds to the tested
corrected tree, which remains a red complete checkpoint.

No-new-green advances 2 to 3; the one-turn exception ends with owner stop.
True cap120/multiple-colony timing, the isolated default-speed brood prerequisite,
enforced food/material cargo preparation, two real process reopens and 0.2.0 stay
outstanding. Item 3 remains closed; item 4 remains open. A further bounded
investigation of the new failure sequences requires the next owner decision.
