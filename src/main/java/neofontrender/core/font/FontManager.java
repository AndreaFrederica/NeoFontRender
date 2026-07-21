package neofontrender.core.font;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.awt.FontSet;
import neofontrender.core.font.awt.FontTexture;
import neofontrender.core.font.awt.GlyphProvider;
import neofontrender.core.font.awt.providers.AwtTtfGlyphProvider;
import neofontrender.core.font.awt.providers.MissingGlyphProvider;
import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.font.cosmic.CosmicRuntimeSupport;
import neofontrender.core.font.cosmic.CosmicTextRenderer;
import neofontrender.core.font.support.FontRenderTuning;
import neofontrender.core.font.skia.SkijaRuntimeSupport;
import neofontrender.core.font.skia.SkijaTextRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-level manager for the replacement font system.
 * Holds the default {@link FontSet} and handles (re)loading.
 *
 * <p>Equivalent to 1.20.1 {@code net.minecraft.client.gui.font.FontManager}.</p>
 */
public class FontManager implements AutoCloseable {

    public static final FontManager INSTANCE = new FontManager();

    private TextureManager textureManager;
    private FontSet defaultFontSet;
    private TextRenderBackend textRenderBackend;
    private boolean active = false;
    private boolean skiaActive = false;
    private boolean cosmicActive = false;
    private String backendVersion = "vanilla Minecraft font renderer";

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
     */
    public synchronized void reload(IResourceManager resourceManager) {
        close(); // dispose old atlas & providers

        if (NeofontrenderConfig.useVanillaEngine()) {
            this.active = false;
            this.skiaActive = false;
            this.cosmicActive = false;
            this.backendVersion = "vanilla Minecraft font renderer";
            resetVanillaFontTextureFiltering();
            return;
        }

        boolean preferSkia = NeofontrenderConfig.useSkiaEngine();
        if (preferSkia) {
            SkijaRuntimeSupport.Compatibility compatibility = SkijaRuntimeSupport.checkCompatibility();
            if (!compatibility.isSupported()) {
                neofontrender.NeoFontRender.LOGGER.warn("Skija renderer disabled: {}. Falling back to vanilla font renderer",
                        compatibility.getMessage());
                // A missing optional Skija add-on should be obvious. Silently selecting AWT or
                // Cosmic makes a stale `rendering.engine=skia` config look successful and hides a
                // broken installation, so preserve the configured value but render with vanilla.
                this.active = false;
                this.skiaActive = false;
                this.cosmicActive = false;
                this.backendVersion = "vanilla Minecraft font renderer";
                resetVanillaFontTextureFiltering();
                return;
            } else {
                try {
                    this.textRenderBackend = new SkijaTextRenderer(textureManager, resourceManager);
                    if (NeofontrenderConfig.performancePrewarmBasicLatin()) {
                        this.textRenderBackend.prewarmBasicLatin();
                    }
                    this.skiaActive = this.textRenderBackend.isReady();
                    this.active = false;
                    this.backendVersion = "Skija " + compatibility.getMessage();
                    neofontrender.NeoFontRender.LOGGER.info("FontManager reloaded with Skija renderer ({})", compatibility.getMessage());
                    return;
                } catch (Throwable t) {
                    this.textRenderBackend = null;
                    this.skiaActive = false;
                    this.backendVersion = "vanilla Minecraft font renderer";
                    neofontrender.NeoFontRender.LOGGER.error(
                            "Failed to initialize Skija renderer ({}); falling back to vanilla font renderer",
                            compatibility.getMessage(),
                            t);
                    resetVanillaFontTextureFiltering();
                    return;
                }
            }
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
                    if (NeofontrenderConfig.performancePrewarmBasicLatin()) {
                        this.textRenderBackend.prewarmBasicLatin();
                    }
                    this.cosmicActive = this.textRenderBackend.isReady();
                    this.skiaActive = false;
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
        for (String fontName : NeofontrenderConfig.fontFamily()) {
            try {
                AwtTtfGlyphProvider ttf = loadAwtFont(resourceManager, fontName, rasterScale, false);
                if (ttf == null) {
                    neofontrender.NeoFontRender.LOGGER.warn("Skipped unavailable fallback font '{}'", fontName);
                    continue;
                }
                providers.add(ttf);
                ttfLoaded = true;
                neofontrender.NeoFontRender.LOGGER.info("Loaded AWT font '{}' (size={}, oversample={} effective={}, autoBaseline={}, baselineShift={})",
                        fontName, NeofontrenderConfig.fontSize(), NeofontrenderConfig.fontOversample(), rasterScale,
                        NeofontrenderConfig.fontAutoBaseline(), NeofontrenderConfig.fontBaselineShift());
            } catch (Exception e) {
                neofontrender.NeoFontRender.LOGGER.error("Failed to load font '{}'", fontName, e);
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
        this.skiaActive = false;
        this.cosmicActive = false;
        this.backendVersion = "AWT Java2D font renderer";
        if (preferSkia || preferCosmic) {
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
                NeofontrenderConfig.fontSize(),
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

    public synchronized boolean isSkiaActive() {
        return skiaActive && textRenderBackend != null;
    }

    public synchronized boolean isCosmicActive() {
        return cosmicActive && textRenderBackend != null;
    }

    /** Human-readable renderer implementation and ABI shown in the F3 diagnostics. */
    public synchronized String getBackendVersion() {
        return backendVersion;
    }

    public synchronized boolean isTextBackendActive() {
        return (skiaActive || cosmicActive) && textRenderBackend != null;
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

    public synchronized SkijaTextRenderer getSkijaTextRenderer() {
        return textRenderBackend instanceof SkijaTextRenderer ? (SkijaTextRenderer) textRenderBackend : null;
    }

    public synchronized CosmicTextRenderer getCosmicTextRenderer() {
        return textRenderBackend instanceof CosmicTextRenderer ? (CosmicTextRenderer) textRenderBackend : null;
    }

    @Override
    public synchronized void close() {
        if (defaultFontSet != null) {
            defaultFontSet.close();
            defaultFontSet = null;
        }
        if (textRenderBackend != null) {
            textRenderBackend.close();
            textRenderBackend = null;
        }
        active = false;
        skiaActive = false;
        cosmicActive = false;
    }
}
