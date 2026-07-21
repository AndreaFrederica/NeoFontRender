package neofontrender.addons.effects;

import net.minecraftforge.common.MinecraftForge;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

public final class ScreenEffectsModule implements UiEnhancementModule {
    @Override public void preInit() { ScreenEffectsConfig.load(); }

    @Override public void init() {
        NfrSettingsPageRegistry.register(new ScreenEffectsSettingsPage());
        MinecraftForge.EVENT_BUS.register(ScreenEffectsRenderer.INSTANCE);
    }
}
