package neofontrender.addons.mainmenu;

import net.minecraftforge.common.MinecraftForge;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

public final class MainMenuModule implements UiEnhancementModule {
    @Override public void preInit() { MainMenuConfig.load(); }

    @Override public void init() {
        LastPlayedGameManager.INSTANCE.initialize();
        MinecraftForge.EVENT_BUS.register(LastPlayedGameManager.INSTANCE);
        NfrSettingsPageRegistry.register(new MainMenuSettingsPage());
    }
}
