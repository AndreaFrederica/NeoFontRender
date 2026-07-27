package neofontrender.addons.loading;

import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

public final class ResourceReloadModule implements UiEnhancementModule {
    @Override public void preInit() { ResourceReloadConfig.load(); }

    @Override public void init() {
        NfrSettingsPageRegistry.register(new ResourceReloadSettingsPage());
    }
}
