package neofontrender.text.syntax;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import neofontrender.text.animation.TextAnimationRenderMode;

/** Extensible effect state emitted by syntax and consumed by post-processors. */
public final class EffectDescriptor {
    private final String groupId;
    private final String effectId;
    private final Map<String, String> parameters;
    private final Set<SyntaxEvent> terminationEvents;
    private final boolean lineWide;
    private final TextAnimationRenderMode animationRenderMode;
    private final boolean stackable;

    public EffectDescriptor(String groupId, String effectId, Map<String, String> parameters,
                            Set<SyntaxEvent> terminationEvents, boolean lineWide) {
        this(groupId, effectId, parameters, terminationEvents, lineWide,
                TextAnimationRenderMode.WHOLE_RUN, false);
    }

    public EffectDescriptor(String groupId, String effectId, Map<String, String> parameters,
                            Set<SyntaxEvent> terminationEvents, boolean lineWide,
                            TextAnimationRenderMode animationRenderMode) {
        this(groupId, effectId, parameters, terminationEvents, lineWide,
                animationRenderMode, false);
    }

    public EffectDescriptor(String groupId, String effectId, Map<String, String> parameters,
                            Set<SyntaxEvent> terminationEvents, boolean lineWide,
                            TextAnimationRenderMode animationRenderMode, boolean stackable) {
        this.groupId = requireId(groupId, "groupId");
        this.effectId = requireId(effectId, "effectId");
        this.parameters = parameters == null || parameters.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
        this.terminationEvents = terminationEvents == null || terminationEvents.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(terminationEvents));
        this.lineWide = lineWide;
        this.animationRenderMode = animationRenderMode == null
                ? TextAnimationRenderMode.WHOLE_RUN : animationRenderMode;
        this.stackable = stackable;
    }

    private static String requireId(String value, String name) {
        Objects.requireNonNull(value, name);
        if (!value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException(name + " must be namespaced: " + value);
        }
        return value;
    }

    public String groupId() { return groupId; }
    public String effectId() { return effectId; }
    public Map<String, String> parameters() { return parameters; }
    public Set<SyntaxEvent> terminationEvents() { return terminationEvents; }
    public boolean lineWide() { return lineWide; }
    public TextAnimationRenderMode animationRenderMode() { return animationRenderMode; }
    /** Whether the same effect group may be nested instead of replacing the active instance. */
    public boolean stackable() { return stackable; }
}
