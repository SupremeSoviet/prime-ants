# 04 — Items, blocks-as-items, equipment, recipes, player UI, commands, networking, build setup

Extraction from the old "Prime Ants" project (code name **Formic Frontier**, mod id `formic_frontier`, Gradle project `formic-frontier`, package `com.formicfrontier`).

- Repo: `<original-project>`, branch `rebuild/anthills-from-scratch`, HEAD `75a70f7` ("Redesign ant castes and Formic equipment art", 2026-07-13). Working tree clean. Read-only access; nothing was built or run.
- All paths are relative to the repo root. Line numbers refer to HEAD `75a70f7`.
- Status tags: **IMPL** = implemented and reachable in normal play · **PARTIAL** = works but incomplete or inconsistent · **STUB/DEAD** = unused, unreachable or no effect.
- "Verified against MC jar" means I disassembled the Mojang-mapped 1.21.11 jar in the local Loom cache (`~/.gradle/caches/fabric-loom/minecraftMaven/...`, read-only) with `javap`, to confirm vanilla behaviour the mod relies on.
- The name "Prime Ants" appears only in the first commit (`9ed2fa7 Add Prime Ants mod project`). Every in-game string and all metadata say "Formic Frontier" (`src/main/resources/fabric.mod.json:5`).

Cross-links to other extractions: castes/resources/research → 01/02-style docs; buildings/structures; diplomacy/trade/raids/events; tests/QA. They are mentioned below but not documented in depth.

---

## 0. Quick facts

| Thing | Count / value | Source |
|---|---|---|
| Standalone items (`ModItems`) | 29 | `src/main/java/com/formicfrontier/registry/ModItems.java:62-90` |
| Blocks (`ModBlocks`) | 19 (18 get an automatic BlockItem; `chitin_bed` is placed by the `chitin_spore` item) | `src/main/java/com/formicfrontier/registry/ModBlocks.java:21-39` |
| Items in registry total | 47 | 29 + 18 |
| Entity types | 1 (`formic_frontier:ant`) | `src/main/java/com/formicfrontier/registry/ModEntities.java:15-22` |
| Armor materials / equipment assets | 2 / 2 (`chitin`, `resin_chitin`) | `ModItems.java:27-60`, `src/main/resources/assets/formic_frontier/equipment/*.json` |
| Item tags | 1 (`formic_frontier:repairs_chitin_armor`) | `src/main/resources/data/formic_frontier/tags/item/repairs_chitin_armor.json:1-8` |
| Crafting recipes | 32 (11 shapeless, 21 shaped) | `src/main/resources/data/formic_frontier/recipe/` |
| Loot tables / advancements / block tags / `minecraft:` tag additions | **0 / 0 / 0 / 0** | `src/main/resources/data/` only contains `recipe/` and `tags/item/` |
| Network payloads | 6 (1 server→client, 5 client→server) | `src/main/java/com/formicfrontier/registry/ModNetworking.java:17-39` |
| Commands | 1 root (`/formic`), 13 executable leaves, **no permission checks** | `src/main/java/com/formicfrontier/command/FormicCommands.java:24-74` |
| Lang keys | 229 in `en_us`, 229 in `ru_ru`, identical key sets | `src/main/resources/assets/formic_frontier/lang/` |
| Minecraft / Loader / Fabric API / Loom / Gradle / Java | 1.21.11 / 0.19.2 / 0.141.3+1.21.11 / 1.16.1 / 9.4.1 / release 21 (CI builds on JDK 25) | `gradle.properties:4-7`, `gradle/wrapper/gradle-wrapper.properties:3`, `build.gradle:55,72-73`, `.github/workflows/build.yaml:22-25` |

---

## 1. Registration and init flow

- **Main initializer** `src/main/java/com/formicfrontier/FormicFrontier.java:27-50`, in this order: `ModBlocks.initialize()` → `ModItems.initialize()` → `ModEntities.initialize()` → `ModNetworking.initialize()` → `FormicCommands.initialize()`. It also registers an `END_SERVER_TICK` hook. Every `ColonyEconomy.ECONOMY_TICK_INTERVAL` = **20 ticks** (`src/main/java/com/formicfrontier/sim/ColonyEconomy.java:4`), the hook runs `ColonySavedState.tickEconomy()` for all colonies and then `tickWorld(overworld)`. **Only the Overworld is ever world-ticked** (`FormicFrontier.java:43`). It logs "Formic Frontier initialized".
- **Items** are registered as a side effect of static field initialisation, through `ModItems.register(name, factory, props)`. That helper sets the item `ResourceKey`, which 1.21.2+ requires (`ModItems.java:95-99`).
- **Blocks** go through `ModBlocks.register(name, factory, props, shouldRegisterItem)` (`ModBlocks.java:44-55`). Their BlockItems use `useBlockDescriptionPrefix()`, so the item name comes from the `block.formic_frontier.*` key.
- **Creative tabs**: the mod only uses vanilla tabs; there is no custom tab.
  - `BUILDING_BLOCKS`: the 18 block items (`ModBlocks.java:57-78`).
  - `SPAWN_EGGS`: `queen_egg`, even though it is not a spawn egg (`ModItems.java:102`).
  - `TOOLS_AND_UTILITIES`: `colony_tablet` (`ModItems.java:103`).
  - `INGREDIENTS`: chitin_shard, chitin_fiber, chitin_plate, resin_glob, fungus_culture, leaf_mash, aphid_honeydew, venom_sac, mandible_plate, royal_wax, chitin_spore, pheromone_token, pheromone_dust, colony_seal, war_banner, raw_biomass, royal_jelly (`ModItems.java:104-122`).
  - `COMBAT`: the 8 armor pieces plus the saber and the spear (`ModItems.java:123-134`).
- **Entity** `formic_frontier:ant`: `MobCategory.CREATURE`, sized with the WORKER caste values (≈0.93 × 1.5), `clientTrackingRange(10)` (`ModEntities.java:15-22`).
  - Default attributes are the worker's values: max health 18, speed 0.28, attack 2.0, follow range 24 (`src/main/java/com/formicfrontier/entity/AntEntity.java:59-65`). Per-caste stats are applied at runtime; see the castes extraction.
  - There is no spawn egg and no natural spawning.
- **Client initializer** `src/client/java/com/formicfrontier/client/FormicFrontierClient.java:15-25` does four things:
  - registers the ant model layers;
  - registers `AntEntityRenderer`;
  - calls `VisualQaClient.initialize()`. That QA screenshot harness does nothing unless `-Dformic.visualQa=true` (`src/client/java/com/formicfrontier/client/VisualQaClient.java:113-118`);
  - registers the S2C receiver that opens the colony screen.

---

## 2. Master catalogue: every item, block and entity (with EN/RU names)

`ru_ru.json` is line-aligned with `en_us.json` (same key on the same line), so one line number serves both files: `src/main/resources/assets/formic_frontier/lang/{en_us,ru_ru}.json:<line>`.

### 2.1 Standalone items (29)

Abbreviations: **Stack** = max stack size; **Tab** = creative tab (Ing = Ingredients, Cmb = Combat, T&U = Tools & Utilities, Egg = Spawn Eggs); **tok** = Pheromone Token.

| # | Registry id | EN name | RU name | Class / stack | Tab | How obtained | What it is used for | Status | Sources (reg / lang) |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `queen_egg` | Queen Egg | Яйцо матки | `QueenEggItem`, 16 | Egg | No recipe. Trade `buy_queen_egg`: 64 tok, needs colony reputation ≥ 40 | Use on any block to found a new **player-allied** colony (§3.1) | IMPL | `ModItems.java:62` / `:21` |
| 2 | `colony_tablet` | Colony Tablet | Планшет колонии | `ColonyTabletItem`, 1 | T&U | **Unobtainable in survival**: no recipe, trade, loot or drop | Right-click opens the colony UI of the **first colony ever created** (§3.2) | PARTIAL | `ModItems.java:63` / `:22` |
| 3 | `chitin_shard` | Chitin Shard | Осколок хитина | Item, 64 | Ing | Harvest a mature Chitin Bed (§3.3). No recipe | Recipes (fiber, spore, dust). Contract delivery for CHITIN. `sell_chitin`. Hand-feed ants (+3 chitin). Armor repair. UI icon for chitin | IMPL | `ModItems.java:64` / `:23` |
| 4 | `chitin_fiber` | Chitin Fiber | Хитиновое волокно | Item, 64 | Ing | Recipe: 3 shards → 2 | Intermediate for chitin_plate (8), royal_jelly and watch_post | IMPL | `ModItems.java:65` / `:24` |
| 5 | `chitin_plate` | Chitin Plate | Хитиновая пластина | Item, 64 | Ing | Recipe: 8 fiber + 1 resin → 1 | Chitin armor, colony_seal, diplomacy_shrine, mandible_plate, mandible_saber, armory, war_banner. Armor repair | IMPL | `ModItems.java:66` / `:25` |
| 6 | `resin_glob` | Resin Glob | Комок смолы | Item, 64 | Ing | Recipe (honeycomb + slime ball + dust → 2). Trade `buy_resin_glob` (3 tok) | Many recipes. Contract delivery for RESIN. `sell_resin`. Armor repair. UI icon for resin | IMPL | `ModItems.java:67` / `:26` |
| 7 | `fungus_culture` | Fungus Culture | Грибная культура | Item, 64 | Ing | Recipe (brown + red mushroom + leaf mash → 2). Trade `buy_fungus_culture` (4 tok, needs research `fungus_symbiosis`) | fungus_garden recipe. Contract delivery for FUNGUS. `sell_fungus` | IMPL | `ModItems.java:68` / `:27` |
| 8 | `leaf_mash` | Leaf Mash | Листовая масса | Item, 64 | Ing | Recipe (moss block + wheat seeds → 3) | Intermediate only: aphid_honeydew, fungus_culture, fungus_garden | IMPL | `ModItems.java:69` / `:28` |
| 9 | `aphid_honeydew` | Aphid Honeydew | Медвяная падь | Item, 16 | Ing | Recipe (honey bottle + leaf mash → 2) | **Nothing.** Not an ingredient, not food, not traded, not referenced in code | STUB/DEAD | `ModItems.java:70` / `:29` |
| 10 | `venom_sac` | Venom Sac | Ядовитый мешок | Item, 16 | Ing | Recipe (spider eye + slime ball + dust → 2). Trade `buy_venom_sac` (5 tok, needs research `venom_drills`) | venom_press and venom_spear recipes. Contract delivery for VENOM. `sell_venom` | IMPL | `ModItems.java:71` / `:30` |
| 11 | `mandible_plate` | Mandible Plate | Мандибулярная пластина | Item, 64 | Ing | Recipe (3 chitin plate + resin + bone → 2) | Resin-chitin armor, saber, spear, venom_press. Armor repair | IMPL | `ModItems.java:72` / `:31` |
| 12 | `royal_wax` | Royal Wax | Королевский воск | Item, 16 | Ing | Recipe (honeycomb + royal jelly + resin → 2) | **Nothing** | STUB/DEAD | `ModItems.java:73` / `:32` |
| 13 | `chitin_spore` | Chitin Spore | Хитиновая спора | `BlockItem(chitin_bed)`, 64 | Ing | Recipe (shard + brown mushroom → 2). Trade `buy_chitin_spore` (2 tok). 45 % chance on each bed harvest | Places a Chitin Bed | IMPL | `ModItems.java:74` / `:33` |
| 14 | `pheromone_token` | Pheromone Token | Феромонный жетон | Item, 64 | Ing | **Colony currency; no recipe.** Earned from contracts and `sell_*` trades | Pays for `buy_*` trades and diplomacy actions. Colony-seal recipe | IMPL | `ModItems.java:75` / `:34` |
| 15 | `pheromone_dust` | Pheromone Dust | Феромонная пыль | Item, 64 | Ing | Recipe (honeycomb + shard → 2). Trade `buy_pheromone_dust` (4 tok) | Recipes: resin_glob, venom_sac, colony_seal, diplomacy_shrine. Diplomacy cost. Contract delivery for KNOWLEDGE. **Use on an ant** to set the colony instinct (§3.4). UI icon for knowledge | IMPL | `ModItems.java:76` / `:35` |
| 16 | `colony_seal` | Colony Seal | Печать колонии | Item, 16 | Ing | Recipe (token + 2 dust + chitin plate + paper). Trade `buy_colony_seal` (16 tok, reputation ≥ 20) | Diplomacy cost (tribute, truce, war_pact). Recipes: diplomacy_shrine (1), pheromone_archive (3) | IMPL | `ModItems.java:77` / `:36` |
| 17 | `war_banner` | War Banner | Боевой стяг | Item, 16 | Ing | Recipe (2 chitin plate + 3 red dye + black banner + stick). Trade `buy_war_banner` (14 tok) | Diplomacy cost (incite, war_pact). Not a placeable banner, just a plain item | IMPL | `ModItems.java:78` / `:37` |
| 18 | `raw_biomass` | Raw Biomass | Сырая биомасса | Item, 64 | Ing | Recipe (rotten flesh + wheat seeds → 2) | Hand-feed an ant (+8 colony food). `sell_biomass` | IMPL | `ModItems.java:79` / `:38` |
| 19 | `royal_jelly` | Royal Jelly | Маточное желе | Item, 16 | Ing | Recipe (honey bottle + sugar + resin + chitin fiber → 1) | Feed the **queen** ant (+24 HP heal, +6 reputation). royal_wax recipe. `sell_royal_jelly` (8 tok) | IMPL | `ModItems.java:80` / `:39` |
| 20 | `chitin_helmet` | Chitin Helmet | Хитиновый шлем | Armor, 1 | Cmb | Recipe (5 plates). Trade (12 tok) | Armor (§4) | IMPL | `ModItems.java:81` / `:40` |
| 21 | `chitin_chestplate` | Chitin Chestplate | Хитиновая кираса | Armor, 1 | Cmb | Recipe (8 plates). Trade (20 tok) | Armor | IMPL | `ModItems.java:82` / `:41` |
| 22 | `chitin_leggings` | Chitin Leggings | Хитиновые поножи | Armor, 1 | Cmb | Recipe (7 plates). Trade (18 tok) | Armor | IMPL | `ModItems.java:83` / `:42` |
| 23 | `chitin_boots` | Chitin Boots | Хитиновые ботинки | Armor, 1 | Cmb | Recipe (4 plates). Trade (10 tok) | Armor | IMPL | `ModItems.java:84` / `:43` |
| 24 | `resin_chitin_helmet` | Resin Chitin Helmet | Смоляно-хитиновый шлем | Armor, 1 | Cmb | Recipe (3 mandible + 2 resin). Trade (20 tok, rep ≥ 15, research `mandible_plating`) | Armor | IMPL | `ModItems.java:85` / `:44` |
| 25 | `resin_chitin_chestplate` | Resin Chitin Chestplate | Смоляно-хитиновая кираса | Armor, 1 | Cmb | Recipe (6 mandible + 2 resin). Trade (34 tok, rep ≥ 20, research) | Armor | IMPL | `ModItems.java:86` / `:45` |
| 26 | `resin_chitin_leggings` | Resin Chitin Leggings | Смоляно-хитиновые поножи | Armor, 1 | Cmb | Recipe (5 mandible + 2 resin). Trade (30 tok, rep ≥ 20, research) | Armor | IMPL | `ModItems.java:87` / `:46` |
| 27 | `resin_chitin_boots` | Resin Chitin Boots | Смоляно-хитиновые ботинки | Armor, 1 | Cmb | Recipe (2 mandible + 2 resin). Trade (18 tok, rep ≥ 15, research) | Armor | IMPL | `ModItems.java:88` / `:47` |
| 28 | `mandible_saber` | Mandible Saber | Мандибулярная сабля | `FormicWeaponItem(5.0, venom=false)`, durability 420 | Cmb | Recipe (mandible + chitin plate + stick). Trade (24 tok, rep ≥ 20, research `mandible_plating`) | Melee weapon (§4.3) | PARTIAL | `ModItems.java:89` / `:48` |
| 29 | `venom_spear` | Venom Spear | Ядовитое копье | `FormicWeaponItem(4.0, venom=true)`, durability 360 | Cmb | Recipe (venom sac + mandible + stick). Trade (22 tok, rep ≥ 15, research `venom_drills`) | Melee weapon with Poison (§4.3) | PARTIAL | `ModItems.java:90` / `:49` |

Trade references in this table: `src/main/java/com/formicfrontier/sim/ColonyTradeCatalog.java:28-54`, one line per offer in the order listed in §3.8. The token prices shown are **base** prices; see the multipliers in §3.8.

### 2.2 Blocks and block items (19)

None of the 19 blocks has a loot table (there is no `data/formic_frontier/loot_table/`), so **every one of them drops nothing when broken**. None is tagged `minecraft:mineable/*`, and none sets `requiresCorrectToolForDrops`. Strength sets hardness and blast resistance to the same value (`ModBlocks.java:21-39`).

| Registry id | EN | RU | Class | Hardness | Sound | Block item? | Recipe | Right-click opens tab | Other roles (cross-link: buildings/castes docs) | Reg / lang |
|---|---|---|---|---|---|---|---|---|---|---|
| `nest_mound` | Nest Mound | Гнездовой курган | ColonyInteractBlock | 1.4 | GRAVEL | yes | – | Overview | Mound shell material. Fallback patrol target for soldiers (`AntEntity.java:421-422`) | `ModBlocks.java:21` / `:2` |
| `nest_core` | Nest Core | Ядро гнезда | ColonyInteractBlock | 2.2 | ROOTED_DIRT | yes | – | Overview | Placed in mound structures | `:22` / `:3` |
| `colony_ledger` | Colony Ledger | Журнал колонии | ColonyInteractBlock | 1.8 | WOOD | yes | – | Overview | Placed by `StructurePlacer`. **Has no texture of its own**: its model uses the `pheromone_archive` texture | `:23` / `:4` |
| `food_chamber` | Food Chamber | Пищевая камера | ColonyInteractBlock | 1.6 | FUNGUS | yes | – | Build | Where ants deliver FOOD (`AntEntity.java:420`) | `:24` / `:5` |
| `nursery_chamber` | Nursery Chamber | Ясли | ColonyInteractBlock | 1.6 | HONEY_BLOCK | yes | – | Build | Structure marker | `:25` / `:6` |
| `mine_chamber` | Mine Chamber | Шахта | ColonyInteractBlock | 2.8 | DEEPSLATE | yes | – | Build | Where ants deliver ORE (`AntEntity.java:419`) | `:26` / `:7` |
| `barracks_chamber` | Barracks Chamber | Казармы | ColonyInteractBlock | 2.4 | NETHERITE_BLOCK | yes | – | Build | Patrol target (`AntEntity.java:421`) | `:27` / `:8` |
| `market_chamber` | Market Chamber | Рынок | ColonyInteractBlock | 1.8 | HONEY_BLOCK | yes | – | **Trade** | Structure marker | `:28` / `:9` |
| `diplomacy_shrine` | Diplomacy Shrine | Святилище дипломатии | ColonyInteractBlock | 2.5 | AMETHYST | yes | yes | **Relations** (server sends "Diplomacy") | Structure marker | `:29` / `:10` |
| `watch_post` | Watch Post | Сторожевой пост | ColonyInteractBlock | 2.2 | BONE_BLOCK | yes | yes | Build | Structure marker | `:30` / `:11` |
| `resin_depot` | Resin Depot | Склад смолы | ColonyInteractBlock | 2.0 | HONEY_BLOCK | yes | yes | Build | RESIN source marker for ants (`AntEntity.java:447`) | `:31` / `:12` |
| `pheromone_archive` | Pheromone Archive | Феромонный архив | ColonyInteractBlock | 2.3 | AMETHYST | yes | yes | **Research** | KNOWLEDGE source marker (`AntEntity.java:450`) | `:32` / `:13` |
| `fungus_garden` | Fungus Garden | Грибной сад | ColonyInteractBlock | 1.2 | FUNGUS | yes | yes | Build | FUNGUS source marker (`AntEntity.java:448`) | `:33` / `:14` |
| `venom_press` | Venom Press | Ядовитый пресс | ColonyInteractBlock | 2.6 | SLIME_BLOCK | yes | yes | Build | VENOM source marker (`AntEntity.java:449`) | `:34` / `:15` |
| `armory` | Armory | Оружейная | ColonyInteractBlock | 2.8 | NETHERITE_BLOCK | yes | yes | Build | Structure marker | `:35` / `:16` |
| `food_node` | Food Node | Источник еды | ColonyInteractBlock | 0.8 | MOSS | yes | – | Build | Resource cluster at colony origin +(54,0,8) (`src/main/java/com/formicfrontier/world/ColonyService.java:661,679`). FOOD source for ants | `:36` / `:17` |
| `ore_node` | Ore Node | Рудная жила | ColonyInteractBlock | 3.0 | STONE | yes | – | Build | Cluster at +(8,0,54) (`ColonyService.java:662,680`). ORE source | `:37` / `:18` |
| `chitin_node` | Chitin Node | Хитиновый узел | ColonyInteractBlock | 2.0 | BONE_BLOCK | yes | – | Build | Cluster at +(−54,0,8) (`ColonyService.java:663,681`). CHITIN source | `:38` / `:19` |
| `chitin_bed` | Chitin Bed | Хитиновая грядка | ChitinBedBlock (`AGE_4`, random ticks) | 0.4 | BONE_BLOCK | **no** (placed by `chitin_spore`) | via spore | – (harvest instead, §3.3) | Placed inside chitin-farm, nursery and vault structures. Its name key doubles as the UI label for the CHITIN_FARM building (`src/main/java/com/formicfrontier/network/ColonyUiSnapshot.java:291`) | `:39` / `:20` |

### 2.3 Entity and named ants

| Id / key | EN | RU | Notes | Source |
|---|---|---|---|---|
| `entity.formic_frontier.ant` | Formic Ant | Муравей Formic | Single entity type; the caste is synced data. Castes are covered in their own doc | `ModEntities.java:15-22` / lang `:51` |
| `formic_frontier.ant.queen` | Queen #%s | Матка #%s | Custom name tag given to the queen at spawn | `ColonyService.java:925-927` / `:52` |
| `formic_frontier.ant.guard` | %s Guard #%s | Страж %s #%s | Given to MAJOR/GIANT. **Bug:** the first argument is the raw caste id, so players see "major Guard #1" | `ColonyService.java:928-930` / `:53` |
| `formic_frontier.ant.worker_foreman` | Worker Foreman #%s | Старший рабочий #%s | Given to one starter worker | `ColonyService.java:908-911,920-923` / `:54` |

---

## 3. How the player interacts with colonies: item-by-item behaviour

### 3.1 Queen Egg (`src/main/java/com/formicfrontier/item/QueenEggItem.java:17-36`) — IMPL, with caveats

- `useOn` (right-click on a block), server side only. It calls `ColonyService.createColony(level, clickedPos.above())` (`:25`).
- `createColony` anchors the origin to the **surface heightmap** at that x/z (`ColonyService.java:95-102`), so the clicked y is ignored.
- The new colony is **player-allied** (`ColonyService.java:51-59`), named "Amber Burrow N" (`src/main/java/com/formicfrontier/sim/ColonyProgress.java:68-70`).
- It seeds the economy with food 120, ore 20, chitin 24, resin 24, fungus 12 (28 for Leafcutter), venom 4 (16 for Fire), knowledge 8. Starter castes: 1 queen, 3+ workers, 1+ scouts, 2 miners, 2 soldiers, 1 major (`ColonyService.java:639-653`).
- **Terrain is destroyed**: `placeNest` clears a radius-72 disc, 10 blocks high, above the origin, lays paths and places 5 complete buildings plus 3 resource clusters (`ColonyService.java:655-683, 711-722`).
- Seven ants are spawned at fixed offsets (`ColonyService.java:905-917`).
- The new colony gets a relation to every existing colony: NEUTRAL, or RIVAL when one side is allied and the other isn't. Wild colonies are always NEUTRAL (`ColonyService.java:74-89`).
- Feedback: action-bar literal "Created Formic colony #N" (English only, `:27`). The colony UI then opens on Overview with the same text as the footer toast (`:29`).
- Consumes 1 egg unless the player is in creative (`:32-34`).
- Missing checks:
  - no dimension check (colonies do not store a dimension, see §14);
  - no protection against overlapping existing colonies;
  - no cost beyond the egg itself;
  - no cooldown.

### 3.2 Colony Tablet (`src/main/java/com/formicfrontier/item/ColonyTabletItem.java:19-36`) — PARTIAL

- Right-click in the air (`use`) opens the UI of `ColonySavedState.firstColony()` (`:27-29`).
- `firstColony()` returns the first entry of a `LinkedHashMap`, i.e. **the oldest colony in the world, whoever owns it** (`src/main/java/com/formicfrontier/world/ColonySavedState.java:35,65-67`).
  - Wild colonies spawn automatically near players every 45 s, up to 6 colonies (`src/main/java/com/formicfrontier/world/ColonyDiscoveryService.java:15-38`). In a normal survival world the tablet will therefore often show a wild colony rather than the player's own.
- Every action taken inside the UI is then applied to the colony **nearest the player** (96 or 128 blocks, §11.3), not the colony shown in the UI.
- With no colonies at all it shows action-bar `formic_frontier.feedback.no_colony` ("No nearby Formic colony.", misleading wording) (`:30-33`).
- Otherwise it opens Overview with the toast `formic_frontier.feedback.open_tablet` ("Opening colony ledger."). That string is resolved **server-side** via `.getString()` (`:34`), so it is not per-client localised.
- Right-clicking a colony block *with* the tablet triggers the block's own handler first (§3.5). That path does target the correct (nearest) colony.
- There is no recipe or trade, so the tablet is creative/`/give` only. The manual playtest doc explicitly uses `/give` (`docs/manual-playtest.md:5`).
- The UI title key is "Colony Journal" and the feedback says "colony ledger"; three names for one concept.

### 3.3 Chitin Spore → Chitin Bed farming loop (`src/main/java/com/formicfrontier/block/ChitinBedBlock.java:22-68`) — IMPL

1. Place a `chitin_spore`. It is a BlockItem for `chitin_bed`, has **no placement restrictions** (any surface), and produces a full solid cube (`cube_all`).
2. **Growth:** on each random tick, if age < 4, there is a 1/3 chance to advance one stage (`:42-47`).
   - Derived estimate (not in code): at default `randomTickSpeed` 3 a given block is random-ticked about every 68 s, so maturity takes roughly 4 × 3 × 68 s ≈ 14 min.
   - All five `age` states share one model (`src/main/resources/assets/formic_frontier/blockstates/chitin_bed.json`), so **growth is invisible**.
3. **Right-click before maturity:** action-bar "Chitin bed growth: N/4" (English literal, `:55-57`).
4. **Right-click at age 4:** harvest.
   - Drops 1 + random(0–2) chitin shards, **+1** if the nearest colony within 96 blocks has researched `chitin_cultivation` (`:59-62`). The bonus expression is a redundant double ternary.
   - 45 % chance of an extra `chitin_spore` (`:63-65`).
   - Resets age to 0 (`:66`); the bed stays in place.
5. Breaking the bed drops **nothing** (no loot table), so the spore is lost.
- This is the **only renewable source of chitin shards**, and shards are the root of the whole chitin/armor tree (§5.2). The colony's own structures also contain chitin beds, which players can harvest.

### 3.4 Using items on ants (`ColonyService.handleAntInteraction`, `ColonyService.java:318-381`) — IMPL

Entry point: `AntEntity.mobInteract` (`src/main/java/com/formicfrontier/entity/AntEntity.java:175-188`). The target colony is the ant's own `colonyId`, falling back to the nearest colony within 96 blocks (`ColonyService.java:321`).

**There is no alliance check**: rival and wild ants accept donations too.

| Held item | Condition | Effect | Player feedback (action bar, English literal) | Source |
|---|---|---|---|---|
| empty hand | any ant | Opens UI on Overview | toast "Inspecting <caste> ant." | `:325-328` |
| Royal Jelly | ant is QUEEN | Consumes 1 (not in creative). Ant heals 24. Colony queenHealth = max(stored, ant HP). Reputation +6. Event and current task logged | "The queen accepts the royal jelly. Reputation increased." | `:330-340` |
| Raw Biomass / Wheat | any ant | Consumes 1. FOOD +8 (biomass) or +3 (wheat). Reputation +1. Ant heals 2 | "The colony stores your food donation." | `:342-352` |
| Chitin Shard | any ant | Consumes 1. CHITIN +3. Reputation +1. Ant heals 4 | "The ant folds the chitin into colony stores." | `:354-364` |
| Pheromone Dust | any ant | Consumes 1. The caste's focus becomes the colony's **top instinct**: miner → ORE; soldier/major/giant → DEFENSE; queen → CHITIN; worker/scout → FOOD (`:630-637`) | "Pheromone instinct set: <id>" | `:366-378` |
| anything else | – | Falls through: opens UI on Overview | toast "Formic <caste> \| hp X/Y" | `AntEntity.java:181-184` |

Reputation is clamped to [−100, 100] (`ColonyProgress.java:223-225`). Because 1 wheat = +1 reputation, the rep-gated trades (queen egg at rep 40, seal at rep 20) are cheap to unlock. This is a balance issue.

### 3.5 Colony blocks as items: `ColonyInteractBlock` (`src/main/java/com/formicfrontier/block/ColonyInteractBlock.java:19-74`) — PARTIAL

- All 18 block items share this class. Right-click, with or without an item in hand, opens the UI of `nearestColony(pos, 96)` (`:32-54`).
  - The starting tab depends on the block (`initialTabFor`, `:56-73`): colony_ledger, nest_core and nest_mound → Overview; market_chamber → Trade; pheromone_archive → Research; diplomacy_shrine → "Diplomacy" (the client maps this to Relations); everything else → "Buildings" (Build).
  - If no colony is within 96 blocks, the chat shows "No Formic colony is linked to this block." (English, `:52`).
- Because `useItemOn` always opens the UI (`:37-39`), players must **sneak** to place blocks against these blocks (vanilla secondary-use behaviour).
- **Placing a crafted Armory, Fungus Garden, Resin Depot etc. does NOT add a building to the colony.**
  - Colony buildings exist only as simulation records (`ColonyBuilding`) created by the colony's autonomous build queue; see the buildings doc. There is no `onPlace` or `setPlacedBy` hook anywhere.
  - Even `NativeBlockRole`, whose Javadoc cites the "craftable/placeable" Fungus Garden block as proof of gameplay reachability, only counts completed FUNGUS_GARDEN *building records* (`src/main/java/com/formicfrontier/sim/NativeBlockRole.java:16-19,35-36,61-65`).
  - The only live effect of a player-placed block: ants search for resource/delivery blocks within 6 blocks of the expected site, or within 48 blocks as a fallback, and walk to them (`AntEntity.java:417-470`).
- All of these blocks drop nothing when broken, so a crafted block is destroyed when mined.

### 3.6 Currency and diplomacy goods

| Item | Sources | Sinks |
|---|---|---|
| Pheromone Token | Contract rewards: `ItemStack(PHEROMONE_TOKEN, rewardTokens)` (`ColonyService.java:186`). All 9 `sell_*` trades (1–8 tok, §3.8) | All 18 `buy_*` trades. Diplomacy (8–28 tok). Colony-seal recipe (1) |
| Pheromone Dust | Recipe; trade (4 tok) | Diplomacy (1–4). Recipes. Contract KNOWLEDGE. Ant instinct |
| Colony Seal | Recipe; trade (16 tok, rep ≥ 20) | Diplomacy: tribute, truce, war_pact (1 each). Shrine and archive recipes |
| War Banner | Recipe; trade (14 tok) | Diplomacy: incite, war_pact (1 each) |

Diplomacy costs are taken from the **player's inventory**; creative bypasses them. Source: `src/main/java/com/formicfrontier/sim/DiplomacyAction.java:6-10` and `ColonyService.java:265-277`. The mechanics of these actions belong to the diplomacy doc.

| Action id | UI label (English literal) | Tokens | Dust | Seals | Banners | Reputation change | Min rank | Relation effect |
|---|---|---|---|---|---|---|---|---|
| `envoy` | Send Envoy | 8 (6 with Treaty Sigils) | 1 | 0 | 0 | +3 | Outpost | improve 1 step |
| `tribute` | Pay Tribute | 16 | 1 | 1 | 0 | +8 | Burrow | improve 2 steps |
| `truce` | Broker Truce | 24 (18 with Treaty Sigils) | 2 | 1 | 0 | +6 | Hive | war/rival → neutral, else improve |
| `incite` | Incite War | 14 | 3 | 0 | 1 | −6 | Burrow | worsen |
| `war_pact` | War Pact | 28 | 4 | 1 | 1 | −10 | Citadel | set to war |

The Treaty Sigils discount is `ceil(cost × 0.75)`, minimum 1, and applies to envoy and truce only (`ColonyService.java:622-628`). The UI always shows the undiscounted cost.

### 3.7 Contract (Needs) delivery items

The colony's resource requests become **contracts** the player can fulfil with items.

- Resource ↔ item mapping, server side: `ColonyService.contractItem` (`ColonyService.java:535-545`).
- Display bundle: `src/main/java/com/formicfrontier/sim/ContractDeliveryOption.java:4-14`.

| Resource | Item | Display bundle ("N items give M resource") |
|---|---|---|
| FOOD | minecraft:wheat | 8 → 12 |
| ORE | minecraft:raw_iron | 4 → 8 |
| CHITIN | chitin_shard | 2 → 6 |
| RESIN | resin_glob | 2 → 6 |
| FUNGUS | fungus_culture | 2 → 6 |
| VENOM | venom_sac | 1 → 5 |
| KNOWLEDGE | pheromone_dust | 4 → 6 |

- **Items required** = `max(1, ceil(delivered × itemCount / resourceAmount))` (`ContractDeliveryOption.java:20-25`).
- **The server delivers the whole outstanding amount in one click** (`ColonyService.java:168-176`; the comment explains the change).
- **The UI card still shows a single capped bundle** (`ColonyUiSnapshot.java:94-95,106-108`). A unit test enshrines that display: a 24-food request shows "8 Wheat gives 12 Food" (`src/test/java/com/formicfrontier/sim/ColonyEconomyTest.java:234-257`). Clicking Help actually needs **16** wheat and delivers 24. **Bug.**
- **Reward** (`src/main/java/com/formicfrontier/sim/ColonyContract.java:17-34`):
  - tokens = `max(1, (missing+7)/8 + priority)`;
  - reputation = `clamp((missing+15)/16 + priority/2, 1, 6)`.
- **Priority** (`ColonyContract.java:71-98`), by reason prefix: famine / invasion = 5; treaty / expansion / repair = 4; research = 3; queen-chamber or barracks building = 3; construction = 2; anything else = 1.
- **Side effects of completing a contract:**
  - it may start the construction the contract was for (`ColonyService.java:187-188,551-559`);
  - it may auto-start the research the contract was for (`src/main/java/com/formicfrontier/sim/ColonyLogistics.java:202-218`);
  - it may secure an expansion outpost (`ColonyService.java:189-191`; events doc).

### 3.8 Trade catalogue: the item side (`ColonyTradeCatalog.java:27-55`)

The mechanics belong to the trade doc. Columns below: offer id, input → output, base counts, colony resource change, reputation change, minimum reputation, and research/culture gate.

- **Sells** (the colony buys from the player; shown as the "Colony requests" column in the UI):

| Offer | Player gives | Player gets | Colony resource | Rep | Gate |
|---|---|---|---|---|---|
| `sell_wheat` | 16 wheat | 1 tok | FOOD +12 | +1 | – |
| `sell_biomass` | 4 raw biomass | 1 tok | FOOD +16 | +2 | – |
| `sell_raw_iron` | 8 raw iron | 2 tok | ORE +12 | +2 | – |
| `sell_iron_ore` | 8 iron ore | 2 tok | ORE +12 | +2 | – |
| `sell_chitin` | 4 shards | 2 tok | CHITIN +8 | +2 | – |
| `sell_resin` | 3 resin | 2 tok | RESIN +8 | +2 | – |
| `sell_fungus` | 3 fungus | 2 tok | FUNGUS +9 | +2 | – |
| `sell_venom` | 2 venom sacs | 3 tok | VENOM +7 | +3 | colony culture FIRE, or research Scented Ledger |
| `sell_royal_jelly` | 1 royal jelly | 8 tok | FOOD +40 | +5 | – |

- **Buys** (the colony sells to the player; shown as "Colony wares"):

| Offer | Price | Gate |
|---|---|---|
| `buy_chitin_spore` | 2 tok | – |
| `buy_pheromone_dust` | 4 tok | – |
| `buy_resin_glob` | 3 tok | – |
| `buy_fungus_culture` | 4 tok | research `fungus_symbiosis` |
| `buy_venom_sac` | 5 tok | research `venom_drills` |
| `buy_colony_seal` | 16 tok | rep ≥ 20 |
| `buy_war_banner` | 14 tok | – |
| `buy_chitin_boots` / `_helmet` / `_leggings` / `_chestplate` | 10 / 12 / 18 / 20 tok | – |
| `buy_resin_chitin_boots` / `_helmet` / `_leggings` / `_chestplate` | 18 / 20 / 30 / 34 tok | rep ≥ 15 / 15 / 20 / 20, plus research `mandible_plating` |
| `buy_mandible_saber` | 24 tok | rep ≥ 20, research `mandible_plating` |
| `buy_venom_spear` | 22 tok | rep ≥ 15, research `venom_drills` |
| `buy_queen_egg` | 64 tok | rep ≥ 40 |

- **Token price multipliers** apply only when the input is the token (`ColonyTradeCatalog.java:84-103`):
  - Scented Ledger ×0.85;
  - then reputation **overwrites** the multiplier: ≥ 60 → 0.75, ≥ 25 → 0.9, < 0 → 1.25. **Bug:** this assignment discards the Scented Ledger discount;
  - Trade Hub ×0.85.
- **Token rewards** (`:105-114`): +1 at rep ≥ 50 and +1 with a Trade Hub, only for outputs under 8 tokens.
- A **duplicate, dead copy** of this whole catalogue exists in `src/main/java/com/formicfrontier/sim/ColonyTrades.java:15-43`. Its only caller is `ColonyService.tabletText` (`ColonyService.java:391-401`), which is never called: the leftover text-only tablet. STUB/DEAD.

### 3.9 Items with no use, or unobtainable in survival

| Item | Problem |
|---|---|
| `aphid_honeydew`, `royal_wax` | Craftable but used by nothing (§2.1 #9, #12) |
| `colony_tablet` | No survival source at all |
| `queen_egg` | Only source is a 64-token, rep-40 trade with an existing colony. A fresh survival player's only colonies are auto-spawned **wild** colonies; whether founding your own colony that way is the intended progression is **unclear** (the Guide's first line assumes you already have an egg) |
| `pheromone_token` | By design only from colony interactions (OK) |
| 11 of the 18 colony block items | No recipe (all nodes, chambers, ledger, core, mound): creative only |
| All 19 blocks | Drop nothing when mined |

---

## 4. Equipment

### 4.1 Armor materials (`ModItems.java:29-60`)

The `ArmorMaterial` record signature was verified against the MC jar: `(durability, defense map, enchantmentValue, equipSound, toughness, knockbackResistance, repairIngredient tag, assetId)`.

| Material | Base durability | Defense H / C / L / B (BODY) | Enchantability | Toughness | Knockback resist | Equip sound | Repair tag | Equipment asset |
|---|---|---|---|---|---|---|---|---|
| Chitin | 15 | 2 / 5 / 4 / 2 (BODY 5, unused) | 18 | 0.0 | 0.0 | `ARMOR_EQUIP_TURTLE` | `formic_frontier:repairs_chitin_armor` | `formic_frontier:chitin` |
| Resin Chitin | 21 | 3 / 6 / 5 / 3 (BODY 6, unused) | 15 | 1.0 | 0.0 | `ARMOR_EQUIP_TURTLE` | same tag | `formic_frontier:resin_chitin` |

Vanilla comparison, for context: chainmail totals 12 armor points, iron totals 15 (durability 15), diamond totals 20 with toughness 2. Chitin totals **13** with iron's durability; Resin Chitin totals **17** with toughness 1.

### 4.2 Per-piece stats

Durability = base × `ArmorType` unit (helmet 11, chestplate 16, leggings 15, boots 13, body 16; verified against the MC jar in the `ArmorType` static initialiser and `getDurability`).

| Item | Slot | Armor | Toughness | Durability |
|---|---|---|---|---|
| chitin_helmet | head | 2 | 0 | 165 |
| chitin_chestplate | chest | 5 | 0 | 240 |
| chitin_leggings | legs | 4 | 0 | 225 |
| chitin_boots | feet | 2 | 0 | 195 |
| resin_chitin_helmet | head | 3 | 1 | 231 |
| resin_chitin_chestplate | chest | 6 | 1 | 336 |
| resin_chitin_leggings | legs | 5 | 1 | 315 |
| resin_chitin_boots | feet | 3 | 1 | 273 |

- Every piece is built with `new Item.Properties().durability(ArmorType.X.getDurability(N)).humanoidArmor(material, type)` (`ModItems.java:81-88`).
  - `humanoidArmor` already sets durability, attributes, enchantability, the `EQUIPPABLE` component and `repairable` (verified against the MC jar: `Item$Properties.humanoidArmor`). The explicit `.durability(...)` call is **redundant**, though it produces the same value.
- **Repair** (anvil): chitin_shard, chitin_plate, mandible_plate or resin_glob, for **both** tiers (`src/main/resources/data/formic_frontier/tags/item/repairs_chitin_armor.json:1-8`). A single shard can repair the top-tier armor, which is a balance smell.
- **Rendering**: 1.21.4+ equipment assets.
  - `src/main/resources/assets/formic_frontier/equipment/chitin.json` and `resin_chitin.json` define only the `humanoid` and `humanoid_leggings` layers.
  - Textures are at `src/main/resources/assets/formic_frontier/textures/entity/equipment/{humanoid,humanoid_leggings}/{chitin,resin_chitin}.png` (128×64, double the vanilla density).
  - No wolf/horse body layer exists, although the BODY defense values are set.

### 4.3 Weapons (`src/main/java/com/formicfrontier/item/FormicWeaponItem.java:9-28`) — PARTIAL

| Item | Bonus damage | Venom | Durability | Attribute modifiers | Enchantable | Tags |
|---|---|---|---|---|---|---|
| mandible_saber | 5.0 | no | 420 | **none** | no | not in `#minecraft:swords` |
| venom_spear | 4.0 | Poison I (amplifier 0) for 80 ticks | 360 | **none** | no | not in `#minecraft:spears` (vanilla 1.21.11 has its own spears) |

What actually happens in combat (vanilla internals verified against the MC jar):

1. The items have no `ATTRIBUTE_MODIFIERS`, so the swing is a bare-hand attack: player base attack damage 1.0, default attack speed 4.0. The tooltip shows no damage or speed lines.
2. `Player.attack` applies the main hit (`hurtOrSimulate`) and afterwards calls `itemAttackInteraction` → `ItemStack.hurtEnemy` → `FormicWeaponItem.hurtEnemy` (verified call order in the `Player` bytecode).
   - `hurtEnemy` calls `target.hurt(damageSources().mobAttack(attacker), bonus)` (`FormicWeaponItem.java:21-23`). This second hit lands inside the target's invulnerability window.
   - `LivingEntity.hurtServer` applies only `amount − lastHurt` while `invulnerableTime > 10` (verified).
   - Net effect: about **5 damage per hit for the saber and 4 for the spear**, the maximum of the two hits rather than their sum. The spear also adds Poison for 4 s, about 3 more damage.
   - The bonus is not scaled by crits, Strength or enchantments, and it uses the damage source `mobAttack` rather than `playerAttack`.
3. **Durability never decreases.** In 1.21.5+ `ItemStack.postHurtEnemy` damages the item only when a `DataComponents.WEAPON` component is present (verified), and these items have none. Mining damage likewise needs a `TOOL` component. Durability 420/360 is cosmetic; there is also no repair ingredient.
4. **Not enchantable.** Vanilla enchantments use `supported_items` tags such as `#minecraft:enchantable/sharp_weapon` → `melee_weapon`/`swords` and `enchantable/durability`. The mod adds nothing to any `minecraft:` tag (verified tag contents in the jar).

### 4.4 What the armor cannot do

- **Not enchantable** at the table or anvil (no Protection, Unbreaking or Mending). Protection's `supported_items` is `#minecraft:enchantable/armor`, which expands to `#minecraft:head_armor` etc. Those tags list only vanilla items (verified). The enchantability values 18/15 are therefore dead.
- **Not trimmable**: `#minecraft:trimmable_armor` is built from the same vanilla lists (verified).
- Otherwise it behaves as normal armor: equippable, defense and toughness attributes, anvil repair.

### 4.5 Colony-side "virtual" equipment (cross-link: raids doc)

`src/main/java/com/formicfrontier/sim/ColonyArmory.java:3-60` mirrors the item stats as plain numbers in raid math. Nothing physical is involved:

- saber attack 5 (unlocked by `mandible_plating`);
- spear attack 4 (`venom_drills`);
- chitin armor +2 defense (needs a completed CHITIN_FARM);
- resin chitin +2 more (unlocked by **`resin_masonry`**, whereas the player's resin-chitin trades need `mandible_plating`: inconsistent unlocks);
- 4 soldiers armed per completed ARMORY.

Ants never hold or wear the items. They appear physically only on a Visual-QA armor stand (`src/main/java/com/formicfrontier/qa/VisualQaScenes.java:795-815`).

---

## 5. Recipes (32)

All recipes live under `src/main/resources/data/formic_frontier/recipe/`.

- None has a `category` or `group` field, and there are **no recipe-unlock advancements**. In vanilla, recipes then stay out of the recipe book until crafted once; they are still craftable.
- **No recipe is gated by research or colony state.** For example, resin-chitin armor and the saber can be crafted from day one if you have the materials.

### 5.1 Full list

| # | Result (count) | Type | Pattern | Key / ingredients (totals) | File:lines |
|---|---|---|---|---|---|
| 1 | aphid_honeydew ×2 | shapeless | – | honey_bottle, leaf_mash | `aphid_honeydew.json:1-11` |
| 2 | armory ×1 | shaped | `IPI` `RCR` `IPI` | I=iron_ingot ×4, P=chitin_plate ×2, R=resin_glob ×2, C=crafting_table ×1 | `armory.json:1-18` |
| 3 | chitin_boots ×1 | shaped | `C C` `C C` | C=chitin_plate ×4 | `chitin_boots.json:1-13` |
| 4 | chitin_chestplate ×1 | shaped | `C C` `CCC` `CCC` | chitin_plate ×8 | `chitin_chestplate.json:1-14` |
| 5 | chitin_fiber ×2 | shapeless | – | chitin_shard ×3 | `chitin_fiber.json:1-12` |
| 6 | chitin_helmet ×1 | shaped | `CCC` `C C` | chitin_plate ×5 | `chitin_helmet.json:1-13` |
| 7 | chitin_leggings ×1 | shaped | `CCC` `C C` `C C` | chitin_plate ×7 | `chitin_leggings.json:1-14` |
| 8 | chitin_plate ×1 | shaped | `FFF` `FRF` `FFF` | F=chitin_fiber ×8, R=resin_glob ×1 | `chitin_plate.json:1-15` |
| 9 | chitin_spore ×2 | shapeless | – | chitin_shard, brown_mushroom | `chitin_spore.json:1-11` |
| 10 | colony_seal ×1 | shaped | ` T ` `DCD` ` P ` | T=pheromone_token ×1, D=pheromone_dust ×2, C=chitin_plate ×1, P=paper ×1 | `colony_seal.json:1-17` |
| 11 | diplomacy_shrine ×1 | shaped | `ASA` `CPC` `RDR` | A=amethyst_shard ×2, S=colony_seal ×1, C=chitin_plate ×2, P=pheromone_dust ×1, R=resin_glob ×2, D=rooted_dirt ×1 | `diplomacy_shrine.json:1-19` |
| 12 | fungus_culture ×2 | shapeless | – | brown_mushroom, red_mushroom, leaf_mash | `fungus_culture.json:1-12` |
| 13 | fungus_garden ×1 | shaped | `FLF` `LML` `FLF` | F=fungus_culture ×4, L=leaf_mash ×4, M=moss_block ×1 | `fungus_garden.json:1-17` |
| 14 | leaf_mash ×3 | shapeless | – | moss_block, wheat_seeds | `leaf_mash.json:1-11` |
| 15 | mandible_plate ×2 | shaped | ` C ` `CRC` ` B ` | C=chitin_plate ×3, R=resin_glob ×1, B=bone ×1 | `mandible_plate.json:1-17` |
| 16 | mandible_saber ×1 | shaped | `  P` ` M ` `S  ` | P=mandible_plate ×1, M=chitin_plate ×1, S=stick ×1 | `mandible_saber.json:1-17` |
| 17 | pheromone_archive ×1 | shaped | `ASA` `SBS` `ARA` | A=amethyst_shard ×4, S=colony_seal ×3, B=book ×1, R=resin_glob ×1 | `pheromone_archive.json:1-18` |
| 18 | pheromone_dust ×2 | shapeless | – | honeycomb, chitin_shard | `pheromone_dust.json:1-11` |
| 19 | raw_biomass ×2 | shapeless | – | rotten_flesh, wheat_seeds | `raw_biomass.json:1-11` |
| 20 | resin_chitin_boots ×1 | shaped | `R R` `P P` | R=resin_glob ×2, P=mandible_plate ×2 | `resin_chitin_boots.json:1-15` |
| 21 | resin_chitin_chestplate ×1 | shaped | `P P` `RPR` `PPP` | P=mandible_plate ×6, R=resin_glob ×2 | `resin_chitin_chestplate.json:1-16` |
| 22 | resin_chitin_helmet ×1 | shaped | `PPP` `R R` | P=mandible_plate ×3, R=resin_glob ×2 | `resin_chitin_helmet.json:1-15` |
| 23 | resin_chitin_leggings ×1 | shaped | `PPP` `R R` `P P` | P=mandible_plate ×5, R=resin_glob ×2 | `resin_chitin_leggings.json:1-16` |
| 24 | resin_depot ×1 | shaped | `RRR` `RCR` `MMM` | R=resin_glob ×5, C=chest ×1, M=packed_mud ×3 | `resin_depot.json:1-17` |
| 25 | resin_glob ×2 | shapeless | – | honeycomb, slime_ball, pheromone_dust | `resin_glob.json:1-12` |
| 26 | royal_jelly ×1 | shapeless | – | honey_bottle, sugar, resin_glob, chitin_fiber | `royal_jelly.json:1-12` |
| 27 | royal_wax ×2 | shapeless | – | honeycomb, royal_jelly, resin_glob | `royal_wax.json:1-12` |
| 28 | venom_press ×1 | shaped | ` V ` `RPR` ` M ` | V=venom_sac ×1, R=resin_glob ×2, P=piston ×1, M=mandible_plate ×1 | `venom_press.json:1-18` |
| 29 | venom_sac ×2 | shapeless | – | spider_eye, slime_ball, pheromone_dust | `venom_sac.json:1-12` |
| 30 | venom_spear ×1 | shaped | `  V` ` M ` `S  ` | V=venom_sac ×1, M=mandible_plate ×1, S=stick ×1 | `venom_spear.json:1-17` |
| 31 | war_banner ×1 | shaped | `CRC` `RBR` ` S ` | C=chitin_plate ×2, R=red_dye ×3, B=black_banner ×1, S=stick ×1 | `war_banner.json:1-17` |
| 32 | watch_post ×1 | shaped | ` B ` `BCB` ` R ` | B=bone_block ×3, C=chitin_fiber ×1, R=rooted_dirt ×1 | `watch_post.json:1-16` |

The chitin armor shapes copy vanilla armor shapes. The resin-chitin shapes are custom mixes of plates and resin.

### 5.2 Derived material costs

These figures are computed from the recipes; they are not stated anywhere in the code.

- 1 chitin_plate = 8 fiber + 1 resin = **12 shards** + 1 resin.
- 1 mandible_plate = 1.5 chitin plates + 0.5 resin + 0.5 bone ≈ **18 shards**.
- Full chitin set = 24 plates = **288 shards** + 24 resin.
- Full resin-chitin set = 16 mandible plates + 8 resin ≈ **288 shards**, about 40 resin, 8 bones.
  - Almost the same shard cost as the chitin set for much better stats (17 armor + toughness vs 13), so crafting the chitin tier is pointless.
- Saber ≈ 30 shards. Spear ≈ 18 shards + 1 venom sac.
- Colony seal by recipe ≈ 1 token + 13 shards, versus 16 tokens by trade.
- Compare the Chitin Bed yield of 1–3 (+1) shards per ~14 min cycle: crafting armor from shards is extremely grindy, which pushes players towards token trades.

### 5.3 Coverage gaps

- **Items with no recipe:** queen_egg, colony_tablet, chitin_shard, pheromone_token.
- **Blocks with no recipe:** nest_mound, nest_core, colony_ledger, food_chamber, nursery_chamber, mine_chamber, barracks_chamber, market_chamber, food_node, ore_node, chitin_node.
- **Recipes whose output has no use:** aphid_honeydew, royal_wax.
- **Recipes for blocks** (armory, diplomacy_shrine, fungus_garden, pheromone_archive, resin_depot, venom_press, watch_post): the result is only a UI terminal and ant marker (§3.5), and it is lost when broken.
- `docs/manual-playtest.md:28` says to plant spores "on dirt/rooted dirt/mud", but no placement rule exists in code.

---

## 6. Tags

| Tag | Values | Used by | Source |
|---|---|---|---|
| `formic_frontier:repairs_chitin_armor` (item) | chitin_shard, chitin_plate, mandible_plate, resin_glob | Repair ingredient of both armor materials (`ModItems.java:26,42,58`). Name key `tag.item.formic_frontier.repairs_chitin_armor` (lang `:50`) | `src/main/resources/data/formic_frontier/tags/item/repairs_chitin_armor.json:1-8` |

Missing tags that a rebuild will need:

- `minecraft:head_armor`, `chest_armor`, `leg_armor`, `foot_armor` (enchanting and trims);
- `minecraft:swords` / `minecraft:spears` or `enchantable/*`;
- `minecraft:mineable/{pickaxe,shovel,axe}` for the blocks;
- block loot tables.

---

## 7. Asset inventory

All asset paths are under `src/main/resources/assets/formic_frontier/`.

| Folder | Files | Notes |
|---|---|---|
| `items/*.json` (1.21.4+ item definitions) | 47, one per registered item | 29 point to `formic_frontier:item/<id>`. 17 block items point straight at `formic_frontier:block/<id>`. **`items/colony_ledger.json` is the odd one out**: it points to `item/colony_ledger`, whose model has parent `block/colony_ledger` |
| `models/item/*.json` | 30 | 29 are `minecraft:item/generated` with `layer0 = formic_frontier:item/<id>`. Plus `colony_ledger` (parent = block model) |
| `models/block/*.json` | 19 | All `minecraft:block/cube_all`. **`colony_ledger` uses the `block/pheromone_archive` texture** (no ledger texture exists) |
| `blockstates/*.json` | 19 | Single `""` variant, except `chitin_bed`, whose `age=0..4` all map to the same model |
| `equipment/*.json` | 2 | `chitin`, `resin_chitin`; layers `humanoid` and `humanoid_leggings` |
| `textures/item/*.png` | 29, all 32×32 | Procedurally generated (§8) |
| `textures/block/*.png` | 18, all **16×16** | Provenance unclear (§8). One per block except `colony_ledger` |
| `textures/entity/ant_*.png` | 7, 256×128 | Worker, scout, miner, soldier, major, giant, queen; all used by `src/client/java/com/formicfrontier/client/render/AntEntityRenderer.java:12-18` |
| `textures/entity/equipment/{humanoid,humanoid_leggings}/*.png` | 4, 128×64 | |
| `textures/block.zip` | 1 zip, 10,171 bytes | **Stray backup shipped in the jar** (§8) |
| `icon.png` | 64×64 | Mod icon (`src/main/resources/fabric.mod.json:12`) |
| `lang/en_us.json`, `lang/ru_ru.json` | 229 keys each | §9 |

**Orphan check:**

- No orphan textures: every PNG is referenced by a model, equipment asset or renderer.
- No missing model or texture references.
- Gaps and oddities:
  - there is no `colony_ledger` block texture;
  - the chitin bed's growth stages have no visuals;
  - block textures are still 16×16 while the roadmap requires 32×32 (`docs/roadmap.md:28-29,51-53`);
  - `block.zip` is junk inside the resources.

---

## 8. How the textures were produced, and asset validation

### 8.1 `tools/generate_formic_textures.py` (520 lines) — procedural, code-drawn pixel art

- Pure Python with **no dependencies**. It writes its own PNGs through `zlib` + `struct` (`Canvas.save`, `:104-121`), with primitives `rect`, `frame`, `ellipse`, `line` (Bresenham with thickness) and scan-line `polygon` (`:26-102`).
- **Palettes:** hard-coded 4-tone `(dark, base, mid, light)` sets.
  - Materials: `CHITIN`, `RESIN_CHITIN`, `BONE`, `PHEROMONE`, `VENOM`, `LEAF`, `ROYAL` (`:134-140`).
  - Per caste: `ANT_PALETTES` (`:124-132`).
- **Detail:** `material_region` paints a UV region at double density, with a dark frame, a base fill, deterministic pseudo-noise `(x*7 + y*11 + seed*13) % 29` and diagonal highlight lines (`:143-163`).
- **Outputs** (`main`, `:481-516`):
  - 7 ant entity textures at 256×128 (`entity_texture`, `:180-234`). Explicit UV regions for head, thorax, abdomen, petiole, legs, crest and plates; eye colour per caste (miner blue, soldier green, major/giant red, others amber); steel bands on soldier/major/giant; translucent wings.
  - 8 armor icons at 32×32, drawn shape by shape; the resin variant adds gold streaks (`armor_icon`, `:242-281`).
  - 21 resource/tool icons at 32×32, each hand-coded (`resource_icon`, `:284-450`).
  - 4 armor layer textures at 128×64 (`equipment_texture`, `:453-478`).
- It does **not** generate block textures, and neither did any earlier version of the script (`git show 9ed2fa7:` and `a74a7fa:` of this file contain no block code).
  - The 18 block PNGs arrived in commit `a74a7fa`; `nest_mound.png` changed in `cc9db89`.
  - `README.md:39-43` says an AI-generated reference sheet (`docs/imagegen-ant-colony-stage3-reference.png`) was the "art target", baked "through the local texture generator".
  - **How the block textures were actually made is unclear from the repo.** No AI images are shipped as textures.
- The generator overwrites files in `src/main/resources/...`; it is deterministic.

### 8.2 `tools/render_texture_contact_sheet.py` (36 lines)

Uses Pillow. It lays out every item texture on a 5-column sheet, with 152×78 cells, 48×48 nearest-neighbour previews, the file name and the pixel size, and saves `build/visual-qa/formic-item-textures.png` (`:12-32`). Items only.

### 8.3 `tools/validate_assets.py` (136 lines)

Wired into Gradle as `validateAssets` (an `Exec` running Python, `build.gradle:94-100`, and `check.dependsOn`) and into a CI job (`.github/workflows/build.yaml:8-16`). It checks:

1. every JSON file under `assets/` and `data/` parses (`:29-33`);
2. `en_us` and `ru_ru` have identical key sets (`:35-43`);
3. RU values contain no mojibake tokens `Ã Â Ð Ñ U+FFFD` (`:44-51`);
4. an item definition that references `formic_frontier:item/<x>` has a matching `models/item/<x>.json` (`:55-62`); block-model references are **not** checked;
5. blockstate `variants` point to existing block models (`:64-75`); `multipart` is not handled;
6. model texture references exist (`:77-85`);
7. every PNG has a valid header and is at most 512 px (`:87-94`);
8. every item texture is exactly **32×32** (`:96-106`);
9. every ant texture is exactly **256×128** (`:108-114`);
10. every armor layer is exactly **128×64** (`:116-123`).

Not checked:

- every registered item or block has an item definition, lang key and loot table;
- recipe ingredients and tag entries exist;
- orphan textures;
- block texture size;
- lang keys are actually used.

### 8.4 `src/main/resources/assets/formic_frontier/textures/block.zip`

Listed with Python `zipfile`, read-only. 19 entries: the `block/` folder plus 18 PNGs, all 16×16, timestamped 2026-04-28 10:33. It was added in commit `cc9db89`.

| Entry | Size (bytes) |
|---|---|
| armory.png | 314 |
| barracks_chamber.png | 348 |
| chitin_bed.png | 355 |
| chitin_node.png | 355 |
| diplomacy_shrine.png | 355 |
| food_chamber.png | 384 |
| food_node.png | 360 |
| fungus_garden.png | 382 |
| market_chamber.png | 354 |
| mine_chamber.png | 378 |
| nest_core.png | 365 |
| nest_mound.png | 369 |
| nursery_chamber.png | 347 |
| ore_node.png | 358 |
| pheromone_archive.png | 364 |
| resin_depot.png | 346 |
| venom_press.png | 363 |
| watch_post.png | 374 |

- By SHA-1, 17 entries are byte-identical to the current `textures/block/*.png`. Only `nest_mound.png` differs: the live file (520 B) is newer.
- So the zip is a backup snapshot, and because it sits under `src/main/resources` it **ships inside the mod jar**.

---

## 9. Localization

### 9.1 Coverage and parity

- `en_us.json` and `ru_ru.json` each have **229 keys**, identical sets, no duplicate keys, and the same line order (checked with a script).
- **Keys missing in either language: none.**
- Two keys have identical values in both files, as expected for format strings: `formic_frontier.label.line` (`%s\n%s`) and `formic_frontier.label.identity_status` (`%s | %s`).
- No key used in code is missing from the lang files. Code builds some keys dynamically from these prefixes:
  - `formic_frontier.culture.` (`ColonyUiSnapshot.java:194`);
  - `.rank.` (`:199`);
  - `.resource.` (`:309`; `src/client/java/com/formicfrontier/client/screen/ColonyStatusScreen.java:1032`);
  - `.caste.` (`:524`);
  - `.instinct.` (`:528`; `ColonyStatusScreen.java:880`);
  - `.relation.` (`:532`);
  - `.ui.status.` (`src/main/java/com/formicfrontier/sim/BuildingVisualStage.java:28`);
  - `.relationship.` (`src/main/java/com/formicfrontier/sim/ColonyIdentity.java:12`);
  - `.research.` (`ColonyStatusScreen.java:563,1011`).

### 9.2 Keys grouped by prefix

The item, block and entity names (50 keys) are in §2. The remaining 179 keys:

| Prefix | Count | Examples (EN → RU) | Where used |
|---|---|---|---|
| `tag.item.formic_frontier.*` | 1 | repairs_chitin_armor "Repairs Chitin Armor" → "Чинит хитиновую броню" | Vanilla/JEI tag display |
| `formic_frontier.ant.*` | 3 | queen "Queen #%s" → "Матка #%s" | Ant name tags (§2.3) |
| `formic_frontier.building.*` | 5 | queen_hall "Queen Hall" → "Зал матки"; trade_hub "Trade Hub" → "Торговый узел"; road "Road" → "Дорога" | Building labels (`ColonyUiSnapshot.java:285-306`) |
| `formic_frontier.culture.*` | 4 | amber "Amber" → "Янтарная"; carpenter "Carpenter" → "Древоточцы" | UI header pill |
| `formic_frontier.rank.*` | 4 | outpost "Outpost" → "Форпост"; citadel "Citadel" → "Цитадель" | Sent in the snapshot but **never rendered** |
| `formic_frontier.resource.*` | 7 | food "Food" → "Еда"; knowledge "Knowledge" → "Знания" | Resource strip, costs, requests |
| `formic_frontier.caste.*` | 7 | worker "Worker" → "Рабочие" (EN singular, RU plural) | Population chips (only the first 4 castes are ever visible) and the queen overview row |
| `formic_frontier.instinct.*` | 4 | defense "Defense" → "Оборона" | Instinct tab rows and buttons |
| `formic_frontier.relation.*` | 4 | war "War" → "Война" | Relations rows |
| `formic_frontier.relationship.*` | 6 | new_allies "New allies" → "Новые союзники"; wild "Wild colony" → "Дикая колония" | Header pill, overview, in-world labels |
| `formic_frontier.personality.*` | 8 | guarded / .detail "Guarded" / "Watches borders" → "Настороженный" / "Следит за границами" | Overview banner, world labels |
| `formic_frontier.ui.*` (general) | 27 | title "Colony Journal" → "Журнал колонии"; colony_id "Colony #%s"; identity_rep "%s \| rep %s"; priority "Priority %s" → "Приоритет %s" | UI chrome, footer, overview |
| `formic_frontier.ui.tab.*` | 18 | research "Research" → "Исследования"; research_short "Tech" → "Наука"; relations "Relations" → "Отношения" | Navigation and section headings |
| `formic_frontier.ui.research.*` | 10 | pan_hint "Drag to explore / right-click to reset" → "Тяните мышью / ПКМ — сбросить"; state.ready "Ready - click to begin" | Research map |
| `formic_frontier.ui.resource.*` | 1 | knowledge_short "Know." → "Знания" | Resource strip |
| `formic_frontier.research.*` | 14 | treaty_sigils "Treaty Sigils" → "Знаки договора"; .detail "Shape colony scents into trusted diplomatic marks." | Research nodes and inspector |
| `formic_frontier.ui.trade.*` | 11 | colony_buys "Colony requests" → "Запросы колонии"; tooltip_exchange "%s %s for %s %s" | Trade tab |
| `formic_frontier.ui.request.*` | 5 | reward "+%s token, +%s rep, P%s" → "+%s токен, +%s реп, П%s"; help_action "Help" → "Помочь" | Needs cards |
| `formic_frontier.ui.status.*` | 9 | construction "Building" → "Строится"; damaged "Damaged" → "Повреждено" | Build cards |
| `formic_frontier.guide.*` | 21 | first_steps.detail "Place Queen Egg, open tablet, watch tasks." → "Поставьте яйцо, откройте планшет, смотрите задачи." | Guide tab |
| `formic_frontier.feedback.*` | 2 | no_colony "No nearby Formic colony." → "Рядом нет муравьиной колонии." | Tablet and handlers |
| `formic_frontier.label.*` | 9 | repairing "Repairing %s%%" → "Ремонт %s%%" | In-world building labels (`src/main/java/com/formicfrontier/world/ColonyLabelService.java:64-98`; buildings doc) |

### 9.3 Unused or never-rendered keys (23)

- **Transmitted but never displayed:** `formic_frontier.rank.{outpost,burrow,hive,citadel}`. The client ignores `snapshot.rank()`, and the diplomacy rows print the English `ColonyRank.displayName()` instead (`src/main/java/com/formicfrontier/sim/ColonyRank.java:4-7`).
- **Never referenced at all** (19): `ui.close` (the close button is a literal "X"), `ui.tab.overview_short`, `ui.tab.build_short`, `ui.tab.needs_short`, `ui.tab.trade_short`, `ui.tab.instinct_short`, `ui.tab.guide_short`, `ui.tab.relations_short`, `ui.tab.diplomacy`, `ui.research.prerequisites`, `ui.research.click_start`, `ui.queen`, `ui.next_buildings`, `ui.instinct_help`, `ui.no_target`, `ui.no_events`, `ui.status.disabled`, `ui.status.building`, `ui.request.help`.
  - The seven `*_short` keys are leftovers from the previous UI generation (`git show f5e9857:.../ColonyStatusScreen.java`, `:28-37`).
- **Referenced but never visible:** `caste.major`, `caste.giant` (§10.4, population chips).

### 9.4 Hard-coded English player-facing strings (partial RU support)

Static chrome is localised. Everything the **server** generates is English, even for RU players:

- colony names: "Amber Burrow N", "<Culture Display Name> N", "... Wild Nest N" (`ColonyProgress.java:68-82`);
- current task and event log text;
- research status ("Complete", "Requires Pheromone Archive", "Needs Knowledge 4", `ColonyUiSnapshot.java:378-408`);
- building cost and detail ("Food 40 (-5)", "Needs Resin 6", "120t", `ColonyUiSnapshot.java:356-376,340`);
- overview values ("3 workers, 2 miners, 2 guards", "140 hp / alive", "Caravan: ...", `ColonyUiSnapshot.java:414-449,470-477`);
- trade status ("Available", "Requires reputation 20", "Trade Hub: +1 token", `ColonyTradeCatalog.java:133-161`);
- diplomacy labels ("Send Envoy", ...) and rank names;
- every feedback toast and action-bar message (§3);
- all command output.

On the client side: "X", "▸ #", the "T D S" cost abbreviations, "Nx > Mx", and "i / n".

### 9.5 Translation-quality notes (observations)

- RU uses "токен" in `ui.request.reward` but "жетон" for the item.
- `item.formic_frontier.royal_jelly` is rendered as "Маточное желе"; the standard Russian term is "маточное молочко".
- RU "Журнал колонии" translates both the Colony Ledger block and the UI title ("Colony Journal").
- EN "Colony requests" (the trade column) clashes with the "Requests" tab.
- RU `guide.castes.detail` says something different from the EN text.

---

## 10. The Colony Tablet UI: `ColonyStatusScreen`

File: `src/client/java/com/formicfrontier/client/screen/ColonyStatusScreen.java` (1309 lines). Line references in this section are to this file unless another file is named.

History: rebuilt several times.

- `a74a7fa` first UI;
- `c3a7aeb` "Rebuild colony tablet UI from scratch with a custom themed widget set";
- `6b1c0b3` redesign;
- `06d1005` "glass" polish.

The roadmap marks slice "R3 Colony Tablet 2.0" as complete (`docs/roadmap.md:149-172`). Its promise was "a living colony journal, not a 2000s RTS ledger": research as a map, trade as a market, requests as help cards, EN/RU layouts, no overlaps.

### 10.1 Entry points and lifecycle

- **Opening.** Every path goes through the server calling `ColonyService.openColonyScreen(player, colony, initialTab, feedback)`, which sends one `ColonyUiPayload` (`ColonyService.java:383-389`). The client handler always does `setScreen(new ColonyStatusScreen(snapshot))` (`FormicFrontierClient.java:20-24`), replacing whatever screen is open. Callers:
  - Queen Egg (Overview);
  - Colony Tablet (Overview, first colony);
  - any colony block (tab per block);
  - ants (Overview);
  - the reply to every C2S action (Needs / Trade / Instinct / "Diplomacy" / Research);
  - QA scenes (`VisualQaScenes.java:571-580`).
- **Static snapshot.** There is no live refresh and no polling. Progress bars and resource counts freeze until the server re-sends.
- **Pauses singleplayer.** `isPauseScreen()` is not overridden, and the vanilla default returns `true` (verified against the client jar), so the integrated server and the colony simulation pause while the journal is open.
- **Every action rebuilds the screen from scratch.** The tab is reset to the one the server names, and research pan, trade page and selected diplomacy target are all lost.
- Closing: the "X" button (`:136-137`) or Esc (default).
- There is no keybind for opening it.
- Tab normalisation (`:1225-1236`): "Buildings"/"Build" → Build; "Requests"/"Needs" → Needs; "Diplomacy"/"Relations"/"Events" → Relations; Research, Trade, Instinct and Guide map to themselves; anything else → Overview.

### 10.2 Layout and theme

- The class Javadoc (`:30-36`) sets the concept: a "modern translucent colony workspace" with a persistent section rail. Warm chitin amber is reserved for focus and identity; "quiet graphite glass" carries the information hierarchy.
- **Panel size** (`:1238-1254`): width = `min(max(320, W−12), max(640, 0.92·W))`, height = `min(max(180, H−12), max(300, 0.90·H))`, centred (W, H = scaled GUI size).
- **Columns** (`:1256-1266`): nav rail 118 px when the panel is at least 560 px wide, otherwise 92 px. Content x = panelX + nav + 7; content width = panel − nav − 15.
- **Surfaces:**
  - 2 px chamfered "cut-corner" rectangles and gradients drawn with raw fills (`fillCutRect`, `fillCutGradient`, `outlineCutRect`, `:803-840`);
  - a soft drop shadow offset (6, 8);
  - a 1 px inner glow and a top bevel highlight (`:168-173`).
- **Palette constants** (`:50-78`), ARGB:
  - Scrim: `0x34060A0C → 0x50030709`, light, so the world stays visible.
  - Panel: `0xB51A2022 → 0xC00D1214`, border `0xA56F817C`, glow `0x3286CDBB`.
  - Accent: `0xFFFFC56B` (amber), dim `0xFF9D7845`.
  - Text: main `0xFFF7EBD8`, soft `0xFFE1D8C8`, muted `0xFFC1AE8D`, faint `0xFF8E9893`.
  - Cards, chips, rows and the viewport use graphite gradients.
- **Semantic colours:**
  - Resources (`:1122-1133`, mirrored in `ColonyUiSnapshot.java:535-545`): food `91C46C`, ore `B9B8AC`, chitin `D6B16E`, resin `D69042`, fungus `9BC76C`, venom `7DD66C`, knowledge `B58BFF`.
  - Castes (`ColonyUiSnapshot.java:547-557`): queen `F0C26E`, giant `D06B5D`, major `D99555`, soldier `C15D48`, miner `A9A9A9`, scout `8EC8D6`, worker `D8B57A`.
  - Status: complete green `6DD08E`, in progress orange `D69042`, research active purple `B58BFF`, startable amber `E0B05A`, locked brown `6C5A43`.
- **Icons:** vanilla item renders of mod or vanilla items.
  - Resources (`:1135-1146`): wheat, raw iron, chitin shard, resin glob, fungus culture, venom sac, pheromone dust.
  - Buildings (`:1148-1162`): hay block, chitin shard, iron ore, bone, bell, resin glob, dust, fungus, venom sac, queen egg; default = colony tablet.
  - Research (`:1164-1181`): the "trade/diplomacy → Colony Seal" branch is unreachable with the current node ids.
  - Trade keys (`:1183-1212`).
- Text overflow: every label goes through `ellipsize()`, which appends "..." (`:1214-1223`).
- `shortName()` cuts a name to its first word when it is longer than 12 characters and contains a space (`:1112-1116`). This produces ambiguous labels such as "Pheromone > Chitin" for every chitin-armor purchase, or "Food for Pheromone" for the Pheromone Archive.

Wireframe of the shell:

```
+----------------------------------------------------------------------------------+
| [amber mark] <colony name>                        ( Culture · Relationship )  [X] |  identity bar, 30 px
+-------------+--------------------------------------------------------------------+
| SECTIONS    | [icon|Food 120][Ore 20][Chitin 24][Resin 24] ...  <- Overview & Build only
| ----------- | <Section title>                                                    |
| |Overview   | ‾‾‾‾ (38 px amber underline)                                       |
| |Buildings  |  content viewport (tab-specific)                                   |
| |Requests   |                                                                    |
| |Tech       |                                                                    |
| |Trade      | [action rail: Instinct buttons / Relations targets + actions]      |
| |Instinct   |                                                                    |
| |Guide      |                                                                    |
| |Relations  | Colony #1   Rep 0   Claim 18                   ▌feedback toast     |
+-------------+--------------------------------------------------------------------+
```

### 10.3 Chrome: header, navigation, resource strip, footer

- **Header** (`:175-190`):
  - an 18×18 amber cross glyph drawn with fills;
  - the colony name (`snapshot.title`), ellipsised to `max(120, pw−390)` px;
  - a right-aligned meta pill "`culture` · `relationship`", at most 204 px.
- **Navigation** (`:113-134`, `:192-199`):
  - caption `ui.navigation` in upper case ("SECTIONS" / "РАЗДЕЛЫ");
  - 8 `FormicButton` tabs, 22 px tall at a 24 px pitch, starting at panel y + 62, each with a coloured accent stripe;
  - labels use the full tab title, except Research, which uses `research_short` ("Tech" / "Наука").

  | Tab id | EN label | RU label | Accent | Source |
  |---|---|---|---|---|
  | Overview | Overview | Обзор | `D6A253` | `:39` |
  | Build | Buildings | Постройки | `C68A54` | `:40` |
  | Needs | Requests | Заявки | `D9C36A` | `:41` |
  | Research | Tech | Наука | `A884E8` | `:42` |
  | Trade | Trade | Торговля | `77C891` | `:43` |
  | Instinct | Instinct | Инстинкт | `D17954` | `:44` |
  | Guide | Guide | Справочник | `78A9C8` | `:45` |
  | Relations | Relations | Отношения | `B78AD6` | `:46` |

- **Resource strip** (Overview and Build only, `:1275-1280`, `:315-344`):
  - 7 chips, `max(4, min(7, (width+6)/116))` columns, 19 px rows at a 22 px pitch;
  - each chip: item icon + colour bar + short label + value;
  - knowledge uses `ui.resource.knowledge_short`.
- **Section heading** (`:213-214`): the tab's full title plus a 38 px amber underline.
- **Action rail** (Instinct and Relations only, `:1268-1273`): starts at panel bottom − 76 px, in a framed strip (`:794-801`).
- **Footer** (`:842-856`):
  - "Colony #%s", "Rep %s" and "Claim %s" at fixed x offsets 0, 82 and 150 px;
  - the **feedback toast** is right-aligned on the same line, green-accented, width `max(60, width−230)`.

### 10.4 The eight tabs

**Overview** (`:362-398`):

- **Identity banner**, 38 px, with a left stripe in the relationship colour:
  - "Personality": `<personality> · <detail>`, e.g. "Steady · Balanced growth";
  - "Relationship": `ui.identity_rep` → "New allies | rep 0";
  - an amber line "Goal: <currentTask>".
- **Up to 6 stat rows** built by the server (`ColonyUiSnapshot.java:410-451`), each a label, value text and mini progress bar. The client shows `max(2, (h−100)/21)` of them.
  1. Current task.
  2. Events: the latest message beginning "Recurring event: ", if any.
  3. Workforce: "N workers, N miners, N guards".
  4. Active build: the first incomplete or queued building, "Type N% | detail".
  5. Top need: the largest open request, "Resource N -> Building", with a % bar.
  6. Research: the active node, or else the first incomplete one, "label | status".
  7. Queen: "N hp / alive|lost".
  - When all 7 rows exist, `limit(6)` drops the Queen row.
- **"Population" chips**: the loop is capped at 4 chips (`:385-396`), so only Worker, Scout, Miner and Soldier are ever shown. Major, Giant and Queen counts never appear.

**Build** (`:400-420`):

- Every building record, plus the build queue.
  - Queued entries have pos "queued", level 1 and progress 0; their detail is the cost text.
- Sorted incomplete first, then by type id.
- Capped at `maxRows(h)` = clamp(h/21, 4, 10) entries, **even in the two-column layout** (2 columns when width ≥ 560).
- Each 30 px card:
  - building icon;
  - "`<name>`  L`<level>`";
  - "`<status>` · `<progress>`% `<detail>`";
  - a wide progress bar, green when complete, otherwise orange.
- Status keys map through `BuildingVisualStage` (`BuildingVisualStage.java:7-48`): Planned / Building / Complete / Upgraded / Damaged / Repairing, plus "Queued".
- Detail per stage (`ColonyUiSnapshot.java:328-354`): costs for planned; "Needs …" for construction/repair; raw ticks such as "120t" for damaged.

**Needs** (Requests) (`:422-460`):

- Open requests only (fulfilled < needed).
- The client re-sorts them by missing amount, descending, which **discards the server's priority sort** (`ColonyUiSnapshot.java:114,312-326`).
- Capped at `cardLimit(h, 60)` = clamp(h/60, 2, 8); 2 columns when width ≥ 600.
- Each 54 px card:
  - resource icon on the left, building icon on the right;
  - title `ui.request.title` "Food for Market";
  - delivery line `ui.request.delivery` "8 Wheat gives 12 Food" (the misleading per-bundle numbers, §3.7);
  - reward line `ui.request.reward` "+3 token, +2 rep, P2";
  - a "Help" pill;
  - a progress bar.
- **Clicking anywhere on the card** sends `ContractRequestPayload(contractId)` (`:263-269`).
- Empty state: the card "No open requests / The colony has enough supplies for its current plan." with a writable-book icon.
- Not shown: the request `reason` and `resourceCost`, although both are transmitted.

**Research** (map) (`:462-567`, `:987-1036`):

- A clipped viewport with a 32 px faint grid. Nodes are 142–184 px wide and 48 px tall, and every node position is **hard-coded**:

  | Node | Column x | y | Prerequisite |
  |---|---|---|---|
  | `chitin_cultivation` | 18 | 56 | – |
  | `resin_masonry` | 18 | 114 | – |
  | `fungus_symbiosis` | 18 | 172 | – |
  | `scented_ledger` | 18 | 230 | – |
  | `mandible_plating` | viewW−nodeW−18 | 114 | resin_masonry |
  | `venom_drills` | viewW−nodeW−18 | 172 | fungus_symbiosis |
  | `treaty_sigils` | viewW−nodeW−18 | 230 | scented_ledger |

  Positions: `:997-1008`. Prerequisites come from `src/main/java/com/formicfrontier/sim/ResearchNode.java:9-15`. An unknown id falls back to (18, 56), overlapping chitin_cultivation.
- **Edges** are orthogonal connectors with an arrowhead: green when the parent is complete, otherwise amber (`:737-745`).
- **Node card**:
  - left stripe coloured by state: complete green, active purple, startable amber, locked brown;
  - icon, localised name, and state text ("Discovered", "Being studied", "Ready - click to begin", "Requirements not met");
  - a ">" marker when startable;
  - a progress bar.
- **Inspector**: a 45 px overlay at the top of the viewport.
  - Idle: pan and hover hints.
  - On hover: "`<name>` / `<state>`", the `.detail` text, and "Required chamber: X / Cost: 12 Knowledge, 16 Chitin". The client computes these from the shared `ResearchNode` enum, not from the snapshot.
- **Controls:**
  - left-drag pans;
  - the wheel pans 22 px per notch on both axes;
  - right-click resets the pan to (16, 12);
  - left-clicking a *startable* node sends `ResearchRequestPayload` (`:234-251`, `:275-310`).
- `clampResearchPan` allows only a horizontal pan range of about [−32, 16]; the vertical range is set by the fixed `RESEARCH_CANVAS_HEIGHT` = 292 (`:78,987-995`).
- "Startable" requires the colony to already hold the resources. The server's "open resource requests instead" branch (`ColonyLogistics.java:104-115`) can therefore only be reached through the `/formic research` command.

**Trade** (market) (`:569-669`, `:1051-1068`):

- A viewport with a 25 px **context banner** on top.
  - Idle: the latest caravan activity ("Caravan: Food 12 -> Resin 8 with #2", from events, `ColonyUiSnapshot.java:461-493`) or the hover hint.
  - On hover: "16 Wheat for 1 Pheromone Token", then "`<server status>` / Click the card to complete this trade" or "The colony cannot accept this trade yet".
- At width ≥ 360 there are **two columns**, "COLONY REQUESTS" (`sell_*` offers) and "COLONY WARES" (`buy_*` offers). Otherwise a single "EXCHANGE OFFERS" list.
- Each 42 px card:
  - input icon → arrow → output icon;
  - "`<Input>` > `<Output>`" (short names);
  - "Nx > Mx";
  - "Available" / "Unavailable";
  - a "Trade" pill.
- Sort order: `sell_wheat` first, `buy_colony_seal` second, then available offers whose status starts with "Trade Hub", then other available offers, then unavailable ones; ties broken by id.
- The mouse wheel pages both columns together, one row per notch, with an "i / n" indicator and a "Scroll for more offers" hint.
- Clicking an **available** card sends `TradeRequestPayload(offerId)` (`:253-261`).
- **Inconsistency:** `available` = `ColonyTradeCatalog.isAvailable`, which ignores `minReputation` (`ColonyTradeCatalog.java:126-131`). Rep-gated offers therefore look green and clickable while their status says "Requires reputation N", and the server then answers "This colony does not trust you enough yet." (`:63-65`).

**Instinct** (`:671-679`, `:872-882`):

- A single help line: "Biases autonomous growth."
- Four rows, one per priority in the current order: "Food — Priority 1" with a bar of 100 %, 75 %, 50 %, 25 % (value = order / 4).
- The action rail holds four equal buttons, Food / Ore / Chitin / Defense, each sending `PriorityRequestPayload(id)`.

**Guide** (`:681-702`, `src/main/java/com/formicfrontier/sim/GuideChapter.java:3-58`):

- Eight 19 px rows: title | one-line detail (or the locked text) | an "Open"/"Locked" pill. Rows are not interactive.

| Chapter | EN title | EN detail | Unlock rule |
|---|---|---|---|
| first_steps | First steps | Place Queen Egg, open tablet, watch tasks. | always |
| castes | Castes | Caste roles: build, haul, scout, defend. | always |
| resources | Resources | Food/ore/chitin; resin/fungus/venom/know. | always |
| buildings | Buildings | Mounds, stores, nurseries, mines, defenses. | always |
| cultures | Cultures | Paths shift food, resin, venom, diplomacy. | always |
| helping | Helping | Donate supplies; trade; set instincts. | always |
| relations | Relations | Tokens and seals shift allies, rivals, wars. (locked: "Meet another colony or raise reputation to unlock diplomacy notes.") | known colonies ≠ ∅, or rep ≠ 0, or rank ≥ Burrow |
| research | Research | Archive plus knowledge unlocks paths. (locked: "Build Archive or gain Knowledge to unlock.") | archive complete, or active/completed research, or **knowledge > 0** |

- New colonies start with knowledge 8 (`ColonyService.java:646`), so the Research chapter is effectively always open.

**Relations** (`:704-723`, `:884-906`):

- If the colony knows no other colonies: an info card "No known colonies".
- Otherwise:
  - up to 4 relation rows: "#id" (with "▸" marking the selected one), the relation label, and a bar (ally 100, neutral 60, rival 30, war 10, `:1077-1085`);
  - "Selected target #id";
  - rows for the **first 3** diplomacy actions only (Envoy, Tribute, Truce): label plus "`T`T `D`D `S`S · `<min rank>`".
- The action rail holds up to 5 target buttons (#id, 42×19) and 3 action buttons that send `DiplomacyRequestPayload(actionId, selectedTarget)`.
- Problems:
  - Incite and War Pact cannot be used from the UI;
  - the banner cost is not shown;
  - the Treaty Sigils discount is not shown;
  - the action buttons exist even when the colony knows no colonies. The target is then 0, and the server auto-picks *any* other colony, including unknown ones (`ColonyService.java:258-260,592-604`);
  - no current rank is shown anywhere.
- No **Events** tab exists. The events list is transmitted but never displayed, although README and the manual playtest still list "Diplomacy" and "Events" tabs (`README.md:32`; `docs/manual-playtest.md:11`).

### 10.5 Widget toolkit

- `FormicButton extends AbstractWidget` with two styles (`:911-985`):
  - `TAB`: left-aligned label with an accent stripe;
  - `ACTION`: centred label.
  - States: selected (amber border), disabled, hover and normal. The disabled style is never used, because `active` is never set to false.
  - Narration reads the title only. Custom-drawn cards have no narration and no keyboard access.
- Clickable cards are implemented as per-frame hitbox lists (`ResearchHitbox`, `TradeHitbox`, `RequestHitbox`, `:1292-1308`), rebuilt on every render and tested in `mouseClicked`.
- No vanilla tooltips are used. Hover information goes into fixed inspector banners instead (research, trade).

### 10.6 UI bugs and quirks (summary)

- The data shown can belong to a different colony than the one actions apply to (§3.2, §11.3).
- Needs cards show the wrong delivery amounts (§3.7).
- Trade cards show "Available" for offers whose reputation gate isn't met.
- Only 3 of 5 diplomacy actions are available; banner costs and discounts are hidden.
- Population chips are capped at 4.
- The Build list is capped at 10 even in two columns.
- The Overview drops the Queen row once there are 7 rows.
- State resets after every action; there is no live refresh; singleplayer pauses.
- Transmitted but unused: `rank`, `queenHealth`, `queenAlive`, `events`, `RequestEntry.reason`, `RequestEntry.resourceCost`, `BuildingEntry.pos` (except the "queued" check), `DiplomacyEntry.bannerCost`.
- Many dynamic strings are English only (§9.4).

---

## 11. Networking

### 11.1 Payload registry (`ModNetworking.java:17-39`)

| Channel id | Direction | Record fields (codec) | Sent from | Server handler |
|---|---|---|---|---|
| `formic_frontier:colony_ui` | S2C | `ColonyUiSnapshot snapshot` (custom codec, `src/main/java/com/formicfrontier/network/ColonyUiPayload.java:9-28`) | `ColonyService.openColonyScreen` (`ColonyService.java:387-389`) | client: `FormicFrontierClient.java:20-24` |
| `formic_frontier:contract_request` | C2S | `String contractId` (`STRING_UTF8`) (`ContractRequestPayload.java:10-17`) | Needs card click (`ColonyStatusScreen.java:263-269`) | `ColonyService.completeContract` |
| `formic_frontier:trade_request` | C2S | `String offerId` (`TradeRequestPayload.java:10-17`) | Trade card click (`ColonyStatusScreen.java:253-261`) | `ColonyService.trade` |
| `formic_frontier:priority_request` | C2S | `String priorityId` (`PriorityRequestPayload.java:10-17`) | Instinct buttons (`ColonyStatusScreen.java:872-882`) | `ColonyService.setTopPriority` |
| `formic_frontier:diplomacy_request` | C2S | `String actionId`, `VarInt targetColonyId` (`DiplomacyRequestPayload.java:10-19`) | Relations action buttons (`ColonyStatusScreen.java:900-905`) | `ColonyService.performDiplomacy(player, action, target)` |
| `formic_frontier:research_request` | C2S | `String nodeId` (`ResearchRequestPayload.java:10-17`) | Research node click (`ColonyStatusScreen.java:240-248`) | `ColonyService.startResearch` |

All payload files live in `src/main/java/com/formicfrontier/network/`. All C2S handlers hop to the server thread with `context.server().execute(...)` (`ModNetworking.java:24-38`).

### 11.2 `ColonyUiSnapshot`: what the server sends (`src/main/java/com/formicfrontier/network/ColonyUiSnapshot.java`)

- **Wire format.** A hand-written, ordered serialisation: `VarInt` for ints, `BOOL`, `STRING_UTF8` (null written as ""), and lists written as a `VarInt` count followed by the entries (`:221-283`, `:576-622`). There is no version field and no size limit.
- The whole snapshot is rebuilt and re-sent after every action.

| # | Field | Type | Built from | Rendered? |
|---|---|---|---|---|
| 1 | colonyId | int | `colony.id()` | footer |
| 2 | title | String | `progress().name()` | header |
| 3 | initialTab | String | caller; defaults to "Overview" | selects the tab |
| 4 | feedbackMessage | String | caller | footer toast |
| 5 | cultureKey | String | `"formic_frontier.culture."+id` | header pill |
| 6–7 | personalityKey, personalityDetailKey | String | `ColonyIdentity.personality()` (`src/main/java/com/formicfrontier/sim/ColonyPersonality.java:39-48`) | overview banner |
| 8–9 | relationshipKey, relationshipColor | String, int | `ColonyIdentity` (`ColonyIdentity.java:11-44`): not allied → wild/rival; rep ≥ 50 trusted, ≥ 15 friendly, < 0 strained, else new_allies | header pill, overview |
| 10 | rank | String key | `ColonyRank.current()` | **no** |
| 11 | currentTask | String | colony | overview "Goal" |
| 12–13 | queenHealth, queenAlive | int, bool | colony | **no** (the server builds the overview row itself) |
| 14–15 | reputation, claimRadius | int | progress (claim 18–48) | footer, overview |
| 16 | overview | `List<OverviewEntry(labelKey, value, progress, color)>`, ≤ 6 | `overviewRows()` (`:410-451`) | Overview |
| 17 | resources | `List<Metric(id, labelKey, value, max=0, color)>`, 7 | colony stock | resource strip |
| 18 | population | `List<Metric>`, 7 castes | caste counts | chips (first 4) |
| 19 | buildings | `List<BuildingEntry(typeId, labelKey, pos, level, progress, complete, statusKey, detail)>` | building records + queue (`:74-89,328-354`) | Build |
| 20 | requests | `List<RequestEntry(buildingId, buildingKey, resourceId, resourceKey, fulfilled, needed, reason, contractId, resourceCost, deliveryItemKey, deliveryItemCount, deliveryAmount, priority, rewardTokens, reputationDelta)>` | requests → contracts (`:91-114`) | Needs |
| 21 | research | `List<ResearchEntry(nodeId, label, progress, duration, complete, active, startable, status)>`, 7 | `ResearchNode.values()` (`:116-132`) | Research |
| 22 | trades | `List<TradeEntry(offerId, inputKey, inputCount, outputKey, outputCount, available, status)>` | visible offers only (`:134-152`). Wrapped in `try/catch (IllegalArgumentException \| LinkageError)` so unit tests can run without MC bootstrap | Trade |
| 23 | tradeActivity | String | latest "Recurring event: trade caravan…" event, compacted | trade banner |
| 24 | instinct | `List<Metric(id, key, value=order, max=4, color)>` | colony priorities (`:154-158`) | Instinct |
| 25 | diplomacy | `List<DiplomacyEntry(actionId, label, token, dust, seal, banner, minRank display name)>`, 5 | `DiplomacyAction.values()` (`:160-163`) | Relations (first 3) |
| 26 | relations | `List<RelationEntry(colonyId, stateId, labelKey)>` | known colonies (`:165-169`); a non-numeric key becomes id 0 | Relations |
| 27 | guide | `List<GuideEntry(chapterId, titleKey, detailKey-or-lockedKey, unlocked, color)>`, 8 | `GuideChapter` (`:171-181`) | Guide |
| 28 | events | `List<EventEntry(ageTicks, message)>` | event log | **no** |

### 11.3 Server-side handling and validation (`ColonyService.java`)

**Common to all handlers:**

- The target colony is always `nearestColony(player.blockPosition(), R)`, not the colony whose UI the player is looking at. The payloads carry no colony id, apart from the diplomacy target.
- There is **no ownership or alliance check**. A player can drive a rival or wild colony's research, instinct and trades by standing near it.
- There is no check that the player has the UI open or holds a tablet, and no rate limiting. A modified client can send any request at any time.
- Every handler replies by re-opening the UI with a feedback toast.

| Handler | Radius | Validation (in order) | Effects |
|---|---|---|---|
| `trade` (`:138-152`) → `ColonyTradeCatalog.execute` (`ColonyTradeCatalog.java:58-82`) | 96 | colony exists → offer id known → rep ≥ `minReputation` → visible (culture / Scented Ledger) and research met → player holds `inputCount` items (**no creative bypass**) | Removes items, gives output (dropped if the inventory is full), applies the colony resource delta and reputation, logs task and event. Reply tab: Trade |
| `completeContract` (`:154-203`) | 128 | colony exists → contract id is currently open (`ColonyLogistics.contract`, `ColonyLogistics.java:41-48`) → player holds `itemCountFor(missing)` items (creative bypass) | Delivers the full missing amount (`ColonyLogistics.fulfillContract`, `:50-83`), gives the token reward, may start construction, research or an expansion outpost. Reply tab: Needs, with "… \| +N tokens, +N rep" |
| `setTopPriority` (`:205-231`) | 96 | priority id valid (else action bar "Unknown instinct: x") → colony exists | Moves the priority to the front, sets current task "Colony instinct biased toward x", logs event. Reply tab: Instinct |
| `performDiplomacy` (`:237-296`) | 128 | action id valid → colony exists → actor rank ≥ minimum (`:253-256`) → target: explicit id must exist and differ from the actor (**need not be a known colony**); id 0 → `diplomacyTarget()` picks the worst relation, then the nearest (`:592-613`) → player holds tokens/dust/seals/banners (creative bypass) | Removes items. Relation is updated **symmetrically**. Actor reputation += delta. Events logged on both colonies. `DiplomacyConsequences.apply` (diplomacy doc). Reply tab "Diplomacy" (shown as Relations) |
| `startResearch` (`:298-316`) → `ColonyLogistics.startResearch` (`:85-122`) | 128 | colony exists → node id valid (else "Unknown research") → not complete → no active research → pheromone archive complete → required building complete → prerequisites complete → resources available; if not, opens "research x" requests and fails | Consumes colony resources, starts the research. Reply tab: Research |

### 11.4 Networking issues

1. The colony shown and the colony acted on can differ (tablet = first colony; actions = nearest colony; inconsistent radii of 96 and 128).
2. No authorisation model at all (no owner, no ally check).
3. Unbounded strings and lists, and no protocol version.
4. The full snapshot is re-sent on every click, and each reply re-creates the screen, losing client state.
5. The client relies on shared enums (`ResearchNode`, `ResourceType`) for graph layout and costs instead of the snapshot, so client and server must ship identical code, which Fabric does anyway.

---

## 12. Commands (`src/main/java/com/formicfrontier/command/FormicCommands.java`)

The whole tree is registered with `CommandRegistrationCallback` and has **no `.requires(...)` anywhere** (`:24-74`). Every command is therefore available to **every player at permission level 0**, on dedicated servers too, including terrain-destroying and colony-wiping ones. `sendSuccess(..., true)` also broadcasts to ops.

| Syntax | Args | Target | Effect | Feedback | Source |
|---|---|---|---|---|---|
| `/formic colony create` | – | position of the command source | `ColonyService.createColony(level, pos)`: a player-allied colony with the full terrain clear and structures (§3.1) | "Created Formic colony #N" | `:27,77-82` |
| `/formic colony dump` | – | **first** colony | Prints `ColonyData.statusText()`: id, origin, culture, personality, relationship/goal, resources, queen, population/upkeep, faction/rank/rep/claim, stage, castes, buildings, queue, relations, priorities, latest event, task (`src/main/java/com/formicfrontier/sim/ColonyData.java:194-213`) | status text, or "No Formic colony exists." | `:28,84-91` |
| `/formic colony renovate` | – | nearest colony ≤ 160 | Clears a radius-72 area, re-places every building at canonical sites, re-lays wide paths and resource clusters (`ColonyService.java:445-491`) | "Renovated nearest Formic colony into Queen Hall campus." / failure "No nearby Formic colony to renovate." | `:29,93-101` |
| `/formic colony tick <ticks>` | int 1..10000 | **all** colonies | Runs `max(1, ticks/20)` rounds of `tickEconomy()` + `tickWorld(level)`. README's "n simulation ticks" is misleading: n = game ticks / 20 | "Advanced colony economy by N economy ticks." | `:30-32,103-113` |
| `/formic colony priority <word>` | word | nearest colony ≤ 96 (needs a player) | `setTopPriority` (alias of `instinct`) | UI re-opens on Instinct | `:33-35,143-149` |
| `/formic colony instinct <word>` | food/ore/chitin/defense (no suggestions) | same | same | same | `:36-38` |
| `/formic colony seed-rivals <count> <distance>` | int 1..6, int 32..256 | around the source | Creates `count` **rival** colonies evenly spaced on a circle of radius `distance` (`ColonyService.createColony(level, pos, false)`); each clears terrain | "Seeded N rival Formic colonies." | `:39-46,115-125` |
| `/formic colony resource set <type> <amount>` | word, int ≥ 0 | **first** colony | Sets that resource. Prints success even when no colony exists; an invalid type throws `IllegalArgumentException`, which surfaces as vanilla's generic "unexpected error" | "Set <type> to N" | `:47-55,127-134` |
| `/formic ant spawn <caste>` | word (no suggestions) | position of the command source | Spawns an ant of that caste, assigned to the nearest colony ≤ 96 (else colony 0). Invalid caste → generic error | "Spawned <caste> ant." | `:56-59,136-141` |
| `/formic trade <offer>` | offer id (no suggestions) | nearest colony ≤ 96 (player) | `ColonyService.trade` | UI re-opens on Trade | `:60-62,151-157` |
| `/formic research <node>` | node id | nearest colony ≤ 128 (player) | `startResearch`; the only way to reach the "open resource requests" branch | UI re-opens on Research | `:63-65,159-165` |
| `/formic diplomacy <action>` | envoy/tribute/truce/incite/war_pact | nearest colony ≤ 128; auto-picked target | `performDiplomacy(player, action)` with target 0; the only way to use incite and war_pact | UI re-opens on Relations | `:66-68,167-173` |
| `/formic qa scene <name>` | 65 scene names, with suggestions (`VisualQaScenes.java:130-196`) | pinned QA origin near the source | **Destructive QA staging** (`VisualQaScenes.java:277-584`), see below | – | `:69-73,175-177` |

What `/formic qa scene` does:

- **wipes every colony in the world** (`savedState.clearColonies()`, `:350`);
- flattens a square of radius 58–136 around the pinned origin down to dirt and grass (58 is the default, e.g. for `tablet_*` scenes), and on first use clears it **up to the world ceiling** (`:344,1880-1901`, `qaRadius` `:608-629`);
- discards ants, display entities and items in that area;
- sets time to 6000 and clears the weather (`:346-347`);
- **switches the caller to SPECTATOR** (`:1386`);
- places a scene and, for `tablet_*` scenes, opens the UI on a preset tab.
- Used by the automated screenshot harness (`VisualQaClient.java:17-40,152`).

The command list in `README.md:20-31` matches these commands, apart from the "n simulation ticks" wording. The tablet line (`README.md:32`) lists tabs that no longer exist.

---

## 13. Build setup and versions

| Item | Value | Source |
|---|---|---|
| Minecraft | 1.21.11 | `gradle.properties:4`; `src/main/resources/fabric.mod.json:24` |
| Fabric Loader | 0.19.2 (depends `>=0.19.2`) | `gradle.properties:5`; `fabric.mod.json:23` |
| Fabric API | 0.141.3+1.21.11 (depends `"*"`) | `gradle.properties:7`; `fabric.mod.json:26`; a copy of the jar sits in the untracked `mods/` folder |
| Fabric Loom | 1.16.1 (`id "fabric-loom"`) | `gradle.properties:6`; `build.gradle:2` |
| Gradle | 9.4.1 (wrapper, `validateDistributionUrl=true`) | `gradle/wrapper/gradle-wrapper.properties:3,5` |
| Java | `options.release = 21`; source/target 21; `fabric.mod.json` `"java": ">=21"`. README says Temurin JDK 25 is used locally; CI uses Temurin **25** | `build.gradle:54-56,69-74`; `fabric.mod.json:25`; `README.md:10`; `.github/workflows/build.yaml:22-25` |
| Mappings | Official Mojang (`loom.officialMojangMappings()`) | `build.gradle:39` |
| Dependencies | `minecraft`, `mappings`, `modImplementation` fabric-loader and fabric-api, `testImplementation` `net.fabricmc:fabric-loader-junit:${loader_version}`. **No other libraries.** Plugins: fabric-loom, maven-publish | `build.gradle:1-4,37-44` |
| Repositories | settings.gradle: plugins from Fabric maven, Gradle Plugin Portal and Maven Central; dependencies from Fabric maven and Maven Central. `build.gradle` has an empty `repositories {}` | `settings.gradle:1-20`; `build.gradle:34-35` |
| Project | `rootProject.name = "formic-frontier"`; `mod_version=0.1.0`; `maven_group=com.formicfrontier`; `archives_base_name=formic-frontier` | `settings.gradle:22`; `gradle.properties:9-11` |
| Source sets | `splitEnvironmentSourceSets()`: `src/main` (common), `src/client` (client only). Mod `formic_frontier` includes both | `build.gradle:13-22` |
| Tests | `fabricApi.configureTests { createSourceSet = true; modId = "formic_frontier_test"; enableGameTests = true; enableClientGameTests = false; eula = true }` creates `src/gametest`. The test mod's `fabric.mod.json` declares entrypoint `fabric-gametest` → `com.formicfrontier.test.FormicFrontierGameTest`. JUnit 5 unit tests in `src/test` (2 GB heap) use a hack that copies Fabric system properties from `.gradle/loom-cache/launch.cfg` | `build.gradle:24-32,76-92`; `src/gametest/resources/fabric.mod.json:1-12` |
| Resources | `processResources` expands `${version}` in `fabric.mod.json` | `build.gradle:46-52` |
| Run configs | Every `run*` JavaExec task gets `formic.visualQa`, `.dir`, `.exit`, `.world`, `.scope`, `.scenes` system properties for the screenshot harness | `build.gradle:58-67` |
| Asset validation | Task `validateAssets` (`Exec`, `python tools/validate_assets.py`; override the interpreter with `-DpythonExecutable`); `check.dependsOn` it | `build.gradle:94-100` |
| Jar / publishing | LICENSE copied into the jar as `LICENSE_formic-frontier`; `maven-publish` publication `mavenJava` | `build.gradle:102-116` |
| Gradle JVM args | `-Xmx2G -Djdk.net.hosts.file=<original-project>/.local/hosts.gradle -Djava.net.preferIPv4Stack=true`; `org.gradle.parallel=true`. A **machine-specific WSL path**; `.local/` is gitignored. When `jdk.net.hosts.file` is set the JDK resolves names from that file only, so this probably breaks dependency download on machines without it, including CI (inferred, not verified) | `gradle.properties:1-2`; `.gitignore:2` |
| fabric.mod.json | id `formic_frontier`, name "Formic Frontier", description "A single-colony MVP for large ant economies, castes, chambers, and debug-driven testing." (stale), authors "Formic Frontier contributors", license MIT, icon `assets/formic_frontier/icon.png`, environment `*`, entrypoints main `com.formicfrontier.FormicFrontier` and client `com.formicfrontier.client.FormicFrontierClient`. No mixins, no access widener | `src/main/resources/fabric.mod.json:1-28` |
| CI | GitHub Actions `Build`: (1) asset validation (Python 3.12); (2) `./gradlew build` (runs the server GameTests) on JDK 25 with wrapper validation, uploading reports on failure; (3) visual-QA manifest check (`tools/visual_qa_report.py --ci-manifest-only`) | `.github/workflows/build.yaml:1-52` |
| Local QA scripts | `scripts/test-mod.ps1` runs `gradlew build` and greps the logs for CRASH / Exception / ERROR / "Missing texture" (`:38-80`). Also doctor, gui-smoke, autonomous-loop and gate scripts (QA doc) | `scripts/` |
| Repo hygiene | An empty stray file at the root named `II, data[16:24]))⏎PY⏎rm -rf assets` (a broken heredoc), hidden by the `.gitignore` pattern `/II, data*` | repo root; `.gitignore:14` |

---

## 14. Consolidated bugs, inconsistencies and magic numbers

### Bugs and inconsistencies

**High severity**

1. **No command permissions.** `/formic qa scene` wipes all colonies, flattens large areas and switches the caller to spectator. `colony create`, `seed-rivals` and `renovate` clear terrain. `resource set` edits colony state. All are usable by any player (`FormicCommands.java:24-74`; `VisualQaScenes.java:344-350,1386`).
2. **Wrong-colony UI.** The tablet shows `firstColony()` (`ColonyTabletItem.java:27-29`; `ColonySavedState.java:65-67`) while actions go to the nearest colony, at 96 or 128 blocks (§11.3).
3. **No loot tables.** All 19 blocks drop nothing, so crafted blocks and chitin beds are lost when mined (`src/main/resources/data/` has no `loot_table/`).

**Medium severity**

4. **Needs cards lie about amounts:** they show one bundle, but the server takes the full outstanding amount (`ColonyUiSnapshot.java:94-95` vs `ColonyService.java:168-176`; the test enshrines the display at `src/test/java/com/formicfrontier/sim/ColonyEconomyTest.java:234-257`).
5. **Weapons**:
   - no attack attributes (bare-hand base hit);
   - bonus damage is a second `hurt()` limited by invulnerability frames;
   - no `WEAPON` component, so durability never drops;
   - not enchantable, not in `#swords` / `#spears`;
   - damage source is `mobAttack` (`FormicWeaponItem.java:20-27`; vanilla behaviour verified in the jar).
6. **Armor is not enchantable or trimmable:** it is missing from the `minecraft:*_armor` tags. The explicit durability calls are redundant (`ModItems.java:81-88`).
7. **Colony Tablet is unobtainable** in survival. **Queen Egg** is only a rep-40, 64-token trade.
8. **Dimensions are ignored.** Colonies store no dimension; Queen Egg and `colony create` work in the Nether and End, but world ticks run only in the Overworld (`FormicFrontier.java:43`), and `nearestColony` ignores dimension.
9. **No ownership or alliance checks** on any player action, donation or trade (§3.4, §11.3).
10. **Crafted colony blocks create no buildings**; this contradicts the design docs (§15).

**Low severity**

11. Trade cards show rep-gated offers as available (`ColonyTradeCatalog.java:126-131` vs `:63-65`).
12. The trade price multiplier overwrites the Scented Ledger discount (`ColonyTradeCatalog.java:88-98`).
13. The Relations tab exposes only 3 of 5 actions, hides banner cost and discounts, offers actions with target 0 (auto-picks any colony), and never checks that the target is a known colony (`ColonyStatusScreen.java:704-723,884-906`; `ColonyService.java:258-260`).
14. The UI is a static snapshot, pauses singleplayer, and loses its state after every action.
15. Population chips are capped at 4; the Build list at 10; the Overview drops the Queen row once there are 7 rows.
16. Needs cards are re-sorted client-side, ignoring the server's priority order.
17. `formic_frontier.ant.guard` receives the raw caste id (`ColonyService.java:929`).
18. Dynamic strings are English only; 23 lang keys are unused; the `ui.status.disabled` and `building` keys are unused.
19. `aphid_honeydew` and `royal_wax` have no use.
20. Dead code:
    - `ColonyTrades` (a duplicate catalogue) and `ColonyService.tabletText`;
    - `ColonyService.placePath` (`:739-751`);
    - `ContractBundle.resourceAmount()` (`:562-564`);
    - an unused `savedState` local in `depositWorkedResource` (`:404`);
    - the Colony Seal branch of `itemForResearch` (`ColonyStatusScreen.java:1174-1176`).
21. `colony dump` and `resource set` act on the first colony; `resource set` reports success when there is no colony.
22. Asset issues: `block.zip` ships in the jar; block textures are 16×16; the ledger reuses the archive texture; chitin-bed growth has no visuals.
23. The Chitin Bed has no placement rule, although the docs say soil only.
24. Balance smells:
    - the shared repair tag lets a single shard repair top-tier armor;
    - a resin-chitin set costs about the same shards as a chitin set but is far stronger;
    - 1 wheat = +1 reputation makes rep gates trivial;
    - crafted chitin armor costs 288 shards against about 60 tokens by trade.
25. `gradle.properties` contains the machine-specific WSL hosts path.
26. Stale documentation:
    - README and the manual playtest describe Diplomacy and Events tabs and chat outputs;
    - `fabric.mod.json` still says "single-colony MVP";
    - the feedback key says "ledger" while the item is a "Tablet" and the UI title is "Journal".

### Magic numbers relevant to this scope

- **Colony lookup radii:** 96 (block UI, trade, priority, ant fallback, chitin-bed bonus, `spawnAnt`), 128 (contract, diplomacy, research, construction work), 160 (renovate).
- **Timing:** economy tick every 20 server ticks. Wild-colony discovery: check every 900 ticks, 56–96 blocks away, at most 6 colonies (`ColonyDiscoveryService.java:15-22`).
- **Chitin bed:** 4 stages, 1/3 growth chance per random tick, 1–3 shards (+1 with research), 45 % spore chance.
- **Hand-feeding:** royal jelly heal 24 / rep +6; biomass +8 food; wheat +3 food; shard +3 chitin; +1 rep each; ant heals 2 / 4.
- **Contract bundles** 8/12, 4/8, 2/6, 2/6, 2/6, 1/5, 4/6. Token reward `(missing+7)/8 + priority`; reputation `clamp((missing+15)/16 + priority/2, 1, 6)`.
- **Weapons:** bonus 5 / 4, durability 420 / 360, poison 80 ticks.
- **Armor:** base durability 15 / 21; enchantability 18 / 15; toughness 0 / 1.
- **Starting colony:** food 120, ore 20, chitin 24, resin 24, fungus 12/28, venom 4/16, knowledge 8.
- **UI:**
  - panel 92 % × 90 % of the screen (minimum 640×300, bounded by screen − 12);
  - nav rail 118 / 92 px;
  - row 19 px at a 21–22 px pitch;
  - column breakpoints 360 / 560 / 600 px;
  - research canvas height 292, wheel pan 22 px, reset pan (16, 12);
  - action rail 76 px from the bottom;
  - `maxRows` clamp 4..10; `cardLimit` clamp 2..8; overview ≤ 6 rows; 4 population chips; 3 diplomacy actions; 5 target buttons.
- **Commands:** `tick` 1..10000; `seed-rivals` count 1..6, distance 32..256; QA radius 58–136.

---

## 15. Design intent vs. reality

| Intent (source) | Reality (source) |
|---|---|
| "A Queen Egg creates a player-allied colony"; the Guide's first step: "Place Queen Egg, open tablet" (`docs/mvp-architecture.md:5-11`; lang `:195`) | True, but a survival player cannot get an egg without first earning 64 tokens and rep 40 from an existing (usually wild) colony, and cannot get a tablet at all (§3.9) |
| The Colony Tablet is *the* window onto *your* colony (`README.md:32`) | It opens the oldest colony in the world, and its buttons act on whichever colony is nearest (§3.2, §11.3) |
| Tabs: "Overview, Instinct, Buildings, Requests, Research, Trade, Diplomacy, Relations, and Events" (`README.md:32`; `docs/manual-playtest.md:11`) | Eight sections: Overview, Buildings, Requests, Tech, Trade, Instinct, Guide, Relations. There is no Diplomacy or Events tab, and the event log is never shown (§10.4) |
| "Research is clearly a map, trade is clearly a market, requests are player-facing help cards … EN/RU" (`docs/roadmap.md:153-165`) | Largely achieved visually. But requests show wrong amounts, the market shows wrong availability, research can't open resource requests, and all dynamic text is English (§10.6, §9.4) |
| "Blocks are used by generation AND craftable/placeable where it makes sense"; "new blocks come from research/stages" (`docs/content-intent/formic-content-intent.md:67-71`) | 7 blocks are craftable immediately, with no research gate. Placing them does nothing for the colony (UI terminal and ant waypoint only), and they drop nothing when mined (§3.5) |
| "Ant-themed weapons/tools … with real combat stats; soldier castes use them in raids/defense" (`docs/content-intent/formic-content-intent.md:73-75`) | Player weapons lack attack attributes, durability wear and enchantability. Soldiers get only numeric bonuses in raid math (`ColonyArmory`); no ant ever holds an item (§4) |
| "Research tree … unlocks castes, blocks, weapons, and diplomacy options" (`docs/content-intent/formic-content-intent.md:49-50`) | For items, research only unlocks *trades* (resin-chitin armor, saber, spear, fungus, venom) and discounts. Every crafting recipe is ungated (§5) |
| "Do not add a block/item/caste with no behavior and call it done" (`docs/content-intent/formic-content-intent.md:96`) | `aphid_honeydew` and `royal_wax` have no behaviour. 23 lang keys and several UI fields are unused |
| "Chitin armor … mid-game goal" (`docs/mvp-architecture.md:43-44`) | By crafting, a set costs about 288 shards, which is extremely grindy. By trade it costs 60 tokens with no research needed for the chitin tier (§5.2) |
| The command API is "debug" tooling (`docs/mvp-architecture.md:21`) | Registered for everyone, with no permission checks, including destructive QA commands (§12) |
| Custom block/item textures must be 32×32 (`docs/roadmap.md:28-29`) | Items, armor and entities were regenerated at 32×32 or double density; blocks are still 16×16 and a zip backup ships in the resources (§8) |
| Manual playtest: interactions "print colony status in chat"; spores are planted on soil (`docs/manual-playtest.md:5-7,28`) | Interactions open the UI instead; spores can be placed anywhere (§3.3, §3.5) |
| `mvp-architecture.md` defers a "full inventory-backed ScreenHandler market" (`:29-34`) | Still deferred. The market is a custom screen plus C2S intents, with no item-slot UI |

---

## 16. Worth keeping for a rebuild

**Concepts and content:**

- **Resource ↔ item mapping** for player help. There are 7 colony resources, each delivered as one clear item (wheat, raw iron, chitin shard, resin glob, fungus culture, venom sac, pheromone dust) with fixed conversion bundles. The table is simple and easy to read (§3.7).
- **Pheromone Token as the colony currency.** Earned only by helping (contracts) or selling to a colony; spent on colony wares and diplomacy. Colony Seal (a trust/treaty token) and War Banner (a hostility token) work well as themed diplomacy consumables (§3.6).
- **Hand-feeding ants**: royal jelly for the queen, food and chitin donations, and Pheromone Dust to "copy a caste's focus into the colony instinct". A tactile, low-UI way to steer the colony (§3.4).
- **The Chitin Bed farm loop** (spore → 4 growth stages → harvest shards with a chance of a spore, plus a research bonus) is a good renewable base, but it needs visible stages, a placement rule and a drop (§3.3).
- **Crafting tree shape**: shard → fiber → plate → mandible plate → armor and weapons, with resin, dust and venom as themed binders. Keep the tree, rebalance the counts (§5).
- **Armor numbers** as a starting point: Chitin sits between chainmail and iron with high enchantability; Resin Chitin sits between iron and diamond with 1 toughness. Add the vanilla armor and enchantable tags (§4).
- **Weapon identities**: the saber is raw damage, the spear adds poison. Rebuild them with real attribute modifiers, a `Weapon` component, tags, or the vanilla spear mechanics.

**UI:**

- **Information architecture**: a persistent section rail with 8 sections; a research *map* with prerequisite edges and a hover inspector; a two-column *market* (colony requests vs colony wares) with a context banner; *help cards* for requests showing delivery and reward; a four-button instinct selector; guide chapters with unlock rules; a relation list with a target picker and action buttons.
- **Visual language**: translucent graphite glass over the world, amber for focus and identity only, 2 px chamfered surfaces, and consistent colour codes for resources, castes and states, with item icons everywhere (§10.2).
- **Server-authoritative "snapshot + intent" networking**: the server sends a view model, the client sends only ids. Keep it, but add the colony id and a protocol version, authorise every request, send deltas or refresh live, and keep client state across updates (§11).

**Tooling and build:**

- The **dependency-free procedural texture generator** (4-tone palettes, double-density UV regions, deterministic noise). Useful for consistent, regenerable item and entity art, and extendable to blocks (§8.1).
- The **asset validator** (lang parity, mojibake check, reference checks, per-folder size contracts) and the item contact-sheet renderer. Extend it to cover recipes, tags, lang keys per registered object, loot tables and orphan detection (§8.3).
- **Build skeleton**: Loom split source sets, the Fabric API test source set with server GameTests, `validateAssets` wired into `check`, and CI that validates assets, builds and runs GameTests. Drop the machine-specific `gradle.properties` flag and the stray files.
- **EN/RU parity discipline**: 229 keys kept identical. Next time, localise server-generated text too, by sending translation keys with arguments instead of English strings.
