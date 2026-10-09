# 03 — Diplomacy, trade, caravans, contracts, armory, raids, recurring events

Extraction from the old "Prime Ants" project (`formic_frontier`, package `com.formicfrontier`), branch
`rebuild/anthills-from-scratch` at commit `75a70f7`. All paths are relative to the repo root
`<original-project>`. Citations are `path:line`. Abbreviations used below:
`sim/` = `src/main/java/com/formicfrontier/sim/`, `world/` = `src/main/java/com/formicfrontier/world/`,
`net/` = `src/main/java/com/formicfrontier/network/`, `client/` = `src/client/java/com/formicfrontier/client/`,
`GT` = `src/gametest/java/com/formicfrontier/test/FormicFrontierGameTest.java`,
`UT` = `src/test/java/com/formicfrontier/sim/ColonyEconomyTest.java` (JUnit),
`lang` = `src/main/resources/assets/formic_frontier/lang/{en_us,ru_ru}.json` (both files have identical key order, so line
numbers are the same in both).

Status tags used: **[Implemented]** works as coded and is reachable in normal play; **[Partial]** works but is incomplete,
inconsistent or has a notable bug; **[Command-only]** reachable only through `/formic ...` commands; **[Dead]** code that is
never called / data that is never produced; **[Flavour]** produces visuals/messages but no mechanical effect.

---

## 0. Units, clocks and where everything runs

| Clock | Definition | Source |
|---|---|---|
| Game tick | 1/20 s | Minecraft |
| **Pass** (economy pass + world pass) | Runs once every `ColonyEconomy.ECONOMY_TICK_INTERVAL = 20` game ticks (1 s at 20 TPS). The server tick handler first calls `ColonySavedState.tickEconomy()`, then `tickWorld(overworld)` | `src/main/java/com/formicfrontier/FormicFrontier.java:34-46`, `sim/ColonyEconomy.java:4` |
| `ColonyData.ageTicks` | +20 per economy pass → measured in game ticks | `sim/ColonyEconomy.java:10` |
| `raidCooldown` | **Not game ticks.** Decremented by 1 in `RaidPlanner.tick` (world pass, only if ≥ 2 colonies exist) **and** by 1 in `CasteJobLoop.patrol` (economy pass, only if SOLDIER+MAJOR > 0). So it falls 1–2 units per second (normally 2) | `world/RaidPlanner.java:29`, `sim/CasteJobLoop.java:78-97` (early return at `:80-82`, decrement at `:97`) |
| `ColonyBuilding.disabledTicks` (raid damage) | Decremented `1 + min(SOLDIER+MAJOR, 3)` per economy pass, only if the colony has defenders | `sim/CasteJobLoop.java:83-96`, `PATROL_DEFENDERS_PER_BUILDING = 3` at `:26` |
| `/formic colony tick N` | Runs `max(1, N/20)` full passes instantly | `src/main/java/com/formicfrontier/command/FormicCommands.java:103-113` |

Order inside one pass (`world/ColonySavedState.java:97-191`):
1. Economy pass, per colony: `ColonyEconomy.tick` → `ColonyLogistics.tick` (auto-fulfil requests, research) → `CasteJobLoop.tick`
   (gather/build/**patrol: raid-cooldown and damage timers**/tend) → `ColonyStageProgression` → `CasteBalancer` → `NativeBlockRole` (`:101-116`).
2. **Autonomous envoy pass**: every colony → `DiplomacyService.tick(colony, nearestNonAlly)` (`:123-128`).
3. **Caravan pass**: every colony → `TradeCaravan.exchange(colony, nearestNonHostile)` (`:133-138`).
4. World pass: `ColonyDiscoveryService.tick` (wild colonies), then per colony `ColonyBuilder.tick` (construction/repair) and
   **`ColonyRecurringEvents.tick`**, then **`RaidPlanner.tick`** for all colonies (`:180-191`).

Two parallel "realities" exist and never reconcile:
- **Abstract colony state** (`ColonyData` caste counts, resources, relations) — used by every rule in this document.
- **Spawned `AntEntity` mobs** — events, pacts and raids spawn ants for show (`ColonyService.spawnAnt`, `world/ColonyService.java:108-121`),
  but spawning does not add to caste counts and entity deaths do not subtract from them (no such code in
  `src/main/java/com/formicfrontier/entity/AntEntity.java`). Hostile entities do fight each other: ants target ants of any
  colony pair where either side views the other as RIVAL/WAR (`AntEntity.java:75` → `ColonyService.areHostile` →
  `world/RaidPlanner.java:48-59`).

### 0.1 Colony kinds (initial values relevant to this scope)

| Kind | Created by | faction | playerAllied | reputation | raidCooldown | culture | Source |
|---|---|---|---|---|---|---|---|
| Allied (player's) | Queen Egg item, `/formic colony create` | `"allied"` | true | 0 | 0 | AMBER | `sim/ColonyProgress.java:68-70` |
| Rival | `/formic colony seed-rivals <count 1-6> <distance 32-256>` only (and QA scenes/tests) | `"rival"` | false | −10 | 200 (FIRE: 120) | `ColonyCulture.rivalFor(id)` = LEAFCUTTER/FIRE/CARPENTER by `(id-1) mod 3` | `sim/ColonyProgress.java:72-78`, `sim/ColonyCulture.java:89-92`, `command/FormicCommands.java:39-46,115-125` |
| Wild | `ColonyDiscoveryService` near survival players (check every 900 ticks = 45 s; 56–96 blocks away; none within 144 of an existing colony; max 6 colonies total; creative/spectator skipped) | `"wild"` | false | −2 | 240 | seeded random of 4 | `sim/ColonyProgress.java:80-82`, `world/ColonyDiscoveryService.java:15-39` |

Codec defaults if a field is missing in saves: faction `wild`, reputation 0, claimRadius 18, raidCooldown 0, no relations
(`sim/ColonyProgress.java:13-30`). Reputation is clamped to −100..100, claim radius to 18..48, cooldown to ≥ 0 (`:55-61`, `:223-225`).
Every new colony starts with FOOD 120, ORE 20, CHITIN 24, RESIN 24, FUNGUS 12 (LEAFCUTTER 28), VENOM 4 (FIRE 16), KNOWLEDGE 8 and
castes QUEEN 1, WORKER 3(+culture bias), SCOUT 1(+bias), MINER 2, SOLDIER 2, MAJOR 1 (`world/ColonyService.java:639-653`) —
i.e. military strength exactly **16**, the raid threshold.

---

## 1. Diplomacy

### 1.1 `DiplomacyState` enum — [Implemented]

`sim/DiplomacyState.java`

| Constant | id | `hostile()` | `improve()` | `worsen()` | EN (`formic_frontier.relation.*`, lang:86-89) | RU |
|---|---|---|---|---|---|---|
| `ALLY` | `ally` | no | ALLY | NEUTRAL | Ally | Союз |
| `NEUTRAL` | `neutral` | no | ALLY | RIVAL | Neutral | Нейтралитет |
| `RIVAL` | `rival` | **yes** | NEUTRAL | WAR | Rival | Соперничество |
| `WAR` | `war` | **yes** | RIVAL | WAR | War | Война |

- `hostile()` = RIVAL or WAR (`:25-27`); `improve/worsen` (`:29-43`); string codec (`:13`); `fromId` throws on unknown (`:45-53`).
- **No numeric relation score exists.** Relations are a 4-state ladder; there are no thresholds, no decay, no drift over time
  (grep for "decay" in `src/main/java` finds nothing).
- Storage: each colony keeps its **own one-directional view** `knownColonies: Map<String colonyId, DiplomacyState>`
  (`sim/ColonyProgress.java:42`). Unknown ids read as **NEUTRAL** (`:215-217`). A↔B views can differ (see 1.5, 1.6, 4.1, 5.4).
- `knownColoniesView()` returns `Map.copyOf(...)` (`:142-144`), whose iteration order is unspecified in Java → the order of the
  Relations list in the tablet (and the default diplomacy target, see 1.4) is effectively arbitrary.
- Initial relations when a colony is created, set symmetrically with every existing colony (`world/ColonyService.java:74-78,84-89`),
  and re-applied for missing pairs by `RaidPlanner.ensureRelation` each pass (`world/RaidPlanner.java:61-69`):
  **NEUTRAL** if either colony is wild or both have the same `playerAllied` flag; otherwise **RIVAL**. So only allied↔rival pairs
  start hostile; rival↔rival, allied↔allied and anything↔wild start neutral.
- UI: relations are listed with a bar value ally 100 / neutral 60 / rival 30 / war 10 (`client/screen/ColonyStatusScreen.java:1077-1085`).

### 1.2 Reputation — [Partial]

A single integer per colony (`ColonyProgress.reputation`), conceptually "how much this colony trusts the player" (shared by all
players; there is no per-player reputation). Range −100..100 (`sim/ColonyProgress.java:223-225`). **No decay.**

Sources and sinks (all found by grep for `addReputation`):

| Change | Amount | Trigger | Source |
|---|---|---|---|
| Sell trade | +1 … +5 per trade (per offer, see 2.2) | player trade | `sim/ColonyTradeCatalog.java:78` |
| Contract delivery | +1 … +6 (formula in 4.1) | player delivery | `sim/ColonyLogistics.java:72-74` |
| Player diplomacy (actor colony) | ENVOY +3, TRIBUTE +8, TRUCE +6, INCITE −6, WAR_PACT −10 | player action | `world/ColonyService.java:282` |
| Autonomous envoy (actor colony) | +3 per relation step | every pass (1.6) | `sim/DiplomacyService.java:52` |
| Colony-to-colony caravan shipment (source colony) | +1 per shipment | every pass (3.1) | `sim/TradeCaravan.java:62` |
| Royal jelly fed to the queen ant | +6 (also heals the queen ant 24) | right-click | `world/ColonyService.java:330-340` |
| Wheat / raw biomass fed to any ant | +1 (+3 / +8 FOOD) | right-click | `world/ColonyService.java:342-352` |
| Chitin shard fed to any ant | +1 (+3 CHITIN) | right-click | `world/ColonyService.java:354-364` |

Where reputation is read:

| Use | Thresholds | Source |
|---|---|---|
| Token price multiplier on "buy" trades | rep < 0 → ×1.25; 0–24 → ×1.0; 25–59 → ×0.9; ≥ 60 → ×0.75 | `sim/ColonyTradeCatalog.java:92-98` |
| +1 token bonus on "sell" trades | rep ≥ 50 (only offers paying < 8 tokens) | `sim/ColonyTradeCatalog.java:109` |
| Min-reputation gates on offers | 15, 20, 40 (see 2.2) | `sim/ColonyTradeCatalog.java:42-54,63-65` |
| Colony rank score | + max(0, rep) | `sim/ColonyRank.java:55` |
| Relationship label (player-allied colonies only) | rep ≥ 50 `trusted`, ≥ 15 `friendly`, < 0 `strained`, else `new_allies`; non-allied: `wild` or `rival` | `sim/ColonyIdentity.java:15-33` |
| Guide "Relations" chapter unlock | rep ≠ 0 (or any known colony, or rank ≥ BURROW) | `sim/GuideChapter.java:50-53` |

Relationship labels (lang:90-95) and colours (`sim/ColonyIdentity.java:35-44`):

| id | EN | RU | colour |
|---|---|---|---|
| new_allies | New allies | Новые союзники | 0xC9974B |
| friendly | Friendly | Дружелюбно | 0x91C46C |
| trusted | Trusted | Доверие | 0x6DD08E |
| strained | Strained | Напряжено | 0xD69042 |
| rival | Rival colony | Колония-соперник | 0xC15D48 |
| wild | Wild colony | Дикая колония | 0xD8B57A |

Issue: AI-only activity (autonomous envoys +3/step, caravan shipments +1/s) inflates the "player trust" number, which then
discounts the player's trade prices. Reputation semantics are muddled (player trust vs colony prestige).

### 1.3 `DiplomacyAction` enum — [Partial]

`sim/DiplomacyAction.java:6-10` (fields: id, label, tokenCost, dustCost, sealCost, bannerCost, reputationDelta, minRank, direction).

| Constant | id | Label (hard-coded EN) | Pheromone Tokens | Pheromone Dust | Colony Seals | War Banners | Reputation | Min rank (actor) | Direction |
|---|---|---|---|---|---|---|---|---|---|
| `ENVOY` | `envoy` | Send Envoy | 8 (6 with Treaty Sigils) | 1 | 0 | 0 | +3 | OUTPOST | IMPROVE (1 step) |
| `TRIBUTE` | `tribute` | Pay Tribute | 16 | 1 | 1 | 0 | +8 | BURROW | IMPROVE_TWICE |
| `TRUCE` | `truce` | Broker Truce | 24 (18 with Treaty Sigils) | 2 | 1 | 0 | +6 | HIVE | NEUTRALIZE |
| `INCITE` | `incite` | Incite War | 14 | 3 | 0 | 1 | −6 | BURROW | WORSEN (1 step) |
| `WAR_PACT` | `war_pact` | War Pact | 28 | 4 | 1 | 1 | −10 | CITADEL | WAR |

`apply()` (`:66-74`) — full transition matrix (row = relation before, as seen by the actor):

| before \ action | ENVOY | TRIBUTE | TRUCE | INCITE | WAR_PACT |
|---|---|---|---|---|---|
| ALLY | ALLY | ALLY | ALLY | NEUTRAL | WAR |
| NEUTRAL | ALLY | ALLY | ALLY | RIVAL | WAR |
| RIVAL | NEUTRAL | ALLY | NEUTRAL | WAR | WAR |
| WAR | RIVAL | NEUTRAL | NEUTRAL | WAR | WAR |

Observations:
- TRUCE (HIVE, 24T+2D+1S) is dominated by TRIBUTE (BURROW, 16T+1D+1S): tribute gives the same or a better result from every
  state. Truce's only unique value is its consequence (raid cooldown, 1.5).
- Rank gates are checked against the **actor** colony's rank (`ColonyRank`, `sim/ColonyRank.java`): OUTPOST 0, BURROW 35, HIVE 85,
  CITADEL 155 score, where score = 10×completed buildings + 2×population + max(0, rep) + queenHealth/12 + 12×(ALLY entries in its
  relation map) (`:4-7,50-58`). A fresh colony (5 starter buildings, 10 ants, 140 hp queen) scores 81 → **BURROW from the start**
  (ENVOY/TRIBUTE/INCITE unlocked immediately; TRUCE after one more building). A fresh LEAFCUTTER rival (12 ants) scores 85 → HIVE.
- Rank display names in diplomacy UI are English literals (`ColonyRank.displayName()`), although translated rank keys exist
  (lang:64-67: Outpost/Форпост, Burrow/Нора, Hive/Улей, Citadel/Цитадель).
- Cost items: Pheromone Token (no crafting recipe; obtained only from sell trades and contract rewards); Pheromone Dust
  (shapeless recipe: honeycomb + chitin shard → 2 dust, `src/main/resources/data/formic_frontier/recipe/pheromone_dust.json`);
  Colony Seal (shaped: token + 2 dust + chitin plate + paper, `recipe/colony_seal.json`); War Banner (shaped: 2 chitin plate +
  3 red dye + black banner + stick, `recipe/war_banner.json`). Seal and banner can also be bought (2.2). Item crafting is
  another agent's scope.

### 1.4 Player diplomacy flow (`ColonyService.performDiplomacy`) — [Partial]

Entry points: tablet Relations tab buttons → C2S `DiplomacyRequestPayload(actionId, targetColonyId)` (`net/DiplomacyRequestPayload.java:10-19`,
`src/main/java/com/formicfrontier/registry/ModNetworking.java:33-35`); command `/formic diplomacy <action>` (no target argument →
auto target) (`command/FormicCommands.java:66-68,167-173`). Commands have no permission requirement.

Steps (`world/ColonyService.java:237-296`):
1. Parse action; unknown → action-bar message `Unknown diplomacy action: <id>` (`:238-244`).
2. **Actor = nearest colony of any faction within 128 blocks of the player** (`:248`) — can be a rival or wild colony. No colony →
   translatable `formic_frontier.feedback.no_colony` ("No nearby Formic colony." / "Рядом нет муравьиной колонии.", lang:215).
3. Rank gate: `<Label> requires colony rank <Rank>` (`:253-256`).
4. Target: explicit id from the UI if > 0 and ≠ actor; otherwise `diplomacyTarget` = most hostile first (WAR 0 < RIVAL 1 <
   NEUTRAL 2 < ALLY 3), then nearest (`:258-260,592-613`). None → `No known colony to target.`
5. Cost check: tokens (with **Treaty Sigils** research: ENVOY/TRUCE cost `max(1, ceil(cost×0.75))` → 6 / 18, `:622-628`), dust,
   seals, banners. Missing → `Need X tokens, Y dust, Z seals, W war banners` — this message uses the **undiscounted** token cost
   (`:615-620`). Creative players pay nothing (`:499-501,575-577`).
6. `before` = actor's view; `after = action.apply(before)`; **sets both views** (actor→target and target→actor) to `after`
   (`:278-281`). Actor reputation += delta (`:282`).
7. Events: actor `"<player> performed <id> toward colony #T: <before> -> <after>"`, target `"Diplomacy from colony #A: <before> -> <after>"` (`:283-284`).
8. World consequence via `DiplomacyConsequences.apply` (1.5): TRIBUTE→ALLY, TRUCE→NEUTRAL, WAR_PACT→WAR. If placed, extra events
   and a themed current task (`:285-292`).
9. Tablet reopens on "Diplomacy" (client maps it to the Relations tab) with `<Label>: colony #T is now <state>` (`:294`).

UI exposure — **only the first 3 actions are reachable in the tablet**: both the button row and the cost list loop `i < 3`
(`client/screen/ColonyStatusScreen.java:718,901`), so **INCITE and WAR_PACT exist only via `/formic diplomacy`** [Command-only].
The cost line shown is `"<T>T <D>D <S>S · <rank>"` using the base token cost (no Treaty Sigils discount, banners not shown)
(`:720-721`; snapshot uses `action.tokenCost()` at `net/ColonyUiSnapshot.java:160-163`). Up to 5 target buttons `#id`, default
target = first relation entry (`client/screen/ColonyStatusScreen.java:884-899`). Lang `formic_frontier.ui.selected_target`
"Selected target"/"Выбранная цель" (lang:182) is used; `formic_frontier.ui.no_target` (lang:181) is unused.

Issues:
- No cooldown and no "state already reached" check: e.g. TRIBUTE on an ALLY still costs, still gives +8 rep (rep farming) and
  re-runs the tribute consequence (new markers + 2 new worker ants each time); WAR_PACT on WAR re-spawns 4 ants and resets raid
  cooldowns.
- Player diplomacy is symmetric, autonomous diplomacy (1.6) is one-sided — two different models of the same relation.
- The actor is simply the nearest colony, so a player standing at a rival nest acts *as the rival* (its rank, its reputation).

### 1.5 Diplomatic consequences in the world (`DiplomacyConsequences`) — [Implemented] (mostly [Flavour])

`world/DiplomacyConsequences.java`. Constants: `ENDPOINT_BUFFER = 12`, `WAR_PACT_ROUTE_OFFSET = 14`, `PACT_CACHE_ROUTE_OFFSET = 10`,
`TRUCE_COOLDOWN_TICKS = 20*90 = 1800` (`:15-18`). "Perpendicular offset" below = `(sideX, sideZ) = (dz, −dx)` or `(0,−1)` if the two
origins coincide, where dx/dz are the signs (−1/0/1) of target−source (`:177-183`). Routes are Manhattan walks (X first, then Z),
skip cells within 12 blocks of either endpoint and within 8 blocks of any building of either colony (`:185-266,446-458`); blocks are
placed with `StructurePlacer.safeSet`, which only overwrites "replaceable" blocks (air, plants, logs, leaves, dirt family, stone,
cobble, deepslate, tuff, terracotta, blackstone…) and never block entities (`world/StructurePlacer.java:68-78,93-…`). Nothing is
ever cleaned up. Mechanical effects are in **bold**.

**Tribute pact** (TRIBUTE ending ALLY; `:24-27,66-72`):
- Beacons at each origin + 12 toward the other: DIRT_PATH, HONEYCOMB_BLOCK, CANDLE; ROOTED_DIRT N and S (`:151-160`).
- Route: alternating DIRT_PATH (even step) / PODZOL; every 9th step a marker on top (AMETHYST_BLOCK every 18th, else HONEYCOMB_BLOCK), ≤ 180 steps (`:185-210`).
- Cache at midpoint + perpendicular×10 (`pactCacheSite`, `:136-149`), forced with `setBlockAndUpdate`: HONEYCOMB, AMETHYST, CANDLE; DIRT_PATH N/S, ROOTED_DIRT E/W (`:268-280`).
- Caravan camp at midpoint + perpendicular×18 (`:124-133`): 5×5 diamond (|x|+|z| ≤ 3; DIRT_PATH inner ring ≤ 1, PODZOL outer), air cleared 2 up; BARREL centre, HAY_BLOCK (−1,+1,0), HONEYCOMB (+1,+1,0), AMETHYST (0,+1,+1); spawns a source-colony WORKER in state CARRYING_RESIN at (−2,+1,−1) and a target-colony WORKER CARRYING_FUNGUS at (+2,+1,+1), each facing the other colony (`:282-311`).
- No resources move. No mechanical effect beyond the relation change itself.

**Truce line** (TRUCE ending NEUTRAL, i.e. from RIVAL/WAR; `:28-31,74-80`):
- **Both colonies' `raidCooldown` = max(current, 1800)** (`:426-429`) → no raids *by either colony against anyone* for 900–1800 s
  (the cooldown is per colony, not per pair — a truce with B also stops A raiding C).
- Seals at each origin + 12: DIRT_PATH, CHISELED_TUFF, CANDLE; MOSS_BLOCK N, ROOTED_DIRT S (`:162-171`).
- Route: step%3==0 MOSS_BLOCK, ==1 DIRT_PATH, ==2 untouched; CHISELED_TUFF marker every 10th step (`:212-241`).
- Cache at pact cache site, forced: MOSS_BLOCK, CHISELED_TUFF, CANDLE; DIRT_PATH N/S, ROOTED_DIRT E/W (`:313-324`).

**War pact** (WAR_PACT ending WAR — always; `:32-35,82-96`):
- **Both colonies' `raidCooldown` = 0** (`:431-434`) → both raid at the next pass if they have strength ≥ 16 and a hostile target.
- Muster camps: source origin + (12·dx + 14·sideX, 0, 12·dz + 14·sideZ); target origin + (−12·dx + 14·sideX, …) — both on the same side of the axis (`:173-175`). War beacon: BLACKSTONE, RED_TERRACOTTA, BONE_BLOCK, CANDLE; POLISHED_BLACKSTONE N/S, DIRT_PATH E/W (`:326-335`).
- War route between the musters: alternating BLACKSTONE/RED_TERRACOTTA, every 8th step marker (BONE_BLOCK every 16th, else POLISHED_BLACKSTONE) (`:243-266`).
- Mid-muster at midpoint + perpendicular×14: RED_TERRACOTTA, BLACKSTONE, BONE_BLOCK; DIRT_PATH N/S, POLISHED_BLACKSTONE E/W (`:113-122,337-345`).
- Spawns 4 PATROLLING source ants at the source muster: SOLDIER (−2,+1,+1), SOLDIER (0,+1,+2), MAJOR (+2,+1,+1), SOLDIER (0,+1,−2), facing the target (`:401-424`).

**Defensive pact response** (called from raids, 6.4; `:98-111`):
- Rally = defender origin + (14·raidDx + 4·allyDx, 0, 14·raidDz + 8·allyDz) — note the asymmetric 4 vs 8 (magic numbers) (`:98-104`).
- Guard beacon: DIRT_PATH, POLISHED_DEEPSLATE, HONEYCOMB, CANDLE; BONE_BLOCK N/S, ROOTED_DIRT E/W (`:347-356`).
- Guard route from the ally's origin to the rally, ≤ 140 steps: alternating DIRT_PATH/ROOTED_DIRT, every 8th step POLISHED_DEEPSLATE (HONEYCOMB every 16th) (`:358-381`).
- 3 PATROLLING ally SOLDIERs at rally (−1,+1,−1), (0,+1,−2), (+1,+1,−1), facing the attacker (`:383-399`).

Messages (`:39-64`, all hard-coded English):

| Action | Source event | Target event | Source current task |
|---|---|---|---|
| TRIBUTE | Tribute pact route marked toward colony #T | Tribute pact route received from colony #A | Diplomacy: tribute pact marked with colony #T |
| TRUCE | Truce line cooled raid route toward colony #T | Truce line accepted from colony #A | Diplomacy: truce line cooled with colony #T |
| WAR_PACT | War pact muster marked toward colony #T | War pact threat mustered by colony #A | Diplomacy: war pact mustering against colony #T |
| other (fallback, unreachable) | `<Label>` consequence marked toward colony #T | `<Label>` consequence received from colony #A | Diplomacy: `<Label>` marked with colony #T |

### 1.6 Autonomous envoy pass (`DiplomacyService`) — [Partial]

`sim/DiplomacyService.java`, called every pass from `world/ColonySavedState.java:123-128`.
- Target = nearest colony the actor does not view as ALLY (`nearestKnownNonAlly`, `world/ColonySavedState.java:161-178`; despite the
  name it does not check "known").
- `tick` requires: completed DIPLOMACY_SHRINE, actor rank ≥ BURROW, actor's view of the target ≠ ALLY (`sim/DiplomacyService.java:66-82`).
- `perform(actor, target, ENVOY)`: ENVOY's own gate (OUTPOST) → `improve()` the **actor's view only**, **actor reputation +3**, event
  `"Diplomacy: Send Envoy toward colony #T (<before> -> <after>)"` (`:42-56`). **No cost, no cooldown.**
- `ENVOY_PASS_INTERVAL_TICKS = 60` is declared with an unrelated copy-pasted comment and **never used** (`:24-25`) → the envoy runs
  every pass (1 s).
- Consequence: any colony with a finished shrine converts its view of every other colony to ALLY within seconds (WAR→ALLY takes 3
  passes), gaining +3 rep per step and +12 rank score per ally. Because culture starter queues put the shrine first for AMBER
  (`sim/ColonyCulture.java:80-87`) and every colony's fallback build sequence ends with DIPLOMACY_SHRINE (`world/ColonyBuilder.java:20-31`),
  over time most colonies unilaterally "ally" everyone. One-sidedness matters: raids use the *attacker's* view and `areHostile` uses
  *either* view, so a rival that still views you as RIVAL keeps raiding and fighting even after your colony "allied" it.
- This is the only autonomous (non-player, non-raid) relation change in the code.

### 1.7 Other diplomatic hooks

- Guide chapter "Relations" (lang:206-208): EN "Tokens and seals shift allies, rivals, wars." / RU "Жетоны и печати меняют союзы,
  соперничество, войны."; locked: "Meet another colony or raise reputation to unlock diplomacy notes." / "Встретьте другую колонию или
  повысьте репутацию, чтобы открыть дипломатию.".
- Right-clicking a DIPLOMACY_SHRINE block opens the tablet on "Diplomacy" → Relations tab; MARKET_CHAMBER opens "Trade"
  (`src/main/java/com/formicfrontier/block/ColonyInteractBlock.java:56-72`).
- Colony personality ("Guarded — Watches borders", etc.) is cosmetic; it has no effect on diplomacy or raids (`sim/ColonyPersonality.java`).

---

## 2. Trade with the player

### 2.1 Two copies of the catalog

- **`ColonyTradeCatalog` is the live one**: used by `ColonyService.trade` (`world/ColonyService.java:146`) and by the tablet snapshot
  (`net/ColonyUiSnapshot.java:134-152`). Lazily-built static list (`sim/ColonyTradeCatalog.java:14-24`).
- **`ColonyTrades` is a byte-for-byte duplicate** of the offers and pricing (verified with diff) plus a text dump `tradeText`
  (`sim/ColonyTrades.java:48-72`). Its only caller is `ColonyService.tabletText` (`world/ColonyService.java:391-401`), which itself
  has no callers → **[Dead]** (the old text-based tablet).

### 2.2 Full trade catalog — [Implemented]

`sim/ColonyTradeCatalog.java:27-55` (record `Offer(id, input, inputCount, output, outputCount, resourceType, resourceDelta,
reputationDelta, minReputation, requiredResearch, requiredCulture)`, `:199-203`). "Colony gets" = resource added to the colony's
stores when the trade completes. "T" = Pheromone Token. Base prices shown; modifiers in 2.3.

**Sell offers** (player gives goods, receives tokens; "Colony requests" column in the UI):

| id | Player gives | Player gets | Colony gets | Rep | Min rep | Research | Culture |
|---|---|---|---|---|---|---|---|
| sell_wheat | 16 Wheat | 1 T | +12 FOOD | +1 | 0 | – | – |
| sell_biomass | 4 Raw Biomass | 1 T | +16 FOOD | +2 | 0 | – | – |
| sell_raw_iron | 8 Raw Iron | 2 T | +12 ORE | +2 | 0 | – | – |
| sell_iron_ore | 8 Iron Ore | 2 T | +12 ORE | +2 | 0 | – | – |
| sell_chitin | 4 Chitin Shard | 2 T | +8 CHITIN | +2 | 0 | – | – |
| sell_resin | 3 Resin Glob | 2 T | +8 RESIN | +2 | 0 | – | – |
| sell_fungus | 3 Fungus Culture | 2 T | +9 FUNGUS | +2 | 0 | – | – |
| sell_venom | 2 Venom Sac | 3 T | +7 VENOM | +3 | 0 | – | FIRE |
| sell_royal_jelly | 1 Royal Jelly | 8 T | +40 FOOD | +5 | 0 | – | – |

**Buy offers** (player pays tokens; "Colony wares" column):

| id | Player pays | Player gets | Min rep | Research gate |
|---|---|---|---|---|
| buy_chitin_spore | 2 T | 1 Chitin Spore | 0 | – |
| buy_pheromone_dust | 4 T | 1 Pheromone Dust | 0 | – |
| buy_resin_glob | 3 T | 1 Resin Glob | 0 | – |
| buy_fungus_culture | 4 T | 1 Fungus Culture | 0 | fungus_symbiosis |
| buy_venom_sac | 5 T | 1 Venom Sac | 0 | venom_drills |
| buy_colony_seal | 16 T | 1 Colony Seal | 20 | – |
| buy_war_banner | 14 T | 1 War Banner | 0 | – |
| buy_chitin_boots | 10 T | Chitin Boots | 0 | – |
| buy_chitin_helmet | 12 T | Chitin Helmet | 0 | – |
| buy_chitin_leggings | 18 T | Chitin Leggings | 0 | – |
| buy_chitin_chestplate | 20 T | Chitin Chestplate | 0 | – |
| buy_resin_chitin_boots | 18 T | Resin Chitin Boots | 15 | mandible_plating |
| buy_resin_chitin_helmet | 20 T | Resin Chitin Helmet | 15 | mandible_plating |
| buy_resin_chitin_leggings | 30 T | Resin Chitin Leggings | 20 | mandible_plating |
| buy_resin_chitin_chestplate | 34 T | Resin Chitin Chestplate | 20 | mandible_plating |
| buy_mandible_saber | 24 T | Mandible Saber | 20 | mandible_plating |
| buy_venom_spear | 22 T | Venom Spear | 15 | venom_drills |
| buy_queen_egg | 64 T | Queen Egg | 40 | – |

Buy offers give the colony nothing and never consume colony stock; sell offers never check whether the colony "wants" the goods.
The market is an infinite abstract vendor attached to whichever colony is nearest.
No rank or stage gates exist on any offer (only reputation, research and culture).

### 2.3 Pricing rules — [Partial] (bug)

Token **cost** of buy offers (`inputCount`, `sim/ColonyTradeCatalog.java:84-103`), applied only when the input is a token:
1. `m = 1.0`; with **Scented Ledger** research `m *= 0.85`.
2. Reputation **overrides** `m` (assignment, not multiplication): rep ≥ 60 → `m = 0.75`; ≥ 25 → `0.9`; < 0 → `1.25`; 0–24 → unchanged.
3. Completed **Trade Hub** → `m *= 0.85` (`TRADE_HUB_TOKEN_COST_MULTIPLIER`, `:12`).
4. Price = `max(1, ceil(base × m))`.

**Bug:** because step 2 assigns instead of multiplying, the Scented Ledger discount is silently discarded whenever rep ≥ 25 or < 0.
With the ledger, raising reputation from 24 to 25 makes prices go *up* (×0.85 → ×0.9).

Token **reward** of sell offers (`outputCount`, `:105-114`): +1 if rep ≥ 50 and base reward < 8; +1 more with a Trade Hub if base < 8
(`TRADE_HUB_TOKEN_REWARD_BONUS = 1`, `:13`). Royal jelly (8 T) never gets bonuses.

Exact prices (computed with Java-double semantics; L = Scented Ledger, H = Trade Hub):

| offer | base | rep<0 | 0-24 | 0-24+L | 25-59 (L ignored) | 60+ (L ignored) | rep<0+H | 0-24+H | 0-24+L+H | 25-59+H | 60+H |
|---|---|---|---|---|---|---|---|---|---|---|---|
| buy_chitin_spore | 2 | 3 | 2 | 2 | 2 | 2 | 3 | 2 | 2 | 2 | 2 |
| buy_pheromone_dust | 4 | 5 | 4 | 4 | 4 | 3 | 5 | 4 | 3 | 4 | 3 |
| buy_resin_glob | 3 | 4 | 3 | 3 | 3 | 3 | 4 | 3 | 3 | 3 | 2 |
| buy_fungus_culture | 4 | 5 | 4 | 4 | 4 | 3 | 5 | 4 | 3 | 4 | 3 |
| buy_venom_sac | 5 | 7 | 5 | 5 | 5 | 4 | 6 | 5 | 4 | 4 | 4 |
| buy_colony_seal | 16 | 20 | 16 | 14 | 15 | 12 | 17 | 14 | 12 | 13 | 11 |
| buy_war_banner | 14 | 18 | 14 | 12 | 13 | 11 | 15 | 12 | 11 | 11 | 9 |
| buy_chitin_boots | 10 | 13 | 10 | 9 | 9 | 8 | 11 | 9 | 8 | 8 | 7 |
| buy_chitin_helmet | 12 | 15 | 12 | 11 | 11 | 9 | 13 | 11 | 9 | 10 | 8 |
| buy_chitin_leggings | 18 | 23 | 18 | 16 | 17 | 14 | 20 | 16 | 14 | 14 | 12 |
| buy_chitin_chestplate | 20 | 25 | 20 | 17 | 18 | 15 | 22 | 17 | 15 | 16 | 13 |
| buy_resin_chitin_boots | 18 | 23 | 18 | 16 | 17 | 14 | 20 | 16 | 14 | 14 | 12 |
| buy_resin_chitin_helmet | 20 | 25 | 20 | 17 | 18 | 15 | 22 | 17 | 15 | 16 | 13 |
| buy_resin_chitin_leggings | 30 | 38 | 30 | 26 | 27 | 23 | 32 | 26 | 22 | 23 | 20 |
| buy_resin_chitin_chestplate | 34 | 43 | 34 | 29 | 31 | 26 | 37 | 29 | 25 | 27 | 22 |
| buy_mandible_saber | 24 | 30 | 24 | 21 | 22 | 18 | 26 | 21 | 18 | 19 | 16 |
| buy_venom_spear | 22 | 28 | 22 | 19 | 20 | 17 | 24 | 19 | 16 | 17 | 15 |
| buy_queen_egg | 64 | 80 | 64 | 55 | 58 | 48 | 68 | 55 | 47 | 49 | 41 |

| sell offer | base T | rep ≥ 50 | Trade Hub | rep ≥ 50 + Hub |
|---|---|---|---|---|
| sell_wheat / sell_biomass | 1 | 2 | 2 | 3 |
| sell_raw_iron / iron_ore / chitin / resin / fungus | 2 | 3 | 3 | 4 |
| sell_venom | 3 | 4 | 4 | 5 |
| sell_royal_jelly | 8 | 8 | 8 | 8 |

No buy→sell arbitrage loop exists at any modifier level (checked for resin, fungus, venom).

### 2.4 Visibility, availability and status text

- **Visible** if the offer has no culture requirement, or it matches the colony's culture, or the colony has **Scented Ledger**
  (`sim/ColonyTradeCatalog.java:120-124`). Only `sell_venom` (FIRE) uses this. Invisible offers are not sent to the client.
- **Available** = visible AND (no research requirement OR researched) (`:126-131`). **Min reputation is not part of "available"**, so
  the client marks rep-locked cards "Available" and clickable, while the status text says "Requires reputation N" and the server
  rejects the click (`:63-65`). Inconsistency.
- Status text priority (`availabilityText`, `:133-148`; hard-coded EN): `Requires reputation N` → `Requires research <node_id>` →
  `Requires <culture> culture` → Trade Hub notes (`Trade Hub: +1 token` for sell offers paying < 8, `Trade Hub: lower token cost` for
  buy offers, `:150-161`) → `Available`.
- Research gates reference: Scented Ledger (MARKET, 160 duration, KNOWLEDGE 18 + RESIN 8), Treaty Sigils (DIPLOMACY_SHRINE, needs
  Scented Ledger, KNOWLEDGE 26 + FUNGUS 8 + RESIN 8), Fungus Symbiosis, Venom Drills, Mandible Plating, Resin Masonry
  (`sim/ResearchNode.java:9-15`; research itself is another agent's scope). Lang: `formic_frontier.research.scented_ledger` "Scented
  Ledger" / "Пахучая книга", detail "Record exchange routes in durable pheromone trails." / "Хранит торговые пути в стойких феромонных
  следах."; `treaty_sigils` "Treaty Sigils" / "Знаки договора", detail "Shape colony scents into trusted diplomatic marks." /
  "Превращает запахи колонии в знаки доверия." (lang:146-149). Neither description mentions the actual effects (trade visibility +
  15% discount; −25% tokens for ENVOY/TRUCE).

### 2.5 Execution flow — [Implemented]

Entry points: tablet Trade tab card click (only sent if `available`, `client/screen/ColonyStatusScreen.java:253-258`) → C2S
`TradeRequestPayload` → `ColonyService.trade` (`registry/ModNetworking.java:27-29`); `/formic trade <offer>` (`command/FormicCommands.java:60-62,151-157`).

`ColonyService.trade` (`world/ColonyService.java:138-152`): colony = **nearest colony of any faction within 96 blocks** (rival and wild
colonies trade too; no MARKET building is required). Then `ColonyTradeCatalog.execute` (`sim/ColonyTradeCatalog.java:58-82`):
unknown → `Unknown trade: <id>`; rep < min → `This colony does not trust you enough yet.`; not available → status text; count input
items across the whole inventory, fail with `Need N <localized item name>`; remove items (creative players **do** pay here, unlike
diplomacy/contracts); give output (dropped if inventory full); add colony resource; add reputation; current task
`Traded with <player>: <id>`; event `Trade: <player> used <id>`; result `Trade complete: <id>`. The tablet re-opens on Trade with the
result message.

UI presentation (`client/screen/ColonyStatusScreen.java:569-668,1051-1068`): two columns at width ≥ 360 — `sell_*` under
"Colony requests" (`ui.trade.colony_buys`), everything else under "Colony wares" (`ui.trade.colony_offers`); single column "Exchange
offers" when narrow. Sort: `sell_wheat` first, `buy_colony_seal` second, available Trade-Hub-affected offers, other available, then
unavailable; ties by id. A banner shows the latest recurring caravan line (`tradeActivity`, e.g. "Caravan: 8 Food -> 8 Resin with #7",
`net/ColonyUiSnapshot.java:461-477`) or the hover hint; on hover it shows `ui.trade.tooltip_exchange` + status + click hint.

Trade UI strings (lang:150-160):

| key | EN | RU |
|---|---|---|
| formic_frontier.ui.trade.colony_buys | Colony requests | Запросы колонии |
| formic_frontier.ui.trade.colony_offers | Colony wares | Товары колонии |
| formic_frontier.ui.trade.exchange | Exchange offers | Предложения обмена |
| formic_frontier.ui.trade.available | Available | Доступно |
| formic_frontier.ui.trade.locked | Unavailable | Недоступно |
| formic_frontier.ui.trade.action | Trade | Обмен |
| formic_frontier.ui.trade.scroll_hint | Scroll for more offers | Прокрутите список |
| formic_frontier.ui.trade.hover_hint | Hover an offer for the colony's current terms. | Наведите на обмен, чтобы увидеть условия колонии. |
| formic_frontier.ui.trade.tooltip_exchange | %s %s for %s %s | %s %s за %s %s |
| formic_frontier.ui.trade.click | Click the card to complete this trade | Нажмите на карточку, чтобы совершить обмен |
| formic_frontier.ui.trade.unavailable | The colony cannot accept this trade yet | Колония пока не может принять этот обмен |
| formic_frontier.ui.tab.trade / trade_short | Trade / Trade (short unused) | Торговля / Обмен |

---

## 3. Colony-to-colony caravans

There is **no travelling caravan**. Three separate things carry the name, none of which moves over the terrain or can be
intercepted: (a) an instant resource transfer every second, (b) a recurring event that instantly swaps resources and leaves a static
camp with two standing ants, (c) the tribute-pact camp (1.5). Spawned carrier ants are not scripted to walk anywhere; afterwards the
generic ant AI takes over (another agent's scope).

### 3.1 Economy-pass caravan (`TradeCaravan.exchange`) — [Implemented] (it is really a one-way gift)

`sim/TradeCaravan.java`, called for every colony every pass with its **nearest colony that it does not view as hostile**
(`world/ColonySavedState.java:133-159`; the partner needs no market and may be wild or rival-faction).

| Rule | Value | Source |
|---|---|---|
| Requires | source has a completed MARKET | `:41-43` |
| Relation rate (source's view) | ALLY 1.0, NEUTRAL 0.5, RIVAL/WAR 0 (refuse) | `:75-81` |
| Cargo | first of FOOD, ORE, CHITIN, RESIN, FUNGUS, VENOM (KNOWLEDGE excluded) where source > **40** (`SURPLUS_THRESHOLD`) and target < **20** (`SCARCITY_THRESHOLD`) | `:18-22,88-95,114-123` |
| Amount | `min(round(8 × rate), source − 40)` → 8 (ally) or 4 (neutral) per second, never taking the source below 40 (`BASE_CARGO = 8`) | `:101-108` |
| Effects | source −N, target +N, **source reputation +1**, source task `Caravan shipped N <res> to colony #T`, events `Trade caravan: sent N <res> to colony #T (<relation>, rate R)` / `Trade caravan: received N <res> from colony #S` | `:60-66` |

Nothing is paid back; it is a scarcity top-up that stops once the target reaches 20. One shipment per source per second, always to
the single nearest partner. The events it writes every second flood the 10-entry event log (see 7.1).

### 3.2 Recurring "trade caravan" event — see 7.4 (swaps 8-for-8 between mutual allies with markets, every ≥ 3 min).

### 3.3 Tribute caravan — see 1.5 (visual only).

---

## 4. Contracts ("Needs" / requests)

### 4.1 Model — [Implemented]

- A contract is a view over an open `ColonyRequest(building, resource, needed, fulfilled, reason)` (`sim/ColonyRequest.java:6-31`;
  `needed ≥ 1`, `fulfilled` clamped to `needed`). Requests are opened with `ColonyLogistics.requestResource`, which ignores duplicates
  (same building+resource+reason, still open) and logs `Request opened: N <res> for <building>` (`sim/ColonyLogistics.java:18-29`).
- `ColonyContract.from(request)` (`sim/ColonyContract.java:17-34`):
  - `missing` = needed − fulfilled (also exposed as `resourceCost`).
  - `priority` from the reason prefix (`:71-98`): `famine` 5, `invasion` 5, `treaty` 4, `expansion` 4, `repair` 4, `research` 3,
    building QUEEN_CHAMBER or BARRACKS 3, `construction` 2, anything else 1. (`migration` is not listed → 1.)
  - `rewardTokens = max(1, ceil(missing/8) + priority)`.
  - `reputationDelta = max(1, min(6, ceil(missing/16) + priority/2))` (integer division).
  - id = `<building_id>:<resource_id>:<reason lowercased, non-alphanumerics→_, max 24 chars>:<hex of reason.hashCode()>` (`:36-43`).
- Partial delivery rewards: tokens `max(1, min(rewardTokens, ceil(delivered/8) + priority))`, rep
  `max(1, min(reputationDelta, ceil(delivered/16) + priority/2))` (`:57-69`). Because rewards are recomputed from the *remaining*
  amount, splitting a delivery changes the total reward.
- `ColonyLogistics.fulfillContract` (`sim/ColonyLogistics.java:50-83`): delivered = min(missing, amount); request updated or removed
  when complete; **colony gains the delivered resource**; colony rep += reward; task `Contract fulfilled: …` / `Contract helped: …`;
  event `Contract: delivered N <res> for <building> (+T token, +R rep)`; research contracts start the research on completion
  (`applyCompletedContractPayoff`, `:202-218`, event `Contract payoff: Research started: <Label>`).
- Colony self-supply vs player supply: each economy pass the colony auto-fills **one** open request from its own stores, up to
  `max(1, workers) + 3 (if RESIN_DEPOT) + culture workerBias` units — except requests whose reason starts with `construction `,
  `research `, `repair `, `famine`, `migration`, `invasion`, `treaty`, `expansion`, which only the player can fill
  (`sim/ColonyLogistics.java:124-158,190-200`).
- **No timers, no expiry, no penalties** for ignoring or "breaking" a contract anywhere in the code.

### 4.2 Contract sources

| Source | Building / resource | Amount | Reason string | Priority | Source |
|---|---|---|---|---|---|
| Queued building lacks resources | building / each missing resource | shortfall | `construction <building_id>` | 2 (3 for barracks/queen chamber) | `world/ColonyBuilder.java:409-415` |
| Research lacks resources | research building / each missing resource | shortfall | `research <node_id>` | 3 | `sim/ColonyLogistics.java:104-111` |
| Damaged building, not enough chitin | building / CHITIN | repairCost − chitin, repairCost = max(4, chitinCost/2 + 2×level) | `repair <building_id>` | 4 | `world/ColonyBuilder.java:138-160` |
| Famine warning event | FOOD_STORE / FOOD | max(36, threshold − food) | `famine food stores` | 5 | `world/ColonyRecurringEvents.java:215,309-311` |
| Migration event | MARKET / FOOD | 42 | `migration trail supplies` | 1 | `:202` |
| Invasion warning event | BARRACKS / CHITIN | 32 | `invasion defense supplies` | 5 | `:174` |
| Treaty opportunity event | DIPLOMACY_SHRINE / RESIN | 18 | `treaty envoy supplies` | 4 | `:158` |
| Expansion opportunity event | WATCH_POST / ORE | 30 | `expansion outpost materials` | 4 | `:99` |

### 4.3 Delivery options (`ContractDeliveryOption`) — [Partial]

`sim/ContractDeliveryOption.java:4-14` (the item actually taken is chosen by a second, duplicated switch in
`world/ColonyService.java:535-545`; both agree):

| Resource | Item | Items per bundle | Resource per bundle | Resource per item |
|---|---|---|---|---|
| FOOD | Wheat | 8 | 12 | 1.5 |
| ORE | Raw Iron | 4 | 8 | 2 |
| CHITIN | Chitin Shard | 2 | 6 | 3 |
| RESIN | Resin Glob | 2 | 6 | 3 |
| FUNGUS | Fungus Culture | 2 | 6 | 3 |
| VENOM | Venom Sac | 1 | 5 | 5 |
| KNOWLEDGE | Pheromone Dust | 4 | 6 | 1.5 |

Items needed for an amount = `max(1, ceil(amount × items / resource))` (`:20-25`). The tablet card shows **one bundle**
(`deliveredAmount = min(missing, bundle)`, `net/ColonyUiSnapshot.java:94-108`; text "`%s gives %s %s`", e.g. "8 Wheat gives 12 Food"),
but the click delivers the **entire remaining amount at once** and takes all the items (`world/ColonyService.java:168-179`, comment
explains the switch). The card therefore understates the item cost (a 92-food famine costs 62 wheat).

### 4.4 Player delivery flow (`ColonyService.completeContract`) — [Implemented]

Entry: tablet Needs card click → C2S `ContractRequestPayload` (`client/screen/ColonyStatusScreen.java:263-268`,
`registry/ModNetworking.java:24-26`). No command. Colony = nearest within **128** blocks (trade uses 96; inconsistent).
Steps (`world/ColonyService.java:154-203`): contract gone → `Contract is no longer open.`; not enough items →
`Need N <item> for this contract.` (nothing consumed); consume items (free in creative); `fulfillContract`; give reward tokens;
payoffs: completed `construction` contract → immediately runs `ColonyBuilder.tick` to start the queued building (`:551-559`);
completed expansion contract → `completeExpansionOutpost` (7.6). Feedback on the Needs tab:
`Expansion outpost secured` | `Construction started: <building>` | payoff message | `Delivered N <res>`, always followed by
` | +T tokens, +R rep`.

Card UI (`client/screen/ColonyStatusScreen.java:425-458`): title `%s for %s` (resource for building), delivery line, reward line
`+%s token, +%s rep, P%s`, "Help" pill, progress bar. The client re-sorts cards by missing amount (the server sorted by
priority, `net/ColonyUiSnapshot.java:312-326`).

Contract UI strings (lang:161,176-177,227-230): `ui.request.help_action` Help / Помочь; `ui.request.title` "%s for %s" / "%s для %s";
`ui.request.delivery` "%s gives %s %s" / "%s даёт %s %s"; `ui.request.help` "Help %s" / "Помочь %s" (not referenced by any code);
`ui.request.reward` "+%s token, +%s rep, P%s" / "+%s токен, +%s реп, П%s" (RU says
"токен" while the item is "Феромонный жетон"); `ui.no_requests` "No open requests" / "Нет открытых заявок"; `ui.no_requests_detail`
"The colony has enough supplies for its current plan." / "Колонии хватает припасов для текущего плана.".

### 4.5 Reward examples (computed with the exact formulas)

| Contract | Res | Amount | Prio | Tokens | Rep | Items handed over |
|---|---|---|---|---|---|---|
| famine (minimum) | FOOD | 36 | 5 | 10 | 5 | 24 wheat |
| famine (starter colony at 4 food; threshold 96) | FOOD | 92 | 5 | 17 | 6 | 62 wheat |
| migration | FOOD | 42 | 1 | 7 | 3 | 28 wheat |
| invasion | CHITIN | 32 | 5 | 9 | 4 | 11 chitin shards |
| treaty | RESIN | 18 | 4 | 7 | 4 | 6 resin globs |
| expansion | ORE | 30 | 4 | 8 | 4 | 15 raw iron |
| repair market (lvl 1, 0 chitin) | CHITIN | 5 | 4 | 5 | 3 | 2 chitin shards |
| construction barracks, 18 ore short | ORE | 18 | 3 | 6 | 3 | 9 raw iron |
| famine 24 (gametest fixture) | FOOD | 24 | 5 | 8 | 4 | 16 wheat |

Contracts pay roughly 2–5× more tokens per item than the equivalent sell trade (62 wheat: 17 T by contract vs 3 T by `sell_wheat`)
and give the colony twice as much resource per item.

---

## 5. Armory (`ColonyArmory`) — [Implemented] (abstract only)

`sim/ColonyArmory.java`. A pure function of colony state used only by raid math (6.2). **It produces, stores, consumes or hands out
no items at all**; no resources are spent on equipment; spawned ant entities are not visibly equipped (no renderer/entity reference).

| Constant | Value | Meaning | Source |
|---|---|---|---|
| `ARMORY_ARMS_PER_BUILDING` | 4 | combat ants armed per completed ARMORY | `:43` |
| `MANDIBLE_SABER_ATTACK` | 5 | attack per armed ant (mirrors item bonus damage 5.0) | `:46` |
| `VENOM_SPEAR_ATTACK` | 4 | attack per armed ant (mirrors item bonus 4.0) | `:48` |
| `CHITIN_ARMOR_DEFENSE` | 2 | defense per armed ant | `:51` |
| `RESIN_CHITIN_DEFENSE_BONUS` | +2 | extra defense per armed ant | `:53` |

Loadout (`:63-106`):
- `armories` = completed ARMORY count; 0 → weapon `bare_mandibles`, nothing else.
- `armedSoldiers = min(SOLDIER + MAJOR + GIANT, 4 × armories)`.
- Weapon: Mandible Plating researched → `mandible_saber`, attack = armed × 5; else Venom Drills → `venom_spear`, armed × 4; else 0
  (an armory without either research gives no attack).
- Armor (only if a CHITIN_FARM is completed): per armed ant 2, or 4 with Resin Masonry (`resinChitin = true`); requires an armory too
  (early return), but not weapon research.
- `weaponAttack` is added to `militaryStrength`; `armorDefense` is added to `defenseRating` (`world/RaidPlanner.java:430,443`).

Actual items for players (cross-link, other agent): Mandible Saber `FormicWeaponItem(5.0f, false)`, durability 420; Venom Spear
`FormicWeaponItem(4.0f, true)`, durability 360; chitin armour durability ×15, resin-chitin ×21
(`src/main/java/com/formicfrontier/registry/ModItems.java:81-90`). They are obtainable by crafting and by trade (2.2) regardless of
whether the colony owns an armory.

---

## 6. Raids (`RaidPlanner`)

### 6.1 Scheduling — who raids whom — [Implemented], but **unreachable in normal survival**

`world/RaidPlanner.java:22-46`, every world pass, only if ≥ 2 colonies exist. For each colony in save order:
1. `raidCooldown − 1`; ensure a relation entry with every other colony (1.1).
2. If cooldown == 0: target = **nearest colony this colony views as RIVAL/WAR** (`:75-81`). If a target exists **and**
   `militaryStrength ≥ 16` → execute the raid and set cooldown **600** (≈ 300–600 s). Otherwise the cooldown stays 0 and it re-checks
   every second.
- Any colony raids, including the player's own allied colony (starts with cooldown 0 and strength 16, so it attacks a newly seeded
  rival on the very next pass). There is no player control over whether "your" colony raids.
- Raid frequency per hostile colony ≈ every 5 minutes (two cooldown decrements per second).
- **Reachability:** hostile pairs exist only between allied and rival-faction colonies, and rival colonies are only created by
  `/formic colony seed-rivals`. Queen-Egg colonies and discovered wild colonies are mutually NEUTRAL, and the tablet only offers
  improving actions (ENVOY/TRIBUTE/TRUCE). **Without commands no relation can ever become hostile, so raids, invasion warnings,
  truce and war-pact consequences never happen in normal play.**

### 6.2 Strength formulas

| Quantity | Formula | Source |
|---|---|---|
| `militaryStrength` | 4×SOLDIER + 8×MAJOR + 16×GIANT + ColonyArmory.weaponAttack | `:422-431` |
| `defenseRating` | 6×completed WATCH_POST + 4×completed BARRACKS + militaryStrength/3 + ColonyArmory.armorDefense | `:433-444` |
| `defensiveSupportRating(ally)` | 0 if none; else 6 + defenseRating(ally)/2 + militaryStrength(ally)/3 | `:346-351` |
| Total defense in a raid | defenseRating(defender) + defensiveSupportRating(ally) | `:148` |

(The invasion-warning event uses a **different** strength formula without the armory bonus, `world/ColonyRecurringEvents.java:1007-1011`.)

Defensive ally (`findDefensiveAlly`, `:83-91`): the nearest third colony C where defender→C is ALLY **and** C→defender is ALLY
(mutual) and C→attacker is not ALLY.

### 6.3 Resolution (`resolveCombat` + `applyRaidOutcome`) — [Implemented]

`world/RaidPlanner.java:145-202`. All integer maths.

| Output | Formula |
|---|---|
| Target resource | defender's richest resource (ties → earliest of FOOD, ORE, CHITIN, RESIN, FUNGUS, VENOM, KNOWLEDGE; KNOWLEDGE can be stolen) (`:412-420`) |
| Stolen | `min(defender stock, max(4, stock/5 − defense/3))`; moved defender → attacker |
| Raw queen damage | `max(0, attack/30 − defense/10)` |
| Queen Vault | if the defender has a completed QUEEN_VAULT: damage `max(0, raw − 3)`; `absorbed = raw − final` (`:405-410`) |
| Casualties (each side) | `breakthrough = clamp((opposing − own)/max(1, opposing), 0, 1)`; `losses = round(breakthrough × pool / 3)`; at least 1 if breakthrough ≥ 0.5; capped at `max(1, pool/2)`; pool = SOLDIER+MAJOR+GIANT; removed soldiers first, then majors, then giants (`:215-254`). Attacker uses (defense vs attack), defender uses (attack vs defense). |
| Relation shift | defender **held** iff `defense > attack`. If not held, the **defender's** view of the attacker worsens one step (ALLY→NEUTRAL→RIVAL→WAR); if that yields WAR and a defensive ally exists, the ally's view of the attacker becomes WAR too. Attacker's view never changes (`:173-176,196-201`). |
| Queen HP | defender queen health −= damage (floor 0; max 140) |

Worked examples (verified numerically):

| Scenario | Attack | Defense | Stolen | Queen dmg | Attacker losses | Defender losses | Held |
|---|---|---|---|---|---|---|---|
| Starter vs starter (2 S + 1 M each; defender 1 barracks; 120 food) | 16 | 9 | 21 food | 0 | 0 | 0 | no → relation worsens |
| Mid-game: 8 S + 2 M + armory w/ saber vs 6 S + 2 M + 1 G, 2 watch posts, 1 barracks, armory + spear + chitin farm + resin masonry, 300 food | 68 | 56 | 42 | 0 | 0 | 1 | no |
| Queen Vault gametest: 22 S + 1 M vs stripped defender (1 barracks), vault | 96 | 4 | 23 | 3 raw → 0 | 0 | 0 | no |
| JUnit weak: 8 S vs 2 S, no buildings | 32 | 2 | 24 | 1 | 0 | 1 | no (RIVAL→WAR) |
| JUnit strong: 8 S vs 18 S + 2 watch posts | 32 | 36 | 12 | 0 | 0 | 0 | yes |

Balance notes: queen damage is negligible (a 140-hp queen needs attack ≥ 30 per point); attackers almost never take losses (needs
defense ≈ 2× attack); raid theft is the main consequence.

### 6.4 World effects of a raid (`executeRaid`) — [Implemented]

`world/RaidPlanner.java:93-133`:
1. Resolve and apply the outcome.
2. **Raid trail** from attacker to defender origin (Manhattan, ≤ 160 steps, skipping within 12 of either origin): alternating DIRT_PATH/COARSE_DIRT; every 9th step a marker alternating BLACKSTONE/RED_TERRACOTTA; every 11th step a PODZOL "shoulder" ±1 block (`:353-381`).
3. Defensive ally → `placeDefensivePactResponse` (1.5).
4. **Building damage**: the first building in the defender's list that is complete, not already damaged and not QUEEN_CHAMBER gets `disableFor(180)`, its structure is re-placed in the DAMAGED visual stage, labels resynced, event `Raid damaged <building_id>` (`:389-403`). Deterministic, not random: for starter colonies this is always the FOOD_STORE first, then NURSERY, MINE, BARRACKS…
5. Messages (below).
6. Spawns **3 attacker SOLDIER entities** at defender origin + (2..4, 0, −2) — at the origin's Y, unlike other spawns that use +1 (possible embedding in terrain; unverified) (`:130-132`). They accumulate (3 per raid).

What damage actually does (cross-link to buildings agent): a damaged building still counts as complete/functional. Next world pass
`ColonyBuilder.repairDamagedBuilding` either consumes chitin (`max(4, chitinCost/2 + 2×level)`), drops the building to 55 % progress
(now non-functional) and repairs `10 + 4×workers + culture bonus` per pass until 100 % (typically ~3 s), or, without chitin, opens a
`repair <type>` contract and waits (`world/ColonyBuilder.java:18,83-160`). Meanwhile patrols tick `disabledTicks` down (180 → 45–180 s)
and the building can become undamaged without repair, leaving the repair contract open forever. Net effect: a raid costs a little
chitin and a few seconds of downtime.

Raid messages (all hard-coded EN, `:103-129`):
- Attacker task `Raided colony #D for N <res>`; attacker event `Raid hit colony #D and stole N <res> (X losses)`; if X > 0 `Raid casualties: lost X attackers`.
- Defender task `Raid by colony #A stole N <res>; X attackers lost, Y defenders lost` or, with an ally, `Defensive pact: colony #C answered raid by colony #A; X attackers lost, Y defenders lost`, plus `; Queen Vault absorbed Q queen damage` if applicable.
- Defender events: `Raid from colony #A stole N <res> and dealt Q queen damage; Y colony casualties`; `Raid casualties: lost Y defenders`; `Raid escalated relation with colony #A to <state>`; `Queen Vault absorbed Q raid queen damage`; `Defensive pact: colony #C sent guard patrols`.
- Ally task `Defensive pact: guarding colony #D`; ally event `Defensive pact answered raid against colony #D`.

Visibility problem: the redesigned tablet never renders the event list (`snapshot.events()` is sent but unused in
`client/screen/ColonyStatusScreen.java`; there is no Events tab — "Events" maps to Relations, `:1225-1235`), and the current-task string
is overwritten every pass by the economy/job loop. So raid outcomes are visible almost only in the world (trail, damaged building,
raider ants). Lang `formic_frontier.ui.no_events` (lang:184) is unused.

### 6.5 `RaidPlan` — [Dead]

`sim/RaidPlan.java:6-17`: record (attackerId, defenderId, ticksRemaining default 200, targetResource default FOOD, amount default 0),
codec and `tickDown()`. Persisted as `raidPlans` in every colony (`sim/ColonyProgress.java:24,43`) but never created or read anywhere
— leftover of a planned "announced raid with countdown" design.

---

## 7. Recurring events (`ColonyRecurringEvents`)

`world/ColonyRecurringEvents.java` (1121 lines). Called for every colony every world pass (`world/ColonySavedState.java:185-188`).

### 7.1 Scheduler — [Partial]

`tick` (`:52-84`):
1. Skip if JVM property `formic.visualQa` is set, or the colony is **not player-allied** (rivals/wild never get events).
2. Skip while any recurring-event contract is open (famine, migration, invasion, treaty, expansion — `:353-359`). Contracts never
   expire, so **one ignored contract blocks every future recurring event of that colony forever.**
3. Global gate: colony `ageTicks ≥ (ageTicks of the newest event whose text starts with "Recurring event:") + EVENT_INTERVAL_TICKS`,
   `EVENT_INTERVAL_TICKS = 20×180 = 3600` game ticks = **3 minutes** (`:23,86-92`). First event no earlier than 3 min of colony age.
4. First eligible event in this fixed priority order fires (at most one per call): **famine → migration → trade caravan → treaty →
   expansion → queen brood → invasion**. Each branch also checks a per-type "3600 after the last event of this type" gate, which is
   always implied by the global gate (redundant code, `:376-430`).
- No randomness/chance anywhere: events are deterministic given state.
- **Bug (timer memory):** "last event time" is derived from the event log, which keeps only the newest **10** entries
  (`sim/ColonyProgress.java:196-201`). Busy colonies log several lines per second (caravan shipments, envoys, logistics, construction),
  so the "Recurring event:" line falls out within seconds and the 3-minute spacing collapses (events may re-fire as soon as their
  conditions hold and no contract is open).
- Partner-side lines (trade caravan, treaty) also start with "Recurring event:" and therefore reset the partner colony's timer.
- Overview row "Events" (`formic_frontier.ui.tab.events`, EN "Events", RU "События") shows the newest recurring-event line minus its
  prefix (`net/ColonyUiSnapshot.java:413,453-459`); the Overview shows at most 6 rows (`:450`).
- Public `trigger*` methods bypass the timers (used by QA scenes and tests) but re-check conditions.

Common helpers: markers are placed with `safeSet` (some forced with `setBlockAndUpdate`); "open spawn" = solid below and 3 air blocks
(`:891-896`); trails skip cells within 7 blocks of any building of the involved colonies (`:1097-1104`); ants face their destination
(`yawToward`, `:1112-1120`).

### 7.2 Famine warning — [Implemented] (contract only)

| Aspect | Detail | Source |
|---|---|---|
| Condition | queen alive; completed FOOD_STORE; FOOD < `max(24, 3 × upkeepPerPass)`; no open famine request | `:231-236,305-307` |
| Effect | opens FOOD_STORE / FOOD contract of `max(36, threshold − food)`, reason `famine food stores` (priority 5). Does not consume or add food. | `:211-221,309-311` |
| Markers | "attachment trail" in front of the food store (z −5..−2: FOOD_NODE at even z, DIRT_PATH at odd; BROWN_MUSHROOM_BLOCK left and COARSE_DIRT right at z −4/−2); air-only markers FOOD_NODE (0,1,−2), BROWN_MUSHROOM_BLOCK (−1,1,−1), HAY_BLOCK (1,1,−1), RED_MUSHROOM_BLOCK (−2,1,0), OCHRE_FROGLIGHT (2,1,0), RED_TERRACOTTA (0,2,−1) | `:455-464,745-756` |
| Ants | none | |
| Messages | task `Recurring event: famine warning, food contract open`; event `Recurring event: famine warning opened food help contract` | `:216-217` |
| Payoff | generic contract reward only (food goes into stores) | |

A starter colony has upkeep 32/pass (queen 12, soldier 3, major 6, miner 2, worker/scout 1 — `sim/AntCaste.java:8-14`) → threshold
96 food, so famine is likely the most common event (and it is first in priority).

### 7.3 Migration preparation — [Flavour] + contract

| Aspect | Detail | Source |
|---|---|---|
| Condition | queen alive; rank ≥ HIVE; population ≥ **34**; completed NURSERY and MARKET; no open migration request | `:32,238-245` |
| Effect | MARKET / FOOD **42**, reason `migration trail supplies` (priority **1**) | `:33,35,198-209` |
| Markers | camp at fixed origin + (−34, 0, −30); diagonal trail from origin (COARSE_DIRT every 3rd, else DIRT_PATH; every 7th a HAY_BLOCK (14th) or ROOTED_DIRT marker); camp DIRT_PATH / HAY_BLOCK / OCHRE_FROGLIGHT stack, ROOTED_DIRT N, PACKED_MUD S, COARSE_DIRT E, DIRT_PATH W; 3 cleared scout posts (±2,0,0), (0,0,−2) | `:466-481,698-719,912-914,958-960` |
| Ants | 3 PATROLLING SCOUTs at the posts (if open) | `:793-804` |
| Messages | task `Recurring event: migration preparation, scout trail marked`; event `Recurring event: migration preparation marked a daughter-nest trail` | `:203-204` |
| Payoff | none beyond the contract. **No daughter colony is ever founded; population is not reduced.** | |

### 7.4 Trade caravan (recurring) — [Implemented]

| Aspect | Detail | Source |
|---|---|---|
| Partner | nearest colony with mutual ALLY views | `:986-993` |
| Condition | both queens alive; colony player-allied; mutual ALLY; **both** have a completed MARKET; colony has ≥ export amount, partner ≥ import amount | `:285-303` |
| Goods | export = colony culture's good (AMBER FOOD, LEAFCUTTER FUNGUS, FIRE VENOM, CARPENTER RESIN); import = partner's culture good, or if equal to the export a fallback (FOOD→RESIN, RESIN→CHITIN, FUNGUS→FOOD, VENOM→ORE) | `:1057-1074` |
| Amounts | 8 each (VENOM 4) (`TRADE_CARAVAN_MIN_AMOUNT = 8`) | `:47,1076-1078` |
| Effect | instant swap: colony −export/+import, partner +export/−import. No reputation. | `:124-150` |
| Markers | camp at midpoint + perpendicular×11: 5×5 diamond (DIRT_PATH inner, COARSE_DIRT outer), air cleared; BARREL centre, export block (−1,1,0), import block (1,1,0), OCHRE_FROGLIGHT (0,1,1), HAY_BLOCK (0,1,−1). Goods blocks: FOOD hay, ORE iron ore, CHITIN chitin node, RESIN honey block, FUNGUS brown mushroom block, VENOM slime block, KNOWLEDGE amethyst. Trails origin→camp→partner (≤ 120 steps each; skip within 8 of ends; DIRT_PATH, PODZOL every 3rd; marker every 10th: HONEYCOMB (20th) or HAY_BLOCK) | `:569-618,1040-1051,1080-1090` |
| Ants | colony WORKER carrying the export at camp (−2,1,−1), partner WORKER carrying the import at (2,1,1) | `:824-843` |
| Messages | colony task `Recurring event: trade caravan with colony #P`; partner task `Trade caravan exchanged <import> with colony #C`; events `Recurring event: trade caravan exchanged 8 food for 8 resin with colony #P` (and mirrored on the partner) | `:139-144` |

### 7.5 Treaty opportunity — [Flavour] + contract

| Aspect | Detail | Source |
|---|---|---|
| Candidate | colony views it NEUTRAL or ALLY and it does not view the colony as hostile; NEUTRAL preferred over ALLY, then nearest | `:969-984,995-997` |
| Condition | queen alive; player-allied; rank ≥ BURROW; completed DIPLOMACY_SHRINE; relation NEUTRAL/ALLY; target not hostile; no open treaty request | `:247-261` |
| Effect | DIPLOMACY_SHRINE / RESIN **18**, reason `treaty envoy supplies` (priority 4) | `:40-42,152-167` |
| Markers | camp at midpoint + perpendicular×8: DIRT_PATH, forced HONEYCOMB / AMETHYST / CANDLE stack; MOSS N, ROOTED_DIRT S, PACKED_MUD E, DIRT_PATH W; envoy posts (±2,1,0) cleared; trails origin→camp→target (DIRT_PATH, MOSS every 3rd; marker every 9th: AMETHYST (18th) / HONEYCOMB) | `:543-567,620-644,1027-1038,1053-1055` |
| Ants | colony SCOUT CARRYING_RESIN (west post) and target WORKER CARRYING_FUNGUS (east post), both at a shared Y, platform forced | `:806-822,845-862` |
| Messages | task `Recurring event: treaty opportunity, envoy supplies needed for colony #T`; events `Recurring event: treaty opportunity opened envoy route to colony #T` / `… received envoy route from colony #C` | `:159-161` |
| Payoff | **none**: completing the contract changes no relation (no special case in `completeContract`). The autonomous envoy (1.6) has the same preconditions and would ally a NEUTRAL candidate within a second anyway. | |

### 7.6 Expansion opportunity (+ outpost completion) — [Partial]

| Aspect | Detail | Source |
|---|---|---|
| Condition | queen alive; player-allied; rank ≥ HIVE; completed MARKET and WATCH_POST; no completed expansion outpost (a completed WATCH_POST within 6 of the first or current outpost site); no open expansion request | `:275-283,361-374` |
| Outpost site | origin + (edge, 0, **12**) with `edge = clamp(claimRadius + 4, 34, 42)`, claimRadius = max(rank claim radius, stored claim radius) — always to the +X (east); ground adjusted by scanning ≤ 8 blocks down for path-like blocks | `:916-948` |
| Effect | WATCH_POST / ORE **30**, reason `expansion outpost materials` (priority 4) | `:43-45,94-106` |
| Markers | trail from origin (diagonal steps; skip within 10 of origin; COARSE_DIRT every 3rd else DIRT_PATH; every 8th marker IRON_ORE (16th) / ROOTED_DIRT); 9×9 area cleared 3 up with a DIRT_PATH ring and ROOTED_DIRT corners; centre DIRT_PATH + WATCH_POST block + COBBLED_DEEPSLATE_WALL + OCHRE_FROGLIGHT stack; supply piles (−2,*,−5) IRON_ORE, (0,0,−5) COARSE_DIRT, (2,*,−5) CHITIN_NODE, (0,1,5) BONE_BLOCK; OAK_FENCE at (±4,1,0), (0,1,±4) | `:483-512,721-743` |
| Ants | MINER CARRYING_ORE (−2,1,−5), WORKER CARRYING_CHITIN (2,1,−5), SCOUT PATROLLING (0,1,5), facing the origin | `:768-791` |
| Messages | task `Recurring event: expansion opportunity, outpost materials needed`; event `Recurring event: expansion opportunity marked claim-edge outpost at x, y, z` | `:100-101` |
| Completion (contract fully delivered) | adds a completed WATCH_POST at the outpost unless one is within 4; claim radius = `min(48, max(current, max(|dx|,|dz|) + 4))` (e.g. 40 → 44); task `Expansion outpost secured; claim radius now N`; event `Expansion complete: claim-edge watch post secured at x, y, z`; removes the temporary stack and places the real WATCH_POST structure; corner markers ROOTED_DIRT + OCHRE_FROGLIGHT at (±5,0,±5), DIRT_PATH + HONEYCOMB at (±6,0,0)/(0,0,±6); spawns the 3-ant crew again | `:108-122,514-541,950-956`; triggered from `world/ColonyService.java:189-191` |

Effectively a **one-time** event per colony. **Bug:** `ColonyBuilder.tick` overwrites the claim radius every world pass with
`max(18 + 6 × queen chamber level, rank claim radius)` (`world/ColonyBuilder.java:48`), so the +claim reward is reverted one second
later (the gametest calls `completeExpansionOutpost` directly and never ticks the builder afterwards). The claim radius is
display-only anyway (tablet "Claim %s" / "Радиус %s", lang:164; status text) apart from locating this outpost. The lasting reward is
the extra WATCH_POST (+6 defense, +10 rank score). The method `spawnExpansionAnt` contains four stray blank lines (`:776-779`).

### 7.7 Queen brood bloom — [Implemented]

| Aspect | Detail | Source |
|---|---|---|
| Condition | queen alive; population < **42**; FOOD ≥ 45 + 4 × upkeep; CHITIN ≥ 12 + 16 = 28; completed NURSERY | `:25-27,223-229` |
| Effect | **FOOD −45, CHITIN −12, WORKER caste +2** (the only event with a direct stat effect) | `:183-196` |
| Markers | attachment trail in front of the nursery (CHITIN_NODE / DIRT_PATH, HONEYCOMB + COARSE_DIRT sides); air-only markers CHITIN_NODE (0,1,−2), HONEYCOMB (−1,1,−1), BONE_BLOCK (1,1,−1), HONEYCOMB (−2,1,0), CHITIN_NODE (2,1,0), OCHRE_FROGLIGHT (0,2,−1) | `:432-447` |
| Ants | 2 WORKER entities at the first open of three candidate spots each | `:449-453,758-766` |
| Messages | task `Recurring event: queen brood bloom`; event `Recurring event: queen brood bloom raised new workers` | `:190-191` |

### 7.8 Invasion warning — [Flavour] + contract; unreachable without commands

| Aspect | Detail | Source |
|---|---|---|
| Threat | nearest colony that views this colony as hostile, whose `raidCooldown` is in 1..**240** (`INVASION_WARNING_WINDOW_TICKS = 20×12`, i.e. 120–240 s before its next raid, not 12 s as the constant suggests) and whose base strength (no armory) ≥ 16. It may end up raiding someone else. | `:36,962-967,999-1011` |
| Condition | queen alive; completed BARRACKS; threat exists; no open invasion request | `:263-273` |
| Effect | BARRACKS / CHITIN **32**, reason `invasion defense supplies` (priority 5) | `:37-39,169-181` |
| Markers | rally = origin + 36 along the dominant axis toward the threat (at origin Y) — this lands 2 blocks from the first starter building on that axis (FOOD_STORE +38 E, NURSERY −38 W, MINE +38 S, BARRACKS −38 N; `world/ColonyBuilder.java` siteFor), so visual overlap is likely; trail from origin (skip within 10; DIRT_PATH/COARSE_DIRT; every 8th BLACKSTONE (16th) / RED_TERRACOTTA); rally DIRT_PATH / RED_TERRACOTTA / BLACKSTONE / CANDLE, BONE_BLOCK and CHITIN_NODE on one side, POLISHED_DEEPSLATE and ROOTED_DIRT on the other; PODZOL + RED_TERRACOTTA "direction proof" 3 blocks further toward the threat | `:646-696,1013-1025` |
| Ants | 3 PATROLLING SOLDIERs at the rally (visual only; they do not count in defense maths) | `:864-889` |
| Messages | task `Recurring event: invasion warning, defense contract open`; event `Recurring event: invasion warning marked threat from colony #T` | `:175-176` |
| Payoff | contract reward + 32 chitin. No defense bonus, no effect on the coming raid. | |

### 7.9 Event summary

| Event | Min rank | Buildings needed | Other triggers | Contract | Direct effect | Priority order |
|---|---|---|---|---|---|---|
| Famine warning | – | FOOD_STORE | food < max(24, 3×upkeep) | FOOD 36+ @ FOOD_STORE, P5 | – | 1 |
| Migration preparation | HIVE | NURSERY, MARKET | pop ≥ 34 | FOOD 42 @ MARKET, P1 | – | 2 |
| Trade caravan | – | MARKET (both) | mutual ALLY partner | – | swap 8/8 (venom 4) | 3 |
| Treaty opportunity | BURROW | DIPLOMACY_SHRINE | neutral/ally, non-hostile neighbour | RESIN 18 @ SHRINE, P4 | – | 4 |
| Expansion opportunity | HIVE | MARKET, WATCH_POST | once per colony | ORE 30 @ WATCH_POST, P4 | on completion: +WATCH_POST, claim (reverted) | 5 |
| Queen brood bloom | – | NURSERY | pop < 42, food ≥ 45+4×upkeep, chitin ≥ 28 | – | −45 food, −12 chitin, +2 workers | 6 |
| Invasion warning | – | BARRACKS | hostile neighbour 1..240 cooldown, strength ≥ 16 | CHITIN 32 @ BARRACKS, P5 | – | 7 |

Only player-allied colonies; ≥ 3 min apart (nominally); blocked by any open event contract; no randomness; no tribute-demand event.

---

## 8. Player-facing text

Everything in this scope that the server produces (trade results, availability text, contract feedback, diplomacy feedback, every
event/task/raid line, diplomacy action labels, rank names in the diplomacy list, caravan activity line) is **hard-coded English**
(`Component.literal` or plain strings) and not translatable. Only static UI labels are localized.

Relevant lang keys not already tabulated above (lang line → EN / RU):

| key | EN | RU |
|---|---|---|
| item.formic_frontier.pheromone_token (34) | Pheromone Token | Феромонный жетон |
| item.formic_frontier.pheromone_dust (35) | Pheromone Dust | Феромонная пыль |
| item.formic_frontier.colony_seal (36) | Colony Seal | Печать колонии |
| item.formic_frontier.war_banner (37) | War Banner | Боевой стяг |
| item.formic_frontier.chitin_spore (33) | Chitin Spore | Хитиновая спора |
| item.formic_frontier.raw_biomass (38) | Raw Biomass | Сырая биомасса |
| item.formic_frontier.royal_jelly (39) | Royal Jelly | Маточное желе |
| item.formic_frontier.chitin_shard / resin_glob / fungus_culture / venom_sac (23/26/27/30) | Chitin Shard / Resin Glob / Fungus Culture / Venom Sac | Осколок хитина / Комок смолы / Грибная культура / Ядовитый мешок |
| item.formic_frontier.queen_egg (21) | Queen Egg | Яйцо матки |
| item.formic_frontier.chitin_{helmet,chestplate,leggings,boots} (40-43) | Chitin Helmet / Chestplate / Leggings / Boots | Хитиновый шлем / Хитиновая кираса / Хитиновые поножи / Хитиновые ботинки |
| item.formic_frontier.resin_chitin_{…} (44-47) | Resin Chitin Helmet / Chestplate / Leggings / Boots | Смоляно-хитиновый шлем / кираса / поножи / ботинки |
| item.formic_frontier.mandible_saber / venom_spear (48/49) | Mandible Saber / Venom Spear | Мандибулярная сабля / Ядовитое копье |
| block.formic_frontier.market_chamber (9) | Market Chamber | Рынок |
| block.formic_frontier.diplomacy_shrine (10) | Diplomacy Shrine | Святилище дипломатии |
| block.formic_frontier.watch_post (11) | Watch Post | Сторожевой пост |
| block.formic_frontier.armory (16) | Armory | Оружейная |
| block.formic_frontier.barracks_chamber (8) | Barracks Chamber | Казармы |
| formic_frontier.building.trade_hub / queen_vault (58/57) | Trade Hub / Queen Vault | Торговый узел / Хранилище матки |
| formic_frontier.ui.reputation (163) | Rep %s | Реп. %s |
| formic_frontier.ui.relationship (169) | Relationship | Отношение |
| formic_frontier.ui.identity_rep (226) | %s \| rep %s | %s \| Реп. %s |
| formic_frontier.ui.top_need (172) | Top need | Дефицит |
| formic_frontier.ui.no_relations (183) | No known colonies | Нет известных колоний |
| formic_frontier.ui.tab.relations (121) | Relations | Отношения |
| formic_frontier.ui.tab.requests / needs_short (110/111) | Requests / Needs | Заявки / Нужно |
| formic_frontier.guide.helping (+ .detail) (204-205) | Helping — "Donate supplies; trade; set instincts." | Помощь — "Несите припасы; торгуйте; меняйте инстинкты." |

Unused keys in this scope: `formic_frontier.ui.tab.diplomacy` (120, "Diplomacy"/"Дипломатия"), `ui.tab.relations_short` (122),
`ui.tab.trade_short` (115), `ui.no_target` (181), `ui.no_events` (184), `ui.request.help` (229).

---

## 9. What the tests assert (in scope)

Gametests (`GT`):

| Test | Asserts |
|---|---|
| rivalColoniesUseStageFourCultures (`GT:513-526`) | a rival colony is never AMBER and starts with resin/fungus/venom > 0 |
| rivalRaidLeavesVisibleTrailAndDamagedTarget (`GT:528-567`) | `RaidPlanner.tick` returns true for a rival (cooldown 0, 4 S + 1 M) vs an allied colony with no soldiers; DIRT_PATH/COARSE_DIRT trail plus RED_TERRACOTTA/BLACKSTONE markers between them; one allied building is damaged; `diplomacy_scene` exists |
| queenVaultAbsorbsRaidQueenDamage (`GT:569-605`) | with a completed QUEEN_VAULT and 22 S + 1 M attackers, queen HP is unchanged; task and event contain "Queen Vault absorbed" |
| tributeDiplomacyPlacesVisiblePactMarkers (`GT:607-651`) | `DiplomacyConsequences.apply(TRIBUTE, ALLY)` returns true; beacons at +12/+52, cache blocks at the cache site, caravan barrel/hay/honeycomb at (32,−18); a resin carrier of the source and a fungus carrier of the target nearby |
| alliedMarketTradeCaravanRunsOutsideTributeDiplomacy (`GT:653-725`) | AMBER + CARPENTER mutual allies with markets: recurring tick swaps exactly 8 food for 8 resin on both sides; events on both; Trade-tab activity "8 Food -> 8 Resin" with "#id"; camp blocks; two carriers; no repeat on the next tick |
| truceDiplomacyCoolsRaidRouteWithVisibleMarkers (`GT:727-760`) | `apply(TRUCE, NEUTRAL)`: both cooldowns ≥ 1800; seals and cache blocks; `RaidPlanner.tick` returns false |
| warPactDiplomacyPlacesVisibleMusterRoute (`GT:762-799`) | `apply(WAR_PACT, WAR)`: both cooldowns 0; musters at (12,−14)/(52,−14), mid-muster (32,−14); ≥ 4 patrolling source ants |
| alliedDefensivePactSendsVisibleGuardResponse (`GT:801-842`) | raid on an allied colony with a mutual ally: guard post at (−28,0,8); ≥ 3 patrolling ally ants; both tasks mention "Defensive pact" |
| damagedBuildingConsumesChitinAndRestoresMarketBlueprint (`GT:1185-1217`) | damaged market → REPAIRING after one builder tick with chitin, COMPLETE within 4 more |
| repairContractDeliveryStartsDamagedBuildingRepair (`GT:1219-1258`) | no chitin → stays DAMAGED and opens a CHITIN contract "repair market"; full delivery completes it; next tick REPAIRING, chitin 0, requests empty |
| constructionContractDeliveryStartsMinimalQueuedSite (`GT:1260-1300`) | missing ore opens "construction market" ORE contract; delivery completes it and adds ore; next tick starts a PLANNED market and clears queue/requests |
| queenBroodRecurringEventLeavesNurseryProof (`GT:1371-1409`) | +2 workers, food and chitin decrease, task/event text, nursery markers, no refire on the next tick |
| famineRecurringEventMarksFoodStoreAndOpensContract (`GT:1411-1450`) | at 4 food: famine task/event, FOOD_STORE/FOOD contract with the famine reason, food unchanged, markers, no duplicate |
| migrationRecurringEventMarksDaughterNestTrail (`GT:1452-1507`) | with market + 26 extra workers: task/event, MARKET/FOOD contract, trail and camp blocks, exactly 3 patrolling scouts, no duplicate |
| invasionWarningRecurringEventMarksApproachAndOpensDefenseContract (`GT:1509-1564`) | rival cooldown 120 + 4 S + 1 M: task/event, BARRACKS/CHITIN contract, rally blocks at (36,0,0), exactly 3 guard soldiers, no duplicate |
| treatyOpportunityRecurringEventMarksEnvoyRouteAndOpensResinContract (`GT:1566-1631`) | with shrine and a neutral wild neighbour: events on both colonies, SHRINE/RESIN contract with priority ≥ 4, camp blocks, exactly 2 envoys (allied resin scout + neutral fungus worker), no duplicate |
| expansionOpportunity…OpensOreContract (`GT:1633-1700`) | with market + watch post: task/event, WATCH_POST/ORE contract priority ≥ 4, outpost stack blocks, trail, supply piles, exactly 3 crew ants, no duplicate |
| completedExpansionOutpostContractSecuresClaimEdgeWatchPost (`GT:1702-1770`) | after full delivery + `completeExpansionOutpost`: completed WATCH_POST registered at the outpost, claim radius > old and ≥ 44, task/event, requests empty, no reopening after another interval (does not tick the builder, so the claim-overwrite bug is not caught) |
| completedTradeHubImprovesTradeTerms (`GT:1925-1950`) | Trade Hub + rep 20: `sell_wheat` pays +1, `buy_colony_seal` costs 14 (from 16), status text contains "Trade Hub" |
| tabletTradeSceneShowsTradeHubTerms (`GT:1952-1979`) | snapshot shows sell_wheat 2 tokens, seal 14 and available, status "Trade Hub"; tradeActivity parses a caravan event into "8 Food -> 8 Resin with #7" |
| tabletControlsChangeColonyState (`GT:2066-2144`) | via a mock player: `completeContract` on a 24-food famine contract with 64 wheat closes the request, raises reputation and grants tokens |

JUnit (`UT`): `diplomacyActionsMoveRelationsWithCostsAndRankGates` (`UT:324-333`: ENVOY WAR→RIVAL, TRIBUTE NEUTRAL→ALLY, TRUCE WAR/RIVAL→NEUTRAL,
WAR_PACT ALLY→WAR, truce costs more than envoy, war pact needs CITADEL); `politicsRelationsShiftFromActionsChangeTradeRate`
(`UT:956-1009`: `DiplomacyService.perform` TRIBUTE NEUTRAL→ALLY raises caravan rate, INCITE ALLY→NEUTRAL lowers it, OUTPOST actor rejected);
`tradeCaravanExchangesResourcesByRelationAndScarcity` (`UT:750-834`: ally ships and conserves resources at rate 1.0, neutral ships less at
0.5, rival refuses, no surplus → nothing); `requestsExposePlayerContractsWithRewardsAndReputation`, `famineRequestsDoNotDrainEmergencyFoodStores`,
`famineContractsRestoreFoodWhenPlayerHelps` (priority ≥ 5; 12 delivered → food 16), `completedResearchContractStartsResearchWithDeliveredMaterials`,
`contractRowsExposeDeliveryCostAndUrgentOrder` (resin research row first; bundle 2 globs/6 resin, 8 wheat/12 food) (`UT:231-252,347-427`);
`soldierWeaponLoadoutChangesCombatStats`, `venomSpearAndArmorAdvanceCombatLoadout` (`UT:1011-1125`); `raidOutcomeChangesColonyStateWithCasualtiesAndRelationShift`
(`UT:1126-1204`); `recurringEventsAppearInOverviewWithoutShowingFoundingNoise` (`UT:217-229`); `wildColoniesExposeDiscoverableRelationship` (`UT:254-264`).

Never exercised by any test: player trade execution (`ColonyTradeCatalog.execute` / `ColonyService.trade`), player diplomacy
(`ColonyService.performDiplomacy`) and Treaty Sigils discount, the live autonomous envoy pass (`DiplomacyService.tick`), reputation
price tiers 25/60 and the Scented Ledger discount, `areHostile`/entity combat, the 10-entry log interaction with event timers.

---

## 10. Bugs, inconsistencies and magic numbers (consolidated)

1. **Raids/war unreachable in survival**: no natural hostile colonies, INCITE/WAR_PACT hidden from the tablet (loops `i < 3`, `client/screen/ColonyStatusScreen.java:718,901`).
2. **Scented Ledger discount overwritten** by reputation tiers (assignment instead of multiply), `sim/ColonyTradeCatalog.java:92-98` (duplicated in `sim/ColonyTrades.java:200-206`).
3. **Autonomous envoy every second, free, one-sided, +3 rep per step**; `ENVOY_PASS_INTERVAL_TICKS` unused with a wrong comment (`sim/DiplomacyService.java:24-25,66-82`).
4. **Recurring-event timer stored in a 10-entry event log** → spacing collapses on busy colonies (`world/ColonyRecurringEvents.java:86-92`, `sim/ColonyProgress.java:196-201`).
5. **Open event contracts never expire** and block all recurring events (`:56-58,353-359`); no penalties for ignoring contracts; lingering `repair` contracts when patrols clear the damage first.
6. **Expansion claim radius reverted** next pass by `world/ColonyBuilder.java:48`.
7. Contract card shows a one-bundle cost but the click charges the full remaining amount (`net/ColonyUiSnapshot.java:94-108` vs `world/ColonyService.java:168-179`).
8. Rep-gated trade offers are flagged "available" (minReputation missing from `isAvailable`, `sim/ColonyTradeCatalog.java:126-131`).
9. Diplomacy UI/message costs ignore the Treaty Sigils discount (`net/ColonyUiSnapshot.java:162`, `world/ColonyService.java:615-620`).
10. Symmetric (player) vs one-sided (autonomous, raid escalation) relation updates; relation views can silently diverge.
11. Player diplomacy has no cooldown or no-op check (repeat TRIBUTE on an ally = rep farm + ant spawns); actor is simply the nearest colony of any faction.
12. Truce cooldown is per colony, so it blocks raids against third parties too; war pact resets both colonies' cooldowns.
13. Time constants named `*_TICKS` are really raid-cooldown units decremented 1–2 per second: `TRUCE_COOLDOWN_TICKS = 20*90` (900–1800 s), `INVASION_WARNING_WINDOW_TICKS = 20*12` (120–240 s), raid cooldown 600 (300–600 s); double decrement from `CasteJobLoop.patrol` and `RaidPlanner.tick`.
14. Two different `militaryStrength` formulas (armory-aware in raids, armory-blind in invasion warnings).
15. `migration` reason missing from `ColonyContract.priorityFor` → priority 1 although it is an event contract (`sim/ColonyContract.java:71-98`).
16. Dead code: `ColonyTrades` (full duplicate) + `ColonyService.tabletText`; `RaidPlan` (persisted, never used); per-type event timers (redundant); fallback message branches in `DiplomacyConsequences` for actions that never produce consequences.
17. Radii differ per action: trade 96, contracts/diplomacy/research 128, priority 96 (`world/ColonyService.java:141,157,248,216`).
18. Creative players pay for trades but not for diplomacy or contracts (`sim/ColonyTradeCatalog.java:163-187` vs `world/ColonyService.java:499-501`).
19. Caravan shipments and envoys raise "player reputation" (`sim/TradeCaravan.java:62`, `sim/DiplomacyService.java:52`).
20. Magic geometry: defensive rally `14·raid + (4, 8)·ally` asymmetric; offsets 8/10/11/14/18/36/(−34,−30)/(34..42, 12); invasion rally 2 blocks from a starter building; expansion always east.
21. Raiders spawn at the defender origin Y without +1 (`world/RaidPlanner.java:131`); spawned ants from events/pacts/raids accumulate and are never cleaned up; world markers are permanent and `safeSet` may cut through logs/stone.
22. Event log is sent to the client but never rendered; current-task messages are overwritten within a second → most raid/diplomacy/event feedback is invisible in the UI.
23. All gameplay messages are English literals; RU uses "токен" vs "жетон" for the same item.
24. Relation list order (and default diplomacy target) comes from `Map.copyOf`, i.e. unspecified order.

---

## Design intent vs. reality

Sources of intent: `docs/content-intent/formic-content-intent.md:54-77`, `docs/content-intent/content-feature-matrix.template.json:55-113`,
`docs/roadmap.md:320-416`, `docs/mvp-architecture.md:36-42`, `docs/manual-playtest.md:19-29`, `README.md:27-37`.

| Intent (docs) | Reality (code) |
|---|---|
| "Markets and caravans exchange resources with neighbor colonies at rates that depend on scarcity and relations." | Implemented literally but thinly: the per-second "caravan" is a one-way top-up (surplus > 40 → scarce < 20, 8/4 units by relation); the recurring caravan is an instant 8-for-8 culture swap. Nothing travels, nothing can be intercepted, no prices between colonies. |
| "Player-facing requests/contracts: the colony asks for resources and rewards delivery (reputation, resources, unlocks)." | Implemented and the most solid loop: priority, token + reputation rewards, construction/research/expansion payoffs. Missing: deadlines, failure, "broken contract" consequences. |
| "Relations shift from actions: tribute, raids, broken contracts, shared enemies." | Tribute: yes (player). Raids: yes (failed defense worsens the defender's view). Shared enemies: partially (defensive ally joins at WAR). Broken contracts: not implemented. |
| "Treaties, alliances, and rivalries have mechanical effects (trade rates, joint defense, raid risk)." | Trade rate: yes (caravan 1.0/0.5/0). Joint defense: yes (defensive pact support rating). Raid risk: only via hostility and truce/war-pact cooldown changes. Rivalry does not change player trade prices (those depend on reputation only). |
| "Recurring diplomatic events (treaty opportunities, tribute demands) drive the world without the player." | Treaty opportunity exists but has no diplomatic outcome; tribute demands do not exist. Events run only for player-allied colonies. The autonomous envoy is the only AI diplomacy, and it degenerates into instant one-sided alliances. |
| Roadmap 7.1: "famine, invasion, queen brood, migration, treaty, and expansion events … produce visible world or UI consequences." | All six (plus trade caravan) exist with markers, ants and contracts. Mechanically only brood (+2 workers), caravan (swap) and expansion (+watch post) do anything; migration founds no daughter nest; invasion does not affect the raid; treaty makes no treaty. Scheduling is deterministic, fixed-priority, and fragile (10-line log). |
| Roadmap 7.2: "alliances, tribute, defensive pacts, and colony wars have visible consequences … diplomacy changes what the player sees and can do." | Visible consequences are thorough (routes, caches, beacons, patrols). But war content (incite, war pact, raids) is command-only, and the event log that would narrate it is not shown. |
| MVP notes: "Diplomacy is intentionally expensive. Envoys are early and modest; tribute needs rank and a crafted seal; truce is Hive-gated; war pacts are Citadel-gated." | Matches the cost/rank table, but the colony does the same for free every second once it has a shrine, and TRIBUTE dominates TRUCE. |
| "Ant-themed weapons … with real combat stats; soldier castes use them in raids/defense." | Weapons/armour exist as player items; colony soldiers "use" them only as numbers derived from armory + research (+5/+4 attack, +2/+4 defense per armed ant, 4 per armory). No production, consumption or visible equipment. |
| "Defensive structures and raid mechanics: attackers, walls, casualties, outcomes that change colony state." | Casualties, theft, queen damage, relation escalation, building damage and repair are implemented; watch posts/barracks/vault contribute. Balance makes raids mild (tiny queen damage, attackers rarely die). Walls do not exist. `RaidPlan` (announced raids) was never built. |
| Manual playtest: "Right-click the market chamber or use the Colony Tablet; confirm the Trade tab…"; "`/formic diplomacy incite` … `war_pact`" | True — the playtest itself relies on commands for incite/war pact and on `seed-rivals` for raids, confirming these were never wired into survival play. |

---

## Worth keeping for a rebuild

- **The 4-state relation ladder** (ALLY/NEUTRAL/RIVAL/WAR with improve/worsen) and the action table (envoy/tribute/truce/incite/
  war pact with token/dust/seal/banner costs, reputation deltas and rank gates) — clear, testable, easy to present. Fix: decide on
  one relation model (symmetric pair, or explicit directed views shown in UI), add cooldowns/no-op checks, make truce distinct from
  tribute, and give the AI diplomacy a real cadence and cost.
- **Reputation as player trust with price tiers** (<0 ×1.25, 25 ×0.9, 60 ×0.75; +1 token at 50) and min-rep gates on premium goods
  (seal 20, resin armour 15/20, saber 20, queen egg 40) — good progression feel; make modifiers multiplicative and keep AI actions
  out of it.
- **The trade catalog** (9 sell / 18 buy offers, research and culture gates, Trade Hub and Scented Ledger bonuses) as a starting
  price sheet — then tie it to colony stock, markets and relations.
- **Contracts** — priority from reason, `tokens = ceil(missing/8) + priority`, `rep = min(6, ceil(missing/16) + priority/2)`, per-
  resource delivery items, and payoffs (construction start, research start, expansion outpost). Add deadlines/expiry, failure
  consequences ("broken contract" relation hits) and show the true item cost.
- **Raid resolution core** (`resolveCombat`: theft `max(4, stock/5 − def/3)`, breakthrough-based casualties capped at half the pool,
  failed defense escalates relation, Queen Vault absorption, defensive-ally support) — deterministic and unit-testable; retune queen
  damage and attacker losses, and make hostility reachable in survival (natural rival spawns, border incidents, tribute demands).
- **Armory as a derived loadout** (armed ants per armory, best available weapon, armour tier) — keep the idea but consider real
  equipment production/consumption and visible gear on soldiers.
- **Event ideas and their world staging**: famine (help contract), brood bloom (+workers), invasion warning (rally + guards), treaty
  camp with paired envoys, market caravan camps with carriers, expansion outpost that becomes a real watch post, war-pact musters,
  truce seals, tribute caches, defensive-pact guard posts, raid trails with damaged buildings. Rebuild the scheduler on explicit
  persisted timestamps (not the event log), add randomness/weights, expiry for event contracts, and give migration/treaty/invasion
  real outcomes (daughter nest, treaty/relation change, defense bonus).
- **Presentation lessons**: surface the event log in the UI, localize gameplay messages, and avoid two sources of truth
  (duplicate catalogs, duplicated item mappings, two strength formulas).
