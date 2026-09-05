package neofontrender.api.text.route;

import net.minecraft.client.gui.FontRenderer;
import neofontrender.text.StructuredText;

import java.util.Objects;

/** Immutable input shared by route selection and all layout operations. */
public final class TextRenderRouteRequest {
    private final FontRenderer font;
    private final String source;
    private final StructuredText structuredText;
    private final float fontSize;
    private final int argb;
    private final boolean shadow;
    private final boolean sourceTransformed;

    public TextRenderRouteRequest(FontRenderer font, String source, StructuredText structuredText,
                                  float fontSize, int argb, boolean shadow,
                                  boolean sourceTransformed) {
        this.font = Objects.requireNonNull(font, "font");
        this.source = source == null ? "" : source;
        this.structuredText = Objects.requireNonNull(structuredText, "structuredText");
        this.fontSize = Float.isFinite(fontSize) ? Math.max(1.0F, fontSize) : 8.0F;
        this.argb = argb;
        this.shadow = shadow;
        this.sourceTransformed = sourceTransformed;
    }

    public FontRenderer font() { return font; }
    public String source() { return source; }
    public StructuredText structuredText() { return structuredText; }
    public float fontSize() { return fontSize; }
    public int argb() { return argb; }
    public boolean shadow() { return shadow; }
    public boolean sourceTransformed() { return sourceTransformed; }

    /** True when vanilla cannot reproduce the parsed result by receiving the original string. */
    public boolean requiresStructuredRendering() {
        return requiresStructuredRendering(structuredText, sourceTransformed);
    }

    public static boolean requiresStructuredRendering(StructuredText text,
                                                      boolean sourceTransformed) {
        Objects.requireNonNull(text, "text");
        if (sourceTransformed || !text.inlineSpans().isEmpty()
                || !text.appliedMiddlewareIds().isEmpty()
                || !text.effects().isEmpty()) {
            return true;
        }
        for (String id : text.appliedSyntaxProviderIds()) {
            if (!"minecraft:legacy_formatting".equals(id)) return true;
        }
        return false;
    }
}
