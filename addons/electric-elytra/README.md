# Revo Electric Elytra

An optional Minecraft 1.7.10 submod built on Revo UI's Flight API v9.

## Flight

- Equip the Electric Elytra in the chest slot.
- Press `G` to arm or stop the engine.
- Press `-` / `=` to decrease or increase throttle in configurable 1% steps.
- Hold jump on the ground for a standing vertical takeoff.
- In powered flight, thrust follows an independent feet-to-head body axis, not the camera ray.
  The rendered body and physics share this axis; it follows view input at a configurable finite
  angular rate. Pitch changes angle of attack and never injects artificial upward velocity.
- Lift and drag use dynamic pressure, a finite-wing induced-drag term, and smooth stall behavior.
- Stop the engine to glide under the same aerodynamic model. The safety limit defaults to
  108 blocks per second.
- Bank angle rotates the wing normal and therefore the lift vector. Signed angle of attack and
  lift support sustained inverted flight when the pilot commands the required negative angle.
- A/D strafe input acts as an aerodynamic rudder: it creates side force and yaw moment instead of
  directly rotating the body heading. Sideslip also produces passive weathercock stability.
- Firework rockets add configurable acceleration along the physical body axis. They never pull a
  faster Electric Elytra back toward vanilla's target speed.
- Creative-mode flight is kept separate and cannot be mistaken for an unpowered Elytra launch.

## Equipment tiers

- Electric Elytra: passive aerodynamic directional stability.
- SAS Electric Elytra: press `H` to capture and hold the current body attitude with proportional
  correction and angular-rate damping, inspired by KSP Stability Assist.
- SAS Flap Electric Elytra: adds `UP`, `TO`, and `LDG` flap detents. `[` retracts one detent and
  `]` extends one detent. Flaps increase low-speed lift and also increase parasite drag.
  The positive maximum lift coefficient changes per detent, so the calculated stall speed falls
  from clean to TO to LDG; flaps do not unrealistically improve inverted negative lift.

The item stores its own energy and starts with 240,000 units on a new or freshly crafted item.

## Revo UI HUD

While the engine is armed or the player is gliding, the submod enables Revo UI's complete flight
mode and adds an Airbus-inspired engine panel after the selected schema-3 HUD. The panel shows N1
power, selected throttle, engine state, fuel/energy percentage, low-energy warning, and estimated
endurance. Electric-flight telemetry is lightly damped to prevent server correction packets from
making the speed tape flicker.
The systems strip shows `SAS HOLD/OFF`, `FLAPS UP/TO/LDG`, physical `BANK`, and the current
flap-dependent `VS`; the same `VS` replaces the vanilla Elytra low-speed reference on the tape.

With the F3 debug screen open, colored world-space vectors show thrust (magenta), lift (green),
drag (red), sideslip force (cyan), gravity (yellow), and the feet-to-head body axis (white).

The flight HUD also contains a game-scaled GPWS/TAWS display. It measures radio altitude to the
terrain directly below and samples the current velocity path up to six seconds ahead. Alerts
escalate through `SINK RATE` or `TERRAIN` to `PULL UP`; `STALL`, `TOO LOW FLAPS`, and post-takeoff
`DON'T SINK` modes are also supported. `RA` is the live ground clearance in blocks. All warning
envelopes can be tuned or disabled under `[warnings]` in the NFR-managed TOML config.

## Configuration

NFR's public configuration library creates
`config/neofontrender-electric-elytra.toml`. It contains energy capacity/drain settings, thrust and
standing-launch settings, plus air density, mass, wing area, lift curve, stall, parasite drag,
induced-drag, directional stability, SAS, flap, gravity, and hard-limit parameters.

## Build

```text
gradlew :addons:electric-elytra:stageElectricElytraRelease
```

The installable remapped JAR is staged under `build/release/electric-elytra`.
