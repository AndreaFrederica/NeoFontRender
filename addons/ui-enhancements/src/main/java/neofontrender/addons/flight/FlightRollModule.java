package neofontrender.addons.flight;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import neofontrender.addons.flight.network.FlightRollNetwork;
import neofontrender.addons.api.flight.FlightApi;
import neofontrender.addons.hud.compositor.HudWindowCompositor;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

public final class FlightRollModule implements UiEnhancementModule {
    @Override public void preInit() {
        FlightApi.installBackend(FlightRollController.INSTANCE);
        FlightRollConfig.load();
        CrosshairConfig.load();
        ShoulderSurfingFixConfig.load();
        FlightHudThemeManager.INSTANCE.initialize();
    }

    @Override public void init() {
        ShoulderSurfingCompat.registerAdaptiveItems();
        ShoulderSurfingFixSettings.register();
        FlightRollNetwork.initializeClient(FlightRollController.INSTANCE);
        // The full UIE jar owns the integrated-server implementation. The separate companion jar
        // supplies the same common protocol to dedicated servers without shipping client renderers.
        FlightRollNetwork.initializeServer();
        ClientRegistry.registerKeyBinding(FlightRollController.ROLL_LEFT);
        ClientRegistry.registerKeyBinding(FlightRollController.ROLL_RIGHT);
        ClientRegistry.registerKeyBinding(FlightRollController.YAW_LEFT);
        ClientRegistry.registerKeyBinding(FlightRollController.YAW_RIGHT);
        MinecraftForge.EVENT_BUS.register(FlightRollController.INSTANCE);
        MinecraftForge.EVENT_BUS.register(FlightHudOverlayController.INSTANCE);
        MinecraftForge.EVENT_BUS.register(CrosshairController.INSTANCE);
        FMLCommonHandler.instance().bus().register(FlightRollController.INSTANCE);
        HudWindowCompositor.INSTANCE.register(FlightHudSurface.INSTANCE);
        NfrSettingsPageRegistry.register(new FlightRollSettingsPage());
        NfrSettingsPageRegistry.register(new CrosshairSettingsPage());
    }
}
