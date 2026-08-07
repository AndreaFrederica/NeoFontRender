package neofontrender.addons.inline;

import neofontrender.addons.api.inline.InlineGlyph;
import neofontrender.addons.api.inline.InlineGlyphMatch;
import neofontrender.addons.api.inline.InlineGlyphProvider;
import neofontrender.addons.chat.EnhancedChatFeatures;

import javax.annotation.Nullable;
import java.nio.file.Path;

/** Maps local gallery filenames to portable {@code :alias:} tokens. */
final class LocalImageGlyphProvider implements InlineGlyphProvider {
    private static final int MAX_ALIAS = 128;

    @Nullable
    @Override public InlineGlyphMatch match(CharSequence source, int sourceIndex) {
        if (!EnhancedChatFeatures.localImageGlyphs() || source.charAt(sourceIndex) != ':') return null;
        int limit = Math.min(source.length(), sourceIndex + MAX_ALIAS);
        for (int end = sourceIndex + 1; end < limit; end++) {
            if (source.charAt(end) != ':') continue;
            String alias = source.subSequence(sourceIndex + 1, end).toString();
            Path image = LocalImageCatalog.INSTANCE.image(alias);
            if (image == null) return null;
            InlineGlyph glyph = InlineImageService.INSTANCE.localGlyph(image,
                    ":" + alias + ": · local gallery");
            return glyph == null ? null : new InlineGlyphMatch(sourceIndex, end + 1, glyph);
        }
        return null;
    }
}
