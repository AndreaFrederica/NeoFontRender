package neofontrender.addons.navigation;

import net.minecraftforge.common.MinecraftForge;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.ui.navigation.UiNavigationApi;
import neofontrender.addons.navigation.modularui.ModularUiTreeProvider;
import neofontrender.addons.navigation.vanilla.VanillaGuiTreeProvider;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.addons.ui.UiEnhancementModule;

public final class UiNavigationModule implements UiEnhancementModule {
    @Override public void preInit() {
        UiNavigationApi.registerProvider(new ResourceLocation(NfrUiEnhancements.MOD_ID, "modularui"),
                1_000, new ModularUiTreeProvider());
        UiNavigationApi.registerProvider(new ResourceLocation(NfrUiEnhancements.MOD_ID, "vanilla"),
                0, new VanillaGuiTreeProvider());
    }

    @Override public void init() {
        MinecraftForge.EVENT_BUS.register(UiNavigationRuntime.instance());
        MinecraftForge.EVENT_BUS.register(new UiFocusRenderer());
    }
}
