package neofontrender.addons.input;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

final class TextInputConfig {
    static boolean iBeamCursor = true;
    private TextInputConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("input.iBeamCursor", true, "Use the operating system text cursor over vanilla text fields.");
        iBeamCursor = file.getBoolean("input.iBeamCursor", true);
        file.save();
    }

    static void save() {
        UiEnhancementsConfig.file().set("input.iBeamCursor", iBeamCursor).save();
    }
}
