package neofontrender.client;

import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import neofontrender.NeoFontRender;
import neofontrender.common.CommonProxy;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.splash.ModernSplashDetector;
import neofontrender.splash.SplashCompat;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        NeoFontRender.LOGGER.info("ClientProxy preInit");
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        NeofontrenderConfig.load();
        if (ModernSplashDetector.isInstalled()) {
            if (SplashCompat.isInstalled()) {
                NeoFontRender.LOGGER.info("ModernSplash font override is active");
            } else if (NeofontrenderConfig.splashFontOverrideEnabled()
                    && NeofontrenderConfig.compatModernSplash()) {
                NeoFontRender.LOGGER.warn("ModernSplash detected but font override was not installed. " +
                        "This usually means ModernSplash changed its internal structure; splash screen will use the default bitmap font.");
            }
        }
        NeofontrenderKeyHandler.init();
        MinecraftForge.EVENT_BUS.register(new NeofontrenderMainMenuBranding());
        MinecraftForge.EVENT_BUS.register(new NeofontrenderOptionsButtonHandler());
        MinecraftForge.EVENT_BUS.register(new NeofontrenderDebugOverlayHandler());
        ClientCommandHandler.instance.registerCommand(new NeofontrenderCommand());
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }
}
