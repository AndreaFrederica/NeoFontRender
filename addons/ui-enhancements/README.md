# NFR UI Enhancements

Optional Minecraft 1.12.2 client addon for Neo Font Render. It contains global
interface features that are intentionally kept out of the font-rendering core.
Each substantially different feature is isolated as a module and contributes
its own tab to NFR's settings screen.

The first module replaces Forge tooltip layout/background rendering while
continuing to publish Forge tooltip color and post-render events.

Current feature modules:

- Smooth wheel scrolling for vanilla `GuiSlot` and Forge `GuiScrollingList`.
- Native GLFW I-beam cursors over vanilla and ModularUI text fields.
- In-world screen background fade, four-corner gradient, and configurable
  two-pass Gaussian blur that yields to an already active post shader.
- Modern tooltip layout, shading, rarity colors, and LegendaryTooltips interop.
- Arc3D-powered health, absorption, armor, toughness, hunger, saturation,
  exhaustion, air, and mount-health bars with Forge height-stack coordination.
- Integrated TabbyChat channels/filters plus extended and persistent chat history.

## Runtime design

- Requires Neo Font Render 0.3.5 or newer.
- Uses Arc3D Core 2026.2.0 distributed by the required NFR main mod.
- Uses Cleanroom's host LWJGL 3.4.1 and never bundles LWJGL or native files.
- Yields to LegendaryTooltips by default when that mod is present.
- Uses NFR visual glyph bounds for wrapping and screen-edge placement.
- Status bars subscribe at Forge's lowest overlay priority, respect elements already
  canceled by other mods, and yield to Classic Bar by default.

## Status-bar integration API

Other client mods can register a data provider through
`neofontrender.addons.hud.api.HudBarRegistry`. A provider selects a Forge element slot and side,
returns a `HudBarValue`, and defaults to reserving space without canceling vanilla rendering.
Only providers that explicitly return `true` from `replacesVanilla()` may replace that element.
Namespaced ids and deterministic ordering allow multiple integrations to share one height stack.

## Build

```powershell
.\gradlew.bat :addons:ui-enhancements:test :addons:ui-enhancements:remapJar
```

The distributable jar is written to `addons/ui-enhancements/build/libs/` without
the `-dev` classifier.

GitHub Actions builds the addon independently for relevant pushes and pull requests. Releases use
tags in the form `ui-enhancements-v<version>` (for example `ui-enhancements-v0.1.0`) and attach the
installable remapped JAR plus its SHA-256 checksum to the repository's GitHub Release.

## Configuration

The addon creates the independent file `config/neofontrender-ui-enhancements.toml`.
Each feature has a separate NFR settings tab. The status-bar master switch leaves
all vanilla and Forge HUD events untouched when disabled; individual vanilla bars,
animation, dimensions, and colors can also be configured independently.
