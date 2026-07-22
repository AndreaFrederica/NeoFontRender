package neofontrender.addons.effects;

import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraftforge.common.MinecraftForge;
import neofontrender.addons.ui.UiEnhancementModule;

/** Registers gradient and Gaussian blur screen effects. */
public final class ScreenEffectsModule implements UiEnhancementModule {
    @Override
    public void preInit() {
        ScreenEffectsConfig.load();
    }

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(ScreenEffectsRenderer.INSTANCE);
        FMLCommonHandler.instance().bus().register(ScreenEffectsRenderer.INSTANCE);
    }
}
