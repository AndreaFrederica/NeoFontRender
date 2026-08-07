package neofontrender.addons.inline;

import neofontrender.addons.api.inline.InlineGlyphRegistry;

/** Registers UIE-owned providers while leaving the registry open to third-party providers. */
public final class InlineGlyphMiddleware {
    private static boolean initialized;

    private InlineGlyphMiddleware() {}

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        InlineGlyphRegistry.register(new ExternalImageGlyphProvider(), 100);
        InlineGlyphRegistry.register(new LocalImageGlyphProvider(), 75);
        InlineGlyphRegistry.register(new GoslingEmojiGlyphProvider(), 50);
        LocalImageCatalog.INSTANCE.initialize();
    }

    public static java.util.List<String> emojiSuggestions(String prefix, int maximum) {
        int limit = Math.max(1, maximum);
        java.util.LinkedHashSet<String> combined = new java.util.LinkedHashSet<>();
        if (neofontrender.addons.chat.EnhancedChatFeatures.localImageGlyphs()) {
            combined.addAll(LocalImageCatalog.INSTANCE.suggestions(prefix, limit));
        }
        if (neofontrender.addons.chat.EnhancedChatFeatures.goslingImageGlyphs()) {
            combined.addAll(GoslingEmojiCatalog.INSTANCE.suggestions(prefix, limit));
        }
        java.util.ArrayList<String> result = new java.util.ArrayList<>(combined);
        return result.size() <= limit ? result : result.subList(0, limit);
    }
}
