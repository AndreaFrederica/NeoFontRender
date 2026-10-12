package neofontrender.addons.navigation;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.FMLCommonHandler;
import neofontrender.addons.api.ui.navigation.UiNavigationApi;
import neofontrender.addons.navigation.vanilla.VanillaGuiTreeProvider;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.addons.ui.UiEnhancementModule;

public final class UiNavigationModule implements UiEnhancementModule {
    @Override public void preInit() {
        // 1.7.10 has no ModularUI navigation API, so the vanilla tree provider is the only source.
        UiNavigationApi.registerProvider(new ResourceLocation(NfrUiEnhancements.MOD_ID, "vanilla"),
                0, new VanillaGuiTreeProvider());
    }

    @Override public void init() {
        // GUI events live on the Forge bus, client ticks on the FML bus; the runtime subscribes to both.
        MinecraftForge.EVENT_BUS.register(UiNavigationRuntime.instance());
        FMLCommonHandler.instance().bus().register(UiNavigationRuntime.instance());
        MinecraftForge.EVENT_BUS.register(new UiFocusRenderer());
    }
}
