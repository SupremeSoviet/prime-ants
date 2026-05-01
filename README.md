# Formic Frontier

Fabric MVP for playable large-ant colonies with visible nests, chamber economy,
large caste variants, autonomous construction, rank progression, pheromone
diplomacy, trading, simple raids, colony cultures, research, logistics requests,
debug tooling, and automated tests.

## Requirements

- Java 21 or newer for running the mod; Temurin JDK 25 is used locally for builds
- Gradle Wrapper (`./gradlew`) with Gradle 9.4.1
- Minecraft/Fabric target: 1.21.11, Fabric Loader 0.19.2, Fabric API 0.141.3+1.21.11

The current workspace did not have Java or Gradle installed when the project was
created, so the wrapper metadata is committed and builds should be run after a
JDK and a valid `gradle-wrapper.jar` are present.

## MVP Controls

- `/formic colony create` creates one test colony at the command source.
- `/formic colony dump` prints colony resources, castes, chambers, and priorities.
- `/formic colony tick <n>` advances the colony economy by `n` simulation ticks.
- `/formic colony resource set <food|ore|chitin|resin|fungus|venom|knowledge> <amount>` edits resources.
- `/formic colony instinct <food|ore|chitin|defense>` changes the top autonomous colony instinct.
- `/formic colony priority <food|ore|chitin|defense>` remains as a compatibility alias.
- `/formic colony seed-rivals <count> <distance>` creates rival colonies around the player.
- `/formic trade <offer>` executes a nearby market trade.
- `/formic research <node>` starts a nearby colony research if its archive, resources, and prerequisites are ready.
- `/formic diplomacy <envoy|tribute|truce|incite|war_pact>` spends pheromone goods to shift relations.
- `/formic ant spawn <caste>` spawns a large ant variant.
- `Queen Egg` creates a colony when used on a block.
- `Colony Tablet` opens a synced tabbed status screen with Overview, Instinct, Buildings, Requests, Research, Trade, Diplomacy, Relations, and Events.
- `Pheromone Dust` used on an ant copies that caste's work focus into the colony instinct.
- `Raw Biomass`, wheat, chitin shards, and royal jelly can be hand-fed to ants for small colony effects.
- `Colony Seal`, `War Banner`, `Resin Glob`, `Chitin Plate`, fungus goods, venom sacs, mandible plates, royal wax, resin chitin armor, mandible saber, and venom spear form the expanded crafting/trading chain.
- Cultures: allied colonies start as Amber; rival colonies rotate through Leafcutter, Fire, and Carpenter.
- Research nodes: `chitin_cultivation`, `resin_masonry`, `fungus_symbiosis`, `venom_drills`, `mandible_plating`, `scented_ledger`, `treaty_sigils`.

## Texture Direction

Stage 3 uses an AI-generated ant-colony pixel-art reference sheet as the art
target, then bakes Minecraft-compatible 16x16 textures through the local
texture generator. The reference is stored in `docs/imagegen-ant-colony-stage3-reference.png`.

## Development

```bash
./gradlew build
./gradlew runClient
```

Server GameTests are wired into `build`; client smoke tests are documented in
`docs/manual-playtest.md`.

## Autonomous QA

For Codex-driven development, run:

```powershell
.\scripts\doctor.cmd
.\scripts\test-mod.cmd
.\scripts\prepare-gui-world.cmd
.\scripts\gui-smoke.cmd
```

`doctor.cmd` requires Java 21+ on `PATH`; the current project target remains
Java 21. Visual QA scenes are available through `/formic qa scene <name>` and
are described in `docs/autonomous-dev.md`.
