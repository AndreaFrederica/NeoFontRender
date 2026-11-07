package neofontrender.addons.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Central registry for mod-compatibility decisions. Add new {@link ModCompat} instances here. */
public final class ModCompatRegistry {
    private static final List<ModCompat> COMPATS = new ArrayList<>();

    static {
        register(new BTFixesCompat());
        register(new CustomMainMenuCompat());
        register(new FancyMenuCompat());
        // Future compats: register(new SomeModCompat());
    }

    private ModCompatRegistry() {}

    public static void register(ModCompat compat) {
        COMPATS.add(compat);
    }

    /** @return all registered compat rules. */
    public static List<ModCompat> all() {
        return Collections.unmodifiableList(COMPATS);
    }

    /** @return only the compats active in the current environment. */
    public static List<ModCompat> active() {
        List<ModCompat> active = new ArrayList<>();
        for (ModCompat compat : COMPATS) {
            if (compat.isActive()) active.add(compat);
        }
        return Collections.unmodifiableList(active);
    }

    /**
     * @param mixinClassName fully-qualified mixin class name
     * @return false if any active compat forbids applying the mixin
     */
    public static boolean shouldApplyMixin(String mixinClassName) {
        for (ModCompat compat : COMPATS) {
            // Runtime-only rules (CMM/FancyMenu) must never query Forge's Loader during
            // mixin selection, before mod discovery has initialized its namedMods map.
            if (!compat.shouldApplyMixin(mixinClassName) && compat.isActive()) {
                return false;
            }
        }
        return true;
    }
}
