package neofontrender.addons.inline;

import neofontrender.api.text.pipeline.InlineContent;
import neofontrender.api.text.pipeline.InlineContentMatch;
import neofontrender.api.text.pipeline.InlineContentMiddleware;
import neofontrender.api.text.pipeline.TextTrigger;
import neofontrender.addons.chat.EnhancedChatFeatures;

import javax.annotation.Nullable;
import java.net.URI;

/** Parses the deliberately explicit experimental syntax {@code <img:https://host/path>}. */
final class ExternalImageContentMiddleware implements InlineContentMiddleware {
    private static final String PREFIX = "<img:";
    private static final int MAX_TOKEN = 2048;

    @Override public String id() { return "neofontrender_ui_enhancements:external_image"; }
    @Override public int priority() { return 100; }
    @Override public TextTrigger trigger() { return TextTrigger.exact('<'); }
    @Override public boolean isEnabled() { return EnhancedChatFeatures.externalImageGlyphs(); }

    @Nullable
    @Override
    public InlineContentMatch match(CharSequence source, int sourceIndex) {
        if (!startsWith(source, sourceIndex, PREFIX)) return null;
        int limit = Math.min(source.length(), sourceIndex + MAX_TOKEN);
        int end = -1;
        for (int i = sourceIndex + PREFIX.length(); i < limit; i++) {
            if (source.charAt(i) == '>') { end = i; break; }
        }
        if (end < 0) return null;
        try {
            URI uri = URI.create(source.subSequence(sourceIndex + PREFIX.length(), end).toString());
            InlineContent glyph = InlineImageService.INSTANCE.glyph(uri,
                    "External image · " + uri.getHost(), false);
            return glyph == null ? null : new InlineContentMatch(sourceIndex, end + 1, glyph);
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
