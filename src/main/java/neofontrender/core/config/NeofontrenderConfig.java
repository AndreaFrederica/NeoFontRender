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
import java.util.Locale;
import java.util.Set;

/**
 * TOML-based configuration manager using NightConfig.
 */
public final class NeofontrenderConfig {

    private static final String CONFIG_NAME = "neofontrender.toml";
    private static final String DEFAULT_FONT = "neofontrender:fonts/sarasa_ui_sc_regular.ttf";
    private static Path configPath;
    private static CommentedFileConfig config;
    private static volatile Snapshot cached = Snapshot.defaults();
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
        return cached.fontStyle;
    }

    public static int fontVariableWeight() {
        return cached.fontVariableWeight;
    }

    public static String cosmicRegularFont() {
        return normalizeFontLocation(config.getOrElse("font.cosmic.regular", ""));
    }

    public static String cosmicBoldFont() {
        return normalizeFontLocation(config.getOrElse("font.cosmic.bold", ""));
    }

    public static String cosmicItalicFont() {
        return normalizeFontLocation(config.getOrElse("font.cosmic.italic", ""));
    }

    public static String cosmicBoldItalicFont() {
        return normalizeFontLocation(config.getOrElse("font.cosmic.boldItalic", ""));
    }

    public static boolean cosmicVariantOverridesOnlySwitchFont() {
        return cached.cosmicVariantOverridesOnlySwitchFont;
    }

    public static List<String> cosmicFaceOverrides() {
        return Collections.unmodifiableList(Arrays.asList(
                cosmicRegularFont(), cosmicBoldFont(), cosmicItalicFont(), cosmicBoldItalicFont()));
    }

    public static float fontSize() {
        return cached.fontSize;
    }

    public static float fontOversample() {
        return cached.fontOversample;
    }

    public static boolean fontAutoBaseline() {
        return cached.fontAutoBaseline;
    }

    public static float fontBaselineShift() {
        return cached.fontBaselineShift;
    }

    public static float fontReferenceBaseline() {
        return cached.fontReferenceBaseline;
    }

    public static boolean fontAntialias() {
        return cached.fontAntialias;
    }

    public static String fontAntialiasMode() {
        return cached.fontAntialiasMode;
    }

    public static boolean fontFractionalMetrics() {
        return cached.fontFractionalMetrics;
    }

    public static boolean fontLcdSubpixel() {
        return cached.fontLcdSubpixel;
    }

    public static boolean builtinFallbacksEnabled() {
        return cached.builtinFallbacks;
    }

    public static List<BuiltinFont> builtinFonts() {
        return BUILTIN_FONTS;
    }

    // ===================== Shadow =====================
    public static float shadowLength() {
        return cached.shadowLength;
    }

    public static float shadowOpacity() {
        return cached.shadowOpacity;
    }

    /** all, mask, emoji, or none. */
    public static String shadowMode() {
        return cached.shadowMode;
    }

    public static String shadowMaskFonts() { return cached.shadowMaskFonts; }
    public static String shadowMaskCodepoints() { return cached.shadowMaskCodepoints; }

    // ===================== Rendering =====================
    public static String renderingEngine() {
        return cached.renderingEngine;
    }

    public static boolean useSfrEngine() {
        return enabled() && "sfr".equals(renderingEngine());
    }

    public static boolean useSkiaEngine() {
        return enabled() && "skia".equals(renderingEngine());
    }

    public static boolean useCosmicEngine() {
        return enabled() && "cosmic".equals(renderingEngine());
    }

    public static boolean skiaAdvancedStringMode() {
        return cached.skiaAdvancedStringMode;
    }

    public static boolean skiaGpuOffscreen() {
        return cached.skiaGpuOffscreen;
    }

    public static boolean skiaGpuSubmitViaCpuTexture() {
        return cached.skiaGpuSubmitViaCpuTexture;
    }

    public static boolean skiaMonochromeText() {
        return cached.skiaMonochromeText;
    }

    public static boolean skiaSegmentCache() {
        return cached.skiaSegmentCache;
    }

    public static int skiaSegmentCacheMinRunLength() {
        return cached.skiaSegmentCacheMinRunLength;
    }

    public static int skiaSegmentCacheMaxRunCodePoints() {
        return cached.skiaSegmentCacheMaxRunCodePoints;
    }

    public static int skiaSegmentCacheMaxSegments() {
        return cached.skiaSegmentCacheMaxSegments;
    }

    public static boolean useVanillaEngine() {
        return !enabled() || "vanilla".equals(renderingEngine());
    }

    public static boolean renderingInterpolation() {
        return cached.renderingInterpolation;
    }

    public static boolean renderingMipmap() {
        return cached.renderingMipmap;
    }

    public static boolean adaptiveRasterScale() {
        return cached.adaptiveRasterScale;
    }

    public static float adaptiveRasterMin() {
        return cached.adaptiveRasterMin;
    }

    public static float adaptiveRasterMax() {
        return cached.adaptiveRasterMax;
    }

    public static float adaptiveRasterStep() {
        return cached.adaptiveRasterStep;
    }

    public static boolean excludeIntegerScale() {
        return cached.excludeIntegerScale;
    }

    public static boolean excludeHighMagnification() {
        return cached.excludeHighMagnification;
    }

    public static float limitMagnification() {
        return cached.limitMagnification;
    }

    public static float scaleRoundingToleranceRate() {
        return cached.scaleRoundingToleranceRate;
    }

    public static float mipmapLodBias() {
        return cached.mipmapLodBias;
    }

    public static float overlayMipmapLodBias() {
        return cached.overlayMipmapLodBias;
    }

    public static boolean anisotropicFiltering() {
        return cached.anisotropicFiltering;
    }

    public static float blurReductionThreshold() {
        return cached.blurReductionThreshold;
    }

    public static float smoothShadowThreshold() {
        return cached.smoothShadowThreshold;
    }

    public static boolean enhancedTextPipeline() {
        return cached.enhancedTextPipeline;
    }

    public static boolean shaderTextPipeline() {
        return cached.shaderTextPipeline;
    }

    public static float renderingBrightness() {
        return cached.renderingBrightness;
    }

    public static boolean textureEdgeBleed() {
        return cached.textureEdgeBleed;
    }

    public static boolean renderingBrightnessAuto() {
        return cached.renderingBrightnessAuto;
    }

    public static boolean enablePremultipliedAlpha() {
        return cached.enablePremultipliedAlpha;
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
        return cached.forceBlendForText;
    }

    // ===================== Performance =====================
    public static boolean performanceAsyncInit() {
        return cached.performanceAsyncInit;
    }

    public static boolean performancePrewarmBasicLatin() {
        return cached.performancePrewarmBasicLatin;
    }

    public static boolean signTextLodCulling() {
        return cached.signTextLodCulling;
    }

    public static float signTextMinPixelHeight() {
        return cached.signTextMinPixelHeight;
    }

    public static boolean signTextBatching() {
        return cached.signTextBatching;
    }

    public static boolean signTextFrustumCulling() {
        return cached.signTextFrustumCulling;
    }

    public static boolean signModelLod() {
        return cached.signModelLod;
    }

    public static float signModelLodDistance() {
        return cached.signModelLodDistance;
    }

    public static float signTextNearThreshold() {
        return cached.signTextNearThreshold;
    }

    public static float signTextNearSupersample() {
        return cached.signTextNearSupersample;
    }

    public static float signTextNearMaxRasterScale() {
        return cached.signTextNearMaxRasterScale;
    }

    public static boolean signCrossTileBatching() {
        return cached.signCrossTileBatching;
    }

    public static int signBatchMaxEntries() {
        return cached.signBatchMaxEntries;
    }

    public static boolean signBlockOcclusionCulling() {
        return cached.signBlockOcclusionCulling;
    }

    public static int signOcclusionChecksPerFrame() {
        return cached.signOcclusionChecksPerFrame;
    }

    public static long signOcclusionCacheMillis() {
        return cached.signOcclusionCacheMillis;
    }

    public static float signOcclusionMinDistance() {
        return cached.signOcclusionMinDistance;
    }

    public static int skiaTextCacheMinEntries() {
        return cached.skiaTextCacheMinEntries;
    }

    public static int skiaTextCacheMaxEntries() {
        return cached.skiaTextCacheMaxEntries;
    }

    public static float skiaTextCacheTtlSeconds() {
        return cached.skiaTextCacheTtlSeconds;
    }

    public static int skiaMeasureCacheMaxEntries() {
        return cached.skiaMeasureCacheMaxEntries;
    }

    public static int skiaSegmentTextureCacheMinEntries() {
        return cached.skiaSegmentTextureCacheMinEntries;
    }

    public static int skiaSegmentTextureCacheMaxEntries() {
        return cached.skiaSegmentTextureCacheMaxEntries;
    }

    public static float skiaSegmentTextureCacheTtlSeconds() {
        return cached.skiaSegmentTextureCacheTtlSeconds;
    }

    // These generic accessors intentionally retain the old TOML keys. Existing installations
    // already tune them, and duplicating identical Skia/Cosmic limits would make engine switches
    // unexpectedly discard the user's memory policy.
    public static int textCacheMinEntries() {
        return skiaTextCacheMinEntries();
    }

    public static int textCacheMaxEntries() {
        return skiaTextCacheMaxEntries();
    }

    public static float textCacheTtlSeconds() {
        return skiaTextCacheTtlSeconds();
    }

    public static int measureCacheMaxEntries() {
        return skiaMeasureCacheMaxEntries();
    }

    // ===================== General =====================
    public static boolean enabled() {
        return cached.enabled;
    }

    public static boolean fixImeInput() {
        return cached.fixImeInput;
    }

    public static boolean debugImeInput() {
        return cached.debugImeInput;
    }

    public static boolean debugRenderStats() {
        return cachedDebugRenderStats;
    }

    public static boolean allowSignPaste() {
        return cached.allowSignPaste;
    }

    public static boolean fixUnicodeTextDeletion() {
        return cached.fixUnicodeTextDeletion;
    }

    public static boolean laboratoryHexChat() {
        return cached.laboratoryHexChat;
    }

    public static boolean laboratoryTextUndoRedo() {
        return cached.laboratoryTextUndoRedo;
    }

    public static boolean compatModernSplash() {
        return cached.compatModernSplash;
    }

    public static boolean splashFontOverrideEnabled() {
        return cached.splashFontOverrideEnabled;
    }

    public static void setEnabled(boolean value) {
        setValue("enabled", value);
    }

    public static void setFontName(String value) {
        setValue("font.name", value);
    }

    public static void setFontFallbacks(List<String> value) {
        setValue("font.fallbacks", value == null ? Collections.emptyList() : new ArrayList<>(value));
    }

    public static void setFontStyle(int value) {
        setValue("font.style", value);
    }

    public static void setFontVariableWeight(int value) {
        setValue("font.variableWeight", Math.max(0, Math.min(1000, value)));
    }

    public static void setCosmicRegularFont(String value) {
        setValue("font.cosmic.regular", value == null ? "" : value.trim());
    }

    public static void setCosmicBoldFont(String value) {
        setValue("font.cosmic.bold", value == null ? "" : value.trim());
    }

    public static void setCosmicItalicFont(String value) {
        setValue("font.cosmic.italic", value == null ? "" : value.trim());
    }

    public static void setCosmicBoldItalicFont(String value) {
        setValue("font.cosmic.boldItalic", value == null ? "" : value.trim());
    }

    public static void setCosmicVariantOverridesOnlySwitchFont(boolean value) {
        setValue("font.cosmic.variantOverridesOnlySwitchFont", value);
    }

    public static void setFontSize(float value) {
        setValue("font.size", value);
    }

    public static void setFontOversample(float value) {
        setValue("font.oversample", value);
    }

    public static void setFontAutoBaseline(boolean value) {
        setValue("font.autoBaseline", value);
    }

    public static void setFontBaselineShift(float value) {
        setValue("font.baselineShift", value);
    }

    public static void setFontReferenceBaseline(float value) {
        setValue("font.referenceBaseline", value);
    }

    public static void setFontAntialias(boolean value) {
        setValue("font.antialias", value);
    }

    public static void setFontAntialiasMode(String value) {
        String mode = normalizeAntialiasMode(value);
        setValue("font.antialiasMode", mode);
        setValue("font.antialias", !"off".equals(mode));
    }

    public static void setFontFractionalMetrics(boolean value) {
        setValue("font.fractionalMetrics", value);
    }

    public static void setFontLcdSubpixel(boolean value) {
        setValue("font.lcdSubpixel", value);
    }

    public static void setBuiltinFallbacksEnabled(boolean value) {
        setValue("font.builtinFallbacks", value);
    }

    public static void setShadowLength(float value) {
        setValue("shadow.length", value);
    }

    public static void setShadowOpacity(float value) {
        setValue("shadow.opacity", value);
    }

    public static void setShadowMode(String value) {
        setValue("shadow.mode", normalizeShadowMode(value));
    }
    public static void setShadowMaskFonts(String value) { setValue("shadow.maskFonts", value == null ? "" : value.trim()); }
    public static void setShadowMaskCodepoints(String value) { setValue("shadow.maskCodepoints", value == null ? "" : value.trim()); }

    public static void setFixImeInput(boolean value) {
        setValue("fixImeInput", value);
    }

    public static void setAllowSignPaste(boolean value) {
        setValue("input.allowSignPaste", value);
    }

    public static void setFixUnicodeTextDeletion(boolean value) {
        setValue("input.fixUnicodeTextDeletion", value);
    }

    public static void setLaboratoryHexChat(boolean value) {
        setValue("laboratory.hexChat", value);
    }

    public static void setLaboratoryTextUndoRedo(boolean value) {
        setValue("laboratory.textUndoRedo", value);
    }

    public static void setCompatModernSplash(boolean value) {
        setValue("compat.modernsplash.enabled", value);
    }

    public static void setSplashFontOverrideEnabled(boolean value) {
        setValue("splash.enabled", value);
    }

    public static void setRenderingInterpolation(boolean value) {
        setValue("rendering.interpolation", value);
    }

    public static void setRenderingMipmap(boolean value) {
        setValue("rendering.mipmap", value);
    }

    public static void setAdaptiveRasterScale(boolean value) {
        setValue("rendering.adaptiveRasterScale", value);
    }

    public static void setAdaptiveRasterMin(float value) {
        setValue("rendering.adaptiveRasterMin", value);
    }

    public static void setAdaptiveRasterMax(float value) {
        setValue("rendering.adaptiveRasterMax", value);
    }

    public static void setAdaptiveRasterStep(float value) {
        setValue("rendering.adaptiveRasterStep", value);
    }

    public static void setExcludeIntegerScale(boolean value) {
        setValue("rendering.excludeIntegerScale", value);
    }

    public static void setExcludeHighMagnification(boolean value) {
        setValue("rendering.excludeHighMagnification", value);
    }

    public static void setLimitMagnification(float value) {
        setValue("rendering.limitMagnification", value);
    }

    public static void setScaleRoundingTolerance(float value) {
        setValue("rendering.scaleRoundingTolerance", value);
    }

    public static void setMipmapLodBias(float value) {
        setValue("rendering.mipmapLodBias", value);
    }

    public static void setOverlayMipmapLodBias(float value) {
        setValue("rendering.overlayMipmapLodBias", value);
    }

    public static void setAnisotropicFiltering(boolean value) {
        setValue("rendering.anisotropicFiltering", value);
    }

    public static void setBlurReductionThreshold(float value) {
        setValue("rendering.blurReduction", value);
    }

    public static void setSmoothShadowThreshold(float value) {
        setValue("rendering.smoothShadowThreshold", value);
    }

    public static void setEnhancedTextPipeline(boolean value) {
        setValue("rendering.enhancedTextPipeline", value);
    }

    public static void setShaderTextPipeline(boolean value) {
        setValue("rendering.shaderTextPipeline", value);
    }

    public static void setRenderingBrightness(float value) {
        setValue("rendering.brightness", value);
    }

    public static void setTextureEdgeBleed(boolean value) {
        setValue("rendering.textureEdgeBleed", value);
    }

    public static void setRenderingBrightnessAuto(boolean value) {
        setValue("rendering.brightnessAuto", value);
    }

    public static void setEnablePremultipliedAlpha(boolean value) {
        setValue("rendering.premultipliedAlpha", value);
    }

    public static void setForceBlendForText(boolean value) {
        setValue("rendering.forceBlendForText", value);
    }

    public static void setRenderingEngine(String value) {
        setValue("rendering.engine", normalizeRenderingEngine(value));
    }

    public static void setSkiaAdvancedStringMode(boolean value) {
        setValue("rendering.skiaAdvancedStringMode", value);
    }

    public static void setSkiaGpuOffscreen(boolean value) {
        setValue("rendering.skiaGpuOffscreen", value);
    }

    public static void setSkiaGpuSubmitViaCpuTexture(boolean value) {
        setValue("rendering.skiaGpuSubmitViaCpuTexture", value);
    }

    public static void setSkiaMonochromeText(boolean value) {
        setValue("rendering.skiaMonochromeText", value);
    }

    public static void setPerformanceAsyncInit(boolean value) {
        setValue("performance.asyncInit", value);
    }

    public static void setPerformancePrewarmBasicLatin(boolean value) {
        setValue("performance.prewarmBasicLatin", value);
    }

    public static void setSignModelLod(boolean value) {
        setValue("performance.signModelLod", value);
    }

    public static void setSignCrossTileBatching(boolean value) {
        setValue("performance.signCrossTileBatching", value);
    }

    public static void setSignBlockOcclusionCulling(boolean value) {
        setValue("performance.signBlockOcclusionCulling", value);
    }

    public static void setSkiaTextCacheMinEntries(int value) {
        setValue("performance.skiaTextCacheMinEntries", value);
    }

    public static void setSkiaTextCacheMaxEntries(int value) {
        setValue("performance.skiaTextCacheMaxEntries", value);
    }

    public static void setSkiaTextCacheTtlSeconds(float value) {
        setValue("performance.skiaTextCacheTtlSeconds", value);
    }

    public static void setSkiaMeasureCacheMaxEntries(int value) {
        setValue("performance.skiaMeasureCacheMaxEntries", value);
    }

    public static void setSkiaSegmentTextureCacheMinEntries(int value) {
        setValue("performance.skiaSegmentTextureCacheMinEntries", value);
    }

    public static void setSkiaSegmentTextureCacheMaxEntries(int value) {
        setValue("performance.skiaSegmentTextureCacheMaxEntries", value);
    }

    public static void setSkiaSegmentTextureCacheTtlSeconds(float value) {
        setValue("performance.skiaSegmentTextureCacheTtlSeconds", value);
    }

    public static void setDebugRenderStats(boolean value) {
        cachedDebugRenderStats = value;
        setValue("debug.renderStats", value);
    }

    public static void save() {
        if (config != null) {
            config.save();
        }
    }

    /** Internal bridge used by the public extension-config API. */
    public static synchronized Object getExtensionValue(String key, Object defaultValue) {
        ensureLoadedForExtensionApi();
        return config.getOrElse(key, defaultValue);
    }

    public static synchronized boolean hasExtensionValue(String key) {
        ensureLoadedForExtensionApi();
        return config.contains(key);
    }

    public static synchronized Object removeExtensionValue(String key) {
        ensureLoadedForExtensionApi();
        return config.remove(key);
    }

    /** Internal bridge used by the public extension-config API. */
    public static synchronized void setExtensionValue(String key, Object value) {
        ensureLoadedForExtensionApi();
        config.set(key, value);
    }

    /** Internal bridge used by the public extension-config API. */
    public static synchronized void setExtensionComment(String key, String comment) {
        ensureLoadedForExtensionApi();
        config.setComment(key, comment);
    }

    /** Internal bridge used by the public extension-config API. */
    public static synchronized void saveExtensionValues() {
        ensureLoadedForExtensionApi();
        config.save();
    }

    private static void ensureLoadedForExtensionApi() {
        if (config == null) load();
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
        cached = Snapshot.from(config);
        cachedDebugRenderStats = cached.debugRenderStats;
    }

    private static void setValue(String key, Object value) {
        config.set(key, value);
        refreshCachedOptions();
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
            w.write("variableWeight = 0\n");
            w.write("size = 8.5\n");
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
            w.write("[font.cosmic]\n");
            w.write("regular = \"\"\n");
            w.write("bold = \"\"\n");
            w.write("italic = \"\"\n");
            w.write("boldItalic = \"\"\n");
            w.write("variantOverridesOnlySwitchFont = false\n");
            w.write("\n");
            w.write("[shadow]\n");
            w.write("length = 1.0\n");
            w.write("opacity = 0.25\n");
            w.write("mode = \"mask\"\n");
            w.write("maskFonts = \"\"\n");
            w.write("maskCodepoints = \"\"\n");
            w.write("\n");
            w.write("[rendering]\n");
            w.write("engine = \"cosmic\"\n");
            w.write("skiaAdvancedStringMode = true\n");
            w.write("skiaGpuOffscreen = true\n");
            w.write("skiaGpuSubmitViaCpuTexture = false\n");
            w.write("skiaMonochromeText = true\n");
            w.write("skiaSegmentCache = true\n");
            w.write("skiaSegmentCacheMinRunLength = 8\n");
            w.write("skiaSegmentCacheMaxRunCodePoints = 24\n");
            w.write("skiaSegmentCacheMaxSegments = 96\n");
            w.write("interpolation = true\n");
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
            w.write("signTextLodCulling = true\n");
            w.write("signTextMinPixelHeight = 4.0\n");
            w.write("signTextBatching = true\n");
            w.write("signTextFrustumCulling = true\n");
            w.write("signModelLod = false\n");
            w.write("signModelLodDistance = 24.0\n");
            w.write("signTextNearThreshold = 6.0\n");
            w.write("signTextNearSupersample = 2.5\n");
            w.write("signTextNearMaxRasterScale = 32.0\n");
            w.write("signCrossTileBatching = false\n");
            w.write("signBatchMaxEntries = 4096\n");
            w.write("signBlockOcclusionCulling = true\n");
            w.write("signOcclusionChecksPerFrame = 48\n");
            w.write("signOcclusionCacheMillis = 250\n");
            w.write("signOcclusionMinDistance = 8.0\n");
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
            w.write("fixUnicodeTextDeletion = true\n");
            w.write("\n");
            w.write("[laboratory]\n");
            w.write("hexChat = false\n");
            w.write("textUndoRedo = false\n");
            w.write("\n");
            w.write("[compat]\n");
            w.write("modernsplash.enabled = true\n");
            w.write("\n");
            w.write("[splash]\n");
            w.write("enabled = true\n");
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
        config.setComment("font.variableWeight", "Variable font wght axis for regular text. 0=auto, otherwise 1-1000.");
        config.setComment("font.size", "Font size in pixels. 8.5 is the default.");
        config.setComment("font.oversample", "Rasterization oversampling factor. Raster resolution is size * oversample; 8.0 at size 8.0 is a 64px glyph raster.");
        config.setComment("font.autoBaseline", "Align each font's measured AWT baseline to the Minecraft reference baseline before manual shift.");
        config.setComment("font.baselineShift", "Additional vertical glyph shift in Minecraft pixels after automatic baseline alignment. Positive moves glyphs down.");
        config.setComment("font.referenceBaseline", "Minecraft-space baseline used by autoBaseline. Vanilla 8px UI text is approximately 7.0.");
        config.setComment("font.antialias", "Enable AWT anti-aliasing during glyph rasterization.");
        config.setComment("font.antialiasMode", "AWT text anti-aliasing mode: off, on, gasp, lcd_hrgb, lcd_hbgr, lcd_vrgb, lcd_vbgr.");
        config.setComment("font.fractionalMetrics", "Enable fractional font metrics for more precise positioning.");
        config.setComment("font.lcdSubpixel", "Enable LCD subpixel anti-aliasing in Skia rasterization. Produces sharper text on standard RGB monitors but may show color fringes.");
        config.setComment("font.builtinFallbacks", "Always append bundled fonts, such as Noto Color Emoji, to the fallback family.");
        config.setComment("font.cosmic", "Optional Cosmic face overrides. Empty values use family and variable-weight auto matching.");
        config.setComment("font.cosmic.regular", "Cosmic regular face override: system face name, local font path, or resource location.");
        config.setComment("font.cosmic.bold", "Cosmic bold face override: system face name, local font path, or resource location.");
        config.setComment("font.cosmic.italic", "Cosmic italic face override: system face name, local font path, or resource location.");
        config.setComment("font.cosmic.boldItalic", "Cosmic bold-italic face override: system face name, local font path, or resource location.");
        config.setComment("font.cosmic.variantOverridesOnlySwitchFont", "For non-regular overrides, select the configured font without additionally requesting bold or italic styling. Empty overrides still use automatic family style matching.");
        config.setComment("shadow", "Text shadow rendering options.");
        config.setComment("shadow.length", "Shadow offset distance in pixels.");
        config.setComment("shadow.opacity", "Shadow opacity multiplier (0.0-1.0).");
        config.setComment("shadow.mode", "Shadow mode: all, mask (skip color glyphs), emoji (skip Unicode emoji), or none.");
        config.setComment("shadow.maskFonts", "Comma-separated font families whose displayable code points skip shadows in mask mode.");
        config.setComment("shadow.maskCodepoints", "Comma-separated Unicode code points/ranges whose shadows are skipped, e.g. 1F300-1FAFF,2600-27BF.");
        config.setComment("input.fixUnicodeTextDeletion", "Delete a whole Unicode code point in text fields instead of half of an emoji surrogate pair.");
        config.setComment("laboratory.textUndoRedo", "Enable per-field undo/redo history in vanilla and ModularUI text inputs (Ctrl+Z, Ctrl+Y, Ctrl+Shift+Z).");
        config.setComment("laboratory.hexChat", "Experimental #RRGGBB chat rendering for Skia/Cosmic text backends.");
        config.setComment("compat", "Compatibility options for third-party mods.");
        config.setComment("compat.modernsplash.enabled", "Allow the loading-screen font override to patch ModernSplash when it is installed. Requires splash.enabled and a restart.");
        config.setComment("splash", "Forge loading-screen font replacement options.");
        config.setComment("splash.enabled", "Replace the Forge loading-screen bitmap font with the configured TTF font. Restart required.");
        config.setComment("rendering", "OpenGL texture rendering options.");
        config.setComment("rendering.engine", "Text renderer engine: vanilla, sfr, skia, or cosmic.");
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
        config.setComment("performance.signTextLodCulling", "Use projected-size LOD and screen culling for sign text. The sign model is still rendered.");
        config.setComment("performance.signTextMinPixelHeight", "Do not submit a sign text line when its projected height is below this many physical framebuffer pixels.");
        config.setComment("performance.signTextBatching", "Combine the four text lines of each sign into one centered Skia texture and one draw call. Disable for vanilla-compatible per-line sign rendering.");
        config.setComment("performance.signTextFrustumCulling", "Skip the complete sign renderer when its model bounds are outside the camera frustum.");
        config.setComment("performance.signModelLod", "Replace distant sign board/stick boxes with flat textured geometry using the currently bound sign texture.");
        config.setComment("performance.signModelLodDistance", "Distance in blocks where the low-poly sign model starts.");
        config.setComment("performance.signTextNearThreshold", "Projected pixels per text unit where the close-up high-resolution sign text path starts.");
        config.setComment("performance.signTextNearSupersample", "Close-up sign text raster pixels per projected framebuffer pixel.");
        config.setComment("performance.signTextNearMaxRasterScale", "Maximum close-up sign text raster scale; higher values are sharper but use more texture memory.");
        config.setComment("performance.signCrossTileBatching", "Collect distant vanilla signs during Forge's TESR pass, submit their models once, then draw their text. Requires a restart when enabled from a fully disabled state.");
        config.setComment("performance.signBatchMaxEntries", "Maximum distant signs collected in one TESR pass before later signs fall back to immediate rendering.");
        config.setComment("performance.signBlockOcclusionCulling", "Skip the complete sign TESR when cached multi-point rays are all blocked by opaque full cubes.");
        config.setComment("performance.signOcclusionChecksPerFrame", "Maximum signs whose block occlusion is refreshed per frame; remaining signs use safe cached results or stay visible.");
        config.setComment("performance.signOcclusionCacheMillis", "How long a sign occlusion result remains fresh while the camera stays within half a block.");
        config.setComment("performance.signOcclusionMinDistance", "Never block-occlusion-cull signs closer than this many blocks to avoid near-camera popping.");
        config.setComment("performance.skiaTextCacheMinEntries", "Minimum number of Skia/Cosmic rendered text textures kept when TTL cleanup runs.");
        config.setComment("performance.skiaTextCacheMaxEntries", "Maximum number of Skia/Cosmic rendered text textures kept in the LRU cache.");
        config.setComment("performance.skiaTextCacheTtlSeconds", "Seconds before an unused Skia/Cosmic rendered text texture can be evicted. 0 disables TTL cleanup.");
        config.setComment("performance.skiaMeasureCacheMaxEntries", "Maximum number of Skia/Cosmic text measurement results kept in memory.");
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
        return getFloat(config, key, defaultValue);
    }

    private static float getFloat(CommentedFileConfig source, String key, float defaultValue) {
        if (source == null) {
            return defaultValue;
        }
        Object val = source.get(key);
        if (val instanceof Number) {
            return ((Number) val).floatValue();
        }
        return defaultValue;
    }

    private static int getInt(String key, int defaultValue) {
        return getInt(config, key, defaultValue);
    }

    private static int getInt(CommentedFileConfig source, String key, int defaultValue) {
        if (source == null) {
            return defaultValue;
        }
        Object val = source.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return defaultValue;
    }

    private static final class Snapshot {
        private final boolean enabled;
        private final boolean fixImeInput;
        private final boolean debugImeInput;
        private final boolean debugRenderStats;
        private final boolean allowSignPaste;
        private final boolean fixUnicodeTextDeletion;
        private final boolean laboratoryHexChat;
        private final boolean laboratoryTextUndoRedo;
        private final boolean compatModernSplash;
        private final boolean splashFontOverrideEnabled;
        private final int fontStyle;
        private final int fontVariableWeight;
        private final boolean cosmicVariantOverridesOnlySwitchFont;
        private final float fontSize;
        private final float fontOversample;
        private final boolean fontAutoBaseline;
        private final float fontBaselineShift;
        private final float fontReferenceBaseline;
        private final boolean fontAntialias;
        private final String fontAntialiasMode;
        private final boolean fontFractionalMetrics;
        private final boolean fontLcdSubpixel;
        private final boolean builtinFallbacks;
        private final float shadowLength;
        private final float shadowOpacity;
        private final String shadowMode;
        private final String shadowMaskFonts;
        private final String shadowMaskCodepoints;
        private final String renderingEngine;
        private final boolean skiaAdvancedStringMode;
        private final boolean skiaGpuOffscreen;
        private final boolean skiaGpuSubmitViaCpuTexture;
        private final boolean skiaMonochromeText;
        private final boolean skiaSegmentCache;
        private final int skiaSegmentCacheMinRunLength;
        private final int skiaSegmentCacheMaxRunCodePoints;
        private final int skiaSegmentCacheMaxSegments;
        private final boolean renderingInterpolation;
        private final boolean renderingMipmap;
        private final boolean adaptiveRasterScale;
        private final float adaptiveRasterMin;
        private final float adaptiveRasterMax;
        private final float adaptiveRasterStep;
        private final boolean excludeIntegerScale;
        private final boolean excludeHighMagnification;
        private final float limitMagnification;
        private final float scaleRoundingToleranceRate;
        private final float mipmapLodBias;
        private final float overlayMipmapLodBias;
        private final boolean anisotropicFiltering;
        private final float blurReductionThreshold;
        private final float smoothShadowThreshold;
        private final boolean enhancedTextPipeline;
        private final boolean shaderTextPipeline;
        private final float renderingBrightness;
        private final boolean textureEdgeBleed;
        private final boolean renderingBrightnessAuto;
        private final boolean enablePremultipliedAlpha;
        private final boolean forceBlendForText;
        private final boolean performanceAsyncInit;
        private final boolean performancePrewarmBasicLatin;
        private final boolean signTextLodCulling;
        private final float signTextMinPixelHeight;
        private final boolean signTextBatching;
        private final boolean signTextFrustumCulling;
        private final boolean signModelLod;
        private final float signModelLodDistance;
        private final float signTextNearThreshold;
        private final float signTextNearSupersample;
        private final float signTextNearMaxRasterScale;
        private final boolean signCrossTileBatching;
        private final int signBatchMaxEntries;
        private final boolean signBlockOcclusionCulling;
        private final int signOcclusionChecksPerFrame;
        private final long signOcclusionCacheMillis;
        private final float signOcclusionMinDistance;
        private final int skiaTextCacheMinEntries;
        private final int skiaTextCacheMaxEntries;
        private final float skiaTextCacheTtlSeconds;
        private final int skiaMeasureCacheMaxEntries;
        private final int skiaSegmentTextureCacheMinEntries;
        private final int skiaSegmentTextureCacheMaxEntries;
        private final float skiaSegmentTextureCacheTtlSeconds;

        private Snapshot() {
            enabled = true;
            fixImeInput = true;
            debugImeInput = false;
            debugRenderStats = false;
            allowSignPaste = true;
            fixUnicodeTextDeletion = true;
            laboratoryHexChat = false;
            laboratoryTextUndoRedo = false;
            compatModernSplash = true;
            splashFontOverrideEnabled = true;
            fontStyle = 0;
            fontVariableWeight = 0;
            cosmicVariantOverridesOnlySwitchFont = false;
            fontSize = 8.5F;
            fontOversample = 12.0F;
            fontAutoBaseline = true;
            fontBaselineShift = 0.0F;
            fontReferenceBaseline = 7.0F;
            fontAntialias = true;
            fontAntialiasMode = "on";
            fontFractionalMetrics = true;
            fontLcdSubpixel = false;
            builtinFallbacks = true;
            shadowLength = 1.0F;
            shadowOpacity = 0.25F;
            shadowMode = "mask";
            shadowMaskFonts = "";
            shadowMaskCodepoints = "";
            renderingEngine = "cosmic";
            skiaAdvancedStringMode = true;
            skiaGpuOffscreen = true;
            skiaGpuSubmitViaCpuTexture = false;
            skiaMonochromeText = true;
            skiaSegmentCache = true;
            skiaSegmentCacheMinRunLength = 8;
            skiaSegmentCacheMaxRunCodePoints = 24;
            skiaSegmentCacheMaxSegments = 96;
            renderingInterpolation = false;
            renderingMipmap = true;
            adaptiveRasterScale = true;
            adaptiveRasterMin = 1.5F;
            adaptiveRasterMax = 14.0F;
            adaptiveRasterStep = 0.5F;
            excludeIntegerScale = true;
            excludeHighMagnification = true;
            limitMagnification = 3.0F;
            scaleRoundingToleranceRate = 0.005F;
            mipmapLodBias = -0.3F;
            overlayMipmapLodBias = -0.5F;
            anisotropicFiltering = true;
            blurReductionThreshold = 10.0F;
            smoothShadowThreshold = 24.0F;
            enhancedTextPipeline = false;
            shaderTextPipeline = false;
            renderingBrightness = 0.0F;
            textureEdgeBleed = false;
            renderingBrightnessAuto = true;
            enablePremultipliedAlpha = false;
            forceBlendForText = true;
            performanceAsyncInit = true;
            performancePrewarmBasicLatin = true;
            signTextLodCulling = true;
            signTextMinPixelHeight = 4.0F;
            signTextBatching = true;
            signTextFrustumCulling = true;
            signModelLod = false;
            signModelLodDistance = 24.0F;
            signTextNearThreshold = 6.0F;
            signTextNearSupersample = 2.5F;
            signTextNearMaxRasterScale = 32.0F;
            signCrossTileBatching = false;
            signBatchMaxEntries = 4096;
            signBlockOcclusionCulling = true;
            signOcclusionChecksPerFrame = 48;
            signOcclusionCacheMillis = 250L;
            signOcclusionMinDistance = 8.0F;
            skiaTextCacheMinEntries = 256;
            skiaTextCacheMaxEntries = 2048;
            skiaTextCacheTtlSeconds = 300.0F;
            skiaMeasureCacheMaxEntries = 4096;
            skiaSegmentTextureCacheMinEntries = 512;
            skiaSegmentTextureCacheMaxEntries = 4096;
            skiaSegmentTextureCacheTtlSeconds = 600.0F;
        }

        private Snapshot(CommentedFileConfig config) {
            enabled = config.getOrElse("enabled", true);
            fixImeInput = config.getOrElse("fixImeInput", true);
            debugImeInput = config.getOrElse("debug.imeInput", false);
            debugRenderStats = config.getOrElse("debug.renderStats", false);
            allowSignPaste = config.getOrElse("input.allowSignPaste", true);
            fixUnicodeTextDeletion = config.getOrElse("input.fixUnicodeTextDeletion", true);
            laboratoryHexChat = config.getOrElse("laboratory.hexChat", false);
            laboratoryTextUndoRedo = config.getOrElse("laboratory.textUndoRedo", false);
            compatModernSplash = config.getOrElse("compat.modernsplash.enabled", true);
            splashFontOverrideEnabled = config.getOrElse("splash.enabled", true);
            fontStyle = config.getOrElse("font.style", 0);
            fontVariableWeight = Math.max(0, Math.min(1000, getInt(config, "font.variableWeight", 0)));
            cosmicVariantOverridesOnlySwitchFont = config.getOrElse("font.cosmic.variantOverridesOnlySwitchFont", false);
            fontSize = getFloat(config, "font.size", 8.5F);
            fontOversample = getFloat(config, "font.oversample", 12.0F);
            fontAutoBaseline = config.getOrElse("font.autoBaseline", true);
            fontBaselineShift = getFloat(config, "font.baselineShift", 0.0F);
            fontReferenceBaseline = getFloat(config, "font.referenceBaseline", 7.0F);
            fontAntialias = config.getOrElse("font.antialias", true);
            fontAntialiasMode = normalizeAntialiasMode(config.getOrElse("font.antialiasMode", fontAntialias ? "on" : "off"));
            fontFractionalMetrics = config.getOrElse("font.fractionalMetrics", true);
            fontLcdSubpixel = config.getOrElse("font.lcdSubpixel", false);
            builtinFallbacks = config.getOrElse("font.builtinFallbacks", true);
            shadowLength = getFloat(config, "shadow.length", 1.0F);
            shadowOpacity = getFloat(config, "shadow.opacity", 0.25F);
            shadowMode = normalizeShadowMode(config.getOrElse("shadow.mode", "mask"));
            shadowMaskFonts = config.getOrElse("shadow.maskFonts", "");
            shadowMaskCodepoints = config.getOrElse("shadow.maskCodepoints", "");
            renderingEngine = normalizeRenderingEngine(config.getOrElse("rendering.engine", "cosmic"));
            skiaAdvancedStringMode = config.getOrElse("rendering.skiaAdvancedStringMode", true);
            skiaGpuOffscreen = config.getOrElse("rendering.skiaGpuOffscreen", true);
            skiaGpuSubmitViaCpuTexture = config.getOrElse("rendering.skiaGpuSubmitViaCpuTexture", false);
            skiaMonochromeText = config.getOrElse("rendering.skiaMonochromeText", true);
            skiaSegmentCache = config.getOrElse("rendering.skiaSegmentCache", true);
            skiaSegmentCacheMinRunLength = Math.max(1, getInt(config, "rendering.skiaSegmentCacheMinRunLength", 8));
            skiaSegmentCacheMaxRunCodePoints = Math.max(1, getInt(config, "rendering.skiaSegmentCacheMaxRunCodePoints", 24));
            skiaSegmentCacheMaxSegments = Math.max(2, getInt(config, "rendering.skiaSegmentCacheMaxSegments", 96));
            renderingInterpolation = config.getOrElse("rendering.interpolation", true);
            renderingMipmap = config.getOrElse("rendering.mipmap", true);
            adaptiveRasterScale = config.getOrElse("rendering.adaptiveRasterScale", true);
            adaptiveRasterMin = getFloat(config, "rendering.adaptiveRasterMin", 1.5F);
            adaptiveRasterMax = getFloat(config, "rendering.adaptiveRasterMax", 14.0F);
            adaptiveRasterStep = getFloat(config, "rendering.adaptiveRasterStep", 0.5F);
            excludeIntegerScale = config.getOrElse("rendering.excludeIntegerScale", true);
            excludeHighMagnification = config.getOrElse("rendering.excludeHighMagnification", true);
            limitMagnification = getFloat(config, "rendering.limitMagnification", 3.0F);
            scaleRoundingToleranceRate = getFloat(config, "rendering.scaleRoundingTolerance", 0.5F) * 0.01F;
            mipmapLodBias = getFloat(config, "rendering.mipmapLodBias", -0.3F);
            overlayMipmapLodBias = getFloat(config, "rendering.overlayMipmapLodBias", -0.5F);
            anisotropicFiltering = config.getOrElse("rendering.anisotropicFiltering", true);
            blurReductionThreshold = getFloat(config, "rendering.blurReduction", 10.0F);
            smoothShadowThreshold = getFloat(config, "rendering.smoothShadowThreshold", 24.0F);
            enhancedTextPipeline = config.getOrElse("rendering.enhancedTextPipeline", false);
            shaderTextPipeline = config.getOrElse("rendering.shaderTextPipeline", false);
            renderingBrightness = getFloat(config, "rendering.brightness", 0.0F);
            textureEdgeBleed = config.getOrElse("rendering.textureEdgeBleed", false);
            renderingBrightnessAuto = config.getOrElse("rendering.brightnessAuto", true);
            enablePremultipliedAlpha = config.getOrElse("rendering.premultipliedAlpha", false);
            forceBlendForText = config.getOrElse("rendering.forceBlendForText", true);
            performanceAsyncInit = config.getOrElse("performance.asyncInit", true);
            performancePrewarmBasicLatin = config.getOrElse("performance.prewarmBasicLatin", true);
            signTextLodCulling = config.getOrElse("performance.signTextLodCulling", true);
            signTextMinPixelHeight = Math.max(0.0F, getFloat(config, "performance.signTextMinPixelHeight", 4.0F));
            signTextBatching = config.getOrElse("performance.signTextBatching", true);
            signTextFrustumCulling = config.getOrElse("performance.signTextFrustumCulling", true);
            signModelLod = config.getOrElse("performance.signModelLod", false);
            signModelLodDistance = Math.max(4.0F, getFloat(config, "performance.signModelLodDistance", 24.0F));
            signTextNearThreshold = Math.max(1.0F, getFloat(config, "performance.signTextNearThreshold", 6.0F));
            signTextNearSupersample = Math.max(1.0F, getFloat(config, "performance.signTextNearSupersample", 2.5F));
            signTextNearMaxRasterScale = Math.max(8.0F, getFloat(config, "performance.signTextNearMaxRasterScale", 32.0F));
            signCrossTileBatching = config.getOrElse("performance.signCrossTileBatching", false);
            signBatchMaxEntries = Math.max(64, getInt(config, "performance.signBatchMaxEntries", 4096));
            signBlockOcclusionCulling = config.getOrElse("performance.signBlockOcclusionCulling", true);
            signOcclusionChecksPerFrame = Math.max(1, getInt(config, "performance.signOcclusionChecksPerFrame", 48));
            signOcclusionCacheMillis = Math.max(50L, getInt(config, "performance.signOcclusionCacheMillis", 250));
            signOcclusionMinDistance = Math.max(2.0F, getFloat(config, "performance.signOcclusionMinDistance", 8.0F));
            skiaTextCacheMinEntries = Math.max(0, getInt(config, "performance.skiaTextCacheMinEntries", 256));
            skiaTextCacheMaxEntries = Math.max(1, getInt(config, "performance.skiaTextCacheMaxEntries", 2048));
            skiaTextCacheTtlSeconds = Math.max(0.0F, getFloat(config, "performance.skiaTextCacheTtlSeconds", 300.0F));
            skiaMeasureCacheMaxEntries = Math.max(1, getInt(config, "performance.skiaMeasureCacheMaxEntries", 4096));
            skiaSegmentTextureCacheMinEntries = Math.max(0, getInt(config, "performance.skiaSegmentTextureCacheMinEntries", 512));
            skiaSegmentTextureCacheMaxEntries = Math.max(1, getInt(config, "performance.skiaSegmentTextureCacheMaxEntries", 4096));
            skiaSegmentTextureCacheTtlSeconds = Math.max(0.0F, getFloat(config, "performance.skiaSegmentTextureCacheTtlSeconds", 600.0F));
        }

        private static Snapshot defaults() {
            return new Snapshot();
        }

        private static Snapshot from(CommentedFileConfig config) {
            return new Snapshot(config);
        }
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
            case "cosmic_text":
            case "cosmic":
                return "cosmic";
            default:
                return "sfr";
        }
    }

    private static String normalizeShadowMode(String value) {
        if (value == null) {
            return "mask";
        }
        String mode = value.trim().toLowerCase(Locale.ROOT);
        return "all".equals(mode) || "mask".equals(mode) || "emoji".equals(mode) || "none".equals(mode)
                ? mode : "mask";
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
