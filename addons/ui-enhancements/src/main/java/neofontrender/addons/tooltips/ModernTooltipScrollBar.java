package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Rectangle;
import java.nio.FloatBuffer;

/** Draws the modern UIE scrollbar skin over an optional foreign scrollbar model. */
public final class ModernTooltipScrollBar {
    private static final int BORDER_SIZE = 1;
    private static final int MIN_MARKER_HEIGHT = 14;
    private static final ThreadLocal<FloatBuffer> MODEL_VIEW =
            ThreadLocal.withInitial(() -> BufferUtils.createFloatBuffer(16));

    private ModernTooltipScrollBar() {}

    public static boolean draw(Rectangle area, int visibleAmount, int hiddenAmount,
                               float scrollOffset, float emphasis) {
        if (area == null || area.width <= 0 || area.height <= 0 || hiddenAmount <= 0) return false;

        Geometry geometry = Geometry.calculate(area, visibleAmount, hiddenAmount, scrollOffset);
        float active = Math.max(0.0F, Math.min(1.0F, emphasis));
        // Follow the theme's readable foreground, without borrowing the saturated panel border.
        int track = withAlpha(TooltipConfig.textColor, Math.round(12 + 12 * active));
        int thumb = withAlpha(TooltipConfig.textColor, Math.round(100 + 75 * active));
        float center = (geometry.thumbLeft + geometry.thumbRight) * 0.5F;
        float halfWidth = Math.min(area.width, 3.0F + active) * 0.5F;

        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        int shadeModel = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
        int srcRgb = GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_RGB);
        int dstRgb = GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_ALPHA);
        int dstAlpha = GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_ALPHA);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();
        // The faint track must survive Minecraft's usual alpha-test threshold.
        GlStateManager.disableAlpha();
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
                    geometry.trackBottom, 1.5F, track);
            drawRounded(center - halfWidth, geometry.thumbTop, center + halfWidth,
                    geometry.thumbBottom, halfWidth, thumb);
        } finally {
            GlStateManager.shadeModel(shadeModel);
            GlStateManager.tryBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
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

    /** HEI draws the bar in translated grid coordinates, including when the panel is pinned. */
    public static boolean isHovered(Rectangle area, Minecraft minecraft) {
        if (area == null || !Mouse.isCreated() || minecraft.displayWidth <= 0
                || minecraft.displayHeight <= 0) return false;
        FloatBuffer matrix = MODEL_VIEW.get();
        matrix.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrix);
        ScaledResolution resolution = new ScaledResolution(minecraft);
        double mouseX = Mouse.getX() * resolution.getScaledWidth_double() / minecraft.displayWidth;
        double mouseY = (minecraft.displayHeight - Mouse.getY() - 1)
                * resolution.getScaledHeight_double() / minecraft.displayHeight;
        // HEI's grid applies translation; allow GUI scaling and rotation as well.
        double determinant = matrix.get(0) * matrix.get(5) - matrix.get(4) * matrix.get(1);
        if (Math.abs(determinant) < 1.0e-6) return false;
        double x = mouseX - matrix.get(12);
        double y = mouseY - matrix.get(13);
        return area.contains((matrix.get(5) * x - matrix.get(4) * y) / determinant,
                (matrix.get(0) * y - matrix.get(1) * x) / determinant);
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
            int trackWidth = Math.max(1, Math.min(3, area.width));
            // Hug the outer edge of HEI's hit area instead of centering in its wide gutter.
            int outerInset = area.width > trackWidth ? 1 : 0;
            int trackRight = area.x + area.width - outerInset;
            int trackLeft = trackRight - trackWidth;
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
                    trackLeft, thumbTop, trackRight, thumbTop + thumbHeight);
        }
    }
}
