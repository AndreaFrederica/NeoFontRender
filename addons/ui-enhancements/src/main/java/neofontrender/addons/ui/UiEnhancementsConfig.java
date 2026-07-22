package neofontrender.addons.ui;

import neofontrender.api.config.NfrConfigApi;
import neofontrender.api.config.NfrConfigFile;
import neofontrender.api.config.NfrConfigStorage;

/** Owns the independent TOML shared by all addon modules. */
public final class UiEnhancementsConfig {
    private static NfrConfigFile file;

    private UiEnhancementsConfig() {}

    /** Opens the addon configuration exactly once. */
    public static synchronized void open() {
        if (file == null) {
            file = NfrConfigApi.builder(NfrUiEnhancements.MOD_ID)
                    .storage(NfrConfigStorage.INDEPENDENT)
                    .fileName("neofontrender-ui-enhancements.toml")
                    .open();
        }
    }

    /** Returns the initialized addon configuration. */
    public static synchronized NfrConfigFile file() {
        open();
        return file;
    }
}
