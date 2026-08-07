# Electric Aircraft API v1

The public API lives in `neofontrender.addons.electricelytra.api`. It complements UIE Flight API
v9: UIE owns virtual-stick input and the shared quaternion `FlightAttitude` used by camera
tracking, body poses and HUD rendering, while this API exposes powered-aircraft discovery and
additive engine thrust. Aircraft physics should derive forward/right/up from that one quaternion
instead of reconstructing a frame from world-up.

The package is exported to Forge as `neofontrender_electric_aircraft_api` version `1`. A consuming
mod can declare `required-after:neofontrender_electric_elytra@[0.1.0,)` (and UIE when it consumes
the Flight API), then compile against the normal Electric Elytra jar. The contract depends only on
public Minecraft/Forge types; consumers do not need to subclass a built-in item.

Register a provider during mod initialization and keep the returned handle if the provider may be
removed dynamically:

```java
ElectricAircraftRegistration registration = ElectricAircraftApi.register(
        new ResourceLocation("example", "test_aircraft"), 100, entity -> {
            if (!isPilotingMyAircraft(entity)) return null;
            return new ElectricAircraft() {
                public ElectricFlightModel getFlightModel() {
                    return ElectricFlightModel.VANILLA_ELYTRA;
                }
                public boolean isEngineEnabled() { return engineEnabled(entity); }
                public boolean hasEnergy() { return storedEnergy(entity) > 0; }
                public double getThrottleFraction() { return throttle(entity); }
                public double getMaximumThrustAcceleration(EntityLivingBase ignored) {
                    return 12.0D;
                }
                public double getSpeedLimitBlocksPerSecond(EntityLivingBase ignored) {
                    return 108.0D;
                }
            };
        });
```

`VANILLA_ELYTRA` receives API-managed additive thrust before Minecraft's original Elytra travel
solver. `AERODYNAMIC` declares a complete aerodynamic implementation. `EXTERNAL` declares that the
provider owns physics; both are discoverable through `ElectricAircraftApi.query` but do not receive
automatic vanilla thrust.

Because discovery is entity-based, a provider may represent an equipped item, a ridden vehicle,
an entity capability, or another mod's synchronized flight state. Higher-priority providers win,
and provider IDs are namespaced and replaceable for development/hot-reload integrations.

Providers own their engine toggles, networking and energy accounting. Return `hasEnergy() == false`
as soon as thrust is unavailable. The built-in Electric Elytra items expose the same interface.

`ElectricThrustEvent` is posted on the Forge event bus before automatic vanilla thrust. It is
cancelable and its normalized direction, acceleration and speed limit are mutable. The event runs
on both the authoritative server entity and the controlling client entity so physics prediction
stays aligned; do not drain energy from this event.

Rules:

- Provider results are ephemeral views; do not retain queried entities.
- Throttle is normalized to `[0, 1]`; acceleration uses blocks/second squared.
- Speed limits use blocks/second. Return positive infinity for no API-side cap.
- Close dynamic registrations. Registering an existing namespaced ID replaces the old provider.
- A provider exception is isolated and yields to lower-priority providers and built-in equipment.
