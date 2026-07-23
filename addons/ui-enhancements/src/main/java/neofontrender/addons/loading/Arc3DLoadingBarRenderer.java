package neofontrender.addons.loading;

import icyllis.arc3d.core.Color;
import icyllis.arc3d.core.MathUtil;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * Arc3D-driven progress animation rendered through Minecraft's current OpenGL context.
 *
 * <p>Arc3D supplies the interpolation and color math. Geometry stays in the host context so the
 * loading screen does not create a competing GPU device while Cleanroom is still starting.</p>
 */
final class Arc3DLoadingBarRenderer {
    private float displayed = 0.02F;
    private long lastNanos;

    void reset(long now) {
        displayed = 0.02F;
        lastNanos = now;
    }

    float update(float target, long now) {
        target = clamp(target);
        if (lastNanos == 0L) {
            reset(now);
        }
        float seconds = Math.max(0.0F, Math.min(0.1F,
                (now - lastNanos) / 1_000_000_000.0F));
        float response = 1.0F - (float) Math.exp(-10.0F * seconds);
        displayed = Math.max(displayed, MathUtil.lerp(displayed, target, response));
        if (target >= 1.0F && 1.0F - displayed < 0.002F) displayed = 1.0F;
        lastNanos = now;
        return clamp(displayed);
    }

    void draw(int width, int height, float amount, int accent, float alpha, long now) {
        if (width <= 0 || height <= 0) return;
        int lineHeight = Math.max(2, height / 360);
        float fillRight = width * clamp(amount);
        // Keep the empty track neutral. A translucent accent across the full width reads as a
        // completed bar, especially over a dark screenshot.
        int track = scaleAlpha(0x70090D12, alpha);
        int fillStart = scaleAlpha(darken(accent, 0.72F), alpha);
        int fillEnd = scaleAlpha(accent, alpha);

        GlStateManager.disableTexture2D();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        quad(0.0F, height - lineHeight, width, height, track, track);
        if (fillRight > 0.0F) {
            quad(0.0F, height - lineHeight, fillRight, height, fillStart, fillEnd);

            float cycle = (now % 1_350_000_000L) / 1_350_000_000.0F;
            float eased = cycle * cycle * (3.0F - 2.0F * cycle);
            float shimmerWidth = Math.max(18.0F, Math.min(72.0F, width * 0.055F));
            float center = MathUtil.lerp(-shimmerWidth, fillRight + shimmerWidth, eased);
            float left = Math.max(0.0F, center - shimmerWidth);
            float right = Math.min(fillRight, center + shimmerWidth);
            if (right > left) {
                int clear = scaleAlpha(accent & 0x00FFFFFF, alpha);
                int glow = scaleAlpha(accent & 0x00FFFFFF | 0xB8000000, alpha);
                float middle = Math.max(left, Math.min(right, center));
                quad(left, height - lineHeight, middle, height, clear, glow);
                quad(middle, height - lineHeight, right, height, glow, clear);
            }
        }
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
    }

    private static void quad(float left, float top, float right, float bottom,
                             int leftColor, int rightColor) {
        if (right <= left || bottom <= top) return;
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vertex(buffer, left, top, leftColor);
        vertex(buffer, left, bottom, leftColor);
        vertex(buffer, right, bottom, rightColor);
        vertex(buffer, right, top, rightColor);
        Tessellator.getInstance().draw();
    }

    private static void vertex(BufferBuilder buffer, float x, float y, int color) {
        buffer.pos(x, y, 0.0D)
                .color(Color.red(color), Color.green(color), Color.blue(color), Color.alpha(color))
                .endVertex();
    }

    private static int darken(int color, float factor) {
        return color & 0xFF000000
                | Math.round(Color.red(color) * factor) << 16
                | Math.round(Color.green(color) * factor) << 8
                | Math.round(Color.blue(color) * factor);
    }

    private static int scaleAlpha(int color, float scale) {
        int alpha = Math.round(Color.alpha(color) * clamp(scale));
        return color & 0x00FFFFFF | alpha << 24;
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
