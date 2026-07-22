package neofontrender.addons.hud;

import net.minecraftforge.common.MinecraftForge;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

/** Loads HUD providers and exposes their live runtime configuration. */
public final class HudBarsModule implements UiEnhancementModule {
    @Override
    public void preInit() {
        HudBarsConfig.load();
        VanillaHudBarProviders.register(FoodStatsAccessFactory.create());
    }

    @Override
    public void init() {
        NfrSettingsPageRegistry.register(new HudBarsSettingsPage());
        MinecraftForge.EVENT_BUS.register(new HudBarsHandler());
    }
}
