package neofontrender.addons.mainmenu;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

public final class MainMenuModule implements UiEnhancementModule {
    @Override public void preInit() { MainMenuConfig.load(); }

    @Override public void init() {
        // The Forge connect helper assumes that its server metadata map was
        // initialized by GuiMultiplayer.  Continue Game connects directly from
        // the main menu, so initialize it for that path as well.
        FMLClientHandler.instance().setupServerList();
        LastPlayedGameManager.INSTANCE.initialize();
        MinecraftForge.EVENT_BUS.register(LastPlayedGameManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(new CustomMainMenuButtonInjector());
        NfrSettingsPageRegistry.register(new MainMenuSettingsPage());
    }
}
