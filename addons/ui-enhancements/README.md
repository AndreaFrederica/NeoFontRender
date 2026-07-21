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

## Runtime design

- Requires Neo Font Render 0.3.4 or newer.
- Uses Arc3D Core 2026.2.0 distributed by the required NFR main mod.
- Uses Cleanroom's host LWJGL 3.4.1 and never bundles LWJGL or native files.
- Yields to LegendaryTooltips by default when that mod is present.
- Uses NFR visual glyph bounds for wrapping and screen-edge placement.

## Build

```powershell
.\gradlew.bat :addons:ui-enhancements:test :addons:ui-enhancements:remapJar
```

The distributable jar is written to `addons/ui-enhancements/build/libs/` without
the `-dev` classifier.

## Configuration

The addon creates the independent file `config/neofontrender-ui-enhancements.toml`.
It controls rounded corners and curve quality, continuous shadow quality/color/offset,
edge antialiasing, title and body text, divider, padding, line spacing, wrapping,
and four-corner fill/border colors. Tooltip options are embedded as their own
NFR settings tab; future scrolling and screen-effect modules will register
separate tabs instead of sharing the tooltip page.
