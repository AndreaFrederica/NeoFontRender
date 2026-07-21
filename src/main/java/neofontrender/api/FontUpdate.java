package neofontrender.api;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Fluent, partial font update. Unspecified properties retain their current configuration values.
 */
public final class FontUpdate {
    String primaryFont;
    String cosmicRegularFont;
    String cosmicBoldFont;
    String cosmicItalicFont;
    String cosmicBoldItalicFont;
    List<String> fallbackFonts;
    Float size;
    FontStyle style;
    Integer variableWeight;
    RenderingEngine engine;
    Boolean enabled;
    boolean persist = true;

    FontUpdate() {}

    /** Select one primary font across all backends and clear Cosmic per-style overrides. */
    public FontUpdate font(String font) {
        primaryFont(font);
        return clearCosmicFaceOverrides();
    }

    public FontUpdate primaryFont(String font) {
        String normalized = normalizeFont(font);
        if (normalized.isEmpty()) throw new IllegalArgumentException("Primary font must not be empty");
        this.primaryFont = normalized;
        return this;
    }

    /** Configure Cosmic's optional regular/bold/italic/bold-italic face selectors. */
    public FontUpdate cosmicFaceOverrides(String regular, String bold, String italic, String boldItalic) {
        this.cosmicRegularFont = normalizeFont(regular);
        this.cosmicBoldFont = normalizeFont(bold);
        this.cosmicItalicFont = normalizeFont(italic);
        this.cosmicBoldItalicFont = normalizeFont(boldItalic);
        return this;
    }

    /** Clear Cosmic face overrides so its automatic family style matching uses the primary font. */
    public FontUpdate clearCosmicFaceOverrides() {
        return cosmicFaceOverrides("", "", "", "");
    }

    public FontUpdate fallbackFonts(String... fonts) {
        return fallbackFonts(fonts == null ? Collections.emptyList() : Arrays.asList(fonts));
    }

    public FontUpdate fallbackFonts(List<String> fonts) {
        List<String> normalized = new ArrayList<>();
        if (fonts != null) {
            for (String font : fonts) {
                String value = normalizeFont(font);
                if (!value.isEmpty() && !normalized.contains(value)) normalized.add(value);
            }
        }
        this.fallbackFonts = normalized;
        return this;
    }

    public FontUpdate size(float size) {
        if (!Float.isFinite(size)) throw new IllegalArgumentException("Font size must be finite");
        this.size = Math.max(4.0F, Math.min(64.0F, size));
        return this;
    }

    public FontUpdate style(FontStyle style) {
        if (style == null) throw new IllegalArgumentException("Font style must not be null");
        this.style = style;
        return this;
    }

    public FontUpdate variableWeight(int weight) {
        this.variableWeight = Math.max(0, Math.min(1000, weight));
        return this;
    }

    public FontUpdate engine(RenderingEngine engine) {
        if (engine == null) throw new IllegalArgumentException("Rendering engine must not be null");
        this.engine = engine;
        return this;
    }

    public FontUpdate enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /** Persist the update to the TOML config. Enabled by default. */
    public FontUpdate persist(boolean persist) {
        this.persist = persist;
        return this;
    }

    /** Schedule this update on Minecraft's client thread and reload the active font backend. */
    public ListenableFuture<?> apply() {
        return NeoFontRenderApi.apply(this);
    }

    private static String normalizeFont(String font) {
        return font == null ? "" : font.trim();
    }
}
