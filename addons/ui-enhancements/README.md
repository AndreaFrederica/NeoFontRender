# Revo UI for Minecraft 1.7.10

Optional Minecraft 1.7.10 client addon for Neo Font Render. It contains global
interface features that are intentionally kept out of the font-rendering core.
Each substantially different feature is isolated as a module and contributes
its own tab to NFR's settings screen.

Current feature modules:

- Experimental Tiqian paragraph typography for Simplified Chinese, with the
  original NFR CJK rules retained as the fallback for other locales and failures.
- Smooth wheel scrolling for vanilla `GuiSlot` and Forge `GuiScrollingList`.
- Native text cursors over vanilla and ModularUI text fields.
- In-world screen background fade, four-corner gradient, and configurable
  two-pass Gaussian blur.
- Modern tooltip layout, shading, rarity colors, and foreign-tooltip
  compatibility rendering.
- Arc3D-powered health, absorption, armor, hunger, saturation, exhaustion, air,
  and mount-health bars with Forge height-stack coordination.
- Integrated TabbyChat channels/filters plus extended and persistent chat history,
  search, player heads, item icons, context menus, inline image glyphs, and
  player-link interactions.
- Embedded TabbyChat 2 Reforged and Salutation 1.12.2 sources. The installable UIE JAR
  supplies both original mod ids in ModList as built-in entries, so the standalone
  `TabbyChat` and `Salutation` JARs are not required.
- Optional continuous three-axis Elytra control with fine-grained mouse/controller
  sensitivity, per-axis inversion, a local-axis third-person pose, and an Arc3D
  flight HUD. The HUD provides layouts inspired by Airbus, Boeing, and MSFS
  avionics, including a flight-path vector, total-energy cue, air-data scales,
  vertical-speed indication, heading presentation, and a physics-derived VLS
  warning line. Nearby player orientation can be synchronized through the UIE
  Server Companion.
- Crosshair customization with vanilla, cross, dot, circle, square, triangle,
  arrow, flight-chevron, debug and pixel-drawn styles, adaptive contrast, outlines,
  dynamic attack/bow spread, target colors, cooldown rings, low-durability warnings,
  ammunition indicators, and an in-game pixel editor.
- Rotating loading-screen tips, resource-reload support, and a modern world-loading
  presentation with save snapshots, progress, and animation.

## Runtime design

- Requires Neo Font Render 0.3.5 or newer.
- Uses Arc3D Core distributed by the required NFR main mod.
- Uses Cleanroom's host LWJGL 3.4.1 and never bundles LWJGL or native files.
- Retains Salutation's original command tree, argument parsers, multiline chat backend,
  sleep-chat screen, and advanced completion behavior under the original `speiger.src.salutation`
  package names. Only its CarbonConfig/standalone Forge entry point is replaced by UIE's config bridge.
- Status bars subscribe at Forge's lowest overlay priority, respect elements already
  canceled by other mods, and yield to Classic Bar by default.

## Status-bar integration API

Other client mods can register a data provider through
`neofontrender.addons.hud.api.HudBarRegistry`. A provider selects a Forge element slot and side,
returns a `HudBarValue`, and defaults to reserving space without canceling vanilla rendering.
Only providers that explicitly return `true` from `replacesVanilla()` may replace that element.
Namespaced ids and deterministic ordering allow multiple integrations to share one height stack.

## Camera mouse-input API

Client mods can subscribe to Forge's event bus for
`neofontrender.addons.api.input.CameraMouseInputEvent`. UIE publishes it after Minecraft polls
relative mouse movement and before the vanilla sensitivity curve and player `turn` call. A handler
may replace or consume either raw axis, or cancel the event to consume both. UIE deliberately leaves
the vanilla `turn` invocation in place, so coremods that redirect that call remain compatible.

Controller integrations can publish normalized axes without adding a hard dependency from UIE to
a particular controller library. Subscribe to `FlightControllerInputEvent` and set any available
axis in `[-1, 1]`; UIE applies the user's controller sensitivity and inversion settings after all
providers have run.

Flight HUD layouts are schema-3 JSON themes. UIE bundles layouts inspired by Airbus, Boeing,
and MSFS avionics, plus FPV OSD and cinematic tactical themes, then loads player files from the
flight HUD theme directory. The generated `README.txt` documents the schema and
`example-airliner-hud.json` demonstrates inheritance. The ordered `elements` array creates and
positions every instrument, and inherited elements merge by `id`, so a custom theme may override
one component without copying the base theme.

The flight stack exposes a versioned extension API for other mods: capability providers can
unlock camera/player rotation outside Elytra flight, controller providers contribute normalized
three-axis input, callers can query or mutate orientation and trigger barrel rolls, telemetry is
replaceable through Forge events, and namespaced schema-3 components/themes can be registered at
runtime. See [`FLIGHT_API.md`](FLIGHT_API.md) for lifecycle, priority and threading contracts.

The full UIE jar owns both halves of the `nfr_ui_flight` protocol in an integrated server. The
renderer-free Server Companion supplies the same server half to dedicated servers. The protocol
performs a versioned handshake, sends the flight-roll policy and maximum angular speed, validates
Elytra flight state and finite roll values, and relays normalized orientations only to nearby
clients that completed the handshake.

## Build

Initialize the pinned Tiqian source before the first UIE build:

```powershell
git submodule update --init addons/ui-enhancements/vendor/tiqian
```

```powershell
.\gradlew.bat :addons:ui-enhancements:compileJava --no-daemon --console=plain
```

The distributable jar is written to `addons/ui-enhancements/build/libs/` without
requiring a separate `runClient` step.
