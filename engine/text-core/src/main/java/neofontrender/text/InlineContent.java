package neofontrender.text;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Renderer-independent inline object descriptor with an optional resolved CPU raster. */
public final class InlineContent {
    private final String kind;
    private final String key;
    private final String description;
    private final int displayHeight;
    private final boolean tint;
    private final boolean matchLineHeight;
    private final InlineRaster raster;
    private final Map<String, String> attributes;
    private final InlineLayout layout;

    public InlineContent(String kind, String key, String description, int displayHeight,
                         boolean tint, boolean matchLineHeight, InlineRaster raster,
                         Map<String, String> attributes) {
        this(kind, key, description, tint, raster, attributes,
                InlineLayout.legacy(displayHeight, matchLineHeight));
    }

    public InlineContent(String kind, String key, String description, boolean tint,
                         InlineRaster raster, Map<String, String> attributes,
                         InlineLayout layout) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.key = Objects.requireNonNull(key, "key");
        this.description = description == null ? key : description;
        this.layout = layout == null ? InlineLayout.oneLine() : layout;
        this.displayHeight = Math.max(1, Math.round(this.layout.hasFixedHeight()
                ? this.layout.fixedHeight() : this.layout.rows() * 18.0F));
        this.tint = tint;
        this.matchLineHeight = !this.layout.hasFixedHeight()
                && Float.compare(this.layout.rows(), 1.0F) == 0;
        this.raster = raster;
        this.attributes = attributes == null || attributes.isEmpty() ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    public String kind() { return kind; }
    public String key() { return key; }
    public String description() { return description; }
    public int displayHeight() { return displayHeight; }
    public boolean tint() { return tint; }
    public boolean matchLineHeight() { return matchLineHeight; }
    public InlineRaster raster() { return raster; }
    public Map<String, String> attributes() { return attributes; }
    public InlineLayout layout() { return layout; }
    public boolean resolved() { return raster != null; }
}
