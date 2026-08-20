package neofontrender.addons.controller;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import neofontrender.addons.api.input.InputApi;
import neofontrender.addons.api.input.InputRegistration;
import neofontrender.addons.controller.sdl.SdlDeviceManager;
import neofontrender.addons.controller.sdl.SdlDeviceSource;
import neofontrender.addons.controller.sdl.SdlBindingProvider;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Optional client-side SDL3 controller adapter for UIE's device-neutral input API. */
@Mod(
        modid = ControllerAddonMod.MOD_ID,
        name = ControllerAddonMod.MOD_NAME,
        version = ControllerAddonMod.VERSION,
        dependencies = "required-after:neofontrender@[0.5.0,);"
                + "required-after:neofontrender_ui_enhancements@[0.6.0,)",
        clientSideOnly = true,
        acceptedMinecraftVersions = "[1.12,1.13)"
)
public final class ControllerAddonMod {
    public static final String MOD_ID = "neofontrender_ui_enhancements_controller";
    public static final String MOD_NAME = "Revo UI Controller Support";
    public static final String VERSION = "0.1.0";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    private static SdlDeviceManager deviceManager;
    @SuppressWarnings("unused")
    private static InputRegistration deviceRegistration;
    @SuppressWarnings("unused")
    private static InputRegistration bindingRegistration;
    private static ControllerKeyBindingBridge keyBindingBridge;
    private static ControllerUiInputBridge uiInputBridge;
    private static ControllerFlightCameraRuntime flightCameraRuntime;

    public static SdlDeviceManager deviceManager() { return deviceManager; }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ControllerConfig.load();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        NfrSettingsPageRegistry.register(new ControllerSettingsPage());
        deviceManager = SdlDeviceManager.open(LOGGER);
        if (deviceManager == null) return;

        deviceRegistration = InputApi.registerDeviceSource(
                new ResourceLocation(MOD_ID, "sdl"), 100,
                new SdlDeviceSource(deviceManager, LOGGER));
        bindingRegistration = InputApi.registerBindingProvider(
                new ResourceLocation(MOD_ID, "default"), 100, new SdlBindingProvider());
        keyBindingBridge = new ControllerKeyBindingBridge(deviceManager);
        MinecraftForge.EVENT_BUS.register(keyBindingBridge);
        uiInputBridge = new ControllerUiInputBridge(deviceManager);
        MinecraftForge.EVENT_BUS.register(uiInputBridge);
        flightCameraRuntime = new ControllerFlightCameraRuntime(deviceManager);
        MinecraftForge.EVENT_BUS.register(flightCameraRuntime);
        Runtime.getRuntime().addShutdownHook(new Thread(deviceManager::close,
                "Revo UI SDL controller shutdown"));
    }
}
