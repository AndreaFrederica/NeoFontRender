package neofontrender.core.font.route;

import net.minecraft.client.gui.FontRenderer;
import neofontrender.api.text.TextVisualBounds;
import neofontrender.api.text.route.TextInlineBounds;
import neofontrender.api.text.route.TextRenderRouteLayout;
import neofontrender.core.font.support.ScopedFontRenderBypass;
import neofontrender.text.StructuredText;

import java.util.Collections;
import java.util.List;

/**
 * Route used by the Compatibility engine for consumers that ask for a layout directly.
 *
 * <p>Vanilla owns layout, so this adapter only forwards draw/measure operations to the vanilla
 * {@link FontRenderer}; the leaf glyph hooks inside that renderer are what draw the compat font.
 * Without this adapter, structured callers such as the embedded Tabby chat would receive the
 * non-handled passthrough layout and draw nothing.</p>
 */
final class CompatRouteLayout implements TextRenderRouteLayout {

    static final String ID = "neofontrender:compat";

    private final FontRenderer font;
    private final String source;
    private final StructuredText structured;
    private final int argb;
    private final boolean shadow;
    private final float advance;
    private final float height;

    CompatRouteLayout(FontRenderer font, String source, StructuredText structured,
                      int argb, boolean shadow) {
        this.font = font;
        this.source = source;
        this.structured = structured;
        this.argb = argb;
        this.shadow = shadow;
        this.advance = ScopedFontRenderBypass.call(() -> (float) font.getStringWidth(source));
        this.height = Math.max(1, font.FONT_HEIGHT);
    }

    @Override public String routeId() { return ID; }
    @Override public boolean handled() { return true; }
    @Override public String source() { return source; }
    @Override public StructuredText structuredText() { return structured; }
    @Override public float advance() { return advance; }
    @Override public float height() { return height; }

    @Override
    public TextVisualBounds visualBounds() {
        return new TextVisualBounds(0, 0, advance, height);
    }

    @Override
    public void draw(float x, float y) {
        ScopedFontRenderBypass.run(() -> {
            if (shadow) {
                font.drawStringWithShadow(source, x, y, argb);
            } else {
                font.drawString(source, x, y, argb, false);
            }
        });
    }

    @Override
    public int sourceIndexAt(float localX) {
        if (localX <= 0.0F) return 0;
        if (localX >= advance) return source.length();
        return ScopedFontRenderBypass.call(
                () -> font.trimStringToWidth(source, Math.max(0, (int) Math.floor(localX))).length());
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
        String remaining = ScopedFontRenderBypass.call(() -> font.trimStringToWidth(
                source, Math.max(0, (int) Math.floor(maximumWidth)), true));
        return Math.max(0, source.length() - remaining.length());
    }

    @Override
    public int sizeToWidth(float maximumWidth, boolean cjkLineBreak) {
        return sourceIndexFitting(maximumWidth);
    }

    @Override
    public List<TextInlineBounds> inlineBounds() {
        return Collections.emptyList();
    }
}
