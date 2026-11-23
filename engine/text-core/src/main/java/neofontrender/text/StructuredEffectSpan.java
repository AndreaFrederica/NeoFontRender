package neofontrender.text;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import neofontrender.text.animation.TextAnimationRenderMode;

/** Namespaced post-processing effect over a half-open plain-text range. */
public final class StructuredEffectSpan {
    private final int start;
    private final int end;
    private final String effectId;
    private final Map<String, String> parameters;
    private final boolean lineWide;
    private final TextAnimationRenderMode animationRenderMode;

    public StructuredEffectSpan(int start, int end, String effectId,
                                Map<String, String> parameters, boolean lineWide) {
        this(start, end, effectId, parameters, lineWide, TextAnimationRenderMode.WHOLE_RUN);
    }

    public StructuredEffectSpan(int start, int end, String effectId,
                                Map<String, String> parameters, boolean lineWide,
                                TextAnimationRenderMode animationRenderMode) {
        if (start < 0 || end < start) throw new IllegalArgumentException("Invalid effect range");
        this.start = start;
        this.end = end;
        this.effectId = Objects.requireNonNull(effectId, "effectId");
        this.parameters = parameters == null || parameters.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
        this.lineWide = lineWide;
        this.animationRenderMode = animationRenderMode == null
                ? TextAnimationRenderMode.WHOLE_RUN : animationRenderMode;
    }

    public int start() { return start; }
    public int end() { return end; }
    public String effectId() { return effectId; }
    public Map<String, String> parameters() { return parameters; }
    public boolean lineWide() { return lineWide; }
    public TextAnimationRenderMode animationRenderMode() { return animationRenderMode; }

    @Override public String toString() {
        return '[' + Integer.toString(start) + ',' + end + ") " + effectId;
    }
}
