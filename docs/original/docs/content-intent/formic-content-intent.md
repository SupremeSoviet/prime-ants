# Formic Content Intent

This pack is the gameplay/content contract for Formic Frontier. It runs as a
**parallel track** next to the visual baseline. The visual track owns how the
colony *looks*; the content track owns how the colony *plays and develops over
time*. Content work is accepted by deterministic gametests (and small targeted
screenshots when a feature has a UI), not by the world visual verdict.

## Relationship to the visual track

- The two tracks are independent backlogs. A content slice is NOT blocked by the
  world architecture still being visually `fail`.
- Hard rule: content work must not visually regress the world. `scripts/test-mod.cmd`
  must stay green, and content changes must not delete or weaken existing visual
  QA scenes, castes, or buildings.
- Mechanics are no longer globally locked behind a perfect world. Only
  world-architecture *visual* rows gate world-architecture *visual* changes.

## North Star

A living ant settlement that grows on its own over time: castes specialize,
resources flow, the colony researches upgrades, trades with and fights
neighbors, conducts politics, and unlocks new blocks and weapons. The player
guides and supplies a colony that would keep developing even if left alone.

## Content Pillars

Each pillar below is a backlog area in `content-feature-matrix.json`. Pick ONE
narrow row per content iteration, implement it, and prove it with a gametest.

### 1. Ant castes and roles (`caste_*`)
- Distinct castes with real behavior, not just models: worker, soldier, scout,
  miner, nurse/brood-tender, forager, builder, and at least one advanced/elite
  caste unlocked by research.
- Each caste has a job loop (gather/build/patrol/tend) that measurably changes
  colony state (resources, construction progress, defense).
- Caste population should shift with colony needs over time (auto-balancing).

### 2. Resources and economy (`resource_*`)
- Resource types: food, ore, chitin, resin, fungus, venom, knowledge (extend as
  needed). Each has a source, a store, a consumer, and a visible flow.
- Production and consumption tick over time so stockpiles rise and fall; scarcity
  drives caste/job reassignment.
- Storage buildings have capacity; overflow and shortage have consequences.

### 3. Time-based settlement development (`progression_*`)
- The colony advances through stages over ticks/days (founding -> growth ->
  established -> mature) with new buildings/castes/recipes unlocking per stage.
- Research tree (tablet) spends knowledge to unlock castes, blocks, weapons, and
  diplomacy options. Research must have prerequisites and real unlock effects.
- An autonomous colony with enough resources should advance a stage without
  player micromanagement, provably, in a gametest.

### 4. Trade (`trade_*`)
- Markets and caravans exchange resources with neighbor colonies at rates that
  depend on scarcity and relations.
- Player-facing requests/contracts: the colony asks for resources and rewards
  delivery (reputation, resources, unlocks).

### 5. Politics and diplomacy (`politics_*`)
- Relations (ally / neutral / rival) shift from actions: tribute, raids, broken
  contracts, shared enemies. Treaties, alliances, and rivalries have mechanical
  effects (trade rates, joint defense, raid risk).
- Recurring diplomatic events (treaty opportunities, tribute demands) drive the
  world without the player.

### 6. Blocks and materials (`block_*`)
- Native Formic blocks with gameplay roles: brood chamber, fungus bed, resin
  vat, storage cells, tunnel/earth materials, defensive walls. New blocks come
  from research/stages, not all at once.
- Blocks are used by generation AND craftable/placeable where it makes sense.

### 7. Weapons and defense (`weapon_*`)
- Ant-themed weapons/tools (mandible blades, venom sprayers, chitin armor) with
  real combat stats; soldier castes use them in raids/defense.
- Defensive structures and raid mechanics: attackers, walls, casualties,
  outcomes that change colony state.

## Content acceptance bar

A content row may be marked `pass` only when:

- A deterministic gametest exercises the feature end to end and asserts the
  state change (resource delta, caste reassignment, stage advance, relation
  change, combat outcome, unlock), and `scripts/test-mod.cmd` is green with that
  test included. Anthropic harness guidance: features are atomic and testable,
  and you only mark a feature passing after the test actually proves it.
- The feature is reachable in normal play (wired into colony tick / tablet /
  worldgen), not dead code.
- If the feature has UI, a targeted tablet screenshot proves labels/icons do not
  overlap and read in EN/RU.
- No existing gametest or visual scene regressed.

## Anti-patterns (content)

- Do not add a block/item/caste with no behavior and call it done.
- Do not weaken or delete a gametest to make a feature "pass".
- Do not jump pillars mid-slice; finish one row with its test before the next.
- Do not block on the world architecture being visually perfect; that is the
  other track.
