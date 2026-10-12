package neofontrender.addons.diagnostics;

import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

public final class DiagnosticsModule implements UiEnhancementModule {
    @Override
    public void preInit() {
        // no-op
    }

    @Override
    public void init() {
        NfrSettingsPageRegistry.register(new DiagnosticsSettingsPage());
    }
}
