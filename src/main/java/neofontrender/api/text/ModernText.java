package neofontrender.api.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable formatted text made of independently colored runs.
 *
 * <p>Run colors replace only the RGB channels supplied to {@link ModernTextApi}; the caller's
 * alpha is retained. Minecraft section-sign formatting inside a run is still interpreted by the
 * selected text backend.</p>
 */
public final class ModernText {
    private static final ModernText EMPTY = new ModernText(Collections.emptyList());

    private final List<Run> runs;

    private ModernText(List<Run> runs) {
        this.runs = Collections.unmodifiableList(runs);
    }

    public static ModernText empty() {
        return EMPTY;
    }

    public static ModernText of(String text) {
        if (text == null || text.isEmpty()) return EMPTY;
        return new ModernText(Collections.singletonList(new Run(text, false, 0)));
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<Run> runs() {
        return runs;
    }

    public boolean isEmpty() {
        return runs.isEmpty();
    }

    public static final class Run {
        private final String text;
        private final boolean colorOverride;
        private final int rgb;

        private Run(String text, boolean colorOverride, int rgb) {
            this.text = text;
            this.colorOverride = colorOverride;
            this.rgb = rgb & 0xFFFFFF;
        }

        public String text() {
            return text;
        }

        public boolean hasColorOverride() {
            return colorOverride;
        }

        public int rgb() {
            return rgb;
        }
    }

    public static final class Builder {
        private final List<Run> runs = new ArrayList<>();

        public Builder append(String text) {
            return append(text, false, 0);
        }

        public Builder append(String text, int rgb) {
            return append(text, true, rgb);
        }

        private Builder append(String text, boolean colorOverride, int rgb) {
            Objects.requireNonNull(text, "text");
            if (text.isEmpty()) return this;
            int normalizedRgb = rgb & 0xFFFFFF;
            if (!runs.isEmpty()) {
                Run previous = runs.get(runs.size() - 1);
                if (previous.colorOverride == colorOverride
                        && (!colorOverride || previous.rgb == normalizedRgb)) {
                    runs.set(runs.size() - 1,
                            new Run(previous.text + text, colorOverride, normalizedRgb));
                    return this;
                }
            }
            runs.add(new Run(text, colorOverride, normalizedRgb));
            return this;
        }

        public ModernText build() {
            if (runs.isEmpty()) return EMPTY;
            return new ModernText(new ArrayList<>(runs));
        }
    }
}
