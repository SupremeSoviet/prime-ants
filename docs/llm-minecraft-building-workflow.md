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

The diplomacy shrine proves that an open-air role does not have to repeat the
market. `diplomacy_shrine_a.json` and `diplomacy_shrine_b.json` carve a compact
sanctum all the way through a taller mound mass, leaving three uneven ritual
horns around the sky opening. A narrow entrance replaces the market's broad
public mouth, while chiseled-tuff lamp plinths, paired gold/honey offerings,
candles, a shrine block and bell form a centered altar rather than trading
stalls. Copper, amethyst and amber accents remain deliberately sparse so the
structure stays an earthwork first. Unit tests require a connected ground ring,
all three elevated horn regions and an uninterrupted sky column; GameTests
verify the live furnishings and the two spaced variants.

The resin depot turns the same-floor overlap pattern into a low organic
cistern. `resin_depot_a.json` and `resin_depot_b.json` combine a broad workshop
shell with two offset storage lobes and a compact sealed crown. Their narrow
mouth leads past barrels, a chest, cauldron and workbench into an overlapping
side vault stocked with honey, honeycomb and warm task lighting. Sparse stripped
mangrove, copper and amber surface accents distinguish resin engineering from
the armory's dark mineral shell without covering the underlying earthwork.
Tests require a fully enclosed workshop and vault, a walkable shared throat,
distinct footprints and more than 42 blocks between repeat sites. Focused
Structure QA verifies both silhouettes and the furnished interior without
opening UI scenes.

The fungus garden uses a low clover plan rather than another circular storage
mound. `fungus_garden_a.json` and `fungus_garden_b.json` overlap a central hall
with uneven left, right, rear and entrance lobes, then close the broad shell
with a shallow cap. A five-block harvest mouth opens onto a clear central aisle;
mycelium and podzol beds, live red and brown mushrooms, mushroom-block planters,
shroomlights, a composter, barrel and culture block make the shaded chamber read
as an actively tended farm. Unit tests require all clover lobes, a sealed rear
shell, connected mass and distinct footprints, while GameTests verify the live
furnishings, broad mouth and two well-spaced variants. Focused Structure QA
checks both silhouettes and the interior at original screenshot resolution.

The venom press returns to a controlled narrow mouth but uses a compressed dark
machine capsule rather than a storage cistern. `venom_press_a.json` and
`venom_press_b.json` combine uneven jaw lobes, a rear reagent pod and a heavier
crown over an overlapping press hall and sealed side vault. The central aisle
ends at a cauldron press framed by blackstone supports, an iron chain and piston;
brewing stands, barrels, toxin vats, lime glass and restrained verdant light
fill the side work zones without blocking movement. Blackstone, mud and sparse
green surface accents distinguish hazardous production from the armory's forge
palette. Unit tests enforce connected asymmetric shells, full enclosure,
walkable room overlap and safe repeat spacing; GameTests verify the machine and
reagent furnishings in both variants. Focused Structure QA accepts all 30
regression and venom scenes at original resolution without opening UI scenes.

The watch post deliberately breaks the low role-mound profile. The three
`watch_post_*.json` variants are tall two-storey sentinels with different
asymmetric buttresses and crowns, a grounded guard room, a separate lookout
loft and an explicit one-block internal stair between them. The lower floor is
furnished for storage, equipment and target practice; the upper floor uses
mapping, observation and signal props, so the storeys do not read as copies.
Mud, tuff and dark deepslate keep the tower inside the colony palette while a
few iron-ore and ochre-light accents make its defensive role legible. The first
four posts use widely separated perimeter sites instead of crowding the colony
core. Unit tests enforce the height, connected shells, two distinct chambers,
stair headroom, variant diversity and perimeter spacing; GameTests verify both
furnished floors in all three variants. Focused Structure QA accepts all 33
structure scenes at original resolution, with the interior camera following
each variant's offset lookout rather than assuming a fixed room position.

`queen_mound_stage_2.json` implements `GREAT_MOUND` as an in-place evolution of
the existing centre rather than a second building. It retains the queen hall,
storage floor, lookout and their first two stairs, grows the landmark from 24
to 33 occupied blocks, adds a fourth crown room and connects it with a third
internal stair. Two unequal ground-level hillocks overlap the queen hall so
their larder and workshop are physically connected annexes with their own
facade mouths, not detached pods. The six rooms keep distinct identities:
royal living, food logistics, fabrication, storage, lookout and crown mapping/
signalling. The reinforced earth palette adds restrained mud brick, tuff,
copper and amethyst accents while preserving the nest-mound base material.
Unit tests enforce connected mass, taller-than-wide proportions, meaningful
growth over stage one, annex overlap and stair topology; GameTests exercise the
live in-place upgrade, furnishings and clear headroom. Focused Structure QA
accepts all 37 scenes at original resolution, including a direct stage-one/
stage-two comparison and the three added room roles.

`queen_vault.json` uses a separate compact subterranean IR because a protected
underground expansion has different invariants from an above-ground mound. The
file authors chamber roles, floor depths and directed stair topology; the
compiler owns ellipsoidal protective shells, tunnel envelopes, two-block
headroom and deterministic underground materials. A six-step descent from the
inherited queen hall reaches a guarded vestibule at `y=-6`; a second six-step
descent reaches the treasury at `y=-12`, whose carved edge overlaps a distinct
brood sanctuary on the same level. Placement excavates through ordinary stone,
granite and ores while preserving bedrock and block entities, and restores the
Great Mound surface block plus its queen core after the in-place project marker.
Unit tests enforce reachability, connected shell mass, chamber overlap and stair
headroom. GameTests exercise granite excavation, all twelve stairs, three
different furnishing identities and preservation of the evolved host mound.
Focused Structure QA now accepts all 41 scenes at original resolution, including
separate views for the descent, guard room, treasury and brood sanctuary.

`trade_hub.json` completes the structure catalog with a unique late-game
destination rather than a scaled copy of the starter market. Four overlapping
ground tiers form an asymmetric eight-block-high perimeter around a large
roofless exchange court. Two enclosed same-floor lobes overlap that court: the
bonded warehouse concentrates bulk cargo and storage against its outer walls,
while the brokerage uses maps, records, pheromone equipment, gold and amethyst.
Both interfaces keep two blocks of clear headroom and the court retains a broad
caravan aisle. The live site moved from the crowded old marker beside the market
to `(74, 0, -52)` relative to the colony, leaving at least 42 blocks to the
nearest market, venom press and first watch post. Unit tests enforce the larger
footprint, open roof, connected mass, chamber overlap and spacing. GameTests
verify the completed endgame project and every furnishing zone. Focused
Structure QA now accepts all 45 scenes at original resolution, with separate
exterior, court, warehouse and brokerage captures.

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
   `QUEEN_VAULT`, then author the separate late-game trade hub (all complete).

Each step must pass unit/GameTests before screenshot QA. Visual acceptance is
based on the rendered silhouette and entrances, not on how plausible the source
code looks.

During structure iteration, run `scripts/gui-smoke.cmd -Scope Structure`. It
captures only focused exteriors/interiors for the queen mound and implemented
role-building families; no UI screens are opened. The legacy full baseline
remains available with `-Scope Full` when a whole-mod release review is
explicitly needed. After a complete Structure run, a single changed scene can
be refreshed with `-Scenes <scene_id>`; existing screenshots are preserved and
the full Structure manifest is still revalidated. Structure runs reuse the
already loaded QA area to avoid fresh-chunk capture races, and the report gate
rejects suspiciously small sky-only PNGs.
