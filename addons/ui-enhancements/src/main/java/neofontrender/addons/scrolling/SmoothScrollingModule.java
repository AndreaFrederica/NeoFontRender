package neofontrender.addons.scrolling;

import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

/** Enables smooth wheel animation for vanilla and Forge list widgets. */
public final class SmoothScrollingModule implements UiEnhancementModule {
    @Override
    public void preInit() {
        SmoothScrollConfig.load();
    }

    @Override
    public void init() {
        NfrSettingsPageRegistry.register(new SmoothScrollingSettingsPage());
    }
}
