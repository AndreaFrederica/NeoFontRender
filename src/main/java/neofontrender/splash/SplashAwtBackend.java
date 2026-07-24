package neofontrender.splash;

import neofontrender.NeoFontRender;
import neofontrender.core.config.NeofontrenderConfig;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.EXTBlendFuncSeparate;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GLContext;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * AWT-based rasterization backend for Forge and ModernSplash loading screens.
 *
 * <p>Each distinct string is rasterized once into a GL texture and cached. The texture is
 * drawn as a quad tinted by the theme color already active in OpenGL. Text is oversampled for
 * quality, while advance and pixel bounds come from the same {@link TextLayout} so long strings
 * and overhanging glyphs are not clipped.</p>
 */
public final class SplashAwtBackend {

    public static final int LOGICAL_HEIGHT = 8;
    private static final int OVERSAMPLE = 2;
    private static final int MAX_CACHE_ENTRIES = 128;

    private final Font font;
    private final FontRenderContext fontRenderContext;
    private final float outlineStrokeWidth;
    private final Map<String, RenderedString> cache = new LinkedHashMap<String, RenderedString>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, RenderedString> eldest) {
            if (size() <= MAX_CACHE_ENTRIES) {
                return false;
            }
            eldest.getValue().delete();
            return true;
        }
    };

    public SplashAwtBackend() {
        float size = resolveFontSize();
        ResolvedFont resolved = resolveFont();
        this.font = resolved.font.deriveFont(Math.max(4.0F, size));
        this.outlineStrokeWidth = SplashFontWeight.outlineStrokeWidth(
                this.font.getSize2D(), resolved.weight.emboldenDelta);
        this.fontRenderContext = new FontRenderContext(null, true,
                NeofontrenderConfig.fontFractionalMetrics());

        if (resolved.weight.adjusted) {
            NeoFontRender.LOGGER.info(
                    "Splash font '{}' uses approximate AWT weight {} (source default={}, stroke={}px{})",
                    this.font.getFontName(), resolved.weight.appliedTarget,
                    resolved.weight.appliedTarget - resolved.weight.emboldenDelta,
                    String.format(Locale.ROOT, "%.3f", outlineStrokeWidth),
                    resolved.weight.clamped
                            ? ", requested " + resolved.weight.requestedTarget + " was clamped"
                            : "");
        }
    }

    private static ResolvedFont resolveFont() {
        for (String candidate : NeofontrenderConfig.fontFamily()) {
            LoadedFont loaded = loadFont(candidate);
            if (loaded != null) {
                return applyWeight(loaded);
            }
        }
        LoadedFont fallback = new LoadedFont(new Font(Font.SANS_SERIF, Font.PLAIN, 1), null);
        return applyWeight(fallback);
    }

    private static ResolvedFont applyWeight(LoadedFont loaded) {
        SplashFontWeight.Resolution weight = SplashFontWeight.resolve(
                loaded.weightAxis, NeofontrenderConfig.fontVariableWeight(),
                NeofontrenderConfig.fontStyle(), loaded.font.getFontName());
        Font styled = loaded.font.deriveFont(weight.awtStyle, 1.0F);
        return new ResolvedFont(styled, weight);
    }

    private static LoadedFont loadFont(String candidate) {
        if (candidate == null || candidate.trim().isEmpty()) {
            return null;
        }
        String name = candidate.trim();
        try {
            File file = new File(name);
            if (file.isFile()) {
                return loadFontBytes(Files.readAllBytes(file.toPath()));
            }

            int separator = name.indexOf(':');
            if (separator > 0 && separator < name.length() - 1) {
                String resourcePath = "/assets/" + name.substring(0, separator) + "/"
                        + name.substring(separator + 1);
                try (InputStream stream = SplashAwtBackend.class.getResourceAsStream(resourcePath)) {
                    if (stream != null) {
                        return loadFontBytes(stream.readAllBytes());
                    }
                }
                return null;
            }

            Font system = new Font(name, Font.PLAIN, 1);
            if (!Font.DIALOG.equals(system.getFamily()) || Font.DIALOG.equalsIgnoreCase(name)) {
                return new LoadedFont(system, SplashFontWeight.inferSystemAxis(system, name));
            }
        } catch (Exception e) {
            NeoFontRender.LOGGER.debug("Could not load splash font candidate '{}'", name, e);
            // Try the next configured fallback.
        }
        return null;
    }

    private static LoadedFont loadFontBytes(byte[] data) throws Exception {
        SplashFontWeight.WeightAxis weightAxis = SplashFontWeight.inspect(data);
        Font font;
        try (ByteArrayInputStream stream = new ByteArrayInputStream(data)) {
            font = Font.createFont(Font.TRUETYPE_FONT, stream);
        }
        return new LoadedFont(font, weightAxis);
    }

    private static float resolveFontSize() {
        float configured = NeofontrenderConfig.fontSize();
        if (configured <= 0) {
            configured = 8.5F;
        }
        // Map the configured Minecraft UI size to an AWT point size that fills the logical cell.
        return configured * ((float) LOGICAL_HEIGHT / 8.5F) * OVERSAMPLE;
    }

    public float measureString(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0F;
        }
        return renderToCache(text).logicalWidth;
    }

    public void drawString(String text, float x, float y, int color) {
        RenderedString rendered = renderToCache(text);
        if (rendered.textureId <= 0 || rendered.logicalWidth <= 0) {
            return;
        }

        try (TextGlState ignored = new TextGlState()) {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, rendered.textureId);

            // ModernSplash sets its configured theme color and fade alpha before calling the
            // renderer. The method's black color argument is only a bitmap-font placeholder.
            // Preserve the current GL color so dark mode and custom theme colors keep working.
            float x0 = x + rendered.xOffset;
            float y0 = y + rendered.baselineOffset;
            float x1 = x0 + rendered.textureWidth;
            float y1 = y0 + rendered.textureHeight;

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0.0F, 0.0F);
            GL11.glVertex2f(x0, y0);
            GL11.glTexCoord2f(0.0F, 1.0F);
            GL11.glVertex2f(x0, y1);
            GL11.glTexCoord2f(1.0F, 1.0F);
            GL11.glVertex2f(x1, y1);
            GL11.glTexCoord2f(1.0F, 0.0F);
            GL11.glVertex2f(x1, y0);
            GL11.glEnd();
        }
    }

    private synchronized RenderedString renderToCache(String text) {
        RenderedString cached = cache.get(text);
        if (cached != null) {
            return cached;
        }

        RenderedString rendered = rasterize(text);
        cache.put(text, rendered);
        return rendered;
    }

    private RenderedString rasterize(String text) {
        TextLayout layout = new TextLayout(text, font, fontRenderContext);
        Rectangle pixelBounds = layout.getPixelBounds(fontRenderContext, 0.0F, 0.0F);
        float logicalWidth = layout.getAdvance() / OVERSAMPLE;
        if (pixelBounds.width <= 0 || pixelBounds.height <= 0) {
            return RenderedString.empty(logicalWidth);
        }

        int padding = rasterPadding(outlineStrokeWidth);
        int width = pixelBounds.width + padding * 2;
        int height = pixelBounds.height + padding * 2;
        float drawX = padding - pixelBounds.x;
        float baseline = padding - pixelBounds.y;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, width, height);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                NeofontrenderConfig.fontLcdSubpixel()
                        ? RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB
                        : RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                NeofontrenderConfig.fontFractionalMetrics()
                        ? RenderingHints.VALUE_FRACTIONALMETRICS_ON
                        : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);

        g.setColor(Color.WHITE);
        drawLayout(g, layout, drawX, baseline, outlineStrokeWidth);
        g.dispose();

        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        Bounds bounds = cropBounds(pixels, width, height);

        if (bounds.isEmpty()) {
            return RenderedString.empty(logicalWidth);
        }

        int cropW = bounds.maxX - bounds.minX + 1;
        int cropH = bounds.maxY - bounds.minY + 1;
        int textureId = uploadTexture(pixels, width, height, bounds);

        float textureWidth = cropW / (float) OVERSAMPLE;
        float textureHeight = cropH / (float) OVERSAMPLE;
        float xOffset = (bounds.minX - drawX) / OVERSAMPLE;
        float referenceBaseline = NeofontrenderConfig.fontAutoBaseline()
                ? NeofontrenderConfig.fontReferenceBaseline()
                : layout.getAscent() / OVERSAMPLE;
        float baselineOffset = referenceBaseline
                - (baseline - bounds.minY) / OVERSAMPLE
                + NeofontrenderConfig.fontBaselineShift();

        return new RenderedString(textureId, logicalWidth, textureWidth, textureHeight,
                xOffset, baselineOffset);
    }

    static void drawLayout(Graphics2D graphics, TextLayout layout, float drawX, float baseline,
                           float strokeWidth) {
        graphics.setComposite(AlphaComposite.SrcOver);
        if (strokeWidth > 0.0F) {
            graphics.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            graphics.draw(layout.getOutline(
                    AffineTransform.getTranslateInstance(drawX, baseline)));
        }
        layout.draw(graphics, drawX, baseline);
    }

    static int rasterPadding(float strokeWidth) {
        int padding = OVERSAMPLE * 2;
        if (strokeWidth > 0.0F) {
            padding += (int) Math.ceil(strokeWidth / 2.0F) + 1;
        }
        return padding;
    }

    private static int uploadTexture(int[] src, int srcW, int srcH, Bounds bounds) {
        int w = bounds.maxX - bounds.minX + 1;
        int h = bounds.maxY - bounds.minY + 1;

        ByteBuffer buffer = BufferUtils.createByteBuffer(w * h * 4);
        for (int y = bounds.minY; y <= bounds.maxY; y++) {
            for (int x = bounds.minX; x <= bounds.maxX; x++) {
                int argb = src[y * srcW + x];
                buffer.put((byte) ((argb >> 16) & 0xFF));
                buffer.put((byte) ((argb >> 8) & 0xFF));
                buffer.put((byte) (argb & 0xFF));
                buffer.put((byte) ((argb >> 24) & 0xFF));
            }
        }
        buffer.flip();

        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        return textureId;
    }

    private static Bounds cropBounds(int[] pixels, int width, int height) {
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if ((pixels[y * width + x] >>> 24) != 0) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        return new Bounds(minX, minY, maxX, maxY);
    }

    private static final class Bounds {
        final int minX, minY, maxX, maxY;

        Bounds(int minX, int minY, int maxX, int maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        boolean isEmpty() {
            return maxX < 0 || maxY < 0;
        }
    }

    private static final class LoadedFont {
        final Font font;
        final SplashFontWeight.WeightAxis weightAxis;

        LoadedFont(Font font, SplashFontWeight.WeightAxis weightAxis) {
            this.font = font;
            this.weightAxis = weightAxis;
        }
    }

    private static final class ResolvedFont {
        final Font font;
        final SplashFontWeight.Resolution weight;

        ResolvedFont(Font font, SplashFontWeight.Resolution weight) {
            this.font = font;
            this.weight = weight;
        }
    }

    /** Keeps Forge's alpha test from cutting off anti-aliased edge coverage. */
    private static final class TextGlState implements AutoCloseable {
        private final BlendFunctionPath blendFunctionPath;
        private final boolean blendEnabled;
        private final boolean alphaTestEnabled;
        private final int srcRgb;
        private final int dstRgb;
        private final int srcAlpha;
        private final int dstAlpha;

        TextGlState() {
            try {
                ContextCapabilities capabilities = GLContext.getCapabilities();
                this.blendFunctionPath = selectBlendFunctionPath(
                        capabilities.OpenGL14, capabilities.GL_EXT_blend_func_separate);
                this.blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
                this.alphaTestEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
                this.srcRgb = GL11.glGetInteger(blendFunctionPath.srcRgbParameter);
                this.dstRgb = GL11.glGetInteger(blendFunctionPath.dstRgbParameter);
                this.srcAlpha = blendFunctionPath.separate
                        ? GL11.glGetInteger(blendFunctionPath.srcAlphaParameter)
                        : srcRgb;
                this.dstAlpha = blendFunctionPath.separate
                        ? GL11.glGetInteger(blendFunctionPath.dstAlphaParameter)
                        : dstRgb;

                GL11.glEnable(GL11.GL_BLEND);
                blendFunctionPath.apply(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                        GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glDisable(GL11.GL_ALPHA_TEST);
            } catch (RuntimeException | LinkageError e) {
                NeoFontRender.LOGGER.error("Failed to configure splash text OpenGL state", e);
                throw e;
            }
        }

        @Override
        public void close() {
            try {
                restore();
            } catch (RuntimeException | LinkageError e) {
                NeoFontRender.LOGGER.error("Failed to restore splash text OpenGL state", e);
                throw e;
            }
        }

        private void restore() {
            try {
                blendFunctionPath.apply(srcRgb, dstRgb, srcAlpha, dstAlpha);
            } finally {
                try {
                    setCapability(GL11.GL_ALPHA_TEST, alphaTestEnabled);
                } finally {
                    setCapability(GL11.GL_BLEND, blendEnabled);
                }
            }
        }
    }

    static BlendFunctionPath selectBlendFunctionPath(boolean openGl14,
                                                     boolean extBlendFuncSeparate) {
        if (openGl14) {
            return BlendFunctionPath.CORE_14;
        }
        if (extBlendFuncSeparate) {
            return BlendFunctionPath.EXTENSION;
        }
        return BlendFunctionPath.LEGACY;
    }

    private static void setCapability(int capability, boolean enabled) {
        if (enabled) {
            GL11.glEnable(capability);
        } else {
            GL11.glDisable(capability);
        }
    }

    enum BlendFunctionPath {
        CORE_14(GL14.GL_BLEND_SRC_RGB, GL14.GL_BLEND_DST_RGB,
                GL14.GL_BLEND_SRC_ALPHA, GL14.GL_BLEND_DST_ALPHA, true) {
            @Override
            void apply(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
                GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            }
        },
        EXTENSION(EXTBlendFuncSeparate.GL_BLEND_SRC_RGB_EXT,
                EXTBlendFuncSeparate.GL_BLEND_DST_RGB_EXT,
                EXTBlendFuncSeparate.GL_BLEND_SRC_ALPHA_EXT,
                EXTBlendFuncSeparate.GL_BLEND_DST_ALPHA_EXT, true) {
            @Override
            void apply(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
                EXTBlendFuncSeparate.glBlendFuncSeparateEXT(
                        srcRgb, dstRgb, srcAlpha, dstAlpha);
            }
        },
        LEGACY(GL11.GL_BLEND_SRC, GL11.GL_BLEND_DST, -1, -1, false) {
            @Override
            void apply(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
                GL11.glBlendFunc(srcRgb, dstRgb);
            }
        };

        final int srcRgbParameter;
        final int dstRgbParameter;
        final int srcAlphaParameter;
        final int dstAlphaParameter;
        final boolean separate;

        BlendFunctionPath(int srcRgbParameter, int dstRgbParameter,
                          int srcAlphaParameter, int dstAlphaParameter, boolean separate) {
            this.srcRgbParameter = srcRgbParameter;
            this.dstRgbParameter = dstRgbParameter;
            this.srcAlphaParameter = srcAlphaParameter;
            this.dstAlphaParameter = dstAlphaParameter;
            this.separate = separate;
        }

        abstract void apply(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha);
    }

    private static final class RenderedString {
        final int textureId;
        final float logicalWidth;
        final float textureWidth;
        final float textureHeight;
        final float xOffset;
        final float baselineOffset;

        RenderedString(int textureId, float logicalWidth, float textureWidth, float textureHeight,
                       float xOffset, float baselineOffset) {
            this.textureId = textureId;
            this.logicalWidth = logicalWidth;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            this.xOffset = xOffset;
            this.baselineOffset = baselineOffset;
        }

        static RenderedString empty(float logicalWidth) {
            return new RenderedString(0, logicalWidth, 0.0F, 0.0F, 0.0F, 0.0F);
        }

        void delete() {
            if (textureId > 0) {
                GL11.glDeleteTextures(textureId);
            }
        }
    }
}
