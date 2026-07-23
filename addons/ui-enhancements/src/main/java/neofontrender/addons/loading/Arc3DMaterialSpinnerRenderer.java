package neofontrender.addons.loading;

import icyllis.arc3d.core.Color;
import icyllis.arc3d.core.MathUtil;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * A generated Material-style indeterminate indicator: one continuously rotating arc whose head
 * and tail ease independently. No texture or implementation from another project is used.
 */
final class Arc3DMaterialSpinnerRenderer {
    private static final float INNER_RADIUS = 6.25F;
    private static final float OUTER_RADIUS = 8.75F;
    private static final float MIN_SWEEP = 42.0F;
    private static final float MAX_SWEEP = 286.0F;
    private static final long MORPH_DURATION_NANOS = 1_333_000_000L;

    void draw(float centerX, float centerY, int accent, float alpha, long now) {
        float cycle = (now % MORPH_DURATION_NANOS) / (float) MORPH_DURATION_NANOS;
        float halfCycle = cycle < 0.5F ? cycle * 2.0F : (cycle - 0.5F) * 2.0F;
        float eased = smoothStep(halfCycle);
        float sweep;
        float tailAdvance;
        if (cycle < 0.5F) {
            sweep = MathUtil.lerp(MIN_SWEEP, MAX_SWEEP, eased);
            tailAdvance = 0.0F;
        } else {
            sweep = MathUtil.lerp(MAX_SWEEP, MIN_SWEEP, eased);
            tailAdvance = (MAX_SWEEP - MIN_SWEEP) * eased;
        }

        // A full base turn is deliberately out of phase with the head/tail morph. This prevents
        // the indicator from appearing to pause at either end of its expansion.
        float baseRotation = (now % 1_600_000_000L) / 1_600_000_000.0F * 360.0F;
        float startDegrees = baseRotation + tailAdvance - 90.0F;
        int color = scaleAlpha(accent, alpha);

        GlStateManager.disableTexture2D();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        drawArc(centerX, centerY, startDegrees, sweep, color);
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
    }

    private static void drawArc(float centerX, float centerY, float startDegrees,
                                float sweepDegrees, int color) {
        int segments = Math.max(12, (int) Math.ceil(sweepDegrees / 5.0F));
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            float angle = radians(startDegrees + sweepDegrees * i / segments);
            vertex(buffer, centerX + (float) Math.cos(angle) * OUTER_RADIUS,
                    centerY + (float) Math.sin(angle) * OUTER_RADIUS, color);
            vertex(buffer, centerX + (float) Math.cos(angle) * INNER_RADIUS,
                    centerY + (float) Math.sin(angle) * INNER_RADIUS, color);
        }
        Tessellator.getInstance().draw();

        float middleRadius = (INNER_RADIUS + OUTER_RADIUS) * 0.5F;
        float capRadius = (OUTER_RADIUS - INNER_RADIUS) * 0.5F;
        float start = radians(startDegrees);
        float end = radians(startDegrees + sweepDegrees);
        drawRoundCap(centerX + (float) Math.cos(start) * middleRadius,
                centerY + (float) Math.sin(start) * middleRadius, capRadius, color);
        drawRoundCap(centerX + (float) Math.cos(end) * middleRadius,
                centerY + (float) Math.sin(end) * middleRadius, capRadius, color);
    }

    private static void drawRoundCap(float centerX, float centerY, float radius, int color) {
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        vertex(buffer, centerX, centerY, color);
        for (int i = 0; i <= 12; i++) {
            float angle = (float) (Math.PI * 2.0D * i / 12.0D);
            vertex(buffer, centerX + (float) Math.cos(angle) * radius,
                    centerY + (float) Math.sin(angle) * radius, color);
        }
        Tessellator.getInstance().draw();
    }

    private static void vertex(BufferBuilder buffer, float x, float y, int color) {
        buffer.pos(x, y, 0.0D)
                .color(Color.red(color), Color.green(color), Color.blue(color), Color.alpha(color))
                .endVertex();
    }

    private static float smoothStep(float value) {
        value = Math.max(0.0F, Math.min(1.0F, value));
        return value * value * (3.0F - 2.0F * value);
    }

    private static float radians(float degrees) {
        return degrees * (float) Math.PI / 180.0F;
    }

    private static int scaleAlpha(int color, float scale) {
        int alpha = Math.round(Color.alpha(color) * Math.max(0.0F, Math.min(1.0F, scale)));
        return color & 0x00FFFFFF | alpha << 24;
    }
}
