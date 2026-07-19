package neofontrender.core.font.skia;

import io.github.humbleui.skija.Bitmap;
import io.github.humbleui.skija.BackendRenderTarget;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorSpace;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.Data;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.FontMgr;
import io.github.humbleui.skija.FontStyle;
import io.github.humbleui.skija.FramebufferFormat;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PixelGeometry;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SurfaceOrigin;
import io.github.humbleui.skija.SurfaceProps;
import io.github.humbleui.skija.Typeface;
import io.github.humbleui.skija.paragraph.FontCollection;
import io.github.humbleui.skija.paragraph.Paragraph;
import io.github.humbleui.skija.paragraph.ParagraphBuilder;
import io.github.humbleui.skija.paragraph.ParagraphStyle;
import io.github.humbleui.skija.paragraph.Alignment;
import io.github.humbleui.skija.paragraph.DecorationLineStyle;
import io.github.humbleui.skija.paragraph.DecorationStyle;
import io.github.humbleui.skija.paragraph.TextStyle;
import io.github.humbleui.skija.paragraph.TypefaceFontProvider;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import neofontrender.NeoFontRender;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.core.font.support.FontBrightnessEstimator;
import neofontrender.core.font.support.FontPixelUtils;
import neofontrender.core.font.support.FontRenderPipeline;
import neofontrender.core.font.support.FontRenderTuning;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * First Skija-backed renderer path. It renders shaped paragraph runs into
 * cached Minecraft dynamic textures. A glyph atlas can replace this later
 * without changing the FontRenderer mixin dispatch surface.
 */
public final class SkijaTextRenderer implements TextRenderBackend {

    private static final int DEFAULT_CACHE_SIZE = 512;
    private static final String[] PLATFORM_EMOJI_FONTS = {
            "Segoe UI Emoji",
            "Apple Color Emoji",
            "Noto Color Emoji",
            "Noto Emoji",
            "Droid Sans Fallback"
    };

    private final TextureManager textureManager;
    private final IResourceManager resourceManager;
    private final TypefaceFontProvider fontProvider;
    private final FontCollection fontCollection;
    private final List<Typeface> ownedTypefaces = new ArrayList<>();
    private final String[] fontFamilies;
    private final Map<RenderKey, RenderedText> renderCache = Collections.synchronizedMap(
            new LinkedHashMap<RenderKey, RenderedText>(DEFAULT_CACHE_SIZE, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<RenderKey, RenderedText> eldest) {
                    if (size() <= maxRenderCacheEntries()) {
                        return false;
                    }
                    if (debugRenderStats()) {
                        renderCacheEvictions++;
                    }
                    eldest.getValue().close();
                    return true;
                }
            });
    private final Map<RenderKey, RenderedText> segmentCache = Collections.synchronizedMap(
            new LinkedHashMap<RenderKey, RenderedText>(DEFAULT_CACHE_SIZE, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<RenderKey, RenderedText> eldest) {
                    if (size() <= maxSegmentCacheEntries()) {
                        return false;
                    }
                    if (debugRenderStats()) {
                        segmentCacheEvictions++;
                    }
                    eldest.getValue().close();
                    return true;
                }
            });
    private final Map<MeasureKey, Float> measureCache = Collections.synchronizedMap(
            new LinkedHashMap<MeasureKey, Float>(DEFAULT_CACHE_SIZE, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<MeasureKey, Float> eldest) {
                    if (size() > maxMeasureCacheEntries()) {
                        if (debugRenderStats()) {
                            measureCacheEvictions++;
                        }
                        return true;
                    }
                    return size() > maxMeasureCacheEntries();
                }
            });
    private int nextTextureId = 0;
    private float autoRefBaseline = Float.NaN;
    private IsolatedGpuContext isolatedGpuContext;
    private ColorGlyphDetector colorGlyphDetector;
    private boolean gpuUnavailable;
    private boolean gpuFallbackLogged;
    private boolean gpuSkipLogged;
    private boolean gpuActiveLogged;
    private boolean prewarming;
    private long cpuRasterCount;
    private long gpuRasterCount;
    private long renderCacheHits;
    private long renderCacheMisses;
    private long renderCacheEvictions;
    private long segmentCacheHits;
    private long segmentCacheMisses;
    private long segmentCacheEvictions;
    private long measureCacheHits;
    private long measureCacheMisses;
    private long measureCacheEvictions;
    private volatile String lastRasterPath = "none";
    private volatile String lastGpuFallbackReason = "";
    private volatile String lastRasterStats = "";
    private static volatile String lastDrawState = "";
    private int gpuCompareSamples;

    private static boolean debugRenderStats() {
        return NeofontrenderConfig.debugRenderStats();
    }

    public SkijaTextRenderer(TextureManager textureManager, IResourceManager resourceManager) throws IOException {
        this.textureManager = textureManager;
        this.resourceManager = resourceManager;
        this.fontProvider = new TypefaceFontProvider();
        this.fontFamilies = registerConfiguredFonts();
        this.fontCollection = new FontCollection()
                .setAssetFontManager(fontProvider)
                .setDefaultFontManager(FontMgr.getDefault())
                .setEnableFallback(true);
        this.colorGlyphDetector = ColorGlyphDetector.create(PLATFORM_EMOJI_FONTS);
    }

    public boolean isReady() {
        return fontCollection != null;
    }

    public String[] getFontFamilies() {
        return fontFamilies;
    }

    public FontCollection getFontCollection() {
        return fontCollection;
    }

    public float measure(String text, boolean bold, boolean italic) {
        if (text == null || text.isEmpty()) {
            return 0.0F;
        }
        float scale = currentRasterScale();
        MeasureKey key = MeasureKey.plain(text, bold, italic, scale);
        Float cached = measureCache.get(key);
        boolean stats = debugRenderStats();
        if (cached != null) {
            if (stats) {
                measureCacheHits++;
            }
            return cached;
        }
        if (stats) {
            measureCacheMisses++;
        }
        float measured;
        try (Paragraph paragraph = buildParagraph(text, 0xFFFFFFFF, bold, italic, scale)) {
            paragraph.layout(100000.0F * scale);
            measured = Math.max(paragraph.getMaxIntrinsicWidth(), paragraph.getLongestLine()) / scale;
        }
        measureCache.put(key, measured);
        trimMeasureCache();
        return measured;
    }

    public float measureFormatted(String text, int baseArgb, boolean shadow) {
        if (text == null || text.isEmpty()) {
            return 0.0F;
        }
        Float segmented = tryMeasureFormattedSegments(text, baseArgb, shadow);
        if (segmented != null) {
            return segmented;
        }
        float scale = currentRasterScale();
        MeasureKey key = MeasureKey.formatted(text, shadow, scale,
                FontRenderTuning.currentDrawContext().perspective());
        Float cached = measureCache.get(key);
        boolean stats = debugRenderStats();
        if (cached != null) {
            if (stats) {
                measureCacheHits++;
            }
            return cached;
        }
        if (stats) {
            measureCacheMisses++;
        }
        float measured;
        try (Paragraph paragraph = buildFormattedParagraph(text, baseArgb, shadow, scale)) {
            paragraph.layout(100000.0F * scale);
            measured = Math.max(paragraph.getMaxIntrinsicWidth(), paragraph.getLongestLine()) / scale;
        }
        measureCache.put(key, measured);
        trimMeasureCache();
        return measured;
    }

    public TextRenderResult render(String text, int argb, boolean bold, boolean italic) {
        if (text == null || text.isEmpty()) {
            return RenderedText.EMPTY;
        }
        float scale = currentRasterScale();
        boolean monochrome = useMonochromeText(text);
        RenderKey key = RenderKey.plain(text, argb, bold, italic, scale, texturePathCacheBucket(), !monochrome);
        RenderedText cached = renderCache.get(key);
        boolean stats = debugRenderStats();
        if (cached != null) {
            if (stats) {
                renderCacheHits++;
            }
            cached.touch();
            return monochrome ? new TintedRenderedText(cached, argb) : cached;
        }
        if (stats) {
            renderCacheMisses++;
        }
        try {
            int rasterArgb = monochrome ? 0xFFFFFFFF : argb;
            RenderedText rendered = rasterize(text, rasterArgb, bold, italic, scale, key.hashCode());
            renderCache.put(key, rendered);
            trimRenderCache();
            return monochrome ? new TintedRenderedText(rendered, argb) : rendered;
        } catch (Throwable t) {
            NeoFontRender.LOGGER.error("Failed to render Skija text run '{}'", text, t);
            return RenderedText.EMPTY;
        }
    }

    @Override
    public TextRenderResult renderSegment(String text, int argb, boolean bold, boolean italic) {
        if (text == null || text.isEmpty()) {
            return RenderedText.EMPTY;
        }
        float scale = currentRasterScale();
        boolean monochrome = useMonochromeText(text);
        RenderKey key = RenderKey.plain(text, argb, bold, italic, scale, texturePathCacheBucket(), !monochrome);
        RenderedText cached = segmentCache.get(key);
        boolean stats = debugRenderStats();
        if (cached != null) {
            if (stats) {
                segmentCacheHits++;
            }
            cached.touch();
            return monochrome ? new TintedRenderedText(cached, argb) : cached;
        }
        if (stats) {
            segmentCacheMisses++;
        }
        try {
            int rasterArgb = monochrome ? 0xFFFFFFFF : argb;
            RenderedText rendered = rasterize(text, rasterArgb, bold, italic, scale, key.hashCode());
            segmentCache.put(key, rendered);
            trimSegmentCache();
            return monochrome ? new TintedRenderedText(rendered, argb) : rendered;
        } catch (Throwable t) {
            NeoFontRender.LOGGER.error("Failed to render Skija segment '{}'", text, t);
            return RenderedText.EMPTY;
        }
    }

    public TextRenderResult renderFormatted(String text, int baseArgb, boolean shadow) {
        if (text == null || text.isEmpty()) {
            return RenderedText.EMPTY;
        }
        TextRenderResult segmented = tryRenderFormattedSegments(text, baseArgb, shadow);
        if (segmented != null) {
            return segmented;
        }
        int normalizedArgb = normalizeBaseArgb(baseArgb);
        float scale = currentRasterScale();
        RenderKey key = RenderKey.formatted(text, normalizedArgb, shadow, scale, texturePathCacheBucket(), true);
        RenderedText cached = renderCache.get(key);
        boolean stats = debugRenderStats();
        if (cached != null) {
            if (stats) {
                renderCacheHits++;
            }
            cached.touch();
            return cached;
        }
        if (stats) {
            renderCacheMisses++;
        }
        try {
            RenderedText rendered = rasterizeFormatted(text, normalizedArgb, shadow, scale, key.hashCode());
            renderCache.put(key, rendered);
            trimRenderCache();
            return rendered;
        } catch (Throwable t) {
            NeoFontRender.LOGGER.error("Failed to render formatted Skija text '{}'", text, t);
            return RenderedText.EMPTY;
        }
    }

    /**
     * Render all four vanilla sign lines into one centered paragraph texture. SignRenderer keeps
     * one stable model/view transform while drawing its lines, so this removes three immediate GL
     * submissions without changing the surrounding world transform.
     */
    public TextRenderResult renderSign(String[] lines) {
        if (lines == null || lines.length == 0) {
            return RenderedText.EMPTY;
        }
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i > 0) {
                joined.append('\n');
            }
            if (i < lines.length && lines[i] != null) {
                joined.append(lines[i]);
            }
        }
        String text = joined.toString();
        float scale = currentRasterScale();
        float projectedScale = FontRenderTuning.currentDrawContext().pixelScale();
        if (projectedScale >= NeofontrenderConfig.signTextNearThreshold()) {
            // Only close signs receive the expensive high-resolution bucket. Applying this to all
            // signs would multiply the cache/VRAM cost of the large sign walls this path targets.
            float requested = projectedScale * NeofontrenderConfig.signTextNearSupersample();
            float nearScale = Math.min(NeofontrenderConfig.signTextNearMaxRasterScale(), requested);
            scale = Math.max(scale, Math.round(nearScale * 2.0F) * 0.5F);
        }
        RenderKey key = RenderKey.formatted("\u0001sign\n" + text, 0, false, scale,
                texturePathCacheBucket(), true);
        RenderedText cached = renderCache.get(key);
        if (cached != null) {
            if (debugRenderStats()) {
                renderCacheHits++;
            }
            cached.touch();
            return cached;
        }
        if (debugRenderStats()) {
            renderCacheMisses++;
        }
        try (Paragraph paragraph = buildFormattedParagraph(text, 0, false, scale, Alignment.CENTER)) {
            float measuredWidth = 90.0F;
            int border = computeBorder(scale);
            int width = Math.max(1, (int) Math.ceil((measuredWidth + border * 2.0F) * scale));
            // Paragraph alignment must use only the 90px content area. The surface is wider to
            // carry transparent glyph padding, and paintX already moves the paragraph past that
            // padding. Including the border in layout width applied it twice and made centered
            // sign text drift right whenever adaptive raster LOD changed the border size.
            paragraph.layout(measuredWidth * scale);
            RenderedText rendered = rasterizeParagraph(paragraph, key.hashCode(), measuredWidth, width, scale, border)
                    // Keep the world-space quad width independent of raster-scale rounding. Without
                    // this, adaptive LOD buckets change ceil(width * scale) and shift the sign text.
                    .withFixedDrawWidth(measuredWidth + border * 2.0F);
            renderCache.put(key, rendered);
            trimRenderCache();
            return rendered;
        } catch (Throwable t) {
            NeoFontRender.LOGGER.error("Failed to render batched sign text", t);
            return RenderedText.EMPTY;
        }
    }

    public void prewarmBasicLatin() {
        prewarming = true;
        try {
            FontBrightnessEstimator.reset();
            int fontRes = Math.round(NeofontrenderConfig.fontSize() * NeofontrenderConfig.fontOversample());
            for (int cp = 32; cp <= 126; cp++) {
                String text = new String(Character.toChars(cp));
                measure(text, false, false);
                if (cp != ' ') {
                    TextRenderResult result = render(text, 0xFFFFFFFF, false, false);
                    RenderedText rt = result instanceof TintedRenderedText
                            ? ((TintedRenderedText) result).rendered
                            : (RenderedText) result;
                    if (rt != null && rt.getCpuTexture() != null && (cp == '1' || cp == '/' || cp == 'I')) {
                        int[] pixels = rt.getCpuTexture().getTextureData();
                        FontBrightnessEstimator.feedSample(pixels, rt.width, rt.height, fontRes);
                    }
                }
            }
            for (int cp = 160; cp <= 255; cp++) {
                String text = new String(Character.toChars(cp));
                measure(text, false, false);
                if (cp != 160) {
                    render(text, 0xFFFFFFFF, false, false);
                }
            }
        } finally {
            prewarming = false;
        }
    }

    private static int computeBorder(float oversample) {
        int border = Math.max(4, (int) (oversample * 8.0f / 16.0f) * 2);
        border += border % 2;
        return Math.max(border, 4);
    }

    private RenderedText rasterize(String text, int argb, boolean bold, boolean italic, float oversample, int cacheHash) throws IOException {
        // Measure and rasterize with the same Paragraph. Calling measure() here used to build
        // and shape a temporary Paragraph, then this method built a second one for painting.
        try (Paragraph paragraph = buildParagraph(text, argb, bold, italic, oversample)) {
            paragraph.layout(100000.0F * oversample);
            float measuredWidth = Math.max(1.0F,
                    Math.max(paragraph.getMaxIntrinsicWidth(), paragraph.getLongestLine()) / oversample);
            int border = computeBorder(oversample);
            int width = Math.max(1, (int) Math.ceil((measuredWidth + border * 2.0F) * oversample));
            paragraph.layout(width);
            return rasterizeParagraph(paragraph, cacheHash, measuredWidth, width, oversample, border);
        }
    }

    private RenderedText rasterizeFormatted(String text, int argb, boolean shadow, float oversample, int cacheHash) throws IOException {
        // Formatted spans pay the same shaping cost, so keep their measurement and paint pass on
        // one Paragraph as well. This removes duplicate fallback/style processing on cache misses.
        try (Paragraph paragraph = buildFormattedParagraph(text, argb, shadow, oversample)) {
            paragraph.layout(100000.0F * oversample);
            float measuredWidth = Math.max(1.0F,
                    Math.max(paragraph.getMaxIntrinsicWidth(), paragraph.getLongestLine()) / oversample);
            int border = computeBorder(oversample);
            int width = Math.max(1, (int) Math.ceil((measuredWidth + border * 2.0F) * oversample));
            paragraph.layout(width);
            return rasterizeParagraph(paragraph, cacheHash, measuredWidth, width, oversample, border);
        }
    }

    private RenderedText rasterizeParagraph(Paragraph paragraph, int cacheHash, float measuredWidth, int width,
                                           float oversample, int border) throws IOException {
        if (shouldUseGpuOffscreen()) {
            try {
                return rasterizeParagraphGpu(paragraph, cacheHash, measuredWidth, width, oversample, border);
            } catch (Throwable t) {
                gpuUnavailable = true;
                lastGpuFallbackReason = "isolated failed: " + t.getClass().getSimpleName();
                if (!gpuFallbackLogged) {
                    gpuFallbackLogged = true;
                    NeoFontRender.LOGGER.warn("Isolated Skia GPU offscreen rendering failed; falling back to CPU rasterization", t);
                }
            }
        }
        cpuRasterCount++;
        lastRasterPath = "cpu";
        int height;
        float verticalOffset;

        height = paragraphTextureHeight(paragraph, oversample, border);
        float paintX = border * oversample;
        float paintY = border * oversample / 2.0f;
        float baselineInTexture = (paintY + paragraph.getAlphabeticBaseline()) / oversample;
        if (NeofontrenderConfig.fontAutoBaseline()) {
            float refBaseline = Float.isNaN(autoRefBaseline) ? computeAutoRefBaseline() : autoRefBaseline;
            verticalOffset = refBaseline + NeofontrenderConfig.fontBaselineShift() - baselineInTexture;
        } else {
            verticalOffset = NeofontrenderConfig.fontReferenceBaseline() + NeofontrenderConfig.fontBaselineShift() - baselineInTexture;
        }
        float horizontalOffset = -paintX / oversample;

        ImageInfo imageInfo = ImageInfo.makeN32Premul(width, height);
        try (Surface surface = Surface.makeRaster(imageInfo, imageInfo.getMinRowBytes(), skiaSurfaceProps())) {
            Canvas canvas = surface.getCanvas();
            canvas.clear(0x00000000);
            paragraph.paint(canvas, paintX, paintY);

            Bitmap bitmap = new Bitmap();
            bitmap.allocN32Pixels(width, height);
            surface.readPixels(bitmap, 0, 0);

            byte[] rawBytes = bitmap.readPixels();
            bitmap.close();

            int[] pixels = new int[width * height];
            ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(pixels);

            boolean premultiplied = NeofontrenderConfig.enablePremultipliedAlpha();
            if (!premultiplied) {
                // Convert Skia premultiplied output to straight alpha
                for (int i = 0; i < pixels.length; i++) {
                    int px = pixels[i];
                    int a = (px >>> 24);
                    if (a > 0 && a < 255) {
                        int r = Math.min(255, ((px >> 16) & 0xFF) * 255 / a);
                        int g = Math.min(255, ((px >> 8) & 0xFF) * 255 / a);
                        int b = Math.min(255, (px & 0xFF) * 255 / a);
                        pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                }
            }
            if (NeofontrenderConfig.textureEdgeBleed()) {
                FontPixelUtils.normalizeTransparentRgb(pixels, width, height);
            }

            float textureScale = FontRenderTuning.textureScale(oversample);
            int uploadWidth = width;
            int uploadHeight = height;
            if (textureScale > 1.0F) {
                int scale = Math.round(textureScale);
                pixels = FontPixelUtils.scaleNearest(pixels, width, height, scale);
                uploadWidth = width * scale;
                uploadHeight = height * scale;
            }
            if (debugRenderStats()) {
                lastRasterStats = "cpu " + RasterStats.fromArgb(pixels, uploadWidth, uploadHeight);
            } else {
                lastRasterStats = "";
            }
            float actualRasterScale = oversample * textureScale;

            DynamicTexture texture = new DynamicTexture(uploadWidth, uploadHeight);
            int[] target = texture.getTextureData();
            System.arraycopy(pixels, 0, target, 0, Math.min(pixels.length, target.length));
            texture.updateDynamicTexture();
            FontRenderTuning.applyFontTextureFilter(texture, actualRasterScale);

            ResourceLocation location = new ResourceLocation("neofontrender",
            "skia/" + Integer.toHexString(cacheHash) + "_" + nextTextureId++);
            textureManager.loadTexture(location, texture);
            FontRenderTuning.applyFontTextureFilter(texture, actualRasterScale);
            return new RenderedText(location, texture, measuredWidth,
                    uploadWidth, uploadHeight,
                    uploadWidth / actualRasterScale, uploadHeight / actualRasterScale,
                    actualRasterScale, horizontalOffset, verticalOffset, false);
        }
    }

    private RenderedText rasterizeParagraphGpu(Paragraph paragraph, int cacheHash, float measuredWidth, int width,
                                               float oversample, int border) throws IOException {
        gpuRasterCount++;
        lastRasterPath = "gpu-isolated";
        if (!gpuActiveLogged) {
            gpuActiveLogged = true;
            NeoFontRender.LOGGER.info("Isolated Skia GPU offscreen rendering is active");
        }

        int height = paragraphTextureHeight(paragraph, oversample, border);
        float paintX = border * oversample;
        float paintY = border * oversample / 2.0f;
        float baselineInTexture = (paintY + paragraph.getAlphabeticBaseline()) / oversample;
        float verticalOffset;
        if (NeofontrenderConfig.fontAutoBaseline()) {
            float refBaseline = Float.isNaN(autoRefBaseline) ? computeAutoRefBaseline() : autoRefBaseline;
            verticalOffset = refBaseline + NeofontrenderConfig.fontBaselineShift() - baselineInTexture;
        } else {
            verticalOffset = NeofontrenderConfig.fontReferenceBaseline() + NeofontrenderConfig.fontBaselineShift() - baselineInTexture;
        }
        float horizontalOffset = -paintX / oversample;

        float textureScale = FontRenderTuning.textureScale(oversample);
        int scale = Math.max(1, Math.round(textureScale));
        int uploadWidth = width * scale;
        int uploadHeight = height * scale;
        float actualRasterScale = oversample * scale;

        boolean perspective = FontRenderTuning.currentDrawContext().perspective();
        boolean submitViaCpuTexture = NeofontrenderConfig.skiaGpuSubmitViaCpuTexture();
        if (submitViaCpuTexture) {
            lastRasterPath = "gpu-isolated-cpu-submit";
            boolean statsEnabled = debugRenderStats();
            GpuReadback readback = getOrCreateIsolatedGpuContext()
                    .renderReadback(paragraph, uploadWidth, uploadHeight, scale, paintX, paintY, statsEnabled);
            int[] pixels = readback.argbPixels;
            if (NeofontrenderConfig.textureEdgeBleed()) {
                FontPixelUtils.normalizeTransparentRgb(pixels, uploadWidth, uploadHeight);
            }

            DynamicTexture texture = new DynamicTexture(uploadWidth, uploadHeight);
            int[] target = texture.getTextureData();
            System.arraycopy(pixels, 0, target, 0, Math.min(pixels.length, target.length));
            texture.updateDynamicTexture();

            if (statsEnabled) {
                String stats = "gpu-cpu-submit " + readback.stats;
                if (gpuCompareSamples < 8) {
                    gpuCompareSamples++;
                    stats += " ref " + rasterizeReferenceStats(paragraph, uploadWidth, uploadHeight, scale, paintX, paintY);
                }
                lastRasterStats = stats;
            } else {
                lastRasterStats = "";
            }

            ResourceLocation location = new ResourceLocation("neofontrender",
                    "skia_gpu_cpu_submit/" + Integer.toHexString(cacheHash) + "_" + nextTextureId++);
            FontRenderTuning.applyFontTextureFilter(texture, actualRasterScale);
            textureManager.loadTexture(location, texture);
            FontRenderTuning.applyFontTextureFilter(texture, actualRasterScale);
            return new RenderedText(location, texture, measuredWidth,
                    uploadWidth, uploadHeight,
                    uploadWidth / actualRasterScale, uploadHeight / actualRasterScale,
                    actualRasterScale, horizontalOffset, verticalOffset, false);
        }

        boolean statsEnabled = debugRenderStats();
        // Minecraft uses a nearest-sampled font atlas for both GUI and world text. Mip/linear
        // sampling of Skia coverage masks creates a soft dark base around sign glyphs just as it
        // creates a gray halo around GUI glyphs and shadows, so shared textures stay at level 0.
        boolean generateMipmaps = false;
        boolean crispSampling = true;
        GpuTextTexture texture = getOrCreateIsolatedGpuContext()
                .render(paragraph, uploadWidth, uploadHeight, scale, paintX, paintY,
                        statsEnabled, generateMipmaps, crispSampling);
        if (statsEnabled) {
            String stats = "gpu " + texture.debugStats();
            if (gpuCompareSamples < 8) {
                gpuCompareSamples++;
                stats += " ref " + rasterizeReferenceStats(paragraph, uploadWidth, uploadHeight, scale, paintX, paintY);
            }
            lastRasterStats = stats;
        } else {
            lastRasterStats = "";
        }
        ResourceLocation location = new ResourceLocation("neofontrender",
                "skia_gpu_iso/" + Integer.toHexString(cacheHash) + "_" + nextTextureId++);
        textureManager.loadTexture(location, texture);
        if (crispSampling) {
            // TextureManager registration may apply the global interpolation setting. Shared GUI
            // text must retain vanilla-style nearest sampling; linear filtering merges the 1px MC
            // shadow with the antialiased foreground into a blurred outline.
            texture.setBlurMipmap(false, false);
        } else {
            FontRenderTuning.applyFontTextureFilter(texture, actualRasterScale, texture.hasMipmaps());
        }
        return new RenderedText(location, texture, measuredWidth,
                uploadWidth, uploadHeight,
                uploadWidth / actualRasterScale, uploadHeight / actualRasterScale,
                actualRasterScale, horizontalOffset, verticalOffset, true);
    }

    private int texturePathCacheBucket() {
        if (!NeofontrenderConfig.skiaGpuOffscreen()
                || NeofontrenderConfig.skiaGpuSubmitViaCpuTexture()
                || !NeofontrenderConfig.enablePremultipliedAlpha()
                || gpuUnavailable
                || prewarming) {
            return 0;
        }
        // GUI and perspective shared textures use different mip/filter policies and must never
        // alias in the cache even when their text and raster scale happen to be identical.
        return FontRenderTuning.currentDrawContext().perspective() ? 2 : 1;
    }

    /**
     * Whether a plain (single-color) text run may use the monochrome fast path:
     * a white glyph texture reused across all colors and tinted at draw time.
     * Requires the config flag, premultiplied alpha (the tint math is only
     * correct under premultiplied blending), and a run with no color glyphs.
     */
    private boolean useMonochromeText(String text) {
        if (!NeofontrenderConfig.skiaMonochromeText()) {
            return false;
        }
        if (!NeofontrenderConfig.enablePremultipliedAlpha()) {
            return false;
        }
        if (colorGlyphDetector == null) {
            return true;
        }
        return !colorGlyphDetector.isColorRun(text);
    }

    private boolean shouldUseGpuOffscreen() {
        if (!NeofontrenderConfig.skiaGpuOffscreen() || gpuUnavailable || prewarming) {
            return false;
        }
        if (!NeofontrenderConfig.enablePremultipliedAlpha()) {
            return skipGpuOffscreen("rendering.premultipliedAlpha is false");
        }
        return true;
    }

    private boolean skipGpuOffscreen(String reason) {
        lastGpuFallbackReason = reason;
        if (!gpuSkipLogged) {
            gpuSkipLogged = true;
            NeoFontRender.LOGGER.warn("Isolated Skia GPU offscreen rendering is enabled but cannot be used: {}. Using CPU rasterization.", reason);
        }
        return false;
    }

    private RasterStats rasterizeReferenceStats(Paragraph paragraph, int width, int height, int textureScale,
                                                float paintX, float paintY) {
        ImageInfo imageInfo = ImageInfo.makeN32Premul(width, height);
        try (Surface surface = Surface.makeRaster(imageInfo, imageInfo.getMinRowBytes(), skiaSurfaceProps())) {
            Canvas canvas = surface.getCanvas();
            canvas.clear(0x00000000);
            if (textureScale != 1) {
                int save = canvas.save();
                canvas.scale(textureScale, textureScale);
                paragraph.paint(canvas, paintX, paintY);
                canvas.restoreToCount(save);
            } else {
                paragraph.paint(canvas, paintX, paintY);
            }

            Bitmap bitmap = new Bitmap();
            bitmap.allocN32Pixels(width, height);
            surface.readPixels(bitmap, 0, 0);
            byte[] rawBytes = bitmap.readPixels();
            bitmap.close();
            int[] pixels = new int[width * height];
            ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(pixels);
            return RasterStats.fromArgb(pixels, width, height);
        } catch (Throwable t) {
            return RasterStats.error(t);
        }
    }

    private static int paragraphTextureHeight(Paragraph paragraph, float oversample, int border) {
        return Math.max(1, (int) Math.ceil(paragraph.getHeight() + border * 2.0F * oversample));
    }

    private IsolatedGpuContext getOrCreateIsolatedGpuContext() throws IOException {
        if (isolatedGpuContext == null) {
            isolatedGpuContext = IsolatedGpuContext.create();
        }
        return isolatedGpuContext;
    }

    public DebugState debugState() {
        synchronized (renderCache) {
            synchronized (segmentCache) {
                synchronized (measureCache) {
                    return new DebugState(
                            NeofontrenderConfig.skiaGpuOffscreen(),
                            isolatedGpuContext != null,
                            gpuUnavailable,
                            lastRasterPath,
                            lastGpuFallbackReason,
                            lastRasterStats,
                            lastDrawState,
                            cpuRasterCount,
                            gpuRasterCount,
                            renderCache.size(),
                            segmentCache.size(),
                            measureCache.size(),
                            maxRenderCacheEntries(),
                            maxSegmentCacheEntries(),
                            maxMeasureCacheEntries(),
                            renderCacheHits,
                            renderCacheMisses,
                            renderCacheEvictions,
                            segmentCacheHits,
                            segmentCacheMisses,
                            segmentCacheEvictions,
                            measureCacheHits,
                            measureCacheMisses,
                            measureCacheEvictions);
                }
            }
        }
    }

    public String exportGpuDiagnostics(File outputDir, String text) throws IOException {
        if (text == null || text.isEmpty()) {
            text = "Hello World 你好世界";
        }
        if (!outputDir.isDirectory() && !outputDir.mkdirs()) {
            throw new IOException("Cannot create export directory: " + outputDir);
        }

        float oversample = currentRasterScale();
        float measuredWidth = Math.max(1.0F, measure(text, false, false));
        int border = computeBorder(oversample);
        int width = Math.max(1, (int) Math.ceil((measuredWidth + border * 2.0F) * oversample));
        try (Paragraph paragraph = buildParagraph(text, 0xFFFFFFFF, false, false, oversample)) {
            paragraph.layout(width);
            int height = paragraphTextureHeight(paragraph, oversample, border);
            float paintX = border * oversample;
            float paintY = border * oversample / 2.0F;
            float textureScale = FontRenderTuning.textureScale(oversample);
            int scale = Math.max(1, Math.round(textureScale));
            int uploadWidth = width * scale;
            int uploadHeight = height * scale;

            int[] cpuPixels = renderReferencePixels(paragraph, uploadWidth, uploadHeight, scale, paintX, paintY);
            ImageIO.write(toImage(cpuPixels, uploadWidth, uploadHeight), "PNG",
                    new File(outputDir, "cpu_reference_texture.png"));
            RasterStats cpuStats = RasterStats.fromArgb(cpuPixels, uploadWidth, uploadHeight);

            GpuReadback gpu = getOrCreateIsolatedGpuContext()
                    .renderReadback(paragraph, uploadWidth, uploadHeight, scale, paintX, paintY, true);
            ImageIO.write(toImage(gpu.argbPixels, uploadWidth, uploadHeight), "PNG",
                    new File(outputDir, "gpu_isolated_readback.png"));

            return "textureSize=" + uploadWidth + "x" + uploadHeight
                    + " oversample=" + oversample
                    + " textureScale=" + scale
                    + "\n  cpu_reference_texture.png: " + cpuStats
                    + "\n  gpu_isolated_readback.png: " + gpu.stats;
        }
    }

    private int[] renderReferencePixels(Paragraph paragraph, int width, int height, int textureScale,
                                        float paintX, float paintY) {
        ImageInfo imageInfo = ImageInfo.makeN32Premul(width, height);
        try (Surface surface = Surface.makeRaster(imageInfo, imageInfo.getMinRowBytes(), skiaSurfaceProps())) {
            Canvas canvas = surface.getCanvas();
            canvas.clear(0x00000000);
            if (textureScale != 1) {
                int save = canvas.save();
                canvas.scale(textureScale, textureScale);
                paragraph.paint(canvas, paintX, paintY);
                canvas.restoreToCount(save);
            } else {
                paragraph.paint(canvas, paintX, paintY);
            }

            Bitmap bitmap = new Bitmap();
            bitmap.allocN32Pixels(width, height);
            surface.readPixels(bitmap, 0, 0);
            byte[] rawBytes = bitmap.readPixels();
            bitmap.close();
            int[] pixels = new int[width * height];
            ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(pixels);
            return pixels;
        }
    }

    private static BufferedImage toImage(int[] argbPixels, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, argbPixels, 0, width);
        return image;
    }

    private float computeAutoRefBaseline() {
        try (Paragraph para = buildParagraph("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789", 0xFFFFFFFF, false, false, 1.0f)) {
            para.layout(10000);
            float alphabeticBaseline = para.getAlphabeticBaseline();
            float mcBaseline = 7.0f;
            autoRefBaseline = mcBaseline + (alphabeticBaseline - mcBaseline) / 2.0f;
            NeoFontRender.LOGGER.debug("Auto reference baseline computed: {}", autoRefBaseline);
            return autoRefBaseline;
        }
    }

    private float currentRasterScale() {
        return FontRenderTuning.rasterScale(NeofontrenderConfig.fontOversample());
    }

    private Paragraph buildParagraph(String text, int argb, boolean bold, boolean italic, float scale) {
        ParagraphBuilder builder = new ParagraphBuilder(new ParagraphStyle(), fontCollection);
        appendStyledText(builder, text, argb, bold, italic, false, false, scale);
        return builder.build();
    }

    private Paragraph buildFormattedParagraph(String text, int baseArgb, boolean shadow, float scale) {
        return buildFormattedParagraph(text, baseArgb, shadow, scale, null);
    }

    private Paragraph buildFormattedParagraph(String text, int baseArgb, boolean shadow, float scale,
                                               Alignment alignment) {
        ParagraphStyle paragraphStyle = new ParagraphStyle();
        if (alignment != null) {
            paragraphStyle.setAlignment(alignment);
        }
        ParagraphBuilder builder = new ParagraphBuilder(paragraphStyle, fontCollection);
        for (FormattedRun run : splitFormattedRuns(text, baseArgb, shadow)) {
            appendStyledText(builder, run.text, run.argb, run.bold, run.italic, run.underline, run.strikethrough, scale);
        }

        return builder.build();
    }

    private Paragraph buildStyledParagraph(String text, int argb, boolean bold, boolean italic,
                                           boolean underline, boolean strikethrough, float scale) {
        ParagraphBuilder builder = new ParagraphBuilder(new ParagraphStyle(), fontCollection);
        appendStyledText(builder, text, argb, bold, italic, underline, strikethrough, scale);
        return builder.build();
    }

    private Float tryMeasureFormattedSegments(String text, int baseArgb, boolean shadow) {
        if (!NeofontrenderConfig.skiaSegmentCache()
                || FontRenderTuning.currentDrawContext().perspective()) {
            return null;
        }
        List<FormattedRun> runs = splitFormattedRuns(text, baseArgb, shadow);
        if (runs.isEmpty()) {
            return 0.0F;
        }
        boolean segmentedAny = false;
        float width = 0.0F;
        for (FormattedRun run : runs) {
            List<String> segments = SkiaTextSegmenter.segment(run.text);
            if (segments != null) {
                segmentedAny = true;
                for (String segment : segments) {
                    width += measure(segment, run.bold, run.italic);
                }
                continue;
            }
            width += measure(run.text, run.bold, run.italic);
        }
        return segmentedAny ? width : null;
    }

    private TextRenderResult tryRenderFormattedSegments(String text, int baseArgb, boolean shadow) {
        if (!NeofontrenderConfig.skiaSegmentCache()
                || FontRenderTuning.currentDrawContext().perspective()) {
            return null;
        }
        List<FormattedRun> runs = splitFormattedRuns(text, baseArgb, shadow);
        if (runs.isEmpty()) {
            return RenderedText.EMPTY;
        }
        ArrayList<CompositePiece> pieces = new ArrayList<>();
        boolean segmentedAny = false;
        float advance = 0.0F;

        for (FormattedRun run : runs) {
            List<String> segments = SkiaTextSegmenter.segment(run.text);
            if (segments != null) {
                segmentedAny = true;
                for (String segment : segments) {
                    TextRenderResult rendered = renderStyledSegment(segment, run.argb, run.bold, run.italic,
                            run.underline, run.strikethrough);
                    float pieceAdvance = measure(segment, run.bold, run.italic);
                    pieces.add(new CompositePiece(rendered, advance));
                    advance += pieceAdvance;
                }
                continue;
            }

            TextRenderResult rendered = renderStyledSegment(run.text, run.argb, run.bold, run.italic,
                    run.underline, run.strikethrough);
            pieces.add(new CompositePiece(rendered, advance));
            advance += measure(run.text, run.bold, run.italic);
        }

        return segmentedAny ? new CompositeRenderedText(advance, pieces) : null;
    }

    private TextRenderResult renderStyledSegment(String text, int argb, boolean bold, boolean italic,
                                                 boolean underline, boolean strikethrough) {
        if (!underline && !strikethrough) {
            return renderSegment(text, argb, bold, italic);
        }
        if (text == null || text.isEmpty()) {
            return RenderedText.EMPTY;
        }
        float scale = currentRasterScale();
        RenderKey key = RenderKey.decoratedSegment(text, argb, bold, italic, underline, strikethrough, scale,
                texturePathCacheBucket(), true);
        RenderedText cached = segmentCache.get(key);
        boolean stats = debugRenderStats();
        if (cached != null) {
            if (stats) {
                segmentCacheHits++;
            }
            cached.touch();
            return cached;
        }
        if (stats) {
            segmentCacheMisses++;
        }
        try {
            RenderedText rendered = rasterizeStyled(text, argb, bold, italic, underline, strikethrough, scale, key.hashCode());
            segmentCache.put(key, rendered);
            trimSegmentCache();
            return rendered;
        } catch (Throwable t) {
            NeoFontRender.LOGGER.error("Failed to render formatted Skija segment '{}'", text, t);
            return RenderedText.EMPTY;
        }
    }

    private RenderedText rasterizeStyled(String text, int argb, boolean bold, boolean italic,
                                         boolean underline, boolean strikethrough, float oversample,
                                         int cacheHash) throws IOException {
        float measuredWidth = Math.max(1.0F, measure(text, bold, italic));
        int border = computeBorder(oversample);
        int width = Math.max(1, (int) Math.ceil((measuredWidth + border * 2.0F) * oversample));
        try (Paragraph paragraph = buildStyledParagraph(text, argb, bold, italic, underline, strikethrough, oversample)) {
            paragraph.layout(width);
            return rasterizeParagraph(paragraph, cacheHash, measuredWidth, width, oversample, border);
        }
    }

    private List<FormattedRun> splitFormattedRuns(String text, int baseArgb, boolean shadow) {
        ArrayList<FormattedRun> runs = new ArrayList<>();
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean strikethrough = false;
        int color = shadow ? shadowBaseArgb(baseArgb) : normalizeBaseArgb(baseArgb);
        int runStart = 0;

        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != 167 || i + 1 >= text.length()) {
                continue;
            }
            if (i > runStart) {
                runs.add(new FormattedRun(text.substring(runStart, i), color, bold, italic, underline, strikethrough));
            }

            int style = "0123456789abcdefklmnor".indexOf(Character.toLowerCase(text.charAt(i + 1)));
            if (style >= 0 && style < 16) {
                bold = false;
                italic = false;
                underline = false;
                strikethrough = false;
                color = colorFromCode(style, shadow, baseArgb);
            } else if (style == 17) {
                bold = true;
            } else if (style == 18) {
                strikethrough = true;
            } else if (style == 19) {
                underline = true;
            } else if (style == 20) {
                italic = true;
            } else if (style == 21) {
                bold = false;
                italic = false;
                underline = false;
                strikethrough = false;
                color = shadow ? shadowBaseArgb(baseArgb) : normalizeBaseArgb(baseArgb);
            }

            i++;
            runStart = i + 1;
        }

        if (runStart < text.length()) {
            runs.add(new FormattedRun(text.substring(runStart), color, bold, italic, underline, strikethrough));
        }
        return runs;
    }

    private void appendStyledText(ParagraphBuilder builder, String text, int argb, boolean bold, boolean italic,
                                  boolean underline, boolean strikethrough, float scale) {
        if (text.isEmpty()) {
            return;
        }
        try (TextStyle textStyle = makeTextStyle(argb, bold, italic, scale);
             Paint foreground = makeForegroundPaint(argb)) {
            textStyle.setForeground(foreground);
            if (underline || strikethrough) {
                textStyle.setDecorationStyle(new DecorationStyle(
                        underline, false, strikethrough, false, opaqueRgb(argb),
                        DecorationLineStyle.SOLID, 1.0F));
            }
            builder.pushStyle(textStyle);
            builder.addText(text);
            builder.popStyle();
        }
    }

    private TextStyle makeTextStyle(int argb, boolean bold, boolean italic, float scale) {
        int configuredStyle = NeofontrenderConfig.fontStyle();
        boolean effectiveBold = bold || (configuredStyle & 1) != 0;
        boolean effectiveItalic = italic || (configuredStyle & 2) != 0;
        FontStyle style = effectiveBold && effectiveItalic ? FontStyle.BOLD_ITALIC
                : effectiveBold ? FontStyle.BOLD
                : effectiveItalic ? FontStyle.ITALIC
                : FontStyle.NORMAL;
        return new TextStyle()
                .setColor(opaqueRgb(argb))
                .setFontSize(NeofontrenderConfig.fontSize() * scale)
                .setFontStyle(style)
                .setFontFamilies(fontFamilies)
                .setHeight(1.0F);
    }

    private static Paint makeForegroundPaint(int argb) {
        return new Paint()
                .setColor(opaqueRgb(argb))
                .setAntiAlias(!"off".equals(NeofontrenderConfig.fontAntialiasMode()));
    }

    private static SurfaceProps skiaSurfaceProps() {
        PixelGeometry geometry = skiaPixelGeometry(NeofontrenderConfig.fontAntialiasMode());
        if (geometry == PixelGeometry.UNKNOWN && NeofontrenderConfig.fontLcdSubpixel()) {
            geometry = PixelGeometry.RGB_H;
        }
        return new SurfaceProps(false, geometry);
    }

    private static PixelGeometry skiaPixelGeometry(String mode) {
        if (mode == null) {
            return PixelGeometry.UNKNOWN;
        }
        switch (mode) {
            case "lcd_hrgb":
                return PixelGeometry.RGB_H;
            case "lcd_hbgr":
                return PixelGeometry.BGR_H;
            case "lcd_vrgb":
                return PixelGeometry.RGB_V;
            case "lcd_vbgr":
                return PixelGeometry.BGR_V;
            default:
                return PixelGeometry.UNKNOWN;
        }
    }

    private static int normalizeBaseArgb(int argb) {
        return (argb & 0xFC000000) == 0 ? argb | 0xFF000000 : argb;
    }

    private static int opaqueRgb(int argb) {
        return normalizeBaseArgb(argb) | 0xFF000000;
    }

    private static int shadowBaseArgb(int argb) {
        int color = normalizeBaseArgb(argb);
        return (color & 0xFCFCFC) >> 2 | color & 0xFF000000;
    }

    private static int colorFromCode(int index, boolean shadow, int baseArgb) {
        int i = (index >> 3 & 1) * 85;
        int r = (index >> 2 & 1) * 170 + i;
        int g = (index >> 1 & 1) * 170 + i;
        int b = (index & 1) * 170 + i;
        if (index == 6) {
            r += 85;
        }
        if (shadow) {
            r >>= 2;
            g >>= 2;
            b >>= 2;
        }
        int a = normalizeBaseArgb(baseArgb) & 0xFF000000;
        return a | r << 16 | g << 8 | b;
    }

    private void trimRenderCache() {
        synchronized (renderCache) {
            int max = maxRenderCacheEntries();
            Iterator<Map.Entry<RenderKey, RenderedText>> iterator = renderCache.entrySet().iterator();
            while (renderCache.size() > max && iterator.hasNext()) {
                Map.Entry<RenderKey, RenderedText> eldest = iterator.next();
                eldest.getValue().close();
                iterator.remove();
                if (debugRenderStats()) {
                    renderCacheEvictions++;
                }
            }

            long ttlMillis = renderCacheTtlMillis();
            if (ttlMillis <= 0L) {
                return;
            }
            int min = minRenderCacheEntries(max);
            long now = System.currentTimeMillis();
            iterator = renderCache.entrySet().iterator();
            while (renderCache.size() > min && iterator.hasNext()) {
                Map.Entry<RenderKey, RenderedText> eldest = iterator.next();
                if (!eldest.getValue().isExpired(now, ttlMillis)) {
                    break;
                }
                eldest.getValue().close();
                iterator.remove();
                if (debugRenderStats()) {
                    renderCacheEvictions++;
                }
            }
        }
    }

    private void trimSegmentCache() {
        synchronized (segmentCache) {
            int max = maxSegmentCacheEntries();
            Iterator<Map.Entry<RenderKey, RenderedText>> iterator = segmentCache.entrySet().iterator();
            while (segmentCache.size() > max && iterator.hasNext()) {
                Map.Entry<RenderKey, RenderedText> eldest = iterator.next();
                eldest.getValue().close();
                iterator.remove();
                if (debugRenderStats()) {
                    segmentCacheEvictions++;
                }
            }

            long ttlMillis = segmentCacheTtlMillis();
            if (ttlMillis <= 0L) {
                return;
            }
            int min = minSegmentCacheEntries(max);
            long now = System.currentTimeMillis();
            iterator = segmentCache.entrySet().iterator();
            while (segmentCache.size() > min && iterator.hasNext()) {
                Map.Entry<RenderKey, RenderedText> eldest = iterator.next();
                if (!eldest.getValue().isExpired(now, ttlMillis)) {
                    break;
                }
                eldest.getValue().close();
                iterator.remove();
                if (debugRenderStats()) {
                    segmentCacheEvictions++;
                }
            }
        }
    }

    private void trimMeasureCache() {
        synchronized (measureCache) {
            int max = maxMeasureCacheEntries();
            Iterator<MeasureKey> iterator = measureCache.keySet().iterator();
            while (measureCache.size() > max && iterator.hasNext()) {
                iterator.next();
                iterator.remove();
                if (debugRenderStats()) {
                    measureCacheEvictions++;
                }
            }
        }
    }

    private static int maxRenderCacheEntries() {
        return Math.max(1, NeofontrenderConfig.skiaTextCacheMaxEntries());
    }

    private static int maxSegmentCacheEntries() {
        return Math.max(1, NeofontrenderConfig.skiaSegmentTextureCacheMaxEntries());
    }

    private static int minRenderCacheEntries(int max) {
        return Math.max(0, Math.min(max, NeofontrenderConfig.skiaTextCacheMinEntries()));
    }

    private static int minSegmentCacheEntries(int max) {
        return Math.max(0, Math.min(max, NeofontrenderConfig.skiaSegmentTextureCacheMinEntries()));
    }

    private static int maxMeasureCacheEntries() {
        return Math.max(1, NeofontrenderConfig.skiaMeasureCacheMaxEntries());
    }

    private static long renderCacheTtlMillis() {
        return (long) (NeofontrenderConfig.skiaTextCacheTtlSeconds() * 1000.0F);
    }

    private static long segmentCacheTtlMillis() {
        return (long) (NeofontrenderConfig.skiaSegmentTextureCacheTtlSeconds() * 1000.0F);
    }

    private String[] registerConfiguredFonts() throws IOException {
        List<String> families = new ArrayList<>();
        for (String name : NeofontrenderConfig.fontFamily()) {
            List<String> registered = registerFont(name);
            for (String family : registered) {
                if (family != null && !family.isEmpty() && !families.contains(family)) {
                    families.add(family);
                }
            }
        }
        for (String emoji : PLATFORM_EMOJI_FONTS) {
            if (!families.contains(emoji)) {
                families.add(emoji);
            }
        }
        if (families.isEmpty()) {
            families.add("SansSerif");
        }
        return families.toArray(new String[0]);
    }

    private List<String> registerFont(String name) throws IOException {
        List<String> aliases = new ArrayList<>();
        if (name == null || name.trim().isEmpty()) {
            return aliases;
        }
        aliases.add(name);
        File file = new File(name);
        if (file.isFile()) {
            Typeface typeface = FontMgr.getDefault().makeFromFile(file.getAbsolutePath());
            ownedTypefaces.add(typeface);
            fontProvider.registerTypeface(typeface, name);
            addTypefaceFamilyAlias(typeface, aliases);
            return aliases;
        }
        if (name.indexOf(':') >= 0) {
            ResourceLocation location = new ResourceLocation(name);
            IResource resource = resourceManager.getResource(location);
            try (InputStream input = resource.getInputStream()) {
                byte[] bytes = readAllBytes(input);
                Typeface typeface = FontMgr.getDefault().makeFromData(Data.makeFromBytes(bytes));
                ownedTypefaces.add(typeface);
                fontProvider.registerTypeface(typeface, name);
                addTypefaceFamilyAlias(typeface, aliases);
                for (NeofontrenderConfig.BuiltinFont builtin : NeofontrenderConfig.builtinFonts()) {
                    if (builtin.location().equals(name)) {
                        fontProvider.registerTypeface(typeface, builtin.displayName());
                        aliases.add(builtin.displayName());
                    }
                }
            }
            return aliases;
        }
        return aliases;
    }

    private void addTypefaceFamilyAlias(Typeface typeface, List<String> aliases) {
        String family = typeface.getFamilyName();
        if (family != null && !family.isEmpty()) {
            fontProvider.registerTypeface(typeface, family);
            aliases.add(family);
        }
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    @Override
    public void close() {
        for (RenderedText text : renderCache.values()) {
            text.close();
        }
        for (RenderedText text : segmentCache.values()) {
            text.close();
        }
        renderCache.clear();
        segmentCache.clear();
        measureCache.clear();
        for (Typeface typeface : ownedTypefaces) {
            typeface.close();
        }
        ownedTypefaces.clear();
        if (isolatedGpuContext != null) {
            isolatedGpuContext.close();
            isolatedGpuContext = null;
        }
        if (colorGlyphDetector != null) {
            colorGlyphDetector.close();
            colorGlyphDetector = null;
        }
        fontCollection.close();
        fontProvider.close();
    }

    private static final class IsolatedGpuContext implements AutoCloseable {
        private static final int GL_FRAMEBUFFER_BINDING = 0x8CA6;
        private static final int GL_RENDERBUFFER_BINDING = 0x8CA7;
        private static final int GL_TEXTURE_BASE_LEVEL = 0x813C;
        private static final int GL_TEXTURE_MAX_LEVEL = 0x813D;

        private final long window;
        private final Object capabilities;
        private final DirectContext context;

        private IsolatedGpuContext(long window, Object capabilities, DirectContext context) {
            this.window = window;
            this.capabilities = capabilities;
            this.context = context;
        }

        private static IsolatedGpuContext create() throws IOException {
            long mainWindow = GLFW.glfwGetCurrentContext();
            if (mainWindow == 0L) {
                throw new IOException("Cannot create isolated Skia GPU context without a current Minecraft GLFW context");
            }
            Object mainCapabilities = currentCapabilities();
            GLFW.glfwDefaultWindowHints();
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API);
            long isolatedWindow = GLFW.glfwCreateWindow(1, 1, "NeoFontRender Skia GPU", 0L, mainWindow);
            GLFW.glfwDefaultWindowHints();
            if (isolatedWindow == 0L) {
                throw new IOException("GLFW failed to create isolated Skia GPU context");
            }

            try {
                GLFW.glfwMakeContextCurrent(isolatedWindow);
                Object isolatedCapabilities = createCapabilities();
                DirectContext directContext = DirectContext.makeGL();
                if (directContext == null) {
                    throw new IOException("Skia DirectContext.makeGL() returned null in isolated context");
                }
                GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 4);
                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
                GLFW.glfwMakeContextCurrent(mainWindow);
                setCapabilities(mainCapabilities);
                NeoFontRender.LOGGER.info("Created isolated Skia GPU OpenGL context");
                return new IsolatedGpuContext(isolatedWindow, isolatedCapabilities, directContext);
            } catch (Throwable t) {
                GLFW.glfwMakeContextCurrent(mainWindow);
                setCapabilities(mainCapabilities);
                GLFW.glfwDestroyWindow(isolatedWindow);
                if (t instanceof IOException) {
                    throw (IOException) t;
                }
                throw new IOException("Failed to initialize isolated Skia GPU context", t);
            }
        }

        private GpuTextTexture render(Paragraph paragraph, int width, int height, int textureScale,
                                      float paintX, float paintY, boolean collectStats,
                                      boolean generateMipmaps, boolean crispSampling) throws IOException {
            if (width <= 0 || height <= 0) {
                throw new IOException("Invalid isolated GPU text texture size " + width + "x" + height);
            }

            ContextSwitch contextSwitch = makeCurrent();
            int textureId = 0;
            int framebufferId = 0;
            int stencilBufferId = 0;
            boolean keepTexture = false;
            try {
                clearGlErrors();
                textureId = GL11.glGenTextures();
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 33071);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 33071);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0,
                        GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
                checkGlError("isolated texture setup");

                framebufferId = GL30.glGenFramebuffers();
                stencilBufferId = GL30.glGenRenderbuffers();
                if (framebufferId <= 0 || stencilBufferId <= 0) {
                    throw new IOException("OpenGL framebuffer/renderbuffer objects are unavailable in isolated context");
                }
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebufferId);
                GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                        GL11.GL_TEXTURE_2D, textureId, 0);
                GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, stencilBufferId);
                GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_STENCIL_INDEX8, width, height);
                GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_STENCIL_ATTACHMENT,
                        GL30.GL_RENDERBUFFER, stencilBufferId);
                checkGlError("isolated framebuffer setup");
                int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
                if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                    throw new IOException("Incomplete isolated Skia GPU framebuffer: status=0x" + Integer.toHexString(status));
                }

                GL11.glViewport(0, 0, width, height);
                context.resetGLAll();
                checkGlError("isolated before Skia surface");
                try (BackendRenderTarget target = BackendRenderTarget.makeGL(
                        width, height, 0, 8, framebufferId, FramebufferFormat.GR_GL_RGBA8);
                     ColorSpace colorSpace = ColorSpace.getSRGB()) {
                    Surface surface = Surface.makeFromBackendRenderTarget(
                            context, target, SurfaceOrigin.BOTTOM_LEFT, ColorType.RGBA_8888, colorSpace, skiaSurfaceProps());
                    if (surface == null) {
                        throw new IOException("Skia failed to create isolated GPU text surface");
                    }
                    try (Surface closeableSurface = surface) {
                        Canvas canvas = closeableSurface.getCanvas();
                        canvas.clear(0x00000000);
                        if (textureScale != 1) {
                            int save = canvas.save();
                            canvas.scale(textureScale, textureScale);
                            paragraph.paint(canvas, paintX, paintY);
                            canvas.restoreToCount(save);
                        } else {
                            paragraph.paint(canvas, paintX, paintY);
                        }
                        context.flushAndSubmit(closeableSurface);
                        GL11.glFinish();
                        checkGlError("isolated Skia flush");
                    }
                } finally {
                    context.resetGLAll();
                }

                RasterStats stats = null;
                if (collectStats) {
                    ByteBuffer readback = BufferUtils.createByteBuffer(width * height * 4);
                    GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, readback);
                    checkGlError("isolated readback stats");
                    stats = RasterStats.fromRgba(readback, width, height);
                }

                boolean mipmapsGenerated = false;
                if (generateMipmaps) {
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
                    int maxLevel = Math.max(0, (int) Math.floor(Math.log(Math.max(width, height)) / Math.log(2.0D)));
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, maxLevel);
                    GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
                    checkGlError("isolated mipmap generation");
                    mipmapsGenerated = true;
                }

                keepTexture = true;
                return new GpuTextTexture(textureId, mipmapsGenerated, crispSampling, stats);
            } finally {
                try {
                    GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
                    GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);
                    if (stencilBufferId > 0) {
                        GL30.glDeleteRenderbuffers(stencilBufferId);
                    }
                    if (framebufferId > 0) {
                        GL30.glDeleteFramebuffers(framebufferId);
                    }
                    if (!keepTexture && textureId > 0) {
                        GL11.glDeleteTextures(textureId);
                    }
                    drainGlErrors();
                } finally {
                    contextSwitch.restore();
                }
            }
        }

        private GpuReadback renderReadback(Paragraph paragraph, int width, int height, int textureScale,
                                           float paintX, float paintY, boolean collectStats) throws IOException {
            if (width <= 0 || height <= 0) {
                throw new IOException("Invalid isolated GPU diagnostic size " + width + "x" + height);
            }

            ContextSwitch contextSwitch = makeCurrent();
            int textureId = 0;
            int framebufferId = 0;
            int stencilBufferId = 0;
            try {
                clearGlErrors();
                textureId = GL11.glGenTextures();
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 33071);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 33071);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0,
                        GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
                checkGlError("diagnostic texture setup");

                framebufferId = GL30.glGenFramebuffers();
                stencilBufferId = GL30.glGenRenderbuffers();
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebufferId);
                GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                        GL11.GL_TEXTURE_2D, textureId, 0);
                GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, stencilBufferId);
                GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_STENCIL_INDEX8, width, height);
                GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_STENCIL_ATTACHMENT,
                        GL30.GL_RENDERBUFFER, stencilBufferId);
                checkGlError("diagnostic framebuffer setup");
                int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
                if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                    throw new IOException("Incomplete diagnostic GPU framebuffer: status=0x" + Integer.toHexString(status));
                }

                GL11.glViewport(0, 0, width, height);
                context.resetGLAll();
                checkGlError("diagnostic before Skia surface");
                try (BackendRenderTarget target = BackendRenderTarget.makeGL(
                        width, height, 0, 8, framebufferId, FramebufferFormat.GR_GL_RGBA8);
                     ColorSpace colorSpace = ColorSpace.getSRGB()) {
                    Surface surface = Surface.makeFromBackendRenderTarget(
                            context, target, SurfaceOrigin.BOTTOM_LEFT, ColorType.RGBA_8888, colorSpace, skiaSurfaceProps());
                    if (surface == null) {
                        throw new IOException("Skia failed to create diagnostic GPU surface");
                    }
                    try (Surface closeableSurface = surface) {
                        Canvas canvas = closeableSurface.getCanvas();
                        canvas.clear(0x00000000);
                        if (textureScale != 1) {
                            int save = canvas.save();
                            canvas.scale(textureScale, textureScale);
                            paragraph.paint(canvas, paintX, paintY);
                            canvas.restoreToCount(save);
                        } else {
                            paragraph.paint(canvas, paintX, paintY);
                        }
                        context.flushAndSubmit(closeableSurface);
                        GL11.glFinish();
                        checkGlError("diagnostic Skia flush");
                    }
                } finally {
                    context.resetGLAll();
                }

                ByteBuffer readback = BufferUtils.createByteBuffer(width * height * 4);
                GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, readback);
                checkGlError("diagnostic readback");
                return GpuReadback.fromBottomLeftRgba(readback, width, height, collectStats);
            } finally {
                try {
                    GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
                    GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);
                    if (stencilBufferId > 0) {
                        GL30.glDeleteRenderbuffers(stencilBufferId);
                    }
                    if (framebufferId > 0) {
                        GL30.glDeleteFramebuffers(framebufferId);
                    }
                    if (textureId > 0) {
                        GL11.glDeleteTextures(textureId);
                    }
                    drainGlErrors();
                } finally {
                    contextSwitch.restore();
                }
            }
        }

        private ContextSwitch makeCurrent() {
            long previousWindow = GLFW.glfwGetCurrentContext();
            Object previousCapabilities = currentCapabilities();
            GLFW.glfwMakeContextCurrent(window);
            setCapabilities(capabilities);
            return new ContextSwitch(previousWindow, previousCapabilities);
        }

        @Override
        public void close() {
            ContextSwitch contextSwitch = null;
            try {
                contextSwitch = makeCurrent();
                context.close();
            } catch (RuntimeException | LinkageError ignored) {
            } finally {
                if (contextSwitch != null) {
                    contextSwitch.restore();
                }
                GLFW.glfwDestroyWindow(window);
            }
        }

        private static Object currentCapabilities() {
            try {
                Class<?> glClass = Class.forName("org.lwjgl.opengl.GL");
                return glClass.getMethod("getCapabilities").invoke(null);
            } catch (Throwable ignored) {
                return null;
            }
        }

        private static Object createCapabilities() throws IOException {
            try {
                Class<?> glClass = Class.forName("org.lwjgl.opengl.GL");
                return glClass.getMethod("createCapabilities").invoke(null);
            } catch (Throwable t) {
                throw new IOException("LWJGL3 GL capabilities API is unavailable", t);
            }
        }

        private static void setCapabilities(Object capabilities) {
            try {
                Class<?> glClass = Class.forName("org.lwjgl.opengl.GL");
                Class<?> capabilitiesClass = Class.forName("org.lwjgl.opengl.GLCapabilities");
                glClass.getMethod("setCapabilities", capabilitiesClass).invoke(null, capabilities);
            } catch (Throwable ignored) {
            }
        }

        private static void clearGlErrors() {
            drainGlErrors();
        }

        private static int drainGlErrors() {
            int first = GL11.GL_NO_ERROR;
            int error;
            while ((error = GL11.glGetError()) != GL11.GL_NO_ERROR) {
                if (first == GL11.GL_NO_ERROR) {
                    first = error;
                }
            }
            return first;
        }

        private static void checkGlError(String stage) throws IOException {
            int error = drainGlErrors();
            if (error != GL11.GL_NO_ERROR) {
                throw new IOException("Skia isolated GPU GL error at " + stage + ": 0x" + Integer.toHexString(error));
            }
        }

        private static final class ContextSwitch {
            private final long previousWindow;
            private final Object previousCapabilities;

            private ContextSwitch(long previousWindow, Object previousCapabilities) {
                this.previousWindow = previousWindow;
                this.previousCapabilities = previousCapabilities;
            }

            private void restore() {
                GLFW.glfwMakeContextCurrent(previousWindow);
                setCapabilities(previousCapabilities);
            }
        }
    }

    private static final class GpuTextTexture extends AbstractTexture {
        private final int textureId;
        private final boolean mipmapsGenerated;
        private final boolean crispSampling;
        private final RasterStats debugStats;

        private GpuTextTexture(int textureId, boolean mipmapsGenerated, boolean crispSampling,
                               RasterStats debugStats) {
            this.textureId = textureId;
            this.mipmapsGenerated = mipmapsGenerated;
            this.crispSampling = crispSampling;
            this.debugStats = debugStats;
        }

        @Override
        public int getGlTextureId() {
            return textureId;
        }

        @Override
        public void deleteGlTexture() {
            TextureUtil.deleteTexture(textureId);
        }

        @Override
        public void loadTexture(IResourceManager resourceManager) {
        }

        @Override
        public void setBlurMipmap(boolean blur, boolean mipmap) {
            super.setBlurMipmap(blur, mipmap && mipmapsGenerated);
        }

        private boolean hasMipmaps() {
            return mipmapsGenerated;
        }

        private boolean usesCrispSampling() {
            return crispSampling;
        }

        private RasterStats debugStats() {
            return debugStats;
        }
    }

    private static final class GpuReadback {
        private final int[] argbPixels;
        private final RasterStats stats;

        private GpuReadback(int[] argbPixels, RasterStats stats) {
            this.argbPixels = argbPixels;
            this.stats = stats;
        }

        private static GpuReadback fromBottomLeftRgba(ByteBuffer rgba, int width, int height, boolean collectStats) {
            int[] argb = new int[width * height];
            for (int y = 0; y < height; y++) {
                int srcY = height - 1 - y;
                for (int x = 0; x < width; x++) {
                    int src = (srcY * width + x) * 4;
                    int r = rgba.get(src) & 0xFF;
                    int g = rgba.get(src +  1) & 0xFF;
                    int b = rgba.get(src + 2) & 0xFF;
                    int a = rgba.get(src + 3) & 0xFF;
                    argb[y * width + x] = (a << 24) | (r << 16) | (g << 8) | b;
                }
            }
            return new GpuReadback(argb, collectStats ? RasterStats.fromArgb(argb, width, height) : null);
        }
    }

    private static final class RasterStats {
        private final int width;
        private final int height;
        private final int nonZeroAlpha;
        private final int strongAlpha;
        private final int maxAlpha;
        private final int minX;
        private final int minY;
        private final int maxX;
        private final int maxY;
        private final String error;

        private RasterStats(int width, int height, int nonZeroAlpha, int strongAlpha, int maxAlpha,
                            int minX, int minY, int maxX, int maxY, String error) {
            this.width = width;
            this.height = height;
            this.nonZeroAlpha = nonZeroAlpha;
            this.strongAlpha = strongAlpha;
            this.maxAlpha = maxAlpha;
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            this.error = error;
        }

        private static RasterStats fromArgb(int[] pixels, int width, int height) {
            StatsBuilder stats = new StatsBuilder(width, height);
            for (int i = 0; i < pixels.length; i++) {
                stats.feed(i % width, i / width, (pixels[i] >>> 24) & 0xFF);
            }
            return stats.build();
        }

        private static RasterStats fromRgba(ByteBuffer pixels, int width, int height) {
            StatsBuilder stats = new StatsBuilder(width, height);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int offset = (y * width + x) * 4 + 3;
                    stats.feed(x, y, pixels.get(offset) & 0xFF);
                }
            }
            return stats.build();
        }

        private static RasterStats error(Throwable t) {
            return new RasterStats(0, 0, 0, 0, 0, 0, 0, -1, -1,
                    "err=" + t.getClass().getSimpleName());
        }

        @Override
        public String toString() {
            if (error != null) {
                return error;
            }
            String box = maxX >= minX && maxY >= minY
                    ? minX + "," + minY + "-" + maxX + "," + maxY
                    : "empty";
            return width + "x" + height
                    + " a>0=" + nonZeroAlpha
                    + " a>8=" + strongAlpha
                    + " maxA=" + maxAlpha
                    + " box=" + box;
        }
    }

    private static final class StatsBuilder {
        private final int width;
        private final int height;
        private int nonZeroAlpha;
        private int strongAlpha;
        private int maxAlpha;
        private int minX = Integer.MAX_VALUE;
        private int minY = Integer.MAX_VALUE;
        private int maxX = -1;
        private int maxY = -1;

        private StatsBuilder(int width, int height) {
            this.width = width;
            this.height = height;
        }

        private void feed(int x, int y, int alpha) {
            if (alpha <= 0) {
                return;
            }
            nonZeroAlpha++;
            if (alpha > 8) {
                strongAlpha++;
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
            if (alpha > maxAlpha) {
                maxAlpha = alpha;
            }
        }

        private RasterStats build() {
            return new RasterStats(width, height, nonZeroAlpha, strongAlpha, maxAlpha,
                    minX == Integer.MAX_VALUE ? 0 : minX,
                    minY == Integer.MAX_VALUE ? 0 : minY,
                    maxX, maxY, null);
        }
    }

    public static final class DebugState {
        private final boolean gpuRequested;
        private final boolean gpuContextCreated;
        private final boolean gpuUnavailable;
        private final String lastRasterPath;
        private final String lastGpuFallbackReason;
        private final String lastRasterStats;
        private final String lastDrawState;
        private final long cpuRasterCount;
        private final long gpuRasterCount;
        private final int renderCacheSize;
        private final int segmentCacheSize;
        private final int measureCacheSize;
        private final int renderCacheMax;
        private final int segmentCacheMax;
        private final int measureCacheMax;
        private final long renderCacheHits;
        private final long renderCacheMisses;
        private final long renderCacheEvictions;
        private final long segmentCacheHits;
        private final long segmentCacheMisses;
        private final long segmentCacheEvictions;
        private final long measureCacheHits;
        private final long measureCacheMisses;
        private final long measureCacheEvictions;

        private DebugState(boolean gpuRequested, boolean gpuContextCreated, boolean gpuUnavailable,
                           String lastRasterPath, String lastGpuFallbackReason,
                           String lastRasterStats,
                           String lastDrawState,
                           long cpuRasterCount, long gpuRasterCount,
                           int renderCacheSize, int segmentCacheSize, int measureCacheSize,
                           int renderCacheMax, int segmentCacheMax, int measureCacheMax,
                           long renderCacheHits, long renderCacheMisses, long renderCacheEvictions,
                           long segmentCacheHits, long segmentCacheMisses, long segmentCacheEvictions,
                           long measureCacheHits, long measureCacheMisses, long measureCacheEvictions) {
            this.gpuRequested = gpuRequested;
            this.gpuContextCreated = gpuContextCreated;
            this.gpuUnavailable = gpuUnavailable;
            this.lastRasterPath = lastRasterPath;
            this.lastGpuFallbackReason = lastGpuFallbackReason;
            this.lastRasterStats = lastRasterStats;
            this.lastDrawState = lastDrawState;
            this.cpuRasterCount = cpuRasterCount;
            this.gpuRasterCount = gpuRasterCount;
            this.renderCacheSize = renderCacheSize;
            this.segmentCacheSize = segmentCacheSize;
            this.measureCacheSize = measureCacheSize;
            this.renderCacheMax = renderCacheMax;
            this.segmentCacheMax = segmentCacheMax;
            this.measureCacheMax = measureCacheMax;
            this.renderCacheHits = renderCacheHits;
            this.renderCacheMisses = renderCacheMisses;
            this.renderCacheEvictions = renderCacheEvictions;
            this.segmentCacheHits = segmentCacheHits;
            this.segmentCacheMisses = segmentCacheMisses;
            this.segmentCacheEvictions = segmentCacheEvictions;
            this.measureCacheHits = measureCacheHits;
            this.measureCacheMisses = measureCacheMisses;
            this.measureCacheEvictions = measureCacheEvictions;
        }

        public boolean gpuRequested() {
            return gpuRequested;
        }

        public boolean gpuContextCreated() {
            return gpuContextCreated;
        }

        public boolean gpuUnavailable() {
            return gpuUnavailable;
        }

        public String lastRasterPath() {
            return lastRasterPath;
        }

        public String lastGpuFallbackReason() {
            return lastGpuFallbackReason;
        }

        public String lastRasterStats() {
            return lastRasterStats;
        }

        public String lastDrawState() {
            return lastDrawState;
        }

        public long cpuRasterCount() {
            return cpuRasterCount;
        }

        public long gpuRasterCount() {
            return gpuRasterCount;
        }

        public int renderCacheSize() {
            return renderCacheSize;
        }

        public int segmentCacheSize() {
            return segmentCacheSize;
        }

        public int measureCacheSize() {
            return measureCacheSize;
        }

        public int renderCacheMax() {
            return renderCacheMax;
        }

        public int segmentCacheMax() {
            return segmentCacheMax;
        }

        public int measureCacheMax() {
            return measureCacheMax;
        }

        public long renderCacheHits() {
            return renderCacheHits;
        }

        public long renderCacheMisses() {
            return renderCacheMisses;
        }

        public long renderCacheEvictions() {
            return renderCacheEvictions;
        }

        public long segmentCacheHits() {
            return segmentCacheHits;
        }

        public long segmentCacheMisses() {
            return segmentCacheMisses;
        }

        public long segmentCacheEvictions() {
            return segmentCacheEvictions;
        }

        public long measureCacheHits() {
            return measureCacheHits;
        }

        public long measureCacheMisses() {
            return measureCacheMisses;
        }

        public long measureCacheEvictions() {
            return measureCacheEvictions;
        }
    }

    public static final class RenderedText implements TextRenderResult, AutoCloseable {
        private static final RenderedText EMPTY = new RenderedText(null, null, 0.0F, 0, 0, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, false);
        private static final java.util.Map<ResourceLocation, Float> LAST_FILTER_SCALE = new java.util.HashMap<>();

        private final ResourceLocation location;
        private final AbstractTexture texture;
        private final float advance;
        private final int width;
        private final int height;
        private final float drawWidth;
        private final float drawHeight;
        private final float rasterScale;
        private final float horizontalOffset;
        private final float verticalOffset;
        private final boolean flipY;
        private volatile long lastAccessMillis;

        private RenderedText(ResourceLocation location, AbstractTexture texture, float advance,
                             int width, int height, float drawWidth, float drawHeight,
                             float rasterScale, float horizontalOffset, float verticalOffset, boolean flipY) {
            this.location = location;
            this.texture = texture;
            this.advance = advance;
            this.width = width;
            this.height = height;
            this.drawWidth = drawWidth;
            this.drawHeight = drawHeight;
            this.rasterScale = rasterScale;
            this.horizontalOffset = horizontalOffset;
            this.verticalOffset = verticalOffset;
            this.flipY = flipY;
            this.lastAccessMillis = System.currentTimeMillis();
        }

        private RenderedText withFixedDrawWidth(float fixedWidth) {
            return new RenderedText(location, texture, advance, width, height, fixedWidth, drawHeight,
                    rasterScale, horizontalOffset, verticalOffset, flipY);
        }

        DynamicTexture getCpuTexture() {
            return texture instanceof DynamicTexture ? (DynamicTexture) texture : null;
        }

        public float advance() {
            return advance;
        }

        public void draw(float x, float y, float alpha) {
            drawTinted(x, y, alpha, 0xFFFFFFFF);
        }

        private void drawTinted(float x, float y, float alpha, int tintArgb) {
            if (location == null || texture == null || width <= 0 || height <= 0) {
                return;
            }
            touch();
            net.minecraft.client.Minecraft.getMinecraft().getTextureManager().bindTexture(location);
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            float tintR = ((tintArgb >>> 16) & 0xFF) / 255.0F;
            float tintG = ((tintArgb >>> 8) & 0xFF) / 255.0F;
            float tintB = (tintArgb & 0xFF) / 255.0F;
            GlStateManager.color(tintR, tintG, tintB, alpha);

            Float lastScale = LAST_FILTER_SCALE.get(location);
            if (lastScale == null || Math.abs(lastScale - rasterScale) > 0.01f) {
                if (texture instanceof GpuTextTexture
                        && ((GpuTextTexture) texture).usesCrispSampling()) {
                    // Other renderers can mutate parameters on the currently bound texture. Reassert
                    // the shared texture's level-0 nearest filter on first use in each scale bucket.
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                } else {
                    FontRenderTuning.applyBoundTextureFilter(rasterScale, !(texture instanceof GpuTextTexture)
                            || ((GpuTextTexture) texture).hasMipmaps());
                }
                LAST_FILTER_SCALE.put(location, rasterScale);
            }
            float left = FontRenderTuning.alignToPixel(x + horizontalOffset);
            float top = FontRenderTuning.alignToPixel(y + verticalOffset);

            boolean premultiplied = flipY;
            boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);
            int origSrcRgb = 0, origDstRgb = 0, origSrcAlpha = 0, origDstAlpha = 0;
            if (premultiplied) {
                origSrcRgb = getInteger(GL14.GL_BLEND_SRC_RGB, GL11.GL_SRC_ALPHA);
                origDstRgb = getInteger(GL14.GL_BLEND_DST_RGB, GL11.GL_ONE_MINUS_SRC_ALPHA);
                origSrcAlpha = getInteger(GL14.GL_BLEND_SRC_ALPHA, GL11.GL_ONE);
                origDstAlpha = getInteger(GL14.GL_BLEND_DST_ALPHA, GL11.GL_ZERO);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(
                        GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA,
                        GL11.GL_ONE, GL11.GL_ZERO);
            }

            try (FontRenderPipeline.State ignored = FontRenderPipeline.begin(rasterScale)) {
                lastDrawState = drawState(texture);
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder buffer = tessellator.getBuffer();
                double topV = flipY ? 1.0D : 0.0D;
                double bottomV = flipY ? 0.0D : 1.0D;
                buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                buffer.pos(left, top, 0.0D).tex(0.0D, topV).color(tintR, tintG, tintB, alpha).endVertex();
                buffer.pos(left, top + drawHeight, 0.0D).tex(0.0D, bottomV).color(tintR, tintG, tintB, alpha).endVertex();
                buffer.pos(left + drawWidth, top + drawHeight, 0.0D).tex(1.0D, bottomV).color(tintR, tintG, tintB, alpha).endVertex();
                buffer.pos(left + drawWidth, top, 0.0D).tex(1.0D, topV).color(tintR, tintG, tintB, alpha).endVertex();
                tessellator.draw();
            }

            if (premultiplied) {
                // Do not restore with raw GL14.glBlendFuncSeparate. Minecraft 1.12 caches these
                // values in GlStateManager; bypassing it leaves the cache and driver out of sync,
                // so later premultiplied glyphs are accidentally drawn with SRC_ALPHA twice.
                GlStateManager.tryBlendFuncSeparate(origSrcRgb, origDstRgb, origSrcAlpha, origDstAlpha);
                if (!wasBlend) {
                    GlStateManager.disableBlend();
                }
            }
        }

        @Override
        public void close() {
            if (texture != null) {
                texture.deleteGlTexture();
            }
        }

        private void touch() {
            lastAccessMillis = System.currentTimeMillis();
        }

        private static String drawState(AbstractTexture texture) {
            boolean gpu = texture instanceof GpuTextTexture;
            boolean mip = gpu && ((GpuTextTexture) texture).hasMipmaps();
            return "tex=" + (gpu ? "gpu" : "cpu")
                    + " mip=" + mip
                    + " min=" + getTexParameteri(GL11.GL_TEXTURE_MIN_FILTER, -1)
                    + " mag=" + getTexParameteri(GL11.GL_TEXTURE_MAG_FILTER, -1)
                    + " blend=" + GL11.glIsEnabled(GL11.GL_BLEND)
                    + " func=" + getInteger(GL14.GL_BLEND_SRC_RGB, -1)
                    + "/" + getInteger(GL14.GL_BLEND_DST_RGB, -1)
                    + "/" + getInteger(GL14.GL_BLEND_SRC_ALPHA, -1)
                    + "/" + getInteger(GL14.GL_BLEND_DST_ALPHA, -1);
        }

        private static int getInteger(int key, int fallback) {
            try {
                return GL11.glGetInteger(key);
            } catch (RuntimeException | LinkageError ignored) {
                return fallback;
            }
        }

        private static int getTexParameteri(int key, int fallback) {
            try {
                return GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, key);
            } catch (RuntimeException | LinkageError ignored) {
                return fallback;
            }
        }

        private boolean isExpired(long nowMillis, long ttlMillis) {
            return nowMillis - lastAccessMillis >= ttlMillis;
        }
    }

    /**
     * Per-draw color for a cached white glyph texture.
     *
     * <p>The monochrome cache key intentionally omits ARGB. Keeping tint on the cached
     * {@link RenderedText} itself makes shadow/foreground passes and repeated formatted
     * segments overwrite each other's color before a composite is drawn.</p>
     */
    private static final class TintedRenderedText implements TextRenderResult {
        private final RenderedText rendered;
        private final int argb;

        private TintedRenderedText(RenderedText rendered, int argb) {
            this.rendered = rendered;
            this.argb = argb;
        }

        @Override
        public float advance() {
            return rendered.advance();
        }

        @Override
        public void draw(float x, float y, float alpha) {
            rendered.drawTinted(x, y, alpha, argb);
        }
    }

    private static final class CompositeRenderedText implements TextRenderResult {
        private final float advance;
        private final List<CompositePiece> pieces;

        private CompositeRenderedText(float advance, List<CompositePiece> pieces) {
            this.advance = advance;
            this.pieces = pieces;
        }

        @Override
        public float advance() {
            return advance;
        }

        @Override
        public void draw(float x, float y, float alpha) {
            for (CompositePiece piece : pieces) {
                piece.result.draw(x + piece.offsetX, y, alpha);
            }
        }
    }

    private static final class CompositePiece {
        private final TextRenderResult result;
        private final float offsetX;

        private CompositePiece(TextRenderResult result, float offsetX) {
            this.result = result;
            this.offsetX = offsetX;
        }
    }

    private static final class FormattedRun {
        private final String text;
        private final int argb;
        private final boolean bold;
        private final boolean italic;
        private final boolean underline;
        private final boolean strikethrough;

        private FormattedRun(String text, int argb, boolean bold, boolean italic,
                             boolean underline, boolean strikethrough) {
            this.text = text;
            this.argb = argb;
            this.bold = bold;
            this.italic = italic;
            this.underline = underline;
            this.strikethrough = strikethrough;
        }
    }

    private static final class RenderKey {
        private final String text;
        private final int argb;
        private final boolean formatted;
        private final int styleFlags;
        private final int scaleBucket;
        private final int texturePathBucket;
        private final boolean colorBaked;

        private RenderKey(String text, int argb, boolean formatted, int styleFlags, float scale,
                          int texturePathBucket, boolean colorBaked) {
            this.text = text;
            this.argb = argb;
            this.formatted = formatted;
            this.styleFlags = styleFlags;
            this.scaleBucket = scaleBucket(scale);
            this.texturePathBucket = texturePathBucket;
            this.colorBaked = colorBaked;
        }

        private static RenderKey plain(String text, int argb, boolean bold, boolean italic, float scale,
                                       int texturePathBucket, boolean colorBaked) {
            return new RenderKey(text, argb, false, styleFlags(bold, italic, false, false, false), scale,
                    texturePathBucket, colorBaked);
        }

        private static RenderKey formatted(String text, int argb, boolean shadow, float scale,
                                           int texturePathBucket, boolean colorBaked) {
            return new RenderKey(text, argb, true, styleFlags(false, false, false, false, shadow), scale,
                    texturePathBucket, colorBaked);
        }

        private static RenderKey decoratedSegment(String text, int argb, boolean bold, boolean italic,
                                                  boolean underline, boolean strikethrough, float scale,
                                                  int texturePathBucket, boolean colorBaked) {
            return new RenderKey(text, argb, true, styleFlags(bold, italic, underline, strikethrough, false), scale,
                    texturePathBucket, colorBaked);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof RenderKey)) {
                return false;
            }
            RenderKey other = (RenderKey) obj;
            return formatted == other.formatted
                    && styleFlags == other.styleFlags
                    && scaleBucket == other.scaleBucket
                    && texturePathBucket == other.texturePathBucket
                    && colorBaked == other.colorBaked
                    && (!colorBaked || argb == other.argb)
                    && text.equals(other.text);
        }

        @Override
        public int hashCode() {
            int result = text.hashCode();
            result = 31 * result + (formatted ? 1 : 0);
            result = 31 * result + styleFlags;
            result = 31 * result + scaleBucket;
            result = 31 * result + texturePathBucket;
            result = 31 * result + (colorBaked ? 1 : 0);
            if (colorBaked) {
                result = 31 * result + argb;
            }
            return result;
        }

        private static int styleFlags(boolean bold, boolean italic, boolean underline,
                                      boolean strikethrough, boolean shadow) {
            int flags = 0;
            if (bold) {
                flags |= 1;
            }
            if (italic) {
                flags |= 1 << 1;
            }
            if (underline) {
                flags |= 1 << 2;
            }
            if (strikethrough) {
                flags |= 1 << 3;
            }
            if (shadow) {
                flags |= 1 << 4;
            }
            return flags;
        }
    }

    private static final class MeasureKey {
        private final String text;
        private final boolean formatted;
        private final boolean primaryStyle;
        private final boolean secondaryStyle;
        private final int scaleBucket;

        private MeasureKey(String text, boolean formatted, boolean primaryStyle, boolean secondaryStyle, float scale) {
            this.text = text;
            this.formatted = formatted;
            this.primaryStyle = primaryStyle;
            this.secondaryStyle = secondaryStyle;
            this.scaleBucket = scaleBucket(scale);
        }

        private static MeasureKey plain(String text, boolean bold, boolean italic, float scale) {
            return new MeasureKey(text, false, bold, italic, scale);
        }

        private static MeasureKey formatted(String text, boolean shadow, float scale, boolean perspective) {
            // Perspective text is measured as one shaped run, while GUI text may be the sum of
            // reusable segments. Keep both results separate because kerning can change the width.
            return new MeasureKey(text, true, shadow, perspective, scale);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MeasureKey)) {
                return false;
            }
            MeasureKey other = (MeasureKey) obj;
            return formatted == other.formatted
                    && primaryStyle == other.primaryStyle
                    && secondaryStyle == other.secondaryStyle
                    && scaleBucket == other.scaleBucket
                    && text.equals(other.text);
        }

        @Override
        public int hashCode() {
            int result = text.hashCode();
            result = 31 * result + (formatted ? 1 : 0);
            result = 31 * result + (primaryStyle ? 1 : 0);
            result = 31 * result + (secondaryStyle ? 1 : 0);
            result = 31 * result + scaleBucket;
            return result;
        }
    }

    private static int scaleBucket(float scale) {
        return Math.round(scale * 100.0F);
    }

}
