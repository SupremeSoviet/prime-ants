# How a colony develops

This page explains how a Prime Ants colony grows in version 0.2.0: stages, chambers, mining and surface building. It
is written for contributors and for players who want to know what goes on underground; for playing tips, see the
[player guide](player-guide.md). Times are game ticks at default speed (20 per second, 24,000 per in-game day).
"Loaded ticks" count only while the nest's chunks are loaded.

## Ground rules

- **Everything is physical.** A colony has exactly the ants, blocks and items in the world. Soil, clay and stone move
  one unit at a time in a worker's mandibles; nothing is created, credited or teleported.
- **Natural ground only.** Colonies work only blocks recorded as natural when their chunk was first generated: soil
  (dirt, grass, coarse dirt, rooted dirt, podzol, mycelium) near the surface, and minable blocks down to 16 blocks
  below it. Chunks generated before the mod was installed have no such records.
- **Any change revokes.** A later write to a block, by a player, a piston or anything else, even putting the same
  block back, clears its natural record and the colony's claim to it (grass spreading or dying back is the exception).
  Colonies never dig, convert, cover or build over a block that is neither natural nor their own.
- **Loaded only.** Digging, upgrading, mining and building happen only while the nest is loaded; for unloaded time see
  [catch-up](catch-up.md). An unloaded block is unknown: it pauses work but never counts as damage, while damage seen
  in loaded blocks always counts.

## Stages

A queen founds a colony by digging a stair and a two-high chamber into natural soil (24 blocks), closing the entrance
behind her and raising her first workers from her body reserve. This founding chamber holds the nursery (the brood
pile) and, after the first forager's delivery, the food store (the food cache).

Requirements are cumulative: each stage also needs everything below it. Founding is the floor.

| Stage | Requirements | Adult cap |
|---|---|---|
| Founding | the queen has settled in her chamber and laid her first clutch | 5 |
| Young | 5 adults (the queen and 4 workers), a nursery and a food store | 30 |
| Mature | 25 adults, a queen's hall, a material store, 4 food units and 16 clay units | 60 |
| Great | 50 adults, all four chamber functions at tier two or higher, and 32 stone units | 120 |

### What counts

- **Adults** are living bodies, the queen included. A worker in an unloaded chunk is unknown, not dead; only a
  recorded death removes an adult.
- **A chamber function** counts while its chamber is open, enclosed and undamaged and holds the colony's own marker:
  the brood pile (nursery), the food cache (food store) or the material store block. The queen's hall has no block; it
  counts while the living queen is settled in her chamber.
- **Stocks.** Food is what the food cache holds. Clay is clay balls in the material store, clay in the colony's own
  packed-clay walls (also under stone facing) and a clay ball on its way to a wall. Stone is cobblestone or stone in
  the store, the colony's own stone facing and a stone on its way to a wall.

A colony moves up only on confirmed state, and moves down only when even its unknown parts could not meet a
requirement. The stage is evaluated after each birth or death and otherwise every 100 loaded ticks. There is no
in-game display yet; the server log records each change as `Colony stage queen=... from=young to=mature ...`.

### Caps

Caps count living adults including the queen, and brood reserves room: the queen stops laying when workers plus brood
reach the cap minus one, and a new adult waits to emerge while the colony is at its cap. A lower cap never removes
adults. Each threshold fits under the cap of the stage below (5, 25 and 50 adults under 5, 30 and 60), so a colony
can always grow back. Server operators can lower all caps with the JVM option `-Dprime_ants.colonyAdultCapacity=N`
(4 to 120, default 120): a colony uses the smaller of its stage cap and N, and each queen keeps the value that was in
force when she was created.

### Moving down

Promotion is immediate. Whether a drop is immediate depends on what was lost:

| Loss | Effect |
|---|---|
| The queen is seen dead | Founding with cap 5 at the next evaluation, whatever else remains; no promotion afterwards |
| A chamber function is seen missing: a hole in the nest shell, a missing or foreign marker, the hall without its living queen | immediate drop to the stage below the first one that needs that function |
| Adults certainly below the threshold of the stage below the current one | immediate drop to the stage the adults support |
| Food, clay or stone below a threshold, a chamber below the tier a stage needs, or adults below the current stage's threshold only | the stage holds for 24,000 loaded ticks, then drops if the shortfall is still there |

Each waiting shortfall has its own clock, saved with the colony and cleared as soon as the shortfall ends. Unknown
requirements never cause a drop and run no clock.

The founding chamber, widening, dug chambers and gallery form one connected nest. Damage anywhere in it leaves every
chamber unconfirmed, so the colony falls to Founding and brood care pauses until the damage is gone. Any solid block
closes a hole in the shell; a block placed inside a dug cell is worse (see [Known limitations](#known-limitations)).

## Who does the work

- **One builder** per colony does all construction, one job at a time: digging, upgrading, mining and surface
  building. A worker is only taken for it if at least two caregivers stay with the brood.
- **Foragers:** one per ten living workers, at least one (two from 20 workers, five at 60 adults, eleven at 120).
- **Order of building work:** (1) the first widening of the founding chamber; (2) new chambers, in Young colonies;
  (3) chamber upgrades from Mature on, whenever the store holds the unit they need, and an unfinished tier-two upgrade
  keeps its place even while it waits for clay; (4) mining; (5) surface building, once nothing above is left,
  including an unfinished gallery.

## Chambers

| Function | Where | Marker |
|---|---|---|
| Nursery | founding chamber | brood pile |
| Food store | founding chamber | nest food cache |
| Material store | a 3 × 3 room off a passage from the founding chamber | material store block |
| Queen's hall | a 3 × 2 extension of the founding chamber | the living queen |

Rooms are two blocks high, so players can walk in. The food cache and the material store show what they hold.

- **Widening.** Once four adult workers live in the colony, one of them widens the founding chamber by 12 blocks on
  one side. Its soil goes on a small fixed set of cells behind the entrance.
- **Material store.** A Young colony that certainly has no store digs one: a passage through the back wall of the
  founding chamber and a 3 × 3 room beside it, 24 blocks, in one of two mirror-image placements. A placement is used
  only if every block to dig and every wall block around it is natural soil (stone may lie under the floor), nothing
  is fluid and the mound has room for all the soil. The digger then sets the material store block on the floor.
- **Queen's hall.** Once the store is confirmed, the colony digs the hall, 12 blocks on the side of the founding
  chamber that the widening did not take, so the queen and her brood never move. Store and hall soil goes onto the
  stage mound, and each chamber is dug only once.

### Tiers and upgrades

Tier one is the earth a chamber was dug into. Mature unlocks tier two, packed clay, and Great unlocks tier three,
nest-cut stone facing over the packed clay. A chamber's walls are the side faces of its cells that no nest plan will
ever open (floor and roof do not count): 8 cells for the founding chamber, 22 for the material store and 14 for the
queen's hall, or 10 if it shares cells with an already upgraded store. A full set of upgrades takes 40 or 44 units
per tier.

The builder upgrades the founding chamber (one job for nursery and food store), then the material store, then the
hall, all three to tier two before any to tier three. For each wall cell it takes one unit from the material store,
carries it to the wall and builds it in:

- **Tier two:** a clay ball, rammed into the cell's own earth, which becomes packed clay. No soil leaves the nest.
- **Tier three:** a stone item (`minecraft:stone`, as from smelting cobblestone; cobblestone is not accepted) and 20
  ticks of work. The cell becomes nest-cut stone; its clay core stays inside and still counts as clay.

Shared cells are converted once and cells already at the tier are skipped. If the store has no suitable unit, the
builder goes back to the colony and the job resumes when one arrives. A wall cell that is no longer the colony's own
earth (tier two) or its own packed clay (tier three), for example because a player changed it, is never converted:
the job stops for good, the carried unit goes back to the store, and no later upgrade starts.

A chamber's tier is the lowest tier among its walls, read from the blocks: any solid block is tier one, the colony's
own packed clay tier two and its own nest-cut stone tier three. A block placed by a player never raises a tier.
Resin masonry also counts as tier two, but no current species builds it.

### What each tier changes

| Function | Tier 1 | Tier 2 | Tier 3 |
|---|---|---|---|
| Nursery | 3 brood slots, 4 more with a confirmed queen's hall | 3 more slots, brood develops 1.5 times as fast | 8 more slots, twice as fast |
| Food store | 6 units, at most 4 of one kind | 12 units, at most 8 of one kind | 18 units, at most 12 of one kind |
| Material store | 32 units, at most 16 that are not clay | 64 units, at most 32 not clay | 96 units, at most 48 not clay |
| Queen's hall | at most one egg per 1,200 ticks | per 600 ticks | per 300 ticks |

Food kinds are sugar (apples, sweet berries, nectar) and protein (raw chicken, rotten flesh, prey). The hall only sets
the fastest rate; laying still needs food, free brood slots and room under the cap. At default timing a nursery keeps
about 12 workers going at tier one, 28 with the hall, 60 at tier two and 120 at tier three, enough for each next
stage. Effects need a confirmed chamber, and a store never loses contents when its tier drops.

## Mining

Young colonies can mine clay and necessary permitted soil access with their one builder once the material store is
confirmed. General mineral production still requires Mature or Great. Digging and upgrades with available recipe
units keep priority; a tier-two clay gap allows the supplying miner to resume. Active builders keep their claims
and cargo, and at least two caregivers must remain.

**What.** Only blocks recorded as natural when the chunk was generated, down to 16 blocks below the surface. Each
block gives exactly one item, with no vanilla loot or Fortune: stone gives cobblestone, clay a clay ball, coal ore
coal, copper ore raw copper and iron ore raw iron; gravel and sand give themselves. Natural soil in the way goes onto
the mound. Cobblestone counts toward Great's stone; gravel, sand and ore are only stockpiled, ore for a later forge.

**Where.** The hard limits are a 16-block horizontal circle around the entrance, 1 to 6 blocks below it and 64
cumulative successful mining edits per colony, including every soil connector and branch. The original general
mineral gallery remains the two-high line 8 to 15 blocks from the entrance, at most 16 blocks. Clay routes can extend
through permitted soil with supported one-block steps or use a physical bank opening. All branches share one saved
edit ledger; retries and readback do not reset it. Chamber, wall and mound support columns remain protected.

**How.** The planner checks every 100 ticks. The miner walks to a supported exposed face, works for 20 ticks, checks
everything again, removes one block and carries its unit to the store. Bank hauls use the existing entrance stairs.
A block is only removed if it is still the recorded natural block, its neighbours are loaded and dry, the
gallery's walls, floor and roof are intact, no sand or gravel rests on it, vanilla support survives and the store (or
the mound) has room for the result. Otherwise the miner waits.

**Player contributions.** Foragers also bring in clay balls, cobblestone, stone, gravel, sand, coal, raw copper and
raw iron that players drop within about 24 blocks of the entrance: one item per trip, only on a trip that found no
food, and only while the store has room for that material. Delivered items count toward the stages like mined ones.

**When things go wrong.** A miner that dies drops its item, which never despawns; a forager can store it like any
dropped material, and another worker takes over the gallery. Below Mature no more blocks are removed, though a
carried item is still delivered. Unloaded parts of the nest pause the work. While the nest is damaged, brood care
pauses, the miner keeps the item it carries until the damage is repaired, and no new worker is taken off care. If
anything changes the gallery's next block, even a player putting the same block back, the gallery ends for good.

## Surface

### The stage mound

Soil from the material store, the queen's hall and the gallery goes on the mound plan of the colony's current stage:
two lobes behind the entrance, one on each side of the approach lane, which stays clear along with the stairs.

| Stage | Mound plan |
|---|---|
| Founding | none: the queen's soil and the first widening go to a small fixed set of cells behind the entrance |
| Young | two lobes up to 3 layers high, 15 × 8 columns, room for 214 blocks |
| Mature and Great | two lobes up to 5 layers high, 23 × 13 columns, room for 798 blocks, around the Young mound |

The plan is room for soil, not a shape to fill: workers only lay soil they dug, so a real mound covers a small part of
it. Each column rests on its own ground, the highest natural soil or stone within two blocks of the entrance level. A
cell takes soil only if it is air or a short plant (buried without drops) on natural ground or the colony's own soil,
and only if vanilla support allows it. Fluids, player blocks, block entities, logs, leaves, crops, sand, snow and
anything else are never covered or removed; the column stops below them. Mound soil stays when a colony drops a stage.

### Arch, crest and fortifications

Mature and Great colonies build fixed plans of nest soil, 57 blocks in all:

- **Mature (19 blocks):** an open arch one block out from the entrance, made of a three-high pillar on each side of
  the approach lane joined by a lintel, so the lane passes under a two-high opening; a one-block step on each side of
  the lane just outside it; and one crest four blocks high on one side of the mound, with steps of three, two and one
  blocks behind it.
- **Great (38 more):** along the line of the arch, a gate (the bottom block of each pillar is turned into mound gate
  masonry in place, keeping its soil), ramparts two blocks high, two columns on each side of the gate, and a watch
  post five blocks high at each end, with a stepped ramp of four, three, two and one blocks running back from it.

One worker takes an exposed block of the colony's mound soil (never one in a column the plans use), carries it to the
next cell and places it; taking and placing are each a 20-tick action with the cell in reach and in sight. Cells go in
a fixed order, lowest layer first and the lintel last. A cell that already holds the colony's soil is confirmed in
place, and the gate blocks are converted the same way. With the soil already on the mound and nothing else for the
builder to do, all 57 blocks take about 11,000 ticks, a little over nine minutes of loaded play.

A plan is laid out only where every column has natural ground within two blocks of the entrance level and the three
arch columns stand at the same height. At that moment only cells holding air or a short plant become building sites,
and player blocks are never converted:

- A building site that is changed later, even by placing the same block there, is withdrawn. The builder waits at
  that cell, and because the order is fixed, nothing after it gets built.
- A finished structure block that a player breaks or replaces is not rebuilt, and that plan stops.
- If the colony drops below a plan's stage, building stops; finished blocks stay.
- A builder that dies, or cannot reach its spot within about a minute, drops the soil it carries as an item, and that
  soil is not used again.

## Founder access

In a colony that built its own Young mound, the base of the Mature crest can end up buried inside it. The cell
already holds the colony's soil and only needs confirming, but no worker can reach it, so surface building would stop
after four cells. For this case only, the colony may move exactly two blocks of its own mound soil. In plan
coordinates (forward, side, layer from the entrance) it moves (-4, 2, 1) to (-9, 8, 0), then (-4, 2, 0) to
(-10, 7, 0), uncovering the buried cell at (-4, 3, 0); each removal and placement is a 20-tick action.

The move is only offered to a Mature or Great colony building its Mature plan with exactly the first four cells done,
on ground level with the entrance, with no tier-two upgrade left to do, both blocks and both destinations as expected,
and exactly 72 blocks of mound and surface soil. Everything is checked again before each step, and there is no third
move or other clearing. A player change to any cell involved, a drop below Mature, the queen's death or new digging
ends the attempt for good, dropping a carried block as an item. If a carrier dies, another worker may pick up its
block once; a second death ends the attempt. Unloaded cells only pause it.

## Established colonies

New terrain also holds colonies that are already Young or Mature.

**Where.** In newly generated Overworld chunks in plains, sunflower plains, meadows and flower forests. One chunk in
every 4 × 4 group of chunks, picked by the world seed, is a candidate site. Once it and its neighbours are fully
loaded, up to 64 spots inside it are tried until one fits; spots within 56 blocks of another living queen are
rejected. The seed and the position fix the kind of site: half are founding queens, a quarter Young nests and a
quarter Mature nests. A Young or Mature site that fits nowhere stays empty rather than becoming a founding queen.
Natural colonies can be switched off with the JVM option `-Dprime_ants.naturalPlacement=false`.

**Fit.** The whole footprint has to fit as it is: every cell to open and every wall around it must be natural soil
(stone only under the floor), the surface cells must be air or short plants on natural soil, and no fluids, block
entities or entities may be in the way. Nothing is flattened, nothing outside the plan is touched and no extra chunks
are loaded. If the operator's adult cap is too low for the starting ants, that kind of nest is not placed.

| | Young | Mature |
|---|---|---|
| Dug space | stairs and founding chamber, 24 blocks | also the widening, the material store with its passage and the queen's hall, 72 blocks |
| Surface | 8 soil blocks behind the entrance | also the complete Mature arch and crest, 27 blocks |
| Ants | queen and 4 workers | queen and 24 workers |
| Stock | 4 apples and 2 raw chicken in the food cache | the same, plus 16 clay balls in the material store |

Workers start as adults, the brood pile starts empty and the queen starts with a full sugar store. The nest counts as
the colony's own, though not as work the colony did, and player changes revoke it like any other colony block. The
stage is not written in: the first evaluation finds Young or Mature from the real adults, chambers and stock, and the
ants stay still until that check passes. An interrupted installation is never retried and missing ants are never
replaced. Afterwards a Young nest digs its own widening, store and hall, and a Mature nest upgrades its chambers.

## Persistence

Mining jobs saved by 0.2.0 keep the original gallery positions and order, claims, deliveries, releases and cargo
accounting. New clay routes save their target positions and existing bank access explicitly. Completed edits remain
in the same cumulative ledger across branches and normal save readback.

A colony is saved with the world: the ants (age, food, task and the item each carries), the nest blocks with their
contents, and world saved data holding chambers, stage and stage clocks, dig, upgrade, mining and surface jobs with
their progress and claims, which blocks belong to the colony, the natural-terrain records, the ledger of natural sites
and items from dead workers still waiting to be placed in the world. Saved records are only claims: a saved stage or
chamber never overrides what the blocks show. Saving relies on normal world saves; recovery from a crash or power
loss in the middle of a save is not guaranteed. For unloaded colonies, see [catch-up](catch-up.md).

**Old saves.** Colonies from 0.1.0 load and keep growing: their founding chamber counts as a tier-one nursery and food
store, and a saved adult cap of 30 (the old default) becomes 120, while a deliberately lower value is kept. Natural
sites still pending from 0.1.0 produce founding queens, not established nests. Mining uses records taken when a chunk
is first generated by 0.2.0 or later, so terrain generated by 0.1.0 has nothing to mine.

## Known limitations

- **Ordinary-world clay progression remains unqualified.** Young-to-Mature growth and all tier-two functions pass
  with controlled geology and finite food. The frozen ordinary Overworld census proves generation-observed clay
  exists, but its declared potential layout has 31 initially dry units in bounds for 40 distinct wall payments.
  A complete ordinary-world colony run has not been demonstrated. These food-assisted tests establish independence
  from material gifts within their fixture; they do not establish complete food autonomy.
- **Promotion stock is reused.** Tier two takes 40 or 44 distinct wall payments in total. The first 16 clay units
  remain in stock or owned walls, leaving 24 or 28 more; nursery and food store share one chamber.
- **Tier three needs stone from players.** Facing takes stone items, and mining only yields cobblestone.
- **Mining remains bounded.** All clay access shares 64 edits. Young access cannot cross natural stone or fluids.
  General mineral production retains its original short gallery and may find nothing useful.
- **No repairs.** A wall that a player replaced counts as tier one and is not upgraded again, a changed structure
  block stops its surface plan, and a changed gallery block ends mining.
- **Lighting preserves existing openings.** Supported dry torches, wall torches, soul variants and standing or hanging
  lanterns preserve the colony's worker-dug or generated openings. The original saved ownership strings stay intact
  through placement, saving and removal. Integrity checks accept these lights; movement, emergence and circulation
  still use physical collision. A lantern needs an alternate route. Lighting grants no excavation or replacement
  permission and never repairs a broken shell, foreign component, wet cell or unavailable terrain.
  If a light's support is unavailable, its retained opening stays unknown, including a worker widening through a
  generated nest's original shell. This uncertainty holds the stage; an independently observed broken wall still
  counts as damage. Restoring support knowledge needs no new excavation or light placement.
  The queen's original excavation has no revocable opening records; its existing collision-free torch behaviour stays
  unchanged. Lantern support applies to worker-dug additions and generated rooms, without inventing authority there.
- **Revoked openings stay revoked.** Solid player blocks and other replacements still remove ownership. Neither
  removing a block nor placing a light restores it. In 0.2.0 even lighting removed the ownership record. Completed
  dig lists and generated plans survive both lighting and deliberate player edits, so they cannot safely justify
  recovery. Previously saved lighting damage has no automatic repair. The save format remains unchanged.
- **Surface plans are rigid.** They need level natural ground by the entrance and follow a fixed order, so one
  blocked cell stops everything after it.
- **Loaded only.** Nothing is built while the nest is unloaded, and a nest across a chunk border pauses whenever part
  of it is unloaded.
- **Large colonies are expensive.** A Great colony of 120 adults is heavy for the server.
