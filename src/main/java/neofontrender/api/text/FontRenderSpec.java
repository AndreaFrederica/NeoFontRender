package neofontrender.api.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable font/backend selection for one scoped render operation. */
public final class FontRenderSpec {
    private final FontRenderBackend backend;
    private final List<String> fonts;
    private final float size;

    private FontRenderSpec(FontRenderBackend backend, List<String> fonts, float size) {
        this.backend = backend == null ? FontRenderBackend.AUTO : backend;
        List<String> normalized = new ArrayList<>();
        if (fonts != null) for (String font : fonts) {
            if (font != null && !font.trim().isEmpty() && !normalized.contains(font.trim())) {
                normalized.add(font.trim());
            }
        }
        this.fonts = Collections.unmodifiableList(normalized);
        this.size = Float.isFinite(size) ? Math.max(1.0F, Math.min(256.0F, size)) : 8.0F;
    }

    public FontRenderBackend backend() { return backend; }
    public List<String> fonts() { return fonts; }
    public float size() { return size; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private FontRenderBackend backend = FontRenderBackend.AUTO;
        private final List<String> fonts = new ArrayList<>();
        private float size = 8.0F;
        public Builder backend(FontRenderBackend value) { backend = value; return this; }
        public Builder font(String value) { if (value != null) fonts.add(value); return this; }
        public Builder fonts(Iterable<String> values) { if (values != null) for (String value : values) font(value); return this; }
        public Builder size(float value) { size = value; return this; }
        public FontRenderSpec build() { return new FontRenderSpec(backend, fonts, size); }
    }
}
