package neofontrender.core.font.support;

import net.minecraft.client.gui.FontRenderer;
import neofontrender.core.font.FontManager;
import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.font.backend.TextRenderResult;

/** Makes Forge tooltip layout account for shaped glyph overhang and shadow pixels. */
public final class TooltipBoundsCompat {
    private static final ThreadLocal<Integer> RICH_TOOLTIP_DEPTH = new ThreadLocal<>();

    private TooltipBoundsCompat() {
    }

    public static int measuredWidth(FontRenderer font, String text) {
        int advanceWidth = font.getStringWidth(text);
        FontManager manager = FontManager.INSTANCE;
        if (!manager.isTextBackendActive()) {
            // SFR glyph quads can still extend slightly past their logical advance. Forge draws
            // tooltip text with a one-pixel shadow, so retain a small safety pixel on each side.
            return manager.isSfrActive() ? advanceWidth + 2 : advanceWidth;
        }

        TextRenderBackend backend = manager.getTextRenderBackend();
        if (backend == null || text == null || text.isEmpty()) {
            return advanceWidth;
        }
        try {
            TextRenderResult rendered = backend.renderFormatted(text, 0xFFFFFFFF, false);
            float left = Math.min(0.0F, rendered.visualLeft());
            // drawStringWithShadow may add one pixel beyond the foreground raster.
            float right = Math.max(rendered.advance(), rendered.visualRight()) + 1.0F;
            int visualWidth = (int) Math.ceil(right - (float) Math.floor(left));
            return Math.max(advanceWidth, visualWidth);
        } catch (RuntimeException | LinkageError ignored) {
            // Tooltip rendering must remain available if an optional native backend fails.
            return advanceWidth + 2;
        }
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
