package neofontrender.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable snapshot of configured font values and the currently active renderer. */
public final class FontState {
    private final String primaryFont;
    private final List<String> fallbackFonts;
    private final List<String> cosmicFaceOverrides;
    private final float size;
    private final FontStyle style;
    private final int variableWeight;
    private final RenderingEngine configuredEngine;
    private final boolean replacementActive;
    private final String activeBackend;

    FontState(String primaryFont, List<String> fallbackFonts, List<String> cosmicFaceOverrides,
              float size, FontStyle style,
              int variableWeight, RenderingEngine configuredEngine, boolean replacementActive,
              String activeBackend) {
        this.primaryFont = primaryFont;
        this.fallbackFonts = Collections.unmodifiableList(new ArrayList<>(fallbackFonts));
        this.cosmicFaceOverrides = Collections.unmodifiableList(new ArrayList<>(cosmicFaceOverrides));
        this.size = size;
        this.style = style;
        this.variableWeight = variableWeight;
        this.configuredEngine = configuredEngine;
        this.replacementActive = replacementActive;
        this.activeBackend = activeBackend;
    }

    public String getPrimaryFont() {
        return primaryFont;
    }

    public List<String> getFallbackFonts() {
        return fallbackFonts;
    }

    /** Regular, bold, italic and bold-italic Cosmic selectors, in that order. */
    public List<String> getCosmicFaceOverrides() {
        return cosmicFaceOverrides;
    }

    public float getSize() {
        return size;
    }

    public FontStyle getStyle() {
        return style;
    }

    public int getVariableWeight() {
        return variableWeight;
    }

    public RenderingEngine getConfiguredEngine() {
        return configuredEngine;
    }

    public boolean isReplacementActive() {
        return replacementActive;
    }

    public String getActiveBackend() {
        return activeBackend;
    }
}
