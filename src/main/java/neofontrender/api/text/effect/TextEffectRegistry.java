package neofontrender.api.text.effect;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Registry for generic structured text visual effects. */
public final class TextEffectRegistry {
    private static final Map<String, TextEffectDefinition> DEFINITIONS = new LinkedHashMap<>();

    static {
        register(new Builtin("brilliant_text:g", 0.0F));
        register(new Builtin("brilliant_text:s", 1.0F));
        register(new Builtin("brilliant_text:q", 2.0F));
        register(new Builtin("brilliant_text:v", 3.0F));
        register(new Builtin("brilliant_text:flame", 3.0F));
    }

    private TextEffectRegistry() {}

    public static synchronized TextEffectDefinition register(TextEffectDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        if (definition.id() == null || !definition.id().matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Text effect id must be namespaced: " + definition.id());
        }
        DEFINITIONS.put(definition.id(), definition);
        return definition;
    }

    public static synchronized TextEffectDefinition get(String id) {
        return DEFINITIONS.get(id);
    }

    public static synchronized TextEffectDefinition getOrDefault(String id) {
        TextEffectDefinition definition = DEFINITIONS.get(id);
        return definition != null ? definition : new Builtin(id, 0.0F);
    }

    public static synchronized Map<String, TextEffectDefinition> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(DEFINITIONS));
    }

    private static final class Builtin implements TextEffectDefinition {
        private final String id;
        private final float shaderType;

        private Builtin(String id, float shaderType) {
            this.id = id;
            this.shaderType = shaderType;
        }

        @Override public String id() { return id; }
        @Override public float shaderType() { return shaderType; }
    }
}
