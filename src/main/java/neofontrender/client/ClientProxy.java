package neofontrender.client;

import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import com.cleanroommc.modularui.api.text.MuiTextBackends;
import neofontrender.NeoFontRender;
import neofontrender.common.CommonProxy;
import neofontrender.client.integration.NfrMuiTextBackend;
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

        // MUI remains independent of NFR. NFR registers its implementation on
        // the client after the required MUI dependency has been loaded.
        MuiTextBackends.register(new NfrMuiTextBackend());

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
        ClientCommandHandler.instance.registerCommand(new NeofontrenderCommand());
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }
}
