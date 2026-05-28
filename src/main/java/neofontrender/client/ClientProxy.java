package neofontrender.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import neofontrender.NeoFontRender;
import neofontrender.common.CommonProxy;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.FontManager;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        NeoFontRender.LOGGER.info("ClientProxy preInit");
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        Minecraft mc = Minecraft.getMinecraft();
        NeofontrenderConfig.load();
        if (mc.getTextureManager() != null) {
            FontManager.INSTANCE.init(mc.getTextureManager());
        }
        if (mc.getResourceManager() != null) {
            FontManager.INSTANCE.reload(mc.getResourceManager());
        }

        NeofontrenderKeyHandler.init();
        MinecraftForge.EVENT_BUS.register(new NeofontrenderMainMenuBranding());
        ClientCommandHandler.instance.registerCommand(new NeofontrenderCommand());
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }
}
