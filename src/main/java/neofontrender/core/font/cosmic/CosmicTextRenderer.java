package neofontrender.core.font.cosmic;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import neofontrender.NeoFontRender;
import neofontrender.api.color.TextColorPaletteRegistry;
import neofontrender.api.text.FontRenderSpec;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.core.font.support.FontRenderTuning;
import neofontrender.core.font.support.FontRenderDiagnostics;
import neofontrender.core.font.support.ClientTextureDisposal;
import neofontrender.core.font.support.ModernShadowRasterizer;
import neofontrender.core.font.support.ShadowColorPolicy;
import neofontrender.core.font.support.ShadowColorRemapRules;
import neofontrender.core.font.support.ShadowMaskRules;
import neofontrender.core.font.support.ShadowRenderSpec;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.lwjgl.BufferUtils;

/** cosmic-text shaping/Swash rasterization with Minecraft's LWJGL2 texture submission. */
public final class CosmicTextRenderer implements TextRenderBackend {
    private static final int RASTER_MAGIC = 0x434F534D;
    // Native Cosmic rasters reserve four transparent texels around the glyph bounds. Keep the
    // encoded distance range within that guard so edge samples never need outside-texture data.
    // Rust rejects either raster dimension above 8192. Keep a generous allowance for glyph
    // bearings, italic overhang, and native padding beyond cosmic-text's line advance.
    private static final float SEGMENT_RASTER_ADVANCE_LIMIT = 8192.0F - 1024.0F;
    private final TextureManager textureManager;
    private final Map<RenderKey, CosmicRenderedText> renderCache = new LinkedHashMap<>(128, 0.75F, true);
    private final Map<MeasureKey, Float> measureCache = new LinkedHashMap<>(256, 0.75F, true);
    private long engine;
    private int nextTextureId;
    private final String primaryFamily;
    private long renderCacheHits;
    private long renderCacheMisses;
    private long renderCacheEvictions;
    private long measureCacheHits;
    private long measureCacheMisses;
    private long measureCacheEvictions;
    private long nativeRasterCount;
    private long cacheOperations;
    private volatile int[] legacyColorCodes = TextColorPaletteRegistry.vanillaColorCodes();

    public CosmicTextRenderer(TextureManager textureManager, IResourceManager resourceManager) throws IOException {
        this(textureManager, resourceManager, null);
    }

    public CosmicTextRenderer(TextureManager textureManager, IResourceManager resourceManager,
                              FontRenderSpec scopedSpec) throws IOException {
        this.textureManager = textureManager;
        List<LoadedFont> loadedFonts = scopedSpec == null
                ? loadConfiguredFonts(resourceManager)
                : loadScopedFonts(resourceManager, scopedSpec.fonts());
        byte[][] fonts = new byte[loadedFonts.size()][];
        String[] aliases = new String[loadedFonts.size()];
        for (int i = 0; i < loadedFonts.size(); i++) {
            aliases[i] = loadedFonts.get(i).alias;
            fonts[i] = loadedFonts.get(i).data;
        }
        // The core package intentionally contains no bundled TTF files. cosmic-text/fontdb still
        // loads the operating system font database, so an empty byte-font list is a supported mode
        // and is also what lets the OS-provided color emoji font participate in fallback.
        // Keep the configured family name separate from byte-backed fallback fonts. Native backends can
        // resolve a system family such as "JetBrains Mono" directly, while an older bridge
        // silently skipped it and promoted the first bundled fallback to primary.
        List<String> configuredFamilies = scopedSpec == null
                ? NeofontrenderConfig.fontFamily() : scopedSpec.fonts();
        String primary = scopedSpec == null ? NeofontrenderConfig.fontName()
                : (configuredFamilies.isEmpty() ? NeofontrenderConfig.fontName() : configuredFamilies.get(0));
        List<String> fallbackFamilies = new ArrayList<>(configuredFamilies);
        if (!fallbackFamilies.isEmpty()) {
            fallbackFamilies = fallbackFamilies.subList(1, fallbackFamilies.size());
        }
        engine = CosmicNative.createEngine(fonts, aliases, primary,
                fallbackFamilies.toArray(new String[0]),
                scopedSpec == null ? NeofontrenderConfig.cosmicRegularFont() : "",
                scopedSpec == null ? NeofontrenderConfig.cosmicBoldFont() : "",
                scopedSpec == null ? NeofontrenderConfig.cosmicItalicFont() : "",
                scopedSpec == null ? NeofontrenderConfig.cosmicBoldItalicFont() : "",
                scopedSpec == null && NeofontrenderConfig.cosmicVariantOverridesOnlySwitchFont(),
                NeofontrenderConfig.fontVariableWeight(),
                scopedSpec == null ? NeofontrenderConfig.fontSize() : scopedSpec.size(),
                Locale.getDefault().toLanguageTag());
        if (engine == 0L) {
            throw new IOException("cosmic-text returned a null engine");
        }
        primaryFamily = CosmicNative.primaryFamily(engine);
        NeoFontRender.LOGGER.info(
                "Cosmic renderer loaded {} font resources; primary family='{}'; faces=[regular={}, bold={}, italic={}, boldItalic={}]",
                loadedFonts.size(), primaryFamily,
                CosmicNative.resolvedFace(engine, 0), CosmicNative.resolvedFace(engine, 1),
                CosmicNative.resolvedFace(engine, 2), CosmicNative.resolvedFace(engine, 3));
        String warnings = CosmicNative.resolutionWarnings(engine);
        if (warnings != null && !warnings.isEmpty()) {
            NeoFontRender.LOGGER.warn("Cosmic font resolution warnings:\n{}", warnings);
        }
    }

    @Override
    public boolean isReady() {
        return engine != 0L;
    }

    @Override
    public void updateLegacyColorCodes(int[] colorCodes) {
        legacyColorCodes = TextColorPaletteRegistry.normalizeColorCodes(colorCodes);
    }

    @Override
    public boolean shouldRenderShadow(String text) {
        String mode = NeofontrenderConfig.shadowMode();
        return "all".equals(mode) || (!"none".equals(mode) && !containsEmoji(text)
                && (!"mask".equals(mode) || !ShadowMaskRules.matches(text)));
    }

    @Override
    public synchronized float measure(String text, boolean bold, boolean italic) {
        return measureAtSize(text, bold, italic, NeofontrenderConfig.fontSize());
    }

    private float measureAtSize(String text, boolean bold, boolean italic, float fontSize) {
        if (text == null || text.isEmpty() || engine == 0L) {
            return 0.0F;
        }
        float logicalSize = Math.max(1.0F, fontSize);
        MeasureKey key = new MeasureKey(text, effectiveFlags(bold, italic),
                Float.floatToIntBits(logicalSize));
        Float cached = measureCache.get(key);
        if (cached != null) {
            measureCacheHits++;
            periodicCacheCleanup();
            return cached;
        }
        measureCacheMisses++;
        float width = CosmicNative.measureSized(engine, text, key.flags, logicalSize);
        measureCache.put(key, width);
        trimMeasureCache();
        periodicCacheCleanup();
        return width;
    }

    @Override
    public synchronized TextRenderResult render(String text, int argb, boolean bold, boolean italic) {
        if (text == null || text.isEmpty() || engine == 0L) {
            return TextRenderResult.EMPTY;
        }
        float scale = Math.max(1.0F, FontRenderTuning.rasterScale(NeofontrenderConfig.fontOversample()));
        return renderAtScale(text, argb, bold, italic, NeofontrenderConfig.fontSize(), scale);
    }

    private TextRenderResult renderAtScale(String text, int argb, boolean bold, boolean italic,
                                           float fontSize, float scale) {
        return renderAtScale(text, argb, bold, italic, false, false, fontSize, scale);
    }

    private TextRenderResult renderAtScale(String text, int argb, boolean bold, boolean italic,
                                           boolean underline, boolean strikethrough,
                                           float fontSize, float scale) {
        List<String> segments = renderingSegments(text, bold, italic, fontSize, scale);
        if (segments.size() > 1) {
            return renderSegments(segments, argb, bold, italic, underline, strikethrough,
                    fontSize, scale);
        }
        try {
            return renderSingle(text, argb, bold, italic, underline, strikethrough,
                    fontSize, scale, false);
        } catch (IllegalStateException error) {
            if (!isRasterSizeError(error)) {
                throw error;
            }
            // Advance is normally a reliable predictor, but unusual glyph bearings can make the
            // tight raster much wider. Recursively bisect only this known native size failure.
            List<String> fallbackSegments = CosmicTextSegmenter.splitInHalf(text);
            if (fallbackSegments.size() <= 1) {
                throw error;
            }
            return renderSegments(fallbackSegments, argb, bold, italic, underline, strikethrough,
                    fontSize, scale);
        }
    }

    private CosmicRenderedText renderSingle(String text, int argb, boolean bold, boolean italic,
                                             boolean underline, boolean strikethrough,
                                             float fontSize, float scale, boolean modernShadow) {
        return renderSingle(text, argb, bold, italic, underline, strikethrough,
                fontSize, scale, modernShadow, null, modernShadow ? ShadowRenderSpec.fromConfig() : null);
    }

    private CosmicRenderedText renderSingle(String text, int argb, boolean bold, boolean italic,
                                             boolean underline, boolean strikethrough,
                                             float fontSize, float scale, boolean modernShadow,
                                             Integer explicitShadowArgb) {
        return renderSingle(text, argb, bold, italic, underline, strikethrough, fontSize, scale,
                modernShadow, explicitShadowArgb,
                modernShadow ? ShadowRenderSpec.fromConfig() : null);
    }

    private CosmicRenderedText renderSingle(String text, int argb, boolean bold, boolean italic,
                                             boolean underline, boolean strikethrough,
                                             float fontSize, float scale, boolean modernShadow,
                                             Integer explicitShadowArgb, ShadowRenderSpec shadowSpec) {
        // Minecraft applies the caller alpha through vertex color during draw. Keeping the cached
        // raster opaque avoids multiplying that alpha a second time and also lets alpha variants
        // share the same native raster/GL texture.
        int rasterArgb = argb | 0xFF000000;
        int shadowProfile = modernShadow ? modernShadowProfile(shadowSpec, explicitShadowArgb) : 0;
        RenderKey key = new RenderKey(text, rasterArgb,
                effectiveFlags(bold, italic, underline, strikethrough),
                Float.floatToIntBits(fontSize), Float.floatToIntBits(scale), shadowProfile,
                NeofontrenderConfig.sdfEnabled(), NeofontrenderConfig.sdfDistanceRange(),
                Float.floatToIntBits(NeofontrenderConfig.sdfEdgeSoftness()));
        CosmicRenderedText cached = renderCache.get(key);
        if (cached != null) {
            renderCacheHits++;
            cached.touch();
            FontRenderDiagnostics.logCosmicRaster("cosmic.render.cache", text, rasterArgb,
                    key.flags, fontSize, scale, true,
                    Math.round(cached.visualRight() - cached.visualLeft()),
                    Math.round(cached.visualBottom() - cached.visualTop()), cached.advance());
            periodicCacheCleanup();
            return cached;
        }
        renderCacheMisses++;
        byte[] encoded = CosmicNative.renderSized(engine, text, rasterArgb, key.flags,
                fontSize, scale);
        nativeRasterCount++;
        CosmicRenderedText rendered = decode(encoded, text, modernShadow, fontSize, rasterArgb,
                explicitShadowArgb, shadowSpec);
        FontRenderDiagnostics.logCosmicRaster("cosmic.render.raster", text, rasterArgb,
                key.flags, fontSize, scale, false, widthOf(encoded), heightOf(encoded), rendered.advance());
        renderCache.put(key, rendered);
        trimRenderCache();
        periodicCacheCleanup();
        return rendered;
    }

    private static int widthOf(byte[] encoded) {
        return encoded == null || encoded.length < 8
                ? 0 : ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
    }

    private static int heightOf(byte[] encoded) {
        return encoded == null || encoded.length < 12
                ? 0 : ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN).getInt(8);
    }

    @Override
    public float measureFormatted(String text, int baseArgb, boolean shadow) {
        return measureFormattedAtSize(text, baseArgb, shadow, NeofontrenderConfig.fontSize());
    }

    @Override
    public float measureFormattedAtSize(String text, int baseArgb, boolean shadow,
                                        float requestedFontSize) {
        float width = 0.0F;
        float scale = Math.max(1.0F, FontRenderTuning.rasterScale(NeofontrenderConfig.fontOversample()));
        float fontSize = Math.max(1.0F, requestedFontSize);
        for (FormattedRun run : parseFormatted(text, baseArgb, shadow)) {
            for (String segment : renderingSegments(run.text, run.bold, run.italic, fontSize, scale)) {
                width += measureAtSize(segment, run.bold, run.italic, fontSize);
            }
        }
        return width;
    }

    @Override
    public TextRenderResult renderFormatted(String text, int baseArgb, boolean shadow) {
        return renderFormattedAtSize(text, baseArgb, shadow, NeofontrenderConfig.fontSize());
    }

    @Override
    public boolean supportsNativeFontSize() {
        return true;
    }

    @Override
    public TextRenderResult renderFormattedAtSize(String text, int baseArgb, boolean shadow,
                                                  float fontSize) {
        List<PositionedResult> results = new ArrayList<>();
        float x = 0.0F;
        float logicalSize = Math.max(1.0F, fontSize);
        float scale = Math.max(1.0F,
                FontRenderTuning.rasterScale(NeofontrenderConfig.fontOversample()));
        for (FormattedRun run : parseFormatted(text, baseArgb, shadow)) {
            TextRenderResult renderedRun = renderAtScale(run.text, run.argb, run.bold, run.italic,
                    run.underline, run.strikethrough, logicalSize, scale);
            results.add(new PositionedResult(x, renderedRun));
            x += renderedRun.advance();
        }
        return results.isEmpty() ? TextRenderResult.EMPTY : new CompositeResult(results, x);
    }

    @Override
    public boolean supportsModernShadow() {
        return true;
    }

    @Override
    public TextRenderResult renderFormattedWithShadow(String text, int baseArgb) {
        return renderFormattedWithShadowAtSize(
                text, baseArgb, NeofontrenderConfig.fontSize());
    }

    @Override
    public TextRenderResult renderFormattedWithShadowAtSize(
            String text, int baseArgb, float requestedFontSize) {
        return renderFormattedWithShadowAtSize(text, baseArgb, requestedFontSize,
                ShadowRenderSpec.fromConfig());
    }

    @Override
    public TextRenderResult renderFormattedWithShadowAtSize(
            String text, int baseArgb, float requestedFontSize, ShadowRenderSpec shadowSpec) {
        ShadowRenderSpec spec = shadowSpec == null ? ShadowRenderSpec.fromConfig() : shadowSpec;
        List<PositionedResult> results = new ArrayList<>();
        float x = 0.0F;
        float scale = Math.max(1.0F, FontRenderTuning.rasterScale(NeofontrenderConfig.fontOversample()));
        float fontSize = Math.max(1.0F, requestedFontSize);
        List<FormattedRun> foregroundRuns = parseFormatted(text, baseArgb, false, spec);
        List<FormattedRun> shadowRuns = parseFormatted(text, baseArgb, true, spec);
        for (int index = 0; index < foregroundRuns.size(); index++) {
            FormattedRun run = foregroundRuns.get(index);
            FormattedRun shadowRun = shadowRuns.get(index);
            for (String segment : renderingSegments(run.text, run.bold, run.italic, fontSize, scale)) {
                TextRenderResult renderedRun = renderSingle(segment, run.argb, run.bold, run.italic,
                        run.underline, run.strikethrough, fontSize, scale, true,
                        shadowRun.argb, spec);
                results.add(new PositionedResult(x, renderedRun));
                x += renderedRun.advance();
            }
        }
        return results.isEmpty() ? TextRenderResult.EMPTY : new CompositeResult(results, x);
    }

    private List<String> renderingSegments(String text, boolean bold, boolean italic,
                                           float fontSize, float scale) {
        return CosmicTextSegmenter.split(text, SEGMENT_RASTER_ADVANCE_LIMIT,
                segment -> measureAtSize(segment, bold, italic, fontSize) * scale);
    }

    private TextRenderResult renderSegments(List<String> segments, int argb,
                                            boolean bold, boolean italic,
                                            boolean underline, boolean strikethrough,
                                            float fontSize, float scale) {
        List<PositionedResult> results = new ArrayList<>(segments.size());
        float x = 0.0F;
        for (String segment : segments) {
            TextRenderResult rendered = renderAtScale(segment, argb, bold, italic,
                    underline, strikethrough, fontSize, scale);
            results.add(new PositionedResult(x, rendered));
            x += rendered.advance();
        }
        return new CompositeResult(results, x);
    }

    private static boolean isRasterSizeError(IllegalStateException error) {
        return error.getMessage() != null
                && error.getMessage().startsWith("invalid cosmic-text raster size ");
    }

    @Override
    public void prewarmBasicLatin() {
        measure("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789", false, false);
    }

    private CosmicRenderedText decode(byte[] encoded, String diagnosticText,
                                      boolean modernShadow, float fontSize,
                                      int foregroundArgb, Integer explicitShadowArgb,
                                      ShadowRenderSpec shadowSpec) {
        ShadowRenderSpec spec = shadowSpec == null ? ShadowRenderSpec.fromConfig() : shadowSpec;
        if (encoded == null || encoded.length < 36) {
            throw new IllegalStateException("cosmic-text returned a truncated raster");
        }
        ByteBuffer data = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
        if (data.getInt() != RASTER_MAGIC) {
            throw new IllegalStateException("cosmic-text returned an invalid raster header");
        }
        int width = data.getInt();
        int height = data.getInt();
        int offsetX = data.getInt();
        int offsetY = data.getInt();
        float advance = data.getFloat();
        float baseline = data.getFloat();
        float scale = data.getFloat();
        int modelFlags = data.getInt();
        long pixelCount = (long) width * height;
        if (width < 0 || height < 0 || pixelCount > Integer.MAX_VALUE || data.remaining() != pixelCount * 4L) {
            // Validate all native dimensions before allocation. A mismatched DLL should degrade to
            // a backend error instead of causing an uncontrolled Java heap allocation.
            throw new IllegalStateException("cosmic-text returned invalid dimensions " + width + "x" + height);
        }
        if (width == 0 || height == 0) {
            return CosmicRenderedText.empty(advance, scale);
        }
        int[] pixels = new int[(int) pixelCount];
        // OptiFine can expose base, normal, and specular layers in one 3x-sized array. The native
        // payload contains only the base layer, so never use the target array length as the number
        // of encoded pixels to consume.
        CosmicRasterPixels.copyBaseLayer(data, (int) pixelCount, pixels);
        int[] foregroundPixels = pixels;
        float baseSize = Math.max(1.0F, NeofontrenderConfig.fontSize());
        float sizeRatio = Math.max(1.0F, fontSize) / baseSize;
        float baseOffsetX = offsetX / scale;
        float baseOffsetY = offsetY / scale + NeofontrenderConfig.fontReferenceBaseline() * sizeRatio
                + NeofontrenderConfig.fontBaselineShift() * sizeRatio - baseline;
        boolean sdf = NeofontrenderConfig.sdfEnabled()
                && (modelFlags & CosmicNative.RASTER_MODEL_GRADIENT_COLOR) == 0
                && (modelFlags & (CosmicNative.RASTER_MODEL_MASK | CosmicNative.RASTER_MODEL_FLAT_COLOR)) != 0
                && CosmicSdfPipeline.isAvailable();
        if (sdf) {
            UploadedTexture foreground = uploadSdfTexture(
                    CosmicSdfGenerator.generate(foregroundPixels, width, height,
                            NeofontrenderConfig.sdfDistanceRange()),
                    width, height, scale);
            UploadedTexture shadowTexture = null;
            float shadowWidth = 0.0F;
            float shadowHeight = 0.0F;
            float shadowOffsetX = baseOffsetX;
            float shadowOffsetY = baseOffsetY;
            if (modernShadow) {
                float shadowGeometryScale = Math.max(1.0F, fontSize)
                        / Math.max(1.0F, NeofontrenderConfig.fontSize());
                ModernShadowRasterizer.Result shadow = ModernShadowRasterizer.shadow(
                        foregroundPixels, width, height, scale,
                        spec.offsetX * shadowGeometryScale,
                        spec.offsetY * shadowGeometryScale,
                        spec.blurRadius * shadowGeometryScale,
                        explicitShadowArgb != null ? explicitShadowArgb
                                : ShadowColorPolicy.modernColor(
                                        foregroundArgb, spec.color, spec.colorMode,
                                        spec.colorOverrides, legacyColorCodes,
                                        spec.coloredRatio, spec.coloredFunction),
                        spec.opacity, false);
                shadowTexture = uploadRgbaTexture(shadow.pixels, shadow.width, shadow.height, scale);
                shadowWidth = shadow.width / scale;
                shadowHeight = shadow.height / scale;
                shadowOffsetX = baseOffsetX - shadow.originX / scale;
                shadowOffsetY = baseOffsetY - shadow.originY / scale;
            }
            return new CosmicRenderedText(diagnosticText, foreground.location, foreground.texture,
                    shadowTexture == null ? null : shadowTexture.location,
                    shadowTexture == null ? null : shadowTexture.texture,
                    advance, width / scale, height / scale, baseOffsetX, baseOffsetY, scale,
                    true, foregroundArgb, shadowWidth, shadowHeight, shadowOffsetX, shadowOffsetY);
        }

        if (modernShadow) {
            // Keep the foreground anchored to the native glyph bearing. Compositing it into the
            // expanded blur texture makes the whole title inherit the blur origin and can turn
            // tiny raster-scale changes into visible horizontal motion.
            float shadowGeometryScale = Math.max(1.0F, fontSize)
                    / Math.max(1.0F, NeofontrenderConfig.fontSize());
            ModernShadowRasterizer.Result shadow = ModernShadowRasterizer.shadow(
                    foregroundPixels, width, height, scale,
                    spec.offsetX * shadowGeometryScale,
                    spec.offsetY * shadowGeometryScale,
                    spec.blurRadius * shadowGeometryScale,
                    explicitShadowArgb != null ? explicitShadowArgb
                            : ShadowColorPolicy.modernColor(
                                        foregroundArgb, spec.color, spec.colorMode,
                                        spec.colorOverrides, legacyColorCodes,
                                        spec.coloredRatio, spec.coloredFunction),
                    spec.opacity, false);
            UploadedTexture foreground = uploadRgbaTexture(foregroundPixels, width, height, scale);
            UploadedTexture shadowTexture = uploadRgbaTexture(shadow.pixels,
                    shadow.width, shadow.height, scale);
            return new CosmicRenderedText(diagnosticText, foreground.location, foreground.texture,
                    shadowTexture.location, shadowTexture.texture,
                    advance, width / scale, height / scale, baseOffsetX, baseOffsetY, scale,
                    false, foregroundArgb, shadow.width / scale, shadow.height / scale,
                    baseOffsetX - shadow.originX / scale,
                    baseOffsetY - shadow.originY / scale);
        }
        UploadedTexture uploaded = uploadRgbaTexture(foregroundPixels, width, height, scale);
        return new CosmicRenderedText(diagnosticText, uploaded.location, uploaded.texture, null, null,
                advance, width / scale, height / scale,
                baseOffsetX, baseOffsetY,
                scale, false, foregroundArgb, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    private UploadedTexture uploadRgbaTexture(int[] pixels, int width, int height, float scale) {
        boolean hdrTexture = KirinoHdrCompat.useHdrTexture();
        AbstractTexture texture;
        if (hdrTexture) {
            texture = new CosmicFloatTexture(pixels, width, height);
        } else {
            int[] premultiplied = new int[pixels.length];
            for (int i = 0; i < pixels.length; i++) premultiplied[i] = premultiply(pixels[i]);
            DynamicTexture legacyTexture = new DynamicTexture(width, height);
            int[] target = legacyTexture.getTextureData();
            System.arraycopy(premultiplied, 0, target, 0, Math.min(premultiplied.length, target.length));
            legacyTexture.updateDynamicTexture();
            texture = legacyTexture;
        }
        FontRenderTuning.applyFontTextureFilter(texture, scale, false);
        ResourceLocation location = new ResourceLocation("neofontrender", "cosmic/" + nextTextureId++);
        textureManager.loadTexture(location, texture);
        FontRenderTuning.applyFontTextureFilter(texture, scale, false);
        return new UploadedTexture(location, texture);
    }

    private UploadedTexture uploadSdfTexture(byte[] pixels, int width, int height, float scale) {
        CosmicSdfTexture texture = new CosmicSdfTexture(pixels, width, height);
        ResourceLocation location = new ResourceLocation("neofontrender", "cosmic/sdf/" + nextTextureId++);
        textureManager.loadTexture(location, texture);
        return new UploadedTexture(location, texture);
    }

    private static int modernShadowProfile(ShadowRenderSpec spec) {
        return (spec == null ? ShadowRenderSpec.fromConfig() : spec).profileHash();
    }

    private static int modernShadowProfile(ShadowRenderSpec spec, Integer explicitShadowArgb) {
        int hash = modernShadowProfile(spec);
        return explicitShadowArgb == null ? hash : 31 * hash + explicitShadowArgb;
    }

    private static int premultiply(int pixel) {
        int alpha = pixel >>> 24;
        int red = ((pixel >>> 16) & 0xFF) * alpha / 255;
        int green = ((pixel >>> 8) & 0xFF) * alpha / 255;
        int blue = (pixel & 0xFF) * alpha / 255;
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private List<LoadedFont> loadConfiguredFonts(IResourceManager resourceManager) throws IOException {
        LinkedHashMap<String, String> selectors = new LinkedHashMap<>();
        selectors.put(NeofontrenderConfig.fontName(), NeofontrenderConfig.primaryFontLocation());
        registerFamilySources(selectors, NeofontrenderConfig.fontName(),
                localFamilyLocations(NeofontrenderConfig.fontName()));
        for (String name : NeofontrenderConfig.cosmicFaceOverrides()) {
            if (name != null && !name.trim().isEmpty()) {
                selectors.putIfAbsent(name, name);
            }
        }
        for (String name : NeofontrenderConfig.fontFamily()) {
            if (!name.equals(NeofontrenderConfig.primaryFontLocation())) {
                // Local fallback selections are stored as family names. Register every face in
                // that family so cosmic-text can choose real bold/italic variants for fallback
                // glyphs instead of being limited to whichever file sorts first.
                registerFamilySources(selectors, name, localFamilyLocations(name));
                selectors.putIfAbsent(name, name);
            }
        }

        List<LoadedFont> fonts = new ArrayList<>();
        for (Map.Entry<String, String> selector : selectors.entrySet()) {
            String alias = selector.getKey();
            String source = selector.getValue();
            if (source == null || source.trim().isEmpty()) {
                continue;
            }
            try {
                File file = NeofontrenderConfig.resolveFontFile(source);
                if (file.isFile()) {
                    try (InputStream input = new FileInputStream(file)) {
                        fonts.add(new LoadedFont(alias, readAllBytes(input)));
                    }
                } else if (source.indexOf(':') >= 0) {
                    IResource resource = resourceManager.getResource(new ResourceLocation(source));
                    try (InputStream input = resource.getInputStream()) {
                        fonts.add(new LoadedFont(alias, readAllBytes(input)));
                    }
                }
            } catch (IOException error) {
                // Core intentionally omits bundled TTF resources. A configured resource from
                // the full/resources package must not make Cosmic fail completely: the native
                // engine can resolve the configured family and emoji through the OS font DB.
                NeoFontRender.LOGGER.warn("Skipped unavailable Cosmic font resource '{}'", source);
            }
        }
        return fonts;
    }

    private static List<String> localFamilyLocations(String family) {
        List<String> locations = new ArrayList<>();
        for (File file : NeofontrenderConfig.fontFamilyFiles(family)) {
            locations.add(NeofontrenderConfig.portableFontLocation(file));
        }
        return locations;
    }

    static void registerFamilySources(LinkedHashMap<String, String> selectors, String family,
                                      Iterable<String> locations) {
        if (family == null || family.trim().isEmpty() || locations == null) {
            return;
        }
        for (String location : locations) {
            if (location == null || location.trim().isEmpty()) {
                continue;
            }
            // Keep the family alias on one source so native resolution can find it before the
            // catalog is built. Every additional face gets a unique source alias while retaining
            // its real internal family metadata for weight/style matching.
            selectors.putIfAbsent(family, location);
            if (!selectors.containsValue(location)) {
                selectors.put(location, location);
            }
        }
    }

    private List<LoadedFont> loadScopedFonts(IResourceManager resourceManager,
                                             List<String> selectors) throws IOException {
        List<LoadedFont> fonts = new ArrayList<>();
        for (String selector : selectors) {
            try {
                File file = NeofontrenderConfig.resolveFontFile(selector);
                if (file.isFile()) {
                    try (InputStream input = new FileInputStream(file)) {
                        fonts.add(new LoadedFont(selector, readAllBytes(input)));
                    }
                } else if (selector.indexOf(':') >= 0) {
                    IResource resource = resourceManager.getResource(new ResourceLocation(selector));
                    try (InputStream input = resource.getInputStream()) {
                        fonts.add(new LoadedFont(selector, readAllBytes(input)));
                    }
                }
            } catch (IOException error) {
                NeoFontRender.LOGGER.warn("Scoped Cosmic renderer skipped unavailable font '{}'", selector);
            }
        }
        return fonts;
    }

    private static final class LoadedFont {
        private final String alias;
        private final byte[] data;

        private LoadedFont(String alias, byte[] data) {
            this.alias = alias;
            this.data = data;
        }
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] block = new byte[8192];
        int read;
        while ((read = input.read(block)) >= 0) {
            output.write(block, 0, read);
        }
        return output.toByteArray();
    }

    private List<FormattedRun> parseFormatted(String text, int baseArgb, boolean shadow) {
        return parseFormatted(text, baseArgb, shadow,
                shadow ? ShadowRenderSpec.fromConfig() : null);
    }

    private List<FormattedRun> parseFormatted(String text, int baseArgb, boolean shadow,
                                               ShadowRenderSpec shadowSpec) {
        List<FormattedRun> runs = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return runs;
        }
        int[] colorCodes = legacyColorCodes;
        ShadowRenderSpec spec = shadowSpec == null ? ShadowRenderSpec.fromConfig() : shadowSpec;
        String colorMode = spec.colorMode;
        ShadowColorRemapRules remapRules = spec.colorOverrides;
        int configuredShadowColor = spec.color;
        int color = shadow
                ? ShadowColorPolicy.shadowColor(normalizeAlpha(baseArgb), colorMode,
                        configuredShadowColor, remapRules, colorCodes,
                        spec.coloredRatio, spec.coloredFunction)
                : normalizeAlpha(baseArgb);
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean strikethrough = false;
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != '\u00a7' || i + 1 >= text.length()) {
                continue;
            }
            if (i > start) {
                runs.add(new FormattedRun(text.substring(start, i), color, bold, italic,
                        underline, strikethrough));
            }
            char code = Character.toLowerCase(text.charAt(++i));
            int colorIndex = "0123456789abcdef".indexOf(code);
            if (colorIndex >= 0) {
                color = ShadowColorPolicy.paletteColor(colorIndex,
                        normalizeAlpha(baseArgb), shadow, colorMode, configuredShadowColor,
                        remapRules, colorCodes, spec.coloredRatio, spec.coloredFunction);
                bold = italic = underline = strikethrough = false;
            } else if (code == 'l') {
                bold = true;
            } else if (code == 'm') {
                strikethrough = true;
            } else if (code == 'n') {
                underline = true;
            } else if (code == 'o') {
                italic = true;
            } else if (code == 'r') {
                color = shadow
                        ? ShadowColorPolicy.shadowColor(normalizeAlpha(baseArgb), colorMode,
                                configuredShadowColor, remapRules, colorCodes,
                                spec.coloredRatio, spec.coloredFunction)
                        : normalizeAlpha(baseArgb);
                bold = italic = underline = strikethrough = false;
            }
            start = i + 1;
        }
        if (start < text.length()) {
            runs.add(new FormattedRun(text.substring(start), color, bold, italic,
                    underline, strikethrough));
        }
        return runs;
    }

    private static int normalizeAlpha(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }

    private static int effectiveFlags(boolean bold, boolean italic) {
        return effectiveFlags(bold, italic, false, false);
    }

    static int effectiveFlags(boolean bold, boolean italic,
                              boolean underline, boolean strikethrough) {
        return composeStyleFlags(NeofontrenderConfig.fontStyle(), bold, italic,
                underline, strikethrough);
    }

    static int composeStyleFlags(int configuredStyle, boolean bold, boolean italic,
                                 boolean underline, boolean strikethrough) {
        return (bold || (configuredStyle & 1) != 0 ? 1 : 0)
                | (italic || (configuredStyle & 2) != 0 ? 2 : 0)
                | (underline ? 4 : 0)
                | (strikethrough ? 8 : 0);
    }

    private static boolean containsEmoji(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int index = 0; index < text.length(); ) {
            int codePoint = text.codePointAt(index);
            if ((codePoint >= 0x1F000 && codePoint <= 0x1FAFF)
                    || (codePoint >= 0x2600 && codePoint <= 0x27BF)
                    || codePoint == 0xFE0F) {
                return true;
            }
            index += Character.charCount(codePoint);
        }
        return false;
    }

    private void trimRenderCache() {
        int max = Math.max(1, NeofontrenderConfig.textCacheMaxEntries());
        Iterator<Map.Entry<RenderKey, CosmicRenderedText>> iterator = renderCache.entrySet().iterator();
        while (renderCache.size() > max && iterator.hasNext()) {
            Map.Entry<RenderKey, CosmicRenderedText> eldest = iterator.next();
            eldest.getValue().close();
            iterator.remove();
            renderCacheEvictions++;
        }

        long ttlMillis = (long) (NeofontrenderConfig.textCacheTtlSeconds() * 1000.0F);
        if (ttlMillis <= 0L) {
            return;
        }
        int min = Math.max(0, Math.min(max, NeofontrenderConfig.textCacheMinEntries()));
        long now = System.currentTimeMillis();
        iterator = renderCache.entrySet().iterator();
        while (renderCache.size() > min && iterator.hasNext()) {
            Map.Entry<RenderKey, CosmicRenderedText> eldest = iterator.next();
            if (!eldest.getValue().isExpired(now, ttlMillis)) {
                break;
            }
            eldest.getValue().close();
            iterator.remove();
            renderCacheEvictions++;
        }
    }

    private void trimMeasureCache() {
        int max = Math.max(1, NeofontrenderConfig.measureCacheMaxEntries());
        Iterator<MeasureKey> iterator = measureCache.keySet().iterator();
        while (measureCache.size() > max && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
            measureCacheEvictions++;
        }
    }

    private void periodicCacheCleanup() {
        // TTL must also progress during a stable screen where every draw is a cache hit. Checking
        // once per 256 operations keeps that guarantee without walking the LRU on every frame.
        if ((++cacheOperations & 255L) == 0L) {
            trimRenderCache();
            trimMeasureCache();
        }
    }

    public synchronized DebugState debugState() {
        return new DebugState(primaryFamily, renderCache.size(), NeofontrenderConfig.textCacheMaxEntries(),
                measureCache.size(), NeofontrenderConfig.measureCacheMaxEntries(),
                renderCacheHits, renderCacheMisses, renderCacheEvictions,
                measureCacheHits, measureCacheMisses, measureCacheEvictions, nativeRasterCount);
    }

    @Override
    public synchronized void close() {
        for (CosmicRenderedText rendered : renderCache.values()) {
            rendered.close();
        }
        renderCache.clear();
        measureCache.clear();
        if (engine != 0L) {
            CosmicNative.destroyEngine(engine);
            engine = 0L;
        }
    }

    private static final class UploadedTexture {
        private final ResourceLocation location;
        private final AbstractTexture texture;

        private UploadedTexture(ResourceLocation location, AbstractTexture texture) {
            this.location = location;
            this.texture = texture;
        }
    }

    private static final class CosmicRenderedText implements TextRenderResult, AutoCloseable {
        private final String diagnosticText;
        private final ResourceLocation location;
        private final AbstractTexture texture;
        private final ResourceLocation shadowLocation;
        private final AbstractTexture shadowTexture;
        private final float advance;
        private final float width;
        private final float height;
        private final float offsetX;
        private final float offsetY;
        private final float scale;
        private final boolean sdf;
        private final int sdfArgb;
        private final float shadowWidth;
        private final float shadowHeight;
        private final float shadowOffsetX;
        private final float shadowOffsetY;
        private final AtomicBoolean closed = new AtomicBoolean();
        private volatile long lastAccessMillis;

        private CosmicRenderedText(String diagnosticText, ResourceLocation location, AbstractTexture texture,
                                   ResourceLocation shadowLocation, AbstractTexture shadowTexture,
                                   float advance, float width, float height, float offsetX,
                                   float offsetY, float scale, boolean sdf, int sdfArgb,
                                   float shadowWidth, float shadowHeight, float shadowOffsetX,
                                   float shadowOffsetY) {
            this.diagnosticText = diagnosticText;
            this.location = location;
            this.texture = texture;
            this.shadowLocation = shadowLocation;
            this.shadowTexture = shadowTexture;
            this.advance = advance;
            this.width = width;
            this.height = height;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.scale = scale;
            this.sdf = sdf;
            this.sdfArgb = sdfArgb;
            this.shadowWidth = shadowWidth;
            this.shadowHeight = shadowHeight;
            this.shadowOffsetX = shadowOffsetX;
            this.shadowOffsetY = shadowOffsetY;
            this.lastAccessMillis = System.currentTimeMillis();
        }

        private static CosmicRenderedText empty(float advance, float scale) {
            return new CosmicRenderedText(null, null, null, null, null, advance, 0.0F, 0.0F,
                    0.0F, 0.0F, scale, false, 0, 0.0F, 0.0F, 0.0F, 0.0F);
        }

        @Override
        public float advance() {
            return advance;
        }

        @Override public float visualLeft() { return shadowTexture == null ? offsetX : Math.min(offsetX, shadowOffsetX); }
        @Override public float visualRight() {
            return shadowTexture == null ? offsetX + width
                    : Math.max(offsetX + width, shadowOffsetX + shadowWidth);
        }
        @Override public float visualTop() { return shadowTexture == null ? offsetY : Math.min(offsetY, shadowOffsetY); }
        @Override public float visualBottom() {
            return shadowTexture == null ? offsetY + height
                    : Math.max(offsetY + height, shadowOffsetY + shadowHeight);
        }

        private void touch() {
            lastAccessMillis = System.currentTimeMillis();
        }

        private boolean isExpired(long now, long ttlMillis) {
            return now - lastAccessMillis >= ttlMillis;
        }

        @Override
        public void draw(float x, float y, float alpha) {
            if (closed.get() || location == null || texture == null
                    || width <= 0.0F || height <= 0.0F) {
                return;
            }
            float tint = premultipliedOpacity(alpha);
            FontRenderDiagnostics.logCosmicDraw("cosmic.draw.before", diagnosticText,
                    x, y, width, height, offsetX, offsetY, scale);
            float left = FontRenderTuning.alignToPixel(x + offsetX);
            float top = FontRenderTuning.alignToPixel(y + offsetY);
            if (sdf) {
                if (shadowTexture != null && shadowLocation != null
                        && shadowWidth > 0.0F && shadowHeight > 0.0F) {
                    float shadowLeft = FontRenderTuning.alignToPixel(x + shadowOffsetX);
                    float shadowTop = FontRenderTuning.alignToPixel(y + shadowOffsetY);
                    try (PremultipliedBlendState ignored = new PremultipliedBlendState()) {
                        drawRgba(shadowLocation, shadowTexture, shadowLeft, shadowTop,
                                shadowWidth, shadowHeight, tint);
                    }
                }
                try (CosmicSdfPipeline.State ignored = CosmicSdfPipeline.begin()) {
                    if (ignored.isNoop()) return;
                    FontRenderDiagnostics.logCosmicDraw("cosmic.draw.prepared", diagnosticText,
                            x, y, width, height, offsetX, offsetY, scale);
                    Minecraft.getMinecraft().getTextureManager().bindTexture(location);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                    float red = ((sdfArgb >>> 16) & 0xFF) / 255.0F;
                    float green = ((sdfArgb >>> 8) & 0xFF) / 255.0F;
                    float blue = (sdfArgb & 0xFF) / 255.0F;
                    CosmicSdfPipeline.draw(left, top, width, height, red, green, blue, tint,
                            NeofontrenderConfig.sdfEdgeSoftness());
                }
                return;
            }
            if (shadowTexture != null && shadowLocation != null
                    && shadowWidth > 0.0F && shadowHeight > 0.0F) {
                float shadowLeft = FontRenderTuning.alignToPixel(x + shadowOffsetX);
                float shadowTop = FontRenderTuning.alignToPixel(y + shadowOffsetY);
                try (PremultipliedBlendState ignored = new PremultipliedBlendState()) {
                    drawRgba(shadowLocation, shadowTexture, shadowLeft, shadowTop,
                            shadowWidth, shadowHeight, tint);
                }
            }
            // Cosmic uploaders provide premultiplied textures (RGBA8 or RGBA16F). Force the matching
            // blend function because surrounding mods frequently leave Minecraft's cached blend
            // state configured for straight-alpha GUI textures.
            try (PremultipliedBlendState ignored = new PremultipliedBlendState()) {
                FontRenderDiagnostics.logCosmicDraw("cosmic.draw.prepared", diagnosticText,
                        x, y, width, height, offsetX, offsetY, scale);
                drawRgba(location, texture, left, top, width, height, tint);
            }
        }

        private void drawRgba(ResourceLocation drawLocation, AbstractTexture drawTexture,
                              float left, float top, float drawWidth, float drawHeight,
                              float tint) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(drawLocation);
            FontRenderTuning.applyBoundTextureFilter(scale, false);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GlStateManager.color(tint, tint, tint, tint);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            buffer.pos(left, top, 0).tex(0, 0).color(tint, tint, tint, tint).endVertex();
            buffer.pos(left, top + drawHeight, 0).tex(0, 1).color(tint, tint, tint, tint).endVertex();
            buffer.pos(left + drawWidth, top + drawHeight, 0).tex(1, 1).color(tint, tint, tint, tint).endVertex();
            buffer.pos(left + drawWidth, top, 0).tex(1, 0).color(tint, tint, tint, tint).endVertex();
            tessellator.draw();
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                if (location != null) ClientTextureDisposal.delete(location);
                if (shadowLocation != null) ClientTextureDisposal.delete(shadowLocation);
            }
        }
    }

    static float premultipliedOpacity(float alpha) {
        return Float.isFinite(alpha) ? Math.max(0.0F, Math.min(1.0F, alpha)) : 0.0F;
    }

    public static final class DebugState {
        public final String primaryFamily;
        public final int renderCacheSize;
        public final int renderCacheMax;
        public final int measureCacheSize;
        public final int measureCacheMax;
        public final long renderHits;
        public final long renderMisses;
        public final long renderEvictions;
        public final long measureHits;
        public final long measureMisses;
        public final long measureEvictions;
        public final long nativeRasterCount;

        private DebugState(String primaryFamily, int renderCacheSize, int renderCacheMax,
                           int measureCacheSize, int measureCacheMax,
                           long renderHits, long renderMisses, long renderEvictions,
                           long measureHits, long measureMisses, long measureEvictions,
                           long nativeRasterCount) {
            this.primaryFamily = primaryFamily;
            this.renderCacheSize = renderCacheSize;
            this.renderCacheMax = renderCacheMax;
            this.measureCacheSize = measureCacheSize;
            this.measureCacheMax = measureCacheMax;
            this.renderHits = renderHits;
            this.renderMisses = renderMisses;
            this.renderEvictions = renderEvictions;
            this.measureHits = measureHits;
            this.measureMisses = measureMisses;
            this.measureEvictions = measureEvictions;
            this.nativeRasterCount = nativeRasterCount;
        }
    }

    private static final class PremultipliedBlendState implements AutoCloseable {
        private final boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        private final boolean alphaTestEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        private final boolean fogEnabled = GL11.glIsEnabled(GL11.GL_FOG);
        private final boolean textureEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        private final int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        private final int dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        private final int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        private final int dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        private final int blendEquationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
        private final int blendEquationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        private final int textureBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        private final boolean[] colorMask = readColorMask();
        private final float[] color = readColor();

        private PremultipliedBlendState() {
            GlStateManager.enableTexture2D();
            GlStateManager.disableAlpha();
            // Fixed-function fog adds fog RGB without scaling it by glyph coverage. That breaks the
            // premultiplied invariant at antialiased edges and GL_ONE then exposes it as a halo.
            GlStateManager.disableFog();
            GL11.glDisable(GL11.GL_FOG);
            GlStateManager.enableBlend();
            // TC6 and some legacy renderers toggle blending through raw GL11 calls, leaving
            // GlStateManager's cached flag out of sync with the driver. Reassert the real state
            // after updating Minecraft's cache so premultiplied glyph textures always blend.
            GL11.glEnable(GL11.GL_BLEND);
            GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD);
            GlStateManager.tryBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            // Mods sometimes mutate the driver through raw GL and leave GlStateManager's cache
            // stale. Reassert the factors in GL after synchronizing Minecraft's cache above.
            GL14.glBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }

        @Override
        public void close() {
            // Restore via GlStateManager so its 1.12-era state cache stays synchronized with GL.
            GL20.glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha);
            GlStateManager.tryBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            if (!blendEnabled) {
                GlStateManager.disableBlend();
                GL11.glDisable(GL11.GL_BLEND);
            } else {
                GlStateManager.enableBlend();
                GL11.glEnable(GL11.GL_BLEND);
            }
            if (alphaTestEnabled) GlStateManager.enableAlpha();
            else GlStateManager.disableAlpha();
            if (fogEnabled) GlStateManager.enableFog();
            else GlStateManager.disableFog();
            if (textureEnabled) GlStateManager.enableTexture2D();
            else GlStateManager.disableTexture2D();
            GlStateManager.bindTexture(textureBinding);
            GL11.glColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
            GlStateManager.color(color[0], color[1], color[2], color[3]);
        }

        private static boolean[] readColorMask() {
            ByteBuffer mask = BufferUtils.createByteBuffer(4);
            GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK, mask);
            return new boolean[]{mask.get(0) != 0, mask.get(1) != 0, mask.get(2) != 0, mask.get(3) != 0};
        }

        private static float[] readColor() {
            FloatBuffer value = BufferUtils.createFloatBuffer(4);
            GL11.glGetFloat(GL11.GL_CURRENT_COLOR, value);
            return new float[]{value.get(0), value.get(1), value.get(2), value.get(3)};
        }
    }

    private static final class CompositeResult implements TextRenderResult {
        private final List<PositionedResult> parts;
        private final float advance;

        private CompositeResult(List<PositionedResult> parts, float advance) {
            this.parts = parts;
            this.advance = advance;
        }

        @Override
        public float advance() {
            return advance;
        }

        @Override
        public float visualLeft() {
            float left = 0.0F;
            for (PositionedResult part : parts) left = Math.min(left, part.x + part.result.visualLeft());
            return left;
        }

        @Override
        public float visualRight() {
            float right = advance;
            for (PositionedResult part : parts) right = Math.max(right, part.x + part.result.visualRight());
            return right;
        }

        @Override
        public float visualTop() {
            float top = 0.0F;
            for (PositionedResult part : parts) top = Math.min(top, part.result.visualTop());
            return top;
        }

        @Override
        public float visualBottom() {
            float bottom = 8.0F;
            for (PositionedResult part : parts) bottom = Math.max(bottom, part.result.visualBottom());
            return bottom;
        }

        @Override
        public void draw(float x, float y, float alpha) {
            for (PositionedResult part : parts) {
                part.result.draw(x + part.x, y, alpha);
            }
        }
    }

    private static final class PositionedResult {
        private final float x;
        private final TextRenderResult result;

        private PositionedResult(float x, TextRenderResult result) {
            this.x = x;
            this.result = result;
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
        private final int flags;
        private final int fontSize;
        private final int scale;
        private final int shadowProfile;
        private final boolean sdf;
        private final int sdfDistanceRange;
        private final int sdfEdgeSoftness;

        private RenderKey(String text, int argb, int flags, int fontSize,
                          int scale, int shadowProfile, boolean sdf, int sdfDistanceRange,
                          int sdfEdgeSoftness) {
            this.text = text;
            this.argb = argb;
            this.flags = flags;
            this.fontSize = fontSize;
            this.scale = scale;
            this.shadowProfile = shadowProfile;
            this.sdf = sdf;
            this.sdfDistanceRange = sdfDistanceRange;
            this.sdfEdgeSoftness = sdfEdgeSoftness;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof RenderKey)) return false;
            RenderKey other = (RenderKey) object;
            return argb == other.argb && flags == other.flags && fontSize == other.fontSize
                    && scale == other.scale
                    && shadowProfile == other.shadowProfile && sdf == other.sdf
                    && sdfDistanceRange == other.sdfDistanceRange
                    && sdfEdgeSoftness == other.sdfEdgeSoftness
                    && text.equals(other.text);
        }

        @Override
        public int hashCode() {
            int hash = text.hashCode();
            hash = 31 * hash + argb;
            hash = 31 * hash + flags;
            hash = 31 * hash + fontSize;
            hash = 31 * hash + scale;
            hash = 31 * hash + shadowProfile;
            hash = 31 * hash + (sdf ? 1 : 0);
            hash = 31 * hash + sdfDistanceRange;
            return 31 * hash + sdfEdgeSoftness;
        }
    }

    private static final class MeasureKey {
        private final String text;
        private final int flags;
        private final int fontSize;

        private MeasureKey(String text, int flags, int fontSize) {
            this.text = text;
            this.flags = flags;
            this.fontSize = fontSize;
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof MeasureKey && flags == ((MeasureKey) object).flags
                    && fontSize == ((MeasureKey) object).fontSize
                    && text.equals(((MeasureKey) object).text);
        }

        @Override
        public int hashCode() {
            int hash = 31 * text.hashCode() + flags;
            return 31 * hash + fontSize;
        }
    }
}
