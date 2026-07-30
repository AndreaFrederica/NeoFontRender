package neofontrender.addons.hover;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

final class HoverEffectsConfig {
    static boolean enabled = true;
    static boolean buttons = true;
    static int buttonEnterMillis = 120;
    static int buttonExitMillis = 160;
    static boolean slots = true;
    static int slotEnterMillis = 100;
    static int slotExitMillis = 180;
    static int slotColor = 0x80FFFFFF;
    static boolean jeiIngredientGrid = true;
    static boolean modularUiSlots = true;
    static boolean modularUiThemeColor = true;

    private HoverEffectsConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("hover.enabled", true, "Master switch for smooth control hover transitions.")
                .define("hover.buttons", true, "Cross-fade vanilla and Forge button states.")
                .define("hover.buttonEnterMillis", 120, "Button hover fade-in duration in milliseconds (0-1000).")
                .define("hover.buttonExitMillis", 160, "Button hover fade-out duration in milliseconds (0-1000).")
                .define("hover.slots", true, "Fade vanilla inventory-slot highlights in and out.")
                .define("hover.slotEnterMillis", 100, "Slot hover fade-in duration in milliseconds (0-1000).")
                .define("hover.slotExitMillis", 180, "Slot hover fade-out duration in milliseconds (0-1000).")
                .define("hover.slotColor", "#80FFFFFF", "ARGB vanilla slot-highlight color and opacity.")
                .define("hover.jeiIngredientGrid", true, "Animate JEI and HEI ingredient-grid highlights.")
                .define("hover.modularUiSlots", true, "Animate ModularUI ItemSlot highlights.")
                .define("hover.modularUiThemeColor", true, "Keep each ModularUI theme's slot-highlight color.");
        enabled = file.getBoolean("hover.enabled", true);
        buttons = file.getBoolean("hover.buttons", true);
        buttonEnterMillis = file.getInt("hover.buttonEnterMillis", 120, 0, 1000);
        buttonExitMillis = file.getInt("hover.buttonExitMillis", 160, 0, 1000);
        slots = file.getBoolean("hover.slots", true);
        slotEnterMillis = file.getInt("hover.slotEnterMillis", 100, 0, 1000);
        slotExitMillis = file.getInt("hover.slotExitMillis", 180, 0, 1000);
        slotColor = parseColor(file.getString("hover.slotColor", "#80FFFFFF"), 0x80FFFFFF);
        jeiIngredientGrid = file.getBoolean("hover.jeiIngredientGrid", true);
        modularUiSlots = file.getBoolean("hover.modularUiSlots", true);
        modularUiThemeColor = file.getBoolean("hover.modularUiThemeColor", true);
        file.save();
    }

    static void save() {
        UiEnhancementsConfig.file().set("hover.enabled", enabled)
                .set("hover.buttons", buttons)
                .set("hover.buttonEnterMillis", buttonEnterMillis)
                .set("hover.buttonExitMillis", buttonExitMillis)
                .set("hover.slots", slots)
                .set("hover.slotEnterMillis", slotEnterMillis)
                .set("hover.slotExitMillis", slotExitMillis)
                .set("hover.slotColor", String.format("#%08X", slotColor))
                .set("hover.jeiIngredientGrid", jeiIngredientGrid)
                .set("hover.modularUiSlots", modularUiSlots)
                .set("hover.modularUiThemeColor", modularUiThemeColor)
                .save();
    }

    private static int parseColor(String value, int fallback) {
        try {
            String hex = value == null ? "" : value.trim();
            if (hex.startsWith("#")) hex = hex.substring(1);
            return (int) Long.parseLong(hex, 16);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
