package neofontrender.core.font.support;

import neofontrender.api.text.TextVisualBounds;

import net.minecraft.client.gui.FontRenderer;

/** Makes Forge tooltip layout account for shaped glyph overhang and shadow pixels. */
public final class TooltipBoundsCompat {
    private static final ThreadLocal<Integer> RICH_TOOLTIP_DEPTH = new ThreadLocal<>();

    private TooltipBoundsCompat() {
    }

    public static int measuredWidth(FontRenderer font, String text) {
        // Forge callers still draw at the logical origin, so include advance and overhang.
        TextVisualBounds bounds = measuredVisualBounds(font, text, true);
        return (int) Math.ceil(Math.max(font.getStringWidth(text), bounds.right)
                - Math.min(0, bounds.left));
    }

    /** Uses the selected FontRenderer route, including its configured shadow and inline offset. */
    public static TextVisualBounds measuredVisualBounds(
            FontRenderer font, String text, boolean shadow) {
        if (text == null || text.isEmpty()) return TextVisualBounds.EMPTY;
        try {
            return neofontrender.api.text.route.TextRenderRouteApi.layout(
                    font, text, 0xFFFFFFFF, shadow).visualBounds();
        } catch (RuntimeException | LinkageError ignored) {
            float width = font == null ? 0 : font.getStringWidth(text);
            return new TextVisualBounds(0, 0, width,
                    Math.max(1, font == null ? 9 : font.FONT_HEIGHT));
        }
    }

    public static HorizontalBounds measuredHorizontalBounds(FontRenderer font, String text,
                                                             boolean shadow) {
        TextVisualBounds bounds = measuredVisualBounds(font, text, shadow);
        return new HorizontalBounds(bounds.left, bounds.right);
    }

    public static final class HorizontalBounds {
        public final float left, right;
        HorizontalBounds(float left, float right) { this.left = left; this.right = right; }
        public int width() { return Math.max(0, (int) Math.ceil(right - left)); }
    }

    public static VerticalBounds measuredVerticalBounds(FontRenderer font, String text, boolean shadow) {
        TextVisualBounds bounds = measuredVisualBounds(font, text, shadow);
        return new VerticalBounds(bounds.top, bounds.bottom);
    }

    public static final class VerticalBounds {
        public final float top, bottom;
        VerticalBounds(float top, float bottom) { this.top = top; this.bottom = bottom; }
    }
    public static void beginRichTooltip() {
        Integer depth = RICH_TOOLTIP_DEPTH.get();
        RICH_TOOLTIP_DEPTH.set(depth == null ? 1 : depth + 1);
    }

    public static void endRichTooltip() {
        Integer depth = RICH_TOOLTIP_DEPTH.get();
        if (depth == null || depth <= 1) RICH_TOOLTIP_DEPTH.remove();
        else RICH_TOOLTIP_DEPTH.set(depth - 1);
    }

    public static boolean isRichTooltipLayout() {
        Integer depth = RICH_TOOLTIP_DEPTH.get();
        return depth != null && depth > 0;
    }
}
