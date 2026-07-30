package neofontrender.api.text;

import neofontrender.core.font.FontManager;
import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.core.font.support.FontRenderTuning;

/** Scoped text API. Unlike ModernTextApi, the caller chooses the backend and font family. */
public final class AdvancedTextApi {
    private AdvancedTextApi() {}

    public static boolean isAvailable(FontRenderSpec spec) {
        TextRenderBackend backend = FontManager.INSTANCE.getScopedTextBackend(spec);
        return backend != null && backend.isReady();
    }

    public static ModernTextLayout layoutFormatted(String text, int color, boolean shadow,
                                                    FontRenderSpec spec) {
        if (text == null || text.isEmpty() || spec == null
                || spec.backend() == FontRenderBackend.VANILLA) return ModernTextLayout.EMPTY;
        TextRenderBackend backend = FontManager.INSTANCE.getScopedTextBackend(spec);
        if (backend == null || !backend.isReady()) return ModernTextLayout.EMPTY;
        FontRenderTuning.updateFromCurrentGlState(shadow);
        TextRenderResult result = backend.renderFormattedAtSize(text, color, shadow, spec.size());
        return new ModernTextLayout(result, alpha(color));
    }

    public static float measureFormatted(String text, int color, boolean shadow,
                                         FontRenderSpec spec) {
        if (text == null || text.isEmpty() || spec == null
                || spec.backend() == FontRenderBackend.VANILLA) return 0.0F;
        TextRenderBackend backend = FontManager.INSTANCE.getScopedTextBackend(spec);
        return backend == null || !backend.isReady() ? 0.0F
                : backend.measureFormattedAtSize(text, color, shadow, spec.size());
    }

    public static float drawFormatted(String text, float x, float y, int color, boolean shadow,
                                      FontRenderSpec spec) {
        ModernTextLayout layout = layoutFormatted(text, color, shadow, spec);
        layout.draw(x, y);
        return layout.advance();
    }

    public static boolean drawWrapped(String text, int x, int y,
                                      int width, int color, FontRenderSpec spec) {
        if (text == null || text.isEmpty() || spec == null
                || spec.backend() == FontRenderBackend.VANILLA) return false;
        TextRenderBackend backend = FontManager.INSTANCE.getScopedTextBackend(spec);
        if (backend == null || !backend.isReady()) return false;
        FontRenderTuning.updateFromCurrentGlState(false);
        float alpha = ((color >>> 24) & 255) == 0 ? 1.0F : ((color >>> 24) & 255) / 255.0F;
        int lineY = y;
        for (String line : wrap(backend, text, width, color, spec.size())) {
            TextRenderResult result = backend.renderFormattedAtSize(line, color, false, spec.size());
            result.draw(x, lineY, alpha);
            lineY += Math.max(1, Math.round(spec.size() + 1.0F));
        }
        return true;
    }

    private static float alpha(int color) {
        int value = color >>> 24;
        return value == 0 ? 1.0F : value / 255.0F;
    }

    private static java.util.List<String> wrap(TextRenderBackend backend, String text, int width,
                                                int color, float size) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && backend.measureFormattedAtSize(candidate, color, false, size) > width) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) lines.add(line.toString());
        if (lines.isEmpty()) lines.add("");
        return lines;
    }
}
