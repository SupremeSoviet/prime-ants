# Formic Visual Assessment Rubric

Use this rubric as a strict checklist for Formic Frontier visual QA. It adapts general usability heuristics, accessibility contrast/readability guidance, and game accessibility patterns to the mod's deterministic screenshots.

Sources to keep in mind:

- NN/G usability heuristics: visibility of status, consistency, recognition over recall, error prevention, aesthetic/minimalist design: https://www.nngroup.com/articles/ten-usability-heuristics/
- NN/G heuristic evaluation: evaluate independently against heuristics, then consolidate issues: https://www.nngroup.com/articles/how-to-conduct-a-heuristic-evaluation/
- WCAG 2.2 distinguishable content, contrast, non-text contrast, focus/target visibility: https://www.w3.org/TR/WCAG22/
- W3C text contrast: use 4.5:1 as the normal text benchmark and 3:1 for large text when measurable: https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html
- W3C non-text contrast: meaningful controls and graphics should be distinguishable, with 3:1 as the benchmark when measurable: https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html
- Xbox text display and contrast guidance: game text and UI should remain readable in realistic play contexts: https://learn.microsoft.com/en-us/gaming/accessibility/xbox-accessibility-guidelines/101 and https://learn.microsoft.com/en-us/gaming/accessibility/xbox-accessibility-guidelines/102
- Game Accessibility Guidelines: small text and low contrast are common game UI complaints; prefer high-contrast backgrounds, outlines, and shadows where needed: https://gameaccessibilityguidelines.com/provide-high-contrast-between-text-ui-and-background/
- APX patterns: Clear Text, Distinguish This From That, Clear Channels: https://accessible.games/accessible-player-experiences/access-patterns/clear-text/ , https://accessible.games/accessible-player-experiences/access-patterns/distinguish-this-from-that/ , https://accessible.games/accessible-player-experiences/access-patterns/clear-channels/

## Severity

- `P0`: artifact or scene is unusable: blank, wrong resolution, crash, missing screenshot, missing model/texture, broken UI state, or scene cannot support assessment.
- `P1`: a player cannot reliably read, identify, or use a core feature. Examples: clipped tablet text, overlapping critical values, ants indistinguishable, colony visibly floating, primary controls unclear.
- `P2`: the feature works but looks untrustworthy, noisy, ugly, or hard to parse. Examples: awkward spacing, weak hierarchy, muddy models, poor composition.
- `P3`: polish issue that should not block a merge alone.

Default verdict: any `P0` or `P1` means `FAIL`.

## UI Readability

Check tablet screenshots first because bad UI blocks play.

- Text must be readable at the captured resolution without zooming into a tiny crop.
- Labels must not clip, overlap, spill out of panels, or collide with numbers/icons.
- Russian text must fit; longer RU strings need real wrapping or wider containers.
- Text contrast must be strong against panel/background. If exact contrast cannot be measured from the screenshot, judge practical legibility and say it is a visual estimate.
- Hierarchy must make the current tab, current colony state, primary values, and actionable controls obvious.
- Controls must look clickable or selectable. Disabled/locked states must be visually distinct without relying only on color.
- Data density must support scanning. A busy screen is acceptable only if grouping, spacing, and headings make it readable.
- Minecraft GUI style is acceptable, but it cannot be used as an excuse for cramped or illegible text.

## Minecraft World Fit

Judge whether the mod content feels physically grounded inside Minecraft.

- Colony floor and starter structures must sit on terrain, not float or intersect weirdly.
- No key block should have visible air under it unless intentionally bridged and readable as such.
- Buildings/resources should align to block grid enough to feel intentional.
- Scene camera must show the feature, not mostly sky, ground, terrain wall, or empty space.
- Lighting/weather/time should make the subject visible.
- Debug, tutorial, chat, cursor, or system overlays are defects if they cover the assessed subject.
- The visual language should feel like a Minecraft mod: blocky, readable, grounded, and coherent with the world.

## Ant Quality

Ants are core fantasy objects; be especially strict.

- Each caste must have a distinct silhouette, scale, or pose.
- Ants must be large enough to identify at gameplay camera distance.
- Texture/materials must not collapse into dark blobs or noisy pixels.
- Orientation should let the player understand head/body/legs without guessing.
- The lineup scene should reveal variety and quality, not hide ants behind camera angle, terrain, or overlap.
- If models look placeholder, toy-like, or unreadable, mark at least `P2`; mark `P1` if gameplay identification is blocked.

## Colony Quality

Judge whether the colony looks like a playable system, not a random prop pile.

- Overview should communicate structure: hub, paths, resources, buildings, progression.
- Ground view should make the player want to approach and understand it.
- Resource nodes and buildings must be visually distinct.
- There should be a clear focal point and readable boundaries.
- Visual noise should not hide gameplay elements.
- The colony should have scale and composition appropriate for Minecraft survival gameplay.

## Localization

- English and Russian tablet scenes must both be assessed.
- Treat mojibake, missing glyphs, untranslated keys, clipped RU text, and inconsistent labels as serious defects.
- If RU text is longer, the layout must adapt; do not accept English-only sizing.

## Evidence Standard

Every finding must include:

- `Scene`: screenshot/artifact path.
- `Evidence`: what is visibly wrong.
- `Impact`: what a player cannot read, understand, or trust.
- `Fix`: concrete implementation direction.
- `Acceptance`: what the next screenshot/log must show.

When uncertain, say "likely" and explain the visual clue. Do not claim code causes unless code was inspected separately.
