package neofontrender.addons.effects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.common.MinecraftForge;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

public final class ScreenEffectsModule implements UiEnhancementModule {
    @Override public void preInit() { ScreenEffectsConfig.load(); }

    @Override public void init() {
        NfrSettingsPageRegistry.register(new ScreenEffectsSettingsPage());
        MinecraftForge.EVENT_BUS.register(ScreenEffectsRenderer.INSTANCE);
        if (Minecraft.getMinecraft().getResourceManager() instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager())
                    .registerReloadListener(ScreenEffectsRenderer.INSTANCE);
        }
    }
}
