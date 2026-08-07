# Revo UI Flight API v9

The public client API is rooted at `neofontrender.addons.api.flight.FlightApi`.
Integrations should depend on the UI Enhancements artifact, not classes in
`neofontrender.addons.flight`. Check `FlightApi.getApiVersion()` before using a newer contract.

All registrations use a namespaced `ResourceLocation`, replace an older registration with the
same id, and return an idempotent `FlightRegistration`. Keep the handle and close it when the
integration is disabled. Higher numeric priority runs first.

API v6 exposes UIE's state-safe Arc3D HUD drawing backend through
`FlightApi.getHudCanvas()`. Debug overlays and addon instruments should use this public
`FlightHudCanvas` instead of linking UIE internals or issuing raw GL lines. `polyline` submits an
entire curve as one continuous line strip.

API v9 uses `FlightAttitude`, a normalized quaternion, as the only physical attitude format.
Local +Z is thrust/forward, local +X is the left wing, local -X is the right wing and local +Y is
aircraft up. This complete right-handed frame remains defined through vertical flight, inverted
flight and repeated loops. Euler yaw/pitch/roll exists only at Minecraft camera and HUD output
boundaries. Register a `FlightHudAttitudeProvider`; returning `null` yields to the next provider
and ultimately to UIE's camera fallback.

```java
FlightRegistration attitude = FlightApi.registerHudAttitudeProvider(
        new ResourceLocation("example", "airframe_attitude"), 100,
        (player, partialTicks) -> {
            FlightAttitude attitude = aircraft.sampleAttitude(player, partialTicks);
            return new FlightHudAttitude(attitude);
        });
```

API v8 separates aircraft maneuver input from camera orientation. A high-priority
`FlightManeuverHandler` receives UIE's normalized virtual-stick pitch/yaw/roll and returns
`true` to prevent the legacy camera-driven flight controller. A separate
`FlightCameraTrackingProvider` then follows the physical airframe attitude. `rigid` tracking is
tick-locked with Minecraft's normal render interpolation; the public constructor supports damped
tracking with a response and maximum angular rate.

The maneuver yaw axis always includes the A/D rudder binding, even when UIE's legacy
`flightRoll.keyboardYaw` option is disabled or the aircraft denies `KEYBOARD_YAW` to stop UIE
from directly rotating the player. `FlightManeuverInput.getKeyboardYaw()` exposes that raw A/D
axis separately; `getYaw()` is the combined, sensitivity-adjusted virtual-stick command.

Camera and HUD consumers must use `FlightApi.getRenderPose(player, partialTicks)` when they need
Euler display angles. UIE resolves the quaternion branch once and returns the same
`FlightRenderPose` to the camera, built-in HUD, and addon instruments for that render sample. This
avoids separate yaw/roll branch choices near vertical flight and during repeated barrel rolls.

```java
FlightApi.registerManeuverHandler(new ResourceLocation("example", "controls"), 100, input -> {
    if (!aircraft.isPiloting(input.getPlayer())) return false;
    aircraft.setPitchStick(input.getPitch());
    aircraft.setRudder(input.getYaw());
    aircraft.setRollStick(input.getRoll());
    return true;
});

FlightApi.registerCameraTrackingProvider(new ResourceLocation("example", "camera"), 100,
        (player, partialTicks) -> aircraft.isPiloting(player)
                ? FlightCameraTracking.rigid(aircraft.attitude(player, partialTicks))
                : null);
```

## Capabilities and camera rotation

Capabilities are independent: `CONTROL`, `KEYBOARD_YAW`, `CAMERA_ROTATION`,
`PLAYER_ROLL_RENDERING`, `HUD`, and `CROSSHAIR_SUPPRESSION`. `KEYBOARD_YAW` specifically owns
UIE's built-in A/D yaw mapping, allowing an aerodynamic flight model to route the same keys to its
own rudder without disabling mouse attitude control or rendering. A provider returns `PASS`,
`ALLOW`, or `DENY`. The first non-PASS result
in priority order becomes the initial result of `FlightCapabilityEvent`; Forge listeners may still
override it. This lets a vehicle unlock rotation without pretending that the player is using an
Elytra.

```java
FlightRegistration camera = FlightApi.registerCameraRotation(
        new ResourceLocation("example", "aircraft_camera"), 100,
        player -> AircraftHooks.isPiloting(player));

// Degrees in the player's local aircraft axes. Run on the Minecraft client thread.
FlightApi.rotateView(pitchDelta, yawDelta, rollDelta);
FlightApi.setRoll(35.0F);
FlightApi.startBarrelRoll(1, 24);
FlightState state = FlightApi.getState(partialTicks);
```

`FlightState` also reports the negotiated server-companion permission, synchronization state and
effective roll-speed limit. Mods with their own network channel may feed remote pose state through
`updateRemotePlayerRoll` and read the interpolated value through `getPlayerRoll`.
Non-Elytra registered flight modes use a generic model-forward roll path; Elytra retains its
aircraft-local pose reconstruction.

Body-pose providers return `new FlightBodyPose(attitude)`. UIE applies that quaternion directly to
the third-person player model, without reconstructing a horizontal basis or converting through
Euler angles. `FlightAttitude.rotateLocal` and `slerp` are the supported integration/interpolation
operations; `forward`, `right` and `up` derive the exact same frame for physics and rendering.

Use `registerFlightMode` to enable the complete controller/camera/player/HUD stack. Use the lower
level `registerCapabilityProvider` when capabilities need different rules. UIE also posts
`FlightOrientationEvent` immediately before applying a local-axis update; it is mutable and
cancelable.

## Input

`registerControlProvider` supplies normalized pitch/yaw/roll axes every captured camera frame.
Multiple providers are accumulated and clamped. The existing Forge `FlightControllerInputEvent`
remains available for soft integrations, and `CameraMouseInputEvent` exposes raw relative mouse
deltas before Minecraft's sensitivity curve.

```java
FlightRegistration controller = FlightApi.registerControlProvider(
        new ResourceLocation("example", "joystick"), 50, input -> {
            input.addPitch(gamepad.pitch());
            input.addYaw(gamepad.yaw());
            input.addRoll(gamepad.roll());
        });
```

## Telemetry and HUD lifecycle

`FlightTelemetryEvent` may replace the complete telemetry snapshot. Replacement data drives both
built-in and third-party instruments. Values use blocks, seconds, and degrees; presentation units
are a renderer concern. `FlightHudRenderEvent.Pre` is cancelable and `Post` runs after the schema-3
HUD. Their `FlightHudRenderContext` provides bounds, canvas conversion, telemetry and controller
state, selected theme metadata, line/text scale and resolved ARGB colors.

Theme coordinates live on a virtual design canvas. UIE maps that canvas into the current scaled
game window with an aspect-preserving safe-area fit, then clips the complete component pass to the
mapped viewport. Components should convert design coordinates through `screenX`/`screenY`; they
must not cache screen coordinates across frames because window and GUI scale changes remap them.
Themes also declare `crosshairMode` as `KEEP` or `HIDE_VANILLA`. It is exposed through
`FlightHudRenderContext.getCrosshairMode()` and is combined with the player's master crosshair
setting; layouts without a replacement aiming symbol should use `KEEP`.

`FlightApi.registerHudTheme(id, json)` registers an in-memory schema-3 theme. The registry id is
authoritative and may be selected through `selectHudTheme`. Themes create components with an
element `type`; namespaced types are supplied by other mods. Every component receives the same
state-safe `FlightHudCanvas`, complete schema element, telemetry and canvas mapping as UIE's
built-ins, so extensions do not need direct OpenGL calls.

`FLIGHT_REFERENCE` elements choose `pitchMode: LIMITED` for a conventional local ladder or
`pitchMode: WRAP_360` for a spherical ladder that stays populated through inverted and
over-the-top attitudes. `pitchRange` is the visible half-window in either mode; it does not limit
the authored angular coverage of `WRAP_360`.

```java
FlightRegistration radarRenderer = FlightApi.registerHudComponent(
        new ResourceLocation("example", "radar"), (context, element) -> {
            float x = context.screenX(element.getX());
            float y = context.screenY(element.getY());
            float radius = element.getData().get("range").getAsFloat() / 16.0F;
            context.getCanvas().circle(x, y, radius, context.getColor("primary", 0xFFFFFFFF),
                    context.getLineWidth(), 48);
        });

String theme = "{\"schema\":3,\"name\":\"Aircraft HUD\",\"elements\":["
        + "{\"id\":\"radar\",\"type\":\"example:radar\",\"x\":270,\"y\":150,"
        + "\"data\":{\"range\":128}}]}";
FlightRegistration themeHandle = FlightApi.registerHudTheme(
        new ResourceLocation("example", "aircraft_hud"), theme);
FlightApi.selectHudTheme("example:aircraft_hud");
```

Components execute inside a restored OpenGL state boundary. Prefer `FlightHudCanvas`; a component
that deliberately makes direct GL calls must still balance its own matrix/attribute pushes.
For a completely independent HUD window, implement the public
`HudSurface` interface and register it with `HudWindowCompositor.INSTANCE`.

## Compatibility rules

- Runtime mutation methods are client-thread APIs.
- Angles are degrees; controller axes are normalized to `[-1, 1]`.
- Capability and controller registrations must not retain world/player objects after disconnect.
- Namespaced custom HUD types prevent collisions; unnamespaced names are reserved by UIE.
- Close registrations instead of assuming a particular mod unload order.
- Mods without a hard UIE dependency may use Forge events behind Forge 1.12 optional-method or
  reflection guards so their subscriber classes are not loaded when UIE is absent.

## Integrated and dedicated servers

The common client/server source set exposes `neofontrender.addons.api.flight.server.FlightServerApi`.
The full UIE jar activates it for Minecraft's integrated server; the renderer-free Server Companion
activates the identical implementation on a dedicated server.
Policy providers run on the server thread and may transform a per-player `FlightServerPolicy`:

```java
FlightServerRegistration policy = FlightServerApi.registerPolicyProvider(
        new ResourceLocation("example", "aircraft"), 100,
        (player, current) -> AircraftHooks.isPiloting(player)
                ? current.withEnabled(true).withSynchronization(true)
                    .withElytraRequired(false).withSynchronizationRange(384)
                : current);
```

The resulting policy is applied to the handshake and every orientation packet. It controls
permission, remote-player synchronization, the advertised maximum roll speed, whether the player
must be Elytra-flying, and the recipient range. This allows server-authoritative vehicle flight
without replacing UIE's channel.
