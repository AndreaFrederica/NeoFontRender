package neofontrender.core.font.route;

import net.minecraft.client.gui.FontRenderer;
import neofontrender.api.text.route.TextInlineBounds;
import neofontrender.api.text.route.TextRenderRouteLayout;
import neofontrender.core.font.support.ScopedFontRenderBypass;
import neofontrender.text.StructuredText;

import java.util.Collections;
import java.util.List;

/** Non-cancelling layout used to make vanilla passthrough an observable route. */
final class VanillaPassthroughLayout implements TextRenderRouteLayout {
    private final FontRenderer font;
    private final String source;
    private final StructuredText structured;
    private final float advance;

    VanillaPassthroughLayout(FontRenderer font, String source, StructuredText structured,
                             int argb, boolean shadow, String reason) {
        this.font = font;
        this.source = source;
        this.structured = structured;
        this.advance = ScopedFontRenderBypass.isActive() ? 0.0F
                : ScopedFontRenderBypass.call(() -> (float) font.getStringWidth(source));
    }

    @Override public String routeId() { return VanillaPassthroughTextRoute.ID; }
    @Override public boolean handled() { return false; }
    @Override public String source() { return source; }
    @Override public StructuredText structuredText() { return structured; }
    @Override public float advance() { return advance; }
    @Override public float height() { return Math.max(1, font.FONT_HEIGHT); }
    @Override public void draw(float x, float y) {}

    @Override
    public int sourceIndexAt(float localX) {
        if (localX <= 0) return 0;
        if (localX >= advance) return source.length();
        return sourceIndexFitting(localX);
    }

    @Override
    public float widthToSource(int sourceIndex) {
        int boundary = Math.max(0, Math.min(source.length(), sourceIndex));
        return ScopedFontRenderBypass.call(
                () -> (float) font.getStringWidth(source.substring(0, boundary)));
    }

    @Override
    public int sourceIndexFitting(float maximumWidth) {
        return ScopedFontRenderBypass.call(() -> font.trimStringToWidth(
                source, Math.max(0, (int) Math.floor(maximumWidth))).length());
    }

    @Override
    public int sourceStartFittingReverse(float maximumWidth) {
        String result = ScopedFontRenderBypass.call(() -> font.trimStringToWidth(
                source, Math.max(0, (int) Math.floor(maximumWidth)), true));
        return Math.max(0, source.length() - result.length());
    }

    @Override
    public int sizeToWidth(float maximumWidth, boolean cjkLineBreak) {
        return sourceIndexFitting(maximumWidth);
    }

    @Override public List<TextInlineBounds> inlineBounds() { return Collections.emptyList(); }
}
