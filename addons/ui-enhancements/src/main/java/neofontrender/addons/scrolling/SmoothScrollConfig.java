package neofontrender.addons.scrolling;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

/** Persistent options for list scrolling. */
final class SmoothScrollConfig {
    static boolean enabled = true;
    static boolean vanillaLists = true;
    static boolean forgeLists = true;
    static boolean creativeInventory = true;
    static boolean chat = true;
    static int durationMillis = 200;
    static int wheelStep = 40;

    private SmoothScrollConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("scrolling.enabled", true, "Master switch for smooth wheel movement.")
                .define("scrolling.vanillaLists", true, "Smooth vanilla GuiSlot lists.")
                .define("scrolling.forgeLists", true, "Smooth Forge GuiScrollingList lists.")
                .define("scrolling.creativeInventory", true, "Smooth creative inventory item grid scrolling.")
                .define("scrolling.chat", true, "Smooth vanilla chat history scrolling.")
                .define("scrolling.durationMillis", 200, "Animation duration in milliseconds (60-600).")
                .define("scrolling.wheelStep", 40, "Pixels per wheel notch (8-160).");
        enabled = file.getBoolean("scrolling.enabled", true);
        vanillaLists = file.getBoolean("scrolling.vanillaLists", true);
        forgeLists = file.getBoolean("scrolling.forgeLists", true);
        creativeInventory = file.getBoolean("scrolling.creativeInventory", true);
        chat = file.getBoolean("scrolling.chat", true);
        durationMillis = file.getInt("scrolling.durationMillis", 200, 60, 600);
        wheelStep = file.getInt("scrolling.wheelStep", 40, 8, 160);
        file.save();
    }

    static void save() {
        UiEnhancementsConfig.file()
                .set("scrolling.enabled", enabled)
                .set("scrolling.vanillaLists", vanillaLists)
                .set("scrolling.forgeLists", forgeLists)
                .set("scrolling.creativeInventory", creativeInventory)
                .set("scrolling.chat", chat)
                .set("scrolling.durationMillis", durationMillis)
                .set("scrolling.wheelStep", wheelStep)
                .save();
    }
}
