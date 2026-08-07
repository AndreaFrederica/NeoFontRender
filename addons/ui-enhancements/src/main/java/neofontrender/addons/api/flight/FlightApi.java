package neofontrender.addons.api.flight;

import net.minecraft.client.entity.EntityPlayerSP;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Stable client flight facade for UIE integrations.
 *
 * <p>Registries accept namespaced ids, are safe to mutate during lifecycle events, and return
 * idempotent handles. Runtime orientation mutation must be performed on Minecraft's client thread.
 * Soft dependencies may use the matching Forge events instead of linking this class.</p>
 */
public final class FlightApi {
    public static final int API_VERSION = 2;
    private static final Logger LOGGER = LogManager.getLogger("Revo UI Flight API");
    private static final java.util.Set<String> REPORTED_FAILURES = ConcurrentHashMap.newKeySet();

    private static final CopyOnWriteArrayList<CapabilityEntry> CAPABILITIES =
            new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<ControlEntry> CONTROLS =
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
        backend.registriesChanged();
        return once(() -> {
            synchronized (HUD_COMPONENTS) {
                if (HUD_COMPONENTS.get(id) == component) HUD_COMPONENTS.remove(id);
            }
            backend.registriesChanged();
        });
    }

    /** Registers a complete schema-3 theme JSON document under a namespaced id. */
    public static FlightRegistration registerHudTheme(ResourceLocation id, String json) {
        Objects.requireNonNull(id, "id"); Objects.requireNonNull(json, "json");
        String key = id.toString();
        HUD_THEMES.put(key, json);
        backend.registriesChanged();
        return once(() -> {
            synchronized (HUD_THEMES) {
                if (json.equals(HUD_THEMES.get(key))) HUD_THEMES.remove(key);
            }
            backend.registriesChanged();
        });
    }

    public static int getApiVersion() { return API_VERSION; }
    public static boolean isAvailable() { return backend != Backend.NOOP; }
    public static FlightState getState(float partialTicks) { return backend.state(partialTicks); }
    public static boolean isActive() { return getState(1.0F).isActive(); }
    public static void setRoll(float degrees) { backend.setRoll(finite(degrees)); }
    public static void rotateView(float pitchDegrees, float yawDegrees, float rollDegrees) {
        backend.rotate(finite(pitchDegrees), finite(yawDegrees), finite(rollDegrees));
    }
    public static boolean startBarrelRoll(int direction, int durationTicks) {
        return backend.startBarrelRoll(direction < 0 ? -1 : 1,
                Math.max(1, Math.min(1200, durationTicks)));
    }
    public static float getPlayerRoll(int entityId, float partialTicks) {
        return backend.playerRoll(entityId, partialTicks);
    }
    public static void updateRemotePlayerRoll(int entityId, boolean rolling, float degrees) {
        backend.updateRemotePlayerRoll(entityId, rolling, finite(degrees));
    }
    public static void resetOrientation() { backend.resetOrientation(); }
    public static java.util.List<String> getAvailableHudThemes() { return backend.hudThemes(); }
    public static String getSelectedHudTheme() { return backend.selectedHudTheme(); }
    public static boolean selectHudTheme(String id) { return backend.selectHudTheme(id); }
    public static void reloadHudThemes() { backend.registriesChanged(); }

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
        for (CapabilityEntry entry : CAPABILITIES) {
            FlightDecision decision;
            try {
                decision = entry.provider.decide(player, capability, builtInDefault);
            } catch (RuntimeException ignored) {
                reportOnce("capability:" + entry.id, ignored);
                continue;
            }
            if (decision != null && decision != FlightDecision.PASS) return decision;
        }
        return FlightDecision.PASS;
    }

    public static void collectControlInput(FlightControlInput input) {
        for (ControlEntry entry : CONTROLS) {
            try { entry.provider.update(input); }
            catch (RuntimeException error) { reportOnce("control:" + entry.id, error); }
        }
    }

    public static boolean renderHudComponent(String type, FlightHudRenderContext context,
                                             FlightHudElement element) {
        FlightHudComponent component = HUD_COMPONENTS.get(type);
        if (component == null) return false;
        try {
            component.render(context, element);
            return true;
        } catch (RuntimeException error) {
            reportOnce("hud:" + type, error);
            return false;
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

    private static void reportOnce(String owner, RuntimeException error) {
        if (REPORTED_FAILURES.add(owner)) LOGGER.error("Suppressing future repeated failures from {}", owner, error);
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
    }
}
