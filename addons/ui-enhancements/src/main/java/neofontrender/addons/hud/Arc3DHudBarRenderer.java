package neofontrender.addons.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import neofontrender.addons.hud.api.HudBarSide;
import neofontrender.addons.hud.api.HudBarValue;
import neofontrender.api.arc3d.Arc3DApi;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/** Uses Arc3D-compatible color math with Minecraft 1.7's LWJGL2 tessellator. */
final class Arc3DHudBarRenderer {
    private static final double Z = 0.0D;

    private final Map<String, AnimatedValue> animation = new HashMap<String, AnimatedValue>();
    private final FloatBuffer colorBuffer = BufferUtils.createFloatBuffer(4);

    void draw(String id, HudBarValue sample, HudBarSide side, int x, int y) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("bar id must not be empty");
        if (sample == null) throw new IllegalArgumentException("bar sample must not be null");
        if (side == null) throw new IllegalArgumentException("bar side must not be null");

        float ratio = animated(id, sample.current) / sample.maximum;
        float secondary = sample.secondary / sample.maximum;
        float preview = sample.preview / sample.maximum;
        float depletion = sample.depletion / sample.maximum;
        float left = x;
        float top = y;
        float right = x + HudBarsConfig.width;
        float bottom = y + HudBarsConfig.height;
        HudBarTheme theme = HudBarTheme.parse(HudBarsConfig.theme);
        float radius = radius(theme);
        float inset = theme == HudBarTheme.MINIMAL ? 0.0F : 1.0F;

        GlState state = GlState.capture(colorBuffer);
        try {
            configureDrawingState();
            drawBackground(left, top, right, bottom, radius, inset, theme);
            drawLayers(sample, side, ratio, secondary, preview, depletion,
                    left + inset, top + inset, right - inset, bottom - inset, radius - inset, theme);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            drawText(sample.text, x, y);
        } finally {
            state.restore();
        }
    }

    private static void configureDrawingState() {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawBackground(float left, float top, float right, float bottom,
                                       float radius, float inset, HudBarTheme theme) {
        if (theme != HudBarTheme.MINIMAL) {
            rounded(left, top, right, bottom, radius, HudBarsConfig.border);
        }
        rounded(left + inset, top + inset, right - inset, bottom - inset,
                Math.max(0.01F, radius - inset), HudBarsConfig.background);
    }

    private static void drawLayers(HudBarValue sample, HudBarSide side, float ratio, float secondary,
                                   float preview, float depletion, float left, float top, float right,
                                   float bottom, float radius, HudBarTheme theme) {
        float span = Math.max(0.0F, right - left);
        if (ratio > 0.0F) {
            if (side == HudBarSide.RIGHT) {
                fill(right - span * ratio, top, right, bottom,
                        Math.min(radius, span * ratio * 0.5F), sample.primaryColor, theme);
            } else {
                fill(left, top, left + span * ratio, bottom,
                        Math.min(radius, span * ratio * 0.5F), sample.primaryColor, theme);
            }
        }
        if (secondary > 0.0F) {
            if (side == HudBarSide.RIGHT) {
                fill(right - span * secondary, top, right, bottom,
                        Math.min(radius, span * secondary * 0.5F), sample.secondaryColor, theme);
            } else {
                fill(left, top, left + span * secondary, bottom,
                        Math.min(radius, span * secondary * 0.5F), sample.secondaryColor, theme);
            }
        }
        if (preview > 0.0F && ratio < 1.0F) {
            float end = Math.min(1.0F, ratio + preview);
            if (side == HudBarSide.RIGHT) {
                quad(right - span * end, top, right - span * ratio, bottom, sample.previewColor);
            } else {
                quad(left + span * ratio, top, left + span * end, bottom, sample.previewColor);
            }
        }
        if (depletion > 0.0F) {
            float stripeBottom = Math.min(bottom, top + 1.25F);
            if (side == HudBarSide.RIGHT) {
                quad(left, top, left + span * depletion, stripeBottom, sample.depletionColor);
            } else {
                quad(right - span * depletion, top, right, stripeBottom, sample.depletionColor);
            }
        }
        if (theme == HudBarTheme.SEGMENTED) {
            int separator = withAlpha(HudBarsConfig.background, 190);
            for (int index = 1; index < 10; index++) {
                float marker = left + span * index / 10.0F;
                quad(marker - 0.35F, top, marker + 0.35F, bottom, separator);
            }
        }
    }

    private static float radius(HudBarTheme theme) {
        if (!HudBarsConfig.rounded || theme == HudBarTheme.CLASSIC
                || theme == HudBarTheme.FLAT || theme == HudBarTheme.SEGMENTED) {
            return 0.01F;
        }
        if (theme == HudBarTheme.MINIMAL) return Math.min(2.0F, HudBarsConfig.height * 0.5F);
        return Math.min(3.5F, HudBarsConfig.height * 0.5F);
    }

    private static void fill(float left, float top, float right, float bottom,
                             float radius, int color, HudBarTheme theme) {
        rounded(left, top, right, bottom, Math.max(0.01F, radius), color);
        if (right - left <= 2.0F || bottom - top <= 2.0F) return;
        if (theme == HudBarTheme.GLASS) {
            quad(left + 1.0F, top + 0.75F, right - 1.0F, Arc3DApi.lerp(top, bottom, 0.42F),
                    withAlpha(0x00FFFFFF, 42));
        } else if (theme == HudBarTheme.CLASSIC) {
            quad(left, top, right, Math.min(bottom, top + 1.0F), withAlpha(0x00FFFFFF, 40));
            quad(left, Math.max(top, bottom - 1.0F), right, bottom, withAlpha(0x00000000, 56));
        }
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
        value.value = Arc3DApi.lerp(value.value, target, factor);
        if (Math.abs(value.value - target) < 0.01F) value.value = target;
        value.nanos = now;
        return value.value;
    }

    private static void drawText(String text, int x, int y) {
        if (text.isEmpty()) return;
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int textX = x + (HudBarsConfig.width - font.getStringWidth(text)) / 2;
        int textY = y + (HudBarsConfig.height - font.FONT_HEIGHT) / 2;
        font.drawString(text, textX, textY, 0xFFFFFFFF, true);
    }

    private static void rounded(float left, float top, float right, float bottom, float requestedRadius, int color) {
        if (right <= left || bottom <= top) return;
        float radius = Math.max(0.01F, Math.min(requestedRadius,
                Math.min((right - left) * 0.5F, (bottom - top) * 0.5F)));
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_TRIANGLE_FAN);
        setColor(tessellator, color);
        tessellator.addVertex((left + right) * 0.5F, (top + bottom) * 0.5F, Z);
        corner(tessellator, right - radius, top + radius, radius, -90, 0);
        corner(tessellator, right - radius, bottom - radius, radius, 0, 90);
        corner(tessellator, left + radius, bottom - radius, radius, 90, 180);
        corner(tessellator, left + radius, top + radius, radius, 180, 270);
        tessellator.addVertex(right - radius, top, Z);
        tessellator.draw();
    }

    private static void quad(float left, float top, float right, float bottom, int color) {
        if (right <= left || bottom <= top) return;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        setColor(tessellator, color);
        tessellator.addVertex(left, top, Z);
        tessellator.addVertex(left, bottom, Z);
        tessellator.addVertex(right, bottom, Z);
        tessellator.addVertex(right, top, Z);
        tessellator.draw();
    }

    private static void corner(Tessellator tessellator, float x, float y, float radius, int from, int to) {
        int segments = 5;
        for (int index = 0; index <= segments; index++) {
            double angle = Math.toRadians(Arc3DApi.lerp(from, to, index / (float) segments));
            tessellator.addVertex(x + Math.cos(angle) * radius, y + Math.sin(angle) * radius, Z);
        }
    }

    private static void setColor(Tessellator tessellator, int color) {
        tessellator.setColorRGBA(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24);
    }

    private static int withAlpha(int color, int alpha) {
        return color & 0x00FFFFFF | Math.max(0, Math.min(255, alpha)) << 24;
    }

    private static final class AnimatedValue {
        private float value;
        private long nanos;

        private AnimatedValue(float value, long nanos) {
            this.value = value;
            this.nanos = nanos;
        }
    }

    private static final class GlState {
        private final boolean lighting;
        private final boolean depth;
        private final boolean depthMask;
        private final boolean blend;
        private final boolean alpha;
        private final boolean texture;
        private final boolean cull;
        private final int shadeModel;
        private final int textureBinding;
        private final int blendSourceRgb;
        private final int blendDestinationRgb;
        private final int blendSourceAlpha;
        private final int blendDestinationAlpha;
        private final float red;
        private final float green;
        private final float blue;
        private final float colorAlpha;

        private GlState(boolean lighting, boolean depth, boolean depthMask, boolean blend, boolean alpha,
                        boolean texture, boolean cull, int shadeModel, int textureBinding,
                        int blendSourceRgb, int blendDestinationRgb, int blendSourceAlpha,
                        int blendDestinationAlpha, float red, float green, float blue, float colorAlpha) {
            this.lighting = lighting;
            this.depth = depth;
            this.depthMask = depthMask;
            this.blend = blend;
            this.alpha = alpha;
            this.texture = texture;
            this.cull = cull;
            this.shadeModel = shadeModel;
            this.textureBinding = textureBinding;
            this.blendSourceRgb = blendSourceRgb;
            this.blendDestinationRgb = blendDestinationRgb;
            this.blendSourceAlpha = blendSourceAlpha;
            this.blendDestinationAlpha = blendDestinationAlpha;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.colorAlpha = colorAlpha;
        }

        private static GlState capture(FloatBuffer colorBuffer) {
            colorBuffer.clear();
            GL11.glGetFloat(GL11.GL_CURRENT_COLOR, colorBuffer);
            return new GlState(
                    GL11.glIsEnabled(GL11.GL_LIGHTING),
                    GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                    GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                    GL11.glIsEnabled(GL11.GL_BLEND),
                    GL11.glIsEnabled(GL11.GL_ALPHA_TEST),
                    GL11.glIsEnabled(GL11.GL_TEXTURE_2D),
                    GL11.glIsEnabled(GL11.GL_CULL_FACE),
                    GL11.glGetInteger(GL11.GL_SHADE_MODEL),
                    GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
                    colorBuffer.get(0), colorBuffer.get(1), colorBuffer.get(2), colorBuffer.get(3));
        }

        private void restore() {
            setEnabled(GL11.GL_LIGHTING, lighting);
            setEnabled(GL11.GL_DEPTH_TEST, depth);
            GL11.glDepthMask(depthMask);
            setEnabled(GL11.GL_BLEND, blend);
            OpenGlHelper.glBlendFunc(
                    blendSourceRgb, blendDestinationRgb, blendSourceAlpha, blendDestinationAlpha);
            setEnabled(GL11.GL_ALPHA_TEST, alpha);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureBinding);
            setEnabled(GL11.GL_TEXTURE_2D, texture);
            setEnabled(GL11.GL_CULL_FACE, cull);
            GL11.glShadeModel(shadeModel);
            GL11.glColor4f(red, green, blue, colorAlpha);
        }

        private static void setEnabled(int capability, boolean enabled) {
            if (enabled) GL11.glEnable(capability);
            else GL11.glDisable(capability);
        }
    }
}
