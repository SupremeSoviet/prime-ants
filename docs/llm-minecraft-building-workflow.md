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
  offset. Adding a higher tier is the primary vertical growth operation.
- `terraces`: shallow earth shelves anchored into the stable tier core. They
  make individual inhabited floors visible without turning them into separate
  buildings.
- `chambers`: vaulted rooms with stable ids and purposes. The compiler selects
  furnishing sets, so `queen_hall`, `storage` and `lookout` do not become copies
  of one generic room.
- `connections`: validated stair passages between chamber ids. The compiler
  derives the rise from the two floor heights, carves two blocks of headroom and
  orients every stair in the declared cardinal direction.
- `mouths`: bounded south-facing carved volumes with a dark rear wall.
- Later schemas can add `lobes` as attached hill volumes without changing the
  tier compiler.

The first blueprint is
`src/main/resources/formic_blueprints/queen_mound_stage_1.json`. Its contract is
a 24-block-tall, at-most-21-block-wide landmark with four offset tiers, two
attached floor terraces, three facade mouths, three purpose-specific interior
rooms and two internal stair passages. It intentionally has no side lobes yet.

## Iteration order

1. Main mass and vertical tier rhythm.
2. Entrance readability, furnished interior rooms and playable stairs between
   every floor.
3. Stage-two height growth.
4. One attached side hill with a tested connection.
5. Additional role-specific attachments, one at a time.

Each step must pass unit/GameTests before screenshot QA. Visual acceptance is
based on the rendered silhouette and entrances, not on how plausible the source
code looks.

During structure iteration, run `scripts/gui-smoke.cmd -Scope Structure`. It
captures only the front, three-quarter and three floor-interior scenes; no UI
screens are opened. The legacy full baseline remains available with
`-Scope Full` when a whole-mod release review is explicitly needed.
