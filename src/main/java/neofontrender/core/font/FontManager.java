package neofontrender.core.font;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourceManager;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.providers.AwtTtfGlyphProvider;
import neofontrender.core.font.providers.MissingGlyphProvider;

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
    private boolean active = false;

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
    public void reload(IResourceManager resourceManager) {
        close(); // dispose old atlas & providers

        if (!NeofontrenderConfig.enabled()) {
            this.active = false;
            return;
        }

        List<GlyphProvider> providers = new ArrayList<>();

        boolean ttfLoaded = false;
        for (String fontName : NeofontrenderConfig.fontFamily()) {
            try {
                AwtTtfGlyphProvider ttf = loadAwtFont(resourceManager, fontName, false);
                if (ttf == null) {
                    neofontrender.NeoFontRender.LOGGER.warn("Skipped unavailable fallback font '{}'", fontName);
                    continue;
                }
                providers.add(ttf);
                ttfLoaded = true;
                neofontrender.NeoFontRender.LOGGER.info("Loaded AWT font '{}' (size={}, oversample={}, autoBaseline={}, baselineShift={})",
                        fontName, NeofontrenderConfig.fontSize(), NeofontrenderConfig.fontOversample(),
                        NeofontrenderConfig.fontAutoBaseline(), NeofontrenderConfig.fontBaselineShift());
            } catch (Exception e) {
                neofontrender.NeoFontRender.LOGGER.error("Failed to load font '{}'", fontName, e);
            }
        }

        if (!ttfLoaded) {
            try {
                AwtTtfGlyphProvider ttf = loadAwtFont(resourceManager, null, true);
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
            return;
        }

        providers.add(new MissingGlyphProvider());

        FontTexture atlas = new FontTexture(textureManager, new net.minecraft.util.ResourceLocation("neofontrender", "default"));
        this.defaultFontSet = new FontSet(providers, atlas);
        this.active = true;
        neofontrender.NeoFontRender.LOGGER.info("FontManager reloaded with {} providers", providers.size());
    }

    private AwtTtfGlyphProvider loadAwtFont(IResourceManager resourceManager, String fontName, boolean allowDefaultFallback) throws Exception {
        return AwtTtfGlyphProvider.load(
                resourceManager,
                fontName,
                NeofontrenderConfig.fontSize(),
                NeofontrenderConfig.fontOversample(),
                0.0F, 0.0F,
                NeofontrenderConfig.fontBaselineShift(),
                NeofontrenderConfig.fontAutoBaseline(),
                NeofontrenderConfig.fontReferenceBaseline(),
                NeofontrenderConfig.fontAntialias(),
                NeofontrenderConfig.fontAntialiasMode(),
                NeofontrenderConfig.fontFractionalMetrics(),
                NeofontrenderConfig.fontStyle(),
                allowDefaultFallback
        );
    }

    public boolean isActive() {
        return active && defaultFontSet != null;
    }

    public FontSet getDefaultFontSet() {
        return defaultFontSet;
    }

    @Override
    public void close() {
        if (defaultFontSet != null) {
            defaultFontSet.close();
            defaultFontSet = null;
        }
        active = false;
    }
}
