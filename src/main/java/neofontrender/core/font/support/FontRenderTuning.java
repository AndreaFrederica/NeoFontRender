package neofontrender.core.font.support;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.texture.AbstractTexture;
import neofontrender.core.config.NeofontrenderConfig;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

/**
 * Runtime choices for font raster scale and texture filtering.
 *
 * <p>When adaptive scaling is enabled, the configured oversample is only the
 * manual-mode value. Adaptive mode follows the current physical text scale and
 * snaps it into a bounded set of cache buckets so small UI text is not baked far
 * above its final framebuffer size.</p>
 */
public final class FontRenderTuning {

    private static final float INTEGER_EPSILON = 0.03F;
    private static final int GL_TEXTURE_LOD_BIAS = 0x8501;
    private static final int GL_TEXTURE_MAX_ANISOTROPY_EXT = 0x84FE;
    private static final int GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT = 0x84FF;
    private static final float[] MODELVIEW = new float[16];
    private static final float[] PROJECTION = new float[16];
    private static volatile DrawContext currentContext;

    private FontRenderTuning() {
    }

    public static DrawContext updateFromCurrentGlState() {
        return updateFromCurrentGlState(false);
    }

    public static DrawContext updateFromCurrentGlState(boolean shadow) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.displayWidth <= 0 || mc.displayHeight <= 0) {
                return currentContext;
            }

            FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buffer);
            buffer.get(MODELVIEW);
            buffer.clear();
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, buffer);
            buffer.get(PROJECTION);

            float[] origin = projectToFramebuffer(0.0F, 0.0F, 0.0F, mc.displayWidth, mc.displayHeight);
            float[] xAxis = projectToFramebuffer(1.0F, 0.0F, 0.0F, mc.displayWidth, mc.displayHeight);
            float[] yAxis = projectToFramebuffer(0.0F, 1.0F, 0.0F, mc.displayWidth, mc.displayHeight);
            float scaleX = distance(origin, xAxis);
            float scaleY = distance(origin, yAxis);
            float scale = (scaleX + scaleY) * 0.5F;
            if (Float.isFinite(scale) && scale > 0.0F) {
                boolean orthographic = Math.abs(PROJECTION[15]) > 0.5F;
                boolean rotation = Math.abs(MODELVIEW[1]) > 0.0001F || Math.abs(MODELVIEW[4]) > 0.0001F;
                boolean fractional = !isNearInteger(origin[0]) || !isNearInteger(origin[1]);
                float clamped = clamp(scale, 0.25F, 64.0F);
                currentContext = new DrawContext(
                        clamped,
                        roundIf(clamped, clamped * NeofontrenderConfig.scaleRoundingToleranceRate()),
                        orthographic,
                        rotation,
                        fractional,
                        !orthographic,
                        shadow);
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
        return currentContext;
    }

    public static float rasterScale(float configuredOversample) {
        float configured = clamp(configuredOversample, 1.0F, 16.0F);
        if (!NeofontrenderConfig.adaptiveRasterScale()) {
            return configured;
        }
        float min = adaptiveRasterMin();
        float max = adaptiveRasterMax(min);
        float target = currentDrawContext().roundedPixelScale() * 2.0F;
        return rasterScaleBucket(clamp(target, min, max), configured);
    }

    public static float rasterScaleBucket(float rasterScale, float configuredOversample) {
        float configured = clamp(configuredOversample, 1.0F, 16.0F);
        if (!NeofontrenderConfig.adaptiveRasterScale()) {
            return configured;
        }
        float min = adaptiveRasterMin();
        float max = adaptiveRasterMax(min);
        float step = adaptiveRasterStep();
        float bucket = Math.round(rasterScale / step) * step;
        return clamp(bucket, min, max);
    }

    public static float textureScale(float rasterScale) {
        if (!NeofontrenderConfig.adaptiveRasterScale()) {
            return 1.0F;
        }
        return rasterScale * 8.0F <= NeofontrenderConfig.blurReductionThreshold() ? 2.0F : 1.0F;
    }

    public static void applyFontTextureFilter(AbstractTexture texture, float rasterScale) {
        applyFontTextureFilter(texture, rasterScale, true);
    }

    public static void applyFontTextureFilter(AbstractTexture texture, float rasterScale, boolean allowMipmap) {
        boolean linear = useLinearFiltering(rasterScale);
        texture.setBlurMipmap(linear, allowMipmap && linear && NeofontrenderConfig.renderingMipmap());
    }

    public static void applyBoundTextureFilter(float rasterScale) {
        applyBoundTextureFilter(rasterScale, true);
    }

    public static void applyBoundTextureFilter(float rasterScale, boolean allowMipmap) {
        boolean linear = useLinearFiltering(rasterScale);
        int min = linear
                ? (allowMipmap && NeofontrenderConfig.renderingMipmap() ? GL11.GL_LINEAR_MIPMAP_LINEAR : GL11.GL_LINEAR)
                : GL11.GL_NEAREST;
        int mag = linear ? GL11.GL_LINEAR : GL11.GL_NEAREST;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, min);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, mag);
        if (NeofontrenderConfig.adaptiveRasterScale()) {
            applyLodBias();
            applyAnisotropicFiltering();
        }
    }

    public static boolean useLinearFiltering(float rasterScale) {
        if (!NeofontrenderConfig.renderingInterpolation()) {
            return false;
        }
        if (!NeofontrenderConfig.adaptiveRasterScale()) {
            return true;
        }

        DrawContext context = currentDrawContext();
        float fontResolution = rasterScale * 8.0F;
        if (context.shadow() && fontResolution < NeofontrenderConfig.smoothShadowThreshold()) {
            return false;
        }
        if (NeofontrenderConfig.excludeHighMagnification()
                && context.roundedPixelScale() >= rasterScale * NeofontrenderConfig.limitMagnification()) {
            return false;
        }
        if (NeofontrenderConfig.excludeIntegerScale()
                && context.orthographic()
                && isNearInteger(context.roundedPixelScale() / Math.max(1.0F, rasterScale))) {
            return false;
        }

        // NOTE: SmoothFont only excludes at exact multiples of (fontRes/8).
        // The old ratio-based exclusions (ratio near-integer, 1/ratio near-integer)
        // were too aggressive — they fired at screenScale=3, rasterScale=6 (1/ratio=2)
        // which is a perfectly normal GUI rendering scenario. Removed to match
        // SmoothFont behavior where linear filtering is almost never excluded.
        return true;
    }

    public static float currentGuiScale() {
        DrawContext context = currentContext;
        float measured = context == null ? 0.0F : context.pixelScale();
        if (measured > 0.0F && context != null) {
            return measured;
        }
        return scaledResolutionScale();
    }

    private static float scaledResolutionScale() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.displayWidth > 0 && mc.displayHeight > 0) {
                return Math.max(1, new ScaledResolution(mc).getScaleFactor());
            }
        } catch (RuntimeException ignored) {
        }
        return 1.0F;
    }

    public static DrawContext currentDrawContext() {
        DrawContext context = currentContext;
        return context == null ? DrawContext.fallback(scaledResolutionScale()) : context;
    }

    public static boolean isCurrentTextQuadVisible(float x, float y, float width, float height,
                                                   float minPixelHeight) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.displayWidth <= 0 || mc.displayHeight <= 0) {
            return true;
        }

        float[][] corners = {
                projectVisiblePoint(x, y, 0.0F, mc.displayWidth, mc.displayHeight),
                projectVisiblePoint(x + width, y, 0.0F, mc.displayWidth, mc.displayHeight),
                projectVisiblePoint(x, y + height, 0.0F, mc.displayWidth, mc.displayHeight),
                projectVisiblePoint(x + width, y + height, 0.0F, mc.displayWidth, mc.displayHeight)
        };
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (float[] corner : corners) {
            // A quad crossing the near plane is conservatively retained to avoid visible popping.
            if (corner == null) {
                return true;
            }
            minX = Math.min(minX, corner[0]);
            minY = Math.min(minY, corner[1]);
            maxX = Math.max(maxX, corner[0]);
            maxY = Math.max(maxY, corner[1]);
        }

        float halfWidth = mc.displayWidth * 0.5F;
        float halfHeight = mc.displayHeight * 0.5F;
        if (maxX < -halfWidth || minX > halfWidth || maxY < -halfHeight || minY > halfHeight) {
            return false;
        }
        return maxY - minY >= Math.max(0.0F, minPixelHeight);
    }

    public static float alignToPixel(float value) {
        DrawContext context = currentDrawContext();
        if (NeofontrenderConfig.adaptiveRasterScale() && (!context.orthographic() || context.rotation())) {
            return value;
        }
        float scale = context.pixelScale();
        if (scale <= 0.0F) {
            return value;
        }
        return Math.round(value * scale) / scale;
    }

    private static void applyLodBias() {
        if (!NeofontrenderConfig.renderingMipmap()) {
            return;
        }
        DrawContext context = currentDrawContext();
        float bias = context.orthographic()
                ? NeofontrenderConfig.overlayMipmapLodBias()
                : NeofontrenderConfig.mipmapLodBias();
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, bias);
    }

    private static void applyAnisotropicFiltering() {
        if (!NeofontrenderConfig.anisotropicFiltering() || currentDrawContext().orthographic()) {
            return;
        }
        try {
            float max = GL11.glGetFloat(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
            if (max > 1.0F) {
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, max);
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private static float[] projectToFramebuffer(float x, float y, float z, int displayWidth, int displayHeight) {
        float[] eye = transform(MODELVIEW, x, y, z, 1.0F);
        float[] clip = transform(PROJECTION, eye[0], eye[1], eye[2], eye[3]);
        if (clip[3] == 0.0F) {
            return new float[] {0.0F, 0.0F};
        }
        float invW = 1.0F / clip[3];
        float ndcX = clip[0] * invW;
        float ndcY = clip[1] * invW;
        return new float[] {
                ndcX * displayWidth * 0.5F,
                ndcY * displayHeight * 0.5F
        };
    }

    private static float[] projectVisiblePoint(float x, float y, float z,
                                               int displayWidth, int displayHeight) {
        float[] eye = transform(MODELVIEW, x, y, z, 1.0F);
        float[] clip = transform(PROJECTION, eye[0], eye[1], eye[2], eye[3]);
        if (!Float.isFinite(clip[3]) || clip[3] <= 0.0F) {
            return null;
        }
        float invW = 1.0F / clip[3];
        return new float[] {
                clip[0] * invW * displayWidth * 0.5F,
                clip[1] * invW * displayHeight * 0.5F
        };
    }

    private static float[] transform(float[] m, float x, float y, float z, float w) {
        return new float[] {
                m[0] * x + m[4] * y + m[8] * z + m[12] * w,
                m[1] * x + m[5] * y + m[9] * z + m[13] * w,
                m[2] * x + m[6] * y + m[10] * z + m[14] * w,
                m[3] * x + m[7] * y + m[11] * z + m[15] * w
        };
    }

    private static float distance(float[] a, float[] b) {
        float dx = b[0] - a[0];
        float dy = b[1] - a[1];
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static boolean isNearInteger(float value) {
        return Math.abs(value - Math.round(value)) <= INTEGER_EPSILON;
    }

    private static float roundIf(float value, float tolerance) {
        float rounded = Math.round(value);
        return Math.abs(value - rounded) <= Math.max(INTEGER_EPSILON, tolerance) ? rounded : value;
    }

    private static float adaptiveRasterMin() {
        return clamp(NeofontrenderConfig.adaptiveRasterMin(), 0.25F, 64.0F);
    }

    private static float adaptiveRasterMax(float min) {
        return clamp(Math.max(min, NeofontrenderConfig.adaptiveRasterMax()), min, 64.0F);
    }

    private static float adaptiveRasterStep() {
        return clamp(NeofontrenderConfig.adaptiveRasterStep(), 0.125F, 8.0F);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class DrawContext {
        private final float pixelScale;
        private final float roundedPixelScale;
        private final boolean orthographic;
        private final boolean rotation;
        private final boolean fractionalCoordinate;
        private final boolean perspective;
        private final boolean shadow;

        private DrawContext(float pixelScale, float roundedPixelScale, boolean orthographic,
                            boolean rotation, boolean fractionalCoordinate, boolean perspective,
                            boolean shadow) {
            this.pixelScale = pixelScale;
            this.roundedPixelScale = roundedPixelScale;
            this.orthographic = orthographic;
            this.rotation = rotation;
            this.fractionalCoordinate = fractionalCoordinate;
            this.perspective = perspective;
            this.shadow = shadow;
        }

        private static DrawContext fallback(float scale) {
            float safeScale = clamp(scale, 0.25F, 64.0F);
            return new DrawContext(safeScale, Math.round(safeScale), true, false, false, false, false);
        }

        public float pixelScale() {
            return pixelScale;
        }

        public float roundedPixelScale() {
            return roundedPixelScale;
        }

        public boolean orthographic() {
            return orthographic;
        }

        public boolean rotation() {
            return rotation;
        }

        public boolean fractionalCoordinate() {
            return fractionalCoordinate;
        }

        public boolean perspective() {
            return perspective;
        }

        public boolean shadow() {
            return shadow;
        }
    }
}
