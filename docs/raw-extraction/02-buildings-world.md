# 02 — Buildings, nest structures, colony founding/layout, world placement

Extraction from the old "Prime Ants" / `formic_frontier` project for a from-scratch rebuild.

- Repo (read-only): `C:\Users\user\Documents\Codex\2026-04-26\new-chat`, branch `rebuild/anthills-from-scratch` at `75a70f7` (2026-07-13). `master` = `946ea44` (it is the merge-base; master has no commits the rebuild branch lacks).
- Citations are `path:line`, relative to the repo root. `src/main/java/com/formicfrontier/` is abbreviated as `…/` where it helps readability.
- Status tags: **[IMPL]** implemented and reachable in normal play · **[PARTIAL]** · **[STUB]** · **[DEAD]** dead code / unreachable · **[QA-ONLY]** only used by visual-QA scenes · **[BUG]** confirmed from code · **[LIKELY-BUG]** reasoned from code + vanilla behaviour, not verified at runtime.
- Axes: Minecraft convention, +X east, +Z south, **−Z north**. Every building entrance ("mouth") lies on the **−Z side (north)**. The code and docs call these "south-facing" mouths (`…/world/structure/TieredMoundBlueprint.java:381-382` requires `frontZ < 0`; `docs/llm-minecraft-building-workflow.md:57`). That word is wrong for the world axes: ant patrol points use `.north(n)` (`…/entity/AntEntity.java:390-398`), and QA "front" cameras sit at −Z (`…/qa/VisualQaScenes.java:1500`).
- Time: a "tick" is a game tick (1/20 s). The colony simulation runs a **pass every 20 game ticks (1 s)**: `tickEconomy` and then `tickWorld` (`…/FormicFrontier.java:34-47`). Every "per pass" number below is therefore also "per second".
- **Measured sizes** (W×D×H, footprint, volume) come from a scratch Python re-implementation of the Java geometry (`contains`/`isSolid`, including the 64-bit noise). It reproduces every size invariant the unit tests assert (for example stage 1 is 24 tall, 21 wide and 17 deep; the trade hub footprint is 436). Treat the numbers as reliable but derived.

---

## 0. Key facts

1. There are **18 building types** (`…/sim/BuildingType.java:8-25`). 17 are real structures; ROAD is a single dirt-path block. 16 types are compiled from **data-driven "tiered mound" blueprints**:
   - 13 role families with 2–3 variants each;
   - the trade hub (1 variant);
   - the central mound in two stages: QUEEN_CHAMBER is stage 1, GREAT_MOUND is stage 2, placed over it.

   The Queen Vault uses a separate underground blueprint format.
2. The colony **builds itself**. Every second the builder adds `8 + 4×workers + cultureBonus` percent to the one active building, the caste job loop adds `min(workers,10)` more, and ants add +6 per construction delivery. **Every building type takes the same time** (about 3–6 s), whatever it costs.
3. After the starter set, expansion **never stops**. The top "instinct" priority picks one building type and the colony builds it forever: chitin farms, roads, nurseries or barracks. Repeated sites follow extrapolated coordinate formulas, and some of them **collide** with other buildings (for example nursery #2 × pheromone archive, barracks #3 × watch post #1).
4. Founding **erases a radius-72 circle up to 10 blocks above the origin**, removing any "replaceable" block, including stone, logs, cobblestone and gold blocks. It then places the whole campus **at the origin's Y with no terrain adaptation**: no per-site surface anchoring and no fill below. Wild-colony discovery does the same thing 56–96 blocks from a player.
5. Visual stages are mostly **invisible in the world**. PLANNED shows a dirt-path pin and CONSTRUCTION a single marker block. COMPLETE and UPGRADED look identical. DAMAGED and REPAIRING change nothing that is visible. Only floating text labels show the state, and those labels have lifecycle bugs.
6. **Culture is ignored** by every structure placer (`…/world/StructurePlacer.java:178-192`). Cultures differ only in starter queues, economy bonuses and the blocks used for wild-colony surface landmarks.
7. The saved state is one overworld `SavedData` holding a list of colonies (`formic_frontier_colonies`). It stores only building *records* (type, center, level, progress, damage). The world geometry is re-derivable from type + position.
8. Automatic wild-colony discovery is gated by `gameTime % 900 == 0` but only evaluated every 20 server ticks. It will probably **never fire in about 19 of 20 sessions** (§7.4, [LIKELY-BUG]).

---

## 1. Building catalog (`BuildingType`)

### 1.1 Constants, costs, structures, names

Constructor `(id, food, ore, chitin[, resin, fungus, venom, knowledge])`, missing values = 0 (`…/sim/BuildingType.java:38-51`). Label keys come from `…/network/ColonyUiSnapshot.java:285-306`. EN/RU strings come from `src/main/resources/assets/formic_frontier/lang/en_us.json` / `ru_ru.json`.

| Constant (`id`) | Food | Ore | Chitin | Resin | Fungus | Venom | Know. | Structure when COMPLETE | Center / functional block | Label key → EN / RU |
|---|---|---|---|---|---|---|---|---|---|---|
| QUEEN_CHAMBER (`queen_chamber`) | 0 | 0 | 0 | 0 | 0 | 0 | 0 | `queen_mound_stage_1` | NEST_MOUND at center, NEST_CORE below | `formic_frontier.building.queen_hall` → Queen Hall / Зал матки |
| FOOD_STORE (`food_store`) | 24 | 0 | 0 | 0 | 0 | 0 | 0 | `food_store_a/b` | FOOD_CHAMBER | `block.formic_frontier.food_chamber` → Food Chamber / Пищевая камера |
| NURSERY (`nursery`) | 32 | 0 | 8 | 0 | 0 | 0 | 0 | `nursery_a/b` | NURSERY_CHAMBER | `block.formic_frontier.nursery_chamber` → Nursery Chamber / Ясли |
| MINE (`mine`) | 20 | 16 | 0 | 0 | 0 | 0 | 0 | `mine_a/b` | MINE_CHAMBER | `block.formic_frontier.mine_chamber` → Mine Chamber / Шахта |
| CHITIN_FARM (`chitin_farm`) | 24 | 4 | 12 | 0 | 0 | 0 | 0 | `chitin_farm_a/b/c` | CHITIN_BED | `block.formic_frontier.chitin_bed` → **Chitin Bed** / Хитиновая грядка |
| BARRACKS (`barracks`) | 36 | 18 | 16 | 0 | 0 | 0 | 0 | `barracks_a/b` | BARRACKS_CHAMBER | `block.formic_frontier.barracks_chamber` → Barracks Chamber / Казармы |
| MARKET (`market`) | 28 | 10 | 6 | 0 | 0 | 0 | 0 | `market_a/b` | MARKET_CHAMBER | `block.formic_frontier.market_chamber` → Market Chamber / Рынок |
| DIPLOMACY_SHRINE (`diplomacy_shrine`) | 40 | 14 | 24 | 0 | 0 | 0 | 0 | `diplomacy_shrine_a/b` | DIPLOMACY_SHRINE | `block.formic_frontier.diplomacy_shrine` → Diplomacy Shrine / Святилище дипломатии |
| WATCH_POST (`watch_post`) | 18 | 12 | 8 | 0 | 0 | 0 | 0 | `watch_post_a/b/c` | WATCH_POST | `block.formic_frontier.watch_post` → Watch Post / Сторожевой пост |
| RESIN_DEPOT (`resin_depot`) | 32 | 8 | 8 | 0 | 0 | 0 | 0 | `resin_depot_a/b` | RESIN_DEPOT | `block.formic_frontier.resin_depot` → Resin Depot / Склад смолы |
| PHEROMONE_ARCHIVE (`pheromone_archive`) | 48 | 16 | 20 | 8 | 4 | 0 | 0 | `pheromone_archive_a/b` | PHEROMONE_ARCHIVE | `block.formic_frontier.pheromone_archive` → Pheromone Archive / Феромонный архив |
| FUNGUS_GARDEN (`fungus_garden`) | 28 | 0 | 8 | 4 | 0 | 0 | 0 | `fungus_garden_a/b` | FUNGUS_GARDEN | `block.formic_frontier.fungus_garden` → Fungus Garden / Грибной сад |
| VENOM_PRESS (`venom_press`) | 36 | 18 | 18 | 12 | 6 | 0 | 0 | `venom_press_a/b` | VENOM_PRESS | `block.formic_frontier.venom_press` → Venom Press / Ядовитый пресс |
| ARMORY (`armory`) | 42 | 28 | 24 | 16 | 0 | 4 | 0 | `armory_a/b` | ARMORY | `block.formic_frontier.armory` → Armory / Оружейная |
| GREAT_MOUND (`great_mound`) | 120 | 48 | 96 | 48 | 24 | 0 | 36 | `queen_mound_stage_2`, placed at the origin over stage 1 | NEST_MOUND + NEST_CORE | `formic_frontier.building.great_mound` → Great Mound / Великий курган |
| QUEEN_VAULT (`queen_vault`) | 80 | 36 | 120 | 42 | 18 | 0 | 48 | `queen_vault` (underground, at the origin) | restores NEST_MOUND/NEST_CORE at the origin | `formic_frontier.building.queen_vault` → Queen Vault / Хранилище матки |
| TRADE_HUB (`trade_hub`) | 96 | 30 | 72 | 54 | 12 | 0 | 54 | `trade_hub` | MARKET_CHAMBER | `formic_frontier.building.trade_hub` → Trade Hub / Торговый узел |
| ROAD (`road`) | 4 | 0 | 0 | 0 | 0 | 0 | 0 | one DIRT_PATH block | DIRT_PATH | `formic_frontier.building.road` → Road / Дорога |

Cost rules:
- Costs are paid **in full when construction starts**. Nothing is paid per step (`…/world/ColonyBuilder.java:263-284`, `:399-403`).
- The research **RESIN_MASONRY** changes every non-zero cost to `max(1, ceil(cost×0.9))` (`ColonyBuilder.java:419-425`).
- **[BUG, cosmetic]** The UI cost text shows the *undiscounted* `type.cost()` (`…/network/ColonyUiSnapshot.java:366-376`).
- **[DEAD]** `BuildingType.canStart` and `consumeStartCost` (`BuildingType.java:81-94`) are never called. `ColonyBuilder` has private copies instead, which add the discount and the VENOM_PRESS research gate.
- `BuildingType.fromId` throws on unknown ids (`BuildingType.java:96-104`). The codec is a plain `xmap`, so **a save containing an unknown building id throws while loading** instead of returning a codec error.

### 1.2 What each completed building does in gameplay

General rules:
- All effects count only buildings with `complete()`, meaning progress ≥ 100.
- **Every completed building gives +10 rank score** (`…/sim/ColonyRank.java:50-58`).
- Economy numbers are per pass (1 s) and come from `…/sim/ColonyEconomy.java:32-57` unless noted.
- A *damaged* building keeps counting as complete until a repair begins (§2.2).
- **No building has storage capacity.** Resources are unbounded integers clamped only at ≥0 (`…/sim/ColonyData.java:85-87`).

| Building | Effects (cross-links) | Status |
|---|---|---|
| QUEEN_CHAMBER | Sets the claim radius to `18 + 6×level` (`ColonyBuilder.java:48`; display-only, §3.1). Scouts deliver FOOD to it (`AntEntity.java:341`). Patrol point at `pos.north(17)` (`AntEntity.java:394`). Never targeted by raid damage (`…/world/RaidPlanner.java:393`). Its contracts get priority 3 (`…/sim/ColonyContract.java:91`). | [IMPL] |
| FOOD_STORE | +2 food each. Delivery target for workers' FOOD trips (`AntEntity.java:368`). The famine event's request is filed against it (`…/world/ColonyRecurringEvents.java:313-319`). | [IMPL] |
| NURSERY | +1 chitin and +1 fungus each (`ColonyEconomy.java:34,39`). Required building for research CHITIN_CULTIVATION (`…/sim/ResearchNode.java:9`). Gate for QUEEN_VAULT. Delivery target for workers' CHITIN/FUNGUS trips. | [IMPL] |
| MINE | +1 ore each. Delivery target for miners (`AntEntity.java:339`). | [IMPL] |
| CHITIN_FARM | +5 chitin each; with CHITIN_CULTIVATION a further `+max(1, 2×farms)` (`ColonyEconomy.java:34-37`). Unlocks chitin armour for armed soldiers (`…/sim/ColonyArmory.java` ~l.100). Its centre block CHITIN_BED can be harvested by players (§10.3). | [IMPL] |
| BARRACKS | Under the DEFENSE instinct the soldier growth target is `3 + 2×barracks` (`ColonyEconomy.java:136-146`). Raid defence +4 each (`RaidPlanner.java:433-444`). Patrol priority 0, patrol point `north(13)`. The invasion-warning event requires one (`ColonyRecurringEvents.java:263-273`). It is the GROWTH stage's "signature" unlock (`…/sim/ColonyStage.java:25`), but it is built at founding, so that unlock never does anything. | [IMPL] |
| MARKET | +1 food each. For AMBER, knowledge `+min(markets, shrines)`. Enables autonomous colony-to-colony caravans (`…/sim/TradeCaravan.java:41`) and the trade-caravan event, which needs a market on both sides (`ColonyRecurringEvents.java:291-303`). Required for research SCENTED_LEDGER. Gate for GREAT_MOUND and TRADE_HUB. Required for the expansion event. | [IMPL] |
| DIPLOMACY_SHRINE | +1 chitin each. For AMBER, +1 knowledge each. At rank BURROW or higher the colony sends an autonomous ENVOY to its nearest non-allied colony (`…/sim/DiplomacyService.java:66-82`). Required for research TREATY_SIGILS. Gate for GREAT_MOUND and TRADE_HUB. | [IMPL] |
| WATCH_POST | Raid defence +6 each. For FIRE, +1 venom each. Patrol point `north(6)`. The expansion event requires one, and its reward is a free watch post (§4.9). | [IMPL] |
| RESIN_DEPOT | +4 resin each. For CARPENTER, `+2×depots + min(depots, archives)`. If at least one exists, logistics throughput +3 (`…/sim/ColonyLogistics.java:126-128`). Source block for workers' resin trips. | [IMPL] |
| PHEROMONE_ARCHIVE | **Required for any research** (`ResearchNode.java:65`, `ColonyLogistics.java:93`). Knowledge when no research is active: `+archives` in the economy (`ColonyEconomy.java:48`) **and** `+(1+archives)` in logistics (`ColonyLogistics.java:163-165`), **[BUG] double income**. Research speed is `20 + 10×archives` progress per pass (`ColonyLogistics.java:170`). ESTABLISHED stage signature. Gate for GREAT_MOUND and QUEEN_VAULT. Unlocks the guide's Research chapter (`…/sim/GuideChapter.java:53`). | [IMPL] |
| FUNGUS_GARDEN | +2 food and +4 fungus each. With FUNGUS_SYMBIOSIS: `+max(1, 3×gardens)` food and +2 fungus. For LEAFCUTTER: +2 food and +1 fungus per garden. In addition, `NativeBlockRole` composts **5 food → 8 fungus per garden per pass** while food exceeds the reserve of 40 (`…/sim/NativeBlockRole.java:26-58`; `wantedFood` at l.45 is unused). | [IMPL] |
| VENOM_PRESS | +3 venom each. **Construction cannot start without research VENOM_DRILLS** (`ColonyBuilder.java:388-390`). | [IMPL] |
| ARMORY | With VENOM_DRILLS, venom `+max(1, soldiers/3 + armories)` (`ColonyEconomy.java:45-47`). For FIRE, +2 venom per armory. Arms up to 4 combat ants per armory (`ColonyArmory.java:43`); weapon type depends on research. Required building for VENOM_DRILLS and MANDIBLE_PLATING. Gate for GREAT_MOUND. | [IMPL] |
| GREAT_MOUND | No production of its own. +10 rank. Prerequisite for QUEEN_VAULT and TRADE_HUB. MATURE stage signature (`ColonyStage.java:29`). | [IMPL] (a prestige and unlock step) |
| QUEEN_VAULT | Reduces raid damage to the queen by 3 (`RaidPlanner.java:405-410`). | [IMPL] |
| TRADE_HUB | Pheromone-token prices ×0.85 and +1 token on token-paying offers whose output count is below 8 (`…/sim/ColonyTradeCatalog.java:12-13, 99-113`). | [IMPL] |
| ROAD | +10 rank. A patrol building with priority 2 (`AntEntity.java:400-414`). | [IMPL] |

**Building level (upgrades) has no economic effect** (§3.4).

Player-crafted building blocks do nothing for the colony (§10.1).

### 1.3 How a building type gets into the build queue

The **build queue** (`ColonyProgress.buildQueue`) is consumed from its head. Types enter it from these sources:

1. **Founding** places QUEEN_CHAMBER, FOOD_STORE, NURSERY, MINE and BARRACKS as already complete (`…/world/ColonyService.java:655-698`).
2. **The culture starter queue** is appended at founding (`ColonyService.java:697`; `…/sim/ColonyCulture.java:80-87`):

   | Culture | Starter queue |
   |---|---|
   | AMBER | DIPLOMACY_SHRINE, MARKET, PHEROMONE_ARCHIVE |
   | LEAFCUTTER | FUNGUS_GARDEN, CHITIN_FARM, MARKET, PHEROMONE_ARCHIVE |
   | FIRE | WATCH_POST, ARMORY, MARKET, PHEROMONE_ARCHIVE |
   | CARPENTER | RESIN_DEPOT, MARKET, PHEROMONE_ARCHIVE |

   Player colonies are always AMBER (`…/sim/ColonyProgress.java:68-70`).
3. **Automatic enqueue** (`ColonyBuilder.enqueueNextBuilding`, `:222-261`) only runs when the queue is empty and no building is incomplete. It enqueues one type, chosen by these rules in order:
   1. The first *not yet completed* type in STARTER_SEQUENCE: QUEEN_CHAMBER, FOOD_STORE, NURSERY, MINE, CHITIN_FARM, BARRACKS, MARKET, PHEROMONE_ARCHIVE, ARMORY, DIPLOMACY_SHRINE (`:20-31`).
   2. RESIN_DEPOT if RESIN_MASONRY is researched and fewer than 1 are completed.
   3. FUNGUS_GARDEN if FUNGUS_SYMBIOSIS is researched and fewer than 2 are completed.
   4. VENOM_PRESS if VENOM_DRILLS is researched and fewer than 1 are completed.
   5. An endgame project, if one is due (§3.5).
   6. `priorityExpansion` (§3.6). **This always returns a type**, so the rules after it are **[DEAD]**: `WATCH_POST if <3`, then `ROAD` (`:256-260`).
4. **Stage progression** appends the new stage's "signature" building if it is not already built, queued or planned (`…/sim/ColonyStageProgression.java:58-71`): GROWTH → BARRACKS, ESTABLISHED → PHEROMONE_ARCHIVE, MATURE → GREAT_MOUND.
   - This path **bypasses the CITADEL gate** that ColonyBuilder applies to the Great Mound.
   - Stage thresholds (`ColonyStage.java:23-29`):

     | Stage | Age (passes) | Food | Ore + chitin + resin |
     |---|---|---|---|
     | GROWTH | 3 | 120 | 40 |
     | ESTABLISHED | 6 | 400 | 180 |
     | MATURE | 12 | 900 | 500 |

   - Age is measured in 20-tick passes, so 3–12 seconds. Stages are effectively gated by resources only.
5. The **expansion event** adds a free, already-complete WATCH_POST (§4.9).

Research is started only by the player (tablet or `/formic research`), or by completing a "research X" contract (`ColonyLogistics.java:202-218`). Wild and rival colonies therefore never research. They never build a resin depot, fungus garden or venom press unless their culture's starter queue contains one.

### 1.4 Build time

Progress sources, all per 20-tick pass:

- **ColonyBuilder**: `+ (8 + 4×max(1, workers) + culture.constructionBonus)` (`ColonyBuilder.java:69-70`). The construction bonus is AMBER 0, LEAFCUTTER 0, FIRE 1, CARPENTER 2 (`ColonyCulture.java:9-12`).
- **CasteJobLoop.build**: `+min(workers, 10)` to the first incomplete building (`…/sim/CasteJobLoop.java:22, 60-76`). It runs earlier in the same pass (`tickEconomy` before `tickWorld`).
- **Worker ants** (physical entities): each finished CONSTRUCTION job deposits `+6` (`AntEntity.java:363-366` sets amount 6 and duration 30; `ColonyService.java:419-443`). This is not deterministic.

Examples:
- An AMBER colony at founding has 3 workers: 8+12+3 = **23 per second**, so about 5 passes plus 1 pass to start ≈ **6 s per building**.
- With 8 workers: 8+32+8 = 48 per second, about **3 s**.

**Every type builds at the same speed.** The Trade Hub takes exactly as long as a Road. Only the up-front resource cost differs.

---

## 2. Building instances and visual stages

### 2.1 `ColonyBuilding` (`…/sim/ColonyBuilding.java`)

- Fields and codec defaults (`:8-14`): `type`, `pos` (the building centre at y = ground), `level` (default 1, clamped ≥1), `constructionProgress` (default 100, clamped 0..100), `disabledTicks` (default 0, clamped ≥0). Clamping happens in the constructor, `:22-28`.
- Factories: `complete()` creates level 1, progress 100. `planned()` creates level 1, progress 0 (`:30-36`).
- State checks: `complete()` means progress ≥ 100. `damaged()` means `disabledTicks > 0`. `repairing()` (`:66-68`) is **[DEAD]** (never called).
- `addConstructionProgress(n)` adds and clamps. `disableFor(t)` sets `disabledTicks = max(disabledTicks, t)`.
- `beginRepair(p)` works only if the building is damaged and complete; it sets progress to `clamp(p, 0, 99)` (`:82-86`).
- `repair(n)` adds progress. When the building becomes complete it clears `disabledTicks` and returns true (`:88-98`).
- `tickDisabled()` subtracts 1. `upgrade()` does `level++` and resets progress to 0 (`:106-109`).
- **"disabledTicks" are not game ticks.** Only `CasteJobLoop.patrol` decrements them, by `1 + min(soldiers+majors, 3)` per pass (`CasteJobLoop.java:78-98`). Raid damage of 180 (`RaidPlanner.java:399`) therefore lasts 90 s with 1 defender, 60 s with 2 and 45 s with 3 or more. With **no defenders it lasts forever**, until a repair completes.

### 2.2 Visual stages (`…/sim/BuildingVisualStage.java`)

Derivation order (`:31-48`):

| Stage | Condition | UI key `formic_frontier.ui.status.<id>` EN / RU | Label (`formic_frontier.label.*`) EN / RU |
|---|---|---|---|
| REPAIRING | disabledTicks>0 && !complete | Repairing / Ремонт | `repairing` "Repairing %s%%" / "Ремонт %s%%" |
| DAMAGED | disabledTicks>0 | Damaged / Повреждено | `damaged` "Damaged" / "Повреждено" |
| PLANNED | progress ≤0 && level=1 | Planned / Запланировано | `planned` "Planned" / "Запланировано" |
| CONSTRUCTION | progress ≤0 && level>1, or progress <100 | Building / Строится | `needs` "Needs %s" / "Нужно: %s" if an open request exists for that building type, else `building` "Building %s%%" / "Строится %s%%" |
| UPGRADED | complete && level>1 | Upgraded / Улучшено | `upgraded` "Upgraded" / "Улучшено" |
| COMPLETE | otherwise | Complete / Готово | `complete` "Complete" / "Готово" |

Other status keys:
- `formic_frontier.ui.status.queued` (Queued / В очереди) is used for queued types in the UI (`ColonyUiSnapshot.java:78-89`).
- `ui.status.disabled` (Disabled / Отключено) and `ui.status.building` (Building / Строится) exist in both lang files but are **referenced nowhere** (orphan keys).
- `BuildingVisualStage.CODEC` and `fromId` are **[DEAD]**; the stage is never serialized.

**What the world shows for each stage** (`StructurePlacer.placeBuilding`, `…/world/StructurePlacer.java:38-66`):

| Stage | ROAD | QUEEN_CHAMBER | GREAT_MOUND | QUEEN_VAULT | 14 organic families (13 role families + TRADE_HUB) |
|---|---|---|---|---|---|
| PLANNED | DIRT_PATH | DIRT_PATH pin at pos | DIRT_PATH pin at the origin (replaces the NEST_MOUND centre) | same as Great Mound | DIRT_PATH pin at the centre |
| CONSTRUCTION | DIRT_PATH | full stage-1 mound | **the full stage-2 mound, from the first construction pass** | **the full vault** | one marker block at the centre (the family's functional block) |
| COMPLETE | DIRT_PATH | stage 1 | stage 2 | vault | full blueprint + functional block |
| UPGRADED | as COMPLETE | stage 1 | stage 2 | vault | identical to COMPLETE |
| DAMAGED / REPAIRING | as COMPLETE | stage 1 | stage 2 | vault | marker block at the centre. `markerBlock` (`StructurePlacer.java:214-233`) equals `functionalBlock` (`…/world/structure/OrganicBuildingPlacer.java:150-168`) for every family, so **nothing visible changes** |

Consequences:
- **[BUG / design gap]** Upgrades, damage and repair are invisible in the world. Only the floating label changes.
- The three central structures appear in full as soon as construction begins, not when it finishes.
- Every re-placement calls `safeSet` with default block states. This **resets every CHITIN_BED in the structure to age 0**, destroying pending player harvests. It happens on upgrade completion, repair completion and renovation.

---

## 3. Autonomous construction (`…/world/ColonyBuilder.java`)

`ColonyBuilder.tick(level, colony)` runs **for every colony**, player, rival and wild alike, once per pass, from `ColonySavedState.tickWorld` (`…/world/ColonySavedState.java:180-191`). One building is active at a time: `firstIncomplete` in list order (`ColonyProgress.java:211-213`).

### 3.1 Order of operations inside one tick (`:46-81`)

1. **Claim radius** is set to `max(18 + 6×(max level of a completed queen chamber), rank.claimRadius)` (`:48`), clamped to 18..48 (`ColonyProgress.java:116-118`). Rank radii are OUTPOST 18, BURROW 24, HIVE 36, CITADEL 48 (`ColonyRank.java:4-7`).
   - The claim radius is only *displayed* (UI and status text) and used to position the expansion outpost. It has no territorial mechanics.
   - **[BUG]** This line recomputes the value every second. It overwrites the larger radius the expansion event just granted (§4.9).
2. **Repair** (`repairDamagedBuilding`, `:83-119`) looks at the first damaged building. If one exists, the tick ends here, even when it is only waiting for supplies.
3. The **endgame project** is computed (§3.5). If there is none, `maybeStartUpgrade` runs (§3.4); if it starts an upgrade, the tick ends.
4. `enqueueNextBuilding` (§1.3).
5. If nothing is incomplete, `startQueuedBuilding` runs (`:263-284`):
   - If the head of the queue is unaffordable (`canStart`, `:387-397`, which includes the VENOM_PRESS research gate), the task becomes "Waiting for resources to build X". It opens **player-supply requests** `construction <id>` for each missing resource (`:405-412`) and the tick ends.
   - Otherwise it clears those requests, pays the costs, pops the queue, adds `ColonyBuilding.planned(type, siteFor(colony, type))`, places the PLANNED pin, syncs labels and logs "Started construction: X".
6. It adds progress (§1.4), then re-places the structure with its current stage. On completion it syncs labels and logs "Completed X at x, y, z".

"construction …" requests are never auto-filled by logistics (`ColonyLogistics.java:190-200`). They are invitations for the player: delivering items through the tablet adds the resources to the stockpile, and the builder is triggered immediately (§12, `completeContract`).

### 3.2 Repair (`:83-160`)

- Repair cost is `max(4, chitinCost/2 + level×2)` chitin, with integer division (`:158-160`). Examples at level 1:

  | Building | Repair cost (chitin) |
  |---|---|
  | FOOD_STORE, MINE, ROAD | 4 |
  | MARKET | 5 |
  | NURSERY, WATCH_POST, RESIN_DEPOT, FUNGUS_GARDEN | 6 |
  | CHITIN_FARM | 8 |
  | BARRACKS | 10 |
  | VENOM_PRESS | 11 |
  | PHEROMONE_ARCHIVE | 12 |
  | DIPLOMACY_SHRINE, ARMORY | 14 |
  | TRADE_HUB | 38 |
  | GREAT_MOUND | 50 |
  | QUEEN_VAULT | 62 |

  Add +2 at level 2.
- **Damaged and complete:**
  - If chitin is short, it opens a request `repair <id>` for the missing amount (a player-supply request) and waits.
  - Otherwise it pays the cost, calls `beginRepair(55)` (`REPAIR_START_PROGRESS`, `:18`), re-places the structure and syncs labels.
- **Damaged and incomplete:** it adds `10 + 4×max(1, workers) + constructionBonus` (`:107-108`). On completion `disabledTicks` becomes 0 and labels are synced.
- **[BUG, economy]** A damaged building keeps producing until the repair crew starts. From `beginRepair` (progress 55) until the repair finishes it is incomplete, so it *stops* counting. Starting the repair lowers output; the damage itself does not.
- **[LIKELY-BUG]** CasteJobLoop and ant deposits add progress with `addConstructionProgress`, not `repair()`. If one of them pushes a repairing building to 100, `disabledTicks` stays above 0. The building then reads as "complete but damaged" again, the builder charges the chitin again and restarts at 55.
  - Example: with 7 workers and bonus 0, one pass brings progress from 55 to 94 (55 + 7 + 32). The next CasteJobLoop pass adds 7 and completes it.
  - The loop ends only when patrols tick `disabledTicks` down to 0.

### 3.3 Upgrades (`:162-220`)

- Upgrades only happen when the queue is empty, nothing is incomplete and no endgame project is due.
- The candidate is the **first** type in UPGRADE_ORDER that has a complete, undamaged building below level 2. The order is MARKET, RESIN_DEPOT, PHEROMONE_ARCHIVE, FOOD_STORE, NURSERY, MINE, BARRACKS, QUEEN_CHAMBER (`:32-41`). `MAX_AUTO_UPGRADE_LEVEL = 2` (`:19`).
- If that single candidate is unaffordable, no upgrade happens this tick and the builder moves on to enqueueing.
- Cost per resource is `ceil(0.6 × effectiveCost)`. RESIN gets an extra `+10 + 4×level`. PHEROMONE_ARCHIVE gets an extra +8 knowledge (`:210-220`). Level 1→2 costs without masonry:

  | Building | Food | Ore | Chitin | Resin | Fungus | Knowledge |
  |---|---|---|---|---|---|---|
  | MARKET | 17 | 6 | 4 | 14 | – | – |
  | RESIN_DEPOT | 20 | 5 | 5 | 14 | – | – |
  | PHEROMONE_ARCHIVE | 29 | 10 | 12 | 19 | 3 | 8 |
  | FOOD_STORE | 15 | – | – | 14 | – | – |
  | NURSERY | 20 | – | 5 | 14 | – | – |
  | MINE | 12 | 10 | – | 14 | – | – |
  | BARRACKS | 22 | 11 | 10 | 14 | – | – |
  | QUEEN_CHAMBER | – | – | – | 14 | – | – |

- `upgrade()` sets progress to 0. The building is then under construction (stage CONSTRUCTION) and **drops out of every "completed" count while it upgrades**. Effects during that time:
  - rank score −10;
  - a sole MARKET stops caravans;
  - a sole ARCHIVE blocks the start of new research.
- The types not listed (farms, shrines, posts, armory, venom, fungus, endgame buildings, roads) never upgrade.

### 3.4 What levels change

- `level` is read only by the repair cost, the upgrade resin cost, the claim radius (queen chamber), renovation and the UI (`grep .level()`).
- **Upgrades give no production, capacity or visual benefit** (§2.2). They cost resources and temporarily disable the building. **[BUG / design gap]**
- The queen-chamber level affects the claim radius only until the colony reaches rank HIVE (36) or higher.

### 3.5 Endgame projects (`:346-385`)

Endgame projects take precedence over research-gated buildings and priority expansion. While one is due, upgrades are suppressed.

| Project | Conditions (in addition to "none completed yet") |
|---|---|
| GREAT_MOUND | rank ≥ CITADEL (score 155); completed QUEEN_CHAMBER, CHITIN_FARM, MARKET, PHEROMONE_ARCHIVE, ARMORY, DIPLOMACY_SHRINE. It can also be queued by the MATURE stage without the rank check (§1.3). |
| QUEEN_VAULT | rank ≥ CITADEL; completed GREAT_MOUND, NURSERY, PHEROMONE_ARCHIVE |
| TRADE_HUB | rank ≥ CITADEL; completed GREAT_MOUND, QUEEN_VAULT, MARKET, DIPLOMACY_SHRINE |

When one is enqueued the colony logs "Endgame project planned: <id>" and sets the task "Planning endgame project: great mound" (the id with spaces).

Rank score is `10×completed buildings + 2×population + max(0, reputation) + queenHealth/12 + 12×allies` (`ColonyRank.java:50-58`). Thresholds: BURROW 35, HIVE 85, CITADEL 155. At founding:
- An AMBER player colony scores 81 (BURROW): 50 + 20 + 0 + 11 (queen health 140).
- A LEAFCUTTER rival or wild colony scores 85, which is already HIVE.

**Wild and rival colonies can also reach CITADEL** and then build Great Mounds, vaults and trade hubs by themselves.

### 3.6 Priority expansion and infinite growth (`:435-442`)

Top instinct = `prioritiesView().getFirst()`. The default order is FOOD, ORE, CHITIN, DEFENSE (`ColonyData.java:50-53`).

| Top instinct | Builds | Then forever |
|---|---|---|
| FOOD (default) | FOOD_STORE until 2 complete | **CHITIN_FARM** |
| ORE | MINE until 2 complete | **ROAD** (4 food, one dirt block, +10 rank each) |
| CHITIN | CHITIN_FARM until 3 complete | **NURSERY** |
| DEFENSE | WATCH_POST until 4 complete | **BARRACKS** |

- There is no cap. The colony expands whenever it is idle and can afford the next building.
- Roads are a **rank exploit**: +10 score for 4 food, and a finished road takes about 5 s. Roads go diagonally from the origin, `(7e, 0, 7e)` (`ColonyBuilder.java:342`). The first road (e=0) overwrites the queen mound's origin NEST_MOUND block with DIRT_PATH. The eighth road (e=7, at (49,49)) lands inside resin depot #1 at (50,50).
- Repeated sites march outwards along linear formulas (§4.4), beyond the cleared area and eventually beyond loaded chunks (§14).
- The instinct is set with the tablet (`ColonyService.setTopPriority`), `/formic colony priority|instinct`, or by using Pheromone Dust on an ant (§12).

---

## 4. Colony founding and layout

### 4.1 Entry points

| Path | Code | Result |
|---|---|---|
| **Queen Egg** used on a block | `…/item/QueenEggItem.java:17-36` → `createColony(level, clickedPos.above())`, allied | "Created Formic colony #N", tablet opens. The egg is consumed unless the player is in creative. The egg (stack 16) is only in the creative Spawn Eggs tab or trade offer `buy_queen_egg` (64 tokens, min reputation 40; `ColonyTradeCatalog.java:54`). There is no recipe. |
| `/formic colony create` | `…/command/FormicCommands.java:77-82` | allied colony at the command position |
| `/formic colony seed-rivals <1-6> <32-256>` | `FormicCommands.java:115-125` | `count` rivals evenly spaced on a circle of `distance` around the source; `playerAllied=false` |
| Automatic discovery | `…/world/ColonyDiscoveryService.java` (§7) | wild colony with a random culture |
| QA scenes | `VisualQaScenes` | [QA-ONLY] |

### 4.2 `createColony` steps (`ColonyService.java:51-82`)

1. `anchorToSurface(origin)` (`:95-102`) sets `y = heightmap(MOTION_BLOCKING_NO_LEAVES) − 1`, the top motion-blocking non-leaf block. **The origin is the ground block itself**; the gametest `colonyCreationAnchorsHighRequestsToGround` checks NEST_MOUND there (`src/gametest/.../FormicFrontierGameTest.java:81-94`).
   - Using a Queen Egg in a cave founds the colony on the surface above the cave.
   - A tree trunk or the water surface counts as the "surface" (a heightmap property).
2. `new ColonyData(savedState.nextId(), origin)`. The progress record comes from `ColonyProgress.allied(id)`, `rival(id, rivalFor(id))` or `wild(id, culture)` (`ColonyProgress.java:68-82`):

   | Factory | Culture | Faction | Name | Colour | Reputation | Raid cooldown |
   |---|---|---|---|---|---|---|
   | allied | AMBER | "allied" | "Amber Burrow <id>" | culture colour | 0 | 0 |
   | rival | `rivalFor(id)` = [LEAFCUTTER, FIRE, CARPENTER][(id−1) mod 3] | "rival" | "<culture display name> <id>" | culture colour | −10 | FIRE 120, others 200 |
   | wild | given | "wild" | "<display name> Wild Nest <id>" | culture colour | −2 | 240 |

   Culture display names: Amber Burrow, Leafcutter Choir, Fire Mandible, Carpenter Resin. Colours: 0xD69A22, 0x5E8F3A, 0xB83224, 0x9A6233.
3. `seedEconomy` (`:639-653`):

   | Resource | Start | Caste | Start |
   |---|---|---|---|
   | FOOD | 120 | QUEEN | 1 |
   | ORE | 20 | WORKER | 3 + workerBias (LEAFCUTTER +1) |
   | CHITIN | 24 | SCOUT | 1 + scoutBias (LEAFCUTTER +1) |
   | RESIN | 24 | MINER | 2 |
   | FUNGUS | 28 for LEAFCUTTER, else 12 | SOLDIER | 2 |
   | VENOM | 16 for FIRE, else 4 | MAJOR | 1 |
   | KNOWLEDGE | 8 | | |

4. Event "Colony founded at x, y, z".
5. `placeNest` (§4.3).
6. `spawnStarterCastes` (`:905-917`) spawns 7 entities, always the same set whatever the sim counts say:

   | Caste | Offset from origin |
   |---|---|
   | QUEEN | (0,1,−11) |
   | WORKER, named "Worker Foreman #N" | (2,1,−11) |
   | WORKER | (−2,1,−11) |
   | WORKER | (0,1,−13) |
   | MINER | (3,1,−13) |
   | SOLDIER | (−4,1,−12) |
   | MAJOR | (4,1,−12) |

   All of them stand in front of the mound's mouth. Names (`applyAntName`, `:919-932`):
   - queen: `formic_frontier.ant.queen` "Queen #%s" / "Матка #%s";
   - majors and giants: `formic_frontier.ant.guard` "%s Guard #%s" / "Страж %s #%s", where the first argument is the **raw caste id** ("major"), untranslated;
   - foreman: `formic_frontier.ant.worker_foreman` "Worker Foreman #%s" / "Старший рабочий #%s".

   The sim has 1–2 scouts and 2 miners, but no scout entity and only 1 miner entity spawn (cross-link: castes extraction).
7. **Initial relations** with every existing colony (`:74-78, 84-93`): if either side is wild → NEUTRAL; if both have the same `playerAllied` flag → NEUTRAL (allies do *not* start as ALLY); otherwise → RIVAL.
8. The colony is saved (`put`, marked dirty) and labels are synced.

There is **no site validation** for player-founded colonies: no spacing check against other colonies, no terrain check. Two eggs placed close together produce two overlapping campuses, and each one runs the radius-72 clear.

### 4.3 `placeNest` (`ColonyService.java:655-683`)

It runs in this order:

1. `clearSettlement(origin, 72, 10)` (`:711-722`) goes through every column with `x²+z² ≤ 72²` (≈16,300 columns) and every `y = origin+1 … origin+10`, calling `safeSet(AIR)`. That is about 163k block updates in one tick.
   - Any "replaceable" block is removed, following the list in §4.6: stone, cobblestone, dirt, logs, leaves, flowers, mud bricks, gold blocks, lanterns, crafting tables, fences, hay…
   - Block entities (chests, barrels, …) and non-listed blocks (sand, planks other than mangrove, glass, …) survive.
   - **[BUG, griefing]** The same happens on `renovate` and when a wild colony is discovered: its site is 56–96 blocks from a player, so the player's base can be inside the circle.
2. **Wide paths** (`placeWidePath`, `:753-786`) run from the origin to the food node, nursery, ore node, barracks, food store and mine, in that order. **The chitin node gets no path at founding**; renovation adds one.
   - Each path goes along X first, then Z. Every tile is DIRT_PATH, 3 wide (centre plus north/south on the X leg, centre plus east/west on the Z leg).
   - Every 5th tile adds COARSE_DIRT two blocks to one side and PODZOL two blocks to the other.
   - Paths are painted **at the origin's Y**. Where the ground is lower they form floating path lines, because `safeSet` places blocks in air.
3. The structures are placed at stage COMPLETE: the queen chamber at the origin, then the food store, nursery, mine and barracks at their `siteFor(…, 0)` positions. Buildings overwrite the paths inside their footprint.
4. **Resource clusters** (`placeResourceCluster`, `:724-737`) use a 5×5 area without the corners (|x|,|z| ≤2 and |x|+|z| ≤3, 21 cells at y=0):

   | Cluster | Centre | Node block (the 5 cells with abs(x)+abs(z) ≤ 1) | Base block | Accent block |
   |---|---|---|---|---|
   | Food | (54,0,8) | FOOD_NODE | MOSS_BLOCK | BROWN_MUSHROOM_BLOCK |
   | Ore | (8,0,54) | ORE_NODE | COBBLED_DEEPSLATE | IRON_ORE |
   | Chitin | (−54,0,8) | CHITIN_NODE | BONE_BLOCK | HONEYCOMB_BLOCK |

   At y=1 it places the node block at (−1,1,0) and the accent block at (1,1,0) and (0,1,1). These are the targets of ants' gathering trips (`AntEntity.java:433-452` searches within 6 blocks of these centres).
5. `registerStarterBuildings` (`:685-698`):
   - adds 5 complete building records;
   - adds 5 `NestChamber` records: `queen_hall`, `food_chamber`, `nursery`, `mine_chamber`, `barracks`;
   - appends the culture starter queue.

### 4.4 Site table (`ColonyBuilder.siteFor(origin, type, existing)`, `:292-344`)

`e` (`existing`) is the number of buildings of that type already present, complete or not (`:286-290`). **All sites use the origin's Y.**

Numbering: this document numbers instances **1-based** (#1 = the first building of a type, #2 = the second). The formulas use the 0-based `e`, so #n has e = n−1.

| Type | #1 (e=0) | #2 (e=1) | #3 and later (e≥2) |
|---|---|---|---|
| QUEEN_CHAMBER, GREAT_MOUND, QUEEN_VAULT | origin | – | – |
| FOOD_STORE | (38,0) | (66,21) | (66+28(e−1), 21+24(e−1)) |
| NURSERY | (−38,0) | (−66,−21) | (−66−28(e−1), −21−24(e−1)) |
| MINE | (0,38) | (21,66) | (21+28(e−1), 66+28(e−1)) |
| CHITIN_FARM | (−38,34) | (−67,58) | (−91−30(e−2), 30−26(e−2)) |
| BARRACKS | (0,−38) | (25,−68) | (25+31(e−1), −68−28(e−1)) |
| MARKET | (34,−34) | (66,−57) | (66+31(e−1), −57−28(e−1)) |
| DIPLOMACY_SHRINE | (−36,−42) | (−70,−75) | (−70−32(e−1), −75−28(e−1)) |
| WATCH_POST | (58,−104) | (−103,−68) | #3 (108,88); #4+ (−125−34(e−3), 82+31(e−3)) |
| RESIN_DEPOT | (50,50) | (82,79) | (82+33(e−1), 79+29(e−1)) |
| PHEROMONE_ARCHIVE | (−58,−18) | (−88,−43) | (−88−30(e−1), −43−26(e−1)) |
| FUNGUS_GARDEN | (−70,92) | (−108,119) | (−108−38(e−1), 119+29(e−1)) |
| VENOM_PRESS | (92,−8) | (129,22) | (129+37(e−1), 22+30(e−1)) |
| ARMORY | (0,−72) | (35,−82) | (35+32(e−1), −82−28(e−1)) |
| TRADE_HUB | (74+34e, −52−30e) | | |
| ROAD | (7e, 7e) | | |

Coarse top-down map (1 character = 8 blocks, north/−Z at the top, `|` marks x=0). Legend:
- Q queen/great mound/vault; F food store; N nursery; M mine; B barracks; C chitin farm; K market; D shrine; W watch post; R resin depot; A archive; G fungus garden; V venom press; Y armory; T trade hub;
- `2` / `3` are the second and third instances of a type; X the expansion outpost; f, o, c the food, ore and chitin nodes;
- dots ≈ the radius-72 cleared circle.

```
                      |
 -104                        W
  -96                        3            (3 = barracks #3 at (56,-96), overlaps W #1)
  -72    W         ...Y..2               (2 = barracks #2 (25,-68))
  -56           .          .   T
  -48          . D          .
  -40         .       B   K  .
  -24        2A               .           (2 = nursery #2 (-66,-21), overlaps A)
   -8        .                .  V
    0        .   N    Q   F   .
    8        . c           Xf .           (X = expansion outpost (34..42,12))
   16        .                2           (2 = food store #2 (66,21))
   24     C                               (C #3 (-91,30))
   32         .  C    M      .
   48           .      o   .R
   56        C   .        .               (C #2 (-67,58))
   64              .....2                 (2 = mine #2 (21,66))
   80 W                                   (W #4 (-125,82))
   88        G                     W      (W #3 (108,88))
```

The spacing contracts are enforced by unit tests (§11): repeat sites at least 34–46 blocks apart per family, watch posts at least 90 apart, and the trade hub at least 42 from market #1, venom press #1 and watch post #1.

### 4.5 Collisions and coverage (computed by the scratch script)

Clear-radius coverage:
- **Entirely outside** the radius-72 cleared circle, so placed on raw terrain at the origin's Y: every WATCH_POST, FUNGUS_GARDEN #1 and later, VENOM_PRESS #1 and later, TRADE_HUB, CHITIN_FARM #2 and later, MARKET #2, DIPLOMACY_SHRINE #2, RESIN_DEPOT #2, PHEROMONE_ARCHIVE #2, ARMORY #2, and every #3+ instance of the other families.
- **Partly outside**: FOOD_STORE #2, NURSERY #2, MINE #2, BARRACKS #2, RESIN_DEPOT #1, ARMORY #1.

Footprint collisions (number of shared columns across variant combinations):

| Pair | Shared columns | Reachable in play? |
|---|---|---|
| NURSERY #2 (−66,−21) × PHEROMONE_ARCHIVE #1 (−58,−18) | **79–89** | Yes, with CHITIN instinct: 3 chitin farms, then nurseries. **[BUG]** |
| BARRACKS #3 (56,−96) × WATCH_POST #1 (58,−104) | **62–73** | Yes, with DEFENSE instinct: 4 watch posts, then barracks. **[BUG]** |
| Expansion outpost watch post at (40,12) × FOOD_STORE #1 (38,0) | 4–5 (1–3 when the outpost is at (34,12) or (42,12)) | Yes (expansion event). **[BUG, minor]** |
| MARKET #2 (66,−57) × TRADE_HUB #1 (74,−52) | 113–114 | Only through renovate or QA; the builder never queues a second market. |
| NURSERY #3 × PHEROMONE_ARCHIVE #2; BARRACKS #2 × ARMORY #2 | 108–120; 1 | Practically unreachable |

The unit test that guards trade-hub spacing (`src/test/.../OrganicBuildingBlueprintTest.java:270-276`) only checks market #1, venom press #1 and watch post #1. It misses market #2.

### 4.6 Block replacement rules (`StructurePlacer`)

`safeSet(level, pos, block)` (`:68-78`) calls `setBlockAndUpdate` only if `canReplace(pos)` (`:93-172`) is true:
- Anything with a **block entity** → never replaced.
- Otherwise replaceable if it is air, or in tag `FLOWERS`, `LEAVES`, `LOGS` or `REPLACEABLE_BY_TREES`, or one of: GRASS_BLOCK, DIRT, COARSE_DIRT, ROOTED_DIRT, PODZOL, MUD, PACKED_MUD, MUD_BRICKS, MUD_BRICK_STAIRS, GRAVEL, STONE, COBBLESTONE, MOSSY_COBBLESTONE, DEEPSLATE, COBBLED_DEEPSLATE, MOSS_BLOCK, MYCELIUM, DIRT_PATH, MANGROVE_ROOTS, MUDDY_MANGROVE_ROOTS, MANGROVE_PLANKS, CUT_COPPER, TUFF, CHISELED_TUFF, RED_TERRACOTTA, BLACKSTONE, POLISHED_BLACKSTONE, POLISHED_DEEPSLATE, HONEYCOMB_BLOCK, HONEY_BLOCK, BROWN/RED_MUSHROOM_BLOCK, COBBLED_DEEPSLATE_WALL, IRON_ORE, DEEPSLATE_IRON_ORE, BONE_BLOCK, OCHRE_FROGLIGHT, AMETHYST_BLOCK, CANDLE, LANTERN, BARREL, CRAFTING_TABLE, COMPOSTER, BELL, OAK_FENCE, OAK_LOG, HAY_BLOCK, **GOLD_BLOCK**, SLIME_BLOCK, and every Formic block (NEST_MOUND … CHITIN_BED).
- Note: `REPLACEABLE_BY_TREES` in vanilla also lists water and seagrass, if memory serves (not verified in this repo). If so, the clears and paths can remove water.

Consequences:
- Non-listed natural blocks stay in place inside structures: sand, sandstone, granite, andesite, diorite, snow blocks, clay, terracotta other than red, planks other than mangrove, and so on.
- Palette blocks that are *not* replaceable — MUSHROOM_STEM (fungus garden), DEEPSLATE_TILES (watch post) — and non-listed furniture (anvil, smithing table, target, bookshelf, cauldron, shroomlight, iron bars/chain/block, piston, glass, lightning rod, cartography table, grindstone, small mushrooms, …) survive later re-placement and carving. Because placement is deterministic this rarely matters, except where two structures overlap (§4.5, §5.2).

`safeCarve(pos)` (`:80-87`) sets AIR on **anything except block entities and bedrock**. Only the vault uses it (§5.3). The vault's `placeStructuralBlock` calls `safeCarve` and then `safeSet`, so underground routes cut through granite, ores and so on.

### 4.7 Other `ColonyService` world helpers

- `placePath(start, end)` (`:739-751`) is a diagonal 1-wide path. **[DEAD]**
- `connectOriginTrail(origin, dest[, type])` (`:793-811`) plus `placeTrailRoleAccents`, `trailMidpoint`, `approachGate`, `trailRoleFloor`, `trailRoleAccent` (`:820-903`) are **[DEAD]**. They are never called, although the Javadoc claims the QA settlement scene and renovation use them. What they would do:
  - a 3-wide trail from the origin to a building;
  - at the midpoint, two role blocks each with an accent on top, two blocks beside the lane, plus a PODZOL + MANGROVE_ROOTS "bench";
  - a gate pair 9 blocks before the destination, if the trail is at least 12 long.

  Role floor/accent pairs:

  | Building | Floor | Accent |
  |---|---|---|
  | MARKET, TRADE_HUB | PACKED_MUD | OCHRE_FROGLIGHT |
  | ARCHIVE | CHISELED_TUFF | AMETHYST_BLOCK |
  | SHRINE | CHISELED_TUFF | CANDLE |
  | RESIN_DEPOT | HONEYCOMB_BLOCK | HONEY_BLOCK |
  | FUNGUS_GARDEN | MYCELIUM | RED_MUSHROOM_BLOCK |
  | ARMORY | COBBLED_DEEPSLATE | POLISHED_DEEPSLATE |
  | BARRACKS, MINE | COBBLED_DEEPSLATE | IRON_ORE |
  | VENOM_PRESS | POLISHED_BLACKSTONE | SLIME_BLOCK |
  | CHITIN_FARM | BONE_BLOCK | HONEYCOMB_BLOCK |
  | NURSERY | BONE_BLOCK | BONE_BLOCK |
  | WATCH_POST | PODZOL | COBBLED_DEEPSLATE_WALL |
  | default | PODZOL | MANGROVE_ROOTS |

### 4.8 Terrain handling: summary

- The origin is snapped to the heightmap surface. Nothing else is: buildings, paths and resource clusters all sit at the **origin's Y**.
- Terrain is flattened **only above** the origin (y+1…y+10, radius 72, replaceable blocks only). **Nothing fills gaps below.** Structures and paths over lower ground float; higher ground above y+10 remains as cliffs.
- Buildings do not orient towards the hub. The blueprint compiler cannot rotate, and all mouths face −Z. As a result:
  - the barracks (0,−38) turns its back to the queen mound;
  - the food store and nursery paths arrive at their side walls;
  - only the mine's mouth faces the hub.

### 4.9 Expansion outpost (`ColonyRecurringEvents.java:94-122, 275-283, 483-541, 916-956`)

This is a recurring event for player colonies only (event scheduling is covered by the events extraction).

- **Conditions**: queen alive; rank ≥ HIVE; completed MARKET and WATCH_POST; no outpost yet (a completed WATCH_POST within 6 blocks of the rank-HIVE outpost position or the current one); no open expansion request.
- **Position**: `origin + (edge, 0, 12)` with `edge = clamp(claim + 4, 34, 42)`, where `claim = max(rank radius, stored claim radius)`. At HIVE (36) this gives (40,0,12); at CITADEL, (42,0,12). The Y is chosen by scanning for path-like ground (`:916-938`).
- **Trigger**:
  - opens the player-supply request **30 ORE** (building WATCH_POST, reason "expansion outpost materials");
  - clears y+1..3 over a 9×9 square and marks its edge with DIRT_PATH (ROOTED_DIRT corners);
  - builds a temporary tower: WATCH_POST block → COBBLED_DEEPSLATE_WALL → OCHRE_FROGLIGHT;
  - adds props: IRON_ORE, CHITIN_NODE, BONE_BLOCK and OAK_FENCE posts;
  - paints a trail from the origin and spawns a crew.
- **Completion** (only when the player completes the contract through the tablet):
  - adds a free complete WATCH_POST record, unless one already exists within 4 blocks;
  - sets the claim radius to `min(48, max(claim, max(|dx|,|dz|) + 4))`, for example 44. **[BUG]** `ColonyBuilder.tick` resets this within 1 s (§3.1);
  - places the full organic WATCH_POST blueprint;
  - then stamps ROOTED_DIRT + OCHRE_FROGLIGHT at (±5,±5) and DIRT_PATH + HONEYCOMB at (±6,0) and (0,±6). These stamps sit **inside the new watch post's shell** (footprint x −9..8, z −5..9). **[BUG, visual]**
  - The comment at `:516-518` about a "one-block building baseline" is stale.

### 4.10 Renovation (`ColonyService.java:445-491`; `/formic colony renovate`, nearest colony within 160)

1. Copies the building list. If there is no queen chamber, inserts a complete one at the origin at index 0.
2. `clearSettlement(72, 10)`, the three resource clusters, and wide paths to them.
3. Clears the building list and the chamber list.
4. For each old building, in order: `nextPos = siteFor(origin, type, count so far)` (re-sites to the canonical layout, a migration tool); rebuilds the record with the same level, progress and damage; registers a NestChamber (except ROAD and WATCH_POST, `:700-705`); paints a wide path from the origin (not for the queen chamber); places the structure at its current stage.
   - Paths painted after earlier buildings **cut 3-wide DIRT_PATH lines through the y=0 layer** of structures already placed, such as the queen mound.
5. Event "Colony renovated into Queen Hall campus", task "Renovated colony campus", labels synced.
6. It does not re-place wild-colony landmarks and does not respawn ants.

### 4.11 "Landmass" and forest-floor dressing

- **Shared landmass: removed.** On master (`946ea44`), `StructurePlacer.placeSharedMoundLandmass` was called from `placeNest`:
  - noise-driven native-earth berms, 13 wide and 4–16 high (saddle profile), along each axis from the origin to the 4 starter satellites, from step 14 to span−5;
  - `placeSharedCampusLandmass2D` [QA-ONLY]: a radius-66 carapace with central peak 46, shoulder 28, body floor 20 and summed sub-lobes per satellite;
  - `carveSharedMoundChamberMouths` [QA-ONLY].

  Source: `git show master:src/main/java/com/formicfrontier/world/StructurePlacer.java`, around l.2634-3035. Commit `6fb5bcd` deleted all of it. `VisualQaScenes.java:640-652` still carries stale comments about the "shared landmass path".
- **Forest-floor dressing** is [QA-ONLY] and never runs in survival (`VisualQaScenes.java:1601-1868`):
  - `dressForestFloor` covers a radius-96 diamond and only touches cells that are GRASS_BLOCK. Density falls off with distance: the skip gate is `roll < dist×18/96`, so about 80% of cells are dressed near the centre and about 25% at the edge.
  - Floor blocks (roll on `h>>>3` mod 100): DIRT_PATH if dist <28 and roll <12; then COARSE_DIRT <26, PODZOL <44, ROOTED_DIRT <60, MOSS <70, DIRT <78, COBBLESTONE <86, MUD <92, PODZOL <96, else unchanged.
  - Tufts at y+1 (roll on `h>>>11`): SHORT_GRASS 14%, TALL_GRASS 8%, FERN 6%, LARGE_FERN 6%, DANDELION 6%, POPPY 5%, CORNFLOWER 5%, AZURE_BLUET 5%, WHITE_TULIP 5%, OXEYE_DAISY 4%, BROWN_MUSHROOM 6%, RED_MUSHROOM 5%, STONE_BUTTON 5%, DEAD_BUSH 4%.
  - Eight clusters on a ring at (±20,±20), (±28,8), (8,±28), cycling root mass → stone cluster → podzol + mushroom block.
  - `dressColonyCore` paints worn 3-wide routes to the starter sites and nodes (grass only) and "skirt" rings at radius 11–14 around buildings, with 70% density on the inner ring and 45% on the outer rings. It adds debris: dead bush, fern, stone button, brown mushroom, short grass.
  - The hash is `(x*73856093) ^ (z*19349663)`, then mixed.
- QA scenes also flatten their area (`prepareFlatQaArea`, `:1880-1904`): DIRT at y−2 and y−1, GRASS_BLOCK at y, and air above up to y+96 (or the world ceiling on first use), over a square of radius 58–136 depending on the scene (`qaRadius`, `:608-629`). The QA ground is pinned at Y=96 (`:61, 335-342`).

---

## 5. Structure system (compilers and data formats)

### 5.1 `StructurePlacer.placeBuilding` dispatch (`…/world/StructurePlacer.java:30-66`)

1. ROAD → one DIRT_PATH block.
2. Stage PLANNED → one DIRT_PATH pin.
3. QUEEN_CHAMBER → `TieredMoundPlacer.placeQueenStageOne`.
4. GREAT_MOUND → `placeQueenStageTwo`.
5. QUEEN_VAULT → `SubterraneanVaultPlacer.placeQueenVault`.
6. An organic family at stage COMPLETE or UPGRADED → `OrganicBuildingPlacer.place`.
7. Anything else → `safeSet(center, markerBlock(type))`.

The `culture` argument is accepted and **ignored** everywhere. The overloads without a culture default to AMBER.

**[DEAD]** helpers: `placeTradeHub` (`:190-192`), `placeCampusBuilding` (`:194-200`), `placeStagedBuilding` (`:202-208`), `placeColonyLedger` (`:210-212`). As a result the **Colony Ledger block is never placed** by the world code; the old master placed it inside the queen hall. The class Javadoc (`:17-25`) still says "only not-yet-rebuilt families remain one-block functional markers". It is stale: every family has been rebuilt.

### 5.2 Tiered mound blueprint format (`…/world/structure/TieredMoundBlueprint.java`)

JSON is loaded from the classpath `formic_blueprints/<name>.json` (`load`, `:60-162`). Top-level fields:

| Field | Type | Req. | Meaning / validation |
|---|---|---|---|
| `schemaVersion` | int | yes | must be 1 (`:34, 234-236`) |
| `name` | string | yes | non-blank |
| `seed` | int | yes | boundary noise and material rolls |
| `palette` | string | yes | one of 16 palettes (`:37-41`): `earth, food_store, nursery, mine, chitin_farm, barracks, market, pheromone_archive, armory, diplomacy_shrine, resin_depot, fungus_garden, venom_press, watch_post, great_mound, trade_hub` (all 16 are used) |
| `tiers[]` | array | yes, ≥1 | overlapping tapered elliptical frusta |
| `terraces[]` | array | yes (may be empty) | thin earth shelves |
| `chambers[]` | array | yes | carved rooms |
| `pits[]` | array | optional | stepped excavations below a chamber floor |
| `connections[]` | array | yes (may be empty) | internal stairs between chambers |
| `mouths[]` | array | yes | entrance cuts on the −Z side |

- **Tier** `{baseY, height, baseRadiusX, baseRadiusZ, topRadiusX, topRadiusZ, offsetX, offsetZ}` (`:467-488`). `topY = baseY + height − 1`. Radii are interpolated linearly by `(y − baseY)/(height − 1)`. Validation (`:246-274`):
  - baseY ≥0; height ≥2; topY < 48 (`MAX_HEIGHT`);
  - tiers ordered by baseY; the first tier must start at y=0; each later tier's baseY must be ≤ the highest topY so far (overlap);
  - radii 1..24 (`MAX_RADIUS`); the top radius must not exceed the base radius (tapering); |offset| ≤24.
- **Contains** (`:199-222`): a point is inside if it lies in any terrace, or in any tier with normalized distance `d = nx²+nz² ≤ 0.82` ("stable core"), or with `d ≤ 1 + 0.07×signedNoise(x,y,z,seed)` (boundary wobble). The noise is a 64-bit xor-multiply hash (`:411-417`).
- **Terrace** `{x, y, z, radiusX, radiusZ, thickness}` (`:491-500`): a flat ellipse covering layers `y−thickness+1..y`. Validation: 1 ≤ y ≤ topY; thickness 1..3; radii 1..24; |x|,|z| ≤24; its centre must lie in a tier's stable core at that y (`:275-290`).
- **Chamber** `{id, purpose, x, floorY, z, radiusX, radiusZ, height, openToSky?}` (`:503-524`). `topY = floorY + height`. It carves `floorY < y ≤ topY` inside an ellipse whose radius is scaled by **0.72 on the top layer and 0.9 on the layer below** (a vault ceiling), or 1.0 on every layer if `openToSky`. Validation (`:291-319`):
  - unique ids;
  - purpose from a 27-value list (`:42-48`), all used;
  - floorY ≥0; height 3..7; topY ≤ the mound top;
  - `openToSky` requires topY == the mound top;
  - radii 1..8; |x|,|z| ≤24;
  - `(x, floorY+1, z)` must lie in a tier core.
- **Pit** `{id, chamber, x, z, radiusX, radiusZ, depth}` (`:527-547`): carves y = floorY down to `floorY−depth+1`. Full depth where `d ≤ 0.36`, `max(1, depth−1)` in the ring outside that. Validation (`:320-339`): depth 1..4; radii 1..4; must fit inside the chamber with a 0.25 margin.
- **Connection** `{id, from, to, startX, startZ, direction, width}` (`:550-591`): `rise = to.floorY − from.floorY`, which must be 2..12. It is a straight stair rising one block per step in a cardinal direction (`east`/`west`/`north`/`south`). Width 1..2 lanes, perpendicular to the direction. It carves 2 blocks of headroom per step plus the landing. Validation (`:340-372`): the start must be on the `from` floor, the landing on the `to` floor, and every step inside a tier (`d ≤ 1.0`).
- **Mouth** `{x, y, frontZ, width, height, depth}` (`:593-612`): a box `x±width/2 × y..y+height−1 × frontZ..frontZ+depth−1`. The top-row outer corners are carved only where `(x+y+z)` is even, which makes the lintel irregular. Validation (`:373-384`): width odd 1..5; height 2..6; depth 2..8; y ≥1; top ≤ the mound top; **frontZ < 0**.
- **Solid** = inside, and not carved by any mouth, chamber, pit or connection (`:224-230`).
- Bounds (`:164-197`) are the tier and terrace ellipses ±1. `minY` is the deepest pit bottom.

Load errors are wrapped in `IllegalArgumentException("Invalid tiered mound blueprint …")` (`:157-161`). The blueprints are loaded in **static initializers** (`TieredMoundPlacer.java:17-18`, `OrganicBuildingPlacer.java:52-110`), so a malformed JSON crashes class initialization.

### 5.3 `TieredMoundPlacer` compile steps (`…/world/structure/TieredMoundPlacer.java`)

`place(level, center, bp)` (`:44-72`):

1. For every solid cell with y in 0..maxY, `safeSet(materialFor)`. It **never places air** for empty cells of the envelope; existing terrain stays unless something below carves it.
2. `carveMouth` (`:74-98`): sets the carved cells to air, and puts a **MUD** rear wall (`TUNNEL_BACK`, `:19`) at `z = rearZ` wherever that cell is inside the envelope. A chamber usually carves through that wall afterwards. If `mouth.y == 1`, it adds a 1-wide DIRT_PATH at y=0 from `frontZ−3` to `frontZ` and clears y1..3 above it.
3. `carveChamber` (`:140-162`): air inside the chamber ∩ envelope, and a **PACKED_MUD floor** where the floor cell is solid.
4. `carvePit` (`:164-187`): air down to each cell's depth. The bottom block is IRON_ORE when `(x·31 + z·17 + seed) mod 7 == 0`, otherwise COBBLED_DEEPSLATE. An **ORE_NODE** goes at the centre bottom.
5. `placeConnection` (`:100-133`): **MUD_BRICK_STAIRS** facing the movement direction (ascending), with PACKED_MUD support under steps above the first, and air in the headroom and landing.
6. `decorateChamber` (`:189-464`) places furniture by purpose at hard-coded offsets from the chamber centre. It **throws if an item falls outside the chamber** (`:466-475`), so chamber sizes in JSON must fit offsets that live in Java. This is a hidden coupling, and the error only appears at placement time.

`placeQueenStageOne/Two` then set NEST_CORE below and NEST_MOUND at the centre (`:32-42`). `OrganicBuildingPlacer.place` sets the family's functional block at the centre (`OrganicBuildingPlacer.java:128-131`).

**Materials** (`materialFor`, `:477-626`): cells that are not on the surface (all 6 neighbours solid, `:628-635`) get NEST_MOUND. Surface cells use `roll = floorMod(x*73428767 ^ y*912931 ^ z*43828933 ^ seed*199999, 100)`. That is 32-bit arithmetic with no mixing step, so faint banding is possible (not verified). Percentages by palette:

| Palette | Surface mix (%) |
|---|---|
| earth | NEST_MOUND 72, ROOTED_DIRT 16, COARSE_DIRT 9, MANGROVE_ROOTS 3 |
| food_store | NM 62, ROOTED 16, COARSE 10, MOSS 7, MANGROVE_ROOTS 5 |
| nursery | NM 62, ROOTED 16, PACKED_MUD 11, MUD 8, MANGROVE_ROOTS 3 |
| mine | NM 50, ROOTED 15, STONE 13, COBBLED_DEEPSLATE 11, DEEPSLATE 8, IRON_ORE 3 |
| chitin_farm | NM 60, ROOTED 15, PACKED_MUD 11, BONE_BLOCK 8, HONEYCOMB 4, MANGROVE_ROOTS 2 |
| barracks | NM 55, ROOTED 15, PACKED_MUD 12, MUD_BRICKS 10, TUFF 6, IRON_ORE 2 |
| market | NM 55, ROOTED 15, COARSE 12, MOSS 8, CUT_COPPER 6, HONEYCOMB 4 |
| pheromone_archive | NM 52, ROOTED 14, MUD_BRICKS 12, TUFF 9, CHISELED_TUFF 7, AMETHYST 4, HONEYCOMB 2 |
| armory | NM 48, ROOTED 13, PACKED_MUD 10, MUD_BRICKS 10, POLISHED_DEEPSLATE 9, BLACKSTONE 6, DEEPSLATE_IRON_ORE 4 |
| diplomacy_shrine | NM 53, ROOTED 14, MUD_BRICKS 11, CHISELED_TUFF 14, CUT_COPPER 4, HONEYCOMB 2, AMETHYST 1, GOLD_BLOCK 1 |
| resin_depot | NM 55, ROOTED 14, PACKED_MUD 12, MUD_BRICKS 8, STRIPPED_MANGROVE_WOOD 6, HONEYCOMB 3, CUT_COPPER 2 |
| fungus_garden | NM 52, ROOTED 13, MUD 10, MOSS 9, MYCELIUM 6, BROWN_MUSHROOM_BLOCK 5, MUSHROOM_STEM 3, RED_MUSHROOM_BLOCK 2 |
| venom_press | NM 48, ROOTED 13, PACKED_MUD 11, MUD_BRICKS 10, BLACKSTONE 8, POLISHED_BLACKSTONE 5, MOSS 3, SLIME_BLOCK 2 |
| watch_post | NM 50, ROOTED 14, PACKED_MUD 11, MUD_BRICKS 9, TUFF 7, COBBLED_DEEPSLATE 5, DEEPSLATE_TILES 2, DEEPSLATE_IRON_ORE 1, OCHRE_FROGLIGHT 1 |
| great_mound | NM 55, ROOTED 13, PACKED_MUD 10, MUD_BRICKS 9, TUFF 6, CHISELED_TUFF 4, CUT_COPPER 2, AMETHYST 1 |
| trade_hub | NM 48, ROOTED 13, PACKED_MUD 11, MUD_BRICKS 10, CHISELED_TUFF 7, CUT_COPPER 6, HONEYCOMB 3, GOLD 1, AMETHYST 1 |

**Furnishing sets** by chamber purpose. Offsets are (x, y above floor, z) from the chamber centre, in `TieredMoundPlacer.java` at the lines given:

| Purpose (lines) | Furniture |
|---|---|
| `queen_hall` (191-198) | CHEST (−4,1,0), BARREL (4,1,0), BARREL (−3,1,3), CRAFTING_TABLE (3,1,3), CHITIN_BED (0,1,3), LANTERN (−4,2,0) |
| `storage` (199-205) | CHEST (−3,1,0), BARREL (3,1,0), HAY_BLOCK (−3,1,2), COMPOSTER (3,1,2), LANTERN (−3,2,0) |
| `lookout` (206-211) | PHEROMONE_ARCHIVE block (0,1,2), OAK_FENCE + LANTERN (−2,1..2,1), BELL (2,1,1) |
| `food_store` (212-222) | CHEST, BARREL (±4,1,0); HAY (−3,1,3), COMPOSTER (3,1,3); FOOD_NODE (0,1,3); LANTERNs (±4,2,0) |
| `nursery` (223-234) | CHITIN_BED ×2 (±4,1,0); HONEYCOMB (−3,1,3), BONE_BLOCK (3,1,3); BARREL (−2,1,4), OCHRE_FROGLIGHT (2,1,4); LANTERNs (±4,2,0) |
| `mine` (235-242) | CHEST, BARREL (±4,1,0); COBBLED_DEEPSLATE_WALL (−3,1,3), IRON_ORE (3,1,3); LANTERNs (±2,2,3); plus the pit with ORE_NODE |
| `chitin_farm` (243-252) | CHITIN_BED ×2 (±4,1,0); BONE (−3,1,2), HONEYCOMB (3,1,2); CHITIN_NODE (−1,1,3), COMPOSTER (1,1,3); LANTERNs (±4,2,0) |
| `barracks` (253-263) | CHITIN_BED ×4 (±6,1,0), (±5,1,3); ANVIL (−3,1,4), SMITHING_TABLE (0,1,4), TARGET (3,1,4); LANTERNs (±6,3,0) |
| `market` (264-274) | CHEST, BARREL (±4,1,0); HAY (−3,1,3), COMPOSTER (3,1,3); BELL (0,1,4); OAK_FENCE + LANTERN posts (±3,1..2,−2) |
| `archive_hall` (275-283) | CHEST, BARREL (±4,1,0); CHISELED_BOOKSHELF (−3,1,3), LECTERN (3,1,3), PHEROMONE_ARCHIVE (0,1,3); LANTERNs |
| `archive_loft` (284-291) | BOOKSHELF (−2,1,0), CHISELED_BOOKSHELF (2,1,0), AMETHYST (−1,1,2), PHEROMONE_ARCHIVE (1,1,2), LANTERNs (±2,2,0) |
| `armory_forge` (292-300) | ANVIL (−4,1,0), SMITHING_TABLE (−3,1,3), BLAST_FURNACE (0,1,3), GRINDSTONE (3,1,3), ARMORY block (4,1,0), LANTERNs |
| `armory_vault` (301-308) | CHEST, BARREL (±2,1,0), IRON_BLOCK (−1,1,2), TARGET (1,1,2), IRON_BARS (0,2,1), LANTERN (0,2,−1) |
| `diplomacy_shrine` (309-320) | CHISELED_TUFF plinths + LANTERNs (±3,1..2,0); HONEYCOMB (−2,1,2), GOLD_BLOCK (2,1,2) with CANDLEs on top; DIPLOMACY_SHRINE block (0,1,2); BELL (0,1,3) |
| `resin_workshop` (321-329) | BARREL (−4,1,0), CHEST (4,1,0), CAULDRON (−3,1,3), RESIN_DEPOT block (0,1,3), CRAFTING_TABLE (3,1,3), LANTERNs |
| `resin_vault` (330-337) | BARREL ×2 (±2,1,0), HONEY_BLOCK (−1,1,2), HONEYCOMB (1,1,2), OCHRE_FROGLIGHT (0,2,1), LANTERN (0,2,−1) |
| `fungus_garden` (338-350) | MYCELIUM + BROWN_MUSHROOM (−4,1..2,0); PODZOL + RED_MUSHROOM (4,1..2,0); mushroom blocks + SHROOMLIGHT (±3,1..2,2); COMPOSTER (−2,1,4), FUNGUS_GARDEN block (0,1,4), BARREL (2,1,4) |
| `venom_press_hall` (351-368) | POLISHED_BLACKSTONE + BREWING_STAND (−4,1..2,0); BARREL (4,1,0); VERDANT_FROGLIGHT (±4,2,−1); SLIME + LIME_STAINED_GLASS (−3,1..2,3); CAULDRON (3,1,3); press: blackstone walls (±1,1..2,3), CAULDRON (0,1,3), IRON_CHAIN (0,2,3), PISTON (0,3,3); VENOM_PRESS block (0,1,4) |
| `venom_vault` (369-378) | BARREL, CHEST (±2,1,0); SLIME + LIME glass (−1,1..2,2); POLISHED_BLACKSTONE + BREWING_STAND (1,1..2,2); POLISHED_BLACKSTONE + SOUL_LANTERN (0,1..2,−1) |
| `watch_guard` (379-388) | BARREL, CHEST (±3,1,0) + LANTERNs; FLETCHING_TABLE (−3,1,2), TARGET (3,1,2); WATCH_POST block (0,1,3) + BELL (0,2,3) |
| `watch_lookout` (389-398) | CARTOGRAPHY_TABLE (−2,1,0), LECTERN (2,1,0), LANTERNs; OCHRE_FROGLIGHT (−1,1,2), DEEPSLATE_IRON_ORE (1,1,2); WATCH_POST block (0,1,2) + LIGHTNING_ROD (0,2,2) |
| `great_larder` (399-407) | CHEST, BARREL (±2,1,0), HAY (−2,1,2), COMPOSTER (2,1,2), FOOD_NODE (0,1,2), LANTERNs (±2,2,0) |
| `great_workshop` (408-416) | BARREL, CHEST (±2,1,0), CRAFTING_TABLE (−2,1,2), SMITHING_TABLE (0,1,2), GRINDSTONE (2,1,2), LANTERN (−2,2,0), OCHRE_FROGLIGHT (2,2,0) |
| `great_crown` (417-428) | CARTOGRAPHY_TABLE (−2,1,1), LECTERN (2,1,1), AMETHYST (−1,1,2), CUT_COPPER (1,1,2), PHEROMONE_ARCHIVE (0,1,2), LANTERNs (±2,2,1), LIGHTNING_ROD (0,2,2) |
| `trade_hub_court` (429-443) | CHEST (−5,1,−1), BARREL (5,1,−1), HAY (−3,1,2), HONEYCOMB (3,1,2), CARTOGRAPHY_TABLE (−2,1,4), LECTERN (2,1,4), BELL (0,1,4), OAK_FENCE + LANTERN posts (±4,1..2,−3) |
| `trade_hub_warehouse` (444-452) | CHEST (−3,1,0), BARREL (−3,1,2), HAY (−1,1,2), HONEYCOMB (1,1,2), CUT_COPPER (3,1,2), LANTERNs (−3,2,0/2) |
| `trade_hub_brokerage` (453-461) | CARTOGRAPHY_TABLE (3,1,0), LECTERN (3,1,2), PHEROMONE_ARCHIVE (−3,1,2), GOLD_BLOCK (−1,1,2), AMETHYST (1,1,2), LANTERNs (3,2,0/2) |

**Furniture has no function.** Chests and barrels are empty, and there is no loot. The functional Formic blocks inside (PHEROMONE_ARCHIVE, ARMORY, …) only open the colony UI when used (§10.2).

### 5.4 Subterranean vault format (`…/world/structure/SubterraneanVaultBlueprint.java`) and placer

| Field | Validation |
|---|---|
| `schemaVersion` = 1; `name`; `seed`; `palette` ∈ {`queen_vault`} (`:32`) | |
| `surfaceAccess {id, to, surfaceFloorY, startX, startZ, direction, width}` | surfaceFloorY must be 0; direction cardinal; width 1..2; drop `0 − to.floorY` must be 3..16; the landing must be on the target's floor (`:199-209`) |
| `chambers[] {id, purpose, x, floorY, z, radiusX, radiusZ, height}` | ≥2 chambers, unique ids; purpose ∈ {`vault_guard`,`vault_treasury`,`vault_sanctum`} (`:33`); floorY −32..−3; height 3..6; topY ≤ −1; radii 2..8; abs(x), abs(z) ≤ 24 (`:182-198`) |
| `connections[] {id, from, to, startX, startZ, direction, width}` ("Descent") | unique ids; drop `from.floorY − to.floorY` must be 2..12; start on the `from` floor and landing on the `to` floor (`:210-226`) |
| topology | every chamber must be reachable from the surface-access target, through descents or same-floor chambers whose interiors overlap (BFS, `:230-272`) |

Geometry:
- Each chamber's shell envelope is its ellipse **+1.5 blocks** (`SHELL_THICKNESS`, `:361`). The envelope extends up to topY+2 with ceiling scales 0.85 and 0.65, and **never reaches y ≥ 0** (`:383-394`).
- Stair envelopes are 3×3 cross-sections from y−1 to y+3 around every step (`:287-303`).
- Solid = envelope minus chamber carve minus 2-block stair headroom (`:153-169`).

`SubterraneanVaultPlacer.place` (`…/world/structure/SubterraneanVaultPlacer.java:29-61`):
1. Places shell blocks with `placeStructuralBlock` (carve, then set; excavates everything except block entities and bedrock).
2. Carves the chambers with `safeCarve`.
3. Builds the descents: MUD_BRICK_STAIRS facing **opposite** to the movement (descending), PACKED_MUD under every step, 2 blocks of headroom, landing cleared (`:79-101`).
4. Decorates (`:120-154`).
5. Finally restores NEST_MOUND at the centre and NEST_CORE below (`:56-60`), because the PLANNED pin replaced them.

Vault palette on surface cells (`:167-180`): NM 45, PACKED_MUD 14, MUD_BRICKS 12, TUFF 11, CHISELED_TUFF 8, POLISHED_DEEPSLATE 6, CUT_COPPER 3, AMETHYST 1.

Furnishing:

| Purpose | Furniture |
|---|---|
| `vault_guard` | CHEST (−4,1,0), BARREL (4,1,0), TARGET (−3,1,3), SMITHING_TABLE (3,1,3), IRON_BARS (±1,1,3) around a NEST_CORE (0,1,3), LANTERNs (±4,2,0) |
| `vault_treasury` | CHEST (−3,1,0), BARREL (2,1,−1), GOLD_BLOCK (−2,1,2), NEST_CORE (0,1,2), AMETHYST (2,1,2), LANTERNs |
| `vault_sanctum` | CHITIN_BED (−2,1,0) and (3,1,0), BONE (−2,1,2), HONEYCOMB (2,1,2), OCHRE_FROGLIGHT (0,1,2), LANTERNs (±2,2,2) |

### 5.5 Organic families and variant selection (`OrganicBuildingPlacer`)

- 14 families: FOOD_STORE, NURSERY, MINE, CHITIN_FARM (3 variants), BARRACKS, MARKET, PHEROMONE_ARCHIVE, ARMORY, DIPLOMACY_SHRINE, RESIN_DEPOT, FUNGUS_GARDEN, VENOM_PRESS, WATCH_POST (3 variants), TRADE_HUB (1 variant). The others have 2 variants (`:52-110`).
- **Variant index** = `floorMod(center.x×31 + center.z×17, variantCount)` on **absolute world coordinates** (`:133-140`).
  - The same site always gets the same silhouette.
  - Which variant a given layout slot gets depends on where the colony is founded. The tests only prove that all variants appear for origin (11,0,−7).
- Functional blocks (`:150-168`): FOOD_CHAMBER, NURSERY_CHAMBER, MINE_CHAMBER, CHITIN_BED, BARRACKS_CHAMBER, MARKET_CHAMBER, PHEROMONE_ARCHIVE, ARMORY, DIPLOMACY_SHRINE, RESIN_DEPOT, FUNGUS_GARDEN, VENOM_PRESS, WATCH_POST, and MARKET_CHAMBER for the trade hub.

---

## 6. Blueprint catalog (`src/main/resources/formic_blueprints/`, 32 files)

The folder has **32** JSON files, not 33. The empty directory `src/main/resources/formic_structures/` remains on disk; git does not track it.

### 6.1 Summary (measured)

Column meanings:
- **Tiers / Ch / Mo / Pit / Conn / Ter**: counts of tiers, chambers, mouths, pits, connections and terraces.
- **W×D**: extent of all solid cells, in blocks (x × z).
- **H**: height in blocks (maxY + 1).
- **Foot**: solid cells at y=0.
- **Solid**: total solid cells.

| File | Palette | Seed | Tiers | Ch | Mo | Pit | Conn | Ter | W×D | H | Foot | Solid |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| queen_mound_stage_1 | earth | 4217 | 4 | 3 | 3 | 0 | 2 | 2 | 21×17 | **24** | 250 | 2003 |
| queen_mound_stage_2 | great_mound | 8123 | 7 | 6 | 6 | 0 | 3 | 3 | 25×19 | **33** | 354 | 3354 |
| food_store_a | food_store | 7129 | 3 | 1 | 1 | 0 | 0 | 0 | 18×13 | 7 | 181 | 471 |
| food_store_b | food_store | 9341 | 3 | 1 | 1 | 0 | 0 | 0 | 17×13 | 7 | 172 | 497 |
| nursery_a | nursery | 11821 | 3 | 1 | 1 | 0 | 0 | 0 | 18×14 | 8 | 201 | 666 |
| nursery_b | nursery | 13597 | 3 | 1 | 1 | 0 | 0 | 0 | 19×15 | 8 | 217 | 735 |
| mine_a | mine | 15791 | 3 | 1 | 1 | 1 | 0 | 0 | 17×15 | 7 | 177 | 530 |
| mine_b | mine | 17417 | 3 | 1 | 1 | 1 | 0 | 0 | 17×15 | 7 | 186 | 544 |
| chitin_farm_a | chitin_farm | 19139 | 3 | 1 | 1 | 0 | 0 | 0 | 19×15 | 6 | 204 | 487 |
| chitin_farm_b | chitin_farm | 20807 | 3 | 1 | 1 | 0 | 0 | 0 | 19×15 | 6 | 211 | 517 |
| chitin_farm_c | chitin_farm | 22483 | 3 | 1 | 1 | 0 | 0 | 0 | 19×14 | 6 | 200 | 482 |
| barracks_a | barracks | 24151 | 4 | 1 | 1 (w5) | 0 | 0 | 0 | 24×15 | 8 | 263 | 791 |
| barracks_b | barracks | 25919 | 4 | 1 | 1 (w5) | 0 | 0 | 0 | 24×15 | 8 | 273 | 827 |
| market_a | market | 27143 | 4 | 1 (open) | 1 (w5) | 0 | 0 | 0 | 19×15 | 4 | 203 | 409 |
| market_b | market | 29873 | 4 | 1 (open) | 1 (w5) | 0 | 0 | 0 | 19×15 | 4 | 204 | 405 |
| pheromone_archive_a | pheromone_archive | 31727 | 4 | 2 | 2 | 0 | 1 | 1 | 18×15 | 11 | 195 | 805 |
| pheromone_archive_b | pheromone_archive | 34649 | 4 | 2 | 2 | 0 | 1 | 1 | 18×14 | 11 | 199 | 811 |
| armory_a | armory | 34123 | 6 | 2 | 1 | 0 | 0 | 1 | 20×17 | 9 | 263 | 1030 |
| armory_b | armory | 35831 | 6 | 2 | 1 | 0 | 0 | 1 | 20×18 | 9 | 267 | 1055 |
| diplomacy_shrine_a | diplomacy_shrine | **37139** | 5 | 1 (open) | 1 | 0 | 0 | 0 | 19×16 | 8 | 230 | 769 |
| diplomacy_shrine_b | diplomacy_shrine | 38903 | 5 | 1 (open) | 1 | 0 | 0 | 0 | 19×17 | 8 | 236 | 802 |
| resin_depot_a | resin_depot | **37139** | 5 | 2 | 1 | 0 | 0 | 1 | 20×16 | 8 | 244 | 770 |
| resin_depot_b | resin_depot | 38971 | 5 | 2 | 1 | 0 | 0 | 1 | 20×16 | 8 | 241 | 775 |
| fungus_garden_a | fungus_garden | 40739 | 6 | 1 | 1 (w5) | 0 | 0 | 0 | 22×17 | 7 | 259 | 744 |
| fungus_garden_b | fungus_garden | 42901 | 6 | 1 | 1 (w5) | 0 | 0 | 0 | 22×17 | 7 | 256 | 738 |
| venom_press_a | venom_press | 45139 | 6 | 2 | 1 | 0 | 0 | 1 | 21×17 | 9 | 253 | 951 |
| venom_press_b | venom_press | 47351 | 6 | 2 | 1 | 0 | 0 | 1 | 21×17 | 9 | 256 | 959 |
| watch_post_a | watch_post | 49853 | 6 | 2 | 2 | 0 | 1 | 1 | 18×13 | **14** | 175 | 944 |
| watch_post_b | watch_post | 51971 | 6 | 2 | 2 | 0 | 1 | 1 | 18×13 | 14 | 179 | 962 |
| watch_post_c | watch_post | 54139 | 7 | 2 | 2 | 0 | 1 | 1 | 17×15 | 14 | 185 | 950 |
| trade_hub | trade_hub | 51827 | 4 | 3 (1 open) | 1 (w5) | 0 | 0 | 0 | **29×21** | 8 | **436** | 1543 |
| queen_vault | queen_vault | 9107 | (vault format) | 3 | – | – | 1 + access | – | 19×13, y −12..0 | 13 deep | – | 1115 shell, 585 carved |

`resin_depot_a` reuses `diplomacy_shrine_a`'s seed 37139, probably by copy-paste. It is harmless because the palettes differ.

General pattern:
- Almost every family pairs a main tier centred near (0, ~1) with 2–5 overlapping lobes.
- Chambers sit on floor 0, and one −Z mouth leads into each.
- The **"b" variant mirrors the lobes, the room offsets and the stair direction in X**. It usually moves the mouth from x=0 or −1 to x=+1 and nudges radii by 0.1–0.5.

### 6.2 Per-building details

#### Central mound — `queen_mound_stage_1.json` (QUEEN_CHAMBER), a 24-tall landmark

- **Tiers**:
  - T0 base y0 h8, radius 10×8 → 7.4×6, offset (0,1);
  - T1 y6 h8, 8×6.3 → 5.8×4.7, offset (−1.2,1.2);
  - T2 y12 h7, 6.2×5 → 4.2×3.4, offset (0.8,1);
  - T3 y17 h7, 4.5×3.7 → 1.4×1.3, offset (0,1). **Peak at y=23.**
- **Terraces**: (−2,7,−4) r4×3; (2,13,−3) r3.5×2.4. These are the front shelves of the upper entrances.
- **Chambers**:
  - `queen_hall` (0,0,2) r5.5×4.5 h5;
  - `middle_store`, purpose `storage`, (−2,7,1) r4.3×3.5 h4;
  - `upper_nook`, purpose `lookout`, (2,13,1) r3.2×2.8 h3.
- **Stairs**:
  - hall_to_store: start (4,3) going west, rise 7;
  - store_to_lookout: start (−4,0) going east, rise 6.
- **Mouths**: (0,1,−7) w5 h4 d6; (−2,8,−5) w5 h4 d4; (2,14,−4) w3 h3 d4. The tests assert mouths on floors y 1, 8 and 14.
- **Size**: 21×17×24, 250 footprint, 2003 solid (tests: height 24, width ≤21, depth ≤18, height > width).
- **Anchors**: NEST_CORE at (0,−1,0) and NEST_MOUND at (0,0,0).

#### `queen_mound_stage_2.json` (GREAT_MOUND): in-place evolution of stage 1

The chambers stage 1 already has are kept at identical coordinates, so furniture lines up when stage 2 is placed over stage 1. Differences from stage 1:

| Aspect | Stage 1 | Stage 2 |
|---|---|---|
| Palette / seed | earth / 4217 | great_mound / 8123 |
| Base tier | 10×8 → 7.4×6, h8 | **11×9 → 8.2×6.8, h10** |
| Ground annexes | none | **left** lobe (−7,4.3) 6×5.4 → 3.2×2.8 h9; **right** lobe (6.5,5) 5.4×4.8 → 2.6×2.4 h7 |
| Mid tier | y6 8×6.3, h8 | y7 8.8×6.8 → 6.4×5, h10 |
| Upper tier | y12 6.2×5, h7 | y14 6.8×5.4 → 4.8×3.8, h9 |
| Crown tiers | y17 4.5×3.7 → 1.4×1.3 (peak 23) | y21 5.2×4.2 → 3.4×2.7 h8; **+ y27 3.8×3.1 → 1.3×1.2 h6 (peak 32)** |
| Terraces | 2 | 3 (adds (−1,20,−2) r3.4×2.4) |
| Chambers | queen_hall, storage, lookout | + `left_larder` (great_larder) (−7,0,4) r3.5×3.2 h4; `right_workshop` (great_workshop) (6,0,5) r3.2×2.9 h4; `crown_chamber` (great_crown) (−1,20,1) r3×2.5 h4. **The annexes overlap the queen hall at head height.** |
| Stairs | 2 | 3 (adds lookout_to_crown: start (4,1) going west, rise 7) |
| Mouths | 3 | 6: main mouth deeper and taller, (0,1,−8) h5 d7; + larder (−7,1,−1) w3 h3 d5; workshop (6,1,−1) w3 h3 d6; crown (−1,21,−3) w3 h4 d4 |
| Size | 21×17×24, 2003 solid, footprint 250 | 25×19×**33**, 3354 solid (>1.65×), footprint 354 (>1.35×) |

- **[LIKELY-BUG]** If a QUEEN_CHAMBER upgrade starts or finishes *after* the Great Mound exists, `placeQueenHall` stamps stage-1 geometry over stage 2 again. Stage-1 solid cells then fill the larder, the workshop and the side mouths. For example `(−7,2,4)` lies in stage 1's stable core. Upgrades are suppressed only while an endgame project is *due*; once all three endgame projects are complete, the queen upgrade can run.
- The queen chamber usually reaches level 2 early (it costs only 14 resin), which makes this rarer.

#### `queen_vault.json` (QUEEN_VAULT): underground, below the Great Mound

- **Surface access** `queen_hall_descent`: from (−4,0,3), which is on the queen-hall floor, going east with width 1, down 6 steps to `guard_vestibule`.
- **Chambers**:
  - `guard_vestibule` (vault_guard) (0,−6,2) r5.5×4.5 h4;
  - `royal_treasury` (vault_treasury) (−3,−12,3) r4.5×3.8 h4;
  - `brood_sanctum` (vault_sanctum) (4,−12,3) r4.2×3.6 h4. It overlaps the treasury, so the lower level is one connected space.
- **Descent** `guard_to_lower_vault`: from (4,3) going west, 6 steps down to the treasury. The landing is at (−2,3).
- **Extent**: x −9..9, y −12..0 (16 envelope cells at y=0 around the stair head), z −4..8.
- The tests assert that minY is −12, maxY is 0, the drops are 6, the landings are (2,3) and (−2,3), the chambers are carved, both stairs have headroom, nothing is placed at (0,1,0), and the shell is connected.
- **[QA-ONLY] setup**: `prepareVaultQaSubsurface` fills stone first (`VisualQaScenes.java:1906-1915`).

#### Food store (`food_store_a/b`): low, single storey, three lobes

- **a**: main (0,1) 8×6.5 → 3.5×2.8 h7; east lobe (5,2) 4.8×4 h5; west lobe (−4,3) 4.4×3.7 h5. `granary` (0,0,1) r5.6×3.7 **h4**. Mouth x=0 (w3 h4 d6, frontZ −6).
- **b**: main 7.5×6.7 → 3.2×3; **mirrored** lobes, west (−5,2) h5 and a smaller east (4,0) h4. Granary **h3** (lower ceiling). Mouth x=1.
- Tests: maxY ≤6, one chamber, no stairs, footprint ≥130, distinct footprints, rear shell intact.
- Commit `3740457` shrank granary radiusZ from 4.2 to 3.7.

#### Nursery (`nursery_a/b`): fuller brood domes, 8 tall

- **a**: main (0,1.5) 8.3×7.2 → 3.5×3 h8; west lobe (−5,2.2) h6; east lobe (4.5,3) h5. `brood_room` (0,0,1) r5.8×4.8 h3 (a thick roof above it). Mouth x=−1, d7.
- **b**: main 8×7.5; **twin symmetric lobes** (±4.8, 3/3.5) h6. Mouth x=+1, frontZ −7, d8.
- Palette: warm mud. Tests: maxY ≤7, footprint ≥150.

#### Mine (`mine_a/b`): low mound with a stepped ore pit

- **a**: main (0,1.2) 8.2×7.3 h7; west (−4.5,3) h5; east (4.5,2) h4. **b** mirrors: east (5,3) h5, west (−4,1) h4; mouth x=1, frontZ −7.
- `mine_room` (0,0,1) r4.8×4.6 h3.
- **Pit** `ore_pit` (0,3) r2.4×2 depth 2. Floor openings reach y −1 at the centre, with ORE_NODE at y −2.
- Tests: maxY ≤6, minY −2, footprint ≥130.

#### Chitin farm (`chitin_farm_a/b/c`): lowest dome, 6 tall, no pit

- **a**: lobes west (−5,3) h4 and east (5,2) h4; mouth x=−1.
- **b**: east (5.2,3) h5, west (−4.5,1) **h3**; mouth x=0.
- **c**: west (−5.2,2) h4, east (4.5,3.5) h5; chamber radiusZ **4.0** instead of 3.5; mouth x=1.
- `cultivation_room` r5.8×3.5 (c: ×4.0) h3.
- Tests: maxY ≤5, a continuous floor at (0,0,4), footprint ≥150, 3 distinct footprints.

#### Barracks (`barracks_a/b`): wide capsule, 24 long

- Main tier radius 11.2×7 (b: 11×7.2) h8, plus three lobes (west, east, rear); b mirrors them.
- `barracks_hall` (0,0,1) r7.5×4.8 h4.
- Mouth **w5**, a troop entrance.
- Tests: width ≥22, width ≥ depth+5, footprint ≥200.

#### Market (`market_a/b`): low earth banks round an open yard

- Four tiers of height 3–4. The main tier tapers only slightly (8.5×7.2 → 6.6×5.5), which gives steep banks.
- `market_yard` (0,0,**0**) r5.4×4.4 h3 **openToSky**. The mouth is w5 h3.
- Height is only 4 (maxY 3) **by design**.
- Tests: roofless at the yard centre for every y, floor solid, top ring ≥24 cells, footprint ≥180.

#### Pheromone archive (`pheromone_archive_a/b`): two storeys

- Base tier (0,1) 8.5×7 → 6×4.8 h7 plus one side lobe. Upper tiers from y5: (1,1.1) 5.8×4.9 h6 and (−2.5,2) h4. Terrace (1,5,−3) r3.2×2.2.
- `reading_hall` (archive_hall) (0,0,1) r5.6×3.7 h4 (b: 5.5). `catalog_loft` (archive_loft) (1,5,1) r3.2×2.5 h4.
- Stair from (−3,2) going east, rise 5. **b** mirrors everything, with the stair going west from (3,2).
- Mouths: (0,1,−6) w3 h4 d7 and (1,6,−4) w3 h3 d5.
- Height 11. Tests: maxY 10; upper-floor mass ≥45; stair directions {east, west}.

#### Armory (`armory_a/b`): heavy dome with a stepped brow

- Six tiers: main 9.2×8.3 h9, four lobes, and an upper tier from y4.
- Terrace (0,4,−3) r5.0×1.8, the "brow".
- `forge_hall` (armory_forge) (−1,0,1) r5.7×4.3 h4 and `weapon_vault` (armory_vault) (4,0,3) r3.6×3 h3. They overlap, forming a shared passage. **b** mirrors them to (1,…) and (−4,…).
- Narrow mouth w3 at x=∓1, frontZ −7, d8.
- Tests: maxY 8, footprint ≥220, 19≤W, 17≤D, |W−D| ≤5, enclosed except at mouths.

#### Diplomacy shrine (`diplomacy_shrine_a/b`): open ring with three horns

- Main tier 8.3×7.6 h8, plus horns west h8, east h7, rear h8 and a lower front lobe h5. **b** mirrors the lobes.
- `open_sanctum` (0,0,1) r4.6×3.8 **h7 openToSky**. Mouth w3, frontZ −7, d8.
- Tests: maxY 7; solid cells with y≥6 at x≤−5, at x≥5 and at z≥6; crown ≥12; footprint ≥190.

#### Resin depot (`resin_depot_a/b`): low cistern with twin pods

- Main 8.4×7.8 h8, three lobes of h6, a cap from y3 (5×4 → 2.3×1.8 h5), terrace (0,3,−3) r4.4×1.7.
- `workshop` (resin_workshop) (−1,0,1) r5.3×4.5 h4 and `sealed_vault` (resin_vault) (4,0,3) r3.4×3 h3. **b** mirrors them.
- Mouth w3, frontZ −7.
- Tests: maxY 7, lobes reaching x ≤−9 and x ≥9, cap mass ≥18, footprint ≥200.

#### Fungus garden (`fungus_garden_a/b`): clover plan

- Main 8.2×7.4 h7, lobes west, east, rear and front-west, plus a shallow cap from y3 (6.2×5.4 → 2.8×2.3 h4).
- `cultivation_hall` (−1,0,2) r5.7×4.5 h4, enclosed.
- Broad mouth **w5** at x=∓1, frontZ −7, d8.
- Tests: maxY 6, lobes reaching x ≤−10, x ≥10 and z ≥10, crown ≥24, footprint ≥220.

#### Venom press (`venom_press_a/b`): dark capsule with jaw lobes

- Main 8.7×7.6 h9, jaw lobes, a rear pod, and a crown from y4 (5.5×4.4 h5). Terrace (0,3,−3) r4.4×1.6.
- `press_hall` (venom_press_hall) (−1,0,1) r5.4×4.3 h4 and `reagent_vault` (venom_vault) (4,0,3) r3.3×3 h3. **b** mirrors them.
- Mouth w3, frontZ −7.
- Tests: maxY 8, crown ≥34, footprint ≥220.

#### Watch post (`watch_post_a/b/c`): tall sentinel, two storeys, 14 tall

- Base tier 7.4×6.6 → 5.1×4.4 h8, buttress lobes, a shoulder from y5 (h9), an upper tier from y7, and a spire from y9 (2.4×2.2 → 1.2×1.0). Terrace at y6.
- `guard_room` (watch_guard) (0,0,1) r4.7×3.5 h4 and `lookout_loft` (watch_lookout) (±1 or 0, 6, 1 or 2) r3.2×2.5 h4.
- Stair rise 6:
  - a: from (−3,2) going east;
  - b: from (3,2) going west;
  - **c: from (−1,−2) going south, straight up from the front**. c also has an extra rear buttress tier (7 tiers) and its loft at z=2.
- Mouths: (0,1,−6) w3 h4 d7; loft mouth (±1 or 0, 7, −4) w3 h3 d5.
- Tests: maxY 13; W ≤22 and D ≤20; upper mass ≥45; crown ≥20; stair directions {east, west, south}.

#### Trade hub (`trade_hub.json`, 1 variant): largest footprint, 29×21

- Four tiers: main (0,2) 13×10 → 9.2×7.2 h8; west (−8,3.5) 7×5.5 h8; east (8,2.5) 6.5×5 h7; rear (0.8,7) 7.2×5.4 h8.
- Chambers:
  - `exchange_court` (trade_hub_court) (0,0,2) r6.4×5.2 h7 **openToSky**;
  - `bonded_warehouse` (−7,0,3) r4.2×3.6 h4;
  - `brokerage_chamber` (7,0,3) r4.2×3.6 h4.
  - Both side rooms overlap the court.
- Mouth (0,1,−8) w5 h4 d8.
- Tests: maxY 7, footprint ≥390, W≥27, D≥21, open court for y 1..7.

---

## 7. Wild and rival colonies

### 7.1 Generation paths

- There are **no worldgen structures or features** (the data folder only has `recipe/` and `tags/`). Colonies exist only after one of these:
  - a Queen Egg or `/formic colony create` (allied);
  - `/formic colony seed-rivals` (rivals);
  - automatic discovery (wild);
  - QA scenes.
- Allied colonies are always AMBER. Rival cultures rotate with the id: `(id−1) mod 3` → LEAFCUTTER, FIRE, CARPENTER. Wild colonies pick a culture pseudo-randomly from all 4 (§7.3).
- **Every colony builds and expands autonomously** (§3), wild and rival ones included.

### 7.2 Discovery algorithm (`…/world/ColonyDiscoveryService.java`)

Constants (`:15-22`):

| Constant | Value |
|---|---|
| Check interval | `20×45 = 900` ticks (45 s) |
| Site distance | 56 + 0..40, i.e. **56–96 blocks** from the player |
| Player clear radius | 144 (no colony within 144 of the player) |
| Site clear radius | 96 (no colony within 96 of the site) |
| Maximum colonies | **6, counting every colony** (the player's own and rivals included) |
| Attempts per check | 12 |
| Region size | 128 |

Per check (`tick`, `:27-39`):
- Skipped if the system property `formic.visualQa` is set, or if `gameTime % 900 != 0` (§7.4).
- For each overworld player who is not in creative or spectator mode and while fewer than 6 colonies exist, it calls `spawnEncounter(player pos, playerRegionSeed(player))`.

Details:
- **Seed** (`:350-358`): `mix(uuidLSB ^ uuidMSB ^ regionX×341873128712 ^ regionZ×132897987541)` with a 128×128 region, so results are deterministic per player and region. `mix` is the murmur3 fmix64 finalizer (`:364-371`).
- **Site search** (`:84-100`):
  - for attempts 0..11, `mixed = mix(seed + attempt×0x9E3779B97F4A7C15)`; distance = `56 + mixed mod 41`; angle = `(mixed>>>11) mod 360` whole degrees;
  - the candidate is anchored to the surface;
  - it is accepted if viable and no colony lies within 96.
- **Viability** (`:106-134`): inside the world border; y > minY+2; the ground block is not air, water or lava; the two blocks above are air. The same holds at 4 samples ±8 blocks away in X and Z, each within ±2 blocks of the candidate's height.
- **Spawn** (`:41-60`):
  - `createWildColony(site, culture)` runs the full founding, including the radius-72 clear and the complete starter campus;
  - task "Watching nearby trails";
  - surface landmarks (§7.3);
  - events: "Discovered wild colony near …", "Surface landmarks mark foraging grounds", "Collapsed scout nest marks an old surface claim", "Surface trail points back toward nearby foragers";
  - an approach trail toward the player.
- **Approach trail** (`:136-184`):
  - an L-shaped walk (X first, then Z) from the player to the colony, at most 160 steps, skipping cells within 8 of the player and within 13 of the colony;
  - each surface-anchored tile becomes DIRT_PATH on even steps and COARSE_DIRT on odd steps, on "trail surfaces" only (not air, water or lava, with air above);
  - every 11th step at least 15 blocks from the player gets a culture trail marker on top;
  - the first painted tile becomes a **trail head**: DIRT_PATH behind and ahead, COARSE_DIRT in the middle, ROOTED_DIRT to the left, the culture's dressing block to the right, air above all five.

### 7.3 Surface landmarks (`placeSurfaceLandmarks`, `:62-82`)

These are placed only for **wild** colonies. Every block is anchored to the local surface.

Trails (`placeLandmarkTrail`, `:190-209`): L-walks of at most 72 steps. Tiles are COARSE_DIRT on every 3rd step and DIRT_PATH otherwise. Every 8th step gets a trail marker on top. Routes:
- (0,−15) → (−12,−18) → (−12,−48) → north forage (0,−58);
- (−12,−12) → west forage (−48,−34);
- (12,10) → ruined scout nest (38,22).

Foraging patches at the north (0,−58) and west (−48,−34) forage points (`:211-229`):
- a radius-4 diamond;
- FOOD_NODE at the centre;
- culture forage ground at distance ≤2, and the outer ground block beyond that;
- forage markers on top at distance 2 where `floorMod(3x+5z, 3) == 0`.

Boundary markers at (54,−16) and (−58,4) (`:231-239`): COARSE_DIRT with a trail marker on top and a boundary-top block above that.

Ruined scout nest at (38,22) (`:241-267`), a radius-5 diamond:

| Distance from centre | Block |
|---|---|
| 0 | NEST_MOUND, with NEST_CORE below |
| 1 | COARSE_DIRT |
| 2 | trail marker where `(x−z)` is even |
| ≤3 | ROOTED_DIRT |
| outer ring | PODZOL / COARSE_DIRT checker |

Two air blocks go above every cell. Props:
- CHITIN_NODE at (−2,1,0);
- BONE_BLOCK at (2,1,0);
- MANGROVE_ROOTS at (0,1,−2);
- a boundary-top block at (0,1,2);
- a mini-path at z=−4: DIRT_PATH, flanked by the trail marker and the dressing block.

Per-culture blocks (`:283-342`) — **the only place where culture changes world blocks**:

| Culture | Forage ground (≤2 / outer) | Forage marker | Trail marker | Trail-head dressing | Boundary top |
|---|---|---|---|---|---|
| LEAFCUTTER | MOSS_BLOCK / PODZOL | BROWN_MUSHROOM_BLOCK | MOSS_BLOCK | PODZOL | BROWN_MUSHROOM_BLOCK |
| FIRE | RED_TERRACOTTA / COARSE_DIRT | BLACKSTONE | RED_TERRACOTTA | COARSE_DIRT | BLACKSTONE |
| CARPENTER | ROOTED_DIRT / PODZOL | MANGROVE_ROOTS | MANGROVE_ROOTS | ROOTED_DIRT | MANGROVE_PLANKS |
| AMBER | HONEYCOMB_BLOCK / ROOTED_DIRT | HONEYCOMB_BLOCK | CHISELED_TUFF | PACKED_MUD | AMETHYST_BLOCK |

Culture of a discovered colony = `cultures[mix(seed ^ site.asLong()) mod 4]` (`:344-348`).

Landmark conflicts:
- The first landmark trail runs along x=−12 through z −18..−48. That crosses the **west edge of BARRACKS #1** (barracks_a spans x −12..11 and z −44..−30), so the trail can repaint its shell blocks.
- The west forage patch overlaps DIPLOMACY_SHRINE #1 at (−36,−42) once an AMBER wild colony builds it (its first starter-queue item). [BUG, minor]

### 7.4 Discovery bugs

- **[LIKELY-BUG] Phase lock.** `tickWorld` runs only on every 20th server tick, counted by an instance field of the mod initializer (`FormicFrontier.java:20, 35-38`). Discovery additionally needs `level.getGameTime() % 900 == 0` (`ColonyDiscoveryService.java:28`), and 900 = 45×20. It can therefore fire only if `gameTime ≡ 0 (mod 20)` on the ticks where the ticker fires. That phase is fixed for the whole session and depends on the saved game time, so in roughly 19 of 20 sessions discovery never happens. The gametest calls `spawnEncounter` directly and would not catch this.
- The maximum of 6 counts *all* colonies. `/formic colony seed-rivals 6` therefore disables discovery permanently.
- The radius-72 clear plus the 56-block minimum distance means a discovered colony can erase blocks within 16 blocks of a player (§4.3).

---

## 8. Saved state (`…/world/ColonySavedState.java`)

- **Storage**: `SavedDataType("formic_frontier_colonies", …)` in the **overworld** data storage (`:28-33, 48-54`). `computeIfAbsent`; there is no DataFixer (the 4th argument is null).
- **Codec**: `List<ColonyData>` (`:27`). `nextId` is not saved; it is rebuilt as `max(id)+1` on load (`:41-46`). `clearColonies` resets it to 1 (QA use, `:91-95`). Colonies are never removed any other way.
- **Persisted fields** (`…/sim/ColonyData.java:15-27`, `…/sim/ColonyProgress.java:13-30`):

  ```text
  ColonyData
    id:int
    origin:BlockPos                      (no dimension!)
    resources:{ResourceType→int}
    castes:{AntCaste→int}
    chambers:[NestChamber{type:string, pos, level}]
    priorities:[TaskPriority]
    queenHealth:int
    ageTicks:int
    currentTask?:string                  (default "Idle" on load)
    stage?:ColonyStage                   (default FOUNDING)
    progress?:ColonyProgress
      culture?          (AMBER)
      faction?          ("wild")
      name?             ("Unnamed Colony")
      color?            (0x8a5b32)
      playerAllied?     (false)
      reputation?       (0, clamped −100..100)
      claimRadius?      (18, clamped 18..48)
      buildings?        [ColonyBuilding{type, pos, level?=1, constructionProgress?=100, disabledTicks?=0}]
      buildQueue?       [BuildingType]
      knownColonies?    {colonyId as string → DiplomacyState}
      raidPlans?        [{attackerId, defenderId, ticksRemaining?=200, targetResource?=FOOD, amount?=0}]
      raidCooldown?     (0)
      events?           [{ageTicks?=0, message}]   (newest first, capped at 10: ColonyProgress.java:196-201)
      requests?         [{building, resource, needed(≥1), fulfilled?=0, reason?="colony logistics"}]
      completedResearch? [string]
      activeResearch?   {nodeId, progressTicks?=0}
  ```

  If `progress` is missing, `fromCodec` substitutes `ColonyProgress.allied(id)` (`ColonyData.java:68`). An old save would therefore turn into an allied AMBER colony.
- **Not persisted in this data**:
  - structure geometry, re-derived from type + position (and the variant hash);
  - ant entities (their colony id is in their own entity NBT, see the castes extraction);
  - label entities (vanilla saves them as ordinary `TextDisplay` entities with tags);
  - the world blocks themselves.
  - There is also **no link from the world back to the sim**. Breaking a building's blocks changes nothing: the record stays "complete", and nothing re-places the blocks until the next placement event.
- **[BUG] No dimension.** `QueenEggItem` passes the current level, so a Nether colony's starter campus is built in the Nether. All later construction happens in the **overworld**, because `tickWorld(OVERWORLD)` is the only world tick (`FormicFrontier.java:43-46`), and it uses the same coordinates. `nearestColony` also ignores dimension.
- **`NestChamber` list [STUB / stale]**:
  - It is written only at founding (5 entries, `ColonyService.java:692-696`) and at renovation (`:700-705`). Newly built buildings never add one.
  - Nothing reads it except the codec, the status text and a unit test (`NestChamber.core` is test-only, `…/sim/NestChamber.java:14-16`).
  - It is dead weight in the save.
- **Tick orchestration**, once per pass (`FormicFrontier.java:34-47`):
  - `tickEconomy()` (`:97-140`): for each colony, ColonyEconomy → ColonyLogistics → CasteJobLoop → ColonyStageProgression → CasteBalancer → NativeBlockRole. Then the autonomous diplomacy pass (every colony to its nearest known non-ally, `DiplomacyService.tick`). Then the caravan pass (every colony to its nearest non-hostile colony, `TradeCaravan.exchange`).
  - `tickWorld(overworld)` (`:180-191`): ColonyDiscoveryService.tick; then for each colony ColonyBuilder.tick and ColonyRecurringEvents.tick; then RaidPlanner.tick.
  - `setDirty` is set whenever either pass reports a change.
  - "Nearest known" means nearest among *all* colonies. `knownColonies` is not consulted for discovery.
- **[LIKELY-BUG, performance]** Every colony is simulated each second, whether or not a player is near. `ColonyBuilder` and label code read and write blocks and entities at the colony, so ServerLevel block access would load, or even generate, chunks around far-away colonies. Based on vanilla `getBlockState` semantics; not profiled.

---

## 9. Labels (`…/world/ColonyLabelService.java`)

- **What they are**: one vanilla **`TextDisplay` entity per building record**. Its visible text is the entity's *custom name* (`setCustomName` + `setCustomNameVisible(true)`); the display's own text field is never set. They have no gravity and two tags, `formic_frontier_label` and `formic_frontier_label_<colonyId>` (`:20, 39-59`).
- **Position**: `(x+0.5, y + h, z+0.5)` above the building centre, where h is:

  | Building | h |
  |---|---|
  | GREAT_MOUND | 9.4 |
  | QUEEN_VAULT | 4.8 |
  | TRADE_HUB | 4.6 |
  | QUEEN_CHAMBER | 6.2 |
  | everything else | 5.2 |

  The queen chamber, Great Mound and Queen Vault labels therefore stack in the same column.
- **Text** (`:61-99`): `formic_frontier.label.line` = `"%s\n%s"` (EN = RU).
  - Queen chamber: colony name (a literal) + `label.identity_status` `"%s | %s"` with the personality label key and the relationship key. Examples: "Steady | New allies" / "Ровный | Новые союзники". Other personalities: Curious/Любопытный, Industrious/Трудолюбивый, Guarded/Настороженный. Relationships: friendly, trusted, strained, rival, wild. The personality is **cosmetic only**, derived from id + culture + allied (`…/sim/ColonyPersonality.java:39-48`).
  - Everything else: the building label key (§1.1) + the stage status (§2.2).
- **Sync** (`syncLabels`, `:25-37`) discards tagged labels inside an AABB of **±80 blocks** horizontally and −12..+24 vertically around the origin, then spawns fresh labels for every building. It is called at founding, construction start and completion, repair start and finish, upgrade start, raid damage, renovation, recurring events and QA scenes (29 call sites).
- **[BUG] Duplicate labels.** Buildings sited more than 80 blocks along an axis never have their old labels removed, so each sync adds another copy. This affects every watch post, fungus garden #1, venom press #1, chitin farm #3+, roads beyond (84,84) and so on. Labels in unloaded chunks are not found either, which also causes duplicates.
- **[BUG] Stale text.** Progress changes are not synced (`ColonyBuilder.java:70-80` only syncs on completion). A building under construction keeps showing "Planned" until it completes, so the "Building %s%%" / "Needs %s" texts rarely appear.
- **Geometry**: for most mounds the anchor at y+5.2 lies inside the solid crown (for example the food store's roof). Whether the text shows through walls depends on vanilla name-tag rendering for Display entities (not verified).

---

## 10. Blocks, assets, recipes

### 10.1 `…/registry/ModBlocks.java`

All blocks except CHITIN_BED are `ColonyInteractBlock`s with block items, listed in the vanilla **Building Blocks** creative tab (`:57-77`).

| Block id | Strength | Sound | Item? | Structural role | EN / RU name |
|---|---|---|---|---|---|
| nest_mound | 1.4 | GRAVEL | yes | base material of every mound (48–72% of surfaces, 100% of interiors); centre of the queen mound | Nest Mound / Гнездовой курган |
| nest_core | 2.2 | ROOTED_DIRT | yes | under the mound centre; vault furniture | Nest Core / Ядро гнезда |
| colony_ledger | 1.8 | WOOD | yes | **never placed by the world code**; no recipe; uses the pheromone_archive texture (`models/block/colony_ledger.json`) | Colony Ledger / Журнал колонии |
| food_chamber | 1.6 | FUNGUS | yes | food store centre | Food Chamber / Пищевая камера |
| nursery_chamber | 1.6 | HONEY_BLOCK | yes | nursery centre | Nursery Chamber / Ясли |
| mine_chamber | 2.8 | DEEPSLATE | yes | mine centre | Mine Chamber / Шахта |
| barracks_chamber | 2.4 | NETHERITE_BLOCK | yes | barracks centre | Barracks Chamber / Казармы |
| market_chamber | 1.8 | HONEY_BLOCK | yes | market and trade hub centre | Market Chamber / Рынок |
| diplomacy_shrine | 2.5 | AMETHYST | yes (recipe) | shrine centre and altar | Diplomacy Shrine / Святилище дипломатии |
| watch_post | 2.2 | BONE_BLOCK | yes (recipe*) | watch post centre and props | Watch Post / Сторожевой пост |
| resin_depot | 2.0 | HONEY_BLOCK | yes (recipe) | resin depot centre | Resin Depot / Склад смолы |
| pheromone_archive | 2.3 | AMETHYST | yes (recipe) | archive centre; lookout and crown props | Pheromone Archive / Феромонный архив |
| fungus_garden | 1.2 | FUNGUS | yes (recipe) | fungus garden centre | Fungus Garden / Грибной сад |
| venom_press | 2.6 | SLIME_BLOCK | yes (recipe*) | venom press centre | Venom Press / Ядовитый пресс |
| armory | 2.8 | NETHERITE_BLOCK | yes (recipe) | armory centre | Armory / Оружейная |
| food_node | 0.8 | MOSS | yes | resource cluster, forage patches, larders | Food Node / Источник еды |
| ore_node | 3.0 | STONE | yes | resource cluster, mine pits | Ore Node / Рудная жила |
| chitin_node | 2.0 | BONE_BLOCK | yes | resource cluster, farm props | Chitin Node / Хитиновый узел |
| chitin_bed | 0.4 | BONE_BLOCK, random ticks | **no own item**; placed with the `chitin_spore` BlockItem (`…/registry/ModItems.java:74`) | chitin farm centre; beds in the queen hall, nursery, barracks, chitin farm and vault sanctum | Chitin Bed / Хитиновая грядка |

- **No loot tables exist** (`data/formic_frontier/` only has `recipe/` and `tags/`). In 1.21, blocks without a loot table drop nothing, so **none of the mod's blocks drop anything when mined**. Players cannot harvest nest_mound, and a crafted building block is lost if broken.
- **Crafting and placing a building block has no sim effect.** There is no `onPlace` registration; the block only opens the UI (§10.2). `NativeBlockRole`'s Javadoc implies that a craftable garden block matters (`…/sim/NativeBlockRole.java:16-19`); it does not.
- **Recipes** for building blocks (`data/formic_frontier/recipe/`):

  | Block | Shaped pattern | Key |
  |---|---|---|
  | armory | `IPI/RCR/IPI` | I iron ingot, P chitin plate, R resin glob, C crafting table |
  | diplomacy_shrine | `ASA/CPC/RDR` | A amethyst shard, S colony seal, C chitin plate, P pheromone dust, R resin glob, D rooted dirt |
  | fungus_garden | `FLF/LML/FLF` | F fungus culture, L leaf mash, M moss block |
  | pheromone_archive | `ASA/SBS/ARA` | A amethyst shard, S seal, B book, R resin |
  | resin_depot | `RRR/RCR/MMM` | R resin, C chest, M packed mud |
  | venom_press | `V / RPR / M` | V venom sac, R resin, P piston, M mandible plate |
  | watch_post | `B / BCB / R` | B bone block, C chitin fiber, R rooted dirt |

  \***[LIKELY-BUG]** The venom_press and watch_post patterns have rows of different widths (1/3/1). Vanilla shaped-recipe parsing rejects that ("each row must be the same width"), so both recipes probably fail to load. Detailed recipe review belongs to the items extraction.

  > **Correction (checked 2026-09-25 while compiling the docs):** this is a false alarm. The actual JSON rows are `" V "`, `"RPR"`, `" M "` and `" B "`, `"BCB"`, `" R "`: all three rows are 3 characters wide, so both recipes are valid. The table above lost the spaces.

### 10.2 `…/block/ColonyInteractBlock.java`

Both `useWithoutItem` and `useItemOn` open the UI of the nearest colony within **96** blocks (`:32-54`). The starting tab depends on the block:

| Block | Tab |
|---|---|
| COLONY_LEDGER, NEST_CORE, NEST_MOUND | Overview |
| MARKET_CHAMBER | Trade |
| PHEROMONE_ARCHIVE | Research |
| DIPLOMACY_SHRINE | Diplomacy |
| anything else | Buildings |

If no colony is within range it shows the literal "No Formic colony is linked to this block."

Because this applies to the thousands of NEST_MOUND blocks in every mound, **right-clicking any mound wall opens the UI**. The click is consumed even with an item in hand, so placing blocks against a mound needs sneaking (vanilla secondary use). [UX issue]

### 10.3 `…/block/ChitinBedBlock.java`

- Property `AGE` 0..4 (`BlockStateProperties.AGE_4`, `:24`).
- Random tick: if age < 4, a 1-in-3 chance to add 1 (`:42-47`). At the default random tick speed (3) a block is random-ticked about every 1365 game ticks. That makes about **205 s per stage and about 13.6 min to mature** (derived).
- Use (`:50-68`):
  - if age < 4: message "Chitin bed growth: N/4";
  - at age 4: drops `1 + rand(0..2)` chitin shards, **+1 if the nearest colony within 96 has CHITIN_CULTIVATION**, plus a chitin spore with **45%** chance. Age resets to 0.
- Blockstate: 5 variants that all point to the same `cube_all` model, so **growth is not visible**.

### 10.4 Blockstates, models, textures

- 19 blockstates with a single variant each (5 for chitin_bed), all `minecraft:block/cube_all`. There are no custom shapes and no rotation.
- 18 block textures, **all 16×16** (read from the PNG headers). The visual intent required 32×32 (`docs/visual-intent/formic-visual-intent.md:140-145`).
- colony_ledger has no texture of its own.
- A stray `textures/block.zip` (10 KB) is in the assets folder and would ship in the jar.

### 10.5 `NativeBlockRole` (fungus composting)

- Runs every economy pass (`NATIVE_BLOCK_TICK_INTERVAL = 20`).
- `run = min(gardens, (food − 40)/5)`. It takes `5×run` food and adds `8×run` fungus (`:35-59`). Constants `FOOD_PER_GARDEN 5`, `FUNGUS_PER_GARDEN 8`, `FOOD_RESERVE 40`.
- It is unit-tested (`src/test/.../sim/ColonyEconomyTest.java` ~l.842-918). [IMPL]

---

## 11. Unit tests for structures (`src/test/java/com/formicfrontier/world/structure/`)

All three classes re-derive geometry from the blueprint (`isSolid` over bounds y 0..maxY) and use a 6-neighbour BFS for connectivity.

- **`TieredMoundBlueprintTest`** (161 lines)
  - Stage 1:
    - height exactly 24; width ≤21; depth ≤18; height > width;
    - 4 tiers, 2 terraces, 3 chambers, 2 connections;
    - purposes {queen_hall, storage, lookout}; 3 mouths at y ∈ {1, 8, 14};
    - terraces solid at (−2,7,−6) and (2,13,−5);
    - carved at (0,2,2), (−2,9,1) and (2,15,1);
    - one connected mass.
  - Stage 2:
    - name and palette; height 33 and height > width; solid volume > 1.65× stage 1; y=0 footprint > 1.35× stage 1;
    - 7 tiers, 3 terraces, 6 chambers, 3 connections, 6 mouths, with the expected purpose set;
    - larder and workshop overlap the queen hall at head height;
    - solid at (−11,1,4) and (10,1,5); carved at (−7,2,4), (6,2,5) and (−1,22,1);
    - crown solid at (0,32,1) and nothing at y 33;
    - the last stair lands in the crown; crown floor at 20; connected.
- **`SubterraneanVaultBlueprintTest`** (126 lines)
  - name and palette; minY −12, maxY 0; 3 purposes; floors −6/−12/−12;
  - access drop 6 landing at (2,3); descent drop 6 landing at (−2,3);
  - chamber centres carved; treasury and sanctum overlap;
  - 2 clear headroom blocks on every step;
  - no mass at (0,1,0); shell connected.
- **`OrganicBuildingBlueprintTest`** (842 lines), per family:
  - variant names; palette; maxY limit or exact value; chamber count and purpose;
  - pits / connections / mouth width;
  - interior carved; `assertRearShell` (the first block behind each vaulted ceiling layer must be solid, so no sky "windows");
  - connected mass; minimum y=0 footprint; distinct footprints per variant;
  - family-specific rules: barracks proportions, market and shrine open to the sky, horn and crown masses, lobe reach, `assertEnclosedExceptMouths` (armory, resin depot, fungus garden, venom press), stair headroom and direction sets (archive, watch post).
  - `positionSelectorIsStable…`: the variant hash is stable and both food-store and nursery variants appear along a line of sites.
  - `repeatedRoleSitesSelectEveryAuthoredVariant`: with origin (11,0,−7), `siteFor` sites use every variant. Spacing: armory ≥34; shrine, resin depot ≥42; fungus garden, venom press ≥46; watch posts pairwise ≥90 (first 4).
  - The trade-hub test also checks spacing from market #1, venom press #1 and watch post #1 (not market #2, §4.5).

Structure behaviour in the world (furnishings, the in-place upgrade, renovation, raids on the vault, label visibility, starter-ant spawn height, expansion markers, …) is covered by GameTests in `src/gametest/.../FormicFrontierGameTest.java` (3085 lines; see the tests/QA extraction).

---

## 12. `ColonyService` method inventory (`…/world/ColonyService.java`, 1040 lines)

| Lines | Method | Behaviour | Status / cross-link |
|---|---|---|---|
| 51-82 | `createColony` ×3, `createWildColony` | founding (§4.2) | [IMPL] |
| 84-93 | `initialRelation`, `isWild` | NEUTRAL/RIVAL rule | diplomacy extraction |
| 95-102 | `anchorToSurface` | heightmap −1 | used by discovery, events, raids |
| 104-121 | `spawnAnt` ×2 | creates an `AntEntity` (reason TRIGGERED), sets caste and colony id (nearest colony within 96 if none given), name, random yaw | castes extraction |
| 123-136 | `colony`, `nearestColony`, `areHostile` | lookups; `areHostile` → `RaidPlanner.areHostile` | |
| 138-152 | `trade` | nearest colony within **96** → `ColonyTradeCatalog.execute`; opens the Trade tab | trade extraction |
| 154-203 | `completeContract` | nearest within **128**; delivers the **whole** missing amount with the resource's item (FOOD wheat, ORE raw iron, CHITIN chitin shard, RESIN resin glob, FUNGUS fungus culture, VENOM venom sac, KNOWLEDGE pheromone dust; `:535-545`), item count from `ContractDeliveryOption`; gives PHEROMONE_TOKEN × reward. A completed "construction …" contract with nothing incomplete runs `ColonyBuilder.tick` immediately (`:551-559`) and reports "Construction started: <id>". A completed expansion contract calls `completeExpansionOutpost`. Feedback line: "… \| +N tokens, +M rep" | [IMPL]; contracts: trade/events extractions |
| 205-231 | `setTopPriority` | nearest within 96; moves the instinct to the front; event "Instinct changed to X by <player>" | drives §3.6 |
| 233-296 | `performDiplomacy` ×2 | nearest within 128; rank gate; target explicit or auto (WAR < RIVAL < NEUTRAL < ALLY, then distance, `:592-613`); costs in tokens (TREATY_SIGILS ×0.75 ceil for ENVOY/TRUCE, `:622-628`), dust, seals, banners (creative: free); symmetric relation change; reputation; `DiplomacyConsequences.apply` places world markers | diplomacy extraction |
| 298-316 | `startResearch` | nearest within 128 → `ColonyLogistics.startResearch` | research extraction |
| 318-381 | `handleAntInteraction` | empty hand → Overview. ROYAL_JELLY on the queen: heal 24, raise stored queen health to the ant's health, +6 rep. RAW_BIOMASS +8 food or WHEAT +3 food, +1 rep, heal 2. CHITIN_SHARD +3 chitin, +1 rep, heal 4. PHEROMONE_DUST sets the instinct from the caste (MINER→ORE, SOLDIER/MAJOR/GIANT→DEFENSE, QUEEN→CHITIN, WORKER/SCOUT→FOOD, `:630-637`). One item consumed unless in creative. | castes/items extractions |
| 383-389 | `openColonyScreen` ×2 | sends `ColonyUiPayload(ColonyUiSnapshot.from(...))` | UI extraction |
| 391-401, 934-1039 | `tabletText` + `buildingsText`, `instinctText`, `diplomacyText`, `requestsText`, `researchText`, `relationsText`, `eventsText` | a text dump of the tablet; also the only user of `ColonyTrades.tradeText` | **[DEAD]**, no callers (ColonyTrades is therefore dead too) |
| 403-417 | `depositWorkedResource` ×2 | adds ant-carried resources to the colony (**created, not moved**); logs an event on **every** delivery, which floods the 10-entry log | the 5-argument overload is **[DEAD]** (unused `savedState` local) |
| 419-443 | `depositConstructionWork` | +max(1, amount) to `firstIncomplete` (possibly a repairing building, §3.2); **re-places the structure on every deposit**; on completion syncs labels and logs "Worker crew completed X" | [IMPL] |
| 445-491 | `renovateNearestColony` (radius 160), `renovateColony` | §4.10 | [IMPL] (command) |
| 493-529, 575-590 | `consumeOne`, `removeItems`, `giveItem`, `itemName`, `hasItems` | inventory helpers; creative ignores costs; `giveItem` drops the stack if the inventory is full | |
| 531-573 | `ContractBundle` record, `contractBundle`, `contractItem`, `isConstructionContract`, `advanceConstructionContract`, `ProgressFactory` | `ContractBundle.resourceAmount()` is **[DEAD]** | |
| 615-620 | `diplomacyCostText` | "N tokens, N dust, N seals, N war banners" | |
| 639-705 | `seedEconomy`, `placeNest`, `registerStarterBuildings`, `registerChamberForBuilding` | §4.2–4.3 | |
| 707-786 | `set`, `clearSettlement`, `placeResourceCluster`, `placePath` [DEAD], `placeWidePath`, `paintWidePathTile` | §4.3 | |
| 793-903 | `connectOriginTrail` ×2, trail accents | §4.7 | **[DEAD]** |
| 905-932 | `spawnStarterCastes`, `applyAntName` | §4.2 | |

**Magic radii**: 96 (trade, instinct, ant fallback, block UI, deposits, chitin-bed bonus), 128 (contracts, diplomacy, research, construction deposit, ant assignment), 160 (renovate), 80 (label sweep), 144/96 (discovery clearances). The values are inconsistent and undocumented.

---

## 13. History: old structures and how the anthill look evolved

### 13.1 Removed `formic_structures/*.json` (master; deleted in `6fb5bcd`)

- **Format** (`git show master:src/main/java/com/formicfrontier/qa/FormicSchematic.java`):
  - `{name, note, palette{char → semantic material}, layers[{y, rows[strings]}]}`;
  - the origin is the bottom centre, `x = col − width/2`, `z = row − depth/2`;
  - characters mapped to "air" and unmapped characters are **skipped**, so the format never carved anything;
  - blocks were placed with raw `setBlockAndUpdate` and no replacement policy.
  - Semantic vocabulary: `mound`→NEST_MOUND, `dark`/`core_wall`→NEST_CORE, `food_core`→FOOD_CHAMBER, `food_node`→FOOD_NODE, `dirt`→COARSE_DIRT, `rooted`→ROOTED_DIRT, `mud`→PACKED_MUD, `roots`→MANGROVE_ROOTS, `path`→DIRT_PATH, `moss`→MOSS_BLOCK, `glow`→OCHRE_FROGLIGHT, unknown→NEST_MOUND.
  - The files were produced by `scripts/gen-food-variants.py` (also deleted). Its docstring says the old schematic convention put **entrances at +Z (true south)**, while the new blueprints put them at −Z.
- **Files**:

  | File | Grid (W×D×layers) | Note (from the file) | Used at runtime? |
  |---|---|---|---|
  | queen_mound_a | 25×25×20 (y0–19): 2040 mound, 282 rooted, 248 dirt, 167 moss, 1 food core | "grand cathedral spire: broad base sweeping to a tall off-centre central peak with two asymmetric shoulder lobes" | **Yes** (`placeQueenHall`). Clipped to an elliptical footprint (score ≤300) and carved by about 25 procedural detail passes (entrances, vents, runnels, porches, trail forks, waystones, crown terraces…). A Colony Ledger was placed at (3,1,0) and a froglight at (0,3,0). |
  | queen_mound_b | 25×25×17 | "twin-turret citadel" | no |
  | queen_mound_c | 27×27×15 | "broad layered citadel … more horizontal authority than height" | no |
  | food_chamber_v1 | 11×11×8 | "rounded earthen dome … hollow interior … dark south tunnel … closed dome roof" | no |
  | food_chamber_v2 | 15×15×15 | "tall termite-style spire (chosen hero food building)" | no |
  | food_chamber_v3 | 19×13×10 | "clustered multi-bulb mound … two smaller satellite bulbs" | no |
  | food_store | 11×11×13 | "campus-fit termite spire (radius≤5, height≤14), arched south entrance, food baskets, hollow FOOD_CHAMBER core" | **Yes** (`placeFoodStoreSchematic`) |

- All other building types on master came from about 3,000 lines of procedural Java: `placeCampusBuilding` with a per-type lobe, taper, noise and roof style, `placeCampusCrownAndTunnelMouth`, stage overlays and so on.
- **Old versus new**:

  | Aspect | Old (master) | New blueprints |
  |---|---|---|
  | Representation | explicit voxel grids (thousands of characters per building) plus imperative carving code | a small parametric IR (tiers, chambers, mouths, pits, stairs), validated and compiled deterministically |
  | Where structures come from | only 2 buildings used schematics | every family has blueprints |
  | Silhouettes | symmetric spire/dome shapes | asymmetric lobes |
  | Interiors | no furnished interiors | purpose-furnished rooms and internal stairs |
  | Variants | no variants | 1–3 variants per family |
  | Testing | nothing tested the geometry | unit tests check connectivity and enclosure |

### 13.2 Timeline (`git log --date=short`)

- **2026-04-29** `9ed2fa7` Add Prime Ants mod project.
  - First layout: rings of radius 22 (`git show 9ed2fa7:…/ColonyBuilder.java`, `siteFor`), +10–12 per repeat.
  - Structures were already placed through `StructurePlacer.placeBuilding`. ColonyService also carried unused helpers from the start — 5×5 "pods" (core, shell, accents) and a 7×7, 3-layer core mound — that were deleted in `6fb5bcd`.
- **2026-05-01** `a74a7fa` autonomous QA harness; `b9f9faa` **"Renovate colony scale and tablet UI"**.
  - The ring radius grew from 22 to **38**, with +18–20 per repeat.
  - Sites were added for GREAT_MOUND, QUEEN_VAULT and TRADE_HUB. This matches roadmap R1's "1.7–2× linear scale" (`docs/roadmap.md:64-74`).
- **2026-06-23** the **R2 series**: `15b5932`, `7af4b42` ("Checkpoint: fresh screenshots + GPT-5.4 mini assessment (attempt 6)"), `59fba60`, `4bd779b`, `556111f` ("R2 retry-07"), `33c1659` ("R2 retry-07 fix").
  - Procedural "satellite campus crowns": tapering native-earth crowns from y6 to 16/19, later a broad ogive dome, plus deep 5×5/5×6 tunnel mouths on the −Z face, buttress ribs, approach aprons and spoil piles.
  - The commit bodies show that the **reach of each change was capped at ±5 blocks so gametest markers would not break** (diplomacy caches at ±6, an expansion crew post).
  - Changes were driven by an external LLM screenshot assessor.
  - Forest-floor dressing was added and then limited to native ground.
  - "R2" is the roadmap slice "Architecture polish". "retry-NN" and "attempt N" are autonomous-loop retries.
- **2026-06-28** `cc9db89` **"Stabilize autonomous loop + one-mound colony with carved chambers"**.
  - Harness and proxy fixes, 10 content rows.
  - "single shared-mound landmass fuses the colony into ONE earthen organism" (§4.11) and "carved dark chamber mouths".
- **2026-06-28** `f5e9857`: the queen and food store switched to FormicSchematic JSON; `c3a7aeb`: tablet UI rebuild.
- **2026-07-11** `946ea44` "Add caste balancing and refine colony landmass". This is the last master commit.
- **2026-07-11** `6fb5bcd` **"Reset anthill structures to minimal markers"**.
  - Deleted 6,846 lines: StructurePlacer −3,214, FormicSchematic, all 7 structure JSONs, the generator script, and 746 lines of gametests.
  - Every building became a single marker block.
  - The new StructurePlacer Javadoc explains that the old generators were deleted, not feature-flagged, so "new mound work cannot accidentally inherit their geometry" (`StructurePlacer.java:17-25`).
- **2026-07-11** `87d8192` "Build the first-stage queen mound": the TieredMound IR, compiler, stage-1 JSON, unit test and `docs/llm-minecraft-building-workflow.md`.
- **2026-07-11 → 07-12**, the **blueprint series**, one family per commit, each with unit tests and GameTests:
  - food store `9a4ec36`;
  - nursery `84f0e27`;
  - mine + chitin farm `136403b` (added pits);
  - barracks `3740457` (also tweaked the food-store and nursery chamber sizes);
  - market `02ba83a` (added `openToSky`);
  - archive `6e599dd` (first two-floor building);
  - armory `3bb9226` (enclosure test);
  - shrine `0db3024`;
  - resin depot `3087b81`;
  - fungus garden `02d3205`;
  - venom press `c635886`;
  - watch post `6646d2f`;
  - Great Mound `c64bba1` (stage 2);
  - queen vault `f6d8402` (new subterranean IR);
  - trade hub `f4b4e04`.
- After that: tablet redesign (`6b1c0b3`, `06d1005`), `88b040f` "Harden manual visual QA for tall mounds" (pinned QA ground Y, checks the stage 1/2 crowns at Y+23/Y+32), and caste art (`75a70f7`).

---

## 14. Consolidated bugs, inconsistencies and dead code (this scope)

**Gameplay and simulation**
1. Expansion never ends and repeats one type per instinct; ROAD spam inflates rank (§3.6).
2. Upgrades have no benefit and temporarily remove the building from every count (§3.3–3.4).
3. "Disabled" (damaged) buildings keep producing; starting the repair is what disables them (§3.2).
4. Possible repeated repair charging, because CasteJobLoop and ant deposits bypass `repair()` (§3.2).
5. The claim radius granted by expansion is overwritten every second (`ColonyBuilder.java:48` vs `ColonyRecurringEvents.java:115`).
6. Pheromone archive knowledge is counted twice (`ColonyEconomy.java:48` + `ColonyLogistics.java:163-165`).
7. The UI shows undiscounted costs (`ColonyUiSnapshot.java:366-376`).
8. Every type builds in the same time (§1.4).
9. The MATURE stage queues the Great Mound without the CITADEL gate (§1.3); stage age thresholds are 3/6/12 seconds.
10. Colonies store no dimension (§8).
11. Discovery phase lock (§7.4); the 6-colony cap includes every colony.

**World and structures**
12. Radius-72 terrain erasure that removes stone, logs, gold blocks and so on, including near players on discovery (§4.3).
13. No per-site terrain adaptation: floating or buried buildings and paths (§4.8).
14. All entrances face −Z; there is no rotation (§4.8).
15. Footprint collisions: nursery #2 × archive #1; barracks #3 × watch post #1; outpost × food store (§4.5).
16. Outpost corner stamps land inside the new watch post (§4.9).
17. Landmark and event trails can repaint building shells, because NEST_MOUND is replaceable and the exclusion radii of 7–8 blocks (`ColonyRecurringEvents.java:1097-1104`, `…/world/DiplomacyConsequences.java:451-458`) are smaller than the mound radii of about 8–14.
18. A queen upgrade after the Great Mound would refill its annexes (§6.2).
19. Visual stages are invisible; chitin beds reset on every re-placement (§2.2).
20. Label duplication beyond ±80 blocks and in unloaded chunks, plus stale label text (§9).
21. Decoration offsets hard-coded in Java throw at placement time if a JSON chamber shrinks (§5.3).
22. Culture is ignored by the structure placers (§5.1).
23. No loot tables; placed building blocks have no sim effect; two building recipes are probably invalid (§10) — **false alarm, see the correction in §10.1: all rows are 3 wide.**

**Dead or stale code**
- `BuildingType.canStart/consumeStartCost`; `ColonyBuilding.repairing`; `BuildingVisualStage.CODEC/fromId`.
- `StructurePlacer.placeTradeHub/placeCampusBuilding/placeStagedBuilding/placeColonyLedger`.
- `ColonyService.placePath`, `connectOriginTrail` and the trail accents, `tabletText` and its helpers, the 5-argument `depositWorkedResource`, `ContractBundle.resourceAmount`.
- `ColonyTrades` (only reached from `tabletText`).
- The unreachable WATCH_POST/ROAD fallbacks in `enqueueNextBuilding`; `NativeBlockRole.wantedFood`.
- The `NestChamber` list (stale data).
- Orphan lang keys `ui.status.disabled` and `ui.status.building`.
- Stale comments about the "shared landmass" (`VisualQaScenes.java:640-652`), `connectOriginTrail`'s callers, and the "one-block baseline" (`ColonyRecurringEvents.java:516-518`).
- The empty `formic_structures/` folder.

---

## 15. Design intent vs. reality

| Intent (source) | Reality in code |
|---|---|
| "Monumental earthen ant-hill architecture with a compact base and a tall, multi-level tapered silhouette"; main mound 20–30 tall (`docs/visual-intent/formic-visual-intent.md:35-36, 183-184`; `docs/roadmap.md:41-44`) | **Met.** Stage 1 is 24 tall (21×17), stage 2 is 33 (25×19), with three and four furnished floors. |
| "Several role-specific landmarks … readable spacing … no colliding/overlapping houses" (`formic-visual-intent.md:38-41, 169`; `roadmap.md:54-56`) | **Mostly met** for the first instance of each family: 14 families, unit-tested spacing. **Not met** for repeated instances: collisions in §4.5, no cap on repetition. |
| Every visible floor leads to a real furnished chamber, joined by stairs with 2-block headroom (`formic-visual-intent.md:118-121`) | **Met** and enforced by tests. The furniture is decorative only (empty containers). |
| Tunnel mouths "scatter entrance positions with noise (varied X/Y/Z and size 1×1 up to 3×3)", dark throat 4–6 deep (`formic-visual-intent.md:109-114`) | **Partly met.** Mouths are authored, 3–5 wide and 4–8 deep, with a MUD rear wall that the chamber usually cuts away. There is no noise, and **every mouth faces −Z**. |
| "Every role building must be CLOSED on top … no open-topped boxes" (player feedback 2026-06-27, `formic-visual-intent.md:53-58`) | **Deliberately contradicted later**: the market, the shrine and the trade-hub court are `openToSky` by design (`docs/llm-minecraft-building-workflow.md:99-137`). |
| Native Formic material language, avoid honey/honeycomb/amethyst/blue-crystal accents; 32×32 textures (`formic-visual-intent.md:129-157`) | **Not met.** Palettes use HONEYCOMB, HONEY, AMETHYST, GOLD and CUT_COPPER accents. Mod blocks are plain 16×16 `cube_all` textures, the chitin bed has no growth stages, and the ledger has no texture. |
| Building visual stages: planned, building, complete, upgraded, damaged and repairing "visually distinct" (`roadmap.md:229-237`) | **Not met in the world.** Only PLANNED (a pin) and CONSTRUCTION (one block) differ. UPGRADED, DAMAGED and REPAIRING look like COMPLETE; only labels differ, and they are stale or duplicated. |
| Culture architecture: Amber warm clay/resin, Leafcutter fungus/green, Fire dark/military, Carpenter wood/resin — "four cultures recognizable in one screenshot" (`roadmap.md:239-249`) | **Not met.** The `culture` parameter is ignored by every placer. The `culture_styles` QA scene differs only in each culture's first starter building. Culture affects only the landmark blocks of wild colonies (§7.3). |
| Players choose a culture path (`roadmap.md:370-380`) | **Not met.** Allied colonies are always AMBER. Other cultures exist only for rival and wild colonies. |
| "A construction site appears, receives materials, changes form, becomes a completed building" (`roadmap.md:294-302`) | **Partly met.** A dirt pin, then one marker block, then the whole building within about 6 s. The costs are paid up front; ant deliveries add progress but consume no stock. |
| "Old buildings can improve or become damaged, and workers repair them when resources exist" (`roadmap.md:304-312`) | **Mechanically present** (chitin repair cost, crew progress) but invisible; upgrades are pointless (§3.4). |
| "Storage buildings have capacity; overflow and shortage have consequences" (`docs/content-intent/formic-content-intent.md:44`) | **Not implemented.** There is no capacity anywhere. |
| Native blocks with gameplay roles, "craftable/placeable where it makes sense" (`formic-content-intent.md:67-71`) | **Partial.** The Fungus Garden composting role exists but depends on the sim building record, not on placed blocks. Crafted blocks do nothing except open the UI. Chitin beds work anywhere. |
| Survival discovery "without debug commands"; "trails, foraging zones, ruined nests, rival borders" (`roadmap.md:361-389`) | **Implemented** as encounter spawning near players, with landmarks for wild colonies only. It is **probably broken by the phase lock** (§7.4), capped at 6 colonies in total, and there are no worldgen structures. |
| Endgame "great mound, pheromone archive network, underground queen vault, trade hub" (`roadmap.md:418-425`) | Great Mound, vault and trade hub **exist**. The "archive network" existed on master (`placeGreatMoundArchiveNetwork`) and was removed in the reset. The endgame buildings give only small bonuses. |
| Time-based development over "ticks/days" (`formic-content-intent.md:46-52`) | Stage age thresholds are 3/6/12 *seconds* (§1.3); growth is gated by resources only. |
| LLM-editable data with Java owning validation (`docs/llm-minecraft-building-workflow.md:31-61`) | **Largely met.** Exceptions: furniture offsets live in Java and throw at runtime, there is no rotation, and the blueprints and layout rely on the absolute-coordinate variant hash. |

---

## 16. Worth keeping for a rebuild

1. **The tiered-mound IR and compiler as a concept** (tiers + terraces + chambers + pits + stairs + mouths; stable-core radius 0.82 with ±7% boundary noise; vaulted ceilings at 0.9/0.72; a MUD rear wall behind each mouth; PACKED_MUD floors; purpose-driven furnishing).
   - It is compact: 60–245 lines of JSON per building.
   - It validates ranges, overlap, anchoring, fit and reachability before touching the world.
   - It produced 30 convincing silhouettes, and each family's design notes are in `docs/llm-minecraft-building-workflow.md:69-235`.
   - Worth adding: **rotation** (facing the hub or a path), per-site **terrain adaptation** (anchor each site, fill foundations, blend skirts), and furniture defined in data rather than Java offsets.
2. **The structural test suite**: 6-neighbour connectivity, the rear-shell and "enclosed except authored mouths" checks, stair-headroom walks, minimum footprints, distinct variants, spacing contracts. It is cheap and caught real defects, such as a sky hole in the armory (`docs/llm-minecraft-building-workflow.md:123-125`).
3. **The in-place evolution of the central mound**: stage 2 keeps stage 1's rooms at the same coordinates, then adds annexes and a crown. The separate **subterranean IR** has shell envelopes, BFS reachability and a `safeCarve` excavation policy. Both are good patterns; guard against re-placing an older stage over a newer one.
4. **Variant families with a stable position hash**, so repeated buildings do not look cloned. Compute the hash relative to the colony, or store the chosen variant, if layouts must be predictable.
5. **The building catalog and roles**: the cost table (§1.1) and effect table (§1.2) are a usable starting balance. They need real differentiation, such as build time scaled by cost, upgrades with effects, capacity, and damage that matters.
6. **The discovery algorithm**: a deterministic per-player, per-region seed, 12 attempts at 56–96 blocks, flat-ground sampling at ±8 with a ±2 height tolerance, clearance radii. Pair it with a cadence that works (§7.4) and a smaller, non-destructive footprint.
7. **Surface landmarks for wild colonies**: trails, forage patches with food nodes, a ruined scout nest, boundary markers and an approach trail head, keyed to culture (§7.3). They are the only existing "readable from the world" culture signal.
8. **Lifecycle model** (level, progress, disabled) with the six visual stages and the EN/RU status and label strings (§2.2). Keep the model and make every stage show in the world.
9. **Replacement policy separation**: `canReplace` (conservative, preserves block entities) versus `safeCarve` (excavation). Narrow `canReplace` so it cannot delete player blocks (gold, stone, logs, fences, lanterns…), and never mass-clear 72 blocks.
10. **The saved-state shape**: a list of colony records with building *records* rather than geometry. Add a dimension key, codecs that fail softly on unknown ids, and drop the unused NestChamber list.
