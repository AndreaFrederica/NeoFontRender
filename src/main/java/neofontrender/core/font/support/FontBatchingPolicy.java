package neofontrender.core.font.support;

import neofontrender.core.config.NeofontrenderConfig;

/**
 * Decides whether modern AWT text should submit ordinary glyphs through NFR's quad batcher.
 *
 * <p>The policy deliberately detects Actinium by optional class names.  NFR therefore keeps
 * compiling and running with the legacy per-glyph path when Actinium is absent, while the
 * {@code force} mode remains available for an apples-to-apples NFR batch benchmark.</p>
 */
public final class FontBatchingPolicy {
    private static final String ACTINIUM_STREAMING_DRAWER =
            "com.dhj.actinium.render.BufferBuilderStreamingDrawer";
    private static final String ANGELICA_STREAMING_DRAWER =
            "com.gtnewhorizons.angelica.glsm.streaming.TessellatorStreamingDrawer";

    private static volatile Boolean actiniumStreamingAvailable;

    private FontBatchingPolicy() {
    }

    /** Returns whether the AWT renderer may use NFR's ordered quad batcher. */
    public static boolean useAwtBatching() {
        String mode = NeofontrenderConfig.glyphBatchingMode();
        if ("off".equals(mode)) return false;
        if ("force".equals(mode)) return true;
        return actiniumStreamingAvailable();
    }

    /** True when Actinium's generic streaming renderer is available without a hard dependency. */
    public static boolean actiniumStreamingAvailable() {
        Boolean cached = actiniumStreamingAvailable;
        if (cached != null) return cached;
        boolean detected;
        try {
            ClassLoader loader = FontBatchingPolicy.class.getClassLoader();
            Class<?> drawer = Class.forName(ACTINIUM_STREAMING_DRAWER, false, loader);
            // Actinium owns the generic Tessellator interception, while Angelica supplies the
            // streaming implementation. Require both classes and honor Actinium's runtime
            // switch so auto mode never claims a persistent path that has been disabled.
            Class.forName(ANGELICA_STREAMING_DRAWER, false, loader);
            detected = isDrawerEnabled(drawer);
        } catch (Throwable ignored) {
            detected = false;
        }
        actiniumStreamingAvailable = detected;
        return detected;
    }

    private static boolean isDrawerEnabled(Class<?> drawer) {
        try {
            Object enabled = drawer.getMethod("isEnabled").invoke(null);
            return Boolean.TRUE.equals(enabled);
        } catch (NoSuchMethodException ignored) {
            // Older Actinium builds do not expose the probe; class presence is still a valid
            // compatibility signal for their Tessellator interception.
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Human-readable active path for diagnostics and benchmark reports. */
    public static String activePath() {
        if (!useAwtBatching()) return "legacy";
        return actiniumStreamingAvailable() ? "nfr-batch+actinium-stream" : "nfr-batch";
    }
}
