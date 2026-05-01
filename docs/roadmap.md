# Formic Frontier Roadmap: Living Ant Village / Живая Муравьиная Деревня

This roadmap turns Formic Frontier into a Millenaire-inspired ant colony mod:
the player can develop an allied colony, but the colony should still feel like
an autonomous living settlement with its own work, requests, culture, and growth.

Every roadmap slice must pass:

- `scripts/test-mod.cmd -AllowMissingGitHub`
- `scripts/gui-smoke.cmd`
- `$formic-visual-assessment` on the latest screenshots
- `scripts/autonomous-gate.cmd -AllowMissingGitHub -NoLaunch`

`P0` and `P1` visual findings block the slice. `PASS` and `PASS WITH NOTES` are
acceptable only when remaining findings are `P2`/`P3` polish debt.

## Active Renovation Track: Big Ant Village And Colony Tablet 2.0

This focused track supersedes the generic stage order until it is done. The goal
is a spacious, readable, Millenaire-like ant village plus a softer, clearer
colony UI.

Slice R1: Settlement scale renovation.

- Player promise: the colony reads as a roomy village-scale ant settlement.
- Implementation notes: starter chambers and advanced buildings occupy roughly
  3-4x the old area through about 1.7-2x linear scale, with wider layout rings,
  large mounds, broad yards, and visible connecting trails.
- Tests: `test-mod`, structure placement checks, visual manifest.
- Visual QA coverage: `colony_overview`, `colony_ground`, `progression_scene`,
  `settlement_scale`.
- Done criteria: no tiny/puppet settlement read, no box campus, no empty
  stretched layout.

Slice R2: Architecture polish.

- Player promise: each building has an organic role-specific silhouette.
- Implementation notes: add mound ribs, tunnel mouths, yards, material markers,
  storage props, brood/fungus/market/archive accents, and non-flat edges.
- Tests: placement tests for key markers and `gui-smoke`.
- Visual QA coverage: `settlement_scale`, `culture_styles`,
  `construction_stage`, `repair_scene`.
- Done criteria: nursery, food, mine, barracks, market, archive, resin, fungus,
  and defense structures are distinguishable from normal gameplay distance.

Slice R3: Colony Tablet 2.0.

- Player promise: the tablet feels like a living colony journal, not a 2000s RTS
  ledger.
- Implementation notes: use a larger adaptive panel, item/resource icons,
  request cards, market cards, and a research node map. Keep all text inside the
  screen bounds and avoid raw ids.
- Tests: snapshot tests where useful, localization validation, `gui-smoke`.
- Visual QA coverage: `tablet_en`, `tablet_ru`, `tablet_guide`,
  `tablet_trade`, `tablet_research_map`, `tablet_market`, `tablet_requests`.
- Done criteria: research is clearly a map, trade is clearly a market, requests
  are player-facing help cards, and no P0/P1 visual findings remain.

## Slice Format For Autonomous Work

Each roadmap item should be implemented as a playable slice, not as an invisible
subsystem-only task.

- Player promise: what the player can see or do after the slice.
- Implementation notes: the smallest code/content change that delivers it.
- Tests: unit/GameTest/build checks required for the slice.
- Visual QA coverage: existing or new `/formic qa scene <name>` coverage.
- Done criteria: player-facing proof and visual assessment threshold.

## Stage 1: Playable Visual Baseline

Goal: the mod looks playable, calm, and readable.

Slice 1.1: Tablet readability baseline.

- Player promise: the Colony Tablet can be scanned without zooming.
- Implementation notes: compact labels, shorter helper copy, translated rank
  labels, no raw ids in overview/build summaries.
- Tests: `test-mod`, localization validation, `gui-smoke`.
- Visual QA coverage: `tablet_en`, `tablet_ru`.
- Done criteria: no clipped critical text, no mojibake, no raw ids.

Slice 1.2: Caste and overview inspection baseline.

- Player promise: ants and colony composition are inspectable in QA screenshots.
- Implementation notes: closer `ant_lineup` camera, cleanup old spawned ants,
  all caste names visible, tighter `colony_overview` framing.
- Tests: `gui-smoke`, visual manifest.
- Visual QA coverage: `ant_lineup`, `colony_overview`.
- Done criteria: all castes are visible and the overview shows hub, paths,
  buildings, and active colony context.

Acceptance:

- A player understands current task, active build, resources, population, and
  instinct priorities within five seconds.
- Visual assessment has no `P0`/`P1`; any remaining Stage 1 debt is no worse
  than minor polish.

## Stage 2: Ant Hill Architecture And Textures

Goal: colonies look like original ant hills, not box campuses.

Slice 2.1: Organic starter mound.

- Player promise: the first colony reads as an ant hill, not a square building.
- Implementation notes: replace the central rectangular shell with layered dirt,
  root, tunnel, and mound forms while keeping block-grid readability.
- Tests: structure placement tests and `gui-smoke`.
- Visual QA coverage: `colony_overview`, `colony_ground`.
- Done criteria: grounded non-boxy central mound with readable entrances.

Slice 2.2: Building visual stages.

- Player promise: planned, building, complete, upgraded, damaged, and repairing
  states are visually distinct.
- Implementation notes: introduce `BuildingVisualStage` and map colony building
  lifecycle state to structure variants or overlays.
- Tests: lifecycle serialization tests, placement tests.
- Visual QA coverage: add `construction_stage`, later `repair_scene`.
- Done criteria: every stage can be captured in deterministic QA scenes.

Slice 2.3: Culture architecture pass.

- Player promise: cultures are distinguishable before opening UI.
- Implementation notes: give cultures distinct architecture:
  - Amber: warm clay, resin, diplomacy accents.
  - Leafcutter: fungus gardens, leaf trails, green organic surfaces.
  - Fire: darker clay, warning colors, military silhouettes.
  - Carpenter: wood/resin engineering, root-like supports.
- Tests: asset validation and culture structure placement tests.
- Visual QA coverage: add `culture_styles`.
- Done criteria: four cultures are recognizable in one screenshot.

Acceptance:

- `colony_overview`, `colony_ground`, and `progression_scene` show a spacious,
  grounded, non-boxy ant settlement.
- Each key building type is recognizable from normal gameplay distance.

## Stage 3: Visible Work And Player Guide

Goal: the player can see who is doing what and why.

Slice 3.1: Work-state legibility.

- Player promise: the player can tell what at least three ants are doing from
  normal gameplay distance.
- Implementation notes: add `ColonyWorkTask` states, carried item visuals,
  particles or small task markers, and caste-specific idle/work poses where
  feasible.
- Tests: work task assignment tests and `gui-smoke`.
- Visual QA coverage: add `work_cycle`.
- Done criteria: resource, logistics, and patrol or construction work are visible
  without debug text.

Slice 3.2: Player guide book.

- Player promise: the player has an in-game manual for colony basics.
- Implementation notes: add `Formic Field Guide` or `Colony Codex`, chapters,
  progress unlock hooks, and short localized copy.
- Tests: guide unlock tests and localization validation.
- Visual QA coverage: add a tablet/book screenshot once the guide UI exists.
- Done criteria: chapters cover castes, resources, buildings, cultures,
  relations, research, and first help actions.

Acceptance:

- `colony_ground` and `work_cycle` show at least three different ant jobs without
  relying on debug text.
- The guide teaches the basics of castes, resources, buildings, cultures,
  relations, and research.

## Stage 4: Autonomous Construction, Upgrades, Repair

Goal: colonies visibly build, improve, break, and recover.

Slice 4.1: Observable construction loop.

- Player promise: a construction site appears, receives materials, changes form,
  and becomes a completed building.
- Implementation notes: connect building requests to visible staged structures
  and worker delivery tasks.
- Tests: save/load lifecycle tests, worker delivery tests.
- Visual QA coverage: `construction_stage`.
- Done criteria: the sequence is observable and persistent.

Slice 4.2: Upgrades and repair loop.

- Player promise: old buildings can improve or become damaged, and workers repair
  them when resources exist.
- Implementation notes: add upgrade rules, damaged visual state, repair tasks,
  and resource costs.
- Tests: upgrade unlock tests, repair persistence tests.
- Visual QA coverage: `repair_scene`.
- Done criteria: damage and repair are visible and survive save/load.

Acceptance:

- A player can watch a building appear, receive materials, change form, finish,
  become damaged, and get repaired.
- Lifecycle state persists across save/load.

## Stage 5: Millenaire-Style Colony Life

Goal: the colony becomes a settlement the player can befriend.

Slice 5.1: Requests and contracts.

- Player promise: the colony asks for help and rewards the player.
- Implementation notes: add `ColonyContract` with cost, priority, reward,
  reputation impact, and visible response.
- Tests: contract lifecycle tests, reputation tests.
- Visual QA coverage: tablet request scene or contract board scene.
- Done criteria: player can help without directly commanding ants.

Slice 5.2: Named living colonies.

- Player promise: colonies have names, personality, culture, current goal, and
  relationship status.
- Implementation notes: generate colony identity and surface it in UI/world
  labels.
- Tests: identity serialization tests.
- Visual QA coverage: tablet relation scene.
- Done criteria: the colony feels like a village with a state, not a machine.

Slice 5.3: Allies, rivals, and caravans.

- Player promise: other colonies can trade, compete, threaten, or cooperate.
- Implementation notes: add simple colony-to-colony events and visible caravan
  or raid markers.
- Tests: diplomacy event tests.
- Visual QA coverage: later `diplomacy_scene`.
- Done criteria: at least one visible non-player colony interaction exists.

Acceptance:

- The player can help without direct command and see the colony respond through
  faster construction, better trade, stronger relations, or new opportunities.

## Stage 6: World Integration And Culture Expansion

Goal: colonies live in the world, not only in debug scenes.

Slice 6.1: Survival discovery.

- Player promise: colonies can be found naturally in survival.
- Implementation notes: add controlled worldgen or encounter spawning for wild
  and cultural colonies.
- Tests: worldgen placement tests and seed stability checks.
- Visual QA coverage: add `worldgen_encounter` once deterministic.
- Done criteria: a player can find a colony without debug commands.

Slice 6.2: Culture progression paths.

- Player promise: culture choice changes economy and behavior.
- Implementation notes: add culture-specific progression paths:
  - Leafcutter: fungus economy and leaf harvesting.
  - Fire: raids, defense pressure, aggressive borders.
  - Carpenter: resin/wood engineering and repairs.
  - Amber: diplomacy, trade, and stable alliance growth.
- Tests: culture progression tests.
- Visual QA coverage: `culture_styles`, later `worldgen_encounter`.
- Done criteria: cultures look and play differently.

Slice 6.3: Surface landmarks.

- Player promise: the world tells ant stories before the UI opens.
- Implementation notes: add trails, foraging zones, ruined nests, and rival
  borders.
- Tests: landmark placement tests.
- Visual QA coverage: `worldgen_encounter`.
- Done criteria: landmarks guide player attention and interaction.

Acceptance:

- In survival, the player can discover colonies naturally, identify their
  culture visually, and understand a first interaction path.

## Stage 7: Long-Term Game Loop

Goal: the player has reasons to return for many in-game days.

Slice 7.1: Recurring colony events.

- Player promise: colonies create new reasons to return.
- Implementation notes: add famine, invasion, queen brood, migration, treaty,
  and expansion events.
- Tests: event scheduling and persistence tests.
- Visual QA coverage: event-specific scenes when visuals exist.
- Done criteria: events produce visible world or UI consequences.

Slice 7.2: Advanced diplomacy.

- Player promise: alliances, tribute, defensive pacts, and colony wars have
  visible consequences.
- Implementation notes: extend reputation and colony-to-colony relations.
- Tests: diplomacy state tests.
- Visual QA coverage: `diplomacy_scene`.
- Done criteria: diplomacy changes what the player sees and can do.

Slice 7.3: Endgame colony projects.

- Player promise: a mature colony builds wonders worth protecting.
- Implementation notes: add great mound, pheromone archive network, underground
  queen vault, and trade hub projects.
- Tests: project unlock and completion tests.
- Visual QA coverage: `endgame_project`.
- Done criteria: the colony has a long progression fantasy after starter builds.

Acceptance:

- The colony has a long progression fantasy and does not feel complete after the
  starter buildings.

## Interface Concepts

- `BuildingVisualStage`: planned, construction, complete, upgraded, damaged,
  repairing.
- `ColonyWorkTask`: resource, logistics, construction, repair, patrol, forage,
  trade.
- `GuideChapter`: unlockable book chapter tied to colony milestones.
- `ColonyContract`: player-facing request with cost, priority, reward, and
  reputation impact.

Preserve current `/formic qa scene <name>` behavior and add new scenes without
removing the existing six baseline screenshots.

Expected future visual QA scenes:

- `work_cycle`
- `construction_stage`
- `culture_styles`
- `repair_scene`
- `diplomacy_scene`
- `worldgen_encounter`
- `endgame_project`
