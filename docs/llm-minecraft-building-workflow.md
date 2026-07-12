# LLM workflow for Minecraft structures

## Investigation result

An LLM should not directly author thousands of block coordinates or one large
imperative geometry method. The reliable workflow is:

1. Turn the user's visual intent into a small, constrained intermediate
   representation.
2. Compile that representation with deterministic, reusable geometry code.
3. Reject invalid bounds, disconnected parts, illegal materials and unsupported
   dimensions before touching the world.
4. Test structural invariants automatically.
5. Execute in Minecraft, capture several fixed camera angles and feed the actual
   result back into the next small iteration.

This combines four useful findings:

- [T2BM](https://arxiv.org/abs/2406.08751) uses prompt refinement, an
  intermediate building representation and a repair pass instead of asking the
  model to control Minecraft blocks directly.
- [VoxelCodeBench](https://arxiv.org/abs/2604.02580) finds that executable code
  is much easier for models than spatially correct 3D composition; its pipeline
  executes generated code and captures multiple screenshots for evaluation.
- [Voyager](https://arxiv.org/abs/2305.16291) uses reusable code skills and
  iterative environment feedback, execution errors and self-verification rather
  than relying on a one-shot program.
- [Language to Subtask Builder](https://arxiv.org/abs/2211.00688) converts a
  language goal into ordered, consistently achievable voxel subgoals.

## Formic Frontier decision

The LLM edits JSON blueprints. Java owns parsing, validation, material selection,
block placement and carving. A blueprint uses a few semantic primitives:

- `tiers`: overlapping tapered ellipses with height, bottom/top radii and an
  offset. Adding a higher tier is the primary vertical growth operation;
  overlapping several tiers at the same base height produces asymmetrical
  single-storey lobes without introducing block-coordinate dumps.
- `palette`: a compiler-owned semantic material family. JSON selects a bounded
  name such as `earth` or `food_store`; it cannot inject arbitrary block ids.
- `terraces`: shallow earth shelves anchored into the stable tier core. They
  make individual inhabited floors visible without turning them into separate
  buildings.
- `chambers`: vaulted rooms with stable ids and purposes. The compiler selects
  furnishing sets, so `queen_hall`, `storage` and `lookout` do not become copies
  of one generic room. An explicit `openToSky` flag turns the same validated
  footprint into a full-height courtyard instead of relying on an accidental
  hole in an otherwise enclosed roof.
- `pits`: optional shallow stepped excavations owned by a chamber. Their
  ellipsoid footprint and bounded depth are validated against the room, the
  chamber floor is not refilled over the opening, and the compiler lines each
  step before placing any role-specific node at the bottom.
- `connections`: validated stair passages between chamber ids. The compiler
  derives the rise from the two floor heights, carves two blocks of headroom and
  orients every stair in the declared cardinal direction.
- `mouths`: bounded south-facing carved volumes with a dark rear wall.
- A role-building family registers one or more validated blueprint resources
  plus its functional interaction block. A simple stable position hash selects
  a variant, so repeated buildings keep their shape across reloads without
  becoming exact clones.

The first blueprint is
`src/main/resources/formic_blueprints/queen_mound_stage_1.json`. Its contract is
a 24-block-tall, at-most-21-block-wide landmark with four offset tiers, two
attached floor terraces, three facade mouths, three purpose-specific interior
rooms and two internal stair passages. It intentionally has no side lobes yet.

The first reusable single-storey family is the food store:
`food_store_a.json` and `food_store_b.json`. Both are seven blocks tall, use
three overlapping low lobes, contain one carved granary with a clear central
aisle, and furnish the walls with storage, food processing and warm light. The
two footprints are structurally distinct and the second colony site is placed
far enough away to preserve open ground between them.

The nursery follows the same compiler contract without copying the granary. Its
two `nursery_a.json` / `nursery_b.json` variants are fuller eight-block brood
domes with offset side lobes, a warmer mud/packed-mud shell palette, paired
chitin resting alcoves, incubation materials and a clear central aisle. Repeated
nurseries move diagonally away from the first site, and diplomacy caches are
offset from the colony-to-colony axis so the enlarged role mound cannot swallow
them.

The remaining starter-economy pair deliberately shares a low mound language
without sharing topology. `mine_a.json` / `mine_b.json` contain a two-block-deep
stepped ore pit, mineral-streaked shell palettes, storage and work lights. The
three `chitin_farm_*.json` variants keep a continuous packed-mud cultivation
floor and instead use chitin beds, bone, honeycomb and a composter. Unit tests
verify connected shells and distinct footprints; GameTests verify the compiled
pit depth, furnishings, crowns and repeat-site spacing.

The barracks family demonstrates that the same vocabulary is not limited to
round economy domes. Two overlapping-lobe blueprints form long capsule-like
footprints with a five-block entrance, fortified mud/tuff palette and a broad
troop hall. Four side resting alcoves leave the central aisle clear, while an
anvil, smithing table and target distinguish the rear training wall. Repeated
sites alternate variants and move more than 32 blocks apart.

The market family is the first intentional open-air use of the vocabulary.
`market_a.json` and `market_b.json` compile to low asymmetrical earth banks
around a roofless packed-mud yard with a five-block public entrance, storage,
produce, a bell and paired lantern posts. Copper and honeycomb accents make the
role readable without turning it into a conventional roofed house. Repeat sites
alternate both footprints and remain more than 31 blocks apart.

The pheromone archive extends the same data-first grammar vertically.
`pheromone_archive_a.json` and `pheromone_archive_b.json` describe compact
two-storey mounds with independent `archive_hall` and `archive_loft` chambers,
separate facade mouths and mirrored internal mud-brick stairs. The lower hall
uses storage, a lectern and catalog blocks; the upper loft uses bookshelves,
amethyst memory accents and quieter lighting. Tests treat the stair as a
traversable contract instead of decoration and verify that both floors remain
enclosed, connected and differently furnished. Repeated archives alternate the
two silhouettes and retain open ground around neighboring colony buildings.

The armory uses the grammar for connected rooms on one floor. `armory_a.json`
and `armory_b.json` pair a broad forge hall with an overlapping side weapon
vault behind a narrow three-block entrance. A stepped brow, heavy side lobes
and dark deepslate/blackstone/ore veins make it read as protected without
turning it into a conventional stone bunker. The forge contains an anvil,
smithing table, blast furnace and grindstone; the vault carries storage,
metal stock and a target rack. Besides whole-mound connectivity, tests walk the
discrete boundary of both carved chambers and reject any opening that is not an
authored mouth. This caught and removed a rear-side sky hole before visual
acceptance. Repeated sites alternate mirrored layouts and stay in a separate
outer district away from the barracks.

Every enclosed role chamber has a discrete rear-shell invariant in addition to
whole-mound connectivity. For each vaulted ceiling layer, the first block behind
the carved ellipse must remain solid. This prevents a geometrically connected
mound from accidentally exposing one-block windows to the sky. Complex or
offset chambers additionally validate their complete discrete boundary against
un-authored openings, while the market opts into its opening deliberately and
validates that it reaches the mound top.

## Iteration order

1. Establish the shared blueprint vocabulary with the main mound.
2. Add one role-building family at a time, including its palette, chamber
   purpose, furnishings, repeated-site variation and spacing contract. When a
   role needs multiple floors, encode the rooms and their traversable connection
   explicitly in the blueprint rather than stacking disconnected shells.
3. Finish the starter economy in order: food store, nursery, mine, chitin farm.
4. Continue through infrastructure, research and defense families.
5. Return to the central mound for `GREAT_MOUND` height/lobes and the underground
   `QUEEN_VAULT`, then author the separate late-game trade hub.

Each step must pass unit/GameTests before screenshot QA. Visual acceptance is
based on the rendered silhouette and entrances, not on how plausible the source
code looks.

During structure iteration, run `scripts/gui-smoke.cmd -Scope Structure`. It
captures only focused exteriors/interiors for the queen mound and implemented
role-building families; no UI screens are opened. The legacy full baseline
remains available with `-Scope Full` when a whole-mod release review is
explicitly needed.
