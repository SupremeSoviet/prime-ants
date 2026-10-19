# Changelog

All notable changes to Prime Ants are listed here. Versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Live shell validation regression covering solid dry, open, fluid and waterlogged blocks, fault order, unavailable
  cells and repairs within the same tick.

### Changed

- Corrected benchmark descriptions to reflect shared-machine measurements and the limits of nursing-phase activity.

### Fixed

- Surface construction jobs now save consistently when several colonies share a world.
- Supported dry torches, soul torches and lanterns now preserve the colony's existing openings, including wall
  torches and standing or hanging lanterns. Removing a light leaves the original opening usable. Ants still collide
  with lanterns. Lighting damage already saved by 0.2.0 cannot be repaired automatically; lost ownership cannot
  safely be distinguished from a player's edits.
- A wall torch in a worker-dug widening of a generated nest no longer makes the colony treat unavailable support
  as a broken wall. Independently visible nest damage still stops development.

## [0.2.0] - 2026-10-18

Colonies now grow past their first chambers: they move through four stages, dig and upgrade chambers, mine, build on
the surface and fortify, and they also exist ready-made in new terrain.

### Added

- **Colony stages.** Founding, Young, Mature and Great, with caps of 5, 30, 60 and 120 adults. A colony moves up
  when it has enough adults, the chambers it needs and enough stored food and materials. It drops back when it
  loses them.
- **Chambers with a purpose.** Nursery, food store, material store and queen's hall. Workers dig them, then rebuild
  them in packed clay (tier two) and stone (tier three). Better chambers hold more and speed up brood development
  and laying.
- **Mining.** Mature colonies send a miner to dig a gallery of up to 16 blocks in natural ground on their own
  territory. Stone, clay, gravel and sand are carried home one piece at a time, and coal, copper and iron ore are
  stockpiled for later. Blocks placed by players are never mined.
- **Surface building.** The soil mound grows with the colony. Mature colonies build a crest and an open arch, and
  Great colonies add fortifications: a gate, ramparts and two watch posts.
- **Established colonies.** Young and Mature colonies with dug-out nests now appear in newly generated terrain,
  next to founding queens.
- **Catch-up.** A colony whose chunks were unloaded catches up on the missed time when they load again:
  - adults age;
  - food is shared out, the hungriest ants first and protein to the larvae;
  - brood develops as fast as the nurses can manage;
  - the colony can change stage.
- **Material drop-offs.** Clay, stone, gravel, sand and ore dropped near a trail are carried to the material store.

### Changed

- The colony cap rose from 30 adults to 120 at the Great stage.

### Known issues

- Colonies rely on players for clay and stone. Wild colonies cannot reach Mature without dropped clay, Mature
  colonies need 40–44 clay for their upgrades, and stone chambers need smooth stone, which mining does not produce.
- Large colonies are expensive for the server. On a Ryzen 7 7700, one Great colony (120 adults) runs at about
  19–20 TPS with little headroom, and three Great colonies close together run at about 7 TPS.
- Awkward terrain, food shortages and crowding can stall a colony.
- Long sessions and multiplayer have had limited testing.

## [0.1.0] - 2026-10-06

The first early release: wild *Lasius niger* colonies that found nests, raise brood and forage.

### Added

- **Founding queens.** They appear in plains, meadows and flower forests, dig stairs and a chamber into natural
  ground, carry the soil out into a mound and raise the first clutch from their own reserves.
- **The brood cycle.** Egg → larva → cocoon → pale young worker → adult, about ten minutes per stage at normal speed.
- **Workers.** They:
  - open the entrance;
  - forage nectar from flowers, small prey in the grass and food that players drop;
  - keep food in a visible store;
  - nurse the brood and the queen, and pass sugar mouth to mouth;
  - enlarge the nest.
- **Growth that follows food.** Laying depends on what the colony actually eats. Ants need meals, age and die, and
  a dead queen lays no more eggs. A colony holds up to 30 adults.
- **Defence.** Hurting an ant or breaking the nest makes nearby workers bite, half a heart per bite.
- **Visiting.** Players can walk into a nest and light it with a torch.

[Unreleased]: https://github.com/SupremeSoviet/prime-ants/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/SupremeSoviet/prime-ants/releases/tag/v0.2.0
[0.1.0]: https://github.com/SupremeSoviet/prime-ants/releases/tag/v0.1.0
