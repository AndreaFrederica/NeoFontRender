package neofontrender.addons.compat;

import java.util.Collections;
import java.util.List;

/**
 * One externally-triggered compatibility entry. Each implementation detects a specific mod or
 * environment condition and decides whether a given mixin should still be applied.
 */
public interface ModCompat {
    /** Internal identifier, e.g. "btfixes". */
    String id();

    /** Human-readable name shown in the diagnostics page. */
    String displayName();

    /** @return true when this compatibility rule is active in the current environment. */
    boolean isActive();

    /** Describes what this compat changes (disabled mixins, replaced behaviour, etc.). */
    default List<CompatImpact> impacts() {
        return Collections.emptyList();
    }

    /**
     * @param mixinClassName fully-qualified mixin class name
     * @return false to prevent the mixin from being applied
     */
    default boolean shouldApplyMixin(String mixinClassName) {
        return true;
    }
}
