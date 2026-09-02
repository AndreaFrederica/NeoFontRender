package neofontrender.addons.inline;

import neofontrender.api.text.pipeline.TextPipelineApi;
import neofontrender.addons.ui.NfrUiEnhancements;

/** Registers UIE-owned providers while leaving the registry open to third-party providers. */
public final class TextPipelineMiddleware {
    private static boolean initialized;

    private TextPipelineMiddleware() {}

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        TextPipelineApi.register(new ExternalImageContentMiddleware());
        TextPipelineApi.register(new SvgContentMiddleware());
        TextPipelineApi.register(new LatexContentMiddleware());
        TextPipelineApi.register(new LocalImageContentMiddleware());
        TextPipelineApi.register(new MarkdownTextMiddleware());
        TextPipelineApi.register(new GoslingEmojiContentMiddleware());
        LocalImageCatalog.INSTANCE.initialize();
        NfrUiEnhancements.LOGGER.info(
                "Initialized text pipeline middleware (LaTeX={}, SVG={}, full SVG={}, Markdown={})",
                EmbeddedContentConfig.latexEnabled(), EmbeddedContentConfig.svgEnabled(),
                EmbeddedContentConfig.fullSvgEnabled(), EmbeddedContentConfig.markdownEnabled());
    }

    public static java.util.List<String> emojiSuggestions(String prefix, int maximum) {
        int limit = Math.max(1, maximum);
        java.util.LinkedHashSet<String> combined = new java.util.LinkedHashSet<>();
        boolean localRaster = neofontrender.addons.chat.EnhancedChatFeatures.localImageGlyphs();
        boolean localSvg = EmbeddedContentConfig.svgEnabled();
        if (localRaster || localSvg) {
            combined.addAll(LocalImageCatalog.INSTANCE.suggestions(
                    prefix, limit, localRaster, localSvg));
        }
        if (neofontrender.addons.chat.EnhancedChatFeatures.goslingImageGlyphs()) {
            combined.addAll(GoslingEmojiCatalog.INSTANCE.suggestions(prefix, limit));
        }
        java.util.ArrayList<String> result = new java.util.ArrayList<>(combined);
        return result.size() <= limit ? result : result.subList(0, limit);
    }
}
