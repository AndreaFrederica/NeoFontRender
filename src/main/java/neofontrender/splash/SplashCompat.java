package neofontrender.splash;

import neofontrender.NeoFontRender;
import neofontrender.core.config.NeofontrenderConfig;
import org.apache.logging.log4j.Logger;

/** Runtime bridge called by the patched ModernSplash font renderer. */
public final class SplashCompat {

    private static final Logger LOGGER = NeoFontRender.LOGGER;

    private static volatile SplashAwtBackend backend;
    private static volatile boolean initialized;
    private static volatile boolean installed;

    private SplashCompat() {}

    /**
     * Initializes on the splash thread, where ModernSplash's shared OpenGL context is current.
     * Returning {@code false} lets the injected renderer methods fall back to vanilla rendering.
     */
    @SuppressWarnings("unused")
    public static boolean isOverrideActive() {
        if (!initialized) {
            initialize();
        }
        return installed;
    }

    private static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        try {
            if (!NeofontrenderConfig.isLoaded()) {
                NeofontrenderConfig.load();
            }
            if (!NeofontrenderConfig.splashFontOverrideEnabled()) {
                LOGGER.info("Loading-screen font override is disabled in config");
                return;
            }

            boolean modernSplash = ModernSplashDetector.isInstalled();
            if (modernSplash && !NeofontrenderConfig.compatModernSplash()) {
                LOGGER.info("ModernSplash font override is disabled in config");
                return;
            }

            backend = new SplashAwtBackend();
            installed = true;
            LOGGER.info("Installed AWT font override for {} loading screen",
                    modernSplash ? "ModernSplash" : "Forge");
        } catch (SplashFontWeight.Fallback fallback) {
            backend = null;
            installed = false;
            LOGGER.warn("Loading-screen AWT font cannot satisfy the requested weight; "
                    + "using its bitmap font: {}", fallback.getMessage());
        } catch (Throwable t) {
            backend = null;
            installed = false;
            LOGGER.error("Failed to initialize loading-screen font override; using its bitmap font", t);
        }
    }

    @SuppressWarnings("unused")
    public static int getStringWidth(String text) {
        SplashAwtBackend active = backend;
        if (active == null || text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(active.measureString(text));
    }

    @SuppressWarnings("unused")
    public static int drawString(String text, int x, int y, int color) {
        SplashAwtBackend active = backend;
        if (active == null || text == null || text.isEmpty()) {
            return x;
        }
        active.drawString(text, x, y, color);
        return (int) (x + active.measureString(text));
    }

    public static boolean isInstalled() {
        return installed;
    }
}
