# Formic Frontier MVP Architecture

## Gameplay Slice

The MVP has grown into a playable colony slice. A Queen Egg creates a
player-allied colony with a visible mound, chamber buildings, resource nodes,
a queen, and several large ant castes. Debug commands can seed rival colonies
around the player. Colonies tick a server-authoritative economy, build queued
structures, farm chitin, trade with the player, accept direct item donations,
follow player-set priorities, advance through colony ranks, keep an event log,
spend pheromone goods on diplomacy, and resolve simple raids.

## Subsystems

- `sim`: pure Java economy model for resources, castes, upkeep, priority-driven growth, buildings, colony ranks, diplomacy actions, trade offers, raid plans, and persisted colony events.
- `world`: saved colony state, safe structure placement, autonomous construction, rival seeding, resource delivery, player orders, direct donations, paid diplomacy, and raid resolution.
- `entity`: one `AntEntity` with synchronized `AntCaste` and `colonyId` variants; hostile colonies can target each other, and allied ants react to useful hand-fed items.
- `registry`: Fabric block, item, armor, entity, and networking registration.
- `block`: interactable colony chambers and the growable `chitin_bed`.
- `client`: caste-specific entity textures and a tabbed Colony Tablet/Market status-trade-orders-diplomacy screen.
- `command`: debug API for creating, seeding rivals, inspecting, ticking, editing resources, trading, and spawning castes.

## Test Strategy

Unit tests target the pure simulation and codec round-trip. Server GameTests
target colony placement and seeded economy. Client testing is a manual smoke
check until headless client GameTests are stable enough to gate CI.

## Deferred Scope

World generation, forced chunk loading, real tunnel excavation, physical
building destruction during war, and a full inventory-backed ScreenHandler
market are intentionally deferred. The current market is server-authoritative
through C2S trade packets, but rendered with the existing tablet screen.

## Stage 3 Balance Notes

- Colony rank is derived, not manually assigned: completed buildings, population,
  reputation, queen health, and allied relations push colonies from Outpost to
  Burrow, Hive, and Citadel.
- Diplomacy is intentionally expensive. Envoys are early and modest; tribute
  needs rank and a crafted seal; truce is Hive-gated; war pacts are Citadel-gated.
- Chitin armor now uses `chitin_plate`, which itself needs fiber and resin. This
  makes armor a mid-game goal rather than the first thing crafted from raw shards.
- The imagegen reference sheet is stored at `docs/imagegen-ant-colony-stage3-reference.png`;
  generated Minecraft PNGs remain small, readable, and deterministic.
