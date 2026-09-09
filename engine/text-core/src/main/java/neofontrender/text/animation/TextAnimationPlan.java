package neofontrender.text.animation;

import neofontrender.text.StructuredText;

/** Backend-neutral decision describing how an animated text value may be rendered safely. */
public final class TextAnimationPlan {
    private final TextAnimationRenderMode requested;
    private final TextAnimationRenderMode selected;
    private final String reason;

    private TextAnimationPlan(TextAnimationRenderMode requested,
                              TextAnimationRenderMode selected, String reason) {
        this.requested = requested;
        this.selected = selected;
        this.reason = reason;
    }

    public static TextAnimationPlan forText(StructuredText text, TextAnimationRenderMode mode) {
        return forText(text, mode, false);
    }

    /**
     * @param clusterAware true when the backend can expose its actual shaping clusters and animate
     *                     one cluster without splitting emoji, ligatures, or combining sequences.
     */
    public static TextAnimationPlan forText(StructuredText text, TextAnimationRenderMode mode,
                                            boolean clusterAware) {
        TextAnimationRenderMode requested = mode == null ? TextAnimationRenderMode.AUTO : mode;
        if (text == null || text.effects().isEmpty()) {
            return new TextAnimationPlan(requested, TextAnimationRenderMode.WHOLE_RUN, "no_effects");
        }
        if (requested == TextAnimationRenderMode.WHOLE_RUN) {
            return new TextAnimationPlan(requested, requested, "requested");
        }
        boolean glyphEffect = false;
        for (neofontrender.text.StructuredEffectSpan effect : text.effects()) {
            if (effect.animationRenderMode() == TextAnimationRenderMode.GLYPH) {
                glyphEffect = true;
                break;
            }
        }
        if (!glyphEffect) {
            return new TextAnimationPlan(requested, TextAnimationRenderMode.WHOLE_RUN,
                    "effects_request_whole_run");
        }
        if (!text.inlineSpans().isEmpty()) {
            return new TextAnimationPlan(requested, TextAnimationRenderMode.WHOLE_RUN, "inline_content");
        }
        if (clusterAware) {
            return new TextAnimationPlan(requested, TextAnimationRenderMode.GLYPH,
                    requested == TextAnimationRenderMode.AUTO ? "native_clusters" : "requested_clusters");
        }
        String value = text.plainText();
        for (neofontrender.text.StructuredEffectSpan effect : text.effects()) {
            if (effect.animationRenderMode() != TextAnimationRenderMode.GLYPH) continue;
            for (int index = effect.start(); index < effect.end();) {
                int codePoint = value.codePointAt(index);
                if (!safeIndependentGlyph(codePoint)) {
                    return new TextAnimationPlan(requested, TextAnimationRenderMode.WHOLE_RUN,
                            "shaping_sensitive_codepoint");
                }
                index += Character.charCount(codePoint);
            }
        }
        return new TextAnimationPlan(requested, TextAnimationRenderMode.GLYPH,
                requested == TextAnimationRenderMode.AUTO ? "auto_safe" : "requested_safe");
    }

    private static boolean safeIndependentGlyph(int codePoint) {
        if (codePoint > 0xFFFF && !isCjkIdeograph(codePoint)) return false;
        int type = Character.getType(codePoint);
        if (type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK || type == Character.FORMAT) return false;
        // Keep emoji, variation selectors, joiners, and symbol sequences on the
        // backend's native shaping path. This is deliberately conservative.
        if ((codePoint >= 0x200D && codePoint <= 0x200F)
                || (codePoint >= 0xFE00 && codePoint <= 0xFE0F)
                || (codePoint >= 0x1F000 && codePoint <= 0x1FAFF)) return false;
        return !(codePoint >= 0x2190 && codePoint <= 0x2BFF);
    }

    private static boolean isCjkIdeograph(int codePoint) {
        return (codePoint >= 0x3400 && codePoint <= 0x4DBF)
                || (codePoint >= 0x4E00 && codePoint <= 0x9FFF)
                || (codePoint >= 0xF900 && codePoint <= 0xFAFF)
                || (codePoint >= 0x20000 && codePoint <= 0x2FA1F);
    }

    public TextAnimationRenderMode requested() { return requested; }
    public TextAnimationRenderMode selected() { return selected; }
    public String reason() { return reason; }
    public boolean usesGlyphs() { return selected == TextAnimationRenderMode.GLYPH; }
}
