package neofontrender.addons.hud;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

final class HudBarsConfig {
    static boolean enabled = true;
    static boolean yieldToClassicBar = true;
    static boolean health = true;
    static boolean absorption = true;
    static boolean armor = true;
    static boolean toughness = true;
    static boolean food = true;
    static boolean air = true;
    static boolean mountHealth = true;
    static boolean showNumbers = true;
    static boolean showIcons = true;
    static boolean smoothValues = true;
    static boolean rounded = true;
    static String theme = HudBarTheme.MODERN.id;
    static int width = 81;
    static int height = 9;
    static int gap = 2;
    static int textScale = 75;
    static String textPosition = "center";
    static int background = 0xA0000000;
    static int border = 0xB0606060;
    static int healthColor = 0xFFE53935;
    static int healthyColor = 0xFF43A047;
    static int absorptionColor = 0xFFFFC928;
    static int armorColor = 0xFFB7C4D6;
    static int toughnessColor = 0xFF8068B3;
    static int foodColor = 0xFFD77B24;
    static int saturationColor = 0xFFFFD54F;
    static int airColor = 0xFF39C7E8;
    static int mountColor = 0xFFE46A6A;

    private HudBarsConfig() {}

    static void load() {
        NfrConfigFile f = UiEnhancementsConfig.file();
        f.define("hudBars.enabled", true, "Master switch; false leaves every vanilla HUD element untouched.")
                .define("hudBars.yieldToClassicBar", true, "Disable this renderer when Classic Bar is installed.")
                .define("hudBars.health", true, "Replace vanilla health hearts.")
                .define("hudBars.absorption", true, "Show absorption as a separate bar.")
                .define("hudBars.armor", true, "Replace vanilla armor icons.")
                .define("hudBars.toughness", true, "Show armor toughness when non-zero.")
                .define("hudBars.food", true, "Replace hunger and render saturation, exhaustion and food preview.")
                .define("hudBars.air", true, "Replace underwater air bubbles.")
                .define("hudBars.mountHealth", true, "Replace mount hearts.")
                .define("hudBars.showNumbers", true, "Draw current and maximum values in bars.")
                .define("hudBars.showIcons", true, "Draw vanilla HUD icons beside status bars.")
                .define("hudBars.smoothValues", true, "Animate fill changes.")
                .define("hudBars.rounded", true, "Use rounded analytic geometry.")
                .define("hudBars.theme", HudBarTheme.MODERN.id,
                        "Visual theme: modern, flat, glass, segmented or minimal.")
                .define("hudBars.width", 81, "Bar width in GUI pixels (48-160).")
                .define("hudBars.height", 9, "Bar height in GUI pixels (7-16).")
                .define("hudBars.gap", 2, "Vertical gap between bars (0-8).")
                .define("hudBars.textScale", 75, "Numeric text scale in percent (50-125).")
                .define("hudBars.textPosition", "center", "Numeric text position: center or classic.")
                .define("hudBars.background", color(background), "ARGB bar background.")
                .define("hudBars.border", color(border), "ARGB one-pixel border.")
                .define("hudBars.color.healthLow", color(healthColor), "Low-health color.")
                .define("hudBars.color.healthHigh", color(healthyColor), "High-health color.")
                .define("hudBars.color.absorption", color(absorptionColor), "Absorption color.")
                .define("hudBars.color.armor", color(armorColor), "Armor color.")
                .define("hudBars.color.toughness", color(toughnessColor), "Armor toughness color.")
                .define("hudBars.color.food", color(foodColor), "Hunger color.")
                .define("hudBars.color.saturation", color(saturationColor), "Saturation color.")
                .define("hudBars.color.air", color(airColor), "Air color.")
                .define("hudBars.color.mount", color(mountColor), "Mount-health color.");
        enabled = f.getBoolean("hudBars.enabled", true);
        yieldToClassicBar = f.getBoolean("hudBars.yieldToClassicBar", true);
        health = f.getBoolean("hudBars.health", true);
        absorption = f.getBoolean("hudBars.absorption", true);
        armor = f.getBoolean("hudBars.armor", true);
        toughness = f.getBoolean("hudBars.toughness", true);
        food = f.getBoolean("hudBars.food", true);
        air = f.getBoolean("hudBars.air", true);
        mountHealth = f.getBoolean("hudBars.mountHealth", true);
        showNumbers = f.getBoolean("hudBars.showNumbers", true);
        showIcons = f.getBoolean("hudBars.showIcons", true);
        smoothValues = f.getBoolean("hudBars.smoothValues", true);
        rounded = f.getBoolean("hudBars.rounded", true);
        theme = HudBarTheme.parse(f.getString("hudBars.theme", HudBarTheme.MODERN.id)).id;
        width = f.getInt("hudBars.width", 81, 48, 160);
        height = f.getInt("hudBars.height", 9, 7, 16);
        gap = f.getInt("hudBars.gap", 2, 0, 8);
        textScale = f.getInt("hudBars.textScale", 75, 50, 125);
        textPosition = textPosition(f.getString("hudBars.textPosition", "center"));
        background = parse(f.getString("hudBars.background", color(background)), background);
        border = parse(f.getString("hudBars.border", color(border)), border);
        healthColor = parse(f.getString("hudBars.color.healthLow", color(healthColor)), healthColor);
        healthyColor = parse(f.getString("hudBars.color.healthHigh", color(healthyColor)), healthyColor);
        absorptionColor = parse(f.getString("hudBars.color.absorption", color(absorptionColor)), absorptionColor);
        armorColor = parse(f.getString("hudBars.color.armor", color(armorColor)), armorColor);
        toughnessColor = parse(f.getString("hudBars.color.toughness", color(toughnessColor)), toughnessColor);
        foodColor = parse(f.getString("hudBars.color.food", color(foodColor)), foodColor);
        saturationColor = parse(f.getString("hudBars.color.saturation", color(saturationColor)), saturationColor);
        airColor = parse(f.getString("hudBars.color.air", color(airColor)), airColor);
        mountColor = parse(f.getString("hudBars.color.mount", color(mountColor)), mountColor);
        f.save();
    }

    static void save() {
        UiEnhancementsConfig.file().set("hudBars.enabled", enabled)
                .set("hudBars.yieldToClassicBar", yieldToClassicBar)
                .set("hudBars.health", health).set("hudBars.absorption", absorption)
                .set("hudBars.armor", armor).set("hudBars.toughness", toughness)
                .set("hudBars.food", food).set("hudBars.air", air).set("hudBars.mountHealth", mountHealth)
                .set("hudBars.showNumbers", showNumbers).set("hudBars.showIcons", showIcons)
                .set("hudBars.smoothValues", smoothValues)
                .set("hudBars.rounded", rounded).set("hudBars.theme", theme).set("hudBars.width", width)
                .set("hudBars.height", height).set("hudBars.gap", gap).set("hudBars.textScale", textScale)
                .set("hudBars.textPosition", textPosition)
                .set("hudBars.background", color(background)).set("hudBars.border", color(border))
                .set("hudBars.color.healthLow", color(healthColor)).set("hudBars.color.healthHigh", color(healthyColor))
                .set("hudBars.color.absorption", color(absorptionColor)).set("hudBars.color.armor", color(armorColor))
                .set("hudBars.color.toughness", color(toughnessColor)).set("hudBars.color.food", color(foodColor))
                .set("hudBars.color.saturation", color(saturationColor)).set("hudBars.color.air", color(airColor))
                .set("hudBars.color.mount", color(mountColor)).save();
    }

    private static String color(int value) { return String.format("#%08X", value); }
    static String textPosition(String value) { return "classic".equalsIgnoreCase(value) ? "classic" : "center"; }
    private static int parse(String value, int fallback) {
        try { return (int) Long.parseLong(value.startsWith("#") ? value.substring(1) : value, 16); }
        catch (RuntimeException ignored) { return fallback; }
    }
}
