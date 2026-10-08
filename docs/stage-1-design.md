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
legacy fixture supply is unchanged. T09's report separates passing supplied-gallery/contribution/origin evidence from
unresolved interruptions and any red full build. Mature-gated mining does not resolve unattended pre-Mature clay supply.
Placement-survey walking routes and dig timelines remain uncovered; supplied passes do not establish unattended
development or arbitrary deposit reliability. Tier-three construction and Great growth acceptance remain deferred.
