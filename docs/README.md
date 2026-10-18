# Documentation

## For players

- [Player guide](player-guide.md): finding, feeding and visiting colonies (English and Russian)
- [Changelog](../CHANGELOG.md)

## How the mod works

- [How a colony develops](colony-development.md): stages, chambers, mining, surface building, established colonies
- [Catch-up](catch-up.md): what happens to colonies while their chunks are unloaded

## For contributors

- [Development](development.md): toolchain, building, project layout
- [Testing](testing.md): test suites, commands, known flaky tests
- [Performance](performance.md): the tick budget, how it is measured, current numbers

## Design documents (in Russian)

- [00-decisions.md](00-decisions.md): the decisions behind the mod (realism, ants as the population, nests in the
  terrain, real species), with implementation notes in English
- [12-gdd-v2-civilization.md](12-gdd-v2-civilization.md): the game design ahead: stages and chambers, mining, the forge
  and armour, diplomacy, the player's place among colonies

Prime Ants is a restart of an earlier mod, *Formic Frontier*. Documents 01–09 review its design in detail. Its ideas
feed the roadmap; where they differ from 00 and 12, 00 and 12 win.

| Document | Topic |
|---|---|
| [01-vision.md](01-vision.md) | the concept, pillars, the player's role, the numbers |
| [02-castes-and-ants.md](02-castes-and-ants.md) | castes, colony growth, the queen, ant work and instincts, appearance |
| [03-resources-economy.md](03-resources-economy.md) | resources, income, logistics |
| [04-buildings-and-nest.md](04-buildings-and-nest.md) | buildings, blueprints, nest layout, wild colonies |
| [05-progression-research.md](05-progression-research.md) | ranks, stages, research, cultures, pacing |
| [06-diplomacy-trade-raids.md](06-diplomacy-trade-raids.md) | relations, reputation, trade, caravans, contracts, raids, events |
| [07-items-blocks-recipes.md](07-items-blocks-recipes.md) | items, blocks, armour, weapons, recipes |
| [08-ui-and-commands.md](08-ui-and-commands.md) | the colony tablet, commands |
| [09-visual-direction.md](09-visual-direction.md) | visual direction and references |

Also in this folder:

| Folder | Contents |
|---|---|
| `references/` | concept art and reference pictures |
| `original/` | the earlier mod's blueprints, recipes, translations and design notes |
| `raw-extraction/` | detailed notes on the earlier mod's code (in English) |
| `images/` | screenshots used in the README |

The earlier mod's code is on the [`archive`](https://github.com/SupremeSoviet/prime-ants/tree/archive) branch.
