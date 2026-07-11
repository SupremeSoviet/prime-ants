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

- Monumental earthen ant-hill architecture with a compact base and a tall,
  multi-level tapered silhouette.
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

The compact vertical main mound is now the accepted architectural baseline. The
active sequence applies the same validated blueprint vocabulary to surrounding
role buildings as lower, mostly single-storey arrangements of overlapping
lobes. Each family must gain its own silhouette, palette accents, carved room,
furnishings and repeated-site variation before the next family starts. Central
growth stages and attached mound-lobes follow after the role-building pass.

### Why the last several attempts failed (read this first)

Past attempts mixed every settlement building into one large geometry method.
That made small visual changes risky and encouraged repeated parameter nudges
without a clear description of the intended form. The replacement is a small,
validated blueprint that the generator compiles deterministically. Each stage
can therefore be evaluated as a whole silhouette instead of as thousands of
unrelated block placements.

### Required representation (the recipe, not adjectives)

Build the colony's main building as a compact, tall earthen hill with several
readable floors. Its height should exceed its width, and it should leave room for
later growth upward and for smaller connected lobes at the sides. Concretely:

- Stage one targets a footprint around 21x17 blocks and a height around 24
  blocks. It is a main-building prototype, not the completed settlement.
- Compose the hill from overlapping, vertically offset tiers. Each tier narrows
  and shifts slightly, creating irregular ledges and floor breaks while keeping
  one connected mass. Avoid a perfect cone, a symmetric pyramid, and a stack of
  identical circular plates.
- Later progression stages extend the same blueprint with extra upper tiers and
  attached mound-lobes. Lobes remain visibly joined to the main mass but retain
  readable saddle gaps and distinct silhouettes.
- Use deterministic boundary variation and a restrained earth/root palette to
  break up long stair-steps without hiding the overall form under decoration.
- Chambers/tunnel mouths are carved voids, not punched grid holes: scatter
  entrance positions with noise (varied X/Y/Z and varied size 1x1 up to 3x3),
  carve a dark throat at least 4-6 blocks deep into the mass behind each mouth,
  and face the rear of the throat with a dark block so the opening reads as
  depth, not a sticker. No freestanding arch, lintel, or portal frame in front
  of any entrance.
- Entrances should occupy different floors so the height reads as inhabited.
  Preserve structural continuity around every carved throat, and validate the
  finished solid volume as one connected component.
- Every visible floor must lead to a real furnished chamber. Floors use distinct
  purposes and furnishing sets rather than cloned chest layouts, and adjacent
  floors are joined by playable internal stairs/passages with two-block
  headroom.

Aim the QA cameras at the vertical middle of the structure. Wide shots must show
both base and peak; the close shot must show the stacked floors and deep,
irregular mouths without cropping the 24-block silhouette.

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
