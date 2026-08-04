package neofontrender.addons.inline;

import neofontrender.addons.api.inline.InlineGlyph;
import neofontrender.addons.api.inline.InlineGlyphMatch;
import neofontrender.addons.api.inline.InlineGlyphProvider;
import neofontrender.addons.chat.EnhancedChatFeatures;

import javax.annotation.Nullable;
import java.net.URI;

/** Parses the deliberately explicit experimental syntax {@code <img:https://host/path>}. */
final class ExternalImageGlyphProvider implements InlineGlyphProvider {
    private static final String PREFIX = "<img:";
    private static final int MAX_TOKEN = 2048;

    @Nullable
    @Override
    public InlineGlyphMatch match(CharSequence source, int sourceIndex) {
        if (!EnhancedChatFeatures.externalImageGlyphs()
                || !startsWith(source, sourceIndex, PREFIX)) return null;
        int limit = Math.min(source.length(), sourceIndex + MAX_TOKEN);
        int end = -1;
        for (int i = sourceIndex + PREFIX.length(); i < limit; i++) {
            if (source.charAt(i) == '>') { end = i; break; }
        }
        if (end < 0) return null;
        try {
            URI uri = URI.create(source.subSequence(sourceIndex + PREFIX.length(), end).toString());
            InlineGlyph glyph = InlineImageService.INSTANCE.glyph(uri,
                    "External image · " + uri.getHost(), false);
            return glyph == null ? null : new InlineGlyphMatch(sourceIndex, end + 1, glyph);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean startsWith(CharSequence source, int index, String value) {
        if (index < 0 || index + value.length() > source.length()) return false;
        for (int i = 0; i < value.length(); i++) {
            if (source.charAt(index + i) != value.charAt(i)) return false;
        }
        return true;
    }
}
