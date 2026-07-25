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
        long completedMorphCycles = Math.floorDiv(now, MORPH_DURATION_NANOS);
        float cycle = Math.floorMod(now, MORPH_DURATION_NANOS)
                / (float) MORPH_DURATION_NANOS;
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
        float startDegrees = continuousStartDegrees(now, completedMorphCycles, tailAdvance);
        int color = scaleAlpha(accent, alpha);

        GlStateManager.disableTexture2D();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        drawArc(centerX, centerY, startDegrees, sweep, color);
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
    }

    static float continuousStartDegrees(long now, long completedMorphCycles, float tailAdvance) {
        float baseRotation = Math.floorMod(now, 1_600_000_000L)
                / 1_600_000_000.0F * 360.0F;
        // The shrinking half advances the tail by MAX_SWEEP - MIN_SWEEP. Carry that advance into
        // every following morph cycle; otherwise tailAdvance snaps back to zero at the loop
        // boundary and the arc visibly jumps backwards by 244 degrees.
        float carriedTailRotation = (float) ((completedMorphCycles
                * (double) (MAX_SWEEP - MIN_SWEEP)) % 360.0D);
        return baseRotation + carriedTailRotation + tailAdvance - 90.0F;
    }

    private static void drawArc(float centerX, float centerY, float startDegrees,
                                float sweepDegrees, int color) {
        int segments = Math.max(16, (int) Math.ceil(sweepDegrees / 3.0F));
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        float previous = radians(startDegrees);
        for (int i = 1; i <= segments; i++) {
            float next = radians(startDegrees + sweepDegrees * i / segments);
            arcQuad(buffer, centerX, centerY, previous, next, color);
            previous = next;
        }

        float middleRadius = (INNER_RADIUS + OUTER_RADIUS) * 0.5F;
        float capRadius = (OUTER_RADIUS - INNER_RADIUS) * 0.5F;
        float start = radians(startDegrees);
        float end = radians(startDegrees + sweepDegrees);
        roundCap(buffer, centerX, centerY, start, middleRadius, capRadius, false, color);
        roundCap(buffer, centerX, centerY, end, middleRadius, capRadius, true, color);
        Tessellator.getInstance().draw();
    }

    private static void arcQuad(BufferBuilder buffer, float cx, float cy,
                                float first, float second, int color) {
        float foX = cx + (float) Math.cos(first) * OUTER_RADIUS;
        float foY = cy + (float) Math.sin(first) * OUTER_RADIUS;
        float fiX = cx + (float) Math.cos(first) * INNER_RADIUS;
        float fiY = cy + (float) Math.sin(first) * INNER_RADIUS;
        float soX = cx + (float) Math.cos(second) * OUTER_RADIUS;
        float soY = cy + (float) Math.sin(second) * OUTER_RADIUS;
        float siX = cx + (float) Math.cos(second) * INNER_RADIUS;
        float siY = cy + (float) Math.sin(second) * INNER_RADIUS;
        vertex(buffer, foX, foY, color); vertex(buffer, fiX, fiY, color);
        vertex(buffer, soX, soY, color);
        vertex(buffer, soX, soY, color); vertex(buffer, fiX, fiY, color);
        vertex(buffer, siX, siY, color);
    }

    private static void roundCap(BufferBuilder buffer, float cx, float cy, float angle,
                                 float middleRadius, float radius, boolean end, int color) {
        float px = cx + (float) Math.cos(angle) * middleRadius;
        float py = cy + (float) Math.sin(angle) * middleRadius;
        float nx = (float) Math.cos(angle);
        float ny = (float) Math.sin(angle);
        float tx = -(float) Math.sin(angle);
        float ty = (float) Math.cos(angle);
        int capSegments = 12;
        for (int i = 0; i < capSegments; i++) {
            float a = (float) Math.PI * i / capSegments;
            float b = (float) Math.PI * (i + 1) / capSegments;
            vertex(buffer, px, py, color);
            capVertex(buffer, px, py, nx, ny, tx, ty, radius, a, end, color);
            capVertex(buffer, px, py, nx, ny, tx, ty, radius, b, end, color);
        }
    }

    private static void capVertex(BufferBuilder buffer, float px, float py,
                                  float nx, float ny, float tx, float ty, float radius,
                                  float phase, boolean end, int color) {
        float normal = (float) Math.cos(phase) * (end ? -1.0F : 1.0F);
        float tangent = (float) Math.sin(phase) * (end ? 1.0F : -1.0F);
        vertex(buffer, px + radius * (normal * nx + tangent * tx),
                py + radius * (normal * ny + tangent * ty), color);
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
