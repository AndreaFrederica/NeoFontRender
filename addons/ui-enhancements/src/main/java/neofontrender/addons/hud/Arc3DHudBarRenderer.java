package neofontrender.addons.hud;

import icyllis.arc3d.core.Color;
import icyllis.arc3d.core.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import neofontrender.addons.hud.api.HudBarSide;
import neofontrender.addons.hud.api.HudBarValue;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Arc3D color math with a host-LWJGL geometry batch; no second graphics context is created. */
final class Arc3DHudBarRenderer {
    private static final double Z = 0.0D;
    private final Map<String, AnimatedValue> animation = new HashMap<>();

    void draw(String id, HudBarValue sample, HudBarSide side, int x, int y) {
        if (sample == null || sample.maximum <= 0.0F) return;
        float current = animated(id, sample.current);
        float ratio = clamp(current / sample.maximum);
        float secondary = clamp(sample.secondary / sample.maximum);
        float preview = clamp(sample.preview / sample.maximum);
        float depletion = clamp(sample.depletion / sample.maximum);
        float left = x;
        float top = y;
        float right = x + HudBarsConfig.width;
        float bottom = y + HudBarsConfig.height;
        float radius = HudBarsConfig.rounded ? Math.min(3.5F, HudBarsConfig.height * 0.5F) : 0.01F;
        float inset = 1.0F;

        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        try {
            rounded(left, top, right, bottom, radius, HudBarsConfig.border);
            rounded(left + inset, top + inset, right - inset, bottom - inset,
                    Math.max(0.01F, radius - inset), HudBarsConfig.background);
            float innerLeft = left + inset;
            float innerRight = right - inset;
            float innerTop = top + inset;
            float innerBottom = bottom - inset;
            float span = Math.max(0.0F, innerRight - innerLeft);
            if (ratio > 0.0F) {
                if (side == HudBarSide.RIGHT) {
                    rounded(innerRight - span * ratio, innerTop, innerRight, innerBottom,
                            Math.min(radius - inset, span * ratio * 0.5F), sample.primaryColor);
                } else {
                    rounded(innerLeft, innerTop, innerLeft + span * ratio, innerBottom,
                            Math.min(radius - inset, span * ratio * 0.5F), sample.primaryColor);
                }
            }
            if (secondary > 0.0F) {
                float stripTop = MathUtil.lerp(innerTop, innerBottom, 0.62F);
                if (side == HudBarSide.RIGHT) quad(innerRight - span * secondary, stripTop,
                        innerRight, innerBottom, sample.secondaryColor);
                else quad(innerLeft, stripTop, innerLeft + span * secondary, innerBottom, sample.secondaryColor);
            }
            if (preview > 0.0F && ratio < 1.0F) {
                float end = Math.min(1.0F, ratio + preview);
                if (side == HudBarSide.RIGHT) quad(innerRight - span * end, innerTop,
                        innerRight - span * ratio, innerBottom, sample.previewColor);
                else quad(innerLeft + span * ratio, innerTop,
                        innerLeft + span * end, innerBottom, sample.previewColor);
            }
            if (depletion > 0.0F) {
                float stripeBottom = Math.min(innerBottom, innerTop + 1.25F);
                if (side == HudBarSide.RIGHT) quad(innerLeft, innerTop,
                        innerLeft + span * depletion, stripeBottom, sample.depletionColor);
                else quad(innerRight - span * depletion, innerTop,
                        innerRight, stripeBottom, sample.depletionColor);
            }
        } finally {
            GlStateManager.shadeModel(GL11.GL_FLAT);
            if (cull) GlStateManager.enableCull();
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.enableDepth();
            GlStateManager.enableLighting();
        }
        drawText(sample.text, x, y);
    }

    private float animated(String id, float target) {
        AnimatedValue value = animation.get(id);
        long now = System.nanoTime();
        if (value == null || !HudBarsConfig.smoothValues) {
            animation.put(id, new AnimatedValue(target, now));
            return target;
        }
        float seconds = Math.min((now - value.nanos) / 1_000_000_000.0F, 0.1F);
        float factor = 1.0F - (float) Math.exp(-12.0F * seconds);
        value.value = MathUtil.lerp(value.value, target, factor);
        if (Math.abs(value.value - target) < 0.01F) value.value = target;
        value.nanos = now;
        return value.value;
    }

    private static void drawText(String text, int x, int y) {
        if (text == null || text.isEmpty()) return;
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int textX = x + (HudBarsConfig.width - font.getStringWidth(text)) / 2;
        int textY = y + (HudBarsConfig.height - font.FONT_HEIGHT) / 2;
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        font.drawString(text, textX, textY, 0xFFFFFFFF, true);
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
    }

    private static void rounded(float left, float top, float right, float bottom, float requestedRadius, int color) {
        if (right <= left || bottom <= top) return;
        float radius = Math.max(0.01F, Math.min(requestedRadius,
                Math.min((right - left) * 0.5F, (bottom - top) * 0.5F)));
        List<Point> points = new ArrayList<>(24);
        corner(points, right - radius, top + radius, radius, -90, 0);
        corner(points, right - radius, bottom - radius, radius, 0, 90);
        corner(points, left + radius, bottom - radius, radius, 90, 180);
        corner(points, left + radius, top + radius, radius, 180, 270);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        vertex(buffer, (left + right) * 0.5F, (top + bottom) * 0.5F, color);
        for (Point point : points) vertex(buffer, point.x, point.y, color);
        vertex(buffer, points.get(0).x, points.get(0).y, color);
        tessellator.draw();
    }

    private static void quad(float left, float top, float right, float bottom, int color) {
        if (right <= left || bottom <= top) return;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vertex(buffer, left, top, color); vertex(buffer, left, bottom, color);
        vertex(buffer, right, bottom, color); vertex(buffer, right, top, color);
        tessellator.draw();
    }

    private static void corner(List<Point> out, float x, float y, float radius, int from, int to) {
        int segments = 5;
        for (int i = 0; i <= segments; i++) {
            double angle = Math.toRadians(MathUtil.lerp(from, to, i / (float) segments));
            out.add(new Point(x + (float) Math.cos(angle) * radius, y + (float) Math.sin(angle) * radius));
        }
    }

    private static void vertex(BufferBuilder buffer, float x, float y, int color) {
        buffer.pos(x, y, Z).color(Color.red(color), Color.green(color), Color.blue(color), Color.alpha(color)).endVertex();
    }

    private static float clamp(float value) { return Math.max(0.0F, Math.min(1.0F, value)); }

    private static final class AnimatedValue {
        private float value;
        private long nanos;
        private AnimatedValue(float value, long nanos) { this.value = value; this.nanos = nanos; }
    }

    private static final class Point {
        private final float x, y;
        private Point(float x, float y) { this.x = x; this.y = y; }
    }
}
