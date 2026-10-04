# Prime Ants (`formic_frontier`) — Extraction 01: Colony simulation core

Scope: castes, ants (entity, AI, model, textures), jobs, resources and economy, instincts, progression (ranks and stages), research, cultures, personalities, identity labels, guide chapters, persistence (`ColonyData`), and the sim unit tests.

Source: repo `C:\Users\user\Documents\Codex\2026-04-26\new-chat`, branch `rebuild/anthills-from-scratch`, HEAD `75a70f7` ("Redesign ant castes and Formic equipment art", 2026-07-13). Read-only; nothing was built or run. §6.3 uses numbers from my own Python re-implementation of the sim passes, not from running the mod.

## 0. Conventions

**Path abbreviations** (all relative to the repo root):

| Short | Full path |
|---|---|
| `sim/X.java` | `src/main/java/com/formicfrontier/sim/X.java` |
| `world/X.java` | `src/main/java/com/formicfrontier/world/X.java` |
| `entity/AntEntity.java` | `src/main/java/com/formicfrontier/entity/AntEntity.java` |
| `render/X.java` | `src/client/java/com/formicfrontier/client/render/X.java` |
| `screen/ColonyStatusScreen.java` | `src/client/java/com/formicfrontier/client/screen/ColonyStatusScreen.java` |
| `network/ColonyUiSnapshot.java` | `src/main/java/com/formicfrontier/network/ColonyUiSnapshot.java` |
| `test/ColonyEconomyTest.java` | `src/test/java/com/formicfrontier/sim/ColonyEconomyTest.java` |
| `en_us.json` / `ru_ru.json` | `src/main/resources/assets/formic_frontier/lang/en_us.json` / `ru_ru.json` (both files use identical line numbers) |

Any other path is given in full.

**Status tags:** **IMPLEMENTED** means it is wired into normal play and does what it says. **PARTIAL** means it is wired but incomplete or inconsistent. **COSMETIC** means display only, with no gameplay effect. **DEAD** means unreachable or unused in normal play.

**Time units.** One "economy tick" (**ET**) is one run of `ColonySavedState.tickEconomy()` followed by `tickWorld()`. It fires every 20 server ticks, which is **1 real second at 20 TPS** (`sim/ColonyEconomy.java:4`, `src/main/java/com/formicfrontier/FormicFrontier.java:34-47`). `ColonyData.ageTicks` grows by 20 per ET (`sim/ColonyEconomy.java:10`), so it counts game ticks. Most other counters (`raidCooldown`, building `disabledTicks`, stage "age") move once per ET, so their units are really seconds. Every per-tick amount in this document is per ET, i.e. per second.

---

## 1. How the simulation runs (architecture)

### 1.1 Tick loop and order — IMPLEMENTED

`FormicFrontier.onInitialize` registers an `END_SERVER_TICK` handler. It counts to 20, then calls `tickEconomy()` and `tickWorld(overworld)` on the single overworld `ColonySavedState` (`src/main/java/com/formicfrontier/FormicFrontier.java:34-47`).

`ColonySavedState.tickEconomy()` (`world/ColonySavedState.java:97-140`) runs these passes for each colony, in insertion order:

| # | Pass | What it does (details in later sections) |
|---|---|---|
| 1 | `ColonyEconomy.tick` | age +20; income per resource; food upkeep; grows at most **one** ant |
| 2 | `ColonyLogistics.tick` | auto-delivers "non-player" requests (none exist in live play, §4.7); advances research, or produces knowledge when idle |
| 3 | `CasteJobLoop.tick` | abstract "gather / build / patrol / tend" |
| 4 | `ColonyStageProgression.tick` | founding → growth → established → mature |
| 5 | `CasteBalancer.tick` | converts surplus castes toward a starving need |
| 6 | `NativeBlockRole.tick` | each fungus garden turns 5 food into 8 fungus |

After all colonies:

- `DiplomacyService.tick` sends one autonomous ENVOY toward the nearest non-allied colony (`world/ColonySavedState.java:123-128`).
- `TradeCaravan.exchange` sends one caravan toward the nearest non-hostile colony (`:133-138`).

`tickWorld()` (`world/ColonySavedState.java:180-191`) then runs:

- `ColonyDiscoveryService.tick`
- per colony: `ColonyBuilder.tick` (construction, upgrades, repair) and `ColonyRecurringEvents.tick`
- `RaidPlanner.tick`

The debug command `/formic colony tick <n>` runs `n/20` ETs back to back (`src/main/java/com/formicfrontier/command/FormicCommands.java:103-113`).

The sim is pure data. Every colony ticks every second whether or not a player or loaded chunk is nearby, and all colonies (player, rival, wild) run the same economy.

### 1.2 Two layers: abstract simulation vs in-world ants

- **Abstract layer (authoritative).** `ColonyData` holds integer caste counts, resource stockpiles, buildings, queue, requests and research. All economy, growth, rank, stage, research and raid logic runs on these numbers.
- **In-world layer (mostly cosmetic).** `AntEntity` mobs, each tagged with a caste and a colony id.
  - Seven are spawned when a colony is founded (`world/ColonyService.java:905-917`).
  - A few more come from events, raids, diplomacy consequences and debug commands (list in §2.4).
  - **Entities are never spawned when the abstract count grows, and entity deaths never reduce the counts.** No death hook exists; grep for `die`/`onDeath` finds nothing.
  - Entities do run a work AI that walks to resource nodes and buildings. Each trip adds a small free amount to the colony stockpile or construction progress (§3.2).

### 1.3 Persistence

`ColonySavedState` is a `SavedData` named `formic_frontier_colonies`, stored in overworld data storage as a list of `ColonyData` (`world/ColonySavedState.java:27-33, 48-54`). `nextId` is not saved; it is recomputed as max id + 1 (`:41-46`). The full field list is in §12.

---

## 2. Castes

### 2.1 `AntCaste` enum — IMPLEMENTED (`sim/AntCaste.java:8-14`)

Constructor order is `(id, height, health, speed, damage, foodUpkeep, foodCost, oreCost, chitinCost)`.

| Constant | id | Height (blocks) | Width = clamp(h·0.62, 0.85..1.65) | Render scale | Max HP | Move-speed attr | Attack dmg | Food upkeep/ET | Growth cost F / O / C |
|---|---|---|---|---|---|---|---|---|---|
| WORKER | `worker` | 1.5 | 0.93 | 1.08 | 18 | 0.28 | 2 | 1 | 6 / 0 / 0 |
| SCOUT | `scout` | 1.55 | 0.961 | 1.14 | 14 | 0.34 | 1 | 1 | 4 / 1 / 0 |
| MINER | `miner` | 1.7 | 1.054 | 1.18 | 22 | 0.24 | 3 | 2 | 8 / 4 / 0 |
| SOLDIER | `soldier` | 1.8 | 1.116 | 1.2 | 30 | 0.26 | 5 | 3 | 10 / 2 / 3 |
| MAJOR | `major` | 2.25 | 1.395 | 1.5 | 48 | 0.22 | 8 | 6 | 24 / 8 / 10 |
| GIANT | `giant` | 3.0 | 1.65 (clamped) | 2.0 | 90 | 0.18 | 14 | 24 | 180 / 45 / 55 |
| QUEEN | `queen` | 2.4 | 1.488 | 1.6 | 140 | 0.08 | 2 | 12 | 0 / 0 / 0 (never grown) |

How the columns are derived and used:

- **Width:** `sim/AntCaste.java:48-50`.
- **Render scale:** hardcoded 1.08 / 1.14 / 1.18 for WORKER / SCOUT / MINER; `height/1.5` for the rest (`sim/AntCaste.java:52-59`). A unit test pins this (§13).
- **HP / speed / damage** only feed the Minecraft entity attributes (`entity/AntEntity.java:190-203`). The abstract sim never uses them; raids use fixed weights instead (§2.3).
- **Growth:** `canGrowFrom` checks food/ore/chitin stock against cost; `consumeGrowthCost` subtracts it (`sim/AntCaste.java:89-99`).
- **Upkeep total:** `ColonyData.upkeepPerEconomyTick()` = Σ foodUpkeep × count (`sim/ColonyData.java:109-115`).

Names (`en_us.json:75-81` / `ru_ru.json:75-81`). The Russian labels are plural because they label population counters.

| id | EN | RU |
|---|---|---|
| worker | Worker | Рабочие |
| scout | Scout | Разведчики |
| miner | Miner | Шахтеры |
| soldier | Soldier | Солдаты |
| major | Major | Майоры |
| giant | Giant | Гиганты |
| queen | Queen | Матка |

- Entity type name: "Formic Ant" / "Муравей Formic" (`en_us.json:51`).
- Named entities (`en_us.json:52-54`): "Queen #%s" / "Матка #%s"; "%s Guard #%s" / "Страж %s #%s"; "Worker Foreman #%s" / "Старший рабочий #%s".
- UI accent colours (`network/ColonyUiSnapshot.java:547-557`): QUEEN 0xF0C26E, GIANT 0xD06B5D, MAJOR 0xD99555, SOLDIER 0xC15D48, MINER 0xA9A9A9, SCOUT 0x8EC8D6, WORKER 0xD8B57A.

### 2.2 What each caste does

| Caste | Abstract economy (per ET) | Abstract job loop (per ET) | In-world entity job | Raid weight |
|---|---|---|---|---|
| WORKER | +3 food, +1 chitin per 2 workers, +1 resin per 3 workers (`sim/ColonyEconomy.java:32,34,38`); drives construction and repair speed (`world/ColonyBuilder.java:69-70,107-108`) and logistics throughput (`sim/ColonyLogistics.java:126`) | +1 food each; build +min(workers,10)%; tend queen +min(workers,5) HP | logistics trips, construction trips, food / chitin / resin / fungus / venom trips | 0 |
| SCOUT | +1 food | +1 chitin each | forages food to the queen chamber | 0 |
| MINER | +2 ore | +2 ore each | hauls ore from the ore node to the mine | 0 |
| SOLDIER | +soldiers/3 venom with VENOM_DRILLS (`sim/ColonyEconomy.java:45-47`) | counts as a defender for patrol | patrol | 4 |
| MAJOR | – | defender for patrol | patrol | 8 |
| GIANT | – (only producible after MANDIBLE_PLATING research) | – (not counted as a patrol defender) | patrol | 16 |
| QUEEN | – (colony "alive" gate) | – | none (the work goal refuses the queen) | – |

- Raid weights come from `world/RaidPlanner.java:422-431`.
- Net food per ant per ET: worker +3 (3 + 1 − 1), scout 0 (+1 chitin), miner −2, soldier −3, major −6, giant −24, queen −12.
- The patrol pass counts SOLDIER + MAJOR only (`sim/CasteJobLoop.java:35`); GIANT is left out, which looks like an oversight.

### 2.3 Combat relevance

- **Entity combat — IMPLEMENTED.** Every caste has `MeleeAttackGoal` and targets `AntEntity`s of hostile colonies (`entity/AntEntity.java:68-76`). Follow range is 24 (`:64`). Entity fights never change colony data.
- **Abstract raids** (raid rules belong to extraction 03; listed here because they involve castes):
  - Military strength = 4·soldiers + 8·majors + 16·giants + armory weapon attack.
  - Defense rating = 6·watch posts + 4·barracks + strength/3 + armor defense (`world/RaidPlanner.java:422-444`).
  - Casualties remove SOLDIER first, then MAJOR, then GIANT, capped at half the combat pool (`world/RaidPlanner.java:215-242`).
  - A raid needs attacker strength ≥ 16 and sets a raid cooldown of 600 (`:36-41`).
- **`ColonyArmory`** (research-driven loadout; §7.3):
  - Each completed ARMORY arms 4 combat ants.
  - MANDIBLE_PLATING gives the mandible saber (+5 attack per armed ant); otherwise VENOM_DRILLS gives the venom spear (+4).
  - A CHITIN_FARM supplies chitin armor (+2 defense per armed ant); RESIN_MASONRY adds +2 more (`sim/ColonyArmory.java:43-53, 63-106`).

### 2.4 How ants are produced (queen, eggs, nursery?)

There are **no eggs, larvae or brood stages**. The "Queen Egg" item founds a whole colony; it does not hatch ants.

1. **Founding — IMPLEMENTED.** Using a Queen Egg on a block calls `ColonyService.createColony(level, clickedPos.above())` (`src/main/java/com/formicfrontier/item/QueenEggItem.java:17-36`). The colony is always player-allied and AMBER culture (`sim/ColonyProgress.java:68-70`). `seedEconomy` (`world/ColonyService.java:639-653`) sets:
   - Castes: QUEEN 1, WORKER 3 + workerBias, SCOUT 1 + scoutBias, MINER 2, SOLDIER 2, MAJOR 1. For AMBER that is a population of 10.
   - Resources: food 120, ore 20, chitin 24, resin 24, fungus 12 (28 for LEAFCUTTER), venom 4 (16 for FIRE), knowledge 8.
   - Rival colonies (`/formic colony seed-rivals`) and discovered wild colonies go through the same `createColony` path with other cultures (`world/ColonyService.java:55-63`).
2. **Starter entities — IMPLEMENTED, but they don't match the counts.** `spawnStarterCastes` spawns exactly 7 mobs around origin + (0,1,−11) (`world/ColonyService.java:905-917`):
   - QUEEN (named), WORKER ×3 (the first is named "Worker Foreman"), MINER ×1, SOLDIER ×1, MAJOR ×1 (named "Guard").
   - No scout entity is spawned, although 1–2 scouts are counted. Only 1 miner and 1 soldier entity exist although 2 of each are counted. LEAFCUTTER's extra worker and scout are never spawned.
3. **Growth loop — IMPLEMENTED; the only regular source of new ants** (`sim/ColonyEconomy.java:73-80, 85-150`).
   - Runs every ET if the queen is alive. It picks **at most one** caste, pays its food/ore/chitin cost from stock and adds +1 to the count. No entity is spawned.
   - The queen has no egg-laying rate and the NURSERY is not required. The only growth limits are resources and the target numbers below.
   - Decision order: first the per-instinct rules, in the colony's instinct order; then the fallback list.

   | Instinct | Rule (first match wins; each also needs `canGrowFrom`) |
   |---|---|
   | FOOD | WORKER if workers < 8 + culture.workerBias; else SCOUT if scouts < 3 + culture.scoutBias |
   | ORE | MINER if miners < 8 |
   | CHITIN | MAJOR if majors < 2; else WORKER if workers < 6 |
   | DEFENSE | SOLDIER if soldiers < 3 + 2·(completed barracks); else MAJOR if majors < 4; else GIANT if food > 250 and MANDIBLE_PLATING is researched |
   | fallback | WORKER if < 3 → MINER if < 2 → SOLDIER if < 2 → GIANT if food > 250 and research → MAJOR if chitin > 30 (**no upper limit**) → SCOUT if < 1 → none |

   - GIANT is research-gated by `canProduceGiant()` = MANDIBLE_PLATING completed && `GIANT.canGrowFrom` (`sim/ColonyEconomy.java:165-168`).
   - **No population cap exists anywhere in growth.** Once the targets are met, the MAJOR fallback keeps adding majors while chitin > 30. Together with the balancer's miner→worker churn (§2.6), population grows by about one ant per second without end (§6.3).
4. **Queen brood bloom event — IMPLEMENTED** (event mechanics belong to extraction 03).
   - Effect: +2 WORKER, −45 food, −12 chitin, and up to 2 worker entities are spawned, one per spawn group if an open spot is found (`world/ColonyRecurringEvents.java:183-196, 449-453, 758-766`). This is the only place where new entities match a count change.
   - Conditions: queen alive; population < 42; food ≥ 45 + 4·upkeep; chitin ≥ 28; NURSERY completed (`:223-229`).
   - Player-allied colonies only (`:53-55`). It is the only mechanic tying growth to the NURSERY.
5. **Role conversion.** `CasteBalancer` (§2.6) moves ants between castes without births.
6. **Other entity spawns, none of which change counts:**
   - event crews for expansion, migration, treaty, trade caravans and invasion guards (`world/ColonyRecurringEvents.java:768-891`);
   - 3 SOLDIER entities per raid, tagged with the attacker's colony id and spawned at the defender's origin (`world/RaidPlanner.java:130-132`). Textures are per caste, not per colony, so they look like any other soldier;
   - diplomacy consequences (`world/DiplomacyConsequences.java:302,392,417`);
   - `/formic ant spawn <caste>` (`src/main/java/com/formicfrontier/command/FormicCommands.java:136-141`).
7. **Losses.** Only raids reduce counts (`world/RaidPlanner.java:237-254`). **Starvation never kills ants:** food is clamped at 0 (`sim/ColonyData.java:85-87`), upkeep just stops being paid, and growth stalls.

### 2.5 The queen — PARTIAL

- **Counting.** Exactly one QUEEN is counted from founding (`world/ColonyService.java:647`). Nothing ever grows another.
  - `queenHealth` starts at 140 (`sim/ColonyData.java:54`).
  - `queenAlive()` = `queenHealth > 0 && queen count > 0` (`:117-119`).
- **Damage** only comes from raids: max(0, attackStrength/30 − defenseRating/10), reduced by 3 with a completed Queen Vault (`world/RaidPlanner.java:151-153, 193, 405-410`).
- **Healing:**
  - CasteJobLoop "tend": +min(workers, 5) per ET, up to 140, but only while alive (`sim/CasteJobLoop.java:100-109`).
  - Royal Jelly used on the queen entity heals the entity by 24 and sets colony `queenHealth` = max(colony value, entity HP). It also gives +6 reputation (`world/ColonyService.java:330-340`).
- **When `queenHealth` hits 0:**
  - The economy still produces income and charges upkeep, but growth stops; the current task shows "Queen lost: growth suspended" (`sim/ColonyEconomy.java:68-71`).
  - The balancer, stage progression, tend and every recurring event freeze.
  - The only recovery is Royal Jelly on a still-living queen entity, which revives the colony through the max() rule above. The stage-progression comment says "Progression resumes once she recovers"; no other recovery path exists.
- **The queen entity is decoupled from `queenHealth`.** Killing it changes nothing; damaging it changes nothing. It has 140 HP and speed 0.08, never works, and can still melee hostile ants.

### 2.6 `CasteBalancer` — IMPLEMENTED (`sim/CasteBalancer.java`)

Constants:

| Constant | Value | Line |
|---|---|---|
| `REASSIGN_CAP_PER_PASS` | 2 | `:38` |
| `FOOD_STARVATION_UPKEEP_MULTIPLE` | 2 | `:40` |
| `RESOURCE_STARVATION_THRESHOLD` | 20 | `:42` |
| `DEFENSE_STARVATION_THRESHOLD` | 2 | `:44` |
| `SURPLUS_HEADROOM` | 2 | `:46` |
| `BALANCER_TICK_INTERVAL` | = ET, unused constant | `:35` |

Rules:

- Does nothing if the queen is dead (`:55-57`).
- Walks the instinct list; stops after 2 reassignments in one pass (`:58-80`).
- Need detection (`:87-102`):
  - FOOD: food < 2·upkeep → target WORKER
  - ORE: ore < 20 → target MINER
  - CHITIN: chitin < 20 → target WORKER
  - DEFENSE: soldiers + majors < 2 → target SOLDIER
- Source caste (`:111-117`): a WORKER target takes from MINER; a MINER or SOLDIER target takes from WORKER. The source must have more than 2, and only the count above 2 can be moved (`:119-121`).
- Side effects: current task "Reassigned N a to b" (`:79`); event "Colony rebalanced castes toward <need> need" (`:81-83`).

Issues:

- With a large population, upkeep·2 usually exceeds the food stock. The FOOD rule then fires every second and converts 2 miners into workers, and the growth loop immediately regrows miners (ORE rule: miners < 8). My re-implementation (§6.3) shows constant "rebalance" churn.
- When FOOD and ORE are both starving in the same pass, a miner can become a worker and then a worker a miner (net zero).

### 2.7 Visuals — IMPLEMENTED

**Registration.**
- Entity type `formic_frontier:ant`, `MobCategory.CREATURE`, default size of WORKER, client tracking range 10 (`src/main/java/com/formicfrontier/registry/ModEntities.java:15-22`).
- `getDefaultDimensions` returns the caste's width × height; `setCaste` calls `refreshDimensions()` (`entity/AntEntity.java:95-105`).
- There is no natural spawning (no biome modifications registered).
- Renderer and layer are registered in `src/client/java/com/formicfrontier/client/FormicFrontierClient.java:15-17`; layer `formic_frontier:ant#main` in `render/ModEntityModelLayers.java:8-15`.

**Renderer** (`render/AntEntityRenderer.java`):
- `MobRenderer` with base shadow 0.75 (`:21`).
- Per-entity shadow radius = max(0.35, 0.42 × render scale) (`:34`): worker 0.45, scout 0.48, miner 0.50, soldier 0.50, major 0.63, giant 0.84, queen 0.67.
- Uniform `poseStack.scale(renderScale)` (`:58-61`).
- One texture per caste (`:12-18, 38-55`): `textures/entity/ant_<caste>.png`.
- Possible issue, not verified against vanilla: `this.shadowRadius` is changed on the shared renderer after `super.extractRenderState()`. If vanilla copies the shadow radius inside that super call, each ant's shadow would use the previous ant's size.

**Model** (`render/AntEntityModel.java`). An "upright ant": one rig for all castes, with the head on top, a vertical thorax and the gaster hanging behind (doc comment `:15-21`). Texture UV space is 128×64 (`:159`); the PNGs are 256×128, i.e. 2× density. Units are 1/16 block, and Y grows downward with the ground at 24.

| Part | Box (size, from `addBox`) | Pivot / notes | Lines |
|---|---|---|---|
| head | 8×7×8 + two eye boxes 2×2×1 at the front corners (texture u=120) | pivot (0, 7.5, −2.5); follows look yaw/pitch | `:93-98` |
| crest (head child) | 8×2×7 on top of the head | hidden by default | `:99-101` |
| antenna L/R | base 1×5×1 + tip 1×6×1 | pivot (±2.1, −4.6, −3.2), rot (−0.34, ±0.18, ∓0.48); tip rot (−0.12, ±0.18, ∓0.24) | `:103-114` |
| mandible L/R | base 2×2×4 + tip 1×1×4 | pivot (±1.5, 0.2, −3.6), yaw ∓0.38; tip yaw ∓0.42 | `:116-127` |
| body (thorax) | 7×8×6 | pivot (0, 14, 0) | `:129-131` |
| plate (body child) | 8×2×7 | hidden by default | `:132-134` |
| wing L/R (body child) | 1×7×9 | pivot (±3.1, −2, 0), rot (0.12, ∓0.2, ∓0.22); hidden (queen only) | `:135-140` |
| petiole | 3×3×3 | (0, 17, 2.4) | `:142-144` |
| post-petiole | 4×4×4 | (0, 19, 3.5) | `:145-147` |
| abdomen | 9×7×8 | (0, 20, 5) | `:148-150` |
| 6 legs | upper 2×6×2 + lower 1×8×1 (knee bend ±0.25 z) | front (±3.3, 10.5, −2.1) x −0.16 z ∓0.40; middle (±3.5, 12.6, 0) z ∓0.34; back (±3.1, 14.3, 2.3) x 0.22 z ∓0.30 | `:152-171` |

**Animation** (`setupAnim`, `:173-228`):
- **Walk** (tripod-like): front = cos(pos·0.75)·0.48·speed; middle = cos(pos·0.75 + π)·0.42·speed; back = cos(pos·0.75)·0.34·speed, with alternating signs for left/right. Lower legs counter-rotate by 0.42 / 0.38 / 0.34 × the upper-leg value.
- **Idle cues:** pulse = sin(age·0.35)·0.12; antenna twitch = sin(age·0.45)·0.11 (tips 0.75×).
- Poses by `workState` (synced entity data):

| State | Pose |
|---|---|
| WORKING | head nods (+sin(age·0.9)·0.32), mandibles open (±0.14 base, ±0.12 tips), both front legs raised to −0.95 ± sin(age·0.9)·0.28, abdomen 0.1 + pulse |
| CARRYING_* | head up (−0.25), body leans −0.08, both front legs held at −1.2 (carrying pose), abdomen 0.08 |
| PATROLLING | head sweeps (+sin(age·0.18)·0.25 yaw), abdomen sways (sin(age·0.12)·0.07 yaw) |
| IDLE | abdomen "breathes" (pulse·0.35) |

**Caste silhouettes** (`applyCasteSilhouette`, `:289-357`). Part scale multipliers stack on top of the overall render scale. Scaling a parent also scales its children, so head scale affects the antennae and mandibles.

| Caste | Extra parts shown | Part scaling (x, y, z) |
|---|---|---|
| WORKER | – | abdomen (1.06, 1.02, 1.08); antennae (0.96, 1.08, 0.96) |
| SCOUT | – | head 0.94; thorax (0.88, 0.98, 0.90); petioles 0.82; abdomen (0.78, 0.88, 0.84); antennae (0.88, 1.28, 0.88); all legs (0.74, 1.10, 0.74) — slim and long-legged |
| MINER | thorax plate | head 1.12; thorax (1.08, 1.02, 1.05); mandibles (1.34, 1.12, 1.38); front legs 1.10 |
| SOLDIER | crest + plate | head (1.20, 1.08, 1.16); thorax 1.10; mandibles (1.55, 1.18, 1.62) |
| MAJOR | crest + plate | head (1.38, 1.18, 1.30) (big-headed); thorax (1.18, 1.10, 1.15); abdomen 1.14; mandibles (1.88, 1.28, 2.0) |
| GIANT | crest + plate | head (1.28, 1.18, 1.25); thorax (1.25, 1.18, 1.24); abdomen (1.34, 1.22, 1.38); mandibles (2.02, 1.34, 2.18); legs (1.12, 1.08, 1.12) |
| QUEEN | crest + plate + wings | head (1.02, 1.0, 1.04); thorax (1.15, 1.10, 1.18); petiole 1.16; post-petiole 1.22; abdomen (1.42, 1.20 + pulse·0.08, 1.58) (pulsing gaster); legs (0.92, 0.96, 0.92); wings flutter ∓pulse·0.08 |

**Textures.** Generated by `tools/generate_formic_textures.py` (`:124-131` palettes, `:180-234` layout):
- Each part region is filled flat with a dark frame, base colour, noise pixels and diagonal highlight lines.
- Wings are semi-transparent (alpha 125–210) with vein lines.
- Palette order below is dark / base / mid / light; I also confirmed the colours by decoding the PNGs.

| Caste | Palette | Eye colour (`:204-211`) | Extra stripes (`:221-231`) |
|---|---|---|---|
| worker | #231310 / #65321F / #AB5B2B / #F19E4B (chocolate / orange) | amber #FFAC3B | – |
| scout | #1A1916 / #594628 / #B5843D / #FFD379 (olive-brown / gold) | amber | – |
| miner | #15171B / #383D43 / #7A735E / #DEC18B (slate grey / sand) | cyan #67DEFF | sand stripe on the crest region (crest is hidden for miners); grey stripe on the thorax region |
| soldier | #220A0D / #651B1D / #B83D25 / #FF873A (maroon / orange) | green #81FF77 | steel stripe on crest, brown-grey on plate |
| major | #1C0F0F / #4B211B / #974828 / #EB9146 | red #FF4A30 | steel stripes |
| giant | #1E0D0B / #572314 / #BB4B1D / #FF9F3F | red #FF4A30 | steel stripes |
| queen | #2F1914 / #854826 / #DE8F43 / #FFDA8B (warm brown / cream) | amber | cream + rust stripes on crest |

**Reference art.** `docs/art/vertical_ant_texture_reference.png` shows the intended look: upright, humanoid ants. It has a worker with a basket, a forager with a spear and leaf, a miner with a pickaxe, a soldier with spear and shield, a gold-armoured officer with a cape, a giant brute with a mace, and a robed winged queen with a huge gaster. The actual model has no held tools, capes or armour meshes; it approximates these only through proportions, the crest/plate/wings and colour.

**Work-cue particles** (server, every 6 ticks, above the head; `entity/AntEntity.java:141-173`):
- WORKING: 3× `DUST_PLUME`.
- PATROLLING: 2× blue dust (0x6FC6FF, size 0.85).
- CARRYING_x: 2× item particles — wheat (food), raw iron (ore), bone (chitin), honeycomb (resin), brown mushroom (fungus), slime ball (venom).

**Names** (`world/ColonyService.java:919-932`):
- The queen always gets a visible name, "Queen #<colony id>".
- MAJOR and GIANT get "%s Guard #%s", filled with the **raw caste id**, e.g. "major Guard #1" or "Страж major #1". This is a localisation bug.
- Only the founding foreman gets "Worker Foreman #id".

---

## 3. Jobs

### 3.1 Abstract job loop (`CasteJobLoop`) — IMPLEMENTED; it duplicates other systems

Constants: `BUILD_PROGRESS_CAP_PER_LOOP` 10 (`sim/CasteJobLoop.java:22`); `TEND_HEALTH_CAP_PER_LOOP` 5 (`:24`); `PATROL_DEFENDERS_PER_BUILDING` 3 (`:26`); `JOB_LOOP_TICK_INTERVAL` is an unused constant (`:19`).

| Job | Effect per ET | Lines | Overlaps with |
|---|---|---|---|
| gather | +workers food, +2·miners ore, +scouts chitin | `:50-58` | `ColonyEconomy` already pays these castes (3 food/worker, 2 ore/miner) and entity trips pay again. Scouts gather **chitin** here but **food** in `ColonyEconomy` and as entities. |
| build | first incomplete building +min(workers, 10)% (0 workers = nothing); event "Builder crew completed X" | `:60-76` | `ColonyBuilder` adds 8 + 4·workers + cultureBonus % each ET, and entity construction trips add +6%. When this loop completes a building it does **not** place the finished structure or refresh labels, which `ColonyBuilder` would do (`world/ColonyBuilder.java:72-79`). Buildings finished here likely keep their construction-stage look (extraction 02 should confirm). |
| patrol | for each damaged building: `disabledTicks` −(1 + min(defenders, 3)); then **decrements the colony's own `raidCooldown`** | `:78-98` | `raidCooldown` is the colony's cooldown before its **own** next raid (`world/RaidPlanner.java:36-41`). Defenders therefore make their colony raid *sooner*, and `RaidPlanner.tick` also decrements it (`:29`), so it runs at 2/s. `ColonyBuilder` starts repair immediately anyway, which zeroes `disabledTicks` on completion. |
| tend | queen HP +min(workers, 5), max 140, only while alive | `:100-109` | Royal Jelly |

- If anything changed (almost always, because gathering is > 0), the current task is set to "Job loop: gathered F food/O ore/C chitin built N% patrols restored K defenses tended queen +H hp" (`:44-46, 111-127`).
- The job loop runs even when the queen is dead.

### 3.2 In-world ant AI (`AntEntity`) — IMPLEMENTED; small real effect

**Synced data:** CASTE, COLONY_ID and WORK_STATE, stored as ordinals (`entity/AntEntity.java:51-53, 78-84`). They are saved as strings `caste`, `colonyId`, `workState` (`:205-219`). `AntCaste.fromId` throws on an unknown id (`sim/AntCaste.java:101-109`).

**Attributes:** registered with WORKER defaults and follow range 24 (`:59-65`). Caste values are applied on `setCaste` and re-applied every 40 ticks (`:135-140, 190-203`).

**Goals** (`:67-76`):

| Priority | Goal | Notes |
|---|---|---|
| 0 | Float | |
| 1 | MeleeAttack (speed 1.0) | |
| 2 | `AntWorkGoal` | flags MOVE + LOOK |
| 3 | RandomStroll (0.75) | |
| 4 | LookAtPlayer (8 blocks) | |
| 5 | RandomLookAround | |
| target 1 | NearestAttackableTarget(AntEntity) | only if `ColonyService.areHostile(colonyA, colonyB)`, i.e. either side's relation is RIVAL or WAR (`world/RaidPlanner.java:48-59`) |

**`AntWorkGoal` flow** (`:225-509`):

1. **`canUse`** (`:241-262`): not a queen; random 1-in-6 chance per evaluation; server side only. Builds an assignment using a random selector 0–99.
2. **Colony lookup** (`:333-345`): the colony with this id, else the nearest colony within 128. With no colony, a fallback block scan is used.
3. **`start`:** state = PATROLLING or WORKING; path to the target. Pathing speed modifier = max(0.8, 4 × caste speed): worker 1.12, scout 1.36, miner 0.96, soldier 1.04, major 0.88, giant 0.80 (`:479-483`).
4. **`tick`** (`:275-318`):
   - Look at the target. If farther than √3.8 ≈ 1.95 blocks (`:485-487`), re-issue the path whenever navigation finishes. There is **no timeout or stuck detection**.
   - At the target: stop and count goal ticks, swinging the arm every 8. After `duration`:
     - PATROL: sets the colony task "<caste> patrols x, y, z" and ends.
     - CONSTRUCTION: `ColonyService.depositConstructionWork` adds +6% to the colony's first incomplete building and re-places its structure (`world/ColonyService.java:419-443`).
     - RESOURCE / LOGISTICS: switch to CARRYING_<res>, walk to the delivery target, work `duration` again, then `ColonyService.depositWorkedResource`. That adds `amount` to the colony stockpile, sets the task "<caste> delivered N res", and **adds an event "<caste> delivered res" on every trip** (`world/ColonyService.java:408-417`).

**Assignment table:**

| Caste | Kind | Source → delivery | Amount | Duration (goal ticks) |
|---|---|---|---|---|
| MINER | RESOURCE ore | ore node near origin + (8, 0, 54) → first MINE building (or its planned site) | 5 | 30 |
| WORKER, any open request | LOGISTICS | source of the request's resource → the request's building (request with the biggest `missing` first) | clamp(missing, 1, 4) | 24 |
| WORKER, else if a building is incomplete | CONSTRUCTION | the building's position | +6 construction % | 30 |
| WORKER, else `selector % 5` | RESOURCE | 0: food node (54, 0, 8) → FOOD_STORE, 4 food; 1: chitin node (−54, 0, 8) → NURSERY, 2 chitin; 2: RESIN_DEPOT → RESIN_DEPOT, 2 resin; 3: FUNGUS_GARDEN → NURSERY, 2 fungus; 4: VENOM_PRESS → ARMORY, 1 venom | see left | 24 |
| SCOUT | RESOURCE food | food node → QUEEN_CHAMBER | 2 | 22 |
| SOLDIER / MAJOR / GIANT | PATROL | patrol points of completed BARRACKS (pos.north(13)), WATCH_POST (north(6)), ROAD (pos), QUEEN_CHAMBER (north(17)), sorted in that priority, picked by selector mod count; colony origin if none | – | 18 |
| QUEEN | none | – | – | – |
| No colony nearby (fallback) | | block scan within 48 (y −3..+4): MINER ORE_NODE → MINE_CHAMBER block, 5 ore; WORKER/SCOUT FOOD_NODE → FOOD_CHAMBER, 2 food; fighters patrol to BARRACKS_CHAMBER or NEST_MOUND block | as left | 24 / 18 |

Sources for the table: `:338-344, 347-374, 380-415, 417-431, 433-461`.

Notes:

- **Where gathering happens.** `resourceSource` (`:433-453`) looks for the matching block within ±6 of an expected point and falls back to that point. Resin, fungus and venom trips use the building's position or its *planned* site even when the building does not exist. Ants can therefore "gather" venom or resin from empty ground.
- **Units.** Durations are counted in goal ticks. Vanilla usually runs goals that don't need every-tick updates every 2 game ticks, so 30 goal ticks is probably about 3 s. I have not verified this for 1.21.11.
- **Logistics trips do not fulfil the request.** They add resources to the general stock only (§4.7). Construction trips carry the label "resin" but consume no resin.
- **Scale.** Entity numbers stay around 7 per colony while the abstract population climbs into the hundreds (§6.3), so entity trips are a minor extra income.
- A gametest pins the targets: a worker target is more than 18 blocks from origin, a miner targets the campus ore node, a soldier gets a patrol point with headroom (`src/gametest/java/com/formicfrontier/test/FormicFrontierGameTest.java:1096-1148`).

### 3.3 `AntWorkState` — IMPLEMENTED (`sim/AntWorkState.java`)

- Values: IDLE, WORKING, CARRYING_FOOD, CARRYING_ORE, CARRYING_CHITIN, CARRYING_RESIN, CARRYING_FUNGUS, CARRYING_VENOM, PATROLLING (`:6-14`).
- `carrying(KNOWLEDGE)` maps to WORKING (`:26-36`).
- `fromId` falls back to IDLE (`:38-46`).

### 3.4 `TaskPlanner` and the "current task" string — mostly invisible in practice

`describeNextTask` walks the instinct list and returns the first message whose condition holds (`sim/TaskPlanner.java:7-33`):

- "Foraging for food" if food < 4·upkeep
- "Mining ore node" if ore < 20
- "Cultivating chitin nursery" if chitin < 20
- "Raising defenders" if soldiers + majors < 2
- otherwise "Balanced maintenance"

`ColonyEconomy` sets this text only when nothing grew (`sim/ColonyEconomy.java:79`). Later in the same ET, `CasteJobLoop`, `CasteBalancer`, `ColonyBuilder`, entity deliveries, events and raids all overwrite `currentTask`. The last writer wins, and in practice that is usually `ColonyBuilder` ("Building X NN%", "Waiting for resources to build X"). The tablet shows it as "Goal" (`screen/ColonyStatusScreen.java:373`).

`TaskPlanner.preferredWorkerFor` is **DEAD** (no callers; `sim/TaskPlanner.java:35-41`).

### 3.5 Player interactions with ants — IMPLEMENTED

`entity/AntEntity.java:175-188` → `world/ColonyService.java:318-381`. Nothing is consumed in creative mode.

| Held item | Target | Effect |
|---|---|---|
| empty hand | any ant | opens the colony tablet, feedback "Inspecting <caste> ant." |
| Royal Jelly | queen only | queen entity heals 24; colony queenHealth = max(colony, entity HP); +6 rep; event |
| Raw Biomass / Wheat | any | +8 / +3 food; +1 rep; ant heals 2 |
| Chitin Shard | any | +3 chitin; +1 rep; ant heals 4 |
| Pheromone Dust | any | moves an instinct to the top: MINER → ORE; SOLDIER / MAJOR / GIANT → DEFENSE; QUEEN → CHITIN; WORKER / SCOUT → FOOD (`:366-378, 630-637`) |
| anything else | any | opens the tablet of the nearest colony within 96 with "Formic <caste> \| hp X/Y" |

---

## 4. Resources and economy

### 4.1 Resource list — IMPLEMENTED (`sim/ResourceType.java:8-14`)

| id | EN / RU (`en_us.json:68-74`) | Seed at founding | UI colour (`network/ColonyUiSnapshot.java:535-545`) | Contract delivery item: items → resource (`sim/ContractDeliveryOption.java:4-14`) | Entity carry cue |
|---|---|---|---|---|---|
| food | Food / Еда | 120 | 0x91C46C | 8 wheat → 12 | wheat |
| ore | Ore / Руда | 20 | 0xB9B8AC | 4 raw iron → 8 | raw iron |
| chitin | Chitin / Хитин | 24 | 0xD6B16E | 2 chitin shard → 6 | bone |
| resin | Resin / Смола | 24 | 0xD69042 | 2 resin glob → 6 | honeycomb |
| fungus | Fungus / Грибы | 12 (LEAFCUTTER 28) | 0x9BC76C | 2 fungus culture → 6 | brown mushroom |
| venom | Venom / Яд | 4 (FIRE 16) | 0x7DD66C | 1 venom sac → 5 | slime ball |
| knowledge | Knowledge / Знания (short "Know." / "Знания", `en_us.json:135`) | 8 | 0xB58BFF | 4 pheromone dust → 6 | – (maps to WORKING) |

**Caps and floors:** there are **no stockpile caps and no storage capacity**. The only rule is `max(0, …)` (`sim/ColonyData.java:85-91`). FOOD_STORE adds income, not capacity. Overflow has no consequence.

**Shortage consequences:**
- Growth, construction, research and upgrades are blocked by missing costs.
- The balancer reacts (§2.6).
- A famine event opens a player contract when food < max(24, 3·upkeep) (`world/ColonyRecurringEvents.java:231-236, 305-311`).
- Nothing dies.

### 4.2 `ColonyEconomy.tick` formulas — IMPLEMENTED (`sim/ColonyEconomy.java:9-83`)

Variables:
- W, S, M, So = worker / scout / miner / soldier counts.
- B_x = number of **completed** buildings of type x. Building level is ignored.
- RB = rank economy bonus (0 / 2 / 5 / 9, §6.1). It is computed from the state *before* this tick's changes (`:29`).
- Integer division throughout.

```
food    = 4 + 3W + S + 2·B_food_store + B_market + 2·B_fungus_garden + RB + culture.foodBonus          (:32)
ore     = 2M + B_mine + RB/2                                                                              (:33)
chitin  = FOOD_stock/25 + W/2 + 5·B_chitin_farm + B_nursery + B_diplomacy_shrine + RB/2                   (:34)
          + max(1, 2·B_chitin_farm)                     if CHITIN_CULTIVATION                             (:35-37)
resin   = 4·B_resin_depot + W/3 + culture.resinBonus                                                      (:38)
fungus  = 4·B_fungus_garden + B_nursery + culture.fungusBonus                                             (:39)
          FUNGUS_SYMBIOSIS: food += max(1, 3·B_fungus_garden); fungus += 2                                (:40-43)
venom   = 3·B_venom_press + culture.venomBonus                                                            (:44)
          + max(1, So/3 + B_armory)                     if VENOM_DRILLS                                   (:45-47)
knowl.  = B_archive  if B_archive > 0 and no active research, else 0                                      (:48)
culture extras:                                                                                           (:49-57)
  AMBER      knowledge += B_shrine + min(B_market, B_shrine)
  LEAFCUTTER food += 2·B_fungus_garden; fungus += B_fungus_garden
  FIRE       venom += 2·B_armory + B_watch_post
  CARPENTER  resin += 2·B_resin_depot + min(B_resin_depot, B_archive)
upkeep  = Σ caste.foodUpkeep × count                                                                      (:58)
FOOD += food − upkeep; the other resources += their income (clamped ≥ 0)                                  (:60-66)
if queen dead: task "Queen lost: growth suspended", return                                                (:68-71)
grow ≤ 1 caste (§2.4)                                                                                     (:73-80)
```

Chitin income rises with the food stock (1 chitin per 25 food held). This is an odd coupling.

### 4.3 Per-building contributions to the colony economy

Buildings are covered in extraction 02; this table lists only their economy hooks.

| Building (per completed instance) | Per-ET income | Other economy hooks |
|---|---|---|
| QUEEN_CHAMBER | – | claim radius 18 + 6·level (`world/ColonyBuilder.java:48`) |
| FOOD_STORE | +2 food | needed for the famine event |
| NURSERY | +1 chitin, +1 fungus | needed for brood bloom; CHITIN_CULTIVATION's required chamber |
| MINE | +1 ore | – |
| CHITIN_FARM | +5 chitin (+2 with CHITIN_CULTIVATION) | lets the armory supply chitin armour |
| BARRACKS | – | DEFENSE soldier target +2; defense rating +4 |
| MARKET | +1 food; AMBER +knowledge paired with shrines | enables `TradeCaravan` |
| DIPLOMACY_SHRINE | +1 chitin; AMBER +1 knowledge (+1 more if paired with a market) | enables the autonomous envoy |
| WATCH_POST | FIRE +1 venom | defense rating +6 |
| RESIN_DEPOT | +4 resin (CARPENTER +2 more, +1 per archive pairing) | logistics throughput +3 (if any exist) |
| PHEROMONE_ARCHIVE | idle: +B knowledge (economy) and +1 + B (logistics) | research speed +10 per archive; required for any research |
| FUNGUS_GARDEN | +2 food, +4 fungus (+3 food with FUNGUS_SYMBIOSIS; LEAFCUTTER +2 food, +1 fungus) | `NativeBlockRole`: −5 food → +8 fungus while food > 40 |
| VENOM_PRESS | +3 venom | can only be started with VENOM_DRILLS |
| ARMORY | FIRE +2 venom; VENOM_DRILLS +1 venom | arms 4 combat ants |
| GREAT_MOUND / QUEEN_VAULT / TRADE_HUB | – | the vault absorbs 3 raid queen damage; the trade hub gives token discounts (extraction 03) |
| ROAD | – | only +10 rank score |

- Every completed building also adds +10 rank score (§6.1).
- Upgrades (level 2) change none of these numbers. Level only affects queen-chamber claim radius, repair cost and visuals.
- During a repair, progress is set to 55 (`world/ColonyBuilder.java:18,99`), so the building stops counting as completed and produces nothing until the repair finishes.

### 4.4 Other resource sources and sinks

| Flow | Amount | Source |
|---|---|---|
| Job loop gather | +W food, +2M ore, +S chitin per ET | `sim/CasteJobLoop.java:50-58` |
| Fungus garden compost | per garden −5 food → +8 fungus, only while food − 40 ≥ 5·gardens run | `sim/NativeBlockRole.java:26-58` |
| Knowledge while idle (logistics) | +1 + B_archive per ET | `sim/ColonyLogistics.java:160-167` |
| Entity deliveries | 1–5 of a resource per trip (§3.2) | `world/ColonyService.java:408-417` |
| Caste growth | caste costs | `sim/AntCaste.java:95-99` |
| Construction and upgrades | building cost (×0.9 with RESIN_MASONRY); upgrade = ceil(0.6·cost) + resin 10 + 4·level (+8 knowledge for the archive) | `world/ColonyBuilder.java:210-220, 399-425` |
| Repair | chitin max(4, chitinCost/2 + 2·level) | `world/ColonyBuilder.java:158-160` |
| Research | node costs (§7) | `sim/ResearchNode.java:84-88` |
| Brood bloom | −45 food, −12 chitin | `world/ColonyRecurringEvents.java:187-189` |
| Player hand-feeding | wheat +3, biomass +8, chitin shard +3 | §3.5 |
| Player trade offers | e.g. `sell_wheat` 16 wheat → +12 food; `sell_royal_jelly` → +40 food | `sim/ColonyTradeCatalog.java:28-36` (extraction 03) |
| Contracts | delivered resources go into the stockpile | `sim/ColonyLogistics.java:73` |
| Raids | defender loses max(4, richest/5 − defense/3) of its richest resource to the attacker | `world/RaidPlanner.java:146-150` |
| Caravans | up to 8 (ally) or 4 (neutral) of a surplus resource (> 40) to a scarce (< 20) neighbour, **every second**; +1 rep to the sender | `sim/TradeCaravan.java:18-22, 37-68, 101-108` |

### 4.5 Worked example: the first second of a new AMBER colony (hand-computed)

Start state: 5 completed starters (queen chamber, food store, nursery, mine, barracks); castes Q1 W3 S1 M2 So2 Ma1; rank score 5·10 + 10·2 + 140/12 = 81 → **BURROW** (RB = 2).

1. **Economy.** food = 4 + 9 + 1 + 2 + 2 = 18; upkeep = 12 + 3 + 1 + 4 + 6 + 6 = 32. Food goes 120 → 106. Ore +6, chitin +7 (4 + 1 + 1 + 1), resin +1, fungus +1. Growth picks WORKER (FOOD rule, 3 < 8): −6 food.
2. **Job loop** with 4 workers: +4 food, +4 ore, +1 chitin.
3. **Builder** starts DIPLOMACY_SHRINE (−40 food, −14 ore, −24 chitin).
4. **End of second 1:** food 64, ore 16, chitin 8, resin 25, fungus 13. My re-implementation reproduces these exact numbers.

### 4.6 Numeric dynamics worth knowing

- One worker nets +3 food per second for a one-off 6 food, so food accelerates as workers accumulate.
- Upkeep scales with population, but each worker's income exceeds its upkeep. No negative feedback stops growth.

### 4.7 Requests, logistics and contracts — PARTIAL

**`ColonyRequest`** (`sim/ColonyRequest.java`) is a record `(building, resource, needed ≥ 1, fulfilled 0..needed, reason)`. Codec defaults: fulfilled 0, reason "colony logistics".

**`requestResource`** (`sim/ColonyLogistics.java:18-29`):
- Ignores needed ≤ 0.
- Deduplicates by building + resource + reason among open requests.
- Logs "Request opened: N res for building".

Callers in live code, all with "player-supply" reasons:

| Reason | Where |
|---|---|
| `construction <type>` | `world/ColonyBuilder.java:405-412` |
| `research <node>` | `sim/ColonyLogistics.java:104-111` |
| `repair <type>` | `world/ColonyBuilder.java:138-143` |
| famine / migration / invasion / treaty / expansion | `world/ColonyRecurringEvents.java:99-215` |

**Auto-delivery (`fulfillRequests`)** — **DEAD in live play** (`sim/ColonyLogistics.java:124-158`):
- Throughput = max(1, workers) + 3 if a resin depot exists + culture.workerBias.
- It moves stock into the first request that is not a player-supply request, one request per ET. It skips reasons starting with construction / research / repair / famine / migration / invasion / treaty / expansion (`:190-200`).
- Every live request uses one of those reasons, so it only purges completed requests. The only non-player requests are created by QA scenes (`src/main/java/com/formicfrontier/qa/VisualQaScenes.java:677-692`).
- Unit tests prove it works with made-up reasons (§13).

**How requests actually close:**
- **Construction:** a request is informational. When stock covers the cost from normal income, the builder starts the building and clears the requests (`world/ColonyBuilder.java:263-284, 414-417`).
- **Research:** the player delivers. On completion, research starts automatically (see below).
- **Repair:** repair starts when chitin stock ≥ cost *or* the repair request is complete (`world/ColonyBuilder.java:121-136`).

**Contracts** (`sim/ColonyContract.java`) are the player-facing view of open requests (extraction 03 and 04 cover the UI):
- `id` = `building:resource:<sanitised reason ≤ 24 chars>:<hex hash of reason>`.
- Priority: famine 5, invasion 5, treaty 4, expansion 4, repair 4, research 3, building QUEEN_CHAMBER or BARRACKS 3, construction 2, else 1 (`:71-98`).
- Reward tokens = max(1, ceil(missing/8) + priority); reputation = clamp(ceil(missing/16) + priority/2, 1, 6) (`:17-34`). Pro-rated per delivery (`:57-69`).
- `fulfillContract` adds the delivered amount to the stockpile and the reputation, and removes the request when complete (`sim/ColonyLogistics.java:50-83`).
- `ColonyService.completeContract` takes the whole missing amount as items and gives Pheromone Tokens (`world/ColonyService.java:154-203`).
- When a **research** contract completes, `startResearch` runs automatically (`sim/ColonyLogistics.java:202-218`); the unit test at `test/ColonyEconomyTest.java:402-427` covers it.

---

## 5. Instincts (`TaskPriority`) — IMPLEMENTED; the player's main lever

**Enum:** FOOD, ORE, CHITIN, DEFENSE (`sim/TaskPriority.java:8-11`). Default order is [FOOD, ORE, CHITIN, DEFENSE] (`sim/ColonyData.java:50-53`).

**Names** (`en_us.json:82-85`): Food / Еда, Ore / Руда, Chitin / Хитин, Defense / Оборона.

**UI strings:** tab "Instinct" / "Инстинкт" (short "Inst." / "Инст."); help "Colony instinct" / "Инстинкт колонии"; detail "Biases autonomous growth." / "Меняет план роста."; rows "Priority %s" / "Приоритет %s" (`en_us.json:116-117, 178-180`). Colours: FOOD 0x91C46C, ORE 0xB9B8AC, CHITIN 0xD6B16E, DEFENSE 0xD06B5D (`network/ColonyUiSnapshot.java:559-566`).

**Changing it.** Moving an instinct to the top keeps the other three in enum order:
- tablet → `PriorityRequestPayload` → `ColonyService.setTopPriority` (`src/main/java/com/formicfrontier/registry/ModNetworking.java:30-32`, `world/ColonyService.java:205-231`);
- `/formic colony instinct|priority <id>`;
- Pheromone Dust on an ant (§3.5).

**What the order affects:**

| Instinct | Growth rule (§2.4) | Balancer need | TaskPlanner text | Builder expansion when it is the **top** instinct (`world/ColonyBuilder.java:435-442`) |
|---|---|---|---|---|
| FOOD | workers < 8 + bias, scouts < 3 + bias | food < 2·upkeep | food < 4·upkeep | FOOD_STORE until 2, then CHITIN_FARM forever |
| ORE | miners < 8 | ore < 20 | ore < 20 | MINE until 2, then ROAD forever |
| CHITIN | majors < 2, workers < 6 | chitin < 20 | chitin < 20 | CHITIN_FARM until 3, then NURSERY forever |
| DEFENSE | soldiers < 3 + 2·barracks, majors < 4, giant | soldiers + majors < 2 | < 2 defenders | WATCH_POST until 4, then BARRACKS forever |

The old tablet help text (dead code) summarised it as "Food=workers/scouts, Ore=miners, Chitin=nursery/major, Defense=soldiers/watch posts." (`world/ColonyService.java:952-958`).

---

## 6. Progression

### 6.1 `ColonyRank` — IMPLEMENTED (`sim/ColonyRank.java`)

**Score** (`:50-58`) = 10 × completed buildings + 2 × population (queen included) + max(0, reputation) + queenHealth/12 + 12 × ALLY relations.

| Rank | id | Display (EN / RU, `en_us.json:64-67`) | Score ≥ | Claim radius | Economy bonus (food / ore / chitin per ET) |
|---|---|---|---|---|---|
| OUTPOST | outpost | Outpost / Форпост | 0 | 18 | 0 / 0 / 0 |
| BURROW | burrow | Burrow / Нора | 35 | 24 | 2 / 1 / 1 |
| HIVE | hive | Hive / Улей | 85 | 36 | 5 / 2 / 2 |
| CITADEL | citadel | Citadel / Цитадель | 155 | 48 | 9 / 4 / 4 |

Rank is derived from state every time it is read; it is not stored. `atLeast(colony)` is used for gates (`:65-67`).

What each rank unlocks:

| Rank | Unlocks |
|---|---|
| OUTPOST | ENVOY diplomacy action |
| BURROW | TRIBUTE and INCITE (`sim/DiplomacyAction.java:6-10`); the autonomous envoy pass (`sim/DiplomacyService.java:73`); the treaty-opportunity event (`world/ColonyRecurringEvents.java:256`); unlock condition for the RELATIONS guide chapter (`sim/GuideChapter.java:50-52`) |
| HIVE | TRUCE; migration and expansion events (`world/ColonyRecurringEvents.java:240, 278`) |
| CITADEL | WAR_PACT; all three endgame projects (GREAT_MOUND → QUEEN_VAULT → TRADE_HUB, `world/ColonyBuilder.java:346-385`) |

- Claim radius each world tick = clamp(max(18 + 6·queen-chamber level, rank radius), 18, 48) (`world/ColonyBuilder.java:48`, `sim/ColonyProgress.java:116-118`).
- In live play a new colony starts at **BURROW** (score 81 for AMBER; LEAFCUTTER starts at 85 = **HIVE**). OUTPOST is only reachable in tests.
- Because score includes 2 × population and 10 × buildings, CITADEL arrives within about half a minute (§6.3).
- The tablet header shows the rank key `formic_frontier.rank.<id>` (`network/ColonyUiSnapshot.java:199`).

### 6.2 `ColonyStage` and `ColonyStageProgression` — PARTIAL (little effect in live play)

Thresholds (`sim/ColonyStage.java:23-29`). "Age" = `ageTicks / 20` = number of ETs = **seconds** (`:84`). "Composite" = ore + chitin + resin in stock (`:86-88`).

| Stage | id | Age ≥ | Food ≥ | Composite ≥ | Signature building (auto-enqueued) |
|---|---|---|---|---|---|
| FOUNDING | founding | 0 | 0 | 0 | none |
| GROWTH | growth | 3 | 120 | 40 | BARRACKS |
| ESTABLISHED | established | 6 | 400 | 180 | PHEROMONE_ARCHIVE |
| MATURE | mature | 12 | 900 | 500 | GREAT_MOUND |

Behaviour:

- `earnedFrom` returns the highest stage whose three thresholds are met. Stock is checked *now*, not historically (`sim/ColonyStage.java:83-101`).
- `tick` advances monotonically through every intermediate stage (`sim/ColonyStageProgression.java:22-51`). Each step:
  - enqueues the signature building unless it is already completed, queued or planned (`:58-71`);
  - logs "Colony advanced to the <id> stage";
  - is frozen while the queen is dead (`:26-28`).
- The stage is saved (`sim/ColonyData.java:26`). **It is not shown in the tablet**; there are no lang keys for stages, and the only display is the debug `statusText` "Stage: <id>" (`sim/ColonyData.java:204`).

Live-play reality:

- Every colony starts with a completed BARRACKS (`world/ColonyService.java:691`), so the GROWTH unlock is always a no-op.
- PHEROMONE_ARCHIVE is in every culture's starter queue (`sim/ColonyCulture.java:80-87`) and in `STARTER_SEQUENCE` (`world/ColonyBuilder.java:20-31`), so the ESTABLISHED unlock is a no-op.
- The MATURE unlock (GREAT_MOUND) bypasses the builder's CITADEL gate (`world/ColonyBuilder.java:359-368`). An unattended colony has usually built the Great Mound through the builder long before MATURE, so this is also a no-op in my re-implementation.
- The unit test (`test/ColonyEconomyTest.java:593-681`) passes only because its fixture has no buildings, unlike a live colony.

### 6.3 Pacing — from my re-implementation, not the mod

I re-implemented in Python the ET passes `ColonyEconomy`, `ColonyLogistics`, `CasteJobLoop`, `ColonyStageProgression`, `CasteBalancer`, `NativeBlockRole` and `ColonyBuilder`, including upgrades and endgame projects, following the Java logic line by line. It excludes recurring events, raids, diplomacy and caravans, entity deliveries and any player action. Script: scratchpad `colonysim.py`.

It reproduces the hand-computed first second exactly. Exact seconds may still differ from the real mod, but the qualitative result follows directly from the code.

Unattended AMBER colony:

| t (s) | Stage | Rank (score) | Population | Completed buildings | Notable |
|---|---|---|---|---|---|
| 0 | founding | burrow (81) | 10 | 5 | queue: shrine, market, archive |
| 2 | founding | hive (85) | 12 | 5 | |
| 20 | growth | hive (141) | 30 | 7 | GROWTH signature BARRACKS: no-op |
| 27 | growth | citadel | ~37 | 8 | |
| 34–35 | | citadel | | | Great Mound planned, built in about 1 s |
| 39–40 | | | | | Queen Vault built |
| 51–52 | | | | | Trade Hub built |
| 60 | growth | citadel (311) | 70 | 16 | endless CHITIN_FARM spam begins (FOOD instinct) |
| 65 | established | | | | ARCHIVE signature: no-op |
| ~180 | mature | citadel (1151) | 190 | 76 | GREAT_MOUND signature: no-op |
| 600 | mature | citadel (4091) | 610 | 286 | |
| 1800 | mature | citadel (12491) | 1810 | 886 | 1201 workers, 592 majors, chitin ≈ 4.7 M |

- LEAFCUTTER, FIRE and CARPENTER behave the same way: Great Mound at 36–38 s, Trade Hub at 62–65 s.
- Population grows about +1 per second without end (growth is capped only at one ant per ET).
- Buildings finish in 1–2 s once there are ~23 workers, because builder progress is 8 + 4·W per second.

---

## 7. Research

### 7.1 Mechanics — IMPLEMENTED, player-initiated only

**Starting** (`sim/ColonyLogistics.java:85-122`): tablet node click → `ResearchRequestPayload` → `ColonyService.startResearch` (nearest colony within 128), or `/formic research <node>`. Checks, in order:

1. Not already complete.
2. No other research active (only one at a time).
3. A completed PHEROMONE_ARCHIVE.
4. The node's required building completed.
5. Prerequisites researched.
6. Costs in stock. For every shortfall a request `research <node>` is opened and the start fails with "Research lacks resources; requests were opened."

On success: clear that node's research requests, pay the costs, set `activeResearch = (node, 0)`, and log the task and event.

- `ResearchNode.canStart` repeats the same checks (without opening requests) and is used only for the UI "startable" flag (`sim/ResearchNode.java:61-82`, `network/ColonyUiSnapshot.java:129`).
- **There is no autonomous research.** Only the player (tablet or command) or a completed research contract (§4.7) starts research.

**Progress** (`sim/ColonyLogistics.java:160-182`):
- Each ET adds 20 + 10 × completed archives to `progressTicks`. It finishes when progress ≥ `durationTicks` (120–200).
- With one archive (speed 30), each node takes 4–7 seconds. With two archives (speed 40), 3–5 seconds.
- **Knowledge generation stops while research is active.** An idle colony with ≥ 1 archive gets knowledge = B_archive (economy) + 1 + B_archive (logistics), i.e. 3 per second with one archive. AMBER adds shrine and market knowledge regardless.

**UI:**
- Research tab; the node map has 2 columns (`screen/ColonyStatusScreen.java:997-1008`):
  - basic column (x = 18): chitin_cultivation (y 56), resin_masonry (114), fungus_symbiosis (172), scented_ledger (230);
  - advanced column (x = advancedX): mandible_plating (114), venom_drills (172), treaty_sigils (230).
- Node label from `formic_frontier.research.<id>`, falling back to the hardcoded English label (`:1010-1014`).
- State strings (`en_us.json:125-134`):
  - complete: "Discovered" / "Открыто"
  - active: "Being studied" / "Изучается"
  - ready: "Ready - click to begin" / "Доступно — нажмите, чтобы начать"
  - locked: "Requirements not met" / "Требования не выполнены"
  - tooltip lines "Required chamber: %s", "Previous discovery: %s", "Cost: %s"
  - hints "Drag to explore / right-click to reset", "Hover a discovery for its purpose, chamber and cost."
- Status text sent from the server (`network/ColonyUiSnapshot.java:378-408`): "Complete", "Active", "Another research is active", "Requires Pheromone Archive", "Requires <building>", "Requires <prereq>", "Needs <res> N", "Ready".

**Persistence:** `completedResearch` (list of ids) and `activeResearch` (`nodeId`, `progressTicks`) (`sim/ColonyProgress.java:28-29`, `sim/ResearchState.java`).

### 7.2 The research tree (`sim/ResearchNode.java:9-15`)

| Node (id) | Required chamber | Duration | Prerequisite | Cost | Real effects (with sources) |
|---|---|---|---|---|---|
| Chitin Cultivation (`chitin_cultivation`) | NURSERY | 120 | – | knowledge 12, chitin 16 | chitin income + max(1, 2·chitin farms) (`sim/ColonyEconomy.java:35-37`); harvesting a mature Chitin Bed gives +1 extra shard if the nearest colony within 96 has it (`src/main/java/com/formicfrontier/block/ChitinBedBlock.java:59-62`) |
| Resin Masonry (`resin_masonry`) | PHEROMONE_ARCHIVE | 140 | – | knowledge 16, resin 12, ore 8 | all building costs ×0.9 (ceil, min 1), for construction and upgrades (`world/ColonyBuilder.java:419-425`); auto-queues a RESIN_DEPOT if none (`:233-236`); armory chitin armour +2 per armed ant, "resin-chitin" (`sim/ColonyArmory.java:100-104`); prerequisite of Mandible Plating |
| Fungus Symbiosis (`fungus_symbiosis`) | PHEROMONE_ARCHIVE | 160 | – | knowledge 18, fungus 10, food 20 | food + max(1, 3·gardens), fungus +2 per ET (`sim/ColonyEconomy.java:40-43`); auto-queues FUNGUS_GARDEN until 2 (`world/ColonyBuilder.java:237-240`); unlocks trade offer `buy_fungus_culture` (`sim/ColonyTradeCatalog.java:40`); prerequisite of Venom Drills |
| Venom Drills (`venom_drills`) | ARMORY | 180 | fungus_symbiosis | knowledge 24, venom 8, ore 16 | venom + max(1, soldiers/3 + armories) (`sim/ColonyEconomy.java:45-47`); VENOM_PRESS can only be started with it (`world/ColonyBuilder.java:388-390`) and gets auto-queued (`:241-244`); armory venom spear +4 attack per armed ant if no mandible plating (`sim/ColonyArmory.java:91-94`); unlocks `buy_venom_sac`, `buy_venom_spear` (`sim/ColonyTradeCatalog.java:41,53`) |
| Mandible Plating (`mandible_plating`) | ARMORY | 200 | resin_masonry | knowledge 30, resin 20, chitin 24 | **unlocks growth of the GIANT caste** (`sim/ColonyEconomy.java:165-168`); armory mandible saber +5 attack per armed ant (`sim/ColonyArmory.java:88-90`); unlocks resin-chitin armour and mandible saber trade offers (`sim/ColonyTradeCatalog.java:48-52`) |
| Scented Ledger (`scented_ledger`) | MARKET | 160 | – | knowledge 18, resin 8 | token prices ×0.85, **overwritten** by the reputation multiplier at rep ≥ 25 or < 0 (bug, §15); culture-locked offers (e.g. Fire-only `sell_venom`) become visible to all (`sim/ColonyTradeCatalog.java:84-103, 120-124`); prerequisite of Treaty Sigils |
| Treaty Sigils (`treaty_sigils`) | DIPLOMACY_SHRINE | 180 | scented_ledger | knowledge 26, fungus 8, resin 8 | player diplomacy ENVOY / TRUCE token cost ×0.75 (ceil, min 1) (`world/ColonyService.java:622-628`); the tablet's diplomacy list still shows the base cost (`network/ColonyUiSnapshot.java:162`) |

Names and descriptions (`en_us.json:136-149` / `ru_ru.json:136-149`):

| id | EN name | EN detail | RU name | RU detail |
|---|---|---|---|---|
| chitin_cultivation | Chitin Cultivation | Raise tougher brood and refine discarded shell. | Выращивание хитина | Укрепляет расплод и позволяет перерабатывать панцири. |
| resin_masonry | Resin Masonry | Bind soil and stone into stronger colony chambers. | Смоляная кладка | Связывает землю и камень в прочные камеры. |
| fungus_symbiosis | Fungus Symbiosis | Cultivate living beds that feed the whole colony. | Грибной симбиоз | Живые грибницы снабжают пищей всю колонию. |
| venom_drills | Venom Drills | Turn venom pressure into precise mining power. | Ядовитые буры | Превращает давление яда в точную силу для добычи. |
| mandible_plating | Mandible Plating | Layer resin and chitin into soldier protection. | Панцирь жвал | Слои смолы и хитина защищают солдат. |
| scented_ledger | Scented Ledger | Record exchange routes in durable pheromone trails. | Пахучая книга | Хранит торговые пути в стойких феромонных следах. |
| treaty_sigils | Treaty Sigils | Shape colony scents into trusted diplomatic marks. | Знаки договора | Превращает запахи колонии в знаки доверия. |

Description vs effect mismatches:
- Venom Drills promises "mining power" but does nothing for ore; it affects venom, the venom press and weapons.
- Chitin Cultivation's "tougher brood" has no effect.
- Resin Masonry's "stronger chambers" means cheaper chambers (plus better armour).
- Mandible Plating's detail doesn't mention its biggest effect, unlocking the Giant caste.

### 7.3 Research-driven combat loadout (`ColonyArmory`) — IMPLEMENTED; used by raids

See §2.3. Test coverage: `test/ColonyEconomyTest.java:1012-1125`.

---

## 8. Cultures (`ColonyCulture`) — IMPLEMENTED (small modifiers)

Constructor order is `(id, displayName, color, foodBonus, resinBonus, fungusBonus, venomBonus, workerBias, scoutBias, constructionBonus)` (`sim/ColonyCulture.java:9-12`).

| Culture | id | displayName (hardcoded EN, used in colony names) | Lang name EN / RU (`en_us.json:60-63`) | Colour | food / resin / fungus / venom bonus per ET | workerBias | scoutBias | constructionBonus |
|---|---|---|---|---|---|---|---|---|
| AMBER | amber | Amber Burrow | Amber / Янтарная | 0xD69A22 | 0 / 0 / 0 / 0 | 0 | 0 | 0 |
| LEAFCUTTER | leafcutter | Leafcutter Choir | Leafcutter / Листорезы | 0x5E8F3A | 2 / 0 / 2 / 0 | 1 | 1 | 0 |
| FIRE | fire | Fire Mandible | Fire / Огненная | 0xB83224 | 0 / 0 / 0 / 2 | 0 | 0 | 1 |
| CARPENTER | carpenter | Carpenter Resin | Carpenter / Древоточцы | 0x9A6233 | 0 / 1 / 0 / 0 | 0 | 0 | 2 |

Where each modifier is used:
- **workerBias:** seed workers, the FOOD worker target (8 + bias) and logistics throughput (`world/ColonyService.java:648`, `sim/ColonyEconomy.java:116`, `sim/ColonyLogistics.java:128`).
- **scoutBias:** seed scouts and the FOOD scout target.
- **constructionBonus:** adds to builder and repair progress per ET (`world/ColonyBuilder.java:70,108`).

Per-culture rules:

| Culture | Starter queue (`sim/ColonyCulture.java:80-87`) | Building synergy (`sim/ColonyEconomy.java:49-57`) | Seed and other |
|---|---|---|---|
| AMBER | DIPLOMACY_SHRINE, MARKET, PHEROMONE_ARCHIVE | knowledge += shrines + min(markets, shrines) | every player colony is AMBER (`sim/ColonyProgress.java:68-70`); the player cannot choose |
| LEAFCUTTER | FUNGUS_GARDEN, CHITIN_FARM, MARKET, PHEROMONE_ARCHIVE | food += 2·gardens; fungus += gardens | fungus seed 28 |
| FIRE | WATCH_POST, ARMORY, MARKET, PHEROMONE_ARCHIVE | venom += 2·armories + watch posts | venom seed 16; rival starting raid cooldown 120 (others 200) (`sim/ColonyProgress.java:77`); Fire-only trade `sell_venom` (`sim/ColonyTradeCatalog.java:35`) |
| CARPENTER | RESIN_DEPOT, MARKET, PHEROMONE_ARCHIVE | resin += 2·depots + min(depots, archives) | – |

- **Assignment:** rivals use `rivalFor(id)` = [LEAFCUTTER, FIRE, CARPENTER][(id − 1) mod 3] (`sim/ColonyCulture.java:89-92`). Wild colonies get a culture from `ColonyDiscoveryService.cultureFor(seed, site)` (extraction 03).
- **Colony names:** allied "Amber Burrow <id>", rival "<displayName> <id>", wild "<displayName> Wild Nest <id>" (`sim/ColonyProgress.java:68-82`).
- Culture also drives architecture style through `StructurePlacer` (extraction 02).
- **Guide text:** "Paths shift food, resin, venom, diplomacy." / "Пути меняют еду, смолу, яд, дипломатию." (`en_us.json:203`).
- The roadmap promised more (e.g. Fire: "raids, defense pressure, aggressive borders"; Carpenter: "repairs"; `docs/roadmap.md:370-380`). Only the numeric bonuses above exist. There is no culture-specific AI behaviour, and there is no "mood" (a test asserts no mood; `test/ColonyEconomyTest.java:450-467`).

---

## 9. Personalities (`ColonyPersonality`) — COSMETIC

`sim/ColonyPersonality.java:6-9`:

| Personality | id | Label EN / RU (`en_us.json:96-103`) | Detail EN / RU | Colour (unused) |
|---|---|---|---|---|
| STEADY | steady | Steady / Ровный | Balanced growth / Сбалансированный рост | 0xD8B57A |
| CURIOUS | curious | Curious / Любопытный | Explores often / Часто разведывает | 0x8EC8D6 |
| INDUSTRIOUS | industrious | Industrious / Трудолюбивый | Builds early / Рано строит | 0xD69042 |
| GUARDED | guarded | Guarded / Настороженный | Watches borders / Следит за границами | 0xC15D48 |

- **Formula (not stored):** `values()[floorMod(id + cultureSeed + alliedSeed − 1, 4)]` (`:39-48`).
  - cultureSeed: AMBER 0, LEAFCUTTER 1, CARPENTER 2, FIRE 3. This is not enum order.
  - alliedSeed: 0 if player-allied, else 1.
  - `ColonyIdentity.personality` wraps it (`sim/ColonyIdentity.java:7-9`).
- **Used only for display:**
  - tablet header "label · detail" (`screen/ColonyStatusScreen.java:371`);
  - floating label over the Queen Chamber "<name>\n<personality> | <relationship>" (`world/ColonyLabelService.java:61-73`);
  - debug `statusText` (`sim/ColonyData.java:198`).
- **No gameplay code reads it.** The details promise behaviours ("Explores often", "Builds early", "Watches borders") that don't exist. The `color` field is unused.

---

## 10. Identity and relationship labels (`ColonyIdentity`) — IMPLEMENTED (display)

`relationshipId` (`sim/ColonyIdentity.java:15-33`):
- Not player-allied: "wild" if faction = wild, else "rival".
- Player-allied, by reputation: ≥ 50 "trusted", ≥ 15 "friendly", < 0 "strained", else "new_allies".

| id | EN / RU (`en_us.json:90-95`) | Colour (`sim/ColonyIdentity.java:35-44`) |
|---|---|---|
| new_allies | New allies / Новые союзники | 0xC9974B |
| friendly | Friendly / Дружелюбно | 0x91C46C |
| trusted | Trusted / Доверие | 0x6DD08E |
| strained | Strained / Напряжено | 0xD69042 |
| rival | Rival colony / Колония-соперник | 0xC15D48 |
| wild | Wild colony / Дикая колония | 0xD8B57A |

- Reputation is clamped to −100..100 (`sim/ColonyProgress.java:223-225`).
- Starting reputation: allied 0, rival −10, wild −2 (`:68-82`).
- Reputation comes from player gifts, trades and contracts, caravans (+1 per shipment per second) and the autonomous envoy (+3 per action per second). Details in extraction 03.

---

## 11. Guide chapters (`GuideChapter`) — PARTIAL (one-liners; lock states unreachable)

`sim/GuideChapter.java:4-11` defines 8 chapters. Each has a title key, a detail key, a locked key and a colour. The tablet "Guide" tab (EN "Guide" / RU "Справочник") draws **one row per chapter**: title, one ellipsised detail line and an "Open" / "Locked" pill ("Open" / "Откр.", "Locked" / "Закр."; `screen/ColonyStatusScreen.java:681-702`, `en_us.json:213-214`). The one-line texts below are **the complete guide content**; there is no longer text and no book item.

| # | id | Colour | Title EN / RU | Detail EN | Detail RU | Unlock rule |
|---|---|---|---|---|---|---|
| 1 | first_steps | 0xF0C26E | First steps / Первые шаги | Place Queen Egg, open tablet, watch tasks. | Поставьте яйцо, откройте планшет, смотрите задачи. | always |
| 2 | castes | 0xD8B57A | Castes / Касты | Caste roles: build, haul, scout, defend. | Рабочие строят; шахтеры носят; разведчики ищут; солдаты бьются. | always |
| 3 | resources | 0x91C46C | Resources / Ресурсы | Food/ore/chitin; resin/fungus/venom/know. | Еда, руда, хитин, смола, грибы, яд, знание. | always |
| 4 | buildings | 0xD69042 | Buildings / Постройки | Mounds, stores, nurseries, mines, defenses. | Курганы, склады, ясли, шахты, оборона. | always |
| 5 | cultures | 0xB58BFF | Cultures / Культуры | Paths shift food, resin, venom, diplomacy. | Пути меняют еду, смолу, яд, дипломатию. | always |
| 6 | helping | 0x6DD08E | Helping / Помощь | Donate supplies; trade; set instincts. | Несите припасы; торгуйте; меняйте инстинкты. | always |
| 7 | relations | 0x8EC8D6 | Relations / Отношения | Tokens and seals shift allies, rivals, wars. | Жетоны и печати меняют союзы, соперничество, войны. | known colonies not empty, or reputation ≠ 0, or rank ≥ BURROW |
| 8 | research | 0xC9974B | Research / Исследования | Archive plus knowledge unlocks paths. | Архив и знания открывают пути. | archive completed, or research active, or any research done, or knowledge > 0 |

- Text lines: `en_us.json:194-211`. Unlock logic: `sim/GuideChapter.java:47-58`.
- Locked texts: relations "Meet another colony or raise reputation to unlock diplomacy notes." / "Встретьте другую колонию или повысьте репутацию, чтобы открыть дипломатию."; research "Build Archive or gain Knowledge to unlock." / "Постройте феромонный архив или получите знания, чтобы открыть исследования." (`en_us.json:208, 211`).
- `formic_frontier.guide.unlock.always` ("Available" / "Доступно", `:212`) is the locked key of the six always-open chapters, so it is never displayed.
- The RU castes line is more specific than EN (workers build, miners haul, scouts search, soldiers fight). That RU text contradicts the code: workers haul and build, miners haul ore.
- **Unreachable locks:** a live colony starts at BURROW with 8 knowledge, so both locks are already open at founding. Only unit-test fixtures show them locked (`test/ColonyEconomyTest.java:267-293`).
- Roadmap intent: "`Formic Field Guide` or `Colony Codex`, chapters, progress unlock hooks" (`docs/roadmap.md:273-281`).

---

## 12. What `ColonyData` persists

**`ColonyData`** codec (`sim/ColonyData.java:15-27`):

| Key | Type | Default / notes |
|---|---|---|
| `id` | int | – |
| `origin` | BlockPos | anchored to the surface at founding (`world/ColonyService.java:95-102`) |
| `resources` | map resource id → int | all 7 types initialised to 0 by the constructor |
| `castes` | map caste id → int | all 7 initialised to 0 |
| `chambers` | list of `NestChamber{type: string, pos, level}` | written at founding: `queen_hall`, `food_chamber`, `nursery`, `mine_chamber`, `barracks` (`world/ColonyService.java:692-696`); **never read by gameplay** (DEAD) |
| `priorities` | list of TaskPriority | [food, ore, chitin, defense] |
| `queenHealth` | int | 140 |
| `ageTicks` | int | +20 per ET |
| `currentTask` | optional string | constructor "Establishing nest"; decoded default "Idle" |
| `progress` | optional `ColonyProgress` | missing → `allied(id)` |
| `stage` | optional ColonyStage | founding |

**`ColonyProgress`** codec (`sim/ColonyProgress.java:13-30`):

| Key | Default / notes |
|---|---|
| `culture` | amber |
| `faction` | "wild" (codec default) — the real values are "allied", "rival", "wild" |
| `name` | "Unnamed Colony" |
| `color` | 0x8a5b32 — **never read** (DEAD) |
| `playerAllied` | false |
| `reputation` | 0, clamped ±100 |
| `claimRadius` | 18, clamped 18..48 |
| `buildings` | list of `ColonyBuilding{type, pos, level = 1, constructionProgress = 100, disabledTicks = 0}` |
| `buildQueue` | list of building ids |
| `knownColonies` | map "id" → DiplomacyState |
| `raidPlans` | list of `RaidPlan` — **never populated** (DEAD) |
| `raidCooldown` | int ≥ 0 |
| `events` | list of `ColonyEvent{ageTicks, message}`, newest first, **max 10** (`:196-201`) |
| `requests` | list of `ColonyRequest` |
| `completedResearch` | list of node ids |
| `activeResearch` | optional `ResearchState{nodeId, progressTicks}` |

Notes:
- Enums are saved as string ids. Their `fromId` methods throw on unknown ids (e.g. `sim/ResourceType.java:28-36`), so renaming an id breaks old saves.
- Entities save `caste`, `colonyId`, `workState` separately (§3.2).
- Personality, rank and relationship label are derived, not stored.

---

## 13. Unit tests (`test/ColonyEconomyTest.java`, 36 JUnit tests)

The main fixture `baseColony()` (`:1298-1308`): AMBER, **no buildings**, Q1 W3 M2 So2, food 120, ore 20, chitin 24. That gives rank OUTPOST and upkeep 25. It differs from a live colony (5 starter buildings, a scout and a major, BURROW rank), so several tests pin behaviour the live game never shows.

**Core scope** (castes, economy, progression, research, culture, identity, guide, persistence):

| Line | Test | What it pins |
|---|---|---|
| 14 | economyConsumesUpkeepAndProducesResources | one tick: food < before + 20; ore rises; age ≥ 20 |
| 27 | resourceTickProducesAndConsumesOverMultipleTicksAssertingRiseAndFall | queen dead (no growth): ore exactly +4/tick, food exactly −12/tick over 6 ticks (income 13, upkeep 25) |
| 84 | giantRequiresLargeEconomy | GIANT affordability: food 10 no; food 500 / ore 100 / chitin 100 yes |
| 101 | smallCastesKeepReadableRenderScaleWithoutChangingGameplaySize | heights 1.5 / 1.55 / 1.7; render scales worker < scout < miner < soldier, worker > h/1.5 |
| 113 | queenDeathSuspendsGrowth | no population change; task contains "Queen lost" |
| 128 | colonyDataRoundTripsThroughCodec | JSON round-trip of resources, castes, chambers, buildings (damaged, repairing, upgraded), requests, research, reputation, task, events, personality, relationship "friendly" (rep 20) |
| 267 | guideChaptersTeachBasicsAndUnlockAdvancedTopics | 6 open chapters; relations and research locked on the fixture; open after a relation and a completed archive exist |
| 296 | defensePriorityRaisesSoldierBeforeBalancedGrowth | DEFENSE first → +1 soldier |
| 310 | rankReflectsBuildingsReputationAndPopulation | fixture OUTPOST; 3 buildings + rep 80 + 23 more ants → ≥ HIVE |
| 336 | logisticsRequestsConsumeResourcesUntilFulfilled | a non-player request ("unit logistics") is auto-filled from stock (resin 6 → 3) |
| 348 | requestsExposePlayerContractsWithRewardsAndReputation | research contract: priority ≥ 3, reward > 0, rep > 0; full delivery closes it |
| 373 | famineRequestsDoNotDrainEmergencyFoodStores | a famine request is not auto-filled |
| 385 | famineContractsRestoreFoodWhenPlayerHelps | famine priority ≥ 5; partial delivery of 12 adds 12 food |
| 402 | completedResearchContractStartsResearchWithDeliveredMaterials | completing a research contract auto-starts Scented Ledger and consumes knowledge and resin |
| 430 | researchRequiresArchiveResourcesAndCompletes | no archive → fails; with archive and resources Resin Masonry completes within 6 ticks |
| 450 | cultureModifiersAffectEconomyWithoutMoodState | Leafcutter makes more fungus than Amber; no "mood" in the status text |
| 470 | cultureSignatureBuildingsChangeEconomyOutputs | Amber shrine + market → knowledge ≥ 2; Leafcutter garden → more food and fungus; Fire armory + watch post → venom ≥ 4; Carpenter depot + archive → resin ≥ 8 |
| 499 | cultureStarterQueuesExposeDistinctProgressionPaths | first queue entry per culture; Leafcutter has an archive, Fire has an armory |
| 530 | casteJobLoopsChangeColonyStateEndToEnd | gather +3 food / +4 ore / +2 chitin; build progress rises; a damaged watch post recovers; queen tended ≤ max; task starts "Job loop:" |
| 593 | colonyAdvancesStageAutonomouslyAndUnlocksBuilding | 7 ticks with food 800 etc. → ≥ ESTABLISHED; queue has BARRACKS and PHEROMONE_ARCHIVE (only because the fixture has no buildings); stage event logged; queenless → frozen |
| 685 | researchUnlockEnablesEliteGiantCaste | no GIANT without MANDIBLE_PLATING; with it, the economy grows a GIANT |
| 1206 | castePopulationAutoBalancesTowardColonyNeeds | defense starving → workers become soldiers; food starving → miners become workers; population conserved; healthy colony and queenless colony are no-ops |

**Cross-area tests** (listed for completeness; covered by other extractions):

| Line | Test |
|---|---|
| 165 | buildingVisualStageDerivesFromLifecycleState |
| 175 | damagedBuildingCanEnterAndFinishRepair |
| 188 | colonyUiSnapshotExposesStructuredStateWithoutStatusParsing |
| 218 | recurringEventsAppearInOverviewWithoutShowingFoundingNoise |
| 232 | contractRowsExposeDeliveryCostAndUrgentOrder |
| 255 | wildColoniesExposeDiscoverableRelationship |
| 325 | diplomacyActionsMoveRelationsWithCostsAndRankGates |
| 509 | endgameProjectsAppearAsNamedBuildingsInUiSnapshot |
| 751 | tradeCaravanExchangesResourcesByRelationAndScarcity |
| 837 | nativeBlockFungusGardenCompostsFoodIntoFungus (pins food 5 → fungus 8, reserve 40) |
| 957 | politicsRelationsShiftFromActionsChangeTradeRate |
| 1012 | soldierWeaponLoadoutChangesCombatStats |
| 1083 | venomSpearAndArmorAdvanceCombatLoadout |
| 1127 | raidOutcomeChangesColonyStateWithCasualtiesAndRelationShift |

The comments call these "gametests", but they are plain JUnit tests on pure data. Real gametests live in `src/gametest/...` (extraction 04).

---

## 14. Cross-links to other extractions

- **Buildings (02):**
  - `BuildingType` costs, `ColonyBuilder` queue, upgrades, repair, endgame, site positions.
  - The fallback branch (WATCH_POST < 3, then ROAD) is unreachable, because `priorityExpansion` never returns null (`world/ColonyBuilder.java:251-260`). This produces endless expansion spam, and `siteFor` pushes far-out buildings ever farther from origin (e.g. CHITIN_FARM, `:304-308`).
  - Buildings completed by `CasteJobLoop` skip final structure placement (§3.1).
- **Diplomacy, trade, raids, events (03):**
  - Caravans and envoys run **every second**; the envoy gives +3 rep per action.
  - Raid numbers (§2.3); queen damage.
  - Brood bloom and famine (§2.4, §4.1).
  - Recurring events find their last run time by searching the 10-entry event log, while each ant delivery adds an event, so the log turns over quickly. Event spacing may therefore break (`world/ColonyRecurringEvents.java:86-92`, `world/ColonyService.java:414`) — likely, needs confirmation.
  - `INVASION_WARNING_WINDOW_TICKS = 20*12` is compared with `raidCooldown`, which counts once per second.
  - `ColonyDiscoveryService.tick` checks `gameTime % 900 == 0`, but only runs every 20 ticks with an arbitrary phase (`world/ColonyDiscoveryService.java:28`). It may never fire in a given session; needs confirmation.
- **Items, UI, commands (04):**
  - Tablet overview shows only the first 4 population chips (WORKER, SCOUT, MINER, SOLDIER); MAJOR, GIANT and QUEEN counts are never shown (`screen/ColonyStatusScreen.java:385-396`).
  - The "Workforce" row counts soldiers only as "guards" (`network/ColonyUiSnapshot.java:414-419`).
  - Player actions (instinct, research, contracts, trade) go to the **nearest colony** within 96–128 blocks, not the colony whose tablet is open (`world/ColonyService.java:205-231, 298-316`).
  - Commands `dump` and `resource set` act on the *first* colony.
- **Tests / QA (04):** gametests for work targets (§3.2); QA scenes create the only non-player requests.
- **Worlds (04 / 02):** colonies live in overworld saved data and all world ticks use the overworld level. A Queen Egg used in another dimension probably misplaces structures (not verified).

---

## 15. Bugs, inconsistencies, magic numbers, dead code (consolidated)

**Pacing and scaling**
1. The 1-second ET makes every "age" or "duration" threshold trivial: stages at 3 / 6 / 12 s, research 4–7 s, buildings 1–2 s with many workers.
2. Unbounded population and building growth (no caps anywhere; §6.3). Food income per worker exceeds upkeep, so there is no brake.
3. Production is triple-counted: `ColonyEconomy` caste income + `CasteJobLoop` gather + entity trips. Knowledge is double-counted while idle (`sim/ColonyEconomy.java:48` + `sim/ColonyLogistics.java:163-166`).
4. Construction is triple-counted: `ColonyBuilder` 8 + 4·W + bonus, job loop min(W, 10), entity trips +6.

**Castes and entities**
5. Scouts gather chitin in the job loop, but food in the economy and as entities.
6. GIANT is not counted as a patrol defender (`sim/CasteJobLoop.java:35`); the CHITIN instinct grows MAJORs (combat caste).
7. Abstract counts and entities are decoupled: starter entities don't match counts; growth spawns nothing; entity deaths cost nothing; the queen entity is unrelated to `queenHealth`.
8. The Royal Jelly max() rule can revive a queen whose `queenHealth` is 0 — the only recovery path, and possibly unintended.
9. The guard name uses the raw caste id (not localised).
10. Entity resin, fungus and venom trips work even when the source building doesn't exist.
11. `AntWorkGoal` has no stuck or timeout handling; targets are 38–54+ blocks away with follow range 24. Whether pathing copes is not verified.
12. Shadow radius may lag one entity behind (unverified).

**Economy**
13. Food at 0 has no consequence; upkeep is not enforced; no starvation deaths.
14. Chitin income depends on the food stock (food/25).
15. Building level never affects income.
16. `ColonyLogistics.fulfillRequests` auto-delivery is dead in live play; entity "logistics" trips don't fulfil requests.
17. The balancer's FOOD rule plus the ORE growth rule create permanent miner↔worker churn at scale.

**Progression and research**
18. Stage signature unlocks are all no-ops live. MATURE's Great Mound enqueue bypasses the CITADEL gate. The stage is invisible in the UI and has no lang keys.
19. `raidCooldown` is decremented by patrols (the colony's *own* raid timer) and again by `RaidPlanner`.
20. Scented Ledger's ×0.85 is overwritten (`multiplier = 0.75/0.9/1.25` instead of `*=`, `sim/ColonyTradeCatalog.java:92-98`, duplicated in `sim/ColonyTrades.java:196-206`). The ledger only matters at reputation 0–24. At ≥ 25 or < 0 it has no effect, and for a ledger colony, rising from 24 to 25 reputation actually raises prices (0.85 → 0.9).
21. Treaty Sigils' discount is not reflected in the tablet cost display.
22. Research descriptions don't match effects (§7.2).
23. No autonomous research, despite the "colony researches upgrades" intent.

**UI, identity, guide**
24. Personality has no effect; its colour field is unused.
25. Guide locks are unreachable live; `guide.unlock.always` is never shown.
26. `currentTask` is overwritten 3–6 times per second; `TaskPlanner` output is almost never visible.

**Dead code**
- `TaskPlanner.preferredWorkerFor` (`sim/TaskPlanner.java:35-41`).
- `ColonyService.tabletText` (`world/ColonyService.java:391-401`), and `ColonyTrades`, which is only used there.
- `BuildingType.canStart` / `consumeStartCost` (`sim/BuildingType.java:81-94`; `ColonyBuilder` has private copies).
- `*_TICK_INTERVAL` constants in `CasteBalancer`, `CasteJobLoop`, `NativeBlockRole`; `DiplomacyService.ENVOY_PASS_INTERVAL_TICKS` (`sim/DiplomacyService.java:25`).
- `ColonyEconomy.EconomyResult` (return value ignored by the live caller).
- `NestChamber` list; `RaidPlan` list; `ColonyProgress.color`.

**Magic numbers without rationale:** base food 4; 3 food per worker; food/25 chitin; balancer thresholds 20 / 2 / 2; growth targets 8 / 3 / 8 / 2 / 6 / 3 + 2·barracks / 4; food > 250 for giants; chitin > 30 for majors; construction 8 + 4·W; repair start 55; contract reward formulas; claim radius 18 + 6·level.

---

## 16. Design intent vs. reality

Intent sources: `docs/content-intent/formic-content-intent.md:19-52`, `docs/roadmap.md`, `docs/mvp-architecture.md`, code comments ("Content pillar", "Content row …"). The comments show each system was added by an autonomous AI loop to satisfy one narrow acceptance row with a unit test (`docs/content-intent/content-feature-matrix.template.json`). That explains why mechanics are stacked instead of integrated.

| Intent | Reality |
|---|---|
| "A living ant settlement that grows on its own over time … would keep developing even if left alone" | It does develop alone, but explosively: CITADEL in ~30 s, all endgame wonders in ~1 min, then +1 ant/s forever and endless building spam (§6.3). No pacing, caps or decay. |
| Distinct castes incl. "nurse/brood-tender, forager, builder" plus an elite caste unlocked by research | 7 castes. Nurse, forager and builder are just formulas on the WORKER count. The elite GIANT is research-gated (this part works). |
| "Each caste has a job loop (gather/build/patrol/tend) that measurably changes colony state" | Implemented as an extra additive pass (`CasteJobLoop`) on top of an economy that already pays castes, plus cosmetic entity trips. Three overlapping sources of the same effects. |
| "Caste population should shift with colony needs (auto-balancing)" | `CasteBalancer` converts ants only on hard starvation thresholds; at scale it churns miner↔worker every second. |
| "Storage buildings have capacity; overflow and shortage have consequences" | No capacity, no overflow; shortage only blocks spending (no deaths). |
| Stages "over ticks/days" unlock new buildings/castes/recipes | Stages are reached in seconds; the three unlocks are no-ops live; the stage is not shown to the player. |
| Research "spends knowledge to unlock castes, blocks, weapons, and diplomacy options" | Seven nodes with real but small effects (income, costs, giant caste, armory loadout, trade offers, diplomacy cost). Player-started only; 4–7 s each; some descriptions mismatch. |
| Cultures "look and play differently" | Small numeric bonuses, a starter queue and one building synergy each; architecture differs (extraction 02). The player colony is always Amber. |
| Millenaire-style named colonies with personality and current goal | Names and relationship labels exist. Personality is a derived label with no behaviour; "current goal" is whichever pass wrote last. |
| Guide book with milestone unlocks | 8 one-line rows in the tablet; all unlocked at founding in live play. |
| Visible work (`ColonyWorkTask`: resource, logistics, construction, repair, patrol, forage, trade) | 4 work kinds with poses, particles and carried-item cues (good). But ~7 entities represent hundreds of abstract ants, and repair and trade work kinds don't exist. |
| Queen and brood fantasy | The queen is a health number and an "alive" gate; there is no egg-laying, brood or nursery gating of growth. |
| Upright humanoid ants with tools and armour (reference art) | One upright cuboid rig with caste proportions, crest, plate, wings and 4-tone palettes; no tools or armour meshes. |

---

## 17. Worth keeping for a rebuild

- **Instinct priority list** as the player's main indirect lever. It reorders growth, balancing and the builder's expansion focus. Setting it by dusting a specific caste with Pheromone Dust is a charming touch.
- **Caste data table** (size, HP, speed, damage, upkeep, cost, render scale) with a research-gated elite caste (Giant via Mandible Plating), and caste combat weights for abstract battles (4 / 8 / 16).
- **One rig, many castes:** per-caste part scaling, optional parts (crest, plate, wings), 4-tone generated palettes and caste eye colours. Cheap, readable variety. Keep the rule that small castes render slightly larger than their hitbox.
- **Work-state visual language:** WORKING / CARRYING / PATROLLING poses, item particles showing what an ant carries, and a synced work state. Players can read ant jobs without UI.
- **Colony requests as player contracts** with priority by reason, pro-rated token and reputation rewards, and item bundles per resource. Especially good: finishing a research contract auto-starts the research.
- **Derived rank** (buildings, population, reputation, queen health, allies) that feeds claim radius, an economy bonus, diplomacy gates and endgame gates. It needs rebalancing, because population and buildings dominate.
- **Cultures as modifier + starter queue + building synergy** (Amber shrine + market → knowledge; Leafcutter gardens; Fire armories and watch posts → venom; Carpenter depots + archives → resin). Easy to extend with real behavioural differences.
- **Small two-column research map** where each node is tied to a chamber and touches a different system (economy, construction, combat loadout, trade, diplomacy).
- **Armory loadout derivation:** building throughput (4 per armory), with research deciding the weapon and armour. It links items, research and abstract combat.
- **Food reserve guard** on conversions (fungus garden never takes food below 40), and a hard stop when the queen is lost (growth, stage and balancer freeze).
- **Deterministic, pure-data sim with string-id codecs**, unit-testable without Minecraft. Keep it, but add an explicit time scale, caps, and a single owner for each effect (no triple counting).
