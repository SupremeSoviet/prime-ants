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
- Dense forest-floor life: grass tufts, flowers, roots, paths, dirt variation,
  stones, storage piles, larvae or brood cues, and worker routes.
- Ants that read as insects: segmented bodies, visible legs, antennae, caste
  scale differences, and grounded posture.
- A native Formic material language: custom blocks, textures, models, and
  structure palettes that belong to this mod.
- Screenshots framed to show the subject clearly, including height and base.

## Native Blocks And Materials

The colony must stop relying on borrowed-looking placeholder blocks such as
honey, honeycomb, apatite-like blue minerals, amethyst-like accents, or random
vanilla decorative materials that do not feel like ants made them. Large
structures should use Formic Frontier's own blocks and textures wherever a
material is part of the colony identity.

Required direction:

- Add or use custom formic soil, packed mound earth, tunnel wall, root-reinforced
  earth, brood clay, fungus bed, resin, larva/storage, and trail materials.
- Give these blocks deterministic resource-pack textures that read clearly in
  Minecraft without shaders.
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
- Ants that look like toy tokens, unclear mobs, or clipped lineup specimens.
- Empty grass plain around the colony with little forest-floor density.
- Mounds or role buildings whose identity depends on honey, apatite-like blue
  blocks, amethyst-like accents, or unrelated vanilla placeholder textures.
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
- Tunnel mouths and chamber openings are readable without zooming.
- The ground plane feels like an inhabited forest floor, not a superflat demo.
- Ant lineup and work-cycle screenshots make ants look like real colony
  members, not UI markers.
- Structures use a native Formic Frontier material palette with custom blocks
  and textures for the colony architecture.
- Tablet/interface can remain functional, but it must not distract from the
  visual baseline work until the world and ants are credible.

Mechanics and playability work must remain locked until this baseline passes.
