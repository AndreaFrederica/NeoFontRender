package neofontrender.addons.ui;

import neofontrender.api.config.NfrConfigApi;
import neofontrender.api.config.NfrConfigFile;
import neofontrender.api.config.NfrConfigStorage;

/** One independent TOML shared by all UI Enhancements feature modules. */
public final class UiEnhancementsConfig {
    private static NfrConfigFile file;

    private UiEnhancementsConfig() {}

    public static synchronized void open() {
        if (file == null) {
            file = NfrConfigApi.builder(NfrUiEnhancements.MOD_ID)
                    .storage(NfrConfigStorage.INDEPENDENT)
                    .fileName("neofontrender-ui-enhancements.toml")
                    .open();
        }
    }

    public static synchronized NfrConfigFile file() {
        open();
        return file;
    }
}
