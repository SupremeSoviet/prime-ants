# Formic Visual Intent

This pack is the art-direction contract for Formic Frontier visual work. The
reference images are intent, not a shader requirement. The mod can stay
Minecraft-native, but the screenshots must move toward the same scale, shape,
life density, and insect realism.

## Reference Slots

Place the user-provided reference images here when binary attachments are
available:

- `reference-forest-foraging.png`: forest floor with realistic ants foraging
  through grass, flowers, roots, and dirt.
- `reference-mega-nest-wide.png`: a huge organic ant-hill slope with many
  chambers, entrances, ants, and warm earthen mass.
- `reference-mega-nest-front.png`: front view of a large ant colony facade
  with tunnel mouths, terraces, chambers, and visible ant traffic.

If these PNG files are missing, this Markdown file and
`reference-manifest.json` are still the authoritative intent.

Current status: the three PNG references are present locally and must be sent to
the image-capable assessor before Minecraft screenshots.

## North Star

The settlement should read as a real ant colony carved into and built out of
earth. It should feel large enough to house a living colony, not like small
decorative huts on a flat test field. The world should suggest damp soil,
roots, grass, leaf litter, tunnels, chambers, and ant traffic.

The desired direction is:

- Monumental earthen ant-hill architecture with a broad base and a tall,
  tapered silhouette.
- Multiple large organic chambers and tunnel mouths visible at once.
- Several role-specific landmarks around the main mound, not one lonely tower.
- Organic, ant-like role buildings: asymmetrical mound/chamber silhouettes,
  readable spacing between buildings, and entrance cuts that feel excavated,
  not architectural arches.
- Ants that read as insects: segmented bodies, visible legs, antennae, caste
  scale differences, and grounded posture.
- A native Formic material language: custom blocks, textures, models, and
  structure palettes that belong to this mod.
- Screenshots framed to show the subject clearly, including height and base.

## Latest player feedback (2026-06-27) — fix these concretely

The player looked at the live colony. Buildings are starting to read as
ant-hills, but there is still obvious breakage. These are first-class blockers:

1. Roofless square boxes. Some buildings (the player reads them as the
   resource/storage buildings) render as open-topped square boxes with no roof
   or crown — a hollow box, not a covered earthen chamber. Every role building
   must be CLOSED on top with an organic crown/dome; no open-topped boxes, no
   flat square walls without a roof. (Track under `organic_asymmetric_ant_buildings`
   / `multiple_large_organic_chambers`.)
2. Ugly tiled custom texture. The square box is wrapped in a custom block
   texture that reads as a repeating/honeycomb-like pattern and looks bad. Redraw
   that block's texture as a clean native Formic earth/resin/chamber material at
   32x32 and stop using it as a large flat wall surface. (Track under
   `holey_block_texture_redraw` + `formic_textures_32x32`.)
3. Tablet menus are non-functional and confusing. The progress/research and
   interaction screens are "just buttons that do not clearly do anything." The
   tablet must be both BEAUTIFUL and actually FUNCTIONAL: every control must have
   a clear, visible effect (open a screen, spend resources, start research,
   accept a request), labels/icons must read in EN/RU with no overlap, and the
   research view must read as a real interactive tree, not a row of dead buttons.
   This is now a required interaction+visual target, not just a beauty pass.
   (Track under `tablet_visual_hierarchy`; functionality is verified by a tablet
   interaction gametest on the content track.)

## Current Architecture Target

The active visual blocker is the building family. Do not spend the next loop on
forest-floor cleanup. Do not spend it on broad material migration. The next
world worker should make the buildings more ant-like and less symmetrical.

### Why the last several attempts failed (read this first)

Past attempts kept the same wrong *representation* and only tuned its
parameters: a cluster of separate, steep, tapered cones/pyramids, each built
from visible stacked block steps, with chamber holes punched in near-regular
rows. The assessor kept returning the same "stepped towers / tower cluster"
verdict, and each retry only nudged a cone center by a few blocks. That is a
local minimum.

The fix is NOT "make the asymmetry bolder" or "offset the cone more." The fix is
a representational change. If you find yourself adjusting a cone radius, a taper
slope, or a center offset, stop — you are repeating the failing approach.

### Required representation (the recipe, not adjectives)

Build the colony as ONE continuous, broad, low earthen mound landmass that the
whole settlement is carved into and grows out of — not as N free-standing
buildings on a flat field. Concretely, the generator should aim for:

- A single base heightmap driven by low-frequency value/Perlin noise over a wide
  footprint (think one big hill, ~40-70 blocks across), so the silhouette is
  broad and dome-like: wider than it is tall. The main mass peaks around 20-30
  blocks but the base flares much wider than the peak (no steep pointed cone).
- Role "buildings" are sub-lobes: local bulges added to that one shared
  heightmap at offset centers, each with its own noise seed so its bump has a
  distinct, non-mirrored silhouette. They read as organs of one organism, joined
  by shared earth skirts/berms, with readable saddle gaps between the lobe peaks
  — not as separate huts you could pick up and move.
- Surface jitter: perturb the shell height by +/-1-2 blocks with a second
  high-frequency noise so the surface never reads as clean ziggurat stair-steps.
  Add scattered surface debris (coarse dirt, mud, roots, mossy/rooted dirt,
  occasional sticks/pebbles via fences/walls) so the skin looks packed and
  lived-in rather than tiled.
- Chambers/tunnel mouths are carved voids, not punched grid holes: scatter
  entrance positions with noise (varied X/Y/Z and varied size 1x1 up to 3x3),
  carve a dark throat at least 4-6 blocks deep into the mass behind each mouth,
  and face the rear of the throat with a dark block so the opening reads as
  depth, not a sticker. No freestanding arch, lintel, or portal frame in front
  of any entrance.
- Spacing/non-overlap is a hard rule: separate lobe peaks must keep a readable
  gap or passage between their silhouettes; the deterministic footprint check
  (bounding ellipses, see the matrix `structure_spacing_non_overlap` row) must
  not report overlapping building cores. Shared berms/skirts are allowed only
  when each lobe peak still reads as its own mass.

Aim the QA cameras so the wide shots (colony_overview, settlement_scale,
culture_styles, endgame_project) show this one broad chambered mound family,
and the close shots (colony_ground, construction_stage, repair_scene) show deep
irregular mouths with dark depth and clear gaps between lobes.

## Native Blocks And Materials

The colony must stop relying on borrowed-looking placeholder blocks such as
honey, honeycomb, apatite-like blue minerals, amethyst-like accents, or random
vanilla decorative materials that do not feel like ants made them. Large
structures should use Formic Frontier's own blocks and textures wherever a
material is part of the colony identity.

Current acceptance: the material palette is good enough for the active world
architecture loop. Mark the broad material-palette row as accepted by user. The
remaining asset task is narrow: redraw the visible "block with a hole" so it
does not read as an unrelated honeycomb/placeholder surface.

Texture resolution requirement: Formic Frontier's custom block and item
textures should be authored at 32x32 pixels, not 16x16. Existing 16x16 Formic
textures are visual debt. The architecture loop should not be interrupted for a
mass texture migration, but the visual baseline cannot move to mechanics until
the asset slice upgrades the custom texture set to 32x32 and the holey block is
redrawn at 32x32.

Required direction:

- Add or use custom formic soil, packed mound earth, tunnel wall, root-reinforced
  earth, brood clay, fungus bed, resin, larva/storage, and trail materials.
- Give these blocks deterministic 32x32 resource-pack textures that read
  clearly in Minecraft without shaders.
- Use custom blocks in generated structures and QA scenes instead of honey,
  apatite/blue crystal, or unrelated vanilla accent blocks.
- Vanilla blocks may still appear as environmental support if they make sense
  as terrain, foliage, stone, or wood, but not as the primary colony material
  language.

## Hard Rejections

Do not pass visual baseline if screenshots still show:

- One flat, wide pad with a thin column, cap, or table-like crown.
- Buildings that are only 3-5 blocks tall when the target is settlement scale.
- Temple, pavilion, ziggurat, tower, or arcade pad language instead of organic
  ant-hill chambers.
- A single central mound carrying the whole scene while surrounding buildings
  remain low decorative pads.
- Symmetric role buildings, repeated circular pads, colliding/overlapping
  houses, or freestanding arches in front of entrances.
- Ants that look like toy tokens, unclear mobs, or clipped lineup specimens.
- Mounds or role buildings whose identity depends on the remaining holey block
  texture as a dominant surface.
- Assessment screenshots that crop off the peak, hide the base, or use stale
  artifacts.

## Visual Baseline Pass Bar

The visual baseline can pass only when required rows in
`visual-feature-matrix.json` are marked `pass` with screenshot evidence. A pass
requires a family resemblance to the reference intent:

- Main mound mass reaches roughly 20-30 blocks of vertical silhouette in QA
  scenes where settlement scale is being judged.
- Several surrounding structures are substantial organic chambers with their
  own height, entrances, and silhouettes.
- Surrounding structures are asymmetrical and ant-like, with no freestanding
  entry arches and no confusing overlap between separate buildings.
- Tunnel mouths and chamber openings are readable without zooming.
- Ant lineup and work-cycle screenshots make ants look like real colony
  members, not UI markers.
- The native material palette is accepted for this stage except the holey block
  texture and 16x16 texture resolution debt, which should be fixed in the asset
  slice.
- Tablet/interface can remain functional until the architecture baseline is
  credible; when interface work starts, the redesign must be very beautiful,
  research-tree oriented, and must prove that labels/icons never overlap.

Mechanics and playability are no longer globally locked behind this baseline.
They progress on the parallel content track (see
`docs/content-intent/formic-content-intent.md` and
`build/autonomous-loop/content-feature-matrix.json`), accepted by gametests via
the content gate. The rule here is narrower: world-architecture *visual* changes
are gated by this visual baseline, and content work must keep `test-mod` green
and must not visually regress the existing QA scenes.
