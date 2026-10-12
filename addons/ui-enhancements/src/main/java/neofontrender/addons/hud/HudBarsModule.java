package neofontrender.addons.hud;

import net.minecraftforge.common.MinecraftForge;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;
import neofontrender.addons.hud.compositor.HudCompositorRenderHandler;

public final class HudBarsModule implements UiEnhancementModule {
    @Override public void preInit() {
        HudBarsConfig.load();
        VanillaHudBarProviders.register();
    }

    @Override public void init() {
        NfrSettingsPageRegistry.register(new HudBarsSettingsPage());
        MinecraftForge.EVENT_BUS.register(new HudBarsHandler());
        MinecraftForge.EVENT_BUS.register(HudCompositorRenderHandler.INSTANCE);
    }
}
