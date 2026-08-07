package neofontrender.addons.flight;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

import java.util.Locale;

/** User-facing modes for optional Shoulder Surfing crosshair repairs. */
public final class ShoulderSurfingFixConfig {
    public static final String MODE_PATCHED = "patched";
    public static final String MODE_ADAPTIVE = "adaptive";
    public static final String MODE_STATIC = "static";
    public static final String MODE_DUAL = "dual";
    public static final String MODE_OFF = "off";

    private static final String MODE_KEY = "compat.shoulderSurfing.crosshairMode";
    private static String mode = MODE_PATCHED;

    private ShoulderSurfingFixConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define(MODE_KEY, MODE_PATCHED,
                "Shoulder Surfing crosshair mode: patched, adaptive, static, dual, or off.");
        mode = normalize(file.getString(MODE_KEY, MODE_PATCHED));
        file.save();
    }

    static void save() {
        mode = normalize(mode);
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.set(MODE_KEY, mode);
        file.save();
    }

    public static String mode() { return mode; }
    static void setMode(String value) { mode = normalize(value); }
    public static boolean enabled() { return !MODE_OFF.equals(mode); }
    public static boolean patched() { return MODE_PATCHED.equals(mode); }
    public static boolean adaptive() { return MODE_ADAPTIVE.equals(mode); }
    public static boolean staticMode() { return MODE_STATIC.equals(mode); }
    public static boolean dual() { return MODE_DUAL.equals(mode); }

    private static String normalize(String value) {
        String normalized = value == null ? MODE_PATCHED
                : value.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case MODE_PATCHED:
            case MODE_ADAPTIVE:
            case MODE_STATIC:
            case MODE_DUAL:
            case MODE_OFF:
                return normalized;
            default:
                return MODE_PATCHED;
        }
    }
}
