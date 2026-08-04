package neofontrender.core.font;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.api.color.TextColorPaletteRegistry;
import neofontrender.core.font.awt.FontSet;
import neofontrender.core.font.awt.FontTexture;
import neofontrender.core.font.awt.GlyphProvider;
import neofontrender.core.font.awt.AwtModernTextRenderer;
import neofontrender.core.font.awt.providers.AwtTtfGlyphProvider;
import neofontrender.core.font.awt.providers.MissingGlyphProvider;
import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.font.cosmic.CosmicRuntimeSupport;
import neofontrender.core.font.cosmic.CosmicTextRenderer;
import neofontrender.core.font.support.FontRenderTuning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Top-level manager for the replacement font system.
 * Holds the default {@link FontSet} and handles (re)loading.
 *
 * <p>Equivalent to 1.20.1 {@code net.minecraft.client.gui.font.FontManager}.</p>
 */
public class FontManager implements AutoCloseable {

    public static final FontManager INSTANCE = new FontManager();

    private static final ExecutorService BACKGROUND_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "NFR-FontLoader");
        t.setDaemon(true);
        return t;
    });

    private TextureManager textureManager;
    private IResourceManager resourceManager;
    private FontSet defaultFontSet;
    private TextRenderBackend textRenderBackend;
    private AwtModernTextRenderer modernAwtTextRenderer;
    private final Map<String, TextRenderBackend> scopedBackends = new LinkedHashMap<>();
    private volatile boolean active = false;
    private volatile boolean cosmicActive = false;
    private String backendVersion = "vanilla Minecraft font renderer";
    private int[] legacyColorCodes = TextColorPaletteRegistry.vanillaColorCodes();

    // Async loading state
    private final AtomicReference<CompletableFuture<Void>> pendingReload = new AtomicReference<>();
    private volatile boolean asyncLoading = false;

    private FontManager() {
    }

    /**
     * Initialise the manager with the game's TextureManager.
     * Called from a Mixin once the Minecraft instance is available.
     */
    public void init(TextureManager textureManager) {
        this.textureManager = textureManager;
    }

    /**
     * Load or reload fonts from resources.
     * If asyncInit is enabled, font loading happens on a background thread.
     */
    public void reload(IResourceManager resourceManager) {
        if (NeofontrenderConfig.performanceAsyncInit() && NeofontrenderConfig.useAwtEngine()) {
            reloadAsync(resourceManager);
        } else {
            reloadSync(resourceManager);
        }
    }

    /**
     * Synchronous font loading. Blocks the calling thread.
     */
    public synchronized void reloadSync(IResourceManager resourceManager) {
        closeInternal();
        this.resourceManager = resourceManager;
        reloadInternal(resourceManager);
    }

    /**
     * Asynchronous font loading. Returns immediately, loading happens on background thread.
     * Call {@link #tick()} from the render thread to check completion.
     */
    public void reloadAsync(IResourceManager resourceManager) {
        // Cancel any pending reload
        CompletableFuture<Void> pending = pendingReload.getAndSet(null);
        if (pending != null) {
            pending.cancel(false);
        }

        this.resourceManager = resourceManager;
        this.asyncLoading = true;

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                // Phase 1: Load font files and compute metrics (background thread)
                // This is the expensive part - file I/O and AWT font loading
                neofontrender.NeoFontRender.LOGGER.info("Starting async font loading...");
                reloadSync(resourceManager);
                neofontrender.NeoFontRender.LOGGER.info("Async font loading complete");
            } catch (Exception e) {
                neofontrender.NeoFontRender.LOGGER.error("Async font loading failed", e);
                // Fall back to vanilla
                synchronized (this) {
                    active = false;
                    cosmicActive = false;
                    backendVersion = "vanilla Minecraft font renderer";
                }
            } finally {
                asyncLoading = false;
            }
        }, BACKGROUND_EXECUTOR);

        pendingReload.set(future);
    }

    /**
     * Check if async loading is in progress.
     */
    public boolean isAsyncLoading() {
        return asyncLoading;
    }

    /**
     * Called from the render thread to handle async loading completion.
     * No-op if async loading is not in progress.
     */
    public void tick() {
        // Nothing to do - the async future handles completion
        // This method exists for future use if we need render-thread finalization
    }

    private void reloadInternal(IResourceManager resourceManager) {
        if (NeofontrenderConfig.useVanillaEngine()) {
            this.active = false;
            this.cosmicActive = false;
            this.backendVersion = "vanilla Minecraft font renderer";
            resetVanillaFontTextureFiltering();
            return;
        }

        boolean preferCosmic = NeofontrenderConfig.useCosmicEngine();
        if (preferCosmic) {
            CosmicRuntimeSupport.Compatibility compatibility = CosmicRuntimeSupport.ensureLoaded();
            if (!compatibility.isSupported()) {
                neofontrender.NeoFontRender.LOGGER.warn("Cosmic renderer disabled: {}. Falling back to AWT font renderer",
                        compatibility.getMessage());
            } else {
                try {
                    this.textRenderBackend = new CosmicTextRenderer(textureManager, resourceManager);
                    this.textRenderBackend.updateLegacyColorCodes(legacyColorCodes);
                    if (NeofontrenderConfig.performancePrewarmBasicLatin()) {
                        this.textRenderBackend.prewarmBasicLatin();
                    }
                    this.cosmicActive = this.textRenderBackend.isReady();
                    this.active = false;
                    this.backendVersion = compatibility.getMessage();
                    neofontrender.NeoFontRender.LOGGER.info("FontManager reloaded with Cosmic renderer ({})",
                            compatibility.getMessage());
                    return;
                } catch (Throwable t) {
                    // A native backend is optional. A bad font, locked extracted DLL, or ABI issue
                    // must not prevent Minecraft from reaching its resource reload fallback path.
                    this.textRenderBackend = null;
                    this.cosmicActive = false;
                    neofontrender.NeoFontRender.LOGGER.error(
                            "Failed to initialize Cosmic renderer ({}); falling back to AWT font renderer",
                            compatibility.getMessage(), t);
                }
            }
        }

        List<GlyphProvider> providers = new ArrayList<>();

        boolean ttfLoaded = false;
        float rasterScale = FontRenderTuning.rasterScale(NeofontrenderConfig.fontOversample());
        float effectiveFontSize = NeofontrenderConfig.adaptiveFontSize();
        for (String fontName : NeofontrenderConfig.fontFamily()) {
            try {
                AwtTtfGlyphProvider ttf = loadAwtFont(resourceManager, fontName, rasterScale, false);
                if (ttf == null) {
                    neofontrender.NeoFontRender.LOGGER.warn("Skipped unavailable fallback font '{}'", fontName);
                    continue;
                }
                providers.add(ttf);
                ttfLoaded = true;
                neofontrender.NeoFontRender.LOGGER.info("Loaded AWT font '{}' (size={}, adaptive={}, oversample={} effective={}, autoBaseline={}, baselineShift={})",
                        fontName, effectiveFontSize, NeofontrenderConfig.fontOversample(), rasterScale,
                        NeofontrenderConfig.fontAutoBaseline(), NeofontrenderConfig.fontBaselineShift());
            } catch (Exception e) {
                neofontrender.NeoFontRender.LOGGER.error("Failed to load font '{}'", fontName, e);
            }
        }

        if (ttfLoaded) {
            try {
                AwtTtfGlyphProvider systemFallback = loadAwtFont(resourceManager, null, rasterScale, true);
                if (systemFallback != null) {
                    providers.add(systemFallback);
                    neofontrender.NeoFontRender.LOGGER.info(
                            "Loaded Java SansSerif composite as the final adaptive system-font fallback");
                }
            } catch (Exception e) {
                neofontrender.NeoFontRender.LOGGER.warn("Failed to load adaptive system-font fallback", e);
            }
        }

        if (!ttfLoaded) {
            try {
                AwtTtfGlyphProvider ttf = loadAwtFont(resourceManager, null, rasterScale, true);
                if (ttf != null) {
                    providers.add(ttf);
                    ttfLoaded = true;
                    neofontrender.NeoFontRender.LOGGER.warn("No configured font loaded; using SansSerif fallback");
                }
            } catch (Exception e) {
                neofontrender.NeoFontRender.LOGGER.error("Failed to load default SansSerif fallback", e);
            }
        }

        if (!ttfLoaded) {
            neofontrender.NeoFontRender.LOGGER.warn("No TTF font loaded; keeping vanilla rendering");
            this.active = false;
            this.backendVersion = "vanilla Minecraft font renderer";
            return;
        }

        providers.add(new MissingGlyphProvider());

        FontTexture atlas = new FontTexture(textureManager, new net.minecraft.util.ResourceLocation("neofontrender", "default"),
                rasterScale * FontRenderTuning.textureScale(rasterScale));
        this.defaultFontSet = new FontSet(providers, atlas);
        if (NeofontrenderConfig.performancePrewarmBasicLatin()) {
            this.defaultFontSet.prewarmBasicLatin();
        }
        this.active = true;
        this.cosmicActive = false;
        this.backendVersion = "AWT Java2D font renderer";
        if (preferCosmic) {
            neofontrender.NeoFontRender.LOGGER.info("FontManager reloaded with {} AWT providers after native backend fallback", providers.size());
        } else {
            neofontrender.NeoFontRender.LOGGER.info("FontManager reloaded with {} providers", providers.size());
        }
    }

    private AwtTtfGlyphProvider loadAwtFont(IResourceManager resourceManager, String fontName,
                                            float rasterScale, boolean allowDefaultFallback) throws Exception {
        return AwtTtfGlyphProvider.load(
                resourceManager,
                fontName,
                NeofontrenderConfig.adaptiveFontSize(),
                rasterScale,
                0.0F, 0.0F,
                NeofontrenderConfig.fontBaselineShift(),
                NeofontrenderConfig.fontAutoBaseline(),
                NeofontrenderConfig.fontReferenceBaseline(),
                NeofontrenderConfig.fontAntialias(),
                NeofontrenderConfig.fontAntialiasMode(),
                NeofontrenderConfig.fontFractionalMetrics(),
                NeofontrenderConfig.fontStyle(),
                NeofontrenderConfig.fontVariableWeight(),
                allowDefaultFallback
        );
    }

    private void resetVanillaFontTextureFiltering() {
        resetTextureFiltering(new ResourceLocation("textures/font/ascii.png"));
        resetTextureFiltering(new ResourceLocation("textures/font/ascii_sga.png"));
        for (int page = 0; page < 256; page++) {
            resetTextureFiltering(new ResourceLocation(String.format("textures/font/unicode_page_%02x.png", page)));
        }
    }

    private void resetTextureFiltering(ResourceLocation location) {
        if (textureManager == null) {
            return;
        }
        ITextureObject texture = textureManager.getTexture(location);
        if (texture instanceof AbstractTexture) {
            // AbstractTexture#setBlurMipmap changes the texture currently bound in OpenGL; it
            // does not bind this AbstractTexture itself. Without this bind we only changed the
            // Java-side flags while ascii.png kept GL_LINEAR, causing blurred pixels and atlas
            // bleeding from neighbouring glyph cells after switching back to vanilla.
            textureManager.bindTexture(location);
            ((AbstractTexture) texture).setBlurMipmap(false, false);
        }
    }

    public synchronized boolean isActive() {
        return active && defaultFontSet != null;
    }

    public boolean isSfrActive() {
        return isActive();
    }

    public synchronized boolean isCosmicActive() {
        return cosmicActive && textRenderBackend != null;
    }

    /** Human-readable renderer implementation and ABI shown in the F3 diagnostics. */
    public synchronized String getBackendVersion() {
        return backendVersion;
    }

    public synchronized boolean isTextBackendActive() {
        return cosmicActive && textRenderBackend != null;
    }

    public synchronized FontSet getDefaultFontSet() {
        return defaultFontSet;
    }

    public synchronized FontSet.DebugState getSfrDebugState() {
        return defaultFontSet == null ? null : defaultFontSet.debugState();
    }

    public synchronized TextRenderBackend getTextRenderBackend() {
        return textRenderBackend;
    }

    /** Applies one selected palette to active, modern-size, and scoped backends. */
    public synchronized void updateLegacyColorCodes(int[] colorCodes) {
        int[] normalized = TextColorPaletteRegistry.normalizeColorCodes(colorCodes);
        if (Arrays.equals(legacyColorCodes, normalized)) return;
        legacyColorCodes = normalized;
        if (textRenderBackend != null) textRenderBackend.updateLegacyColorCodes(normalized);
        if (modernAwtTextRenderer != null) modernAwtTextRenderer.updateLegacyColorCodes(normalized);
        for (TextRenderBackend backend : scopedBackends.values()) {
            if (backend != textRenderBackend) backend.updateLegacyColorCodes(normalized);
        }
    }

    /**
     * Backend used by the public native-size text API. Modern native engines are preferred; SFR
     * and vanilla selections receive a lazily-created AWT adapter with true per-size atlases.
     */
    public synchronized TextRenderBackend getModernTextBackend() {
        if (textRenderBackend != null && textRenderBackend.isReady()
                && textRenderBackend.supportsNativeFontSize()) {
            return textRenderBackend;
        }
        if (modernAwtTextRenderer == null && textureManager != null && resourceManager != null) {
            modernAwtTextRenderer = new AwtModernTextRenderer(textureManager, resourceManager);
            modernAwtTextRenderer.updateLegacyColorCodes(legacyColorCodes);
        }
        return modernAwtTextRenderer != null && modernAwtTextRenderer.isReady()
                ? modernAwtTextRenderer : null;
    }

    /** Returns a cached backend for a scoped font request. Custom font lists use the AWT adapter;
     * COSMIC/AUTO use the active native backend when the request does not override the family. */
    public synchronized TextRenderBackend getScopedTextBackend(neofontrender.api.text.FontRenderSpec spec) {
        if (spec == null || spec.backend() == neofontrender.api.text.FontRenderBackend.VANILLA) return null;
        boolean requestCosmic = spec.backend() == neofontrender.api.text.FontRenderBackend.COSMIC
                || spec.backend() == neofontrender.api.text.FontRenderBackend.AUTO && isCosmicActive();
        if (requestCosmic && spec.fonts().isEmpty() && textRenderBackend != null && textRenderBackend.isReady()) {
            return textRenderBackend;
        }
        String key = spec.backend() + "|" + spec.size() + "|" + spec.fonts();
        TextRenderBackend cached = scopedBackends.get(key);
        if (cached != null && cached.isReady()) return cached;
        if (textureManager == null || resourceManager == null) return null;
        TextRenderBackend created = null;
        if (requestCosmic && CosmicRuntimeSupport.ensureLoaded().isSupported()) {
            try {
                created = new CosmicTextRenderer(textureManager, resourceManager, spec);
            } catch (Exception error) {
                neofontrender.NeoFontRender.LOGGER.warn(
                        "Scoped Cosmic renderer failed; falling back to AWT", error);
            }
        }
        if (created == null) created = new AwtModernTextRenderer(textureManager, resourceManager,
                spec.fonts().isEmpty() ? NeofontrenderConfig.fontFamily() : spec.fonts());
        created.updateLegacyColorCodes(legacyColorCodes);
        scopedBackends.put(key, created);
        return created;
    }

    public synchronized CosmicTextRenderer getCosmicTextRenderer() {
        return textRenderBackend instanceof CosmicTextRenderer ? (CosmicTextRenderer) textRenderBackend : null;
    }

    @Override
    public synchronized void close() {
        closeInternal();
    }

    private void closeInternal() {
        if (modernAwtTextRenderer != null) {
            modernAwtTextRenderer.close();
            modernAwtTextRenderer = null;
        }
        if (defaultFontSet != null) {
            defaultFontSet.close();
            defaultFontSet = null;
        }
        if (textRenderBackend != null) {
            textRenderBackend.close();
            textRenderBackend = null;
        }
        for (TextRenderBackend backend : scopedBackends.values()) {
            if (backend != textRenderBackend) backend.close();
        }
        scopedBackends.clear();
        active = false;
        cosmicActive = false;
    }
}
