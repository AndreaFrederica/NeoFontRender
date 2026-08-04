package neofontrender.core.font.support;

/** Shared color selection for legacy two-pass and modern composed text shadows. */
public final class ShadowColorPolicy {
    private ShadowColorPolicy() {}

    /** Keeps the foreground hue when colored shadows are enabled, otherwise darkens like vanilla. */
    public static int legacyColor(int foregroundArgb, boolean colored) {
        return colored
                ? foregroundArgb
                : (foregroundArgb & 0xFF000000) | ((foregroundArgb & 0xFCFCFC) >> 2);
    }

    /** Keeps legacy behavior while allowing colored shadows to remap selected foreground RGBs. */
    public static int legacyColor(int foregroundArgb, boolean colored,
                                  ShadowColorRemapRules rules, int[] palette) {
        int color = legacyColor(foregroundArgb, colored);
        return colored && rules != null ? rules.remap(color, palette) : color;
    }

    /** Selects the foreground or shadow half of a 32-entry Minecraft palette. */
    public static int paletteIndex(int foregroundIndex, boolean shadow, boolean colored) {
        return foregroundIndex + (shadow && !colored ? 16 : 0);
    }

    /** Selects the per-run foreground hue or the configured global modern-shadow color. */
    public static int modernColor(int foregroundArgb, int configuredArgb, boolean colored) {
        return colored ? foregroundArgb : configuredArgb;
    }

    /** Selects and then remaps a per-run modern colored shadow while preserving run alpha. */
    public static int modernColor(int foregroundArgb, int configuredArgb, boolean colored,
                                  ShadowColorRemapRules rules, int[] palette) {
        int color = modernColor(foregroundArgb, configuredArgb, colored);
        return colored && rules != null ? rules.remap(color, palette) : color;
    }
}
