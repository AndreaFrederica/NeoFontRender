package neofontrender.addons.scrolling;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

final class SmoothScrollConfig {
    static boolean enabled = true;
    static int durationMillis = 200;
    static int wheelStep = 40;

    private SmoothScrollConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("scrolling.enabled", true, "Smooth vanilla GuiSlot and Forge GuiScrollingList wheel movement.")
                .define("scrolling.durationMillis", 200, "Animation duration in milliseconds (60-600).")
                .define("scrolling.wheelStep", 40, "Pixels added to the target per wheel notch (8-160). ");
        enabled = file.getBoolean("scrolling.enabled", true);
        durationMillis = file.getInt("scrolling.durationMillis", 200, 60, 600);
        wheelStep = file.getInt("scrolling.wheelStep", 40, 8, 160);
        file.save();
    }

    static void save() {
        UiEnhancementsConfig.file().set("scrolling.enabled", enabled)
                .set("scrolling.durationMillis", durationMillis)
                .set("scrolling.wheelStep", wheelStep)
                .save();
    }
}
