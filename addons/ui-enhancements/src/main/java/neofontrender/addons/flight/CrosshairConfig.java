package neofontrender.addons.flight;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

import java.util.Locale;

final class CrosshairConfig {
    static boolean customEnabled;
    static boolean hideVanillaDuringFlightHud = true;
    static boolean hideForgeLayerDuringFlightHud;
    static String style = "cross";
    static int color = 0xFFFFFFFF;
    static int scalePercent = 100;
    static int gap = 3;
    static int armLength = 5;
    static int thickness = 1;

    private CrosshairConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("crosshair.customEnabled", false,
                        "Replace vanilla's crosshair only when no earlier Forge handler takes ownership.")
                .define("crosshair.hideVanillaDuringFlightHud", true,
                        "Allow themes using HIDE_VANILLA to suppress Minecraft's vanilla/custom crosshair.")
                .define("crosshair.hideForgeLayerDuringFlightHud", false,
                        "Cancel Forge's CROSSHAIRS layer too; this can also hide mod crosshairs.")
                .define("crosshair.style", "cross", "cross, dot, circle, or chevron")
                .define("crosshair.color", "#FFFFFFFF", "Custom crosshair ARGB color.")
                .define("crosshair.scalePercent", 100, "Custom crosshair scale percentage.")
                .define("crosshair.gap", 3, "Center gap in pixels at 100% scale.")
                .define("crosshair.armLength", 5, "Arm length in pixels at 100% scale.")
                .define("crosshair.thickness", 1, "Line thickness in pixels.");
        customEnabled = file.getBoolean("crosshair.customEnabled", false);
        hideVanillaDuringFlightHud = file.getBoolean(
                "crosshair.hideVanillaDuringFlightHud", true);
        hideForgeLayerDuringFlightHud = file.getBoolean(
                "crosshair.hideForgeLayerDuringFlightHud", false);
        style = normalizeStyle(file.getString("crosshair.style", "cross"));
        color = parseColor(file.getString("crosshair.color", "#FFFFFFFF"), 0xFFFFFFFF);
        scalePercent = file.getInt("crosshair.scalePercent", 100, 50, 300);
        gap = file.getInt("crosshair.gap", 3, 0, 16);
        armLength = file.getInt("crosshair.armLength", 5, 1, 24);
        thickness = file.getInt("crosshair.thickness", 1, 1, 6);
        file.save();
    }

    static void save() {
        style = normalizeStyle(style);
        scalePercent = clamp(scalePercent, 50, 300);
        gap = clamp(gap, 0, 16);
        armLength = clamp(armLength, 1, 24);
        thickness = clamp(thickness, 1, 6);
        UiEnhancementsConfig.file()
                .set("crosshair.customEnabled", customEnabled)
                .set("crosshair.hideVanillaDuringFlightHud", hideVanillaDuringFlightHud)
                .set("crosshair.hideForgeLayerDuringFlightHud", hideForgeLayerDuringFlightHud)
                .set("crosshair.style", style)
                .set("crosshair.color", String.format(Locale.ROOT, "#%08X", color))
                .set("crosshair.scalePercent", scalePercent)
                .set("crosshair.gap", gap)
                .set("crosshair.armLength", armLength)
                .set("crosshair.thickness", thickness)
                .save();
    }

    private static String normalizeStyle(String value) {
        String normalized = value == null ? "cross" : value.trim().toLowerCase(Locale.ROOT);
        return "dot".equals(normalized) || "circle".equals(normalized)
                || "chevron".equals(normalized) ? normalized : "cross";
    }

    private static int parseColor(String value, int fallback) {
        try {
            String normalized = value == null ? "" : value.trim();
            if (normalized.startsWith("#")) normalized = normalized.substring(1);
            else if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
                normalized = normalized.substring(2);
            }
            long parsed = Long.parseLong(normalized, 16);
            if (normalized.length() <= 6) parsed |= 0xFF000000L;
            return (int) parsed;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
