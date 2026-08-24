package neofontrender.addons.worldcreation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Carries the game-rule values chosen on the create-world screen to the new world's
 * {@code WorldInfo}. The screen stashes values on the client thread when "Create New
 * World" is pressed; the integrated server thread consumes them while constructing
 * {@code WorldInfo(WorldSettings, String)} (only invoked for brand-new saves), so
 * loading an existing world is never affected.
 */
public final class CreateWorldGameRulesState {
    private static volatile Map<String, String> pending;

    private CreateWorldGameRulesState() {}

    public static void setPending(Map<String, String> rules) {
        pending = rules == null || rules.isEmpty() ? null : new LinkedHashMap<>(rules);
    }

    public static void clearPending() {
        pending = null;
    }

    /** Returns the stashed values exactly once; subsequent calls return {@code null}. */
    public static Map<String, String> consumePending() {
        Map<String, String> rules = pending;
        pending = null;
        return rules;
    }
}
