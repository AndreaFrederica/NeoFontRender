package neofontrender.addons.input;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

/** Persistent options for text input feedback. */
final class TextInputConfig {
    static boolean iBeamCursor = true;

    private TextInputConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("input.iBeamCursor", true, "Use a native I-beam cursor over vanilla text fields.");
        iBeamCursor = file.getBoolean("input.iBeamCursor", true);
        file.save();
    }

    static void save() {
        UiEnhancementsConfig.file()
                .set("input.iBeamCursor", iBeamCursor)
                .save();
    }
}
