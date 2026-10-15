## Follow-up - 2026-10-15 (stage-1c T09 established generation)

Production new-terrain generation now selects **50% Founding / 25% Young / 25% Mature before fit rejection**, using the
existing deterministic queen identity. There is no fallback to another profile. The unchanged 4x4 lattice, 56-block
spacing, 64-column/66-evaluation search and four supported biomes remain. Legacy pending records without a profile
retain Founding interpretation. Founding insertion still makes **zero block edits**, then ordinary queens excavate.

[EstablishedGeneration](../src/main/java/dev/primeants/founding/EstablishedGeneration.java) installs only a completely
validated plan footprint: witnessed natural soil, revocable witnessed surface air/short plants, supported dry
space, loaded and entity-ticking required cells, world border, no block entities, foreign structures or overlapping
bodies. Unsuitable sites are rejected; no neighbors are acquired and no terrain outside the plan is cleared. Young has
the founding nursery/food room and eight earth-lobe cells. Mature additionally has `widening_right`, the `left` store,
`hall_left`, and the complete 19-cell Mature arch/stepped crest: **24/72 underground cells**, **8/27 surface cells**,
**32/99 unique editable cells**, **34/102 explicit space/surface/marker operations**,
**35/104 admitted chunk writes including canonical stock projections**, and **5/25 actor insertions**. Workers retain their existing terrain
authority, including only the two previously authorized founder access moves.

Initial adults are queen plus **4/24 mature workers**, below caps **30/60 including the queen**. Queen age is 24,000;
workers have initial ages 4,800 + 100 per index. Both caches physically hold four apples/two chickens; Mature's owned
store physically holds sixteen clay balls. The queen has **8,000 initial ingested sugar**, zero initial protein and
the existing finite body reserve (0..39,000, default 39,000). Initial brood is zero. These are explicitly **generated initial world state**:
canonical membership has generated provenance, an empty generated nursery has no observed births/consumed brood, and
generated voids/surface cells have revocable ownership. Initial sugar is a separately persisted conservation base,
including in terminal adult history; it is never ingestion, income or consumption history. No excavation, hauling,
construction payment or surface-payment history is invented. The chamber registry starts Founding and ordinary live
evaluation derives Young/Mature from actual adults, chambers and stock.

The installation freezes version, profile, orientation, footprint limit and all actor identities before a verified
SavedData reservation and exclusive insertion fence. Its synchronous attempt is bounded, including every admitted LevelChunk write, and rejects any write outside the frozen cells. Validated canonical exterior heights/deposit routes are retained, not replaced with legacy geometry. The existing operator adult bound must admit the complete initial population; actors remain inert until live completion checks pass. Refusal or interruption is
terminal uncertainty, never an invitation to restore actors or stock. Focused negatives retain 99 unique edited cells (102 explicit write operations) with
zero actors, or those same 99 cells plus three inert partial actors; their markers have no initialized inventories or completed
colony registry. Completed events, unavailable lookups and observed deaths never authorize replacement.

Twelve permanent [generation GameTests](../src/gametest/java/dev/primeants/gametest/EstablishedGenerationGameTest.java)
cover genuine conversion, every physical component, live stages, ordinary movement/nursing and **200 consecutive
settled nursery-loaded ticks**. Seed **0**, vanilla Overworld noise/settings/biome source in the separately named
`prime_ants_test:established_native` diagnostic dimension: Young chunk **(6,5)**; Mature **(18,-3)** after rejecting
**(-2,13)** and **(2,13)**. Each profile freezes 256 biome queries and up to four sites before generation: **512 query
executions / 256 distinct queried chunks** per two-profile workload, four actual generated sites in successful complete
groups. This is one seed and observed plains fits, with no player contributions or terrain preparation; it is not broad
biome, slope or ecological sustainability coverage. Native Mature's first observer crashed while registering another
GameTest callback during iteration; its one-body recovery passes after using one persistent observer, with no larger
timeout or memory limit. Earlier gameplay negatives and the mining-origin fixture relocation remain disclosed. Source review then identified discarded exterior route data: the new genuine raised-route test fails on C8 at tick 4 and passes with retained canonical data. C8 long was intentionally cancelled and remains unaccepted; the owned native/client cancellation exits and partial reports are preserved. C10 proves total write accounting; C11 additionally guards operator adult bounds and activation ordering.

Persistence proof is **actual SavedData disk readback**, normal entity/block-entity reconstruction **in memory**, and
twice repeated FULL chunk serialization/deserialization. Entity decoding never inserts a replacement. This is not
chunk/entity disk unloading and reloading, a full-world restart, an atomic entity/block/ledger transaction, or power-loss
durability. Additive defaults preserve old saves: emerged membership, non-generated nurseries and zero generated sugar.
The original mining-origin test body is unchanged; its same sixteen-chunk development fixture follows the explicitly
selected Founding cell. All other T08 gameplay bodies, supplies and bounds remain intact.

**C12 controlled-fixture recovery.** The complete C11 checkpoint executes all 224 long identities and is valid red,
223/224, only the unchanged source-free scan timing out at its 18,000-tick bound. Explicit Founding selection moved
its unrelated nectar-control colony one lattice cell while the old one-flower fixture remained fixed. That same bounded
sixteen-chunk control region now follows the selected Founding cell, preserving the original source-free box, flower,
relative source position, amounts, assertions and bound. The exact original identity passes before the revised complete
checkpoint. Production code and all original GameTest bodies are byte-identical between C11 and C12.

Final checkpoint status is recorded in the external T09 settling packet:
`<turnloop>/directions/prime-ants-stage1c/turns/T09/raw/established-generation-acceptance.json`, with its report and compact
appendix. **Final C12 acceptance is green: 73 fast / 224 long / 118 unit / 26 model**, with **297 distinct server identities
exactly once** and no failures, errors or skips. Command wall times are **136.9727086s / 8539.0193074s**, not colony MSPT.
All **1,094** actual fast/long executable inputs agree. Both client tasks actually guard-SKIP; **373** protected client
files and **3,037** historical files are unchanged. Native peak is three; retention is newest ten plus active leases,
all leases release after native exits, and free space stays above 25 GB. No owner integration. No-new-green remains zero.
Tier three remains delivered; established generation is delivered. Performance/persistence and release acceptance remain open;
120-adult/multiple-colony performance and persistence, then release **0.2.0**, remain outstanding.

**Dated T08 erratum (2026-10-15).** The frozen `raw/t08-feeding-plan-c8-final-frozen.json` phrase **"Same C6 executable
test sources"** is stale prose. Its hashes and T08's independent review identify **C8**, including C7/C8 feeding
revisions. The frozen T08 file and historical failures have not been rewritten.

---


## Follow-up - 2026-10-12 (stage-1c T01 tier-three physical construction; partial effects)

**Worker-built tier-three construction and focused safety are accepted; item 2 is not fully accepted.** Ordinary
workers withdrew exact `minecraft:stone` from a confirmed physical store and faced all **44** owned packed-clay wall
cells: founding **8** (nursery/food together), material store **22**, hall **14**. The old one-unit paid clay core stays
in each wall; no clay is recovered, destroyed or paid again. Three physical jobs serve four live-confirmed functions,
with supported stands, one-unit cargo, twenty loaded facing ticks, one shared builder and two caregivers. Foreign and
same-state player writes revoke eligibility; general earth authorization is not extended to arbitrary packed clay.

The frozen effects are existing nursery **15 slots / x2** with a confirmed hall, food **18** (six reserved per kind),
materials **96** (48 reserved for clay), and hall **300** base loaded ticks. Tier-one/two recipes and effects remain
unchanged. Exact facing input is intact stone; cobblestone remains admitted bulk material. Thus original Great's
**32 cobblestone** is never facing input under ordinary rules. Actual stone walls and carried stone preserve the Great
stock lineage; retained clay cores preserve Mature clay. Jobs save exact material identity with a compatible clay
default for old tier-two jobs. Inventory format 3 keeps larger contents across a lower live capacity; format 1/2 bounds
retain their meaning. Projection ranges expand to food **1,216** and material **5,200** states, with in-cell geometry.

The new full-cache causal body first times out at its frozen **6,000** bound: full food **18**, four food-laden foragers,
zero stored clay and six clay on the ground. Tier-three dropped-food admission now reserves space for actual returning
food; it never spends, redirects or credits cargo. The same body passes at **3,686**, with food 18, six actual stored
clay and 200 settled loaded ticks. Earlier cache tiers retain share-only selection. Real carried-stone disk/data and
actor reload, lethal/repeated death with one named custody identity, a **600-loaded-tick** UNKNOWN pause, safe returns
at player/foreign walls, and missing-supply resume/full completion pass in permanent acceptance. No broader or
abrupt-crash persistence claim follows.

The all-effects body remains valid red at its unchanged **20,000-tick** deadline. Its final complete-suite construction
finishes at **7,523**, with 44 payments, 44 retained cores and empty claims/cargo. It observes **15** natural brood,
**944** consecutive doubled development steps and food beyond twelve. The last periodic sample at **19,500** has
**51** material units and **2** of the frozen **3** exact three-tick laying pairs. Exact timeout inventory is unknown;
no failure-listener stock snapshot was declared. Greater material admission and the larger-inventory reload/capacity-
loss endpoint are **not settled**. Neither constants nor this component progress closes item 2.

New-fixture initializer/observer/supply-budget failures, the earlier 32/44 food-blocked physical negative, a disclosed
prospective hungry-history and supply-phase correction, and all later red attempts remain in external evidence. No
existing gameplay assertion, bound, surface geometry, source supply, reach, action, builder count or founder relocation
is loosened. The asset coverage set initially retained old 16/8 ranges; that pre-server final-fast failure is preserved,
and a smallest asset-only recovery passes before the fresh final fast. No unchanged complete workload is restarted.

Final fast: **73 server / 118 unit / 26 model**, exit 0, **153.277s**. Complete unfiltered long: **201/202**, exit 1,
**5,099.920s**, native/launcher **0/1/0**; only the new all-effects body fails. All **196** prior long identities pass,
including unchanged original Great at **11,224**: **57/57 physical cells**, Mature **19**, Great **38**, two retaining
gate conversions, soil **80**, stone **32**, finite food **59**, actual passage and **200** settled nursery-loaded ticks.
These are command wall durations, not colony MSPT. Exactly-once union covers **275** server identities; **1,037** final
input hashes agree. Three-server concurrency, actual client-guard skips, protected files and newest-ten/active-lease
retention validate. Original fixture/access sources and historical reports remain unchanged.

Settling packet: `<turnloop>/directions/prime-ants-stage1c/turns/T01/raw/tier-three-acceptance.json`, with its report and
compact appendix. Consecutive no-new-green turns: **zero** (five new permanent physical/safety positives). Preserve the
unexplained eighteen-unit food failure at **10,323**, concurrent mining-death timeouts and historical regression/tier-
two timing causes. Remaining tier-three effect/reload proof, established generation, 120-adult/multiple-colony
performance, broad persistence and release **0.2.0** stay open. Only the existing two founder moves and unchanged
mining recovery **6,000 loaded within 60,000 total**, with **600/200** windows, remain authorized.

---

## Follow-up - 2026-10-11 (stage-1b T01 Great completion)

The original Great identity now completes within its unchanged **12,000-tick** bound: the final complete-suite
execution records **11,248**, independently inspected **57/57 physical cells**, Mature **19** and Great **38**
placements, two retaining gate conversions, soil **80**, stone **32**, finite food **59** accounted, an actual
ordinary walker and **200 settled nursery-loaded ticks**. The preceding exact-name candidate also passed at 11,277.
The historical Stage-1 report and appendix retain their original valid-red handoff and coverage limits.

Two small real-tick regressions establish concrete progress losses. A paid cell released its eligible builder but
left the 100-tick failed-attempt gate active; successful payment now invalidates that transient gate. Choosing the
build stand again after actual source recovery exposed a second refusal: the released worker could hold the next
queued cell while idle nursing had no accepting recipient. The original admission guard rejected before checking
that worker. Ordinary admission now permits only that sole eligible blocking body, with a supported source/build
goal that clears the target. Original work guards still forbid any recovery or placement while the target is occupied.
Other occupants and active finite access retain their original admission guards. No terrain, supplies, form, reach,
action duration, builder cap, priority, nursing rule, queue, receipt or save schema changes.

The three candidates are retained separately: first Great timeout at **50/57 receipts / 52/57 physical**, then
Mature **5/19** with no Great admission, then green. The cached-goal observation supported a reselection hypothesis;
it is not independently claimed as a general navigation root-cause proof. The sole-body regression first fails after
600 loaded ticks, then the same actual worker pays the pending cell after 180. Original plus two identical concurrent
copies of both new regressions pass **6/6**; temporary wrappers and classes are removed. The observer classifies an
intentional cleared stand at committed recovery as fetching work and preserves its original events.

Final `checkFast` passes **73 server / 115 unit / 26 model** in 135.609 seconds, with all guards. Complete unfiltered
`checkLong` passes **196/196** in 4,144.435 seconds; all three native/launcher exits are zero. Fresh exact union XML
validates **269/269 permanent server identities**, with identical final fast/long executable inputs. These durations
are command wall time, not colony MSPT. All **21 required surface long identities** are derived before launch; the unfiltered
suite also checks autonomous surface admission in development/mining scenarios. Only the ordinary surface successful
recovery branch of `WorkerTasks` changes; common movement, scheduling, nursing, custody, development and finite-access
helpers remain unchanged. Both final workloads explicitly request three shards and concurrency three (one native fast
server, three concurrent long servers). Fresh native,
launcher, XML, input, client and newest-ten/active-lease evidence is retained without world copies.

External settling evidence: `<turnloop>/directions/prime-ants-stage1b/turns/T01/raw/great-acceptance.json`,
with the report and compact appendix in the same turn. Consecutive no-green turns: **zero**. The pre-Java diagnostic-copy
launch failure is preserved; its parent-freeze path correction passes the fresh six-body diagnostic. A completed-workload
outer capture was stopped after its native/wrapper exit evidence was preserved, and direct-child exit probes validate
the final launchers. Earlier native timeout callbacks at 12,002 grant no construction
time. Preserve the unexplained eighteen-unit food failure at **10,323**, concurrent mining-death campaign timeouts and
historical regression/tier-two timing causes. Tier three, established colonies, 120-adult/multiple-colony performance,
broad persistence and release **0.2.0** remain open. Only the original two founder relocations and unchanged nursing/
mining recovery protections are authorized.

---

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


### Stage-1c T02 (2026-10-12): current oversized retention; hall remains valid red

The frozen final T01 fixture, all supplies/history/timing/placement and the 20,000-tick combined bound remain unchanged.
A scoped causal check finds native-food positions scanned after both actual native admission gates close, while dropped
materials remain admissible. Five `WorkerTasks.search` lines now use the existing material search after the full dropped-food
turn for a live tier-three cache in that state; pickup still rechecks food. The actual clay-window inadmissible inspections
change 134,456 to 0, with real food 18/material 6/200 settlement. Earlier cache tiers, recipes, food/growth rules, nursing,
single-unit trips, one builder, reach/supports and action durations stay unchanged.

The new independent larger-inventory body passes at 19,622: current 8 apples + 5 chickens and 65 clay survive all five exact
ownership/content/display boundaries, in-memory serialization/reconstruction and loaded capacity loss 18/96 to 6/32.
Both inventories remain oversized; admission refuses more units. This is block-entity reconstruction in the same loaded
level, not disk/world reload. T01 carried-job disk/data proof remains separate. The body preserves 44 stone payments and
paid clay cores, empty construction claims/cargo, 15 natural brood, 1,770 doubled measurements and 12,133 settled loaded ticks.

Hall acceptance now requires four actual egg UUIDs and three consecutive intervals with all non-cadence gates observed
before production decisions throughout each interval. The original combined and independent hall identities still time out at 20k;
each final peak is one interval. Queen nutrition/funding shortages and later 15/15 slots interrupt eligibility; no laying rule,
food gate or fixture is changed to manufacture a fourth event. Last bounded combined clocks 19,999/20,000 show 7 food and 74
materials (72 clay + 2 cobble), valid care/hall/free adult cap and funded growth, but no free brood slot. Listener 20,002 is
diagnostic lifecycle evidence and adds no acceptance time. T01's 19,500: 51 materials/two isolated pairs, timeout stock UNKNOWN
and unreached reconstruction/loss phase remain distinct.

Final fast 73/118/26 passes, exit 0, 152.409s. One complete unfiltered long run is 200/204, exit 1, 7,133.617s, native 1/1/2.
The other negatives are original material hauling 48k and tier-two natural growth 60k; attribution is unresolved, with their
unchanged sources and endpoint limits retained. 194/196 pre-T01 and 199/201 previously green T01 long pass. All five existing
green tier-three safety/construction bodies and the new inventory witness pass. Original Great remains green at 11,262,
57/57, 19/38 placements, soil 80, stone 32, food 59, passage and 200 settlement, with unchanged 12,000/200 bounds.

Infrastructure validates 277 unique server identities, 1,048 matching input hashes, guards, protected clients, leases/newest-ten
retention and original native/XML evidence. No unchanged broad restart or relaxation occurs. Settling evidence is
`<turnloop>/directions/prime-ants-stage1c/turns/T02/raw/tier-three-effects-acceptance.json`, with linked gate and negative packets.
Item 2 remains open; consecutive no-new-green count 0. Historical food 10,323, mining-death/timing uncertainties and Great 47/57
stay historical. Established colonies, performance/broad persistence and release 0.2.0 remain later work.


### T03 food-service regression checkpoint (2026-10-12)

VALID_RED; item 2 remains open. T03 adds scoped read-only FoodServiceProbe observations, actual controller-action
traces and live pre-success snapshots for the original older bodies. Production, fixtures, supplies/history, multipliers,
recipes, capacities, reach/actions, one builder, food-first rules, reserves and queenNeedsFood's protein threshold remain unchanged.
The four-event/three-consecutive-interval counter and strict 20k helper deadline remain unchanged.

Final fast 73/118/26 passes, exit 0, 127.249s. One complete unfiltered long run is 202/204,
exit 1, 5882.835s, native 0/1/1. Exact original results:
Material hauling PASS (48000; 20063/14333); Tier-two natural PASS (60000; 52018/46288); Combined tier-three RED (20000; timeout 20000); Independent hall RED (20000; timeout 20000).
All native negatives remain visible; no unchanged complete suite is repeated for a preferred outcome.

Independent complete/canonical cache, ingested adult/brood and worker-cargo decomposition matches production funding.
Commitments remain obligations; historical consumption receipts are not current stock. T02's loaded 8423 sugar closure
(51731<55000, queen protein 6,000) and 11877 protein closure (19200<27200, queen protein 2,000) remain adverse evidence.
Later 15/15 slots legitimately block laying. No hall defect or protein-prefeeding necessity is inferred from these counters.
Physical pickup/delivery, nurse withdrawal/recipient/approach/feeding/return and actual consumption clocks remain inspectable.

T02's original material carrier stalls in DEPOSIT after crop sharing and later starves/releases its one clay. Its exact
historical route and timeout inventory remain unknown; later green replays do not establish a repair. T02's earliest
all-earlier-effects tier-two interval has true sugar/income deficits and unreached natural phases. New exact observations
and any current positives remain separate from those historical failures. A targeted missing-supply reference-equality
negative after another test's global saved-data reinstall is retained; the isolated original reuses identical compiled
inputs and passes. No fixture/assertion repair is used to conceal it.

Independent current inventory passes at helper 18,585: 5 apples + 9 chickens and 67 clay at all five reconstruction/loss
boundaries. Original Great passes at 11,241 with 57/57 cells, 19/38 placements, soil 80, stone 32, food 59, real passage and
200 settled loaded ticks. These new observations do not replace T02's exact positives or historical negatives.

Infrastructure validates 277 unique server identities, 1,055 frozen inputs, original native/merged XML and logs, protected
clients, actual guard-SKIPs and leases/newest-ten retention. Tier-three inventory preservation remains in-memory
block-entity reconstruction and loaded capacity loss 18/96 to 6/32; existing carried-job disk/data safety is separate.
Settlement: `<turnloop>/directions/prime-ants-stage1c/turns/T03/raw/t03-food-service-checkpoint.json`.
No-new-green count 0 reflects both restored older complete acceptances and the green regression checkpoint;
no causal production repair or general timing robustness is claimed. Historical food 10,323, mining-death/regression/tier-two timing uncertainty and
Great 47/57 remain. Generation, performance/broad persistence and release 0.2.0 remain later work.


### Stage-1c T04 (2026-10-12): personal-meal nursing recovery accepted; hall remains separate

Real-tick frozen regression before repair witnesses original nurse personally eating its carried apple, then empty
NURSE_FEED retaining the old recipient through600 ticks despite an accepting queen and one reachable replacement.
Three production lines now distinguish genuinely empty feed cargo: clear stale recipient/action and resume ordinary
NURSE_CACHE. Meal priority, actual single personal receipt, nonempty invalid cargo, authorization/caregiver counting,
ordinary cache withdrawal/walking/reach/20-tick feeding/revalidation/return, other claims and saved phases are unchanged.
Same living nurse after repair: meal6324, recovery6325, replacement6350, actual feeding6370, settlement6570.
Exactly one personal and one recipient consumption conserve two units independently;200 settled ordinary caregiver ticks.
Saved-empty and non-food controls pass targeted and complete long. Initial checkpoint/history is explicit; no forced
empty transition, direct ingestion or controller tick after the acceptance boundary. Nursing restore scope is in-memory.

Original same-server missing-supply/cargo-reload pair reproduces the reference failure, then passes2/2 after a narrow
assertion correction to persisted logical identity and paid-prefix continuity. Two actual reconstruction overlaps
include another colony's waiting1/8 job; global persisted jobs/terrain/registry/custody and canonical claimed cargo/task
state match exactly. Original600 pause, resumption and200 settlement retained. T03 co-run518/reload514 and isolated
control remain historical negatives; final sharding alone is not this interaction proof.

Unchanged targeted hall pair still times out at20000. Complete hall comparison: HALL: empty 0 worker-ticks / invalid-empty 0, max interval chain 2; COMBINED: empty 3 worker-ticks / invalid-empty 0, max interval chain 1.
Long empty-feed dwell is repaired, but actual withdrawal/feeding counts and independent nutrition/funding/income/slots
still interrupt the four-egg/three-consecutive-qualified-interval contract. No hall cause or threshold/prefeeding policy
is inferred from nursing recovery; final T01/T02 fixtures, supplies/history, positions, waves, multipliers, recipes,
capacities, cadence, reserves and queenNeedsFood remain frozen. Strict helper<20000; listener clocks are diagnostic.

Final fast73/118/26 green, exit0,117.807s. One complete unfiltered long204/207, exit1,
6592.480s, native[0, 2, 1]. Regression checkpoint green=False; item2 accepted=False.
280 exact identities,1061 frozen inputs, native/merged XML/logs/executable archives, actual guard-SKIPs, protected clients,
leases/newest-ten retention and concurrency≤3 preserved. No unchanged full retry. No-new-green count0 is earned by
demonstrated same-nurse physical recovery, not repeated older positives. Settlement:
`<turnloop>/directions/prime-ants-stage1c/turns/T04/raw/nursing-recovery-acceptance.json`.

T01/T02/T03 hall timeouts and older unexplained hauling/tier-two outcomes, food10323, mining-death/timing uncertainties
and historical Great47/57 remain separate from later positives. Current inventory reconstruction is in-memory;
original carried-job disk/data safety is distinct. Established generation, performance/broad persistence and0.2.0 stay open.
The supplied founder/Mature surface identity additionally times out at60000. Original59500 receipt prefix4/19,
recovered2/placed2,cargo0/claimnull andstopped_access_stage_or_priority_changed remain bounded native evidence;
complete form, passage and200 settlement unmet. Relationship to nursing recovery is not established. Shared regression
checkpoint stays RED/INCOMPLETE; controlled Great is separate. No policy/fixture/route expansion or preferred-result retry.

Current T04 other acceptances: hauling20123/14393; tier-two natural54198/48468. Inventory19306 retains8 apples+6 chickens
and67 clay across all five exact in-memory boundaries. Controlled Great11265 retains57/57,19/38,soil80,stone32,food59,
walking passage and200 settlement, separate from supplied founder/Mature4/19 timeout. Older timing causes remain unresolved.


### Stage-1c T05 (2026-10-12): finite access admission ordering

Original supplied founder is PASS at helper 43678 within unchanged60,000. Its fixture, supplies,
histories, terrain/accounting, stage promotion, physical19-cell form, ordinary passage and 200 loaded settlement assertions
remain unchanged. This is one current original result, not general timing robustness. Controlled Great independently passes
11238:57 physical cells,19+38 worker payments,soil 80/stone 32/food 59, passage and 200 settlement. Nursing recovery and controls pass.

The prospective read-only observer captures the actual first stopping decision in a controlled real-tick reproducer at 545:
Mature/unlocked true, widening/excavation/existing-upgrade/mining false, confirmed Queen's Hall tier 1 with one physically
delivered clay enabling genuine tier-two priority. Operation one carries its original named dirt, operation two is unstarted,
and prefix remains4/19. Nondeath terminal custody conserves 71 owned soil+one named unit=72. This matches T16's Hall mechanism;
T04's matching native sequence still has no individual first-stop predicate/exact timeout census, and nursing attribution
remains unproved. Pre-repair instrumented original PASS 43818 is separate evidence, not the causal before-run.

A single new-admission guard waits through pending genuine tier-two selection, including a temporarily unfunded gap.
Earlier priorities, exclusive builder, saved shapes and terminal/revocation safeguards retain their behavior. Identical
before3/after1 test source changes red 547 to green 5058: ordinary ten-clay Hall completion precedes one access admission,
then both authorized physical relocations, actual motivating-target payment and 200 loaded settlement occur. Binding,
prefix and original two unit identities remain continuous within each attempt; fresh-world IDs differ. Access custody
settles with zero deaths/pickups/releases; later ordinary surface work may still use its one builder. No old terminal
attempt is revived, no priority suppressed and no third relocation authorized. Semantic surface coordinates and dy=layer+1
remain; original disk/revocation/stage-loss/death/obstruction/cargo-quarantine controls execute unchanged.

Fast 73/118/26 passes in 127.769s. One unfiltered native long suite is 207/210 in 5596.806s,
native exits [0, 1, 2]; final inventory 283=73+210 includes three causal copies. COMBINED: max chain 1, 20,000 timeout; HALL: max chain 1, 20,000 timeout.
Tier-two natural growth also times out at 60,000: all chamber functions are built, but natural brood display/reload/UNKNOWN phases are unreached; last bounded diagnostic 59506/53776 shows recent-income shortage. Its relationship to this repair is unproved.
Historical T04 Hall maxima2/1 and T01–T04 negatives remain distinct; founder/Great do not cover Hall. No unchanged Hall
campaign is added. T06's scheduled complete suite remains mandatory.

Executor session storage failure disconnects the original Gradle client at11:16:19, cancelling its wrapper while frozen
native servers continue. Original parent exits remain UNKNOWN. Targeted durable-launcher exit 42 and report-only gate0
prove orchestration recovery. First report gate 1 fails on the missing original aggregate; exact single-case aggregation
smoke then native 210-row reconstruction restore the report path. The recovered final checkLong exit 1 validates those same complete reports with no new
gameplay, compile, world or server restart. Actual original guard-SKIPs,1073 frozen inputs/archives, protected clients,
newest-ten+active leases, concurrency≤3 and prelaunch25GB minimum are preserved. Registration-blocked/inconclusive
prototype setup and local reader/display errors stay disclosed in the appendix; no original acceptance is relaxed.

Settlement: `<turnloop>/directions/prime-ants-stage1c/turns/T05/raw/founder-access-acceptance.json`; linked report, appendix,
causal/final-native/recovery evidence and final commit/tree verification remain external. No-new-green count 0 is earned
by demonstrated physical before/after access progress. Food 10323, mining-death/timing uncertainties and historical
Great 47/57 persist. In-memory nursing/inventory reconstruction is separate from disk/world persistence; established
generation, performance/broad persistence and release 0.2.0 remain open.


### Stage-1c T06 (2026-10-12): actual admission ordering and a bounded funding negative

The shared original/copy founder regression now checks the real nonnull SurfaceAccess.admit return: all ten paid
Hall conversions and completion, released builder/null claim, zero carried/released clay, exact ledger and physically
confirmed live Hall2 must already hold. Same-clock event order is accepted; no extra tick is added. Both authorized
relocations, immutable prefix/binding/unit identities, physical motivating target, finite39 food/10 clay/72 soil,
6,000-loaded/12,000-total bounds and200 settlement remain. Original and both copies pass targeted and complete checks.

An independent new control declares an existing tier-three habitat with nine workers and zero initial food/nutrition
receipts. Its finite16 apples/6 chicken budget (64,000/48,000) is dropped physically in two fixed waves; matched shortage
uses one chicken. Preparation4800, measurement600 loaded and total5400 are frozen. Both controls fail preparation:
queen genuinely consumes2 apples/1 chicken, workers consume zero, and maximum real recent sugar8000 misses the8250
minimum reserve. Ordinary peer-service eligibility needs6000 fasting ticks (personal hunger12000), outside preparation.
Current stock and independent physical+single-consumed16/6 and16/1 ledgers remain exact; ground units are excluded.
No actual eggs or intervals are obtained, no budget/bound is extended and no hall-cadence defect/repair is claimed.
The plan predates every native child but follows wrapper compilation/freeze by0.060s after a CRLF checker error;
JSON first-wave1 versus frozen executable0/1 (actual0) is disclosed. Setup is neither worker construction nor new birth.

T05 tier-two's first recorded missed window after nursery expansion is30939/25210: income32000>=22500 but current
sugar27216<31500. Two ordinary pickups add8000 real cargo without intake receipts; next lay31050/25321 follows112
loaded ticks instead of12. Third-brood31203 needs37000 vs32029. At59506/53776, current29664>=24000 disables the
fixture's wave while growth needs37250 current and32250 income (actual4000). Meals/feeding/deliveries continue;
permanent service stall, bad accounting and admission-repair causation are unproved. Exact T05 timeout census remains
UNKNOWN; existing59800/54070 observation is separate. No original fixture, policy, cost/reserve or saved format changes.

Mandatory fast passes73/118/26, exit0, 140.061s. Complete original-plus-control long is207/212,
exit1, 7280.720s, native[1, 3, 1]; final inventory285=73+212. Original Hall maxima HALL:1, COMBINED:2,
both timeout20000; current tier-two PASS at54153 (does not explain T05). Original supplied founder times out60000
at4/19; Great passes57/57 at11426. Their original physical/passage/accounting/200-settlement assertions remain;
earlier negatives and the current original-founder failure are preserved.
Current original supplied founder4/19 timeout60000 has admission59911 (89 ticks remaining), both operations stillSOURCE,
action0 and no first terminal stop; the preceding delay cause is unproved. Focused copies do not replace it. T05's original
19/19 at43678 remains separate historical acceptance. Current tier-two PASS, if present, does not diagnose T05's red run.
Durable smoke42 and real wrapper exits verify; no recovery/replay, client launches or executable mutation while native
servers run. Actual client guard-SKIPs,962 archived source/class/resource inputs plus120 pinned JAR hashes, protected
clients, newest-ten+active leases and25GB floor verify. Command durations are not colony performance.

Settlement: `<turnloop>/directions/prime-ants-stage1c/turns/T06/raw/t06-hall-funding-checkpoint.json`, with report/appendix,
native/merged XML, logs, freezes, exits, shutdown, conservation and final verification. No-new-green1; a fresh
prospective control must establish ordinary adult service/recent income eligibility before measuring cadence. T01–T05
hall negatives, T16/T04 founder history, food10323, mining/hauling uncertainties and Great47/57 remain. In-memory
reconstruction is separate from persistence; generation, performance/broad persistence and release0.2.0 stay open.

### Stage-1c T07 (2026-10-12): meal-eligible physical hall control pair accepted

The independent HallFunding pair now declares nine mature workers and the queen with coherent 12,000-tick
age/active/fasting history, default 144,000 lifespan/24,000 grace, completed callow clocks and zero initial
nutrition, ingestion, maintenance or coverage receipts. This is pre-existing adult history, not new birth or
nutrition credit. All histories and supported initial positions are fixed before the first callback.
The same finite 16 apples/6 chicken (shortage 16/1) is allocated as initial physical cache 8/3 (8/1), then
ordinary dropped remaining 8/3 (8/0) at the explicitly declared helper 0/1 startup callback. No second wave,
post-boundary actor manipulation, direct ingestion or growth call is used. Preparation 4,800, measurement
600 nursery-loaded and total 5,400 bounds and original four-event/three-interval/30-refusal assertions remain.

The plan precedes compilation and wrapper launch, with normalized Git-content and actual-byte source/hash
correspondence passing first. An initial validation error read the detached empty cache object after
SurfaceFixture.loadBlock had replaced it: both native controls fail the setup assertion at tick0. Its exact
source, plan, native XML/logs and exits [1,1] remain archived. Only that assertion's reference changes to the
actual world cache; the physical setup, histories, resources, schedule and bounds are unchanged. The ensuing
matched run passes both controls, exit0/native[0,0]. This is a disclosed assertion correction, not an adjusted
preparation experiment. Any measured failed preparation would still prohibit a setup retry or extension.

Five genuine worker apple ingestions occur at loaded20/20/25/37/37, with queen apple/chicken receipts visible
by73/74; ordinary crop sharing serves remaining fasting adults without ingestion credit. At warmup2401,
recent sugar is24,000 and all ten adults qualify. Funded eggs are four distinct UUIDs at2401/2404/2407/2410;
all nine intervening loaded pre-decision rows qualify and three consecutive unchanged3-tick intervals pass.
The decision sugar/protein stocks are54,104/48,000;53,094/46,000;52,086/44,000;51,082/42,000, against unchanged
requirements13,250/10,000;18,000/18,000;22,750/26,000;27,500/34,000. Queen nutrition also qualifies at every lay.
The shortage arm records thirty actual FoodLimitedGrowth.allows false returns at2401..2430 with protein
8,000<10,000 while care/habitat/slots/population/queen/income/current sugar qualify; zero eggs are laid.
Observers distinguish calculated eligibility, earlier/cadence permission, actual funding return and laying;
no observer invokes a second mutating decision and interruption resets remain intact.

Independent per-callback unit and yield ledgers and the explicit before-success census conserve every item.
Funded apples:1 ground+9 canonical cargo+6 consumed; chicken:5 cached+1 consumed. Shortage apples:10 cached+
6 consumed; chicken:1 consumed. Custody is zero. All declared adult UUIDs survive, genuine births zero.
Funded maintenance is5,918 sugar plus4,000/8,000 egg spending; shortage maintenance5,968. Listener snapshots
after habitat cleanup/result finalization are not substituted for this acceptance census. The controls do
not prove eventual larval development, worker construction of the initial habitat or integrated hall success.

T06's failed zero-worker-consumption preparation, sources, plans and five complete-checkpoint failures remain
unchanged. Wording correction:30,939/25,210 is a calculated pre-lay funding shortfall, not an actual funding
refusal. Previous lay25,209/cadence12 causes an earlier return; first due25,221 was not captured. Retained
growth.reason is not another decision. T06's accepted fast73/118/26, complete207/212 and Great57/57 at11,426
remain historical checkpoint facts, separate from the current component result.

Original founder native chronology: founding settles5,757, first Young8,572, first Mature47,081 (28 adults,
physical clay16), access59,911. Young-to-Mature consumes38,509 ticks; serial8/22/10 paid clay upgrades with
eight material-store/two Hall waiting-for-store releases consume another12,830 after Mature. Actual Hall2
completion/released builder precedes admission. T06 still times out4/19 with both operations SOURCE, no action
or terminal stop and only89 ticks, below even the unchanged200 settlement. Exact silent supply callbacks,
continuous caregiver/free-builder availability and the longest continuously blocked admission predicate are
UNKNOWN; events without clocks retain neighboring native-clock brackets. No original rerun or causal repair
is claimed, and focused-copy success does not replace this supplied founder negative.

Final scoped acceptance: checkFast with explicit shards3/concurrency3 passes73/118/26, exit0; selected long is
the exact two independent identities,2/2. Inventory remains285=73+212, Python harness26 passes. Shared production,
common helpers/observers and original TierThree/TierTwo/Surface fixtures are unchanged, so no unfiltered long
suite is launched; T09's scheduled complete suite remains mandatory. Actual client guard-SKIPs, protected client
inventories, frozen input archives/120 pinned JAR hashes, exits/shutdown, newest-ten+active leases and25GB floor
verify. Settlement is `<turnloop>/directions/prime-ants-stage1c/turns/T07/raw/t07-hall-control-acceptance.json`,
with linked report/appendix and primary native evidence. No-new-green resets1->0 on the qualifying pair.
Original integrated hall acceptance and founder reliability remain open; recurrent tier-two/hauling, food10,323,
mining-death and historical Great47/57 uncertainties persist. UNKNOWN nursing and loaded-damage mining bounds,
established generation, performance/broad persistence and release0.2.0 remain unchanged/open.

### Stage-1c T08 (2026-10-15): owner delivery decision and test-only feeding

The owner's 2026-10-14 decision accepts tier three as delivered through physical construction and its
independent effects, including T07's funded four eggs and thirty actual protein refusals. T08 restores
regression acceptance under that decision. Historical reports and failed experiments remain intact;
generation, 120-adult/multiple-colony performance and persistence, and release 0.2.0 follow this checkpoint.

The supplied founder uses an explicit `EARLY_PHYSICAL_DEVELOPMENT` player-supply profile. The initial
12 apples/10 chicken remain; small refills count current ground, cache, canonical worker cargo and named
custody together, with targets 2/4 before nursery tier two and 4/8 afterward, at most two units per kind
per 200 ticks. Finite total caps are 1,212 apples/610 chicken. The first sixteen clay arrive when the
physical owned store exists, with the remaining forty after nursery tier two: the original 56-clay budget,
recipes, real founding, stages, geometry, two authorized relocations, walking and 200-tick settlement remain.
This avoids adding food behind already carried units when the six-unit cache is full. Milestones, actual
drops and compact silent no-drop reasons separate population, food, clay, worker counts and unfinished jobs.

Natural tier two has its own `NATURAL_HALL_AFTER_CONSTRUCTION` profile and the unchanged eighty clay.
New chicken is withheld from actual Mature until physical Hall tier two; existing food and brood remain.
After Hall confirmation, the twelve-unit physical food target becomes eight apples/four chicken. Ordinary
meals produce income and natural expanded brood; stock itself supplies no income. Its paid geometry uses
8/22/14 clay cells, whereas the supplied founder uses 8/22/10; earlier generic planning wording conflated
these. Recipes and actual quantities never change. Other `TierTwoFixture` callers retain identical legacy
feeding. UNKNOWN nursing authorization, loaded-damage miner cargo/claims, 6,000 nursery-loaded recovery
within 60,000 total, and the 600/200 windows are preserved.

Only the two original integrated hall bodies use `POST_CONSTRUCTION_HALL`. Their 49 existing adults have
coherent pre-boundary 12,000 covered/active ticks and 3,000 maintenance each. Forty-four retain 1,000 sugar
from one apple; five retain 5,000 from two. Queen nutrition starts at two apples/one chicken; cache holds
eight apples/four chicken. Initial totals are 64/5, including every consumed unit. After all 44 real paid
stone conversions, ordinary physical waves add at most 120 apples/72 chicken; the original oversized-stock
wave remains 18/18. Total finite maxima are 202/95. Food drops use the supported ordinary meal approach
(4,-1,-2); materials and the accepted inventory/safety profiles retain their sources and supplies. Initial
history is excluded from rolling income. Four real eggs require actual recent receipts, current reserves,
queen nutrition, free slots and all intermediate gates. All construction, exact ledgers, natural brood,
development measurements, current oversized reconstruction/loss boundaries and bounds remain unchanged.

C1-C5 negatives, plans, exact diffs, commands, exits and executable archives remain in T08 evidence.
The C5 forecast understated additional maintenance for three reserve adults (4,500, not 1,500 combined);
its first three eggs and fourth-gate shortfall remain explicit. The revised five-adult reserve calculation
accounts for 7,500 additional maintenance and keeps stock separate from ingestion receipts. No history,
nutrition, hunger, brood or gate is manipulated after the boundary. Source comparison proves every original
body assertion/bound and legacy feeding unchanged; production, client behavior, assets, versions and save
schemas match baseline `14ef11f`.

Procedural follow-up: T07's external correspondence at 11:12:50.079 UTC followed wrapper startup at
11:12:40.340. T08 preserves that deviation and checks externally before durable supervisor startup,
then internally before Gradle/native execution. The delayed smoke returns actual 42. Fresh complete
fast/long acceptance, exact rediscovered coverage, native exits/shutdown, frozen archives, actual client
guard-SKIPs, protected clients, newest-ten plus active leases, the 25 GB floor and peak direction concurrency
are settled in `<turnloop>/directions/prime-ants-stage1c/turns/T08/raw/t08-green-checkpoint.json` and its
linked report/appendix; selected positives alone cannot establish the complete checkpoint.

### T08 complete-suite follow-up (2026-10-15): prepared carrier feeding window

The first fresh complete workload is preserved as **211/212 long**, exit1/native[0,1,0],6949.597s.
It passes the original founder at44,144 (15,856 ticks remain), both original integrated halls, natural
tier two, Great and inventory. Its sole new negative is the existing forager-scaling simultaneous-clay
witness at the unchanged30,000 deadline. Both foragers physically deliver all eight clay units, but never
hold two clay units simultaneously; the eventual mixed apple/clay reload is not that required witness.

Only that witness's test feeding preparation now continues the existing on-demand refills while waiting
for its two actual empty SEARCH foragers. Before the same finite eight-clay drop, a read-only readiness
check requires complete/nonhungry live supply, current sugar covering existing brood plus the unchanged
2,400-loaded-tick maintenance safety calculation, and non-queen protein covering existing brood and a
needed queen portion. Ground food supplies no income or investment stock. Shared onDemand/F3 behavior,
3/2 opening food, five-unit physical target,200-tick kind intervals, seven-by-seven supported L route,
eight-clay budget, claims/caregivers, real simultaneous-clay/reload/service assertions and30,000 bound
are unchanged. No actor, nutrition, hunger, history, phase, progress or gate is assigned/reset.

The frozen matched F2/F3 targeted workload passes2/2, exit0/native[0,0],145.512s. F2 stages the funded
clay drop22,699 and records two actual clay carriers23,103; F3 reaches31 adults32,226. The saved reload
uses its actual cargo types (two apples), with the separate later two-clay witness still mandatory.
Read-only buffer/phase/cargo traces retain the distinction. This is a test-only response to the new
complete-suite negative; older hauling failures are not retrospectively explained. Original negative
XML/logs/assignments/executable archives remain. A new fresh fast and complete long workload follows the
executable change; an unchanged complete workload is never repeated for a preferred result. Current
settlement remains `<turnloop>/directions/prime-ants-stage1c/turns/T08/raw/t08-green-checkpoint.json`.

### T08 natural feeding follow-up (2026-10-15): ingestion during real development

The second complete workload is preserved as 211/212 long. The repaired F2, original
founder, integrated halls and inventory bodies pass. Natural tier two times out at
60,000, Young with 24 adults and no brood: current protein remains 40,000 while
recent sugar receipts of 8,000--16,000 miss the next investment's 18,750 maintenance
requirement. Earlier green checkpoints do not explain or cancel this negative.

Only the explicit natural tier-two feeding profile changes. Before real Mature,
opening food is three apples/two chicken and physical refills maintain the same
five-unit 3/2 mix, one unit at a time, apples first, with existing 200-tick kind
intervals and all incoming cargo/custody counted. This uses the unchanged F3
control's real-growth feeding conditions; integrated completion is still required.
At real Mature, four physical apples support maintenance and stage evidence.
Until physical Hall2, chicken deliveries cover unfed existing brood plus a needed
queen portion; after Hall2 the previous 8/4 physical target resumes for expanded
brood and reload acceptance. Current stock is never credited as rolling income.
Finite caps remain 1,212 apples/610 chicken; actual unit/yield ledgers cover every
wave. All 80 clay, recipes, physical hauling and original assertions/windows/bounds
remain. The founder's 12/10 opening and feeding profile remain unchanged. No
nutrition, hunger, receipts, progress, slots, adults, rooms or stage are assigned.

The matched frozen natural/founder target passes before the new fresh fast and
complete long workload. Both complete negatives, their frozen inputs, logs, XML and
exits remain under `<turnloop>/directions/prime-ants-stage1c/turns/T08`. No unchanged
complete workload is repeated for a preferred result. The owner decision that tier
three is delivered remains authoritative; regression acceptance is settled only by
`<turnloop>/directions/prime-ants-stage1c/turns/T08/raw/t08-green-checkpoint.json`.
