package neofontrender.addons.inline;

import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.text.StructuredTextApi;
import neofontrender.uie.text.v3.UiEnhancementsTextPlugin;

/** Registers UIE-owned providers while leaving the registry open to third-party providers. */
public final class TextPipelineMiddleware {
    private static boolean initialized;

    private TextPipelineMiddleware() {}

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        StructuredTextApi.register(new UiEnhancementsTextPlugin(
                new UiEnhancementsTextPlugin.Config(
                        EmbeddedContentConfig::markdownEnabled,
                        EmbeddedContentConfig::latexEnabled,
                        EmbeddedContentConfig::svgEnabled,
                        neofontrender.addons.chat.EnhancedChatFeatures::localImageGlyphs,
                        neofontrender.addons.chat.EnhancedChatFeatures::externalImageGlyphs,
                        neofontrender.addons.chat.EnhancedChatFeatures::goslingImageGlyphs,
                        LocalImageCatalog.galleryRoot(
                                net.minecraft.client.Minecraft.getMinecraft().gameDir.toPath()),
                        EmbeddedContentConfig.latexOversample(),
                        EmbeddedContentConfig.fullSvgEnabled(),
                        EmbeddedContentConfig::latexMatchLineHeight,
                        TextPipelineMiddleware::latexFontSelectors,
                        EmbeddedContentConfig::rasterCacheEntries,
                        EmbeddedContentConfig::rasterCacheMegapixels,
                        neofontrender.api.text.route.TextRenderRouteApi::invalidate)));
        StructuredTextApi.register(UiEInlineContentResolver.INSTANCE);
        LocalImageCatalog.INSTANCE.initialize();
        NfrUiEnhancements.LOGGER.info(
                "Initialized text pipeline middleware (LaTeX={}, SVG={}, full SVG={}, Markdown={})",
                EmbeddedContentConfig.latexEnabled(), EmbeddedContentConfig.svgEnabled(),
                EmbeddedContentConfig.fullSvgEnabled(), EmbeddedContentConfig.markdownEnabled());
    }

    private static java.util.List<String> latexFontSelectors() {
        String configured = EmbeddedContentConfig.latexFontFamily();
        if (!configured.isEmpty()) return java.util.Collections.singletonList(configured);
        java.util.ArrayList<String> selectors = new java.util.ArrayList<>();
        selectors.add(EmbeddedContentFonts.FIRA_MATH_LOCATION);
        selectors.addAll(neofontrender.core.config.NeofontrenderConfig.fontFamily());
        return selectors;
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
