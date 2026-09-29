package neofontrender.addons.cjk;

import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;
import neofontrender.api.text.paragraph.TextParagraphApi;

public final class CjkTypographyModule implements UiEnhancementModule {
    @Override
    public void preInit() {
        CjkTypographyConfig.load();
        TextParagraphApi.register(TiqianParagraphProvider.INSTANCE);
    }

    @Override
    public void init() {
        NfrSettingsPageRegistry.register(new CjkTypographySettingsPage());
    }
}
