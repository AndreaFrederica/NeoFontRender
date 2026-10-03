package neofontrender.addons.inline;

import neofontrender.api.text.route.InlineContentResolver;
import neofontrender.text.InlineContent;

import java.net.URI;

/** Minecraft-side resolver for UIE descriptors whose bytes arrive asynchronously. */
final class UiEInlineContentResolver implements InlineContentResolver {
    static final UiEInlineContentResolver INSTANCE = new UiEInlineContentResolver();

    private UiEInlineContentResolver() {}

    @Override public String id() {
        return "neofontrender_ui_enhancements:remote_raster";
    }

    @Override public int priority() { return 100; }

    @Override
    public boolean supports(InlineContent content) {
        return content != null && !content.resolved()
                && ("external_image".equals(content.kind())
                || "gosling_emoji".equals(content.kind()));
    }

    @Override
    public InlineContent resolve(InlineContent content) {
        String value = content.attributes().get("uri");
        if (value == null || value.isEmpty()) return content;
        try {
            return InlineImageService.INSTANCE.resolveStructured(content, URI.create(value),
                    "gosling_emoji".equals(content.kind()));
        } catch (IllegalArgumentException ignored) {
            return content;
        }
    }
}
