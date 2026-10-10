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
