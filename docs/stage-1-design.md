# Stage 1 current status (2026-10-11, T20)

**Verified partial handoff; Great remains infrastructure-valid/gameplay-red, cause unresolved; 0.2.0 is not accepted.**
The twenty-turn direction closes as partial. See the [public report](stage-1-report.md) and
[evidence/limits appendix](stage-1-report-appendix.md). The sections below retain earlier implementation notes as history;
their proposed next-turn work and historical inventory counts are not current acceptance claims.

T18 remains the complete valid-red checkpoint: 267 server identities = 73/73 fast + 193/194 long, plus 115 unit and
26 model cases; Great alone times out at 12,000 ticks. T19's separate bounded diagnosis has receipt prefix 43/57 and
physical form 47/57, six post-step/four upper watch-post cells absent, 79 soil blocks + one canonical cargo = 80 and
stone 32. Complete form, walking passage and 200 settled loaded ticks remain unmet. T19 fast passes 73/115/26 and
26 Python harness tests. T20's sole fresh fast acceptance/packaging run passes 73 fast, 115 unit, 26 model and 26 Python
harness tests in 120.243 wall seconds; all required guards execute, both client tasks actually guard-SKIP, 373 protected
files match and both jars match T19. Its settling
external evidence is `<turnloop>/directions/prime-ants-stage1/turns/T20/raw/stage-1-handoff.json`.

**T20 clock correction:** first certain Great is **helper tick 100, nursery-loaded tick 101**, not a shared 100 clock.
Mature completion is helper 4,256; Great admission is helper 4,301. The endpoint is the native failure-listener
observation at 12,002 after the unchanged 12,000 timeout. Automatic observations at 12,000/12,001 are excluded from
the bounded controller total 9,250; no later material/receipt transition or executor continuation occurs. Movement
requests, supported stands and path metadata establish no navigation cause. T19's archived appendix is unchanged.

Only documentation changes in T20: all production, client, tests, fixtures, observers, scripts, configuration and
assets remain unchanged, so no long body is affected and no Great/complete-long/client campaign is repeated. Current
jars retain version 0.1.0 as unreleased master artifacts; packaging is not release acceptance. Tier three, established
Young/Mature generation, 120-adult/multiple-colony performance, broad persistence and fresh Stage-1 client/screenshots
remain unmet. Preserve earlier founder/mining negatives and the exact two-relocation/nursing/recovery limits recorded
in the appendix. Recommend bounded Stage-1 completion before forging, armor or diplomacy, starting with Great's
unresolved deadline failure; no particular repair or new turn in this exhausted direction is promised.

---

# Stage 1 design (T01–T09)

Stages describe live bodies, owned chambers and physical stock. `ChamberRegistry` persists claims, stage and unmet-since
clocks; `ColonyDevelopment` confirms them from loaded terrain and observed adults. Loaded damage wins over unknown cells.

| Stage | Cumulative requirements | Adult cap |
|---|---|---|
| Founding | founding chamber | 5 |
| Young | 5 adults (queen + 4 workers), nursery, food store | 30 |
| Mature | 25 adults, hall, material store, 4 food, 16 clay | 60 |
| Great | 50 adults, four functions at tier 2, 32 stone | 120 |

Promotion is immediate. Ordinary adult/stock/tier shortfalls need **24,000 consecutive nursery loaded ticks** on separate,
persisted requirement clocks. Unknowns start no clock. Observed queen death independently forces Founding/cap five at the
next production evaluation, clears clocks and prevents promotion, even with surviving workers and intact chambers.
Observed function loss is immediate. Catastrophic population loss drops to the stage remaining adults support; caps never
remove adults. Unavailable entrance/plug remains unknown while the living queen is observed; loaded breach/obstruction wins.

`NestBlueprint` supplies bounded dig tasks for widening, material store and the hall beside the queen's existing chamber.
One real builder, with two caregivers kept, transports each excavated soil unit to `NestMound`: Young's two lobes have
214 cells; Mature's enclosing plan has 798. Supported witnessed soil/stone, air and short native plants are allowed;
player cells are preserved. Plug soil uses Young's plan when founding deposits are full.

`ChamberUpgrade` upgrades founding, material store, then hall. Founding is one physical job for nursery and food store.
Each wall conversion uses one clay physically fetched from a confirmed store; shared walls count once. Wall tiers are
read live: solid 1, colony packed clay/resin 2, colony nest-cut stone 3. Player walls confer no upgraded tier. The exact job
ledger is taken = carried + built + released; cumulative releases and live pending custody stay separate and persist.
Tier-three construction remains deferred until after mining.

| Function | Tier 1 | Tier 2 |
|---|---|---|
| Nursery | 3 slots + 4 for confirmed hall, ×1 | +3 slots, ×1.5 development |
| Food store | 6 units; 2 reserved per kind | 12 units; 4 reserved per kind |
| Material store | 32 units; 16 reserved for clay | 64 units; 32 reserved for clay |
| Hall | 1,200-tick base laying cadence | 600-tick base cadence |

The existing time multiplier scales hall cadence. Live confirmed hall tier owns its effect; egg costs, food gates,
nutrition, adult caps and brood slots stay unchanged. Foragers scale one per ten living workers (at least one), retain
caregivers/builder space and saved claims, and carry one physical unit per trip, food first. Canonical inventories survive
unknown terrain and capacity loss; admission stops rather than truncating contents, and food-share surplus uses custody.
Food display projects exact total plus six kind samples (**832 states**); material heaps and four kind samples use
**2,448 states**, separating kind sets and totals differing by eight units. Old inventory formats keep old validation.

Server GameTests create fresh UUID directories and retain the newest ten direct runs, current included. Escaping paths,
reparse points and locked/active deletions are rejected; non-runs, client worlds and evidence stay untouched. The legacy
client-directory delete action is skipped in the server graph. Logs/XML/retention are archived before another run.

T08 separates deterministic five-record serialization from natural expanded-brood/reload evidence. The long acceptance
retains Mature at every evaluation, faster hall laying, real expanded stock and a 600-loaded-tick unknown interval plus
two restored evaluations; bounded concurrency and final-build results are in T08's report. The frozen survey retains
1,846 sites / 1,842 openings / 764 store-and-hall fits (763 with support); walking routes and dig timelines are excluded.
**Unattended pre-Mature clay remains missing:** Mature-gated mining cannot supply that earlier gap. T09 prioritizes mining
and player contributions, with any diagnosed reliability carryovers; tier three and Great follow mining.

T09 adds a conservative physical gallery beyond the store passage: forward 8..15, side 0, two high at entrance depths
1..2. Runtime guards use a **16-block horizontal circle, depth 1..6, and 64 newly excavated mining cells per colony**,
including connector soil; the first fixed route actually contains at most sixteen cells. Planning reads those sixteen
cells on a 100-tick colony cadence. Unknown terrain pauses; player writes, including same-state writes, revoke origin.
`NaturalMaterials` records only witnessed genuine new generation separately from unchanged founding/chamber soil authority.
Standard stone/clay/gravel/sand/coal ore/copper ore/iron ore yield exactly one cobblestone/clay ball/gravel/sand/coal/raw
copper/raw iron, without vanilla loot or processing. Each claimed worker acts at an exposed supported face for twenty
loaded ticks, carries one unit, walks to the confirmed store and deposits. Connector soil uses the existing mound path.

Mining shares one builder, leaves two caregivers, and yields empty to available upgrades. Existing unfinished dig/upgrade
jobs can hold this first planner, including upgrades waiting for supplied clay. A miner yielding after a surface deposit
returns through the founding stair before fetching upgrade clay. All commit guards and eventual material room are checked
again before removal; changed availability after pickup retains the cargo. Falling roofs and survival/support breaches
are refused. Plans, successful edits, worker claims, per-item delivery history and named death custody transfers persist;
history is separate from current stock. Only declared, actually worker-opened gallery cells authorize chamber connections.

The permanent tests retain 60,000 total ticks, 600 loaded negative windows, two restored confirmations and 200 settled
ticks. New mining fixtures open with three apples/two chickens, then use the existing counted finite food/clay waves;
legacy supplies retain the accepted recipe. The attempted 8,000-sugar late hall-wave trigger was abandoned and restored
to 24,000 before final recovery and acceptance. T09's report separates passing supplied-gallery/contribution/origin evidence from
unresolved interruptions and any red full build. Mature-gated mining does not resolve unattended pre-Mature clay supply.
Placement-survey walking routes and dig timelines remain uncovered; supplied passes do not establish unattended
development or arbitrary deposit reliability. Tier-three construction and Great growth acceptance remain deferred.

The owner's 2026-10-09 15:25 decision allows real living members retaining valid nursing roles to count toward two
caregivers only while connected habitat has `Findings.Verdict.UNKNOWN`. Their work authorization still pauses. The fixture
checks lineage, home, nursing plan, actual loaded/ticking body, enabled mature state, queen occupancy and the owned nursery,
excluding every builder and forager claim. Loaded damage retains precedence and the ordinary authorized-caregiver predicate.
Fresh targeted recovery completed both 600-loaded-tick target/store unknown windows and two restored evaluations after
each, then failed at 42,849: a loaded foreign store revokes an actual chamber opening, making habitat `DAMAGED` and
pausing nursing while mining cargo retains its builder claim. The unknown exception was not extended. The foreign-store
window, same-state replacement and settlement remain unverified; T09 stopped at the prescribed observed-damage boundary.
Food component/identity and full-store preparation observers remain for subsequent diagnosis; no accounting, supply,
production or material-placement repair is claimed. The shape guard retains loaded-breach precedence.
Historical restored-recipe isolated galleries pass at 46,049 / 46,371 / 48,638, tier two at 51,452,
and reload/death at 46,790. The last inherited unfiltered run passes gallery at 47,346, tier two at 51,127, reload/death at
46,003, full-store at 56,913 and contributions at 22,143. Its only server failure is the active-caregiver assertion at
43,175 (zero active, 27 saved nursing roles); its model XML is stale. These supplied results are separate from the retry's
fresh acceptance in `<turnloop>/directions/prime-ants-stage1/turns/T09/report.md`.

Client capture JSON uses repository-relative paths, with an explicit repository root for serialization and absolute
runtime locations for the client working directory. Existing image/model/provenance validators remain enforced. A build
guard scans all tracked text, including newly indexed files, for profile absolute paths; diagnostics show filenames and
lines only. Binary assets are preserved. Fresh model verification precedes server acceptance even when that server run fails.

T10 recovery applies the owner's 2026-10-09 damage-pause decision only to the foreign-store interruption's existing
productive miner. A phase-specific snapshot requires its same living body, claim and exact cargo, observed loaded
DAMAGED habitat, unauthorized store and nursing, unchanged removal/delivery/release history, no nurse diversion,
one-builder exclusivity and every physical ledger. Shared UNKNOWN nurse qualifications and damage precedence remain;
ordinary authorized caregivers apply again on restoration. Production is unchanged. The body also waits for restored
Mature admission and an actual resumed action before replacement, and observes the full window after claim release.
Fresh completion is 44,755 ticks, including four 600-loaded-tick negatives, restored evaluations and 200 settled ticks.

The full-store stone contribution uses a checked supported approach-lane cell at forward -1, retaining the same 32
units, nursery-tier timing and all real one-unit trips. Instrumented original preparation identified delayed pickup at
45,644 after all upgrades; corrected original/copies finish at 56,141 / 54,110 / 55,527, preserving ownership, available
worker checks, 600 ready backpressure ticks, physical release, restored evaluations, observed cargo/delivery and 200
settled ticks. The 10-apple/8-chicken campaign passes all three identical bodies with unchanged diagnostics and 24,000
bound; the historical cause remains unknown. Additional mining diagnostics expose untouched regression/death assertions
and late gravity preparation. Fresh dependency and final-build results are in `<turnloop>/directions/prime-ants-stage1/turns/T10/report.md`.
Surface construction remains deferred. Unattended pre-Mature clay, arbitrary veins, all sixty-four edits, placement-survey
walking routes/dig timelines and unattended development remain outside the demonstrated coverage.

T11 separates unsafe mining refusal from assignment. A test-only observer forwards the real production admission
guard's identical result and records eligibility; production is unchanged. Assigned gravity preparation keeps only a
remote unopened gallery cell unavailable until three real upgrades confirm all four tier-two functions. Existing habitat
stays clear, origins survive and no mining work is manufactured. Ordinary admission then assigns the miner for both real
connector removals/deliveries. A separate permanent first-face case installs its declared closed stone/sand geology once
after upgrades and before any mining plan, requiring actual production refusal before assignment. Both bodies and two
identical copies each complete their original 600-negative/200-settled loaded windows within 60,000 total ticks.

The full-store fixture still offers 32 cobble at its nursery-tier trigger on the supported forward-minus-one lane, but
holds only that contribution's pickup until the confirmed store actually has tier-two capacity. This prevents tier-one
share backpressure from trapping foragers during clay-dependent preparation. All units remain on the ground while held,
then undergo real hauling. Food/clay waves, the 24,000 hall trigger, priorities, ledgers and acceptance windows are unchanged.
Smallest recovery completes at 57,018; fresh permanent mining revalidation completes this body at 54,908.

Regression and tier-two original/copy campaigns pass, with historical causes still unresolved. Death's three identical
bodies fail their immediate gallery-completion recovery assertion: seven cobble stored plus the named unit on the ground
conserve eight; later ordinary pickup/deposit reaches eight stored. Independent recovery can finish after mining. That
assertion conflict remains explicit and unchanged, despite a later permanent-body pass. Diagnostics retain custody,
cargo identities, accepted supplies, ingestion and valid food-gate pauses. Full acceptance is recorded separately in
`<turnloop>/directions/prime-ants-stage1/turns/T11/report.md`; a green execution cannot erase these negatives. The historical
eighteen-unit failure remains unexplained. Surface construction remains deferred and its T12 checkpoint is at risk.


### T12 bounded surface pipeline (2026-10-10)


T12 adds separate bundled semantic surface plans and a physical soil relocation owner after existing builder work.
The spoil envelopes remain 214/798 capacities; nineteen Mature and fifty-seven Great desired structural cells do not
create material. Each new cell takes one already owned recoverable mound unit through a twenty-loaded-tick worker
action; two gate conversions retain their existing paid unit. Current locations include gates, while historical
excavation deposits stay unchanged. Current owned paid structures survive regression; unavailable or player-revoked
targets remain incomplete. Stand goals are descriptive persisted destinations, with actual loaded support, collision,
visibility, reach and ground contact still checked at work.

Physical T12 delivery is partial: fifteen Mature cells in a controlled Great habitat, with eighty soil conserved,
then no reachable stand for the next raised mound step. No finished arch, crest or fortification is claimed. Same-state
protection passes 600/200; target protection, real hauling reload and named worker-death release are observed, but that
construction misses its fixed 12,000 bound. The independent death-unit recovery phase keeps 6,000 loaded ticks inside
60,000 total and requires direct named pickup/deposit. An early isolated pass and one late concurrent recovery pass
remain separate from two preparation/mining timeouts. See the T12 report for fresh unfiltered acceptance; all historical
coverage gaps in the preceding notes remain.

A bounded surface route stall releases the shared builder and preserves any paid cargo through named custody.

### T13 surface access boundary (2026-10-10)

A surface builder may briefly intersect its future cell on the way to an owned source or while centering on its saved
stand. It now keeps the claim and uses ordinary collision-checked movement toward that already validated stand; physical
work waits until all existing guards pass again. The permanent diagonal-source regression measures the original target
guard before movement and requires the same living worker's actual paid block and 200 settled loaded ticks. Independent
soil, current mandible cargo and custody remain conserved. Other occupancy keeps the existing refusal.

Each construction prefix has a supported, connected, visible next-task stand on open declared ground. The present closed
eight-high GameTest enclosure rejects the fifteen-cell prefix because raised stands need the barrier ceiling's cell for
two-high clearance; the four-high crest reaches that ceiling and five-high posts exceed it. Neither safe navigation nor
queue reordering grants permission to remove that barrier. Complete physical surface acceptance remains blocked within
the frozen enclosure. Planning bounds, 19/57 paid cells, 38 additional placements, two retaining conversions, 214/798
spoil capacities, supplies and deadlines stay fixed.

Plan order and the saved job codec remain unchanged. Actual T12 fifteen-paid-cell golden records preserve every receipt
and named release, and a physical fourteen-paid-cell hauling disk/data and canonical actor reload retains its claim,
source and one-unit cargo before resumed placement. Full build and all incomplete phases are recorded in the T13 report;
the historical timing negatives and coverage limits above remain.

### T14 acceptance harness and surface enclosure (2026-10-10)

Only surface fixtures use `surface_ground` (64 x 16 x 64). Minecraft barriers and actual air clearance are asserted;
ground remains local four, with the same terrain, actors, sources and bounds. The protection oracle retains its original
snapshot and accepts only an exact registered chamber wall transition observed during ordinary physical upgrade,
including original material authority, canonical clay consumption, built-cell prefix and exact payment ledger.
Unrelated player writes and unevidenced mod-owned writes remain negative. Great prefix geometry separately starts with
all nineteen Mature cells and remaining spoil; this abstract check does not establish real navigation.

The explicit permanent partition is `config/gametest-suites.json`: 73 quick server bodies and 179 long bodies. Discovery
rejects omitted, duplicate, unknown or unclassified names and changed declared bounds. Tick-path weights and T13 times
are scheduling evidence, not additive wall time or colony MSPT. Standalone server JVMs share one frozen compilation;
each has a fresh UUID world, report, log, execution manifest and lease. Long assignments are frozen before launch and
balanced by declared duration weights. Concurrency defaults to four, configurable only in the range one through four.
Registration and pruning share a mutex; newest ten worlds plus every older active lease/session are retained, with
temporary exceptions recorded. Historic required-case guards apply to the complete merged report.

Commands (Windows; the evidence helper sets the pinned JDK):

```powershell
.\gradlew.bat checkFast --console=plain --rerun-tasks
.\gradlew.bat checkLong -PprimeAntsGameTestShards=4 -PprimeAntsGameTestConcurrency=4 --console=plain
.\gradlew.bat checkLong -PprimeAntsLongNames=prime_ants_test:surface_game_test_controlled_mature_habitat_completes_every_paid_construction_prefix_and_walking_passage --console=plain
.\gradlew.bat checkServerSmoke -PprimeAntsGameTestConcurrency=2 --console=plain
.\gradlew.bat build --console=plain --rerun-tasks
```

`primeAntsLongNames` is a comma-separated list of exact permanent long names; it is explicitly diagnostic and forbidden
in `check`/`build`/`checkFast`. Existing single-case `runGameTest -PprimeAntsServerDiagnosticFilter=<exact name>` and
concurrent `runGameTest -PprimeAntsServerDiagnosticGroup=surface` remain labelled diagnostic interfaces. `checkFast`
executes every unit case, all 26 model cases, both asset guards, public-path/client-validator guards, harness/retention
tests and the quick server inventory. A complete build executes fast then complete long and verifies their exactly-once
union; child failures remain in their original XML and the merged report. A red server union is rejected after both
suites have been archived. Normal production jars remain build outputs.

Owner cadence: fast suite every turn plus affected exact long names; complete long suite at T15, T18 and release, and
after broadly shared colony changes. T14's actual supported-root reports verify its execution counts and red union;
review found that its wrapper rejection coverage was incomplete, so that run does not fully validate the harness.
T15 intentionally supports only a `testsuite` root: wrappers are rejected before case extraction in server, unit/model
and archived complete acceptance. Permanent offline fixtures preserve rejected originals/issues, including when a
case-only merged report passes independently. Freshness and exact discovery checks remain unchanged.
Surface milestones and
any remaining red components are recorded independently in the T14 report; no complete founder is inferred from a
controlled habitat or prefix model.

T15 prepares the unimplemented [finite founder-access proposal](founder-access-proposal.md): two exact owned-spoil
relocations `(-4,2,1) -> (-9,8,0)` then `(-4,2,0) -> (-10,7,0)` before retaining buried target `(-4,3,0)`.
Its one read-only diagnostic witnesses the first recoverable source and both destinations; after-state geometry and
saved access accounting remain proposed. No new terrain authority, structural-column exception or gameplay change
is granted. The decision packet and complete checkpoint are in `<turnloop>/directions/prime-ants-stage1/turns/T15/`.

### T16 finite founder access (2026-10-11)

The owner's answer authorizes the exact upper-then-lower two-move subjob. SurfaceAccess is saved inside SurfaceWork;
it shares the existing builder and preserves every earlier priority. Admission requires the actual owner/home, immutable
Mature plan, two placements plus two retained receipts, next paid target `(-4,3,0)`, seventy other owned soil units and
both positively owned sources. Only `(-9,8,0)` and `(-10,7,0)` receive saved owner/unit air reservations after live Mature
envelope/capacity/support checks. Every write, including air-to-air, revokes them; reload cannot recreate them.

Each material action earns twenty consecutive loaded game ticks at a reached, collision-checked supported stand with
visible mouth reach. Operation two reuses unchanged recoverable after operation one redeposits. Source blocks become
canonical one-dirt equipment carrying a stable unit identity, then actual owned mound blocks. Named custody uses the
existing TransferCustody owner, with one pickup after a real first carrier death and terminal custody after a second.
Pending custody forbids a same-identity ground shortcut. The named stand must see the actual low item point as well as
pass ordinary stand/path checks; a cube-centre ray alone is insufficient.

Access history `removed + named_pickups = redeposited + carried + released` is separate from structural/excavation
credit. Claims, fixed endpoints/stands, binding, clock, unit identities, actual transitions and terminal release references
survive coherent normal saves without a job cargo stack. Unknowns pause; known revocation, regression and route failure
end this attempt while preserving cargo. The ordinary selector must independently reach and retain the exposed paid
target afterwards. No third move or broader Great repair is authorized. Outcomes and measured bounds are in
`<turnloop>/directions/prime-ants-stage1/turns/T17/raw/access-safety-acceptance.json`; this is not abrupt-crash, tier-three,
established-colony, 120-adult performance, broad persistence or release 0.2.0 acceptance.

T16 outcomes: selected original founder completes nineteen paid cells, ordinary passage walking and 200 settled loaded
ticks at 53,657. The nine access bodies and their two copies have usable 27-body behavioral evidence, but eighteen
frozen copy names differ from native XML; T16's exact-assignment claim is false. Its final fast is green (73 server,
112 unit, 26 model), with 23/24 selected long passing and Great retained red. T16 missed empty-handed death/save and
continuing motivating-target ownership paths; its general safe-access claim is qualified by the T17 reproductions.
The required simultaneous six-body cohort remains red (four pass, two fail): founder reaches prefix 4/19, removes the
first source, then yields permanently when newly delivered clay enables existing Queen's Hall upgrade priority; Great
still misses its 12,000-tick deadline. Final founder custody is exactly the saved original one-dirt entity, with 71 positively
owned physical soil blocks alongside it. Access history is `1+0=0+0+1`; no continuation or fresh admission is minted.
This safe stop is a new founder negative and prevents overall complete-form acceptance despite the selected-run positive.
Priorities are unchanged, no broader repair is attempted, and the next complete-long checkpoint remains T18.

### T17 access death, target authority and diagnostic identities (2026-10-11)

Real lethal damage after five consecutive empty-handed removal or named-pickup ticks reproduced unloadable normal
saves: the dead claim cleared but access action progress remained. Access-specific cleanup now resets claim/action/time
without changing binding, endpoints, reservations or unit history. Source death safely stops with zero material events;
empty replacement death preserves the original named UUID/unit and carrier-death count, then permits only the existing
one pickup and actual redeposition after twenty fresh ticks. Positive action without a saved actor claim remains rejected;
already invalid T16-shaped records receive no cargo or authority migration. Old records without access and golden paid
prefixes retain their fields and credit.

The original paid target `(-4,3,0)` must remain positively owned throughout active access. Reconciliation before priority
gating, consideration, worker action and commit enforce this guard. Ordinary same-state target writes during removal,
carrying and named pickup stop the same attempt within 100 nursery-loaded ticks, preserving exact cargo/custody through
normal disk reload. Physical target presence is separate from its revoked ownership. UNKNOWN pauses and resets partial
work for the full 600 loaded ticks, then requires two fresh production evaluations. All other units and the original
protection snapshot remain checked; no structural/excavation credit, supply, form, priority, reach or bound is changed.

All six new bodies plus two identical concurrent copies pass (18/18). Pre-run runtime names exactly match XML; actual
archived bytecode verifies each method's mode, 12,000-tick annotation and structure and one shared body implementation.
Five-tick deaths, new SavedDataStorage reads, surviving canonical actors and fifteen named entity-region reads are
evidenced. The latter precede native GameTest success, which discards fixture entities; post-success absence does not
establish a production custody endpoint. T16's original mismatching packet and conserved founder negative are preserved.
The discovery helper now matches Fabric's letter-to-uppercase rule, including its unsplit digit boundaries. Diagnostic
collection freezes identities, bodies, bounds and inputs before launch, retains every red result, and validates native
exits even when PowerShell wraps the error line. Temporary wrappers/classes are archived and removed.

Current inventory is 267 server cases = 73 fast + 194 long, with 115 unit and 26 model cases. Final unfiltered fast passes;
selected long is 29/30 (the original 24 plus six new bodies), Great's unchanged 12,000-tick timeout only. The selected
founder completes 19/19 at 44,720; the one simultaneous six-body cohort is 5/6, with founder 19/19 at 45,290 and Great
red. Both founder passes include passage walking and 200 settled loaded ticks. This fresh cohort does not erase T16's
4/19 Queen's Hall priority cancellation, its 71 owned blocks plus one original named unit, or its never-started operation
two; no priority repair or preferred-result restart occurred. Other 164 long identities lack the changed conditional
access branches; T18 remains the next complete-long checkpoint. Packaging excludes test/copy classes and is not release
acceptance. Historical mining preparation timeouts, regression/tier-two timing causes, the eighteen-unit failure at
10,323, other paid-stall/persistence and unattended-development gaps remain. Tier three, established colonies,
120-adult performance, broad persistence and release 0.2.0 acceptance remain unmet; three turns remain after T17.

### T18 diagnostic exit contract and complete checkpoint (2026-10-11)

Diagnostic collection now returns CLI zero only when **both `infrastructure_valid` and `gameplay_passed` are true**.
The latter describes nonempty extracted XML gameplay rows and their failure/error/skip declarations; it cannot settle
acceptance without valid infrastructure. Native/launcher exit disagreement remains an infrastructure failure even when
the XML rows pass. Identity, freshness, executable-input, body/bounds, copy-equivalence and supported-root checks remain.
Five new permanent tests execute the actual collector and `main` in child Python processes, with real exit statuses.
Their twelve fresh, explicitly offline packets cover valid green, truncated/missing completion or shutdown, native exit
3 with PowerShell wrapping and launcher exit 3, each absent/unskipped client guard, and explained gameplay red/exit 1.
Only input/correspondence dependencies are isolated. Original fixture bytes and the CLI results are retained; none is
gameplay evidence. All 22 harness tests pass. Pre-repair false-success packets and the corrected packets are archived in
`<turnloop>/directions/prime-ants-stage1/turns/T18/raw/collector-cli-regressions.json`.

The scheduled single unfiltered `build --console=plain --rerun-tasks` is a **complete, validated red checkpoint**:
267 permanent server identities = 73 fast + 194 long, all exactly once, plus 115 unit and 26 model cases. Fast is green;
long is 193/194, with zero errors/skips and only the carried Great identity failing: "Didn't succeed or fail within
12000 ticks". The last primary curve is 42/57 at tick 11,500; it is partial construction, with no Great completion.
The native/launcher shard exits 0/1/0/0 agree with their XML and the build exit 1. Original child/merged XML, failure,
logs, process records and timestamps remain unchanged; supported `testsuite` roots, exact discovery, prospective frozen
assignments and all 985 executable-input hashes validate. Both client tasks are actually guard-SKIPPED, all 373 protected
client files and directory inventories match, peak long concurrency is four, and active leases/newest-ten retention hold.

Measured wall seconds: fast **126.114**, long **3714.922**, shards **2716.453 / 3298.953 / 3714.594 / 2513.421**,
total **3844.543**. T15's 3798.704 total/3668.031 long are historical planning observations, not promised timings or a
controlled benchmark. Packaging completed in the same build; both production jars exclude test/helper/copy entries.
No separate complete `checkLong`, restart of the red workload, new cohort/copy campaign or gameplay repair occurred.
The settling artifact is `<turnloop>/directions/prime-ants-stage1/turns/T18/raw/checkpoint-acceptance.json`; preparatory
fixture newline and external audit root-total mistakes are disclosed in its linked appendix, with original evidence.

T18's sharded founder body completes 19/19 at **44,978**, with passage walking and 200 settled ticks. It does not replace
T17's selected **19/19 at 44,720**, simultaneous **19/19 at 45,290**, or the simultaneous cohort's **5/6**, Great red.
T16 remains **4/19 after Queen's Hall priority cancellation**, 71 owned blocks plus its original named unit, operation
two never started. T17's real matching exits, markers and guards remain accepted within their stated scope; the collector
exit defect does not retrospectively invalidate them. Great's bound stays **12,000**; historical saved **43/57 is not
completion**. No priority, terrain authority, supplies, forms, assertions or stability windows change.

Retained nurses during UNKNOWN, loaded-damage miner cargo waits, and named mining recovery **6,000 nursery-loaded ticks
inside 60,000 total**, with **600/200** windows, remain unchanged. Historical mining-death preparation timeouts,
regression/tier-two timing causes, the unexplained eighteen-unit failure at 10,323, paid-stall/retained-building and
persistence gaps, mixed chunk-boundary issues, unattended pre-Mature clay/development, arbitrary-vein/all64 limits and
survey walking/dig limits remain. **Tier three, established colonies, 120-adult performance, broad persistence and release
0.2.0 acceptance remain unmet.** Two turns remain. Use T19 for bounded Great diagnosis from the preserved terminal
evidence and T20 for a verified partial Stage-1 handoff or honest closure, preserving bounds and the two-move terrain limit.

### T19 bounded Great disposition and required log evidence (2026-10-11)

Diagnostic collection now rejects omitted, missing or unreadable log evidence with a recorded infrastructure failure,
unknown native exit and nonzero CLI exit, preserving the independent XML gameplay verdict. Four added permanent
actual-collector/CLI child-process regressions bring the Python harness to 26 tests. Fresh before/after packets are
offline verifier fixtures, not gameplay evidence. T18 remains the independently validated complete red checkpoint;
all 520 preserved T18 evidence files retain their bytes. No unchanged complete workload was rerun.

The sole prospectively frozen, read-only Great diagnostic retains the 12,000-tick timeout and all original predicates.
First certain Great is observed at 100; Mature completes at 4,256; Great is admitted at 4,301. At the deadline its
receipt prefix remains 43/57, with 28 new Great placements, two paid gate conversions and thirteen inherited receipts.
Physical form is 47/57: six post-step cells and four upper watch-post cells remain absent. The next component is
post_step (-2,-4,2). Current physical soil is 79 blocks plus one canonical carried dirt, total 80; store stone remains 32.
Complete form, actual passage and 200 settled loaded ticks are not achieved. Historical saved 43/57 stays separate.

Bounded controller observations total 9,250: 7,225 movement-request observations, 960 fetching actions, 980 building
actions and 85 observed guard refusals. These overlap the coarse 4,256 preparation / 45 admission-wait / 7,699 Great-work
intervals and cannot be added to them. Four empty claims release after original source refusals; no bounded route-stall
release, false authorization, true priority or null selected stand is observed. Transient occupied-target rejection is
demonstrated; a navigation root cause remains unresolved. The native failure listener arrives at 12,002; its two automatic
lifecycle controller samples at 12,000/12,001 are excluded from bounded totals. No material transition occurs there and
no executor continuation is run. The endpoint accounting is explicitly labelled at the listener, with no 12,000 summary.

Final single `checkFast assemble` passes 73 fast, 115 unit, 26 model and 26 harness tests, with public-path, assets,
retention and client-validator guards. Both client tasks actually guard-SKIP and all 373 protected files match. Both jars
exclude helpers/tests/copies and are byte-identical to T18 production jars; packaging is not release acceptance.
Other long bodies retain production behavior, fixtures/assertions and suite membership; observation activates only for
this Great queen/world/lifetime. No new registered bodies, copies, terrain operation or gameplay repair was made.
Disposition and primary evidence: `<turnloop>/directions/prime-ants-stage1/turns/T19/raw/great-disposition.json`.

T18 founder 19/19 at 44,978, T17 selected 44,720 and simultaneous 45,290/cohort 5/6, and T16 priority-cancelled 4/19
with 71 owned blocks plus its named unit remain separate. Earlier mining timing/conservation uncertainty, eighteen-unit
10,323 failure, paid-stall/persistence gaps, mixed chunk-boundary issues and unattended-development/coverage limits remain.
Retained nurses during UNKNOWN, loaded-damage miner cargo waits, named recovery 6,000 nursery-loaded ticks within
60,000 total and 600/200 windows are unchanged. **Tier three, established colonies, 120-adult performance, broad
persistence and release 0.2.0 acceptance remain unmet.** T20 has one turn for a verified partial handoff or honest closure,
using T18's complete checkpoint plus T19's bounded unresolved Great diagnosis, without claiming complete Stage 1.
