package neofontrender.addons.worldcreation;

/**
 * Value classification for 1.7.10 game rules.
 *
 * <p>1.7.10 keeps every rule as text and declares no value type, whereas the newer platform's
 * {@code GameRules.ValueType} is decided from the default text when a rule is registered. Reading
 * that same text back therefore reproduces the newer platform's answer for vanilla and modded
 * rules alike.</p>
 */
enum GameRuleValueType {
    BOOLEAN,
    NUMERIC,
    TEXT;

    static GameRuleValueType of(String defaultValue) {
        if (defaultValue == null) return TEXT;
        if ("true".equalsIgnoreCase(defaultValue) || "false".equalsIgnoreCase(defaultValue)) {
            return BOOLEAN;
        }
        return defaultValue.matches("-?\\d+(?:\\.\\d+)?") ? NUMERIC : TEXT;
    }
}
