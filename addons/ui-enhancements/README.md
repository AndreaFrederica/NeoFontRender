# Revo UI

Optional Minecraft 1.12.2 client addon for Neo Font Render. It contains global
interface features that are intentionally kept out of the font-rendering core.
Each substantially different feature is isolated as a module and contributes
its own tab to NFR's settings screen.

The first module replaces Forge tooltip layout/background rendering while
continuing to publish Forge tooltip color and post-render events.

Current feature modules:

- Experimental Tiqian paragraph typography for Simplified Chinese, with the
  original NFR CJK rules retained as the fallback for other locales and failures.
- Smooth wheel scrolling for vanilla `GuiSlot` and Forge `GuiScrollingList`.
- Native GLFW I-beam cursors over vanilla and ModularUI text fields.
- In-world screen background fade, four-corner gradient, and configurable
  two-pass Gaussian blur that yields to an already active post shader.
- Modern tooltip layout, shading, rarity colors, and LegendaryTooltips interop.
- Arc3D-powered health, absorption, armor, toughness, hunger, saturation,
  exhaustion, air, and mount-health bars with Forge height-stack coordination.
- Integrated TabbyChat channels/filters plus extended and persistent chat history.
- Embedded TabbyChat 2 Reforged and Salutation 1.12.2 sources. The installable UIE JAR
  supplies both original mod ids in ModList as built-in entries, so the standalone
  `TabbyChat` and `Salutation` JARs are not required.
- Optional continuous three-axis Elytra control. Horizontal mouse input rolls without an angle
  limit, vertical input pitches around the current aircraft frame, and an optional momentum mode
  behaves like a persistent virtual flight stick. Fine-grained mouse/controller sensitivity,
  per-axis inversion, a local-axis third-person pose, and an Arc3D flight HUD are included. The HUD
  provides layouts inspired by Airbus, Boeing, and MSFS avionics, with a
  flight-path vector, total-energy cue, narrow air-data scales, unit selection, vertical-speed
  indication, heading presentation, and a physics-derived VLS warning line. Nearby player orientation can be
  synchronized through the UIE Server Companion.

## Runtime design

- Requires Neo Font Render 0.5.1 or newer.
- Uses Arc3D Core 2026.2.0 distributed by the required NFR main mod.
- Uses Cleanroom's host LWJGL 3.4.1 and never bundles LWJGL or native files.
- Yields to LegendaryTooltips by default when that mod is present.
- Uses NFR visual glyph bounds for wrapping and screen-edge placement.
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
relative mouse movement and before the vanilla sensitivity curve and
`EntityPlayerSP.turn(float, float)` call. A handler may replace or consume either raw axis, or
cancel the event to consume both. UIE deliberately leaves the vanilla `turn` invocation in place,
so coremods that redirect that call remain compatible.

```java
@SubscribeEvent
public void cameraMouse(CameraMouseInputEvent event) {
    event.setDeltaX(event.getDeltaX() / 2);
    if (lockPitch) event.consumeVertical();
}
```

Controller integrations can publish normalized axes without adding a hard dependency from UIE to
a particular controller library. Subscribe to `FlightControllerInputEvent` and set any available
axis in `[-1, 1]`; UIE applies the user's controller sensitivity and inversion settings after all
providers have run.

```java
@SubscribeEvent
public void flightController(FlightControllerInputEvent event) {
    event.setPitch(gamepad.leftStickY());
    event.setYaw(gamepad.rightStickX());
    event.setRoll(gamepad.leftStickX());
}
```

The conformal pitch ladder, flight-path vector, total-energy cue, bank scale, input marker and
air-data scales use Arc3D Core color/math and are registered as a non-interactive surface in UIE's
shared HUD compositor.

Flight HUD layouts are schema-3 JSON themes. UIE bundles layouts inspired by Airbus, Boeing,
and MSFS avionics, plus FPV OSD and cinematic tactical themes, then
loads player files from `neofontrender/flight_hud_themes`. The generated `README.txt` documents the
schema and `example-airliner-hud.json` demonstrates inheritance; files are checked for changes once per
second while flying. The ordered `elements` array creates and positions every instrument—including
the virtual stick—and inherited elements merge by `id`, so a custom theme may override one
component without copying the base theme. Airspeed,
altitude and vertical-speed units are player settings rather than theme properties, so changing a
layout never silently changes units.

While the flight HUD is visible, its settings page can independently authorize a theme's
`KEEP`/`HIDE_VANILLA` crosshair policy and suppress the hotbar, player-status bars, experience,
chat, boss bars, potion icons, subtitles,
player list, and overlay text. Each choice cancels only the corresponding Forge overlay event;
the complete HUD event and UIE compositor remain available to other HUD surfaces.

The flight stack exposes a versioned extension API for other mods: capability providers can
unlock camera/player rotation outside Elytra flight, controller providers contribute normalized
three-axis input, callers can query or mutate orientation and trigger barrel rolls, telemetry is
replaceable through Forge events, and namespaced schema-3 components/themes can be registered at
runtime. See [`FLIGHT_API.md`](FLIGHT_API.md) for lifecycle, priority and threading contracts.

Displayed airspeed is the magnitude of the same three-axis motion vector consumed by vanilla
Elytra travel. The VLS-style line solves vanilla's gravity/lift term
`-0.08 + cos(pitch)^2 * 0.06` against its pitch-up energy conversion
`horizontalSpeed * -sin(pitch) * 0.04 * 3.2`; it is therefore a useful gameplay warning reference,
not a claim that Minecraft simulates real angle of attack. Speed acceleration and vertical speed
are sampled at the entity's 20 Hz physics ticks so render-frame repetition does not create spikes.

The Crosshair settings page provides the traditional Custom Crosshair Mod feature set on 1.12.2:
vanilla, cross, dot, circle, square, triangle, arrow, flight-chevron, debug and pixel-drawn styles; independent
width/height/gap/thickness, rotation, scale and offsets; adaptive contrast, outlines and center
dots; contextual visibility, dynamic attack/bow spread, target colors, rainbow animation,
cooldown rings, low-durability warnings and ammunition indicators. The drawn style includes an
in-game pixel editor.

UIE subscribes to Forge's `CROSSHAIRS` element at lowest priority and can either yield to an item
mod or claim the layer according to the configured priority. An active Shoulder Surfing camera is
not mistaken for ordinary third person. Vanilla's two regular third-person perspectives remain
cursorless unless UIE's explicit third-person visibility option is enabled. Independently,
vanilla/custom crosshairs can be hidden only while the flight HUD is visible, leaving the HUD's
aircraft-center symbol as the reticle.

The full UIE jar owns both halves of the `nfr_ui_flight` protocol in an integrated server. The
renderer-free Server Companion supplies the same server half to dedicated servers. The protocol
performs a versioned handshake, sends the flight-roll policy and maximum angular speed, validates
Elytra flight state and finite roll values, and relays normalized orientations only to nearby
clients that completed the handshake. A remote server without either implementation does not
register the channel, so the client does not send flight packets.

## Build

Initialize the pinned Tiqian source before the first UIE build:

```powershell
git submodule update --init addons/ui-enhancements/vendor/tiqian
```

```powershell
.\gradlew.bat :addons:ui-enhancements:test :addons:ui-enhancements:remapJar
```

The distributable jar is written to `addons/ui-enhancements/build/libs/` without
the `-dev` classifier.

GitHub Actions builds the pinned `AndreaFrederica/ModularUI` fork first, installs its development
artifact into Maven Local for compilation, and exports its remapped JAR with the Revo UI bundle.
Tags use the form `uie/<version>` (for example `uie/0.6.0`). The GitHub Release contains ModularUI,
Revo UI, the optional controller and server companions, and Electric Elytra. CurseForge publishing
for this bundle will be enabled separately after each project and dependency relationship is ready.

## Configuration

The addon creates the independent file `config/neofontrender-ui-enhancements.toml`.
Each feature has a separate NFR settings tab. The status-bar master switch leaves
all vanilla and Forge HUD events untouched when disabled; individual vanilla bars,
animation, dimensions, and colors can also be configured independently.
