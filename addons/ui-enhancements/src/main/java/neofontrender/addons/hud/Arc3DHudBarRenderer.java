package neofontrender.addons.hud;

import icyllis.arc3d.core.Color;
import icyllis.arc3d.core.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
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
    private static final ResourceLocation VANILLA_ICONS = new ResourceLocation("textures/gui/icons.png");
    /** 1.7.10 Gui.drawTexturedModalRect is an instance method; zLevel 0 matches this batch. */
    private static final Gui GUI = new Gui();
    private final Map<String, AnimatedValue> animation = new HashMap<>();

    void draw(String id, HudBarValue sample, HudBarSide side, int x, int y) {
        if (sample == null || sample.maximum <= 0.0F) return;
        float current = animated(id, sample.current);
        float ratio = clamp(current / sample.maximum);
        float secondary = clamp(sample.secondary / sample.maximum);
        float previewEnd = clamp((sample.current + sample.preview) / sample.maximum);
        float secondaryPreviewEnd = clamp((sample.secondary + sample.secondaryPreview) / sample.maximum);
        float depletion = clamp(sample.depletion / sample.maximum);
        float left = x;
        float top = y;
        float right = x + HudBarsConfig.width;
        float bottom = y + HudBarsConfig.height;
        HudBarTheme theme = HudBarTheme.parse(HudBarsConfig.theme);
        float radius = radius(theme);
        float inset = theme == HudBarTheme.MINIMAL ? 0.0F : 1.0F;
        int previewAlpha = sample.animatePreview ? HudBarPulseAnimation.alpha(System.nanoTime()) : 255;

        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean alphaTest = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        int shadeModel = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
        int textureBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        try {
            if (theme != HudBarTheme.MINIMAL) rounded(left, top, right, bottom, radius, HudBarsConfig.border);
            rounded(left + inset, top + inset, right - inset, bottom - inset,
                    Math.max(0.01F, radius - inset), HudBarsConfig.background);
            float innerLeft = left + inset;
            float innerRight = right - inset;
            float innerTop = top + inset;
            float innerBottom = bottom - inset;
            float span = Math.max(0.0F, innerRight - innerLeft);
            if (ratio > 0.0F) {
                if (side == HudBarSide.RIGHT) {
                    fill(innerRight - span * ratio, innerTop, innerRight, innerBottom,
                            Math.min(radius - inset, span * ratio * 0.5F), sample.primaryColor, theme);
                } else {
                    fill(innerLeft, innerTop, innerLeft + span * ratio, innerBottom,
                            Math.min(radius - inset, span * ratio * 0.5F), sample.primaryColor, theme);
                }
            }
            if (secondary > 0.0F) {
                // Saturation is a value in its own right. Drawing it at full height makes its
                // relationship to hunger immediately readable instead of looking like a clipped bar.
                if (side == HudBarSide.RIGHT) fill(innerRight - span * secondary, innerTop,
                        innerRight, innerBottom, Math.min(radius - inset, span * secondary * 0.5F),
                        sample.secondaryColor, theme);
                else fill(innerLeft, innerTop, innerLeft + span * secondary, innerBottom,
                        Math.min(radius - inset, span * secondary * 0.5F), sample.secondaryColor, theme);
            }
            if (sample.preview > 0.0F) {
                drawPreview(side, innerLeft, innerTop, innerRight, innerBottom, span,
                        previewEnd, multiplyAlpha(sample.previewColor, previewAlpha));
            }
            if (sample.secondaryPreview > 0.0F) {
                drawPreview(side, innerLeft, innerTop, innerRight, innerBottom, span,
                        secondaryPreviewEnd, multiplyAlpha(sample.secondaryColor, previewAlpha));
            }
            if (depletion > 0.0F) {
                float stripeBottom = Math.min(innerBottom, innerTop + 1.25F);
                if (side == HudBarSide.RIGHT) quad(innerLeft, innerTop,
                        innerLeft + span * depletion, stripeBottom, sample.depletionColor);
                else quad(innerRight - span * depletion, innerTop,
                        innerRight, stripeBottom, sample.depletionColor);
            }
            if (theme == HudBarTheme.SEGMENTED) {
                int separator = withAlpha(HudBarsConfig.background, 190);
                for (int i = 1; i < 10; i++) {
                    float marker = innerLeft + span * i / 10.0F;
                    quad(marker - 0.35F, innerTop, marker + 0.35F, innerBottom, separator);
                }
            }
            if (theme == HudBarTheme.CLASSIC) drawClassicBevel(innerLeft, innerTop, innerRight, innerBottom);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            drawText(sample.text, x, y);
            if (HudBarsConfig.showIcons) drawIcon(id, side, x, y);
        } finally {
            GL11.glShadeModel(shadeModel);
            if (cull) GL11.glEnable(GL11.GL_CULL_FACE);
            else GL11.glDisable(GL11.GL_CULL_FACE);
            if (texture) GL11.glEnable(GL11.GL_TEXTURE_2D);
            else GL11.glDisable(GL11.GL_TEXTURE_2D);
            if (blend) GL11.glEnable(GL11.GL_BLEND);
            else GL11.glDisable(GL11.GL_BLEND);
            if (depth) GL11.glEnable(GL11.GL_DEPTH_TEST);
            else GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(depthMask);
            if (alphaTest) GL11.glEnable(GL11.GL_ALPHA_TEST);
            else GL11.glDisable(GL11.GL_ALPHA_TEST);
            if (lighting) GL11.glEnable(GL11.GL_LIGHTING);
            else GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureBinding);
        }
    }

    private static float radius(HudBarTheme theme) {
        if (!HudBarsConfig.rounded || theme == HudBarTheme.FLAT || theme == HudBarTheme.SEGMENTED
                || theme == HudBarTheme.CLASSIC) return 0.01F;
        if (theme == HudBarTheme.MINIMAL) return Math.min(2.0F, HudBarsConfig.height * 0.5F);
        return Math.min(3.5F, HudBarsConfig.height * 0.5F);
    }

    private static void fill(float left, float top, float right, float bottom,
                             float radius, int color, HudBarTheme theme) {
        rounded(left, top, right, bottom, Math.max(0.01F, radius), color);
        if (theme == HudBarTheme.GLASS && right - left > 2.0F && bottom - top > 2.0F) {
            float highlightBottom = MathUtil.lerp(top, bottom, 0.42F);
            quad(left + 1.0F, top + 0.75F, right - 1.0F, highlightBottom,
                    withAlpha(0x00FFFFFF, 42));
        }
    }

    private static int withAlpha(int color, int alpha) {
        return color & 0x00FFFFFF | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    private static int multiplyAlpha(int color, int alpha) {
        return withAlpha(color, Color.alpha(color) * Math.max(0, Math.min(255, alpha)) / 255);
    }

    private static void drawPreview(HudBarSide side, float left, float top, float right, float bottom,
                                    float span, float end, int color) {
        if (side == HudBarSide.RIGHT) quad(right - span * end, top, right, bottom, color);
        else quad(left, top, left + span * end, bottom, color);
    }

    private static void drawClassicBevel(float left, float top, float right, float bottom) {
        quad(left, top, right, top + 1.0F, 0x70FFFFFF);
        quad(left, top, left + 1.0F, bottom, 0x70FFFFFF);
        quad(left, bottom - 1.0F, right, bottom, 0x80000000);
        quad(right - 1.0F, top, right, bottom, 0x80000000);
    }

    private static void drawIcon(String id, HudBarSide side, int x, int y) {
        int u;
        int v;
        int backgroundU = -1;
        if (id.endsWith(":health")) { u = 52; v = 0; backgroundU = 16; }
        else if (id.endsWith(":absorption")) { u = 160; v = 0; backgroundU = 16; }
        else if (id.endsWith(":armor")) { u = 43; v = 9; }
        else if (id.endsWith(":food")) { u = 52; v = 27; backgroundU = 16; }
        else if (id.endsWith(":air")) { u = 16; v = 18; }
        else if (id.endsWith(":mount_health")) { u = 88; v = 9; backgroundU = 16; }
        else return;
        int iconX = side == HudBarSide.RIGHT ? x + HudBarsConfig.width + 1 : x - 10;
        int iconY = y + (HudBarsConfig.height - 9) / 2;
        Minecraft.getMinecraft().getTextureManager().bindTexture(VANILLA_ICONS);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        // icons.png is the vanilla 256x256 sheet, which is exactly what 1.7.10's
        // drawTexturedModalRect assumes, so the 1.12 custom-size helper is unnecessary here.
        // 1.7.10 keeps that helper on the Gui instance instead of the class.
        if (backgroundU >= 0) GUI.drawTexturedModalRect(iconX, iconY, backgroundU, v, 9, 9);
        GUI.drawTexturedModalRect(iconX, iconY, u, v, 9, 9);
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
        float scale = Math.max(0.5F, Math.min(1.25F, HudBarsConfig.textScale / 100.0F));
        float textX = "classic".equals(HudBarsConfig.textPosition)
                ? x + 9.0F
                : x + (HudBarsConfig.width - font.getStringWidth(text) * scale) * 0.5F;
        float textY = y + (HudBarsConfig.height - font.FONT_HEIGHT * scale) * 0.5F;
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(textX, textY, 0.0F);
            GL11.glScalef(scale, scale, 1.0F);
            font.drawString(text, 0, 0, 0xFFFFFFFF, true);
        } finally {
            GL11.glPopMatrix();
        }
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
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_TRIANGLE_FAN);
        vertex((left + right) * 0.5F, (top + bottom) * 0.5F, color);
        for (Point point : points) vertex(point.x, point.y, color);
        vertex(points.get(0).x, points.get(0).y, color);
        tessellator.draw();
    }

    private static void quad(float left, float top, float right, float bottom, int color) {
        if (right <= left || bottom <= top) return;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_QUADS);
        vertex(left, top, color); vertex(left, bottom, color);
        vertex(right, bottom, color); vertex(right, top, color);
        tessellator.draw();
    }

    private static void corner(List<Point> out, float x, float y, float radius, int from, int to) {
        int segments = 5;
        for (int i = 0; i <= segments; i++) {
            double angle = Math.toRadians(MathUtil.lerp(from, to, i / (float) segments));
            out.add(new Point(x + (float) Math.cos(angle) * radius, y + (float) Math.sin(angle) * radius));
        }
    }

    private static void vertex(float x, float y, int color) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.setColorRGBA_F(Color.red(color) / 255.0F, Color.green(color) / 255.0F,
                Color.blue(color) / 255.0F, Color.alpha(color) / 255.0F);
        tessellator.addVertex(x, y, Z);
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
