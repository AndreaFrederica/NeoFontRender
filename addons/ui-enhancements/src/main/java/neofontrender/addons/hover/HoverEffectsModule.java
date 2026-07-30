package neofontrender.addons.hover;

import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

public final class HoverEffectsModule implements UiEnhancementModule {
    @Override public void preInit() { HoverEffectsConfig.load(); }
    @Override public void init() { NfrSettingsPageRegistry.register(new HoverEffectsSettingsPage()); }
}
