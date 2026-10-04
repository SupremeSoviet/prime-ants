---
name: formic-tablet-ui
description: "How the Formic colony tablet GUI is built, and the key lesson that gradient polish over vanilla buttons is not enough"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: f4152c93-af52-4383-a3ce-2526f6fd6f25
---

The colony tablet UI is [ColonyStatusScreen.java](src/client/java/com/formicfrontier/client/screen/ColonyStatusScreen.java) (client sourceset). It's a single custom `Screen` that draws a warm amber/chitin "field tablet": gradient panel + glowing double frame + drop shadow, a tab bar, a resource chip strip, a per-tab content body, an optional action rail, and a footer. Data comes from `ColonyUiSnapshot` (record accessors); actions go out via `ClientPlayNetworking.send(...payload)`.

**The lesson (user rejected the first attempt):** when the user says a Minecraft-mod GUI is "ugly, redo from scratch," adding gradients/bevels to the existing draw calls is NOT enough — the dominant ugliness was vanilla grey **stone `Button` widgets** clashing with the hand-drawn panel, plus unbalanced layouts. The fix that landed:
- Replace every `net.minecraft...Button` with a custom `FormicButton extends AbstractWidget` that fully self-draws (gradient + bevel + amber border + hover/disabled/selected states). **You cannot subclass `Button`/`AbstractButton` for this** in 1.21.11: `AbstractButton.renderWidget` is `final` and draws the vanilla sprite via `renderDefaultSprite`. Extend `AbstractWidget` directly and implement `renderWidget` + `updateWidgetNarration`; handle clicks by overriding `onClick(MouseButtonEvent, boolean)` (the 1.21.11 signature — there is no `OnPress`/`Button.OnPress` here, pass a plain `Runnable`).
- Fix layout density, not just colours: compute sections top-down with real spacing (heading must clear the resource strip), wrap chips to ≤2 rows, render trade offers as compact fixed-width cards with a SHORT arrow (not one full-width card with a giant stretched arrow), top-align research tier columns sized to content (no empty bands), and always render ≥1 card even when the panel is short (small GUI scale -> ~300px logical height).

Verify by rendering the `tablet_*` scenes via `scripts/gui-smoke.cmd` and reading the PNGs (see [[launching-the-autonomous-loop]] for the gui-smoke false-alarm note). Client-only change, so the 50 gametests are unaffected.
