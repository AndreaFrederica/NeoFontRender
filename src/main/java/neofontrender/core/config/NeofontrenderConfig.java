package neofontrender.core.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.minecraft.client.Minecraft;
import neofontrender.NeoFontRender;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * TOML-based configuration manager using NightConfig.
 */
public final class NeofontrenderConfig {

    private static final String CONFIG_NAME = "neofontrender.toml";
    private static final String DEFAULT_FONT = "neofontrender:fonts/sarasa_ui_sc_regular.ttf";
    private static Path configPath;
    private static CommentedFileConfig config;
    private static volatile boolean cachedDebugRenderStats;
    private static final List<BuiltinFont> BUILTIN_FONTS = Collections.unmodifiableList(Arrays.asList(
            new BuiltinFont("Sarasa UI SC", DEFAULT_FONT),
            new BuiltinFont("Noto Color Emoji", "neofontrender:fonts/noto_color_emoji_regular.ttf")
    ));

    public static boolean isLoaded() {
        return config != null;
    }

    // ===================== Font =====================
    public static String fontName() {
        return normalizeFontLocation(config.getOrElse("font.name", DEFAULT_FONT));
    }

    public static List<String> fontFamily() {
        Set<String> fonts = new LinkedHashSet<>();
        addFontNames(fonts, fontName());
        fonts.addAll(fontFallbacks());
        if (builtinFallbacksEnabled()) {
            for (BuiltinFont font : builtinFonts()) {
                fonts.add(font.location);
            }
        }
        if (fonts.isEmpty()) {
            fonts.add(DEFAULT_FONT);
        }
        return Collections.unmodifiableList(new ArrayList<>(fonts));
    }

    public static List<String> fontFallbacks() {
        Set<String> fonts = new LinkedHashSet<>();
        Object fallbackValue = config.get("font.fallbacks");
        if (fallbackValue instanceof List) {
            for (Object value : (List<?>) fallbackValue) {
                if (value != null) {
                    addFontNames(fonts, value.toString());
                }
            }
        } else if (fallbackValue != null) {
            addFontNames(fonts, fallbackValue.toString());
        }
        return Collections.unmodifiableList(new ArrayList<>(fonts));
    }

    public static int fontStyle() {
        return config.getOrElse("font.style", 0);
    }

    public static float fontSize() {
        return getFloat("font.size", 10.0f);
    }

    public static float fontOversample() {
        return getFloat("font.oversample", 12.0f);
    }

    public static boolean fontAutoBaseline() {
        return config.getOrElse("font.autoBaseline", true);
    }

    public static float fontBaselineShift() {
        return getFloat("font.baselineShift", 0.0f);
    }

    public static float fontReferenceBaseline() {
        return getFloat("font.referenceBaseline", 7.0f);
    }

    public static boolean fontAntialias() {
        return config.getOrElse("font.antialias", true);
    }

    public static String fontAntialiasMode() {
        String mode = config.getOrElse("font.antialiasMode", fontAntialias() ? "on" : "off");
        return normalizeAntialiasMode(mode);
    }

    public static boolean fontFractionalMetrics() {
        return config.getOrElse("font.fractionalMetrics", true);
    }

    public static boolean fontLcdSubpixel() {
        return config.getOrElse("font.lcdSubpixel", false);
    }

    public static boolean builtinFallbacksEnabled() {
        return config.getOrElse("font.builtinFallbacks", true);
    }

    public static List<BuiltinFont> builtinFonts() {
        return BUILTIN_FONTS;
    }

    // ===================== Shadow =====================
    public static float shadowLength() {
        return getFloat("shadow.length", 1.0f);
    }

    public static float shadowOpacity() {
        return getFloat("shadow.opacity", 0.25f);
    }

    // ===================== Rendering =====================
    public static String renderingEngine() {
        return normalizeRenderingEngine(config.getOrElse("rendering.engine", "skia"));
    }

    public static boolean useSfrEngine() {
        return enabled() && "sfr".equals(renderingEngine());
    }

    public static boolean useSkiaEngine() {
        return enabled() && "skia".equals(renderingEngine());
    }

    public static boolean skiaAdvancedStringMode() {
        return config.getOrElse("rendering.skiaAdvancedStringMode", true);
    }

    public static boolean skiaGpuOffscreen() {
        return config.getOrElse("rendering.skiaGpuOffscreen", false);
    }

    public static boolean skiaGpuSubmitViaCpuTexture() {
        return config.getOrElse("rendering.skiaGpuSubmitViaCpuTexture", true);
    }

    public static boolean skiaMonochromeText() {
        return config.getOrElse("rendering.skiaMonochromeText", true);
    }

    public static boolean skiaSegmentCache() {
        return config.getOrElse("rendering.skiaSegmentCache", true);
    }

    public static int skiaSegmentCacheMinRunLength() {
        return Math.max(1, getInt("rendering.skiaSegmentCacheMinRunLength", 8));
    }

    public static int skiaSegmentCacheMaxRunCodePoints() {
        return Math.max(1, getInt("rendering.skiaSegmentCacheMaxRunCodePoints", 24));
    }

    public static int skiaSegmentCacheMaxSegments() {
        return Math.max(2, getInt("rendering.skiaSegmentCacheMaxSegments", 96));
    }

    public static boolean useVanillaEngine() {
        return !enabled() || "vanilla".equals(renderingEngine());
    }

    public static boolean renderingInterpolation() {
        return config.getOrElse("rendering.interpolation", false);
    }

    public static boolean renderingMipmap() {
        return config.getOrElse("rendering.mipmap", true);
    }

    public static boolean adaptiveRasterScale() {
        return config.getOrElse("rendering.adaptiveRasterScale", true);
    }

    public static float adaptiveRasterMin() {
        return getFloat("rendering.adaptiveRasterMin", 1.5f);
    }

    public static float adaptiveRasterMax() {
        return getFloat("rendering.adaptiveRasterMax", 14.0f);
    }

    public static float adaptiveRasterStep() {
        return getFloat("rendering.adaptiveRasterStep", 0.5f);
    }

    public static boolean excludeIntegerScale() {
        return config.getOrElse("rendering.excludeIntegerScale", true);
    }

    public static boolean excludeHighMagnification() {
        return config.getOrElse("rendering.excludeHighMagnification", true);
    }

    public static float limitMagnification() {
        return getFloat("rendering.limitMagnification", 3.0f);
    }

    public static float scaleRoundingToleranceRate() {
        return getFloat("rendering.scaleRoundingTolerance", 0.5f) * 0.01f;
    }

    public static float mipmapLodBias() {
        return getFloat("rendering.mipmapLodBias", -0.3f);
    }

    public static float overlayMipmapLodBias() {
        return getFloat("rendering.overlayMipmapLodBias", -0.5f);
    }

    public static boolean anisotropicFiltering() {
        return config.getOrElse("rendering.anisotropicFiltering", true);
    }

    public static float blurReductionThreshold() {
        return getFloat("rendering.blurReduction", 10.0f);
    }

    public static float smoothShadowThreshold() {
        return getFloat("rendering.smoothShadowThreshold", 24.0f);
    }

    public static boolean enhancedTextPipeline() {
        return config.getOrElse("rendering.enhancedTextPipeline", false);
    }

    public static boolean shaderTextPipeline() {
        return config.getOrElse("rendering.shaderTextPipeline", false);
    }

    public static float renderingBrightness() {
        return getFloat("rendering.brightness", 0.0f);
    }

    public static boolean textureEdgeBleed() {
        return config.getOrElse("rendering.textureEdgeBleed", false);
    }

    public static boolean renderingBrightnessAuto() {
        return config.getOrElse("rendering.brightnessAuto", true);
    }

    public static boolean enablePremultipliedAlpha() {
        return config.getOrElse("rendering.premultipliedAlpha", false);
    }

    /**
     * Force GL_BLEND enabled when drawing Skia-rendered text.
     *
     * <p>Vanilla MC disables blend in some code paths (e.g.
     * {@code RenderItem.renderItemOverlayIntoGUI} for item counts,
     * durability bars, cooldown overlays) because the default bitmap
     * font uses 1-bit alpha — every pixel is either fully opaque or
     * fully transparent, so blend is unnecessary.</p>
     *
     * <p>Skia, however, produces anti-aliased text with multi-bit
     * alpha (semi-transparent edge pixels).  When GL_BLEND is off,
     * those edge pixels write their raw RGB directly to the
     * framebuffer instead of blending with the background, causing
     * dark fringes and jagged edges — especially visible on inventory
     * item counts.</p>
     *
     * <p>Setting this to true ensures blend is always on during Skia
     * text rendering regardless of what the calling MC code path
     * requested.  This is the correct behavior for alpha-composited
     * anti-aliased text and matches how SmoothFont handles the same
     * situation.</p>
     */
    public static boolean forceBlendForText() {
        return config.getOrElse("rendering.forceBlendForText", true);
    }

    // ===================== Performance =====================
    public static boolean performanceAsyncInit() {
        return config.getOrElse("performance.asyncInit", true);
    }

    public static boolean performancePrewarmBasicLatin() {
        return config.getOrElse("performance.prewarmBasicLatin", true);
    }

    public static int skiaTextCacheMinEntries() {
        return Math.max(0, getInt("performance.skiaTextCacheMinEntries", 256));
    }

    public static int skiaTextCacheMaxEntries() {
        return Math.max(1, getInt("performance.skiaTextCacheMaxEntries", 2048));
    }

    public static float skiaTextCacheTtlSeconds() {
        return Math.max(0.0f, getFloat("performance.skiaTextCacheTtlSeconds", 300.0f));
    }

    public static int skiaMeasureCacheMaxEntries() {
        return Math.max(1, getInt("performance.skiaMeasureCacheMaxEntries", 4096));
    }

    public static int skiaSegmentTextureCacheMinEntries() {
        return Math.max(0, getInt("performance.skiaSegmentTextureCacheMinEntries", 512));
    }

    public static int skiaSegmentTextureCacheMaxEntries() {
        return Math.max(1, getInt("performance.skiaSegmentTextureCacheMaxEntries", 4096));
    }

    public static float skiaSegmentTextureCacheTtlSeconds() {
        return Math.max(0.0f, getFloat("performance.skiaSegmentTextureCacheTtlSeconds", 600.0f));
    }

    // ===================== General =====================
    public static boolean enabled() {
        return config.getOrElse("enabled", true);
    }

    public static boolean fixImeInput() {
        return config.getOrElse("fixImeInput", true);
    }

    public static boolean debugImeInput() {
        return config.getOrElse("debug.imeInput", false);
    }

    public static boolean debugRenderStats() {
        return cachedDebugRenderStats;
    }

    public static boolean allowSignPaste() {
        return config.getOrElse("input.allowSignPaste", true);
    }

    public static void setEnabled(boolean value) {
        config.set("enabled", value);
    }

    public static void setFontName(String value) {
        config.set("font.name", value);
    }

    public static void setFontFallbacks(List<String> value) {
        config.set("font.fallbacks", value == null ? Collections.emptyList() : new ArrayList<>(value));
    }

    public static void setFontStyle(int value) {
        config.set("font.style", value);
    }

    public static void setFontSize(float value) {
        config.set("font.size", value);
    }

    public static void setFontOversample(float value) {
        config.set("font.oversample", value);
    }

    public static void setFontAutoBaseline(boolean value) {
        config.set("font.autoBaseline", value);
    }

    public static void setFontBaselineShift(float value) {
        config.set("font.baselineShift", value);
    }

    public static void setFontReferenceBaseline(float value) {
        config.set("font.referenceBaseline", value);
    }

    public static void setFontAntialias(boolean value) {
        config.set("font.antialias", value);
    }

    public static void setFontAntialiasMode(String value) {
        String mode = normalizeAntialiasMode(value);
        config.set("font.antialiasMode", mode);
        config.set("font.antialias", !"off".equals(mode));
    }

    public static void setFontFractionalMetrics(boolean value) {
        config.set("font.fractionalMetrics", value);
    }

    public static void setFontLcdSubpixel(boolean value) {
        config.set("font.lcdSubpixel", value);
    }

    public static void setBuiltinFallbacksEnabled(boolean value) {
        config.set("font.builtinFallbacks", value);
    }

    public static void setShadowLength(float value) {
        config.set("shadow.length", value);
    }

    public static void setShadowOpacity(float value) {
        config.set("shadow.opacity", value);
    }

    public static void setRenderingInterpolation(boolean value) {
        config.set("rendering.interpolation", value);
    }

    public static void setRenderingMipmap(boolean value) {
        config.set("rendering.mipmap", value);
    }

    public static void setAdaptiveRasterScale(boolean value) {
        config.set("rendering.adaptiveRasterScale", value);
    }

    public static void setAdaptiveRasterMin(float value) {
        config.set("rendering.adaptiveRasterMin", value);
    }

    public static void setAdaptiveRasterMax(float value) {
        config.set("rendering.adaptiveRasterMax", value);
    }

    public static void setAdaptiveRasterStep(float value) {
        config.set("rendering.adaptiveRasterStep", value);
    }

    public static void setExcludeIntegerScale(boolean value) {
        config.set("rendering.excludeIntegerScale", value);
    }

    public static void setExcludeHighMagnification(boolean value) {
        config.set("rendering.excludeHighMagnification", value);
    }

    public static void setLimitMagnification(float value) {
        config.set("rendering.limitMagnification", value);
    }

    public static void setScaleRoundingTolerance(float value) {
        config.set("rendering.scaleRoundingTolerance", value);
    }

    public static void setMipmapLodBias(float value) {
        config.set("rendering.mipmapLodBias", value);
    }

    public static void setOverlayMipmapLodBias(float value) {
        config.set("rendering.overlayMipmapLodBias", value);
    }

    public static void setAnisotropicFiltering(boolean value) {
        config.set("rendering.anisotropicFiltering", value);
    }

    public static void setBlurReductionThreshold(float value) {
        config.set("rendering.blurReduction", value);
    }

    public static void setSmoothShadowThreshold(float value) {
        config.set("rendering.smoothShadowThreshold", value);
    }

    public static void setEnhancedTextPipeline(boolean value) {
        config.set("rendering.enhancedTextPipeline", value);
    }

    public static void setShaderTextPipeline(boolean value) {
        config.set("rendering.shaderTextPipeline", value);
    }

    public static void setRenderingBrightness(float value) {
        config.set("rendering.brightness", value);
    }

    public static void setTextureEdgeBleed(boolean value) {
        config.set("rendering.textureEdgeBleed", value);
    }

    public static void setRenderingBrightnessAuto(boolean value) {
        config.set("rendering.brightnessAuto", value);
    }

    public static void setEnablePremultipliedAlpha(boolean value) {
        config.set("rendering.premultipliedAlpha", value);
    }

    public static void setForceBlendForText(boolean value) {
        config.set("rendering.forceBlendForText", value);
    }

    public static void setRenderingEngine(String value) {
        config.set("rendering.engine", normalizeRenderingEngine(value));
    }

    public static void setSkiaAdvancedStringMode(boolean value) {
        config.set("rendering.skiaAdvancedStringMode", value);
    }

    public static void setSkiaGpuOffscreen(boolean value) {
        config.set("rendering.skiaGpuOffscreen", value);
    }

    public static void setSkiaGpuSubmitViaCpuTexture(boolean value) {
        config.set("rendering.skiaGpuSubmitViaCpuTexture", value);
    }

    public static void setSkiaMonochromeText(boolean value) {
        config.set("rendering.skiaMonochromeText", value);
    }

    public static void setPerformanceAsyncInit(boolean value) {
        config.set("performance.asyncInit", value);
    }

    public static void setPerformancePrewarmBasicLatin(boolean value) {
        config.set("performance.prewarmBasicLatin", value);
    }

    public static void setSkiaTextCacheMinEntries(int value) {
        config.set("performance.skiaTextCacheMinEntries", value);
    }

    public static void setSkiaTextCacheMaxEntries(int value) {
        config.set("performance.skiaTextCacheMaxEntries", value);
    }

    public static void setSkiaTextCacheTtlSeconds(float value) {
        config.set("performance.skiaTextCacheTtlSeconds", value);
    }

    public static void setSkiaMeasureCacheMaxEntries(int value) {
        config.set("performance.skiaMeasureCacheMaxEntries", value);
    }

    public static void setDebugRenderStats(boolean value) {
        cachedDebugRenderStats = value;
        config.set("debug.renderStats", value);
    }

    public static void save() {
        if (config != null) {
            config.save();
        }
    }

    public static void load() {
        if (configPath == null) {
            configPath = new File(Minecraft.getMinecraft().gameDir, "config" + File.separator + CONFIG_NAME).toPath();
        }

        File configFile = configPath.toFile();
        boolean needsDefault = !configFile.exists();

        if (needsDefault) {
            try {
                Files.createDirectories(configPath.getParent());
                writeDefaultConfig(configFile);
            } catch (IOException e) {
                NeoFontRender.LOGGER.error("Failed to create default config", e);
            }
        }

        config = CommentedFileConfig.builder(configPath, TomlFormat.instance())
                .preserveInsertionOrder()
                .build();
        config.load();

        if (needsDefault) {
            addComments();
            config.save();
        }
        refreshCachedOptions();
        ensureFontDirectory();
    }

    private static void refreshCachedOptions() {
        cachedDebugRenderStats = config.getOrElse("debug.renderStats", false);
    }

    public static File fontDirectory() {
        return new File(Minecraft.getMinecraft().gameDir, "neofontrender" + File.separator + "fonts");
    }

    public static File ensureFontDirectory() {
        File dir = fontDirectory();
        if (!dir.isDirectory() && !dir.mkdirs()) {
            NeoFontRender.LOGGER.warn("Failed to create font directory '{}'", dir);
        }
        return dir;
    }

    private static void writeDefaultConfig(File file) throws IOException {
        try (Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            w.write("# Neo Font Render Configuration\n");
            w.write("\n");
            w.write("enabled = true\n");
            w.write("\n");
            w.write("[font]\n");
            w.write("name = \"" + DEFAULT_FONT + "\"\n");
            w.write("fallbacks = [\"Serif\", \"Monospaced\"]\n");
            w.write("style = 0\n");
            w.write("size = 10.0\n");
            w.write("oversample = 12.0\n");
            w.write("autoBaseline = true\n");
            w.write("baselineShift = 0.0\n");
            w.write("referenceBaseline = 7.0\n");
            w.write("antialias = true\n");
            w.write("antialiasMode = \"on\"\n");
            w.write("fractionalMetrics = true\n");
            w.write("lcdSubpixel = false\n");
            w.write("builtinFallbacks = true\n");
            w.write("\n");
            w.write("[shadow]\n");
            w.write("length = 1.0\n");
            w.write("opacity = 0.25\n");
            w.write("\n");
            w.write("[rendering]\n");
            w.write("engine = \"skia\"\n");
            w.write("skiaAdvancedStringMode = true\n");
            w.write("skiaGpuOffscreen = false\n");
            w.write("skiaGpuSubmitViaCpuTexture = true\n");
            w.write("skiaMonochromeText = true\n");
            w.write("skiaSegmentCache = true\n");
            w.write("skiaSegmentCacheMinRunLength = 8\n");
            w.write("skiaSegmentCacheMaxRunCodePoints = 24\n");
            w.write("skiaSegmentCacheMaxSegments = 96\n");
            w.write("interpolation = false\n");
            w.write("mipmap = true\n");
            w.write("adaptiveRasterScale = true\n");
            w.write("adaptiveRasterMin = 1.5\n");
            w.write("adaptiveRasterMax = 14.0\n");
            w.write("adaptiveRasterStep = 0.5\n");
            w.write("excludeIntegerScale = true\n");
            w.write("excludeHighMagnification = true\n");
            w.write("limitMagnification = 3.0\n");
            w.write("scaleRoundingTolerance = 0.5\n");
            w.write("mipmapLodBias = -0.3\n");
            w.write("overlayMipmapLodBias = -0.5\n");
            w.write("anisotropicFiltering = true\n");
            w.write("blurReduction = 10.0\n");
            w.write("smoothShadowThreshold = 24.0\n");
            w.write("enhancedTextPipeline = false\n");
            w.write("shaderTextPipeline = false\n");
            w.write("brightness = 0.0\n");
            w.write("brightnessAuto = true\n");
            w.write("premultipliedAlpha = false\n");
            w.write("textureEdgeBleed = false\n");
            w.write("forceBlendForText = true\n");
            w.write("\n");
            w.write("[performance]\n");
            w.write("asyncInit = true\n");
            w.write("prewarmBasicLatin = true\n");
            w.write("skiaTextCacheMinEntries = 256\n");
            w.write("skiaTextCacheMaxEntries = 2048\n");
            w.write("skiaTextCacheTtlSeconds = 300.0\n");
            w.write("skiaMeasureCacheMaxEntries = 4096\n");
            w.write("skiaSegmentTextureCacheMinEntries = 512\n");
            w.write("skiaSegmentTextureCacheMaxEntries = 4096\n");
            w.write("skiaSegmentTextureCacheTtlSeconds = 600.0\n");
            w.write("\n");
            w.write("[input]\n");
            w.write("allowSignPaste = true\n");
            w.write("\n");
            w.write("[debug]\n");
            w.write("imeInput = false\n");
            w.write("renderStats = false\n");
        }
    }

    private static void addComments() {
        config.setComment("enabled", "Enable/disable the entire font replacement pipeline.");
        config.setComment("font", "Font selection and rasterization settings.");
        config.setComment("font.name", "Primary font name or TTF file path. Comma/semicolon-separated font family lists are also supported.");
        config.setComment("font.fallbacks", "Fallback font names or TTF file paths queried after font.name when a glyph is missing.");
        config.setComment("font.style", "Font style: 0=Plain, 1=Bold, 2=Italic, 3=Bold+Italic.");
        config.setComment("font.size", "Font size in pixels. 10.0 is a comfortable default.");
        config.setComment("font.oversample", "Rasterization oversampling factor. Raster resolution is size * oversample; 8.0 at size 8.0 is a 64px glyph raster.");
        config.setComment("font.autoBaseline", "Align each font's measured AWT baseline to the Minecraft reference baseline before manual shift.");
        config.setComment("font.baselineShift", "Additional vertical glyph shift in Minecraft pixels after automatic baseline alignment. Positive moves glyphs down.");
        config.setComment("font.referenceBaseline", "Minecraft-space baseline used by autoBaseline. Vanilla 8px UI text is approximately 7.0.");
        config.setComment("font.antialias", "Enable AWT anti-aliasing during glyph rasterization.");
        config.setComment("font.antialiasMode", "AWT text anti-aliasing mode: off, on, gasp, lcd_hrgb, lcd_hbgr, lcd_vrgb, lcd_vbgr.");
        config.setComment("font.fractionalMetrics", "Enable fractional font metrics for more precise positioning.");
        config.setComment("font.lcdSubpixel", "Enable LCD subpixel anti-aliasing in Skia rasterization. Produces sharper text on standard RGB monitors but may show color fringes.");
        config.setComment("font.builtinFallbacks", "Always append bundled fonts, such as Noto Color Emoji, to the fallback family.");
        config.setComment("shadow", "Text shadow rendering options.");
        config.setComment("shadow.length", "Shadow offset distance in pixels.");
        config.setComment("shadow.opacity", "Shadow opacity multiplier (0.0-1.0).");
        config.setComment("rendering", "OpenGL texture rendering options.");
        config.setComment("rendering.engine", "Text renderer engine: vanilla, sfr, or skia.");
        config.setComment("rendering.skiaAdvancedStringMode", "In Skia mode, render full formatted strings as one paragraph so shaping, ligatures, kerning, emoji ZWJ, and BiDi can work across the whole text. Disable to use legacy per-format-run rendering.");
        config.setComment("rendering.skiaGpuOffscreen", "Experimental: render Skia text cache textures in an isolated hidden OpenGL context shared with Minecraft, instead of CPU rasterization. Requires rendering.premultipliedAlpha=true. Failures automatically fall back to CPU rasterization.");
        config.setComment("rendering.skiaGpuSubmitViaCpuTexture", "Default safe mode for skiaGpuOffscreen: rasterize in the isolated GPU context, read pixels back, then submit through Minecraft DynamicTexture like the CPU path. Disable only to test the experimental shared-GL texture path.");
        config.setComment("rendering.skiaMonochromeText", "Rasterize monochrome (single-color) text runs as white glyphs and tint them with vertex color at draw time, so one glyph texture is reused across all colors instead of being re-rasterized per color. Greatly improves Skia cache hit rate. Only applies to premultiplied-alpha rendering (bypassed automatically when premultipliedAlpha=false). Disable to fall back to baking color into textures.");
        config.setComment("rendering.skiaSegmentCache", "When skiaAdvancedStringMode=false, split safe Skia text runs into reusable cache tokens: Latin words, individual digits, CJK/Hiragana/Katakana/Hangul characters, and simple punctuation. Complex shaping text stays on the full-run path.");
        config.setComment("rendering.skiaSegmentCacheMinRunLength", "Minimum formatted run length before Skia token cache segmentation is attempted.");
        config.setComment("rendering.skiaSegmentCacheMaxRunCodePoints", "Maximum code points kept in one reusable Skia segment before forcing another token boundary.");
        config.setComment("rendering.skiaSegmentCacheMaxSegments", "Maximum number of Skia segments produced from one formatted run. Runs exceeding this limit render as one full texture to avoid too many draw calls.");
        config.setComment("rendering.interpolation", "Use GL_LINEAR texture filtering instead of GL_NEAREST.");
        config.setComment("rendering.mipmap", "Enable mipmapping for font textures (may help at small sizes).");
        config.setComment("rendering.adaptiveRasterScale", "Use a 1.5x-14x adaptive raster scale based on the current framebuffer text scale, and use nearest filtering for 1:1/integer pixel output to avoid over-downsample blur.");
        config.setComment("rendering.adaptiveRasterMin", "Minimum adaptive raster scale bucket.");
        config.setComment("rendering.adaptiveRasterMax", "Maximum adaptive raster scale bucket.");
        config.setComment("rendering.adaptiveRasterStep", "Adaptive raster scale bucket step.");
        config.setComment("rendering.excludeIntegerScale", "When adaptiveRasterScale is enabled, use nearest filtering for near-integer raster/screen scale ratios.");
        config.setComment("rendering.excludeHighMagnification", "When adaptiveRasterScale is enabled, use nearest filtering when text is magnified far beyond the font texture resolution.");
        config.setComment("rendering.limitMagnification", "Magnification threshold used by excludeHighMagnification.");
        config.setComment("rendering.scaleRoundingTolerance", "Percent tolerance used when rounding the measured framebuffer text scale.");
        config.setComment("rendering.mipmapLodBias", "Mipmap LOD bias for perspective/world text while adaptiveRasterScale is enabled.");
        config.setComment("rendering.overlayMipmapLodBias", "Mipmap LOD bias for orthographic GUI text while adaptiveRasterScale is enabled.");
        config.setComment("rendering.anisotropicFiltering", "Enable anisotropic filtering for perspective/world text while adaptiveRasterScale is enabled.");
        config.setComment("rendering.blurReduction", "If the effective font resolution is at or below this value, upload a 2x nearest-neighbor texture to reduce blur.");
        config.setComment("rendering.smoothShadowThreshold", "Minimum effective font resolution where shadow text is allowed to use smooth filtering.");
        config.setComment("rendering.enhancedTextPipeline", "Use a dedicated text draw pipeline that forces straight-alpha blending and restores previous GL state after rendering. Keep this OFF for color emoji; it can alter emoji colors.");
        config.setComment("rendering.shaderTextPipeline", "Use a tiny fixed-pipeline-compatible shader to compensate thin anti-aliased glyph edges. Automatically falls back if shader compilation fails.");
        config.setComment("rendering.brightness", "Text edge compensation strength used by the enhanced shader pipeline. 0 disables extra alpha boost; 3 is close to SmoothFont-style defaults.");
        config.setComment("rendering.brightnessAuto", "Automatically detect brightness compensation from sample glyph rasterization. When true, rendering.brightness is ignored.");
        config.setComment("rendering.premultipliedAlpha", "Upload glyph textures with premultiplied alpha. Requires the enhanced shader pipeline to look correct. Matches SmoothFont's premultiplied-alpha mode.");
        config.setComment("rendering.textureEdgeBleed", "Fill fully-transparent Skia text pixels with neighboring RGB to prevent black fringes when linear filtering samples color outside glyph edges.");
        config.setComment("rendering.forceBlendForText", "Force GL_BLEND on when drawing Skia text. MC disables blend in some paths (e.g. renderItemOverlayIntoGUI for item counts) because the vanilla bitmap font uses 1-bit alpha. Skia produces anti-aliased text with multi-bit alpha that needs blend to composite correctly; without it, semi-transparent edge pixels write raw RGB causing dark fringes and jagged edges.");
        config.setComment("performance", "Performance tuning options.");
        config.setComment("performance.asyncInit", "Initialize font rasterization on a background thread.");
        config.setComment("performance.prewarmBasicLatin", "Pre-bake common Basic Latin and Latin-1 glyphs before enabling replacement rendering.");
        config.setComment("performance.skiaTextCacheMinEntries", "Minimum number of Skia rendered text textures kept when TTL cleanup runs.");
        config.setComment("performance.skiaTextCacheMaxEntries", "Maximum number of Skia rendered text textures kept in the LRU cache.");
        config.setComment("performance.skiaTextCacheTtlSeconds", "Seconds before an unused Skia rendered text texture can be evicted. 0 disables TTL cleanup.");
        config.setComment("performance.skiaMeasureCacheMaxEntries", "Maximum number of Skia text measurement results kept in memory.");
        config.setComment("performance.skiaSegmentTextureCacheMinEntries", "Minimum number of reusable Skia segment textures kept when TTL cleanup runs.");
        config.setComment("performance.skiaSegmentTextureCacheMaxEntries", "Maximum number of reusable Skia segment textures kept in the segment LRU cache.");
        config.setComment("performance.skiaSegmentTextureCacheTtlSeconds", "Seconds before an unused Skia segment texture can be evicted. 0 disables TTL cleanup.");
        config.setComment("input", "Input behavior tweaks.");
        config.setComment("input.allowSignPaste", "Allow Ctrl+V paste in the vanilla sign editor. This is intentionally config-file only.");
        config.setComment("debug", "Debug logging options.");
        config.setComment("debug.imeInput", "Log IME input fix details to game log (for diagnosing emoji input issues).");
        config.setComment("debug.renderStats", "Collect high-frequency font renderer hit/miss/eviction counters, segment counters, and expensive raster pixel statistics for F3/commands. Disable for normal gameplay.");
    }

    private static float getFloat(String key, float defaultValue) {
        Object val = config.get(key);
        if (val instanceof Number) {
            return ((Number) val).floatValue();
        }
        return defaultValue;
    }

    private static int getInt(String key, int defaultValue) {
        Object val = config.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return defaultValue;
    }

    private static void addFontNames(Set<String> fonts, String value) {
        if (value == null) {
            return;
        }
        for (String part : value.split("[,;]")) {
            String font = part.trim();
            if (!font.isEmpty()) {
                fonts.add(normalizeFontLocation(font));
            }
        }
    }

    private static String normalizeFontLocation(String font) {
        if ("neofontrender:fonts/NotoColorEmoji-Regular.ttf".equals(font)) {
            return "neofontrender:fonts/noto_color_emoji_regular.ttf";
        }
        if ("neofontrender:fonts/IBMPlexSansSC-Regular.ttf".equals(font)) {
            return DEFAULT_FONT;
        }
        return font;
    }

    private static String normalizeAntialiasMode(String value) {
        if (value == null) {
            return "on";
        }
        String mode = value.trim().toLowerCase().replace('-', '_');
        switch (mode) {
            case "false":
            case "none":
            case "off":
                return "off";
            case "true":
            case "default":
            case "on":
                return "on";
            case "gasp":
            case "lcd_hrgb":
            case "lcd_hbgr":
            case "lcd_vrgb":
            case "lcd_vbgr":
                return mode;
            default:
                return "on";
        }
    }

    private static String normalizeRenderingEngine(String value) {
        if (value == null) {
            return "sfr";
        }
        String mode = value.trim().toLowerCase().replace('-', '_');
        switch (mode) {
            case "off":
            case "original":
            case "default":
            case "minecraft":
            case "vanilla":
                return "vanilla";
            case "smr":
            case "sfr":
            case "awt":
                return "sfr";
            case "skija":
            case "skia":
                return "skia";
            default:
                return "sfr";
        }
    }

    public static void reload() {
        if (config != null) {
            config.load();
        }
    }

    public static final class BuiltinFont {
        private final String displayName;
        private final String location;

        private BuiltinFont(String displayName, String location) {
            this.displayName = displayName;
            this.location = location;
        }

        public String displayName() {
            return displayName;
        }

        public String location() {
            return location;
        }
    }
}
