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
import neofontrender.api.text.StructuredTextApi;
import neofontrender.core.font.pipeline.builtin.HexChatStructuredMiddleware;
import neofontrender.core.font.pipeline.builtin.TinkersAntiqueSyntaxProvider;
import neofontrender.text.pipeline.TextPipelinePlugin;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        NeoFontRender.LOGGER.info("ClientProxy preInit");
        StructuredTextApi.register(TinkersAntiqueSyntaxProvider.INSTANCE);
        StructuredTextApi.register(new TextPipelinePlugin() {
            @Override public String id() { return "neofontrender:builtins"; }

            @Override
            public java.util.Collection<? extends
                    neofontrender.text.pipeline.StructuredTextMiddleware> structuredMiddlewares() {
                return java.util.Collections.singletonList(HexChatStructuredMiddleware.INSTANCE);
            }
        });
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        // 1.7.10's ModularUI2 has no MuiTextBackends API; MUI text is integrated by
        // MixinModularTextRenderer instead.
        if (!NeofontrenderConfig.isLoaded()) {
            NeofontrenderConfig.load();
        }
        if (ModernSplashDetector.isInstalled()) {
            if (SplashCompat.isInstalled()) {
                NeoFontRender.LOGGER.info("ModernSplash font override is active");
            } else if (NeofontrenderConfig.splashFontOverrideEnabled()
                    && NeofontrenderConfig.compatModernSplash()) {
                NeoFontRender.LOGGER.warn("ModernSplash detected but font override was not installed. " +
                        "This usually means ModernSplash changed its internal structure; splash screen will use the default bitmap font.");
            }
        }
        NeofontrenderBranding.applyModMetadata();
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
