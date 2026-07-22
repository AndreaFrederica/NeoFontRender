package neofontrender.addons.hud.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registry used by optional integrations to add bars without taking over Forge's HUD event. */
public final class HudBarRegistry {
    private static final Map<String, HudBarProvider> PROVIDERS = new LinkedHashMap<>();

    private HudBarRegistry() {}

    public static synchronized void register(HudBarProvider provider) {
        if (provider == null) throw new IllegalArgumentException("provider must not be null");
        String id = validateId(provider.id());
        if (provider.element() == null || provider.side() == null)
            throw new IllegalArgumentException("element and side must not be null");
        if (PROVIDERS.containsKey(id)) throw new IllegalStateException("HUD bar already registered: " + id);
        PROVIDERS.put(id, provider);
    }

    public static synchronized boolean unregister(String id) {
        return PROVIDERS.remove(validateId(id)) != null;
    }

    public static synchronized List<HudBarProvider> snapshot(HudBarElement element) {
        List<HudBarProvider> result = new ArrayList<>();
        for (HudBarProvider provider : PROVIDERS.values()) if (provider.element() == element) result.add(provider);
        result.sort(Comparator.comparingInt(HudBarProvider::order).thenComparing(HudBarProvider::id));
        return result;
    }

    private static String validateId(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_.-]+"))
            throw new IllegalArgumentException("bar id must be namespaced, for example modid:mana");
        return id;
    }
}
