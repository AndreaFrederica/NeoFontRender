package neofontrender.addons.hud;

import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

import java.util.Locale;

/** Persistent runtime configuration for replacement HUD bars and their appearance. */
final class HudBarsConfig {
    static boolean enabled = true;
    static boolean yieldToClassicBar = true;
    static boolean health = true;
    static boolean absorption = true;
    static boolean armor = true;
    static boolean food = true;
    static boolean air = true;
    static boolean mountHealth = true;
    static boolean showNumbers = true;
    static boolean smoothValues = true;
    static boolean rounded = true;
    static String theme = HudBarTheme.MODERN.id;
    static int width = 81;
    static int height = 9;
    static int gap = 2;
    static int background = 0xA0000000;
    static int border = 0xB0606060;
    static int healthColor = 0xFFE53935;
    static int healthyColor = 0xFF43A047;
    static int absorptionColor = 0xFFFFC928;
    static int armorColor = 0xFFB7C4D6;
    static int foodColor = 0xFFD77B24;
    static int saturationColor = 0xFFFFD54F;
    static int airColor = 0xFF39C7E8;
    static int mountColor = 0xFFE46A6A;

    private HudBarsConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        defineDefaults(file);
        enabled = file.getBoolean("hudBars.enabled", true);
        yieldToClassicBar = file.getBoolean("hudBars.yieldToClassicBar", true);
        health = file.getBoolean("hudBars.health", true);
        absorption = file.getBoolean("hudBars.absorption", true);
        armor = file.getBoolean("hudBars.armor", true);
        food = file.getBoolean("hudBars.food", true);
        air = file.getBoolean("hudBars.air", true);
        mountHealth = file.getBoolean("hudBars.mountHealth", true);
        showNumbers = file.getBoolean("hudBars.showNumbers", true);
        smoothValues = file.getBoolean("hudBars.smoothValues", true);
        rounded = file.getBoolean("hudBars.rounded", true);
        theme = HudBarTheme.parse(file.getString("hudBars.theme", HudBarTheme.MODERN.id)).id;
        width = file.getInt("hudBars.width", 81, 48, 160);
        height = file.getInt("hudBars.height", 9, 7, 16);
        gap = file.getInt("hudBars.gap", 2, 0, 8);
        background = parseColor(file.getString("hudBars.background", color(background)), background);
        border = parseColor(file.getString("hudBars.border", color(border)), border);
        healthColor = parseColor(file.getString("hudBars.color.healthLow", color(healthColor)), healthColor);
        healthyColor = parseColor(file.getString("hudBars.color.healthHigh", color(healthyColor)), healthyColor);
        absorptionColor = parseColor(
                file.getString("hudBars.color.absorption", color(absorptionColor)), absorptionColor);
        armorColor = parseColor(file.getString("hudBars.color.armor", color(armorColor)), armorColor);
        foodColor = parseColor(file.getString("hudBars.color.food", color(foodColor)), foodColor);
        saturationColor = parseColor(
                file.getString("hudBars.color.saturation", color(saturationColor)), saturationColor);
        airColor = parseColor(file.getString("hudBars.color.air", color(airColor)), airColor);
        mountColor = parseColor(file.getString("hudBars.color.mount", color(mountColor)), mountColor);
        file.save();
    }

    static void save() {
        theme = HudBarTheme.parse(theme).id;
        UiEnhancementsConfig.file()
                .set("hudBars.enabled", enabled)
                .set("hudBars.yieldToClassicBar", yieldToClassicBar)
                .set("hudBars.health", health)
                .set("hudBars.absorption", absorption)
                .set("hudBars.armor", armor)
                .set("hudBars.food", food)
                .set("hudBars.air", air)
                .set("hudBars.mountHealth", mountHealth)
                .set("hudBars.showNumbers", showNumbers)
                .set("hudBars.smoothValues", smoothValues)
                .set("hudBars.rounded", rounded)
                .set("hudBars.theme", theme)
                .set("hudBars.width", width)
                .set("hudBars.height", height)
                .set("hudBars.gap", gap)
                .set("hudBars.background", color(background))
                .set("hudBars.border", color(border))
                .set("hudBars.color.healthLow", color(healthColor))
                .set("hudBars.color.healthHigh", color(healthyColor))
                .set("hudBars.color.absorption", color(absorptionColor))
                .set("hudBars.color.armor", color(armorColor))
                .set("hudBars.color.food", color(foodColor))
                .set("hudBars.color.saturation", color(saturationColor))
                .set("hudBars.color.air", color(airColor))
                .set("hudBars.color.mount", color(mountColor))
                .save();
    }

    static int parseColor(String input, int fallback) {
        if (input == null) {
            NfrUiEnhancements.LOGGER.error("HUD color must not be null; using fallback {}", color(fallback));
            return fallback;
        }
        try {
            String value = input.trim();
            if (value.startsWith("#")) value = value.substring(1);
            if (value.length() == 6) value = "FF" + value;
            if (value.length() != 8) throw new IllegalArgumentException("expected RGB or ARGB hexadecimal color");
            return (int) Long.parseLong(value, 16);
        } catch (RuntimeException exception) {
            NfrUiEnhancements.LOGGER.error("Invalid HUD color '{}'; using fallback {}", input, color(fallback), exception);
            return fallback;
        }
    }

    private static void defineDefaults(NfrConfigFile file) {
        file.define("hudBars.enabled", true, "Master switch; false leaves every vanilla HUD element untouched.")
                .define("hudBars.yieldToClassicBar", true, "Disable this renderer when Classic Bar is installed.")
                .define("hudBars.health", true, "Replace vanilla health hearts.")
                .define("hudBars.absorption", true, "Show absorption as a separate bar.")
                .define("hudBars.armor", true, "Replace vanilla armor icons.")
                .define("hudBars.food", true, "Replace hunger and show saturation, exhaustion and food preview.")
                .define("hudBars.air", true, "Replace underwater air bubbles.")
                .define("hudBars.mountHealth", true, "Replace mount hearts.")
                .define("hudBars.showNumbers", true, "Draw current and maximum values in bars.")
                .define("hudBars.smoothValues", true, "Animate fill changes.")
                .define("hudBars.rounded", true, "Use rounded geometry for compatible themes.")
                .define("hudBars.theme", HudBarTheme.MODERN.id,
                        "Visual theme: classic, modern, flat, glass, segmented or minimal.")
                .define("hudBars.width", 81, "Bar width in GUI pixels (48-160).")
                .define("hudBars.height", 9, "Bar height in GUI pixels (7-16).")
                .define("hudBars.gap", 2, "Vertical gap between bars (0-8).")
                .define("hudBars.background", color(background), "ARGB bar background.")
                .define("hudBars.border", color(border), "ARGB one-pixel border.")
                .define("hudBars.color.healthLow", color(healthColor), "Low-health color.")
                .define("hudBars.color.healthHigh", color(healthyColor), "High-health color.")
                .define("hudBars.color.absorption", color(absorptionColor), "Absorption color.")
                .define("hudBars.color.armor", color(armorColor), "Armor color.")
                .define("hudBars.color.food", color(foodColor), "Hunger color.")
                .define("hudBars.color.saturation", color(saturationColor), "Saturation color.")
                .define("hudBars.color.air", color(airColor), "Air color.")
                .define("hudBars.color.mount", color(mountColor), "Mount-health color.");
    }

    private static String color(int value) {
        return String.format(Locale.ROOT, "#%08X", value);
    }
}
