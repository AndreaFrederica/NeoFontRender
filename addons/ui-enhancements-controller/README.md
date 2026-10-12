# Revo UI Controller Support

This is an optional client-side addon for Revo UI. It directly depends on Revo Font
(`neofontrender`) and Revo UI (`neofontrender_ui_enhancements`), then registers SDL3
devices with the UIE Input API. The UIE main jar does not contain this addon or any
SDL runtime.

Build with:

```text
gradlew.bat :addons:ui-enhancements-controller:jar
```

The addon bundles SDL 3.4.14 for Windows, Linux, and macOS on x86-64 and ARM64.
At startup it extracts the matching library to a content-addressed temporary
directory and loads it through the Java 25 FFM backend. A custom build can still
override the bundled library with:

```text
-Ddev.isxander.sdl.library=C:\path\to\SDL3.dll
```

The implementation exposes raw linear device controls, samples every connected controller,
and routes the selected target device into UIE. Its NFR settings page contains a live Arc3D
controller tester, raw/dead-zone/mapped history and response curves, editable UIE action
bindings, and every vanilla or Forge-registered `KeyBinding`. Stick clicks, face buttons,
triggers, D-pad, paddles, touchpad, and generic joystick axes/buttons/hats can all be captured.

Movement actions keep their sign: the default left-stick Y binding is inverted so pushing up
produces positive `moveForward`, while left-stick X is inverted so left produces positive
`moveStrafe`, matching Forge 1.12.2 `MovementInput`. A vanilla or Forge key binding can capture
either half of an axis independently (`+` or `-` in the row label), so forward/back and
left/right do not collapse into one undirected trigger.

GUI input follows the same editable logical-action layer. By default the left stick moves a
virtual cursor, the right stick scrolls lists, A performs a held left click, B returns, X performs
a held right click, Y quick-moves a container slot, and the D-pad navigates between vanilla
buttons and slots with hold repeat. Mouse press, drag, and release remain separate events, so
items, scroll bars, and sliders can be dragged instead of every controller click being forced into
an immediate tap. Creative inventory scrolling and discoverable vanilla/Forge list widgets have
dedicated controller scrolling paths.

The 1.12.2 movement/turning behavior was checked against MrCrayfish Controllable 0.11.1,
reference commit [`d2b47f2`](https://github.com/MrCrayfish/Controllable/commit/d2b47f279e5bae5e47f83aa613c7902d8437c58f):
its `InputUpdateEvent` path writes signed `moveForward`/`moveStrafe`, and its render-tick path
applies look deltas to the player. UIE preserves that signed movement contract while routing
look through the active target: normal and shoulder views rotate the player, free-look can
rotate either the player or detached camera, Drone rotates its camera rig, and active Flight
consumes the right stick as pitch/roll input without a second vanilla turn.

The page also configures dead-zone, camera/flight sensitivity, axis inversion, and the future
vibration output preference. Values and both binding profiles are stored in
`config/neofontrender-ui-enhancements-controller.toml` and apply without restarting.

Per-device saved profiles, controller button prompts, and actual haptic output remain
future work. Target-device selection is currently retained for the running session and
falls back automatically when that device disconnects.

Run the native release check for the current machine with:

```text
gradlew.bat :addons:ui-enhancements-controller:smokeTestBundledSdl
```

To verify that every button, stick, and trigger of a connected controller (e.g. an Xbox One
pad) is recognized by SDL outside the game, run the interactive Swing probe and press each
control; closing the window prints a coverage report:

```text
gradlew.bat :addons:ui-enhancements-controller:probeGamepad
```
