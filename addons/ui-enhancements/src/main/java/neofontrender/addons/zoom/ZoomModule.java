package neofontrender.addons.zoom;

import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

public final class ZoomModule implements UiEnhancementModule {
    @Override public void preInit() { ZoomConfig.load(); }

    @Override public void init() {
        ClientRegistry.registerKeyBinding(ZoomHandler.ZOOM_KEY);
        // GuiOpenEvent and WorldEvent.Unload fire on the Forge bus, ClientTickEvent on the
        // FML bus; the handler subscribes to both kinds, so register it on both buses.
        MinecraftForge.EVENT_BUS.register(ZoomHandler.INSTANCE);
        FMLCommonHandler.instance().bus().register(ZoomHandler.INSTANCE);
        NfrSettingsPageRegistry.register(new ZoomSettingsPage());
    }
}
