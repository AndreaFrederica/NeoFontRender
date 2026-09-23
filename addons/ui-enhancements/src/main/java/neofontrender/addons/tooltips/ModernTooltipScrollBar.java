package neofontrender.addons.tooltips;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.awt.Rectangle;

/** Draws the modern UIE scrollbar skin over an optional foreign scrollbar model. */
public final class ModernTooltipScrollBar {
    private static final int BORDER_SIZE = 1;
    private static final int MIN_MARKER_HEIGHT = 14;

    private ModernTooltipScrollBar() {}

    public static boolean draw(Rectangle area, int visibleAmount, int hiddenAmount,
                               float scrollOffset, ItemStack stack) {
        if (area == null || area.width <= 0 || area.height <= 0 || hiddenAmount <= 0) return false;

        Geometry geometry = Geometry.calculate(area, visibleAmount, hiddenAmount, scrollOffset);
        int accent = accentColor(stack);
        int track = withAlpha(0xFF000000, 95);
        int thumb = withAlpha(accent, 225);
        int thumbEdge = withAlpha(accent, 150);

        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        try {
            drawRounded(geometry.trackLeft, geometry.trackTop, geometry.trackRight,
                    geometry.trackBottom, 2.0F, track);
            drawRounded(geometry.thumbLeft, geometry.thumbTop, geometry.thumbRight,
                    geometry.thumbBottom, 2.5F, thumb);
            // A subtle edge keeps the thumb visible on very dark tooltip fills without bringing
            // back the opaque, texture-heavy appearance of HEI's nine-slice marker.
            ModernTooltipRenderer.drawRoundedBorder(geometry.thumbLeft, geometry.thumbTop,
                    geometry.thumbRight, geometry.thumbBottom, 2.5F, 0.5F,
                    new int[] {thumbEdge, thumbEdge, thumbEdge, thumbEdge});
        } finally {
            GlStateManager.shadeModel(GL11.GL_FLAT);
            restoreLighting(lighting);
            restoreDepth(depth);
            restoreTexture(texture);
            restoreAlpha(alpha);
            restoreBlend(blend);
            restoreCull(cull);
        }
        return true;
    }

    private static void drawRounded(float left, float top, float right, float bottom,
                                    float radius, int color) {
        int[] colors = {color, color, color, color};
        ModernTooltipRenderer.drawRoundedFill(left, top, right, bottom, radius, colors);
    }

    private static int accentColor(ItemStack stack) {
        if (TooltipConfig.adaptiveBorder && stack != null && !stack.isEmpty()) {
            AdaptiveBorderColors.Result adaptive = AdaptiveBorderColors.compute(
                    stack, stack.getDisplayName(), TooltipConfig.borderColors);
            return adaptive.colors[0];
        }
        return TooltipConfig.borderColors[0];
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    private static void restoreLighting(boolean enabled) {
        if (enabled) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
    }

    private static void restoreDepth(boolean enabled) {
        if (enabled) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
    }

    private static void restoreTexture(boolean enabled) {
        if (enabled) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
    }

    private static void restoreAlpha(boolean enabled) {
        if (enabled) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
    }

    private static void restoreBlend(boolean enabled) {
        if (enabled) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
    }

    private static void restoreCull(boolean enabled) {
        if (enabled) GlStateManager.enableCull(); else GlStateManager.disableCull();
    }

    static final class Geometry {
        final int trackLeft, trackTop, trackRight, trackBottom;
        final int thumbLeft, thumbTop, thumbRight, thumbBottom;

        private Geometry(int trackLeft, int trackTop, int trackRight, int trackBottom,
                         int thumbLeft, int thumbTop, int thumbRight, int thumbBottom) {
            this.trackLeft = trackLeft;
            this.trackTop = trackTop;
            this.trackRight = trackRight;
            this.trackBottom = trackBottom;
            this.thumbLeft = thumbLeft;
            this.thumbTop = thumbTop;
            this.thumbRight = thumbRight;
            this.thumbBottom = thumbBottom;
        }

        static Geometry calculate(Rectangle area, int visibleAmount, int hiddenAmount,
                                  float scrollOffset) {
            int trackTop = area.y + BORDER_SIZE;
            int trackBottom = Math.max(trackTop + 1, area.y + area.height - BORDER_SIZE);
            int trackWidth = Math.max(2, Math.min(5, area.width - 6));
            int trackLeft = area.x + (area.width - trackWidth) / 2;
            int trackRight = trackLeft + trackWidth;
            int trackHeight = Math.max(1, trackBottom - trackTop);
            int total = Math.max(0, visibleAmount) + Math.max(0, hiddenAmount);
            int thumbHeight = total <= 0 ? trackHeight
                    : Math.round(trackHeight * (visibleAmount / (float) total));
            thumbHeight = Math.max(Math.min(MIN_MARKER_HEIGHT, trackHeight), thumbHeight);
            thumbHeight = Math.min(trackHeight, Math.max(1, thumbHeight));
            int travel = Math.max(0, trackHeight - thumbHeight);
            float offset = Math.max(0.0F, Math.min(1.0F, scrollOffset));
            int thumbTop = trackTop + Math.round(travel * offset);
            return new Geometry(trackLeft, trackTop, trackRight, trackBottom,
                    trackLeft - 1, thumbTop, trackRight + 1, thumbTop + thumbHeight);
        }
    }
}
