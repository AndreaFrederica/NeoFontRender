package neofontrender.addons.zoom;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

public final class ZoomModule implements UiEnhancementModule {
    /** Cross-module view used by crosshair/spyglass compatibility. */
    public static boolean isZoomActive() {
        return ZoomHandler.INSTANCE.isZooming();
    }

    @Override public void preInit() { ZoomConfig.load(); }

    @Override public void init() {
        ClientRegistry.registerKeyBinding(ZoomHandler.ZOOM_KEY);
        MinecraftForge.EVENT_BUS.register(ZoomHandler.INSTANCE);
        NfrSettingsPageRegistry.register(new ZoomSettingsPage());
    }
}
