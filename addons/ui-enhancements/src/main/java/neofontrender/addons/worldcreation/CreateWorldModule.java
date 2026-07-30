package neofontrender.addons.worldcreation;

import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

public final class CreateWorldModule implements UiEnhancementModule {
    @Override public void preInit() { CreateWorldConfig.load(); }
    @Override public void init() { NfrSettingsPageRegistry.register(new CreateWorldSettingsPage()); }
}
