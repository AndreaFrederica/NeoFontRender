package neofontrender.addons.mainmenu;

import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.FMLCommonHandler;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

public final class MainMenuModule implements UiEnhancementModule {
    @Override public void preInit() { MainMenuConfig.load(); }

    @Override public void init() {
        LastPlayedGameManager.INSTANCE.initialize();
        // ClientTickEvent fires on the FML bus, not on MinecraftForge.EVENT_BUS.
        FMLCommonHandler.instance().bus().register(LastPlayedGameManager.INSTANCE);
        NfrSettingsPageRegistry.register(new MainMenuSettingsPage());
    }
}
