package neofontrender.addons.loading;

import net.minecraftforge.common.MinecraftForge;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

public final class WorldLoadingModule implements UiEnhancementModule {
    @Override public void preInit() { WorldLoadingConfig.load(); }

    @Override public void init() {
        NfrSettingsPageRegistry.register(new WorldLoadingSettingsPage());
        MinecraftForge.EVENT_BUS.register(WorldLoadingRenderer.INSTANCE);
    }
}
