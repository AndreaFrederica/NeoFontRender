package neofontrender.addons.api.flight;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.Function;

/**
 * Stable client flight facade for UIE integrations.
 *
 * <p>Registries accept namespaced ids, are safe to mutate during lifecycle events, and return
 * idempotent handles. Runtime orientation mutation must be performed on Minecraft's client thread.
 * Soft dependencies may use the matching Forge events instead of linking this class.</p>
 */
public final class FlightApi {
    public static final int API_VERSION = 9;
    private static final Logger LOGGER = LogManager.getLogger("Revo UI Flight API");
    private static volatile String lastCapabilityProviderId;
    private static volatile java.util.List<String> lastControlProviderIds = Collections.emptyList();
    private static volatile String lastBodyPoseProviderId;
    private static volatile String lastHudAttitudeProviderId;
    private static volatile String lastManeuverHandlerId;
    private static volatile String lastCameraTrackingProviderId;
    private static volatile String lastHudComponentType;

    private static final CopyOnWriteArrayList<CapabilityEntry> CAPABILITIES =
            new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<ControlEntry> CONTROLS =
            new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<BodyPoseEntry> BODY_POSES =
            new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<HudAttitudeEntry> HUD_ATTITUDES =
            new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<ManeuverEntry> MANEUVER_HANDLERS =
            new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<CameraTrackingEntry> CAMERA_TRACKERS =
            new CopyOnWriteArrayList<>();
    private static final Map<String, FlightHudComponent> HUD_COMPONENTS =
            Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<String, String> HUD_THEMES =
            Collections.synchronizedMap(new LinkedHashMap<>());
    private static volatile Backend backend = Backend.NOOP;

    private FlightApi() {}

    public static FlightRegistration registerCapabilityProvider(ResourceLocation id, int priority,
                                                                 FlightCapabilityProvider provider) {
        Objects.requireNonNull(id, "id"); Objects.requireNonNull(provider, "provider");
        CapabilityEntry entry = new CapabilityEntry(id.toString(), priority, provider);
        removeCapability(entry.id);
        CAPABILITIES.add(entry);
        CAPABILITIES.sort(Comparator.comparingInt((CapabilityEntry value) -> value.priority)
                .reversed().thenComparing(value -> value.id));
        return once(() -> CAPABILITIES.remove(entry));
    }

    public static FlightRegistration registerControlProvider(ResourceLocation id, int priority,
                                                              FlightControlProvider provider) {
        Objects.requireNonNull(id, "id"); Objects.requireNonNull(provider, "provider");
        ControlEntry entry = new ControlEntry(id.toString(), priority, provider);
        removeControl(entry.id);
        CONTROLS.add(entry);
        CONTROLS.sort(Comparator.comparingInt((ControlEntry value) -> value.priority)
                .reversed().thenComparing(value -> value.id));
        return once(() -> CONTROLS.remove(entry));
    }

    public static FlightRegistration registerBodyPoseProvider(ResourceLocation id, int priority,
                                                               FlightBodyPoseProvider provider) {
        Objects.requireNonNull(id, "id"); Objects.requireNonNull(provider, "provider");
        BodyPoseEntry entry = new BodyPoseEntry(id.toString(), priority, provider);
        BODY_POSES.removeIf(value -> value.id.equals(entry.id));
        BODY_POSES.add(entry);
        BODY_POSES.sort(Comparator.comparingInt((BodyPoseEntry value) -> value.priority)
                .reversed().thenComparing(value -> value.id));
        return once(() -> BODY_POSES.remove(entry));
    }

    /** Registers a body/vehicle attitude source that replaces camera attitude in the HUD. */
    public static FlightRegistration registerHudAttitudeProvider(
            ResourceLocation id, int priority, FlightHudAttitudeProvider provider) {
        Objects.requireNonNull(id, "id"); Objects.requireNonNull(provider, "provider");
        HudAttitudeEntry entry = new HudAttitudeEntry(id.toString(), priority, provider);
        HUD_ATTITUDES.removeIf(value -> value.id.equals(entry.id));
        HUD_ATTITUDES.add(entry);
        HUD_ATTITUDES.sort(Comparator.comparingInt((HudAttitudeEntry value) -> value.priority)
                .reversed().thenComparing(value -> value.id));
        return once(() -> HUD_ATTITUDES.remove(entry));
    }

    public static FlightRegistration registerManeuverHandler(
            ResourceLocation id, int priority, FlightManeuverHandler handler) {
        Objects.requireNonNull(id, "id"); Objects.requireNonNull(handler, "handler");
        ManeuverEntry entry = new ManeuverEntry(id.toString(), priority, handler);
        MANEUVER_HANDLERS.removeIf(value -> value.id.equals(entry.id));
        MANEUVER_HANDLERS.add(entry);
        MANEUVER_HANDLERS.sort(Comparator.comparingInt((ManeuverEntry value) -> value.priority)
                .reversed().thenComparing(value -> value.id));
        return once(() -> MANEUVER_HANDLERS.remove(entry));
    }

    public static FlightRegistration registerCameraTrackingProvider(
            ResourceLocation id, int priority, FlightCameraTrackingProvider provider) {
        Objects.requireNonNull(id, "id"); Objects.requireNonNull(provider, "provider");
        CameraTrackingEntry entry = new CameraTrackingEntry(id.toString(), priority, provider);
        CAMERA_TRACKERS.removeIf(value -> value.id.equals(entry.id));
        CAMERA_TRACKERS.add(entry);
        CAMERA_TRACKERS.sort(Comparator.comparingInt((CameraTrackingEntry value) -> value.priority)
                .reversed().thenComparing(value -> value.id));
        return once(() -> CAMERA_TRACKERS.remove(entry));
    }

    /** Convenience registration for vehicles or custom flight states that want the whole stack. */
    public static FlightRegistration registerFlightMode(ResourceLocation id, int priority,
                                                        Predicate<EntityPlayerSP> active) {
        Objects.requireNonNull(active, "active");
        return registerCapabilityProvider(id, priority, (player, capability, builtIn) ->
                active.test(player) ? FlightDecision.ALLOW : FlightDecision.PASS);
    }

    /** Unlocks camera and third-person longitudinal rotation without enabling UIE controls/HUD. */
    public static FlightRegistration registerCameraRotation(ResourceLocation id, int priority,
                                                            Predicate<EntityPlayerSP> active) {
        Objects.requireNonNull(active, "active");
        return registerCapabilityProvider(id, priority, (player, capability, builtIn) ->
                active.test(player) && (capability == FlightCapability.CAMERA_ROTATION
                        || capability == FlightCapability.PLAYER_ROLL_RENDERING)
                        ? FlightDecision.ALLOW : FlightDecision.PASS);
    }

    public static FlightRegistration registerHudComponent(ResourceLocation type,
                                                           FlightHudComponent component) {
        Objects.requireNonNull(type, "type"); Objects.requireNonNull(component, "component");
        String id = type.toString();
        HUD_COMPONENTS.put(id, component);
        backendRun("registriesChanged", backend::registriesChanged);
        return once(() -> {
            synchronized (HUD_COMPONENTS) {
                if (HUD_COMPONENTS.get(id) == component) HUD_COMPONENTS.remove(id);
            }
            backendRun("registriesChanged", backend::registriesChanged);
        });
    }

    /** Registers a complete schema-3 theme JSON document under a namespaced id. */
    public static FlightRegistration registerHudTheme(ResourceLocation id, String json) {
        Objects.requireNonNull(id, "id"); Objects.requireNonNull(json, "json");
        String key = id.toString();
        HUD_THEMES.put(key, json);
        backendRun("registriesChanged", backend::registriesChanged);
        return once(() -> {
            synchronized (HUD_THEMES) {
                if (json.equals(HUD_THEMES.get(key))) HUD_THEMES.remove(key);
            }
            backendRun("registriesChanged", backend::registriesChanged);
        });
    }

    public static int getApiVersion() { return API_VERSION; }
    public static boolean isAvailable() { return backend != Backend.NOOP; }
    /** Returns UIE's state-safe HUD canvas, or null before the backend is installed. */
    public static FlightHudCanvas getHudCanvas() { return backendCall("hudCanvas", backend::hudCanvas); }
    public static FlightState getState(float partialTicks) {
        return backendCall("state", () -> backend.state(partialTicks));
    }
    /** Returns UIE's single cached quaternion render sample for the current camera frame. */
    public static FlightRenderPose getRenderPose(EntityPlayerSP player, float partialTicks) {
        float clamped = Math.max(0.0F, Math.min(1.0F, partialTicks));
        return backendCall("renderPose", () -> backend.renderPose(player, clamped));
    }
    public static boolean isActive() { return getState(1.0F).isActive(); }
    public static void setRoll(float degrees) {
        float value = finite(degrees);
        backendRun("setRoll", () -> backend.setRoll(value));
    }
    public static void rotateView(float pitchDegrees, float yawDegrees, float rollDegrees) {
        float pitch = finite(pitchDegrees), yaw = finite(yawDegrees), roll = finite(rollDegrees);
        backendRun("rotate", () -> backend.rotate(pitch, yaw, roll));
    }
    public static boolean startBarrelRoll(int direction, int durationTicks) {
        int side = direction < 0 ? -1 : 1;
        int duration = Math.max(1, Math.min(1200, durationTicks));
        return backendCall("startBarrelRoll", () -> backend.startBarrelRoll(side, duration));
    }
    public static float getPlayerRoll(int entityId, float partialTicks) {
        return backendCall("playerRoll", () -> backend.playerRoll(entityId, partialTicks));
    }
    public static void updateRemotePlayerRoll(int entityId, boolean rolling, float degrees) {
        float value = finite(degrees);
        backendRun("updateRemotePlayerRoll",
                () -> backend.updateRemotePlayerRoll(entityId, rolling, value));
    }
    public static void resetOrientation() { backendRun("resetOrientation", backend::resetOrientation); }
    public static java.util.List<String> getAvailableHudThemes() {
        return backendCall("hudThemes", backend::hudThemes);
    }
    public static String getSelectedHudTheme() {
        return backendCall("selectedHudTheme", backend::selectedHudTheme);
    }
    public static boolean selectHudTheme(String id) {
        return backendCall("selectHudTheme", () -> backend.selectHudTheme(id));
    }
    public static void reloadHudThemes() { backendRun("registriesChanged", backend::registriesChanged); }

    /** UIE implementation hook. Other mods should not replace the active backend. */
    public static synchronized void installBackend(Backend implementation) {
        Objects.requireNonNull(implementation, "implementation");
        if (backend != Backend.NOOP && backend != implementation) {
            throw new IllegalStateException("Flight API backend already installed");
        }
        backend = implementation;
    }

    public static FlightDecision queryCapability(EntityPlayerSP player,
                                                 FlightCapability capability,
                                                 boolean builtInDefault) {
        lastCapabilityProviderId = null;
        for (CapabilityEntry entry : CAPABILITIES) {
            FlightDecision decision;
            try {
                decision = entry.provider.decide(player, capability, builtInDefault);
            } catch (RuntimeException error) {
                throw callbackFailure("capability:" + entry.id, "decide", error);
            }
            if (decision != null && decision != FlightDecision.PASS) {
                lastCapabilityProviderId = entry.id;
                return decision;
            }
        }
        lastCapabilityProviderId = null;
        return FlightDecision.PASS;
    }

    public static void collectControlInput(FlightControlInput input) {
        lastControlProviderIds = Collections.emptyList();
        java.util.List<String> applied = new java.util.ArrayList<>();
        for (ControlEntry entry : CONTROLS) {
            try {
                entry.provider.update(input);
                applied.add(entry.id);
            } catch (RuntimeException error) {
                throw callbackFailure("control:" + entry.id, "update", error);
            }
        }
        lastControlProviderIds = Collections.unmodifiableList(applied);
    }

    public static FlightBodyPose queryBodyPose(AbstractClientPlayer player, float partialTicks) {
        lastBodyPoseProviderId = null;
        for (BodyPoseEntry entry : BODY_POSES) {
            try {
                FlightBodyPose pose = entry.provider.pose(player, partialTicks);
                if (pose != null) {
                    lastBodyPoseProviderId = entry.id;
                    return pose;
                }
            } catch (RuntimeException error) {
                throw callbackFailure("body-pose:" + entry.id, "pose", error);
            }
        }
        lastBodyPoseProviderId = null;
        return null;
    }

    public static FlightHudAttitude queryHudAttitude(EntityPlayerSP player, float partialTicks) {
        lastHudAttitudeProviderId = null;
        for (HudAttitudeEntry entry : HUD_ATTITUDES) {
            try {
                FlightHudAttitude attitude = entry.provider.attitude(player, partialTicks);
                if (attitude != null) {
                    lastHudAttitudeProviderId = entry.id;
                    return attitude;
                }
            } catch (RuntimeException error) {
                throw callbackFailure("hud-attitude:" + entry.id, "attitude", error);
            }
        }
        lastHudAttitudeProviderId = null;
        return null;
    }

    public static boolean dispatchManeuverInput(FlightManeuverInput input) {
        Objects.requireNonNull(input, "input");
        lastManeuverHandlerId = null;
        for (ManeuverEntry entry : MANEUVER_HANDLERS) {
            try {
                if (entry.handler.handle(input)) {
                    lastManeuverHandlerId = entry.id;
                    return true;
                }
            } catch (RuntimeException error) {
                throw callbackFailure("maneuver:" + entry.id, "handle", error);
            }
        }
        lastManeuverHandlerId = null;
        return false;
    }

    public static FlightCameraTracking queryCameraTracking(EntityPlayerSP player,
                                                            float partialTicks) {
        lastCameraTrackingProviderId = null;
        for (CameraTrackingEntry entry : CAMERA_TRACKERS) {
            try {
                FlightCameraTracking tracking = entry.provider.tracking(player, partialTicks);
                if (tracking != null) {
                    lastCameraTrackingProviderId = entry.id;
                    return tracking;
                }
            } catch (RuntimeException error) {
                throw callbackFailure("camera-tracking:" + entry.id, "tracking", error);
            }
        }
        lastCameraTrackingProviderId = null;
        return null;
    }

    public static boolean renderHudComponent(String type, FlightHudRenderContext context,
                                             FlightHudElement element) {
        FlightHudComponent component = HUD_COMPONENTS.get(type);
        lastHudComponentType = null;
        if (component == null) return false;
        try {
            component.render(context, element);
            lastHudComponentType = type;
            return true;
        } catch (RuntimeException error) {
            throw callbackFailure("hud:" + type, "render", error);
        }
    }

    public static boolean hasHudComponent(String type) {
        return HUD_COMPONENTS.containsKey(type);
    }

    /** Immutable snapshot of namespaced component types currently exposed to theme JSON. */
    public static Set<String> registeredHudComponentTypes() {
        synchronized (HUD_COMPONENTS) {
            return Collections.unmodifiableSet(new java.util.LinkedHashSet<>(HUD_COMPONENTS.keySet()));
        }
    }

    /** Snapshot consumed by UIE's theme loader; returned map cannot mutate the registry. */
    public static Map<String, String> registeredHudThemes() {
        synchronized (HUD_THEMES) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(HUD_THEMES));
        }
    }

    public static FlightDiagnostics diagnostics() {
        return new FlightDiagnostics(lastCapabilityProviderId, lastControlProviderIds,
                lastBodyPoseProviderId, lastHudAttitudeProviderId, lastManeuverHandlerId,
                lastCameraTrackingProviderId, lastHudComponentType);
    }

    public static java.util.List<String> capabilityProviderIds() {
        return entryIds(CAPABILITIES, value -> value.id);
    }
    public static java.util.List<String> controlProviderIds() {
        return entryIds(CONTROLS, value -> value.id);
    }
    public static java.util.List<String> bodyPoseProviderIds() {
        return entryIds(BODY_POSES, value -> value.id);
    }
    public static java.util.List<String> hudAttitudeProviderIds() {
        return entryIds(HUD_ATTITUDES, value -> value.id);
    }
    public static java.util.List<String> maneuverHandlerIds() {
        return entryIds(MANEUVER_HANDLERS, value -> value.id);
    }
    public static java.util.List<String> cameraTrackingProviderIds() {
        return entryIds(CAMERA_TRACKERS, value -> value.id);
    }

    private static void removeCapability(String id) {
        CAPABILITIES.removeIf(value -> value.id.equals(id));
    }

    private static void removeControl(String id) {
        CONTROLS.removeIf(value -> value.id.equals(id));
    }

    private static FlightRegistration once(Runnable action) {
        return new FlightRegistration() {
            private boolean closed;
            @Override public synchronized void close() {
                if (closed) return;
                closed = true;
                action.run();
            }
        };
    }

    private static float finite(float value) { return Float.isFinite(value) ? value : 0.0F; }

    private static RuntimeException callbackFailure(String owner, String operation,
                                                    RuntimeException error) {
        LOGGER.error("Flight API callback {} failed for {}", operation, owner, error);
        return error;
    }

    private static <T> T backendCall(String operation, Supplier<T> callback) {
        try {
            return callback.get();
        } catch (RuntimeException error) {
            throw callbackFailure("backend", operation, error);
        }
    }

    private static void backendRun(String operation, Runnable callback) {
        try {
            callback.run();
        } catch (RuntimeException error) {
            throw callbackFailure("backend", operation, error);
        }
    }

    private static <T> java.util.List<String> entryIds(java.util.List<T> entries,
                                                       Function<T, String> id) {
        java.util.List<String> result = new java.util.ArrayList<>();
        for (T entry : entries) result.add(id.apply(entry));
        return Collections.unmodifiableList(result);
    }

    private static final class CapabilityEntry {
        final String id; final int priority; final FlightCapabilityProvider provider;
        CapabilityEntry(String id, int priority, FlightCapabilityProvider provider) {
            this.id = id; this.priority = priority; this.provider = provider;
        }
    }

    private static final class ControlEntry {
        final String id; final int priority; final FlightControlProvider provider;
        ControlEntry(String id, int priority, FlightControlProvider provider) {
            this.id = id; this.priority = priority; this.provider = provider;
        }
    }

    private static final class BodyPoseEntry {
        final String id; final int priority; final FlightBodyPoseProvider provider;
        BodyPoseEntry(String id, int priority, FlightBodyPoseProvider provider) {
            this.id = id; this.priority = priority; this.provider = provider;
        }
    }

    private static final class HudAttitudeEntry {
        final String id; final int priority; final FlightHudAttitudeProvider provider;
        HudAttitudeEntry(String id, int priority, FlightHudAttitudeProvider provider) {
            this.id = id; this.priority = priority; this.provider = provider;
        }
    }

    private static final class ManeuverEntry {
        final String id; final int priority; final FlightManeuverHandler handler;
        ManeuverEntry(String id, int priority, FlightManeuverHandler handler) {
            this.id = id; this.priority = priority; this.handler = handler;
        }
    }

    private static final class CameraTrackingEntry {
        final String id; final int priority; final FlightCameraTrackingProvider provider;
        CameraTrackingEntry(String id, int priority, FlightCameraTrackingProvider provider) {
            this.id = id; this.priority = priority; this.provider = provider;
        }
    }

    /** Implemented by UIE; exposed for loader-neutral bootstrapping and test doubles. */
    public interface Backend {
        Backend NOOP = new Backend() {};
        default FlightState state(float partialTicks) { return FlightState.INACTIVE; }
        default void setRoll(float degrees) {}
        default void rotate(float pitchDegrees, float yawDegrees, float rollDegrees) {}
        default boolean startBarrelRoll(int direction, int durationTicks) { return false; }
        default void resetOrientation() {}
        default void registriesChanged() {}
        default java.util.List<String> hudThemes() { return Collections.emptyList(); }
        default String selectedHudTheme() { return ""; }
        default boolean selectHudTheme(String id) { return false; }
        default float playerRoll(int entityId, float partialTicks) { return 0.0F; }
        default void updateRemotePlayerRoll(int entityId, boolean rolling, float degrees) {}
        default FlightHudCanvas hudCanvas() { return null; }
        default FlightRenderPose renderPose(EntityPlayerSP player, float partialTicks) { return null; }
    }
}
