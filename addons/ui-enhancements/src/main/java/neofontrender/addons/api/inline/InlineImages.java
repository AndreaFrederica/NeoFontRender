package neofontrender.addons.api.inline;

import neofontrender.addons.inline.InlineImageService;

import javax.annotation.Nullable;
import java.net.URI;

/** Safe public entry point for user-configured external image glyphs. */
public final class InlineImages {
    private InlineImages() {}

    /** Returns {@code null} unless the URL passes the current experimental allow/deny policy. */
    @Nullable
    public static InlineGlyph external(URI uri, String description) {
        return InlineImageService.INSTANCE.glyph(uri, description, false);
    }

    /** Resolves an alias from the client-owned gallery when local images are enabled. */
    @Nullable
    public static InlineGlyph local(String alias, String description) {
        return InlineImageService.INSTANCE.localGlyph(alias, description);
    }
}
