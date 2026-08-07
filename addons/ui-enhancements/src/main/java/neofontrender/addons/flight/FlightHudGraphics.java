package neofontrender.addons.flight;

import icyllis.arc3d.core.Color;
import icyllis.arc3d.core.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.nio.IntBuffer;
import java.util.Locale;

/** Shared Arc3D/GL primitives and coordinate-independent HUD drawing operations. */
final class FlightHudGraphics {
    private FlightHudGraphics() {}

    static int primary(FlightHudTheme theme) { return theme.color("primary", 0xFF91FFB2); }
    static int halo(FlightHudTheme theme) { return theme.color("halo", 0xD0001709); }

    static double compassHeading(float yaw) {
        double heading = yaw + 180.0D;
        heading %= 360.0D;
        return heading < 0.0D ? heading + 360.0D : heading;
    }

    static boolean nearMultiple(double value, double step) {
        return Math.abs(value / step - Math.rint(value / step)) < 0.02D;
    }

    static double niceStep(double value) {
        if (!Double.isFinite(value) || value <= 0.0D) return 1.0D;
        double magnitude = Math.pow(10.0D, Math.floor(Math.log10(value)));
        double normalized = value / magnitude;
        double nice = normalized < 1.5D ? 1.0D
                : normalized < 3.5D ? 2.0D : normalized < 7.5D ? 5.0D : 10.0D;
        return nice * magnitude;
    }

    static double wrapDegrees(double value) {
        value %= 360.0D;
        if (value >= 180.0D) value -= 360.0D;
        if (value < -180.0D) value += 360.0D;
        return value;
    }

    static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    static float clampAxis(float value) { return Math.max(-1.0F, Math.min(1.0F, value)); }

    static void rotatedLine(float cx, float cy, float ax, float ay, float bx, float by,
                            float cos, float sin, int color, float width) {
        line(cx + ax * cos - ay * sin, cy + ax * sin + ay * cos,
                cx + bx * cos - by * sin, cy + bx * sin + by * cos, color, width);
    }

    static void dashedRotatedLine(float cx, float cy, float ax, float ay,
                                  float bx, float by, float cos, float sin,
                                  int color, float width) {
        for (int i = 0; i < 5; i += 2) {
            float t1 = i / 5.0F;
            float t2 = Math.min(1.0F, (i + 1) / 5.0F);
            rotatedLine(cx, cy, MathUtil.lerp(ax, bx, t1), MathUtil.lerp(ay, by, t1),
                    MathUtil.lerp(ax, bx, t2), MathUtil.lerp(ay, by, t2),
                    cos, sin, color, width);
        }
    }

    static void rotatedText(String value, float cx, float cy, float x, float y,
                            float cos, float sin, float scale, int color, int shadow) {
        text(value, cx + x * cos - y * sin, cy + x * sin + y * cos,
                scale, color, shadow);
    }

    static void outline(float left, float top, float right, float bottom,
                        int color, float width) {
        line(left, top, right, top, color, width);
        line(right, top, right, bottom, color, width);
        line(right, bottom, left, bottom, color, width);
        line(left, bottom, left, top, color, width);
    }

    static void triangle(float x, float y, float size, boolean pointsDown,
                         int color, float width) {
        float direction = pointsDown ? 1.0F : -1.0F;
        line(x, y, x - size, y + direction * size * 1.7F, color, width);
        line(x - size, y + direction * size * 1.7F,
                x + size, y + direction * size * 1.7F, color, width);
        line(x + size, y + direction * size * 1.7F, x, y, color, width);
    }

    static void orientedTriangle(float tipX, float tipY, float directionX,
                                 float directionY, float size,
                                 int color, float width) {
        float length = (float) Math.sqrt(directionX * directionX + directionY * directionY);
        if (length < 1.0E-5F) return;
        float dx = directionX / length;
        float dy = directionY / length;
        float baseX = tipX - dx * size * 1.7F;
        float baseY = tipY - dy * size * 1.7F;
        float px = -dy * size;
        float py = dx * size;
        line(tipX, tipY, baseX + px, baseY + py, color, width);
        line(baseX + px, baseY + py, baseX - px, baseY - py, color, width);
        line(baseX - px, baseY - py, tipX, tipY, color, width);
    }

    static void diamond(float x, float y, float size, int color, float width) {
        line(x, y - size, x + size, y, color, width);
        line(x + size, y, x, y + size, color, width);
        line(x, y + size, x - size, y, color, width);
        line(x - size, y, x, y - size, color, width);
    }

    static void circle(float cx, float cy, float radius, int color,
                       float width, int segments) {
        circleArc(cx, cy, radius, 0.0D, 360.0D, color, width, segments);
    }

    static void disc(float cx, float cy, float radius, int color, int segments) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_TRIANGLE_FAN);
        vertex(tessellator, cx, cy, color);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2.0D * i / segments;
            vertex(tessellator, cx + (float) Math.cos(angle) * radius,
                    cy + (float) Math.sin(angle) * radius, color);
        }
        tessellator.draw();
    }

    static void quad(float left, float top, float right, float bottom, int color) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_QUADS);
        vertex(tessellator, left, top, color); vertex(tessellator, left, bottom, color);
        vertex(tessellator, right, bottom, color); vertex(tessellator, right, top, color);
        tessellator.draw();
    }

    static void circleArc(float cx, float cy, float radius, double startDegrees,
                          double endDegrees, int color, float width, int segments) {
        GL11.glLineWidth(Math.max(1.0F, width));
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_LINE_STRIP);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.toRadians(startDegrees
                    + (endDegrees - startDegrees) * i / segments);
            vertex(tessellator, cx + (float) Math.cos(angle) * radius,
                    cy + (float) Math.sin(angle) * radius, color);
        }
        tessellator.draw();
    }

    static void line(float x1, float y1, float x2, float y2, int color, float width) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth(Math.max(1.0F, width));
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_LINES);
        vertex(tessellator, x1, y1, color); vertex(tessellator, x2, y2, color);
        tessellator.draw();
    }

    static void centeredText(String value, float centerX, float y, float scale,
                             int color, int shadow) {
        text(value, centerX - textWidth(value, scale) * 0.5F, y, scale, color, shadow);
    }

    static void text(String value, float x, float y, float scale,
                     int color, int shadowColor) {
        if (value == null || value.isEmpty() || scale <= 0.0F) return;
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(x, y, 0.0F);
            GL11.glScalef(scale, scale, 1.0F);
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            font.drawString(value, 1, 1, shadowColor, false);
            font.drawString(value, 0, 0, color, false);
        } finally {
            GL11.glPopMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        }
    }

    static float textWidth(String value, float scale) {
        return Minecraft.getMinecraft().fontRenderer.getStringWidth(value) * scale;
    }

    static String format(double value, int decimals) {
        return String.format(Locale.ROOT,
                decimals <= 0 ? "%.0f" : decimals == 1 ? "%.1f" : "%.2f", value);
    }

    static void withGuiScissor(float left, float top, float right, float bottom, Runnable draw) {
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(minecraft,
                minecraft.displayWidth, minecraft.displayHeight);
        int factor = resolution.getScaleFactor();
        int x = Math.min(minecraft.displayWidth, Math.max(0, (int) Math.floor(left * factor)));
        int y = Math.min(minecraft.displayHeight,
                Math.max(0, (int) Math.floor(minecraft.displayHeight - bottom * factor)));
        int width = Math.min(minecraft.displayWidth - x,
                Math.max(0, (int) Math.ceil((right - left) * factor)));
        int height = Math.min(minecraft.displayHeight - y,
                Math.max(0, (int) Math.ceil((bottom - top) * factor)));
        boolean wasEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        IntBuffer previous = BufferUtils.createIntBuffer(4);
        if (wasEnabled) {
            GL11.glGetInteger(GL11.GL_SCISSOR_BOX, previous);
            int clippedX = Math.max(x, previous.get(0));
            int clippedY = Math.max(y, previous.get(1));
            int clippedRight = Math.min(x + width, previous.get(0) + previous.get(2));
            int clippedTop = Math.min(y + height, previous.get(1) + previous.get(3));
            x = clippedX; y = clippedY;
            width = Math.max(0, clippedRight - clippedX);
            height = Math.max(0, clippedTop - clippedY);
        }
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x, y, width, height);
        try {
            draw.run();
        } finally {
            if (wasEnabled) {
                GL11.glScissor(previous.get(0), previous.get(1),
                        previous.get(2), previous.get(3));
            } else {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
        }
    }

    private static void vertex(Tessellator tessellator, float x, float y, int color) {
        tessellator.setColorRGBA(Color.red(color), Color.green(color),
                Color.blue(color), Color.alpha(color));
        tessellator.addVertex(x, y, 0.0D);
    }

    static final class State {
        private boolean texture, depth, lighting, blend, cull;
        private float lineWidth;
        private int sourceRgb, destinationRgb, sourceAlpha, destinationAlpha;

        void begin() {
            texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
            depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
            blend = GL11.glIsEnabled(GL11.GL_BLEND);
            cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            lineWidth = GL11.glGetFloat(GL11.GL_LINE_WIDTH);
            sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
            destinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
            sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
            destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA,
                    GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ZERO);
        }

        void restore() {
            GL11.glLineWidth(lineWidth);
            GL14.glBlendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
            if (cull) GL11.glEnable(GL11.GL_CULL_FACE); else GL11.glDisable(GL11.GL_CULL_FACE);
            if (texture) GL11.glEnable(GL11.GL_TEXTURE_2D); else GL11.glDisable(GL11.GL_TEXTURE_2D);
            if (depth) GL11.glEnable(GL11.GL_DEPTH_TEST); else GL11.glDisable(GL11.GL_DEPTH_TEST);
            if (lighting) GL11.glEnable(GL11.GL_LIGHTING); else GL11.glDisable(GL11.GL_LIGHTING);
            if (blend) GL11.glEnable(GL11.GL_BLEND); else GL11.glDisable(GL11.GL_BLEND);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
