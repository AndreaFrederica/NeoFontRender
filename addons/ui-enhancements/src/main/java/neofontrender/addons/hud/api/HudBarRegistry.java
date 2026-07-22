package neofontrender.addons.hud.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Registry through which built-in and optional integrations contribute deterministic HUD bars. */
public final class HudBarRegistry {
    private static final Logger LOGGER = LogManager.getLogger("NFR UI Enhancements/HUD Registry");
    private static final Map<String, HudBarRegistration> PROVIDERS =
            new LinkedHashMap<String, HudBarRegistration>();
    private static final ThreadLocal<Boolean> MATERIALIZING = new ThreadLocal<Boolean>();

    private HudBarRegistry() {}

    /** Registers one provider and rejects invalid or duplicate identifiers immediately. */
    public static void register(HudBarProvider provider) {
        if (provider == null) throw new IllegalArgumentException("provider must not be null");
        ensureMutationAllowed();
        HudBarRegistration registration = materialize(provider);
        synchronized (PROVIDERS) {
            if (PROVIDERS.containsKey(registration.id())) {
                throw new IllegalStateException("HUD bar already registered: " + registration.id());
            }
            PROVIDERS.put(registration.id(), registration);
        }
    }

    /** Removes the provider with the supplied valid namespaced identifier. */
    public static boolean unregister(String id) {
        ensureMutationAllowed();
        String validatedId = validateId(id);
        synchronized (PROVIDERS) {
            return PROVIDERS.remove(validatedId) != null;
        }
    }

    /** Returns an isolated metadata snapshot sorted without invoking third-party provider callbacks. */
    public static List<HudBarRegistration> snapshot(HudBarElement element) {
        if (element == null) throw new IllegalArgumentException("element must not be null");
        List<HudBarRegistration> registrations;
        synchronized (PROVIDERS) {
            registrations = new ArrayList<HudBarRegistration>(PROVIDERS.values());
        }
        List<HudBarRegistration> result = new ArrayList<HudBarRegistration>();
        for (HudBarRegistration registration : registrations) {
            if (registration.element() == element) result.add(registration);
        }
        result.sort(Comparator.comparingInt(HudBarRegistration::order).thenComparing(HudBarRegistration::id));
        return result;
    }

    private static HudBarRegistration materialize(HudBarProvider provider) {
        MATERIALIZING.set(Boolean.TRUE);
        String id = null;
        try {
            id = validateId(provider.id());
            HudBarElement element = provider.element();
            if (element == null) throw new IllegalArgumentException("element must not be null");
            HudBarSide side = provider.side();
            if (side == null) throw new IllegalArgumentException("side must not be null");
            int order = provider.order();
            boolean replacesVanilla = provider.replacesVanilla();
            return new HudBarRegistration(provider, id, element, side, order, replacesVanilla);
        } catch (RuntimeException | LinkageError exception) {
            String identity = id == null ? provider.getClass().getName() : id;
            LOGGER.error("Failed to materialize HUD provider metadata for '{}'; registration rejected",
                    identity, exception);
            throw exception;
        } finally {
            MATERIALIZING.remove();
        }
    }

    private static void ensureMutationAllowed() {
        if (Boolean.TRUE.equals(MATERIALIZING.get())) {
            throw new IllegalStateException("HUD registry mutation from a provider metadata callback is not allowed");
        }
    }

    private static String validateId(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("bar id must be namespaced, for example modid:mana");
        }
        return id;
    }
}
