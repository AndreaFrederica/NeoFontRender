package neofontrender.addons.outlines;

import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

public final class BlockOutlinesModule implements UiEnhancementModule {
    @Override public void preInit() { BlockOutlineConfig.load(); }
    @Override public void init() { NfrSettingsPageRegistry.register(new BlockOutlinesSettingsPage()); }
}
