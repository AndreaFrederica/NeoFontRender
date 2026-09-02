package neofontrender.addons.inline;

import neofontrender.api.text.pipeline.InlineContent;
import neofontrender.api.text.pipeline.InlineContentMatch;
import neofontrender.api.text.pipeline.InlineContentMiddleware;
import neofontrender.api.text.pipeline.TextTrigger;
import neofontrender.addons.chat.EnhancedChatFeatures;

import javax.annotation.Nullable;
import java.nio.file.Path;

/** Maps local gallery filenames to portable {@code :alias:} tokens. */
final class LocalImageContentMiddleware implements InlineContentMiddleware {
    private static final int MAX_ALIAS = 128;

    @Override public boolean isEnabled() { return EnhancedChatFeatures.localImageGlyphs(); }

    @Nullable
    @Override public InlineContentMatch match(CharSequence source, int sourceIndex) {
        if (source.charAt(sourceIndex) != ':') return null;
        int limit = Math.min(source.length(), sourceIndex + MAX_ALIAS);
        for (int end = sourceIndex + 1; end < limit; end++) {
            if (source.charAt(end) != ':') continue;
            String alias = source.subSequence(sourceIndex + 1, end).toString();
            Path image = LocalImageCatalog.INSTANCE.image(alias);
            if (image == null || LocalImageCatalog.isSvg(image)) return null;
            InlineContent glyph = InlineImageService.INSTANCE.localGlyph(image,
                    ":" + alias + ": · local gallery");
            return glyph == null ? null : new InlineContentMatch(sourceIndex, end + 1, glyph);
        }
        return null;
    }

    @Override public String id() { return "neofontrender_ui_enhancements:local_image"; }
    @Override public int priority() { return 75; }
    @Override public TextTrigger trigger() { return TextTrigger.exact(':'); }
}
